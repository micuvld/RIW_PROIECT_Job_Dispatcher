package socket;

import board.JobBoard;
import board.TaskBoard;
import com.mongodb.Mongo;
import mongo.MongoConnector;
import utils.Configs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vlad on 28.03.2017.
 */
public class JobBoardSocket{
    private ServerSocket serverSocket;
    private int port = 8080;

    private MasterServerSocket masterServerSocket;
    private List<WorkerServerSocket> workers = new ArrayList<WorkerServerSocket>();
    private int workerId = 0;

    private JobBoard jobBoard;

    public JobBoardSocket() {
        try {
            port = Integer.parseInt(Configs.JOB_BOARD_PORT);
            jobBoard = new TaskBoard();
            serverSocket = new ServerSocket(port);

            System.out.println("Server initiated at " + serverSocket.getInetAddress() + ":" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void work() {
        try {
            while (true) {
                System.out.println("Waiting for connections");
                Socket clientSocket = serverSocket.accept();

                addClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addClient(Socket clientSocket) throws IOException {
        BufferedReader socketReader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
        String clientType = readSocketLine(socketReader);

        if (clientType.equals("MASTER")) {
            System.out.println("Master connected!");

            masterServerSocket = new MasterServerSocket(clientSocket, jobBoard);
            masterServerSocket.writeString("OK");
            setWorkersMasterSocket();

            Thread masterThread = new Thread(masterServerSocket);
            masterThread.start();
        } else if(clientType.equals("WORKER")){
            System.out.println("Worker" + workerId + " connected!");

            WorkerServerSocket workerSocket = new WorkerServerSocket(clientSocket, jobBoard, masterServerSocket);
            workerSocket.writeString("OK");

            Thread workerThread = new Thread(workerSocket);
            workerThread.start();

            workers.add(workerSocket);
        }
    }

    public void setWorkersMasterSocket() {
        for (WorkerServerSocket worker : workers) {
            worker.setMasterSocket(masterServerSocket);
        }
    }

    public String readSocketLine(BufferedReader socketReader) throws IOException {
        while(!(socketReader.readLine()).equals("START"));
        return socketReader.readLine();
    }
}
