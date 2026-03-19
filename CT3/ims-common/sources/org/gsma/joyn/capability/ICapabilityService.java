package org.gsma.joyn.capability;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.capability.ICapabilitiesListener;

public interface ICapabilityService extends IInterface {
    void addCapabilitiesListener(ICapabilitiesListener iCapabilitiesListener) throws RemoteException;

    void addContactCapabilitiesListener(String str, ICapabilitiesListener iCapabilitiesListener) throws RemoteException;

    void addServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    void forceRequestContactCapabilities(String str) throws RemoteException;

    Capabilities getContactCapabilities(String str) throws RemoteException;

    Capabilities getMyCapabilities() throws RemoteException;

    int getServiceVersion() throws RemoteException;

    boolean isServiceRegistered() throws RemoteException;

    void removeCapabilitiesListener(ICapabilitiesListener iCapabilitiesListener) throws RemoteException;

    void removeContactCapabilitiesListener(String str, ICapabilitiesListener iCapabilitiesListener) throws RemoteException;

    void removeServiceRegistrationListener(IJoynServiceRegistrationListener iJoynServiceRegistrationListener) throws RemoteException;

    void requestAllContactsCapabilities() throws RemoteException;

    void requestContactCapabilities(String str) throws RemoteException;

    void requestContactsCapabilities(List<String> list) throws RemoteException;

    public static abstract class Stub extends Binder implements ICapabilityService {
        private static final String DESCRIPTOR = "org.gsma.joyn.capability.ICapabilityService";
        static final int TRANSACTION_addCapabilitiesListener = 8;
        static final int TRANSACTION_addContactCapabilitiesListener = 10;
        static final int TRANSACTION_addServiceRegistrationListener = 2;
        static final int TRANSACTION_forceRequestContactCapabilities = 13;
        static final int TRANSACTION_getContactCapabilities = 5;
        static final int TRANSACTION_getMyCapabilities = 4;
        static final int TRANSACTION_getServiceVersion = 12;
        static final int TRANSACTION_isServiceRegistered = 1;
        static final int TRANSACTION_removeCapabilitiesListener = 9;
        static final int TRANSACTION_removeContactCapabilitiesListener = 11;
        static final int TRANSACTION_removeServiceRegistrationListener = 3;
        static final int TRANSACTION_requestAllContactsCapabilities = 7;
        static final int TRANSACTION_requestContactCapabilities = 6;
        static final int TRANSACTION_requestContactsCapabilities = 14;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICapabilityService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICapabilityService)) {
                return (ICapabilityService) iin;
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
                    Capabilities _result2 = getMyCapabilities();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    Capabilities _result3 = getContactCapabilities(_arg03);
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    requestContactCapabilities(_arg04);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    requestAllContactsCapabilities();
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    ICapabilitiesListener _arg05 = ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
                    addCapabilitiesListener(_arg05);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    ICapabilitiesListener _arg06 = ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
                    removeCapabilitiesListener(_arg06);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    ICapabilitiesListener _arg1 = ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
                    addContactCapabilitiesListener(_arg07, _arg1);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    ICapabilitiesListener _arg12 = ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
                    removeContactCapabilitiesListener(_arg08, _arg12);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = getServiceVersion();
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    forceRequestContactCapabilities(_arg09);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg010 = data.createStringArrayList();
                    requestContactsCapabilities(_arg010);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ICapabilityService {
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
            public Capabilities getMyCapabilities() throws RemoteException {
                Capabilities capabilitiesCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        capabilitiesCreateFromParcel = Capabilities.CREATOR.createFromParcel(_reply);
                    } else {
                        capabilitiesCreateFromParcel = null;
                    }
                    return capabilitiesCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Capabilities getContactCapabilities(String contact) throws RemoteException {
                Capabilities capabilitiesCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        capabilitiesCreateFromParcel = Capabilities.CREATOR.createFromParcel(_reply);
                    } else {
                        capabilitiesCreateFromParcel = null;
                    }
                    return capabilitiesCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestContactCapabilities(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestAllContactsCapabilities() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addCapabilitiesListener(ICapabilitiesListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeCapabilitiesListener(ICapabilitiesListener listener) throws RemoteException {
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
            public void addContactCapabilitiesListener(String contact, ICapabilitiesListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeContactCapabilitiesListener(String contact, ICapabilitiesListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(11, _data, _reply, 0);
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
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void forceRequestContactCapabilities(String contact) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(contact);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestContactsCapabilities(List<String> contacts) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(contacts);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
