package com.android.bluetooth.mesh;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothMesh;
import android.bluetooth.mesh.BluetoothMeshAccessRxMessage;
import android.bluetooth.mesh.BluetoothMeshAccessTxMessage;
import android.bluetooth.mesh.ConfigMessageParams;
import android.bluetooth.mesh.IBluetoothMeshCallback;
import android.bluetooth.mesh.MeshInitParams;
import android.bluetooth.mesh.MeshModel;
import android.bluetooth.mesh.OtaOperationParams;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;

public class MeshService extends ProfileService {
    private static final boolean DBG = false;
    private static final int MESSAGE_ACCESS_MODEL_REPLY = 20;
    private static final int MESSAGE_BIND_MODEL_APP = 19;
    private static final int MESSAGE_DISABLE = 2;
    private static final int MESSAGE_DUMP_DATA = 15;
    private static final int MESSAGE_ENABLE = 1;
    private static final int MESSAGE_GATT_CONNECT = 22;
    private static final int MESSAGE_GATT_DISCONNECT = 23;
    private static final int MESSAGE_GET_DEFAULT_TTL = 18;
    private static final int MESSAGE_GET_ELEMENT_ADDR = 16;
    private static final int MESSAGE_INVITE_PROV = 7;
    private static final int MESSAGE_RESET_DATA = 12;
    private static final int MESSAGE_SAVE_DATA = 13;
    private static final int MESSAGE_SEND_PACKET = 11;
    private static final int MESSAGE_SET_APP_KEY = 5;
    private static final int MESSAGE_SET_DEFAULT_TTL = 17;
    private static final int MESSAGE_SET_LOG_LEVEL = 21;
    private static final int MESSAGE_SET_MODEL_CC_TX_MSG = 10;
    private static final int MESSAGE_SET_MODEL_DATA = 3;
    private static final int MESSAGE_SET_NET_KEY = 4;
    private static final int MESSAGE_SET_PRVO_FACTOR = 9;
    private static final int MESSAGE_SHOW_VERSION = 14;
    private static final int MESSAGE_START_PROV = 8;
    private static final int MESSAGE_UNPROV_SCAN = 6;
    private static final String TAG = "MeshService";
    private static MeshService sMeshService;
    private IBluetoothMeshCallback mCallback;
    private boolean mNativeAvailable;
    private BluetoothDevice mTargetDevice = null;
    private int meshRole = -1;
    private boolean meshState = false;

    private static native void classInitNative();

    private native void cleanupNative();

    private native void initializeNative();

    private native int meshAccessModelReplyNative(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage, BluetoothMeshAccessTxMessage bluetoothMeshAccessTxMessage);

    private native int meshAddDevkeyNative(int i, int[] iArr, int[] iArr2);

    private native int meshAddElementNative(int i);

    private native int meshAddModelNative(MeshModel meshModel);

    private native int meshBearerAdvSetParams(long j, int i, int i2, int i3, long j2, int i4, int i5);

    private native int meshDelDevKeyNative(int i);

    private native int meshDisableNative();

    private native void meshDumpNative(int i);

    private native int meshEnableNative(MeshInitParams meshInitParams);

    private native int meshGattConnectNative(byte[] bArr, int i, int i2);

    private native int meshGattDisconnectNative();

    private native int meshGetDefaultTTLNative();

    private native int[] meshGetDevKeyNative(int i);

    private native int meshGetElementAddrNative(int i);

    private native int meshGetModelHandleNative(long j, int i);

    private native char[] meshGetVersionNative();

    private native int meshInviteProvisioningNative(int[] iArr, int i);

    private native int meshModelAppBindNative(int i, int i2);

    private native int[] meshOtaGetClientModelHandleNative();

    private native int meshOtaInitiatorOperationNative(OtaOperationParams otaOperationParams);

    private native int meshPublishModelNative(int i, int i2, int i3, int[] iArr);

    private native boolean meshResetDataNative(int i);

    private native boolean meshSaveDataNative();

    private native int meshSendConfigMessageNative(int i, int i2, int i3, int i4, int i5, ConfigMessageParams configMessageParams);

