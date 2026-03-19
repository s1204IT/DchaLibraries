package com.android.server.statusbar;

import android.content.ComponentName;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.service.quicksettings.TileService;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

public class StatusBarShellCommand extends ShellCommand {
    private final StatusBarManagerService mInterface;

    public StatusBarShellCommand(StatusBarManagerService statusBarManagerService) {
        this.mInterface = statusBarManagerService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        try {
            switch (str) {
                case "expand-notifications":
                    return runExpandNotifications();
                case "expand-settings":
                    return runExpandSettings();
                case "collapse":
                    return runCollapse();
                case "add-tile":
                    return runAddTile();
                case "remove-tile":
                    return runRemoveTile();
                case "click-tile":
                    return runClickTile();
                case "check-support":
                    getOutPrintWriter().println(String.valueOf(TileService.isQuickSettingsSupported()));
                    return 0;
                case "get-status-icons":
                    return runGetStatusIcons();
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            getOutPrintWriter().println("Remote exception: " + e);
            return -1;
        }
    }

    private int runAddTile() throws RemoteException {
        this.mInterface.addTile(ComponentName.unflattenFromString(getNextArgRequired()));
        return 0;
    }

    private int runRemoveTile() throws RemoteException {
        this.mInterface.remTile(ComponentName.unflattenFromString(getNextArgRequired()));
        return 0;
    }

    private int runClickTile() throws RemoteException {
        this.mInterface.clickTile(ComponentName.unflattenFromString(getNextArgRequired()));
        return 0;
    }

    private int runCollapse() throws RemoteException {
        this.mInterface.collapsePanels();
        return 0;
    }

    private int runExpandSettings() throws RemoteException {
        this.mInterface.expandSettingsPanel(null);
        return 0;
    }

    private int runExpandNotifications() throws RemoteException {
        this.mInterface.expandNotificationsPanel();
        return 0;
    }

    private int runGetStatusIcons() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        for (String str : this.mInterface.getStatusBarIcons()) {
            outPrintWriter.println(str);
        }
        return 0;
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Status bar commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  expand-notifications");
        outPrintWriter.println("    Open the notifications panel.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  expand-settings");
        outPrintWriter.println("    Open the notifications panel and expand quick settings if present.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  collapse");
        outPrintWriter.println("    Collapse the notifications and settings panel.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  add-tile COMPONENT");
        outPrintWriter.println("    Add a TileService of the specified component");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  remove-tile COMPONENT");
        outPrintWriter.println("    Remove a TileService of the specified component");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  click-tile COMPONENT");
        outPrintWriter.println("    Click on a TileService of the specified component");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  check-support");
        outPrintWriter.println("    Check if this device supports QS + APIs");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-status-icons");
        outPrintWriter.println("    Print the list of status bar icons and the order they appear in");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }
}
