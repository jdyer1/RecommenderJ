package rl4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class LabeledMatrix {
    public final FlexCompRowMatrix m;
    public final String[] rowLabels;
    public final String[] colLabels;

    public LabeledMatrix(Matrix data, String[] rowLabels, String[] colLabels) {
        if(data instanceof FlexCompRowMatrix) {
            this.m = (FlexCompRowMatrix) data;
        } else {
            this.m = new FlexCompRowMatrix(data);
        }
        this.rowLabels = rowLabels;
        this.colLabels = colLabels;
        if (m.numRows() != rowLabels.length) {
            throw new IllegalArgumentException(
                "Matrix has " + m.numRows() + " rows but there are " + rowLabels.length + " row labels.");
        }
        if (m.numColumns() != colLabels.length) {
            throw new IllegalArgumentException(
                "Matrix has " + m.numColumns() + " columns but there are " + colLabels.length + " column labels.");
        }
    }

    public String rowLabel(int index) {
        return rowLabels[index];
    }

    public String colLabel(int index) {
        return colLabels[index];
    }
    
    public void save(String path) {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.ROOT);
        dfs.setInfinity("Inf");
        dfs.setNaN("NaN");
        DecimalFormat df = new DecimalFormat("#.##", dfs);
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            for(int i=0; i<colLabels.length ; i++) {
                String cl = colLabels[i];
                if(i>0) {
                    bw.write(",");
                }
                bw.write("\"");
                bw.write(cl);  
                bw.write("\"");             
            }
            bw.write("\n");
            for(int i=0 ; i<m.numRows() ; i++) {
                String rl = rowLabels[i];
                bw.write("\"");
                bw.write(rl);
                bw.write("\"");
                SparseVector sv = m.getRow(i);
                for(int j=0 ; j<sv.size() ; j++) {                    
                    String rv = df.format(sv.get(j));
                    bw.write(",");
                    bw.write(rv); 
                }
                bw.write("\n");
            }
            
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.ROOT);
        dfs.setInfinity("Inf");
        dfs.setNaN("NaN");
        DecimalFormat df = new DecimalFormat("#.##", dfs);
        sb.append(rowLabels.length).append(" x ").append(colLabels.length).append("\n\n");
        sb.append("           ");
        
        for(int i=0; i<colLabels.length ; i++) {
            if(i>15) {
                sb.append("...etc...");
                break;
            }
            String cl = colLabels[i];
            cl = cl + "        ";
            sb.append(cl.substring(0, 8)).append(" ");
            
        }
        sb.append("\n");
        for(int i=0 ; i<m.numRows() ; i++) {
            if(i>100) {
                sb.append("...etc...\n");
                break;
            }
            String rl = rowLabels[i] + "          ";
            sb.append(rl.substring(0, 10)).append(" ");
            SparseVector sv = m.getRow(i);
            for(int j=0 ; j<sv.size() ; j++) {
                if(j>15) {
                    sb.append("...etc...");
                    break;
                }
                String rv = df.format(sv.get(j)) + "        ";
                sb.append(rv.substring(0, 8)).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();        
    }
}
