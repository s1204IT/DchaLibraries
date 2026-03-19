package com.android.bluetooth.gatt;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.bluetooth.le.ResultStorageDescriptor;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.gatt.HandleMap;
import com.android.bluetooth.util.NumberUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GattService extends ProfileService {
    private static final int ADVT_STATE_ONFOUND = 0;
    private static final int ADVT_STATE_ONLOST = 1;
    private static final int ET_LEGACY_MASK = 16;
    private static final int MAC_ADDRESS_LENGTH = 6;
    private static final int NUM_SCAN_EVENTS_KEPT = 20;
    static final int SCAN_FILTER_ENABLED = 1;
    static final int SCAN_FILTER_MODIFIED = 2;
    private static final String TAG = "BtGatt.GattService";
    private static final int TIME_STAMP_LENGTH = 2;
    private static final int TRUNCATED_RESULT_SIZE = 11;
    private static GattService sGattService;
    private BluetoothAdapter mAdapter;
    private AdvertiseManager mAdvertiseManager;
    private AppOpsManager mAppOps;
    private int mMaxScanFilters;
    private PeriodicScanManager mPeriodicScanManager;
    private ScanManager mScanManager;
    private static final boolean DBG = GattServiceConfig.DBG;
    private static final boolean VDBG = GattServiceConfig.VDBG;
    private static final UUID[] HID_UUIDS = {UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB"), UUID.fromString("00002A4B-0000-1000-8000-00805F9B34FB"), UUID.fromString("00002A4C-0000-1000-8000-00805F9B34FB"), UUID.fromString("00002A4D-0000-1000-8000-00805F9B34FB")};
    private static final UUID[] FIDO_UUIDS = {UUID.fromString("0000FFFD-0000-1000-8000-00805F9B34FB")};
    ScannerMap mScannerMap = new ScannerMap();
    ClientMap mClientMap = new ClientMap();
    ServerMap mServerMap = new ServerMap();
    HandleMap mHandleMap = new HandleMap();
    private List<UUID> mAdvertisingServiceUuids = new ArrayList();
    private final ArrayList<BluetoothMetricsProto.ScanEvent> mScanEvents = new ArrayList<>(20);
    private final Map<Integer, List<BluetoothGattService>> mGattClientDatabases = new HashMap();
    private Set<String> mReliableQueue = new HashSet();

    private static native void classInitNative();

    private native void cleanupNative();

    private native void gattClientConfigureMTUNative(int i, int i2);

    private native void gattClientConnectNative(int i, String str, boolean z, int i2, boolean z2, int i3);

    private native void gattClientDisconnectNative(int i, String str, int i2);

    private native void gattClientDiscoverServiceByUuidNative(int i, long j, long j2);

    private native void gattClientExecuteWriteNative(int i, boolean z);

    private native int gattClientGetDeviceTypeNative(String str);

    private native void gattClientGetGattDbNative(int i);

    private native void gattClientReadCharacteristicNative(int i, int i2, int i3);

    private native void gattClientReadDescriptorNative(int i, int i2, int i3);

    private native void gattClientReadPhyNative(int i, String str);

    private native void gattClientReadRemoteRssiNative(int i, String str);

    private native void gattClientReadUsingCharacteristicUuidNative(int i, long j, long j2, int i2, int i3, int i4);

    private native void gattClientRefreshNative(int i, String str);

    private native void gattClientRegisterAppNative(long j, long j2);

    private native void gattClientRegisterForNotificationsNative(int i, String str, int i2, boolean z);

    private native void gattClientSearchServiceNative(int i, boolean z, long j, long j2);

    private native void gattClientSetPreferredPhyNative(int i, String str, int i2, int i3, int i4);

    private native void gattClientUnregisterAppNative(int i);

    private native void gattClientWriteCharacteristicNative(int i, int i2, int i3, int i4, byte[] bArr);

    private native void gattClientWriteDescriptorNative(int i, int i2, int i3, byte[] bArr);

    private native void gattConnectionParameterUpdateNative(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7);

    private native void gattServerAddServiceNative(int i, List<GattDbElement> list);

    private native void gattServerConnectNative(int i, String str, boolean z, int i2);

    private native void gattServerDeleteServiceNative(int i, int i2);

    private native void gattServerDisconnectNative(int i, String str, int i2);

    private native void gattServerReadPhyNative(int i, String str);

    private native void gattServerRegisterAppNative(long j, long j2);

    private native void gattServerSendIndicationNative(int i, int i2, int i3, byte[] bArr);

    private native void gattServerSendNotificationNative(int i, int i2, int i3, byte[] bArr);

    private native void gattServerSendResponseNative(int i, int i2, int i3, int i4, int i5, int i6, byte[] bArr, int i7);

    private native void gattServerSetPreferredPhyNative(int i, String str, int i2, int i3, int i4);

    private native void gattServerStopServiceNative(int i, int i2);

    private native void gattServerUnregisterAppNative(int i);

    private native void gattTestNative(int i, long j, long j2, String str, int i2, int i3, int i4, int i5, int i6);

    private native void initializeNative();

    static {
        classInitNative();
    }

    class PendingIntentInfo {
        public String callingPackage;
        public List<ScanFilter> filters;
        public PendingIntent intent;
        public ScanSettings settings;

        PendingIntentInfo() {
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PendingIntentInfo)) {
                return false;
            }
            return this.intent.equals(obj.intent);
        }
    }

    class ScannerMap extends ContextMap<IScannerCallback, PendingIntentInfo> {
        ScannerMap() {
        }
    }

    class ClientMap extends ContextMap<IBluetoothGattCallback, Void> {
        ClientMap() {
        }
    }

    class ServerMap extends ContextMap<IBluetoothGattServerCallback, Void> {
        ServerMap() {
        }
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothGattBinder(this);
    }

    @Override
    protected boolean start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }
        initializeNative();
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mAppOps = (AppOpsManager) getSystemService(AppOpsManager.class);
        this.mAdvertiseManager = new AdvertiseManager(this, AdapterService.getAdapterService());
        this.mAdvertiseManager.start();
        this.mScanManager = new ScanManager(this);
        this.mScanManager.start();
        this.mPeriodicScanManager = new PeriodicScanManager(AdapterService.getAdapterService());
        this.mPeriodicScanManager.start();
        setGattService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        setGattService(null);
        this.mScannerMap.clear();
        this.mClientMap.clear();
        this.mServerMap.clear();
        this.mHandleMap.clear();
        this.mReliableQueue.clear();
        if (this.mAdvertiseManager != null) {
            this.mAdvertiseManager.cleanup();
        }
        if (this.mScanManager != null) {
            this.mScanManager.cleanup();
        }
        if (this.mPeriodicScanManager != null) {
            this.mPeriodicScanManager.cleanup();
            return true;
        }
        return true;
    }

    @Override
    protected void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }
        cleanupNative();
        if (this.mAdvertiseManager != null) {
            this.mAdvertiseManager.cleanup();
        }
        if (this.mScanManager != null) {
            this.mScanManager.cleanup();
        }
        if (this.mPeriodicScanManager != null) {
            this.mPeriodicScanManager.cleanup();
        }
    }

    @VisibleForTesting
    public static synchronized GattService getGattService() {
        if (sGattService == null) {
            Log.w(TAG, "getGattService(): service is null");
            return null;
        }
        if (!sGattService.isAvailable()) {
            Log.w(TAG, "getGattService(): service is not available");
            return null;
        }
        return sGattService;
    }

    private static synchronized void setGattService(GattService gattService) {
        if (DBG) {
            Log.d(TAG, "setGattService(): set to: " + gattService);
        }
        sGattService = gattService;
    }

    boolean permissionCheck(UUID uuid) {
        return !isRestrictedCharUuid(uuid) || checkCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == 0;
    }

    boolean permissionCheck(int i, int i2) {
        List<BluetoothGattService> list = this.mGattClientDatabases.get(Integer.valueOf(i));
        if (list == null) {
            return true;
        }
        for (BluetoothGattService bluetoothGattService : list) {
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                if (i2 == bluetoothGattCharacteristic.getInstanceId()) {
                    return !(isRestrictedCharUuid(bluetoothGattCharacteristic.getUuid()) || isRestrictedSrvcUuid(bluetoothGattService.getUuid())) || checkCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == 0;
                }
                Iterator<BluetoothGattDescriptor> it = bluetoothGattCharacteristic.getDescriptors().iterator();
                while (it.hasNext()) {
                    if (i2 == it.next().getInstanceId()) {
                        return !(isRestrictedCharUuid(bluetoothGattCharacteristic.getUuid()) || isRestrictedSrvcUuid(bluetoothGattService.getUuid())) || checkCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") == 0;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        if (GattDebugUtils.handleDebugAction(this, intent)) {
            return 2;
        }
        return super.onStartCommand(intent, i, i2);
    }

    class ScannerDeathRecipient implements IBinder.DeathRecipient {
        int mScannerId;

        ScannerDeathRecipient(int i) {
            this.mScannerId = i;
        }

        @Override
        public void binderDied() {
            if (GattService.DBG) {
                Log.d(GattService.TAG, "Binder is dead - unregistering scanner (" + this.mScannerId + ")!");
            }
            if (isScanClient(this.mScannerId)) {
                ScanClient scanClient = new ScanClient(this.mScannerId);
                scanClient.appDied = true;
                GattService.this.stopScan(scanClient);
            }
        }

        private boolean isScanClient(int i) {
            Iterator<ScanClient> it = GattService.this.mScanManager.getRegularScanQueue().iterator();
            while (it.hasNext()) {
                if (it.next().scannerId == i) {
                    return true;
                }
            }
            Iterator<ScanClient> it2 = GattService.this.mScanManager.getBatchScanQueue().iterator();
            while (it2.hasNext()) {
                if (it2.next().scannerId == i) {
                    return true;
                }
            }
            return false;
        }
    }

    class ServerDeathRecipient implements IBinder.DeathRecipient {
        int mAppIf;

        ServerDeathRecipient(int i) {
            this.mAppIf = i;
        }

        @Override
        public void binderDied() {
            if (GattService.DBG) {
                Log.d(GattService.TAG, "Binder is dead - unregistering server (" + this.mAppIf + ")!");
            }
            GattService.this.unregisterServer(this.mAppIf);
        }
    }

    class ClientDeathRecipient implements IBinder.DeathRecipient {
        int mAppIf;

        ClientDeathRecipient(int i) {
            this.mAppIf = i;
        }

        @Override
        public void binderDied() {
            if (GattService.DBG) {
                Log.d(GattService.TAG, "Binder is dead - unregistering client (" + this.mAppIf + ")!");
            }
            GattService.this.unregisterClient(this.mAppIf);
        }
    }

    private static class BluetoothGattBinder extends IBluetoothGatt.Stub implements ProfileService.IProfileServiceBinder {
        private GattService mService;

        BluetoothGattBinder(GattService gattService) {
            this.mService = gattService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private GattService getService() {
            if (this.mService != null && this.mService.isAvailable()) {
                return this.mService;
            }
            Log.e(GattService.TAG, "getService() - Service requested, but not available!");
            return null;
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            GattService service = getService();
            if (service == null) {
                return new ArrayList();
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public void registerClient(ParcelUuid parcelUuid, IBluetoothGattCallback iBluetoothGattCallback) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.registerClient(parcelUuid.getUuid(), iBluetoothGattCallback);
        }

        public void unregisterClient(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.unregisterClient(i);
        }

        public void registerScanner(IScannerCallback iScannerCallback, WorkSource workSource) throws RemoteException {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.registerScanner(iScannerCallback, workSource);
        }

        public void unregisterScanner(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.unregisterScanner(i);
        }

        public void startScan(int i, ScanSettings scanSettings, List<ScanFilter> list, List list2, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.startScan(i, scanSettings, list, list2, str);
        }

        public void startScanForIntent(PendingIntent pendingIntent, ScanSettings scanSettings, List<ScanFilter> list, String str) throws RemoteException {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.registerPiAndStartScan(pendingIntent, scanSettings, list, str);
        }

        public void stopScanForIntent(PendingIntent pendingIntent, String str) throws RemoteException {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.stopScan(pendingIntent, str);
        }

        public void stopScan(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.stopScan(new ScanClient(i));
        }

        public void flushPendingBatchResults(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.flushPendingBatchResults(i);
        }

        public void clientConnect(int i, String str, boolean z, int i2, boolean z2, int i3) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.clientConnect(i, str, z, i2, z2, i3);
        }

        public void clientDisconnect(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.clientDisconnect(i, str);
        }

        public void clientSetPreferredPhy(int i, String str, int i2, int i3, int i4) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.clientSetPreferredPhy(i, str, i2, i3, i4);
        }

        public void clientReadPhy(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.clientReadPhy(i, str);
        }

        public void refreshDevice(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.refreshDevice(i, str);
        }

        public void discoverServices(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.discoverServices(i, str);
        }

        public void discoverServiceByUuid(int i, String str, ParcelUuid parcelUuid) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.discoverServiceByUuid(i, str, parcelUuid.getUuid());
        }

        public void readCharacteristic(int i, String str, int i2, int i3) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.readCharacteristic(i, str, i2, i3);
        }

        public void readUsingCharacteristicUuid(int i, String str, ParcelUuid parcelUuid, int i2, int i3, int i4) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.readUsingCharacteristicUuid(i, str, parcelUuid.getUuid(), i2, i3, i4);
        }

        public void writeCharacteristic(int i, String str, int i2, int i3, int i4, byte[] bArr) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.writeCharacteristic(i, str, i2, i3, i4, bArr);
        }

        public void readDescriptor(int i, String str, int i2, int i3) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.readDescriptor(i, str, i2, i3);
        }

        public void writeDescriptor(int i, String str, int i2, int i3, byte[] bArr) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.writeDescriptor(i, str, i2, i3, bArr);
        }

        public void beginReliableWrite(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.beginReliableWrite(i, str);
        }

        public void endReliableWrite(int i, String str, boolean z) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.endReliableWrite(i, str, z);
        }

        public void registerForNotification(int i, String str, int i2, boolean z) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.registerForNotification(i, str, i2, z);
        }

        public void readRemoteRssi(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.readRemoteRssi(i, str);
        }

        public void configureMTU(int i, String str, int i2) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.configureMTU(i, str, i2);
        }

        public void connectionParameterUpdate(int i, String str, int i2) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.connectionParameterUpdate(i, str, i2);
        }

        public void leConnectionUpdate(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.leConnectionUpdate(i, str, i2, i3, i4, i5, i6, i7);
        }

        public void registerServer(ParcelUuid parcelUuid, IBluetoothGattServerCallback iBluetoothGattServerCallback) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.registerServer(parcelUuid.getUuid(), iBluetoothGattServerCallback);
        }

        public void unregisterServer(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.unregisterServer(i);
        }

        public void serverConnect(int i, String str, boolean z, int i2) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.serverConnect(i, str, z, i2);
        }

        public void serverDisconnect(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.serverDisconnect(i, str);
        }

        public void serverSetPreferredPhy(int i, String str, int i2, int i3, int i4) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.serverSetPreferredPhy(i, str, i2, i3, i4);
        }

        public void serverReadPhy(int i, String str) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.serverReadPhy(i, str);
        }

        public void addService(int i, BluetoothGattService bluetoothGattService) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.addService(i, bluetoothGattService);
        }

        public void removeService(int i, int i2) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.removeService(i, i2);
        }

        public void clearServices(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.clearServices(i);
        }

        public void sendResponse(int i, String str, int i2, int i3, int i4, byte[] bArr) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.sendResponse(i, str, i2, i3, i4, bArr);
        }

        public void sendNotification(int i, String str, int i2, boolean z, byte[] bArr) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.sendNotification(i, str, i2, z, bArr);
        }

        public void startAdvertisingSet(AdvertisingSetParameters advertisingSetParameters, AdvertiseData advertiseData, AdvertiseData advertiseData2, PeriodicAdvertisingParameters periodicAdvertisingParameters, AdvertiseData advertiseData3, int i, int i2, IAdvertisingSetCallback iAdvertisingSetCallback) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.startAdvertisingSet(advertisingSetParameters, advertiseData, advertiseData2, periodicAdvertisingParameters, advertiseData3, i, i2, iAdvertisingSetCallback);
        }

        public void stopAdvertisingSet(IAdvertisingSetCallback iAdvertisingSetCallback) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.stopAdvertisingSet(iAdvertisingSetCallback);
        }

        public void getOwnAddress(int i) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.getOwnAddress(i);
        }

        public void enableAdvertisingSet(int i, boolean z, int i2, int i3) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.enableAdvertisingSet(i, z, i2, i3);
        }

        public void setAdvertisingData(int i, AdvertiseData advertiseData) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.setAdvertisingData(i, advertiseData);
        }

        public void setScanResponseData(int i, AdvertiseData advertiseData) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.setScanResponseData(i, advertiseData);
        }

        public void setAdvertisingParameters(int i, AdvertisingSetParameters advertisingSetParameters) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.setAdvertisingParameters(i, advertisingSetParameters);
        }

        public void setPeriodicAdvertisingParameters(int i, PeriodicAdvertisingParameters periodicAdvertisingParameters) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.setPeriodicAdvertisingParameters(i, periodicAdvertisingParameters);
        }

        public void setPeriodicAdvertisingData(int i, AdvertiseData advertiseData) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.setPeriodicAdvertisingData(i, advertiseData);
        }

        public void setPeriodicAdvertisingEnable(int i, boolean z) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.setPeriodicAdvertisingEnable(i, z);
        }

        public void registerSync(ScanResult scanResult, int i, int i2, IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.registerSync(scanResult, i, i2, iPeriodicAdvertisingCallback);
        }

        public void unregisterSync(IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.unregisterSync(iPeriodicAdvertisingCallback);
        }

        public void disconnectAll() {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.disconnectAll();
        }

        public void unregAll() {
            GattService service = getService();
            if (service == null) {
                return;
            }
            service.unregAll();
        }

        public int numHwTrackFiltersAvailable() {
            GattService service = getService();
            if (service == null) {
                return 0;
            }
            return service.numHwTrackFiltersAvailable();
        }
    }

    void onScanResult(int i, int i2, String str, int i3, int i4, int i5, int i6, int i7, int i8, byte[] bArr) {
        int i9;
        int i10;
        int i11;
        byte[] bArr2;
        UUID[] uuidArr;
        String str2 = str;
        byte[] bArr3 = bArr;
        if (VDBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onScanResult() - eventType=0x");
            sb.append(Integer.toHexString(i));
            sb.append(", addressType=");
            sb.append(i2);
            sb.append(", address=");
            sb.append(str2);
            sb.append(", primaryPhy=");
            i9 = i3;
            sb.append(i9);
            sb.append(", secondaryPhy=");
            i10 = i4;
            sb.append(i10);
            sb.append(", advertisingSid=0x");
            sb.append(Integer.toHexString(i5));
            sb.append(", txPower=");
            i11 = i6;
            sb.append(i11);
            sb.append(", rssi=");
            sb.append(i7);
            sb.append(", periodicAdvInt=0x");
            sb.append(Integer.toHexString(i8));
            Log.d(TAG, sb.toString());
        } else {
            i9 = i3;
            i10 = i4;
            i11 = i6;
        }
        List<UUID> uuids = parseUuids(bArr3);
        int i12 = 0;
        byte[] bArrCopyOfRange = Arrays.copyOfRange(bArr3, 0, 62);
        Iterator<ScanClient> it = this.mScanManager.getRegularScanQueue().iterator();
        while (it.hasNext()) {
            ScanClient next = it.next();
            if (next.uuids.length > 0) {
                UUID[] uuidArr2 = next.uuids;
                int length = uuidArr2.length;
                int i13 = i12;
                int i14 = i13;
                while (i13 < length) {
                    UUID uuid = uuidArr2[i13];
                    Iterator<UUID> it2 = uuids.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            uuidArr = uuidArr2;
                            break;
                        }
                        uuidArr = uuidArr2;
                        if (!it2.next().equals(uuid)) {
                            uuidArr2 = uuidArr;
                        } else {
                            i14++;
                            break;
                        }
                    }
                    i13++;
                    uuidArr2 = uuidArr;
                }
                if (i14 >= next.uuids.length) {
                    ContextMap<IScannerCallback, PendingIntentInfo>.App byId = this.mScannerMap.getById(next.scannerId);
                    if (byId != null) {
                        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(str2);
                        ScanSettings scanSettings = next.settings;
                        if (scanSettings.getLegacy()) {
                            if ((i & 16) != 0) {
                                bArr2 = bArrCopyOfRange;
                            }
                        } else {
                            bArr2 = bArr3;
                        }
                        Iterator<ScanClient> it3 = it;
                        List<UUID> list = uuids;
                        ScanResult scanResult = new ScanResult(remoteDevice, i, i9, i10, i5, i11, i7, i8, ScanRecord.parseFromBytes(bArr2), SystemClock.elapsedRealtimeNanos());
                        if (hasScanResultPermission(next) && matchesFilters(next, scanResult) && (scanSettings.getCallbackType() & 1) != 0) {
                            try {
                                byId.appScanStats.addResult(next.scannerId);
                                if (byId.callback != null) {
                                    byId.callback.onScanResult(scanResult);
                                } else {
                                    ArrayList<ScanResult> arrayList = new ArrayList<>();
                                    arrayList.add(scanResult);
                                    sendResultsByPendingIntent(byId.info, arrayList, 1);
                                }
                            } catch (PendingIntent.CanceledException | RemoteException e) {
                                Log.e(TAG, "Exception: " + e);
                                this.mScannerMap.remove(next.scannerId);
                                this.mScanManager.stopScan(next);
                            }
                        }
                        i10 = i4;
                        i11 = i6;
                        i12 = 0;
                        it = it3;
                        uuids = list;
                        str2 = str;
                        bArr3 = bArr;
                    }
                }
                i12 = 0;
            }
        }
    }

    private void sendResultByPendingIntent(PendingIntentInfo pendingIntentInfo, ScanResult scanResult, int i, ScanClient scanClient) {
        ArrayList<ScanResult> arrayList = new ArrayList<>();
        arrayList.add(scanResult);
        try {
            sendResultsByPendingIntent(pendingIntentInfo, arrayList, i);
        } catch (PendingIntent.CanceledException e) {
            stopScan(scanClient);
            unregisterScanner(scanClient.scannerId);
        }
    }

    private void sendResultsByPendingIntent(PendingIntentInfo pendingIntentInfo, ArrayList<ScanResult> arrayList, int i) throws PendingIntent.CanceledException {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra("android.bluetooth.le.extra.LIST_SCAN_RESULT", arrayList);
        intent.putExtra("android.bluetooth.le.extra.CALLBACK_TYPE", i);
        pendingIntentInfo.intent.send(this, 0, intent);
    }

    private void sendErrorByPendingIntent(PendingIntentInfo pendingIntentInfo, int i) throws PendingIntent.CanceledException {
        Intent intent = new Intent();
        intent.putExtra("android.bluetooth.le.extra.ERROR_CODE", i);
        pendingIntentInfo.intent.send(this, 0, intent);
    }

    void onScannerRegistered(int i, int i2, long j, long j2) throws RemoteException {
        UUID uuid = new UUID(j2, j);
        if (DBG) {
            Log.d(TAG, "onScannerRegistered() - UUID=" + uuid + ", scannerId=" + i2 + ", status=" + i);
        }
        ContextMap<IScannerCallback, PendingIntentInfo>.App byUuid = this.mScannerMap.getByUuid(uuid);
        if (byUuid != null) {
            if (i == 0) {
                byUuid.id = i2;
                if (byUuid.callback != null) {
                    byUuid.linkToDeath(new ScannerDeathRecipient(i2));
                } else {
                    continuePiStartScan(i2, byUuid);
                }
            } else {
                this.mScannerMap.remove(i2);
            }
            if (byUuid.callback != null) {
                byUuid.callback.onScannerRegistered(i, i2);
            }
        }
    }

    private boolean hasScanResultPermission(ScanClient scanClient) {
        boolean z = !getResources().getBoolean(R.bool.strict_location_check) || (Settings.Secure.getInt(getContentResolver(), "location_mode", 0) != 0) || scanClient.legacyForegroundApp;
        if (scanClient.hasPeersMacAddressPermission) {
            return true;
        }
        return scanClient.hasLocationPermission && z;
    }

    private boolean matchesFilters(ScanClient scanClient, ScanResult scanResult) {
        if (scanClient.filters == null || scanClient.filters.isEmpty()) {
            return true;
        }
        Iterator<ScanFilter> it = scanClient.filters.iterator();
        while (it.hasNext()) {
            if (it.next().matches(scanResult)) {
                return true;
            }
        }
        return false;
    }

    void onClientRegistered(int i, int i2, long j, long j2) throws RemoteException {
        UUID uuid = new UUID(j2, j);
        if (DBG) {
            Log.d(TAG, "onClientRegistered() - UUID=" + uuid + ", clientIf=" + i2);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byUuid = this.mClientMap.getByUuid(uuid);
        if (byUuid != null) {
            if (i == 0) {
                byUuid.id = i2;
                byUuid.linkToDeath(new ClientDeathRecipient(i2));
            } else {
                this.mClientMap.remove(uuid);
            }
            byUuid.callback.onClientRegistered(i, i2);
        }
    }

    void onConnected(int i, int i2, int i3, String str) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onConnected() - clientIf=" + i + ", connId=" + i2 + ", address=" + str);
        }
        if (i3 == 0) {
            this.mClientMap.addConnection(i, i2, str);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byId = this.mClientMap.getById(i);
        if (byId != null) {
            byId.callback.onClientConnectionState(i3, i, i3 == 0, str);
        }
    }

    void onDisconnected(int i, int i2, int i3, String str) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onDisconnected() - clientIf=" + i + ", connId=" + i2 + ", address=" + str);
        }
        this.mClientMap.removeConnection(i, i2);
        ContextMap<IBluetoothGattCallback, Void>.App byId = this.mClientMap.getById(i);
        if (byId != null) {
            byId.callback.onClientConnectionState(i3, i, false, str);
        }
    }

    void onClientPhyUpdate(int i, int i2, int i3, int i4) throws RemoteException {
        ContextMap<IBluetoothGattCallback, Void>.App byConnId;
        if (DBG) {
            Log.d(TAG, "onClientPhyUpdate() - connId=" + i + ", status=" + i4);
        }
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (strAddressByConnId == null || (byConnId = this.mClientMap.getByConnId(i)) == null) {
            return;
        }
        byConnId.callback.onPhyUpdate(strAddressByConnId, i2, i3, i4);
    }

    void onClientPhyRead(int i, String str, int i2, int i3, int i4) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onClientPhyRead() - address=" + str + ", status=" + i4 + ", clientIf=" + i);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.d(TAG, "onClientPhyRead() - no connection to " + str);
            return;
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(numConnIdByAddress.intValue());
        if (byConnId == null) {
            return;
        }
        byConnId.callback.onPhyRead(str, i2, i3, i4);
    }

    void onClientConnUpdate(int i, int i2, int i3, int i4, int i5) throws RemoteException {
        ContextMap<IBluetoothGattCallback, Void>.App byConnId;
        if (DBG) {
            Log.d(TAG, "onClientConnUpdate() - connId=" + i + ", status=" + i5);
        }
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (strAddressByConnId == null || (byConnId = this.mClientMap.getByConnId(i)) == null) {
            return;
        }
        byConnId.callback.onConnectionUpdated(strAddressByConnId, i2, i3, i4, i5);
    }

    void onServerPhyUpdate(int i, int i2, int i3, int i4) throws RemoteException {
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId;
        if (DBG) {
            Log.d(TAG, "onServerPhyUpdate() - connId=" + i + ", status=" + i4);
        }
        String strAddressByConnId = this.mServerMap.addressByConnId(i);
        if (strAddressByConnId == null || (byConnId = this.mServerMap.getByConnId(i)) == null) {
            return;
        }
        byConnId.callback.onPhyUpdate(strAddressByConnId, i2, i3, i4);
    }

    void onServerPhyRead(int i, String str, int i2, int i3, int i4) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onServerPhyRead() - address=" + str + ", status=" + i4);
        }
        Integer numConnIdByAddress = this.mServerMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.d(TAG, "onServerPhyRead() - no connection to " + str);
            return;
        }
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId = this.mServerMap.getByConnId(numConnIdByAddress.intValue());
        if (byConnId == null) {
            return;
        }
        byConnId.callback.onPhyRead(str, i2, i3, i4);
    }

    void onServerConnUpdate(int i, int i2, int i3, int i4, int i5) throws RemoteException {
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId;
        if (DBG) {
            Log.d(TAG, "onServerConnUpdate() - connId=" + i + ", status=" + i5);
        }
        String strAddressByConnId = this.mServerMap.addressByConnId(i);
        if (strAddressByConnId == null || (byConnId = this.mServerMap.getByConnId(i)) == null) {
            return;
        }
        byConnId.callback.onConnectionUpdated(strAddressByConnId, i2, i3, i4, i5);
    }

    void onSearchCompleted(final int i, int i2) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onSearchCompleted() - connId=" + i + ", status=" + i2);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                GattService.this.gattClientGetGattDbNative(i);
            }
        }).start();
    }

    GattDbElement getSampleGattDbElement() {
        return new GattDbElement();
    }

    void onGetGattDb(int i, ArrayList<GattDbElement> arrayList) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (DBG) {
            Log.d(TAG, "onGetGattDb() - address=" + strAddressByConnId);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId == null || byConnId.callback == null) {
            Log.e(TAG, "app or callback is null");
            return;
        }
        ArrayList arrayList2 = new ArrayList();
        BluetoothGattService bluetoothGattService = null;
        BluetoothGattCharacteristic bluetoothGattCharacteristic = null;
        for (GattDbElement gattDbElement : arrayList) {
            switch (gattDbElement.type) {
                case 0:
                case 1:
                    if (DBG) {
                        Log.d(TAG, "got service with UUID=" + gattDbElement.uuid + " id: " + gattDbElement.id);
                    }
                    bluetoothGattService = new BluetoothGattService(gattDbElement.uuid, gattDbElement.id, gattDbElement.type);
                    arrayList2.add(bluetoothGattService);
                    break;
                case 2:
                    if (DBG) {
                        Log.d(TAG, "got included service with UUID=" + gattDbElement.uuid + " id: " + gattDbElement.id + " startHandle: " + gattDbElement.startHandle);
                    }
                    bluetoothGattService.addIncludedService(new BluetoothGattService(gattDbElement.uuid, gattDbElement.startHandle, gattDbElement.type));
                    break;
                case 3:
                    if (DBG) {
                        Log.d(TAG, "got characteristic with UUID=" + gattDbElement.uuid + " id: " + gattDbElement.id);
                    }
                    bluetoothGattCharacteristic = new BluetoothGattCharacteristic(gattDbElement.uuid, gattDbElement.id, gattDbElement.properties, 0);
                    bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);
                    break;
                case 4:
                    if (DBG) {
                        Log.d(TAG, "got descriptor with UUID=" + gattDbElement.uuid + " id: " + gattDbElement.id);
                    }
                    bluetoothGattCharacteristic.addDescriptor(new BluetoothGattDescriptor(gattDbElement.uuid, gattDbElement.id, 0));
                    break;
                default:
                    Log.e(TAG, "got unknown element with type=" + gattDbElement.type + " and UUID=" + gattDbElement.uuid + " id: " + gattDbElement.id);
                    break;
            }
        }
        this.mGattClientDatabases.put(Integer.valueOf(i), arrayList2);
        byConnId.callback.onSearchComplete(strAddressByConnId, arrayList2, 0);
    }

    void onRegisterForNotifications(int i, int i2, int i3, int i4) {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (DBG) {
            Log.d(TAG, "onRegisterForNotifications() - address=" + strAddressByConnId + ", status=" + i2 + ", registered=" + i3 + ", handle=" + i4);
        }
    }

    void onNotify(int i, String str, int i2, boolean z, byte[] bArr) throws RemoteException {
        if (VDBG) {
            Log.d(TAG, "onNotify() - address=" + str + ", handle=" + i2 + ", length=" + bArr.length);
        }
        if (!permissionCheck(i, i2)) {
            Log.w(TAG, "onNotify() - permission check failed!");
            return;
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.callback.onNotify(str, i2, bArr);
        }
    }

    void onReadCharacteristic(int i, int i2, int i3, byte[] bArr) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (VDBG) {
            Log.d(TAG, "onReadCharacteristic() - address=" + strAddressByConnId + ", status=" + i2 + ", length=" + bArr.length);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.callback.onCharacteristicRead(strAddressByConnId, i2, i3, bArr);
        }
    }

    void onWriteCharacteristic(int i, int i2, int i3) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (VDBG) {
            Log.d(TAG, "onWriteCharacteristic() - address=" + strAddressByConnId + ", status=" + i2);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId == null) {
            return;
        }
        if (!byConnId.isCongested.booleanValue()) {
            byConnId.callback.onCharacteristicWrite(strAddressByConnId, i2, i3);
            return;
        }
        if (i2 == 143) {
            i2 = 0;
        }
        byConnId.queueCallback(new CallbackInfo(strAddressByConnId, i2, i3));
    }

    void onExecuteCompleted(int i, int i2) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (VDBG) {
            Log.d(TAG, "onExecuteCompleted() - address=" + strAddressByConnId + ", status=" + i2);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.callback.onExecuteWrite(strAddressByConnId, i2);
        }
    }

    void onReadDescriptor(int i, int i2, int i3, byte[] bArr) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (VDBG) {
            Log.d(TAG, "onReadDescriptor() - address=" + strAddressByConnId + ", status=" + i2 + ", length=" + bArr.length);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.callback.onDescriptorRead(strAddressByConnId, i2, i3, bArr);
        }
    }

    void onWriteDescriptor(int i, int i2, int i3) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (VDBG) {
            Log.d(TAG, "onWriteDescriptor() - address=" + strAddressByConnId + ", status=" + i2);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.callback.onDescriptorWrite(strAddressByConnId, i2, i3);
        }
    }

    void onReadRemoteRssi(int i, String str, int i2, int i3) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onReadRemoteRssi() - clientIf=" + i + " address=" + str + ", rssi=" + i2 + ", status=" + i3);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byId = this.mClientMap.getById(i);
        if (byId != null) {
            byId.callback.onReadRemoteRssi(str, i2, i3);
        }
    }

    void onScanFilterEnableDisabled(int i, int i2, int i3) {
        if (DBG) {
            Log.d(TAG, "onScanFilterEnableDisabled() - clientIf=" + i3 + ", status=" + i2 + ", action=" + i);
        }
        this.mScanManager.callbackDone(i3, i2);
    }

    void onScanFilterParamsConfigured(int i, int i2, int i3, int i4) {
        if (DBG) {
            Log.d(TAG, "onScanFilterParamsConfigured() - clientIf=" + i3 + ", status=" + i2 + ", action=" + i + ", availableSpace=" + i4);
        }
        this.mScanManager.callbackDone(i3, i2);
    }

    void onScanFilterConfig(int i, int i2, int i3, int i4, int i5) {
        if (DBG) {
            Log.d(TAG, "onScanFilterConfig() - clientIf=" + i3 + ", action = " + i + " status = " + i2 + ", filterType=" + i4 + ", availableSpace=" + i5);
        }
        this.mScanManager.callbackDone(i3, i2);
    }

    void onBatchScanStorageConfigured(int i, int i2) {
        if (DBG) {
            Log.d(TAG, "onBatchScanStorageConfigured() - clientIf=" + i2 + ", status=" + i);
        }
        this.mScanManager.callbackDone(i2, i);
    }

    void onBatchScanStartStopped(int i, int i2, int i3) {
        if (DBG) {
            Log.d(TAG, "onBatchScanStartStopped() - clientIf=" + i3 + ", status=" + i2 + ", startStopAction=" + i);
        }
        this.mScanManager.callbackDone(i3, i2);
    }

    ScanClient findBatchScanClientById(int i) {
        for (ScanClient scanClient : this.mScanManager.getBatchScanQueue()) {
            if (scanClient.scannerId == i) {
                return scanClient;
            }
        }
        return null;
    }

    void onBatchScanReports(int i, int i2, int i3, int i4, byte[] bArr) throws RemoteException {
        ScanClient scanClientFindBatchScanClientById;
        if (DBG) {
            Log.d(TAG, "onBatchScanReports() - scannerId=" + i2 + ", status=" + i + ", reportType=" + i3 + ", numRecords=" + i4);
        }
        this.mScanManager.callbackDone(i2, i);
        Set<ScanResult> batchScanResults = parseBatchScanResults(i4, i3, bArr);
        if (i3 == 1) {
            ContextMap<IScannerCallback, PendingIntentInfo>.App byId = this.mScannerMap.getById(i2);
            if (byId == null || (scanClientFindBatchScanClientById = findBatchScanClientById(i2)) == null || !hasScanResultPermission(scanClientFindBatchScanClientById)) {
                return;
            }
            if (byId.callback == null) {
                try {
                    sendResultsByPendingIntent(byId.info, new ArrayList<>(batchScanResults), 1);
                    return;
                } catch (PendingIntent.CanceledException e) {
                    return;
                }
            } else {
                byId.callback.onBatchScanResults(new ArrayList(batchScanResults));
                return;
            }
        }
        Iterator<ScanClient> it = this.mScanManager.getFullBatchScanQueue().iterator();
        while (it.hasNext()) {
            deliverBatchScan(it.next(), batchScanResults);
        }
    }

    private void sendBatchScanResults(ContextMap<IScannerCallback, PendingIntentInfo>.App app, ScanClient scanClient, ArrayList<ScanResult> arrayList) {
        try {
            if (app.callback != null) {
                app.callback.onBatchScanResults(arrayList);
            } else {
                sendResultsByPendingIntent(app.info, arrayList, 1);
            }
        } catch (PendingIntent.CanceledException | RemoteException e) {
            Log.e(TAG, "Exception: " + e);
            this.mScannerMap.remove(scanClient.scannerId);
            this.mScanManager.stopScan(scanClient);
        }
    }

    private void deliverBatchScan(ScanClient scanClient, Set<ScanResult> set) throws RemoteException {
        ContextMap<IScannerCallback, PendingIntentInfo>.App byId = this.mScannerMap.getById(scanClient.scannerId);
        if (byId == null || !hasScanResultPermission(scanClient)) {
            return;
        }
        if (scanClient.filters == null || scanClient.filters.isEmpty()) {
            sendBatchScanResults(byId, scanClient, new ArrayList<>(set));
        }
        ArrayList<ScanResult> arrayList = new ArrayList<>();
        for (ScanResult scanResult : set) {
            if (matchesFilters(scanClient, scanResult)) {
                arrayList.add(scanResult);
            }
        }
        sendBatchScanResults(byId, scanClient, arrayList);
    }

    private Set<ScanResult> parseBatchScanResults(int i, int i2, byte[] bArr) {
        if (i == 0) {
            return Collections.emptySet();
        }
        if (DBG) {
            Log.d(TAG, "current time is " + SystemClock.elapsedRealtimeNanos());
        }
        if (i2 == 1) {
            return parseTruncatedResults(i, bArr);
        }
        return parseFullResults(i, bArr);
    }

    private Set<ScanResult> parseTruncatedResults(int i, byte[] bArr) {
        if (DBG) {
            Log.d(TAG, "batch record " + Arrays.toString(bArr));
        }
        HashSet hashSet = new HashSet(i);
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        for (int i2 = 0; i2 < i; i2++) {
            byte[] bArrExtractBytes = extractBytes(bArr, i2 * 11, 11);
            byte[] bArrExtractBytes2 = extractBytes(bArrExtractBytes, 0, 6);
            reverse(bArrExtractBytes2);
            hashSet.add(new ScanResult(this.mAdapter.getRemoteDevice(bArrExtractBytes2), ScanRecord.parseFromBytes(new byte[0]), bArrExtractBytes[8], jElapsedRealtimeNanos - parseTimestampNanos(extractBytes(bArrExtractBytes, 9, 2))));
        }
        return hashSet;
    }

    @VisibleForTesting
    long parseTimestampNanos(byte[] bArr) {
        return TimeUnit.MILLISECONDS.toNanos(((long) NumberUtils.littleEndianByteArrayToInt(bArr)) * 50);
    }

    private Set<ScanResult> parseFullResults(int i, byte[] bArr) {
        if (DBG) {
            Log.d(TAG, "Batch record : " + Arrays.toString(bArr));
        }
        HashSet hashSet = new HashSet(i);
        long jElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
        int i2 = 0;
        while (i2 < bArr.length) {
            byte[] bArrExtractBytes = extractBytes(bArr, i2, 6);
            reverse(bArrExtractBytes);
            BluetoothDevice remoteDevice = this.mAdapter.getRemoteDevice(bArrExtractBytes);
            int i3 = i2 + 6 + 1 + 1;
            int i4 = i3 + 1;
            byte b = bArr[i3];
            long timestampNanos = jElapsedRealtimeNanos - parseTimestampNanos(extractBytes(bArr, i4, 2));
            int i5 = i4 + 2;
            int i6 = i5 + 1;
            int i7 = bArr[i5];
            byte[] bArrExtractBytes2 = extractBytes(bArr, i6, i7);
            int i8 = i6 + i7;
            int i9 = i8 + 1;
            int i10 = bArr[i8];
            byte[] bArrExtractBytes3 = extractBytes(bArr, i9, i10);
            int i11 = i9 + i10;
            byte[] bArr2 = new byte[i7 + i10];
            System.arraycopy(bArrExtractBytes2, 0, bArr2, 0, i7);
            System.arraycopy(bArrExtractBytes3, 0, bArr2, i7, i10);
            if (DBG) {
                Log.d(TAG, "ScanRecord : " + Arrays.toString(bArr2));
            }
            hashSet.add(new ScanResult(remoteDevice, ScanRecord.parseFromBytes(bArr2), b, timestampNanos));
            i2 = i11;
        }
        return hashSet;
    }

    private void reverse(byte[] bArr) {
        int length = bArr.length;
        for (int i = 0; i < length / 2; i++) {
            byte b = bArr[i];
            int i2 = (length - 1) - i;
            bArr[i] = bArr[i2];
            bArr[i2] = b;
        }
    }

    private static byte[] extractBytes(byte[] bArr, int i, int i2) {
        byte[] bArr2 = new byte[i2];
        System.arraycopy(bArr, i, bArr2, 0, i2);
        return bArr2;
    }

    void onBatchScanThresholdCrossed(int i) {
        if (DBG) {
            Log.d(TAG, "onBatchScanThresholdCrossed() - clientIf=" + i);
        }
        flushPendingBatchResults(i);
    }

    AdvtFilterOnFoundOnLostInfo createOnTrackAdvFoundLostObject(int i, int i2, byte[] bArr, int i3, byte[] bArr2, int i4, int i5, int i6, String str, int i7, int i8, int i9, int i10) {
        return new AdvtFilterOnFoundOnLostInfo(i, i2, bArr, i3, bArr2, i4, i5, i6, str, i7, i8, i9, i10);
    }

    void onTrackAdvFoundLost(AdvtFilterOnFoundOnLostInfo advtFilterOnFoundOnLostInfo) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onTrackAdvFoundLost() - scannerId= " + advtFilterOnFoundOnLostInfo.getClientIf() + " address = " + advtFilterOnFoundOnLostInfo.getAddress() + " adv_state = " + advtFilterOnFoundOnLostInfo.getAdvState());
        }
        ContextMap<IScannerCallback, PendingIntentInfo>.App byId = this.mScannerMap.getById(advtFilterOnFoundOnLostInfo.getClientIf());
        if (byId == null || (byId.callback == null && byId.info == null)) {
            Log.e(TAG, "app or callback is null");
            return;
        }
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(advtFilterOnFoundOnLostInfo.getAddress());
        int advState = advtFilterOnFoundOnLostInfo.getAdvState();
        ScanResult scanResult = new ScanResult(remoteDevice, ScanRecord.parseFromBytes(advtFilterOnFoundOnLostInfo.getResult()), advtFilterOnFoundOnLostInfo.getRSSIValue(), SystemClock.elapsedRealtimeNanos());
        for (ScanClient scanClient : this.mScanManager.getRegularScanQueue()) {
            if (scanClient.scannerId == advtFilterOnFoundOnLostInfo.getClientIf()) {
                ScanSettings scanSettings = scanClient.settings;
                if (advState == 0 && (scanSettings.getCallbackType() & 2) != 0) {
                    if (byId.callback != null) {
                        byId.callback.onFoundOrLost(true, scanResult);
                    } else {
                        sendResultByPendingIntent(byId.info, scanResult, 2, scanClient);
                    }
                } else if (advState == 1 && (scanSettings.getCallbackType() & 4) != 0) {
                    if (byId.callback != null) {
                        byId.callback.onFoundOrLost(false, scanResult);
                    } else {
                        sendResultByPendingIntent(byId.info, scanResult, 4, scanClient);
                    }
                } else if (DBG) {
                    Log.d(TAG, "Not reporting onlost/onfound : " + advState + " scannerId = " + scanClient.scannerId + " callbackType " + scanSettings.getCallbackType());
                }
            }
        }
    }

    void onScanParamSetupCompleted(int i, int i2) throws RemoteException {
        ContextMap<IScannerCallback, PendingIntentInfo>.App byId = this.mScannerMap.getById(i2);
        if (byId == null || byId.callback == null) {
            Log.e(TAG, "Advertise app or callback is null");
        } else if (DBG) {
            Log.d(TAG, "onScanParamSetupCompleted : " + i);
        }
    }

    void onScanManagerErrorCallback(int i, int i2) throws RemoteException {
        ContextMap<IScannerCallback, PendingIntentInfo>.App byId = this.mScannerMap.getById(i);
        if (byId == null || (byId.callback == null && byId.info == null)) {
            Log.e(TAG, "App or callback is null");
            return;
        }
        if (byId.callback != null) {
            byId.callback.onScanManagerErrorCallback(i2);
            return;
        }
        try {
            sendErrorByPendingIntent(byId.info, i2);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Error sending error code via PendingIntent:" + e);
        }
    }

    void onConfigureMTU(int i, int i2, int i3) throws RemoteException {
        String strAddressByConnId = this.mClientMap.addressByConnId(i);
        if (DBG) {
            Log.d(TAG, "onConfigureMTU() address=" + strAddressByConnId + ", status=" + i2 + ", mtu=" + i3);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.callback.onConfigureMTU(strAddressByConnId, i3, i2);
        }
    }

    void onClientCongestion(int i, boolean z) throws RemoteException {
        CallbackInfo callbackInfoPopQueuedCallback;
        if (VDBG) {
            Log.d(TAG, "onClientCongestion() - connId=" + i + ", congested=" + z);
        }
        ContextMap<IBluetoothGattCallback, Void>.App byConnId = this.mClientMap.getByConnId(i);
        if (byConnId != null) {
            byConnId.isCongested = Boolean.valueOf(z);
            while (!byConnId.isCongested.booleanValue() && (callbackInfoPopQueuedCallback = byConnId.popQueuedCallback()) != null) {
                byConnId.callback.onCharacteristicWrite(callbackInfoPopQueuedCallback.address, callbackInfoPopQueuedCallback.status, callbackInfoPopQueuedCallback.handle);
            }
        }
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HashMap map = new HashMap();
        for (BluetoothDevice bluetoothDevice : this.mAdapter.getBondedDevices()) {
            if (getDeviceType(bluetoothDevice) != 1) {
                map.put(bluetoothDevice, 0);
            }
        }
        HashSet hashSet = new HashSet();
        hashSet.addAll(this.mClientMap.getConnectedDevices());
        hashSet.addAll(this.mServerMap.getConnectedDevices());
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            BluetoothDevice remoteDevice = this.mAdapter.getRemoteDevice((String) it.next());
            if (remoteDevice != null) {
                map.put(remoteDevice, 2);
            }
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry entry : map.entrySet()) {
            for (int i : iArr) {
                if (((Integer) entry.getValue()).intValue() == i) {
                    arrayList.add((BluetoothDevice) entry.getKey());
                }
            }
        }
        return arrayList;
    }

    void registerScanner(IScannerCallback iScannerCallback, WorkSource workSource) throws RemoteException {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        UUID uuidRandomUUID = UUID.randomUUID();
        if (DBG) {
            Log.d(TAG, "registerScanner() - UUID=" + uuidRandomUUID);
        }
        if (workSource != null) {
            enforceImpersonatationPermission();
        }
        AppScanStats appScanStatsByUid = this.mScannerMap.getAppScanStatsByUid(Binder.getCallingUid());
        if (appScanStatsByUid != null && appScanStatsByUid.isScanningTooFrequently() && checkCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED") != 0) {
            Log.e(TAG, "App '" + appScanStatsByUid.appName + "' is scanning too frequently");
            iScannerCallback.onScannerRegistered(6, -1);
            return;
        }
        this.mScannerMap.add(uuidRandomUUID, workSource, iScannerCallback, null, this);
        this.mScanManager.registerScanner(uuidRandomUUID);
    }

    void unregisterScanner(int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "unregisterScanner() - scannerId=" + i);
        }
        this.mScannerMap.remove(i);
        this.mScanManager.unregisterScanner(i);
    }

    void startScan(int i, ScanSettings scanSettings, List<ScanFilter> list, List<List<ResultStorageDescriptor>> list2, String str) {
        if (DBG) {
            Log.d(TAG, "start scan with filters");
        }
        enforceAdminPermission();
        if (needsPrivilegedPermissionForScan(scanSettings)) {
            enforcePrivilegedPermission();
        }
        ScanClient scanClient = new ScanClient(i, scanSettings, list, list2);
        scanClient.hasLocationPermission = Utils.checkCallerHasLocationPermission(this, this.mAppOps, str);
        scanClient.hasPeersMacAddressPermission = Utils.checkCallerHasPeersMacAddressPermission(this);
        scanClient.legacyForegroundApp = Utils.isLegacyForegroundApp(this, str);
        AppScanStats appScanStatsById = this.mScannerMap.getAppScanStatsById(i);
        if (appScanStatsById != null) {
            scanClient.stats = appScanStatsById;
            appScanStatsById.recordScanStart(scanSettings, (list == null || list.isEmpty()) ? false : true, i);
        }
        this.mScanManager.startScan(scanClient);
    }

    void registerPiAndStartScan(PendingIntent pendingIntent, ScanSettings scanSettings, List<ScanFilter> list, String str) {
        if (DBG) {
            Log.d(TAG, "start scan with filters, for PendingIntent");
        }
        enforceAdminPermission();
        if (needsPrivilegedPermissionForScan(scanSettings)) {
            enforcePrivilegedPermission();
        }
        UUID uuidRandomUUID = UUID.randomUUID();
        if (DBG) {
            Log.d(TAG, "startScan(PI) - UUID=" + uuidRandomUUID);
        }
        PendingIntentInfo pendingIntentInfo = new PendingIntentInfo();
        pendingIntentInfo.intent = pendingIntent;
        pendingIntentInfo.settings = scanSettings;
        pendingIntentInfo.filters = list;
        pendingIntentInfo.callingPackage = str;
        ContextMap<IScannerCallback, PendingIntentInfo>.App appAdd = this.mScannerMap.add(uuidRandomUUID, null, null, pendingIntentInfo, this);
        try {
            appAdd.hasLocationPermisson = Utils.checkCallerHasLocationPermission(this, this.mAppOps, str);
        } catch (SecurityException e) {
            appAdd.hasLocationPermisson = false;
        }
        try {
            appAdd.hasPeersMacAddressPermission = Utils.checkCallerHasPeersMacAddressPermission(this);
        } catch (SecurityException e2) {
            appAdd.hasPeersMacAddressPermission = false;
        }
        this.mScanManager.registerScanner(uuidRandomUUID);
    }

    void continuePiStartScan(int i, ContextMap<IScannerCallback, PendingIntentInfo>.App app) {
        PendingIntentInfo pendingIntentInfo = app.info;
        ScanClient scanClient = new ScanClient(i, pendingIntentInfo.settings, pendingIntentInfo.filters, null);
        scanClient.hasLocationPermission = app.hasLocationPermisson;
        scanClient.hasPeersMacAddressPermission = app.hasPeersMacAddressPermission;
        scanClient.legacyForegroundApp = Utils.isLegacyForegroundApp(this, pendingIntentInfo.callingPackage);
        AppScanStats appScanStatsById = this.mScannerMap.getAppScanStatsById(i);
        if (appScanStatsById != null) {
            scanClient.stats = appScanStatsById;
            appScanStatsById.recordScanStart(pendingIntentInfo.settings, (pendingIntentInfo.filters == null || pendingIntentInfo.filters.isEmpty()) ? false : true, i);
        }
        this.mScanManager.startScan(scanClient);
    }

    void flushPendingBatchResults(int i) {
        if (DBG) {
            Log.d(TAG, "flushPendingBatchResults - scannerId=" + i);
        }
        this.mScanManager.flushBatchScanResults(new ScanClient(i));
    }

    void stopScan(ScanClient scanClient) {
        enforceAdminPermission();
        int size = this.mScanManager.getBatchScanQueue().size() + this.mScanManager.getRegularScanQueue().size();
        if (DBG) {
            Log.d(TAG, "stopScan() - queue size =" + size);
        }
        AppScanStats appScanStatsById = this.mScannerMap.getAppScanStatsById(scanClient.scannerId);
        if (appScanStatsById != null) {
            appScanStatsById.recordScanStop(scanClient.scannerId);
        }
        this.mScanManager.stopScan(scanClient);
    }

    void stopScan(PendingIntent pendingIntent, String str) {
        enforceAdminPermission();
        PendingIntentInfo pendingIntentInfo = new PendingIntentInfo();
        pendingIntentInfo.intent = pendingIntent;
        ContextMap<IScannerCallback, PendingIntentInfo>.App byContextInfo = this.mScannerMap.getByContextInfo(pendingIntentInfo);
        if (VDBG) {
            Log.d(TAG, "stopScan(PendingIntent): app found = " + byContextInfo);
        }
        if (byContextInfo != null) {
            int i = byContextInfo.id;
            stopScan(new ScanClient(i));
            unregisterScanner(i);
        }
    }

    void disconnectAll() {
        if (DBG) {
            Log.d(TAG, "disconnectAll()");
        }
        for (Map.Entry<Integer, String> entry : this.mClientMap.getConnectedMap().entrySet()) {
            if (DBG) {
                Log.d(TAG, "disconnecting addr:" + entry.getValue());
            }
            clientDisconnect(entry.getKey().intValue(), entry.getValue());
        }
    }

    void unregAll() {
        for (Integer num : this.mClientMap.getAllAppsIds()) {
            if (DBG) {
                Log.d(TAG, "unreg:" + num);
            }
            unregisterClient(num.intValue());
        }
    }

    void registerSync(ScanResult scanResult, int i, int i2, IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
        enforceAdminPermission();
        this.mPeriodicScanManager.startSync(scanResult, i, i2, iPeriodicAdvertisingCallback);
    }

    void unregisterSync(IPeriodicAdvertisingCallback iPeriodicAdvertisingCallback) {
        enforceAdminPermission();
        this.mPeriodicScanManager.stopSync(iPeriodicAdvertisingCallback);
    }

    void startAdvertisingSet(AdvertisingSetParameters advertisingSetParameters, AdvertiseData advertiseData, AdvertiseData advertiseData2, PeriodicAdvertisingParameters periodicAdvertisingParameters, AdvertiseData advertiseData3, int i, int i2, IAdvertisingSetCallback iAdvertisingSetCallback) {
        enforceAdminPermission();
        this.mAdvertiseManager.startAdvertisingSet(advertisingSetParameters, advertiseData, advertiseData2, periodicAdvertisingParameters, advertiseData3, i, i2, iAdvertisingSetCallback);
    }

    void stopAdvertisingSet(IAdvertisingSetCallback iAdvertisingSetCallback) {
        enforceAdminPermission();
        this.mAdvertiseManager.stopAdvertisingSet(iAdvertisingSetCallback);
    }

    void getOwnAddress(int i) {
        enforcePrivilegedPermission();
        this.mAdvertiseManager.getOwnAddress(i);
    }

    void enableAdvertisingSet(int i, boolean z, int i2, int i3) {
        enforceAdminPermission();
        this.mAdvertiseManager.enableAdvertisingSet(i, z, i2, i3);
    }

    void setAdvertisingData(int i, AdvertiseData advertiseData) {
        enforceAdminPermission();
        this.mAdvertiseManager.setAdvertisingData(i, advertiseData);
    }

    void setScanResponseData(int i, AdvertiseData advertiseData) {
        enforceAdminPermission();
        this.mAdvertiseManager.setScanResponseData(i, advertiseData);
    }

    void setAdvertisingParameters(int i, AdvertisingSetParameters advertisingSetParameters) {
        enforceAdminPermission();
        this.mAdvertiseManager.setAdvertisingParameters(i, advertisingSetParameters);
    }

    void setPeriodicAdvertisingParameters(int i, PeriodicAdvertisingParameters periodicAdvertisingParameters) {
        enforceAdminPermission();
        this.mAdvertiseManager.setPeriodicAdvertisingParameters(i, periodicAdvertisingParameters);
    }

    void setPeriodicAdvertisingData(int i, AdvertiseData advertiseData) {
        enforceAdminPermission();
        this.mAdvertiseManager.setPeriodicAdvertisingData(i, advertiseData);
    }

    void setPeriodicAdvertisingEnable(int i, boolean z) {
        enforceAdminPermission();
        this.mAdvertiseManager.setPeriodicAdvertisingEnable(i, z);
    }

    void registerClient(UUID uuid, IBluetoothGattCallback iBluetoothGattCallback) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "registerClient() - UUID=" + uuid);
        }
        this.mClientMap.add(uuid, null, iBluetoothGattCallback, null, this);
        gattClientRegisterAppNative(uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
    }

    void unregisterClient(int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "unregisterClient() - clientIf=" + i);
        }
        this.mClientMap.remove(i);
        gattClientUnregisterAppNative(i);
    }

    void clientConnect(int i, String str, boolean z, int i2, boolean z2, int i3) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "clientConnect() - address=" + str + ", isDirect=" + z + ", opportunistic=" + z2 + ", phy=" + i3);
        }
        gattClientConnectNative(i, str, z, i2, z2, i3);
    }

    void clientDisconnect(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (DBG) {
            Log.d(TAG, "clientDisconnect() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        gattClientDisconnectNative(i, str, numConnIdByAddress != null ? numConnIdByAddress.intValue() : 0);
    }

    void clientSetPreferredPhy(int i, String str, int i2, int i3, int i4) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            if (DBG) {
                Log.d(TAG, "clientSetPreferredPhy() - no connection to " + str);
                return;
            }
            return;
        }
        if (DBG) {
            Log.d(TAG, "clientSetPreferredPhy() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        gattClientSetPreferredPhyNative(i, str, i2, i3, i4);
    }

    void clientReadPhy(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            if (DBG) {
                Log.d(TAG, "clientReadPhy() - no connection to " + str);
                return;
            }
            return;
        }
        if (DBG) {
            Log.d(TAG, "clientReadPhy() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        gattClientReadPhyNative(i, str);
    }

    int numHwTrackFiltersAvailable() {
        return AdapterService.getAdapterService().getTotalNumOfTrackableAdvertisements() - this.mScanManager.getCurrentUsedTrackingAdvertisement();
    }

    synchronized List<ParcelUuid> getRegisteredServiceUuids() {
        ArrayList arrayList;
        Utils.enforceAdminPermission(this);
        arrayList = new ArrayList();
        Iterator<HandleMap.Entry> it = this.mHandleMap.mEntries.iterator();
        while (it.hasNext()) {
            arrayList.add(new ParcelUuid(it.next().uuid));
        }
        return arrayList;
    }

    List<String> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HashSet hashSet = new HashSet();
        hashSet.addAll(this.mClientMap.getConnectedDevices());
        hashSet.addAll(this.mServerMap.getConnectedDevices());
        return new ArrayList(hashSet);
    }

    void refreshDevice(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "refreshDevice() - address=" + str);
        }
        gattClientRefreshNative(i, str);
    }

    void discoverServices(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (DBG) {
            Log.d(TAG, "discoverServices() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        if (numConnIdByAddress != null) {
            gattClientSearchServiceNative(numConnIdByAddress.intValue(), true, 0L, 0L);
            return;
        }
        Log.e(TAG, "discoverServices() - No connection for " + str + "...");
    }

    void discoverServiceByUuid(int i, String str, UUID uuid) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress != null) {
            gattClientDiscoverServiceByUuidNative(numConnIdByAddress.intValue(), uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
            return;
        }
        Log.e(TAG, "discoverServiceByUuid() - No connection for " + str + "...");
    }

    void readCharacteristic(int i, String str, int i2, int i3) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "readCharacteristic() - address=" + str);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.e(TAG, "readCharacteristic() - No connection for " + str + "...");
            return;
        }
        if (!permissionCheck(numConnIdByAddress.intValue(), i2)) {
            Log.w(TAG, "readCharacteristic() - permission check failed!");
        } else {
            gattClientReadCharacteristicNative(numConnIdByAddress.intValue(), i2, i3);
        }
    }

    void readUsingCharacteristicUuid(int i, String str, UUID uuid, int i2, int i3, int i4) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "readUsingCharacteristicUuid() - address=" + str);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.e(TAG, "readUsingCharacteristicUuid() - No connection for " + str + "...");
            return;
        }
        if (!permissionCheck(uuid)) {
            Log.w(TAG, "readUsingCharacteristicUuid() - permission check failed!");
        } else {
            gattClientReadUsingCharacteristicUuidNative(numConnIdByAddress.intValue(), uuid.getLeastSignificantBits(), uuid.getMostSignificantBits(), i2, i3, i4);
        }
    }

    void writeCharacteristic(int i, String str, int i2, int i3, int i4, byte[] bArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "writeCharacteristic() - address=" + str);
        }
        if (this.mReliableQueue.contains(str)) {
            i3 = 3;
        }
        int i5 = i3;
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.e(TAG, "writeCharacteristic() - No connection for " + str + "...");
            return;
        }
        if (!permissionCheck(numConnIdByAddress.intValue(), i2)) {
            Log.w(TAG, "writeCharacteristic() - permission check failed!");
        } else {
            gattClientWriteCharacteristicNative(numConnIdByAddress.intValue(), i2, i5, i4, bArr);
        }
    }

    void readDescriptor(int i, String str, int i2, int i3) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "readDescriptor() - address=" + str);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.e(TAG, "readDescriptor() - No connection for " + str + "...");
            return;
        }
        if (!permissionCheck(numConnIdByAddress.intValue(), i2)) {
            Log.w(TAG, "readDescriptor() - permission check failed!");
        } else {
            gattClientReadDescriptorNative(numConnIdByAddress.intValue(), i2, i3);
        }
    }

    void writeDescriptor(int i, String str, int i2, int i3, byte[] bArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "writeDescriptor() - address=" + str);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.e(TAG, "writeDescriptor() - No connection for " + str + "...");
            return;
        }
        if (!permissionCheck(numConnIdByAddress.intValue(), i2)) {
            Log.w(TAG, "writeDescriptor() - permission check failed!");
        } else {
            gattClientWriteDescriptorNative(numConnIdByAddress.intValue(), i2, i3, bArr);
        }
    }

    void beginReliableWrite(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "beginReliableWrite() - address=" + str);
        }
        this.mReliableQueue.add(str);
    }

    void endReliableWrite(int i, String str, boolean z) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "endReliableWrite() - address=" + str + " execute: " + z);
        }
        this.mReliableQueue.remove(str);
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress != null) {
            gattClientExecuteWriteNative(numConnIdByAddress.intValue(), z);
        }
    }

    void registerForNotification(int i, String str, int i2, boolean z) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "registerForNotification() - address=" + str + " enable: " + z);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            Log.e(TAG, "registerForNotification() - No connection for " + str + "...");
            return;
        }
        if (!permissionCheck(numConnIdByAddress.intValue(), i2)) {
            Log.w(TAG, "registerForNotification() - permission check failed!");
        } else {
            gattClientRegisterForNotificationsNative(i, str, i2, z);
        }
    }

    void readRemoteRssi(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "readRemoteRssi() - address=" + str);
        }
        gattClientReadRemoteRssiNative(i, str);
    }

    void configureMTU(int i, String str, int i2) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "configureMTU() - address=" + str + " mtu=" + i2);
        }
        Integer numConnIdByAddress = this.mClientMap.connIdByAddress(i, str);
        if (numConnIdByAddress != null) {
            gattClientConfigureMTUNative(numConnIdByAddress.intValue(), i2);
            return;
        }
        Log.e(TAG, "configureMTU() - No connection for " + str + "...");
    }

    void connectionParameterUpdate(int i, String str, int i2) {
        int integer;
        int integer2;
        int integer3;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        switch (i2) {
            case 1:
                integer = getResources().getInteger(R.integer.gatt_high_priority_min_interval);
                integer2 = getResources().getInteger(R.integer.gatt_high_priority_max_interval);
                integer3 = getResources().getInteger(R.integer.gatt_high_priority_latency);
                break;
            case 2:
                integer = getResources().getInteger(R.integer.gatt_low_power_min_interval);
                integer2 = getResources().getInteger(R.integer.gatt_low_power_max_interval);
                integer3 = getResources().getInteger(R.integer.gatt_low_power_latency);
                break;
            default:
                integer = getResources().getInteger(R.integer.gatt_balanced_priority_min_interval);
                integer2 = getResources().getInteger(R.integer.gatt_balanced_priority_max_interval);
                integer3 = getResources().getInteger(R.integer.gatt_balanced_priority_latency);
                break;
        }
        int i3 = integer;
        int i4 = integer2;
        int i5 = integer3;
        if (DBG) {
            Log.d(TAG, "connectionParameterUpdate() - address=" + str + "params=" + i2 + " interval=" + i3 + "/" + i4);
        }
        gattConnectionParameterUpdateNative(i, str, i3, i4, i5, 2000, 0, 0);
    }

    void leConnectionUpdate(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "leConnectionUpdate() - address=" + str + ", intervals=" + i2 + "/" + i3 + ", latency=" + i4 + ", timeout=" + i5 + "msec, min_ce=" + i6 + ", max_ce=" + i7);
        }
        gattConnectionParameterUpdateNative(i, str, i2, i3, i4, i5, i6, i7);
    }

    void onServerRegistered(int i, int i2, long j, long j2) throws RemoteException {
        UUID uuid = new UUID(j2, j);
        if (DBG) {
            Log.d(TAG, "onServerRegistered() - UUID=" + uuid + ", serverIf=" + i2);
        }
        ContextMap<IBluetoothGattServerCallback, Void>.App byUuid = this.mServerMap.getByUuid(uuid);
        if (byUuid != null) {
            byUuid.id = i2;
            byUuid.linkToDeath(new ServerDeathRecipient(i2));
            byUuid.callback.onServerRegistered(i, i2);
        }
    }

    void onServiceAdded(int i, int i2, List<GattDbElement> list) throws RemoteException {
        BluetoothGattService bluetoothGattService;
        if (DBG) {
            Log.d(TAG, "onServiceAdded(), status=" + i);
        }
        if (i != 0) {
            return;
        }
        GattDbElement gattDbElement = list.get(0);
        int i3 = gattDbElement.attributeHandle;
        BluetoothGattService bluetoothGattService2 = null;
        for (GattDbElement gattDbElement2 : list) {
            if (gattDbElement2.type == 0) {
                this.mHandleMap.addService(i2, gattDbElement2.attributeHandle, gattDbElement2.uuid, 0, 0, false);
                bluetoothGattService = new BluetoothGattService(gattDbElement.uuid, gattDbElement.attributeHandle, 0);
            } else if (gattDbElement2.type == 1) {
                this.mHandleMap.addService(i2, gattDbElement2.attributeHandle, gattDbElement2.uuid, 1, 0, false);
                bluetoothGattService = new BluetoothGattService(gattDbElement.uuid, gattDbElement.attributeHandle, 1);
            } else if (gattDbElement2.type == 3) {
                this.mHandleMap.addCharacteristic(i2, gattDbElement2.attributeHandle, gattDbElement2.uuid, i3);
                bluetoothGattService2.addCharacteristic(new BluetoothGattCharacteristic(gattDbElement2.uuid, gattDbElement2.attributeHandle, gattDbElement2.properties, gattDbElement2.permissions));
            } else if (gattDbElement2.type == 4) {
                this.mHandleMap.addDescriptor(i2, gattDbElement2.attributeHandle, gattDbElement2.uuid, i3);
                List<BluetoothGattCharacteristic> characteristics = bluetoothGattService2.getCharacteristics();
                characteristics.get(characteristics.size() - 1).addDescriptor(new BluetoothGattDescriptor(gattDbElement2.uuid, gattDbElement2.attributeHandle, gattDbElement2.permissions));
            }
            bluetoothGattService2 = bluetoothGattService;
        }
        this.mHandleMap.setStarted(i2, i3, true);
        ContextMap<IBluetoothGattServerCallback, Void>.App byId = this.mServerMap.getById(i2);
        if (byId != null) {
            byId.callback.onServiceAdded(i, bluetoothGattService2);
        }
    }

    void onServiceStopped(int i, int i2, int i3) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onServiceStopped() srvcHandle=" + i3 + ", status=" + i);
        }
        if (i == 0) {
            this.mHandleMap.setStarted(i2, i3, false);
        }
        stopNextService(i2, i);
    }

    void onServiceDeleted(int i, int i2, int i3) {
        if (DBG) {
            Log.d(TAG, "onServiceDeleted() srvcHandle=" + i3 + ", status=" + i);
        }
        this.mHandleMap.deleteService(i2, i3);
    }

    void onClientConnected(String str, boolean z, int i, int i2) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onClientConnected() connId=" + i + ", address=" + str + ", connected=" + z);
        }
        ContextMap<IBluetoothGattServerCallback, Void>.App byId = this.mServerMap.getById(i2);
        if (byId == null) {
            return;
        }
        if (z) {
            this.mServerMap.addConnection(i2, i, str);
        } else {
            this.mServerMap.removeConnection(i2, i);
        }
        byId.callback.onServerConnectionState(0, i2, z, str);
    }

    void onServerReadCharacteristic(String str, int i, int i2, int i3, int i4, boolean z) throws RemoteException {
        if (VDBG) {
            Log.d(TAG, "onServerReadCharacteristic() connId=" + i + ", address=" + str + ", handle=" + i3 + ", requestId=" + i2 + ", offset=" + i4);
        }
        HandleMap.Entry byHandle = this.mHandleMap.getByHandle(i3);
        if (byHandle == null) {
            return;
        }
        this.mHandleMap.addRequest(i2, i3);
        ContextMap<IBluetoothGattServerCallback, Void>.App byId = this.mServerMap.getById(byHandle.serverIf);
        if (byId == null) {
            return;
        }
        byId.callback.onCharacteristicReadRequest(str, i2, i4, z, i3);
    }

    void onServerReadDescriptor(String str, int i, int i2, int i3, int i4, boolean z) throws RemoteException {
        if (VDBG) {
            Log.d(TAG, "onServerReadDescriptor() connId=" + i + ", address=" + str + ", handle=" + i3 + ", requestId=" + i2 + ", offset=" + i4);
        }
        HandleMap.Entry byHandle = this.mHandleMap.getByHandle(i3);
        if (byHandle == null) {
            return;
        }
        this.mHandleMap.addRequest(i2, i3);
        ContextMap<IBluetoothGattServerCallback, Void>.App byId = this.mServerMap.getById(byHandle.serverIf);
        if (byId == null) {
            return;
        }
        byId.callback.onDescriptorReadRequest(str, i2, i4, z, i3);
    }

    void onServerWriteCharacteristic(String str, int i, int i2, int i3, int i4, int i5, boolean z, boolean z2, byte[] bArr) throws RemoteException {
        String str2;
        int i6;
        boolean z3;
        if (VDBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onServerWriteCharacteristic() connId=");
            sb.append(i);
            sb.append(", address=");
            str2 = str;
            sb.append(str2);
            sb.append(", handle=");
            sb.append(i3);
            sb.append(", requestId=");
            sb.append(i2);
            sb.append(", isPrep=");
            z3 = z2;
            sb.append(z3);
            sb.append(", offset=");
            i6 = i4;
            sb.append(i6);
            Log.d(TAG, sb.toString());
        } else {
            str2 = str;
            i6 = i4;
            z3 = z2;
        }
        HandleMap.Entry byHandle = this.mHandleMap.getByHandle(i3);
        if (byHandle == null) {
            return;
        }
        this.mHandleMap.addRequest(i2, i3);
        ContextMap<IBluetoothGattServerCallback, Void>.App byId = this.mServerMap.getById(byHandle.serverIf);
        if (byId == null) {
            return;
        }
        byId.callback.onCharacteristicWriteRequest(str2, i2, i6, i5, z3, z, i3, bArr);
    }

    void onServerWriteDescriptor(String str, int i, int i2, int i3, int i4, int i5, boolean z, boolean z2, byte[] bArr) throws RemoteException {
        String str2;
        int i6;
        boolean z3;
        if (VDBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onAttributeWrite() connId=");
            sb.append(i);
            sb.append(", address=");
            str2 = str;
            sb.append(str2);
            sb.append(", handle=");
            sb.append(i3);
            sb.append(", requestId=");
            sb.append(i2);
            sb.append(", isPrep=");
            z3 = z2;
            sb.append(z3);
            sb.append(", offset=");
            i6 = i4;
            sb.append(i6);
            Log.d(TAG, sb.toString());
        } else {
            str2 = str;
            i6 = i4;
            z3 = z2;
        }
        HandleMap.Entry byHandle = this.mHandleMap.getByHandle(i3);
        if (byHandle == null) {
            return;
        }
        this.mHandleMap.addRequest(i2, i3);
        ContextMap<IBluetoothGattServerCallback, Void>.App byId = this.mServerMap.getById(byHandle.serverIf);
        if (byId == null) {
            return;
        }
        byId.callback.onDescriptorWriteRequest(str2, i2, i6, i5, z3, z, i3, bArr);
    }

    void onExecuteWrite(String str, int i, int i2, int i3) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "onExecuteWrite() connId=" + i + ", address=" + str + ", transId=" + i2);
        }
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId = this.mServerMap.getByConnId(i);
        if (byConnId == null) {
            return;
        }
        byConnId.callback.onExecuteWrite(str, i2, i3 == 1);
    }

    void onResponseSendCompleted(int i, int i2) {
        if (DBG) {
            Log.d(TAG, "onResponseSendCompleted() handle=" + i2);
        }
    }

    void onNotificationSent(int i, int i2) throws RemoteException {
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId;
        if (VDBG) {
            Log.d(TAG, "onNotificationSent() connId=" + i + ", status=" + i2);
        }
        String strAddressByConnId = this.mServerMap.addressByConnId(i);
        if (strAddressByConnId == null || (byConnId = this.mServerMap.getByConnId(i)) == null) {
            return;
        }
        if (!byConnId.isCongested.booleanValue()) {
            byConnId.callback.onNotificationSent(strAddressByConnId, i2);
            return;
        }
        if (i2 == 143) {
            i2 = 0;
        }
        byConnId.queueCallback(new CallbackInfo(strAddressByConnId, i2));
    }

    void onServerCongestion(int i, boolean z) throws RemoteException {
        CallbackInfo callbackInfoPopQueuedCallback;
        if (DBG) {
            Log.d(TAG, "onServerCongestion() - connId=" + i + ", congested=" + z);
        }
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId = this.mServerMap.getByConnId(i);
        if (byConnId == null) {
            return;
        }
        byConnId.isCongested = Boolean.valueOf(z);
        while (!byConnId.isCongested.booleanValue() && (callbackInfoPopQueuedCallback = byConnId.popQueuedCallback()) != null) {
            byConnId.callback.onNotificationSent(callbackInfoPopQueuedCallback.address, callbackInfoPopQueuedCallback.status);
        }
    }

    void onMtuChanged(int i, int i2) throws RemoteException {
        ContextMap<IBluetoothGattServerCallback, Void>.App byConnId;
        if (DBG) {
            Log.d(TAG, "onMtuChanged() - connId=" + i + ", mtu=" + i2);
        }
        String strAddressByConnId = this.mServerMap.addressByConnId(i);
        if (strAddressByConnId == null || (byConnId = this.mServerMap.getByConnId(i)) == null) {
            return;
        }
        byConnId.callback.onMtuChanged(strAddressByConnId, i2);
    }

    void registerServer(UUID uuid, IBluetoothGattServerCallback iBluetoothGattServerCallback) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "registerServer() - UUID=" + uuid);
        }
        this.mServerMap.add(uuid, null, iBluetoothGattServerCallback, null, this);
        gattServerRegisterAppNative(uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
    }

    void unregisterServer(int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "unregisterServer() - serverIf=" + i);
        }
        deleteServices(i);
        this.mServerMap.remove(i);
        gattServerUnregisterAppNative(i);
    }

    void serverConnect(int i, String str, boolean z, int i2) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "serverConnect() - address=" + str);
        }
        gattServerConnectNative(i, str, z, i2);
    }

    void serverDisconnect(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mServerMap.connIdByAddress(i, str);
        if (DBG) {
            Log.d(TAG, "serverDisconnect() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        gattServerDisconnectNative(i, str, numConnIdByAddress != null ? numConnIdByAddress.intValue() : 0);
    }

    void serverSetPreferredPhy(int i, String str, int i2, int i3, int i4) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mServerMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            if (DBG) {
                Log.d(TAG, "serverSetPreferredPhy() - no connection to " + str);
                return;
            }
            return;
        }
        if (DBG) {
            Log.d(TAG, "serverSetPreferredPhy() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        gattServerSetPreferredPhyNative(i, str, i2, i3, i4);
    }

    void serverReadPhy(int i, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Integer numConnIdByAddress = this.mServerMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null) {
            if (DBG) {
                Log.d(TAG, "serverReadPhy() - no connection to " + str);
                return;
            }
            return;
        }
        if (DBG) {
            Log.d(TAG, "serverReadPhy() - address=" + str + ", connId=" + numConnIdByAddress);
        }
        gattServerReadPhyNative(i, str);
    }

    void addService(int i, BluetoothGattService bluetoothGattService) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "addService() - uuid=" + bluetoothGattService.getUuid());
        }
        ArrayList arrayList = new ArrayList();
        if (bluetoothGattService.getType() == 0) {
            arrayList.add(GattDbElement.createPrimaryService(bluetoothGattService.getUuid()));
        } else {
            arrayList.add(GattDbElement.createSecondaryService(bluetoothGattService.getUuid()));
        }
        for (BluetoothGattService bluetoothGattService2 : bluetoothGattService.getIncludedServices()) {
            int instanceId = bluetoothGattService2.getInstanceId();
            if (this.mHandleMap.checkServiceExists(bluetoothGattService2.getUuid(), instanceId)) {
                arrayList.add(GattDbElement.createIncludedService(instanceId));
            } else {
                Log.e(TAG, "included service with UUID " + bluetoothGattService2.getUuid() + " not found!");
            }
        }
        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
            arrayList.add(GattDbElement.createCharacteristic(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic.getProperties(), ((bluetoothGattCharacteristic.getKeySize() - 7) << 12) + bluetoothGattCharacteristic.getPermissions()));
            for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattCharacteristic.getDescriptors()) {
                arrayList.add(GattDbElement.createDescriptor(bluetoothGattDescriptor.getUuid(), ((bluetoothGattCharacteristic.getKeySize() - 7) << 12) + bluetoothGattDescriptor.getPermissions()));
            }
        }
        gattServerAddServiceNative(i, arrayList);
    }

    void removeService(int i, int i2) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "removeService() - handle=" + i2);
        }
        gattServerDeleteServiceNative(i, i2);
    }

    void clearServices(int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (DBG) {
            Log.d(TAG, "clearServices()");
        }
        deleteServices(i);
    }

    void sendResponse(int i, String str, int i2, int i3, int i4, byte[] bArr) {
        int i5;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "sendResponse() - address=" + str);
        }
        HandleMap.Entry byRequestId = this.mHandleMap.getByRequestId(i2);
        if (byRequestId == null) {
            i5 = 0;
        } else {
            i5 = byRequestId.handle;
        }
        Integer numConnIdByAddress = this.mServerMap.connIdByAddress(i, str);
        gattServerSendResponseNative(i, numConnIdByAddress != null ? numConnIdByAddress.intValue() : 0, i2, (byte) i3, i5, i4, bArr, 0);
        this.mHandleMap.deleteRequest(i2);
    }

    void sendNotification(int i, String str, int i2, boolean z, byte[] bArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (VDBG) {
            Log.d(TAG, "sendNotification() - address=" + str + " handle=" + i2);
        }
        Integer numConnIdByAddress = this.mServerMap.connIdByAddress(i, str);
        if (numConnIdByAddress == null || numConnIdByAddress.intValue() == 0) {
            return;
        }
        if (z) {
            gattServerSendIndicationNative(i, i2, numConnIdByAddress.intValue(), bArr);
        } else {
            gattServerSendNotificationNative(i, i2, numConnIdByAddress.intValue(), bArr);
        }
    }

    private boolean isRestrictedCharUuid(UUID uuid) {
        return isHidUuid(uuid);
    }

    private boolean isRestrictedSrvcUuid(UUID uuid) {
        return isFidoUUID(uuid);
    }

    private boolean isHidUuid(UUID uuid) {
        for (UUID uuid2 : HID_UUIDS) {
            if (uuid2.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFidoUUID(UUID uuid) {
        for (UUID uuid2 : FIDO_UUIDS) {
            if (uuid2.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private int getDeviceType(BluetoothDevice bluetoothDevice) {
        int iGattClientGetDeviceTypeNative = gattClientGetDeviceTypeNative(bluetoothDevice.getAddress());
        if (DBG) {
            Log.d(TAG, "getDeviceType() - device=" + bluetoothDevice + ", type=" + iGattClientGetDeviceTypeNative);
        }
        return iGattClientGetDeviceTypeNative;
    }

    private void enforceAdminPermission() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
    }

    private boolean needsPrivilegedPermissionForScan(ScanSettings scanSettings) {
        if (BluetoothAdapter.getDefaultAdapter().getState() != 12) {
            return true;
        }
        return (scanSettings == null || scanSettings.getReportDelayMillis() == 0 || scanSettings.getScanResultType() != 1) ? false : true;
    }

    private void enforcePrivilegedPermission() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH_PRIVILEGED permission");
    }

    private void enforceImpersonatationPermission() {
        enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", "Need UPDATE_DEVICE_STATS permission");
    }

    private void stopNextService(int i, int i2) throws RemoteException {
        if (DBG) {
            Log.d(TAG, "stopNextService() - serverIf=" + i + ", status=" + i2);
        }
        if (i2 == 0) {
            for (HandleMap.Entry entry : this.mHandleMap.getEntries()) {
                if (entry.type == 1 && entry.serverIf == i && entry.started) {
                    gattServerStopServiceNative(i, entry.handle);
                    return;
                }
            }
        }
    }

    private void deleteServices(int i) {
        if (DBG) {
            Log.d(TAG, "deleteServices() - serverIf=" + i);
        }
        ArrayList arrayList = new ArrayList();
        for (HandleMap.Entry entry : this.mHandleMap.getEntries()) {
            if (entry.type == 1 && entry.serverIf == i) {
                arrayList.add(Integer.valueOf(entry.handle));
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            gattServerDeleteServiceNative(i, ((Integer) it.next()).intValue());
        }
    }

    private List<UUID> parseUuids(byte[] bArr) {
        int i;
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < bArr.length - 2; i2 = i) {
            int i3 = i2 + 1;
            int unsignedInt = Byte.toUnsignedInt(bArr[i2]);
            if (unsignedInt != 0) {
                i = i3 + 1;
                switch (bArr[i3]) {
                    case 2:
                    case 3:
                        break;
                    default:
                        i += unsignedInt - 1;
                        continue;
                        break;
                }
                while (unsignedInt > 1) {
                    int i4 = i + 1;
                    unsignedInt -= 2;
                    arrayList.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", Integer.valueOf(bArr[i] + (bArr[i4] << 8)))));
                    i = i4 + 1;
                }
            } else {
                return arrayList;
            }
        }
        return arrayList;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mAdvertisingServiceUuids:");
        Iterator<UUID> it = this.mAdvertisingServiceUuids.iterator();
        while (it.hasNext()) {
            println(sb, "  " + it.next());
        }
        println(sb, "mMaxScanFilters: " + this.mMaxScanFilters);
        sb.append("\nGATT Scanner Map\n");
        this.mScannerMap.dump(sb);
        sb.append("GATT Client Map\n");
        this.mClientMap.dump(sb);
        sb.append("GATT Server Map\n");
        this.mServerMap.dump(sb);
        sb.append("GATT Handle Map\n");
        this.mHandleMap.dump(sb);
    }

    void addScanEvent(BluetoothMetricsProto.ScanEvent scanEvent) {
        synchronized (this.mScanEvents) {
            if (this.mScanEvents.size() == 20) {
                this.mScanEvents.remove(0);
            }
            this.mScanEvents.add(scanEvent);
        }
    }

    @Override
    public void dumpProto(BluetoothMetricsProto.BluetoothLog.Builder builder) {
        synchronized (this.mScanEvents) {
            builder.addAllScanEvent(this.mScanEvents);
        }
    }

    void gattTestCommand(int i, UUID uuid, String str, int i2, int i3, int i4, int i5, int i6) {
        String str2 = str == null ? "00:00:00:00:00:00" : str;
        if (uuid != null) {
            gattTestNative(i, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits(), str2, i2, i3, i4, i5, i6);
        } else {
            gattTestNative(i, 0L, 0L, str2, i2, i3, i4, i5, i6);
        }
    }
}
