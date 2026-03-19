package com.android.server.net;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;

class NetworkPolicyManagerShellCommand extends ShellCommand {
    private final NetworkPolicyManagerService mInterface;
    private final WifiManager mWifiManager;

    NetworkPolicyManagerShellCommand(Context context, NetworkPolicyManagerService networkPolicyManagerService) {
        this.mInterface = networkPolicyManagerService;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    public int onCommand(String str) {
        if (str == null) {
            return handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = getOutPrintWriter();
        try {
            switch (str) {
                case "get":
                    return runGet();
                case "set":
                    return runSet();
                case "list":
                    return runList();
                case "add":
                    return runAdd();
                case "remove":
                    return runRemove();
                default:
                    return handleDefaultCommands(str);
            }
        } catch (RemoteException e) {
            outPrintWriter.println("Remote exception: " + e);
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.println("Network policy manager (netpolicy) commands:");
        outPrintWriter.println("  help");
        outPrintWriter.println("    Print this help text.");
        outPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        outPrintWriter.println("  add restrict-background-whitelist UID");
        outPrintWriter.println("    Adds a UID to the whitelist for restrict background usage.");
        outPrintWriter.println("  add restrict-background-blacklist UID");
        outPrintWriter.println("    Adds a UID to the blacklist for restrict background usage.");
        outPrintWriter.println("  get restrict-background");
        outPrintWriter.println("    Gets the global restrict background usage status.");
        outPrintWriter.println("  list wifi-networks [true|false]");
        outPrintWriter.println("    Lists all saved wifi networks and whether they are metered or not.");
        outPrintWriter.println("    If a boolean argument is passed, filters just the metered (or unmetered)");
        outPrintWriter.println("    networks.");
        outPrintWriter.println("  list restrict-background-whitelist");
        outPrintWriter.println("    Lists UIDs that are whitelisted for restrict background usage.");
        outPrintWriter.println("  list restrict-background-blacklist");
        outPrintWriter.println("    Lists UIDs that are blacklisted for restrict background usage.");
        outPrintWriter.println("  remove restrict-background-whitelist UID");
        outPrintWriter.println("    Removes a UID from the whitelist for restrict background usage.");
        outPrintWriter.println("  remove restrict-background-blacklist UID");
        outPrintWriter.println("    Removes a UID from the blacklist for restrict background usage.");
        outPrintWriter.println("  set metered-network ID [undefined|true|false]");
        outPrintWriter.println("    Toggles whether the given wi-fi network is metered.");
        outPrintWriter.println("  set restrict-background BOOLEAN");
        outPrintWriter.println("    Sets the global restrict background usage status.");
        outPrintWriter.println("  set sub-plan-owner subId [packageName]");
        outPrintWriter.println("    Sets the data plan owner package for subId.");
    }

    private int runGet() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify type of data to get");
            return -1;
        }
        if (((nextArg.hashCode() == -747095841 && nextArg.equals("restrict-background")) ? (byte) 0 : (byte) -1) == 0) {
            return getRestrictBackground();
        }
        outPrintWriter.println("Error: unknown get type '" + nextArg + "'");
        return -1;
    }

    private int runSet() throws RemoteException {
        byte b;
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify type of data to set");
            return -1;
        }
        int iHashCode = nextArg.hashCode();
        if (iHashCode != -983249079) {
            if (iHashCode != -747095841) {
                b = (iHashCode == 1846940860 && nextArg.equals("sub-plan-owner")) ? (byte) 2 : (byte) -1;
            } else if (nextArg.equals("restrict-background")) {
                b = 1;
            }
        } else if (nextArg.equals("metered-network")) {
            b = 0;
        }
        switch (b) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                outPrintWriter.println("Error: unknown set type '" + nextArg + "'");
                break;
        }
        return -1;
    }

    private int runList() throws RemoteException {
        byte b;
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify type of data to list");
            return -1;
        }
        int iHashCode = nextArg.hashCode();
        if (iHashCode != -668534353) {
            if (iHashCode != -363534403) {
                b = (iHashCode == 639570137 && nextArg.equals("restrict-background-whitelist")) ? (byte) 1 : (byte) -1;
            } else if (nextArg.equals("wifi-networks")) {
                b = 0;
            }
        } else if (nextArg.equals("restrict-background-blacklist")) {
            b = 2;
        }
        switch (b) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                outPrintWriter.println("Error: unknown list type '" + nextArg + "'");
                break;
        }
        return -1;
    }

    private int runAdd() throws RemoteException {
        byte b;
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify type of data to add");
            return -1;
        }
        int iHashCode = nextArg.hashCode();
        if (iHashCode != -668534353) {
            b = (iHashCode == 639570137 && nextArg.equals("restrict-background-whitelist")) ? (byte) 0 : (byte) -1;
        } else if (nextArg.equals("restrict-background-blacklist")) {
            b = 1;
        }
        switch (b) {
            case 0:
                break;
            case 1:
                break;
            default:
                outPrintWriter.println("Error: unknown add type '" + nextArg + "'");
                break;
        }
        return -1;
    }

    private int runRemove() throws RemoteException {
        byte b;
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify type of data to remove");
            return -1;
        }
        int iHashCode = nextArg.hashCode();
        if (iHashCode != -668534353) {
            b = (iHashCode == 639570137 && nextArg.equals("restrict-background-whitelist")) ? (byte) 0 : (byte) -1;
        } else if (nextArg.equals("restrict-background-blacklist")) {
            b = 1;
        }
        switch (b) {
            case 0:
                break;
            case 1:
                break;
            default:
                outPrintWriter.println("Error: unknown remove type '" + nextArg + "'");
                break;
        }
        return -1;
    }

    private int listUidPolicies(String str, int i) throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        int[] uidsWithPolicy = this.mInterface.getUidsWithPolicy(i);
        outPrintWriter.print(str);
        outPrintWriter.print(": ");
        if (uidsWithPolicy.length == 0) {
            outPrintWriter.println("none");
        } else {
            for (int i2 : uidsWithPolicy) {
                outPrintWriter.print(i2);
                outPrintWriter.print(' ');
            }
        }
        outPrintWriter.println();
        return 0;
    }

    private int listRestrictBackgroundWhitelist() throws RemoteException {
        return listUidPolicies("Restrict background whitelisted UIDs", 4);
    }

    private int listRestrictBackgroundBlacklist() throws RemoteException {
        return listUidPolicies("Restrict background blacklisted UIDs", 1);
    }

    private int getRestrictBackground() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        outPrintWriter.print("Restrict background status: ");
        outPrintWriter.println(this.mInterface.getRestrictBackground() ? "enabled" : "disabled");
        return 0;
    }

    private int setRestrictBackground() throws RemoteException {
        int nextBooleanArg = getNextBooleanArg();
        if (nextBooleanArg < 0) {
            return nextBooleanArg;
        }
        this.mInterface.setRestrictBackground(nextBooleanArg > 0);
        return 0;
    }

    private int setSubPlanOwner() throws RemoteException {
        this.mInterface.setSubscriptionPlansOwner(Integer.parseInt(getNextArgRequired()), getNextArg());
        return 0;
    }

    private int setUidPolicy(int i) throws RemoteException {
        int uidFromNextArg = getUidFromNextArg();
        if (uidFromNextArg < 0) {
            return uidFromNextArg;
        }
        this.mInterface.setUidPolicy(uidFromNextArg, i);
        return 0;
    }

    private int resetUidPolicy(String str, int i) throws RemoteException {
        int uidFromNextArg = getUidFromNextArg();
        if (uidFromNextArg < 0) {
            return uidFromNextArg;
        }
        if (this.mInterface.getUidPolicy(uidFromNextArg) != i) {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.print("Error: UID ");
            outPrintWriter.print(uidFromNextArg);
            outPrintWriter.print(' ');
            outPrintWriter.println(str);
            return -1;
        }
        this.mInterface.setUidPolicy(uidFromNextArg, 0);
        return 0;
    }

    private int addRestrictBackgroundWhitelist() throws RemoteException {
        return setUidPolicy(4);
    }

    private int removeRestrictBackgroundWhitelist() throws RemoteException {
        return resetUidPolicy("not whitelisted", 4);
    }

    private int addRestrictBackgroundBlacklist() throws RemoteException {
        return setUidPolicy(1);
    }

    private int removeRestrictBackgroundBlacklist() throws RemoteException {
        return resetUidPolicy("not blacklisted", 1);
    }

    private int listWifiNetworks() {
        int i;
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg != null) {
            if (Boolean.parseBoolean(nextArg)) {
                i = 1;
            } else {
                i = 2;
            }
        } else {
            i = 0;
        }
        for (WifiConfiguration wifiConfiguration : this.mWifiManager.getConfiguredNetworks()) {
            if (nextArg == null || wifiConfiguration.meteredOverride == i) {
                outPrintWriter.print(NetworkPolicyManager.resolveNetworkId(wifiConfiguration));
                outPrintWriter.print(';');
                outPrintWriter.println(overrideToString(wifiConfiguration.meteredOverride));
            }
        }
        return 0;
    }

    private int setMeteredWifiNetwork() throws RemoteException {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify networkId");
            return -1;
        }
        String nextArg2 = getNextArg();
        if (nextArg2 == null) {
            outPrintWriter.println("Error: didn't specify meteredOverride");
            return -1;
        }
        this.mInterface.setWifiMeteredOverride(NetworkPolicyManager.resolveNetworkId(nextArg), stringToOverride(nextArg2));
        return -1;
    }

    private static String overrideToString(int i) {
        switch (i) {
            case 1:
                return "true";
            case 2:
                return "false";
            default:
                return "none";
        }
    }

    private static int stringToOverride(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 3569038) {
            b = (iHashCode == 97196323 && str.equals("false")) ? (byte) 1 : (byte) -1;
        } else if (str.equals("true")) {
            b = 0;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            default:
                return 0;
        }
    }

    private int getNextBooleanArg() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg != null) {
            return Boolean.valueOf(nextArg).booleanValue() ? 1 : 0;
        }
        outPrintWriter.println("Error: didn't specify BOOLEAN");
        return -1;
    }

    private int getUidFromNextArg() {
        PrintWriter outPrintWriter = getOutPrintWriter();
        String nextArg = getNextArg();
        if (nextArg == null) {
            outPrintWriter.println("Error: didn't specify UID");
            return -1;
        }
        try {
            return Integer.parseInt(nextArg);
        } catch (NumberFormatException e) {
            outPrintWriter.println("Error: UID (" + nextArg + ") should be a number");
            return -2;
        }
    }
}
