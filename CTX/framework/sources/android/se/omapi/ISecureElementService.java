package android.se.omapi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.se.omapi.ISecureElementReader;

public interface ISecureElementService extends IInterface {
    ISecureElementReader getReader(String str) throws RemoteException;

    String[] getReaders() throws RemoteException;

    boolean[] isNFCEventAllowed(String str, byte[] bArr, String[] strArr) throws RemoteException;

    public static abstract class Stub extends Binder implements ISecureElementService {
        private static final String DESCRIPTOR = "android.se.omapi.ISecureElementService";
        static final int TRANSACTION_getReader = 2;
        static final int TRANSACTION_getReaders = 1;
        static final int TRANSACTION_isNFCEventAllowed = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISecureElementService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISecureElementService)) {
                return (ISecureElementService) iInterfaceQueryLocalInterface;
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
                    String[] readers = getReaders();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(readers);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    ISecureElementReader reader = getReader(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(reader != null ? reader.asBinder() : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean[] zArrIsNFCEventAllowed = isNFCEventAllowed(parcel.readString(), parcel.createByteArray(), parcel.createStringArray());
                    parcel2.writeNoException();
                    parcel2.writeBooleanArray(zArrIsNFCEventAllowed);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ISecureElementService {
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
            public String[] getReaders() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ISecureElementReader getReader(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return ISecureElementReader.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean[] isNFCEventAllowed(String str, byte[] bArr, String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createBooleanArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
