package com.android.server.content;

import android.content.IContentService;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

public class ContentShellCommand extends ShellCommand {
    final IContentService mInterface;

    ContentShellCommand(IContentService iContentService) {
        this.mInterface = iContentService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            if (((str.hashCode() == -796331115 && str.equals("reset-today-stats")) ? (byte) 0 : (byte) -1) == 0) {
                return runResetTodayStats();
            }
            return handleDefaultCommands(str);
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runResetTodayStats() throws RemoteException {
        this.mInterface.resetTodayStats();
        return 0;
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Content service commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  reset-today-stats");
        outPrintWriter.println("    Reset 1-day sync stats.");
        outPrintWriter.println();
    }
}
