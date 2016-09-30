package rl4j;

import java.util.Map;

public interface CollaborativeFilter {
    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors,
        int numRecommendations);

    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations);
}
