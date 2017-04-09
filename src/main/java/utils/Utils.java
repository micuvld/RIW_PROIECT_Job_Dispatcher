package utils;

import java.nio.file.Path;

/**
 * Created by vlad on 07.03.2017.
 */
public class Utils {
    public static String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');

        if (i > 0) {
            return fileName.substring(i+1);
        } else {
            return "";
        }
    }

    public static String getRelativeFilePath(Path path) {
        String stringPath = path.toString();
        return stringPath.substring(Configs.WORKDIR_PATH.length(), stringPath.length());
    }

    public static String getAbsoluteWorkdir(String path) {
        return Configs.WORKDIR_PATH + path;
    }

    public static String getAbsoluteTempdir(String path) {
        return Configs.TEMPDIR_PATH + path;
    }
}
