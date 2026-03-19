package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothMesh;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.mesh.BluetoothMeshAccessRxMessage;
import android.bluetooth.mesh.BluetoothMeshAccessTxMessage;
import android.bluetooth.mesh.BluetoothMeshCallback;
import android.bluetooth.mesh.ConfigMessageParams;
import android.bluetooth.mesh.IBluetoothMeshCallback;
import android.bluetooth.mesh.MeshInitParams;
import android.bluetooth.mesh.MeshModel;
import android.bluetooth.mesh.OtaOperationParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BluetoothMesh implements BluetoothProfile {
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothMesh";
    private static final boolean VDBG = true;
    private static volatile BluetoothMesh sInstance;
    private BluetoothMeshCallback mCallback;
    private Context mContext;
    private volatile IBluetoothMesh mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private Map<Integer, MeshModel> mModelMap = new HashMap();
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothMesh.TAG, "onBluetoothStateChange: up=" + z);
            if (z) {
                synchronized (BluetoothMesh.this.mConnection) {
                    try {
                    } catch (Exception e) {
                        Log.e(BluetoothMesh.TAG, "", e);
                    }
                    if (BluetoothMesh.this.mService == null) {
                        Log.d(BluetoothMesh.TAG, "Binding service...");
                        BluetoothMesh.this.doBind();
                    }
                }
                return;
            }
            Log.d(BluetoothMesh.TAG, "Unbinding service...");
            synchronized (BluetoothMesh.this.mConnection) {
                try {
                    BluetoothMesh.this.mService = null;
                    BluetoothMesh.this.mContext.unbindService(BluetoothMesh.this.mConnection);
                } catch (Exception e2) {
                    Log.e(BluetoothMesh.TAG, "", e2);
                }
            }
        }
    };
    private final IBluetoothMeshCallback mBluetoothMeshCallback = new IBluetoothMeshCallback.Stub() {
        @Override
        public void onMeshEnabled() {
            Log.d(BluetoothMesh.TAG, "onMeshEnabled");
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onMeshEnabled();
            }
        }

        @Override
        public void onConfigReset() {
            Log.d(BluetoothMesh.TAG, "onConfigReset");
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onConfigReset();
            }
        }

        @Override
        public void onFriendShipStatus(int i, int i2) {
            Log.d(BluetoothMesh.TAG, "onFriendShipStatus addr=" + i + ", staTus=" + i2);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onFriendShipStatus(i, i2);
            }
        }

        @Override
        public void onOTAEvent(int i, int i2, long j, long j2, long j3, int i3, int i4, int i5, int i6, int i7, int[] iArr) {
            Log.d(BluetoothMesh.TAG, "onOTAEvent eventId=" + i + ", errorCode=" + i2 + ", nodesNum=" + i3 + ",curr_block=" + i4 + ",total_block=" + i5 + ",curr_chunk=" + i6 + ",chunk_mask =" + i7);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onOTAEvent(i, i2, j, j2, j3, i3, i4, i5, i6, i7, iArr);
            }
        }

        @Override
        public void onAdvReport(int i, int[] iArr, int i2, int i3, int[] iArr2) {
            Log.d(BluetoothMesh.TAG, "onAdvReport addrType=" + i + ", addr=" + Arrays.toString(iArr) + ", rssi=" + i2 + ", reportType=" + i3 + ", data=" + Arrays.toString(iArr));
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onAdvReport(i, iArr, i2, i3, iArr2);
            }
        }

        @Override
        public void onProvScanComplete() {
            Log.d(BluetoothMesh.TAG, "onProvScanComplete");
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onProvScanComplete();
            }
        }

        @Override
        public void onScanUnProvDevice(int[] iArr, int i, int[] iArr2, int i2) {
            Log.d(BluetoothMesh.TAG, "onScanUnProvDevice uuid=" + Arrays.toString(iArr) + ", oobInfom=" + i + ", uriHash=" + Arrays.toString(iArr2));
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onScanUnProvDevice(iArr, i, iArr2, i2);
            }
        }

        @Override
        public void onProvCapabilities(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            Log.d(BluetoothMesh.TAG, "onProvCapabilities numberOfElements=" + i + ", algorithms=" + i2 + ", publicKeyType=" + i3 + ", staticOOBType=" + i4 + ", outputOobSize=" + i5 + ", outputOobAction=" + i6 + ", inputOobSize=" + i7 + ", inputOobAction=" + i8);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onProvCapabilities(i, i2, i3, i4, i5, i6, i7, i8);
            }
        }

        @Override
        public void onRequestOobPublicKey() {
            Log.d(BluetoothMesh.TAG, "onRequestOobPublicKey");
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onRequestOobPublicKey();
            }
        }

        @Override
        public void onRequestOobAuthValue(int i, int i2, int i3) {
            Log.d(BluetoothMesh.TAG, "onRequestOobAuthValue method=" + i + ", action=" + i2 + ", size=" + i3);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onRequestOobAuthValue(i, i2, i3);
            }
        }

        @Override
        public void onProvShowOobPublicKey(int[] iArr) {
            Log.d(BluetoothMesh.TAG, "onProvShowOobPublicKey publicKey" + Arrays.toString(iArr));
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onProvShowOobPublicKey(iArr);
            }
        }

        @Override
        public void onProvShowOobAuthValue(int[] iArr) {
            Log.d(BluetoothMesh.TAG, "onProvShowOobAuthValue authValue =" + Arrays.toString(iArr));
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onProvShowOobAuthValue(iArr);
            }
        }

        @Override
        public void onProvDone(int i, int[] iArr, boolean z, boolean z2) {
            Log.d(BluetoothMesh.TAG, "onProvDone address=" + i + ", deviceKey=" + Arrays.toString(iArr) + ", success=" + z + ", gattBearer=" + z2);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onProvDone(i, iArr, z, z2);
            }
        }

        @Override
        public void onScanResult(ScanResult scanResult) {
            Log.d(BluetoothMesh.TAG, "onScanResult scanResult=" + scanResult.toString());
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onScanResult(scanResult);
            }
        }

        @Override
        public void onKeyRefresh(int i, int i2) {
            Log.d(BluetoothMesh.TAG, "onKeyRefresh netKeyIndex=" + i + ", phase=" + i2);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onKeyRefresh(i, i2);
            }
        }

        @Override
        public void onIvUpdate(int i, int i2) {
            Log.d(BluetoothMesh.TAG, "onIvUpdate ivIndex=" + i + ", state=" + i2);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onIvUpdate(i, i2);
            }
        }

        @Override
        public void onSeqChange(int i) {
            Log.d(BluetoothMesh.TAG, "onSeqChange seqNumber=" + i);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onSeqChange(i);
            }
        }

        @Override
        public void onProvFactor(int i, int[] iArr, int i2) {
            Log.d(BluetoothMesh.TAG, "onProvFactor type=" + i + ", buf[0]=" + iArr[0]);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onProvFactor(i, iArr, i2);
            }
        }

        @Override
        public void onHeartbeat(int i, int i2) {
            Log.d(BluetoothMesh.TAG, "onHeartbeat address=" + i + ", active=" + i2);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onHeartbeat(i, i2);
            }
        }

        @Override
        public void onBearerGattStatus(long j, int i) {
            Log.d(BluetoothMesh.TAG, "onBearerGattStatus: handle=" + j + " status=" + i);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onBearerGattStatus(j, i);
            }
        }

        @Override
        public void onEvtErrorCode(int i) {
            Log.d(BluetoothMesh.TAG, "onEvtErrorCode: type=" + i);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onEvtErrorCode(i);
            }
        }

        @Override
        public void onOTAMsgHandler(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage) {
            Log.d(BluetoothMesh.TAG, "onOTAMsgHandler" + i);
            if (BluetoothMesh.this.mCallback != null) {
                BluetoothMesh.this.mCallback.onOTAMsgHandler(i, bluetoothMeshAccessRxMessage);
            }
        }

        @Override
        public void onMsgHandler(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage) {
            Log.d(BluetoothMesh.TAG, "onMsgHandler" + i);
            MeshModel meshModel = (MeshModel) BluetoothMesh.this.mModelMap.get(Integer.valueOf(i));
            if (meshModel != null) {
                meshModel.onMsgHandler(i, bluetoothMeshAccessRxMessage);
            }
        }

        @Override
        public void onPublishTimeoutCallback(int i) {
            Log.d(BluetoothMesh.TAG, "onPublishTimeoutCallback" + i);
            MeshModel meshModel = (MeshModel) BluetoothMesh.this.mModelMap.get(Integer.valueOf(i));
            if (meshModel != null) {
                meshModel.onPublishTimeoutCallback(i);
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(BluetoothMesh.TAG, "Proxy object connected");
            BluetoothMesh.this.mService = IBluetoothMesh.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothMesh.this.mServiceListener != null) {
                BluetoothMesh.this.mServiceListener.onServiceConnected(22, BluetoothMesh.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(BluetoothMesh.TAG, "Proxy object disconnected");
            BluetoothMesh.this.mService = null;
            if (BluetoothMesh.this.mServiceListener != null) {
                BluetoothMesh.this.mServiceListener.onServiceDisconnected(22);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothMesh(Context context, BluetoothProfile.ServiceListener serviceListener) {
        this.mContext = context;
        this.mServiceListener = serviceListener;
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        if (isBluetoothEnabled()) {
            Log.d(TAG, "bluetooth state is ON, do bind MeshService...");
            doBind();
        }
    }

    public static BluetoothMesh getDefaultMesh(Context context, BluetoothProfile.ServiceListener serviceListener) {
        if (context == null || serviceListener == null) {
            return null;
        }
        if (sInstance == null) {
            synchronized (BluetoothMesh.class) {
                if (sInstance == null) {
                    sInstance = new BluetoothMesh(context, serviceListener);
                }
            }
        }
        return sInstance;
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothMesh.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth MESH Service with " + intent);
            return false;
        }
        return true;
    }

    public void close() {
        Log.d(TAG, "close()");
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        synchronized (this.mConnection) {
            if (this.mService != null) {
                try {
                    this.mService = null;
                    if (this.mContext == null) {
                        Log.d(TAG, "Context is null");
                    } else {
                        this.mContext.unbindService(this.mConnection);
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "", e2);
                }
            }
        }
        this.mContext = null;
        this.mServiceListener = null;
        sInstance = null;
    }

    public boolean registerCallback(BluetoothMeshCallback bluetoothMeshCallback) {
        Log.d(TAG, "registerCallback()");
        if (this.mService == null) {
            return false;
        }
        this.mCallback = bluetoothMeshCallback;
        try {
            this.mService.registerCallback(this.mBluetoothMeshCallback);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        return null;
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        return null;
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        return 0;
    }

    public int getMeshRole() {
        Log.d(TAG, "getMeshRole");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                int meshRole = this.mService.getMeshRole();
                Log.d(TAG, "getMeshRole role=" + meshRole);
                return meshRole;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public boolean getMeshState() {
        Log.d(TAG, "getMeshState");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                boolean meshState = this.mService.getMeshState();
                Log.d(TAG, "getMeshState state=" + meshState);
                return meshState;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public int enable(MeshInitParams meshInitParams) {
        Log.d(TAG, "enable, role=" + meshInitParams.getRole());
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.enable(meshInitParams);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int disable() {
        Log.d(TAG, "disable");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.disable();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public boolean setCompositionDataHeader(int[] iArr) {
        Log.d(TAG, "setCompositionDataHeader");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setCompositionDataHeader(iArr);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public int addElement(int i) {
        Log.d(TAG, "addElement");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.addElement(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public boolean setElementAddr(int i) {
        Log.d(TAG, "setElementAddr");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setElementAddr(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public int addModel(MeshModel meshModel) {
        Log.d(TAG, "addModel modelopcode=0x" + Integer.toHexString(meshModel.getModelOpcode()) + ",elementIndex=0x" + Integer.toHexString(meshModel.getElementIndex()));
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                int iAddModel = this.mService.addModel(meshModel);
                if (iAddModel > -1) {
                    this.mModelMap.put(Integer.valueOf(iAddModel), meshModel);
                }
                Log.d(TAG, "addModel modelHandle=0x" + Integer.toHexString(iAddModel));
                return iAddModel;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setNetkey(int i, int[] iArr, int i2) {
        Log.d(TAG, "setNetkey");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setNetkey(i, iArr, i2);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setAppkey(int i, int[] iArr, int i2, int i3) {
        Log.d(TAG, "setAppkey");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setAppkey(i, iArr, i2, i3);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public void unProvisionScan(boolean z, int i) {
        Log.d(TAG, "unProvisionScan start=" + z);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                this.mService.unProvisionScan(z, i);
            } else if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
    }

    public int inviteProvisioning(int[] iArr, int i) {
        Log.d(TAG, "inviteProvisioning with UUID " + Arrays.toString(iArr));
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.inviteProvisioning(iArr, i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int startProvisioning(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, long j, int i7, int i8, int i9) {
        Log.d(TAG, "startProvisioning");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.startProvisioning(i, i2, i3, i4, i5, iArr, i6, j, i7, i8, i9);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setProvisionFactor(int i, int[] iArr) {
        Log.d(TAG, "setProvisionFactor type=" + i);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setProvisionFactor(i, iArr);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int sendConfigMessage(int i, int i2, int i3, int i4, int i5, ConfigMessageParams configMessageParams) {
        Log.d(TAG, "sendConfigMessage");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.sendConfigMessage(i, i2, i3, i4, i5, configMessageParams);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int sendPacket(int i, int i2, int i3, int i4, int i5, int[] iArr) {
        Log.d(TAG, "sendPacket");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.sendPacket(i, i2, i3, i4, i5, iArr);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int publishModel(int i, int i2, int i3, int[] iArr) {
        Log.d(TAG, "publishModel modelHandle=0x" + Integer.toHexString(i));
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.publishModel(i, i2, i3, iArr);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setMeshMode(int i) {
        Log.d(TAG, "setMeshMode mode=" + i);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setMeshMode(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public boolean resetData(int i) {
        Log.d(TAG, "resetData, sector=0x" + Integer.toHexString(i));
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.resetData(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public boolean saveData() {
        Log.d(TAG, "saveData");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.saveData();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public void setData() {
    }

    public String getVersion() {
        Log.d(TAG, "getVersion");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.getVersion();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        }
    }

    public void dump(int i) {
        Log.d(TAG, "dump with type " + i);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                this.mService.dump(i);
            } else if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
    }

    public int getElementAddr(int i) {
        Log.d(TAG, "getElementAddr by elementIndex " + i);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.getElementAddr(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public void setDefaultTTL(int i) {
        Log.d(TAG, "setDefaultTTL ttl=" + i);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                this.mService.setDefaultTTL(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
    }

    public int getDefaultTTL() {
        Log.d(TAG, "getDefaultTTL");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.getDefaultTTL();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setIv(long j, int i) {
        Log.d(TAG, "setIv");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setIv(j, i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int addDevKey(int i, int[] iArr, int[] iArr2) {
        Log.d(TAG, "addDevKey");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.addDevKey(i, iArr, iArr2);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int[] getDevKey(int i) {
        Log.d(TAG, "getDevKey");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.getDevKey(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        }
    }

    public int delDevKey(int i) {
        Log.d(TAG, "delDevKey");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.delDevKey(i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int modelAppBind(int i, int i2) {
        Log.d(TAG, "setModelAppBind");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.modelAppBind(i, i2);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int accessModelReply(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage, BluetoothMeshAccessTxMessage bluetoothMeshAccessTxMessage) {
        Log.d(TAG, "accessModelReply");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.accessModelReply(i, bluetoothMeshAccessRxMessage, bluetoothMeshAccessTxMessage);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public void setLogLevel(long j) {
        Log.d(TAG, "setLogLevel");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                this.mService.setLogLevel(j);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
    }

    public int getModelHandle(long j, int i) {
        Log.d(TAG, "getModelHandle");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.getModelHandle(j, i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int gattConnect(String str, int i, int i2) {
        Log.d(TAG, "gattConnect");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.gattConnect(str, i, i2);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int gattDisconnect() {
        Log.d(TAG, "gattDisconnect");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.gattDisconnect();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setHeartbeatPeriod(int i, long j) {
        Log.d(TAG, "setHeartbeatPeriod");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setHeartbeatPeriod(i, j);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int[] otaGetClientModelHandle() {
        Log.d(TAG, "otaGetClientModelHandle");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.otaGetClientModelHandle();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        }
    }

    public int otaInitiatorOperation(OtaOperationParams otaOperationParams) {
        Log.d(TAG, "otaInitiatorOperation");
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.otaInitiatorOperation(otaOperationParams);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int bearerAdvSetParams(long j, int i, int i2, int i3, long j2, int i4, int i5) {
        Log.d(TAG, "bearerAdvSetParams:advPeriod=" + j + ", minInterval=" + i + ", maxInterval=" + i2 + ", resend=" + i3 + ", scanPeriod=" + j2 + ", scanInterval=" + i4 + ", scanWindow=" + i5);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.bearerAdvSetParams(j, i, i2, i3, j2, i4, i5);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setScanParams(int i, int i2) {
        Log.d(TAG, "setScanParams: scanInterval=" + i + ", scanWindow=" + i2);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setScanParams(i, i2);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    public int setSpecialPktParams(boolean z, int i, int i2, int i3) {
        Log.d(TAG, "setSpecialPktParams: isSnIncrease=" + z + ", snIncreaseInterval=" + i + ", advInterval=" + i2 + ",advPeriod=" + i3);
        try {
            if (this.mService != null && isBluetoothEnabled()) {
                return this.mService.setSpecialPktParams(z, i, i2, i3);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return -1;
        }
    }

    private boolean isBluetoothEnabled() {
        return this.mAdapter.getState() == 12;
    }
}
