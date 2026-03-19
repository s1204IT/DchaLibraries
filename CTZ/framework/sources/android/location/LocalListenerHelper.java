package android.location;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

abstract class LocalListenerHelper<TListener> {
    private final Context mContext;
    private final HashMap<TListener, Handler> mListeners = new HashMap<>();
    private final String mTag;

    protected interface ListenerOperation<TListener> {
        void execute(TListener tlistener) throws RemoteException;
    }

    protected abstract boolean registerWithServer() throws RemoteException;

    protected abstract void unregisterFromServer() throws RemoteException;

    protected LocalListenerHelper(Context context, String str) {
        Preconditions.checkNotNull(str);
        this.mContext = context;
        this.mTag = str;
    }

    public boolean add(TListener tlistener, Handler handler) {
        Preconditions.checkNotNull(tlistener);
        synchronized (this.mListeners) {
            if (this.mListeners.isEmpty()) {
                try {
                    if (!registerWithServer()) {
                        Log.e(this.mTag, "Unable to register listener transport.");
                        return false;
                    }
                } catch (RemoteException e) {
                    Log.e(this.mTag, "Error handling first listener.", e);
                    return false;
                }
            }
            if (this.mListeners.containsKey(tlistener)) {
                return true;
            }
            this.mListeners.put(tlistener, handler);
            return true;
        }
    }

    public void remove(TListener tlistener) {
        Preconditions.checkNotNull(tlistener);
        synchronized (this.mListeners) {
            boolean zContainsKey = this.mListeners.containsKey(tlistener);
            this.mListeners.remove(tlistener);
            if (zContainsKey && this.mListeners.isEmpty()) {
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

    private void executeOperation(ListenerOperation<TListener> listenerOperation, TListener tlistener) {
        try {
            listenerOperation.execute(tlistener);
        } catch (RemoteException e) {
            Log.e(this.mTag, "Error in monitored listener.", e);
        }
    }

    protected void foreach(final ListenerOperation<TListener> listenerOperation) {
        ArrayList<Map.Entry> arrayList;
        synchronized (this.mListeners) {
            arrayList = new ArrayList(this.mListeners.entrySet());
        }
        for (final Map.Entry entry : arrayList) {
            if (entry.getValue() == null) {
                executeOperation(listenerOperation, entry.getKey());
            } else {
                ((Handler) entry.getValue()).post(new Runnable() {
                    @Override
                    public void run() {
                        LocalListenerHelper.this.executeOperation(listenerOperation, entry.getKey());
                    }
                });
            }
        }
    }
}
