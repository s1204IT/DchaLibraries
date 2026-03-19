package com.android.internal.telephony;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.telephony.uicc.AdnRecord;
import com.mediatek.internal.telephony.uicc.AlphaTag;
import com.mediatek.internal.telephony.uicc.UsimGroup;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.List;

public interface IIccPhoneBook extends IInterface {
    boolean addContactToGroup(int i, int i2, int i3) throws RemoteException;

    List<AdnRecord> getAdnRecordsInEf(int i) throws RemoteException;

    List<AdnRecord> getAdnRecordsInEfForSubscriber(int i, int i2) throws RemoteException;

    int[] getAdnRecordsSize(int i) throws RemoteException;

    int[] getAdnRecordsSizeForSubscriber(int i, int i2) throws RemoteException;

    int getAnrCount(int i) throws RemoteException;

    int getEmailCount(int i) throws RemoteException;

    UsimPBMemInfo[] getPhonebookMemStorageExt(int i) throws RemoteException;

    int getSneRecordLen(int i) throws RemoteException;

    String getUsimAasById(int i, int i2) throws RemoteException;

    List<AlphaTag> getUsimAasList(int i) throws RemoteException;

    int getUsimAasMaxCount(int i) throws RemoteException;

    int getUsimAasMaxNameLen(int i) throws RemoteException;

    String getUsimGroupById(int i, int i2) throws RemoteException;

    List<UsimGroup> getUsimGroups(int i) throws RemoteException;

    int getUsimGrpMaxCount(int i) throws RemoteException;

    int getUsimGrpMaxNameLen(int i) throws RemoteException;

    int hasExistGroup(int i, String str) throws RemoteException;

    boolean hasSne(int i) throws RemoteException;

    int insertUsimAas(int i, String str) throws RemoteException;

    int insertUsimGroup(int i, String str) throws RemoteException;

    boolean isAdnAccessible(int i) throws RemoteException;

    boolean isPhbReady(int i) throws RemoteException;

    boolean moveContactFromGroupsToGroups(int i, int i2, int[] iArr, int[] iArr2) throws RemoteException;

    boolean removeContactFromGroup(int i, int i2, int i3) throws RemoteException;

    boolean removeUsimAasById(int i, int i2, int i3) throws RemoteException;

    boolean removeUsimGroupById(int i, int i2) throws RemoteException;

    boolean updateAdnRecordsInEfByIndex(int i, String str, String str2, int i2, String str3) throws RemoteException;

    boolean updateAdnRecordsInEfByIndexForSubscriber(int i, int i2, String str, String str2, int i3, String str3) throws RemoteException;

    int updateAdnRecordsInEfByIndexWithError(int i, int i2, String str, String str2, int i3, String str3) throws RemoteException;

