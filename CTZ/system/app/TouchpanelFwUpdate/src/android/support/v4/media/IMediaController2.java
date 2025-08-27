package android.support.v4.media;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.IMediaSession2;
import java.util.List;

/* loaded from: classes.dex */
public interface IMediaController2 extends IInterface {
    void onAllowedCommandsChanged(Bundle bundle) throws RemoteException;

    void onBufferingStateChanged(Bundle bundle, int i, long j) throws RemoteException;

    void onChildrenChanged(String str, int i, Bundle bundle) throws RemoteException;

    void onConnected(IMediaSession2 iMediaSession2, Bundle bundle, int i, Bundle bundle2, long j, long j2, float f, long j3, Bundle bundle3, int i2, int i3, List<Bundle> list, PendingIntent pendingIntent) throws RemoteException;

    void onCurrentMediaItemChanged(Bundle bundle) throws RemoteException;

    void onCustomCommand(Bundle bundle, Bundle bundle2, ResultReceiver resultReceiver) throws RemoteException;

    void onCustomLayoutChanged(List<Bundle> list) throws RemoteException;

    void onDisconnected() throws RemoteException;

    void onError(int i, Bundle bundle) throws RemoteException;

    void onGetChildrenDone(String str, int i, int i2, List<Bundle> list, Bundle bundle) throws RemoteException;

    void onGetItemDone(String str, Bundle bundle) throws RemoteException;

    void onGetLibraryRootDone(Bundle bundle, String str, Bundle bundle2) throws RemoteException;

    void onGetSearchResultDone(String str, int i, int i2, List<Bundle> list, Bundle bundle) throws RemoteException;

    void onPlaybackInfoChanged(Bundle bundle) throws RemoteException;

    void onPlaybackSpeedChanged(long j, long j2, float f) throws RemoteException;

    void onPlayerStateChanged(long j, long j2, int i) throws RemoteException;

    void onPlaylistChanged(List<Bundle> list, Bundle bundle) throws RemoteException;

    void onPlaylistMetadataChanged(Bundle bundle) throws RemoteException;

    void onRepeatModeChanged(int i) throws RemoteException;

    void onRoutesInfoChanged(List<Bundle> list) throws RemoteException;

    void onSearchResultChanged(String str, int i, Bundle bundle) throws RemoteException;

    void onSeekCompleted(long j, long j2, long j3) throws RemoteException;

