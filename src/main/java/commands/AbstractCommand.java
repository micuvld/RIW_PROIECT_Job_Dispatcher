package commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import socket.ISocket;

/**
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

    public void processCommand(ISocket socket) throws UnableToProcessCommandException, JsonProcessingException {

    }
}
