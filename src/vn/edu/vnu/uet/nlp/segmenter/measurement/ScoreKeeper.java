package vn.edu.vnu.uet.nlp.segmenter.measurement;

/**
 * Created by Eliana Vornov on 3/15/18.
 */
public class ScoreKeeper {
    private int N1; // number of words recognized by the system
    private int N2; // number of words in the manually segmented text
    private int N3; // number of right segmented words.

    public ScoreKeeper() {
        this(0, 0, 0);
    }

    public ScoreKeeper(int one, int two, int three) {
        N1 = one;
        N2 = two;
        N3 = three;
    }

    public void incrementN1() {
        N1++;
    }

    public void incrementN2() {
        N2++;
    }

    public void incrementN3() {
        N3++;
    }

    public int getN1() {
        return N1;
    }
    public int getN2() {
        return N2;
    }
    public int getN3() {
        return N3;
    }
}
