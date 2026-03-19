package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.hardware.wifi.supplicant.V1_1.ISupplicant;
import android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.net.IpConfiguration;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiSsid;
import android.os.Build;
import android.os.HidlSupport;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.WnmData;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.ANQPParser;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.util.NativeUtil;
import com.mediatek.server.wifi.MtkGbkSsid;
import com.mediatek.server.wifi.MtkWapi;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class SupplicantStaIfaceHal {
    private static final String TAG = "SupplicantStaIfaceHal";
    private static final Pattern WPS_DEVICE_TYPE_PATTERN = Pattern.compile("^(\\d{1,2})-([0-9a-fA-F]{8})-(\\d{1,2})$");
    private final Context mContext;
    private WifiNative.SupplicantDeathEventHandler mDeathEventHandler;
    private ISupplicant mISupplicant;
    private final WifiMonitor mWifiMonitor;
    private final Object mLock = new Object();
    private boolean mVerboseLoggingEnabled = false;
    private IServiceManager mIServiceManager = null;
    private HashMap<String, ISupplicantStaIface> mISupplicantStaIfaces = new HashMap<>();
    private HashMap<String, ISupplicantStaIfaceCallback> mISupplicantStaIfaceCallbacks = new HashMap<>();
    private HashMap<String, SupplicantStaNetworkHal> mCurrentNetworkRemoteHandles = new HashMap<>();
    private HashMap<String, WifiConfiguration> mCurrentNetworkLocalConfigs = new HashMap<>();
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String str, String str2, boolean z) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    Log.i(SupplicantStaIfaceHal.TAG, "IServiceNotification.onRegistration for: " + str + ", " + str2 + " preexisting=" + z);
                }
                if (!SupplicantStaIfaceHal.this.initSupplicantService()) {
                    Log.e(SupplicantStaIfaceHal.TAG, "initalizing ISupplicant failed.");
                    SupplicantStaIfaceHal.this.supplicantServiceDiedHandler();
                } else {
                    Log.i(SupplicantStaIfaceHal.TAG, "Completed initialization of ISupplicant.");
                }
            }
        }
    };
    private final IHwBinder.DeathRecipient mServiceManagerDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            SupplicantStaIfaceHal.lambda$new$0(this.f$0, j);
        }
    };
    private final IHwBinder.DeathRecipient mSupplicantDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            SupplicantStaIfaceHal.lambda$new$1(this.f$0, j);
        }
    };

    public static void lambda$new$0(SupplicantStaIfaceHal supplicantStaIfaceHal, long j) {
        synchronized (supplicantStaIfaceHal.mLock) {
            Log.w(TAG, "IServiceManager died: cookie=" + j);
            supplicantStaIfaceHal.supplicantServiceDiedHandler();
            supplicantStaIfaceHal.mIServiceManager = null;
        }
    }

    public static void lambda$new$1(SupplicantStaIfaceHal supplicantStaIfaceHal, long j) {
        synchronized (supplicantStaIfaceHal.mLock) {
            Log.w(TAG, "ISupplicant died: cookie=" + j);
            supplicantStaIfaceHal.supplicantServiceDiedHandler();
        }
    }

    public SupplicantStaIfaceHal(Context context, WifiMonitor wifiMonitor) {
        this.mContext = context;
        this.mWifiMonitor = wifiMonitor;
    }

    void enableVerboseLogging(boolean z) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = z;
        }
    }

    private boolean linkToServiceManagerDeath() {
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                return false;
            }
            try {
                if (!this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0L)) {
                    Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                    supplicantServiceDiedHandler();
                    this.mIServiceManager = null;
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering ISupplicant service ready callback.");
            }
            this.mISupplicant = null;
            this.mISupplicantStaIfaces.clear();
            if (this.mIServiceManager != null) {
                return true;
            }
            try {
                this.mIServiceManager = getServiceManagerMockable();
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for ISupplicant service: " + e);
                supplicantServiceDiedHandler();
            }
            if (this.mIServiceManager == null) {
                Log.e(TAG, "Failed to get HIDL Service Manager");
                return false;
            }
            if (!linkToServiceManagerDeath()) {
                return false;
            }
            if (this.mIServiceManager.registerForNotifications(ISupplicant.kInterfaceName, "", this.mServiceNotificationCallback)) {
                return true;
            }
            Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.supplicant@1.0::ISupplicant");
            this.mIServiceManager = null;
            return false;
        }
    }

    private boolean linkToSupplicantDeath() {
        synchronized (this.mLock) {
            if (this.mISupplicant == null) {
                return false;
            }
            try {
                if (!this.mISupplicant.linkToDeath(this.mSupplicantDeathRecipient, 0L)) {
                    Log.wtf(TAG, "Error on linkToDeath on ISupplicant");
                    supplicantServiceDiedHandler();
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean initSupplicantService() {
        synchronized (this.mLock) {
            try {
                try {
                    this.mISupplicant = getSupplicantMockable();
                    if (this.mISupplicant == null) {
                        Log.e(TAG, "Got null ISupplicant service. Stopping supplicant HIDL startup");
                        return false;
                    }
                    if (!linkToSupplicantDeath()) {
                        return false;
                    }
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicant.getService exception: " + e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int getCurrentNetworkId(String str) {
        synchronized (this.mLock) {
            WifiConfiguration currentNetworkLocalConfig = getCurrentNetworkLocalConfig(str);
            if (currentNetworkLocalConfig == null) {
                return -1;
            }
            return currentNetworkLocalConfig.networkId;
        }
    }

    public boolean setupIface(String str) {
        ISupplicantIface ifaceV1_0;
        if (checkSupplicantStaIfaceAndLogFailure(str, "setupIface") != null) {
            return false;
        }
        if (isV1_1()) {
            ifaceV1_0 = addIfaceV1_1(str);
        } else {
            ifaceV1_0 = getIfaceV1_0(str);
        }
        if (ifaceV1_0 == null) {
            Log.e(TAG, "setupIface got null iface");
            return false;
        }
        MtkWapi.setupMtkIface(str);
        SupplicantStaIfaceHalCallback supplicantStaIfaceHalCallback = new SupplicantStaIfaceHalCallback(str);
        if (isV1_1()) {
            android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface staIfaceMockableV1_1 = getStaIfaceMockableV1_1(ifaceV1_0);
            android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback supplicantStaIfaceHalCallbackV1_1 = new SupplicantStaIfaceHalCallbackV1_1(str, supplicantStaIfaceHalCallback);
            if (!registerCallbackV1_1(staIfaceMockableV1_1, supplicantStaIfaceHalCallbackV1_1)) {
                return false;
            }
            this.mISupplicantStaIfaces.put(str, staIfaceMockableV1_1);
            this.mISupplicantStaIfaceCallbacks.put(str, supplicantStaIfaceHalCallbackV1_1);
            return true;
        }
        ISupplicantStaIface staIfaceMockable = getStaIfaceMockable(ifaceV1_0);
        if (!registerCallback(staIfaceMockable, supplicantStaIfaceHalCallback)) {
            return false;
        }
        this.mISupplicantStaIfaces.put(str, staIfaceMockable);
        this.mISupplicantStaIfaceCallbacks.put(str, supplicantStaIfaceHalCallback);
        return true;
    }

    private ISupplicantIface getIfaceV1_0(String str) {
        synchronized (this.mLock) {
            final ArrayList arrayList = new ArrayList();
            try {
                this.mISupplicant.listInterfaces(new ISupplicant.listInterfacesCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList2) {
                        SupplicantStaIfaceHal.lambda$getIfaceV1_0$2(arrayList, supplicantStatus, arrayList2);
                    }
                });
                if (arrayList.size() == 0) {
                    Log.e(TAG, "Got zero HIDL supplicant ifaces. Stopping supplicant HIDL startup.");
                    return null;
                }
                final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
                Iterator it = arrayList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ISupplicant.IfaceInfo ifaceInfo = (ISupplicant.IfaceInfo) it.next();
                    if (ifaceInfo.type == 0 && str.equals(ifaceInfo.name)) {
                        try {
                            break;
                        } catch (RemoteException e) {
                            Log.e(TAG, "ISupplicant.getInterface exception: " + e);
                            handleRemoteException(e, "getInterface");
                            return null;
                        }
                    }
                }
                return (ISupplicantIface) mutable.value;
            } catch (RemoteException e2) {
                Log.e(TAG, "ISupplicant.listInterfaces exception: " + e2);
                handleRemoteException(e2, "listInterfaces");
                return null;
            }
        }
    }

    static void lambda$getIfaceV1_0$2(ArrayList arrayList, SupplicantStatus supplicantStatus, ArrayList arrayList2) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "Getting Supplicant Interfaces failed: " + supplicantStatus.code);
            return;
        }
        arrayList.addAll(arrayList2);
    }

    static void lambda$getIfaceV1_0$3(HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "Failed to get ISupplicantIface " + supplicantStatus.code);
            return;
        }
        mutable.value = iSupplicantIface;
    }

    private ISupplicantIface addIfaceV1_1(String str) {
        ISupplicantIface iSupplicantIface;
        synchronized (this.mLock) {
            ISupplicant.IfaceInfo ifaceInfo = new ISupplicant.IfaceInfo();
            ifaceInfo.name = str;
            ifaceInfo.type = 0;
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                getSupplicantMockableV1_1().addInterface(ifaceInfo, new ISupplicant.addInterfaceCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface2) {
                        SupplicantStaIfaceHal.lambda$addIfaceV1_1$4(mutable, supplicantStatus, iSupplicantIface2);
                    }
                });
                iSupplicantIface = (ISupplicantIface) mutable.value;
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.addInterface exception: " + e);
                handleRemoteException(e, "addInterface");
                return null;
            }
        }
        return iSupplicantIface;
    }

    static void lambda$addIfaceV1_1$4(HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        if (supplicantStatus.code != 0 && supplicantStatus.code != 5) {
            Log.e(TAG, "Failed to create ISupplicantIface " + supplicantStatus.code);
            return;
        }
        mutable.value = iSupplicantIface;
    }

    public boolean teardownIface(String str) {
        synchronized (this.mLock) {
            if (checkSupplicantStaIfaceAndLogFailure(str, "teardownIface") == null) {
                return false;
            }
            if (isV1_1() && !removeIfaceV1_1(str)) {
                Log.e(TAG, "Failed to remove iface = " + str);
                return false;
            }
            if (this.mISupplicantStaIfaces.remove(str) == null) {
                Log.e(TAG, "Trying to teardown unknown inteface");
                return false;
            }
            this.mISupplicantStaIfaceCallbacks.remove(str);
            return true;
        }
    }

    private boolean removeIfaceV1_1(String str) {
        synchronized (this.mLock) {
            try {
                try {
                    ISupplicant.IfaceInfo ifaceInfo = new ISupplicant.IfaceInfo();
                    ifaceInfo.name = str;
                    ifaceInfo.type = 0;
                    SupplicantStatus supplicantStatusRemoveInterface = getSupplicantMockableV1_1().removeInterface(ifaceInfo);
                    if (supplicantStatusRemoveInterface.code != 0) {
                        Log.e(TAG, "Failed to remove iface " + supplicantStatusRemoveInterface.code);
                        return false;
                    }
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicant.removeInterface exception: " + e);
                    handleRemoteException(e, "removeInterface");
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean registerDeathHandler(WifiNative.SupplicantDeathEventHandler supplicantDeathEventHandler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = supplicantDeathEventHandler;
        return true;
    }

    public boolean deregisterDeathHandler() {
        if (this.mDeathEventHandler == null) {
            Log.e(TAG, "No Death handler present");
        }
        this.mDeathEventHandler = null;
        return true;
    }

    private void clearState() {
        synchronized (this.mLock) {
            this.mISupplicant = null;
            this.mISupplicantStaIfaces.clear();
            this.mCurrentNetworkLocalConfigs.clear();
            this.mCurrentNetworkRemoteHandles.clear();
        }
    }

    private void supplicantServiceDiedHandler() {
        synchronized (this.mLock) {
            Iterator<String> it = this.mISupplicantStaIfaces.keySet().iterator();
            while (it.hasNext()) {
                this.mWifiMonitor.broadcastSupplicantDisconnectionEvent(it.next());
            }
            clearState();
            if (this.mDeathEventHandler != null) {
                this.mDeathEventHandler.onDeath();
            }
        }
    }

    public boolean isInitializationStarted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIServiceManager != null;
        }
        return z;
    }

    public boolean isInitializationComplete() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mISupplicant != null;
        }
        return z;
    }

    public void terminate() {
        synchronized (this.mLock) {
            if (checkSupplicantAndLogFailure("terminate")) {
                try {
                    if (isV1_1()) {
                        getSupplicantMockableV1_1().terminate();
                    }
                } catch (RemoteException e) {
                    handleRemoteException(e, "terminate");
                }
            }
        }
    }

    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    protected android.hardware.wifi.supplicant.V1_0.ISupplicant getSupplicantMockable() throws RemoteException {
        android.hardware.wifi.supplicant.V1_0.ISupplicant service;
        synchronized (this.mLock) {
            try {
                try {
                    service = android.hardware.wifi.supplicant.V1_0.ISupplicant.getService();
                } catch (NoSuchElementException e) {
                    Log.e(TAG, "Failed to get ISupplicant", e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return service;
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicant getSupplicantMockableV1_1() throws RemoteException {
        android.hardware.wifi.supplicant.V1_1.ISupplicant iSupplicantCastFrom;
        synchronized (this.mLock) {
            try {
                try {
                    iSupplicantCastFrom = android.hardware.wifi.supplicant.V1_1.ISupplicant.castFrom((IHwInterface) android.hardware.wifi.supplicant.V1_0.ISupplicant.getService());
                } catch (NoSuchElementException e) {
                    Log.e(TAG, "Failed to get ISupplicant", e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return iSupplicantCastFrom;
    }

    protected ISupplicantStaIface getStaIfaceMockable(ISupplicantIface iSupplicantIface) {
        ISupplicantStaIface iSupplicantStaIfaceAsInterface;
        synchronized (this.mLock) {
            iSupplicantStaIfaceAsInterface = ISupplicantStaIface.asInterface(iSupplicantIface.asBinder());
        }
        return iSupplicantStaIfaceAsInterface;
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface getStaIfaceMockableV1_1(ISupplicantIface iSupplicantIface) {
        android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface iSupplicantStaIfaceAsInterface;
        synchronized (this.mLock) {
            iSupplicantStaIfaceAsInterface = android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface.asInterface(iSupplicantIface.asBinder());
        }
        return iSupplicantStaIfaceAsInterface;
    }

    private boolean isV1_1() {
        boolean z;
        synchronized (this.mLock) {
            try {
                try {
                    z = getSupplicantMockableV1_1() != null;
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicant.getService exception: " + e);
                    handleRemoteException(e, "getSupplicantMockable");
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return z;
    }

    private ISupplicantStaIface getStaIface(String str) {
        return this.mISupplicantStaIfaces.get(str);
    }

    private SupplicantStaNetworkHal getCurrentNetworkRemoteHandle(String str) {
        return this.mCurrentNetworkRemoteHandles.get(str);
    }

    private WifiConfiguration getCurrentNetworkLocalConfig(String str) {
        return this.mCurrentNetworkLocalConfigs.get(str);
    }

    private Pair<SupplicantStaNetworkHal, WifiConfiguration> addNetworkAndSaveConfig(String str, WifiConfiguration wifiConfiguration) {
        synchronized (this.mLock) {
            logi("addSupplicantStaNetwork via HIDL");
            if (wifiConfiguration == null) {
                loge("Cannot add NULL network!");
                return null;
            }
            SupplicantStaNetworkHal supplicantStaNetworkHalAddNetwork = addNetwork(str);
            if (supplicantStaNetworkHalAddNetwork == null) {
                loge("Failed to add a network!");
                return null;
            }
            boolean zSaveWifiConfiguration = false;
            try {
                zSaveWifiConfiguration = supplicantStaNetworkHalAddNetwork.saveWifiConfiguration(wifiConfiguration);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Exception while saving config params: " + wifiConfiguration, e);
            }
            if (!zSaveWifiConfiguration) {
                loge("Failed to save variables for: " + wifiConfiguration.configKey());
                if (!removeAllNetworks(str)) {
                    loge("Failed to remove all networks on failure.");
                }
                return null;
            }
            return new Pair<>(supplicantStaNetworkHalAddNetwork, new WifiConfiguration(wifiConfiguration));
        }
    }

    public boolean connectToNetwork(String str, WifiConfiguration wifiConfiguration) {
        synchronized (this.mLock) {
            logd("connectToNetwork " + wifiConfiguration.configKey());
            WifiConfiguration currentNetworkLocalConfig = getCurrentNetworkLocalConfig(str);
            if (WifiConfigurationUtil.isSameNetwork(wifiConfiguration, currentNetworkLocalConfig)) {
                if (Objects.equals(wifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionBSSID(), currentNetworkLocalConfig.getNetworkSelectionStatus().getNetworkSelectionBSSID())) {
                    logd("Network is already saved, will not trigger remove and add operation.");
                } else {
                    logd("Network is already saved, but need to update BSSID.");
                    if (!setCurrentNetworkBssid(str, wifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionBSSID())) {
                        loge("Failed to set current network BSSID.");
                        return false;
                    }
                    this.mCurrentNetworkLocalConfigs.put(str, new WifiConfiguration(wifiConfiguration));
                }
            } else {
                this.mCurrentNetworkRemoteHandles.remove(str);
                this.mCurrentNetworkLocalConfigs.remove(str);
                if (!removeAllNetworks(str)) {
                    loge("Failed to remove existing networks");
                    return false;
                }
                Pair<SupplicantStaNetworkHal, WifiConfiguration> pairAddNetworkAndSaveConfig = addNetworkAndSaveConfig(str, wifiConfiguration);
                if (pairAddNetworkAndSaveConfig == null) {
                    loge("Failed to add/save network configuration: " + wifiConfiguration.configKey());
                    return false;
                }
                this.mCurrentNetworkRemoteHandles.put(str, (SupplicantStaNetworkHal) pairAddNetworkAndSaveConfig.first);
                this.mCurrentNetworkLocalConfigs.put(str, (WifiConfiguration) pairAddNetworkAndSaveConfig.second);
            }
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "connectToNetwork");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure != null && supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.select()) {
                return true;
            }
            loge("Failed to select network configuration: " + wifiConfiguration.configKey());
            return false;
        }
    }

    public boolean roamToNetwork(String str, WifiConfiguration wifiConfiguration) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId(str) != wifiConfiguration.networkId) {
                Log.w(TAG, "Cannot roam to a different network, initiate new connection. Current network ID: " + getCurrentNetworkId(str));
                return connectToNetwork(str, wifiConfiguration);
            }
            String networkSelectionBSSID = wifiConfiguration.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            logd("roamToNetwork" + wifiConfiguration.configKey() + " (bssid " + networkSelectionBSSID + ")");
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "roamToNetwork");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure != null && supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.setBssid(networkSelectionBSSID)) {
                if (!reassociate(str)) {
                    loge("Failed to trigger reassociate");
                    return false;
                }
                return true;
            }
            loge("Failed to set new bssid on network: " + wifiConfiguration.configKey());
            return false;
        }
    }

    public boolean loadNetworks(String str, Map<String, WifiConfiguration> map, SparseArray<Map<String, String>> sparseArray) {
        boolean zLoadWifiConfiguration;
        synchronized (this.mLock) {
            ArrayList<Integer> arrayListListNetworks = listNetworks(str);
            if (arrayListListNetworks == null) {
                Log.e(TAG, "Failed to list networks");
                return false;
            }
            for (Integer num : arrayListListNetworks) {
                SupplicantStaNetworkHal network = getNetwork(str, num.intValue());
                if (network == null) {
                    Log.e(TAG, "Failed to get network with ID: " + num);
                    return false;
                }
                WifiConfiguration wifiConfiguration = new WifiConfiguration();
                HashMap map2 = new HashMap();
                try {
                    zLoadWifiConfiguration = network.loadWifiConfiguration(wifiConfiguration, map2);
                } catch (IllegalArgumentException e) {
                    Log.wtf(TAG, "Exception while loading config params: " + wifiConfiguration, e);
                    zLoadWifiConfiguration = false;
                }
                if (!zLoadWifiConfiguration) {
                    Log.e(TAG, "Failed to load wifi configuration for network with ID: " + num + ". Skipping...");
                } else {
                    wifiConfiguration.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                    wifiConfiguration.setProxySettings(IpConfiguration.ProxySettings.NONE);
                    sparseArray.put(num.intValue(), map2);
                    WifiConfiguration wifiConfigurationPut = map.put(map2.get(SupplicantStaNetworkHal.ID_STRING_KEY_CONFIG_KEY), wifiConfiguration);
                    if (wifiConfigurationPut != null) {
                        Log.i(TAG, "Replacing duplicate network: " + wifiConfigurationPut.networkId);
                        removeNetwork(str, wifiConfigurationPut.networkId);
                        sparseArray.remove(wifiConfigurationPut.networkId);
                    }
                }
            }
            return true;
        }
    }

    public void removeNetworkIfCurrent(String str, int i) {
        synchronized (this.mLock) {
            if (getCurrentNetworkId(str) == i) {
                removeAllNetworks(str);
            }
        }
    }

    public boolean removeAllNetworks(String str) {
        synchronized (this.mLock) {
            ArrayList<Integer> arrayListListNetworks = listNetworks(str);
            if (arrayListListNetworks == null) {
                Log.e(TAG, "removeAllNetworks failed, got null networks");
                return false;
            }
            Iterator<Integer> it = arrayListListNetworks.iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                if (!removeNetwork(str, iIntValue)) {
                    Log.e(TAG, "removeAllNetworks failed to remove network: " + iIntValue);
                    return false;
                }
            }
            this.mCurrentNetworkRemoteHandles.remove(str);
            this.mCurrentNetworkLocalConfigs.remove(str);
            return true;
        }
    }

    public boolean setCurrentNetworkBssid(String str, String str2) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "setCurrentNetworkBssid");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.setBssid(str2);
        }
    }

    public String getCurrentNetworkWpsNfcConfigurationToken(String str) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "getCurrentNetworkWpsNfcConfigurationToken");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return null;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.getWpsNfcConfigurationToken();
        }
    }

    public String getCurrentNetworkEapAnonymousIdentity(String str) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "getCurrentNetworkEapAnonymousIdentity");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return null;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.fetchEapAnonymousIdentity();
        }
    }

    public boolean sendCurrentNetworkEapIdentityResponse(String str, String str2, String str3) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "sendCurrentNetworkEapIdentityResponse");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.sendNetworkEapIdentityResponse(str2, str3);
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthResponse(String str, String str2) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "sendCurrentNetworkEapSimGsmAuthResponse");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.sendNetworkEapSimGsmAuthResponse(str2);
        }
    }

    public boolean sendCurrentNetworkEapSimGsmAuthFailure(String str) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "sendCurrentNetworkEapSimGsmAuthFailure");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.sendNetworkEapSimGsmAuthFailure();
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthResponse(String str, String str2) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "sendCurrentNetworkEapSimUmtsAuthResponse");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.sendNetworkEapSimUmtsAuthResponse(str2);
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAutsResponse(String str, String str2) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "sendCurrentNetworkEapSimUmtsAutsResponse");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.sendNetworkEapSimUmtsAutsResponse(str2);
        }
    }

    public boolean sendCurrentNetworkEapSimUmtsAuthFailure(String str) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure = checkSupplicantStaNetworkAndLogFailure(str, "sendCurrentNetworkEapSimUmtsAuthFailure");
            if (supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure == null) {
                return false;
            }
            return supplicantStaNetworkHalCheckSupplicantStaNetworkAndLogFailure.sendNetworkEapSimUmtsAuthFailure();
        }
    }

    private SupplicantStaNetworkHal addNetwork(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "addNetwork");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return null;
            }
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.addNetwork(new ISupplicantIface.addNetworkCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
                        SupplicantStaIfaceHal.lambda$addNetwork$5(this.f$0, mutable, supplicantStatus, iSupplicantNetwork);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "addNetwork");
            }
            if (mutable.value == 0) {
                return null;
            }
            return getStaNetworkMockable(str, ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) mutable.value).asBinder()));
        }
    }

    public static void lambda$addNetwork$5(SupplicantStaIfaceHal supplicantStaIfaceHal, HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(supplicantStatus, "addNetwork")) {
            mutable.value = iSupplicantNetwork;
        }
    }

    private boolean removeNetwork(String str, int i) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "removeNetwork");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.removeNetwork(i), "removeNetwork");
            } catch (RemoteException e) {
                handleRemoteException(e, "removeNetwork");
                return false;
            }
        }
    }

    protected SupplicantStaNetworkHal getStaNetworkMockable(String str, ISupplicantStaNetwork iSupplicantStaNetwork) {
        SupplicantStaNetworkHal supplicantStaNetworkHal;
        synchronized (this.mLock) {
            supplicantStaNetworkHal = new SupplicantStaNetworkHal(iSupplicantStaNetwork, str, this.mContext, this.mWifiMonitor);
            supplicantStaNetworkHal.enableVerboseLogging(this.mVerboseLoggingEnabled);
        }
        return supplicantStaNetworkHal;
    }

    private SupplicantStaNetworkHal getNetwork(String str, int i) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "getNetwork");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return null;
            }
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.getNetwork(i, new ISupplicantIface.getNetworkCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
                        SupplicantStaIfaceHal.lambda$getNetwork$6(this.f$0, mutable, supplicantStatus, iSupplicantNetwork);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "getNetwork");
            }
            if (mutable.value == 0) {
                return null;
            }
            return getStaNetworkMockable(str, ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) mutable.value).asBinder()));
        }
    }

    public static void lambda$getNetwork$6(SupplicantStaIfaceHal supplicantStaIfaceHal, HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(supplicantStatus, "getNetwork")) {
            mutable.value = iSupplicantNetwork;
        }
    }

    private boolean registerCallback(ISupplicantStaIface iSupplicantStaIface, ISupplicantStaIfaceCallback iSupplicantStaIfaceCallback) {
        synchronized (this.mLock) {
            if (iSupplicantStaIface == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIface.registerCallback(iSupplicantStaIfaceCallback), "registerCallback");
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback");
                return false;
            }
        }
    }

    private boolean registerCallbackV1_1(android.hardware.wifi.supplicant.V1_1.ISupplicantStaIface iSupplicantStaIface, android.hardware.wifi.supplicant.V1_1.ISupplicantStaIfaceCallback iSupplicantStaIfaceCallback) {
        synchronized (this.mLock) {
            if (iSupplicantStaIface == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIface.registerCallback_1_1(iSupplicantStaIfaceCallback), "registerCallback_1_1");
            } catch (RemoteException e) {
                handleRemoteException(e, "registerCallback_1_1");
                return false;
            }
        }
    }

    private ArrayList<Integer> listNetworks(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "listNetworks");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return null;
            }
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.listNetworks(new ISupplicantIface.listNetworksCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        SupplicantStaIfaceHal.lambda$listNetworks$7(this.f$0, mutable, supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "listNetworks");
            }
            return (ArrayList) mutable.value;
        }
    }

    public static void lambda$listNetworks$7(SupplicantStaIfaceHal supplicantStaIfaceHal, HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(supplicantStatus, "listNetworks")) {
            mutable.value = arrayList;
        }
    }

    public boolean setWpsDeviceName(String str, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsDeviceName");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsDeviceName(str2), "setWpsDeviceName");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsDeviceName");
                return false;
            }
        }
    }

    public boolean setWpsDeviceType(String str, String str2) {
        synchronized (this.mLock) {
            try {
                try {
                    Matcher matcher = WPS_DEVICE_TYPE_PATTERN.matcher(str2);
                    if (matcher.find() && matcher.groupCount() == 3) {
                        short s = Short.parseShort(matcher.group(1));
                        byte[] bArrHexStringToByteArray = NativeUtil.hexStringToByteArray(matcher.group(2));
                        short s2 = Short.parseShort(matcher.group(3));
                        byte[] bArr = new byte[8];
                        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.BIG_ENDIAN);
                        byteBufferOrder.putShort(s);
                        byteBufferOrder.put(bArrHexStringToByteArray);
                        byteBufferOrder.putShort(s2);
                        return setWpsDeviceType(str, bArr);
                    }
                    Log.e(TAG, "Malformed WPS device type " + str2);
                    return false;
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean setWpsDeviceType(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsDeviceType");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsDeviceType(bArr), "setWpsDeviceType");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsDeviceType");
                return false;
            }
        }
    }

    public boolean setWpsManufacturer(String str, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsManufacturer");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsManufacturer(str2), "setWpsManufacturer");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsManufacturer");
                return false;
            }
        }
    }

    public boolean setWpsModelName(String str, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsModelName");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsModelName(str2), "setWpsModelName");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsModelName");
                return false;
            }
        }
    }

    public boolean setWpsModelNumber(String str, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsModelNumber");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsModelNumber(str2), "setWpsModelNumber");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsModelNumber");
                return false;
            }
        }
    }

    public boolean setWpsSerialNumber(String str, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsSerialNumber");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsSerialNumber(str2), "setWpsSerialNumber");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsSerialNumber");
                return false;
            }
        }
    }

    public boolean setWpsConfigMethods(String str, String str2) {
        boolean wpsConfigMethods;
        synchronized (this.mLock) {
            short sStringToWpsConfigMethod = 0;
            for (String str3 : str2.split("\\s+")) {
                sStringToWpsConfigMethod = (short) (sStringToWpsConfigMethod | stringToWpsConfigMethod(str3));
            }
            wpsConfigMethods = setWpsConfigMethods(str, sStringToWpsConfigMethod);
        }
        return wpsConfigMethods;
    }

    private boolean setWpsConfigMethods(String str, short s) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setWpsConfigMethods");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setWpsConfigMethods(s), "setWpsConfigMethods");
            } catch (RemoteException e) {
                handleRemoteException(e, "setWpsConfigMethods");
                return false;
            }
        }
    }

    public boolean reassociate(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "reassociate");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.reassociate(), "reassociate");
            } catch (RemoteException e) {
                handleRemoteException(e, "reassociate");
                return false;
            }
        }
    }

    public boolean reconnect(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "reconnect");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.reconnect(), "reconnect");
            } catch (RemoteException e) {
                handleRemoteException(e, "reconnect");
                return false;
            }
        }
    }

    public boolean disconnect(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "disconnect");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.disconnect(), "disconnect");
            } catch (RemoteException e) {
                handleRemoteException(e, "disconnect");
                return false;
            }
        }
    }

    public boolean setPowerSave(String str, boolean z) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setPowerSave");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setPowerSave(z), "setPowerSave");
            } catch (RemoteException e) {
                handleRemoteException(e, "setPowerSave");
                return false;
            }
        }
    }

    public boolean initiateTdlsDiscover(String str, String str2) {
        boolean zInitiateTdlsDiscover;
        synchronized (this.mLock) {
            try {
                try {
                    zInitiateTdlsDiscover = initiateTdlsDiscover(str, NativeUtil.macAddressToByteArray(str2));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zInitiateTdlsDiscover;
    }

    private boolean initiateTdlsDiscover(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "initiateTdlsDiscover");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.initiateTdlsDiscover(bArr), "initiateTdlsDiscover");
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsDiscover");
                return false;
            }
        }
    }

    public boolean initiateTdlsSetup(String str, String str2) {
        boolean zInitiateTdlsSetup;
        synchronized (this.mLock) {
            try {
                try {
                    zInitiateTdlsSetup = initiateTdlsSetup(str, NativeUtil.macAddressToByteArray(str2));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zInitiateTdlsSetup;
    }

    private boolean initiateTdlsSetup(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "initiateTdlsSetup");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.initiateTdlsSetup(bArr), "initiateTdlsSetup");
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsSetup");
                return false;
            }
        }
    }

    public boolean initiateTdlsTeardown(String str, String str2) {
        boolean zInitiateTdlsTeardown;
        synchronized (this.mLock) {
            try {
                try {
                    zInitiateTdlsTeardown = initiateTdlsTeardown(str, NativeUtil.macAddressToByteArray(str2));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zInitiateTdlsTeardown;
    }

    private boolean initiateTdlsTeardown(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "initiateTdlsTeardown");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.initiateTdlsTeardown(bArr), "initiateTdlsTeardown");
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateTdlsTeardown");
                return false;
            }
        }
    }

    public boolean initiateAnqpQuery(String str, String str2, ArrayList<Short> arrayList, ArrayList<Integer> arrayList2) {
        boolean zInitiateAnqpQuery;
        synchronized (this.mLock) {
            try {
                try {
                    zInitiateAnqpQuery = initiateAnqpQuery(str, NativeUtil.macAddressToByteArray(str2), arrayList, arrayList2);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zInitiateAnqpQuery;
    }

    private boolean initiateAnqpQuery(String str, byte[] bArr, ArrayList<Short> arrayList, ArrayList<Integer> arrayList2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "initiateAnqpQuery");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.initiateAnqpQuery(bArr, arrayList, arrayList2), "initiateAnqpQuery");
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateAnqpQuery");
                return false;
            }
        }
    }

    public boolean initiateHs20IconQuery(String str, String str2, String str3) {
        boolean zInitiateHs20IconQuery;
        synchronized (this.mLock) {
            try {
                try {
                    zInitiateHs20IconQuery = initiateHs20IconQuery(str, NativeUtil.macAddressToByteArray(str2), str3);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zInitiateHs20IconQuery;
    }

    private boolean initiateHs20IconQuery(String str, byte[] bArr, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "initiateHs20IconQuery");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.initiateHs20IconQuery(bArr, str2), "initiateHs20IconQuery");
            } catch (RemoteException e) {
                handleRemoteException(e, "initiateHs20IconQuery");
                return false;
            }
        }
    }

    public String getMacAddress(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "getMacAddress");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return null;
            }
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.getMacAddress(new ISupplicantStaIface.getMacAddressCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantStaIfaceHal.lambda$getMacAddress$8(this.f$0, mutable, supplicantStatus, bArr);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "getMacAddress");
            }
            return (String) mutable.value;
        }
    }

    public static void lambda$getMacAddress$8(SupplicantStaIfaceHal supplicantStaIfaceHal, HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, byte[] bArr) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(supplicantStatus, "getMacAddress")) {
            mutable.value = NativeUtil.macAddressFromByteArray(bArr);
        }
    }

    public boolean startRxFilter(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "startRxFilter");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.startRxFilter(), "startRxFilter");
            } catch (RemoteException e) {
                handleRemoteException(e, "startRxFilter");
                return false;
            }
        }
    }

    public boolean stopRxFilter(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "stopRxFilter");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.stopRxFilter(), "stopRxFilter");
            } catch (RemoteException e) {
                handleRemoteException(e, "stopRxFilter");
                return false;
            }
        }
    }

    public boolean addRxFilter(String str, int i) {
        synchronized (this.mLock) {
            byte b = 0;
            switch (i) {
                case 0:
                    break;
                case 1:
                    b = 1;
                    break;
                default:
                    Log.e(TAG, "Invalid Rx Filter type: " + i);
                    return false;
            }
            return addRxFilter(str, b);
        }
    }

    private boolean addRxFilter(String str, byte b) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "addRxFilter");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.addRxFilter(b), "addRxFilter");
            } catch (RemoteException e) {
                handleRemoteException(e, "addRxFilter");
                return false;
            }
        }
    }

    public boolean removeRxFilter(String str, int i) {
        synchronized (this.mLock) {
            byte b = 0;
            switch (i) {
                case 0:
                    break;
                case 1:
                    b = 1;
                    break;
                default:
                    Log.e(TAG, "Invalid Rx Filter type: " + i);
                    return false;
            }
            return removeRxFilter(str, b);
        }
    }

    private boolean removeRxFilter(String str, byte b) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "removeRxFilter");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.removeRxFilter(b), "removeRxFilter");
            } catch (RemoteException e) {
                handleRemoteException(e, "removeRxFilter");
                return false;
            }
        }
    }

    public boolean setBtCoexistenceMode(String str, int i) {
        synchronized (this.mLock) {
            byte b = 0;
            switch (i) {
                case 0:
                    break;
                case 1:
                    b = 1;
                    break;
                case 2:
                    b = 2;
                    break;
                default:
                    Log.e(TAG, "Invalid Bt Coex mode: " + i);
                    return false;
            }
            return setBtCoexistenceMode(str, b);
        }
    }

    private boolean setBtCoexistenceMode(String str, byte b) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setBtCoexistenceMode");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setBtCoexistenceMode(b), "setBtCoexistenceMode");
            } catch (RemoteException e) {
                handleRemoteException(e, "setBtCoexistenceMode");
                return false;
            }
        }
    }

    public boolean setBtCoexistenceScanModeEnabled(String str, boolean z) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setBtCoexistenceScanModeEnabled");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setBtCoexistenceScanModeEnabled(z), "setBtCoexistenceScanModeEnabled");
            } catch (RemoteException e) {
                handleRemoteException(e, "setBtCoexistenceScanModeEnabled");
                return false;
            }
        }
    }

    public boolean setSuspendModeEnabled(String str, boolean z) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setSuspendModeEnabled");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setSuspendModeEnabled(z), "setSuspendModeEnabled");
            } catch (RemoteException e) {
                handleRemoteException(e, "setSuspendModeEnabled");
                return false;
            }
        }
    }

    public boolean setCountryCode(String str, String str2) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(str2)) {
                return false;
            }
            return setCountryCode(str, NativeUtil.stringToByteArray(str2));
        }
    }

    private boolean setCountryCode(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setCountryCode");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setCountryCode(bArr), "setCountryCode");
            } catch (RemoteException e) {
                handleRemoteException(e, "setCountryCode");
                return false;
            }
        }
    }

    public boolean startWpsRegistrar(String str, String str2, String str3) {
        synchronized (this.mLock) {
            if (TextUtils.isEmpty(str2) || TextUtils.isEmpty(str3)) {
                return false;
            }
            try {
                return startWpsRegistrar(str, NativeUtil.macAddressToByteArray(str2), str3);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument " + str2, e);
                return false;
            }
        }
    }

    private boolean startWpsRegistrar(String str, byte[] bArr, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "startWpsRegistrar");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.startWpsRegistrar(bArr, str2), "startWpsRegistrar");
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsRegistrar");
                return false;
            }
        }
    }

    public boolean startWpsPbc(String str, String str2) {
        boolean zStartWpsPbc;
        synchronized (this.mLock) {
            try {
                try {
                    zStartWpsPbc = startWpsPbc(str, NativeUtil.macAddressToByteArray(str2));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zStartWpsPbc;
    }

    private boolean startWpsPbc(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "startWpsPbc");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.startWpsPbc(bArr), "startWpsPbc");
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPbc");
                return false;
            }
        }
    }

    public boolean startWpsPinKeypad(String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return false;
        }
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "startWpsPinKeypad");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.startWpsPinKeypad(str2), "startWpsPinKeypad");
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPinKeypad");
                return false;
            }
        }
    }

    public String startWpsPinDisplay(String str, String str2) {
        String strStartWpsPinDisplay;
        synchronized (this.mLock) {
            try {
                try {
                    strStartWpsPinDisplay = startWpsPinDisplay(str, NativeUtil.macAddressToByteArray(str2));
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Illegal argument " + str2, e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return strStartWpsPinDisplay;
    }

    private String startWpsPinDisplay(String str, byte[] bArr) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "startWpsPinDisplay");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return null;
            }
            final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
            try {
                iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.startWpsPinDisplay(bArr, new ISupplicantStaIface.startWpsPinDisplayCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str2) {
                        SupplicantStaIfaceHal.lambda$startWpsPinDisplay$9(this.f$0, mutable, supplicantStatus, str2);
                    }
                });
            } catch (RemoteException e) {
                handleRemoteException(e, "startWpsPinDisplay");
            }
            return (String) mutable.value;
        }
    }

    public static void lambda$startWpsPinDisplay$9(SupplicantStaIfaceHal supplicantStaIfaceHal, HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, String str) {
        if (supplicantStaIfaceHal.checkStatusAndLogFailure(supplicantStatus, "startWpsPinDisplay")) {
            mutable.value = str;
        }
    }

    public boolean cancelWps(String str) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "cancelWps");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.cancelWps(), "cancelWps");
            } catch (RemoteException e) {
                handleRemoteException(e, "cancelWps");
                return false;
            }
        }
    }

    public boolean setExternalSim(String str, boolean z) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "setExternalSim");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.setExternalSim(z), "setExternalSim");
            } catch (RemoteException e) {
                handleRemoteException(e, "setExternalSim");
                return false;
            }
        }
    }

    public boolean enableAutoReconnect(String str, boolean z) {
        synchronized (this.mLock) {
            ISupplicantStaIface iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure = checkSupplicantStaIfaceAndLogFailure(str, "enableAutoReconnect");
            if (iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure == null) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(iSupplicantStaIfaceCheckSupplicantStaIfaceAndLogFailure.enableAutoReconnect(z), "enableAutoReconnect");
            } catch (RemoteException e) {
                handleRemoteException(e, "enableAutoReconnect");
                return false;
            }
        }
    }

    public boolean setLogLevel(boolean z) {
        synchronized (this.mLock) {
            if (!"user".equals(Build.TYPE)) {
                return setDebugParams(z ? 0 : 3, true, true);
            }
            return setDebugParams(z ? 2 : 3, false, false);
        }
    }

    private boolean setDebugParams(int i, boolean z, boolean z2) {
        synchronized (this.mLock) {
            if (!checkSupplicantAndLogFailure("setDebugParams")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicant.setDebugParams(i, z, z2), "setDebugParams");
            } catch (RemoteException e) {
                handleRemoteException(e, "setDebugParams");
                return false;
            }
        }
    }

    public boolean setConcurrencyPriority(boolean z) {
        synchronized (this.mLock) {
            try {
                if (z) {
                    return setConcurrencyPriority(0);
                }
                return setConcurrencyPriority(1);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean setConcurrencyPriority(int i) {
        synchronized (this.mLock) {
            if (!checkSupplicantAndLogFailure("setConcurrencyPriority")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mISupplicant.setConcurrencyPriority(i), "setConcurrencyPriority");
            } catch (RemoteException e) {
                handleRemoteException(e, "setConcurrencyPriority");
                return false;
            }
        }
    }

    private boolean checkSupplicantAndLogFailure(String str) {
        synchronized (this.mLock) {
            if (this.mISupplicant == null) {
                Log.e(TAG, "Can't call " + str + ", ISupplicant is null");
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Do ISupplicant." + str);
            }
            return true;
        }
    }

    private ISupplicantStaIface checkSupplicantStaIfaceAndLogFailure(String str, String str2) {
        synchronized (this.mLock) {
            ISupplicantStaIface staIface = getStaIface(str);
            if (staIface == null) {
                Log.e(TAG, "Can't call " + str2 + ", ISupplicantStaIface is null");
                return null;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "Do ISupplicantStaIface." + str2);
            }
            return staIface;
        }
    }

    private SupplicantStaNetworkHal checkSupplicantStaNetworkAndLogFailure(String str, String str2) {
        synchronized (this.mLock) {
            SupplicantStaNetworkHal currentNetworkRemoteHandle = getCurrentNetworkRemoteHandle(str);
            if (currentNetworkRemoteHandle != null) {
                return currentNetworkRemoteHandle;
            }
            Log.e(TAG, "Can't call " + str2 + ", SupplicantStaNetwork is null");
            return null;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus supplicantStatus, String str) {
        synchronized (this.mLock) {
            if (supplicantStatus.code != 0) {
                Log.e(TAG, "ISupplicantStaIface." + str + " failed: " + supplicantStatus);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaIface." + str + " succeeded");
            }
            return true;
        }
    }

    private void logCallback(String str) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "ISupplicantStaIfaceCallback." + str + " received");
            }
        }
    }

    private void handleRemoteException(RemoteException remoteException, String str) {
        synchronized (this.mLock) {
            clearState();
            Log.e(TAG, "ISupplicantStaIface." + str + " failed with exception", remoteException);
        }
    }

    private static short stringToWpsConfigMethod(String str) {
        switch (str) {
            case "usba":
                return (short) 1;
            case "ethernet":
                return (short) 2;
            case "label":
                return (short) 4;
            case "display":
                return (short) 8;
            case "int_nfc_token":
                return (short) 32;
            case "ext_nfc_token":
                return (short) 16;
            case "nfc_interface":
                return (short) 64;
            case "push_button":
                return WpsConfigMethods.PUSHBUTTON;
            case "keypad":
                return WpsConfigMethods.KEYPAD;
            case "virtual_push_button":
                return WpsConfigMethods.VIRT_PUSHBUTTON;
            case "physical_push_button":
                return WpsConfigMethods.PHY_PUSHBUTTON;
            case "p2ps":
                return WpsConfigMethods.P2PS;
            case "virtual_display":
                return WpsConfigMethods.VIRT_DISPLAY;
            case "physical_display":
                return WpsConfigMethods.PHY_DISPLAY;
            default:
                throw new IllegalArgumentException("Invalid WPS config method: " + str);
        }
    }

    private static SupplicantState supplicantHidlStateToFrameworkState(int i) {
        switch (i) {
            case 0:
                return SupplicantState.DISCONNECTED;
            case 1:
                return SupplicantState.INTERFACE_DISABLED;
            case 2:
                return SupplicantState.INACTIVE;
            case 3:
                return SupplicantState.SCANNING;
            case 4:
                return SupplicantState.AUTHENTICATING;
            case 5:
                return SupplicantState.ASSOCIATING;
            case 6:
                return SupplicantState.ASSOCIATED;
            case 7:
                return SupplicantState.FOUR_WAY_HANDSHAKE;
            case 8:
                return SupplicantState.GROUP_HANDSHAKE;
            case 9:
                return SupplicantState.COMPLETED;
            default:
                throw new IllegalArgumentException("Invalid state: " + i);
        }
    }

    private class SupplicantStaIfaceHalCallback extends ISupplicantStaIfaceCallback.Stub {
        private String mIfaceName;
        private boolean mStateIsFourway = false;

        SupplicantStaIfaceHalCallback(String str) {
            this.mIfaceName = str;
        }

        private ANQPElement parseAnqpElement(Constants.ANQPElementType aNQPElementType, ArrayList<Byte> arrayList) {
            ANQPElement hS20Element;
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                try {
                    try {
                        if (Constants.getANQPElementID(aNQPElementType) != null) {
                            hS20Element = ANQPParser.parseElement(aNQPElementType, ByteBuffer.wrap(NativeUtil.byteArrayFromArrayList(arrayList)));
                        } else {
                            hS20Element = ANQPParser.parseHS20Element(aNQPElementType, ByteBuffer.wrap(NativeUtil.byteArrayFromArrayList(arrayList)));
                        }
                    } catch (IOException | BufferUnderflowException e) {
                        Log.e(SupplicantStaIfaceHal.TAG, "Failed parsing ANQP element payload: " + aNQPElementType, e);
                        return null;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return hS20Element;
        }

        private void addAnqpElementToMap(Map<Constants.ANQPElementType, ANQPElement> map, Constants.ANQPElementType aNQPElementType, ArrayList<Byte> arrayList) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                if (arrayList != null) {
                    try {
                        if (!arrayList.isEmpty()) {
                            ANQPElement anqpElement = parseAnqpElement(aNQPElementType, arrayList);
                            if (anqpElement != null) {
                                map.put(aNQPElementType, anqpElement);
                            }
                        }
                    } finally {
                    }
                }
            }
        }

        @Override
        public void onNetworkAdded(int i) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onNetworkAdded");
            }
        }

        @Override
        public void onNetworkRemoved(int i) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onNetworkRemoved");
            }
        }

        @Override
        public void onStateChanged(int i, byte[] bArr, int i2, ArrayList<Byte> arrayList) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onStateChanged");
                SupplicantState supplicantStateSupplicantHidlStateToFrameworkState = SupplicantStaIfaceHal.supplicantHidlStateToFrameworkState(i);
                WifiSsid wifiSsidCreateFromByteArray = WifiSsid.createFromByteArray(NativeUtil.byteArrayFromArrayList(arrayList));
                MtkGbkSsid.checkAndSetGbk(wifiSsidCreateFromByteArray);
                String strMacAddressFromByteArray = NativeUtil.macAddressFromByteArray(bArr);
                this.mStateIsFourway = i == 7;
                if (supplicantStateSupplicantHidlStateToFrameworkState == SupplicantState.COMPLETED) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkConnectionEvent(this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(this.mIfaceName), strMacAddressFromByteArray);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastSupplicantStateChangeEvent(this.mIfaceName, SupplicantStaIfaceHal.this.getCurrentNetworkId(this.mIfaceName), wifiSsidCreateFromByteArray, strMacAddressFromByteArray, supplicantStateSupplicantHidlStateToFrameworkState);
            }
        }

        @Override
        public void onAnqpQueryDone(byte[] bArr, ISupplicantStaIfaceCallback.AnqpData anqpData, ISupplicantStaIfaceCallback.Hs20AnqpData hs20AnqpData) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAnqpQueryDone");
                HashMap map = new HashMap();
                addAnqpElementToMap(map, Constants.ANQPElementType.ANQPVenueName, anqpData.venueName);
                addAnqpElementToMap(map, Constants.ANQPElementType.ANQPRoamingConsortium, anqpData.roamingConsortium);
                addAnqpElementToMap(map, Constants.ANQPElementType.ANQPIPAddrAvailability, anqpData.ipAddrTypeAvailability);
                addAnqpElementToMap(map, Constants.ANQPElementType.ANQPNAIRealm, anqpData.naiRealm);
                addAnqpElementToMap(map, Constants.ANQPElementType.ANQP3GPPNetwork, anqpData.anqp3gppCellularNetwork);
                addAnqpElementToMap(map, Constants.ANQPElementType.ANQPDomName, anqpData.domainName);
                addAnqpElementToMap(map, Constants.ANQPElementType.HSFriendlyName, hs20AnqpData.operatorFriendlyName);
                addAnqpElementToMap(map, Constants.ANQPElementType.HSWANMetrics, hs20AnqpData.wanMetrics);
                addAnqpElementToMap(map, Constants.ANQPElementType.HSConnCapability, hs20AnqpData.connectionCapability);
                addAnqpElementToMap(map, Constants.ANQPElementType.HSOSUProviders, hs20AnqpData.osuProvidersList);
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAnqpDoneEvent(this.mIfaceName, new AnqpEvent(NativeUtil.macAddressToLong(bArr).longValue(), map));
            }
        }

        @Override
        public void onHs20IconQueryDone(byte[] bArr, String str, ArrayList<Byte> arrayList) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20IconQueryDone");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastIconDoneEvent(this.mIfaceName, new IconEvent(NativeUtil.macAddressToLong(bArr).longValue(), str, arrayList.size(), NativeUtil.byteArrayFromArrayList(arrayList)));
            }
        }

        @Override
        public void onHs20SubscriptionRemediation(byte[] bArr, byte b, String str) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20SubscriptionRemediation");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWnmEvent(this.mIfaceName, new WnmData(NativeUtil.macAddressToLong(bArr).longValue(), str, b));
            }
        }

        @Override
        public void onHs20DeauthImminentNotice(byte[] bArr, int i, int i2, String str) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onHs20DeauthImminentNotice");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWnmEvent(this.mIfaceName, new WnmData(NativeUtil.macAddressToLong(bArr).longValue(), str, i == 1, i2));
            }
        }

        @Override
        public void onDisconnected(byte[] bArr, boolean z, int i) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onDisconnected");
                if (SupplicantStaIfaceHal.this.mVerboseLoggingEnabled) {
                    Log.e(SupplicantStaIfaceHal.TAG, "onDisconnected 4way=" + this.mStateIsFourway + " locallyGenerated=" + z + " reasonCode=" + i);
                }
                if (this.mStateIsFourway && (!z || i != 17)) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 2, -1);
                }
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastNetworkDisconnectionEvent(this.mIfaceName, z ? 1 : 0, i, NativeUtil.macAddressFromByteArray(bArr));
            }
        }

        @Override
        public void onAssociationRejected(byte[] bArr, int i, boolean z) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAssociationRejected");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociationRejectionEvent(this.mIfaceName, i, z, NativeUtil.macAddressFromByteArray(bArr));
            }
        }

        @Override
        public void onAuthenticationTimeout(byte[] bArr) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onAuthenticationTimeout");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 1, -1);
            }
        }

        @Override
        public void onBssidChanged(byte b, byte[] bArr) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onBssidChanged");
                if (b == 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastTargetBssidEvent(this.mIfaceName, NativeUtil.macAddressFromByteArray(bArr));
                } else if (b == 1) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAssociatedBssidEvent(this.mIfaceName, NativeUtil.macAddressFromByteArray(bArr));
                }
            }
        }

        @Override
        public void onEapFailure() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 3, -1);
            }
        }

        @Override
        public void onWpsEventSuccess() {
            SupplicantStaIfaceHal.this.logCallback("onWpsEventSuccess");
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsSuccessEvent(this.mIfaceName);
            }
        }

        @Override
        public void onWpsEventFail(byte[] bArr, short s, short s2) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventFail");
                if (s != 16 || s2 != 0) {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsFailEvent(this.mIfaceName, s, s2);
                } else {
                    SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsTimeoutEvent(this.mIfaceName);
                }
            }
        }

        @Override
        public void onWpsEventPbcOverlap() {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onWpsEventPbcOverlap");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastWpsOverlapEvent(this.mIfaceName);
            }
        }

        @Override
        public void onExtRadioWorkStart(int i) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onExtRadioWorkStart");
            }
        }

        @Override
        public void onExtRadioWorkTimeout(int i) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onExtRadioWorkTimeout");
            }
        }
    }

    private class SupplicantStaIfaceHalCallbackV1_1 extends ISupplicantStaIfaceCallback.Stub {
        private SupplicantStaIfaceHalCallback mCallbackV1_0;
        private String mIfaceName;

        SupplicantStaIfaceHalCallbackV1_1(String str, SupplicantStaIfaceHalCallback supplicantStaIfaceHalCallback) {
            this.mIfaceName = str;
            this.mCallbackV1_0 = supplicantStaIfaceHalCallback;
        }

        @Override
        public void onNetworkAdded(int i) {
            this.mCallbackV1_0.onNetworkAdded(i);
        }

        @Override
        public void onNetworkRemoved(int i) {
            this.mCallbackV1_0.onNetworkRemoved(i);
        }

        @Override
        public void onStateChanged(int i, byte[] bArr, int i2, ArrayList<Byte> arrayList) {
            this.mCallbackV1_0.onStateChanged(i, bArr, i2, arrayList);
        }

        @Override
        public void onAnqpQueryDone(byte[] bArr, ISupplicantStaIfaceCallback.AnqpData anqpData, ISupplicantStaIfaceCallback.Hs20AnqpData hs20AnqpData) {
            this.mCallbackV1_0.onAnqpQueryDone(bArr, anqpData, hs20AnqpData);
        }

        @Override
        public void onHs20IconQueryDone(byte[] bArr, String str, ArrayList<Byte> arrayList) {
            this.mCallbackV1_0.onHs20IconQueryDone(bArr, str, arrayList);
        }

        @Override
        public void onHs20SubscriptionRemediation(byte[] bArr, byte b, String str) {
            this.mCallbackV1_0.onHs20SubscriptionRemediation(bArr, b, str);
        }

        @Override
        public void onHs20DeauthImminentNotice(byte[] bArr, int i, int i2, String str) {
            this.mCallbackV1_0.onHs20DeauthImminentNotice(bArr, i, i2, str);
        }

        @Override
        public void onDisconnected(byte[] bArr, boolean z, int i) {
            this.mCallbackV1_0.onDisconnected(bArr, z, i);
        }

        @Override
        public void onAssociationRejected(byte[] bArr, int i, boolean z) {
            this.mCallbackV1_0.onAssociationRejected(bArr, i, z);
        }

        @Override
        public void onAuthenticationTimeout(byte[] bArr) {
            this.mCallbackV1_0.onAuthenticationTimeout(bArr);
        }

        @Override
        public void onBssidChanged(byte b, byte[] bArr) {
            this.mCallbackV1_0.onBssidChanged(b, bArr);
        }

        @Override
        public void onEapFailure() {
            this.mCallbackV1_0.onEapFailure();
        }

        @Override
        public void onEapFailure_1_1(int i) {
            synchronized (SupplicantStaIfaceHal.this.mLock) {
                SupplicantStaIfaceHal.this.logCallback("onEapFailure_1_1");
                SupplicantStaIfaceHal.this.mWifiMonitor.broadcastAuthenticationFailureEvent(this.mIfaceName, 3, i);
            }
        }

        @Override
        public void onWpsEventSuccess() {
            this.mCallbackV1_0.onWpsEventSuccess();
        }

        @Override
        public void onWpsEventFail(byte[] bArr, short s, short s2) {
            this.mCallbackV1_0.onWpsEventFail(bArr, s, s2);
        }

        @Override
        public void onWpsEventPbcOverlap() {
            this.mCallbackV1_0.onWpsEventPbcOverlap();
        }

        @Override
        public void onExtRadioWorkStart(int i) {
            this.mCallbackV1_0.onExtRadioWorkStart(i);
        }

        @Override
        public void onExtRadioWorkTimeout(int i) {
            this.mCallbackV1_0.onExtRadioWorkTimeout(i);
        }
    }

    private static void logd(String str) {
        Log.d(TAG, str);
    }

    private static void logi(String str) {
        Log.i(TAG, str);
    }

    private static void loge(String str) {
        Log.e(TAG, str);
    }
}
