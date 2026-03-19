package com.android.server.power;

import android.content.Intent;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

class PowerManagerShellCommand extends ShellCommand {
    private static final int LOW_POWER_MODE_ON = 1;
    final IPowerManager mInterface;

    PowerManagerShellCommand(IPowerManager iPowerManager) {
        this.mInterface = iPowerManager;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            if (((str.hashCode() == 1369181230 && str.equals("set-mode")) ? (byte) 0 : (byte) -1) == 0) {
                return runSetMode();
            }
            return handleDefaultCommands(str);
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runSetMode() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            this.mInterface.setPowerSaveMode(Integer.parseInt(getNextArgRequired()) == 1);
            return 0;
        } catch (RuntimeException e) {
            outPrintWriter.println("Error: " + e.toString());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Power manager (power) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  set-mode MODE");
        outPrintWriter.println("    sets the power mode of the device to MODE.");
        outPrintWriter.println("    1 turns low power mode on and 0 turns low power mode off.");
        outPrintWriter.println();
        Intent.printIntentArgsHelp(outPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }
}
