package rl4j;

import java.util.Arrays;
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

    public MemoryUserBasedCollaborativeFilter(LabeledMatrix trainingExamples, Similarity sim, double likeThreshold) {
        this.trainingExamples = trainingExamples;
        this.sim = sim;
        this.likeThreshold = likeThreshold;
    }

    private FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        FlexCompRowMatrix simMatrix = sim.similarity(testExamples.m, trainingExamples.m, likeThreshold);
        KNNResults knn = KNearestNeighbor.knn(simMatrix, numNeighbors);
        FlexCompRowMatrix items = ratings(trainingExamples.m, knn.indexes);
        return items;
    }

    @Override
    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors,
        int numRecommendations) {
        return CollaborativeFilterHelper.generateRecommendations(testExamples,
            ratingsMatrix(testExamples, numNeighbors), numRecommendations, likeThreshold);
    }

    @Override
    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        return CollaborativeFilterHelper.recommendationsAsTopNList(testExamples,
            ratingsMatrix(testExamples, numNeighbors), numRecommendations, likeThreshold);
    }

    private FlexCompRowMatrix ratings(Matrix data, Matrix knnResults) {
        FlexCompRowMatrix ratings = new FlexCompRowMatrix(knnResults.numColumns(), data.numColumns());
        int[] colIndexes = IntStream.rangeClosed(0, data.numColumns() - 1).toArray();
        for (int i = 0; i < knnResults.numColumns(); i++) {
            double[] rowIndexesD = Matrices.getArray(Matrices.getColumn(knnResults, i));
            int[] rowIndexes = Arrays.stream(rowIndexesD).mapToInt(e -> (int) e).toArray();
            Vector colSums = columnSums(Matrices.getSubMatrix(data, rowIndexes, colIndexes));
            ratings.setRow(i, new SparseVector(colSums));
        }
        return ratings;
    }

    private Vector columnSums(Matrix m) {
        Vector v = new DenseVector(m.numColumns());
        for (int i = 0; i < m.numColumns(); i++) {
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
        SparseVector[] v = new SparseVector[10];
        v[0] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 1, 1, 0, 0 });
        v[1] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 1, 1, 0, 0 });
        v[2] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 1, 1, 0, 0 });
        v[3] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 1, 1, 0, 0 });
        v[4] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 0, 0, 1, 1 });
        v[5] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 0, 0, 1, 1 });
        v[6] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 0, 0, 1, 1 });
        v[7] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 0, 0, 1, 1 });
        v[8] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 1, 0, 0, 0 });
        v[9] = new SparseVector(4, new int[] { 0, 1, 2, 3 }, new double[] { 0, 0, 0, 1 });

        FlexCompRowMatrix trainM = new FlexCompRowMatrix(8, 4);
        FlexCompRowMatrix testM = new FlexCompRowMatrix(2, 4);
        for (int i = 0; i < v.length; i++) {
            if (i < 8) {
                trainM.setRow(i, v[i]);
            }
        }
        testM.setRow(0, v[8]);
        testM.setRow(1, v[9]);

        LabeledMatrix train =
            new LabeledMatrix(trainM, new String[] { "one", "two", "three", "four", "five", "six", "seven", "eight" },
                new String[] { "c0", "c1", "c2", "c3" });
        LabeledMatrix test =
            new LabeledMatrix(testM, new String[] { "nine", "ten" }, new String[] { "c0", "c1", "c2", "c3" });

        MemoryUserBasedCollaborativeFilter ubcf =
            new MemoryUserBasedCollaborativeFilter(train, new CosineSimilarity(), 1);
        Map<String, String[]> recommendations = ubcf.generateRecommendations(test, 5, 1);
        for (String row : test.rowLabels) {
            System.out.print(row + ": ");
            for (String s : recommendations.get(row)) {
                System.out.print(s + " , ");
            }
            System.out.println("");
        }

    }

}
