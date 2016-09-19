package rl4j;

public class CosineSimilarity extends Similarity {
    @Override
    public double similarity(double[] a, double[] b, double threshold) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("The two must be the same length");
        }
        int n_aIb = 0;
        int n_a = 0;
        int n_b = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >= threshold) {
                n_a++;
            }
            if (b[i] >= threshold) {
                n_b++;
            }
            if (a[i] >= threshold && b[i] >= threshold) {
                n_aIb++;
            }

        }
        double combinedNorms = Math.sqrt(n_a * n_b);
        return combinedNorms == 0 ? 0 : ((double) n_aIb / combinedNorms);
    }   
}
