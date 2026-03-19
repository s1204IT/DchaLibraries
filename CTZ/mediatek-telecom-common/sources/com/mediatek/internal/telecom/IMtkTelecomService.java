package com.mediatek.internal.telecom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import java.util.List;

public interface IMtkTelecomService extends IInterface {
    List<PhoneAccountHandle> getAllPhoneAccountHandlesIncludingVirtual() throws RemoteException;

    List<PhoneAccount> getAllPhoneAccountsIncludingVirtual() throws RemoteException;

    boolean isInCall(String str) throws RemoteException;

    boolean isInVideoCall(String str) throws RemoteException;

    boolean isInVolteCall(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkTelecomService {
        private static final String DESCRIPTOR = "com.mediatek.internal.telecom.IMtkTelecomService";
        static final int TRANSACTION_getAllPhoneAccountHandlesIncludingVirtual = 4;
        static final int TRANSACTION_getAllPhoneAccountsIncludingVirtual = 3;
        static final int TRANSACTION_isInCall = 5;
        static final int TRANSACTION_isInVideoCall = 1;
        static final int TRANSACTION_isInVolteCall = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkTelecomService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkTelecomService)) {
                return (IMtkTelecomService) iInterfaceQueryLocalInterface;
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
                    boolean zIsInVideoCall = isInVideoCall(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInVideoCall ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInVolteCall = isInVolteCall(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInVolteCall ? 1 : 0);
                    return true;
                case TRANSACTION_getAllPhoneAccountsIncludingVirtual:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<PhoneAccount> allPhoneAccountsIncludingVirtual = getAllPhoneAccountsIncludingVirtual();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allPhoneAccountsIncludingVirtual);
                    return true;
                case TRANSACTION_getAllPhoneAccountHandlesIncludingVirtual:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<PhoneAccountHandle> allPhoneAccountHandlesIncludingVirtual = getAllPhoneAccountHandlesIncludingVirtual();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allPhoneAccountHandlesIncludingVirtual);
                    return true;
                case TRANSACTION_isInCall:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInCall = isInCall(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInCall ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkTelecomService {
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
            public boolean isInVideoCall(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInVolteCall(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<PhoneAccount> getAllPhoneAccountsIncludingVirtual() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getAllPhoneAccountsIncludingVirtual, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(PhoneAccount.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<PhoneAccountHandle> getAllPhoneAccountHandlesIncludingVirtual() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getAllPhoneAccountHandlesIncludingVirtual, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(PhoneAccountHandle.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInCall(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_isInCall, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
