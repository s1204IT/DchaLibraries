package com.android.server.ethernet;

import android.R;
import android.content.Context;
import android.net.EthernetManager;
import android.net.IEthernetServiceListener;
import android.net.InterfaceConfiguration;
import android.net.IpConfiguration;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.StaticIpConfiguration;
import android.net.ip.IpManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.net.BaseNetworkObserver;
import java.io.FileDescriptor;

class EthernetNetworkFactory {
    private static final boolean DBG = true;
    private static final int NETWORK_SCORE = 70;
    private static final String NETWORK_TYPE = "Ethernet";
    private static final String TAG = "EthernetNetworkFactory";
    private static boolean mLinkUp;
    private Context mContext;
    private EthernetManager mEthernetManager;
    private LocalNetworkFactory mFactory;
    private String mHwAddr;
    private InterfaceObserver mInterfaceObserver;
    private IpManager mIpManager;
    private Thread mIpProvisioningThread;
    private final RemoteCallbackList<IEthernetServiceListener> mListeners;
    private INetworkManagementService mNMService;
    private NetworkAgent mNetworkAgent;
    private NetworkCapabilities mNetworkCapabilities;
    private static String mIfaceMatch = "";
    private static String mIface = "";
    private NetworkInfo mNetworkInfo = new NetworkInfo(9, 0, NETWORK_TYPE, "");
    private LinkProperties mLinkProperties = new LinkProperties();

    EthernetNetworkFactory(RemoteCallbackList<IEthernetServiceListener> listeners) {
        initNetworkCapabilities();
        this.mListeners = listeners;
    }

    private class LocalNetworkFactory extends NetworkFactory {
        LocalNetworkFactory(String name, Context context, Looper looper) {
            super(looper, context, name, new NetworkCapabilities());
        }

        protected void startNetwork() {
            EthernetNetworkFactory.this.onRequestNetwork();
        }

        protected void stopNetwork() {
        }
    }

    private void stopIpManagerLocked() {
        if (this.mIpManager == null) {
            return;
        }
        this.mIpManager.shutdown();
        this.mIpManager = null;
    }

    private void stopIpProvisioningThreadLocked() {
        stopIpManagerLocked();
        if (this.mIpProvisioningThread == null) {
            return;
        }
        this.mIpProvisioningThread.interrupt();
        this.mIpProvisioningThread = null;
    }

