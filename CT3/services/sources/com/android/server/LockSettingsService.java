package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.backup.BackupManager;
import android.app.trust.IStrongAuthTracker;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.keystore.KeyProtection;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.LockSettingsStorage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import libcore.util.HexEncoding;

public class LockSettingsService extends ILockSettings.Stub {
    private static final boolean DEBUG = false;
    private static final int FBE_ENCRYPTED_NOTIFICATION = 0;
    private static final String PERMISSION = "android.permission.ACCESS_KEYGUARD_SECURE_STORAGE";
    private static final int PROFILE_KEY_IV_SIZE = 12;
    private static final String[] READ_CONTACTS_PROTECTED_SETTINGS;
    private static final String[] READ_PASSWORD_PROTECTED_SETTINGS;
    private static final String SEPARATE_PROFILE_CHALLENGE_KEY = "lockscreen.profilechallenge";
    private static final String[] SETTINGS_TO_BACKUP;
    private static final String TAG = "LockSettingsService";
    private static final String[] VALID_SETTINGS;
    private final Context mContext;
    private IGateKeeperService mGateKeeperService;
    private LockPatternUtils mLockPatternUtils;
    private NotificationManager mNotificationManager;
    private final LockSettingsStorage mStorage;
    private final LockSettingsStrongAuth mStrongAuth;
    private final SynchronizedStrongAuthTracker mStrongAuthTracker;
    private UserManager mUserManager;
    private static final int[] SYSTEM_CREDENTIAL_UIDS = {1010, 1016, 0, 1000};
    private static final Intent ACTION_NULL = new Intent("android.intent.action.MAIN");
    private final Object mSeparateChallengeLock = new Object();
    private final KeyStore mKeyStore = KeyStore.getInstance();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Throwable {
            int userHandle;
            if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                int userHandle2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                if (userHandle2 > 0) {
                    LockSettingsService.this.removeUser(userHandle2, true);
                }
                KeyStore ks = KeyStore.getInstance();
                UserInfo parentInfo = LockSettingsService.this.mUserManager.getProfileParent(userHandle2);
                int parentHandle = parentInfo != null ? parentInfo.id : -1;
                ks.onUserAdded(userHandle2, parentHandle);
                return;
            }
            if ("android.intent.action.USER_STARTING".equals(intent.getAction())) {
                LockSettingsService.this.mStorage.prefetchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
            } else {
                if (!"android.intent.action.USER_REMOVED".equals(intent.getAction()) || (userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0)) <= 0) {
                    return;
                }
                LockSettingsService.this.removeUser(userHandle, false);
            }
        }
    };
    private final Handler mHandler = new Handler();
    private boolean mFirstCallToVold = true;

    private interface CredentialUtil {
        String adjustForKeystore(String str);

        void setCredential(String str, String str2, int i) throws RemoteException;

        byte[] toHash(String str, int i);
    }

    static {
        ACTION_NULL.addCategory("android.intent.category.HOME");
        VALID_SETTINGS = new String[]{"lockscreen.lockedoutpermanently", "lockscreen.lockoutattemptdeadline", "lockscreen.patterneverchosen", "lockscreen.password_type", "lockscreen.password_type_alternate", "lockscreen.password_salt", "lockscreen.disabled", "lockscreen.options", "lockscreen.biometric_weak_fallback", "lockscreen.weak_fallback", "lockscreen.weak_fallback_for", "lockscreen.voice_weak_fallback_set", "lockscreen.biometricweakeverchosen", "lockscreen.power_button_instantly_locks", "lockscreen.passwordhistory", "lock_pattern_autolock", "lock_biometric_weak_flags", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled"};
        READ_CONTACTS_PROTECTED_SETTINGS = new String[]{"lock_screen_owner_info_enabled", "lock_screen_owner_info"};
        READ_PASSWORD_PROTECTED_SETTINGS = new String[]{"lockscreen.password_salt", "lockscreen.passwordhistory", "lockscreen.password_type", SEPARATE_PROFILE_CHALLENGE_KEY};
        SETTINGS_TO_BACKUP = new String[]{"lock_screen_owner_info_enabled", "lock_screen_owner_info"};
    }

    public static final class Lifecycle extends SystemService {
        private LockSettingsService mLockSettingsService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            AndroidKeyStoreProvider.install();
            this.mLockSettingsService = new LockSettingsService(getContext());
            publishBinderService("lock_settings", this.mLockSettingsService);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mLockSettingsService.maybeShowEncryptionNotifications();
            } else {
                if (phase == 1000) {
                }
            }
        }

        @Override
        public void onUnlockUser(int userHandle) {
            this.mLockSettingsService.onUnlockUser(userHandle);
        }

        @Override
        public void onCleanupUser(int userHandle) {
            this.mLockSettingsService.onCleanupUser(userHandle);
        }
    }

    private class SynchronizedStrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        public SynchronizedStrongAuthTracker(Context context) {
            super(context);
        }

        protected void handleStrongAuthRequiredChanged(int strongAuthFlags, int userId) {
            synchronized (this) {
                super.handleStrongAuthRequiredChanged(strongAuthFlags, userId);
            }
        }

        public int getStrongAuthForUser(int userId) {
            int strongAuthForUser;
            synchronized (this) {
                strongAuthForUser = super.getStrongAuthForUser(userId);
            }
            return strongAuthForUser;
        }

        void register() {
            LockSettingsService.this.mStrongAuth.registerStrongAuthTracker(this.mStub);
        }
    }

    public void tieManagedProfileLockIfNecessary(int managedUserId, String managedUserPassword) {
        if (!UserManager.get(this.mContext).getUserInfo(managedUserId).isManagedProfile() || this.mLockPatternUtils.isSeparateProfileChallengeEnabled(managedUserId) || this.mStorage.hasChildProfileLock(managedUserId)) {
            return;
        }
        int parentId = this.mUserManager.getProfileParent(managedUserId).id;
        if (!this.mStorage.hasPassword(parentId) && !this.mStorage.hasPattern(parentId)) {
            return;
        }
        byte[] bArr = new byte[0];
        try {
            byte[] randomLockSeed = SecureRandom.getInstance("SHA1PRNG").generateSeed(40);
            String newPassword = String.valueOf(HexEncoding.encode(randomLockSeed));
            setLockPasswordInternal(newPassword, managedUserPassword, managedUserId);
            setLong("lockscreen.password_type", 327680L, managedUserId);
            tieProfileLockToParent(managedUserId, newPassword);
        } catch (RemoteException | NoSuchAlgorithmException e) {
            Slog.e(TAG, "Fail to tie managed profile", e);
        }
    }

    public LockSettingsService(Context context) {
        this.mContext = context;
        this.mStrongAuth = new LockSettingsStrongAuth(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_STARTING");
        filter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mStorage = new LockSettingsStorage(context, new LockSettingsStorage.Callback() {
            @Override
            public void initialize(SQLiteDatabase db) {
                boolean lockScreenDisable = SystemProperties.getBoolean("ro.lockscreen.disable.default", false);
                if (!lockScreenDisable) {
                    return;
                }
                LockSettingsService.this.mStorage.writeKeyValue(db, "lockscreen.disabled", "1", 0);
            }
        });
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mStrongAuthTracker = new SynchronizedStrongAuthTracker(this.mContext);
        this.mStrongAuthTracker.register();
    }

    private void maybeShowEncryptionNotifications() {
        List<UserInfo> users = this.mUserManager.getUsers();
        for (int i = 0; i < users.size(); i++) {
            UserInfo user = users.get(i);
            UserHandle userHandle = user.getUserHandle();
            if (!this.mUserManager.isUserUnlockingOrUnlocked(userHandle)) {
                if (!user.isManagedProfile()) {
                    showEncryptionNotification(userHandle);
                } else {
                    UserInfo parent = this.mUserManager.getProfileParent(user.id);
                    if (parent != null && this.mUserManager.isUserUnlockingOrUnlocked(parent.getUserHandle()) && !this.mUserManager.isQuietModeEnabled(userHandle)) {
                        showEncryptionNotificationForProfile(userHandle);
                    }
                }
            }
        }
    }

    private void showEncryptionNotificationForProfile(UserHandle user) {
        Resources r = this.mContext.getResources();
        CharSequence title = r.getText(R.string.me);
        CharSequence message = r.getText(R.string.media_route_chooser_title);
        CharSequence detail = r.getText(R.string.media_route_chooser_searching);
        KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
        Intent unlockIntent = km.createConfirmDeviceCredentialIntent(null, null, user.getIdentifier());
        if (unlockIntent == null) {
            return;
        }
        unlockIntent.setFlags(276824064);
        PendingIntent intent = PendingIntent.getActivity(this.mContext, 0, unlockIntent, 134217728);
        showEncryptionNotification(user, title, message, detail, intent);
    }

    private void showEncryptionNotification(UserHandle user) {
        Resources r = this.mContext.getResources();
        CharSequence title = r.getText(R.string.me);
        CharSequence message = r.getText(R.string.media_route_button_content_description);
        CharSequence detail = r.getText(R.string.media_route_chooser_extended_settings);
        PendingIntent intent = PendingIntent.getBroadcast(this.mContext, 0, ACTION_NULL, 134217728);
        showEncryptionNotification(user, title, message, detail, intent);
    }

    private void showEncryptionNotification(UserHandle user, CharSequence title, CharSequence message, CharSequence detail, PendingIntent intent) {
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            Notification notification = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_expand_more_48dp).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setPriority(2).setColor(this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(message).setSubText(detail).setVisibility(1).setContentIntent(intent).build();
            this.mNotificationManager.notifyAsUser(null, 0, notification, user);
        }
    }

    public void hideEncryptionNotification(UserHandle userHandle) {
        this.mNotificationManager.cancelAsUser(null, 0, userHandle);
    }

    public void onCleanupUser(int userId) {
        hideEncryptionNotification(new UserHandle(userId));
    }

    public void onUnlockUser(final int userId) {
        hideEncryptionNotification(new UserHandle(userId));
        if (this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    LockSettingsService.this.tieManagedProfileLockIfNecessary(userId, null);
                }
            });
        }
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        for (int i = 0; i < profiles.size(); i++) {
            UserInfo profile = profiles.get(i);
            if (profile.isManagedProfile()) {
                UserHandle userHandle = profile.getUserHandle();
                if (!this.mUserManager.isUserUnlockingOrUnlocked(userHandle) && !this.mUserManager.isQuietModeEnabled(userHandle)) {
                    showEncryptionNotificationForProfile(userHandle);
                }
            }
        }
    }

    public void systemReady() throws Throwable {
        migrateOldData();
        try {
            getGateKeeperService();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failure retrieving IGateKeeperService", e);
        }
        this.mStorage.prefetchUser(0);
    }

    private void migrateOldData() {
        try {
            if (getString("migrated", null, 0) == null) {
                ContentResolver cr = this.mContext.getContentResolver();
                for (String validSetting : VALID_SETTINGS) {
                    String value = Settings.Secure.getString(cr, validSetting);
                    if (value != null) {
                        setString(validSetting, value, 0);
                    }
                }
                setString("migrated", "true", 0);
                Slog.i(TAG, "Migrated lock settings to new location");
            }
            if (getString("migrated_user_specific", null, 0) == null) {
                ContentResolver cr2 = this.mContext.getContentResolver();
                List<UserInfo> users = this.mUserManager.getUsers();
                for (int user = 0; user < users.size(); user++) {
                    int userId = users.get(user).id;
                    String ownerInfo = Settings.Secure.getStringForUser(cr2, "lock_screen_owner_info", userId);
                    if (!TextUtils.isEmpty(ownerInfo)) {
                        setString("lock_screen_owner_info", ownerInfo, userId);
                        Settings.Secure.putStringForUser(cr2, "lock_screen_owner_info", "", userId);
                    }
                    try {
                        int ivalue = Settings.Secure.getIntForUser(cr2, "lock_screen_owner_info_enabled", userId);
                        boolean enabled = ivalue != 0;
                        setLong("lock_screen_owner_info_enabled", enabled ? 1 : 0, userId);
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
            if (getString("migrated_biometric_weak", null, 0) == null) {
                List<UserInfo> users2 = this.mUserManager.getUsers();
                for (int i = 0; i < users2.size(); i++) {
                    int userId2 = users2.get(i).id;
                    long type = getLong("lockscreen.password_type", 0L, userId2);
                    long alternateType = getLong("lockscreen.password_type_alternate", 0L, userId2);
                    if (type == 32768) {
                        setLong("lockscreen.password_type", alternateType, userId2);
                    }
                    setLong("lockscreen.password_type_alternate", 0L, userId2);
                }
                setString("migrated_biometric_weak", "true", 0);
                Slog.i(TAG, "Migrated biometric weak to use the fallback instead");
            }
            if (getString("migrated_lockscreen_disabled", null, 0) == null) {
                List<UserInfo> users3 = this.mUserManager.getUsers();
                int userCount = users3.size();
                int switchableUsers = 0;
                for (int i2 = 0; i2 < userCount; i2++) {
                    if (users3.get(i2).supportsSwitchTo()) {
                        switchableUsers++;
                    }
                }
                if (switchableUsers > 1) {
                    for (int i3 = 0; i3 < userCount; i3++) {
                        int id = users3.get(i3).id;
                        if (getBoolean("lockscreen.disabled", false, id)) {
                            setBoolean("lockscreen.disabled", false, id);
                        }
                    }
                }
                setString("migrated_lockscreen_disabled", "true", 0);
                Slog.i(TAG, "Migrated lockscreen disabled flag");
            }
            List<UserInfo> users4 = this.mUserManager.getUsers();
            for (int i4 = 0; i4 < users4.size(); i4++) {
                UserInfo userInfo = users4.get(i4);
                if (userInfo.isManagedProfile() && this.mStorage.hasChildProfileLock(userInfo.id)) {
                    long quality = getLong("lockscreen.password_type", 0L, userInfo.id);
                    if (quality == 0) {
                        Slog.i(TAG, "Migrated tied profile lock type");
                        setLong("lockscreen.password_type", 327680L, userInfo.id);
                    } else if (quality != 327680) {
                        Slog.e(TAG, "Invalid tied profile lock type: " + quality);
                    }
                }
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
        for (int i = 0; i < READ_CONTACTS_PROTECTED_SETTINGS.length; i++) {
            String key = READ_CONTACTS_PROTECTED_SETTINGS[i];
            if (key.equals(requestedKey) && this.mContext.checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
                throw new SecurityException("uid=" + callingUid + " needs permission android.permission.READ_CONTACTS to read " + requestedKey + " for user " + userId);
            }
        }
        for (int i2 = 0; i2 < READ_PASSWORD_PROTECTED_SETTINGS.length; i2++) {
            String key2 = READ_PASSWORD_PROTECTED_SETTINGS[i2];
            if (key2.equals(requestedKey) && this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
                throw new SecurityException("uid=" + callingUid + " needs permission " + PERMISSION + " to read " + requestedKey + " for user " + userId);
            }
        }
    }

    public boolean getSeparateProfileChallengeEnabled(int userId) throws RemoteException {
        boolean z;
        checkReadPermission(SEPARATE_PROFILE_CHALLENGE_KEY, userId);
        synchronized (this.mSeparateChallengeLock) {
            z = getBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, false, userId);
        }
        return z;
    }

    public void setSeparateProfileChallengeEnabled(int userId, boolean enabled, String managedUserPassword) throws RemoteException {
        checkWritePermission(userId);
        synchronized (this.mSeparateChallengeLock) {
            setBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, enabled, userId);
            if (enabled) {
                this.mStorage.removeChildProfileLock(userId);
                removeKeystoreProfileKey(userId);
            } else {
                tieManagedProfileLockIfNecessary(userId, managedUserPassword);
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
        if (!ArrayUtils.contains(SETTINGS_TO_BACKUP, key)) {
            return;
        }
        BackupManager.dataChanged("com.android.providers.settings");
    }

    public boolean getBoolean(String key, boolean defaultValue, int userId) throws RemoteException {
        checkReadPermission(key, userId);
        String value = getStringUnchecked(key, null, userId);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        if (value.equals("1")) {
            return true;
        }
        boolean defaultValue2 = value.equals("true");
        return defaultValue2;
    }

    public long getLong(String key, long defaultValue, int userId) throws RemoteException {
        checkReadPermission(key, userId);
        String value = getStringUnchecked(key, null, userId);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        long defaultValue2 = Long.parseLong(value);
        return defaultValue2;
    }

    public String getString(String key, String defaultValue, int userId) throws RemoteException {
        checkReadPermission(key, userId);
        return getStringUnchecked(key, defaultValue, userId);
    }

    public String getStringUnchecked(String key, String defaultValue, int userId) {
        if ("lock_pattern_autolock".equals(key)) {
            long ident = Binder.clearCallingIdentity();
            try {
                return this.mLockPatternUtils.isLockPatternEnabled(userId) ? "1" : "0";
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        if ("legacy_lock_pattern_enabled".equals(key)) {
            key = "lock_pattern_autolock";
        }
        return this.mStorage.readKeyValue(key, defaultValue, userId);
    }

    public boolean havePassword(int userId) throws RemoteException {
        return this.mStorage.hasPassword(userId);
    }

    public boolean havePattern(int userId) throws RemoteException {
        return this.mStorage.hasPattern(userId);
    }

    private void setKeystorePassword(String password, int userHandle) {
        KeyStore ks = KeyStore.getInstance();
        ks.onUserPasswordChanged(userHandle, password);
    }

    private void unlockKeystore(String password, int userHandle) {
        KeyStore ks = KeyStore.getInstance();
        ks.unlock(userHandle, password);
    }

    private String getDecryptedPasswordForTiedProfile(int userId) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, InvalidKeyException, KeyStoreException, CertificateException, InvalidAlgorithmParameterException {
        byte[] storedData = this.mStorage.readChildProfileLock(userId);
        if (storedData == null) {
            throw new FileNotFoundException("Child profile lock file not found");
        }
        byte[] iv = Arrays.copyOfRange(storedData, 0, 12);
        byte[] encryptedPassword = Arrays.copyOfRange(storedData, 12, storedData.length);
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        SecretKey decryptionKey = (SecretKey) keyStore.getKey("profile_key_name_decrypt_" + userId, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, decryptionKey, new GCMParameterSpec(128, iv));
        byte[] decryptionResult = cipher.doFinal(encryptedPassword);
        return new String(decryptionResult, StandardCharsets.UTF_8);
    }

    private void unlockChildProfile(int profileHandle) throws RemoteException {
        try {
            doVerifyPassword(getDecryptedPasswordForTiedProfile(profileHandle), false, 0L, profileHandle);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            if (e instanceof FileNotFoundException) {
                Slog.i(TAG, "Child profile key not found");
            } else {
                Slog.e(TAG, "Failed to decrypt child profile key", e);
            }
        }
    }

    private void unlockUser(int userId, byte[] token, byte[] secret) {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            ActivityManagerNative.getDefault().unlockUser(userId, token, secret, new IProgressListener.Stub() {
                public void onStarted(int id, Bundle extras) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser started");
                }

                public void onProgress(int id, int progress, Bundle extras) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser progress " + progress);
                }

                public void onFinished(int id, Bundle extras) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser finished");
                    latch.countDown();
                }
            });
            try {
                latch.await(15L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                if (this.mUserManager.getUserInfo(userId).isManagedProfile()) {
                    return;
                }
                List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
                for (UserInfo pi : profiles) {
                    if (pi.isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(pi.id) && this.mStorage.hasChildProfileLock(pi.id)) {
                        unlockChildProfile(pi.id);
                    }
                }
            } catch (RemoteException e2) {
                Log.d(TAG, "Failed to unlock child profile", e2);
            }
        } catch (RemoteException e3) {
            throw e3.rethrowAsRuntimeException();
        }
    }

    private byte[] getCurrentHandle(int userId) throws Throwable {
        byte[] currentHandle;
        int currentHandleType = this.mStorage.getStoredCredentialType(userId);
        switch (currentHandleType) {
            case 1:
                LockSettingsStorage.CredentialHash credential = this.mStorage.readPatternHash(userId);
                if (credential != null) {
                    currentHandle = credential.hash;
                } else {
                    currentHandle = null;
                }
                break;
            case 2:
                LockSettingsStorage.CredentialHash credential2 = this.mStorage.readPasswordHash(userId);
                if (credential2 != null) {
                    currentHandle = credential2.hash;
                } else {
                    currentHandle = null;
                }
                break;
            default:
                currentHandle = null;
                break;
        }
        if (currentHandleType != -1 && currentHandle == null) {
            Slog.e(TAG, "Stored handle type [" + currentHandleType + "] but no handle available");
        }
        return currentHandle;
    }

    private void onUserLockChanged(int userId) throws Throwable {
        if (this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            return;
        }
        boolean isSecure = !this.mStorage.hasPassword(userId) ? this.mStorage.hasPattern(userId) : true;
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        int size = profiles.size();
        for (int i = 0; i < size; i++) {
            UserInfo profile = profiles.get(i);
            if (profile.isManagedProfile()) {
                int managedUserId = profile.id;
                if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(managedUserId)) {
                    if (isSecure) {
                        tieManagedProfileLockIfNecessary(managedUserId, null);
                    } else {
                        clearUserKeyProtection(managedUserId);
                        getGateKeeperService().clearSecureUserId(managedUserId);
                        this.mStorage.writePatternHash(null, managedUserId);
                        setKeystorePassword(null, managedUserId);
                        fixateNewestUserKeyAuth(managedUserId);
                        this.mStorage.removeChildProfileLock(managedUserId);
                        removeKeystoreProfileKey(managedUserId);
                    }
                }
            }
        }
    }

    private boolean isManagedProfileWithUnifiedLock(int userId) {
        return this.mUserManager.getUserInfo(userId).isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId);
    }

    private boolean isManagedProfileWithSeparatedLock(int userId) {
        if (this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            return this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId);
        }
        return false;
    }

    public void setLockPattern(String pattern, String savedCredential, int userId) throws RemoteException {
        checkWritePermission(userId);
        synchronized (this.mSeparateChallengeLock) {
            setLockPatternInternal(pattern, savedCredential, userId);
            setSeparateProfileChallengeEnabled(userId, true, null);
            notifyPasswordChanged(userId);
        }
    }

    private void setLockPatternInternal(String pattern, String savedCredential, int userId) throws Throwable {
        byte[] currentHandle = getCurrentHandle(userId);
        if (pattern == null) {
            clearUserKeyProtection(userId);
            getGateKeeperService().clearSecureUserId(userId);
            this.mStorage.writePatternHash(null, userId);
            setKeystorePassword(null, userId);
            fixateNewestUserKeyAuth(userId);
            onUserLockChanged(userId);
            notifyActivePasswordMetricsAvailable(null, userId);
            return;
        }
        if (isManagedProfileWithUnifiedLock(userId)) {
            try {
                savedCredential = getDecryptedPasswordForTiedProfile(userId);
            } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                if (e instanceof FileNotFoundException) {
                    Slog.i(TAG, "Child profile key not found");
                } else {
                    Slog.e(TAG, "Failed to decrypt child profile key", e);
                }
            }
        } else if (currentHandle == null) {
            if (savedCredential != null) {
                Slog.w(TAG, "Saved credential provided, but none stored");
            }
            savedCredential = null;
        }
        byte[] enrolledHandle = enrollCredential(currentHandle, savedCredential, pattern, userId);
        if (enrolledHandle != null) {
            LockSettingsStorage.CredentialHash willStore = new LockSettingsStorage.CredentialHash(enrolledHandle, 1);
            setUserKeyProtection(userId, pattern, doVerifyPattern(pattern, willStore, true, 0L, userId));
            this.mStorage.writePatternHash(enrolledHandle, userId);
            fixateNewestUserKeyAuth(userId);
            onUserLockChanged(userId);
            return;
        }
        throw new RemoteException("Failed to enroll pattern");
    }

    public void setLockPassword(String password, String savedCredential, int userId) throws RemoteException {
        checkWritePermission(userId);
        synchronized (this.mSeparateChallengeLock) {
            setLockPasswordInternal(password, savedCredential, userId);
            setSeparateProfileChallengeEnabled(userId, true, null);
            notifyPasswordChanged(userId);
        }
    }

    private void setLockPasswordInternal(String password, String savedCredential, int userId) throws Throwable {
        byte[] currentHandle = getCurrentHandle(userId);
        if (password == null) {
            clearUserKeyProtection(userId);
            getGateKeeperService().clearSecureUserId(userId);
            this.mStorage.writePasswordHash(null, userId);
            setKeystorePassword(null, userId);
            fixateNewestUserKeyAuth(userId);
            onUserLockChanged(userId);
            notifyActivePasswordMetricsAvailable(null, userId);
            return;
        }
        if (isManagedProfileWithUnifiedLock(userId)) {
            try {
                savedCredential = getDecryptedPasswordForTiedProfile(userId);
            } catch (FileNotFoundException e) {
                Slog.i(TAG, "Child profile key not found");
            } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                Slog.e(TAG, "Failed to decrypt child profile key", e2);
            }
        } else if (currentHandle == null) {
            if (savedCredential != null) {
                Slog.w(TAG, "Saved credential provided, but none stored");
            }
            savedCredential = null;
        }
        byte[] enrolledHandle = enrollCredential(currentHandle, savedCredential, password, userId);
        if (enrolledHandle != null) {
            LockSettingsStorage.CredentialHash willStore = new LockSettingsStorage.CredentialHash(enrolledHandle, 1);
            setUserKeyProtection(userId, password, doVerifyPassword(password, willStore, true, 0L, userId));
            this.mStorage.writePasswordHash(enrolledHandle, userId);
            fixateNewestUserKeyAuth(userId);
            onUserLockChanged(userId);
            return;
        }
        throw new RemoteException("Failed to enroll password");
    }

    private void tieProfileLockToParent(int userId, String password) throws Throwable {
        byte[] randomLockSeed = password.getBytes(StandardCharsets.UTF_8);
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.setEntry("profile_key_name_encrypt_" + userId, new KeyStore.SecretKeyEntry(secretKey), new KeyProtection.Builder(1).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            keyStore.setEntry("profile_key_name_decrypt_" + userId, new KeyStore.SecretKeyEntry(secretKey), new KeyProtection.Builder(2).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(30).build());
            SecretKey keyStoreEncryptionKey = (SecretKey) keyStore.getKey("profile_key_name_encrypt_" + userId, null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, keyStoreEncryptionKey);
            byte[] encryptionResult = cipher.doFinal(randomLockSeed);
            byte[] iv = cipher.getIV();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                if (iv.length != 12) {
                    throw new RuntimeException("Invalid iv length: " + iv.length);
                }
                outputStream.write(iv);
                outputStream.write(encryptionResult);
                this.mStorage.writeChildProfileLock(userId, outputStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Failed to concatenate byte arrays", e);
            }
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
            throw new RuntimeException("Failed to encrypt key", e2);
        }
    }

    private byte[] enrollCredential(byte[] enrolledHandle, String enrolledCredential, String toEnroll, int userId) throws RemoteException {
        byte[] bytes;
        byte[] bytes2;
        checkWritePermission(userId);
        if (enrolledCredential == null) {
            bytes = null;
        } else {
            bytes = enrolledCredential.getBytes();
        }
        if (toEnroll == null) {
            bytes2 = null;
        } else {
            bytes2 = toEnroll.getBytes();
        }
        GateKeeperResponse response = getGateKeeperService().enroll(userId, enrolledHandle, bytes, bytes2);
        if (response == null) {
            return null;
        }
        byte[] hash = response.getPayload();
        if (hash != null) {
            setKeystorePassword(toEnroll, userId);
        } else {
            Slog.e(TAG, "Throttled while enrolling a password");
        }
        return hash;
    }

    private void setUserKeyProtection(int userId, String credential, VerifyCredentialResponse vcr) throws RemoteException {
        if (vcr == null) {
            throw new RemoteException("Null response verifying a credential we just set");
        }
        if (vcr.getResponseCode() != 0) {
            throw new RemoteException("Non-OK response verifying a credential we just set: " + vcr.getResponseCode());
        }
        byte[] token = vcr.getPayload();
        if (token == null) {
            throw new RemoteException("Empty payload verifying a credential we just set");
        }
        addUserKeyAuth(userId, token, secretFromCredential(credential));
    }

    private void clearUserKeyProtection(int userId) throws RemoteException {
        addUserKeyAuth(userId, null, null);
    }

    private static byte[] secretFromCredential(String credential) throws RemoteException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] personalization = "Android FBE credential hash".getBytes(StandardCharsets.UTF_8);
            digest.update(Arrays.copyOf(personalization, 128));
            digest.update(credential.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException for SHA-512");
        }
    }

    private void addUserKeyAuth(int userId, byte[] token, byte[] secret) throws RemoteException {
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(userId);
        IMountService mountService = getMountService();
        long callingId = Binder.clearCallingIdentity();
        try {
            mountService.addUserKeyAuth(userId, userInfo.serialNumber, token, secret);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void fixateNewestUserKeyAuth(int userId) throws RemoteException {
        IMountService mountService = getMountService();
        long callingId = Binder.clearCallingIdentity();
        try {
            mountService.fixateNewestUserKeyAuth(userId);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void resetKeyStore(int userId) throws Throwable {
        checkWritePermission(userId);
        int managedUserId = -1;
        String managedUserDecryptedPassword = null;
        List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
        for (UserInfo pi : profiles) {
            if (pi.isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(pi.id) && this.mStorage.hasChildProfileLock(pi.id)) {
                if (managedUserId == -1) {
                    try {
                        managedUserDecryptedPassword = getDecryptedPasswordForTiedProfile(pi.id);
                        managedUserId = pi.id;
                    } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                        Slog.e(TAG, "Failed to decrypt child profile key", e);
                    }
                } else {
                    Slog.e(TAG, "More than one managed profile, uid1:" + managedUserId + ", uid2:" + pi.id);
                }
            }
        }
        try {
            for (int profileId : this.mUserManager.getProfileIdsWithDisabled(userId)) {
                for (int uid : SYSTEM_CREDENTIAL_UIDS) {
                    this.mKeyStore.clearUid(UserHandle.getUid(profileId, uid));
                }
            }
        } finally {
            if (managedUserId != -1 && managedUserDecryptedPassword != null) {
                tieProfileLockToParent(managedUserId, managedUserDecryptedPassword);
            }
        }
    }

    public VerifyCredentialResponse checkPattern(String pattern, int userId) throws RemoteException {
        return doVerifyPattern(pattern, false, 0L, userId);
    }

    public VerifyCredentialResponse verifyPattern(String pattern, long challenge, int userId) throws RemoteException {
        return doVerifyPattern(pattern, true, challenge, userId);
    }

    private VerifyCredentialResponse doVerifyPattern(String pattern, boolean hasChallenge, long challenge, int userId) throws Throwable {
        checkPasswordReadPermission(userId);
        if (TextUtils.isEmpty(pattern)) {
            throw new IllegalArgumentException("Pattern can't be null or empty");
        }
        LockSettingsStorage.CredentialHash storedHash = this.mStorage.readPatternHash(userId);
        return doVerifyPattern(pattern, storedHash, hasChallenge, challenge, userId);
    }

    private VerifyCredentialResponse doVerifyPattern(String pattern, LockSettingsStorage.CredentialHash storedHash, boolean hasChallenge, long challenge, int userId) throws Throwable {
        String patternToVerify;
        boolean shouldReEnrollBaseZero = storedHash != null ? storedHash.isBaseZeroPattern : false;
        if (shouldReEnrollBaseZero) {
            patternToVerify = LockPatternUtils.patternStringToBaseZero(pattern);
        } else {
            patternToVerify = pattern;
        }
        VerifyCredentialResponse response = verifyCredential(userId, storedHash, patternToVerify, hasChallenge, challenge, new CredentialUtil() {
            @Override
            public void setCredential(String pattern2, String oldPattern, int userId2) throws Throwable {
                LockSettingsService.this.setLockPatternInternal(pattern2, oldPattern, userId2);
            }

            @Override
            public byte[] toHash(String pattern2, int userId2) {
                return LockPatternUtils.patternToHash(LockPatternUtils.stringToPattern(pattern2));
            }

            @Override
            public String adjustForKeystore(String pattern2) {
                return LockPatternUtils.patternStringToBaseZero(pattern2);
            }
        });
        if (response.getResponseCode() == 0 && shouldReEnrollBaseZero) {
            setLockPatternInternal(pattern, patternToVerify, userId);
        }
        return response;
    }

    public VerifyCredentialResponse checkPassword(String password, int userId) throws RemoteException {
        return doVerifyPassword(password, false, 0L, userId);
    }

    public VerifyCredentialResponse verifyPassword(String password, long challenge, int userId) throws RemoteException {
        return doVerifyPassword(password, true, challenge, userId);
    }

    public VerifyCredentialResponse verifyTiedProfileChallenge(String password, boolean isPattern, long challenge, int userId) throws Throwable {
        VerifyCredentialResponse parentResponse;
        checkPasswordReadPermission(userId);
        if (!isManagedProfileWithUnifiedLock(userId)) {
            throw new RemoteException("User id must be managed profile with unified lock");
        }
        int parentProfileId = this.mUserManager.getProfileParent(userId).id;
        if (isPattern) {
            parentResponse = doVerifyPattern(password, true, challenge, parentProfileId);
        } else {
            parentResponse = doVerifyPassword(password, true, challenge, parentProfileId);
        }
        if (parentResponse.getResponseCode() != 0) {
            return parentResponse;
        }
        try {
            return doVerifyPassword(getDecryptedPasswordForTiedProfile(userId), true, challenge, userId);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Slog.e(TAG, "Failed to decrypt child profile key", e);
            throw new RemoteException("Unable to get tied profile token");
        }
    }

    private VerifyCredentialResponse doVerifyPassword(String password, boolean hasChallenge, long challenge, int userId) throws Throwable {
        checkPasswordReadPermission(userId);
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Password can't be null or empty");
        }
        LockSettingsStorage.CredentialHash storedHash = this.mStorage.readPasswordHash(userId);
        return doVerifyPassword(password, storedHash, hasChallenge, challenge, userId);
    }

    private VerifyCredentialResponse doVerifyPassword(String password, LockSettingsStorage.CredentialHash storedHash, boolean hasChallenge, long challenge, int userId) throws RemoteException {
        return verifyCredential(userId, storedHash, password, hasChallenge, challenge, new CredentialUtil() {
            @Override
            public void setCredential(String password2, String oldPassword, int userId2) throws Throwable {
                LockSettingsService.this.setLockPasswordInternal(password2, oldPassword, userId2);
            }

            @Override
            public byte[] toHash(String password2, int userId2) {
                return LockSettingsService.this.mLockPatternUtils.passwordToHash(password2, userId2);
            }

            @Override
            public String adjustForKeystore(String password2) {
                return password2;
            }
        });
    }

    private VerifyCredentialResponse verifyCredential(int userId, LockSettingsStorage.CredentialHash storedHash, String credential, boolean hasChallenge, long challenge, CredentialUtil credentialUtil) throws RemoteException {
        VerifyCredentialResponse response;
        if ((storedHash == null || storedHash.hash.length == 0) && TextUtils.isEmpty(credential)) {
            return VerifyCredentialResponse.OK;
        }
        if (TextUtils.isEmpty(credential)) {
            return VerifyCredentialResponse.ERROR;
        }
        if (storedHash == null || storedHash.hash.length == 0) {
            return VerifyCredentialResponse.OK;
        }
        if (storedHash.version == 0) {
            byte[] hash = credentialUtil.toHash(credential, userId);
            if (Arrays.equals(hash, storedHash.hash)) {
                unlockKeystore(credentialUtil.adjustForKeystore(credential), userId);
                Slog.i(TAG, "Unlocking user with fake token: " + userId);
                byte[] fakeToken = String.valueOf(userId).getBytes();
                unlockUser(userId, fakeToken, fakeToken);
                credentialUtil.setCredential(credential, null, userId);
                if (!hasChallenge) {
                    notifyActivePasswordMetricsAvailable(credential, userId);
                    return VerifyCredentialResponse.OK;
                }
            } else {
                return VerifyCredentialResponse.ERROR;
            }
        }
        boolean shouldReEnroll = false;
        GateKeeperResponse gateKeeperResponse = getGateKeeperService().verifyChallenge(userId, challenge, storedHash.hash, credential.getBytes());
        int responseCode = gateKeeperResponse.getResponseCode();
        if (responseCode == 1) {
            response = new VerifyCredentialResponse(gateKeeperResponse.getTimeout());
        } else if (responseCode == 0) {
            byte[] token = gateKeeperResponse.getPayload();
            if (token == null) {
                Slog.e(TAG, "verifyChallenge response had no associated payload");
                response = VerifyCredentialResponse.ERROR;
            } else {
                shouldReEnroll = gateKeeperResponse.getShouldReEnroll();
                response = new VerifyCredentialResponse(token);
            }
        } else {
            response = VerifyCredentialResponse.ERROR;
        }
        if (response.getResponseCode() == 0) {
            notifyActivePasswordMetricsAvailable(credential, userId);
            unlockKeystore(credential, userId);
            Slog.i(TAG, "Unlocking user " + userId + " with token length " + response.getPayload().length);
            unlockUser(userId, response.getPayload(), secretFromCredential(credential));
            if (isManagedProfileWithSeparatedLock(userId)) {
                TrustManager trustManager = (TrustManager) this.mContext.getSystemService("trust");
                trustManager.setDeviceLockedForUser(userId, false);
            }
            if (shouldReEnroll) {
                credentialUtil.setCredential(credential, credential, userId);
            }
        } else if (response.getResponseCode() == 1 && response.getTimeout() > 0) {
            requireStrongAuth(8, userId);
        }
        return response;
    }

    private void notifyActivePasswordMetricsAvailable(final String password, final int userId) {
        final int quality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(userId);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                int length = 0;
                int letters = 0;
                int uppercase = 0;
                int lowercase = 0;
                int numbers = 0;
                int symbols = 0;
                int nonletter = 0;
                if (password != null) {
                    length = password.length();
                    for (int i = 0; i < length; i++) {
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
                }
                DevicePolicyManager dpm = (DevicePolicyManager) LockSettingsService.this.mContext.getSystemService("device_policy");
                dpm.setActivePasswordState(quality, length, letters, uppercase, lowercase, numbers, symbols, nonletter, userId);
            }
        });
    }

    private void notifyPasswordChanged(final int userId) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LockSettingsService.this.m457com_android_server_LockSettingsService_lambda$1(userId);
            }
        });
    }

    void m457com_android_server_LockSettingsService_lambda$1(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        dpm.reportPasswordChanged(userId);
    }

    public boolean checkVoldPassword(int userId) throws RemoteException {
        if (!this.mFirstCallToVold) {
            return false;
        }
        this.mFirstCallToVold = false;
        checkPasswordReadPermission(userId);
        IMountService service = getMountService();
        long identity = Binder.clearCallingIdentity();
        try {
            String password = service.getPassword();
            service.clearPassword();
            if (password == null) {
                return false;
            }
            try {
                if (this.mLockPatternUtils.isLockPatternEnabled(userId)) {
                    if (checkPattern(password, userId).getResponseCode() == 0) {
                        return true;
                    }
                }
            } catch (Exception e) {
            }
            try {
                if (this.mLockPatternUtils.isLockPasswordEnabled(userId)) {
                    if (checkPassword(password, userId).getResponseCode() == 0) {
                        return true;
                    }
                }
            } catch (Exception e2) {
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void removeUser(int userId, boolean unknownUser) {
        this.mStorage.removeUser(userId);
        this.mStrongAuth.removeUser(userId);
        android.security.KeyStore ks = android.security.KeyStore.getInstance();
        ks.onUserRemoved(userId);
        try {
            IGateKeeperService gk = getGateKeeperService();
            if (gk != null) {
                gk.clearSecureUserId(userId);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "unable to clear GK secure user id");
        }
        if (!unknownUser && !this.mUserManager.getUserInfo(userId).isManagedProfile()) {
            return;
        }
        removeKeystoreProfileKey(userId);
    }

    private void removeKeystoreProfileKey(int targetUserId) {
        try {
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry("profile_key_name_encrypt_" + targetUserId);
            keyStore.deleteEntry("profile_key_name_decrypt_" + targetUserId);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            Slog.e(TAG, "Unable to remove keystore profile key for user:" + targetUserId, e);
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker tracker) {
        checkPasswordReadPermission(-1);
        this.mStrongAuth.registerStrongAuthTracker(tracker);
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker tracker) {
        checkPasswordReadPermission(-1);
        this.mStrongAuth.unregisterStrongAuthTracker(tracker);
    }

    public void requireStrongAuth(int strongAuthReason, int userId) {
        checkWritePermission(userId);
        this.mStrongAuth.requireStrongAuth(strongAuthReason, userId);
    }

    public void userPresent(int userId) {
        checkWritePermission(userId);
        this.mStrongAuth.reportUnlock(userId);
    }

    public int getStrongAuthForUser(int userId) {
        checkPasswordReadPermission(userId);
        return this.mStrongAuthTracker.getStrongAuthForUser(userId);
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }

    private class GateKeeperDiedRecipient implements IBinder.DeathRecipient {
        GateKeeperDiedRecipient(LockSettingsService this$0, GateKeeperDiedRecipient gateKeeperDiedRecipient) {
            this();
        }

        private GateKeeperDiedRecipient() {
        }

        @Override
        public void binderDied() {
            LockSettingsService.this.mGateKeeperService.asBinder().unlinkToDeath(this, 0);
            LockSettingsService.this.mGateKeeperService = null;
        }
    }

    private synchronized IGateKeeperService getGateKeeperService() throws RemoteException {
        if (this.mGateKeeperService != null) {
            return this.mGateKeeperService;
        }
        IBinder service = ServiceManager.getService("android.service.gatekeeper.IGateKeeperService");
        if (service != null) {
            service.linkToDeath(new GateKeeperDiedRecipient(this, null), 0);
            this.mGateKeeperService = IGateKeeperService.Stub.asInterface(service);
            return this.mGateKeeperService;
        }
        Slog.e(TAG, "Unable to acquire GateKeeperService");
        return null;
    }
}
