package com.android.server.location;

import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

abstract class RemoteListenerHelper<TListener extends IInterface> {
    private static final boolean DEBUG = false;
    protected static final int RESULT_GPS_LOCATION_DISABLED = 3;
    protected static final int RESULT_INTERNAL_ERROR = 4;
    protected static final int RESULT_NOT_ALLOWED = 6;
    protected static final int RESULT_NOT_AVAILABLE = 1;
    protected static final int RESULT_NOT_SUPPORTED = 2;
    protected static final int RESULT_SUCCESS = 0;
    protected static final int RESULT_UNKNOWN = 5;
    private static final int RUNNABLE_COUNT_LIMIT = 200;
    private final Handler mHandler;
    private boolean mHasIsSupported;
    private volatile boolean mIsRegistered;
    private boolean mIsSupported;
    private final String mTag;
    private final Map<IBinder, RemoteListenerHelper<TListener>.LinkedListener> mListenerMap = new HashMap();
    private int mLastReportedResult = 5;
    private int mInsertedRunnableCount = 0;
    private final Object mLock = new Object();
    private int mLogCount = 0;

    protected interface ListenerOperation<TListener extends IInterface> {
        void execute(TListener tlistener) throws RemoteException;
    }

    protected abstract ListenerOperation<TListener> getHandlerOperation(int i);

    protected abstract boolean isAvailableInPlatform();

    protected abstract boolean isGpsEnabled();

    protected abstract int registerWithService();

    protected abstract void unregisterFromService();

    static int access$610(RemoteListenerHelper remoteListenerHelper) {
        int i = remoteListenerHelper.mInsertedRunnableCount;
        remoteListenerHelper.mInsertedRunnableCount = i - 1;
        return i;
    }

    protected RemoteListenerHelper(Handler handler, String str) {
        Preconditions.checkNotNull(str);
        this.mHandler = handler;
        this.mTag = str;
    }

    public boolean isRegistered() {
        return this.mIsRegistered;
    }

    public boolean addListener(TListener tlistener) {
        int i;
        Preconditions.checkNotNull(tlistener, "Attempted to register a 'null' listener.");
        IBinder iBinderAsBinder = tlistener.asBinder();
        RemoteListenerHelper<TListener>.LinkedListener linkedListener = new LinkedListener(tlistener);
        synchronized (this.mListenerMap) {
            if (this.mListenerMap.containsKey(iBinderAsBinder)) {
                return true;
            }
            try {
                iBinderAsBinder.linkToDeath(linkedListener, 0);
                this.mListenerMap.put(iBinderAsBinder, linkedListener);
                Log.v(this.mTag, "addListener, current listener size: " + this.mListenerMap.size());
                if (isAvailableInPlatform()) {
                    if (this.mHasIsSupported && !this.mIsSupported) {
                        i = 2;
                    } else if (!isGpsEnabled()) {
                        i = 3;
                    } else {
                        if (!this.mHasIsSupported || !this.mIsSupported) {
                            return true;
                        }
                        tryRegister();
                        i = 0;
                    }
                } else {
                    i = 1;
                }
                post(tlistener, getHandlerOperation(i));
                return true;
            } catch (RemoteException e) {
                Log.v(this.mTag, "Remote listener already died.", e);
                return false;
            }
        }
    }

    public void removeListener(TListener tlistener) {
        RemoteListenerHelper<TListener>.LinkedListener linkedListenerRemove;
        Preconditions.checkNotNull(tlistener, "Attempted to remove a 'null' listener.");
        IBinder iBinderAsBinder = tlistener.asBinder();
        synchronized (this.mListenerMap) {
            linkedListenerRemove = this.mListenerMap.remove(iBinderAsBinder);
            if (this.mListenerMap.isEmpty()) {
                tryUnregister();
            }
        }
        if (linkedListenerRemove != null) {
            iBinderAsBinder.unlinkToDeath(linkedListenerRemove, 0);
        }
    }

    protected void foreach(ListenerOperation<TListener> listenerOperation) {
        synchronized (this.mListenerMap) {
            foreachUnsafe(listenerOperation);
        }
    }

    protected void setSupported(boolean z) {
        synchronized (this.mListenerMap) {
            this.mHasIsSupported = true;
            this.mIsSupported = z;
        }
    }

