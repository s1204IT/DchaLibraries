package com.android.commands.monkey;

import android.app.IActivityManager;
import android.content.pm.IPackageManager;
import android.content.pm.PackageItemInfo;
import android.content.pm.PermissionInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.view.IWindowManager;

public class MonkeyPermissionEvent extends MonkeyEvent {
    private PermissionInfo mPermissionInfo;
    private String mPkg;

    public MonkeyPermissionEvent(String str, PermissionInfo permissionInfo) {
        super(7);
        this.mPkg = str;
        this.mPermissionInfo = permissionInfo;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        try {
            boolean z = iPackageManagerAsInterface.checkPermission(((PackageItemInfo) this.mPermissionInfo).name, this.mPkg, UserHandle.myUserId()) == -1;
            Logger logger = Logger.out;
            Object[] objArr = new Object[3];
            objArr[0] = z ? "grant" : "revoke";
            objArr[1] = ((PackageItemInfo) this.mPermissionInfo).name;
            objArr[2] = this.mPkg;
            logger.println(String.format(":Permission %s %s to package %s", objArr));
            if (z) {
                iPackageManagerAsInterface.grantRuntimePermission(this.mPkg, ((PackageItemInfo) this.mPermissionInfo).name, UserHandle.myUserId());
            } else {
                iPackageManagerAsInterface.revokeRuntimePermission(this.mPkg, ((PackageItemInfo) this.mPermissionInfo).name, UserHandle.myUserId());
            }
            return 1;
        } catch (RemoteException e) {
            return -1;
        }
    }
}
