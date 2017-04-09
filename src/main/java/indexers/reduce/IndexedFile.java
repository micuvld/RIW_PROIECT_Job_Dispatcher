package indexers.reduce;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates a document stored in mongo that specifies:
 *  - the indexed file
 *  - the total number of words that were mapped out of that file
 *  - the norm of the file's vector
 * Created by vlad on 09.04.2017.
 */
public class IndexedFile {
    @JsonProperty("file")
    String file;
    @JsonProperty("count")
    int count;
    @JsonProperty("norm")
    double norm;

    public String getFile() {
        return file;
    }

    public int getCount() {
        return count;
    }

    public double getNorm() {
        return norm;
    }

    public IndexedFile(String file, int count, double norm) {
        this.file = file;
        this.count = count;
        this.norm = norm;
    }

    public IndexedFile(String file, int count) {
        this.file = file;
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IndexedFile) {
            return this.file.equals(((IndexedFile) o).file);
        } else {
            return false;
        }
    }
}
