package org.mitallast.queue.raft.action.command;

import org.mitallast.queue.action.ActionResponse;
import org.mitallast.queue.common.builder.EntryBuilder;
import org.mitallast.queue.common.stream.StreamInput;
import org.mitallast.queue.common.stream.StreamOutput;
import org.mitallast.queue.common.stream.Streamable;
import org.mitallast.queue.common.stream.StreamableError;

import java.io.IOException;

public class CommandResponse implements ActionResponse<CommandResponse> {
    private final StreamableError error;
    private final long version;
    private final Streamable result;

    private CommandResponse(StreamableError error, long version, Streamable result) {
        this.error = error;
        this.version = version;
        this.result = result;
    }

    @Override
    public StreamableError error() {
        return error;
    }

    public long version() {
        return version;
    }

    public Streamable result() {
        return result;
    }

    @Override
    public String toString() {
        return "CommandResponse{" +
            "error=" + error +
            ", version=" + version +
            ", result=" + result +
            '}';
    }

    @Override
    public Builder toBuilder() {
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements EntryBuilder<CommandResponse> {
        private StreamableError error;
        private long version;
        private Streamable result;

        public Builder from(CommandResponse entry) {
            error = entry.error;
            version = entry.version;
            result = entry.result;
            return this;
        }

        public Builder setError(StreamableError error) {
            this.error = error;
            return this;
        }

        public Builder setVersion(long version) {
            this.version = version;
            return this;
        }

        public Builder setResult(Streamable result) {
            this.result = result;
            return this;
        }

        public CommandResponse build() {
            return new CommandResponse(error, version, result);
        }

        @Override
        public void readFrom(StreamInput stream) throws IOException {
            if (stream.readBoolean()) {
                error = stream.readError();
                return;
            }
            version = stream.readLong();
            result = stream.readStreamable();
        }

        @Override
        public void writeTo(StreamOutput stream) throws IOException {
            if (error != null) {
                stream.writeBoolean(true);
                stream.writeError(error);
                return;
            }
            stream.writeBoolean(false);
            stream.writeLong(version);
            stream.writeClass(result.getClass());
            stream.writeStreamable(result);
        }
    }

}
