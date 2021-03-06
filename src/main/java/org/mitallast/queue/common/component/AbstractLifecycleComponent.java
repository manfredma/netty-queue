package org.mitallast.queue.common.component;

import org.mitallast.queue.common.settings.Settings;

import java.io.IOException;

public abstract class AbstractLifecycleComponent extends AbstractComponent implements LifecycleComponent {

    private final Lifecycle lifecycle = new Lifecycle();

    protected AbstractLifecycleComponent(Settings settings) {
        super(settings);
    }

    protected AbstractLifecycleComponent(Settings settings, Class customClass) {
        super(settings, customClass);
    }

    protected AbstractLifecycleComponent(Settings settings, Class loggerClass, Class componentClass) {
        super(settings, loggerClass, componentClass);
    }

    protected AbstractLifecycleComponent(Settings settings, String prefixSettings) {
        super(settings, prefixSettings);
    }

    protected AbstractLifecycleComponent(Settings settings, String prefixSettings, Class customClass) {
        super(settings, prefixSettings, customClass);
    }

    protected AbstractLifecycleComponent(Settings settings, String prefixSettings, Class loggerClass, Class componentClass) {
        super(settings, prefixSettings, loggerClass, componentClass);
    }

    @Override
    public Lifecycle lifecycle() {
        return lifecycle;
    }

    @Override
    public void checkIsStarted() {
        if (!lifecycle.started()) {
            throw new IllegalStateException(logger.getName() + " is not started");
        }
    }

    @Override
    public void checkIsStopped() {
        if (!lifecycle.started()) {
            throw new IllegalStateException(logger.getName() + " is not started");
        }
    }

    @Override
    public void checkIsClosed() {
        if (!lifecycle.started()) {
            throw new IllegalStateException(logger.getName() + " is not started");
        }
    }

    @Override
    public void start() throws IOException {
        if (!lifecycle.canMoveToStarted()) {
            logger.warn("Can't move to started, " + lifecycle.state());
            return;
        }
        if (!lifecycle.moveToStarted()) {
            logger.warn("Don't moved to started, " + lifecycle.state());
        }
        logger.info("starting");
        doStart();
        logger.info("started");
    }

    protected abstract void doStart() throws IOException;

    @Override
    public void stop() throws IOException {
        if (!lifecycle.canMoveToStopped()) {
            logger.warn("Can't move to stopped, it's a " + lifecycle.state());
            return;
        }
        lifecycle.moveToStopped();
        logger.info("stopping");
        doStop();
        logger.debug("stopped");
    }

    protected abstract void doStop() throws IOException;

    @Override
    public void close() throws IOException {
        if (lifecycle.started()) {
            stop();
        }
        if (!lifecycle.canMoveToClosed()) {
            logger.warn("Can't move to closed, it's a " + lifecycle.state());
            return;
        }
        lifecycle.moveToClosed();
        logger.info("closing");
        doClose();
        logger.info("closed");
    }

    protected abstract void doClose() throws IOException;
}

