package com.android.server.net.watchlist;

import android.content.Context;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.provider.Settings;
import java.io.FileInputStream;
import java.io.PrintWriter;

class NetworkWatchlistShellCommand extends ShellCommand {
    final Context mContext;
    final NetworkWatchlistService mService;

    NetworkWatchlistShellCommand(NetworkWatchlistService networkWatchlistService, Context context) {
        this.mContext = context;
        this.mService = networkWatchlistService;
    }

    public int onCommand(String str) {
        byte b;
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            int iHashCode = str.hashCode();
            if (iHashCode != 1757613042) {
                b = (iHashCode == 1854202282 && str.equals("force-generate-report")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("set-test-config")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    return runSetTestConfig();
                case 1:
                    return runForceGenerateReport();
                default:
                    return handleDefaultCommands(str);
            }
        } catch (Exception e) {
            outPrintWriter.println("Exception: " + e);
            return -1;
        }
    }

    private int runSetTestConfig() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            ParcelFileDescriptor parcelFileDescriptorOpenFileForSystem = openFileForSystem(getNextArgRequired(), "r");
            if (parcelFileDescriptorOpenFileForSystem != null) {
                WatchlistConfig.getInstance().setTestMode(new FileInputStream(parcelFileDescriptorOpenFileForSystem.getFileDescriptor()));
            }
            outPrintWriter.println("Success!");
            return 0;
        } catch (Exception e) {
            outPrintWriter.println("Error: " + e.toString());
            return -1;
        }
    }

    private int runForceGenerateReport() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (WatchlistConfig.getInstance().isConfigSecure()) {
                outPrintWriter.println("Error: Cannot force generate report under production config");
                return -1;
            }
            Settings.Global.putLong(this.mContext.getContentResolver(), "network_watchlist_last_report_time", 0L);
            this.mService.forceReportWatchlistForTest(System.currentTimeMillis());
            outPrintWriter.println("Success!");
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return 0;
        } catch (Exception e) {
            outPrintWriter.println("Error: " + e);
            return -1;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Network watchlist manager commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  set-test-config your_watchlist_config.xml");
        outPrintWriter.println("    Set network watchlist test config file.");
        outPrintWriter.println("  force-generate-report");
        outPrintWriter.println("    Force generate watchlist test report.");
    }
}
