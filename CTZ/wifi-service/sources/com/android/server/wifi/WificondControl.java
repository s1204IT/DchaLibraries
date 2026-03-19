package com.android.server.wifi;

import android.net.MacAddress;
import android.net.wifi.IApInterface;
import android.net.wifi.IApInterfaceEventCallback;
import android.net.wifi.IClientInterface;
import android.net.wifi.IPnoScanEvent;
import android.net.wifi.IScanEvent;
import android.net.wifi.IWifiScannerImpl;
import android.net.wifi.IWificond;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiSsid;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.util.InformationElementUtil;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.wificond.ChannelSettings;
import com.android.server.wifi.wificond.HiddenNetwork;
import com.android.server.wifi.wificond.NativeScanResult;
import com.android.server.wifi.wificond.PnoNetwork;
import com.android.server.wifi.wificond.PnoSettings;
import com.android.server.wifi.wificond.RadioChainInfo;
import com.android.server.wifi.wificond.SingleScanSettings;
import com.mediatek.server.wifi.MtkGbkSsid;
import com.mediatek.server.wifi.MtkWapi;
import com.mediatek.server.wifi.MtkWifiApmDelegate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WificondControl implements IBinder.DeathRecipient {
    public static final int SCAN_TYPE_PNO_SCAN = 1;
    public static final int SCAN_TYPE_SINGLE_SCAN = 0;
    private static final String TAG = "WificondControl";
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private WifiNative.WificondDeathEventHandler mDeathEventHandler;
    private WifiInjector mWifiInjector;
    private WifiMonitor mWifiMonitor;
    private IWificond mWificond;
    private boolean mVerboseLoggingEnabled = false;
    private HashMap<String, IClientInterface> mClientInterfaces = new HashMap<>();
    private HashMap<String, IApInterface> mApInterfaces = new HashMap<>();
    private HashMap<String, IWifiScannerImpl> mWificondScanners = new HashMap<>();
    private HashMap<String, IScanEvent> mScanEventHandlers = new HashMap<>();
    private HashMap<String, IPnoScanEvent> mPnoScanEventHandlers = new HashMap<>();
    private HashMap<String, IApInterfaceEventCallback> mApInterfaceListeners = new HashMap<>();

    private class ScanEventHandler extends IScanEvent.Stub {
        private String mIfaceName;

        ScanEventHandler(String str) {
            this.mIfaceName = str;
        }

        @Override
        public void OnScanResultReady() {
            Log.d(WificondControl.TAG, "Scan result ready event");
            WificondControl.this.mWifiMonitor.broadcastScanResultEvent(this.mIfaceName);
        }

        @Override
        public void OnScanFailed() {
            Log.d(WificondControl.TAG, "Scan failed event");
            WificondControl.this.mWifiMonitor.broadcastScanFailedEvent(this.mIfaceName);
        }
    }

    WificondControl(WifiInjector wifiInjector, WifiMonitor wifiMonitor, CarrierNetworkConfig carrierNetworkConfig) {
        this.mWifiInjector = wifiInjector;
        this.mWifiMonitor = wifiMonitor;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
    }

    private class PnoScanEventHandler extends IPnoScanEvent.Stub {
        private String mIfaceName;

        PnoScanEventHandler(String str) {
            this.mIfaceName = str;
        }

        @Override
        public void OnPnoNetworkFound() {
            Log.d(WificondControl.TAG, "Pno scan result event");
            WificondControl.this.mWifiMonitor.broadcastPnoScanResultEvent(this.mIfaceName);
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoFoundNetworkEventCount();
        }

        @Override
        public void OnPnoScanFailed() {
            Log.d(WificondControl.TAG, "Pno Scan failed event");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedCount();
        }

        @Override
        public void OnPnoScanOverOffloadStarted() {
            Log.d(WificondControl.TAG, "Pno scan over offload started");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanStartedOverOffloadCount();
        }

        @Override
        public void OnPnoScanOverOffloadFailed(int i) {
            Log.d(WificondControl.TAG, "Pno scan over offload failed");
            WificondControl.this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedOverOffloadCount();
        }
    }

    private class ApInterfaceEventCallback extends IApInterfaceEventCallback.Stub {
        private WifiNative.SoftApListener mSoftApListener;

        ApInterfaceEventCallback(WifiNative.SoftApListener softApListener) {
            this.mSoftApListener = softApListener;
        }

        @Override
        public void onNumAssociatedStationsChanged(int i) {
            this.mSoftApListener.onNumAssociatedStationsChanged(i);
        }

        @Override
        public void onSoftApChannelSwitched(int i, int i2) {
            this.mSoftApListener.onSoftApChannelSwitched(i, i2);
        }
    }

    @Override
    public void binderDied() {
        Log.e(TAG, "Wificond died!");
        clearState();
        this.mWificond = null;
        if (this.mDeathEventHandler != null) {
            this.mDeathEventHandler.onDeath();
        }
    }

    public void enableVerboseLogging(boolean z) {
        this.mVerboseLoggingEnabled = z;
    }

    public boolean initialize(WifiNative.WificondDeathEventHandler wificondDeathEventHandler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = wificondDeathEventHandler;
        tearDownInterfaces();
        return true;
    }

    private boolean retrieveWificondAndRegisterForDeath() {
        if (this.mWificond != null) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Wificond handle already retrieved");
            }
            return true;
        }
        this.mWificond = this.mWifiInjector.makeWificond();
        if (this.mWificond == null) {
            Log.e(TAG, "Failed to get reference to wificond");
            return false;
        }
        try {
            this.mWificond.asBinder().linkToDeath(this, 0);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register death notification for wificond");
            return false;
        }
    }

    public IClientInterface setupInterfaceForClientMode(String str) {
        IWifiScannerImpl wifiScannerImpl;
        Log.d(TAG, "Setting up interface for client mode");
        if (!retrieveWificondAndRegisterForDeath()) {
            return null;
        }
        try {
            IClientInterface iClientInterfaceCreateClientInterface = this.mWificond.createClientInterface(str);
            if (iClientInterfaceCreateClientInterface == null) {
                Log.e(TAG, "Could not get IClientInterface instance from wificond");
                return null;
            }
            Binder.allowBlocking(iClientInterfaceCreateClientInterface.asBinder());
            this.mClientInterfaces.put(str, iClientInterfaceCreateClientInterface);
            try {
                wifiScannerImpl = iClientInterfaceCreateClientInterface.getWifiScannerImpl();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to refresh wificond scanner due to remote exception");
            }
            if (wifiScannerImpl == null) {
                Log.e(TAG, "Failed to get WificondScannerImpl");
                return null;
            }
            this.mWificondScanners.put(str, wifiScannerImpl);
            Binder.allowBlocking(wifiScannerImpl.asBinder());
            ScanEventHandler scanEventHandler = new ScanEventHandler(str);
            this.mScanEventHandlers.put(str, scanEventHandler);
            wifiScannerImpl.subscribeScanEvents(scanEventHandler);
            PnoScanEventHandler pnoScanEventHandler = new PnoScanEventHandler(str);
            this.mPnoScanEventHandlers.put(str, pnoScanEventHandler);
            wifiScannerImpl.subscribePnoScanEvents(pnoScanEventHandler);
            return iClientInterfaceCreateClientInterface;
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to get IClientInterface due to remote exception");
            return null;
        }
    }

    public boolean tearDownClientInterface(String str) {
        if (getClientInterface(str) == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return false;
        }
        try {
            IWifiScannerImpl iWifiScannerImpl = this.mWificondScanners.get(str);
            if (iWifiScannerImpl != null) {
                iWifiScannerImpl.unsubscribeScanEvents();
                iWifiScannerImpl.unsubscribePnoScanEvents();
            }
            try {
                if (!this.mWificond.tearDownClientInterface(str)) {
                    Log.e(TAG, "Failed to teardown client interface");
                    return false;
                }
                this.mClientInterfaces.remove(str);
                this.mWificondScanners.remove(str);
                this.mScanEventHandlers.remove(str);
                this.mPnoScanEventHandlers.remove(str);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to teardown client interface due to remote exception");
                return false;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to unsubscribe wificond scanner due to remote exception");
            return false;
        }
    }

    public IApInterface setupInterfaceForSoftApMode(String str) {
        Log.d(TAG, "Setting up interface for soft ap mode");
        if (!retrieveWificondAndRegisterForDeath()) {
            return null;
        }
        try {
            IApInterface iApInterfaceCreateApInterface = this.mWificond.createApInterface(str);
            if (iApInterfaceCreateApInterface == null) {
                Log.e(TAG, "Could not get IApInterface instance from wificond");
                return null;
            }
            Binder.allowBlocking(iApInterfaceCreateApInterface.asBinder());
            this.mApInterfaces.put(str, iApInterfaceCreateApInterface);
            return iApInterfaceCreateApInterface;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get IApInterface due to remote exception");
            return null;
        }
    }

    public boolean tearDownSoftApInterface(String str) {
        if (getApInterface(str) == null) {
            Log.e(TAG, "No valid wificond ap interface handler");
            return false;
        }
        try {
            if (!this.mWificond.tearDownApInterface(str)) {
                Log.e(TAG, "Failed to teardown AP interface");
                return false;
            }
            this.mApInterfaces.remove(str);
            this.mApInterfaceListeners.remove(str);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to teardown AP interface due to remote exception");
            return false;
        }
    }

    public boolean tearDownInterfaces() {
        Log.d(TAG, "tearing down interfaces in wificond");
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            for (Map.Entry<String, IWifiScannerImpl> entry : this.mWificondScanners.entrySet()) {
                entry.getValue().unsubscribeScanEvents();
                entry.getValue().unsubscribePnoScanEvents();
            }
            this.mWificond.tearDownInterfaces();
            clearState();
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to tear down interfaces due to remote exception");
            return false;
        }
    }

    private IClientInterface getClientInterface(String str) {
        return this.mClientInterfaces.get(str);
    }

    public boolean disableSupplicant() {
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            return this.mWificond.disableSupplicant();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disable supplicant due to remote exception");
            return false;
        }
    }

    public boolean enableSupplicant() {
        if (!retrieveWificondAndRegisterForDeath()) {
            return false;
        }
        try {
            return this.mWificond.enableSupplicant();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to enable supplicant due to remote exception");
            return false;
        }
    }

    public WifiNative.SignalPollResult signalPoll(String str) {
        IClientInterface clientInterface = getClientInterface(str);
        if (clientInterface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return null;
        }
        try {
            int[] iArrSignalPoll = clientInterface.signalPoll();
            if (iArrSignalPoll == null || iArrSignalPoll.length != 3) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            WifiNative.SignalPollResult signalPollResult = new WifiNative.SignalPollResult();
            signalPollResult.currentRssi = iArrSignalPoll[0];
            signalPollResult.txBitrate = iArrSignalPoll[1];
            signalPollResult.associationFrequency = iArrSignalPoll[2];
            return signalPollResult;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    public WifiNative.TxPacketCounters getTxPacketCounters(String str) {
        IClientInterface clientInterface = getClientInterface(str);
        if (clientInterface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return null;
        }
        try {
            int[] packetCounters = clientInterface.getPacketCounters();
            if (packetCounters == null || packetCounters.length != 2) {
                Log.e(TAG, "Invalid signal poll result from wificond");
                return null;
            }
            WifiNative.TxPacketCounters txPacketCounters = new WifiNative.TxPacketCounters();
            txPacketCounters.txSucceeded = packetCounters[0];
            txPacketCounters.txFailed = packetCounters[1];
            return txPacketCounters;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to do signal polling due to remote exception");
            return null;
        }
    }

    private IWifiScannerImpl getScannerImpl(String str) {
        return this.mWificondScanners.get(str);
    }

    public ArrayList<ScanDetail> getScanResults(String str, int i) {
        NativeScanResult[] nativeScanResultArr;
        int i2;
        ArrayList<ScanDetail> arrayList = new ArrayList<>();
        IWifiScannerImpl scannerImpl = getScannerImpl(str);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return arrayList;
        }
        try {
            NativeScanResult[] scanResults = i == 0 ? scannerImpl.getScanResults() : scannerImpl.getPnoScanResults();
            int length = scanResults.length;
            int i3 = 0;
            while (i3 < length) {
                NativeScanResult nativeScanResult = scanResults[i3];
                WifiSsid wifiSsidCreateFromByteArray = WifiSsid.createFromByteArray(nativeScanResult.ssid);
                MtkGbkSsid.checkAndSetGbk(wifiSsidCreateFromByteArray);
                try {
                    String strMacAddressFromByteArray = NativeUtil.macAddressFromByteArray(nativeScanResult.bssid);
                    if (strMacAddressFromByteArray == null) {
                        Log.e(TAG, "Illegal null bssid");
                        nativeScanResultArr = scanResults;
                        i2 = i3;
                    } else {
                        ScanResult.InformationElement[] informationElements = InformationElementUtil.parseInformationElements(nativeScanResult.infoElement);
                        InformationElementUtil.Capabilities capabilities = new InformationElementUtil.Capabilities();
                        capabilities.from(informationElements, nativeScanResult.capability);
                        try {
                            i2 = i3;
                            nativeScanResultArr = scanResults;
                            ScanDetail scanDetail = new ScanDetail(new NetworkDetail(strMacAddressFromByteArray, informationElements, null, nativeScanResult.frequency), wifiSsidCreateFromByteArray, strMacAddressFromByteArray, MtkWapi.generateCapabilitiesString(informationElements, nativeScanResult.capability, capabilities.generateCapabilitiesString()), nativeScanResult.signalMbm / 100, nativeScanResult.frequency, nativeScanResult.tsf, informationElements, null);
                            ScanResult scanResult = scanDetail.getScanResult();
                            if (ScanResultUtil.isScanResultForEapNetwork(scanDetail.getScanResult()) && this.mCarrierNetworkConfig.isCarrierNetwork(wifiSsidCreateFromByteArray.toString())) {
                                scanResult.isCarrierAp = true;
                                scanResult.carrierApEapType = this.mCarrierNetworkConfig.getNetworkEapType(wifiSsidCreateFromByteArray.toString());
                                scanResult.carrierName = this.mCarrierNetworkConfig.getCarrierName(wifiSsidCreateFromByteArray.toString());
                            }
                            if (nativeScanResult.radioChainInfos != null) {
                                scanResult.radioChainInfos = new ScanResult.RadioChainInfo[nativeScanResult.radioChainInfos.size()];
                                int i4 = 0;
                                for (RadioChainInfo radioChainInfo : nativeScanResult.radioChainInfos) {
                                    scanResult.radioChainInfos[i4] = new ScanResult.RadioChainInfo();
                                    scanResult.radioChainInfos[i4].id = radioChainInfo.chainId;
                                    scanResult.radioChainInfos[i4].level = radioChainInfo.level;
                                    i4++;
                                }
                            }
                            arrayList.add(scanDetail);
                        } catch (IllegalArgumentException e) {
                            nativeScanResultArr = scanResults;
                            i2 = i3;
                            Log.e(TAG, "Illegal argument for scan result with bssid: " + strMacAddressFromByteArray, e);
                        }
                    }
                } catch (IllegalArgumentException e2) {
                    nativeScanResultArr = scanResults;
                    i2 = i3;
                    Log.e(TAG, "Illegal argument " + nativeScanResult.bssid, e2);
                }
                i3 = i2 + 1;
                scanResults = nativeScanResultArr;
            }
        } catch (RemoteException e3) {
            Log.e(TAG, "Failed to create ScanDetail ArrayList");
        }
        if (this.mVerboseLoggingEnabled) {
            Log.d(TAG, "get " + arrayList.size() + " scan results from wificond");
        }
        return arrayList;
    }

    private static int getScanType(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                throw new IllegalArgumentException("Invalid scan type " + i);
        }
    }

    public boolean scan(String str, int i, Set<Integer> set, List<String> list) {
        IWifiScannerImpl scannerImpl = getScannerImpl(str);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        SingleScanSettings singleScanSettings = new SingleScanSettings();
        try {
            singleScanSettings.scanType = getScanType(i);
            singleScanSettings.channelSettings = new ArrayList<>();
            singleScanSettings.hiddenNetworks = new ArrayList<>();
            if (set != null) {
                for (Integer num : set) {
                    ChannelSettings channelSettings = new ChannelSettings();
                    channelSettings.frequency = num.intValue();
                    singleScanSettings.channelSettings.add(channelSettings);
                }
            }
            if (list != null) {
                for (String str2 : list) {
                    HiddenNetwork hiddenNetwork = new HiddenNetwork();
                    try {
                        hiddenNetwork.ssid = NativeUtil.byteArrayFromArrayList(NativeUtil.decodeSsid(str2));
                        singleScanSettings.hiddenNetworks.add(hiddenNetwork);
                        HiddenNetwork hiddenNetworkNeedAddExtraGbkSsid = MtkGbkSsid.needAddExtraGbkSsid(str2);
                        if (hiddenNetworkNeedAddExtraGbkSsid != null) {
                            singleScanSettings.hiddenNetworks.add(hiddenNetworkNeedAddExtraGbkSsid);
                            Log.i(TAG, "scan with extra gbk ssid for hidden network");
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Illegal argument " + str2, e);
                    }
                }
            }
            try {
                MtkWifiApmDelegate.getInstance().notifyStartScanTime();
                return scannerImpl.scan(singleScanSettings);
            } catch (RemoteException e2) {
                Log.e(TAG, "Failed to request scan due to remote exception");
                return false;
            }
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "Invalid scan type ", e3);
            return false;
        }
    }

    public boolean startPnoScan(String str, WifiNative.PnoSettings pnoSettings) {
        IWifiScannerImpl scannerImpl = getScannerImpl(str);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        PnoSettings pnoSettings2 = new PnoSettings();
        pnoSettings2.pnoNetworks = new ArrayList<>();
        pnoSettings2.intervalMs = pnoSettings.periodInMs;
        pnoSettings2.min2gRssi = pnoSettings.min24GHzRssi;
        pnoSettings2.min5gRssi = pnoSettings.min5GHzRssi;
        if (pnoSettings.networkList != null) {
            for (WifiNative.PnoNetwork pnoNetwork : pnoSettings.networkList) {
                PnoNetwork pnoNetwork2 = new PnoNetwork();
                pnoNetwork2.isHidden = (pnoNetwork.flags & 1) != 0;
                try {
                    pnoNetwork2.ssid = NativeUtil.byteArrayFromArrayList(NativeUtil.decodeSsid(pnoNetwork.ssid));
                    pnoSettings2.pnoNetworks.add(pnoNetwork2);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + pnoNetwork.ssid, e);
                }
            }
        }
        try {
            boolean zStartPnoScan = scannerImpl.startPnoScan(pnoSettings2);
            this.mWifiInjector.getWifiMetrics().incrementPnoScanStartAttempCount();
            if (!zStartPnoScan) {
                this.mWifiInjector.getWifiMetrics().incrementPnoScanFailedCount();
            }
            return zStartPnoScan;
        } catch (RemoteException e2) {
            Log.e(TAG, "Failed to start pno scan due to remote exception");
            return false;
        }
    }

    public boolean stopPnoScan(String str) {
        IWifiScannerImpl scannerImpl = getScannerImpl(str);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return false;
        }
        try {
            return scannerImpl.stopPnoScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop pno scan due to remote exception");
            return false;
        }
    }

    public void abortScan(String str) {
        IWifiScannerImpl scannerImpl = getScannerImpl(str);
        if (scannerImpl == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return;
        }
        try {
            scannerImpl.abortScan();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request abortScan due to remote exception");
        }
    }

    public int[] getChannelsForBand(int i) {
        if (this.mWificond == null) {
            Log.e(TAG, "No valid wificond scanner interface handler");
            return null;
        }
        try {
            if (i != 4) {
                switch (i) {
                    case 1:
                        return this.mWificond.getAvailable2gChannels();
                    case 2:
                        return this.mWificond.getAvailable5gNonDFSChannels();
                    default:
                        throw new IllegalArgumentException("unsupported band " + i);
                }
            }
            return this.mWificond.getAvailableDFSChannels();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request getChannelsForBand due to remote exception");
            return null;
        }
    }

    private IApInterface getApInterface(String str) {
        return this.mApInterfaces.get(str);
    }

    public boolean startHostapd(String str, WifiNative.SoftApListener softApListener) {
        IApInterface apInterface = getApInterface(str);
        if (apInterface == null) {
            Log.e(TAG, "No valid ap interface handler");
            return false;
        }
        try {
            ApInterfaceEventCallback apInterfaceEventCallback = new ApInterfaceEventCallback(softApListener);
            this.mApInterfaceListeners.put(str, apInterfaceEventCallback);
            if (!apInterface.startHostapd(apInterfaceEventCallback)) {
                Log.e(TAG, "Failed to start hostapd.");
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in starting soft AP: " + e);
            return false;
        }
    }

    public boolean stopHostapd(String str) {
        IApInterface apInterface = getApInterface(str);
        if (apInterface == null) {
            Log.e(TAG, "No valid ap interface handler");
            return false;
        }
        try {
            if (!apInterface.stopHostapd()) {
                Log.e(TAG, "Failed to stop hostapd.");
                return false;
            }
            this.mApInterfaceListeners.remove(str);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Exception in stopping soft AP: " + e);
            return false;
        }
    }

    public boolean setMacAddress(String str, MacAddress macAddress) {
        IClientInterface clientInterface = getClientInterface(str);
        if (clientInterface == null) {
            Log.e(TAG, "No valid wificond client interface handler");
            return false;
        }
        try {
            clientInterface.setMacAddress(macAddress.toByteArray());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to setMacAddress due to remote exception");
            return false;
        }
    }

    private void clearState() {
        this.mClientInterfaces.clear();
        this.mWificondScanners.clear();
        this.mPnoScanEventHandlers.clear();
        this.mScanEventHandlers.clear();
        this.mApInterfaces.clear();
        this.mApInterfaceListeners.clear();
        MtkGbkSsid.clear();
    }
}