    boolean updateAdnRecordsInEfBySearch(int i, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    boolean updateAdnRecordsInEfBySearchForSubscriber(int i, int i2, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    int updateAdnRecordsInEfBySearchWithError(int i, int i2, String str, String str2, String str3, String str4, String str5) throws RemoteException;

    boolean updateContactToGroups(int i, int i2, int[] iArr) throws RemoteException;

    boolean updateUsimAas(int i, int i2, int i3, String str) throws RemoteException;

    int updateUsimGroup(int i, int i2, String str) throws RemoteException;

    int updateUsimPBRecordsByIndexWithError(int i, int i2, AdnRecord adnRecord, int i3) throws RemoteException;

    int updateUsimPBRecordsBySearchWithError(int i, int i2, AdnRecord adnRecord, AdnRecord adnRecord2) throws RemoteException;

    int updateUsimPBRecordsInEfByIndexWithError(int i, int i2, String str, String str2, String str3, String str4, String[] strArr, int i3) throws RemoteException;

    int updateUsimPBRecordsInEfBySearchWithError(int i, int i2, String str, String str2, String str3, String str4, String[] strArr, String str5, String str6, String str7, String str8, String[] strArr2) throws RemoteException;

    public static abstract class Stub extends Binder implements IIccPhoneBook {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IIccPhoneBook";
        static final int TRANSACTION_addContactToGroup = 21;
        static final int TRANSACTION_getAdnRecordsInEf = 1;
        static final int TRANSACTION_getAdnRecordsInEfForSubscriber = 2;
        static final int TRANSACTION_getAdnRecordsSize = 13;
        static final int TRANSACTION_getAdnRecordsSizeForSubscriber = 14;
        static final int TRANSACTION_getAnrCount = 31;
        static final int TRANSACTION_getEmailCount = 32;
        static final int TRANSACTION_getPhonebookMemStorageExt = 40;
        static final int TRANSACTION_getSneRecordLen = 38;
        static final int TRANSACTION_getUsimAasById = 29;
        static final int TRANSACTION_getUsimAasList = 28;
        static final int TRANSACTION_getUsimAasMaxCount = 33;
        static final int TRANSACTION_getUsimAasMaxNameLen = 34;
        static final int TRANSACTION_getUsimGroupById = 17;
        static final int TRANSACTION_getUsimGroups = 16;
        static final int TRANSACTION_getUsimGrpMaxCount = 27;
        static final int TRANSACTION_getUsimGrpMaxNameLen = 26;
        static final int TRANSACTION_hasExistGroup = 25;
        static final int TRANSACTION_hasSne = 37;
        static final int TRANSACTION_insertUsimAas = 30;
        static final int TRANSACTION_insertUsimGroup = 19;
        static final int TRANSACTION_isAdnAccessible = 39;
        static final int TRANSACTION_isPhbReady = 15;
        static final int TRANSACTION_moveContactFromGroupsToGroups = 24;
        static final int TRANSACTION_removeContactFromGroup = 22;
        static final int TRANSACTION_removeUsimAasById = 36;
        static final int TRANSACTION_removeUsimGroupById = 18;
        static final int TRANSACTION_updateAdnRecordsInEfByIndex = 7;
        static final int TRANSACTION_updateAdnRecordsInEfByIndexForSubscriber = 8;
        static final int TRANSACTION_updateAdnRecordsInEfByIndexWithError = 9;
        static final int TRANSACTION_updateAdnRecordsInEfBySearch = 3;
        static final int TRANSACTION_updateAdnRecordsInEfBySearchForSubscriber = 4;
        static final int TRANSACTION_updateAdnRecordsInEfBySearchWithError = 5;
        static final int TRANSACTION_updateContactToGroups = 23;
        static final int TRANSACTION_updateUsimAas = 35;
        static final int TRANSACTION_updateUsimGroup = 20;
        static final int TRANSACTION_updateUsimPBRecordsByIndexWithError = 11;
        static final int TRANSACTION_updateUsimPBRecordsBySearchWithError = 12;
        static final int TRANSACTION_updateUsimPBRecordsInEfByIndexWithError = 10;
        static final int TRANSACTION_updateUsimPBRecordsInEfBySearchWithError = 6;

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
            AdnRecord adnRecordCreateFromParcel;
            AdnRecord adnRecordCreateFromParcel2;
            AdnRecord adnRecordCreateFromParcel3;
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
                    String _arg6 = data.readString();
                    boolean _result4 = updateAdnRecordsInEfBySearchForSubscriber(_arg04, _arg13, _arg22, _arg32, _arg42, _arg52, _arg6);
                    reply.writeNoException();
                    reply.writeInt(_result4 ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg05 = data.readInt();
                    int _arg14 = data.readInt();
                    String _arg23 = data.readString();
                    String _arg33 = data.readString();
                    String _arg43 = data.readString();
                    String _arg53 = data.readString();
                    String _arg62 = data.readString();
                    int _result5 = updateAdnRecordsInEfBySearchWithError(_arg05, _arg14, _arg23, _arg33, _arg43, _arg53, _arg62);
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg06 = data.readInt();
                    int _arg15 = data.readInt();
                    String _arg24 = data.readString();
                    String _arg34 = data.readString();
                    String _arg44 = data.readString();
                    String _arg54 = data.readString();
                    String[] _arg63 = data.createStringArray();
                    String _arg7 = data.readString();
                    String _arg8 = data.readString();
                    String _arg9 = data.readString();
                    String _arg10 = data.readString();
                    String[] _arg11 = data.createStringArray();
                    int _result6 = updateUsimPBRecordsInEfBySearchWithError(_arg06, _arg15, _arg24, _arg34, _arg44, _arg54, _arg63, _arg7, _arg8, _arg9, _arg10, _arg11);
                    reply.writeNoException();
                    reply.writeInt(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    String _arg16 = data.readString();
                    String _arg25 = data.readString();
                    int _arg35 = data.readInt();
                    String _arg45 = data.readString();
                    boolean _result7 = updateAdnRecordsInEfByIndex(_arg07, _arg16, _arg25, _arg35, _arg45);
                    reply.writeNoException();
                    reply.writeInt(_result7 ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    int _arg17 = data.readInt();
                    String _arg26 = data.readString();
                    String _arg36 = data.readString();
                    int _arg46 = data.readInt();
                    String _arg55 = data.readString();
                    boolean _result8 = updateAdnRecordsInEfByIndexForSubscriber(_arg08, _arg17, _arg26, _arg36, _arg46, _arg55);
                    reply.writeNoException();
                    reply.writeInt(_result8 ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    int _arg18 = data.readInt();
                    String _arg27 = data.readString();
                    String _arg37 = data.readString();
                    int _arg47 = data.readInt();
                    String _arg56 = data.readString();
                    int _result9 = updateAdnRecordsInEfByIndexWithError(_arg09, _arg18, _arg27, _arg37, _arg47, _arg56);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _arg19 = data.readInt();
                    String _arg28 = data.readString();
                    String _arg38 = data.readString();
                    String _arg48 = data.readString();
                    String _arg57 = data.readString();
                    String[] _arg64 = data.createStringArray();
                    int _arg72 = data.readInt();
                    int _result10 = updateUsimPBRecordsInEfByIndexWithError(_arg010, _arg19, _arg28, _arg38, _arg48, _arg57, _arg64, _arg72);
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    int _arg110 = data.readInt();
                    if (data.readInt() != 0) {
                        adnRecordCreateFromParcel3 = AdnRecord.CREATOR.createFromParcel(data);
                    } else {
                        adnRecordCreateFromParcel3 = null;
                    }
                    int _arg39 = data.readInt();
                    int _result11 = updateUsimPBRecordsByIndexWithError(_arg011, _arg110, adnRecordCreateFromParcel3, _arg39);
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    int _arg111 = data.readInt();
                    if (data.readInt() != 0) {
                        adnRecordCreateFromParcel = AdnRecord.CREATOR.createFromParcel(data);
                    } else {
                        adnRecordCreateFromParcel = null;
                    }
                    if (data.readInt() != 0) {
                        adnRecordCreateFromParcel2 = AdnRecord.CREATOR.createFromParcel(data);
                    } else {
                        adnRecordCreateFromParcel2 = null;
                    }
                    int _result12 = updateUsimPBRecordsBySearchWithError(_arg012, _arg111, adnRecordCreateFromParcel, adnRecordCreateFromParcel2);
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    int[] _result13 = getAdnRecordsSize(_arg013);
                    reply.writeNoException();
                    reply.writeIntArray(_result13);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg014 = data.readInt();
                    int _arg112 = data.readInt();
                    int[] _result14 = getAdnRecordsSizeForSubscriber(_arg014, _arg112);
                    reply.writeNoException();
                    reply.writeIntArray(_result14);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    boolean _result15 = isPhbReady(_arg015);
                    reply.writeNoException();
                    reply.writeInt(_result15 ? 1 : 0);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    List<UsimGroup> _result16 = getUsimGroups(_arg016);
                    reply.writeNoException();
                    reply.writeTypedList(_result16);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _arg113 = data.readInt();
                    String _result17 = getUsimGroupById(_arg017, _arg113);
                    reply.writeNoException();
                    reply.writeString(_result17);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    int _arg114 = data.readInt();
                    boolean _result18 = removeUsimGroupById(_arg018, _arg114);
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    String _arg115 = data.readString();
                    int _result19 = insertUsimGroup(_arg019, _arg115);
                    reply.writeNoException();
                    reply.writeInt(_result19);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg020 = data.readInt();
                    int _arg116 = data.readInt();
                    String _arg29 = data.readString();
                    int _result20 = updateUsimGroup(_arg020, _arg116, _arg29);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    int _arg117 = data.readInt();
                    int _arg210 = data.readInt();
                    boolean _result21 = addContactToGroup(_arg021, _arg117, _arg210);
                    reply.writeNoException();
                    reply.writeInt(_result21 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg022 = data.readInt();
                    int _arg118 = data.readInt();
                    int _arg211 = data.readInt();
                    boolean _result22 = removeContactFromGroup(_arg022, _arg118, _arg211);
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg023 = data.readInt();
                    int _arg119 = data.readInt();
                    int[] _arg212 = data.createIntArray();
                    boolean _result23 = updateContactToGroups(_arg023, _arg119, _arg212);
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg024 = data.readInt();
                    int _arg120 = data.readInt();
                    int[] _arg213 = data.createIntArray();
                    int[] _arg310 = data.createIntArray();
                    boolean _result24 = moveContactFromGroupsToGroups(_arg024, _arg120, _arg213, _arg310);
                    reply.writeNoException();
                    reply.writeInt(_result24 ? 1 : 0);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg025 = data.readInt();
                    String _arg121 = data.readString();
                    int _result25 = hasExistGroup(_arg025, _arg121);
                    reply.writeNoException();
                    reply.writeInt(_result25);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg026 = data.readInt();
                    int _result26 = getUsimGrpMaxNameLen(_arg026);
                    reply.writeNoException();
                    reply.writeInt(_result26);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg027 = data.readInt();
                    int _result27 = getUsimGrpMaxCount(_arg027);
                    reply.writeNoException();
                    reply.writeInt(_result27);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg028 = data.readInt();
                    List<AlphaTag> _result28 = getUsimAasList(_arg028);
                    reply.writeNoException();
                    reply.writeTypedList(_result28);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    int _arg122 = data.readInt();
                    String _result29 = getUsimAasById(_arg029, _arg122);
                    reply.writeNoException();
                    reply.writeString(_result29);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg030 = data.readInt();
                    String _arg123 = data.readString();
                    int _result30 = insertUsimAas(_arg030, _arg123);
                    reply.writeNoException();
                    reply.writeInt(_result30);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg031 = data.readInt();
                    int _result31 = getAnrCount(_arg031);
                    reply.writeNoException();
                    reply.writeInt(_result31);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg032 = data.readInt();
                    int _result32 = getEmailCount(_arg032);
                    reply.writeNoException();
                    reply.writeInt(_result32);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg033 = data.readInt();
                    int _result33 = getUsimAasMaxCount(_arg033);
                    reply.writeNoException();
                    reply.writeInt(_result33);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg034 = data.readInt();
                    int _result34 = getUsimAasMaxNameLen(_arg034);
                    reply.writeNoException();
                    reply.writeInt(_result34);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg035 = data.readInt();
                    int _arg124 = data.readInt();
                    int _arg214 = data.readInt();
                    String _arg311 = data.readString();
                    boolean _result35 = updateUsimAas(_arg035, _arg124, _arg214, _arg311);
                    reply.writeNoException();
                    reply.writeInt(_result35 ? 1 : 0);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg036 = data.readInt();
                    int _arg125 = data.readInt();
                    int _arg215 = data.readInt();
                    boolean _result36 = removeUsimAasById(_arg036, _arg125, _arg215);
                    reply.writeNoException();
                    reply.writeInt(_result36 ? 1 : 0);
                    return true;
                case 37:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg037 = data.readInt();
                    boolean _result37 = hasSne(_arg037);
                    reply.writeNoException();
                    reply.writeInt(_result37 ? 1 : 0);
                    return true;
                case 38:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg038 = data.readInt();
                    int _result38 = getSneRecordLen(_arg038);
                    reply.writeNoException();
                    reply.writeInt(_result38);
                    return true;
                case 39:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg039 = data.readInt();
                    boolean _result39 = isAdnAccessible(_arg039);
                    reply.writeNoException();
                    reply.writeInt(_result39 ? 1 : 0);
                    return true;
                case 40:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg040 = data.readInt();
                    UsimPBMemInfo[] _result40 = getPhonebookMemStorageExt(_arg040);
                    reply.writeNoException();
                    reply.writeTypedArray(_result40, 1);
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
            public int updateAdnRecordsInEfBySearchWithError(int subId, int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
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
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int updateUsimPBRecordsInEfBySearchWithError(int subId, int efid, String oldTag, String oldPhoneNumber, String oldAnr, String oldGrpIds, String[] oldEmails, String newTag, String newPhoneNumber, String newAnr, String newGrpIds, String[] newEmails) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    _data.writeString(oldTag);
                    _data.writeString(oldPhoneNumber);
                    _data.writeString(oldAnr);
                    _data.writeString(oldGrpIds);
                    _data.writeStringArray(oldEmails);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeString(newAnr);
                    _data.writeString(newGrpIds);
                    _data.writeStringArray(newEmails);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
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
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
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
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int updateAdnRecordsInEfByIndexWithError(int subId, int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
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
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int updateUsimPBRecordsInEfByIndexWithError(int subId, int efid, String newTag, String newPhoneNumber, String newAnr, String newGrpIds, String[] newEmails, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    _data.writeString(newTag);
                    _data.writeString(newPhoneNumber);
                    _data.writeString(newAnr);
                    _data.writeString(newGrpIds);
                    _data.writeStringArray(newEmails);
                    _data.writeInt(index);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int updateUsimPBRecordsByIndexWithError(int subId, int efid, AdnRecord record, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    if (record != null) {
                        _data.writeInt(1);
                        record.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(index);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int updateUsimPBRecordsBySearchWithError(int subId, int efid, AdnRecord oldAdn, AdnRecord newAdn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(efid);
                    if (oldAdn != null) {
                        _data.writeInt(1);
                        oldAdn.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (newAdn != null) {
                        _data.writeInt(1);
                        newAdn.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
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
                    this.mRemote.transact(13, _data, _reply, 0);
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
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isPhbReady(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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
            public List<UsimGroup> getUsimGroups(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    List<UsimGroup> _result = _reply.createTypedArrayList(UsimGroup.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getUsimGroupById(int subId, int nGasId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(nGasId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean removeUsimGroupById(int subId, int nGasId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(nGasId);
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
            public int insertUsimGroup(int subId, String grpName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(grpName);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int updateUsimGroup(int subId, int nGasId, String grpName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(nGasId);
                    _data.writeString(grpName);
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
            public boolean addContactToGroup(int subId, int adnIndex, int grpIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(adnIndex);
                    _data.writeInt(grpIndex);
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
            public boolean removeContactFromGroup(int subId, int adnIndex, int grpIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(adnIndex);
                    _data.writeInt(grpIndex);
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
            public boolean updateContactToGroups(int subId, int adnIndex, int[] grpIdList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(adnIndex);
                    _data.writeIntArray(grpIdList);
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
            public boolean moveContactFromGroupsToGroups(int subId, int adnIndex, int[] fromGrpIdList, int[] toGrpIdList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(adnIndex);
                    _data.writeIntArray(fromGrpIdList);
                    _data.writeIntArray(toGrpIdList);
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
            public int hasExistGroup(int subId, String grpName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(grpName);
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
            public int getUsimGrpMaxNameLen(int subId) throws RemoteException {
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
            public int getUsimGrpMaxCount(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<AlphaTag> getUsimAasList(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    List<AlphaTag> _result = _reply.createTypedArrayList(AlphaTag.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getUsimAasById(int subId, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(index);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int insertUsimAas(int subId, String aasName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(aasName);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getAnrCount(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getEmailCount(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getUsimAasMaxCount(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getUsimAasMaxNameLen(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean updateUsimAas(int subId, int index, int pbrIndex, String aasName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(index);
                    _data.writeInt(pbrIndex);
                    _data.writeString(aasName);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean removeUsimAasById(int subId, int index, int pbrIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(index);
                    _data.writeInt(pbrIndex);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean hasSne(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getSneRecordLen(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isAdnAccessible(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public UsimPBMemInfo[] getPhonebookMemStorageExt(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    UsimPBMemInfo[] _result = (UsimPBMemInfo[]) _reply.createTypedArray(UsimPBMemInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
