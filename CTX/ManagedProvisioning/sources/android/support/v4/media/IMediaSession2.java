package android.support.v4.media;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.IMediaController2;
import com.android.managedprovisioning.preprovisioning.EncryptionController;
import java.util.List;

public interface IMediaSession2 extends IInterface {
    void addPlaylistItem(IMediaController2 iMediaController2, int i, Bundle bundle) throws RemoteException;

    void adjustVolume(IMediaController2 iMediaController2, int i, int i2) throws RemoteException;

    void connect(IMediaController2 iMediaController2, String str) throws RemoteException;

    void fastForward(IMediaController2 iMediaController2) throws RemoteException;

    void getChildren(IMediaController2 iMediaController2, String str, int i, int i2, Bundle bundle) throws RemoteException;

    void getItem(IMediaController2 iMediaController2, String str) throws RemoteException;

    void getLibraryRoot(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void getSearchResult(IMediaController2 iMediaController2, String str, int i, int i2, Bundle bundle) throws RemoteException;

    void pause(IMediaController2 iMediaController2) throws RemoteException;

    void play(IMediaController2 iMediaController2) throws RemoteException;

    void playFromMediaId(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void playFromSearch(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void playFromUri(IMediaController2 iMediaController2, Uri uri, Bundle bundle) throws RemoteException;

    void prepare(IMediaController2 iMediaController2) throws RemoteException;

    void prepareFromMediaId(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void prepareFromSearch(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void prepareFromUri(IMediaController2 iMediaController2, Uri uri, Bundle bundle) throws RemoteException;

    void release(IMediaController2 iMediaController2) throws RemoteException;

    void removePlaylistItem(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void replacePlaylistItem(IMediaController2 iMediaController2, int i, Bundle bundle) throws RemoteException;

    void reset(IMediaController2 iMediaController2) throws RemoteException;

    void rewind(IMediaController2 iMediaController2) throws RemoteException;

    void search(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void seekTo(IMediaController2 iMediaController2, long j) throws RemoteException;

    void selectRoute(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void sendCustomCommand(IMediaController2 iMediaController2, Bundle bundle, Bundle bundle2, ResultReceiver resultReceiver) throws RemoteException;

    void setPlaybackSpeed(IMediaController2 iMediaController2, float f) throws RemoteException;

    void setPlaylist(IMediaController2 iMediaController2, List<Bundle> list, Bundle bundle) throws RemoteException;

    void setRating(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void setRepeatMode(IMediaController2 iMediaController2, int i) throws RemoteException;

    void setShuffleMode(IMediaController2 iMediaController2, int i) throws RemoteException;

    void setVolumeTo(IMediaController2 iMediaController2, int i, int i2) throws RemoteException;

    void skipToNextItem(IMediaController2 iMediaController2) throws RemoteException;

    void skipToPlaylistItem(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void skipToPreviousItem(IMediaController2 iMediaController2) throws RemoteException;

    void subscribe(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void subscribeRoutesInfo(IMediaController2 iMediaController2) throws RemoteException;

    void unsubscribe(IMediaController2 iMediaController2, String str) throws RemoteException;

    void unsubscribeRoutesInfo(IMediaController2 iMediaController2) throws RemoteException;

    void updatePlaylistMetadata(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    public static abstract class Stub extends Binder implements IMediaSession2 {
        public static IMediaSession2 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("android.support.v4.media.IMediaSession2");
            if (iin != null && (iin instanceof IMediaSession2)) {
                return (IMediaSession2) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Bundle bundle;
            Bundle bundle2;
            Uri uri;
            Uri uri2;
            if (i == 1598968902) {
                parcel2.writeString("android.support.v4.media.IMediaSession2");
                return true;
            }
            switch (i) {
                case EncryptionController.NOTIFICATION_ID:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    connect(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    return true;
                case 2:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    release(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 3:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    setVolumeTo(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt());
                    return true;
                case 4:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    adjustVolume(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt());
                    return true;
                case 5:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    play(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 6:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    pause(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 7:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    reset(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 8:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    prepare(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 9:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    fastForward(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 10:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    rewind(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 11:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    seekTo(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readLong());
                    return true;
                case 12:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    IMediaController2 iMediaController2AsInterface = IMediaController2.Stub.asInterface(parcel.readStrongBinder());
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
                    sendCustomCommand(iMediaController2AsInterface, bundle, bundle2, parcel.readInt() != 0 ? (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 13:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    IMediaController2 iMediaController2AsInterface2 = IMediaController2.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        uri = (Uri) Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uri = null;
                    }
                    prepareFromUri(iMediaController2AsInterface2, uri, parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 14:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    prepareFromSearch(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 15:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    prepareFromMediaId(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 16:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    IMediaController2 iMediaController2AsInterface3 = IMediaController2.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        uri2 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uri2 = null;
                    }
                    playFromUri(iMediaController2AsInterface3, uri2, parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 17:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    playFromSearch(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 18:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    playFromMediaId(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 19:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    setRating(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 20:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    setPlaybackSpeed(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readFloat());
                    return true;
                case 21:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    setPlaylist(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.createTypedArrayList(Bundle.CREATOR), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 22:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    updatePlaylistMetadata(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 23:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    addPlaylistItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 24:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    removePlaylistItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 25:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    replacePlaylistItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 26:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    skipToPlaylistItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 27:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    skipToPreviousItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 28:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    skipToNextItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 29:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    setRepeatMode(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    return true;
                case 30:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    setShuffleMode(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    return true;
                case 31:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    subscribeRoutesInfo(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 32:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    unsubscribeRoutesInfo(IMediaController2.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 33:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    selectRoute(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 34:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    getLibraryRoot(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 35:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    getItem(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    return true;
                case 36:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    getChildren(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 37:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    search(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 38:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    getSearchResult(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 39:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    subscribe(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 40:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    unsubscribe(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMediaSession2 {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void connect(IMediaController2 caller, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void release(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setVolumeTo(IMediaController2 caller, int value, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(value);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void adjustVolume(IMediaController2 caller, int direction, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(direction);
                    _data.writeInt(flags);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void play(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void pause(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void reset(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void prepare(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void fastForward(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void rewind(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void seekTo(IMediaController2 caller, long pos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeLong(pos);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void sendCustomCommand(IMediaController2 caller, Bundle command, Bundle args, ResultReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
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
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void prepareFromUri(IMediaController2 caller, Uri uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void prepareFromSearch(IMediaController2 caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void prepareFromMediaId(IMediaController2 caller, String mediaId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void playFromUri(IMediaController2 caller, Uri uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void playFromSearch(IMediaController2 caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void playFromMediaId(IMediaController2 caller, String mediaId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setRating(IMediaController2 caller, String mediaId, Bundle rating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (rating != null) {
                        _data.writeInt(1);
                        rating.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setPlaybackSpeed(IMediaController2 caller, float speed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeFloat(speed);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setPlaylist(IMediaController2 caller, List<Bundle> playlist, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeTypedList(playlist);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void updatePlaylistMetadata(IMediaController2 caller, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void addPlaylistItem(IMediaController2 caller, int index, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(index);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void removePlaylistItem(IMediaController2 caller, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void replacePlaylistItem(IMediaController2 caller, int index, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(index);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void skipToPlaylistItem(IMediaController2 caller, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void skipToPreviousItem(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void skipToNextItem(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setRepeatMode(IMediaController2 caller, int repeatMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(repeatMode);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setShuffleMode(IMediaController2 caller, int shuffleMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(shuffleMode);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void subscribeRoutesInfo(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void unsubscribeRoutesInfo(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void selectRoute(IMediaController2 caller, Bundle route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getLibraryRoot(IMediaController2 caller, Bundle rootHints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (rootHints != null) {
                        _data.writeInt(1);
                        rootHints.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getItem(IMediaController2 caller, String mediaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getChildren(IMediaController2 caller, String parentId, int page, int pageSize, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(parentId);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void search(IMediaController2 caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void getSearchResult(IMediaController2 caller, String query, int page, int pageSize, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void subscribe(IMediaController2 caller, String parentId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(parentId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(39, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void unsubscribe(IMediaController2 caller, String parentId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(parentId);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
