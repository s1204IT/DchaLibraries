package android.os;

public interface IBenesseExtensionService extends IInterface {

    public static abstract class Stub extends Binder implements IBenesseExtensionService {
        private static final String DESCRIPTOR = "android.os.IBenesseExtensionService";
        static final int TRANSACTION_checkPassword = 7;
        static final int TRANSACTION_getDchaState = 1;
        static final int TRANSACTION_getInt = 5;
        static final int TRANSACTION_getString = 3;
        static final int TRANSACTION_putInt = 6;
        static final int TRANSACTION_putString = 4;
        static final int TRANSACTION_setDchaState = 2;

        private static class Proxy implements IBenesseExtensionService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public boolean checkPassword(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_checkPassword, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDchaState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getDchaState, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getInt(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_getInt, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public String getString(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_getString, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean putInt(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_putInt, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean putString(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(Stub.TRANSACTION_putString, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDchaState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_setDchaState, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBenesseExtensionService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IBenesseExtensionService)) ? new Proxy(iBinder) : (IBenesseExtensionService) iInterfaceQueryLocalInterface;
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
                case TRANSACTION_getDchaState:
                    parcel.enforceInterface(DESCRIPTOR);
                    int dchaState = getDchaState();
                    parcel2.writeNoException();
                    parcel2.writeInt(dchaState);
                    return true;
                case TRANSACTION_setDchaState:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDchaState(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getString:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = getString(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(string);
                    return true;
                case TRANSACTION_putString:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zPutString = putString(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zPutString ? 1 : 0);
                    return true;
                case TRANSACTION_getInt:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = getInt(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(i3);
                    return true;
                case TRANSACTION_putInt:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zPutInt = putInt(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zPutInt ? 1 : 0);
                    return true;
                case TRANSACTION_checkPassword:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCheckPassword = checkPassword(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCheckPassword ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }

    boolean checkPassword(String str) throws RemoteException;

    int getDchaState() throws RemoteException;

    int getInt(String str) throws RemoteException;

    String getString(String str) throws RemoteException;

    boolean putInt(String str, int i) throws RemoteException;

    boolean putString(String str, String str2) throws RemoteException;

    void setDchaState(int i) throws RemoteException;
}
