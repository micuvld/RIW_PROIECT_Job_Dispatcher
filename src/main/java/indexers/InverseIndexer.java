package indexers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.Mongo;
import com.mongodb.client.MongoCollection;
import indexers.reduce.InvertedIndexEntry;
import indexers.reduce.DirectIndexEntry;
import mongo.MongoConnector;
import org.bson.Document;
import utils.Configs;
import utils.Utils;

import java.io.*;
import java.util.*;

/**
 * Class used by workers to compute the inversed index of a file.
 * Inversed index is done in two phases: sort and reduce
 * Created by vlad on 08.03.2017.
 */
public class InverseIndexer {
    private long totalNumberOfFiles = 0;

    public InverseIndexer() {
    }

    /**
     * - receives the relative path to the file
     * - translates it to absolute path
     * - reads the direct index in a TreeSet,
     *   sorts it by word and
     *   writes them in tempFiles
     * @param collectionName
     * @return
     * @throws IOException
     */
    public String sort(String collectionName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        TreeSet<DirectIndexEntry> sortedDirectIndexEntries = new TreeSet<>();

        MongoCollection<Document> collection = MongoConnector.getCollection("DirectIndex", collectionName);
        for (Document document : collection.find()) {
            sortedDirectIndexEntries.add(objectMapper.readValue(document.toJson(), DirectIndexEntry.class));
        }

        String outFileName = collectionName.charAt(0) + "Sorted";
        String outFilePath = Utils.getAbsoluteTempdir(outFileName);
        objectMapper.writeValue(new File(outFilePath), sortedDirectIndexEntries);

        return outFileName;
    }

    /**
     * Reduces a file that contains sorted direct indexes
     * - reduces the lines containing the same word
     *   and creates inverted index entries
     * - calculates idf for the word
     * - writes inversed index entries to collections like "aDirectIndex",
     *   where "a" is the first letter of the indexed word
     * @param target
     * - file to process
     * @return
     * @throws IOException
     */
    public String reduce(String target) throws IOException {
        String absolutePath = Utils.getAbsoluteTempdir(target);

        ObjectMapper objectMapper = new ObjectMapper();
        DirectIndexEntry[] directIndexEntries = objectMapper.readValue(new File(absolutePath), DirectIndexEntry[].class);
        totalNumberOfFiles = calculateTotalNumberOfFiles();

        String outCollectionName = directIndexEntries[0].getToken().charAt(0) + "InvertedIndex";

        String latestToken = directIndexEntries[0].getToken();
        InvertedIndexEntry invertedIndexEntry = new InvertedIndexEntry(latestToken);

        int numberOfFiles = 0;
        for (DirectIndexEntry directIndexEntry : directIndexEntries) {
            if (!latestToken.equals(directIndexEntry.getToken())) {
                invertedIndexEntry.setIdf(Math.log((double) totalNumberOfFiles / numberOfFiles));
                writeInverseIndexToMongo(invertedIndexEntry, outCollectionName);

                invertedIndexEntry = new InvertedIndexEntry(directIndexEntry.getToken());
                latestToken = directIndexEntry.getToken();
                numberOfFiles = 0;
            }
            invertedIndexEntry.addApparition(directIndexEntry.getFile(), directIndexEntry.getCount());
            numberOfFiles++;
        }

        invertedIndexEntry.setIdf(Math.log((double) totalNumberOfFiles / numberOfFiles));
        writeInverseIndexToMongo(invertedIndexEntry, outCollectionName);

        return outCollectionName;
    }

    public void writeInverseIndexToMongo(InvertedIndexEntry invertedIndexEntry, String collectionName) throws JsonProcessingException {
        String mapDatabase = "RIW";
        String invertedIndexMapCollection = "invertedIndexMap";
        MongoConnector.writeToCollection(invertedIndexEntry, "InvertedIndex", collectionName);
        MongoConnector.writeToCollection(new IndexMapEntry(invertedIndexEntry.getToken(), collectionName), mapDatabase,
                invertedIndexMapCollection);
    }

    public long calculateTotalNumberOfFiles() {
        return MongoConnector.getCollection("RIW", "indexedFiles").count();
    }
}
