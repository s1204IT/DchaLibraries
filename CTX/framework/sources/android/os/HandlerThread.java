package android.os;

public class HandlerThread extends Thread {
    private Handler mHandler;
    Looper mLooper;
    int mPriority;
    int mTid;

    public HandlerThread(String str) {
        super(str);
        this.mTid = -1;
        this.mPriority = 0;
    }

    public HandlerThread(String str, int i) {
        super(str);
        this.mTid = -1;
        this.mPriority = i;
    }

    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        this.mTid = Process.myTid();
        Looper.prepare();
        synchronized (this) {
            this.mLooper = Looper.myLooper();
            notifyAll();
        }
        Process.setThreadPriority(this.mPriority);
        onLooperPrepared();
        Looper.loop();
        this.mTid = -1;
    }

    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        synchronized (this) {
            while (isAlive() && this.mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return this.mLooper;
    }

    public Handler getThreadHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler(getLooper());
        }
        return this.mHandler;
    }

    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    public int getThreadId() {
        return this.mTid;
    }
}
