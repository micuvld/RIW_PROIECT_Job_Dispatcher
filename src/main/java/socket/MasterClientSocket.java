package socket;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import indexers.DirectoryParser;
import job.Job;
import job.JobState;
import job.JobType;
import mongo.MongoConnector;
import monitor.TaskResultsMonitor;
import org.bson.Document;
import search.DocumentScore;
import search.Search;
import commands.AddJobsCommand;
import utils.Configs;
import utils.MenuOption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents the Master entity that connects to the job board,
 * sends commands and processes feedback.
 * It has three lists that help managing the sent jobs, succeeded jobs and failed jobs.
 * It uses a TaskResultsMonitor to redispatch the failed jobs
 * Created by vlad on 28.03.2017.
 */
public class MasterClientSocket extends AbstractSocket {
    private List<Job> sentJobs = new ArrayList<>();
    private LinkedBlockingQueue<Job> failedJobs = new LinkedBlockingQueue<>();
    private List<Job> succeededJobs = new ArrayList<>();

    private boolean processingInProgress = true;

    public MasterClientSocket(String hostname, int port) {
        super(hostname,port);
        socketType = SocketType.MASTER;
        new Thread(new TaskResultsMonitor(this)).start();
    }

    public LinkedBlockingQueue<Job> getFailedJobs() {
        return failedJobs;
    }

    /**
     * According to the input option it:
     *  1. initiates file indexing of the configured folder
     *  2. serves as search engine
     *  3. exits...
     * @param folderPath
     * @throws IOException
     */
    public void work(String folderPath) throws IOException {
        MenuOption option = readOption();
        System.out.println(option);

        switch (option) {
            case INDEX_FILES:
                processingInProgress = true;
                cleanDatabases();
                createTempDir();
                sendFilesToMap(folderPath);

                while (processingInProgress) {
                    System.out.println("Waiting for feedback...");

                    String command = getFeedback();
                    processFeedback(command);
                }

                System.out.println("Finished indexing files!");
                System.out.println("Exiting...");
                socket.close();
                break;
            case SEARCH:
                Search search = new Search();
                while(true) {
                    String searchQuery = readQuery();
                    if(searchQuery.equals("0")) {
                        socket.close();
                        return;
                    }

                    List<DocumentScore> documentScores = search.rankedSearch(searchQuery);
                    printFirstDocuments(documentScores, Configs.SEARCH_DOCUMENTS_NUMBER_LIMIT);
                }
            case EXIT:
                System.out.println("Exiting...");
                socket.close();
                return;
            default:
                System.out.println("Wrong option");
        }
    }

    public void cleanDatabases() {
        MongoConnector.dropDatabase("RIW");
        MongoConnector.dropDatabase("DirectIndex");
        MongoConnector.dropDatabase("InvertedIndex");
    }

    public void createTempDir() {
        new File(Configs.TEMPDIR_PATH).mkdir();
    }

    public MenuOption readOption() {
        System.out.println("1. Index files\n2. Search\n3. Exit");
        Scanner scanner = new Scanner(System.in);
        return MenuOption.values()[scanner.nextInt() - 1];
    }

