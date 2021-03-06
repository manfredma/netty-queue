package org.mitallast.queue.transport.client;

import org.junit.Assert;
import org.junit.Test;
import org.mitallast.queue.action.queue.push.PushRequest;
import org.mitallast.queue.action.queue.push.PushResponse;
import org.mitallast.queue.common.BaseQueueTest;
import org.mitallast.queue.transport.DiscoveryNode;
import org.mitallast.queue.transport.TransportServer;
import org.mitallast.queue.transport.TransportService;

public class NodeNettyTransportServiceTest extends BaseQueueTest {
    @Test
    public void testPush() throws Exception {
        createQueue();
        assertQueueEmpty();

        DiscoveryNode discoveryNode = node().injector().getInstance(TransportServer.class).localNode();

        TransportService transportService = node().injector().getInstance(TransportService.class);
        transportService.connectToNode(discoveryNode.address());

        PushResponse pushResponse = transportService.client(discoveryNode.address()).<PushRequest, PushResponse>send(
            PushRequest.builder()
                .setQueue(queueName())
                .setMessage(createMessage())
                .build())
            .get();

        Assert.assertNotNull(pushResponse.messageUUID());
    }
}
