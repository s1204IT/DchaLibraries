package android.content;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.app.QueuedWork;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

public abstract class BroadcastReceiver {
    private boolean mDebugUnregister;
    private PendingResult mPendingResult;

    public abstract void onReceive(Context context, Intent intent);

    public static class PendingResult {
        public static final int TYPE_COMPONENT = 0;
        public static final int TYPE_REGISTERED = 1;
        public static final int TYPE_UNREGISTERED = 2;
        boolean mAbortBroadcast;
        boolean mFinished;
        final int mFlags;
        final boolean mInitialStickyHint;
        final boolean mOrderedHint;
        int mResultCode;
        String mResultData;
        Bundle mResultExtras;
        final int mSendingUser;
        final IBinder mToken;
        final int mType;

        public PendingResult(int i, String str, Bundle bundle, int i2, boolean z, boolean z2, IBinder iBinder, int i3, int i4) {
            this.mResultCode = i;
            this.mResultData = str;
            this.mResultExtras = bundle;
            this.mType = i2;
            this.mOrderedHint = z;
            this.mInitialStickyHint = z2;
            this.mToken = iBinder;
            this.mSendingUser = i3;
            this.mFlags = i4;
        }

        public final void setResultCode(int i) {
            checkSynchronousHint();
            this.mResultCode = i;
        }

        public final int getResultCode() {
            return this.mResultCode;
        }

        public final void setResultData(String str) {
            checkSynchronousHint();
            this.mResultData = str;
        }

        public final String getResultData() {
            return this.mResultData;
        }

        public final void setResultExtras(Bundle bundle) {
            checkSynchronousHint();
            this.mResultExtras = bundle;
        }

        public final Bundle getResultExtras(boolean z) {
            Bundle bundle = this.mResultExtras;
            if (z && bundle == null) {
                Bundle bundle2 = new Bundle();
                this.mResultExtras = bundle2;
                return bundle2;
            }
            return bundle;
        }

        public final void setResult(int i, String str, Bundle bundle) {
            checkSynchronousHint();
            this.mResultCode = i;
            this.mResultData = str;
            this.mResultExtras = bundle;
        }

        public final boolean getAbortBroadcast() {
            return this.mAbortBroadcast;
        }

        public final void abortBroadcast() {
            checkSynchronousHint();
            this.mAbortBroadcast = true;
        }

        public final void clearAbortBroadcast() {
            this.mAbortBroadcast = false;
        }

        public final void finish() {
            if (this.mType != 0) {
                if (this.mOrderedHint && this.mType != 2) {
                    if (ActivityThread.DEBUG_BROADCAST) {
                        Slog.i(ActivityThread.TAG, "Finishing broadcast to " + this.mToken);
                    }
                    sendFinished(ActivityManager.getService());
                    return;
                }
                return;
            }
            final IActivityManager service = ActivityManager.getService();
            if (QueuedWork.hasPendingWork()) {
                QueuedWork.queue(new Runnable() {
                    @Override
                    public void run() {
                        if (ActivityThread.DEBUG_BROADCAST) {
                            Slog.i(ActivityThread.TAG, "Finishing broadcast after work to component " + PendingResult.this.mToken);
                        }
                        PendingResult.this.sendFinished(service);
                    }
                }, false);
                return;
            }
            if (ActivityThread.DEBUG_BROADCAST) {
                Slog.i(ActivityThread.TAG, "Finishing broadcast to component " + this.mToken);
            }
            sendFinished(service);
        }

        public void setExtrasClassLoader(ClassLoader classLoader) {
            if (this.mResultExtras != null) {
                this.mResultExtras.setClassLoader(classLoader);
            }
        }

        public void sendFinished(IActivityManager iActivityManager) {
            synchronized (this) {
                if (this.mFinished) {
                    throw new IllegalStateException("Broadcast already finished");
                }
                this.mFinished = true;
                try {
                    if (this.mResultExtras != null) {
                        this.mResultExtras.setAllowFds(false);
                    }
                    if (this.mOrderedHint) {
                        iActivityManager.finishReceiver(this.mToken, this.mResultCode, this.mResultData, this.mResultExtras, this.mAbortBroadcast, this.mFlags);
                    } else {
                        iActivityManager.finishReceiver(this.mToken, 0, null, null, false, this.mFlags);
                    }
                } catch (RemoteException e) {
                }
            }
        }

