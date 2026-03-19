package com.android.providers.contacts;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ContactsTaskScheduler {
    public static final boolean VERBOSE_LOGGING = AbstractContactsProvider.VERBOSE_LOGGING;
    private MyHandler mHandler;
    private final Object mLock;
    private final String mName;
    private final Runnable mQuitter;
    private final int mShutdownTimeoutSeconds;
    private HandlerThread mThread;
    private final AtomicInteger mThreadSequenceNumber;

    public abstract void onPerformTask(int i, Object obj);

    public ContactsTaskScheduler(String str) {
        this(str, 60);
    }

    protected ContactsTaskScheduler(String str, int i) {
        this.mThreadSequenceNumber = new AtomicInteger();
        this.mLock = new Object();
        this.mQuitter = new Runnable() {
            @Override
            public final void run() {
                ContactsTaskScheduler.lambda$new$0(this.f$0);
            }
        };
        this.mName = str;
        this.mShutdownTimeoutSeconds = i;
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (ContactsTaskScheduler.VERBOSE_LOGGING) {
                Log.v("ContactsTaskScheduler", "[" + ContactsTaskScheduler.this.mName + "] " + ContactsTaskScheduler.this.mThread + " dispatching " + message.what);
            }
            ContactsTaskScheduler.this.onPerformTask(message.what, message.obj);
        }
    }

    public static void lambda$new$0(ContactsTaskScheduler contactsTaskScheduler) {
        synchronized (contactsTaskScheduler.mLock) {
            contactsTaskScheduler.stopThread(false);
        }
    }

    private boolean isRunning() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mThread != null;
        }
        return z;
    }

    @VisibleForTesting
    public void scheduleTask(int i) {
        scheduleTask(i, null);
    }

    @VisibleForTesting
    public void scheduleTask(int i, Object obj) {
        synchronized (this.mLock) {
            if (!isRunning()) {
                this.mThread = new HandlerThread("Worker-" + this.mThreadSequenceNumber.incrementAndGet());
                this.mThread.start();
                this.mHandler = new MyHandler(this.mThread.getLooper());
                if (VERBOSE_LOGGING) {
                    Log.v("ContactsTaskScheduler", "[" + this.mName + "] " + this.mThread + " started.");
                }
            }
            if (obj == null) {
                this.mHandler.sendEmptyMessage(i);
            } else {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(i, obj));
            }
            this.mHandler.removeCallbacks(this.mQuitter);
            this.mHandler.postDelayed(this.mQuitter, this.mShutdownTimeoutSeconds * 1000);
        }
    }

    @VisibleForTesting
    public void shutdownForTest() {
        stopThread(true);
    }

    private void stopThread(boolean z) {
        synchronized (this.mLock) {
            if (VERBOSE_LOGGING) {
                Log.v("ContactsTaskScheduler", "[" + this.mName + "] " + this.mThread + " stopping...");
            }
            if (this.mThread != null) {
                this.mThread.quit();
                if (z) {
                    try {
                        this.mThread.join();
                    } catch (InterruptedException e) {
                    }
                }
            }
            this.mThread = null;
            this.mHandler = null;
        }
    }

    @VisibleForTesting
    public int getThreadSequenceNumber() {
        return this.mThreadSequenceNumber.get();
    }

    @VisibleForTesting
    public boolean isRunningForTest() {
        return isRunning();
    }
}
