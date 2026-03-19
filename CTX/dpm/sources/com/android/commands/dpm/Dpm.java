package com.android.commands.dpm;

import android.app.ActivityManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;

public final class Dpm extends BaseCommand {
    private static final String COMMAND_CLEAR_FREEZE_PERIOD_RECORD = "clear-freeze-period-record";
    private static final String COMMAND_FORCE_SECURITY_LOGS = "force-security-logs";
    private static final String COMMAND_REMOVE_ACTIVE_ADMIN = "remove-active-admin";
    private static final String COMMAND_SET_ACTIVE_ADMIN = "set-active-admin";
    private static final String COMMAND_SET_DEVICE_OWNER = "set-device-owner";
    private static final String COMMAND_SET_PROFILE_OWNER = "set-profile-owner";
    private IDevicePolicyManager mDevicePolicyManager;
    private int mUserId = 0;
    private String mName = "";
    private ComponentName mComponent = null;

    public static void main(String[] strArr) {
        new Dpm().run(strArr);
    }

    public void onShowUsage(PrintStream printStream) {
        printStream.println("usage: dpm [subcommand] [options]\nusage: dpm set-active-admin [ --user <USER_ID> | current ] <COMPONENT>\nusage: dpm set-device-owner [ --user <USER_ID> | current *EXPERIMENTAL* ] [ --name <NAME> ] <COMPONENT>\nusage: dpm set-profile-owner [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>\nusage: dpm remove-active-admin [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>\n\ndpm set-active-admin: Sets the given component as active admin for an existing user.\n\ndpm set-device-owner: Sets the given component as active admin, and its package as device owner.\n\ndpm set-profile-owner: Sets the given component as active admin and profile owner for an existing user.\n\ndpm remove-active-admin: Disables an active admin, the admin must have declared android:testOnly in the application in its manifest. This will also remove device and profile owners.\n\ndpm clear-freeze-period-record: clears framework-maintained record of past freeze periods that the device went through. For use during feature development to prevent triggering restriction on setting freeze periods.\n\ndpm force-security-logs: makes all security logs available to the DPC and triggers DeviceAdminReceiver.onSecurityLogsAvailable() if needed.");
    }

    public void onRun() throws Exception {
        String strNextArgRequired;
        this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (this.mDevicePolicyManager == null) {
            showError("Error: Could not access the Device Policy Manager. Is the system running?");
            return;
        }
        strNextArgRequired = nextArgRequired();
        switch (strNextArgRequired) {
            case "set-active-admin":
                runSetActiveAdmin();
                return;
            case "set-device-owner":
                runSetDeviceOwner();
                return;
            case "set-profile-owner":
                runSetProfileOwner();
                return;
            case "remove-active-admin":
                runRemoveActiveAdmin();
                return;
            case "clear-freeze-period-record":
                runClearFreezePeriodRecord();
                return;
            case "force-security-logs":
                runForceSecurityLogs();
                return;
            default:
                throw new IllegalArgumentException("unknown command '" + strNextArgRequired + "'");
        }
    }

    private void runForceSecurityLogs() throws InterruptedException, RemoteException {
        while (true) {
            long jForceSecurityLogs = this.mDevicePolicyManager.forceSecurityLogs();
            if (jForceSecurityLogs != 0) {
                System.out.println("We have to wait for " + jForceSecurityLogs + " milliseconds...");
                Thread.sleep(jForceSecurityLogs);
            } else {
                System.out.println("Success");
                return;
            }
        }
    }

    private void parseArgs(boolean z) {
        String strNextOption;
        while (true) {
            strNextOption = nextOption();
            if (strNextOption != null) {
                if ("--user".equals(strNextOption)) {
                    String strNextArgRequired = nextArgRequired();
                    if ("current".equals(strNextArgRequired) || "cur".equals(strNextArgRequired)) {
                        this.mUserId = -2;
                    } else {
                        this.mUserId = parseInt(strNextArgRequired);
                    }
                    if (this.mUserId == -2) {
                        try {
                            this.mUserId = ActivityManager.getService().getCurrentUser().id;
                        } catch (RemoteException e) {
                            e.rethrowAsRuntimeException();
                        }
                    }
                } else if (!z || !"--name".equals(strNextOption)) {
                    break;
                } else {
                    this.mName = nextArgRequired();
                }
            } else {
                this.mComponent = parseComponentName(nextArgRequired());
                return;
            }
        }
        throw new IllegalArgumentException("Unknown option: " + strNextOption);
    }

    private void runSetActiveAdmin() throws RemoteException {
        parseArgs(false);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        System.out.println("Success: Active admin set to component " + this.mComponent.toShortString());
    }

    private void runSetDeviceOwner() throws Exception {
        parseArgs(true);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        try {
            if (!this.mDevicePolicyManager.setDeviceOwner(this.mComponent, this.mName, this.mUserId)) {
                throw new RuntimeException("Can't set package " + this.mComponent + " as device owner.");
            }
            this.mDevicePolicyManager.setUserProvisioningState(3, this.mUserId);
            System.out.println("Success: Device owner set to package " + this.mComponent);
            System.out.println("Active admin set to component " + this.mComponent.toShortString());
        } catch (Exception e) {
            this.mDevicePolicyManager.removeActiveAdmin(this.mComponent, 0);
            throw e;
        }
    }

    private void runRemoveActiveAdmin() throws RemoteException {
        parseArgs(false);
        this.mDevicePolicyManager.forceRemoveActiveAdmin(this.mComponent, this.mUserId);
        System.out.println("Success: Admin removed " + this.mComponent);
    }

    private void runSetProfileOwner() throws Exception {
        parseArgs(true);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        try {
            if (!this.mDevicePolicyManager.setProfileOwner(this.mComponent, this.mName, this.mUserId)) {
                throw new RuntimeException("Can't set component " + this.mComponent.toShortString() + " as profile owner for user " + this.mUserId);
            }
            this.mDevicePolicyManager.setUserProvisioningState(3, this.mUserId);
            System.out.println("Success: Active admin and profile owner set to " + this.mComponent.toShortString() + " for user " + this.mUserId);
        } catch (Exception e) {
            this.mDevicePolicyManager.removeActiveAdmin(this.mComponent, this.mUserId);
            throw e;
        }
    }

    private void runClearFreezePeriodRecord() throws RemoteException {
        this.mDevicePolicyManager.clearSystemUpdatePolicyFreezePeriodRecord();
        System.out.println("Success");
    }

    private ComponentName parseComponentName(String str) {
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str);
        if (componentNameUnflattenFromString == null) {
            throw new IllegalArgumentException("Invalid component " + str);
        }
        return componentNameUnflattenFromString;
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer argument '" + str + "'", e);
        }
    }
}
