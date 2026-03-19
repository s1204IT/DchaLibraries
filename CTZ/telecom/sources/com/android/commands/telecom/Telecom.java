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
    private static final String COMMAND_WAIT_ON_HANDLERS = "wait-on-handlers";
    private String mAccountId;
    private ComponentName mComponent;
    private ITelecomService mTelecomService;
    private IUserManager mUserManager;

    public static void main(String[] strArr) {
        new Telecom().run(strArr);
    }

    public void onShowUsage(PrintStream printStream) {
        printStream.println("usage: telecom [subcommand] [options]\nusage: telecom set-phone-account-enabled <COMPONENT> <ID> <USER_SN>\nusage: telecom set-phone-account-disabled <COMPONENT> <ID> <USER_SN>\nusage: telecom register-phone-account <COMPONENT> <ID> <USER_SN> <LABEL>\nusage: telecom register-sim-phone-account <COMPONENT> <ID> <USER_SN> <LABEL> <ADDRESS>\nusage: telecom unregister-phone-account <COMPONENT> <ID> <USER_SN>\nusage: telecom set-default-dialer <PACKAGE>\nusage: telecom get-default-dialer\nusage: telecom get-system-dialer\nusage: telecom wait-on-handlers\n\ntelecom set-phone-account-enabled: Enables the given phone account, if it has \n already been registered with Telecom.\n\ntelecom set-phone-account-disabled: Disables the given phone account, if it \n has already been registered with telecom.\n\ntelecom set-default-dialer: Sets the default dialer to the given component. \n\ntelecom get-default-dialer: Displays the current default dialer. \n\ntelecom get-system-dialer: Displays the current system dialer. \n\ntelecom wait-on-handlers: Wait until all handlers finish their work. \n");
    }

    public void onRun() throws Exception {
        String strNextArgRequired;
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
        strNextArgRequired = nextArgRequired();
        switch (strNextArgRequired) {
            case "set-phone-account-enabled":
                runSetPhoneAccountEnabled(true);
                return;
            case "set-phone-account-disabled":
                runSetPhoneAccountEnabled(false);
                return;
            case "register-phone-account":
                runRegisterPhoneAccount();
                return;
            case "register-sim-phone-account":
                runRegisterSimPhoneAccount();
                return;
            case "unregister-phone-account":
                runUnregisterPhoneAccount();
                return;
            case "set-default-dialer":
                runSetDefaultDialer();
                return;
            case "get-default-dialer":
                runGetDefaultDialer();
                return;
            case "get-system-dialer":
                runGetSystemDialer();
                return;
            case "wait-on-handlers":
                runWaitOnHandler();
                return;
            default:
                throw new IllegalArgumentException("unknown command '" + strNextArgRequired + "'");
        }
    }

    private void runSetPhoneAccountEnabled(boolean z) throws RemoteException {
        PhoneAccountHandle phoneAccountHandleFromArgs = getPhoneAccountHandleFromArgs();
        if (this.mTelecomService.enablePhoneAccount(phoneAccountHandleFromArgs, z)) {
            PrintStream printStream = System.out;
            StringBuilder sb = new StringBuilder();
            sb.append("Success - ");
            sb.append(phoneAccountHandleFromArgs);
            sb.append(z ? " enabled." : " disabled.");
            printStream.println(sb.toString());
            return;
        }
        System.out.println("Error - is " + phoneAccountHandleFromArgs + " a valid PhoneAccount?");
    }

    private void runRegisterPhoneAccount() throws RemoteException {
        PhoneAccountHandle phoneAccountHandleFromArgs = getPhoneAccountHandleFromArgs();
        this.mTelecomService.registerPhoneAccount(PhoneAccount.builder(phoneAccountHandleFromArgs, nextArgRequired()).setCapabilities(2).build());
        System.out.println("Success - " + phoneAccountHandleFromArgs + " registered.");
    }

    private void runRegisterSimPhoneAccount() throws RemoteException {
        PhoneAccountHandle phoneAccountHandleFromArgs = getPhoneAccountHandleFromArgs();
        String strNextArgRequired = nextArgRequired();
        String strNextArgRequired2 = nextArgRequired();
        this.mTelecomService.registerPhoneAccount(PhoneAccount.builder(phoneAccountHandleFromArgs, strNextArgRequired).setAddress(Uri.parse(strNextArgRequired2)).setSubscriptionAddress(Uri.parse(strNextArgRequired2)).setCapabilities(6).setShortDescription(strNextArgRequired).addSupportedUriScheme("tel").addSupportedUriScheme("voicemail").build());
        System.out.println("Success - " + phoneAccountHandleFromArgs + " registered.");
    }

    private void runUnregisterPhoneAccount() throws RemoteException {
        PhoneAccountHandle phoneAccountHandleFromArgs = getPhoneAccountHandleFromArgs();
        this.mTelecomService.unregisterPhoneAccount(phoneAccountHandleFromArgs);
        System.out.println("Success - " + phoneAccountHandleFromArgs + " unregistered.");
    }

    private void runSetDefaultDialer() throws RemoteException {
        String strNextArgRequired = nextArgRequired();
        if (this.mTelecomService.setDefaultDialer(strNextArgRequired)) {
            System.out.println("Success - " + strNextArgRequired + " set as default dialer.");
            return;
        }
        System.out.println("Error - " + strNextArgRequired + " is not an installed Dialer app, \n or is already the default dialer.");
    }

    private void runGetDefaultDialer() throws RemoteException {
        System.out.println(this.mTelecomService.getDefaultDialerPackage());
    }

    private void runGetSystemDialer() throws RemoteException {
        System.out.println(this.mTelecomService.getSystemDialerPackage());
    }

    private void runWaitOnHandler() throws RemoteException {
    }

    private PhoneAccountHandle getPhoneAccountHandleFromArgs() throws RemoteException {
        ComponentName componentName = parseComponentName(nextArgRequired());
        String strNextArgRequired = nextArgRequired();
        String strNextArgRequired2 = nextArgRequired();
        try {
            return new PhoneAccountHandle(componentName, strNextArgRequired, UserHandle.of(this.mUserManager.getUserHandle(Integer.parseInt(strNextArgRequired2))));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user serial number " + strNextArgRequired2);
        }
    }

    private ComponentName parseComponentName(String str) {
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str);
        if (componentNameUnflattenFromString == null) {
            throw new IllegalArgumentException("Invalid component " + str);
        }
        return componentNameUnflattenFromString;
    }
}
