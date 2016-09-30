package rl4j;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
/**
 * Java/recommenderlab implementation of:
 * 
 * CLiMF: Learning to Maximize Reciprocal Rank with Collaborative Less-is-More Filtering 
 * Yue Shi, Martha Larson, Alexandros Karatzoglou, Nuria Oliver, Linas Baltrunas, Alan Hanjalic 
 * ACM RecSys 2012
 * 
 * This implementation is based loosely on the python at: https://github.com/gamboviol/climf
 * 
 * However, this java version is not finished and may not give satisfactory results.
 *
 */
public class ClimfCollaborativeFilter implements CollaborativeFilter {
    private final double likeThreshold;
    private final int dimensionality;
    private final double lambda;
    private final double gamma;
    private final int maxIterations;
    private final LabeledMatrix trainingExamples;

    private FlexCompRowMatrix userGradients;
    private FlexCompRowMatrix itemGradients;
    private LabeledMatrix data;

    public ClimfCollaborativeFilter(LabeledMatrix trainingExamples, double likeThreshold, int dimensionality,
        double lambda, double gamma, int maxIterations) {
        this.likeThreshold = likeThreshold;
        this.dimensionality = dimensionality;
        this.lambda = lambda;
        this.gamma = gamma;
        this.maxIterations = maxIterations;
        this.trainingExamples = trainingExamples;
    }

    private void gradientAscent() {
        for (int i = 0; i < maxIterations; i++) {
            gradientAscentUpdate(data);
            double objective = objective(data);
            System.out.println(new Date() + " : iter: " + i + " / objective: " + objective);
        }
    }

    private void gradientAscentUpdate(LabeledMatrix data) {
        for (int i = 0; i < userGradients.numRows(); i++) {

            // dU = -lbda*U[i]
            SparseVector userGradient = userGradients.getRow(i);
            Vector dU = userGradient.copy().scale(-lambda);

            // f = precompute_f(data,U,V,i)
            Map<Integer, Double> f = userItemDotProductsByItemIndex(i);

            for (Map.Entry<Integer, Double> entryJ : f.entrySet()) {

                // dV = g(-f[j])-lbda*V[j]
                // aka: -(lbda*V[j]) + (g(-f[j]))
                int j = entryJ.getKey();
                double userItemDotProduct = entryJ.getValue();
                double sigmoidItemGradientJ = sigmoid(-userItemDotProduct);
                Vector itemGradient = itemGradients.getRow(j);
                Vector dV = itemGradient.copy().scale(-lambda);
                for (VectorEntry ve : dV) {
                    dV.add(ve.index(), sigmoidItemGradientJ);
                }

                for (Map.Entry<Integer, Double> entryK : f.entrySet()) {

                    // dV += dg(f[j]-f[k])*(1/(1-g(f[k]-f[j]))-1/(1-g(f[j]-f[k])))*U[i]
                    double d = entryK.getValue();
                    Vector _dV = userGradient.copy().scale((derivativeOfSigmoid(userItemDotProduct - d))
                        * (1 / (1 - sigmoid(d - userItemDotProduct)) - 1 / (1 - sigmoid(userItemDotProduct - d))));
                    dV = dV.add(_dV);
                }

                // V[j] += gamma*dV
                Vector temp = dV.copy().scale(gamma);
                itemGradient = itemGradient.add(temp);
                SparseVector svItemGradient =
                    itemGradient instanceof SparseVector ? (SparseVector) itemGradient : new SparseVector(itemGradient);
                itemGradients.setRow(j, svItemGradient);

                // dU += g(-f[j])*V[j]
                dU = dU.add(itemGradient.copy().scale(sigmoidItemGradientJ));
                for (Map.Entry<Integer, Double> entryK : f.entrySet()) {

                    // dU += (V[j]-V[k])*dg(f[k]-f[j])/(1-g(f[k]-f[j]))
                    int k = entryK.getKey();
                    double d = entryK.getValue();
                    SparseVector svItemGradient1 = itemGradients.getRow(k);
                    Vector du1 = (svItemGradient.copy().add(-1, svItemGradient1)).copy();
                    double du2 = derivativeOfSigmoid(d - userItemDotProduct);
                    double du3 = 1 - sigmoid(d - userItemDotProduct);
                    Vector _dU = du1.scale(du2).scale(1 / du3);
                    dU = dU.add(_dU);
                }
            }

            // U[i] += gamma*dU
            Vector updatedUserGradient = userGradient.copy().add(dU.copy().scale(gamma));
            SparseVector svUpdatedUserGradient = updatedUserGradient instanceof SparseVector
                ? (SparseVector) updatedUserGradient : new SparseVector(updatedUserGradient);
            userGradients.setRow(i, svUpdatedUserGradient);
        }
    }

