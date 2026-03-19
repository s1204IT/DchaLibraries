package android.bluetooth;

import android.bluetooth.mesh.BluetoothMeshAccessRxMessage;
import android.bluetooth.mesh.BluetoothMeshAccessTxMessage;
import android.bluetooth.mesh.ConfigMessageParams;
import android.bluetooth.mesh.IBluetoothMeshCallback;
import android.bluetooth.mesh.MeshInitParams;
import android.bluetooth.mesh.MeshModel;
import android.bluetooth.mesh.OtaOperationParams;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IBluetoothMesh extends IInterface {
    int accessModelReply(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage, BluetoothMeshAccessTxMessage bluetoothMeshAccessTxMessage) throws RemoteException;

    int addDevKey(int i, int[] iArr, int[] iArr2) throws RemoteException;

    int addElement(int i) throws RemoteException;

    int addModel(MeshModel meshModel) throws RemoteException;

    int bearerAdvSetParams(long j, int i, int i2, int i3, long j2, int i4, int i5) throws RemoteException;

    int delDevKey(int i) throws RemoteException;

    int disable() throws RemoteException;

    void dump(int i) throws RemoteException;

    int enable(MeshInitParams meshInitParams) throws RemoteException;

    int gattConnect(String str, int i, int i2) throws RemoteException;

    int gattDisconnect() throws RemoteException;

    int getDefaultTTL() throws RemoteException;

    int[] getDevKey(int i) throws RemoteException;

    int getElementAddr(int i) throws RemoteException;

    int getMeshRole() throws RemoteException;

    boolean getMeshState() throws RemoteException;

    int getModelHandle(long j, int i) throws RemoteException;

    String getVersion() throws RemoteException;

    int inviteProvisioning(int[] iArr, int i) throws RemoteException;

    int modelAppBind(int i, int i2) throws RemoteException;

    int[] otaGetClientModelHandle() throws RemoteException;

    int otaInitiatorOperation(OtaOperationParams otaOperationParams) throws RemoteException;

    int publishModel(int i, int i2, int i3, int[] iArr) throws RemoteException;

    void registerCallback(IBluetoothMeshCallback iBluetoothMeshCallback) throws RemoteException;

    boolean resetData(int i) throws RemoteException;

    boolean saveData() throws RemoteException;

    int sendConfigMessage(int i, int i2, int i3, int i4, int i5, ConfigMessageParams configMessageParams) throws RemoteException;

    int sendPacket(int i, int i2, int i3, int i4, int i5, int[] iArr) throws RemoteException;

    int setAppkey(int i, int[] iArr, int i2, int i3) throws RemoteException;

    boolean setCompositionDataHeader(int[] iArr) throws RemoteException;

    void setData() throws RemoteException;

    void setDefaultTTL(int i) throws RemoteException;

    boolean setElementAddr(int i) throws RemoteException;

    int setHeartbeatPeriod(int i, long j) throws RemoteException;

    int setIv(long j, int i) throws RemoteException;

    void setLogLevel(long j) throws RemoteException;

    int setMeshMode(int i) throws RemoteException;

    int setNetkey(int i, int[] iArr, int i2) throws RemoteException;

    int setProvisionFactor(int i, int[] iArr) throws RemoteException;

    int setScanParams(int i, int i2) throws RemoteException;

    int setSpecialPktParams(boolean z, int i, int i2, int i3) throws RemoteException;

    int startProvisioning(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, long j, int i7, int i8, int i9) throws RemoteException;

    void unProvisionScan(boolean z, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IBluetoothMesh {
        private static final String DESCRIPTOR = "android.bluetooth.IBluetoothMesh";
        static final int TRANSACTION_accessModelReply = 33;
        static final int TRANSACTION_addDevKey = 29;
        static final int TRANSACTION_addElement = 7;
        static final int TRANSACTION_addModel = 9;
        static final int TRANSACTION_bearerAdvSetParams = 41;
        static final int TRANSACTION_delDevKey = 31;
        static final int TRANSACTION_disable = 5;
        static final int TRANSACTION_dump = 24;
        static final int TRANSACTION_enable = 4;
        static final int TRANSACTION_gattConnect = 36;
        static final int TRANSACTION_gattDisconnect = 37;
        static final int TRANSACTION_getDefaultTTL = 27;
        static final int TRANSACTION_getDevKey = 30;
        static final int TRANSACTION_getElementAddr = 25;
        static final int TRANSACTION_getMeshRole = 2;
        static final int TRANSACTION_getMeshState = 3;
        static final int TRANSACTION_getModelHandle = 35;
        static final int TRANSACTION_getVersion = 23;
        static final int TRANSACTION_inviteProvisioning = 13;
        static final int TRANSACTION_modelAppBind = 32;
        static final int TRANSACTION_otaGetClientModelHandle = 39;
        static final int TRANSACTION_otaInitiatorOperation = 40;
        static final int TRANSACTION_publishModel = 18;
        static final int TRANSACTION_registerCallback = 1;
        static final int TRANSACTION_resetData = 20;
        static final int TRANSACTION_saveData = 21;
        static final int TRANSACTION_sendConfigMessage = 16;
        static final int TRANSACTION_sendPacket = 17;
        static final int TRANSACTION_setAppkey = 11;
        static final int TRANSACTION_setCompositionDataHeader = 6;
        static final int TRANSACTION_setData = 22;
        static final int TRANSACTION_setDefaultTTL = 26;
        static final int TRANSACTION_setElementAddr = 8;
        static final int TRANSACTION_setHeartbeatPeriod = 38;
        static final int TRANSACTION_setIv = 28;
        static final int TRANSACTION_setLogLevel = 34;
        static final int TRANSACTION_setMeshMode = 19;
        static final int TRANSACTION_setNetkey = 10;
        static final int TRANSACTION_setProvisionFactor = 15;
        static final int TRANSACTION_setScanParams = 42;
        static final int TRANSACTION_setSpecialPktParams = 43;
        static final int TRANSACTION_startProvisioning = 14;
        static final int TRANSACTION_unProvisionScan = 12;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBluetoothMesh asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IBluetoothMesh)) {
                return (IBluetoothMesh) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ConfigMessageParams configMessageParamsCreateFromParcel;
            BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessageCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerCallback(IBluetoothMeshCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int meshRole = getMeshRole();
                    parcel2.writeNoException();
                    parcel2.writeInt(meshRole);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean meshState = getMeshState();
                    parcel2.writeNoException();
                    parcel2.writeInt(meshState ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iEnable = enable(parcel.readInt() != 0 ? MeshInitParams.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iEnable);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iDisable = disable();
                    parcel2.writeNoException();
                    parcel2.writeInt(iDisable);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean compositionDataHeader = setCompositionDataHeader(parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(compositionDataHeader ? 1 : 0);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddElement = addElement(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddElement);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean elementAddr = setElementAddr(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(elementAddr ? 1 : 0);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddModel = addModel(parcel.readInt() != 0 ? MeshModel.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddModel);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int netkey = setNetkey(parcel.readInt(), parcel.createIntArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(netkey);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    int appkey = setAppkey(parcel.readInt(), parcel.createIntArray(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(appkey);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    unProvisionScan(parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iInviteProvisioning = inviteProvisioning(parcel.createIntArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iInviteProvisioning);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStartProvisioning = startProvisioning(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createIntArray(), parcel.readInt(), parcel.readLong(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iStartProvisioning);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    int provisionFactor = setProvisionFactor(parcel.readInt(), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(provisionFactor);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    int i4 = parcel.readInt();
                    int i5 = parcel.readInt();
                    int i6 = parcel.readInt();
                    int i7 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        configMessageParamsCreateFromParcel = ConfigMessageParams.CREATOR.createFromParcel(parcel);
                    } else {
                        configMessageParamsCreateFromParcel = null;
                    }
                    int iSendConfigMessage = sendConfigMessage(i3, i4, i5, i6, i7, configMessageParamsCreateFromParcel);
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendConfigMessage);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSendPacket = sendPacket(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iSendPacket);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iPublishModel = publishModel(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iPublishModel);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    int meshMode = setMeshMode(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(meshMode);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zResetData = resetData(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zResetData ? 1 : 0);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zSaveData = saveData();
                    parcel2.writeNoException();
                    parcel2.writeInt(zSaveData ? 1 : 0);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    setData();
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    String version = getVersion();
                    parcel2.writeNoException();
                    parcel2.writeString(version);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    dump(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    int elementAddr2 = getElementAddr(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(elementAddr2);
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultTTL(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    int defaultTTL = getDefaultTTL();
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultTTL);
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iv = setIv(parcel.readLong(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iv);
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddDevKey = addDevKey(parcel.readInt(), parcel.createIntArray(), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddDevKey);
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] devKey = getDevKey(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(devKey);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iDelDevKey = delDevKey(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iDelDevKey);
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iModelAppBind = modelAppBind(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iModelAppBind);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i8 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        bluetoothMeshAccessRxMessageCreateFromParcel = BluetoothMeshAccessRxMessage.CREATOR.createFromParcel(parcel);
                    } else {
                        bluetoothMeshAccessRxMessageCreateFromParcel = null;
                    }
                    int iAccessModelReply = accessModelReply(i8, bluetoothMeshAccessRxMessageCreateFromParcel, parcel.readInt() != 0 ? BluetoothMeshAccessTxMessage.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAccessModelReply);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    setLogLevel(parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    int modelHandle = getModelHandle(parcel.readLong(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(modelHandle);
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iGattConnect = gattConnect(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iGattConnect);
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iGattDisconnect = gattDisconnect();
                    parcel2.writeNoException();
                    parcel2.writeInt(iGattDisconnect);
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    int heartbeatPeriod = setHeartbeatPeriod(parcel.readInt(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(heartbeatPeriod);
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] iArrOtaGetClientModelHandle = otaGetClientModelHandle();
                    parcel2.writeNoException();
                    parcel2.writeIntArray(iArrOtaGetClientModelHandle);
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iOtaInitiatorOperation = otaInitiatorOperation(parcel.readInt() != 0 ? OtaOperationParams.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iOtaInitiatorOperation);
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iBearerAdvSetParams = bearerAdvSetParams(parcel.readLong(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readLong(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iBearerAdvSetParams);
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    int scanParams = setScanParams(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(scanParams);
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    int specialPktParams = setSpecialPktParams(parcel.readInt() != 0, parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(specialPktParams);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IBluetoothMesh {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void registerCallback(IBluetoothMeshCallback iBluetoothMeshCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBluetoothMeshCallback != null ? iBluetoothMeshCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMeshRole() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getMeshState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int enable(MeshInitParams meshInitParams) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (meshInitParams != null) {
                        parcelObtain.writeInt(1);
                        meshInitParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int disable() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setCompositionDataHeader(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addElement(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setElementAddr(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addModel(MeshModel meshModel) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (meshModel != null) {
                        parcelObtain.writeInt(1);
                        meshModel.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setNetkey(int i, int[] iArr, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setAppkey(int i, int[] iArr, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unProvisionScan(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int inviteProvisioning(int[] iArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startProvisioning(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, long j, int i7, int i8, int i9) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(i5);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeInt(i6);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(i7);
                    parcelObtain.writeInt(i8);
                    parcelObtain.writeInt(i9);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setProvisionFactor(int i, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int sendConfigMessage(int i, int i2, int i3, int i4, int i5, ConfigMessageParams configMessageParams) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(i5);
                    if (configMessageParams != null) {
                        parcelObtain.writeInt(1);
                        configMessageParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int sendPacket(int i, int i2, int i3, int i4, int i5, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(i5);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int publishModel(int i, int i2, int i3, int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setMeshMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean resetData(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean saveData() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setData() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getVersion() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dump(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getElementAddr(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultTTL(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDefaultTTL() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setIv(long j, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addDevKey(int i, int[] iArr, int[] iArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeIntArray(iArr2);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getDevKey(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int delDevKey(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int modelAppBind(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int accessModelReply(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage, BluetoothMeshAccessTxMessage bluetoothMeshAccessTxMessage) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (bluetoothMeshAccessRxMessage != null) {
                        parcelObtain.writeInt(1);
                        bluetoothMeshAccessRxMessage.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bluetoothMeshAccessTxMessage != null) {
                        parcelObtain.writeInt(1);
                        bluetoothMeshAccessTxMessage.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setLogLevel(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getModelHandle(long j, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int gattConnect(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int gattDisconnect() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setHeartbeatPeriod(int i, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] otaGetClientModelHandle() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int otaInitiatorOperation(OtaOperationParams otaOperationParams) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (otaOperationParams != null) {
                        parcelObtain.writeInt(1);
                        otaOperationParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int bearerAdvSetParams(long j, int i, int i2, int i3, long j2, int i4, int i5) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(i5);
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setScanParams(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setSpecialPktParams(boolean z, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
