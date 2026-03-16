package com.android.server.dreams;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.service.dreams.IDreamService;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

final class DreamController {
    private static final int DREAM_CONNECTION_TIMEOUT = 5000;
    private static final int DREAM_FINISH_TIMEOUT = 5000;
    private static final String TAG = "DreamController";
    private final Context mContext;
    private DreamRecord mCurrentDream;
    private final Handler mHandler;
    private final Listener mListener;
    private final Intent mDreamingStartedIntent = new Intent("android.intent.action.DREAMING_STARTED").addFlags(1073741824);
    private final Intent mDreamingStoppedIntent = new Intent("android.intent.action.DREAMING_STOPPED").addFlags(1073741824);
    private final Intent mCloseNotificationShadeIntent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
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

    public interface Listener {
        void onDreamStopped(Binder binder);
    }

    public DreamController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
    }

    public void dump(PrintWriter pw) {
        pw.println("Dreamland:");
        if (this.mCurrentDream != null) {
            pw.println("  mCurrentDream:");
            pw.println("    mToken=" + this.mCurrentDream.mToken);
            pw.println("    mName=" + this.mCurrentDream.mName);
            pw.println("    mIsTest=" + this.mCurrentDream.mIsTest);
            pw.println("    mCanDoze=" + this.mCurrentDream.mCanDoze);
            pw.println("    mUserId=" + this.mCurrentDream.mUserId);
            pw.println("    mBound=" + this.mCurrentDream.mBound);
            pw.println("    mService=" + this.mCurrentDream.mService);
            pw.println("    mSentStartBroadcast=" + this.mCurrentDream.mSentStartBroadcast);
            pw.println("    mWakingGently=" + this.mCurrentDream.mWakingGently);
            return;
        }
        pw.println("  mCurrentDream: null");
    }

    public void startDream(Binder token, ComponentName name, boolean isTest, boolean canDoze, int userId) {
        Intent intent;
        stopDream(true);
        Trace.traceBegin(131072L, "startDream");
        try {
            this.mContext.sendBroadcastAsUser(this.mCloseNotificationShadeIntent, UserHandle.ALL);
            Slog.i(TAG, "Starting dream: name=" + name + ", isTest=" + isTest + ", canDoze=" + canDoze + ", userId=" + userId);
            this.mCurrentDream = new DreamRecord(token, name, isTest, canDoze, userId);
            this.mIWindowManager.addWindowToken(token, 2023);
            intent = new Intent("android.service.dreams.DreamService");
            intent.setComponent(name);
            intent.addFlags(8388608);
            if (this.mContext.bindServiceAsUser(intent, this.mCurrentDream, 1, new UserHandle(userId))) {
                this.mCurrentDream.mBound = true;
                this.mHandler.postDelayed(this.mStopUnconnectedDreamRunnable, 5000L);
            } else {
                Slog.e(TAG, "Unable to bind dream service: " + intent);
                stopDream(true);
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "Unable to add window token for dream.", ex);
            stopDream(true);
        } catch (SecurityException ex2) {
            Slog.e(TAG, "Unable to bind dream service: " + intent, ex2);
            stopDream(true);
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    public void stopDream(boolean immediate) {
        if (this.mCurrentDream == null) {
            return;
        }
        Trace.traceBegin(131072L, "stopDream");
        if (!immediate) {
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
        final DreamRecord oldDream = this.mCurrentDream;
        this.mCurrentDream = null;
        Slog.i(TAG, "Stopping dream: name=" + oldDream.mName + ", isTest=" + oldDream.mIsTest + ", canDoze=" + oldDream.mCanDoze + ", userId=" + oldDream.mUserId);
        this.mHandler.removeCallbacks(this.mStopUnconnectedDreamRunnable);
        this.mHandler.removeCallbacks(this.mStopStubbornDreamRunnable);
        if (oldDream.mSentStartBroadcast) {
            this.mContext.sendBroadcastAsUser(this.mDreamingStoppedIntent, UserHandle.ALL);
        }
        if (oldDream.mService != null) {
            try {
                oldDream.mService.detach();
            } catch (RemoteException e2) {
            }
            try {
                oldDream.mService.asBinder().unlinkToDeath(oldDream, 0);
            } catch (NoSuchElementException e3) {
            }
            oldDream.mService = null;
        }
        if (oldDream.mBound) {
            this.mContext.unbindService(oldDream);
        }
        try {
            this.mIWindowManager.removeWindowToken(oldDream.mToken);
        } catch (RemoteException ex) {
            Slog.w(TAG, "Error removing window token for dream.", ex);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                DreamController.this.mListener.onDreamStopped(oldDream.mToken);
            }
        });
    }

    private void attach(IDreamService service) {
        try {
            service.asBinder().linkToDeath(this.mCurrentDream, 0);
            service.attach(this.mCurrentDream.mToken, this.mCurrentDream.mCanDoze);
            this.mCurrentDream.mService = service;
            if (!this.mCurrentDream.mIsTest) {
                this.mContext.sendBroadcastAsUser(this.mDreamingStartedIntent, UserHandle.ALL);
                this.mCurrentDream.mSentStartBroadcast = true;
            }
        } catch (RemoteException ex) {
            Slog.e(TAG, "The dream service died unexpectedly.", ex);
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
        public boolean mWakingGently;

        public DreamRecord(Binder token, ComponentName name, boolean isTest, boolean canDoze, int userId) {
            this.mToken = token;
            this.mName = name;
            this.mIsTest = isTest;
            this.mCanDoze = canDoze;
            this.mUserId = userId;
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
        public void onServiceConnected(ComponentName name, final IBinder service) {
            DreamController.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DreamRecord.this.mConnected = true;
                    if (DreamController.this.mCurrentDream == DreamRecord.this && DreamRecord.this.mService == null) {
                        DreamController.this.attach(IDreamService.Stub.asInterface(service));
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
    }
}
