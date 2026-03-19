package com.android.server.dreams;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.service.dreams.IDreamService;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.logging.MetricsLogger;
import com.android.server.NetworkManagementService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

final class DreamController {
    private static final int DREAM_CONNECTION_TIMEOUT = 5000;
    private static final int DREAM_FINISH_TIMEOUT = 5000;
    private static final String TAG = "DreamController";
    private final Context mContext;
    private DreamRecord mCurrentDream;
    private long mDreamStartTime;
    private final Handler mHandler;
    private final Listener mListener;
    private final Intent mDreamingStartedIntent = new Intent("android.intent.action.DREAMING_STARTED").addFlags(1073741824);
    private final Intent mDreamingStoppedIntent = new Intent("android.intent.action.DREAMING_STOPPED").addFlags(1073741824);
    private final Runnable mStopUnconnectedDreamRunnable = new Runnable() {
        @Override
        public void run() {
            if (DreamController.this.mCurrentDream != null && DreamController.this.mCurrentDream.mBound && !DreamController.this.mCurrentDream.mConnected) {
                Slog.w(DreamController.TAG, "Bound dream did not connect in the time allotted");
                DreamController.this.stopDream(true);
            }
        }
    };
    private final Runnable mStopStubbornDreamRunnable = new Runnable() {
        @Override
        public void run() {
            Slog.w(DreamController.TAG, "Stubborn dream did not finish itself in the time allotted");
            DreamController.this.stopDream(true);
        }
    };
    private final IWindowManager mIWindowManager = WindowManagerGlobal.getWindowManagerService();
    private final Intent mCloseNotificationShadeIntent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");

    public interface Listener {
        void onDreamStopped(Binder binder);
    }

