package monitor;

import socket.ISocket;
import commands.AbstractCommand;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class used by MasterServerSocket to manage responses
 * sent from workers to master.
 * Created by vlad on 07.04.2017.
 */
public class ResponseCommandsMonitor implements Runnable{
    LinkedBlockingQueue<AbstractCommand> commandsQueue = new LinkedBlockingQueue<>();
    ISocket masterSocket;

    public ResponseCommandsMonitor(LinkedBlockingQueue<AbstractCommand> commandsQueue, ISocket masterSocket) {
        this.commandsQueue = commandsQueue;
        this.masterSocket = masterSocket;
    }

    /**
     * Attempts to read from the queue and
     * redirects the commands to master
     */
    @Override
    public void run() {
        while(true) {
            try {
                AbstractCommand command = commandsQueue.take();
                masterSocket.writeAsJson(command);
            } catch (InterruptedException e) {
                System.out.println("Interrupted while taking object from commands queue!");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Failed to send response to master client!");
                e.printStackTrace();
            }
        }
    }
}
