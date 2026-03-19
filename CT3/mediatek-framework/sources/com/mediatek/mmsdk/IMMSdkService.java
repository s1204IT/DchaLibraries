package com.mediatek.mmsdk;

import android.hardware.camera2.CaptureRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMMSdkService extends IInterface {
    int connectFeatureManager(BinderHolder binderHolder) throws RemoteException;

    int setConfigureParam(int i, CaptureRequest captureRequest) throws RemoteException;

    public static abstract class Stub extends Binder implements IMMSdkService {
        private static final String DESCRIPTOR = "com.mediatek.mmsdk.IMMSdkService";
        static final int TRANSACTION_connectFeatureManager = 1;
        static final int TRANSACTION_setConfigureParam = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMMSdkService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IMMSdkService)) {
                return (IMMSdkService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            CaptureRequest captureRequest;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    BinderHolder _arg0 = new BinderHolder();
                    int _result = connectFeatureManager(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg0 != null) {
                        reply.writeInt(1);
                        _arg0.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    if (data.readInt() != 0) {
                        captureRequest = (CaptureRequest) CaptureRequest.CREATOR.createFromParcel(data);
                    } else {
                        captureRequest = null;
                    }
                    int _result2 = setConfigureParam(_arg02, captureRequest);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IMMSdkService {
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
            public int connectFeatureManager(BinderHolder featureManager) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        featureManager.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setConfigureParam(int openId, CaptureRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(openId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
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
        }
    }
}
