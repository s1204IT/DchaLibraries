package android.net.sip;

import android.net.sip.ISipSession;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISipSessionListener extends IInterface {
    void onCallBusy(ISipSession iSipSession) throws RemoteException;

    void onCallChangeFailed(ISipSession iSipSession, int i, String str) throws RemoteException;

    void onCallEnded(ISipSession iSipSession) throws RemoteException;

    void onCallEstablished(ISipSession iSipSession, String str) throws RemoteException;

    void onCallTransferring(ISipSession iSipSession, String str) throws RemoteException;

    void onCalling(ISipSession iSipSession) throws RemoteException;

    void onError(ISipSession iSipSession, int i, String str) throws RemoteException;

    void onRegistering(ISipSession iSipSession) throws RemoteException;

    void onRegistrationDone(ISipSession iSipSession, int i) throws RemoteException;

    void onRegistrationFailed(ISipSession iSipSession, int i, String str) throws RemoteException;

    void onRegistrationTimeout(ISipSession iSipSession) throws RemoteException;

    void onRinging(ISipSession iSipSession, SipProfile sipProfile, String str) throws RemoteException;

    void onRingingBack(ISipSession iSipSession) throws RemoteException;

    public static abstract class Stub extends Binder implements ISipSessionListener {
        private static final String DESCRIPTOR = "android.net.sip.ISipSessionListener";
        static final int TRANSACTION_onCallBusy = 6;
        static final int TRANSACTION_onCallChangeFailed = 9;
        static final int TRANSACTION_onCallEnded = 5;
        static final int TRANSACTION_onCallEstablished = 4;
        static final int TRANSACTION_onCallTransferring = 7;
        static final int TRANSACTION_onCalling = 1;
        static final int TRANSACTION_onError = 8;
        static final int TRANSACTION_onRegistering = 10;
        static final int TRANSACTION_onRegistrationDone = 11;
        static final int TRANSACTION_onRegistrationFailed = 12;
        static final int TRANSACTION_onRegistrationTimeout = 13;
        static final int TRANSACTION_onRinging = 2;
        static final int TRANSACTION_onRingingBack = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISipSessionListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISipSessionListener)) {
                return (ISipSessionListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            SipProfile sipProfileCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg0 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    onCalling(_arg0);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg02 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        sipProfileCreateFromParcel = SipProfile.CREATOR.createFromParcel(data);
                    } else {
                        sipProfileCreateFromParcel = null;
                    }
                    String _arg2 = data.readString();
                    onRinging(_arg02, sipProfileCreateFromParcel, _arg2);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg03 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    onRingingBack(_arg03);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg04 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    String _arg1 = data.readString();
                    onCallEstablished(_arg04, _arg1);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg05 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    onCallEnded(_arg05);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg06 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    onCallBusy(_arg06);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg07 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    String _arg12 = data.readString();
                    onCallTransferring(_arg07, _arg12);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg08 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    int _arg13 = data.readInt();
                    String _arg22 = data.readString();
                    onError(_arg08, _arg13, _arg22);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg09 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    int _arg14 = data.readInt();
                    String _arg23 = data.readString();
                    onCallChangeFailed(_arg09, _arg14, _arg23);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg010 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    onRegistering(_arg010);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_onRegistrationDone:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg011 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    int _arg15 = data.readInt();
                    onRegistrationDone(_arg011, _arg15);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_onRegistrationFailed:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg012 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    int _arg16 = data.readInt();
                    String _arg24 = data.readString();
                    onRegistrationFailed(_arg012, _arg16, _arg24);
                    reply.writeNoException();
                    return true;
                case TRANSACTION_onRegistrationTimeout:
                    data.enforceInterface(DESCRIPTOR);
                    ISipSession _arg013 = ISipSession.Stub.asInterface(data.readStrongBinder());
                    onRegistrationTimeout(_arg013);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISipSessionListener {
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
            public void onCalling(ISipSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRinging(ISipSession session, SipProfile caller, String sessionDescription) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (caller != null) {
                        _data.writeInt(1);
                        caller.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(sessionDescription);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRingingBack(ISipSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onCallEstablished(ISipSession session, String sessionDescription) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeString(sessionDescription);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onCallEnded(ISipSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onCallBusy(ISipSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onCallTransferring(ISipSession newSession, String sessionDescription) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    _data.writeString(sessionDescription);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onError(ISipSession session, int errorCode, String errorMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(errorCode);
                    _data.writeString(errorMessage);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onCallChangeFailed(ISipSession session, int errorCode, String errorMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(errorCode);
                    _data.writeString(errorMessage);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRegistering(ISipSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRegistrationDone(ISipSession session, int duration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(duration);
                    this.mRemote.transact(Stub.TRANSACTION_onRegistrationDone, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRegistrationFailed(ISipSession session, int errorCode, String errorMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(errorCode);
                    _data.writeString(errorMessage);
                    this.mRemote.transact(Stub.TRANSACTION_onRegistrationFailed, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRegistrationTimeout(ISipSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(Stub.TRANSACTION_onRegistrationTimeout, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
