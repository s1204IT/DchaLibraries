package com.android.server.net;

import android.content.Context;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NetworkPolicyManagerShellCommand extends ShellCommand {
    private final INetworkPolicyManager mInterface;
    private final WifiManager mWifiManager;

    NetworkPolicyManagerShellCommand(Context context, INetworkPolicyManager service) {
        this.mInterface = service;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter pw = getOutPrintWriter();
        try {
            return cmd.equals("get") ? runGet() : cmd.equals("set") ? runSet() : cmd.equals("list") ? runList() : cmd.equals("add") ? runAdd() : cmd.equals("remove") ? runRemove() : handleDefaultCommands(cmd);
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Network policy manager (netpolicy) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  add restrict-background-whitelist UID");
        pw.println("    Adds a UID to the whitelist for restrict background usage.");
        pw.println("  add restrict-background-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict background usage.");
        pw.println("  get restrict-background");
        pw.println("    Gets the global restrict background usage status.");
        pw.println("  list wifi-networks [BOOLEAN]");
        pw.println("    Lists all saved wifi networks and whether they are metered or not.");
        pw.println("    If a boolean argument is passed, filters just the metered (or unmetered)");
        pw.println("    networks.");
        pw.println("  list restrict-background-whitelist");
        pw.println("    Lists UIDs that are whitelisted for restrict background usage.");
        pw.println("  list restrict-background-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict background usage.");
        pw.println("  remove restrict-background-whitelist UID");
        pw.println("    Removes a UID from the whitelist for restrict background usage.");
        pw.println("  remove restrict-background-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict background usage.");
        pw.println("  set metered-network ID BOOLEAN");
        pw.println("    Toggles whether the given wi-fi network is metered.");
        pw.println("  set restrict-background BOOLEAN");
        pw.println("    Sets the global restrict background usage status.");
    }

    private int runGet() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to get");
            return -1;
        }
        if (type.equals("restrict-background")) {
            return getRestrictBackground();
        }
        pw.println("Error: unknown get type '" + type + "'");
        return -1;
    }

    private int runSet() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to set");
            return -1;
        }
        if (!type.equals("metered-network")) {
            if (type.equals("restrict-background")) {
                return setRestrictBackground();
            }
            pw.println("Error: unknown set type '" + type + "'");
            return -1;
        }
        return setMeteredWifiNetwork();
    }

    private int runList() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to list");
            return -1;
        }
        if (!type.equals("wifi-networks")) {
            if (!type.equals("restrict-background-whitelist")) {
                if (type.equals("restrict-background-blacklist")) {
                    return listRestrictBackgroundBlacklist();
                }
                pw.println("Error: unknown list type '" + type + "'");
                return -1;
            }
            return listRestrictBackgroundWhitelist();
        }
        return listWifiNetworks();
    }

    private int runAdd() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to add");
            return -1;
        }
        if (!type.equals("restrict-background-whitelist")) {
            if (type.equals("restrict-background-blacklist")) {
                return addRestrictBackgroundBlacklist();
            }
            pw.println("Error: unknown add type '" + type + "'");
            return -1;
        }
        return addRestrictBackgroundWhitelist();
    }

    private int runRemove() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to remove");
            return -1;
        }
        if (!type.equals("restrict-background-whitelist")) {
            if (type.equals("restrict-background-blacklist")) {
                return removeRestrictBackgroundBlacklist();
            }
            pw.println("Error: unknown remove type '" + type + "'");
            return -1;
        }
        return removeRestrictBackgroundWhitelist();
    }

    private int listRestrictBackgroundWhitelist() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int[] uids = this.mInterface.getRestrictBackgroundWhitelistedUids();
        pw.print("Restrict background whitelisted UIDs: ");
        if (uids.length == 0) {
            pw.println("none");
        } else {
            for (int uid : uids) {
                pw.print(uid);
                pw.print(' ');
            }
        }
        pw.println();
        return 0;
    }

    private int listRestrictBackgroundBlacklist() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        int[] uids = this.mInterface.getUidsWithPolicy(1);
        pw.print("Restrict background blacklisted UIDs: ");
        if (uids.length == 0) {
            pw.println("none");
        } else {
            for (int uid : uids) {
                pw.print(uid);
                pw.print(' ');
            }
        }
        pw.println();
        return 0;
    }

    private int getRestrictBackground() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        pw.print("Restrict background status: ");
        pw.println(this.mInterface.getRestrictBackground() ? "enabled" : "disabled");
        return 0;
    }

    private int setRestrictBackground() throws RemoteException {
        int enabled = getNextBooleanArg();
        if (enabled < 0) {
            return enabled;
        }
        this.mInterface.setRestrictBackground(enabled > 0);
        return 0;
    }

    private int addRestrictBackgroundWhitelist() throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.addRestrictBackgroundWhitelistedUid(uid);
        return 0;
    }

    private int removeRestrictBackgroundWhitelist() throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.removeRestrictBackgroundWhitelistedUid(uid);
        return 0;
    }

    private int addRestrictBackgroundBlacklist() throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.setUidPolicy(uid, 1);
        return 0;
    }

    private int removeRestrictBackgroundBlacklist() throws RemoteException {
        int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        this.mInterface.setUidPolicy(uid, 0);
        return 0;
    }

    private int listWifiNetworks() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        Boolean boolValueOf = arg == null ? null : Boolean.valueOf(arg);
        for (NetworkPolicy policy : getWifiPolicies()) {
            if (boolValueOf == null || boolValueOf.booleanValue() == policy.metered) {
                pw.print(getNetworkId(policy));
                pw.print(';');
                pw.println(policy.metered);
            }
        }
        return 0;
    }

    private int setMeteredWifiNetwork() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String id = getNextArg();
        if (id == null) {
            pw.println("Error: didn't specify ID");
            return -1;
        }
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify BOOLEAN");
            return -1;
        }
        boolean metered = Boolean.valueOf(arg).booleanValue();
        NetworkPolicy[] policies = this.mInterface.getNetworkPolicies((String) null);
        boolean changed = false;
        for (NetworkPolicy policy : policies) {
            if (!policy.template.isMatchRuleMobile() && policy.metered != metered) {
                String networkId = getNetworkId(policy);
                if (id.equals(networkId)) {
                    Log.i("NetworkPolicy", "Changing " + networkId + " metered status to " + metered);
                    policy.metered = metered;
                    changed = true;
                }
            }
        }
        if (changed) {
            this.mInterface.setNetworkPolicies(policies);
            return 0;
        }
        for (WifiConfiguration config : this.mWifiManager.getConfiguredNetworks()) {
            String ssid = WifiInfo.removeDoubleQuotes(config.SSID);
            if (id.equals(ssid)) {
                NetworkPolicy policy2 = newPolicy(ssid);
                policy2.metered = true;
                Log.i("NetworkPolicy", "Creating new policy for " + ssid + ": " + policy2);
                NetworkPolicy[] newPolicies = new NetworkPolicy[policies.length + 1];
                System.arraycopy(policies, 0, newPolicies, 0, policies.length);
                newPolicies[newPolicies.length - 1] = policy2;
                this.mInterface.setNetworkPolicies(newPolicies);
            }
        }
        return 0;
    }

    private List<NetworkPolicy> getWifiPolicies() throws RemoteException {
        List<WifiConfiguration> configs = this.mWifiManager.getConfiguredNetworks();
        int size = configs != null ? configs.size() : 0;
        Set<String> ssids = new HashSet<>(size);
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                ssids.add(WifiInfo.removeDoubleQuotes(config.SSID));
            }
        }
        NetworkPolicy[] policies = this.mInterface.getNetworkPolicies((String) null);
        List<NetworkPolicy> wifiPolicies = new ArrayList<>(policies.length);
        for (NetworkPolicy policy : policies) {
            if (!policy.template.isMatchRuleMobile()) {
                wifiPolicies.add(policy);
                String netId = getNetworkId(policy);
                ssids.remove(netId);
            }
        }
        for (String ssid : ssids) {
            wifiPolicies.add(newPolicy(ssid));
        }
        return wifiPolicies;
    }

    private NetworkPolicy newPolicy(String ssid) {
        NetworkTemplate template = NetworkTemplate.buildTemplateWifi(ssid);
        NetworkPolicy policy = NetworkPolicyManagerService.newWifiPolicy(template, false);
        return policy;
    }

    private String getNetworkId(NetworkPolicy policy) {
        return WifiInfo.removeDoubleQuotes(policy.template.getNetworkId());
    }

    private int getNextBooleanArg() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg != null) {
            return Boolean.valueOf(arg).booleanValue() ? 1 : 0;
        }
        pw.println("Error: didn't specify BOOLEAN");
        return -1;
    }

    private int getUidFromNextArg() {
        PrintWriter pw = getOutPrintWriter();
        String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify UID");
            return -1;
        }
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            pw.println("Error: UID (" + arg + ") should be a number");
            return -2;
        }
    }
}
