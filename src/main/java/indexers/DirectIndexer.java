package indexers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import indexers.map.MappedFile;
import indexers.reduce.DirectIndexEntry;
import indexers.reduce.IndexedFile;
import job.JobType;
import mongo.MongoConnector;
import utils.Configs;
import utils.Utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by vlad on 22.02.2017.
 */
public class DirectIndexer {
    private final String INDEX_DIRECTORY_PATH = Configs.TEMPDIR_PATH;
    private MappedFile mappedFile;

    private int fileWordCount = 0;

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

    public boolean isValid(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }

    private void mapWord(String word) {
        if (!WordSieve.isException(word)) {
            if (WordSieve.isStopWord(word)) {
                return;
            }

            word = WordSieve.toCanonicalForm(word);
        }

        mappedFile.mapWord(word);
        fileWordCount++;
    }

    public String generateMapFilePath(Path path) {
        return Utils.getAbsoluteTempdir(JobType.MAP.name() + "-" + path.getFileName());//INDEX_DIRECTORY_PATH + JobType.MAP.name() + "-" + path.getFileName();
    }

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

        MongoConnector.writeToCollection(new IndexedFile(inPath.toString(), fileWordCount), "RIW",
                "indexedFiles");
        return Utils.getRelativeFilePath(Paths.get(outPath));
    }

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