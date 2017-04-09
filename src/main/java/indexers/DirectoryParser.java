package indexers;


import utils.Utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Parses the directory and returns the list of all "*.txt" file paths
 * Created by vlad on 23.02.2017.
 */
public class DirectoryParser {
    public void parseDirectory(Path path, List<String> filesPaths) throws IOException {
        DirectoryStream<Path> stream = Files.newDirectoryStream(path);
        for (Path entry : stream) {
            if (Files.isDirectory(entry)) {
                parseDirectory(entry, filesPaths);
            } else {
                if (Utils.getFileExtension(entry.getFileName().toString()).equals("txt")) {
                    filesPaths.add(Utils.getRelativeFilePath(entry));
                }
            }
        }

    }
}
