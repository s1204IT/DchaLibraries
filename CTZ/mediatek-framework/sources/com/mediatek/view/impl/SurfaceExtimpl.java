package com.mediatek.view.impl;

import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.view.SurfaceExt;

public class SurfaceExtimpl extends SurfaceExt {
    private static final String TAG = "SurfaceExt";
    private static final boolean ENABLE_WHITE_LIST = SystemProperties.getBoolean("debug.enable.whitelist", false);
    private static final String[] WHITE_LIST = {"com.tencent.qqlive"};

    public boolean isInWhiteList() {
        if (ENABLE_WHITE_LIST) {
            return true;
        }
        String callerProcessName = getCallerProcessName();
        if (WHITE_LIST != null && callerProcessName != null) {
            for (String str : WHITE_LIST) {
                if (str.equals(callerProcessName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getCallerProcessName() {
        int callingUid = Binder.getCallingUid();
        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (iPackageManagerAsInterface != null) {
            try {
                return iPackageManagerAsInterface.getNameForUid(callingUid);
            } catch (RemoteException e) {
                Log.e(TAG, "getCallerProcessName exception :" + e);
                return null;
            }
        }
        return null;
    }
}
