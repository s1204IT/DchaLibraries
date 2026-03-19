package android.os;

public class ConditionVariable {
    private volatile boolean mCondition;

    public ConditionVariable() {
        this.mCondition = false;
    }

    public ConditionVariable(boolean z) {
        this.mCondition = z;
    }

    public void open() {
        synchronized (this) {
            boolean z = this.mCondition;
            this.mCondition = true;
            if (!z) {
                notifyAll();
            }
        }
    }

    public void close() {
        synchronized (this) {
            this.mCondition = false;
        }
    }

    public void block() {
        synchronized (this) {
            while (!this.mCondition) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public boolean block(long j) {
        boolean z;
        if (j != 0) {
            synchronized (this) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                long j2 = j + jCurrentTimeMillis;
                while (!this.mCondition && jCurrentTimeMillis < j2) {
                    try {
                        wait(j2 - jCurrentTimeMillis);
                    } catch (InterruptedException e) {
                    }
                    jCurrentTimeMillis = System.currentTimeMillis();
                }
                z = this.mCondition;
            }
            return z;
        }
        block();
        return true;
    }
}
