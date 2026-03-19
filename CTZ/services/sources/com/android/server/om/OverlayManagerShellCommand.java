package com.android.server.om;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

final class OverlayManagerShellCommand extends ShellCommand {
    private final IOverlayManager mInterface;

    OverlayManagerShellCommand(IOverlayManager iOverlayManager) {
        this.mInterface = iOverlayManager;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter errPrintWriter = getErrPrintWriter();
        try {
            switch (str) {
                case "list":
                    return runList();
                case "enable":
                    return runEnableDisable(true);
                case "disable":
                    return runEnableDisable(false);
                case "enable-exclusive":
                    return runEnableExclusive();
                case "set-priority":
                    return runSetPriority();
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            errPrintWriter.println("Remote exception: " + e);
            return -1;
        } catch (IllegalArgumentException e2) {
            errPrintWriter.println("Error: " + e2.getMessage());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Overlay manager (overlay) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  dump [--verbose] [--user USER_ID] [PACKAGE [PACKAGE [...]]]");
        outPrintWriter.println("    Print debugging information about the overlay manager.");
        outPrintWriter.println("  list [--user USER_ID] [PACKAGE [PACKAGE [...]]]");
        outPrintWriter.println("    Print information about target and overlay packages.");
        outPrintWriter.println("    Overlay packages are printed in priority order. With optional");
        outPrintWriter.println("    parameters PACKAGEs, limit output to the specified packages");
        outPrintWriter.println("    but include more information about each package.");
        outPrintWriter.println("  enable [--user USER_ID] PACKAGE");
        outPrintWriter.println("    Enable overlay package PACKAGE.");
        outPrintWriter.println("  disable [--user USER_ID] PACKAGE");
        outPrintWriter.println("    Disable overlay package PACKAGE.");
        outPrintWriter.println("  enable-exclusive [--user USER_ID] [--category] PACKAGE");
        outPrintWriter.println("    Enable overlay package PACKAGE and disable all other overlays for");
        outPrintWriter.println("    its target package. If the --category option is given, only disables");
        outPrintWriter.println("    other overlays in the same category.");
        outPrintWriter.println("  set-priority [--user USER_ID] PACKAGE PARENT|lowest|highest");
        outPrintWriter.println("    Change the priority of the overlay PACKAGE to be just higher than");
        outPrintWriter.println("    the priority of PACKAGE_PARENT If PARENT is the special keyword");
        outPrintWriter.println("    'lowest', change priority of PACKAGE to the lowest priority.");
        outPrintWriter.println("    If PARENT is the special keyword 'highest', change priority of");
        outPrintWriter.println("    PACKAGE to the highest priority.");
    }

    private int runList() throws RemoteException {
        String str;
        PrintWriter outPrintWriter = getOutPrintWriter();
        PrintWriter errPrintWriter = getErrPrintWriter();
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                if (nextOption.hashCode() == 1333469547 && nextOption.equals("--user")) {
                    b = 0;
                }
                if (b != 0) {
                    errPrintWriter.println("Error: Unknown option: " + nextOption);
                    return 1;
                }
                userArg = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                Map allOverlays = this.mInterface.getAllOverlays(userArg);
                for (String str2 : allOverlays.keySet()) {
                    outPrintWriter.println(str2);
                    List list = (List) allOverlays.get(str2);
                    int size = list.size();
                    for (int i = 0; i < size; i++) {
                        OverlayInfo overlayInfo = (OverlayInfo) list.get(i);
                        int i2 = overlayInfo.state;
                        if (i2 != 6) {
                            switch (i2) {
                                case 2:
                                    str = "[ ]";
                                    break;
                                case 3:
                                    str = "[x]";
                                    break;
                                default:
                                    str = "---";
                                    break;
                            }
                        }
                        outPrintWriter.println(String.format("%s %s", str, overlayInfo.packageName));
                    }
                    outPrintWriter.println();
                }
                return 0;
            }
        }
    }

    private int runEnableDisable(boolean z) throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                if (nextOption.hashCode() == 1333469547 && nextOption.equals("--user")) {
                    b = 0;
                }
                if (b == 0) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    errPrintWriter.println("Error: Unknown option: " + nextOption);
                    return 1;
                }
            } else {
                return !this.mInterface.setEnabled(getNextArgRequired(), z, userArg) ? 1 : 0;
            }
        }
    }

    private int runEnableExclusive() throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        Object[] objArr = false;
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 66265758) {
                    if (iHashCode == 1333469547 && nextOption.equals("--user")) {
                        b = 0;
                    }
                } else if (nextOption.equals("--category")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        userArg = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    case 1:
                        objArr = true;
                        break;
                    default:
                        errPrintWriter.println("Error: Unknown option: " + nextOption);
                        return 1;
                }
            } else {
                String nextArgRequired = getNextArgRequired();
                return objArr != false ? !this.mInterface.setEnabledExclusiveInCategory(nextArgRequired, userArg) ? 1 : 0 : !this.mInterface.setEnabledExclusive(nextArgRequired, true, userArg) ? 1 : 0;
            }
        }
    }

    private int runSetPriority() throws RemoteException {
        PrintWriter errPrintWriter = getErrPrintWriter();
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                byte b = -1;
                if (nextOption.hashCode() == 1333469547 && nextOption.equals("--user")) {
                    b = 0;
                }
                if (b == 0) {
                    userArg = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    errPrintWriter.println("Error: Unknown option: " + nextOption);
                    return 1;
                }
            } else {
                String nextArgRequired = getNextArgRequired();
                String nextArgRequired2 = getNextArgRequired();
                return "highest".equals(nextArgRequired2) ? !this.mInterface.setHighestPriority(nextArgRequired, userArg) ? 1 : 0 : "lowest".equals(nextArgRequired2) ? !this.mInterface.setLowestPriority(nextArgRequired, userArg) ? 1 : 0 : !this.mInterface.setPriority(nextArgRequired, nextArgRequired2, userArg) ? 1 : 0;
            }
        }
    }
}
