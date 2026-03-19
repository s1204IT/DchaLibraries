package com.android.commands.svc;

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
        return shortHelp() + "\n\nusage: svc data [enable|disable]\n         Turn mobile data on or off.\n\n";
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
                ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                try {
                    if (z2) {
                        iTelephonyAsInterface.enableDataConnectivity();
                    } else {
                        iTelephonyAsInterface.disableDataConnectivity();
                    }
                    return;
                } catch (RemoteException e) {
                    System.err.println("Mobile data operation failed: " + e);
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }
}
