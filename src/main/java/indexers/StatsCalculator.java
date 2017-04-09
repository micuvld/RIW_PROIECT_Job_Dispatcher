package indexers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import mongo.MongoConnector;
import org.bson.Document;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by vlad on 06.04.2017.
 */
public class StatsCalculator {
    public static MongoCollection<Document> indexedFilesCollection;
    public static MongoCollection<Document> directIndexMapCollection;
    public static MongoCollection<Document> invertedIndexMapCollection;

    public StatsCalculator() {
        indexedFilesCollection =  MongoConnector.getCollection(
                "RIW", "indexedFiles");
        directIndexMapCollection =  MongoConnector.getCollection(
                "RIW", "directIndexMap");
        invertedIndexMapCollection =  MongoConnector.getCollection(
                "RIW", "invertedIndexMap");
    }

    public void calculateNorms(List<String> targetFiles) {
        for (String file : targetFiles) {
            calculateNorm(file);
        }
    }

    public void calculateNorm(String targetFile) {
        Document indexedFile = indexedFilesCollection.find(new Document("file", targetFile)).first();
        int fileCount = indexedFile.getInteger("count");
        double norm = 0;

        List<String> directIndexCollections = MongoConnector.getCollections("DirectIndex");

        for (String collectionName : directIndexCollections) {
            MongoCollection<Document> directIndexCollection = MongoConnector.getCollection(
                    "DirectIndex", collectionName);
            MongoCollection<Document> invertedIndexCollection = getInvertedIndexCollection(collectionName);

            MongoCursor<Document> fileTerms = directIndexCollection.find(new Document("file", targetFile)).iterator();

            while (fileTerms.hasNext()) {
                Document directIndexTerm = fileTerms.next();
                double tf = (double) directIndexTerm.getInteger("count") / fileCount;

                Document invertedIndexTerm = invertedIndexCollection.find(new Document(
                        "token", directIndexTerm.getString("token"))).first();

                double idf = invertedIndexTerm.getDouble("idf");

                norm += tf * idf * tf * idf;
            }
        }

        norm = Math.sqrt(norm);
        writeNorm(targetFile, norm);
    }

    /**
     * Gets an inverted index collection corresponding to the direct index collection
     *  - ex: aDirectIndex -> aInversedIndex
     * @param directIndexCollection
     * @return
     */
    private MongoCollection<Document> getInvertedIndexCollection(String directIndexCollection) {
        return MongoConnector.getCollection("InvertedIndex",
                directIndexCollection.charAt(0) + "InvertedIndex");
    }

    private void writeNorm(String fileName, double norm) {
        Document normDocument = new Document("norm", norm);
        indexedFilesCollection.updateOne(eq("file", fileName), new Document("$set", normDocument));
    }

    public static double getIdf(String token) {
        Document invertedIndexCollectionEntry = invertedIndexMapCollection.find(eq("token", token)).first();
        if (invertedIndexCollectionEntry == null) {
            return 0;
        }

        MongoCollection<Document> invertedIndexCollection = MongoConnector.getCollection(
                "InvertedIndex", invertedIndexCollectionEntry.getString("collection"));
        Document invertedIndex = invertedIndexCollection.find(eq("token", token)).first();

        if (invertedIndex != null) {
            return invertedIndex.getDouble("idf");
        } else {
            //word not found in database;
            return 0;
        }
    }
}
