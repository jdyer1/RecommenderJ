package rl4j.lucene;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

public class ConstantSimilarity extends TFIDFSimilarity {

    @Override
    public float coord(int overlap, int maxOverlap) {
        return 1;
    }

    @Override
    public float queryNorm(float sumOfSquaredWeights) {
        return 1;
    }

    @Override
    public float tf(float freq) {
        return freq;
    }

    @Override
    public float idf(long docFreq, long docCount) {
        return 1;
    }

    @Override
    public float lengthNorm(FieldInvertState state) {
        return 1;
    }

    @Override
    public float decodeNormValue(long norm) {
        return norm;
    }

    @Override
    public long encodeNormValue(float f) {
        return (long) f;
    }

    @Override
    public float sloppyFreq(int distance) {
        return 1;
    }

    @Override
    public float scorePayload(int doc, int start, int end, BytesRef payload) {
        return 1;
    }

}
