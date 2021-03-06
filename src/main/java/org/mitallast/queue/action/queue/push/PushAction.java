package org.mitallast.queue.action.queue.push;

import com.google.inject.Inject;
import org.mitallast.queue.action.AbstractAction;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.queue.QueueMessageUuidDuplicateException;
import org.mitallast.queue.queue.transactional.TransactionalQueueService;
import org.mitallast.queue.queues.QueueMissingException;
import org.mitallast.queue.queues.transactional.TransactionalQueuesService;
import org.mitallast.queue.transport.TransportController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class PushAction extends AbstractAction<PushRequest, PushResponse> {

    private final TransactionalQueuesService queuesService;

    @Inject
    public PushAction(Settings settings, TransportController controller, TransactionalQueuesService queuesService) {
        super(settings, controller);
        this.queuesService = queuesService;
    }

    @Override
    protected void executeInternal(PushRequest request, CompletableFuture<PushResponse> listener) {
        TransactionalQueueService queueService = queuesService.queue(request.queue());
        if (queueService == null) {
            listener.completeExceptionally(new QueueMissingException(request.queue()));
            return;
        }
        try {
            queueService.push(request.message());
            listener.complete(PushResponse.builder()
                .setMessageUUID(request.message().getUuid())
                .build());
        } catch (QueueMessageUuidDuplicateException | IOException e) {
            listener.completeExceptionally(e);
        }
    }
}