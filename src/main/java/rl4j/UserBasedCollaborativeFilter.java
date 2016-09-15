package rl4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import rl4j.KNearestNeighbor.KNNResults;

public class UserBasedCollaborativeFilter {
    private final LabeledMatrix trainingExamples;
    private final double likeThreshold;
    private final Similarity sim = new JaccardSimilarity();

    public UserBasedCollaborativeFilter(LabeledMatrix trainingExamples, double likeThreshold) {
        this.trainingExamples = trainingExamples;
        this.likeThreshold = likeThreshold;
    }

    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        FlexCompRowMatrix ratingsMatrix = ratingsMatrix(testExamples, numNeighbors);
        Utilities.removeKnownRatings(ratingsMatrix, testExamples.m, likeThreshold);
        RecommendationsByExampleIndex rawRecommendations =
            recommendationsByExampleIndex(ratingsMatrix, numRecommendations, 0);
        Map<String, String[]> recommendations = new HashMap<>();
        for (Map.Entry<Integer, int[]> entry : rawRecommendations.items.entrySet()) {
            String[] rowRecommendations = new String[entry.getValue().length];
            for (int i = 0; i < entry.getValue().length; i++) {
                rowRecommendations[i] = "" + testExamples.colLabel(entry.getValue()[i]);
            }
            recommendations.put(testExamples.rowLabel(entry.getKey()), rowRecommendations);
        }
        return recommendations;
    }

    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        try {
            FlexCompRowMatrix ratingsMatrix = ratingsMatrix(testExamples, numNeighbors);
            Utilities.removeKnownRatings(ratingsMatrix, testExamples.m, likeThreshold);
            RecommendationsByExampleIndex rawRecommendations =
                recommendationsByExampleIndex(ratingsMatrix, numRecommendations, 1);
            List<int[]> items = new ArrayList<>(rawRecommendations.items.size());
            List<double[]> ratings = new ArrayList<>(rawRecommendations.items.size());
            for (Map.Entry<Integer, int[]> entry : rawRecommendations.items.entrySet()) {
                items.add(entry.getValue());
                ratings.add(rawRecommendations.ratings.get(entry.getKey()));
            }
            String[] itemLabels = testExamples.colLabels;
            return new TopNList(items, ratings, itemLabels, numRecommendations);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static RecommendationsByExampleIndex recommendationsByExampleIndex(FlexCompRowMatrix ratingMatrix, int n,
        int offset) {
        Map<Integer, int[]> items = new HashMap<>();
        Map<Integer, double[]> ratings = new HashMap<>();
        for (int i = 0; i < ratingMatrix.numRows(); i++) {
            Vector v = ratingMatrix.getRow(i);
            PriorityQueue<PQEntry> pq = new PriorityQueue<>(n + 1);
            for (VectorEntry ve : v) {
                double val = ve.get();
                if (val > 0) {
                    pq.add(new PQEntry(val, ve.index()));
                }
                if (pq.size() > n) {
                    pq.poll();
                }
            }
            int[] rowRecommendationItems = new int[pq.size()];
            double[] rowRecommendationRatings = new double[pq.size()];
            int ii = pq.size() - 1;
            PQEntry pqe;
            while ((pqe = pq.poll()) != null) {
                rowRecommendationRatings[ii] = pqe.v;
                rowRecommendationItems[ii--] = pqe.i + offset;
            }
            items.put(i, rowRecommendationItems);
            ratings.put(i, rowRecommendationRatings);
        }
        return new RecommendationsByExampleIndex(items, ratings);
    }

    public static class RecommendationsByExampleIndex {
        public final Map<Integer, int[]> items;
        public final Map<Integer, double[]> ratings;

        public RecommendationsByExampleIndex(Map<Integer, int[]> items, Map<Integer, double[]> ratings) {
            this.items = Collections.unmodifiableMap(items);
            this.ratings = Collections.unmodifiableMap(ratings);
        }
    }

    protected FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        FlexCompRowMatrix simMatrix = sim.similarity(testExamples.m, trainingExamples.m, likeThreshold);
        KNNResults knn = KNearestNeighbor.knn(simMatrix, numNeighbors);
        FlexCompRowMatrix items = Utilities.ratings(trainingExamples.m, knn.indexes);
        return items;
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

        UserBasedCollaborativeFilter ubcf = new UserBasedCollaborativeFilter(train, 1);
        Map<String, String[]> recommendations = ubcf.generateRecommendations(test, 20, 5);
        for (String row : test.rowLabels) {
            System.out.print(row + ": ");
            for (String s : recommendations.get(row)) {
                System.out.print(s + " , ");
            }
            System.out.println("");
        }

    }

}
