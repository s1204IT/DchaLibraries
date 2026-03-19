package com.android.commands.svc;

import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import com.android.commands.svc.Svc;

public class PowerCommand extends Svc.Command {
    public PowerCommand() {
        super("power");
    }

    @Override
    public String shortHelp() {
        return "Control the power manager";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n\nusage: svc power stayon [true|false|usb|ac|wireless]\n         Set the 'keep awake while plugged in' setting.\n       svc power reboot [reason]\n         Perform a runtime shutdown and reboot device with specified reason.\n       svc power shutdown\n         Perform a runtime shutdown and power off the device.\n";
    }

    @Override
    public void run(String[] strArr) {
        int i = 2;
        if (strArr.length >= 2) {
            IPowerManager iPowerManagerAsInterface = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            if ("stayon".equals(strArr[1]) && strArr.length == 3) {
                if ("true".equals(strArr[2])) {
                    i = 7;
                } else if (!"false".equals(strArr[2])) {
                    if (!"usb".equals(strArr[2])) {
                        if (!"ac".equals(strArr[2])) {
                            if ("wireless".equals(strArr[2])) {
                                i = 4;
                            }
                        } else {
                            i = 1;
                        }
                    }
                } else {
                    i = 0;
                }
                if (i != 0) {
                    try {
                        iPowerManagerAsInterface.wakeUp(SystemClock.uptimeMillis(), "PowerCommand", (String) null);
                    } catch (RemoteException e) {
                        System.err.println("Faild to set setting: " + e);
                        return;
                    }
                }
                iPowerManagerAsInterface.setStayOnSetting(i);
                return;
            }
            if ("reboot".equals(strArr[1])) {
                try {
                    iPowerManagerAsInterface.reboot(false, strArr.length == 3 ? strArr[2] : null, true);
                    return;
                } catch (RemoteException e2) {
                    maybeLogRemoteException("Failed to reboot.");
                    return;
                }
            } else if ("shutdown".equals(strArr[1])) {
                try {
                    iPowerManagerAsInterface.shutdown(false, (String) null, true);
                    return;
                } catch (RemoteException e3) {
                    maybeLogRemoteException("Failed to shutdown.");
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }

    private void maybeLogRemoteException(String str) {
        if (SystemProperties.get("sys.powerctl").isEmpty()) {
            System.err.println(str);
        }
    }
}
