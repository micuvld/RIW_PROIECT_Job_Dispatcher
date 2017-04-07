package socket;

import java.io.IOException;

/**
 * Created by vlad on 29.03.2017.
 */
public interface ISocket {
    public boolean doHandShake();

    public void writeString(String s) throws IOException;

    public void writeAsJson(Object obj) throws IOException;

    public String readSocketLines() throws IOException;
}
