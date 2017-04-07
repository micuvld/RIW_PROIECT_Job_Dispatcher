package search;

import com.mongodb.client.MongoCollection;
import indexers.StatsCalculator;
import indexers.WordSieve;
import indexers.reduce.FileApparition;
import mongo.MongoConnector;
import org.bson.Document;
import utils.porter.Porter;

import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by vlad on 09.03.2017.
 */
public class SearchWorker {
    public static MongoCollection<Document> indexedFilesCollection;
    public static StatsCalculator statsCalculator;

    public SearchWorker() {
        indexedFilesCollection =  MongoConnector.getCollection("RIW", "indexedFiles");
        statsCalculator = new StatsCalculator();
    }

    /**
     * Searches using tf-idf
     * @return
     *  List of files, ordered by rank
     */
    public List<String> rankedSearch(String interogation) {
        List<String> results = new ArrayList<>();
        List<FileApparition> filesOfInterest = booleanSearch(interogation);

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


        for (DocumentScore documentScore : documentScores) {
            results.add(documentScore.getFileName());
            System.out.println(documentScore.getFileName() + ": " + documentScore.getScore());
        }

        return results;
    }

    private List<FileApparition> booleanSearch(String interogation) {
        List<String> tokens = tokenizeInterogation(interogation);
        List<FileApparition> files = new ArrayList<FileApparition>();

        for (String token : tokens) {
            files = processToken(files, token);
        }

        return files;
    }

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

    public List<DocumentScore> initScores(List<FileApparition> filesOfInterest) {
        List<DocumentScore> scores = new ArrayList<>();

        for (FileApparition file : filesOfInterest) {
            scores.add(new DocumentScore(file.getFile(), 0.0));
        }

        return scores;
    }


    private double calculateDocumentWeight(String term, FileApparition currentFile) {
        return calculateTf(term, currentFile) * StatsCalculator.getIdf(term);
    }

    private double calculateQueryTermWeight(String term, List<String> queryTokens) {
        return calculateQueryTf(term, queryTokens) * StatsCalculator.getIdf(term);
    }

    private double calculateTf(String token, FileApparition fileApparition) {
        Document indexedFile = indexedFilesCollection.find(new Document("file", fileApparition.getFile())).first();

        MongoCollection<Document> directIndexMap = MongoConnector.getCollection("RIW", "directIndexMap");
        Document directIndexMapEntry =  directIndexMap.find(new Document("token", token)).first();

        MongoCollection<Document> directIndexCollection = MongoConnector.getCollection("DirectIndex",
                (String)directIndexMapEntry.get("collection"));

        Document tokenDocument = directIndexCollection.find(Document.parse(
                "{ $and:[" +
                        "{ token: \"" + token + "\"}" +
                        "{ file: \"" + fileApparition.getFile() + "\"}]}")).first();

        return (double)((Integer)tokenDocument.get("count")) / (Integer)indexedFile.get("count");
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

    private void addToken(String token, List<String> tokens) {
        if (!WordSieve.isException(token)) {
            if (WordSieve.isStopWord(token)) {
                return;
            }

            token = WordSieve.toCanonicalForm(token);
        }

        tokens.add(token);
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

    private List<FileApparition> processToken(List<FileApparition> currentFilesList, String token) {
        return reunion(currentFilesList, getFileApparitions(token));
    }


    /**
     * boolean search
     * @param word
     * @return
     */
    public List<FileApparition> getFileApparitions(String word) {
        List<FileApparition> fileApparitions = new ArrayList<>();
        MongoCollection<Document> collection = MongoConnector.getCollection("RIW", "invertedIndexMap");
        Document indexPointer = collection.find(new Document("token", word)).first();

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

            fileApparitions.add(new FileApparition(fileName, fileCount, norm));
        }
        return fileApparitions;
    }


    private List<FileApparition> reunion(List<FileApparition> l1, List<FileApparition> l2) {
        if (l1.size() < l2.size()) {
            List<FileApparition> reunionList = new ArrayList<>(l2);

            for (FileApparition element : l1) {
                if (!reunionList.contains(element)) {
                    reunionList.add(element);
                }
            }

            return reunionList;
        } else {
            List<FileApparition> reunionList = new ArrayList<>(l1);

            for (FileApparition element : l2) {
                if (!reunionList.contains(element)) {
                    reunionList.add(element);
                }
            }

            return reunionList;
        }
    }
}
