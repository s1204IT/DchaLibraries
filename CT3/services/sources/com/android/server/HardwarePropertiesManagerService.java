package com.android.server;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.CpuUsageInfo;
import android.os.IHardwarePropertiesManager;
import android.os.UserHandle;
import com.android.server.vr.VrManagerInternal;

public class HardwarePropertiesManagerService extends IHardwarePropertiesManager.Stub {
    private final Context mContext;
    private final Object mLock = new Object();

    private static native CpuUsageInfo[] nativeGetCpuUsages();

    private static native float[] nativeGetDeviceTemperatures(int i, int i2);

    private static native float[] nativeGetFanSpeeds();

    private static native void nativeInit();

    public HardwarePropertiesManagerService(Context context) {
        this.mContext = context;
        synchronized (this.mLock) {
            nativeInit();
        }
    }

    public float[] getDeviceTemperatures(String callingPackage, int type, int source) throws SecurityException {
        float[] fArrNativeGetDeviceTemperatures;
        enforceHardwarePropertiesRetrievalAllowed(callingPackage);
        synchronized (this.mLock) {
            fArrNativeGetDeviceTemperatures = nativeGetDeviceTemperatures(type, source);
        }
        return fArrNativeGetDeviceTemperatures;
    }

    public CpuUsageInfo[] getCpuUsages(String callingPackage) throws SecurityException {
        CpuUsageInfo[] cpuUsageInfoArrNativeGetCpuUsages;
        enforceHardwarePropertiesRetrievalAllowed(callingPackage);
        synchronized (this.mLock) {
            cpuUsageInfoArrNativeGetCpuUsages = nativeGetCpuUsages();
        }
        return cpuUsageInfoArrNativeGetCpuUsages;
    }

    public float[] getFanSpeeds(String callingPackage) throws SecurityException {
        float[] fArrNativeGetFanSpeeds;
        enforceHardwarePropertiesRetrievalAllowed(callingPackage);
        synchronized (this.mLock) {
            fArrNativeGetFanSpeeds = nativeGetFanSpeeds();
        }
        return fArrNativeGetFanSpeeds;
    }

    private void enforceHardwarePropertiesRetrievalAllowed(String callingPackage) throws SecurityException {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            int uid = pm.getPackageUid(callingPackage, 0);
            if (Binder.getCallingUid() != uid) {
                throw new SecurityException("The caller has faked the package name.");
            }
            int userId = UserHandle.getUserId(uid);
            VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
            DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
            if (dpm.isDeviceOwnerApp(callingPackage) || dpm.isProfileOwnerApp(callingPackage) || vrService.isCurrentVrListener(callingPackage, userId)) {
            } else {
                throw new SecurityException("The caller is not a device or profile owner or bound VrListenerService.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("The caller has faked the package name.");
        }
    }
}
