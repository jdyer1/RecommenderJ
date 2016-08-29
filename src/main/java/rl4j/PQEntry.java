package rl4j;

public class PQEntry implements Comparable<PQEntry> {
    final double v;
    final int i;

    PQEntry(double v, int i) {
        this.v = v;
        this.i = i;
    }

    @Override
    public int compareTo(PQEntry that) {
        double comp = that.v - this.v;
        return comp < -.0001 ? 1 : comp > .0001 ? -1 : 0;
    }

}