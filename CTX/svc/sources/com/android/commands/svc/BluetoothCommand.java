package com.android.commands.svc;

import android.bluetooth.BluetoothAdapter;
import com.android.commands.svc.Svc;

public class BluetoothCommand extends Svc.Command {
    public BluetoothCommand() {
        super("bluetooth");
    }

    @Override
    public String shortHelp() {
        return "Control Bluetooth service";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n\nusage: svc bluetooth [enable|disable]\n         Turn Bluetooth on or off.\n\n";
    }

    @Override
    public void run(String[] strArr) {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter == null) {
            System.err.println("Got a null BluetoothAdapter, is the system running?");
            return;
        }
        if (strArr.length == 2 && "enable".equals(strArr[1])) {
            defaultAdapter.enable();
        } else if (strArr.length == 2 && "disable".equals(strArr[1])) {
            defaultAdapter.disable();
        } else {
            System.err.println(longHelp());
        }
    }
}
