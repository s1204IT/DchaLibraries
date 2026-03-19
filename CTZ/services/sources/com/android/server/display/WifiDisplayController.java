package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplaySessionInfo;
import android.media.RemoteDisplay;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;
import android.view.Surface;
import com.android.internal.util.DumpUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;

final class WifiDisplayController implements DumpUtils.Dump {
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    private static final int CONNECT_MAX_RETRIES = 3;
    private static final int CONNECT_RETRY_DELAY_MILLIS = 500;
    private static final boolean DEBUG = false;
    private static final int DEFAULT_CONTROL_PORT = 7236;
    private static final int DISCOVER_PEERS_INTERVAL_MILLIS = 10000;
    private static final int MAX_THROUGHPUT = 50;
    private static final int RTSP_TIMEOUT_SECONDS = 30;
    private static final int RTSP_TIMEOUT_SECONDS_CERT_MODE = 120;
    private static final String TAG = "WifiDisplayController";
    private WifiDisplay mAdvertisedDisplay;
    private int mAdvertisedDisplayFlags;
    private int mAdvertisedDisplayHeight;
    private Surface mAdvertisedDisplaySurface;
    private int mAdvertisedDisplayWidth;
    private WifiP2pDevice mCancelingDevice;
    private WifiP2pDevice mConnectedDevice;
    private WifiP2pGroup mConnectedDeviceGroupInfo;
    private WifiP2pDevice mConnectingDevice;
    private int mConnectionRetriesLeft;
    private final Context mContext;
    private WifiP2pDevice mDesiredDevice;
    private WifiP2pDevice mDisconnectingDevice;
    private boolean mDiscoverPeersInProgress;
    private final Handler mHandler;
    private final Listener mListener;
    private NetworkInfo mNetworkInfo;
    private RemoteDisplay mRemoteDisplay;
    private boolean mRemoteDisplayConnected;
    private String mRemoteDisplayInterface;
    private boolean mScanRequested;
    private WifiP2pDevice mThisDevice;
    private boolean mWfdEnabled;
    private boolean mWfdEnabling;
    private boolean mWifiDisplayCertMode;
    private boolean mWifiDisplayOnSetting;
    private final WifiP2pManager.Channel mWifiP2pChannel;
    private boolean mWifiP2pEnabled;
    private final WifiP2pManager mWifiP2pManager;
    private final ArrayList<WifiP2pDevice> mAvailableWifiDisplayPeers = new ArrayList<>();
    private int mWifiDisplayWpsConfig = 4;
    private final Runnable mDiscoverPeers = new Runnable() {
        @Override
        public void run() {
            WifiDisplayController.this.tryDiscoverPeers();
        }
    };
    private final Runnable mConnectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectingDevice != null && WifiDisplayController.this.mConnectingDevice == WifiDisplayController.this.mDesiredDevice) {
                Slog.i(WifiDisplayController.TAG, "Timed out waiting for Wifi display connection after 30 seconds: " + WifiDisplayController.this.mConnectingDevice.deviceName);
                WifiDisplayController.this.handleConnectionFailure(true);
            }
        }
    };
    private final Runnable mRtspTimeout = new Runnable() {
        @Override
        public void run() {
            if (WifiDisplayController.this.mConnectedDevice != null && WifiDisplayController.this.mRemoteDisplay != null && !WifiDisplayController.this.mRemoteDisplayConnected) {
                Slog.i(WifiDisplayController.TAG, "Timed out waiting for Wifi display RTSP connection after 30 seconds: " + WifiDisplayController.this.mConnectedDevice.deviceName);
                WifiDisplayController.this.handleConnectionFailure(true);
            }
        }
    };
    private final BroadcastReceiver mWifiP2pReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.net.wifi.p2p.STATE_CHANGED")) {
                WifiDisplayController.this.handleStateChanged(intent.getIntExtra("wifi_p2p_state", 1) == 2);
                return;
            }
            if (action.equals("android.net.wifi.p2p.PEERS_CHANGED")) {
                WifiDisplayController.this.handlePeersChanged();
            } else if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")) {
                WifiDisplayController.this.handleConnectionChanged((NetworkInfo) intent.getParcelableExtra("networkInfo"));
            } else if (action.equals("android.net.wifi.p2p.THIS_DEVICE_CHANGED")) {
                WifiDisplayController.this.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
            }
        }
    };

    public interface Listener {
        void onDisplayChanged(WifiDisplay wifiDisplay);

        void onDisplayConnected(WifiDisplay wifiDisplay, Surface surface, int i, int i2, int i3);

        void onDisplayConnecting(WifiDisplay wifiDisplay);

        void onDisplayConnectionFailed();

        void onDisplayDisconnected();

        void onDisplaySessionInfo(WifiDisplaySessionInfo wifiDisplaySessionInfo);

        void onFeatureStateChanged(int i);

        void onScanFinished();

        void onScanResults(WifiDisplay[] wifiDisplayArr);

        void onScanStarted();
    }

    static int access$3220(WifiDisplayController wifiDisplayController, int i) {
        int i2 = wifiDisplayController.mConnectionRetriesLeft - i;
        wifiDisplayController.mConnectionRetriesLeft = i2;
        return i2;
    }

    public WifiDisplayController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mWifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
        this.mWifiP2pChannel = this.mWifiP2pManager.initialize(context, handler.getLooper(), null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.p2p.STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.p2p.PEERS_CHANGED");
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        context.registerReceiver(this.mWifiP2pReceiver, intentFilter, null, this.mHandler);
        ContentObserver contentObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z, Uri uri) {
                WifiDisplayController.this.updateSettings();
            }
        };
        ContentResolver contentResolver = this.mContext.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_on"), false, contentObserver);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_certification_on"), false, contentObserver);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("wifi_display_wps_config"), false, contentObserver);
        updateSettings();
    }

    private void updateSettings() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mWifiDisplayOnSetting = Settings.Global.getInt(contentResolver, "wifi_display_on", 0) != 0;
        this.mWifiDisplayCertMode = Settings.Global.getInt(contentResolver, "wifi_display_certification_on", 0) != 0;
        this.mWifiDisplayWpsConfig = 4;
        if (this.mWifiDisplayCertMode) {
            this.mWifiDisplayWpsConfig = Settings.Global.getInt(contentResolver, "wifi_display_wps_config", 4);
        }
        updateWfdEnableState();
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println("mWifiDisplayOnSetting=" + this.mWifiDisplayOnSetting);
        printWriter.println("mWifiP2pEnabled=" + this.mWifiP2pEnabled);
        printWriter.println("mWfdEnabled=" + this.mWfdEnabled);
        printWriter.println("mWfdEnabling=" + this.mWfdEnabling);
        printWriter.println("mNetworkInfo=" + this.mNetworkInfo);
        printWriter.println("mScanRequested=" + this.mScanRequested);
        printWriter.println("mDiscoverPeersInProgress=" + this.mDiscoverPeersInProgress);
        printWriter.println("mDesiredDevice=" + describeWifiP2pDevice(this.mDesiredDevice));
        printWriter.println("mConnectingDisplay=" + describeWifiP2pDevice(this.mConnectingDevice));
        printWriter.println("mDisconnectingDisplay=" + describeWifiP2pDevice(this.mDisconnectingDevice));
        printWriter.println("mCancelingDisplay=" + describeWifiP2pDevice(this.mCancelingDevice));
        printWriter.println("mConnectedDevice=" + describeWifiP2pDevice(this.mConnectedDevice));
        printWriter.println("mConnectionRetriesLeft=" + this.mConnectionRetriesLeft);
        printWriter.println("mRemoteDisplay=" + this.mRemoteDisplay);
        printWriter.println("mRemoteDisplayInterface=" + this.mRemoteDisplayInterface);
        printWriter.println("mRemoteDisplayConnected=" + this.mRemoteDisplayConnected);
        printWriter.println("mAdvertisedDisplay=" + this.mAdvertisedDisplay);
        printWriter.println("mAdvertisedDisplaySurface=" + this.mAdvertisedDisplaySurface);
        printWriter.println("mAdvertisedDisplayWidth=" + this.mAdvertisedDisplayWidth);
        printWriter.println("mAdvertisedDisplayHeight=" + this.mAdvertisedDisplayHeight);
        printWriter.println("mAdvertisedDisplayFlags=" + this.mAdvertisedDisplayFlags);
        printWriter.println("mAvailableWifiDisplayPeers: size=" + this.mAvailableWifiDisplayPeers.size());
        Iterator<WifiP2pDevice> it = this.mAvailableWifiDisplayPeers.iterator();
        while (it.hasNext()) {
            printWriter.println("  " + describeWifiP2pDevice(it.next()));
        }
    }

    public void requestStartScan() {
        if (!this.mScanRequested) {
            this.mScanRequested = true;
            updateScanState();
        }
    }

    public void requestStopScan() {
        if (this.mScanRequested) {
            this.mScanRequested = false;
            updateScanState();
        }
    }

    public void requestConnect(String str) {
        for (WifiP2pDevice wifiP2pDevice : this.mAvailableWifiDisplayPeers) {
            if (wifiP2pDevice.deviceAddress.equals(str)) {
                connect(wifiP2pDevice);
            }
        }
    }

    public void requestPause() {
        if (this.mRemoteDisplay != null) {
            this.mRemoteDisplay.pause();
        }
    }

    public void requestResume() {
        if (this.mRemoteDisplay != null) {
            this.mRemoteDisplay.resume();
        }
    }

    public void requestDisconnect() {
        disconnect();
    }

    private void updateWfdEnableState() {
        if (this.mWifiDisplayOnSetting && this.mWifiP2pEnabled) {
            if (!this.mWfdEnabled && !this.mWfdEnabling) {
                this.mWfdEnabling = true;
                WifiP2pWfdInfo wifiP2pWfdInfo = new WifiP2pWfdInfo();
                wifiP2pWfdInfo.setWfdEnabled(true);
                wifiP2pWfdInfo.setDeviceType(0);
                wifiP2pWfdInfo.setSessionAvailable(true);
                wifiP2pWfdInfo.setControlPort(DEFAULT_CONTROL_PORT);
                wifiP2pWfdInfo.setMaxThroughput(50);
                this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, wifiP2pWfdInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        if (WifiDisplayController.this.mWfdEnabling) {
                            WifiDisplayController.this.mWfdEnabling = false;
                            WifiDisplayController.this.mWfdEnabled = true;
                            WifiDisplayController.this.reportFeatureState();
                            WifiDisplayController.this.updateScanState();
                        }
                    }

                    @Override
                    public void onFailure(int i) {
                        WifiDisplayController.this.mWfdEnabling = false;
                    }
                });
                return;
            }
            return;
        }
        if (this.mWfdEnabled || this.mWfdEnabling) {
            WifiP2pWfdInfo wifiP2pWfdInfo2 = new WifiP2pWfdInfo();
            wifiP2pWfdInfo2.setWfdEnabled(false);
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, wifiP2pWfdInfo2, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int i) {
                }
            });
        }
        this.mWfdEnabling = false;
        this.mWfdEnabled = false;
        reportFeatureState();
        updateScanState();
        disconnect();
    }

    private void reportFeatureState() {
        final int iComputeFeatureState = computeFeatureState();
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayController.this.mListener.onFeatureStateChanged(iComputeFeatureState);
            }
        });
    }

    private int computeFeatureState() {
        if (this.mWifiP2pEnabled) {
            return this.mWifiDisplayOnSetting ? 3 : 2;
        }
        return 1;
    }

    private void updateScanState() {
        if (this.mScanRequested && this.mWfdEnabled && this.mDesiredDevice == null) {
            if (!this.mDiscoverPeersInProgress) {
                Slog.i(TAG, "Starting Wifi display scan.");
                this.mDiscoverPeersInProgress = true;
                handleScanStarted();
                tryDiscoverPeers();
                return;
            }
            return;
        }
        if (this.mDiscoverPeersInProgress) {
            this.mHandler.removeCallbacks(this.mDiscoverPeers);
            if (this.mDesiredDevice == null || this.mDesiredDevice == this.mConnectedDevice) {
                Slog.i(TAG, "Stopping Wifi display scan.");
                this.mDiscoverPeersInProgress = false;
                stopPeerDiscovery();
                handleScanFinished();
            }
        }
    }

    private void tryDiscoverPeers() {
        this.mWifiP2pManager.discoverPeers(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (WifiDisplayController.this.mDiscoverPeersInProgress) {
                    WifiDisplayController.this.requestPeers();
                }
            }

            @Override
            public void onFailure(int i) {
            }
        });
        this.mHandler.postDelayed(this.mDiscoverPeers, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void stopPeerDiscovery() {
        this.mWifiP2pManager.stopPeerDiscovery(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int i) {
            }
        });
    }

    private void requestPeers() {
        this.mWifiP2pManager.requestPeers(this.mWifiP2pChannel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                WifiDisplayController.this.mAvailableWifiDisplayPeers.clear();
                for (WifiP2pDevice wifiP2pDevice : wifiP2pDeviceList.getDeviceList()) {
                    if (WifiDisplayController.isWifiDisplay(wifiP2pDevice)) {
                        WifiDisplayController.this.mAvailableWifiDisplayPeers.add(wifiP2pDevice);
                    }
                }
                if (WifiDisplayController.this.mDiscoverPeersInProgress) {
                    WifiDisplayController.this.handleScanResults();
                }
            }
        });
    }

    private void handleScanStarted() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayController.this.mListener.onScanStarted();
            }
        });
    }

    private void handleScanResults() {
        int size = this.mAvailableWifiDisplayPeers.size();
        final WifiDisplay[] wifiDisplayArr = (WifiDisplay[]) WifiDisplay.CREATOR.newArray(size);
        for (int i = 0; i < size; i++) {
            WifiP2pDevice wifiP2pDevice = this.mAvailableWifiDisplayPeers.get(i);
            wifiDisplayArr[i] = createWifiDisplay(wifiP2pDevice);
            updateDesiredDevice(wifiP2pDevice);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayController.this.mListener.onScanResults(wifiDisplayArr);
            }
        });
    }

    private void handleScanFinished() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayController.this.mListener.onScanFinished();
            }
        });
    }

    private void updateDesiredDevice(WifiP2pDevice wifiP2pDevice) {
        String str = wifiP2pDevice.deviceAddress;
        if (this.mDesiredDevice != null && this.mDesiredDevice.deviceAddress.equals(str)) {
            this.mDesiredDevice.update(wifiP2pDevice);
            if (this.mAdvertisedDisplay != null && this.mAdvertisedDisplay.getDeviceAddress().equals(str)) {
                readvertiseDisplay(createWifiDisplay(this.mDesiredDevice));
            }
        }
    }

    private void connect(WifiP2pDevice wifiP2pDevice) {
        if (this.mDesiredDevice != null && !this.mDesiredDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
            return;
        }
        if (this.mConnectedDevice != null && !this.mConnectedDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress) && this.mDesiredDevice == null) {
            return;
        }
        if (!this.mWfdEnabled) {
            Slog.i(TAG, "Ignoring request to connect to Wifi display because the  feature is currently disabled: " + wifiP2pDevice.deviceName);
            return;
        }
        this.mDesiredDevice = wifiP2pDevice;
        this.mConnectionRetriesLeft = 3;
        updateConnection();
    }

    private void disconnect() {
        this.mDesiredDevice = null;
        updateConnection();
    }

    private void retryConnection() {
        this.mDesiredDevice = new WifiP2pDevice(this.mDesiredDevice);
        updateConnection();
    }

    private void updateConnection() {
        updateScanState();
        if (this.mRemoteDisplay != null && this.mConnectedDevice != this.mDesiredDevice) {
            Slog.i(TAG, "Stopped listening for RTSP connection on " + this.mRemoteDisplayInterface + " from Wifi display: " + this.mConnectedDevice.deviceName);
            this.mRemoteDisplay.dispose();
            this.mRemoteDisplay = null;
            this.mRemoteDisplayInterface = null;
            this.mRemoteDisplayConnected = false;
            this.mHandler.removeCallbacks(this.mRtspTimeout);
            this.mWifiP2pManager.setMiracastMode(0);
            unadvertiseDisplay();
        }
        if (this.mDisconnectingDevice != null) {
            return;
        }
        if (this.mConnectedDevice != null && this.mConnectedDevice != this.mDesiredDevice) {
            Slog.i(TAG, "Disconnecting from Wifi display: " + this.mConnectedDevice.deviceName);
            this.mDisconnectingDevice = this.mConnectedDevice;
            this.mConnectedDevice = null;
            this.mConnectedDeviceGroupInfo = null;
            unadvertiseDisplay();
            final WifiP2pDevice wifiP2pDevice = this.mDisconnectingDevice;
            this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(WifiDisplayController.TAG, "Disconnected from Wifi display: " + wifiP2pDevice.deviceName);
                    next();
                }

                @Override
                public void onFailure(int i) {
                    Slog.i(WifiDisplayController.TAG, "Failed to disconnect from Wifi display: " + wifiP2pDevice.deviceName + ", reason=" + i);
                    next();
                }

                private void next() {
                    if (WifiDisplayController.this.mDisconnectingDevice == wifiP2pDevice) {
                        WifiDisplayController.this.mDisconnectingDevice = null;
                        WifiDisplayController.this.updateConnection();
                    }
                }
            });
            return;
        }
        if (this.mCancelingDevice != null) {
            return;
        }
        if (this.mConnectingDevice != null && this.mConnectingDevice != this.mDesiredDevice) {
            Slog.i(TAG, "Canceling connection to Wifi display: " + this.mConnectingDevice.deviceName);
            this.mCancelingDevice = this.mConnectingDevice;
            this.mConnectingDevice = null;
            unadvertiseDisplay();
            this.mHandler.removeCallbacks(this.mConnectionTimeout);
            final WifiP2pDevice wifiP2pDevice2 = this.mCancelingDevice;
            this.mWifiP2pManager.cancelConnect(this.mWifiP2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(WifiDisplayController.TAG, "Canceled connection to Wifi display: " + wifiP2pDevice2.deviceName);
                    next();
                }

                @Override
                public void onFailure(int i) {
                    Slog.i(WifiDisplayController.TAG, "Failed to cancel connection to Wifi display: " + wifiP2pDevice2.deviceName + ", reason=" + i);
                    next();
                }

                private void next() {
                    if (WifiDisplayController.this.mCancelingDevice == wifiP2pDevice2) {
                        WifiDisplayController.this.mCancelingDevice = null;
                        WifiDisplayController.this.updateConnection();
                    }
                }
            });
            return;
        }
        if (this.mDesiredDevice == null) {
            if (this.mWifiDisplayCertMode) {
                this.mListener.onDisplaySessionInfo(getSessionInfo(this.mConnectedDeviceGroupInfo, 0));
            }
            unadvertiseDisplay();
            return;
        }
        if (this.mConnectedDevice == null && this.mConnectingDevice == null) {
            Slog.i(TAG, "Connecting to Wifi display: " + this.mDesiredDevice.deviceName);
            this.mConnectingDevice = this.mDesiredDevice;
            WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
            WpsInfo wpsInfo = new WpsInfo();
            if (this.mWifiDisplayWpsConfig != 4) {
                wpsInfo.setup = this.mWifiDisplayWpsConfig;
            } else if (this.mConnectingDevice.wpsPbcSupported()) {
                wpsInfo.setup = 0;
            } else if (this.mConnectingDevice.wpsDisplaySupported()) {
                wpsInfo.setup = 2;
            } else {
                wpsInfo.setup = 1;
            }
            wifiP2pConfig.wps = wpsInfo;
            wifiP2pConfig.deviceAddress = this.mConnectingDevice.deviceAddress;
            wifiP2pConfig.groupOwnerIntent = 0;
            advertiseDisplay(createWifiDisplay(this.mConnectingDevice), null, 0, 0, 0);
            final WifiP2pDevice wifiP2pDevice3 = this.mDesiredDevice;
            this.mWifiP2pManager.connect(this.mWifiP2pChannel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Slog.i(WifiDisplayController.TAG, "Initiated connection to Wifi display: " + wifiP2pDevice3.deviceName);
                    WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mConnectionTimeout, 30000L);
                }

                @Override
                public void onFailure(int i) {
                    if (WifiDisplayController.this.mConnectingDevice == wifiP2pDevice3) {
                        Slog.i(WifiDisplayController.TAG, "Failed to initiate connection to Wifi display: " + wifiP2pDevice3.deviceName + ", reason=" + i);
                        WifiDisplayController.this.mConnectingDevice = null;
                        WifiDisplayController.this.handleConnectionFailure(false);
                    }
                }
            });
            return;
        }
        if (this.mConnectedDevice != null && this.mRemoteDisplay == null) {
            Inet4Address interfaceAddress = getInterfaceAddress(this.mConnectedDeviceGroupInfo);
            if (interfaceAddress == null) {
                Slog.i(TAG, "Failed to get local interface address for communicating with Wifi display: " + this.mConnectedDevice.deviceName);
                handleConnectionFailure(false);
                return;
            }
            this.mWifiP2pManager.setMiracastMode(1);
            final WifiP2pDevice wifiP2pDevice4 = this.mConnectedDevice;
            String str = interfaceAddress.getHostAddress() + ":" + getPortNumber(this.mConnectedDevice);
            this.mRemoteDisplayInterface = str;
            Slog.i(TAG, "Listening for RTSP connection on " + str + " from Wifi display: " + this.mConnectedDevice.deviceName);
            this.mRemoteDisplay = RemoteDisplay.listen(str, new RemoteDisplay.Listener() {
                public void onDisplayConnected(Surface surface, int i, int i2, int i3, int i4) {
                    if (WifiDisplayController.this.mConnectedDevice == wifiP2pDevice4 && !WifiDisplayController.this.mRemoteDisplayConnected) {
                        Slog.i(WifiDisplayController.TAG, "Opened RTSP connection with Wifi display: " + WifiDisplayController.this.mConnectedDevice.deviceName);
                        WifiDisplayController.this.mRemoteDisplayConnected = true;
                        WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                        if (WifiDisplayController.this.mWifiDisplayCertMode) {
                            WifiDisplayController.this.mListener.onDisplaySessionInfo(WifiDisplayController.this.getSessionInfo(WifiDisplayController.this.mConnectedDeviceGroupInfo, i4));
                        }
                        WifiDisplayController.this.advertiseDisplay(WifiDisplayController.createWifiDisplay(WifiDisplayController.this.mConnectedDevice), surface, i, i2, i3);
                    }
                }

                public void onDisplayDisconnected() {
                    if (WifiDisplayController.this.mConnectedDevice == wifiP2pDevice4) {
                        Slog.i(WifiDisplayController.TAG, "Closed RTSP connection with Wifi display: " + WifiDisplayController.this.mConnectedDevice.deviceName);
                        WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                        WifiDisplayController.this.disconnect();
                    }
                }

                public void onDisplayError(int i) {
                    if (WifiDisplayController.this.mConnectedDevice == wifiP2pDevice4) {
                        Slog.i(WifiDisplayController.TAG, "Lost RTSP connection with Wifi display due to error " + i + ": " + WifiDisplayController.this.mConnectedDevice.deviceName);
                        WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                        WifiDisplayController.this.handleConnectionFailure(false);
                    }
                }

                public void onDisplayKeyEvent(int i, int i2) {
                    Slog.d(WifiDisplayController.TAG, "onDisplayKeyEvent:uniCode=" + i + "flags" + i2);
                }

                public void onDisplayGenericMsgEvent(int i) {
                    Slog.d(WifiDisplayController.TAG, "onDisplayGenericMsgEvent: " + i);
                }
            }, this.mHandler, this.mContext.getOpPackageName());
            this.mHandler.postDelayed(this.mRtspTimeout, (this.mWifiDisplayCertMode ? RTSP_TIMEOUT_SECONDS_CERT_MODE : 30) * 1000);
        }
    }

    private WifiDisplaySessionInfo getSessionInfo(WifiP2pGroup wifiP2pGroup, int i) {
        if (wifiP2pGroup == null) {
            return null;
        }
        Inet4Address interfaceAddress = getInterfaceAddress(wifiP2pGroup);
        return new WifiDisplaySessionInfo(!wifiP2pGroup.getOwner().deviceAddress.equals(this.mThisDevice.deviceAddress), i, wifiP2pGroup.getOwner().deviceAddress + " " + wifiP2pGroup.getNetworkName(), wifiP2pGroup.getPassphrase(), interfaceAddress != null ? interfaceAddress.getHostAddress() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    }

    private void handleStateChanged(boolean z) {
        this.mWifiP2pEnabled = z;
        updateWfdEnableState();
    }

    private void handlePeersChanged() {
        requestPeers();
    }

    private void handleConnectionChanged(NetworkInfo networkInfo) {
        this.mNetworkInfo = networkInfo;
        if (this.mWfdEnabled && networkInfo.isConnected()) {
            if (this.mDesiredDevice != null || this.mWifiDisplayCertMode) {
                this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                        if (WifiDisplayController.this.mConnectingDevice == null || wifiP2pGroup.contains(WifiDisplayController.this.mConnectingDevice)) {
                            if (WifiDisplayController.this.mDesiredDevice == null || wifiP2pGroup.contains(WifiDisplayController.this.mDesiredDevice)) {
                                if (WifiDisplayController.this.mWifiDisplayCertMode) {
                                    boolean zEquals = wifiP2pGroup.getOwner().deviceAddress.equals(WifiDisplayController.this.mThisDevice.deviceAddress);
                                    if (!zEquals || !wifiP2pGroup.getClientList().isEmpty()) {
                                        if (WifiDisplayController.this.mConnectingDevice == null && WifiDisplayController.this.mDesiredDevice == null) {
                                            WifiDisplayController.this.mConnectingDevice = WifiDisplayController.this.mDesiredDevice = zEquals ? wifiP2pGroup.getClientList().iterator().next() : wifiP2pGroup.getOwner();
                                        }
                                    } else {
                                        WifiDisplayController.this.mConnectingDevice = WifiDisplayController.this.mDesiredDevice = null;
                                        WifiDisplayController.this.mConnectedDeviceGroupInfo = wifiP2pGroup;
                                        WifiDisplayController.this.updateConnection();
                                    }
                                }
                                if (WifiDisplayController.this.mConnectingDevice != null && WifiDisplayController.this.mConnectingDevice == WifiDisplayController.this.mDesiredDevice) {
                                    Slog.i(WifiDisplayController.TAG, "Connected to Wifi display: " + WifiDisplayController.this.mConnectingDevice.deviceName);
                                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mConnectionTimeout);
                                    WifiDisplayController.this.mConnectedDeviceGroupInfo = wifiP2pGroup;
                                    WifiDisplayController.this.mConnectedDevice = WifiDisplayController.this.mConnectingDevice;
                                    WifiDisplayController.this.mConnectingDevice = null;
                                    WifiDisplayController.this.updateConnection();
                                    return;
                                }
                                return;
                            }
                            WifiDisplayController.this.disconnect();
                            return;
                        }
                        Slog.i(WifiDisplayController.TAG, "Aborting connection to Wifi display because the current P2P group does not contain the device we expected to find: " + WifiDisplayController.this.mConnectingDevice.deviceName + ", group info was: " + WifiDisplayController.describeWifiP2pGroup(wifiP2pGroup));
                        WifiDisplayController.this.handleConnectionFailure(false);
                    }
                });
                return;
            }
            return;
        }
        this.mConnectedDeviceGroupInfo = null;
        if (this.mConnectingDevice != null || this.mConnectedDevice != null) {
            disconnect();
        }
        if (this.mWfdEnabled) {
            requestPeers();
        }
    }

    private void handleConnectionFailure(boolean z) {
        Slog.i(TAG, "Wifi display connection failed!");
        if (this.mDesiredDevice != null) {
            if (this.mConnectionRetriesLeft > 0) {
                final WifiP2pDevice wifiP2pDevice = this.mDesiredDevice;
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (WifiDisplayController.this.mDesiredDevice == wifiP2pDevice && WifiDisplayController.this.mConnectionRetriesLeft > 0) {
                            WifiDisplayController.access$3220(WifiDisplayController.this, 1);
                            Slog.i(WifiDisplayController.TAG, "Retrying Wifi display connection.  Retries left: " + WifiDisplayController.this.mConnectionRetriesLeft);
                            WifiDisplayController.this.retryConnection();
                        }
                    }
                }, z ? 0L : 500L);
            } else {
                disconnect();
            }
        }
    }

    private void advertiseDisplay(final WifiDisplay wifiDisplay, final Surface surface, final int i, final int i2, final int i3) {
        if (!Objects.equals(this.mAdvertisedDisplay, wifiDisplay) || this.mAdvertisedDisplaySurface != surface || this.mAdvertisedDisplayWidth != i || this.mAdvertisedDisplayHeight != i2 || this.mAdvertisedDisplayFlags != i3) {
            final WifiDisplay wifiDisplay2 = this.mAdvertisedDisplay;
            final Surface surface2 = this.mAdvertisedDisplaySurface;
            this.mAdvertisedDisplay = wifiDisplay;
            this.mAdvertisedDisplaySurface = surface;
            this.mAdvertisedDisplayWidth = i;
            this.mAdvertisedDisplayHeight = i2;
            this.mAdvertisedDisplayFlags = i3;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (surface2 != null && surface != surface2) {
                        WifiDisplayController.this.mListener.onDisplayDisconnected();
                    } else if (wifiDisplay2 != null && !wifiDisplay2.hasSameAddress(wifiDisplay)) {
                        WifiDisplayController.this.mListener.onDisplayConnectionFailed();
                    }
                    if (wifiDisplay != null) {
                        if (!wifiDisplay.hasSameAddress(wifiDisplay2)) {
                            WifiDisplayController.this.mListener.onDisplayConnecting(wifiDisplay);
                        } else if (!wifiDisplay.equals(wifiDisplay2)) {
                            WifiDisplayController.this.mListener.onDisplayChanged(wifiDisplay);
                        }
                        if (surface != null && surface != surface2) {
                            WifiDisplayController.this.mListener.onDisplayConnected(wifiDisplay, surface, i, i2, i3);
                        }
                    }
                }
            });
        }
    }

    private void unadvertiseDisplay() {
        advertiseDisplay(null, null, 0, 0, 0);
    }

    private void readvertiseDisplay(WifiDisplay wifiDisplay) {
        advertiseDisplay(wifiDisplay, this.mAdvertisedDisplaySurface, this.mAdvertisedDisplayWidth, this.mAdvertisedDisplayHeight, this.mAdvertisedDisplayFlags);
    }

    private static Inet4Address getInterfaceAddress(WifiP2pGroup wifiP2pGroup) {
        try {
            Enumeration<InetAddress> inetAddresses = NetworkInterface.getByName(wifiP2pGroup.getInterface()).getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddressNextElement = inetAddresses.nextElement();
                if (inetAddressNextElement instanceof Inet4Address) {
                    return (Inet4Address) inetAddressNextElement;
                }
            }
            Slog.w(TAG, "Could not obtain address of network interface " + wifiP2pGroup.getInterface() + " because it had no IPv4 addresses.");
            return null;
        } catch (SocketException e) {
            Slog.w(TAG, "Could not obtain address of network interface " + wifiP2pGroup.getInterface(), e);
            return null;
        }
    }

    private static int getPortNumber(WifiP2pDevice wifiP2pDevice) {
        if (wifiP2pDevice.deviceName.startsWith("DIRECT-") && wifiP2pDevice.deviceName.endsWith("Broadcom")) {
            return 8554;
        }
        return DEFAULT_CONTROL_PORT;
    }

    private static boolean isWifiDisplay(WifiP2pDevice wifiP2pDevice) {
        return wifiP2pDevice.wfdInfo != null && wifiP2pDevice.wfdInfo.isWfdEnabled() && isPrimarySinkDeviceType(wifiP2pDevice.wfdInfo.getDeviceType());
    }

    private static boolean isPrimarySinkDeviceType(int i) {
        return i == 1 || i == 3;
    }

    private static String describeWifiP2pDevice(WifiP2pDevice wifiP2pDevice) {
        return wifiP2pDevice != null ? wifiP2pDevice.toString().replace('\n', ',') : "null";
    }

    private static String describeWifiP2pGroup(WifiP2pGroup wifiP2pGroup) {
        return wifiP2pGroup != null ? wifiP2pGroup.toString().replace('\n', ',') : "null";
    }

    private static WifiDisplay createWifiDisplay(WifiP2pDevice wifiP2pDevice) {
        return new WifiDisplay(wifiP2pDevice.deviceAddress, wifiP2pDevice.deviceName, (String) null, true, wifiP2pDevice.wfdInfo.isSessionAvailable(), false);
    }
}
