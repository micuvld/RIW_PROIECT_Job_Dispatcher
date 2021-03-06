package socket;

import board.JobBoard;
import commands.*;

import java.io.IOException;
import java.net.Socket;

/**
 * Used by the job board to communicate with a worker.
 * It sends jobs and redirects responses to master
 * Created by vlad on 28.03.2017.
 */
public class WorkerServerSocket extends AbstractSocket implements Runnable{
    JobBoard jobBoard;
    MasterServerSocket masterSocket;

    public JobBoard getJobBoard() {
        return jobBoard;
    }

    public void setMasterSocket(MasterServerSocket masterSocket) {
        this.masterSocket = masterSocket;
    }

    public WorkerServerSocket(Socket socket, JobBoard jobBoard, MasterServerSocket masterSocket) {
        super(socket);
        this.jobBoard = jobBoard;
        this.masterSocket = masterSocket;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void run() {
        System.out.println("Worker started");
        while (true) {
            try {
                String command = readSocketLines();
                processCommand(command);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * Redirect is done by pushing the command in the master socket's response queue
     * @param command
     */
    public void redirectToMaster(AbstractCommand command) {
        try {
            masterSocket.pushResponseCommand(command);
        } catch (InterruptedException e) {
            System.out.println("Redirecting job response to master failed!");
            e.printStackTrace();
        }
    }
}
