package android.os;

public interface IStatsManager extends IInterface {
    void addConfiguration(long j, byte[] bArr, String str) throws RemoteException;

    byte[] getData(long j, String str) throws RemoteException;

    byte[] getMetadata(String str) throws RemoteException;

    void informAlarmForSubscriberTriggeringFired() throws RemoteException;

    void informAllUidData(int[] iArr, long[] jArr, String[] strArr) throws RemoteException;

    void informAnomalyAlarmFired() throws RemoteException;

    void informDeviceShutdown() throws RemoteException;

    void informOnePackage(String str, int i, long j) throws RemoteException;

    void informOnePackageRemoved(String str, int i) throws RemoteException;

    void informPollAlarmFired() throws RemoteException;

    void removeConfiguration(long j, String str) throws RemoteException;

    void removeDataFetchOperation(long j, String str) throws RemoteException;

    void sendAppBreadcrumbAtom(int i, int i2) throws RemoteException;

    void setBroadcastSubscriber(long j, long j2, IBinder iBinder, String str) throws RemoteException;

    void setDataFetchOperation(long j, IBinder iBinder, String str) throws RemoteException;

    void statsCompanionReady() throws RemoteException;

    void systemRunning() throws RemoteException;

    void unsetBroadcastSubscriber(long j, long j2, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IStatsManager {
        private static final String DESCRIPTOR = "android.os.IStatsManager";
        static final int TRANSACTION_addConfiguration = 12;
        static final int TRANSACTION_getData = 10;
        static final int TRANSACTION_getMetadata = 11;
        static final int TRANSACTION_informAlarmForSubscriberTriggeringFired = 5;
        static final int TRANSACTION_informAllUidData = 7;
        static final int TRANSACTION_informAnomalyAlarmFired = 3;
        static final int TRANSACTION_informDeviceShutdown = 6;
        static final int TRANSACTION_informOnePackage = 8;
        static final int TRANSACTION_informOnePackageRemoved = 9;
        static final int TRANSACTION_informPollAlarmFired = 4;
        static final int TRANSACTION_removeConfiguration = 15;
        static final int TRANSACTION_removeDataFetchOperation = 14;
        static final int TRANSACTION_sendAppBreadcrumbAtom = 18;
        static final int TRANSACTION_setBroadcastSubscriber = 16;
        static final int TRANSACTION_setDataFetchOperation = 13;
        static final int TRANSACTION_statsCompanionReady = 2;
        static final int TRANSACTION_systemRunning = 1;
        static final int TRANSACTION_unsetBroadcastSubscriber = 17;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatsManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IStatsManager)) {
                return (IStatsManager) iInterfaceQueryLocalInterface;
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
                    systemRunning();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    statsCompanionReady();
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    informAnomalyAlarmFired();
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    informPollAlarmFired();
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    informAlarmForSubscriberTriggeringFired();
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    informDeviceShutdown();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    informAllUidData(parcel.createIntArray(), parcel.createLongArray(), parcel.createStringArray());
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    informOnePackage(parcel.readString(), parcel.readInt(), parcel.readLong());
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    informOnePackageRemoved(parcel.readString(), parcel.readInt());
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] data = getData(parcel.readLong(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(data);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] metadata = getMetadata(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(metadata);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    addConfiguration(parcel.readLong(), parcel.createByteArray(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDataFetchOperation(parcel.readLong(), parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeDataFetchOperation(parcel.readLong(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeConfiguration(parcel.readLong(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    setBroadcastSubscriber(parcel.readLong(), parcel.readLong(), parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    unsetBroadcastSubscriber(parcel.readLong(), parcel.readLong(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendAppBreadcrumbAtom(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IStatsManager {
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
            public void systemRunning() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void statsCompanionReady() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informAnomalyAlarmFired() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informPollAlarmFired() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informAlarmForSubscriberTriggeringFired() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informDeviceShutdown() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informAllUidData(int[] iArr, long[] jArr, String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeLongArray(jArr);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informOnePackage(String str, int i, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void informOnePackageRemoved(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getData(long j, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] getMetadata(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addConfiguration(long j, byte[] bArr, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDataFetchOperation(long j, IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeDataFetchOperation(long j, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeConfiguration(long j, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setBroadcastSubscriber(long j, long j2, IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unsetBroadcastSubscriber(long j, long j2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendAppBreadcrumbAtom(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