        public int getSendingUserId() {
            return this.mSendingUser;
        }

        void checkSynchronousHint() {
            if (this.mOrderedHint || this.mInitialStickyHint) {
                return;
            }
            RuntimeException runtimeException = new RuntimeException("BroadcastReceiver trying to return result during a non-ordered broadcast");
            runtimeException.fillInStackTrace();
            Log.e("BroadcastReceiver", runtimeException.getMessage(), runtimeException);
        }
    }

    public final PendingResult goAsync() {
        PendingResult pendingResult = this.mPendingResult;
        this.mPendingResult = null;
        return pendingResult;
    }

    public IBinder peekService(Context context, Intent intent) {
        IActivityManager service = ActivityManager.getService();
        try {
            intent.prepareToLeaveProcess(context);
            return service.peekService(intent, intent.resolveTypeIfNeeded(context.getContentResolver()), context.getOpPackageName());
        } catch (RemoteException e) {
            return null;
        }
    }

    public final void setResultCode(int i) {
        checkSynchronousHint();
        this.mPendingResult.mResultCode = i;
    }

    public final int getResultCode() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mResultCode;
        }
        return 0;
    }

    public final void setResultData(String str) {
        checkSynchronousHint();
        this.mPendingResult.mResultData = str;
    }

    public final String getResultData() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mResultData;
        }
        return null;
    }

    public final void setResultExtras(Bundle bundle) {
        checkSynchronousHint();
        this.mPendingResult.mResultExtras = bundle;
    }

    public final Bundle getResultExtras(boolean z) {
        if (this.mPendingResult == null) {
            return null;
        }
        Bundle bundle = this.mPendingResult.mResultExtras;
        if (z && bundle == null) {
            PendingResult pendingResult = this.mPendingResult;
            Bundle bundle2 = new Bundle();
            pendingResult.mResultExtras = bundle2;
            return bundle2;
        }
        return bundle;
    }

    public final void setResult(int i, String str, Bundle bundle) {
        checkSynchronousHint();
        this.mPendingResult.mResultCode = i;
        this.mPendingResult.mResultData = str;
        this.mPendingResult.mResultExtras = bundle;
    }

    public final boolean getAbortBroadcast() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mAbortBroadcast;
        }
        return false;
    }

    public final void abortBroadcast() {
        checkSynchronousHint();
        this.mPendingResult.mAbortBroadcast = true;
    }

    public final void clearAbortBroadcast() {
        if (this.mPendingResult != null) {
            this.mPendingResult.mAbortBroadcast = false;
        }
    }

    public final boolean isOrderedBroadcast() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mOrderedHint;
        }
        return false;
    }

    public final boolean isInitialStickyBroadcast() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mInitialStickyHint;
        }
        return false;
    }

    public final void setOrderedHint(boolean z) {
    }

    public final void setPendingResult(PendingResult pendingResult) {
        this.mPendingResult = pendingResult;
    }

    public final PendingResult getPendingResult() {
        return this.mPendingResult;
    }

    public int getSendingUserId() {
        return this.mPendingResult.mSendingUser;
    }

    public final void setDebugUnregister(boolean z) {
        this.mDebugUnregister = z;
    }

    public final boolean getDebugUnregister() {
        return this.mDebugUnregister;
    }

    void checkSynchronousHint() {
        if (this.mPendingResult == null) {
            throw new IllegalStateException("Call while result is not pending");
        }
        if (this.mPendingResult.mOrderedHint || this.mPendingResult.mInitialStickyHint) {
            return;
        }
        RuntimeException runtimeException = new RuntimeException("BroadcastReceiver trying to return result during a non-ordered broadcast");
        runtimeException.fillInStackTrace();
        Log.e("BroadcastReceiver", runtimeException.getMessage(), runtimeException);
    }
}
