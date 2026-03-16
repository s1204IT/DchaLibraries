package com.android.commands.svc;

import android.net.IConnectivityManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.commands.svc.Svc;
import com.android.internal.telephony.ITelephony;

public class DataCommand extends Svc.Command {
    public DataCommand() {
        super("data");
    }

    @Override
    public String shortHelp() {
        return "Control mobile data connectivity";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n\nusage: svc data [enable|disable]\n         Turn mobile data on or off.\n\n       svc data usbTether [enable|disable]\n         Turn usb tether on or off.\n";
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
            } else if ("usbTether".equals(args[1])) {
                boolean tetherValid = false;
                boolean tetherFlag = false;
                if ("enable".equals(args[2])) {
                    tetherFlag = true;
                    tetherValid = true;
                } else if ("disable".equals(args[2])) {
                    tetherFlag = false;
                    tetherValid = true;
                }
                if (tetherValid) {
                    IConnectivityManager connMgr = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
                    boolean usbTethered = false;
                    try {
                        String[] arr$ = connMgr.getTetheredIfaces();
                        for (String s : arr$) {
                            String[] arr$2 = connMgr.getTetherableUsbRegexs();
                            for (String regex : arr$2) {
                                if (s.matches(regex)) {
                                    usbTethered = true;
                                }
                            }
                        }
                        if (tetherFlag != usbTethered) {
                            connMgr.setUsbTethering(tetherFlag);
                            return;
                        }
                        return;
                    } catch (RemoteException e) {
                        System.err.println("Failed to set usb Tethering: " + e);
                        return;
                    }
                }
                return;
            }
            if (validCommand) {
                ITelephony phoneMgr = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                try {
                    if (flag) {
                        phoneMgr.enableDataConnectivity();
                    } else {
                        phoneMgr.disableDataConnectivity();
                    }
                    return;
                } catch (RemoteException e2) {
                    System.err.println("Mobile data operation failed: " + e2);
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }
}
