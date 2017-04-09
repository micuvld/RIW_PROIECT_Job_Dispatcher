package socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import job.Job;
import job.JobState;
import commands.RequestJobCommand;
import commands.ResponseJobCommand;
import workers.*;

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
        AbstractWorker worker = WorkerFactory.instantiateWorker(
                job.getJobType(), targetsToProcess
        );

        try {
            System.out.println(job.getJobType() + " JOB FOR: " + targetsToProcess);
            worker.work();
            setResultedTargets(worker.getOutputTargets(), job);
            return true;
        } catch (IOException e) {
            System.out.println("Failed to" + job.getJobType() + " files!");
            return false;
        }
    }

    /**
     *
     * Only works for one file!
     * @param outFilesPaths
     * @param job
     */
    private void setResultedTargets(List<String> outFilesPaths, Job job) {
        job.setTargets(outFilesPaths);
    }

    private void sendResponse(Job job) throws IOException {
        ResponseJobCommand command = new ResponseJobCommand(job);
        writeAsJson(command);
    }
}