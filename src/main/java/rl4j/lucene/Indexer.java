package rl4j.lucene;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;

public class Indexer extends LuceneAccessBase { 
    
    protected Indexer(String path, IndexedFieldType indexedFieldType) throws IOException {
        super(path, indexedFieldType);
    }

    public void importData(boolean[] data, String[] rowLabels, String[] colLabels, boolean overwrite)
        throws IOException {
        if (data == null || colLabels == null) {
            throw new IllegalArgumentException("both the data array and the column labels are required.");
        }
        if (rowLabels == null) {
            rowLabels = new String[data.length / colLabels.length];
            for (int i = 0; i < rowLabels.length; i++) {
                rowLabels[i] = "X" + (i + 1);
            }
        }
        IndexWriterConfig iwc = indexWriterConfig(overwrite);
        try (IndexWriter iw = new IndexWriter(dir, iwc)) {
            int k=0;
            for (int i = 0; i < rowLabels.length; i++) {
                Document doc = new Document();
                doc.add(new StringField("id", rowLabels[i], Store.YES));
                for (int j = 0; j < colLabels.length; j++) {
                    String fieldName = "x" + j;
                    if (data[k]) { 
                        doc.add(new StoredField(fieldName, "1"));
                        switch(indexedFieldType) {
                            case POINT:
                                doc.add(new IntPoint(fieldName, 1));
                                break;
                            case STRING:
                                doc.add(Rl4jField.IntAsStringField(fieldName, 1));
                                break;
                            case STRING_TERM_VECTORS:
                                doc.add(Rl4jField.IntAsStringTermVectorField(fieldName, 1));
                                break;
                        }                        
                    }
                    k++;
                }
                if(overwrite) {
                    iw.addDocument(doc);
                } else {
                    iw.updateDocument(new Term("id", rowLabels[i]), doc);
                }
            }
            iw.commit();
        } catch (IOException e) {
            throw e;
        }
    }
        
}
