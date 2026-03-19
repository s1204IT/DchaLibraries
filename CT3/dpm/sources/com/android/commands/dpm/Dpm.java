package com.android.commands.dpm;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;

public final class Dpm extends BaseCommand {
    private static final String COMMAND_REMOVE_ACTIVE_ADMIN = "remove-active-admin";
    private static final String COMMAND_SET_ACTIVE_ADMIN = "set-active-admin";
    private static final String COMMAND_SET_DEVICE_OWNER = "set-device-owner";
    private static final String COMMAND_SET_PROFILE_OWNER = "set-profile-owner";
    private IDevicePolicyManager mDevicePolicyManager;
    private int mUserId = 0;
    private String mName = "";
    private ComponentName mComponent = null;

    public static void main(String[] args) {
        new Dpm().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: dpm [subcommand] [options]\nusage: dpm set-active-admin [ --user <USER_ID> | current ] <COMPONENT>\nusage: dpm set-device-owner [ --user <USER_ID> | current *EXPERIMENTAL* ] [ --name <NAME> ] <COMPONENT>\nusage: dpm set-profile-owner [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>\nusage: dpm remove-active-admin [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>\n\ndpm set-active-admin: Sets the given component as active admin for an existing user.\n\ndpm set-device-owner: Sets the given component as active admin, and its package as device owner.\n\ndpm set-profile-owner: Sets the given component as active admin and profile owner for an existing user.\n\ndpm remove-active-admin: Disables an active admin, the admin must have declared android:testOnly in the application in its manifest. This will also remove device and profile owners\n");
    }

    public void onRun() throws Exception {
        this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (this.mDevicePolicyManager == null) {
            showError("Error: Could not access the Device Policy Manager. Is the system running?");
            return;
        }
        String command = nextArgRequired();
        if (command.equals(COMMAND_SET_ACTIVE_ADMIN)) {
            runSetActiveAdmin();
            return;
        }
        if (command.equals(COMMAND_SET_DEVICE_OWNER)) {
            runSetDeviceOwner();
        } else if (command.equals(COMMAND_SET_PROFILE_OWNER)) {
            runSetProfileOwner();
        } else {
            if (!command.equals(COMMAND_REMOVE_ACTIVE_ADMIN)) {
                throw new IllegalArgumentException("unknown command '" + command + "'");
            }
            runRemoveActiveAdmin();
        }
    }

    private void parseArgs(boolean canHaveName) {
        String opt;
        while (true) {
            opt = nextOption();
            if (opt != null) {
                if ("--user".equals(opt)) {
                    String arg = nextArgRequired();
                    if ("current".equals(arg) || "cur".equals(arg)) {
                        this.mUserId = -2;
                    } else {
                        this.mUserId = parseInt(arg);
                    }
                    if (this.mUserId == -2) {
                        IActivityManager activityManager = ActivityManagerNative.getDefault();
                        try {
                            this.mUserId = activityManager.getCurrentUser().id;
                        } catch (RemoteException e) {
                            e.rethrowAsRuntimeException();
                        }
                    }
                } else if (!canHaveName || !"--name".equals(opt)) {
                    break;
                } else {
                    this.mName = nextArgRequired();
                }
            } else {
                this.mComponent = parseComponentName(nextArgRequired());
                return;
            }
        }
        throw new IllegalArgumentException("Unknown option: " + opt);
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

    private ComponentName parseComponentName(String component) {
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn == null) {
            throw new IllegalArgumentException("Invalid component " + component);
        }
        return cn;
    }

    private int parseInt(String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer argument '" + argument + "'", e);
        }
    }
}
