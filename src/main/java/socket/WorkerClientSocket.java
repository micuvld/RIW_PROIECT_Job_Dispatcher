package socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import indexers.StatsCalculator;
import job.Job;
import job.JobState;
import socket.commands.RequestJobCommand;
import socket.commands.ResponseJobCommand;
import workers.InvertedIndexReducer;
import workers.Mapper;
import workers.Reducer;
import workers.Sorter;

import java.io.IOException;
import java.util.List;

/**
 * Created by vlad on 28.03.2017.
 */
public class WorkerClientSocket extends AbstractSocket {
    public WorkerClientSocket(String hostname, int port) {
        super(hostname, port);
        socketType = SocketType.WORKER;
    }

    public void work() throws IOException {
        while(true) {
            System.out.println("Requesting job...");
            Job job = requestJob();
            boolean isFinished = processJob(job);

            if (isFinished) {
                job.setState(JobState.COMPLETED);
            } else {
                job.setState(JobState.ABORTED);
            }

            sendResponse(job);
        }
    }

    public Job requestJob() throws IOException {;
        this.writeAsJson(new RequestJobCommand());

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(readSocketLines(), Job.class);
    }

    private boolean processJob(Job job) {
        List<String> targetsToProcess = job.getTargets();

        switch(job.getJobType()){
            case MAP:
                System.out.println("MAP JOB FOR: " + targetsToProcess);
                Mapper mapper = new Mapper(targetsToProcess);

                try {
                    mapper.work();
                    setResultedTargets(mapper.getOutputTargets(), job);
                    return true;
                } catch (IOException e) {
                    System.out.println("Failed to MAP files!");
                    return false;
                }
            case REDUCE_DIRECT_INDEX:
                System.out.println("REDUCE_DIRECT_INDEX JOB FOR: " + targetsToProcess);
                Reducer reducer = new Reducer(targetsToProcess);

                try {
                    reducer.work();
                    return true;
                } catch (IOException e) {
                    System.out.println("Failed to REDUCE_DIRECT_INDEX files");
                    e.printStackTrace();
                    return false;
                }
            case SORT:
                System.out.println("SORT JOB FOR: " + targetsToProcess);
                Sorter sorter = new Sorter(targetsToProcess);

                try {
                    sorter.work();
                    setResultedTargets(sorter.getOutputTargets(), job);
                    return true;
                } catch (IOException e) {
                    System.out.println("Failed to SORT files");
                    e.printStackTrace();
                    return false;
                }
            case REDUCE_INVERTED_INDEX:
                System.out.println("REDUCE_INVERSED_INDEX JOB FOR: " + targetsToProcess);
                InvertedIndexReducer invertedIndexReducer = new InvertedIndexReducer(targetsToProcess);

                try {
                    invertedIndexReducer.work();
                    return true;
                } catch (IOException e) {
                    System.out.println("Failed to REDUCE_INVERSED_INDEX files");
                    e.printStackTrace();
                    return false;
                }
            case CALCULATE_NORMS:
                System.out.println("CALCULATE_NORMS JOB FOR: " + targetsToProcess);
                StatsCalculator statsCalculator = new StatsCalculator();
                statsCalculator.calculateNorms(targetsToProcess);

                System.out.println("Finished calculating norm for " + targetsToProcess);
                return true;
        }

        return false;
    }

    /**
     *
     * Only works for one file!
     * @param outFilesPaths
     * @param job
     */
    private void setResultedTargets(List<String> outFilesPaths, Job job) {
        job.setTarget(outFilesPaths.get(0));
    }

    private void sendResponse(Job job) throws IOException {
        ResponseJobCommand command = new ResponseJobCommand(job);
        writeAsJson(command);
    }
}