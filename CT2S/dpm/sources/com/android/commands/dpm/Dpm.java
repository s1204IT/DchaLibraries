package com.android.commands.dpm;

import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.BaseCommand;
import java.io.PrintStream;

public final class Dpm extends BaseCommand {
    private static final String COMMAND_SET_ACTIVE_ADMIN = "set-active-admin";
    private static final String COMMAND_SET_DEVICE_OWNER = "set-device-owner";
    private static final String COMMAND_SET_PROFILE_OWNER = "set-profile-owner";
    private IDevicePolicyManager mDevicePolicyManager;
    private int mUserId = 0;
    private ComponentName mComponent = null;

    public static void main(String[] args) {
        new Dpm().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: dpm [subcommand] [options]\nusage: dpm set-active-admin [ --user <USER_ID> ] <COMPONENT>\nusage: dpm set-device-owner <COMPONENT>\nusage: dpm set-profile-owner <COMPONENT> <USER_ID>\n\ndpm set-active-admin: Sets the given component as active admin for an existing user.\n\ndpm set-device-owner: Sets the given component as active admin, and its\n  package as device owner.\n\ndpm set-profile-owner: Sets the given component as active admin and profile  owner for an existing user.\n");
    }

    public void onRun() throws Exception {
        String command;
        this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (this.mDevicePolicyManager == null) {
            showError("Error: Could not access the Device Policy Manager. Is the system running?");
            return;
        }
        command = nextArgRequired();
        switch (command) {
            case "set-active-admin":
                runSetActiveAdmin();
                return;
            case "set-device-owner":
                runSetDeviceOwner();
                return;
            case "set-profile-owner":
                runSetProfileOwner();
                return;
            default:
                throw new IllegalArgumentException("unknown command '" + command + "'");
        }
    }

    private void parseArgs(boolean canHaveUser) {
        String nextArg = nextArgRequired();
        if (canHaveUser && "--user".equals(nextArg)) {
            this.mUserId = parseInt(nextArgRequired());
            nextArg = nextArgRequired();
        }
        this.mComponent = parseComponentName(nextArg);
    }

    private void runSetActiveAdmin() throws RemoteException {
        parseArgs(true);
        this.mDevicePolicyManager.setActiveAdmin(this.mComponent, true, this.mUserId);
        System.out.println("Success: Active admin set to component " + this.mComponent.toShortString());
    }

    private void runSetDeviceOwner() throws Exception {
        ComponentName component = parseComponentName(nextArgRequired());
        this.mDevicePolicyManager.setActiveAdmin(component, true, 0);
        String packageName = component.getPackageName();
        try {
            if (!this.mDevicePolicyManager.setDeviceOwner(packageName, (String) null)) {
                throw new RuntimeException("Can't set package " + packageName + " as device owner.");
            }
            System.out.println("Success: Device owner set to package " + packageName);
            System.out.println("Active admin set to component " + component.toShortString());
        } catch (Exception e) {
            this.mDevicePolicyManager.removeActiveAdmin(component, 0);
            throw e;
        }
    }

    private void runSetProfileOwner() throws Exception {
        ComponentName component = parseComponentName(nextArgRequired());
        int userId = parseInt(nextArgRequired());
        this.mDevicePolicyManager.setActiveAdmin(component, true, userId);
        try {
            if (!this.mDevicePolicyManager.setProfileOwner(component, "", userId)) {
                throw new RuntimeException("Can't set component " + component.toShortString() + " as profile owner for user " + userId);
            }
            System.out.println("Success: Active admin and profile owner set to " + component.toShortString() + " for user " + userId);
        } catch (Exception e) {
            this.mDevicePolicyManager.removeActiveAdmin(component, userId);
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
