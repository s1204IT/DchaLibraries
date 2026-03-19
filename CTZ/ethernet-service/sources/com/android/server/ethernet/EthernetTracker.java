package com.android.server.ethernet;

import android.R;
import android.content.Context;
import android.net.IEthernetServiceListener;
import android.net.InterfaceConfiguration;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.NetworkCapabilities;
import android.net.StaticIpConfiguration;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.net.BaseNetworkObserver;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

final class EthernetTracker {
    private static final boolean DBG = true;
    private static final String TAG = EthernetTracker.class.getSimpleName();
    private final EthernetConfigStore mConfigStore;
    private final EthernetNetworkFactory mFactory;
    private final Handler mHandler;
    private final String mIfaceMatch;
    private volatile IpConfiguration mIpConfigForDefaultInterface;
    private final ConcurrentHashMap<String, NetworkCapabilities> mNetworkCapabilities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpConfiguration> mIpConfigurations = new ConcurrentHashMap<>();
    private final RemoteCallbackList<IEthernetServiceListener> mListeners = new RemoteCallbackList<>();
    private final INetworkManagementService mNMService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));

    EthernetTracker(Context context, Handler handler) {
        this.mHandler = handler;
        this.mIfaceMatch = context.getResources().getString(R.string.addToDictionary);
        for (String str : context.getResources().getStringArray(R.array.config_biometric_sensors)) {
            parseEthernetConfig(str);
        }
        this.mConfigStore = new EthernetConfigStore();
        this.mFactory = new EthernetNetworkFactory(handler, context, createNetworkCapabilities(DBG));
        this.mFactory.register();
    }

    void start() {
        this.mConfigStore.read();
        this.mIpConfigForDefaultInterface = this.mConfigStore.getIpConfigurationForDefaultInterface();
        ArrayMap<String, IpConfiguration> ipConfigurations = this.mConfigStore.getIpConfigurations();
        for (int i = 0; i < ipConfigurations.size(); i++) {
            this.mIpConfigurations.put(ipConfigurations.keyAt(i), ipConfigurations.valueAt(i));
        }
        try {
            this.mNMService.registerObserver(new InterfaceObserver());
        } catch (RemoteException e) {
            Log.e(TAG, "Could not register InterfaceObserver " + e);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.trackAvailableInterfaces();
            }
        });
    }

    void updateIpConfiguration(final String str, final IpConfiguration ipConfiguration) {
        Log.i(TAG, "updateIpConfiguration, iface: " + str + ", cfg: " + ipConfiguration);
        this.mConfigStore.write(str, ipConfiguration);
        this.mIpConfigurations.put(str, ipConfiguration);
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mFactory.updateIpConfiguration(str, ipConfiguration);
            }
        });
    }

    IpConfiguration getIpConfiguration(String str) {
        return this.mIpConfigurations.get(str);
    }

    boolean isTrackingInterface(String str) {
        return this.mFactory.hasInterface(str);
    }

    String[] getInterfaces(boolean z) {
        return this.mFactory.getAvailableInterfaces(z);
    }

    boolean isRestrictedInterface(String str) {
        NetworkCapabilities networkCapabilities = this.mNetworkCapabilities.get(str);
        if (networkCapabilities == null || networkCapabilities.hasCapability(13)) {
            return false;
        }
        return DBG;
    }

    void addListener(IEthernetServiceListener iEthernetServiceListener, boolean z) {
        this.mListeners.register(iEthernetServiceListener, new ListenerInfo(z));
    }

    void removeListener(IEthernetServiceListener iEthernetServiceListener) {
        this.mListeners.unregister(iEthernetServiceListener);
    }

    private void removeInterface(String str) {
        this.mFactory.removeInterface(str);
    }

    private void addInterface(String str) {
        InterfaceConfiguration interfaceConfig;
        try {
            this.mNMService.setInterfaceUp(str);
            interfaceConfig = this.mNMService.getInterfaceConfig(str);
        } catch (RemoteException | IllegalStateException e) {
            Log.e(TAG, "Error upping interface " + str, e);
            interfaceConfig = null;
        }
        if (interfaceConfig == null) {
            Log.e(TAG, "Null interface config for " + str + ". Bailing out.");
            return;
        }
        String hardwareAddress = interfaceConfig.getHardwareAddress();
        NetworkCapabilities networkCapabilitiesCreateDefaultNetworkCapabilities = this.mNetworkCapabilities.get(str);
        if (networkCapabilitiesCreateDefaultNetworkCapabilities == null && (networkCapabilitiesCreateDefaultNetworkCapabilities = this.mNetworkCapabilities.get(hardwareAddress)) == null) {
            networkCapabilitiesCreateDefaultNetworkCapabilities = createDefaultNetworkCapabilities();
        }
        IpConfiguration ipConfigurationCreateDefaultIpConfiguration = this.mIpConfigurations.get(str);
        if (ipConfigurationCreateDefaultIpConfiguration == null) {
            ipConfigurationCreateDefaultIpConfiguration = createDefaultIpConfiguration();
        }
        Log.d(TAG, "Started tracking interface " + str);
        this.mFactory.addInterface(str, hardwareAddress, networkCapabilitiesCreateDefaultNetworkCapabilities, ipConfigurationCreateDefaultIpConfiguration);
        if (interfaceConfig.hasFlag("running")) {
            updateInterfaceState(str, DBG);
        }
    }

    private void updateInterfaceState(String str, boolean z) {
        if (this.mFactory.updateInterfaceLinkState(str, z)) {
            boolean zIsRestrictedInterface = isRestrictedInterface(str);
            int iBeginBroadcast = this.mListeners.beginBroadcast();
            for (int i = 0; i < iBeginBroadcast; i++) {
                if (zIsRestrictedInterface) {
                    try {
                        if (((ListenerInfo) this.mListeners.getBroadcastCookie(i)).canUseRestrictedNetworks) {
                            this.mListeners.getBroadcastItem(i).onAvailabilityChanged(str, z);
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mListeners.finishBroadcast();
        }
    }

    private void maybeTrackInterface(String str) {
        Log.i(TAG, "maybeTrackInterface " + str);
        if (!str.matches(this.mIfaceMatch) || this.mFactory.hasInterface(str)) {
            return;
        }
        if (this.mIpConfigForDefaultInterface != null) {
            updateIpConfiguration(str, this.mIpConfigForDefaultInterface);
            this.mIpConfigForDefaultInterface = null;
        }
        addInterface(str);
    }

    private void trackAvailableInterfaces() {
        try {
            for (String str : this.mNMService.listInterfaces()) {
                maybeTrackInterface(str);
            }
        } catch (RemoteException | IllegalStateException e) {
            Log.e(TAG, "Could not get list of interfaces " + e);
        }
    }

    private class InterfaceObserver extends BaseNetworkObserver {
        private InterfaceObserver() {
        }

        public void interfaceLinkStateChanged(final String str, final boolean z) {
            Log.i(EthernetTracker.TAG, "interfaceLinkStateChanged, iface: " + str + ", up: " + z);
            EthernetTracker.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    EthernetTracker.this.updateInterfaceState(str, z);
                }
            });
        }

        public void interfaceAdded(final String str) {
            EthernetTracker.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    EthernetTracker.this.maybeTrackInterface(str);
                }
            });
        }

        public void interfaceRemoved(final String str) {
            EthernetTracker.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    EthernetTracker.this.removeInterface(str);
                }
            });
        }
    }

    private static class ListenerInfo {
        boolean canUseRestrictedNetworks;

        ListenerInfo(boolean z) {
            this.canUseRestrictedNetworks = false;
            this.canUseRestrictedNetworks = z;
        }
    }

    private void parseEthernetConfig(String str) {
        String[] strArrSplit = str.split(";");
        String str2 = strArrSplit[0];
        String str3 = strArrSplit.length > 1 ? strArrSplit[1] : null;
        this.mNetworkCapabilities.put(str2, createNetworkCapabilities(true ^ TextUtils.isEmpty(str3), str3));
        if (strArrSplit.length > 2 && !TextUtils.isEmpty(strArrSplit[2])) {
            this.mIpConfigurations.put(str2, parseStaticIpConfiguration(strArrSplit[2]));
        }
    }

    private static NetworkCapabilities createDefaultNetworkCapabilities() {
        NetworkCapabilities networkCapabilitiesCreateNetworkCapabilities = createNetworkCapabilities(false);
        networkCapabilitiesCreateNetworkCapabilities.addCapability(12);
        networkCapabilitiesCreateNetworkCapabilities.addCapability(13);
        networkCapabilitiesCreateNetworkCapabilities.addCapability(11);
        networkCapabilitiesCreateNetworkCapabilities.addCapability(18);
        networkCapabilitiesCreateNetworkCapabilities.addCapability(20);
        return networkCapabilitiesCreateNetworkCapabilities;
    }

    private static NetworkCapabilities createNetworkCapabilities(boolean z) {
        return createNetworkCapabilities(z, null);
    }

    private static NetworkCapabilities createNetworkCapabilities(boolean z, String str) {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        if (z) {
            networkCapabilities.clearAll();
        }
        networkCapabilities.addTransportType(3);
        networkCapabilities.setLinkUpstreamBandwidthKbps(100000);
        networkCapabilities.setLinkDownstreamBandwidthKbps(100000);
        if (!TextUtils.isEmpty(str)) {
            for (String str2 : str.split(",")) {
                if (!TextUtils.isEmpty(str2)) {
                    networkCapabilities.addCapability(Integer.valueOf(str2).intValue());
                }
            }
        }
        return networkCapabilities;
    }

    @VisibleForTesting
    static IpConfiguration parseStaticIpConfiguration(String str) {
        StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
        for (String str2 : str.trim().split(" ")) {
            if (!TextUtils.isEmpty(str2)) {
                String[] strArrSplit = str2.split("=");
                if (strArrSplit.length != 2) {
                    throw new IllegalArgumentException("Unexpected token: " + str2 + " in " + str);
                }
                String str3 = strArrSplit[0];
                byte b = 1;
                String str4 = strArrSplit[1];
                int iHashCode = str3.hashCode();
                if (iHashCode == -189118908) {
                    if (str3.equals("gateway")) {
                        b = 2;
                    }
                    switch (b) {
                    }
                } else if (iHashCode == 3367) {
                    if (str3.equals("ip")) {
                        b = 0;
                    }
                    switch (b) {
                    }
                } else if (iHashCode != 99625) {
                    if (iHashCode != 1837548591 || !str3.equals("domains")) {
                        b = -1;
                    }
                    switch (b) {
                        case 0:
                            staticIpConfiguration.ipAddress = new LinkAddress(str4);
                            break;
                        case 1:
                            staticIpConfiguration.domains = str4;
                            break;
                        case 2:
                            staticIpConfiguration.gateway = InetAddress.parseNumericAddress(str4);
                            break;
                        case 3:
                            ArrayList arrayList = new ArrayList();
                            for (String str5 : str4.split(",")) {
                                arrayList.add(InetAddress.parseNumericAddress(str5));
                            }
                            staticIpConfiguration.dnsServers.addAll(arrayList);
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected key: " + str3 + " in " + str);
                    }
                } else {
                    if (str3.equals("dns")) {
                        b = 3;
                    }
                    switch (b) {
                    }
                }
            }
        }
        return new IpConfiguration(IpConfiguration.IpAssignment.STATIC, IpConfiguration.ProxySettings.NONE, staticIpConfiguration, null);
    }

    private static IpConfiguration createDefaultIpConfiguration() {
        return new IpConfiguration(IpConfiguration.IpAssignment.DHCP, IpConfiguration.ProxySettings.NONE, null, null);
    }

    private void postAndWaitForRunnable(Runnable runnable) {
        this.mHandler.runWithScissors(runnable, 2000L);
    }

    void dump(final FileDescriptor fileDescriptor, final IndentingPrintWriter indentingPrintWriter, final String[] strArr) {
        postAndWaitForRunnable(new Runnable() {
            @Override
            public final void run() {
                EthernetTracker.lambda$dump$1(this.f$0, indentingPrintWriter, fileDescriptor, strArr);
            }
        });
    }

    public static void lambda$dump$1(EthernetTracker ethernetTracker, IndentingPrintWriter indentingPrintWriter, FileDescriptor fileDescriptor, String[] strArr) {
        indentingPrintWriter.println(ethernetTracker.getClass().getSimpleName());
        indentingPrintWriter.println("Ethernet interface name filter: " + ethernetTracker.mIfaceMatch);
        indentingPrintWriter.println("Listeners: " + ethernetTracker.mListeners.getRegisteredCallbackCount());
        indentingPrintWriter.println("IP Configurations:");
        indentingPrintWriter.increaseIndent();
        for (String str : ethernetTracker.mIpConfigurations.keySet()) {
            indentingPrintWriter.println(str + ": " + ethernetTracker.mIpConfigurations.get(str));
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.println("Network Capabilities:");
        indentingPrintWriter.increaseIndent();
        for (String str2 : ethernetTracker.mNetworkCapabilities.keySet()) {
            indentingPrintWriter.println(str2 + ": " + ethernetTracker.mNetworkCapabilities.get(str2));
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        ethernetTracker.mFactory.dump(fileDescriptor, indentingPrintWriter, strArr);
    }
}
