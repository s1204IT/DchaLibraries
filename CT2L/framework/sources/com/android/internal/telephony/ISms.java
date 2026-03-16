package com.android.internal.telephony;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface ISms extends IInterface {
    boolean copyMessageToIccEf(String str, int i, byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean copyMessageToIccEfForSubscriber(int i, String str, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean disableCellBroadcast(int i, int i2) throws RemoteException;

    boolean disableCellBroadcastForSubscriber(int i, int i2, int i3) throws RemoteException;

    boolean disableCellBroadcastRange(int i, int i2, int i3) throws RemoteException;

    boolean disableCellBroadcastRangeForSubscriber(int i, int i2, int i3, int i4) throws RemoteException;

    boolean enableCellBroadcast(int i, int i2) throws RemoteException;

    boolean enableCellBroadcastForSubscriber(int i, int i2, int i3) throws RemoteException;

    boolean enableCellBroadcastRange(int i, int i2, int i3) throws RemoteException;

    boolean enableCellBroadcastRangeForSubscriber(int i, int i2, int i3, int i4) throws RemoteException;

    List<SmsRawData> getAllMessagesFromIccEf(String str) throws RemoteException;

    List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int i, String str) throws RemoteException;

    String getImsSmsFormat() throws RemoteException;

    String getImsSmsFormatForSubscriber(int i) throws RemoteException;

    int getPreferredSmsSubscription() throws RemoteException;

    int getPremiumSmsPermission(String str) throws RemoteException;

    int getPremiumSmsPermissionForSubscriber(int i, String str) throws RemoteException;

    void injectSmsPdu(byte[] bArr, String str, PendingIntent pendingIntent) throws RemoteException;

    boolean isImsSmsSupported() throws RemoteException;

    boolean isImsSmsSupportedForSubscriber(int i) throws RemoteException;

    boolean isSMSPromptEnabled() throws RemoteException;

    boolean isSmsSimPickActivityNeeded(int i) throws RemoteException;

    void sendData(String str, String str2, String str3, int i, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendDataForSubscriber(int i, String str, String str2, String str3, int i2, byte[] bArr, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendMultipartText(String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3) throws RemoteException;

    void sendMultipartTextForSubscriber(int i, String str, String str2, String str3, List<String> list, List<PendingIntent> list2, List<PendingIntent> list3) throws RemoteException;

    void sendStoredMultipartText(int i, String str, Uri uri, String str2, List<PendingIntent> list, List<PendingIntent> list2) throws RemoteException;

    void sendStoredText(int i, String str, Uri uri, String str2, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendText(String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void sendTextForSubscriber(int i, String str, String str2, String str3, String str4, PendingIntent pendingIntent, PendingIntent pendingIntent2) throws RemoteException;

    void setPremiumSmsPermission(String str, int i) throws RemoteException;

    void setPremiumSmsPermissionForSubscriber(int i, String str, int i2) throws RemoteException;

    boolean updateMessageOnIccEf(String str, int i, int i2, byte[] bArr) throws RemoteException;

    boolean updateMessageOnIccEfForSubscriber(int i, String str, int i2, int i3, byte[] bArr) throws RemoteException;

    public static abstract class Stub extends Binder implements ISms {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ISms";
        static final int TRANSACTION_copyMessageToIccEf = 5;
        static final int TRANSACTION_copyMessageToIccEfForSubscriber = 6;
        static final int TRANSACTION_disableCellBroadcast = 16;
        static final int TRANSACTION_disableCellBroadcastForSubscriber = 17;
        static final int TRANSACTION_disableCellBroadcastRange = 20;
        static final int TRANSACTION_disableCellBroadcastRangeForSubscriber = 21;
        static final int TRANSACTION_enableCellBroadcast = 14;
        static final int TRANSACTION_enableCellBroadcastForSubscriber = 15;
        static final int TRANSACTION_enableCellBroadcastRange = 18;
        static final int TRANSACTION_enableCellBroadcastRangeForSubscriber = 19;
        static final int TRANSACTION_getAllMessagesFromIccEf = 1;
        static final int TRANSACTION_getAllMessagesFromIccEfForSubscriber = 2;
        static final int TRANSACTION_getImsSmsFormat = 30;
        static final int TRANSACTION_getImsSmsFormatForSubscriber = 31;
        static final int TRANSACTION_getPreferredSmsSubscription = 29;
        static final int TRANSACTION_getPremiumSmsPermission = 22;
        static final int TRANSACTION_getPremiumSmsPermissionForSubscriber = 23;
        static final int TRANSACTION_injectSmsPdu = 11;
        static final int TRANSACTION_isImsSmsSupported = 26;
        static final int TRANSACTION_isImsSmsSupportedForSubscriber = 27;
        static final int TRANSACTION_isSMSPromptEnabled = 32;
        static final int TRANSACTION_isSmsSimPickActivityNeeded = 28;
        static final int TRANSACTION_sendData = 7;
        static final int TRANSACTION_sendDataForSubscriber = 8;
        static final int TRANSACTION_sendMultipartText = 12;
        static final int TRANSACTION_sendMultipartTextForSubscriber = 13;
        static final int TRANSACTION_sendStoredMultipartText = 34;
        static final int TRANSACTION_sendStoredText = 33;
        static final int TRANSACTION_sendText = 9;
        static final int TRANSACTION_sendTextForSubscriber = 10;
        static final int TRANSACTION_setPremiumSmsPermission = 24;
        static final int TRANSACTION_setPremiumSmsPermissionForSubscriber = 25;
        static final int TRANSACTION_updateMessageOnIccEf = 3;
        static final int TRANSACTION_updateMessageOnIccEfForSubscriber = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISms asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISms)) {
                return (ISms) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Uri _arg2;
            Uri _arg22;
            PendingIntent _arg4;
            PendingIntent _arg5;
            PendingIntent _arg23;
            PendingIntent _arg52;
            PendingIntent _arg6;
            PendingIntent _arg42;
            PendingIntent _arg53;
            PendingIntent _arg62;
            PendingIntent _arg7;
            PendingIntent _arg54;
            PendingIntent _arg63;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0 = data.readString();
                    List<SmsRawData> _result = getAllMessagesFromIccEf(_arg0);
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    String _arg1 = data.readString();
                    List<SmsRawData> _result2 = getAllMessagesFromIccEfForSubscriber(_arg02, _arg1);
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    int _arg12 = data.readInt();
                    int _arg24 = data.readInt();
                    byte[] _arg3 = data.createByteArray();
                    boolean _result3 = updateMessageOnIccEf(_arg03, _arg12, _arg24, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    String _arg13 = data.readString();
                    int _arg25 = data.readInt();
                    int _arg32 = data.readInt();
                    byte[] _arg43 = data.createByteArray();
                    boolean _result4 = updateMessageOnIccEfForSubscriber(_arg04, _arg13, _arg25, _arg32, _arg43);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg14 = data.readInt();
                    byte[] _arg26 = data.createByteArray();
                    byte[] _arg33 = data.createByteArray();
                    boolean _result5 = copyMessageToIccEf(_arg05, _arg14, _arg26, _arg33);
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    String _arg15 = data.readString();
                    int _arg27 = data.readInt();
                    byte[] _arg34 = data.createByteArray();
                    byte[] _arg44 = data.createByteArray();
                    boolean _result6 = copyMessageToIccEfForSubscriber(_arg06, _arg15, _arg27, _arg34, _arg44);
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    String _arg16 = data.readString();
                    String _arg28 = data.readString();
                    int _arg35 = data.readInt();
                    byte[] _arg45 = data.createByteArray();
                    if (data.readInt() != 0) {
                        _arg54 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg54 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg63 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg63 = null;
                    }
                    sendData(_arg07, _arg16, _arg28, _arg35, _arg45, _arg54, _arg63);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    String _arg17 = data.readString();
                    String _arg29 = data.readString();
                    String _arg36 = data.readString();
                    int _arg46 = data.readInt();
                    byte[] _arg55 = data.createByteArray();
                    if (data.readInt() != 0) {
                        _arg62 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg62 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg7 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg7 = null;
                    }
                    sendDataForSubscriber(_arg08, _arg17, _arg29, _arg36, _arg46, _arg55, _arg62, _arg7);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    String _arg18 = data.readString();
                    String _arg210 = data.readString();
                    String _arg37 = data.readString();
                    if (data.readInt() != 0) {
                        _arg42 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg42 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg53 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg53 = null;
                    }
                    sendText(_arg09, _arg18, _arg210, _arg37, _arg42, _arg53);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    String _arg19 = data.readString();
                    String _arg211 = data.readString();
                    String _arg38 = data.readString();
                    String _arg47 = data.readString();
                    if (data.readInt() != 0) {
                        _arg52 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg52 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg6 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg6 = null;
                    }
                    sendTextForSubscriber(_arg010, _arg19, _arg211, _arg38, _arg47, _arg52, _arg6);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg011 = data.createByteArray();
                    String _arg110 = data.readString();
                    if (data.readInt() != 0) {
                        _arg23 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    injectSmsPdu(_arg011, _arg110, _arg23);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    String _arg111 = data.readString();
                    String _arg212 = data.readString();
                    List<String> _arg39 = data.createStringArrayList();
                    List<PendingIntent> _arg48 = data.createTypedArrayList(PendingIntent.CREATOR);
                    List<PendingIntent> _arg56 = data.createTypedArrayList(PendingIntent.CREATOR);
                    sendMultipartText(_arg012, _arg111, _arg212, _arg39, _arg48, _arg56);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    String _arg112 = data.readString();
                    String _arg213 = data.readString();
                    String _arg310 = data.readString();
                    List<String> _arg49 = data.createStringArrayList();
                    List<PendingIntent> _arg57 = data.createTypedArrayList(PendingIntent.CREATOR);
                    List<PendingIntent> _arg64 = data.createTypedArrayList(PendingIntent.CREATOR);
                    sendMultipartTextForSubscriber(_arg013, _arg112, _arg213, _arg310, _arg49, _arg57, _arg64);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg014 = data.readInt();
                    int _arg113 = data.readInt();
                    boolean _result7 = enableCellBroadcast(_arg014, _arg113);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    int _arg114 = data.readInt();
                    int _arg214 = data.readInt();
                    boolean _result8 = enableCellBroadcastForSubscriber(_arg015, _arg114, _arg214);
                    reply.writeNoException();
                    reply.writeInt(_result8 ? 1 : 0);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    int _arg115 = data.readInt();
                    boolean _result9 = disableCellBroadcast(_arg016, _arg115);
                    reply.writeNoException();
                    reply.writeInt(_result9 ? 1 : 0);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _arg116 = data.readInt();
                    int _arg215 = data.readInt();
                    boolean _result10 = disableCellBroadcastForSubscriber(_arg017, _arg116, _arg215);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    int _arg117 = data.readInt();
                    int _arg216 = data.readInt();
                    boolean _result11 = enableCellBroadcastRange(_arg018, _arg117, _arg216);
                    reply.writeNoException();
                    reply.writeInt(_result11 ? 1 : 0);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    int _arg118 = data.readInt();
                    int _arg217 = data.readInt();
                    int _arg311 = data.readInt();
                    boolean _result12 = enableCellBroadcastRangeForSubscriber(_arg019, _arg118, _arg217, _arg311);
                    reply.writeNoException();
                    reply.writeInt(_result12 ? 1 : 0);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    int _arg119 = data.readInt();
                    int _arg218 = data.readInt();
                    boolean _result13 = disableCellBroadcastRange(_arg020, _arg119, _arg218);
                    reply.writeNoException();
                    reply.writeInt(_result13 ? 1 : 0);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    int _arg120 = data.readInt();
                    int _arg219 = data.readInt();
                    int _arg312 = data.readInt();
                    boolean _result14 = disableCellBroadcastRangeForSubscriber(_arg021, _arg120, _arg219, _arg312);
                    reply.writeNoException();
                    reply.writeInt(_result14 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    int _result15 = getPremiumSmsPermission(_arg022);
                    reply.writeNoException();
                    reply.writeInt(_result15);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg023 = data.readInt();
                    String _arg121 = data.readString();
                    int _result16 = getPremiumSmsPermissionForSubscriber(_arg023, _arg121);
                    reply.writeNoException();
                    reply.writeInt(_result16);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg024 = data.readString();
                    int _arg122 = data.readInt();
                    setPremiumSmsPermission(_arg024, _arg122);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg025 = data.readInt();
                    String _arg123 = data.readString();
                    int _arg220 = data.readInt();
                    setPremiumSmsPermissionForSubscriber(_arg025, _arg123, _arg220);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result17 = isImsSmsSupported();
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg026 = data.readInt();
                    boolean _result18 = isImsSmsSupportedForSubscriber(_arg026);
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg027 = data.readInt();
                    boolean _result19 = isSmsSimPickActivityNeeded(_arg027);
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    int _result20 = getPreferredSmsSubscription();
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String _result21 = getImsSmsFormat();
                    reply.writeNoException();
                    reply.writeString(_result21);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg028 = data.readInt();
                    String _result22 = getImsSmsFormatForSubscriber(_arg028);
                    reply.writeNoException();
                    reply.writeString(_result22);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result23 = isSMSPromptEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    String _arg124 = data.readString();
                    if (data.readInt() != 0) {
                        _arg22 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    String _arg313 = data.readString();
                    if (data.readInt() != 0) {
                        _arg4 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg5 = PendingIntent.CREATOR.createFromParcel(data);
                    } else {
                        _arg5 = null;
                    }
                    sendStoredText(_arg029, _arg124, _arg22, _arg313, _arg4, _arg5);
                    reply.writeNoException();
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg030 = data.readInt();
                    String _arg125 = data.readString();
                    if (data.readInt() != 0) {
                        _arg2 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    String _arg314 = data.readString();
                    List<PendingIntent> _arg410 = data.createTypedArrayList(PendingIntent.CREATOR);
                    List<PendingIntent> _arg58 = data.createTypedArrayList(PendingIntent.CREATOR);
                    sendStoredMultipartText(_arg030, _arg125, _arg2, _arg314, _arg410, _arg58);
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ISms {
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
            public List<SmsRawData> getAllMessagesFromIccEf(String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<SmsRawData> _result = _reply.createTypedArrayList(SmsRawData.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<SmsRawData> getAllMessagesFromIccEfForSubscriber(int subId, String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<SmsRawData> _result = _reply.createTypedArrayList(SmsRawData.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateMessageOnIccEf(String callingPkg, int messageIndex, int newStatus, byte[] pdu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeInt(messageIndex);
                    _data.writeInt(newStatus);
                    _data.writeByteArray(pdu);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateMessageOnIccEfForSubscriber(int subId, String callingPkg, int messageIndex, int newStatus, byte[] pdu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeInt(messageIndex);
                    _data.writeInt(newStatus);
                    _data.writeByteArray(pdu);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean copyMessageToIccEf(String callingPkg, int status, byte[] pdu, byte[] smsc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeInt(status);
                    _data.writeByteArray(pdu);
                    _data.writeByteArray(smsc);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean copyMessageToIccEfForSubscriber(int subId, String callingPkg, int status, byte[] pdu, byte[] smsc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeInt(status);
                    _data.writeByteArray(pdu);
                    _data.writeByteArray(smsc);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendData(String callingPkg, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeInt(destPort);
                    _data.writeByteArray(data);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendDataForSubscriber(int subId, String callingPkg, String destAddr, String scAddr, int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeInt(destPort);
                    _data.writeByteArray(data);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendText(String callingPkg, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeString(text);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendTextForSubscriber(int subId, String callingPkg, String destAddr, String scAddr, String text, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destAddr);
                    _data.writeString(scAddr);
                    _data.writeString(text);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void injectSmsPdu(byte[] pdu, String format, PendingIntent receivedIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(pdu);
                    _data.writeString(format);
                    if (receivedIntent != null) {
                        _data.writeInt(1);
                        receivedIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendMultipartText(String callingPkg, String destinationAddress, String scAddress, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(destinationAddress);
                    _data.writeString(scAddress);
                    _data.writeStringList(parts);
                    _data.writeTypedList(sentIntents);
                    _data.writeTypedList(deliveryIntents);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendMultipartTextForSubscriber(int subId, String callingPkg, String destinationAddress, String scAddress, List<String> parts, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(destinationAddress);
                    _data.writeString(scAddress);
                    _data.writeStringList(parts);
                    _data.writeTypedList(sentIntents);
                    _data.writeTypedList(deliveryIntents);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean enableCellBroadcast(int messageIdentifier, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(messageIdentifier);
                    _data.writeInt(ranType);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean enableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(messageIdentifier);
                    _data.writeInt(ranType);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean disableCellBroadcast(int messageIdentifier, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(messageIdentifier);
                    _data.writeInt(ranType);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean disableCellBroadcastForSubscriber(int subId, int messageIdentifier, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(messageIdentifier);
                    _data.writeInt(ranType);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean enableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startMessageId);
                    _data.writeInt(endMessageId);
                    _data.writeInt(ranType);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean enableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(startMessageId);
                    _data.writeInt(endMessageId);
                    _data.writeInt(ranType);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean disableCellBroadcastRange(int startMessageId, int endMessageId, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startMessageId);
                    _data.writeInt(endMessageId);
                    _data.writeInt(ranType);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean disableCellBroadcastRangeForSubscriber(int subId, int startMessageId, int endMessageId, int ranType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(startMessageId);
                    _data.writeInt(endMessageId);
                    _data.writeInt(ranType);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPremiumSmsPermission(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPremiumSmsPermissionForSubscriber(int subId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(packageName);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPremiumSmsPermission(String packageName, int permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(permission);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPremiumSmsPermissionForSubscriber(int subId, String packageName, int permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(packageName);
                    _data.writeInt(permission);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isImsSmsSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isImsSmsSupportedForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSmsSimPickActivityNeeded(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPreferredSmsSubscription() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getImsSmsFormat() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getImsSmsFormatForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSMSPromptEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendStoredText(int subId, String callingPkg, Uri messageUri, String scAddress, PendingIntent sentIntent, PendingIntent deliveryIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(scAddress);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deliveryIntent != null) {
                        _data.writeInt(1);
                        deliveryIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendStoredMultipartText(int subId, String callingPkg, Uri messageUri, String scAddress, List<PendingIntent> sentIntents, List<PendingIntent> deliveryIntents) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(scAddress);
                    _data.writeTypedList(sentIntents);
                    _data.writeTypedList(deliveryIntents);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
