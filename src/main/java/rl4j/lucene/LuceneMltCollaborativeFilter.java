package rl4j.lucene;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import rl4j.CollaborativeFilter;
import rl4j.CollaborativeFilterHelper;
import rl4j.LabeledMatrix;
import rl4j.TopNList;

public class LuceneMltCollaborativeFilter extends LuceneAccessBase implements CollaborativeFilter {
    private final double likeThreshold;

    static {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    public LuceneMltCollaborativeFilter(String path, double likeThreshold) throws IOException {
        super(path, IndexedFieldType.STRING_TERM_VECTORS);
        this.likeThreshold = likeThreshold;
    }

    private FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        try {
            Indexer indexer = new Indexer(path, indexedFieldType);
            indexer.importData(convertToBooleanArray(testExamples), testExamples.rowLabels, testExamples.colLabels,
                false);
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            String[] fieldNames = new String[testExamples.colLabels.length];
            for (int i = 0; i < fieldNames.length; i++) {
                fieldNames[i] = "x" + i;
            }
            MoreLikeThis mlt = new MoreLikeThis(searcher.getIndexReader());
            mlt.setMinWordLen(0);
            mlt.setMaxWordLen(Integer.MAX_VALUE);
            mlt.setMinTermFreq(1);
            mlt.setMinDocFreq(1);
            mlt.setMaxDocFreq(Integer.MAX_VALUE);
            mlt.setMaxQueryTerms(Integer.MAX_VALUE);
            mlt.setBoost(false);
            mlt.setFieldNames(fieldNames);
            FlexCompRowMatrix ratings = new FlexCompRowMatrix(testExamples.m.numRows(), testExamples.m.numColumns());
            for (int i = 0; i < testExamples.m.numRows(); i++) {
                SparseVector v = testExamples.m.getRow(i);
                if (v.getUsed() == 0) {
                    continue;
                }
                TopDocs td = searcher.search(new TermQuery(new Term("id", testExamples.rowLabel(i))), 1);
                if (td.totalHits == 1) {
                    Query q = mlt.like(td.scoreDocs[0].doc);
                    td = searcher.search(q, numNeighbors);
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
        Indexer indexer = new Indexer(path, IndexedFieldType.STRING_TERM_VECTORS);
        indexer.importData(trainB, train.rowLabels, train.colLabels, true);
        LuceneMltCollaborativeFilter lcf = new LuceneMltCollaborativeFilter(path, 1);
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
