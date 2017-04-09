package utils;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by vlad on 08.04.2017.
 */
public class Configs {
    public static String JOB_BOARD_IP_ADDRESS;
    public static String JOB_BOARD_PORT;

    public static String WORKDIR_PATH;
    public static String TEMPDIR_PATH;

    public static String MONGO_IP_ADDRESS;
    public static String MONGO_PORT;

    public static String EXCEPTIONS_LIST_PATH;
    public static String STOP_WORDS_LIST_PATH;

    public static int SEARCH_DOCUMENTS_NUMBER_LIMIT;

    public static boolean loadProperties(String configFilePath) {
        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(configFilePath);

            prop.load(input);
            JOB_BOARD_IP_ADDRESS = prop.getProperty("job_board_ip_address");
            JOB_BOARD_PORT = prop.getProperty("job_board_port");
            WORKDIR_PATH = prop.getProperty("workdir_path");
            TEMPDIR_PATH = prop.getProperty("tempdir_path");
            MONGO_IP_ADDRESS = prop.getProperty("mongo_ip_address");
            MONGO_PORT = prop.getProperty("mongo_port");
            EXCEPTIONS_LIST_PATH = prop.getProperty("exceptions_list_path");
            STOP_WORDS_LIST_PATH = prop.getProperty("stop_words_list_path");
            SEARCH_DOCUMENTS_NUMBER_LIMIT = Integer.parseInt(prop.getProperty("search_documents_number_limit"));

            return true;
        } catch (IOException ex) {
            System.out.println("Failed to load properties file!");
            return false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
