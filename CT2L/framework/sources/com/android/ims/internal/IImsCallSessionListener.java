package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.ims.ImsCallProfile;
import com.android.ims.ImsConferenceState;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsStreamMediaProfile;
import com.android.ims.internal.IImsCallSession;

public interface IImsCallSessionListener extends IInterface {
    void callSessionConferenceExtendFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionConferenceExtendReceived(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionConferenceExtended(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionConferenceStateUpdated(IImsCallSession iImsCallSession, ImsConferenceState imsConferenceState) throws RemoteException;

    void callSessionHandover(IImsCallSession iImsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionHandoverFailed(IImsCallSession iImsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionHeld(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionHoldFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionHoldReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionInviteParticipantsRequestDelivered(IImsCallSession iImsCallSession) throws RemoteException;

    void callSessionInviteParticipantsRequestFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionMergeComplete(IImsCallSession iImsCallSession) throws RemoteException;

    void callSessionMergeFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionMergeStarted(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionProgressing(IImsCallSession iImsCallSession, ImsStreamMediaProfile imsStreamMediaProfile) throws RemoteException;

    void callSessionRemoveParticipantsRequestDelivered(IImsCallSession iImsCallSession) throws RemoteException;

    void callSessionRemoveParticipantsRequestFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionResumeFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionResumeReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionResumed(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionStartFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionStarted(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionTerminated(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionTtyModeReceived(IImsCallSession iImsCallSession, int i) throws RemoteException;

    void callSessionUpdateFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void callSessionUpdateReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionUpdated(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException;

    void callSessionUssdMessageReceived(IImsCallSession iImsCallSession, int i, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsCallSessionListener {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsCallSessionListener";
        static final int TRANSACTION_callSessionConferenceExtendFailed = 18;
        static final int TRANSACTION_callSessionConferenceExtendReceived = 19;
        static final int TRANSACTION_callSessionConferenceExtended = 17;
        static final int TRANSACTION_callSessionConferenceStateUpdated = 24;
        static final int TRANSACTION_callSessionHandover = 26;
        static final int TRANSACTION_callSessionHandoverFailed = 27;
        static final int TRANSACTION_callSessionHeld = 5;
        static final int TRANSACTION_callSessionHoldFailed = 6;
        static final int TRANSACTION_callSessionHoldReceived = 7;
        static final int TRANSACTION_callSessionInviteParticipantsRequestDelivered = 20;
        static final int TRANSACTION_callSessionInviteParticipantsRequestFailed = 21;
        static final int TRANSACTION_callSessionMergeComplete = 12;
        static final int TRANSACTION_callSessionMergeFailed = 13;
        static final int TRANSACTION_callSessionMergeStarted = 11;
        static final int TRANSACTION_callSessionProgressing = 1;
        static final int TRANSACTION_callSessionRemoveParticipantsRequestDelivered = 22;
        static final int TRANSACTION_callSessionRemoveParticipantsRequestFailed = 23;
        static final int TRANSACTION_callSessionResumeFailed = 9;
        static final int TRANSACTION_callSessionResumeReceived = 10;
        static final int TRANSACTION_callSessionResumed = 8;
        static final int TRANSACTION_callSessionStartFailed = 3;
        static final int TRANSACTION_callSessionStarted = 2;
        static final int TRANSACTION_callSessionTerminated = 4;
        static final int TRANSACTION_callSessionTtyModeReceived = 28;
        static final int TRANSACTION_callSessionUpdateFailed = 15;
        static final int TRANSACTION_callSessionUpdateReceived = 16;
        static final int TRANSACTION_callSessionUpdated = 14;
        static final int TRANSACTION_callSessionUssdMessageReceived = 25;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsCallSessionListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IImsCallSessionListener)) {
                return (IImsCallSessionListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ImsReasonInfo _arg3;
            ImsReasonInfo _arg32;
            ImsConferenceState _arg1;
            ImsReasonInfo _arg12;
            ImsReasonInfo _arg13;
            ImsCallProfile _arg2;
            ImsReasonInfo _arg14;
            ImsCallProfile _arg22;
            ImsCallProfile _arg15;
            ImsReasonInfo _arg16;
            ImsCallProfile _arg17;
            ImsReasonInfo _arg18;
            ImsCallProfile _arg23;
            ImsCallProfile _arg19;
            ImsReasonInfo _arg110;
            ImsCallProfile _arg111;
            ImsCallProfile _arg112;
            ImsReasonInfo _arg113;
            ImsCallProfile _arg114;
            ImsReasonInfo _arg115;
            ImsReasonInfo _arg116;
            ImsCallProfile _arg117;
            ImsStreamMediaProfile _arg118;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg0 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg118 = ImsStreamMediaProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg118 = null;
                    }
                    callSessionProgressing(_arg0, _arg118);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg02 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg117 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg117 = null;
                    }
                    callSessionStarted(_arg02, _arg117);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg03 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg116 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg116 = null;
                    }
                    callSessionStartFailed(_arg03, _arg116);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg04 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg115 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg115 = null;
                    }
                    callSessionTerminated(_arg04, _arg115);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg05 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg114 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg114 = null;
                    }
                    callSessionHeld(_arg05, _arg114);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg06 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg113 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg113 = null;
                    }
                    callSessionHoldFailed(_arg06, _arg113);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg07 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg112 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg112 = null;
                    }
                    callSessionHoldReceived(_arg07, _arg112);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg08 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg111 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg111 = null;
                    }
                    callSessionResumed(_arg08, _arg111);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg09 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg110 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg110 = null;
                    }
                    callSessionResumeFailed(_arg09, _arg110);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg010 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg19 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg19 = null;
                    }
                    callSessionResumeReceived(_arg010, _arg19);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg011 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    IImsCallSession _arg119 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg23 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    callSessionMergeStarted(_arg011, _arg119, _arg23);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg012 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    callSessionMergeComplete(_arg012);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg013 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg18 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg18 = null;
                    }
                    callSessionMergeFailed(_arg013, _arg18);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg014 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg17 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg17 = null;
                    }
                    callSessionUpdated(_arg014, _arg17);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg015 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg16 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg16 = null;
                    }
                    callSessionUpdateFailed(_arg015, _arg16);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg016 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg15 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg15 = null;
                    }
                    callSessionUpdateReceived(_arg016, _arg15);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg017 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    IImsCallSession _arg120 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg22 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    callSessionConferenceExtended(_arg017, _arg120, _arg22);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg018 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg14 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    callSessionConferenceExtendFailed(_arg018, _arg14);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg019 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    IImsCallSession _arg121 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg2 = ImsCallProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    callSessionConferenceExtendReceived(_arg019, _arg121, _arg2);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg020 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    callSessionInviteParticipantsRequestDelivered(_arg020);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg021 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg13 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    callSessionInviteParticipantsRequestFailed(_arg021, _arg13);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg022 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    callSessionRemoveParticipantsRequestDelivered(_arg022);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg023 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg12 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    callSessionRemoveParticipantsRequestFailed(_arg023, _arg12);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg024 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg1 = ImsConferenceState.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    callSessionConferenceStateUpdated(_arg024, _arg1);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg025 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    int _arg122 = data.readInt();
                    String _arg24 = data.readString();
                    callSessionUssdMessageReceived(_arg025, _arg122, _arg24);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg026 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    int _arg123 = data.readInt();
                    int _arg25 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg32 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg32 = null;
                    }
                    callSessionHandover(_arg026, _arg123, _arg25, _arg32);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg027 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    int _arg124 = data.readInt();
                    int _arg26 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = ImsReasonInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    callSessionHandoverFailed(_arg027, _arg124, _arg26, _arg3);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    IImsCallSession _arg028 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                    int _arg125 = data.readInt();
                    callSessionTtyModeReceived(_arg028, _arg125);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IImsCallSessionListener {
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
            public void callSessionProgressing(IImsCallSession session, ImsStreamMediaProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionStarted(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionStartFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionTerminated(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionHeld(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionHoldFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionHoldReceived(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionResumed(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionResumeFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionResumeReceived(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionMergeStarted(IImsCallSession session, IImsCallSession newSession, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionMergeComplete(IImsCallSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionMergeFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionUpdated(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionUpdateFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionUpdateReceived(IImsCallSession session, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionConferenceExtended(IImsCallSession session, IImsCallSession newSession, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionConferenceExtendFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionConferenceExtendReceived(IImsCallSession session, IImsCallSession newSession, ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeStrongBinder(newSession != null ? newSession.asBinder() : null);
                    if (profile != null) {
                        _data.writeInt(1);
                        profile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionInviteParticipantsRequestDelivered(IImsCallSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionInviteParticipantsRequestFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionRemoveParticipantsRequestDelivered(IImsCallSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionRemoveParticipantsRequestFailed(IImsCallSession session, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionConferenceStateUpdated(IImsCallSession session, ImsConferenceState state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    if (state != null) {
                        _data.writeInt(1);
                        state.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionUssdMessageReceived(IImsCallSession session, int mode, String ussdMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(mode);
                    _data.writeString(ussdMessage);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionHandover(IImsCallSession session, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(srcAccessTech);
                    _data.writeInt(targetAccessTech);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionHandoverFailed(IImsCallSession session, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(srcAccessTech);
                    _data.writeInt(targetAccessTech);
                    if (reasonInfo != null) {
                        _data.writeInt(1);
                        reasonInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void callSessionTtyModeReceived(IImsCallSession session, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(session != null ? session.asBinder() : null);
                    _data.writeInt(mode);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
