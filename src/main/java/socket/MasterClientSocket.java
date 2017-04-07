package socket;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import indexers.DirectoryParser;
import job.Job;
import job.JobState;
import job.JobType;
import mongo.MongoConnector;
import org.bson.Document;
import search.DocumentScore;
import search.SearchWorker;
import socket.commands.AddJobsCommand;
import utils.MenuOption;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by vlad on 28.03.2017.
 */
public class MasterClientSocket extends AbstractSocket {
    private final int SEARCH_DOCUMENTS_NUMBER_LIMIT = 10;
    List<Job> sentJobs = new ArrayList<>();
    List<Job> failedJobs = new ArrayList<>();
    List<Job> succeededJobs = new ArrayList<>();

    boolean processingInProgress = true;

    public MasterClientSocket(String hostname, int port) throws IOException {
        super(hostname,port);
        socketType = SocketType.MASTER;
    }

    public void work(String folderPath) throws IOException {
        MenuOption option = readOption();
        System.out.println(option);
        switch (option) {
            case INDEX_FILES:
                processingInProgress = true;
                cleanDatabases();
                sendFilesToMap(folderPath);

                while(processingInProgress) {
                    System.out.println("Waiting for feedback...");

                    String command = getFeedback();
                    processFeedback(command);
                }
                break;
            case SEARCH:
                processingInProgress = true;
                String searchQuery = readQuery();
                SearchWorker searchWorker = new SearchWorker();

                List<DocumentScore> documentScores = searchWorker.rankedSearch(searchQuery);
                printFirstDocuments(documentScores, SEARCH_DOCUMENTS_NUMBER_LIMIT);
                break;
            case EXIT:
                break;
            default:
                System.out.println("Wrong option");
        }


    }

    public MenuOption readOption() {
        System.out.println("1. Index files\n2. Search\n3.Exit");

        Scanner scanner = new Scanner(System.in);

        return MenuOption.values()[scanner.nextInt() - 1];
    }

    public void cleanDatabases() {
        MongoConnector.dropDatabase("RIW");
        MongoConnector.dropDatabase("DirectIndex");
        MongoConnector.dropDatabase("InvertedIndex");
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
            addJobsCommand.addJobGenerateIndex(job);
            sentJobs.add(job);
        }

        this.writeAsJson(addJobsCommand);
    }

    public String getFeedback() throws IOException {
        return readSocketLines();
    }

    public void processFeedback(String command) throws IOException {
        System.out.println(command);
        processCommand(command);
    }

    public void processJobResponse(Job job) {
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
        Job reduceJob = new Job(job);

        reduceJob.setJobType(jobType);
        command.addJobKeepIndex(reduceJob);
        try {
            this.writeAsJson(command);
            sentJobs.add(job);
        } catch (IOException e) {
            System.out.println("Error at sending job #" + job.getJobId() +" for: " + job.getTargets());
            e.printStackTrace();
        }
    }

    public boolean sentJobsAreDone() {
        return sentJobs.size() == succeededJobs.size();
    }

    public void printJobsStats() {
        System.out.println("Sent jobs: " + sentJobs.size());
        System.out.println("Succeeded jobs: " + succeededJobs.size());
    }

    public void sendCollectionsToSort() {
        List<String> collections = MongoConnector.getCollections("DirectIndex");

        Job job;
        AddJobsCommand addJobsCommand = new AddJobsCommand();
        for (String collection : collections) {
            job = new Job(JobType.SORT, collection, JobState.PENDING);
            addJobsCommand.addJobGenerateIndex(job);
            sentJobs.add(job);
        }

        try {
            this.writeAsJson(addJobsCommand);
        } catch (IOException e) {
            System.out.println("Failed to send collections to sort!");
            e.printStackTrace();
        }
    }

    public void endIndexing() {
        processingInProgress = false;
    }

    public String readQuery() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
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
            addJobsCommand.addJobGenerateIndex(job);
            sentJobs.add(job);
        }

        try {
            this.writeAsJson(addJobsCommand);
        } catch (IOException e) {
            System.out.println("Failed to send files for norms!");
            e.printStackTrace();
        }
    }

    public void printFirstDocuments(List<DocumentScore> documentScores, int numberOfDocuments) {
        int resultCount = 0;
        for (DocumentScore documentScore : documentScores) {
            if (resultCount == 10) {
                break;
            }
            System.out.println(documentScore.getFileName() + ": " + documentScore.getScore());
            resultCount++;
        }
    }
}