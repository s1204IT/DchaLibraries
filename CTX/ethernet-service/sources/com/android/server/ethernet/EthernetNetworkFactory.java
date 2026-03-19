package com.android.server.ethernet;

import android.R;
import android.content.Context;
import android.net.IpConfiguration;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.StringNetworkSpecifier;
import android.net.ip.IpClient;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.ethernet.EthernetNetworkFactory;
import java.io.FileDescriptor;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class EthernetNetworkFactory extends NetworkFactory {
    static final boolean DBG = true;
    private static final int NETWORK_SCORE = 70;
    private static final String NETWORK_TYPE = "Ethernet";
    private static final String TAG = EthernetNetworkFactory.class.getSimpleName();
    private final Context mContext;
    private final Handler mHandler;
    private final ConcurrentHashMap<String, NetworkInterfaceState> mTrackingInterfaces;

    public EthernetNetworkFactory(Handler handler, Context context, NetworkCapabilities networkCapabilities) {
        super(handler.getLooper(), context, NETWORK_TYPE, networkCapabilities);
        this.mTrackingInterfaces = new ConcurrentHashMap<>();
        this.mHandler = handler;
        this.mContext = context;
        setScoreFilter(NETWORK_SCORE);
    }

    public boolean acceptRequest(NetworkRequest networkRequest, int i) {
        Log.d(TAG, "acceptRequest, request: " + networkRequest + ", score: " + i);
        if (networkForRequest(networkRequest) != null) {
            return DBG;
        }
        return false;
    }

    protected void needNetworkFor(NetworkRequest networkRequest, int i) {
        NetworkInterfaceState networkInterfaceStateNetworkForRequest = networkForRequest(networkRequest);
        if (networkInterfaceStateNetworkForRequest == null) {
            Log.e(TAG, "needNetworkFor, failed to get a network for " + networkRequest);
            return;
        }
        long j = networkInterfaceStateNetworkForRequest.refCount + 1;
        networkInterfaceStateNetworkForRequest.refCount = j;
        if (j != 1) {
            return;
        }
        networkInterfaceStateNetworkForRequest.start();
    }

    protected void releaseNetworkFor(NetworkRequest networkRequest) {
        NetworkInterfaceState networkInterfaceStateNetworkForRequest = networkForRequest(networkRequest);
        if (networkInterfaceStateNetworkForRequest == null) {
            Log.e(TAG, "needNetworkFor, failed to get a network for " + networkRequest);
            return;
        }
        long j = networkInterfaceStateNetworkForRequest.refCount - 1;
        networkInterfaceStateNetworkForRequest.refCount = j;
        if (j == 1) {
            networkInterfaceStateNetworkForRequest.stop();
        }
    }

    String[] getAvailableInterfaces(final boolean z) {
        return (String[]) this.mTrackingInterfaces.values().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return EthernetNetworkFactory.lambda$getAvailableInterfaces$0(z, (EthernetNetworkFactory.NetworkInterfaceState) obj);
            }
        }).sorted(new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return EthernetNetworkFactory.lambda$getAvailableInterfaces$1((EthernetNetworkFactory.NetworkInterfaceState) obj, (EthernetNetworkFactory.NetworkInterfaceState) obj2);
            }
        }).map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((EthernetNetworkFactory.NetworkInterfaceState) obj).name;
            }
        }).toArray(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return EthernetNetworkFactory.lambda$getAvailableInterfaces$3(i);
            }
        });
    }

    static boolean lambda$getAvailableInterfaces$0(boolean z, NetworkInterfaceState networkInterfaceState) {
        if (!networkInterfaceState.isRestricted() || z) {
            return DBG;
        }
        return false;
    }

    static int lambda$getAvailableInterfaces$1(NetworkInterfaceState networkInterfaceState, NetworkInterfaceState networkInterfaceState2) {
        int iCompare = Boolean.compare(networkInterfaceState.isRestricted(), networkInterfaceState2.isRestricted());
        return iCompare == 0 ? networkInterfaceState.name.compareTo(networkInterfaceState2.name) : iCompare;
    }

    static String[] lambda$getAvailableInterfaces$3(int i) {
        return new String[i];
    }

    void addInterface(String str, String str2, NetworkCapabilities networkCapabilities, IpConfiguration ipConfiguration) {
        if (this.mTrackingInterfaces.containsKey(str)) {
            Log.e(TAG, "Interface with name " + str + " already exists.");
            return;
        }
        Log.d(TAG, "addInterface, iface: " + str + ", capabilities: " + networkCapabilities);
        NetworkInterfaceState networkInterfaceState = new NetworkInterfaceState(str, str2, this.mHandler, this.mContext, networkCapabilities);
        networkInterfaceState.setIpConfig(ipConfiguration);
        this.mTrackingInterfaces.put(str, networkInterfaceState);
        updateCapabilityFilter();
    }

    private void updateCapabilityFilter() {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.clearAll();
        Iterator<NetworkInterfaceState> it = this.mTrackingInterfaces.values().iterator();
        while (it.hasNext()) {
            networkCapabilities.combineCapabilities(it.next().mCapabilities);
        }
        Log.d(TAG, "updateCapabilityFilter: " + networkCapabilities);
        setCapabilityFilter(networkCapabilities);
    }

    void removeInterface(String str) {
        NetworkInterfaceState networkInterfaceStateRemove = this.mTrackingInterfaces.remove(str);
        if (networkInterfaceStateRemove != null) {
            networkInterfaceStateRemove.stop();
        }
        updateCapabilityFilter();
    }

    boolean updateInterfaceLinkState(String str, boolean z) {
        if (!this.mTrackingInterfaces.containsKey(str)) {
            return false;
        }
        Log.d(TAG, "updateInterfaceLinkState, iface: " + str + ", up: " + z);
        return this.mTrackingInterfaces.get(str).updateLinkState(z);
    }

    boolean hasInterface(String str) {
        return this.mTrackingInterfaces.containsKey(str);
    }

    void updateIpConfiguration(String str, IpConfiguration ipConfiguration) {
        NetworkInterfaceState networkInterfaceState = this.mTrackingInterfaces.get(str);
        if (networkInterfaceState != null) {
            networkInterfaceState.setIpConfig(ipConfiguration);
        }
    }

    private NetworkInterfaceState networkForRequest(NetworkRequest networkRequest) {
        String str;
        StringNetworkSpecifier networkSpecifier = networkRequest.networkCapabilities.getNetworkSpecifier();
        NetworkInterfaceState networkInterfaceState = null;
        if (networkSpecifier instanceof StringNetworkSpecifier) {
            str = networkSpecifier.specifier;
        } else {
            str = null;
        }
        if (!TextUtils.isEmpty(str)) {
            NetworkInterfaceState networkInterfaceState2 = this.mTrackingInterfaces.get(str);
            if (networkInterfaceState2 != null && networkInterfaceState2.statisified(networkRequest.networkCapabilities)) {
                networkInterfaceState = networkInterfaceState2;
            }
        } else {
            Iterator<NetworkInterfaceState> it = this.mTrackingInterfaces.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                NetworkInterfaceState next = it.next();
                if (next.statisified(networkRequest.networkCapabilities)) {
                    networkInterfaceState = next;
                    break;
                }
            }
        }
        Log.i(TAG, "networkForRequest, request: " + networkRequest + ", network: " + networkInterfaceState);
        return networkInterfaceState;
    }

    private static class NetworkInterfaceState {
        private static String sTcpBufferSizes = null;
        private final NetworkCapabilities mCapabilities;
        private final Context mContext;
        private final Handler mHandler;
        private final String mHwAddress;
        private IpClient mIpClient;
        private IpConfiguration mIpConfig;
        private boolean mLinkUp;
        private NetworkAgent mNetworkAgent;
        final String name;
        private LinkProperties mLinkProperties = new LinkProperties();
        long refCount = 0;
        private final IpClient.Callback mIpClientCallback = new AnonymousClass1();
        private final NetworkInfo mNetworkInfo = new NetworkInfo(9, 0, EthernetNetworkFactory.NETWORK_TYPE, "");

        class AnonymousClass1 extends IpClient.Callback {
            AnonymousClass1() {
            }

            public void onProvisioningSuccess(final LinkProperties linkProperties) {
                NetworkInterfaceState.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        EthernetNetworkFactory.NetworkInterfaceState.this.onIpLayerStarted(linkProperties);
                    }
                });
            }

            public void onProvisioningFailure(final LinkProperties linkProperties) {
                NetworkInterfaceState.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        EthernetNetworkFactory.NetworkInterfaceState.this.onIpLayerStopped(linkProperties);
                    }
                });
            }

            public void onLinkPropertiesChange(final LinkProperties linkProperties) {
                NetworkInterfaceState.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        EthernetNetworkFactory.NetworkInterfaceState.this.updateLinkProperties(linkProperties);
                    }
                });
            }
        }

        NetworkInterfaceState(String str, String str2, Handler handler, Context context, NetworkCapabilities networkCapabilities) {
            this.name = str;
            this.mCapabilities = networkCapabilities;
            this.mHandler = handler;
            this.mContext = context;
            this.mHwAddress = str2;
            this.mNetworkInfo.setExtraInfo(this.mHwAddress);
            this.mNetworkInfo.setIsAvailable(EthernetNetworkFactory.DBG);
        }

        void setIpConfig(IpConfiguration ipConfiguration) {
            this.mIpConfig = ipConfiguration;
        }

        boolean statisified(NetworkCapabilities networkCapabilities) {
            return networkCapabilities.satisfiedByNetworkCapabilities(this.mCapabilities);
        }

        boolean isRestricted() {
            return this.mCapabilities.hasCapability(13);
        }

        private void start() {
            if (this.mIpClient != null) {
                Log.d(EthernetNetworkFactory.TAG, "IpClient already started");
                return;
            }
            Log.d(EthernetNetworkFactory.TAG, String.format("starting IpClient(%s): mNetworkInfo=%s", this.name, this.mNetworkInfo));
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR, null, this.mHwAddress);
            this.mIpClient = new IpClient(this.mContext, this.name, this.mIpClientCallback);
            if (sTcpBufferSizes == null) {
                sTcpBufferSizes = this.mContext.getResources().getString(R.string.add_account_button_label);
            }
            provisionIpClient(this.mIpClient, this.mIpConfig, sTcpBufferSizes);
        }

        void onIpLayerStarted(LinkProperties linkProperties) {
            if (this.mNetworkAgent != null) {
                Log.e(EthernetNetworkFactory.TAG, "Already have a NetworkAgent - aborting new request");
                stop();
            } else {
                this.mLinkProperties = linkProperties;
                this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, this.mHwAddress);
                this.mNetworkInfo.setIsAvailable(EthernetNetworkFactory.DBG);
                this.mNetworkAgent = new NetworkAgent(this.mHandler.getLooper(), this.mContext, EthernetNetworkFactory.NETWORK_TYPE, this.mNetworkInfo, this.mCapabilities, this.mLinkProperties, EthernetNetworkFactory.NETWORK_SCORE) {
                    public void unwanted() {
                        if (this != NetworkInterfaceState.this.mNetworkAgent) {
                            if (NetworkInterfaceState.this.mNetworkAgent != null) {
                                Log.d(EthernetNetworkFactory.TAG, "Ignoring unwanted as we have a more modern instance");
                                return;
                            }
                            return;
                        }
                        NetworkInterfaceState.this.stop();
                    }
                };
            }
        }

        void onIpLayerStopped(LinkProperties linkProperties) {
            stop();
            start();
        }

        void updateLinkProperties(LinkProperties linkProperties) {
            this.mLinkProperties = linkProperties;
            if (this.mNetworkAgent != null) {
                this.mNetworkAgent.sendLinkProperties(linkProperties);
            }
        }

        boolean updateLinkState(boolean z) {
            if (this.mLinkUp == z) {
                return false;
            }
            this.mLinkUp = z;
            stop();
            if (z) {
                start();
                return EthernetNetworkFactory.DBG;
            }
            return EthernetNetworkFactory.DBG;
        }

        void stop() {
            if (this.mIpClient != null) {
                this.mIpClient.shutdown();
                this.mIpClient.awaitShutdown();
                this.mIpClient = null;
            }
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, this.mHwAddress);
            if (this.mNetworkAgent != null) {
                updateAgent();
                this.mNetworkAgent = null;
            }
            clear();
        }

        private void updateAgent() {
            if (this.mNetworkAgent == null) {
                return;
            }
            Log.i(EthernetNetworkFactory.TAG, "Updating mNetworkAgent with: " + this.mCapabilities + ", " + this.mNetworkInfo + ", " + this.mLinkProperties);
            this.mNetworkAgent.sendNetworkCapabilities(this.mCapabilities);
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
            this.mNetworkAgent.sendNetworkScore(this.mLinkUp ? EthernetNetworkFactory.NETWORK_SCORE : 0);
        }

        private void clear() {
            this.mLinkProperties.clear();
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.IDLE, null, null);
            this.mNetworkInfo.setIsAvailable(false);
        }

        private static void provisionIpClient(IpClient ipClient, IpConfiguration ipConfiguration, String str) {
            IpClient.ProvisioningConfiguration provisioningConfigurationBuild;
            if (ipConfiguration.getProxySettings() == IpConfiguration.ProxySettings.STATIC || ipConfiguration.getProxySettings() == IpConfiguration.ProxySettings.PAC) {
                ipClient.setHttpProxy(ipConfiguration.getHttpProxy());
            }
            if (!TextUtils.isEmpty(str)) {
                ipClient.setTcpBufferSizes(str);
            }
            if (ipConfiguration.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
                provisioningConfigurationBuild = IpClient.buildProvisioningConfiguration().withStaticConfiguration(ipConfiguration.getStaticIpConfiguration()).build();
            } else {
                provisioningConfigurationBuild = IpClient.buildProvisioningConfiguration().withProvisioningTimeoutMs(0).build();
            }
            ipClient.startProvisioning(provisioningConfigurationBuild);
        }

        public String toString() {
            return getClass().getSimpleName() + "{ iface: " + this.name + ", up: " + this.mLinkUp + ", hwAddress: " + this.mHwAddress + ", networkInfo: " + this.mNetworkInfo + ", networkAgent: " + this.mNetworkAgent + ", ipClient: " + this.mIpClient + ",linkProperties: " + this.mLinkProperties + "}";
        }
    }

    void dump(FileDescriptor fileDescriptor, IndentingPrintWriter indentingPrintWriter, String[] strArr) {
        super.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.println(getClass().getSimpleName());
        indentingPrintWriter.println("Tracking interfaces:");
        indentingPrintWriter.increaseIndent();
        for (String str : this.mTrackingInterfaces.keySet()) {
            NetworkInterfaceState networkInterfaceState = this.mTrackingInterfaces.get(str);
            indentingPrintWriter.println(str + ":" + networkInterfaceState);
            indentingPrintWriter.increaseIndent();
            IpClient ipClient = networkInterfaceState.mIpClient;
            if (ipClient != null) {
                ipClient.dump(fileDescriptor, indentingPrintWriter, strArr);
            } else {
                indentingPrintWriter.println("IpClient is null");
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
    }
}
