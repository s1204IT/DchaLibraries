package org.gsma.joyn.vsh;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import org.gsma.joyn.vsh.IVideoPlayerListener;

public interface IVideoPlayer extends IInterface {
    void addEventListener(IVideoPlayerListener iVideoPlayerListener) throws RemoteException;

    void close() throws RemoteException;

    VideoCodec getCodec() throws RemoteException;

    int getLocalRtpPort() throws RemoteException;

    VideoCodec[] getSupportedCodecs() throws RemoteException;

    void open(VideoCodec videoCodec, String str, int i) throws RemoteException;

    void removeEventListener(IVideoPlayerListener iVideoPlayerListener) throws RemoteException;

    void setOrientationHeaderId(int i) throws RemoteException;

    void start() throws RemoteException;

    void stop() throws RemoteException;

    public static abstract class Stub extends Binder implements IVideoPlayer {
        private static final String DESCRIPTOR = "org.gsma.joyn.vsh.IVideoPlayer";
        static final int TRANSACTION_addEventListener = 8;
        static final int TRANSACTION_close = 2;
        static final int TRANSACTION_getCodec = 6;
        static final int TRANSACTION_getLocalRtpPort = 5;
        static final int TRANSACTION_getSupportedCodecs = 7;
        static final int TRANSACTION_open = 1;
        static final int TRANSACTION_removeEventListener = 9;
        static final int TRANSACTION_setOrientationHeaderId = 10;
        static final int TRANSACTION_start = 3;
        static final int TRANSACTION_stop = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVideoPlayer asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVideoPlayer)) {
                return (IVideoPlayer) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            VideoCodec videoCodecCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        videoCodecCreateFromParcel = VideoCodec.CREATOR.createFromParcel(data);
                    } else {
                        videoCodecCreateFromParcel = null;
                    }
                    String _arg1 = data.readString();
                    int _arg2 = data.readInt();
                    open(videoCodecCreateFromParcel, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    close();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    start();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    stop();
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _result = getLocalRtpPort();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    VideoCodec _result2 = getCodec();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    VideoCodec[] _result3 = getSupportedCodecs();
                    reply.writeNoException();
                    reply.writeTypedArray(_result3, 1);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    IVideoPlayerListener _arg0 = IVideoPlayerListener.Stub.asInterface(data.readStrongBinder());
                    addEventListener(_arg0);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    IVideoPlayerListener _arg02 = IVideoPlayerListener.Stub.asInterface(data.readStrongBinder());
                    removeEventListener(_arg02);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    setOrientationHeaderId(_arg03);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IVideoPlayer {
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
            public void open(VideoCodec codec, String remoteHost, int remotePort) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (codec != null) {
                        _data.writeInt(1);
                        codec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(remoteHost);
                    _data.writeInt(remotePort);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void close() throws RemoteException {
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
            public void start() throws RemoteException {
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
            public void stop() throws RemoteException {
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
            public int getLocalRtpPort() throws RemoteException {
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
            public VideoCodec getCodec() throws RemoteException {
                VideoCodec videoCodecCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        videoCodecCreateFromParcel = VideoCodec.CREATOR.createFromParcel(_reply);
                    } else {
                        videoCodecCreateFromParcel = null;
                    }
                    return videoCodecCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public VideoCodec[] getSupportedCodecs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    VideoCodec[] _result = (VideoCodec[]) _reply.createTypedArray(VideoCodec.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addEventListener(IVideoPlayerListener listener) throws RemoteException {
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
            public void removeEventListener(IVideoPlayerListener listener) throws RemoteException {
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
            public void setOrientationHeaderId(int headerId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(headerId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