    private double objective(LabeledMatrix data) {
        // F = -0.5*lbda*(np.sum(U*U)+np.sum(V*V))
        double F = -.5 * lambda * (total(elementwiseSquare(userGradients)) + total(elementwiseSquare(itemGradients)));
        for (int i = 0; i < userGradients.numRows(); i++) {

            // f = precompute_f(data,U,V,i)
            Map<Integer, Double> f = userItemDotProductsByItemIndex(i);
            for (Map.Entry<Integer, Double> entryJ : f.entrySet()) {
                double userItemDotProduct = entryJ.getValue();
                // F += log(g(f[j]))
                F += Math.log(sigmoid(userItemDotProduct));
                for (Map.Entry<Integer, Double> entryK : f.entrySet()) {
                    double d = entryK.getValue();
                    // F += log(1-g(f[k]-f[j]))
                    F += Math.log(1 - (sigmoid(d - userItemDotProduct)));
                }
            }
        }
        return F;
    }

    private Map<Integer, Double> userItemDotProductsByItemIndex(int i) {
        SortedMap<Integer, Double> m = new TreeMap<>();
        int[] indices = indices(data.m.getRow(i));
        for (int j = 0; j < indices.length; j++) {
            SparseVector userGradient = userGradients.getRow(i);
            SparseVector itemGradient = itemGradients.getRow(indices[j]);
            double d = userGradient.dot(itemGradient);
            m.put(indices[j], d);
        }
        return m;
    }

    private int[] indices(SparseVector v) {
        int[] indices = new int[v.getUsed()];
        int i = 0;
        for (VectorEntry ve : v) {
            indices[i++] = ve.index();
        }
        return indices;
    }

    private double total(Matrix m) {
        double total = 0;
        for (MatrixEntry me : m) {
            total += me.get();
        }
        return total;
    }

