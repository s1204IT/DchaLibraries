package android.media.session;

import android.content.Intent;
import android.media.Rating;
import android.media.session.ISessionControllerCallback;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;

public interface ISessionCallback extends IInterface {
    void onAdjustVolume(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) throws RemoteException;

    void onCommand(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle, ResultReceiver resultReceiver) throws RemoteException;

    void onCustomAction(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onFastForward(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onMediaButton(String str, int i, int i2, Intent intent, int i3, ResultReceiver resultReceiver) throws RemoteException;

    void onMediaButtonFromController(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Intent intent) throws RemoteException;

    void onNext(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPause(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPlay(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPlayFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPlayFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPlayFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) throws RemoteException;

    void onPrepare(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPrepareFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPrepareFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPrepareFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) throws RemoteException;

    void onPrevious(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onRate(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Rating rating) throws RemoteException;

    void onRewind(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onSeekTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) throws RemoteException;

    void onSetVolumeTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) throws RemoteException;

    void onSkipToTrack(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) throws RemoteException;

    void onStop(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements ISessionCallback {
        private static final String DESCRIPTOR = "android.media.session.ISessionCallback";
        static final int TRANSACTION_onAdjustVolume = 22;
        static final int TRANSACTION_onCommand = 1;
        static final int TRANSACTION_onCustomAction = 21;
        static final int TRANSACTION_onFastForward = 17;
        static final int TRANSACTION_onMediaButton = 2;
        static final int TRANSACTION_onMediaButtonFromController = 3;
        static final int TRANSACTION_onNext = 15;
        static final int TRANSACTION_onPause = 13;
        static final int TRANSACTION_onPlay = 8;
        static final int TRANSACTION_onPlayFromMediaId = 9;
        static final int TRANSACTION_onPlayFromSearch = 10;
        static final int TRANSACTION_onPlayFromUri = 11;
        static final int TRANSACTION_onPrepare = 4;
        static final int TRANSACTION_onPrepareFromMediaId = 5;
        static final int TRANSACTION_onPrepareFromSearch = 6;
        static final int TRANSACTION_onPrepareFromUri = 7;
        static final int TRANSACTION_onPrevious = 16;
        static final int TRANSACTION_onRate = 20;
        static final int TRANSACTION_onRewind = 18;
        static final int TRANSACTION_onSeekTo = 19;
        static final int TRANSACTION_onSetVolumeTo = 23;
        static final int TRANSACTION_onSkipToTrack = 12;
        static final int TRANSACTION_onStop = 14;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISessionCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISessionCallback)) {
                return (ISessionCallback) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            ResultReceiver resultReceiverCreateFromParcel;
            Intent intentCreateFromParcel;
            ResultReceiver resultReceiverCreateFromParcel2;
            Intent intentCreateFromParcel2;
            Bundle bundleCreateFromParcel2;
            Bundle bundleCreateFromParcel3;
            Uri uriCreateFromParcel;
            Bundle bundleCreateFromParcel4;
            Bundle bundleCreateFromParcel5;
            Bundle bundleCreateFromParcel6;
            Uri uriCreateFromParcel2;
            Bundle bundleCreateFromParcel7;
            Rating ratingCreateFromParcel;
            Bundle bundleCreateFromParcel8;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    int i3 = parcel.readInt();
                    int i4 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string2 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        resultReceiverCreateFromParcel = ResultReceiver.CREATOR.createFromParcel(parcel);
                    } else {
                        resultReceiverCreateFromParcel = null;
                    }
                    onCommand(string, i3, i4, iSessionControllerCallbackAsInterface, string2, bundleCreateFromParcel, resultReceiverCreateFromParcel);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string3 = parcel.readString();
                    int i5 = parcel.readInt();
                    int i6 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel = null;
                    }
                    int i7 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        resultReceiverCreateFromParcel2 = ResultReceiver.CREATOR.createFromParcel(parcel);
                    } else {
                        resultReceiverCreateFromParcel2 = null;
                    }
                    onMediaButton(string3, i5, i6, intentCreateFromParcel, i7, resultReceiverCreateFromParcel2);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string4 = parcel.readString();
                    int i8 = parcel.readInt();
                    int i9 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface2 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel2 = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel2 = null;
                    }
                    onMediaButtonFromController(string4, i8, i9, iSessionControllerCallbackAsInterface2, intentCreateFromParcel2);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPrepare(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string5 = parcel.readString();
                    int i10 = parcel.readInt();
                    int i11 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface3 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string6 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel2 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel2 = null;
                    }
                    onPrepareFromMediaId(string5, i10, i11, iSessionControllerCallbackAsInterface3, string6, bundleCreateFromParcel2);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string7 = parcel.readString();
                    int i12 = parcel.readInt();
                    int i13 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface4 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string8 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel3 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel3 = null;
                    }
                    onPrepareFromSearch(string7, i12, i13, iSessionControllerCallbackAsInterface4, string8, bundleCreateFromParcel3);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string9 = parcel.readString();
                    int i14 = parcel.readInt();
                    int i15 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface5 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel4 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel4 = null;
                    }
                    onPrepareFromUri(string9, i14, i15, iSessionControllerCallbackAsInterface5, uriCreateFromParcel, bundleCreateFromParcel4);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPlay(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string10 = parcel.readString();
                    int i16 = parcel.readInt();
                    int i17 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface6 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string11 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel5 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel5 = null;
                    }
                    onPlayFromMediaId(string10, i16, i17, iSessionControllerCallbackAsInterface6, string11, bundleCreateFromParcel5);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string12 = parcel.readString();
                    int i18 = parcel.readInt();
                    int i19 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface7 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string13 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel6 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel6 = null;
                    }
                    onPlayFromSearch(string12, i18, i19, iSessionControllerCallbackAsInterface7, string13, bundleCreateFromParcel6);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string14 = parcel.readString();
                    int i20 = parcel.readInt();
                    int i21 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface8 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel2 = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel2 = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel7 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel7 = null;
                    }
                    onPlayFromUri(string14, i20, i21, iSessionControllerCallbackAsInterface8, uriCreateFromParcel2, bundleCreateFromParcel7);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSkipToTrack(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readLong());
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPause(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    onStop(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNext(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPrevious(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFastForward(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    onRewind(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()));
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSeekTo(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readLong());
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string15 = parcel.readString();
                    int i22 = parcel.readInt();
                    int i23 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface9 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        ratingCreateFromParcel = Rating.CREATOR.createFromParcel(parcel);
                    } else {
                        ratingCreateFromParcel = null;
                    }
                    onRate(string15, i22, i23, iSessionControllerCallbackAsInterface9, ratingCreateFromParcel);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string16 = parcel.readString();
                    int i24 = parcel.readInt();
                    int i25 = parcel.readInt();
                    ISessionControllerCallback iSessionControllerCallbackAsInterface10 = ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string17 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel8 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel8 = null;
                    }
                    onCustomAction(string16, i24, i25, iSessionControllerCallbackAsInterface10, string17, bundleCreateFromParcel8);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    onAdjustVolume(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    onSetVolumeTo(parcel.readString(), parcel.readInt(), parcel.readInt(), ISessionControllerCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ISessionCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void onCommand(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle, ResultReceiver resultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (resultReceiver != null) {
                        parcelObtain.writeInt(1);
                        resultReceiver.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onMediaButton(String str, int i, int i2, Intent intent, int i3, ResultReceiver resultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i3);
                    if (resultReceiver != null) {
                        parcelObtain.writeInt(1);
                        resultReceiver.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onMediaButtonFromController(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Intent intent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPrepare(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPrepareFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
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
            public void onPrepareFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
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
            public void onPrepareFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
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
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlay(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlayFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlayFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPlayFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
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
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSkipToTrack(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPause(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(13, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onStop(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(14, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNext(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(15, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPrevious(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(16, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFastForward(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(17, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRewind(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    this.mRemote.transact(18, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSeekTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(19, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRate(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Rating rating) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    if (rating != null) {
                        parcelObtain.writeInt(1);
                        rating.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(20, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onCustomAction(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
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
            public void onAdjustVolume(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(22, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onSetVolumeTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iSessionControllerCallback != null ? iSessionControllerCallback.asBinder() : null);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(23, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
