package com.google.common.util.concurrent;

import com.android.contacts.compat.CompatUtils;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenerCallQueue;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractService implements Service {
    private static final ListenerCallQueue.Callback<Service.Listener> STARTING_CALLBACK = new ListenerCallQueue.Callback<Service.Listener>("starting()") {
        @Override
        void call(Service.Listener listener) {
            listener.starting();
        }
    };
    private static final ListenerCallQueue.Callback<Service.Listener> RUNNING_CALLBACK = new ListenerCallQueue.Callback<Service.Listener>("running()") {
        @Override
        void call(Service.Listener listener) {
            listener.running();
        }
    };
    private static final ListenerCallQueue.Callback<Service.Listener> STOPPING_FROM_STARTING_CALLBACK = stoppingCallback(Service.State.STARTING);
    private static final ListenerCallQueue.Callback<Service.Listener> STOPPING_FROM_RUNNING_CALLBACK = stoppingCallback(Service.State.RUNNING);
    private static final ListenerCallQueue.Callback<Service.Listener> TERMINATED_FROM_NEW_CALLBACK = terminatedCallback(Service.State.NEW);
    private static final ListenerCallQueue.Callback<Service.Listener> TERMINATED_FROM_RUNNING_CALLBACK = terminatedCallback(Service.State.RUNNING);
    private static final ListenerCallQueue.Callback<Service.Listener> TERMINATED_FROM_STOPPING_CALLBACK = terminatedCallback(Service.State.STOPPING);
    private final Monitor monitor = new Monitor();
    private final Monitor.Guard isStartable = new Monitor.Guard(this.monitor) {
        @Override
        public boolean isSatisfied() {
            return AbstractService.this.state() == Service.State.NEW;
        }
    };
    private final Monitor.Guard isStoppable = new Monitor.Guard(this.monitor) {
        @Override
        public boolean isSatisfied() {
            return AbstractService.this.state().compareTo(Service.State.RUNNING) <= 0;
        }
    };
    private final Monitor.Guard hasReachedRunning = new Monitor.Guard(this.monitor) {
        @Override
        public boolean isSatisfied() {
            return AbstractService.this.state().compareTo(Service.State.RUNNING) >= 0;
        }
    };
    private final Monitor.Guard isStopped = new Monitor.Guard(this.monitor) {
        @Override
        public boolean isSatisfied() {
            return AbstractService.this.state().isTerminal();
        }
    };
    private final List<ListenerCallQueue<Service.Listener>> listeners = Collections.synchronizedList(new ArrayList());
    private volatile StateSnapshot snapshot = new StateSnapshot(Service.State.NEW);

    protected abstract void doStart();

    protected abstract void doStop();

    private static ListenerCallQueue.Callback<Service.Listener> terminatedCallback(final Service.State state) {
        return new ListenerCallQueue.Callback<Service.Listener>("terminated({from = " + state + "})") {
            @Override
            void call(Service.Listener listener) {
                listener.terminated(state);
            }
        };
    }

    private static ListenerCallQueue.Callback<Service.Listener> stoppingCallback(final Service.State state) {
        return new ListenerCallQueue.Callback<Service.Listener>("stopping({from = " + state + "})") {
            @Override
            void call(Service.Listener listener) {
                listener.stopping(state);
            }
        };
    }

    protected AbstractService() {
    }

    @Override
    public final Service startAsync() {
        if (this.monitor.enterIf(this.isStartable)) {
            try {
                try {
                    this.snapshot = new StateSnapshot(Service.State.STARTING);
                    starting();
                    doStart();
                } catch (Throwable th) {
                    notifyFailed(th);
                }
                return this;
            } finally {
                this.monitor.leave();
                executeListeners();
            }
        }
        throw new IllegalStateException("Service " + this + " has already been started");
    }

    @Override
    public final Service stopAsync() {
        try {
            if (this.monitor.enterIf(this.isStoppable)) {
                try {
                    Service.State state = state();
                    switch (AnonymousClass10.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()]) {
                        case 1:
                            this.snapshot = new StateSnapshot(Service.State.TERMINATED);
                            terminated(Service.State.NEW);
                            break;
                        case 2:
                            this.snapshot = new StateSnapshot(Service.State.STARTING, true, null);
                            stopping(Service.State.STARTING);
                            break;
                        case 3:
                            this.snapshot = new StateSnapshot(Service.State.STOPPING);
                            stopping(Service.State.RUNNING);
                            doStop();
                            break;
                        case CompatUtils.TYPE_ASSERT:
                        case 5:
                        case 6:
                            throw new AssertionError("isStoppable is incorrectly implemented, saw: " + state);
                        default:
                            throw new AssertionError("Unexpected state: " + state);
                    }
                } catch (Throwable th) {
                    notifyFailed(th);
                }
            }
            return this;
        } finally {
            this.monitor.leave();
            executeListeners();
        }
    }

    static class AnonymousClass10 {
        static final int[] $SwitchMap$com$google$common$util$concurrent$Service$State = new int[Service.State.values().length];

        static {
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.NEW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.STARTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.RUNNING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.STOPPING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.TERMINATED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$google$common$util$concurrent$Service$State[Service.State.FAILED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    @Override
    public final void awaitRunning() {
        this.monitor.enterWhenUninterruptibly(this.hasReachedRunning);
        try {
            checkCurrentState(Service.State.RUNNING);
        } finally {
            this.monitor.leave();
        }
    }

    @Override
    public final void awaitRunning(long j, TimeUnit timeUnit) throws TimeoutException {
        if (this.monitor.enterWhenUninterruptibly(this.hasReachedRunning, j, timeUnit)) {
            try {
                checkCurrentState(Service.State.RUNNING);
            } finally {
                this.monitor.leave();
            }
        } else {
            throw new TimeoutException("Timed out waiting for " + this + " to reach the RUNNING state. Current state: " + state());
        }
    }

    @Override
    public final void awaitTerminated() {
        this.monitor.enterWhenUninterruptibly(this.isStopped);
        try {
            checkCurrentState(Service.State.TERMINATED);
        } finally {
            this.monitor.leave();
        }
    }

    @Override
    public final void awaitTerminated(long j, TimeUnit timeUnit) throws TimeoutException {
        if (this.monitor.enterWhenUninterruptibly(this.isStopped, j, timeUnit)) {
            try {
                checkCurrentState(Service.State.TERMINATED);
            } finally {
                this.monitor.leave();
            }
        } else {
            throw new TimeoutException("Timed out waiting for " + this + " to reach a terminal state. Current state: " + state());
        }
    }

    private void checkCurrentState(Service.State state) {
        Service.State state2 = state();
        if (state2 != state) {
            if (state2 == Service.State.FAILED) {
                throw new IllegalStateException("Expected the service to be " + state + ", but the service has FAILED", failureCause());
            }
            throw new IllegalStateException("Expected the service to be " + state + ", but was " + state2);
        }
    }

    protected final void notifyStarted() {
        this.monitor.enter();
        try {
            if (this.snapshot.state != Service.State.STARTING) {
                IllegalStateException illegalStateException = new IllegalStateException("Cannot notifyStarted() when the service is " + this.snapshot.state);
                notifyFailed(illegalStateException);
                throw illegalStateException;
            }
            if (this.snapshot.shutdownWhenStartupFinishes) {
                this.snapshot = new StateSnapshot(Service.State.STOPPING);
                doStop();
            } else {
                this.snapshot = new StateSnapshot(Service.State.RUNNING);
                running();
            }
        } finally {
            this.monitor.leave();
            executeListeners();
        }
    }

    protected final void notifyStopped() {
        this.monitor.enter();
        try {
            Service.State state = this.snapshot.state;
            if (state != Service.State.STOPPING && state != Service.State.RUNNING) {
                IllegalStateException illegalStateException = new IllegalStateException("Cannot notifyStopped() when the service is " + state);
                notifyFailed(illegalStateException);
                throw illegalStateException;
            }
            this.snapshot = new StateSnapshot(Service.State.TERMINATED);
            terminated(state);
        } finally {
            this.monitor.leave();
            executeListeners();
        }
    }

    protected final void notifyFailed(Throwable th) {
        Preconditions.checkNotNull(th);
        this.monitor.enter();
        try {
            Service.State state = state();
            switch (AnonymousClass10.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()]) {
                case 1:
                case 5:
                    throw new IllegalStateException("Failed while in state:" + state, th);
                case 2:
                case 3:
                case CompatUtils.TYPE_ASSERT:
                    this.snapshot = new StateSnapshot(Service.State.FAILED, false, th);
                    failed(state, th);
                    break;
                case 6:
                    break;
                default:
                    throw new AssertionError("Unexpected state: " + state);
            }
        } finally {
            this.monitor.leave();
            executeListeners();
        }
    }

    @Override
    public final boolean isRunning() {
        return state() == Service.State.RUNNING;
    }

    @Override
    public final Service.State state() {
        return this.snapshot.externalState();
    }

    @Override
    public final Throwable failureCause() {
        return this.snapshot.failureCause();
    }

    @Override
    public final void addListener(Service.Listener listener, Executor executor) {
        Preconditions.checkNotNull(listener, "listener");
        Preconditions.checkNotNull(executor, "executor");
        this.monitor.enter();
        try {
            if (!state().isTerminal()) {
                this.listeners.add(new ListenerCallQueue<>(listener, executor));
            }
        } finally {
            this.monitor.leave();
        }
    }

    public String toString() {
        return getClass().getSimpleName() + " [" + state() + "]";
    }

    private void executeListeners() {
        if (!this.monitor.isOccupiedByCurrentThread()) {
            for (int i = 0; i < this.listeners.size(); i++) {
                this.listeners.get(i).execute();
            }
        }
    }

    private void starting() {
        STARTING_CALLBACK.enqueueOn(this.listeners);
    }

    private void running() {
        RUNNING_CALLBACK.enqueueOn(this.listeners);
    }

    private void stopping(Service.State state) {
        if (state == Service.State.STARTING) {
            STOPPING_FROM_STARTING_CALLBACK.enqueueOn(this.listeners);
        } else {
            if (state == Service.State.RUNNING) {
                STOPPING_FROM_RUNNING_CALLBACK.enqueueOn(this.listeners);
                return;
            }
            throw new AssertionError();
        }
    }

    private void terminated(Service.State state) {
        int i = AnonymousClass10.$SwitchMap$com$google$common$util$concurrent$Service$State[state.ordinal()];
        if (i == 1) {
            TERMINATED_FROM_NEW_CALLBACK.enqueueOn(this.listeners);
            return;
        }
        switch (i) {
            case 3:
                TERMINATED_FROM_RUNNING_CALLBACK.enqueueOn(this.listeners);
                return;
            case CompatUtils.TYPE_ASSERT:
                TERMINATED_FROM_STOPPING_CALLBACK.enqueueOn(this.listeners);
                return;
            default:
                throw new AssertionError();
        }
    }

    private void failed(final Service.State state, final Throwable th) {
        new ListenerCallQueue.Callback<Service.Listener>("failed({from = " + state + ", cause = " + th + "})") {
            @Override
            void call(Service.Listener listener) {
                listener.failed(state, th);
            }
        }.enqueueOn(this.listeners);
    }

    private static final class StateSnapshot {
        final Throwable failure;
        final boolean shutdownWhenStartupFinishes;
        final Service.State state;

        StateSnapshot(Service.State state) {
            this(state, false, null);
        }

        StateSnapshot(Service.State state, boolean z, Throwable th) {
            Preconditions.checkArgument(!z || state == Service.State.STARTING, "shudownWhenStartupFinishes can only be set if state is STARTING. Got %s instead.", state);
            Preconditions.checkArgument(!((th != null) ^ (state == Service.State.FAILED)), "A failure cause should be set if and only if the state is failed.  Got %s and %s instead.", state, th);
            this.state = state;
            this.shutdownWhenStartupFinishes = z;
            this.failure = th;
        }

        Service.State externalState() {
            if (this.shutdownWhenStartupFinishes && this.state == Service.State.STARTING) {
                return Service.State.STOPPING;
            }
            return this.state;
        }

        Throwable failureCause() {
            Preconditions.checkState(this.state == Service.State.FAILED, "failureCause() is only valid if the service has failed, service is %s", this.state);
            return this.failure;
        }
    }
}
