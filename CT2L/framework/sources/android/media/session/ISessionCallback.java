package android.media.session;

import android.content.Intent;
import android.media.Rating;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;

public interface ISessionCallback extends IInterface {
    void onAdjustVolume(int i) throws RemoteException;

    void onCommand(String str, Bundle bundle, ResultReceiver resultReceiver) throws RemoteException;

    void onCustomAction(String str, Bundle bundle) throws RemoteException;

    void onFastForward() throws RemoteException;

    void onMediaButton(Intent intent, int i, ResultReceiver resultReceiver) throws RemoteException;

    void onNext() throws RemoteException;

    void onPause() throws RemoteException;

    void onPlay() throws RemoteException;

    void onPlayFromMediaId(String str, Bundle bundle) throws RemoteException;

    void onPlayFromSearch(String str, Bundle bundle) throws RemoteException;

    void onPrevious() throws RemoteException;

    void onRate(Rating rating) throws RemoteException;

    void onRewind() throws RemoteException;

    void onSeekTo(long j) throws RemoteException;

    void onSetVolumeTo(int i) throws RemoteException;

    void onSkipToTrack(long j) throws RemoteException;

    void onStop() throws RemoteException;

    public static abstract class Stub extends Binder implements ISessionCallback {
        private static final String DESCRIPTOR = "android.media.session.ISessionCallback";
        static final int TRANSACTION_onAdjustVolume = 16;
        static final int TRANSACTION_onCommand = 1;
        static final int TRANSACTION_onCustomAction = 15;
        static final int TRANSACTION_onFastForward = 11;
        static final int TRANSACTION_onMediaButton = 2;
        static final int TRANSACTION_onNext = 9;
        static final int TRANSACTION_onPause = 7;
        static final int TRANSACTION_onPlay = 3;
        static final int TRANSACTION_onPlayFromMediaId = 4;
        static final int TRANSACTION_onPlayFromSearch = 5;
        static final int TRANSACTION_onPrevious = 10;
        static final int TRANSACTION_onRate = 14;
        static final int TRANSACTION_onRewind = 12;
        static final int TRANSACTION_onSeekTo = 13;
        static final int TRANSACTION_onSetVolumeTo = 17;
        static final int TRANSACTION_onSkipToTrack = 6;
        static final int TRANSACTION_onStop = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISessionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISessionCallback)) {
                return (ISessionCallback) iin;
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
            Rating _arg0;
            Bundle _arg12;
            Bundle _arg13;
            Intent _arg02;
            ResultReceiver _arg2;
            Bundle _arg14;
            ResultReceiver _arg22;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    if (data.readInt() != 0) {
                        _arg14 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg14 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg22 = ResultReceiver.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    onCommand(_arg03, _arg14, _arg22);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    int _arg15 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg2 = ResultReceiver.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    onMediaButton(_arg02, _arg15, _arg2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    onPlay();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    if (data.readInt() != 0) {
                        _arg13 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg13 = null;
                    }
                    onPlayFromMediaId(_arg04, _arg13);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    if (data.readInt() != 0) {
                        _arg12 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    onPlayFromSearch(_arg05, _arg12);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg06 = data.readLong();
                    onSkipToTrack(_arg06);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    onPause();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    onStop();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    onNext();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    onPrevious();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    onFastForward();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    onRewind();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg07 = data.readLong();
                    onSeekTo(_arg07);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = Rating.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    onRate(_arg0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    onCustomAction(_arg08, _arg1);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    onAdjustVolume(_arg09);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    onSetVolumeTo(_arg010);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISessionCallback {
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
            public void onCommand(String command, Bundle args, ResultReceiver cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(command);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (cb != null) {
                        _data.writeInt(1);
                        cb.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMediaButton(Intent mediaButtonIntent, int sequenceNumber, ResultReceiver cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (mediaButtonIntent != null) {
                        _data.writeInt(1);
                        mediaButtonIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sequenceNumber);
                    if (cb != null) {
                        _data.writeInt(1);
                        cb.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPlay() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPlayFromMediaId(String uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uri);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPlayFromSearch(String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSkipToTrack(long id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(id);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onStop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onNext() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPrevious() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onFastForward() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onRewind() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSeekTo(long pos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(pos);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onRate(Rating rating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (rating != null) {
                        _data.writeInt(1);
                        rating.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCustomAction(String action, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onAdjustVolume(int direction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(direction);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSetVolumeTo(int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(value);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
