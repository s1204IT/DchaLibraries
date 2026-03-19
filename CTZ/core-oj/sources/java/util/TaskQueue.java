package java.util;

class TaskQueue {
    static final boolean $assertionsDisabled = false;
    private TimerTask[] queue = new TimerTask[128];
    private int size = 0;

    TaskQueue() {
    }

    int size() {
        return this.size;
    }

    void add(TimerTask timerTask) {
        if (this.size + 1 == this.queue.length) {
            this.queue = (TimerTask[]) Arrays.copyOf(this.queue, 2 * this.queue.length);
        }
        TimerTask[] timerTaskArr = this.queue;
        int i = this.size + 1;
        this.size = i;
        timerTaskArr[i] = timerTask;
        fixUp(this.size);
    }

    TimerTask getMin() {
        return this.queue[1];
    }

    TimerTask get(int i) {
        return this.queue[i];
    }

    void removeMin() {
        this.queue[1] = this.queue[this.size];
        TimerTask[] timerTaskArr = this.queue;
        int i = this.size;
        this.size = i - 1;
        timerTaskArr[i] = null;
        fixDown(1);
    }

    void quickRemove(int i) {
        this.queue[i] = this.queue[this.size];
        TimerTask[] timerTaskArr = this.queue;
        int i2 = this.size;
        this.size = i2 - 1;
        timerTaskArr[i2] = null;
    }

    void rescheduleMin(long j) {
        this.queue[1].nextExecutionTime = j;
        fixDown(1);
    }

    boolean isEmpty() {
        return this.size == 0;
    }

    void clear() {
        for (int i = 1; i <= this.size; i++) {
            this.queue[i] = null;
        }
        this.size = 0;
    }

    private void fixUp(int i) {
        while (i > 1) {
            int i2 = i >> 1;
            if (this.queue[i2].nextExecutionTime > this.queue[i].nextExecutionTime) {
                TimerTask timerTask = this.queue[i2];
                this.queue[i2] = this.queue[i];
                this.queue[i] = timerTask;
                i = i2;
            } else {
                return;
            }
        }
    }

    private void fixDown(int i) {
        while (true) {
            int i2 = i << 1;
            if (i2 <= this.size && i2 > 0) {
                if (i2 < this.size) {
                    int i3 = i2 + 1;
                    if (this.queue[i2].nextExecutionTime > this.queue[i3].nextExecutionTime) {
                        i2 = i3;
                    }
                }
                if (this.queue[i].nextExecutionTime > this.queue[i2].nextExecutionTime) {
                    TimerTask timerTask = this.queue[i2];
                    this.queue[i2] = this.queue[i];
                    this.queue[i] = timerTask;
                    i = i2;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void heapify() {
        for (int i = this.size / 2; i >= 1; i--) {
            fixDown(i);
        }
    }
}
