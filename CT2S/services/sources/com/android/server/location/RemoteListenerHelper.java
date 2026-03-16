package com.android.server.location;

import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.HashMap;

abstract class RemoteListenerHelper<TListener extends IInterface> {
    protected static final int RESULT_GPS_LOCATION_DISABLED = 3;
    protected static final int RESULT_INTERNAL_ERROR = 4;
    protected static final int RESULT_NOT_AVAILABLE = 1;
    protected static final int RESULT_NOT_SUPPORTED = 2;
    protected static final int RESULT_SUCCESS = 0;
    private final Handler mHandler;
    private boolean mHasIsSupported;
    private boolean mIsRegistered;
    private boolean mIsSupported;
    private final HashMap<IBinder, RemoteListenerHelper<TListener>.LinkedListener> mListenerMap = new HashMap<>();
    private final String mTag;

    protected interface ListenerOperation<TListener extends IInterface> {
        void execute(TListener tlistener) throws RemoteException;
    }

    protected abstract ListenerOperation<TListener> getHandlerOperation(int i);

    protected abstract void handleGpsEnabledChanged(boolean z);

    protected abstract boolean isAvailableInPlatform();

    protected abstract boolean isGpsEnabled();

    protected abstract boolean registerWithService();

    protected abstract void unregisterFromService();

    protected RemoteListenerHelper(Handler handler, String name) {
        Preconditions.checkNotNull(name);
        this.mHandler = handler;
        this.mTag = name;
    }

    public boolean addListener(TListener listener) {
        int result;
        Preconditions.checkNotNull(listener, "Attempted to register a 'null' listener.");
        IBinder binder = listener.asBinder();
        RemoteListenerHelper<TListener>.LinkedListener linkedListener = new LinkedListener(listener);
        synchronized (this.mListenerMap) {
            if (this.mListenerMap.containsKey(binder)) {
                return true;
            }
            try {
                binder.linkToDeath(linkedListener, 0);
                this.mListenerMap.put(binder, linkedListener);
                if (!isAvailableInPlatform()) {
                    result = 1;
                } else if (this.mHasIsSupported && !this.mIsSupported) {
                    result = 2;
                } else if (!isGpsEnabled()) {
                    result = 3;
                } else if (!tryRegister()) {
                    result = 4;
                } else {
                    if (!this.mHasIsSupported || !this.mIsSupported) {
                        return true;
                    }
                    result = 0;
                }
                post(listener, getHandlerOperation(result));
                return true;
            } catch (RemoteException e) {
                Log.v(this.mTag, "Remote listener already died.", e);
                return false;
            }
        }
    }

    public void removeListener(TListener listener) {
        RemoteListenerHelper<TListener>.LinkedListener linkedListener;
        Preconditions.checkNotNull(listener, "Attempted to remove a 'null' listener.");
        IBinder binder = listener.asBinder();
        synchronized (this.mListenerMap) {
            linkedListener = this.mListenerMap.remove(binder);
            if (this.mListenerMap.isEmpty()) {
                tryUnregister();
            }
        }
        if (linkedListener != null) {
            binder.unlinkToDeath(linkedListener, 0);
        }
    }

    public void onGpsEnabledChanged(boolean enabled) {
        handleGpsEnabledChanged(enabled);
        synchronized (this.mListenerMap) {
            if (!enabled) {
                tryUnregister();
            } else if (!this.mListenerMap.isEmpty()) {
                if (!tryRegister()) {
                    ListenerOperation<TListener> operation = getHandlerOperation(4);
                    foreachUnsafe(operation);
                }
            }
        }
    }

    protected void foreach(ListenerOperation<TListener> operation) {
        synchronized (this.mListenerMap) {
            foreachUnsafe(operation);
        }
    }

    protected void setSupported(boolean value, ListenerOperation<TListener> notifier) {
        synchronized (this.mListenerMap) {
            this.mHasIsSupported = true;
            this.mIsSupported = value;
            foreachUnsafe(notifier);
        }
    }

    private void foreachUnsafe(ListenerOperation<TListener> operation) {
        for (RemoteListenerHelper<TListener>.LinkedListener linkedListener : this.mListenerMap.values()) {
            post(linkedListener.getUnderlyingListener(), operation);
        }
    }

    private void post(TListener listener, ListenerOperation<TListener> operation) {
        if (operation != null) {
            this.mHandler.post(new HandlerRunnable(listener, operation));
        }
    }

    private boolean tryRegister() {
        if (!this.mIsRegistered) {
            this.mIsRegistered = registerWithService();
        }
        return this.mIsRegistered;
    }

    private void tryUnregister() {
        if (this.mIsRegistered) {
            unregisterFromService();
            this.mIsRegistered = false;
        }
    }

    private class LinkedListener implements IBinder.DeathRecipient {
        private final TListener mListener;

        public LinkedListener(TListener listener) {
            this.mListener = listener;
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

        public HandlerRunnable(TListener listener, ListenerOperation<TListener> operation) {
            this.mListener = listener;
            this.mOperation = operation;
        }

        @Override
        public void run() {
            try {
                this.mOperation.execute(this.mListener);
            } catch (RemoteException e) {
                Log.v(RemoteListenerHelper.this.mTag, "Error in monitored listener.", e);
            }
        }
    }
}
