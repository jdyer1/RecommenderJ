package rl4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

public class CollaborativeFilterHelper {        
    
    public static class RecommendationsByExampleIndex {
        public final Map<Integer, int[]> items;
        public final Map<Integer, double[]> ratings;

        public RecommendationsByExampleIndex(Map<Integer, int[]> items, Map<Integer, double[]> ratings) {
            this.items = Collections.unmodifiableMap(items);
            this.ratings = Collections.unmodifiableMap(ratings);
        }
    }    
    
    public void removeKnownRatings(Matrix ratingMatrix, Matrix knownRatings, double threshold) {
        if(ratingMatrix == null || knownRatings==null) {
            return;
        }
        if(ratingMatrix.numRows()==0 || knownRatings.numRows()==0) {
            return;
        }            
        if(ratingMatrix.numRows() != knownRatings.numRows()) {
            throw new IllegalArgumentException("# ratings should be the same as # known ratings");
        }
        for (MatrixEntry e : knownRatings) {
            if(e.get()>=threshold) {
                ratingMatrix.set(e.row(), e.column(), 0);
            }
        }
    } 
    
    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, FlexCompRowMatrix ratingsMatrix, int numRecommendations, double likeThreshold) {
        removeKnownRatings(ratingsMatrix, testExamples.m, likeThreshold);
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

    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, FlexCompRowMatrix ratingsMatrix, int numRecommendations, double likeThreshold) {
        try {            
            removeKnownRatings(ratingsMatrix, testExamples.m, likeThreshold);
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

    public RecommendationsByExampleIndex recommendationsByExampleIndex(FlexCompRowMatrix ratingMatrix, int n,
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
}
