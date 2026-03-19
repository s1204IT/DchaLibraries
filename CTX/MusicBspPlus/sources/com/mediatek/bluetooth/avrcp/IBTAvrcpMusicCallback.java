package com.mediatek.bluetooth.avrcp;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IBTAvrcpMusicCallback extends IInterface {
    void notifyAppSettingChanged() throws RemoteException;

    void notifyNowPlayingContentChanged() throws RemoteException;

    void notifyPlaybackPosChanged() throws RemoteException;

    void notifyPlaybackStatus(byte b) throws RemoteException;

    void notifyTrackChanged(long j) throws RemoteException;

    void notifyTrackReachEnd() throws RemoteException;

    void notifyTrackReachStart() throws RemoteException;

    void notifyVolumehanged(byte b) throws RemoteException;

    public static abstract class Stub extends Binder implements IBTAvrcpMusicCallback {
        public static IBTAvrcpMusicCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IBTAvrcpMusicCallback)) {
                return (IBTAvrcpMusicCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyPlaybackStatus(parcel.readByte());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyTrackChanged(parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyTrackReachStart();
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyTrackReachEnd();
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyPlaybackPosChanged();
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyAppSettingChanged();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyNowPlayingContentChanged();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    notifyVolumehanged(parcel.readByte());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IBTAvrcpMusicCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override
            public void notifyPlaybackStatus(byte b) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    parcelObtain.writeByte(b);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyTrackChanged(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyTrackReachStart() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyTrackReachEnd() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyPlaybackPosChanged() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyAppSettingChanged() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyNowPlayingContentChanged() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyVolumehanged(byte b) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken("com.mediatek.bluetooth.avrcp.IBTAvrcpMusicCallback");
                    parcelObtain.writeByte(b);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
