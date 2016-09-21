package rl4j.lucene;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

public class Rl4jField {
    public static final FieldType IAST = new FieldType();
    public static final FieldType IAST_TV = new FieldType();

    static {      
        IAST.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        IAST.setTokenized(false);
        IAST.setStored(false);
        IAST.setOmitNorms(true);
        IAST.setStoreTermVectors(false);
        IAST.freeze();
      
        IAST_TV.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        IAST_TV.setTokenized(false);
        IAST_TV.setStored(false);
        IAST_TV.setOmitNorms(true);
        IAST_TV.setStoreTermVectors(true);
        IAST_TV.freeze();
    }
    private Rl4jField() { }    
    
    public static Field IntAsStringField(String name, int val) {
        return new Field(name, "" + val, IAST);
    }
    public static Field IntAsStringTermVectorField(String name, int val) {
        return new Field(name, "" + val, IAST_TV);
    }
    
}
