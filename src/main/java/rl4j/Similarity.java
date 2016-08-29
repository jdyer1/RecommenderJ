package rl4j;

import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

public interface Similarity {
    public double similarity(double[] a, double[] b, double threshold) ;
    
    public FlexCompRowMatrix similarity(FlexCompRowMatrix a, double threshold) ;
    
    public FlexCompRowMatrix similarity(FlexCompRowMatrix a, FlexCompRowMatrix cross, double threshold) ;
    

}
