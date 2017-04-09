package commands;

import job.Job;
import socket.ISocket;
import socket.WorkerServerSocket;

import java.io.IOException;

/**
 * Created by vlad on 28.03.2017.
 */
public class RequestJobCommand extends AbstractCommand {
    public RequestJobCommand() {
        this.type = CommandType.REQUEST_JOB;
    }

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
