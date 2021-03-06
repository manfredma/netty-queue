package org.mitallast.queue.raft.state;

import org.mitallast.queue.common.component.AbstractComponent;
import org.mitallast.queue.common.component.Lifecycle;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.raft.action.append.AppendRequest;
import org.mitallast.queue.raft.action.append.AppendResponse;
import org.mitallast.queue.raft.action.command.CommandRequest;
import org.mitallast.queue.raft.action.command.CommandResponse;
import org.mitallast.queue.raft.action.join.JoinRequest;
import org.mitallast.queue.raft.action.join.JoinResponse;
import org.mitallast.queue.raft.action.keepalive.KeepAliveRequest;
import org.mitallast.queue.raft.action.keepalive.KeepAliveResponse;
import org.mitallast.queue.raft.action.leave.LeaveRequest;
import org.mitallast.queue.raft.action.leave.LeaveResponse;
import org.mitallast.queue.raft.action.query.QueryRequest;
import org.mitallast.queue.raft.action.query.QueryResponse;
import org.mitallast.queue.raft.action.register.RegisterRequest;
import org.mitallast.queue.raft.action.register.RegisterResponse;
import org.mitallast.queue.raft.action.vote.VoteRequest;
import org.mitallast.queue.raft.action.vote.VoteResponse;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractState extends AbstractComponent {

    private final Lifecycle lifecycle = new Lifecycle();

    protected AbstractState(Settings settings) {
        super(settings);
    }

    public abstract RaftStateType type();

    public abstract CompletableFuture<JoinResponse> join(JoinRequest request);

    public abstract CompletableFuture<LeaveResponse> leave(LeaveRequest request);

    public abstract CompletableFuture<RegisterResponse> register(RegisterRequest request);

    public abstract CompletableFuture<KeepAliveResponse> keepAlive(KeepAliveRequest request);

    public abstract CompletableFuture<AppendResponse> append(AppendRequest request);

    public abstract CompletableFuture<VoteResponse> vote(VoteRequest request);

    public abstract CompletableFuture<CommandResponse> command(CommandRequest request);

    public abstract CompletableFuture<QueryResponse> query(QueryRequest request);

    public final Lifecycle lifecycle() {
        return lifecycle;
    }

    public final void start() {
        if (!lifecycle.canMoveToStarted()) {
            logger.warn("Can't move to started, " + lifecycle.state());
            return;
        }
        if (!lifecycle.moveToStarted()) {
            logger.warn("Don't moved to started, " + lifecycle.state());
        }
        logger.info("starting");
        startInternal();
        logger.info("started");
    }

    protected abstract void startInternal();

    public final void stop() {
        if (!lifecycle.canMoveToStopped()) {
            logger.warn("Can't move to stopped, it's a " + lifecycle.state());
            return;
        }
        lifecycle.moveToStopped();
        logger.info("stopping");
        stopInternal();
        logger.debug("stopped");
    }

    protected abstract void stopInternal();
}
