package indexers.reduce;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates a direct index document stored in mongo
 * Created by vlad on 30.03.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectIndexEntry implements Comparable{
    @JsonProperty("file")
    String file;
    @JsonProperty("token")
    String token;
    @JsonProperty("count")
    int count;

    public String getToken() {
        return token;
    }

    public String getFile() {
        return file;
    }

    public int getCount() {
        return count;
    }

    @JsonCreator
    public DirectIndexEntry(@JsonProperty("file") String file,
                            @JsonProperty("token") String token,
                            @JsonProperty("count") int count) {
        this.file = file;
        this.token = token;
        this.count = count;
    }

    /**
     * comparison done by token
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        if (o instanceof DirectIndexEntry) {
            DirectIndexEntry directIndexEntry = (DirectIndexEntry) o;
            int tokenCompareResult = this.token.compareTo(directIndexEntry.token);

            if (tokenCompareResult == 0) {
                return this.file.compareTo(directIndexEntry.file);
            } else {
                return tokenCompareResult;
            }
        } else {
            return 0;
        }
    }
}
