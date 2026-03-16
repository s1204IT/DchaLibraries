package com.android.internal.telecom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.AudioState;
import android.telecom.ConnectionRequest;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telecom.IConnectionServiceAdapter;

public interface IConnectionService extends IInterface {
    void abort(String str) throws RemoteException;

    void addConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter) throws RemoteException;

    void answer(String str) throws RemoteException;

    void answerVideo(String str, int i) throws RemoteException;

    void conference(String str, String str2) throws RemoteException;

    void createConnection(PhoneAccountHandle phoneAccountHandle, String str, ConnectionRequest connectionRequest, boolean z, boolean z2) throws RemoteException;

    void disconnect(String str) throws RemoteException;

    void hold(String str) throws RemoteException;

    void mergeConference(String str) throws RemoteException;

    void onAudioStateChanged(String str, AudioState audioState) throws RemoteException;

    void onPostDialContinue(String str, boolean z) throws RemoteException;

    void playDtmfTone(String str, char c) throws RemoteException;

    void reject(String str) throws RemoteException;

    void removeConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter) throws RemoteException;

    void splitFromConference(String str) throws RemoteException;

    void stopDtmfTone(String str) throws RemoteException;

    void swapConference(String str) throws RemoteException;

    void unhold(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IConnectionService {
        private static final String DESCRIPTOR = "com.android.internal.telecom.IConnectionService";
        static final int TRANSACTION_abort = 4;
        static final int TRANSACTION_addConnectionServiceAdapter = 1;
        static final int TRANSACTION_answer = 6;
        static final int TRANSACTION_answerVideo = 5;
        static final int TRANSACTION_conference = 14;
        static final int TRANSACTION_createConnection = 3;
        static final int TRANSACTION_disconnect = 8;
        static final int TRANSACTION_hold = 9;
        static final int TRANSACTION_mergeConference = 16;
        static final int TRANSACTION_onAudioStateChanged = 11;
        static final int TRANSACTION_onPostDialContinue = 18;
        static final int TRANSACTION_playDtmfTone = 12;
        static final int TRANSACTION_reject = 7;
        static final int TRANSACTION_removeConnectionServiceAdapter = 2;
        static final int TRANSACTION_splitFromConference = 15;
        static final int TRANSACTION_stopDtmfTone = 13;
        static final int TRANSACTION_swapConference = 17;
        static final int TRANSACTION_unhold = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectionService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IConnectionService)) {
                return (IConnectionService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            AudioState _arg1;
            PhoneAccountHandle _arg0;
            ConnectionRequest _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IConnectionServiceAdapter _arg02 = IConnectionServiceAdapter.Stub.asInterface(data.readStrongBinder());
                    addConnectionServiceAdapter(_arg02);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IConnectionServiceAdapter _arg03 = IConnectionServiceAdapter.Stub.asInterface(data.readStrongBinder());
                    removeConnectionServiceAdapter(_arg03);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = PhoneAccountHandle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    String _arg12 = data.readString();
                    if (data.readInt() != 0) {
                        _arg2 = ConnectionRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    boolean _arg3 = data.readInt() != 0;
                    boolean _arg4 = data.readInt() != 0;
                    createConnection(_arg0, _arg12, _arg2, _arg3, _arg4);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    abort(_arg04);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg13 = data.readInt();
                    answerVideo(_arg05, _arg13);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    answer(_arg06);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    reject(_arg07);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    disconnect(_arg08);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    hold(_arg09);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    unhold(_arg010);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = AudioState.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    onAudioStateChanged(_arg011, _arg1);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    char _arg14 = (char) data.readInt();
                    playDtmfTone(_arg012, _arg14);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    stopDtmfTone(_arg013);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    String _arg15 = data.readString();
                    conference(_arg014, _arg15);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    splitFromConference(_arg015);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    mergeConference(_arg016);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    swapConference(_arg017);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    boolean _arg16 = data.readInt() != 0;
                    onPostDialContinue(_arg018, _arg16);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IConnectionService {
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
            public void addConnectionServiceAdapter(IConnectionServiceAdapter adapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(adapter != null ? adapter.asBinder() : null);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void removeConnectionServiceAdapter(IConnectionServiceAdapter adapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(adapter != null ? adapter.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void createConnection(PhoneAccountHandle connectionManagerPhoneAccount, String callId, ConnectionRequest request, boolean isIncoming, boolean isUnknown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (connectionManagerPhoneAccount != null) {
                        _data.writeInt(1);
                        connectionManagerPhoneAccount.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(isIncoming ? 1 : 0);
                    _data.writeInt(isUnknown ? 1 : 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void abort(String callId) throws RemoteException {
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
            public void answerVideo(String callId, int videoState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(videoState);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void answer(String callId) throws RemoteException {
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
            public void reject(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void disconnect(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void hold(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void unhold(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onAudioStateChanged(String activeCallId, AudioState audioState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(activeCallId);
                    if (audioState != null) {
                        _data.writeInt(1);
                        audioState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void playDtmfTone(String callId, char digit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(digit);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void stopDtmfTone(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void conference(String conferenceCallId, String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(conferenceCallId);
                    _data.writeString(callId);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void splitFromConference(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void mergeConference(String conferenceCallId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(conferenceCallId);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void swapConference(String conferenceCallId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(conferenceCallId);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPostDialContinue(String callId, boolean proceed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(proceed ? 1 : 0);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
