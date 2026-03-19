package com.android.commands.svc;

import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
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
        return shortHelp() + "\n\nusage: svc usb setFunctions [function]\n         Set the current usb function. If function is blank, sets to charging.\n       svc usb setScreenUnlockedFunctions [function]\n         Sets the functions which, if the device was charging, become current onscreen unlock. If function is blank, turn off this feature.\n       svc usb getFunctions\n          Gets the list of currently enabled functions\n\npossible values of [function] are any of 'mtp', 'ptp', 'rndis', 'midi'\n";
    }

    @Override
    public void run(String[] strArr) {
        if (strArr.length >= 2) {
            IUsbManager iUsbManagerAsInterface = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
            if ("setFunctions".equals(strArr[1])) {
                try {
                    iUsbManagerAsInterface.setCurrentFunctions(UsbManager.usbFunctionsFromString(strArr.length >= 3 ? strArr[2] : ""));
                    return;
                } catch (RemoteException e) {
                    System.err.println("Error communicating with UsbManager: " + e);
                    return;
                }
            }
            if ("getFunctions".equals(strArr[1])) {
                try {
                    System.err.println(UsbManager.usbFunctionsToString(iUsbManagerAsInterface.getCurrentFunctions()));
                    return;
                } catch (RemoteException e2) {
                    System.err.println("Error communicating with UsbManager: " + e2);
                    return;
                }
            }
            if ("setScreenUnlockedFunctions".equals(strArr[1])) {
                try {
                    iUsbManagerAsInterface.setScreenUnlockedFunctions(UsbManager.usbFunctionsFromString(strArr.length >= 3 ? strArr[2] : ""));
                    return;
                } catch (RemoteException e3) {
                    System.err.println("Error communicating with UsbManager: " + e3);
                    return;
                }
            }
        }
        System.err.println(longHelp());
    }
}
