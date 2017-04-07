package socket;

import board.JobBoard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import job.Job;
import monitor.ResponseCommandsMonitor;
import socket.commands.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by vlad on 28.03.2017.
 */
public class MasterServerSocket extends AbstractSocket implements Runnable{
    private JobBoard jobBoard;
    public LinkedBlockingQueue<AbstractCommand> responseCommands = new LinkedBlockingQueue<>();

    public JobBoard getJobBoard() {
        return jobBoard;
    }

    public MasterServerSocket(String hostname, int port) {
        super(hostname, port);
    }

    public MasterServerSocket(Socket clientSocket, JobBoard jobBoard) {
        super(clientSocket);
        this.jobBoard = jobBoard;
        new Thread(new ResponseCommandsMonitor(responseCommands, this)).start();
    }

    @Override
    public void run() {
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

    public void sendResponseToMasterClient(final AbstractCommand command) throws IOException {
        try {
            writeAsJson(command);
        } catch (IOException e) {
            System.out.println("Failed to send response to master client.");
            e.printStackTrace();
        }
    }

    public void pushResponseCommand(AbstractCommand command) throws InterruptedException {
        responseCommands.put(command);
    }
}