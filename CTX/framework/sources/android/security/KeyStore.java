package android.security;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.security.IKeystoreService;
import android.security.keymaster.ExportResult;
import android.security.keymaster.KeyCharacteristics;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterBlob;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keymaster.KeymasterDefs;
import android.security.keymaster.OperationResult;
import android.security.keystore.KeyExpiredException;
import android.security.keystore.KeyNotYetValidException;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Locale;

public class KeyStore {
    public static final int CANNOT_ATTEST_IDS = -66;
    public static final int CONFIRMATIONUI_ABORTED = 2;
    public static final int CONFIRMATIONUI_CANCELED = 1;
    public static final int CONFIRMATIONUI_IGNORED = 4;
    public static final int CONFIRMATIONUI_OK = 0;
    public static final int CONFIRMATIONUI_OPERATION_PENDING = 3;
    public static final int CONFIRMATIONUI_SYSTEM_ERROR = 5;
    public static final int CONFIRMATIONUI_UIERROR = 65536;
    public static final int CONFIRMATIONUI_UIERROR_MALFORMED_UTF8_ENCODING = 65539;
    public static final int CONFIRMATIONUI_UIERROR_MESSAGE_TOO_LONG = 65538;
    public static final int CONFIRMATIONUI_UIERROR_MISSING_GLYPH = 65537;
    public static final int CONFIRMATIONUI_UNEXPECTED = 7;
    public static final int CONFIRMATIONUI_UNIMPLEMENTED = 6;
    public static final int FLAG_CRITICAL_TO_DEVICE_ENCRYPTION = 8;
    public static final int FLAG_ENCRYPTED = 1;
    public static final int FLAG_NONE = 0;
    public static final int FLAG_SOFTWARE = 2;
    public static final int FLAG_STRONGBOX = 16;
    public static final int HARDWARE_TYPE_UNAVAILABLE = -68;
    public static final int KEY_NOT_FOUND = 7;
    public static final int LOCKED = 2;
    public static final int NO_ERROR = 1;
    public static final int OP_AUTH_NEEDED = 15;
    public static final int PERMISSION_DENIED = 6;
    public static final int PROTOCOL_ERROR = 5;
    public static final int SYSTEM_ERROR = 4;
    private static final String TAG = "KeyStore";
    public static final int UID_SELF = -1;
    public static final int UNDEFINED_ACTION = 9;
    public static final int UNINITIALIZED = 3;
    public static final int VALUE_CORRUPTED = 8;
    public static final int WRONG_PASSWORD = 10;
    private final IKeystoreService mBinder;
    private IBinder mToken;
    private int mError = 1;
    private final Context mContext = getApplicationContext();

    public enum State {
        UNLOCKED,
        LOCKED,
        UNINITIALIZED
    }

    private KeyStore(IKeystoreService iKeystoreService) {
        this.mBinder = iKeystoreService;
    }

    public static Context getApplicationContext() {
        Application applicationCurrentApplication = ActivityThread.currentApplication();
        if (applicationCurrentApplication == null) {
            throw new IllegalStateException("Failed to obtain application Context from ActivityThread");
        }
        return applicationCurrentApplication;
    }

    public static KeyStore getInstance() {
        return new KeyStore(IKeystoreService.Stub.asInterface(ServiceManager.getService("android.security.keystore")));
    }

    private synchronized IBinder getToken() {
        if (this.mToken == null) {
            this.mToken = new Binder();
        }
        return this.mToken;
    }

