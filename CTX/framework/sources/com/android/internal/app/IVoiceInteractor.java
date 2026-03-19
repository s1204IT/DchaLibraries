package com.android.internal.app;

import android.app.VoiceInteractor;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;

public interface IVoiceInteractor extends IInterface {
    IVoiceInteractorRequest startAbortVoice(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startCommand(String str, IVoiceInteractorCallback iVoiceInteractorCallback, String str2, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startCompleteVoice(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startConfirmation(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, Bundle bundle) throws RemoteException;

    IVoiceInteractorRequest startPickOption(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, VoiceInteractor.PickOptionRequest.Option[] optionArr, Bundle bundle) throws RemoteException;

    boolean[] supportsCommands(String str, String[] strArr) throws RemoteException;

    public static abstract class Stub extends Binder implements IVoiceInteractor {
        private static final String DESCRIPTOR = "com.android.internal.app.IVoiceInteractor";
        static final int TRANSACTION_startAbortVoice = 4;
        static final int TRANSACTION_startCommand = 5;
        static final int TRANSACTION_startCompleteVoice = 3;
        static final int TRANSACTION_startConfirmation = 1;
        static final int TRANSACTION_startPickOption = 2;
        static final int TRANSACTION_supportsCommands = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceInteractor asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IVoiceInteractor)) {
                return (IVoiceInteractor) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            VoiceInteractor.Prompt promptCreateFromParcel;
            Bundle bundleCreateFromParcel;
            VoiceInteractor.Prompt promptCreateFromParcel2;
            Bundle bundleCreateFromParcel2;
            VoiceInteractor.Prompt promptCreateFromParcel3;
            Bundle bundleCreateFromParcel3;
            VoiceInteractor.Prompt promptCreateFromParcel4;
            Bundle bundleCreateFromParcel4;
            Bundle bundleCreateFromParcel5;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    IVoiceInteractorCallback iVoiceInteractorCallbackAsInterface = IVoiceInteractorCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        promptCreateFromParcel = VoiceInteractor.Prompt.CREATOR.createFromParcel(parcel);
                    } else {
                        promptCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    IVoiceInteractorRequest iVoiceInteractorRequestStartConfirmation = startConfirmation(string, iVoiceInteractorCallbackAsInterface, promptCreateFromParcel, bundleCreateFromParcel);
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iVoiceInteractorRequestStartConfirmation != null ? iVoiceInteractorRequestStartConfirmation.asBinder() : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string2 = parcel.readString();
                    IVoiceInteractorCallback iVoiceInteractorCallbackAsInterface2 = IVoiceInteractorCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        promptCreateFromParcel2 = VoiceInteractor.Prompt.CREATOR.createFromParcel(parcel);
                    } else {
                        promptCreateFromParcel2 = null;
                    }
                    VoiceInteractor.PickOptionRequest.Option[] optionArr = (VoiceInteractor.PickOptionRequest.Option[]) parcel.createTypedArray(VoiceInteractor.PickOptionRequest.Option.CREATOR);
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel2 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel2 = null;
                    }
                    IVoiceInteractorRequest iVoiceInteractorRequestStartPickOption = startPickOption(string2, iVoiceInteractorCallbackAsInterface2, promptCreateFromParcel2, optionArr, bundleCreateFromParcel2);
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iVoiceInteractorRequestStartPickOption != null ? iVoiceInteractorRequestStartPickOption.asBinder() : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string3 = parcel.readString();
                    IVoiceInteractorCallback iVoiceInteractorCallbackAsInterface3 = IVoiceInteractorCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        promptCreateFromParcel3 = VoiceInteractor.Prompt.CREATOR.createFromParcel(parcel);
                    } else {
                        promptCreateFromParcel3 = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel3 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel3 = null;
                    }
                    IVoiceInteractorRequest iVoiceInteractorRequestStartCompleteVoice = startCompleteVoice(string3, iVoiceInteractorCallbackAsInterface3, promptCreateFromParcel3, bundleCreateFromParcel3);
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iVoiceInteractorRequestStartCompleteVoice != null ? iVoiceInteractorRequestStartCompleteVoice.asBinder() : null);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string4 = parcel.readString();
                    IVoiceInteractorCallback iVoiceInteractorCallbackAsInterface4 = IVoiceInteractorCallback.Stub.asInterface(parcel.readStrongBinder());
                    if (parcel.readInt() != 0) {
                        promptCreateFromParcel4 = VoiceInteractor.Prompt.CREATOR.createFromParcel(parcel);
                    } else {
                        promptCreateFromParcel4 = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel4 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel4 = null;
                    }
                    IVoiceInteractorRequest iVoiceInteractorRequestStartAbortVoice = startAbortVoice(string4, iVoiceInteractorCallbackAsInterface4, promptCreateFromParcel4, bundleCreateFromParcel4);
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iVoiceInteractorRequestStartAbortVoice != null ? iVoiceInteractorRequestStartAbortVoice.asBinder() : null);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string5 = parcel.readString();
                    IVoiceInteractorCallback iVoiceInteractorCallbackAsInterface5 = IVoiceInteractorCallback.Stub.asInterface(parcel.readStrongBinder());
                    String string6 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel5 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel5 = null;
                    }
                    IVoiceInteractorRequest iVoiceInteractorRequestStartCommand = startCommand(string5, iVoiceInteractorCallbackAsInterface5, string6, bundleCreateFromParcel5);
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iVoiceInteractorRequestStartCommand != null ? iVoiceInteractorRequestStartCommand.asBinder() : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean[] zArrSupportsCommands = supportsCommands(parcel.readString(), parcel.createStringArray());
                    parcel2.writeNoException();
                    parcel2.writeBooleanArray(zArrSupportsCommands);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IVoiceInteractor {
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
            public IVoiceInteractorRequest startConfirmation(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iVoiceInteractorCallback != null ? iVoiceInteractorCallback.asBinder() : null);
                    if (prompt != null) {
                        parcelObtain.writeInt(1);
                        prompt.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IVoiceInteractorRequest.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startPickOption(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, VoiceInteractor.PickOptionRequest.Option[] optionArr, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iVoiceInteractorCallback != null ? iVoiceInteractorCallback.asBinder() : null);
                    if (prompt != null) {
                        parcelObtain.writeInt(1);
                        prompt.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeTypedArray(optionArr, 0);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IVoiceInteractorRequest.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startCompleteVoice(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iVoiceInteractorCallback != null ? iVoiceInteractorCallback.asBinder() : null);
                    if (prompt != null) {
                        parcelObtain.writeInt(1);
                        prompt.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IVoiceInteractorRequest.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startAbortVoice(String str, IVoiceInteractorCallback iVoiceInteractorCallback, VoiceInteractor.Prompt prompt, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iVoiceInteractorCallback != null ? iVoiceInteractorCallback.asBinder() : null);
                    if (prompt != null) {
                        parcelObtain.writeInt(1);
                        prompt.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IVoiceInteractorRequest.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IVoiceInteractorRequest startCommand(String str, IVoiceInteractorCallback iVoiceInteractorCallback, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iVoiceInteractorCallback != null ? iVoiceInteractorCallback.asBinder() : null);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IVoiceInteractorRequest.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean[] supportsCommands(String str, String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createBooleanArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
