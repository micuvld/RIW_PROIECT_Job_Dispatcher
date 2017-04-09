package workers;

import job.JobType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to create specific workers,
 * depending on the job type found in json
 * Created by vlad on 08.04.2017.
 */
public class WorkerFactory {
    private final static Map<JobType, Class> workerClassesMap = new HashMap<>();

    static {
        workerClassesMap.put(JobType.MAP, Mapper.class);
        workerClassesMap.put(JobType.REDUCE_DIRECT_INDEX, Reducer.class);
        workerClassesMap.put(JobType.SORT, Sorter.class);
        workerClassesMap.put(JobType.REDUCE_INVERTED_INDEX, InvertedIndexReducer.class);
        workerClassesMap.put(JobType.CALCULATE_NORMS, NormCalculator.class);
    }

    public static AbstractWorker instantiateWorker(JobType jobType, List<String> targetsToProcess) {
        if (workerClassesMap.containsKey(jobType)) {
            try {
                return (AbstractWorker) workerClassesMap.get(jobType).getConstructor(List.class).newInstance(targetsToProcess);
            } catch (IllegalAccessException |
                    InstantiationException |
                    InvocationTargetException |
                    NoSuchMethodException e) {
                System.out.println("Error at instantiating command object");
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }
}
