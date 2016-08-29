package rl4j;

import java.util.PriorityQueue;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

public class KNearestNeighbor {
    public static KNNResults knn(FlexCompRowMatrix sim, int k) {
        KNNResults nn = null;
        int rownum = 0;
        for (double[] row : Matrices.getArray(sim)) {
            PriorityQueue<PQEntry> pq = new PriorityQueue<>(k + 1);
            for (int i = 0; i < row.length; i++) {
                pq.add(new PQEntry(row[i], i));
                if (pq.size() > k) {
                    pq.poll();                    
                }
            }
            if (nn == null) {
                nn = new KNNResults(pq.size(), sim.numRows());
            }
            int ii = pq.size() - 1;
            PQEntry pqe;
            while ((pqe = pq.poll()) != null) {                
                nn.values.set(ii, rownum, pqe.v);
                nn.indexes.set(ii--, rownum, pqe.i);
            }
            rownum++;
        }
        return nn == null ? new KNNResults(0,0) : nn;
    }

    public static class KNNResults {
        public final Matrix values;
        public final Matrix indexes;

        public KNNResults(int r, int c) {
            values = new DenseMatrix(r, c);
            indexes = new DenseMatrix(r, c);
        }
    }

}
