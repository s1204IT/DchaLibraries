package com.android.server.pm;

import android.content.pm.IOtaDexopt;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.util.Locale;

class OtaDexoptShellCommand extends ShellCommand {
    final IOtaDexopt mInterface;

    OtaDexoptShellCommand(OtaDexoptService otaDexoptService) {
        this.mInterface = otaDexoptService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(null);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str) {
                case "prepare":
                    return runOtaPrepare();
                case "cleanup":
                    return runOtaCleanup();
                case "done":
                    return runOtaDone();
                case "step":
                    return runOtaStep();
                case "next":
                    return runOtaNext();
                case "progress":
                    return runOtaProgress();
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runOtaPrepare() throws RemoteException {
        this.mInterface.prepare();
        getOutPrintWriter().println("Success");
        return 0;
    }

    private int runOtaCleanup() throws RemoteException {
        this.mInterface.cleanup();
        return 0;
    }

    private int runOtaDone() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        if (this.mInterface.isDone()) {
            outPrintWriter.println("OTA complete.");
            return 0;
        }
        outPrintWriter.println("OTA incomplete.");
        return 0;
    }

    private int runOtaStep() throws RemoteException {
        this.mInterface.dexoptNextPackage();
        return 0;
    }

    private int runOtaNext() throws RemoteException {
        getOutPrintWriter().println(this.mInterface.nextDexoptCommand());
        return 0;
    }

    private int runOtaProgress() throws RemoteException {
        getOutPrintWriter().format(Locale.ROOT, "%.2f", Float.valueOf(this.mInterface.getProgress()));
        return 0;
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("OTA Dexopt (ota) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  prepare");
        outPrintWriter.println("    Prepare an OTA dexopt pass, collecting all packages.");
        outPrintWriter.println("  done");
        outPrintWriter.println("    Replies whether the OTA is complete or not.");
        outPrintWriter.println("  step");
        outPrintWriter.println("    OTA dexopt the next package.");
        outPrintWriter.println("  next");
        outPrintWriter.println("    Get parameters for OTA dexopt of the next package.");
        outPrintWriter.println("  cleanup");
        outPrintWriter.println("    Clean up internal states. Ends an OTA session.");
    }
}
