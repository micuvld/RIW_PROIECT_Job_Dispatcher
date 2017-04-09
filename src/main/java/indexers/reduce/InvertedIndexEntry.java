package indexers.reduce;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates a inverted index document stored in mongo
 *  - it also contains the idf of the indexed word
 * Created by vlad on 01.04.2017.
 */
public class InvertedIndexEntry {
    @JsonProperty("token")
    String token;
    @JsonProperty("apparitions")
    List<FileApparition> apparitions;
    @JsonProperty("idf")
    double idf;

    public String getToken() {
        return token;
    }

    public List<FileApparition> getApparitions() {
        return apparitions;
    }

    public InvertedIndexEntry(String token) {
        this.token = token;
        this.apparitions = new ArrayList<>();
    }

    public void addApparition(String file, Integer count) {
        apparitions.add(new FileApparition(file, count));
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }
}