    private native int meshSendPacketNative(int i, int i2, int i3, int i4, int i5, int[] iArr);

    private native int meshSetAppKeyNative(int i, int[] iArr, int i2, int i3);

    private native boolean meshSetCompositionDataHeaderNative(int[] iArr);

    private native void meshSetDefaultTTLNative(int i);

    private native boolean meshSetElementAddrNative(int i);

    private native int meshSetHeartbeatPeriodNative(int i, long j);

    private native int meshSetIvNative(long j, int i);

    private native void meshSetLogLevelNative(long j);

    private native int meshSetMeshModeNative(int i);

    private native int meshSetNetKeyNative(int i, int[] iArr, int i2);

    private native int meshSetProvisionFactorNative(int i, int[] iArr);

    private native int meshSetScanParamsNative(int i, int i2);

    private native int meshSetSpecialPktParams(boolean z, int i, int i2, int i3);

    private native int meshStartProvisioningNative(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, long j, int i7, int i8, int i9);

    private native void meshUnProvDevScanNative(boolean z, int i);

    static {
        classInitNative();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothMeshBinder(this);
    }

    @Override
    protected boolean start() {
        Log.d(TAG, "starting Bluetooth MeshService");
        initializeNative();
        this.mNativeAvailable = true;
        setMeshService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        Log.d(TAG, "Stopping Bluetooth MeshService");
        if (this.meshState) {
            Log.d(TAG, "disable Mesh when stopping Bluetooth MeshService");
            disable();
        }
        this.meshState = false;
        this.meshRole = -1;
        return true;
    }

    @Override
    protected void cleanup() {
        Log.d(TAG, "cleanup Bluetooth MeshService");
        if (this.mNativeAvailable) {
            cleanupNative();
            this.mNativeAvailable = false;
        }
        clearMeshService();
    }

    public static synchronized MeshService getMeshService() {
        if (sMeshService != null && sMeshService.isAvailable()) {
            return sMeshService;
        }
        if (sMeshService == null) {
            Log.w(TAG, "getMeshService(): service is NULL");
        } else if (!sMeshService.isAvailable()) {
            Log.w(TAG, "getMeshService(): service is not available");
        }
        return null;
    }

    private static synchronized void setMeshService(MeshService meshService) {
        if (meshService != null) {
            try {
                if (meshService.isAvailable()) {
                    sMeshService = meshService;
                } else if (sMeshService == null) {
                    Log.w(TAG, "setMeshService(): service not available");
                } else if (!sMeshService.isAvailable()) {
                    Log.w(TAG, "setMeshService(): service is cleaning up");
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static synchronized void clearMeshService() {
        sMeshService = null;
    }

    private static class BluetoothMeshBinder extends IBluetoothMesh.Stub implements ProfileService.IProfileServiceBinder {
        private MeshService mService;

        public BluetoothMeshBinder(MeshService meshService) {
            this.mService = meshService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private MeshService getService() {
            if (!Utils.checkCaller()) {
                Log.w(MeshService.TAG, "InputDevice call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public void registerCallback(IBluetoothMeshCallback iBluetoothMeshCallback) {
            MeshService service = getService();
            if (service == null) {
                return;
            }
            service.registerCallback(iBluetoothMeshCallback);
        }

        public int getMeshRole() {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getMeshRole();
        }

        public boolean getMeshState() {
            MeshService service = getService();
            if (service == null) {
                return false;
            }
            return service.getMeshState();
        }

        public int enable(MeshInitParams meshInitParams) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.enable(meshInitParams);
        }

        public int disable() {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.disable();
        }

        public boolean setCompositionDataHeader(int[] iArr) {
            MeshService service = getService();
            if (service == null) {
                return false;
            }
            return service.setCompositionDataHeader(iArr);
        }

        public int addElement(int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.addElement(i);
        }

        public boolean setElementAddr(int i) {
            MeshService service = getService();
            if (service == null) {
                return false;
            }
            return service.setElementAddr(i);
        }

        public int addModel(MeshModel meshModel) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.addModel(meshModel);
        }

        public int setNetkey(int i, int[] iArr, int i2) {
            MeshService service = getService();
            if (service == null) {
                return 0;
            }
            return service.setNetkey(i, iArr, i2);
        }

        public int setAppkey(int i, int[] iArr, int i2, int i3) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setAppkey(i, iArr, i2, i3);
        }

        public void unProvisionScan(boolean z, int i) {
            MeshService service = getService();
            if (service == null) {
                return;
            }
            service.unProvisionScan(z, i);
        }

        public int inviteProvisioning(int[] iArr, int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.inviteProvisioning(iArr, i);
        }

        public int startProvisioning(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, long j, int i7, int i8, int i9) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.startProvisioning(i, i2, i3, i4, i5, iArr, i6, j, i7, i8, i9);
        }

        public int setProvisionFactor(int i, int[] iArr) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setProvisionFactor(i, iArr);
        }

        public int sendConfigMessage(int i, int i2, int i3, int i4, int i5, ConfigMessageParams configMessageParams) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.sendConfigMessage(i, i2, i3, i4, i5, configMessageParams);
        }

        public int sendPacket(int i, int i2, int i3, int i4, int i5, int[] iArr) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.sendPacket(i, i2, i3, i4, i5, iArr);
        }

        public int publishModel(int i, int i2, int i3, int[] iArr) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.publishModel(i, i2, i3, iArr);
        }

