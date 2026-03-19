package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.ISupplicantIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIface;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pNetwork;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.hardware.wifi.supplicant.V1_1.ISupplicant;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.util.NativeUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SupplicantP2pIfaceHal {
    private static final boolean DBG = true;
    private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
    private static final int DEFAULT_OPERATING_CLASS = 81;
    private static final int RESULT_NOT_VALID = -1;
    private static final String TAG = "SupplicantP2pIfaceHal";
    private static final Pattern WPS_DEVICE_TYPE_PATTERN = Pattern.compile("^(\\d{1,2})-([0-9a-fA-F]{8})-(\\d{1,2})$");
    private final WifiP2pMonitor mMonitor;
    private Object mLock = new Object();
    private IServiceManager mIServiceManager = null;
    private ISupplicant mISupplicant = null;
    private ISupplicantIface mHidlSupplicantIface = null;
    private ISupplicantP2pIface mISupplicantP2pIface = null;
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String str, String str2, boolean z) {
            synchronized (SupplicantP2pIfaceHal.this.mLock) {
                Log.i(SupplicantP2pIfaceHal.TAG, "IServiceNotification.onRegistration for: " + str + ", " + str2 + " preexisting=" + z);
                if (!SupplicantP2pIfaceHal.this.initSupplicantService()) {
                    Log.e(SupplicantP2pIfaceHal.TAG, "initalizing ISupplicant failed.");
                    SupplicantP2pIfaceHal.this.supplicantServiceDiedHandler();
                } else {
                    Log.i(SupplicantP2pIfaceHal.TAG, "Completed initialization of ISupplicant interfaces.");
                }
            }
        }
    };
    private final IHwBinder.DeathRecipient mServiceManagerDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            SupplicantP2pIfaceHal.lambda$new$0(this.f$0, j);
        }
    };
    private final IHwBinder.DeathRecipient mSupplicantDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            SupplicantP2pIfaceHal.lambda$new$1(this.f$0, j);
        }
    };
    private SupplicantP2pIfaceCallback mCallback = null;

    public static void lambda$new$0(SupplicantP2pIfaceHal supplicantP2pIfaceHal, long j) {
        Log.w(TAG, "IServiceManager died: cookie=" + j);
        synchronized (supplicantP2pIfaceHal.mLock) {
            supplicantP2pIfaceHal.supplicantServiceDiedHandler();
            supplicantP2pIfaceHal.mIServiceManager = null;
        }
    }

    public static void lambda$new$1(SupplicantP2pIfaceHal supplicantP2pIfaceHal, long j) {
        Log.w(TAG, "ISupplicant/ISupplicantStaIface died: cookie=" + j);
        synchronized (supplicantP2pIfaceHal.mLock) {
            supplicantP2pIfaceHal.supplicantServiceDiedHandler();
        }
    }

    public SupplicantP2pIfaceHal(WifiP2pMonitor wifiP2pMonitor) {
        this.mMonitor = wifiP2pMonitor;
    }

    private boolean linkToServiceManagerDeath() {
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

    public boolean initialize() {
        Log.i(TAG, "Registering ISupplicant service ready callback.");
        synchronized (this.mLock) {
            if (this.mIServiceManager != null) {
                Log.i(TAG, "Supplicant HAL already initialized.");
                return true;
            }
            this.mISupplicant = null;
            this.mISupplicantP2pIface = null;
            try {
                this.mIServiceManager = getServiceManagerMockable();
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
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for ISupplicant service: " + e);
                supplicantServiceDiedHandler();
                return false;
            }
        }
    }

    private boolean linkToSupplicantDeath() {
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

    private boolean linkToSupplicantP2pIfaceDeath() {
        if (this.mISupplicantP2pIface == null) {
            return false;
        }
        try {
            if (!this.mISupplicantP2pIface.linkToDeath(this.mSupplicantDeathRecipient, 0L)) {
                Log.wtf(TAG, "Error on linkToDeath on ISupplicantP2pIface");
                supplicantServiceDiedHandler();
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "ISupplicantP2pIface.linkToDeath exception", e);
            return false;
        }
    }

    public boolean setupIface(String str) {
        ISupplicantIface ifaceV1_0;
        synchronized (this.mLock) {
            if (this.mISupplicantP2pIface != null) {
                return false;
            }
            if (isV1_1()) {
                ifaceV1_0 = addIfaceV1_1(str);
            } else {
                ifaceV1_0 = getIfaceV1_0(str);
            }
            if (ifaceV1_0 == null) {
                Log.e(TAG, "initSupplicantP2pIface got null iface");
                return false;
            }
            this.mISupplicantP2pIface = getP2pIfaceMockable(ifaceV1_0);
            if (!linkToSupplicantP2pIfaceDeath()) {
                return false;
            }
            if (this.mISupplicantP2pIface != null && this.mMonitor != null) {
                this.mCallback = new SupplicantP2pIfaceCallback(str, this.mMonitor);
                if (!registerCallback(this.mCallback)) {
                    Log.e(TAG, "Callback registration failed. Initialization incomplete.");
                    return false;
                }
            }
            return true;
        }
    }

    private ISupplicantIface getIfaceV1_0(String str) {
        final ArrayList arrayList = new ArrayList();
        try {
            this.mISupplicant.listInterfaces(new ISupplicant.listInterfacesCallback() {
                @Override
                public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList2) {
                    SupplicantP2pIfaceHal.lambda$getIfaceV1_0$2(arrayList, supplicantStatus, arrayList2);
                }
            });
            if (arrayList.size() == 0) {
                Log.e(TAG, "Got zero HIDL supplicant ifaces. Stopping supplicant HIDL startup.");
                supplicantServiceDiedHandler();
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getInterface()");
            Iterator it = arrayList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ISupplicant.IfaceInfo ifaceInfo = (ISupplicant.IfaceInfo) it.next();
                if (ifaceInfo.type == 1 && str.equals(ifaceInfo.name)) {
                    try {
                        this.mISupplicant.getInterface(ifaceInfo, new ISupplicant.getInterfaceCallback() {
                            @Override
                            public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
                                SupplicantP2pIfaceHal.lambda$getIfaceV1_0$3(supplicantResult, supplicantStatus, iSupplicantIface);
                            }
                        });
                        break;
                    } catch (RemoteException e) {
                        Log.e(TAG, "ISupplicant.getInterface exception: " + e);
                        supplicantServiceDiedHandler();
                        return null;
                    }
                }
            }
            return (ISupplicantIface) supplicantResult.getResult();
        } catch (RemoteException e2) {
            Log.e(TAG, "ISupplicant.listInterfaces exception: " + e2);
            return null;
        }
    }

    static void lambda$getIfaceV1_0$2(ArrayList arrayList, SupplicantStatus supplicantStatus, ArrayList arrayList2) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "Getting Supplicant Interfaces failed: " + supplicantStatus.code);
            return;
        }
        arrayList.addAll(arrayList2);
    }

    static void lambda$getIfaceV1_0$3(SupplicantResult supplicantResult, SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "Failed to get ISupplicantIface " + supplicantStatus.code);
            return;
        }
        supplicantResult.setResult(supplicantStatus, iSupplicantIface);
    }

    private ISupplicantIface addIfaceV1_1(String str) {
        synchronized (this.mLock) {
            ISupplicant.IfaceInfo ifaceInfo = new ISupplicant.IfaceInfo();
            ifaceInfo.name = str;
            ifaceInfo.type = 1;
            final SupplicantResult supplicantResult = new SupplicantResult("addInterface(" + ifaceInfo + ")");
            try {
                android.hardware.wifi.supplicant.V1_1.ISupplicant supplicantMockableV1_1 = getSupplicantMockableV1_1();
                if (supplicantMockableV1_1 == null) {
                    Log.e(TAG, "Can't call addIface: ISupplicantP2pIface is null");
                    return null;
                }
                supplicantMockableV1_1.addInterface(ifaceInfo, new ISupplicant.addInterfaceCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
                        SupplicantP2pIfaceHal.lambda$addIfaceV1_1$4(supplicantResult, supplicantStatus, iSupplicantIface);
                    }
                });
                return (ISupplicantIface) supplicantResult.getResult();
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicant.addInterface exception: " + e);
                supplicantServiceDiedHandler();
                return null;
            }
        }
    }

    static void lambda$addIfaceV1_1$4(SupplicantResult supplicantResult, SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        if (supplicantStatus.code != 0 && supplicantStatus.code != 5) {
            Log.e(TAG, "Failed to get ISupplicantIface " + supplicantStatus.code);
            return;
        }
        supplicantResult.setResult(supplicantStatus, iSupplicantIface);
    }

    public boolean teardownIface(String str) {
        synchronized (this.mLock) {
            if (this.mISupplicantP2pIface == null) {
                return false;
            }
            if (isV1_1()) {
                return removeIfaceV1_1(str);
            }
            return true;
        }
    }

    private boolean removeIfaceV1_1(String str) {
        synchronized (this.mLock) {
            try {
                try {
                    android.hardware.wifi.supplicant.V1_1.ISupplicant supplicantMockableV1_1 = getSupplicantMockableV1_1();
                    if (supplicantMockableV1_1 == null) {
                        Log.e(TAG, "Can't call removeIface: ISupplicantP2pIface is null");
                        return false;
                    }
                    ISupplicant.IfaceInfo ifaceInfo = new ISupplicant.IfaceInfo();
                    ifaceInfo.name = str;
                    ifaceInfo.type = 1;
                    SupplicantStatus supplicantStatusRemoveInterface = supplicantMockableV1_1.removeInterface(ifaceInfo);
                    if (supplicantStatusRemoveInterface.code != 0) {
                        Log.e(TAG, "Failed to remove iface " + supplicantStatusRemoveInterface.code);
                        return false;
                    }
                    this.mCallback = null;
                    this.mISupplicantP2pIface = null;
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicant.removeInterface exception: " + e);
                    supplicantServiceDiedHandler();
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void supplicantServiceDiedHandler() {
        synchronized (this.mLock) {
            this.mISupplicant = null;
            this.mISupplicantP2pIface = null;
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
        return this.mISupplicant != null;
    }

    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        return IServiceManager.getService();
    }

    protected android.hardware.wifi.supplicant.V1_0.ISupplicant getSupplicantMockable() throws RemoteException {
        try {
            return android.hardware.wifi.supplicant.V1_0.ISupplicant.getService();
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Failed to get ISupplicant", e);
            return null;
        }
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

    protected ISupplicantP2pIface getP2pIfaceMockable(ISupplicantIface iSupplicantIface) {
        return ISupplicantP2pIface.asInterface(iSupplicantIface.asBinder());
    }

    protected ISupplicantP2pNetwork getP2pNetworkMockable(ISupplicantNetwork iSupplicantNetwork) {
        return ISupplicantP2pNetwork.asInterface(iSupplicantNetwork.asBinder());
    }

    private boolean isV1_1() {
        boolean z;
        synchronized (this.mLock) {
            try {
                try {
                    z = getSupplicantMockableV1_1() != null;
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicant.getService exception: " + e);
                    supplicantServiceDiedHandler();
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return z;
    }

    protected static void logd(String str) {
        Log.d(TAG, str);
    }

    protected static void logCompletion(String str, SupplicantStatus supplicantStatus) {
        if (supplicantStatus == null) {
            Log.w(TAG, str + " failed: no status code returned.");
            return;
        }
        if (supplicantStatus.code == 0) {
            logd(str + " completed successfully.");
            return;
        }
        Log.w(TAG, str + " failed: " + supplicantStatus.code + " (" + supplicantStatus.debugMessage + ")");
    }

    private boolean checkSupplicantP2pIfaceAndLogFailure(String str) {
        if (this.mISupplicantP2pIface == null) {
            Log.e(TAG, "Can't call " + str + ": ISupplicantP2pIface is null");
            return false;
        }
        return true;
    }

    private int wpsInfoToConfigMethod(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
            case 3:
                return 2;
            default:
                Log.e(TAG, "Unsupported WPS provision method: " + i);
                return -1;
        }
    }

    public String getName() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getName")) {
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getName()");
            try {
                this.mISupplicantP2pIface.getName(new ISupplicantIface.getNameCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, String str) {
                        supplicantResult.setResult(supplicantStatus, str);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return (String) supplicantResult.getResult();
        }
    }

    public boolean registerCallback(ISupplicantP2pIfaceCallback iSupplicantP2pIfaceCallback) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("registerCallback")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("registerCallback()");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.registerCallback(iSupplicantP2pIfaceCallback));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean find(int i) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("find")) {
                return false;
            }
            if (i < 0) {
                Log.e(TAG, "Invalid timeout value: " + i);
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("find(" + i + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.find(i));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean stopFind() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("stopFind")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("stopFind()");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.stopFind());
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean flush() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("flush")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("flush()");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.flush());
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean serviceFlush() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("serviceFlush")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("serviceFlush()");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.flushServices());
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean setPowerSave(String str, boolean z) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setPowerSave")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setPowerSave(" + str + ", " + z + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.setPowerSave(str, z));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean setGroupIdle(String str, int i) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setGroupIdle")) {
                return false;
            }
            if (i < 0) {
                Log.e(TAG, "Invalid group timeout value " + i);
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setGroupIdle(" + str + ", " + i + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.setGroupIdle(str, i));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean setSsidPostfix(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setSsidPostfix")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "Invalid SSID postfix value (null).");
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setSsidPostfix(" + str + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.setSsidPostfix(NativeUtil.decodeSsid("\"" + str + "\"")));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Could not decode SSID.", e2);
                return false;
            }
            return supplicantResult.isSuccess();
        }
    }

    public String connect(WifiP2pConfig wifiP2pConfig, boolean z) {
        int i;
        if (wifiP2pConfig == null) {
            return null;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setSsidPostfix")) {
                return null;
            }
            if (wifiP2pConfig == null) {
                Log.e(TAG, "Could not connect: null config.");
                return null;
            }
            if (wifiP2pConfig.deviceAddress == null) {
                Log.e(TAG, "Could not parse null mac address.");
                return null;
            }
            if (wifiP2pConfig.wps.setup == 0 && !TextUtils.isEmpty(wifiP2pConfig.wps.pin)) {
                Log.e(TAG, "Expected empty pin for PBC.");
                return null;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(wifiP2pConfig.deviceAddress);
                int iWpsInfoToConfigMethod = wpsInfoToConfigMethod(wifiP2pConfig.wps.setup);
                if (iWpsInfoToConfigMethod == -1) {
                    Log.e(TAG, "Invalid WPS config method: " + wifiP2pConfig.wps.setup);
                    return null;
                }
                String str = TextUtils.isEmpty(wifiP2pConfig.wps.pin) ? "" : wifiP2pConfig.wps.pin;
                boolean z2 = wifiP2pConfig.netId == -2;
                if (z) {
                    i = 0;
                } else {
                    int i2 = wifiP2pConfig.groupOwnerIntent;
                    if (i2 < 0 || i2 > 15) {
                        i2 = 6;
                    }
                    i = i2;
                }
                final SupplicantResult supplicantResult = new SupplicantResult("connect(" + wifiP2pConfig.deviceAddress + ")");
                try {
                    this.mISupplicantP2pIface.connect(bArrMacAddressToByteArray, iWpsInfoToConfigMethod, str, z, z2, i, new ISupplicantP2pIface.connectCallback() {
                        @Override
                        public final void onValues(SupplicantStatus supplicantStatus, String str2) {
                            supplicantResult.setResult(supplicantStatus, str2);
                        }
                    });
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return (String) supplicantResult.getResult();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse peer mac address.", e2);
                return null;
            }
        }
    }

    public boolean cancelConnect() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("cancelConnect")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("cancelConnect()");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.cancelConnect());
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean provisionDiscovery(WifiP2pConfig wifiP2pConfig) {
        if (wifiP2pConfig == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("provisionDiscovery")) {
                return false;
            }
            int iWpsInfoToConfigMethod = wpsInfoToConfigMethod(wifiP2pConfig.wps.setup);
            if (iWpsInfoToConfigMethod == -1) {
                Log.e(TAG, "Unrecognized WPS configuration method: " + wifiP2pConfig.wps.setup);
                return false;
            }
            if (iWpsInfoToConfigMethod != 1) {
                if (iWpsInfoToConfigMethod == 2) {
                    iWpsInfoToConfigMethod = 1;
                }
            } else {
                iWpsInfoToConfigMethod = 2;
            }
            if (wifiP2pConfig.deviceAddress == null) {
                Log.e(TAG, "Cannot parse null mac address.");
                return false;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(wifiP2pConfig.deviceAddress);
                SupplicantResult supplicantResult = new SupplicantResult("provisionDiscovery(" + wifiP2pConfig.deviceAddress + ", " + wifiP2pConfig.wps.setup + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.provisionDiscovery(bArrMacAddressToByteArray, iWpsInfoToConfigMethod));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse peer mac address.", e2);
                return false;
            }
        }
    }

    public boolean invite(WifiP2pGroup wifiP2pGroup, String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("invite")) {
                return false;
            }
            if (wifiP2pGroup == null) {
                Log.e(TAG, "Cannot invite to null group.");
                return false;
            }
            if (wifiP2pGroup.getOwner() == null) {
                Log.e(TAG, "Cannot invite to group with null owner.");
                return false;
            }
            if (wifiP2pGroup.getOwner().deviceAddress == null) {
                Log.e(TAG, "Group owner has no mac address.");
                return false;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(wifiP2pGroup.getOwner().deviceAddress);
                if (str == null) {
                    Log.e(TAG, "Cannot parse peer mac address.");
                    return false;
                }
                try {
                    byte[] bArrMacAddressToByteArray2 = NativeUtil.macAddressToByteArray(str);
                    SupplicantResult supplicantResult = new SupplicantResult("invite(" + wifiP2pGroup.getInterface() + ", " + wifiP2pGroup.getOwner().deviceAddress + ", " + str + ")");
                    try {
                        supplicantResult.setResult(this.mISupplicantP2pIface.invite(wifiP2pGroup.getInterface(), bArrMacAddressToByteArray, bArrMacAddressToByteArray2));
                    } catch (RemoteException e) {
                        Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                        supplicantServiceDiedHandler();
                    }
                    return supplicantResult.isSuccess();
                } catch (Exception e2) {
                    Log.e(TAG, "Peer mac address parse error.", e2);
                    return false;
                }
            } catch (Exception e3) {
                Log.e(TAG, "Group owner mac address parse error.", e3);
                return false;
            }
        }
    }

    public boolean reject(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("reject")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "Cannot parse rejected peer's mac address.");
                return false;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str);
                SupplicantResult supplicantResult = new SupplicantResult("reject(" + str + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.reject(bArrMacAddressToByteArray));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse peer mac address.", e2);
                return false;
            }
        }
    }

    public String getDeviceAddress() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getDeviceAddress")) {
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getDeviceAddress()");
            try {
                this.mISupplicantP2pIface.getDeviceAddress(new ISupplicantP2pIface.getDeviceAddressCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                        SupplicantP2pIfaceHal.lambda$getDeviceAddress$7(supplicantResult, supplicantStatus, bArr);
                    }
                });
                return (String) supplicantResult.getResult();
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
                return null;
            }
        }
    }

    static void lambda$getDeviceAddress$7(SupplicantResult supplicantResult, SupplicantStatus supplicantStatus, byte[] bArr) {
        String strMacAddressFromByteArray;
        try {
            strMacAddressFromByteArray = NativeUtil.macAddressFromByteArray(bArr);
        } catch (Exception e) {
            Log.e(TAG, "Could not process reported address.", e);
            strMacAddressFromByteArray = null;
        }
        supplicantResult.setResult(supplicantStatus, strMacAddressFromByteArray);
    }

    public String getSsid(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getSsid")) {
                return null;
            }
            if (str == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return null;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str);
                final SupplicantResult supplicantResult = new SupplicantResult("getSsid(" + str + ")");
                try {
                    this.mISupplicantP2pIface.getSsid(bArrMacAddressToByteArray, new ISupplicantP2pIface.getSsidCallback() {
                        @Override
                        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                            SupplicantP2pIfaceHal.lambda$getSsid$8(supplicantResult, supplicantStatus, arrayList);
                        }
                    });
                    return (String) supplicantResult.getResult();
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                    return null;
                }
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse mac address.", e2);
                return null;
            }
        }
    }

    static void lambda$getSsid$8(SupplicantResult supplicantResult, SupplicantStatus supplicantStatus, ArrayList arrayList) {
        String strRemoveEnclosingQuotes;
        if (arrayList != null) {
            try {
                strRemoveEnclosingQuotes = NativeUtil.removeEnclosingQuotes(NativeUtil.encodeSsid(arrayList));
            } catch (Exception e) {
                Log.e(TAG, "Could not encode SSID.", e);
                strRemoveEnclosingQuotes = null;
            }
        } else {
            strRemoveEnclosingQuotes = null;
        }
        supplicantResult.setResult(supplicantStatus, strRemoveEnclosingQuotes);
    }

    public boolean reinvoke(int i, String str) {
        if (TextUtils.isEmpty(str) || i < 0) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("reinvoke")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return false;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str);
                SupplicantResult supplicantResult = new SupplicantResult("reinvoke(" + i + ", " + str + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.reinvoke(i, bArrMacAddressToByteArray));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse mac address.", e2);
                return false;
            }
        }
    }

    public boolean groupAdd(int i, boolean z) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("groupAdd")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("groupAdd(" + i + ", " + z + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.addGroup(z, i));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean groupAdd(boolean z) {
        return groupAdd(-1, z);
    }

    public boolean groupRemove(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("groupRemove")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("groupRemove(" + str + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.removeGroup(str));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public int getGroupCapability(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getGroupCapability")) {
                return -1;
            }
            if (str == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return -1;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str);
                final SupplicantResult supplicantResult = new SupplicantResult("getGroupCapability(" + str + ")");
                try {
                    this.mISupplicantP2pIface.getGroupCapability(bArrMacAddressToByteArray, new ISupplicantP2pIface.getGroupCapabilityCallback() {
                        @Override
                        public final void onValues(SupplicantStatus supplicantStatus, int i) {
                            supplicantResult.setResult(supplicantStatus, Integer.valueOf(i));
                        }
                    });
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                if (!supplicantResult.isSuccess()) {
                    return -1;
                }
                return ((Integer) supplicantResult.getResult()).intValue();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse group address.", e2);
                return -1;
            }
        }
    }

    public boolean configureExtListen(boolean z, int i, int i2) {
        if (z && i2 < i) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("configureExtListen")) {
                return false;
            }
            if (!z) {
                i = 0;
                i2 = 0;
            }
            if (i >= 0 && i2 >= 0) {
                SupplicantResult supplicantResult = new SupplicantResult("configureExtListen(" + i + ", " + i2 + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.configureExtListen(i, i2));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            }
            Log.e(TAG, "Invalid parameters supplied to configureExtListen: " + i + ", " + i2);
            return false;
        }
    }

    public boolean setListenChannel(int i, int i2) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setListenChannel")) {
                return false;
            }
            if (i >= 1 && i <= 11) {
                SupplicantResult supplicantResult = new SupplicantResult("setListenChannel(" + i + ", 81)");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.setListenChannel(i, 81));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                if (!supplicantResult.isSuccess()) {
                    return false;
                }
            } else if (i != 0) {
                return false;
            }
            if (i2 < 0 || i2 > 165) {
                return false;
            }
            ArrayList<ISupplicantP2pIface.FreqRange> arrayList = new ArrayList<>();
            if (i2 >= 1 && i2 <= 165) {
                int i3 = (i2 <= 14 ? 2407 : ScoringParams.BAND5) + (i2 * 5);
                ISupplicantP2pIface.FreqRange freqRange = new ISupplicantP2pIface.FreqRange();
                freqRange.min = 1000;
                freqRange.max = i3 - 5;
                ISupplicantP2pIface.FreqRange freqRange2 = new ISupplicantP2pIface.FreqRange();
                freqRange2.min = i3 + 5;
                freqRange2.max = 6000;
                arrayList.add(freqRange);
                arrayList.add(freqRange2);
            }
            SupplicantResult supplicantResult2 = new SupplicantResult("setDisallowedFrequencies(" + arrayList + ")");
            try {
                supplicantResult2.setResult(this.mISupplicantP2pIface.setDisallowedFrequencies(arrayList));
            } catch (RemoteException e2) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e2);
                supplicantServiceDiedHandler();
            }
            return supplicantResult2.isSuccess();
        }
    }

    public boolean serviceAdd(WifiP2pServiceInfo wifiP2pServiceInfo) {
        SupplicantResult supplicantResult;
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("serviceAdd")) {
                return false;
            }
            if (wifiP2pServiceInfo == null) {
                Log.e(TAG, "Null service info passed.");
                return false;
            }
            for (String str : wifiP2pServiceInfo.getSupplicantQueryList()) {
                if (str == null) {
                    Log.e(TAG, "Invalid service description (null).");
                    return false;
                }
                String[] strArrSplit = str.split(" ");
                if (strArrSplit.length < 3) {
                    Log.e(TAG, "Service specification invalid: " + str);
                    return false;
                }
                try {
                    if ("upnp".equals(strArrSplit[0])) {
                        try {
                            int i = Integer.parseInt(strArrSplit[1], 16);
                            supplicantResult = new SupplicantResult("addUpnpService(" + strArrSplit[1] + ", " + strArrSplit[2] + ")");
                            try {
                                supplicantResult.setResult(this.mISupplicantP2pIface.addUpnpService(i, strArrSplit[2]));
                            } catch (RemoteException e) {
                                e = e;
                                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                                supplicantServiceDiedHandler();
                            }
                        } catch (NumberFormatException e2) {
                            Log.e(TAG, "UPnP Service specification invalid: " + str, e2);
                            return false;
                        }
                    } else {
                        if (!"bonjour".equals(strArrSplit[0])) {
                            return false;
                        }
                        if (strArrSplit[1] != null && strArrSplit[2] != null) {
                            try {
                                ArrayList<Byte> arrayListByteArrayToArrayList = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(strArrSplit[1]));
                                ArrayList<Byte> arrayListByteArrayToArrayList2 = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(strArrSplit[2]));
                                SupplicantResult supplicantResult2 = new SupplicantResult("addBonjourService(" + strArrSplit[1] + ", " + strArrSplit[2] + ")");
                                try {
                                    supplicantResult2.setResult(this.mISupplicantP2pIface.addBonjourService(arrayListByteArrayToArrayList, arrayListByteArrayToArrayList2));
                                    supplicantResult = supplicantResult2;
                                } catch (RemoteException e3) {
                                    e = e3;
                                    supplicantResult = supplicantResult2;
                                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                                    supplicantServiceDiedHandler();
                                }
                            } catch (Exception e4) {
                                Log.e(TAG, "Invalid bonjour service description.");
                                return false;
                            }
                        } else {
                            supplicantResult = null;
                        }
                    }
                } catch (RemoteException e5) {
                    e = e5;
                    supplicantResult = null;
                }
                if (supplicantResult != null && supplicantResult.isSuccess()) {
                }
                return false;
            }
            return true;
        }
    }

    public boolean serviceRemove(WifiP2pServiceInfo wifiP2pServiceInfo) {
        SupplicantResult supplicantResult;
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("serviceRemove")) {
                return false;
            }
            if (wifiP2pServiceInfo == null) {
                Log.e(TAG, "Null service info passed.");
                return false;
            }
            for (String str : wifiP2pServiceInfo.getSupplicantQueryList()) {
                if (str == null) {
                    Log.e(TAG, "Invalid service description (null).");
                    return false;
                }
                String[] strArrSplit = str.split(" ");
                if (strArrSplit.length < 3) {
                    Log.e(TAG, "Service specification invalid: " + str);
                    return false;
                }
                try {
                    if ("upnp".equals(strArrSplit[0])) {
                        try {
                            int i = Integer.parseInt(strArrSplit[1], 16);
                            supplicantResult = new SupplicantResult("removeUpnpService(" + strArrSplit[1] + ", " + strArrSplit[2] + ")");
                            try {
                                supplicantResult.setResult(this.mISupplicantP2pIface.removeUpnpService(i, strArrSplit[2]));
                            } catch (RemoteException e) {
                                e = e;
                                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                                supplicantServiceDiedHandler();
                            }
                        } catch (NumberFormatException e2) {
                            Log.e(TAG, "UPnP Service specification invalid: " + str, e2);
                            return false;
                        }
                    } else if ("bonjour".equals(strArrSplit[0])) {
                        if (strArrSplit[1] != null) {
                            try {
                                ArrayList<Byte> arrayListByteArrayToArrayList = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(strArrSplit[1]));
                                SupplicantResult supplicantResult2 = new SupplicantResult("removeBonjourService(" + strArrSplit[1] + ")");
                                try {
                                    supplicantResult2.setResult(this.mISupplicantP2pIface.removeBonjourService(arrayListByteArrayToArrayList));
                                    supplicantResult = supplicantResult2;
                                } catch (RemoteException e3) {
                                    e = e3;
                                    supplicantResult = supplicantResult2;
                                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                                    supplicantServiceDiedHandler();
                                }
                            } catch (Exception e4) {
                                Log.e(TAG, "Invalid bonjour service description.");
                                return false;
                            }
                        } else {
                            supplicantResult = null;
                        }
                    } else {
                        Log.e(TAG, "Unknown / unsupported P2P service requested: " + strArrSplit[0]);
                        return false;
                    }
                } catch (RemoteException e5) {
                    e = e5;
                    supplicantResult = null;
                }
                if (supplicantResult != null && supplicantResult.isSuccess()) {
                }
                return false;
            }
            return true;
        }
    }

    public String requestServiceDiscovery(String str, String str2) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("requestServiceDiscovery")) {
                return null;
            }
            if (str == null) {
                Log.e(TAG, "Cannot parse peer mac address.");
                return null;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str);
                if (str2 == null) {
                    Log.e(TAG, "Cannot parse service discovery query: " + str2);
                    return null;
                }
                try {
                    ArrayList<Byte> arrayListByteArrayToArrayList = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(str2));
                    final SupplicantResult supplicantResult = new SupplicantResult("requestServiceDiscovery(" + str + ", " + str2 + ")");
                    try {
                        this.mISupplicantP2pIface.requestServiceDiscovery(bArrMacAddressToByteArray, arrayListByteArrayToArrayList, new ISupplicantP2pIface.requestServiceDiscoveryCallback() {
                            @Override
                            public final void onValues(SupplicantStatus supplicantStatus, long j) {
                                supplicantResult.setResult(supplicantStatus, new Long(j));
                            }
                        });
                    } catch (RemoteException e) {
                        Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                        supplicantServiceDiedHandler();
                    }
                    Long l = (Long) supplicantResult.getResult();
                    if (l == null) {
                        return null;
                    }
                    return l.toString();
                } catch (Exception e2) {
                    Log.e(TAG, "Could not parse service query.", e2);
                    return null;
                }
            } catch (Exception e3) {
                Log.e(TAG, "Could not process peer MAC address.", e3);
                return null;
            }
        }
    }

    public boolean cancelServiceDiscovery(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("cancelServiceDiscovery")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "cancelServiceDiscovery requires a valid tag.");
                return false;
            }
            try {
                long j = Long.parseLong(str);
                SupplicantResult supplicantResult = new SupplicantResult("cancelServiceDiscovery(" + str + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.cancelServiceDiscovery(j));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            } catch (NumberFormatException e2) {
                Log.e(TAG, "Service discovery identifier invalid: " + str, e2);
                return false;
            }
        }
    }

    public boolean setMiracastMode(int i) {
        synchronized (this.mLock) {
            byte b = 0;
            if (!checkSupplicantP2pIfaceAndLogFailure("setMiracastMode")) {
                return false;
            }
            switch (i) {
                case 1:
                    b = 1;
                    break;
                case 2:
                    b = 2;
                    break;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setMiracastMode(" + i + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.setMiracastMode(b));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean startWpsPbc(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "Group name required when requesting WPS PBC. Got (" + str + ")");
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("startWpsPbc")) {
                return false;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str2);
                SupplicantResult supplicantResult = new SupplicantResult("startWpsPbc(" + str + ", " + str2 + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.startWpsPbc(str, bArrMacAddressToByteArray));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse BSSID.", e2);
                return false;
            }
        }
    }

    public boolean startWpsPinKeypad(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("startWpsPinKeypad")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "Group name required when requesting WPS KEYPAD.");
                return false;
            }
            if (str2 == null) {
                Log.e(TAG, "PIN required when requesting WPS KEYPAD.");
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("startWpsPinKeypad(" + str + ", " + str2 + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.startWpsPinKeypad(str, str2));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public String startWpsPinDisplay(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("startWpsPinDisplay")) {
                return null;
            }
            if (str == null) {
                Log.e(TAG, "Group name required when requesting WPS KEYPAD.");
                return null;
            }
            try {
                byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(str2);
                final SupplicantResult supplicantResult = new SupplicantResult("startWpsPinDisplay(" + str + ", " + str2 + ")");
                try {
                    this.mISupplicantP2pIface.startWpsPinDisplay(str, bArrMacAddressToByteArray, new ISupplicantP2pIface.startWpsPinDisplayCallback() {
                        @Override
                        public final void onValues(SupplicantStatus supplicantStatus, String str3) {
                            supplicantResult.setResult(supplicantStatus, str3);
                        }
                    });
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return (String) supplicantResult.getResult();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse BSSID.", e2);
                return null;
            }
        }
    }

    public boolean cancelWps(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("cancelWps")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "Group name required when requesting WPS KEYPAD.");
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("cancelWps(" + str + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.cancelWps(str));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean enableWfd(boolean z) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("enableWfd")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("enableWfd(" + z + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.enableWfd(z));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean setWfdDeviceInfo(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setWfdDeviceInfo")) {
                return false;
            }
            if (str == null) {
                Log.e(TAG, "Cannot parse null WFD info string.");
                return false;
            }
            try {
                byte[] bArrHexStringToByteArray = NativeUtil.hexStringToByteArray(str);
                SupplicantResult supplicantResult = new SupplicantResult("setWfdDeviceInfo(" + str + ")");
                try {
                    supplicantResult.setResult(this.mISupplicantP2pIface.setWfdDeviceInfo(bArrHexStringToByteArray));
                } catch (RemoteException e) {
                    Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                    supplicantServiceDiedHandler();
                }
                return supplicantResult.isSuccess();
            } catch (Exception e2) {
                Log.e(TAG, "Could not parse WFD Device Info string.");
                return false;
            }
        }
    }

    public boolean removeNetwork(int i) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("removeNetwork")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("removeNetwork(" + i + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.removeNetwork(i));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    private List<Integer> listNetworks() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("listNetworks")) {
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("listNetworks()");
            try {
                this.mISupplicantP2pIface.listNetworks(new ISupplicantIface.listNetworksCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        supplicantResult.setResult(supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return (List) supplicantResult.getResult();
        }
    }

    private ISupplicantP2pNetwork getNetwork(int i) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getNetwork")) {
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getNetwork(" + i + ")");
            try {
                this.mISupplicantP2pIface.getNetwork(i, new ISupplicantIface.getNetworkCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
                        supplicantResult.setResult(supplicantStatus, iSupplicantNetwork);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            if (supplicantResult.getResult() == null) {
                Log.e(TAG, "getNetwork got null network");
                return null;
            }
            return getP2pNetworkMockable((ISupplicantNetwork) supplicantResult.getResult());
        }
    }

    public boolean loadGroups(WifiP2pGroupList wifiP2pGroupList) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("loadGroups")) {
                return false;
            }
            List<Integer> listListNetworks = listNetworks();
            if (listListNetworks != null && !listListNetworks.isEmpty()) {
                for (Integer num : listListNetworks) {
                    ISupplicantP2pNetwork network = getNetwork(num.intValue());
                    if (network == null) {
                        Log.e(TAG, "Failed to retrieve network object for " + num);
                    } else {
                        final SupplicantResult supplicantResult = new SupplicantResult("isCurrent(" + num + ")");
                        try {
                            network.isCurrent(new ISupplicantP2pNetwork.isCurrentCallback() {
                                @Override
                                public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                    supplicantResult.setResult(supplicantStatus, Boolean.valueOf(z));
                                }
                            });
                        } catch (RemoteException e) {
                            Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                            supplicantServiceDiedHandler();
                        }
                        if (!supplicantResult.isSuccess() || ((Boolean) supplicantResult.getResult()).booleanValue()) {
                            Log.i(TAG, "Skipping current network");
                        } else {
                            final SupplicantResult supplicantResult2 = new SupplicantResult("isPersistent(" + num + ")");
                            try {
                                network.isPersistent(new ISupplicantP2pNetwork.isPersistentCallback() {
                                    @Override
                                    public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                        supplicantResult2.setResult(supplicantStatus, Boolean.valueOf(z));
                                    }
                                });
                            } catch (RemoteException e2) {
                                Log.e(TAG, "ISupplicantP2pIface exception: " + e2);
                                supplicantServiceDiedHandler();
                            }
                            if (!supplicantResult2.isSuccess() || !((Boolean) supplicantResult2.getResult()).booleanValue()) {
                                logd("clean up the unused persistent group. netId=" + num);
                                removeNetwork(num.intValue());
                            } else {
                                WifiP2pGroup wifiP2pGroup = new WifiP2pGroup();
                                wifiP2pGroup.setNetworkId(num.intValue());
                                final SupplicantResult supplicantResult3 = new SupplicantResult("getSsid(" + num + ")");
                                try {
                                    network.getSsid(new ISupplicantP2pNetwork.getSsidCallback() {
                                        @Override
                                        public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                                            supplicantResult3.setResult(supplicantStatus, arrayList);
                                        }
                                    });
                                } catch (RemoteException e3) {
                                    Log.e(TAG, "ISupplicantP2pIface exception: " + e3);
                                    supplicantServiceDiedHandler();
                                }
                                if (supplicantResult3.isSuccess() && supplicantResult3.getResult() != null && !((ArrayList) supplicantResult3.getResult()).isEmpty()) {
                                    wifiP2pGroup.setNetworkName(NativeUtil.removeEnclosingQuotes(NativeUtil.encodeSsid((ArrayList) supplicantResult3.getResult())));
                                }
                                final SupplicantResult supplicantResult4 = new SupplicantResult("getBssid(" + num + ")");
                                try {
                                    network.getBssid(new ISupplicantP2pNetwork.getBssidCallback() {
                                        @Override
                                        public final void onValues(SupplicantStatus supplicantStatus, byte[] bArr) {
                                            supplicantResult4.setResult(supplicantStatus, bArr);
                                        }
                                    });
                                } catch (RemoteException e4) {
                                    Log.e(TAG, "ISupplicantP2pIface exception: " + e4);
                                    supplicantServiceDiedHandler();
                                }
                                if (supplicantResult4.isSuccess() && !ArrayUtils.isEmpty((byte[]) supplicantResult4.getResult())) {
                                    WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
                                    wifiP2pDevice.deviceAddress = NativeUtil.macAddressFromByteArray((byte[]) supplicantResult4.getResult());
                                    wifiP2pGroup.setOwner(wifiP2pDevice);
                                }
                                final SupplicantResult supplicantResult5 = new SupplicantResult("isGo(" + num + ")");
                                try {
                                    network.isGo(new ISupplicantP2pNetwork.isGoCallback() {
                                        @Override
                                        public final void onValues(SupplicantStatus supplicantStatus, boolean z) {
                                            supplicantResult5.setResult(supplicantStatus, Boolean.valueOf(z));
                                        }
                                    });
                                } catch (RemoteException e5) {
                                    Log.e(TAG, "ISupplicantP2pIface exception: " + e5);
                                    supplicantServiceDiedHandler();
                                }
                                if (supplicantResult5.isSuccess()) {
                                    wifiP2pGroup.setIsGroupOwner(((Boolean) supplicantResult5.getResult()).booleanValue());
                                }
                                wifiP2pGroupList.add(wifiP2pGroup);
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

    public boolean setWpsDeviceName(String str) {
        if (str == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setWpsDeviceName")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setWpsDeviceName(" + str + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.setWpsDeviceName(str));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean setWpsDeviceType(String str) {
        try {
            Matcher matcher = WPS_DEVICE_TYPE_PATTERN.matcher(str);
            if (matcher.find() && matcher.groupCount() == 3) {
                short s = Short.parseShort(matcher.group(1));
                byte[] bArrHexStringToByteArray = NativeUtil.hexStringToByteArray(matcher.group(2));
                short s2 = Short.parseShort(matcher.group(3));
                byte[] bArr = new byte[8];
                ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.BIG_ENDIAN);
                byteBufferOrder.putShort(s);
                byteBufferOrder.put(bArrHexStringToByteArray);
                byteBufferOrder.putShort(s2);
                synchronized (this.mLock) {
                    if (!checkSupplicantP2pIfaceAndLogFailure("setWpsDeviceType")) {
                        return false;
                    }
                    SupplicantResult supplicantResult = new SupplicantResult("setWpsDeviceType(" + str + ")");
                    try {
                        supplicantResult.setResult(this.mISupplicantP2pIface.setWpsDeviceType(bArr));
                    } catch (RemoteException e) {
                        Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                        supplicantServiceDiedHandler();
                    }
                    return supplicantResult.isSuccess();
                }
            }
            Log.e(TAG, "Malformed WPS device type " + str);
            return false;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Illegal argument " + str, e2);
            return false;
        }
    }

    public boolean setWpsConfigMethods(String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setWpsConfigMethods")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setWpsConfigMethods(" + str + ")");
            short sStringToWpsConfigMethod = (short) 0;
            for (String str2 : str.split("\\s+")) {
                sStringToWpsConfigMethod = (short) (sStringToWpsConfigMethod | stringToWpsConfigMethod(str2));
            }
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.setWpsConfigMethods(sStringToWpsConfigMethod));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
        }
    }

    public String getNfcHandoverRequest() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getNfcHandoverRequest")) {
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getNfcHandoverRequest()");
            try {
                this.mISupplicantP2pIface.createNfcHandoverRequestMessage(new ISupplicantP2pIface.createNfcHandoverRequestMessageCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        supplicantResult.setResult(supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            if (!supplicantResult.isSuccess()) {
                return null;
            }
            return NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList((ArrayList) supplicantResult.getResult()));
        }
    }

    public String getNfcHandoverSelect() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getNfcHandoverSelect")) {
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getNfcHandoverSelect()");
            try {
                this.mISupplicantP2pIface.createNfcHandoverSelectMessage(new ISupplicantP2pIface.createNfcHandoverSelectMessageCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        supplicantResult.setResult(supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            if (!supplicantResult.isSuccess()) {
                return null;
            }
            return NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList((ArrayList) supplicantResult.getResult()));
        }
    }

    public boolean initiatorReportNfcHandover(String str) {
        if (str == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("initiatorReportNfcHandover")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("initiatorReportNfcHandover(" + str + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.reportNfcHandoverInitiation(NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(str))));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Illegal argument " + str, e2);
                return false;
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean responderReportNfcHandover(String str) {
        if (str == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("responderReportNfcHandover")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("responderReportNfcHandover(" + str + ")");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.reportNfcHandoverResponse(NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(str))));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Illegal argument " + str, e2);
                return false;
            }
            return supplicantResult.isSuccess();
        }
    }

    public boolean setClientList(int i, String str) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("setClientList")) {
                return false;
            }
            if (TextUtils.isEmpty(str)) {
                Log.e(TAG, "Invalid client list");
                return false;
            }
            ISupplicantP2pNetwork network = getNetwork(i);
            if (network == null) {
                Log.e(TAG, "Invalid network id ");
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("setClientList(" + i + ", " + str + ")");
            try {
                ArrayList<byte[]> arrayList = new ArrayList<>();
                Iterator it = Arrays.asList(str.split("\\s+")).iterator();
                while (it.hasNext()) {
                    arrayList.add(NativeUtil.macAddressToByteArray((String) it.next()));
                }
                supplicantResult.setResult(network.setClientList(arrayList));
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Illegal argument " + str, e2);
                return false;
            }
            return supplicantResult.isSuccess();
        }
    }

    public String getClientList(int i) {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("getClientList")) {
                return null;
            }
            ISupplicantP2pNetwork network = getNetwork(i);
            if (network == null) {
                Log.e(TAG, "Invalid network id ");
                return null;
            }
            final SupplicantResult supplicantResult = new SupplicantResult("getClientList(" + i + ")");
            try {
                network.getClientList(new ISupplicantP2pNetwork.getClientListCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList) {
                        supplicantResult.setResult(supplicantStatus, arrayList);
                    }
                });
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            if (!supplicantResult.isSuccess()) {
                return null;
            }
            return (String) ((ArrayList) supplicantResult.getResult()).stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return NativeUtil.macAddressFromByteArray((byte[]) obj);
                }
            }).collect(Collectors.joining(" "));
        }
    }

    public boolean saveConfig() {
        synchronized (this.mLock) {
            if (!checkSupplicantP2pIfaceAndLogFailure("saveConfig")) {
                return false;
            }
            SupplicantResult supplicantResult = new SupplicantResult("saveConfig()");
            try {
                supplicantResult.setResult(this.mISupplicantP2pIface.saveConfig());
            } catch (RemoteException e) {
                Log.e(TAG, "ISupplicantP2pIface exception: " + e);
                supplicantServiceDiedHandler();
            }
            return supplicantResult.isSuccess();
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

    private static class SupplicantResult<E> {
        private String mMethodName;
        private SupplicantStatus mStatus = null;
        private E mValue = null;

        SupplicantResult(String str) {
            this.mMethodName = str;
            SupplicantP2pIfaceHal.logd("entering " + this.mMethodName);
        }

        public void setResult(SupplicantStatus supplicantStatus, E e) {
            SupplicantP2pIfaceHal.logCompletion(this.mMethodName, supplicantStatus);
            SupplicantP2pIfaceHal.logd("leaving " + this.mMethodName + " with result = " + e);
            this.mStatus = supplicantStatus;
            this.mValue = e;
        }

        public void setResult(SupplicantStatus supplicantStatus) {
            SupplicantP2pIfaceHal.logCompletion(this.mMethodName, supplicantStatus);
            SupplicantP2pIfaceHal.logd("leaving " + this.mMethodName);
            this.mStatus = supplicantStatus;
        }

        public boolean isSuccess() {
            return this.mStatus != null && (this.mStatus.code == 0 || this.mStatus.code == 5);
        }

        public E getResult() {
            if (isSuccess()) {
                return this.mValue;
            }
            return null;
        }
    }
}
