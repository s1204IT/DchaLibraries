package com.android.internal.telecom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.CameraCapabilities;
import android.telecom.VideoProfile;

public interface IVideoCallback extends IInterface {
    void changeCallDataUsage(int i) throws RemoteException;

    void changeCameraCapabilities(CameraCapabilities cameraCapabilities) throws RemoteException;

    void changePeerDimensions(int i, int i2) throws RemoteException;

    void handleCallSessionEvent(int i) throws RemoteException;

    void receiveSessionModifyRequest(VideoProfile videoProfile) throws RemoteException;

    void receiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2) throws RemoteException;

    public static abstract class Stub extends Binder implements IVideoCallback {
        private static final String DESCRIPTOR = "com.android.internal.telecom.IVideoCallback";
        static final int TRANSACTION_changeCallDataUsage = 5;
        static final int TRANSACTION_changeCameraCapabilities = 6;
        static final int TRANSACTION_changePeerDimensions = 4;
        static final int TRANSACTION_handleCallSessionEvent = 3;
        static final int TRANSACTION_receiveSessionModifyRequest = 1;
        static final int TRANSACTION_receiveSessionModifyResponse = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVideoCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVideoCallback)) {
                return (IVideoCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            CameraCapabilities _arg0;
            VideoProfile _arg1;
            VideoProfile _arg2;
            VideoProfile _arg02;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = VideoProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    receiveSessionModifyRequest(_arg02);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = VideoProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg2 = VideoProfile.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    receiveSessionModifyResponse(_arg03, _arg1, _arg2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    handleCallSessionEvent(_arg04);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    int _arg12 = data.readInt();
                    changePeerDimensions(_arg05, _arg12);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    changeCallDataUsage(_arg06);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = CameraCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    changeCameraCapabilities(_arg0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IVideoCallback {
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
            public void receiveSessionModifyRequest(VideoProfile videoProfile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (videoProfile != null) {
                        _data.writeInt(1);
                        videoProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void receiveSessionModifyResponse(int status, VideoProfile requestedProfile, VideoProfile responseProfile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    if (requestedProfile != null) {
                        _data.writeInt(1);
                        requestedProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (responseProfile != null) {
                        _data.writeInt(1);
                        responseProfile.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void handleCallSessionEvent(int event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(event);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void changePeerDimensions(int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void changeCallDataUsage(int dataUsage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(dataUsage);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void changeCameraCapabilities(CameraCapabilities cameraCapabilities) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (cameraCapabilities != null) {
                        _data.writeInt(1);
                        cameraCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
