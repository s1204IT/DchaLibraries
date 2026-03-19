package com.mediatek.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.wifi.supplicant.V1_0.ISupplicant;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.HidlSupport;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.security.KeyStore;
import android.util.Log;
import android.util.MutableBoolean;
import com.android.server.wifi.SupplicantStaIfaceHal;
import com.android.server.wifi.SupplicantStaNetworkHal;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicantIface;
import vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicantNetwork;
import vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicantStaIface;
import vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicantStaNetwork;

public class MtkWapi {
    public static final int EID_WAPI = 68;
    private static final String TAG = "MtkWapi";
    private static final int WAPI_AUTH_KEY_MGMT_PSK = 41030656;
    private static final int WAPI_AUTH_KEY_MGMT_WAI = 24253440;
    private static final int WAPI_VERSION = 1;
    private static ISupplicantStaIface mMtkiface;
    public static String[] mWapiCertSelCache;
    private static MtkWapi sMtkWapi = null;
    public static boolean mIsSystemSupportWapi = false;
    public static boolean mIsCheckedSupport = false;

    public static String parseWapiElement(ScanResult.InformationElement informationElement) {
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(informationElement.bytes).order(ByteOrder.LITTLE_ENDIAN);
        Log.d("InformationElementUtil.WAPI", "parseWapiElement start");
        try {
            if (byteBufferOrder.getShort() != 1) {
                Log.e("InformationElementUtil.WAPI", "incorrect WAPI version");
                return null;
            }
            short s = byteBufferOrder.getShort();
            if (s != 1) {
                Log.e("InformationElementUtil.WAPI", "WAPI IE invalid AKM count: " + ((int) s));
            }
            String str = "[WAPI";
            int i = byteBufferOrder.getInt();
            if (i == WAPI_AUTH_KEY_MGMT_WAI) {
                str = "[WAPI-CERT";
            } else if (i == WAPI_AUTH_KEY_MGMT_PSK) {
                str = "[WAPI-PSK";
            }
            return str + "]";
        } catch (BufferUnderflowException e) {
            Log.e("IE_Capabilities", "Couldn't parse WAPI element, buffer underflow");
            return null;
        }
    }

