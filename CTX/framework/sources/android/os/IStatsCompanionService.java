package android.os;

public interface IStatsCompanionService extends IInterface {
    void cancelAlarmForSubscriberTriggering() throws RemoteException;

    void cancelAnomalyAlarm() throws RemoteException;

    void cancelPullingAlarm() throws RemoteException;

    StatsLogEventWrapper[] pullData(int i) throws RemoteException;

    void sendDataBroadcast(IBinder iBinder, long j) throws RemoteException;

    void sendSubscriberBroadcast(IBinder iBinder, long j, long j2, long j3, long j4, String[] strArr, StatsDimensionsValue statsDimensionsValue) throws RemoteException;

    void setAlarmForSubscriberTriggering(long j) throws RemoteException;

    void setAnomalyAlarm(long j) throws RemoteException;

    void setPullingAlarm(long j) throws RemoteException;

    void statsdReady() throws RemoteException;

    void triggerUidSnapshot() throws RemoteException;

    public static abstract class Stub extends Binder implements IStatsCompanionService {
        private static final String DESCRIPTOR = "android.os.IStatsCompanionService";
        static final int TRANSACTION_cancelAlarmForSubscriberTriggering = 7;
        static final int TRANSACTION_cancelAnomalyAlarm = 3;
        static final int TRANSACTION_cancelPullingAlarm = 5;
        static final int TRANSACTION_pullData = 8;
        static final int TRANSACTION_sendDataBroadcast = 9;
        static final int TRANSACTION_sendSubscriberBroadcast = 10;
        static final int TRANSACTION_setAlarmForSubscriberTriggering = 6;
        static final int TRANSACTION_setAnomalyAlarm = 2;
        static final int TRANSACTION_setPullingAlarm = 4;
        static final int TRANSACTION_statsdReady = 1;
        static final int TRANSACTION_triggerUidSnapshot = 11;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatsCompanionService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IStatsCompanionService)) {
                return (IStatsCompanionService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            StatsDimensionsValue statsDimensionsValueCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    statsdReady();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAnomalyAlarm(parcel.readLong());
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelAnomalyAlarm();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPullingAlarm(parcel.readLong());
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelPullingAlarm();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAlarmForSubscriberTriggering(parcel.readLong());
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelAlarmForSubscriberTriggering();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatsLogEventWrapper[] statsLogEventWrapperArrPullData = pullData(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(statsLogEventWrapperArrPullData, 1);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendDataBroadcast(parcel.readStrongBinder(), parcel.readLong());
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder = parcel.readStrongBinder();
                    long j = parcel.readLong();
                    long j2 = parcel.readLong();
                    long j3 = parcel.readLong();
                    long j4 = parcel.readLong();
                    String[] strArrCreateStringArray = parcel.createStringArray();
                    if (parcel.readInt() != 0) {
                        statsDimensionsValueCreateFromParcel = StatsDimensionsValue.CREATOR.createFromParcel(parcel);
                    } else {
                        statsDimensionsValueCreateFromParcel = null;
                    }
                    sendSubscriberBroadcast(strongBinder, j, j2, j3, j4, strArrCreateStringArray, statsDimensionsValueCreateFromParcel);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    triggerUidSnapshot();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IStatsCompanionService {
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
            public void statsdReady() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAnomalyAlarm(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelAnomalyAlarm() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPullingAlarm(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelPullingAlarm() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAlarmForSubscriberTriggering(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelAlarmForSubscriberTriggering() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatsLogEventWrapper[] pullData(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (StatsLogEventWrapper[]) parcelObtain2.createTypedArray(StatsLogEventWrapper.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendDataBroadcast(IBinder iBinder, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendSubscriberBroadcast(IBinder iBinder, long j, long j2, long j3, long j4, String[] strArr, StatsDimensionsValue statsDimensionsValue) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeLong(j3);
                    parcelObtain.writeLong(j4);
                    parcelObtain.writeStringArray(strArr);
                    if (statsDimensionsValue != null) {
                        parcelObtain.writeInt(1);
                        statsDimensionsValue.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void triggerUidSnapshot() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
