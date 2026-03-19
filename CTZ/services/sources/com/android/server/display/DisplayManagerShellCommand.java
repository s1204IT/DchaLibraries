package com.android.server.display;

import android.content.Intent;
import android.os.ShellCommand;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.display.DisplayManagerService;
import java.io.PrintWriter;

class DisplayManagerShellCommand extends ShellCommand {
    private static final String TAG = "DisplayManagerShellCommand";
    private final DisplayManagerService.BinderService mService;

    DisplayManagerShellCommand(DisplayManagerService.BinderService binderService) {
        this.mService = binderService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        getOutPrintWriter();
        byte b = -1;
        int iHashCode = str.hashCode();
        if (iHashCode != -1505467592) {
            if (iHashCode == 1604823708 && str.equals("set-brightness")) {
                b = 0;
            }
        } else if (str.equals("reset-brightness-configuration")) {
            b = 1;
        }
        switch (b) {
        }
        return handleDefaultCommands(str);
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Display manager commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println();
        outPrintWriter.println("  set-brightness BRIGHTNESS");
        outPrintWriter.println("    Sets the current brightness to BRIGHTNESS (a number between 0 and 1).");
        outPrintWriter.println("  reset-brightness-configuration");
        outPrintWriter.println("    Reset the brightness to its default configuration.");
        outPrintWriter.println();
        Intent.printIntentArgsHelp(outPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    private int setBrightness() {
        float f;
        String nextArg = getNextArg();
        if (nextArg == null) {
            getErrPrintWriter().println("Error: no brightness specified");
            return 1;
        }
        try {
            f = Float.parseFloat(nextArg);
        } catch (NumberFormatException e) {
            f = -1.0f;
        }
        if (f < 0.0f || f > 1.0f) {
            getErrPrintWriter().println("Error: brightness should be a number between 0 and 1");
            return 1;
        }
        this.mService.setBrightness(((int) f) * 255);
        return 0;
    }

    private int resetBrightnessConfiguration() {
        this.mService.resetBrightnessConfiguration();
        return 0;
    }
}
