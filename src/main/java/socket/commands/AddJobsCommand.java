package socket.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import job.Job;
import job.JobType;
import socket.AbstractSocket;
import socket.ISocket;
import socket.MasterServerSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vlad on 28.03.2017.
 */
public class AddJobsCommand extends AbstractCommand{
    private static int nextJobId = 0;
    @JsonProperty("jobList")
    List<Job> jobList;

    public AddJobsCommand() {
        this.type = CommandType.ADD_JOBS;
        jobList = new ArrayList<>();
    }

    public AddJobsCommand(List<Job> jobList) {
        this.type = CommandType.ADD_JOBS;
        this.jobList = jobList;
    }

    public List<Job> getJobList() {
        return jobList;
    }

    public void setJobList(List<Job> jobList) {
        this.jobList = jobList;
    }

    @JsonCreator
    public AddJobsCommand(@JsonProperty("type") CommandType type,
                          @JsonProperty("jobList") List<Job> jobList) {
        this.type = type;
        this.jobList = jobList;
    }

    public Job addJobGenerateIndex(JobType jobType, String target) {
        Job job = new Job(nextJobId++, jobType, target);
        jobList.add(job);
        System.out.println(job.getJobId());
        return job;
    }

    public Job addJobGenerateIndex(Job job) {
        job.setJobId(nextJobId++);
        jobList.add(job);
        System.out.println(job.getJobId());
        return job;
    }

    public Job addJobKeepIndex(Job job) {
        jobList.add(job);
        System.out.println(job.getJobId());
        return job;
    }

    @Override
    public void processCommand(ISocket socket) throws UnableToProcessCommandException {
        if (socket instanceof MasterServerSocket) {
            for (Job job : jobList) {
                ((MasterServerSocket) socket).getJobBoard().pushJob(job);
            }
        } else {
            throw new UnableToProcessCommandException("Not a MasterServerSocket");
        }
    }
}

