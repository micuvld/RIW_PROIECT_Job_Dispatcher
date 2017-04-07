package indexers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import indexers.reduce.FileApparition;
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
    public static MongoCollection<Document> inverseIndexMapCollection;

    public StatsCalculator() {
        indexedFilesCollection =  MongoConnector.getCollection(
                "RIW", "indexedFiles");
        directIndexMapCollection =  MongoConnector.getCollection(
                "RIW", "directIndexMap");
        inverseIndexMapCollection =  MongoConnector.getCollection(
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
}
