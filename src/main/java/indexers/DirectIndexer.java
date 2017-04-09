package indexers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import indexers.map.MappedFile;
import indexers.reduce.DirectIndexEntry;
import indexers.reduce.IndexedFile;
import job.JobType;
import mongo.MongoConnector;
import utils.Utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class used by workers to compute the direct index of a file.
 * Direct index is done in two phases: map and reduce
 * Created by vlad on 22.02.2017.
 */
public class DirectIndexer {
    private MappedFile mappedFile;

    private int mappedWordsCount = 0;

    /**
     * - receives the relative path to the file
     * - translates it to absolute path
     * - parses the file and maps the words that pass the WordSieve
     * @param path
     *  - relative path to the file to map
     * @return
     *  - path to the file that contains the mapped words
     * @throws IOException
     */
    public String mapFile(Path path) throws IOException {
        String absolutePath = Utils.getAbsoluteWorkdir(path.toString());
        mappedFile = new MappedFile(path.toString());

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(absolutePath),
                Charset.forName("UTF-8")));
        StringBuilder word = new StringBuilder();

        int c;
        while((c = reader.read()) != -1) {
            char charC = (char)c;
            if (isValid(charC)) {
                word.append(charC);
            } else {
                if (!word.toString().equals("")) {
                    mapWord(word.toString().toLowerCase());
                }
                word.replace(0, word.length(), "");
            }
        }

        return writeIndex(path);
    }

    /**
     * Only validates alphabetic characters
     * @param c
     * @return
     *  - true if the character is alphabetic
     */
    public boolean isValid(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }

    /**
     * - passes the word through a WordSieve
     * - maps the word if the word passed the conditions
     * - increments the total number of mapped words
     * @param word
     */
    private void mapWord(String word) {
        if (!WordSieve.isException(word)) {
            if (WordSieve.isStopWord(word)) {
                return;
            }

            word = WordSieve.toCanonicalForm(word);
        }

        mappedFile.mapWord(word);
        mappedWordsCount++;
    }

    public String generateMapFilePath(Path path) {
        return Utils.getAbsoluteTempdir(JobType.MAP.name() + "-" + path.getFileName());
    }

    /**
     * Writes the mapped words as json into a temp file
     * @param inPath
     * @return
     * - - path to the file that was written
     * @throws JsonProcessingException
     */
    public String writeIndex(Path inPath) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String outPath = generateMapFilePath(inPath);
        try {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            objectMapper.writeValue(new File(outPath),
                    mappedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MongoConnector.writeToCollection(new IndexedFile(inPath.toString(), mappedWordsCount), "RIW",
                "indexedFiles");
        return Utils.getRelativeFilePath(Paths.get(outPath));
    }

    /**
     * Reduces a file that contains mapped words
     * - sums the array of "1" for each mapped word
     * - writes direct index entries to collections like "aDirectIndex",
     *   where "a" is the first letter of the indexed word
     * @param path
     * @throws IOException
     */
    public void reduceFile(String path) throws IOException {
        String absolutePath = Utils.getAbsoluteTempdir(path);
        ObjectMapper objectMapper = new ObjectMapper();
        MappedFile mappedFile = objectMapper.readValue(new File(absolutePath), MappedFile.class);

        Map<String, Collection<Integer>> mapOfWords = mappedFile.getMap();
        String token;
        List<Integer> apparitions;
        int count;

        for (Map.Entry entry : mapOfWords.entrySet()) {
            token = (String) entry.getKey();
            apparitions = (List<Integer>) entry.getValue();

            count = reduceList(apparitions);

            writeDirectIndexToMongo(mappedFile.getFilePath(), token, count);
        }
    }

    public int reduceList(List<Integer> apparitionsList) {
        return apparitionsList.size();
    }

    public void writeDirectIndexToMongo(String file, String token, int count) {
        DirectIndexEntry directIndexEntry = new DirectIndexEntry(file, token, count);
        String database = "DirectIndex";
        String mapDatabase = "RIW";

        String collection = token.charAt(0) + "DirectIndex";
        String directIndexMapCollection = "directIndexMap";

        try {
            MongoConnector.writeToCollection(directIndexEntry, database, collection);
            MongoConnector.writeToCollection(new IndexMapEntry(token, collection), mapDatabase, directIndexMapCollection);
        } catch (JsonProcessingException e) {
            System.out.println("Failed to write object to Mongo in " + database + "." + collection);
            e.printStackTrace();
        }
    }

}