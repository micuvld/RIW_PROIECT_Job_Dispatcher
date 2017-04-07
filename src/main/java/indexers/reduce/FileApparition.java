package indexers.reduce;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by vlad on 01.04.2017.
 */
public class FileApparition {
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

    public FileApparition(String file, int count, double norm) {
        this.file = file;
        this.count = count;
        this.norm = norm;
    }

    public FileApparition(String file, int count) {
        this.file = file;
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileApparition) {
            return this.file.equals(((FileApparition) o).file);
        } else {
            return false;
        }
    }
}
