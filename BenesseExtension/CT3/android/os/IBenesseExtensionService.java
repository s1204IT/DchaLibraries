package android.os;

/* loaded from: a04br3-02.05.000-framework.jar:android/os/IBenesseExtensionService.class */
public interface IBenesseExtensionService extends IInterface {

    /* loaded from: a04br3-02.05.000-framework.jar:android/os/IBenesseExtensionService$Stub.class */
    public static abstract class Stub extends Binder implements IBenesseExtensionService {
        private static final String DESCRIPTOR = "android.os.IBenesseExtensionService";
        static final int TRANSACTION_checkPassword = 5;
        static final int TRANSACTION_checkUsbCam = 4;
        static final int TRANSACTION_getDchaState = 1;
        static final int TRANSACTION_getString = 3;
        static final int TRANSACTION_setDchaState = 2;

        /* loaded from: a04br3-02.05.000-framework.jar:android/os/IBenesseExtensionService$Stub$Proxy.class */
        private static class Proxy implements IBenesseExtensionService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.os.IBenesseExtensionService
            public boolean checkPassword(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    boolean z = parcelObtain2.readInt() != 0;
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                    return z;
                } catch (Throwable th) {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                    throw th;
                }
            }

            @Override // android.os.IBenesseExtensionService
            public boolean checkUsbCam() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    boolean z = parcelObtain2.readInt() != 0;
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                    return z;
                } catch (Throwable th) {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                    throw th;
                }
            }

            @Override // android.os.IBenesseExtensionService
            public int getDchaState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
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

            @Override // android.os.IBenesseExtensionService
            public String getString(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // android.os.IBenesseExtensionService
            public void setDchaState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
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

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int dchaState = getDchaState();
                    parcel2.writeNoException();
                    parcel2.writeInt(dchaState);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDchaState(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = getString(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(string);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCheckUsbCam = checkUsbCam();
                    parcel2.writeNoException();
                    int i3 = 0;
                    if (zCheckUsbCam) {
                        i3 = 1;
                    }
                    parcel2.writeInt(i3);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zCheckPassword = checkPassword(parcel.readString());
                    parcel2.writeNoException();
                    int i4 = 0;
                    if (zCheckPassword) {
                        i4 = 1;
                    }
                    parcel2.writeInt(i4);
                    return true;
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }

    boolean checkPassword(String str) throws RemoteException;

    boolean checkUsbCam() throws RemoteException;

    int getDchaState() throws RemoteException;

    String getString(String str) throws RemoteException;

    void setDchaState(int i) throws RemoteException;
}