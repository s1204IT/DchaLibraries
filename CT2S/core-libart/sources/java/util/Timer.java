package java.util;

public class Timer {
    private static long timerId;
    private final FinalizerHelper finalizer;
    private final TimerImpl impl;

    private static final class TimerImpl extends Thread {
        private boolean cancelled;
        private boolean finished;
        private TimerHeap tasks = new TimerHeap();

        private static final class TimerHeap {
            private int DEFAULT_HEAP_SIZE;
            private int deletedCancelledNumber;
            private int size;
            private TimerTask[] timers;

            private TimerHeap() {
                this.DEFAULT_HEAP_SIZE = 256;
                this.timers = new TimerTask[this.DEFAULT_HEAP_SIZE];
                this.size = 0;
                this.deletedCancelledNumber = 0;
            }

            public TimerTask minimum() {
                return this.timers[0];
            }

            public boolean isEmpty() {
                return this.size == 0;
            }

            public void insert(TimerTask task) {
                if (this.timers.length == this.size) {
                    TimerTask[] appendedTimers = new TimerTask[this.size * 2];
                    System.arraycopy(this.timers, 0, appendedTimers, 0, this.size);
                    this.timers = appendedTimers;
                }
                TimerTask[] timerTaskArr = this.timers;
                int i = this.size;
                this.size = i + 1;
                timerTaskArr[i] = task;
                upHeap();
            }

            public void delete(int pos) {
                if (pos >= 0 && pos < this.size) {
                    TimerTask[] timerTaskArr = this.timers;
                    TimerTask[] timerTaskArr2 = this.timers;
                    int i = this.size - 1;
                    this.size = i;
                    timerTaskArr[pos] = timerTaskArr2[i];
                    this.timers[this.size] = null;
                    downHeap(pos);
                }
            }

            private void upHeap() {
                int current = this.size - 1;
                while (true) {
                    int parent = (current - 1) / 2;
                    if (this.timers[current].when < this.timers[parent].when) {
                        TimerTask tmp = this.timers[current];
                        this.timers[current] = this.timers[parent];
                        this.timers[parent] = tmp;
                        current = parent;
                    } else {
                        return;
                    }
                }
            }

