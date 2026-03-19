package com.android.server.slice;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ShellCommand;
import android.util.ArraySet;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

public class SliceShellCommand extends ShellCommand {
    private final SliceManagerService mService;

    public SliceShellCommand(SliceManagerService sliceManagerService) {
        this.mService = sliceManagerService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        byte b = -1;
        if (str.hashCode() == -185318259 && str.equals("get-permissions")) {
            b = 0;
        }
        if (b != 0) {
            return 0;
        }
        return runGetPermissions(getNextArgRequired());
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Status bar commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  get-permissions <authority>");
        outPrintWriter.println("    List the pkgs that have permission to an authority.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    private int runGetPermissions(String str) {
        if (Binder.getCallingUid() != 2000 && Binder.getCallingUid() != 0) {
            getOutPrintWriter().println("Only shell can get permissions");
            return -1;
        }
        Context context = this.mService.getContext();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Uri uriBuild = new Uri.Builder().scheme("content").authority(str).build();
            if (!"vnd.android.slice".equals(context.getContentResolver().getType(uriBuild))) {
                getOutPrintWriter().println(str + " is not a slice provider");
                return -1;
            }
            Bundle bundleCall = context.getContentResolver().call(uriBuild, "get_permissions", (String) null, (Bundle) null);
            if (bundleCall == null) {
                getOutPrintWriter().println("An error occurred getting permissions");
                return -1;
            }
            String[] stringArray = bundleCall.getStringArray("result");
            PrintWriter outPrintWriter = getOutPrintWriter();
            ArraySet arraySet = new ArraySet();
            if (stringArray != null && stringArray.length != 0) {
                for (PackageInfo packageInfo : context.getPackageManager().getPackagesHoldingPermissions(stringArray, 0)) {
                    outPrintWriter.println(packageInfo.packageName);
                    arraySet.add(packageInfo.packageName);
                }
            }
            for (String str2 : this.mService.getAllPackagesGranted(str)) {
                if (!arraySet.contains(str2)) {
                    outPrintWriter.println(str2);
                    arraySet.add(str2);
                }
            }
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
