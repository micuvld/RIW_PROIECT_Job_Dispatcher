package indexers.map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;


/**
 * Created by vlad on 23.03.2017.
 */
public class MappedFile {
    @JsonProperty("filePath")
    private String filePath;

    @JsonIgnore
    private ArrayListMultimap<String, Integer> indexList;

    @JsonGetter("indexList")
    public Map<String, Collection<Integer>> getMap() {
        return indexList.asMap();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public ArrayListMultimap<String, Integer> getIndexList() {
        return indexList;
    }

    public void setIndexList(ArrayListMultimap<String, Integer> indexList) {
        this.indexList = indexList;
    }

    public MappedFile(String filePath) {
        this.filePath = filePath;
        indexList = ArrayListMultimap.create();
    }

    @JsonCreator
    public MappedFile(@JsonProperty("indexList") Map<String, Collection<Integer>> indexList,
                      @JsonProperty("filePath") String filePath,
                      @JsonProperty("wordCount") int wordCount) {
        this.indexList = ArrayListMultimap.create();
        for (Map.Entry<String, Collection<Integer>> entry : indexList.entrySet()) {
            this.indexList.putAll(entry.getKey(), entry.getValue());
        }
        this.filePath = filePath;
    }

    public void mapWord(String word) {
        if (!word.equals("")) {
            indexList.put(word, 1);
        }
    }
}
