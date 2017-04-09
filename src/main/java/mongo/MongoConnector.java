package mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import utils.Configs;

import java.util.ArrayList;
import java.util.List;


/**
 * Class that manages the mongo connections,
 * writes to collections and gets collections.
 * All the methods use the MongoClient generated at the first request
 * Created by vlad on 18.03.2017.
 */
public class MongoConnector {
    private static MongoClient mongoClient;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static MongoClient getClient() {
        if (mongoClient == null) {
            mongoClient = new MongoClient(Configs.MONGO_IP_ADDRESS, Integer.parseInt(Configs.MONGO_PORT));
        }

        return mongoClient;
    }

    /**
     * Writes a generic object to a collection
     * @param objectToWrite
     * @param databaseName
     * @param collectionName
     * @throws JsonProcessingException
     */
    public static void writeToCollection(Object objectToWrite, String databaseName, String collectionName) throws JsonProcessingException {
        MongoDatabase database = getClient().getDatabase(databaseName);

        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document docToInsert;
        try {
            docToInsert = Document.parse(objectMapper.writeValueAsString(objectToWrite));
            collection.insertOne(docToInsert);
        } catch (JsonProcessingException e) {
            System.out.println("Failed to insert document in Mongo.");
            throw e;
        }
    }

    /**
     * Returns the specified collection
     * @param databaseName
     * @param collectionName
     * @return
     */
    public static MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        return getClient().getDatabase(databaseName).getCollection(collectionName);
    }

    /**
     * Returns a list containing the name of all collections
     * from the specified database
     * @param databaseName
     * @return
     */
    public static List<String> getCollections(String databaseName) {
       MongoIterable<String> collectionNames = getClient().getDatabase(databaseName).listCollectionNames();
       List<String> collectionNamesAsList = new ArrayList<>();

       for (String collection : collectionNames) {
           collectionNamesAsList.add(collection);
       }

       return collectionNamesAsList;
    }

    public static void dropDatabase(String databaseName) {
        getClient().dropDatabase(databaseName);
    }
}
