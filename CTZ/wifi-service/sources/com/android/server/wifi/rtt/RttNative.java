package com.android.server.wifi.rtt;

import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiRttControllerEventCallback;
import android.hardware.wifi.V1_0.RttCapabilities;
import android.hardware.wifi.V1_0.RttConfig;
import android.hardware.wifi.V1_0.RttResult;
import android.hardware.wifi.V1_0.WifiStatus;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.ResponderConfig;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.HalDeviceManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ListIterator;

public class RttNative extends IWifiRttControllerEventCallback.Stub {
    private static final String TAG = "RttNative";
    private static final boolean VDBG = false;
    private final HalDeviceManager mHalDeviceManager;
    private volatile IWifiRttController mIWifiRttController;
    private volatile RttCapabilities mRttCapabilities;
    private final RttServiceImpl mRttService;
    boolean mDbg = false;
    private Object mLock = new Object();

    public RttNative(RttServiceImpl rttServiceImpl, HalDeviceManager halDeviceManager) {
        this.mRttService = rttServiceImpl;
        this.mHalDeviceManager = halDeviceManager;
    }

    public void start(Handler handler) {
        synchronized (this.mLock) {
            this.mHalDeviceManager.initialize();
            this.mHalDeviceManager.registerStatusListener(new HalDeviceManager.ManagerStatusListener() {
                @Override
                public final void onStatusChanged() {
                    this.f$0.updateController();
                }
            }, handler);
            updateController();
        }
    }

    public boolean isReady() {
        return this.mIWifiRttController != null;
    }

    private void updateController() {
        if (this.mDbg) {
            Log.v(TAG, "updateController: mIWifiRttController=" + this.mIWifiRttController);
        }
        synchronized (this.mLock) {
            IWifiRttController iWifiRttControllerCreateRttController = this.mIWifiRttController;
            if (this.mHalDeviceManager.isStarted()) {
                if (iWifiRttControllerCreateRttController == null) {
                    iWifiRttControllerCreateRttController = this.mHalDeviceManager.createRttController();
                    if (iWifiRttControllerCreateRttController == null) {
                        Log.e(TAG, "updateController: Failed creating RTT controller - but Wifi is started!");
                    } else {
                        try {
                            iWifiRttControllerCreateRttController.registerEventCallback(this);
                        } catch (RemoteException e) {
                            Log.e(TAG, "updateController: exception registering callback: " + e);
                            iWifiRttControllerCreateRttController = null;
                        }
                    }
                    this.mIWifiRttController = iWifiRttControllerCreateRttController;
                    if (this.mIWifiRttController == null) {
                    }
                } else {
                    this.mIWifiRttController = iWifiRttControllerCreateRttController;
                    if (this.mIWifiRttController == null) {
                        this.mRttService.disable();
                    } else {
                        this.mRttService.enableIfPossible();
                        updateRttCapabilities();
                    }
                }
            } else {
                iWifiRttControllerCreateRttController = null;
                this.mIWifiRttController = iWifiRttControllerCreateRttController;
                if (this.mIWifiRttController == null) {
                }
            }
        }
    }

    void updateRttCapabilities() {
        if (this.mRttCapabilities != null) {
            return;
        }
        synchronized (this.mLock) {
            try {
                this.mIWifiRttController.getCapabilities(new IWifiRttController.getCapabilitiesCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
                        RttNative.lambda$updateRttCapabilities$1(this.f$0, wifiStatus, rttCapabilities);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "updateController: exception requesting capabilities: " + e);
            }
            if (this.mRttCapabilities != null && !this.mRttCapabilities.rttFtmSupported) {
                Log.wtf(TAG, "Firmware indicates RTT is not supported - but device supports RTT - ignored!?");
            }
        }
    }

    public static void lambda$updateRttCapabilities$1(RttNative rttNative, WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
        if (wifiStatus.code != 0) {
            Log.e(TAG, "updateController: error requesting capabilities -- code=" + wifiStatus.code);
            return;
        }
        if (rttNative.mDbg) {
            Log.v(TAG, "updateController: RTT capabilities=" + rttCapabilities);
        }
        rttNative.mRttCapabilities = rttCapabilities;
    }

