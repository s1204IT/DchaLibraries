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
import java.util.List;

public interface IMediaSession2 extends IInterface {

    public static abstract class Stub extends Binder implements IMediaSession2 {

        private static class Proxy implements IMediaSession2 {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public void addPlaylistItem(IMediaController2 iMediaController2, int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeInt(i);
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
            public void adjustVolume(IMediaController2 iMediaController2, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void connect(IMediaController2 iMediaController2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void fastForward(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getChildren(IMediaController2 iMediaController2, String str, int i, int i2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(36, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getItem(IMediaController2 iMediaController2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(35, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getLibraryRoot(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(34, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getSearchResult(IMediaController2 iMediaController2, String str, int i, int i2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(38, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void pause(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void play(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void playFromMediaId(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(18, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void playFromSearch(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(17, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void playFromUri(IMediaController2 iMediaController2, Uri uri, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
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
            public void prepare(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void prepareFromMediaId(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(15, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void prepareFromSearch(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(14, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void prepareFromUri(IMediaController2 iMediaController2, Uri uri, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(13, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void release(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removePlaylistItem(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(24, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void replacePlaylistItem(IMediaController2 iMediaController2, int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(25, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reset(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void rewind(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void search(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(37, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void seekTo(IMediaController2 iMediaController2, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void selectRoute(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(33, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendCustomCommand(IMediaController2 iMediaController2, Bundle bundle, Bundle bundle2, ResultReceiver resultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
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
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPlaybackSpeed(IMediaController2 iMediaController2, float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(20, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPlaylist(IMediaController2 iMediaController2, List<Bundle> list, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
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
            public void setRating(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
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
            public void setRepeatMode(IMediaController2 iMediaController2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(29, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setShuffleMode(IMediaController2 iMediaController2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(30, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setVolumeTo(IMediaController2 iMediaController2, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void skipToNextItem(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(28, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void skipToPlaylistItem(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(26, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void skipToPreviousItem(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(27, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void subscribe(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(39, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void subscribeRoutesInfo(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(31, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unsubscribe(IMediaController2 iMediaController2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(40, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unsubscribeRoutesInfo(IMediaController2 iMediaController2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
                    this.mRemote.transact(32, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updatePlaylistMetadata(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    parcelObtain.writeStrongBinder(iMediaController2 != null ? iMediaController2.asBinder() : null);
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
        }

        public static IMediaSession2 asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("android.support.v4.media.IMediaSession2");
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IMediaSession2)) ? new Proxy(iBinder) : (IMediaSession2) iInterfaceQueryLocalInterface;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("android.support.v4.media.IMediaSession2");
                return true;
            }
            switch (i) {
                case 1:
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
                    sendCustomCommand(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 13:
                    parcel.enforceInterface("android.support.v4.media.IMediaSession2");
                    prepareFromUri(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Uri) Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
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
                    playFromUri(IMediaController2.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? (Uri) Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null);
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
    }

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
}
