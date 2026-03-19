package com.android.server.devicepolicy;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.IStopUserCallback;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyCache;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.NetworkEvent;
import android.app.admin.PasswordMetrics;
import android.app.admin.SecurityLog;
import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.app.backup.IBackupManager;
import android.app.backup.ISelectBackupTransportCallback;
import android.app.trust.TrustManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.StringParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.IAudioService;
import android.net.ConnectivityManager;
import android.net.IIpConnectivityMetrics;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RecoverySystem;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.provider.ContactsContract;
import android.provider.ContactsInternal;
import android.provider.Settings;
import android.provider.Telephony;
import android.security.IKeyChainAliasCallback;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.BatteryService;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.NetworkManagementService;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.devicepolicy.TransferOwnershipMetadataManager;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.google.android.collect.Sets;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DevicePolicyManagerService extends BaseIDevicePolicyManager {
    private static final String ACTION_EXPIRED_PASSWORD_NOTIFICATION = "com.android.server.ACTION_EXPIRED_PASSWORD_NOTIFICATION";
    private static final String ATTR_ALIAS = "alias";
    private static final String ATTR_APPLICATION_RESTRICTIONS_MANAGER = "application-restrictions-manager";
    private static final String ATTR_DELEGATED_CERT_INSTALLER = "delegated-cert-installer";
    private static final String ATTR_DEVICE_PAIRED = "device-paired";
    private static final String ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED = "device-provisioning-config-applied";
    private static final String ATTR_DISABLED = "disabled";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PERMISSION_POLICY = "permission-policy";
    private static final String ATTR_PERMISSION_PROVIDER = "permission-provider";
    private static final String ATTR_PROVISIONING_STATE = "provisioning-state";
    private static final String ATTR_SETUP_COMPLETE = "setup-complete";
    private static final String ATTR_VALUE = "value";
    private static final int DEVICE_ADMIN_DEACTIVATE_TIMEOUT = 10000;
    private static final String DEVICE_POLICIES_XML = "device_policies.xml";
    private static final String DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML = "do-not-ask-credentials-on-boot";
    private static boolean ENABLE_LOCK_GUARD = false;
    private static final Set<String> GLOBAL_SETTINGS_DEPRECATED;
    private static final Set<String> GLOBAL_SETTINGS_WHITELIST;
    protected static final String LOG_TAG = "DevicePolicyManager";
    private static final String LOG_TAG_DEVICE_OWNER = "device-owner";
    private static final String LOG_TAG_PROFILE_OWNER = "profile-owner";
    private static final long MINIMUM_STRONG_AUTH_TIMEOUT_MS;
    private static final int PROFILE_KEYGUARD_FEATURES = 440;
    private static final int PROFILE_KEYGUARD_FEATURES_PROFILE_ONLY = 8;
    private static final String PROPERTY_DEVICE_OWNER_PRESENT = "ro.device_owner";
    private static final int REQUEST_EXPIRE_PASSWORD = 5571;
    private static final Set<String> SECURE_SETTINGS_DEVICEOWNER_WHITELIST;
    private static final int STATUS_BAR_DISABLE2_MASK = 1;
    private static final int STATUS_BAR_DISABLE_MASK = 34013184;
    private static final Set<String> SYSTEM_SETTINGS_WHITELIST;
    private static final String TAG_ACCEPTED_CA_CERTIFICATES = "accepted-ca-certificate";
    private static final String TAG_ADMIN_BROADCAST_PENDING = "admin-broadcast-pending";
    private static final String TAG_AFFILIATION_ID = "affiliation-id";
    private static final String TAG_CURRENT_INPUT_METHOD_SET = "current-ime-set";
    private static final String TAG_INITIALIZATION_BUNDLE = "initialization-bundle";
    private static final String TAG_LAST_BUG_REPORT_REQUEST = "last-bug-report-request";
    private static final String TAG_LAST_NETWORK_LOG_RETRIEVAL = "last-network-log-retrieval";
    private static final String TAG_LAST_SECURITY_LOG_RETRIEVAL = "last-security-log-retrieval";
    private static final String TAG_LOCK_TASK_COMPONENTS = "lock-task-component";
    private static final String TAG_LOCK_TASK_FEATURES = "lock-task-features";
    private static final String TAG_OWNER_INSTALLED_CA_CERT = "owner-installed-ca-cert";
    private static final String TAG_PASSWORD_TOKEN_HANDLE = "password-token";
    private static final String TAG_PASSWORD_VALIDITY = "password-validity";
    private static final String TAG_STATUS_BAR = "statusbar";
    private static final String TAG_TRANSFER_OWNERSHIP_BUNDLE = "transfer-ownership-bundle";
    private static final String TRANSFER_OWNERSHIP_PARAMETERS_XML = "transfer-ownership-parameters.xml";
    private static final boolean VERBOSE_LOG = false;
    final Handler mBackgroundHandler;
    private final CertificateMonitor mCertificateMonitor;
    private final DevicePolicyConstants mConstants;
    final Context mContext;
    private final DeviceAdminServiceController mDeviceAdminServiceController;
    final Handler mHandler;
    final boolean mHasFeature;
    final IPackageManager mIPackageManager;
    final Injector mInjector;
    final boolean mIsWatch;
    final LocalService mLocalService;
    private final Object mLockDoNoUseDirectly;
    private final LockPatternUtils mLockPatternUtils;

    @GuardedBy("getLockObject()")
    private NetworkLogger mNetworkLogger;
    private final OverlayPackagesProvider mOverlayPackagesProvider;

    @VisibleForTesting
    final Owners mOwners;
    private final Set<Pair<String, Integer>> mPackagesToRemove;
    private final DevicePolicyCacheImpl mPolicyCache;
    final BroadcastReceiver mReceiver;
    private final BroadcastReceiver mRemoteBugreportConsentReceiver;
    private final BroadcastReceiver mRemoteBugreportFinishedReceiver;
    private final AtomicBoolean mRemoteBugreportServiceIsActive;
    private final AtomicBoolean mRemoteBugreportSharingAccepted;
    private final Runnable mRemoteBugreportTimeoutRunnable;
    private final SecurityLogMonitor mSecurityLogMonitor;
    private final SetupContentObserver mSetupContentObserver;
    private final StatLogger mStatLogger;
    final TelephonyManager mTelephonyManager;
    private final Binder mToken;

    @VisibleForTesting
    final TransferOwnershipMetadataManager mTransferOwnershipMetadataManager;
    final UsageStatsManagerInternal mUsageStatsManagerInternal;

    @GuardedBy("getLockObject()")
    final SparseArray<DevicePolicyData> mUserData;
    final UserManager mUserManager;
    final UserManagerInternal mUserManagerInternal;

    @GuardedBy("getLockObject()")
    final SparseArray<PasswordMetrics> mUserPasswordMetrics;
    private static final long MS_PER_DAY = TimeUnit.DAYS.toMillis(1);
    private static final long EXPIRATION_GRACE_PERIOD_MS = 5 * MS_PER_DAY;
    private static final String[] DELEGATIONS = {"delegation-cert-install", "delegation-app-restrictions", "delegation-block-uninstall", "delegation-enable-system-app", "delegation-keep-uninstalled-packages", "delegation-package-access", "delegation-permission-grant", "delegation-install-existing-package", "delegation-keep-uninstalled-packages"};
    private static final Set<String> SECURE_SETTINGS_WHITELIST = new ArraySet();

    interface Stats {
        public static final int COUNT = 1;
        public static final int LOCK_GUARD_GUARD = 0;
    }

    static {
        SECURE_SETTINGS_WHITELIST.add("default_input_method");
        SECURE_SETTINGS_WHITELIST.add("skip_first_use_hints");
        SECURE_SETTINGS_WHITELIST.add("install_non_market_apps");
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST = new ArraySet();
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST.addAll(SECURE_SETTINGS_WHITELIST);
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST.add("location_mode");
        GLOBAL_SETTINGS_WHITELIST = new ArraySet();
        GLOBAL_SETTINGS_WHITELIST.add("adb_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("auto_time");
        GLOBAL_SETTINGS_WHITELIST.add("auto_time_zone");
        GLOBAL_SETTINGS_WHITELIST.add("data_roaming");
        GLOBAL_SETTINGS_WHITELIST.add("usb_mass_storage_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("wifi_sleep_policy");
        GLOBAL_SETTINGS_WHITELIST.add("stay_on_while_plugged_in");
        GLOBAL_SETTINGS_WHITELIST.add("wifi_device_owner_configs_lockdown");
        GLOBAL_SETTINGS_DEPRECATED = new ArraySet();
        GLOBAL_SETTINGS_DEPRECATED.add("bluetooth_on");
        GLOBAL_SETTINGS_DEPRECATED.add("development_settings_enabled");
        GLOBAL_SETTINGS_DEPRECATED.add("mode_ringer");
        GLOBAL_SETTINGS_DEPRECATED.add("network_preference");
        GLOBAL_SETTINGS_DEPRECATED.add("wifi_on");
        SYSTEM_SETTINGS_WHITELIST = new ArraySet();
        SYSTEM_SETTINGS_WHITELIST.add("screen_brightness");
        SYSTEM_SETTINGS_WHITELIST.add("screen_brightness_mode");
        SYSTEM_SETTINGS_WHITELIST.add("screen_off_timeout");
        MINIMUM_STRONG_AUTH_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1L);
        boolean z = true;
        if (!Build.IS_ENG && SystemProperties.getInt("debug.dpm.lock_guard", 0) != 1) {
            z = false;
        }
        ENABLE_LOCK_GUARD = z;
    }

    final Object getLockObject() {
        if (ENABLE_LOCK_GUARD) {
            long time = this.mStatLogger.getTime();
            LockGuard.guard(7);
            this.mStatLogger.logDurationStat(0, time);
        }
        return this.mLockDoNoUseDirectly;
    }

    final void ensureLocked() {
        if (Thread.holdsLock(this.mLockDoNoUseDirectly)) {
            return;
        }
        Slog.wtfStack(LOG_TAG, "Not holding DPMS lock.");
    }

    public static final class Lifecycle extends SystemService {
        private BaseIDevicePolicyManager mService;

        public Lifecycle(Context context) {
            super(context);
            String string = context.getResources().getString(R.string.activitychooserview_choose_application);
            string = TextUtils.isEmpty(string) ? DevicePolicyManagerService.class.getName() : string;
            try {
                this.mService = (BaseIDevicePolicyManager) Class.forName(string).getConstructor(Context.class).newInstance(context);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to instantiate DevicePolicyManagerService with class name: " + string, e);
            }
        }

        @Override
        public void onStart() {
            publishBinderService("device_policy", this.mService);
        }

        @Override
        public void onBootPhase(int i) {
            this.mService.systemReady(i);
        }

        @Override
        public void onStartUser(int i) {
            this.mService.handleStartUser(i);
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.handleUnlockUser(i);
        }

        @Override
        public void onStopUser(int i) {
            this.mService.handleStopUser(i);
        }
    }

    public static class DevicePolicyData {
        int mPermissionPolicy;
        ComponentName mRestrictionsProvider;
        int mUserHandle;
        int mUserProvisioningState;
        int mFailedPasswordAttempts = 0;
        boolean mPasswordValidAtLastCheckpoint = true;
        int mPasswordOwner = -1;
        long mLastMaximumTimeToLock = -1;
        boolean mUserSetupComplete = false;
        boolean mPaired = false;
        boolean mDeviceProvisioningConfigApplied = false;
        final ArrayMap<ComponentName, ActiveAdmin> mAdminMap = new ArrayMap<>();
        final ArrayList<ActiveAdmin> mAdminList = new ArrayList<>();
        final ArrayList<ComponentName> mRemovingAdmins = new ArrayList<>();
        final ArraySet<String> mAcceptedCaCertificates = new ArraySet<>();
        List<String> mLockTaskPackages = new ArrayList();
        int mLockTaskFeatures = 16;
        boolean mStatusBarDisabled = false;
        final ArrayMap<String, List<String>> mDelegationMap = new ArrayMap<>();
        boolean doNotAskCredentialsOnBoot = false;
        Set<String> mAffiliationIds = new ArraySet();
        long mLastSecurityLogRetrievalTime = -1;
        long mLastBugReportRequestTime = -1;
        long mLastNetworkLogsRetrievalTime = -1;
        boolean mCurrentInputMethodSet = false;
        Set<String> mOwnerInstalledCaCerts = new ArraySet();
        boolean mAdminBroadcastPending = false;
        PersistableBundle mInitBundle = null;
        long mPasswordTokenHandle = 0;

        public DevicePolicyData(int i) {
            this.mUserHandle = i;
        }
    }

    protected static class RestrictionsListener implements UserManagerInternal.UserRestrictionsListener {
        private Context mContext;

        public RestrictionsListener(Context context) {
            this.mContext = context;
        }

        public void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
            if (bundle.getBoolean("no_sharing_into_profile") != bundle2.getBoolean("no_sharing_into_profile")) {
                Intent intent = new Intent("android.app.action.DATA_SHARING_RESTRICTION_CHANGED");
                intent.setPackage(DevicePolicyManagerService.getManagedProvisioningPackage(this.mContext));
                intent.putExtra("android.intent.extra.USER_ID", i);
                intent.addFlags(268435456);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
            }
        }
    }

    static class ActiveAdmin {
        private static final String ATTR_LAST_NETWORK_LOGGING_NOTIFICATION = "last-notification";
        private static final String ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS = "num-notifications";
        private static final String ATTR_VALUE = "value";
        static final int DEF_KEYGUARD_FEATURES_DISABLED = 0;
        static final int DEF_MAXIMUM_FAILED_PASSWORDS_FOR_WIPE = 0;
        static final int DEF_MAXIMUM_NETWORK_LOGGING_NOTIFICATIONS_SHOWN = 2;
        static final long DEF_MAXIMUM_TIME_TO_UNLOCK = 0;
        static final int DEF_MINIMUM_PASSWORD_LENGTH = 0;
        static final int DEF_MINIMUM_PASSWORD_LETTERS = 1;
        static final int DEF_MINIMUM_PASSWORD_LOWER_CASE = 0;
        static final int DEF_MINIMUM_PASSWORD_NON_LETTER = 0;
        static final int DEF_MINIMUM_PASSWORD_NUMERIC = 1;
        static final int DEF_MINIMUM_PASSWORD_SYMBOLS = 1;
        static final int DEF_MINIMUM_PASSWORD_UPPER_CASE = 0;
        static final int DEF_ORGANIZATION_COLOR = Color.parseColor("#00796B");
        static final long DEF_PASSWORD_EXPIRATION_DATE = 0;
        static final long DEF_PASSWORD_EXPIRATION_TIMEOUT = 0;
        static final int DEF_PASSWORD_HISTORY_LENGTH = 0;
        private static final String TAG_ACCOUNT_TYPE = "account-type";
        private static final String TAG_CROSS_PROFILE_WIDGET_PROVIDERS = "cross-profile-widget-providers";
        private static final String TAG_DEFAULT_ENABLED_USER_RESTRICTIONS = "default-enabled-user-restrictions";
        private static final String TAG_DISABLE_ACCOUNT_MANAGEMENT = "disable-account-management";
        private static final String TAG_DISABLE_BLUETOOTH_CONTACT_SHARING = "disable-bt-contacts-sharing";
        private static final String TAG_DISABLE_CALLER_ID = "disable-caller-id";
        private static final String TAG_DISABLE_CAMERA = "disable-camera";
        private static final String TAG_DISABLE_CONTACTS_SEARCH = "disable-contacts-search";
        private static final String TAG_DISABLE_KEYGUARD_FEATURES = "disable-keyguard-features";
        private static final String TAG_DISABLE_SCREEN_CAPTURE = "disable-screen-capture";
        private static final String TAG_ENCRYPTION_REQUESTED = "encryption-requested";
        private static final String TAG_END_USER_SESSION_MESSAGE = "end_user_session_message";
        private static final String TAG_FORCE_EPHEMERAL_USERS = "force_ephemeral_users";
        private static final String TAG_GLOBAL_PROXY_EXCLUSION_LIST = "global-proxy-exclusion-list";
        private static final String TAG_GLOBAL_PROXY_SPEC = "global-proxy-spec";
        private static final String TAG_IS_LOGOUT_ENABLED = "is_logout_enabled";
        private static final String TAG_IS_NETWORK_LOGGING_ENABLED = "is_network_logging_enabled";
        private static final String TAG_KEEP_UNINSTALLED_PACKAGES = "keep-uninstalled-packages";
        private static final String TAG_LONG_SUPPORT_MESSAGE = "long-support-message";
        private static final String TAG_MANAGE_TRUST_AGENT_FEATURES = "manage-trust-agent-features";
        private static final String TAG_MANDATORY_BACKUP_TRANSPORT = "mandatory_backup_transport";
        private static final String TAG_MAX_FAILED_PASSWORD_WIPE = "max-failed-password-wipe";
        private static final String TAG_MAX_TIME_TO_UNLOCK = "max-time-to-unlock";
        private static final String TAG_METERED_DATA_DISABLED_PACKAGES = "metered_data_disabled_packages";
        private static final String TAG_MIN_PASSWORD_LENGTH = "min-password-length";
        private static final String TAG_MIN_PASSWORD_LETTERS = "min-password-letters";
        private static final String TAG_MIN_PASSWORD_LOWERCASE = "min-password-lowercase";
        private static final String TAG_MIN_PASSWORD_NONLETTER = "min-password-nonletter";
        private static final String TAG_MIN_PASSWORD_NUMERIC = "min-password-numeric";
        private static final String TAG_MIN_PASSWORD_SYMBOLS = "min-password-symbols";
        private static final String TAG_MIN_PASSWORD_UPPERCASE = "min-password-uppercase";
        private static final String TAG_ORGANIZATION_COLOR = "organization-color";
        private static final String TAG_ORGANIZATION_NAME = "organization-name";
        private static final String TAG_PACKAGE_LIST_ITEM = "item";
        private static final String TAG_PARENT_ADMIN = "parent-admin";
        private static final String TAG_PASSWORD_EXPIRATION_DATE = "password-expiration-date";
        private static final String TAG_PASSWORD_EXPIRATION_TIMEOUT = "password-expiration-timeout";
        private static final String TAG_PASSWORD_HISTORY_LENGTH = "password-history-length";
        private static final String TAG_PASSWORD_QUALITY = "password-quality";
        private static final String TAG_PERMITTED_ACCESSIBILITY_SERVICES = "permitted-accessiblity-services";
        private static final String TAG_PERMITTED_IMES = "permitted-imes";
        private static final String TAG_PERMITTED_NOTIFICATION_LISTENERS = "permitted-notification-listeners";
        private static final String TAG_POLICIES = "policies";
        private static final String TAG_PROVIDER = "provider";
        private static final String TAG_REQUIRE_AUTO_TIME = "require_auto_time";
        private static final String TAG_RESTRICTION = "restriction";
        private static final String TAG_SHORT_SUPPORT_MESSAGE = "short-support-message";
        private static final String TAG_SPECIFIES_GLOBAL_PROXY = "specifies-global-proxy";
        private static final String TAG_START_USER_SESSION_MESSAGE = "start_user_session_message";
        private static final String TAG_STRONG_AUTH_UNLOCK_TIMEOUT = "strong-auth-unlock-timeout";
        private static final String TAG_TEST_ONLY_ADMIN = "test-only-admin";
        private static final String TAG_TRUST_AGENT_COMPONENT = "component";
        private static final String TAG_TRUST_AGENT_COMPONENT_OPTIONS = "trust-agent-component-options";
        private static final String TAG_USER_RESTRICTIONS = "user-restrictions";
        List<String> crossProfileWidgetProviders;
        DeviceAdminInfo info;
        final boolean isParent;
        List<String> keepUninstalledPackages;
        List<String> meteredDisabledPackages;
        ActiveAdmin parentAdmin;
        List<String> permittedAccessiblityServices;
        List<String> permittedInputMethods;
        List<String> permittedNotificationListeners;
        Bundle userRestrictions;
        int passwordHistoryLength = 0;
        PasswordMetrics minimumPasswordMetrics = new PasswordMetrics(0, 0, 1, 0, 0, 1, 1, 0);
        long maximumTimeToUnlock = 0;
        long strongAuthUnlockTimeout = 0;
        int maximumFailedPasswordsForWipe = 0;
        long passwordExpirationTimeout = 0;
        long passwordExpirationDate = 0;
        int disabledKeyguardFeatures = 0;
        boolean encryptionRequested = false;
        boolean testOnlyAdmin = false;
        boolean disableCamera = false;
        boolean disableCallerId = false;
        boolean disableContactsSearch = false;
        boolean disableBluetoothContactSharing = true;
        boolean disableScreenCapture = false;
        boolean requireAutoTime = false;
        boolean forceEphemeralUsers = false;
        boolean isNetworkLoggingEnabled = false;
        boolean isLogoutEnabled = false;
        int numNetworkLoggingNotifications = 0;
        long lastNetworkLoggingNotificationTimeMs = 0;
        final Set<String> accountTypesWithManagementDisabled = new ArraySet();
        boolean specifiesGlobalProxy = false;
        String globalProxySpec = null;
        String globalProxyExclusionList = null;
        ArrayMap<String, TrustAgentInfo> trustAgentInfos = new ArrayMap<>();
        final Set<String> defaultEnabledRestrictionsAlreadySet = new ArraySet();
        CharSequence shortSupportMessage = null;
        CharSequence longSupportMessage = null;
        int organizationColor = DEF_ORGANIZATION_COLOR;
        String organizationName = null;
        ComponentName mandatoryBackupTransport = null;
        String startUserSessionMessage = null;
        String endUserSessionMessage = null;

        static class TrustAgentInfo {
            public PersistableBundle options;

            TrustAgentInfo(PersistableBundle persistableBundle) {
                this.options = persistableBundle;
            }
        }

        ActiveAdmin(DeviceAdminInfo deviceAdminInfo, boolean z) {
            this.info = deviceAdminInfo;
            this.isParent = z;
        }

        ActiveAdmin getParentActiveAdmin() {
            Preconditions.checkState(!this.isParent);
            if (this.parentAdmin == null) {
                this.parentAdmin = new ActiveAdmin(this.info, true);
            }
            return this.parentAdmin;
        }

        boolean hasParentActiveAdmin() {
            return this.parentAdmin != null;
        }

        int getUid() {
            return this.info.getActivityInfo().applicationInfo.uid;
        }

        public UserHandle getUserHandle() {
            return UserHandle.of(UserHandle.getUserId(this.info.getActivityInfo().applicationInfo.uid));
        }

        void writeToXml(XmlSerializer xmlSerializer) throws IllegalStateException, IOException, IllegalArgumentException {
            xmlSerializer.startTag(null, TAG_POLICIES);
            this.info.writePoliciesToXml(xmlSerializer);
            xmlSerializer.endTag(null, TAG_POLICIES);
            if (this.minimumPasswordMetrics.quality != 0) {
                xmlSerializer.startTag(null, TAG_PASSWORD_QUALITY);
                xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.quality));
                xmlSerializer.endTag(null, TAG_PASSWORD_QUALITY);
                if (this.minimumPasswordMetrics.length != 0) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_LENGTH);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.length));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_LENGTH);
                }
                if (this.passwordHistoryLength != 0) {
                    xmlSerializer.startTag(null, TAG_PASSWORD_HISTORY_LENGTH);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.passwordHistoryLength));
                    xmlSerializer.endTag(null, TAG_PASSWORD_HISTORY_LENGTH);
                }
                if (this.minimumPasswordMetrics.upperCase != 0) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_UPPERCASE);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.upperCase));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_UPPERCASE);
                }
                if (this.minimumPasswordMetrics.lowerCase != 0) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_LOWERCASE);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.lowerCase));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_LOWERCASE);
                }
                if (this.minimumPasswordMetrics.letters != 1) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_LETTERS);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.letters));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_LETTERS);
                }
                if (this.minimumPasswordMetrics.numeric != 1) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_NUMERIC);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.numeric));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_NUMERIC);
                }
                if (this.minimumPasswordMetrics.symbols != 1) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_SYMBOLS);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.symbols));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_SYMBOLS);
                }
                if (this.minimumPasswordMetrics.nonLetter > 0) {
                    xmlSerializer.startTag(null, TAG_MIN_PASSWORD_NONLETTER);
                    xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordMetrics.nonLetter));
                    xmlSerializer.endTag(null, TAG_MIN_PASSWORD_NONLETTER);
                }
            }
            if (this.maximumTimeToUnlock != 0) {
                xmlSerializer.startTag(null, TAG_MAX_TIME_TO_UNLOCK);
                xmlSerializer.attribute(null, ATTR_VALUE, Long.toString(this.maximumTimeToUnlock));
                xmlSerializer.endTag(null, TAG_MAX_TIME_TO_UNLOCK);
            }
            if (this.strongAuthUnlockTimeout != 259200000) {
                xmlSerializer.startTag(null, TAG_STRONG_AUTH_UNLOCK_TIMEOUT);
                xmlSerializer.attribute(null, ATTR_VALUE, Long.toString(this.strongAuthUnlockTimeout));
                xmlSerializer.endTag(null, TAG_STRONG_AUTH_UNLOCK_TIMEOUT);
            }
            if (this.maximumFailedPasswordsForWipe != 0) {
                xmlSerializer.startTag(null, TAG_MAX_FAILED_PASSWORD_WIPE);
                xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.maximumFailedPasswordsForWipe));
                xmlSerializer.endTag(null, TAG_MAX_FAILED_PASSWORD_WIPE);
            }
            if (this.specifiesGlobalProxy) {
                xmlSerializer.startTag(null, TAG_SPECIFIES_GLOBAL_PROXY);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.specifiesGlobalProxy));
                xmlSerializer.endTag(null, TAG_SPECIFIES_GLOBAL_PROXY);
                if (this.globalProxySpec != null) {
                    xmlSerializer.startTag(null, TAG_GLOBAL_PROXY_SPEC);
                    xmlSerializer.attribute(null, ATTR_VALUE, this.globalProxySpec);
                    xmlSerializer.endTag(null, TAG_GLOBAL_PROXY_SPEC);
                }
                if (this.globalProxyExclusionList != null) {
                    xmlSerializer.startTag(null, TAG_GLOBAL_PROXY_EXCLUSION_LIST);
                    xmlSerializer.attribute(null, ATTR_VALUE, this.globalProxyExclusionList);
                    xmlSerializer.endTag(null, TAG_GLOBAL_PROXY_EXCLUSION_LIST);
                }
            }
            if (this.passwordExpirationTimeout != 0) {
                xmlSerializer.startTag(null, TAG_PASSWORD_EXPIRATION_TIMEOUT);
                xmlSerializer.attribute(null, ATTR_VALUE, Long.toString(this.passwordExpirationTimeout));
                xmlSerializer.endTag(null, TAG_PASSWORD_EXPIRATION_TIMEOUT);
            }
            if (this.passwordExpirationDate != 0) {
                xmlSerializer.startTag(null, TAG_PASSWORD_EXPIRATION_DATE);
                xmlSerializer.attribute(null, ATTR_VALUE, Long.toString(this.passwordExpirationDate));
                xmlSerializer.endTag(null, TAG_PASSWORD_EXPIRATION_DATE);
            }
            if (this.encryptionRequested) {
                xmlSerializer.startTag(null, TAG_ENCRYPTION_REQUESTED);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.encryptionRequested));
                xmlSerializer.endTag(null, TAG_ENCRYPTION_REQUESTED);
            }
            if (this.testOnlyAdmin) {
                xmlSerializer.startTag(null, TAG_TEST_ONLY_ADMIN);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.testOnlyAdmin));
                xmlSerializer.endTag(null, TAG_TEST_ONLY_ADMIN);
            }
            if (this.disableCamera) {
                xmlSerializer.startTag(null, TAG_DISABLE_CAMERA);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.disableCamera));
                xmlSerializer.endTag(null, TAG_DISABLE_CAMERA);
            }
            if (this.disableCallerId) {
                xmlSerializer.startTag(null, TAG_DISABLE_CALLER_ID);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.disableCallerId));
                xmlSerializer.endTag(null, TAG_DISABLE_CALLER_ID);
            }
            if (this.disableContactsSearch) {
                xmlSerializer.startTag(null, TAG_DISABLE_CONTACTS_SEARCH);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.disableContactsSearch));
                xmlSerializer.endTag(null, TAG_DISABLE_CONTACTS_SEARCH);
            }
            if (!this.disableBluetoothContactSharing) {
                xmlSerializer.startTag(null, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.disableBluetoothContactSharing));
                xmlSerializer.endTag(null, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING);
            }
            if (this.disableScreenCapture) {
                xmlSerializer.startTag(null, TAG_DISABLE_SCREEN_CAPTURE);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.disableScreenCapture));
                xmlSerializer.endTag(null, TAG_DISABLE_SCREEN_CAPTURE);
            }
            if (this.requireAutoTime) {
                xmlSerializer.startTag(null, TAG_REQUIRE_AUTO_TIME);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.requireAutoTime));
                xmlSerializer.endTag(null, TAG_REQUIRE_AUTO_TIME);
            }
            if (this.forceEphemeralUsers) {
                xmlSerializer.startTag(null, TAG_FORCE_EPHEMERAL_USERS);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.forceEphemeralUsers));
                xmlSerializer.endTag(null, TAG_FORCE_EPHEMERAL_USERS);
            }
            if (this.isNetworkLoggingEnabled) {
                xmlSerializer.startTag(null, TAG_IS_NETWORK_LOGGING_ENABLED);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.isNetworkLoggingEnabled));
                xmlSerializer.attribute(null, ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS, Integer.toString(this.numNetworkLoggingNotifications));
                xmlSerializer.attribute(null, ATTR_LAST_NETWORK_LOGGING_NOTIFICATION, Long.toString(this.lastNetworkLoggingNotificationTimeMs));
                xmlSerializer.endTag(null, TAG_IS_NETWORK_LOGGING_ENABLED);
            }
            if (this.disabledKeyguardFeatures != 0) {
                xmlSerializer.startTag(null, TAG_DISABLE_KEYGUARD_FEATURES);
                xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.disabledKeyguardFeatures));
                xmlSerializer.endTag(null, TAG_DISABLE_KEYGUARD_FEATURES);
            }
            if (!this.accountTypesWithManagementDisabled.isEmpty()) {
                xmlSerializer.startTag(null, TAG_DISABLE_ACCOUNT_MANAGEMENT);
                writeAttributeValuesToXml(xmlSerializer, TAG_ACCOUNT_TYPE, this.accountTypesWithManagementDisabled);
                xmlSerializer.endTag(null, TAG_DISABLE_ACCOUNT_MANAGEMENT);
            }
            if (!this.trustAgentInfos.isEmpty()) {
                Set<Map.Entry<String, TrustAgentInfo>> setEntrySet = this.trustAgentInfos.entrySet();
                xmlSerializer.startTag(null, TAG_MANAGE_TRUST_AGENT_FEATURES);
                for (Map.Entry<String, TrustAgentInfo> entry : setEntrySet) {
                    TrustAgentInfo value = entry.getValue();
                    xmlSerializer.startTag(null, TAG_TRUST_AGENT_COMPONENT);
                    xmlSerializer.attribute(null, ATTR_VALUE, entry.getKey());
                    if (value.options != null) {
                        xmlSerializer.startTag(null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                        try {
                            value.options.saveToXml(xmlSerializer);
                        } catch (XmlPullParserException e) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "Failed to save TrustAgent options", e);
                        }
                        xmlSerializer.endTag(null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                    }
                    xmlSerializer.endTag(null, TAG_TRUST_AGENT_COMPONENT);
                }
                xmlSerializer.endTag(null, TAG_MANAGE_TRUST_AGENT_FEATURES);
            }
            if (this.crossProfileWidgetProviders != null && !this.crossProfileWidgetProviders.isEmpty()) {
                xmlSerializer.startTag(null, TAG_CROSS_PROFILE_WIDGET_PROVIDERS);
                writeAttributeValuesToXml(xmlSerializer, TAG_PROVIDER, this.crossProfileWidgetProviders);
                xmlSerializer.endTag(null, TAG_CROSS_PROFILE_WIDGET_PROVIDERS);
            }
            writePackageListToXml(xmlSerializer, TAG_PERMITTED_ACCESSIBILITY_SERVICES, this.permittedAccessiblityServices);
            writePackageListToXml(xmlSerializer, TAG_PERMITTED_IMES, this.permittedInputMethods);
            writePackageListToXml(xmlSerializer, TAG_PERMITTED_NOTIFICATION_LISTENERS, this.permittedNotificationListeners);
            writePackageListToXml(xmlSerializer, TAG_KEEP_UNINSTALLED_PACKAGES, this.keepUninstalledPackages);
            writePackageListToXml(xmlSerializer, TAG_METERED_DATA_DISABLED_PACKAGES, this.meteredDisabledPackages);
            if (hasUserRestrictions()) {
                UserRestrictionsUtils.writeRestrictions(xmlSerializer, this.userRestrictions, TAG_USER_RESTRICTIONS);
            }
            if (!this.defaultEnabledRestrictionsAlreadySet.isEmpty()) {
                xmlSerializer.startTag(null, TAG_DEFAULT_ENABLED_USER_RESTRICTIONS);
                writeAttributeValuesToXml(xmlSerializer, TAG_RESTRICTION, this.defaultEnabledRestrictionsAlreadySet);
                xmlSerializer.endTag(null, TAG_DEFAULT_ENABLED_USER_RESTRICTIONS);
            }
            if (!TextUtils.isEmpty(this.shortSupportMessage)) {
                xmlSerializer.startTag(null, TAG_SHORT_SUPPORT_MESSAGE);
                xmlSerializer.text(this.shortSupportMessage.toString());
                xmlSerializer.endTag(null, TAG_SHORT_SUPPORT_MESSAGE);
            }
            if (!TextUtils.isEmpty(this.longSupportMessage)) {
                xmlSerializer.startTag(null, TAG_LONG_SUPPORT_MESSAGE);
                xmlSerializer.text(this.longSupportMessage.toString());
                xmlSerializer.endTag(null, TAG_LONG_SUPPORT_MESSAGE);
            }
            if (this.parentAdmin != null) {
                xmlSerializer.startTag(null, TAG_PARENT_ADMIN);
                this.parentAdmin.writeToXml(xmlSerializer);
                xmlSerializer.endTag(null, TAG_PARENT_ADMIN);
            }
            if (this.organizationColor != DEF_ORGANIZATION_COLOR) {
                xmlSerializer.startTag(null, TAG_ORGANIZATION_COLOR);
                xmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(this.organizationColor));
                xmlSerializer.endTag(null, TAG_ORGANIZATION_COLOR);
            }
            if (this.organizationName != null) {
                xmlSerializer.startTag(null, TAG_ORGANIZATION_NAME);
                xmlSerializer.text(this.organizationName);
                xmlSerializer.endTag(null, TAG_ORGANIZATION_NAME);
            }
            if (this.isLogoutEnabled) {
                xmlSerializer.startTag(null, TAG_IS_LOGOUT_ENABLED);
                xmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(this.isLogoutEnabled));
                xmlSerializer.endTag(null, TAG_IS_LOGOUT_ENABLED);
            }
            if (this.mandatoryBackupTransport != null) {
                xmlSerializer.startTag(null, TAG_MANDATORY_BACKUP_TRANSPORT);
                xmlSerializer.attribute(null, ATTR_VALUE, this.mandatoryBackupTransport.flattenToString());
                xmlSerializer.endTag(null, TAG_MANDATORY_BACKUP_TRANSPORT);
            }
            if (this.startUserSessionMessage != null) {
                xmlSerializer.startTag(null, TAG_START_USER_SESSION_MESSAGE);
                xmlSerializer.text(this.startUserSessionMessage);
                xmlSerializer.endTag(null, TAG_START_USER_SESSION_MESSAGE);
            }
            if (this.endUserSessionMessage != null) {
                xmlSerializer.startTag(null, TAG_END_USER_SESSION_MESSAGE);
                xmlSerializer.text(this.endUserSessionMessage);
                xmlSerializer.endTag(null, TAG_END_USER_SESSION_MESSAGE);
            }
        }

        void writePackageListToXml(XmlSerializer xmlSerializer, String str, List<String> list) throws IllegalStateException, IOException, IllegalArgumentException {
            if (list == null) {
                return;
            }
            xmlSerializer.startTag(null, str);
            writeAttributeValuesToXml(xmlSerializer, "item", list);
            xmlSerializer.endTag(null, str);
        }

        void writeAttributeValuesToXml(XmlSerializer xmlSerializer, String str, Collection<String> collection) throws IOException {
            for (String str2 : collection) {
                xmlSerializer.startTag(null, str);
                xmlSerializer.attribute(null, ATTR_VALUE, str2);
                xmlSerializer.endTag(null, str);
            }
        }

        void readFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        if (next != 3 && next != 4) {
                            String name = xmlPullParser.getName();
                            if (TAG_POLICIES.equals(name)) {
                                this.info.readPoliciesFromXml(xmlPullParser);
                            } else if (TAG_PASSWORD_QUALITY.equals(name)) {
                                this.minimumPasswordMetrics.quality = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_LENGTH.equals(name)) {
                                this.minimumPasswordMetrics.length = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_PASSWORD_HISTORY_LENGTH.equals(name)) {
                                this.passwordHistoryLength = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_UPPERCASE.equals(name)) {
                                this.minimumPasswordMetrics.upperCase = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_LOWERCASE.equals(name)) {
                                this.minimumPasswordMetrics.lowerCase = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_LETTERS.equals(name)) {
                                this.minimumPasswordMetrics.letters = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_NUMERIC.equals(name)) {
                                this.minimumPasswordMetrics.numeric = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_SYMBOLS.equals(name)) {
                                this.minimumPasswordMetrics.symbols = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MIN_PASSWORD_NONLETTER.equals(name)) {
                                this.minimumPasswordMetrics.nonLetter = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MAX_TIME_TO_UNLOCK.equals(name)) {
                                this.maximumTimeToUnlock = Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_STRONG_AUTH_UNLOCK_TIMEOUT.equals(name)) {
                                this.strongAuthUnlockTimeout = Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MAX_FAILED_PASSWORD_WIPE.equals(name)) {
                                this.maximumFailedPasswordsForWipe = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_SPECIFIES_GLOBAL_PROXY.equals(name)) {
                                this.specifiesGlobalProxy = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_GLOBAL_PROXY_SPEC.equals(name)) {
                                this.globalProxySpec = xmlPullParser.getAttributeValue(null, ATTR_VALUE);
                            } else if (TAG_GLOBAL_PROXY_EXCLUSION_LIST.equals(name)) {
                                this.globalProxyExclusionList = xmlPullParser.getAttributeValue(null, ATTR_VALUE);
                            } else if (TAG_PASSWORD_EXPIRATION_TIMEOUT.equals(name)) {
                                this.passwordExpirationTimeout = Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_PASSWORD_EXPIRATION_DATE.equals(name)) {
                                this.passwordExpirationDate = Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_ENCRYPTION_REQUESTED.equals(name)) {
                                this.encryptionRequested = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_TEST_ONLY_ADMIN.equals(name)) {
                                this.testOnlyAdmin = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_DISABLE_CAMERA.equals(name)) {
                                this.disableCamera = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_DISABLE_CALLER_ID.equals(name)) {
                                this.disableCallerId = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_DISABLE_CONTACTS_SEARCH.equals(name)) {
                                this.disableContactsSearch = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_DISABLE_BLUETOOTH_CONTACT_SHARING.equals(name)) {
                                this.disableBluetoothContactSharing = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_DISABLE_SCREEN_CAPTURE.equals(name)) {
                                this.disableScreenCapture = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_REQUIRE_AUTO_TIME.equals(name)) {
                                this.requireAutoTime = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_FORCE_EPHEMERAL_USERS.equals(name)) {
                                this.forceEphemeralUsers = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_IS_NETWORK_LOGGING_ENABLED.equals(name)) {
                                this.isNetworkLoggingEnabled = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                                this.lastNetworkLoggingNotificationTimeMs = Long.parseLong(xmlPullParser.getAttributeValue(null, ATTR_LAST_NETWORK_LOGGING_NOTIFICATION));
                                this.numNetworkLoggingNotifications = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS));
                            } else if (TAG_DISABLE_KEYGUARD_FEATURES.equals(name)) {
                                this.disabledKeyguardFeatures = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_DISABLE_ACCOUNT_MANAGEMENT.equals(name)) {
                                readAttributeValues(xmlPullParser, TAG_ACCOUNT_TYPE, this.accountTypesWithManagementDisabled);
                            } else if (TAG_MANAGE_TRUST_AGENT_FEATURES.equals(name)) {
                                this.trustAgentInfos = getAllTrustAgentInfos(xmlPullParser, name);
                            } else if (TAG_CROSS_PROFILE_WIDGET_PROVIDERS.equals(name)) {
                                this.crossProfileWidgetProviders = new ArrayList();
                                readAttributeValues(xmlPullParser, TAG_PROVIDER, this.crossProfileWidgetProviders);
                            } else if (TAG_PERMITTED_ACCESSIBILITY_SERVICES.equals(name)) {
                                this.permittedAccessiblityServices = readPackageList(xmlPullParser, name);
                            } else if (TAG_PERMITTED_IMES.equals(name)) {
                                this.permittedInputMethods = readPackageList(xmlPullParser, name);
                            } else if (TAG_PERMITTED_NOTIFICATION_LISTENERS.equals(name)) {
                                this.permittedNotificationListeners = readPackageList(xmlPullParser, name);
                            } else if (TAG_KEEP_UNINSTALLED_PACKAGES.equals(name)) {
                                this.keepUninstalledPackages = readPackageList(xmlPullParser, name);
                            } else if (TAG_METERED_DATA_DISABLED_PACKAGES.equals(name)) {
                                this.meteredDisabledPackages = readPackageList(xmlPullParser, name);
                            } else if (TAG_USER_RESTRICTIONS.equals(name)) {
                                this.userRestrictions = UserRestrictionsUtils.readRestrictions(xmlPullParser);
                            } else if (TAG_DEFAULT_ENABLED_USER_RESTRICTIONS.equals(name)) {
                                readAttributeValues(xmlPullParser, TAG_RESTRICTION, this.defaultEnabledRestrictionsAlreadySet);
                            } else if (TAG_SHORT_SUPPORT_MESSAGE.equals(name)) {
                                if (xmlPullParser.next() == 4) {
                                    this.shortSupportMessage = xmlPullParser.getText();
                                } else {
                                    Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading short support message");
                                }
                            } else if (TAG_LONG_SUPPORT_MESSAGE.equals(name)) {
                                if (xmlPullParser.next() == 4) {
                                    this.longSupportMessage = xmlPullParser.getText();
                                } else {
                                    Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading long support message");
                                }
                            } else if (TAG_PARENT_ADMIN.equals(name)) {
                                Preconditions.checkState(!this.isParent);
                                this.parentAdmin = new ActiveAdmin(this.info, true);
                                this.parentAdmin.readFromXml(xmlPullParser);
                            } else if (TAG_ORGANIZATION_COLOR.equals(name)) {
                                this.organizationColor = Integer.parseInt(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_ORGANIZATION_NAME.equals(name)) {
                                if (xmlPullParser.next() == 4) {
                                    this.organizationName = xmlPullParser.getText();
                                } else {
                                    Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading organization name");
                                }
                            } else if (TAG_IS_LOGOUT_ENABLED.equals(name)) {
                                this.isLogoutEnabled = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_MANDATORY_BACKUP_TRANSPORT.equals(name)) {
                                this.mandatoryBackupTransport = ComponentName.unflattenFromString(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_START_USER_SESSION_MESSAGE.equals(name)) {
                                if (xmlPullParser.next() == 4) {
                                    this.startUserSessionMessage = xmlPullParser.getText();
                                } else {
                                    Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading start session message");
                                }
                            } else if (TAG_END_USER_SESSION_MESSAGE.equals(name)) {
                                if (xmlPullParser.next() == 4) {
                                    this.endUserSessionMessage = xmlPullParser.getText();
                                } else {
                                    Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading end session message");
                                }
                            } else {
                                Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown admin tag: " + name);
                                XmlUtils.skipCurrentTag(xmlPullParser);
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private List<String> readPackageList(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
            ArrayList arrayList = new ArrayList();
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                    break;
                }
                if (next != 3 && next != 4) {
                    String name = xmlPullParser.getName();
                    if ("item".equals(name)) {
                        String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_VALUE);
                        if (attributeValue != null) {
                            arrayList.add(attributeValue);
                        } else {
                            Slog.w(DevicePolicyManagerService.LOG_TAG, "Package name missing under " + name);
                        }
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + str + ": " + name);
                    }
                }
            }
            return arrayList;
        }

        private void readAttributeValues(XmlPullParser xmlPullParser, String str, Collection<String> collection) throws XmlPullParserException, IOException {
            collection.clear();
            int depth = xmlPullParser.getDepth();
            while (true) {
                int next = xmlPullParser.next();
                if (next != 1) {
                    if (next != 3 || xmlPullParser.getDepth() > depth) {
                        if (next != 3 && next != 4) {
                            String name = xmlPullParser.getName();
                            if (str.equals(name)) {
                                collection.add(xmlPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else {
                                Slog.e(DevicePolicyManagerService.LOG_TAG, "Expected tag " + str + " but found " + name);
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        private ArrayMap<String, TrustAgentInfo> getAllTrustAgentInfos(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            ArrayMap<String, TrustAgentInfo> arrayMap = new ArrayMap<>();
            while (true) {
                int next = xmlPullParser.next();
                if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                    break;
                }
                if (next != 3 && next != 4) {
                    String name = xmlPullParser.getName();
                    if (TAG_TRUST_AGENT_COMPONENT.equals(name)) {
                        arrayMap.put(xmlPullParser.getAttributeValue(null, ATTR_VALUE), getTrustAgentInfo(xmlPullParser, str));
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + str + ": " + name);
                    }
                }
            }
            return arrayMap;
        }

        private TrustAgentInfo getTrustAgentInfo(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
            int depth = xmlPullParser.getDepth();
            TrustAgentInfo trustAgentInfo = new TrustAgentInfo(null);
            while (true) {
                int next = xmlPullParser.next();
                if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                    break;
                }
                if (next != 3 && next != 4) {
                    String name = xmlPullParser.getName();
                    if (TAG_TRUST_AGENT_COMPONENT_OPTIONS.equals(name)) {
                        trustAgentInfo.options = PersistableBundle.restoreFromXml(xmlPullParser);
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + str + ": " + name);
                    }
                }
            }
            return trustAgentInfo;
        }

        boolean hasUserRestrictions() {
            return this.userRestrictions != null && this.userRestrictions.size() > 0;
        }

        Bundle ensureUserRestrictions() {
            if (this.userRestrictions == null) {
                this.userRestrictions = new Bundle();
            }
            return this.userRestrictions;
        }

        public void transfer(DeviceAdminInfo deviceAdminInfo) {
            if (hasParentActiveAdmin()) {
                this.parentAdmin.info = deviceAdminInfo;
            }
            this.info = deviceAdminInfo;
        }

        void dump(String str, PrintWriter printWriter) {
            printWriter.print(str);
            printWriter.print("uid=");
            printWriter.println(getUid());
            printWriter.print(str);
            printWriter.print("testOnlyAdmin=");
            printWriter.println(this.testOnlyAdmin);
            printWriter.print(str);
            printWriter.println("policies:");
            ArrayList usedPolicies = this.info.getUsedPolicies();
            if (usedPolicies != null) {
                for (int i = 0; i < usedPolicies.size(); i++) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.println(((DeviceAdminInfo.PolicyInfo) usedPolicies.get(i)).tag);
                }
            }
            printWriter.print(str);
            printWriter.print("passwordQuality=0x");
            printWriter.println(Integer.toHexString(this.minimumPasswordMetrics.quality));
            printWriter.print(str);
            printWriter.print("minimumPasswordLength=");
            printWriter.println(this.minimumPasswordMetrics.length);
            printWriter.print(str);
            printWriter.print("passwordHistoryLength=");
            printWriter.println(this.passwordHistoryLength);
            printWriter.print(str);
            printWriter.print("minimumPasswordUpperCase=");
            printWriter.println(this.minimumPasswordMetrics.upperCase);
            printWriter.print(str);
            printWriter.print("minimumPasswordLowerCase=");
            printWriter.println(this.minimumPasswordMetrics.lowerCase);
            printWriter.print(str);
            printWriter.print("minimumPasswordLetters=");
            printWriter.println(this.minimumPasswordMetrics.letters);
            printWriter.print(str);
            printWriter.print("minimumPasswordNumeric=");
            printWriter.println(this.minimumPasswordMetrics.numeric);
            printWriter.print(str);
            printWriter.print("minimumPasswordSymbols=");
            printWriter.println(this.minimumPasswordMetrics.symbols);
            printWriter.print(str);
            printWriter.print("minimumPasswordNonLetter=");
            printWriter.println(this.minimumPasswordMetrics.nonLetter);
            printWriter.print(str);
            printWriter.print("maximumTimeToUnlock=");
            printWriter.println(this.maximumTimeToUnlock);
            printWriter.print(str);
            printWriter.print("strongAuthUnlockTimeout=");
            printWriter.println(this.strongAuthUnlockTimeout);
            printWriter.print(str);
            printWriter.print("maximumFailedPasswordsForWipe=");
            printWriter.println(this.maximumFailedPasswordsForWipe);
            printWriter.print(str);
            printWriter.print("specifiesGlobalProxy=");
            printWriter.println(this.specifiesGlobalProxy);
            printWriter.print(str);
            printWriter.print("passwordExpirationTimeout=");
            printWriter.println(this.passwordExpirationTimeout);
            printWriter.print(str);
            printWriter.print("passwordExpirationDate=");
            printWriter.println(this.passwordExpirationDate);
            if (this.globalProxySpec != null) {
                printWriter.print(str);
                printWriter.print("globalProxySpec=");
                printWriter.println(this.globalProxySpec);
            }
            if (this.globalProxyExclusionList != null) {
                printWriter.print(str);
                printWriter.print("globalProxyEclusionList=");
                printWriter.println(this.globalProxyExclusionList);
            }
            printWriter.print(str);
            printWriter.print("encryptionRequested=");
            printWriter.println(this.encryptionRequested);
            printWriter.print(str);
            printWriter.print("disableCamera=");
            printWriter.println(this.disableCamera);
            printWriter.print(str);
            printWriter.print("disableCallerId=");
            printWriter.println(this.disableCallerId);
            printWriter.print(str);
            printWriter.print("disableContactsSearch=");
            printWriter.println(this.disableContactsSearch);
            printWriter.print(str);
            printWriter.print("disableBluetoothContactSharing=");
            printWriter.println(this.disableBluetoothContactSharing);
            printWriter.print(str);
            printWriter.print("disableScreenCapture=");
            printWriter.println(this.disableScreenCapture);
            printWriter.print(str);
            printWriter.print("requireAutoTime=");
            printWriter.println(this.requireAutoTime);
            printWriter.print(str);
            printWriter.print("forceEphemeralUsers=");
            printWriter.println(this.forceEphemeralUsers);
            printWriter.print(str);
            printWriter.print("isNetworkLoggingEnabled=");
            printWriter.println(this.isNetworkLoggingEnabled);
            printWriter.print(str);
            printWriter.print("disabledKeyguardFeatures=");
            printWriter.println(this.disabledKeyguardFeatures);
            printWriter.print(str);
            printWriter.print("crossProfileWidgetProviders=");
            printWriter.println(this.crossProfileWidgetProviders);
            if (this.permittedAccessiblityServices != null) {
                printWriter.print(str);
                printWriter.print("permittedAccessibilityServices=");
                printWriter.println(this.permittedAccessiblityServices);
            }
            if (this.permittedInputMethods != null) {
                printWriter.print(str);
                printWriter.print("permittedInputMethods=");
                printWriter.println(this.permittedInputMethods);
            }
            if (this.permittedNotificationListeners != null) {
                printWriter.print(str);
                printWriter.print("permittedNotificationListeners=");
                printWriter.println(this.permittedNotificationListeners);
            }
            if (this.keepUninstalledPackages != null) {
                printWriter.print(str);
                printWriter.print("keepUninstalledPackages=");
                printWriter.println(this.keepUninstalledPackages);
            }
            printWriter.print(str);
            printWriter.print("organizationColor=");
            printWriter.println(this.organizationColor);
            if (this.organizationName != null) {
                printWriter.print(str);
                printWriter.print("organizationName=");
                printWriter.println(this.organizationName);
            }
            printWriter.print(str);
            printWriter.println("userRestrictions:");
            UserRestrictionsUtils.dumpRestrictions(printWriter, str + "  ", this.userRestrictions);
            printWriter.print(str);
            printWriter.print("defaultEnabledRestrictionsAlreadySet=");
            printWriter.println(this.defaultEnabledRestrictionsAlreadySet);
            printWriter.print(str);
            printWriter.print("isParent=");
            printWriter.println(this.isParent);
            if (this.parentAdmin != null) {
                printWriter.print(str);
                printWriter.println("parentAdmin:");
                this.parentAdmin.dump(str + "  ", printWriter);
            }
        }
    }

    private void handlePackagesChanged(String str, int i) {
        boolean z;
        DevicePolicyData userData = getUserData(i);
        synchronized (getLockObject()) {
            boolean z2 = false;
            z = false;
            for (int size = userData.mAdminList.size() - 1; size >= 0; size--) {
                ActiveAdmin activeAdmin = userData.mAdminList.get(size);
                try {
                    String packageName = activeAdmin.info.getPackageName();
                    if ((str == null || str.equals(packageName)) && (this.mIPackageManager.getPackageInfo(packageName, 0, i) == null || this.mIPackageManager.getReceiverInfo(activeAdmin.info.getComponent(), 786432, i) == null)) {
                        try {
                            userData.mAdminList.remove(size);
                            userData.mAdminMap.remove(activeAdmin.info.getComponent());
                            pushActiveAdminPackagesLocked(i);
                            pushMeteredDisabledPackagesLocked(i);
                            z = true;
                        } catch (RemoteException e) {
                            z = true;
                        }
                    }
                } catch (RemoteException e2) {
                }
            }
            if (z) {
                validatePasswordOwnerLocked(userData);
            }
            for (int size2 = userData.mDelegationMap.size() - 1; size2 >= 0; size2--) {
                if (isRemovedPackage(str, userData.mDelegationMap.keyAt(size2), i)) {
                    userData.mDelegationMap.removeAt(size2);
                    z2 = true;
                }
            }
            ComponentName ownerComponent = getOwnerComponent(i);
            if (str != null && ownerComponent != null && ownerComponent.getPackageName().equals(str)) {
                startOwnerService(i, "package-broadcast");
            }
            if (z || z2) {
                saveSettingsLocked(userData.mUserHandle);
            }
        }
        if (z) {
            pushUserRestrictions(i);
        }
    }

    private boolean isRemovedPackage(String str, String str2, int i) {
        if (str2 == null) {
            return false;
        }
        if (str != null) {
            try {
                if (!str.equals(str2)) {
                    return false;
                }
            } catch (RemoteException e) {
                return false;
            }
        }
        return this.mIPackageManager.getPackageInfo(str2, 0, i) == null;
    }

    @VisibleForTesting
    static class Injector {
        public final Context mContext;

        Injector(Context context) {
            this.mContext = context;
        }

        public boolean hasFeature() {
            return getPackageManager().hasSystemFeature("android.software.device_admin");
        }

        Context createContextAsUser(UserHandle userHandle) throws PackageManager.NameNotFoundException {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        }

        Resources getResources() {
            return this.mContext.getResources();
        }

        Owners newOwners() {
            return new Owners(getUserManager(), getUserManagerInternal(), getPackageManagerInternal());
        }

        UserManager getUserManager() {
            return UserManager.get(this.mContext);
        }

        UserManagerInternal getUserManagerInternal() {
            return (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }

        PackageManagerInternal getPackageManagerInternal() {
            return (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }

        UsageStatsManagerInternal getUsageStatsManagerInternal() {
            return (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        }

        NetworkPolicyManagerInternal getNetworkPolicyManagerInternal() {
            return (NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class);
        }

        NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        }

        IIpConnectivityMetrics getIIpConnectivityMetrics() {
            return IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
        }

        PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        PowerManagerInternal getPowerManagerInternal() {
            return (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }

        TelephonyManager getTelephonyManager() {
            return TelephonyManager.from(this.mContext);
        }

        TrustManager getTrustManager() {
            return (TrustManager) this.mContext.getSystemService("trust");
        }

        AlarmManager getAlarmManager() {
            return (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }

        IWindowManager getIWindowManager() {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }

        IActivityManager getIActivityManager() {
            return ActivityManager.getService();
        }

        ActivityManagerInternal getActivityManagerInternal() {
            return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }

        IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        IBackupManager getIBackupManager() {
            return IBackupManager.Stub.asInterface(ServiceManager.getService(BatteryService.HealthServiceWrapper.INSTANCE_HEALTHD));
        }

        IAudioService getIAudioService() {
            return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        }

        boolean isBuildDebuggable() {
            return Build.IS_DEBUGGABLE;
        }

        LockPatternUtils newLockPatternUtils() {
            return new LockPatternUtils(this.mContext);
        }

        boolean storageManagerIsFileBasedEncryptionEnabled() {
            return StorageManager.isFileEncryptedNativeOnly();
        }

        boolean storageManagerIsNonDefaultBlockEncrypted() {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return StorageManager.isNonDefaultBlockEncrypted();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        boolean storageManagerIsEncrypted() {
            return StorageManager.isEncrypted();
        }

        boolean storageManagerIsEncryptable() {
            return StorageManager.isEncryptable();
        }

        Looper getMyLooper() {
            return Looper.myLooper();
        }

        WifiManager getWifiManager() {
            return (WifiManager) this.mContext.getSystemService(WifiManager.class);
        }

        long binderClearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        void binderRestoreCallingIdentity(long j) {
            Binder.restoreCallingIdentity(j);
        }

        int binderGetCallingUid() {
            return Binder.getCallingUid();
        }

        int binderGetCallingPid() {
            return Binder.getCallingPid();
        }

        UserHandle binderGetCallingUserHandle() {
            return Binder.getCallingUserHandle();
        }

        boolean binderIsCallingUidMyUid() {
            return Binder.getCallingUid() == Process.myUid();
        }

        void binderWithCleanCallingIdentity(FunctionalUtils.ThrowingRunnable throwingRunnable) {
            Binder.withCleanCallingIdentity(throwingRunnable);
        }

        final int userHandleGetCallingUserId() {
            return UserHandle.getUserId(binderGetCallingUid());
        }

        File environmentGetUserSystemDirectory(int i) {
            return Environment.getUserSystemDirectory(i);
        }

        void powerManagerGoToSleep(long j, int i, int i2) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).goToSleep(j, i, i2);
        }

        void powerManagerReboot(String str) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(str);
        }

        void recoverySystemRebootWipeUserData(boolean z, String str, boolean z2, boolean z3) throws IOException {
            RecoverySystem.rebootWipeUserData(this.mContext, z, str, z2, z3);
        }

        boolean systemPropertiesGetBoolean(String str, boolean z) {
            return SystemProperties.getBoolean(str, z);
        }

        long systemPropertiesGetLong(String str, long j) {
            return SystemProperties.getLong(str, j);
        }

        String systemPropertiesGet(String str, String str2) {
            return SystemProperties.get(str, str2);
        }

        String systemPropertiesGet(String str) {
            return SystemProperties.get(str);
        }

        void systemPropertiesSet(String str, String str2) {
            SystemProperties.set(str, str2);
        }

        boolean userManagerIsSplitSystemUser() {
            return UserManager.isSplitSystemUser();
        }

        String getDevicePolicyFilePathForSystemUser() {
            return "/data/system/";
        }

        PendingIntent pendingIntentGetActivityAsUser(Context context, int i, Intent intent, int i2, Bundle bundle, UserHandle userHandle) {
            return PendingIntent.getActivityAsUser(context, i, intent, i2, bundle, userHandle);
        }

        void registerContentObserver(Uri uri, boolean z, ContentObserver contentObserver, int i) {
            this.mContext.getContentResolver().registerContentObserver(uri, z, contentObserver, i);
        }

        int settingsSecureGetIntForUser(String str, int i, int i2) {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), str, i, i2);
        }

        String settingsSecureGetStringForUser(String str, int i) {
            return Settings.Secure.getStringForUser(this.mContext.getContentResolver(), str, i);
        }

        void settingsSecurePutIntForUser(String str, int i, int i2) {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), str, i, i2);
        }

        void settingsSecurePutStringForUser(String str, String str2, int i) {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), str, str2, i);
        }

        void settingsGlobalPutStringForUser(String str, String str2, int i) {
            Settings.Global.putStringForUser(this.mContext.getContentResolver(), str, str2, i);
        }

        void settingsSecurePutInt(String str, int i) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), str, i);
        }

        int settingsGlobalGetInt(String str, int i) {
            return Settings.Global.getInt(this.mContext.getContentResolver(), str, i);
        }

        String settingsGlobalGetString(String str) {
            return Settings.Global.getString(this.mContext.getContentResolver(), str);
        }

        void settingsGlobalPutInt(String str, int i) {
            Settings.Global.putInt(this.mContext.getContentResolver(), str, i);
        }

        void settingsSecurePutString(String str, String str2) {
            Settings.Secure.putString(this.mContext.getContentResolver(), str, str2);
        }

        void settingsGlobalPutString(String str, String str2) {
            Settings.Global.putString(this.mContext.getContentResolver(), str, str2);
        }

        void settingsSystemPutStringForUser(String str, String str2, int i) {
            Settings.System.putStringForUser(this.mContext.getContentResolver(), str, str2, i);
        }

        void securityLogSetLoggingEnabledProperty(boolean z) {
            SecurityLog.setLoggingEnabledProperty(z);
        }

        boolean securityLogGetLoggingEnabledProperty() {
            return SecurityLog.getLoggingEnabledProperty();
        }

        boolean securityLogIsLoggingEnabled() {
            return SecurityLog.isLoggingEnabled();
        }

        KeyChain.KeyChainConnection keyChainBindAsUser(UserHandle userHandle) throws InterruptedException {
            return KeyChain.bindAsUser(this.mContext, userHandle);
        }

        void postOnSystemServerInitThreadPool(Runnable runnable) {
            SystemServerInitThreadPool.get().submit(runnable, DevicePolicyManagerService.LOG_TAG);
        }

        public TransferOwnershipMetadataManager newTransferOwnershipMetadataManager() {
            return new TransferOwnershipMetadataManager();
        }

        public void runCryptoSelfTest() {
            CryptoTestHelper.runAndLogSelfTest();
        }
    }

    public DevicePolicyManagerService(Context context) {
        this(new Injector(context));
    }

    @VisibleForTesting
    DevicePolicyManagerService(Injector injector) {
        this.mPolicyCache = new DevicePolicyCacheImpl();
        this.mPackagesToRemove = new ArraySet();
        this.mToken = new Binder();
        this.mRemoteBugreportServiceIsActive = new AtomicBoolean();
        this.mRemoteBugreportSharingAccepted = new AtomicBoolean();
        this.mStatLogger = new StatLogger(new String[]{"LockGuard.guard()"});
        this.mLockDoNoUseDirectly = LockGuard.installNewLock(7, true);
        this.mRemoteBugreportTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (DevicePolicyManagerService.this.mRemoteBugreportServiceIsActive.get()) {
                    DevicePolicyManagerService.this.onBugreportFailed();
                }
            }
        };
        this.mRemoteBugreportFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) throws Throwable {
                if ("android.intent.action.REMOTE_BUGREPORT_DISPATCH".equals(intent.getAction()) && DevicePolicyManagerService.this.mRemoteBugreportServiceIsActive.get()) {
                    DevicePolicyManagerService.this.onBugreportFinished(intent);
                }
            }
        };
        this.mRemoteBugreportConsentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) throws Throwable {
                String action = intent.getAction();
                DevicePolicyManagerService.this.mInjector.getNotificationManager().cancel(DevicePolicyManagerService.LOG_TAG, 678432343);
                if ("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED".equals(action)) {
                    DevicePolicyManagerService.this.onBugreportSharingAccepted();
                } else if ("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED".equals(action)) {
                    DevicePolicyManagerService.this.onBugreportSharingDeclined();
                }
                DevicePolicyManagerService.this.mContext.unregisterReceiver(DevicePolicyManagerService.this.mRemoteBugreportConsentReceiver);
            }
        };
        this.mUserData = new SparseArray<>();
        this.mUserPasswordMetrics = new SparseArray<>();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                final int intExtra = intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId());
                if ("android.intent.action.USER_STARTED".equals(action) && intExtra == DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserId()) {
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        if (DevicePolicyManagerService.this.isNetworkLoggingEnabledInternalLocked()) {
                            DevicePolicyManagerService.this.setNetworkLoggingActiveInternal(true);
                        }
                    }
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) && intExtra == DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserId() && DevicePolicyManagerService.this.getDeviceOwnerRemoteBugreportUri() != null) {
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED");
                    intentFilter.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED");
                    DevicePolicyManagerService.this.mContext.registerReceiver(DevicePolicyManagerService.this.mRemoteBugreportConsentReceiver, intentFilter);
                    DevicePolicyManagerService.this.mInjector.getNotificationManager().notifyAsUser(DevicePolicyManagerService.LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(DevicePolicyManagerService.this.mContext, 3), UserHandle.ALL);
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) || DevicePolicyManagerService.ACTION_EXPIRED_PASSWORD_NOTIFICATION.equals(action)) {
                    DevicePolicyManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            DevicePolicyManagerService.this.handlePasswordExpirationNotification(intExtra);
                        }
                    });
                }
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_ADDED", intExtra);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybePauseDeviceWideLoggingLocked();
                    }
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_REMOVED", intExtra);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        boolean zIsUserAffiliatedWithDeviceLocked = DevicePolicyManagerService.this.isUserAffiliatedWithDeviceLocked(intExtra);
                        DevicePolicyManagerService.this.removeUserData(intExtra);
                        if (!zIsUserAffiliatedWithDeviceLocked) {
                            DevicePolicyManagerService.this.discardDeviceWideLogsLocked();
                            DevicePolicyManagerService.this.maybeResumeDeviceWideLoggingLocked();
                        }
                    }
                    return;
                }
                if ("android.intent.action.USER_STARTED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_STARTED", intExtra);
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybeSendAdminEnabledBroadcastLocked(intExtra);
                        DevicePolicyManagerService.this.mUserData.remove(intExtra);
                    }
                    DevicePolicyManagerService.this.handlePackagesChanged(null, intExtra);
                    return;
                }
                if ("android.intent.action.USER_STOPPED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_STOPPED", intExtra);
                    return;
                }
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    sendDeviceOwnerUserCommand("android.app.action.USER_SWITCHED", intExtra);
                    return;
                }
                if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    synchronized (DevicePolicyManagerService.this.getLockObject()) {
                        DevicePolicyManagerService.this.maybeSendAdminEnabledBroadcastLocked(intExtra);
                    }
                    return;
                }
                if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(null, intExtra);
                    return;
                }
                if ("android.intent.action.PACKAGE_CHANGED".equals(action) || ("android.intent.action.PACKAGE_ADDED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false))) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), intExtra);
                    return;
                }
                if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), intExtra);
                    return;
                }
                if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action)) {
                    DevicePolicyManagerService.this.clearWipeProfileNotification();
                } else if ("android.intent.action.DATE_CHANGED".equals(action) || "android.intent.action.TIME_SET".equals(action)) {
                    DevicePolicyManagerService.this.updateSystemUpdateFreezePeriodsRecord(true);
                }
            }

            private void sendDeviceOwnerUserCommand(String str, int i) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    ActiveAdmin deviceOwnerAdminLocked = DevicePolicyManagerService.this.getDeviceOwnerAdminLocked();
                    if (deviceOwnerAdminLocked != null) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("android.intent.extra.USER", UserHandle.of(i));
                        DevicePolicyManagerService.this.sendAdminCommandLocked(deviceOwnerAdminLocked, str, bundle, null, true);
                    }
                }
            }
        };
        this.mInjector = injector;
        this.mContext = (Context) Preconditions.checkNotNull(injector.mContext);
        this.mHandler = new Handler((Looper) Preconditions.checkNotNull(injector.getMyLooper()));
        this.mConstants = DevicePolicyConstants.loadFromString(this.mInjector.settingsGlobalGetString("device_policy_constants"));
        this.mOwners = (Owners) Preconditions.checkNotNull(injector.newOwners());
        this.mUserManager = (UserManager) Preconditions.checkNotNull(injector.getUserManager());
        this.mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull(injector.getUserManagerInternal());
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Preconditions.checkNotNull(injector.getUsageStatsManagerInternal());
        this.mIPackageManager = (IPackageManager) Preconditions.checkNotNull(injector.getIPackageManager());
        this.mTelephonyManager = (TelephonyManager) Preconditions.checkNotNull(injector.getTelephonyManager());
        this.mLocalService = new LocalService();
        this.mLockPatternUtils = injector.newLockPatternUtils();
        this.mSecurityLogMonitor = new SecurityLogMonitor(this);
        this.mHasFeature = this.mInjector.hasFeature();
        this.mIsWatch = this.mInjector.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        this.mBackgroundHandler = BackgroundThread.getHandler();
        this.mCertificateMonitor = new CertificateMonitor(this, this.mInjector, this.mBackgroundHandler);
        this.mDeviceAdminServiceController = new DeviceAdminServiceController(this, this.mConstants);
        this.mOverlayPackagesProvider = new OverlayPackagesProvider(this.mContext);
        this.mTransferOwnershipMetadataManager = this.mInjector.newTransferOwnershipMetadataManager();
        if (!this.mHasFeature) {
            this.mSetupContentObserver = null;
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        intentFilter.addAction(ACTION_EXPIRED_PASSWORD_NOTIFICATION);
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        intentFilter2.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter2.addDataScheme(com.android.server.pm.Settings.ATTR_PACKAGE);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter3.addAction("android.intent.action.TIME_SET");
        intentFilter3.addAction("android.intent.action.DATE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter3, null, this.mHandler);
        LocalServices.addService(DevicePolicyManagerInternal.class, this.mLocalService);
        this.mSetupContentObserver = new SetupContentObserver(this.mHandler);
        this.mUserManagerInternal.addUserRestrictionsListener(new RestrictionsListener(this.mContext));
    }

    DevicePolicyData getUserData(int i) {
        DevicePolicyData devicePolicyData;
        synchronized (getLockObject()) {
            devicePolicyData = this.mUserData.get(i);
            if (devicePolicyData == null) {
                devicePolicyData = new DevicePolicyData(i);
                this.mUserData.append(i, devicePolicyData);
                loadSettingsLocked(devicePolicyData, i);
            }
        }
        return devicePolicyData;
    }

    PasswordMetrics getUserPasswordMetricsLocked(int i) {
        return this.mUserPasswordMetrics.get(i);
    }

    DevicePolicyData getUserDataUnchecked(int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return getUserData(i);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    void removeUserData(int i) {
        synchronized (getLockObject()) {
            try {
                if (i == 0) {
                    Slog.w(LOG_TAG, "Tried to remove device policy file for user 0! Ignoring.");
                    return;
                }
                this.mPolicyCache.onUserRemoved(i);
                this.mOwners.removeProfileOwner(i);
                this.mOwners.writeProfileOwner(i);
                if (this.mUserData.get(i) != null) {
                    this.mUserData.remove(i);
                }
                if (this.mUserPasswordMetrics.get(i) != null) {
                    this.mUserPasswordMetrics.remove(i);
                }
                File file = new File(this.mInjector.environmentGetUserSystemDirectory(i), DEVICE_POLICIES_XML);
                file.delete();
                Slog.i(LOG_TAG, "Removed device policy file " + file.getAbsolutePath());
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    void loadOwners() {
        synchronized (getLockObject()) {
            this.mOwners.load();
            setDeviceOwnerSystemPropertyLocked();
            findOwnerComponentIfNecessaryLocked();
            migrateUserRestrictionsIfNecessaryLocked();
            maybeSetDefaultDeviceOwnerUserRestrictionsLocked();
            updateDeviceOwnerLocked();
        }
    }

    private void maybeSetDefaultDeviceOwnerUserRestrictionsLocked() {
        ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
        if (deviceOwnerAdminLocked != null) {
            maybeSetDefaultRestrictionsForAdminLocked(this.mOwners.getDeviceOwnerUserId(), deviceOwnerAdminLocked, UserRestrictionsUtils.getDefaultEnabledForDeviceOwner());
        }
    }

    private void maybeSetDefaultProfileOwnerUserRestrictions() {
        synchronized (getLockObject()) {
            Iterator<Integer> it = this.mOwners.getProfileOwnerKeys().iterator();
            while (it.hasNext()) {
                int iIntValue = it.next().intValue();
                ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(iIntValue);
                if (profileOwnerAdminLocked != null && this.mUserManager.isManagedProfile(iIntValue)) {
                    maybeSetDefaultRestrictionsForAdminLocked(iIntValue, profileOwnerAdminLocked, UserRestrictionsUtils.getDefaultEnabledForManagedProfiles());
                    ensureUnknownSourcesRestrictionForProfileOwnerLocked(iIntValue, profileOwnerAdminLocked, false);
                }
            }
        }
    }

    private void ensureUnknownSourcesRestrictionForProfileOwnerLocked(int i, ActiveAdmin activeAdmin, boolean z) {
        if (z || this.mInjector.settingsSecureGetIntForUser("unknown_sources_default_reversed", 0, i) != 0) {
            activeAdmin.ensureUserRestrictions().putBoolean("no_install_unknown_sources", true);
            saveUserRestrictionsLocked(i);
            this.mInjector.settingsSecurePutIntForUser("unknown_sources_default_reversed", 0, i);
        }
    }

    private void maybeSetDefaultRestrictionsForAdminLocked(int i, ActiveAdmin activeAdmin, Set<String> set) {
        if (set.equals(activeAdmin.defaultEnabledRestrictionsAlreadySet)) {
            return;
        }
        Slog.i(LOG_TAG, "New user restrictions need to be set by default for user " + i);
        ArraySet arraySet = new ArraySet(set);
        arraySet.removeAll(activeAdmin.defaultEnabledRestrictionsAlreadySet);
        if (!arraySet.isEmpty()) {
            Iterator it = arraySet.iterator();
            while (it.hasNext()) {
                activeAdmin.ensureUserRestrictions().putBoolean((String) it.next(), true);
            }
            activeAdmin.defaultEnabledRestrictionsAlreadySet.addAll(arraySet);
            Slog.i(LOG_TAG, "Enabled the following restrictions by default: " + arraySet);
            saveUserRestrictionsLocked(i);
        }
    }

    private void setDeviceOwnerSystemPropertyLocked() {
        boolean z = this.mInjector.settingsGlobalGetInt("device_provisioned", 0) != 0;
        boolean zHasDeviceOwner = this.mOwners.hasDeviceOwner();
        if ((!zHasDeviceOwner && !z) || StorageManager.inCryptKeeperBounce()) {
            return;
        }
        if (!this.mInjector.systemPropertiesGet(PROPERTY_DEVICE_OWNER_PRESENT, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).isEmpty()) {
            Slog.w(LOG_TAG, "Trying to set ro.device_owner, but it has already been set?");
            return;
        }
        String string = Boolean.toString(zHasDeviceOwner);
        this.mInjector.systemPropertiesSet(PROPERTY_DEVICE_OWNER_PRESENT, string);
        Slog.i(LOG_TAG, "Set ro.device_owner property to " + string);
    }

    private void maybeStartSecurityLogMonitorOnActivityManagerReady() {
        synchronized (getLockObject()) {
            if (this.mInjector.securityLogIsLoggingEnabled()) {
                this.mSecurityLogMonitor.start();
                this.mInjector.runCryptoSelfTest();
                maybePauseDeviceWideLoggingLocked();
            }
        }
    }

    private void findOwnerComponentIfNecessaryLocked() {
        if (!this.mOwners.hasDeviceOwner()) {
            return;
        }
        ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
        if (!TextUtils.isEmpty(deviceOwnerComponent.getClassName())) {
            return;
        }
        ComponentName componentNameFindAdminComponentWithPackageLocked = findAdminComponentWithPackageLocked(deviceOwnerComponent.getPackageName(), this.mOwners.getDeviceOwnerUserId());
        if (componentNameFindAdminComponentWithPackageLocked == null) {
            Slog.e(LOG_TAG, "Device-owner isn't registered as device-admin");
        } else {
            this.mOwners.setDeviceOwnerWithRestrictionsMigrated(componentNameFindAdminComponentWithPackageLocked, this.mOwners.getDeviceOwnerName(), this.mOwners.getDeviceOwnerUserId(), !this.mOwners.getDeviceOwnerUserRestrictionsNeedsMigration());
            this.mOwners.writeDeviceOwner();
        }
    }

    private void migrateUserRestrictionsIfNecessaryLocked() {
        if (this.mOwners.getDeviceOwnerUserRestrictionsNeedsMigration()) {
            migrateUserRestrictionsForUser(UserHandle.SYSTEM, getDeviceOwnerAdminLocked(), null, true);
            pushUserRestrictions(0);
            this.mOwners.setDeviceOwnerUserRestrictionsMigrated();
        }
        ArraySet arraySetNewArraySet = Sets.newArraySet(new String[]{"no_outgoing_calls", "no_sms"});
        for (UserInfo userInfo : this.mUserManager.getUsers()) {
            int i = userInfo.id;
            if (this.mOwners.getProfileOwnerUserRestrictionsNeedsMigration(i)) {
                migrateUserRestrictionsForUser(userInfo.getUserHandle(), getProfileOwnerAdminLocked(i), i == 0 ? null : arraySetNewArraySet, false);
                pushUserRestrictions(i);
                this.mOwners.setProfileOwnerUserRestrictionsMigrated(i);
            }
        }
    }

    private void migrateUserRestrictionsForUser(UserHandle userHandle, ActiveAdmin activeAdmin, Set<String> set, boolean z) {
        boolean zCanProfileOwnerChange;
        Bundle baseUserRestrictions = this.mUserManagerInternal.getBaseUserRestrictions(userHandle.getIdentifier());
        Bundle bundle = new Bundle();
        Bundle bundle2 = new Bundle();
        for (String str : baseUserRestrictions.keySet()) {
            if (baseUserRestrictions.getBoolean(str)) {
                if (z) {
                    zCanProfileOwnerChange = UserRestrictionsUtils.canDeviceOwnerChange(str);
                } else {
                    zCanProfileOwnerChange = UserRestrictionsUtils.canProfileOwnerChange(str, userHandle.getIdentifier());
                }
                if (!zCanProfileOwnerChange || (set != null && set.contains(str))) {
                    bundle.putBoolean(str, true);
                } else {
                    bundle2.putBoolean(str, true);
                }
            }
        }
        this.mUserManagerInternal.setBaseUserRestrictionsByDpmsForMigration(userHandle.getIdentifier(), bundle);
        if (activeAdmin != null) {
            activeAdmin.ensureUserRestrictions().clear();
            activeAdmin.ensureUserRestrictions().putAll(bundle2);
        } else {
            Slog.w(LOG_TAG, "ActiveAdmin for DO/PO not found. user=" + userHandle.getIdentifier());
        }
        saveSettingsLocked(userHandle.getIdentifier());
    }

    private ComponentName findAdminComponentWithPackageLocked(String str, int i) {
        DevicePolicyData userData = getUserData(i);
        int size = userData.mAdminList.size();
        ComponentName component = null;
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            ActiveAdmin activeAdmin = userData.mAdminList.get(i3);
            if (str.equals(activeAdmin.info.getPackageName())) {
                if (i2 == 0) {
                    component = activeAdmin.info.getComponent();
                }
                i2++;
            }
        }
        if (i2 > 1) {
            Slog.w(LOG_TAG, "Multiple DA found; assume the first one is DO.");
        }
        return component;
    }

    private void setExpirationAlarmCheckLocked(Context context, int i, boolean z) {
        long j;
        long passwordExpirationLocked = getPasswordExpirationLocked(null, i, z);
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j2 = passwordExpirationLocked - jCurrentTimeMillis;
        if (passwordExpirationLocked == 0) {
            j = 0;
        } else if (j2 <= 0) {
            j = MS_PER_DAY + jCurrentTimeMillis;
        } else {
            long j3 = j2 % MS_PER_DAY;
            if (j3 == 0) {
                j3 = MS_PER_DAY;
            }
            j = jCurrentTimeMillis + j3;
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        if (z) {
            try {
                i = getProfileParentId(i);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
        AlarmManager alarmManager = this.mInjector.getAlarmManager();
        PendingIntent broadcastAsUser = PendingIntent.getBroadcastAsUser(context, REQUEST_EXPIRE_PASSWORD, new Intent(ACTION_EXPIRED_PASSWORD_NOTIFICATION), 1207959552, UserHandle.of(i));
        alarmManager.cancel(broadcastAsUser);
        if (j != 0) {
            alarmManager.set(1, j, broadcastAsUser);
        }
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName componentName, int i) {
        ensureLocked();
        ActiveAdmin activeAdmin = getUserData(i).mAdminMap.get(componentName);
        if (activeAdmin != null && componentName.getPackageName().equals(activeAdmin.info.getActivityInfo().packageName) && componentName.getClassName().equals(activeAdmin.info.getActivityInfo().name)) {
            return activeAdmin;
        }
        return null;
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName componentName, int i, boolean z) {
        ensureLocked();
        if (z) {
            enforceManagedProfile(i, "call APIs on the parent profile");
        }
        ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
        if (activeAdminUncheckedLocked != null && z) {
            return activeAdminUncheckedLocked.getParentActiveAdmin();
        }
        return activeAdminUncheckedLocked;
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName componentName, int i) throws SecurityException {
        ensureLocked();
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        ActiveAdmin activeAdminWithPolicyForUidLocked = getActiveAdminWithPolicyForUidLocked(componentName, i, iBinderGetCallingUid);
        if (activeAdminWithPolicyForUidLocked != null) {
            return activeAdminWithPolicyForUidLocked;
        }
        if (componentName != null) {
            ActiveAdmin activeAdmin = getUserData(UserHandle.getUserId(iBinderGetCallingUid)).mAdminMap.get(componentName);
            if (i == -2) {
                throw new SecurityException("Admin " + activeAdmin.info.getComponent() + " does not own the device");
            }
            if (i == -1) {
                throw new SecurityException("Admin " + activeAdmin.info.getComponent() + " does not own the profile");
            }
            throw new SecurityException("Admin " + activeAdmin.info.getComponent() + " did not specify uses-policy for: " + activeAdmin.info.getTagForPolicy(i));
        }
        throw new SecurityException("No active admin owned by uid " + this.mInjector.binderGetCallingUid() + " for policy #" + i);
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName componentName, int i, boolean z) throws SecurityException {
        ensureLocked();
        if (z) {
            enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "call APIs on the parent profile");
        }
        ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, i);
        return z ? activeAdminForCallerLocked.getParentActiveAdmin() : activeAdminForCallerLocked;
    }

    private ActiveAdmin getActiveAdminForUidLocked(ComponentName componentName, int i) {
        ensureLocked();
        ActiveAdmin activeAdmin = getUserData(UserHandle.getUserId(i)).mAdminMap.get(componentName);
        if (activeAdmin == null) {
            throw new SecurityException("No active admin " + componentName);
        }
        if (activeAdmin.getUid() != i) {
            throw new SecurityException("Admin " + componentName + " is not owned by uid " + i);
        }
        return activeAdmin;
    }

    private ActiveAdmin getActiveAdminWithPolicyForUidLocked(ComponentName componentName, int i, int i2) {
        ensureLocked();
        int userId = UserHandle.getUserId(i2);
        DevicePolicyData userData = getUserData(userId);
        if (componentName != null) {
            ActiveAdmin activeAdmin = userData.mAdminMap.get(componentName);
            if (activeAdmin == null) {
                throw new SecurityException("No active admin " + componentName);
            }
            if (activeAdmin.getUid() != i2) {
                throw new SecurityException("Admin " + componentName + " is not owned by uid " + i2);
            }
            if (isActiveAdminWithPolicyForUserLocked(activeAdmin, i, userId)) {
                return activeAdmin;
            }
            return null;
        }
        for (ActiveAdmin activeAdmin2 : userData.mAdminList) {
            if (activeAdmin2.getUid() == i2 && isActiveAdminWithPolicyForUserLocked(activeAdmin2, i, userId)) {
                return activeAdmin2;
            }
        }
        return null;
    }

    @VisibleForTesting
    boolean isActiveAdminWithPolicyForUserLocked(ActiveAdmin activeAdmin, int i, int i2) {
        ensureLocked();
        boolean zIsDeviceOwner = isDeviceOwner(activeAdmin.info.getComponent(), i2);
        boolean zIsProfileOwner = isProfileOwner(activeAdmin.info.getComponent(), i2);
        if (i == -2) {
            return zIsDeviceOwner;
        }
        if (i == -1) {
            return zIsDeviceOwner || zIsProfileOwner;
        }
        return activeAdmin.info.usesPolicy(i);
    }

    void sendAdminCommandLocked(ActiveAdmin activeAdmin, String str) {
        sendAdminCommandLocked(activeAdmin, str, null);
    }

    void sendAdminCommandLocked(ActiveAdmin activeAdmin, String str, BroadcastReceiver broadcastReceiver) {
        sendAdminCommandLocked(activeAdmin, str, (Bundle) null, broadcastReceiver);
    }

    void sendAdminCommandLocked(ActiveAdmin activeAdmin, String str, Bundle bundle, BroadcastReceiver broadcastReceiver) {
        sendAdminCommandLocked(activeAdmin, str, bundle, broadcastReceiver, false);
    }

    boolean sendAdminCommandLocked(ActiveAdmin activeAdmin, String str, Bundle bundle, BroadcastReceiver broadcastReceiver, boolean z) {
        Intent intent = new Intent(str);
        intent.setComponent(activeAdmin.info.getComponent());
        if (UserManager.isDeviceInDemoMode(this.mContext)) {
            intent.addFlags(268435456);
        }
        if (str.equals("android.app.action.ACTION_PASSWORD_EXPIRING")) {
            intent.putExtra("expiration", activeAdmin.passwordExpirationDate);
        }
        if (z) {
            intent.addFlags(268435456);
        }
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (this.mInjector.getPackageManager().queryBroadcastReceiversAsUser(intent, 268435456, activeAdmin.getUserHandle()).isEmpty()) {
            return false;
        }
        if (broadcastReceiver != null) {
            this.mContext.sendOrderedBroadcastAsUser(intent, activeAdmin.getUserHandle(), null, broadcastReceiver, this.mHandler, -1, null, null);
            return true;
        }
        this.mContext.sendBroadcastAsUser(intent, activeAdmin.getUserHandle());
        return true;
    }

    void sendAdminCommandLocked(String str, int i, int i2, Bundle bundle) {
        DevicePolicyData userData = getUserData(i2);
        int size = userData.mAdminList.size();
        for (int i3 = 0; i3 < size; i3++) {
            ActiveAdmin activeAdmin = userData.mAdminList.get(i3);
            if (activeAdmin.info.usesPolicy(i)) {
                sendAdminCommandLocked(activeAdmin, str, bundle, (BroadcastReceiver) null);
            }
        }
    }

    private void sendAdminCommandToSelfAndProfilesLocked(String str, int i, int i2, Bundle bundle) {
        for (int i3 : this.mUserManager.getProfileIdsWithDisabled(i2)) {
            sendAdminCommandLocked(str, i, i3, bundle);
        }
    }

    private void sendAdminCommandForLockscreenPoliciesLocked(String str, int i, int i2) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("android.intent.extra.USER", UserHandle.of(i2));
        if (isSeparateProfileChallengeEnabled(i2)) {
            sendAdminCommandLocked(str, i, i2, bundle);
        } else {
            sendAdminCommandToSelfAndProfilesLocked(str, i, i2, bundle);
        }
    }

    void removeActiveAdminLocked(final ComponentName componentName, final int i) {
        ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
        DevicePolicyData userData = getUserData(i);
        if (activeAdminUncheckedLocked != null && !userData.mRemovingAdmins.contains(componentName)) {
            userData.mRemovingAdmins.add(componentName);
            sendAdminCommandLocked(activeAdminUncheckedLocked, "android.app.action.DEVICE_ADMIN_DISABLED", new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    DevicePolicyManagerService.this.removeAdminArtifacts(componentName, i);
                    DevicePolicyManagerService.this.removePackageIfRequired(componentName.getPackageName(), i);
                }
            });
        }
    }

    public DeviceAdminInfo findAdmin(ComponentName componentName, int i, boolean z) {
        ActivityInfo receiverInfo;
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(i);
        try {
            receiverInfo = this.mIPackageManager.getReceiverInfo(componentName, 819328, i);
        } catch (RemoteException e) {
            receiverInfo = null;
        }
        if (receiverInfo == null) {
            throw new IllegalArgumentException("Unknown admin: " + componentName);
        }
        if (!"android.permission.BIND_DEVICE_ADMIN".equals(receiverInfo.permission)) {
            String str = "DeviceAdminReceiver " + componentName + " must be protected with android.permission.BIND_DEVICE_ADMIN";
            Slog.w(LOG_TAG, str);
            if (z && receiverInfo.applicationInfo.targetSdkVersion > 23) {
                throw new IllegalArgumentException(str);
            }
        }
        try {
            return new DeviceAdminInfo(this.mContext, receiverInfo);
        } catch (IOException | XmlPullParserException e2) {
            Slog.w(LOG_TAG, "Bad device admin requested for user=" + i + ": " + componentName, e2);
            return null;
        }
    }

    private File getPolicyFileDirectory(int i) {
        if (i == 0) {
            return new File(this.mInjector.getDevicePolicyFilePathForSystemUser());
        }
        return this.mInjector.environmentGetUserSystemDirectory(i);
    }

    private JournaledFile makeJournaledFile(int i) {
        String absolutePath = new File(getPolicyFileDirectory(i), DEVICE_POLICIES_XML).getAbsolutePath();
        return new JournaledFile(new File(absolutePath), new File(absolutePath + ".tmp"));
    }

    private void saveSettingsLocked(int i) {
        FileOutputStream fileOutputStream;
        DevicePolicyData userData = getUserData(i);
        JournaledFile journaledFileMakeJournaledFile = makeJournaledFile(i);
        try {
            fileOutputStream = new FileOutputStream(journaledFileMakeJournaledFile.chooseForWrite(), false);
            try {
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStream, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, "policies");
                if (userData.mRestrictionsProvider != null) {
                    fastXmlSerializer.attribute(null, ATTR_PERMISSION_PROVIDER, userData.mRestrictionsProvider.flattenToString());
                }
                if (userData.mUserSetupComplete) {
                    fastXmlSerializer.attribute(null, ATTR_SETUP_COMPLETE, Boolean.toString(true));
                }
                if (userData.mPaired) {
                    fastXmlSerializer.attribute(null, ATTR_DEVICE_PAIRED, Boolean.toString(true));
                }
                if (userData.mDeviceProvisioningConfigApplied) {
                    fastXmlSerializer.attribute(null, ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED, Boolean.toString(true));
                }
                if (userData.mUserProvisioningState != 0) {
                    fastXmlSerializer.attribute(null, ATTR_PROVISIONING_STATE, Integer.toString(userData.mUserProvisioningState));
                }
                if (userData.mPermissionPolicy != 0) {
                    fastXmlSerializer.attribute(null, ATTR_PERMISSION_POLICY, Integer.toString(userData.mPermissionPolicy));
                }
                for (int i2 = 0; i2 < userData.mDelegationMap.size(); i2++) {
                    String strKeyAt = userData.mDelegationMap.keyAt(i2);
                    for (String str : userData.mDelegationMap.valueAt(i2)) {
                        fastXmlSerializer.startTag(null, "delegation");
                        fastXmlSerializer.attribute(null, "delegatePackage", strKeyAt);
                        fastXmlSerializer.attribute(null, "scope", str);
                        fastXmlSerializer.endTag(null, "delegation");
                    }
                }
                int size = userData.mAdminList.size();
                for (int i3 = 0; i3 < size; i3++) {
                    ActiveAdmin activeAdmin = userData.mAdminList.get(i3);
                    if (activeAdmin != null) {
                        fastXmlSerializer.startTag(null, "admin");
                        fastXmlSerializer.attribute(null, "name", activeAdmin.info.getComponent().flattenToString());
                        activeAdmin.writeToXml(fastXmlSerializer);
                        fastXmlSerializer.endTag(null, "admin");
                    }
                }
                if (userData.mPasswordOwner >= 0) {
                    fastXmlSerializer.startTag(null, "password-owner");
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(userData.mPasswordOwner));
                    fastXmlSerializer.endTag(null, "password-owner");
                }
                if (userData.mFailedPasswordAttempts != 0) {
                    fastXmlSerializer.startTag(null, "failed-password-attempts");
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(userData.mFailedPasswordAttempts));
                    fastXmlSerializer.endTag(null, "failed-password-attempts");
                }
                if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                    fastXmlSerializer.startTag(null, TAG_PASSWORD_VALIDITY);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(userData.mPasswordValidAtLastCheckpoint));
                    fastXmlSerializer.endTag(null, TAG_PASSWORD_VALIDITY);
                }
                for (int i4 = 0; i4 < userData.mAcceptedCaCertificates.size(); i4++) {
                    fastXmlSerializer.startTag(null, TAG_ACCEPTED_CA_CERTIFICATES);
                    fastXmlSerializer.attribute(null, "name", userData.mAcceptedCaCertificates.valueAt(i4));
                    fastXmlSerializer.endTag(null, TAG_ACCEPTED_CA_CERTIFICATES);
                }
                for (int i5 = 0; i5 < userData.mLockTaskPackages.size(); i5++) {
                    String str2 = userData.mLockTaskPackages.get(i5);
                    fastXmlSerializer.startTag(null, TAG_LOCK_TASK_COMPONENTS);
                    fastXmlSerializer.attribute(null, "name", str2);
                    fastXmlSerializer.endTag(null, TAG_LOCK_TASK_COMPONENTS);
                }
                if (userData.mLockTaskFeatures != 0) {
                    fastXmlSerializer.startTag(null, TAG_LOCK_TASK_FEATURES);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Integer.toString(userData.mLockTaskFeatures));
                    fastXmlSerializer.endTag(null, TAG_LOCK_TASK_FEATURES);
                }
                if (userData.mStatusBarDisabled) {
                    fastXmlSerializer.startTag(null, TAG_STATUS_BAR);
                    fastXmlSerializer.attribute(null, ATTR_DISABLED, Boolean.toString(userData.mStatusBarDisabled));
                    fastXmlSerializer.endTag(null, TAG_STATUS_BAR);
                }
                if (userData.doNotAskCredentialsOnBoot) {
                    fastXmlSerializer.startTag(null, DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML);
                    fastXmlSerializer.endTag(null, DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML);
                }
                for (String str3 : userData.mAffiliationIds) {
                    fastXmlSerializer.startTag(null, TAG_AFFILIATION_ID);
                    fastXmlSerializer.attribute(null, ATTR_ID, str3);
                    fastXmlSerializer.endTag(null, TAG_AFFILIATION_ID);
                }
                if (userData.mLastSecurityLogRetrievalTime >= 0) {
                    fastXmlSerializer.startTag(null, TAG_LAST_SECURITY_LOG_RETRIEVAL);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Long.toString(userData.mLastSecurityLogRetrievalTime));
                    fastXmlSerializer.endTag(null, TAG_LAST_SECURITY_LOG_RETRIEVAL);
                }
                if (userData.mLastBugReportRequestTime >= 0) {
                    fastXmlSerializer.startTag(null, TAG_LAST_BUG_REPORT_REQUEST);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Long.toString(userData.mLastBugReportRequestTime));
                    fastXmlSerializer.endTag(null, TAG_LAST_BUG_REPORT_REQUEST);
                }
                if (userData.mLastNetworkLogsRetrievalTime >= 0) {
                    fastXmlSerializer.startTag(null, TAG_LAST_NETWORK_LOG_RETRIEVAL);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Long.toString(userData.mLastNetworkLogsRetrievalTime));
                    fastXmlSerializer.endTag(null, TAG_LAST_NETWORK_LOG_RETRIEVAL);
                }
                if (userData.mAdminBroadcastPending) {
                    fastXmlSerializer.startTag(null, TAG_ADMIN_BROADCAST_PENDING);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Boolean.toString(userData.mAdminBroadcastPending));
                    fastXmlSerializer.endTag(null, TAG_ADMIN_BROADCAST_PENDING);
                }
                if (userData.mInitBundle != null) {
                    fastXmlSerializer.startTag(null, TAG_INITIALIZATION_BUNDLE);
                    userData.mInitBundle.saveToXml(fastXmlSerializer);
                    fastXmlSerializer.endTag(null, TAG_INITIALIZATION_BUNDLE);
                }
                if (userData.mPasswordTokenHandle != 0) {
                    fastXmlSerializer.startTag(null, TAG_PASSWORD_TOKEN_HANDLE);
                    fastXmlSerializer.attribute(null, ATTR_VALUE, Long.toString(userData.mPasswordTokenHandle));
                    fastXmlSerializer.endTag(null, TAG_PASSWORD_TOKEN_HANDLE);
                }
                if (userData.mCurrentInputMethodSet) {
                    fastXmlSerializer.startTag(null, TAG_CURRENT_INPUT_METHOD_SET);
                    fastXmlSerializer.endTag(null, TAG_CURRENT_INPUT_METHOD_SET);
                }
                for (String str4 : userData.mOwnerInstalledCaCerts) {
                    fastXmlSerializer.startTag(null, TAG_OWNER_INSTALLED_CA_CERT);
                    fastXmlSerializer.attribute(null, ATTR_ALIAS, str4);
                    fastXmlSerializer.endTag(null, TAG_OWNER_INSTALLED_CA_CERT);
                }
                fastXmlSerializer.endTag(null, "policies");
                fastXmlSerializer.endDocument();
                fileOutputStream.flush();
                FileUtils.sync(fileOutputStream);
                fileOutputStream.close();
                journaledFileMakeJournaledFile.commit();
                sendChangedNotification(i);
            } catch (IOException | XmlPullParserException e) {
                e = e;
                Slog.w(LOG_TAG, "failed writing file", e);
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e2) {
                    }
                }
                journaledFileMakeJournaledFile.rollback();
            }
        } catch (IOException | XmlPullParserException e3) {
            e = e3;
            fileOutputStream = null;
        }
    }

    private void sendChangedNotification(int i) {
        Intent intent = new Intent("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intent.setFlags(1073741824);
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(i));
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void loadSettingsLocked(DevicePolicyData devicePolicyData, int i) {
        boolean z;
        FileInputStream fileInputStream;
        XmlPullParser xmlPullParserNewPullParser;
        int next;
        String name;
        String attributeValue;
        int next2;
        File fileChooseForRead = makeJournaledFile(i).chooseForRead();
        try {
            fileInputStream = new FileInputStream(fileChooseForRead);
            try {
                try {
                    xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
                    do {
                        next = xmlPullParserNewPullParser.next();
                        if (next == 1) {
                            break;
                        }
                    } while (next != 2);
                    name = xmlPullParserNewPullParser.getName();
                } catch (FileNotFoundException e) {
                    z = false;
                }
            } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e2) {
                e = e2;
                z = false;
            }
        } catch (FileNotFoundException e3) {
            z = false;
            fileInputStream = null;
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e4) {
            e = e4;
            z = false;
            fileInputStream = null;
        }
        if (!"policies".equals(name)) {
            throw new XmlPullParserException("Settings do not start with policies tag: found " + name);
        }
        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PERMISSION_PROVIDER);
        if (attributeValue2 != null) {
            devicePolicyData.mRestrictionsProvider = ComponentName.unflattenFromString(attributeValue2);
        }
        String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_SETUP_COMPLETE);
        if (attributeValue3 != null && Boolean.toString(true).equals(attributeValue3)) {
            devicePolicyData.mUserSetupComplete = true;
        }
        String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_DEVICE_PAIRED);
        if (attributeValue4 != null && Boolean.toString(true).equals(attributeValue4)) {
            devicePolicyData.mPaired = true;
        }
        String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_DEVICE_PROVISIONING_CONFIG_APPLIED);
        if (attributeValue5 != null && Boolean.toString(true).equals(attributeValue5)) {
            devicePolicyData.mDeviceProvisioningConfigApplied = true;
        }
        String attributeValue6 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PROVISIONING_STATE);
        if (!TextUtils.isEmpty(attributeValue6)) {
            devicePolicyData.mUserProvisioningState = Integer.parseInt(attributeValue6);
        }
        String attributeValue7 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_PERMISSION_POLICY);
        if (!TextUtils.isEmpty(attributeValue7)) {
            devicePolicyData.mPermissionPolicy = Integer.parseInt(attributeValue7);
        }
        String attributeValue8 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_DELEGATED_CERT_INSTALLER);
        if (attributeValue8 == null) {
            z = false;
            try {
                try {
                    attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_APPLICATION_RESTRICTIONS_MANAGER);
                    if (attributeValue != null) {
                        List<String> arrayList = devicePolicyData.mDelegationMap.get(attributeValue);
                        if (arrayList == null) {
                            arrayList = new ArrayList<>();
                            devicePolicyData.mDelegationMap.put(attributeValue, arrayList);
                        }
                        if (!arrayList.contains("delegation-app-restrictions")) {
                            arrayList.add("delegation-app-restrictions");
                            z = true;
                        }
                    }
                    xmlPullParserNewPullParser.next();
                    int depth = xmlPullParserNewPullParser.getDepth();
                    devicePolicyData.mLockTaskPackages.clear();
                    devicePolicyData.mAdminList.clear();
                    devicePolicyData.mAdminMap.clear();
                    devicePolicyData.mAffiliationIds.clear();
                    devicePolicyData.mOwnerInstalledCaCerts.clear();
                    while (true) {
                        next2 = xmlPullParserNewPullParser.next();
                        if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                            break;
                        }
                        if (next2 != 3 && next2 != 4) {
                            String name2 = xmlPullParserNewPullParser.getName();
                            if ("admin".equals(name2)) {
                                String attributeValue9 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                                try {
                                    DeviceAdminInfo deviceAdminInfoFindAdmin = findAdmin(ComponentName.unflattenFromString(attributeValue9), i, false);
                                    if (deviceAdminInfoFindAdmin != null) {
                                        ActiveAdmin activeAdmin = new ActiveAdmin(deviceAdminInfoFindAdmin, false);
                                        activeAdmin.readFromXml(xmlPullParserNewPullParser);
                                        devicePolicyData.mAdminMap.put(activeAdmin.info.getComponent(), activeAdmin);
                                    }
                                } catch (RuntimeException e5) {
                                    Slog.w(LOG_TAG, "Failed loading admin " + attributeValue9, e5);
                                }
                            } else if ("delegation".equals(name2)) {
                                String attributeValue10 = xmlPullParserNewPullParser.getAttributeValue(null, "delegatePackage");
                                String attributeValue11 = xmlPullParserNewPullParser.getAttributeValue(null, "scope");
                                List<String> arrayList2 = devicePolicyData.mDelegationMap.get(attributeValue10);
                                if (arrayList2 == null) {
                                    arrayList2 = new ArrayList<>();
                                    devicePolicyData.mDelegationMap.put(attributeValue10, arrayList2);
                                }
                                if (!arrayList2.contains(attributeValue11)) {
                                    arrayList2.add(attributeValue11);
                                }
                            } else if ("failed-password-attempts".equals(name2)) {
                                devicePolicyData.mFailedPasswordAttempts = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if ("password-owner".equals(name2)) {
                                devicePolicyData.mPasswordOwner = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_ACCEPTED_CA_CERTIFICATES.equals(name2)) {
                                devicePolicyData.mAcceptedCaCertificates.add(xmlPullParserNewPullParser.getAttributeValue(null, "name"));
                            } else if (TAG_LOCK_TASK_COMPONENTS.equals(name2)) {
                                devicePolicyData.mLockTaskPackages.add(xmlPullParserNewPullParser.getAttributeValue(null, "name"));
                            } else if (TAG_LOCK_TASK_FEATURES.equals(name2)) {
                                devicePolicyData.mLockTaskFeatures = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_STATUS_BAR.equals(name2)) {
                                devicePolicyData.mStatusBarDisabled = Boolean.parseBoolean(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_DISABLED));
                            } else if (DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML.equals(name2)) {
                                devicePolicyData.doNotAskCredentialsOnBoot = true;
                            } else if (TAG_AFFILIATION_ID.equals(name2)) {
                                devicePolicyData.mAffiliationIds.add(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ID));
                            } else if (TAG_LAST_SECURITY_LOG_RETRIEVAL.equals(name2)) {
                                devicePolicyData.mLastSecurityLogRetrievalTime = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_LAST_BUG_REPORT_REQUEST.equals(name2)) {
                                devicePolicyData.mLastBugReportRequestTime = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_LAST_NETWORK_LOG_RETRIEVAL.equals(name2)) {
                                devicePolicyData.mLastNetworkLogsRetrievalTime = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_ADMIN_BROADCAST_PENDING.equals(name2)) {
                                devicePolicyData.mAdminBroadcastPending = Boolean.toString(true).equals(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_INITIALIZATION_BUNDLE.equals(name2)) {
                                devicePolicyData.mInitBundle = PersistableBundle.restoreFromXml(xmlPullParserNewPullParser);
                            } else if ("active-password".equals(name2)) {
                                z = true;
                            } else if (TAG_PASSWORD_VALIDITY.equals(name2)) {
                                if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                                    devicePolicyData.mPasswordValidAtLastCheckpoint = Boolean.parseBoolean(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                                }
                            } else if (TAG_PASSWORD_TOKEN_HANDLE.equals(name2)) {
                                devicePolicyData.mPasswordTokenHandle = Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_VALUE));
                            } else if (TAG_CURRENT_INPUT_METHOD_SET.equals(name2)) {
                                devicePolicyData.mCurrentInputMethodSet = true;
                            } else if (TAG_OWNER_INSTALLED_CA_CERT.equals(name2)) {
                                devicePolicyData.mOwnerInstalledCaCerts.add(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ALIAS));
                            } else {
                                Slog.w(LOG_TAG, "Unknown tag: " + name2);
                                XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                            }
                        }
                    }
                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e6) {
                    e = e6;
                    Slog.w(LOG_TAG, "failed parsing " + fileChooseForRead, e);
                }
            } catch (FileNotFoundException e7) {
            }
        } else {
            List<String> arrayList3 = devicePolicyData.mDelegationMap.get(attributeValue8);
            if (arrayList3 == null) {
                arrayList3 = new ArrayList<>();
                devicePolicyData.mDelegationMap.put(attributeValue8, arrayList3);
            }
            if (!arrayList3.contains("delegation-cert-install")) {
                arrayList3.add("delegation-cert-install");
                z = true;
            }
            attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_APPLICATION_RESTRICTIONS_MANAGER);
            if (attributeValue != null) {
            }
            xmlPullParserNewPullParser.next();
            int depth2 = xmlPullParserNewPullParser.getDepth();
            devicePolicyData.mLockTaskPackages.clear();
            devicePolicyData.mAdminList.clear();
            devicePolicyData.mAdminMap.clear();
            devicePolicyData.mAffiliationIds.clear();
            devicePolicyData.mOwnerInstalledCaCerts.clear();
            while (true) {
                next2 = xmlPullParserNewPullParser.next();
                if (next2 == 1) {
                    break;
                }
                break;
                break;
            }
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e8) {
            }
        }
        devicePolicyData.mAdminList.addAll(devicePolicyData.mAdminMap.values());
        if (z) {
            saveSettingsLocked(i);
        }
        validatePasswordOwnerLocked(devicePolicyData);
        updateMaximumTimeToLockLocked(i);
        updateLockTaskPackagesLocked(devicePolicyData.mLockTaskPackages, i);
        updateLockTaskFeaturesLocked(devicePolicyData.mLockTaskFeatures, i);
        if (devicePolicyData.mStatusBarDisabled) {
            setStatusBarDisabledInternal(devicePolicyData.mStatusBarDisabled, i);
        }
    }

    private void updateLockTaskPackagesLocked(List<String> list, int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().updateLockTaskPackages(i, (String[]) list.toArray(new String[list.size()]));
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            throw th;
        }
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
    }

    private void updateLockTaskFeaturesLocked(int i, int i2) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().updateLockTaskFeatures(i2, i);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            throw th;
        }
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
    }

    private void updateDeviceOwnerLocked() {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
            if (deviceOwnerComponent != null) {
                this.mInjector.getIActivityManager().updateDeviceOwner(deviceOwnerComponent.getPackageName());
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            throw th;
        }
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
    }

    static void validateQualityConstant(int i) {
        if (i == 0 || i == 32768 || i == 65536 || i == 131072 || i == 196608 || i == 262144 || i == 327680 || i == 393216 || i == 524288) {
            return;
        }
        throw new IllegalArgumentException("Invalid quality constant: 0x" + Integer.toHexString(i));
    }

    void validatePasswordOwnerLocked(DevicePolicyData devicePolicyData) {
        if (devicePolicyData.mPasswordOwner >= 0) {
            boolean z = false;
            int size = devicePolicyData.mAdminList.size() - 1;
            while (true) {
                if (size < 0) {
                    break;
                }
                if (devicePolicyData.mAdminList.get(size).getUid() != devicePolicyData.mPasswordOwner) {
                    size--;
                } else {
                    z = true;
                    break;
                }
            }
            if (!z) {
                Slog.w(LOG_TAG, "Previous password owner " + devicePolicyData.mPasswordOwner + " no longer active; disabling");
                devicePolicyData.mPasswordOwner = -1;
            }
        }
    }

    @Override
    @VisibleForTesting
    void systemReady(int i) {
        if (!this.mHasFeature) {
            return;
        }
        if (i == 480) {
            onLockSettingsReady();
            loadAdminDataAsync();
            this.mOwners.systemReady();
        } else if (i == 550) {
            maybeStartSecurityLogMonitorOnActivityManagerReady();
        } else if (i == 1000) {
            ensureDeviceOwnerUserStarted();
        }
    }

    private void onLockSettingsReady() {
        List<String> keepUninstalledPackagesLocked;
        getUserData(0);
        loadOwners();
        cleanUpOldUsers();
        maybeSetDefaultProfileOwnerUserRestrictions();
        handleStartUser(0);
        maybeLogStart();
        this.mSetupContentObserver.register();
        updateUserSetupCompleteAndPaired();
        synchronized (getLockObject()) {
            keepUninstalledPackagesLocked = getKeepUninstalledPackagesLocked();
        }
        if (keepUninstalledPackagesLocked != null) {
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(keepUninstalledPackagesLocked);
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
            if (deviceOwnerAdminLocked != null) {
                this.mUserManagerInternal.setForceEphemeralUsers(deviceOwnerAdminLocked.forceEphemeralUsers);
                ActivityManagerInternal activityManagerInternal = this.mInjector.getActivityManagerInternal();
                activityManagerInternal.setSwitchingFromSystemUserMessage(deviceOwnerAdminLocked.startUserSessionMessage);
                activityManagerInternal.setSwitchingToSystemUserMessage(deviceOwnerAdminLocked.endUserSessionMessage);
            }
            revertTransferOwnershipIfNecessaryLocked();
        }
    }

    private void revertTransferOwnershipIfNecessaryLocked() {
        if (!this.mTransferOwnershipMetadataManager.metadataFileExists()) {
            return;
        }
        Slog.e(LOG_TAG, "Owner transfer metadata file exists! Reverting transfer.");
        TransferOwnershipMetadataManager.Metadata metadataLoadMetadataFile = this.mTransferOwnershipMetadataManager.loadMetadataFile();
        if (metadataLoadMetadataFile.adminType.equals(LOG_TAG_PROFILE_OWNER)) {
            transferProfileOwnershipLocked(metadataLoadMetadataFile.targetComponent, metadataLoadMetadataFile.sourceComponent, metadataLoadMetadataFile.userId);
            deleteTransferOwnershipMetadataFileLocked();
            deleteTransferOwnershipBundleLocked(metadataLoadMetadataFile.userId);
        } else if (metadataLoadMetadataFile.adminType.equals(LOG_TAG_DEVICE_OWNER)) {
            transferDeviceOwnershipLocked(metadataLoadMetadataFile.targetComponent, metadataLoadMetadataFile.sourceComponent, metadataLoadMetadataFile.userId);
            deleteTransferOwnershipMetadataFileLocked();
            deleteTransferOwnershipBundleLocked(metadataLoadMetadataFile.userId);
        }
        updateSystemUpdateFreezePeriodsRecord(true);
    }

    private void maybeLogStart() {
        if (!SecurityLog.isLoggingEnabled()) {
            return;
        }
        SecurityLog.writeEvent(210009, new Object[]{this.mInjector.systemPropertiesGet("ro.boot.verifiedbootstate"), this.mInjector.systemPropertiesGet("ro.boot.veritymode")});
    }

    private void ensureDeviceOwnerUserStarted() {
        synchronized (getLockObject()) {
            if (this.mOwners.hasDeviceOwner()) {
                int deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
                if (deviceOwnerUserId != 0) {
                    try {
                        this.mInjector.getIActivityManager().startUserInBackground(deviceOwnerUserId);
                    } catch (RemoteException e) {
                        Slog.w(LOG_TAG, "Exception starting user", e);
                    }
                }
            }
        }
    }

    @Override
    void handleStartUser(int i) {
        updateScreenCaptureDisabled(i, getScreenCaptureDisabled(null, i));
        pushUserRestrictions(i);
        startOwnerService(i, "start-user");
    }

    @Override
    void handleUnlockUser(int i) {
        startOwnerService(i, "unlock-user");
    }

    @Override
    void handleStopUser(int i) {
        stopOwnerService(i, "stop-user");
    }

    private void startOwnerService(int i, String str) {
        ComponentName ownerComponent = getOwnerComponent(i);
        if (ownerComponent != null) {
            this.mDeviceAdminServiceController.startServiceForOwner(ownerComponent.getPackageName(), i, str);
        }
    }

    private void stopOwnerService(int i, String str) {
        this.mDeviceAdminServiceController.stopServiceForOwner(i, str);
    }

    private void cleanUpOldUsers() {
        Set<Integer> profileOwnerKeys;
        ArraySet arraySet;
        synchronized (getLockObject()) {
            profileOwnerKeys = this.mOwners.getProfileOwnerKeys();
            arraySet = new ArraySet();
            for (int i = 0; i < this.mUserData.size(); i++) {
                arraySet.add(Integer.valueOf(this.mUserData.keyAt(i)));
            }
        }
        List users = this.mUserManager.getUsers();
        ArraySet arraySet2 = new ArraySet();
        arraySet2.addAll(profileOwnerKeys);
        arraySet2.addAll((Collection) arraySet);
        Iterator it = users.iterator();
        while (it.hasNext()) {
            arraySet2.remove(Integer.valueOf(((UserInfo) it.next()).id));
        }
        Iterator it2 = arraySet2.iterator();
        while (it2.hasNext()) {
            removeUserData(((Integer) it2.next()).intValue());
        }
    }

    private void handlePasswordExpirationNotification(int i) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("android.intent.extra.USER", UserHandle.of(i));
        synchronized (getLockObject()) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, false);
            int size = activeAdminsForLockscreenPoliciesLocked.size();
            for (int i2 = 0; i2 < size; i2++) {
                ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i2);
                if (activeAdmin.info.usesPolicy(6) && activeAdmin.passwordExpirationTimeout > 0 && jCurrentTimeMillis >= activeAdmin.passwordExpirationDate - EXPIRATION_GRACE_PERIOD_MS && activeAdmin.passwordExpirationDate > 0) {
                    sendAdminCommandLocked(activeAdmin, "android.app.action.ACTION_PASSWORD_EXPIRING", bundle, (BroadcastReceiver) null);
                }
            }
            setExpirationAlarmCheckLocked(this.mContext, i, false);
        }
    }

    protected void onInstalledCertificatesChanged(UserHandle userHandle, Collection<String> collection) {
        if (!this.mHasFeature) {
            return;
        }
        enforceManageUsers();
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(userHandle.getIdentifier());
            if (userData.mOwnerInstalledCaCerts.retainAll(collection) | false | userData.mAcceptedCaCertificates.retainAll(collection)) {
                saveSettingsLocked(userHandle.getIdentifier());
            }
        }
    }

    protected Set<String> getAcceptedCaCertificates(UserHandle userHandle) {
        ArraySet<String> arraySet;
        if (!this.mHasFeature) {
            return Collections.emptySet();
        }
        synchronized (getLockObject()) {
            arraySet = getUserData(userHandle.getIdentifier()).mAcceptedCaCertificates;
        }
        return arraySet;
    }

    public void setActiveAdmin(ComponentName componentName, boolean z, int i) {
        if (!this.mHasFeature) {
            return;
        }
        setActiveAdmin(componentName, z, i, null);
    }

    private void setActiveAdmin(ComponentName componentName, boolean z, int i, Bundle bundle) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
        enforceFullCrossUsersPermission(i);
        DevicePolicyData userData = getUserData(i);
        DeviceAdminInfo deviceAdminInfoFindAdmin = findAdmin(componentName, i, true);
        synchronized (getLockObject()) {
            checkActiveAdminPrecondition(componentName, deviceAdminInfoFindAdmin, userData);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
                if (!z && activeAdminUncheckedLocked != null) {
                    throw new IllegalArgumentException("Admin is already added");
                }
                int i2 = 0;
                ActiveAdmin activeAdmin = new ActiveAdmin(deviceAdminInfoFindAdmin, false);
                activeAdmin.testOnlyAdmin = activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.testOnlyAdmin : isPackageTestOnly(componentName.getPackageName(), i);
                userData.mAdminMap.put(componentName, activeAdmin);
                int size = userData.mAdminList.size();
                while (true) {
                    if (i2 < size) {
                        if (userData.mAdminList.get(i2).info.getComponent().equals(componentName)) {
                            break;
                        } else {
                            i2++;
                        }
                    } else {
                        i2 = -1;
                        break;
                    }
                }
                if (i2 == -1) {
                    userData.mAdminList.add(activeAdmin);
                    enableIfNecessary(deviceAdminInfoFindAdmin.getPackageName(), i);
                    this.mUsageStatsManagerInternal.onActiveAdminAdded(componentName.getPackageName(), i);
                } else {
                    userData.mAdminList.set(i2, activeAdmin);
                }
                saveSettingsLocked(i);
                sendAdminCommandLocked(activeAdmin, "android.app.action.DEVICE_ADMIN_ENABLED", bundle, (BroadcastReceiver) null);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    private void loadAdminDataAsync() {
        this.mInjector.postOnSystemServerInitThreadPool(new Runnable() {
            @Override
            public final void run() {
                DevicePolicyManagerService.lambda$loadAdminDataAsync$0(this.f$0);
            }
        });
    }

    public static void lambda$loadAdminDataAsync$0(DevicePolicyManagerService devicePolicyManagerService) {
        devicePolicyManagerService.pushActiveAdminPackages();
        devicePolicyManagerService.mUsageStatsManagerInternal.onAdminDataAvailable();
        devicePolicyManagerService.pushAllMeteredRestrictedPackages();
        devicePolicyManagerService.mInjector.getNetworkPolicyManagerInternal().onAdminDataAvailable();
    }

    private void pushActiveAdminPackages() {
        synchronized (getLockObject()) {
            List users = this.mUserManager.getUsers();
            for (int size = users.size() - 1; size >= 0; size--) {
                int i = ((UserInfo) users.get(size)).id;
                this.mUsageStatsManagerInternal.setActiveAdminApps(getActiveAdminPackagesLocked(i), i);
            }
        }
    }

    private void pushAllMeteredRestrictedPackages() {
        synchronized (getLockObject()) {
            List users = this.mUserManager.getUsers();
            for (int size = users.size() - 1; size >= 0; size--) {
                int i = ((UserInfo) users.get(size)).id;
                this.mInjector.getNetworkPolicyManagerInternal().setMeteredRestrictedPackagesAsync(getMeteredDisabledPackagesLocked(i), i);
            }
        }
    }

    private void pushActiveAdminPackagesLocked(int i) {
        this.mUsageStatsManagerInternal.setActiveAdminApps(getActiveAdminPackagesLocked(i), i);
    }

    private Set<String> getActiveAdminPackagesLocked(int i) {
        DevicePolicyData userData = getUserData(i);
        ArraySet arraySet = null;
        for (int size = userData.mAdminList.size() - 1; size >= 0; size--) {
            String packageName = userData.mAdminList.get(size).info.getPackageName();
            if (arraySet == null) {
                arraySet = new ArraySet();
            }
            arraySet.add(packageName);
        }
        return arraySet;
    }

    private void transferActiveAdminUncheckedLocked(ComponentName componentName, ComponentName componentName2, int i) {
        DevicePolicyData userData = getUserData(i);
        if (!userData.mAdminMap.containsKey(componentName2) && userData.mAdminMap.containsKey(componentName)) {
            return;
        }
        DeviceAdminInfo deviceAdminInfoFindAdmin = findAdmin(componentName, i, true);
        ActiveAdmin activeAdmin = userData.mAdminMap.get(componentName2);
        int uid = activeAdmin.getUid();
        activeAdmin.transfer(deviceAdminInfoFindAdmin);
        userData.mAdminMap.remove(componentName2);
        userData.mAdminMap.put(componentName, activeAdmin);
        if (userData.mPasswordOwner == uid) {
            userData.mPasswordOwner = activeAdmin.getUid();
        }
        saveSettingsLocked(i);
        sendAdminCommandLocked(activeAdmin, "android.app.action.DEVICE_ADMIN_ENABLED", (Bundle) null, (BroadcastReceiver) null);
    }

    private void checkActiveAdminPrecondition(ComponentName componentName, DeviceAdminInfo deviceAdminInfo, DevicePolicyData devicePolicyData) {
        if (deviceAdminInfo == null) {
            throw new IllegalArgumentException("Bad admin: " + componentName);
        }
        if (!deviceAdminInfo.getActivityInfo().applicationInfo.isInternal()) {
            throw new IllegalArgumentException("Only apps in internal storage can be active admin: " + componentName);
        }
        if (deviceAdminInfo.getActivityInfo().applicationInfo.isInstantApp()) {
            throw new IllegalArgumentException("Instant apps cannot be device admins: " + componentName);
        }
        if (devicePolicyData.mRemovingAdmins.contains(componentName)) {
            throw new IllegalArgumentException("Trying to set an admin which is being removed");
        }
    }

    public boolean isAdminActive(ComponentName componentName, int i) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            z = getActiveAdminUncheckedLocked(componentName, i) != null;
        }
        return z;
    }

    public boolean isRemovingAdmin(ComponentName componentName, int i) {
        boolean zContains;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            zContains = getUserData(i).mRemovingAdmins.contains(componentName);
        }
        return zContains;
    }

    public boolean hasGrantedPolicy(ComponentName componentName, int i, int i2) {
        boolean zUsesPolicy;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(i2);
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i2);
            if (activeAdminUncheckedLocked == null) {
                throw new SecurityException("No active admin " + componentName);
            }
            zUsesPolicy = activeAdminUncheckedLocked.info.usesPolicy(i);
        }
        return zUsesPolicy;
    }

    public List<ComponentName> getActiveAdmins(int i) {
        if (!this.mHasFeature) {
            return Collections.EMPTY_LIST;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(i);
            int size = userData.mAdminList.size();
            if (size <= 0) {
                return null;
            }
            ArrayList arrayList = new ArrayList(size);
            for (int i2 = 0; i2 < size; i2++) {
                arrayList.add(userData.mAdminList.get(i2).info.getComponent());
            }
            return arrayList;
        }
    }

    public boolean packageHasActiveAdmins(String str, int i) {
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(i);
            int size = userData.mAdminList.size();
            for (int i2 = 0; i2 < size; i2++) {
                if (userData.mAdminList.get(i2).info.getPackageName().equals(str)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void forceRemoveActiveAdmin(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        enforceShell("forceRemoveActiveAdmin");
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                if (!isAdminTestOnlyLocked(componentName, i)) {
                    throw new SecurityException("Attempt to remove non-test admin " + componentName + " " + i);
                }
                if (isDeviceOwner(componentName, i)) {
                    clearDeviceOwnerLocked(getDeviceOwnerAdminLocked(), i);
                }
                if (isProfileOwner(componentName, i)) {
                    clearProfileOwnerLocked(getActiveAdminUncheckedLocked(componentName, i, false), i);
                }
            }
            removeAdminArtifacts(componentName, i);
            Slog.i(LOG_TAG, "Admin " + componentName + " removed from user " + i);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void clearDeviceOwnerUserRestrictionLocked(UserHandle userHandle) {
        if (this.mUserManager.hasUserRestriction("no_add_user", userHandle)) {
            this.mUserManager.setUserRestriction("no_add_user", false, userHandle);
        }
    }

    private boolean isPackageTestOnly(String str, int i) {
        try {
            ApplicationInfo applicationInfo = this.mInjector.getIPackageManager().getApplicationInfo(str, 786432, i);
            if (applicationInfo != null) {
                return (applicationInfo.flags & 256) != 0;
            }
            throw new IllegalStateException("Couldn't find package: " + str + " on user " + i);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isAdminTestOnlyLocked(ComponentName componentName, int i) {
        ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
        return activeAdminUncheckedLocked != null && activeAdminUncheckedLocked.testOnlyAdmin;
    }

    private void enforceShell(String str) {
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        if (iBinderGetCallingUid != 2000 && iBinderGetCallingUid != 0) {
            throw new SecurityException("Non-shell user attempted to call " + str);
        }
    }

    public void removeActiveAdmin(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(i);
        enforceUserUnlocked(i);
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                return;
            }
            if (!isDeviceOwner(componentName, i) && !isProfileOwner(componentName, i)) {
                if (activeAdminUncheckedLocked.getUid() != this.mInjector.binderGetCallingUid()) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
                }
                long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                try {
                    removeActiveAdminLocked(componentName, i);
                    return;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                }
            }
            Slog.e(LOG_TAG, "Device/profile owner cannot be removed: component=" + componentName);
        }
    }

    public boolean isSeparateProfileChallengeAllowed(int i) {
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Caller must be system");
        }
        ComponentName profileOwner = getProfileOwner(i);
        return profileOwner != null && getTargetSdk(profileOwner.getPackageName(), i) > 23;
    }

    public void setPasswordQuality(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        validateQualityConstant(i);
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics passwordMetrics = getActiveAdminForCallerLocked(componentName, 0, z).minimumPasswordMetrics;
            if (passwordMetrics.quality != i) {
                passwordMetrics.quality = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    private void updatePasswordValidityCheckpointLocked(int i, boolean z) {
        int credentialOwner = getCredentialOwner(i, z);
        DevicePolicyData userData = getUserData(credentialOwner);
        PasswordMetrics userPasswordMetricsLocked = getUserPasswordMetricsLocked(credentialOwner);
        if (userPasswordMetricsLocked == null) {
            userPasswordMetricsLocked = new PasswordMetrics();
        }
        userData.mPasswordValidAtLastCheckpoint = isPasswordSufficientForUserWithoutCheckpointLocked(userPasswordMetricsLocked, i, z);
        saveSettingsLocked(credentialOwner);
    }

    public int getPasswordQuality(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.minimumPasswordMetrics.quality : 0;
                }
                List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
                int size = activeAdminsForLockscreenPoliciesLocked.size();
                int i2 = 0;
                while (i < size) {
                    ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i);
                    if (i2 < activeAdmin.minimumPasswordMetrics.quality) {
                        i2 = activeAdmin.minimumPasswordMetrics.quality;
                    }
                    i++;
                }
                return i2;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private List<ActiveAdmin> getActiveAdminsForLockscreenPoliciesLocked(int i, boolean z) {
        if (!z && isSeparateProfileChallengeEnabled(i)) {
            return getUserDataUnchecked(i).mAdminList;
        }
        ArrayList arrayList = new ArrayList();
        for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
            DevicePolicyData userData = getUserData(userInfo.id);
            if (!userInfo.isManagedProfile()) {
                arrayList.addAll(userData.mAdminList);
            } else {
                boolean zIsSeparateProfileChallengeEnabled = isSeparateProfileChallengeEnabled(userInfo.id);
                int size = userData.mAdminList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    ActiveAdmin activeAdmin = userData.mAdminList.get(i2);
                    if (activeAdmin.hasParentActiveAdmin()) {
                        arrayList.add(activeAdmin.getParentActiveAdmin());
                    }
                    if (!zIsSeparateProfileChallengeEnabled) {
                        arrayList.add(activeAdmin);
                    }
                }
            }
        }
        return arrayList;
    }

    private boolean isSeparateProfileChallengeEnabled(int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void setPasswordMinimumLength(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics passwordMetrics = getActiveAdminForCallerLocked(componentName, 0, z).minimumPasswordMetrics;
            if (passwordMetrics.length != i) {
                passwordMetrics.length = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumLength(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.length);
            }
        }, 0);
    }

    public void setPasswordHistoryLength(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 0, z);
            if (activeAdminForCallerLocked.passwordHistoryLength != i) {
                activeAdminForCallerLocked.passwordHistoryLength = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210018, new Object[]{componentName.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), Integer.valueOf(z ? getProfileParentId(iUserHandleGetCallingUserId) : iUserHandleGetCallingUserId), Integer.valueOf(i)});
        }
    }

    public int getPasswordHistoryLength(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).passwordHistoryLength);
            }
        }, 0);
    }

    public void setPasswordExpirationTimeout(ComponentName componentName, long j, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkArgumentNonnegative(j, "Timeout must be >= 0 ms");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 6, z);
            long jCurrentTimeMillis = j > 0 ? System.currentTimeMillis() + j : 0L;
            activeAdminForCallerLocked.passwordExpirationDate = jCurrentTimeMillis;
            activeAdminForCallerLocked.passwordExpirationTimeout = j;
            if (j > 0) {
                Slog.w(LOG_TAG, "setPasswordExpiration(): password will expire on " + DateFormat.getDateTimeInstance(2, 2).format(new Date(jCurrentTimeMillis)));
            }
            saveSettingsLocked(iUserHandleGetCallingUserId);
            setExpirationAlarmCheckLocked(this.mContext, iUserHandleGetCallingUserId, z);
        }
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210016, new Object[]{componentName.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), Integer.valueOf(z ? getProfileParentId(iUserHandleGetCallingUserId) : iUserHandleGetCallingUserId), Long.valueOf(j)});
        }
    }

    public long getPasswordExpirationTimeout(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.passwordExpirationTimeout : 0L;
                }
                List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
                int size = activeAdminsForLockscreenPoliciesLocked.size();
                long j = 0;
                for (int i2 = 0; i2 < size; i2++) {
                    ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i2);
                    if (j == 0 || (activeAdmin.passwordExpirationTimeout != 0 && j > activeAdmin.passwordExpirationTimeout)) {
                        j = activeAdmin.passwordExpirationTimeout;
                    }
                }
                return j;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean addCrossProfileWidgetProvider(ComponentName componentName, String str) {
        ArrayList arrayList;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.crossProfileWidgetProviders == null) {
                activeAdminForCallerLocked.crossProfileWidgetProviders = new ArrayList();
            }
            List<String> list = activeAdminForCallerLocked.crossProfileWidgetProviders;
            if (!list.contains(str)) {
                list.add(str);
                arrayList = new ArrayList(list);
                saveSettingsLocked(callingUserId);
            } else {
                arrayList = null;
            }
        }
        if (arrayList == null) {
            return false;
        }
        this.mLocalService.notifyCrossProfileProvidersChanged(callingUserId, arrayList);
        return true;
    }

    public boolean removeCrossProfileWidgetProvider(ComponentName componentName, String str) {
        ArrayList arrayList;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.crossProfileWidgetProviders != null && !activeAdminForCallerLocked.crossProfileWidgetProviders.isEmpty()) {
                List<String> list = activeAdminForCallerLocked.crossProfileWidgetProviders;
                if (list.remove(str)) {
                    arrayList = new ArrayList(list);
                    saveSettingsLocked(callingUserId);
                } else {
                    arrayList = null;
                }
                if (arrayList == null) {
                    return false;
                }
                this.mLocalService.notifyCrossProfileProvidersChanged(callingUserId, arrayList);
                return true;
            }
            return false;
        }
    }

    public List<String> getCrossProfileWidgetProviders(ComponentName componentName) {
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.crossProfileWidgetProviders != null && !activeAdminForCallerLocked.crossProfileWidgetProviders.isEmpty()) {
                if (this.mInjector.binderIsCallingUidMyUid()) {
                    return new ArrayList(activeAdminForCallerLocked.crossProfileWidgetProviders);
                }
                return activeAdminForCallerLocked.crossProfileWidgetProviders;
            }
            return null;
        }
    }

    private long getPasswordExpirationLocked(ComponentName componentName, int i, boolean z) {
        if (componentName != null) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
            if (activeAdminUncheckedLocked != null) {
                return activeAdminUncheckedLocked.passwordExpirationDate;
            }
            return 0L;
        }
        List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
        int size = activeAdminsForLockscreenPoliciesLocked.size();
        long j = 0;
        for (int i2 = 0; i2 < size; i2++) {
            ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i2);
            if (j == 0 || (activeAdmin.passwordExpirationDate != 0 && j > activeAdmin.passwordExpirationDate)) {
                j = activeAdmin.passwordExpirationDate;
            }
        }
        return j;
    }

    public long getPasswordExpiration(ComponentName componentName, int i, boolean z) {
        long passwordExpirationLocked;
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            passwordExpirationLocked = getPasswordExpirationLocked(componentName, i, z);
        }
        return passwordExpirationLocked;
    }

    public void setPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics passwordMetrics = getActiveAdminForCallerLocked(componentName, 0, z).minimumPasswordMetrics;
            if (passwordMetrics.upperCase != i) {
                passwordMetrics.upperCase = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.upperCase);
            }
        }, 393216);
    }

    public void setPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics passwordMetrics = getActiveAdminForCallerLocked(componentName, 0, z).minimumPasswordMetrics;
            if (passwordMetrics.lowerCase != i) {
                passwordMetrics.lowerCase = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.lowerCase);
            }
        }, 393216);
    }

    public void setPasswordMinimumLetters(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics passwordMetrics = getActiveAdminForCallerLocked(componentName, 0, z).minimumPasswordMetrics;
            if (passwordMetrics.letters != i) {
                passwordMetrics.letters = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumLetters(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.letters);
            }
        }, 393216);
    }

    public void setPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            PasswordMetrics passwordMetrics = getActiveAdminForCallerLocked(componentName, 0, z).minimumPasswordMetrics;
            if (passwordMetrics.numeric != i) {
                passwordMetrics.numeric = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.numeric);
            }
        }, 393216);
    }

    public void setPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 0, z);
            PasswordMetrics passwordMetrics = activeAdminForCallerLocked.minimumPasswordMetrics;
            if (passwordMetrics.symbols != i) {
                activeAdminForCallerLocked.minimumPasswordMetrics.symbols = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.symbols);
            }
        }, 393216);
    }

    public void setPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 0, z);
            PasswordMetrics passwordMetrics = activeAdminForCallerLocked.minimumPasswordMetrics;
            if (passwordMetrics.nonLetter != i) {
                activeAdminForCallerLocked.minimumPasswordMetrics.nonLetter = i;
                updatePasswordValidityCheckpointLocked(iUserHandleGetCallingUserId, z);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
            maybeLogPasswordComplexitySet(componentName, iUserHandleGetCallingUserId, z, passwordMetrics);
        }
    }

    public int getPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) {
        return getStrictestPasswordRequirement(componentName, i, z, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DevicePolicyManagerService.ActiveAdmin) obj).minimumPasswordMetrics.nonLetter);
            }
        }, 393216);
    }

    private int getStrictestPasswordRequirement(ComponentName componentName, int i, boolean z, Function<ActiveAdmin, Integer> function, int i2) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                    return activeAdminUncheckedLocked != null ? function.apply(activeAdminUncheckedLocked).intValue() : 0;
                }
                List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
                int size = activeAdminsForLockscreenPoliciesLocked.size();
                int iIntValue = 0;
                while (i < size) {
                    ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i);
                    if (isLimitPasswordAllowed(activeAdmin, i2)) {
                        Integer numApply = function.apply(activeAdmin);
                        if (numApply.intValue() > iIntValue) {
                            iIntValue = numApply.intValue();
                        }
                    }
                    i++;
                }
                return iIntValue;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean isActivePasswordSufficient(int i, boolean z) {
        boolean zIsActivePasswordSufficientForUserLocked;
        if (!this.mHasFeature) {
            return true;
        }
        enforceFullCrossUsersPermission(i);
        enforceUserUnlocked(i, z);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(null, 0, z);
            int credentialOwner = getCredentialOwner(i, z);
            DevicePolicyData userDataUnchecked = getUserDataUnchecked(credentialOwner);
            zIsActivePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(userDataUnchecked.mPasswordValidAtLastCheckpoint, getUserPasswordMetricsLocked(credentialOwner), i, z);
        }
        return zIsActivePasswordSufficientForUserLocked;
    }

    @Override
    public boolean isUsingUnifiedPassword(ComponentName componentName) {
        if (!this.mHasFeature) {
            return true;
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        enforceProfileOrDeviceOwner(componentName);
        enforceManagedProfile(iUserHandleGetCallingUserId, "query unified challenge status");
        return !isSeparateProfileChallengeEnabled(iUserHandleGetCallingUserId);
    }

    public boolean isProfileActivePasswordSufficientForParent(int i) {
        boolean zIsActivePasswordSufficientForUserLocked;
        if (!this.mHasFeature) {
            return true;
        }
        enforceFullCrossUsersPermission(i);
        enforceManagedProfile(i, "call APIs refering to the parent profile");
        synchronized (getLockObject()) {
            int profileParentId = getProfileParentId(i);
            enforceUserUnlocked(profileParentId, false);
            int credentialOwner = getCredentialOwner(i, false);
            DevicePolicyData userDataUnchecked = getUserDataUnchecked(credentialOwner);
            zIsActivePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(userDataUnchecked.mPasswordValidAtLastCheckpoint, getUserPasswordMetricsLocked(credentialOwner), profileParentId, false);
        }
        return zIsActivePasswordSufficientForUserLocked;
    }

    private boolean isActivePasswordSufficientForUserLocked(boolean z, PasswordMetrics passwordMetrics, int i, boolean z2) {
        if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled() && passwordMetrics == null) {
            return z;
        }
        if (passwordMetrics == null) {
            passwordMetrics = new PasswordMetrics();
        }
        return isPasswordSufficientForUserWithoutCheckpointLocked(passwordMetrics, i, z2);
    }

    private boolean isPasswordSufficientForUserWithoutCheckpointLocked(PasswordMetrics passwordMetrics, int i, boolean z) {
        int passwordQuality = getPasswordQuality(null, i, z);
        if (passwordMetrics.quality < passwordQuality) {
            return false;
        }
        if (passwordQuality >= 131072 && passwordMetrics.length < getPasswordMinimumLength(null, i, z)) {
            return false;
        }
        if (passwordQuality != 393216) {
            return true;
        }
        return passwordMetrics.upperCase >= getPasswordMinimumUpperCase(null, i, z) && passwordMetrics.lowerCase >= getPasswordMinimumLowerCase(null, i, z) && passwordMetrics.letters >= getPasswordMinimumLetters(null, i, z) && passwordMetrics.numeric >= getPasswordMinimumNumeric(null, i, z) && passwordMetrics.symbols >= getPasswordMinimumSymbols(null, i, z) && passwordMetrics.nonLetter >= getPasswordMinimumNonLetter(null, i, z);
    }

    public int getCurrentFailedPasswordAttempts(int i, boolean z) {
        int i2;
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            if (!isCallerWithSystemUid()) {
                getActiveAdminForCallerLocked(null, 1, z);
            }
            i2 = getUserDataUnchecked(getCredentialOwner(i, z)).mFailedPasswordAttempts;
        }
        return i2;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, 4, z);
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 1, z);
            if (activeAdminForCallerLocked.maximumFailedPasswordsForWipe != i) {
                activeAdminForCallerLocked.maximumFailedPasswordsForWipe = i;
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210020, new Object[]{componentName.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), Integer.valueOf(z ? getProfileParentId(iUserHandleGetCallingUserId) : iUserHandleGetCallingUserId), Integer.valueOf(i)});
        }
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) {
        ActiveAdmin adminWithMinimumFailedPasswordsForWipeLocked;
        int i2;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    adminWithMinimumFailedPasswordsForWipeLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                } else {
                    adminWithMinimumFailedPasswordsForWipeLocked = getAdminWithMinimumFailedPasswordsForWipeLocked(i, z);
                }
                i2 = adminWithMinimumFailedPasswordsForWipeLocked != null ? adminWithMinimumFailedPasswordsForWipeLocked.maximumFailedPasswordsForWipe : 0;
            } catch (Throwable th) {
                throw th;
            }
        }
        return i2;
    }

    public int getProfileWithMinimumFailedPasswordsForWipe(int i, boolean z) {
        int identifier;
        if (!this.mHasFeature) {
            return -10000;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            ActiveAdmin adminWithMinimumFailedPasswordsForWipeLocked = getAdminWithMinimumFailedPasswordsForWipeLocked(i, z);
            identifier = adminWithMinimumFailedPasswordsForWipeLocked != null ? adminWithMinimumFailedPasswordsForWipeLocked.getUserHandle().getIdentifier() : -10000;
        }
        return identifier;
    }

    private ActiveAdmin getAdminWithMinimumFailedPasswordsForWipeLocked(int i, boolean z) {
        List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
        int size = activeAdminsForLockscreenPoliciesLocked.size();
        ActiveAdmin activeAdmin = null;
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            ActiveAdmin activeAdmin2 = activeAdminsForLockscreenPoliciesLocked.get(i3);
            if (activeAdmin2.maximumFailedPasswordsForWipe != 0) {
                int identifier = activeAdmin2.getUserHandle().getIdentifier();
                if (i2 == 0 || i2 > activeAdmin2.maximumFailedPasswordsForWipe || (i2 == activeAdmin2.maximumFailedPasswordsForWipe && getUserInfo(identifier).isPrimary())) {
                    i2 = activeAdmin2.maximumFailedPasswordsForWipe;
                    activeAdmin = activeAdmin2;
                }
            }
        }
        return activeAdmin;
    }

    private UserInfo getUserInfo(int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mUserManager.getUserInfo(i);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private boolean canPOorDOCallResetPassword(ActiveAdmin activeAdmin, int i) {
        return getTargetSdk(activeAdmin.info.getPackageName(), i) < 26;
    }

    private boolean canUserHaveUntrustedCredentialReset(int i) {
        synchronized (getLockObject()) {
            for (ActiveAdmin activeAdmin : getUserData(i).mAdminList) {
                if (isActiveAdminWithPolicyForUserLocked(activeAdmin, -1, i) && canPOorDOCallResetPassword(activeAdmin, i)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean resetPassword(String str, int i) throws RemoteException {
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String str2 = str;
        if (TextUtils.isEmpty(str2)) {
            enforceNotManagedProfile(iUserHandleGetCallingUserId, "clear the active password");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminWithPolicyForUidLocked = getActiveAdminWithPolicyForUidLocked(null, -1, iBinderGetCallingUid);
            boolean z = true;
            if (activeAdminWithPolicyForUidLocked != null) {
                if (!canPOorDOCallResetPassword(activeAdminWithPolicyForUidLocked, iUserHandleGetCallingUserId)) {
                    throw new SecurityException("resetPassword() is deprecated for DPC targeting O or later");
                }
                if (getTargetSdk(activeAdminWithPolicyForUidLocked.info.getPackageName(), iUserHandleGetCallingUserId) > 23) {
                    z = false;
                }
            } else {
                if (getTargetSdk(getActiveAdminForCallerLocked(null, 2).info.getPackageName(), iUserHandleGetCallingUserId) > 23) {
                    z = false;
                }
                if (TextUtils.isEmpty(str2)) {
                    if (!z) {
                        throw new SecurityException("Cannot call with null password");
                    }
                    Slog.e(LOG_TAG, "Cannot call with null password");
                    return false;
                }
                if (isLockScreenSecureUnchecked(iUserHandleGetCallingUserId)) {
                    if (!z) {
                        throw new SecurityException("Admin cannot change current password");
                    }
                    Slog.e(LOG_TAG, "Admin cannot change current password");
                    return false;
                }
            }
            if (!isManagedProfile(iUserHandleGetCallingUserId)) {
                Iterator it = this.mUserManager.getProfiles(iUserHandleGetCallingUserId).iterator();
                while (it.hasNext()) {
                    if (((UserInfo) it.next()).isManagedProfile()) {
                        if (!z) {
                            throw new IllegalStateException("Cannot reset password on user has managed profile");
                        }
                        Slog.e(LOG_TAG, "Cannot reset password on user has managed profile");
                        return false;
                    }
                }
            }
            if (!this.mUserManager.isUserUnlocked(iUserHandleGetCallingUserId)) {
                if (!z) {
                    throw new IllegalStateException("Cannot reset password when user is locked");
                }
                Slog.e(LOG_TAG, "Cannot reset password when user is locked");
                return false;
            }
            return resetPasswordInternal(str2, 0L, null, i, iBinderGetCallingUid, iUserHandleGetCallingUserId);
        }
    }

    private boolean resetPasswordInternal(String str, long j, byte[] bArr, int i, int i2, int i3) throws Throwable {
        long j2;
        int i4;
        long j3;
        boolean lockCredentialWithToken;
        synchronized (getLockObject()) {
            int passwordQuality = getPasswordQuality(null, i3, false);
            if (passwordQuality == 524288) {
                passwordQuality = 0;
            }
            PasswordMetrics passwordMetricsComputeForPassword = PasswordMetrics.computeForPassword(str);
            int i5 = passwordMetricsComputeForPassword.quality;
            if (i5 < passwordQuality && passwordQuality != 393216) {
                Slog.w(LOG_TAG, "resetPassword: password quality 0x" + Integer.toHexString(i5) + " does not meet required quality 0x" + Integer.toHexString(passwordQuality));
                return false;
            }
            int iMax = Math.max(i5, passwordQuality);
            int passwordMinimumLength = getPasswordMinimumLength(null, i3, false);
            if (str.length() < passwordMinimumLength) {
                Slog.w(LOG_TAG, "resetPassword: password length " + str.length() + " does not meet required length " + passwordMinimumLength);
                return false;
            }
            if (iMax == 393216) {
                int passwordMinimumLetters = getPasswordMinimumLetters(null, i3, false);
                if (passwordMetricsComputeForPassword.letters < passwordMinimumLetters) {
                    Slog.w(LOG_TAG, "resetPassword: number of letters " + passwordMetricsComputeForPassword.letters + " does not meet required number of letters " + passwordMinimumLetters);
                    return false;
                }
                int passwordMinimumNumeric = getPasswordMinimumNumeric(null, i3, false);
                if (passwordMetricsComputeForPassword.numeric < passwordMinimumNumeric) {
                    Slog.w(LOG_TAG, "resetPassword: number of numerical digits " + passwordMetricsComputeForPassword.numeric + " does not meet required number of numerical digits " + passwordMinimumNumeric);
                    return false;
                }
                int passwordMinimumLowerCase = getPasswordMinimumLowerCase(null, i3, false);
                if (passwordMetricsComputeForPassword.lowerCase < passwordMinimumLowerCase) {
                    Slog.w(LOG_TAG, "resetPassword: number of lowercase letters " + passwordMetricsComputeForPassword.lowerCase + " does not meet required number of lowercase letters " + passwordMinimumLowerCase);
                    return false;
                }
                int passwordMinimumUpperCase = getPasswordMinimumUpperCase(null, i3, false);
                if (passwordMetricsComputeForPassword.upperCase < passwordMinimumUpperCase) {
                    Slog.w(LOG_TAG, "resetPassword: number of uppercase letters " + passwordMetricsComputeForPassword.upperCase + " does not meet required number of uppercase letters " + passwordMinimumUpperCase);
                    return false;
                }
                int passwordMinimumSymbols = getPasswordMinimumSymbols(null, i3, false);
                if (passwordMetricsComputeForPassword.symbols < passwordMinimumSymbols) {
                    Slog.w(LOG_TAG, "resetPassword: number of special symbols " + passwordMetricsComputeForPassword.symbols + " does not meet required number of special symbols " + passwordMinimumSymbols);
                    return false;
                }
                int passwordMinimumNonLetter = getPasswordMinimumNonLetter(null, i3, false);
                if (passwordMetricsComputeForPassword.nonLetter < passwordMinimumNonLetter) {
                    Slog.w(LOG_TAG, "resetPassword: number of non-letter characters " + passwordMetricsComputeForPassword.nonLetter + " does not meet required number of non-letter characters " + passwordMinimumNonLetter);
                    return false;
                }
            }
            DevicePolicyData userData = getUserData(i3);
            if (userData.mPasswordOwner >= 0 && userData.mPasswordOwner != i2) {
                Slog.w(LOG_TAG, "resetPassword: already set by another uid and not entered by user");
                return false;
            }
            boolean zIsCallerDeviceOwner = isCallerDeviceOwner(i2);
            boolean z = true;
            boolean z2 = (i & 2) != 0;
            if (zIsCallerDeviceOwner && z2) {
                setDoNotAskCredentialsOnBoot();
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                if (bArr == null) {
                    if (TextUtils.isEmpty(str)) {
                        this.mLockPatternUtils.clearLock((String) null, i3);
                    } else {
                        this.mLockPatternUtils.saveLockPassword(str, (String) null, iMax, i3);
                    }
                    i4 = 2;
                    j3 = jBinderClearCallingIdentity;
                    lockCredentialWithToken = true;
                } else {
                    i4 = 2;
                    j3 = jBinderClearCallingIdentity;
                    try {
                        lockCredentialWithToken = this.mLockPatternUtils.setLockCredentialWithToken(str, TextUtils.isEmpty(str) ? -1 : 2, iMax, j, bArr, i3);
                    } catch (Throwable th) {
                        th = th;
                        this.mInjector.binderRestoreCallingIdentity(j2);
                        throw th;
                    }
                }
                if ((i & 1) == 0) {
                    z = false;
                }
                if (z) {
                    this.mLockPatternUtils.requireStrongAuth(i4, -1);
                }
                synchronized (getLockObject()) {
                    int i6 = z ? i2 : -1;
                    try {
                        if (userData.mPasswordOwner != i6) {
                            userData.mPasswordOwner = i6;
                            saveSettingsLocked(i3);
                        }
                    } finally {
                        th = th;
                        j2 = j3;
                        while (true) {
                            try {
                                try {
                                } catch (Throwable th2) {
                                    th = th2;
                                    this.mInjector.binderRestoreCallingIdentity(j2);
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                    }
                }
                this.mInjector.binderRestoreCallingIdentity(j3);
                return lockCredentialWithToken;
            } catch (Throwable th4) {
                th = th4;
                j2 = jBinderClearCallingIdentity;
            }
        }
    }

    private boolean isLockScreenSecureUnchecked(int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mLockPatternUtils.isSecure(i);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void setDoNotAskCredentialsOnBoot() {
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(0);
            if (!userData.doNotAskCredentialsOnBoot) {
                userData.doNotAskCredentialsOnBoot = true;
                saveSettingsLocked(0);
            }
        }
    }

    public boolean getDoNotAskCredentialsOnBoot() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.QUERY_DO_NOT_ASK_CREDENTIALS_ON_BOOT", null);
        synchronized (getLockObject()) {
            z = getUserData(0).doNotAskCredentialsOnBoot;
        }
        return z;
    }

    public void setMaximumTimeToLock(ComponentName componentName, long j, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 3, z);
            if (activeAdminForCallerLocked.maximumTimeToUnlock != j) {
                activeAdminForCallerLocked.maximumTimeToUnlock = j;
                saveSettingsLocked(iUserHandleGetCallingUserId);
                updateMaximumTimeToLockLocked(iUserHandleGetCallingUserId);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210019, new Object[]{componentName.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), Integer.valueOf(z ? getProfileParentId(iUserHandleGetCallingUserId) : iUserHandleGetCallingUserId), Long.valueOf(j)});
        }
    }

    private void updateMaximumTimeToLockLocked(int i) {
        if (isManagedProfile(i)) {
            updateProfileLockTimeoutLocked(i);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            int profileParentId = getProfileParentId(i);
            long maximumTimeToLockPolicyFromAdmins = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(profileParentId, false));
            DevicePolicyData userDataUnchecked = getUserDataUnchecked(profileParentId);
            if (userDataUnchecked.mLastMaximumTimeToLock == maximumTimeToLockPolicyFromAdmins) {
                return;
            }
            userDataUnchecked.mLastMaximumTimeToLock = maximumTimeToLockPolicyFromAdmins;
            if (userDataUnchecked.mLastMaximumTimeToLock != JobStatus.NO_LATEST_RUNTIME) {
                this.mInjector.settingsGlobalPutInt("stay_on_while_plugged_in", 0);
            }
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            this.mInjector.getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin(0, maximumTimeToLockPolicyFromAdmins);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void updateProfileLockTimeoutLocked(int i) {
        long maximumTimeToLockPolicyFromAdmins;
        if (isSeparateProfileChallengeEnabled(i)) {
            maximumTimeToLockPolicyFromAdmins = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(i, false));
        } else {
            maximumTimeToLockPolicyFromAdmins = JobStatus.NO_LATEST_RUNTIME;
        }
        DevicePolicyData userDataUnchecked = getUserDataUnchecked(i);
        if (userDataUnchecked.mLastMaximumTimeToLock == maximumTimeToLockPolicyFromAdmins) {
            return;
        }
        userDataUnchecked.mLastMaximumTimeToLock = maximumTimeToLockPolicyFromAdmins;
        this.mInjector.getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin(i, userDataUnchecked.mLastMaximumTimeToLock);
    }

    public long getMaximumTimeToLock(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.maximumTimeToUnlock : 0L;
                }
                long maximumTimeToLockPolicyFromAdmins = getMaximumTimeToLockPolicyFromAdmins(getActiveAdminsForLockscreenPoliciesLocked(i, z));
                if (maximumTimeToLockPolicyFromAdmins == JobStatus.NO_LATEST_RUNTIME) {
                    maximumTimeToLockPolicyFromAdmins = 0;
                }
                return maximumTimeToLockPolicyFromAdmins;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private long getMaximumTimeToLockPolicyFromAdmins(List<ActiveAdmin> list) {
        long j = JobStatus.NO_LATEST_RUNTIME;
        for (ActiveAdmin activeAdmin : list) {
            if (activeAdmin.maximumTimeToUnlock > 0 && activeAdmin.maximumTimeToUnlock < j) {
                j = activeAdmin.maximumTimeToUnlock;
            }
        }
        return j;
    }

    public void setRequiredStrongAuthTimeout(ComponentName componentName, long j, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkArgument(j >= 0, "Timeout must not be a negative number.");
        long minimumStrongAuthTimeoutMs = getMinimumStrongAuthTimeoutMs();
        if (j != 0 && j < minimumStrongAuthTimeoutMs) {
            j = minimumStrongAuthTimeoutMs;
        }
        if (j > 259200000) {
            j = 259200000;
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1, z);
            if (activeAdminForCallerLocked.strongAuthUnlockTimeout != j) {
                activeAdminForCallerLocked.strongAuthUnlockTimeout = j;
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
    }

    public long getRequiredStrongAuthTimeout(ComponentName componentName, int i, boolean z) {
        long jMin = 259200000;
        if (!this.mHasFeature) {
            return 259200000L;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.strongAuthUnlockTimeout : 0L;
                }
                List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
                for (int i2 = 0; i2 < activeAdminsForLockscreenPoliciesLocked.size(); i2++) {
                    long j = activeAdminsForLockscreenPoliciesLocked.get(i2).strongAuthUnlockTimeout;
                    if (j != 0) {
                        jMin = Math.min(j, jMin);
                    }
                }
                return Math.max(jMin, getMinimumStrongAuthTimeoutMs());
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private long getMinimumStrongAuthTimeoutMs() {
        if (!this.mInjector.isBuildDebuggable()) {
            return MINIMUM_STRONG_AUTH_TIMEOUT_MS;
        }
        return Math.min(this.mInjector.systemPropertiesGetLong("persist.sys.min_str_auth_timeo", MINIMUM_STRONG_AUTH_TIMEOUT_MS), MINIMUM_STRONG_AUTH_TIMEOUT_MS);
    }

    public void lockNow(int i, boolean z) {
        Injector injector;
        if (!this.mHasFeature) {
            return;
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(null, 3, z);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                ComponentName component = activeAdminForCallerLocked.info.getComponent();
                if ((i & 1) != 0) {
                    enforceManagedProfile(iUserHandleGetCallingUserId, "set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY");
                    if (!isProfileOwner(component, iUserHandleGetCallingUserId)) {
                        throw new SecurityException("Only profile owner admins can set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY");
                    }
                    if (z) {
                        throw new IllegalArgumentException("Cannot set FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY for the parent");
                    }
                    if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                        throw new UnsupportedOperationException("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY only applies to FBE devices");
                    }
                    this.mUserManager.evictCredentialEncryptionKey(iUserHandleGetCallingUserId);
                }
                int i2 = (z || !isSeparateProfileChallengeEnabled(iUserHandleGetCallingUserId)) ? -1 : iUserHandleGetCallingUserId;
                this.mLockPatternUtils.requireStrongAuth(2, i2);
                if (i2 == -1) {
                    this.mInjector.powerManagerGoToSleep(SystemClock.uptimeMillis(), 1, 0);
                    this.mInjector.getIWindowManager().lockNow((Bundle) null);
                } else {
                    this.mInjector.getTrustManager().setDeviceLockedForUser(i2, true);
                }
                if (SecurityLog.isLoggingEnabled()) {
                    SecurityLog.writeEvent(210022, new Object[]{component.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), Integer.valueOf(z ? getProfileParentId(iUserHandleGetCallingUserId) : iUserHandleGetCallingUserId)});
                }
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
            injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void enforceCanManageCaCerts(ComponentName componentName, String str) {
        if (componentName == null) {
            if (!isCallerDelegate(str, "delegation-cert-install")) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CA_CERTIFICATES", null);
                return;
            }
            return;
        }
        enforceProfileOrDeviceOwner(componentName);
    }

    private void enforceProfileOrDeviceOwner(ComponentName componentName) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
        }
    }

    public boolean approveCaCert(String str, int i, boolean z) {
        enforceManageUsers();
        synchronized (getLockObject()) {
            ArraySet<String> arraySet = getUserData(i).mAcceptedCaCertificates;
            if (!(z ? arraySet.add(str) : arraySet.remove(str))) {
                return false;
            }
            saveSettingsLocked(i);
            this.mCertificateMonitor.onCertificateApprovalsChanged(i);
            return true;
        }
    }

    public boolean isCaCertApproved(String str, int i) {
        boolean zContains;
        enforceManageUsers();
        synchronized (getLockObject()) {
            zContains = getUserData(i).mAcceptedCaCertificates.contains(str);
        }
        return zContains;
    }

    private void removeCaApprovalsIfNeeded(int i) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
            boolean zIsSecure = this.mLockPatternUtils.isSecure(userInfo.id);
            if (userInfo.isManagedProfile()) {
                zIsSecure |= this.mLockPatternUtils.isSecure(getProfileParentId(userInfo.id));
            }
            if (!zIsSecure) {
                synchronized (getLockObject()) {
                    getUserData(userInfo.id).mAcceptedCaCertificates.clear();
                    saveSettingsLocked(userInfo.id);
                }
                this.mCertificateMonitor.onCertificateApprovalsChanged(i);
            }
        }
    }

    public boolean installCaCert(ComponentName componentName, String str, byte[] bArr) throws RemoteException {
        if (!this.mHasFeature) {
            return false;
        }
        enforceCanManageCaCerts(componentName, str);
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            String strInstallCaCert = this.mCertificateMonitor.installCaCert(userHandleBinderGetCallingUserHandle, bArr);
            if (strInstallCaCert == null) {
                Log.w(LOG_TAG, "Problem installing cert");
                return false;
            }
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            synchronized (getLockObject()) {
                getUserData(userHandleBinderGetCallingUserHandle.getIdentifier()).mOwnerInstalledCaCerts.add(strInstallCaCert);
                saveSettingsLocked(userHandleBinderGetCallingUserHandle.getIdentifier());
            }
            return true;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void uninstallCaCerts(ComponentName componentName, String str, String[] strArr) {
        if (!this.mHasFeature) {
            return;
        }
        enforceCanManageCaCerts(componentName, str);
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mCertificateMonitor.uninstallCaCerts(UserHandle.of(iUserHandleGetCallingUserId), strArr);
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            synchronized (getLockObject()) {
                if (getUserData(iUserHandleGetCallingUserId).mOwnerInstalledCaCerts.removeAll(Arrays.asList(strArr))) {
                    saveSettingsLocked(iUserHandleGetCallingUserId);
                }
            }
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            throw th;
        }
    }

    public boolean installKeyPair(ComponentName componentName, String str, byte[] bArr, byte[] bArr2, byte[] bArr3, String str2, boolean z, boolean z2) {
        KeyChain.KeyChainConnection keyChainConnectionBindAsUser;
        enforceCanManageScope(componentName, str, -1, "delegation-cert-install");
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                keyChainConnectionBindAsUser = KeyChain.bindAsUser(this.mContext, UserHandle.getUserHandleForUid(iBinderGetCallingUid));
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "Interrupted while installing certificate", e);
                Thread.currentThread().interrupt();
            }
            try {
                try {
                    IKeyChainService service = keyChainConnectionBindAsUser.getService();
                    if (!service.installKeyPair(bArr, bArr2, bArr3, str2)) {
                        return false;
                    }
                    if (z) {
                        service.setGrant(iBinderGetCallingUid, str2, true);
                    }
                    service.setUserSelectable(str2, z2);
                    return true;
                } finally {
                    keyChainConnectionBindAsUser.close();
                }
            } catch (RemoteException e2) {
                Log.e(LOG_TAG, "Installing certificate", e2);
                keyChainConnectionBindAsUser.close();
                return false;
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public boolean removeKeyPair(ComponentName componentName, String str, String str2) {
        KeyChain.KeyChainConnection keyChainConnectionBindAsUser;
        enforceCanManageScope(componentName, str, -1, "delegation-cert-install");
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                keyChainConnectionBindAsUser = KeyChain.bindAsUser(this.mContext, userHandle);
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "Interrupted while removing keypair", e);
                Thread.currentThread().interrupt();
            }
            try {
                try {
                    return keyChainConnectionBindAsUser.getService().removeKeyPair(str2);
                } finally {
                    keyChainConnectionBindAsUser.close();
                }
            } catch (RemoteException e2) {
                Log.e(LOG_TAG, "Removing keypair", e2);
                keyChainConnectionBindAsUser.close();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void enforceIsDeviceOwnerOrCertInstallerOfDeviceOwner(ComponentName componentName, String str, int i) throws SecurityException {
        if (componentName == null) {
            if (!this.mOwners.hasDeviceOwner()) {
                throw new SecurityException("Not in Device Owner mode.");
            }
            if (UserHandle.getUserId(i) != this.mOwners.getDeviceOwnerUserId()) {
                throw new SecurityException("Caller not from device owner user");
            }
            if (!isCallerDelegate(str, "delegation-cert-install")) {
                throw new SecurityException("Caller with uid " + this.mInjector.binderGetCallingUid() + "has no permission to generate keys.");
            }
            return;
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
    }

    @VisibleForTesting
    public static int[] translateIdAttestationFlags(int i) {
        HashMap map = new HashMap();
        map.put(2, 1);
        map.put(4, 2);
        map.put(8, 3);
        int iBitCount = Integer.bitCount(i);
        if (iBitCount == 0) {
            return null;
        }
        if ((i & 1) != 0) {
            iBitCount--;
            i &= -2;
        }
        int[] iArr = new int[iBitCount];
        int i2 = 0;
        for (Integer num : map.keySet()) {
            if ((num.intValue() & i) != 0) {
                iArr[i2] = ((Integer) map.get(num)).intValue();
                i2++;
            }
        }
        return iArr;
    }

    @Override
    public boolean generateKeyPair(ComponentName componentName, String str, String str2, ParcelableKeyGenParameterSpec parcelableKeyGenParameterSpec, int i, KeymasterCertificateChain keymasterCertificateChain) {
        int iAttestKey;
        int[] iArrTranslateIdAttestationFlags = translateIdAttestationFlags(i);
        boolean z = iArrTranslateIdAttestationFlags != null;
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        if (!z || iArrTranslateIdAttestationFlags.length <= 0) {
            enforceCanManageScope(componentName, str, -1, "delegation-cert-install");
        } else {
            enforceIsDeviceOwnerOrCertInstallerOfDeviceOwner(componentName, str, iBinderGetCallingUid);
        }
        KeyGenParameterSpec spec = parcelableKeyGenParameterSpec.getSpec();
        String keystoreAlias = spec.getKeystoreAlias();
        if (TextUtils.isEmpty(keystoreAlias)) {
            throw new IllegalArgumentException("Empty alias provided.");
        }
        if (spec.getUid() != -1) {
            Log.e(LOG_TAG, "Only the caller can be granted access to the generated keypair.");
            return false;
        }
        if (z && spec.getAttestationChallenge() == null) {
            throw new IllegalArgumentException("Requested Device ID attestation but challenge is empty.");
        }
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                KeyChain.KeyChainConnection keyChainConnectionBindAsUser = KeyChain.bindAsUser(this.mContext, userHandleBinderGetCallingUserHandle);
                try {
                    IKeyChainService service = keyChainConnectionBindAsUser.getService();
                    int iGenerateKeyPair = service.generateKeyPair(str2, new ParcelableKeyGenParameterSpec(new KeyGenParameterSpec.Builder(spec).setAttestationChallenge(null).build()));
                    if (iGenerateKeyPair != 0) {
                        Log.e(LOG_TAG, String.format("KeyChain failed to generate a keypair, error %d.", Integer.valueOf(iGenerateKeyPair)));
                        return false;
                    }
                    service.setGrant(iBinderGetCallingUid, keystoreAlias, true);
                    byte[] attestationChallenge = spec.getAttestationChallenge();
                    if (attestationChallenge == null || (iAttestKey = service.attestKey(keystoreAlias, attestationChallenge, iArrTranslateIdAttestationFlags, keymasterCertificateChain)) == 0) {
                        if (keyChainConnectionBindAsUser != null) {
                            $closeResource(null, keyChainConnectionBindAsUser);
                        }
                        return true;
                    }
                    Log.e(LOG_TAG, String.format("Attestation for %s failed (rc=%d), deleting key.", keystoreAlias, Integer.valueOf(iAttestKey)));
                    service.removeKeyPair(keystoreAlias);
                    if (iAttestKey == 3) {
                        throw new UnsupportedOperationException("Device does not support Device ID attestation.");
                    }
                    if (keyChainConnectionBindAsUser != null) {
                        $closeResource(null, keyChainConnectionBindAsUser);
                    }
                    return false;
                } finally {
                    if (keyChainConnectionBindAsUser != null) {
                        $closeResource(null, keyChainConnectionBindAsUser);
                    }
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "KeyChain error while generating a keypair", e);
            return false;
        } catch (InterruptedException e2) {
            Log.w(LOG_TAG, "Interrupted while generating keypair", e2);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @Override
    public boolean setKeyPairCertificate(ComponentName componentName, String str, String str2, byte[] bArr, byte[] bArr2, boolean z) {
        enforceCanManageScope(componentName, str, -1, "delegation-cert-install");
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                KeyChain.KeyChainConnection keyChainConnectionBindAsUser = KeyChain.bindAsUser(this.mContext, UserHandle.getUserHandleForUid(iBinderGetCallingUid));
                try {
                    IKeyChainService service = keyChainConnectionBindAsUser.getService();
                    if (!service.setKeyPairCertificate(str2, bArr, bArr2)) {
                        return false;
                    }
                    service.setUserSelectable(str2, z);
                    if (keyChainConnectionBindAsUser != null) {
                        $closeResource(null, keyChainConnectionBindAsUser);
                    }
                    return true;
                } finally {
                    if (keyChainConnectionBindAsUser != null) {
                        $closeResource(null, keyChainConnectionBindAsUser);
                    }
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Failed setting keypair certificate", e);
                return false;
            } catch (InterruptedException e2) {
                Log.w(LOG_TAG, "Interrupted while setting keypair certificate", e2);
                Thread.currentThread().interrupt();
                return false;
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void choosePrivateKeyAlias(int i, Uri uri, String str, final IBinder iBinder) {
        if (!isCallerWithSystemUid()) {
            return;
        }
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        ComponentName profileOwner = getProfileOwner(userHandleBinderGetCallingUserHandle.getIdentifier());
        if (profileOwner == null && userHandleBinderGetCallingUserHandle.isSystem()) {
            synchronized (getLockObject()) {
                ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
                if (deviceOwnerAdminLocked != null) {
                    profileOwner = deviceOwnerAdminLocked.info.getComponent();
                }
            }
        }
        if (profileOwner == null) {
            sendPrivateKeyAliasResponse(null, iBinder);
            return;
        }
        Intent intent = new Intent("android.app.action.CHOOSE_PRIVATE_KEY_ALIAS");
        intent.setComponent(profileOwner);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_SENDER_UID", i);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_URI", uri);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_ALIAS", str);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_RESPONSE", iBinder);
        intent.addFlags(268435456);
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.sendOrderedBroadcastAsUser(intent, userHandleBinderGetCallingUserHandle, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent2) {
                    DevicePolicyManagerService.this.sendPrivateKeyAliasResponse(getResultData(), iBinder);
                }
            }, null, -1, null, null);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void sendPrivateKeyAliasResponse(String str, IBinder iBinder) {
        try {
            IKeyChainAliasCallback.Stub.asInterface(iBinder).alias(str);
        } catch (Exception e) {
            Log.e(LOG_TAG, "error while responding to callback", e);
        }
    }

    private static boolean shouldCheckIfDelegatePackageIsInstalled(String str, int i, List<String> list) {
        if (i >= 24) {
            return true;
        }
        return ((list.size() == 1 && list.get(0).equals("delegation-cert-install")) || list.isEmpty()) ? false : true;
    }

    public void setDelegatedScopes(ComponentName componentName, String str, List<String> list) throws SecurityException {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkStringNotEmpty(str, "Delegate package is null or empty");
        Preconditions.checkCollectionElementsNotNull(list, "Scopes");
        ArrayList<String> arrayList = new ArrayList<>(new ArraySet(list));
        if (arrayList.retainAll(Arrays.asList(DELEGATIONS))) {
            throw new IllegalArgumentException("Unexpected delegation scopes");
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            if (shouldCheckIfDelegatePackageIsInstalled(str, getTargetSdk(componentName.getPackageName(), iUserHandleGetCallingUserId), arrayList) && !isPackageInstalledForUser(str, iUserHandleGetCallingUserId)) {
                throw new IllegalArgumentException("Package " + str + " is not installed on the current user");
            }
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            if (!arrayList.isEmpty()) {
                userData.mDelegationMap.put(str, new ArrayList(arrayList));
            } else {
                userData.mDelegationMap.remove(str);
            }
            Intent intent = new Intent("android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED");
            intent.addFlags(1073741824);
            intent.setPackage(str);
            intent.putStringArrayListExtra("android.app.extra.DELEGATION_SCOPES", arrayList);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(iUserHandleGetCallingUserId));
            saveSettingsLocked(iUserHandleGetCallingUserId);
        }
    }

    public List<String> getDelegatedScopes(ComponentName componentName, String str) throws SecurityException {
        List<String> list;
        Preconditions.checkNotNull(str, "Delegate package is null");
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        int userId = UserHandle.getUserId(iBinderGetCallingUid);
        synchronized (getLockObject()) {
            if (componentName != null) {
                getActiveAdminForCallerLocked(componentName, -1);
            } else {
                int packageUidAsUser = 0;
                try {
                    packageUidAsUser = this.mInjector.getPackageManager().getPackageUidAsUser(str, userId);
                } catch (PackageManager.NameNotFoundException e) {
                }
                if (packageUidAsUser != iBinderGetCallingUid) {
                    throw new SecurityException("Caller with uid " + iBinderGetCallingUid + " is not " + str);
                }
            }
            list = getUserData(userId).mDelegationMap.get(str);
            if (list == null) {
                list = Collections.EMPTY_LIST;
            }
        }
        return list;
    }

    public List<String> getDelegatePackages(ComponentName componentName, String str) throws SecurityException {
        ArrayList arrayList;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkNotNull(str, "Scope is null");
        if (!Arrays.asList(DELEGATIONS).contains(str)) {
            throw new IllegalArgumentException("Unexpected delegation scope: " + str);
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            arrayList = new ArrayList();
            for (int i = 0; i < userData.mDelegationMap.size(); i++) {
                if (userData.mDelegationMap.valueAt(i).contains(str)) {
                    arrayList.add(userData.mDelegationMap.keyAt(i));
                }
            }
        }
        return arrayList;
    }

    private boolean isCallerDelegate(String str, String str2) {
        Preconditions.checkNotNull(str, "callerPackage is null");
        if (!Arrays.asList(DELEGATIONS).contains(str2)) {
            throw new IllegalArgumentException("Unexpected delegation scope: " + str2);
        }
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        int userId = UserHandle.getUserId(iBinderGetCallingUid);
        synchronized (getLockObject()) {
            List<String> list = getUserData(userId).mDelegationMap.get(str);
            if (list != null && list.contains(str2)) {
                try {
                    return this.mInjector.getPackageManager().getPackageUidAsUser(str, userId) == iBinderGetCallingUid;
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            return false;
        }
    }

    private void enforceCanManageScope(ComponentName componentName, String str, int i, String str2) {
        if (componentName != null) {
            synchronized (getLockObject()) {
                getActiveAdminForCallerLocked(componentName, i);
            }
        } else if (!isCallerDelegate(str, str2)) {
            throw new SecurityException("Caller with uid " + this.mInjector.binderGetCallingUid() + " is not a delegate of scope " + str2 + ".");
        }
    }

    private void setDelegatedScopePreO(ComponentName componentName, String str, String str2) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            if (str != null) {
                List<String> arrayList = userData.mDelegationMap.get(str);
                if (arrayList == null) {
                    arrayList = new ArrayList<>();
                }
                if (!arrayList.contains(str2)) {
                    arrayList.add(str2);
                    setDelegatedScopes(componentName, str, arrayList);
                }
            }
            for (int i = 0; i < userData.mDelegationMap.size(); i++) {
                String strKeyAt = userData.mDelegationMap.keyAt(i);
                List<String> listValueAt = userData.mDelegationMap.valueAt(i);
                if (!strKeyAt.equals(str) && listValueAt.contains(str2)) {
                    ArrayList arrayList2 = new ArrayList(listValueAt);
                    arrayList2.remove(str2);
                    setDelegatedScopes(componentName, strKeyAt, arrayList2);
                }
            }
        }
    }

    public void setCertInstallerPackage(ComponentName componentName, String str) throws SecurityException {
        setDelegatedScopePreO(componentName, str, "delegation-cert-install");
    }

    public String getCertInstallerPackage(ComponentName componentName) throws SecurityException {
        List<String> delegatePackages = getDelegatePackages(componentName, "delegation-cert-install");
        if (delegatePackages.size() > 0) {
            return delegatePackages.get(0);
        }
        return null;
    }

    public boolean setAlwaysOnVpnPackage(ComponentName componentName, String str, boolean z) throws SecurityException {
        enforceProfileOrDeviceOwner(componentName);
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        if (str != null) {
            try {
                if (!isPackageInstalledForUser(str, iUserHandleGetCallingUserId)) {
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        if (!((ConnectivityManager) this.mContext.getSystemService("connectivity")).setAlwaysOnVpnPackageForUser(iUserHandleGetCallingUserId, str, z)) {
            throw new UnsupportedOperationException();
        }
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        return true;
    }

    public String getAlwaysOnVpnPackage(ComponentName componentName) throws SecurityException {
        enforceProfileOrDeviceOwner(componentName);
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getAlwaysOnVpnPackageForUser(iUserHandleGetCallingUserId);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void forceWipeDeviceNoLock(boolean z, String str, boolean z2) {
        wtfIfInLock();
        if (z) {
            try {
                try {
                    ((StorageManager) this.mContext.getSystemService("storage")).wipeAdoptableDisks();
                } catch (IOException | SecurityException e) {
                    Slog.w(LOG_TAG, "Failed requesting data wipe", e);
                    SecurityLog.writeEvent(210023, new Object[0]);
                    return;
                }
            } catch (Throwable th) {
                SecurityLog.writeEvent(210023, new Object[0]);
                throw th;
            }
        }
        this.mInjector.recoverySystemRebootWipeUserData(false, str, true, z2);
    }

    private void forceWipeUser(int i, String str) throws Throwable {
        boolean zRemoveUserEvenWhenDisallowed;
        try {
            IActivityManager iActivityManager = this.mInjector.getIActivityManager();
            if (iActivityManager.getCurrentUser().id == i) {
                iActivityManager.switchUser(0);
            }
            zRemoveUserEvenWhenDisallowed = this.mUserManagerInternal.removeUserEvenWhenDisallowed(i);
            try {
                if (!zRemoveUserEvenWhenDisallowed) {
                    Slog.w(LOG_TAG, "Couldn't remove user " + i);
                } else if (isManagedProfile(i)) {
                    sendWipeProfileNotification(str);
                }
                if (zRemoveUserEvenWhenDisallowed) {
                    return;
                }
            } catch (RemoteException e) {
                if (zRemoveUserEvenWhenDisallowed) {
                    return;
                }
            } catch (Throwable th) {
                th = th;
                if (!zRemoveUserEvenWhenDisallowed) {
                    SecurityLog.writeEvent(210023, new Object[0]);
                }
                throw th;
            }
        } catch (RemoteException e2) {
            zRemoveUserEvenWhenDisallowed = false;
        } catch (Throwable th2) {
            th = th2;
            zRemoveUserEvenWhenDisallowed = false;
        }
        SecurityLog.writeEvent(210023, new Object[0]);
    }

    public void wipeDataWithReason(int i, String str) {
        ActiveAdmin activeAdminForCallerLocked;
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkStringNotEmpty(str, "wipeReasonForUser is null or empty");
        enforceFullCrossUsersPermission(this.mInjector.userHandleGetCallingUserId());
        synchronized (getLockObject()) {
            activeAdminForCallerLocked = getActiveAdminForCallerLocked(null, 4);
        }
        wipeDataNoLock(activeAdminForCallerLocked.info.getComponent(), i, "DevicePolicyManager.wipeDataWithReason() from " + activeAdminForCallerLocked.info.getComponent().flattenToShortString(), str, activeAdminForCallerLocked.getUserHandle().getIdentifier());
    }

    private void wipeDataNoLock(ComponentName componentName, int i, String str, String str2, int i2) {
        String str3;
        wtfIfInLock();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        if (i2 == 0) {
            str3 = "no_factory_reset";
        } else {
            try {
                if (isManagedProfile(i2)) {
                    str3 = "no_remove_managed_profile";
                } else {
                    str3 = "no_remove_user";
                }
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
        if (isAdminAffectedByRestriction(componentName, str3, i2)) {
            throw new SecurityException("Cannot wipe data. " + str3 + " restriction is set for user " + i2);
        }
        if ((i & 2) != 0) {
            if (!isDeviceOwner(componentName, i2)) {
                throw new SecurityException("Only device owner admins can set WIPE_RESET_PROTECTION_DATA");
            }
            PersistentDataBlockManager persistentDataBlockManager = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
            if (persistentDataBlockManager != null) {
                persistentDataBlockManager.wipe();
            }
        }
        if (i2 == 0) {
            forceWipeDeviceNoLock((i & 1) != 0, str, (i & 4) != 0);
        } else {
            forceWipeUser(i2, str2);
        }
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
    }

    private void sendWipeProfileNotification(String str) {
        this.mInjector.getNotificationManager().notify(NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE, new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(R.drawable.stat_sys_warning).setContentTitle(this.mContext.getString(R.string.notification_ranker_binding_label)).setContentText(str).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setStyle(new Notification.BigTextStyle().bigText(str)).build());
    }

    private void clearWipeProfileNotification() {
        this.mInjector.getNotificationManager().cancel(NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE);
    }

    public void getRemoveWarning(ComponentName componentName, final RemoteCallback remoteCallback, int i) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                remoteCallback.sendResult((Bundle) null);
                return;
            }
            Intent intent = new Intent("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
            intent.setFlags(268435456);
            intent.setComponent(activeAdminUncheckedLocked.info.getComponent());
            this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(i), null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent2) {
                    remoteCallback.sendResult(getResultExtras(false));
                }
            }, null, -1, null, null);
        }
    }

    public void setActivePasswordState(PasswordMetrics passwordMetrics, int i) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (isManagedProfile(i) && !isSeparateProfileChallengeEnabled(i)) {
            passwordMetrics = new PasswordMetrics();
        }
        validateQualityConstant(passwordMetrics.quality);
        synchronized (getLockObject()) {
            this.mUserPasswordMetrics.put(i, passwordMetrics);
        }
    }

    public void reportPasswordChanged(int i) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(i);
        if (!isSeparateProfileChallengeEnabled(i)) {
            enforceNotManagedProfile(i, "set the active password");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        DevicePolicyData userData = getUserData(i);
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                userData.mFailedPasswordAttempts = 0;
                updatePasswordValidityCheckpointLocked(i, false);
                saveSettingsLocked(i);
                updatePasswordExpirationsLocked(i);
                setExpirationAlarmCheckLocked(this.mContext, i, false);
                sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_CHANGED", 0, i);
            }
            removeCaApprovalsIfNeeded(i);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void updatePasswordExpirationsLocked(int i) {
        ArraySet arraySet = new ArraySet();
        List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, false);
        int size = activeAdminsForLockscreenPoliciesLocked.size();
        for (int i2 = 0; i2 < size; i2++) {
            ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i2);
            if (activeAdmin.info.usesPolicy(6)) {
                arraySet.add(Integer.valueOf(activeAdmin.getUserHandle().getIdentifier()));
                long j = activeAdmin.passwordExpirationTimeout;
                activeAdmin.passwordExpirationDate = j > 0 ? System.currentTimeMillis() + j : 0L;
            }
        }
        Iterator it = arraySet.iterator();
        while (it.hasNext()) {
            saveSettingsLocked(((Integer) it.next()).intValue());
        }
    }

    public void reportFailedPasswordAttempt(int i) {
        boolean z;
        int i2;
        enforceFullCrossUsersPermission(i);
        if (!isSeparateProfileChallengeEnabled(i)) {
            enforceNotManagedProfile(i, "report failed password attempt if separate profile challenge is not in place");
        }
        ActiveAdmin adminWithMinimumFailedPasswordsForWipeLocked = null;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                DevicePolicyData userData = getUserData(i);
                userData.mFailedPasswordAttempts++;
                saveSettingsLocked(i);
                if (this.mHasFeature) {
                    adminWithMinimumFailedPasswordsForWipeLocked = getAdminWithMinimumFailedPasswordsForWipeLocked(i, false);
                    if (adminWithMinimumFailedPasswordsForWipeLocked != null) {
                        i2 = adminWithMinimumFailedPasswordsForWipeLocked.maximumFailedPasswordsForWipe;
                    } else {
                        i2 = 0;
                    }
                    z = i2 > 0 && userData.mFailedPasswordAttempts >= i2;
                    sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_FAILED", 1, i);
                } else {
                    z = false;
                }
            }
            if (z && adminWithMinimumFailedPasswordsForWipeLocked != null) {
                int identifier = adminWithMinimumFailedPasswordsForWipeLocked.getUserHandle().getIdentifier();
                Slog.i(LOG_TAG, "Max failed password attempts policy reached for admin: " + adminWithMinimumFailedPasswordsForWipeLocked.info.getComponent().flattenToShortString() + ". Calling wipeData for user " + identifier);
                try {
                    wipeDataNoLock(adminWithMinimumFailedPasswordsForWipeLocked.info.getComponent(), 0, "reportFailedPasswordAttempt()", this.mContext.getString(R.string.notification_title_abusive_bg_apps), identifier);
                } catch (SecurityException e) {
                    Slog.w(LOG_TAG, "Failed to wipe user " + identifier + " after max failed password attempts reached.", e);
                }
            }
            if (this.mInjector.securityLogIsLoggingEnabled()) {
                SecurityLog.writeEvent(210007, new Object[]{0, 1});
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void reportSuccessfulPasswordAttempt(int i) {
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(i);
            if (userData.mFailedPasswordAttempts != 0 || userData.mPasswordOwner >= 0) {
                long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                try {
                    userData.mFailedPasswordAttempts = 0;
                    userData.mPasswordOwner = -1;
                    saveSettingsLocked(i);
                    if (this.mHasFeature) {
                        sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_SUCCEEDED", 1, i);
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                }
            }
        }
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{1, 1});
        }
    }

    public void reportFailedFingerprintAttempt(int i) {
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{0, 0});
        }
    }

    public void reportSuccessfulFingerprintAttempt(int i) {
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210007, new Object[]{1, 0});
        }
    }

    public void reportKeyguardDismissed(int i) {
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210006, new Object[0]);
        }
    }

    public void reportKeyguardSecured(int i) {
        enforceFullCrossUsersPermission(i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (this.mInjector.securityLogIsLoggingEnabled()) {
            SecurityLog.writeEvent(210008, new Object[0]);
        }
    }

    public ComponentName setGlobalProxy(ComponentName componentName, String str, String str2) {
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            Preconditions.checkNotNull(componentName, "ComponentName is null");
            DevicePolicyData userData = getUserData(0);
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 5);
            for (ComponentName componentName2 : userData.mAdminMap.keySet()) {
                if (userData.mAdminMap.get(componentName2).specifiesGlobalProxy && !componentName2.equals(componentName)) {
                    return componentName2;
                }
            }
            if (UserHandle.getCallingUserId() != 0) {
                Slog.w(LOG_TAG, "Only the owner is allowed to set the global proxy. User " + UserHandle.getCallingUserId() + " is not permitted.");
                return null;
            }
            if (str == null) {
                activeAdminForCallerLocked.specifiesGlobalProxy = false;
                activeAdminForCallerLocked.globalProxySpec = null;
                activeAdminForCallerLocked.globalProxyExclusionList = null;
            } else {
                activeAdminForCallerLocked.specifiesGlobalProxy = true;
                activeAdminForCallerLocked.globalProxySpec = str;
                activeAdminForCallerLocked.globalProxyExclusionList = str2;
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                resetGlobalProxyLocked(userData);
                return null;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public ComponentName getGlobalProxyAdmin(int i) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(0);
            int size = userData.mAdminList.size();
            for (int i2 = 0; i2 < size; i2++) {
                ActiveAdmin activeAdmin = userData.mAdminList.get(i2);
                if (activeAdmin.specifiesGlobalProxy) {
                    return activeAdmin.info.getComponent();
                }
            }
            return null;
        }
    }

    public void setRecommendedGlobalProxy(ComponentName componentName, ProxyInfo proxyInfo) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            ((ConnectivityManager) this.mContext.getSystemService("connectivity")).setGlobalProxy(proxyInfo);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void resetGlobalProxyLocked(DevicePolicyData devicePolicyData) {
        int size = devicePolicyData.mAdminList.size();
        for (int i = 0; i < size; i++) {
            ActiveAdmin activeAdmin = devicePolicyData.mAdminList.get(i);
            if (activeAdmin.specifiesGlobalProxy) {
                saveGlobalProxyLocked(activeAdmin.globalProxySpec, activeAdmin.globalProxyExclusionList);
                return;
            }
        }
        saveGlobalProxyLocked(null, null);
    }

    private void saveGlobalProxyLocked(String str, String str2) {
        if (str2 == null) {
            str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        if (str == null) {
            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String[] strArrSplit = str.trim().split(":");
        int i = 8080;
        if (strArrSplit.length > 1) {
            try {
                i = Integer.parseInt(strArrSplit[1]);
            } catch (NumberFormatException e) {
            }
        }
        String strTrim = str2.trim();
        ProxyInfo proxyInfo = new ProxyInfo(strArrSplit[0], i, strTrim);
        if (!proxyInfo.isValid()) {
            Slog.e(LOG_TAG, "Invalid proxy properties, ignoring: " + proxyInfo.toString());
            return;
        }
        this.mInjector.settingsGlobalPutString("global_http_proxy_host", strArrSplit[0]);
        this.mInjector.settingsGlobalPutInt("global_http_proxy_port", i);
        this.mInjector.settingsGlobalPutString("global_http_proxy_exclusion_list", strTrim);
    }

    public int setStorageEncryption(ComponentName componentName, boolean z) {
        int i;
        if (!this.mHasFeature) {
            return 0;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            try {
                if (callingUserId != 0) {
                    Slog.w(LOG_TAG, "Only owner/system user is allowed to set storage encryption. User " + UserHandle.getCallingUserId() + " is not permitted.");
                    return 0;
                }
                ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 7);
                if (!isEncryptionSupported()) {
                    return 0;
                }
                if (activeAdminForCallerLocked.encryptionRequested != z) {
                    activeAdminForCallerLocked.encryptionRequested = z;
                    saveSettingsLocked(callingUserId);
                }
                DevicePolicyData userData = getUserData(0);
                int size = userData.mAdminList.size();
                boolean z2 = false;
                for (int i2 = 0; i2 < size; i2++) {
                    z2 |= userData.mAdminList.get(i2).encryptionRequested;
                }
                setEncryptionRequested(z2);
                if (z2) {
                    i = 3;
                } else {
                    i = 1;
                }
                return i;
            } finally {
            }
        }
    }

    public boolean getStorageEncryption(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.encryptionRequested : false;
                }
                DevicePolicyData userData = getUserData(i);
                int size = userData.mAdminList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    if (userData.mAdminList.get(i2).encryptionRequested) {
                        return true;
                    }
                }
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public int getStorageEncryptionStatus(String str, int i) {
        boolean z = this.mHasFeature;
        enforceFullCrossUsersPermission(i);
        ensureCallerPackage(str);
        try {
            boolean z2 = this.mIPackageManager.getApplicationInfo(str, 0, i).targetSdkVersion <= 23;
            int encryptionStatus = getEncryptionStatus();
            if (encryptionStatus == 5 && z2) {
                return 3;
            }
            return encryptionStatus;
        } catch (RemoteException e) {
            throw new SecurityException(e);
        }
    }

    private boolean isEncryptionSupported() {
        return getEncryptionStatus() != 0;
    }

    private int getEncryptionStatus() {
        if (this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
            return 5;
        }
        if (this.mInjector.storageManagerIsNonDefaultBlockEncrypted()) {
            return 3;
        }
        if (this.mInjector.storageManagerIsEncrypted()) {
            return 4;
        }
        if (this.mInjector.storageManagerIsEncryptable()) {
            return 1;
        }
        return 0;
    }

    private void setEncryptionRequested(boolean z) {
    }

    public void setScreenCaptureDisabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.disableScreenCapture != z) {
                activeAdminForCallerLocked.disableScreenCapture = z;
                saveSettingsLocked(callingUserId);
                updateScreenCaptureDisabled(callingUserId, z);
            }
        }
    }

    public boolean getScreenCaptureDisabled(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.disableScreenCapture : false;
                }
                DevicePolicyData userData = getUserData(i);
                int size = userData.mAdminList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    if (userData.mAdminList.get(i2).disableScreenCapture) {
                        return true;
                    }
                }
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void updateScreenCaptureDisabled(final int i, boolean z) {
        this.mPolicyCache.setScreenCaptureDisabled(i, z);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DevicePolicyManagerService.this.mInjector.getIWindowManager().refreshScreenCaptureDisabled(i);
                } catch (RemoteException e) {
                    Log.w(DevicePolicyManagerService.LOG_TAG, "Unable to notify WindowManager.", e);
                }
            }
        });
    }

    public void setAutoTimeRequired(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.requireAutoTime != z) {
                activeAdminForCallerLocked.requireAutoTime = z;
                saveSettingsLocked(callingUserId);
            }
        }
        if (z) {
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mInjector.settingsGlobalPutInt("auto_time", 1);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public boolean getAutoTimeRequired() {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
            if (deviceOwnerAdminLocked != null && deviceOwnerAdminLocked.requireAutoTime) {
                return true;
            }
            Iterator<Integer> it = this.mOwners.getProfileOwnerKeys().iterator();
            while (it.hasNext()) {
                ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(it.next().intValue());
                if (profileOwnerAdminLocked != null && profileOwnerAdminLocked.requireAutoTime) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setForceEphemeralUsers(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (z && !this.mInjector.userManagerIsSplitSystemUser()) {
            throw new UnsupportedOperationException("Cannot force ephemeral users on systems without split system user.");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -2);
            if (activeAdminForCallerLocked.forceEphemeralUsers != z) {
                activeAdminForCallerLocked.forceEphemeralUsers = z;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                this.mUserManagerInternal.setForceEphemeralUsers(z);
            } else {
                z = false;
            }
        }
        if (z) {
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mUserManagerInternal.removeAllUsers();
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public boolean getForceEphemeralUsers(ComponentName componentName) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(componentName, -2).forceEphemeralUsers;
        }
        return z;
    }

    private void ensureDeviceOwnerAndAllUsersAffiliated(ComponentName componentName) throws SecurityException {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            if (!areAllUsersAffiliatedWithDeviceLocked()) {
                throw new SecurityException("Not all users are affiliated.");
            }
        }
    }

    public boolean requestBugreport(ComponentName componentName) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        ensureDeviceOwnerAndAllUsersAffiliated(componentName);
        if (this.mRemoteBugreportServiceIsActive.get() || getDeviceOwnerRemoteBugreportUri() != null) {
            Slog.d(LOG_TAG, "Remote bugreport wasn't started because there's already one running.");
            return false;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(0);
            if (jCurrentTimeMillis > userData.mLastBugReportRequestTime) {
                userData.mLastBugReportRequestTime = jCurrentTimeMillis;
                saveSettingsLocked(0);
            }
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().requestBugReport(2);
            this.mRemoteBugreportServiceIsActive.set(true);
            this.mRemoteBugreportSharingAccepted.set(false);
            registerRemoteBugreportReceivers();
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 1), UserHandle.ALL);
            this.mHandler.postDelayed(this.mRemoteBugreportTimeoutRunnable, 600000L);
            return true;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to make remote calls to start bugreportremote service", e);
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    void sendDeviceOwnerCommand(String str, Bundle bundle) {
        int deviceOwnerUserId;
        ComponentName deviceOwnerComponent;
        synchronized (getLockObject()) {
            deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
            deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
        }
        sendActiveAdminCommand(str, bundle, deviceOwnerUserId, deviceOwnerComponent);
    }

    private void sendProfileOwnerCommand(String str, Bundle bundle, int i) {
        sendActiveAdminCommand(str, bundle, i, this.mOwners.getProfileOwnerComponent(i));
    }

    private void sendActiveAdminCommand(String str, Bundle bundle, int i, ComponentName componentName) {
        Intent intent = new Intent(str);
        intent.setComponent(componentName);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(i));
    }

    private void sendOwnerChangedBroadcast(String str, int i) {
        this.mContext.sendBroadcastAsUser(new Intent(str).addFlags(DumpState.DUMP_SERVICE_PERMISSIONS), UserHandle.of(i));
    }

    private String getDeviceOwnerRemoteBugreportUri() {
        String deviceOwnerRemoteBugreportUri;
        synchronized (getLockObject()) {
            deviceOwnerRemoteBugreportUri = this.mOwners.getDeviceOwnerRemoteBugreportUri();
        }
        return deviceOwnerRemoteBugreportUri;
    }

    private void setDeviceOwnerRemoteBugreportUriAndHash(String str, String str2) {
        synchronized (getLockObject()) {
            this.mOwners.setDeviceOwnerRemoteBugreportUriAndHash(str, str2);
        }
    }

    private void registerRemoteBugreportReceivers() {
        try {
            this.mContext.registerReceiver(this.mRemoteBugreportFinishedReceiver, new IntentFilter("android.intent.action.REMOTE_BUGREPORT_DISPATCH", "application/vnd.android.bugreport"));
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Slog.w(LOG_TAG, "Failed to set type application/vnd.android.bugreport", e);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED");
        intentFilter.addAction("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED");
        this.mContext.registerReceiver(this.mRemoteBugreportConsentReceiver, intentFilter);
    }

    private void onBugreportFinished(Intent intent) throws Throwable {
        String string;
        this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
        this.mRemoteBugreportServiceIsActive.set(false);
        Uri data = intent.getData();
        if (data != null) {
            string = data.toString();
        } else {
            string = null;
        }
        String stringExtra = intent.getStringExtra("android.intent.extra.REMOTE_BUGREPORT_HASH");
        if (this.mRemoteBugreportSharingAccepted.get()) {
            shareBugreportWithDeviceOwnerIfExists(string, stringExtra);
            this.mInjector.getNotificationManager().cancel(LOG_TAG, 678432343);
        } else {
            setDeviceOwnerRemoteBugreportUriAndHash(string, stringExtra);
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 3), UserHandle.ALL);
        }
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    private void onBugreportFailed() {
        this.mRemoteBugreportServiceIsActive.set(false);
        this.mInjector.systemPropertiesSet("ctl.stop", "bugreportremote");
        this.mRemoteBugreportSharingAccepted.set(false);
        setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        this.mInjector.getNotificationManager().cancel(LOG_TAG, 678432343);
        Bundle bundle = new Bundle();
        bundle.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 0);
        sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", bundle);
        this.mContext.unregisterReceiver(this.mRemoteBugreportConsentReceiver);
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    private void onBugreportSharingAccepted() throws Throwable {
        String deviceOwnerRemoteBugreportUri;
        String deviceOwnerRemoteBugreportHash;
        this.mRemoteBugreportSharingAccepted.set(true);
        synchronized (getLockObject()) {
            deviceOwnerRemoteBugreportUri = getDeviceOwnerRemoteBugreportUri();
            deviceOwnerRemoteBugreportHash = this.mOwners.getDeviceOwnerRemoteBugreportHash();
        }
        if (deviceOwnerRemoteBugreportUri != null) {
            shareBugreportWithDeviceOwnerIfExists(deviceOwnerRemoteBugreportUri, deviceOwnerRemoteBugreportHash);
        } else if (this.mRemoteBugreportServiceIsActive.get()) {
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 2), UserHandle.ALL);
        }
    }

    private void onBugreportSharingDeclined() {
        if (this.mRemoteBugreportServiceIsActive.get()) {
            this.mInjector.systemPropertiesSet("ctl.stop", "bugreportremote");
            this.mRemoteBugreportServiceIsActive.set(false);
            this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
            this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
        }
        this.mRemoteBugreportSharingAccepted.set(false);
        setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        sendDeviceOwnerCommand("android.app.action.BUGREPORT_SHARING_DECLINED", null);
    }

    private void shareBugreportWithDeviceOwnerIfExists(String str, String str2) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            try {
            } catch (Throwable th) {
                th = th;
                if (0 != 0) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e) {
                    }
                }
                this.mRemoteBugreportSharingAccepted.set(false);
                setDeviceOwnerRemoteBugreportUriAndHash(null, null);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            parcelFileDescriptorOpenFileDescriptor = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
            }
            this.mRemoteBugreportSharingAccepted.set(false);
            setDeviceOwnerRemoteBugreportUriAndHash(null, null);
            throw th;
        }
        if (str == null) {
            throw new FileNotFoundException();
        }
        Uri uri = Uri.parse(str);
        parcelFileDescriptorOpenFileDescriptor = this.mContext.getContentResolver().openFileDescriptor(uri, "r");
        try {
            synchronized (getLockObject()) {
                Intent intent = new Intent("android.app.action.BUGREPORT_SHARE");
                intent.setComponent(this.mOwners.getDeviceOwnerComponent());
                intent.setDataAndType(uri, "application/vnd.android.bugreport");
                intent.putExtra("android.app.extra.BUGREPORT_HASH", str2);
                intent.setFlags(1);
                ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).grantUriPermissionFromIntent(PowerHalManager.ROTATE_BOOST_TIME, this.mOwners.getDeviceOwnerComponent().getPackageName(), intent, this.mOwners.getDeviceOwnerUserId());
                this.mContext.sendBroadcastAsUser(intent, UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
            }
            if (parcelFileDescriptorOpenFileDescriptor != null) {
                try {
                    parcelFileDescriptorOpenFileDescriptor.close();
                } catch (IOException e3) {
                }
            }
        } catch (FileNotFoundException e4) {
            Bundle bundle = new Bundle();
            bundle.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 1);
            sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", bundle);
            if (parcelFileDescriptorOpenFileDescriptor != null) {
                try {
                    parcelFileDescriptorOpenFileDescriptor.close();
                } catch (IOException e5) {
                }
            }
        }
        this.mRemoteBugreportSharingAccepted.set(false);
        setDeviceOwnerRemoteBugreportUriAndHash(null, null);
    }

    public void setCameraDisabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 8);
            if (activeAdminForCallerLocked.disableCamera != z) {
                activeAdminForCallerLocked.disableCamera = z;
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
        pushUserRestrictions(iUserHandleGetCallingUserId);
    }

    public boolean getCameraDisabled(ComponentName componentName, int i) {
        return getCameraDisabled(componentName, i, true);
    }

    private boolean getCameraDisabled(ComponentName componentName, int i, boolean z) {
        ActiveAdmin deviceOwnerAdminLocked;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            try {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.disableCamera : false;
                }
                if (z && (deviceOwnerAdminLocked = getDeviceOwnerAdminLocked()) != null && deviceOwnerAdminLocked.disableCamera) {
                    return true;
                }
                DevicePolicyData userData = getUserData(i);
                int size = userData.mAdminList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    if (userData.mAdminList.get(i2).disableCamera) {
                        return true;
                    }
                }
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void setKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        if (isManagedProfile(iUserHandleGetCallingUserId)) {
            if (z) {
                i &= 432;
            } else {
                i &= PROFILE_KEYGUARD_FEATURES;
            }
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, 9, z);
            if (activeAdminForCallerLocked.disabledKeyguardFeatures != i) {
                activeAdminForCallerLocked.disabledKeyguardFeatures = i;
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210021, new Object[]{componentName.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), Integer.valueOf(z ? getProfileParentId(iUserHandleGetCallingUserId) : iUserHandleGetCallingUserId), Integer.valueOf(i)});
        }
    }

    public int getKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) {
        List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(i);
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                if (componentName != null) {
                    ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                    return activeAdminUncheckedLocked != null ? activeAdminUncheckedLocked.disabledKeyguardFeatures : 0;
                }
                if (!z && isManagedProfile(i)) {
                    activeAdminsForLockscreenPoliciesLocked = getUserDataUnchecked(i).mAdminList;
                } else {
                    activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
                }
                int size = activeAdminsForLockscreenPoliciesLocked.size();
                int i2 = 0;
                for (int i3 = 0; i3 < size; i3++) {
                    ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i3);
                    int identifier = activeAdmin.getUserHandle().getIdentifier();
                    if ((!z && identifier == i) || !isManagedProfile(identifier)) {
                        i2 |= activeAdmin.disabledKeyguardFeatures;
                    } else {
                        i2 |= activeAdmin.disabledKeyguardFeatures & 432;
                    }
                }
                return i2;
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void setKeepUninstalledPackages(ComponentName componentName, String str, List<String> list) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(list, "packageList is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -2, "delegation-keep-uninstalled-packages");
            getDeviceOwnerAdminLocked().keepUninstalledPackages = list;
            saveSettingsLocked(callingUserId);
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(list);
        }
    }

    public List<String> getKeepUninstalledPackages(ComponentName componentName, String str) {
        List<String> keepUninstalledPackagesLocked;
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -2, "delegation-keep-uninstalled-packages");
            keepUninstalledPackagesLocked = getKeepUninstalledPackagesLocked();
        }
        return keepUninstalledPackagesLocked;
    }

    private List<String> getKeepUninstalledPackagesLocked() {
        ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
        if (deviceOwnerAdminLocked != null) {
            return deviceOwnerAdminLocked.keepUninstalledPackages;
        }
        return null;
    }

    public boolean setDeviceOwner(ComponentName componentName, String str, int i) {
        Set<String> defaultEnabledForDeviceOwner;
        if (!this.mHasFeature) {
            return false;
        }
        if (componentName == null || !isPackageInstalledForUser(componentName.getPackageName(), i)) {
            throw new IllegalArgumentException("Invalid component " + componentName + " for device owner");
        }
        boolean zHasIncompatibleAccountsOrNonAdbNoLock = hasIncompatibleAccountsOrNonAdbNoLock(i, componentName);
        synchronized (getLockObject()) {
            enforceCanSetDeviceOwnerLocked(componentName, i, zHasIncompatibleAccountsOrNonAdbNoLock);
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null || getUserData(i).mRemovingAdmins.contains(componentName)) {
                throw new IllegalArgumentException("Not active admin: " + componentName);
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    if (this.mInjector.getIBackupManager() != null) {
                        this.mInjector.getIBackupManager().setBackupServiceActive(0, false);
                        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                        if (isAdb()) {
                            MetricsLogger.action(this.mContext, NetworkManagementService.NetdResponseCode.StrictCleartext, LOG_TAG_DEVICE_OWNER);
                        }
                        this.mOwners.setDeviceOwner(componentName, str, i);
                        this.mOwners.writeDeviceOwner();
                        updateDeviceOwnerLocked();
                        setDeviceOwnerSystemPropertyLocked();
                        defaultEnabledForDeviceOwner = UserRestrictionsUtils.getDefaultEnabledForDeviceOwner();
                        if (!defaultEnabledForDeviceOwner.isEmpty()) {
                            Iterator<String> it = defaultEnabledForDeviceOwner.iterator();
                            while (it.hasNext()) {
                                activeAdminUncheckedLocked.ensureUserRestrictions().putBoolean(it.next(), true);
                            }
                            activeAdminUncheckedLocked.defaultEnabledRestrictionsAlreadySet.addAll(defaultEnabledForDeviceOwner);
                            Slog.i(LOG_TAG, "Enabled the following restrictions by default: " + defaultEnabledForDeviceOwner);
                            saveUserRestrictionsLocked(i);
                        }
                        jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                        try {
                            sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", i);
                            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                            this.mDeviceAdminServiceController.startServiceForOwner(componentName.getPackageName(), i, "set-device-owner");
                            Slog.i(LOG_TAG, "Device owner set: " + componentName + " on user " + i);
                        } finally {
                        }
                    } else {
                        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                        if (isAdb()) {
                        }
                        this.mOwners.setDeviceOwner(componentName, str, i);
                        this.mOwners.writeDeviceOwner();
                        updateDeviceOwnerLocked();
                        setDeviceOwnerSystemPropertyLocked();
                        defaultEnabledForDeviceOwner = UserRestrictionsUtils.getDefaultEnabledForDeviceOwner();
                        if (!defaultEnabledForDeviceOwner.isEmpty()) {
                        }
                        jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                        sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", i);
                        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                        this.mDeviceAdminServiceController.startServiceForOwner(componentName.getPackageName(), i, "set-device-owner");
                        Slog.i(LOG_TAG, "Device owner set: " + componentName + " on user " + i);
                    }
                } finally {
                }
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed deactivating backup service.", e);
            }
        }
        return true;
    }

    public boolean hasDeviceOwner() {
        enforceDeviceOwnerOrManageUsers();
        return this.mOwners.hasDeviceOwner();
    }

    boolean isDeviceOwner(ActiveAdmin activeAdmin) {
        return isDeviceOwner(activeAdmin.info.getComponent(), activeAdmin.getUserHandle().getIdentifier());
    }

    public boolean isDeviceOwner(ComponentName componentName, int i) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == i && this.mOwners.getDeviceOwnerComponent().equals(componentName);
        }
        return z;
    }

    private boolean isDeviceOwnerPackage(String str, int i) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == i && this.mOwners.getDeviceOwnerPackageName().equals(str);
        }
        return z;
    }

    private boolean isProfileOwnerPackage(String str, int i) {
        boolean z;
        synchronized (getLockObject()) {
            z = this.mOwners.hasProfileOwner(i) && this.mOwners.getProfileOwnerPackage(i).equals(str);
        }
        return z;
    }

    public boolean isProfileOwner(ComponentName componentName, int i) {
        return componentName != null && componentName.equals(getProfileOwner(i));
    }

    public ComponentName getDeviceOwnerComponent(boolean z) {
        if (!this.mHasFeature) {
            return null;
        }
        if (!z) {
            enforceManageUsers();
        }
        synchronized (getLockObject()) {
            if (!this.mOwners.hasDeviceOwner()) {
                return null;
            }
            if (z && this.mInjector.userHandleGetCallingUserId() != this.mOwners.getDeviceOwnerUserId()) {
                return null;
            }
            return this.mOwners.getDeviceOwnerComponent();
        }
    }

    public int getDeviceOwnerUserId() {
        int deviceOwnerUserId;
        if (!this.mHasFeature) {
            return -10000;
        }
        enforceManageUsers();
        synchronized (getLockObject()) {
            deviceOwnerUserId = this.mOwners.hasDeviceOwner() ? this.mOwners.getDeviceOwnerUserId() : -10000;
        }
        return deviceOwnerUserId;
    }

    public String getDeviceOwnerName() {
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        synchronized (getLockObject()) {
            if (!this.mOwners.hasDeviceOwner()) {
                return null;
            }
            return getApplicationLabel(this.mOwners.getDeviceOwnerPackageName(), 0);
        }
    }

    @VisibleForTesting
    ActiveAdmin getDeviceOwnerAdminLocked() {
        ensureLocked();
        ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
        if (deviceOwnerComponent == null) {
            return null;
        }
        DevicePolicyData userData = getUserData(this.mOwners.getDeviceOwnerUserId());
        int size = userData.mAdminList.size();
        for (int i = 0; i < size; i++) {
            ActiveAdmin activeAdmin = userData.mAdminList.get(i);
            if (deviceOwnerComponent.equals(activeAdmin.info.getComponent())) {
                return activeAdmin;
            }
        }
        Slog.wtf(LOG_TAG, "Active admin for device owner not found. component=" + deviceOwnerComponent);
        return null;
    }

    public void clearDeviceOwner(String str) {
        Preconditions.checkNotNull(str, "packageName is null");
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        try {
            if (this.mInjector.getPackageManager().getPackageUidAsUser(str, UserHandle.getUserId(iBinderGetCallingUid)) != iBinderGetCallingUid) {
                throw new SecurityException("Invalid packageName");
            }
            synchronized (getLockObject()) {
                ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
                int deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
                if (!this.mOwners.hasDeviceOwner() || !deviceOwnerComponent.getPackageName().equals(str) || deviceOwnerUserId != UserHandle.getUserId(iBinderGetCallingUid)) {
                    throw new SecurityException("clearDeviceOwner can only be called by the device owner");
                }
                enforceUserUnlocked(deviceOwnerUserId);
                ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
                long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                try {
                    clearDeviceOwnerLocked(deviceOwnerAdminLocked, deviceOwnerUserId);
                    removeActiveAdminLocked(deviceOwnerComponent, deviceOwnerUserId);
                    sendOwnerChangedBroadcast("android.app.action.DEVICE_OWNER_CHANGED", deviceOwnerUserId);
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    Slog.i(LOG_TAG, "Device owner removed: " + deviceOwnerComponent);
                } catch (Throwable th) {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    throw th;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException(e);
        }
    }

    private void clearOverrideApnUnchecked() {
        setOverrideApnsEnabledUnchecked(false);
        List<ApnSetting> overrideApnsUnchecked = getOverrideApnsUnchecked();
        for (int i = 0; i < overrideApnsUnchecked.size(); i++) {
            removeOverrideApnUnchecked(overrideApnsUnchecked.get(i).getId());
        }
    }

    private void clearDeviceOwnerLocked(ActiveAdmin activeAdmin, int i) {
        this.mDeviceAdminServiceController.stopServiceForOwner(i, "clear-device-owner");
        if (activeAdmin != null) {
            activeAdmin.disableCamera = false;
            activeAdmin.userRestrictions = null;
            activeAdmin.defaultEnabledRestrictionsAlreadySet.clear();
            activeAdmin.forceEphemeralUsers = false;
            activeAdmin.isNetworkLoggingEnabled = false;
            this.mUserManagerInternal.setForceEphemeralUsers(activeAdmin.forceEphemeralUsers);
        }
        getUserData(i).mCurrentInputMethodSet = false;
        saveSettingsLocked(i);
        DevicePolicyData userData = getUserData(0);
        userData.mLastSecurityLogRetrievalTime = -1L;
        userData.mLastBugReportRequestTime = -1L;
        userData.mLastNetworkLogsRetrievalTime = -1L;
        saveSettingsLocked(0);
        clearUserPoliciesLocked(i);
        clearOverrideApnUnchecked();
        this.mOwners.clearDeviceOwner();
        this.mOwners.writeDeviceOwner();
        updateDeviceOwnerLocked();
        clearDeviceOwnerUserRestrictionLocked(UserHandle.of(i));
        this.mInjector.securityLogSetLoggingEnabledProperty(false);
        this.mSecurityLogMonitor.stop();
        setNetworkLoggingActiveInternal(false);
        deleteTransferOwnershipBundleLocked(i);
        try {
            if (this.mInjector.getIBackupManager() != null) {
                this.mInjector.getIBackupManager().setBackupServiceActive(0, true);
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed reactivating backup service.", e);
        }
    }

    public boolean setProfileOwner(ComponentName componentName, String str, int i) {
        if (!this.mHasFeature) {
            return false;
        }
        if (componentName == null || !isPackageInstalledForUser(componentName.getPackageName(), i)) {
            throw new IllegalArgumentException("Component " + componentName + " not installed for userId:" + i);
        }
        boolean zHasIncompatibleAccountsOrNonAdbNoLock = hasIncompatibleAccountsOrNonAdbNoLock(i, componentName);
        synchronized (getLockObject()) {
            enforceCanSetProfileOwnerLocked(componentName, i, zHasIncompatibleAccountsOrNonAdbNoLock);
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null || getUserData(i).mRemovingAdmins.contains(componentName)) {
                throw new IllegalArgumentException("Not active admin: " + componentName);
            }
            if (isAdb()) {
                MetricsLogger.action(this.mContext, NetworkManagementService.NetdResponseCode.StrictCleartext, LOG_TAG_PROFILE_OWNER);
            }
            this.mOwners.setProfileOwner(componentName, str, i);
            this.mOwners.writeProfileOwner(i);
            Slog.i(LOG_TAG, "Profile owner set: " + componentName + " on user " + i);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                if (this.mUserManager.isManagedProfile(i)) {
                    maybeSetDefaultRestrictionsForAdminLocked(i, activeAdminUncheckedLocked, UserRestrictionsUtils.getDefaultEnabledForManagedProfiles());
                    ensureUnknownSourcesRestrictionForProfileOwnerLocked(i, activeAdminUncheckedLocked, true);
                }
                sendOwnerChangedBroadcast("android.app.action.PROFILE_OWNER_CHANGED", i);
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                this.mDeviceAdminServiceController.startServiceForOwner(componentName.getPackageName(), i, "set-profile-owner");
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
        return true;
    }

    public void clearProfileOwner(ComponentName componentName) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        enforceNotManagedProfile(iUserHandleGetCallingUserId, "clear profile owner");
        enforceUserUnlocked(iUserHandleGetCallingUserId);
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                clearProfileOwnerLocked(activeAdminForCallerLocked, iUserHandleGetCallingUserId);
                removeActiveAdminLocked(componentName, iUserHandleGetCallingUserId);
                sendOwnerChangedBroadcast("android.app.action.PROFILE_OWNER_CHANGED", iUserHandleGetCallingUserId);
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                Slog.i(LOG_TAG, "Profile owner " + componentName + " removed from user " + iUserHandleGetCallingUserId);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
    }

    public void clearProfileOwnerLocked(ActiveAdmin activeAdmin, int i) {
        this.mDeviceAdminServiceController.stopServiceForOwner(i, "clear-profile-owner");
        if (activeAdmin != null) {
            activeAdmin.disableCamera = false;
            activeAdmin.userRestrictions = null;
            activeAdmin.defaultEnabledRestrictionsAlreadySet.clear();
        }
        DevicePolicyData userData = getUserData(i);
        userData.mCurrentInputMethodSet = false;
        userData.mOwnerInstalledCaCerts.clear();
        saveSettingsLocked(i);
        clearUserPoliciesLocked(i);
        this.mOwners.removeProfileOwner(i);
        this.mOwners.writeProfileOwner(i);
        deleteTransferOwnershipBundleLocked(i);
    }

    public void setDeviceOwnerLockScreenInfo(ComponentName componentName, CharSequence charSequence) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (!this.mHasFeature) {
            return;
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mLockPatternUtils.setDeviceOwnerInfo(charSequence != null ? charSequence.toString() : null);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public CharSequence getDeviceOwnerLockScreenInfo() {
        return this.mLockPatternUtils.getDeviceOwnerInfo();
    }

    private void clearUserPoliciesLocked(int i) {
        DevicePolicyData userData = getUserData(i);
        userData.mPermissionPolicy = 0;
        userData.mDelegationMap.clear();
        userData.mStatusBarDisabled = false;
        userData.mUserProvisioningState = 0;
        userData.mAffiliationIds.clear();
        userData.mLockTaskPackages.clear();
        updateLockTaskPackagesLocked(userData.mLockTaskPackages, i);
        userData.mLockTaskFeatures = 0;
        saveSettingsLocked(i);
        try {
            this.mIPackageManager.updatePermissionFlagsForAllApps(4, 0, i);
            pushUserRestrictions(i);
        } catch (RemoteException e) {
        }
    }

    public boolean hasUserSetupCompleted() {
        return hasUserSetupCompleted(UserHandle.getCallingUserId());
    }

    private boolean hasUserSetupCompleted(int i) {
        if (!this.mHasFeature) {
            return true;
        }
        return getUserData(i).mUserSetupComplete;
    }

    private boolean hasPaired(int i) {
        if (!this.mHasFeature) {
            return true;
        }
        return getUserData(i).mPaired;
    }

    public int getUserProvisioningState() {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceManageUsers();
        return getUserProvisioningState(this.mInjector.userHandleGetCallingUserId());
    }

    private int getUserProvisioningState(int i) {
        return getUserData(i).mUserProvisioningState;
    }

    public void setUserProvisioningState(int i, int i2) {
        if (!this.mHasFeature) {
            return;
        }
        if (i2 != this.mOwners.getDeviceOwnerUserId() && !this.mOwners.hasProfileOwner(i2) && getManagedUserId(i2) == -1) {
            throw new IllegalStateException("Not allowed to change provisioning state unless a device or profile owner is set.");
        }
        synchronized (getLockObject()) {
            boolean z = true;
            if (isAdb()) {
                if (getUserProvisioningState(i2) != 0 || i != 3) {
                    throw new IllegalStateException("Not allowed to change provisioning state unless current provisioning state is unmanaged, and new state is finalized.");
                }
                z = false;
            } else {
                enforceCanManageProfileAndDeviceOwners();
            }
            DevicePolicyData userData = getUserData(i2);
            if (z) {
                checkUserProvisioningStateTransition(userData.mUserProvisioningState, i);
            }
            userData.mUserProvisioningState = i;
            saveSettingsLocked(i2);
        }
    }

    private void checkUserProvisioningStateTransition(int i, int i2) {
        if (i != 4) {
            switch (i) {
                case 0:
                    if (i2 != 0) {
                        return;
                    }
                    break;
                case 1:
                case 2:
                    if (i2 == 3) {
                        return;
                    }
                    break;
            }
        } else if (i2 == 0) {
            return;
        }
        throw new IllegalStateException("Cannot move to user provisioning state [" + i2 + "] from state [" + i + "]");
    }

    public void setProfileEnabled(ComponentName componentName) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            int callingUserId = UserHandle.getCallingUserId();
            enforceManagedProfile(callingUserId, "enable the profile");
            if (getUserInfo(callingUserId).isEnabled()) {
                Slog.e(LOG_TAG, "setProfileEnabled is called when the profile is already enabled");
                return;
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mUserManager.setUserEnabled(callingUserId);
                UserInfo profileParent = this.mUserManager.getProfileParent(callingUserId);
                Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_ADDED");
                intent.putExtra("android.intent.extra.USER", new UserHandle(callingUserId));
                intent.addFlags(1342177280);
                this.mContext.sendBroadcastAsUser(intent, new UserHandle(profileParent.id));
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public void setProfileName(ComponentName componentName, String str) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        getActiveAdminForCallerLocked(componentName, -1);
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManager.setUserName(callingUserId, str);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public ComponentName getProfileOwner(int i) {
        ComponentName profileOwnerComponent;
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            profileOwnerComponent = this.mOwners.getProfileOwnerComponent(i);
        }
        return profileOwnerComponent;
    }

    @VisibleForTesting
    ActiveAdmin getProfileOwnerAdminLocked(int i) {
        ComponentName profileOwnerComponent = this.mOwners.getProfileOwnerComponent(i);
        if (profileOwnerComponent == null) {
            return null;
        }
        DevicePolicyData userData = getUserData(i);
        int size = userData.mAdminList.size();
        for (int i2 = 0; i2 < size; i2++) {
            ActiveAdmin activeAdmin = userData.mAdminList.get(i2);
            if (profileOwnerComponent.equals(activeAdmin.info.getComponent())) {
                return activeAdmin;
            }
        }
        return null;
    }

    public String getProfileOwnerName(int i) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        ComponentName profileOwner = getProfileOwner(i);
        if (profileOwner == null) {
            return null;
        }
        return getApplicationLabel(profileOwner.getPackageName(), i);
    }

    private String getApplicationLabel(String str, int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            Context contextCreatePackageContextAsUser = this.mContext.createPackageContextAsUser(str, 0, new UserHandle(i));
            ApplicationInfo applicationInfo = contextCreatePackageContextAsUser.getApplicationInfo();
            CharSequence charSequenceLoadUnsafeLabel = applicationInfo != null ? applicationInfo.loadUnsafeLabel(contextCreatePackageContextAsUser.getPackageManager()) : null;
            return charSequenceLoadUnsafeLabel != null ? charSequenceLoadUnsafeLabel.toString() : null;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, str + " is not installed for user " + i, e);
            return null;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void wtfIfInLock() {
        if (Thread.holdsLock(this)) {
            Slog.wtfStack(LOG_TAG, "Shouldn't be called with DPMS lock held");
        }
    }

    private void enforceCanSetProfileOwnerLocked(ComponentName componentName, int i, boolean z) {
        UserInfo userInfo = getUserInfo(i);
        if (userInfo == null) {
            throw new IllegalArgumentException("Attempted to set profile owner for invalid userId: " + i);
        }
        if (userInfo.isGuest()) {
            throw new IllegalStateException("Cannot set a profile owner on a guest");
        }
        if (this.mOwners.hasProfileOwner(i)) {
            throw new IllegalStateException("Trying to set the profile owner, but profile owner is already set.");
        }
        if (this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == i) {
            throw new IllegalStateException("Trying to set the profile owner, but the user already has a device owner.");
        }
        if (isAdb()) {
            if ((this.mIsWatch || hasUserSetupCompleted(i)) && z) {
                throw new IllegalStateException("Not allowed to set the profile owner because there are already some accounts on the profile");
            }
            return;
        }
        enforceCanManageProfileAndDeviceOwners();
        if ((this.mIsWatch || hasUserSetupCompleted(i)) && !isCallerWithSystemUid()) {
            throw new IllegalStateException("Cannot set the profile owner on a user which is already set-up");
        }
    }

    private void enforceCanSetDeviceOwnerLocked(ComponentName componentName, int i, boolean z) {
        if (!isAdb()) {
            enforceCanManageProfileAndDeviceOwners();
        }
        int iCheckDeviceOwnerProvisioningPreConditionLocked = checkDeviceOwnerProvisioningPreConditionLocked(componentName, i, isAdb(), z);
        switch (iCheckDeviceOwnerProvisioningPreConditionLocked) {
            case 0:
                return;
            case 1:
                throw new IllegalStateException("Trying to set the device owner, but device owner is already set.");
            case 2:
                throw new IllegalStateException("Trying to set the device owner, but the user already has a profile owner.");
            case 3:
                throw new IllegalStateException("User not running: " + i);
            case 4:
                throw new IllegalStateException("Cannot set the device owner if the device is already set-up");
            case 5:
                throw new IllegalStateException("Not allowed to set the device owner because there are already several users on the device");
            case 6:
                throw new IllegalStateException("Not allowed to set the device owner because there are already some accounts on the device");
            case 7:
                throw new IllegalStateException("User is not system user");
            case 8:
                throw new IllegalStateException("Not allowed to set the device owner because this device has already paired");
            default:
                throw new IllegalStateException("Unexpected @ProvisioningPreCondition " + iCheckDeviceOwnerProvisioningPreConditionLocked);
        }
    }

    private void enforceUserUnlocked(int i) {
        Preconditions.checkState(this.mUserManager.isUserUnlocked(i), "User must be running and unlocked");
    }

    private void enforceUserUnlocked(int i, boolean z) {
        if (z) {
            enforceUserUnlocked(getProfileParentId(i));
        } else {
            enforceUserUnlocked(i);
        }
    }

    private void enforceManageUsers() {
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        if (!isCallerWithSystemUid() && iBinderGetCallingUid != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        }
    }

    private void enforceFullCrossUsersPermission(int i) {
        enforceSystemUserOrPermissionIfCrossUser(i, "android.permission.INTERACT_ACROSS_USERS_FULL");
    }

    private void enforceCrossUsersPermission(int i) {
        enforceSystemUserOrPermissionIfCrossUser(i, "android.permission.INTERACT_ACROSS_USERS");
    }

    private void enforceSystemUserOrPermission(String str) {
        if (!isCallerWithSystemUid() && this.mInjector.binderGetCallingUid() != 0) {
            this.mContext.enforceCallingOrSelfPermission(str, "Must be system or have " + str + " permission");
        }
    }

    private void enforceSystemUserOrPermissionIfCrossUser(int i, String str) {
        if (i < 0) {
            throw new IllegalArgumentException("Invalid userId " + i);
        }
        if (i == this.mInjector.userHandleGetCallingUserId()) {
            return;
        }
        enforceSystemUserOrPermission(str);
    }

    private void enforceManagedProfile(int i, String str) {
        if (!isManagedProfile(i)) {
            throw new SecurityException("You can not " + str + " outside a managed profile.");
        }
    }

    private void enforceNotManagedProfile(int i, String str) {
        if (isManagedProfile(i)) {
            throw new SecurityException("You can not " + str + " for a managed profile.");
        }
    }

    private void enforceDeviceOwnerOrManageUsers() {
        synchronized (getLockObject()) {
            if (getActiveAdminWithPolicyForUidLocked(null, -2, this.mInjector.binderGetCallingUid()) != null) {
                return;
            }
            enforceManageUsers();
        }
    }

    private void enforceProfileOwnerOrSystemUser() {
        synchronized (getLockObject()) {
            if (getActiveAdminWithPolicyForUidLocked(null, -1, this.mInjector.binderGetCallingUid()) != null) {
                return;
            }
            Preconditions.checkState(isCallerWithSystemUid(), "Only profile owner, device owner and system may call this method.");
        }
    }

    private void enforceProfileOwnerOrFullCrossUsersPermission(int i) {
        if (i == this.mInjector.userHandleGetCallingUserId()) {
            synchronized (getLockObject()) {
                if (getActiveAdminWithPolicyForUidLocked(null, -1, this.mInjector.binderGetCallingUid()) != null) {
                    return;
                }
            }
        }
        enforceSystemUserOrPermission("android.permission.INTERACT_ACROSS_USERS_FULL");
    }

    private boolean canUserUseLockTaskLocked(int i) {
        if (isUserAffiliatedWithDeviceLocked(i)) {
            return true;
        }
        return (this.mOwners.hasDeviceOwner() || getProfileOwner(i) == null || isManagedProfile(i)) ? false : true;
    }

    private void enforceCanCallLockTaskLocked(ComponentName componentName) {
        getActiveAdminForCallerLocked(componentName, -1);
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        if (!canUserUseLockTaskLocked(iUserHandleGetCallingUserId)) {
            throw new SecurityException("User " + iUserHandleGetCallingUserId + " is not allowed to use lock task");
        }
    }

    private void ensureCallerPackage(String str) {
        if (str == null) {
            Preconditions.checkState(isCallerWithSystemUid(), "Only caller can omit package name");
            return;
        }
        try {
            Preconditions.checkState(this.mIPackageManager.getApplicationInfo(str, 0, this.mInjector.userHandleGetCallingUserId()).uid == this.mInjector.binderGetCallingUid(), "Unmatching package name");
        } catch (RemoteException e) {
        }
    }

    private boolean isCallerWithSystemUid() {
        return UserHandle.isSameApp(this.mInjector.binderGetCallingUid(), 1000);
    }

    protected int getProfileParentId(int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo profileParent = this.mUserManager.getProfileParent(i);
            if (profileParent != null) {
                i = profileParent.id;
            }
            return i;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private int getCredentialOwner(int i, boolean z) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        if (z) {
            try {
                UserInfo profileParent = this.mUserManager.getProfileParent(i);
                if (profileParent != null) {
                    i = profileParent.id;
                }
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
        int credentialOwnerProfile = this.mUserManager.getCredentialOwnerProfile(i);
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        return credentialOwnerProfile;
    }

    private boolean isManagedProfile(int i) {
        UserInfo userInfo = getUserInfo(i);
        return userInfo != null && userInfo.isManagedProfile();
    }

    private void enableIfNecessary(String str, int i) {
        try {
            if (this.mIPackageManager.getApplicationInfo(str, 32768, i).enabledSetting == 4) {
                this.mIPackageManager.setApplicationEnabledSetting(str, 0, 1, i, LOG_TAG);
            }
        } catch (RemoteException e) {
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, printWriter)) {
            synchronized (getLockObject()) {
                printWriter.println("Current Device Policy Manager state:");
                this.mOwners.dump("  ", printWriter);
                this.mDeviceAdminServiceController.dump("  ", printWriter);
                int size = this.mUserData.size();
                for (int i = 0; i < size; i++) {
                    DevicePolicyData userData = getUserData(this.mUserData.keyAt(i));
                    printWriter.println();
                    printWriter.println("  Enabled Device Admins (User " + userData.mUserHandle + ", provisioningState: " + userData.mUserProvisioningState + "):");
                    int size2 = userData.mAdminList.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        ActiveAdmin activeAdmin = userData.mAdminList.get(i2);
                        if (activeAdmin != null) {
                            printWriter.print("    ");
                            printWriter.print(activeAdmin.info.getComponent().flattenToShortString());
                            printWriter.println(":");
                            activeAdmin.dump("      ", printWriter);
                        }
                    }
                    if (!userData.mRemovingAdmins.isEmpty()) {
                        printWriter.println("    Removing Device Admins (User " + userData.mUserHandle + "): " + userData.mRemovingAdmins);
                    }
                    printWriter.println(" ");
                    printWriter.print("    mPasswordOwner=");
                    printWriter.println(userData.mPasswordOwner);
                }
                printWriter.println();
                this.mConstants.dump("  ", printWriter);
                printWriter.println();
                this.mStatLogger.dump(printWriter, "  ");
                printWriter.println();
                printWriter.println("  Encryption Status: " + getEncryptionStatusName(getEncryptionStatus()));
            }
        }
    }

    private String getEncryptionStatusName(int i) {
        switch (i) {
            case 0:
                return "unsupported";
            case 1:
                return "inactive";
            case 2:
                return "activating";
            case 3:
                return "block";
            case 4:
                return "block default key";
            case 5:
                return "per-user";
            default:
                return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
    }

    public void addPersistentPreferredActivity(ComponentName componentName, IntentFilter intentFilter, ComponentName componentName2) {
        Injector injector;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.addPersistentPreferredActivity(intentFilter, componentName2, callingUserId);
                this.mIPackageManager.flushPackageRestrictionsAsUser(callingUserId);
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
            injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void clearPackagePersistentPreferredActivities(ComponentName componentName, String str) {
        Injector injector;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.clearPackagePersistentPreferredActivities(str, callingUserId);
                this.mIPackageManager.flushPackageRestrictionsAsUser(callingUserId);
                injector = this.mInjector;
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
            injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @Override
    public void setDefaultSmsApplication(ComponentName componentName, final String str) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
            public final void runOrThrow() {
                SmsApplication.setDefaultApplication(str, this.f$0.mContext);
            }
        });
    }

    public boolean setApplicationRestrictionsManagingPackage(ComponentName componentName, String str) {
        try {
            setDelegatedScopePreO(componentName, str, "delegation-app-restrictions");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getApplicationRestrictionsManagingPackage(ComponentName componentName) {
        List<String> delegatePackages = getDelegatePackages(componentName, "delegation-app-restrictions");
        if (delegatePackages.size() > 0) {
            return delegatePackages.get(0);
        }
        return null;
    }

    public boolean isCallerApplicationRestrictionsManagingPackage(String str) {
        return isCallerDelegate(str, "delegation-app-restrictions");
    }

    public void setApplicationRestrictions(ComponentName componentName, String str, String str2, Bundle bundle) {
        enforceCanManageScope(componentName, str, -1, "delegation-app-restrictions");
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManager.setApplicationRestrictions(str2, bundle, userHandleBinderGetCallingUserHandle);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void setTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "admin is null");
        Preconditions.checkNotNull(componentName2, "agent is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, 9, z).trustAgentInfos.put(componentName2.flattenToString(), new ActiveAdmin.TrustAgentInfo(persistableBundle));
            saveSettingsLocked(callingUserId);
        }
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, int i, boolean z) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName2, "agent null");
        enforceFullCrossUsersPermission(i);
        synchronized (getLockObject()) {
            String strFlattenToString = componentName2.flattenToString();
            if (componentName != null) {
                ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i, z);
                if (activeAdminUncheckedLocked == null) {
                    return null;
                }
                ActiveAdmin.TrustAgentInfo trustAgentInfo = activeAdminUncheckedLocked.trustAgentInfos.get(strFlattenToString);
                if (trustAgentInfo != null && trustAgentInfo.options != null) {
                    ArrayList arrayList = new ArrayList();
                    arrayList.add(trustAgentInfo.options);
                    return arrayList;
                }
                return null;
            }
            List<ActiveAdmin> activeAdminsForLockscreenPoliciesLocked = getActiveAdminsForLockscreenPoliciesLocked(i, z);
            int size = activeAdminsForLockscreenPoliciesLocked.size();
            boolean z2 = false;
            int i2 = 0;
            ArrayList arrayList2 = null;
            while (true) {
                boolean z3 = true;
                if (i2 < size) {
                    ActiveAdmin activeAdmin = activeAdminsForLockscreenPoliciesLocked.get(i2);
                    if ((activeAdmin.disabledKeyguardFeatures & 16) == 0) {
                        z3 = false;
                    }
                    ActiveAdmin.TrustAgentInfo trustAgentInfo2 = activeAdmin.trustAgentInfos.get(strFlattenToString);
                    if (trustAgentInfo2 != null && trustAgentInfo2.options != null && !trustAgentInfo2.options.isEmpty()) {
                        if (z3) {
                            if (arrayList2 == null) {
                                arrayList2 = new ArrayList();
                            }
                            arrayList2.add(trustAgentInfo2.options);
                        } else {
                            Log.w(LOG_TAG, "Ignoring admin " + activeAdmin.info + " because it has trust options but doesn't declare KEYGUARD_DISABLE_TRUST_AGENTS");
                        }
                    } else if (z3) {
                        break;
                    }
                    i2++;
                } else {
                    z2 = true;
                    break;
                }
            }
            return z2 ? arrayList2 : null;
        }
    }

    public void setRestrictionsProvider(ComponentName componentName, ComponentName componentName2) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            int callingUserId = UserHandle.getCallingUserId();
            getUserData(callingUserId).mRestrictionsProvider = componentName2;
            saveSettingsLocked(callingUserId);
        }
    }

    public ComponentName getRestrictionsProvider(int i) {
        ComponentName componentName;
        synchronized (getLockObject()) {
            if (!isCallerWithSystemUid()) {
                throw new SecurityException("Only the system can query the permission provider");
            }
            DevicePolicyData userData = getUserData(i);
            componentName = userData != null ? userData.mRestrictionsProvider : null;
        }
        return componentName;
    }

    public void addCrossProfileIntentFilter(ComponentName componentName, IntentFilter intentFilter, int i) {
        Injector injector;
        UserInfo profileParent;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                profileParent = this.mUserManager.getProfileParent(callingUserId);
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
            if (profileParent == null) {
                Slog.e(LOG_TAG, "Cannot call addCrossProfileIntentFilter if there is no parent");
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                return;
            }
            if ((i & 1) != 0) {
                this.mIPackageManager.addCrossProfileIntentFilter(intentFilter, componentName.getPackageName(), callingUserId, profileParent.id, 0);
            }
            if ((i & 2) != 0) {
                this.mIPackageManager.addCrossProfileIntentFilter(intentFilter, componentName.getPackageName(), profileParent.id, callingUserId, 0);
            }
            injector = this.mInjector;
            injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void clearCrossProfileIntentFilters(ComponentName componentName) {
        Injector injector;
        UserInfo profileParent;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                profileParent = this.mUserManager.getProfileParent(callingUserId);
            } catch (RemoteException e) {
                injector = this.mInjector;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
            if (profileParent == null) {
                Slog.e(LOG_TAG, "Cannot call clearCrossProfileIntentFilter if there is no parent");
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            } else {
                this.mIPackageManager.clearCrossProfileIntentFilters(callingUserId, componentName.getPackageName());
                this.mIPackageManager.clearCrossProfileIntentFilters(profileParent.id, componentName.getPackageName());
                injector = this.mInjector;
                injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    private boolean checkPackagesInPermittedListOrSystem(List<String> list, List<String> list2, int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo userInfo = getUserInfo(i);
            if (userInfo.isManagedProfile()) {
                i = userInfo.profileGroupId;
            }
            Iterator<String> it = list.iterator();
            while (true) {
                boolean z = true;
                if (!it.hasNext()) {
                    return true;
                }
                String next = it.next();
                try {
                    if ((this.mIPackageManager.getApplicationInfo(next, 8192, i).flags & 1) == 0) {
                        z = false;
                    }
                } catch (RemoteException e) {
                    Log.i(LOG_TAG, "Can't talk to package managed", e);
                    z = false;
                }
                if (!z && !list2.contains(next)) {
                    return false;
                }
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private AccessibilityManager getAccessibilityManagerForUser(int i) {
        IBinder service = ServiceManager.getService("accessibility");
        return new AccessibilityManager(this.mContext, service == null ? null : IAccessibilityManager.Stub.asInterface(service), i);
    }

    public boolean setPermittedAccessibilityServices(ComponentName componentName, List list) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (list != null) {
            int callingUserId = UserHandle.getCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                UserInfo userInfo = getUserInfo(callingUserId);
                if (userInfo.isManagedProfile()) {
                    callingUserId = userInfo.profileGroupId;
                }
                List<AccessibilityServiceInfo> enabledAccessibilityServiceList = getAccessibilityManagerForUser(callingUserId).getEnabledAccessibilityServiceList(-1);
                if (enabledAccessibilityServiceList != null) {
                    ArrayList arrayList = new ArrayList();
                    Iterator<AccessibilityServiceInfo> it = enabledAccessibilityServiceList.iterator();
                    while (it.hasNext()) {
                        arrayList.add(it.next().getResolveInfo().serviceInfo.packageName);
                    }
                    if (!checkPackagesInPermittedListOrSystem(arrayList, list, callingUserId)) {
                        Slog.e(LOG_TAG, "Cannot set permitted accessibility services, because it contains already enabled accesibility services.");
                        return false;
                    }
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1).permittedAccessiblityServices = list;
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
        return true;
    }

    public List getPermittedAccessibilityServices(ComponentName componentName) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            list = getActiveAdminForCallerLocked(componentName, -1).permittedAccessiblityServices;
        }
        return list;
    }

    public List getPermittedAccessibilityServicesForUser(int i) {
        ArrayList arrayList;
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        synchronized (getLockObject()) {
            int[] profileIdsWithDisabled = this.mUserManager.getProfileIdsWithDisabled(i);
            int length = profileIdsWithDisabled.length;
            arrayList = null;
            int i2 = 0;
            while (i2 < length) {
                DevicePolicyData userDataUnchecked = getUserDataUnchecked(profileIdsWithDisabled[i2]);
                int size = userDataUnchecked.mAdminList.size();
                ArrayList arrayList2 = arrayList;
                for (int i3 = 0; i3 < size; i3++) {
                    List<String> list = userDataUnchecked.mAdminList.get(i3).permittedAccessiblityServices;
                    if (list != null) {
                        if (arrayList2 == null) {
                            arrayList2 = new ArrayList(list);
                        } else {
                            arrayList2.retainAll(list);
                        }
                    }
                }
                i2++;
                arrayList = arrayList2;
            }
            if (arrayList != null) {
                long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                try {
                    UserInfo userInfo = getUserInfo(i);
                    if (userInfo.isManagedProfile()) {
                        i = userInfo.profileGroupId;
                    }
                    List<AccessibilityServiceInfo> installedAccessibilityServiceList = getAccessibilityManagerForUser(i).getInstalledAccessibilityServiceList();
                    if (installedAccessibilityServiceList != null) {
                        Iterator<AccessibilityServiceInfo> it = installedAccessibilityServiceList.iterator();
                        while (it.hasNext()) {
                            ServiceInfo serviceInfo = it.next().getResolveInfo().serviceInfo;
                            if ((serviceInfo.applicationInfo.flags & 1) != 0) {
                                arrayList.add(serviceInfo.packageName);
                            }
                        }
                    }
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                } catch (Throwable th) {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    throw th;
                }
            }
        }
        return arrayList;
    }

    public boolean isAccessibilityServicePermittedByAdmin(ComponentName componentName, String str, int i) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkStringNotEmpty(str, "packageName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query if an accessibility service is disabled by admin");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                return false;
            }
            if (activeAdminUncheckedLocked.permittedAccessiblityServices == null) {
                return true;
            }
            return checkPackagesInPermittedListOrSystem(Collections.singletonList(str), activeAdminUncheckedLocked.permittedAccessiblityServices, i);
        }
    }

    private boolean checkCallerIsCurrentUserOrProfile() {
        int callingUserId = UserHandle.getCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo userInfo = getUserInfo(callingUserId);
            UserInfo currentUser = this.mInjector.getIActivityManager().getCurrentUser();
            if (userInfo.isManagedProfile() && userInfo.profileGroupId != currentUser.id) {
                Slog.e(LOG_TAG, "Cannot set permitted input methods for managed profile of a user that isn't the foreground user.");
                return false;
            }
            if (userInfo.isManagedProfile() || callingUserId == currentUser.id) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                return true;
            }
            Slog.e(LOG_TAG, "Cannot set permitted input methods of a user that isn't the foreground user.");
            return false;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to talk to activity managed.", e);
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public boolean setPermittedInputMethods(ComponentName componentName, List list) {
        List<InputMethodInfo> enabledInputMethodList;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (!checkCallerIsCurrentUserOrProfile()) {
            return false;
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        if (list != null && (enabledInputMethodList = ((InputMethodManager) this.mContext.getSystemService(InputMethodManager.class)).getEnabledInputMethodList()) != null) {
            ArrayList arrayList = new ArrayList();
            Iterator<InputMethodInfo> it = enabledInputMethodList.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().getPackageName());
            }
            if (!checkPackagesInPermittedListOrSystem(arrayList, list, iUserHandleGetCallingUserId)) {
                Slog.e(LOG_TAG, "Cannot set permitted input methods, because it contains already enabled input method.");
                return false;
            }
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1).permittedInputMethods = list;
            saveSettingsLocked(iUserHandleGetCallingUserId);
        }
        return true;
    }

    public List getPermittedInputMethods(ComponentName componentName) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            list = getActiveAdminForCallerLocked(componentName, -1).permittedInputMethods;
        }
        return list;
    }

    public List getPermittedInputMethodsForCurrentUser() {
        ArrayList arrayList;
        enforceManageUsers();
        try {
            int i = this.mInjector.getIActivityManager().getCurrentUser().id;
            synchronized (getLockObject()) {
                int[] profileIdsWithDisabled = this.mUserManager.getProfileIdsWithDisabled(i);
                int length = profileIdsWithDisabled.length;
                arrayList = null;
                int i2 = 0;
                while (i2 < length) {
                    DevicePolicyData userDataUnchecked = getUserDataUnchecked(profileIdsWithDisabled[i2]);
                    int size = userDataUnchecked.mAdminList.size();
                    ArrayList arrayList2 = arrayList;
                    for (int i3 = 0; i3 < size; i3++) {
                        List<String> list = userDataUnchecked.mAdminList.get(i3).permittedInputMethods;
                        if (list != null) {
                            if (arrayList2 == null) {
                                arrayList2 = new ArrayList(list);
                            } else {
                                arrayList2.retainAll(list);
                            }
                        }
                    }
                    i2++;
                    arrayList = arrayList2;
                }
                if (arrayList != null) {
                    List<InputMethodInfo> inputMethodList = ((InputMethodManager) this.mContext.getSystemService(InputMethodManager.class)).getInputMethodList();
                    long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                    if (inputMethodList != null) {
                        try {
                            Iterator<InputMethodInfo> it = inputMethodList.iterator();
                            while (it.hasNext()) {
                                ServiceInfo serviceInfo = it.next().getServiceInfo();
                                if ((serviceInfo.applicationInfo.flags & 1) != 0) {
                                    arrayList.add(serviceInfo.packageName);
                                }
                            }
                        } finally {
                            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                        }
                    }
                }
            }
            return arrayList;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to make remote calls to get current user", e);
            return null;
        }
    }

    public boolean isInputMethodPermittedByAdmin(ComponentName componentName, String str, int i) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkStringNotEmpty(str, "packageName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query if an input method is disabled by admin");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                return false;
            }
            if (activeAdminUncheckedLocked.permittedInputMethods == null) {
                return true;
            }
            return checkPackagesInPermittedListOrSystem(Collections.singletonList(str), activeAdminUncheckedLocked.permittedInputMethods, i);
        }
    }

    public boolean setPermittedCrossProfileNotificationListeners(ComponentName componentName, List<String> list) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        if (!isManagedProfile(iUserHandleGetCallingUserId)) {
            return false;
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1).permittedNotificationListeners = list;
            saveSettingsLocked(iUserHandleGetCallingUserId);
        }
        return true;
    }

    public List<String> getPermittedCrossProfileNotificationListeners(ComponentName componentName) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            list = getActiveAdminForCallerLocked(componentName, -1).permittedNotificationListeners;
        }
        return list;
    }

    public boolean isNotificationListenerServicePermitted(String str, int i) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkStringNotEmpty(str, "packageName is null or empty");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query if a notification listener service is permitted");
        }
        synchronized (getLockObject()) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            if (profileOwnerAdminLocked != null && profileOwnerAdminLocked.permittedNotificationListeners != null) {
                return checkPackagesInPermittedListOrSystem(Collections.singletonList(str), profileOwnerAdminLocked.permittedNotificationListeners, i);
            }
            return true;
        }
    }

    private void maybeSendAdminEnabledBroadcastLocked(int i) {
        DevicePolicyData userData = getUserData(i);
        if (userData.mAdminBroadcastPending) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            boolean zSendAdminCommandLocked = true;
            if (profileOwnerAdminLocked != null) {
                PersistableBundle persistableBundle = userData.mInitBundle;
                zSendAdminCommandLocked = sendAdminCommandLocked(profileOwnerAdminLocked, "android.app.action.DEVICE_ADMIN_ENABLED", persistableBundle == null ? null : new Bundle(persistableBundle), null, true);
            }
            if (zSendAdminCommandLocked) {
                userData.mInitBundle = null;
                userData.mAdminBroadcastPending = false;
                saveSettingsLocked(i);
            }
        }
    }

    public UserHandle createAndManageUser(ComponentName componentName, String str, ComponentName componentName2, PersistableBundle persistableBundle, int i) throws ServiceSpecificException {
        Preconditions.checkNotNull(componentName, "admin is null");
        Preconditions.checkNotNull(componentName2, "profileOwner is null");
        if (!componentName.getPackageName().equals(componentName2.getPackageName())) {
            throw new IllegalArgumentException("profileOwner " + componentName2 + " and admin " + componentName + " are not in the same package");
        }
        if (!this.mInjector.binderGetCallingUserHandle().isSystem()) {
            throw new SecurityException("createAndManageUser was called from non-system user");
        }
        boolean z = (i & 2) != 0;
        boolean z2 = (i & 4) != 0 && UserManager.isDeviceInDemoMode(this.mContext);
        boolean z3 = (i & 16) != 0;
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                int uidTargetSdkVersion = this.mInjector.getPackageManagerInternal().getUidTargetSdkVersion(iBinderGetCallingUid);
                if (((DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class)).isMemoryLow()) {
                    if (uidTargetSdkVersion < 28) {
                        return null;
                    }
                    throw new ServiceSpecificException(5, "low device storage");
                }
                if (!this.mUserManager.canAddMoreUsers()) {
                    if (uidTargetSdkVersion < 28) {
                        return null;
                    }
                    throw new ServiceSpecificException(6, "user limit reached");
                }
                int i2 = z ? 256 : 0;
                if (z2) {
                    i2 |= 512;
                }
                UserInfo userInfoCreateUserEvenWhenDisallowed = this.mUserManagerInternal.createUserEvenWhenDisallowed(str, i2, !z3 ? (String[]) this.mOverlayPackagesProvider.getNonRequiredApps(componentName, UserHandle.myUserId(), "android.app.action.PROVISION_MANAGED_USER").toArray(new String[0]) : null);
                UserHandle userHandle = userInfoCreateUserEvenWhenDisallowed != null ? userInfoCreateUserEvenWhenDisallowed.getUserHandle() : null;
                if (userHandle == null) {
                    if (uidTargetSdkVersion < 28) {
                        return null;
                    }
                    throw new ServiceSpecificException(1, "failed to create user");
                }
                int identifier = userHandle.getIdentifier();
                this.mContext.sendBroadcastAsUser(new Intent("android.app.action.MANAGED_USER_CREATED").putExtra("android.intent.extra.user_handle", identifier).putExtra("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", z3).setPackage(getManagedProvisioningPackage(this.mContext)).addFlags(268435456), UserHandle.SYSTEM);
                jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                try {
                    String packageName = componentName.getPackageName();
                    try {
                        if (!this.mIPackageManager.isPackageAvailable(packageName, identifier)) {
                            this.mIPackageManager.installExistingPackageAsUser(packageName, identifier, 0, 1);
                        }
                    } catch (RemoteException e) {
                    }
                    setActiveAdmin(componentName2, true, identifier);
                    setProfileOwner(componentName2, getProfileOwnerName(Process.myUserHandle().getIdentifier()), identifier);
                    synchronized (getLockObject()) {
                        DevicePolicyData userData = getUserData(identifier);
                        userData.mInitBundle = persistableBundle;
                        userData.mAdminBroadcastPending = true;
                        saveSettingsLocked(identifier);
                    }
                    if ((i & 1) != 0) {
                        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, identifier);
                    }
                    return userHandle;
                } catch (Throwable th) {
                    this.mUserManager.removeUser(identifier);
                    if (uidTargetSdkVersion < 28) {
                        return null;
                    }
                    throw new ServiceSpecificException(1, th.getMessage());
                } finally {
                }
            } finally {
            }
        }
    }

    public boolean removeUser(ComponentName componentName, UserHandle userHandle) {
        String str;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkNotNull(userHandle, "UserHandle is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            if (isManagedProfile(userHandle.getIdentifier())) {
                str = "no_remove_managed_profile";
            } else {
                str = "no_remove_user";
            }
            if (isAdminAffectedByRestriction(componentName, str, iUserHandleGetCallingUserId)) {
                Log.w(LOG_TAG, "The device owner cannot remove a user because " + str + " is enabled, and was not set by the device owner");
                return false;
            }
            return this.mUserManagerInternal.removeUserEvenWhenDisallowed(userHandle.getIdentifier());
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private boolean isAdminAffectedByRestriction(ComponentName componentName, String str, int i) {
        int userRestrictionSource = this.mUserManager.getUserRestrictionSource(str, UserHandle.of(i));
        if (userRestrictionSource == 0) {
            return false;
        }
        if (userRestrictionSource == 2) {
            return !isDeviceOwner(componentName, i);
        }
        if (userRestrictionSource != 4) {
            return true;
        }
        return !isProfileOwner(componentName, i);
    }

    public boolean switchUser(ComponentName componentName, UserHandle userHandle) {
        int identifier;
        boolean zSwitchUser;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            if (userHandle != null) {
                try {
                    try {
                        identifier = userHandle.getIdentifier();
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, "Couldn't switch user", e);
                        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                        return false;
                    }
                } catch (Throwable th) {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    throw th;
                }
            } else {
                identifier = 0;
            }
            zSwitchUser = this.mInjector.getIActivityManager().switchUser(identifier);
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
        return zSwitchUser;
    }

    public int startUserInBackground(ComponentName componentName, UserHandle userHandle) {
        Injector injector;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkNotNull(userHandle, "UserHandle is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        int identifier = userHandle.getIdentifier();
        if (isManagedProfile(identifier)) {
            Log.w(LOG_TAG, "Managed profile cannot be started in background");
            return 2;
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            if (!this.mInjector.getActivityManagerInternal().canStartMoreUsers()) {
                Log.w(LOG_TAG, "Cannot start more users in background");
                return 3;
            }
            if (this.mInjector.getIActivityManager().startUserInBackground(identifier)) {
                return 0;
            }
            return 1;
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public int stopUser(ComponentName componentName, UserHandle userHandle) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkNotNull(userHandle, "UserHandle is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        int identifier = userHandle.getIdentifier();
        if (isManagedProfile(identifier)) {
            Log.w(LOG_TAG, "Managed profile cannot be stopped");
            return 2;
        }
        return stopUserUnchecked(identifier);
    }

    public int logoutUser(ComponentName componentName) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            if (!isUserAffiliatedWithDeviceLocked(iUserHandleGetCallingUserId)) {
                throw new SecurityException("Admin " + componentName + " is neither the device owner or affiliated user's profile owner.");
            }
        }
        if (isManagedProfile(iUserHandleGetCallingUserId)) {
            Log.w(LOG_TAG, "Managed profile cannot be logout");
            return 2;
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            if (this.mInjector.getIActivityManager().switchUser(0)) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                return stopUserUnchecked(iUserHandleGetCallingUserId);
            }
            Log.w(LOG_TAG, "Failed to switch to primary user");
            return 1;
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private int stopUserUnchecked(int i) {
        Injector injector;
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            int iStopUser = this.mInjector.getIActivityManager().stopUser(i, true, (IStopUserCallback) null);
            if (iStopUser == -2) {
                return 4;
            }
            if (iStopUser != 0) {
                return 1;
            }
            return 0;
        } catch (RemoteException e) {
            return 1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public List<UserHandle> getSecondaryUsers(ComponentName componentName) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            List<UserInfo> users = this.mInjector.getUserManager().getUsers(true);
            ArrayList arrayList = new ArrayList();
            for (UserInfo userInfo : users) {
                UserHandle userHandle = userInfo.getUserHandle();
                if (!userHandle.isSystem() && !isManagedProfile(userHandle.getIdentifier())) {
                    arrayList.add(userInfo.getUserHandle());
                }
            }
            return arrayList;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public boolean isEphemeralUser(ComponentName componentName) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mInjector.getUserManager().isUserEphemeral(iUserHandleGetCallingUserId);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public Bundle getApplicationRestrictions(ComponentName componentName, String str, String str2) {
        enforceCanManageScope(componentName, str, -1, "delegation-app-restrictions");
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            Bundle applicationRestrictions = this.mUserManager.getApplicationRestrictions(str2, userHandleBinderGetCallingUserHandle);
            if (applicationRestrictions == null) {
                applicationRestrictions = Bundle.EMPTY;
            }
            return applicationRestrictions;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public String[] setPackagesSuspended(ComponentName componentName, String str, String[] strArr, boolean z) {
        String[] packagesSuspendedAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-package-access");
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    packagesSuspendedAsUser = this.mIPackageManager.setPackagesSuspendedAsUser(strArr, z, (PersistableBundle) null, (PersistableBundle) null, (String) null, PackageManagerService.PLATFORM_PACKAGE_NAME, callingUserId);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                }
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Failed talking to the package manager", e);
                return strArr;
            }
        }
        return packagesSuspendedAsUser;
    }

    public boolean isPackageSuspended(ComponentName componentName, String str, String str2) {
        boolean zIsPackageSuspendedForUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-package-access");
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    zIsPackageSuspendedForUser = this.mIPackageManager.isPackageSuspendedForUser(str2, callingUserId);
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed talking to the package manager", e);
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return zIsPackageSuspendedForUser;
    }

    public void setUserRestriction(ComponentName componentName, String str, boolean z) {
        int i;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (!UserRestrictionsUtils.isValidRestriction(str)) {
            return;
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (isDeviceOwner(componentName, iUserHandleGetCallingUserId)) {
                if (!UserRestrictionsUtils.canDeviceOwnerChange(str)) {
                    throw new SecurityException("Device owner cannot set user restriction " + str);
                }
            } else if (!UserRestrictionsUtils.canProfileOwnerChange(str, iUserHandleGetCallingUserId)) {
                throw new SecurityException("Profile owner cannot set user restriction " + str);
            }
            Bundle bundleEnsureUserRestrictions = activeAdminForCallerLocked.ensureUserRestrictions();
            if (z) {
                bundleEnsureUserRestrictions.putBoolean(str, true);
            } else {
                bundleEnsureUserRestrictions.remove(str);
            }
            saveUserRestrictionsLocked(iUserHandleGetCallingUserId);
        }
        if (SecurityLog.isLoggingEnabled()) {
            if (z) {
                i = 210027;
            } else {
                i = 210028;
            }
            SecurityLog.writeEvent(i, new Object[]{componentName.getPackageName(), Integer.valueOf(iUserHandleGetCallingUserId), str});
        }
    }

    private void saveUserRestrictionsLocked(int i) {
        saveSettingsLocked(i);
        pushUserRestrictions(i);
        sendChangedNotification(i);
    }

    private void pushUserRestrictions(int i) {
        Bundle bundle;
        synchronized (getLockObject()) {
            boolean zIsDeviceOwnerUserId = this.mOwners.isDeviceOwnerUserId(i);
            boolean z = false;
            if (zIsDeviceOwnerUserId) {
                ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
                if (deviceOwnerAdminLocked == null) {
                    return;
                }
                bundle = deviceOwnerAdminLocked.userRestrictions;
                z = deviceOwnerAdminLocked.disableCamera;
            } else {
                ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
                bundle = profileOwnerAdminLocked != null ? profileOwnerAdminLocked.userRestrictions : null;
            }
            this.mUserManagerInternal.setDevicePolicyUserRestrictions(i, bundle, zIsDeviceOwnerUserId, getCameraRestrictionScopeLocked(i, z));
        }
    }

    private int getCameraRestrictionScopeLocked(int i, boolean z) {
        if (z) {
            return 2;
        }
        return getCameraDisabled(null, i, false) ? 1 : 0;
    }

    public Bundle getUserRestrictions(ComponentName componentName) {
        Bundle bundle;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            bundle = getActiveAdminForCallerLocked(componentName, -1).userRestrictions;
        }
        return bundle;
    }

    public boolean setApplicationHidden(ComponentName componentName, String str, String str2, boolean z) {
        boolean applicationHiddenSettingAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-package-access");
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    applicationHiddenSettingAsUser = this.mIPackageManager.setApplicationHiddenSettingAsUser(str2, z, callingUserId);
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed to setApplicationHiddenSetting", e);
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public boolean isApplicationHidden(ComponentName componentName, String str, String str2) {
        boolean applicationHiddenSettingAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-package-access");
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    applicationHiddenSettingAsUser = this.mIPackageManager.getApplicationHiddenSettingAsUser(str2, callingUserId);
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed to getApplicationHiddenSettingAsUser", e);
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public void enableSystemApp(ComponentName componentName, String str, String str2) {
        Injector injector;
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-enable-system-app");
            boolean zIsCurrentUserDemo = isCurrentUserDemo();
            int callingUserId = UserHandle.getCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    int profileParentId = getProfileParentId(callingUserId);
                    if (!zIsCurrentUserDemo && !isSystemApp(this.mIPackageManager, str2, profileParentId)) {
                        throw new IllegalArgumentException("Only system apps can be enabled this way.");
                    }
                    this.mIPackageManager.installExistingPackageAsUser(str2, callingUserId, 0, 1);
                    if (zIsCurrentUserDemo) {
                        this.mIPackageManager.setApplicationEnabledSetting(str2, 1, 1, callingUserId, LOG_TAG);
                    }
                    injector = this.mInjector;
                } catch (RemoteException e) {
                    Slog.wtf(LOG_TAG, "Failed to install " + str2, e);
                    injector = this.mInjector;
                }
                injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
    }

    public int enableSystemAppWithIntent(ComponentName componentName, String str, Intent intent) {
        int i;
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-enable-system-app");
            int callingUserId = UserHandle.getCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            i = 0;
            try {
                int profileParentId = getProfileParentId(callingUserId);
                List<ResolveInfo> list = this.mIPackageManager.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, profileParentId).getList();
                if (list != null) {
                    int i2 = 0;
                    for (ResolveInfo resolveInfo : list) {
                        if (resolveInfo.activityInfo != null) {
                            String str2 = resolveInfo.activityInfo.packageName;
                            if (isSystemApp(this.mIPackageManager, str2, profileParentId)) {
                                i2++;
                                this.mIPackageManager.installExistingPackageAsUser(str2, callingUserId, 0, 1);
                            } else {
                                Slog.d(LOG_TAG, "Not enabling " + str2 + " since is not a system app");
                            }
                        }
                    }
                    i = i2;
                }
            } catch (RemoteException e) {
                Slog.wtf(LOG_TAG, "Failed to resolve intent for: " + intent);
                return 0;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return i;
    }

    private boolean isSystemApp(IPackageManager iPackageManager, String str, int i) throws RemoteException {
        ApplicationInfo applicationInfo = iPackageManager.getApplicationInfo(str, 8192, i);
        if (applicationInfo != null) {
            return (applicationInfo.flags & 1) != 0;
        }
        throw new IllegalArgumentException("The application " + str + " is not present on this device");
    }

    public boolean installExistingPackage(ComponentName componentName, String str, String str2) {
        boolean z;
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-install-existing-package");
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            if (!isUserAffiliatedWithDeviceLocked(iUserHandleGetCallingUserId)) {
                throw new SecurityException("Admin " + componentName + " is neither the device owner or affiliated user's profile owner.");
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                z = this.mIPackageManager.installExistingPackageAsUser(str2, iUserHandleGetCallingUserId, 0, 1) == 1;
            } catch (RemoteException e) {
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return z;
    }

    public void setAccountManagementDisabled(ComponentName componentName, String str, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (z) {
                activeAdminForCallerLocked.accountTypesWithManagementDisabled.add(str);
            } else {
                activeAdminForCallerLocked.accountTypesWithManagementDisabled.remove(str);
            }
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
    }

    public String[] getAccountTypesWithManagementDisabled() {
        return getAccountTypesWithManagementDisabledAsUser(UserHandle.getCallingUserId());
    }

    public String[] getAccountTypesWithManagementDisabledAsUser(int i) {
        String[] strArr;
        enforceFullCrossUsersPermission(i);
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(i);
            int size = userData.mAdminList.size();
            ArraySet arraySet = new ArraySet();
            for (int i2 = 0; i2 < size; i2++) {
                arraySet.addAll(userData.mAdminList.get(i2).accountTypesWithManagementDisabled);
            }
            strArr = (String[]) arraySet.toArray(new String[arraySet.size()]);
        }
        return strArr;
    }

    public void setUninstallBlocked(ComponentName componentName, String str, String str2, boolean z) {
        Injector injector;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-block-uninstall");
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    this.mIPackageManager.setBlockUninstallForUser(str2, z, callingUserId);
                    injector = this.mInjector;
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed to setBlockUninstallForUser", e);
                    injector = this.mInjector;
                }
                injector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
    }

    public boolean isUninstallBlocked(ComponentName componentName, String str) {
        boolean blockUninstallForUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            if (componentName != null) {
                try {
                    getActiveAdminForCallerLocked(componentName, -1);
                } catch (Throwable th) {
                    throw th;
                }
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    blockUninstallForUser = this.mIPackageManager.getBlockUninstallForUser(str, callingUserId);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                }
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Failed to getBlockUninstallForUser", e);
                return false;
            }
        }
        return blockUninstallForUser;
    }

    public void setCrossProfileCallerIdDisabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.disableCallerId != z) {
                activeAdminForCallerLocked.disableCallerId = z;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public boolean getCrossProfileCallerIdDisabled(ComponentName componentName) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(componentName, -1).disableCallerId;
        }
        return z;
    }

    public boolean getCrossProfileCallerIdDisabledForUser(int i) {
        boolean z;
        enforceCrossUsersPermission(i);
        synchronized (getLockObject()) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            z = profileOwnerAdminLocked != null ? profileOwnerAdminLocked.disableCallerId : false;
        }
        return z;
    }

    public void setCrossProfileContactsSearchDisabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.disableContactsSearch != z) {
                activeAdminForCallerLocked.disableContactsSearch = z;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public boolean getCrossProfileContactsSearchDisabled(ComponentName componentName) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(componentName, -1).disableContactsSearch;
        }
        return z;
    }

    public boolean getCrossProfileContactsSearchDisabledForUser(int i) {
        boolean z;
        enforceCrossUsersPermission(i);
        synchronized (getLockObject()) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            z = profileOwnerAdminLocked != null ? profileOwnerAdminLocked.disableContactsSearch : false;
        }
        return z;
    }

    public void startManagedQuickContact(String str, long j, boolean z, long j2, Intent intent) {
        Intent intentRebuildManagedQuickContactsIntent = ContactsContract.QuickContact.rebuildManagedQuickContactsIntent(str, j, z, j2, intent);
        int callingUserId = UserHandle.getCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                int managedUserId = getManagedUserId(callingUserId);
                if (managedUserId < 0) {
                    return;
                }
                if (isCrossProfileQuickContactDisabled(managedUserId)) {
                    return;
                }
                ContactsInternal.startQuickContactWithErrorToastForUser(this.mContext, intentRebuildManagedQuickContactsIntent, new UserHandle(managedUserId));
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private boolean isCrossProfileQuickContactDisabled(int i) {
        return getCrossProfileCallerIdDisabledForUser(i) && getCrossProfileContactsSearchDisabledForUser(i);
    }

    public int getManagedUserId(int i) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
            if (userInfo.id != i && userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -1;
    }

    public void setBluetoothContactSharingDisabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (activeAdminForCallerLocked.disableBluetoothContactSharing != z) {
                activeAdminForCallerLocked.disableBluetoothContactSharing = z;
                saveSettingsLocked(UserHandle.getCallingUserId());
            }
        }
    }

    public boolean getBluetoothContactSharingDisabled(ComponentName componentName) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            z = getActiveAdminForCallerLocked(componentName, -1).disableBluetoothContactSharing;
        }
        return z;
    }

    public boolean getBluetoothContactSharingDisabledForUser(int i) {
        boolean z;
        synchronized (getLockObject()) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            z = profileOwnerAdminLocked != null ? profileOwnerAdminLocked.disableBluetoothContactSharing : false;
        }
        return z;
    }

    public void setLockTaskPackages(ComponentName componentName, String[] strArr) throws SecurityException {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkNotNull(strArr, "packages is null");
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(componentName);
            setLockTaskPackagesLocked(this.mInjector.userHandleGetCallingUserId(), new ArrayList(Arrays.asList(strArr)));
        }
    }

    private void setLockTaskPackagesLocked(int i, List<String> list) {
        getUserData(i).mLockTaskPackages = list;
        saveSettingsLocked(i);
        updateLockTaskPackagesLocked(list, i);
    }

    public String[] getLockTaskPackages(ComponentName componentName) {
        String[] strArr;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int identifier = this.mInjector.binderGetCallingUserHandle().getIdentifier();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(componentName);
            List<String> list = getUserData(identifier).mLockTaskPackages;
            strArr = (String[]) list.toArray(new String[list.size()]);
        }
        return strArr;
    }

    public boolean isLockTaskPermitted(String str) {
        boolean zContains;
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            zContains = getUserData(iUserHandleGetCallingUserId).mLockTaskPackages.contains(str);
        }
        return zContains;
    }

    public void setLockTaskFeatures(ComponentName componentName, int i) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        boolean z = (i & 4) != 0;
        Preconditions.checkArgument(z || !((i & 8) != 0), "Cannot use LOCK_TASK_FEATURE_OVERVIEW without LOCK_TASK_FEATURE_HOME");
        Preconditions.checkArgument(z || !((i & 2) != 0), "Cannot use LOCK_TASK_FEATURE_NOTIFICATIONS without LOCK_TASK_FEATURE_HOME");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(componentName);
            setLockTaskFeaturesLocked(iUserHandleGetCallingUserId, i);
        }
    }

    private void setLockTaskFeaturesLocked(int i, int i2) {
        getUserData(i).mLockTaskFeatures = i2;
        saveSettingsLocked(i);
        updateLockTaskFeaturesLocked(i2, i);
    }

    public int getLockTaskFeatures(ComponentName componentName) {
        int i;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            enforceCanCallLockTaskLocked(componentName);
            i = getUserData(iUserHandleGetCallingUserId).mLockTaskFeatures;
        }
        return i;
    }

    private void maybeClearLockTaskPolicyLocked() {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            List users = this.mUserManager.getUsers(true);
            for (int size = users.size() - 1; size >= 0; size--) {
                int i = ((UserInfo) users.get(size)).id;
                if (!canUserUseLockTaskLocked(i)) {
                    if (!getUserData(i).mLockTaskPackages.isEmpty()) {
                        Slog.d(LOG_TAG, "User id " + i + " not affiliated. Clearing lock task packages");
                        setLockTaskPackagesLocked(i, Collections.emptyList());
                    }
                    if (getUserData(i).mLockTaskFeatures != 0) {
                        Slog.d(LOG_TAG, "User id " + i + " not affiliated. Clearing lock task features");
                        setLockTaskFeaturesLocked(i, 0);
                    }
                }
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void notifyLockTaskModeChanged(boolean z, String str, int i) {
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("notifyLockTaskModeChanged can only be called by system");
        }
        synchronized (getLockObject()) {
            DevicePolicyData userData = getUserData(i);
            if (userData.mStatusBarDisabled) {
                setStatusBarDisabledInternal(!z, i);
            }
            Bundle bundle = new Bundle();
            bundle.putString("android.app.extra.LOCK_TASK_PACKAGE", str);
            for (ActiveAdmin activeAdmin : userData.mAdminList) {
                boolean zIsDeviceOwner = isDeviceOwner(activeAdmin.info.getComponent(), i);
                boolean zIsProfileOwner = isProfileOwner(activeAdmin.info.getComponent(), i);
                if (zIsDeviceOwner || zIsProfileOwner) {
                    if (z) {
                        sendAdminCommandLocked(activeAdmin, "android.app.action.LOCK_TASK_ENTERING", bundle, (BroadcastReceiver) null);
                    } else {
                        sendAdminCommandLocked(activeAdmin, "android.app.action.LOCK_TASK_EXITING");
                    }
                }
            }
        }
    }

    public void setGlobalSetting(ComponentName componentName, String str, String str2) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            if (GLOBAL_SETTINGS_DEPRECATED.contains(str)) {
                Log.i(LOG_TAG, "Global setting no longer supported: " + str);
                return;
            }
            if (!GLOBAL_SETTINGS_WHITELIST.contains(str) && !UserManager.isDeviceInDemoMode(this.mContext)) {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", str));
            }
            if ("stay_on_while_plugged_in".equals(str)) {
                long maximumTimeToLock = getMaximumTimeToLock(componentName, this.mInjector.userHandleGetCallingUserId(), false);
                if (maximumTimeToLock > 0 && maximumTimeToLock < JobStatus.NO_LATEST_RUNTIME) {
                    return;
                }
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mInjector.settingsGlobalPutString(str, str2);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    @Override
    public void setSystemSetting(ComponentName componentName, final String str, final String str2) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkStringNotEmpty(str, "String setting is null or empty");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            if (!SYSTEM_SETTINGS_WHITELIST.contains(str)) {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", str));
            }
            final int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
                public final void runOrThrow() {
                    this.f$0.mInjector.settingsSystemPutStringForUser(str, str2, iUserHandleGetCallingUserId);
                }
            });
        }
    }

    public boolean setTime(ComponentName componentName, final long j) {
        Preconditions.checkNotNull(componentName, "ComponentName is null in setTime");
        getActiveAdminForCallerLocked(componentName, -2);
        if (this.mInjector.settingsGlobalGetInt("auto_time", 0) == 1) {
            return false;
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
            public final void runOrThrow() {
                this.f$0.mInjector.getAlarmManager().setTime(j);
            }
        });
        return true;
    }

    public boolean setTimeZone(ComponentName componentName, final String str) {
        Preconditions.checkNotNull(componentName, "ComponentName is null in setTimeZone");
        getActiveAdminForCallerLocked(componentName, -2);
        if (this.mInjector.settingsGlobalGetInt("auto_time_zone", 0) == 1) {
            return false;
        }
        this.mInjector.binderWithCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() {
            public final void runOrThrow() {
                this.f$0.mInjector.getAlarmManager().setTimeZone(str);
            }
        });
        return true;
    }

    public void setSecureSetting(ComponentName componentName, String str, String str2) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            if (isDeviceOwner(componentName, iUserHandleGetCallingUserId)) {
                if (!SECURE_SETTINGS_DEVICEOWNER_WHITELIST.contains(str) && !isCurrentUserDemo()) {
                    throw new SecurityException(String.format("Permission denial: Device owners cannot update %1$s", str));
                }
            } else if (!SECURE_SETTINGS_WHITELIST.contains(str) && !isCurrentUserDemo()) {
                throw new SecurityException(String.format("Permission denial: Profile owners cannot update %1$s", str));
            }
            if (str.equals("install_non_market_apps")) {
                if (getTargetSdk(componentName.getPackageName(), iUserHandleGetCallingUserId) >= 26) {
                    throw new UnsupportedOperationException("install_non_market_apps is deprecated. Please use the user restriction no_install_unknown_sources instead.");
                }
                if (!this.mUserManager.isManagedProfile(iUserHandleGetCallingUserId)) {
                    Slog.e(LOG_TAG, "Ignoring setSecureSetting request for " + str + ". User restriction no_install_unknown_sources should be used instead.");
                } else {
                    try {
                        setUserRestriction(componentName, "no_install_unknown_sources", Integer.parseInt(str2) == 0);
                    } catch (NumberFormatException e) {
                        Slog.e(LOG_TAG, "Invalid value: " + str2 + " for setting " + str);
                    }
                }
                return;
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                if ("default_input_method".equals(str)) {
                    if (!TextUtils.equals(this.mInjector.settingsSecureGetStringForUser("default_input_method", iUserHandleGetCallingUserId), str2)) {
                        this.mSetupContentObserver.addPendingChangeByOwnerLocked(iUserHandleGetCallingUserId);
                    }
                    getUserData(iUserHandleGetCallingUserId).mCurrentInputMethodSet = true;
                    saveSettingsLocked(iUserHandleGetCallingUserId);
                }
                this.mInjector.settingsSecurePutStringForUser(str, str2, iUserHandleGetCallingUserId);
                return;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public void setMasterVolumeMuted(ComponentName componentName, boolean z) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            setUserRestriction(componentName, "disallow_unmute_device", z);
        }
    }

    public boolean isMasterVolumeMuted(ComponentName componentName) {
        boolean zIsMasterMute;
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            zIsMasterMute = ((AudioManager) this.mContext.getSystemService("audio")).isMasterMute();
        }
        return zIsMasterMute;
    }

    public void setUserIcon(ComponentName componentName, Bitmap bitmap) {
        synchronized (getLockObject()) {
            Preconditions.checkNotNull(componentName, "ComponentName is null");
            getActiveAdminForCallerLocked(componentName, -1);
            int callingUserId = UserHandle.getCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mUserManagerInternal.setUserIcon(callingUserId, bitmap);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public boolean setKeyguardDisabled(ComponentName componentName, boolean z) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            if (!isUserAffiliatedWithDeviceLocked(iUserHandleGetCallingUserId)) {
                throw new SecurityException("Admin " + componentName + " is neither the device owner or affiliated user's profile owner.");
            }
        }
        if (isManagedProfile(iUserHandleGetCallingUserId)) {
            throw new SecurityException("Managed profile cannot disable keyguard");
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        if (z) {
            try {
                if (this.mLockPatternUtils.isSecure(iUserHandleGetCallingUserId)) {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                    return false;
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
        this.mLockPatternUtils.setLockScreenDisabled(z, iUserHandleGetCallingUserId);
        this.mInjector.getIWindowManager().dismissKeyguard((IKeyguardDismissCallback) null, (CharSequence) null);
        this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        return true;
    }

    public boolean setStatusBarDisabled(ComponentName componentName, boolean z) {
        boolean z2;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            if (!isUserAffiliatedWithDeviceLocked(callingUserId)) {
                throw new SecurityException("Admin " + componentName + " is neither the device owner or affiliated user's profile owner.");
            }
            if (isManagedProfile(callingUserId)) {
                throw new SecurityException("Managed profile cannot disable status bar");
            }
            DevicePolicyData userData = getUserData(callingUserId);
            if (userData.mStatusBarDisabled != z) {
                try {
                    z2 = this.mInjector.getIActivityManager().getLockTaskModeState() != 0;
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed to get LockTask mode");
                    z2 = false;
                }
                if (!z2 && !setStatusBarDisabledInternal(z, callingUserId)) {
                    return false;
                }
                userData.mStatusBarDisabled = z;
                saveSettingsLocked(callingUserId);
            }
            return true;
        }
    }

    private boolean setStatusBarDisabledInternal(boolean z, int i) {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                IStatusBarService iStatusBarServiceAsInterface = IStatusBarService.Stub.asInterface(ServiceManager.checkService(TAG_STATUS_BAR));
                if (iStatusBarServiceAsInterface != null) {
                    iStatusBarServiceAsInterface.disableForUser(z ? STATUS_BAR_DISABLE_MASK : 0, this.mToken, this.mContext.getPackageName(), i);
                    iStatusBarServiceAsInterface.disable2ForUser(z ? 1 : 0, this.mToken, this.mContext.getPackageName(), i);
                    return true;
                }
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Failed to disable the status bar", e);
            }
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    void updateUserSetupCompleteAndPaired() {
        List users = this.mUserManager.getUsers(true);
        int size = users.size();
        for (int i = 0; i < size; i++) {
            int i2 = ((UserInfo) users.get(i)).id;
            if (this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, i2) != 0) {
                DevicePolicyData userData = getUserData(i2);
                if (!userData.mUserSetupComplete) {
                    userData.mUserSetupComplete = true;
                    synchronized (getLockObject()) {
                        saveSettingsLocked(i2);
                    }
                }
            }
            if (this.mIsWatch && this.mInjector.settingsSecureGetIntForUser("device_paired", 0, i2) != 0) {
                DevicePolicyData userData2 = getUserData(i2);
                if (userData2.mPaired) {
                    continue;
                } else {
                    userData2.mPaired = true;
                    synchronized (getLockObject()) {
                        saveSettingsLocked(i2);
                    }
                }
            }
        }
    }

    private class SetupContentObserver extends ContentObserver {
        private final Uri mDefaultImeChanged;
        private final Uri mDeviceProvisioned;
        private final Uri mPaired;

        @GuardedBy("getLockObject()")
        private Set<Integer> mUserIdsWithPendingChangesByOwner;
        private final Uri mUserSetupComplete;

        public SetupContentObserver(Handler handler) {
            super(handler);
            this.mUserSetupComplete = Settings.Secure.getUriFor("user_setup_complete");
            this.mDeviceProvisioned = Settings.Global.getUriFor("device_provisioned");
            this.mPaired = Settings.Secure.getUriFor("device_paired");
            this.mDefaultImeChanged = Settings.Secure.getUriFor("default_input_method");
            this.mUserIdsWithPendingChangesByOwner = new ArraySet();
        }

        void register() {
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mUserSetupComplete, false, this, -1);
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDeviceProvisioned, false, this, -1);
            if (DevicePolicyManagerService.this.mIsWatch) {
                DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mPaired, false, this, -1);
            }
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDefaultImeChanged, false, this, -1);
        }

        @GuardedBy("getLockObject()")
        private void addPendingChangeByOwnerLocked(int i) {
            this.mUserIdsWithPendingChangesByOwner.add(Integer.valueOf(i));
        }

        @Override
        public void onChange(boolean z, Uri uri, int i) {
            if (this.mUserSetupComplete.equals(uri) || (DevicePolicyManagerService.this.mIsWatch && this.mPaired.equals(uri))) {
                DevicePolicyManagerService.this.updateUserSetupCompleteAndPaired();
                return;
            }
            if (this.mDeviceProvisioned.equals(uri)) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    DevicePolicyManagerService.this.setDeviceOwnerSystemPropertyLocked();
                }
            } else if (this.mDefaultImeChanged.equals(uri)) {
                synchronized (DevicePolicyManagerService.this.getLockObject()) {
                    if (this.mUserIdsWithPendingChangesByOwner.contains(Integer.valueOf(i))) {
                        this.mUserIdsWithPendingChangesByOwner.remove(Integer.valueOf(i));
                    } else {
                        DevicePolicyManagerService.this.getUserData(i).mCurrentInputMethodSet = false;
                        DevicePolicyManagerService.this.saveSettingsLocked(i);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    final class LocalService extends DevicePolicyManagerInternal {
        private List<DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener> mWidgetProviderListeners;

        LocalService() {
        }

        public List<String> getCrossProfileWidgetProviders(int i) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (DevicePolicyManagerService.this.mOwners == null) {
                    return Collections.emptyList();
                }
                ComponentName profileOwnerComponent = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(i);
                if (profileOwnerComponent == null) {
                    return Collections.emptyList();
                }
                ActiveAdmin activeAdmin = DevicePolicyManagerService.this.getUserDataUnchecked(i).mAdminMap.get(profileOwnerComponent);
                if (activeAdmin != null && activeAdmin.crossProfileWidgetProviders != null && !activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                    return activeAdmin.crossProfileWidgetProviders;
                }
                return Collections.emptyList();
            }
        }

        public void addOnCrossProfileWidgetProvidersChangeListener(DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener onCrossProfileWidgetProvidersChangeListener) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                if (this.mWidgetProviderListeners == null) {
                    this.mWidgetProviderListeners = new ArrayList();
                }
                if (!this.mWidgetProviderListeners.contains(onCrossProfileWidgetProvidersChangeListener)) {
                    this.mWidgetProviderListeners.add(onCrossProfileWidgetProvidersChangeListener);
                }
            }
        }

        public boolean isActiveAdminWithPolicy(int i, int i2) {
            boolean z;
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                z = DevicePolicyManagerService.this.getActiveAdminWithPolicyForUidLocked(null, i2, i) != null;
            }
            return z;
        }

        private void notifyCrossProfileProvidersChanged(int i, List<String> list) {
            ArrayList arrayList;
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                arrayList = new ArrayList(this.mWidgetProviderListeners);
            }
            int size = arrayList.size();
            for (int i2 = 0; i2 < size; i2++) {
                ((DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener) arrayList.get(i2)).onCrossProfileWidgetProvidersChanged(i, list);
            }
        }

        public Intent createShowAdminSupportIntent(int i, boolean z) {
            ComponentName profileOwnerComponent = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(i);
            if (profileOwnerComponent != null) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent(profileOwnerComponent, i);
            }
            Pair<Integer, ComponentName> deviceOwnerUserIdAndComponent = DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserIdAndComponent();
            if (deviceOwnerUserIdAndComponent != null && ((Integer) deviceOwnerUserIdAndComponent.first).intValue() == i) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent((ComponentName) deviceOwnerUserIdAndComponent.second, i);
            }
            if (z) {
                return DevicePolicyManagerService.this.createShowAdminSupportIntent(null, i);
            }
            return null;
        }

        public Intent createUserRestrictionSupportIntent(int i, String str) {
            Pair<Integer, ComponentName> deviceOwnerUserIdAndComponent;
            long jBinderClearCallingIdentity = DevicePolicyManagerService.this.mInjector.binderClearCallingIdentity();
            try {
                int userRestrictionSource = DevicePolicyManagerService.this.mUserManager.getUserRestrictionSource(str, UserHandle.of(i));
                DevicePolicyManagerService.this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                if ((userRestrictionSource & 1) != 0) {
                    return null;
                }
                boolean z = (userRestrictionSource & 2) != 0;
                boolean z2 = (userRestrictionSource & 4) != 0;
                if (z && z2) {
                    return DevicePolicyManagerService.this.createShowAdminSupportIntent(null, i);
                }
                if (z2) {
                    ComponentName profileOwnerComponent = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(i);
                    if (profileOwnerComponent != null) {
                        return DevicePolicyManagerService.this.createShowAdminSupportIntent(profileOwnerComponent, i);
                    }
                    return null;
                }
                if (!z || (deviceOwnerUserIdAndComponent = DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserIdAndComponent()) == null) {
                    return null;
                }
                return DevicePolicyManagerService.this.createShowAdminSupportIntent((ComponentName) deviceOwnerUserIdAndComponent.second, ((Integer) deviceOwnerUserIdAndComponent.first).intValue());
            } catch (Throwable th) {
                DevicePolicyManagerService.this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }

        public boolean isUserAffiliatedWithDevice(int i) {
            return DevicePolicyManagerService.this.isUserAffiliatedWithDeviceLocked(i);
        }

        public void reportSeparateProfileChallengeChanged(int i) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                DevicePolicyManagerService.this.updateMaximumTimeToLockLocked(i);
            }
        }

        public boolean canUserHaveUntrustedCredentialReset(int i) {
            return DevicePolicyManagerService.this.canUserHaveUntrustedCredentialReset(i);
        }

        public CharSequence getPrintingDisabledReasonForUser(int i) {
            synchronized (DevicePolicyManagerService.this.getLockObject()) {
                DevicePolicyManagerService.this.getUserData(i);
                if (!DevicePolicyManagerService.this.mUserManager.hasUserRestriction("no_printing", UserHandle.of(i))) {
                    Log.e(DevicePolicyManagerService.LOG_TAG, "printing is enabled");
                    return null;
                }
                String profileOwnerPackage = DevicePolicyManagerService.this.mOwners.getProfileOwnerPackage(i);
                if (profileOwnerPackage == null) {
                    profileOwnerPackage = DevicePolicyManagerService.this.mOwners.getDeviceOwnerPackageName();
                }
                PackageManager packageManager = DevicePolicyManagerService.this.mInjector.getPackageManager();
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(profileOwnerPackage, 0);
                    if (packageInfo == null) {
                        Log.e(DevicePolicyManagerService.LOG_TAG, "packageInfo is inexplicably null");
                        return null;
                    }
                    ApplicationInfo applicationInfo = packageInfo.applicationInfo;
                    if (applicationInfo == null) {
                        Log.e(DevicePolicyManagerService.LOG_TAG, "appInfo is inexplicably null");
                        return null;
                    }
                    CharSequence applicationLabel = packageManager.getApplicationLabel(applicationInfo);
                    if (applicationLabel == null) {
                        Log.e(DevicePolicyManagerService.LOG_TAG, "appLabel is inexplicably null");
                        return null;
                    }
                    return ActivityThread.currentActivityThread().getSystemUiContext().getResources().getString(R.string.keyguard_accessibility_widget_empty_slot, applicationLabel);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(DevicePolicyManagerService.LOG_TAG, "getPackageInfo error", e);
                    return null;
                }
            }
        }

        protected DevicePolicyCache getDevicePolicyCache() {
            return DevicePolicyManagerService.this.mPolicyCache;
        }
    }

    private Intent createShowAdminSupportIntent(ComponentName componentName, int i) {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        intent.putExtra("android.intent.extra.USER_ID", i);
        intent.putExtra("android.app.extra.DEVICE_ADMIN", componentName);
        intent.setFlags(268435456);
        return intent;
    }

    public Intent createAdminSupportIntent(String str) {
        Intent intentCreateShowAdminSupportIntent;
        ActiveAdmin deviceOwnerAdminLocked;
        Preconditions.checkNotNull(str);
        int userId = UserHandle.getUserId(this.mInjector.binderGetCallingUid());
        if ("policy_disable_camera".equals(str) || "policy_disable_screen_capture".equals(str) || "policy_mandatory_backups".equals(str)) {
            synchronized (getLockObject()) {
                DevicePolicyData userData = getUserData(userId);
                int size = userData.mAdminList.size();
                for (int i = 0; i < size; i++) {
                    ActiveAdmin activeAdmin = userData.mAdminList.get(i);
                    if ((activeAdmin.disableCamera && "policy_disable_camera".equals(str)) || ((activeAdmin.disableScreenCapture && "policy_disable_screen_capture".equals(str)) || (activeAdmin.mandatoryBackupTransport != null && "policy_mandatory_backups".equals(str)))) {
                        intentCreateShowAdminSupportIntent = createShowAdminSupportIntent(activeAdmin.info.getComponent(), userId);
                        break;
                    }
                }
                intentCreateShowAdminSupportIntent = null;
                if (intentCreateShowAdminSupportIntent == null && "policy_disable_camera".equals(str) && (deviceOwnerAdminLocked = getDeviceOwnerAdminLocked()) != null && deviceOwnerAdminLocked.disableCamera) {
                    intentCreateShowAdminSupportIntent = createShowAdminSupportIntent(deviceOwnerAdminLocked.info.getComponent(), this.mOwners.getDeviceOwnerUserId());
                }
            }
        } else {
            intentCreateShowAdminSupportIntent = this.mLocalService.createUserRestrictionSupportIntent(userId, str);
        }
        if (intentCreateShowAdminSupportIntent != null) {
            intentCreateShowAdminSupportIntent.putExtra("android.app.extra.RESTRICTION", str);
        }
        return intentCreateShowAdminSupportIntent;
    }

    private static boolean isLimitPasswordAllowed(ActiveAdmin activeAdmin, int i) {
        if (activeAdmin.minimumPasswordMetrics.quality < i) {
            return false;
        }
        return activeAdmin.info.usesPolicy(0);
    }

    public void setSystemUpdatePolicy(ComponentName componentName, SystemUpdatePolicy systemUpdatePolicy) {
        if (systemUpdatePolicy != null) {
            systemUpdatePolicy.validateType();
            systemUpdatePolicy.validateFreezePeriods();
            Pair<LocalDate, LocalDate> systemUpdateFreezePeriodRecord = this.mOwners.getSystemUpdateFreezePeriodRecord();
            systemUpdatePolicy.validateAgainstPreviousFreezePeriod((LocalDate) systemUpdateFreezePeriodRecord.first, (LocalDate) systemUpdateFreezePeriodRecord.second, LocalDate.now());
        }
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            if (systemUpdatePolicy == null) {
                this.mOwners.clearSystemUpdatePolicy();
            } else {
                this.mOwners.setSystemUpdatePolicy(systemUpdatePolicy);
                updateSystemUpdateFreezePeriodsRecord(false);
            }
            this.mOwners.writeDeviceOwner();
        }
        this.mContext.sendBroadcastAsUser(new Intent("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED"), UserHandle.SYSTEM);
    }

    public SystemUpdatePolicy getSystemUpdatePolicy() {
        synchronized (getLockObject()) {
            SystemUpdatePolicy systemUpdatePolicy = this.mOwners.getSystemUpdatePolicy();
            if (systemUpdatePolicy == null || systemUpdatePolicy.isValid()) {
                return systemUpdatePolicy;
            }
            Slog.w(LOG_TAG, "Stored system update policy is invalid, return null instead.");
            return null;
        }
    }

    private static boolean withinRange(Pair<LocalDate, LocalDate> pair, LocalDate localDate) {
        return (localDate.isBefore((ChronoLocalDate) pair.first) || localDate.isAfter((ChronoLocalDate) pair.second)) ? false : true;
    }

    private void updateSystemUpdateFreezePeriodsRecord(boolean z) {
        boolean systemUpdateFreezePeriodRecord;
        Slog.d(LOG_TAG, "updateSystemUpdateFreezePeriodsRecord");
        synchronized (getLockObject()) {
            SystemUpdatePolicy systemUpdatePolicy = this.mOwners.getSystemUpdatePolicy();
            if (systemUpdatePolicy == null) {
                return;
            }
            LocalDate localDateNow = LocalDate.now();
            Pair currentFreezePeriod = systemUpdatePolicy.getCurrentFreezePeriod(localDateNow);
            if (currentFreezePeriod == null) {
                return;
            }
            Pair<LocalDate, LocalDate> systemUpdateFreezePeriodRecord2 = this.mOwners.getSystemUpdateFreezePeriodRecord();
            LocalDate localDate = (LocalDate) systemUpdateFreezePeriodRecord2.first;
            LocalDate localDate2 = (LocalDate) systemUpdateFreezePeriodRecord2.second;
            if (localDate2 == null || localDate == null) {
                systemUpdateFreezePeriodRecord = this.mOwners.setSystemUpdateFreezePeriodRecord(localDateNow, localDateNow);
            } else if (localDateNow.equals(localDate2.plusDays(1L))) {
                systemUpdateFreezePeriodRecord = this.mOwners.setSystemUpdateFreezePeriodRecord(localDate, localDateNow);
            } else if (localDateNow.isAfter(localDate2.plusDays(1L))) {
                if (withinRange(currentFreezePeriod, localDate) && withinRange(currentFreezePeriod, localDate2)) {
                    systemUpdateFreezePeriodRecord = this.mOwners.setSystemUpdateFreezePeriodRecord(localDate, localDateNow);
                } else {
                    systemUpdateFreezePeriodRecord = this.mOwners.setSystemUpdateFreezePeriodRecord(localDateNow, localDateNow);
                }
            } else if (localDateNow.isBefore(localDate)) {
                systemUpdateFreezePeriodRecord = this.mOwners.setSystemUpdateFreezePeriodRecord(localDateNow, localDateNow);
            } else {
                systemUpdateFreezePeriodRecord = false;
            }
            if (systemUpdateFreezePeriodRecord && z) {
                this.mOwners.writeDeviceOwner();
            }
        }
    }

    @Override
    public void clearSystemUpdatePolicyFreezePeriodRecord() {
        enforceShell("clearSystemUpdatePolicyFreezePeriodRecord");
        synchronized (getLockObject()) {
            Slog.i(LOG_TAG, "Clear freeze period record: " + this.mOwners.getSystemUpdateFreezePeriodRecordAsString());
            if (this.mOwners.setSystemUpdateFreezePeriodRecord(null, null)) {
                this.mOwners.writeDeviceOwner();
            }
        }
    }

    @VisibleForTesting
    boolean isCallerDeviceOwner(int i) {
        synchronized (getLockObject()) {
            if (!this.mOwners.hasDeviceOwner()) {
                return false;
            }
            if (UserHandle.getUserId(i) != this.mOwners.getDeviceOwnerUserId()) {
                return false;
            }
            String packageName = this.mOwners.getDeviceOwnerComponent().getPackageName();
            try {
                for (String str : this.mInjector.getIPackageManager().getPackagesForUid(i)) {
                    if (packageName.equals(str)) {
                        return true;
                    }
                }
                return false;
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public void notifyPendingSystemUpdate(SystemUpdateInfo systemUpdateInfo) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NOTIFY_PENDING_SYSTEM_UPDATE", "Only the system update service can broadcast update information");
        if (UserHandle.getCallingUserId() != 0) {
            Slog.w(LOG_TAG, "Only the system update service in the system user can broadcast update information.");
            return;
        }
        if (!this.mOwners.saveSystemUpdateInfo(systemUpdateInfo)) {
            return;
        }
        Intent intentPutExtra = new Intent("android.app.action.NOTIFY_PENDING_SYSTEM_UPDATE").putExtra("android.app.extra.SYSTEM_UPDATE_RECEIVED_TIME", systemUpdateInfo == null ? -1L : systemUpdateInfo.getReceivedTime());
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                if (this.mOwners.hasDeviceOwner()) {
                    UserHandle userHandleOf = UserHandle.of(this.mOwners.getDeviceOwnerUserId());
                    intentPutExtra.setComponent(this.mOwners.getDeviceOwnerComponent());
                    this.mContext.sendBroadcastAsUser(intentPutExtra, userHandleOf);
                }
            }
            for (int i : this.mInjector.getIActivityManager().getRunningUserIds()) {
                synchronized (getLockObject()) {
                    ComponentName profileOwnerComponent = this.mOwners.getProfileOwnerComponent(i);
                    if (profileOwnerComponent != null) {
                        intentPutExtra.setComponent(profileOwnerComponent);
                        this.mContext.sendBroadcastAsUser(intentPutExtra, UserHandle.of(i));
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Could not retrieve the list of running users", e);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public SystemUpdateInfo getPendingSystemUpdate(ComponentName componentName) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        enforceProfileOrDeviceOwner(componentName);
        return this.mOwners.getSystemUpdateInfo();
    }

    public void setPermissionPolicy(ComponentName componentName, String str, int i) throws RemoteException {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-permission-grant");
            DevicePolicyData userData = getUserData(callingUserId);
            if (userData.mPermissionPolicy != i) {
                userData.mPermissionPolicy = i;
                saveSettingsLocked(callingUserId);
            }
        }
    }

    public int getPermissionPolicy(ComponentName componentName) throws RemoteException {
        int i;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (getLockObject()) {
            i = getUserData(callingUserId).mPermissionPolicy;
        }
        return i;
    }

    public boolean setPermissionGrantState(ComponentName componentName, String str, String str2, String str3, int i) throws RemoteException {
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        synchronized (getLockObject()) {
            enforceCanManageScope(componentName, str, -1, "delegation-permission-grant");
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    if (getTargetSdk(str2, userHandleBinderGetCallingUserHandle.getIdentifier()) < 23) {
                        return false;
                    }
                    if (!isRuntimePermission(str3)) {
                        return false;
                    }
                    PackageManager packageManager = this.mInjector.getPackageManager();
                    switch (i) {
                        case 0:
                            packageManager.updatePermissionFlags(str3, str2, 4, 0, userHandleBinderGetCallingUserHandle);
                            break;
                        case 1:
                            this.mInjector.getPackageManagerInternal().grantRuntimePermission(str2, str3, userHandleBinderGetCallingUserHandle.getIdentifier(), true);
                            packageManager.updatePermissionFlags(str3, str2, 4, 4, userHandleBinderGetCallingUserHandle);
                            break;
                        case 2:
                            this.mInjector.getPackageManagerInternal().revokeRuntimePermission(str2, str3, userHandleBinderGetCallingUserHandle.getIdentifier(), true);
                            packageManager.updatePermissionFlags(str3, str2, 4, 4, userHandleBinderGetCallingUserHandle);
                            break;
                    }
                    return true;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                }
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            } catch (SecurityException e2) {
                return false;
            }
        }
    }

    public int getPermissionGrantState(ComponentName componentName, String str, String str2, String str3) throws RemoteException {
        int i;
        PackageManager packageManager = this.mInjector.getPackageManager();
        UserHandle userHandleBinderGetCallingUserHandle = this.mInjector.binderGetCallingUserHandle();
        if (!isCallerWithSystemUid()) {
            enforceCanManageScope(componentName, str, -1, "delegation-permission-grant");
        }
        synchronized (getLockObject()) {
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                int iCheckPermission = this.mIPackageManager.checkPermission(str3, str2, userHandleBinderGetCallingUserHandle.getIdentifier());
                if ((packageManager.getPermissionFlags(str3, str2, userHandleBinderGetCallingUserHandle) & 4) != 4) {
                    return 0;
                }
                if (iCheckPermission == 0) {
                    i = 1;
                } else {
                    i = 2;
                }
                return i;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    boolean isPackageInstalledForUser(String str, int i) {
        try {
            PackageInfo packageInfo = this.mInjector.getIPackageManager().getPackageInfo(str, 0, i);
            if (packageInfo != null) {
                return packageInfo.applicationInfo.flags != 0;
            }
            return false;
        } catch (RemoteException e) {
            throw new RuntimeException("Package manager has died", e);
        }
    }

    public boolean isRuntimePermission(String str) throws PackageManager.NameNotFoundException {
        return (this.mInjector.getPackageManager().getPermissionInfo(str, 0).protectionLevel & 15) == 1;
    }

    public boolean isProvisioningAllowed(String str, String str2) {
        Preconditions.checkNotNull(str2);
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                Preconditions.checkArgument(iBinderGetCallingUid == this.mInjector.getPackageManager().getPackageUidAsUser(str2, UserHandle.getUserId(iBinderGetCallingUid)), "Caller uid doesn't match the one for the provided package.");
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                return checkProvisioningPreConditionSkipPermission(str, str2) == 0;
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Invalid package provided " + str2, e);
            }
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            throw th;
        }
    }

    public int checkProvisioningPreCondition(String str, String str2) {
        Preconditions.checkNotNull(str2);
        enforceCanManageProfileAndDeviceOwners();
        return checkProvisioningPreConditionSkipPermission(str, str2);
    }

    private int checkProvisioningPreConditionSkipPermission(String str, String str2) {
        if (!this.mHasFeature) {
            return 13;
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        if (str != null) {
            byte b = -1;
            int iHashCode = str.hashCode();
            if (iHashCode != -920528692) {
                if (iHashCode != -514404415) {
                    if (iHashCode != -340845101) {
                        if (iHashCode == 631897778 && str.equals("android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE")) {
                            b = 3;
                        }
                    } else if (str.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                        b = 0;
                    }
                } else if (str.equals("android.app.action.PROVISION_MANAGED_USER")) {
                    b = 2;
                }
            } else if (str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    return checkManagedProfileProvisioningPreCondition(str2, iUserHandleGetCallingUserId);
                case 1:
                    return checkDeviceOwnerProvisioningPreCondition(iUserHandleGetCallingUserId);
                case 2:
                    return checkManagedUserProvisioningPreCondition(iUserHandleGetCallingUserId);
                case 3:
                    return checkManagedShareableDeviceProvisioningPreCondition(iUserHandleGetCallingUserId);
            }
        }
        throw new IllegalArgumentException("Unknown provisioning action " + str);
    }

    private int checkDeviceOwnerProvisioningPreConditionLocked(ComponentName componentName, int i, boolean z, boolean z2) {
        if (this.mOwners.hasDeviceOwner()) {
            return 1;
        }
        if (this.mOwners.hasProfileOwner(i)) {
            return 2;
        }
        if (!this.mUserManager.isUserRunning(new UserHandle(i))) {
            return 3;
        }
        if (this.mIsWatch && hasPaired(0)) {
            return 8;
        }
        if (z) {
            if ((this.mIsWatch || hasUserSetupCompleted(0)) && !this.mInjector.userManagerIsSplitSystemUser()) {
                if (this.mUserManager.getUserCount() > 1) {
                    return 5;
                }
                if (z2) {
                    return 6;
                }
            }
            return 0;
        }
        if (!this.mInjector.userManagerIsSplitSystemUser()) {
            if (i != 0) {
                return 7;
            }
            if (hasUserSetupCompleted(0)) {
                return 4;
            }
        }
        return 0;
    }

    private int checkDeviceOwnerProvisioningPreCondition(int i) {
        int iCheckDeviceOwnerProvisioningPreConditionLocked;
        synchronized (getLockObject()) {
            iCheckDeviceOwnerProvisioningPreConditionLocked = checkDeviceOwnerProvisioningPreConditionLocked(null, i, false, true);
        }
        return iCheckDeviceOwnerProvisioningPreConditionLocked;
    }

    private int checkManagedProfileProvisioningPreCondition(String str, int i) {
        Injector injector;
        if (!hasFeatureManagedUsers()) {
            return 9;
        }
        if (i == 0 && this.mInjector.userManagerIsSplitSystemUser()) {
            return 14;
        }
        if (getProfileOwner(i) != null) {
            return 2;
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            UserHandle userHandleOf = UserHandle.of(i);
            ComponentName ownerComponent = getOwnerComponent(str, i);
            if (this.mUserManager.hasUserRestriction("no_add_managed_profile", userHandleOf) && (ownerComponent == null || isAdminAffectedByRestriction(ownerComponent, "no_add_managed_profile", i))) {
                return 15;
            }
            boolean z = true;
            if (this.mUserManager.hasUserRestriction("no_remove_managed_profile", userHandleOf) && (ownerComponent == null || isAdminAffectedByRestriction(ownerComponent, "no_remove_managed_profile", i))) {
                z = false;
            }
            if (this.mUserManager.canAddMoreManagedProfiles(i, z)) {
                return 0;
            }
            return 11;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private ComponentName getOwnerComponent(String str, int i) {
        if (isDeviceOwnerPackage(str, i)) {
            return this.mOwners.getDeviceOwnerComponent();
        }
        if (isProfileOwnerPackage(str, i)) {
            return this.mOwners.getProfileOwnerComponent(i);
        }
        return null;
    }

    private ComponentName getOwnerComponent(int i) {
        synchronized (getLockObject()) {
            if (this.mOwners.getDeviceOwnerUserId() == i) {
                return this.mOwners.getDeviceOwnerComponent();
            }
            if (this.mOwners.hasProfileOwner(i)) {
                return this.mOwners.getProfileOwnerComponent(i);
            }
            return null;
        }
    }

    private int checkManagedUserProvisioningPreCondition(int i) {
        if (!hasFeatureManagedUsers()) {
            return 9;
        }
        if (!this.mInjector.userManagerIsSplitSystemUser()) {
            return 12;
        }
        if (i == 0) {
            return 10;
        }
        if (hasUserSetupCompleted(i)) {
            return 4;
        }
        return (this.mIsWatch && hasPaired(0)) ? 8 : 0;
    }

    private int checkManagedShareableDeviceProvisioningPreCondition(int i) {
        if (!this.mInjector.userManagerIsSplitSystemUser()) {
            return 12;
        }
        return checkDeviceOwnerProvisioningPreCondition(i);
    }

    private boolean hasFeatureManagedUsers() {
        try {
            return this.mIPackageManager.hasSystemFeature("android.software.managed_users", 0);
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getWifiMacAddress(ComponentName componentName) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            WifiInfo connectionInfo = this.mInjector.getWifiManager().getConnectionInfo();
            if (connectionInfo == null) {
                return null;
            }
            return connectionInfo.hasRealMacAddress() ? connectionInfo.getMacAddress() : null;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private int getTargetSdk(String str, int i) {
        try {
            ApplicationInfo applicationInfo = this.mIPackageManager.getApplicationInfo(str, 0, i);
            if (applicationInfo == null) {
                return 0;
            }
            return applicationInfo.targetSdkVersion;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isManagedProfile(ComponentName componentName) {
        enforceProfileOrDeviceOwner(componentName);
        return isManagedProfile(this.mInjector.userHandleGetCallingUserId());
    }

    public boolean isSystemOnlyUser(ComponentName componentName) {
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        return UserManager.isSplitSystemUser() && this.mInjector.userHandleGetCallingUserId() == 0;
    }

    public void reboot(ComponentName componentName) {
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            if (this.mTelephonyManager.getCallState() != 0) {
                throw new IllegalStateException("Cannot be called with ongoing call on the device");
            }
            this.mInjector.powerManagerReboot("deviceowner");
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void setShortSupportMessage(ComponentName componentName, CharSequence charSequence) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForUidLocked = getActiveAdminForUidLocked(componentName, this.mInjector.binderGetCallingUid());
            if (!TextUtils.equals(activeAdminForUidLocked.shortSupportMessage, charSequence)) {
                activeAdminForUidLocked.shortSupportMessage = charSequence;
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
    }

    public CharSequence getShortSupportMessage(ComponentName componentName) {
        CharSequence charSequence;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            charSequence = getActiveAdminForUidLocked(componentName, this.mInjector.binderGetCallingUid()).shortSupportMessage;
        }
        return charSequence;
    }

    public void setLongSupportMessage(ComponentName componentName, CharSequence charSequence) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForUidLocked = getActiveAdminForUidLocked(componentName, this.mInjector.binderGetCallingUid());
            if (!TextUtils.equals(activeAdminForUidLocked.longSupportMessage, charSequence)) {
                activeAdminForUidLocked.longSupportMessage = charSequence;
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
    }

    public CharSequence getLongSupportMessage(ComponentName componentName) {
        CharSequence charSequence;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        synchronized (getLockObject()) {
            charSequence = getActiveAdminForUidLocked(componentName, this.mInjector.binderGetCallingUid()).longSupportMessage;
        }
        return charSequence;
    }

    public CharSequence getShortSupportMessageForUser(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query support message for user");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                return null;
            }
            return activeAdminUncheckedLocked.shortSupportMessage;
        }
    }

    public CharSequence getLongSupportMessageForUser(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query support message for user");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                return null;
            }
            return activeAdminUncheckedLocked.longSupportMessage;
        }
    }

    public void setOrganizationColor(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        enforceManagedProfile(iUserHandleGetCallingUserId, "set organization color");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1).organizationColor = i;
            saveSettingsLocked(iUserHandleGetCallingUserId);
        }
    }

    public void setOrganizationColorForUser(int i, int i2) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(i2);
        enforceManageUsers();
        enforceManagedProfile(i2, "set organization color");
        synchronized (getLockObject()) {
            getProfileOwnerAdminLocked(i2).organizationColor = i;
            saveSettingsLocked(i2);
        }
    }

    public int getOrganizationColor(ComponentName componentName) {
        int i;
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "get organization color");
        synchronized (getLockObject()) {
            i = getActiveAdminForCallerLocked(componentName, -1).organizationColor;
        }
        return i;
    }

    public int getOrganizationColorForUser(int i) {
        int i2;
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        enforceFullCrossUsersPermission(i);
        enforceManagedProfile(i, "get organization color");
        synchronized (getLockObject()) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            if (profileOwnerAdminLocked != null) {
                i2 = profileOwnerAdminLocked.organizationColor;
            } else {
                i2 = ActiveAdmin.DEF_ORGANIZATION_COLOR;
            }
        }
        return i2;
    }

    public void setOrganizationName(ComponentName componentName, CharSequence charSequence) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            if (!TextUtils.equals(activeAdminForCallerLocked.organizationName, charSequence)) {
                activeAdminForCallerLocked.organizationName = (charSequence == null || charSequence.length() == 0) ? null : charSequence.toString();
                saveSettingsLocked(iUserHandleGetCallingUserId);
            }
        }
    }

    public CharSequence getOrganizationName(ComponentName componentName) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "get organization name");
        synchronized (getLockObject()) {
            str = getActiveAdminForCallerLocked(componentName, -1).organizationName;
        }
        return str;
    }

    public CharSequence getDeviceOwnerOrganizationName() {
        String str = null;
        if (!this.mHasFeature) {
            return null;
        }
        enforceDeviceOwnerOrManageUsers();
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
            if (deviceOwnerAdminLocked != null) {
                str = deviceOwnerAdminLocked.organizationName;
            }
        }
        return str;
    }

    public CharSequence getOrganizationNameForUser(int i) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(i);
        enforceManagedProfile(i, "get organization name");
        synchronized (getLockObject()) {
            ActiveAdmin profileOwnerAdminLocked = getProfileOwnerAdminLocked(i);
            str = profileOwnerAdminLocked != null ? profileOwnerAdminLocked.organizationName : null;
        }
        return str;
    }

    @Override
    public List<String> setMeteredDataDisabledPackages(ComponentName componentName, List<String> list) {
        List<String> listRemoveInvalidPkgsForMeteredDataRestriction;
        Preconditions.checkNotNull(componentName);
        Preconditions.checkNotNull(list);
        if (!this.mHasFeature) {
            return list;
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                listRemoveInvalidPkgsForMeteredDataRestriction = removeInvalidPkgsForMeteredDataRestriction(iUserHandleGetCallingUserId, list);
                activeAdminForCallerLocked.meteredDisabledPackages = list;
                pushMeteredDisabledPackagesLocked(iUserHandleGetCallingUserId);
                saveSettingsLocked(iUserHandleGetCallingUserId);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return listRemoveInvalidPkgsForMeteredDataRestriction;
    }

    private List<String> removeInvalidPkgsForMeteredDataRestriction(int i, List<String> list) {
        Set<String> activeAdminPackagesLocked = getActiveAdminPackagesLocked(i);
        ArrayList arrayList = new ArrayList();
        for (int size = list.size() - 1; size >= 0; size--) {
            String str = list.get(size);
            if (activeAdminPackagesLocked.contains(str)) {
                arrayList.add(str);
            } else {
                try {
                    if (!this.mInjector.getIPackageManager().isPackageAvailable(str, i)) {
                        arrayList.add(str);
                    }
                } catch (RemoteException e) {
                }
            }
        }
        list.removeAll(arrayList);
        return arrayList;
    }

    @Override
    public List<String> getMeteredDataDisabledPackages(ComponentName componentName) {
        List<String> arrayList;
        Preconditions.checkNotNull(componentName);
        if (!this.mHasFeature) {
            return new ArrayList();
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -1);
            arrayList = activeAdminForCallerLocked.meteredDisabledPackages == null ? new ArrayList<>() : activeAdminForCallerLocked.meteredDisabledPackages;
        }
        return arrayList;
    }

    @Override
    public boolean isMeteredDataDisabledPackageForUser(ComponentName componentName, String str, int i) {
        Preconditions.checkNotNull(componentName);
        if (!this.mHasFeature) {
            return false;
        }
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query restricted pkgs for a specific user");
        }
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null || activeAdminUncheckedLocked.meteredDisabledPackages == null) {
                return false;
            }
            return activeAdminUncheckedLocked.meteredDisabledPackages.contains(str);
        }
    }

    private void pushMeteredDisabledPackagesLocked(int i) {
        this.mInjector.getNetworkPolicyManagerInternal().setMeteredRestrictedPackages(getMeteredDisabledPackagesLocked(i), i);
    }

    private Set<String> getMeteredDisabledPackagesLocked(int i) {
        ActiveAdmin activeAdminUncheckedLocked;
        ComponentName ownerComponent = getOwnerComponent(i);
        ArraySet arraySet = new ArraySet();
        if (ownerComponent != null && (activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(ownerComponent, i)) != null && activeAdminUncheckedLocked.meteredDisabledPackages != null) {
            arraySet.addAll(activeAdminUncheckedLocked.meteredDisabledPackages);
        }
        return arraySet;
    }

    public void setAffiliationIds(ComponentName componentName, List<String> list) {
        if (!this.mHasFeature) {
            return;
        }
        if (list == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (TextUtils.isEmpty(it.next())) {
                throw new IllegalArgumentException("ids must not contain empty string");
            }
        }
        ArraySet arraySet = new ArraySet(list);
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            getUserData(iUserHandleGetCallingUserId).mAffiliationIds = arraySet;
            saveSettingsLocked(iUserHandleGetCallingUserId);
            if (iUserHandleGetCallingUserId != 0 && isDeviceOwner(componentName, iUserHandleGetCallingUserId)) {
                getUserData(0).mAffiliationIds = arraySet;
                saveSettingsLocked(0);
            }
            maybePauseDeviceWideLoggingLocked();
            maybeResumeDeviceWideLoggingLocked();
            maybeClearLockTaskPolicyLocked();
        }
    }

    public List<String> getAffiliationIds(ComponentName componentName) {
        ArrayList arrayList;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            arrayList = new ArrayList(getUserData(this.mInjector.userHandleGetCallingUserId()).mAffiliationIds);
        }
        return arrayList;
    }

    public boolean isAffiliatedUser() {
        boolean zIsUserAffiliatedWithDeviceLocked;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            zIsUserAffiliatedWithDeviceLocked = isUserAffiliatedWithDeviceLocked(this.mInjector.userHandleGetCallingUserId());
        }
        return zIsUserAffiliatedWithDeviceLocked;
    }

    private boolean isUserAffiliatedWithDeviceLocked(int i) {
        if (!this.mOwners.hasDeviceOwner()) {
            return false;
        }
        if (i == this.mOwners.getDeviceOwnerUserId() || i == 0) {
            return true;
        }
        if (getProfileOwner(i) == null) {
            return false;
        }
        Set<String> set = getUserData(i).mAffiliationIds;
        Set<String> set2 = getUserData(0).mAffiliationIds;
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            if (set2.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    private boolean areAllUsersAffiliatedWithDeviceLocked() {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            List users = this.mUserManager.getUsers(true);
            for (int i = 0; i < users.size(); i++) {
                int i2 = ((UserInfo) users.get(i)).id;
                if (!isUserAffiliatedWithDeviceLocked(i2)) {
                    Slog.d(LOG_TAG, "User id " + i2 + " not affiliated.");
                    return false;
                }
            }
            return true;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void setSecurityLoggingEnabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
            if (z == this.mInjector.securityLogGetLoggingEnabledProperty()) {
                return;
            }
            this.mInjector.securityLogSetLoggingEnabledProperty(z);
            if (z) {
                this.mSecurityLogMonitor.start();
                maybePauseDeviceWideLoggingLocked();
            } else {
                this.mSecurityLogMonitor.stop();
            }
        }
    }

    public boolean isSecurityLoggingEnabled(ComponentName componentName) {
        boolean zSecurityLogGetLoggingEnabledProperty;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            if (!isCallerWithSystemUid()) {
                Preconditions.checkNotNull(componentName);
                getActiveAdminForCallerLocked(componentName, -2);
            }
            zSecurityLogGetLoggingEnabledProperty = this.mInjector.securityLogGetLoggingEnabledProperty();
        }
        return zSecurityLogGetLoggingEnabledProperty;
    }

    private void recordSecurityLogRetrievalTime() {
        synchronized (getLockObject()) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            DevicePolicyData userData = getUserData(0);
            if (jCurrentTimeMillis > userData.mLastSecurityLogRetrievalTime) {
                userData.mLastSecurityLogRetrievalTime = jCurrentTimeMillis;
                saveSettingsLocked(0);
            }
        }
    }

    public ParceledListSlice<SecurityLog.SecurityEvent> retrievePreRebootSecurityLogs(ComponentName componentName) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName);
        ensureDeviceOwnerAndAllUsersAffiliated(componentName);
        if (!this.mContext.getResources().getBoolean(R.^attr-private.pointerIconAlias) || !this.mInjector.securityLogGetLoggingEnabledProperty()) {
            return null;
        }
        recordSecurityLogRetrievalTime();
        ArrayList arrayList = new ArrayList();
        try {
            SecurityLog.readPreviousEvents(arrayList);
            return new ParceledListSlice<>(arrayList);
        } catch (IOException e) {
            Slog.w(LOG_TAG, "Fail to read previous events", e);
            return new ParceledListSlice<>(Collections.emptyList());
        }
    }

    public ParceledListSlice<SecurityLog.SecurityEvent> retrieveSecurityLogs(ComponentName componentName) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName);
        ensureDeviceOwnerAndAllUsersAffiliated(componentName);
        if (!this.mInjector.securityLogGetLoggingEnabledProperty()) {
            return null;
        }
        recordSecurityLogRetrievalTime();
        List<SecurityLog.SecurityEvent> listRetrieveLogs = this.mSecurityLogMonitor.retrieveLogs();
        if (listRetrieveLogs != null) {
            return new ParceledListSlice<>(listRetrieveLogs);
        }
        return null;
    }

    @Override
    public long forceSecurityLogs() {
        enforceShell("forceSecurityLogs");
        if (!this.mInjector.securityLogGetLoggingEnabledProperty()) {
            throw new IllegalStateException("logging is not available");
        }
        return this.mSecurityLogMonitor.forceLogs();
    }

    private void enforceCanManageDeviceAdmin() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
    }

    private void enforceCanManageProfileAndDeviceOwners() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", null);
    }

    private void enforceCallerSystemUserHandle() {
        if (UserHandle.getUserId(this.mInjector.binderGetCallingUid()) != 0) {
            throw new SecurityException("Caller has to be in user 0");
        }
    }

    public boolean isUninstallInQueue(String str) {
        boolean zContains;
        enforceCanManageDeviceAdmin();
        Pair pair = new Pair(str, Integer.valueOf(this.mInjector.userHandleGetCallingUserId()));
        synchronized (getLockObject()) {
            zContains = this.mPackagesToRemove.contains(pair);
        }
        return zContains;
    }

    public void uninstallPackageWithActiveAdmins(final String str) {
        enforceCanManageDeviceAdmin();
        Preconditions.checkArgument(!TextUtils.isEmpty(str));
        final int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        enforceUserUnlocked(iUserHandleGetCallingUserId);
        ComponentName profileOwner = getProfileOwner(iUserHandleGetCallingUserId);
        if (profileOwner != null && str.equals(profileOwner.getPackageName())) {
            throw new IllegalArgumentException("Cannot uninstall a package with a profile owner");
        }
        ComponentName deviceOwnerComponent = getDeviceOwnerComponent(false);
        if (getDeviceOwnerUserId() == iUserHandleGetCallingUserId && deviceOwnerComponent != null && str.equals(deviceOwnerComponent.getPackageName())) {
            throw new IllegalArgumentException("Cannot uninstall a package with a device owner");
        }
        Pair<String, Integer> pair = new Pair<>(str, Integer.valueOf(iUserHandleGetCallingUserId));
        synchronized (getLockObject()) {
            this.mPackagesToRemove.add(pair);
        }
        List<ComponentName> activeAdmins = getActiveAdmins(iUserHandleGetCallingUserId);
        final ArrayList arrayList = new ArrayList();
        if (activeAdmins != null) {
            for (ComponentName componentName : activeAdmins) {
                if (str.equals(componentName.getPackageName())) {
                    arrayList.add(componentName);
                    removeActiveAdmin(componentName, iUserHandleGetCallingUserId);
                }
            }
        }
        if (arrayList.size() == 0) {
            startUninstallIntent(str, iUserHandleGetCallingUserId);
        } else {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Iterator it = arrayList.iterator();
                    while (it.hasNext()) {
                        DevicePolicyManagerService.this.removeAdminArtifacts((ComponentName) it.next(), iUserHandleGetCallingUserId);
                    }
                    DevicePolicyManagerService.this.startUninstallIntent(str, iUserHandleGetCallingUserId);
                }
            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    public boolean isDeviceProvisioned() {
        boolean z;
        enforceManageUsers();
        synchronized (getLockObject()) {
            z = getUserDataUnchecked(0).mUserSetupComplete;
        }
        return z;
    }

    private boolean isCurrentUserDemo() {
        if (UserManager.isDeviceInDemoMode(this.mContext)) {
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                return this.mUserManager.getUserInfo(iUserHandleGetCallingUserId).isDemo();
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return false;
    }

    private void removePackageIfRequired(String str, int i) {
        if (!packageHasActiveAdmins(str, i)) {
            startUninstallIntent(str, i);
        }
    }

    private void startUninstallIntent(String str, int i) {
        Pair pair = new Pair(str, Integer.valueOf(i));
        synchronized (getLockObject()) {
            if (this.mPackagesToRemove.contains(pair)) {
                this.mPackagesToRemove.remove(pair);
                try {
                    if (this.mInjector.getIPackageManager().getPackageInfo(str, 0, i) == null) {
                        return;
                    }
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Failure talking to PackageManager while getting package info");
                }
                try {
                    this.mInjector.getIActivityManager().forceStopPackage(str, i);
                } catch (RemoteException e2) {
                    Log.e(LOG_TAG, "Failure talking to ActivityManager while force stopping package");
                }
                Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse("package:" + str));
                intent.setFlags(268435456);
                this.mContext.startActivityAsUser(intent, UserHandle.of(i));
            }
        }
    }

    private void removeAdminArtifacts(ComponentName componentName, int i) {
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminUncheckedLocked = getActiveAdminUncheckedLocked(componentName, i);
            if (activeAdminUncheckedLocked == null) {
                return;
            }
            DevicePolicyData userData = getUserData(i);
            boolean zUsesPolicy = activeAdminUncheckedLocked.info.usesPolicy(5);
            userData.mAdminList.remove(activeAdminUncheckedLocked);
            userData.mAdminMap.remove(componentName);
            validatePasswordOwnerLocked(userData);
            if (zUsesPolicy) {
                resetGlobalProxyLocked(userData);
            }
            pushActiveAdminPackagesLocked(i);
            pushMeteredDisabledPackagesLocked(i);
            saveSettingsLocked(i);
            updateMaximumTimeToLockLocked(i);
            userData.mRemovingAdmins.remove(componentName);
            Slog.i(LOG_TAG, "Device admin " + componentName + " removed from user " + i);
            pushUserRestrictions(i);
        }
    }

    public void setDeviceProvisioningConfigApplied() {
        enforceManageUsers();
        synchronized (getLockObject()) {
            getUserData(0).mDeviceProvisioningConfigApplied = true;
            saveSettingsLocked(0);
        }
    }

    public boolean isDeviceProvisioningConfigApplied() {
        boolean z;
        enforceManageUsers();
        synchronized (getLockObject()) {
            z = getUserData(0).mDeviceProvisioningConfigApplied;
        }
        return z;
    }

    public void forceUpdateUserSetupComplete() {
        enforceCanManageProfileAndDeviceOwners();
        enforceCallerSystemUserHandle();
        if (!this.mInjector.isBuildDebuggable()) {
            return;
        }
        getUserData(0).mUserSetupComplete = this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, 0) != 0;
        synchronized (getLockObject()) {
            saveSettingsLocked(0);
        }
    }

    public void setBackupServiceEnabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -2);
            if (!z) {
                activeAdminForCallerLocked.mandatoryBackupTransport = null;
                saveSettingsLocked(0);
            }
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                IBackupManager iBackupManager = this.mInjector.getIBackupManager();
                if (iBackupManager != null) {
                    iBackupManager.setBackupServiceActive(0, z);
                }
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed ");
                sb.append(z ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "de");
                sb.append("activating backup service.");
                throw new IllegalStateException(sb.toString(), e);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public boolean isBackupServiceEnabled(ComponentName componentName) {
        boolean z;
        Preconditions.checkNotNull(componentName);
        if (!this.mHasFeature) {
            return true;
        }
        synchronized (getLockObject()) {
            try {
                try {
                    getActiveAdminForCallerLocked(componentName, -2);
                    IBackupManager iBackupManager = this.mInjector.getIBackupManager();
                    if (iBackupManager != null) {
                        z = iBackupManager.isBackupServiceActive(0);
                    }
                } catch (RemoteException e) {
                    throw new IllegalStateException("Failed requesting backup service state.", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return z;
    }

    public boolean setMandatoryBackupTransport(final ComponentName componentName, final ComponentName componentName2) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        final int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        ISelectBackupTransportCallback.Stub stub = new ISelectBackupTransportCallback.Stub() {
            public void onSuccess(String str) {
                DevicePolicyManagerService.this.saveMandatoryBackupTransport(componentName, iBinderGetCallingUid, componentName2);
                atomicBoolean.set(true);
                countDownLatch.countDown();
            }

            public void onFailure(int i) {
                countDownLatch.countDown();
            }
        };
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                IBackupManager iBackupManager = this.mInjector.getIBackupManager();
                if (iBackupManager != null && componentName2 != null) {
                    if (!iBackupManager.isBackupServiceActive(0)) {
                        iBackupManager.setBackupServiceActive(0, true);
                    }
                    iBackupManager.selectBackupTransportAsync(componentName2, stub);
                    countDownLatch.await();
                    if (atomicBoolean.get()) {
                        iBackupManager.setBackupEnabled(true);
                    }
                } else if (componentName2 == null) {
                    saveMandatoryBackupTransport(componentName, iBinderGetCallingUid, componentName2);
                    atomicBoolean.set(true);
                }
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                return atomicBoolean.get();
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed to set mandatory backup transport.", e);
            } catch (InterruptedException e2) {
                throw new IllegalStateException("Failed to set mandatory backup transport.", e2);
            }
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            throw th;
        }
    }

    private void saveMandatoryBackupTransport(ComponentName componentName, int i, ComponentName componentName2) {
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminWithPolicyForUidLocked = getActiveAdminWithPolicyForUidLocked(componentName, -2, i);
            if (!Objects.equals(componentName2, activeAdminWithPolicyForUidLocked.mandatoryBackupTransport)) {
                activeAdminWithPolicyForUidLocked.mandatoryBackupTransport = componentName2;
                saveSettingsLocked(0);
            }
        }
    }

    public ComponentName getMandatoryBackupTransport() {
        ComponentName componentName = null;
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
            if (deviceOwnerAdminLocked != null) {
                componentName = deviceOwnerAdminLocked.mandatoryBackupTransport;
            }
        }
        return componentName;
    }

    public boolean bindDeviceAdminServiceAsUser(ComponentName componentName, IApplicationThread iApplicationThread, IBinder iBinder, Intent intent, IServiceConnection iServiceConnection, int i, int i2) {
        String ownerPackageNameForUserLocked;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName);
        Preconditions.checkNotNull(iApplicationThread);
        Preconditions.checkNotNull(intent);
        Preconditions.checkArgument((intent.getComponent() == null && intent.getPackage() == null) ? false : true, "Service intent must be explicit (with a package name or component): " + intent);
        Preconditions.checkNotNull(iServiceConnection);
        Preconditions.checkArgument(this.mInjector.userHandleGetCallingUserId() != i2, "target user id must be different from the calling user id");
        if (!getBindDeviceAdminTargetUsers(componentName).contains(UserHandle.of(i2))) {
            throw new SecurityException("Not allowed to bind to target user id");
        }
        synchronized (getLockObject()) {
            ownerPackageNameForUserLocked = getOwnerPackageNameForUserLocked(i2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            if (createCrossUserServiceIntent(intent, ownerPackageNameForUserLocked, i2) == null) {
                return false;
            }
            return this.mInjector.getIActivityManager().bindService(iApplicationThread, iBinder, intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), iServiceConnection, i, this.mContext.getOpPackageName(), i2) != 0;
        } catch (RemoteException e) {
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName componentName) {
        ArrayList arrayList;
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                arrayList = new ArrayList();
                if (!isDeviceOwner(componentName, iUserHandleGetCallingUserId)) {
                    if (canUserBindToDeviceOwnerLocked(iUserHandleGetCallingUserId)) {
                        arrayList.add(UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
                    }
                } else {
                    List users = this.mUserManager.getUsers(true);
                    for (int i = 0; i < users.size(); i++) {
                        int i2 = ((UserInfo) users.get(i)).id;
                        if (i2 != iUserHandleGetCallingUserId && canUserBindToDeviceOwnerLocked(i2)) {
                            arrayList.add(UserHandle.of(i2));
                        }
                    }
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return arrayList;
    }

    private boolean canUserBindToDeviceOwnerLocked(int i) {
        if (this.mOwners.hasDeviceOwner() && i != this.mOwners.getDeviceOwnerUserId() && this.mOwners.hasProfileOwner(i) && TextUtils.equals(this.mOwners.getDeviceOwnerPackageName(), this.mOwners.getProfileOwnerPackage(i))) {
            return isUserAffiliatedWithDeviceLocked(i);
        }
        return false;
    }

    private boolean hasIncompatibleAccountsOrNonAdbNoLock(int i, ComponentName componentName) {
        if (!isAdb()) {
            return true;
        }
        wtfIfInLock();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            AccountManager accountManager = AccountManager.get(this.mContext);
            Account[] accountsAsUser = accountManager.getAccountsAsUser(i);
            boolean z = false;
            if (accountsAsUser.length == 0) {
                return false;
            }
            synchronized (getLockObject()) {
                if (componentName != null) {
                    if (isAdminTestOnlyLocked(componentName, i)) {
                        String[] strArr = {"android.account.DEVICE_OR_PROFILE_OWNER_ALLOWED"};
                        String[] strArr2 = {"android.account.DEVICE_OR_PROFILE_OWNER_DISALLOWED"};
                        int length = accountsAsUser.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 >= length) {
                                z = true;
                                break;
                            }
                            Account account = accountsAsUser[i2];
                            if (hasAccountFeatures(accountManager, account, strArr2)) {
                                Log.e(LOG_TAG, account + " has " + strArr2[0]);
                                break;
                            }
                            if (!hasAccountFeatures(accountManager, account, strArr)) {
                                Log.e(LOG_TAG, account + " doesn't have " + strArr[0]);
                                break;
                            }
                            i2++;
                        }
                        if (z) {
                            Log.w(LOG_TAG, "All accounts are compatible");
                        } else {
                            Log.e(LOG_TAG, "Found incompatible accounts");
                        }
                        return !z;
                    }
                }
                Log.w(LOG_TAG, "Non test-only owner can't be installed with existing accounts.");
                return true;
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private boolean hasAccountFeatures(AccountManager accountManager, Account account, String[] strArr) {
        try {
            return accountManager.hasFeatures(account, strArr, null, null).getResult().booleanValue();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to get account feature", e);
            return false;
        }
    }

    private boolean isAdb() {
        int iBinderGetCallingUid = this.mInjector.binderGetCallingUid();
        return iBinderGetCallingUid == 2000 || iBinderGetCallingUid == 0;
    }

    public void setNetworkLoggingEnabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        synchronized (getLockObject()) {
            Preconditions.checkNotNull(componentName);
            getActiveAdminForCallerLocked(componentName, -2);
            if (z == isNetworkLoggingEnabledInternalLocked()) {
                return;
            }
            ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
            deviceOwnerAdminLocked.isNetworkLoggingEnabled = z;
            if (!z) {
                deviceOwnerAdminLocked.numNetworkLoggingNotifications = 0;
                deviceOwnerAdminLocked.lastNetworkLoggingNotificationTimeMs = 0L;
            }
            saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            setNetworkLoggingActiveInternal(z);
        }
    }

    private void setNetworkLoggingActiveInternal(boolean z) {
        synchronized (getLockObject()) {
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                if (z) {
                    this.mNetworkLogger = new NetworkLogger(this, this.mInjector.getPackageManagerInternal());
                    if (!this.mNetworkLogger.startNetworkLogging()) {
                        this.mNetworkLogger = null;
                        Slog.wtf(LOG_TAG, "Network logging could not be started due to the logging service not being available yet.");
                    }
                    maybePauseDeviceWideLoggingLocked();
                    sendNetworkLoggingNotificationLocked();
                } else {
                    if (this.mNetworkLogger != null && !this.mNetworkLogger.stopNetworkLogging()) {
                        Slog.wtf(LOG_TAG, "Network logging could not be stopped due to the logging service not being available yet.");
                    }
                    this.mNetworkLogger = null;
                    this.mInjector.getNotificationManager().cancel(1002);
                }
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                throw th;
            }
        }
    }

    private void maybePauseDeviceWideLoggingLocked() {
        if (!areAllUsersAffiliatedWithDeviceLocked()) {
            Slog.i(LOG_TAG, "There are unaffiliated users, security and network logging will be paused if enabled.");
            this.mSecurityLogMonitor.pause();
            if (this.mNetworkLogger != null) {
                this.mNetworkLogger.pause();
            }
        }
    }

    private void maybeResumeDeviceWideLoggingLocked() {
        if (areAllUsersAffiliatedWithDeviceLocked()) {
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                this.mSecurityLogMonitor.resume();
                if (this.mNetworkLogger != null) {
                    this.mNetworkLogger.resume();
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    private void discardDeviceWideLogsLocked() {
        this.mSecurityLogMonitor.discardLogs();
        if (this.mNetworkLogger != null) {
            this.mNetworkLogger.discardLogs();
        }
    }

    public boolean isNetworkLoggingEnabled(ComponentName componentName) {
        boolean zIsNetworkLoggingEnabledInternalLocked;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            enforceDeviceOwnerOrManageUsers();
            zIsNetworkLoggingEnabledInternalLocked = isNetworkLoggingEnabledInternalLocked();
        }
        return zIsNetworkLoggingEnabledInternalLocked;
    }

    private boolean isNetworkLoggingEnabledInternalLocked() {
        ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
        return deviceOwnerAdminLocked != null && deviceOwnerAdminLocked.isNetworkLoggingEnabled;
    }

    public List<NetworkEvent> retrieveNetworkLogs(ComponentName componentName, long j) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName);
        ensureDeviceOwnerAndAllUsersAffiliated(componentName);
        synchronized (getLockObject()) {
            if (this.mNetworkLogger != null && isNetworkLoggingEnabledInternalLocked()) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                DevicePolicyData userData = getUserData(0);
                if (jCurrentTimeMillis > userData.mLastNetworkLogsRetrievalTime) {
                    userData.mLastNetworkLogsRetrievalTime = jCurrentTimeMillis;
                    saveSettingsLocked(0);
                }
                return this.mNetworkLogger.retrieveLogs(j);
            }
            return null;
        }
    }

    private void sendNetworkLoggingNotificationLocked() {
        ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
        if (deviceOwnerAdminLocked == null || !deviceOwnerAdminLocked.isNetworkLoggingEnabled || deviceOwnerAdminLocked.numNetworkLoggingNotifications >= 2) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - deviceOwnerAdminLocked.lastNetworkLoggingNotificationTimeMs < MS_PER_DAY) {
            return;
        }
        deviceOwnerAdminLocked.numNetworkLoggingNotifications++;
        if (deviceOwnerAdminLocked.numNetworkLoggingNotifications >= 2) {
            deviceOwnerAdminLocked.lastNetworkLoggingNotificationTimeMs = 0L;
        } else {
            deviceOwnerAdminLocked.lastNetworkLoggingNotificationTimeMs = jCurrentTimeMillis;
        }
        Intent intent = new Intent("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
        intent.setPackage("com.android.systemui");
        this.mInjector.getNotificationManager().notify(1002, new Notification.Builder(this.mContext, SystemNotificationChannels.DEVICE_ADMIN).setSmallIcon(R.drawable.frame_gallery_thumb).setContentTitle(this.mContext.getString(R.string.ext_media_status_bad_removal)).setContentText(this.mContext.getString(R.string.ext_media_seamless_action)).setTicker(this.mContext.getString(R.string.ext_media_status_bad_removal)).setShowWhen(true).setContentIntent(PendingIntent.getBroadcastAsUser(this.mContext, 0, intent, 0, UserHandle.CURRENT)).setStyle(new Notification.BigTextStyle().bigText(this.mContext.getString(R.string.ext_media_seamless_action))).build());
        saveSettingsLocked(this.mOwners.getDeviceOwnerUserId());
    }

    private String getOwnerPackageNameForUserLocked(int i) {
        if (this.mOwners.getDeviceOwnerUserId() == i) {
            return this.mOwners.getDeviceOwnerPackageName();
        }
        return this.mOwners.getProfileOwnerPackage(i);
    }

    private Intent createCrossUserServiceIntent(Intent intent, String str, int i) throws RemoteException, SecurityException {
        ResolveInfo resolveInfoResolveService = this.mIPackageManager.resolveService(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0, i);
        if (resolveInfoResolveService == null || resolveInfoResolveService.serviceInfo == null) {
            Log.e(LOG_TAG, "Fail to look up the service: " + intent + " or user " + i + " is not running");
            return null;
        }
        if (!str.equals(resolveInfoResolveService.serviceInfo.packageName)) {
            throw new SecurityException("Only allow to bind service in " + str);
        }
        if (resolveInfoResolveService.serviceInfo.exported && !"android.permission.BIND_DEVICE_ADMIN".equals(resolveInfoResolveService.serviceInfo.permission)) {
            throw new SecurityException("Service must be protected by BIND_DEVICE_ADMIN permission");
        }
        intent.setComponent(resolveInfoResolveService.serviceInfo.getComponentName());
        return intent;
    }

    public long getLastSecurityLogRetrievalTime() {
        enforceDeviceOwnerOrManageUsers();
        return getUserData(0).mLastSecurityLogRetrievalTime;
    }

    public long getLastBugReportRequestTime() {
        enforceDeviceOwnerOrManageUsers();
        return getUserData(0).mLastBugReportRequestTime;
    }

    public long getLastNetworkLogRetrievalTime() {
        enforceDeviceOwnerOrManageUsers();
        return getUserData(0).mLastNetworkLogsRetrievalTime;
    }

    public boolean setResetPasswordToken(ComponentName componentName, byte[] bArr) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        if (bArr == null || bArr.length < 32) {
            throw new IllegalArgumentException("token must be at least 32-byte long");
        }
        synchronized (getLockObject()) {
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(componentName, -1);
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                if (userData.mPasswordTokenHandle != 0) {
                    this.mLockPatternUtils.removeEscrowToken(userData.mPasswordTokenHandle, iUserHandleGetCallingUserId);
                }
                userData.mPasswordTokenHandle = this.mLockPatternUtils.addEscrowToken(bArr, iUserHandleGetCallingUserId);
                saveSettingsLocked(iUserHandleGetCallingUserId);
                z = userData.mPasswordTokenHandle != 0;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
        return z;
    }

    public boolean clearResetPasswordToken(ComponentName componentName) {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(componentName, -1);
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            if (userData.mPasswordTokenHandle == 0) {
                return false;
            }
            long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                boolean zRemoveEscrowToken = this.mLockPatternUtils.removeEscrowToken(userData.mPasswordTokenHandle, iUserHandleGetCallingUserId);
                userData.mPasswordTokenHandle = 0L;
                saveSettingsLocked(iUserHandleGetCallingUserId);
                return zRemoveEscrowToken;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
            }
        }
    }

    public boolean isResetPasswordTokenActive(ComponentName componentName) {
        synchronized (getLockObject()) {
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(componentName, -1);
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            if (userData.mPasswordTokenHandle != 0) {
                long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
                try {
                    return this.mLockPatternUtils.isEscrowTokenActive(userData.mPasswordTokenHandle, iUserHandleGetCallingUserId);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
                }
            }
            return false;
        }
    }

    public boolean resetPasswordWithToken(ComponentName componentName, String str, byte[] bArr, int i) {
        Preconditions.checkNotNull(bArr);
        synchronized (getLockObject()) {
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(componentName, -1);
            DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
            if (userData.mPasswordTokenHandle != 0) {
                if (str == null) {
                    str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                }
                return resetPasswordInternal(str, userData.mPasswordTokenHandle, bArr, i, this.mInjector.binderGetCallingUid(), iUserHandleGetCallingUserId);
            }
            Slog.w(LOG_TAG, "No saved token handle");
            return false;
        }
    }

    public boolean isCurrentInputMethodSetByOwner() {
        enforceProfileOwnerOrSystemUser();
        return getUserData(this.mInjector.userHandleGetCallingUserId()).mCurrentInputMethodSet;
    }

    public StringParceledListSlice getOwnerInstalledCaCerts(UserHandle userHandle) {
        StringParceledListSlice stringParceledListSlice;
        int identifier = userHandle.getIdentifier();
        enforceProfileOwnerOrFullCrossUsersPermission(identifier);
        synchronized (getLockObject()) {
            stringParceledListSlice = new StringParceledListSlice(new ArrayList(getUserData(identifier).mOwnerInstalledCaCerts));
        }
        return stringParceledListSlice;
    }

    public void clearApplicationUserData(ComponentName componentName, String str, IPackageDataObserver iPackageDataObserver) {
        Preconditions.checkNotNull(componentName, "ComponentName is null");
        Preconditions.checkNotNull(str, "packageName is null");
        Preconditions.checkNotNull(iPackageDataObserver, "callback is null");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -1);
        }
        int callingUserId = UserHandle.getCallingUserId();
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                ActivityManager.getService().clearApplicationUserData(str, false, iPackageDataObserver, callingUserId);
            } catch (RemoteException e) {
            } catch (SecurityException e2) {
                Slog.w(LOG_TAG, "Not allowed to clear application user data for package " + str, e2);
                try {
                    iPackageDataObserver.onRemoveCompleted(str, false);
                } catch (RemoteException e3) {
                }
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    public void setLogoutEnabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -2);
            if (activeAdminForCallerLocked.isLogoutEnabled == z) {
                return;
            }
            activeAdminForCallerLocked.isLogoutEnabled = z;
            saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
        }
    }

    public boolean isLogoutEnabled() {
        boolean z = false;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (getLockObject()) {
            ActiveAdmin deviceOwnerAdminLocked = getDeviceOwnerAdminLocked();
            if (deviceOwnerAdminLocked != null && deviceOwnerAdminLocked.isLogoutEnabled) {
                z = true;
            }
        }
        return z;
    }

    public List<String> getDisallowedSystemApps(ComponentName componentName, int i, String str) throws RemoteException {
        enforceCanManageProfileAndDeviceOwners();
        return new ArrayList(this.mOverlayPackagesProvider.getNonRequiredApps(componentName, i, str));
    }

    @Override
    public void transferOwnership(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "Admin cannot be null.");
        Preconditions.checkNotNull(componentName2, "Target cannot be null.");
        enforceProfileOrDeviceOwner(componentName);
        if (componentName.equals(componentName2)) {
            throw new IllegalArgumentException("Provided administrator and target are the same object.");
        }
        if (componentName.getPackageName().equals(componentName2.getPackageName())) {
            throw new IllegalArgumentException("Provided administrator and target have the same package name.");
        }
        int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
        DevicePolicyData userData = getUserData(iUserHandleGetCallingUserId);
        DeviceAdminInfo deviceAdminInfoFindAdmin = findAdmin(componentName2, iUserHandleGetCallingUserId, true);
        checkActiveAdminPrecondition(componentName2, deviceAdminInfoFindAdmin, userData);
        if (!deviceAdminInfoFindAdmin.supportsTransferOwnership()) {
            throw new IllegalArgumentException("Provided target does not support ownership transfer.");
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (getLockObject()) {
                if (persistableBundle == null) {
                    try {
                        persistableBundle = new PersistableBundle();
                    } finally {
                    }
                }
                if (isProfileOwner(componentName, iUserHandleGetCallingUserId)) {
                    prepareTransfer(componentName, componentName2, persistableBundle, iUserHandleGetCallingUserId, LOG_TAG_PROFILE_OWNER);
                    transferProfileOwnershipLocked(componentName, componentName2, iUserHandleGetCallingUserId);
                    sendProfileOwnerCommand("android.app.action.TRANSFER_OWNERSHIP_COMPLETE", getTransferOwnershipAdminExtras(persistableBundle), iUserHandleGetCallingUserId);
                    postTransfer("android.app.action.PROFILE_OWNER_CHANGED", iUserHandleGetCallingUserId);
                    if (isUserAffiliatedWithDeviceLocked(iUserHandleGetCallingUserId)) {
                        notifyAffiliatedProfileTransferOwnershipComplete(iUserHandleGetCallingUserId);
                    }
                } else if (isDeviceOwner(componentName, iUserHandleGetCallingUserId)) {
                    prepareTransfer(componentName, componentName2, persistableBundle, iUserHandleGetCallingUserId, LOG_TAG_DEVICE_OWNER);
                    transferDeviceOwnershipLocked(componentName, componentName2, iUserHandleGetCallingUserId);
                    sendDeviceOwnerCommand("android.app.action.TRANSFER_OWNERSHIP_COMPLETE", getTransferOwnershipAdminExtras(persistableBundle));
                    postTransfer("android.app.action.DEVICE_OWNER_CHANGED", iUserHandleGetCallingUserId);
                }
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    private void prepareTransfer(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle, int i, String str) {
        saveTransferOwnershipBundleLocked(persistableBundle, i);
        this.mTransferOwnershipMetadataManager.saveMetadataFile(new TransferOwnershipMetadataManager.Metadata(componentName, componentName2, i, str));
    }

    private void postTransfer(String str, int i) {
        deleteTransferOwnershipMetadataFileLocked();
        sendOwnerChangedBroadcast(str, i);
    }

    private void notifyAffiliatedProfileTransferOwnershipComplete(int i) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("android.intent.extra.USER", UserHandle.of(i));
        sendDeviceOwnerCommand("android.app.action.AFFILIATED_PROFILE_TRANSFER_OWNERSHIP_COMPLETE", bundle);
    }

    private void transferProfileOwnershipLocked(ComponentName componentName, ComponentName componentName2, int i) {
        transferActiveAdminUncheckedLocked(componentName2, componentName, i);
        this.mOwners.transferProfileOwner(componentName2, i);
        Slog.i(LOG_TAG, "Profile owner set: " + componentName2 + " on user " + i);
        this.mOwners.writeProfileOwner(i);
        this.mDeviceAdminServiceController.startServiceForOwner(componentName2.getPackageName(), i, "transfer-profile-owner");
    }

    private void transferDeviceOwnershipLocked(ComponentName componentName, ComponentName componentName2, int i) {
        transferActiveAdminUncheckedLocked(componentName2, componentName, i);
        this.mOwners.transferDeviceOwnership(componentName2);
        Slog.i(LOG_TAG, "Device owner set: " + componentName2 + " on user " + i);
        this.mOwners.writeDeviceOwner();
        this.mDeviceAdminServiceController.startServiceForOwner(componentName2.getPackageName(), i, "transfer-device-owner");
    }

    private Bundle getTransferOwnershipAdminExtras(PersistableBundle persistableBundle) {
        Bundle bundle = new Bundle();
        if (persistableBundle != null) {
            bundle.putParcelable("android.app.extra.TRANSFER_OWNERSHIP_ADMIN_EXTRAS_BUNDLE", persistableBundle);
        }
        return bundle;
    }

    @Override
    public void setStartUserSessionMessage(ComponentName componentName, CharSequence charSequence) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName);
        String string = charSequence != null ? charSequence.toString() : null;
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -2);
            if (TextUtils.equals(activeAdminForCallerLocked.startUserSessionMessage, charSequence)) {
                return;
            }
            activeAdminForCallerLocked.startUserSessionMessage = string;
            saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            this.mInjector.getActivityManagerInternal().setSwitchingFromSystemUserMessage(string);
        }
    }

    @Override
    public void setEndUserSessionMessage(ComponentName componentName, CharSequence charSequence) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName);
        String string = charSequence != null ? charSequence.toString() : null;
        synchronized (getLockObject()) {
            ActiveAdmin activeAdminForCallerLocked = getActiveAdminForCallerLocked(componentName, -2);
            if (TextUtils.equals(activeAdminForCallerLocked.endUserSessionMessage, charSequence)) {
                return;
            }
            activeAdminForCallerLocked.endUserSessionMessage = string;
            saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            this.mInjector.getActivityManagerInternal().setSwitchingToSystemUserMessage(string);
        }
    }

    @Override
    public String getStartUserSessionMessage(ComponentName componentName) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            str = getActiveAdminForCallerLocked(componentName, -2).startUserSessionMessage;
        }
        return str;
    }

    @Override
    public String getEndUserSessionMessage(ComponentName componentName) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(componentName);
        synchronized (getLockObject()) {
            str = getActiveAdminForCallerLocked(componentName, -2).endUserSessionMessage;
        }
        return str;
    }

    private void deleteTransferOwnershipMetadataFileLocked() {
        this.mTransferOwnershipMetadataManager.deleteMetadataFile();
    }

    @Override
    public PersistableBundle getTransferOwnershipBundle() {
        Throwable th;
        synchronized (getLockObject()) {
            int iUserHandleGetCallingUserId = this.mInjector.userHandleGetCallingUserId();
            getActiveAdminForCallerLocked(null, -1);
            File file = new File(this.mInjector.environmentGetUserSystemDirectory(iUserHandleGetCallingUserId), TRANSFER_OWNERSHIP_PARAMETERS_XML);
            if (!file.exists()) {
                return null;
            }
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStream, null);
                    xmlPullParserNewPullParser.next();
                    PersistableBundle persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParserNewPullParser);
                    $closeResource(null, fileInputStream);
                    return persistableBundleRestoreFromXml;
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    $closeResource(th, fileInputStream);
                    throw th;
                }
            } catch (IOException | IllegalArgumentException | XmlPullParserException e) {
                Slog.e(LOG_TAG, "Caught exception while trying to load the owner transfer parameters from file " + file, e);
                return null;
            }
        }
    }

    @Override
    public int addOverrideApn(ComponentName componentName, ApnSetting apnSetting) {
        if (!this.mHasFeature) {
            return -1;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null in addOverrideApn");
        Preconditions.checkNotNull(apnSetting, "ApnSetting is null in addOverrideApn");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            Uri uriInsert = this.mContext.getContentResolver().insert(Telephony.Carriers.DPC_URI, apnSetting.toContentValues());
            if (uriInsert != null) {
                try {
                    return Integer.parseInt(uriInsert.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Slog.e(LOG_TAG, "Failed to parse inserted override APN id.", e);
                }
            }
            return -1;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @Override
    public boolean updateOverrideApn(ComponentName componentName, int i, ApnSetting apnSetting) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null in updateOverrideApn");
        Preconditions.checkNotNull(apnSetting, "ApnSetting is null in updateOverrideApn");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        if (i < 0) {
            return false;
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mContext.getContentResolver().update(Uri.withAppendedPath(Telephony.Carriers.DPC_URI, Integer.toString(i)), apnSetting.toContentValues(), null, null) > 0;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @Override
    public boolean removeOverrideApn(ComponentName componentName, int i) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null in removeOverrideApn");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        return removeOverrideApnUnchecked(i);
    }

    private boolean removeOverrideApnUnchecked(int i) {
        if (i < 0) {
            return false;
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mContext.getContentResolver().delete(Uri.withAppendedPath(Telephony.Carriers.DPC_URI, Integer.toString(i)), null, null) > 0;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @Override
    public List<ApnSetting> getOverrideApns(ComponentName componentName) {
        if (!this.mHasFeature) {
            return Collections.emptyList();
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null in getOverrideApns");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        return getOverrideApnsUnchecked();
    }

    private List<ApnSetting> getOverrideApnsUnchecked() {
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            Cursor cursorQuery = this.mContext.getContentResolver().query(Telephony.Carriers.DPC_URI, null, null, null, null);
            if (cursorQuery == null) {
                return Collections.emptyList();
            }
            try {
                ArrayList arrayList = new ArrayList();
                cursorQuery.moveToPosition(-1);
                while (cursorQuery.moveToNext()) {
                    arrayList.add(ApnSetting.makeApnSetting(cursorQuery));
                }
                return arrayList;
            } finally {
                cursorQuery.close();
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @Override
    public void setOverrideApnsEnabled(ComponentName componentName, boolean z) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null in setOverrideApnEnabled");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        setOverrideApnsEnabledUnchecked(z);
    }

    private void setOverrideApnsEnabledUnchecked(boolean z) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("enforced", Boolean.valueOf(z));
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.getContentResolver().update(Telephony.Carriers.ENFORCE_MANAGED_URI, contentValues, null, null);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @Override
    public boolean isOverrideApnEnabled(ComponentName componentName) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(componentName, "ComponentName is null in isOverrideApnEnabled");
        synchronized (getLockObject()) {
            getActiveAdminForCallerLocked(componentName, -2);
        }
        long jBinderClearCallingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            Cursor cursorQuery = this.mContext.getContentResolver().query(Telephony.Carriers.ENFORCE_MANAGED_URI, null, null, null, null);
            if (cursorQuery == null) {
                return false;
            }
            try {
                try {
                    if (cursorQuery.moveToFirst()) {
                        return cursorQuery.getInt(cursorQuery.getColumnIndex("enforced")) == 1;
                    }
                } catch (IllegalArgumentException e) {
                    Slog.e(LOG_TAG, "Cursor returned from ENFORCE_MANAGED_URI doesn't contain correct info.", e);
                }
                return false;
            } finally {
                cursorQuery.close();
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(jBinderClearCallingIdentity);
        }
    }

    @VisibleForTesting
    void saveTransferOwnershipBundleLocked(PersistableBundle persistableBundle, int i) {
        FileOutputStream fileOutputStreamStartWrite;
        File file = new File(this.mInjector.environmentGetUserSystemDirectory(i), TRANSFER_OWNERSHIP_PARAMETERS_XML);
        AtomicFile atomicFile = new AtomicFile(file);
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
            try {
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, TAG_TRANSFER_OWNERSHIP_BUNDLE);
                persistableBundle.saveToXml(fastXmlSerializer);
                fastXmlSerializer.endTag(null, TAG_TRANSFER_OWNERSHIP_BUNDLE);
                fastXmlSerializer.endDocument();
                atomicFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException | XmlPullParserException e) {
                e = e;
                Slog.e(LOG_TAG, "Caught exception while trying to save the owner transfer parameters to file " + file, e);
                file.delete();
                atomicFile.failWrite(fileOutputStreamStartWrite);
            }
        } catch (IOException | XmlPullParserException e2) {
            e = e2;
            fileOutputStreamStartWrite = null;
        }
    }

    void deleteTransferOwnershipBundleLocked(int i) {
        new File(this.mInjector.environmentGetUserSystemDirectory(i), TRANSFER_OWNERSHIP_PARAMETERS_XML).delete();
    }

    private void maybeLogPasswordComplexitySet(ComponentName componentName, int i, boolean z, PasswordMetrics passwordMetrics) {
        if (SecurityLog.isLoggingEnabled()) {
            SecurityLog.writeEvent(210017, new Object[]{componentName.getPackageName(), Integer.valueOf(i), Integer.valueOf(z ? getProfileParentId(i) : i), Integer.valueOf(passwordMetrics.length), Integer.valueOf(passwordMetrics.quality), Integer.valueOf(passwordMetrics.letters), Integer.valueOf(passwordMetrics.nonLetter), Integer.valueOf(passwordMetrics.numeric), Integer.valueOf(passwordMetrics.upperCase), Integer.valueOf(passwordMetrics.lowerCase), Integer.valueOf(passwordMetrics.symbols)});
        }
    }

    private static String getManagedProvisioningPackage(Context context) {
        return context.getResources().getString(R.string.alwaysUse);
    }
}
