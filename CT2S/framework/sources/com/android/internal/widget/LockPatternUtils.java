package com.android.internal.widget;

import android.Manifest;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.widget.Button;
import com.android.internal.R;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternView;
import com.google.android.collect.Lists;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LockPatternUtils {
    public static final String BIOMETRIC_WEAK_EVER_CHOSEN_KEY = "lockscreen.biometricweakeverchosen";
    private static final boolean DEBUG = false;
    public static final String DISABLE_LOCKSCREEN_KEY = "lockscreen.disabled";
    private static final String ENABLED_TRUST_AGENTS = "lockscreen.enabledtrustagents";
    public static final int FAILED_ATTEMPTS_BEFORE_RESET = 20;
    public static final int FAILED_ATTEMPTS_BEFORE_TIMEOUT = 5;
    public static final int FAILED_ATTEMPTS_BEFORE_WIPE_GRACE = 5;
    public static final long FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS = 1000;
    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;
    public static final int FLAG_BIOMETRIC_WEAK_LIVELINESS = 1;
    public static final int ID_DEFAULT_STATUS_WIDGET = -2;
    public static final String KEYGUARD_SHOW_APPWIDGET = "showappwidget";
    public static final String KEYGUARD_SHOW_SECURITY_CHALLENGE = "showsecuritychallenge";
    public static final String KEYGUARD_SHOW_USER_SWITCHER = "showuserswitcher";
    public static final String LOCKOUT_ATTEMPT_DEADLINE = "lockscreen.lockoutattemptdeadline";
    public static final String LOCKOUT_PERMANENT_KEY = "lockscreen.lockedoutpermanently";
    public static final String LOCKSCREEN_BIOMETRIC_WEAK_FALLBACK = "lockscreen.biometric_weak_fallback";
    public static final String LOCKSCREEN_OPTIONS = "lockscreen.options";
    public static final String LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS = "lockscreen.power_button_instantly_locks";
    public static final String LOCKSCREEN_WIDGETS_ENABLED = "lockscreen.widgets_enabled";
    public static final String LOCK_PASSWORD_SALT_KEY = "lockscreen.password_salt";
    private static final String LOCK_SCREEN_OWNER_INFO = "lock_screen_owner_info";
    private static final String LOCK_SCREEN_OWNER_INFO_ENABLED = "lock_screen_owner_info_enabled";
    public static final int MAX_ALLOWED_SEQUENCE = 3;
    public static final int MIN_LOCK_PATTERN_SIZE = 4;
    public static final int MIN_PATTERN_REGISTER_FAIL = 4;
    public static final String PASSWORD_HISTORY_KEY = "lockscreen.passwordhistory";
    public static final String PASSWORD_TYPE_ALTERNATE_KEY = "lockscreen.password_type_alternate";
    public static final String PASSWORD_TYPE_KEY = "lockscreen.password_type";
    public static final String PATTERN_EVER_CHOSEN_KEY = "lockscreen.patterneverchosen";
    private static final String TAG = "LockPatternUtils";
    private static volatile int sCurrentUserId = -10000;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private DevicePolicyManager mDevicePolicyManager;
    private ILockSettings mLockSettingsService;
    private final boolean mMultiUserMode;

    public DevicePolicyManager getDevicePolicyManager() {
        if (this.mDevicePolicyManager == null) {
            this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (this.mDevicePolicyManager == null) {
                Log.e(TAG, "Can't get DevicePolicyManagerService: is it running?", new IllegalStateException("Stack trace:"));
            }
        }
        return this.mDevicePolicyManager;
    }

    private TrustManager getTrustManager() {
        TrustManager trust = (TrustManager) this.mContext.getSystemService(Context.TRUST_SERVICE);
        if (trust == null) {
            Log.e(TAG, "Can't get TrustManagerService: is it running?", new IllegalStateException("Stack trace:"));
        }
        return trust;
    }

    public LockPatternUtils(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mMultiUserMode = context.checkCallingOrSelfPermission(Manifest.permission.INTERACT_ACROSS_USERS_FULL) == 0;
    }

    private ILockSettings getLockSettings() {
        if (this.mLockSettingsService == null) {
            ILockSettings service = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
            this.mLockSettingsService = service;
        }
        return this.mLockSettingsService;
    }

    public int getRequestedMinimumPasswordLength() {
        return getDevicePolicyManager().getPasswordMinimumLength(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordQuality() {
        return getDevicePolicyManager().getPasswordQuality(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordHistoryLength() {
        return getDevicePolicyManager().getPasswordHistoryLength(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordMinimumLetters() {
        return getDevicePolicyManager().getPasswordMinimumLetters(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordMinimumUpperCase() {
        return getDevicePolicyManager().getPasswordMinimumUpperCase(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordMinimumLowerCase() {
        return getDevicePolicyManager().getPasswordMinimumLowerCase(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordMinimumNumeric() {
        return getDevicePolicyManager().getPasswordMinimumNumeric(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordMinimumSymbols() {
        return getDevicePolicyManager().getPasswordMinimumSymbols(null, getCurrentOrCallingUserId());
    }

    public int getRequestedPasswordMinimumNonLetter() {
        return getDevicePolicyManager().getPasswordMinimumNonLetter(null, getCurrentOrCallingUserId());
    }

    public void reportFailedPasswordAttempt() {
        int userId = getCurrentOrCallingUserId();
        getDevicePolicyManager().reportFailedPasswordAttempt(userId);
        getTrustManager().reportUnlockAttempt(false, userId);
        getTrustManager().reportRequireCredentialEntry(userId);
    }

    public void reportSuccessfulPasswordAttempt() {
        getDevicePolicyManager().reportSuccessfulPasswordAttempt(getCurrentOrCallingUserId());
        getTrustManager().reportUnlockAttempt(true, getCurrentOrCallingUserId());
    }

    public void setCurrentUser(int userId) {
        sCurrentUserId = userId;
    }

    public int getCurrentUser() {
        if (sCurrentUserId != -10000) {
            return sCurrentUserId;
        }
        try {
            return ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void removeUser(int userId) {
        try {
            getLockSettings().removeUser(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't remove lock settings for user " + userId);
        }
    }

    private int getCurrentOrCallingUserId() {
        return this.mMultiUserMode ? getCurrentUser() : UserHandle.getCallingUserId();
    }

    public boolean checkPattern(List<LockPatternView.Cell> pattern) {
        int userId = getCurrentOrCallingUserId();
        try {
            return getLockSettings().checkPattern(patternToString(pattern), userId);
        } catch (RemoteException e) {
            return true;
        }
    }

    public boolean checkPassword(String password) {
        int userId = getCurrentOrCallingUserId();
        try {
            return getLockSettings().checkPassword(password, userId);
        } catch (RemoteException e) {
            return true;
        }
    }

    public boolean checkVoldPassword() {
        int userId = getCurrentOrCallingUserId();
        try {
            return getLockSettings().checkVoldPassword(userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean checkPasswordHistory(String password) {
        String passwordHashString = new String(passwordToHash(password, getCurrentOrCallingUserId()));
        String passwordHistory = getString(PASSWORD_HISTORY_KEY);
        if (passwordHistory == null) {
            return false;
        }
        int passwordHashLength = passwordHashString.length();
        int passwordHistoryLength = getRequestedPasswordHistoryLength();
        if (passwordHistoryLength == 0) {
            return false;
        }
        int neededPasswordHistoryLength = ((passwordHashLength * passwordHistoryLength) + passwordHistoryLength) - 1;
        if (passwordHistory.length() > neededPasswordHistoryLength) {
            passwordHistory = passwordHistory.substring(0, neededPasswordHistoryLength);
        }
        return passwordHistory.contains(passwordHashString);
    }

    public boolean savedPatternExists() {
        return savedPatternExists(getCurrentOrCallingUserId());
    }

    public boolean savedPatternExists(int userId) {
        try {
            return getLockSettings().havePattern(userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean savedPasswordExists() {
        return savedPasswordExists(getCurrentOrCallingUserId());
    }

    public boolean savedPasswordExists(int userId) {
        try {
            return getLockSettings().havePassword(userId);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isPatternEverChosen() {
        return getBoolean(PATTERN_EVER_CHOSEN_KEY, false);
    }

    public boolean isBiometricWeakEverChosen() {
        return getBoolean(BIOMETRIC_WEAK_EVER_CHOSEN_KEY, false);
    }

    public int getActivePasswordQuality() {
        int quality = (int) getLong(PASSWORD_TYPE_KEY, Trace.TRACE_TAG_BIONIC);
        switch (quality) {
            case 32768:
                if (isBiometricWeakInstalled()) {
                }
                break;
            case 65536:
                if (isLockPatternEnabled()) {
                }
                break;
            case 131072:
                if (isLockPasswordEnabled()) {
                }
                break;
            case 196608:
                if (isLockPasswordEnabled()) {
                }
                break;
            case 262144:
                if (isLockPasswordEnabled()) {
                }
                break;
            case 327680:
                if (isLockPasswordEnabled()) {
                }
                break;
            case 393216:
                if (isLockPasswordEnabled()) {
                }
                break;
        }
        return 0;
    }

    public void clearLock(boolean isFallback) {
        clearLock(isFallback, getCurrentOrCallingUserId());
    }

    public void clearLock(boolean isFallback, int userHandle) {
        if (!isFallback) {
            deleteGallery(userHandle);
        }
        saveLockPassword(null, 65536, isFallback, userHandle);
        setLockPatternEnabled(false, userHandle);
        saveLockPattern(null, isFallback, userHandle);
        setLong(PASSWORD_TYPE_KEY, 0L, userHandle);
        setLong(PASSWORD_TYPE_ALTERNATE_KEY, 0L, userHandle);
        onAfterChangingPassword(userHandle);
    }

    public void setLockScreenDisabled(boolean disable) {
        setLong("lockscreen.disabled", disable ? 1L : 0L);
    }

    public boolean isLockScreenDisabled() {
        if (isSecure() || getLong("lockscreen.disabled", 0L) == 0) {
            return false;
        }
        List<UserInfo> users = UserManager.get(this.mContext).getUsers(true);
        int userCount = users.size();
        int switchableUsers = 0;
        for (int i = 0; i < userCount; i++) {
            if (users.get(i).supportsSwitchTo()) {
                switchableUsers++;
            }
        }
        return switchableUsers < 2;
    }

    public void deleteTempGallery() {
        Intent intent = new Intent().setAction("com.android.facelock.DELETE_GALLERY");
        intent.putExtra("deleteTempGallery", true);
        this.mContext.sendBroadcast(intent);
    }

    void deleteGallery(int userId) {
        if (usingBiometricWeak(userId)) {
            Intent intent = new Intent().setAction("com.android.facelock.DELETE_GALLERY");
            intent.putExtra("deleteGallery", true);
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
        }
    }

    public void saveLockPattern(List<LockPatternView.Cell> pattern) {
        saveLockPattern(pattern, false);
    }

    public void saveLockPattern(List<LockPatternView.Cell> pattern, boolean isFallback) {
        saveLockPattern(pattern, isFallback, getCurrentOrCallingUserId());
    }

    public void saveLockPattern(List<LockPatternView.Cell> pattern, boolean isFallback, int userId) {
        try {
            getLockSettings().setLockPattern(patternToString(pattern), userId);
            DevicePolicyManager dpm = getDevicePolicyManager();
            if (pattern != null) {
                if (userId == 0 && isDeviceEncryptionEnabled()) {
                    boolean required = isCredentialRequiredToDecrypt(true);
                    if (!required) {
                        clearEncryptionPassword();
                    } else {
                        String stringPattern = patternToString(pattern);
                        updateEncryptionPassword(2, stringPattern);
                    }
                }
                setBoolean(PATTERN_EVER_CHOSEN_KEY, true, userId);
                if (!isFallback) {
                    deleteGallery(userId);
                    setLong(PASSWORD_TYPE_KEY, Trace.TRACE_TAG_BIONIC, userId);
                    dpm.setActivePasswordState(65536, pattern.size(), 0, 0, 0, 0, 0, 0, userId);
                } else {
                    setLong(PASSWORD_TYPE_KEY, Trace.TRACE_TAG_RS, userId);
                    setLong(PASSWORD_TYPE_ALTERNATE_KEY, Trace.TRACE_TAG_BIONIC, userId);
                    finishBiometricWeak(userId);
                    dpm.setActivePasswordState(32768, 0, 0, 0, 0, 0, 0, 0, userId);
                }
            } else {
                dpm.setActivePasswordState(0, 0, 0, 0, 0, 0, 0, 0, userId);
            }
            onAfterChangingPassword(userId);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't save lock pattern " + re);
        }
    }

    private void updateCryptoUserInfo() {
        int userId = getCurrentOrCallingUserId();
        if (userId == 0) {
            String ownerInfo = isOwnerInfoEnabled() ? getOwnerInfo(userId) : ProxyInfo.LOCAL_EXCL_LIST;
            IBinder service = ServiceManager.getService("mount");
            if (service == null) {
                Log.e(TAG, "Could not find the mount service to update the user info");
                return;
            }
            IMountService mountService = IMountService.Stub.asInterface(service);
            try {
                Log.d(TAG, "Setting owner info");
                mountService.setField(StorageManager.OWNER_INFO_KEY, ownerInfo);
            } catch (RemoteException e) {
                Log.e(TAG, "Error changing user info", e);
            }
        }
    }

    public void setOwnerInfo(String info, int userId) {
        setString("lock_screen_owner_info", info, userId);
        updateCryptoUserInfo();
    }

    public void setOwnerInfoEnabled(boolean enabled) {
        setBoolean("lock_screen_owner_info_enabled", enabled);
        updateCryptoUserInfo();
    }

    public String getOwnerInfo(int userId) {
        return getString("lock_screen_owner_info");
    }

    public boolean isOwnerInfoEnabled() {
        return getBoolean("lock_screen_owner_info_enabled", false);
    }

    public static int computePasswordQuality(String password) {
        boolean hasDigit = false;
        boolean hasNonDigit = false;
        int len = password.length();
        for (int i = 0; i < len; i++) {
            if (Character.isDigit(password.charAt(i))) {
                hasDigit = true;
            } else {
                hasNonDigit = true;
            }
        }
        if (hasNonDigit && hasDigit) {
            return 327680;
        }
        if (hasNonDigit) {
            return 262144;
        }
        if (hasDigit) {
            return maxLengthSequence(password) > 3 ? 131072 : 196608;
        }
        return 0;
    }

    private static int categoryChar(char c) {
        if ('a' <= c && c <= 'z') {
            return 0;
        }
        if ('A' > c || c > 'Z') {
            return ('0' > c || c > '9') ? 3 : 2;
        }
        return 1;
    }

    private static int maxDiffCategory(int category) {
        if (category == 0 || category == 1) {
            return 1;
        }
        return category == 2 ? 10 : 0;
    }

    public static int maxLengthSequence(String string) {
        if (string.length() == 0) {
            return 0;
        }
        char previousChar = string.charAt(0);
        int category = categoryChar(previousChar);
        int diff = 0;
        boolean hasDiff = false;
        int maxLength = 0;
        int startSequence = 0;
        for (int current = 1; current < string.length(); current++) {
            char currentChar = string.charAt(current);
            int categoryCurrent = categoryChar(currentChar);
            int currentDiff = currentChar - previousChar;
            if (categoryCurrent != category || Math.abs(currentDiff) > maxDiffCategory(category)) {
                maxLength = Math.max(maxLength, current - startSequence);
                startSequence = current;
                hasDiff = false;
                category = categoryCurrent;
            } else {
                if (hasDiff && currentDiff != diff) {
                    maxLength = Math.max(maxLength, current - startSequence);
                    startSequence = current - 1;
                }
                diff = currentDiff;
                hasDiff = true;
            }
            previousChar = currentChar;
        }
        return Math.max(maxLength, string.length() - startSequence);
    }

    private void updateEncryptionPassword(final int type, final String password) {
        if (isDeviceEncryptionEnabled()) {
            final IBinder service = ServiceManager.getService("mount");
            if (service == null) {
                Log.e(TAG, "Could not find the mount service to update the encryption password");
            } else {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... dummy) {
                        IMountService mountService = IMountService.Stub.asInterface(service);
                        try {
                            mountService.changeEncryptionPassword(type, password);
                            return null;
                        } catch (RemoteException e) {
                            Log.e(LockPatternUtils.TAG, "Error changing encryption password", e);
                            return null;
                        }
                    }
                }.execute(new Void[0]);
            }
        }
    }

    public void saveLockPassword(String password, int quality) {
        saveLockPassword(password, quality, false, getCurrentOrCallingUserId());
    }

    public void saveLockPassword(String password, int quality, boolean isFallback) {
        saveLockPassword(password, quality, isFallback, getCurrentOrCallingUserId());
    }

    public void saveLockPassword(String password, int quality, boolean isFallback, int userHandle) {
        String passwordHistory;
        try {
            DevicePolicyManager dpm = getDevicePolicyManager();
            if (!TextUtils.isEmpty(password)) {
                getLockSettings().setLockPassword(password, userHandle);
                int computedQuality = computePasswordQuality(password);
                if (userHandle == 0 && isDeviceEncryptionEnabled()) {
                    if (!isCredentialRequiredToDecrypt(true)) {
                        clearEncryptionPassword();
                    } else {
                        boolean numeric = computedQuality == 131072;
                        boolean numericComplex = computedQuality == 196608;
                        int type = (numeric || numericComplex) ? 3 : 0;
                        updateEncryptionPassword(type, password);
                    }
                }
                if (!isFallback) {
                    deleteGallery(userHandle);
                    setLong(PASSWORD_TYPE_KEY, Math.max(quality, computedQuality), userHandle);
                    if (computedQuality != 0) {
                        int letters = 0;
                        int uppercase = 0;
                        int lowercase = 0;
                        int numbers = 0;
                        int symbols = 0;
                        int nonletter = 0;
                        for (int i = 0; i < password.length(); i++) {
                            char c = password.charAt(i);
                            if (c >= 'A' && c <= 'Z') {
                                letters++;
                                uppercase++;
                            } else if (c >= 'a' && c <= 'z') {
                                letters++;
                                lowercase++;
                            } else if (c >= '0' && c <= '9') {
                                numbers++;
                                nonletter++;
                            } else {
                                symbols++;
                                nonletter++;
                            }
                        }
                        dpm.setActivePasswordState(Math.max(quality, computedQuality), password.length(), letters, uppercase, lowercase, numbers, symbols, nonletter, userHandle);
                    } else {
                        dpm.setActivePasswordState(0, 0, 0, 0, 0, 0, 0, 0, userHandle);
                    }
                } else {
                    setLong(PASSWORD_TYPE_KEY, Trace.TRACE_TAG_RS, userHandle);
                    setLong(PASSWORD_TYPE_ALTERNATE_KEY, Math.max(quality, computedQuality), userHandle);
                    finishBiometricWeak(userHandle);
                    dpm.setActivePasswordState(32768, 0, 0, 0, 0, 0, 0, 0, userHandle);
                }
                String passwordHistory2 = getString(PASSWORD_HISTORY_KEY, userHandle);
                if (passwordHistory2 == null) {
                    passwordHistory2 = ProxyInfo.LOCAL_EXCL_LIST;
                }
                int passwordHistoryLength = getRequestedPasswordHistoryLength();
                if (passwordHistoryLength == 0) {
                    passwordHistory = ProxyInfo.LOCAL_EXCL_LIST;
                } else {
                    byte[] hash = passwordToHash(password, userHandle);
                    String passwordHistory3 = new String(hash) + "," + passwordHistory2;
                    passwordHistory = passwordHistory3.substring(0, Math.min(((hash.length * passwordHistoryLength) + passwordHistoryLength) - 1, passwordHistory3.length()));
                }
                setString(PASSWORD_HISTORY_KEY, passwordHistory, userHandle);
            } else {
                getLockSettings().setLockPassword(null, userHandle);
                if (userHandle == 0) {
                    updateEncryptionPassword(1, null);
                }
                dpm.setActivePasswordState(0, 0, 0, 0, 0, 0, 0, 0, userHandle);
            }
            onAfterChangingPassword(userHandle);
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to save lock password " + re);
        }
    }

    public static boolean isDeviceEncrypted() {
        IMountService mountService = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        try {
            if (mountService.getEncryptionState() != 1) {
                if (mountService.getPasswordType() != 1) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException re) {
            Log.e(TAG, "Error getting encryption state", re);
            return true;
        }
    }

    public static boolean isDeviceEncryptionEnabled() {
        String status = SystemProperties.get("ro.crypto.state", "unsupported");
        return "encrypted".equalsIgnoreCase(status);
    }

    public void clearEncryptionPassword() {
        updateEncryptionPassword(1, null);
    }

    public int getKeyguardStoredPasswordQuality() {
        return getKeyguardStoredPasswordQuality(getCurrentOrCallingUserId());
    }

    public int getKeyguardStoredPasswordQuality(int userHandle) {
        int quality = (int) getLong(PASSWORD_TYPE_KEY, 0L, userHandle);
        if (quality == 32768) {
            return (int) getLong(PASSWORD_TYPE_ALTERNATE_KEY, 0L, userHandle);
        }
        return quality;
    }

    public boolean usingBiometricWeak() {
        return usingBiometricWeak(getCurrentOrCallingUserId());
    }

    public boolean usingBiometricWeak(int userId) {
        int quality = (int) getLong(PASSWORD_TYPE_KEY, 0L, userId);
        return quality == 32768;
    }

    public static List<LockPatternView.Cell> stringToPattern(String string) {
        List<LockPatternView.Cell> result = Lists.newArrayList();
        byte[] bytes = string.getBytes();
        for (byte b : bytes) {
            result.add(LockPatternView.Cell.of(b / 3, b % 3));
        }
        return result;
    }

    public static String patternToString(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        int patternSize = pattern.size();
        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) ((cell.getRow() * 3) + cell.getColumn());
        }
        return new String(res);
    }

    public static byte[] patternToHash(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return null;
        }
        int patternSize = pattern.size();
        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) ((cell.getRow() * 3) + cell.getColumn());
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(res);
        } catch (NoSuchAlgorithmException e) {
            return res;
        }
    }

    private String getSalt(int userId) {
        long salt = getLong(LOCK_PASSWORD_SALT_KEY, 0L, userId);
        if (salt == 0) {
            try {
                salt = SecureRandom.getInstance("SHA1PRNG").nextLong();
                setLong(LOCK_PASSWORD_SALT_KEY, salt, userId);
                Log.v(TAG, "Initialized lock password salt for user: " + userId);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Couldn't get SecureRandom number", e);
            }
        }
        return Long.toHexString(salt);
    }

    public byte[] passwordToHash(String password, int userId) {
        if (password == null) {
            return null;
        }
        String algo = null;
        try {
            byte[] saltedPassword = (password + getSalt(userId)).getBytes();
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(saltedPassword);
            algo = "MD5";
            byte[] md5 = MessageDigest.getInstance("MD5").digest(saltedPassword);
            byte[] hashed = (toHex(sha1) + toHex(md5)).getBytes();
            return hashed;
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Failed to encode string because of missing algorithm: " + algo);
            return null;
        }
    }

    private static String toHex(byte[] ary) {
        String ret = ProxyInfo.LOCAL_EXCL_LIST;
        for (int i = 0; i < ary.length; i++) {
            ret = (ret + "0123456789ABCDEF".charAt((ary[i] >> 4) & 15)) + "0123456789ABCDEF".charAt(ary[i] & 15);
        }
        return ret;
    }

    public boolean isLockPasswordEnabled() {
        long mode = getLong(PASSWORD_TYPE_KEY, 0L);
        long backupMode = getLong(PASSWORD_TYPE_ALTERNATE_KEY, 0L);
        boolean passwordEnabled = mode == 262144 || mode == Trace.TRACE_TAG_POWER || mode == 196608 || mode == 327680 || mode == 393216;
        boolean backupEnabled = backupMode == 262144 || backupMode == Trace.TRACE_TAG_POWER || backupMode == 196608 || backupMode == 327680 || backupMode == 393216;
        return savedPasswordExists() && (passwordEnabled || (usingBiometricWeak() && backupEnabled));
    }

    public boolean isLockPatternEnabled() {
        return isLockPatternEnabled(getCurrentOrCallingUserId());
    }

    public boolean isLockPatternEnabled(int userId) {
        boolean backupEnabled = getLong(PASSWORD_TYPE_ALTERNATE_KEY, 0L, userId) == Trace.TRACE_TAG_BIONIC;
        if (getBoolean("lock_pattern_autolock", false, userId)) {
            if (getLong(PASSWORD_TYPE_KEY, 0L, userId) == Trace.TRACE_TAG_BIONIC) {
                return true;
            }
            if (usingBiometricWeak(userId) && backupEnabled) {
                return true;
            }
        }
        return false;
    }

    public boolean isBiometricWeakInstalled() {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            pm.getPackageInfo("com.android.facelock", 1);
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT) && getDevicePolicyManager().getCameraDisabled(null, getCurrentOrCallingUserId())) {
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    public void setBiometricWeakLivelinessEnabled(boolean enabled) {
        long newFlag;
        long currentFlag = getLong(Settings.Secure.LOCK_BIOMETRIC_WEAK_FLAGS, 0L);
        if (enabled) {
            newFlag = currentFlag | 1;
        } else {
            newFlag = currentFlag & (-2);
        }
        setLong(Settings.Secure.LOCK_BIOMETRIC_WEAK_FLAGS, newFlag);
    }

    public boolean isBiometricWeakLivelinessEnabled() {
        long currentFlag = getLong(Settings.Secure.LOCK_BIOMETRIC_WEAK_FLAGS, 0L);
        return (1 & currentFlag) != 0;
    }

    public void setLockPatternEnabled(boolean enabled) {
        setLockPatternEnabled(enabled, getCurrentOrCallingUserId());
    }

    public void setLockPatternEnabled(boolean enabled, int userHandle) {
        setBoolean("lock_pattern_autolock", enabled, userHandle);
    }

    public boolean isVisiblePatternEnabled() {
        return getBoolean("lock_pattern_visible_pattern", false);
    }

    public void setVisiblePatternEnabled(boolean enabled) {
        setBoolean("lock_pattern_visible_pattern", enabled);
        int userId = getCurrentOrCallingUserId();
        if (userId == 0) {
            IBinder service = ServiceManager.getService("mount");
            if (service == null) {
                Log.e(TAG, "Could not find the mount service to update the user info");
                return;
            }
            IMountService mountService = IMountService.Stub.asInterface(service);
            try {
                mountService.setField(StorageManager.PATTERN_VISIBLE_KEY, enabled ? WifiEnterpriseConfig.ENGINE_ENABLE : WifiEnterpriseConfig.ENGINE_DISABLE);
            } catch (RemoteException e) {
                Log.e(TAG, "Error changing pattern visible state", e);
            }
        }
    }

    public boolean isTactileFeedbackEnabled() {
        return Settings.System.getIntForUser(this.mContentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, -2) != 0;
    }

    public long setLockoutAttemptDeadline() {
        long deadline = SystemClock.elapsedRealtime() + 30000;
        setLong(LOCKOUT_ATTEMPT_DEADLINE, deadline);
        return deadline;
    }

    public long getLockoutAttemptDeadline() {
        long deadline = getLong(LOCKOUT_ATTEMPT_DEADLINE, 0L);
        long now = SystemClock.elapsedRealtime();
        if (deadline < now || deadline > 30000 + now) {
            return 0L;
        }
        return deadline;
    }

    public boolean isPermanentlyLocked() {
        return getBoolean(LOCKOUT_PERMANENT_KEY, false);
    }

    public void setPermanentlyLocked(boolean locked) {
        setBoolean(LOCKOUT_PERMANENT_KEY, locked);
    }

    public boolean isEmergencyCallCapable() {
        return this.mContext.getResources().getBoolean(R.bool.config_voice_capable);
    }

    public boolean isPukUnlockScreenEnable() {
        return this.mContext.getResources().getBoolean(R.bool.config_enable_puk_unlock_screen);
    }

    public boolean isEmergencyCallEnabledWhileSimLocked() {
        return this.mContext.getResources().getBoolean(R.bool.config_enable_emergency_call_while_sim_locked);
    }

    public AlarmManager.AlarmClockInfo getNextAlarm() {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        return alarmManager.getNextAlarmClock(-2);
    }

    private boolean getBoolean(String secureSettingKey, boolean defaultValue, int userId) {
        try {
            return getLockSettings().getBoolean(secureSettingKey, defaultValue, userId);
        } catch (RemoteException e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(String secureSettingKey, boolean defaultValue) {
        return getBoolean(secureSettingKey, defaultValue, getCurrentOrCallingUserId());
    }

    private void setBoolean(String secureSettingKey, boolean enabled, int userId) {
        try {
            getLockSettings().setBoolean(secureSettingKey, enabled, userId);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't write boolean " + secureSettingKey + re);
        }
    }

    private void setBoolean(String secureSettingKey, boolean enabled) {
        setBoolean(secureSettingKey, enabled, getCurrentOrCallingUserId());
    }

    public int[] getAppWidgets() {
        return getAppWidgets(-2);
    }

    private int[] getAppWidgets(int userId) {
        String appWidgetIdString = Settings.Secure.getStringForUser(this.mContentResolver, Settings.Secure.LOCK_SCREEN_APPWIDGET_IDS, userId);
        if (appWidgetIdString == null || appWidgetIdString.length() <= 0) {
            return new int[0];
        }
        String[] appWidgetStringIds = appWidgetIdString.split(",");
        int[] appWidgetIds = new int[appWidgetStringIds.length];
        for (int i = 0; i < appWidgetStringIds.length; i++) {
            String appWidget = appWidgetStringIds[i];
            try {
                appWidgetIds[i] = Integer.decode(appWidget).intValue();
            } catch (NumberFormatException e) {
                Log.d(TAG, "Error when parsing widget id " + appWidget);
                return null;
            }
        }
        return appWidgetIds;
    }

    private static String combineStrings(int[] list, String separator) {
        int listLength = list.length;
        switch (listLength) {
            case 0:
                return ProxyInfo.LOCAL_EXCL_LIST;
            case 1:
                return Integer.toString(list[0]);
            default:
                int strLength = 0;
                int separatorLength = separator.length();
                String[] stringList = new String[list.length];
                for (int i = 0; i < listLength; i++) {
                    stringList[i] = Integer.toString(list[i]);
                    strLength += stringList[i].length();
                    if (i < listLength - 1) {
                        strLength += separatorLength;
                    }
                }
                StringBuilder sb = new StringBuilder(strLength);
                for (int i2 = 0; i2 < listLength; i2++) {
                    sb.append(list[i2]);
                    if (i2 < listLength - 1) {
                        sb.append(separator);
                    }
                }
                return sb.toString();
        }
    }

    public void writeFallbackAppWidgetId(int appWidgetId) {
        Settings.Secure.putIntForUser(this.mContentResolver, Settings.Secure.LOCK_SCREEN_FALLBACK_APPWIDGET_ID, appWidgetId, -2);
    }

    public int getFallbackAppWidgetId() {
        return Settings.Secure.getIntForUser(this.mContentResolver, Settings.Secure.LOCK_SCREEN_FALLBACK_APPWIDGET_ID, 0, -2);
    }

    private void writeAppWidgets(int[] appWidgetIds) {
        Settings.Secure.putStringForUser(this.mContentResolver, Settings.Secure.LOCK_SCREEN_APPWIDGET_IDS, combineStrings(appWidgetIds, ","), -2);
    }

    public boolean addAppWidget(int widgetId, int index) {
        int[] widgets = getAppWidgets();
        if (widgets == null || index < 0 || index > widgets.length) {
            return false;
        }
        int[] newWidgets = new int[widgets.length + 1];
        int i = 0;
        int j = 0;
        while (i < newWidgets.length) {
            if (index == i) {
                newWidgets[i] = widgetId;
                i++;
            }
            if (i < newWidgets.length) {
                newWidgets[i] = widgets[j];
                j++;
            }
            i++;
        }
        writeAppWidgets(newWidgets);
        return true;
    }

    public boolean removeAppWidget(int widgetId) {
        int[] widgets = getAppWidgets();
        if (widgets.length == 0) {
            return false;
        }
        int[] newWidgets = new int[widgets.length - 1];
        int j = 0;
        for (int i = 0; i < widgets.length; i++) {
            if (widgets[i] != widgetId) {
                if (j >= newWidgets.length) {
                    return false;
                }
                newWidgets[j] = widgets[i];
                j++;
            }
        }
        writeAppWidgets(newWidgets);
        return true;
    }

    private long getLong(String secureSettingKey, long defaultValue, int userHandle) {
        try {
            return getLockSettings().getLong(secureSettingKey, defaultValue, userHandle);
        } catch (RemoteException e) {
            return defaultValue;
        }
    }

    private long getLong(String secureSettingKey, long defaultValue) {
        try {
            return getLockSettings().getLong(secureSettingKey, defaultValue, getCurrentOrCallingUserId());
        } catch (RemoteException e) {
            return defaultValue;
        }
    }

    private void setLong(String secureSettingKey, long value) {
        setLong(secureSettingKey, value, getCurrentOrCallingUserId());
    }

    private void setLong(String secureSettingKey, long value, int userHandle) {
        try {
            getLockSettings().setLong(secureSettingKey, value, userHandle);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't write long " + secureSettingKey + re);
        }
    }

    private String getString(String secureSettingKey) {
        return getString(secureSettingKey, getCurrentOrCallingUserId());
    }

    private String getString(String secureSettingKey, int userHandle) {
        try {
            return getLockSettings().getString(secureSettingKey, null, userHandle);
        } catch (RemoteException e) {
            return null;
        }
    }

    private void setString(String secureSettingKey, String value, int userHandle) {
        try {
            getLockSettings().setString(secureSettingKey, value, userHandle);
        } catch (RemoteException re) {
            Log.e(TAG, "Couldn't write string " + secureSettingKey + re);
        }
    }

    public boolean isSecure() {
        return isSecure(getCurrentOrCallingUserId());
    }

    public boolean isSecure(int userId) {
        long mode = getKeyguardStoredPasswordQuality(userId);
        boolean isPattern = mode == Trace.TRACE_TAG_BIONIC;
        boolean isPassword = mode == Trace.TRACE_TAG_POWER || mode == 196608 || mode == 262144 || mode == 327680 || mode == 393216;
        if (isPattern && isLockPatternEnabled(userId) && savedPatternExists(userId)) {
            return true;
        }
        return isPassword && savedPasswordExists(userId);
    }

    public void updateEmergencyCallButtonState(Button button, boolean shown, boolean showIcon) {
        int textId;
        if (isEmergencyCallCapable() && shown) {
            button.setVisibility(0);
            if (isInCall()) {
                textId = R.string.lockscreen_return_to_call;
                int phoneCallIcon = showIcon ? 17301636 : 0;
                button.setCompoundDrawablesWithIntrinsicBounds(phoneCallIcon, 0, 0, 0);
            } else {
                textId = R.string.lockscreen_emergency_call;
                int emergencyIcon = showIcon ? R.drawable.ic_emergency : 0;
                button.setCompoundDrawablesWithIntrinsicBounds(emergencyIcon, 0, 0, 0);
            }
            button.setText(textId);
            return;
        }
        button.setVisibility(8);
    }

    public void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    public boolean isInCall() {
        return getTelecommManager().isInCall();
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) this.mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    private void finishBiometricWeak(int userId) {
        setBoolean(BIOMETRIC_WEAK_EVER_CHOSEN_KEY, true, userId);
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.SetupEndScreen");
        this.mContext.startActivityAsUser(intent, new UserHandle(userId));
    }

    public void setPowerButtonInstantlyLocks(boolean enabled) {
        setBoolean(LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS, enabled);
    }

    public boolean getPowerButtonInstantlyLocks() {
        return getBoolean(LOCKSCREEN_POWER_BUTTON_INSTANTLY_LOCKS, true);
    }

    public static boolean isSafeModeEnabled() {
        try {
            return IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE)).isSafeModeEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean hasWidgetsEnabledInKeyguard(int userid) {
        int[] widgets = getAppWidgets(userid);
        for (int i : widgets) {
            if (i > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean getWidgetsEnabled() {
        return getWidgetsEnabled(getCurrentOrCallingUserId());
    }

    public boolean getWidgetsEnabled(int userId) {
        return getBoolean(LOCKSCREEN_WIDGETS_ENABLED, false, userId);
    }

    public void setWidgetsEnabled(boolean enabled) {
        setWidgetsEnabled(enabled, getCurrentOrCallingUserId());
    }

    public void setWidgetsEnabled(boolean enabled, int userId) {
        setBoolean(LOCKSCREEN_WIDGETS_ENABLED, enabled, userId);
    }

    public void setEnabledTrustAgents(Collection<ComponentName> activeTrustAgents) {
        setEnabledTrustAgents(activeTrustAgents, getCurrentOrCallingUserId());
    }

    public List<ComponentName> getEnabledTrustAgents() {
        return getEnabledTrustAgents(getCurrentOrCallingUserId());
    }

    public void setEnabledTrustAgents(Collection<ComponentName> activeTrustAgents, int userId) {
        StringBuilder sb = new StringBuilder();
        for (ComponentName cn : activeTrustAgents) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(cn.flattenToShortString());
        }
        setString(ENABLED_TRUST_AGENTS, sb.toString(), userId);
        getTrustManager().reportEnabledTrustAgentsChanged(getCurrentOrCallingUserId());
    }

    public List<ComponentName> getEnabledTrustAgents(int userId) {
        String serialized = getString(ENABLED_TRUST_AGENTS, userId);
        if (TextUtils.isEmpty(serialized)) {
            return null;
        }
        String[] split = serialized.split(",");
        ArrayList<ComponentName> activeTrustAgents = new ArrayList<>(split.length);
        for (String s : split) {
            if (!TextUtils.isEmpty(s)) {
                activeTrustAgents.add(ComponentName.unflattenFromString(s));
            }
        }
        return activeTrustAgents;
    }

    public void requireCredentialEntry(int userId) {
        getTrustManager().reportRequireCredentialEntry(userId);
    }

    private void onAfterChangingPassword(int userHandle) {
        getTrustManager().reportEnabledTrustAgentsChanged(userHandle);
    }

    public boolean isCredentialRequiredToDecrypt(boolean defaultValue) {
        int value = Settings.Global.getInt(this.mContentResolver, Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, -1);
        return value == -1 ? defaultValue : value != 0;
    }

    public void setCredentialRequiredToDecrypt(boolean required) {
        if (getCurrentUser() != 0) {
            Log.w(TAG, "Only device owner may call setCredentialRequiredForDecrypt()");
        } else {
            Settings.Global.putInt(this.mContext.getContentResolver(), Settings.Global.REQUIRE_PASSWORD_TO_DECRYPT, required ? 1 : 0);
        }
    }
}