    protected void tryUpdateRegistrationWithService() {
        synchronized (this.mListenerMap) {
            if (!isGpsEnabled()) {
                tryUnregister();
            } else {
                if (this.mListenerMap.isEmpty()) {
                    return;
                }
                tryRegister();
            }
        }
    }

    protected void updateResult() {
        synchronized (this.mListenerMap) {
            int iCalculateCurrentResultUnsafe = calculateCurrentResultUnsafe();
            if (this.mLastReportedResult == iCalculateCurrentResultUnsafe) {
                return;
            }
            foreachUnsafe(getHandlerOperation(iCalculateCurrentResultUnsafe));
            this.mLastReportedResult = iCalculateCurrentResultUnsafe;
        }
    }

    private void foreachUnsafe(ListenerOperation<TListener> listenerOperation) {
        Iterator<RemoteListenerHelper<TListener>.LinkedListener> it = this.mListenerMap.values().iterator();
        while (it.hasNext()) {
            post(it.next().getUnderlyingListener(), listenerOperation);
        }
    }

    private void post(TListener tlistener, ListenerOperation<TListener> listenerOperation) {
        if (listenerOperation != null) {
            synchronized (this.mLock) {
                if (this.mInsertedRunnableCount < 200) {
                    this.mHandler.post(new HandlerRunnable(tlistener, listenerOperation));
                    this.mInsertedRunnableCount++;
                } else {
                    if (this.mLogCount % 10 == 0) {
                        Log.d(this.mTag, "Skip post runnable due to size overflow size: " + this.mInsertedRunnableCount);
                        this.mLogCount = 0;
                    }
                    this.mLogCount++;
                }
            }
        }
    }

    class AnonymousClass1 implements Runnable {
        int registrationState = 4;

        AnonymousClass1() {
        }

        @Override
        public void run() {
            if (!RemoteListenerHelper.this.mIsRegistered) {
                this.registrationState = RemoteListenerHelper.this.registerWithService();
                RemoteListenerHelper.this.mIsRegistered = this.registrationState == 0;
            }
            if (!RemoteListenerHelper.this.mIsRegistered) {
                RemoteListenerHelper.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (RemoteListenerHelper.this.mListenerMap) {
                            RemoteListenerHelper.this.foreachUnsafe(RemoteListenerHelper.this.getHandlerOperation(AnonymousClass1.this.registrationState));
                        }
                    }
                });
            }
        }
    }

    private void tryRegister() {
        this.mHandler.post(new AnonymousClass1());
    }

    private void tryUnregister() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!RemoteListenerHelper.this.mIsRegistered) {
                    return;
                }
                RemoteListenerHelper.this.unregisterFromService();
                RemoteListenerHelper.this.mIsRegistered = false;
            }
        });
    }

    private int calculateCurrentResultUnsafe() {
        if (!isAvailableInPlatform()) {
            return 1;
        }
        if (!this.mHasIsSupported || this.mListenerMap.isEmpty()) {
            return 5;
        }
        if (!this.mIsSupported) {
            return 2;
        }
        if (!isGpsEnabled()) {
            return 3;
        }
        return 0;
    }

    private class LinkedListener implements IBinder.DeathRecipient {
        private final TListener mListener;

        public LinkedListener(TListener tlistener) {
            this.mListener = tlistener;
        }

        public TListener getUnderlyingListener() {
            return this.mListener;
        }

        @Override
        public void binderDied() {
            Log.d(RemoteListenerHelper.this.mTag, "Remote Listener died: " + this.mListener);
            RemoteListenerHelper.this.removeListener(this.mListener);
        }
    }

    private class HandlerRunnable implements Runnable {
        private final TListener mListener;
        private final ListenerOperation<TListener> mOperation;

        public HandlerRunnable(TListener tlistener, ListenerOperation<TListener> listenerOperation) {
            this.mListener = tlistener;
            this.mOperation = listenerOperation;
        }

        @Override
        public void run() {
            try {
                synchronized (RemoteListenerHelper.this.mLock) {
                    RemoteListenerHelper.access$610(RemoteListenerHelper.this);
                }
                this.mOperation.execute(this.mListener);
            } catch (RemoteException e) {
                Log.v(RemoteListenerHelper.this.mTag, "Error in monitored listener.", e);
            }
        }
    }
}
