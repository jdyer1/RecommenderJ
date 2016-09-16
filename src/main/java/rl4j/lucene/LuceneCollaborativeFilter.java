package rl4j.lucene;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import rl4j.LabeledMatrix;
import rl4j.UserBasedCollaborativeFilter;

public class LuceneCollaborativeFilter extends UserBasedCollaborativeFilter {
    private final Directory dir;
    
    static {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    public LuceneCollaborativeFilter(String path, double likeThreshold) throws IOException {
        super(null, likeThreshold);
        this.dir = FSDirectory.open(FileSystems.getDefault().getPath(path));
    }
    
    @Override
    protected FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        try {
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            FlexCompRowMatrix ratings = new FlexCompRowMatrix(testExamples.m.numRows(), testExamples.m.numColumns());
            for(int i=0 ; i< testExamples.m.numRows() ; i++) {
                SparseVector v = testExamples.m.getRow(i);
                if(v.getUsed()==0) {
                    continue;
                }
                BooleanQuery.Builder b = new BooleanQuery.Builder();
                b.setMinimumNumberShouldMatch(1);            
                for(VectorEntry ve : v) {
                    String colName = "X" + ve.index();
                    int val = (int) ve.get();
                    b.add(new BooleanClause(IntPoint.newExactQuery(colName, val), Occur.SHOULD));                
                }
                TopDocs td = searcher.search(b.build(), numNeighbors);
                for (ScoreDoc scoredoc : td.scoreDocs) {
                    Document doc = searcher.doc(scoredoc.doc);
                    for(IndexableField f : doc.getFields()) {
                        if(f.name().startsWith("X")) {
                            int index = Integer.parseInt(f.name().substring(1));
                            int value = f.numericValue().intValue();
                            ratings.add(i, index, value);
                        }
                    }
                }                    
            }            
            return ratings;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    } 
    public static void main(String[] args) throws Exception {
        SparseVector va = new SparseVector(6, new int[] { 0, 1, 2, 3, 4, 5 }, new double[] { 1, 1, 1, 1, 1, 1 });
        SparseVector vb = new SparseVector(6, new int[] { 0, 1, 2 }, new double[] { 1, 1, 1 });
        SparseVector vc = new SparseVector(6, new int[] { 1, 2, 3, 4 }, new double[] { 1, 1, 1, 1 });
        SparseVector vd = new SparseVector(6, new int[] { 0, 1 }, new double[] { 1, 1 });
        SparseVector ve = new SparseVector(6, new int[] { 0, 1, 2, 3 }, new double[] { 1, 1, 1, 1 });
        FlexCompRowMatrix m = new FlexCompRowMatrix(5, 6);
        m.setRow(0, va);
        m.setRow(1, vb);
        m.setRow(2, vc);
        m.setRow(3, vd);
        m.setRow(4, ve);

        FlexCompRowMatrix trainM =
            new FlexCompRowMatrix(Matrices.getSubMatrix(m, new int[] { 0, 1, 2 }, new int[] { 0, 1, 2, 3, 4, 5 }));
        FlexCompRowMatrix testM =
            new FlexCompRowMatrix(Matrices.getSubMatrix(m, new int[] { 3, 4 }, new int[] { 0, 1, 2, 3, 4, 5 }));
        LabeledMatrix train = new LabeledMatrix(trainM, new String[] { "zero", "one", "two" },
            new String[] { "c1", "c2", "c3", "c4", "c5", "c6" });
        LabeledMatrix test = new LabeledMatrix(testM, new String[] { "three", "four" },
            new String[] { "c1", "c2", "c3", "c4", "c5", "c6" });
        boolean[] trainB = new boolean[trainM.numColumns() * trainM.numRows()];
        int k=0;
        for(int i=0 ; i<trainM.numRows() ; i++) {
            for(int j=0 ; j<trainM.numColumns() ; j++) {
                if(trainM.get(i, j) >0) {
                    trainB[k] = true;
                }
                k++;
            }
        }
        String path = "/tmp/LuceneIndex";
        Indexer indexer = new Indexer(path);
        indexer.importData(trainB, train.rowLabels, train.colLabels, true);
        LuceneCollaborativeFilter lcf = new LuceneCollaborativeFilter(path, 1);
        Map<String, String[]> recommendations = lcf.generateRecommendations(test, 20, 5);
        for (String row : test.rowLabels) {
            System.out.print(row + ": ");
            for (String s : recommendations.get(row)) {
                System.out.print(s + " , ");
            }
            System.out.println("");
        }

    }
}
