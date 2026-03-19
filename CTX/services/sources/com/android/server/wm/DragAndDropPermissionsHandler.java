package com.android.server.wm;

import android.app.ActivityManager;
import android.content.ClipData;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.internal.view.IDragAndDropPermissions;
import java.util.ArrayList;

class DragAndDropPermissionsHandler extends IDragAndDropPermissions.Stub implements IBinder.DeathRecipient {
    private final int mMode;
    private final int mSourceUid;
    private final int mSourceUserId;
    private final String mTargetPackage;
    private final int mTargetUserId;
    private final ArrayList<Uri> mUris = new ArrayList<>();
    private IBinder mActivityToken = null;
    private IBinder mPermissionOwnerToken = null;
    private IBinder mTransientToken = null;

    DragAndDropPermissionsHandler(ClipData clipData, int i, String str, int i2, int i3, int i4) {
        this.mSourceUid = i;
        this.mTargetPackage = str;
        this.mMode = i2;
        this.mSourceUserId = i3;
        this.mTargetUserId = i4;
        clipData.collectUris(this.mUris);
    }

    public void take(IBinder iBinder) throws RemoteException {
        if (this.mActivityToken != null || this.mPermissionOwnerToken != null) {
            return;
        }
        this.mActivityToken = iBinder;
        doTake(ActivityManager.getService().getUriPermissionOwnerForActivity(this.mActivityToken));
    }

    private void doTake(IBinder iBinder) throws RemoteException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        for (int i = 0; i < this.mUris.size(); i++) {
            try {
                ActivityManager.getService().grantUriPermissionFromOwner(iBinder, this.mSourceUid, this.mTargetPackage, this.mUris.get(i), this.mMode, this.mSourceUserId, this.mTargetUserId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void takeTransient(IBinder iBinder) throws RemoteException {
        if (this.mActivityToken != null || this.mPermissionOwnerToken != null) {
            return;
        }
        this.mPermissionOwnerToken = ActivityManager.getService().newUriPermissionOwner("drop");
        this.mTransientToken = iBinder;
        this.mTransientToken.linkToDeath(this, 0);
        doTake(this.mPermissionOwnerToken);
    }

    public void release() throws RemoteException {
        IBinder uriPermissionOwnerForActivity;
        if (this.mActivityToken == null && this.mPermissionOwnerToken == null) {
            return;
        }
        if (this.mActivityToken != null) {
            try {
                uriPermissionOwnerForActivity = ActivityManager.getService().getUriPermissionOwnerForActivity(this.mActivityToken);
            } catch (Exception e) {
                return;
            } finally {
                this.mActivityToken = null;
            }
        } else {
            uriPermissionOwnerForActivity = this.mPermissionOwnerToken;
            this.mPermissionOwnerToken = null;
            this.mTransientToken.unlinkToDeath(this, 0);
            this.mTransientToken = null;
        }
        for (int i = 0; i < this.mUris.size(); i++) {
            ActivityManager.getService().revokeUriPermissionFromOwner(uriPermissionOwnerForActivity, this.mUris.get(i), this.mMode, this.mSourceUserId);
        }
    }

    @Override
    public void binderDied() {
        try {
            release();
        } catch (RemoteException e) {
        }
    }
}
