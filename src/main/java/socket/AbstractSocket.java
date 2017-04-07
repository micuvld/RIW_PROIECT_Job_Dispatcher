package socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import socket.commands.AbstractCommand;
import socket.commands.CommandFactory;
import socket.commands.UnableToProcessCommandException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

/**
 * Abstract class that defines a generic socket
 * Implements methods for:
 *  - handshacking with the server
 *  - writing to socket output (details below)
 *  - reading from socket input (details below)
 *  - processing a specific command
 *
 * Writing and reading to/from socket's in/out stream is done
 * using a specific protocol:
 *  - every message starts with a start line: "START\n"
 *  - every message ends with a end line: "END\n"
 *
 * Created by vlad on 28.03.2017.
 */
public abstract class AbstractSocket implements ISocket {
    Socket socket;
    protected BufferedReader socketReader;
    protected DataOutputStream socketWriter;
    protected SocketType socketType;
    ObjectMapper objectMapper = new ObjectMapper();

    public AbstractSocket(String hostname, int port) {
        try {
            socket = new Socket(hostname,port);
            socketReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            socketWriter = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error at initializing socket!");
            e.printStackTrace();
        }
    }

    public AbstractSocket(Socket socket) {
        try {
            this.socket = socket;
            socketReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            socketWriter = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error at initializing socket!");
            e.printStackTrace();
        }
    }

    /**
     * Identifies self to the server and waits for
     * identification result
     * @return
     *  handshake result
     */
    public boolean doHandShake() {
        try {
            this.writeString(socketType.name());
            return (readSocketLines().equals("OK"));
        } catch (IOException e) {
            System.out.println("Handshake failed!");
            e.printStackTrace();
        }

        return false;
    }

    public void writeString(String s) throws IOException {
        socketWriter.writeBytes("START\n" + s + "\nEND\n");
        socketWriter.flush();
    }

    public void writeAsJson(Object obj) throws IOException {
        socketWriter.writeBytes("START\n" + objectMapper.writeValueAsString(obj) + "\nEND\n");
        socketWriter.flush();
    }

    /**
     * Reads a message from socket, using the specific protocol
     * @return
     *  received message, from socket's input stream
     * @throws IOException
     */
    public String readSocketLines() throws IOException {
        String line;
        while (!(socketReader.readLine()).equals("START"));

        StringBuilder stringBuilder = new StringBuilder();
        while(!(line = socketReader.readLine()).equals("END")) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }

    protected void processCommand(String command) throws IOException {
        AbstractCommand commandObject = CommandFactory.createCommand(command);
        try {
            if (commandObject != null) {
                commandObject.processCommand(this);
            } else {
                throw new UnableToProcessCommandException("Wrong command type");
            }
        } catch (UnableToProcessCommandException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
