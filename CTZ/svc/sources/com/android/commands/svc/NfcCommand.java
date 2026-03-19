package com.android.commands.svc;

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
    public void run(String[] strArr) {
        INfcAdapter iNfcAdapterAsInterface = INfcAdapter.Stub.asInterface(ServiceManager.getService("nfc"));
        if (iNfcAdapterAsInterface == null) {
            System.err.println("Got a null NfcAdapter, is the system running?");
            return;
        }
        try {
            if (strArr.length == 2 && "enable".equals(strArr[1])) {
                iNfcAdapterAsInterface.enable();
            } else if (strArr.length == 2 && "disable".equals(strArr[1])) {
                iNfcAdapterAsInterface.disable(true);
            } else {
                System.err.println(longHelp());
            }
        } catch (RemoteException e) {
            System.err.println("NFC operation failed: " + e);
        }
    }
}
