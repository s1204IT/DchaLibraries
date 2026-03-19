package org.gsma.joyn.ft;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;
import org.gsma.joyn.ft.INewFileTransferListener;

public interface IFileTransferService extends IInterface {
    void addNewFileTransferListener(INewFileTransferListener iNewFileTransferListener) throws RemoteException;

    void addServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    FileTransferServiceConfiguration getConfiguration() throws RemoteException;

    IFileTransfer getFileTransfer(String str) throws RemoteException;

    List<IBinder> getFileTransfers() throws RemoteException;

    int getMaxFileTransfers() throws RemoteException;

    int getServiceVersion() throws RemoteException;

    boolean isServiceRegistered() throws RemoteException;

    IFileTransfer prosecuteFile(String str, String str2, IFileTransferListener iFileTransferListener) throws RemoteException;

    void removeNewFileTransferListener(INewFileTransferListener iNewFileTransferListener) throws RemoteException;

    void removeServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    IFileTransfer resumeFileTransfer(String str, IFileTransferListener iFileTransferListener) throws RemoteException;

    IFileTransfer transferFile(String str, String str2, String str3, IFileTransferListener iFileTransferListener) throws RemoteException;

    IFileTransfer transferFileEx(String str, String str2, String str3, int i, String str4, IFileTransferListener iFileTransferListener) throws RemoteException;

    IFileTransfer transferFileToGroup(String str, List<String> list, String str2, String str3, int i, IFileTransferListener iFileTransferListener) throws RemoteException;

    IFileTransfer transferFileToGroupEx(String str, String str2, String str3, int i, String str4, IFileTransferListener iFileTransferListener) throws RemoteException;

    IFileTransfer transferFileToMultiple(List<String> list, String str, String str2, int i, String str3, IFileTransferListener iFileTransferListener) throws RemoteException;

