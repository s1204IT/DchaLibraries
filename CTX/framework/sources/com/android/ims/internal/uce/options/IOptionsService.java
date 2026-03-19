package com.android.ims.internal.uce.options;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.ims.internal.uce.common.CapInfo;
import com.android.ims.internal.uce.common.StatusCode;
import com.android.ims.internal.uce.common.UceLong;
import com.android.ims.internal.uce.options.IOptionsListener;

public interface IOptionsService extends IInterface {
    StatusCode addListener(int i, IOptionsListener iOptionsListener, UceLong uceLong) throws RemoteException;

    StatusCode getContactCap(int i, String str, int i2) throws RemoteException;

    StatusCode getContactListCap(int i, String[] strArr, int i2) throws RemoteException;

    StatusCode getMyInfo(int i, int i2) throws RemoteException;

    StatusCode getVersion(int i) throws RemoteException;

    StatusCode removeListener(int i, UceLong uceLong) throws RemoteException;

    StatusCode responseIncomingOptions(int i, int i2, int i3, String str, OptionsCapInfo optionsCapInfo, boolean z) throws RemoteException;

    StatusCode setMyInfo(int i, CapInfo capInfo, int i2) throws RemoteException;

    public static abstract class Stub extends Binder implements IOptionsService {
        private static final String DESCRIPTOR = "com.android.ims.internal.uce.options.IOptionsService";
        static final int TRANSACTION_addListener = 2;
        static final int TRANSACTION_getContactCap = 6;
        static final int TRANSACTION_getContactListCap = 7;
        static final int TRANSACTION_getMyInfo = 5;
        static final int TRANSACTION_getVersion = 1;
        static final int TRANSACTION_removeListener = 3;
        static final int TRANSACTION_responseIncomingOptions = 8;
        static final int TRANSACTION_setMyInfo = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOptionsService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IOptionsService)) {
                return (IOptionsService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode version = getVersion(parcel.readInt());
                    parcel2.writeNoException();
                    if (version != null) {
                        parcel2.writeInt(1);
                        version.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    IOptionsListener iOptionsListenerAsInterface = IOptionsListener.Stub.asInterface(parcel.readStrongBinder());
                    UceLong uceLongCreateFromParcel = parcel.readInt() != 0 ? UceLong.CREATOR.createFromParcel(parcel) : null;
                    StatusCode statusCodeAddListener = addListener(i3, iOptionsListenerAsInterface, uceLongCreateFromParcel);
                    parcel2.writeNoException();
                    if (statusCodeAddListener != null) {
                        parcel2.writeInt(1);
                        statusCodeAddListener.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    if (uceLongCreateFromParcel != null) {
                        parcel2.writeInt(1);
                        uceLongCreateFromParcel.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode statusCodeRemoveListener = removeListener(parcel.readInt(), parcel.readInt() != 0 ? UceLong.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    if (statusCodeRemoveListener != null) {
                        parcel2.writeInt(1);
                        statusCodeRemoveListener.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode myInfo = setMyInfo(parcel.readInt(), parcel.readInt() != 0 ? CapInfo.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    if (myInfo != null) {
                        parcel2.writeInt(1);
                        myInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode myInfo2 = getMyInfo(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (myInfo2 != null) {
                        parcel2.writeInt(1);
                        myInfo2.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode contactCap = getContactCap(parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (contactCap != null) {
                        parcel2.writeInt(1);
                        contactCap.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode contactListCap = getContactListCap(parcel.readInt(), parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    if (contactListCap != null) {
                        parcel2.writeInt(1);
                        contactListCap.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    StatusCode statusCodeResponseIncomingOptions = responseIncomingOptions(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt() != 0 ? OptionsCapInfo.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (statusCodeResponseIncomingOptions != null) {
                        parcel2.writeInt(1);
                        statusCodeResponseIncomingOptions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IOptionsService {
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
            public StatusCode getVersion(int i) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode addListener(int i, IOptionsListener iOptionsListener, UceLong uceLong) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    StatusCode statusCodeCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iOptionsListener != null ? iOptionsListener.asBinder() : null);
                    if (uceLong != null) {
                        parcelObtain.writeInt(1);
                        uceLong.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    }
                    if (parcelObtain2.readInt() != 0) {
                        uceLong.readFromParcel(parcelObtain2);
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode removeListener(int i, UceLong uceLong) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (uceLong != null) {
                        parcelObtain.writeInt(1);
                        uceLong.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode setMyInfo(int i, CapInfo capInfo, int i2) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (capInfo != null) {
                        parcelObtain.writeInt(1);
                        capInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode getMyInfo(int i, int i2) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode getContactCap(int i, String str, int i2) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode getContactListCap(int i, String[] strArr, int i2) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public StatusCode responseIncomingOptions(int i, int i2, int i3, String str, OptionsCapInfo optionsCapInfo, boolean z) throws RemoteException {
                StatusCode statusCodeCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str);
                    if (optionsCapInfo != null) {
                        parcelObtain.writeInt(1);
                        optionsCapInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        statusCodeCreateFromParcel = StatusCode.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        statusCodeCreateFromParcel = null;
                    }
                    return statusCodeCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
