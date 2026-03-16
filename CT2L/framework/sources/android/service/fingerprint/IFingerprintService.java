package android.service.fingerprint;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.fingerprint.IFingerprintServiceReceiver;

public interface IFingerprintService extends IInterface {
    void enroll(IBinder iBinder, long j, int i) throws RemoteException;

    void enrollCancel(IBinder iBinder, int i) throws RemoteException;

    void remove(IBinder iBinder, int i, int i2) throws RemoteException;

    void startListening(IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i) throws RemoteException;

    void stopListening(IBinder iBinder, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IFingerprintService {
        private static final String DESCRIPTOR = "android.service.fingerprint.IFingerprintService";
        static final int TRANSACTION_enroll = 1;
        static final int TRANSACTION_enrollCancel = 2;
        static final int TRANSACTION_remove = 3;
        static final int TRANSACTION_startListening = 4;
        static final int TRANSACTION_stopListening = 5;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IFingerprintService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IFingerprintService)) {
                return (IFingerprintService) iin;
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
                    IBinder _arg0 = data.readStrongBinder();
                    long _arg1 = data.readLong();
                    int _arg2 = data.readInt();
                    enroll(_arg0, _arg1, _arg2);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg02 = data.readStrongBinder();
                    int _arg12 = data.readInt();
                    enrollCancel(_arg02, _arg12);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg03 = data.readStrongBinder();
                    int _arg13 = data.readInt();
                    int _arg22 = data.readInt();
                    remove(_arg03, _arg13, _arg22);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg04 = data.readStrongBinder();
                    IFingerprintServiceReceiver _arg14 = IFingerprintServiceReceiver.Stub.asInterface(data.readStrongBinder());
                    int _arg23 = data.readInt();
                    startListening(_arg04, _arg14, _arg23);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg05 = data.readStrongBinder();
                    int _arg15 = data.readInt();
                    stopListening(_arg05, _arg15);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IFingerprintService {
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
            public void enroll(IBinder token, long timeout, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeLong(timeout);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void enrollCancel(IBinder token, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void remove(IBinder token, int fingerprintId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(fingerprintId);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void startListening(IBinder token, IFingerprintServiceReceiver receiver, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void stopListening(IBinder token, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
