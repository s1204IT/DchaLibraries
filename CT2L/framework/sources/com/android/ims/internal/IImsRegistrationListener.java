package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IImsRegistrationListener extends IInterface {
    void registrationConnected() throws RemoteException;

    void registrationDisconnected() throws RemoteException;

    void registrationFeatureCapabilityChanged(int i, int[] iArr, int[] iArr2) throws RemoteException;

    void registrationResumed() throws RemoteException;

    void registrationServiceCapabilityChanged(int i, int i2) throws RemoteException;

    void registrationSuspended() throws RemoteException;

    public static abstract class Stub extends Binder implements IImsRegistrationListener {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsRegistrationListener";
        static final int TRANSACTION_registrationConnected = 1;
        static final int TRANSACTION_registrationDisconnected = 2;
        static final int TRANSACTION_registrationFeatureCapabilityChanged = 6;
        static final int TRANSACTION_registrationResumed = 3;
        static final int TRANSACTION_registrationServiceCapabilityChanged = 5;
        static final int TRANSACTION_registrationSuspended = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsRegistrationListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IImsRegistrationListener)) {
                return (IImsRegistrationListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int[] _arg1;
            int[] _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    registrationConnected();
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    registrationDisconnected();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    registrationResumed();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    registrationSuspended();
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _arg12 = data.readInt();
                    registrationServiceCapabilityChanged(_arg0, _arg12);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    int _arg1_length = data.readInt();
                    if (_arg1_length < 0) {
                        _arg1 = null;
                    } else {
                        _arg1 = new int[_arg1_length];
                    }
                    int _arg2_length = data.readInt();
                    if (_arg2_length < 0) {
                        _arg2 = null;
                    } else {
                        _arg2 = new int[_arg2_length];
                    }
                    registrationFeatureCapabilityChanged(_arg02, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeIntArray(_arg1);
                    reply.writeIntArray(_arg2);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IImsRegistrationListener {
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
            public void registrationConnected() throws RemoteException {
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
            public void registrationDisconnected() throws RemoteException {
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
            public void registrationResumed() throws RemoteException {
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
            public void registrationSuspended() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registrationServiceCapabilityChanged(int serviceClass, int event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serviceClass);
                    _data.writeInt(event);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registrationFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures, int[] disabledFeatures) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serviceClass);
                    if (enabledFeatures == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(enabledFeatures.length);
                    }
                    if (disabledFeatures == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(disabledFeatures.length);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    _reply.readIntArray(enabledFeatures);
                    _reply.readIntArray(disabledFeatures);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
