package rl4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.IntStream;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import rl4j.KNearestNeighbor.KNNResults;

public class MemoryUserBasedCollaborativeFilter implements CollaborativeFilter {
    private final LabeledMatrix trainingExamples;
    private final double likeThreshold;
    private final Similarity sim;
    private final CollaborativeFilterHelper cfh;

    public MemoryUserBasedCollaborativeFilter(LabeledMatrix trainingExamples, Similarity sim, double likeThreshold) {
        this.trainingExamples = trainingExamples;
        this.sim = sim;
        this.likeThreshold = likeThreshold;
        this.cfh = new CollaborativeFilterHelper(this);
    }    

    public static class RecommendationsByExampleIndex {
        public final Map<Integer, int[]> items;
        public final Map<Integer, double[]> ratings;

        public RecommendationsByExampleIndex(Map<Integer, int[]> items, Map<Integer, double[]> ratings) {
            this.items = Collections.unmodifiableMap(items);
            this.ratings = Collections.unmodifiableMap(ratings);
        }
    }
    @Override
    public FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        FlexCompRowMatrix simMatrix = sim.similarity(testExamples.m, trainingExamples.m, likeThreshold);
        KNNResults knn = KNearestNeighbor.knn(simMatrix, numNeighbors);
        FlexCompRowMatrix items = ratings(trainingExamples.m, knn.indexes);
        return items;
    }
    
    @Override
    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        return cfh.generateRecommendations(testExamples, numNeighbors, numRecommendations);
    }
    
    @Override
    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        return cfh.recommendationsAsTopNList(testExamples, numNeighbors, numRecommendations);
    }
    
    private FlexCompRowMatrix ratings(Matrix data, Matrix knnResults) {
        FlexCompRowMatrix ratings = new FlexCompRowMatrix(knnResults.numColumns(), data.numColumns());
        int[] colIndexes = IntStream.rangeClosed(0, data.numColumns()-1).toArray();
        for(int i=0 ; i<knnResults.numColumns() ; i++) {
            double[] rowIndexesD = Matrices.getArray(Matrices.getColumn(knnResults, i));
            int[] rowIndexes = Arrays.stream(rowIndexesD).mapToInt(e -> (int) e).toArray();
            Vector colSums = columnSums(Matrices.getSubMatrix(data, rowIndexes, colIndexes));
            ratings.setRow(i, new SparseVector(colSums));
        }
        return ratings;
    }
    
    private Vector columnSums(Matrix m) {
        Vector v = new DenseVector(m.numColumns());
        for(int i=0 ; i<m.numColumns() ; i++) {
            Vector col = Matrices.getColumn(m, i);
            double sum = 0;
            for (VectorEntry e : col) {
                sum += e.get();
            }
            v.set(i, sum);            
        }
        return v;
    }

    public static void main(String[] args) throws Exception {
        SparseVector va = new SparseVector(6, new int[] { 0, 1, 2, 3, 4, 5 }, new double[] { 1, 1, 1, 1, 1, 1 });
        SparseVector vb = new SparseVector(6, new int[] { 0, 1, 2 }, new double[] { 1, 1, 1 });
        SparseVector vc = new SparseVector(6, new int[] { 1, 2, 3, 4 }, new double[] { 1, 1, 1, 1 });
        SparseVector vd = new SparseVector(6, new int[] { 0, 1 }, new double[] { 1, 1 });
        SparseVector ve = new SparseVector(6, new int[] { 0, 1, 2, 3 }, new double[] { 1, 1, 1, 1 });
        FlexCompRowMatrix m = new FlexCompRowMatrix(5, 6);
        m.setRow(0, va);
        m.setRow(1, vb);
        m.setRow(2, vc);
        m.setRow(3, vd);
        m.setRow(4, ve);

        FlexCompRowMatrix trainM =
            new FlexCompRowMatrix(Matrices.getSubMatrix(m, new int[] { 0, 1, 2 }, new int[] { 0, 1, 2, 3, 4, 5 }));
        FlexCompRowMatrix testM =
            new FlexCompRowMatrix(Matrices.getSubMatrix(m, new int[] { 3, 4 }, new int[] { 0, 1, 2, 3, 4, 5 }));
        LabeledMatrix train = new LabeledMatrix(trainM, new String[] { "zero", "one", "two" },
            new String[] { "c1", "c2", "c3", "c4", "c5", "c6" });
        LabeledMatrix test = new LabeledMatrix(testM, new String[] { "three", "four" },
            new String[] { "c1", "c2", "c3", "c4", "c5", "c6" });

        MemoryUserBasedCollaborativeFilter ubcf = new MemoryUserBasedCollaborativeFilter(train, new CosineSimilarity(), 1);
        Map<String, String[]> recommendations = ubcf.generateRecommendations(test, 20, 5);
        for (String row : test.rowLabels) {
            System.out.print(row + ": ");
            for (String s : recommendations.get(row)) {
                System.out.print(s + " , ");
            }
            System.out.println("");
        }

    }
    @Override
    public double getLikeThreshold() {
        return likeThreshold;
    }

}
