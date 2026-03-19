package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IUpdateLock;
import android.os.RemoteException;
import android.os.TokenWatcher;
import android.os.UserHandle;
import com.android.internal.util.DumpUtils;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class UpdateLockService extends IUpdateLock.Stub {
    static final boolean DEBUG = false;
    static final String PERMISSION = "android.permission.UPDATE_LOCK";
    static final String TAG = "UpdateLockService";
    Context mContext;
    LockWatcher mLocks = new LockWatcher(new Handler(), "UpdateLocks");

    class LockWatcher extends TokenWatcher {
        LockWatcher(Handler handler, String str) {
            super(handler, str);
        }

        @Override
        public void acquired() {
            UpdateLockService.this.sendLockChangedBroadcast(false);
        }

        @Override
        public void released() {
            UpdateLockService.this.sendLockChangedBroadcast(true);
        }
    }

    UpdateLockService(Context context) {
        this.mContext = context;
        sendLockChangedBroadcast(true);
    }

    void sendLockChangedBroadcast(boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendStickyBroadcastAsUser(new Intent("android.os.UpdateLock.UPDATE_LOCK_CHANGED").putExtra("nowisconvenient", z).putExtra(WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP, System.currentTimeMillis()).addFlags(67108864), UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void acquireUpdateLock(IBinder iBinder, String str) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "acquireUpdateLock");
        this.mLocks.acquire(iBinder, makeTag(str));
    }

    public void releaseUpdateLock(IBinder iBinder) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "releaseUpdateLock");
        this.mLocks.release(iBinder);
    }

    private String makeTag(String str) {
        return "{tag=" + str + " uid=" + Binder.getCallingUid() + " pid=" + Binder.getCallingPid() + '}';
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            this.mLocks.dump(printWriter);
        }
    }
}
