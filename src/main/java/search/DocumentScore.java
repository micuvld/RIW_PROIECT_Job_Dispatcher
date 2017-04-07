package search;

/**
 * Created by vlad on 02.04.2017.
 */
public class DocumentScore implements Comparable{
    private String fileName;
    private Double score;

    public String getFileName() {
        return fileName;
    }

    public Double getScore() {
        return score;
    }

    public DocumentScore(String fileName, Double score) {
        this.fileName = fileName;
        this.score = score;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof DocumentScore) {
            if (this.score > ((DocumentScore) o).score) {
                return -1;
            } else if(this.score == ((DocumentScore) o).score) {
                return 0;
            } else {
                return 1;
            }
        }
        return 0;
    }
}
