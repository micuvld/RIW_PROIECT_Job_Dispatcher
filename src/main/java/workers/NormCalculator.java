package workers;

import indexers.DirectIndexer;
import indexers.StatsCalculator;

import java.io.IOException;
import java.util.List;

/**
 * Created by vlad on 08.04.2017.
 */
public class NormCalculator extends AbstractWorker {
    public NormCalculator(List<String> targetsToProcess) {
        super(targetsToProcess);
    }

    @Override
    public void work() throws IOException {
        StatsCalculator statsCalculator = new StatsCalculator();
        statsCalculator.calculateNorms(targetsToProcess);

        outputTargets = targetsToProcess;
    }
}
