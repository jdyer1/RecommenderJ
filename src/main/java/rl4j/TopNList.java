package rl4j;

import java.util.List;

public class TopNList {
    public final int[] items;
    public final double[] ratings;
    public final String[] itemLabels;
    public final int n;
    
    public TopNList(List<int[]> items, List<double[]> ratings, String[] itemLabels, int n) {
        this.items = new int[items.size() * n];
        this.ratings = new double[this.items.length];
        int i=0;
        for(int[] itemArr : items) {
            itemArr = sizeArray(itemArr,n);
            double[] ratingArr = sizeArray(ratings.get(i), n);
            System.arraycopy(itemArr, 0, this.items, (n*i), n);    
            System.arraycopy(ratingArr, 0, this.ratings, (n*i), n);
            i++;
        }
        this.itemLabels = itemLabels;
        this.n = n;
    }
    private int[] sizeArray(int[] a, int n) {
        if(a.length==n) {
            return a;
        }
        int[] a1 = new int[n];
        if(a.length>n) {
            System.arraycopy(a, 0, a1, 0, n);
        } else {
            System.arraycopy(a, 0, a1, 0, a.length);
            for(int i=a.length ; i<n ; i++) {
                a1[i] = 0;
            }
        }
        return a1;
    }
    private double[] sizeArray(double[] a, int n) {
        if(a.length==n) {
            return a;
        }
        double[] a1 = new double[n];
        if(a.length>n) {
            System.arraycopy(a, 0, a1, 0, n);
        } else {
            System.arraycopy(a, 0, a1, 0, a.length);
            for(int i=a.length ; i<n ; i++) {
                a1[i] = 0;
            }
        }
        return a1;
    }

}
