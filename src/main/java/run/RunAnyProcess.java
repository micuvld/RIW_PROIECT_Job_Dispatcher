package run;

import socket.JobBoardSocket;
import socket.MasterClientSocket;
import socket.WorkerClientSocket;
import utils.Configs;

import java.io.IOException;

/**
 * Created by vlad on 08.04.2017.
 */
public class RunAnyProcess {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Run parameteres: job_board/master/worker configFilePath");
            return;
        }

        String processType = args[0];
        String configFilePath = args[1];

        if (entityIsValid(processType)) {
            boolean propertiesLoadedSuccessfully = Configs.loadProperties(configFilePath);

            if (propertiesLoadedSuccessfully) {
                runProcess(processType);
            } else {
                System.err.println("Failed to load properties!");
            }
        } else {
            System.err.println("Invalid process type! Valid process types: job_board, master, worker");
        }
    }

    private static boolean entityIsValid(String entity) {
        return entity.equals("job_board") || entity.equals("master") || entity.equals("worker");
    }

    private static void runProcess(String processType) {
        switch (processType) {
            case "job_board":
                JobBoardSocket jobBoardSocket = new JobBoardSocket();
                jobBoardSocket.work();
                break;
            case "master":

                MasterClientSocket masterClientSocket = new MasterClientSocket(Configs.JOB_BOARD_IP_ADDRESS,
                        Integer.parseInt(Configs.JOB_BOARD_PORT));

                if (masterClientSocket.doHandShake())  {
                    System.out.println("Handshake succeeded!");
                } else {
                    System.out.println("Handshake failed. Exiting.");
                    return;
                }

                try {
                    masterClientSocket.work(Configs.WORKDIR_PATH);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "worker":
                WorkerClientSocket workerSocket = new WorkerClientSocket(Configs.JOB_BOARD_IP_ADDRESS,
                        Integer.parseInt(Configs.JOB_BOARD_PORT));

                if (workerSocket.doHandShake()) {
                    System.out.println("Handshake succeeded!");
                } else {
                    System.out.println("Handshake failed. Exiting.");
                    return;
                }

                try {
                    workerSocket.work();
                } catch (IOException e) {
                    System.out.println("Failed to request job");
                    e.printStackTrace();
                }
                break;
        }
    }
}
