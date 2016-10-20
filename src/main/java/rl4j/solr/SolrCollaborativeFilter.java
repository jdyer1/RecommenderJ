package rl4j.solr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import rl4j.CollaborativeFilter;
import rl4j.CollaborativeFilterHelper;
import rl4j.LabeledMatrix;
import rl4j.TopNList;

public class SolrCollaborativeFilter implements CollaborativeFilter {
    private final SolrClient solrClient;
    private final String idFieldname;
    private final String dataFieldname;
    private final double likeThreshold;

    public SolrCollaborativeFilter(String hosts, boolean cloud, String collectionName, String idFieldname,
        String dataFieldname, double likeThreshold) {
        this.idFieldname = idFieldname;
        this.dataFieldname = dataFieldname;
        String[] hostArray = hosts.split("\\s*,\\s*");
        if (cloud) {
            CloudSolrClient.Builder b = new CloudSolrClient.Builder();
            for (String host : hostArray) {
                b.withZkHost(host);
            }            
            CloudSolrClient csc = b.build();
            csc.setDefaultCollection(collectionName);
            this.solrClient = csc;
        } else if (hostArray.length == 1) {
            this.solrClient =
                new HttpSolrClient.Builder(hostArray[0] + (hostArray[0].endsWith("/") ? "" : "/") + collectionName)
                    .build();
        } else {
            LBHttpSolrClient.Builder b = new LBHttpSolrClient.Builder();
            for (String host : hostArray) {
                b.withBaseSolrUrl(host + (host.endsWith("/") ? "" : "/") + collectionName);
            }
            this.solrClient = b.build();
        }
        this.likeThreshold = likeThreshold;
    }

    public void indexTrainingData(boolean[] data, String[] rowLabels, String[] colLabels, boolean deleteAll) {
        if (data == null || colLabels == null) {
            throw new IllegalArgumentException("both the data array and the column labels are required.");
        }
        if (rowLabels == null) {
            rowLabels = new String[data.length / colLabels.length];
            for (int i = 0; i < rowLabels.length; i++) {
                rowLabels[i] = "X" + (i + 1);
            }
        }
        try {
            if (deleteAll) {
                solrClient.deleteByQuery("*:*", 0);
            }
            int k = 0;
            for (int i = 0; i < rowLabels.length; i++) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField(idFieldname, rowLabels[i]);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < colLabels.length; j++) {
                    String fieldName = "x" + j;
                    if (data[k]) {
                        sb.append(sb.length() == 0 ? "" : " ");
                        sb.append(fieldName);
                    }
                    k++;
                }
                doc.addField(dataFieldname, sb.toString());
                solrClient.add(doc);
            }
            solrClient.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    
    public void indexTrainingData(BufferedReader r, boolean deleteAll) {        
        try {
            if (deleteAll) {
                solrClient.deleteByQuery("*:*", 0);
            }
            Pattern commaPattern = Pattern.compile("[,]");
            Pattern quotePattern = Pattern.compile("[\"]");
            String a = r.readLine();
            a = quotePattern.matcher(a).replaceAll("");
            String[] colLabels = commaPattern.split(a);
            while ((a=r.readLine())!=null) {
                a = quotePattern.matcher(a).replaceAll("");
                String[] tokens = commaPattern.split(a);
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField(idFieldname, tokens[0]);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < colLabels.length; j++) {
                    if ("1".equals(tokens[j+1])) {
                        sb.append(sb.length() == 0 ? "" : " ");
                        sb.append("x" + colLabels[j]);
                    }
                }
                doc.addField(dataFieldname, sb.toString());
                solrClient.add(doc);
            }
            solrClient.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private FlexCompRowMatrix ratingsMatrix(LabeledMatrix testExamples, int numNeighbors) {
        try {
            FlexCompRowMatrix ratings = new FlexCompRowMatrix(testExamples.m.numRows(), testExamples.m.numColumns());
            for (int i = 0; i < testExamples.m.numRows(); i++) {
                SparseVector v = testExamples.m.getRow(i);
                if (v.getUsed() == 0) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (VectorEntry ve : v) {
                    String colName = "x" + ve.index();
                    int val = (int) ve.get();
                    if (val >= likeThreshold) {
                        sb.append(sb.length() == 0 ? "" : " ");
                        sb.append(colName);
                    }
                }
                if (sb.length() > 0) {
                    SolrQuery query = new SolrQuery();
                    query.set("defType", "dismax");
                    query.set("mm", "1");
                    query.set("qf", dataFieldname);
                    query.set("q", sb.toString());
                    query.set("rows", "" + numNeighbors);
                    QueryResponse res = solrClient.query(query);
                    for (SolrDocument doc : res.getResults()) {
                        String data = (String) doc.getFirstValue(dataFieldname);
                        for (String s : data.split(" ")) {
                            int index = Integer.parseInt(s.substring(1));
                            ratings.add(i, index, 1);
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
        if (args.length < 5) {
            System.out.println("[host] [collection] [idfieldname] [datafieldname] [file]");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(args[4]))) {
            SolrCollaborativeFilter scf = new SolrCollaborativeFilter(args[0], true, args[1], args[2], args[3], 1.0);
            scf.indexTrainingData(br, true);
        }
        
        /*SparseVector va = new SparseVector(6, new int[] { 0, 1, 2, 3, 4, 5 }, new double[] { 1, 1, 1, 1, 1, 1 });
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
        SolrCollaborativeFilter scf = new SolrCollaborativeFilter(args[0], true, args[1], args[2], args[3], 1.0);
        scf.indexTrainingData(trainB, train.rowLabels, train.colLabels, true);
        Map<String, String[]> recommendations = scf.generateRecommendations(test, 20, 5);
        for (String row : test.rowLabels) {
            System.out.print(row + ": ");
            for (String s : recommendations.get(row)) {
                System.out.print(s + " , ");
            }
            System.out.println("");
        }*/

    }
}
