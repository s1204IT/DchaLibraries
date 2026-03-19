package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;

public interface IImsMmTelFeature extends IInterface {
    void acknowledgeSms(int i, int i2, int i3) throws RemoteException;

    void acknowledgeSmsReport(int i, int i2, int i3) throws RemoteException;

    void addCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void changeCapabilitiesConfiguration(CapabilityChangeRequest capabilityChangeRequest, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    ImsCallProfile createCallProfile(int i, int i2) throws RemoteException;

    IImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws RemoteException;

    IImsEcbm getEcbmInterface() throws RemoteException;

    int getFeatureState() throws RemoteException;

    IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException;

    String getSmsFormat() throws RemoteException;

    IImsUt getUtInterface() throws RemoteException;

    void onSmsReady() throws RemoteException;

    void queryCapabilityConfiguration(int i, int i2, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    int queryCapabilityStatus() throws RemoteException;

    void removeCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) throws RemoteException;

    void setListener(IImsMmTelListener iImsMmTelListener) throws RemoteException;

    void setSmsListener(IImsSmsListener iImsSmsListener) throws RemoteException;

    void setUiTtyMode(int i, Message message) throws RemoteException;

    int shouldProcessCall(String[] strArr) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsMmTelFeature {
        private static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsMmTelFeature";
        static final int TRANSACTION_acknowledgeSms = 17;
        static final int TRANSACTION_acknowledgeSmsReport = 18;
        static final int TRANSACTION_addCapabilityCallback = 11;
        static final int TRANSACTION_changeCapabilitiesConfiguration = 13;
        static final int TRANSACTION_createCallProfile = 3;
        static final int TRANSACTION_createCallSession = 4;
        static final int TRANSACTION_getEcbmInterface = 7;
        static final int TRANSACTION_getFeatureState = 2;
        static final int TRANSACTION_getMultiEndpointInterface = 9;
        static final int TRANSACTION_getSmsFormat = 19;
        static final int TRANSACTION_getUtInterface = 6;
        static final int TRANSACTION_onSmsReady = 20;
        static final int TRANSACTION_queryCapabilityConfiguration = 14;
        static final int TRANSACTION_queryCapabilityStatus = 10;
        static final int TRANSACTION_removeCapabilityCallback = 12;
        static final int TRANSACTION_sendSms = 16;
        static final int TRANSACTION_setListener = 1;
        static final int TRANSACTION_setSmsListener = 15;
        static final int TRANSACTION_setUiTtyMode = 8;
        static final int TRANSACTION_shouldProcessCall = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsMmTelFeature asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IImsMmTelFeature)) {
                return (IImsMmTelFeature) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ImsCallProfile imsCallProfileCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    setListener(IImsMmTelListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int featureState = getFeatureState();
                    parcel2.writeNoException();
                    parcel2.writeInt(featureState);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    ImsCallProfile imsCallProfileCreateCallProfile = createCallProfile(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (imsCallProfileCreateCallProfile != null) {
                        parcel2.writeInt(1);
                        imsCallProfileCreateCallProfile.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        imsCallProfileCreateFromParcel = ImsCallProfile.CREATOR.createFromParcel(parcel);
                    } else {
                        imsCallProfileCreateFromParcel = null;
                    }
                    IImsCallSession iImsCallSessionCreateCallSession = createCallSession(imsCallProfileCreateFromParcel);
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iImsCallSessionCreateCallSession != null ? iImsCallSessionCreateCallSession.asBinder() : null);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iShouldProcessCall = shouldProcessCall(parcel.createStringArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iShouldProcessCall);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsUt utInterface = getUtInterface();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(utInterface != null ? utInterface.asBinder() : null);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsEcbm ecbmInterface = getEcbmInterface();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(ecbmInterface != null ? ecbmInterface.asBinder() : null);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setUiTtyMode(parcel.readInt(), parcel.readInt() != 0 ? Message.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    IImsMultiEndpoint multiEndpointInterface = getMultiEndpointInterface();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(multiEndpointInterface != null ? multiEndpointInterface.asBinder() : null);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iQueryCapabilityStatus = queryCapabilityStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(iQueryCapabilityStatus);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    addCapabilityCallback(IImsCapabilityCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeCapabilityCallback(IImsCapabilityCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    changeCapabilitiesConfiguration(parcel.readInt() != 0 ? CapabilityChangeRequest.CREATOR.createFromParcel(parcel) : null, IImsCapabilityCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    queryCapabilityConfiguration(parcel.readInt(), parcel.readInt(), IImsCapabilityCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSmsListener(IImsSmsListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendSms(parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readInt() != 0, parcel.createByteArray());
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    acknowledgeSms(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    acknowledgeSmsReport(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    String smsFormat = getSmsFormat();
                    parcel2.writeNoException();
                    parcel2.writeString(smsFormat);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSmsReady();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IImsMmTelFeature {
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
            public void setListener(IImsMmTelListener iImsMmTelListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iImsMmTelListener != null ? iImsMmTelListener.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getFeatureState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ImsCallProfile createCallProfile(int i, int i2) throws RemoteException {
                ImsCallProfile imsCallProfileCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        imsCallProfileCreateFromParcel = ImsCallProfile.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        imsCallProfileCreateFromParcel = null;
                    }
                    return imsCallProfileCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (imsCallProfile != null) {
                        parcelObtain.writeInt(1);
                        imsCallProfile.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsCallSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int shouldProcessCall(String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsUt getUtInterface() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsUt.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IImsEcbm getEcbmInterface() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsEcbm.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setUiTtyMode(int i, Message message) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (message != null) {
                        parcelObtain.writeInt(1);
                        message.writeToParcel(parcelObtain, 0);
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
            public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IImsMultiEndpoint.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int queryCapabilityStatus() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iImsCapabilityCallback != null ? iImsCapabilityCallback.asBinder() : null);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iImsCapabilityCallback != null ? iImsCapabilityCallback.asBinder() : null);
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void changeCapabilitiesConfiguration(CapabilityChangeRequest capabilityChangeRequest, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (capabilityChangeRequest != null) {
                        parcelObtain.writeInt(1);
                        capabilityChangeRequest.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iImsCapabilityCallback != null ? iImsCapabilityCallback.asBinder() : null);
                    this.mRemote.transact(13, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void queryCapabilityConfiguration(int i, int i2, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iImsCapabilityCallback != null ? iImsCapabilityCallback.asBinder() : null);
                    this.mRemote.transact(14, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSmsListener(IImsSmsListener iImsSmsListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iImsSmsListener != null ? iImsSmsListener.asBinder() : null);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(16, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void acknowledgeSms(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(17, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void acknowledgeSmsReport(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(18, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getSmsFormat() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSmsReady() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
