package com.android.server;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Binder;
import android.os.CpuUsageInfo;
import android.os.IHardwarePropertiesManager;
import android.os.UserHandle;
import com.android.internal.util.DumpUtils;
import com.android.server.UiModeManagerService;
import com.android.server.vr.VrManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class HardwarePropertiesManagerService extends IHardwarePropertiesManager.Stub {
    private static final String TAG = "HardwarePropertiesManagerService";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final Object mLock = new Object();

    private static native CpuUsageInfo[] nativeGetCpuUsages();

    private static native float[] nativeGetDeviceTemperatures(int i, int i2);

    private static native float[] nativeGetFanSpeeds();

    private static native void nativeInit();

    public HardwarePropertiesManagerService(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        synchronized (this.mLock) {
            nativeInit();
        }
    }

    public float[] getDeviceTemperatures(String str, int i, int i2) throws SecurityException {
        float[] fArrNativeGetDeviceTemperatures;
        enforceHardwarePropertiesRetrievalAllowed(str);
        synchronized (this.mLock) {
            fArrNativeGetDeviceTemperatures = nativeGetDeviceTemperatures(i, i2);
        }
        return fArrNativeGetDeviceTemperatures;
    }

    public CpuUsageInfo[] getCpuUsages(String str) throws SecurityException {
        CpuUsageInfo[] cpuUsageInfoArrNativeGetCpuUsages;
        enforceHardwarePropertiesRetrievalAllowed(str);
        synchronized (this.mLock) {
            cpuUsageInfoArrNativeGetCpuUsages = nativeGetCpuUsages();
        }
        return cpuUsageInfoArrNativeGetCpuUsages;
    }

    public float[] getFanSpeeds(String str) throws SecurityException {
        float[] fArrNativeGetFanSpeeds;
        enforceHardwarePropertiesRetrievalAllowed(str);
        synchronized (this.mLock) {
            fArrNativeGetFanSpeeds = nativeGetFanSpeeds();
        }
        return fArrNativeGetFanSpeeds;
    }

    private String getCallingPackageName() {
        String[] packagesForUid = this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid != null && packagesForUid.length > 0) {
            return packagesForUid[0];
        }
        return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
    }

    private void dumpTempValues(String str, PrintWriter printWriter, int i, String str2) {
        dumpTempValues(str, printWriter, i, str2, "temperatures: ", 0);
        dumpTempValues(str, printWriter, i, str2, "throttling temperatures: ", 1);
        dumpTempValues(str, printWriter, i, str2, "shutdown temperatures: ", 2);
        dumpTempValues(str, printWriter, i, str2, "vr throttling temperatures: ", 3);
    }

    private void dumpTempValues(String str, PrintWriter printWriter, int i, String str2, String str3, int i2) {
        printWriter.println(str2 + str3 + Arrays.toString(getDeviceTemperatures(str, i, i2)));
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("****** Dump of HardwarePropertiesManagerService ******");
            String callingPackageName = getCallingPackageName();
            dumpTempValues(callingPackageName, printWriter, 0, "CPU ");
            dumpTempValues(callingPackageName, printWriter, 1, "GPU ");
            dumpTempValues(callingPackageName, printWriter, 2, "Battery ");
            dumpTempValues(callingPackageName, printWriter, 3, "Skin ");
            printWriter.println("Fan speed: " + Arrays.toString(getFanSpeeds(callingPackageName)) + "\n");
            CpuUsageInfo[] cpuUsages = getCpuUsages(callingPackageName);
            for (int i = 0; i < cpuUsages.length; i++) {
                printWriter.println("Cpu usage of core: " + i + ", active = " + cpuUsages[i].getActive() + ", total = " + cpuUsages[i].getTotal());
            }
            printWriter.println("****** End of HardwarePropertiesManagerService dump ******");
        }
    }

    private void enforceHardwarePropertiesRetrievalAllowed(String str) throws SecurityException {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        VrManagerInternal vrManagerInternal = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (!((DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class)).isDeviceOwnerApp(str) && !vrManagerInternal.isCurrentVrListener(str, userId) && this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("The caller is not a device owner, bound VrListenerService, or holding the DEVICE_POWER permission.");
        }
    }
}
