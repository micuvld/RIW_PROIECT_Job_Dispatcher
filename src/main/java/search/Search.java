package search;

import com.mongodb.client.MongoCollection;
import indexers.StatsCalculator;
import indexers.WordSieve;
import indexers.reduce.FileApparition;
import indexers.reduce.IndexedFile;
import mongo.MongoConnector;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by vlad on 09.03.2017.
 */
public class Search {
    public static MongoCollection<Document> indexedFilesCollection;
    public static StatsCalculator statsCalculator;

    public Search() {
        indexedFilesCollection =  MongoConnector.getCollection("RIW", "indexedFiles");
        statsCalculator = new StatsCalculator();
    }

    /**
     * Searches using tf-idf
     * @return
     *  List of files, ordered by rank
     */
    public List<DocumentScore> rankedSearch(String interogation) {
        List<IndexedFile> filesOfInterest = booleanSearch(interogation);

        List<String> tokens = tokenizeInterogation(interogation);
        trimTokens(tokens);


        List<DocumentScore> documentScores = initScores(filesOfInterest);
        double queryNorm = 0;

        for (String token : tokens) {
            double queryTermWeight = calculateQueryTermWeight(token, tokens);
            for (int i = 0; i < filesOfInterest.size(); ++i) {
                double documentTermWeight = calculateDocumentWeight(token, filesOfInterest.get(i));
                documentScores.get(i).addToScore(queryTermWeight * documentTermWeight);
            }

            queryNorm += queryTermWeight * queryTermWeight;
        }

        queryNorm = Math.sqrt(queryNorm);

        for (int i = 0; i < filesOfInterest.size(); ++i) {
            double currentScore = documentScores.get(i).getScore();
            documentScores.get(i).setScore(currentScore / filesOfInterest.get(i).getNorm() * queryNorm);
        }

        Collections.sort(documentScores);

        return documentScores;
    }

    private List<IndexedFile> booleanSearch(String interogation) {
        List<String> tokens = tokenizeInterogation(interogation);
        List<IndexedFile> files = new ArrayList<>();

        for (String token : tokens) {
            files = processToken(files, token);
        }

        return files;
    }

    /**
     * Splits the interogation into tokens
     * - follows the same processing as the indexer:
     *      - exception
     *      - stop word
     *      - canonical form
     * @param interogation
     * @return
     */
    private List<String> tokenizeInterogation(String interogation) {
        StringBuilder token = new StringBuilder();
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < interogation.length(); ++i) {
            char currentChar = interogation.charAt(i);
            if (currentChar != ' ') {
                token.append(currentChar);
            } else {
                if (!token.toString().equals("")) {
                    addToken(token.toString(), tokens);
                }
                token.replace(0, token.length(), "");
            }
        }

        tokens.add(token.toString());
        return tokens;
    }

    private void addToken(String token, List<String> tokens) {
        if (!WordSieve.isException(token)) {
            if (WordSieve.isStopWord(token)) {
                return;
            }

            token = WordSieve.toCanonicalForm(token);
        }

        tokens.add(token);
    }

    public List<DocumentScore> initScores(List<IndexedFile> filesOfInterest) {
        List<DocumentScore> scores = new ArrayList<>();

        for (IndexedFile file : filesOfInterest) {
            scores.add(new DocumentScore(file.getFile(), 0.0));
        }

        return scores;
    }


    private double calculateDocumentWeight(String term, IndexedFile currentFile) {
        return calculateTf(term, currentFile) * StatsCalculator.getIdf(term);
    }

    private double calculateQueryTermWeight(String term, List<String> queryTokens) {
        return calculateQueryTf(term, queryTokens) * StatsCalculator.getIdf(term);
    }

    private double calculateTf(String token, IndexedFile indexedFile) {
        MongoCollection<Document> directIndexMap = MongoConnector.getCollection("RIW", "directIndexMap");
        Document directIndexMapEntry =  directIndexMap.find(new Document("token", token)).first();
        if (directIndexMapEntry == null) {
            return 0;
        }

        MongoCollection<Document> directIndexCollection = MongoConnector.getCollection("DirectIndex",
                (String)directIndexMapEntry.get("collection"));

        Document tokenDocument = directIndexCollection.find(Document.parse(
                "{ $and:[" +
                        "{ token: \"" + token + "\"}" +
                        "{ file: \"" + indexedFile.getFile() + "\"}]}")).first();

        if (tokenDocument != null) {
            return (double) ((Integer) tokenDocument.get("count")) / indexedFile.getCount();
        } else {
            return 0;
        }
    }

    private double calculateQueryTf(String token, List<String> queryTokens) {
        int tokenCount = 0;
        int totalCount = 0;

        for (String queryToken : queryTokens) {
            if (queryToken.equals(token)) {
                tokenCount++;
            }
            totalCount++;
        }

        return (double) tokenCount / totalCount;
    }

    /**
     * Removes any query operators bound to the tokens
     * @param tokens
     */
    private void trimTokens(List<String> tokens) {
        for (int i = 0; i < tokens.size(); ++i) {
            String token =  tokens.get(i);
            if (token.charAt(0) == '-') {
                tokens.remove(i);
            }
        }
    }

    private List<IndexedFile> processToken(List<IndexedFile> currentFilesList, String token) {
        return reunion(currentFilesList, getFileApparitions(token));
    }


    /**
     * boolean search
     * @param word
     * @return
     */
    public List<IndexedFile> getFileApparitions(String word) {
        List<IndexedFile> fileApparitions = new ArrayList<>();
        MongoCollection<Document> collection = MongoConnector.getCollection("RIW", "invertedIndexMap");
        Document indexPointer = collection.find(new Document("token", word)).first();
        if (indexPointer == null) {
            return new ArrayList<>();
        }

        MongoCollection<Document> indexCollection = MongoConnector.getCollection("InvertedIndex",
                (String)(indexPointer.get("collection")));

        Document indexEntry = indexCollection.find(new Document("token", word)).first();
        List<Document> filesAsDocuments = (ArrayList<Document>)indexEntry.get("apparitions");

        for (Document  fileDocument : filesAsDocuments) {
            Document indexedFileDocument = indexedFilesCollection.find(eq("file",
                    fileDocument.getString("file"))).first();

            String fileName = indexedFileDocument.getString("file");
            int fileCount = indexedFileDocument.getInteger("count");
            double norm = indexedFileDocument.getDouble("norm");

            fileApparitions.add(new IndexedFile(fileName, fileCount, norm));
        }
        return fileApparitions;
    }


    private List<IndexedFile> reunion(List<IndexedFile> l1, List<IndexedFile> l2) {
        if (l1.size() < l2.size()) {
            List<IndexedFile> reunionList = new ArrayList<>(l2);

            for (IndexedFile element : l1) {
                if (!reunionList.contains(element)) {
                    reunionList.add(element);
                }
            }

            return reunionList;
        } else {
            List<IndexedFile> reunionList = new ArrayList<>(l1);

            for (IndexedFile element : l2) {
                if (!reunionList.contains(element)) {
                    reunionList.add(element);
                }
            }

            return reunionList;
        }
    }
}
