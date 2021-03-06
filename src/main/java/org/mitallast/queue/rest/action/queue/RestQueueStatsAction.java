package org.mitallast.queue.rest.action.queue;

import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.mitallast.queue.action.queue.stats.QueueStatsRequest;
import org.mitallast.queue.action.queue.stats.QueueStatsResponse;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.common.xstream.XStreamBuilder;
import org.mitallast.queue.queues.stats.QueueStats;
import org.mitallast.queue.rest.BaseRestHandler;
import org.mitallast.queue.rest.RestController;
import org.mitallast.queue.rest.RestRequest;
import org.mitallast.queue.rest.RestSession;
import org.mitallast.queue.rest.response.ByteBufRestResponse;
import org.mitallast.queue.transport.TransportService;

import java.io.IOException;

public class RestQueueStatsAction extends BaseRestHandler {
    private final TransportService transportService;

    @Inject
    public RestQueueStatsAction(Settings settings, RestController controller, TransportService transportService) {
        super(settings);
        this.transportService = transportService;
        controller.registerHandler(HttpMethod.GET, "/{queue}/_stats", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestSession session) {
        QueueStatsRequest queueStatsRequest = QueueStatsRequest.builder()
            .setQueue(request.param("queue").toString())
            .build();

        transportService.client().<QueueStatsRequest, QueueStatsResponse>send(queueStatsRequest)
            .whenComplete((response, error) -> {
                if (error == null) {
                    QueueStats queueStats = response.stats();
                    ByteBuf buffer = Unpooled.buffer();
                    try {
                        try (XStreamBuilder builder = createBuilder(request, buffer)) {
                            builder.writeStartObject();
                            builder.writeStringField("name", queueStats.getQueue().getName());
                            builder.writeNumberField("size", queueStats.getSize());
                            builder.writeEndObject();
                        }
                        session.sendResponse(new ByteBufRestResponse(HttpResponseStatus.OK, buffer));
                    } catch (IOException e) {
                        session.sendResponse(e);
                    }
                } else {
                    session.sendResponse(error);
                }
            });
    }
}