    IFileTransfer transferFileToSecondaryDevice(String str, String str2, int i, String str3, IFileTransferListener iFileTransferListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IFileTransferService {
        private static final String DESCRIPTOR = "org.gsma.joyn.ft.IFileTransferService";
        static final int TRANSACTION_addNewFileTransferListener = 15;
        static final int TRANSACTION_addServiceRegistrationListener = 2;
        static final int TRANSACTION_getConfiguration = 4;
        static final int TRANSACTION_getFileTransfer = 6;
        static final int TRANSACTION_getFileTransfers = 5;
        static final int TRANSACTION_getMaxFileTransfers = 18;
        static final int TRANSACTION_getServiceVersion = 17;
        static final int TRANSACTION_isServiceRegistered = 1;
        static final int TRANSACTION_prosecuteFile = 14;
        static final int TRANSACTION_removeNewFileTransferListener = 16;
        static final int TRANSACTION_removeServiceRegistrationListener = 3;
        static final int TRANSACTION_resumeFileTransfer = 13;
        static final int TRANSACTION_transferFile = 7;
        static final int TRANSACTION_transferFileEx = 8;
        static final int TRANSACTION_transferFileToGroup = 10;
        static final int TRANSACTION_transferFileToGroupEx = 11;
        static final int TRANSACTION_transferFileToMultiple = 9;
        static final int TRANSACTION_transferFileToSecondaryDevice = 12;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFileTransferService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFileTransferService)) {
                return (IFileTransferService) iin;
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
                    boolean _result = isServiceRegistered();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IJoynServiceRegistrationListener _arg0 = IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
                    addServiceRegistrationListener(_arg0);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IJoynServiceRegistrationListener _arg02 = IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
                    removeServiceRegistrationListener(_arg02);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    FileTransferServiceConfiguration _result2 = getConfiguration();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    List<IBinder> _result3 = getFileTransfers();
                    reply.writeNoException();
                    reply.writeBinderList(_result3);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    IFileTransfer _result4 = getFileTransfer(_arg03);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    String _arg1 = data.readString();
                    String _arg2 = data.readString();
                    IFileTransferListener _arg3 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result5 = transferFile(_arg04, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result5 != null ? _result5.asBinder() : null);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    String _arg12 = data.readString();
                    String _arg22 = data.readString();
                    int _arg32 = data.readInt();
                    String _arg4 = data.readString();
                    IFileTransferListener _arg5 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result6 = transferFileEx(_arg05, _arg12, _arg22, _arg32, _arg4, _arg5);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result6 != null ? _result6.asBinder() : null);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg06 = data.createStringArrayList();
                    String _arg13 = data.readString();
                    String _arg23 = data.readString();
                    int _arg33 = data.readInt();
                    String _arg42 = data.readString();
                    IFileTransferListener _arg52 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result7 = transferFileToMultiple(_arg06, _arg13, _arg23, _arg33, _arg42, _arg52);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result7 != null ? _result7.asBinder() : null);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    List<String> _arg14 = data.createStringArrayList();
                    String _arg24 = data.readString();
                    String _arg34 = data.readString();
                    int _arg43 = data.readInt();
                    IFileTransferListener _arg53 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result8 = transferFileToGroup(_arg07, _arg14, _arg24, _arg34, _arg43, _arg53);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result8 != null ? _result8.asBinder() : null);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    String _arg15 = data.readString();
                    String _arg25 = data.readString();
                    int _arg35 = data.readInt();
                    String _arg44 = data.readString();
                    IFileTransferListener _arg54 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result9 = transferFileToGroupEx(_arg08, _arg15, _arg25, _arg35, _arg44, _arg54);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result9 != null ? _result9.asBinder() : null);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    String _arg16 = data.readString();
                    int _arg26 = data.readInt();
                    String _arg36 = data.readString();
                    IFileTransferListener _arg45 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result10 = transferFileToSecondaryDevice(_arg09, _arg16, _arg26, _arg36, _arg45);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result10 != null ? _result10.asBinder() : null);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    IFileTransferListener _arg17 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result11 = resumeFileTransfer(_arg010, _arg17);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result11 != null ? _result11.asBinder() : null);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    String _arg18 = data.readString();
                    IFileTransferListener _arg27 = IFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    IFileTransfer _result12 = prosecuteFile(_arg011, _arg18, _arg27);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result12 != null ? _result12.asBinder() : null);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    INewFileTransferListener _arg012 = INewFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    addNewFileTransferListener(_arg012);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    INewFileTransferListener _arg013 = INewFileTransferListener.Stub.asInterface(data.readStrongBinder());
                    removeNewFileTransferListener(_arg013);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _result13 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _result14 = getMaxFileTransfers();
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IFileTransferService {
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
            public boolean isServiceRegistered() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public FileTransferServiceConfiguration getConfiguration() throws RemoteException {
                FileTransferServiceConfiguration fileTransferServiceConfigurationCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        fileTransferServiceConfigurationCreateFromParcel = FileTransferServiceConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        fileTransferServiceConfigurationCreateFromParcel = null;
                    }
                    return fileTransferServiceConfigurationCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getFileTransfers() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer getFileTransfer(String transferId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(transferId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer transferFile(String contact, String filename, String fileicon, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeString(filename);
                    _data.writeString(fileicon);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer transferFileEx(String contact, String filename, String fileicon, int duration, String type, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeString(filename);
                    _data.writeString(fileicon);
                    _data.writeInt(duration);
                    _data.writeString(type);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer transferFileToMultiple(List<String> contacts, String filename, String fileicon, int duration, String type, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(contacts);
                    _data.writeString(filename);
                    _data.writeString(fileicon);
                    _data.writeInt(duration);
                    _data.writeString(type);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer transferFileToGroup(String chatId, List<String> contacts, String filename, String fileicon, int duration, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    _data.writeStringList(contacts);
                    _data.writeString(filename);
                    _data.writeString(fileicon);
                    _data.writeInt(duration);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer transferFileToGroupEx(String chatId, String filename, String fileicon, int duration, String type, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(chatId);
                    _data.writeString(filename);
                    _data.writeString(fileicon);
                    _data.writeInt(duration);
                    _data.writeString(type);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer transferFileToSecondaryDevice(String filename, String fileicon, int duration, String type, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(filename);
                    _data.writeString(fileicon);
                    _data.writeInt(duration);
                    _data.writeString(type);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer resumeFileTransfer(String fileTranferId, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fileTranferId);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IFileTransfer prosecuteFile(String contact, String transferId, IFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeString(transferId);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    IFileTransfer _result = IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addNewFileTransferListener(INewFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeNewFileTransferListener(INewFileTransferListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getServiceVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMaxFileTransfers() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
