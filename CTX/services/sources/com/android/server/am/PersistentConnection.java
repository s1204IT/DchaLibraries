package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;

public abstract class PersistentConnection<T> {
    private static final boolean DEBUG = false;

    @GuardedBy("mLock")
    private boolean mBound;
    private final ComponentName mComponentName;
    private final Context mContext;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private boolean mIsConnected;
    private long mNextBackoffMs;
    private final double mRebindBackoffIncrease;
    private final long mRebindBackoffMs;
    private final long mRebindMaxBackoffMs;

    @GuardedBy("mLock")
    private boolean mRebindScheduled;
    private long mReconnectTime;

    @GuardedBy("mLock")
    private T mService;

    @GuardedBy("mLock")
    private boolean mShouldBeBound;
    private final String mTag;
    private final int mUserId;
    private final Object mLock = new Object();
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (PersistentConnection.this.mLock) {
                if (!PersistentConnection.this.mBound) {
                    Slog.w(PersistentConnection.this.mTag, "Connected: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId + " but not bound, ignore.");
                    return;
                }
                Slog.i(PersistentConnection.this.mTag, "Connected: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId);
                PersistentConnection.this.mIsConnected = true;
                PersistentConnection.this.mService = PersistentConnection.this.asInterface(iBinder);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (PersistentConnection.this.mLock) {
                Slog.i(PersistentConnection.this.mTag, "Disconnected: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId);
                PersistentConnection.this.cleanUpConnectionLocked();
            }
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            synchronized (PersistentConnection.this.mLock) {
                if (!PersistentConnection.this.mBound) {
                    Slog.w(PersistentConnection.this.mTag, "Binding died: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId + " but not bound, ignore.");
                    return;
                }
                Slog.w(PersistentConnection.this.mTag, "Binding died: " + PersistentConnection.this.mComponentName.flattenToShortString() + " u" + PersistentConnection.this.mUserId);
                PersistentConnection.this.scheduleRebindLocked();
            }
        }
    };
    private final Runnable mBindForBackoffRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.bindForBackoff();
        }
    };

    protected abstract T asInterface(IBinder iBinder);

    public PersistentConnection(String str, Context context, Handler handler, int i, ComponentName componentName, long j, double d, long j2) {
        this.mTag = str;
        this.mContext = context;
        this.mHandler = handler;
        this.mUserId = i;
        this.mComponentName = componentName;
        this.mRebindBackoffMs = j * 1000;
        this.mRebindBackoffIncrease = d;
        this.mRebindMaxBackoffMs = j2 * 1000;
        this.mNextBackoffMs = this.mRebindBackoffMs;
    }

    public final ComponentName getComponentName() {
        return this.mComponentName;
    }

    public final boolean isBound() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mBound;
        }
        return z;
    }

    public final boolean isRebindScheduled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mRebindScheduled;
        }
        return z;
    }

    public final boolean isConnected() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsConnected;
        }
        return z;
    }

    public final T getServiceBinder() {
        T t;
        synchronized (this.mLock) {
            t = this.mService;
        }
        return t;
    }

    public final void bind() {
        synchronized (this.mLock) {
            this.mShouldBeBound = true;
            bindInnerLocked(true);
        }
    }

    @GuardedBy("mLock")
    public final void bindInnerLocked(boolean z) {
        unscheduleRebindLocked();
        if (this.mBound) {
            return;
        }
        this.mBound = true;
        if (z) {
            this.mNextBackoffMs = this.mRebindBackoffMs;
        }
        Intent component = new Intent().setComponent(this.mComponentName);
        if (!this.mContext.bindServiceAsUser(component, this.mServiceConnection, 67108865, this.mHandler, UserHandle.of(this.mUserId))) {
            Slog.e(this.mTag, "Binding: " + component.getComponent() + " u" + this.mUserId + " failed.");
        }
    }

    final void bindForBackoff() {
        synchronized (this.mLock) {
            if (this.mShouldBeBound) {
                bindInnerLocked(false);
            }
        }
    }

    @GuardedBy("mLock")
    private void cleanUpConnectionLocked() {
        this.mIsConnected = false;
        this.mService = null;
    }

    public final void unbind() {
        synchronized (this.mLock) {
            this.mShouldBeBound = false;
            unbindLocked();
        }
    }

    @GuardedBy("mLock")
    private final void unbindLocked() {
        unscheduleRebindLocked();
        if (!this.mBound) {
            return;
        }
        Slog.i(this.mTag, "Stopping: " + this.mComponentName.flattenToShortString() + " u" + this.mUserId);
        this.mBound = false;
        this.mContext.unbindService(this.mServiceConnection);
        cleanUpConnectionLocked();
    }

    @GuardedBy("mLock")
    void unscheduleRebindLocked() {
        injectRemoveCallbacks(this.mBindForBackoffRunnable);
        this.mRebindScheduled = false;
    }

    @GuardedBy("mLock")
    void scheduleRebindLocked() {
        unbindLocked();
        if (!this.mRebindScheduled) {
            Slog.i(this.mTag, "Scheduling to reconnect in " + this.mNextBackoffMs + " ms (uptime)");
            this.mReconnectTime = injectUptimeMillis() + this.mNextBackoffMs;
            injectPostAtTime(this.mBindForBackoffRunnable, this.mReconnectTime);
            this.mNextBackoffMs = Math.min(this.mRebindMaxBackoffMs, (long) (((double) this.mNextBackoffMs) * this.mRebindBackoffIncrease));
            this.mRebindScheduled = true;
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.print(str);
            printWriter.print(this.mComponentName.flattenToShortString());
            printWriter.print(this.mBound ? "  [bound]" : "  [not bound]");
            printWriter.print(this.mIsConnected ? "  [connected]" : "  [not connected]");
            if (this.mRebindScheduled) {
                printWriter.print("  reconnect in ");
                TimeUtils.formatDuration(this.mReconnectTime - injectUptimeMillis(), printWriter);
            }
            printWriter.println();
            printWriter.print(str);
            printWriter.print("  Next backoff(sec): ");
            printWriter.print(this.mNextBackoffMs / 1000);
        }
    }

    @VisibleForTesting
    void injectRemoveCallbacks(Runnable runnable) {
        this.mHandler.removeCallbacks(runnable);
    }

    @VisibleForTesting
    void injectPostAtTime(Runnable runnable, long j) {
        this.mHandler.postAtTime(runnable, j);
    }

    @VisibleForTesting
    long injectUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    @VisibleForTesting
    long getNextBackoffMsForTest() {
        return this.mNextBackoffMs;
    }

    @VisibleForTesting
    long getReconnectTimeForTest() {
        return this.mReconnectTime;
    }

    @VisibleForTesting
    ServiceConnection getServiceConnectionForTest() {
        return this.mServiceConnection;
    }

    @VisibleForTesting
    Runnable getBindForBackoffRunnableForTest() {
        return this.mBindForBackoffRunnable;
    }

    @VisibleForTesting
    boolean shouldBeBoundForTest() {
        return this.mShouldBeBound;
    }
}
