package com.android.ims.internal;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;

public interface IImsRegistrationListener extends IInterface {
    void registrationAssociatedUriChanged(Uri[] uriArr) throws RemoteException;

    void registrationChangeFailed(int i, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void registrationConnected() throws RemoteException;

    void registrationConnectedWithRadioTech(int i) throws RemoteException;

    void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException;

    void registrationFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) throws RemoteException;

    void registrationProgressing() throws RemoteException;

    void registrationProgressingWithRadioTech(int i) throws RemoteException;

    void registrationResumed() throws RemoteException;

    void registrationServiceCapabilityChanged(int i, int i2) throws RemoteException;

    void registrationSuspended() throws RemoteException;

    void voiceMessageCountUpdate(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsRegistrationListener {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsRegistrationListener";
        static final int TRANSACTION_registrationAssociatedUriChanged = 11;
        static final int TRANSACTION_registrationChangeFailed = 12;
        static final int TRANSACTION_registrationConnected = 1;
        static final int TRANSACTION_registrationConnectedWithRadioTech = 3;
        static final int TRANSACTION_registrationDisconnected = 5;
        static final int TRANSACTION_registrationFeatureCapabilityChanged = 9;
        static final int TRANSACTION_registrationProgressing = 2;
        static final int TRANSACTION_registrationProgressingWithRadioTech = 4;
        static final int TRANSACTION_registrationResumed = 6;
        static final int TRANSACTION_registrationServiceCapabilityChanged = 8;
        static final int TRANSACTION_registrationSuspended = 7;
        static final int TRANSACTION_voiceMessageCountUpdate = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsRegistrationListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IImsRegistrationListener)) {
                return (IImsRegistrationListener) iInterfaceQueryLocalInterface;
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
                    registrationConnected();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationProgressing();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationConnectedWithRadioTech(parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationProgressingWithRadioTech(parcel.readInt());
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationDisconnected(parcel.readInt() != 0 ? ImsReasonInfo.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationResumed();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationSuspended();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationServiceCapabilityChanged(parcel.readInt(), parcel.readInt());
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationFeatureCapabilityChanged(parcel.readInt(), parcel.createIntArray(), parcel.createIntArray());
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    voiceMessageCountUpdate(parcel.readInt());
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationAssociatedUriChanged((Uri[]) parcel.createTypedArray(Uri.CREATOR));
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    registrationChangeFailed(parcel.readInt(), parcel.readInt() != 0 ? ImsReasonInfo.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IImsRegistrationListener {
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
            public void registrationConnected() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationProgressing() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationConnectedWithRadioTech(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationProgressingWithRadioTech(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (imsReasonInfo != null) {
                        parcelObtain.writeInt(1);
                        imsReasonInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationResumed() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationSuspended() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationServiceCapabilityChanged(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeIntArray(iArr2);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void voiceMessageCountUpdate(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationAssociatedUriChanged(Uri[] uriArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedArray(uriArr, 0);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registrationChangeFailed(int i, ImsReasonInfo imsReasonInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (imsReasonInfo != null) {
                        parcelObtain.writeInt(1);
                        imsReasonInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
