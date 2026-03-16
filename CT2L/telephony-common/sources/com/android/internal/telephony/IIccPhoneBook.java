package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.telephony.uicc.AdnRecord;
import java.util.List;

public interface IIccPhoneBook extends IInterface {
    List<AdnRecord> getAdnRecordsInEf(int i) throws RemoteException;

    List<AdnRecord> getAdnRecordsInEfForSubscriber(int i, int i2) throws RemoteException;

    int[] getAdnRecordsSize(int i) throws RemoteException;

    int[] getAdnRecordsSizeForSubscriber(int i, int i2) throws RemoteException;

    int[] getContactItemMaxLength() throws RemoteException;

    int[] getContactItemMaxLengthUsingSubId(int i) throws RemoteException;

    int getEmailFieldSize() throws RemoteException;

    int getEmailFieldSizeUsingSubId(int i) throws RemoteException;

    String[] getGrpRecords(int i) throws RemoteException;

    String[] getGrpRecordsUsingSubId(int i, int i2) throws RemoteException;

    int getPBInitCount() throws RemoteException;

    int getPBInitCountUsingSubId(int i) throws RemoteException;

    int getTotalAdnRecordsSize() throws RemoteException;

    int getTotalAdnRecordsSizeUsingSubId(int i) throws RemoteException;

    boolean isSimContactsLoaded() throws RemoteException;

    boolean isSimContactsLoadedUsingSubId(int i) throws RemoteException;

    boolean isSneFieldEnable() throws RemoteException;

    boolean isSneFieldEnableUsingSubId(int i) throws RemoteException;

    boolean updateAdnRecordsGrpInEfBySearch(int i, String str, String str2, String str3, String str4, byte[] bArr, byte b, String str5) throws RemoteException;

    boolean updateAdnRecordsGrpInEfBySearchUsingSubId(int i, int i2, String str, String str2, String str3, String str4, byte[] bArr, byte b, String str5) throws RemoteException;

    boolean updateAdnRecordsGrpTagInEfByIndex(int i, String str, String str2, String str3) throws RemoteException;

    boolean updateAdnRecordsGrpTagInEfByIndexUsingSubId(int i, int i2, String str, String str2, String str3) throws RemoteException;

    boolean updateAdnRecordsInEfByIndex(int i, String str, String str2, int i2, String str3) throws RemoteException;

    boolean updateAdnRecordsInEfByIndexForSubscriber(int i, int i2, String str, String str2, int i3, String str3) throws RemoteException;

