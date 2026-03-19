package org.gsma.joyn.ipcall;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.ipcall.IIPCall;
import org.gsma.joyn.ipcall.IIPCallListener;
import org.gsma.joyn.ipcall.IIPCallPlayer;
import org.gsma.joyn.ipcall.IIPCallRenderer;
import org.gsma.joyn.ipcall.INewIPCallListener;

public interface IIPCallService extends IInterface {
    void addNewIPCallListener(INewIPCallListener iNewIPCallListener) throws RemoteException;

    void addServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    IPCallServiceConfiguration getConfiguration() throws RemoteException;

    IIPCall getIPCall(String str) throws RemoteException;

    List<IBinder> getIPCalls() throws RemoteException;

    int getServiceVersion() throws RemoteException;

    IIPCall initiateCall(String str, IIPCallPlayer iIPCallPlayer, IIPCallRenderer iIPCallRenderer, IIPCallListener iIPCallListener) throws RemoteException;

    IIPCall initiateVisioCall(String str, IIPCallPlayer iIPCallPlayer, IIPCallRenderer iIPCallRenderer, IIPCallListener iIPCallListener) throws RemoteException;

    boolean isServiceRegistered() throws RemoteException;

    void removeNewIPCallListener(INewIPCallListener iNewIPCallListener) throws RemoteException;

    void removeServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IIPCallService {
        private static final String DESCRIPTOR = "org.gsma.joyn.ipcall.IIPCallService";
        static final int TRANSACTION_addNewIPCallListener = 9;
        static final int TRANSACTION_addServiceRegistrationListener = 2;
        static final int TRANSACTION_getConfiguration = 4;
        static final int TRANSACTION_getIPCall = 6;
        static final int TRANSACTION_getIPCalls = 5;
        static final int TRANSACTION_getServiceVersion = 11;
        static final int TRANSACTION_initiateCall = 7;
        static final int TRANSACTION_initiateVisioCall = 8;
        static final int TRANSACTION_isServiceRegistered = 1;
        static final int TRANSACTION_removeNewIPCallListener = 10;
        static final int TRANSACTION_removeServiceRegistrationListener = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIPCallService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIPCallService)) {
                return (IIPCallService) iin;
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
                    IPCallServiceConfiguration _result2 = getConfiguration();
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
                    List<IBinder> _result3 = getIPCalls();
                    reply.writeNoException();
                    reply.writeBinderList(_result3);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    IIPCall _result4 = getIPCall(_arg03);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result4 != null ? _result4.asBinder() : null);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    IIPCallPlayer _arg1 = IIPCallPlayer.Stub.asInterface(data.readStrongBinder());
                    IIPCallRenderer _arg2 = IIPCallRenderer.Stub.asInterface(data.readStrongBinder());
                    IIPCallListener _arg3 = IIPCallListener.Stub.asInterface(data.readStrongBinder());
                    IIPCall _result5 = initiateCall(_arg04, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result5 != null ? _result5.asBinder() : null);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    IIPCallPlayer _arg12 = IIPCallPlayer.Stub.asInterface(data.readStrongBinder());
                    IIPCallRenderer _arg22 = IIPCallRenderer.Stub.asInterface(data.readStrongBinder());
                    IIPCallListener _arg32 = IIPCallListener.Stub.asInterface(data.readStrongBinder());
                    IIPCall _result6 = initiateVisioCall(_arg05, _arg12, _arg22, _arg32);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result6 != null ? _result6.asBinder() : null);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    INewIPCallListener _arg06 = INewIPCallListener.Stub.asInterface(data.readStrongBinder());
                    addNewIPCallListener(_arg06);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    INewIPCallListener _arg07 = INewIPCallListener.Stub.asInterface(data.readStrongBinder());
                    removeNewIPCallListener(_arg07);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _result7 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IIPCallService {
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
            public IPCallServiceConfiguration getConfiguration() throws RemoteException {
                IPCallServiceConfiguration iPCallServiceConfigurationCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        iPCallServiceConfigurationCreateFromParcel = IPCallServiceConfiguration.CREATOR.createFromParcel(_reply);
                    } else {
                        iPCallServiceConfigurationCreateFromParcel = null;
                    }
                    return iPCallServiceConfigurationCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<IBinder> getIPCalls() throws RemoteException {
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
            public IIPCall getIPCall(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IIPCall _result = IIPCall.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IIPCall initiateCall(String contact, IIPCallPlayer player, IIPCallRenderer renderer, IIPCallListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(player != null ? player.asBinder() : null);
                    _data.writeStrongBinder(renderer != null ? renderer.asBinder() : null);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IIPCall _result = IIPCall.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IIPCall initiateVisioCall(String contact, IIPCallPlayer player, IIPCallRenderer renderer, IIPCallListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(player != null ? player.asBinder() : null);
                    _data.writeStrongBinder(renderer != null ? renderer.asBinder() : null);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    IIPCall _result = IIPCall.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addNewIPCallListener(INewIPCallListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeNewIPCallListener(INewIPCallListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
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
                    this.mRemote.transact(11, _data, _reply, 0);
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
