package com.android.server.wifi.p2p;

import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HalDeviceManager;

public class WifiP2pNative {
    private static final int CONNECT_TO_SUPPLICANT_MAX_SAMPLES = 50;
    private static final int CONNECT_TO_SUPPLICANT_SAMPLING_INTERVAL_MS = 100;
    private static final String TAG = "WifiP2pNative";
    private final HalDeviceManager mHalDeviceManager;
    private IWifiP2pIface mIWifiP2pIface;
    private InterfaceAvailableListenerInternal mInterfaceAvailableListener;
    private InterfaceDestroyedListenerInternal mInterfaceDestroyedListener;
    private final SupplicantP2pIfaceHal mSupplicantP2pIfaceHal;

    private class InterfaceAvailableListenerInternal implements HalDeviceManager.InterfaceAvailableForRequestListener {
        private final HalDeviceManager.InterfaceAvailableForRequestListener mExternalListener;

        InterfaceAvailableListenerInternal(HalDeviceManager.InterfaceAvailableForRequestListener interfaceAvailableForRequestListener) {
            this.mExternalListener = interfaceAvailableForRequestListener;
        }

        @Override
        public void onAvailabilityChanged(boolean z) {
            Log.d(WifiP2pNative.TAG, "P2P InterfaceAvailableListener " + z);
            if (WifiP2pNative.this.mIWifiP2pIface != null && !z) {
                Log.i(WifiP2pNative.TAG, "Masking interface non-availability callback because we created a P2P iface");
            } else {
                this.mExternalListener.onAvailabilityChanged(z);
            }
        }
    }

    private class InterfaceDestroyedListenerInternal implements HalDeviceManager.InterfaceDestroyedListener {
        private final HalDeviceManager.InterfaceDestroyedListener mExternalListener;
        private boolean mValid = true;

        InterfaceDestroyedListenerInternal(HalDeviceManager.InterfaceDestroyedListener interfaceDestroyedListener) {
            this.mExternalListener = interfaceDestroyedListener;
        }

        public void teardownAndInvalidate(String str) {
            WifiP2pNative.this.mSupplicantP2pIfaceHal.teardownIface(str);
            WifiP2pNative.this.mIWifiP2pIface = null;
            this.mValid = false;
        }

        @Override
        public void onDestroyed(String str) {
            Log.d(WifiP2pNative.TAG, "P2P InterfaceDestroyedListener " + str);
            if (!this.mValid) {
                Log.d(WifiP2pNative.TAG, "Ignoring stale interface destroyed listener");
            } else {
                teardownAndInvalidate(str);
                this.mExternalListener.onDestroyed(str);
            }
        }
    }

    public WifiP2pNative(SupplicantP2pIfaceHal supplicantP2pIfaceHal, HalDeviceManager halDeviceManager) {
        this.mSupplicantP2pIfaceHal = supplicantP2pIfaceHal;
        this.mHalDeviceManager = halDeviceManager;
    }

    public void enableVerboseLogging(int i) {
    }