    private void updateInterfaceState(String iface, boolean up) {
        if (!mIface.equals(iface)) {
            return;
        }
        Log.d(TAG, "updateInterface: " + iface + " link " + (up ? "up" : "down"));
        synchronized (this) {
            mLinkUp = up;
            this.mNetworkInfo.setIsAvailable(up);
            if (!up) {
                this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, this.mHwAddr);
                stopIpProvisioningThreadLocked();
            }
            updateAgent();
            this.mFactory.setScoreFilter(up ? NETWORK_SCORE : -1);
        }
    }

    private class InterfaceObserver extends BaseNetworkObserver {
        InterfaceObserver(EthernetNetworkFactory this$0, InterfaceObserver interfaceObserver) {
            this();
        }

        private InterfaceObserver() {
        }

        public void interfaceLinkStateChanged(String iface, boolean up) {
            EthernetNetworkFactory.this.updateInterfaceState(iface, up);
        }

        public void interfaceAdded(String iface) {
            EthernetNetworkFactory.this.maybeTrackInterface(iface);
        }

        public void interfaceRemoved(String iface) {
            EthernetNetworkFactory.this.stopTrackingInterface(iface);
        }
    }

    private void setInterfaceUp(String iface) {
        try {
            this.mNMService.setInterfaceUp(iface);
            InterfaceConfiguration config = this.mNMService.getInterfaceConfig(iface);
            if (config == null) {
                Log.e(TAG, "Null iterface config for " + iface + ". Bailing out.");
                return;
            }
            synchronized (this) {
                if (!isTrackingInterface()) {
                    setInterfaceInfoLocked(iface, config.getHardwareAddress());
                    this.mNetworkInfo.setIsAvailable(DBG);
                    this.mNetworkInfo.setExtraInfo(this.mHwAddr);
                } else {
                    Log.e(TAG, "Interface unexpectedly changed from " + iface + " to " + mIface);
                    this.mNMService.setInterfaceDown(iface);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error upping interface " + mIface + ": " + e);
        }
    }

    private boolean maybeTrackInterface(String iface) {
        if (!iface.matches(mIfaceMatch) || isTrackingInterface()) {
            return false;
        }
        Log.d(TAG, "Started tracking interface " + iface);
        setInterfaceUp(iface);
        return DBG;
    }

    private void stopTrackingInterface(String iface) {
        if (!iface.equals(mIface)) {
            return;
        }
        Log.d(TAG, "Stopped tracking interface " + iface);
        synchronized (this) {
            stopIpProvisioningThreadLocked();
            setInterfaceInfoLocked("", null);
            this.mNetworkInfo.setExtraInfo(null);
            mLinkUp = false;
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, this.mHwAddr);
            updateAgent();
            this.mNetworkAgent = null;
            this.mNetworkInfo = new NetworkInfo(9, 0, NETWORK_TYPE, "");
            this.mLinkProperties = new LinkProperties();
        }
    }

    private boolean setStaticIpAddress(StaticIpConfiguration staticConfig) {
        if (staticConfig.ipAddress != null && staticConfig.gateway != null && staticConfig.dnsServers.size() > 0) {
            try {
                Log.i(TAG, "Applying static IPv4 configuration to " + mIface + ": " + staticConfig);
                InterfaceConfiguration config = this.mNMService.getInterfaceConfig(mIface);
                config.setLinkAddress(staticConfig.ipAddress);
                this.mNMService.setInterfaceConfig(mIface, config);
                return DBG;
            } catch (RemoteException | IllegalStateException e) {
                Log.e(TAG, "Setting static IP address failed: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Invalid static IP configuration.");
        }
        return false;
    }

    public void updateAgent() {
        synchronized (this) {
            if (this.mNetworkAgent == null) {
                return;
            }
            Log.i(TAG, "Updating mNetworkAgent with: " + this.mNetworkCapabilities + ", " + this.mNetworkInfo + ", " + this.mLinkProperties);
            this.mNetworkAgent.sendNetworkCapabilities(this.mNetworkCapabilities);
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
            this.mNetworkAgent.sendNetworkScore(mLinkUp ? NETWORK_SCORE : 0);
        }
    }

    public void onRequestNetwork() {
        synchronized (this) {
            if (this.mIpProvisioningThread != null) {
                return;
            }
            Thread ipProvisioningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LinkProperties linkProperties;
                    Log.d(EthernetNetworkFactory.TAG, String.format("starting ipProvisioningThread(%s): mNetworkInfo=%s", EthernetNetworkFactory.mIface, EthernetNetworkFactory.this.mNetworkInfo));
                    IpConfiguration config = EthernetNetworkFactory.this.mEthernetManager.getConfiguration();
                    if (config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
                        if (!EthernetNetworkFactory.this.setStaticIpAddress(config.getStaticIpConfiguration())) {
                            return;
                        } else {
                            linkProperties = config.getStaticIpConfiguration().toLinkProperties(EthernetNetworkFactory.mIface);
                        }
                    } else {
                        EthernetNetworkFactory.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR, null, EthernetNetworkFactory.this.mHwAddr);
                        IpManager.WaitForProvisioningCallback ipmCallback = new IpManager.WaitForProvisioningCallback() {
                            public void onLinkPropertiesChange(LinkProperties newLp) {
                                synchronized (EthernetNetworkFactory.this) {
                                    if (EthernetNetworkFactory.this.mNetworkAgent != null && EthernetNetworkFactory.this.mNetworkInfo.isConnected()) {
                                        EthernetNetworkFactory.this.mLinkProperties = newLp;
                                        EthernetNetworkFactory.this.mNetworkAgent.sendLinkProperties(newLp);
                                    }
                                }
                            }
                        };
                        synchronized (EthernetNetworkFactory.this) {
                            EthernetNetworkFactory.this.stopIpManagerLocked();
                            EthernetNetworkFactory.this.mIpManager = new IpManager(EthernetNetworkFactory.this.mContext, EthernetNetworkFactory.mIface, ipmCallback);
                            if (config.getProxySettings() == IpConfiguration.ProxySettings.STATIC || config.getProxySettings() == IpConfiguration.ProxySettings.PAC) {
                                EthernetNetworkFactory.this.mIpManager.setHttpProxy(config.getHttpProxy());
                            }
                            String tcpBufferSizes = EthernetNetworkFactory.this.mContext.getResources().getString(R.string.DndMmi);
                            if (!TextUtils.isEmpty(tcpBufferSizes)) {
                                EthernetNetworkFactory.this.mIpManager.setTcpBufferSizes(tcpBufferSizes);
                            }
                            IpManager unused = EthernetNetworkFactory.this.mIpManager;
                            IpManager.ProvisioningConfiguration provisioningConfiguration = IpManager.buildProvisioningConfiguration().withProvisioningTimeoutMs(0).build();
                            EthernetNetworkFactory.this.mIpManager.startProvisioning(provisioningConfiguration);
                        }
                        linkProperties = ipmCallback.waitForProvisioning();
                        if (linkProperties == null) {
                            Log.e(EthernetNetworkFactory.TAG, "IP provisioning error");
                            EthernetNetworkFactory.this.mFactory.setScoreFilter(-1);
                            synchronized (EthernetNetworkFactory.this) {
                                EthernetNetworkFactory.this.stopIpManagerLocked();
                            }
                            return;
                        }
                    }
                    synchronized (EthernetNetworkFactory.this) {
                        if (EthernetNetworkFactory.this.mNetworkAgent != null) {
                            Log.e(EthernetNetworkFactory.TAG, "Already have a NetworkAgent - aborting new request");
                            EthernetNetworkFactory.this.stopIpManagerLocked();
                            EthernetNetworkFactory.this.mIpProvisioningThread = null;
                        } else {
                            EthernetNetworkFactory.this.mLinkProperties = linkProperties;
                            EthernetNetworkFactory.this.mNetworkInfo.setIsAvailable(EthernetNetworkFactory.DBG);
                            EthernetNetworkFactory.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, EthernetNetworkFactory.this.mHwAddr);
                            EthernetNetworkFactory.this.mNetworkAgent = new NetworkAgent(EthernetNetworkFactory.this.mFactory.getLooper(), EthernetNetworkFactory.this.mContext, EthernetNetworkFactory.NETWORK_TYPE, EthernetNetworkFactory.this.mNetworkInfo, EthernetNetworkFactory.this.mNetworkCapabilities, EthernetNetworkFactory.this.mLinkProperties, EthernetNetworkFactory.NETWORK_SCORE) {
                                public void unwanted() {
                                    synchronized (EthernetNetworkFactory.this) {
                                        if (this == EthernetNetworkFactory.this.mNetworkAgent) {
                                            EthernetNetworkFactory.this.stopIpManagerLocked();
                                            EthernetNetworkFactory.this.mLinkProperties.clear();
                                            EthernetNetworkFactory.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, EthernetNetworkFactory.this.mHwAddr);
                                            EthernetNetworkFactory.this.updateAgent();
                                            EthernetNetworkFactory.this.mNetworkAgent = null;
                                            try {
                                                EthernetNetworkFactory.this.mNMService.clearInterfaceAddresses(EthernetNetworkFactory.mIface);
                                            } catch (Exception e) {
                                                Log.e(EthernetNetworkFactory.TAG, "Failed to clear addresses or disable ipv6" + e);
                                            }
                                        } else {
                                            Log.d(EthernetNetworkFactory.TAG, "Ignoring unwanted as we have a more modern instance");
                                        }
                                    }
                                }
                            };
                            EthernetNetworkFactory.this.mIpProvisioningThread = null;
                            Log.d(EthernetNetworkFactory.TAG, String.format("exiting ipProvisioningThread(%s): mNetworkInfo=%s", EthernetNetworkFactory.mIface, EthernetNetworkFactory.this.mNetworkInfo));
                        }
                    }
                }
            });
            synchronized (this) {
                if (this.mIpProvisioningThread == null) {
                    this.mIpProvisioningThread = ipProvisioningThread;
                    this.mIpProvisioningThread.start();
                }
            }
        }
    }

    public synchronized void start(Context context, Handler target) {
        IBinder b = ServiceManager.getService("network_management");
        this.mNMService = INetworkManagementService.Stub.asInterface(b);
        this.mEthernetManager = (EthernetManager) context.getSystemService("ethernet");
        mIfaceMatch = context.getResources().getString(R.string.config_systemAmbientAudioIntelligence);
        this.mFactory = new LocalNetworkFactory(NETWORK_TYPE, context, target.getLooper());
        this.mFactory.setCapabilityFilter(this.mNetworkCapabilities);
        this.mFactory.setScoreFilter(-1);
        this.mFactory.register();
        this.mContext = context;
        this.mInterfaceObserver = new InterfaceObserver(this, null);
        try {
            this.mNMService.registerObserver(this.mInterfaceObserver);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not register InterfaceObserver " + e);
        }
        try {
            String[] ifaces = this.mNMService.listInterfaces();
            int i = 0;
            int length = ifaces.length;
            while (true) {
                if (i >= length) {
                    break;
                }
                String iface = ifaces[i];
                synchronized (this) {
                    if (maybeTrackInterface(iface)) {
                        break;
                    }
                }
                i++;
            }
        } catch (RemoteException | IllegalStateException e2) {
            Log.e(TAG, "Could not get list of interfaces " + e2);
        }
    }

    public synchronized void stop() {
        stopIpProvisioningThreadLocked();
        this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, this.mHwAddr);
        mLinkUp = false;
        updateAgent();
        this.mLinkProperties = new LinkProperties();
        this.mNetworkAgent = null;
        setInterfaceInfoLocked("", null);
        this.mNetworkInfo = new NetworkInfo(9, 0, NETWORK_TYPE, "");
        this.mFactory.unregister();
    }

    private void initNetworkCapabilities() {
        this.mNetworkCapabilities = new NetworkCapabilities();
        this.mNetworkCapabilities.addTransportType(3);
        this.mNetworkCapabilities.addCapability(12);
        this.mNetworkCapabilities.addCapability(13);
        this.mNetworkCapabilities.setLinkUpstreamBandwidthKbps(100000);
        this.mNetworkCapabilities.setLinkDownstreamBandwidthKbps(100000);
    }

    public synchronized boolean isTrackingInterface() {
        return TextUtils.isEmpty(mIface) ? false : DBG;
    }

    private void setInterfaceInfoLocked(String iface, String hwAddr) {
        boolean oldAvailable = isTrackingInterface();
        mIface = iface;
        this.mHwAddr = hwAddr;
        boolean available = isTrackingInterface();
        if (oldAvailable == available) {
            return;
        }
        int n = this.mListeners.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                this.mListeners.getBroadcastItem(i).onAvailabilityChanged(available);
            } catch (RemoteException e) {
            }
        }
        this.mListeners.finishBroadcast();
    }

    synchronized void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        if (isTrackingInterface()) {
            pw.println("Tracking interface: " + mIface);
            pw.increaseIndent();
            pw.println("MAC address: " + this.mHwAddr);
            pw.println("Link state: " + (mLinkUp ? "up" : "down"));
            pw.decreaseIndent();
        } else {
            pw.println("Not tracking any interface");
        }
        pw.println();
        pw.println("NetworkInfo: " + this.mNetworkInfo);
        pw.println("LinkProperties: " + this.mLinkProperties);
        pw.println("NetworkAgent: " + this.mNetworkAgent);
        if (this.mIpManager != null) {
            pw.println("IpManager:");
            pw.increaseIndent();
            this.mIpManager.dump(fd, pw, args);
            pw.decreaseIndent();
        }
    }
}
