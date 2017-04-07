package indexers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import indexers.map.MappedFile;
import indexers.reduce.DirectIndexEntry;
import indexers.reduce.FileApparition;
import job.JobType;
import mongo.MongoConnector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by vlad on 22.02.2017.
 */
public class DirectIndexer {
    private final String INDEX_DIRECTORY_PATH = "/home/vlad/workspace/RIW_PROIECT/outdir/";
    private MappedFile mappedFile;

    private int fileWordCount = 0;

    public DirectIndexer() {
        //populateExceptionAndStopLists();
    }

    public String mapFile(Path path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path.toString()),
                Charset.forName("UTF-8")));
        mappedFile = new MappedFile(path.toString());

        StringBuilder word = new StringBuilder();

        int c;
        while((c = reader.read()) != -1) {
            char charC = (char)c;
            if (Character.isLetter(charC)) {
                word.append(charC);
            } else {
                if (!word.toString().equals("")) {
                    mapWord(word.toString().toLowerCase());
                }
                word.replace(0, word.length(), "");
            }
        }

        String outPath = writeIndex(path);
        return outPath;
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

    public String generateOutPath(Path path, JobType jobType) {
        return INDEX_DIRECTORY_PATH + JobType.MAP.name() + "-" + path.getFileName();
    }

    public String writeIndex(Path inPath) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String outPath = generateOutPath(inPath, JobType.MAP);
        try {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            objectMapper.writeValue(new File(outPath),
                    mappedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MongoConnector.writeToCollection(new FileApparition(inPath.toString(), fileWordCount), "RIW",
                "indexedFiles");
        return outPath;
    }

    public void reduceFile(String path) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        MappedFile mappedFile = objectMapper.readValue(new File(path), MappedFile.class);

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