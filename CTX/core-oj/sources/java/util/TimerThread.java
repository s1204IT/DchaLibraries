package java.util;

class TimerThread extends Thread {
    boolean newTasksMayBeScheduled = true;
    private TaskQueue queue;

    TimerThread(TaskQueue taskQueue) {
        this.queue = taskQueue;
    }

    @Override
    public void run() {
        try {
            mainLoop();
            synchronized (this.queue) {
                this.newTasksMayBeScheduled = false;
                this.queue.clear();
            }
        } catch (Throwable th) {
            synchronized (this.queue) {
                this.newTasksMayBeScheduled = false;
                this.queue.clear();
                throw th;
            }
        }
    }

    private void mainLoop() {
        while (true) {
            synchronized (this.queue) {
                while (this.queue.isEmpty() && this.newTasksMayBeScheduled) {
                    this.queue.wait();
                }
                if (!this.queue.isEmpty()) {
                    TimerTask min = this.queue.getMin();
                    synchronized (min.lock) {
                        if (min.state == 3) {
                            this.queue.removeMin();
                        } else {
                            long jCurrentTimeMillis = System.currentTimeMillis();
                            long j = min.nextExecutionTime;
                            boolean z = j <= jCurrentTimeMillis;
                            if (z) {
                                if (min.period == 0) {
                                    this.queue.removeMin();
                                    min.state = 2;
                                } else {
                                    this.queue.rescheduleMin(min.period < 0 ? jCurrentTimeMillis - min.period : min.period + j);
                                }
                            }
                            if (!z) {
                                this.queue.wait(j - jCurrentTimeMillis);
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }
}
