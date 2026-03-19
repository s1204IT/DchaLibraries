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

    int addRngEntropy(byte[] bArr, int i) throws RemoteException;

    int attestDeviceIds(KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    int attestKey(String str, KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    OperationResult begin(IBinder iBinder, String str, int i, boolean z, KeymasterArguments keymasterArguments, byte[] bArr, int i2) throws RemoteException;

    int cancelConfirmationPrompt(IBinder iBinder) throws RemoteException;

    int clear_uid(long j) throws RemoteException;

    int del(String str, int i) throws RemoteException;

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

    String grant(String str, int i) throws RemoteException;

    int importKey(String str, KeymasterArguments keymasterArguments, int i, byte[] bArr, int i2, int i3, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int importWrappedKey(String str, byte[] bArr, String str2, byte[] bArr2, KeymasterArguments keymasterArguments, long j, long j2, KeyCharacteristics keyCharacteristics) throws RemoteException;

    int import_key(String str, byte[] bArr, int i, int i2) throws RemoteException;

    int insert(String str, byte[] bArr, int i, int i2) throws RemoteException;

    boolean isConfirmationPromptSupported() throws RemoteException;

    int isEmpty(int i) throws RemoteException;

    boolean isOperationAuthorized(IBinder iBinder) throws RemoteException;

    int is_hardware_backed(String str) throws RemoteException;

    String[] list(String str, int i) throws RemoteException;

    int lock(int i) throws RemoteException;

    int onDeviceOffBody() throws RemoteException;

    int onKeyguardVisibilityChanged(boolean z, int i) throws RemoteException;

    int onUserAdded(int i, int i2) throws RemoteException;

    int onUserPasswordChanged(int i, String str) throws RemoteException;

    int onUserRemoved(int i) throws RemoteException;

    int presentConfirmationPrompt(IBinder iBinder, String str, byte[] bArr, String str2, int i) throws RemoteException;

    int reset() throws RemoteException;

    byte[] sign(String str, byte[] bArr) throws RemoteException;

    int ungrant(String str, int i) throws RemoteException;

    int unlock(int i, String str) throws RemoteException;

    OperationResult update(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr) throws RemoteException;

    int verify(String str, byte[] bArr, byte[] bArr2) throws RemoteException;

    public static abstract class Stub extends Binder implements IKeystoreService {
        private static final String DESCRIPTOR = "android.security.IKeystoreService";
        static final int TRANSACTION_abort = 30;
        static final int TRANSACTION_addAuthToken = 32;
        static final int TRANSACTION_addRngEntropy = 22;
        static final int TRANSACTION_attestDeviceIds = 36;
        static final int TRANSACTION_attestKey = 35;
        static final int TRANSACTION_begin = 27;
        static final int TRANSACTION_cancelConfirmationPrompt = 40;
        static final int TRANSACTION_clear_uid = 21;
        static final int TRANSACTION_del = 4;
        static final int TRANSACTION_exist = 5;
        static final int TRANSACTION_exportKey = 26;
        static final int TRANSACTION_finish = 29;
        static final int TRANSACTION_generate = 12;
        static final int TRANSACTION_generateKey = 23;
        static final int TRANSACTION_get = 2;
        static final int TRANSACTION_getKeyCharacteristics = 24;
        static final int TRANSACTION_getState = 1;
        static final int TRANSACTION_get_pubkey = 16;
        static final int TRANSACTION_getmtime = 19;
        static final int TRANSACTION_grant = 17;
        static final int TRANSACTION_importKey = 25;
        static final int TRANSACTION_importWrappedKey = 38;
        static final int TRANSACTION_import_key = 13;
        static final int TRANSACTION_insert = 3;
        static final int TRANSACTION_isConfirmationPromptSupported = 41;
        static final int TRANSACTION_isEmpty = 11;
        static final int TRANSACTION_isOperationAuthorized = 31;
        static final int TRANSACTION_is_hardware_backed = 20;
        static final int TRANSACTION_list = 6;
        static final int TRANSACTION_lock = 9;
        static final int TRANSACTION_onDeviceOffBody = 37;
        static final int TRANSACTION_onKeyguardVisibilityChanged = 42;
        static final int TRANSACTION_onUserAdded = 33;
        static final int TRANSACTION_onUserPasswordChanged = 8;
        static final int TRANSACTION_onUserRemoved = 34;
        static final int TRANSACTION_presentConfirmationPrompt = 39;
        static final int TRANSACTION_reset = 7;
        static final int TRANSACTION_sign = 14;
        static final int TRANSACTION_ungrant = 18;
        static final int TRANSACTION_unlock = 10;
        static final int TRANSACTION_update = 28;
        static final int TRANSACTION_verify = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IKeystoreService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IKeystoreService)) {
                return (IKeystoreService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            KeystoreArguments keystoreArgumentsCreateFromParcel;
            KeymasterArguments keymasterArgumentsCreateFromParcel;
            KeymasterBlob keymasterBlobCreateFromParcel;
            KeymasterArguments keymasterArgumentsCreateFromParcel2;
            KeymasterBlob keymasterBlobCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int state = getState(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(state);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArr = get(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArr);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iInsert = insert(parcel.readString(), parcel.createByteArray(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iInsert);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iDel = del(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iDel);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iExist = exist(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iExist);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] list = list(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeStringArray(list);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iReset = reset();
                    parcel2.writeNoException();
                    parcel2.writeInt(iReset);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iOnUserPasswordChanged = onUserPasswordChanged(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iOnUserPasswordChanged);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iLock = lock(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iLock);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iUnlock = unlock(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iUnlock);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iIsEmpty = isEmpty(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iIsEmpty);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    int i3 = parcel.readInt();
                    int i4 = parcel.readInt();
                    int i5 = parcel.readInt();
                    int i6 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        keystoreArgumentsCreateFromParcel = KeystoreArguments.CREATOR.createFromParcel(parcel);
                    } else {
                        keystoreArgumentsCreateFromParcel = null;
                    }
                    int iGenerate = generate(string, i3, i4, i5, i6, keystoreArgumentsCreateFromParcel);
                    parcel2.writeNoException();
                    parcel2.writeInt(iGenerate);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iImport_key = import_key(parcel.readString(), parcel.createByteArray(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iImport_key);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrSign = sign(parcel.readString(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrSign);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iVerify = verify(parcel.readString(), parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iVerify);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArr2 = get_pubkey(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArr2);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    String strGrant = grant(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(strGrant);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iUngrant = ungrant(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iUngrant);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    long j = getmtime(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeLong(j);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iIs_hardware_backed = is_hardware_backed(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iIs_hardware_backed);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iClear_uid = clear_uid(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(iClear_uid);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddRngEntropy = addRngEntropy(parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddRngEntropy);
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string2 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel = KeymasterArguments.CREATOR.createFromParcel(parcel);
                    } else {
                        keymasterArgumentsCreateFromParcel = null;
                    }
                    byte[] bArrCreateByteArray = parcel.createByteArray();
                    int i7 = parcel.readInt();
                    int i8 = parcel.readInt();
                    KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
                    int iGenerateKey = generateKey(string2, keymasterArgumentsCreateFromParcel, bArrCreateByteArray, i7, i8, keyCharacteristics);
                    parcel2.writeNoException();
                    parcel2.writeInt(iGenerateKey);
                    parcel2.writeInt(1);
                    keyCharacteristics.writeToParcel(parcel2, 1);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string3 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        keymasterBlobCreateFromParcel = KeymasterBlob.CREATOR.createFromParcel(parcel);
                    } else {
                        keymasterBlobCreateFromParcel = null;
                    }
                    KeymasterBlob keymasterBlobCreateFromParcel3 = parcel.readInt() != 0 ? KeymasterBlob.CREATOR.createFromParcel(parcel) : null;
                    int i9 = parcel.readInt();
                    KeyCharacteristics keyCharacteristics2 = new KeyCharacteristics();
                    int keyCharacteristics3 = getKeyCharacteristics(string3, keymasterBlobCreateFromParcel, keymasterBlobCreateFromParcel3, i9, keyCharacteristics2);
                    parcel2.writeNoException();
                    parcel2.writeInt(keyCharacteristics3);
                    parcel2.writeInt(1);
                    keyCharacteristics2.writeToParcel(parcel2, 1);
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string4 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        keymasterArgumentsCreateFromParcel2 = KeymasterArguments.CREATOR.createFromParcel(parcel);
                    } else {
                        keymasterArgumentsCreateFromParcel2 = null;
                    }
                    int i10 = parcel.readInt();
                    byte[] bArrCreateByteArray2 = parcel.createByteArray();
                    int i11 = parcel.readInt();
                    int i12 = parcel.readInt();
                    KeyCharacteristics keyCharacteristics4 = new KeyCharacteristics();
                    int iImportKey = importKey(string4, keymasterArgumentsCreateFromParcel2, i10, bArrCreateByteArray2, i11, i12, keyCharacteristics4);
                    parcel2.writeNoException();
                    parcel2.writeInt(iImportKey);
                    parcel2.writeInt(1);
                    keyCharacteristics4.writeToParcel(parcel2, 1);
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string5 = parcel.readString();
                    int i13 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        keymasterBlobCreateFromParcel2 = KeymasterBlob.CREATOR.createFromParcel(parcel);
                    } else {
                        keymasterBlobCreateFromParcel2 = null;
                    }
                    ExportResult exportResultExportKey = exportKey(string5, i13, keymasterBlobCreateFromParcel2, parcel.readInt() != 0 ? KeymasterBlob.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    if (exportResultExportKey != null) {
                        parcel2.writeInt(1);
                        exportResultExportKey.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    OperationResult operationResultBegin = begin(parcel.readStrongBinder(), parcel.readString(), parcel.readInt(), parcel.readInt() != 0, parcel.readInt() != 0 ? KeymasterArguments.CREATOR.createFromParcel(parcel) : null, parcel.createByteArray(), parcel.readInt());
                    parcel2.writeNoException();
                    if (operationResultBegin != null) {
                        parcel2.writeInt(1);
                        operationResultBegin.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    OperationResult operationResultUpdate = update(parcel.readStrongBinder(), parcel.readInt() != 0 ? KeymasterArguments.CREATOR.createFromParcel(parcel) : null, parcel.createByteArray());
                    parcel2.writeNoException();
                    if (operationResultUpdate != null) {
                        parcel2.writeInt(1);
                        operationResultUpdate.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    OperationResult operationResultFinish = finish(parcel.readStrongBinder(), parcel.readInt() != 0 ? KeymasterArguments.CREATOR.createFromParcel(parcel) : null, parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    if (operationResultFinish != null) {
                        parcel2.writeInt(1);
                        operationResultFinish.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAbort = abort(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAbort);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsOperationAuthorized = isOperationAuthorized(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsOperationAuthorized ? 1 : 0);
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddAuthToken = addAuthToken(parcel.createByteArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddAuthToken);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iOnUserAdded = onUserAdded(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iOnUserAdded);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iOnUserRemoved = onUserRemoved(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iOnUserRemoved);
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string6 = parcel.readString();
                    KeymasterArguments keymasterArgumentsCreateFromParcel3 = parcel.readInt() != 0 ? KeymasterArguments.CREATOR.createFromParcel(parcel) : null;
                    KeymasterCertificateChain keymasterCertificateChain = new KeymasterCertificateChain();
                    int iAttestKey = attestKey(string6, keymasterArgumentsCreateFromParcel3, keymasterCertificateChain);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAttestKey);
                    parcel2.writeInt(1);
                    keymasterCertificateChain.writeToParcel(parcel2, 1);
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    KeymasterArguments keymasterArgumentsCreateFromParcel4 = parcel.readInt() != 0 ? KeymasterArguments.CREATOR.createFromParcel(parcel) : null;
                    KeymasterCertificateChain keymasterCertificateChain2 = new KeymasterCertificateChain();
                    int iAttestDeviceIds = attestDeviceIds(keymasterArgumentsCreateFromParcel4, keymasterCertificateChain2);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAttestDeviceIds);
                    parcel2.writeInt(1);
                    keymasterCertificateChain2.writeToParcel(parcel2, 1);
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iOnDeviceOffBody = onDeviceOffBody();
                    parcel2.writeNoException();
                    parcel2.writeInt(iOnDeviceOffBody);
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string7 = parcel.readString();
                    byte[] bArrCreateByteArray3 = parcel.createByteArray();
                    String string8 = parcel.readString();
                    byte[] bArrCreateByteArray4 = parcel.createByteArray();
                    KeymasterArguments keymasterArgumentsCreateFromParcel5 = parcel.readInt() != 0 ? KeymasterArguments.CREATOR.createFromParcel(parcel) : null;
                    long j2 = parcel.readLong();
                    long j3 = parcel.readLong();
                    KeyCharacteristics keyCharacteristics5 = new KeyCharacteristics();
                    int iImportWrappedKey = importWrappedKey(string7, bArrCreateByteArray3, string8, bArrCreateByteArray4, keymasterArgumentsCreateFromParcel5, j2, j3, keyCharacteristics5);
                    parcel2.writeNoException();
                    parcel2.writeInt(iImportWrappedKey);
                    parcel2.writeInt(1);
                    keyCharacteristics5.writeToParcel(parcel2, 1);
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iPresentConfirmationPrompt = presentConfirmationPrompt(parcel.readStrongBinder(), parcel.readString(), parcel.createByteArray(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iPresentConfirmationPrompt);
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCancelConfirmationPrompt = cancelConfirmationPrompt(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCancelConfirmationPrompt);
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsConfirmationPromptSupported = isConfirmationPromptSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsConfirmationPromptSupported ? 1 : 0);
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iOnKeyguardVisibilityChanged = onKeyguardVisibilityChanged(parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iOnKeyguardVisibilityChanged);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IKeystoreService {
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
            public int getState(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] get(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int insert(String str, byte[] bArr, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int del(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int exist(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] list(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int reset() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int onUserPasswordChanged(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int lock(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int unlock(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int isEmpty(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int generate(String str, int i, int i2, int i3, int i4, KeystoreArguments keystoreArguments) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    if (keystoreArguments != null) {
                        parcelObtain.writeInt(1);
                        keystoreArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int import_key(String str, byte[] bArr, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] sign(String str, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int verify(String str, byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] get_pubkey(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String grant(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int ungrant(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long getmtime(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int is_hardware_backed(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int clear_uid(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addRngEntropy(byte[] bArr, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int generateKey(String str, KeymasterArguments keymasterArguments, byte[] bArr, int i, int i2, KeyCharacteristics keyCharacteristics) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i3 = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        keyCharacteristics.readFromParcel(parcelObtain2);
                    }
                    return i3;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getKeyCharacteristics(String str, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i, KeyCharacteristics keyCharacteristics) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (keymasterBlob != null) {
                        parcelObtain.writeInt(1);
                        keymasterBlob.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (keymasterBlob2 != null) {
                        parcelObtain.writeInt(1);
                        keymasterBlob2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i2 = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        keyCharacteristics.readFromParcel(parcelObtain2);
                    }
                    return i2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int importKey(String str, KeymasterArguments keymasterArguments, int i, byte[] bArr, int i2, int i3, KeyCharacteristics keyCharacteristics) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i4 = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        keyCharacteristics.readFromParcel(parcelObtain2);
                    }
                    return i4;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ExportResult exportKey(String str, int i, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i2) throws RemoteException {
                ExportResult exportResultCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (keymasterBlob != null) {
                        parcelObtain.writeInt(1);
                        keymasterBlob.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (keymasterBlob2 != null) {
                        parcelObtain.writeInt(1);
                        keymasterBlob2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        exportResultCreateFromParcel = ExportResult.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        exportResultCreateFromParcel = null;
                    }
                    return exportResultCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public OperationResult begin(IBinder iBinder, String str, int i, boolean z, KeymasterArguments keymasterArguments, byte[] bArr, int i2) throws RemoteException {
                OperationResult operationResultCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        operationResultCreateFromParcel = OperationResult.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        operationResultCreateFromParcel = null;
                    }
                    return operationResultCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public OperationResult update(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr) throws RemoteException {
                OperationResult operationResultCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        operationResultCreateFromParcel = OperationResult.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        operationResultCreateFromParcel = null;
                    }
                    return operationResultCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public OperationResult finish(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr, byte[] bArr2) throws RemoteException {
                OperationResult operationResultCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        operationResultCreateFromParcel = OperationResult.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        operationResultCreateFromParcel = null;
                    }
                    return operationResultCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int abort(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isOperationAuthorized(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addAuthToken(byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int onUserAdded(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int onUserRemoved(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int attestKey(String str, KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        keymasterCertificateChain.readFromParcel(parcelObtain2);
                    }
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int attestDeviceIds(KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        keymasterCertificateChain.readFromParcel(parcelObtain2);
                    }
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int onDeviceOffBody() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int importWrappedKey(String str, byte[] bArr, String str2, byte[] bArr2, KeymasterArguments keymasterArguments, long j, long j2, KeyCharacteristics keyCharacteristics) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeByteArray(bArr2);
                    if (keymasterArguments != null) {
                        parcelObtain.writeInt(1);
                        keymasterArguments.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeLong(j);
                    parcelObtain.writeLong(j2);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    int i = parcelObtain2.readInt();
                    if (parcelObtain2.readInt() != 0) {
                        keyCharacteristics.readFromParcel(parcelObtain2);
                    }
                    return i;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int presentConfirmationPrompt(IBinder iBinder, String str, byte[] bArr, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int cancelConfirmationPrompt(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isConfirmationPromptSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int onKeyguardVisibilityChanged(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