            private void downHeap(int pos) {
                int current = pos;
                while (true) {
                    int child = (current * 2) + 1;
                    if (child < this.size && this.size > 0) {
                        if (child + 1 < this.size && this.timers[child + 1].when < this.timers[child].when) {
                            child++;
                        }
                        if (this.timers[current].when >= this.timers[child].when) {
                            TimerTask tmp = this.timers[current];
                            this.timers[current] = this.timers[child];
                            this.timers[child] = tmp;
                            current = child;
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }

            public void reset() {
                this.timers = new TimerTask[this.DEFAULT_HEAP_SIZE];
                this.size = 0;
            }

            public void adjustMinimum() {
                downHeap(0);
            }

            public void deleteIfCancelled() {
                int i = 0;
                while (i < this.size) {
                    if (this.timers[i].cancelled) {
                        this.deletedCancelledNumber++;
                        delete(i);
                        i--;
                    }
                    i++;
                }
            }

            private int getTask(TimerTask task) {
                for (int i = 0; i < this.timers.length; i++) {
                    if (this.timers[i] == task) {
                        return i;
                    }
                }
                return -1;
            }
        }

        TimerImpl(String name, boolean isDaemon) {
            setName(name);
            setDaemon(isDaemon);
            start();
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    if (!this.cancelled) {
                        if (this.tasks.isEmpty()) {
                            if (this.finished) {
                                return;
                            } else {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        } else {
                            long currentTime = System.currentTimeMillis();
                            TimerTask task = this.tasks.minimum();
                            synchronized (task.lock) {
                                if (task.cancelled) {
                                    this.tasks.delete(0);
                                } else {
                                    long timeToSleep = task.when - currentTime;
                                    if (timeToSleep > 0) {
                                        try {
                                            wait(timeToSleep);
                                        } catch (InterruptedException e2) {
                                        }
                                    } else {
                                        synchronized (task.lock) {
                                            int pos = 0;
                                            if (this.tasks.minimum().when != task.when) {
                                                pos = this.tasks.getTask(task);
                                            }
                                            if (task.cancelled) {
                                                this.tasks.delete(this.tasks.getTask(task));
                                            } else {
                                                task.setScheduledTime(task.when);
                                                this.tasks.delete(pos);
                                                if (task.period >= 0) {
                                                    if (task.fixedRate) {
                                                        task.when += task.period;
                                                    } else {
                                                        task.when = System.currentTimeMillis() + task.period;
                                                    }
                                                    insertTask(task);
                                                } else {
                                                    task.when = 0L;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        return;
                    }
                }
            }
        }

        private void insertTask(TimerTask newTask) {
            this.tasks.insert(newTask);
            notify();
        }

        public synchronized void cancel() {
            this.cancelled = true;
            this.tasks.reset();
            notify();
        }

        public int purge() {
            if (this.tasks.isEmpty()) {
                return 0;
            }
            this.tasks.deletedCancelledNumber = 0;
            this.tasks.deleteIfCancelled();
            return this.tasks.deletedCancelledNumber;
        }
    }

    private static final class FinalizerHelper {
        private final TimerImpl impl;

        FinalizerHelper(TimerImpl impl) {
            this.impl = impl;
        }

        protected void finalize() throws Throwable {
            try {
                synchronized (this.impl) {
                    this.impl.finished = true;
                    this.impl.notify();
                }
            } finally {
                super.finalize();
            }
        }
    }

    private static synchronized long nextId() {
        long j;
        j = timerId;
        timerId = 1 + j;
        return j;
    }

    public Timer(String name, boolean isDaemon) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        this.impl = new TimerImpl(name, isDaemon);
        this.finalizer = new FinalizerHelper(this.impl);
    }

    public Timer(String name) {
        this(name, false);
    }

    public Timer(boolean isDaemon) {
        this("Timer-" + nextId(), isDaemon);
    }

    public Timer() {
        this(false);
    }

    public void cancel() {
        this.impl.cancel();
    }

    public int purge() {
        int iPurge;
        synchronized (this.impl) {
            iPurge = this.impl.purge();
        }
        return iPurge;
    }

    public void schedule(TimerTask task, Date when) {
        if (when.getTime() < 0) {
            throw new IllegalArgumentException("when < 0: " + when.getTime());
        }
        long delay = when.getTime() - System.currentTimeMillis();
        scheduleImpl(task, delay >= 0 ? delay : 0L, -1L, false);
    }

    public void schedule(TimerTask task, long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("delay < 0: " + delay);
        }
        scheduleImpl(task, delay, -1L, false);
    }

    public void schedule(TimerTask task, long delay, long period) {
        if (delay < 0 || period <= 0) {
            throw new IllegalArgumentException();
        }
        scheduleImpl(task, delay, period, false);
    }

    public void schedule(TimerTask task, Date when, long period) {
        if (period <= 0 || when.getTime() < 0) {
            throw new IllegalArgumentException();
        }
        long delay = when.getTime() - System.currentTimeMillis();
        scheduleImpl(task, delay >= 0 ? delay : 0L, period, false);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        if (delay < 0 || period <= 0) {
            throw new IllegalArgumentException();
        }
        scheduleImpl(task, delay, period, true);
    }

    public void scheduleAtFixedRate(TimerTask task, Date when, long period) {
        if (period <= 0 || when.getTime() < 0) {
            throw new IllegalArgumentException();
        }
        long delay = when.getTime() - System.currentTimeMillis();
        scheduleImpl(task, delay, period, true);
    }

    private void scheduleImpl(TimerTask task, long delay, long period, boolean fixed) {
        synchronized (this.impl) {
            if (this.impl.cancelled) {
                throw new IllegalStateException("Timer was canceled");
            }
            long when = delay + System.currentTimeMillis();
            if (when < 0) {
                throw new IllegalArgumentException("Illegal delay to start the TimerTask: " + when);
            }
            synchronized (task.lock) {
                if (task.isScheduled()) {
                    throw new IllegalStateException("TimerTask is scheduled already");
                }
                if (task.cancelled) {
                    throw new IllegalStateException("TimerTask is canceled");
                }
                task.when = when;
                task.period = period;
                task.fixedRate = fixed;
            }
            this.impl.insertTask(task);
        }
    }
}
