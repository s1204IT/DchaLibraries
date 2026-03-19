package com.android.deskclock.alarms;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import com.android.deskclock.LogUtils;

public class PowerOffAlarm {
    private static IStorageManager getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IStorageManager.Stub.asInterface(service);
        }
        return null;
    }

    public static int getPasswordType() {
        try {
            return getMountService().getPasswordType();
        } catch (RemoteException e) {
            LogUtils.e("Error getPasswordType " + e, new Object[0]);
            return 0;
        }
    }

    static boolean deviceUnencrypted() {
        LogUtils.i("DeviceUnencrypted State = " + SystemProperties.get("ro.crypto.state"), new Object[0]);
        LogUtils.i("deviceUnencrypted Type = " + SystemProperties.get("ro.crypto.type"), new Object[0]);
        return ("encrypted".equals(SystemProperties.get("ro.crypto.state")) && "block".equals(SystemProperties.get("ro.crypto.type"))) ? false : true;
    }

    public static boolean canEnablePowerOffAlarm() {
        boolean z = true;
        if (UserHandle.myUserId() != 0 || (!deviceUnencrypted() && 1 != getPasswordType())) {
            z = false;
        }
        LogUtils.v("Power Off Alarm enabled: " + z, new Object[0]);
        return z;
    }
}
