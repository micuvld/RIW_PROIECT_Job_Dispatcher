package workers;

import indexers.InverseIndexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 01.04.2017.
 */
public class Sorter implements IWorker {
    List<String> collectionsToSort;
    List<String> outFilesPath;

    public Sorter(List<String> collectionsToSort) {
        this.collectionsToSort = collectionsToSort;
        outFilesPath = new ArrayList<>();
    }

    @Override
    public void work() throws IOException {
        InverseIndexer inverseIndexer = new InverseIndexer();

        for (String collection : collectionsToSort) {
            outFilesPath.add(inverseIndexer.sort(collection));
        }
    }

    public List<String> getOutFilesPath() {
        return outFilesPath;
    }
}
