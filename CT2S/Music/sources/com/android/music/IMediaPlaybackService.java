package com.android.music;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMediaPlaybackService extends IInterface {
    long duration() throws RemoteException;

    void enqueue(long[] jArr, int i) throws RemoteException;

    long getAlbumId() throws RemoteException;

    String getAlbumName() throws RemoteException;

    long getArtistId() throws RemoteException;

    String getArtistName() throws RemoteException;

    long getAudioId() throws RemoteException;

    int getAudioSessionId() throws RemoteException;

    int getMediaMountedCount() throws RemoteException;

    String getPath() throws RemoteException;

    long[] getQueue() throws RemoteException;

    int getQueuePosition() throws RemoteException;

    int getRepeatMode() throws RemoteException;

    int getShuffleMode() throws RemoteException;

    String getTrackName() throws RemoteException;

    boolean isPlaying() throws RemoteException;

    void moveQueueItem(int i, int i2) throws RemoteException;

    void next() throws RemoteException;

    void open(long[] jArr, int i) throws RemoteException;

    void openFile(String str) throws RemoteException;

    void pause() throws RemoteException;

    void play() throws RemoteException;

    long position() throws RemoteException;

    void prev() throws RemoteException;

    int removeTrack(long j) throws RemoteException;

    int removeTracks(int i, int i2) throws RemoteException;

    long seek(long j) throws RemoteException;

    void setQueuePosition(int i) throws RemoteException;

    void setRepeatMode(int i) throws RemoteException;

    void setShuffleMode(int i) throws RemoteException;

    void stop() throws RemoteException;

    public static abstract class Stub extends Binder implements IMediaPlaybackService {
        public Stub() {
            attachInterface(this, "com.android.music.IMediaPlaybackService");
        }

        public static IMediaPlaybackService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.music.IMediaPlaybackService");
            if (iin != null && (iin instanceof IMediaPlaybackService)) {
                return (IMediaPlaybackService) iin;
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
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    String _arg0 = data.readString();
                    openFile(_arg0);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long[] _arg02 = data.createLongArray();
                    int _arg1 = data.readInt();
                    open(_arg02, _arg1);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _result = getQueuePosition();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 4:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    boolean _result2 = isPlaying();
                    reply.writeNoException();
                    reply.writeInt(_result2 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    stop();
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    pause();
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    play();
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    prev();
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    next();
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _result3 = duration();
                    reply.writeNoException();
                    reply.writeLong(_result3);
                    return true;
                case 11:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _result4 = position();
                    reply.writeNoException();
                    reply.writeLong(_result4);
                    return true;
                case 12:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _arg03 = data.readLong();
                    long _result5 = seek(_arg03);
                    reply.writeNoException();
                    reply.writeLong(_result5);
                    return true;
                case 13:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    String _result6 = getTrackName();
                    reply.writeNoException();
                    reply.writeString(_result6);
                    return true;
                case 14:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    String _result7 = getAlbumName();
                    reply.writeNoException();
                    reply.writeString(_result7);
                    return true;
                case 15:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _result8 = getAlbumId();
                    reply.writeNoException();
                    reply.writeLong(_result8);
                    return true;
                case 16:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    String _result9 = getArtistName();
                    reply.writeNoException();
                    reply.writeString(_result9);
                    return true;
                case 17:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _result10 = getArtistId();
                    reply.writeNoException();
                    reply.writeLong(_result10);
                    return true;
                case 18:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long[] _arg04 = data.createLongArray();
                    int _arg12 = data.readInt();
                    enqueue(_arg04, _arg12);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long[] _result11 = getQueue();
                    reply.writeNoException();
                    reply.writeLongArray(_result11);
                    return true;
                case 20:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _arg05 = data.readInt();
                    int _arg13 = data.readInt();
                    moveQueueItem(_arg05, _arg13);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _arg06 = data.readInt();
                    setQueuePosition(_arg06);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    String _result12 = getPath();
                    reply.writeNoException();
                    reply.writeString(_result12);
                    return true;
                case 23:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _result13 = getAudioId();
                    reply.writeNoException();
                    reply.writeLong(_result13);
                    return true;
                case 24:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _arg07 = data.readInt();
                    setShuffleMode(_arg07);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _result14 = getShuffleMode();
                    reply.writeNoException();
                    reply.writeInt(_result14);
                    return true;
                case 26:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _arg08 = data.readInt();
                    int _arg14 = data.readInt();
                    int _result15 = removeTracks(_arg08, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result15);
                    return true;
                case 27:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    long _arg09 = data.readLong();
                    int _result16 = removeTrack(_arg09);
                    reply.writeNoException();
                    reply.writeInt(_result16);
                    return true;
                case 28:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _arg010 = data.readInt();
                    setRepeatMode(_arg010);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _result17 = getRepeatMode();
                    reply.writeNoException();
                    reply.writeInt(_result17);
                    return true;
                case 30:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _result18 = getMediaMountedCount();
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case 31:
                    data.enforceInterface("com.android.music.IMediaPlaybackService");
                    int _result19 = getAudioSessionId();
                    reply.writeNoException();
                    reply.writeInt(_result19);
                    return true;
                case 1598968902:
                    reply.writeString("com.android.music.IMediaPlaybackService");
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IMediaPlaybackService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void openFile(String path) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeString(path);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void open(long[] list, int position) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeLongArray(list);
                    _data.writeInt(position);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getQueuePosition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
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
            public boolean isPlaying() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
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
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void pause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void play() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void prev() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void next() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long duration() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long position() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long seek(long pos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeLong(pos);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getTrackName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getAlbumName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getAlbumId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getArtistName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getArtistId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enqueue(long[] list, int action) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeLongArray(list);
                    _data.writeInt(action);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long[] getQueue() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    long[] _result = _reply.createLongArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void moveQueueItem(int from, int to) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeInt(from);
                    _data.writeInt(to);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setQueuePosition(int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeInt(index);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getPath() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getAudioId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setShuffleMode(int shufflemode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeInt(shufflemode);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getShuffleMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int removeTracks(int first, int last) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeInt(first);
                    _data.writeInt(last);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int removeTrack(long id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeLong(id);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setRepeatMode(int repeatmode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    _data.writeInt(repeatmode);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getRepeatMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getMediaMountedCount() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getAudioSessionId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(31, _data, _reply, 0);
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
