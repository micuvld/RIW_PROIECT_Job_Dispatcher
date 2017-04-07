package indexers;

import utils.porter.Porter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 06.04.2017.
 */
public class WordSieve {
    private final static String STOP_WORDS_FILE = "resources/stop_words.txt";
    private final static String EXCEPTION_WORDS_FILE = "resources/exception_words.txt";

    private final static List<String> stopWords = new ArrayList<String>();
    private final static List<String> exceptionWords = new ArrayList<String>();
    private final static Porter porter = new Porter();

    static {
        //populateExceptionAndStopLists();
    }

    public static boolean isException(String word) {
        return exceptionWords.contains(word);
    }

    public static boolean isStopWord(String word) {
        return stopWords.contains(word);
    }

    public static String toCanonicalForm(String word) {
        return porter.stripAffixes(word);
    }

    private static void populateExceptionAndStopLists() {
        BufferedReader bufferedReader = null;
        String currentLine;

        try {
            bufferedReader = new BufferedReader(new FileReader(STOP_WORDS_FILE));
            while ((currentLine = bufferedReader.readLine()) != null) {
                stopWords.add(currentLine);
            }

            bufferedReader = new BufferedReader(new FileReader(EXCEPTION_WORDS_FILE));
            while ((currentLine = bufferedReader.readLine()) != null) {
                exceptionWords.add(currentLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
