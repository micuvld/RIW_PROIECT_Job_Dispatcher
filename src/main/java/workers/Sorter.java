package workers;

import indexers.InverseIndexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 01.04.2017.
 */
public class Sorter extends AbstractWorker {
    public Sorter(List<String> collectionsToSort) {
        super(collectionsToSort);
    }

    @Override
    public void work() throws IOException {
        InverseIndexer inverseIndexer = new InverseIndexer();

        for (String collection : targetsToProcess) {
            outputTargets.add(inverseIndexer.sort(collection));
        }
    }
}
