package com.android.server.wifi;

import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.ShellCommand;
import java.io.PrintWriter;

public class WifiShellCommand extends ShellCommand {
    private final IPackageManager mPM = AppGlobals.getPackageManager();
    private final WifiStateMachine mStateMachine;

    WifiShellCommand(WifiStateMachine wifiStateMachine) {
        this.mStateMachine = wifiStateMachine;
    }

    public int onCommand(String str) {
        byte b;
        checkRootPermission();
        PrintWriter outPrintWriter = getOutPrintWriter();
        String str2 = str != null ? str : "";
        try {
            int iHashCode = str2.hashCode();
            boolean z = true;
            if (iHashCode != -1861126232) {
                if (iHashCode != -1267819290) {
                    if (iHashCode != -29690534) {
                        b = (iHashCode == 1120712756 && str2.equals("get-ipreach-disconnect")) ? (byte) 1 : (byte) -1;
                    } else if (str2.equals("set-poll-rssi-interval-msecs")) {
                        b = 2;
                    }
                } else if (str2.equals("get-poll-rssi-interval-msecs")) {
                    b = 3;
                }
            } else if (str2.equals("set-ipreach-disconnect")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    String nextArgRequired = getNextArgRequired();
                    if (!"enabled".equals(nextArgRequired)) {
                        if (!"disabled".equals(nextArgRequired)) {
                            outPrintWriter.println("Invalid argument to 'set-ipreach-disconnect' - must be 'enabled' or 'disabled'");
                            return -1;
                        }
                        z = false;
                    }
                    this.mStateMachine.setIpReachabilityDisconnectEnabled(z);
                    return 0;
                case 1:
                    outPrintWriter.println("IPREACH_DISCONNECT state is " + this.mStateMachine.getIpReachabilityDisconnectEnabled());
                    return 0;
                case 2:
                    try {
                        int i = Integer.parseInt(getNextArgRequired());
                        if (i < 1) {
                            outPrintWriter.println("Invalid argument to 'set-poll-rssi-interval-msecs' - must be a positive integer");
                            return -1;
                        }
                        this.mStateMachine.setPollRssiIntervalMsecs(i);
                        return 0;
                    } catch (NumberFormatException e) {
                        outPrintWriter.println("Invalid argument to 'set-poll-rssi-interval-msecs' - must be a positive integer");
                        return -1;
                    }
                case 3:
                    outPrintWriter.println("WifiStateMachine.mPollRssiIntervalMsecs = " + this.mStateMachine.getPollRssiIntervalMsecs());
                    return 0;
                default:
                    return handleDefaultCommands(str);
            }
        } catch (Exception e2) {
            outPrintWriter.println("Exception: " + e2);
            return -1;
        }
        outPrintWriter.println("Exception: " + e2);
        return -1;
    }

    private void checkRootPermission() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0) {
            return;
        }
        throw new SecurityException("Uid " + callingUid + " does not have access to wifi commands");
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Wi-Fi (wifi) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  set-ipreach-disconnect enabled|disabled");
        outPrintWriter.println("    Sets whether CMD_IP_REACHABILITY_LOST events should trigger disconnects.");
        outPrintWriter.println("  get-ipreach-disconnect");
        outPrintWriter.println("    Gets setting of CMD_IP_REACHABILITY_LOST events triggering disconnects.");
        outPrintWriter.println("  set-poll-rssi-interval-msecs <int>");
        outPrintWriter.println("    Sets the interval between RSSI polls to <int> milliseconds.");
        outPrintWriter.println("  get-poll-rssi-interval-msecs");
        outPrintWriter.println("    Gets current interval between RSSI polls, in milliseconds.");
        outPrintWriter.println();
    }
}
