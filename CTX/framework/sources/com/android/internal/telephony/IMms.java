package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMms extends IInterface {
    Uri addMultimediaMessageDraft(String str, Uri uri) throws RemoteException;

    Uri addTextMessageDraft(String str, String str2, String str3) throws RemoteException;

    boolean archiveStoredConversation(String str, long j, boolean z) throws RemoteException;

    boolean deleteStoredConversation(String str, long j) throws RemoteException;

    boolean deleteStoredMessage(String str, Uri uri) throws RemoteException;

    void downloadMessage(int i, String str, String str2, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException;

    boolean getAutoPersisting() throws RemoteException;

    Bundle getCarrierConfigValues(int i) throws RemoteException;

    Uri importMultimediaMessage(String str, Uri uri, String str2, long j, boolean z, boolean z2) throws RemoteException;

    Uri importTextMessage(String str, String str2, int i, String str3, long j, boolean z, boolean z2) throws RemoteException;

    void sendMessage(int i, String str, Uri uri, String str2, Bundle bundle, PendingIntent pendingIntent) throws RemoteException;

    void sendStoredMessage(int i, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException;

    void setAutoPersisting(String str, boolean z) throws RemoteException;

    boolean updateStoredMessageStatus(String str, Uri uri, ContentValues contentValues) throws RemoteException;

    public static abstract class Stub extends Binder implements IMms {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IMms";
        static final int TRANSACTION_addMultimediaMessageDraft = 11;
        static final int TRANSACTION_addTextMessageDraft = 10;
        static final int TRANSACTION_archiveStoredConversation = 9;
        static final int TRANSACTION_deleteStoredConversation = 7;
        static final int TRANSACTION_deleteStoredMessage = 6;
        static final int TRANSACTION_downloadMessage = 2;
        static final int TRANSACTION_getAutoPersisting = 14;
        static final int TRANSACTION_getCarrierConfigValues = 3;
        static final int TRANSACTION_importMultimediaMessage = 5;
        static final int TRANSACTION_importTextMessage = 4;
        static final int TRANSACTION_sendMessage = 1;
        static final int TRANSACTION_sendStoredMessage = 12;
        static final int TRANSACTION_setAutoPersisting = 13;
        static final int TRANSACTION_updateStoredMessageStatus = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMms asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMms)) {
                return (IMms) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Uri uriCreateFromParcel;
            Bundle bundleCreateFromParcel;
            PendingIntent pendingIntentCreateFromParcel;
            Uri uriCreateFromParcel2;
            Bundle bundleCreateFromParcel2;
            PendingIntent pendingIntentCreateFromParcel2;
            Uri uriCreateFromParcel3;
            Uri uriCreateFromParcel4;
            Uri uriCreateFromParcel5;
            Bundle bundleCreateFromParcel3;
            PendingIntent pendingIntentCreateFromParcel3;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    String string = parcel.readString();
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    String string2 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        pendingIntentCreateFromParcel = PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntentCreateFromParcel = null;
                    }
                    sendMessage(i3, string, uriCreateFromParcel, string2, bundleCreateFromParcel, pendingIntentCreateFromParcel);
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i4 = parcel.readInt();
                    String string3 = parcel.readString();
                    String string4 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel2 = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel2 = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel2 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel2 = null;
                    }
                    if (parcel.readInt() != 0) {
                        pendingIntentCreateFromParcel2 = PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntentCreateFromParcel2 = null;
                    }
                    downloadMessage(i4, string3, string4, uriCreateFromParcel2, bundleCreateFromParcel2, pendingIntentCreateFromParcel2);
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bundle carrierConfigValues = getCarrierConfigValues(parcel.readInt());
                    parcel2.writeNoException();
                    if (carrierConfigValues != null) {
                        parcel2.writeInt(1);
                        carrierConfigValues.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    Uri uriImportTextMessage = importTextMessage(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readLong(), parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (uriImportTextMessage != null) {
                        parcel2.writeInt(1);
                        uriImportTextMessage.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string5 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel3 = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel3 = null;
                    }
                    Uri uriImportMultimediaMessage = importMultimediaMessage(string5, uriCreateFromParcel3, parcel.readString(), parcel.readLong(), parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (uriImportMultimediaMessage != null) {
                        parcel2.writeInt(1);
                        uriImportMultimediaMessage.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDeleteStoredMessage = deleteStoredMessage(parcel.readString(), parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeleteStoredMessage ? 1 : 0);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDeleteStoredConversation = deleteStoredConversation(parcel.readString(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeleteStoredConversation ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string6 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel4 = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel4 = null;
                    }
                    boolean zUpdateStoredMessageStatus = updateStoredMessageStatus(string6, uriCreateFromParcel4, parcel.readInt() != 0 ? ContentValues.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateStoredMessageStatus ? 1 : 0);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zArchiveStoredConversation = archiveStoredConversation(parcel.readString(), parcel.readLong(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zArchiveStoredConversation ? 1 : 0);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    Uri uriAddTextMessageDraft = addTextMessageDraft(parcel.readString(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    if (uriAddTextMessageDraft != null) {
                        parcel2.writeInt(1);
                        uriAddTextMessageDraft.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    Uri uriAddMultimediaMessageDraft = addMultimediaMessageDraft(parcel.readString(), parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    if (uriAddMultimediaMessageDraft != null) {
                        parcel2.writeInt(1);
                        uriAddMultimediaMessageDraft.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i5 = parcel.readInt();
                    String string7 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel5 = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel5 = null;
                    }
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel3 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel3 = null;
                    }
                    if (parcel.readInt() != 0) {
                        pendingIntentCreateFromParcel3 = PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntentCreateFromParcel3 = null;
                    }
                    sendStoredMessage(i5, string7, uriCreateFromParcel5, bundleCreateFromParcel3, pendingIntentCreateFromParcel3);
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAutoPersisting(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean autoPersisting = getAutoPersisting();
                    parcel2.writeNoException();
                    parcel2.writeInt(autoPersisting ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMms {
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
            public void sendMessage(int i, String str, Uri uri, String str2, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void downloadMessage(int i, String str, String str2, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
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
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getCarrierConfigValues(int i) throws RemoteException {
                Bundle bundleCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    return bundleCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Uri importTextMessage(String str, String str2, int i, String str3, long j, boolean z, boolean z2) throws RemoteException {
                Uri uriCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    return uriCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Uri importMultimediaMessage(String str, Uri uri, String str2, long j, boolean z, boolean z2) throws RemoteException {
                Uri uriCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    return uriCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean deleteStoredMessage(String str, Uri uri) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    boolean z = true;
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean deleteStoredConversation(String str, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean updateStoredMessageStatus(String str, Uri uri, ContentValues contentValues) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    boolean z = true;
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (contentValues != null) {
                        parcelObtain.writeInt(1);
                        contentValues.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean archiveStoredConversation(String str, long j, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Uri addTextMessageDraft(String str, String str2, String str3) throws RemoteException {
                Uri uriCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    return uriCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Uri addMultimediaMessageDraft(String str, Uri uri) throws RemoteException {
                Uri uriCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    return uriCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendStoredMessage(int i, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
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
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAutoPersisting(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getAutoPersisting() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
