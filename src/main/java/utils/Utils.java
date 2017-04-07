package utils;

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

    public static String getFileExtensionFromPath(String path) {
        int i = path.lastIndexOf('.');

        if (i > 0) {
            return path.substring(i+1);
        } else {
            return "";
        }
    }

    public static String changeFileExtension(String fileName, String newExtension) {
        int i = fileName.lastIndexOf('.');

        if (i > 0) {
            return fileName.substring(0, i + 1) + newExtension;
        } else {
            return "";
        }
    }

    public static String getRelativePath(String absolutePath, String rootPath) {
        return absolutePath.substring(rootPath.length());
    }
}
