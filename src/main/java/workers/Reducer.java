package workers;

import indexers.DirectIndexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by vlad on 30.03.2017.
 */
public class Reducer implements IWorker {
    List<String> filesToReduce;

    public Reducer(List<String> filesToReduce) {
        this.filesToReduce = filesToReduce;
    }
    @Override
    public void work() throws IOException {
        DirectIndexer directIndexer = new DirectIndexer();

        for (String file : filesToReduce) {
            directIndexer.reduceFile(file);
        }
    }
}
