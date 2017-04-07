package search;

import com.mongodb.client.MongoCollection;
import indexers.WordSieve;
import indexers.reduce.FileApparition;
import mongo.MongoConnector;
import org.bson.Document;
import utils.porter.Porter;

import java.util.*;

/**
 * Created by vlad on 09.03.2017.
 */
public class SearchWorker {
    public static MongoCollection<Document> indexedFilesCollection;

    public SearchWorker() {
        indexedFilesCollection =  MongoConnector.getCollection("RIW", "indexedFiles");
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

        List<Double> queryWeights = calculateQueryWeights(tokens, filesOfInterest);
        Map<FileApparition, List<Double>> documentsWeights = new HashMap<>();
        TreeSet<DocumentScore> finalScores = new TreeSet<>();

        for (FileApparition file : filesOfInterest) {
            documentsWeights.put(file, calculateDocumentWeights(tokens, file, filesOfInterest));
        }

        for (Map.Entry<FileApparition, List<Double>> entry : documentsWeights.entrySet()) {
            double score = calculateScore(queryWeights, entry.getValue());
            System.out.println("Score" + score);
            finalScores.add(new DocumentScore(entry.getKey().getFile(), score));
        }

        for (DocumentScore documentScore : finalScores) {
            results.add(documentScore.getFileName());
            System.out.println(documentScore.getFileName() + ": " + documentScore.getScore());
        }

        return results;
    }

    private List<Double> calculateDocumentWeights(List<String> queryTokens, FileApparition currentFile, List<FileApparition> filesOfInterest) {
        List<Double> documentWeights = new ArrayList<>();

        for (String token : queryTokens) {
            documentWeights.add(calculateTf(token, currentFile) * calculateIdf(token, filesOfInterest));
        }

        return documentWeights;
    }

    private List<Double> calculateQueryWeights(List<String> queryTokens, List<FileApparition> filesOfInterest) {
        List<Double> queryWeights = new ArrayList<>();

        for (String token : queryTokens) {
            queryWeights.add(calculateQueryTf(token, queryTokens) * calculateIdf(token,filesOfInterest));
        }

        return queryWeights;
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

    private double calculateIdf(String token, List<FileApparition> fileApparitions) {
        long numberOfFilesContainingToken = fileApparitions.size();

        long totalNumberOfFiles = indexedFilesCollection.count();

        return 1 + Math.log((double) totalNumberOfFiles / numberOfFilesContainingToken);
    }

    private double calculateScore(List<Double> queryWeights, List<Double> documentWeights) {
        double upper = 0;
        double lower = 0;
        double queryWeightsLength = 0;
        double documentWeightsLength = 0;

        for (int i = 0; i < queryWeights.size(); ++i) {
            upper += queryWeights.get(i) * documentWeights.get(i);
            queryWeightsLength += queryWeights.get(i) * queryWeights.get(i);
            documentWeightsLength += documentWeights.get(i) * documentWeights.get(i);
        }

        queryWeightsLength = Math.sqrt(queryWeightsLength);
        documentWeightsLength = Math.sqrt(documentWeightsLength);
        lower = queryWeightsLength * documentWeightsLength;

        return upper / lower;
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
        //token = porter.stripAffixes(token);
        switch(token.charAt(0)) {
            case '-': //NOT
                return difference(currentFilesList, getFileApparitions(token.substring(1, token.length())));
            default:
                return reunion(currentFilesList, getFileApparitions(token));
        }
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

        for (Document  file : filesAsDocuments) {
            fileApparitions.add(new FileApparition((String)file.get("file"), (Integer)file.get("count")));
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

    private List<FileApparition> difference(List<FileApparition> l1, List<FileApparition> l2) {
        List<FileApparition> differenceList = new ArrayList<>();

        for (FileApparition element : l1) {
            if (!l2.contains(element)) {
                differenceList.add(element);
            }
        }

        return differenceList;
    }
}
