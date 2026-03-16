package android.location;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

abstract class LocalListenerHelper<TListener> {
    private final Context mContext;
    private final HashSet<TListener> mListeners = new HashSet<>();
    private final String mTag;

    protected interface ListenerOperation<TListener> {
        void execute(TListener tlistener) throws RemoteException;
    }

    protected abstract boolean registerWithServer() throws RemoteException;

    protected abstract void unregisterFromServer() throws RemoteException;

    protected LocalListenerHelper(Context context, String name) {
        Preconditions.checkNotNull(name);
        this.mContext = context;
        this.mTag = name;
    }

    public boolean add(TListener listener) {
        boolean zAdd = false;
        Preconditions.checkNotNull(listener);
        synchronized (this.mListeners) {
            if (this.mListeners.isEmpty()) {
                try {
                    boolean registeredWithService = registerWithServer();
                    if (!registeredWithService) {
                        Log.e(this.mTag, "Unable to register listener transport.");
                    } else if (this.mListeners.contains(listener)) {
                        zAdd = true;
                    } else {
                        zAdd = this.mListeners.add(listener);
                    }
                } catch (RemoteException e) {
                    Log.e(this.mTag, "Error handling first listener.", e);
                }
            }
        }
        return zAdd;
    }

    public void remove(TListener listener) {
        Preconditions.checkNotNull(listener);
        synchronized (this.mListeners) {
            boolean removed = this.mListeners.remove(listener);
            boolean isLastRemoved = removed && this.mListeners.isEmpty();
            if (isLastRemoved) {
                try {
                    unregisterFromServer();
                } catch (RemoteException e) {
                    Log.v(this.mTag, "Error handling last listener removal", e);
                }
            }
        }
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected void foreach(ListenerOperation<TListener> listenerOperation) {
        Collection<TListener> listeners;
        synchronized (this.mListeners) {
            listeners = new ArrayList<>(this.mListeners);
        }
        for (TListener listener : listeners) {
            try {
                listenerOperation.execute(listener);
            } catch (RemoteException e) {
                Log.e(this.mTag, "Error in monitored listener.", e);
            }
        }
    }
}
