package android.hardware.camera2;

import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.utils.LongParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import java.util.List;

public interface ICameraDeviceUser extends IInterface {
    int beginConfigure() throws RemoteException;

    int cancelRequest(int i, LongParcelable longParcelable) throws RemoteException;

    int createDefaultRequest(int i, CameraMetadataNative cameraMetadataNative) throws RemoteException;

    int createStream(int i, int i2, int i3, Surface surface) throws RemoteException;

    int deleteStream(int i) throws RemoteException;

    void disconnect() throws RemoteException;

    int endConfigure() throws RemoteException;

    int flush(LongParcelable longParcelable) throws RemoteException;

    int getCameraInfo(CameraMetadataNative cameraMetadataNative) throws RemoteException;

    String getParameters() throws RemoteException;

    int getRegister(int i) throws RemoteException;

    void setParameters(String str) throws RemoteException;

    int setRegister(int i, int i2) throws RemoteException;

    void stopFaceDetection() throws RemoteException;

    int submitRequest(CaptureRequest captureRequest, boolean z, LongParcelable longParcelable) throws RemoteException;

    int submitRequestList(List<CaptureRequest> list, boolean z, LongParcelable longParcelable) throws RemoteException;

    int waitUntilIdle() throws RemoteException;

    public static abstract class Stub extends Binder implements ICameraDeviceUser {
        private static final String DESCRIPTOR = "android.hardware.camera2.ICameraDeviceUser";
        static final int TRANSACTION_beginConfigure = 5;
        static final int TRANSACTION_cancelRequest = 4;
        static final int TRANSACTION_createDefaultRequest = 9;
        static final int TRANSACTION_createStream = 8;
        static final int TRANSACTION_deleteStream = 7;
        static final int TRANSACTION_disconnect = 1;
        static final int TRANSACTION_endConfigure = 6;
        static final int TRANSACTION_flush = 17;
        static final int TRANSACTION_getCameraInfo = 10;
        static final int TRANSACTION_getParameters = 11;
        static final int TRANSACTION_getRegister = 13;
        static final int TRANSACTION_setParameters = 12;
        static final int TRANSACTION_setRegister = 14;
        static final int TRANSACTION_stopFaceDetection = 15;
        static final int TRANSACTION_submitRequest = 2;
        static final int TRANSACTION_submitRequestList = 3;
        static final int TRANSACTION_waitUntilIdle = 16;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICameraDeviceUser asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICameraDeviceUser)) {
                return (ICameraDeviceUser) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Surface _arg3;
            CaptureRequest _arg0;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    disconnect();
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = CaptureRequest.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    boolean _arg1 = data.readInt() != 0;
                    LongParcelable _arg2 = new LongParcelable();
                    int _result = submitRequest(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    if (_arg2 != null) {
                        reply.writeInt(1);
                        _arg2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    List<CaptureRequest> _arg02 = data.createTypedArrayList(CaptureRequest.CREATOR);
                    boolean _arg12 = data.readInt() != 0;
                    LongParcelable _arg22 = new LongParcelable();
                    int _result2 = submitRequestList(_arg02, _arg12, _arg22);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    if (_arg22 != null) {
                        reply.writeInt(1);
                        _arg22.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    LongParcelable _arg13 = new LongParcelable();
                    int _result3 = cancelRequest(_arg03, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    if (_arg13 != null) {
                        reply.writeInt(1);
                        _arg13.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = beginConfigure();
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _result5 = endConfigure();
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _result6 = deleteStream(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg14 = data.readInt();
                    int _arg23 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = Surface.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    int _result7 = createStream(_arg04, _arg14, _arg23, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    CameraMetadataNative _arg15 = new CameraMetadataNative();
                    int _result8 = createDefaultRequest(_arg05, _arg15);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    if (_arg15 != null) {
                        reply.writeInt(1);
                        _arg15.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    CameraMetadataNative _arg06 = new CameraMetadataNative();
                    int _result9 = getCameraInfo(_arg06);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    if (_arg06 != null) {
                        reply.writeInt(1);
                        _arg06.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _result10 = getParameters();
                    reply.writeNoException();
                    reply.writeString(_result10);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    setParameters(data.readString());
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _result11 = getRegister(data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _result12 = setRegister(data.readInt(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    stopFaceDetection();
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _result13 = waitUntilIdle();
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    LongParcelable _arg07 = new LongParcelable();
                    int _result14 = flush(_arg07);
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    if (_arg07 != null) {
                        reply.writeInt(1);
                        _arg07.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ICameraDeviceUser {
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
            public void disconnect() throws RemoteException {
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
            public int submitRequest(CaptureRequest request, boolean streaming, LongParcelable lastFrameNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(streaming ? 1 : 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        lastFrameNumber.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int submitRequestList(List<CaptureRequest> requestList, boolean streaming, LongParcelable lastFrameNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(requestList);
                    _data.writeInt(streaming ? 1 : 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        lastFrameNumber.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int cancelRequest(int requestId, LongParcelable lastFrameNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(requestId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        lastFrameNumber.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int beginConfigure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int endConfigure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int deleteStream(int streamId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int createStream(int width, int height, int format, Surface surface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeInt(format);
                    if (surface != null) {
                        _data.writeInt(1);
                        surface.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int createDefaultRequest(int templateId, CameraMetadataNative request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(templateId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        request.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getCameraInfo(CameraMetadataNative info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        info.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getParameters() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setParameters(String param) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(param);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getRegister(int address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(address);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setRegister(int address, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(address);
                    _data.writeInt(value);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopFaceDetection() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int waitUntilIdle() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int flush(LongParcelable lastFrameNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        lastFrameNumber.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
