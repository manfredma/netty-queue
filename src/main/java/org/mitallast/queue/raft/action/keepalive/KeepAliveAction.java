package org.mitallast.queue.raft.action.keepalive;

import com.google.inject.Inject;
import org.mitallast.queue.action.AbstractAction;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.raft.state.RaftStateContext;
import org.mitallast.queue.raft.util.ExecutionContext;
import org.mitallast.queue.transport.TransportController;

import java.util.concurrent.CompletableFuture;

public class KeepAliveAction extends AbstractAction<KeepAliveRequest, KeepAliveResponse> {

    private final RaftStateContext context;
    private final ExecutionContext executionContext;

    @Inject
    public KeepAliveAction(Settings settings, TransportController controller, RaftStateContext context, ExecutionContext executionContext) {
        super(settings, controller);
        this.context = context;
        this.executionContext = executionContext;
    }

    @Override
    protected void executeInternal(KeepAliveRequest request, CompletableFuture<KeepAliveResponse> listener) {
        executionContext.execute("keep alive request handler", () -> {
            context.raftState().keepAlive(request).whenComplete((response, error) -> {
                if (error == null) {
                    listener.complete(response);
                } else {
                    listener.completeExceptionally(error);
                }
            });
        });
    }
}
