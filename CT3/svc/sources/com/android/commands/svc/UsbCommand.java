package com.android.commands.svc;

import android.hardware.usb.IUsbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.android.commands.svc.Svc;

public class UsbCommand extends Svc.Command {
    public UsbCommand() {
        super("usb");
    }

    @Override
    public String shortHelp() {
        return "Control Usb state";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n\nusage: svc usb setFunction [function]\n         Set the current usb function.\n\n       svc usb getFunction\n          Gets the list of currently enabled functions\n";
    }

    @Override
    public void run(String[] args) {
        if (args.length >= 2) {
            if ("setFunction".equals(args[1])) {
                IUsbManager usbMgr = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
                try {
                    usbMgr.setCurrentFunction(args.length >= 3 ? args[2] : null);
                    return;
                } catch (RemoteException e) {
                    System.err.println("Error communicating with UsbManager: " + e);
                    return;
                }
            }
            if ("getFunction".equals(args[1])) {
                System.err.println(SystemProperties.get("sys.usb.config"));
                return;
            }
        }
        System.err.println(longHelp());
    }
}
