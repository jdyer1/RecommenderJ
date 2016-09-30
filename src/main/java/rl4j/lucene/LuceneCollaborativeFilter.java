package rl4j.lucene;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import rl4j.CollaborativeFilter;
import rl4j.CollaborativeFilterHelper;
import rl4j.LabeledMatrix;
import rl4j.TopNList;

public class LuceneCollaborativeFilter extends LuceneAccessBase implements CollaborativeFilter {
    private final double likeThreshold;

    public LuceneCollaborativeFilter(String path, double likeThreshold, IndexedFieldType indexedFieldType)
        throws IOException {
        super(path, indexedFieldType);
        this.likeThreshold = likeThreshold;
    }

    private FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        try {
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(Indexer.sim);
            FlexCompRowMatrix ratings = new FlexCompRowMatrix(testExamples.m.numRows(), testExamples.m.numColumns());
            for (int i = 0; i < testExamples.m.numRows(); i++) {
                SparseVector v = testExamples.m.getRow(i);
                if (v.getUsed() == 0) {
                    continue;
                }
                BooleanQuery.Builder b = new BooleanQuery.Builder();
                b.setMinimumNumberShouldMatch(1);
                for (VectorEntry ve : v) {
                    String colName = "x" + ve.index();
                    int val = (int) ve.get();
                    switch (indexedFieldType) {
                        case POINT:
                            b.add(new BooleanClause(IntPoint.newExactQuery(colName, val), Occur.SHOULD));
                            break;
                        case STRING:
                            b.add(new BooleanClause(new TermQuery(new Term(colName, "" + val)), Occur.SHOULD));
                            break;
                        case STRING_TERM_VECTORS:
                            b.add(new BooleanClause(new TermQuery(new Term(colName, "" + val)), Occur.SHOULD));
                            break;
                    }

                }
                Query q = b.build();
                TopDocs td = searcher.search(q, numNeighbors);
                for (ScoreDoc scoredoc : td.scoreDocs) {
                    Document doc = searcher.doc(scoredoc.doc);
                    for (IndexableField f : doc.getFields()) {
                        if (f.name().startsWith("x")) {
                            int index = Integer.parseInt(f.name().substring(1));
                            int value = Integer.parseInt(f.stringValue());
                            ratings.add(i, index, value);
                        }
                    }
                }
            }
            return ratings;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String[]> generateRecommendations(LabeledMatrix testExamples, int numNeighbors,
        int numRecommendations) {
        return CollaborativeFilterHelper.generateRecommendations(testExamples,
            ratingsMatrix(testExamples, numNeighbors), numRecommendations, likeThreshold);
    }

    @Override
    public TopNList recommendationsAsTopNList(LabeledMatrix testExamples, int numNeighbors, int numRecommendations) {
        return CollaborativeFilterHelper.recommendationsAsTopNList(testExamples,
            ratingsMatrix(testExamples, numNeighbors), numRecommendations, likeThreshold);
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
        int k = 0;
        for (int i = 0; i < trainM.numRows(); i++) {
            for (int j = 0; j < trainM.numColumns(); j++) {
                if (trainM.get(i, j) > 0) {
                    trainB[k] = true;
                }
                k++;
            }
        }
        String path = "/tmp/LuceneIndex";
        Indexer indexer = new Indexer(path, IndexedFieldType.STRING);
        indexer.importData(trainB, train.rowLabels, train.colLabels, true);
        LuceneCollaborativeFilter lcf = new LuceneCollaborativeFilter(path, 1.0, IndexedFieldType.STRING);
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
