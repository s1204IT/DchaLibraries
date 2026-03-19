package com.mediatek.internal.telephony;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.telephony.SmsRawData;
import java.util.List;
import mediatek.telephony.MtkSimSmsInsertStatus;
import mediatek.telephony.MtkSmsParameters;

public interface IMtkSms extends IInterface {
    boolean activateCellBroadcastSmsForSubscriber(int i, boolean z) throws RemoteException;

    int copyTextMessageToIccCardForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, long j) throws RemoteException;

    List<SmsRawData> getAllMessagesFromIccEfByModeForSubscriber(int i, String str, int i2) throws RemoteException;

    String getCellBroadcastLangsForSubscriber(int i) throws RemoteException;

    String getCellBroadcastRangesForSubscriber(int i) throws RemoteException;

    SmsRawData getMessageFromIccEfForSubscriber(int i, String str, int i2) throws RemoteException;

    String getScAddressForSubscriber(int i) throws RemoteException;

    Bundle getScAddressWithErrorCodeForSubscriber(int i) throws RemoteException;

    MtkSmsParameters getSmsParametersForSubscriber(int i, String str) throws RemoteException;

    MtkIccSmsStorageStatus getSmsSimMemoryStatusForSubscriber(int i, String str) throws RemoteException;

    MtkSimSmsInsertStatus insertRawMessageToIccCardForSubscriber(int i, String str, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    MtkSimSmsInsertStatus insertTextMessageToIccCardForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, long j) throws RemoteException;

    boolean isSmsReadyForSubscriber(int i) throws RemoteException;

    boolean queryCellBroadcastSmsActivationForSubscriber(int i) throws RemoteException;

    boolean removeCellBroadcastMsgForSubscriber(int i, int i2, int i3) throws RemoteException;

    void sendDataWithOriginalPortForSubscriber(int i, String str, String str2, String str3, int i2, int i3, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendMultipartTextWithEncodingTypeForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) throws RemoteException;

    void sendMultipartTextWithExtraParamsForSubscriber(int i, String str, String str2, String str3, List<String> list, Bundle bundle, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) throws RemoteException;

    void sendTextWithEncodingTypeForSubscriber(int i, String str, String str2, String str3, String str4, int i2, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) throws RemoteException;

    void sendTextWithExtraParamsForSubscriber(int i, String str, String str2, String str3, String str4, Bundle bundle, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) throws RemoteException;

    boolean setCellBroadcastLangsForSubscriber(int i, String str) throws RemoteException;

    boolean setEtwsConfigForSubscriber(int i, int i2) throws RemoteException;

    boolean setScAddressForSubscriber(int i, String str) throws RemoteException;

    void setSmsMemoryStatusForSubscriber(int i, boolean z) throws RemoteException;

    boolean setSmsParametersForSubscriber(int i, String str, MtkSmsParameters mtkSmsParameters) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkSms {
        private static final String DESCRIPTOR = "com.mediatek.internal.telephony.IMtkSms";
        static final int TRANSACTION_activateCellBroadcastSmsForSubscriber = 17;
        static final int TRANSACTION_copyTextMessageToIccCardForSubscriber = 2;
        static final int TRANSACTION_getAllMessagesFromIccEfByModeForSubscriber = 1;
        static final int TRANSACTION_getCellBroadcastLangsForSubscriber = 22;
        static final int TRANSACTION_getCellBroadcastRangesForSubscriber = 20;
        static final int TRANSACTION_getMessageFromIccEfForSubscriber = 15;
        static final int TRANSACTION_getScAddressForSubscriber = 23;
        static final int TRANSACTION_getScAddressWithErrorCodeForSubscriber = 24;
        static final int TRANSACTION_getSmsParametersForSubscriber = 13;
        static final int TRANSACTION_getSmsSimMemoryStatusForSubscriber = 6;
        static final int TRANSACTION_insertRawMessageToIccCardForSubscriber = 10;
        static final int TRANSACTION_insertTextMessageToIccCardForSubscriber = 9;
        static final int TRANSACTION_isSmsReadyForSubscriber = 4;
        static final int TRANSACTION_queryCellBroadcastSmsActivationForSubscriber = 16;
        static final int TRANSACTION_removeCellBroadcastMsgForSubscriber = 18;
        static final int TRANSACTION_sendDataWithOriginalPortForSubscriber = 3;
        static final int TRANSACTION_sendMultipartTextWithEncodingTypeForSubscriber = 8;
        static final int TRANSACTION_sendMultipartTextWithExtraParamsForSubscriber = 12;
        static final int TRANSACTION_sendTextWithEncodingTypeForSubscriber = 7;
        static final int TRANSACTION_sendTextWithExtraParamsForSubscriber = 11;
        static final int TRANSACTION_setCellBroadcastLangsForSubscriber = 21;
        static final int TRANSACTION_setEtwsConfigForSubscriber = 19;
        static final int TRANSACTION_setScAddressForSubscriber = 25;
        static final int TRANSACTION_setSmsMemoryStatusForSubscriber = 5;
        static final int TRANSACTION_setSmsParametersForSubscriber = 14;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkSms asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkSms)) {
                return (IMtkSms) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            PendingIntent pendingIntent;
            PendingIntent pendingIntent2;
            PendingIntent pendingIntent3;
            Bundle bundle;
            PendingIntent pendingIntent4;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<SmsRawData> allMessagesFromIccEfByModeForSubscriber = getAllMessagesFromIccEfByModeForSubscriber(parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allMessagesFromIccEfByModeForSubscriber);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCopyTextMessageToIccCardForSubscriber = copyTextMessageToIccCardForSubscriber(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.createStringArrayList(), parcel.readInt(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCopyTextMessageToIccCardForSubscriber);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    String string = parcel.readString();
                    String string2 = parcel.readString();
                    String string3 = parcel.readString();
                    int i4 = parcel.readInt();
                    int i5 = parcel.readInt();
                    byte[] bArrCreateByteArray = parcel.createByteArray();
                    if (parcel.readInt() != 0) {
                        pendingIntent = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntent = null;
                    }
                    if (parcel.readInt() != 0) {
                        pendingIntent2 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntent2 = null;
                    }
                    sendDataWithOriginalPortForSubscriber(i3, string, string2, string3, i4, i5, bArrCreateByteArray, pendingIntent, pendingIntent2);
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSmsReadyForSubscriber = isSmsReadyForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSmsReadyForSubscriber ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSmsMemoryStatusForSubscriber(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    MtkIccSmsStorageStatus smsSimMemoryStatusForSubscriber = getSmsSimMemoryStatusForSubscriber(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    if (smsSimMemoryStatusForSubscriber != null) {
                        parcel2.writeInt(1);
                        smsSimMemoryStatusForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i6 = parcel.readInt();
                    String string4 = parcel.readString();
                    String string5 = parcel.readString();
                    String string6 = parcel.readString();
                    String string7 = parcel.readString();
                    int i7 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        pendingIntent3 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntent3 = null;
                    }
                    sendTextWithEncodingTypeForSubscriber(i6, string4, string5, string6, string7, i7, pendingIntent3, parcel.readInt() != 0 ? (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendMultipartTextWithEncodingTypeForSubscriber(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.createStringArrayList(), parcel.readInt(), parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    MtkSimSmsInsertStatus mtkSimSmsInsertStatusInsertTextMessageToIccCardForSubscriber = insertTextMessageToIccCardForSubscriber(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.createStringArrayList(), parcel.readInt(), parcel.readLong());
                    parcel2.writeNoException();
                    if (mtkSimSmsInsertStatusInsertTextMessageToIccCardForSubscriber != null) {
                        parcel2.writeInt(1);
                        mtkSimSmsInsertStatusInsertTextMessageToIccCardForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    MtkSimSmsInsertStatus mtkSimSmsInsertStatusInsertRawMessageToIccCardForSubscriber = insertRawMessageToIccCardForSubscriber(parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    if (mtkSimSmsInsertStatusInsertRawMessageToIccCardForSubscriber != null) {
                        parcel2.writeInt(1);
                        mtkSimSmsInsertStatusInsertRawMessageToIccCardForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i8 = parcel.readInt();
                    String string8 = parcel.readString();
                    String string9 = parcel.readString();
                    String string10 = parcel.readString();
                    String string11 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundle = null;
                    }
                    if (parcel.readInt() != 0) {
                        pendingIntent4 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                    } else {
                        pendingIntent4 = null;
                    }
                    sendTextWithExtraParamsForSubscriber(i8, string8, string9, string10, string11, bundle, pendingIntent4, parcel.readInt() != 0 ? (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendMultipartTextWithExtraParamsForSubscriber(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.createStringArrayList(), parcel.readInt() != 0 ? (Bundle) Bundle.CREATOR.createFromParcel(parcel) : null, parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.createTypedArrayList(PendingIntent.CREATOR), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    MtkSmsParameters smsParametersForSubscriber = getSmsParametersForSubscriber(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    if (smsParametersForSubscriber != null) {
                        parcel2.writeInt(1);
                        smsParametersForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean smsParametersForSubscriber2 = setSmsParametersForSubscriber(parcel.readInt(), parcel.readString(), parcel.readInt() != 0 ? MtkSmsParameters.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(smsParametersForSubscriber2 ? 1 : 0);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    SmsRawData messageFromIccEfForSubscriber = getMessageFromIccEfForSubscriber(parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (messageFromIccEfForSubscriber != null) {
                        parcel2.writeInt(1);
                        messageFromIccEfForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zQueryCellBroadcastSmsActivationForSubscriber = queryCellBroadcastSmsActivationForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zQueryCellBroadcastSmsActivationForSubscriber ? 1 : 0);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zActivateCellBroadcastSmsForSubscriber = activateCellBroadcastSmsForSubscriber(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zActivateCellBroadcastSmsForSubscriber ? 1 : 0);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRemoveCellBroadcastMsgForSubscriber = removeCellBroadcastMsgForSubscriber(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRemoveCellBroadcastMsgForSubscriber ? 1 : 0);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean etwsConfigForSubscriber = setEtwsConfigForSubscriber(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(etwsConfigForSubscriber ? 1 : 0);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    String cellBroadcastRangesForSubscriber = getCellBroadcastRangesForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(cellBroadcastRangesForSubscriber);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean cellBroadcastLangsForSubscriber = setCellBroadcastLangsForSubscriber(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(cellBroadcastLangsForSubscriber ? 1 : 0);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    String cellBroadcastLangsForSubscriber2 = getCellBroadcastLangsForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(cellBroadcastLangsForSubscriber2);
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    String scAddressForSubscriber = getScAddressForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(scAddressForSubscriber);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bundle scAddressWithErrorCodeForSubscriber = getScAddressWithErrorCodeForSubscriber(parcel.readInt());
                    parcel2.writeNoException();
                    if (scAddressWithErrorCodeForSubscriber != null) {
                        parcel2.writeInt(1);
                        scAddressWithErrorCodeForSubscriber.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case TRANSACTION_setScAddressForSubscriber:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean scAddressForSubscriber2 = setScAddressForSubscriber(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(scAddressForSubscriber2 ? 1 : 0);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkSms {
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
            public List<SmsRawData> getAllMessagesFromIccEfByModeForSubscriber(int i, String str, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(SmsRawData.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int copyTextMessageToIccCardForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendDataWithOriginalPortForSubscriber(int i, String str, String str2, String str3, int i2, int i3, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeByteArray(bArr);
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (pendingIntent2 != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isSmsReadyForSubscriber(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSmsMemoryStatusForSubscriber(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public MtkIccSmsStorageStatus getSmsSimMemoryStatusForSubscriber(int i, String str) throws RemoteException {
                MtkIccSmsStorageStatus mtkIccSmsStorageStatusCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        mtkIccSmsStorageStatusCreateFromParcel = MtkIccSmsStorageStatus.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        mtkIccSmsStorageStatusCreateFromParcel = null;
                    }
                    return mtkIccSmsStorageStatusCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendTextWithEncodingTypeForSubscriber(int i, String str, String str2, String str3, String str4, int i2, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeString(str4);
                    parcelObtain.writeInt(i2);
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (pendingIntent2 != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendMultipartTextWithEncodingTypeForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeTypedList(list2);
                    parcelObtain.writeTypedList(list3);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public MtkSimSmsInsertStatus insertTextMessageToIccCardForSubscriber(int i, String str, String str2, String str3, List<String> list, int i2, long j) throws RemoteException {
                MtkSimSmsInsertStatus mtkSimSmsInsertStatusCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        mtkSimSmsInsertStatusCreateFromParcel = MtkSimSmsInsertStatus.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        mtkSimSmsInsertStatusCreateFromParcel = null;
                    }
                    return mtkSimSmsInsertStatusCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public MtkSimSmsInsertStatus insertRawMessageToIccCardForSubscriber(int i, String str, int i2, byte[] bArr, byte[] bArr2) throws RemoteException {
                MtkSimSmsInsertStatus mtkSimSmsInsertStatusCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        mtkSimSmsInsertStatusCreateFromParcel = MtkSimSmsInsertStatus.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        mtkSimSmsInsertStatusCreateFromParcel = null;
                    }
                    return mtkSimSmsInsertStatusCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendTextWithExtraParamsForSubscriber(int i, String str, String str2, String str3, String str4, Bundle bundle, PendingIntent pendingIntent, PendingIntent pendingIntent2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeString(str4);
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
                    if (pendingIntent2 != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendMultipartTextWithExtraParamsForSubscriber(int i, String str, String str2, String str3, List<String> list, Bundle bundle, List<PendingIntent> list2, List<PendingIntent> list3, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeStringList(list);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeTypedList(list2);
                    parcelObtain.writeTypedList(list3);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public MtkSmsParameters getSmsParametersForSubscriber(int i, String str) throws RemoteException {
                MtkSmsParameters mtkSmsParametersCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        mtkSmsParametersCreateFromParcel = MtkSmsParameters.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        mtkSmsParametersCreateFromParcel = null;
                    }
                    return mtkSmsParametersCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setSmsParametersForSubscriber(int i, String str, MtkSmsParameters mtkSmsParameters) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    boolean z = true;
                    if (mtkSmsParameters != null) {
                        parcelObtain.writeInt(1);
                        mtkSmsParameters.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
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
            public SmsRawData getMessageFromIccEfForSubscriber(int i, String str, int i2) throws RemoteException {
                SmsRawData smsRawData;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        smsRawData = (SmsRawData) SmsRawData.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        smsRawData = null;
                    }
                    return smsRawData;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean queryCellBroadcastSmsActivationForSubscriber(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean activateCellBroadcastSmsForSubscriber(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean removeCellBroadcastMsgForSubscriber(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setEtwsConfigForSubscriber(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCellBroadcastRangesForSubscriber(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setCellBroadcastLangsForSubscriber(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCellBroadcastLangsForSubscriber(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getScAddressForSubscriber(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getScAddressWithErrorCodeForSubscriber(int i) throws RemoteException {
                Bundle bundle;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundle = null;
                    }
                    return bundle;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setScAddressForSubscriber(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(Stub.TRANSACTION_setScAddressForSubscriber, parcelObtain, parcelObtain2, 0);
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
