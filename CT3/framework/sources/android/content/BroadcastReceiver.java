package android.content;

import android.app.ActivityManagerNative;
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

        public PendingResult(int resultCode, String resultData, Bundle resultExtras, int type, boolean ordered, boolean sticky, IBinder token, int userId, int flags) {
            this.mResultCode = resultCode;
            this.mResultData = resultData;
            this.mResultExtras = resultExtras;
            this.mType = type;
            this.mOrderedHint = ordered;
            this.mInitialStickyHint = sticky;
            this.mToken = token;
            this.mSendingUser = userId;
            this.mFlags = flags;
        }

        public final void setResultCode(int code) {
            checkSynchronousHint();
            this.mResultCode = code;
        }

        public final int getResultCode() {
            return this.mResultCode;
        }

        public final void setResultData(String data) {
            checkSynchronousHint();
            this.mResultData = data;
        }

        public final String getResultData() {
            return this.mResultData;
        }

        public final void setResultExtras(Bundle extras) {
            checkSynchronousHint();
            this.mResultExtras = extras;
        }

        public final Bundle getResultExtras(boolean makeMap) {
            Bundle e = this.mResultExtras;
            if (makeMap && e == null) {
                Bundle e2 = new Bundle();
                this.mResultExtras = e2;
                return e2;
            }
            return e;
        }

        public final void setResult(int code, String data, Bundle extras) {
            checkSynchronousHint();
            this.mResultCode = code;
            this.mResultData = data;
            this.mResultExtras = extras;
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
            if (this.mType == 0) {
                final IActivityManager mgr = ActivityManagerNative.getDefault();
                if (QueuedWork.hasPendingWork()) {
                    QueuedWork.singleThreadExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (ActivityThread.DEBUG_BROADCAST) {
                                Slog.i(ActivityThread.TAG, "Finishing broadcast after work to component " + PendingResult.this.mToken);
                            }
                            PendingResult.this.sendFinished(mgr);
                        }
                    });
                    return;
                }
                if (ActivityThread.DEBUG_BROADCAST) {
                    Slog.i(ActivityThread.TAG, "Finishing broadcast to component " + this.mToken);
                }
                sendFinished(mgr);
                return;
            }
            if (!this.mOrderedHint || this.mType == 2) {
                return;
            }
            if (ActivityThread.DEBUG_BROADCAST) {
                Slog.i(ActivityThread.TAG, "Finishing broadcast to " + this.mToken);
            }
            sendFinished(ActivityManagerNative.getDefault());
        }

        public void setExtrasClassLoader(ClassLoader cl) {
            if (this.mResultExtras == null) {
                return;
            }
            this.mResultExtras.setClassLoader(cl);
        }

        public void sendFinished(IActivityManager am) {
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
                        if (this.mType == 1) {
                            Slog.d(ActivityThread.TAG, "BDC-Calling finishReceiver: IIntentReceiver=" + Integer.toHexString(System.identityHashCode(this.mToken)));
                        }
                        am.finishReceiver(this.mToken, this.mResultCode, this.mResultData, this.mResultExtras, this.mAbortBroadcast, this.mFlags);
                    } else {
                        am.finishReceiver(this.mToken, 0, null, null, false, this.mFlags);
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
            RuntimeException e = new RuntimeException("BroadcastReceiver trying to return result during a non-ordered broadcast");
            e.fillInStackTrace();
            Log.e("BroadcastReceiver", e.getMessage(), e);
        }
    }

    public final PendingResult goAsync() {
        PendingResult res = this.mPendingResult;
        this.mPendingResult = null;
        return res;
    }

    public IBinder peekService(Context myContext, Intent service) {
        IActivityManager am = ActivityManagerNative.getDefault();
        try {
            service.prepareToLeaveProcess(myContext);
            IBinder binder = am.peekService(service, service.resolveTypeIfNeeded(myContext.getContentResolver()), myContext.getOpPackageName());
            return binder;
        } catch (RemoteException e) {
            return null;
        }
    }

    public final void setResultCode(int code) {
        checkSynchronousHint();
        this.mPendingResult.mResultCode = code;
    }

    public final int getResultCode() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mResultCode;
        }
        return 0;
    }

    public final void setResultData(String data) {
        checkSynchronousHint();
        this.mPendingResult.mResultData = data;
    }

    public final String getResultData() {
        if (this.mPendingResult != null) {
            return this.mPendingResult.mResultData;
        }
        return null;
    }

    public final void setResultExtras(Bundle extras) {
        checkSynchronousHint();
        this.mPendingResult.mResultExtras = extras;
    }

    public final Bundle getResultExtras(boolean makeMap) {
        if (this.mPendingResult == null) {
            return null;
        }
        Bundle e = this.mPendingResult.mResultExtras;
        if (makeMap && e == null) {
            PendingResult pendingResult = this.mPendingResult;
            Bundle e2 = new Bundle();
            pendingResult.mResultExtras = e2;
            return e2;
        }
        return e;
    }

    public final void setResult(int code, String data, Bundle extras) {
        checkSynchronousHint();
        this.mPendingResult.mResultCode = code;
        this.mPendingResult.mResultData = data;
        this.mPendingResult.mResultExtras = extras;
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
        if (this.mPendingResult == null) {
            return;
        }
        this.mPendingResult.mAbortBroadcast = false;
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

    public final void setOrderedHint(boolean isOrdered) {
    }

    public final void setPendingResult(PendingResult result) {
        this.mPendingResult = result;
    }

    public final PendingResult getPendingResult() {
        return this.mPendingResult;
    }

    public int getSendingUserId() {
        return this.mPendingResult.mSendingUser;
    }

    public final void setDebugUnregister(boolean debug) {
        this.mDebugUnregister = debug;
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
        RuntimeException e = new RuntimeException("BroadcastReceiver trying to return result during a non-ordered broadcast");
        e.fillInStackTrace();
        Log.e("BroadcastReceiver", e.getMessage(), e);
    }
}
