package com.google.common.util.concurrent;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Service;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public abstract class AbstractExecutionThreadService implements Service {
    private static final Logger logger = Logger.getLogger(AbstractExecutionThreadService.class.getName());
    private final Service delegate = new AbstractService() {
        @Override
        protected final void doStart() {
            MoreExecutors.renamingDecorator(AbstractExecutionThreadService.this.executor(), new Supplier<String>() {
                @Override
                public String get() {
                    return AbstractExecutionThreadService.this.serviceName();
                }
            }).execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AbstractExecutionThreadService.this.startUp();
                        notifyStarted();
                        if (isRunning()) {
                            try {
                                AbstractExecutionThreadService.this.run();
                            } finally {
                            }
                        }
                        AbstractExecutionThreadService.this.shutDown();
                        notifyStopped();
                    } catch (Throwable th) {
                        notifyFailed(th);
                        throw Throwables.propagate(th);
                    }
                }
            });
        }

        @Override
        protected void doStop() {
            AbstractExecutionThreadService.this.triggerShutdown();
        }
    };

    protected abstract void run() throws Exception;

    protected AbstractExecutionThreadService() {
    }

    protected void startUp() throws Exception {
    }

    protected void shutDown() throws Exception {
    }

    protected void triggerShutdown() {
    }

    protected Executor executor() {
        return new Executor() {
            @Override
            public void execute(Runnable runnable) {
                MoreExecutors.newThread(AbstractExecutionThreadService.this.serviceName(), runnable).start();
            }
        };
    }

    public String toString() {
        return serviceName() + " [" + state() + "]";
    }

    @Override
    public final boolean isRunning() {
        return this.delegate.isRunning();
    }

    @Override
    public final Service.State state() {
        return this.delegate.state();
    }

    @Override
    public final void addListener(Service.Listener listener, Executor executor) {
        this.delegate.addListener(listener, executor);
    }

    @Override
    public final Throwable failureCause() {
        return this.delegate.failureCause();
    }

    @Override
    public final Service startAsync() {
        this.delegate.startAsync();
        return this;
    }

    @Override
    public final Service stopAsync() {
        this.delegate.stopAsync();
        return this;
    }

    @Override
    public final void awaitRunning() {
        this.delegate.awaitRunning();
    }

    @Override
    public final void awaitRunning(long j, TimeUnit timeUnit) throws TimeoutException {
        this.delegate.awaitRunning(j, timeUnit);
    }

    @Override
    public final void awaitTerminated() {
        this.delegate.awaitTerminated();
    }

    @Override
    public final void awaitTerminated(long j, TimeUnit timeUnit) throws TimeoutException {
        this.delegate.awaitTerminated(j, timeUnit);
    }

    protected String serviceName() {
        return getClass().getSimpleName();
    }
}
