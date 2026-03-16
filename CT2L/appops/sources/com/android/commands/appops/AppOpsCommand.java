package com.android.commands.appops;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.pm.IPackageManager;
import android.os.ServiceManager;
import android.util.TimeUtils;
import com.android.internal.app.IAppOpsService;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;
import java.util.List;

public class AppOpsCommand extends BaseCommand {
    private static final String ARGUMENT_USER = "--user";
    private static final String COMMAND_GET = "get";
    private static final String COMMAND_RESET = "reset";
    private static final String COMMAND_SET = "set";
    private static final String MODE_ALLOW = "allow";
    private static final String MODE_DEFAULT = "default";
    private static final String MODE_DENY = "deny";
    private static final String MODE_IGNORE = "ignore";

    public static void main(String[] args) {
        new AppOpsCommand().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: appops set [--user <USER_ID>] <PACKAGE> <OP> <MODE>\n       appops get [--user <USER_ID>] <PACKAGE> [<OP>]\n       appops reset [--user <USER_ID>] [<PACKAGE>]\n  <PACKAGE> an Android package name.\n  <OP>      an AppOps operation.\n  <MODE>    one of allow, ignore, deny, or default\n  <USER_ID> the user id under which the package is installed. If --user is not\n            specified, the current user is assumed.\n");
    }

    public void onRun() throws Exception {
        String command;
        command = nextArgRequired();
        switch (command) {
            case "set":
                runSet();
                break;
            case "get":
                runGet();
                break;
            case "reset":
                runReset();
                break;
            default:
                System.err.println("Error: Unknown command: '" + command + "'.");
                break;
        }
    }

    private int strOpToOp(String op) {
        try {
            return AppOpsManager.strOpToOp(op);
        } catch (IllegalArgumentException e) {
            try {
                return Integer.parseInt(op);
            } catch (NumberFormatException e2) {
                try {
                    return AppOpsManager.strDebugOpToOp(op);
                } catch (IllegalArgumentException e3) {
                    System.err.println("Error: " + e3.getMessage());
                    return -1;
                }
            }
        }
    }

    private void runSet() throws Exception {
        int modeInt;
        String packageName = null;
        String op = null;
        String mode = null;
        int userId = -2;
        while (true) {
            String argument = nextArg();
            if (argument != null) {
                if (ARGUMENT_USER.equals(argument)) {
                    userId = Integer.parseInt(nextArgRequired());
                } else if (packageName == null) {
                    packageName = argument;
                } else if (op == null) {
                    op = argument;
                } else if (mode == null) {
                    mode = argument;
                } else {
                    System.err.println("Error: Unsupported argument: " + argument);
                    return;
                }
            } else {
                if (packageName == null) {
                    System.err.println("Error: Package name not specified.");
                    return;
                }
                if (op == null) {
                    System.err.println("Error: Operation not specified.");
                    return;
                }
                if (mode == null) {
                    System.err.println("Error: Mode not specified.");
                    return;
                }
                int opInt = strOpToOp(op);
                if (opInt >= 0) {
                    switch (mode) {
                        case "allow":
                            modeInt = 0;
                            break;
                        case "deny":
                            modeInt = 2;
                            break;
                        case "ignore":
                            modeInt = 1;
                            break;
                        case "default":
                            modeInt = 3;
                            break;
                        default:
                            System.err.println("Error: Mode " + mode + " is not valid,");
                            return;
                    }
                    if (userId == -2) {
                        userId = ActivityManager.getCurrentUser();
                    }
                    IPackageManager pm = ActivityThread.getPackageManager();
                    IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
                    int uid = pm.getPackageUid(packageName, userId);
                    if (uid < 0) {
                        System.err.println("Error: No UID for " + packageName + " in user " + userId);
                        return;
                    } else {
                        appOpsService.setMode(opInt, uid, packageName, modeInt);
                        return;
                    }
                }
                return;
            }
        }
    }

