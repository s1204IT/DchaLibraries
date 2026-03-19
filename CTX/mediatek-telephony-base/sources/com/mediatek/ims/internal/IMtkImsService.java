package com.mediatek.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.telephony.ims.aidl.IImsSmsListener;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;
import com.mediatek.gba.NafSessionKey;
import com.mediatek.ims.internal.IMtkImsCallSession;
import com.mediatek.ims.internal.IMtkImsConfig;
import com.mediatek.ims.internal.IMtkImsUt;

public interface IMtkImsService extends IInterface {
    void UpdateImsState(int i) throws RemoteException;

    void addImsSmsListener(int i, IImsSmsListener iImsSmsListener) throws RemoteException;

    IMtkImsCallSession createMtkCallSession(int i, ImsCallProfile imsCallProfile, IImsCallSessionListener iImsCallSessionListener, IImsCallSession iImsCallSession) throws RemoteException;

    void deregisterIms(int i) throws RemoteException;

    IMtkImsConfig getConfigInterfaceEx(int i) throws RemoteException;

    int getCurrentCallCount(int i) throws RemoteException;

    int[] getImsNetworkState(int i) throws RemoteException;

    int getImsRegUriType(int i) throws RemoteException;

    int getImsState(int i) throws RemoteException;

    int getModemMultiImsCount() throws RemoteException;

    IMtkImsUt getMtkUtInterface(int i) throws RemoteException;

    IMtkImsCallSession getPendingMtkCallSession(String str) throws RemoteException;

    IImsUt getUtInterface(int i) throws RemoteException;

    void hangupAllCall(int i) throws RemoteException;

    void registerProprietaryImsListener(int i, IImsRegistrationListener iImsRegistrationListener, boolean z) throws RemoteException;

    NafSessionKey runGbaAuthentication(String str, byte[] bArr, boolean z, int i, int i2) throws RemoteException;

    void sendSms(int i, int i2, int i3, String str, String str2, boolean z, byte[] bArr) throws RemoteException;

    void setCallIndication(int i, String str, String str2, int i2, String str3, boolean z) throws RemoteException;

    void updateRadioState(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkImsService {
        private static final String DESCRIPTOR = "com.mediatek.ims.internal.IMtkImsService";
        static final int TRANSACTION_UpdateImsState = 9;
        static final int TRANSACTION_addImsSmsListener = 17;
        static final int TRANSACTION_createMtkCallSession = 2;
        static final int TRANSACTION_deregisterIms = 7;
        static final int TRANSACTION_getConfigInterfaceEx = 10;
        static final int TRANSACTION_getCurrentCallCount = 15;
        static final int TRANSACTION_getImsNetworkState = 16;
        static final int TRANSACTION_getImsRegUriType = 5;
        static final int TRANSACTION_getImsState = 4;
        static final int TRANSACTION_getModemMultiImsCount = 14;
        static final int TRANSACTION_getMtkUtInterface = 12;
        static final int TRANSACTION_getPendingMtkCallSession = 3;
        static final int TRANSACTION_getUtInterface = 11;
        static final int TRANSACTION_hangupAllCall = 6;
        static final int TRANSACTION_registerProprietaryImsListener = 19;
        static final int TRANSACTION_runGbaAuthentication = 13;
        static final int TRANSACTION_sendSms = 18;
        static final int TRANSACTION_setCallIndication = 1;
        static final int TRANSACTION_updateRadioState = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkImsService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkImsService)) {
                return (IMtkImsService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ImsCallProfile imsCallProfile;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    setCallIndication(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        imsCallProfile = (ImsCallProfile) ImsCallProfile.CREATOR.createFromParcel(parcel);
                    } else {
                        imsCallProfile = null;
                    }
                    IMtkImsCallSession iMtkImsCallSessionCreateMtkCallSession = createMtkCallSession(i3, imsCallProfile, IImsCallSessionListener.Stub.asInterface(parcel.readStrongBinder()), IImsCallSession.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iMtkImsCallSessionCreateMtkCallSession != null ? iMtkImsCallSessionCreateMtkCallSession.asBinder() : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    IMtkImsCallSession pendingMtkCallSession = getPendingMtkCallSession(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(pendingMtkCallSession != null ? pendingMtkCallSession.asBinder() : null);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int imsState = getImsState(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(imsState);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int imsRegUriType = getImsRegUriType(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(imsRegUriType);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    hangupAllCall(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    deregisterIms(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateRadioState(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    UpdateImsState(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    IMtkImsConfig configInterfaceEx = getConfigInterfaceEx(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(configInterfaceEx != null ? configInterfaceEx.asBinder() : null);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsUt utInterface = getUtInterface(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(utInterface != null ? utInterface.asBinder() : null);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    IMtkImsUt mtkUtInterface = getMtkUtInterface(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(mtkUtInterface != null ? mtkUtInterface.asBinder() : null);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    NafSessionKey nafSessionKeyRunGbaAuthentication = runGbaAuthentication(parcel.readString(), parcel.createByteArray(), parcel.readInt() != 0, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (nafSessionKeyRunGbaAuthentication != null) {
                        parcel2.writeInt(1);
                        nafSessionKeyRunGbaAuthentication.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    int modemMultiImsCount = getModemMultiImsCount();
                    parcel2.writeNoException();
                    parcel2.writeInt(modemMultiImsCount);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    int currentCallCount = getCurrentCallCount(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(currentCallCount);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] imsNetworkState = getImsNetworkState(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(imsNetworkState);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    addImsSmsListener(parcel.readInt(), IImsSmsListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendSms(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readInt() != 0, parcel.createByteArray());
                    parcel2.writeNoException();
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerProprietaryImsListener(parcel.readInt(), IImsRegistrationListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkImsService {
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
            public void setCallIndication(int i, String str, String str2, int i2, String str3, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IMtkImsCallSession createMtkCallSession(int i, ImsCallProfile imsCallProfile, IImsCallSessionListener iImsCallSessionListener, IImsCallSession iImsCallSession) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (imsCallProfile != null) {
                        parcelObtain.writeInt(1);
                        imsCallProfile.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iImsCallSessionListener != null ? iImsCallSessionListener.asBinder() : null);
                    parcelObtain.writeStrongBinder(iImsCallSession != null ? iImsCallSession.asBinder() : null);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMtkImsCallSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IMtkImsCallSession getPendingMtkCallSession(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMtkImsCallSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getImsState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getImsRegUriType(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void hangupAllCall(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deregisterIms(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateRadioState(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void UpdateImsState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IMtkImsConfig getConfigInterfaceEx(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMtkImsConfig.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsUt getUtInterface(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsUt.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IMtkImsUt getMtkUtInterface(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMtkImsUt.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public NafSessionKey runGbaAuthentication(String str, byte[] bArr, boolean z, int i, int i2) throws RemoteException {
                NafSessionKey nafSessionKeyCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        nafSessionKeyCreateFromParcel = NafSessionKey.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        nafSessionKeyCreateFromParcel = null;
                    }
                    return nafSessionKeyCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getModemMultiImsCount() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getCurrentCallCount(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getImsNetworkState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addImsSmsListener(int i, IImsSmsListener iImsSmsListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iImsSmsListener != null ? iImsSmsListener.asBinder() : null);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendSms(int i, int i2, int i3, String str, String str2, boolean z, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerProprietaryImsListener(int i, IImsRegistrationListener iImsRegistrationListener, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iImsRegistrationListener != null ? iImsRegistrationListener.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
