package android.media.tv;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.InputChannel;
import java.util.List;

public interface ITvInputClient extends IInterface {
    void onChannelRetuned(Uri uri, int i) throws RemoteException;

    void onContentAllowed(int i) throws RemoteException;

    void onContentBlocked(String str, int i) throws RemoteException;

    void onLayoutSurface(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void onSessionCreated(String str, IBinder iBinder, InputChannel inputChannel, int i) throws RemoteException;

    void onSessionEvent(String str, Bundle bundle, int i) throws RemoteException;

    void onSessionReleased(int i) throws RemoteException;

    void onTrackSelected(int i, String str, int i2) throws RemoteException;

    void onTracksChanged(List<TvTrackInfo> list, int i) throws RemoteException;

    void onVideoAvailable(int i) throws RemoteException;

    void onVideoUnavailable(int i, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements ITvInputClient {
        private static final String DESCRIPTOR = "android.media.tv.ITvInputClient";
        static final int TRANSACTION_onChannelRetuned = 4;
        static final int TRANSACTION_onContentAllowed = 9;
        static final int TRANSACTION_onContentBlocked = 10;
        static final int TRANSACTION_onLayoutSurface = 11;
        static final int TRANSACTION_onSessionCreated = 1;
        static final int TRANSACTION_onSessionEvent = 3;
        static final int TRANSACTION_onSessionReleased = 2;
        static final int TRANSACTION_onTrackSelected = 6;
        static final int TRANSACTION_onTracksChanged = 5;
        static final int TRANSACTION_onVideoAvailable = 7;
        static final int TRANSACTION_onVideoUnavailable = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITvInputClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInputClient)) {
                return (ITvInputClient) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Uri _arg0;
            Bundle _arg1;
            InputChannel _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    IBinder _arg12 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        _arg2 = InputChannel.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    int _arg3 = data.readInt();
                    onSessionCreated(_arg02, _arg12, _arg2, _arg3);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    onSessionReleased(_arg03);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _arg22 = data.readInt();
                    onSessionEvent(_arg04, _arg1, _arg22);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _arg13 = data.readInt();
                    onChannelRetuned(_arg0, _arg13);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    List<TvTrackInfo> _arg05 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                    int _arg14 = data.readInt();
                    onTracksChanged(_arg05, _arg14);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    String _arg15 = data.readString();
                    int _arg23 = data.readInt();
                    onTrackSelected(_arg06, _arg15, _arg23);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    onVideoAvailable(_arg07);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    int _arg16 = data.readInt();
                    onVideoUnavailable(_arg08, _arg16);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    onContentAllowed(_arg09);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg17 = data.readInt();
                    onContentBlocked(_arg010, _arg17);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    int _arg18 = data.readInt();
                    int _arg24 = data.readInt();
                    int _arg32 = data.readInt();
                    int _arg4 = data.readInt();
                    onLayoutSurface(_arg011, _arg18, _arg24, _arg32, _arg4);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ITvInputClient {
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
            public void onSessionCreated(String inputId, IBinder token, InputChannel channel, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputId);
                    _data.writeStrongBinder(token);
                    if (channel != null) {
                        _data.writeInt(1);
                        channel.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(seq);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSessionReleased(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSessionEvent(String name, Bundle args, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(seq);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onChannelRetuned(Uri channelUri, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (channelUri != null) {
                        _data.writeInt(1);
                        channelUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(seq);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onTracksChanged(List<TvTrackInfo> tracks, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(tracks);
                    _data.writeInt(seq);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onTrackSelected(int type, String trackId, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(trackId);
                    _data.writeInt(seq);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onVideoAvailable(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onVideoUnavailable(int reason, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    _data.writeInt(seq);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onContentAllowed(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onContentBlocked(String rating, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(rating);
                    _data.writeInt(seq);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onLayoutSurface(int left, int top, int right, int bottom, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(right);
                    _data.writeInt(bottom);
                    _data.writeInt(seq);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