        public int setMeshMode(int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setMeshMode(i);
        }

        public boolean resetData(int i) {
            MeshService service = getService();
            if (service == null) {
                return false;
            }
            return service.resetData(i);
        }

        public boolean saveData() {
            MeshService service = getService();
            if (service == null) {
                return false;
            }
            return service.saveData();
        }

        public void setData() {
        }

        public String getVersion() {
            MeshService service = getService();
            if (service == null) {
                return null;
            }
            return service.getVersion();
        }

        public void dump(int i) {
            MeshService service = getService();
            if (service == null) {
                return;
            }
            service.dump(i);
        }

        public int getElementAddr(int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getElementAddr(i);
        }

        public void setDefaultTTL(int i) {
            MeshService service = getService();
            if (service == null) {
                return;
            }
            service.setDefaultTTL(i);
        }

        public int getDefaultTTL() {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getDefaultTTL();
        }

        public int setIv(long j, int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setIv(j, i);
        }

        public int addDevKey(int i, int[] iArr, int[] iArr2) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.addDevKey(i, iArr, iArr2);
        }

        public int[] getDevKey(int i) {
            MeshService service = getService();
            if (service == null) {
                return null;
            }
            return service.getDevKey(i);
        }

        public int delDevKey(int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.delDevKey(i);
        }

        public int modelAppBind(int i, int i2) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.modelAppBind(i, i2);
        }

        public int accessModelReply(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage, BluetoothMeshAccessTxMessage bluetoothMeshAccessTxMessage) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.accessModelReply(i, bluetoothMeshAccessRxMessage, bluetoothMeshAccessTxMessage);
        }

        public int getModelHandle(long j, int i) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getModelHandle(j, i);
        }

        public void setLogLevel(long j) {
            MeshService service = getService();
            if (service == null) {
                return;
            }
            service.setLogLevel(j);
        }

        public int gattConnect(String str, int i, int i2) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.gattConnect(str, i, i2);
        }

        public int gattDisconnect() {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.gattDisconnect();
        }

        public int setHeartbeatPeriod(int i, long j) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setHeartbeatPeriod(i, j);
        }

        public int[] otaGetClientModelHandle() {
            MeshService service = getService();
            if (service == null) {
                return null;
            }
            return service.otaGetClientModelHandle();
        }

        public int otaInitiatorOperation(OtaOperationParams otaOperationParams) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.otaInitiatorOperation(otaOperationParams);
        }

        public int bearerAdvSetParams(long j, int i, int i2, int i3, long j2, int i4, int i5) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.bearerAdvSetParams(j, i, i2, i3, j2, i4, i5);
        }

        public int setScanParams(int i, int i2) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setScanParams(i, i2);
        }

        public int setSpecialPktParams(boolean z, int i, int i2, int i3) {
            MeshService service = getService();
            if (service == null) {
                return -1;
            }
            return service.setSpecialPktParams(z, i, i2, i3);
        }
    }

    public void registerCallback(IBluetoothMeshCallback iBluetoothMeshCallback) {
        this.mCallback = iBluetoothMeshCallback;
    }

    public int getMeshRole() {
        return this.meshRole;
    }

    public boolean getMeshState() {
        return this.meshState;
    }

    public int enable(MeshInitParams meshInitParams) {
        this.meshRole = meshInitParams.getRole();
        return meshEnableNative(meshInitParams);
    }

    public int disable() {
        int iMeshDisableNative = meshDisableNative();
        if (iMeshDisableNative == 0) {
            this.meshState = false;
            this.meshRole = -1;
        }
        return iMeshDisableNative;
    }

    public int setNetkey(int i, int[] iArr, int i2) {
        return meshSetNetKeyNative(i, iArr, i2);
    }

    public int setAppkey(int i, int[] iArr, int i2, int i3) {
        return meshSetAppKeyNative(i, iArr, i2, i3);
    }

    public int setIv(long j, int i) {
        return meshSetIvNative(j, i);
    }

    public int addDevKey(int i, int[] iArr, int[] iArr2) {
        return meshAddDevkeyNative(i, iArr, iArr2);
    }

    public int[] getDevKey(int i) {
        return meshGetDevKeyNative(i);
    }

    public int delDevKey(int i) {
        return meshDelDevKeyNative(i);
    }

    public void unProvisionScan(boolean z, int i) {
        meshUnProvDevScanNative(z, i);
    }

    public int inviteProvisioning(int[] iArr, int i) {
        return meshInviteProvisioningNative(iArr, i);
    }

    public int startProvisioning(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, long j, int i7, int i8, int i9) {
        return meshStartProvisioningNative(i, i2, i3, i4, i5, iArr, i6, j, i7, i8, i9);
    }

    public int setProvisionFactor(int i, int[] iArr) {
        return meshSetProvisionFactorNative(i, iArr);
    }

    public boolean setCompositionDataHeader(int[] iArr) {
        return meshSetCompositionDataHeaderNative(iArr);
    }

    public int addElement(int i) {
        return meshAddElementNative(i);
    }

    public boolean setElementAddr(int i) {
        return meshSetElementAddrNative(i);
    }

    public int getElementAddr(int i) {
        return meshGetElementAddrNative(i);
    }

    public void setDefaultTTL(int i) {
        meshSetDefaultTTLNative(i);
    }

    public int getDefaultTTL() {
        return meshGetDefaultTTLNative();
    }

    public int modelAppBind(int i, int i2) {
        return meshModelAppBindNative(i, i2);
    }

    public int accessModelReply(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage, BluetoothMeshAccessTxMessage bluetoothMeshAccessTxMessage) {
        return meshAccessModelReplyNative(i, bluetoothMeshAccessRxMessage, bluetoothMeshAccessTxMessage);
    }

    public int getModelHandle(long j, int i) {
        return meshGetModelHandleNative(j, i);
    }

    public int addModel(MeshModel meshModel) {
        return meshAddModelNative(meshModel);
    }

    public int sendConfigMessage(int i, int i2, int i3, int i4, int i5, ConfigMessageParams configMessageParams) {
        return meshSendConfigMessageNative(i, i2, i3, i4, i5, configMessageParams);
    }

    public int sendPacket(int i, int i2, int i3, int i4, int i5, int[] iArr) {
        return meshSendPacketNative(i, i2, i3, i4, i5, iArr);
    }

    public int publishModel(int i, int i2, int i3, int[] iArr) {
        return meshPublishModelNative(i, i2, i3, iArr);
    }

    public int setMeshMode(int i) {
        return meshSetMeshModeNative(i);
    }

    public boolean resetData(int i) {
        return meshResetDataNative(i);
    }

    public boolean saveData() {
        return meshSaveDataNative();
    }

    public String getVersion() {
        char[] cArrMeshGetVersionNative = meshGetVersionNative();
        if (cArrMeshGetVersionNative == null) {
            return null;
        }
        return String.valueOf(cArrMeshGetVersionNative);
    }

    public void dump(int i) {
        meshDumpNative(i);
    }

    public void setLogLevel(long j) {
        meshSetLogLevelNative(j);
    }

    public int gattConnect(String str, int i, int i2) {
        return meshGattConnectNative(Utils.getBytesFromAddress(str), i, i2);
    }

    public int gattDisconnect() {
        return meshGattDisconnectNative();
    }

    public int setHeartbeatPeriod(int i, long j) {
        return meshSetHeartbeatPeriodNative(i, j);
    }

    public int[] otaGetClientModelHandle() {
        return meshOtaGetClientModelHandleNative();
    }

    public int otaInitiatorOperation(OtaOperationParams otaOperationParams) {
        return meshOtaInitiatorOperationNative(otaOperationParams);
    }

    public int bearerAdvSetParams(long j, int i, int i2, int i3, long j2, int i4, int i5) {
        return meshBearerAdvSetParams(j, i, i2, i3, j2, i4, i5);
    }

    public int setScanParams(int i, int i2) {
        return meshSetScanParamsNative(i, i2);
    }

    public int setSpecialPktParams(boolean z, int i, int i2, int i3) {
        return meshSetSpecialPktParams(z, i, i2, i3);
    }

    public void onMeshEnabled() {
        this.meshState = true;
        if (this.mCallback != null) {
            try {
                this.mCallback.onMeshEnabled();
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onConfigReset() {
        if (this.mCallback != null) {
            try {
                this.mCallback.onConfigReset();
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onFriendShipStatus(int i, int i2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onFriendShipStatus(i, i2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onOTAEvent(int i, int i2, long j, long j2, long j3, int i3, int i4, int i5, int i6, int i7, int[] iArr) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onOTAEvent(i, i2, j, j2, j3, i3, i4, i5, i6, i7, iArr);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onAdvReport(int i, int[] iArr, int i2, int i3, int[] iArr2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onAdvReport(i, iArr, i2, i3, iArr2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvScanComplete() {
        if (this.mCallback != null) {
            try {
                this.mCallback.onProvScanComplete();
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvScanResult(int[] iArr, int i, int[] iArr2, int i2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onScanUnProvDevice(iArr, i, iArr2, i2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvCapabilities(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onProvCapabilities(i, i2, i3, i4, i5, i6, i7, i8);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onRequestOobPublicKey() {
        if (this.mCallback != null) {
            try {
                this.mCallback.onRequestOobPublicKey();
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onRequestOobAuthValue(int i, int i2, int i3) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onRequestOobAuthValue(i, i2, i3);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvShowOobPublicKey(int[] iArr) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onProvShowOobPublicKey(iArr);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvShowOobAuthValue(int[] iArr) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onProvShowOobAuthValue(iArr);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvisionDone(int[] iArr, int i, boolean z, boolean z2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onProvDone(i, iArr, z, z2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onKeyRefresh(int i, int i2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onKeyRefresh(i, i2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onIvUpdate(int i, int i2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onIvUpdate(i, i2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onSeqChange(int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onSeqChange(i);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onProvFactor(int i, int[] iArr, int i2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onProvFactor(i, iArr, i2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onHeartbeat(int i, int i2) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onHeartbeat(i, i2);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onBearerGattStatus(long j, int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onBearerGattStatus(j, i);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onEvtErrorCode(int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onEvtErrorCode(i);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onOTAMsgHandler(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onOTAMsgHandler(i, bluetoothMeshAccessRxMessage);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onMsgHandler(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onMsgHandler(i, bluetoothMeshAccessRxMessage);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    public void onPublishTimeoutCallback(int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onPublishTimeoutCallback(i);
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
    }

    BluetoothMeshAccessRxMessage CreateAccessMessageRxObject(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
        return new BluetoothMeshAccessRxMessage(i, i2, iArr, i3, i4, i5, i6, i7, i8, i9);
    }
}
