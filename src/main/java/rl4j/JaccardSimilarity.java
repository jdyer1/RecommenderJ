package rl4j;

public class JaccardSimilarity extends Similarity {
    @Override
    public double similarity(double[] a, double[] b, double threshold) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("The two must be the same length");
        }
        int aIb = 0;
        int aUb = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >= threshold || b[i] >= threshold) {
                aUb++;
                if (a[i] >= threshold && b[i] >= threshold) {
                    aIb++;
                }
            }
        }
        return aUb == 0 ? 0 : ((double) aIb / aUb);
    }
}