    boolean updateAdnRecordsInEfBySearch(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    boolean updateAdnRecordsInEfBySearch2(int i, String str, String str2, String[] strArr, String str3, String str4, byte[] bArr, String str5, String str6, String[] strArr2, String str7, String str8, byte[] bArr2, String str9) throws RemoteException;

    boolean updateAdnRecordsInEfBySearch2UsingSubId(int i, int i2, String str, String str2, String[] strArr, String str3, String str4, byte[] bArr, String str5, String str6, String[] strArr2, String str7, String str8, byte[] bArr2, String str9) throws RemoteException;

    boolean updateAdnRecordsInEfBySearchForSubscriber(int i, int i2, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    public static abstract class Stub extends Binder implements IIccPhoneBook {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IIccPhoneBook";
        static final int TRANSACTION_getAdnRecordsInEf = 1;
        static final int TRANSACTION_getAdnRecordsInEfForSubscriber = 2;
        static final int TRANSACTION_getAdnRecordsSize = 15;
        static final int TRANSACTION_getAdnRecordsSizeForSubscriber = 16;
        static final int TRANSACTION_getContactItemMaxLength = 27;
        static final int TRANSACTION_getContactItemMaxLengthUsingSubId = 28;
        static final int TRANSACTION_getEmailFieldSize = 18;
        static final int TRANSACTION_getEmailFieldSizeUsingSubId = 21;
        static final int TRANSACTION_getGrpRecords = 11;
        static final int TRANSACTION_getGrpRecordsUsingSubId = 13;
        static final int TRANSACTION_getPBInitCount = 17;
        static final int TRANSACTION_getPBInitCountUsingSubId = 20;
        static final int TRANSACTION_getTotalAdnRecordsSize = 25;
        static final int TRANSACTION_getTotalAdnRecordsSizeUsingSubId = 26;
        static final int TRANSACTION_isSimContactsLoaded = 23;
        static final int TRANSACTION_isSimContactsLoadedUsingSubId = 24;
        static final int TRANSACTION_isSneFieldEnable = 19;
        static final int TRANSACTION_isSneFieldEnableUsingSubId = 22;
        static final int TRANSACTION_updateAdnRecordsGrpInEfBySearch = 6;
        static final int TRANSACTION_updateAdnRecordsGrpInEfBySearchUsingSubId = 8;
        static final int TRANSACTION_updateAdnRecordsGrpTagInEfByIndex = 10;
        static final int TRANSACTION_updateAdnRecordsGrpTagInEfByIndexUsingSubId = 12;
        static final int TRANSACTION_updateAdnRecordsInEfByIndex = 9;
        static final int TRANSACTION_updateAdnRecordsInEfByIndexForSubscriber = 14;
        static final int TRANSACTION_updateAdnRecordsInEfBySearch = 3;
        static final int TRANSACTION_updateAdnRecordsInEfBySearch2 = 5;
        static final int TRANSACTION_updateAdnRecordsInEfBySearch2UsingSubId = 7;
        static final int TRANSACTION_updateAdnRecordsInEfBySearchForSubscriber = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIccPhoneBook asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIccPhoneBook)) {
                return (IIccPhoneBook) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    List<AdnRecord> _result = getAdnRecordsInEf(_arg0);
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg02 = data.readInt();
                    int _arg1 = data.readInt();
                    List<AdnRecord> _result2 = getAdnRecordsInEfForSubscriber(_arg02, _arg1);
                    reply.writeNoException();
                    reply.writeTypedList(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg03 = data.readInt();
                    String _arg12 = data.readString();
                    String _arg2 = data.readString();
                    String _arg3 = data.readString();
                    String _arg4 = data.readString();
                    String _arg5 = data.readString();
                    boolean _result3 = updateAdnRecordsInEfBySearch(_arg03, _arg12, _arg2, _arg3, _arg4, _arg5);
                    reply.writeNoException();
                    reply.writeInt(_result3 ? 1 : 0);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg04 = data.readInt();
                    int _arg13 = data.readInt();
                    String _arg22 = data.readString();
                    String _arg32 = data.readString();
                    String _arg42 = data.readString();
                    String _arg52 = data.readString();
                    boolean _result4 = updateAdnRecordsInEfBySearchForSubscriber(_arg04, _arg13, _arg22, _arg32, _arg42, _arg52, data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    String _arg14 = data.readString();
                    String _arg23 = data.readString();
                    String[] _arg33 = data.createStringArray();
                    String _arg43 = data.readString();
                    String _arg53 = data.readString();
                    byte[] _arg6 = data.createByteArray();
                    String _arg7 = data.readString();
                    String _arg8 = data.readString();
                    String[] _arg9 = data.createStringArray();
                    String _arg10 = data.readString();
                    String _arg11 = data.readString();
                    byte[] _arg122 = data.createByteArray();
                    String _arg132 = data.readString();
                    boolean _result5 = updateAdnRecordsInEfBySearch2(_arg05, _arg14, _arg23, _arg33, _arg43, _arg53, _arg6, _arg7, _arg8, _arg9, _arg10, _arg11, _arg122, _arg132);
                    reply.writeNoException();
                    reply.writeInt(_result5 ? 1 : 0);
                    reply.writeStringArray(_arg33);
                    reply.writeStringArray(_arg9);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    String _arg15 = data.readString();
                    String _arg24 = data.readString();
                    String _arg34 = data.readString();
                    String _arg44 = data.readString();
                    byte[] _arg54 = data.createByteArray();
                    byte _arg62 = data.readByte();
                    String _arg72 = data.readString();
                    boolean _result6 = updateAdnRecordsGrpInEfBySearch(_arg06, _arg15, _arg24, _arg34, _arg44, _arg54, _arg62, _arg72);
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    reply.writeByteArray(_arg54);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    int _arg16 = data.readInt();
                    String _arg25 = data.readString();
                    String _arg35 = data.readString();
                    String[] _arg45 = data.createStringArray();
                    String _arg55 = data.readString();
                    String _arg63 = data.readString();
                    byte[] _arg73 = data.createByteArray();
                    String _arg82 = data.readString();
                    String _arg92 = data.readString();
                    String[] _arg102 = data.createStringArray();
                    String _arg112 = data.readString();
                    String _arg123 = data.readString();
                    byte[] _arg133 = data.createByteArray();
                    String _arg142 = data.readString();
                    boolean _result7 = updateAdnRecordsInEfBySearch2UsingSubId(_arg07, _arg16, _arg25, _arg35, _arg45, _arg55, _arg63, _arg73, _arg82, _arg92, _arg102, _arg112, _arg123, _arg133, _arg142);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    reply.writeStringArray(_arg45);
                    reply.writeStringArray(_arg102);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    int _arg17 = data.readInt();
                    String _arg26 = data.readString();
                    String _arg36 = data.readString();
                    String _arg46 = data.readString();
                    String _arg56 = data.readString();
                    byte[] _arg64 = data.createByteArray();
                    byte _arg74 = data.readByte();
                    String _arg83 = data.readString();
                    boolean _result8 = updateAdnRecordsGrpInEfBySearchUsingSubId(_arg08, _arg17, _arg26, _arg36, _arg46, _arg56, _arg64, _arg74, _arg83);
                    reply.writeNoException();
                    reply.writeInt(_result8 ? 1 : 0);
                    reply.writeByteArray(_arg64);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    String _arg18 = data.readString();
                    String _arg27 = data.readString();
                    int _arg37 = data.readInt();
                    String _arg47 = data.readString();
                    boolean _result9 = updateAdnRecordsInEfByIndex(_arg09, _arg18, _arg27, _arg37, _arg47);
                    reply.writeNoException();
                    reply.writeInt(_result9 ? 1 : 0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    String _arg19 = data.readString();
                    String _arg28 = data.readString();
                    String _arg38 = data.readString();
                    boolean _result10 = updateAdnRecordsGrpTagInEfByIndex(_arg010, _arg19, _arg28, _arg38);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    String[] _result11 = getGrpRecords(_arg011);
                    reply.writeNoException();
                    reply.writeStringArray(_result11);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    int _arg110 = data.readInt();
                    String _arg29 = data.readString();
                    String _arg39 = data.readString();
                    String _arg48 = data.readString();
                    boolean _result12 = updateAdnRecordsGrpTagInEfByIndexUsingSubId(_arg012, _arg110, _arg29, _arg39, _arg48);
                    reply.writeNoException();
                    reply.writeInt(_result12 ? 1 : 0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    int _arg111 = data.readInt();
                    String[] _result13 = getGrpRecordsUsingSubId(_arg013, _arg111);
                    reply.writeNoException();
                    reply.writeStringArray(_result13);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg014 = data.readInt();
                    int _arg113 = data.readInt();
                    String _arg210 = data.readString();
                    String _arg310 = data.readString();
                    int _arg49 = data.readInt();
                    String _arg57 = data.readString();
                    boolean _result14 = updateAdnRecordsInEfByIndexForSubscriber(_arg014, _arg113, _arg210, _arg310, _arg49, _arg57);
                    reply.writeNoException();
                    reply.writeInt(_result14 ? 1 : 0);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    int[] _result15 = getAdnRecordsSize(_arg015);
                    reply.writeNoException();
                    reply.writeIntArray(_result15);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    int _arg114 = data.readInt();
                    int[] _result16 = getAdnRecordsSizeForSubscriber(_arg016, _arg114);
                    reply.writeNoException();
                    reply.writeIntArray(_result16);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _result17 = getPBInitCount();
                    reply.writeNoException();
                    reply.writeInt(_result17);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _result18 = getEmailFieldSize();
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result19 = isSneFieldEnable();
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _result20 = getPBInitCountUsingSubId(_arg017);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    int _result21 = getEmailFieldSizeUsingSubId(_arg018);
                    reply.writeNoException();
                    reply.writeInt(_result21);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    boolean _result22 = isSneFieldEnableUsingSubId(_arg019);
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result23 = isSimContactsLoaded();
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    boolean _result24 = isSimContactsLoadedUsingSubId(_arg020);
                    reply.writeNoException();
                    reply.writeInt(_result24 ? 1 : 0);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    int _result25 = getTotalAdnRecordsSize();
                    reply.writeNoException();
                    reply.writeInt(_result25);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    int _result26 = getTotalAdnRecordsSizeUsingSubId(_arg021);
                    reply.writeNoException();
                    reply.writeInt(_result26);
                    return true;
                case TRANSACTION_getContactItemMaxLength:
                    data.enforceInterface(DESCRIPTOR);
                    int[] _result27 = getContactItemMaxLength();
                    reply.writeNoException();
                    reply.writeIntArray(_result27);
                    return true;
                case TRANSACTION_getContactItemMaxLengthUsingSubId:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg022 = data.readInt();
                    int[] _result28 = getContactItemMaxLengthUsingSubId(_arg022);
                    reply.writeNoException();
                    reply.writeIntArray(_result28);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IIccPhoneBook {
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
            public List<AdnRecord> getAdnRecordsInEf(int efid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<AdnRecord> _result = _reply.createTypedArrayList(AdnRecord.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<AdnRecord> getAdnRecordsInEfForSubscriber(int subId, int efid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<AdnRecord> _result = _reply.createTypedArrayList(AdnRecord.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    _data.writeString(oldTag);
                    _data.writeString(oldPhoneNumber);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeString(pin2);
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
            public boolean updateAdnRecordsInEfBySearchForSubscriber(int subId, int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    _data.writeString(oldTag);
                    _data.writeString(oldPhoneNumber);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeString(pin2);
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
            public boolean updateAdnRecordsInEfBySearch2(int efid, String oldTag, String oldPhoneNumber, String[] oldEmails, String oldPhoneNumber2, String oldSne, byte[] oldGrps, String newTag, String newPhoneNumber, String[] newEmails, String newPhoneNumber2, String newSne, byte[] newGrps, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    _data.writeString(oldTag);
                    _data.writeString(oldPhoneNumber);
                    _data.writeStringArray(oldEmails);
                    _data.writeString(oldPhoneNumber2);
                    _data.writeString(oldSne);
                    _data.writeByteArray(oldGrps);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeStringArray(newEmails);
                    _data.writeString(newPhoneNumber2);
                    _data.writeString(newSne);
                    _data.writeByteArray(newGrps);
                    _data.writeString(pin2);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.readStringArray(oldEmails);
                    _reply.readStringArray(newEmails);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsGrpInEfBySearch(int efType, String name, String number, String email, String number2, byte[] grps, byte addgrp, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efType);
                    _data.writeString(name);
                    _data.writeString(number);
                    _data.writeString(email);
                    _data.writeString(number2);
                    _data.writeByteArray(grps);
                    _data.writeByte(addgrp);
                    _data.writeString(pin2);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.readByteArray(grps);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsInEfBySearch2UsingSubId(int subId, int efid, String oldTag, String oldPhoneNumber, String[] oldEmails, String oldPhoneNumber2, String oldSne, byte[] oldGrps, String newTag, String newPhoneNumber, String[] newEmails, String newPhoneNumber2, String newSne, byte[] newGrps, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    _data.writeString(oldTag);
                    _data.writeString(oldPhoneNumber);
                    _data.writeStringArray(oldEmails);
                    _data.writeString(oldPhoneNumber2);
                    _data.writeString(oldSne);
                    _data.writeByteArray(oldGrps);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeStringArray(newEmails);
                    _data.writeString(newPhoneNumber2);
                    _data.writeString(newSne);
                    _data.writeByteArray(newGrps);
                    _data.writeString(pin2);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.readStringArray(oldEmails);
                    _reply.readStringArray(newEmails);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsGrpInEfBySearchUsingSubId(int subId, int efType, String name, String number, String email, String number2, byte[] grps, byte addgrp, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efType);
                    _data.writeString(name);
                    _data.writeString(number);
                    _data.writeString(email);
                    _data.writeString(number2);
                    _data.writeByteArray(grps);
                    _data.writeByte(addgrp);
                    _data.writeString(pin2);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.readByteArray(grps);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeInt(index);
                    _data.writeString(pin2);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsGrpTagInEfByIndex(int efid, String grpId, String grpTag, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    _data.writeString(grpId);
                    _data.writeString(grpTag);
                    _data.writeString(pin2);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getGrpRecords(int efid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsGrpTagInEfByIndexUsingSubId(int subId, int efid, String grpId, String grpTag, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    _data.writeString(grpId);
                    _data.writeString(grpTag);
                    _data.writeString(pin2);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String[] getGrpRecordsUsingSubId(int subId, int efid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateAdnRecordsInEfByIndexForSubscriber(int subId, int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeInt(index);
                    _data.writeString(pin2);
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
            public int[] getAdnRecordsSize(int efid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(efid);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getAdnRecordsSizeForSubscriber(int subId, int efid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPBInitCount() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getEmailFieldSize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSneFieldEnable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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
            public int getPBInitCountUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getEmailFieldSizeUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSneFieldEnableUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSimContactsLoaded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSimContactsLoadedUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getTotalAdnRecordsSize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getTotalAdnRecordsSizeUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getContactItemMaxLength() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getContactItemMaxLength, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getContactItemMaxLengthUsingSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(Stub.TRANSACTION_getContactItemMaxLengthUsingSubId, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
