package org.gsma.joyn.chat;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.chat.ConferenceEventData;

public interface IGroupChatListener extends IInterface {
    void onAbortConversationResult(int i, int i2) throws RemoteException;

    void onChairmanChanged(String str) throws RemoteException;

    void onComposingEvent(String str, boolean z) throws RemoteException;

    void onConferenceNotify(String str, List<ConferenceEventData.ConferenceUser> list) throws RemoteException;

    void onGroupChatDissolved() throws RemoteException;

    void onInviteParticipantsResult(int i, String str) throws RemoteException;

    void onModifyNickNameResult(int i, int i2) throws RemoteException;

    void onModifySubjectResult(int i, int i2) throws RemoteException;

    void onNewExtendMessage(ExtendMessage extendMessage) throws RemoteException;

    void onNewGeoloc(GeolocMessage geolocMessage) throws RemoteException;

    void onNewMessage(ChatMessage chatMessage) throws RemoteException;

    void onNickNameChanged(String str, String str2) throws RemoteException;

    void onParticipantDisconnected(String str) throws RemoteException;

    void onParticipantJoined(String str, String str2) throws RemoteException;

    void onParticipantLeft(String str) throws RemoteException;

    void onQuitConversationResult(int i, int i2) throws RemoteException;

    void onRemoveParticipantResult(int i, int i2, String str) throws RemoteException;

    void onReportFailedMessage(String str, int i, String str2) throws RemoteException;

    void onReportMeKickedOut(String str) throws RemoteException;

    void onReportMessageDelivered(String str) throws RemoteException;

    void onReportMessageDeliveredContact(String str, String str2) throws RemoteException;

    void onReportMessageDisplayed(String str) throws RemoteException;

    void onReportMessageDisplayedContact(String str, String str2) throws RemoteException;

    void onReportMessageFailed(String str) throws RemoteException;

    void onReportMessageFailedContact(String str, String str2) throws RemoteException;

    void onReportParticipantKickedOut(String str) throws RemoteException;

    void onReportSentMessage(String str) throws RemoteException;

    void onSessionAborted() throws RemoteException;

    void onSessionAbortedbyChairman() throws RemoteException;

    void onSessionError(int i) throws RemoteException;

    void onSessionStarted() throws RemoteException;

    void onSetChairmanResult(int i, int i2) throws RemoteException;

