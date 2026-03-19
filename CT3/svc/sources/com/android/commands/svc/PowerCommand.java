package com.android.commands.svc;

import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
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
    public void run(String[] args) {
        int val;
        if (args.length >= 2) {
            IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            if ("stayon".equals(args[1]) && args.length == 3) {
                if ("true".equals(args[2])) {
                    val = 7;
                } else if ("false".equals(args[2])) {
                    val = 0;
                } else if ("usb".equals(args[2])) {
                    val = 2;
                } else if ("ac".equals(args[2])) {
                    val = 1;
                } else if ("wireless".equals(args[2])) {
                    val = 4;
                }
                if (val != 0) {
                    try {
                        pm.wakeUp(SystemClock.uptimeMillis(), "PowerCommand", (String) null);
                    } catch (RemoteException e) {
                        System.err.println("Faild to set setting: " + e);
                        return;
                    }
                }
                pm.setStayOnSetting(val);
                return;
            }
            if ("reboot".equals(args[1])) {
                String mode = null;
                if (args.length == 3) {
                    mode = args[2];
                }
                try {
                    pm.reboot(false, mode, true);
                    return;
                } catch (RemoteException e2) {
                    System.err.println("Failed to reboot.");
                    return;
                }
            }
            if ("shutdown".equals(args[1])) {
                try {
                    pm.shutdown(false, (String) null, true);
                    return;
                } catch (RemoteException e3) {
                    System.err.println("Failed to shutdown.");
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }
}
