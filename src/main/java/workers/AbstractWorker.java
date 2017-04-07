package workers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 07.04.2017.
 */
public abstract class AbstractWorker implements IWorker {
    List<String> targetsToProcess;
    List<String> outputTargets;

    public List<String> getTargetsToProcess() {
        return targetsToProcess;
    }

    public List<String> getOutputTargets() {
        return outputTargets;
    }

    public AbstractWorker(List<String> targetsToProcess) {
        this.targetsToProcess = targetsToProcess;
        this.outputTargets = new ArrayList<>();
    }
}
