package com.android.commands.svc;

import android.net.wifi.IWifiManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.commands.svc.Svc;

public class WifiCommand extends Svc.Command {
    public WifiCommand() {
        super("wifi");
    }

    @Override
    public String shortHelp() {
        return "Control the Wi-Fi manager";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n\nusage: svc wifi [enable|disable]\n         Turn Wi-Fi on or off.\n\n";
    }

    @Override
    public void run(String[] strArr) {
        if (strArr.length >= 2) {
            boolean z = true;
            boolean z2 = false;
            if (!"enable".equals(strArr[1])) {
                if (!"disable".equals(strArr[1])) {
                    z = false;
                }
            } else {
                z2 = true;
            }
            if (z) {
                IWifiManager iWifiManagerAsInterface = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
                if (iWifiManagerAsInterface == null) {
                    System.err.println("Wi-Fi service is not ready");
                    return;
                }
                try {
                    iWifiManagerAsInterface.setWifiEnabled("com.android.shell", z2);
                    return;
                } catch (RemoteException e) {
                    System.err.println("Wi-Fi operation failed: " + e);
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }
}
