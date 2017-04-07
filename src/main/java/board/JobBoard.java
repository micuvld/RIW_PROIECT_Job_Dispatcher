package board;

import job.Job;
import job.JobType;

/**
 * Created by vlad on 27.03.2017.
 */
public interface JobBoard {
    void pushJob(Job job);

    Job popJob();
}
