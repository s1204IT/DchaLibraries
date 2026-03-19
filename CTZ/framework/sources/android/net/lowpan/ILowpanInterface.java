package android.net.lowpan;

import android.net.IpPrefix;
import android.net.lowpan.ILowpanEnergyScanCallback;
import android.net.lowpan.ILowpanInterfaceListener;
import android.net.lowpan.ILowpanNetScanCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.Map;

public interface ILowpanInterface extends IInterface {
    public static final int ERROR_ALREADY = 9;
    public static final int ERROR_BUSY = 8;
    public static final int ERROR_CANCELED = 10;
    public static final int ERROR_DISABLED = 3;
    public static final int ERROR_FEATURE_NOT_SUPPORTED = 11;
    public static final int ERROR_FORM_FAILED_AT_SCAN = 15;
    public static final int ERROR_INVALID_ARGUMENT = 2;
    public static final int ERROR_IO_FAILURE = 6;
    public static final int ERROR_JOIN_FAILED_AT_AUTH = 14;
    public static final int ERROR_JOIN_FAILED_AT_SCAN = 13;
    public static final int ERROR_JOIN_FAILED_UNKNOWN = 12;
    public static final int ERROR_NCP_PROBLEM = 7;
    public static final int ERROR_TIMEOUT = 5;
    public static final int ERROR_UNSPECIFIED = 1;
    public static final int ERROR_WRONG_STATE = 4;
    public static final String KEY_CHANNEL_MASK = "android.net.lowpan.property.CHANNEL_MASK";
    public static final String KEY_MAX_TX_POWER = "android.net.lowpan.property.MAX_TX_POWER";
    public static final String NETWORK_TYPE_THREAD_V1 = "org.threadgroup.thread.v1";
    public static final String NETWORK_TYPE_UNKNOWN = "unknown";
    public static final String PERM_ACCESS_LOWPAN_STATE = "android.permission.ACCESS_LOWPAN_STATE";
    public static final String PERM_CHANGE_LOWPAN_STATE = "android.permission.CHANGE_LOWPAN_STATE";
    public static final String PERM_READ_LOWPAN_CREDENTIAL = "android.permission.READ_LOWPAN_CREDENTIAL";
    public static final String ROLE_COORDINATOR = "coordinator";
    public static final String ROLE_DETACHED = "detached";
    public static final String ROLE_END_DEVICE = "end-device";
    public static final String ROLE_LEADER = "leader";
    public static final String ROLE_ROUTER = "router";
    public static final String ROLE_SLEEPY_END_DEVICE = "sleepy-end-device";
    public static final String ROLE_SLEEPY_ROUTER = "sleepy-router";
    public static final String STATE_ATTACHED = "attached";
    public static final String STATE_ATTACHING = "attaching";
    public static final String STATE_COMMISSIONING = "commissioning";
    public static final String STATE_FAULT = "fault";
    public static final String STATE_OFFLINE = "offline";

    void addExternalRoute(IpPrefix ipPrefix, int i) throws RemoteException;

    void addListener(ILowpanInterfaceListener iLowpanInterfaceListener) throws RemoteException;

    void addOnMeshPrefix(IpPrefix ipPrefix, int i) throws RemoteException;

    void attach(LowpanProvision lowpanProvision) throws RemoteException;

    void beginLowPower() throws RemoteException;

    void closeCommissioningSession() throws RemoteException;

    void form(LowpanProvision lowpanProvision) throws RemoteException;

    String getDriverVersion() throws RemoteException;

    byte[] getExtendedAddress() throws RemoteException;

    String[] getLinkAddresses() throws RemoteException;

    IpPrefix[] getLinkNetworks() throws RemoteException;

    LowpanCredential getLowpanCredential() throws RemoteException;

    LowpanIdentity getLowpanIdentity() throws RemoteException;

    byte[] getMacAddress() throws RemoteException;

    String getName() throws RemoteException;

    String getNcpVersion() throws RemoteException;

    String getPartitionId() throws RemoteException;

    String getRole() throws RemoteException;

    String getState() throws RemoteException;

    LowpanChannelInfo[] getSupportedChannels() throws RemoteException;

    String[] getSupportedNetworkTypes() throws RemoteException;

    boolean isCommissioned() throws RemoteException;

    boolean isConnected() throws RemoteException;

    boolean isEnabled() throws RemoteException;

    boolean isUp() throws RemoteException;

    void join(LowpanProvision lowpanProvision) throws RemoteException;

    void leave() throws RemoteException;

    void onHostWake() throws RemoteException;

    void pollForData() throws RemoteException;

    void removeExternalRoute(IpPrefix ipPrefix) throws RemoteException;

    void removeListener(ILowpanInterfaceListener iLowpanInterfaceListener) throws RemoteException;

    void removeOnMeshPrefix(IpPrefix ipPrefix) throws RemoteException;

    void reset() throws RemoteException;

