package android.hardware.location;

import android.hardware.location.IFusedLocationHardwareSink;
import android.location.FusedBatchOptions;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFusedLocationHardware extends IInterface {
    void flushBatchedLocations() throws RemoteException;

    int getSupportedBatchSize() throws RemoteException;

    int getVersion() throws RemoteException;

    void injectDeviceContext(int i) throws RemoteException;

    void injectDiagnosticData(String str) throws RemoteException;

    void registerSink(IFusedLocationHardwareSink iFusedLocationHardwareSink) throws RemoteException;

    void requestBatchOfLocations(int i) throws RemoteException;

    void startBatching(int i, FusedBatchOptions fusedBatchOptions) throws RemoteException;

    void stopBatching(int i) throws RemoteException;

    boolean supportsDeviceContextInjection() throws RemoteException;

    boolean supportsDiagnosticDataInjection() throws RemoteException;

    void unregisterSink(IFusedLocationHardwareSink iFusedLocationHardwareSink) throws RemoteException;

    void updateBatchingOptions(int i, FusedBatchOptions fusedBatchOptions) throws RemoteException;

    public static abstract class Stub extends Binder implements IFusedLocationHardware {
        private static final String DESCRIPTOR = "android.hardware.location.IFusedLocationHardware";
        static final int TRANSACTION_flushBatchedLocations = 12;
        static final int TRANSACTION_getSupportedBatchSize = 3;
        static final int TRANSACTION_getVersion = 13;
        static final int TRANSACTION_injectDeviceContext = 11;
        static final int TRANSACTION_injectDiagnosticData = 9;
        static final int TRANSACTION_registerSink = 1;
        static final int TRANSACTION_requestBatchOfLocations = 7;
        static final int TRANSACTION_startBatching = 4;
        static final int TRANSACTION_stopBatching = 5;
        static final int TRANSACTION_supportsDeviceContextInjection = 10;
        static final int TRANSACTION_supportsDiagnosticDataInjection = 8;
        static final int TRANSACTION_unregisterSink = 2;
        static final int TRANSACTION_updateBatchingOptions = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFusedLocationHardware asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFusedLocationHardware)) {
                return (IFusedLocationHardware) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            FusedBatchOptions fusedBatchOptionsCreateFromParcel;
            FusedBatchOptions fusedBatchOptionsCreateFromParcel2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IFusedLocationHardwareSink _arg0 = IFusedLocationHardwareSink.Stub.asInterface(data.readStrongBinder());
                    registerSink(_arg0);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IFusedLocationHardwareSink _arg02 = IFusedLocationHardwareSink.Stub.asInterface(data.readStrongBinder());
                    unregisterSink(_arg02);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _result = getSupportedBatchSize();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    if (data.readInt() != 0) {
                        fusedBatchOptionsCreateFromParcel2 = FusedBatchOptions.CREATOR.createFromParcel(data);
                    } else {
                        fusedBatchOptionsCreateFromParcel2 = null;
                    }
                    startBatching(_arg03, fusedBatchOptionsCreateFromParcel2);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    stopBatching(_arg04);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    if (data.readInt() != 0) {
                        fusedBatchOptionsCreateFromParcel = FusedBatchOptions.CREATOR.createFromParcel(data);
                    } else {
                        fusedBatchOptionsCreateFromParcel = null;
                    }
                    updateBatchingOptions(_arg05, fusedBatchOptionsCreateFromParcel);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    requestBatchOfLocations(_arg06);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result2 = supportsDiagnosticDataInjection();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    injectDiagnosticData(_arg07);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result3 = supportsDeviceContextInjection();
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    injectDeviceContext(_arg08);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    flushBatchedLocations();
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = getVersion();
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IFusedLocationHardware {
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
            public void registerSink(IFusedLocationHardwareSink eventSink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(eventSink != null ? eventSink.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterSink(IFusedLocationHardwareSink eventSink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(eventSink != null ? eventSink.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getSupportedBatchSize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startBatching(int id, FusedBatchOptions batchOptions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    if (batchOptions != null) {
                        _data.writeInt(1);
                        batchOptions.writeToParcel(_data, 0);
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
            public void stopBatching(int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updateBatchingOptions(int id, FusedBatchOptions batchOptions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    if (batchOptions != null) {
                        _data.writeInt(1);
                        batchOptions.writeToParcel(_data, 0);
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
            public void requestBatchOfLocations(int batchSizeRequested) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(batchSizeRequested);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean supportsDiagnosticDataInjection() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void injectDiagnosticData(String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(data);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean supportsDeviceContextInjection() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void injectDeviceContext(int deviceEnabledContext) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceEnabledContext);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void flushBatchedLocations() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
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
