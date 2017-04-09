package commands;

import job.Job;
import socket.ISocket;
import socket.WorkerServerSocket;

import java.io.IOException;

/**
 * Command that is used by workers to request a job from the job board
 * Created by vlad on 28.03.2017.
 */
public class RequestJobCommand extends AbstractCommand {
    public RequestJobCommand() {
        this.type = CommandType.REQUEST_JOB;
    }

    /**
     * If received by the WorkerServerSocket, it will try to get a job
     * from the job board and send it to the requesting worker
     * It blocks while the queue is empty
     * @param socket
     *  - the socket that received the command
     * @throws UnableToProcessCommandException
     */
    @Override
    public void processCommand(ISocket socket) throws UnableToProcessCommandException {
        Job job = ((WorkerServerSocket)socket).getJobBoard().popJob();

        try {
            socket.writeAsJson(job);
        } catch (IOException e) {
            throw new UnableToProcessCommandException("Could not send job", e);
        }
        System.out.println("Job sent!");
    }
}
