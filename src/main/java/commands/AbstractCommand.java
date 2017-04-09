package commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import socket.ISocket;

/**
 * Commands are used to communicate between the system's entities (job_board, master, workers)
 * Created by vlad on 28.03.2017.
 */
public abstract class AbstractCommand {
    @JsonProperty("type")
    CommandType type;

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    /**
     * Processes the command according to the command type and specifications
     * @param socket
     *  - the socket that received the command
     * @throws UnableToProcessCommandException
     * @throws JsonProcessingException
     */
    public void processCommand(ISocket socket) throws UnableToProcessCommandException, JsonProcessingException {

    }
}
