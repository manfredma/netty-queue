package org.mitallast.queue.transport.netty;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import org.mitallast.queue.action.ActionRequest;
import org.mitallast.queue.action.ActionResponse;
import org.mitallast.queue.common.Immutable;
import org.mitallast.queue.common.builder.EntryBuilder;
import org.mitallast.queue.common.concurrent.Futures;
import org.mitallast.queue.common.concurrent.NamedExecutors;
import org.mitallast.queue.common.netty.NettyClientBootstrap;
import org.mitallast.queue.common.settings.Settings;
import org.mitallast.queue.common.stream.StreamService;
import org.mitallast.queue.transport.*;
import org.mitallast.queue.transport.netty.codec.StreamableTransportFrame;
import org.mitallast.queue.transport.netty.codec.TransportFrame;
import org.mitallast.queue.transport.netty.codec.TransportFrameDecoder;
import org.mitallast.queue.transport.netty.codec.TransportFrameEncoder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class NettyTransportService extends NettyClientBootstrap implements TransportService {
    private final static AttributeKey<AtomicLong> flushCounterAttr = AttributeKey.valueOf("flushCounter");
    private final static AttributeKey<ConcurrentMap<Long, CompletableFuture>> responseMapAttr = AttributeKey.valueOf("responseMapAttr");
    private final ReentrantLock connectionLock;
    private final int channelCount;
    private final TransportServer transportServer;
    private final StreamService streamService;
    private final List<TransportListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService;
    private volatile ImmutableMap<HostAndPort, NodeChannel> connectedNodes;

    @Inject
    public NettyTransportService(Settings settings, TransportServer transportServer, StreamService streamService) {
        super(settings, TransportService.class, TransportModule.class);
        this.transportServer = transportServer;
        this.streamService = streamService;
        channelCount = componentSettings.getAsInt("channel_count", Runtime.getRuntime().availableProcessors());
        connectedNodes = ImmutableMap.of();
        connectionLock = new ReentrantLock();
        executorService = NamedExecutors.newSingleThreadPool("reconnect");
    }

    @Override
    protected ChannelInitializer channelInitializer() {
        return new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new TransportFrameDecoder(streamService));
                pipeline.addLast(new TransportFrameEncoder(streamService));
                pipeline.addLast(new SimpleChannelInboundHandler<TransportFrame>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    protected void channelRead0(ChannelHandlerContext ctx, TransportFrame frame) throws Exception {
                        CompletableFuture future = ctx.attr(responseMapAttr).get().remove(frame.request());
                        if (future == null) {
                            logger.warn("future not found");
                        } else {
                            if (frame instanceof StreamableTransportFrame) {
                                EntryBuilder<ActionResponse> builder = ((StreamableTransportFrame) frame).message();
                                future.complete(builder.build());
                            } else {
                                future.complete(frame);
                            }
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        logger.error("unexpected exception {}", ctx, cause);
                        ctx.close();
                    }
                });
            }
        };
    }

    @Override
    protected void doStop() throws IOException {
        List<Runnable> tasks = executorService.shutdownNow();
        logger.warn("not executed tasks {}", tasks);
        ImmutableMap<HostAndPort, NodeChannel> connectedNodes = this.connectedNodes;
        connectedNodes.keySet().forEach(this::disconnectFromNode);
        super.doStop();
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public HostAndPort localAddress() {
        return transportServer.localAddress();
    }

    @Override
    public DiscoveryNode localNode() {
        return transportServer.localNode();
    }

    @Override
    public void connectToNode(HostAndPort address) {
        checkIsStarted();
        if (address == null) {
            throw new IllegalArgumentException("can't connect to null address");
        }
        if (address.equals(localAddress())) {
            logger.debug("connect to local node");
            return;
        }
        boolean connected = false;
        connectionLock.lock();
        try {
            if (connectedNodes.get(address) != null) {
                return;
            }
            NodeChannel nodeChannel = new NodeChannel(address);
            connectedNodes = Immutable.compose(connectedNodes, address, nodeChannel);
            nodeChannel.open();
            connected = true;
            logger.info("connected to node {}", address);
        } finally {
            connectionLock.unlock();
            if (connected) {
                listeners.forEach(listener -> listener.connected(address));
            }
        }
    }

    @Override
    public void disconnectFromNode(HostAndPort address) {
        if (address == null) {
            throw new IllegalArgumentException("can't disconnect from null node");
        }
        if (address.equals(localAddress())) {
            throw new IllegalArgumentException("can't disconnect from local node");
        }
        boolean disconnected = false;
        connectionLock.lock();
        try {
            NodeChannel nodeChannel = connectedNodes.get(address);
            if (nodeChannel == null) {
                return;
            }
            logger.info("disconnect from node {}", address);
            nodeChannel.close();
            connectedNodes = Immutable.subtract(connectedNodes, address);
            disconnected = true;
        } finally {
            connectionLock.unlock();
            if (disconnected) {
                listeners.forEach(listener -> listener.disconnected(address));
            }
        }
    }

    @Override
    public TransportClient client() {
        return transportServer.localClient();
    }

    @Override
    public TransportClient client(HostAndPort address) {
        if (address.equals(localAddress())) {
            return transportServer.localClient();
        } else {
            NodeChannel nodeChannel = connectedNodes.get(address);
            if (nodeChannel == null) {
                throw new IllegalArgumentException("Not connected to node: " + address);
            }
            return nodeChannel;
        }
    }

    @Override
    public void addListener(TransportListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(TransportListener listener) {
        listeners.remove(listener);
    }

    private class NodeChannel implements TransportClient, Closeable {
        private final HostAndPort address;
        private final AtomicLong channelRequestCounter = new AtomicLong();
        private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Channel[] channels;

        private NodeChannel(HostAndPort address) {
            this.address = address;
            this.channels = new Channel[channelCount];
        }

        public synchronized void open() {
            logger.debug("connect to {}", address);
            ChannelFuture[] channelFutures = new ChannelFuture[channelCount];
            for (int i = 0; i < channelCount; i++) {
                channelFutures[i] = connect(address);
            }
            logger.debug("await channel open {}", address);
            for (int i = 0; i < channelCount; i++) {
                try {
                    channels[i] = channelFutures[i]
                        .awaitUninterruptibly()
                        .channel();
                    initChannel(channels[i]);
                } catch (Throwable e) {
                    logger.error("error connect to {}", address, e);
                    if (reconnectScheduled.compareAndSet(false, true)) {
                        executorService.execute(this::reconnect);
                    }
                }
            }
        }

        public synchronized void reconnect() {
            if (closed.get()) {
                return;
            }
            logger.warn("reconnect to {}", address);
            for (int i = 0; i < channels.length; i++) {
                if (channels[i] == null || !channels[i].isOpen()) {
                    try {
                        channels[i] = connect(address)
                            .awaitUninterruptibly()
                            .channel();
                        initChannel(channels[i]);
                    } catch (Throwable e) {
                        logger.error("error reconnect to {}", address, e);
                    }
                }
            }
            reconnectScheduled.set(false);
        }

        private void initChannel(Channel newChannel) {
            newChannel.attr(responseMapAttr).set(new ConcurrentHashMap<>());
            newChannel.attr(flushCounterAttr).set(new AtomicLong());
            newChannel.closeFuture().addListener(future -> {
                ConcurrentMap<Long, CompletableFuture> futures = newChannel.attr(responseMapAttr).get();
                Iterator<Map.Entry<Long, CompletableFuture>> iterator = futures.entrySet().iterator();
                while (iterator.hasNext()) {
                    iterator.next().getValue().completeExceptionally(new IOException("channel is closed"));
                    iterator.remove();
                }
            });
        }

        @Override
        public synchronized void close() {
            closed.set(true);
            List<ChannelFuture> closeFutures = new ArrayList<>(channels.length);
            for (Channel channel : channels) {
                if (channel != null && channel.isOpen()) {
                    channel.flush();
                    closeFutures.add(channel.close());
                }
            }
            for (ChannelFuture closeFuture : closeFutures) {
                try {
                    closeFuture.awaitUninterruptibly();
                } catch (Exception e) {
                    //ignore
                }
            }
        }

        @Override
        public CompletableFuture<TransportFrame> send(TransportFrame frame) {
            Channel channel = channel((int) frame.request());
            if (channel == null) {
                return Futures.completeExceptionally(new IOException("channel is closed"));
            }
            CompletableFuture<TransportFrame> future = Futures.future();
            channel.attr(responseMapAttr).get().put(frame.request(), future);
            AtomicLong channelFlushCounter = channel.attr(flushCounterAttr).get();
            channelFlushCounter.incrementAndGet();
            channel.write(frame, channel.voidPromise());
            channel.pipeline().lastContext().executor().execute(() -> {
                if (channelFlushCounter.decrementAndGet() == 0) {
                    if (channel.isOpen()) {
                        channel.flush();
                    }
                }
            });
            return future;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse>
        CompletableFuture<Response> sendRaw(Request request) {
            long requestId = channelRequestCounter.incrementAndGet();
            Channel channel = channel((int) requestId);
            if (channel == null) {
                return Futures.completeExceptionally(new IOException("channel is closed"));
            }
            CompletableFuture<Response> future = Futures.future();
            StreamableTransportFrame frame = StreamableTransportFrame.of(requestId, request.toBuilder());
            channel.attr(responseMapAttr).get().put(requestId, future);
            AtomicLong channelFlushCounter = channel.attr(flushCounterAttr).get();
            channelFlushCounter.incrementAndGet();
            channel.write(frame, channel.voidPromise());
            channel.pipeline().lastContext().executor().execute(() -> {
                if (channelFlushCounter.decrementAndGet() == 0) {
                    if (channel.isOpen()) {
                        channel.flush();
                    }
                }
            });
            return future;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse>
        CompletableFuture<Response> send(Request request) {
            CompletableFuture<Response> future = Futures.future();
            this.<Request, Response>sendRaw(request).whenComplete((response, error) -> {
                if (error != null) {
                    future.completeExceptionally(error);
                } else if (response.hasError()) {
                    future.completeExceptionally(response.error());
                } else {
                    future.complete(response);
                }
            });
            return future;
        }

        private Channel channel(int request) {
            int index = Math.abs(request % channels.length);
            int loopIndex = index;
            do {
                index--;
                if (index < 0) {
                    index += channels.length;
                }
                if (channels[index] != null && channels[index].isOpen()) {
                    return channels[index];
                } else if (reconnectScheduled.compareAndSet(false, true)) {
                    executorService.execute(this::reconnect);
                }
            } while (index != loopIndex);
            return null;
        }
    }
}
