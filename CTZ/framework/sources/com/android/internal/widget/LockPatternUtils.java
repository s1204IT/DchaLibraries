package com.android.internal.widget;

import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.app.trust.IStrongAuthTracker;
import android.app.trust.TrustManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternView;
import com.android.server.LocalServices;
import com.google.android.collect.Lists;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import libcore.util.HexEncoding;

public class LockPatternUtils {

    @Deprecated
    public static final String BIOMETRIC_WEAK_EVER_CHOSEN_KEY = "lockscreen.biometricweakeverchosen";
    public static final int CREDENTIAL_TYPE_NONE = -1;
    public static final int CREDENTIAL_TYPE_PASSWORD = 2;
    public static final int CREDENTIAL_TYPE_PATTERN = 1;
    private static final boolean DEBUG = false;
    public static final String DISABLE_LOCKSCREEN_KEY = "lockscreen.disabled";
    private static final String ENABLED_TRUST_AGENTS = "lockscreen.enabledtrustagents";
    public static final int FAILED_ATTEMPTS_BEFORE_RESET = 20;
    public static final int FAILED_ATTEMPTS_BEFORE_WIPE_GRACE = 5;
    public static final long FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS = 1000;
    private static final boolean FRP_CREDENTIAL_ENABLED = true;
    private static final String HISTORY_DELIMITER = ",";
    private static final String IS_TRUST_USUALLY_MANAGED = "lockscreen.istrustusuallymanaged";
    public static final String LEGACY_LOCK_PATTERN_ENABLED = "legacy_lock_pattern_enabled";

    @Deprecated
    public static final String LOCKOUT_PERMANENT_KEY = "lockscreen.lockedoutpermanently";

    @Deprecated
    public static final String LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK = "lockscreen.biometric_weak_fallback";
    public static final String LOCKSCREEN_OPTIONS = "lockscreen.options";
    public static final String LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS = "lockscreen.power_button_instantly_locks";

    @Deprecated
    public static final String LOCKSCREEN_WIDGETS_ENABLED = "lockscreen.widgets_enabled";
    public static final String LOCK_PASSWORD_SALT_KEY = "lockscreen.password_salt";
    private static final String LOCK_SCREEN_DEVICE_OWNER_INFO = "lockscreen.device_owner_info";
    private static final String LOCK_SCREEN_OWNER_INFO = "lock_screen_owner_info";
    private static final String LOCK_SCREEN_OWNER_INFO_ENABLED = "lock_screen_owner_info_enabled";
    public static final int MIN_LOCK_PASSWORD_SIZE = 4;
    public static final int MIN_LOCK_PATTERN_SIZE = 4;
    public static final int MIN_PATTERN_REGISTER_FAIL = 4;
    public static final String PASSWORD_HISTORY_KEY = "lockscreen.passwordhistory";

    @Deprecated
    public static final String PASSWORD_TYPE_ALTERNATE_KEY = "lockscreen.password_type_alternate";
    public static final String PASSWORD_TYPE_KEY = "lockscreen.password_type";
    public static final String PATTERN_EVER_CHOSEN_KEY = "lockscreen.patterneverchosen";
    public static final String PROFILE_KEY_NAME_DECRYPT = "profile_key_name_decrypt_";
    public static final String PROFILE_KEY_NAME_ENCRYPT = "profile_key_name_encrypt_";
    public static final String SYNTHETIC_PASSWORD_ENABLED_KEY = "enable-sp";
    public static final String SYNTHETIC_PASSWORD_HANDLE_KEY = "sp-handle";
    public static final String SYNTHETIC_PASSWORD_KEY_PREFIX = "synthetic_password_";
    private static final String TAG = "LockPatternUtils";
    public static final int USER_FRP = -9999;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private DevicePolicyManager mDevicePolicyManager;
    private final Handler mHandler;
    private ILockSettings mLockSettingsService;
    private final SparseLongArray mLockoutDeadlines = new SparseLongArray();
    private UserManager mUserManager;

    public interface CheckCredentialProgressCallback {
        void onEarlyMatched();
    }

