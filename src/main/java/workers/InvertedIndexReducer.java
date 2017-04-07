package workers;

import indexers.DirectIndexer;
import indexers.InverseIndexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by vlad on 01.04.2017.
 */
public class InvertedIndexReducer implements IWorker{
    List<String> filesToReduce;

    public InvertedIndexReducer(List<String> filesToReduce) {
        this.filesToReduce = filesToReduce;
    }
    @Override
    public void work() throws IOException {
        InverseIndexer inverseIndexer = new InverseIndexer();

        for (String file : filesToReduce) {
            inverseIndexer.reduce(file);
        }
    }
}