    public static boolean updateWapiCertSelList(WifiConfiguration wifiConfiguration) {
        String[] list = KeyStore.getInstance().list("WAPI_CACERT_", 1010);
        Arrays.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            sb.append(str);
            sb.append(NAIRealmData.NAI_REALM_STRING_SEPARATOR);
        }
        if (isWapiCertSelListChanged(list) && !setWapiCertAliasList(sb.toString())) {
            Log.e(TAG, "failed to set alias list: " + sb.toString());
            return false;
        }
        return true;
    }

    public static boolean isWapiCertSelListChanged(String[] strArr) {
        if (mWapiCertSelCache == null || !Arrays.equals(mWapiCertSelCache, strArr)) {
            mWapiCertSelCache = strArr;
            return true;
        }
        return false;
    }

    public static boolean hasWapiConfigChanged(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2) {
        if (!mIsSystemSupportWapi || !isConfigForWapiNetwork(wifiConfiguration) || !isConfigForWapiNetwork(wifiConfiguration2)) {
            return false;
        }
        if (wifiConfiguration.getAuthType() == 8 && wifiConfiguration2.getAuthType() == 8) {
            return false;
        }
        return wifiConfiguration.wapiCertSel == null ? wifiConfiguration2.wapiCertSel != null : !wifiConfiguration.wapiCertSel.equals(wifiConfiguration2.wapiCertSel);
    }

    public static MtkWapi getInstance() {
        if (sMtkWapi == null) {
            synchronized (TAG) {
                sMtkWapi = new MtkWapi();
            }
        }
        return sMtkWapi;
    }

    public static boolean isConfigForWapiNetwork(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null) {
            return false;
        }
        if (!isWapiPskConfiguration(wifiConfiguration) && !isWapiCertConfiguration(wifiConfiguration)) {
            return false;
        }
        return true;
    }

    public static boolean isWapiPskConfiguration(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null || !wifiConfiguration.allowedKeyManagement.get(8)) {
            return false;
        }
        return true;
    }

    public static boolean isWapiCertConfiguration(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null || !wifiConfiguration.allowedKeyManagement.get(9)) {
            return false;
        }
        return true;
    }

    public static String generateCapabilitiesString(ScanResult.InformationElement[] informationElementArr, BitSet bitSet, String str) {
        if (informationElementArr == null || bitSet == null) {
            return str;
        }
        if (!mIsCheckedSupport && checkSupportWapi()) {
            init();
        }
        if (!mIsSystemSupportWapi) {
            return str;
        }
        String wapiElement = null;
        for (ScanResult.InformationElement informationElement : informationElementArr) {
            if (informationElement.id == 68) {
                wapiElement = parseWapiElement(informationElement);
            }
        }
        if (wapiElement != null) {
            str = str + wapiElement;
            if (str.contains("[WEP]")) {
                return str.replace("[WEP]", "");
            }
        }
        return str;
    }

    public static boolean isScanResultForWapiNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI");
    }

    public static void setupMtkIface(String str) {
        ISupplicantIface mtkIfaceV2_0 = getMtkIfaceV2_0(str);
        if (mtkIfaceV2_0 == null) {
            Log.e(TAG, "setupMtkIface got null iface");
            return;
        }
        Log.i(TAG, "mtkIfaceHwBinder get successfully");
        mMtkiface = getMtkStaIfaceMockableV2_0(mtkIfaceV2_0);
        if (mMtkiface == null) {
            Log.e(TAG, "Mtk sta iface null");
        }
    }

    protected static ISupplicantStaIface getMtkStaIfaceMockableV2_0(ISupplicantIface iSupplicantIface) {
        ISupplicantStaIface iSupplicantStaIfaceAsInterface;
        synchronized (getLockForSupplicantStaIfaceHal()) {
            iSupplicantStaIfaceAsInterface = ISupplicantStaIface.asInterface(iSupplicantIface.asBinder());
        }
        return iSupplicantStaIfaceAsInterface;
    }

    private static ISupplicantIface getMtkIfaceV2_0(String str) {
        synchronized (getLockForSupplicantStaIfaceHal()) {
            final ArrayList arrayList = new ArrayList();
            try {
                getISupplicant().listInterfaces(new ISupplicant.listInterfacesCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, ArrayList arrayList2) {
                        MtkWapi.lambda$getMtkIfaceV2_0$0(arrayList, supplicantStatus, arrayList2);
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
                            supplicantServiceDiedHandler(str);
                            return null;
                        }
                    }
                }
                return (ISupplicantIface) mutable.value;
            } catch (RemoteException e2) {
                Log.e(TAG, "ISupplicant.listInterfaces exception: " + e2);
                supplicantServiceDiedHandler(str);
                return null;
            }
        }
    }

    static void lambda$getMtkIfaceV2_0$0(ArrayList arrayList, SupplicantStatus supplicantStatus, ArrayList arrayList2) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "Getting Supplicant Interfaces failed: " + supplicantStatus.code);
            return;
        }
        arrayList.addAll(arrayList2);
    }

    static void lambda$getMtkIfaceV2_0$1(HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ISupplicantIface iSupplicantIface) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "Failed to get ISupplicantIface " + supplicantStatus.code);
            return;
        }
        mutable.value = iSupplicantIface;
    }

    protected static vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicant getMtkSupplicantMockableV2_0() throws RemoteException {
        vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicant iSupplicantCastFrom;
        synchronized (getLockForSupplicantStaIfaceHal()) {
            try {
                try {
                    iSupplicantCastFrom = vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicant.castFrom((IHwInterface) vendor.mediatek.hardware.wifi.supplicant.V2_0.ISupplicant.getService());
                } catch (NoSuchElementException e) {
                    Log.e(TAG, "Failed to get IMtkSupplicant", e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return iSupplicantCastFrom;
    }

    private static boolean checkSupportWapi() {
        final SupplicantStaIfaceHal supplicantStaIfaceHal = WifiInjector.getInstance().getSupplicantStaIfaceHal();
        synchronized (getLock(supplicantStaIfaceHal)) {
            if (!checkMtkIfaceAndLogFailure("getMtkFeatureMask")) {
                return false;
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                mMtkiface.getFeatureMask(new ISupplicantStaIface.getFeatureMaskCallback() {
                    @Override
                    public final void onValues(SupplicantStatus supplicantStatus, int i) {
                        MtkWapi.lambda$checkSupportWapi$2(mutableBoolean, supplicantStaIfaceHal, supplicantStatus, i);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(supplicantStaIfaceHal, e, "getMtkFeatureMask");
                return false;
            }
        }
    }

    static void lambda$checkSupportWapi$2(MutableBoolean mutableBoolean, SupplicantStaIfaceHal supplicantStaIfaceHal, SupplicantStatus supplicantStatus, int i) {
        mutableBoolean.value = supplicantStatus.code == 0;
        if (mutableBoolean.value) {
            mIsSystemSupportWapi = (i & 1) == 1;
            mIsCheckedSupport = true;
        }
        checkStatusAndLogFailure(supplicantStaIfaceHal, supplicantStatus, "getMtkFeatureMask");
    }

    public static boolean setWapiCertAliasList(String str) {
        SupplicantStaIfaceHal supplicantStaIfaceHal = WifiInjector.getInstance().getSupplicantStaIfaceHal();
        synchronized (getLock(supplicantStaIfaceHal)) {
            if (!checkMtkIfaceAndLogFailure("setWapiCertAliasList")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(supplicantStaIfaceHal, mMtkiface.setWapiCertAliasList(str), "setWapiCertAliasList");
            } catch (RemoteException e) {
                handleRemoteException(supplicantStaIfaceHal, e, "setWapiCertAliasList");
                return false;
            }
        }
    }

    public static boolean setWapiCertAlias(SupplicantStaNetworkHal supplicantStaNetworkHal, int i, String str) {
        boolean zCheckStatusAndLogFailure;
        Log.d(TAG, "supplicantNetworkId= " + i);
        if (supplicantStaNetworkHal == null) {
            return false;
        }
        synchronized (getLock(supplicantStaNetworkHal)) {
            try {
                try {
                    ISupplicantStaNetwork mtkStaNetwork = getMtkStaNetwork(i);
                    if (str == null) {
                        str = "NULL";
                    }
                    zCheckStatusAndLogFailure = checkStatusAndLogFailure(supplicantStaNetworkHal, mtkStaNetwork.setWapiCertAlias(str), "setWapiCertAlias");
                } catch (RemoteException e) {
                    handleRemoteException(supplicantStaNetworkHal, e, "setWapiCertAlias");
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return zCheckStatusAndLogFailure;
    }

    private static ISupplicantStaNetwork getMtkStaNetwork(int i) {
        final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
        try {
            mMtkiface.getNetwork(i, new ISupplicantIface.getNetworkCallback() {
                @Override
                public final void onValues(SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
                    MtkWapi.lambda$getMtkStaNetwork$3(mutable, supplicantStatus, iSupplicantNetwork);
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "MtkStaIface.getMtkStaNetwork failed with exception", e);
        }
        if (mutable.value != 0) {
            return ISupplicantStaNetwork.asInterface(((ISupplicantNetwork) mutable.value).asBinder());
        }
        return null;
    }

    static void lambda$getMtkStaNetwork$3(HidlSupport.Mutable mutable, SupplicantStatus supplicantStatus, ISupplicantNetwork iSupplicantNetwork) {
        if (checkStatusAndLogFailure(supplicantStatus, "getMtkStaNetwork")) {
            mutable.value = iSupplicantNetwork;
        }
    }

    private static Object getLockForSupplicantStaIfaceHal() {
        return getLock(WifiInjector.getInstance().getSupplicantStaIfaceHal());
    }

    private static Object getLock(SupplicantStaIfaceHal supplicantStaIfaceHal) {
        try {
            Field declaredField = supplicantStaIfaceHal.getClass().getDeclaredField("mLock");
            declaredField.setAccessible(true);
            return declaredField.get(supplicantStaIfaceHal);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return new Object();
        }
    }

    private static ISupplicant getISupplicant() {
        SupplicantStaIfaceHal supplicantStaIfaceHal = WifiInjector.getInstance().getSupplicantStaIfaceHal();
        try {
            Field declaredField = supplicantStaIfaceHal.getClass().getDeclaredField("mISupplicant");
            declaredField.setAccessible(true);
            return (ISupplicant) declaredField.get(supplicantStaIfaceHal);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean checkStatusAndLogFailure(SupplicantStatus supplicantStatus, String str) {
        if (supplicantStatus.code != 0) {
            Log.e(TAG, "ISupplicantStaIface." + str + " failed: " + supplicantStatus);
            return false;
        }
        Log.d(TAG, "ISupplicantStaIface." + str + " succeeded");
        return true;
    }

    private static String getInterfaceName() {
        WifiStateMachine wifiStateMachine = WifiInjector.getInstance().getWifiStateMachine();
        try {
            Field declaredField = wifiStateMachine.getClass().getDeclaredField("mInterfaceName");
            declaredField.setAccessible(true);
            return (String) declaredField.get(wifiStateMachine);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static void supplicantServiceDiedHandler(String str) {
        SupplicantStaIfaceHal supplicantStaIfaceHal = WifiInjector.getInstance().getSupplicantStaIfaceHal();
        try {
            Method declaredMethod = supplicantStaIfaceHal.getClass().getDeclaredMethod("supplicantServiceDiedHandler", String.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(supplicantStaIfaceHal, str);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkStatusAndLogFailure(SupplicantStaIfaceHal supplicantStaIfaceHal, SupplicantStatus supplicantStatus, String str) {
        try {
            Method declaredMethod = supplicantStaIfaceHal.getClass().getDeclaredMethod("checkStatusAndLogFailure", SupplicantStatus.class, String.class);
            declaredMethod.setAccessible(true);
            return ((Boolean) declaredMethod.invoke(supplicantStaIfaceHal, supplicantStatus, str)).booleanValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void handleRemoteException(SupplicantStaIfaceHal supplicantStaIfaceHal, RemoteException remoteException, String str) {
        try {
            Method declaredMethod = supplicantStaIfaceHal.getClass().getDeclaredMethod("handleRemoteException", RemoteException.class, String.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(supplicantStaIfaceHal, remoteException, str);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static Object getLock(SupplicantStaNetworkHal supplicantStaNetworkHal) {
        try {
            Field declaredField = supplicantStaNetworkHal.getClass().getDeclaredField("mLock");
            declaredField.setAccessible(true);
            return declaredField.get(supplicantStaNetworkHal);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return new Object();
        }
    }

    private static boolean checkMtkIfaceAndLogFailure(String str) {
        if (mMtkiface == null) {
            Log.e(TAG, "Can't call " + str + ", Mtkiface is null");
            return false;
        }
        Log.d(TAG, "Do Mtkiface." + str);
        return true;
    }

    private static boolean checkStatusAndLogFailure(SupplicantStaNetworkHal supplicantStaNetworkHal, SupplicantStatus supplicantStatus, String str) {
        try {
            Method declaredMethod = supplicantStaNetworkHal.getClass().getDeclaredMethod("checkStatusAndLogFailure", SupplicantStatus.class, String.class);
            declaredMethod.setAccessible(true);
            return ((Boolean) declaredMethod.invoke(supplicantStaNetworkHal, supplicantStatus, str)).booleanValue();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void handleRemoteException(SupplicantStaNetworkHal supplicantStaNetworkHal, RemoteException remoteException, String str) {
        try {
            Method declaredMethod = supplicantStaNetworkHal.getClass().getDeclaredMethod("handleRemoteException", RemoteException.class, String.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(supplicantStaNetworkHal, remoteException, str);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static Context getContext() {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        try {
            Field declaredField = wifiInjector.getClass().getDeclaredField("mContext");
            declaredField.setAccessible(true);
            return (Context) declaredField.get(wifiInjector);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void init() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    int intExtra = intent.getIntExtra("wifi_state", 4);
                    Log.d(MtkWapi.TAG, "onReceive WIFI_STATE_CHANGED_ACTION state --> " + intExtra);
                    if (intExtra == 1) {
                        MtkWapi.mWapiCertSelCache = null;
                    }
                }
            }
        }, new IntentFilter(intentFilter));
    }
}
