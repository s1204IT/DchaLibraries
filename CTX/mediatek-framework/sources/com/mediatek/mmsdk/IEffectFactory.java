package com.mediatek.mmsdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface IEffectFactory extends IInterface {
    int createCallbackClient(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException;

    int createEffectHal(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException;

    int createEffectHalClient(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException;

    int getAllSupportedEffectHal(List<String> list) throws RemoteException;

    int getSupportedVersion(String str, List<EffectHalVersion> list) throws RemoteException;

    public static abstract class Stub extends Binder implements IEffectFactory {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.IEffectFactory";
        static final int TRANSACTION_createCallbackClient = 1;
        static final int TRANSACTION_createEffectHal = 2;
        static final int TRANSACTION_createEffectHalClient = 3;
        static final int TRANSACTION_getAllSupportedEffectHal = 5;
        static final int TRANSACTION_getSupportedVersion = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEffectFactory asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IEffectFactory)) {
                return (IEffectFactory) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            EffectHalVersion effectHalVersionCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    effectHalVersionCreateFromParcel = parcel.readInt() != 0 ? EffectHalVersion.CREATOR.createFromParcel(parcel) : null;
                    BinderHolder binderHolder = new BinderHolder();
                    int iCreateCallbackClient = createCallbackClient(effectHalVersionCreateFromParcel, binderHolder);
                    parcel2.writeNoException();
                    parcel2.writeInt(iCreateCallbackClient);
                    parcel2.writeInt(1);
                    binderHolder.writeToParcel(parcel2, 1);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    effectHalVersionCreateFromParcel = parcel.readInt() != 0 ? EffectHalVersion.CREATOR.createFromParcel(parcel) : null;
                    BinderHolder binderHolder2 = new BinderHolder();
                    int iCreateEffectHal = createEffectHal(effectHalVersionCreateFromParcel, binderHolder2);
                    parcel2.writeNoException();
                    parcel2.writeInt(iCreateEffectHal);
                    parcel2.writeInt(1);
                    binderHolder2.writeToParcel(parcel2, 1);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    effectHalVersionCreateFromParcel = parcel.readInt() != 0 ? EffectHalVersion.CREATOR.createFromParcel(parcel) : null;
                    BinderHolder binderHolder3 = new BinderHolder();
                    int iCreateEffectHalClient = createEffectHalClient(effectHalVersionCreateFromParcel, binderHolder3);
                    parcel2.writeNoException();
                    parcel2.writeInt(iCreateEffectHalClient);
                    parcel2.writeInt(1);
                    binderHolder3.writeToParcel(parcel2, 1);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    ArrayList arrayList = new ArrayList();
                    int supportedVersion = getSupportedVersion(string, arrayList);
                    parcel2.writeNoException();
                    parcel2.writeInt(supportedVersion);
                    parcel2.writeTypedList(arrayList);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    ArrayList arrayList2 = new ArrayList();
                    int allSupportedEffectHal = getAllSupportedEffectHal(arrayList2);
                    parcel2.writeNoException();
                    parcel2.writeInt(allSupportedEffectHal);
                    parcel2.writeStringList(arrayList2);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IEffectFactory {
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
            public int createCallbackClient(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException {
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
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
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

            @Override
            public int createEffectHal(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException {
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
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
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

            @Override
            public int createEffectHalClient(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException {
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

            @Override
            public int getSupportedVersion(String str, List<EffectHalVersion> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    parcelObtain2.readTypedList(list, EffectHalVersion.CREATOR);
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getAllSupportedEffectHal(List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    parcelObtain2.readStringList(list);
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
