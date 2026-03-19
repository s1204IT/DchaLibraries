package com.android.commands.svc;

import android.content.pm.IPackageManager;
import android.nfc.INfcAdapter;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.commands.svc.Svc;

public class NfcCommand extends Svc.Command {
    public NfcCommand() {
        super("nfc");
    }

    @Override
    public String shortHelp() {
        return "Control NFC functions";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n\nusage: svc nfc [enable|disable]\n         Turn NFC on or off.\n\n";
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
                IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
                try {
                    if (pm.hasSystemFeature("android.hardware.nfc", 0)) {
                        INfcAdapter nfc = INfcAdapter.Stub.asInterface(ServiceManager.getService("nfc"));
                        try {
                            if (flag) {
                                nfc.enable();
                            } else {
                                nfc.disable(true);
                            }
                        } catch (RemoteException e) {
                            System.err.println("NFC operation failed: " + e);
                        }
                    } else {
                        System.err.println("NFC feature not supported.");
                    }
                    return;
                } catch (RemoteException e2) {
                    System.err.println("RemoteException while calling PackageManager, is the system running?");
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }
}
