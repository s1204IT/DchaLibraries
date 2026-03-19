package sun.nio.ch;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import sun.nio.ch.Invoker;
import sun.nio.ch.Port;

final class EPollPort extends Port {
    private static final int ENOENT = 2;
    private static final int MAX_EPOLL_EVENTS = 512;
    private final Event EXECUTE_TASK_OR_SHUTDOWN;
    private final Event NEED_TO_POLL;
    private final long address;
    private boolean closed;
    private final int epfd;
    private final ArrayBlockingQueue<Event> queue;
    private final int[] sp;
    private final AtomicInteger wakeupCount;

    private static native void close0(int i);

    private static native void drain1(int i) throws IOException;

    private static native void interrupt(int i) throws IOException;

    private static native void socketpair(int[] iArr) throws IOException;

    static class Event {
        final Port.PollableChannel channel;
        final int events;

        Event(Port.PollableChannel pollableChannel, int i) {
            this.channel = pollableChannel;
            this.events = i;
        }

        Port.PollableChannel channel() {
            return this.channel;
        }

        int events() {
            return this.events;
        }
    }

    EPollPort(AsynchronousChannelProvider asynchronousChannelProvider, ThreadPool threadPool) throws IOException {
        super(asynchronousChannelProvider, threadPool);
        this.wakeupCount = new AtomicInteger();
        this.NEED_TO_POLL = new Event(null, 0);
        this.EXECUTE_TASK_OR_SHUTDOWN = new Event(null, 0);
        this.epfd = EPoll.epollCreate();
        int[] iArr = new int[2];
        try {
            socketpair(iArr);
            EPoll.epollCtl(this.epfd, 1, iArr[0], Net.POLLIN);
            this.sp = iArr;
            this.address = EPoll.allocatePollArray(512);
            this.queue = new ArrayBlockingQueue<>(512);
            this.queue.offer(this.NEED_TO_POLL);
        } catch (IOException e) {
            close0(this.epfd);
            throw e;
        }
    }

    EPollPort start() {
        startThreads(new EventHandlerTask());
        return this;
    }

    private void implClose() {
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            EPoll.freePollArray(this.address);
            close0(this.sp[0]);
            close0(this.sp[1]);
            close0(this.epfd);
        }
    }

    private void wakeup() {
        if (this.wakeupCount.incrementAndGet() == 1) {
            try {
                interrupt(this.sp[1]);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Override
    void executeOnHandlerTask(Runnable runnable) {
        synchronized (this) {
            if (this.closed) {
                throw new RejectedExecutionException();
            }
            offerTask(runnable);
            wakeup();
        }
    }

    @Override
    void shutdownHandlerTasks() {
        int iThreadCount = threadCount();
        if (iThreadCount == 0) {
            implClose();
            return;
        }
        while (true) {
            int i = iThreadCount - 1;
            if (iThreadCount > 0) {
                wakeup();
                iThreadCount = i;
            } else {
                return;
            }
        }
    }

    @Override
    void startPoll(int i, int i2) {
        int i3 = i2 | 1073741824;
        int iEpollCtl = EPoll.epollCtl(this.epfd, 3, i, i3);
        if (iEpollCtl == 2) {
            iEpollCtl = EPoll.epollCtl(this.epfd, 1, i, i3);
        }
        if (iEpollCtl != 0) {
            throw new AssertionError();
        }
    }

    private class EventHandlerTask implements Runnable {
        private EventHandlerTask() {
        }

        private Event poll() throws IOException {
            while (true) {
                try {
                    int iEpollWait = EPoll.epollWait(EPollPort.this.epfd, EPollPort.this.address, 512);
                    EPollPort.this.fdToChannelLock.readLock().lock();
                    while (true) {
                        int i = iEpollWait - 1;
                        if (iEpollWait > 0) {
                            try {
                                long event = EPoll.getEvent(EPollPort.this.address, i);
                                int descriptor = EPoll.getDescriptor(event);
                                if (descriptor == EPollPort.this.sp[0]) {
                                    if (EPollPort.this.wakeupCount.decrementAndGet() == 0) {
                                        EPollPort.drain1(EPollPort.this.sp[0]);
                                    }
                                    if (i > 0) {
                                        EPollPort.this.queue.offer(EPollPort.this.EXECUTE_TASK_OR_SHUTDOWN);
                                    } else {
                                        return EPollPort.this.EXECUTE_TASK_OR_SHUTDOWN;
                                    }
                                } else {
                                    Port.PollableChannel pollableChannel = EPollPort.this.fdToChannel.get(Integer.valueOf(descriptor));
                                    if (pollableChannel != null) {
                                        Event event2 = new Event(pollableChannel, EPoll.getEvents(event));
                                        if (i > 0) {
                                            EPollPort.this.queue.offer(event2);
                                        } else {
                                            return event2;
                                        }
                                    } else {
                                        continue;
                                    }
                                }
                                iEpollWait = i;
                            } finally {
                                EPollPort.this.fdToChannelLock.readLock().unlock();
                            }
                        }
                    }
                } finally {
                    EPollPort.this.queue.offer(EPollPort.this.NEED_TO_POLL);
                }
            }
        }

        @Override
        public void run() throws Throwable {
            Event eventPoll;
            Invoker.GroupAndInvokeCount groupAndInvokeCount = Invoker.getGroupAndInvokeCount();
            boolean z = false;
            boolean z2 = groupAndInvokeCount != null;
            while (true) {
                boolean z3 = false;
                while (true) {
                    if (z2) {
                        try {
                            groupAndInvokeCount.resetInvokeCount();
                            try {
                                try {
                                    eventPoll = (Event) EPollPort.this.queue.take();
                                    if (eventPoll == EPollPort.this.NEED_TO_POLL) {
                                        try {
                                            eventPoll = poll();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            if (EPollPort.this.threadExit(this, false) == 0 && EPollPort.this.isShutdown()) {
                                                EPollPort.this.implClose();
                                                return;
                                            }
                                            return;
                                        }
                                    }
                                    if (eventPoll == EPollPort.this.EXECUTE_TASK_OR_SHUTDOWN) {
                                        try {
                                            break;
                                        } catch (Error e2) {
                                            throw e2;
                                        } catch (RuntimeException e3) {
                                            throw e3;
                                        }
                                    }
                                    Runnable runnablePollTask = EPollPort.this.pollTask();
                                    if (runnablePollTask == null) {
                                        if (EPollPort.this.threadExit(this, false) == 0 && EPollPort.this.isShutdown()) {
                                            EPollPort.this.implClose();
                                            return;
                                        }
                                        return;
                                    }
                                    try {
                                        runnablePollTask.run();
                                        z3 = true;
                                    } catch (Throwable th) {
                                        th = th;
                                        z = true;
                                    }
                                    th = th;
                                    z = true;
                                } catch (InterruptedException e4) {
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            z = z3;
                        }
                    } else {
                        eventPoll = (Event) EPollPort.this.queue.take();
                        if (eventPoll == EPollPort.this.NEED_TO_POLL) {
                        }
                        if (eventPoll == EPollPort.this.EXECUTE_TASK_OR_SHUTDOWN) {
                        }
                        th = th;
                        z = true;
                    }
                    if (EPollPort.this.threadExit(this, z) == 0 && EPollPort.this.isShutdown()) {
                        EPollPort.this.implClose();
                    }
                    throw th;
                }
            }
        }
    }
}
