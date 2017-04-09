package board;


import job.Job;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class that manages the jobs queue
 *  - it uses a LinkedBlockingQueue, so it manages multi-threading operations
 *  - this is where the jobs for workers are hold
 * Created by vlad on 27.03.2017.
 */
public class TaskBoard implements JobBoard {
    private LinkedBlockingQueue<Job> taskQueue;

    public TaskBoard(){
        taskQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void pushJob(Job job) {
        try {
            taskQueue.put(job);
        } catch (InterruptedException e) {
            System.out.println("Failed at inserting in task queue");
            e.printStackTrace();
        }
    }

    @Override
    public Job popJob() {
        try {
            return taskQueue.take();
        } catch (InterruptedException e) {
            System.out.println("Error at getting items from queue");
            e.printStackTrace();
        }

        return null;
    }
}
