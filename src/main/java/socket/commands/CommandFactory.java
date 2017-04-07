package socket.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Used to create specific commands,
 * depending on the command type found in json
 * Created by vlad on 29.03.2017.
 */
public class CommandFactory {
    private final static Map<CommandType, Class> commandClassesMap = new HashMap<>();

    static {
        commandClassesMap.put(CommandType.ADD_JOBS, AddJobsCommand.class);
        commandClassesMap.put(CommandType.REQUEST_JOB, RequestJobCommand.class);
        commandClassesMap.put(CommandType.RESPONSE_JOB, ResponseJobCommand.class);
    }

    public static AbstractCommand createCommand(String command) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(command);

        CommandType commandType = objectMapper.convertValue(node.get("type"), CommandType.class);
        if (commandClassesMap.containsKey(commandType)) {
            try {
                return (AbstractCommand) objectMapper.readValue(command, commandClassesMap.get(commandType));
            } catch (IOException e) {
                System.out.println("Error at instantiating command object");
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }
}
