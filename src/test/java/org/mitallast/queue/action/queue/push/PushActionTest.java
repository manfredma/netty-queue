package org.mitallast.queue.action.queue.push;

import org.junit.Test;
import org.mitallast.queue.common.BaseQueueTest;
import org.mitallast.queue.queue.QueueMessage;

public class PushActionTest extends BaseQueueTest {

    @Test
    public void testSingleThread() throws Exception {
        createQueue();
        // warm up
        send(max());

        long start = System.currentTimeMillis();
        send(max());
        long end = System.currentTimeMillis();
        printQps("send", max(), start, end);
    }

    @Test
    public void testMultiThread() throws Exception {
        createQueue();
        // warm up
        send(max());

        long start = System.currentTimeMillis();
        executeConcurrent(() -> send(max()));
        long end = System.currentTimeMillis();
        printQps("send", total(), start, end);
    }

    private void send(int max) throws Exception {
        for (int i = 0; i < max; i++) {
            QueueMessage message = createMessage();
            PushRequest request = PushRequest.builder()
                .setQueue(queueName())
                .setMessage(message)
                .build();
            PushResponse response = localClient().<PushRequest, PushResponse>send(request).get();
            assert response.messageUUID().equals(message.getUuid());
        }
    }
}
