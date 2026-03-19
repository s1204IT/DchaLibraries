package android.net;

import android.content.Context;
import android.net.INetworkPolicyListener;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.keystore.KeyProperties;
import android.util.DebugUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import java.time.ZonedDateTime;
import java.util.Iterator;

public class NetworkPolicyManager {
    private static final boolean ALLOW_PLATFORM_APP_POLICY = true;
    public static final String EXTRA_NETWORK_TEMPLATE = "android.net.NETWORK_TEMPLATE";
    public static final int FIREWALL_CHAIN_DOZABLE = 1;
    public static final String FIREWALL_CHAIN_NAME_DOZABLE = "dozable";
    public static final String FIREWALL_CHAIN_NAME_NONE = "none";
    public static final String FIREWALL_CHAIN_NAME_POWERSAVE = "powersave";
    public static final String FIREWALL_CHAIN_NAME_STANDBY = "standby";
    public static final int FIREWALL_CHAIN_NONE = 0;
    public static final int FIREWALL_CHAIN_POWERSAVE = 3;
    public static final int FIREWALL_CHAIN_STANDBY = 2;
    public static final int FIREWALL_RULE_ALLOW = 1;
    public static final int FIREWALL_RULE_DEFAULT = 0;
    public static final int FIREWALL_RULE_DENY = 2;
    public static final int FIREWALL_TYPE_BLACKLIST = 1;
    public static final int FIREWALL_TYPE_WHITELIST = 0;
    public static final int FOREGROUND_THRESHOLD_STATE = 4;
    public static final int MASK_ALL_NETWORKS = 240;
    public static final int MASK_METERED_NETWORKS = 15;
    public static final int OVERRIDE_CONGESTED = 2;
    public static final int OVERRIDE_UNMETERED = 1;
    public static final int POLICY_ALLOW_METERED_BACKGROUND = 4;
    public static final int POLICY_NONE = 0;
    public static final int POLICY_REJECT_METERED_BACKGROUND = 1;
    public static final int RULE_ALLOW_ALL = 32;
    public static final int RULE_ALLOW_METERED = 1;
    public static final int RULE_NONE = 0;
    public static final int RULE_REJECT_ALL = 64;
    public static final int RULE_REJECT_METERED = 4;
    public static final int RULE_TEMPORARY_ALLOW_METERED = 2;
    private static final String TAG = "NetworkPolicyManager";
    private final Context mContext;
    private INetworkPolicyManager mService;

    public NetworkPolicyManager(Context context, INetworkPolicyManager iNetworkPolicyManager) {
        if (iNetworkPolicyManager == null) {
            throw new IllegalArgumentException("missing INetworkPolicyManager");
        }
        this.mContext = context;
        this.mService = iNetworkPolicyManager;
    }

    public static NetworkPolicyManager from(Context context) {
        return (NetworkPolicyManager) context.getSystemService(Context.NETWORK_POLICY_SERVICE);
    }

    public void setUidPolicy(int i, int i2) {
        try {
            this.mService.setUidPolicy(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addUidPolicy(int i, int i2) {
        try {
            this.mService.addUidPolicy(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeUidPolicy(int i, int i2) {
        try {
            this.mService.removeUidPolicy(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getUidPolicy(int i) {
        try {
            return this.mService.getUidPolicy(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int[] getUidsWithPolicy(int i) {
        try {
            return this.mService.getUidsWithPolicy(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerListener(INetworkPolicyListener iNetworkPolicyListener) {
        try {
            this.mService.registerListener(iNetworkPolicyListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterListener(INetworkPolicyListener iNetworkPolicyListener) {
        try {
            this.mService.unregisterListener(iNetworkPolicyListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNetworkPolicies(NetworkPolicy[] networkPolicyArr) {
        try {
            this.mService.setNetworkPolicies(networkPolicyArr);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkPolicy[] getNetworkPolicies() {
        try {
            return this.mService.getNetworkPolicies(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRestrictBackground(boolean z) {
        Log.d(TAG, "setRestrictBackground " + z);
        try {
            this.mService.setRestrictBackground(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean getRestrictBackground() {
        try {
            return this.mService.getRestrictBackground();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void factoryReset(String str) {
        try {
            this.mService.factoryReset(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public static Iterator<Pair<ZonedDateTime, ZonedDateTime>> cycleIterator(NetworkPolicy networkPolicy) {
        final Iterator<Range<ZonedDateTime>> itCycleIterator = networkPolicy.cycleIterator();
        return new Iterator<Pair<ZonedDateTime, ZonedDateTime>>() {
            @Override
            public boolean hasNext() {
                return itCycleIterator.hasNext();
            }

            @Override
            public Pair<ZonedDateTime, ZonedDateTime> next() {
                if (hasNext()) {
                    Range range = (Range) itCycleIterator.next();
                    return Pair.create((ZonedDateTime) range.getLower(), (ZonedDateTime) range.getUpper());
                }
                return Pair.create(null, null);
            }
        };
    }

    @Deprecated
    public static boolean isUidValidForPolicy(Context context, int i) {
        if (!UserHandle.isApp(i)) {
            return false;
        }
        return true;
    }

    public static String uidRulesToString(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        sb.append(" (");
        if (i == 0) {
            sb.append(KeyProperties.DIGEST_NONE);
        } else {
            sb.append(DebugUtils.flagsToString(NetworkPolicyManager.class, "RULE_", i));
        }
        sb.append(")");
        return sb.toString();
    }

    public static String uidPoliciesToString(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        sb.append(" (");
        if (i == 0) {
            sb.append(KeyProperties.DIGEST_NONE);
        } else {
            sb.append(DebugUtils.flagsToString(NetworkPolicyManager.class, "POLICY_", i));
        }
        sb.append(")");
        return sb.toString();
    }

    public static boolean isProcStateAllowedWhileIdleOrPowerSaveMode(int i) {
        return i <= 4;
    }

    public static boolean isProcStateAllowedWhileOnRestrictBackground(int i) {
        return i <= 4;
    }

    public static String resolveNetworkId(WifiConfiguration wifiConfiguration) {
        return WifiInfo.removeDoubleQuotes(wifiConfiguration.isPasspoint() ? wifiConfiguration.providerFriendlyName : wifiConfiguration.SSID);
    }

    public static String resolveNetworkId(String str) {
        return WifiInfo.removeDoubleQuotes(str);
    }

    public static class Listener extends INetworkPolicyListener.Stub {
        @Override
        public void onUidRulesChanged(int i, int i2) {
        }

        @Override
        public void onMeteredIfacesChanged(String[] strArr) {
        }

        @Override
        public void onRestrictBackgroundChanged(boolean z) {
        }

        @Override
        public void onUidPoliciesChanged(int i, int i2) {
        }

        @Override
        public void onSubscriptionOverride(int i, int i2, int i3) {
        }
    }
}
