package indexers;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates an entry of index map stored in mongo
 * Created by vlad on 01.04.2017.
 */
public class IndexMapEntry {
    @JsonProperty("token")
    String token;
    @JsonProperty("collection")
    String collection;

    public IndexMapEntry(String token, String collection) {
        this.token = token;
        this.collection = collection;
    }
}
