package com.android.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.ims.internal.IImsVideoCallCallback;

public interface IImsVideoCallProvider extends IInterface {
    void requestCallDataUsage() throws RemoteException;

    void requestCameraCapabilities() throws RemoteException;

    void sendSessionModifyRequest(VideoProfile videoProfile) throws RemoteException;

    void sendSessionModifyResponse(VideoProfile videoProfile) throws RemoteException;

    void setCallback(IImsVideoCallCallback iImsVideoCallCallback) throws RemoteException;

    void setCamera(String str) throws RemoteException;

    void setDeviceOrientation(int i) throws RemoteException;

    void setDisplaySurface(Surface surface) throws RemoteException;

    void setPauseImage(String str) throws RemoteException;

    void setPreviewSurface(Surface surface) throws RemoteException;

    void setZoom(float f) throws RemoteException;

    public static abstract class Stub extends Binder implements IImsVideoCallProvider {
        private static final String DESCRIPTOR = "com.android.ims.internal.IImsVideoCallProvider";
        static final int TRANSACTION_requestCallDataUsage = 10;
        static final int TRANSACTION_requestCameraCapabilities = 9;
        static final int TRANSACTION_sendSessionModifyRequest = 7;
        static final int TRANSACTION_sendSessionModifyResponse = 8;
        static final int TRANSACTION_setCallback = 1;
        static final int TRANSACTION_setCamera = 2;
        static final int TRANSACTION_setDeviceOrientation = 5;
        static final int TRANSACTION_setDisplaySurface = 4;
        static final int TRANSACTION_setPauseImage = 11;
        static final int TRANSACTION_setPreviewSurface = 3;
        static final int TRANSACTION_setZoom = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IImsVideoCallProvider asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IImsVideoCallProvider)) {
                return (IImsVideoCallProvider) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            VideoProfile _arg0;
            VideoProfile _arg02;
            Surface _arg03;
            Surface _arg04;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IImsVideoCallCallback _arg05 = IImsVideoCallCallback.Stub.asInterface(data.readStrongBinder());
                    setCallback(_arg05);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    setCamera(_arg06);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = Surface.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    setPreviewSurface(_arg04);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = Surface.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    setDisplaySurface(_arg03);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    setDeviceOrientation(_arg07);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    float _arg08 = data.readFloat();
                    setZoom(_arg08);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = VideoProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    sendSessionModifyRequest(_arg02);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = VideoProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    sendSessionModifyResponse(_arg0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    requestCameraCapabilities();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    requestCallDataUsage();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    setPauseImage(_arg09);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IImsVideoCallProvider {
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
            public void setCallback(IImsVideoCallCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setCamera(String cameraId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(cameraId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setPreviewSurface(Surface surface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (surface != null) {
                        _data.writeInt(1);
                        surface.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setDisplaySurface(Surface surface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (surface != null) {
                        _data.writeInt(1);
                        surface.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setDeviceOrientation(int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(rotation);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setZoom(float value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(value);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendSessionModifyRequest(VideoProfile reqProfile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (reqProfile != null) {
                        _data.writeInt(1);
                        reqProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendSessionModifyResponse(VideoProfile responseProfile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (responseProfile != null) {
                        _data.writeInt(1);
                        responseProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void requestCameraCapabilities() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void requestCallDataUsage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setPauseImage(String uri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uri);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
