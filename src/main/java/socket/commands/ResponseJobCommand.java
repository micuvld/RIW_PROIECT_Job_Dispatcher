package socket.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import job.Job;
import socket.ISocket;
import socket.MasterClientSocket;
import socket.WorkerServerSocket;

/**
 * Created by vlad on 29.03.2017.
 */
public class ResponseJobCommand extends AbstractCommand {
    @JsonProperty("job")
    Job job;

    @JsonCreator
    public ResponseJobCommand(@JsonProperty("job") Job job) {
        this.type = CommandType.RESPONSE_JOB;
        this.job = job;
    }

    @Override
    public void processCommand(ISocket socket) {
        if (socket instanceof WorkerServerSocket) {
            WorkerServerSocket workerSocket = (WorkerServerSocket)socket;
            workerSocket.redirectToMaster(this);
        }

        if (socket instanceof MasterClientSocket) {
            ((MasterClientSocket)socket).processJobResponse(job);
        }
    }
}
