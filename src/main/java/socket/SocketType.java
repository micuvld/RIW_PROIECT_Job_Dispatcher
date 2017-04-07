package socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import job.Job;
import job.JobState;
import job.JobType;
import socket.commands.RequestJobCommand;
import socket.commands.ResponseJobCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vlad on 28.03.2017.
 */
public enum SocketType {
    MASTER,
    WORKER,
    BOARD;
}
