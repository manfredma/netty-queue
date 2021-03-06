package org.mitallast.queue.raft.log;

public class DescriptorException extends RaftLogException {

    public DescriptorException(String message, Object... args) {
        super(String.format(message, args));
    }

    public DescriptorException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }

    public DescriptorException(Throwable cause) {
        super(cause);
    }

}
