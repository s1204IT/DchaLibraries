package com.android.bluetooth.pan;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.DhcpResults;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Slog;

public class BluetoothTetheringNetworkFactory extends NetworkFactory {
    private static final int NETWORK_SCORE = 69;
    private static final String NETWORK_TYPE = "Bluetooth Tethering";
    private static final String TAG = "BluetoothTetheringNetworkFactory";
    private final Context mContext;
    private LinkProperties mLinkProperties;
    private NetworkAgent mNetworkAgent;
    private final NetworkCapabilities mNetworkCapabilities;
    private final NetworkInfo mNetworkInfo;
    private final PanService mPanService;

    public BluetoothTetheringNetworkFactory(Context context, Looper looper, PanService panService) {
        super(looper, context, NETWORK_TYPE, new NetworkCapabilities());
        this.mContext = context;
        this.mPanService = panService;
        this.mNetworkInfo = new NetworkInfo(7, 0, NETWORK_TYPE, "");
        this.mLinkProperties = new LinkProperties();
        this.mNetworkCapabilities = new NetworkCapabilities();
        initNetworkCapabilities();
        setCapabilityFilter(this.mNetworkCapabilities);
    }

    protected void startNetwork() {
        Thread dhcpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (BluetoothTetheringNetworkFactory.this) {
                    LinkProperties linkProperties = BluetoothTetheringNetworkFactory.this.mLinkProperties;
                    if (linkProperties.getInterfaceName() != null) {
                        BluetoothTetheringNetworkFactory.this.log("dhcpThread(+" + linkProperties.getInterfaceName() + "): mNetworkInfo=" + BluetoothTetheringNetworkFactory.this.mNetworkInfo);
                        DhcpResults dhcpResults = new DhcpResults();
                        if (!NetworkUtils.runDhcp(linkProperties.getInterfaceName(), dhcpResults)) {
                            Slog.e(BluetoothTetheringNetworkFactory.TAG, "DHCP request error:" + NetworkUtils.getDhcpError());
                            synchronized (BluetoothTetheringNetworkFactory.this) {
                                BluetoothTetheringNetworkFactory.this.setScoreFilter(-1);
                            }
                            return;
                        }
                        synchronized (BluetoothTetheringNetworkFactory.this) {
                            BluetoothTetheringNetworkFactory.this.mLinkProperties = dhcpResults.toLinkProperties(linkProperties.getInterfaceName());
                            BluetoothTetheringNetworkFactory.this.mNetworkInfo.setIsAvailable(true);
                            BluetoothTetheringNetworkFactory.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                            BluetoothTetheringNetworkFactory.this.mNetworkAgent = new NetworkAgent(BluetoothTetheringNetworkFactory.this.getLooper(), BluetoothTetheringNetworkFactory.this.mContext, BluetoothTetheringNetworkFactory.NETWORK_TYPE, BluetoothTetheringNetworkFactory.this.mNetworkInfo, BluetoothTetheringNetworkFactory.this.mNetworkCapabilities, BluetoothTetheringNetworkFactory.this.mLinkProperties, BluetoothTetheringNetworkFactory.NETWORK_SCORE) {
                                public void unwanted() {
                                    BluetoothTetheringNetworkFactory.this.onCancelRequest();
                                }
                            };
                        }
                        return;
                    }
                    Slog.e(BluetoothTetheringNetworkFactory.TAG, "attempted to reverse tether without interface name");
                }
            }
        });
        dhcpThread.start();
    }

    protected void stopNetwork() {
    }

    private synchronized void onCancelRequest() {
        if (!TextUtils.isEmpty(this.mLinkProperties.getInterfaceName())) {
            NetworkUtils.stopDhcp(this.mLinkProperties.getInterfaceName());
        }
        this.mLinkProperties.clear();
        this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent = null;
        }
        for (BluetoothDevice device : this.mPanService.getConnectedDevices()) {
            this.mPanService.disconnect(device);
        }
    }

    public void startReverseTether(String iface) {
        if (iface == null || TextUtils.isEmpty(iface)) {
            Slog.e(TAG, "attempted to reverse tether with empty interface");
            return;
        }
        synchronized (this) {
            if (this.mLinkProperties.getInterfaceName() != null) {
                Slog.e(TAG, "attempted to reverse tether while already in process");
            } else {
                this.mLinkProperties = new LinkProperties();
                this.mLinkProperties.setInterfaceName(iface);
                register();
                setScoreFilter(NETWORK_SCORE);
            }
        }
    }

    public synchronized void stopReverseTether() {
        if (TextUtils.isEmpty(this.mLinkProperties.getInterfaceName())) {
            Slog.e(TAG, "attempted to stop reverse tether with nothing tethered");
        } else {
            onCancelRequest();
            setScoreFilter(-1);
            unregister();
        }
    }

    private void initNetworkCapabilities() {
        this.mNetworkCapabilities.addTransportType(2);
        this.mNetworkCapabilities.addCapability(12);
        this.mNetworkCapabilities.addCapability(13);
        this.mNetworkCapabilities.setLinkUpstreamBandwidthKbps(24000);
        this.mNetworkCapabilities.setLinkDownstreamBandwidthKbps(24000);
    }
}
