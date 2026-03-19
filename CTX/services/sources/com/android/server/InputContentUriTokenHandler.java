package com.android.server;

import android.app.ActivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.inputmethod.IInputContentUriToken;

final class InputContentUriTokenHandler extends IInputContentUriToken.Stub {
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private IBinder mPermissionOwnerToken = null;
    private final int mSourceUid;
    private final int mSourceUserId;
    private final String mTargetPackage;
    private final int mTargetUserId;
    private final Uri mUri;

    InputContentUriTokenHandler(Uri uri, int i, String str, int i2, int i3) {
        this.mUri = uri;
        this.mSourceUid = i;
        this.mTargetPackage = str;
        this.mSourceUserId = i2;
        this.mTargetUserId = i3;
    }

    public void take() {
        synchronized (this.mLock) {
            if (this.mPermissionOwnerToken != null) {
                return;
            }
            try {
                this.mPermissionOwnerToken = ActivityManager.getService().newUriPermissionOwner("InputContentUriTokenHandler");
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
            doTakeLocked(this.mPermissionOwnerToken);
        }
    }

    private void doTakeLocked(IBinder iBinder) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                ActivityManager.getService().grantUriPermissionFromOwner(iBinder, this.mSourceUid, this.mTargetPackage, this.mUri, 1, this.mSourceUserId, this.mTargetUserId);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void release() {
        synchronized (this.mLock) {
            if (this.mPermissionOwnerToken == null) {
                return;
            }
            try {
                try {
                    ActivityManager.getService().revokeUriPermissionFromOwner(this.mPermissionOwnerToken, this.mUri, 1, this.mSourceUserId);
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
            } finally {
                this.mPermissionOwnerToken = null;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            release();
        } finally {
            super/*java.lang.Object*/.finalize();
        }
    }
}