    public String readQuery() {
        System.out.print("\nSearch (0 to exit): ");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    /**
     * Waits and reads the feedback for sent jobs
     * @return
     * @throws IOException
     */
    public String getFeedback() throws IOException {
        return readSocketLines();
    }

    /**
     * Processes the feedback according to the command type
     * @param command
     * @throws IOException
     */
    public void processFeedback(String command) throws IOException {
        System.out.println(command);
        processCommand(command);
    }

    /**
     * Processes the feedback according to the job type
     * - it usually sends a job that would follow the previous job
     * - it waits for all the sent jobs when needed and
     *   it sends a couple of new jobs for the next phase
     * @param job
     */
    public void processCommandResponse(Job job) {
        printJobsStats();
        addJobToMonitoringList(job);

        switch (job.getJobType()) {
            case MAP:
                sendJob(job, JobType.REDUCE_DIRECT_INDEX);
                break;
            case REDUCE_DIRECT_INDEX:
                if (sentJobsAreDone()) {
                    sendCollectionsToSort();
                }
                break;
            case SORT:
                sendJob(job, JobType.REDUCE_INVERTED_INDEX);
                break;
            case REDUCE_INVERTED_INDEX:
                if (sentJobsAreDone()) {
                    sendFilesForNormCalculation();
                }
                break;
            case CALCULATE_NORMS:
                if (sentJobsAreDone()) {
                    endIndexing();
                }
                break;
        }
    }

    public void addJobToMonitoringList(Job job) {
        switch(job.getState()) {
            case COMPLETED:
                succeededJobs.add(job);
                break;
            case ABORTED:
                failedJobs.add(job);
                break;
        }
    }

    public void sendJob(Job job, JobType jobType) {
        AddJobsCommand command = new AddJobsCommand();
        Job jobToSend = new Job(job);

        jobToSend.setJobType(jobType);
        command.addJobKeepId(jobToSend);
        try {
            this.writeAsJson(command);
            sentJobs.add(job);
        } catch (IOException e) {
            System.out.println("Error at sending job #" + job.getJobId() +" for: " + job.getTargets());
            e.printStackTrace();
        }
    }

    public void sendFilesToMap(String folderPath) throws IOException {
        List<String> pathsList = new ArrayList<>();
        DirectoryParser directoryParser = new DirectoryParser();

        try {
            directoryParser.parseDirectory(Paths.get(folderPath), pathsList);
        } catch (IOException e) {
            System.out.println("Error when parsing directory!");
            e.printStackTrace();
        }

        Job job;

        AddJobsCommand addJobsCommand = new AddJobsCommand();
        for (String path : pathsList) {
            job = new Job(JobType.MAP, path, JobState.PENDING);
            addJobsCommand.addJobGenerateId(job);
            sentJobs.add(job);
        }

        this.writeAsJson(addJobsCommand);
    }

    public void sendCollectionsToSort() {
        List<String> collections = MongoConnector.getCollections("DirectIndex");

        Job job;
        AddJobsCommand addJobsCommand = new AddJobsCommand();
        for (String collection : collections) {
            job = new Job(JobType.SORT, collection, JobState.PENDING);
            addJobsCommand.addJobGenerateId(job);
            sentJobs.add(job);
        }

        try {
            this.writeAsJson(addJobsCommand);
        } catch (IOException e) {
            System.out.println("Failed to send collections to sort!");
            e.printStackTrace();
        }
    }

    public void sendFilesForNormCalculation() {
        MongoCollection<Document> indexedFilesCollection = MongoConnector.getCollection(
                "RIW", "indexedFiles");

        MongoCursor<Document> cursor = indexedFilesCollection.find().iterator();
        Job job;
        AddJobsCommand addJobsCommand = new AddJobsCommand();

        while(cursor.hasNext()) {
            Document file = cursor.next();

            job = new Job(JobType.CALCULATE_NORMS, file.getString("file"), JobState.PENDING);
            addJobsCommand.addJobGenerateId(job);
            sentJobs.add(job);
        }

        try {
            this.writeAsJson(addJobsCommand);
        } catch (IOException e) {
            System.out.println("Failed to send files for norms!");
            e.printStackTrace();
        }
    }

    public boolean sentJobsAreDone() {
        return sentJobs.size() == succeededJobs.size();
    }

    public void endIndexing() {
        processingInProgress = false;
    }

    public void printJobsStats() {
        System.out.println("Sent jobs: " + sentJobs.size());
        System.out.println("Succeeded jobs: " + succeededJobs.size());
    }

    public void printFirstDocuments(List<DocumentScore> documentScores, int numberOfDocuments) {
        int resultCount = 0;
        for (DocumentScore documentScore : documentScores) {
            if (resultCount == numberOfDocuments) {
                break;
            }
            System.out.println(documentScore.getFileName() + ": " + documentScore.getScore());
            resultCount++;
        }
    }
}