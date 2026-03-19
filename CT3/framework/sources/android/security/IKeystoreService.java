package android.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.security.keymaster.ExportResult;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterBlob;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.OperationResult;

public interface IKeystoreService extends IInterface {
    int abort(IBinder iBinder) throws RemoteException;

    int addAuthToken(byte[] bArr) throws RemoteException;

    int addRngEntropy(byte[] bArr) throws RemoteException;

    int attestKey(String str, KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    OperationResult begin(IBinder iBinder, String str, int i, boolean z, KeymasterArguments keymasterArguments, byte[] bArr, int i2) throws RemoteException;

    int clear_uid(long j) throws RemoteException;

    int del(String str, int i) throws RemoteException;

    int duplicate(String str, int i, String str2, int i2) throws RemoteException;

    int exist(String str, int i) throws RemoteException;

    ExportResult exportKey(String str, int i, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i2) throws RemoteException;

    OperationResult finish(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr, byte[] bArr2) throws RemoteException;

    int generate(String str, int i, int i2, int i3, int i4, KeystoreArguments keystoreArguments) throws RemoteException;

    int generateKey(String str, KeymasterArguments keymasterArguments, byte[] bArr, int i, int i2, KeyCharacteristics keyCharacteristics) throws RemoteException;

    byte[] get(String str, int i) throws RemoteException;

    int getKeyCharacteristics(String str, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int getState(int i) throws RemoteException;

    byte[] get_pubkey(String str) throws RemoteException;

    long getmtime(String str, int i) throws RemoteException;

    int grant(String str, int i) throws RemoteException;

    int importKey(String str, KeymasterArguments keymasterArguments, int i, byte[] bArr, int i2, int i3, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int import_key(String str, byte[] bArr, int i, int i2) throws RemoteException;

    int insert(String str, byte[] bArr, int i, int i2) throws RemoteException;

    int isEmpty(int i) throws RemoteException;

    boolean isOperationAuthorized(IBinder iBinder) throws RemoteException;

    int is_hardware_backed(String str) throws RemoteException;

    String[] list(String str, int i) throws RemoteException;

    int lock(int i) throws RemoteException;

    int onUserAdded(int i, int i2) throws RemoteException;

    int onUserPasswordChanged(int i, String str) throws RemoteException;

    int onUserRemoved(int i) throws RemoteException;

    int reset() throws RemoteException;

    byte[] sign(String str, byte[] bArr) throws RemoteException;

    int ungrant(String str, int i) throws RemoteException;

    int unlock(int i, String str) throws RemoteException;

    OperationResult update(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr) throws RemoteException;

    int verify(String str, byte[] bArr, byte[] bArr2) throws RemoteException;

    public static abstract class Stub extends Binder implements IKeystoreService {
        private static final String DESCRIPTOR = "android.security.IKeystoreService";
        static final int TRANSACTION_abort = 31;
        static final int TRANSACTION_addAuthToken = 33;
        static final int TRANSACTION_addRngEntropy = 23;
        static final int TRANSACTION_attestKey = 36;
        static final int TRANSACTION_begin = 28;
        static final int TRANSACTION_clear_uid = 22;
        static final int TRANSACTION_del = 4;
        static final int TRANSACTION_duplicate = 20;
        static final int TRANSACTION_exist = 5;
        static final int TRANSACTION_exportKey = 27;
        static final int TRANSACTION_finish = 30;
        static final int TRANSACTION_generate = 12;
        static final int TRANSACTION_generateKey = 24;
        static final int TRANSACTION_get = 2;
        static final int TRANSACTION_getKeyCharacteristics = 25;
        static final int TRANSACTION_getState = 1;
        static final int TRANSACTION_get_pubkey = 16;
        static final int TRANSACTION_getmtime = 19;
        static final int TRANSACTION_grant = 17;
        static final int TRANSACTION_importKey = 26;
        static final int TRANSACTION_import_key = 13;
        static final int TRANSACTION_insert = 3;
        static final int TRANSACTION_isEmpty = 11;
        static final int TRANSACTION_isOperationAuthorized = 32;
        static final int TRANSACTION_is_hardware_backed = 21;
        static final int TRANSACTION_list = 6;
        static final int TRANSACTION_lock = 9;
        static final int TRANSACTION_onUserAdded = 34;
        static final int TRANSACTION_onUserPasswordChanged = 8;
        static final int TRANSACTION_onUserRemoved = 35;
        static final int TRANSACTION_reset = 7;
        static final int TRANSACTION_sign = 14;
        static final int TRANSACTION_ungrant = 18;
        static final int TRANSACTION_unlock = 10;
        static final int TRANSACTION_update = 29;
        static final int TRANSACTION_verify = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IKeystoreService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IKeystoreService)) {
                return (IKeystoreService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            KeymasterArguments keymasterArgumentsCreateFromParcel;
            KeymasterArguments keymasterArgumentsCreateFromParcel2;
            KeymasterArguments keymasterArgumentsCreateFromParcel3;
            KeymasterArguments keymasterArgumentsCreateFromParcel4;
            KeymasterBlob keymasterBlobCreateFromParcel;
            KeymasterBlob keymasterBlobCreateFromParcel2;
            KeymasterArguments keymasterArgumentsCreateFromParcel5;
            KeymasterBlob keymasterBlobCreateFromParcel3;
            KeymasterBlob keymasterBlobCreateFromParcel4;
            KeymasterArguments keymasterArgumentsCreateFromParcel6;
            KeystoreArguments keystoreArgumentsCreateFromParcel;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    int _result = getState(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg02 = data.readString();
                    int _arg1 = data.readInt();
                    byte[] _result2 = get(_arg02, _arg1);
                    reply.writeNoException();
                    reply.writeByteArray(_result2);
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg03 = data.readString();
                    byte[] _arg12 = data.createByteArray();
                    int _arg2 = data.readInt();
                    int _arg3 = data.readInt();
                    int _result3 = insert(_arg03, _arg12, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    int _arg13 = data.readInt();
                    int _result4 = del(_arg04, _arg13);
                    reply.writeNoException();
                    reply.writeInt(_result4);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg14 = data.readInt();
                    int _result5 = exist(_arg05, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result5);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    int _arg15 = data.readInt();
                    String[] _result6 = list(_arg06, _arg15);
                    reply.writeNoException();
                    reply.writeStringArray(_result6);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _result7 = reset();
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg07 = data.readInt();
                    String _arg16 = data.readString();
                    int _result8 = onUserPasswordChanged(_arg07, _arg16);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    int _result9 = lock(_arg08);
                    reply.writeNoException();
                    reply.writeInt(_result9);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    String _arg17 = data.readString();
                    int _result10 = unlock(_arg09, _arg17);
                    reply.writeNoException();
                    reply.writeInt(_result10);
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _result11 = isEmpty(_arg010);
                    reply.writeNoException();
                    reply.writeInt(_result11);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    int _arg18 = data.readInt();
                    int _arg22 = data.readInt();
                    int _arg32 = data.readInt();
                    int _arg4 = data.readInt();
                    if (data.readInt() != 0) {
                        keystoreArgumentsCreateFromParcel = KeystoreArguments.CREATOR.createFromParcel(data);
                    } else {
                        keystoreArgumentsCreateFromParcel = null;
                    }
                    int _result12 = generate(_arg011, _arg18, _arg22, _arg32, _arg4, keystoreArgumentsCreateFromParcel);
                    reply.writeNoException();
                    reply.writeInt(_result12);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    byte[] _arg19 = data.createByteArray();
                    int _arg23 = data.readInt();
                    int _arg33 = data.readInt();
                    int _result13 = import_key(_arg012, _arg19, _arg23, _arg33);
                    reply.writeNoException();
                    reply.writeInt(_result13);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    byte[] _arg110 = data.createByteArray();
                    byte[] _result14 = sign(_arg013, _arg110);
                    reply.writeNoException();
                    reply.writeByteArray(_result14);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    byte[] _arg111 = data.createByteArray();
                    int _result15 = verify(_arg014, _arg111, data.createByteArray());
                    reply.writeNoException();
                    reply.writeInt(_result15);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    byte[] _result16 = get_pubkey(_arg015);
                    reply.writeNoException();
                    reply.writeByteArray(_result16);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    int _arg112 = data.readInt();
                    int _result17 = grant(_arg016, _arg112);
                    reply.writeNoException();
                    reply.writeInt(_result17);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    int _arg113 = data.readInt();
                    int _result18 = ungrant(_arg017, _arg113);
                    reply.writeNoException();
                    reply.writeInt(_result18);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    int _arg114 = data.readInt();
                    long _result19 = getmtime(_arg018, _arg114);
                    reply.writeNoException();
                    reply.writeLong(_result19);
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    int _arg115 = data.readInt();
                    String _arg24 = data.readString();
                    int _arg34 = data.readInt();
                    int _result20 = duplicate(_arg019, _arg115, _arg24, _arg34);
                    reply.writeNoException();
                    reply.writeInt(_result20);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    int _result21 = is_hardware_backed(_arg020);
                    reply.writeNoException();
                    reply.writeInt(_result21);
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    long _arg021 = data.readLong();
                    int _result22 = clear_uid(_arg021);
                    reply.writeNoException();
                    reply.writeInt(_result22);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg022 = data.createByteArray();
                    int _result23 = addRngEntropy(_arg022);
                    reply.writeNoException();
                    reply.writeInt(_result23);
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg023 = data.readString();
                    if (data.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel6 = KeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        keymasterArgumentsCreateFromParcel6 = null;
                    }
                    byte[] _arg25 = data.createByteArray();
                    int _arg35 = data.readInt();
                    int _arg42 = data.readInt();
                    KeyCharacteristics _arg5 = new KeyCharacteristics();
                    int _result24 = generateKey(_arg023, keymasterArgumentsCreateFromParcel6, _arg25, _arg35, _arg42, _arg5);
                    reply.writeNoException();
                    reply.writeInt(_result24);
                    if (_arg5 != null) {
                        reply.writeInt(1);
                        _arg5.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg024 = data.readString();
                    if (data.readInt() != 0) {
                        keymasterBlobCreateFromParcel3 = KeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        keymasterBlobCreateFromParcel3 = null;
                    }
                    if (data.readInt() != 0) {
                        keymasterBlobCreateFromParcel4 = KeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        keymasterBlobCreateFromParcel4 = null;
                    }
                    int _arg36 = data.readInt();
                    KeyCharacteristics _arg43 = new KeyCharacteristics();
                    int _result25 = getKeyCharacteristics(_arg024, keymasterBlobCreateFromParcel3, keymasterBlobCreateFromParcel4, _arg36, _arg43);
                    reply.writeNoException();
                    reply.writeInt(_result25);
                    if (_arg43 != null) {
                        reply.writeInt(1);
                        _arg43.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg025 = data.readString();
                    if (data.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel5 = KeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        keymasterArgumentsCreateFromParcel5 = null;
                    }
                    int _arg26 = data.readInt();
                    byte[] _arg37 = data.createByteArray();
                    int _arg44 = data.readInt();
                    int _arg52 = data.readInt();
                    KeyCharacteristics _arg6 = new KeyCharacteristics();
                    int _result26 = importKey(_arg025, keymasterArgumentsCreateFromParcel5, _arg26, _arg37, _arg44, _arg52, _arg6);
                    reply.writeNoException();
                    reply.writeInt(_result26);
                    if (_arg6 != null) {
                        reply.writeInt(1);
                        _arg6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg026 = data.readString();
                    int _arg116 = data.readInt();
                    if (data.readInt() != 0) {
                        keymasterBlobCreateFromParcel = KeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        keymasterBlobCreateFromParcel = null;
                    }
                    if (data.readInt() != 0) {
                        keymasterBlobCreateFromParcel2 = KeymasterBlob.CREATOR.createFromParcel(data);
                    } else {
                        keymasterBlobCreateFromParcel2 = null;
                    }
                    int _arg45 = data.readInt();
                    ExportResult _result27 = exportKey(_arg026, _arg116, keymasterBlobCreateFromParcel, keymasterBlobCreateFromParcel2, _arg45);
                    reply.writeNoException();
                    if (_result27 != null) {
                        reply.writeInt(1);
                        _result27.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg027 = data.readStrongBinder();
                    String _arg117 = data.readString();
                    int _arg27 = data.readInt();
                    boolean _arg38 = data.readInt() != 0;
                    if (data.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel4 = KeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        keymasterArgumentsCreateFromParcel4 = null;
                    }
                    byte[] _arg53 = data.createByteArray();
                    OperationResult _result28 = begin(_arg027, _arg117, _arg27, _arg38, keymasterArgumentsCreateFromParcel4, _arg53, data.readInt());
                    reply.writeNoException();
                    if (_result28 != null) {
                        reply.writeInt(1);
                        _result28.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg028 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel3 = KeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        keymasterArgumentsCreateFromParcel3 = null;
                    }
                    OperationResult _result29 = update(_arg028, keymasterArgumentsCreateFromParcel3, data.createByteArray());
                    reply.writeNoException();
                    if (_result29 != null) {
                        reply.writeInt(1);
                        _result29.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg029 = data.readStrongBinder();
                    if (data.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel2 = KeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        keymasterArgumentsCreateFromParcel2 = null;
                    }
                    byte[] _arg28 = data.createByteArray();
                    byte[] _arg39 = data.createByteArray();
                    OperationResult _result30 = finish(_arg029, keymasterArgumentsCreateFromParcel2, _arg28, _arg39);
                    reply.writeNoException();
                    if (_result30 != null) {
                        reply.writeInt(1);
                        _result30.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg030 = data.readStrongBinder();
                    int _result31 = abort(_arg030);
                    reply.writeNoException();
                    reply.writeInt(_result31);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    IBinder _arg031 = data.readStrongBinder();
                    boolean _result32 = isOperationAuthorized(_arg031);
                    reply.writeNoException();
                    reply.writeInt(_result32 ? 1 : 0);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg032 = data.createByteArray();
                    int _result33 = addAuthToken(_arg032);
                    reply.writeNoException();
                    reply.writeInt(_result33);
                    return true;
                case 34:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg033 = data.readInt();
                    int _arg118 = data.readInt();
                    int _result34 = onUserAdded(_arg033, _arg118);
                    reply.writeNoException();
                    reply.writeInt(_result34);
                    return true;
                case 35:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg034 = data.readInt();
                    int _result35 = onUserRemoved(_arg034);
                    reply.writeNoException();
                    reply.writeInt(_result35);
                    return true;
                case 36:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg035 = data.readString();
                    if (data.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel = KeymasterArguments.CREATOR.createFromParcel(data);
                    } else {
                        keymasterArgumentsCreateFromParcel = null;
                    }
                    KeymasterCertificateChain _arg29 = new KeymasterCertificateChain();
                    int _result36 = attestKey(_arg035, keymasterArgumentsCreateFromParcel, _arg29);
                    reply.writeNoException();
                    reply.writeInt(_result36);
                    if (_arg29 != null) {
                        reply.writeInt(1);
                        _arg29.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IKeystoreService {
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
            public int getState(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] get(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int insert(String name, byte[] item, int uid, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(item);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
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
            public int del(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int exist(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
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
            public String[] list(String namePrefix, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(namePrefix);
                    _data.writeInt(uid);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int reset() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int onUserPasswordChanged(int userId, String newPassword) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(newPassword);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int lock(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
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
            public int unlock(int userId, String userPassword) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(userPassword);
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
            public int isEmpty(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
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
            public int generate(String name, int uid, int keyType, int keySize, int flags, KeystoreArguments args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    _data.writeInt(keyType);
                    _data.writeInt(keySize);
                    _data.writeInt(flags);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
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
            public int import_key(String name, byte[] data, int uid, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(data);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
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
            public byte[] sign(String name, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(data);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int verify(String name, byte[] data, byte[] signature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeByteArray(data);
                    _data.writeByteArray(signature);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public byte[] get_pubkey(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int grant(String name, int granteeUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(granteeUid);
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
            public int ungrant(String name, int granteeUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(granteeUid);
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
            public long getmtime(String name, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeInt(uid);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int duplicate(String srcKey, int srcUid, String destKey, int destUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(srcKey);
                    _data.writeInt(srcUid);
                    _data.writeString(destKey);
                    _data.writeInt(destUid);
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
            public int is_hardware_backed(String string) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(string);
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
            public int clear_uid(long uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(uid);
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
            public int addRngEntropy(byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(data);
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
            public int generateKey(String alias, KeymasterArguments arguments, byte[] entropy, int uid, int flags, KeyCharacteristics characteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (arguments != null) {
                        _data.writeInt(1);
                        arguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(entropy);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        characteristics.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getKeyCharacteristics(String alias, KeymasterBlob clientId, KeymasterBlob appId, int uid, KeyCharacteristics characteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (clientId != null) {
                        _data.writeInt(1);
                        clientId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (appId != null) {
                        _data.writeInt(1);
                        appId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        characteristics.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int importKey(String alias, KeymasterArguments arguments, int format, byte[] keyData, int uid, int flags, KeyCharacteristics characteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (arguments != null) {
                        _data.writeInt(1);
                        arguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(format);
                    _data.writeByteArray(keyData);
                    _data.writeInt(uid);
                    _data.writeInt(flags);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        characteristics.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ExportResult exportKey(String alias, int format, KeymasterBlob clientId, KeymasterBlob appId, int uid) throws RemoteException {
                ExportResult exportResultCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(format);
                    if (clientId != null) {
                        _data.writeInt(1);
                        clientId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (appId != null) {
                        _data.writeInt(1);
                        appId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        exportResultCreateFromParcel = ExportResult.CREATOR.createFromParcel(_reply);
                    } else {
                        exportResultCreateFromParcel = null;
                    }
                    return exportResultCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public OperationResult begin(IBinder appToken, String alias, int purpose, boolean pruneable, KeymasterArguments params, byte[] entropy, int uid) throws RemoteException {
                OperationResult operationResultCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    _data.writeString(alias);
                    _data.writeInt(purpose);
                    _data.writeInt(pruneable ? 1 : 0);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(entropy);
                    _data.writeInt(uid);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        operationResultCreateFromParcel = OperationResult.CREATOR.createFromParcel(_reply);
                    } else {
                        operationResultCreateFromParcel = null;
                    }
                    return operationResultCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public OperationResult update(IBinder token, KeymasterArguments params, byte[] input) throws RemoteException {
                OperationResult operationResultCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(input);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        operationResultCreateFromParcel = OperationResult.CREATOR.createFromParcel(_reply);
                    } else {
                        operationResultCreateFromParcel = null;
                    }
                    return operationResultCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public OperationResult finish(IBinder token, KeymasterArguments params, byte[] signature, byte[] entropy) throws RemoteException {
                OperationResult operationResultCreateFromParcel;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(signature);
                    _data.writeByteArray(entropy);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        operationResultCreateFromParcel = OperationResult.CREATOR.createFromParcel(_reply);
                    } else {
                        operationResultCreateFromParcel = null;
                    }
                    return operationResultCreateFromParcel;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int abort(IBinder handle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(handle);
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
            public boolean isOperationAuthorized(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
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
            public int addAuthToken(byte[] authToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(authToken);
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
            public int onUserAdded(int userId, int parentId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(parentId);
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
            public int onUserRemoved(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int attestKey(String alias, KeymasterArguments params, KeymasterCertificateChain chain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        chain.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
