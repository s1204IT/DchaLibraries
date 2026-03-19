package org.gsma.joyn.ft;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INewFileTransferListener extends IInterface {
    void onFileDeliveredReport(String str, String str2) throws RemoteException;

    void onFileDisplayedReport(String str, String str2) throws RemoteException;

    void onNewBurnFileTransfer(String str, boolean z, String str2, String str3) throws RemoteException;

    void onNewFileTransfer(String str) throws RemoteException;

    void onNewFileTransferReceived(String str, boolean z, boolean z2, String str2, String str3, int i) throws RemoteException;

    void onNewPublicAccountChatFile(String str, boolean z, boolean z2, String str2, String str3) throws RemoteException;

    void onReportFileDelivered(String str) throws RemoteException;

    void onReportFileDisplayed(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements INewFileTransferListener {
        private static final String DESCRIPTOR = "org.gsma.joyn.ft.INewFileTransferListener";
        static final int TRANSACTION_onFileDeliveredReport = 4;
        static final int TRANSACTION_onFileDisplayedReport = 5;
        static final int TRANSACTION_onNewBurnFileTransfer = 8;
        static final int TRANSACTION_onNewFileTransfer = 1;
        static final int TRANSACTION_onNewFileTransferReceived = 6;
        static final int TRANSACTION_onNewPublicAccountChatFile = 7;
        static final int TRANSACTION_onReportFileDelivered = 2;
        static final int TRANSACTION_onReportFileDisplayed = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INewFileTransferListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INewFileTransferListener)) {
                return (INewFileTransferListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    onNewFileTransfer(_arg0);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    onReportFileDelivered(_arg02);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    onReportFileDisplayed(_arg03);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    String _arg1 = data.readString();
                    onFileDeliveredReport(_arg04, _arg1);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    String _arg12 = data.readString();
                    onFileDisplayedReport(_arg05, _arg12);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    boolean _arg13 = data.readInt() != 0;
                    boolean _arg2 = data.readInt() != 0;
                    String _arg3 = data.readString();
                    String _arg4 = data.readString();
                    int _arg5 = data.readInt();
                    onNewFileTransferReceived(_arg06, _arg13, _arg2, _arg3, _arg4, _arg5);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    boolean _arg14 = data.readInt() != 0;
                    boolean _arg22 = data.readInt() != 0;
                    String _arg32 = data.readString();
                    String _arg42 = data.readString();
                    onNewPublicAccountChatFile(_arg07, _arg14, _arg22, _arg32, _arg42);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    boolean _arg15 = data.readInt() != 0;
                    String _arg23 = data.readString();
                    String _arg33 = data.readString();
                    onNewBurnFileTransfer(_arg08, _arg15, _arg23, _arg33);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INewFileTransferListener {
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
            public void onNewFileTransfer(String transferId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportFileDelivered(String transferId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onReportFileDisplayed(String transferId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onFileDeliveredReport(String transferId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    _data.writeString(contact);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onFileDisplayedReport(String transferId, String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    _data.writeString(contact);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onNewFileTransferReceived(String transferId, boolean isAutoAccept, boolean isGroup, String chatSessionId, String ChatId, int timeLen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    _data.writeInt(isAutoAccept ? 1 : 0);
                    _data.writeInt(isGroup ? 1 : 0);
                    _data.writeString(chatSessionId);
                    _data.writeString(ChatId);
                    _data.writeInt(timeLen);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onNewPublicAccountChatFile(String transferId, boolean isAutoAccept, boolean isGroup, String chatSessionId, String ChatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    _data.writeInt(isAutoAccept ? 1 : 0);
                    _data.writeInt(isGroup ? 1 : 0);
                    _data.writeString(chatSessionId);
                    _data.writeString(ChatId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void onNewBurnFileTransfer(String transferId, boolean isGroup, String chatSessionId, String ChatId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    _data.writeInt(isGroup ? 1 : 0);
                    _data.writeString(chatSessionId);
                    _data.writeString(ChatId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
