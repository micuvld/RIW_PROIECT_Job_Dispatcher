package workers;

import indexers.DirectIndexer;
import indexers.InverseIndexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by vlad on 01.04.2017.
 */
public class InvertedIndexReducer extends AbstractWorker{
    public InvertedIndexReducer(List<String> filesToReduce) {
        super(filesToReduce);
    }

    @Override
    public void work() throws IOException {
        InverseIndexer inverseIndexer = new InverseIndexer();

        for (String file : targetsToProcess) {
            inverseIndexer.reduce(file);
            outputTargets.add(file);
        }
    }
}