    private void runGet() throws Exception {
        String packageName = null;
        String op = null;
        int userId = -2;
        while (true) {
            String argument = nextArg();
            if (argument != null) {
                if (ARGUMENT_USER.equals(argument)) {
                    userId = Integer.parseInt(nextArgRequired());
                } else if (packageName == null) {
                    packageName = argument;
                } else if (op == null) {
                    op = argument;
                } else {
                    System.err.println("Error: Unsupported argument: " + argument);
                    return;
                }
            } else {
                if (packageName == null) {
                    System.err.println("Error: Package name not specified.");
                    return;
                }
                int opInt = op != null ? strOpToOp(op) : 0;
                if (userId == -2) {
                    userId = ActivityManager.getCurrentUser();
                }
                IPackageManager pm = ActivityThread.getPackageManager();
                IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
                int uid = pm.getPackageUid(packageName, userId);
                if (uid < 0) {
                    System.err.println("Error: No UID for " + packageName + " in user " + userId);
                    return;
                }
                List<AppOpsManager.PackageOps> ops = appOpsService.getOpsForPackage(uid, packageName, op != null ? new int[]{opInt} : null);
                if (ops == null || ops.size() <= 0) {
                    System.out.println("No operations.");
                    return;
                }
                long now = System.currentTimeMillis();
                for (int i = 0; i < ops.size(); i++) {
                    List<AppOpsManager.OpEntry> entries = ops.get(i).getOps();
                    for (int j = 0; j < entries.size(); j++) {
                        AppOpsManager.OpEntry ent = entries.get(j);
                        System.out.print(AppOpsManager.opToName(ent.getOp()));
                        System.out.print(": ");
                        switch (ent.getMode()) {
                            case 0:
                                System.out.print(MODE_ALLOW);
                                break;
                            case 1:
                                System.out.print(MODE_IGNORE);
                                break;
                            case 2:
                                System.out.print(MODE_DENY);
                                break;
                            case 3:
                                System.out.print(MODE_DEFAULT);
                                break;
                            default:
                                System.out.print("mode=");
                                System.out.print(ent.getMode());
                                break;
                        }
                        if (ent.getTime() != 0) {
                            System.out.print("; time=");
                            StringBuilder sb = new StringBuilder();
                            TimeUtils.formatDuration(now - ent.getTime(), sb);
                            System.out.print(sb);
                            System.out.print(" ago");
                        }
                        if (ent.getRejectTime() != 0) {
                            System.out.print("; rejectTime=");
                            StringBuilder sb2 = new StringBuilder();
                            TimeUtils.formatDuration(now - ent.getRejectTime(), sb2);
                            System.out.print(sb2);
                            System.out.print(" ago");
                        }
                        if (ent.getDuration() == -1) {
                            System.out.print(" (running)");
                        } else if (ent.getDuration() != 0) {
                            System.out.print("; duration=");
                            StringBuilder sb3 = new StringBuilder();
                            TimeUtils.formatDuration(ent.getDuration(), sb3);
                            System.out.print(sb3);
                        }
                        System.out.println();
                    }
                }
                return;
            }
        }
    }

    private void runReset() throws Exception {
        String packageName = null;
        int userId = -2;
        while (true) {
            String argument = nextArg();
            if (argument != null) {
                if (ARGUMENT_USER.equals(argument)) {
                    String userStr = nextArgRequired();
                    if ("all".equals(userStr)) {
                        userId = -1;
                    } else if ("current".equals(userStr)) {
                        userId = -2;
                    } else if ("owner".equals(userStr)) {
                        userId = 0;
                    } else {
                        userId = Integer.parseInt(nextArgRequired());
                    }
                } else if (packageName == null) {
                    packageName = argument;
                } else {
                    System.err.println("Error: Unsupported argument: " + argument);
                    return;
                }
            } else {
                if (userId == -2) {
                    userId = ActivityManager.getCurrentUser();
                }
                IAppOpsService appOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
                appOpsService.resetAllModes(userId, packageName);
                System.out.print("Reset all modes for: ");
                if (userId == -1) {
                    System.out.print("all users");
                } else {
                    System.out.print("user ");
                    System.out.print(userId);
                }
                System.out.print(", ");
                if (packageName == null) {
                    System.out.println("all packages");
                    return;
                } else {
                    System.out.print("package ");
                    System.out.println(packageName);
                    return;
                }
            }
        }
    }
}