    private boolean waitForSupplicantConnection() {
        if (!this.mSupplicantP2pIfaceHal.isInitializationStarted() && !this.mSupplicantP2pIfaceHal.initialize()) {
            return false;
        }
        int i = 0;
        while (true) {
            int i2 = i + 1;
            if (i >= 50) {
                return false;
            }
            if (this.mSupplicantP2pIfaceHal.isInitializationComplete()) {
                return true;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
            i = i2;
        }
    }

    public void closeSupplicantConnection() {
    }

    public void registerInterfaceAvailableListener(HalDeviceManager.InterfaceAvailableForRequestListener interfaceAvailableForRequestListener, final Handler handler) {
        this.mInterfaceAvailableListener = new InterfaceAvailableListenerInternal(interfaceAvailableForRequestListener);
        this.mHalDeviceManager.registerStatusListener(new HalDeviceManager.ManagerStatusListener() {
            @Override
            public final void onStatusChanged() {
                WifiP2pNative.lambda$registerInterfaceAvailableListener$0(this.f$0, handler);
            }
        }, handler);
        if (this.mHalDeviceManager.isStarted()) {
            this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(2, this.mInterfaceAvailableListener, handler);
        }
    }

    public static void lambda$registerInterfaceAvailableListener$0(WifiP2pNative wifiP2pNative, Handler handler) {
        if (wifiP2pNative.mHalDeviceManager.isStarted()) {
            Log.i(TAG, "Registering for interface available listener");
            wifiP2pNative.mHalDeviceManager.registerInterfaceAvailableForRequestListener(2, wifiP2pNative.mInterfaceAvailableListener, handler);
        }
    }

    public String setupInterface(HalDeviceManager.InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        Log.d(TAG, "Setup P2P interface");
        if (this.mIWifiP2pIface == null) {
            this.mInterfaceDestroyedListener = new InterfaceDestroyedListenerInternal(interfaceDestroyedListener);
            this.mIWifiP2pIface = this.mHalDeviceManager.createP2pIface(this.mInterfaceDestroyedListener, handler);
            if (this.mIWifiP2pIface == null) {
                Log.e(TAG, "Failed to create P2p iface in HalDeviceManager");
                return null;
            }
            if (!waitForSupplicantConnection()) {
                Log.e(TAG, "Failed to connect to supplicant");
                teardownInterface();
                return null;
            }
            String name = HalDeviceManager.getName(this.mIWifiP2pIface);
            if (TextUtils.isEmpty(name)) {
                Log.e(TAG, "Failed to get p2p iface name");
                teardownInterface();
                return null;
            }
            if (!this.mSupplicantP2pIfaceHal.setupIface(name)) {
                Log.e(TAG, "Failed to setup P2p iface in supplicant");
                teardownInterface();
                return null;
            }
            Log.i(TAG, "P2P interface setup completed");
        }
        return HalDeviceManager.getName(this.mIWifiP2pIface);
    }

    public void teardownInterface() {
        Log.d(TAG, "Teardown P2P interface");
        if (this.mIWifiP2pIface != null) {
            String name = HalDeviceManager.getName(this.mIWifiP2pIface);
            this.mHalDeviceManager.removeIface(this.mIWifiP2pIface);
            this.mInterfaceDestroyedListener.teardownAndInvalidate(name);
            Log.i(TAG, "P2P interface teardown completed");
        }
    }

    public boolean setDeviceName(String str) {
        return this.mSupplicantP2pIfaceHal.setWpsDeviceName(str);
    }

    public boolean p2pListNetworks(WifiP2pGroupList wifiP2pGroupList) {
        return this.mSupplicantP2pIfaceHal.loadGroups(wifiP2pGroupList);
    }

    public boolean startWpsPbc(String str, String str2) {
        return this.mSupplicantP2pIfaceHal.startWpsPbc(str, str2);
    }

    public boolean startWpsPinKeypad(String str, String str2) {
        return this.mSupplicantP2pIfaceHal.startWpsPinKeypad(str, str2);
    }

    public String startWpsPinDisplay(String str, String str2) {
        return this.mSupplicantP2pIfaceHal.startWpsPinDisplay(str, str2);
    }

    public boolean removeP2pNetwork(int i) {
        return this.mSupplicantP2pIfaceHal.removeNetwork(i);
    }

    public boolean setP2pDeviceName(String str) {
        return this.mSupplicantP2pIfaceHal.setWpsDeviceName(str);
    }

    public boolean setP2pDeviceType(String str) {
        return this.mSupplicantP2pIfaceHal.setWpsDeviceType(str);
    }

    public boolean setConfigMethods(String str) {
        return this.mSupplicantP2pIfaceHal.setWpsConfigMethods(str);
    }

    public boolean setP2pSsidPostfix(String str) {
        return this.mSupplicantP2pIfaceHal.setSsidPostfix(str);
    }

    public boolean setP2pGroupIdle(String str, int i) {
        return this.mSupplicantP2pIfaceHal.setGroupIdle(str, i);
    }

    public boolean setP2pPowerSave(String str, boolean z) {
        return this.mSupplicantP2pIfaceHal.setPowerSave(str, z);
    }

    public boolean setWfdEnable(boolean z) {
        return this.mSupplicantP2pIfaceHal.enableWfd(z);
    }

    public boolean setWfdDeviceInfo(String str) {
        return this.mSupplicantP2pIfaceHal.setWfdDeviceInfo(str);
    }

    public boolean p2pFind() {
        return p2pFind(0);
    }

    public boolean p2pFind(int i) {
        return this.mSupplicantP2pIfaceHal.find(i);
    }

    public boolean p2pStopFind() {
        return this.mSupplicantP2pIfaceHal.stopFind();
    }

    public boolean p2pExtListen(boolean z, int i, int i2) {
        return this.mSupplicantP2pIfaceHal.configureExtListen(z, i, i2);
    }

    public boolean p2pSetChannel(int i, int i2) {
        return this.mSupplicantP2pIfaceHal.setListenChannel(i, i2);
    }

    public boolean p2pFlush() {
        return this.mSupplicantP2pIfaceHal.flush();
    }

    public String p2pConnect(WifiP2pConfig wifiP2pConfig, boolean z) {
        return this.mSupplicantP2pIfaceHal.connect(wifiP2pConfig, z);
    }

    public boolean p2pCancelConnect() {
        return this.mSupplicantP2pIfaceHal.cancelConnect();
    }

    public boolean p2pProvisionDiscovery(WifiP2pConfig wifiP2pConfig) {
        return this.mSupplicantP2pIfaceHal.provisionDiscovery(wifiP2pConfig);
    }

    public boolean p2pGroupAdd(boolean z) {
        return this.mSupplicantP2pIfaceHal.groupAdd(z);
    }

    public boolean p2pGroupAdd(int i) {
        return this.mSupplicantP2pIfaceHal.groupAdd(i, true);
    }

    public boolean p2pGroupRemove(String str) {
        return this.mSupplicantP2pIfaceHal.groupRemove(str);
    }

    public boolean p2pReject(String str) {
        return this.mSupplicantP2pIfaceHal.reject(str);
    }

    public boolean p2pInvite(WifiP2pGroup wifiP2pGroup, String str) {
        return this.mSupplicantP2pIfaceHal.invite(wifiP2pGroup, str);
    }

    public boolean p2pReinvoke(int i, String str) {
        return this.mSupplicantP2pIfaceHal.reinvoke(i, str);
    }

    public String p2pGetSsid(String str) {
        return this.mSupplicantP2pIfaceHal.getSsid(str);
    }

    public String p2pGetDeviceAddress() {
        return this.mSupplicantP2pIfaceHal.getDeviceAddress();
    }

    public int getGroupCapability(String str) {
        return this.mSupplicantP2pIfaceHal.getGroupCapability(str);
    }

    public boolean p2pServiceAdd(WifiP2pServiceInfo wifiP2pServiceInfo) {
        return this.mSupplicantP2pIfaceHal.serviceAdd(wifiP2pServiceInfo);
    }

    public boolean p2pServiceDel(WifiP2pServiceInfo wifiP2pServiceInfo) {
        return this.mSupplicantP2pIfaceHal.serviceRemove(wifiP2pServiceInfo);
    }

    public boolean p2pServiceFlush() {
        return this.mSupplicantP2pIfaceHal.serviceFlush();
    }

    public String p2pServDiscReq(String str, String str2) {
        return this.mSupplicantP2pIfaceHal.requestServiceDiscovery(str, str2);
    }

    public boolean p2pServDiscCancelReq(String str) {
        return this.mSupplicantP2pIfaceHal.cancelServiceDiscovery(str);
    }

    public void setMiracastMode(int i) {
        this.mSupplicantP2pIfaceHal.setMiracastMode(i);
    }

    public String getNfcHandoverRequest() {
        return this.mSupplicantP2pIfaceHal.getNfcHandoverRequest();
    }

    public String getNfcHandoverSelect() {
        return this.mSupplicantP2pIfaceHal.getNfcHandoverSelect();
    }

    public boolean initiatorReportNfcHandover(String str) {
        return this.mSupplicantP2pIfaceHal.initiatorReportNfcHandover(str);
    }

    public boolean responderReportNfcHandover(String str) {
        return this.mSupplicantP2pIfaceHal.responderReportNfcHandover(str);
    }

    public String getP2pClientList(int i) {
        return this.mSupplicantP2pIfaceHal.getClientList(i);
    }

    public boolean setP2pClientList(int i, String str) {
        return this.mSupplicantP2pIfaceHal.setClientList(i, str);
    }

    public boolean saveConfig() {
        return this.mSupplicantP2pIfaceHal.saveConfig();
    }
}
