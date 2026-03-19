package android.app;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ExponentiallyBucketedHistogram;
import java.util.Iterator;
import java.util.LinkedList;

public class QueuedWork {
    private static final boolean DEBUG = false;
    private static final long DELAY = 100;
    private static final long MAX_WAIT_TIME_MILLIS = 512;
    private static final String LOG_TAG = QueuedWork.class.getSimpleName();
    private static final Object sLock = new Object();
    private static Object sProcessingWork = new Object();

    @GuardedBy("sLock")
    private static final LinkedList<Runnable> sFinishers = new LinkedList<>();

    @GuardedBy("sLock")
    private static Handler sHandler = null;

    @GuardedBy("sLock")
    private static final LinkedList<Runnable> sWork = new LinkedList<>();

    @GuardedBy("sLock")
    private static boolean sCanDelay = true;

    @GuardedBy("sLock")
    private static final ExponentiallyBucketedHistogram mWaitTimes = new ExponentiallyBucketedHistogram(16);
    private static int mNumWaits = 0;

    private static Handler getHandler() {
        Handler handler;
        synchronized (sLock) {
            if (sHandler == null) {
                HandlerThread handlerThread = new HandlerThread("queued-work-looper", -2);
                handlerThread.start();
                sHandler = new QueuedWorkHandler(handlerThread.getLooper());
            }
            handler = sHandler;
        }
        return handler;
    }

    public static void addFinisher(Runnable runnable) {
        synchronized (sLock) {
            sFinishers.add(runnable);
        }
    }

    public static void removeFinisher(Runnable runnable) {
        synchronized (sLock) {
            sFinishers.remove(runnable);
        }
    }

    public static void waitToFinish() {
        Runnable runnablePoll;
        long jCurrentTimeMillis = System.currentTimeMillis();
        Handler handler = getHandler();
        synchronized (sLock) {
            if (handler.hasMessages(1)) {
                handler.removeMessages(1);
            }
            sCanDelay = false;
        }
        StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
        try {
            processPendingWork();
            while (true) {
                try {
                    synchronized (sLock) {
                        runnablePoll = sFinishers.poll();
                    }
                    if (runnablePoll == null) {
                        break;
                    } else {
                        runnablePoll.run();
                    }
                } catch (Throwable th) {
                    sCanDelay = true;
                    throw th;
                }
            }
            sCanDelay = true;
            synchronized (sLock) {
                long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                if (jCurrentTimeMillis2 > 0) {
                    mWaitTimes.add(Long.valueOf(jCurrentTimeMillis2).intValue());
                    mNumWaits++;
                    if (mNumWaits % 1024 == 0 || jCurrentTimeMillis2 > 512) {
                        mWaitTimes.log(LOG_TAG, "waited: ");
                    }
                }
            }
        } finally {
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
        }
    }

    public static void queue(Runnable runnable, boolean z) {
        Handler handler = getHandler();
        synchronized (sLock) {
            sWork.add(runnable);
            if (z && sCanDelay) {
                handler.sendEmptyMessageDelayed(1, DELAY);
            } else {
                handler.sendEmptyMessage(1);
            }
        }
    }

    public static boolean hasPendingWork() {
        boolean z;
        synchronized (sLock) {
            z = !sWork.isEmpty();
        }
        return z;
    }

    private static void processPendingWork() {
        LinkedList linkedList;
        synchronized (sProcessingWork) {
            synchronized (sLock) {
                linkedList = (LinkedList) sWork.clone();
                sWork.clear();
                getHandler().removeMessages(1);
            }
            if (linkedList.size() > 0) {
                Iterator it = linkedList.iterator();
                while (it.hasNext()) {
                    ((Runnable) it.next()).run();
                }
            }
        }
    }

    private static class QueuedWorkHandler extends Handler {
        static final int MSG_RUN = 1;

        QueuedWorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                QueuedWork.processPendingWork();
            }
        }
    }
}
