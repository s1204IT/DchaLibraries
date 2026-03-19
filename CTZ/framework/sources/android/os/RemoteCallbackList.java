package android.os;

import android.os.IBinder;
import android.os.IInterface;
import android.util.ArrayMap;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class RemoteCallbackList<E extends IInterface> {
    private static final String TAG = "RemoteCallbackList";
    private Object[] mActiveBroadcast;
    private StringBuilder mRecentCallers;
    ArrayMap<IBinder, RemoteCallbackList<E>.Callback> mCallbacks = new ArrayMap<>();
    private int mBroadcastCount = -1;
    private boolean mKilled = false;

    private final class Callback implements IBinder.DeathRecipient {
        final E mCallback;
        final Object mCookie;

        Callback(E e, Object obj) {
            this.mCallback = e;
            this.mCookie = obj;
        }

        @Override
        public void binderDied() {
            synchronized (RemoteCallbackList.this.mCallbacks) {
                RemoteCallbackList.this.mCallbacks.remove(this.mCallback.asBinder());
            }
            RemoteCallbackList.this.onCallbackDied(this.mCallback, this.mCookie);
        }
    }

    public boolean register(E e) {
        return register(e, null);
    }

    public boolean register(E e, Object obj) {
        synchronized (this.mCallbacks) {
            if (this.mKilled) {
                return false;
            }
            logExcessiveCallbacks();
            IBinder iBinderAsBinder = e.asBinder();
            try {
                RemoteCallbackList<E>.Callback callback = new Callback(e, obj);
                iBinderAsBinder.linkToDeath(callback, 0);
                this.mCallbacks.put(iBinderAsBinder, callback);
                return true;
            } catch (RemoteException e2) {
                return false;
            }
        }
    }

    public boolean unregister(E e) {
        synchronized (this.mCallbacks) {
            RemoteCallbackList<E>.Callback callbackRemove = this.mCallbacks.remove(e.asBinder());
            if (callbackRemove != null) {
                callbackRemove.mCallback.asBinder().unlinkToDeath(callbackRemove, 0);
                return true;
            }
            return false;
        }
    }

    public void kill() {
        synchronized (this.mCallbacks) {
            for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
                RemoteCallbackList<E>.Callback callbackValueAt = this.mCallbacks.valueAt(size);
                callbackValueAt.mCallback.asBinder().unlinkToDeath(callbackValueAt, 0);
            }
            this.mCallbacks.clear();
            this.mKilled = true;
        }
    }

    public void onCallbackDied(E e) {
    }

    public void onCallbackDied(E e, Object obj) {
        onCallbackDied(e);
    }

    public int beginBroadcast() {
        synchronized (this.mCallbacks) {
            if (this.mBroadcastCount > 0) {
                throw new IllegalStateException("beginBroadcast() called while already in a broadcast");
            }
            int size = this.mCallbacks.size();
            this.mBroadcastCount = size;
            if (size <= 0) {
                return 0;
            }
            Object[] objArr = this.mActiveBroadcast;
            if (objArr == null || objArr.length < size) {
                objArr = new Object[size];
                this.mActiveBroadcast = objArr;
            }
            for (int i = 0; i < size; i++) {
                objArr[i] = this.mCallbacks.valueAt(i);
            }
            return size;
        }
    }

    public E getBroadcastItem(int i) {
        return ((Callback) this.mActiveBroadcast[i]).mCallback;
    }

    public Object getBroadcastCookie(int i) {
        return ((Callback) this.mActiveBroadcast[i]).mCookie;
    }

    public void finishBroadcast() {
        synchronized (this.mCallbacks) {
            if (this.mBroadcastCount < 0) {
                throw new IllegalStateException("finishBroadcast() called outside of a broadcast");
            }
            Object[] objArr = this.mActiveBroadcast;
            if (objArr != null) {
                int i = this.mBroadcastCount;
                for (int i2 = 0; i2 < i; i2++) {
                    objArr[i2] = null;
                }
            }
            this.mBroadcastCount = -1;
        }
    }

    public void broadcast(Consumer<E> consumer) {
        int iBeginBroadcast = beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                consumer.accept(getBroadcastItem(i));
            } finally {
                finishBroadcast();
            }
        }
    }

    public <C> void broadcastForEachCookie(Consumer<C> consumer) {
        int iBeginBroadcast = beginBroadcast();
        for (int i = 0; i < iBeginBroadcast; i++) {
            try {
                consumer.accept(getBroadcastCookie(i));
            } finally {
                finishBroadcast();
            }
        }
    }

    public int getRegisteredCallbackCount() {
        synchronized (this.mCallbacks) {
            if (this.mKilled) {
                return 0;
            }
            return this.mCallbacks.size();
        }
    }

    public E getRegisteredCallbackItem(int i) {
        synchronized (this.mCallbacks) {
            if (this.mKilled) {
                return null;
            }
            return this.mCallbacks.valueAt(i).mCallback;
        }
    }

    public Object getRegisteredCallbackCookie(int i) {
        synchronized (this.mCallbacks) {
            if (this.mKilled) {
                return null;
            }
            return this.mCallbacks.valueAt(i).mCookie;
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("callbacks: ");
        printWriter.println(this.mCallbacks.size());
        printWriter.print(str);
        printWriter.print("killed: ");
        printWriter.println(this.mKilled);
        printWriter.print(str);
        printWriter.print("broadcasts count: ");
        printWriter.println(this.mBroadcastCount);
    }

    private void logExcessiveCallbacks() {
        long size = this.mCallbacks.size();
        if (size >= 3000) {
            if (size == 3000 && this.mRecentCallers == null) {
                this.mRecentCallers = new StringBuilder();
            }
            if (this.mRecentCallers != null && this.mRecentCallers.length() < 1000) {
                this.mRecentCallers.append(Debug.getCallers(5));
                this.mRecentCallers.append('\n');
                if (this.mRecentCallers.length() >= 1000) {
                    Slog.wtf(TAG, "More than 3000 remote callbacks registered. Recent callers:\n" + this.mRecentCallers.toString());
                    this.mRecentCallers = null;
                }
            }
        }
    }
}
