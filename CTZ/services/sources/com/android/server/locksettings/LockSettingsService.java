package com.android.server.locksettings;

import android.R;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.PasswordMetrics;
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
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.authsecret.V1_0.IAuthSecret;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCallback;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.security.keystore.AndroidKeyStoreProvider;
import android.security.keystore.KeyProtection;
import android.security.keystore.UserNotAuthenticatedException;
import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.RecoveryCertPath;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.service.gatekeeper.GateKeeperResponse;
import android.service.gatekeeper.IGateKeeperService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.ICheckCredentialProgressCallback;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockSettingsInternal;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.locksettings.LockSettingsStorage;
import com.android.server.locksettings.SyntheticPasswordManager;
import com.android.server.locksettings.recoverablekeystore.RecoverableKeyStoreManager;
import com.android.server.pm.DumpState;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    private static final String PERMISSION = "android.permission.ACCESS_KEYGUARD_SECURE_STORAGE";
    private static final int PROFILE_KEY_IV_SIZE = 12;
    private static final int SYNTHETIC_PASSWORD_ENABLED_BY_DEFAULT = 1;
    private static final String TAG = "LockSettingsService";
    private final IActivityManager mActivityManager;
    protected IAuthSecret mAuthSecretService;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final DeviceProvisionedObserver mDeviceProvisionedObserver;
    private boolean mFirstCallToVold;
    protected IGateKeeperService mGateKeeperService;

    @VisibleForTesting
    protected final Handler mHandler;
    private final Injector mInjector;
    private final KeyStore mKeyStore;
    private final LockPatternUtils mLockPatternUtils;
    private final NotificationManager mNotificationManager;
    private final RecoverableKeyStoreManager mRecoverableKeyStoreManager;
    private final Object mSeparateChallengeLock;

    @GuardedBy("mSpManager")
    private SparseArray<SyntheticPasswordManager.AuthenticationToken> mSpCache;
    private final SyntheticPasswordManager mSpManager;

    @VisibleForTesting
    protected final LockSettingsStorage mStorage;
    private final LockSettingsStrongAuth mStrongAuth;
    private final SynchronizedStrongAuthTracker mStrongAuthTracker;
    private final UserManager mUserManager;
    private static final int[] SYSTEM_CREDENTIAL_UIDS = {1010, 1016, 0, 1000};
    private static final String[] VALID_SETTINGS = {"lockscreen.lockedoutpermanently", "lockscreen.patterneverchosen", "lockscreen.password_type", "lockscreen.password_type_alternate", "lockscreen.password_salt", "lockscreen.disabled", "lockscreen.options", "lockscreen.biometric_weak_fallback", "lockscreen.biometricweakeverchosen", "lockscreen.power_button_instantly_locks", "lockscreen.passwordhistory", "lock_pattern_autolock", "lock_biometric_weak_flags", "lock_pattern_visible_pattern", "lock_pattern_tactile_feedback_enabled"};
    private static final String[] READ_CONTACTS_PROTECTED_SETTINGS = {"lock_screen_owner_info_enabled", "lock_screen_owner_info"};
    private static final String SEPARATE_PROFILE_CHALLENGE_KEY = "lockscreen.profilechallenge";
    private static final String[] READ_PASSWORD_PROTECTED_SETTINGS = {"lockscreen.password_salt", "lockscreen.passwordhistory", "lockscreen.password_type", SEPARATE_PROFILE_CHALLENGE_KEY};
    private static final String[] SETTINGS_TO_BACKUP = {"lock_screen_owner_info_enabled", "lock_screen_owner_info", "lock_pattern_visible_pattern", "lockscreen.power_button_instantly_locks"};

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
        public void onBootPhase(int i) {
            super.onBootPhase(i);
            if (i == 550) {
                this.mLockSettingsService.migrateOldDataAfterSystemReady();
            }
        }

        @Override
        public void onStartUser(int i) {
            this.mLockSettingsService.onStartUser(i);
        }

        @Override
        public void onUnlockUser(int i) {
            this.mLockSettingsService.onUnlockUser(i);
        }

        @Override
        public void onCleanupUser(int i) {
            this.mLockSettingsService.onCleanupUser(i);
        }
    }

    @VisibleForTesting
    protected static class SynchronizedStrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        public SynchronizedStrongAuthTracker(Context context) {
            super(context);
        }

        protected void handleStrongAuthRequiredChanged(int i, int i2) {
            synchronized (this) {
                super.handleStrongAuthRequiredChanged(i, i2);
            }
        }

        public int getStrongAuthForUser(int i) {
            int strongAuthForUser;
            synchronized (this) {
                strongAuthForUser = super.getStrongAuthForUser(i);
            }
            return strongAuthForUser;
        }

        void register(LockSettingsStrongAuth lockSettingsStrongAuth) {
            lockSettingsStrongAuth.registerStrongAuthTracker(this.mStub);
        }
    }

    public void tieManagedProfileLockIfNecessary(int i, String str) {
        if (!this.mUserManager.getUserInfo(i).isManagedProfile() || this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i) || this.mStorage.hasChildProfileLock(i)) {
            return;
        }
        int i2 = this.mUserManager.getProfileParent(i).id;
        if (!isUserSecure(i2)) {
            return;
        }
        try {
            if (getGateKeeperService().getSecureUserId(i2) == 0) {
                return;
            }
            byte[] bArr = new byte[0];
            try {
                String strValueOf = String.valueOf(HexEncoding.encode(SecureRandom.getInstance("SHA1PRNG").generateSeed(40)));
                setLockCredentialInternal(strValueOf, 2, str, 327680, i);
                setLong("lockscreen.password_type", 327680L, i);
                tieProfileLockToParent(i, strValueOf);
            } catch (RemoteException | NoSuchAlgorithmException e) {
                Slog.e(TAG, "Fail to tie managed profile", e);
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "Failed to talk to GateKeeper service", e2);
        }
    }

    static class Injector {
        protected Context mContext;

        public Injector(Context context) {
            this.mContext = context;
        }

        public Context getContext() {
            return this.mContext;
        }

        public Handler getHandler() {
            return new Handler();
        }

        public LockSettingsStorage getStorage() {
            final LockSettingsStorage lockSettingsStorage = new LockSettingsStorage(this.mContext);
            lockSettingsStorage.setDatabaseOnCreateCallback(new LockSettingsStorage.Callback() {
                @Override
                public void initialize(SQLiteDatabase sQLiteDatabase) {
                    if (SystemProperties.getBoolean("ro.lockscreen.disable.default", false)) {
                        lockSettingsStorage.writeKeyValue(sQLiteDatabase, "lockscreen.disabled", "1", 0);
                    }
                }
            });
            return lockSettingsStorage;
        }

        public LockSettingsStrongAuth getStrongAuth() {
            return new LockSettingsStrongAuth(this.mContext);
        }

        public SynchronizedStrongAuthTracker getStrongAuthTracker() {
            return new SynchronizedStrongAuthTracker(this.mContext);
        }

        public IActivityManager getActivityManager() {
            return ActivityManager.getService();
        }

        public LockPatternUtils getLockPatternUtils() {
            return new LockPatternUtils(this.mContext);
        }

        public NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService("notification");
        }

        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService("user");
        }

        public DevicePolicyManager getDevicePolicyManager() {
            return (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        }

        public KeyStore getKeyStore() {
            return KeyStore.getInstance();
        }

        public RecoverableKeyStoreManager getRecoverableKeyStoreManager(KeyStore keyStore) {
            return RecoverableKeyStoreManager.getInstance(this.mContext, keyStore);
        }

        public IStorageManager getStorageManager() {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                return IStorageManager.Stub.asInterface(service);
            }
            return null;
        }

        public SyntheticPasswordManager getSyntheticPasswordManager(LockSettingsStorage lockSettingsStorage) {
            return new SyntheticPasswordManager(getContext(), lockSettingsStorage, getUserManager());
        }

        public int binderGetCallingUid() {
            return Binder.getCallingUid();
        }
    }

    public LockSettingsService(Context context) {
        this(new Injector(context));
    }

    @VisibleForTesting
    protected LockSettingsService(Injector injector) {
        this.mSeparateChallengeLock = new Object();
        this.mDeviceProvisionedObserver = new DeviceProvisionedObserver();
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) throws Throwable {
                int intExtra;
                if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                    int intExtra2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    if (intExtra2 > 0) {
                        LockSettingsService.this.removeUser(intExtra2, true);
                    }
                    KeyStore keyStore = KeyStore.getInstance();
                    UserInfo profileParent = LockSettingsService.this.mUserManager.getProfileParent(intExtra2);
                    keyStore.onUserAdded(intExtra2, profileParent != null ? profileParent.id : -1);
                    return;
                }
                if ("android.intent.action.USER_STARTING".equals(intent.getAction())) {
                    LockSettingsService.this.mStorage.prefetchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(intent.getAction()) && (intExtra = intent.getIntExtra("android.intent.extra.user_handle", 0)) > 0) {
                    LockSettingsService.this.removeUser(intExtra, false);
                }
            }
        };
        this.mSpCache = new SparseArray<>();
        this.mInjector = injector;
        this.mContext = injector.getContext();
        this.mKeyStore = injector.getKeyStore();
        this.mRecoverableKeyStoreManager = injector.getRecoverableKeyStoreManager(this.mKeyStore);
        this.mHandler = injector.getHandler();
        this.mStrongAuth = injector.getStrongAuth();
        this.mActivityManager = injector.getActivityManager();
        this.mLockPatternUtils = injector.getLockPatternUtils();
        this.mFirstCallToVold = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_STARTING");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        injector.getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mStorage = injector.getStorage();
        this.mNotificationManager = injector.getNotificationManager();
        this.mUserManager = injector.getUserManager();
        this.mStrongAuthTracker = injector.getStrongAuthTracker();
        this.mStrongAuthTracker.register(this.mStrongAuth);
        this.mSpManager = injector.getSyntheticPasswordManager(this.mStorage);
        LocalServices.addService(LockSettingsInternal.class, new LocalService());
    }

    private void maybeShowEncryptionNotificationForUser(int i) {
        UserInfo profileParent;
        UserInfo userInfo = this.mUserManager.getUserInfo(i);
        if (!userInfo.isManagedProfile()) {
            return;
        }
        UserHandle userHandle = userInfo.getUserHandle();
        if (isUserSecure(i) && !this.mUserManager.isUserUnlockingOrUnlocked(userHandle) && (profileParent = this.mUserManager.getProfileParent(i)) != null && this.mUserManager.isUserUnlockingOrUnlocked(profileParent.getUserHandle()) && !this.mUserManager.isQuietModeEnabled(userHandle)) {
            showEncryptionNotificationForProfile(userHandle);
        }
    }

    private void showEncryptionNotificationForProfile(UserHandle userHandle) {
        Resources resources = this.mContext.getResources();
        CharSequence text = resources.getText(R.string.mmcc_authentication_reject_msim_template);
        CharSequence text2 = resources.getText(R.string.keyguard_accessibility_widget_reorder_start);
        CharSequence text3 = resources.getText(R.string.keyguard_accessibility_widget_reorder_end);
        Intent intentCreateConfirmDeviceCredentialIntent = ((KeyguardManager) this.mContext.getSystemService("keyguard")).createConfirmDeviceCredentialIntent(null, null, userHandle.getIdentifier());
        if (intentCreateConfirmDeviceCredentialIntent == null) {
            return;
        }
        intentCreateConfirmDeviceCredentialIntent.setFlags(276824064);
        showEncryptionNotification(userHandle, text, text2, text3, PendingIntent.getActivity(this.mContext, 0, intentCreateConfirmDeviceCredentialIntent, 134217728));
    }

    private void showEncryptionNotification(UserHandle userHandle, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, PendingIntent pendingIntent) {
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            this.mNotificationManager.notifyAsUser(null, 9, new Notification.Builder(this.mContext, SystemNotificationChannels.SECURITY).setSmallIcon(R.drawable.ic_media_route_connecting_dark_13_mtrl).setWhen(0L).setOngoing(true).setTicker(charSequence).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setContentTitle(charSequence).setContentText(charSequence2).setSubText(charSequence3).setVisibility(1).setContentIntent(pendingIntent).build(), userHandle);
        }
    }

    private void hideEncryptionNotification(UserHandle userHandle) {
        this.mNotificationManager.cancelAsUser(null, 9, userHandle);
    }

    public void onCleanupUser(int i) {
        hideEncryptionNotification(new UserHandle(i));
        requireStrongAuth(1, i);
    }

    public void onStartUser(int i) {
        maybeShowEncryptionNotificationForUser(i);
    }

    private void ensureProfileKeystoreUnlocked(int i) {
        if (KeyStore.getInstance().state(i) == KeyStore.State.LOCKED && tiedManagedProfileReadyToUnlock(this.mUserManager.getUserInfo(i))) {
            Slog.i(TAG, "Managed profile got unlocked, will unlock its keystore");
            try {
                unlockChildProfile(i, true);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to unlock child profile");
            }
        }
    }

    public void onUnlockUser(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LockSettingsService.this.ensureProfileKeystoreUnlocked(i);
                LockSettingsService.this.hideEncryptionNotification(new UserHandle(i));
                List profiles = LockSettingsService.this.mUserManager.getProfiles(i);
                for (int i2 = 0; i2 < profiles.size(); i2++) {
                    UserInfo userInfo = (UserInfo) profiles.get(i2);
                    if (LockSettingsService.this.isUserSecure(userInfo.id) && userInfo.isManagedProfile()) {
                        UserHandle userHandle = userInfo.getUserHandle();
                        if (!LockSettingsService.this.mUserManager.isUserUnlockingOrUnlocked(userHandle) && !LockSettingsService.this.mUserManager.isQuietModeEnabled(userHandle)) {
                            LockSettingsService.this.showEncryptionNotificationForProfile(userHandle);
                        }
                    }
                }
                if (LockSettingsService.this.mUserManager.getUserInfo(i).isManagedProfile()) {
                    LockSettingsService.this.tieManagedProfileLockIfNecessary(i, null);
                }
                if (LockSettingsService.this.mUserManager.getUserInfo(i).isPrimary() && !LockSettingsService.this.isUserSecure(i)) {
                    LockSettingsService.this.tryDeriveAuthTokenForUnsecuredPrimaryUser(i);
                }
            }
        });
    }

    private void tryDeriveAuthTokenForUnsecuredPrimaryUser(int i) {
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(i)) {
                try {
                    SyntheticPasswordManager.AuthenticationResult authenticationResultUnwrapPasswordBasedSyntheticPassword = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(i), null, i, null);
                    if (authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken != null) {
                        Slog.i(TAG, "Retrieved auth token for user " + i);
                        onAuthTokenKnownForUser(i, authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken);
                    } else {
                        Slog.e(TAG, "Auth token not available for user " + i);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failure retrieving auth token", e);
                }
            }
        }
    }

    public void systemReady() throws Throwable {
        if (this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
            EventLog.writeEvent(1397638484, "28251513", Integer.valueOf(getCallingUid()), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        checkWritePermission(0);
        migrateOldData();
        try {
            getGateKeeperService();
            this.mSpManager.initWeaverService();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failure retrieving IGateKeeperService", e);
        }
        try {
            this.mAuthSecretService = IAuthSecret.getService();
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to get AuthSecret HAL", e2);
        } catch (NoSuchElementException e3) {
            Slog.i(TAG, "Device doesn't implement AuthSecret HAL");
        }
        this.mDeviceProvisionedObserver.onSystemReady();
        this.mStorage.prefetchUser(0);
        this.mStrongAuth.systemReady();
    }

    private void migrateOldData() {
        if (getString("migrated", null, 0) == null) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            for (String str : VALID_SETTINGS) {
                String string = Settings.Secure.getString(contentResolver, str);
                if (string != null) {
                    setString(str, string, 0);
                }
            }
            setString("migrated", "true", 0);
            Slog.i(TAG, "Migrated lock settings to new location");
        }
        if (getString("migrated_user_specific", null, 0) == null) {
            ContentResolver contentResolver2 = this.mContext.getContentResolver();
            List users = this.mUserManager.getUsers();
            for (int i = 0; i < users.size(); i++) {
                int i2 = ((UserInfo) users.get(i)).id;
                String stringForUser = Settings.Secure.getStringForUser(contentResolver2, "lock_screen_owner_info", i2);
                if (!TextUtils.isEmpty(stringForUser)) {
                    setString("lock_screen_owner_info", stringForUser, i2);
                    Settings.Secure.putStringForUser(contentResolver2, "lock_screen_owner_info", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i2);
                }
                try {
                    setLong("lock_screen_owner_info_enabled", Settings.Secure.getIntForUser(contentResolver2, "lock_screen_owner_info_enabled", i2) != 0 ? 1L : 0L, i2);
                } catch (Settings.SettingNotFoundException e) {
                    if (!TextUtils.isEmpty(stringForUser)) {
                        setLong("lock_screen_owner_info_enabled", 1L, i2);
                    }
                }
                Settings.Secure.putIntForUser(contentResolver2, "lock_screen_owner_info_enabled", 0, i2);
            }
            setString("migrated_user_specific", "true", 0);
            Slog.i(TAG, "Migrated per-user lock settings to new location");
        }
        if (getString("migrated_biometric_weak", null, 0) == null) {
            List users2 = this.mUserManager.getUsers();
            for (int i3 = 0; i3 < users2.size(); i3++) {
                int i4 = ((UserInfo) users2.get(i3)).id;
                long j = getLong("lockscreen.password_type", 0L, i4);
                long j2 = getLong("lockscreen.password_type_alternate", 0L, i4);
                if (j == 32768) {
                    setLong("lockscreen.password_type", j2, i4);
                }
                setLong("lockscreen.password_type_alternate", 0L, i4);
            }
            setString("migrated_biometric_weak", "true", 0);
            Slog.i(TAG, "Migrated biometric weak to use the fallback instead");
        }
        if (getString("migrated_lockscreen_disabled", null, 0) == null) {
            List users3 = this.mUserManager.getUsers();
            int size = users3.size();
            int i5 = 0;
            for (int i6 = 0; i6 < size; i6++) {
                if (((UserInfo) users3.get(i6)).supportsSwitchTo()) {
                    i5++;
                }
            }
            if (i5 > 1) {
                for (int i7 = 0; i7 < size; i7++) {
                    int i8 = ((UserInfo) users3.get(i7)).id;
                    if (getBoolean("lockscreen.disabled", false, i8)) {
                        setBoolean("lockscreen.disabled", false, i8);
                    }
                }
            }
            setString("migrated_lockscreen_disabled", "true", 0);
            Slog.i(TAG, "Migrated lockscreen disabled flag");
        }
        List users4 = this.mUserManager.getUsers();
        for (int i9 = 0; i9 < users4.size(); i9++) {
            UserInfo userInfo = (UserInfo) users4.get(i9);
            if (userInfo.isManagedProfile() && this.mStorage.hasChildProfileLock(userInfo.id)) {
                long j3 = getLong("lockscreen.password_type", 0L, userInfo.id);
                if (j3 == 0) {
                    Slog.i(TAG, "Migrated tied profile lock type");
                    setLong("lockscreen.password_type", 327680L, userInfo.id);
                } else if (j3 != 327680) {
                    Slog.e(TAG, "Invalid tied profile lock type: " + j3);
                }
            }
            try {
                String str2 = "profile_key_name_encrypt_" + userInfo.id;
                java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                if (keyStore.containsAlias(str2)) {
                    keyStore.deleteEntry(str2);
                }
            } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e2) {
                Slog.e(TAG, "Unable to remove tied profile key", e2);
            }
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch") && getString("migrated_wear_lockscreen_disabled", null, 0) == null) {
            int size2 = users4.size();
            for (int i10 = 0; i10 < size2; i10++) {
                setBoolean("lockscreen.disabled", false, ((UserInfo) users4.get(i10)).id);
            }
            setString("migrated_wear_lockscreen_disabled", "true", 0);
            Slog.i(TAG, "Migrated lockscreen_disabled for Wear devices");
        }
    }

    private void migrateOldDataAfterSystemReady() {
        try {
            if (LockPatternUtils.frpCredentialEnabled(this.mContext) && !getBoolean("migrated_frp", false, 0)) {
                migrateFrpCredential();
                setBoolean("migrated_frp", true, 0);
                Slog.i(TAG, "Migrated migrated_frp.");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to migrateOldDataAfterSystemReady", e);
        }
    }

    private void migrateFrpCredential() throws RemoteException {
        if (this.mStorage.readPersistentDataBlock() != LockSettingsStorage.PersistentData.NONE) {
            return;
        }
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            if (LockPatternUtils.userOwnsFrpCredential(this.mContext, userInfo) && isUserSecure(userInfo.id)) {
                synchronized (this.mSpManager) {
                    if (isSyntheticPasswordBasedCredentialLocked(userInfo.id)) {
                        this.mSpManager.migrateFrpPasswordLocked(getSyntheticPasswordHandleLocked(userInfo.id), userInfo, redactActualQualityToMostLenientEquivalentQuality((int) getLong("lockscreen.password_type", 0L, userInfo.id)));
                    }
                }
                return;
            }
        }
    }

    private int redactActualQualityToMostLenientEquivalentQuality(int i) {
        if (i == 131072 || i == 196608) {
            return DumpState.DUMP_INTENT_FILTER_VERIFIERS;
        }
        return (i == 262144 || i == 327680 || i == 393216) ? DumpState.DUMP_DOMAIN_PREFERRED : i;
    }

    private final void checkWritePermission(int i) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsWrite");
    }

    private final void checkPasswordReadPermission(int i) {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsRead");
    }

    private final void checkPasswordHavePermission(int i) {
        if (this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
            EventLog.writeEvent(1397638484, "28251513", Integer.valueOf(getCallingUid()), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        this.mContext.enforceCallingOrSelfPermission(PERMISSION, "LockSettingsHave");
    }

    private final void checkReadPermission(String str, int i) {
        int callingUid = Binder.getCallingUid();
        for (int i2 = 0; i2 < READ_CONTACTS_PROTECTED_SETTINGS.length; i2++) {
            if (READ_CONTACTS_PROTECTED_SETTINGS[i2].equals(str) && this.mContext.checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
                throw new SecurityException("uid=" + callingUid + " needs permission android.permission.READ_CONTACTS to read " + str + " for user " + i);
            }
        }
        for (int i3 = 0; i3 < READ_PASSWORD_PROTECTED_SETTINGS.length; i3++) {
            if (READ_PASSWORD_PROTECTED_SETTINGS[i3].equals(str) && this.mContext.checkCallingOrSelfPermission(PERMISSION) != 0) {
                throw new SecurityException("uid=" + callingUid + " needs permission " + PERMISSION + " to read " + str + " for user " + i);
            }
        }
    }

    public boolean getSeparateProfileChallengeEnabled(int i) {
        boolean z;
        checkReadPermission(SEPARATE_PROFILE_CHALLENGE_KEY, i);
        synchronized (this.mSeparateChallengeLock) {
            z = getBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, false, i);
        }
        return z;
    }

    public void setSeparateProfileChallengeEnabled(int i, boolean z, String str) {
        checkWritePermission(i);
        synchronized (this.mSeparateChallengeLock) {
            setSeparateProfileChallengeEnabledLocked(i, z, str);
        }
        notifySeparateProfileChallengeChanged(i);
    }

    @GuardedBy("mSeparateChallengeLock")
    private void setSeparateProfileChallengeEnabledLocked(int i, boolean z, String str) {
        setBoolean(SEPARATE_PROFILE_CHALLENGE_KEY, z, i);
        if (z) {
            this.mStorage.removeChildProfileLock(i);
            removeKeystoreProfileKey(i);
        } else {
            tieManagedProfileLockIfNecessary(i, str);
        }
    }

    private void notifySeparateProfileChallengeChanged(int i) {
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (devicePolicyManagerInternal != null) {
            devicePolicyManagerInternal.reportSeparateProfileChallengeChanged(i);
        }
    }

    public void setBoolean(String str, boolean z, int i) {
        checkWritePermission(i);
        setStringUnchecked(str, i, z ? "1" : "0");
    }

    public void setLong(String str, long j, int i) {
        checkWritePermission(i);
        setStringUnchecked(str, i, Long.toString(j));
    }

    public void setString(String str, String str2, int i) {
        checkWritePermission(i);
        setStringUnchecked(str, i, str2);
    }

    private void setStringUnchecked(String str, int i, String str2) {
        Preconditions.checkArgument(i != -9999, "cannot store lock settings for FRP user");
        this.mStorage.writeKeyValue(str, str2, i);
        if (ArrayUtils.contains(SETTINGS_TO_BACKUP, str)) {
            BackupManager.dataChanged(BackupManagerService.SETTINGS_PACKAGE);
        }
    }

    public boolean getBoolean(String str, boolean z, int i) {
        checkReadPermission(str, i);
        String stringUnchecked = getStringUnchecked(str, null, i);
        if (TextUtils.isEmpty(stringUnchecked)) {
            return z;
        }
        return stringUnchecked.equals("1") || stringUnchecked.equals("true");
    }

    public long getLong(String str, long j, int i) {
        checkReadPermission(str, i);
        String stringUnchecked = getStringUnchecked(str, null, i);
        return TextUtils.isEmpty(stringUnchecked) ? j : Long.parseLong(stringUnchecked);
    }

    public String getString(String str, String str2, int i) {
        checkReadPermission(str, i);
        return getStringUnchecked(str, str2, i);
    }

    public String getStringUnchecked(String str, String str2, int i) {
        if ("lock_pattern_autolock".equals(str)) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return this.mLockPatternUtils.isLockPatternEnabled(i) ? "1" : "0";
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        if (i == -9999) {
            return getFrpStringUnchecked(str);
        }
        if ("legacy_lock_pattern_enabled".equals(str)) {
            str = "lock_pattern_autolock";
        }
        return this.mStorage.readKeyValue(str, str2, i);
    }

    private String getFrpStringUnchecked(String str) {
        if ("lockscreen.password_type".equals(str)) {
            return String.valueOf(readFrpPasswordQuality());
        }
        return null;
    }

    private int readFrpPasswordQuality() {
        return this.mStorage.readPersistentDataBlock().qualityForUi;
    }

    public boolean havePassword(int i) throws RemoteException {
        checkPasswordHavePermission(i);
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(i)) {
                return this.mSpManager.getCredentialType(getSyntheticPasswordHandleLocked(i), i) == 2;
            }
            return this.mStorage.hasPassword(i);
        }
    }

    public boolean havePattern(int i) throws RemoteException {
        checkPasswordHavePermission(i);
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(i)) {
                boolean z = true;
                if (this.mSpManager.getCredentialType(getSyntheticPasswordHandleLocked(i), i) != 1) {
                    z = false;
                }
                return z;
            }
            return this.mStorage.hasPattern(i);
        }
    }

    private boolean isUserSecure(int i) {
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(i)) {
                return this.mSpManager.getCredentialType(getSyntheticPasswordHandleLocked(i), i) != -1;
            }
            return this.mStorage.hasCredential(i);
        }
    }

    private void setKeystorePassword(String str, int i) {
        KeyStore.getInstance().onUserPasswordChanged(i, str);
    }

    private void unlockKeystore(String str, int i) {
        KeyStore.getInstance().unlock(i, str);
    }

    @VisibleForTesting
    protected String getDecryptedPasswordForTiedProfile(int i) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, InvalidKeyException, KeyStoreException, CertificateException, InvalidAlgorithmParameterException {
        byte[] childProfileLock = this.mStorage.readChildProfileLock(i);
        if (childProfileLock == null) {
            throw new FileNotFoundException("Child profile lock file not found");
        }
        byte[] bArrCopyOfRange = Arrays.copyOfRange(childProfileLock, 0, 12);
        byte[] bArrCopyOfRange2 = Arrays.copyOfRange(childProfileLock, 12, childProfileLock.length);
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        SecretKey secretKey = (SecretKey) keyStore.getKey("profile_key_name_decrypt_" + i, null);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, secretKey, new GCMParameterSpec(128, bArrCopyOfRange));
        return new String(cipher.doFinal(bArrCopyOfRange2), StandardCharsets.UTF_8);
    }

    private void unlockChildProfile(int i, boolean z) throws RemoteException {
        try {
            doVerifyCredential(getDecryptedPasswordForTiedProfile(i), 2, false, 0L, i, null);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            if (e instanceof FileNotFoundException) {
                Slog.i(TAG, "Child profile key not found");
            } else if (z && (e instanceof UserNotAuthenticatedException)) {
                Slog.i(TAG, "Parent keystore seems locked, ignoring");
            } else {
                Slog.e(TAG, "Failed to decrypt child profile key", e);
            }
        }
    }

    private void unlockUser(int i, byte[] bArr, byte[] bArr2) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            this.mActivityManager.unlockUser(i, bArr, bArr2, new IProgressListener.Stub() {
                public void onStarted(int i2, Bundle bundle) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser started");
                }

                public void onProgress(int i2, int i3, Bundle bundle) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser progress " + i3);
                }

                public void onFinished(int i2, Bundle bundle) throws RemoteException {
                    Log.d(LockSettingsService.TAG, "unlockUser finished");
                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await(15L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                if (!this.mUserManager.getUserInfo(i).isManagedProfile()) {
                    for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
                        if (tiedManagedProfileReadyToUnlock(userInfo)) {
                            unlockChildProfile(userInfo.id, false);
                        }
                    }
                }
            } catch (RemoteException e2) {
                Log.d(TAG, "Failed to unlock child profile", e2);
            }
        } catch (RemoteException e3) {
            throw e3.rethrowAsRuntimeException();
        }
    }

    private boolean tiedManagedProfileReadyToUnlock(UserInfo userInfo) {
        return userInfo.isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userInfo.id) && this.mStorage.hasChildProfileLock(userInfo.id) && this.mUserManager.isUserRunning(userInfo.id);
    }

    private Map<Integer, String> getDecryptedPasswordsForAllTiedProfiles(int i) {
        if (this.mUserManager.getUserInfo(i).isManagedProfile()) {
            return null;
        }
        ArrayMap arrayMap = new ArrayMap();
        List profiles = this.mUserManager.getProfiles(i);
        int size = profiles.size();
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = (UserInfo) profiles.get(i2);
            if (userInfo.isManagedProfile()) {
                int i3 = userInfo.id;
                if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i3)) {
                    try {
                        arrayMap.put(Integer.valueOf(i3), getDecryptedPasswordForTiedProfile(i3));
                    } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                        Slog.e(TAG, "getDecryptedPasswordsForAllTiedProfiles failed for user " + i3, e);
                    }
                }
            }
        }
        return arrayMap;
    }

    private void synchronizeUnifiedWorkChallengeForProfiles(int i, Map<Integer, String> map) throws RemoteException {
        if (this.mUserManager.getUserInfo(i).isManagedProfile()) {
            return;
        }
        boolean zIsUserSecure = isUserSecure(i);
        List profiles = this.mUserManager.getProfiles(i);
        int size = profiles.size();
        for (int i2 = 0; i2 < size; i2++) {
            UserInfo userInfo = (UserInfo) profiles.get(i2);
            if (userInfo.isManagedProfile()) {
                int i3 = userInfo.id;
                if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i3)) {
                    if (zIsUserSecure) {
                        tieManagedProfileLockIfNecessary(i3, null);
                    } else {
                        if (map != null && map.containsKey(Integer.valueOf(i3))) {
                            setLockCredentialInternal(null, -1, map.get(Integer.valueOf(i3)), 0, i3);
                        } else {
                            Slog.wtf(TAG, "clear tied profile challenges, but no password supplied.");
                            setLockCredentialInternal(null, -1, null, 0, i3);
                        }
                        this.mStorage.removeChildProfileLock(i3);
                        removeKeystoreProfileKey(i3);
                    }
                }
            }
        }
    }

    private boolean isManagedProfileWithUnifiedLock(int i) {
        return this.mUserManager.getUserInfo(i).isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i);
    }

    private boolean isManagedProfileWithSeparatedLock(int i) {
        return this.mUserManager.getUserInfo(i).isManagedProfile() && this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i);
    }

    public void setLockCredential(String str, int i, String str2, int i2, int i3) throws RemoteException {
        checkWritePermission(i3);
        synchronized (this.mSeparateChallengeLock) {
            setLockCredentialInternal(str, i, str2, i2, i3);
            setSeparateProfileChallengeEnabledLocked(i3, true, null);
            notifyPasswordChanged(i3);
        }
        notifySeparateProfileChallengeChanged(i3);
    }

    private void setLockCredentialInternal(String str, int i, String str2, int i2, int i3) throws Throwable {
        String decryptedPasswordForTiedProfile;
        String str3 = TextUtils.isEmpty(str2) ? null : str2;
        String str4 = TextUtils.isEmpty(str) ? null : str;
        synchronized (this.mSpManager) {
            if (isSyntheticPasswordBasedCredentialLocked(i3)) {
                spBasedSetLockCredentialInternalLocked(str4, i, str3, i2, i3);
                return;
            }
            if (i == -1) {
                if (str4 != null) {
                    Slog.wtf(TAG, "CredentialType is none, but credential is non-null.");
                }
                clearUserKeyProtection(i3);
                getGateKeeperService().clearSecureUserId(i3);
                this.mStorage.writeCredentialHash(LockSettingsStorage.CredentialHash.createEmptyHash(), i3);
                setKeystorePassword(null, i3);
                fixateNewestUserKeyAuth(i3);
                synchronizeUnifiedWorkChallengeForProfiles(i3, null);
                notifyActivePasswordMetricsAvailable(null, i3);
                this.mRecoverableKeyStoreManager.lockScreenSecretChanged(i, str4, i3);
                return;
            }
            if (str4 != null) {
                LockSettingsStorage.CredentialHash credentialHash = this.mStorage.readCredentialHash(i3);
                if (isManagedProfileWithUnifiedLock(i3)) {
                    if (str3 == null) {
                        try {
                            decryptedPasswordForTiedProfile = getDecryptedPasswordForTiedProfile(i3);
                        } catch (FileNotFoundException e) {
                            Slog.i(TAG, "Child profile key not found");
                            decryptedPasswordForTiedProfile = str3;
                        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                            Slog.e(TAG, "Failed to decrypt child profile key", e2);
                            decryptedPasswordForTiedProfile = str3;
                        }
                    } else {
                        decryptedPasswordForTiedProfile = str3;
                    }
                } else if (credentialHash.hash == null) {
                    if (str3 != null) {
                        Slog.w(TAG, "Saved credential provided, but none stored");
                    }
                    decryptedPasswordForTiedProfile = null;
                }
                synchronized (this.mSpManager) {
                    if (shouldMigrateToSyntheticPasswordLocked(i3)) {
                        initializeSyntheticPasswordLocked(credentialHash.hash, decryptedPasswordForTiedProfile, credentialHash.type, i2, i3);
                        spBasedSetLockCredentialInternalLocked(str4, i, decryptedPasswordForTiedProfile, i2, i3);
                        return;
                    }
                    byte[] bArrEnrollCredential = enrollCredential(credentialHash.hash, decryptedPasswordForTiedProfile, str4, i3);
                    if (bArrEnrollCredential != null) {
                        LockSettingsStorage.CredentialHash credentialHashCreate = LockSettingsStorage.CredentialHash.create(bArrEnrollCredential, i);
                        this.mStorage.writeCredentialHash(credentialHashCreate, i3);
                        setUserKeyProtection(i3, str4, convertResponse(getGateKeeperService().verifyChallenge(i3, 0L, credentialHashCreate.hash, str4.getBytes())));
                        fixateNewestUserKeyAuth(i3);
                        doVerifyCredential(str4, i, true, 0L, i3, null);
                        synchronizeUnifiedWorkChallengeForProfiles(i3, null);
                        this.mRecoverableKeyStoreManager.lockScreenSecretChanged(i, str4, i3);
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Failed to enroll ");
                    sb.append(i == 2 ? "password" : "pattern");
                    throw new RemoteException(sb.toString());
                }
            }
            throw new RemoteException("Null credential with mismatched credential type");
        }
    }

    private VerifyCredentialResponse convertResponse(GateKeeperResponse gateKeeperResponse) {
        return VerifyCredentialResponse.fromGateKeeperResponse(gateKeeperResponse);
    }

    @VisibleForTesting
    protected void tieProfileLockToParent(int i, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(new SecureRandom());
            SecretKey secretKeyGenerateKey = keyGenerator.generateKey();
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            try {
                keyStore.setEntry("profile_key_name_encrypt_" + i, new KeyStore.SecretKeyEntry(secretKeyGenerateKey), new KeyProtection.Builder(1).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
                keyStore.setEntry("profile_key_name_decrypt_" + i, new KeyStore.SecretKeyEntry(secretKeyGenerateKey), new KeyProtection.Builder(2).setBlockModes("GCM").setEncryptionPaddings("NoPadding").setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(30).setCriticalToDeviceEncryption(true).build());
                SecretKey secretKey = (SecretKey) keyStore.getKey("profile_key_name_encrypt_" + i, null);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(1, secretKey);
                byte[] bArrDoFinal = cipher.doFinal(bytes);
                byte[] iv = cipher.getIV();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    if (iv.length != 12) {
                        throw new RuntimeException("Invalid iv length: " + iv.length);
                    }
                    byteArrayOutputStream.write(iv);
                    byteArrayOutputStream.write(bArrDoFinal);
                    this.mStorage.writeChildProfileLock(i, byteArrayOutputStream.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to concatenate byte arrays", e);
                }
            } finally {
                keyStore.deleteEntry("profile_key_name_encrypt_" + i);
            }
        } catch (IOException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
            throw new RuntimeException("Failed to encrypt key", e2);
        }
    }

    private byte[] enrollCredential(byte[] bArr, String str, String str2, int i) throws RemoteException {
        byte[] bytes;
        byte[] bytes2;
        checkWritePermission(i);
        if (str != null) {
            bytes = str.getBytes();
        } else {
            bytes = null;
        }
        if (str2 != null) {
            bytes2 = str2.getBytes();
        } else {
            bytes2 = null;
        }
        GateKeeperResponse gateKeeperResponseEnroll = getGateKeeperService().enroll(i, bArr, bytes, bytes2);
        if (gateKeeperResponseEnroll == null) {
            return null;
        }
        byte[] payload = gateKeeperResponseEnroll.getPayload();
        if (payload != null) {
            setKeystorePassword(str2, i);
        } else {
            Slog.e(TAG, "Throttled while enrolling a password");
        }
        return payload;
    }

    private void setAuthlessUserKeyProtection(int i, byte[] bArr) throws RemoteException {
        addUserKeyAuth(i, null, bArr);
    }

    private void setUserKeyProtection(int i, String str, VerifyCredentialResponse verifyCredentialResponse) throws RemoteException {
        if (verifyCredentialResponse == null) {
            throw new RemoteException("Null response verifying a credential we just set");
        }
        if (verifyCredentialResponse.getResponseCode() != 0) {
            throw new RemoteException("Non-OK response verifying a credential we just set: " + verifyCredentialResponse.getResponseCode());
        }
        byte[] payload = verifyCredentialResponse.getPayload();
        if (payload == null) {
            throw new RemoteException("Empty payload verifying a credential we just set");
        }
        addUserKeyAuth(i, payload, secretFromCredential(str));
    }

    private void clearUserKeyProtection(int i) throws RemoteException {
        addUserKeyAuth(i, null, null);
    }

    private static byte[] secretFromCredential(String str) throws RemoteException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.update(Arrays.copyOf("Android FBE credential hash".getBytes(StandardCharsets.UTF_8), 128));
            messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException for SHA-512");
        }
    }

    private void addUserKeyAuth(int i, byte[] bArr, byte[] bArr2) throws RemoteException {
        UserInfo userInfo = this.mUserManager.getUserInfo(i);
        IStorageManager storageManager = this.mInjector.getStorageManager();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            storageManager.addUserKeyAuth(i, userInfo.serialNumber, bArr, bArr2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void fixateNewestUserKeyAuth(int i) throws RemoteException {
        IStorageManager storageManager = this.mInjector.getStorageManager();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            storageManager.fixateNewestUserKeyAuth(i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void resetKeyStore(int i) throws RemoteException {
        String decryptedPasswordForTiedProfile;
        checkWritePermission(i);
        String str = null;
        int i2 = -1;
        for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
            if (userInfo.isManagedProfile() && !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userInfo.id) && this.mStorage.hasChildProfileLock(userInfo.id)) {
                if (i2 == -1) {
                    try {
                        decryptedPasswordForTiedProfile = getDecryptedPasswordForTiedProfile(userInfo.id);
                    } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                        e = e;
                    }
                    try {
                        i2 = userInfo.id;
                        str = decryptedPasswordForTiedProfile;
                    } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                        e = e2;
                        str = decryptedPasswordForTiedProfile;
                        Slog.e(TAG, "Failed to decrypt child profile key", e);
                    }
                } else {
                    Slog.e(TAG, "More than one managed profile, uid1:" + i2 + ", uid2:" + userInfo.id);
                }
            }
        }
        try {
            for (int i3 : this.mUserManager.getProfileIdsWithDisabled(i)) {
                for (int i4 : SYSTEM_CREDENTIAL_UIDS) {
                    this.mKeyStore.clearUid(UserHandle.getUid(i3, i4));
                }
            }
        } finally {
            if (i2 != -1 && str != null) {
                tieProfileLockToParent(i2, str);
            }
        }
    }

    public VerifyCredentialResponse checkCredential(String str, int i, int i2, ICheckCredentialProgressCallback iCheckCredentialProgressCallback) throws RemoteException {
        checkPasswordReadPermission(i2);
        return doVerifyCredential(str, i, false, 0L, i2, iCheckCredentialProgressCallback);
    }

    public VerifyCredentialResponse verifyCredential(String str, int i, long j, int i2) throws RemoteException {
        checkPasswordReadPermission(i2);
        return doVerifyCredential(str, i, true, j, i2, null);
    }

    private VerifyCredentialResponse doVerifyCredential(String str, int i, boolean z, long j, int i2, ICheckCredentialProgressCallback iCheckCredentialProgressCallback) throws Throwable {
        String strPatternStringToBaseZero;
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Credential can't be null or empty");
        }
        if (i2 == -9999 && Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            Slog.e(TAG, "FRP credential can only be verified prior to provisioning.");
            return VerifyCredentialResponse.ERROR;
        }
        VerifyCredentialResponse verifyCredentialResponseSpBasedDoVerifyCredential = spBasedDoVerifyCredential(str, i, z, j, i2, iCheckCredentialProgressCallback);
        if (verifyCredentialResponseSpBasedDoVerifyCredential != null) {
            if (verifyCredentialResponseSpBasedDoVerifyCredential.getResponseCode() == 0) {
                this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(i, str, i2);
            }
            return verifyCredentialResponseSpBasedDoVerifyCredential;
        }
        if (i2 == -9999) {
            Slog.wtf(TAG, "Unexpected FRP credential type, should be SP based.");
            return VerifyCredentialResponse.ERROR;
        }
        LockSettingsStorage.CredentialHash credentialHash = this.mStorage.readCredentialHash(i2);
        if (credentialHash.type != i) {
            Slog.wtf(TAG, "doVerifyCredential type mismatch with stored credential?? stored: " + credentialHash.type + " passed in: " + i);
            return VerifyCredentialResponse.ERROR;
        }
        boolean z2 = credentialHash.type == 1 && credentialHash.isBaseZeroPattern;
        if (z2) {
            strPatternStringToBaseZero = LockPatternUtils.patternStringToBaseZero(str);
        } else {
            strPatternStringToBaseZero = str;
        }
        VerifyCredentialResponse verifyCredentialResponseVerifyCredential = verifyCredential(i2, credentialHash, strPatternStringToBaseZero, z, j, iCheckCredentialProgressCallback);
        if (verifyCredentialResponseVerifyCredential.getResponseCode() == 0) {
            this.mStrongAuth.reportSuccessfulStrongAuthUnlock(i2);
            if (z2) {
                setLockCredentialInternal(str, credentialHash.type, strPatternStringToBaseZero, 65536, i2);
            }
        }
        return verifyCredentialResponseVerifyCredential;
    }

    public VerifyCredentialResponse verifyTiedProfileChallenge(String str, int i, long j, int i2) throws Throwable {
        checkPasswordReadPermission(i2);
        if (!isManagedProfileWithUnifiedLock(i2)) {
            throw new RemoteException("User id must be managed profile with unified lock");
        }
        VerifyCredentialResponse verifyCredentialResponseDoVerifyCredential = doVerifyCredential(str, i, true, j, this.mUserManager.getProfileParent(i2).id, null);
        if (verifyCredentialResponseDoVerifyCredential.getResponseCode() != 0) {
            return verifyCredentialResponseDoVerifyCredential;
        }
        try {
            return doVerifyCredential(getDecryptedPasswordForTiedProfile(i2), 2, true, j, i2, null);
        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Slog.e(TAG, "Failed to decrypt child profile key", e);
            throw new RemoteException("Unable to get tied profile token");
        }
    }

    private VerifyCredentialResponse verifyCredential(int i, LockSettingsStorage.CredentialHash credentialHash, String str, boolean z, long j, ICheckCredentialProgressCallback iCheckCredentialProgressCallback) throws Throwable {
        byte[] bytes;
        if ((credentialHash == null || credentialHash.hash.length == 0) && TextUtils.isEmpty(str)) {
            return VerifyCredentialResponse.OK;
        }
        if (credentialHash == null || TextUtils.isEmpty(str)) {
            return VerifyCredentialResponse.ERROR;
        }
        StrictMode.noteDiskRead();
        if (credentialHash.version == 0) {
            if (credentialHash.type == 1) {
                bytes = LockPatternUtils.patternToHash(LockPatternUtils.stringToPattern(str));
            } else {
                bytes = this.mLockPatternUtils.legacyPasswordToHash(str, i).getBytes(StandardCharsets.UTF_8);
            }
            if (Arrays.equals(bytes, credentialHash.hash)) {
                if (credentialHash.type == 1) {
                    unlockKeystore(LockPatternUtils.patternStringToBaseZero(str), i);
                } else {
                    unlockKeystore(str, i);
                }
                Slog.i(TAG, "Unlocking user with fake token: " + i);
                byte[] bytes2 = String.valueOf(i).getBytes();
                unlockUser(i, bytes2, bytes2);
                setLockCredentialInternal(str, credentialHash.type, null, credentialHash.type == 1 ? 65536 : 327680, i);
                if (!z) {
                    notifyActivePasswordMetricsAvailable(str, i);
                    this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(credentialHash.type, str, i);
                    return VerifyCredentialResponse.OK;
                }
            } else {
                return VerifyCredentialResponse.ERROR;
            }
        }
        GateKeeperResponse gateKeeperResponseVerifyChallenge = getGateKeeperService().verifyChallenge(i, j, credentialHash.hash, str.getBytes());
        VerifyCredentialResponse verifyCredentialResponseConvertResponse = convertResponse(gateKeeperResponseVerifyChallenge);
        boolean shouldReEnroll = gateKeeperResponseVerifyChallenge.getShouldReEnroll();
        if (verifyCredentialResponseConvertResponse.getResponseCode() == 0) {
            if (iCheckCredentialProgressCallback != null) {
                iCheckCredentialProgressCallback.onCredentialVerified();
            }
            notifyActivePasswordMetricsAvailable(str, i);
            unlockKeystore(str, i);
            Slog.i(TAG, "Unlocking user " + i + " with token length " + verifyCredentialResponseConvertResponse.getPayload().length);
            unlockUser(i, verifyCredentialResponseConvertResponse.getPayload(), secretFromCredential(str));
            if (isManagedProfileWithSeparatedLock(i)) {
                ((TrustManager) this.mContext.getSystemService("trust")).setDeviceLockedForUser(i, false);
            }
            int i2 = credentialHash.type == 1 ? 65536 : 327680;
            if (shouldReEnroll) {
                setLockCredentialInternal(str, credentialHash.type, str, i2, i);
            } else {
                synchronized (this.mSpManager) {
                    if (shouldMigrateToSyntheticPasswordLocked(i)) {
                        activateEscrowTokens(initializeSyntheticPasswordLocked(credentialHash.hash, str, credentialHash.type, i2, i), i);
                    }
                }
            }
            this.mRecoverableKeyStoreManager.lockScreenSecretAvailable(credentialHash.type, str, i);
        } else if (verifyCredentialResponseConvertResponse.getResponseCode() == 1 && verifyCredentialResponseConvertResponse.getTimeout() > 0) {
            requireStrongAuth(8, i);
        }
        return verifyCredentialResponseConvertResponse;
    }

    private void notifyActivePasswordMetricsAvailable(String str, final int i) {
        final PasswordMetrics passwordMetricsComputeForPassword;
        if (str == null) {
            passwordMetricsComputeForPassword = new PasswordMetrics();
        } else {
            passwordMetricsComputeForPassword = PasswordMetrics.computeForPassword(str);
            passwordMetricsComputeForPassword.quality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(i);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                ((DevicePolicyManager) this.f$0.mContext.getSystemService("device_policy")).setActivePasswordState(passwordMetricsComputeForPassword, i);
            }
        });
    }

    private void notifyPasswordChanged(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                ((DevicePolicyManager) this.f$0.mContext.getSystemService("device_policy")).reportPasswordChanged(i);
            }
        });
    }

    public boolean checkVoldPassword(int i) throws RemoteException {
        if (!this.mFirstCallToVold) {
            return false;
        }
        this.mFirstCallToVold = false;
        checkPasswordReadPermission(i);
        IStorageManager storageManager = this.mInjector.getStorageManager();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String password = storageManager.getPassword();
            storageManager.clearPassword();
            if (password == null) {
                return false;
            }
            try {
                if (this.mLockPatternUtils.isLockPatternEnabled(i)) {
                    if (checkCredential(password, 1, i, null).getResponseCode() == 0) {
                        return true;
                    }
                }
            } catch (Exception e) {
            }
            try {
                if (this.mLockPatternUtils.isLockPasswordEnabled(i)) {
                    if (checkCredential(password, 2, i, null).getResponseCode() == 0) {
                        return true;
                    }
                }
            } catch (Exception e2) {
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void removeUser(int i, boolean z) {
        this.mSpManager.removeUser(i);
        this.mStorage.removeUser(i);
        this.mStrongAuth.removeUser(i);
        tryRemoveUserFromSpCacheLater(i);
        android.security.KeyStore.getInstance().onUserRemoved(i);
        try {
            IGateKeeperService gateKeeperService = getGateKeeperService();
            if (gateKeeperService != null) {
                gateKeeperService.clearSecureUserId(i);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "unable to clear GK secure user id");
        }
        if (z || this.mUserManager.getUserInfo(i).isManagedProfile()) {
            removeKeystoreProfileKey(i);
        }
    }

    private void removeKeystoreProfileKey(int i) {
        try {
            java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry("profile_key_name_encrypt_" + i);
            keyStore.deleteEntry("profile_key_name_decrypt_" + i);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            Slog.e(TAG, "Unable to remove keystore profile key for user:" + i, e);
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker iStrongAuthTracker) {
        checkPasswordReadPermission(-1);
        this.mStrongAuth.registerStrongAuthTracker(iStrongAuthTracker);
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker iStrongAuthTracker) {
        checkPasswordReadPermission(-1);
        this.mStrongAuth.unregisterStrongAuthTracker(iStrongAuthTracker);
    }

    public void requireStrongAuth(int i, int i2) {
        checkWritePermission(i2);
        this.mStrongAuth.requireStrongAuth(i, i2);
    }

    public void userPresent(int i) {
        checkWritePermission(i);
        this.mStrongAuth.reportUnlock(i);
    }

    public int getStrongAuthForUser(int i) {
        checkPasswordReadPermission(i);
        return this.mStrongAuthTracker.getStrongAuthForUser(i);
    }

    private boolean isCallerShell() {
        int callingUid = Binder.getCallingUid();
        return callingUid == 2000 || callingUid == 0;
    }

    private void enforceShell() {
        if (!isCallerShell()) {
            throw new SecurityException("Caller must be shell");
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
        enforceShell();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            new LockSettingsShellCommand(this.mContext, new LockPatternUtils(this.mContext)).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void initRecoveryServiceWithSigFile(String str, byte[] bArr, byte[] bArr2) throws Exception {
        this.mRecoverableKeyStoreManager.initRecoveryServiceWithSigFile(str, bArr, bArr2);
    }

    public KeyChainSnapshot getKeyChainSnapshot() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getKeyChainSnapshot();
    }

    public void setSnapshotCreatedPendingIntent(PendingIntent pendingIntent) throws RemoteException {
        this.mRecoverableKeyStoreManager.setSnapshotCreatedPendingIntent(pendingIntent);
    }

    public void setServerParams(byte[] bArr) throws RemoteException, ServiceSpecificException {
        this.mRecoverableKeyStoreManager.setServerParams(bArr);
    }

    public void setRecoveryStatus(String str, int i) throws RemoteException, ServiceSpecificException {
        this.mRecoverableKeyStoreManager.setRecoveryStatus(str, i);
    }

    public Map getRecoveryStatus() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getRecoveryStatus();
    }

    public void setRecoverySecretTypes(int[] iArr) throws Exception {
        this.mRecoverableKeyStoreManager.setRecoverySecretTypes(iArr);
    }

    public int[] getRecoverySecretTypes() throws RemoteException {
        return this.mRecoverableKeyStoreManager.getRecoverySecretTypes();
    }

    public byte[] startRecoverySessionWithCertPath(String str, String str2, RecoveryCertPath recoveryCertPath, byte[] bArr, byte[] bArr2, List<KeyChainProtectionParams> list) throws RemoteException {
        return this.mRecoverableKeyStoreManager.startRecoverySessionWithCertPath(str, str2, recoveryCertPath, bArr, bArr2, list);
    }

    public Map<String, String> recoverKeyChainSnapshot(String str, byte[] bArr, List<WrappedApplicationKey> list) throws RemoteException {
        return this.mRecoverableKeyStoreManager.recoverKeyChainSnapshot(str, bArr, list);
    }

    public void closeSession(String str) throws RemoteException {
        this.mRecoverableKeyStoreManager.closeSession(str);
    }

    public void removeKey(String str) throws RemoteException, ServiceSpecificException {
        this.mRecoverableKeyStoreManager.removeKey(str);
    }

    public String generateKey(String str) throws RemoteException {
        return this.mRecoverableKeyStoreManager.generateKey(str);
    }

    public String importKey(String str, byte[] bArr) throws RemoteException {
        return this.mRecoverableKeyStoreManager.importKey(str, bArr);
    }

    public String getKey(String str) throws RemoteException {
        return this.mRecoverableKeyStoreManager.getKey(str);
    }

    private class GateKeeperDiedRecipient implements IBinder.DeathRecipient {
        private GateKeeperDiedRecipient() {
        }

        @Override
        public void binderDied() {
            LockSettingsService.this.mGateKeeperService.asBinder().unlinkToDeath(this, 0);
            LockSettingsService.this.mGateKeeperService = null;
        }
    }

    protected synchronized IGateKeeperService getGateKeeperService() throws RemoteException {
        if (this.mGateKeeperService != null) {
            return this.mGateKeeperService;
        }
        IBinder service = ServiceManager.getService("android.service.gatekeeper.IGateKeeperService");
        if (service != null) {
            service.linkToDeath(new GateKeeperDiedRecipient(), 0);
            this.mGateKeeperService = IGateKeeperService.Stub.asInterface(service);
            return this.mGateKeeperService;
        }
        Slog.e(TAG, "Unable to acquire GateKeeperService");
        return null;
    }

    private void onAuthTokenKnownForUser(int i, SyntheticPasswordManager.AuthenticationToken authenticationToken) {
        Slog.i(TAG, "Caching SP for user " + i);
        synchronized (this.mSpManager) {
            this.mSpCache.put(i, authenticationToken);
        }
        tryRemoveUserFromSpCacheLater(i);
        if (this.mAuthSecretService != null && this.mUserManager.getUserInfo(i).isPrimary()) {
            try {
                byte[] bArrDeriveVendorAuthSecret = authenticationToken.deriveVendorAuthSecret();
                ArrayList<Byte> arrayList = new ArrayList<>(bArrDeriveVendorAuthSecret.length);
                for (byte b : bArrDeriveVendorAuthSecret) {
                    arrayList.add(Byte.valueOf(b));
                }
                this.mAuthSecretService.primaryUserCredential(arrayList);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to pass primary user secret to AuthSecret HAL", e);
            }
        }
    }

    private void tryRemoveUserFromSpCacheLater(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                LockSettingsService.lambda$tryRemoveUserFromSpCacheLater$2(this.f$0, i);
            }
        });
    }

    public static void lambda$tryRemoveUserFromSpCacheLater$2(LockSettingsService lockSettingsService, int i) {
        if (!lockSettingsService.shouldCacheSpForUser(i)) {
            Slog.i(TAG, "Removing SP from cache for user " + i);
            synchronized (lockSettingsService.mSpManager) {
                lockSettingsService.mSpCache.remove(i);
            }
        }
    }

    private boolean shouldCacheSpForUser(int i) {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, i) == 0) {
            return true;
        }
        DevicePolicyManagerInternal devicePolicyManagerInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (devicePolicyManagerInternal == null) {
            return false;
        }
        return devicePolicyManagerInternal.canUserHaveUntrustedCredentialReset(i);
    }

    @GuardedBy("mSpManager")
    @VisibleForTesting
    protected SyntheticPasswordManager.AuthenticationToken initializeSyntheticPasswordLocked(byte[] bArr, String str, int i, int i2, int i3) throws RemoteException {
        Slog.i(TAG, "Initialize SyntheticPassword for user: " + i3);
        SyntheticPasswordManager.AuthenticationToken authenticationTokenNewSyntheticPasswordAndSid = this.mSpManager.newSyntheticPasswordAndSid(getGateKeeperService(), bArr, str, i3);
        onAuthTokenKnownForUser(i3, authenticationTokenNewSyntheticPasswordAndSid);
        if (authenticationTokenNewSyntheticPasswordAndSid == null) {
            Slog.wtf(TAG, "initializeSyntheticPasswordLocked returns null auth token");
            return null;
        }
        long jCreatePasswordBasedSyntheticPassword = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), str, i, authenticationTokenNewSyntheticPasswordAndSid, i2, i3);
        if (str != null) {
            if (bArr == null) {
                this.mSpManager.newSidForUser(getGateKeeperService(), authenticationTokenNewSyntheticPasswordAndSid, i3);
            }
            this.mSpManager.verifyChallenge(getGateKeeperService(), authenticationTokenNewSyntheticPasswordAndSid, 0L, i3);
            setAuthlessUserKeyProtection(i3, authenticationTokenNewSyntheticPasswordAndSid.deriveDiskEncryptionKey());
            setKeystorePassword(authenticationTokenNewSyntheticPasswordAndSid.deriveKeyStorePassword(), i3);
        } else {
            clearUserKeyProtection(i3);
            setKeystorePassword(null, i3);
            getGateKeeperService().clearSecureUserId(i3);
        }
        fixateNewestUserKeyAuth(i3);
        setLong("sp-handle", jCreatePasswordBasedSyntheticPassword, i3);
        return authenticationTokenNewSyntheticPasswordAndSid;
    }

    private long getSyntheticPasswordHandleLocked(int i) {
        return getLong("sp-handle", 0L, i);
    }

    private boolean isSyntheticPasswordBasedCredentialLocked(int i) {
        if (i != -9999) {
            return (getLong("enable-sp", 1L, 0) == 0 || getSyntheticPasswordHandleLocked(i) == 0) ? false : true;
        }
        int i2 = this.mStorage.readPersistentDataBlock().type;
        return i2 == 1 || i2 == 2;
    }

    @VisibleForTesting
    protected boolean shouldMigrateToSyntheticPasswordLocked(int i) {
        return getLong("enable-sp", 1L, 0) != 0 && getSyntheticPasswordHandleLocked(i) == 0;
    }

    private void enableSyntheticPasswordLocked() {
        setLong("enable-sp", 1L, 0);
    }

    private VerifyCredentialResponse spBasedDoVerifyCredential(String str, int i, boolean z, long j, int i2, ICheckCredentialProgressCallback iCheckCredentialProgressCallback) throws RemoteException {
        if (i == -1) {
            str = null;
        }
        synchronized (this.mSpManager) {
            if (!isSyntheticPasswordBasedCredentialLocked(i2)) {
                return null;
            }
            if (i2 == -9999) {
                return this.mSpManager.verifyFrpCredential(getGateKeeperService(), str, i, iCheckCredentialProgressCallback);
            }
            SyntheticPasswordManager.AuthenticationResult authenticationResultUnwrapPasswordBasedSyntheticPassword = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(i2), str, i2, iCheckCredentialProgressCallback);
            if (authenticationResultUnwrapPasswordBasedSyntheticPassword.credentialType != i) {
                Slog.e(TAG, "Credential type mismatch.");
                return VerifyCredentialResponse.ERROR;
            }
            VerifyCredentialResponse verifyCredentialResponseVerifyChallenge = authenticationResultUnwrapPasswordBasedSyntheticPassword.gkResponse;
            if (verifyCredentialResponseVerifyChallenge.getResponseCode() == 0) {
                verifyCredentialResponseVerifyChallenge = this.mSpManager.verifyChallenge(getGateKeeperService(), authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken, j, i2);
                if (verifyCredentialResponseVerifyChallenge.getResponseCode() != 0) {
                    Slog.wtf(TAG, "verifyChallenge with SP failed.");
                    return VerifyCredentialResponse.ERROR;
                }
            }
            if (verifyCredentialResponseVerifyChallenge.getResponseCode() == 0) {
                notifyActivePasswordMetricsAvailable(str, i2);
                unlockKeystore(authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken.deriveKeyStorePassword(), i2);
                byte[] bArrDeriveDiskEncryptionKey = authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken.deriveDiskEncryptionKey();
                Slog.i(TAG, "Unlocking user " + i2 + " with secret only, length " + bArrDeriveDiskEncryptionKey.length);
                unlockUser(i2, null, bArrDeriveDiskEncryptionKey);
                activateEscrowTokens(authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken, i2);
                if (isManagedProfileWithSeparatedLock(i2)) {
                    ((TrustManager) this.mContext.getSystemService("trust")).setDeviceLockedForUser(i2, false);
                }
                this.mStrongAuth.reportSuccessfulStrongAuthUnlock(i2);
                onAuthTokenKnownForUser(i2, authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken);
            } else if (verifyCredentialResponseVerifyChallenge.getResponseCode() == 1 && verifyCredentialResponseVerifyChallenge.getTimeout() > 0) {
                requireStrongAuth(8, i2);
            }
            return verifyCredentialResponseVerifyChallenge;
        }
    }

    @GuardedBy("mSpManager")
    private long setLockCredentialWithAuthTokenLocked(String str, int i, SyntheticPasswordManager.AuthenticationToken authenticationToken, int i2, int i3) throws RemoteException {
        long jCreatePasswordBasedSyntheticPassword = this.mSpManager.createPasswordBasedSyntheticPassword(getGateKeeperService(), str, i, authenticationToken, i2, i3);
        Map<Integer, String> map = null;
        if (str != null) {
            if (this.mSpManager.hasSidForUser(i3)) {
                this.mSpManager.verifyChallenge(getGateKeeperService(), authenticationToken, 0L, i3);
            } else {
                this.mSpManager.newSidForUser(getGateKeeperService(), authenticationToken, i3);
                this.mSpManager.verifyChallenge(getGateKeeperService(), authenticationToken, 0L, i3);
                setAuthlessUserKeyProtection(i3, authenticationToken.deriveDiskEncryptionKey());
                fixateNewestUserKeyAuth(i3);
                setKeystorePassword(authenticationToken.deriveKeyStorePassword(), i3);
            }
        } else {
            Map<Integer, String> decryptedPasswordsForAllTiedProfiles = getDecryptedPasswordsForAllTiedProfiles(i3);
            this.mSpManager.clearSidForUser(i3);
            getGateKeeperService().clearSecureUserId(i3);
            clearUserKeyProtection(i3);
            fixateNewestUserKeyAuth(i3);
            setKeystorePassword(null, i3);
            map = decryptedPasswordsForAllTiedProfiles;
        }
        setLong("sp-handle", jCreatePasswordBasedSyntheticPassword, i3);
        synchronizeUnifiedWorkChallengeForProfiles(i3, map);
        notifyActivePasswordMetricsAvailable(str, i3);
        return jCreatePasswordBasedSyntheticPassword;
    }

    @GuardedBy("mSpManager")
    private void spBasedSetLockCredentialInternalLocked(String str, int i, String str2, int i2, int i3) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, UnrecoverableKeyException, RemoteException, IOException, InvalidKeyException, KeyStoreException, CertificateException, InvalidAlgorithmParameterException {
        if (isManagedProfileWithUnifiedLock(i3)) {
            try {
                str2 = getDecryptedPasswordForTiedProfile(i3);
            } catch (FileNotFoundException e) {
                Slog.i(TAG, "Child profile key not found");
            } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e2) {
                Slog.e(TAG, "Failed to decrypt child profile key", e2);
            }
        }
        long syntheticPasswordHandleLocked = getSyntheticPasswordHandleLocked(i3);
        SyntheticPasswordManager.AuthenticationResult authenticationResultUnwrapPasswordBasedSyntheticPassword = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), syntheticPasswordHandleLocked, str2, i3, null);
        VerifyCredentialResponse verifyCredentialResponse = authenticationResultUnwrapPasswordBasedSyntheticPassword.gkResponse;
        SyntheticPasswordManager.AuthenticationToken authenticationToken = authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken;
        if (str2 != null && authenticationToken == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to enroll ");
            sb.append(i == 2 ? "password" : "pattern");
            throw new RemoteException(sb.toString());
        }
        boolean z = false;
        if (authenticationToken != null) {
            onAuthTokenKnownForUser(i3, authenticationToken);
        } else if (verifyCredentialResponse != null && verifyCredentialResponse.getResponseCode() == -1) {
            Slog.w(TAG, "Untrusted credential change invoked");
            authenticationToken = this.mSpCache.get(i3);
            z = true;
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("spBasedSetLockCredentialInternalLocked: ");
            sb2.append(verifyCredentialResponse != null ? "rate limit exceeded" : "failed");
            Slog.w(TAG, sb2.toString());
            return;
        }
        SyntheticPasswordManager.AuthenticationToken authenticationToken2 = authenticationToken;
        if (authenticationToken2 != null) {
            if (z) {
                this.mSpManager.newSidForUser(getGateKeeperService(), authenticationToken2, i3);
            }
            setLockCredentialWithAuthTokenLocked(str, i, authenticationToken2, i2, i3);
            this.mSpManager.destroyPasswordBasedSyntheticPassword(syntheticPasswordHandleLocked, i3);
            this.mRecoverableKeyStoreManager.lockScreenSecretChanged(i, str, i3);
            return;
        }
        throw new IllegalStateException("Untrusted credential reset not possible without cached SP");
    }

    public byte[] getHashFactor(String str, int i) throws RemoteException {
        checkPasswordReadPermission(i);
        if (TextUtils.isEmpty(str)) {
            str = null;
        }
        if (isManagedProfileWithUnifiedLock(i)) {
            try {
                str = getDecryptedPasswordForTiedProfile(i);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to get work profile credential", e);
                return null;
            }
        }
        String str2 = str;
        synchronized (this.mSpManager) {
            if (!isSyntheticPasswordBasedCredentialLocked(i)) {
                Slog.w(TAG, "Synthetic password not enabled");
                return null;
            }
            SyntheticPasswordManager.AuthenticationResult authenticationResultUnwrapPasswordBasedSyntheticPassword = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(i), str2, i, null);
            if (authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken == null) {
                Slog.w(TAG, "Current credential is incorrect");
                return null;
            }
            return authenticationResultUnwrapPasswordBasedSyntheticPassword.authToken.derivePasswordHashFactor();
        }
    }

    private long addEscrowToken(byte[] bArr, int i) throws RemoteException {
        long jCreateTokenBasedSyntheticPassword;
        synchronized (this.mSpManager) {
            enableSyntheticPasswordLocked();
            SyntheticPasswordManager.AuthenticationToken authenticationTokenInitializeSyntheticPasswordLocked = null;
            if (!isUserSecure(i)) {
                if (shouldMigrateToSyntheticPasswordLocked(i)) {
                    authenticationTokenInitializeSyntheticPasswordLocked = initializeSyntheticPasswordLocked(null, null, -1, 0, i);
                } else {
                    authenticationTokenInitializeSyntheticPasswordLocked = this.mSpManager.unwrapPasswordBasedSyntheticPassword(getGateKeeperService(), getSyntheticPasswordHandleLocked(i), null, i, null).authToken;
                }
            }
            if (isSyntheticPasswordBasedCredentialLocked(i)) {
                disableEscrowTokenOnNonManagedDevicesIfNeeded(i);
                if (!this.mSpManager.hasEscrowData(i)) {
                    throw new SecurityException("Escrow token is disabled on the current user");
                }
            }
            jCreateTokenBasedSyntheticPassword = this.mSpManager.createTokenBasedSyntheticPassword(bArr, i);
            if (authenticationTokenInitializeSyntheticPasswordLocked != null) {
                this.mSpManager.activateTokenBasedSyntheticPassword(jCreateTokenBasedSyntheticPassword, authenticationTokenInitializeSyntheticPasswordLocked, i);
            }
        }
        return jCreateTokenBasedSyntheticPassword;
    }

    private void activateEscrowTokens(SyntheticPasswordManager.AuthenticationToken authenticationToken, int i) {
        synchronized (this.mSpManager) {
            disableEscrowTokenOnNonManagedDevicesIfNeeded(i);
            Iterator<Long> it = this.mSpManager.getPendingTokensForUser(i).iterator();
            while (it.hasNext()) {
                long jLongValue = it.next().longValue();
                Slog.i(TAG, String.format("activateEscrowTokens: %x %d ", Long.valueOf(jLongValue), Integer.valueOf(i)));
                this.mSpManager.activateTokenBasedSyntheticPassword(jLongValue, authenticationToken, i);
            }
        }
    }

    private boolean isEscrowTokenActive(long j, int i) {
        boolean zExistsHandle;
        synchronized (this.mSpManager) {
            zExistsHandle = this.mSpManager.existsHandle(j, i);
        }
        return zExistsHandle;
    }

    private boolean removeEscrowToken(long j, int i) {
        synchronized (this.mSpManager) {
            if (j == getSyntheticPasswordHandleLocked(i)) {
                Slog.w(TAG, "Cannot remove password handle");
                return false;
            }
            if (this.mSpManager.removePendingToken(j, i)) {
                return true;
            }
            if (!this.mSpManager.existsHandle(j, i)) {
                return false;
            }
            this.mSpManager.destroyTokenBasedSyntheticPassword(j, i);
            return true;
        }
    }

    private boolean setLockCredentialWithToken(String str, int i, long j, byte[] bArr, int i2, int i3) throws RemoteException {
        boolean lockCredentialWithTokenInternal;
        synchronized (this.mSpManager) {
            if (!this.mSpManager.hasEscrowData(i3)) {
                throw new SecurityException("Escrow token is disabled on the current user");
            }
            lockCredentialWithTokenInternal = setLockCredentialWithTokenInternal(str, i, j, bArr, i2, i3);
        }
        if (lockCredentialWithTokenInternal) {
            synchronized (this.mSeparateChallengeLock) {
                setSeparateProfileChallengeEnabledLocked(i3, true, null);
            }
            notifyPasswordChanged(i3);
            notifySeparateProfileChallengeChanged(i3);
        }
        return lockCredentialWithTokenInternal;
    }

    private boolean setLockCredentialWithTokenInternal(String str, int i, long j, byte[] bArr, int i2, int i3) throws RemoteException {
        synchronized (this.mSpManager) {
            SyntheticPasswordManager.AuthenticationResult authenticationResultUnwrapTokenBasedSyntheticPassword = this.mSpManager.unwrapTokenBasedSyntheticPassword(getGateKeeperService(), j, bArr, i3);
            if (authenticationResultUnwrapTokenBasedSyntheticPassword.authToken == null) {
                Slog.w(TAG, "Invalid escrow token supplied");
                return false;
            }
            if (authenticationResultUnwrapTokenBasedSyntheticPassword.gkResponse.getResponseCode() != 0) {
                Slog.e(TAG, "Obsolete token: synthetic password derived but it fails GK verification.");
                return false;
            }
            setLong("lockscreen.password_type", i2, i3);
            long syntheticPasswordHandleLocked = getSyntheticPasswordHandleLocked(i3);
            setLockCredentialWithAuthTokenLocked(str, i, authenticationResultUnwrapTokenBasedSyntheticPassword.authToken, i2, i3);
            this.mSpManager.destroyPasswordBasedSyntheticPassword(syntheticPasswordHandleLocked, i3);
            onAuthTokenKnownForUser(i3, authenticationResultUnwrapTokenBasedSyntheticPassword.authToken);
            return true;
        }
    }

    private boolean unlockUserWithToken(long j, byte[] bArr, int i) throws RemoteException {
        synchronized (this.mSpManager) {
            if (!this.mSpManager.hasEscrowData(i)) {
                throw new SecurityException("Escrow token is disabled on the current user");
            }
            SyntheticPasswordManager.AuthenticationResult authenticationResultUnwrapTokenBasedSyntheticPassword = this.mSpManager.unwrapTokenBasedSyntheticPassword(getGateKeeperService(), j, bArr, i);
            if (authenticationResultUnwrapTokenBasedSyntheticPassword.authToken == null) {
                Slog.w(TAG, "Invalid escrow token supplied");
                return false;
            }
            unlockUser(i, null, authenticationResultUnwrapTokenBasedSyntheticPassword.authToken.deriveDiskEncryptionKey());
            onAuthTokenKnownForUser(i, authenticationResultUnwrapTokenBasedSyntheticPassword.authToken);
            return true;
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("Current lock settings service state:");
            printWriter.println(String.format("SP Enabled = %b", Boolean.valueOf(this.mLockPatternUtils.isSyntheticPasswordEnabled())));
            List users = this.mUserManager.getUsers();
            for (int i = 0; i < users.size(); i++) {
                int i2 = ((UserInfo) users.get(i)).id;
                printWriter.println("    User " + i2);
                synchronized (this.mSpManager) {
                    printWriter.println(String.format("        SP Handle = %x", Long.valueOf(getSyntheticPasswordHandleLocked(i2))));
                }
                try {
                    printWriter.println(String.format("        SID = %x", Long.valueOf(getGateKeeperService().getSecureUserId(i2))));
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void disableEscrowTokenOnNonManagedDevicesIfNeeded(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mUserManager.getUserInfo(i).isManagedProfile()) {
                Slog.i(TAG, "Managed profile can have escrow token");
                return;
            }
            DevicePolicyManager devicePolicyManager = this.mInjector.getDevicePolicyManager();
            if (devicePolicyManager.getDeviceOwnerComponentOnAnyUser() != null) {
                Slog.i(TAG, "Corp-owned device can have escrow token");
                return;
            }
            if (devicePolicyManager.getProfileOwnerAsUser(i) != null) {
                Slog.i(TAG, "User with profile owner can have escrow token");
                return;
            }
            if (!devicePolicyManager.isDeviceProvisioned()) {
                Slog.i(TAG, "Postpone disabling escrow tokens until device is provisioned");
                return;
            }
            if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                return;
            }
            Slog.i(TAG, "Disabling escrow token on user " + i);
            if (isSyntheticPasswordBasedCredentialLocked(i)) {
                this.mSpManager.destroyEscrowData(i);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private class DeviceProvisionedObserver extends ContentObserver {
        private final Uri mDeviceProvisionedUri;
        private boolean mRegistered;
        private final Uri mUserSetupCompleteUri;

        public DeviceProvisionedObserver() {
            super(null);
            this.mDeviceProvisionedUri = Settings.Global.getUriFor("device_provisioned");
            this.mUserSetupCompleteUri = Settings.Secure.getUriFor("user_setup_complete");
        }

        @Override
        public void onChange(boolean z, Uri uri, int i) {
            if (this.mDeviceProvisionedUri.equals(uri)) {
                updateRegistration();
                if (isProvisioned()) {
                    Slog.i(LockSettingsService.TAG, "Reporting device setup complete to IGateKeeperService");
                    reportDeviceSetupComplete();
                    clearFrpCredentialIfOwnerNotSecure();
                    return;
                }
                return;
            }
            if (this.mUserSetupCompleteUri.equals(uri)) {
                LockSettingsService.this.tryRemoveUserFromSpCacheLater(i);
            }
        }

        public void onSystemReady() {
            if (LockPatternUtils.frpCredentialEnabled(LockSettingsService.this.mContext)) {
                updateRegistration();
            } else if (!isProvisioned()) {
                Slog.i(LockSettingsService.TAG, "FRP credential disabled, reporting device setup complete to Gatekeeper immediately");
                reportDeviceSetupComplete();
            }
        }

        private void reportDeviceSetupComplete() {
            try {
                LockSettingsService.this.getGateKeeperService().reportDeviceSetupComplete();
            } catch (RemoteException e) {
                Slog.e(LockSettingsService.TAG, "Failure reporting to IGateKeeperService", e);
            }
        }

        private void clearFrpCredentialIfOwnerNotSecure() {
            for (UserInfo userInfo : LockSettingsService.this.mUserManager.getUsers()) {
                if (LockPatternUtils.userOwnsFrpCredential(LockSettingsService.this.mContext, userInfo)) {
                    if (!LockSettingsService.this.isUserSecure(userInfo.id)) {
                        LockSettingsService.this.mStorage.writePersistentDataBlock(0, userInfo.id, 0, null);
                        return;
                    }
                    return;
                }
            }
        }

        private void updateRegistration() {
            boolean z = !isProvisioned();
            if (z == this.mRegistered) {
                return;
            }
            if (z) {
                LockSettingsService.this.mContext.getContentResolver().registerContentObserver(this.mDeviceProvisionedUri, false, this);
                LockSettingsService.this.mContext.getContentResolver().registerContentObserver(this.mUserSetupCompleteUri, false, this, -1);
            } else {
                LockSettingsService.this.mContext.getContentResolver().unregisterContentObserver(this);
            }
            this.mRegistered = z;
        }

        private boolean isProvisioned() {
            return Settings.Global.getInt(LockSettingsService.this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        }
    }

    private final class LocalService extends LockSettingsInternal {
        private LocalService() {
        }

        public long addEscrowToken(byte[] bArr, int i) {
            try {
                return LockSettingsService.this.addEscrowToken(bArr, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public boolean removeEscrowToken(long j, int i) {
            return LockSettingsService.this.removeEscrowToken(j, i);
        }

        public boolean isEscrowTokenActive(long j, int i) {
            return LockSettingsService.this.isEscrowTokenActive(j, i);
        }

        public boolean setLockCredentialWithToken(String str, int i, long j, byte[] bArr, int i2, int i3) {
            try {
                return LockSettingsService.this.setLockCredentialWithToken(str, i, j, bArr, i2, i3);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public boolean unlockUserWithToken(long j, byte[] bArr, int i) {
            try {
                return LockSettingsService.this.unlockUserWithToken(j, bArr, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }
}