    public State state(int i) {
        try {
            switch (this.mBinder.getState(i)) {
                case 1:
                    return State.UNLOCKED;
                case 2:
                    return State.LOCKED;
                case 3:
                    return State.UNINITIALIZED;
                default:
                    throw new AssertionError(this.mError);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            throw new AssertionError(e);
        }
    }

    public State state() {
        return state(UserHandle.myUserId());
    }

    public boolean isUnlocked() {
        return state() == State.UNLOCKED;
    }

    public byte[] get(String str, int i) {
        if (str == null) {
            str = "";
        }
        try {
            return this.mBinder.get(str, i);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        } catch (ServiceSpecificException e2) {
            Log.w(TAG, "KeyStore exception", e2);
            return null;
        }
    }

    public byte[] get(String str) {
        return get(str, -1);
    }

    public boolean put(String str, byte[] bArr, int i, int i2) {
        return insert(str, bArr, i, i2) == 1;
    }

    public int insert(String str, byte[] bArr, int i, int i2) {
        if (bArr == null) {
            try {
                bArr = new byte[0];
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return 4;
            }
        }
        return this.mBinder.insert(str, bArr, i, i2);
    }

    public boolean delete(String str, int i) {
        try {
            int iDel = this.mBinder.del(str, i);
            if (iDel != 1 && iDel != 7) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delete(String str) {
        return delete(str, -1);
    }

    public boolean contains(String str, int i) {
        try {
            return this.mBinder.exist(str, i) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean contains(String str) {
        return contains(str, -1);
    }

    public String[] list(String str, int i) {
        try {
            return this.mBinder.list(str, i);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        } catch (ServiceSpecificException e2) {
            Log.w(TAG, "KeyStore exception", e2);
            return null;
        }
    }

    public String[] list(String str) {
        return list(str, -1);
    }

    public boolean reset() {
        try {
            return this.mBinder.reset() == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean lock(int i) {
        try {
            return this.mBinder.lock(i) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean lock() {
        return lock(UserHandle.myUserId());
    }

    public boolean unlock(int i, String str) {
        if (str == null) {
            str = "";
        }
        try {
            this.mError = this.mBinder.unlock(i, str);
            return this.mError == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean unlock(String str) {
        return unlock(UserHandle.getUserId(Process.myUid()), str);
    }

    public boolean isEmpty(int i) {
        try {
            return this.mBinder.isEmpty(i) != 0;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean isEmpty() {
        return isEmpty(UserHandle.myUserId());
    }

    public boolean generate(String str, int i, int i2, int i3, int i4, byte[][] bArr) {
        try {
            return this.mBinder.generate(str, i, i2, i3, i4, new KeystoreArguments(bArr)) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean importKey(String str, byte[] bArr, int i, int i2) {
        try {
            return this.mBinder.import_key(str, bArr, i, i2) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public byte[] sign(String str, byte[] bArr) {
        try {
            return this.mBinder.sign(str, bArr);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        } catch (ServiceSpecificException e2) {
            Log.w(TAG, "KeyStore exception", e2);
            return null;
        }
    }

    public boolean verify(String str, byte[] bArr, byte[] bArr2) {
        if (bArr2 == null) {
            try {
                bArr2 = new byte[0];
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return false;
            } catch (ServiceSpecificException e2) {
                Log.w(TAG, "KeyStore exception", e2);
                return false;
            }
        }
        return this.mBinder.verify(str, bArr, bArr2) == 1;
    }

    public String grant(String str, int i) {
        try {
            String strGrant = this.mBinder.grant(str, i);
            if (strGrant == "") {
                return null;
            }
            return strGrant;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean ungrant(String str, int i) {
        try {
            return this.mBinder.ungrant(str, i) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public long getmtime(String str, int i) {
        try {
            long j = this.mBinder.getmtime(str, i);
            if (j == -1) {
                return -1L;
            }
            return j * 1000;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return -1L;
        }
    }

    public long getmtime(String str) {
        return getmtime(str, -1);
    }

    public boolean isHardwareBacked() {
        return isHardwareBacked(KeyProperties.KEY_ALGORITHM_RSA);
    }

    public boolean isHardwareBacked(String str) {
        try {
            return this.mBinder.is_hardware_backed(str.toUpperCase(Locale.US)) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean clearUid(int i) {
        try {
            return this.mBinder.clear_uid((long) i) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public int getLastError() {
        return this.mError;
    }

    public boolean addRngEntropy(byte[] bArr, int i) {
        try {
            return this.mBinder.addRngEntropy(bArr, i) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public int generateKey(String str, KeymasterArguments keymasterArguments, byte[] bArr, int i, int i2, KeyCharacteristics keyCharacteristics) {
        if (bArr == null) {
            try {
                bArr = new byte[0];
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return 4;
            }
        }
        byte[] bArr2 = bArr;
        if (keymasterArguments == null) {
            keymasterArguments = new KeymasterArguments();
        }
        return this.mBinder.generateKey(str, keymasterArguments, bArr2, i, i2, keyCharacteristics);
    }

    public int generateKey(String str, KeymasterArguments keymasterArguments, byte[] bArr, int i, KeyCharacteristics keyCharacteristics) {
        return generateKey(str, keymasterArguments, bArr, -1, i, keyCharacteristics);
    }

    public int getKeyCharacteristics(String str, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i, KeyCharacteristics keyCharacteristics) {
        if (keymasterBlob == null) {
            try {
                keymasterBlob = new KeymasterBlob(new byte[0]);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return 4;
            }
        }
        KeymasterBlob keymasterBlob3 = keymasterBlob;
        if (keymasterBlob2 == null) {
            keymasterBlob2 = new KeymasterBlob(new byte[0]);
        }
        return this.mBinder.getKeyCharacteristics(str, keymasterBlob3, keymasterBlob2, i, keyCharacteristics);
    }

    public int getKeyCharacteristics(String str, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, KeyCharacteristics keyCharacteristics) {
        return getKeyCharacteristics(str, keymasterBlob, keymasterBlob2, -1, keyCharacteristics);
    }

    public int importKey(String str, KeymasterArguments keymasterArguments, int i, byte[] bArr, int i2, int i3, KeyCharacteristics keyCharacteristics) {
        try {
            return this.mBinder.importKey(str, keymasterArguments, i, bArr, i2, i3, keyCharacteristics);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return 4;
        }
    }

    public int importKey(String str, KeymasterArguments keymasterArguments, int i, byte[] bArr, int i2, KeyCharacteristics keyCharacteristics) {
        return importKey(str, keymasterArguments, i, bArr, -1, i2, keyCharacteristics);
    }

    public int importWrappedKey(String str, byte[] bArr, String str2, byte[] bArr2, KeymasterArguments keymasterArguments, long j, long j2, int i, KeyCharacteristics keyCharacteristics) {
        try {
            return this.mBinder.importWrappedKey(str, bArr, str2, bArr2, keymasterArguments, j, j2, keyCharacteristics);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return 4;
        }
    }

    public ExportResult exportKey(String str, int i, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2, int i2) {
        if (keymasterBlob == null) {
            try {
                keymasterBlob = new KeymasterBlob(new byte[0]);
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return null;
            }
        }
        KeymasterBlob keymasterBlob3 = keymasterBlob;
        if (keymasterBlob2 == null) {
            keymasterBlob2 = new KeymasterBlob(new byte[0]);
        }
        return this.mBinder.exportKey(str, i, keymasterBlob3, keymasterBlob2, i2);
    }

    public ExportResult exportKey(String str, int i, KeymasterBlob keymasterBlob, KeymasterBlob keymasterBlob2) {
        return exportKey(str, i, keymasterBlob, keymasterBlob2, -1);
    }

    public OperationResult begin(String str, int i, boolean z, KeymasterArguments keymasterArguments, byte[] bArr, int i2) {
        if (keymasterArguments == null) {
            try {
                keymasterArguments = new KeymasterArguments();
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return null;
            }
        }
        KeymasterArguments keymasterArguments2 = keymasterArguments;
        if (bArr == null) {
            bArr = new byte[0];
        }
        return this.mBinder.begin(getToken(), str, i, z, keymasterArguments2, bArr, i2);
    }

    public OperationResult begin(String str, int i, boolean z, KeymasterArguments keymasterArguments, byte[] bArr) {
        if (bArr == null) {
            bArr = new byte[0];
        }
        byte[] bArr2 = bArr;
        if (keymasterArguments == null) {
            keymasterArguments = new KeymasterArguments();
        }
        return begin(str, i, z, keymasterArguments, bArr2, -1);
    }

    public OperationResult update(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr) {
        if (keymasterArguments == null) {
            try {
                keymasterArguments = new KeymasterArguments();
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return null;
            }
        }
        if (bArr == null) {
            bArr = new byte[0];
        }
        return this.mBinder.update(iBinder, keymasterArguments, bArr);
    }

    public OperationResult finish(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr, byte[] bArr2) {
        if (keymasterArguments == null) {
            try {
                keymasterArguments = new KeymasterArguments();
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return null;
            }
        }
        if (bArr2 == null) {
            bArr2 = new byte[0];
        }
        if (bArr == null) {
            bArr = new byte[0];
        }
        return this.mBinder.finish(iBinder, keymasterArguments, bArr, bArr2);
    }

    public OperationResult finish(IBinder iBinder, KeymasterArguments keymasterArguments, byte[] bArr) {
        return finish(iBinder, keymasterArguments, bArr, null);
    }

    public int abort(IBinder iBinder) {
        try {
            return this.mBinder.abort(iBinder);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return 4;
        }
    }

    public boolean isOperationAuthorized(IBinder iBinder) {
        try {
            return this.mBinder.isOperationAuthorized(iBinder);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public int addAuthToken(byte[] bArr) {
        try {
            return this.mBinder.addAuthToken(bArr);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return 4;
        }
    }

    public boolean onUserPasswordChanged(int i, String str) {
        if (str == null) {
            str = "";
        }
        try {
            return this.mBinder.onUserPasswordChanged(i, str) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public void onUserAdded(int i, int i2) {
        try {
            this.mBinder.onUserAdded(i, i2);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
        }
    }

    public void onUserAdded(int i) {
        onUserAdded(i, -1);
    }

    public void onUserRemoved(int i) {
        try {
            this.mBinder.onUserRemoved(i);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
        }
    }

    public boolean onUserPasswordChanged(String str) {
        return onUserPasswordChanged(UserHandle.getUserId(Process.myUid()), str);
    }

    public void onUserLockedStateChanged(int i, boolean z) {
        try {
            this.mBinder.onKeyguardVisibilityChanged(z, i);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to update user locked state " + i, e);
        }
    }

    public int attestKey(String str, KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) {
        if (keymasterArguments == null) {
            try {
                keymasterArguments = new KeymasterArguments();
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return 4;
            }
        }
        if (keymasterCertificateChain == null) {
            keymasterCertificateChain = new KeymasterCertificateChain();
        }
        return this.mBinder.attestKey(str, keymasterArguments, keymasterCertificateChain);
    }

    public int attestDeviceIds(KeymasterArguments keymasterArguments, KeymasterCertificateChain keymasterCertificateChain) {
        if (keymasterArguments == null) {
            try {
                keymasterArguments = new KeymasterArguments();
            } catch (RemoteException e) {
                Log.w(TAG, "Cannot connect to keystore", e);
                return 4;
            }
        }
        if (keymasterCertificateChain == null) {
            keymasterCertificateChain = new KeymasterCertificateChain();
        }
        return this.mBinder.attestDeviceIds(keymasterArguments, keymasterCertificateChain);
    }

    public void onDeviceOffBody() {
        try {
            this.mBinder.onDeviceOffBody();
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
        }
    }

    public int presentConfirmationPrompt(IBinder iBinder, String str, byte[] bArr, String str2, int i) {
        try {
            return this.mBinder.presentConfirmationPrompt(iBinder, str, bArr, str2, i);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return 5;
        }
    }

    public int cancelConfirmationPrompt(IBinder iBinder) {
        try {
            return this.mBinder.cancelConfirmationPrompt(iBinder);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return 5;
        }
    }

    public boolean isConfirmationPromptSupported() {
        try {
            return this.mBinder.isConfirmationPromptSupported();
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public static KeyStoreException getKeyStoreException(int i) {
        if (i > 0) {
            if (i != 15) {
                switch (i) {
                    case 1:
                        return new KeyStoreException(i, "OK");
                    case 2:
                        return new KeyStoreException(i, "User authentication required");
                    case 3:
                        return new KeyStoreException(i, "Keystore not initialized");
                    case 4:
                        return new KeyStoreException(i, "System error");
                    default:
                        switch (i) {
                            case 6:
                                return new KeyStoreException(i, "Permission denied");
                            case 7:
                                return new KeyStoreException(i, "Key not found");
                            case 8:
                                return new KeyStoreException(i, "Key blob corrupted");
                            default:
                                return new KeyStoreException(i, String.valueOf(i));
                        }
                }
            }
            return new KeyStoreException(i, "Operation requires authorization");
        }
        if (i == -16) {
            return new KeyStoreException(i, "Invalid user authentication validity duration");
        }
        return new KeyStoreException(i, KeymasterDefs.getErrorMessage(i));
    }

    public InvalidKeyException getInvalidKeyException(String str, int i, KeyStoreException keyStoreException) {
        int errorCode = keyStoreException.getErrorCode();
        if (errorCode != 15) {
            switch (errorCode) {
                case -26:
                    break;
                case -25:
                    return new KeyExpiredException();
                case -24:
                    return new KeyNotYetValidException();
                default:
                    switch (errorCode) {
                        case 2:
                            return new UserNotAuthenticatedException();
                        case 3:
                            return new KeyPermanentlyInvalidatedException();
                        default:
                            return new InvalidKeyException("Keystore operation failed", keyStoreException);
                    }
            }
        }
        KeyCharacteristics keyCharacteristics = new KeyCharacteristics();
        int keyCharacteristics2 = getKeyCharacteristics(str, null, null, i, keyCharacteristics);
        if (keyCharacteristics2 != 1) {
            return new InvalidKeyException("Failed to obtained key characteristics", getKeyStoreException(keyCharacteristics2));
        }
        List<BigInteger> unsignedLongs = keyCharacteristics.getUnsignedLongs(KeymasterDefs.KM_TAG_USER_SECURE_ID);
        if (unsignedLongs.isEmpty()) {
            return new KeyPermanentlyInvalidatedException();
        }
        long secureUserId = GateKeeper.getSecureUserId();
        if (secureUserId != 0 && unsignedLongs.contains(KeymasterArguments.toUint64(secureUserId))) {
            return new UserNotAuthenticatedException();
        }
        long fingerprintOnlySid = getFingerprintOnlySid();
        if (fingerprintOnlySid != 0 && unsignedLongs.contains(KeymasterArguments.toUint64(fingerprintOnlySid))) {
            return new UserNotAuthenticatedException();
        }
        return new KeyPermanentlyInvalidatedException();
    }

    private long getFingerprintOnlySid() {
        FingerprintManager fingerprintManager;
        if (this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) && (fingerprintManager = (FingerprintManager) this.mContext.getSystemService(FingerprintManager.class)) != null) {
            return fingerprintManager.getAuthenticatorId();
        }
        return 0L;
    }

    public InvalidKeyException getInvalidKeyException(String str, int i, int i2) {
        return getInvalidKeyException(str, i, getKeyStoreException(i2));
    }
}
