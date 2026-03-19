package android.view;

import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.TimeUtils;
import android.view.animation.AnimationUtils;
import com.mediatek.view.ViewDebugManager;
import java.io.PrintWriter;

public final class Choreographer {
    public static final int CALLBACK_ANIMATION = 1;
    public static final int CALLBACK_COMMIT = 3;
    public static final int CALLBACK_INPUT = 0;
    private static final int CALLBACK_LAST = 3;
    public static final int CALLBACK_TRAVERSAL = 2;
    private static final int MSG_DO_FRAME = 0;
    private static final int MSG_DO_SCHEDULE_CALLBACK = 2;
    private static final int MSG_DO_SCHEDULE_VSYNC = 1;
    private static final String TAG = "Choreographer";
    private static volatile Choreographer mMainInstance;
    private CallbackRecord mCallbackPool;
    private final CallbackQueue[] mCallbackQueues;
    private boolean mCallbacksRunning;
    private boolean mDebugPrintNextFrameTimeDelta;
    private final FrameDisplayEventReceiver mDisplayEventReceiver;
    private int mFPSDivisor;
    FrameInfo mFrameInfo;
    private long mFrameIntervalNanos;
    private boolean mFrameScheduled;
    private final FrameHandler mHandler;
    private long mLastFrameTimeNanos;
    private final Object mLock;
    private final Looper mLooper;
    private static final boolean DEBUG_JANK = ViewDebugManager.DEBUG_CHOREOGRAPHER_JANK;
    private static final boolean DEBUG_FRAMES = ViewDebugManager.DEBUG_CHOREOGRAPHER_FRAMES;
    private static final long DEFAULT_FRAME_DELAY = 10;
    private static volatile long sFrameDelay = DEFAULT_FRAME_DELAY;
    private static final ThreadLocal<Choreographer> sThreadInstance = new ThreadLocal<Choreographer>() {
        @Override
        protected Choreographer initialValue() {
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper == null) {
                throw new IllegalStateException("The current thread must have a looper!");
            }
            Choreographer choreographer = new Choreographer(looperMyLooper, 0);
            if (looperMyLooper == Looper.getMainLooper()) {
                Choreographer unused = Choreographer.mMainInstance = choreographer;
            }
            return choreographer;
        }
    };
    private static final ThreadLocal<Choreographer> sSfThreadInstance = new ThreadLocal<Choreographer>() {
        @Override
        protected Choreographer initialValue() {
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper == null) {
                throw new IllegalStateException("The current thread must have a looper!");
            }
            return new Choreographer(looperMyLooper, 1);
        }
    };
    private static final boolean USE_VSYNC = SystemProperties.getBoolean("debug.choreographer.vsync", true);
    private static final boolean USE_FRAME_TIME = SystemProperties.getBoolean("debug.choreographer.frametime", true);
    private static final int SKIPPED_FRAME_WARNING_LIMIT = SystemProperties.getInt("debug.choreographer.skipwarning", 30);
    private static final Object FRAME_CALLBACK_TOKEN = new Object() {
        public String toString() {
            return "FRAME_CALLBACK_TOKEN";
        }
    };
    private static final String[] CALLBACK_TRACE_TITLES = {"input", "animation", "traversal", "commit"};

    public interface FrameCallback {
        void doFrame(long j);
    }

    private Choreographer(Looper looper, int i) {
        FrameDisplayEventReceiver frameDisplayEventReceiver;
        this.mLock = new Object();
        this.mFPSDivisor = 1;
        this.mFrameInfo = new FrameInfo();
        this.mLooper = looper;
        this.mHandler = new FrameHandler(looper);
        if (USE_VSYNC) {
            frameDisplayEventReceiver = new FrameDisplayEventReceiver(looper, i);
        } else {
            frameDisplayEventReceiver = null;
        }
        this.mDisplayEventReceiver = frameDisplayEventReceiver;
        this.mLastFrameTimeNanos = Long.MIN_VALUE;
        this.mFrameIntervalNanos = (long) (1.0E9f / getRefreshRate());
        this.mCallbackQueues = new CallbackQueue[4];
        for (int i2 = 0; i2 <= 3; i2++) {
            this.mCallbackQueues[i2] = new CallbackQueue();
        }
        setFPSDivisor(SystemProperties.getInt(ThreadedRenderer.DEBUG_FPS_DIVISOR, 1));
    }

    private static float getRefreshRate() {
        return DisplayManagerGlobal.getInstance().getDisplayInfo(0).getMode().getRefreshRate();
    }

    public static Choreographer getInstance() {
        return sThreadInstance.get();
    }

    public static Choreographer getSfInstance() {
        return sSfThreadInstance.get();
    }

    public static Choreographer getMainThreadInstance() {
        return mMainInstance;
    }

    public static void releaseInstance() {
        Choreographer choreographer = sThreadInstance.get();
        sThreadInstance.remove();
        choreographer.dispose();
    }

    private void dispose() {
        this.mDisplayEventReceiver.dispose();
    }

    public static long getFrameDelay() {
        return sFrameDelay;
    }

    public static void setFrameDelay(long j) {
        sFrameDelay = j;
    }

    public static long subtractFrameDelay(long j) {
        long j2 = sFrameDelay;
        if (j <= j2) {
            return 0L;
        }
        return j - j2;
    }

    public long getFrameIntervalNanos() {
        return this.mFrameIntervalNanos;
    }

    void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.println("Choreographer:");
        printWriter.print(str2);
        printWriter.print("mFrameScheduled=");
        printWriter.println(this.mFrameScheduled);
        printWriter.print(str2);
        printWriter.print("mLastFrameTime=");
        printWriter.println(TimeUtils.formatUptime(this.mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS));
    }

    public void postCallback(int i, Runnable runnable, Object obj) {
        postCallbackDelayed(i, runnable, obj, 0L);
    }

    public void postCallbackDelayed(int i, Runnable runnable, Object obj, long j) {
        if (runnable == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("callbackType is invalid");
        }
        postCallbackDelayedInternal(i, runnable, obj, j);
    }

    private void postCallbackDelayedInternal(int i, Object obj, Object obj2, long j) {
        if (DEBUG_FRAMES) {
            Log.d(TAG, "PostCallback: type=" + i + ", action=" + obj + ", token=" + obj2 + ", delayMillis=" + j, new Throwable());
        }
        synchronized (this.mLock) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            long j2 = j + jUptimeMillis;
            this.mCallbackQueues[i].addCallbackLocked(j2, obj, obj2);
            if (j2 <= jUptimeMillis) {
                scheduleFrameLocked(jUptimeMillis);
            } else {
                Message messageObtainMessage = this.mHandler.obtainMessage(2, obj);
                messageObtainMessage.arg1 = i;
                messageObtainMessage.setAsynchronous(true);
                this.mHandler.sendMessageAtTime(messageObtainMessage, j2);
            }
        }
    }

    public void removeCallbacks(int i, Runnable runnable, Object obj) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("callbackType is invalid");
        }
        removeCallbacksInternal(i, runnable, obj);
    }

    private void removeCallbacksInternal(int i, Object obj, Object obj2) {
        if (DEBUG_FRAMES) {
            Log.d(TAG, "RemoveCallbacks: type=" + i + ", action=" + obj + ", token=" + obj2);
        }
        synchronized (this.mLock) {
            this.mCallbackQueues[i].removeCallbacksLocked(obj, obj2);
            if (obj != null && obj2 == null) {
                this.mHandler.removeMessages(2, obj);
            }
        }
    }

    public void postFrameCallback(FrameCallback frameCallback) {
        postFrameCallbackDelayed(frameCallback, 0L);
    }

    public void postFrameCallbackDelayed(FrameCallback frameCallback, long j) {
        if (frameCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        postCallbackDelayedInternal(1, frameCallback, FRAME_CALLBACK_TOKEN, j);
    }

    public void removeFrameCallback(FrameCallback frameCallback) {
        if (frameCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        removeCallbacksInternal(1, frameCallback, FRAME_CALLBACK_TOKEN);
    }

    public long getFrameTime() {
        return getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;
    }

    public long getFrameTimeNanos() {
        long jNanoTime;
        synchronized (this.mLock) {
            if (!this.mCallbacksRunning) {
                throw new IllegalStateException("This method must only be called as part of a callback while a frame is in progress.");
            }
            jNanoTime = USE_FRAME_TIME ? this.mLastFrameTimeNanos : System.nanoTime();
        }
        return jNanoTime;
    }

    public long getLastFrameTimeNanos() {
        long jNanoTime;
        synchronized (this.mLock) {
            jNanoTime = USE_FRAME_TIME ? this.mLastFrameTimeNanos : System.nanoTime();
        }
        return jNanoTime;
    }

    private void scheduleFrameLocked(long j) {
        if (!this.mFrameScheduled) {
            this.mFrameScheduled = true;
            if (USE_VSYNC) {
                if (DEBUG_FRAMES) {
                    Log.d(TAG, "Scheduling next frame on vsync.");
                }
                if (!isRunningOnLooperThreadLocked()) {
                    Message messageObtainMessage = this.mHandler.obtainMessage(1);
                    messageObtainMessage.setAsynchronous(true);
                    this.mHandler.sendMessageAtFrontOfQueue(messageObtainMessage);
                    return;
                }
                scheduleVsyncLocked();
                return;
            }
            long jMax = Math.max((this.mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS) + sFrameDelay, j);
            if (DEBUG_FRAMES) {
                Log.d(TAG, "Scheduling next frame in " + (jMax - j) + " ms.");
            }
            Message messageObtainMessage2 = this.mHandler.obtainMessage(0);
            messageObtainMessage2.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(messageObtainMessage2, jMax);
        }
    }

    void setFPSDivisor(int i) {
        if (i <= 0) {
            i = 1;
        }
        this.mFPSDivisor = i;
        ThreadedRenderer.setFPSDivisor(i);
    }

    void doFrame(long j, int i) {
        long j2;
        long j3;
        synchronized (this.mLock) {
            if (this.mFrameScheduled) {
                if (DEBUG_JANK && this.mDebugPrintNextFrameTimeDelta) {
                    this.mDebugPrintNextFrameTimeDelta = false;
                    Log.d(TAG, "Frame time delta: " + ((j - this.mLastFrameTimeNanos) * 1.0E-6f) + " ms");
                }
                long jNanoTime = System.nanoTime();
                long j4 = jNanoTime - j;
                if (j4 >= this.mFrameIntervalNanos) {
                    long j5 = j4 / this.mFrameIntervalNanos;
                    if (j5 >= SKIPPED_FRAME_WARNING_LIMIT) {
                        Log.i(TAG, "Skipped " + j5 + " frames!  The application may be doing too much work on its main thread.");
                    }
                    long j6 = j4 % this.mFrameIntervalNanos;
                    if (DEBUG_JANK) {
                        Log.d(TAG, "Missed vsync by " + (j4 * 1.0E-6f) + " ms which is more than the frame interval of " + (this.mFrameIntervalNanos * 1.0E-6f) + " ms!  Skipping " + j5 + " frames and setting frame time to " + (j6 * 1.0E-6f) + " ms in the past.");
                    }
                    j2 = jNanoTime - j6;
                } else {
                    j2 = j;
                }
                if (j2 < this.mLastFrameTimeNanos) {
                    if (DEBUG_JANK) {
                        Log.d(TAG, "Frame time appears to be going backwards.  May be due to a previously skipped frame.  Waiting for next vsync.");
                    }
                    scheduleVsyncLocked();
                    return;
                }
                if (this.mFPSDivisor > 1) {
                    long j7 = j2 - this.mLastFrameTimeNanos;
                    j3 = jNanoTime;
                    if (j7 < this.mFrameIntervalNanos * ((long) this.mFPSDivisor) && j7 > 0) {
                        scheduleVsyncLocked();
                        return;
                    }
                } else {
                    j3 = jNanoTime;
                }
                this.mFrameInfo.setVsync(j, j2);
                this.mFrameScheduled = false;
                this.mLastFrameTimeNanos = j2;
                try {
                    Trace.traceBegin(8L, "Choreographer#doFrame");
                    AnimationUtils.lockAnimationClock(j2 / TimeUtils.NANOS_PER_MS);
                    this.mFrameInfo.markInputHandlingStart();
                    doCallbacks(0, j2);
                    this.mFrameInfo.markAnimationsStart();
                    doCallbacks(1, j2);
                    this.mFrameInfo.markPerformTraversalsStart();
                    doCallbacks(2, j2);
                    doCallbacks(3, j2);
                    AnimationUtils.unlockAnimationClock();
                    Trace.traceEnd(8L);
                    this.mFrameInfo.markDoFrameEnd();
                    if (DEBUG_FRAMES) {
                        long jNanoTime2 = System.nanoTime();
                        Log.d(TAG, "Frame " + i + ": Finished, took " + ((jNanoTime2 - j3) * 1.0E-6f) + " ms, latency " + ((j3 - j2) * 1.0E-6f) + " ms.");
                    }
                } catch (Throwable th) {
                    AnimationUtils.unlockAnimationClock();
                    Trace.traceEnd(8L);
                    throw th;
                }
            }
        }
    }

    void doCallbacks(int i, long j) {
        long j2;
        synchronized (this.mLock) {
            long jNanoTime = System.nanoTime();
            CallbackRecord callbackRecordExtractDueCallbacksLocked = this.mCallbackQueues[i].extractDueCallbacksLocked(jNanoTime / TimeUtils.NANOS_PER_MS);
            if (callbackRecordExtractDueCallbacksLocked == null) {
                return;
            }
            this.mCallbacksRunning = true;
            if (i == 3) {
                long j3 = jNanoTime - j;
                Trace.traceCounter(8L, "jitterNanos", (int) j3);
                if (j3 >= 2 * this.mFrameIntervalNanos) {
                    long j4 = (j3 % this.mFrameIntervalNanos) + this.mFrameIntervalNanos;
                    if (DEBUG_JANK) {
                        Log.d(TAG, "Commit callback delayed by " + (j3 * 1.0E-6f) + " ms which is more than twice the frame interval of " + (this.mFrameIntervalNanos * 1.0E-6f) + " ms!  Setting frame time to " + (j4 * 1.0E-6f) + " ms in the past.");
                        this.mDebugPrintNextFrameTimeDelta = true;
                    }
                    long j5 = jNanoTime - j4;
                    this.mLastFrameTimeNanos = j5;
                    j2 = j5;
                } else {
                    j2 = j;
                }
            }
            try {
                Trace.traceBegin(8L, CALLBACK_TRACE_TITLES[i]);
                for (CallbackRecord callbackRecord = callbackRecordExtractDueCallbacksLocked; callbackRecord != null; callbackRecord = callbackRecord.next) {
                    if (DEBUG_FRAMES) {
                        Log.d(TAG, "RunCallback: type=" + i + ", action=" + callbackRecord.action + ", token=" + callbackRecord.token + ", latencyMillis=" + (SystemClock.uptimeMillis() - callbackRecord.dueTime));
                    }
                    callbackRecord.run(j2);
                }
                synchronized (this.mLock) {
                    this.mCallbacksRunning = false;
                    while (true) {
                        CallbackRecord callbackRecord2 = callbackRecordExtractDueCallbacksLocked.next;
                        recycleCallbackLocked(callbackRecordExtractDueCallbacksLocked);
                        if (callbackRecord2 != null) {
                            callbackRecordExtractDueCallbacksLocked = callbackRecord2;
                        }
                    }
                }
                Trace.traceEnd(8L);
            } catch (Throwable th) {
                synchronized (this.mLock) {
                    this.mCallbacksRunning = false;
                    while (true) {
                        CallbackRecord callbackRecord3 = callbackRecordExtractDueCallbacksLocked.next;
                        recycleCallbackLocked(callbackRecordExtractDueCallbacksLocked);
                        if (callbackRecord3 == null) {
                            Trace.traceEnd(8L);
                            throw th;
                        }
                        callbackRecordExtractDueCallbacksLocked = callbackRecord3;
                    }
                }
            }
        }
    }

    void doScheduleVsync() {
        synchronized (this.mLock) {
            if (this.mFrameScheduled) {
                scheduleVsyncLocked();
            }
        }
    }

    void doScheduleCallback(int i) {
        synchronized (this.mLock) {
            if (!this.mFrameScheduled) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (this.mCallbackQueues[i].hasDueCallbacksLocked(jUptimeMillis)) {
                    scheduleFrameLocked(jUptimeMillis);
                }
            }
        }
    }

    private void scheduleVsyncLocked() {
        this.mDisplayEventReceiver.scheduleVsync();
    }

    private boolean isRunningOnLooperThreadLocked() {
        return Looper.myLooper() == this.mLooper;
    }

    private CallbackRecord obtainCallbackLocked(long j, Object obj, Object obj2) {
        CallbackRecord callbackRecord = this.mCallbackPool;
        if (callbackRecord == null) {
            callbackRecord = new CallbackRecord();
        } else {
            this.mCallbackPool = callbackRecord.next;
            callbackRecord.next = null;
        }
        callbackRecord.dueTime = j;
        callbackRecord.action = obj;
        callbackRecord.token = obj2;
        return callbackRecord;
    }

    private void recycleCallbackLocked(CallbackRecord callbackRecord) {
        callbackRecord.action = null;
        callbackRecord.token = null;
        callbackRecord.next = this.mCallbackPool;
        this.mCallbackPool = callbackRecord;
    }

    private final class FrameHandler extends Handler {
        public FrameHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Choreographer.this.doFrame(System.nanoTime(), 0);
                    break;
                case 1:
                    Choreographer.this.doScheduleVsync();
                    break;
                case 2:
                    Choreographer.this.doScheduleCallback(message.arg1);
                    break;
            }
        }
    }

    private final class FrameDisplayEventReceiver extends DisplayEventReceiver implements Runnable {
        private int mFrame;
        private boolean mHavePendingVsync;
        private long mTimestampNanos;

        public FrameDisplayEventReceiver(Looper looper, int i) {
            super(looper, i);
        }

        @Override
        public void onVsync(long j, int i, int i2) {
            if (i != 0) {
                Log.d(Choreographer.TAG, "Received vsync from secondary display, but we don't support this case yet.  Choreographer needs a way to explicitly request vsync for a specific display to ensure it doesn't lose track of its scheduled vsync.");
                scheduleVsync();
                return;
            }
            long jNanoTime = System.nanoTime();
            if (j > jNanoTime) {
                Log.w(Choreographer.TAG, "Frame time is " + ((j - jNanoTime) * 1.0E-6f) + " ms in the future!  Check that graphics HAL is generating vsync timestamps using the correct timebase.");
                j = jNanoTime;
            }
            if (this.mHavePendingVsync) {
                Log.w(Choreographer.TAG, "Already have a pending vsync event.  There should only be one at a time.");
            } else {
                this.mHavePendingVsync = true;
            }
            this.mTimestampNanos = j;
            this.mFrame = i2;
            Message messageObtain = Message.obtain(Choreographer.this.mHandler, this);
            messageObtain.setAsynchronous(true);
            Choreographer.this.mHandler.sendMessageAtTime(messageObtain, j / TimeUtils.NANOS_PER_MS);
        }

        @Override
        public void run() {
            this.mHavePendingVsync = false;
            Choreographer.this.doFrame(this.mTimestampNanos, this.mFrame);
        }
    }

    private static final class CallbackRecord {
        public Object action;
        public long dueTime;
        public CallbackRecord next;
        public Object token;

        private CallbackRecord() {
        }

        public void run(long j) {
            if (this.token == Choreographer.FRAME_CALLBACK_TOKEN) {
                ((FrameCallback) this.action).doFrame(j);
            } else {
                ((Runnable) this.action).run();
            }
        }
    }

    private final class CallbackQueue {
        private CallbackRecord mHead;

        private CallbackQueue() {
        }

        public boolean hasDueCallbacksLocked(long j) {
            return this.mHead != null && this.mHead.dueTime <= j;
        }

        public CallbackRecord extractDueCallbacksLocked(long j) {
            CallbackRecord callbackRecord = this.mHead;
            if (callbackRecord == null || callbackRecord.dueTime > j) {
                return null;
            }
            CallbackRecord callbackRecord2 = callbackRecord.next;
            CallbackRecord callbackRecord3 = callbackRecord;
            while (true) {
                if (callbackRecord2 == null) {
                    break;
                }
                if (callbackRecord2.dueTime > j) {
                    callbackRecord3.next = null;
                    break;
                }
                callbackRecord3 = callbackRecord2;
                callbackRecord2 = callbackRecord2.next;
            }
            this.mHead = callbackRecord2;
            return callbackRecord;
        }

        public void addCallbackLocked(long j, Object obj, Object obj2) {
            CallbackRecord callbackRecordObtainCallbackLocked = Choreographer.this.obtainCallbackLocked(j, obj, obj2);
            CallbackRecord callbackRecord = this.mHead;
            if (callbackRecord == null) {
                this.mHead = callbackRecordObtainCallbackLocked;
                return;
            }
            if (j < callbackRecord.dueTime) {
                callbackRecordObtainCallbackLocked.next = callbackRecord;
                this.mHead = callbackRecordObtainCallbackLocked;
                return;
            }
            while (true) {
                if (callbackRecord.next == null) {
                    break;
                }
                if (j < callbackRecord.next.dueTime) {
                    callbackRecordObtainCallbackLocked.next = callbackRecord.next;
                    break;
                }
                callbackRecord = callbackRecord.next;
            }
            callbackRecord.next = callbackRecordObtainCallbackLocked;
        }

        public void removeCallbacksLocked(Object obj, Object obj2) {
            CallbackRecord callbackRecord = this.mHead;
            CallbackRecord callbackRecord2 = null;
            while (callbackRecord != null) {
                CallbackRecord callbackRecord3 = callbackRecord.next;
                if ((obj == null || callbackRecord.action == obj) && (obj2 == null || callbackRecord.token == obj2)) {
                    if (callbackRecord2 != null) {
                        callbackRecord2.next = callbackRecord3;
                    } else {
                        this.mHead = callbackRecord3;
                    }
                    Choreographer.this.recycleCallbackLocked(callbackRecord);
                } else {
                    callbackRecord2 = callbackRecord;
                }
                callbackRecord = callbackRecord3;
            }
        }
    }
}
