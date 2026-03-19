package com.mediatek.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.internal.IImsCallSession;
import com.mediatek.ims.internal.IMtkImsCallSessionListener;

public interface IMtkImsCallSession extends IInterface {
    void callTerminated() throws RemoteException;

    void cancelDeviceSwitch() throws RemoteException;

    void close() throws RemoteException;

    void deviceSwitch(String str, String str2) throws RemoteException;

    void explicitCallTransfer() throws RemoteException;

    String getCallId() throws RemoteException;

    ImsCallProfile getCallProfile() throws RemoteException;

    IImsCallSession getIImsCallSession() throws RemoteException;

    boolean isIncomingCallMultiparty() throws RemoteException;

    void notifyDeviceSwitchFailed(ImsReasonInfo imsReasonInfo) throws RemoteException;

    void notifyDeviceSwitched() throws RemoteException;

    void notifyRttECCRedialEvent() throws RemoteException;

    void notifyTextCapabilityChanged(int i, int i2, int i3, int i4) throws RemoteException;

    void resume() throws RemoteException;

    void sendDtmfbyTarget(char c, Message message, Messenger messenger) throws RemoteException;

    void setIImsCallSession(IImsCallSession iImsCallSession) throws RemoteException;

    void setListener(IMtkImsCallSessionListener iMtkImsCallSessionListener) throws RemoteException;

    void unattendedCallTransfer(String str, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkImsCallSession {
        private static final String DESCRIPTOR = "com.mediatek.ims.internal.IMtkImsCallSession";
        static final int TRANSACTION_callTerminated = 18;
        static final int TRANSACTION_cancelDeviceSwitch = 12;
        static final int TRANSACTION_close = 1;
        static final int TRANSACTION_deviceSwitch = 11;
        static final int TRANSACTION_explicitCallTransfer = 9;
        static final int TRANSACTION_getCallId = 2;
        static final int TRANSACTION_getCallProfile = 3;
        static final int TRANSACTION_getIImsCallSession = 5;
        static final int TRANSACTION_isIncomingCallMultiparty = 7;
        static final int TRANSACTION_notifyDeviceSwitchFailed = 15;
        static final int TRANSACTION_notifyDeviceSwitched = 16;
        static final int TRANSACTION_notifyRttECCRedialEvent = 13;
        static final int TRANSACTION_notifyTextCapabilityChanged = 14;
        static final int TRANSACTION_resume = 17;
        static final int TRANSACTION_sendDtmfbyTarget = 8;
        static final int TRANSACTION_setIImsCallSession = 6;
        static final int TRANSACTION_setListener = 4;
        static final int TRANSACTION_unattendedCallTransfer = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkImsCallSession asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkImsCallSession)) {
                return (IMtkImsCallSession) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Message message;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    close();
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String callId = getCallId();
                    parcel2.writeNoException();
                    parcel2.writeString(callId);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    ImsCallProfile callProfile = getCallProfile();
                    parcel2.writeNoException();
                    if (callProfile != null) {
                        parcel2.writeInt(1);
                        callProfile.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    setListener(IMtkImsCallSessionListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsCallSession iImsCallSession = getIImsCallSession();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iImsCallSession != null ? iImsCallSession.asBinder() : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    setIImsCallSession(IImsCallSession.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsIncomingCallMultiparty = isIncomingCallMultiparty();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsIncomingCallMultiparty ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    char c = (char) parcel.readInt();
                    if (parcel.readInt() != 0) {
                        message = (Message) Message.CREATOR.createFromParcel(parcel);
                    } else {
                        message = null;
                    }
                    sendDtmfbyTarget(c, message, parcel.readInt() != 0 ? (Messenger) Messenger.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    explicitCallTransfer();
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    unattendedCallTransfer(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    deviceSwitch(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelDeviceSwitch();
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyRttECCRedialEvent();
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyTextCapabilityChanged(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyDeviceSwitchFailed(parcel.readInt() != 0 ? (ImsReasonInfo) ImsReasonInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyDeviceSwitched();
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    resume();
                    parcel2.writeNoException();
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    callTerminated();
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkImsCallSession {
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
            public void close() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCallId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ImsCallProfile getCallProfile() throws RemoteException {
                ImsCallProfile imsCallProfile;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        imsCallProfile = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        imsCallProfile = null;
                    }
                    return imsCallProfile;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setListener(IMtkImsCallSessionListener iMtkImsCallSessionListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsCallSessionListener != null ? iMtkImsCallSessionListener.asBinder() : null);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsCallSession getIImsCallSession() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsCallSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setIImsCallSession(IImsCallSession iImsCallSession) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iImsCallSession != null ? iImsCallSession.asBinder() : null);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isIncomingCallMultiparty() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendDtmfbyTarget(char c, Message message, Messenger messenger) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(c);
                    if (message != null) {
                        parcelObtain.writeInt(1);
                        message.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (messenger != null) {
                        parcelObtain.writeInt(1);
                        messenger.writeToParcel(parcelObtain, 0);
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

            @Override
            public void explicitCallTransfer() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unattendedCallTransfer(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deviceSwitch(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelDeviceSwitch() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyRttECCRedialEvent() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyTextCapabilityChanged(int i, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyDeviceSwitchFailed(ImsReasonInfo imsReasonInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (imsReasonInfo != null) {
                        parcelObtain.writeInt(1);
                        imsReasonInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyDeviceSwitched() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resume() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void callTerminated() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