    public DreamController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mCloseNotificationShadeIntent.putExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, "dream");
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Dreamland:");
        if (this.mCurrentDream != null) {
            printWriter.println("  mCurrentDream:");
            printWriter.println("    mToken=" + this.mCurrentDream.mToken);
            printWriter.println("    mName=" + this.mCurrentDream.mName);
            printWriter.println("    mIsTest=" + this.mCurrentDream.mIsTest);
            printWriter.println("    mCanDoze=" + this.mCurrentDream.mCanDoze);
            printWriter.println("    mUserId=" + this.mCurrentDream.mUserId);
            printWriter.println("    mBound=" + this.mCurrentDream.mBound);
            printWriter.println("    mService=" + this.mCurrentDream.mService);
            printWriter.println("    mSentStartBroadcast=" + this.mCurrentDream.mSentStartBroadcast);
            printWriter.println("    mWakingGently=" + this.mCurrentDream.mWakingGently);
            return;
        }
        printWriter.println("  mCurrentDream: null");
    }

    public void startDream(Binder binder, ComponentName componentName, boolean z, boolean z2, int i, PowerManager.WakeLock wakeLock) {
        Intent intent;
        stopDream(true);
        Trace.traceBegin(131072L, "startDream");
        try {
            this.mContext.sendBroadcastAsUser(this.mCloseNotificationShadeIntent, UserHandle.ALL);
            Slog.i(TAG, "Starting dream: name=" + componentName + ", isTest=" + z + ", canDoze=" + z2 + ", userId=" + i);
            this.mCurrentDream = new DreamRecord(binder, componentName, z, z2, i, wakeLock);
            this.mDreamStartTime = SystemClock.elapsedRealtime();
            MetricsLogger.visible(this.mContext, this.mCurrentDream.mCanDoze ? NetworkManagementService.NetdResponseCode.ClatdStatusResult : NetworkManagementService.NetdResponseCode.DnsProxyQueryResult);
            this.mIWindowManager.addWindowToken(binder, 2023, 0);
            intent = new Intent("android.service.dreams.DreamService");
            intent.setComponent(componentName);
            intent.addFlags(DumpState.DUMP_VOLUMES);
            if (this.mContext.bindServiceAsUser(intent, this.mCurrentDream, 67108865, new UserHandle(i))) {
                this.mCurrentDream.mBound = true;
                this.mHandler.postDelayed(this.mStopUnconnectedDreamRunnable, 5000L);
                return;
            }
            Slog.e(TAG, "Unable to bind dream service: " + intent);
            stopDream(true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to add window token for dream.", e);
            stopDream(true);
        } catch (SecurityException e2) {
            Slog.e(TAG, "Unable to bind dream service: " + intent, e2);
            stopDream(true);
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    public void stopDream(boolean z) {
        if (this.mCurrentDream == null) {
            return;
        }
        Trace.traceBegin(131072L, "stopDream");
        if (!z) {
            try {
                if (this.mCurrentDream.mWakingGently) {
                    return;
                }
                if (this.mCurrentDream.mService != null) {
                    this.mCurrentDream.mWakingGently = true;
                    try {
                        this.mCurrentDream.mService.wakeUp();
                        this.mHandler.postDelayed(this.mStopStubbornDreamRunnable, 5000L);
                        return;
                    } catch (RemoteException e) {
                    }
                }
            } finally {
                Trace.traceEnd(131072L);
            }
        }
        final DreamRecord dreamRecord = this.mCurrentDream;
        this.mCurrentDream = null;
        Slog.i(TAG, "Stopping dream: name=" + dreamRecord.mName + ", isTest=" + dreamRecord.mIsTest + ", canDoze=" + dreamRecord.mCanDoze + ", userId=" + dreamRecord.mUserId);
        MetricsLogger.hidden(this.mContext, dreamRecord.mCanDoze ? NetworkManagementService.NetdResponseCode.ClatdStatusResult : NetworkManagementService.NetdResponseCode.DnsProxyQueryResult);
        MetricsLogger.histogram(this.mContext, dreamRecord.mCanDoze ? "dozing_minutes" : "dreaming_minutes", (int) ((SystemClock.elapsedRealtime() - this.mDreamStartTime) / 60000));
        this.mHandler.removeCallbacks(this.mStopUnconnectedDreamRunnable);
        this.mHandler.removeCallbacks(this.mStopStubbornDreamRunnable);
        if (dreamRecord.mSentStartBroadcast) {
            this.mContext.sendBroadcastAsUser(this.mDreamingStoppedIntent, UserHandle.ALL);
        }
        if (dreamRecord.mService != null) {
            try {
                dreamRecord.mService.detach();
            } catch (RemoteException e2) {
            }
            try {
                dreamRecord.mService.asBinder().unlinkToDeath(dreamRecord, 0);
            } catch (NoSuchElementException e3) {
            }
            dreamRecord.mService = null;
        }
        if (dreamRecord.mBound) {
            this.mContext.unbindService(dreamRecord);
        }
        dreamRecord.releaseWakeLockIfNeeded();
        try {
            this.mIWindowManager.removeWindowToken(dreamRecord.mToken, 0);
        } catch (RemoteException e4) {
            Slog.w(TAG, "Error removing window token for dream.", e4);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                DreamController.this.mListener.onDreamStopped(dreamRecord.mToken);
            }
        });
    }

    private void attach(IDreamService iDreamService) {
        try {
            iDreamService.asBinder().linkToDeath(this.mCurrentDream, 0);
            iDreamService.attach(this.mCurrentDream.mToken, this.mCurrentDream.mCanDoze, this.mCurrentDream.mDreamingStartedCallback);
            this.mCurrentDream.mService = iDreamService;
            if (!this.mCurrentDream.mIsTest) {
                this.mContext.sendBroadcastAsUser(this.mDreamingStartedIntent, UserHandle.ALL);
                this.mCurrentDream.mSentStartBroadcast = true;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "The dream service died unexpectedly.", e);
            stopDream(true);
        }
    }

    private final class DreamRecord implements IBinder.DeathRecipient, ServiceConnection {
        public boolean mBound;
        public final boolean mCanDoze;
        public boolean mConnected;
        public final boolean mIsTest;
        public final ComponentName mName;
        public boolean mSentStartBroadcast;
        public IDreamService mService;
        public final Binder mToken;
        public final int mUserId;
        public PowerManager.WakeLock mWakeLock;
        public boolean mWakingGently;
        final Runnable mReleaseWakeLockIfNeeded = new Runnable() {
            @Override
            public final void run() {
                this.f$0.releaseWakeLockIfNeeded();
            }
        };
        final IRemoteCallback mDreamingStartedCallback = new IRemoteCallback.Stub() {
            public void sendResult(Bundle bundle) throws RemoteException {
                DreamController.this.mHandler.post(DreamRecord.this.mReleaseWakeLockIfNeeded);
            }
        };

        public DreamRecord(Binder binder, ComponentName componentName, boolean z, boolean z2, int i, PowerManager.WakeLock wakeLock) {
            this.mToken = binder;
            this.mName = componentName;
            this.mIsTest = z;
            this.mCanDoze = z2;
            this.mUserId = i;
            this.mWakeLock = wakeLock;
            this.mWakeLock.acquire();
            DreamController.this.mHandler.postDelayed(this.mReleaseWakeLockIfNeeded, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }

        @Override
        public void binderDied() {
            DreamController.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamRecord.this.mService = null;
                    if (DreamController.this.mCurrentDream == DreamRecord.this) {
                        DreamController.this.stopDream(true);
                    }
                }
            });
        }

        @Override
        public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
            DreamController.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamRecord.this.mConnected = true;
                    if (DreamController.this.mCurrentDream == DreamRecord.this && DreamRecord.this.mService == null) {
                        DreamController.this.attach(IDreamService.Stub.asInterface(iBinder));
                    } else {
                        DreamRecord.this.releaseWakeLockIfNeeded();
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            DreamController.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamRecord.this.mService = null;
                    if (DreamController.this.mCurrentDream == DreamRecord.this) {
                        DreamController.this.stopDream(true);
                    }
                }
            });
        }

        void releaseWakeLockIfNeeded() {
            if (this.mWakeLock != null) {
                this.mWakeLock.release();
                this.mWakeLock = null;
                DreamController.this.mHandler.removeCallbacks(this.mReleaseWakeLockIfNeeded);
            }
        }
    }
}
