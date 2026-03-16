package android.security;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.security.IKeystoreService;
import android.util.Log;
import java.util.Locale;

public class KeyStore {
    public static final int FLAG_ENCRYPTED = 1;
    public static final int FLAG_NONE = 0;
    public static final int KEY_NOT_FOUND = 7;
    public static final int LOCKED = 2;
    public static final int NO_ERROR = 1;
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
    private int mError = 1;

    public enum State {
        UNLOCKED,
        LOCKED,
        UNINITIALIZED
    }

    private KeyStore(IKeystoreService binder) {
        this.mBinder = binder;
    }

    public static KeyStore getInstance() {
        IKeystoreService keystore = IKeystoreService.Stub.asInterface(ServiceManager.getService("android.security.keystore"));
        return new KeyStore(keystore);
    }

    static int getKeyTypeForAlgorithm(String keyType) throws IllegalArgumentException {
        if ("RSA".equalsIgnoreCase(keyType)) {
            return 6;
        }
        if ("DSA".equalsIgnoreCase(keyType)) {
            return 116;
        }
        if ("EC".equalsIgnoreCase(keyType)) {
            return 408;
        }
        throw new IllegalArgumentException("Unsupported key type: " + keyType);
    }

    public State state() {
        try {
            int ret = this.mBinder.test();
            switch (ret) {
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

    public boolean isUnlocked() {
        return state() == State.UNLOCKED;
    }

    public byte[] get(String key) {
        try {
            return this.mBinder.get(key);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean put(String key, byte[] value, int uid, int flags) {
        try {
            return this.mBinder.insert(key, value, uid, flags) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delete(String key, int uid) {
        try {
            return this.mBinder.del(key, uid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delete(String key) {
        return delete(key, -1);
    }

    public boolean contains(String key, int uid) {
        try {
            return this.mBinder.exist(key, uid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean contains(String key) {
        return contains(key, -1);
    }

    public String[] saw(String prefix, int uid) {
        try {
            return this.mBinder.saw(prefix, uid);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public String[] saw(String prefix) {
        return saw(prefix, -1);
    }

    public boolean reset() {
        try {
            return this.mBinder.reset() == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean password(String password) {
        try {
            return this.mBinder.password(password) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean lock() {
        try {
            return this.mBinder.lock() == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean unlock(String password) {
        try {
            this.mError = this.mBinder.unlock(password);
            return this.mError == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean isEmpty() {
        try {
            return this.mBinder.zero() == 7;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean generate(String key, int uid, int keyType, int keySize, int flags, byte[][] args) {
        try {
            return this.mBinder.generate(key, uid, keyType, keySize, flags, args) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean importKey(String keyName, byte[] key, int uid, int flags) {
        try {
            return this.mBinder.import_key(keyName, key, uid, flags) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public byte[] getPubkey(String key) {
        try {
            return this.mBinder.get_pubkey(key);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean delKey(String key, int uid) {
        try {
            return this.mBinder.del_key(key, uid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean delKey(String key) {
        return delKey(key, -1);
    }

    public byte[] sign(String key, byte[] data) {
        try {
            return this.mBinder.sign(key, data);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return null;
        }
    }

    public boolean verify(String key, byte[] data, byte[] signature) {
        try {
            return this.mBinder.verify(key, data, signature) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean grant(String key, int uid) {
        try {
            return this.mBinder.grant(key, uid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean ungrant(String key, int uid) {
        try {
            return this.mBinder.ungrant(key, uid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public long getmtime(String key) {
        try {
            long millis = this.mBinder.getmtime(key);
            if (millis == -1) {
                return -1L;
            }
            return 1000 * millis;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return -1L;
        }
    }

    public boolean duplicate(String srcKey, int srcUid, String destKey, int destUid) {
        try {
            return this.mBinder.duplicate(srcKey, srcUid, destKey, destUid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean isHardwareBacked() {
        return isHardwareBacked("RSA");
    }

    public boolean isHardwareBacked(String keyType) {
        try {
            return this.mBinder.is_hardware_backed(keyType.toUpperCase(Locale.US)) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean clearUid(int uid) {
        try {
            return this.mBinder.clear_uid((long) uid) == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean resetUid(int uid) {
        try {
            this.mError = this.mBinder.reset_uid(uid);
            return this.mError == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean syncUid(int sourceUid, int targetUid) {
        try {
            this.mError = this.mBinder.sync_uid(sourceUid, targetUid);
            return this.mError == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public boolean passwordUid(String password, int uid) {
        try {
            this.mError = this.mBinder.password_uid(password, uid);
            return this.mError == 1;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to keystore", e);
            return false;
        }
    }

    public int getLastError() {
        return this.mError;
    }
}
