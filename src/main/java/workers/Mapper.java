package workers;

import indexers.DirectIndexer;
import utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 23.03.2017.
 */
public class Mapper extends AbstractWorker {
    public Mapper(List<String> filesToMap) {
        super(filesToMap);
    }

    public void work() throws IOException {
        DirectIndexer directIndexer = new DirectIndexer();

        for (String file : targetsToProcess) {
            outputTargets.add(directIndexer.mapFile(Paths.get(file)));
        }
    }
}
