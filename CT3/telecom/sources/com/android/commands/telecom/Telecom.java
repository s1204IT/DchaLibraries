package com.android.commands.telecom;

import android.content.ComponentName;
import android.net.Uri;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import com.android.internal.os.BaseCommand;
import com.android.internal.telecom.ITelecomService;
import java.io.PrintStream;

public final class Telecom extends BaseCommand {
    private static final String COMMAND_GET_DEFAULT_DIALER = "get-default-dialer";
    private static final String COMMAND_GET_SYSTEM_DIALER = "get-system-dialer";
    private static final String COMMAND_REGISTER_PHONE_ACCOUNT = "register-phone-account";
    private static final String COMMAND_REGISTER_SIM_PHONE_ACCOUNT = "register-sim-phone-account";
    private static final String COMMAND_SET_DEFAULT_DIALER = "set-default-dialer";
    private static final String COMMAND_SET_PHONE_ACCOUNT_DISABLED = "set-phone-account-disabled";
    private static final String COMMAND_SET_PHONE_ACCOUNT_ENABLED = "set-phone-account-enabled";
    private static final String COMMAND_UNREGISTER_PHONE_ACCOUNT = "unregister-phone-account";
    private String mAccountId;
    private ComponentName mComponent;
    private ITelecomService mTelecomService;
    private IUserManager mUserManager;

    public static void main(String[] args) {
        new Telecom().run(args);
    }

    public void onShowUsage(PrintStream out) {
        out.println("usage: telecom [subcommand] [options]\nusage: telecom set-phone-account-enabled <COMPONENT> <ID> <USER_SN>\nusage: telecom set-phone-account-disabled <COMPONENT> <ID> <USER_SN>\nusage: telecom register-phone-account <COMPONENT> <ID> <USER_SN> <LABEL>\nusage: telecom register-sim-phone-account <COMPONENT> <ID> <USER_SN> <LABEL> <ADDRESS>\nusage: telecom unregister-phone-account <COMPONENT> <ID> <USER_SN>\nusage: telecom set-default-dialer <PACKAGE>\nusage: telecom get-default-dialer\nusage: telecom get-system-dialer\n\ntelecom set-phone-account-enabled: Enables the given phone account, if it has \n already been registered with Telecom.\n\ntelecom set-phone-account-disabled: Disables the given phone account, if it \n has already been registered with telecom.\n\ntelecom set-default-dialer: Sets the default dialer to the given component. \n\ntelecom get-default-dialer: Displays the current default dialer. \n\ntelecom get-system-dialer: Displays the current system dialer. \n");
    }

    public void onRun() throws Exception {
        this.mTelecomService = ITelecomService.Stub.asInterface(ServiceManager.getService("telecom"));
        if (this.mTelecomService == null) {
            showError("Error: Could not access the Telecom Manager. Is the system running?");
            return;
        }
        this.mUserManager = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
        if (this.mUserManager == null) {
            showError("Error: Could not access the User Manager. Is the system running?");
            return;
        }
        String command = nextArgRequired();
        if (command.equals(COMMAND_SET_PHONE_ACCOUNT_ENABLED)) {
            runSetPhoneAccountEnabled(true);
            return;
        }
        if (command.equals(COMMAND_SET_PHONE_ACCOUNT_DISABLED)) {
            runSetPhoneAccountEnabled(false);
            return;
        }
        if (command.equals(COMMAND_REGISTER_PHONE_ACCOUNT)) {
            runRegisterPhoneAccount();
            return;
        }
        if (command.equals(COMMAND_REGISTER_SIM_PHONE_ACCOUNT)) {
            runRegisterSimPhoneAccount();
            return;
        }
        if (command.equals(COMMAND_UNREGISTER_PHONE_ACCOUNT)) {
            runUnregisterPhoneAccount();
            return;
        }
        if (command.equals(COMMAND_SET_DEFAULT_DIALER)) {
            runSetDefaultDialer();
        } else if (command.equals(COMMAND_GET_DEFAULT_DIALER)) {
            runGetDefaultDialer();
        } else {
            if (!command.equals(COMMAND_GET_SYSTEM_DIALER)) {
                throw new IllegalArgumentException("unknown command '" + command + "'");
            }
            runGetSystemDialer();
        }
    }

    private void runSetPhoneAccountEnabled(boolean enabled) throws RemoteException {
        PhoneAccountHandle handle = getPhoneAccountHandleFromArgs();
        boolean success = this.mTelecomService.enablePhoneAccount(handle, enabled);
        if (success) {
            System.out.println("Success - " + handle + (enabled ? " enabled." : " disabled."));
        } else {
            System.out.println("Error - is " + handle + " a valid PhoneAccount?");
        }
    }

    private void runRegisterPhoneAccount() throws RemoteException {
        PhoneAccountHandle handle = getPhoneAccountHandleFromArgs();
        String label = nextArgRequired();
        PhoneAccount account = PhoneAccount.builder(handle, label).setCapabilities(2).build();
        this.mTelecomService.registerPhoneAccount(account);
        System.out.println("Success - " + handle + " registered.");
    }

    private void runRegisterSimPhoneAccount() throws RemoteException {
        PhoneAccountHandle handle = getPhoneAccountHandleFromArgs();
        String label = nextArgRequired();
        String address = nextArgRequired();
        PhoneAccount account = PhoneAccount.builder(handle, label).setAddress(Uri.parse(address)).setSubscriptionAddress(Uri.parse(address)).setCapabilities(6).setShortDescription(label).addSupportedUriScheme("tel").addSupportedUriScheme("voicemail").build();
        this.mTelecomService.registerPhoneAccount(account);
        System.out.println("Success - " + handle + " registered.");
    }

    private void runUnregisterPhoneAccount() throws RemoteException {
        PhoneAccountHandle handle = getPhoneAccountHandleFromArgs();
        this.mTelecomService.unregisterPhoneAccount(handle);
        System.out.println("Success - " + handle + " unregistered.");
    }

    private void runSetDefaultDialer() throws RemoteException {
        String packageName = nextArgRequired();
        boolean success = this.mTelecomService.setDefaultDialer(packageName);
        if (success) {
            System.out.println("Success - " + packageName + " set as default dialer.");
        } else {
            System.out.println("Error - " + packageName + " is not an installed Dialer app, \n or is already the default dialer.");
        }
    }

    private void runGetDefaultDialer() throws RemoteException {
        System.out.println(this.mTelecomService.getDefaultDialerPackage());
    }

    private void runGetSystemDialer() throws RemoteException {
        System.out.println(this.mTelecomService.getSystemDialerPackage());
    }

    private PhoneAccountHandle getPhoneAccountHandleFromArgs() throws RemoteException {
        ComponentName component = parseComponentName(nextArgRequired());
        String accountId = nextArgRequired();
        String userSnInStr = nextArgRequired();
        try {
            int userSn = Integer.parseInt(userSnInStr);
            UserHandle userHandle = UserHandle.of(this.mUserManager.getUserHandle(userSn));
            return new PhoneAccountHandle(component, accountId, userHandle);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user serial number " + userSnInStr);
        }
    }

    private ComponentName parseComponentName(String component) {
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn == null) {
            throw new IllegalArgumentException("Invalid component " + component);
        }
        return cn;
    }
}
