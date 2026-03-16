package android.powerhint;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IPowerHintService extends IInterface {
    int cancelDurablePowerHint(PowerHintData powerHintData) throws RemoteException;

    double obtainDurablePowerHintTimer() throws RemoteException;

    int sendDurablePowerHint(PowerHintData powerHintData) throws RemoteException;

    int sendPowerHint(PowerHintData powerHintData) throws RemoteException;

    public static abstract class Stub extends Binder implements IPowerHintService {
        private static final String DESCRIPTOR = "android.powerhint.IPowerHintService";
        static final int TRANSACTION_cancelDurablePowerHint = 4;
        static final int TRANSACTION_obtainDurablePowerHintTimer = 3;
        static final int TRANSACTION_sendDurablePowerHint = 2;
        static final int TRANSACTION_sendPowerHint = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPowerHintService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPowerHintService)) {
                return (IPowerHintService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            PowerHintData _arg0;
            PowerHintData _arg02;
            PowerHintData _arg03;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = PowerHintData.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    int _result = sendPowerHint(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = PowerHintData.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    int _result2 = sendDurablePowerHint(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    double _result3 = obtainDurablePowerHintTimer();
                    reply.writeNoException();
                    reply.writeDouble(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = PowerHintData.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _result4 = cancelDurablePowerHint(_arg0);
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

        private static class Proxy implements IPowerHintService {
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
            public int sendPowerHint(PowerHintData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int sendDurablePowerHint(PowerHintData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public double obtainDurablePowerHintTimer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    double _result = _reply.readDouble();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int cancelDurablePowerHint(PowerHintData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
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
