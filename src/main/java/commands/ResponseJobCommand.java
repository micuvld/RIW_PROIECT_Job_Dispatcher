package commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import job.Job;
import socket.ISocket;
import socket.MasterClientSocket;
import socket.WorkerServerSocket;

/**Command used to give feedback after a worker finished
 * to process a command
 *  - it travels from a worker to the job board, then to the master
 * Created by vlad on 29.03.2017.
 */
public class ResponseJobCommand extends AbstractCommand {
    @JsonProperty("job")
    Job job;

    public Job getJob() {
        return job;
    }

    @JsonCreator
    public ResponseJobCommand(@JsonProperty("job") Job job) {
        this.type = CommandType.RESPONSE_JOB;
        this.job = job;
    }

    /**
     * - if received by the WorkerServerSocket, it will redirect the command to master
     * - if received by the MasterClientSocket (master), it will process the feedback
     * @param socket
     *  - the socket that received the command
     */
    @Override
    public void processCommand(ISocket socket) {
        if (socket instanceof WorkerServerSocket) {
            WorkerServerSocket workerSocket = (WorkerServerSocket)socket;
            workerSocket.redirectToMaster(this);
        }

        if (socket instanceof MasterClientSocket) {
            ((MasterClientSocket)socket).processCommandResponse(job);
        }
    }
}
