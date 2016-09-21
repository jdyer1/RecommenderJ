package rl4j;

import java.util.Map;

import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

public interface CollaborativeFilter {
    public FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors);

    public double getLikeThreshold();

    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors,
        int numRecommendations);

    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations);
}
