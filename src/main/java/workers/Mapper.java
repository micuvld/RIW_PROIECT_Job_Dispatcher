package workers;

import indexers.DirectIndexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 23.03.2017.
 */
public class Mapper implements IWorker {
    List<String> filesToMap;
    List<String> outFilesPath;

    public List<String> getOutFilesPath() {
        return outFilesPath;
    }

    public Mapper(List<String> filesToMap) {
        this.filesToMap = filesToMap;
        this.outFilesPath = new ArrayList<>();
    }

    public void work() throws IOException {
        DirectIndexer directIndexer = new DirectIndexer();

        for (String file : filesToMap) {
            outFilesPath.add(directIndexer.mapFile(Paths.get(file)));
        }
    }
}
