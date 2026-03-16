package jp.co.omronsoft.iwnnime.ml;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IEngineService extends IInterface {
    int addWord(String str, String str2, String str3) throws RemoteException;

    int addWordDetail(String str, String str2, String str3, int i, int i2, int i3) throws RemoteException;

    Bundle convert(String str, String str2, int i) throws RemoteException;

    Bundle convertWithAnnotation(String str, String str2, int i) throws RemoteException;

    Bundle convertWithAnnotation2(String str, String str2, int i, int i2) throws RemoteException;

    boolean deleteWord(String str, String str2, String str3) throws RemoteException;

    void disconnect(String str) throws RemoteException;

    int getDictionaryType(String str) throws RemoteException;

    int getErrorCode(String str) throws RemoteException;

    Bundle getNextCandidate(String str, int i) throws RemoteException;

    Bundle getNextCandidateWithAnnotation(String str, int i) throws RemoteException;

    Bundle getNextCandidateWithAnnotation2(String str, int i, int i2) throws RemoteException;

    int getStatus(String str) throws RemoteException;

    void init(String str, String str2, int i) throws RemoteException;

    boolean initializeDictionary(String str) throws RemoteException;

    boolean isAlive(String str) throws RemoteException;

    boolean isGijiDic(String str, int i) throws RemoteException;

    boolean learnCandidate(String str, int i) throws RemoteException;

    boolean learnCandidateNoConnect(String str, int i) throws RemoteException;

    boolean learnCandidateNoStore(String str, int i) throws RemoteException;

    boolean learnWord(String str, String str2, String str3) throws RemoteException;

    boolean learnWordNoConnect(String str, String str2, String str3) throws RemoteException;

    boolean learnWordNoStore(String str, String str2, String str3) throws RemoteException;

    int predict(String str, String str2, int i, int i2) throws RemoteException;

    int searchWords(String str, String str2) throws RemoteException;

    int searchWordsDetail(String str, String str2, int i, int i2) throws RemoteException;

    boolean setDictionary(String str, String str2, int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) throws RemoteException;

    boolean setDictionaryDecoratedPict(String str, String str2, int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7) throws RemoteException;

    int setEnableConsecutivePhraseLevelConversion(String str, boolean z) throws RemoteException;

    boolean setGijiFilter(String str, int[] iArr) throws RemoteException;

    void setLearnDictionary(String str) throws RemoteException;

    void setNormalDictionary(String str) throws RemoteException;

    void setUserDictionary(String str) throws RemoteException;

    void startInput(String str) throws RemoteException;

    boolean undo(String str) throws RemoteException;

    boolean writeoutDictionary(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IEngineService {
        private static final String DESCRIPTOR = "jp.co.omronsoft.iwnnime.ml.IEngineService";
        static final int TRANSACTION_addWord = 11;
        static final int TRANSACTION_addWordDetail = 12;
        static final int TRANSACTION_convert = 8;
        static final int TRANSACTION_convertWithAnnotation = 9;
        static final int TRANSACTION_convertWithAnnotation2 = 10;
        static final int TRANSACTION_deleteWord = 15;
        static final int TRANSACTION_disconnect = 35;
        static final int TRANSACTION_getDictionaryType = 33;
        static final int TRANSACTION_getErrorCode = 34;
        static final int TRANSACTION_getNextCandidate = 4;
        static final int TRANSACTION_getNextCandidateWithAnnotation = 5;
        static final int TRANSACTION_getNextCandidateWithAnnotation2 = 6;
        static final int TRANSACTION_getStatus = 32;
        static final int TRANSACTION_init = 1;
        static final int TRANSACTION_initializeDictionary = 17;
        static final int TRANSACTION_isAlive = 36;
        static final int TRANSACTION_isGijiDic = 29;
        static final int TRANSACTION_learnCandidate = 7;
        static final int TRANSACTION_learnCandidateNoConnect = 22;
        static final int TRANSACTION_learnCandidateNoStore = 21;
        static final int TRANSACTION_learnWord = 23;
        static final int TRANSACTION_learnWordNoConnect = 25;
        static final int TRANSACTION_learnWordNoStore = 24;
        static final int TRANSACTION_predict = 2;
        static final int TRANSACTION_searchWords = 13;
        static final int TRANSACTION_searchWordsDetail = 14;
        static final int TRANSACTION_setDictionary = 26;
        static final int TRANSACTION_setDictionaryDecoratedPict = 27;
        static final int TRANSACTION_setEnableConsecutivePhraseLevelConversion = 3;
        static final int TRANSACTION_setGijiFilter = 30;
        static final int TRANSACTION_setLearnDictionary = 19;
        static final int TRANSACTION_setNormalDictionary = 20;
        static final int TRANSACTION_setUserDictionary = 18;
        static final int TRANSACTION_startInput = 31;
        static final int TRANSACTION_undo = 28;
        static final int TRANSACTION_writeoutDictionary = 16;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEngineService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IEngineService)) {
                return (IEngineService) iin;
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
                    String _arg0 = data.readString();
                    String _arg1 = data.readString();
                    int _arg2 = data.readInt();
                    init(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    String _arg12 = data.readString();
                    int _arg22 = data.readInt();
                    int _arg3 = data.readInt();
                    int _result = predict(_arg02, _arg12, _arg22, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    int _result2 = setEnableConsecutivePhraseLevelConversion(_arg03, data.readInt() != 0);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    Bundle _result3 = getNextCandidate(_arg04, data.readInt());
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    Bundle _result4 = getNextCandidateWithAnnotation(_arg05, data.readInt());
                    reply.writeNoException();
                    if (_result4 != null) {
                        reply.writeInt(1);
                        _result4.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    int _arg13 = data.readInt();
                    int _arg23 = data.readInt();
                    Bundle _result5 = getNextCandidateWithAnnotation2(_arg06, _arg13, _arg23);
                    reply.writeNoException();
                    if (_result5 != null) {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    boolean _result6 = learnCandidate(_arg07, data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result6 ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    String _arg14 = data.readString();
                    int _arg24 = data.readInt();
                    Bundle _result7 = convert(_arg08, _arg14, _arg24);
                    reply.writeNoException();
                    if (_result7 != null) {
                        reply.writeInt(1);
                        _result7.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    String _arg15 = data.readString();
                    int _arg25 = data.readInt();
                    Bundle _result8 = convertWithAnnotation(_arg09, _arg15, _arg25);
                    reply.writeNoException();
                    if (_result8 != null) {
                        reply.writeInt(1);
                        _result8.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    String _arg16 = data.readString();
                    int _arg26 = data.readInt();
                    int _arg32 = data.readInt();
                    Bundle _result9 = convertWithAnnotation2(_arg010, _arg16, _arg26, _arg32);
                    reply.writeNoException();
                    if (_result9 != null) {
                        reply.writeInt(1);
                        _result9.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    String _arg17 = data.readString();
                    String _arg27 = data.readString();
                    int _result10 = addWord(_arg011, _arg17, _arg27);
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    String _arg18 = data.readString();
                    String _arg28 = data.readString();
                    int _arg33 = data.readInt();
                    int _arg4 = data.readInt();
                    int _arg5 = data.readInt();
                    int _result11 = addWordDetail(_arg012, _arg18, _arg28, _arg33, _arg4, _arg5);
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    int _result12 = searchWords(_arg013, data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    String _arg19 = data.readString();
                    int _arg29 = data.readInt();
                    int _arg34 = data.readInt();
                    int _result13 = searchWordsDetail(_arg014, _arg19, _arg29, _arg34);
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    String _arg110 = data.readString();
                    String _arg210 = data.readString();
                    boolean _result14 = deleteWord(_arg015, _arg110, _arg210);
                    reply.writeNoException();
                    reply.writeInt(_result14 ? 1 : 0);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    boolean _result15 = writeoutDictionary(_arg016);
                    reply.writeNoException();
                    reply.writeInt(_result15 ? 1 : 0);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    boolean _result16 = initializeDictionary(_arg017);
                    reply.writeNoException();
                    reply.writeInt(_result16 ? 1 : 0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    setUserDictionary(_arg018);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    setLearnDictionary(_arg019);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    setNormalDictionary(_arg020);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg021 = data.readString();
                    boolean _result17 = learnCandidateNoStore(_arg021, data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result17 ? 1 : 0);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    boolean _result18 = learnCandidateNoConnect(_arg022, data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result18 ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    String _arg111 = data.readString();
                    String _arg211 = data.readString();
                    boolean _result19 = learnWord(_arg023, _arg111, _arg211);
                    reply.writeNoException();
                    reply.writeInt(_result19 ? 1 : 0);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg024 = data.readString();
                    String _arg112 = data.readString();
                    String _arg212 = data.readString();
                    boolean _result20 = learnWordNoStore(_arg024, _arg112, _arg212);
                    reply.writeNoException();
                    reply.writeInt(_result20 ? 1 : 0);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    String _arg113 = data.readString();
                    String _arg213 = data.readString();
                    boolean _result21 = learnWordNoConnect(_arg025, _arg113, _arg213);
                    reply.writeNoException();
                    reply.writeInt(_result21 ? 1 : 0);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    String _arg114 = data.readString();
                    int _arg214 = data.readInt();
                    int _arg35 = data.readInt();
                    boolean _arg42 = data.readInt() != 0;
                    boolean _arg52 = data.readInt() != 0;
                    boolean _arg6 = data.readInt() != 0;
                    boolean _arg7 = data.readInt() != 0;
                    boolean _arg8 = data.readInt() != 0;
                    boolean _arg9 = data.readInt() != 0;
                    boolean _result22 = setDictionary(_arg026, _arg114, _arg214, _arg35, _arg42, _arg52, _arg6, _arg7, _arg8, _arg9);
                    reply.writeNoException();
                    reply.writeInt(_result22 ? 1 : 0);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    String _arg115 = data.readString();
                    int _arg215 = data.readInt();
                    int _arg36 = data.readInt();
                    boolean _arg43 = data.readInt() != 0;
                    boolean _arg53 = data.readInt() != 0;
                    boolean _arg62 = data.readInt() != 0;
                    boolean _arg72 = data.readInt() != 0;
                    boolean _arg82 = data.readInt() != 0;
                    boolean _arg92 = data.readInt() != 0;
                    boolean _arg10 = data.readInt() != 0;
                    boolean _result23 = setDictionaryDecoratedPict(_arg027, _arg115, _arg215, _arg36, _arg43, _arg53, _arg62, _arg72, _arg82, _arg92, _arg10);
                    reply.writeNoException();
                    reply.writeInt(_result23 ? 1 : 0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg028 = data.readString();
                    boolean _result24 = undo(_arg028);
                    reply.writeNoException();
                    reply.writeInt(_result24 ? 1 : 0);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg029 = data.readString();
                    boolean _result25 = isGijiDic(_arg029, data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result25 ? 1 : 0);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg030 = data.readString();
                    int[] _arg116 = data.createIntArray();
                    boolean _result26 = setGijiFilter(_arg030, _arg116);
                    reply.writeNoException();
                    reply.writeInt(_result26 ? 1 : 0);
                    reply.writeIntArray(_arg116);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg031 = data.readString();
                    startInput(_arg031);
                    reply.writeNoException();
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg032 = data.readString();
                    int _result27 = getStatus(_arg032);
                    reply.writeNoException();
                    reply.writeInt(_result27);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg033 = data.readString();
                    int _result28 = getDictionaryType(_arg033);
                    reply.writeNoException();
                    reply.writeInt(_result28);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg034 = data.readString();
                    int _result29 = getErrorCode(_arg034);
                    reply.writeNoException();
                    reply.writeInt(_result29);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg035 = data.readString();
                    disconnect(_arg035);
                    reply.writeNoException();
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg036 = data.readString();
                    boolean _result30 = isAlive(_arg036);
                    reply.writeNoException();
                    reply.writeInt(_result30 ? 1 : 0);
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IEngineService {
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
            public void init(String packageName, String password, int initLevel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(password);
                    _data.writeInt(initLevel);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int predict(String packageName, String stroke, int minLen, int maxLen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(stroke);
                    _data.writeInt(minLen);
                    _data.writeInt(maxLen);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setEnableConsecutivePhraseLevelConversion(String packageName, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(enable ? 1 : 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getNextCandidate(String packageName, int numberOfCandidates) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(numberOfCandidates);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getNextCandidateWithAnnotation(String packageName, int numberOfCandidates) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(numberOfCandidates);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getNextCandidateWithAnnotation2(String packageName, int numberOfCandidates, int emojitype) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(numberOfCandidates);
                    _data.writeInt(emojitype);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean learnCandidate(String packageName, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(index);
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
            public Bundle convert(String packageName, String stroke, int divide) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(stroke);
                    _data.writeInt(divide);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle convertWithAnnotation(String packageName, String stroke, int divide) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(stroke);
                    _data.writeInt(divide);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle convertWithAnnotation2(String packageName, String stroke, int divide, int emojitype) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(stroke);
                    _data.writeInt(divide);
                    _data.writeInt(emojitype);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int addWord(String packageName, String candidate, String stroke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(candidate);
                    _data.writeString(stroke);
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
            public int addWordDetail(String packageName, String candidate, String stroke, int hinsi, int type, int relation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(candidate);
                    _data.writeString(stroke);
                    _data.writeInt(hinsi);
                    _data.writeInt(type);
                    _data.writeInt(relation);
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
            public int searchWords(String packageName, String stroke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(stroke);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int searchWordsDetail(String packageName, String stroke, int method, int order) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(stroke);
                    _data.writeInt(method);
                    _data.writeInt(order);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean deleteWord(String packageName, String candidate, String stroke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(candidate);
                    _data.writeString(stroke);
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
            public boolean writeoutDictionary(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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
            public boolean initializeDictionary(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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
            public void setUserDictionary(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setLearnDictionary(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setNormalDictionary(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean learnCandidateNoStore(String packageName, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(index);
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
            public boolean learnCandidateNoConnect(String packageName, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(index);
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
            public boolean learnWord(String packageName, String candidate, String stroke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(candidate);
                    _data.writeString(stroke);
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
            public boolean learnWordNoStore(String packageName, String candidate, String stroke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(candidate);
                    _data.writeString(stroke);
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
            public boolean learnWordNoConnect(String packageName, String candidate, String stroke) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(candidate);
                    _data.writeString(stroke);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setDictionary(String packageName, String configurationFile, int language, int dictionary, boolean flexibleSearch, boolean tenKeyType, boolean emojiFilter, boolean emailFilter, boolean convertCandidates, boolean learnNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(configurationFile);
                    _data.writeInt(language);
                    _data.writeInt(dictionary);
                    _data.writeInt(flexibleSearch ? 1 : 0);
                    _data.writeInt(tenKeyType ? 1 : 0);
                    _data.writeInt(emojiFilter ? 1 : 0);
                    _data.writeInt(emailFilter ? 1 : 0);
                    _data.writeInt(convertCandidates ? 1 : 0);
                    _data.writeInt(learnNumber ? 1 : 0);
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
            public boolean setDictionaryDecoratedPict(String packageName, String configurationFile, int language, int dictionary, boolean flexibleSearch, boolean tenKeyType, boolean emojiFilter, boolean decoemojiFilter, boolean emailFilter, boolean convertCandidates, boolean learnNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(configurationFile);
                    _data.writeInt(language);
                    _data.writeInt(dictionary);
                    _data.writeInt(flexibleSearch ? 1 : 0);
                    _data.writeInt(tenKeyType ? 1 : 0);
                    _data.writeInt(emojiFilter ? 1 : 0);
                    _data.writeInt(decoemojiFilter ? 1 : 0);
                    _data.writeInt(emailFilter ? 1 : 0);
                    _data.writeInt(convertCandidates ? 1 : 0);
                    _data.writeInt(learnNumber ? 1 : 0);
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
            public boolean undo(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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
            public boolean isGijiDic(String packageName, int index) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(index);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setGijiFilter(String packageName, int[] type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeIntArray(type);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.readIntArray(type);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startInput(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getStatus(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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
            public int getDictionaryType(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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
            public int getErrorCode(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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
            public void disconnect(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isAlive(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
