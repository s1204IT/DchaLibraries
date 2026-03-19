package com.android.music;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMediaPlaybackService extends IInterface {
    boolean canUseAsRingtone() throws RemoteException;

    long duration() throws RemoteException;

    void enqueue(long[] jArr, int i) throws RemoteException;

    long getAlbumId() throws RemoteException;

    String getAlbumName() throws RemoteException;

    long getArtistId() throws RemoteException;

    String getArtistName() throws RemoteException;

    long getAudioId() throws RemoteException;

    int getAudioSessionId() throws RemoteException;

    String getMIMEType() throws RemoteException;

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

        public static IMediaPlaybackService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.android.music.IMediaPlaybackService");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMediaPlaybackService)) {
                return (IMediaPlaybackService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("com.android.music.IMediaPlaybackService");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    openFile(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    open(parcel.createLongArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int queuePosition = getQueuePosition();
                    parcel2.writeNoException();
                    parcel2.writeInt(queuePosition);
                    return true;
                case 4:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    boolean zIsPlaying = isPlaying();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsPlaying ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    stop();
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    pause();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    play();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    prev();
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    next();
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long jDuration = duration();
                    parcel2.writeNoException();
                    parcel2.writeLong(jDuration);
                    return true;
                case 11:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long jPosition = position();
                    parcel2.writeNoException();
                    parcel2.writeLong(jPosition);
                    return true;
                case 12:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long jSeek = seek(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeLong(jSeek);
                    return true;
                case 13:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    String trackName = getTrackName();
                    parcel2.writeNoException();
                    parcel2.writeString(trackName);
                    return true;
                case 14:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    String albumName = getAlbumName();
                    parcel2.writeNoException();
                    parcel2.writeString(albumName);
                    return true;
                case 15:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long albumId = getAlbumId();
                    parcel2.writeNoException();
                    parcel2.writeLong(albumId);
                    return true;
                case 16:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    String artistName = getArtistName();
                    parcel2.writeNoException();
                    parcel2.writeString(artistName);
                    return true;
                case 17:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long artistId = getArtistId();
                    parcel2.writeNoException();
                    parcel2.writeLong(artistId);
                    return true;
                case 18:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    String mIMEType = getMIMEType();
                    parcel2.writeNoException();
                    parcel2.writeString(mIMEType);
                    return true;
                case 19:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    enqueue(parcel.createLongArray(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 20:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long[] queue = getQueue();
                    parcel2.writeNoException();
                    parcel2.writeLongArray(queue);
                    return true;
                case 21:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    moveQueueItem(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 22:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    setQueuePosition(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    String path = getPath();
                    parcel2.writeNoException();
                    parcel2.writeString(path);
                    return true;
                case 24:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    long audioId = getAudioId();
                    parcel2.writeNoException();
                    parcel2.writeLong(audioId);
                    return true;
                case 25:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    setShuffleMode(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 26:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int shuffleMode = getShuffleMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(shuffleMode);
                    return true;
                case 27:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int iRemoveTracks = removeTracks(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iRemoveTracks);
                    return true;
                case 28:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int iRemoveTrack = removeTrack(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(iRemoveTrack);
                    return true;
                case 29:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    setRepeatMode(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 30:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int repeatMode = getRepeatMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(repeatMode);
                    return true;
                case 31:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int mediaMountedCount = getMediaMountedCount();
                    parcel2.writeNoException();
                    parcel2.writeInt(mediaMountedCount);
                    return true;
                case 32:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    int audioSessionId = getAudioSessionId();
                    parcel2.writeNoException();
                    parcel2.writeInt(audioSessionId);
                    return true;
                case 33:
                    parcel.enforceInterface("com.android.music.IMediaPlaybackService");
                    boolean zCanUseAsRingtone = canUseAsRingtone();
                    parcel2.writeNoException();
                    parcel2.writeInt(zCanUseAsRingtone ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMediaPlaybackService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void openFile(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void open(long[] jArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeLongArray(jArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getQueuePosition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isPlaying() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stop() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void pause() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void play() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void prev() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void next() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long duration() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long position() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long seek(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getTrackName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getAlbumName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getAlbumId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getArtistName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getArtistId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getMIMEType() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enqueue(long[] jArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeLongArray(jArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long[] getQueue() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createLongArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveQueueItem(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setQueuePosition(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getPath() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getAudioId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setShuffleMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getShuffleMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int removeTracks(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int removeTrack(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRepeatMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getRepeatMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMediaMountedCount() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getAudioSessionId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean canUseAsRingtone() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.android.music.IMediaPlaybackService");
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
