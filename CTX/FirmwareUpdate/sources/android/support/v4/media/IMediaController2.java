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

public interface IMediaController2 extends IInterface {

    public static abstract class Stub extends Binder implements IMediaController2 {

        private static class Proxy implements IMediaController2 {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void onAllowedCommandsChanged(Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(16, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onBufferingStateChanged(Bundle bundle, int i, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onChildrenChanged(String str, int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(20, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onConnected(IMediaSession2 iMediaSession2, Bundle bundle, int i, Bundle bundle2, long j, long j2, float f, long j3, Bundle bundle3, int i2, int i3, List<Bundle> list, PendingIntent pendingIntent) throws Throwable {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeStrongBinder(iMediaSession2 != null ? iMediaSession2.asBinder() : null);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    if (bundle2 != null) {
                        parcelObtain.writeInt(1);
                        bundle2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    try {
                        parcelObtain.writeLong(j);
                        try {
                            parcelObtain.writeLong(j2);
                            try {
                                parcelObtain.writeFloat(f);
                                try {
                                    parcelObtain.writeLong(j3);
                                    if (bundle3 != null) {
                                        parcelObtain.writeInt(1);
                                        bundle3.writeToParcel(parcelObtain, 0);
                                    } else {
                                        parcelObtain.writeInt(0);
                                    }
                                    try {
                                        parcelObtain.writeInt(i2);
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                            try {
                                parcelObtain.writeInt(i3);
                                parcelObtain.writeTypedList(list);
                                if (pendingIntent != null) {
                                    parcelObtain.writeInt(1);
                                    pendingIntent.writeToParcel(parcelObtain, 0);
                                } else {
                                    parcelObtain.writeInt(0);
                                }
                                this.mRemote.transact(13, parcelObtain, null, 1);
                                parcelObtain.recycle();
                            } catch (Throwable th4) {
                                th = th4;
                                parcelObtain.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                } catch (Throwable th7) {
                    th = th7;
                }
            }

            @Override
            public void onCurrentMediaItemChanged(Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onCustomCommand(Bundle bundle, Bundle bundle2, ResultReceiver resultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle2 != null) {
                        parcelObtain.writeInt(1);
                        bundle2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (resultReceiver != null) {
                        parcelObtain.writeInt(1);
                        resultReceiver.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(17, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onCustomLayoutChanged(List<Bundle> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(15, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onDisconnected() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    this.mRemote.transact(14, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onError(int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGetChildrenDone(String str, int i, int i2, List<Bundle> list, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeTypedList(list);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(21, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGetItemDone(String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(19, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGetLibraryRootDone(Bundle bundle, String str, Bundle bundle2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    if (bundle2 != null) {
                        parcelObtain.writeInt(1);
                        bundle2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(18, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGetSearchResultDone(String str, int i, int i2, List<Bundle> list, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeTypedList(list);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(23, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlaybackInfoChanged(Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlaybackSpeedChanged(long j, long j2, float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlayerStateChanged(long j, long j2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlaylistChanged(List<Bundle> list, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeTypedList(list);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlaylistMetadataChanged(Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRepeatModeChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRoutesInfoChanged(List<Bundle> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSearchResultChanged(String str, int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(22, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSeekCompleted(long j, long j2, long j3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    parcelObtain.writeLong(j3);
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onShuffleModeChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }

        public static IMediaController2 asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("android.support.v4.media.IMediaController2");
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IMediaController2)) ? new Proxy(iBinder) : (IMediaController2) iInterfaceQueryLocalInterface;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
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
                    onCustomCommand(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 18:
                    parcel.enforceInterface("android.support.v4.media.IMediaController2");
                    onGetLibraryRootDone(parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
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
    }

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
}
