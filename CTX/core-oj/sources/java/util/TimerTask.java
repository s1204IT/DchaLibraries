package java.util;

public abstract class TimerTask implements Runnable {
    static final int CANCELLED = 3;
    static final int EXECUTED = 2;
    static final int SCHEDULED = 1;
    static final int VIRGIN = 0;
    long nextExecutionTime;
    final Object lock = new Object();
    int state = 0;
    long period = 0;

    @Override
    public abstract void run();

    protected TimerTask() {
    }

    public boolean cancel() {
        boolean z;
        synchronized (this.lock) {
            z = true;
            if (this.state != 1) {
                z = false;
            }
            this.state = 3;
        }
        return z;
    }

    public long scheduledExecutionTime() {
        long j;
        synchronized (this.lock) {
            j = this.period < 0 ? this.nextExecutionTime + this.period : this.nextExecutionTime - this.period;
        }
        return j;
    }
}