    public boolean rangeRequest(int i, RangingRequest rangingRequest, boolean z) {
        if (this.mDbg) {
            Log.v(TAG, "rangeRequest: cmdId=" + i + ", # of requests=" + rangingRequest.mRttPeers.size());
        }
        updateRttCapabilities();
        synchronized (this.mLock) {
            if (!isReady()) {
                Log.e(TAG, "rangeRequest: RttController is null");
                return false;
            }
            ArrayList<RttConfig> arrayListConvertRangingRequestToRttConfigs = convertRangingRequestToRttConfigs(rangingRequest, z, this.mRttCapabilities);
            if (arrayListConvertRangingRequestToRttConfigs == null) {
                Log.e(TAG, "rangeRequest: invalid request parameters");
                return false;
            }
            if (arrayListConvertRangingRequestToRttConfigs.size() == 0) {
                Log.e(TAG, "rangeRequest: all requests invalidated");
                this.mRttService.onRangingResults(i, new ArrayList());
                return true;
            }
            try {
                WifiStatus wifiStatusRangeRequest = this.mIWifiRttController.rangeRequest(i, arrayListConvertRangingRequestToRttConfigs);
                if (wifiStatusRangeRequest.code == 0) {
                    return true;
                }
                Log.e(TAG, "rangeRequest: cannot issue range request -- code=" + wifiStatusRangeRequest.code);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "rangeRequest: exception issuing range request: " + e);
                return false;
            }
        }
    }

    public boolean rangeCancel(int i, ArrayList<byte[]> arrayList) {
        if (this.mDbg) {
            Log.v(TAG, "rangeCancel: cmdId=" + i);
        }
        synchronized (this.mLock) {
            if (!isReady()) {
                Log.e(TAG, "rangeCancel: RttController is null");
                return false;
            }
            try {
                WifiStatus wifiStatusRangeCancel = this.mIWifiRttController.rangeCancel(i, arrayList);
                if (wifiStatusRangeCancel.code != 0) {
                    Log.e(TAG, "rangeCancel: cannot issue range cancel -- code=" + wifiStatusRangeCancel.code);
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "rangeCancel: exception issuing range cancel: " + e);
                return false;
            }
        }
    }

    @Override
    public void onResults(int i, ArrayList<RttResult> arrayList) {
        if (this.mDbg) {
            Log.v(TAG, "onResults: cmdId=" + i + ", # of results=" + arrayList.size());
        }
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        ListIterator<RttResult> listIterator = arrayList.listIterator();
        while (listIterator.hasNext()) {
            if (listIterator.next() == null) {
                listIterator.remove();
            }
        }
        this.mRttService.onRangingResults(i, arrayList);
    }

    private static ArrayList<RttConfig> convertRangingRequestToRttConfigs(RangingRequest rangingRequest, boolean z, RttCapabilities rttCapabilities) {
        boolean z2;
        ArrayList<RttConfig> arrayList = new ArrayList<>(rangingRequest.mRttPeers.size());
        for (ResponderConfig responderConfig : rangingRequest.mRttPeers) {
            if (!z && !responderConfig.supports80211mc) {
                Log.e(TAG, "Invalid responder: does not support 802.11mc");
            } else {
                RttConfig rttConfig = new RttConfig();
                System.arraycopy(responderConfig.macAddress.toByteArray(), 0, rttConfig.addr, 0, rttConfig.addr.length);
                try {
                    z2 = true;
                    rttConfig.type = responderConfig.supports80211mc ? 2 : 1;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid configuration: " + e.getMessage());
                }
                if (rttConfig.type == 1 && rttCapabilities != null && !rttCapabilities.rttOneSidedSupported) {
                    Log.w(TAG, "Device does not support one-sided RTT");
                } else {
                    rttConfig.peer = halRttPeerTypeFromResponderType(responderConfig.responderType);
                    rttConfig.channel.width = halChannelWidthFromResponderChannelWidth(responderConfig.channelWidth);
                    rttConfig.channel.centerFreq = responderConfig.frequency;
                    rttConfig.channel.centerFreq0 = responderConfig.centerFreq0;
                    rttConfig.channel.centerFreq1 = responderConfig.centerFreq1;
                    rttConfig.bw = halRttChannelBandwidthFromResponderChannelWidth(responderConfig.channelWidth);
                    rttConfig.preamble = halRttPreambleFromResponderPreamble(responderConfig.preamble);
                    if (rttConfig.peer == 5) {
                        rttConfig.mustRequestLci = false;
                        rttConfig.mustRequestLcr = false;
                        rttConfig.burstPeriod = 0;
                        rttConfig.numBurst = 0;
                        rttConfig.numFramesPerBurst = 5;
                        rttConfig.numRetriesPerRttFrame = 0;
                        rttConfig.numRetriesPerFtmr = 3;
                        rttConfig.burstDuration = 9;
                    } else {
                        rttConfig.mustRequestLci = z;
                        rttConfig.mustRequestLcr = z;
                        rttConfig.burstPeriod = 0;
                        rttConfig.numBurst = 0;
                        rttConfig.numFramesPerBurst = 8;
                        rttConfig.numRetriesPerRttFrame = rttConfig.type == 2 ? 0 : 3;
                        rttConfig.numRetriesPerFtmr = 3;
                        rttConfig.burstDuration = 9;
                        if (rttCapabilities != null) {
                            rttConfig.mustRequestLci = rttConfig.mustRequestLci && rttCapabilities.lciSupported;
                            if (!rttConfig.mustRequestLcr || !rttCapabilities.lcrSupported) {
                                z2 = false;
                            }
                            rttConfig.mustRequestLcr = z2;
                            rttConfig.bw = halRttChannelBandwidthCapabilityLimiter(rttConfig.bw, rttCapabilities);
                            rttConfig.preamble = halRttPreambleCapabilityLimiter(rttConfig.preamble, rttCapabilities);
                        }
                    }
                    arrayList.add(rttConfig);
                }
            }
        }
        return arrayList;
    }

    private static int halRttPeerTypeFromResponderType(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            default:
                throw new IllegalArgumentException("halRttPeerTypeFromResponderType: bad " + i);
        }
    }

    private static int halChannelWidthFromResponderChannelWidth(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                throw new IllegalArgumentException("halChannelWidthFromResponderChannelWidth: bad " + i);
        }
    }

    private static int halRttChannelBandwidthFromResponderChannelWidth(int i) {
        switch (i) {
            case 0:
                return 4;
            case 1:
                return 8;
            case 2:
                return 16;
            case 3:
                return 32;
            case 4:
                return 32;
            default:
                throw new IllegalArgumentException("halRttChannelBandwidthFromHalBandwidth: bad " + i);
        }
    }

    private static int halRttPreambleFromResponderPreamble(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            default:
                throw new IllegalArgumentException("halRttPreambleFromResponderPreamble: bad " + i);
        }
    }

    private static int halRttChannelBandwidthCapabilityLimiter(int i, RttCapabilities rttCapabilities) {
        while (i != 0 && (rttCapabilities.bwSupport & i) == 0) {
            i >>= 1;
        }
        if (i != 0) {
            return i;
        }
        throw new IllegalArgumentException("RTT BW=" + i + ", not supported by device capabilities=" + rttCapabilities + " - and no supported alternative");
    }

    private static int halRttPreambleCapabilityLimiter(int i, RttCapabilities rttCapabilities) {
        while (i != 0 && (rttCapabilities.preambleSupport & i) == 0) {
            i >>= 1;
        }
        if (i != 0) {
            return i;
        }
        throw new IllegalArgumentException("RTT Preamble=" + i + ", not supported by device capabilities=" + rttCapabilities + " - and no supported alternative");
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("RttNative:");
        printWriter.println("  mHalDeviceManager: " + this.mHalDeviceManager);
        printWriter.println("  mIWifiRttController: " + this.mIWifiRttController);
        printWriter.println("  mRttCapabilities: " + this.mRttCapabilities);
    }
}
