package org.mitallast.queue.transport;

import org.mitallast.queue.action.ActionRequest;
import org.mitallast.queue.action.ActionResponse;
import org.mitallast.queue.transport.netty.codec.TransportFrame;

import java.util.concurrent.CompletableFuture;

public interface TransportClient {

    CompletableFuture<TransportFrame> send(TransportFrame frame);

    <Request extends ActionRequest, Response extends ActionResponse>
    CompletableFuture<Response> send(Request request);

    <Request extends ActionRequest, Response extends ActionResponse>
    CompletableFuture<Response> sendRaw(Request request);
}