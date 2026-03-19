package org.gsma.joyn.ipcall;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import org.gsma.joyn.ipcall.IIPCallPlayerListener;

public interface IIPCallPlayer extends IInterface {
    void addEventListener(IIPCallPlayerListener iIPCallPlayerListener) throws RemoteException;

    void close() throws RemoteException;

    AudioCodec getAudioCodec() throws RemoteException;

    int getLocalAudioRtpPort() throws RemoteException;

    int getLocalVideoRtpPort() throws RemoteException;

    AudioCodec[] getSupportedAudioCodecs() throws RemoteException;

    VideoCodec[] getSupportedVideoCodecs() throws RemoteException;

    VideoCodec getVideoCodec() throws RemoteException;

    void open(AudioCodec audioCodec, VideoCodec videoCodec, String str, int i, int i2) throws RemoteException;

    void removeEventListener(IIPCallPlayerListener iIPCallPlayerListener) throws RemoteException;

    void start() throws RemoteException;

    void stop() throws RemoteException;

    public static abstract class Stub extends Binder implements IIPCallPlayer {
        private static final String DESCRIPTOR = "org.gsma.joyn.ipcall.IIPCallPlayer";
        static final int TRANSACTION_addEventListener = 11;
        static final int TRANSACTION_close = 2;
        static final int TRANSACTION_getAudioCodec = 6;
        static final int TRANSACTION_getLocalAudioRtpPort = 5;
        static final int TRANSACTION_getLocalVideoRtpPort = 8;
        static final int TRANSACTION_getSupportedAudioCodecs = 7;
        static final int TRANSACTION_getSupportedVideoCodecs = 10;
        static final int TRANSACTION_getVideoCodec = 9;
        static final int TRANSACTION_open = 1;
        static final int TRANSACTION_removeEventListener = 12;
        static final int TRANSACTION_start = 3;
        static final int TRANSACTION_stop = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIPCallPlayer asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIPCallPlayer)) {
                return (IIPCallPlayer) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            AudioCodec audioCodecCreateFromParcel;
            VideoCodec videoCodecCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        audioCodecCreateFromParcel = AudioCodec.CREATOR.createFromParcel(data);
                    } else {
                        audioCodecCreateFromParcel = null;
                    }
                    if (data.readInt() != 0) {
                        videoCodecCreateFromParcel = VideoCodec.CREATOR.createFromParcel(data);
                    } else {
                        videoCodecCreateFromParcel = null;
                    }
                    String _arg2 = data.readString();
                    int _arg3 = data.readInt();
                    int _arg4 = data.readInt();
                    open(audioCodecCreateFromParcel, videoCodecCreateFromParcel, _arg2, _arg3, _arg4);
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
                    int _result = getLocalAudioRtpPort();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    AudioCodec _result2 = getAudioCodec();
                    reply.writeNoException();
                    if (_result2 != null) {
                        reply.writeInt(1);
                        _result2.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    AudioCodec[] _result3 = getSupportedAudioCodecs();
                    reply.writeNoException();
                    reply.writeTypedArray(_result3, 1);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _result4 = getLocalVideoRtpPort();
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    VideoCodec _result5 = getVideoCodec();
                    reply.writeNoException();
                    if (_result5 != null) {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    VideoCodec[] _result6 = getSupportedVideoCodecs();
                    reply.writeNoException();
                    reply.writeTypedArray(_result6, 1);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    IIPCallPlayerListener _arg0 = IIPCallPlayerListener.Stub.asInterface(data.readStrongBinder());
                    addEventListener(_arg0);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    IIPCallPlayerListener _arg02 = IIPCallPlayerListener.Stub.asInterface(data.readStrongBinder());
                    removeEventListener(_arg02);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IIPCallPlayer {
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
            public void open(AudioCodec audiocodec, VideoCodec videocodec, String remoteHost, int remoteAudioPort, int remoteVideoPort) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (audiocodec != null) {
                        _data.writeInt(1);
                        audiocodec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (videocodec != null) {
                        _data.writeInt(1);
                        videocodec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(remoteHost);
                    _data.writeInt(remoteAudioPort);
                    _data.writeInt(remoteVideoPort);
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
            public int getLocalAudioRtpPort() throws RemoteException {
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
            public AudioCodec getAudioCodec() throws RemoteException {
                AudioCodec audioCodecCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        audioCodecCreateFromParcel = AudioCodec.CREATOR.createFromParcel(_reply);
                    } else {
                        audioCodecCreateFromParcel = null;
                    }
                    return audioCodecCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public AudioCodec[] getSupportedAudioCodecs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    AudioCodec[] _result = (AudioCodec[]) _reply.createTypedArray(AudioCodec.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getLocalVideoRtpPort() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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
            public VideoCodec getVideoCodec() throws RemoteException {
                VideoCodec videoCodecCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
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
            public VideoCodec[] getSupportedVideoCodecs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    VideoCodec[] _result = (VideoCodec[]) _reply.createTypedArray(VideoCodec.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void addEventListener(IIPCallPlayerListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeEventListener(IIPCallPlayerListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
