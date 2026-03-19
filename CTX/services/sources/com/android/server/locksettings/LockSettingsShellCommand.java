package com.android.server.locksettings;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;

class LockSettingsShellCommand extends ShellCommand {
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_GET_DISABLED = "get-disabled";
    private static final String COMMAND_SET_DISABLED = "set-disabled";
    private static final String COMMAND_SET_PASSWORD = "set-password";
    private static final String COMMAND_SET_PATTERN = "set-pattern";
    private static final String COMMAND_SET_PIN = "set-pin";
    private static final String COMMAND_SP = "sp";
    private static final String COMMAND_VERIFY = "verify";
    private final Context mContext;
    private int mCurrentUserId;
    private final LockPatternUtils mLockPatternUtils;
    private String mOld = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mNew = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;

    LockSettingsShellCommand(Context context, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public int onCommand(String str) {
        try {
            this.mCurrentUserId = ActivityManager.getService().getCurrentUser().id;
            parseArgs();
            if (!checkCredential()) {
                return -1;
            }
            switch (str) {
                case "set-pattern":
                    runSetPattern();
                    return 0;
                case "set-password":
                    runSetPassword();
                    return 0;
                case "set-pin":
                    runSetPin();
                    return 0;
                case "clear":
                    runClear();
                    return 0;
                case "sp":
                    runChangeSp();
                    return 0;
                case "set-disabled":
                    runSetDisabled();
                    return 0;
                case "verify":
                    runVerify();
                    return 0;
                case "get-disabled":
                    runGetDisabled();
                    return 0;
                default:
                    getErrPrintWriter().println("Unknown command: " + str);
                    return 0;
            }
        } catch (Exception e) {
            getErrPrintWriter().println("Error while executing command: " + str);
            e.printStackTrace(getErrPrintWriter());
            return -1;
        }
    }

    private void runVerify() {
        getOutPrintWriter().println("Lock credential verified successfully");
    }

    public void onHelp() {
    }

    private void parseArgs() {
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if ("--old".equals(nextOption)) {
                    this.mOld = getNextArgRequired();
                } else if ("--user".equals(nextOption)) {
                    this.mCurrentUserId = Integer.parseInt(getNextArgRequired());
                } else {
                    getErrPrintWriter().println("Unknown option: " + nextOption);
                    throw new IllegalArgumentException();
                }
            } else {
                this.mNew = getNextArg();
                return;
            }
        }
    }

    private void runChangeSp() {
        if (this.mNew != null) {
            if ("1".equals(this.mNew)) {
                this.mLockPatternUtils.enableSyntheticPassword();
                getOutPrintWriter().println("Synthetic password enabled");
            } else if ("0".equals(this.mNew)) {
                this.mLockPatternUtils.disableSyntheticPassword();
                getOutPrintWriter().println("Synthetic password disabled");
            }
        }
        getOutPrintWriter().println(String.format("SP Enabled = %b", Boolean.valueOf(this.mLockPatternUtils.isSyntheticPasswordEnabled())));
    }

    private void runSetPattern() throws RemoteException {
        this.mLockPatternUtils.saveLockPattern(LockPatternUtils.stringToPattern(this.mNew), this.mOld, this.mCurrentUserId);
        getOutPrintWriter().println("Pattern set to '" + this.mNew + "'");
    }

    private void runSetPassword() throws RemoteException {
        this.mLockPatternUtils.saveLockPassword(this.mNew, this.mOld, DumpState.DUMP_DOMAIN_PREFERRED, this.mCurrentUserId);
        getOutPrintWriter().println("Password set to '" + this.mNew + "'");
    }

    private void runSetPin() throws RemoteException {
        this.mLockPatternUtils.saveLockPassword(this.mNew, this.mOld, DumpState.DUMP_INTENT_FILTER_VERIFIERS, this.mCurrentUserId);
        getOutPrintWriter().println("Pin set to '" + this.mNew + "'");
    }

    private void runClear() throws RemoteException {
        this.mLockPatternUtils.clearLock(this.mOld, this.mCurrentUserId);
        getOutPrintWriter().println("Lock credential cleared");
    }

    private void runSetDisabled() throws RemoteException {
        boolean z = Boolean.parseBoolean(this.mNew);
        this.mLockPatternUtils.setLockScreenDisabled(z, this.mCurrentUserId);
        getOutPrintWriter().println("Lock screen disabled set to " + z);
    }

    private void runGetDisabled() {
        getOutPrintWriter().println(this.mLockPatternUtils.isLockScreenDisabled(this.mCurrentUserId));
    }

    private boolean checkCredential() throws RemoteException {
        boolean zCheckPattern;
        boolean zIsLockPasswordEnabled = this.mLockPatternUtils.isLockPasswordEnabled(this.mCurrentUserId);
        boolean zIsLockPatternEnabled = this.mLockPatternUtils.isLockPatternEnabled(this.mCurrentUserId);
        if (zIsLockPasswordEnabled || zIsLockPatternEnabled) {
            if (this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(this.mCurrentUserId)) {
                getOutPrintWriter().println("Profile uses unified challenge");
                return false;
            }
            try {
                if (zIsLockPasswordEnabled) {
                    zCheckPattern = this.mLockPatternUtils.checkPassword(this.mOld, this.mCurrentUserId);
                } else {
                    zCheckPattern = this.mLockPatternUtils.checkPattern(LockPatternUtils.stringToPattern(this.mOld), this.mCurrentUserId);
                }
                if (!zCheckPattern) {
                    if (!this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(this.mCurrentUserId)) {
                        this.mLockPatternUtils.reportFailedPasswordAttempt(this.mCurrentUserId);
                    }
                    getOutPrintWriter().println("Old password '" + this.mOld + "' didn't match");
                }
                return zCheckPattern;
            } catch (LockPatternUtils.RequestThrottledException e) {
                getOutPrintWriter().println("Request throttled");
                return false;
            }
        }
        return true;
    }
}
