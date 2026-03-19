package java.util;

import dalvik.annotation.optimization.ReachabilitySensitive;
import java.util.concurrent.atomic.AtomicInteger;

public class Timer {
    private static final AtomicInteger nextSerialNumber = new AtomicInteger(0);

    @ReachabilitySensitive
    private final TaskQueue queue;

    @ReachabilitySensitive
    private final TimerThread thread;
    private final Object threadReaper;

    private static int serialNumber() {
        return nextSerialNumber.getAndIncrement();
    }

    public Timer() {
        this("Timer-" + serialNumber());
    }

    public Timer(boolean z) {
        this("Timer-" + serialNumber(), z);
    }

    public Timer(String str) {
        this.queue = new TaskQueue();
        this.thread = new TimerThread(this.queue);
        this.threadReaper = new Object() {
            protected void finalize() throws Throwable {
                synchronized (Timer.this.queue) {
                    Timer.this.thread.newTasksMayBeScheduled = false;
                    Timer.this.queue.notify();
                }
            }
        };
        this.thread.setName(str);
        this.thread.start();
    }

    public Timer(String str, boolean z) {
        this.queue = new TaskQueue();
        this.thread = new TimerThread(this.queue);
        this.threadReaper = new Object() {
            protected void finalize() throws Throwable {
                synchronized (Timer.this.queue) {
                    Timer.this.thread.newTasksMayBeScheduled = false;
                    Timer.this.queue.notify();
                }
            }
        };
        this.thread.setName(str);
        this.thread.setDaemon(z);
        this.thread.start();
    }

    public void schedule(TimerTask timerTask, long j) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        sched(timerTask, System.currentTimeMillis() + j, 0L);
    }

    public void schedule(TimerTask timerTask, Date date) {
        sched(timerTask, date.getTime(), 0L);
    }

    public void schedule(TimerTask timerTask, long j, long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (j2 <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(timerTask, System.currentTimeMillis() + j, -j2);
    }

    public void schedule(TimerTask timerTask, Date date, long j) {
        if (j <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(timerTask, date.getTime(), -j);
    }

    public void scheduleAtFixedRate(TimerTask timerTask, long j, long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (j2 <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(timerTask, System.currentTimeMillis() + j, j2);
    }

    public void scheduleAtFixedRate(TimerTask timerTask, Date date, long j) {
        if (j <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        sched(timerTask, date.getTime(), j);
    }

    private void sched(TimerTask timerTask, long j, long j2) {
        if (j < 0) {
            throw new IllegalArgumentException("Illegal execution time.");
        }
        if (Math.abs(j2) > 4611686018427387903L) {
            j2 >>= 1;
        }
        synchronized (this.queue) {
            if (!this.thread.newTasksMayBeScheduled) {
                throw new IllegalStateException("Timer already cancelled.");
            }
            synchronized (timerTask.lock) {
                if (timerTask.state != 0) {
                    throw new IllegalStateException("Task already scheduled or cancelled");
                }
                timerTask.nextExecutionTime = j;
                timerTask.period = j2;
                timerTask.state = 1;
            }
            this.queue.add(timerTask);
            if (this.queue.getMin() == timerTask) {
                this.queue.notify();
            }
        }
    }

    public void cancel() {
        synchronized (this.queue) {
            this.thread.newTasksMayBeScheduled = false;
            this.queue.clear();
            this.queue.notify();
        }
    }

    public int purge() {
        int i;
        synchronized (this.queue) {
            i = 0;
            for (int size = this.queue.size(); size > 0; size--) {
                if (this.queue.get(size).state == 3) {
                    this.queue.quickRemove(size);
                    i++;
                }
            }
            if (i != 0) {
                this.queue.heapify();
            }
        }
        return i;
    }
}
