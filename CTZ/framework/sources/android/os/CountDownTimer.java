package android.os;

public abstract class CountDownTimer {
    private static final int MSG = 1;
    private final long mCountdownInterval;
    private final long mMillisInFuture;
    private long mStopTimeInFuture;
    private boolean mCancelled = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            long j;
            synchronized (CountDownTimer.this) {
                if (CountDownTimer.this.mCancelled) {
                    return;
                }
                long jElapsedRealtime = CountDownTimer.this.mStopTimeInFuture - SystemClock.elapsedRealtime();
                if (jElapsedRealtime <= 0) {
                    CountDownTimer.this.onFinish();
                } else {
                    long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                    CountDownTimer.this.onTick(jElapsedRealtime);
                    long jElapsedRealtime3 = SystemClock.elapsedRealtime() - jElapsedRealtime2;
                    if (jElapsedRealtime >= CountDownTimer.this.mCountdownInterval) {
                        j = CountDownTimer.this.mCountdownInterval - jElapsedRealtime3;
                        while (j < 0) {
                            j += CountDownTimer.this.mCountdownInterval;
                        }
                    } else {
                        j = jElapsedRealtime - jElapsedRealtime3;
                        if (j < 0) {
                            j = 0;
                        }
                    }
                    sendMessageDelayed(obtainMessage(1), j);
                }
            }
        }
    };

    public abstract void onFinish();

    public abstract void onTick(long j);

    public CountDownTimer(long j, long j2) {
        this.mMillisInFuture = j;
        this.mCountdownInterval = j2;
    }

    public final synchronized void cancel() {
        this.mCancelled = true;
        this.mHandler.removeMessages(1);
    }

    public final synchronized CountDownTimer start() {
        this.mCancelled = false;
        if (this.mMillisInFuture <= 0) {
            onFinish();
            return this;
        }
        this.mStopTimeInFuture = SystemClock.elapsedRealtime() + this.mMillisInFuture;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
        return this;
    }
}
