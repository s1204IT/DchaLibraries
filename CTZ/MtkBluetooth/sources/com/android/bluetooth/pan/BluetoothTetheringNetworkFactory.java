package com.android.bluetooth.pan;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.ip.IpClient;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Slog;
import java.util.Iterator;

public class BluetoothTetheringNetworkFactory extends NetworkFactory {
    private static final int NETWORK_SCORE = 69;
    private static final String NETWORK_TYPE = "Bluetooth Tethering";
    private static final String TAG = "BluetoothTetheringNetworkFactory";
    private final Context mContext;
    private String mInterfaceName;
    private IpClient mIpClient;
    private NetworkAgent mNetworkAgent;
    private final NetworkCapabilities mNetworkCapabilities;
    private final NetworkInfo mNetworkInfo;
    private final PanService mPanService;

    public BluetoothTetheringNetworkFactory(Context context, Looper looper, PanService panService) {
        super(looper, context, NETWORK_TYPE, new NetworkCapabilities());
        this.mContext = context;
        this.mPanService = panService;
        this.mNetworkInfo = new NetworkInfo(7, 0, NETWORK_TYPE, "");
        this.mNetworkCapabilities = new NetworkCapabilities();
        initNetworkCapabilities();
        setCapabilityFilter(this.mNetworkCapabilities);
    }

    private void stopIpClientLocked() {
        if (this.mIpClient != null) {
            this.mIpClient.shutdown();
            this.mIpClient = null;
        }
    }

    protected void startNetwork() {
        if (this.mNetworkAgent != null) {
            log("Ignore startNetwork and still keep the current network agent " + this.mNetworkAgent);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                IpClient.WaitForProvisioningCallback waitForProvisioningCallback = new IpClient.WaitForProvisioningCallback() {
                    @Override
                    public void onLinkPropertiesChange(LinkProperties linkProperties) {
                        synchronized (BluetoothTetheringNetworkFactory.this) {
                            if (BluetoothTetheringNetworkFactory.this.mNetworkAgent != null && BluetoothTetheringNetworkFactory.this.mNetworkInfo.isConnected()) {
                                BluetoothTetheringNetworkFactory.this.mNetworkAgent.sendLinkProperties(linkProperties);
                            }
                        }
                    }
                };
                synchronized (BluetoothTetheringNetworkFactory.this) {
                    if (TextUtils.isEmpty(BluetoothTetheringNetworkFactory.this.mInterfaceName)) {
                        Slog.e(BluetoothTetheringNetworkFactory.TAG, "attempted to reverse tether without interface name");
                        return;
                    }
                    BluetoothTetheringNetworkFactory.this.log("ipProvisioningThread(+" + BluetoothTetheringNetworkFactory.this.mInterfaceName + "): mNetworkInfo=" + BluetoothTetheringNetworkFactory.this.mNetworkInfo);
                    BluetoothTetheringNetworkFactory.this.mIpClient = new IpClient(BluetoothTetheringNetworkFactory.this.mContext, BluetoothTetheringNetworkFactory.this.mInterfaceName, waitForProvisioningCallback);
                    IpClient ipClient = BluetoothTetheringNetworkFactory.this.mIpClient;
                    IpClient unused = BluetoothTetheringNetworkFactory.this.mIpClient;
                    ipClient.startProvisioning(IpClient.buildProvisioningConfiguration().withoutMultinetworkPolicyTracker().withoutIpReachabilityMonitor().build());
                    BluetoothTetheringNetworkFactory.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR, null, null);
                    LinkProperties linkPropertiesWaitForProvisioning = waitForProvisioningCallback.waitForProvisioning();
                    if (linkPropertiesWaitForProvisioning == null) {
                        Slog.e(BluetoothTetheringNetworkFactory.TAG, "IP provisioning error.");
                        synchronized (BluetoothTetheringNetworkFactory.this) {
                            BluetoothTetheringNetworkFactory.this.stopIpClientLocked();
                            BluetoothTetheringNetworkFactory.this.setScoreFilter(-1);
                        }
                        return;
                    }
                    synchronized (BluetoothTetheringNetworkFactory.this) {
                        BluetoothTetheringNetworkFactory.this.mNetworkInfo.setIsAvailable(true);
                        BluetoothTetheringNetworkFactory.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                        BluetoothTetheringNetworkFactory.this.mNetworkAgent = new NetworkAgent(BluetoothTetheringNetworkFactory.this.getLooper(), BluetoothTetheringNetworkFactory.this.mContext, BluetoothTetheringNetworkFactory.NETWORK_TYPE, BluetoothTetheringNetworkFactory.this.mNetworkInfo, BluetoothTetheringNetworkFactory.this.mNetworkCapabilities, linkPropertiesWaitForProvisioning, 69) {
                            public void unwanted() {
                                BluetoothTetheringNetworkFactory.this.onCancelRequest();
                            }
                        };
                    }
                }
            }
        }).start();
    }

    protected void stopNetwork() {
    }

    private synchronized void onCancelRequest() {
        stopIpClientLocked();
        this.mInterfaceName = "";
        this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent = null;
        }
        Iterator<BluetoothDevice> it = this.mPanService.getConnectedDevices().iterator();
        while (it.hasNext()) {
            this.mPanService.disconnect(it.next());
        }
    }

    public void startReverseTether(String str) {
        if (str == null || TextUtils.isEmpty(str)) {
            Slog.e(TAG, "attempted to reverse tether with empty interface");
            return;
        }
        synchronized (this) {
            if (!TextUtils.isEmpty(this.mInterfaceName)) {
                Slog.e(TAG, "attempted to reverse tether while already in process");
                return;
            }
            this.mInterfaceName = str;
            register();
            setScoreFilter(69);
        }
    }

    public synchronized void stopReverseTether() {
        if (TextUtils.isEmpty(this.mInterfaceName)) {
            Slog.e(TAG, "attempted to stop reverse tether with nothing tethered");
            return;
        }
        onCancelRequest();
        setScoreFilter(-1);
        unregister();
    }

    private void initNetworkCapabilities() {
        this.mNetworkCapabilities.addTransportType(2);
        this.mNetworkCapabilities.addCapability(12);
        this.mNetworkCapabilities.addCapability(13);
        this.mNetworkCapabilities.addCapability(18);
        this.mNetworkCapabilities.setLinkUpstreamBandwidthKbps(24000);
        this.mNetworkCapabilities.setLinkDownstreamBandwidthKbps(24000);
    }
}
