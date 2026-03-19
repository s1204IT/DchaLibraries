package com.android.server.accessibility;

import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;

final class AccessibilityShellCommand extends ShellCommand {
    final AccessibilityManagerService mService;

    AccessibilityShellCommand(AccessibilityManagerService accessibilityManagerService) {
        this.mService = accessibilityManagerService;
    }

    public int onCommand(String str) {
        byte b;
        if (str == null) {
            return handleDefaultCommands(str);
        }
        int iHashCode = str.hashCode();
        if (iHashCode != -859068373) {
            b = (iHashCode == 789489311 && str.equals("set-bind-instant-service-allowed")) ? (byte) 1 : (byte) -1;
        } else if (str.equals("get-bind-instant-service-allowed")) {
            b = 0;
        }
        switch (b) {
            case 0:
                return runGetBindInstantServiceAllowed();
            case 1:
                return runSetBindInstantServiceAllowed();
            default:
                return -1;
        }
    }

    private int runGetBindInstantServiceAllowed() {
        Integer userId = parseUserId();
        if (userId == null) {
            return -1;
        }
        getOutPrintWriter().println(Boolean.toString(this.mService.getBindInstantServiceAllowed(userId.intValue())));
        return 0;
    }

    private int runSetBindInstantServiceAllowed() {
        Integer userId = parseUserId();
        if (userId == null) {
            return -1;
        }
        String nextArgRequired = getNextArgRequired();
        if (nextArgRequired == null) {
            getErrPrintWriter().println("Error: no true/false specified");
            return -1;
        }
        this.mService.setBindInstantServiceAllowed(userId.intValue(), Boolean.parseBoolean(nextArgRequired));
        return 0;
    }

    private Integer parseUserId() {
        String nextOption = getNextOption();
        if (nextOption != null) {
            if (nextOption.equals("--user")) {
                return Integer.valueOf(UserHandle.parseUserArg(getNextArgRequired()));
            }
            getErrPrintWriter().println("Unknown option: " + nextOption);
            return null;
        }
        return 0;
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Accessibility service (accessibility) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  set-bind-instant-service-allowed [--user <USER_ID>] true|false ");
        outPrintWriter.println("    Set whether binding to services provided by instant apps is allowed.");
        outPrintWriter.println("  get-bind-instant-service-allowed [--user <USER_ID>]");
        outPrintWriter.println("    Get whether binding to services provided by instant apps is allowed.");
    }
}
