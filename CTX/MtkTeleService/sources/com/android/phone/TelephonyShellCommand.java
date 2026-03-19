package com.android.phone;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;
import java.io.PrintWriter;

public class TelephonyShellCommand extends ShellCommand {
    private static final int DEFAULT_PHONE_ID = 0;
    private static final String IMS_DISABLE = "disable";
    private static final String IMS_ENABLE = "enable";
    private static final String IMS_GET_CARRIER_SERVICE = "get-ims-service";
    private static final String IMS_SET_CARRIER_SERVICE = "set-ims-service";
    private static final String IMS_SUBCOMMAND = "ims";
    private static final String LOG_TAG = "TelephonyShellCommand";
    private static final boolean VDBG = true;
    private final ITelephony mInterface;

    public TelephonyShellCommand(ITelephony iTelephony) {
        this.mInterface = iTelephony;
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(null);
        }
        byte b = -1;
        if (str.hashCode() == 104399 && str.equals(IMS_SUBCOMMAND)) {
            b = 0;
        }
        if (b == 0) {
            return handleImsCommand();
        }
        return handleDefaultCommands(str);
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Telephony Commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println("  ims");
        outPrintWriter.println("    IMS Commands.");
        onHelpIms();
    }

    private void onHelpIms() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("IMS Commands:");
        outPrintWriter.println("  ims set-ims-service [-s SLOT_ID] (-c | -d) PACKAGE_NAME");
        outPrintWriter.println("    Sets the ImsService defined in PACKAGE_NAME to to be the bound");
        outPrintWriter.println("    ImsService. Options are:");
        outPrintWriter.println("      -s: the slot ID that the ImsService should be bound for. If no option");
        outPrintWriter.println("          is specified, it will choose the default voice SIM slot.");
        outPrintWriter.println("      -c: Override the ImsService defined in the carrier configuration.");
        outPrintWriter.println("      -d: Override the ImsService defined in the device overlay.");
        outPrintWriter.println("  ims get-ims-service [-s SLOT_ID] [-c | -d]");
        outPrintWriter.println("    Gets the package name of the currently defined ImsService.");
        outPrintWriter.println("    Options are:");
        outPrintWriter.println("      -s: The SIM slot ID for the registered ImsService. If no option");
        outPrintWriter.println("          is specified, it will choose the default voice SIM slot.");
        outPrintWriter.println("      -c: The ImsService defined as the carrier configured ImsService.");
        outPrintWriter.println("      -c: The ImsService defined as the device default ImsService.");
        outPrintWriter.println("  ims enable [-s SLOT_ID]");
        outPrintWriter.println("    enables IMS for the SIM slot specified, or for the default voice SIM slot");
        outPrintWriter.println("    if none is specified.");
        outPrintWriter.println("  ims disable [-s SLOT_ID]");
        outPrintWriter.println("    disables IMS for the SIM slot specified, or for the default voice SIM");
        outPrintWriter.println("    slot if none is specified.");
    }

    private int handleImsCommand() {
        String nextArg = getNextArg();
        byte b = 0;
        if (nextArg == null) {
            onHelpIms();
            return 0;
        }
        int iHashCode = nextArg.hashCode();
        if (iHashCode != -1298848381) {
            if (iHashCode != 436884160) {
                if (iHashCode != 1347636684) {
                    b = (iHashCode == 1671308008 && nextArg.equals(IMS_DISABLE)) ? (byte) 3 : (byte) -1;
                } else if (!nextArg.equals(IMS_SET_CARRIER_SERVICE)) {
                }
            } else if (nextArg.equals(IMS_GET_CARRIER_SERVICE)) {
                b = 1;
            }
        } else if (nextArg.equals(IMS_ENABLE)) {
            b = 2;
        }
        switch (b) {
            case 0:
                return handleImsSetServiceCommand();
            case 1:
                return handleImsGetServiceCommand();
            case 2:
                return handleEnableIms();
            case 3:
                return handleDisableIms();
            default:
                return -1;
        }
    }

    private int handleImsSetServiceCommand() {
        byte b;
        PrintWriter errPrintWriter = getErrPrintWriter();
        int defaultSlot = getDefaultSlot();
        Boolean boolValueOf = null;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1510) {
                    switch (iHashCode) {
                        case 1494:
                            b = !nextOption.equals("-c") ? (byte) -1 : (byte) 1;
                            break;
                        case 1495:
                            b = !nextOption.equals("-d") ? (byte) -1 : (byte) 2;
                            break;
                        default:
                            b = -1;
                            break;
                    }
                } else {
                    b = nextOption.equals("-s") ? (byte) 0 : (byte) -1;
                }
                switch (b) {
                    case 0:
                        try {
                            defaultSlot = Integer.parseInt(getNextArgRequired());
                        } catch (NumberFormatException e) {
                            errPrintWriter.println("ims set-ims-service requires an integer as a SLOT_ID.");
                            return -1;
                        }
                        break;
                    case 1:
                        boolValueOf = Boolean.valueOf(VDBG);
                        break;
                    case 2:
                        boolValueOf = false;
                        break;
                }
            } else {
                if (boolValueOf == null) {
                    errPrintWriter.println("ims set-ims-service requires either \"-c\" or \"-d\" to be set.");
                    return -1;
                }
                String nextArg = getNextArg();
                if (nextArg == null) {
                    nextArg = "";
                }
                try {
                    boolean imsService = this.mInterface.setImsService(defaultSlot, boolValueOf.booleanValue(), nextArg);
                    StringBuilder sb = new StringBuilder();
                    sb.append("ims set-ims-service -s ");
                    sb.append(defaultSlot);
                    sb.append(" ");
                    sb.append(boolValueOf.booleanValue() ? "-c " : "-d ");
                    sb.append(nextArg);
                    sb.append(", result=");
                    sb.append(imsService);
                    Log.v(LOG_TAG, sb.toString());
                    getOutPrintWriter().println(imsService);
                    return 0;
                } catch (RemoteException e2) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("ims set-ims-service -s ");
                    sb2.append(defaultSlot);
                    sb2.append(" ");
                    sb2.append(boolValueOf.booleanValue() ? "-c " : "-d ");
                    sb2.append(nextArg);
                    sb2.append(", error");
                    sb2.append(e2.getMessage());
                    Log.w(LOG_TAG, sb2.toString());
                    errPrintWriter.println("Exception: " + e2.getMessage());
                    return -1;
                }
            }
        }
    }

    private int handleImsGetServiceCommand() {
        byte b;
        PrintWriter errPrintWriter = getErrPrintWriter();
        int defaultSlot = getDefaultSlot();
        Boolean boolValueOf = null;
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1510) {
                    switch (iHashCode) {
                        case 1494:
                            b = !nextOption.equals("-c") ? (byte) -1 : (byte) 1;
                            break;
                        case 1495:
                            b = !nextOption.equals("-d") ? (byte) -1 : (byte) 2;
                            break;
                        default:
                            b = -1;
                            break;
                    }
                } else {
                    b = nextOption.equals("-s") ? (byte) 0 : (byte) -1;
                }
                switch (b) {
                    case 0:
                        try {
                            defaultSlot = Integer.parseInt(getNextArgRequired());
                        } catch (NumberFormatException e) {
                            errPrintWriter.println("ims set-ims-service requires an integer as a SLOT_ID.");
                            return -1;
                        }
                        break;
                    case 1:
                        boolValueOf = Boolean.valueOf(VDBG);
                        break;
                    case 2:
                        boolValueOf = false;
                        break;
                }
            } else {
                if (boolValueOf == null) {
                    errPrintWriter.println("ims set-ims-service requires either \"-c\" or \"-d\" to be set.");
                    return -1;
                }
                try {
                    String imsService = this.mInterface.getImsService(defaultSlot, boolValueOf.booleanValue());
                    StringBuilder sb = new StringBuilder();
                    sb.append("ims get-ims-service -s ");
                    sb.append(defaultSlot);
                    sb.append(" ");
                    sb.append(boolValueOf.booleanValue() ? "-c " : "-d ");
                    sb.append(", returned: ");
                    sb.append(imsService);
                    Log.v(LOG_TAG, sb.toString());
                    getOutPrintWriter().println(imsService);
                    return 0;
                } catch (RemoteException e2) {
                    return -1;
                }
            }
        }
    }

    private int handleEnableIms() {
        int defaultSlot = getDefaultSlot();
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (((nextOption.hashCode() == 1510 && nextOption.equals("-s")) ? (byte) 0 : (byte) -1) == 0) {
                    try {
                        defaultSlot = Integer.parseInt(getNextArgRequired());
                    } catch (NumberFormatException e) {
                        getErrPrintWriter().println("ims enable requires an integer as a SLOT_ID.");
                        return -1;
                    }
                }
            } else {
                try {
                    this.mInterface.enableIms(defaultSlot);
                    Log.v(LOG_TAG, "ims enable -s " + defaultSlot);
                    return 0;
                } catch (RemoteException e2) {
                    return -1;
                }
            }
        }
    }

    private int handleDisableIms() {
        int defaultSlot = getDefaultSlot();
        while (true) {
            String nextOption = getNextOption();
            if (nextOption != null) {
                if (((nextOption.hashCode() == 1510 && nextOption.equals("-s")) ? (byte) 0 : (byte) -1) == 0) {
                    try {
                        defaultSlot = Integer.parseInt(getNextArgRequired());
                    } catch (NumberFormatException e) {
                        getErrPrintWriter().println("ims disable requires an integer as a SLOT_ID.");
                        return -1;
                    }
                }
            } else {
                try {
                    this.mInterface.disableIms(defaultSlot);
                    Log.v(LOG_TAG, "ims disable -s " + defaultSlot);
                    return 0;
                } catch (RemoteException e2) {
                    return -1;
                }
            }
        }
    }

    private int getDefaultSlot() {
        int defaultVoicePhoneId = SubscriptionManager.getDefaultVoicePhoneId();
        if (defaultVoicePhoneId <= -1 || defaultVoicePhoneId == Integer.MAX_VALUE) {
            return 0;
        }
        return defaultVoicePhoneId;
    }
}