    void onShuffleModeChanged(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IMediaController2 {
        public static IMediaController2 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("android.support.v4.media.IMediaController2");
            if (iin != null && (iin instanceof IMediaController2)) {
                return (IMediaController2) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Bundle bundle;
            Bundle bundle2;
            Bundle bundle3;
            if (i == 1598968902) {
                parcel2.writeString("android.support.v4.media.IMediaController2");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onCurrentMediaItemChanged(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 2:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onPlayerStateChanged(parcel.readLong(), parcel.readLong(), parcel.readInt());
                    return true;
                case 3:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onPlaybackSpeedChanged(parcel.readLong(), parcel.readLong(), parcel.readFloat());
                    return true;
                case 4:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onBufferingStateChanged(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readLong());
                    return true;
                case 5:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onPlaylistChanged(parcel.createTypedArrayList(Bundle.CREATOR), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 6:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onPlaylistMetadataChanged(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 7:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onPlaybackInfoChanged(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 8:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onRepeatModeChanged(parcel.readInt());
                    return true;
                case 9:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onShuffleModeChanged(parcel.readInt());
                    return true;
                case 10:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onSeekCompleted(parcel.readLong(), parcel.readLong(), parcel.readLong());
                    return true;
                case 11:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onError(parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 12:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onRoutesInfoChanged(parcel.createTypedArrayList(Bundle.CREATOR));
                    return true;
                case 13:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onConnected(IMediaSession2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readLong(), parcel.readLong(), parcel.readFloat(), parcel.readLong(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt(), parcel.createTypedArrayList(Bundle.CREATOR), parcel.readInt() != 0 ? (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 14:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onDisconnected();
                    return true;
                case 15:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onCustomLayoutChanged(parcel.createTypedArrayList(Bundle.CREATOR));
                    return true;
                case 16:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onAllowedCommandsChanged(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 17:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    if (parcel.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundle = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundle2 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundle2 = null;
                    }
                    onCustomCommand(bundle, bundle2, parcel.readInt() != 0 ? (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 18:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    if (parcel.readInt() != 0) {
                        bundle3 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundle3 = null;
                    }
                    onGetLibraryRootDone(bundle3, parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 19:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onGetItemDone(parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 20:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onChildrenChanged(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 21:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onGetChildrenDone(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.createTypedArrayList(Bundle.CREATOR), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 22:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onSearchResultChanged(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 23:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onGetSearchResultDone(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.createTypedArrayList(Bundle.CREATOR), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMediaController2 {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.support.v4.media.IMediaController2
            public void onCurrentMediaItemChanged(Bundle item) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (item != null) {
                        _data.writeInt(1);
                        item.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onPlayerStateChanged(long eventTimeMs, long positionMs, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeLong(eventTimeMs);
                    _data.writeLong(positionMs);
                    _data.writeInt(state);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeLong(eventTimeMs);
                    _data.writeLong(positionMs);
                    _data.writeFloat(speed);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onBufferingStateChanged(Bundle item, int state, long bufferedPositionMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (item != null) {
                        _data.writeInt(1);
                        item.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    _data.writeLong(bufferedPositionMs);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onPlaylistChanged(List<Bundle> playlist, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeTypedList(playlist);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onPlaylistMetadataChanged(Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onPlaybackInfoChanged(Bundle playbackInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (playbackInfo != null) {
                        _data.writeInt(1);
                        playbackInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onRepeatModeChanged(int repeatMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeInt(repeatMode);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onShuffleModeChanged(int shuffleMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeInt(shuffleMode);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeLong(eventTimeMs);
                    _data.writeLong(positionMs);
                    _data.writeLong(seekPositionMs);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onError(int errorCode, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeInt(errorCode);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeTypedList(routes);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onConnected(IMediaSession2 sessionBinder, Bundle commandGroup, int playerState, Bundle currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed, long bufferedPositionMs, Bundle playbackInfo, int repeatMode, int shuffleMode, List<Bundle> playlist, PendingIntent sessionActivity) throws Throwable {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeStrongBinder(sessionBinder != null ? sessionBinder.asBinder() : null);
                    if (commandGroup != null) {
                        _data.writeInt(1);
                        commandGroup.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(playerState);
                    if (currentItem != null) {
                        _data.writeInt(1);
                        currentItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    _data.writeLong(positionEventTimeMs);
                } catch (Throwable th2) {
                    th = th2;
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeLong(positionMs);
                } catch (Throwable th3) {
                    th = th3;
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeFloat(playbackSpeed);
                } catch (Throwable th4) {
                    th = th4;
                    _data.recycle();
                    throw th;
                }
                try {
                    _data.writeLong(bufferedPositionMs);
                    if (playbackInfo != null) {
                        _data.writeInt(1);
                        playbackInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    try {
                        _data.writeInt(repeatMode);
                    } catch (Throwable th5) {
                        th = th5;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(shuffleMode);
                        _data.writeTypedList(playlist);
                        if (sessionActivity != null) {
                            _data.writeInt(1);
                            sessionActivity.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        this.mRemote.transact(13, _data, null, 1);
                        _data.recycle();
                    } catch (Throwable th6) {
                        th = th6;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onDisconnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onCustomLayoutChanged(List<Bundle> commandButtonlist) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeTypedList(commandButtonlist);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onAllowedCommandsChanged(Bundle commands) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (commands != null) {
                        _data.writeInt(1);
                        commands.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onCustomCommand(Bundle command, Bundle args, ResultReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (command != null) {
                        _data.writeInt(1);
                        command.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (receiver != null) {
                        _data.writeInt(1);
                        receiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (rootHints != null) {
                        _data.writeInt(1);
                        rootHints.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(rootMediaId);
                    if (rootExtra != null) {
                        _data.writeInt(1);
                        rootExtra.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onGetItemDone(String mediaId, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(mediaId);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onChildrenChanged(String parentId, int itemCount, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(parentId);
                    _data.writeInt(itemCount);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onGetChildrenDone(String parentId, int page, int pageSize, List<Bundle> result, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(parentId);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    _data.writeTypedList(result);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onSearchResultChanged(String query, int itemCount, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(query);
                    _data.writeInt(itemCount);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.support.v4.media.IMediaController2
            public void onGetSearchResultDone(String query, int page, int pageSize, List<Bundle> result, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(query);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    _data.writeTypedList(result);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
