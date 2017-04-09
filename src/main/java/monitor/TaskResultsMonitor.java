package monitor;

import commands.AbstractCommand;
import job.Job;
import socket.ISocket;
import socket.MasterClientSocket;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * The class monitors the failedJobsQueue of the MasterClientSocket
 * - if there are any aborted jobs commands received from workers,
 *  the jobs will we posted again to the board
 * Created by vlad on 27.03.2017.
 */
public class TaskResultsMonitor implements Runnable{
    LinkedBlockingQueue<Job> failedJobsQueue = new LinkedBlockingQueue<>();
    MasterClientSocket masterSocket;

    public TaskResultsMonitor(MasterClientSocket masterSocket) {
        this.masterSocket = masterSocket;
        this.failedJobsQueue = masterSocket.getFailedJobs();
    }

    @Override
    public void run() {
        try {
            Job job = failedJobsQueue.take();
            masterSocket.sendJob(job, job.getJobType());
        } catch (InterruptedException e) {
            System.out.println("Interrupted while taking object from commands queue!");
            e.printStackTrace();
        }

    }
}
