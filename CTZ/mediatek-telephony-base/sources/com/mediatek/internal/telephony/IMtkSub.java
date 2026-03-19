package com.mediatek.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMtkSub extends IInterface {
    int getSubIdUsingPhoneId(int i) throws RemoteException;

    MtkSubscriptionInfo getSubInfo(String str, int i) throws RemoteException;

    MtkSubscriptionInfo getSubInfoForIccId(String str, String str2) throws RemoteException;

    void setDefaultDataSubIdWithoutCapabilitySwitch(int i) throws RemoteException;

    void setDefaultFallbackSubId(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkSub {
        private static final String DESCRIPTOR = "com.mediatek.internal.telephony.IMtkSub";
        static final int TRANSACTION_getSubIdUsingPhoneId = 3;
        static final int TRANSACTION_getSubInfo = 1;
        static final int TRANSACTION_getSubInfoForIccId = 2;
        static final int TRANSACTION_setDefaultDataSubIdWithoutCapabilitySwitch = 5;
        static final int TRANSACTION_setDefaultFallbackSubId = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkSub asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkSub)) {
                return (IMtkSub) iInterfaceQueryLocalInterface;
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
                    MtkSubscriptionInfo subInfo = getSubInfo(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (subInfo != null) {
                        parcel2.writeInt(1);
                        subInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    MtkSubscriptionInfo subInfoForIccId = getSubInfoForIccId(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    if (subInfoForIccId != null) {
                        parcel2.writeInt(1);
                        subInfoForIccId.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int subIdUsingPhoneId = getSubIdUsingPhoneId(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(subIdUsingPhoneId);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultFallbackSubId(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDefaultDataSubIdWithoutCapabilitySwitch(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkSub {
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
            public MtkSubscriptionInfo getSubInfo(String str, int i) throws RemoteException {
                MtkSubscriptionInfo mtkSubscriptionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        mtkSubscriptionInfoCreateFromParcel = MtkSubscriptionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        mtkSubscriptionInfoCreateFromParcel = null;
                    }
                    return mtkSubscriptionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public MtkSubscriptionInfo getSubInfoForIccId(String str, String str2) throws RemoteException {
                MtkSubscriptionInfo mtkSubscriptionInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        mtkSubscriptionInfoCreateFromParcel = MtkSubscriptionInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        mtkSubscriptionInfoCreateFromParcel = null;
                    }
                    return mtkSubscriptionInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getSubIdUsingPhoneId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultFallbackSubId(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDefaultDataSubIdWithoutCapabilitySwitch(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
