package rl4j.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
    private final String path;
    private final Directory dir;

    public Indexer(String path) throws IOException {
        this.dir = FSDirectory.open(FileSystems.getDefault().getPath(new File(path).getParent(), "LuceneIndex"));
        this.path = path;
    }

    public void reindexFile(boolean overwrite) throws IOException {
        IndexWriterConfig iwc = indexWriterConfig(overwrite);
        String[] colLabels;
        try (BufferedReader br = new BufferedReader(new FileReader(path)); IndexWriter iw = new IndexWriter(dir, iwc)) {
            Pattern commaPattern = Pattern.compile(",");
            Pattern quotePattern = Pattern.compile("[\"]");
            colLabels = commaPattern.split(quotePattern.matcher(br.readLine()).replaceAll(""));
            String a;
            while ((a = br.readLine()) != null) {
                String[] tokens = commaPattern.split(a);
                String id = quotePattern.matcher(tokens[0]).replaceAll("");
                Document doc = new Document();
                doc.add(new StringField("id", id, Store.YES));
                for (int j = 1; j < tokens.length; j++) {
                    String fieldName = colLabels[j - 1];
                    int val = Integer.parseInt(tokens[j]);
                    if (val != 0) {
                        doc.add(new IntPoint(fieldName, val));
                        doc.add(new StoredField(fieldName, val));
                    }
                }
                if(overwrite) {
                    iw.addDocument(doc);
                } else {
                    iw.updateDocument(new Term("id", id), doc);
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }

    private IndexWriterConfig indexWriterConfig(boolean overwrite) {
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(overwrite ? OpenMode.CREATE : OpenMode.CREATE_OR_APPEND);
        return iwc;
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
                    String fieldName = "X" + j;
                    if (data[k]) {
                        doc.add(new IntPoint(fieldName, 1));
                        doc.add(new StoredField(fieldName, 1));
                    }                    
                    k++;
                }
                if(overwrite) {
                    iw.addDocument(doc);
                } else {
                    iw.updateDocument(new Term("id", rowLabels[i]), doc);
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        String path = "/home/jdyer1/Desktop/bwm.txt";
        Indexer indexer = new Indexer(path);
        indexer.reindexFile(true);
        DirectoryReader reader = DirectoryReader.open(indexer.dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        int chunkSize = 1000;
        Query q = new MatchAllDocsQuery();
        TopDocs td = searcher.search(q, chunkSize);
        int hits = td.totalHits;
        int seen = 0;
        ScoreDoc lastDoc = null;
        while (seen < hits) {
            seen += td.scoreDocs.length;
            for (ScoreDoc scoredoc : td.scoreDocs) {
                Document doc = searcher.doc(scoredoc.doc);
                String docStr = doc.toString();
                docStr = docStr.length() > 300 ? docStr.substring(0, 300) + "..." : docStr;
                System.out.println(scoredoc.doc + " | " + scoredoc.score + " | " + docStr);
                lastDoc = scoredoc;
            }
            if (seen < hits) {
                td = searcher.searchAfter(lastDoc, q, chunkSize);
            }
        }
    }

}
