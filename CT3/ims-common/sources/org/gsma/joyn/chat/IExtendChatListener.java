package org.gsma.joyn.chat;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IExtendChatListener extends IInterface {
    void onNewMessage(ExtendMessage extendMessage) throws RemoteException;

    void onReportMessageDelivered(String str, String str2) throws RemoteException;

    void onReportMessageDisplayed(String str, String str2) throws RemoteException;

    void onReportMessageFailed(String str, int i, String str2) throws RemoteException;

    void onReportMessageInviteError(String str, String str2, boolean z) throws RemoteException;

    void onReportMessageSent(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IExtendChatListener {
        private static final String DESCRIPTOR = "org.gsma.joyn.chat.IExtendChatListener";
        static final int TRANSACTION_onNewMessage = 1;
        static final int TRANSACTION_onReportMessageDelivered = 2;
        static final int TRANSACTION_onReportMessageDisplayed = 3;
        static final int TRANSACTION_onReportMessageFailed = 4;
        static final int TRANSACTION_onReportMessageInviteError = 5;
        static final int TRANSACTION_onReportMessageSent = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IExtendChatListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IExtendChatListener)) {
                return (IExtendChatListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ExtendMessage extendMessageCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        extendMessageCreateFromParcel = ExtendMessage.CREATOR.createFromParcel(data);
                    } else {
                        extendMessageCreateFromParcel = null;
                    }
                    onNewMessage(extendMessageCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    onReportMessageDelivered(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    String _arg12 = data.readString();
                    onReportMessageDisplayed(_arg02, _arg12);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    int _arg13 = data.readInt();
                    String _arg2 = data.readString();
                    onReportMessageFailed(_arg03, _arg13, _arg2);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    String _arg14 = data.readString();
                    boolean _arg22 = data.readInt() != 0;
                    onReportMessageInviteError(_arg04, _arg14, _arg22);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    onReportMessageSent(_arg05);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IExtendChatListener {
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
            public void onNewMessage(ExtendMessage message) throws RemoteException {
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
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageDelivered(String msgId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeString(contact);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageDisplayed(String msgId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeString(contact);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageFailed(String msgId, int errCode, String statusCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeInt(errCode);
                    _data.writeString(statusCode);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageInviteError(String msgId, String warningText, boolean isForbidden) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    _data.writeString(warningText);
                    _data.writeInt(isForbidden ? 1 : 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportMessageSent(String msgId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msgId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
