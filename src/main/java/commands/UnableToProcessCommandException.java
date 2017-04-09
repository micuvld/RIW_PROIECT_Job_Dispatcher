package commands;

/**
 * Created by vlad on 29.03.2017.
 */
public class UnableToProcessCommandException extends Exception{
    public UnableToProcessCommandException(String s) {
        super(s);
    }

    public UnableToProcessCommandException(String s, Throwable e) {
        super(s, e);
    }
}
