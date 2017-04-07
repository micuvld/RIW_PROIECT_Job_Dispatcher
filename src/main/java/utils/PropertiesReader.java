package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by vlad on 28.03.2017.
 */
public class PropertiesReader {
    /**
     * Read properties from specified file
     * @param filePath
     *      properties file
     * @return
     */
    public static Properties readProperties(String filePath) {
        Properties properties = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(filePath);
            properties.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return properties;
    }
}
