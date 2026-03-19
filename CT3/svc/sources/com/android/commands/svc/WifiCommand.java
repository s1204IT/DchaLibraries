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
    public void run(String[] args) {
        boolean validCommand = false;
        if (args.length >= 2) {
            boolean flag = false;
            if ("enable".equals(args[1])) {
                flag = true;
                validCommand = true;
            } else if ("disable".equals(args[1])) {
                flag = false;
                validCommand = true;
            }
            if (validCommand) {
                IWifiManager wifiMgr = IWifiManager.Stub.asInterface(ServiceManager.getService("wifi"));
                try {
                    wifiMgr.setWifiEnabled(flag);
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