    public boolean isTrustUsuallyManaged(int i) {
        if (!(this.mLockSettingsService instanceof ILockSettings.Stub)) {
            throw new IllegalStateException("May only be called by TrustManagerService. Use TrustManager.isTrustUsuallyManaged()");
        }
        try {
            return getLockSettings().getBoolean(IS_TRUST_USUALLY_MANAGED, false, i);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setTrustUsuallyManaged(boolean z, int i) {
        try {
            getLockSettings().setBoolean(IS_TRUST_USUALLY_MANAGED, z, i);
        } catch (RemoteException e) {
        }
    }

    public void userPresent(int i) {
        try {
            getLockSettings().userPresent(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static final class RequestThrottledException extends Exception {
        private int mTimeoutMs;

        public RequestThrottledException(int i) {
            this.mTimeoutMs = i;
        }

        public int getTimeoutMs() {
            return this.mTimeoutMs;
        }
    }

    public DevicePolicyManager getDevicePolicyManager() {
        if (this.mDevicePolicyManager == null) {
            this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (this.mDevicePolicyManager == null) {
                Log.e(TAG, "Can't get DevicePolicyManagerService: is it running?", new IllegalStateException("Stack trace:"));
            }
        }
        return this.mDevicePolicyManager;
    }

    private UserManager getUserManager() {
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(this.mContext);
        }
        return this.mUserManager;
    }

    private TrustManager getTrustManager() {
        TrustManager trustManager = (TrustManager) this.mContext.getSystemService(Context.TRUST_SERVICE);
        if (trustManager == null) {
            Log.e(TAG, "Can't get TrustManagerService: is it running?", new IllegalStateException("Stack trace:"));
        }
        return trustManager;
    }

    public LockPatternUtils(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        Looper looperMyLooper = Looper.myLooper();
        this.mHandler = looperMyLooper != null ? new Handler(looperMyLooper) : null;
    }

    @VisibleForTesting
    public ILockSettings getLockSettings() {
        if (this.mLockSettingsService == null) {
            this.mLockSettingsService = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
        }
        return this.mLockSettingsService;
    }

    public int getRequestedMinimumPasswordLength(int i) {
        return getDevicePolicyManager().getPasswordMinimumLength(null, i);
    }

    public int getRequestedPasswordQuality(int i) {
        return getDevicePolicyManager().getPasswordQuality(null, i);
    }

    private int getRequestedPasswordHistoryLength(int i) {
        return getDevicePolicyManager().getPasswordHistoryLength(null, i);
    }

    public int getRequestedPasswordMinimumLetters(int i) {
        return getDevicePolicyManager().getPasswordMinimumLetters(null, i);
    }

    public int getRequestedPasswordMinimumUpperCase(int i) {
        return getDevicePolicyManager().getPasswordMinimumUpperCase(null, i);
    }

    public int getRequestedPasswordMinimumLowerCase(int i) {
        return getDevicePolicyManager().getPasswordMinimumLowerCase(null, i);
    }

    public int getRequestedPasswordMinimumNumeric(int i) {
        return getDevicePolicyManager().getPasswordMinimumNumeric(null, i);
    }

    public int getRequestedPasswordMinimumSymbols(int i) {
        return getDevicePolicyManager().getPasswordMinimumSymbols(null, i);
    }

    public int getRequestedPasswordMinimumNonLetter(int i) {
        return getDevicePolicyManager().getPasswordMinimumNonLetter(null, i);
    }

    public void reportFailedPasswordAttempt(int i) {
        if (i == -9999 && frpCredentialEnabled(this.mContext)) {
            return;
        }
        getDevicePolicyManager().reportFailedPasswordAttempt(i);
        getTrustManager().reportUnlockAttempt(false, i);
    }

    public void reportSuccessfulPasswordAttempt(int i) {
        if (i == -9999 && frpCredentialEnabled(this.mContext)) {
            return;
        }
        getDevicePolicyManager().reportSuccessfulPasswordAttempt(i);
        getTrustManager().reportUnlockAttempt(true, i);
    }

    public void reportPasswordLockout(int i, int i2) {
        if (i2 == -9999 && frpCredentialEnabled(this.mContext)) {
            return;
        }
        getTrustManager().reportUnlockLockout(i, i2);
    }

    public int getCurrentFailedPasswordAttempts(int i) {
        if (i == -9999 && frpCredentialEnabled(this.mContext)) {
            return 0;
        }
        return getDevicePolicyManager().getCurrentFailedPasswordAttempts(i);
    }

    public int getMaximumFailedPasswordsForWipe(int i) {
        if (i == -9999 && frpCredentialEnabled(this.mContext)) {
            return 0;
        }
        return getDevicePolicyManager().getMaximumFailedPasswordsForWipe(null, i);
    }

    private byte[] verifyCredential(String str, int i, long j, int i2) throws RequestThrottledException {
        try {
            VerifyCredentialResponse verifyCredentialResponseVerifyCredential = getLockSettings().verifyCredential(str, i, j, i2);
            if (verifyCredentialResponseVerifyCredential.getResponseCode() == 0) {
                return verifyCredentialResponseVerifyCredential.getPayload();
            }
            if (verifyCredentialResponseVerifyCredential.getResponseCode() != 1) {
                return null;
            }
            throw new RequestThrottledException(verifyCredentialResponseVerifyCredential.getTimeout());
        } catch (RemoteException e) {
            return null;
        }
    }

    private boolean checkCredential(String str, int i, int i2, CheckCredentialProgressCallback checkCredentialProgressCallback) throws RequestThrottledException {
        try {
            VerifyCredentialResponse verifyCredentialResponseCheckCredential = getLockSettings().checkCredential(str, i, i2, wrapCallback(checkCredentialProgressCallback));
            if (verifyCredentialResponseCheckCredential.getResponseCode() == 0) {
                return true;
            }
            if (verifyCredentialResponseCheckCredential.getResponseCode() != 1) {
                return false;
            }
            throw new RequestThrottledException(verifyCredentialResponseCheckCredential.getTimeout());
        } catch (RemoteException e) {
            return false;
        }
    }

    public byte[] verifyPattern(List<LockPatternView.Cell> list, long j, int i) throws RequestThrottledException {
        throwIfCalledOnMainThread();
        return verifyCredential(patternToString(list), 1, j, i);
    }

    public boolean checkPattern(List<LockPatternView.Cell> list, int i) throws RequestThrottledException {
        return checkPattern(list, i, null);
    }

    public boolean checkPattern(List<LockPatternView.Cell> list, int i, CheckCredentialProgressCallback checkCredentialProgressCallback) throws RequestThrottledException {
        throwIfCalledOnMainThread();
        return checkCredential(patternToString(list), 1, i, checkCredentialProgressCallback);
    }

    public byte[] verifyPassword(String str, long j, int i) throws RequestThrottledException {
        throwIfCalledOnMainThread();
        return verifyCredential(str, 2, j, i);
    }

    public byte[] verifyTiedProfileChallenge(String str, boolean z, long j, int i) throws RequestThrottledException {
        throwIfCalledOnMainThread();
        try {
            VerifyCredentialResponse verifyCredentialResponseVerifyTiedProfileChallenge = getLockSettings().verifyTiedProfileChallenge(str, z ? 1 : 2, j, i);
            if (verifyCredentialResponseVerifyTiedProfileChallenge.getResponseCode() != 0) {
                if (verifyCredentialResponseVerifyTiedProfileChallenge.getResponseCode() != 1) {
                    return null;
                }
                throw new RequestThrottledException(verifyCredentialResponseVerifyTiedProfileChallenge.getTimeout());
            }
            return verifyCredentialResponseVerifyTiedProfileChallenge.getPayload();
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean checkPassword(String str, int i) throws RequestThrottledException {
        return checkPassword(str, i, null);
    }

    public boolean checkPassword(String str, int i, CheckCredentialProgressCallback checkCredentialProgressCallback) throws RequestThrottledException {
        throwIfCalledOnMainThread();
        return checkCredential(str, 2, i, checkCredentialProgressCallback);
    }

    public boolean checkVoldPassword(int i) {
        try {
            return getLockSettings().checkVoldPassword(i);
        } catch (RemoteException e) {
            return false;
        }
    }

    public byte[] getPasswordHistoryHashFactor(String str, int i) {
        try {
            return getLockSettings().getHashFactor(str, i);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to get hash factor", e);
            return null;
        }
    }

    public boolean checkPasswordHistory(String str, byte[] bArr, int i) {
        int requestedPasswordHistoryLength;
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "checkPasswordHistory: empty password");
            return false;
        }
        String string = getString(PASSWORD_HISTORY_KEY, i);
        if (TextUtils.isEmpty(string) || (requestedPasswordHistoryLength = getRequestedPasswordHistoryLength(i)) == 0) {
            return false;
        }
        String strLegacyPasswordToHash = legacyPasswordToHash(str, i);
        String strPasswordToHistoryHash = passwordToHistoryHash(str, bArr, i);
        String[] strArrSplit = string.split(HISTORY_DELIMITER);
        for (int i2 = 0; i2 < Math.min(requestedPasswordHistoryLength, strArrSplit.length); i2++) {
            if (strArrSplit[i2].equals(strLegacyPasswordToHash) || strArrSplit[i2].equals(strPasswordToHistoryHash)) {
                return true;
            }
        }
        return false;
    }

    private boolean savedPatternExists(int i) {
        try {
            return getLockSettings().havePattern(i);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean savedPasswordExists(int i) {
        try {
            return getLockSettings().havePassword(i);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isPatternEverChosen(int i) {
        return getBoolean(PATTERN_EVER_CHOSEN_KEY, false, i);
    }

    public void reportPatternWasChosen(int i) {
        setBoolean(PATTERN_EVER_CHOSEN_KEY, true, i);
    }

    public int getActivePasswordQuality(int i) {
        int keyguardStoredPasswordQuality = getKeyguardStoredPasswordQuality(i);
        if (isLockPasswordEnabled(keyguardStoredPasswordQuality, i) || isLockPatternEnabled(keyguardStoredPasswordQuality, i)) {
            return keyguardStoredPasswordQuality;
        }
        return 0;
    }

    public void resetKeyStore(int i) {
        try {
            getLockSettings().resetKeyStore(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't reset keystore " + e);
        }
    }

    public void clearLock(String str, int i) {
        int keyguardStoredPasswordQuality = getKeyguardStoredPasswordQuality(i);
        setKeyguardStoredPasswordQuality(0, i);
        try {
            getLockSettings().setLockCredential(null, -1, str, 0, i);
            if (i == 0) {
                updateEncryptionPassword(1, null);
                setCredentialRequiredToDecrypt(false);
            }
            onAfterChangingPassword(i);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear lock", e);
            setKeyguardStoredPasswordQuality(keyguardStoredPasswordQuality, i);
        }
    }

    public void setLockScreenDisabled(boolean z, int i) {
        setBoolean("lockscreen.disabled", z, i);
    }

    public boolean isLockScreenDisabled(int i) {
        if (isSecure(i)) {
            return false;
        }
        boolean z = this.mContext.getResources().getBoolean(R.bool.config_disableLockscreenByDefault);
        boolean z2 = UserManager.isSplitSystemUser() && i == 0;
        UserInfo userInfo = getUserManager().getUserInfo(i);
        return getBoolean("lockscreen.disabled", false, i) || (z && !z2) || (UserManager.isDeviceInDemoMode(this.mContext) && userInfo != null && userInfo.isDemo());
    }

    public void saveLockPattern(List<LockPatternView.Cell> list, int i) {
        saveLockPattern(list, null, i);
    }

    public void saveLockPattern(List<LockPatternView.Cell> list, String str, int i) {
        if (list == null || list.size() < 4) {
            throw new IllegalArgumentException("pattern must not be null and at least 4 dots long.");
        }
        String strPatternToString = patternToString(list);
        int keyguardStoredPasswordQuality = getKeyguardStoredPasswordQuality(i);
        setKeyguardStoredPasswordQuality(65536, i);
        try {
            getLockSettings().setLockCredential(strPatternToString, 1, str, 65536, i);
            if (i == 0 && isDeviceEncryptionEnabled()) {
                if (!shouldEncryptWithCredentials(true)) {
                    clearEncryptionPassword();
                } else {
                    updateEncryptionPassword(2, strPatternToString);
                }
            }
            reportPatternWasChosen(i);
            onAfterChangingPassword(i);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't save lock pattern", e);
            setKeyguardStoredPasswordQuality(keyguardStoredPasswordQuality, i);
        }
    }

    private void updateCryptoUserInfo(int i) {
        if (i != 0) {
            return;
        }
        String ownerInfo = isOwnerInfoEnabled(i) ? getOwnerInfo(i) : "";
        IBinder service = ServiceManager.getService("mount");
        if (service == null) {
            Log.e(TAG, "Could not find the mount service to update the user info");
            return;
        }
        IStorageManager iStorageManagerAsInterface = IStorageManager.Stub.asInterface(service);
        try {
            Log.d(TAG, "Setting owner info");
            iStorageManagerAsInterface.setField(StorageManager.OWNER_INFO_KEY, ownerInfo);
        } catch (RemoteException e) {
            Log.e(TAG, "Error changing user info", e);
        }
    }

    public void setOwnerInfo(String str, int i) {
        setString("lock_screen_owner_info", str, i);
        updateCryptoUserInfo(i);
    }

    public void setOwnerInfoEnabled(boolean z, int i) {
        setBoolean("lock_screen_owner_info_enabled", z, i);
        updateCryptoUserInfo(i);
    }

    public String getOwnerInfo(int i) {
        return getString("lock_screen_owner_info", i);
    }

    public boolean isOwnerInfoEnabled(int i) {
        return getBoolean("lock_screen_owner_info_enabled", false, i);
    }

    public void setDeviceOwnerInfo(String str) {
        if (str != null && str.isEmpty()) {
            str = null;
        }
        setString(LOCK_SCREEN_DEVICE_OWNER_INFO, str, 0);
    }

    public String getDeviceOwnerInfo() {
        return getString(LOCK_SCREEN_DEVICE_OWNER_INFO, 0);
    }

    public boolean isDeviceOwnerInfoEnabled() {
        return getDeviceOwnerInfo() != null;
    }

    private void updateEncryptionPassword(final int i, final String str) {
        if (!isDeviceEncryptionEnabled()) {
            return;
        }
        final IBinder service = ServiceManager.getService("mount");
        if (service == null) {
            Log.e(TAG, "Could not find the mount service to update the encryption password");
        } else {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) {
                    try {
                        IStorageManager.Stub.asInterface(service).changeEncryptionPassword(i, str);
                        return null;
                    } catch (RemoteException e) {
                        Log.e(LockPatternUtils.TAG, "Error changing encryption password", e);
                        return null;
                    }
                }
            }.execute(new Void[0]);
        }
    }

    public void saveLockPassword(String str, String str2, int i, int i2) {
        if (str == null || str.length() < 4) {
            throw new IllegalArgumentException("password must not be null and at least of length 4");
        }
        int keyguardStoredPasswordQuality = getKeyguardStoredPasswordQuality(i2);
        setKeyguardStoredPasswordQuality(computePasswordQuality(2, str, i), i2);
        try {
            getLockSettings().setLockCredential(str, 2, str2, i, i2);
            updateEncryptionPasswordIfNeeded(str, PasswordMetrics.computeForPassword(str).quality, i2);
            updatePasswordHistory(str, i2);
            onAfterChangingPassword(i2);
        } catch (Exception e) {
            Log.e(TAG, "Unable to save lock password", e);
            setKeyguardStoredPasswordQuality(keyguardStoredPasswordQuality, i2);
        }
    }

    private void updateEncryptionPasswordIfNeeded(String str, int i, int i2) {
        if (i2 == 0 && isDeviceEncryptionEnabled()) {
            boolean z = true;
            if (!shouldEncryptWithCredentials(true)) {
                clearEncryptionPassword();
                return;
            }
            boolean z2 = i == 131072;
            if (i != 196608) {
                z = false;
            }
            updateEncryptionPassword((z2 || z) ? 3 : 0, str);
        }
    }

    private void updatePasswordHistory(String str, int i) {
        String string;
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "checkPasswordHistory: empty password");
            return;
        }
        String string2 = getString(PASSWORD_HISTORY_KEY, i);
        if (string2 == null) {
            string2 = "";
        }
        int requestedPasswordHistoryLength = getRequestedPasswordHistoryLength(i);
        if (requestedPasswordHistoryLength == 0) {
            string = "";
        } else {
            String strPasswordToHistoryHash = passwordToHistoryHash(str, getPasswordHistoryHashFactor(str, i), i);
            if (strPasswordToHistoryHash == null) {
                Log.e(TAG, "Compute new style password hash failed, fallback to legacy style");
                string = legacyPasswordToHash(str, i);
            } else {
                string = strPasswordToHistoryHash;
            }
            if (!TextUtils.isEmpty(string2)) {
                String[] strArrSplit = string2.split(HISTORY_DELIMITER);
                StringJoiner stringJoiner = new StringJoiner(HISTORY_DELIMITER);
                stringJoiner.add(string);
                for (int i2 = 0; i2 < requestedPasswordHistoryLength - 1 && i2 < strArrSplit.length; i2++) {
                    stringJoiner.add(strArrSplit[i2]);
                }
                string = stringJoiner.toString();
            }
        }
        setString(PASSWORD_HISTORY_KEY, string, i);
    }

    public static boolean isDeviceEncryptionEnabled() {
        return StorageManager.isEncrypted();
    }

    public static boolean isFileEncryptionEnabled() {
        return StorageManager.isFileEncryptedNativeOrEmulated();
    }

    public void clearEncryptionPassword() {
        updateEncryptionPassword(1, null);
    }

    public int getKeyguardStoredPasswordQuality(int i) {
        return (int) getLong(PASSWORD_TYPE_KEY, 0L, i);
    }

    private void setKeyguardStoredPasswordQuality(int i, int i2) {
        setLong(PASSWORD_TYPE_KEY, i, i2);
    }

    private int computePasswordQuality(int i, String str, int i2) {
        if (i == 2) {
            return Math.max(i2, PasswordMetrics.computeForPassword(str).quality);
        }
        if (i == 1) {
            return 65536;
        }
        return 0;
    }

    public void setSeparateProfileChallengeEnabled(int i, boolean z, String str) {
        if (!isManagedProfile(i)) {
            return;
        }
        try {
            getLockSettings().setSeparateProfileChallengeEnabled(i, z, str);
            onAfterChangingPassword(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't update work profile challenge enabled");
        }
    }

    public boolean isSeparateProfileChallengeEnabled(int i) {
        return isManagedProfile(i) && hasSeparateChallenge(i);
    }

    public boolean isManagedProfileWithUnifiedChallenge(int i) {
        return isManagedProfile(i) && !hasSeparateChallenge(i);
    }

    public boolean isSeparateProfileChallengeAllowed(int i) {
        return isManagedProfile(i) && getDevicePolicyManager().isSeparateProfileChallengeAllowed(i);
    }

    public boolean isSeparateProfileChallengeAllowedToUnify(int i) {
        return getDevicePolicyManager().isProfileActivePasswordSufficientForParent(i) && !getUserManager().hasUserRestriction(UserManager.DISALLOW_UNIFIED_PASSWORD, UserHandle.of(i));
    }

    private boolean hasSeparateChallenge(int i) {
        try {
            return getLockSettings().getSeparateProfileChallengeEnabled(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't get separate profile challenge enabled");
            return false;
        }
    }

    private boolean isManagedProfile(int i) {
        UserInfo userInfo = getUserManager().getUserInfo(i);
        return userInfo != null && userInfo.isManagedProfile();
    }

    public static List<LockPatternView.Cell> stringToPattern(String str) {
        if (str == null) {
            return null;
        }
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        for (byte b : str.getBytes()) {
            byte b2 = (byte) (b - 49);
            arrayListNewArrayList.add(LockPatternView.Cell.of(b2 / 3, b2 % 3));
        }
        return arrayListNewArrayList;
    }

    public static String patternToString(List<LockPatternView.Cell> list) {
        if (list == null) {
            return "";
        }
        int size = list.size();
        byte[] bArr = new byte[size];
        for (int i = 0; i < size; i++) {
            LockPatternView.Cell cell = list.get(i);
            bArr[i] = (byte) ((cell.getRow() * 3) + cell.getColumn() + 49);
        }
        return new String(bArr);
    }

    public static String patternStringToBaseZero(String str) {
        if (str == null) {
            return "";
        }
        int length = str.length();
        byte[] bArr = new byte[length];
        byte[] bytes = str.getBytes();
        for (int i = 0; i < length; i++) {
            bArr[i] = (byte) (bytes[i] - 49);
        }
        return new String(bArr);
    }

    public static byte[] patternToHash(List<LockPatternView.Cell> list) {
        if (list == null) {
            return null;
        }
        int size = list.size();
        byte[] bArr = new byte[size];
        for (int i = 0; i < size; i++) {
            LockPatternView.Cell cell = list.get(i);
            bArr[i] = (byte) ((cell.getRow() * 3) + cell.getColumn());
        }
        try {
            return MessageDigest.getInstance(KeyProperties.DIGEST_SHA1).digest(bArr);
        } catch (NoSuchAlgorithmException e) {
            return bArr;
        }
    }

    private String getSalt(int i) {
        long jNextLong = getLong(LOCK_PASSWORD_SALT_KEY, 0L, i);
        if (jNextLong == 0) {
            try {
                jNextLong = SecureRandom.getInstance("SHA1PRNG").nextLong();
                setLong(LOCK_PASSWORD_SALT_KEY, jNextLong, i);
                Log.v(TAG, "Initialized lock password salt for user: " + i);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Couldn't get SecureRandom number", e);
            }
        }
        return Long.toHexString(jNextLong);
    }

    public String legacyPasswordToHash(String str, int i) {
        if (str == null) {
            return null;
        }
        try {
            byte[] bytes = (str + getSalt(i)).getBytes();
            byte[] bArrDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA1).digest(bytes);
            byte[] bArrDigest2 = MessageDigest.getInstance(KeyProperties.DIGEST_MD5).digest(bytes);
            byte[] bArr = new byte[bArrDigest.length + bArrDigest2.length];
            System.arraycopy(bArrDigest, 0, bArr, 0, bArrDigest.length);
            System.arraycopy(bArrDigest2, 0, bArr, bArrDigest.length, bArrDigest2.length);
            return new String(HexEncoding.encode(bArr));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Missing digest algorithm: ", e);
        }
    }

    private String passwordToHistoryHash(String str, byte[] bArr, int i) {
        if (TextUtils.isEmpty(str) || bArr == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256);
            messageDigest.update(bArr);
            messageDigest.update((str + getSalt(i)).getBytes());
            return new String(HexEncoding.encode(messageDigest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Missing digest algorithm: ", e);
        }
    }

    public boolean isSecure(int i) {
        int keyguardStoredPasswordQuality = getKeyguardStoredPasswordQuality(i);
        return isLockPatternEnabled(keyguardStoredPasswordQuality, i) || isLockPasswordEnabled(keyguardStoredPasswordQuality, i);
    }

    public boolean isLockPasswordEnabled(int i) {
        return isLockPasswordEnabled(getKeyguardStoredPasswordQuality(i), i);
    }

    private boolean isLockPasswordEnabled(int i, int i2) {
        return (i == 262144 || i == 131072 || i == 196608 || i == 327680 || i == 393216 || i == 524288) && savedPasswordExists(i2);
    }

    public boolean isLockPatternEnabled(int i) {
        return isLockPatternEnabled(getKeyguardStoredPasswordQuality(i), i);
    }

    @Deprecated
    public boolean isLegacyLockPatternEnabled(int i) {
        return getBoolean(LEGACY_LOCK_PATTERN_ENABLED, true, i);
    }

    @Deprecated
    public void setLegacyLockPatternEnabled(int i) {
        setBoolean("lock_pattern_autolock", true, i);
    }

    private boolean isLockPatternEnabled(int i, int i2) {
        return i == 65536 && savedPatternExists(i2);
    }

    public boolean isVisiblePatternEnabled(int i) {
        return getBoolean("lock_pattern_visible_pattern", false, i);
    }

    public void setVisiblePatternEnabled(boolean z, int i) {
        setBoolean("lock_pattern_visible_pattern", z, i);
        if (i != 0) {
            return;
        }
        IBinder service = ServiceManager.getService("mount");
        if (service == null) {
            Log.e(TAG, "Could not find the mount service to update the user info");
            return;
        }
        try {
            IStorageManager.Stub.asInterface(service).setField(StorageManager.PATTERN_VISIBLE_KEY, z ? WifiEnterpriseConfig.ENGINE_ENABLE : WifiEnterpriseConfig.ENGINE_DISABLE);
        } catch (RemoteException e) {
            Log.e(TAG, "Error changing pattern visible state", e);
        }
    }

    public boolean isVisiblePatternEverChosen(int i) {
        return getString("lock_pattern_visible_pattern", i) != null;
    }

    public void setVisiblePasswordEnabled(boolean z, int i) {
        if (i != 0) {
            return;
        }
        IBinder service = ServiceManager.getService("mount");
        if (service == null) {
            Log.e(TAG, "Could not find the mount service to update the user info");
            return;
        }
        try {
            IStorageManager.Stub.asInterface(service).setField(StorageManager.PASSWORD_VISIBLE_KEY, z ? WifiEnterpriseConfig.ENGINE_ENABLE : WifiEnterpriseConfig.ENGINE_DISABLE);
        } catch (RemoteException e) {
            Log.e(TAG, "Error changing password visible state", e);
        }
    }

    public boolean isTactileFeedbackEnabled() {
        return Settings.System.getIntForUser(this.mContentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, -2) != 0;
    }

    public long setLockoutAttemptDeadline(int i, int i2) {
        long jElapsedRealtime = SystemClock.elapsedRealtime() + ((long) i2);
        if (i == -9999) {
            return jElapsedRealtime;
        }
        this.mLockoutDeadlines.put(i, jElapsedRealtime);
        return jElapsedRealtime;
    }

    public long getLockoutAttemptDeadline(int i) {
        long j = this.mLockoutDeadlines.get(i, 0L);
        if (j < SystemClock.elapsedRealtime() && j != 0) {
            this.mLockoutDeadlines.put(i, 0L);
            return 0L;
        }
        return j;
    }

    private boolean getBoolean(String str, boolean z, int i) {
        try {
            Log.d(TAG, "start getBoolean " + str + ",defaultValue = " + z);
            boolean z2 = getLockSettings().getBoolean(str, z, i);
            StringBuilder sb = new StringBuilder();
            sb.append("end getBoolean ret = ");
            sb.append(z2);
            Log.d(TAG, sb.toString());
            return z2;
        } catch (RemoteException e) {
            return z;
        }
    }

    private void setBoolean(String str, boolean z, int i) {
        try {
            getLockSettings().setBoolean(str, z, i);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't write boolean " + str + e);
        }
    }

    private long getLong(String str, long j, int i) {
        try {
            return getLockSettings().getLong(str, j, i);
        } catch (RemoteException e) {
            return j;
        }
    }

    private void setLong(String str, long j, int i) {
        try {
            getLockSettings().setLong(str, j, i);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't write long " + str + e);
        }
    }

    private String getString(String str, int i) {
        try {
            return getLockSettings().getString(str, null, i);
        } catch (RemoteException e) {
            return null;
        }
    }

    private void setString(String str, String str2, int i) {
        try {
            getLockSettings().setString(str, str2, i);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't write string " + str + e);
        }
    }

    public void setPowerButtonInstantlyLocks(boolean z, int i) {
        setBoolean(LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS, z, i);
    }

    public boolean getPowerButtonInstantlyLocks(int i) {
        return getBoolean(LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS, true, i);
    }

    public boolean isPowerButtonInstantlyLocksEverChosen(int i) {
        return getString(LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS, i) != null;
    }

    public void setEnabledTrustAgents(Collection<ComponentName> collection, int i) {
        StringBuilder sb = new StringBuilder();
        for (ComponentName componentName : collection) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(componentName.flattenToShortString());
        }
        setString(ENABLED_TRUST_AGENTS, sb.toString(), i);
        getTrustManager().reportEnabledTrustAgentsChanged(i);
    }

    public List<ComponentName> getEnabledTrustAgents(int i) {
        String string = getString(ENABLED_TRUST_AGENTS, i);
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        String[] strArrSplit = string.split(HISTORY_DELIMITER);
        ArrayList arrayList = new ArrayList(strArrSplit.length);
        for (String str : strArrSplit) {
            if (!TextUtils.isEmpty(str)) {
                arrayList.add(ComponentName.unflattenFromString(str));
            }
        }
        return arrayList;
    }

    public void requireCredentialEntry(int i) {
        requireStrongAuth(4, i);
    }

    public void requireStrongAuth(int i, int i2) {
        try {
            getLockSettings().requireStrongAuth(i, i2);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while requesting strong auth: " + e);
        }
    }

    private void onAfterChangingPassword(int i) {
        getTrustManager().reportEnabledTrustAgentsChanged(i);
    }

    public boolean isCredentialRequiredToDecrypt(boolean z) {
        int i = Settings.Global.getInt(this.mContentResolver, Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, -1);
        return i == -1 ? z : i != 0;
    }

    public void setCredentialRequiredToDecrypt(boolean z) {
        if (!getUserManager().isSystemUser() && !getUserManager().isPrimaryUser()) {
            throw new IllegalStateException("Only the system or primary user may call setCredentialRequiredForDecrypt()");
        }
        if (isDeviceEncryptionEnabled()) {
            Settings.Global.putInt(this.mContext.getContentResolver(), Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, z ? 1 : 0);
        }
    }

    private boolean isDoNotAskCredentialsOnBootSet() {
        return getDevicePolicyManager().getDoNotAskCredentialsOnBoot();
    }

    private boolean shouldEncryptWithCredentials(boolean z) {
        return isCredentialRequiredToDecrypt(z) && !isDoNotAskCredentialsOnBootSet();
    }

    private void throwIfCalledOnMainThread() {
        if (Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("should not be called from the main thread.");
        }
    }

    public void registerStrongAuthTracker(StrongAuthTracker strongAuthTracker) {
        try {
            getLockSettings().registerStrongAuthTracker(strongAuthTracker.mStub);
        } catch (RemoteException e) {
            throw new RuntimeException("Could not register StrongAuthTracker");
        }
    }

    public void unregisterStrongAuthTracker(StrongAuthTracker strongAuthTracker) {
        try {
            getLockSettings().unregisterStrongAuthTracker(strongAuthTracker.mStub);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not unregister StrongAuthTracker", e);
        }
    }

    public int getStrongAuthForUser(int i) {
        try {
            return getLockSettings().getStrongAuthForUser(i);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not get StrongAuth", e);
            return StrongAuthTracker.getDefaultFlags(this.mContext);
        }
    }

    public boolean isTrustAllowedForUser(int i) {
        return getStrongAuthForUser(i) == 0;
    }

    public boolean isFingerprintAllowedForUser(int i) {
        return (getStrongAuthForUser(i) & (-5)) == 0;
    }

    public boolean isUserInLockdown(int i) {
        return getStrongAuthForUser(i) == 32;
    }

    private ICheckCredentialProgressCallback wrapCallback(final CheckCredentialProgressCallback checkCredentialProgressCallback) {
        if (checkCredentialProgressCallback == null) {
            return null;
        }
        if (this.mHandler == null) {
            throw new IllegalStateException("Must construct LockPatternUtils on a looper thread to use progress callbacks.");
        }
        return new ICheckCredentialProgressCallback.Stub() {
            @Override
            public void onCredentialVerified() throws RemoteException {
                Handler handler = LockPatternUtils.this.mHandler;
                final CheckCredentialProgressCallback checkCredentialProgressCallback2 = checkCredentialProgressCallback;
                Objects.requireNonNull(checkCredentialProgressCallback2);
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        checkCredentialProgressCallback2.onEarlyMatched();
                    }
                });
            }
        };
    }

    private LockSettingsInternal getLockSettingsInternal() {
        LockSettingsInternal lockSettingsInternal = (LockSettingsInternal) LocalServices.getService(LockSettingsInternal.class);
        if (lockSettingsInternal == null) {
            throw new SecurityException("Only available to system server itself");
        }
        return lockSettingsInternal;
    }

    public long addEscrowToken(byte[] bArr, int i) {
        return getLockSettingsInternal().addEscrowToken(bArr, i);
    }

    public boolean removeEscrowToken(long j, int i) {
        return getLockSettingsInternal().removeEscrowToken(j, i);
    }

    public boolean isEscrowTokenActive(long j, int i) {
        return getLockSettingsInternal().isEscrowTokenActive(j, i);
    }

    public boolean setLockCredentialWithToken(String str, int i, int i2, long j, byte[] bArr, int i3) {
        LockSettingsInternal lockSettingsInternal = getLockSettingsInternal();
        if (i != -1) {
            if (TextUtils.isEmpty(str) || str.length() < 4) {
                throw new IllegalArgumentException("password must not be null and at least of length 4");
            }
            int iComputePasswordQuality = computePasswordQuality(i, str, i2);
            if (!lockSettingsInternal.setLockCredentialWithToken(str, i, j, bArr, iComputePasswordQuality, i3)) {
                return false;
            }
            setKeyguardStoredPasswordQuality(iComputePasswordQuality, i3);
            updateEncryptionPasswordIfNeeded(str, iComputePasswordQuality, i3);
            updatePasswordHistory(str, i3);
            onAfterChangingPassword(i3);
        } else if (TextUtils.isEmpty(str)) {
            if (!lockSettingsInternal.setLockCredentialWithToken(null, -1, j, bArr, 0, i3)) {
                return false;
            }
            setKeyguardStoredPasswordQuality(0, i3);
            if (i3 == 0) {
                updateEncryptionPassword(1, null);
                setCredentialRequiredToDecrypt(false);
            }
        } else {
            throw new IllegalArgumentException("password must be emtpy for NONE type");
        }
        onAfterChangingPassword(i3);
        return true;
    }

    public boolean unlockUserWithToken(long j, byte[] bArr, int i) {
        return getLockSettingsInternal().unlockUserWithToken(j, bArr, i);
    }

    public static class StrongAuthTracker {
        private static final int ALLOWING_FINGERPRINT = 4;
        public static final int SOME_AUTH_REQUIRED_AFTER_USER_REQUEST = 4;
        public static final int STRONG_AUTH_NOT_REQUIRED = 0;
        public static final int STRONG_AUTH_REQUIRED_AFTER_BOOT = 1;
        public static final int STRONG_AUTH_REQUIRED_AFTER_DPM_LOCK_NOW = 2;
        public static final int STRONG_AUTH_REQUIRED_AFTER_LOCKOUT = 8;
        public static final int STRONG_AUTH_REQUIRED_AFTER_TIMEOUT = 16;
        public static final int STRONG_AUTH_REQUIRED_AFTER_USER_LOCKDOWN = 32;
        private final int mDefaultStrongAuthFlags;
        private final H mHandler;
        private final SparseIntArray mStrongAuthRequiredForUser;
        protected final IStrongAuthTracker.Stub mStub;

        @Retention(RetentionPolicy.SOURCE)
        public @interface StrongAuthFlags {
        }

        public StrongAuthTracker(Context context) {
            this(context, Looper.myLooper());
        }

        public StrongAuthTracker(Context context, Looper looper) {
            this.mStrongAuthRequiredForUser = new SparseIntArray();
            this.mStub = new IStrongAuthTracker.Stub() {
                @Override
                public void onStrongAuthRequiredChanged(int i, int i2) {
                    StrongAuthTracker.this.mHandler.obtainMessage(1, i, i2).sendToTarget();
                }
            };
            this.mHandler = new H(looper);
            this.mDefaultStrongAuthFlags = getDefaultFlags(context);
        }

        public static int getDefaultFlags(Context context) {
            return context.getResources().getBoolean(R.bool.config_strongAuthRequiredOnBoot) ? 1 : 0;
        }

        public int getStrongAuthForUser(int i) {
            return this.mStrongAuthRequiredForUser.get(i, this.mDefaultStrongAuthFlags);
        }

        public boolean isTrustAllowedForUser(int i) {
            return getStrongAuthForUser(i) == 0;
        }

        public boolean isFingerprintAllowedForUser(int i) {
            return (getStrongAuthForUser(i) & (-5)) == 0;
        }

        public void onStrongAuthRequiredChanged(int i) {
        }

        protected void handleStrongAuthRequiredChanged(int i, int i2) {
            if (i != getStrongAuthForUser(i2)) {
                if (i == this.mDefaultStrongAuthFlags) {
                    this.mStrongAuthRequiredForUser.delete(i2);
                } else {
                    this.mStrongAuthRequiredForUser.put(i2, i);
                }
                onStrongAuthRequiredChanged(i2);
            }
        }

        private class H extends Handler {
            static final int MSG_ON_STRONG_AUTH_REQUIRED_CHANGED = 1;

            public H(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    StrongAuthTracker.this.handleStrongAuthRequiredChanged(message.arg1, message.arg2);
                }
            }
        }
    }

    public void enableSyntheticPassword() {
        setLong(SYNTHETIC_PASSWORD_ENABLED_KEY, 1L, 0);
    }

    public void disableSyntheticPassword() {
        setLong(SYNTHETIC_PASSWORD_ENABLED_KEY, 0L, 0);
    }

    public boolean isSyntheticPasswordEnabled() {
        return getLong(SYNTHETIC_PASSWORD_ENABLED_KEY, 0L, 0) != 0;
    }

    public static boolean userOwnsFrpCredential(Context context, UserInfo userInfo) {
        return userInfo != null && userInfo.isPrimary() && userInfo.isAdmin() && frpCredentialEnabled(context);
    }

    public static boolean frpCredentialEnabled(Context context) {
        return context.getResources().getBoolean(R.bool.config_enableCredentialFactoryResetProtection);
    }
}
