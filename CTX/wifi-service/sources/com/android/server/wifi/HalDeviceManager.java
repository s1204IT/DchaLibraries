package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifi;
import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.IWifiChipEventCallback;
import android.hardware.wifi.V1_0.IWifiEventCallback;
import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.WifiDebugRingBufferStatus;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Handler;
import android.os.HidlSupport;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.MutableInt;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HalDeviceManager {

    @VisibleForTesting
    public static final String HAL_INSTANCE_NAME = "default";
    private static final int[] IFACE_TYPES_BY_PRIORITY = {1, 0, 2, 3};
    private static final int START_HAL_RETRY_INTERVAL_MS = 20;

    @VisibleForTesting
    public static final int START_HAL_RETRY_TIMES = 3;
    private static final String TAG = "HalDevMgr";
    private static final boolean VDBG = false;
    private final Clock mClock;
    private IServiceManager mServiceManager;
    private IWifi mWifi;
    private boolean mDbg = false;
    private final Object mLock = new Object();
    private final WifiEventCallback mWifiEventCallback = new WifiEventCallback();
    private final Set<ManagerStatusListenerProxy> mManagerStatusListeners = new HashSet();
    private final SparseArray<Map<InterfaceAvailableForRequestListenerProxy, Boolean>> mInterfaceAvailableForRequestListeners = new SparseArray<>();
    private final SparseArray<IWifiChipEventCallback.Stub> mDebugCallbacks = new SparseArray<>();
    private final Map<Pair<String, Integer>, InterfaceCacheEntry> mInterfaceInfoCache = new HashMap();
    private final IHwBinder.DeathRecipient mServiceManagerDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            HalDeviceManager.lambda$new$2(this.f$0, j);
        }
    };
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String str, String str2, boolean z) {
            Log.d(HalDeviceManager.TAG, "IWifi registration notification: fqName=" + str + ", name=" + str2 + ", preexisting=" + z);
            synchronized (HalDeviceManager.this.mLock) {
                HalDeviceManager.this.initIWifiIfNecessary();
            }
        }
    };
    private final IHwBinder.DeathRecipient mIWifiDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            HalDeviceManager.lambda$new$3(this.f$0, j);
        }
    };

    public interface InterfaceAvailableForRequestListener {
        void onAvailabilityChanged(boolean z);
    }

    public interface InterfaceDestroyedListener {
        void onDestroyed(String str);
    }

    public interface ManagerStatusListener {
        void onStatusChanged();
    }

    public HalDeviceManager(Clock clock) {
        this.mClock = clock;
        this.mInterfaceAvailableForRequestListeners.put(0, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(1, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(2, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(3, new HashMap());
    }

    void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
    }

    public void initialize() {
        initializeInternal();
    }

    public void registerStatusListener(ManagerStatusListener managerStatusListener, Handler handler) {
        synchronized (this.mLock) {
            if (!this.mManagerStatusListeners.add(new ManagerStatusListenerProxy(managerStatusListener, handler))) {
                Log.w(TAG, "registerStatusListener: duplicate registration ignored");
            }
        }
    }

    public boolean isSupported() {
        return isSupportedInternal();
    }

    public boolean isReady() {
        return this.mWifi != null;
    }

    public boolean isStarted() {
        return isWifiStarted();
    }

    public boolean start() {
        return startWifi();
    }

    public void stop() {
        stopWifi();
    }

    public Set<Integer> getSupportedIfaceTypes() {
        return getSupportedIfaceTypesInternal(null);
    }

    public Set<Integer> getSupportedIfaceTypes(IWifiChip iWifiChip) {
        return getSupportedIfaceTypesInternal(iWifiChip);
    }

    public IWifiStaIface createStaIface(boolean z, InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        return (IWifiStaIface) createIface(0, z, interfaceDestroyedListener, handler);
    }

    public IWifiApIface createApIface(InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        return (IWifiApIface) createIface(1, false, interfaceDestroyedListener, handler);
    }

    public IWifiP2pIface createP2pIface(InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        return (IWifiP2pIface) createIface(2, false, interfaceDestroyedListener, handler);
    }

    public IWifiNanIface createNanIface(InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        return (IWifiNanIface) createIface(3, false, interfaceDestroyedListener, handler);
    }

    public boolean removeIface(IWifiIface iWifiIface) {
        boolean zRemoveIfaceInternal = removeIfaceInternal(iWifiIface);
        dispatchAvailableForRequestListeners();
        return zRemoveIfaceInternal;
    }

    public IWifiChip getChip(IWifiIface iWifiIface) {
        String name = getName(iWifiIface);
        int type = getType(iWifiIface);
        synchronized (this.mLock) {
            InterfaceCacheEntry interfaceCacheEntry = this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (interfaceCacheEntry == null) {
                Log.e(TAG, "getChip: no entry for iface(name)=" + name);
                return null;
            }
            return interfaceCacheEntry.chip;
        }
    }

    public boolean registerDestroyedListener(IWifiIface iWifiIface, InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        String name = getName(iWifiIface);
        int type = getType(iWifiIface);
        synchronized (this.mLock) {
            InterfaceCacheEntry interfaceCacheEntry = this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (interfaceCacheEntry == null) {
                Log.e(TAG, "registerDestroyedListener: no entry for iface(name)=" + name);
                return false;
            }
            return interfaceCacheEntry.destroyedListeners.add(new InterfaceDestroyedListenerProxy(name, interfaceDestroyedListener, handler));
        }
    }

    public void registerInterfaceAvailableForRequestListener(int i, InterfaceAvailableForRequestListener interfaceAvailableForRequestListener, Handler handler) {
        synchronized (this.mLock) {
            InterfaceAvailableForRequestListenerProxy interfaceAvailableForRequestListenerProxy = new InterfaceAvailableForRequestListenerProxy(interfaceAvailableForRequestListener, handler);
            if (this.mInterfaceAvailableForRequestListeners.get(i).containsKey(interfaceAvailableForRequestListenerProxy)) {
                return;
            }
            this.mInterfaceAvailableForRequestListeners.get(i).put(interfaceAvailableForRequestListenerProxy, null);
            WifiChipInfo[] allChipInfo = getAllChipInfo();
            if (allChipInfo == null) {
                Log.e(TAG, "registerInterfaceAvailableForRequestListener: no chip info found - but possibly registered pre-started - ignoring");
            } else {
                dispatchAvailableForRequestListenersForType(i, allChipInfo);
            }
        }
    }

    public void unregisterInterfaceAvailableForRequestListener(int i, InterfaceAvailableForRequestListener interfaceAvailableForRequestListener) {
        synchronized (this.mLock) {
            this.mInterfaceAvailableForRequestListeners.get(i).remove(new InterfaceAvailableForRequestListenerProxy(interfaceAvailableForRequestListener, null));
        }
    }

    public static String getName(IWifiIface iWifiIface) {
        if (iWifiIface == null) {
            return "<null>";
        }
        final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
        try {
            iWifiIface.getName(new IWifiIface.getNameCallback() {
                @Override
                public final void onValues(WifiStatus wifiStatus, String str) {
                    HalDeviceManager.lambda$getName$0(mutable, wifiStatus, str);
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Exception on getName: " + e);
        }
        return (String) mutable.value;
    }

    static void lambda$getName$0(HidlSupport.Mutable mutable, WifiStatus wifiStatus, String str) {
        if (wifiStatus.code == 0) {
            mutable.value = str;
            return;
        }
        Log.e(TAG, "Error on getName: " + statusString(wifiStatus));
    }

    public IWifiRttController createRttController() {
        synchronized (this.mLock) {
            if (this.mWifi == null) {
                Log.e(TAG, "createRttController: null IWifi");
                return null;
            }
            WifiChipInfo[] allChipInfo = getAllChipInfo();
            if (allChipInfo == null) {
                Log.e(TAG, "createRttController: no chip info found");
                stopWifi();
                return null;
            }
            for (WifiChipInfo wifiChipInfo : allChipInfo) {
                final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
                try {
                    wifiChipInfo.chip.createRttController(null, new IWifiChip.createRttControllerCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, IWifiRttController iWifiRttController) {
                            HalDeviceManager.lambda$createRttController$1(mutable, wifiStatus, iWifiRttController);
                        }
                    });
                } catch (RemoteException e) {
                    Log.e(TAG, "IWifiChip.createRttController exception: " + e);
                }
                if (mutable.value != 0) {
                    return (IWifiRttController) mutable.value;
                }
            }
            Log.e(TAG, "createRttController: not available from any of the chips");
            return null;
        }
    }

    static void lambda$createRttController$1(HidlSupport.Mutable mutable, WifiStatus wifiStatus, IWifiRttController iWifiRttController) {
        if (wifiStatus.code == 0) {
            mutable.value = iWifiRttController;
            return;
        }
        Log.e(TAG, "IWifiChip.createRttController failed: " + statusString(wifiStatus));
    }

    private class InterfaceCacheEntry {
        public IWifiChip chip;
        public int chipId;
        public long creationTime;
        public Set<InterfaceDestroyedListenerProxy> destroyedListeners;
        public boolean isLowPriority;
        public String name;
        public int type;

        private InterfaceCacheEntry() {
            this.destroyedListeners = new HashSet();
        }

        public String toString() {
            return "{name=" + this.name + ", type=" + this.type + ", destroyedListeners.size()=" + this.destroyedListeners.size() + ", creationTime=" + this.creationTime + ", isLowPriority=" + this.isLowPriority + "}";
        }
    }

    private class WifiIfaceInfo {
        public IWifiIface iface;
        public String name;

        private WifiIfaceInfo() {
        }
    }

    private class WifiChipInfo {
        public ArrayList<IWifiChip.ChipMode> availableModes;
        public IWifiChip chip;
        public int chipId;
        public int currentModeId;
        public boolean currentModeIdValid;
        public WifiIfaceInfo[][] ifaces;

        private WifiChipInfo() {
            this.ifaces = new WifiIfaceInfo[HalDeviceManager.IFACE_TYPES_BY_PRIORITY.length][];
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{chipId=");
            sb.append(this.chipId);
            sb.append(", availableModes=");
            sb.append(this.availableModes);
            sb.append(", currentModeIdValid=");
            sb.append(this.currentModeIdValid);
            sb.append(", currentModeId=");
            sb.append(this.currentModeId);
            for (int i : HalDeviceManager.IFACE_TYPES_BY_PRIORITY) {
                sb.append(", ifaces[" + i + "].length=");
                sb.append(this.ifaces[i].length);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    protected IWifi getWifiServiceMockable() {
        try {
            return IWifi.getService();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IWifi service: " + e);
            return null;
        }
    }

    protected IServiceManager getServiceManagerMockable() {
        try {
            return IServiceManager.getService();
        } catch (RemoteException e) {
            Log.e(TAG, "Exception getting IServiceManager: " + e);
            return null;
        }
    }

    private void initializeInternal() {
        initIServiceManagerIfNecessary();
        if (isSupportedInternal()) {
            initIWifiIfNecessary();
        }
    }

    private void teardownInternal() {
        managerStatusListenerDispatch();
        dispatchAllDestroyedListeners();
        this.mInterfaceAvailableForRequestListeners.get(0).clear();
        this.mInterfaceAvailableForRequestListeners.get(1).clear();
        this.mInterfaceAvailableForRequestListeners.get(2).clear();
        this.mInterfaceAvailableForRequestListeners.get(3).clear();
    }

    public static void lambda$new$2(HalDeviceManager halDeviceManager, long j) {
        Log.wtf(TAG, "IServiceManager died: cookie=" + j);
        synchronized (halDeviceManager.mLock) {
            halDeviceManager.mServiceManager = null;
        }
    }

    private void initIServiceManagerIfNecessary() {
        if (this.mDbg) {
            Log.d(TAG, "initIServiceManagerIfNecessary");
        }
        synchronized (this.mLock) {
            if (this.mServiceManager != null) {
                return;
            }
            this.mServiceManager = getServiceManagerMockable();
            if (this.mServiceManager == null) {
                Log.wtf(TAG, "Failed to get IServiceManager instance");
            } else {
                try {
                    if (!this.mServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0L)) {
                        Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                        this.mServiceManager = null;
                    } else if (!this.mServiceManager.registerForNotifications(IWifi.kInterfaceName, "", this.mServiceNotificationCallback)) {
                        Log.wtf(TAG, "Failed to register a listener for IWifi service");
                        this.mServiceManager = null;
                    }
                } catch (RemoteException e) {
                    Log.wtf(TAG, "Exception while operating on IServiceManager: " + e);
                    this.mServiceManager = null;
                }
            }
        }
    }

    private boolean isSupportedInternal() {
        synchronized (this.mLock) {
            if (this.mServiceManager == null) {
                Log.e(TAG, "isSupported: called but mServiceManager is null!?");
                return false;
            }
            try {
                return this.mServiceManager.getTransport(IWifi.kInterfaceName, HAL_INSTANCE_NAME) != 0;
            } catch (RemoteException e) {
                Log.wtf(TAG, "Exception while operating on IServiceManager: " + e);
                return false;
            }
        }
    }

    public static void lambda$new$3(HalDeviceManager halDeviceManager, long j) {
        Log.e(TAG, "IWifi HAL service died! Have a listener for it ... cookie=" + j);
        synchronized (halDeviceManager.mLock) {
            halDeviceManager.mWifi = null;
            halDeviceManager.teardownInternal();
        }
    }

    private void initIWifiIfNecessary() {
        if (this.mDbg) {
            Log.d(TAG, "initIWifiIfNecessary");
        }
        synchronized (this.mLock) {
            if (this.mWifi != null) {
                return;
            }
            try {
                this.mWifi = getWifiServiceMockable();
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while operating on IWifi: " + e);
            }
            if (this.mWifi == null) {
                Log.e(TAG, "IWifi not (yet) available - but have a listener for it ...");
                return;
            }
            if (!this.mWifi.linkToDeath(this.mIWifiDeathRecipient, 0L)) {
                Log.e(TAG, "Error on linkToDeath on IWifi - will retry later");
                return;
            }
            WifiStatus wifiStatusRegisterEventCallback = this.mWifi.registerEventCallback(this.mWifiEventCallback);
            if (wifiStatusRegisterEventCallback.code != 0) {
                Log.e(TAG, "IWifi.registerEventCallback failed: " + statusString(wifiStatusRegisterEventCallback));
                this.mWifi = null;
                return;
            }
            stopWifi();
        }
    }

    private void initIWifiChipDebugListeners() {
    }

    private static void lambda$initIWifiChipDebugListeners$4(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getChipIds failed: " + statusString(wifiStatus));
    }

    private static void lambda$initIWifiChipDebugListeners$5(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, IWifiChip iWifiChip) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = iWifiChip;
            return;
        }
        Log.e(TAG, "getChip failed: " + statusString(wifiStatus));
    }

    class AnonymousClass2 extends IWifiChipEventCallback.Stub {
        AnonymousClass2() {
        }

        @Override
        public void onChipReconfigured(int i) throws RemoteException {
            Log.d(HalDeviceManager.TAG, "onChipReconfigured: modeId=" + i);
        }

        @Override
        public void onChipReconfigureFailure(WifiStatus wifiStatus) throws RemoteException {
            Log.d(HalDeviceManager.TAG, "onChipReconfigureFailure: status=" + HalDeviceManager.statusString(wifiStatus));
        }

        @Override
        public void onIfaceAdded(int i, String str) throws RemoteException {
            Log.d(HalDeviceManager.TAG, "onIfaceAdded: type=" + i + ", name=" + str);
        }

        @Override
        public void onIfaceRemoved(int i, String str) throws RemoteException {
            Log.d(HalDeviceManager.TAG, "onIfaceRemoved: type=" + i + ", name=" + str);
        }

        @Override
        public void onDebugRingBufferDataAvailable(WifiDebugRingBufferStatus wifiDebugRingBufferStatus, ArrayList<Byte> arrayList) throws RemoteException {
            Log.d(HalDeviceManager.TAG, "onDebugRingBufferDataAvailable");
        }

        @Override
        public void onDebugErrorAlert(int i, ArrayList<Byte> arrayList) throws RemoteException {
            Log.d(HalDeviceManager.TAG, "onDebugErrorAlert");
        }
    }

    private WifiChipInfo[] getAllChipInfo() {
        synchronized (this.mLock) {
            WifiChipInfo[] wifiChipInfoArr = null;
            if (this.mWifi == null) {
                Log.e(TAG, "getAllChipInfo: called but mWifi is null!?");
                return null;
            }
            try {
                ?? r10 = 0;
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
                this.mWifi.getChipIds(new IWifi.getChipIdsCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        HalDeviceManager.lambda$getAllChipInfo$6(mutableBoolean, mutable, wifiStatus, arrayList);
                    }
                });
                if (!mutableBoolean.value) {
                    return null;
                }
                if (((ArrayList) mutable.value).size() == 0) {
                    Log.e(TAG, "Should have at least 1 chip!");
                    return null;
                }
                WifiChipInfo[] wifiChipInfoArr2 = new WifiChipInfo[((ArrayList) mutable.value).size()];
                final HidlSupport.Mutable mutable2 = new HidlSupport.Mutable();
                Iterator it = ((ArrayList) mutable.value).iterator();
                int i = 0;
                while (it.hasNext()) {
                    Integer num = (Integer) it.next();
                    this.mWifi.getChip(num.intValue(), new IWifi.getChipCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, IWifiChip iWifiChip) {
                            HalDeviceManager.lambda$getAllChipInfo$7(mutableBoolean, mutable2, wifiStatus, iWifiChip);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return wifiChipInfoArr;
                    }
                    final HidlSupport.Mutable mutable3 = new HidlSupport.Mutable();
                    ((IWifiChip) mutable2.value).getAvailableModes(new IWifiChip.getAvailableModesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$8(mutableBoolean, mutable3, wifiStatus, arrayList);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return wifiChipInfoArr;
                    }
                    final MutableBoolean mutableBoolean2 = new MutableBoolean(r10);
                    final MutableInt mutableInt = new MutableInt(r10);
                    ((IWifiChip) mutable2.value).getMode(new IWifiChip.getModeCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, int i2) {
                            HalDeviceManager.lambda$getAllChipInfo$9(mutableBoolean, mutableBoolean2, mutableInt, wifiStatus, i2);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return wifiChipInfoArr;
                    }
                    final HidlSupport.Mutable mutable4 = new HidlSupport.Mutable();
                    MutableInt mutableInt2 = new MutableInt(r10);
                    ((IWifiChip) mutable2.value).getStaIfaceNames(new IWifiChip.getStaIfaceNamesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$10(mutableBoolean, mutable4, wifiStatus, arrayList);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return wifiChipInfoArr;
                    }
                    WifiIfaceInfo[] wifiIfaceInfoArr = new WifiIfaceInfo[((ArrayList) mutable4.value).size()];
                    Iterator it2 = ((ArrayList) mutable4.value).iterator();
                    while (it2.hasNext()) {
                        final String str = (String) it2.next();
                        Iterator it3 = it2;
                        Iterator it4 = it;
                        Integer num2 = num;
                        final MutableInt mutableInt3 = mutableInt2;
                        WifiChipInfo[] wifiChipInfoArr3 = wifiChipInfoArr2;
                        HidlSupport.Mutable mutable5 = mutable4;
                        MutableInt mutableInt4 = mutableInt;
                        MutableBoolean mutableBoolean3 = mutableBoolean2;
                        final WifiIfaceInfo[] wifiIfaceInfoArr2 = wifiIfaceInfoArr;
                        WifiIfaceInfo[] wifiIfaceInfoArr3 = wifiIfaceInfoArr;
                        HidlSupport.Mutable mutable6 = mutable3;
                        ((IWifiChip) mutable2.value).getStaIface(str, new IWifiChip.getStaIfaceCallback() {
                            @Override
                            public final void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
                                HalDeviceManager.lambda$getAllChipInfo$11(this.f$0, mutableBoolean, str, wifiIfaceInfoArr2, mutableInt3, wifiStatus, iWifiStaIface);
                            }
                        });
                        if (!mutableBoolean.value) {
                            return null;
                        }
                        mutable3 = mutable6;
                        mutable4 = mutable5;
                        it2 = it3;
                        it = it4;
                        num = num2;
                        mutableInt2 = mutableInt3;
                        wifiChipInfoArr2 = wifiChipInfoArr3;
                        mutableInt = mutableInt4;
                        mutableBoolean2 = mutableBoolean3;
                        wifiIfaceInfoArr = wifiIfaceInfoArr3;
                    }
                    final MutableInt mutableInt5 = mutableInt2;
                    MutableInt mutableInt6 = mutableInt;
                    MutableBoolean mutableBoolean4 = mutableBoolean2;
                    WifiIfaceInfo[] wifiIfaceInfoArr4 = wifiIfaceInfoArr;
                    WifiChipInfo[] wifiChipInfoArr4 = wifiChipInfoArr2;
                    Iterator it5 = it;
                    Integer num3 = num;
                    final HidlSupport.Mutable mutable7 = mutable4;
                    HidlSupport.Mutable mutable8 = mutable3;
                    mutableInt5.value = 0;
                    ((IWifiChip) mutable2.value).getApIfaceNames(new IWifiChip.getApIfaceNamesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$12(mutableBoolean, mutable7, wifiStatus, arrayList);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return null;
                    }
                    WifiIfaceInfo[] wifiIfaceInfoArr5 = new WifiIfaceInfo[((ArrayList) mutable7.value).size()];
                    Iterator it6 = ((ArrayList) mutable7.value).iterator();
                    while (it6.hasNext()) {
                        final String str2 = (String) it6.next();
                        Iterator it7 = it6;
                        HidlSupport.Mutable mutable9 = mutable8;
                        final WifiIfaceInfo[] wifiIfaceInfoArr6 = wifiIfaceInfoArr5;
                        WifiIfaceInfo[] wifiIfaceInfoArr7 = wifiIfaceInfoArr5;
                        ((IWifiChip) mutable2.value).getApIface(str2, new IWifiChip.getApIfaceCallback() {
                            @Override
                            public final void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
                                HalDeviceManager.lambda$getAllChipInfo$13(this.f$0, mutableBoolean, str2, wifiIfaceInfoArr6, mutableInt5, wifiStatus, iWifiApIface);
                            }
                        });
                        if (!mutableBoolean.value) {
                            return null;
                        }
                        it6 = it7;
                        mutable8 = mutable9;
                        wifiIfaceInfoArr5 = wifiIfaceInfoArr7;
                    }
                    HidlSupport.Mutable mutable10 = mutable8;
                    WifiIfaceInfo[] wifiIfaceInfoArr8 = wifiIfaceInfoArr5;
                    mutableInt5.value = 0;
                    ((IWifiChip) mutable2.value).getP2pIfaceNames(new IWifiChip.getP2pIfaceNamesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$14(mutableBoolean, mutable7, wifiStatus, arrayList);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return null;
                    }
                    WifiIfaceInfo[] wifiIfaceInfoArr9 = new WifiIfaceInfo[((ArrayList) mutable7.value).size()];
                    Iterator it8 = ((ArrayList) mutable7.value).iterator();
                    while (it8.hasNext()) {
                        final String str3 = (String) it8.next();
                        Iterator it9 = it8;
                        final WifiIfaceInfo[] wifiIfaceInfoArr10 = wifiIfaceInfoArr9;
                        WifiIfaceInfo[] wifiIfaceInfoArr11 = wifiIfaceInfoArr9;
                        ((IWifiChip) mutable2.value).getP2pIface(str3, new IWifiChip.getP2pIfaceCallback() {
                            @Override
                            public final void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
                                HalDeviceManager.lambda$getAllChipInfo$15(this.f$0, mutableBoolean, str3, wifiIfaceInfoArr10, mutableInt5, wifiStatus, iWifiP2pIface);
                            }
                        });
                        if (!mutableBoolean.value) {
                            return null;
                        }
                        it8 = it9;
                        wifiIfaceInfoArr9 = wifiIfaceInfoArr11;
                    }
                    WifiIfaceInfo[] wifiIfaceInfoArr12 = wifiIfaceInfoArr9;
                    mutableInt5.value = 0;
                    ((IWifiChip) mutable2.value).getNanIfaceNames(new IWifiChip.getNanIfaceNamesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            HalDeviceManager.lambda$getAllChipInfo$16(mutableBoolean, mutable7, wifiStatus, arrayList);
                        }
                    });
                    if (!mutableBoolean.value) {
                        return null;
                    }
                    final WifiIfaceInfo[] wifiIfaceInfoArr13 = new WifiIfaceInfo[((ArrayList) mutable7.value).size()];
                    Iterator it10 = ((ArrayList) mutable7.value).iterator();
                    while (it10.hasNext()) {
                        final String str4 = (String) it10.next();
                        Iterator it11 = it10;
                        ((IWifiChip) mutable2.value).getNanIface(str4, new IWifiChip.getNanIfaceCallback() {
                            @Override
                            public final void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
                                HalDeviceManager.lambda$getAllChipInfo$17(this.f$0, mutableBoolean, str4, wifiIfaceInfoArr13, mutableInt5, wifiStatus, iWifiNanIface);
                            }
                        });
                        if (!mutableBoolean.value) {
                            return null;
                        }
                        it10 = it11;
                    }
                    WifiChipInfo wifiChipInfo = new WifiChipInfo();
                    int i2 = i + 1;
                    wifiChipInfoArr4[i] = wifiChipInfo;
                    wifiChipInfo.chip = (IWifiChip) mutable2.value;
                    wifiChipInfo.chipId = num3.intValue();
                    wifiChipInfo.availableModes = (ArrayList) mutable10.value;
                    wifiChipInfo.currentModeIdValid = mutableBoolean4.value;
                    wifiChipInfo.currentModeId = mutableInt6.value;
                    wifiChipInfo.ifaces[0] = wifiIfaceInfoArr4;
                    wifiChipInfo.ifaces[1] = wifiIfaceInfoArr8;
                    wifiChipInfo.ifaces[2] = wifiIfaceInfoArr12;
                    wifiChipInfo.ifaces[3] = wifiIfaceInfoArr13;
                    i = i2;
                    r10 = 0;
                    it = it5;
                    wifiChipInfoArr2 = wifiChipInfoArr4;
                    wifiChipInfoArr = null;
                }
                return wifiChipInfoArr2;
            } catch (RemoteException e) {
                Log.e(TAG, "getAllChipInfoAndValidateCache exception: " + e);
                return null;
            }
        }
    }

    static void lambda$getAllChipInfo$6(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getChipIds failed: " + statusString(wifiStatus));
    }

    static void lambda$getAllChipInfo$7(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, IWifiChip iWifiChip) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = iWifiChip;
            return;
        }
        Log.e(TAG, "getChip failed: " + statusString(wifiStatus));
    }

    static void lambda$getAllChipInfo$8(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getAvailableModes failed: " + statusString(wifiStatus));
    }

    static void lambda$getAllChipInfo$9(MutableBoolean mutableBoolean, MutableBoolean mutableBoolean2, MutableInt mutableInt, WifiStatus wifiStatus, int i) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutableBoolean2.value = true;
            mutableInt.value = i;
        } else {
            if (wifiStatus.code == 5) {
                mutableBoolean.value = true;
                return;
            }
            Log.e(TAG, "getMode failed: " + statusString(wifiStatus));
        }
    }

    static void lambda$getAllChipInfo$10(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getStaIfaceNames failed: " + statusString(wifiStatus));
    }

    public static void lambda$getAllChipInfo$11(HalDeviceManager halDeviceManager, MutableBoolean mutableBoolean, String str, WifiIfaceInfo[] wifiIfaceInfoArr, MutableInt mutableInt, WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            WifiIfaceInfo wifiIfaceInfo = new WifiIfaceInfo();
            wifiIfaceInfo.name = str;
            wifiIfaceInfo.iface = iWifiStaIface;
            int i = mutableInt.value;
            mutableInt.value = i + 1;
            wifiIfaceInfoArr[i] = wifiIfaceInfo;
            return;
        }
        Log.e(TAG, "getStaIface failed: " + statusString(wifiStatus));
    }

    static void lambda$getAllChipInfo$12(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getApIfaceNames failed: " + statusString(wifiStatus));
    }

    public static void lambda$getAllChipInfo$13(HalDeviceManager halDeviceManager, MutableBoolean mutableBoolean, String str, WifiIfaceInfo[] wifiIfaceInfoArr, MutableInt mutableInt, WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            WifiIfaceInfo wifiIfaceInfo = new WifiIfaceInfo();
            wifiIfaceInfo.name = str;
            wifiIfaceInfo.iface = iWifiApIface;
            int i = mutableInt.value;
            mutableInt.value = i + 1;
            wifiIfaceInfoArr[i] = wifiIfaceInfo;
            return;
        }
        Log.e(TAG, "getApIface failed: " + statusString(wifiStatus));
    }

    static void lambda$getAllChipInfo$14(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getP2pIfaceNames failed: " + statusString(wifiStatus));
    }

    public static void lambda$getAllChipInfo$15(HalDeviceManager halDeviceManager, MutableBoolean mutableBoolean, String str, WifiIfaceInfo[] wifiIfaceInfoArr, MutableInt mutableInt, WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            WifiIfaceInfo wifiIfaceInfo = new WifiIfaceInfo();
            wifiIfaceInfo.name = str;
            wifiIfaceInfo.iface = iWifiP2pIface;
            int i = mutableInt.value;
            mutableInt.value = i + 1;
            wifiIfaceInfoArr[i] = wifiIfaceInfo;
            return;
        }
        Log.e(TAG, "getP2pIface failed: " + statusString(wifiStatus));
    }

    static void lambda$getAllChipInfo$16(MutableBoolean mutableBoolean, HidlSupport.Mutable mutable, WifiStatus wifiStatus, ArrayList arrayList) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            mutable.value = arrayList;
            return;
        }
        Log.e(TAG, "getNanIfaceNames failed: " + statusString(wifiStatus));
    }

    public static void lambda$getAllChipInfo$17(HalDeviceManager halDeviceManager, MutableBoolean mutableBoolean, String str, WifiIfaceInfo[] wifiIfaceInfoArr, MutableInt mutableInt, WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
        mutableBoolean.value = wifiStatus.code == 0;
        if (mutableBoolean.value) {
            WifiIfaceInfo wifiIfaceInfo = new WifiIfaceInfo();
            wifiIfaceInfo.name = str;
            wifiIfaceInfo.iface = iWifiNanIface;
            int i = mutableInt.value;
            mutableInt.value = i + 1;
            wifiIfaceInfoArr[i] = wifiIfaceInfo;
            return;
        }
        Log.e(TAG, "getNanIface failed: " + statusString(wifiStatus));
    }

    private boolean validateInterfaceCache(WifiChipInfo[] wifiChipInfoArr) {
        boolean z;
        InterfaceCacheEntry next;
        synchronized (this.mLock) {
            Iterator<InterfaceCacheEntry> it = this.mInterfaceInfoCache.values().iterator();
            do {
                z = true;
                if (!it.hasNext()) {
                    return true;
                }
                next = it.next();
                WifiChipInfo wifiChipInfo = null;
                int length = wifiChipInfoArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    WifiChipInfo wifiChipInfo2 = wifiChipInfoArr[i];
                    if (wifiChipInfo2.chipId != next.chipId) {
                        i++;
                    } else {
                        wifiChipInfo = wifiChipInfo2;
                        break;
                    }
                }
                if (wifiChipInfo == null) {
                    Log.e(TAG, "validateInterfaceCache: no chip found for " + next);
                    return false;
                }
                WifiIfaceInfo[] wifiIfaceInfoArr = wifiChipInfo.ifaces[next.type];
                if (wifiIfaceInfoArr == null) {
                    Log.e(TAG, "validateInterfaceCache: invalid type on entry " + next);
                    return false;
                }
                int length2 = wifiIfaceInfoArr.length;
                int i2 = 0;
                while (true) {
                    if (i2 < length2) {
                        if (wifiIfaceInfoArr[i2].name.equals(next.name)) {
                            break;
                        }
                        i2++;
                    } else {
                        z = false;
                        break;
                    }
                }
            } while (z);
            Log.e(TAG, "validateInterfaceCache: no interface found for " + next);
            return false;
        }
    }

    private boolean isWifiStarted() {
        synchronized (this.mLock) {
            try {
                try {
                    if (this.mWifi == null) {
                        Log.w(TAG, "isWifiStarted called but mWifi is null!?");
                        return false;
                    }
                    return this.mWifi.isStarted();
                } catch (RemoteException e) {
                    Log.e(TAG, "isWifiStarted exception: " + e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean startWifi() {
        synchronized (this.mLock) {
            try {
                try {
                    if (this.mWifi == null) {
                        Log.w(TAG, "startWifi called but mWifi is null!?");
                        return false;
                    }
                    int i = 0;
                    while (i <= 3) {
                        WifiStatus wifiStatusStart = this.mWifi.start();
                        if (wifiStatusStart.code == 0) {
                            initIWifiChipDebugListeners();
                            managerStatusListenerDispatch();
                            if (i != 0) {
                                Log.d(TAG, "start IWifi succeeded after trying " + i + " times");
                            }
                            return true;
                        }
                        if (wifiStatusStart.code == 5) {
                            Log.e(TAG, "Cannot start IWifi: " + statusString(wifiStatusStart) + ", Retrying...");
                            try {
                                Thread.sleep(20L);
                            } catch (InterruptedException e) {
                            }
                            i++;
                        } else {
                            Log.e(TAG, "Cannot start IWifi: " + statusString(wifiStatusStart));
                            return false;
                        }
                    }
                    Log.e(TAG, "Cannot start IWifi after trying " + i + " times");
                    return false;
                } catch (RemoteException e2) {
                    Log.e(TAG, "startWifi exception: " + e2);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void stopWifi() {
        synchronized (this.mLock) {
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "stopWifi called but mWifi is null!?");
                } else {
                    WifiStatus wifiStatusStop = this.mWifi.stop();
                    if (wifiStatusStop.code != 0) {
                        Log.e(TAG, "Cannot stop IWifi: " + statusString(wifiStatusStop));
                    }
                    teardownInternal();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "stopWifi exception: " + e);
            }
        }
    }

    private class WifiEventCallback extends IWifiEventCallback.Stub {
        private WifiEventCallback() {
        }

        @Override
        public void onStart() throws RemoteException {
        }

        @Override
        public void onStop() throws RemoteException {
        }

        @Override
        public void onFailure(WifiStatus wifiStatus) throws RemoteException {
            Log.e(HalDeviceManager.TAG, "IWifiEventCallback.onFailure: " + HalDeviceManager.statusString(wifiStatus));
            HalDeviceManager.this.teardownInternal();
        }
    }

    private void managerStatusListenerDispatch() {
        synchronized (this.mLock) {
            Iterator<ManagerStatusListenerProxy> it = this.mManagerStatusListeners.iterator();
            while (it.hasNext()) {
                it.next().trigger();
            }
        }
    }

    private class ManagerStatusListenerProxy extends ListenerProxy<ManagerStatusListener> {
        ManagerStatusListenerProxy(ManagerStatusListener managerStatusListener, Handler handler) {
            super(managerStatusListener, handler, "ManagerStatusListenerProxy");
        }

        @Override
        protected void action() {
            ((ManagerStatusListener) this.mListener).onStatusChanged();
        }
    }

    Set<Integer> getSupportedIfaceTypesInternal(IWifiChip iWifiChip) {
        HashSet hashSet = new HashSet();
        WifiChipInfo[] allChipInfo = getAllChipInfo();
        if (allChipInfo == null) {
            Log.e(TAG, "getSupportedIfaceTypesInternal: no chip info found");
            return hashSet;
        }
        final MutableInt mutableInt = new MutableInt(0);
        if (iWifiChip != null) {
            final MutableBoolean mutableBoolean = new MutableBoolean(false);
            try {
                iWifiChip.getId(new IWifiChip.getIdCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, int i) {
                        HalDeviceManager.lambda$getSupportedIfaceTypesInternal$18(mutableInt, mutableBoolean, wifiStatus, i);
                    }
                });
                if (!mutableBoolean.value) {
                    return hashSet;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getSupportedIfaceTypesInternal IWifiChip.getId() exception: " + e);
                return hashSet;
            }
        }
        for (WifiChipInfo wifiChipInfo : allChipInfo) {
            if (iWifiChip == null || wifiChipInfo.chipId == mutableInt.value) {
                Iterator<IWifiChip.ChipMode> it = wifiChipInfo.availableModes.iterator();
                while (it.hasNext()) {
                    Iterator<IWifiChip.ChipIfaceCombination> it2 = it.next().availableCombinations.iterator();
                    while (it2.hasNext()) {
                        Iterator<IWifiChip.ChipIfaceCombinationLimit> it3 = it2.next().limits.iterator();
                        while (it3.hasNext()) {
                            Iterator<Integer> it4 = it3.next().types.iterator();
                            while (it4.hasNext()) {
                                hashSet.add(Integer.valueOf(it4.next().intValue()));
                            }
                        }
                    }
                }
            }
        }
        return hashSet;
    }

    static void lambda$getSupportedIfaceTypesInternal$18(MutableInt mutableInt, MutableBoolean mutableBoolean, WifiStatus wifiStatus, int i) {
        if (wifiStatus.code == 0) {
            mutableInt.value = i;
            mutableBoolean.value = true;
            return;
        }
        Log.e(TAG, "getSupportedIfaceTypesInternal: IWifiChip.getId() error: " + statusString(wifiStatus));
        mutableBoolean.value = false;
    }

    private IWifiIface createIface(int i, boolean z, InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        if (this.mDbg) {
            Log.d(TAG, "createIface: ifaceType=" + i + ", lowPriority=" + z);
        }
        synchronized (this.mLock) {
            WifiChipInfo[] allChipInfo = getAllChipInfo();
            if (allChipInfo == null) {
                Log.e(TAG, "createIface: no chip info found");
                stopWifi();
                return null;
            }
            if (!validateInterfaceCache(allChipInfo)) {
                Log.e(TAG, "createIface: local cache is invalid!");
                stopWifi();
                return null;
            }
            IWifiIface iWifiIfaceCreateIfaceIfPossible = createIfaceIfPossible(allChipInfo, i, z, interfaceDestroyedListener, handler);
            if (iWifiIfaceCreateIfaceIfPossible == null || dispatchAvailableForRequestListeners()) {
                return iWifiIfaceCreateIfaceIfPossible;
            }
            return null;
        }
    }

    private IWifiIface createIfaceIfPossible(WifiChipInfo[] wifiChipInfoArr, int i, boolean z, InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
        IWifiIface iWifiIfaceExecuteChipReconfiguration;
        synchronized (this.mLock) {
            IfaceCreationData ifaceCreationData = null;
            for (WifiChipInfo wifiChipInfo : wifiChipInfoArr) {
                Iterator<IWifiChip.ChipMode> it = wifiChipInfo.availableModes.iterator();
                while (it.hasNext()) {
                    IWifiChip.ChipMode next = it.next();
                    Iterator<IWifiChip.ChipIfaceCombination> it2 = next.availableCombinations.iterator();
                    while (it2.hasNext()) {
                        int[][] iArrExpandIfaceCombos = expandIfaceCombos(it2.next());
                        int length = iArrExpandIfaceCombos.length;
                        IfaceCreationData ifaceCreationData2 = ifaceCreationData;
                        int i2 = 0;
                        while (i2 < length) {
                            int i3 = i2;
                            int i4 = length;
                            int[][] iArr = iArrExpandIfaceCombos;
                            Iterator<IWifiChip.ChipIfaceCombination> it3 = it2;
                            IWifiChip.ChipMode chipMode = next;
                            Iterator<IWifiChip.ChipMode> it4 = it;
                            IfaceCreationData ifaceCreationDataCanIfaceComboSupportRequest = canIfaceComboSupportRequest(wifiChipInfo, next, iArrExpandIfaceCombos[i2], i, z);
                            if (compareIfaceCreationData(ifaceCreationDataCanIfaceComboSupportRequest, ifaceCreationData2)) {
                                ifaceCreationData2 = ifaceCreationDataCanIfaceComboSupportRequest;
                            }
                            i2 = i3 + 1;
                            next = chipMode;
                            length = i4;
                            iArrExpandIfaceCombos = iArr;
                            it2 = it3;
                            it = it4;
                        }
                        ifaceCreationData = ifaceCreationData2;
                    }
                }
            }
            if (ifaceCreationData == null || (iWifiIfaceExecuteChipReconfiguration = executeChipReconfiguration(ifaceCreationData, i)) == null) {
                return null;
            }
            InterfaceCacheEntry interfaceCacheEntry = new InterfaceCacheEntry();
            interfaceCacheEntry.chip = ifaceCreationData.chipInfo.chip;
            interfaceCacheEntry.chipId = ifaceCreationData.chipInfo.chipId;
            interfaceCacheEntry.name = getName(iWifiIfaceExecuteChipReconfiguration);
            interfaceCacheEntry.type = i;
            if (interfaceDestroyedListener != null) {
                interfaceCacheEntry.destroyedListeners.add(new InterfaceDestroyedListenerProxy(interfaceCacheEntry.name, interfaceDestroyedListener, handler));
            }
            interfaceCacheEntry.creationTime = this.mClock.getUptimeSinceBootMillis();
            interfaceCacheEntry.isLowPriority = z;
            if (this.mDbg) {
                Log.d(TAG, "createIfaceIfPossible: added cacheEntry=" + interfaceCacheEntry);
            }
            this.mInterfaceInfoCache.put(Pair.create(interfaceCacheEntry.name, Integer.valueOf(interfaceCacheEntry.type)), interfaceCacheEntry);
            return iWifiIfaceExecuteChipReconfiguration;
        }
    }

    private boolean isItPossibleToCreateIface(WifiChipInfo[] wifiChipInfoArr, int i) {
        for (WifiChipInfo wifiChipInfo : wifiChipInfoArr) {
            for (IWifiChip.ChipMode chipMode : wifiChipInfo.availableModes) {
                Iterator<IWifiChip.ChipIfaceCombination> it = chipMode.availableCombinations.iterator();
                while (it.hasNext()) {
                    int[][] iArrExpandIfaceCombos = expandIfaceCombos(it.next());
                    int length = iArrExpandIfaceCombos.length;
                    int i2 = 0;
                    while (i2 < length) {
                        int i3 = i2;
                        int i4 = length;
                        if (canIfaceComboSupportRequest(wifiChipInfo, chipMode, iArrExpandIfaceCombos[i2], i, false) == null) {
                            i2 = i3 + 1;
                            length = i4;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int[][] expandIfaceCombos(IWifiChip.ChipIfaceCombination chipIfaceCombination) {
        Iterator<IWifiChip.ChipIfaceCombinationLimit> it = chipIfaceCombination.limits.iterator();
        int size = 1;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            IWifiChip.ChipIfaceCombinationLimit next = it.next();
            for (int i = 0; i < next.maxIfaces; i++) {
                size *= next.types.size();
            }
        }
        int[][] iArr = (int[][]) Array.newInstance((Class<?>) int.class, size, IFACE_TYPES_BY_PRIORITY.length);
        int i2 = size;
        for (IWifiChip.ChipIfaceCombinationLimit chipIfaceCombinationLimit : chipIfaceCombination.limits) {
            int size2 = i2;
            for (int i3 = 0; i3 < chipIfaceCombinationLimit.maxIfaces; i3++) {
                size2 /= chipIfaceCombinationLimit.types.size();
                for (int i4 = 0; i4 < size; i4++) {
                    int[] iArr2 = iArr[i4];
                    int iIntValue = chipIfaceCombinationLimit.types.get((i4 / size2) % chipIfaceCombinationLimit.types.size()).intValue();
                    iArr2[iIntValue] = iArr2[iIntValue] + 1;
                }
            }
            i2 = size2;
        }
        return iArr;
    }

    private class IfaceCreationData {
        public WifiChipInfo chipInfo;
        public int chipModeId;
        public List<WifiIfaceInfo> interfacesToBeRemovedFirst;

        private IfaceCreationData() {
        }

        public String toString() {
            return "{chipInfo=" + this.chipInfo + ", chipModeId=" + this.chipModeId + ", interfacesToBeRemovedFirst=" + this.interfacesToBeRemovedFirst + ")";
        }
    }

    private IfaceCreationData canIfaceComboSupportRequest(WifiChipInfo wifiChipInfo, IWifiChip.ChipMode chipMode, int[] iArr, int i, boolean z) {
        if (iArr[i] == 0) {
            return null;
        }
        int i2 = 0;
        if (wifiChipInfo.currentModeIdValid && wifiChipInfo.currentModeId != chipMode.id) {
            int[] iArr2 = IFACE_TYPES_BY_PRIORITY;
            int length = iArr2.length;
            while (i2 < length) {
                int i3 = iArr2[i2];
                if (wifiChipInfo.ifaces[i3].length != 0 && (z || !allowedToDeleteIfaceTypeForRequestedType(i3, i, wifiChipInfo.ifaces, wifiChipInfo.ifaces[i3].length))) {
                    return null;
                }
                i2++;
            }
            IfaceCreationData ifaceCreationData = new IfaceCreationData();
            ifaceCreationData.chipInfo = wifiChipInfo;
            ifaceCreationData.chipModeId = chipMode.id;
            return ifaceCreationData;
        }
        List<WifiIfaceInfo> arrayList = new ArrayList<>();
        int[] iArr3 = IFACE_TYPES_BY_PRIORITY;
        int length2 = iArr3.length;
        while (i2 < length2) {
            int i4 = iArr3[i2];
            int length3 = wifiChipInfo.ifaces[i4].length - iArr[i4];
            if (i4 == i) {
                length3++;
            }
            if (length3 > 0) {
                if (z || !allowedToDeleteIfaceTypeForRequestedType(i4, i, wifiChipInfo.ifaces, length3)) {
                    return null;
                }
                arrayList = selectInterfacesToDelete(length3, wifiChipInfo.ifaces[i4]);
            }
            i2++;
        }
        IfaceCreationData ifaceCreationData2 = new IfaceCreationData();
        ifaceCreationData2.chipInfo = wifiChipInfo;
        ifaceCreationData2.chipModeId = chipMode.id;
        ifaceCreationData2.interfacesToBeRemovedFirst = arrayList;
        return ifaceCreationData2;
    }

    private boolean compareIfaceCreationData(IfaceCreationData ifaceCreationData, IfaceCreationData ifaceCreationData2) {
        int size;
        int size2;
        if (ifaceCreationData == null) {
            return false;
        }
        if (ifaceCreationData2 == null) {
            return true;
        }
        for (int i : IFACE_TYPES_BY_PRIORITY) {
            if (ifaceCreationData.chipInfo.currentModeIdValid && ifaceCreationData.chipInfo.currentModeId != ifaceCreationData.chipModeId) {
                size = ifaceCreationData.chipInfo.ifaces[i].length;
            } else {
                size = ifaceCreationData.interfacesToBeRemovedFirst.size();
            }
            if (ifaceCreationData2.chipInfo.currentModeIdValid && ifaceCreationData2.chipInfo.currentModeId != ifaceCreationData2.chipModeId) {
                size2 = ifaceCreationData2.chipInfo.ifaces[i].length;
            } else {
                size2 = ifaceCreationData2.interfacesToBeRemovedFirst.size();
            }
            if (size < size2) {
                return true;
            }
        }
        return false;
    }

    private boolean allowedToDeleteIfaceTypeForRequestedType(int i, int i2, WifiIfaceInfo[][] wifiIfaceInfoArr, int i3) {
        int i4 = 0;
        for (InterfaceCacheEntry interfaceCacheEntry : this.mInterfaceInfoCache.values()) {
            if (interfaceCacheEntry.type == i && interfaceCacheEntry.isLowPriority) {
                i4++;
            }
        }
        if (i4 >= i3) {
            return true;
        }
        if (i == i2 || wifiIfaceInfoArr[i2].length != 0) {
            return false;
        }
        if (wifiIfaceInfoArr[i].length > 1) {
            return true;
        }
        if (i2 == 3) {
            return false;
        }
        return i2 != 2 || i == 3;
    }

    private List<WifiIfaceInfo> selectInterfacesToDelete(int i, WifiIfaceInfo[] wifiIfaceInfoArr) {
        boolean z;
        LongSparseArray longSparseArray = new LongSparseArray();
        LongSparseArray longSparseArray2 = new LongSparseArray();
        int length = wifiIfaceInfoArr.length;
        int i2 = 0;
        while (true) {
            if (i2 < length) {
                WifiIfaceInfo wifiIfaceInfo = wifiIfaceInfoArr[i2];
                InterfaceCacheEntry interfaceCacheEntry = this.mInterfaceInfoCache.get(Pair.create(wifiIfaceInfo.name, Integer.valueOf(getType(wifiIfaceInfo.iface))));
                if (interfaceCacheEntry == null) {
                    Log.e(TAG, "selectInterfacesToDelete: can't find cache entry with name=" + wifiIfaceInfo.name);
                    z = true;
                    break;
                }
                if (interfaceCacheEntry.isLowPriority) {
                    longSparseArray.append(interfaceCacheEntry.creationTime, wifiIfaceInfo);
                } else {
                    longSparseArray2.append(interfaceCacheEntry.creationTime, wifiIfaceInfo);
                }
                i2++;
            } else {
                z = false;
                break;
            }
        }
        if (z) {
            Log.e(TAG, "selectInterfacesToDelete: falling back to arbitrary selection");
            return Arrays.asList((WifiIfaceInfo[]) Arrays.copyOf(wifiIfaceInfoArr, i));
        }
        ArrayList arrayList = new ArrayList(i);
        for (int i3 = 0; i3 < i; i3++) {
            int size = (longSparseArray.size() - i3) - 1;
            if (size >= 0) {
                arrayList.add((WifiIfaceInfo) longSparseArray.valueAt(size));
            } else {
                arrayList.add((WifiIfaceInfo) longSparseArray2.valueAt(((longSparseArray2.size() - i3) + longSparseArray.size()) - 1));
            }
        }
        return arrayList;
    }

    private IWifiIface executeChipReconfiguration(IfaceCreationData ifaceCreationData, int i) {
        if (this.mDbg) {
            Log.d(TAG, "executeChipReconfiguration: ifaceCreationData=" + ifaceCreationData + ", ifaceType=" + i);
        }
        synchronized (this.mLock) {
            try {
                try {
                    boolean z = (ifaceCreationData.chipInfo.currentModeIdValid && ifaceCreationData.chipInfo.currentModeId == ifaceCreationData.chipModeId) ? false : true;
                    if (this.mDbg) {
                        Log.d(TAG, "isModeConfigNeeded=" + z);
                    }
                    if (z) {
                        for (WifiIfaceInfo[] wifiIfaceInfoArr : ifaceCreationData.chipInfo.ifaces) {
                            for (WifiIfaceInfo wifiIfaceInfo : wifiIfaceInfoArr) {
                                removeIfaceInternal(wifiIfaceInfo.iface);
                            }
                        }
                        WifiStatus wifiStatusConfigureChip = ifaceCreationData.chipInfo.chip.configureChip(ifaceCreationData.chipModeId);
                        if (wifiStatusConfigureChip.code != 0) {
                            Log.e(TAG, "executeChipReconfiguration: configureChip error: " + statusString(wifiStatusConfigureChip));
                            return null;
                        }
                    } else {
                        Iterator<WifiIfaceInfo> it = ifaceCreationData.interfacesToBeRemovedFirst.iterator();
                        while (it.hasNext()) {
                            removeIfaceInternal(it.next().iface);
                        }
                    }
                    final HidlSupport.Mutable mutable = new HidlSupport.Mutable();
                    final HidlSupport.Mutable mutable2 = new HidlSupport.Mutable();
                    switch (i) {
                        case 0:
                            ifaceCreationData.chipInfo.chip.createStaIface(new IWifiChip.createStaIfaceCallback() {
                                @Override
                                public final void onValues(WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
                                    HalDeviceManager.lambda$executeChipReconfiguration$19(mutable, mutable2, wifiStatus, iWifiStaIface);
                                }
                            });
                            break;
                        case 1:
                            ifaceCreationData.chipInfo.chip.createApIface(new IWifiChip.createApIfaceCallback() {
                                @Override
                                public final void onValues(WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
                                    HalDeviceManager.lambda$executeChipReconfiguration$20(mutable, mutable2, wifiStatus, iWifiApIface);
                                }
                            });
                            break;
                        case 2:
                            ifaceCreationData.chipInfo.chip.createP2pIface(new IWifiChip.createP2pIfaceCallback() {
                                @Override
                                public final void onValues(WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
                                    HalDeviceManager.lambda$executeChipReconfiguration$21(mutable, mutable2, wifiStatus, iWifiP2pIface);
                                }
                            });
                            break;
                        case 3:
                            ifaceCreationData.chipInfo.chip.createNanIface(new IWifiChip.createNanIfaceCallback() {
                                @Override
                                public final void onValues(WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
                                    HalDeviceManager.lambda$executeChipReconfiguration$22(mutable, mutable2, wifiStatus, iWifiNanIface);
                                }
                            });
                            break;
                    }
                    if (((WifiStatus) mutable.value).code != 0) {
                        Log.e(TAG, "executeChipReconfiguration: failed to create interface ifaceType=" + i + ": " + statusString((WifiStatus) mutable.value));
                        return null;
                    }
                    return (IWifiIface) mutable2.value;
                } catch (RemoteException e) {
                    Log.e(TAG, "executeChipReconfiguration exception: " + e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    static void lambda$executeChipReconfiguration$19(HidlSupport.Mutable mutable, HidlSupport.Mutable mutable2, WifiStatus wifiStatus, IWifiStaIface iWifiStaIface) {
        mutable.value = wifiStatus;
        mutable2.value = iWifiStaIface;
    }

    static void lambda$executeChipReconfiguration$20(HidlSupport.Mutable mutable, HidlSupport.Mutable mutable2, WifiStatus wifiStatus, IWifiApIface iWifiApIface) {
        mutable.value = wifiStatus;
        mutable2.value = iWifiApIface;
    }

    static void lambda$executeChipReconfiguration$21(HidlSupport.Mutable mutable, HidlSupport.Mutable mutable2, WifiStatus wifiStatus, IWifiP2pIface iWifiP2pIface) {
        mutable.value = wifiStatus;
        mutable2.value = iWifiP2pIface;
    }

    static void lambda$executeChipReconfiguration$22(HidlSupport.Mutable mutable, HidlSupport.Mutable mutable2, WifiStatus wifiStatus, IWifiNanIface iWifiNanIface) {
        mutable.value = wifiStatus;
        mutable2.value = iWifiNanIface;
    }

    private boolean removeIfaceInternal(IWifiIface iWifiIface) {
        WifiStatus wifiStatusRemoveStaIface;
        String name = getName(iWifiIface);
        int type = getType(iWifiIface);
        if (this.mDbg) {
            Log.d(TAG, "removeIfaceInternal: iface(name)=" + name + ", type=" + type);
        }
        if (type == -1) {
            Log.e(TAG, "removeIfaceInternal: can't get type -- iface(name)=" + name);
            return false;
        }
        synchronized (this.mLock) {
            if (this.mWifi == null) {
                Log.e(TAG, "removeIfaceInternal: null IWifi -- iface(name)=" + name);
                return false;
            }
            IWifiChip chip = getChip(iWifiIface);
            if (chip == null) {
                Log.e(TAG, "removeIfaceInternal: null IWifiChip -- iface(name)=" + name);
                return false;
            }
            if (name == null) {
                Log.e(TAG, "removeIfaceInternal: can't get name");
                return false;
            }
            WifiStatus wifiStatus = null;
            try {
                switch (type) {
                    case 0:
                        wifiStatusRemoveStaIface = chip.removeStaIface(name);
                        break;
                    case 1:
                        wifiStatusRemoveStaIface = chip.removeApIface(name);
                        break;
                    case 2:
                        wifiStatusRemoveStaIface = chip.removeP2pIface(name);
                        break;
                    case 3:
                        wifiStatusRemoveStaIface = chip.removeNanIface(name);
                        break;
                    default:
                        Log.wtf(TAG, "removeIfaceInternal: invalid type=" + type);
                        return false;
                }
                wifiStatus = wifiStatusRemoveStaIface;
            } catch (RemoteException e) {
                Log.e(TAG, "IWifiChip.removeXxxIface exception: " + e);
            }
            dispatchDestroyedListeners(name, type);
            if (wifiStatus != null && wifiStatus.code == 0) {
                return true;
            }
            Log.e(TAG, "IWifiChip.removeXxxIface failed: " + statusString(wifiStatus));
            return false;
        }
    }

    private boolean dispatchAvailableForRequestListeners() {
        synchronized (this.mLock) {
            WifiChipInfo[] allChipInfo = getAllChipInfo();
            if (allChipInfo == null) {
                Log.e(TAG, "dispatchAvailableForRequestListeners: no chip info found");
                stopWifi();
                return false;
            }
            for (int i : IFACE_TYPES_BY_PRIORITY) {
                dispatchAvailableForRequestListenersForType(i, allChipInfo);
            }
            return true;
        }
    }

    private void dispatchAvailableForRequestListenersForType(int i, WifiChipInfo[] wifiChipInfoArr) {
        synchronized (this.mLock) {
            Map<InterfaceAvailableForRequestListenerProxy, Boolean> map = this.mInterfaceAvailableForRequestListeners.get(i);
            if (map.size() == 0) {
                return;
            }
            boolean zIsItPossibleToCreateIface = isItPossibleToCreateIface(wifiChipInfoArr, i);
            for (Map.Entry<InterfaceAvailableForRequestListenerProxy, Boolean> entry : map.entrySet()) {
                if (entry.getValue() == null || entry.getValue().booleanValue() != zIsItPossibleToCreateIface) {
                    entry.getKey().triggerWithArg(zIsItPossibleToCreateIface);
                }
                entry.setValue(Boolean.valueOf(zIsItPossibleToCreateIface));
            }
        }
    }

    private void dispatchDestroyedListeners(String str, int i) {
        synchronized (this.mLock) {
            InterfaceCacheEntry interfaceCacheEntry = this.mInterfaceInfoCache.get(Pair.create(str, Integer.valueOf(i)));
            if (interfaceCacheEntry == null) {
                Log.e(TAG, "dispatchDestroyedListeners: no cache entry for iface(name)=" + str);
                return;
            }
            Iterator<InterfaceDestroyedListenerProxy> it = interfaceCacheEntry.destroyedListeners.iterator();
            while (it.hasNext()) {
                it.next().trigger();
            }
            interfaceCacheEntry.destroyedListeners.clear();
            this.mInterfaceInfoCache.remove(Pair.create(str, Integer.valueOf(i)));
        }
    }

    private void dispatchAllDestroyedListeners() {
        synchronized (this.mLock) {
            Iterator<Map.Entry<Pair<String, Integer>, InterfaceCacheEntry>> it = this.mInterfaceInfoCache.entrySet().iterator();
            while (it.hasNext()) {
                InterfaceCacheEntry value = it.next().getValue();
                Iterator<InterfaceDestroyedListenerProxy> it2 = value.destroyedListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().trigger();
                }
                value.destroyedListeners.clear();
                it.remove();
            }
        }
    }

    private abstract class ListenerProxy<LISTENER> {
        private Handler mHandler;
        protected LISTENER mListener;

        public boolean equals(Object obj) {
            return this.mListener == ((ListenerProxy) obj).mListener;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }

        void trigger() {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.action();
                    }
                });
            } else {
                action();
            }
        }

        void triggerWithArg(final boolean z) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.actionWithArg(z);
                    }
                });
            } else {
                actionWithArg(z);
            }
        }

        protected void action() {
        }

        protected void actionWithArg(boolean z) {
        }

        ListenerProxy(LISTENER listener, Handler handler, String str) {
            this.mListener = listener;
            this.mHandler = handler;
        }
    }

    private class InterfaceDestroyedListenerProxy extends ListenerProxy<InterfaceDestroyedListener> {
        private final String mIfaceName;

        InterfaceDestroyedListenerProxy(String str, InterfaceDestroyedListener interfaceDestroyedListener, Handler handler) {
            super(interfaceDestroyedListener, handler, "InterfaceDestroyedListenerProxy");
            this.mIfaceName = str;
        }

        @Override
        protected void action() {
            ((InterfaceDestroyedListener) this.mListener).onDestroyed(this.mIfaceName);
        }
    }

    private class InterfaceAvailableForRequestListenerProxy extends ListenerProxy<InterfaceAvailableForRequestListener> {
        InterfaceAvailableForRequestListenerProxy(InterfaceAvailableForRequestListener interfaceAvailableForRequestListener, Handler handler) {
            super(interfaceAvailableForRequestListener, handler, "InterfaceAvailableForRequestListenerProxy");
        }

        @Override
        protected void actionWithArg(boolean z) {
            ((InterfaceAvailableForRequestListener) this.mListener).onAvailabilityChanged(z);
        }
    }

    private static String statusString(WifiStatus wifiStatus) {
        if (wifiStatus == null) {
            return "status=null";
        }
        return wifiStatus.code + " (" + wifiStatus.description + ")";
    }

    private static int getType(IWifiIface iWifiIface) {
        final MutableInt mutableInt = new MutableInt(-1);
        try {
            iWifiIface.getType(new IWifiIface.getTypeCallback() {
                @Override
                public final void onValues(WifiStatus wifiStatus, int i) {
                    HalDeviceManager.lambda$getType$23(mutableInt, wifiStatus, i);
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Exception on getType: " + e);
        }
        return mutableInt.value;
    }

    static void lambda$getType$23(MutableInt mutableInt, WifiStatus wifiStatus, int i) {
        if (wifiStatus.code == 0) {
            mutableInt.value = i;
            return;
        }
        Log.e(TAG, "Error on getType: " + statusString(wifiStatus));
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HalDeviceManager:");
        printWriter.println("  mServiceManager: " + this.mServiceManager);
        printWriter.println("  mWifi: " + this.mWifi);
        printWriter.println("  mManagerStatusListeners: " + this.mManagerStatusListeners);
        printWriter.println("  mInterfaceAvailableForRequestListeners: " + this.mInterfaceAvailableForRequestListeners);
        printWriter.println("  mInterfaceInfoCache: " + this.mInterfaceInfoCache);
    }
}
