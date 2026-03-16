package com.android.internal.telecom;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.StatusHints;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telecom.RemoteServiceCallback;
import java.util.List;

public interface IConnectionServiceAdapter extends IInterface {
    void addConferenceCall(String str, ParcelableConference parcelableConference) throws RemoteException;

    void addExistingConnection(String str, ParcelableConnection parcelableConnection) throws RemoteException;

    void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection) throws RemoteException;

    void onPostDialChar(String str, char c) throws RemoteException;

    void onPostDialWait(String str, String str2) throws RemoteException;

    void queryRemoteConnectionServices(RemoteServiceCallback remoteServiceCallback) throws RemoteException;

    void removeCall(String str) throws RemoteException;

    void setActive(String str) throws RemoteException;

    void setAddress(String str, Uri uri, int i) throws RemoteException;

    void setCallerDisplayName(String str, String str2, int i) throws RemoteException;

    void setConferenceableConnections(String str, List<String> list) throws RemoteException;

    void setConnectionCapabilities(String str, int i) throws RemoteException;

    void setDialing(String str) throws RemoteException;

    void setDisconnected(String str, DisconnectCause disconnectCause) throws RemoteException;

    void setIsConferenced(String str, String str2) throws RemoteException;

    void setIsVoipAudioMode(String str, boolean z) throws RemoteException;

    void setOnHold(String str) throws RemoteException;

    void setRingbackRequested(String str, boolean z) throws RemoteException;

    void setRinging(String str) throws RemoteException;

    void setStatusHints(String str, StatusHints statusHints) throws RemoteException;

    void setVideoProvider(String str, IVideoProvider iVideoProvider) throws RemoteException;

    void setVideoState(String str, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IConnectionServiceAdapter {
        private static final String DESCRIPTOR = "com.android.internal.telecom.IConnectionServiceAdapter";
        static final int TRANSACTION_addConferenceCall = 10;
        static final int TRANSACTION_addExistingConnection = 22;
        static final int TRANSACTION_handleCreateConnectionComplete = 1;
        static final int TRANSACTION_onPostDialChar = 13;
        static final int TRANSACTION_onPostDialWait = 12;
        static final int TRANSACTION_queryRemoteConnectionServices = 14;
        static final int TRANSACTION_removeCall = 11;
        static final int TRANSACTION_setActive = 2;
        static final int TRANSACTION_setAddress = 19;
        static final int TRANSACTION_setCallerDisplayName = 20;
        static final int TRANSACTION_setConferenceableConnections = 21;
        static final int TRANSACTION_setConnectionCapabilities = 8;
        static final int TRANSACTION_setDialing = 4;
        static final int TRANSACTION_setDisconnected = 5;
        static final int TRANSACTION_setIsConferenced = 9;
        static final int TRANSACTION_setIsVoipAudioMode = 17;
        static final int TRANSACTION_setOnHold = 6;
        static final int TRANSACTION_setRingbackRequested = 7;
        static final int TRANSACTION_setRinging = 3;
        static final int TRANSACTION_setStatusHints = 18;
        static final int TRANSACTION_setVideoProvider = 15;
        static final int TRANSACTION_setVideoState = 16;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectionServiceAdapter asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IConnectionServiceAdapter)) {
                return (IConnectionServiceAdapter) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ParcelableConnection _arg1;
            Uri _arg12;
            StatusHints _arg13;
            boolean _arg14;
            ParcelableConference _arg15;
            DisconnectCause _arg16;
            ConnectionRequest _arg17;
            ParcelableConnection _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    if (data.readInt() != 0) {
                        _arg17 = ConnectionRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg17 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg2 = ParcelableConnection.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    handleCreateConnectionComplete(_arg0, _arg17, _arg2);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    setActive(_arg02);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    setRinging(_arg03);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    setDialing(_arg04);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    if (data.readInt() != 0) {
                        _arg16 = DisconnectCause.CREATOR.createFromParcel(data);
                    } else {
                        _arg16 = null;
                    }
                    setDisconnected(_arg05, _arg16);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    setOnHold(_arg06);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    _arg14 = data.readInt() != 0;
                    setRingbackRequested(_arg07, _arg14);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    setConnectionCapabilities(_arg08, data.readInt());
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    setIsConferenced(_arg09, data.readString());
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    if (data.readInt() != 0) {
                        _arg15 = ParcelableConference.CREATOR.createFromParcel(data);
                    } else {
                        _arg15 = null;
                    }
                    addConferenceCall(_arg010, _arg15);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    removeCall(_arg011);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    onPostDialWait(_arg012, data.readString());
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    onPostDialChar(_arg013, (char) data.readInt());
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    RemoteServiceCallback _arg014 = RemoteServiceCallback.Stub.asInterface(data.readStrongBinder());
                    queryRemoteConnectionServices(_arg014);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    setVideoProvider(_arg015, IVideoProvider.Stub.asInterface(data.readStrongBinder()));
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    setVideoState(_arg016, data.readInt());
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    _arg14 = data.readInt() != 0;
                    setIsVoipAudioMode(_arg017, _arg14);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    if (data.readInt() != 0) {
                        _arg13 = StatusHints.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    setStatusHints(_arg018, _arg13);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    if (data.readInt() != 0) {
                        _arg12 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    int _arg22 = data.readInt();
                    setAddress(_arg019, _arg12, _arg22);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    String _arg18 = data.readString();
                    int _arg23 = data.readInt();
                    setCallerDisplayName(_arg020, _arg18, _arg23);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg021 = data.readString();
                    setConferenceableConnections(_arg021, data.createStringArrayList());
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = ParcelableConnection.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    addExistingConnection(_arg022, _arg1);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IConnectionServiceAdapter {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void handleCreateConnectionComplete(String callId, ConnectionRequest request, ParcelableConnection connection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (connection != null) {
                        _data.writeInt(1);
                        connection.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setActive(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setRinging(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setDialing(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setDisconnected(String callId, DisconnectCause disconnectCause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (disconnectCause != null) {
                        _data.writeInt(1);
                        disconnectCause.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setOnHold(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setRingbackRequested(String callId, boolean ringing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(ringing ? 1 : 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setConnectionCapabilities(String callId, int connectionCapabilities) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(connectionCapabilities);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setIsConferenced(String callId, String conferenceCallId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(conferenceCallId);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void addConferenceCall(String callId, ParcelableConference conference) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (conference != null) {
                        _data.writeInt(1);
                        conference.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void removeCall(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPostDialWait(String callId, String remaining) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(remaining);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPostDialChar(String callId, char nextChar) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(nextChar);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void queryRemoteConnectionServices(RemoteServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setVideoProvider(String callId, IVideoProvider videoProvider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeStrongBinder(videoProvider != null ? videoProvider.asBinder() : null);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setVideoState(String callId, int videoState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(videoState);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setIsVoipAudioMode(String callId, boolean isVoip) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(isVoip ? 1 : 0);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setStatusHints(String callId, StatusHints statusHints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (statusHints != null) {
                        _data.writeInt(1);
                        statusHints.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setAddress(String callId, Uri address, int presentation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (address != null) {
                        _data.writeInt(1);
                        address.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(presentation);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setCallerDisplayName(String callId, String callerDisplayName, int presentation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(callerDisplayName);
                    _data.writeInt(presentation);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setConferenceableConnections(String callId, List<String> conferenceableCallIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeStringList(conferenceableCallIds);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void addExistingConnection(String callId, ParcelableConnection connection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    if (connection != null) {
                        _data.writeInt(1);
                        connection.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
