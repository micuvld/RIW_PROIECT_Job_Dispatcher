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

    public static MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        return getClient().getDatabase(databaseName).getCollection(collectionName);
    }

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
