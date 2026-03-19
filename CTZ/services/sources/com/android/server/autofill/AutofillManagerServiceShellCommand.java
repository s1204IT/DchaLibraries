package com.android.server.autofill;

import android.os.Bundle;
import android.os.RemoteCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.service.autofill.AutofillFieldClassificationService;
import com.android.internal.os.IResultReceiver;
import com.android.server.BatteryService;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class AutofillManagerServiceShellCommand extends ShellCommand {
    private final AutofillManagerService mService;

    public AutofillManagerServiceShellCommand(AutofillManagerService autofillManagerService) {
        this.mService = autofillManagerService;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        switch (str) {
        }
        return handleDefaultCommands(str);
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        Throwable th = null;
        try {
            outPrintWriter.println("AutoFill Service (autofill) commands:");
            outPrintWriter.println("  help");
            outPrintWriter.println("    Prints this help text.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  get log_level ");
            outPrintWriter.println("    Gets the Autofill log level (off | debug | verbose).");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  get max_partitions");
            outPrintWriter.println("    Gets the maximum number of partitions per session.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  get max_visible_datasets");
            outPrintWriter.println("    Gets the maximum number of visible datasets in the UI.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  get full_screen_mode");
            outPrintWriter.println("    Gets the Fill UI full screen mode");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  get fc_score [--algorithm ALGORITHM] value1 value2");
            outPrintWriter.println("    Gets the field classification score for 2 fields.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  get bind-instant-service-allowed");
            outPrintWriter.println("    Gets whether binding to services provided by instant apps is allowed");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  set log_level [off | debug | verbose]");
            outPrintWriter.println("    Sets the Autofill log level.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  set max_partitions number");
            outPrintWriter.println("    Sets the maximum number of partitions per session.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  set max_visible_datasets number");
            outPrintWriter.println("    Sets the maximum number of visible datasets in the UI.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  set full_screen_mode [true | false | default]");
            outPrintWriter.println("    Sets the Fill UI full screen mode");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  set bind-instant-service-allowed [true | false]");
            outPrintWriter.println("    Sets whether binding to services provided by instant apps is allowed");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  list sessions [--user USER_ID]");
            outPrintWriter.println("    Lists all pending sessions.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  destroy sessions [--user USER_ID]");
            outPrintWriter.println("    Destroys all pending sessions.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            outPrintWriter.println("  reset");
            outPrintWriter.println("    Resets all pending sessions and cached service connections.");
            outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            if (outPrintWriter != null) {
                outPrintWriter.close();
            }
        } catch (Throwable th2) {
            if (outPrintWriter != null) {
                if (0 != 0) {
                    try {
                        outPrintWriter.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    outPrintWriter.close();
                }
            }
            throw th2;
        }
    }

    private int requestGet(PrintWriter printWriter) {
        String nextArgRequired;
        nextArgRequired = getNextArgRequired();
        switch (nextArgRequired) {
            case "log_level":
                return getLogLevel(printWriter);
            case "max_partitions":
                return getMaxPartitions(printWriter);
            case "max_visible_datasets":
                return getMaxVisibileDatasets(printWriter);
            case "fc_score":
                return getFieldClassificationScore(printWriter);
            case "full_screen_mode":
                return getFullScreenMode(printWriter);
            case "bind-instant-service-allowed":
                return getBindInstantService(printWriter);
            default:
                printWriter.println("Invalid set: " + nextArgRequired);
                return -1;
        }
    }

    private int requestSet(PrintWriter printWriter) {
        String nextArgRequired;
        nextArgRequired = getNextArgRequired();
        switch (nextArgRequired) {
            case "log_level":
                return setLogLevel(printWriter);
            case "max_partitions":
                return setMaxPartitions();
            case "max_visible_datasets":
                return setMaxVisibileDatasets();
            case "full_screen_mode":
                return setFullScreenMode(printWriter);
            case "bind-instant-service-allowed":
                return setBindInstantService(printWriter);
            default:
                printWriter.println("Invalid set: " + nextArgRequired);
                return -1;
        }
    }

    private int getLogLevel(PrintWriter printWriter) {
        int logLevel = this.mService.getLogLevel();
        if (logLevel == 0) {
            printWriter.println("off");
            return 0;
        }
        if (logLevel == 2) {
            printWriter.println("debug");
            return 0;
        }
        if (logLevel == 4) {
            printWriter.println("verbose");
            return 0;
        }
        printWriter.println("unknow (" + logLevel + ")");
        return 0;
    }

    private int setLogLevel(PrintWriter printWriter) {
        byte b;
        String nextArgRequired = getNextArgRequired();
        String lowerCase = nextArgRequired.toLowerCase();
        int iHashCode = lowerCase.hashCode();
        if (iHashCode != 109935) {
            if (iHashCode != 95458899) {
                b = (iHashCode == 351107458 && lowerCase.equals("verbose")) ? (byte) 0 : (byte) -1;
            } else if (lowerCase.equals("debug")) {
                b = 1;
            }
        } else if (lowerCase.equals("off")) {
            b = 2;
        }
        switch (b) {
            case 0:
                this.mService.setLogLevel(4);
                return 0;
            case 1:
                this.mService.setLogLevel(2);
                return 0;
            case 2:
                this.mService.setLogLevel(0);
                return 0;
            default:
                printWriter.println("Invalid level: " + nextArgRequired);
                return -1;
        }
    }

    private int getMaxPartitions(PrintWriter printWriter) {
        printWriter.println(this.mService.getMaxPartitions());
        return 0;
    }

    private int setMaxPartitions() {
        this.mService.setMaxPartitions(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    private int getMaxVisibileDatasets(PrintWriter printWriter) {
        printWriter.println(this.mService.getMaxVisibleDatasets());
        return 0;
    }

    private int setMaxVisibileDatasets() {
        this.mService.setMaxVisibleDatasets(Integer.parseInt(getNextArgRequired()));
        return 0;
    }

    private int getFieldClassificationScore(final PrintWriter printWriter) {
        String nextArgRequired;
        String nextArgRequired2;
        String nextArgRequired3 = getNextArgRequired();
        if ("--algorithm".equals(nextArgRequired3)) {
            nextArgRequired2 = getNextArgRequired();
            nextArgRequired = getNextArgRequired();
        } else {
            nextArgRequired = nextArgRequired3;
            nextArgRequired2 = null;
        }
        String nextArgRequired4 = getNextArgRequired();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        this.mService.getScore(nextArgRequired2, nextArgRequired, nextArgRequired4, new RemoteCallback(new RemoteCallback.OnResultListener() {
            public final void onResult(Bundle bundle) {
                AutofillManagerServiceShellCommand.lambda$getFieldClassificationScore$0(printWriter, countDownLatch, bundle);
            }
        }));
        return waitForLatch(printWriter, countDownLatch);
    }

    static void lambda$getFieldClassificationScore$0(PrintWriter printWriter, CountDownLatch countDownLatch, Bundle bundle) {
        AutofillFieldClassificationService.Scores parcelable = bundle.getParcelable("scores");
        if (parcelable == null) {
            printWriter.println("no score");
        } else {
            printWriter.println(parcelable.scores[0][0]);
        }
        countDownLatch.countDown();
    }

    private int getFullScreenMode(PrintWriter printWriter) {
        Boolean fullScreenMode = this.mService.getFullScreenMode();
        if (fullScreenMode == null) {
            printWriter.println(BatteryService.HealthServiceWrapper.INSTANCE_VENDOR);
            return 0;
        }
        if (fullScreenMode.booleanValue()) {
            printWriter.println("true");
            return 0;
        }
        printWriter.println("false");
        return 0;
    }

    private int setFullScreenMode(PrintWriter printWriter) {
        byte b;
        String nextArgRequired = getNextArgRequired();
        String lowerCase = nextArgRequired.toLowerCase();
        int iHashCode = lowerCase.hashCode();
        if (iHashCode != 3569038) {
            if (iHashCode != 97196323) {
                b = (iHashCode == 1544803905 && lowerCase.equals(BatteryService.HealthServiceWrapper.INSTANCE_VENDOR)) ? (byte) 2 : (byte) -1;
            } else if (lowerCase.equals("false")) {
                b = 1;
            }
        } else if (lowerCase.equals("true")) {
            b = 0;
        }
        switch (b) {
            case 0:
                this.mService.setFullScreenMode(Boolean.TRUE);
                return 0;
            case 1:
                this.mService.setFullScreenMode(Boolean.FALSE);
                return 0;
            case 2:
                this.mService.setFullScreenMode(null);
                return 0;
            default:
                printWriter.println("Invalid mode: " + nextArgRequired);
                return -1;
        }
    }

    private int getBindInstantService(PrintWriter printWriter) {
        if (this.mService.getAllowInstantService()) {
            printWriter.println("true");
            return 0;
        }
        printWriter.println("false");
        return 0;
    }

    private int setBindInstantService(PrintWriter printWriter) {
        byte b;
        String nextArgRequired = getNextArgRequired();
        String lowerCase = nextArgRequired.toLowerCase();
        int iHashCode = lowerCase.hashCode();
        if (iHashCode != 3569038) {
            b = (iHashCode == 97196323 && lowerCase.equals("false")) ? (byte) 1 : (byte) -1;
        } else if (lowerCase.equals("true")) {
            b = 0;
        }
        switch (b) {
            case 0:
                this.mService.setAllowInstantService(true);
                return 0;
            case 1:
                this.mService.setAllowInstantService(false);
                return 0;
            default:
                printWriter.println("Invalid mode: " + nextArgRequired);
                return -1;
        }
    }

    private int requestDestroy(PrintWriter printWriter) {
        if (!isNextArgSessions(printWriter)) {
            return -1;
        }
        final int userIdFromArgsOrAllUsers = getUserIdFromArgsOrAllUsers();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final IResultReceiver.Stub stub = new IResultReceiver.Stub() {
            public void send(int i, Bundle bundle) {
                countDownLatch.countDown();
            }
        };
        return requestSessionCommon(printWriter, countDownLatch, new Runnable() {
            @Override
            public final void run() {
                this.f$0.mService.destroySessions(userIdFromArgsOrAllUsers, stub);
            }
        });
    }

    private int requestList(final PrintWriter printWriter) {
        if (!isNextArgSessions(printWriter)) {
            return -1;
        }
        final int userIdFromArgsOrAllUsers = getUserIdFromArgsOrAllUsers();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final IResultReceiver.Stub stub = new IResultReceiver.Stub() {
            public void send(int i, Bundle bundle) {
                Iterator<String> it = bundle.getStringArrayList("sessions").iterator();
                while (it.hasNext()) {
                    printWriter.println(it.next());
                }
                countDownLatch.countDown();
            }
        };
        return requestSessionCommon(printWriter, countDownLatch, new Runnable() {
            @Override
            public final void run() {
                this.f$0.mService.listSessions(userIdFromArgsOrAllUsers, stub);
            }
        });
    }

    private boolean isNextArgSessions(PrintWriter printWriter) {
        if (!getNextArgRequired().equals("sessions")) {
            printWriter.println("Error: invalid list type");
            return false;
        }
        return true;
    }

    private int requestSessionCommon(PrintWriter printWriter, CountDownLatch countDownLatch, Runnable runnable) {
        runnable.run();
        return waitForLatch(printWriter, countDownLatch);
    }

    private int waitForLatch(PrintWriter printWriter, CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(5L, TimeUnit.SECONDS)) {
                printWriter.println("Timed out after 5 seconds");
                return -1;
            }
            return 0;
        } catch (InterruptedException e) {
            printWriter.println("System call interrupted");
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private int requestReset() {
        this.mService.reset();
        return 0;
    }

    private int getUserIdFromArgsOrAllUsers() {
        if ("--user".equals(getNextArg())) {
            return UserHandle.parseUserArg(getNextArgRequired());
        }
        return -1;
    }
}
