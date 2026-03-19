package com.android.server.wm;

import android.app.ActivityManagerNative;
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

    DragAndDropPermissionsHandler(ClipData clipData, int sourceUid, String targetPackage, int mode, int sourceUserId, int targetUserId) {
        this.mSourceUid = sourceUid;
        this.mTargetPackage = targetPackage;
        this.mMode = mode;
        this.mSourceUserId = sourceUserId;
        this.mTargetUserId = targetUserId;
        clipData.collectUris(this.mUris);
    }

    public void take(IBinder activityToken) throws RemoteException {
        if (this.mActivityToken != null || this.mPermissionOwnerToken != null) {
            return;
        }
        this.mActivityToken = activityToken;
        IBinder permissionOwner = ActivityManagerNative.getDefault().getUriPermissionOwnerForActivity(this.mActivityToken);
        doTake(permissionOwner);
    }

    private void doTake(IBinder permissionOwner) throws RemoteException {
        long origId = Binder.clearCallingIdentity();
        for (int i = 0; i < this.mUris.size(); i++) {
            try {
                ActivityManagerNative.getDefault().grantUriPermissionFromOwner(permissionOwner, this.mSourceUid, this.mTargetPackage, this.mUris.get(i), this.mMode, this.mSourceUserId, this.mTargetUserId);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    public void takeTransient(IBinder permissionOwnerToken) throws RemoteException {
        if (this.mActivityToken != null || this.mPermissionOwnerToken != null) {
            return;
        }
        this.mPermissionOwnerToken = permissionOwnerToken;
        this.mPermissionOwnerToken.linkToDeath(this, 0);
        doTake(this.mPermissionOwnerToken);
    }

    public void release() throws RemoteException {
        IBinder permissionOwner;
        if (this.mActivityToken == null && this.mPermissionOwnerToken == null) {
            return;
        }
        if (this.mActivityToken != null) {
            try {
                permissionOwner = ActivityManagerNative.getDefault().getUriPermissionOwnerForActivity(this.mActivityToken);
            } catch (Exception e) {
                return;
            } finally {
                this.mActivityToken = null;
            }
        } else {
            permissionOwner = this.mPermissionOwnerToken;
            this.mPermissionOwnerToken.unlinkToDeath(this, 0);
            this.mPermissionOwnerToken = null;
        }
        for (int i = 0; i < this.mUris.size(); i++) {
            ActivityManagerNative.getDefault().revokeUriPermissionFromOwner(permissionOwner, this.mUris.get(i), this.mMode, this.mSourceUserId);
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
