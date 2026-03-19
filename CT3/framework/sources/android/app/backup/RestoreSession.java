package android.app.backup;

import android.app.backup.IRestoreObserver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class RestoreSession {
    static final String TAG = "RestoreSession";
    IRestoreSession mBinder;
    final Context mContext;
    RestoreObserverWrapper mObserver = null;

    public int getAvailableRestoreSets(RestoreObserver observer) {
        RestoreObserverWrapper obsWrapper = new RestoreObserverWrapper(this.mContext, observer);
        try {
            int err = this.mBinder.getAvailableRestoreSets(obsWrapper);
            return err;
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to get available sets");
            return -1;
        }
    }

    public int restoreAll(long token, RestoreObserver observer) {
        if (this.mObserver != null) {
            Log.d(TAG, "restoreAll() called during active restore");
            return -1;
        }
        this.mObserver = new RestoreObserverWrapper(this.mContext, observer);
        try {
            int err = this.mBinder.restoreAll(token, this.mObserver);
            return err;
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to restore");
            return -1;
        }
    }

    public int restoreSome(long token, RestoreObserver observer, String[] packages) {
        if (this.mObserver != null) {
            Log.d(TAG, "restoreAll() called during active restore");
            return -1;
        }
        this.mObserver = new RestoreObserverWrapper(this.mContext, observer);
        try {
            int err = this.mBinder.restoreSome(token, this.mObserver, packages);
            return err;
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to restore packages");
            return -1;
        }
    }

    public int restorePackage(String packageName, RestoreObserver observer) {
        if (this.mObserver != null) {
            Log.d(TAG, "restorePackage() called during active restore");
            return -1;
        }
        this.mObserver = new RestoreObserverWrapper(this.mContext, observer);
        try {
            int err = this.mBinder.restorePackage(packageName, this.mObserver);
            return err;
        } catch (RemoteException e) {
            Log.d(TAG, "Can't contact server to restore package");
            return -1;
        }
    }

    public void endRestoreSession() {
        try {
            try {
                this.mBinder.endRestoreSession();
            } catch (RemoteException e) {
                Log.d(TAG, "Can't contact server to get available sets");
            }
        } finally {
            this.mBinder = null;
        }
    }

    RestoreSession(Context context, IRestoreSession binder) {
        this.mContext = context;
        this.mBinder = binder;
    }

    private class RestoreObserverWrapper extends IRestoreObserver.Stub {
        static final int MSG_RESTORE_FINISHED = 3;
        static final int MSG_RESTORE_SETS_AVAILABLE = 4;
        static final int MSG_RESTORE_STARTING = 1;
        static final int MSG_UPDATE = 2;
        final RestoreObserver mAppObserver;
        final Handler mHandler;

        RestoreObserverWrapper(Context context, RestoreObserver appObserver) {
            this.mHandler = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            RestoreObserverWrapper.this.mAppObserver.restoreStarting(msg.arg1);
                            break;
                        case 2:
                            RestoreObserverWrapper.this.mAppObserver.onUpdate(msg.arg1, (String) msg.obj);
                            break;
                        case 3:
                            RestoreObserverWrapper.this.mAppObserver.restoreFinished(msg.arg1);
                            break;
                        case 4:
                            RestoreObserverWrapper.this.mAppObserver.restoreSetsAvailable((RestoreSet[]) msg.obj);
                            break;
                    }
                }
            };
            this.mAppObserver = appObserver;
        }

        @Override
        public void restoreSetsAvailable(RestoreSet[] result) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(4, result));
        }

        @Override
        public void restoreStarting(int numPackages) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, numPackages, 0));
        }

        @Override
        public void onUpdate(int nowBeingRestored, String currentPackage) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, nowBeingRestored, 0, currentPackage));
        }

        @Override
        public void restoreFinished(int error) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3, error, 0));
        }
    }
}
