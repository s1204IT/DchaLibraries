package com.android.server.job;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.ShellCommand;
import android.os.UserHandle;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

public final class JobSchedulerShellCommand extends ShellCommand {
    public static final int CMD_ERR_CONSTRAINTS = -1002;
    public static final int CMD_ERR_NO_JOB = -1001;
    public static final int CMD_ERR_NO_PACKAGE = -1000;
    JobSchedulerService mInternal;
    IPackageManager mPM = AppGlobals.getPackageManager();

    JobSchedulerShellCommand(JobSchedulerService jobSchedulerService) {
        this.mInternal = jobSchedulerService;
    }

    public int onCommand(String str) {
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str != null ? str : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) {
                case "run":
                    return runJob(outPrintWriter);
                case "timeout":
                    return timeout(outPrintWriter);
                case "cancel":
                    return cancelJob(outPrintWriter);
                case "monitor-battery":
                    return monitorBattery(outPrintWriter);
                case "get-battery-seq":
                    return getBatterySeq(outPrintWriter);
                case "get-battery-charging":
                    return getBatteryCharging(outPrintWriter);
                case "get-battery-not-low":
                    return getBatteryNotLow(outPrintWriter);
                case "get-storage-seq":
                    return getStorageSeq(outPrintWriter);
                case "get-storage-not-low":
                    return getStorageNotLow(outPrintWriter);
                case "get-job-state":
                    return getJobState(outPrintWriter);
                case "heartbeat":
                    return doHeartbeat(outPrintWriter);
                case "trigger-dock-state":
                    return triggerDockState(outPrintWriter);
                default:
                    return handleDefaultCommands(str);
            }
        } catch (Exception e) {
            outPrintWriter.println("Exception: " + e);
            return -1;
        }
    }

    private void checkPermission(String str) throws Exception {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && this.mPM.checkUidPermission("android.permission.CHANGE_APP_IDLE_STATE", callingUid) != 0) {
            throw new SecurityException("Uid " + callingUid + " not permitted to " + str);
        }
    }

    private boolean printError(int i, String str, int i2, int i3) {
        switch (i) {
            case CMD_ERR_CONSTRAINTS:
                PrintWriter errPrintWriter = getErrPrintWriter();
                errPrintWriter.print("Job ");
                errPrintWriter.print(i3);
                errPrintWriter.print(" in package ");
                errPrintWriter.print(str);
                errPrintWriter.print(" / user ");
                errPrintWriter.print(i2);
                errPrintWriter.println(" has functional constraints but --force not specified");
                break;
            case CMD_ERR_NO_JOB:
                PrintWriter errPrintWriter2 = getErrPrintWriter();
                errPrintWriter2.print("Could not find job ");
                errPrintWriter2.print(i3);
                errPrintWriter2.print(" in package ");
                errPrintWriter2.print(str);
                errPrintWriter2.print(" / user ");
                errPrintWriter2.println(i2);
                break;
            case CMD_ERR_NO_PACKAGE:
                PrintWriter errPrintWriter3 = getErrPrintWriter();
                errPrintWriter3.print("Package not found: ");
                errPrintWriter3.print(str);
                errPrintWriter3.print(" / user ");
                errPrintWriter3.println(i2);
                break;
        }
        return true;
    }

    private int runJob(PrintWriter printWriter) throws Exception {
        byte b;
        checkPermission("force scheduled jobs");
        int i = 0;
        boolean z = false;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != -1626076853) {
                    if (iHashCode != 1497) {
                        if (iHashCode != 1512) {
                            b = (iHashCode == 1333469547 && nextOption.equals("--user")) ? (byte) 3 : (byte) -1;
                        } else if (nextOption.equals("-u")) {
                            b = 2;
                        }
                    } else if (nextOption.equals("-f")) {
                        b = 0;
                    }
                } else if (nextOption.equals("--force")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                    case 1:
                        z = true;
                        break;
                    case 2:
                    case 3:
                        i = Integer.parseInt(getNextArgRequired());
                        break;
                    default:
                        printWriter.println("Error: unknown option '" + nextOption + "'");
                        return -1;
                }
            } else {
                String nextArgRequired = getNextArgRequired();
                int i2 = Integer.parseInt(getNextArgRequired());
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    int iExecuteRunCommand = this.mInternal.executeRunCommand(nextArgRequired, i, i2, z);
                    if (printError(iExecuteRunCommand, nextArgRequired, i, i2)) {
                        return iExecuteRunCommand;
                    }
                    printWriter.print("Running job");
                    if (z) {
                        printWriter.print(" [FORCED]");
                    }
                    printWriter.println();
                    return iExecuteRunCommand;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    private int timeout(PrintWriter printWriter) throws Exception {
        checkPermission("force timeout jobs");
        int currentUser = -1;
        while (true) {
            String nextOption = getNextOption();
            byte b = 0;
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1512) {
                    b = (iHashCode == 1333469547 && nextOption.equals("--user")) ? (byte) 1 : (byte) -1;
                } else if (!nextOption.equals("-u")) {
                }
                switch (b) {
                    case 0:
                    case 1:
                        currentUser = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    default:
                        printWriter.println("Error: unknown option '" + nextOption + "'");
                        return -1;
                }
            } else {
                if (currentUser == -2) {
                    currentUser = ActivityManager.getCurrentUser();
                }
                int i = currentUser;
                String nextArg = getNextArg();
                String nextArg2 = getNextArg();
                int i2 = nextArg2 != null ? Integer.parseInt(nextArg2) : -1;
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mInternal.executeTimeoutCommand(printWriter, nextArg, i, nextArg2 != null, i2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    private int cancelJob(PrintWriter printWriter) throws Exception {
        checkPermission("cancel jobs");
        int userArg = 0;
        while (true) {
            String nextOption = getNextOption();
            byte b = 1;
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1512) {
                    if (iHashCode != 1333469547 || !nextOption.equals("--user")) {
                        b = -1;
                    }
                } else if (nextOption.equals("-u")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                    case 1:
                        userArg = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    default:
                        printWriter.println("Error: unknown option '" + nextOption + "'");
                        return -1;
                }
            } else {
                if (userArg < 0) {
                    printWriter.println("Error: must specify a concrete user ID");
                    return -1;
                }
                String nextArg = getNextArg();
                String nextArg2 = getNextArg();
                int i = nextArg2 != null ? Integer.parseInt(nextArg2) : -1;
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    return this.mInternal.executeCancelCommand(printWriter, nextArg, userArg, nextArg2 != null, i);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    private int monitorBattery(PrintWriter printWriter) throws Exception {
        checkPermission("change battery monitoring");
        String nextArgRequired = getNextArgRequired();
        boolean z = true;
        if (!"on".equals(nextArgRequired)) {
            if (!"off".equals(nextArgRequired)) {
                getErrPrintWriter().println("Error: unknown option " + nextArgRequired);
                return 1;
            }
            z = false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mInternal.setMonitorBattery(z);
            if (z) {
                printWriter.println("Battery monitoring enabled");
            } else {
                printWriter.println("Battery monitoring disabled");
            }
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int getBatterySeq(PrintWriter printWriter) {
        printWriter.println(this.mInternal.getBatterySeq());
        return 0;
    }

    private int getBatteryCharging(PrintWriter printWriter) {
        printWriter.println(this.mInternal.getBatteryCharging());
        return 0;
    }

    private int getBatteryNotLow(PrintWriter printWriter) {
        printWriter.println(this.mInternal.getBatteryNotLow());
        return 0;
    }

    private int getStorageSeq(PrintWriter printWriter) {
        printWriter.println(this.mInternal.getStorageSeq());
        return 0;
    }

    private int getStorageNotLow(PrintWriter printWriter) {
        printWriter.println(this.mInternal.getStorageNotLow());
        return 0;
    }

    private int getJobState(PrintWriter printWriter) throws Exception {
        byte b;
        checkPermission("force timeout jobs");
        int currentUser = 0;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1512) {
                    b = (iHashCode == 1333469547 && nextOption.equals("--user")) ? (byte) 1 : (byte) -1;
                } else if (nextOption.equals("-u")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                    case 1:
                        currentUser = UserHandle.parseUserArg(getNextArgRequired());
                        break;
                    default:
                        printWriter.println("Error: unknown option '" + nextOption + "'");
                        return -1;
                }
            } else {
                if (currentUser == -2) {
                    currentUser = ActivityManager.getCurrentUser();
                }
                String nextArgRequired = getNextArgRequired();
                int i = Integer.parseInt(getNextArgRequired());
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    int jobState = this.mInternal.getJobState(printWriter, nextArgRequired, currentUser, i);
                    printError(jobState, nextArgRequired, currentUser, i);
                    return jobState;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    private int doHeartbeat(PrintWriter printWriter) throws Exception {
        checkPermission("manipulate scheduler heartbeat");
        String nextArg = getNextArg();
        int i = nextArg != null ? Integer.parseInt(nextArg) : 0;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mInternal.executeHeartbeatCommand(printWriter, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int triggerDockState(PrintWriter printWriter) throws Exception {
        checkPermission("trigger wireless charging dock state");
        String nextArgRequired = getNextArgRequired();
        boolean z = true;
        if (!"idle".equals(nextArgRequired)) {
            if (!"active".equals(nextArgRequired)) {
                getErrPrintWriter().println("Error: unknown option " + nextArgRequired);
                return 1;
            }
            z = false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mInternal.triggerDockState(z);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Job scheduler (jobscheduler) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  run [-f | --force] [-u | --user USER_ID] PACKAGE JOB_ID");
        outPrintWriter.println("    Trigger immediate execution of a specific scheduled job.");
        outPrintWriter.println("    Options:");
        outPrintWriter.println("      -f or --force: run the job even if technical constraints such as");
        outPrintWriter.println("         connectivity are not currently met");
        outPrintWriter.println("      -u or --user: specify which user's job is to be run; the default is");
        outPrintWriter.println("         the primary or system user");
        outPrintWriter.println("  timeout [-u | --user USER_ID] [PACKAGE] [JOB_ID]");
        outPrintWriter.println("    Trigger immediate timeout of currently executing jobs, as if their.");
        outPrintWriter.println("    execution timeout had expired.");
        outPrintWriter.println("    Options:");
        outPrintWriter.println("      -u or --user: specify which user's job is to be run; the default is");
        outPrintWriter.println("         all users");
        outPrintWriter.println("  cancel [-u | --user USER_ID] PACKAGE [JOB_ID]");
        outPrintWriter.println("    Cancel a scheduled job.  If a job ID is not supplied, all jobs scheduled");
        outPrintWriter.println("    by that package will be canceled.  USE WITH CAUTION.");
        outPrintWriter.println("    Options:");
        outPrintWriter.println("      -u or --user: specify which user's job is to be run; the default is");
        outPrintWriter.println("         the primary or system user");
        outPrintWriter.println("  heartbeat [num]");
        outPrintWriter.println("    With no argument, prints the current standby heartbeat.  With a positive");
        outPrintWriter.println("    argument, advances the standby heartbeat by that number.");
        outPrintWriter.println("  monitor-battery [on|off]");
        outPrintWriter.println("    Control monitoring of all battery changes.  Off by default.  Turning");
        outPrintWriter.println("    on makes get-battery-seq useful.");
        outPrintWriter.println("  get-battery-seq");
        outPrintWriter.println("    Return the last battery update sequence number that was received.");
        outPrintWriter.println("  get-battery-charging");
        outPrintWriter.println("    Return whether the battery is currently considered to be charging.");
        outPrintWriter.println("  get-battery-not-low");
        outPrintWriter.println("    Return whether the battery is currently considered to not be low.");
        outPrintWriter.println("  get-storage-seq");
        outPrintWriter.println("    Return the last storage update sequence number that was received.");
        outPrintWriter.println("  get-storage-not-low");
        outPrintWriter.println("    Return whether storage is currently considered to not be low.");
        outPrintWriter.println("  get-job-state [-u | --user USER_ID] PACKAGE JOB_ID");
        outPrintWriter.println("    Return the current state of a job, may be any combination of:");
        outPrintWriter.println("      pending: currently on the pending list, waiting to be active");
        outPrintWriter.println("      active: job is actively running");
        outPrintWriter.println("      user-stopped: job can't run because its user is stopped");
        outPrintWriter.println("      backing-up: job can't run because app is currently backing up its data");
        outPrintWriter.println("      no-component: job can't run because its component is not available");
        outPrintWriter.println("      ready: job is ready to run (all constraints satisfied or bypassed)");
        outPrintWriter.println("      waiting: if nothing else above is printed, job not ready to run");
        outPrintWriter.println("    Options:");
        outPrintWriter.println("      -u or --user: specify which user's job is to be run; the default is");
        outPrintWriter.println("         the primary or system user");
        outPrintWriter.println("  trigger-dock-state [idle|active]");
        outPrintWriter.println("    Trigger wireless charging dock state.  Active by default.");
        outPrintWriter.println();
    }
}
