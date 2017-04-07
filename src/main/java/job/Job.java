package job;

import com.fasterxml.jackson.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 27.03.2017.
 */
public class Job {
    @JsonProperty("jobId")
    int jobId;
    @JsonProperty("jobType")
    JobType jobType;
    @JsonProperty("targets")
    List<String> targets;
    @JsonProperty("state")
    JobState state;

    @JsonCreator
    public Job(@JsonProperty("jobId") int jobId,
               @JsonProperty("jobType") JobType jobType,
               @JsonProperty("targets") List<String> targets,
               @JsonProperty("state") String state) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.targets = targets;
        this.state = JobState.valueOf(state);
    }


    public Job() {

    }

    public Job(Job jobToCopy) {
        this.jobId = jobToCopy.jobId;
        this.jobType = jobToCopy.jobType;
        this.targets = new ArrayList<>(jobToCopy.targets);
        this.state = jobToCopy.state;
    }

    public Job(int jobId, JobType jobType, String target, JobState state) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.targets = new ArrayList<>();
        this.targets.add(target);
        this.state = state;
    }

    public Job(JobType jobType, String target, JobState state) {
        this.jobType = jobType;
        this.targets = new ArrayList<>();
        this.targets.add(target);
        this.state = state;
    }

    public Job(int jobId, JobType jobType, String target) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.targets = new ArrayList<>();
        this.targets.add(target);
        this.state = JobState.PENDING;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public List<String> getTargets() {
        return targets;
    }

    @JsonSetter("targets")
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public void setTarget(String target) {
        this.targets = new ArrayList<>();
        this.targets.add(target);
    }

    @JsonGetter("state")
    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
