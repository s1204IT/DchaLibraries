package com.mediatek.internal.telecom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.ConnectionRequest;
import android.telecom.Logging.Session;
import android.telecom.PhoneAccountHandle;
import com.mediatek.internal.telecom.IMtkConnectionServiceAdapter;
import java.util.List;

public interface IMtkConnectionService extends IInterface {
    void addMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter iMtkConnectionServiceAdapter) throws RemoteException;

    void blindAssuredEct(String str, String str2, int i) throws RemoteException;

    void cancelDeviceSwitch(String str) throws RemoteException;

    void clearMtkConnectionServiceAdapter() throws RemoteException;

    void createConference(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, List<String> list, boolean z, Session.Info info) throws RemoteException;

    void deviceSwitch(String str, String str2, String str3) throws RemoteException;

    void explicitCallTransfer(String str) throws RemoteException;

    IBinder getBinder() throws RemoteException;

    void handleOrderedOperation(String str, String str2, String str3) throws RemoteException;

    void hangupAll(String str) throws RemoteException;

    void inviteConferenceParticipants(String str, List<String> list) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkConnectionService {
        private static final String DESCRIPTOR = "com.mediatek.internal.telecom.IMtkConnectionService";
        static final int TRANSACTION_addMtkConnectionServiceAdapter = 2;
        static final int TRANSACTION_blindAssuredEct = 7;
        static final int TRANSACTION_cancelDeviceSwitch = 11;
        static final int TRANSACTION_clearMtkConnectionServiceAdapter = 3;
        static final int TRANSACTION_createConference = 9;
        static final int TRANSACTION_deviceSwitch = 10;
        static final int TRANSACTION_explicitCallTransfer = 6;
        static final int TRANSACTION_getBinder = 1;
        static final int TRANSACTION_handleOrderedOperation = 5;
        static final int TRANSACTION_hangupAll = 4;
        static final int TRANSACTION_inviteConferenceParticipants = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkConnectionService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkConnectionService)) {
                return (IMtkConnectionService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            PhoneAccountHandle phoneAccountHandle;
            ConnectionRequest connectionRequest;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder binder = getBinder();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(binder);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    addMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case TRANSACTION_clearMtkConnectionServiceAdapter:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearMtkConnectionServiceAdapter();
                    return true;
                case TRANSACTION_hangupAll:
                    parcel.enforceInterface(DESCRIPTOR);
                    hangupAll(parcel.readString());
                    return true;
                case TRANSACTION_handleOrderedOperation:
                    parcel.enforceInterface(DESCRIPTOR);
                    handleOrderedOperation(parcel.readString(), parcel.readString(), parcel.readString());
                    return true;
                case TRANSACTION_explicitCallTransfer:
                    parcel.enforceInterface(DESCRIPTOR);
                    explicitCallTransfer(parcel.readString());
                    return true;
                case TRANSACTION_blindAssuredEct:
                    parcel.enforceInterface(DESCRIPTOR);
                    blindAssuredEct(parcel.readString(), parcel.readString(), parcel.readInt());
                    return true;
                case TRANSACTION_inviteConferenceParticipants:
                    parcel.enforceInterface(DESCRIPTOR);
                    inviteConferenceParticipants(parcel.readString(), parcel.createStringArrayList());
                    return true;
                case TRANSACTION_createConference:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        phoneAccountHandle = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                    } else {
                        phoneAccountHandle = null;
                    }
                    String string = parcel.readString();
                    if (parcel.readInt() != 0) {
                        connectionRequest = (ConnectionRequest) ConnectionRequest.CREATOR.createFromParcel(parcel);
                    } else {
                        connectionRequest = null;
                    }
                    createConference(phoneAccountHandle, string, connectionRequest, parcel.createStringArrayList(), parcel.readInt() != 0, parcel.readInt() != 0 ? (Session.Info) Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case TRANSACTION_deviceSwitch:
                    parcel.enforceInterface(DESCRIPTOR);
                    deviceSwitch(parcel.readString(), parcel.readString(), parcel.readString());
                    return true;
                case TRANSACTION_cancelDeviceSwitch:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelDeviceSwitch(parcel.readString());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkConnectionService {
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
            public IBinder getBinder() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter iMtkConnectionServiceAdapter) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkConnectionServiceAdapter != null ? iMtkConnectionServiceAdapter.asBinder() : null);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearMtkConnectionServiceAdapter() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_clearMtkConnectionServiceAdapter, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void hangupAll(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_hangupAll, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void handleOrderedOperation(String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(Stub.TRANSACTION_handleOrderedOperation, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void explicitCallTransfer(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_explicitCallTransfer, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void blindAssuredEct(String str, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(Stub.TRANSACTION_blindAssuredEct, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void inviteConferenceParticipants(String str, List<String> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringList(list);
                    this.mRemote.transact(Stub.TRANSACTION_inviteConferenceParticipants, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void createConference(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, List<String> list, boolean z, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (phoneAccountHandle != null) {
                        parcelObtain.writeInt(1);
                        phoneAccountHandle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    if (connectionRequest != null) {
                        parcelObtain.writeInt(1);
                        connectionRequest.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_createConference, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deviceSwitch(String str, String str2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(Stub.TRANSACTION_deviceSwitch, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelDeviceSwitch(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_cancelDeviceSwitch, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
