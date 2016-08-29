package rl4j;

import java.util.Arrays;
import java.util.stream.IntStream;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class Utilities {
    public static Vector columnSums(Matrix m) {
        Vector v = new DenseVector(m.numColumns());
        for(int i=0 ; i<m.numColumns() ; i++) {
            Vector col = Matrices.getColumn(m, i);
            double sum = 0;
            for (VectorEntry e : col) {
                sum += e.get();
            }
            v.set(i, sum);            
        }
        return v;
    }
    public static FlexCompRowMatrix ratings(Matrix data, Matrix knnResults) {
        FlexCompRowMatrix ratings = new FlexCompRowMatrix(knnResults.numColumns(), data.numColumns());
        int[] colIndexes = IntStream.rangeClosed(0, data.numColumns()-1).toArray();
        for(int i=0 ; i<knnResults.numColumns() ; i++) {
            double[] rowIndexesD = Matrices.getArray(Matrices.getColumn(knnResults, i));
            int[] rowIndexes = Arrays.stream(rowIndexesD).mapToInt(e -> (int) e).toArray();
            Vector colSums = Utilities.columnSums(Matrices.getSubMatrix(data, rowIndexes, colIndexes));
            ratings.setRow(i, new SparseVector(colSums));
        }
        return ratings;
    }
    public static void removeKnownRatings(Matrix ratingMatrix, Matrix knownRatings, double threshold) {
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
}
