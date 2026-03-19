package com.android.commands.vr;

import android.app.Vr2dDisplayProperties;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.vr.IVrManager;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;

public final class Vr extends BaseCommand {
    private static final String COMMAND_ENABLE_VD = "enable-virtual-display";
    private static final String COMMAND_SET_PERSISTENT_VR_MODE_ENABLED = "set-persistent-vr-mode-enabled";
    private static final String COMMAND_SET_VR2D_DISPLAY_PROPERTIES = "set-display-props";
    private IVrManager mVrService;

    public static void main(String[] strArr) {
        new Vr().run(strArr);
    }

    public void onShowUsage(PrintStream printStream) {
        printStream.println("usage: vr [subcommand]\nusage: vr set-persistent-vr-mode-enabled [true|false]\nusage: vr set-display-props [width] [height] [dpi]\nusage: vr enable-virtual-display [true|false]\n");
    }

    public void onRun() throws Exception {
        this.mVrService = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (this.mVrService == null) {
            showError("Error: Could not access the Vr Manager. Is the system running?");
            return;
        }
        String strNextArgRequired = nextArgRequired();
        byte b = -1;
        int iHashCode = strNextArgRequired.hashCode();
        if (iHashCode != -190799946) {
            if (iHashCode != -111561094) {
                if (iHashCode == 2040743325 && strNextArgRequired.equals(COMMAND_SET_PERSISTENT_VR_MODE_ENABLED)) {
                    b = 1;
                }
            } else if (strNextArgRequired.equals(COMMAND_SET_VR2D_DISPLAY_PROPERTIES)) {
                b = 0;
            }
        } else if (strNextArgRequired.equals(COMMAND_ENABLE_VD)) {
            b = 2;
        }
        switch (b) {
            case 0:
                runSetVr2dDisplayProperties();
                return;
            case 1:
                runSetPersistentVrModeEnabled();
                return;
            case 2:
                runEnableVd();
                return;
            default:
                throw new IllegalArgumentException("unknown command '" + strNextArgRequired + "'");
        }
    }

    private void runSetVr2dDisplayProperties() throws RemoteException {
        try {
            this.mVrService.setVr2dDisplayProperties(new Vr2dDisplayProperties(Integer.parseInt(nextArgRequired()), Integer.parseInt(nextArgRequired()), Integer.parseInt(nextArgRequired())));
        } catch (RemoteException e) {
            System.err.println("Error: Can't set persistent mode " + e);
        }
    }

    private void runEnableVd() throws RemoteException {
        Vr2dDisplayProperties.Builder builder = new Vr2dDisplayProperties.Builder();
        String strNextArgRequired = nextArgRequired();
        if ("true".equals(strNextArgRequired)) {
            builder.setEnabled(true);
        } else if ("false".equals(strNextArgRequired)) {
            builder.setEnabled(false);
        }
        try {
            this.mVrService.setVr2dDisplayProperties(builder.build());
        } catch (RemoteException e) {
            System.err.println("Error: Can't enable (" + strNextArgRequired + ") virtual display" + e);
        }
    }

    private void runSetPersistentVrModeEnabled() throws RemoteException {
        try {
            this.mVrService.setPersistentVrModeEnabled(Boolean.parseBoolean(nextArg()));
        } catch (RemoteException e) {
            System.err.println("Error: Can't set persistent mode " + e);
        }
    }
}
