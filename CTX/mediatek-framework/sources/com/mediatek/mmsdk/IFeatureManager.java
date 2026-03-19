package com.mediatek.mmsdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFeatureManager extends IInterface {
    int getEffectFactory(BinderHolder binderHolder) throws RemoteException;

    String getParameter(String str) throws RemoteException;

    int setParameter(String str, String str2) throws RemoteException;

    int setUp(EffectHalVersion effectHalVersion) throws RemoteException;

    int tearDown(EffectHalVersion effectHalVersion) throws RemoteException;

    public static abstract class Stub extends Binder implements IFeatureManager {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.IFeatureManager";
        static final int TRANSACTION_getEffectFactory = 5;
        static final int TRANSACTION_getParameter = 2;
        static final int TRANSACTION_setParameter = 1;
        static final int TRANSACTION_setUp = 3;
        static final int TRANSACTION_tearDown = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFeatureManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IFeatureManager)) {
                return (IFeatureManager) iInterfaceQueryLocalInterface;
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
                    int parameter = setParameter(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(parameter);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String parameter2 = getParameter(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(parameter2);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int up = setUp(parcel.readInt() != 0 ? EffectHalVersion.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(up);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iTearDown = tearDown(parcel.readInt() != 0 ? EffectHalVersion.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iTearDown);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    BinderHolder binderHolder = new BinderHolder();
                    int effectFactory = getEffectFactory(binderHolder);
                    parcel2.writeNoException();
                    parcel2.writeInt(effectFactory);
                    parcel2.writeInt(1);
                    binderHolder.writeToParcel(parcel2, 1);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IFeatureManager {
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
            public int setParameter(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getParameter(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setUp(EffectHalVersion effectHalVersion) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (effectHalVersion != null) {
                        parcelObtain.writeInt(1);
                        effectHalVersion.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int tearDown(EffectHalVersion effectHalVersion) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (effectHalVersion != null) {
                        parcelObtain.writeInt(1);
                        effectHalVersion.writeToParcel(parcelObtain, 0);
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
            public int getEffectFactory(BinderHolder binderHolder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        binderHolder.readFromParcel(parcelObtain2);
                    }
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