    void sendToCommissioner(byte[] bArr) throws RemoteException;

    void setEnabled(boolean z) throws RemoteException;

    void startCommissioningSession(LowpanBeaconInfo lowpanBeaconInfo) throws RemoteException;

    void startEnergyScan(Map map, ILowpanEnergyScanCallback iLowpanEnergyScanCallback) throws RemoteException;

    void startNetScan(Map map, ILowpanNetScanCallback iLowpanNetScanCallback) throws RemoteException;

    void stopEnergyScan() throws RemoteException;

    void stopNetScan() throws RemoteException;

    public static abstract class Stub extends Binder implements ILowpanInterface {
        private static final String DESCRIPTOR = "android.net.lowpan.ILowpanInterface";
        static final int TRANSACTION_addExternalRoute = 39;
        static final int TRANSACTION_addListener = 31;
        static final int TRANSACTION_addOnMeshPrefix = 37;
        static final int TRANSACTION_attach = 22;
        static final int TRANSACTION_beginLowPower = 28;
        static final int TRANSACTION_closeCommissioningSession = 26;
        static final int TRANSACTION_form = 21;
        static final int TRANSACTION_getDriverVersion = 3;
        static final int TRANSACTION_getExtendedAddress = 15;
        static final int TRANSACTION_getLinkAddresses = 18;
        static final int TRANSACTION_getLinkNetworks = 19;
        static final int TRANSACTION_getLowpanCredential = 17;
        static final int TRANSACTION_getLowpanIdentity = 16;
        static final int TRANSACTION_getMacAddress = 6;
        static final int TRANSACTION_getName = 1;
        static final int TRANSACTION_getNcpVersion = 2;
        static final int TRANSACTION_getPartitionId = 14;
        static final int TRANSACTION_getRole = 13;
        static final int TRANSACTION_getState = 12;
        static final int TRANSACTION_getSupportedChannels = 4;
        static final int TRANSACTION_getSupportedNetworkTypes = 5;
        static final int TRANSACTION_isCommissioned = 10;
        static final int TRANSACTION_isConnected = 11;
        static final int TRANSACTION_isEnabled = 7;
        static final int TRANSACTION_isUp = 9;
        static final int TRANSACTION_join = 20;
        static final int TRANSACTION_leave = 23;
        static final int TRANSACTION_onHostWake = 30;
        static final int TRANSACTION_pollForData = 29;
        static final int TRANSACTION_removeExternalRoute = 40;
        static final int TRANSACTION_removeListener = 32;
        static final int TRANSACTION_removeOnMeshPrefix = 38;
        static final int TRANSACTION_reset = 24;
        static final int TRANSACTION_sendToCommissioner = 27;
        static final int TRANSACTION_setEnabled = 8;
        static final int TRANSACTION_startCommissioningSession = 25;
        static final int TRANSACTION_startEnergyScan = 35;
        static final int TRANSACTION_startNetScan = 33;
        static final int TRANSACTION_stopEnergyScan = 36;
        static final int TRANSACTION_stopNetScan = 34;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ILowpanInterface asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ILowpanInterface)) {
                return (ILowpanInterface) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    String name = getName();
                    parcel2.writeNoException();
                    parcel2.writeString(name);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String ncpVersion = getNcpVersion();
                    parcel2.writeNoException();
                    parcel2.writeString(ncpVersion);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String driverVersion = getDriverVersion();
                    parcel2.writeNoException();
                    parcel2.writeString(driverVersion);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    LowpanChannelInfo[] supportedChannels = getSupportedChannels();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(supportedChannels, 1);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] supportedNetworkTypes = getSupportedNetworkTypes();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(supportedNetworkTypes);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] macAddress = getMacAddress();
                    parcel2.writeNoException();
                    parcel2.writeByteArray(macAddress);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsEnabled = isEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsEnabled ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setEnabled(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsUp = isUp();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsUp ? 1 : 0);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsCommissioned = isCommissioned();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsCommissioned ? 1 : 0);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsConnected = isConnected();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsConnected ? 1 : 0);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    String state = getState();
                    parcel2.writeNoException();
                    parcel2.writeString(state);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    String role = getRole();
                    parcel2.writeNoException();
                    parcel2.writeString(role);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    String partitionId = getPartitionId();
                    parcel2.writeNoException();
                    parcel2.writeString(partitionId);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] extendedAddress = getExtendedAddress();
                    parcel2.writeNoException();
                    parcel2.writeByteArray(extendedAddress);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    LowpanIdentity lowpanIdentity = getLowpanIdentity();
                    parcel2.writeNoException();
                    if (lowpanIdentity != null) {
                        parcel2.writeInt(1);
                        lowpanIdentity.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    LowpanCredential lowpanCredential = getLowpanCredential();
                    parcel2.writeNoException();
                    if (lowpanCredential != null) {
                        parcel2.writeInt(1);
                        lowpanCredential.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] linkAddresses = getLinkAddresses();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(linkAddresses);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    IpPrefix[] linkNetworks = getLinkNetworks();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(linkNetworks, 1);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    join(parcel.readInt() != 0 ? LowpanProvision.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    form(parcel.readInt() != 0 ? LowpanProvision.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    attach(parcel.readInt() != 0 ? LowpanProvision.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    leave();
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    reset();
                    parcel2.writeNoException();
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    startCommissioningSession(parcel.readInt() != 0 ? LowpanBeaconInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    closeCommissioningSession();
                    parcel2.writeNoException();
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendToCommissioner(parcel.createByteArray());
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    beginLowPower();
                    parcel2.writeNoException();
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    pollForData();
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    onHostWake();
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    addListener(ILowpanInterfaceListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeListener(ILowpanInterfaceListener.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    startNetScan(parcel.readHashMap(getClass().getClassLoader()), ILowpanNetScanCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopNetScan();
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    startEnergyScan(parcel.readHashMap(getClass().getClassLoader()), ILowpanEnergyScanCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopEnergyScan();
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    addOnMeshPrefix(parcel.readInt() != 0 ? IpPrefix.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeOnMeshPrefix(parcel.readInt() != 0 ? IpPrefix.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    addExternalRoute(parcel.readInt() != 0 ? IpPrefix.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeExternalRoute(parcel.readInt() != 0 ? IpPrefix.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ILowpanInterface {
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
            public String getName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getNcpVersion() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getDriverVersion() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public LowpanChannelInfo[] getSupportedChannels() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (LowpanChannelInfo[]) parcelObtain2.createTypedArray(LowpanChannelInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getSupportedNetworkTypes() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getMacAddress() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setEnabled(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isUp() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isCommissioned() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isConnected() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getRole() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getPartitionId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getExtendedAddress() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public LowpanIdentity getLowpanIdentity() throws RemoteException {
                LowpanIdentity lowpanIdentityCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        lowpanIdentityCreateFromParcel = LowpanIdentity.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        lowpanIdentityCreateFromParcel = null;
                    }
                    return lowpanIdentityCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public LowpanCredential getLowpanCredential() throws RemoteException {
                LowpanCredential lowpanCredentialCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        lowpanCredentialCreateFromParcel = LowpanCredential.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        lowpanCredentialCreateFromParcel = null;
                    }
                    return lowpanCredentialCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getLinkAddresses() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IpPrefix[] getLinkNetworks() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (IpPrefix[]) parcelObtain2.createTypedArray(IpPrefix.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void join(LowpanProvision lowpanProvision) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lowpanProvision != null) {
                        parcelObtain.writeInt(1);
                        lowpanProvision.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void form(LowpanProvision lowpanProvision) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lowpanProvision != null) {
                        parcelObtain.writeInt(1);
                        lowpanProvision.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void attach(LowpanProvision lowpanProvision) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lowpanProvision != null) {
                        parcelObtain.writeInt(1);
                        lowpanProvision.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void leave() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reset() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startCommissioningSession(LowpanBeaconInfo lowpanBeaconInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lowpanBeaconInfo != null) {
                        parcelObtain.writeInt(1);
                        lowpanBeaconInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closeCommissioningSession() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendToCommissioner(byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(27, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void beginLowPower() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void pollForData() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onHostWake() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addListener(ILowpanInterfaceListener iLowpanInterfaceListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iLowpanInterfaceListener != null ? iLowpanInterfaceListener.asBinder() : null);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeListener(ILowpanInterfaceListener iLowpanInterfaceListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iLowpanInterfaceListener != null ? iLowpanInterfaceListener.asBinder() : null);
                    this.mRemote.transact(32, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startNetScan(Map map, ILowpanNetScanCallback iLowpanNetScanCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeMap(map);
                    parcelObtain.writeStrongBinder(iLowpanNetScanCallback != null ? iLowpanNetScanCallback.asBinder() : null);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopNetScan() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(34, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startEnergyScan(Map map, ILowpanEnergyScanCallback iLowpanEnergyScanCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeMap(map);
                    parcelObtain.writeStrongBinder(iLowpanEnergyScanCallback != null ? iLowpanEnergyScanCallback.asBinder() : null);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopEnergyScan() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addOnMeshPrefix(IpPrefix ipPrefix, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ipPrefix != null) {
                        parcelObtain.writeInt(1);
                        ipPrefix.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeOnMeshPrefix(IpPrefix ipPrefix) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ipPrefix != null) {
                        parcelObtain.writeInt(1);
                        ipPrefix.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(38, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addExternalRoute(IpPrefix ipPrefix, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ipPrefix != null) {
                        parcelObtain.writeInt(1);
                        ipPrefix.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeExternalRoute(IpPrefix ipPrefix) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (ipPrefix != null) {
                        parcelObtain.writeInt(1);
                        ipPrefix.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(40, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