    private FlexCompRowMatrix elementwiseSquare(Matrix A) {
        FlexCompRowMatrix B = new FlexCompRowMatrix(A.numRows(), A.numColumns());
        for (MatrixEntry me : A) {
            double val = me.get();
            B.set(me.row(), me.column(), (val * val));
        }
        return B;
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private double derivativeOfSigmoid(double x) {
        return Math.exp(x) / Math.pow((1 + Math.exp(x)), 2);
    }

    private FlexCompRowMatrix initialRandomGradients(boolean items) {
        int numRows = items ? data.colLabels.length : data.rowLabels.length;
        Random random = new Random();
        FlexCompRowMatrix randomMatrix = new FlexCompRowMatrix(numRows, dimensionality);
        for (int j = 0; j < randomMatrix.numColumns(); j++) {
            for (int i = 0; i < randomMatrix.numRows(); i++) {
                randomMatrix.set(i, j, random.nextDouble() * .01);
                // randomMatrix.set(i, j, items ? .002 : .001);
            }
        }
        return randomMatrix;
    }

    private FlexCompRowMatrix ratingsMatrix(LabeledMatrix trainingExamples, LabeledMatrix testExamples) {
        FlexCompRowMatrix dataMatrix = new FlexCompRowMatrix(trainingExamples.m.numRows() + testExamples.m.numRows(),
            trainingExamples.m.numColumns());
        String[] rowLabels = new String[dataMatrix.numRows()];
        for (int i = 0; i < trainingExamples.m.numRows(); i++) {
            rowLabels[i] = trainingExamples.rowLabel(i);
            dataMatrix.setRow(i, trainingExamples.m.getRow(i));
        }
        for (int i = 0, j = trainingExamples.m.numRows(); i < testExamples.m.numRows(); i++, j++) {
            rowLabels[j] = testExamples.rowLabel(i);
            dataMatrix.setRow(j, testExamples.m.getRow(i));
        }
        data = new LabeledMatrix(dataMatrix, rowLabels, trainingExamples.colLabels);
        userGradients = initialRandomGradients(false);
        itemGradients = initialRandomGradients(true);
        gradientAscent();

        FlexCompRowMatrix ratingsMatrix = new FlexCompRowMatrix(testExamples.rowLabels.length, testExamples.colLabels.length);
        for (int i = 0; i < testExamples.rowLabels.length; i++) {
            int rowNum = trainingExamples.m.numRows() + i;
            SparseVector userGradient = userGradients.getRow(rowNum);
            Prediction[] predictions = new Prediction[itemGradients.numRows()];
            double[] totals = new double[itemGradients.numRows()];
            for (int j = 0; j < itemGradients.numRows(); j++) {
                SparseVector itemGradient = itemGradients.getRow(j);
                double total = 0;
                for (int k = 0; k < itemGradient.size(); k++) {
                    total += (itemGradient.get(k) * userGradient.get(k));
                }
                totals[j] = total;
                predictions[j] = new Prediction(total, j);
            }
            Arrays.sort(predictions);
            SparseVector v = new SparseVector(predictions.length);
            for(int j=0 ; j<predictions.length ; j++) {
                v.set(j, predictions[j].i);
            }
            ratingsMatrix.setRow(i, v);
        }
        return ratingsMatrix;
    }

    private static class Prediction implements Comparable<Prediction> {
        final double v;
        final int i;

        Prediction(double v, int i) {
            this.v = v;
            this.i = i;
        }

        @Override
        public int compareTo(Prediction that) {
            return Double.compare(this.v, that.v);
        }

        public String toString() {
            return (i + ": " + v);
        }

    }

    @Override
    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors,
        int numRecommendations) {
        FlexCompRowMatrix ratingsMatrix = ratingsMatrix(trainingExamples, testExamples);
        return CollaborativeFilterHelper.generateRecommendations(testExamples, ratingsMatrix, numRecommendations, likeThreshold);
    }

    @Override
    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        return CollaborativeFilterHelper.recommendationsAsTopNList(testExamples, ratingsMatrix(trainingExamples, testExamples),
            numRecommendations, likeThreshold);
    }

    public double getLikeThreshold() {
        return likeThreshold;
    }

    private static LabeledMatrix loadTestMatrix(String path) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        br.readLine();
        br.readLine();
        String a = br.readLine();
        String[] tokens = a.split(" ");
        int numRows = Integer.parseInt(tokens[0]);
        int numCols = Integer.parseInt(tokens[1]);
        FlexCompRowMatrix m = new FlexCompRowMatrix(numRows, numCols);
        while ((a = br.readLine()) != null) {
            tokens = a.split(" ");
            int row = Integer.parseInt(tokens[0]) - 1;
            int col = Integer.parseInt(tokens[1]) - 1;
            int val = Integer.parseInt(tokens[2]);
            m.set(row, col, val);
        }
        br.close();
        String[] rowLabels = new String[m.numRows()];
        for (int i = 0; i < rowLabels.length; i++) {
            rowLabels[i] = "R" + (i + 1);
        }
        String[] colLabels = new String[m.numColumns()];
        for (int i = 0; i < colLabels.length; i++) {
            colLabels[i] = "X" + (i + 1);
        }
        LabeledMatrix lm = new LabeledMatrix(m, rowLabels, colLabels);
        return lm;
    }

    public static void main(String[] args) throws Exception {
        // BufferedReader br = new BufferedReader(new InputStreamReader(new
        // FileInputStream("climf/EP25_UPL5_train.mtx")));
        LabeledMatrix train = loadTestMatrix("climf/simple1.mtx");
        LabeledMatrix test = loadTestMatrix("climf/simple1_test.mtx");
        ClimfCollaborativeFilter ccf = new ClimfCollaborativeFilter(train, 1.0, 10, 0, .0001, 20);
        Map<String, String[]> recommendations = ccf.generateRecommendations(test, 20, 1);
        for (String row : test.rowLabels) {
            System.out.print(row + ": ");
            for (String s : recommendations.get(row)) {
                System.out.print(s + " , ");
            }
            System.out.println("");
        }
    }

}
