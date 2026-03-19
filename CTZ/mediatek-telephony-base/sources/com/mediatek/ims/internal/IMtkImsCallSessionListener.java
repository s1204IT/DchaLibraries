package com.mediatek.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import com.mediatek.ims.internal.IMtkImsCallSession;

public interface IMtkImsCallSessionListener extends IInterface {
    void callSessionDeviceSwitchFailed(IMtkImsCallSession iMtkImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionDeviceSwitched(IMtkImsCallSession iMtkImsCallSession) throws RemoteException;

    void callSessionMergeComplete(IMtkImsCallSession iMtkImsCallSession) throws RemoteException;

    void callSessionMergeStarted(IMtkImsCallSession iMtkImsCallSession, IMtkImsCallSession iMtkImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionRttEventReceived(IMtkImsCallSession iMtkImsCallSession, int i) throws RemoteException;

    void callSessionTextCapabilityChanged(IMtkImsCallSession iMtkImsCallSession, int i, int i2, int i3, int i4) throws RemoteException;

    void callSessionTransferFailed(IMtkImsCallSession iMtkImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionTransferred(IMtkImsCallSession iMtkImsCallSession) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkImsCallSessionListener {
        private static final String DESCRIPTOR = "com.mediatek.ims.internal.IMtkImsCallSessionListener";
        static final int TRANSACTION_callSessionDeviceSwitchFailed = 8;
        static final int TRANSACTION_callSessionDeviceSwitched = 7;
        static final int TRANSACTION_callSessionMergeComplete = 6;
        static final int TRANSACTION_callSessionMergeStarted = 5;
        static final int TRANSACTION_callSessionRttEventReceived = 4;
        static final int TRANSACTION_callSessionTextCapabilityChanged = 3;
        static final int TRANSACTION_callSessionTransferFailed = 2;
        static final int TRANSACTION_callSessionTransferred = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkImsCallSessionListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkImsCallSessionListener)) {
                return (IMtkImsCallSessionListener) iInterfaceQueryLocalInterface;
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
                    callSessionTransferred(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionTransferFailed(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionTextCapabilityChanged(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionRttEventReceived(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionMergeStarted(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()), IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionMergeComplete(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionDeviceSwitched(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    callSessionDeviceSwitchFailed(IMtkImsCallSession.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkImsCallSessionListener {
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
            public void callSessionTransferred(IMtkImsCallSession iMtkImsCallSession) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionTransferFailed(IMtkImsCallSession iMtkImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    if (imsReasonInfo != null) {
                        parcelObtain.writeInt(1);
                        imsReasonInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionTextCapabilityChanged(IMtkImsCallSession iMtkImsCallSession, int i, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionRttEventReceived(IMtkImsCallSession iMtkImsCallSession, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionMergeStarted(IMtkImsCallSession iMtkImsCallSession, IMtkImsCallSession iMtkImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession2 != null ? iMtkImsCallSession2.asBinder() : null);
                    if (imsCallProfile != null) {
                        parcelObtain.writeInt(1);
                        imsCallProfile.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionMergeComplete(IMtkImsCallSession iMtkImsCallSession) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionDeviceSwitched(IMtkImsCallSession iMtkImsCallSession) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callSessionDeviceSwitchFailed(IMtkImsCallSession iMtkImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSession != null ? iMtkImsCallSession.asBinder() : null);
                    if (imsReasonInfo != null) {
                        parcelObtain.writeInt(1);
                        imsReasonInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
