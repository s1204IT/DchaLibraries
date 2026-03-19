package com.mediatek.mmsdk;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface IEffectFactory extends IInterface {
    int createEffectHal(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException;

    int createEffectHalClient(EffectHalVersion effectHalVersion, BinderHolder binderHolder) throws RemoteException;

    int getAllSupportedEffectHal(List<String> list) throws RemoteException;

    int getSupportedVersion(String str, List<EffectHalVersion> list) throws RemoteException;

    public static abstract class Stub extends Binder implements IEffectFactory {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.IEffectFactory";
        static final int TRANSACTION_createEffectHal = 1;
        static final int TRANSACTION_createEffectHalClient = 2;
        static final int TRANSACTION_getAllSupportedEffectHal = 4;
        static final int TRANSACTION_getSupportedVersion = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEffectFactory asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IEffectFactory)) {
                return (IEffectFactory) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            EffectHalVersion effectHalVersionCreateFromParcel;
            EffectHalVersion effectHalVersionCreateFromParcel2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        effectHalVersionCreateFromParcel2 = EffectHalVersion.CREATOR.createFromParcel(data);
                    } else {
                        effectHalVersionCreateFromParcel2 = null;
                    }
                    BinderHolder _arg1 = new BinderHolder();
                    int _result = createEffectHal(effectHalVersionCreateFromParcel2, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg1 != null) {
                        reply.writeInt(1);
                        _arg1.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        effectHalVersionCreateFromParcel = EffectHalVersion.CREATOR.createFromParcel(data);
                    } else {
                        effectHalVersionCreateFromParcel = null;
                    }
                    BinderHolder _arg12 = new BinderHolder();
                    int _result2 = createEffectHalClient(effectHalVersionCreateFromParcel, _arg12);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    if (_arg12 != null) {
                        reply.writeInt(1);
                        _arg12.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    ArrayList arrayList = new ArrayList();
                    int _result3 = getSupportedVersion(_arg0, arrayList);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    reply.writeTypedList(arrayList);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    List<String> _arg02 = new ArrayList<>();
                    int _result4 = getAllSupportedEffectHal(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    reply.writeStringList(_arg02);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IEffectFactory {
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
            public int createEffectHal(EffectHalVersion nameVersion, BinderHolder effectHal) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (nameVersion != null) {
                        _data.writeInt(1);
                        nameVersion.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        effectHal.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int createEffectHalClient(EffectHalVersion nameVersion, BinderHolder effectHalClient) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (nameVersion != null) {
                        _data.writeInt(1);
                        nameVersion.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        effectHalClient.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getSupportedVersion(String effectName, List<EffectHalVersion> versions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(effectName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readTypedList(versions, EffectHalVersion.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getAllSupportedEffectHal(List<String> version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readStringList(version);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
