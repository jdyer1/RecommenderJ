package rl4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.regex.Pattern;

import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class FileLoader {
    
    public LabeledMatrix importData(boolean[] data, String[] rowLabels, String[] colLabels) {
        if(data==null || colLabels == null) {
           throw new IllegalArgumentException("both the data array and the column labels are required.");
        }
        if(rowLabels==null) {
            rowLabels = new String[data.length / colLabels.length];
            for(int i=0 ; i<rowLabels.length ; i++) {
                rowLabels[i] = "X" + (i+1);
            }
        }
        FlexCompRowMatrix m = new FlexCompRowMatrix(rowLabels.length, colLabels.length);
        int k=0;
        for(int i=0 ; i < rowLabels.length ; i++) {
            for(int j=0 ; j < colLabels.length ; j++) {
                m.set(i, j, data[k] ? 1 : 0);
                k++;
            }            
        }
        LabeledMatrix lm = new LabeledMatrix(m, rowLabels, colLabels);
        return lm;
    }
    
    
    public LabeledMatrix loadFile(String path) throws IOException {
        int numRows = (int) Files.lines(Paths.get(path)).count() - 1;
        FlexCompRowMatrix m;
        String[] colLabels;
        String[] rowLabels;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            Pattern commaPattern = Pattern.compile(",");
            Pattern quotePattern = Pattern.compile("[\"]");
            colLabels = commaPattern.split(quotePattern.matcher(br.readLine()).replaceAll(""));
            rowLabels = new String[numRows];
            m = new FlexCompRowMatrix(numRows, colLabels.length);
            String a;
            int row=0;
            while ((a = br.readLine()) != null) {
                String[] tokens = commaPattern.split(a);
                rowLabels[row] = quotePattern.matcher(tokens[0]).replaceAll("");
                for (int j = 1; j < tokens.length; j++) {
                    int col = j-1;
                    m.add(row, col, new BigDecimal(tokens[j]).doubleValue());
                }
                row++;
            }
        } catch (IOException e) {
            throw e;
        }
        return new LabeledMatrix(m, rowLabels, colLabels);
    }
    
    public static class TrainAndTest {
        public final LabeledMatrix train;
        public final LabeledMatrix test;
        
        public TrainAndTest(LabeledMatrix train, LabeledMatrix test) {
            this.train = train;
            this.test = test;
        }
        
        public TrainAndTest(LabeledMatrix lm, double percentTrain) {
            int minTrain = (int) (percentTrain * 100);
            int[] rand = new int[lm.rowLabels.length];
            int numTrain = 0;
            int numTest = 0;
            Random r = new Random();
            for(int i=0 ; i<rand.length ; i++) {
                rand[i] = r.nextInt(100) + 1;
                if(rand[i]>minTrain) {
                    numTest++;
                } else {
                    numTrain++;
                }
            }
            FlexCompRowMatrix _train = new FlexCompRowMatrix(numTrain, lm.colLabels.length);
            FlexCompRowMatrix _test = new FlexCompRowMatrix(numTest, lm.colLabels.length);
            String[] trainRowLabels = new String[numTrain];
            String[] testRowLabels = new String[numTest];
            int trainRow = 0;
            int testRow = 0;
            for(int i=0 ; i<rand.length ; i++) {
                SparseVector v = lm.m.getRow(i);
                if(rand[i]>minTrain) {
                    _test.setRow(testRow, v);
                    testRowLabels[testRow] = lm.rowLabel(i);
                    testRow++;
                } else {
                    _train.setRow(trainRow, v);
                    trainRowLabels[trainRow] = lm.rowLabel(i);
                    trainRow++;
                }
            }
            this.train = new LabeledMatrix(_train, trainRowLabels, lm.colLabels);
            this.test = new LabeledMatrix(_test, testRowLabels, lm.colLabels);
        }
    }
}
