package jp.co.benesse.dcha.databox;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISbox extends IInterface {

    public static class Default implements ISbox {
        @Override
        public IBinder asBinder() {
            return null;
        }

        @Override
        public String getAppIdentifier(int i) throws RemoteException {
            return null;
        }

        @Override
        public String getArrayValues(String str) throws RemoteException {
            return null;
        }

        @Override
        public String getAuthUrl(int i) throws RemoteException {
            return null;
        }

        @Override
        public String getStringValue(String str) throws RemoteException {
            return null;
        }

        @Override
        public void setArrayValues(String str, String str2) throws RemoteException {
        }

        @Override
        public void setStringValue(String str, String str2) throws RemoteException {
        }
    }

    String getAppIdentifier(int i) throws RemoteException;

    String getArrayValues(String str) throws RemoteException;

    String getAuthUrl(int i) throws RemoteException;

    String getStringValue(String str) throws RemoteException;

    void setArrayValues(String str, String str2) throws RemoteException;

    void setStringValue(String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements ISbox {
        private static final String DESCRIPTOR = "jp.co.benesse.dcha.databox.ISbox";
        static final int TRANSACTION_getAppIdentifier = 5;
        static final int TRANSACTION_getArrayValues = 3;
        static final int TRANSACTION_getAuthUrl = 6;
        static final int TRANSACTION_getStringValue = 1;
        static final int TRANSACTION_setArrayValues = 4;
        static final int TRANSACTION_setStringValue = 2;

        @Override
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISbox asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISbox)) {
                return (ISbox) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case TRANSACTION_getStringValue:
                    parcel.enforceInterface(DESCRIPTOR);
                    String stringValue = getStringValue(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(stringValue);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    setStringValue(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String arrayValues = getArrayValues(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(arrayValues);
                    return true;
                case TRANSACTION_setArrayValues:
                    parcel.enforceInterface(DESCRIPTOR);
                    setArrayValues(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getAppIdentifier:
                    parcel.enforceInterface(DESCRIPTOR);
                    String appIdentifier = getAppIdentifier(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(appIdentifier);
                    return true;
                case TRANSACTION_getAuthUrl:
                    parcel.enforceInterface(DESCRIPTOR);
                    String authUrl = getAuthUrl(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(authUrl);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ISbox {
            public static ISbox sDefaultImpl;
            private IBinder mRemote;

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public String getStringValue(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getStringValue, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getStringValue(str);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setStringValue(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (!this.mRemote.transact(2, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setStringValue(str, str2);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getArrayValues(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (!this.mRemote.transact(3, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getArrayValues(str);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setArrayValues(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (!this.mRemote.transact(Stub.TRANSACTION_setArrayValues, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        Stub.getDefaultImpl().setArrayValues(str, str2);
                    } else {
                        parcelObtain2.readException();
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getAppIdentifier(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getAppIdentifier, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAppIdentifier(i);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getAuthUrl(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (!this.mRemote.transact(Stub.TRANSACTION_getAuthUrl, parcelObtain, parcelObtain2, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAuthUrl(i);
                    }
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public static boolean setDefaultImpl(ISbox iSbox) {
            if (Proxy.sDefaultImpl != null || iSbox == null) {
                return false;
            }
            Proxy.sDefaultImpl = iSbox;
            return true;
        }

        public static ISbox getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
