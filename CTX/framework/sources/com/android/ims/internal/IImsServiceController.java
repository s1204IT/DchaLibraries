package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsMMTelFeature;
import com.android.ims.internal.IImsRcsFeature;

public interface IImsServiceController extends IInterface {
    IImsMMTelFeature createEmergencyMMTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    IImsMMTelFeature createMMTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    IImsRcsFeature createRcsFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsServiceController {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsServiceController";
        static final int TRANSACTION_createEmergencyMMTelFeature = 1;
        static final int TRANSACTION_createMMTelFeature = 2;
        static final int TRANSACTION_createRcsFeature = 3;
        static final int TRANSACTION_removeImsFeature = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsServiceController asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IImsServiceController)) {
                return (IImsServiceController) iInterfaceQueryLocalInterface;
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
                    IImsMMTelFeature iImsMMTelFeatureCreateEmergencyMMTelFeature = createEmergencyMMTelFeature(parcel.readInt(), IImsFeatureStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iImsMMTelFeatureCreateEmergencyMMTelFeature != null ? iImsMMTelFeatureCreateEmergencyMMTelFeature.asBinder() : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsMMTelFeature iImsMMTelFeatureCreateMMTelFeature = createMMTelFeature(parcel.readInt(), IImsFeatureStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iImsMMTelFeatureCreateMMTelFeature != null ? iImsMMTelFeatureCreateMMTelFeature.asBinder() : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsRcsFeature iImsRcsFeatureCreateRcsFeature = createRcsFeature(parcel.readInt(), IImsFeatureStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iImsRcsFeatureCreateRcsFeature != null ? iImsRcsFeatureCreateRcsFeature.asBinder() : null);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeImsFeature(parcel.readInt(), parcel.readInt(), IImsFeatureStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IImsServiceController {
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
            public IImsMMTelFeature createEmergencyMMTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iImsFeatureStatusCallback != null ? iImsFeatureStatusCallback.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsMMTelFeature.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsMMTelFeature createMMTelFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iImsFeatureStatusCallback != null ? iImsFeatureStatusCallback.asBinder() : null);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsMMTelFeature.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsRcsFeature createRcsFeature(int i, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iImsFeatureStatusCallback != null ? iImsFeatureStatusCallback.asBinder() : null);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsRcsFeature.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeImsFeature(int i, int i2, IImsFeatureStatusCallback iImsFeatureStatusCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iImsFeatureStatusCallback != null ? iImsFeatureStatusCallback.asBinder() : null);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
