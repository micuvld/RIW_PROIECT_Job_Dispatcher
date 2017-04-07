package board;


import job.Job;

import java.util.concurrent.LinkedBlockingQueue;

/**
 *
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
