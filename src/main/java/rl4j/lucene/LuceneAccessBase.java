package rl4j.lucene;

import java.io.IOException;
import java.nio.file.FileSystems;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import rl4j.LabeledMatrix;

public abstract class LuceneAccessBase {
    protected final String path;
    protected final Directory dir;
    protected final IndexedFieldType indexedFieldType;
    
    static {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }
    
    protected static final Similarity sim = new ConstantSimilarity();

    protected LuceneAccessBase(String path, IndexedFieldType indexedFieldType) throws IOException {
        this.path = path;
        this.dir = FSDirectory.open(FileSystems.getDefault().getPath(path));
        this.indexedFieldType = indexedFieldType;
    }

    protected IndexWriterConfig indexWriterConfig(boolean overwrite) {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(overwrite ? OpenMode.CREATE : OpenMode.CREATE_OR_APPEND);
        iwc.setSimilarity(sim);
        return iwc;
    }
    
    protected boolean[] convertToBooleanArray(LabeledMatrix lm) {
        boolean[] boolArray = new boolean[lm.m.numColumns() * lm.m.numRows()];
        int k = 0;
        for (int i = 0; i < lm.m.numRows(); i++) {
            for (int j = 0; j < lm.m.numColumns(); j++) {
                if (lm.m.get(i, j) > 0) {
                    boolArray[k] = true;
                }
                k++;
            }
        }
        return boolArray;
    }

}
