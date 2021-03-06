package org.mitallast.queue.action.queue.pop;

import com.google.inject.Inject;
import org.mitallast.queue.action.AbstractAction;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.queue.QueueMessage;
import org.mitallast.queue.queue.transactional.TransactionalQueueService;
import org.mitallast.queue.queues.QueueMissingException;
import org.mitallast.queue.queues.transactional.TransactionalQueuesService;
import org.mitallast.queue.transport.TransportController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class PopAction extends AbstractAction<PopRequest, PopResponse> {

    private final TransactionalQueuesService queuesService;

    @Inject
    public PopAction(Settings settings, TransportController controller, TransactionalQueuesService queuesService) {
        super(settings, controller);
        this.queuesService = queuesService;
    }

    @Override
    protected void executeInternal(PopRequest request, CompletableFuture<PopResponse> listener) {
        if (!queuesService.hasQueue(request.queue())) {
            listener.completeExceptionally(new QueueMissingException(request.queue()));
            return;
        }
        TransactionalQueueService queueService = queuesService.queue(request.queue());
        try {
            QueueMessage message = queueService.lockAndPop();
            listener.complete(PopResponse.builder()
                .setMessage(message)
                .build());
        } catch (IOException e) {
            listener.completeExceptionally(e);
        }
    }
}