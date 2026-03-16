package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LockSettingsStorage;
import java.util.Arrays;
import java.util.List;

public class LockSettingsService extends ILockSettings.Stub {
    private static final String PERMISSION = "android.permission.ACCESS_KEYGUARD_SECURE_STORAGE";
    private static final String TAG = "LockSettingsService";
    private final Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private final LockSettingsStorage mStorage;
    private static final String[] VALID_SETTINGS = {"lockscreen.lockedoutpermanently", "lockscreen.lockoutattemptdeadline", "lockscreen.patterneverchosen", "lockscreen.password_type", "lockscreen.password_type_alternate", "lockscreen.password_salt", "lockscreen.disabled", "lockscreen.options", "lockscreen.biometric_weak_fallback", "lockscreen.biometricweakeverchosen", "lockscreen.power_button_instantly_locks", "lockscreen.passwordhistory", "lock_pattern_autolock", "lock_biometric_weak_flags", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled"};
    private static final String[] READ_PROFILE_PROTECTED_SETTINGS = {"lock_screen_owner_info_enabled", "lock_screen_owner_info"};
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Throwable {
            if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                int userSysUid = UserHandle.getUid(userHandle, 1000);
                KeyStore ks = KeyStore.getInstance();
                ks.resetUid(userSysUid);
                UserManager um = (UserManager) LockSettingsService.this.mContext.getSystemService("user");
                UserInfo parentInfo = um.getProfileParent(userHandle);
                if (parentInfo != null) {
                    int parentSysUid = UserHandle.getUid(parentInfo.id, 1000);
                    ks.syncUid(parentSysUid, userSysUid);
                    return;
                }
                return;
            }
            if ("android.intent.action.USER_STARTING".equals(intent.getAction())) {
                LockSettingsService.this.mStorage.prefetchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
            }
        }
    };
    private boolean mFirstCallToVold = true;

    public LockSettingsService(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_STARTING");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mStorage = new LockSettingsStorage(context, new LockSettingsStorage.Callback() {
            @Override
            public void initialize(SQLiteDatabase db) {
                boolean lockScreenDisable = SystemProperties.getBoolean("ro.lockscreen.disable.default", false);
                if (lockScreenDisable) {
                    LockSettingsService.this.mStorage.writeKeyValue(db, "lockscreen.disabled", "1", 0);
                }
            }
        });
    }

    public void systemReady() throws Throwable {
        migrateOldData();
        this.mStorage.prefetchUser(0);
    }

    private void migrateOldData() {
        try {
            if (getString("migrated", null, 0) == null) {
                ContentResolver cr = this.mContext.getContentResolver();
                String[] arr$ = VALID_SETTINGS;
                for (String validSetting : arr$) {
                    String value = Settings.Secure.getString(cr, validSetting);
                    if (value != null) {
                        setString(validSetting, value, 0);
                    }
                }
                setString("migrated", "true", 0);
                Slog.i(TAG, "Migrated lock settings to new location");
            }
            if (getString("migrated_user_specific", null, 0) == null) {
                UserManager um = (UserManager) this.mContext.getSystemService("user");
                ContentResolver cr2 = this.mContext.getContentResolver();
                List<UserInfo> users = um.getUsers();
                for (int user = 0; user < users.size(); user++) {
                    int userId = users.get(user).id;
                    String ownerInfo = Settings.Secure.getStringForUser(cr2, "lock_screen_owner_info", userId);
                    if (ownerInfo != null) {
                        setString("lock_screen_owner_info", ownerInfo, userId);
                        Settings.Secure.putStringForUser(cr2, ownerInfo, "", userId);
                    }
                    try {
                        int ivalue = Settings.Secure.getIntForUser(cr2, "lock_screen_owner_info_enabled", userId);
                        boolean enabled = ivalue != 0;
                        setLong("lock_screen_owner_info_enabled", enabled ? 1L : 0L, userId);
                    } catch (Settings.SettingNotFoundException e) {
                        if (!TextUtils.isEmpty(ownerInfo)) {
                            setLong("lock_screen_owner_info_enabled", 1L, userId);
                        }
                    }
                    Settings.Secure.putIntForUser(cr2, "lock_screen_owner_info_enabled", 0, userId);
                }
                setString("migrated_user_specific", "true", 0);
                Slog.i(TAG, "Migrated per-user lock settings to new location");
            }
        } catch (RemoteException re) {
            Slog.e(TAG, "Unable to migrate old data", re);
        }
    }

    private final void checkWritePermission(int userId) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsWrite");
    }

    private final void checkPasswordReadPermission(int userId) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsRead");
    }

    private final void checkReadPermission(String requestedKey, int userId) {
        int callingUid = Binder.getCallingUid();
        for (int i = 0; i < READ_PROFILE_PROTECTED_SETTINGS.length; i++) {
            String key = READ_PROFILE_PROTECTED_SETTINGS[i];
            if (key.equals(requestedKey) && this.mContext.checkCallingOrSelfPermission("android.permission.READ_PROFILE") != 0) {
                throw new SecurityException("uid=" + callingUid + " needs permission android.permission.READ_PROFILE to read " + requestedKey + " for user " + userId);
            }
        }
    }

    public void setBoolean(String key, boolean value, int userId) throws RemoteException {
        checkWritePermission(userId);
        setStringUnchecked(key, userId, value ? "1" : "0");
    }

    public void setLong(String key, long value, int userId) throws RemoteException {
        checkWritePermission(userId);
        setStringUnchecked(key, userId, Long.toString(value));
    }

    public void setString(String key, String value, int userId) throws RemoteException {
        checkWritePermission(userId);
        setStringUnchecked(key, userId, value);
    }

    private void setStringUnchecked(String key, int userId, String value) {
        this.mStorage.writeKeyValue(key, value, userId);
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) throws RemoteException {
        checkReadPermission(key, userId);
        String value = this.mStorage.readKeyValue(key, null, userId);
        return TextUtils.isEmpty(value) ? defaultValue : value.equals("1") || value.equals("true");
    }

    public long getLong(String key, long defaultValue, int userId) throws RemoteException {
        checkReadPermission(key, userId);
        String value = this.mStorage.readKeyValue(key, null, userId);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        long defaultValue2 = Long.parseLong(value);
        return defaultValue2;
    }

    public String getString(String key, String defaultValue, int userId) throws RemoteException {
        checkReadPermission(key, userId);
        return this.mStorage.readKeyValue(key, defaultValue, userId);
    }

    public boolean havePassword(int userId) throws RemoteException {
        return this.mStorage.hasPassword(userId);
    }

    public boolean havePattern(int userId) throws RemoteException {
        return this.mStorage.hasPattern(userId);
    }

    private void maybeUpdateKeystore(String password, int userHandle) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        KeyStore ks = KeyStore.getInstance();
        List<UserInfo> profiles = um.getProfiles(userHandle);
        boolean shouldReset = TextUtils.isEmpty(password);
        if (userHandle == 0 && profiles.size() == 1 && !ks.isEmpty()) {
            shouldReset = false;
        }
        for (UserInfo pi : profiles) {
            int profileUid = UserHandle.getUid(pi.id, 1000);
            if (shouldReset) {
                ks.resetUid(profileUid);
            } else {
                ks.passwordUid(password, profileUid);
            }
        }
    }

    public void setLockPattern(String pattern, int userId) throws Throwable {
        checkWritePermission(userId);
        maybeUpdateKeystore(pattern, userId);
        byte[] hash = LockPatternUtils.patternToHash(LockPatternUtils.stringToPattern(pattern));
        this.mStorage.writePatternHash(hash, userId);
    }

    public void setLockPassword(String password, int userId) throws Throwable {
        checkWritePermission(userId);
        maybeUpdateKeystore(password, userId);
        this.mStorage.writePasswordHash(this.mLockPatternUtils.passwordToHash(password, userId), userId);
    }

    public boolean checkPattern(String pattern, int userId) throws Throwable {
        checkPasswordReadPermission(userId);
        byte[] hash = LockPatternUtils.patternToHash(LockPatternUtils.stringToPattern(pattern));
        byte[] storedHash = this.mStorage.readPatternHash(userId);
        if (storedHash == null) {
            return true;
        }
        boolean matched = Arrays.equals(hash, storedHash);
        if (matched && !TextUtils.isEmpty(pattern)) {
            maybeUpdateKeystore(pattern, userId);
            return matched;
        }
        return matched;
    }

    public boolean checkPassword(String password, int userId) throws Throwable {
        checkPasswordReadPermission(userId);
        byte[] hash = this.mLockPatternUtils.passwordToHash(password, userId);
        byte[] storedHash = this.mStorage.readPasswordHash(userId);
        if (storedHash == null) {
            return true;
        }
        boolean matched = Arrays.equals(hash, storedHash);
        if (matched && !TextUtils.isEmpty(password)) {
            maybeUpdateKeystore(password, userId);
            return matched;
        }
        return matched;
    }

    public boolean checkVoldPassword(int userId) throws RemoteException {
        if (!this.mFirstCallToVold) {
            return false;
        }
        this.mFirstCallToVold = false;
        checkPasswordReadPermission(userId);
        IMountService service = getMountService();
        String password = service.getPassword();
        service.clearPassword();
        if (password == null) {
            return false;
        }
        try {
            if (this.mLockPatternUtils.isLockPatternEnabled()) {
                if (checkPattern(password, userId)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        try {
            if (this.mLockPatternUtils.isLockPasswordEnabled()) {
                return checkPassword(password, userId);
            }
            return false;
        } catch (Exception e2) {
            return false;
        }
    }

    public void removeUser(int userId) {
        checkWritePermission(userId);
        this.mStorage.removeUser(userId);
        KeyStore ks = KeyStore.getInstance();
        int userUid = UserHandle.getUid(userId, 1000);
        ks.resetUid(userUid);
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }
}