    void onSubjectChanged(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IGroupChatListener {
        private static final String DESCRIPTOR = "org.gsma.joyn.chat.IGroupChatListener";
        static final int TRANSACTION_onAbortConversationResult = 31;
        static final int TRANSACTION_onChairmanChanged = 23;
        static final int TRANSACTION_onComposingEvent = 18;
        static final int TRANSACTION_onConferenceNotify = 33;
        static final int TRANSACTION_onGroupChatDissolved = 16;
        static final int TRANSACTION_onInviteParticipantsResult = 17;
        static final int TRANSACTION_onModifyNickNameResult = 26;
        static final int TRANSACTION_onModifySubjectResult = 24;
        static final int TRANSACTION_onNewExtendMessage = 6;
        static final int TRANSACTION_onNewGeoloc = 7;
        static final int TRANSACTION_onNewMessage = 5;
        static final int TRANSACTION_onNickNameChanged = 27;
        static final int TRANSACTION_onParticipantDisconnected = 21;
        static final int TRANSACTION_onParticipantJoined = 19;
        static final int TRANSACTION_onParticipantLeft = 20;
        static final int TRANSACTION_onQuitConversationResult = 32;
        static final int TRANSACTION_onRemoveParticipantResult = 28;
        static final int TRANSACTION_onReportFailedMessage = 14;
        static final int TRANSACTION_onReportMeKickedOut = 29;
        static final int TRANSACTION_onReportMessageDelivered = 11;
        static final int TRANSACTION_onReportMessageDeliveredContact = 8;
        static final int TRANSACTION_onReportMessageDisplayed = 12;
        static final int TRANSACTION_onReportMessageDisplayedContact = 9;
        static final int TRANSACTION_onReportMessageFailed = 13;
        static final int TRANSACTION_onReportMessageFailedContact = 10;
        static final int TRANSACTION_onReportParticipantKickedOut = 30;
        static final int TRANSACTION_onReportSentMessage = 15;
        static final int TRANSACTION_onSessionAborted = 2;
        static final int TRANSACTION_onSessionAbortedbyChairman = 3;
        static final int TRANSACTION_onSessionError = 4;
        static final int TRANSACTION_onSessionStarted = 1;
        static final int TRANSACTION_onSetChairmanResult = 22;
        static final int TRANSACTION_onSubjectChanged = 25;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IGroupChatListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IGroupChatListener)) {
                return (IGroupChatListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            GeolocMessage geolocMessageCreateFromParcel;
            ExtendMessage extendMessageCreateFromParcel;
            ChatMessage chatMessageCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    onSessionStarted();
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    onSessionAborted();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    onSessionAbortedbyChairman();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    onSessionError(_arg0);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        chatMessageCreateFromParcel = ChatMessage.CREATOR.createFromParcel(data);
                    } else {
                        chatMessageCreateFromParcel = null;
                    }
                    onNewMessage(chatMessageCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        extendMessageCreateFromParcel = ExtendMessage.CREATOR.createFromParcel(data);
                    } else {
                        extendMessageCreateFromParcel = null;
                    }
                    onNewExtendMessage(extendMessageCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        geolocMessageCreateFromParcel = GeolocMessage.CREATOR.createFromParcel(data);
                    } else {
                        geolocMessageCreateFromParcel = null;
                    }
                    onNewGeoloc(geolocMessageCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    String _arg1 = data.readString();
                    onReportMessageDeliveredContact(_arg02, _arg1);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    String _arg12 = data.readString();
                    onReportMessageDisplayedContact(_arg03, _arg12);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    String _arg13 = data.readString();
                    onReportMessageFailedContact(_arg04, _arg13);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    onReportMessageDelivered(_arg05);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    onReportMessageDisplayed(_arg06);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    onReportMessageFailed(_arg07);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _arg14 = data.readInt();
                    String _arg2 = data.readString();
                    onReportFailedMessage(_arg08, _arg14, _arg2);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    onReportSentMessage(_arg09);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    onGroupChatDissolved();
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    String _arg15 = data.readString();
                    onInviteParticipantsResult(_arg010, _arg15);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    boolean _arg16 = data.readInt() != 0;
                    onComposingEvent(_arg011, _arg16);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    String _arg17 = data.readString();
                    onParticipantJoined(_arg012, _arg17);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    onParticipantLeft(_arg013);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    onParticipantDisconnected(_arg014);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    int _arg18 = data.readInt();
                    onSetChairmanResult(_arg015, _arg18);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    onChairmanChanged(_arg016);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _arg19 = data.readInt();
                    onModifySubjectResult(_arg017, _arg19);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    onSubjectChanged(_arg018);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    int _arg110 = data.readInt();
                    onModifyNickNameResult(_arg019, _arg110);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    String _arg111 = data.readString();
                    onNickNameChanged(_arg020, _arg111);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    int _arg112 = data.readInt();
                    String _arg22 = data.readString();
                    onRemoveParticipantResult(_arg021, _arg112, _arg22);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    onReportMeKickedOut(_arg022);
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    onReportParticipantKickedOut(_arg023);
                    reply.writeNoException();
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg024 = data.readInt();
                    int _arg113 = data.readInt();
                    onAbortConversationResult(_arg024, _arg113);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg025 = data.readInt();
                    int _arg114 = data.readInt();
                    onQuitConversationResult(_arg025, _arg114);
                    reply.writeNoException();
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    List<ConferenceEventData.ConferenceUser> _arg115 = data.createTypedArrayList(ConferenceEventData.ConferenceUser.CREATOR);
                    onConferenceNotify(_arg026, _arg115);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IGroupChatListener {
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
            public void onSessionStarted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onSessionAborted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onSessionAbortedbyChairman() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onSessionError(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onNewMessage(ChatMessage message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (message != null) {
                        _data.writeInt(1);
                        message.writeToParcel(_data, 0);
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
            public void onNewExtendMessage(ExtendMessage message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (message != null) {
                        _data.writeInt(1);
                        message.writeToParcel(_data, 0);
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
            public void onNewGeoloc(GeolocMessage message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (message != null) {
                        _data.writeInt(1);
                        message.writeToParcel(_data, 0);
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
            public void onReportMessageDeliveredContact(String msgId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeString(contact);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageDisplayedContact(String msgId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeString(contact);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageFailedContact(String msgId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeString(contact);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageDelivered(String msgId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageDisplayed(String msgId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageFailed(String msgId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportFailedMessage(String msgId, int errtype, String statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeInt(errtype);
                    _data.writeString(statusCode);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportSentMessage(String msgId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onGroupChatDissolved() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onInviteParticipantsResult(int errType, String statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeString(statusCode);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onComposingEvent(String contact, boolean status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeInt(status ? 1 : 0);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onParticipantJoined(String contact, String contactDisplayname) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeString(contactDisplayname);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onParticipantLeft(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onParticipantDisconnected(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onSetChairmanResult(int errType, int statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeInt(statusCode);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onChairmanChanged(String newChairman) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(newChairman);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onModifySubjectResult(int errType, int statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeInt(statusCode);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onSubjectChanged(String newSubject) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(newSubject);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onModifyNickNameResult(int errType, int statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeInt(statusCode);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onNickNameChanged(String contact, String newNickName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeString(newNickName);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onRemoveParticipantResult(int errType, int statusCode, String participant) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeInt(statusCode);
                    _data.writeString(participant);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMeKickedOut(String from) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(from);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportParticipantKickedOut(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onAbortConversationResult(int errType, int statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeInt(statusCode);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onQuitConversationResult(int errType, int statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errType);
                    _data.writeInt(statusCode);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onConferenceNotify(String confState, List<ConferenceEventData.ConferenceUser> users) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(confState);
                    _data.writeTypedList(users);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
