package android.media;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRemoteControlDisplay extends IInterface {
    void setAllMetadata(int i, Bundle bundle, Bitmap bitmap) throws RemoteException;

    void setArtwork(int i, Bitmap bitmap) throws RemoteException;

    void setCurrentClientId(int i, PendingIntent pendingIntent, boolean z) throws RemoteException;

    void setEnabled(boolean z) throws RemoteException;

    void setMetadata(int i, Bundle bundle) throws RemoteException;

    void setPlaybackState(int i, int i2, long j, long j2, float f) throws RemoteException;

    void setTransportControlInfo(int i, int i2, int i3) throws RemoteException;

    public static abstract class Stub extends Binder implements IRemoteControlDisplay {
        private static final String DESCRIPTOR = "android.media.IRemoteControlDisplay";
        static final int TRANSACTION_setAllMetadata = 7;
        static final int TRANSACTION_setArtwork = 6;
        static final int TRANSACTION_setCurrentClientId = 1;
        static final int TRANSACTION_setEnabled = 2;
        static final int TRANSACTION_setMetadata = 5;
        static final int TRANSACTION_setPlaybackState = 3;
        static final int TRANSACTION_setTransportControlInfo = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteControlDisplay asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IRemoteControlDisplay)) {
                return (IRemoteControlDisplay) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Bundle _arg1;
            Bitmap _arg2;
            Bitmap _arg12;
            Bundle _arg13;
            PendingIntent _arg14;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg14 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    boolean _arg22 = data.readInt() != 0;
                    setCurrentClientId(_arg0, _arg14, _arg22);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg02 = data.readInt() != 0;
                    setEnabled(_arg02);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    int _arg15 = data.readInt();
                    long _arg23 = data.readLong();
                    long _arg3 = data.readLong();
                    float _arg4 = data.readFloat();
                    setPlaybackState(_arg03, _arg15, _arg23, _arg3, _arg4);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg16 = data.readInt();
                    int _arg24 = data.readInt();
                    setTransportControlInfo(_arg04, _arg16, _arg24);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg13 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    setMetadata(_arg05, _arg13);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg12 = Bitmap.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    setArtwork(_arg06, _arg12);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg2 = Bitmap.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    setAllMetadata(_arg07, _arg1, _arg2);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IRemoteControlDisplay {
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
            public void setCurrentClientId(int clientGeneration, PendingIntent clientMediaIntent, boolean clearing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(clientGeneration);
                    if (clientMediaIntent != null) {
                        _data.writeInt(1);
                        clientMediaIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(clearing ? 1 : 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setPlaybackState(int generationId, int state, long stateChangeTimeMs, long currentPosMs, float speed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(generationId);
                    _data.writeInt(state);
                    _data.writeLong(stateChangeTimeMs);
                    _data.writeLong(currentPosMs);
                    _data.writeFloat(speed);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setTransportControlInfo(int generationId, int transportControlFlags, int posCapabilities) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(generationId);
                    _data.writeInt(transportControlFlags);
                    _data.writeInt(posCapabilities);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setMetadata(int generationId, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(generationId);
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

            @Override
            public void setArtwork(int generationId, Bitmap artwork) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(generationId);
                    if (artwork != null) {
                        _data.writeInt(1);
                        artwork.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setAllMetadata(int generationId, Bundle metadata, Bitmap artwork) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(generationId);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (artwork != null) {
                        _data.writeInt(1);
                        artwork.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
