package com.mediatek.runningbooster;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

class a {
    private static a A = null;
    private static IPackageManager B = null;
    private static IActivityManager C = null;
    private static Context mContext = null;

    private a(Context context) {
        C = b();
        B = a();
        mContext = context;
        b.init(mContext);
    }

    public static final a a(Context context) {
        if (A == null) {
            A = new a(context);
        }
        return A;
    }

    private static IPackageManager a() {
        IPackageManager iPackageManagerAsInterface = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (iPackageManagerAsInterface == null) {
            throw new RuntimeException("null package manager service");
        }
        return iPackageManagerAsInterface;
    }

    private static IActivityManager b() {
        IActivityManager iActivityManager = ActivityManagerNative.getDefault();
        if (iActivityManager == null) {
            throw new RuntimeException("null activity manager service");
        }
        return iActivityManager;
    }

    private boolean a(PackageInfo packageInfo) {
        if (packageInfo != null && packageInfo.signatures != null) {
            for (Signature signature : packageInfo.signatures) {
                if (b.a(signature)) {
                    Log.d("LicenseController", "Package check C pass");
                    return true;
                }
            }
            Log.e("LicenseController", "Invalid C! ");
            return false;
        }
        Log.e("LicenseController", "Package without C! ");
        return false;
    }

    private boolean a(int i) {
        try {
            int userId = UserHandle.getUserId(i);
            String[] packagesForUid = B.getPackagesForUid(i);
            if (packagesForUid == null) {
                Log.e("LicenseController", "getPackagesForUid() with null packages! ");
                return false;
            }
            for (String str : packagesForUid) {
                if (!a(B.getPackageInfo(str, 4160, userId))) {
                    return false;
                }
            }
            Log.d("LicenseController", "checkProtocol(" + i + ") passed!");
            return true;
        } catch (RemoteException e) {
            Log.e("LicenseController", "get PackagesInfo failed! ", e);
            return false;
        }
    }

    private boolean c() {
        return a(Binder.getCallingUid());
    }

    public void f(String str) {
        if (!c()) {
            throw new SecurityException("Use API without valid license: " + str + " uid: " + Binder.getCallingUid() + " pid: " + Binder.getCallingPid());
        }
    }
}
