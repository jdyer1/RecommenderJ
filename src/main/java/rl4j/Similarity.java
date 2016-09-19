package rl4j;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

public abstract class Similarity {
    public abstract double similarity(double[] a, double[] b, double threshold) ;
    
    public FlexCompRowMatrix similarity(FlexCompRowMatrix a, double threshold) {
        if (a.numRows() < 2) {
            return new FlexCompRowMatrix(0, 0);
        }
        FlexCompRowMatrix simMatrix = new FlexCompRowMatrix(a.numRows(), a.numRows());
        for (int i = 0; i < a.numRows(); i++) {
            for (int j = i; j < a.numRows(); j++) {
                double[] v1 = Matrices.getArray(a.getRow(i));
                double[] v2 = Matrices.getArray(a.getRow(j));
                double sim = similarity(v1, v2, threshold);
                simMatrix.set(j, i, sim);
                simMatrix.set(i, j, sim);
            }
        }
        return simMatrix;
    }
    
    public FlexCompRowMatrix similarity(FlexCompRowMatrix a, FlexCompRowMatrix cross, double threshold) {
        if(a.numRows() == 0  && cross.numRows() == 0) {
            return new FlexCompRowMatrix(0, 0);
        }
        if(a.numRows() == 0) {
            return similarity(cross, threshold);
        }
        if(cross.numRows() == 0) {
            return similarity(a, threshold);
        }
        FlexCompRowMatrix simMatrix = new FlexCompRowMatrix(a.numRows(), cross.numRows());
        for (int i = 0; i < a.numRows(); i++) {
            for (int j = 0; j < cross.numRows(); j++) {
                double[] v1 = Matrices.getArray(a.getRow(i));
                double[] v2 = Matrices.getArray(cross.getRow(j));
                double sim = similarity(v1, v2, threshold);
                simMatrix.set(i, j, sim);
            }
        }
        return simMatrix;
    }
    

}
