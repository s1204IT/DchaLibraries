package com.android.server.devicepolicy;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.AccountManager;
import android.annotation.IntDef;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.SecurityLog;
import android.app.admin.SystemUpdatePolicy;
import android.app.backup.IBackupManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.IAudioService;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RecoverySystem;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.StorageManager;
import android.provider.ContactsContract;
import android.provider.ContactsInternal;
import android.provider.Settings;
import android.security.Credentials;
import android.security.IKeyChainAliasCallback;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
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
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.ParcelableString;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationFudger;
import com.android.server.notification.ZenModeHelper;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserRestrictionsUtils;
import com.google.android.collect.Sets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DevicePolicyManagerService extends IDevicePolicyManager.Stub {
    protected static final String ACTION_BOOT_IPO = "android.intent.action.ACTION_BOOT_IPO";
    private static final String ACTION_EXPIRED_PASSWORD_NOTIFICATION = "com.android.server.ACTION_EXPIRED_PASSWORD_NOTIFICATION";
    private static final String ATTR_APPLICATION_RESTRICTIONS_MANAGER = "application-restrictions-manager";
    private static final String ATTR_DELEGATED_CERT_INSTALLER = "delegated-cert-installer";
    private static final String ATTR_DISABLED = "disabled";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PERMISSION_POLICY = "permission-policy";
    private static final String ATTR_PERMISSION_PROVIDER = "permission-provider";
    private static final String ATTR_PROVISIONING_STATE = "provisioning-state";
    private static final String ATTR_SETUP_COMPLETE = "setup-complete";
    private static final String ATTR_VALUE = "value";
    private static final int CODE_ACCOUNTS_NOT_EMPTY = 6;
    private static final int CODE_HAS_DEVICE_OWNER = 1;
    private static final int CODE_NONSYSTEM_USER_EXISTS = 5;
    private static final int CODE_NOT_SYSTEM_USER = 7;
    private static final int CODE_OK = 0;
    private static final int CODE_USER_HAS_PROFILE_OWNER = 2;
    private static final int CODE_USER_NOT_RUNNING = 3;
    private static final int CODE_USER_SETUP_COMPLETED = 4;
    private static final int DEVICE_ADMIN_DEACTIVATE_TIMEOUT = 10000;
    private static final String DEVICE_POLICIES_XML = "device_policies.xml";
    private static final String DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML = "do-not-ask-credentials-on-boot";
    private static final long EXPIRATION_GRACE_PERIOD_MS = 432000000;
    private static final Set<String> GLOBAL_SETTINGS_DEPRECATED;
    private static final Set<String> GLOBAL_SETTINGS_WHITELIST;
    private static final String LOG_TAG = "DevicePolicyManagerService";
    private static final int MONITORING_CERT_NOTIFICATION_ID = 18087937;
    private static final long MS_PER_DAY = 86400000;
    private static final int PROFILE_KEYGUARD_FEATURES = 56;
    private static final int PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER = 48;
    private static final int PROFILE_KEYGUARD_FEATURES_PROFILE_ONLY = 8;
    private static final int PROFILE_WIPED_NOTIFICATION_ID = 1001;
    private static final String PROPERTY_DEVICE_OWNER_PRESENT = "ro.device_owner";
    private static final int REQUEST_EXPIRE_PASSWORD = 5571;
    private static final Set<String> SECURE_SETTINGS_DEVICEOWNER_WHITELIST;
    private static final Set<String> SECURE_SETTINGS_WHITELIST = new ArraySet();
    private static final int STATUS_BAR_DISABLE2_MASK = 1;
    private static final int STATUS_BAR_DISABLE_MASK = 34013184;
    private static final String TAG_ACCEPTED_CA_CERTIFICATES = "accepted-ca-certificate";
    private static final String TAG_ADMIN_BROADCAST_PENDING = "admin-broadcast-pending";
    private static final String TAG_AFFILIATION_ID = "affiliation-id";
    private static final String TAG_INITIALIZATION_BUNDLE = "initialization-bundle";
    private static final String TAG_LOCK_TASK_COMPONENTS = "lock-task-component";
    private static final String TAG_STATUS_BAR = "statusbar";
    private static final boolean VERBOSE_LOG = false;
    final Context mContext;
    final Handler mHandler;
    boolean mHasFeature;
    final IPackageManager mIPackageManager;
    final Injector mInjector;
    final LocalService mLocalService;
    private final LockPatternUtils mLockPatternUtils;
    final Owners mOwners;
    private final Set<Pair<String, Integer>> mPackagesToRemove;
    BroadcastReceiver mReceiver;
    private final BroadcastReceiver mRemoteBugreportConsentReceiver;
    private final BroadcastReceiver mRemoteBugreportFinishedReceiver;
    private final AtomicBoolean mRemoteBugreportServiceIsActive;
    private final AtomicBoolean mRemoteBugreportSharingAccepted;
    private final Runnable mRemoteBugreportTimeoutRunnable;
    private final SecurityLogMonitor mSecurityLogMonitor;
    final TelephonyManager mTelephonyManager;
    private final Binder mToken;
    final SparseArray<DevicePolicyData> mUserData;
    final UserManager mUserManager;
    final UserManagerInternal mUserManagerInternal;

    @IntDef({0, ZenModeHelper.SUPPRESSED_EFFECT_NOTIFICATIONS, ZenModeHelper.SUPPRESSED_EFFECT_CALLS, ZenModeHelper.SUPPRESSED_EFFECT_ALL, 4, 7})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DeviceOwnerPreConditionCode {
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
    }

    public static final class Lifecycle extends SystemService {
        private DevicePolicyManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new DevicePolicyManagerService(context);
        }

        @Override
        public void onStart() {
            publishBinderService("device_policy", this.mService);
        }

        @Override
        public void onBootPhase(int phase) {
            this.mService.systemReady(phase);
        }

        @Override
        public void onStartUser(int userHandle) {
            this.mService.onStartUser(userHandle);
        }
    }

    public static class DevicePolicyData {
        String mApplicationRestrictionsManagingPackage;
        String mDelegatedCertInstallerPackage;
        int mPermissionPolicy;
        ComponentName mRestrictionsProvider;
        int mUserHandle;
        int mUserProvisioningState;
        int mActivePasswordQuality = 0;
        int mActivePasswordLength = 0;
        int mActivePasswordUpperCase = 0;
        int mActivePasswordLowerCase = 0;
        int mActivePasswordLetters = 0;
        int mActivePasswordNumeric = 0;
        int mActivePasswordSymbols = 0;
        int mActivePasswordNonLetter = 0;
        int mFailedPasswordAttempts = 0;
        int mPasswordOwner = -1;
        long mLastMaximumTimeToLock = -1;
        boolean mUserSetupComplete = false;
        final ArrayMap<ComponentName, ActiveAdmin> mAdminMap = new ArrayMap<>();
        final ArrayList<ActiveAdmin> mAdminList = new ArrayList<>();
        final ArrayList<ComponentName> mRemovingAdmins = new ArrayList<>();
        final ArraySet<String> mAcceptedCaCertificates = new ArraySet<>();
        List<String> mLockTaskPackages = new ArrayList();
        boolean mStatusBarDisabled = false;
        boolean doNotAskCredentialsOnBoot = false;
        Set<String> mAffiliationIds = new ArraySet();
        boolean mAdminBroadcastPending = false;
        PersistableBundle mInitBundle = null;

        public DevicePolicyData(int userHandle) {
            this.mUserHandle = userHandle;
        }
    }

    static class ActiveAdmin {
        private static final String ATTR_VALUE = "value";
        static final int DEF_KEYGUARD_FEATURES_DISABLED = 0;
        static final int DEF_MAXIMUM_FAILED_PASSWORDS_FOR_WIPE = 0;
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
        private static final String TAG_DISABLE_ACCOUNT_MANAGEMENT = "disable-account-management";
        private static final String TAG_DISABLE_BLUETOOTH_CONTACT_SHARING = "disable-bt-contacts-sharing";
        private static final String TAG_DISABLE_CALLER_ID = "disable-caller-id";
        private static final String TAG_DISABLE_CAMERA = "disable-camera";
        private static final String TAG_DISABLE_CONTACTS_SEARCH = "disable-contacts-search";
        private static final String TAG_DISABLE_KEYGUARD_FEATURES = "disable-keyguard-features";
        private static final String TAG_DISABLE_SCREEN_CAPTURE = "disable-screen-capture";
        private static final String TAG_ENCRYPTION_REQUESTED = "encryption-requested";
        private static final String TAG_FORCE_EPHEMERAL_USERS = "force_ephemeral_users";
        private static final String TAG_GLOBAL_PROXY_EXCLUSION_LIST = "global-proxy-exclusion-list";
        private static final String TAG_GLOBAL_PROXY_SPEC = "global-proxy-spec";
        private static final String TAG_KEEP_UNINSTALLED_PACKAGES = "keep-uninstalled-packages";
        private static final String TAG_LONG_SUPPORT_MESSAGE = "long-support-message";
        private static final String TAG_MANAGE_TRUST_AGENT_FEATURES = "manage-trust-agent-features";
        private static final String TAG_MAX_FAILED_PASSWORD_WIPE = "max-failed-password-wipe";
        private static final String TAG_MAX_TIME_TO_UNLOCK = "max-time-to-unlock";
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
        private static final String TAG_POLICIES = "policies";
        private static final String TAG_PROVIDER = "provider";
        private static final String TAG_REQUIRE_AUTO_TIME = "require_auto_time";
        private static final String TAG_SHORT_SUPPORT_MESSAGE = "short-support-message";
        private static final String TAG_SPECIFIES_GLOBAL_PROXY = "specifies-global-proxy";
        private static final String TAG_TRUST_AGENT_COMPONENT = "component";
        private static final String TAG_TRUST_AGENT_COMPONENT_OPTIONS = "trust-agent-component-options";
        private static final String TAG_USER_RESTRICTIONS = "user-restrictions";
        List<String> crossProfileWidgetProviders;
        final DeviceAdminInfo info;
        final boolean isParent;
        List<String> keepUninstalledPackages;
        ActiveAdmin parentAdmin;
        List<String> permittedAccessiblityServices;
        List<String> permittedInputMethods;
        Bundle userRestrictions;
        int passwordQuality = 0;
        int minimumPasswordLength = 0;
        int passwordHistoryLength = 0;
        int minimumPasswordUpperCase = 0;
        int minimumPasswordLowerCase = 0;
        int minimumPasswordLetters = 1;
        int minimumPasswordNumeric = 1;
        int minimumPasswordSymbols = 1;
        int minimumPasswordNonLetter = 0;
        long maximumTimeToUnlock = 0;
        int maximumFailedPasswordsForWipe = 0;
        long passwordExpirationTimeout = 0;
        long passwordExpirationDate = 0;
        int disabledKeyguardFeatures = 0;
        boolean encryptionRequested = false;
        boolean disableCamera = false;
        boolean disableCallerId = false;
        boolean disableContactsSearch = false;
        boolean disableBluetoothContactSharing = true;
        boolean disableScreenCapture = false;
        boolean requireAutoTime = false;
        boolean forceEphemeralUsers = false;
        Set<String> accountTypesWithManagementDisabled = new ArraySet();
        boolean specifiesGlobalProxy = false;
        String globalProxySpec = null;
        String globalProxyExclusionList = null;
        ArrayMap<String, TrustAgentInfo> trustAgentInfos = new ArrayMap<>();
        CharSequence shortSupportMessage = null;
        CharSequence longSupportMessage = null;
        int organizationColor = DEF_ORGANIZATION_COLOR;
        String organizationName = null;

        static class TrustAgentInfo {
            public PersistableBundle options;

            TrustAgentInfo(PersistableBundle bundle) {
                this.options = bundle;
            }
        }

        ActiveAdmin(DeviceAdminInfo _info, boolean parent) {
            this.info = _info;
            this.isParent = parent;
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

        void writeToXml(XmlSerializer out) throws IllegalStateException, IOException, IllegalArgumentException {
            out.startTag(null, TAG_POLICIES);
            this.info.writePoliciesToXml(out);
            out.endTag(null, TAG_POLICIES);
            if (this.passwordQuality != 0) {
                out.startTag(null, TAG_PASSWORD_QUALITY);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.passwordQuality));
                out.endTag(null, TAG_PASSWORD_QUALITY);
                if (this.minimumPasswordLength != 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_LENGTH);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordLength));
                    out.endTag(null, TAG_MIN_PASSWORD_LENGTH);
                }
                if (this.passwordHistoryLength != 0) {
                    out.startTag(null, TAG_PASSWORD_HISTORY_LENGTH);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.passwordHistoryLength));
                    out.endTag(null, TAG_PASSWORD_HISTORY_LENGTH);
                }
                if (this.minimumPasswordUpperCase != 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_UPPERCASE);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordUpperCase));
                    out.endTag(null, TAG_MIN_PASSWORD_UPPERCASE);
                }
                if (this.minimumPasswordLowerCase != 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_LOWERCASE);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordLowerCase));
                    out.endTag(null, TAG_MIN_PASSWORD_LOWERCASE);
                }
                if (this.minimumPasswordLetters != 1) {
                    out.startTag(null, TAG_MIN_PASSWORD_LETTERS);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordLetters));
                    out.endTag(null, TAG_MIN_PASSWORD_LETTERS);
                }
                if (this.minimumPasswordNumeric != 1) {
                    out.startTag(null, TAG_MIN_PASSWORD_NUMERIC);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordNumeric));
                    out.endTag(null, TAG_MIN_PASSWORD_NUMERIC);
                }
                if (this.minimumPasswordSymbols != 1) {
                    out.startTag(null, TAG_MIN_PASSWORD_SYMBOLS);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordSymbols));
                    out.endTag(null, TAG_MIN_PASSWORD_SYMBOLS);
                }
                if (this.minimumPasswordNonLetter > 0) {
                    out.startTag(null, TAG_MIN_PASSWORD_NONLETTER);
                    out.attribute(null, ATTR_VALUE, Integer.toString(this.minimumPasswordNonLetter));
                    out.endTag(null, TAG_MIN_PASSWORD_NONLETTER);
                }
            }
            if (this.maximumTimeToUnlock != 0) {
                out.startTag(null, TAG_MAX_TIME_TO_UNLOCK);
                out.attribute(null, ATTR_VALUE, Long.toString(this.maximumTimeToUnlock));
                out.endTag(null, TAG_MAX_TIME_TO_UNLOCK);
            }
            if (this.maximumFailedPasswordsForWipe != 0) {
                out.startTag(null, TAG_MAX_FAILED_PASSWORD_WIPE);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.maximumFailedPasswordsForWipe));
                out.endTag(null, TAG_MAX_FAILED_PASSWORD_WIPE);
            }
            if (this.specifiesGlobalProxy) {
                out.startTag(null, TAG_SPECIFIES_GLOBAL_PROXY);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.specifiesGlobalProxy));
                out.endTag(null, TAG_SPECIFIES_GLOBAL_PROXY);
                if (this.globalProxySpec != null) {
                    out.startTag(null, TAG_GLOBAL_PROXY_SPEC);
                    out.attribute(null, ATTR_VALUE, this.globalProxySpec);
                    out.endTag(null, TAG_GLOBAL_PROXY_SPEC);
                }
                if (this.globalProxyExclusionList != null) {
                    out.startTag(null, TAG_GLOBAL_PROXY_EXCLUSION_LIST);
                    out.attribute(null, ATTR_VALUE, this.globalProxyExclusionList);
                    out.endTag(null, TAG_GLOBAL_PROXY_EXCLUSION_LIST);
                }
            }
            if (this.passwordExpirationTimeout != 0) {
                out.startTag(null, TAG_PASSWORD_EXPIRATION_TIMEOUT);
                out.attribute(null, ATTR_VALUE, Long.toString(this.passwordExpirationTimeout));
                out.endTag(null, TAG_PASSWORD_EXPIRATION_TIMEOUT);
            }
            if (this.passwordExpirationDate != 0) {
                out.startTag(null, TAG_PASSWORD_EXPIRATION_DATE);
                out.attribute(null, ATTR_VALUE, Long.toString(this.passwordExpirationDate));
                out.endTag(null, TAG_PASSWORD_EXPIRATION_DATE);
            }
            if (this.encryptionRequested) {
                out.startTag(null, TAG_ENCRYPTION_REQUESTED);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.encryptionRequested));
                out.endTag(null, TAG_ENCRYPTION_REQUESTED);
            }
            if (this.disableCamera) {
                out.startTag(null, TAG_DISABLE_CAMERA);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableCamera));
                out.endTag(null, TAG_DISABLE_CAMERA);
            }
            if (this.disableCallerId) {
                out.startTag(null, TAG_DISABLE_CALLER_ID);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableCallerId));
                out.endTag(null, TAG_DISABLE_CALLER_ID);
            }
            if (this.disableContactsSearch) {
                out.startTag(null, TAG_DISABLE_CONTACTS_SEARCH);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableContactsSearch));
                out.endTag(null, TAG_DISABLE_CONTACTS_SEARCH);
            }
            if (!this.disableBluetoothContactSharing) {
                out.startTag(null, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableBluetoothContactSharing));
                out.endTag(null, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING);
            }
            if (this.disableScreenCapture) {
                out.startTag(null, TAG_DISABLE_SCREEN_CAPTURE);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.disableScreenCapture));
                out.endTag(null, TAG_DISABLE_SCREEN_CAPTURE);
            }
            if (this.requireAutoTime) {
                out.startTag(null, TAG_REQUIRE_AUTO_TIME);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.requireAutoTime));
                out.endTag(null, TAG_REQUIRE_AUTO_TIME);
            }
            if (this.forceEphemeralUsers) {
                out.startTag(null, TAG_FORCE_EPHEMERAL_USERS);
                out.attribute(null, ATTR_VALUE, Boolean.toString(this.forceEphemeralUsers));
                out.endTag(null, TAG_FORCE_EPHEMERAL_USERS);
            }
            if (this.disabledKeyguardFeatures != 0) {
                out.startTag(null, TAG_DISABLE_KEYGUARD_FEATURES);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.disabledKeyguardFeatures));
                out.endTag(null, TAG_DISABLE_KEYGUARD_FEATURES);
            }
            if (!this.accountTypesWithManagementDisabled.isEmpty()) {
                out.startTag(null, TAG_DISABLE_ACCOUNT_MANAGEMENT);
                for (String ac : this.accountTypesWithManagementDisabled) {
                    out.startTag(null, TAG_ACCOUNT_TYPE);
                    out.attribute(null, ATTR_VALUE, ac);
                    out.endTag(null, TAG_ACCOUNT_TYPE);
                }
                out.endTag(null, TAG_DISABLE_ACCOUNT_MANAGEMENT);
            }
            if (!this.trustAgentInfos.isEmpty()) {
                Set<Map.Entry<String, TrustAgentInfo>> set = this.trustAgentInfos.entrySet();
                out.startTag(null, TAG_MANAGE_TRUST_AGENT_FEATURES);
                for (Map.Entry<String, TrustAgentInfo> entry : set) {
                    TrustAgentInfo trustAgentInfo = entry.getValue();
                    out.startTag(null, TAG_TRUST_AGENT_COMPONENT);
                    out.attribute(null, ATTR_VALUE, entry.getKey());
                    if (trustAgentInfo.options != null) {
                        out.startTag(null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                        try {
                            trustAgentInfo.options.saveToXml(out);
                        } catch (XmlPullParserException e) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "Failed to save TrustAgent options", e);
                        }
                        out.endTag(null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                    }
                    out.endTag(null, TAG_TRUST_AGENT_COMPONENT);
                }
                out.endTag(null, TAG_MANAGE_TRUST_AGENT_FEATURES);
            }
            if (this.crossProfileWidgetProviders != null && !this.crossProfileWidgetProviders.isEmpty()) {
                out.startTag(null, TAG_CROSS_PROFILE_WIDGET_PROVIDERS);
                int providerCount = this.crossProfileWidgetProviders.size();
                for (int i = 0; i < providerCount; i++) {
                    String provider = this.crossProfileWidgetProviders.get(i);
                    out.startTag(null, TAG_PROVIDER);
                    out.attribute(null, ATTR_VALUE, provider);
                    out.endTag(null, TAG_PROVIDER);
                }
                out.endTag(null, TAG_CROSS_PROFILE_WIDGET_PROVIDERS);
            }
            writePackageListToXml(out, TAG_PERMITTED_ACCESSIBILITY_SERVICES, this.permittedAccessiblityServices);
            writePackageListToXml(out, TAG_PERMITTED_IMES, this.permittedInputMethods);
            writePackageListToXml(out, TAG_KEEP_UNINSTALLED_PACKAGES, this.keepUninstalledPackages);
            if (hasUserRestrictions()) {
                UserRestrictionsUtils.writeRestrictions(out, this.userRestrictions, TAG_USER_RESTRICTIONS);
            }
            if (!TextUtils.isEmpty(this.shortSupportMessage)) {
                out.startTag(null, TAG_SHORT_SUPPORT_MESSAGE);
                out.text(this.shortSupportMessage.toString());
                out.endTag(null, TAG_SHORT_SUPPORT_MESSAGE);
            }
            if (!TextUtils.isEmpty(this.longSupportMessage)) {
                out.startTag(null, TAG_LONG_SUPPORT_MESSAGE);
                out.text(this.longSupportMessage.toString());
                out.endTag(null, TAG_LONG_SUPPORT_MESSAGE);
            }
            if (this.parentAdmin != null) {
                out.startTag(null, TAG_PARENT_ADMIN);
                this.parentAdmin.writeToXml(out);
                out.endTag(null, TAG_PARENT_ADMIN);
            }
            if (this.organizationColor != DEF_ORGANIZATION_COLOR) {
                out.startTag(null, TAG_ORGANIZATION_COLOR);
                out.attribute(null, ATTR_VALUE, Integer.toString(this.organizationColor));
                out.endTag(null, TAG_ORGANIZATION_COLOR);
            }
            if (this.organizationName == null) {
                return;
            }
            out.startTag(null, TAG_ORGANIZATION_NAME);
            out.text(this.organizationName);
            out.endTag(null, TAG_ORGANIZATION_NAME);
        }

        void writePackageListToXml(XmlSerializer out, String outerTag, List<String> packageList) throws IllegalStateException, IOException, IllegalArgumentException {
            if (packageList == null) {
                return;
            }
            out.startTag(null, outerTag);
            for (String packageName : packageList) {
                out.startTag(null, TAG_PACKAGE_LIST_ITEM);
                out.attribute(null, ATTR_VALUE, packageName);
                out.endTag(null, TAG_PACKAGE_LIST_ITEM);
            }
            out.endTag(null, outerTag);
        }

        void readFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1) {
                    return;
                }
                if (type == 3 && parser.getDepth() <= outerDepth) {
                    return;
                }
                if (type != 3 && type != 4) {
                    String tag = parser.getName();
                    if (TAG_POLICIES.equals(tag)) {
                        this.info.readPoliciesFromXml(parser);
                    } else if (TAG_PASSWORD_QUALITY.equals(tag)) {
                        this.passwordQuality = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_LENGTH.equals(tag)) {
                        this.minimumPasswordLength = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_PASSWORD_HISTORY_LENGTH.equals(tag)) {
                        this.passwordHistoryLength = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_UPPERCASE.equals(tag)) {
                        this.minimumPasswordUpperCase = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_LOWERCASE.equals(tag)) {
                        this.minimumPasswordLowerCase = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_LETTERS.equals(tag)) {
                        this.minimumPasswordLetters = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_NUMERIC.equals(tag)) {
                        this.minimumPasswordNumeric = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_SYMBOLS.equals(tag)) {
                        this.minimumPasswordSymbols = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MIN_PASSWORD_NONLETTER.equals(tag)) {
                        this.minimumPasswordNonLetter = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MAX_TIME_TO_UNLOCK.equals(tag)) {
                        this.maximumTimeToUnlock = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_MAX_FAILED_PASSWORD_WIPE.equals(tag)) {
                        this.maximumFailedPasswordsForWipe = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_SPECIFIES_GLOBAL_PROXY.equals(tag)) {
                        this.specifiesGlobalProxy = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_GLOBAL_PROXY_SPEC.equals(tag)) {
                        this.globalProxySpec = parser.getAttributeValue(null, ATTR_VALUE);
                    } else if (TAG_GLOBAL_PROXY_EXCLUSION_LIST.equals(tag)) {
                        this.globalProxyExclusionList = parser.getAttributeValue(null, ATTR_VALUE);
                    } else if (TAG_PASSWORD_EXPIRATION_TIMEOUT.equals(tag)) {
                        this.passwordExpirationTimeout = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_PASSWORD_EXPIRATION_DATE.equals(tag)) {
                        this.passwordExpirationDate = Long.parseLong(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_ENCRYPTION_REQUESTED.equals(tag)) {
                        this.encryptionRequested = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_CAMERA.equals(tag)) {
                        this.disableCamera = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_CALLER_ID.equals(tag)) {
                        this.disableCallerId = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_CONTACTS_SEARCH.equals(tag)) {
                        this.disableContactsSearch = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_BLUETOOTH_CONTACT_SHARING.equals(tag)) {
                        this.disableBluetoothContactSharing = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_SCREEN_CAPTURE.equals(tag)) {
                        this.disableScreenCapture = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_REQUIRE_AUTO_TIME.equals(tag)) {
                        this.requireAutoTime = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_FORCE_EPHEMERAL_USERS.equals(tag)) {
                        this.forceEphemeralUsers = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_KEYGUARD_FEATURES.equals(tag)) {
                        this.disabledKeyguardFeatures = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_DISABLE_ACCOUNT_MANAGEMENT.equals(tag)) {
                        this.accountTypesWithManagementDisabled = readDisableAccountInfo(parser, tag);
                    } else if (TAG_MANAGE_TRUST_AGENT_FEATURES.equals(tag)) {
                        this.trustAgentInfos = getAllTrustAgentInfos(parser, tag);
                    } else if (TAG_CROSS_PROFILE_WIDGET_PROVIDERS.equals(tag)) {
                        this.crossProfileWidgetProviders = getCrossProfileWidgetProviders(parser, tag);
                    } else if (TAG_PERMITTED_ACCESSIBILITY_SERVICES.equals(tag)) {
                        this.permittedAccessiblityServices = readPackageList(parser, tag);
                    } else if (TAG_PERMITTED_IMES.equals(tag)) {
                        this.permittedInputMethods = readPackageList(parser, tag);
                    } else if (TAG_KEEP_UNINSTALLED_PACKAGES.equals(tag)) {
                        this.keepUninstalledPackages = readPackageList(parser, tag);
                    } else if (TAG_USER_RESTRICTIONS.equals(tag)) {
                        UserRestrictionsUtils.readRestrictions(parser, ensureUserRestrictions());
                    } else if (TAG_SHORT_SUPPORT_MESSAGE.equals(tag)) {
                        if (parser.next() == 4) {
                            this.shortSupportMessage = parser.getText();
                        } else {
                            Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading short support message");
                        }
                    } else if (TAG_LONG_SUPPORT_MESSAGE.equals(tag)) {
                        if (parser.next() == 4) {
                            this.longSupportMessage = parser.getText();
                        } else {
                            Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading long support message");
                        }
                    } else if (TAG_PARENT_ADMIN.equals(tag)) {
                        Preconditions.checkState(!this.isParent);
                        this.parentAdmin = new ActiveAdmin(this.info, true);
                        this.parentAdmin.readFromXml(parser);
                    } else if (TAG_ORGANIZATION_COLOR.equals(tag)) {
                        this.organizationColor = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                    } else if (TAG_ORGANIZATION_NAME.equals(tag)) {
                        if (parser.next() == 4) {
                            this.organizationName = parser.getText();
                        } else {
                            Log.w(DevicePolicyManagerService.LOG_TAG, "Missing text when loading organization name");
                        }
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown admin tag: " + tag);
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }

        private List<String> readPackageList(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            List<String> result = new ArrayList<>();
            int outerDepth = parser.getDepth();
            while (true) {
                int outerType = parser.next();
                if (outerType == 1 || (outerType == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
                if (outerType != 3 && outerType != 4) {
                    String outerTag = parser.getName();
                    if (TAG_PACKAGE_LIST_ITEM.equals(outerTag)) {
                        String packageName = parser.getAttributeValue(null, ATTR_VALUE);
                        if (packageName != null) {
                            result.add(packageName);
                        } else {
                            Slog.w(DevicePolicyManagerService.LOG_TAG, "Package name missing under " + outerTag);
                        }
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + outerTag);
                    }
                }
            }
            return result;
        }

        private Set<String> readDisableAccountInfo(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            Set<String> result = new ArraySet<>();
            while (true) {
                int typeDAM = parser.next();
                if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    break;
                }
                if (typeDAM != 3 && typeDAM != 4) {
                    String tagDAM = parser.getName();
                    if (TAG_ACCOUNT_TYPE.equals(tagDAM)) {
                        result.add(parser.getAttributeValue(null, ATTR_VALUE));
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + tagDAM);
                    }
                }
            }
            return result;
        }

        private ArrayMap<String, TrustAgentInfo> getAllTrustAgentInfos(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            ArrayMap<String, TrustAgentInfo> result = new ArrayMap<>();
            while (true) {
                int typeDAM = parser.next();
                if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    break;
                }
                if (typeDAM != 3 && typeDAM != 4) {
                    String tagDAM = parser.getName();
                    if (TAG_TRUST_AGENT_COMPONENT.equals(tagDAM)) {
                        String component = parser.getAttributeValue(null, ATTR_VALUE);
                        TrustAgentInfo trustAgentInfo = getTrustAgentInfo(parser, tag);
                        result.put(component, trustAgentInfo);
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + tagDAM);
                    }
                }
            }
            return result;
        }

        private TrustAgentInfo getTrustAgentInfo(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            TrustAgentInfo result = new TrustAgentInfo(null);
            while (true) {
                int typeDAM = parser.next();
                if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    break;
                }
                if (typeDAM != 3 && typeDAM != 4) {
                    String tagDAM = parser.getName();
                    if (TAG_TRUST_AGENT_COMPONENT_OPTIONS.equals(tagDAM)) {
                        result.options = PersistableBundle.restoreFromXml(parser);
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + tagDAM);
                    }
                }
            }
            return result;
        }

        private List<String> getCrossProfileWidgetProviders(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            ArrayList arrayList = null;
            while (true) {
                int typeDAM = parser.next();
                if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    break;
                }
                if (typeDAM != 3 && typeDAM != 4) {
                    String tagDAM = parser.getName();
                    if (TAG_PROVIDER.equals(tagDAM)) {
                        String provider = parser.getAttributeValue(null, ATTR_VALUE);
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(provider);
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + tagDAM);
                    }
                }
            }
            return arrayList;
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

        void dump(String prefix, PrintWriter pw) {
            pw.print(prefix);
            pw.print("uid=");
            pw.println(getUid());
            pw.print(prefix);
            pw.println("policies:");
            ArrayList<DeviceAdminInfo.PolicyInfo> pols = this.info.getUsedPolicies();
            if (pols != null) {
                for (int i = 0; i < pols.size(); i++) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.println(pols.get(i).tag);
                }
            }
            pw.print(prefix);
            pw.print("passwordQuality=0x");
            pw.println(Integer.toHexString(this.passwordQuality));
            pw.print(prefix);
            pw.print("minimumPasswordLength=");
            pw.println(this.minimumPasswordLength);
            pw.print(prefix);
            pw.print("passwordHistoryLength=");
            pw.println(this.passwordHistoryLength);
            pw.print(prefix);
            pw.print("minimumPasswordUpperCase=");
            pw.println(this.minimumPasswordUpperCase);
            pw.print(prefix);
            pw.print("minimumPasswordLowerCase=");
            pw.println(this.minimumPasswordLowerCase);
            pw.print(prefix);
            pw.print("minimumPasswordLetters=");
            pw.println(this.minimumPasswordLetters);
            pw.print(prefix);
            pw.print("minimumPasswordNumeric=");
            pw.println(this.minimumPasswordNumeric);
            pw.print(prefix);
            pw.print("minimumPasswordSymbols=");
            pw.println(this.minimumPasswordSymbols);
            pw.print(prefix);
            pw.print("minimumPasswordNonLetter=");
            pw.println(this.minimumPasswordNonLetter);
            pw.print(prefix);
            pw.print("maximumTimeToUnlock=");
            pw.println(this.maximumTimeToUnlock);
            pw.print(prefix);
            pw.print("maximumFailedPasswordsForWipe=");
            pw.println(this.maximumFailedPasswordsForWipe);
            pw.print(prefix);
            pw.print("specifiesGlobalProxy=");
            pw.println(this.specifiesGlobalProxy);
            pw.print(prefix);
            pw.print("passwordExpirationTimeout=");
            pw.println(this.passwordExpirationTimeout);
            pw.print(prefix);
            pw.print("passwordExpirationDate=");
            pw.println(this.passwordExpirationDate);
            if (this.globalProxySpec != null) {
                pw.print(prefix);
                pw.print("globalProxySpec=");
                pw.println(this.globalProxySpec);
            }
            if (this.globalProxyExclusionList != null) {
                pw.print(prefix);
                pw.print("globalProxyEclusionList=");
                pw.println(this.globalProxyExclusionList);
            }
            pw.print(prefix);
            pw.print("encryptionRequested=");
            pw.println(this.encryptionRequested);
            pw.print(prefix);
            pw.print("disableCamera=");
            pw.println(this.disableCamera);
            pw.print(prefix);
            pw.print("disableCallerId=");
            pw.println(this.disableCallerId);
            pw.print(prefix);
            pw.print("disableContactsSearch=");
            pw.println(this.disableContactsSearch);
            pw.print(prefix);
            pw.print("disableBluetoothContactSharing=");
            pw.println(this.disableBluetoothContactSharing);
            pw.print(prefix);
            pw.print("disableScreenCapture=");
            pw.println(this.disableScreenCapture);
            pw.print(prefix);
            pw.print("requireAutoTime=");
            pw.println(this.requireAutoTime);
            pw.print(prefix);
            pw.print("forceEphemeralUsers=");
            pw.println(this.forceEphemeralUsers);
            pw.print(prefix);
            pw.print("disabledKeyguardFeatures=");
            pw.println(this.disabledKeyguardFeatures);
            pw.print(prefix);
            pw.print("crossProfileWidgetProviders=");
            pw.println(this.crossProfileWidgetProviders);
            if (this.permittedAccessiblityServices != null) {
                pw.print(prefix);
                pw.print("permittedAccessibilityServices=");
                pw.println(this.permittedAccessiblityServices);
            }
            if (this.permittedInputMethods != null) {
                pw.print(prefix);
                pw.print("permittedInputMethods=");
                pw.println(this.permittedInputMethods);
            }
            if (this.keepUninstalledPackages != null) {
                pw.print(prefix);
                pw.print("keepUninstalledPackages=");
                pw.println(this.keepUninstalledPackages);
            }
            pw.print(prefix);
            pw.print("organizationColor=");
            pw.println(this.organizationColor);
            if (this.organizationName != null) {
                pw.print(prefix);
                pw.print("organizationName=");
                pw.println(this.organizationName);
            }
            pw.print(prefix);
            pw.println("userRestrictions:");
            UserRestrictionsUtils.dumpRestrictions(pw, prefix + "  ", this.userRestrictions);
            pw.print(prefix);
            pw.print("isParent=");
            pw.println(this.isParent);
            if (this.parentAdmin == null) {
                return;
            }
            pw.print(prefix);
            pw.println("parentAdmin:");
            this.parentAdmin.dump(prefix + "  ", pw);
        }
    }

    private void handlePackagesChanged(String packageName, int userHandle) {
        boolean removed = false;
        DevicePolicyData policy = getUserData(userHandle);
        synchronized (this) {
            for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
                ActiveAdmin aa = policy.mAdminList.get(i);
                try {
                    String adminPackage = aa.info.getPackageName();
                    if ((packageName == null || packageName.equals(adminPackage)) && (this.mIPackageManager.getPackageInfo(adminPackage, 0, userHandle) == null || this.mIPackageManager.getReceiverInfo(aa.info.getComponent(), 786432, userHandle) == null)) {
                        removed = true;
                        policy.mAdminList.remove(i);
                        policy.mAdminMap.remove(aa.info.getComponent());
                    }
                } catch (RemoteException e) {
                }
            }
            if (removed) {
                validatePasswordOwnerLocked(policy);
                saveSettingsLocked(policy.mUserHandle);
            }
            if (isRemovedPackage(packageName, policy.mDelegatedCertInstallerPackage, userHandle)) {
                policy.mDelegatedCertInstallerPackage = null;
                saveSettingsLocked(policy.mUserHandle);
            }
            if (isRemovedPackage(packageName, policy.mApplicationRestrictionsManagingPackage, userHandle)) {
                policy.mApplicationRestrictionsManagingPackage = null;
                saveSettingsLocked(policy.mUserHandle);
            }
        }
        if (!removed) {
            return;
        }
        pushUserRestrictions(userHandle);
    }

    private boolean isRemovedPackage(String changedPackage, String targetPackage, int userHandle) {
        if (targetPackage == null) {
            return false;
        }
        if (changedPackage != null) {
            try {
                if (!changedPackage.equals(targetPackage)) {
                    return false;
                }
            } catch (RemoteException e) {
                return false;
            }
        }
        return this.mIPackageManager.getPackageInfo(targetPackage, 0, userHandle) == null;
    }

    static class Injector {
        private final Context mContext;

        Injector(Context context) {
            this.mContext = context;
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

        NotificationManager getNotificationManager() {
            return (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
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

        IWindowManager getIWindowManager() {
            return IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }

        IActivityManager getIActivityManager() {
            return ActivityManagerNative.getDefault();
        }

        IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        IBackupManager getIBackupManager() {
            return IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        }

        IAudioService getIAudioService() {
            return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        }

        LockPatternUtils newLockPatternUtils() {
            return new LockPatternUtils(this.mContext);
        }

        boolean storageManagerIsFileBasedEncryptionEnabled() {
            return StorageManager.isFileEncryptedNativeOnly();
        }

        boolean storageManagerIsNonDefaultBlockEncrypted() {
            long identity = Binder.clearCallingIdentity();
            try {
                return StorageManager.isNonDefaultBlockEncrypted();
            } finally {
                Binder.restoreCallingIdentity(identity);
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

        void binderRestoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
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
            return DevicePolicyManagerService.getCallingUid() == Process.myUid();
        }

        final int userHandleGetCallingUserId() {
            return UserHandle.getUserId(binderGetCallingUid());
        }

        File environmentGetUserSystemDirectory(int userId) {
            return Environment.getUserSystemDirectory(userId);
        }

        void powerManagerGoToSleep(long time, int reason, int flags) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).goToSleep(time, reason, flags);
        }

        void powerManagerReboot(String reason) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(reason);
        }

        boolean systemPropertiesGetBoolean(String key, boolean def) {
            return SystemProperties.getBoolean(key, def);
        }

        long systemPropertiesGetLong(String key, long def) {
            return SystemProperties.getLong(key, def);
        }

        String systemPropertiesGet(String key, String def) {
            return SystemProperties.get(key, def);
        }

        String systemPropertiesGet(String key) {
            return SystemProperties.get(key);
        }

        void systemPropertiesSet(String key, String value) {
            SystemProperties.set(key, value);
        }

        boolean userManagerIsSplitSystemUser() {
            return UserManager.isSplitSystemUser();
        }

        String getDevicePolicyFilePathForSystemUser() {
            return "/data/system/";
        }

        void registerContentObserver(Uri uri, boolean notifyForDescendents, ContentObserver observer, int userHandle) {
            this.mContext.getContentResolver().registerContentObserver(uri, notifyForDescendents, observer, userHandle);
        }

        int settingsSecureGetIntForUser(String name, int def, int userHandle) {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), name, def, userHandle);
        }

        void settingsSecurePutIntForUser(String name, int value, int userHandle) {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsSecurePutStringForUser(String name, String value, int userHandle) {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsGlobalPutStringForUser(String name, String value, int userHandle) {
            Settings.Global.putStringForUser(this.mContext.getContentResolver(), name, value, userHandle);
        }

        void settingsSecurePutInt(String name, int value) {
            Settings.Secure.putInt(this.mContext.getContentResolver(), name, value);
        }

        int settingsGlobalGetInt(String name, int def) {
            return Settings.Global.getInt(this.mContext.getContentResolver(), name, def);
        }

        void settingsGlobalPutInt(String name, int value) {
            Settings.Global.putInt(this.mContext.getContentResolver(), name, value);
        }

        void settingsSecurePutString(String name, String value) {
            Settings.Secure.putString(this.mContext.getContentResolver(), name, value);
        }

        void settingsGlobalPutString(String name, String value) {
            Settings.Global.putString(this.mContext.getContentResolver(), name, value);
        }

        void securityLogSetLoggingEnabledProperty(boolean enabled) {
            SecurityLog.setLoggingEnabledProperty(enabled);
        }

        boolean securityLogGetLoggingEnabledProperty() {
            return SecurityLog.getLoggingEnabledProperty();
        }

        boolean securityLogIsLoggingEnabled() {
            return SecurityLog.isLoggingEnabled();
        }
    }

    public DevicePolicyManagerService(Context context) {
        this(new Injector(context));
    }

    DevicePolicyManagerService(Injector injector) {
        this.mPackagesToRemove = new ArraySet();
        this.mToken = new Binder();
        this.mRemoteBugreportServiceIsActive = new AtomicBoolean();
        this.mRemoteBugreportSharingAccepted = new AtomicBoolean();
        this.mRemoteBugreportTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!DevicePolicyManagerService.this.mRemoteBugreportServiceIsActive.get()) {
                    return;
                }
                DevicePolicyManagerService.this.onBugreportFailed();
            }
        };
        this.mRemoteBugreportFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!"android.intent.action.REMOTE_BUGREPORT_DISPATCH".equals(intent.getAction()) || !DevicePolicyManagerService.this.mRemoteBugreportServiceIsActive.get()) {
                    return;
                }
                DevicePolicyManagerService.this.onBugreportFinished(intent);
            }
        };
        this.mRemoteBugreportConsentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                DevicePolicyManagerService.this.mInjector.getNotificationManager().cancel(DevicePolicyManagerService.LOG_TAG, 678432343);
                if ("com.android.server.action.BUGREPORT_SHARING_ACCEPTED".equals(action)) {
                    DevicePolicyManagerService.this.onBugreportSharingAccepted();
                } else if ("com.android.server.action.BUGREPORT_SHARING_DECLINED".equals(action)) {
                    DevicePolicyManagerService.this.onBugreportSharingDeclined();
                }
                DevicePolicyManagerService.this.mContext.unregisterReceiver(DevicePolicyManagerService.this.mRemoteBugreportConsentReceiver);
            }
        };
        this.mUserData = new SparseArray<>();
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MonitoringCertNotificationTask monitoringCertNotificationTask = null;
                String action = intent.getAction();
                final int userHandle = intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId());
                if ("android.intent.action.BOOT_COMPLETED".equals(action) && userHandle == DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserId() && DevicePolicyManagerService.this.getDeviceOwnerRemoteBugreportUri() != null) {
                    IntentFilter filterConsent = new IntentFilter();
                    filterConsent.addAction("com.android.server.action.BUGREPORT_SHARING_DECLINED");
                    filterConsent.addAction("com.android.server.action.BUGREPORT_SHARING_ACCEPTED");
                    DevicePolicyManagerService.this.mContext.registerReceiver(DevicePolicyManagerService.this.mRemoteBugreportConsentReceiver, filterConsent);
                    DevicePolicyManagerService.this.mInjector.getNotificationManager().notifyAsUser(DevicePolicyManagerService.LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(DevicePolicyManagerService.this.mContext, 3), UserHandle.ALL);
                }
                if ("android.intent.action.BOOT_COMPLETED".equals(action) || "android.intent.action.ACTION_BOOT_IPO".equals(action) || DevicePolicyManagerService.ACTION_EXPIRED_PASSWORD_NOTIFICATION.equals(action)) {
                    DevicePolicyManagerService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            DevicePolicyManagerService.this.handlePasswordExpirationNotification(userHandle);
                        }
                    });
                }
                if ("android.intent.action.USER_UNLOCKED".equals(action) || "android.intent.action.USER_STARTED".equals(action) || "android.security.STORAGE_CHANGED".equals(action)) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    new MonitoringCertNotificationTask(DevicePolicyManagerService.this, monitoringCertNotificationTask).execute(Integer.valueOf(userId));
                }
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    DevicePolicyManagerService.this.disableSecurityLoggingIfNotCompliant();
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    DevicePolicyManagerService.this.disableSecurityLoggingIfNotCompliant();
                    DevicePolicyManagerService.this.removeUserData(userHandle);
                    return;
                }
                if ("android.intent.action.USER_STARTED".equals(action)) {
                    synchronized (DevicePolicyManagerService.this) {
                        DevicePolicyManagerService.this.mUserData.remove(userHandle);
                        DevicePolicyManagerService.this.sendAdminEnabledBroadcastLocked(userHandle);
                    }
                    DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                    return;
                }
                if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                    return;
                }
                if ("android.intent.action.PACKAGE_CHANGED".equals(action) || ("android.intent.action.PACKAGE_ADDED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false))) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                    return;
                }
                if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
                } else {
                    if (!"android.intent.action.MANAGED_PROFILE_ADDED".equals(action)) {
                        return;
                    }
                    DevicePolicyManagerService.this.clearWipeProfileNotification();
                }
            }
        };
        this.mInjector = injector;
        this.mContext = (Context) Preconditions.checkNotNull(injector.mContext);
        this.mHandler = new Handler((Looper) Preconditions.checkNotNull(injector.getMyLooper()));
        this.mOwners = (Owners) Preconditions.checkNotNull(injector.newOwners());
        this.mUserManager = (UserManager) Preconditions.checkNotNull(injector.getUserManager());
        this.mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull(injector.getUserManagerInternal());
        this.mIPackageManager = (IPackageManager) Preconditions.checkNotNull(injector.getIPackageManager());
        this.mTelephonyManager = (TelephonyManager) Preconditions.checkNotNull(injector.getTelephonyManager());
        this.mLocalService = new LocalService();
        this.mLockPatternUtils = injector.newLockPatternUtils();
        this.mSecurityLogMonitor = new SecurityLogMonitor(this);
        this.mHasFeature = this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin");
        if (!this.mHasFeature) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        filter.addAction(ACTION_EXPIRED_PASSWORD_NOTIFICATION);
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_STARTED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.security.STORAGE_CHANGED");
        filter.setPriority(1000);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mHandler);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.PACKAGE_CHANGED");
        filter2.addAction("android.intent.action.PACKAGE_REMOVED");
        filter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        filter2.addAction("android.intent.action.PACKAGE_ADDED");
        filter2.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter2, null, this.mHandler);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter3, null, this.mHandler);
        LocalServices.addService(DevicePolicyManagerInternal.class, this.mLocalService);
    }

    DevicePolicyData getUserData(int userHandle) {
        DevicePolicyData policy;
        synchronized (this) {
            policy = this.mUserData.get(userHandle);
            if (policy == null) {
                policy = new DevicePolicyData(userHandle);
                this.mUserData.append(userHandle, policy);
                loadSettingsLocked(policy, userHandle);
            }
        }
        return policy;
    }

    DevicePolicyData getUserDataUnchecked(int userHandle) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            return getUserData(userHandle);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    void removeUserData(int userHandle) {
        synchronized (this) {
            if (userHandle == 0) {
                Slog.w(LOG_TAG, "Tried to remove device policy file for user 0! Ignoring.");
                return;
            }
            this.mOwners.removeProfileOwner(userHandle);
            this.mOwners.writeProfileOwner(userHandle);
            DevicePolicyData policy = this.mUserData.get(userHandle);
            if (policy != null) {
                this.mUserData.remove(userHandle);
            }
            File policyFile = new File(this.mInjector.environmentGetUserSystemDirectory(userHandle), DEVICE_POLICIES_XML);
            policyFile.delete();
            Slog.i(LOG_TAG, "Removed device policy file " + policyFile.getAbsolutePath());
            updateScreenCaptureDisabledInWindowManager(userHandle, false);
        }
    }

    void loadOwners() {
        synchronized (this) {
            this.mOwners.load();
            setDeviceOwnerSystemPropertyLocked();
            findOwnerComponentIfNecessaryLocked();
            migrateUserRestrictionsIfNecessaryLocked();
            updateDeviceOwnerLocked();
        }
    }

    private void setDeviceOwnerSystemPropertyLocked() {
        if (this.mInjector.settingsGlobalGetInt("device_provisioned", 0) == 0 || StorageManager.inCryptKeeperBounce()) {
            return;
        }
        if (!TextUtils.isEmpty(this.mInjector.systemPropertiesGet(PROPERTY_DEVICE_OWNER_PRESENT))) {
            Slog.w(LOG_TAG, "Trying to set ro.device_owner, but it has already been set?");
            return;
        }
        if (this.mOwners.hasDeviceOwner()) {
            this.mInjector.systemPropertiesSet(PROPERTY_DEVICE_OWNER_PRESENT, "true");
            Slog.i(LOG_TAG, "Set ro.device_owner property to true");
            disableSecurityLoggingIfNotCompliant();
            if (!this.mInjector.securityLogGetLoggingEnabledProperty()) {
                return;
            }
            this.mSecurityLogMonitor.start();
            return;
        }
        this.mInjector.systemPropertiesSet(PROPERTY_DEVICE_OWNER_PRESENT, "false");
        Slog.i(LOG_TAG, "Set ro.device_owner property to false");
    }

    private void findOwnerComponentIfNecessaryLocked() {
        if (!this.mOwners.hasDeviceOwner()) {
            return;
        }
        ComponentName doComponentName = this.mOwners.getDeviceOwnerComponent();
        if (!TextUtils.isEmpty(doComponentName.getClassName())) {
            return;
        }
        ComponentName doComponent = findAdminComponentWithPackageLocked(doComponentName.getPackageName(), this.mOwners.getDeviceOwnerUserId());
        if (doComponent == null) {
            Slog.e(LOG_TAG, "Device-owner isn't registered as device-admin");
        } else {
            this.mOwners.setDeviceOwnerWithRestrictionsMigrated(doComponent, this.mOwners.getDeviceOwnerName(), this.mOwners.getDeviceOwnerUserId(), !this.mOwners.getDeviceOwnerUserRestrictionsNeedsMigration());
            this.mOwners.writeDeviceOwner();
        }
    }

    private void migrateUserRestrictionsIfNecessaryLocked() {
        if (this.mOwners.getDeviceOwnerUserRestrictionsNeedsMigration()) {
            ActiveAdmin deviceOwnerAdmin = getDeviceOwnerAdminLocked();
            migrateUserRestrictionsForUser(UserHandle.SYSTEM, deviceOwnerAdmin, null, true);
            pushUserRestrictions(0);
            this.mOwners.setDeviceOwnerUserRestrictionsMigrated();
        }
        Set<String> secondaryUserExceptionList = Sets.newArraySet(new String[]{"no_outgoing_calls", "no_sms"});
        for (UserInfo ui : this.mUserManager.getUsers()) {
            int userId = ui.id;
            if (this.mOwners.getProfileOwnerUserRestrictionsNeedsMigration(userId)) {
                ActiveAdmin profileOwnerAdmin = getProfileOwnerAdminLocked(userId);
                migrateUserRestrictionsForUser(ui.getUserHandle(), profileOwnerAdmin, userId == 0 ? null : secondaryUserExceptionList, false);
                pushUserRestrictions(userId);
                this.mOwners.setProfileOwnerUserRestrictionsMigrated(userId);
            }
        }
    }

    private void migrateUserRestrictionsForUser(UserHandle user, ActiveAdmin admin, Set<String> exceptionList, boolean isDeviceOwner) {
        boolean canOwnerChange;
        Bundle origRestrictions = this.mUserManagerInternal.getBaseUserRestrictions(user.getIdentifier());
        Bundle newBaseRestrictions = new Bundle();
        Bundle newOwnerRestrictions = new Bundle();
        for (String key : origRestrictions.keySet()) {
            if (origRestrictions.getBoolean(key)) {
                if (isDeviceOwner) {
                    canOwnerChange = UserRestrictionsUtils.canDeviceOwnerChange(key);
                } else {
                    canOwnerChange = UserRestrictionsUtils.canProfileOwnerChange(key, user.getIdentifier());
                }
                if (!canOwnerChange || (exceptionList != null && exceptionList.contains(key))) {
                    newBaseRestrictions.putBoolean(key, true);
                } else {
                    newOwnerRestrictions.putBoolean(key, true);
                }
            }
        }
        this.mUserManagerInternal.setBaseUserRestrictionsByDpmsForMigration(user.getIdentifier(), newBaseRestrictions);
        if (admin != null) {
            admin.ensureUserRestrictions().clear();
            admin.ensureUserRestrictions().putAll(newOwnerRestrictions);
        } else {
            Slog.w(LOG_TAG, "ActiveAdmin for DO/PO not found. user=" + user.getIdentifier());
        }
        saveSettingsLocked(user.getIdentifier());
    }

    private ComponentName findAdminComponentWithPackageLocked(String packageName, int userId) {
        DevicePolicyData policy = getUserData(userId);
        int n = policy.mAdminList.size();
        ComponentName found = null;
        int nFound = 0;
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (packageName.equals(admin.info.getPackageName())) {
                if (nFound == 0) {
                    found = admin.info.getComponent();
                }
                nFound++;
            }
        }
        if (nFound > 1) {
            Slog.w(LOG_TAG, "Multiple DA found; assume the first one is DO.");
        }
        return found;
    }

    private void setExpirationAlarmCheckLocked(Context context, int userHandle, boolean parent) {
        long alarmTime;
        int affectedUserHandle;
        long expiration = getPasswordExpirationLocked(null, userHandle, parent);
        long now = System.currentTimeMillis();
        long timeToExpire = expiration - now;
        if (expiration == 0) {
            alarmTime = 0;
        } else if (timeToExpire <= 0) {
            alarmTime = now + 86400000;
        } else {
            long alarmInterval = timeToExpire % 86400000;
            if (alarmInterval == 0) {
                alarmInterval = 86400000;
            }
            alarmTime = now + alarmInterval;
        }
        long token = this.mInjector.binderClearCallingIdentity();
        if (!parent) {
            affectedUserHandle = userHandle;
        } else {
            try {
                affectedUserHandle = getProfileParentId(userHandle);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(token);
            }
        }
        AlarmManager am = (AlarmManager) context.getSystemService("alarm");
        PendingIntent pi = PendingIntent.getBroadcastAsUser(context, REQUEST_EXPIRE_PASSWORD, new Intent(ACTION_EXPIRED_PASSWORD_NOTIFICATION), 1207959552, UserHandle.of(affectedUserHandle));
        am.cancel(pi);
        if (alarmTime != 0) {
            am.set(1, alarmTime, pi);
        }
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle) {
        ActiveAdmin admin = getUserData(userHandle).mAdminMap.get(who);
        if (admin != null && who.getPackageName().equals(admin.info.getActivityInfo().packageName) && who.getClassName().equals(admin.info.getActivityInfo().name)) {
            return admin;
        }
        return null;
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle, boolean parent) {
        if (parent) {
            enforceManagedProfile(userHandle, "call APIs on the parent profile");
        }
        ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
        if (admin != null && parent) {
            return admin.getParentActiveAdmin();
        }
        return admin;
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy) throws SecurityException {
        int callingUid = this.mInjector.binderGetCallingUid();
        ActiveAdmin result = getActiveAdminWithPolicyForUidLocked(who, reqPolicy, callingUid);
        if (result != null) {
            return result;
        }
        if (who != null) {
            int userId = UserHandle.getUserId(callingUid);
            DevicePolicyData policy = getUserData(userId);
            ActiveAdmin admin = policy.mAdminMap.get(who);
            if (reqPolicy == -2) {
                throw new SecurityException("Admin " + admin.info.getComponent() + " does not own the device");
            }
            if (reqPolicy == -1) {
                throw new SecurityException("Admin " + admin.info.getComponent() + " does not own the profile");
            }
            throw new SecurityException("Admin " + admin.info.getComponent() + " did not specify uses-policy for: " + admin.info.getTagForPolicy(reqPolicy));
        }
        throw new SecurityException("No active admin owned by uid " + this.mInjector.binderGetCallingUid() + " for policy #" + reqPolicy);
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy, boolean parent) throws SecurityException {
        if (parent) {
            enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "call APIs on the parent profile");
        }
        ActiveAdmin admin = getActiveAdminForCallerLocked(who, reqPolicy);
        return parent ? admin.getParentActiveAdmin() : admin;
    }

    private ActiveAdmin getActiveAdminForUidLocked(ComponentName who, int uid) {
        int userId = UserHandle.getUserId(uid);
        DevicePolicyData policy = getUserData(userId);
        ActiveAdmin admin = policy.mAdminMap.get(who);
        if (admin == null) {
            throw new SecurityException("No active admin " + who);
        }
        if (admin.getUid() != uid) {
            throw new SecurityException("Admin " + who + " is not owned by uid " + uid);
        }
        return admin;
    }

    private ActiveAdmin getActiveAdminWithPolicyForUidLocked(ComponentName who, int reqPolicy, int uid) {
        int userId = UserHandle.getUserId(uid);
        DevicePolicyData policy = getUserData(userId);
        if (who != null) {
            ActiveAdmin admin = policy.mAdminMap.get(who);
            if (admin == null) {
                throw new SecurityException("No active admin " + who);
            }
            if (admin.getUid() != uid) {
                throw new SecurityException("Admin " + who + " is not owned by uid " + uid);
            }
            if (isActiveAdminWithPolicyForUserLocked(admin, reqPolicy, userId)) {
                return admin;
            }
        } else {
            for (ActiveAdmin admin2 : policy.mAdminList) {
                if (admin2.getUid() == uid && isActiveAdminWithPolicyForUserLocked(admin2, reqPolicy, userId)) {
                    return admin2;
                }
            }
        }
        return null;
    }

    boolean isActiveAdminWithPolicyForUserLocked(ActiveAdmin admin, int reqPolicy, int userId) {
        boolean ownsDevice = isDeviceOwner(admin.info.getComponent(), userId);
        boolean ownsProfile = isProfileOwner(admin.info.getComponent(), userId);
        if (reqPolicy == -2) {
            return ownsDevice;
        }
        if (reqPolicy == -1) {
            if (ownsDevice) {
                return true;
            }
            return ownsProfile;
        }
        return admin.info.usesPolicy(reqPolicy);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action) {
        sendAdminCommandLocked(admin, action, (BroadcastReceiver) null);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action, BroadcastReceiver result) {
        sendAdminCommandLocked(admin, action, null, result);
    }

    void sendAdminCommandLocked(ActiveAdmin admin, String action, Bundle adminExtras, BroadcastReceiver result) {
        Intent intent = new Intent(action);
        intent.setComponent(admin.info.getComponent());
        if (action.equals("android.app.action.ACTION_PASSWORD_EXPIRING")) {
            intent.putExtra("expiration", admin.passwordExpirationDate);
        }
        if (adminExtras != null) {
            intent.putExtras(adminExtras);
        }
        if (result != null) {
            this.mContext.sendOrderedBroadcastAsUser(intent, admin.getUserHandle(), null, result, this.mHandler, -1, null, null);
        } else {
            this.mContext.sendBroadcastAsUser(intent, admin.getUserHandle());
        }
    }

    void sendAdminCommandLocked(String action, int reqPolicy, int userHandle) {
        DevicePolicyData policy = getUserData(userHandle);
        int count = policy.mAdminList.size();
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (admin.info.usesPolicy(reqPolicy)) {
                sendAdminCommandLocked(admin, action);
            }
        }
    }

    private void sendAdminCommandToSelfAndProfilesLocked(String action, int reqPolicy, int userHandle) {
        int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userHandle);
        for (int profileId : profileIds) {
            sendAdminCommandLocked(action, reqPolicy, profileId);
        }
    }

    private void sendAdminCommandForLockscreenPoliciesLocked(String action, int reqPolicy, int userHandle) {
        if (isSeparateProfileChallengeEnabled(userHandle)) {
            sendAdminCommandLocked(action, reqPolicy, userHandle);
        } else {
            sendAdminCommandToSelfAndProfilesLocked(action, reqPolicy, userHandle);
        }
    }

    void removeActiveAdminLocked(final ComponentName adminReceiver, final int userHandle) {
        ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
        if (admin == null) {
            return;
        }
        getUserData(userHandle).mRemovingAdmins.add(adminReceiver);
        sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_DISABLED", new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DevicePolicyManagerService.this.removeAdminArtifacts(adminReceiver, userHandle);
                DevicePolicyManagerService.this.removePackageIfRequired(adminReceiver.getPackageName(), userHandle);
            }
        });
    }

    public DeviceAdminInfo findAdmin(ComponentName adminName, int userHandle, boolean throwForMissiongPermission) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(userHandle);
        ActivityInfo ai = null;
        try {
            ai = this.mIPackageManager.getReceiverInfo(adminName, 819328, userHandle);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            throw new IllegalArgumentException("Unknown admin: " + adminName);
        }
        if (!"android.permission.BIND_DEVICE_ADMIN".equals(ai.permission)) {
            String message = "DeviceAdminReceiver " + adminName + " must be protected with android.permission.BIND_DEVICE_ADMIN";
            Slog.w(LOG_TAG, message);
            if (throwForMissiongPermission && ai.applicationInfo.targetSdkVersion > 23) {
                throw new IllegalArgumentException(message);
            }
        }
        try {
            return new DeviceAdminInfo(this.mContext, ai);
        } catch (IOException | XmlPullParserException e2) {
            Slog.w(LOG_TAG, "Bad device admin requested for user=" + userHandle + ": " + adminName, e2);
            return null;
        }
    }

    private JournaledFile makeJournaledFile(int userHandle) {
        String base;
        if (userHandle == 0) {
            base = this.mInjector.getDevicePolicyFilePathForSystemUser() + DEVICE_POLICIES_XML;
        } else {
            base = new File(this.mInjector.environmentGetUserSystemDirectory(userHandle), DEVICE_POLICIES_XML).getAbsolutePath();
        }
        return new JournaledFile(new File(base), new File(base + ".tmp"));
    }

    private void saveSettingsLocked(int userHandle) {
        FileOutputStream stream;
        DevicePolicyData policy = getUserData(userHandle);
        JournaledFile journal = makeJournaledFile(userHandle);
        FileOutputStream fileOutputStream = null;
        try {
            stream = new FileOutputStream(journal.chooseForWrite(), false);
        } catch (IOException | XmlPullParserException e) {
            e = e;
        }
        try {
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, "policies");
            if (policy.mRestrictionsProvider != null) {
                out.attribute(null, ATTR_PERMISSION_PROVIDER, policy.mRestrictionsProvider.flattenToString());
            }
            if (policy.mUserSetupComplete) {
                out.attribute(null, ATTR_SETUP_COMPLETE, Boolean.toString(true));
            }
            if (policy.mUserProvisioningState != 0) {
                out.attribute(null, ATTR_PROVISIONING_STATE, Integer.toString(policy.mUserProvisioningState));
            }
            if (policy.mPermissionPolicy != 0) {
                out.attribute(null, ATTR_PERMISSION_POLICY, Integer.toString(policy.mPermissionPolicy));
            }
            if (policy.mDelegatedCertInstallerPackage != null) {
                out.attribute(null, ATTR_DELEGATED_CERT_INSTALLER, policy.mDelegatedCertInstallerPackage);
            }
            if (policy.mApplicationRestrictionsManagingPackage != null) {
                out.attribute(null, ATTR_APPLICATION_RESTRICTIONS_MANAGER, policy.mApplicationRestrictionsManagingPackage);
            }
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin ap = policy.mAdminList.get(i);
                if (ap != null) {
                    out.startTag(null, "admin");
                    out.attribute(null, ATTR_NAME, ap.info.getComponent().flattenToString());
                    ap.writeToXml(out);
                    out.endTag(null, "admin");
                }
            }
            if (policy.mPasswordOwner >= 0) {
                out.startTag(null, "password-owner");
                out.attribute(null, ATTR_VALUE, Integer.toString(policy.mPasswordOwner));
                out.endTag(null, "password-owner");
            }
            if (policy.mFailedPasswordAttempts != 0) {
                out.startTag(null, "failed-password-attempts");
                out.attribute(null, ATTR_VALUE, Integer.toString(policy.mFailedPasswordAttempts));
                out.endTag(null, "failed-password-attempts");
            }
            if (!this.mInjector.storageManagerIsFileBasedEncryptionEnabled() && (policy.mActivePasswordQuality != 0 || policy.mActivePasswordLength != 0 || policy.mActivePasswordUpperCase != 0 || policy.mActivePasswordLowerCase != 0 || policy.mActivePasswordLetters != 0 || policy.mActivePasswordNumeric != 0 || policy.mActivePasswordSymbols != 0 || policy.mActivePasswordNonLetter != 0)) {
                out.startTag(null, "active-password");
                out.attribute(null, "quality", Integer.toString(policy.mActivePasswordQuality));
                out.attribute(null, "length", Integer.toString(policy.mActivePasswordLength));
                out.attribute(null, "uppercase", Integer.toString(policy.mActivePasswordUpperCase));
                out.attribute(null, "lowercase", Integer.toString(policy.mActivePasswordLowerCase));
                out.attribute(null, "letters", Integer.toString(policy.mActivePasswordLetters));
                out.attribute(null, "numeric", Integer.toString(policy.mActivePasswordNumeric));
                out.attribute(null, "symbols", Integer.toString(policy.mActivePasswordSymbols));
                out.attribute(null, "nonletter", Integer.toString(policy.mActivePasswordNonLetter));
                out.endTag(null, "active-password");
            }
            for (int i2 = 0; i2 < policy.mAcceptedCaCertificates.size(); i2++) {
                out.startTag(null, TAG_ACCEPTED_CA_CERTIFICATES);
                out.attribute(null, ATTR_NAME, policy.mAcceptedCaCertificates.valueAt(i2));
                out.endTag(null, TAG_ACCEPTED_CA_CERTIFICATES);
            }
            for (int i3 = 0; i3 < policy.mLockTaskPackages.size(); i3++) {
                String component = policy.mLockTaskPackages.get(i3);
                out.startTag(null, TAG_LOCK_TASK_COMPONENTS);
                out.attribute(null, ATTR_NAME, component);
                out.endTag(null, TAG_LOCK_TASK_COMPONENTS);
            }
            if (policy.mStatusBarDisabled) {
                out.startTag(null, TAG_STATUS_BAR);
                out.attribute(null, ATTR_DISABLED, Boolean.toString(policy.mStatusBarDisabled));
                out.endTag(null, TAG_STATUS_BAR);
            }
            if (policy.doNotAskCredentialsOnBoot) {
                out.startTag(null, DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML);
                out.endTag(null, DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML);
            }
            for (String id : policy.mAffiliationIds) {
                out.startTag(null, TAG_AFFILIATION_ID);
                out.attribute(null, "id", id);
                out.endTag(null, TAG_AFFILIATION_ID);
            }
            if (policy.mAdminBroadcastPending) {
                out.startTag(null, TAG_ADMIN_BROADCAST_PENDING);
                out.attribute(null, ATTR_VALUE, Boolean.toString(policy.mAdminBroadcastPending));
                out.endTag(null, TAG_ADMIN_BROADCAST_PENDING);
            }
            if (policy.mInitBundle != null) {
                out.startTag(null, TAG_INITIALIZATION_BUNDLE);
                policy.mInitBundle.saveToXml(out);
                out.endTag(null, TAG_INITIALIZATION_BUNDLE);
            }
            out.endTag(null, "policies");
            out.endDocument();
            stream.flush();
            FileUtils.sync(stream);
            stream.close();
            journal.commit();
            sendChangedNotification(userHandle);
        } catch (IOException | XmlPullParserException e2) {
            e = e2;
            fileOutputStream = stream;
            Slog.w(LOG_TAG, "failed writing file", e);
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e3) {
                }
            }
            journal.rollback();
        }
    }

    private void sendChangedNotification(int userHandle) {
        Intent intent = new Intent("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intent.setFlags(1073741824);
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void loadSettingsLocked(DevicePolicyData policy, int userHandle) {
        FileInputStream stream;
        XmlPullParser parser;
        int type;
        String tag;
        JournaledFile journal = makeJournaledFile(userHandle);
        FileInputStream fileInputStream = null;
        File file = journal.chooseForRead();
        boolean needsRewrite = false;
        try {
            stream = new FileInputStream(file);
            try {
                try {
                    parser = Xml.newPullParser();
                    parser.setInput(stream, StandardCharsets.UTF_8.name());
                    do {
                        type = parser.next();
                        if (type == 1) {
                            break;
                        }
                    } while (type != 2);
                    tag = parser.getName();
                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
                    e = e;
                    fileInputStream = stream;
                    Slog.w(LOG_TAG, "failed parsing " + file, e);
                }
            } catch (FileNotFoundException e2) {
                fileInputStream = stream;
            }
        } catch (FileNotFoundException e3) {
        } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e4) {
            e = e4;
        }
        if (!"policies".equals(tag)) {
            throw new XmlPullParserException("Settings do not start with policies tag: found " + tag);
        }
        String permissionProvider = parser.getAttributeValue(null, ATTR_PERMISSION_PROVIDER);
        if (permissionProvider != null) {
            policy.mRestrictionsProvider = ComponentName.unflattenFromString(permissionProvider);
        }
        String userSetupComplete = parser.getAttributeValue(null, ATTR_SETUP_COMPLETE);
        if (userSetupComplete != null && Boolean.toString(true).equals(userSetupComplete)) {
            policy.mUserSetupComplete = true;
        }
        String provisioningState = parser.getAttributeValue(null, ATTR_PROVISIONING_STATE);
        if (!TextUtils.isEmpty(provisioningState)) {
            policy.mUserProvisioningState = Integer.parseInt(provisioningState);
        }
        String permissionPolicy = parser.getAttributeValue(null, ATTR_PERMISSION_POLICY);
        if (!TextUtils.isEmpty(permissionPolicy)) {
            policy.mPermissionPolicy = Integer.parseInt(permissionPolicy);
        }
        policy.mDelegatedCertInstallerPackage = parser.getAttributeValue(null, ATTR_DELEGATED_CERT_INSTALLER);
        policy.mApplicationRestrictionsManagingPackage = parser.getAttributeValue(null, ATTR_APPLICATION_RESTRICTIONS_MANAGER);
        parser.next();
        int outerDepth = parser.getDepth();
        policy.mLockTaskPackages.clear();
        policy.mAdminList.clear();
        policy.mAdminMap.clear();
        policy.mAffiliationIds.clear();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type2 != 3 && type2 != 4) {
                String tag2 = parser.getName();
                if ("admin".equals(tag2)) {
                    String name = parser.getAttributeValue(null, ATTR_NAME);
                    try {
                        DeviceAdminInfo dai = findAdmin(ComponentName.unflattenFromString(name), userHandle, false);
                        if (dai != null) {
                            ActiveAdmin ap = new ActiveAdmin(dai, false);
                            ap.readFromXml(parser);
                            if (policy.mAdminMap.get(ap.info.getComponent()) != null) {
                                Slog.e(LOG_TAG, "This ActiveAdmin:" + ap.info.getComponent() + " already exist on user " + userHandle + ", ignore it. callstack: " + Log.getStackTraceString(new Throwable()));
                            } else {
                                policy.mAdminMap.put(ap.info.getComponent(), ap);
                            }
                        }
                    } catch (RuntimeException e5) {
                        Slog.w(LOG_TAG, "Failed loading admin " + name, e5);
                    }
                } else if ("failed-password-attempts".equals(tag2)) {
                    policy.mFailedPasswordAttempts = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("password-owner".equals(tag2)) {
                    policy.mPasswordOwner = Integer.parseInt(parser.getAttributeValue(null, ATTR_VALUE));
                } else if (TAG_ACCEPTED_CA_CERTIFICATES.equals(tag2)) {
                    policy.mAcceptedCaCertificates.add(parser.getAttributeValue(null, ATTR_NAME));
                } else if (TAG_LOCK_TASK_COMPONENTS.equals(tag2)) {
                    policy.mLockTaskPackages.add(parser.getAttributeValue(null, ATTR_NAME));
                } else if (TAG_STATUS_BAR.equals(tag2)) {
                    policy.mStatusBarDisabled = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_DISABLED));
                } else if (DO_NOT_ASK_CREDENTIALS_ON_BOOT_XML.equals(tag2)) {
                    policy.doNotAskCredentialsOnBoot = true;
                } else if (TAG_AFFILIATION_ID.equals(tag2)) {
                    policy.mAffiliationIds.add(parser.getAttributeValue(null, "id"));
                } else if (TAG_ADMIN_BROADCAST_PENDING.equals(tag2)) {
                    String pending = parser.getAttributeValue(null, ATTR_VALUE);
                    policy.mAdminBroadcastPending = Boolean.toString(true).equals(pending);
                } else if (TAG_INITIALIZATION_BUNDLE.equals(tag2)) {
                    policy.mInitBundle = PersistableBundle.restoreFromXml(parser);
                } else if (!"active-password".equals(tag2)) {
                    Slog.w(LOG_TAG, "Unknown tag: " + tag2);
                    XmlUtils.skipCurrentTag(parser);
                } else if (this.mInjector.storageManagerIsFileBasedEncryptionEnabled()) {
                    needsRewrite = true;
                } else {
                    policy.mActivePasswordQuality = Integer.parseInt(parser.getAttributeValue(null, "quality"));
                    policy.mActivePasswordLength = Integer.parseInt(parser.getAttributeValue(null, "length"));
                    policy.mActivePasswordUpperCase = Integer.parseInt(parser.getAttributeValue(null, "uppercase"));
                    policy.mActivePasswordLowerCase = Integer.parseInt(parser.getAttributeValue(null, "lowercase"));
                    policy.mActivePasswordLetters = Integer.parseInt(parser.getAttributeValue(null, "letters"));
                    policy.mActivePasswordNumeric = Integer.parseInt(parser.getAttributeValue(null, "numeric"));
                    policy.mActivePasswordSymbols = Integer.parseInt(parser.getAttributeValue(null, "symbols"));
                    policy.mActivePasswordNonLetter = Integer.parseInt(parser.getAttributeValue(null, "nonletter"));
                }
            }
        }
        fileInputStream = stream;
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e6) {
            }
        }
        policy.mAdminList.addAll(policy.mAdminMap.values());
        if (needsRewrite) {
            saveSettingsLocked(userHandle);
        }
        validatePasswordOwnerLocked(policy);
        updateMaximumTimeToLockLocked(userHandle);
        updateLockTaskPackagesLocked(policy.mLockTaskPackages, userHandle);
        if (policy.mStatusBarDisabled) {
            setStatusBarDisabledInternal(policy.mStatusBarDisabled, userHandle);
        }
    }

    private void updateLockTaskPackagesLocked(List<String> packages, int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.getIActivityManager().updateLockTaskPackages(userId, (String[]) packages.toArray(new String[packages.size()]));
        } catch (RemoteException e) {
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void updateDeviceOwnerLocked() {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
            if (deviceOwnerComponent != null) {
                this.mInjector.getIActivityManager().updateDeviceOwner(deviceOwnerComponent.getPackageName());
            }
        } catch (RemoteException e) {
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    static void validateQualityConstant(int quality) {
        switch (quality) {
            case 0:
            case PackageManagerService.DumpState.DUMP_KEYSETS:
            case PackageManagerService.DumpState.DUMP_VERSION:
            case PackageManagerService.DumpState.DUMP_INSTALLS:
            case PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS:
            case 196608:
            case PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED:
            case 327680:
            case 393216:
            case PackageManagerService.DumpState.DUMP_FROZEN:
                return;
            default:
                throw new IllegalArgumentException("Invalid quality constant: 0x" + Integer.toHexString(quality));
        }
    }

    void validatePasswordOwnerLocked(DevicePolicyData policy) {
        if (policy.mPasswordOwner < 0) {
            return;
        }
        boolean haveOwner = false;
        int i = policy.mAdminList.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            if (policy.mAdminList.get(i).getUid() != policy.mPasswordOwner) {
                i--;
            } else {
                haveOwner = true;
                break;
            }
        }
        if (haveOwner) {
            return;
        }
        Slog.w(LOG_TAG, "Previous password owner " + policy.mPasswordOwner + " no longer active; disabling");
        policy.mPasswordOwner = -1;
    }

    void systemReady(int phase) {
        if (!this.mHasFeature) {
        }
        switch (phase) {
            case SystemService.PHASE_LOCK_SETTINGS_READY:
                onLockSettingsReady();
                break;
            case 1000:
                ensureDeviceOwnerUserStarted();
                break;
        }
    }

    private void onLockSettingsReady() {
        List<String> packageList;
        getUserData(0);
        loadOwners();
        cleanUpOldUsers();
        onStartUser(0);
        new SetupContentObserver(this.mHandler).register();
        updateUserSetupComplete();
        synchronized (this) {
            packageList = getKeepUninstalledPackagesLocked();
        }
        if (packageList != null) {
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(packageList);
        }
        synchronized (this) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            if (deviceOwner != null) {
                this.mUserManagerInternal.setForceEphemeralUsers(deviceOwner.forceEphemeralUsers);
            }
        }
    }

    private void ensureDeviceOwnerUserStarted() {
        synchronized (this) {
            if (!this.mOwners.hasDeviceOwner()) {
                return;
            }
            int userId = this.mOwners.getDeviceOwnerUserId();
            if (userId == 0) {
                return;
            }
            try {
                this.mInjector.getIActivityManager().startUserInBackground(userId);
            } catch (RemoteException e) {
                Slog.w(LOG_TAG, "Exception starting user", e);
            }
        }
    }

    private void onStartUser(int userId) {
        updateScreenCaptureDisabledInWindowManager(userId, getScreenCaptureDisabled(null, userId));
        pushUserRestrictions(userId);
    }

    private void cleanUpOldUsers() {
        Collection<? extends Integer> usersWithProfileOwners;
        ArraySet arraySet;
        synchronized (this) {
            usersWithProfileOwners = this.mOwners.getProfileOwnerKeys();
            arraySet = new ArraySet();
            for (int i = 0; i < this.mUserData.size(); i++) {
                arraySet.add(Integer.valueOf(this.mUserData.keyAt(i)));
            }
        }
        List<UserInfo> allUsers = this.mUserManager.getUsers();
        Set<Integer> deletedUsers = new ArraySet<>();
        deletedUsers.addAll(usersWithProfileOwners);
        deletedUsers.addAll(arraySet);
        for (UserInfo userInfo : allUsers) {
            deletedUsers.remove(Integer.valueOf(userInfo.id));
        }
        for (Integer userId : deletedUsers) {
            removeUserData(userId.intValue());
        }
    }

    private void handlePasswordExpirationNotification(int userHandle) {
        synchronized (this) {
            long now = System.currentTimeMillis();
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, false);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin = admins.get(i);
                if (admin.info.usesPolicy(6) && admin.passwordExpirationTimeout > 0 && now >= admin.passwordExpirationDate - EXPIRATION_GRACE_PERIOD_MS && admin.passwordExpirationDate > 0) {
                    sendAdminCommandLocked(admin, "android.app.action.ACTION_PASSWORD_EXPIRING");
                }
            }
            setExpirationAlarmCheckLocked(this.mContext, userHandle, false);
        }
    }

    private class MonitoringCertNotificationTask extends AsyncTask<Integer, Void, Void> {
        MonitoringCertNotificationTask(DevicePolicyManagerService this$0, MonitoringCertNotificationTask monitoringCertNotificationTask) {
            this();
        }

        private MonitoringCertNotificationTask() {
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int userHandle = params[0].intValue();
            if (userHandle == -1) {
                for (UserInfo userInfo : DevicePolicyManagerService.this.mUserManager.getUsers(true)) {
                    manageNotification(userInfo.getUserHandle());
                }
                return null;
            }
            manageNotification(UserHandle.of(userHandle));
            return null;
        }

        private void manageNotification(UserHandle userHandle) {
            String contentText;
            int smallIconId;
            if (DevicePolicyManagerService.this.mUserManager.isUserUnlocked(userHandle)) {
                try {
                    List<String> pendingCertificates = getInstalledCaCertificates(userHandle);
                    synchronized (DevicePolicyManagerService.this) {
                        DevicePolicyData policy = DevicePolicyManagerService.this.getUserData(userHandle.getIdentifier());
                        if (policy.mAcceptedCaCertificates.retainAll(pendingCertificates)) {
                            DevicePolicyManagerService.this.saveSettingsLocked(userHandle.getIdentifier());
                        }
                        pendingCertificates.removeAll(policy.mAcceptedCaCertificates);
                    }
                    if (pendingCertificates.isEmpty()) {
                        DevicePolicyManagerService.this.mInjector.getNotificationManager().cancelAsUser(null, 18087937, userHandle);
                        return;
                    }
                    int parentUserId = userHandle.getIdentifier();
                    if (DevicePolicyManagerService.this.getProfileOwner(userHandle.getIdentifier()) != null) {
                        contentText = DevicePolicyManagerService.this.mContext.getString(R.string.accessibility_shortcut_multiple_service_warning_title, DevicePolicyManagerService.this.getProfileOwnerName(userHandle.getIdentifier()));
                        smallIconId = R.drawable.media_seamless_background;
                        parentUserId = DevicePolicyManagerService.this.getProfileParentId(userHandle.getIdentifier());
                    } else if (DevicePolicyManagerService.this.getDeviceOwnerUserId() == userHandle.getIdentifier()) {
                        contentText = DevicePolicyManagerService.this.mContext.getString(R.string.accessibility_shortcut_multiple_service_warning_title, DevicePolicyManagerService.this.getDeviceOwnerName());
                        smallIconId = R.drawable.media_seamless_background;
                    } else {
                        contentText = DevicePolicyManagerService.this.mContext.getString(R.string.accessibility_shortcut_multiple_service_list);
                        smallIconId = R.drawable.stat_sys_warning;
                    }
                    int numberOfCertificates = pendingCertificates.size();
                    Intent dialogIntent = new Intent("com.android.settings.MONITORING_CERT_INFO");
                    dialogIntent.setFlags(268468224);
                    dialogIntent.setPackage("com.android.settings");
                    dialogIntent.putExtra("android.settings.extra.number_of_certificates", numberOfCertificates);
                    dialogIntent.putExtra("android.intent.extra.USER_ID", userHandle.getIdentifier());
                    PendingIntent notifyIntent = PendingIntent.getActivityAsUser(DevicePolicyManagerService.this.mContext, 0, dialogIntent, 134217728, null, new UserHandle(parentUserId));
                    if (BenesseExtension.getDchaState() != 0) {
                        notifyIntent = null;
                    }
                    try {
                        String packageName = DevicePolicyManagerService.this.mContext.getPackageName();
                        Context userContext = DevicePolicyManagerService.this.mContext.createPackageContextAsUser(packageName, 0, userHandle);
                        Notification noti = new Notification.Builder(userContext).setSmallIcon(smallIconId).setContentTitle(DevicePolicyManagerService.this.mContext.getResources().getQuantityText(18087937, numberOfCertificates)).setContentText(contentText).setContentIntent(notifyIntent).setPriority(1).setShowWhen(false).setColor(DevicePolicyManagerService.this.mContext.getColor(R.color.system_accent3_600)).build();
                        DevicePolicyManagerService.this.mInjector.getNotificationManager().notifyAsUser(null, 18087937, noti, userHandle);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(DevicePolicyManagerService.LOG_TAG, "Create context as " + userHandle + " failed", e);
                    }
                } catch (RemoteException | RuntimeException e2) {
                    Log.e(DevicePolicyManagerService.LOG_TAG, "Could not retrieve certificates from KeyChain service", e2);
                }
            }
        }

        private List<String> getInstalledCaCertificates(UserHandle userHandle) throws RemoteException, RuntimeException {
            KeyChain.KeyChainConnection conn = null;
            try {
                try {
                    conn = KeyChain.bindAsUser(DevicePolicyManagerService.this.mContext, userHandle);
                    List<ParcelableString> aliases = conn.getService().getUserCaAliases().getList();
                    List<String> result = new ArrayList<>(aliases.size());
                    for (int i = 0; i < aliases.size(); i++) {
                        result.add(aliases.get(i).string);
                    }
                    if (conn != null) {
                        conn.close();
                    }
                    return result;
                } catch (AssertionError e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                    if (conn != null) {
                        conn.close();
                    }
                    return null;
                }
            } catch (Throwable th) {
                if (conn != null) {
                    conn.close();
                }
                throw th;
            }
        }
    }

    public void setActiveAdmin(ComponentName adminReceiver, boolean refreshing, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        setActiveAdmin(adminReceiver, refreshing, userHandle, null);
    }

    private void setActiveAdmin(ComponentName adminReceiver, boolean refreshing, int userHandle, Bundle onEnableData) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
        enforceFullCrossUsersPermission(userHandle);
        DevicePolicyData policy = getUserData(userHandle);
        DeviceAdminInfo info = findAdmin(adminReceiver, userHandle, true);
        if (info == null) {
            throw new IllegalArgumentException("Bad admin: " + adminReceiver);
        }
        if (!info.getActivityInfo().applicationInfo.isInternal()) {
            throw new IllegalArgumentException("Only apps in internal storage can be active admin: " + adminReceiver);
        }
        synchronized (this) {
            long ident = this.mInjector.binderClearCallingIdentity();
            if (!refreshing) {
                try {
                    if (getActiveAdminUncheckedLocked(adminReceiver, userHandle) != null) {
                        throw new IllegalArgumentException("Admin is already added");
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
            if (policy.mRemovingAdmins.contains(adminReceiver)) {
                throw new IllegalArgumentException("Trying to set an admin which is being removed");
            }
            ActiveAdmin newAdmin = new ActiveAdmin(info, false);
            policy.mAdminMap.put(adminReceiver, newAdmin);
            int replaceIndex = -1;
            int N = policy.mAdminList.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                ActiveAdmin oldAdmin = policy.mAdminList.get(i);
                if (!oldAdmin.info.getComponent().equals(adminReceiver)) {
                    i++;
                } else {
                    replaceIndex = i;
                    break;
                }
            }
            if (replaceIndex == -1) {
                policy.mAdminList.add(newAdmin);
                enableIfNecessary(info.getPackageName(), userHandle);
            } else {
                policy.mAdminList.set(replaceIndex, newAdmin);
            }
            saveSettingsLocked(userHandle);
            sendAdminCommandLocked(newAdmin, "android.app.action.DEVICE_ADMIN_ENABLED", onEnableData, null);
        }
    }

    public boolean isAdminActive(ComponentName adminReceiver, int userHandle) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            z = getActiveAdminUncheckedLocked(adminReceiver, userHandle) != null;
        }
        return z;
    }

    public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) {
        boolean zContains;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            DevicePolicyData policyData = getUserData(userHandle);
            zContains = policyData.mRemovingAdmins.contains(adminReceiver);
        }
        return zContains;
    }

    public boolean hasGrantedPolicy(ComponentName adminReceiver, int policyId, int userHandle) {
        boolean zUsesPolicy;
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            ActiveAdmin administrator = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (administrator == null) {
                throw new SecurityException("No active admin " + adminReceiver);
            }
            zUsesPolicy = administrator.info.usesPolicy(policyId);
        }
        return zUsesPolicy;
    }

    public List<ComponentName> getActiveAdmins(int userHandle) {
        if (!this.mHasFeature) {
            return Collections.EMPTY_LIST;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            if (N <= 0) {
                return null;
            }
            ArrayList<ComponentName> res = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                res.add(policy.mAdminList.get(i).info.getComponent());
            }
            return res;
        }
    }

    public boolean packageHasActiveAdmins(String packageName, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                if (policy.mAdminList.get(i).info.getPackageName().equals(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void forceRemoveActiveAdmin(ComponentName adminReceiver, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(adminReceiver, "ComponentName is null");
        enforceShell("forceRemoveActiveAdmin");
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            try {
                ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(adminReceiver.getPackageName(), 0, userHandle);
                if (ai == null) {
                    throw new IllegalStateException("Couldn't find package to remove admin " + adminReceiver.getPackageName() + " " + userHandle);
                }
                if ((ai.flags & 256) == 0) {
                    throw new SecurityException("Attempt to remove non-test admin " + adminReceiver + adminReceiver + " " + userHandle);
                }
                synchronized (this) {
                    if (isDeviceOwner(adminReceiver, userHandle)) {
                        clearDeviceOwnerLocked(getDeviceOwnerAdminLocked(), userHandle);
                    }
                    if (isProfileOwner(adminReceiver, userHandle)) {
                        ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle, false);
                        clearProfileOwnerLocked(admin, userHandle);
                    }
                }
                removeAdminArtifacts(adminReceiver, userHandle);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void enforceShell(String method) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || callingUid == 0) {
        } else {
            throw new SecurityException("Non-shell user attempted to call " + method);
        }
    }

    public void removeActiveAdmin(ComponentName adminReceiver, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(userHandle);
        enforceUserUnlocked(userHandle);
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (admin == null) {
                return;
            }
            if (isDeviceOwner(adminReceiver, userHandle) || isProfileOwner(adminReceiver, userHandle)) {
                Slog.e(LOG_TAG, "Device/profile owner cannot be removed: component=" + adminReceiver);
                return;
            }
            if (admin.getUid() != this.mInjector.binderGetCallingUid()) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                removeActiveAdminLocked(adminReceiver, userHandle);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public boolean isSeparateProfileChallengeAllowed(int userHandle) {
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Caller must be system");
        }
        ComponentName profileOwner = getProfileOwner(userHandle);
        return profileOwner != null && getTargetSdk(profileOwner.getPackageName(), userHandle) > 23;
    }

    public void setPasswordQuality(ComponentName who, int quality, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        validateQualityConstant(quality);
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.passwordQuality != quality) {
                ap.passwordQuality = quality;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int mode = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.passwordQuality : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (mode < admin2.passwordQuality) {
                    mode = admin2.passwordQuality;
                }
            }
            return mode;
        }
    }

    private List<ActiveAdmin> getActiveAdminsForLockscreenPoliciesLocked(int userHandle, boolean parent) {
        if (!parent && isSeparateProfileChallengeEnabled(userHandle)) {
            return getUserDataUnchecked(userHandle).mAdminList;
        }
        ArrayList<ActiveAdmin> admins = new ArrayList<>();
        for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
            DevicePolicyData policy = getUserData(userInfo.id);
            if (!userInfo.isManagedProfile()) {
                admins.addAll(policy.mAdminList);
            } else {
                boolean hasSeparateChallenge = isSeparateProfileChallengeEnabled(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin = policy.mAdminList.get(i);
                    if (admin.hasParentActiveAdmin()) {
                        admins.add(admin.getParentActiveAdmin());
                    }
                    if (!hasSeparateChallenge) {
                        admins.add(admin);
                    }
                }
            }
        }
        return admins;
    }

    private boolean isSeparateProfileChallengeEnabled(int userHandle) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userHandle);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordLength != length) {
                ap.minimumPasswordLength = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordLength : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (length < admin2.minimumPasswordLength) {
                    length = admin2.minimumPasswordLength;
                }
            }
            return length;
        }
    }

    public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.passwordHistoryLength != length) {
                ap.passwordHistoryLength = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.passwordHistoryLength : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (length < admin2.passwordHistoryLength) {
                    length = admin2.passwordHistoryLength;
                }
            }
            return length;
        }
    }

    public void setPasswordExpirationTimeout(ComponentName who, long timeout, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkArgumentNonnegative(timeout, "Timeout must be >= 0 ms");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 6, parent);
            long expiration = timeout > 0 ? timeout + System.currentTimeMillis() : 0L;
            ap.passwordExpirationDate = expiration;
            ap.passwordExpirationTimeout = timeout;
            if (timeout > 0) {
                Slog.w(LOG_TAG, "setPasswordExpiration(): password will expire on " + DateFormat.getDateTimeInstance(2, 2).format(new Date(expiration)));
            }
            saveSettingsLocked(userHandle);
            setExpirationAlarmCheckLocked(this.mContext, userHandle, parent);
        }
    }

    public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            long timeout = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.passwordExpirationTimeout : 0L;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (timeout == 0 || (admin2.passwordExpirationTimeout != 0 && timeout > admin2.passwordExpirationTimeout)) {
                    timeout = admin2.passwordExpirationTimeout;
                }
            }
            return timeout;
        }
    }

    public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) throws Throwable {
        int userId = UserHandle.getCallingUserId();
        List<String> list = null;
        synchronized (this) {
            try {
                ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
                if (activeAdmin.crossProfileWidgetProviders == null) {
                    activeAdmin.crossProfileWidgetProviders = new ArrayList();
                }
                List<String> providers = activeAdmin.crossProfileWidgetProviders;
                if (!providers.contains(packageName)) {
                    providers.add(packageName);
                    List<String> changedProviders = new ArrayList<>(providers);
                    try {
                        saveSettingsLocked(userId);
                        list = changedProviders;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (list != null) {
                    this.mLocalService.notifyCrossProfileProvidersChanged(userId, list);
                    return true;
                }
                return false;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) throws Throwable {
        int userId = UserHandle.getCallingUserId();
        List<String> list = null;
        synchronized (this) {
            try {
                ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
                if (activeAdmin.crossProfileWidgetProviders == null) {
                    return false;
                }
                List<String> providers = activeAdmin.crossProfileWidgetProviders;
                if (providers.remove(packageName)) {
                    List<String> changedProviders = new ArrayList<>(providers);
                    try {
                        saveSettingsLocked(userId);
                        list = changedProviders;
                    } catch (Throwable th) {
                        th = th;
                    }
                }
                if (list == null) {
                    return false;
                }
                this.mLocalService.notifyCrossProfileProvidersChanged(userId, list);
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
            throw th;
        }
    }

    public List<String> getCrossProfileWidgetProviders(ComponentName admin) {
        synchronized (this) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
            if (activeAdmin.crossProfileWidgetProviders == null || activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                return null;
            }
            if (this.mInjector.binderIsCallingUidMyUid()) {
                return new ArrayList(activeAdmin.crossProfileWidgetProviders);
            }
            return activeAdmin.crossProfileWidgetProviders;
        }
    }

    private long getPasswordExpirationLocked(ComponentName who, int userHandle, boolean parent) {
        long timeout = 0;
        if (who != null) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
            if (admin != null) {
                return admin.passwordExpirationDate;
            }
            return 0L;
        }
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin2 = admins.get(i);
            if (timeout == 0 || (admin2.passwordExpirationDate != 0 && timeout > admin2.passwordExpirationDate)) {
                timeout = admin2.passwordExpirationDate;
            }
        }
        return timeout;
    }

    public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) {
        long passwordExpirationLocked;
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            passwordExpirationLocked = getPasswordExpirationLocked(who, userHandle, parent);
        }
        return passwordExpirationLocked;
    }

    public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordUpperCase != length) {
                ap.minimumPasswordUpperCase = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordUpperCase : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (length < admin2.minimumPasswordUpperCase) {
                    length = admin2.minimumPasswordUpperCase;
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordLowerCase != length) {
                ap.minimumPasswordLowerCase = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordLowerCase : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (length < admin2.minimumPasswordLowerCase) {
                    length = admin2.minimumPasswordLowerCase;
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordLetters != length) {
                ap.minimumPasswordLetters = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordLetters : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (isLimitPasswordAllowed(admin2, 393216) && length < admin2.minimumPasswordLetters) {
                    length = admin2.minimumPasswordLetters;
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordNumeric != length) {
                ap.minimumPasswordNumeric = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordNumeric : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (isLimitPasswordAllowed(admin2, 393216) && length < admin2.minimumPasswordNumeric) {
                    length = admin2.minimumPasswordNumeric;
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordSymbols != length) {
                ap.minimumPasswordSymbols = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordSymbols : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (isLimitPasswordAllowed(admin2, 393216) && length < admin2.minimumPasswordSymbols) {
                    length = admin2.minimumPasswordSymbols;
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0, parent);
            if (ap.minimumPasswordNonLetter != length) {
                ap.minimumPasswordNonLetter = length;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.minimumPasswordNonLetter : 0;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            int N = admins.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = admins.get(i);
                if (isLimitPasswordAllowed(admin2, 393216) && length < admin2.minimumPasswordNonLetter) {
                    length = admin2.minimumPasswordNonLetter;
                }
            }
            return length;
        }
    }

    public boolean isActivePasswordSufficient(int userHandle, boolean parent) {
        boolean zIsActivePasswordSufficientForUserLocked;
        if (!this.mHasFeature) {
            return true;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            getActiveAdminForCallerLocked(null, 0, parent);
            DevicePolicyData policy = getUserDataUnchecked(getCredentialOwner(userHandle, parent));
            zIsActivePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(policy, userHandle, parent);
        }
        return zIsActivePasswordSufficientForUserLocked;
    }

    public boolean isProfileActivePasswordSufficientForParent(int userHandle) {
        boolean zIsActivePasswordSufficientForUserLocked;
        if (!this.mHasFeature) {
            return true;
        }
        enforceFullCrossUsersPermission(userHandle);
        enforceManagedProfile(userHandle, "call APIs refering to the parent profile");
        synchronized (this) {
            int targetUser = getProfileParentId(userHandle);
            DevicePolicyData policy = getUserDataUnchecked(getCredentialOwner(userHandle, false));
            zIsActivePasswordSufficientForUserLocked = isActivePasswordSufficientForUserLocked(policy, targetUser, false);
        }
        return zIsActivePasswordSufficientForUserLocked;
    }

    private boolean isActivePasswordSufficientForUserLocked(DevicePolicyData policy, int userHandle, boolean parent) {
        long id = Binder.clearCallingIdentity();
        try {
            enforceUserUnlocked(userHandle, parent);
            Binder.restoreCallingIdentity(id);
            if (policy.mActivePasswordQuality < getPasswordQuality(null, userHandle, parent) || policy.mActivePasswordLength < getPasswordMinimumLength(null, userHandle, parent)) {
                return false;
            }
            if (policy.mActivePasswordQuality != 393216) {
                return true;
            }
            if (policy.mActivePasswordUpperCase < getPasswordMinimumUpperCase(null, userHandle, parent) || policy.mActivePasswordLowerCase < getPasswordMinimumLowerCase(null, userHandle, parent) || policy.mActivePasswordLetters < getPasswordMinimumLetters(null, userHandle, parent) || policy.mActivePasswordNumeric < getPasswordMinimumNumeric(null, userHandle, parent) || policy.mActivePasswordSymbols < getPasswordMinimumSymbols(null, userHandle, parent)) {
                return false;
            }
            return policy.mActivePasswordNonLetter >= getPasswordMinimumNonLetter(null, userHandle, parent);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(id);
            throw th;
        }
    }

    public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) {
        int i;
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            if (!isCallerWithSystemUid()) {
                getActiveAdminForCallerLocked(null, 1, parent);
            }
            DevicePolicyData policy = getUserDataUnchecked(getCredentialOwner(userHandle, parent));
            i = policy.mFailedPasswordAttempts;
        }
        return i;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName who, int num, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, 4, parent);
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 1, parent);
            if (ap.maximumFailedPasswordsForWipe != num) {
                ap.maximumFailedPasswordsForWipe = num;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName who, int userHandle, boolean parent) {
        ActiveAdmin admin;
        int i;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            if (who != null) {
                admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
            } else {
                admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, parent);
            }
            i = admin != null ? admin.maximumFailedPasswordsForWipe : 0;
        }
        return i;
    }

    public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) {
        int identifier;
        if (!this.mHasFeature) {
            return -10000;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            ActiveAdmin admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, parent);
            identifier = admin != null ? admin.getUserHandle().getIdentifier() : -10000;
        }
        return identifier;
    }

    private ActiveAdmin getAdminWithMinimumFailedPasswordsForWipeLocked(int userHandle, boolean parent) {
        int count = 0;
        ActiveAdmin strictestAdmin = null;
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin = admins.get(i);
            if (admin.maximumFailedPasswordsForWipe != 0) {
                int userId = admin.getUserHandle().getIdentifier();
                if (count == 0 || count > admin.maximumFailedPasswordsForWipe || (count == admin.maximumFailedPasswordsForWipe && getUserInfo(userId).isPrimary())) {
                    count = admin.maximumFailedPasswordsForWipe;
                    strictestAdmin = admin;
                }
            }
        }
        return strictestAdmin;
    }

    private UserInfo getUserInfo(int userId) {
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mUserManager.getUserInfo(userId);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    public boolean resetPassword(String passwordOrNull, int flags) throws RemoteException {
        boolean preN;
        if (!this.mHasFeature) {
            return false;
        }
        int callingUid = this.mInjector.binderGetCallingUid();
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        String password = passwordOrNull != null ? passwordOrNull : "";
        if (TextUtils.isEmpty(password)) {
            enforceNotManagedProfile(userHandle, "clear the active password");
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminWithPolicyForUidLocked(null, -1, callingUid);
            if (admin != null) {
                preN = getTargetSdk(admin.info.getPackageName(), userHandle) <= 23;
            } else {
                preN = getTargetSdk(getActiveAdminForCallerLocked(null, 2).info.getPackageName(), userHandle) <= 23;
                if (TextUtils.isEmpty(password)) {
                    if (!preN) {
                        throw new SecurityException("Cannot call with null password");
                    }
                    Slog.e(LOG_TAG, "Cannot call with null password");
                    return false;
                }
                if (isLockScreenSecureUnchecked(userHandle)) {
                    if (!preN) {
                        throw new SecurityException("Admin cannot change current password");
                    }
                    Slog.e(LOG_TAG, "Admin cannot change current password");
                    return false;
                }
            }
            if (!isManagedProfile(userHandle)) {
                for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
                    if (userInfo.isManagedProfile()) {
                        if (!preN) {
                            throw new IllegalStateException("Cannot reset password on user has managed profile");
                        }
                        Slog.e(LOG_TAG, "Cannot reset password on user has managed profile");
                        return false;
                    }
                }
            }
            if (!this.mUserManager.isUserUnlocked(userHandle)) {
                if (!preN) {
                    throw new IllegalStateException("Cannot reset password when user is locked");
                }
                Slog.e(LOG_TAG, "Cannot reset password when user is locked");
                return false;
            }
            int quality = getPasswordQuality(null, userHandle, false);
            if (quality == 524288) {
                quality = 0;
            }
            if (quality != 0) {
                int realQuality = LockPatternUtils.computePasswordQuality(password);
                if (realQuality < quality && quality != 393216) {
                    Slog.w(LOG_TAG, "resetPassword: password quality 0x" + Integer.toHexString(realQuality) + " does not meet required quality 0x" + Integer.toHexString(quality));
                    return false;
                }
                quality = Math.max(realQuality, quality);
            }
            int length = getPasswordMinimumLength(null, userHandle, false);
            if (password.length() < length) {
                Slog.w(LOG_TAG, "resetPassword: password length " + password.length() + " does not meet required length " + length);
                return false;
            }
            if (quality == 393216) {
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
                    } else if (c < '0' || c > '9') {
                        symbols++;
                        nonletter++;
                    } else {
                        numbers++;
                        nonletter++;
                    }
                }
                int neededLetters = getPasswordMinimumLetters(null, userHandle, false);
                if (letters < neededLetters) {
                    Slog.w(LOG_TAG, "resetPassword: number of letters " + letters + " does not meet required number of letters " + neededLetters);
                    return false;
                }
                int neededNumbers = getPasswordMinimumNumeric(null, userHandle, false);
                if (numbers < neededNumbers) {
                    Slog.w(LOG_TAG, "resetPassword: number of numerical digits " + numbers + " does not meet required number of numerical digits " + neededNumbers);
                    return false;
                }
                int neededLowerCase = getPasswordMinimumLowerCase(null, userHandle, false);
                if (lowercase < neededLowerCase) {
                    Slog.w(LOG_TAG, "resetPassword: number of lowercase letters " + lowercase + " does not meet required number of lowercase letters " + neededLowerCase);
                    return false;
                }
                int neededUpperCase = getPasswordMinimumUpperCase(null, userHandle, false);
                if (uppercase < neededUpperCase) {
                    Slog.w(LOG_TAG, "resetPassword: number of uppercase letters " + uppercase + " does not meet required number of uppercase letters " + neededUpperCase);
                    return false;
                }
                int neededSymbols = getPasswordMinimumSymbols(null, userHandle, false);
                if (symbols < neededSymbols) {
                    Slog.w(LOG_TAG, "resetPassword: number of special symbols " + symbols + " does not meet required number of special symbols " + neededSymbols);
                    return false;
                }
                int neededNonLetter = getPasswordMinimumNonLetter(null, userHandle, false);
                if (nonletter < neededNonLetter) {
                    Slog.w(LOG_TAG, "resetPassword: number of non-letter characters " + nonletter + " does not meet required number of non-letter characters " + neededNonLetter);
                    return false;
                }
            }
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mPasswordOwner >= 0 && policy.mPasswordOwner != callingUid) {
                Slog.w(LOG_TAG, "resetPassword: already set by another uid and not entered by user");
                return false;
            }
            boolean callerIsDeviceOwnerAdmin = isCallerDeviceOwner(callingUid);
            boolean doNotAskCredentialsOnBoot = (flags & 2) != 0;
            if (callerIsDeviceOwnerAdmin && doNotAskCredentialsOnBoot) {
                setDoNotAskCredentialsOnBoot();
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                if (TextUtils.isEmpty(password)) {
                    this.mLockPatternUtils.clearLock(userHandle);
                } else {
                    this.mLockPatternUtils.saveLockPassword(password, (String) null, quality, userHandle);
                }
                boolean requireEntry = (flags & 1) != 0;
                if (requireEntry) {
                    this.mLockPatternUtils.requireStrongAuth(2, -1);
                }
                synchronized (this) {
                    int newOwner = requireEntry ? callingUid : -1;
                    if (policy.mPasswordOwner != newOwner) {
                        policy.mPasswordOwner = newOwner;
                        saveSettingsLocked(userHandle);
                    }
                }
                this.mInjector.binderRestoreCallingIdentity(ident);
                return true;
            } catch (Throwable th) {
                this.mInjector.binderRestoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    private boolean isLockScreenSecureUnchecked(int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            return this.mLockPatternUtils.isSecure(userId);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void setDoNotAskCredentialsOnBoot() {
        synchronized (this) {
            DevicePolicyData policyData = getUserData(0);
            if (!policyData.doNotAskCredentialsOnBoot) {
                policyData.doNotAskCredentialsOnBoot = true;
                saveSettingsLocked(0);
            }
        }
    }

    public boolean getDoNotAskCredentialsOnBoot() {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.QUERY_DO_NOT_ASK_CREDENTIALS_ON_BOOT", null);
        synchronized (this) {
            DevicePolicyData policyData = getUserData(0);
            z = policyData.doNotAskCredentialsOnBoot;
        }
        return z;
    }

    public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 3, parent);
            if (ap.maximumTimeToUnlock != timeMs) {
                ap.maximumTimeToUnlock = timeMs;
                saveSettingsLocked(userHandle);
                updateMaximumTimeToLockLocked(userHandle);
            }
        }
    }

    void updateMaximumTimeToLockLocked(int userHandle) {
        long timeMs = JobStatus.NO_LATEST_RUNTIME;
        int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userHandle);
        for (int profileId : profileIds) {
            DevicePolicyData policy = getUserDataUnchecked(profileId);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin = policy.mAdminList.get(i);
                if (admin.maximumTimeToUnlock > 0 && timeMs > admin.maximumTimeToUnlock) {
                    timeMs = admin.maximumTimeToUnlock;
                }
                if (admin.hasParentActiveAdmin()) {
                    ActiveAdmin parentAdmin = admin.getParentActiveAdmin();
                    if (parentAdmin.maximumTimeToUnlock > 0 && timeMs > parentAdmin.maximumTimeToUnlock) {
                        timeMs = parentAdmin.maximumTimeToUnlock;
                    }
                }
            }
        }
        DevicePolicyData policy2 = getUserDataUnchecked(getProfileParentId(userHandle));
        if (policy2.mLastMaximumTimeToLock == timeMs) {
            return;
        }
        policy2.mLastMaximumTimeToLock = timeMs;
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            if (policy2.mLastMaximumTimeToLock != JobStatus.NO_LATEST_RUNTIME) {
                this.mInjector.settingsGlobalPutInt("stay_on_while_plugged_in", 0);
            }
            this.mInjector.getPowerManagerInternal().setMaximumScreenOffTimeoutFromDeviceAdmin((int) Math.min(policy2.mLastMaximumTimeToLock, 2147483647L));
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                return admin != null ? admin.maximumTimeToUnlock : 0L;
            }
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            return getMaximumTimeToLockPolicyFromAdmins(admins);
        }
    }

    public long getMaximumTimeToLockForUserAndProfiles(int userHandle) {
        long maximumTimeToLockPolicyFromAdmins;
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            ArrayList<ActiveAdmin> admins = new ArrayList<>();
            for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
                DevicePolicyData policy = getUserData(userInfo.id);
                admins.addAll(policy.mAdminList);
                if (userInfo.isManagedProfile()) {
                    for (ActiveAdmin admin : policy.mAdminList) {
                        if (admin.hasParentActiveAdmin()) {
                            admins.add(admin.getParentActiveAdmin());
                        }
                    }
                }
            }
            maximumTimeToLockPolicyFromAdmins = getMaximumTimeToLockPolicyFromAdmins(admins);
        }
        return maximumTimeToLockPolicyFromAdmins;
    }

    private long getMaximumTimeToLockPolicyFromAdmins(List<ActiveAdmin> admins) {
        long time = 0;
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin = admins.get(i);
            if (time == 0) {
                time = admin.maximumTimeToUnlock;
            } else if (admin.maximumTimeToUnlock != 0 && time > admin.maximumTimeToUnlock) {
                time = admin.maximumTimeToUnlock;
            }
        }
        return time;
    }

    public void lockNow(boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        synchronized (this) {
            getActiveAdminForCallerLocked(null, 3, parent);
            int userToLock = this.mInjector.userHandleGetCallingUserId();
            if (parent || !isSeparateProfileChallengeEnabled(userToLock)) {
                userToLock = -1;
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                this.mLockPatternUtils.requireStrongAuth(2, userToLock);
                if (userToLock == -1) {
                    this.mInjector.powerManagerGoToSleep(SystemClock.uptimeMillis(), 1, 0);
                    this.mInjector.getIWindowManager().lockNow((Bundle) null);
                } else {
                    this.mInjector.getTrustManager().setDeviceLockedForUser(userToLock, true);
                }
            } catch (RemoteException e) {
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public void enforceCanManageCaCerts(ComponentName who) {
        if (who == null) {
            if (isCallerDelegatedCertInstaller()) {
                return;
            }
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CA_CERTIFICATES", null);
        } else {
            synchronized (this) {
                getActiveAdminForCallerLocked(who, -1);
            }
        }
    }

    private void enforceCanManageInstalledKeys(ComponentName who) {
        if (who == null) {
            if (isCallerDelegatedCertInstaller()) {
            } else {
                throw new SecurityException("who == null, but caller is not cert installer");
            }
        } else {
            synchronized (this) {
                getActiveAdminForCallerLocked(who, -1);
            }
        }
    }

    private boolean isCallerDelegatedCertInstaller() {
        int callingUid = this.mInjector.binderGetCallingUid();
        int userHandle = UserHandle.getUserId(callingUid);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mDelegatedCertInstallerPackage == null) {
                return false;
            }
            try {
                int uid = this.mContext.getPackageManager().getPackageUidAsUser(policy.mDelegatedCertInstallerPackage, userHandle);
                return uid == callingUid;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }

    public boolean approveCaCert(String alias, int userId, boolean approval) {
        enforceManageUsers();
        synchronized (this) {
            Set<String> certs = getUserData(userId).mAcceptedCaCertificates;
            boolean changed = approval ? certs.add(alias) : certs.remove(alias);
            if (!changed) {
                return false;
            }
            saveSettingsLocked(userId);
            new MonitoringCertNotificationTask(this, null).execute(Integer.valueOf(userId));
            return true;
        }
    }

    public boolean isCaCertApproved(String alias, int userId) {
        boolean zContains;
        enforceManageUsers();
        synchronized (this) {
            zContains = getUserData(userId).mAcceptedCaCertificates.contains(alias);
        }
        return zContains;
    }

    private void removeCaApprovalsIfNeeded(int userId) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(userId)) {
            boolean isSecure = this.mLockPatternUtils.isSecure(userInfo.id);
            if (userInfo.isManagedProfile()) {
                isSecure |= this.mLockPatternUtils.isSecure(getProfileParentId(userInfo.id));
            }
            if (!isSecure) {
                synchronized (this) {
                    getUserData(userInfo.id).mAcceptedCaCertificates.clear();
                    saveSettingsLocked(userInfo.id);
                }
                new MonitoringCertNotificationTask(this, null).execute(Integer.valueOf(userInfo.id));
            }
        }
    }

    public boolean installCaCert(ComponentName admin, byte[] certBuffer) throws RemoteException {
        enforceCanManageCaCerts(admin);
        try {
            X509Certificate cert = parseCert(certBuffer);
            byte[] pemCert = Credentials.convertToPem(new Certificate[]{cert});
            UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
                try {
                    keyChainConnection.getService().installCaCertificate(pemCert);
                    return true;
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "installCaCertsToKeyChain(): ", e);
                    return false;
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e1) {
                Log.w(LOG_TAG, "installCaCertsToKeyChain(): ", e1);
                Thread.currentThread().interrupt();
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Problem reading cert", ioe);
            return false;
        } catch (CertificateException ce) {
            Log.e(LOG_TAG, "Problem converting cert", ce);
            return false;
        }
    }

    private static X509Certificate parseCert(byte[] certBuffer) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBuffer));
    }

    public void uninstallCaCerts(ComponentName admin, String[] aliases) {
        enforceCanManageCaCerts(admin);
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
            for (String str : aliases) {
                try {
                    keyChainConnection.getService().deleteCaCertificate(str);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "from CaCertUninstaller: ", e);
                } finally {
                    keyChainConnection.close();
                }
            }
        } catch (InterruptedException ie) {
            Log.w(LOG_TAG, "CaCertUninstaller: ", ie);
            Thread.currentThread().interrupt();
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public boolean installKeyPair(ComponentName who, byte[] privKey, byte[] cert, byte[] chain, String alias, boolean requestAccess) {
        Injector injector;
        enforceCanManageInstalledKeys(who);
        int callingUid = this.mInjector.binderGetCallingUid();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, UserHandle.getUserHandleForUid(callingUid));
            try {
                IKeyChainService keyChain = keyChainConnection.getService();
                if (!keyChain.installKeyPair(privKey, cert, chain, alias)) {
                    return false;
                }
                if (requestAccess) {
                    keyChain.setGrant(callingUid, alias, true);
                }
                return true;
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Installing certificate", e);
                return false;
            } finally {
                keyChainConnection.close();
            }
        } catch (InterruptedException e2) {
            Log.w(LOG_TAG, "Interrupted while installing certificate", e2);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public boolean removeKeyPair(ComponentName who, String alias) {
        enforceCanManageInstalledKeys(who);
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        long id = Binder.clearCallingIdentity();
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
            try {
                IKeyChainService keyChain = keyChainConnection.getService();
                return keyChain.removeKeyPair(alias);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Removing keypair", e);
                return false;
            } finally {
                keyChainConnection.close();
            }
        } catch (InterruptedException e2) {
            Log.w(LOG_TAG, "Interrupted while removing keypair", e2);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    public void choosePrivateKeyAlias(int uid, Uri uri, String alias, final IBinder response) {
        ActiveAdmin deviceOwnerAdmin;
        if (!isCallerWithSystemUid()) {
            return;
        }
        UserHandle caller = this.mInjector.binderGetCallingUserHandle();
        ComponentName aliasChooser = getProfileOwner(caller.getIdentifier());
        if (aliasChooser == null && caller.isSystem() && (deviceOwnerAdmin = getDeviceOwnerAdminLocked()) != null) {
            aliasChooser = deviceOwnerAdmin.info.getComponent();
        }
        if (aliasChooser == null) {
            sendPrivateKeyAliasResponse(null, response);
            return;
        }
        Intent intent = new Intent("android.app.action.CHOOSE_PRIVATE_KEY_ALIAS");
        intent.setComponent(aliasChooser);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_SENDER_UID", uid);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_URI", uri);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_ALIAS", alias);
        intent.putExtra("android.app.extra.CHOOSE_PRIVATE_KEY_RESPONSE", response);
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            this.mContext.sendOrderedBroadcastAsUser(intent, caller, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent2) {
                    String chosenAlias = getResultData();
                    DevicePolicyManagerService.this.sendPrivateKeyAliasResponse(chosenAlias, response);
                }
            }, null, -1, null, null);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    private void sendPrivateKeyAliasResponse(final String alias, IBinder responseBinder) {
        final IKeyChainAliasCallback keyChainAliasResponse = IKeyChainAliasCallback.Stub.asInterface(responseBinder);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                try {
                    Slog.d(DevicePolicyManagerService.LOG_TAG, "sendPrivateKeyAliasResponse alias = " + alias + ", keyChainAliasResponse = " + keyChainAliasResponse);
                    keyChainAliasResponse.alias(alias);
                    return null;
                } catch (Exception e) {
                    Log.e(DevicePolicyManagerService.LOG_TAG, "error while responding to callback", e);
                    return null;
                }
            }
        }.execute(new Void[0]);
    }

    public void setCertInstallerPackage(ComponentName who, String installerPackage) throws SecurityException {
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            if (getTargetSdk(who.getPackageName(), userHandle) >= 24 && installerPackage != null && !isPackageInstalledForUser(installerPackage, userHandle)) {
                throw new IllegalArgumentException("Package " + installerPackage + " is not installed on the current user");
            }
            DevicePolicyData policy = getUserData(userHandle);
            policy.mDelegatedCertInstallerPackage = installerPackage;
            saveSettingsLocked(userHandle);
        }
    }

    public String getCertInstallerPackage(ComponentName who) throws SecurityException {
        String str;
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            DevicePolicyData policy = getUserData(userHandle);
            str = policy.mDelegatedCertInstallerPackage;
        }
        return str;
    }

    public boolean setAlwaysOnVpnPackage(ComponentName admin, String vpnPackage, boolean lockdown) throws SecurityException {
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
        }
        int userId = this.mInjector.userHandleGetCallingUserId();
        long token = this.mInjector.binderClearCallingIdentity();
        if (vpnPackage != null) {
            try {
                if (!isPackageInstalledForUser(vpnPackage, userId)) {
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(token);
            }
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (!connectivityManager.setAlwaysOnVpnPackageForUser(userId, vpnPackage, lockdown)) {
            throw new UnsupportedOperationException();
        }
        this.mInjector.binderRestoreCallingIdentity(token);
        return true;
    }

    public String getAlwaysOnVpnPackage(ComponentName admin) throws SecurityException {
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
        }
        int userId = this.mInjector.userHandleGetCallingUserId();
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            return connectivityManager.getAlwaysOnVpnPackageForUser(userId);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    private void wipeDataNoLock(boolean wipeExtRequested, String reason) {
        if (wipeExtRequested) {
            StorageManager sm = (StorageManager) this.mContext.getSystemService("storage");
            sm.wipeAdoptableDisks();
        }
        try {
            RecoverySystem.rebootWipeUserData(this.mContext, reason);
        } catch (IOException | SecurityException e) {
            Slog.w(LOG_TAG, "Failed requesting data wipe", e);
        }
    }

    public void wipeData(int flags) {
        String source;
        if (!this.mHasFeature) {
            return;
        }
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(null, 4);
            source = admin.info.getComponent().flattenToShortString();
            long ident = this.mInjector.binderClearCallingIdentity();
            if ((flags & 2) != 0) {
                try {
                    if (!isDeviceOwner(admin.info.getComponent(), userHandle)) {
                        throw new SecurityException("Only device owner admins can set WIPE_RESET_PROTECTION_DATA");
                    }
                    PersistentDataBlockManager manager = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
                    if (manager != null) {
                        manager.wipe();
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        }
        boolean wipeExtRequested = (flags & 1) != 0;
        wipeDeviceNoLock(wipeExtRequested, userHandle, "DevicePolicyManager.wipeData() from " + source);
    }

    private void wipeDeviceNoLock(boolean wipeExtRequested, final int userHandle, String reason) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            if (userHandle == 0) {
                wipeDataNoLock(wipeExtRequested, reason);
            } else {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IActivityManager am = DevicePolicyManagerService.this.mInjector.getIActivityManager();
                            if (am.getCurrentUser().id == userHandle) {
                                am.switchUser(0);
                            }
                            boolean isManagedProfile = DevicePolicyManagerService.this.isManagedProfile(userHandle);
                            if (!DevicePolicyManagerService.this.mUserManager.removeUser(userHandle)) {
                                Slog.w(DevicePolicyManagerService.LOG_TAG, "Couldn't remove user " + userHandle);
                            } else {
                                if (!isManagedProfile) {
                                    return;
                                }
                                DevicePolicyManagerService.this.sendWipeProfileNotification();
                            }
                        } catch (RemoteException e) {
                        }
                    }
                });
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void sendWipeProfileNotification() {
        String contentText = this.mContext.getString(R.string.accessibility_shortcut_single_service_warning_title);
        Notification notification = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.stat_sys_warning).setContentTitle(this.mContext.getString(R.string.accessibility_shortcut_off)).setContentText(contentText).setColor(this.mContext.getColor(R.color.system_accent3_600)).setStyle(new Notification.BigTextStyle().bigText(contentText)).build();
        this.mInjector.getNotificationManager().notify(1001, notification);
    }

    private void clearWipeProfileNotification() {
        this.mInjector.getNotificationManager().cancel(1001);
    }

    public void getRemoveWarning(ComponentName comp, final RemoteCallback result, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(comp, userHandle);
            if (admin == null) {
                result.sendResult((Bundle) null);
                return;
            }
            Intent intent = new Intent("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
            intent.setFlags(268435456);
            intent.setComponent(admin.info.getComponent());
            this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(userHandle), null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent2) {
                    result.sendResult(getResultExtras(false));
                }
            }, null, -1, null, null);
        }
    }

    public void setActivePasswordState(int quality, int length, int letters, int uppercase, int lowercase, int numbers, int symbols, int nonletter, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (isManagedProfile(userHandle) && !isSeparateProfileChallengeEnabled(userHandle)) {
            quality = 0;
            length = 0;
            letters = 0;
            uppercase = 0;
            lowercase = 0;
            numbers = 0;
            symbols = 0;
            nonletter = 0;
        }
        validateQualityConstant(quality);
        DevicePolicyData policy = getUserData(userHandle);
        synchronized (this) {
            policy.mActivePasswordQuality = quality;
            policy.mActivePasswordLength = length;
            policy.mActivePasswordLetters = letters;
            policy.mActivePasswordLowerCase = lowercase;
            policy.mActivePasswordUpperCase = uppercase;
            policy.mActivePasswordNumeric = numbers;
            policy.mActivePasswordSymbols = symbols;
            policy.mActivePasswordNonLetter = nonletter;
        }
    }

    public void reportPasswordChanged(int userId) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(userId);
        if (!isSeparateProfileChallengeEnabled(userId)) {
            enforceNotManagedProfile(userId, "set the active password");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        DevicePolicyData policy = getUserData(userId);
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this) {
                policy.mFailedPasswordAttempts = 0;
                saveSettingsLocked(userId);
                updatePasswordExpirationsLocked(userId);
                setExpirationAlarmCheckLocked(this.mContext, userId, false);
                sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_CHANGED", 0, userId);
            }
            removeCaApprovalsIfNeeded(userId);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private void updatePasswordExpirationsLocked(int userHandle) {
        ArraySet<Integer> affectedUserIds = new ArraySet<>();
        List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, false);
        int N = admins.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin admin = admins.get(i);
            if (admin.info.usesPolicy(6)) {
                affectedUserIds.add(Integer.valueOf(admin.getUserHandle().getIdentifier()));
                long timeout = admin.passwordExpirationTimeout;
                long expiration = timeout > 0 ? timeout + System.currentTimeMillis() : 0L;
                admin.passwordExpirationDate = expiration;
            }
        }
        Iterator affectedUserId$iterator = affectedUserIds.iterator();
        while (affectedUserId$iterator.hasNext()) {
            int affectedUserId = ((Integer) affectedUserId$iterator.next()).intValue();
            saveSettingsLocked(affectedUserId);
        }
    }

    public void reportFailedPasswordAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        if (!isSeparateProfileChallengeEnabled(userHandle)) {
            enforceNotManagedProfile(userHandle, "report failed password attempt if separate profile challenge is not in place");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        long ident = this.mInjector.binderClearCallingIdentity();
        boolean wipeData = false;
        int identifier = 0;
        try {
            synchronized (this) {
                DevicePolicyData policy = getUserData(userHandle);
                policy.mFailedPasswordAttempts++;
                saveSettingsLocked(userHandle);
                if (this.mHasFeature) {
                    ActiveAdmin strictestAdmin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle, false);
                    int max = strictestAdmin != null ? strictestAdmin.maximumFailedPasswordsForWipe : 0;
                    if (max > 0 && policy.mFailedPasswordAttempts >= max) {
                        wipeData = true;
                        identifier = strictestAdmin.getUserHandle().getIdentifier();
                    }
                    sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_FAILED", 1, userHandle);
                }
            }
            if (wipeData) {
                wipeDeviceNoLock(false, identifier, "reportFailedPasswordAttempt()");
            }
            this.mInjector.binderRestoreCallingIdentity(ident);
            if (!this.mInjector.securityLogIsLoggingEnabled()) {
                return;
            }
            SecurityLog.writeEvent(210007, new Object[]{0, 1});
        } catch (Throwable th) {
            this.mInjector.binderRestoreCallingIdentity(ident);
            throw th;
        }
    }

    public void reportSuccessfulPasswordAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mFailedPasswordAttempts != 0 || policy.mPasswordOwner >= 0) {
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    policy.mFailedPasswordAttempts = 0;
                    policy.mPasswordOwner = -1;
                    saveSettingsLocked(userHandle);
                    if (this.mHasFeature) {
                        sendAdminCommandForLockscreenPoliciesLocked("android.app.action.ACTION_PASSWORD_SUCCEEDED", 1, userHandle);
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        }
        if (!this.mInjector.securityLogIsLoggingEnabled()) {
            return;
        }
        SecurityLog.writeEvent(210007, new Object[]{1, 1});
    }

    public void reportFailedFingerprintAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (!this.mInjector.securityLogIsLoggingEnabled()) {
            return;
        }
        SecurityLog.writeEvent(210007, new Object[]{0, 0});
    }

    public void reportSuccessfulFingerprintAttempt(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (!this.mInjector.securityLogIsLoggingEnabled()) {
            return;
        }
        SecurityLog.writeEvent(210007, new Object[]{1, 0});
    }

    public void reportKeyguardDismissed(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (!this.mInjector.securityLogIsLoggingEnabled()) {
            return;
        }
        SecurityLog.writeEvent(210006, new Object[0]);
    }

    public void reportKeyguardSecured(int userHandle) {
        enforceFullCrossUsersPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        if (!this.mInjector.securityLogIsLoggingEnabled()) {
            return;
        }
        SecurityLog.writeEvent(210008, new Object[0]);
    }

    public ComponentName setGlobalProxy(ComponentName who, String proxySpec, String exclusionList) {
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (this) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            DevicePolicyData policy = getUserData(0);
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, 5);
            Set<ComponentName> compSet = policy.mAdminMap.keySet();
            for (ComponentName component : compSet) {
                ActiveAdmin ap = policy.mAdminMap.get(component);
                if (ap.specifiesGlobalProxy && !component.equals(who)) {
                    return component;
                }
            }
            if (UserHandle.getCallingUserId() != 0) {
                Slog.w(LOG_TAG, "Only the owner is allowed to set the global proxy. User " + UserHandle.getCallingUserId() + " is not permitted.");
                return null;
            }
            if (proxySpec == null) {
                admin.specifiesGlobalProxy = false;
                admin.globalProxySpec = null;
                admin.globalProxyExclusionList = null;
            } else {
                admin.specifiesGlobalProxy = true;
                admin.globalProxySpec = proxySpec;
                admin.globalProxyExclusionList = exclusionList;
            }
            long origId = this.mInjector.binderClearCallingIdentity();
            try {
                resetGlobalProxyLocked(policy);
                return null;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(origId);
            }
        }
    }

    public ComponentName getGlobalProxyAdmin(int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            DevicePolicyData policy = getUserData(0);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin ap = policy.mAdminList.get(i);
                if (ap.specifiesGlobalProxy) {
                    return ap.info.getComponent();
                }
            }
            return null;
        }
    }

    public void setRecommendedGlobalProxy(ComponentName who, ProxyInfo proxyInfo) {
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
        }
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            connectivityManager.setGlobalProxy(proxyInfo);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    private void resetGlobalProxyLocked(DevicePolicyData policy) {
        int N = policy.mAdminList.size();
        for (int i = 0; i < N; i++) {
            ActiveAdmin ap = policy.mAdminList.get(i);
            if (ap.specifiesGlobalProxy) {
                saveGlobalProxyLocked(ap.globalProxySpec, ap.globalProxyExclusionList);
                return;
            }
        }
        saveGlobalProxyLocked(null, null);
    }

    private void saveGlobalProxyLocked(String proxySpec, String exclusionList) {
        if (exclusionList == null) {
            exclusionList = "";
        }
        if (proxySpec == null) {
            proxySpec = "";
        }
        String[] data = proxySpec.trim().split(":");
        int proxyPort = 8080;
        if (data.length > 1) {
            try {
                proxyPort = Integer.parseInt(data[1]);
            } catch (NumberFormatException e) {
            }
        }
        String exclusionList2 = exclusionList.trim();
        ProxyInfo proxyProperties = new ProxyInfo(data[0], proxyPort, exclusionList2);
        if (!proxyProperties.isValid()) {
            Slog.e(LOG_TAG, "Invalid proxy properties, ignoring: " + proxyProperties.toString());
            return;
        }
        this.mInjector.settingsGlobalPutString("global_http_proxy_host", data[0]);
        this.mInjector.settingsGlobalPutInt("global_http_proxy_port", proxyPort);
        this.mInjector.settingsGlobalPutString("global_http_proxy_exclusion_list", exclusionList2);
    }

    public int setStorageEncryption(ComponentName who, boolean encrypt) {
        int i;
        if (!this.mHasFeature) {
            return 0;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            if (userHandle != 0) {
                Slog.w(LOG_TAG, "Only owner/system user is allowed to set storage encryption. User " + UserHandle.getCallingUserId() + " is not permitted.");
                return 0;
            }
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 7);
            if (!isEncryptionSupported()) {
                return 0;
            }
            if (ap.encryptionRequested != encrypt) {
                ap.encryptionRequested = encrypt;
                saveSettingsLocked(userHandle);
            }
            DevicePolicyData policy = getUserData(0);
            boolean newRequested = false;
            int N = policy.mAdminList.size();
            for (int i2 = 0; i2 < N; i2++) {
                newRequested |= policy.mAdminList.get(i2).encryptionRequested;
            }
            setEncryptionRequested(newRequested);
            if (newRequested) {
                i = 3;
            } else {
                i = 1;
            }
            return i;
        }
    }

    public boolean getStorageEncryption(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            if (who != null) {
                ActiveAdmin ap = getActiveAdminUncheckedLocked(who, userHandle);
                return ap != null ? ap.encryptionRequested : false;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                if (policy.mAdminList.get(i).encryptionRequested) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getStorageEncryptionStatus(String callerPackage, int userHandle) {
        if (!this.mHasFeature) {
        }
        enforceFullCrossUsersPermission(userHandle);
        ensureCallerPackage(callerPackage);
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(callerPackage, 0, userHandle);
            boolean legacyApp = false;
            if (ai.targetSdkVersion <= 23) {
                legacyApp = true;
            } else if ("com.google.android.apps.enterprise.dmagent".equals(ai.packageName) && ai.versionCode == 697) {
                legacyApp = true;
            }
            int rawStatus = getEncryptionStatus();
            if (rawStatus == 5 && legacyApp) {
                return 3;
            }
            return rawStatus;
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

    private void setEncryptionRequested(boolean encrypt) {
    }

    public void setScreenCaptureDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1);
            if (ap.disableScreenCapture != disabled) {
                ap.disableScreenCapture = disabled;
                saveSettingsLocked(userHandle);
                updateScreenCaptureDisabledInWindowManager(userHandle, disabled);
            }
        }
    }

    public boolean getScreenCaptureDisabled(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (this) {
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                return admin != null ? admin.disableScreenCapture : false;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                if (policy.mAdminList.get(i).disableScreenCapture) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateScreenCaptureDisabledInWindowManager(final int userHandle, final boolean disabled) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    DevicePolicyManagerService.this.mInjector.getIWindowManager().setScreenCaptureDisabled(userHandle, disabled);
                } catch (RemoteException e) {
                    Log.w(DevicePolicyManagerService.LOG_TAG, "Unable to notify WindowManager.", e);
                }
            }
        });
    }

    public void setAutoTimeRequired(ComponentName who, boolean required) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -2);
            if (admin.requireAutoTime != required) {
                admin.requireAutoTime = required;
                saveSettingsLocked(userHandle);
            }
        }
        if (!required) {
            return;
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            this.mInjector.settingsGlobalPutInt("auto_time", 1);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public boolean getAutoTimeRequired() {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (this) {
            ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
            z = deviceOwner != null ? deviceOwner.requireAutoTime : false;
        }
        return z;
    }

    public void setForceEphemeralUsers(ComponentName who, boolean forceEphemeralUsers) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (forceEphemeralUsers && !this.mInjector.userManagerIsSplitSystemUser()) {
            throw new UnsupportedOperationException("Cannot force ephemeral users on systems without split system user.");
        }
        boolean removeAllUsers = false;
        synchronized (this) {
            ActiveAdmin deviceOwner = getActiveAdminForCallerLocked(who, -2);
            if (deviceOwner.forceEphemeralUsers != forceEphemeralUsers) {
                deviceOwner.forceEphemeralUsers = forceEphemeralUsers;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
                this.mUserManagerInternal.setForceEphemeralUsers(forceEphemeralUsers);
                removeAllUsers = forceEphemeralUsers;
            }
        }
        if (!removeAllUsers) {
            return;
        }
        long identitity = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManagerInternal.removeAllUsers();
        } finally {
            this.mInjector.binderRestoreCallingIdentity(identitity);
        }
    }

    public boolean getForceEphemeralUsers(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin deviceOwner = getActiveAdminForCallerLocked(who, -2);
            z = deviceOwner.forceEphemeralUsers;
        }
        return z;
    }

    private boolean isDeviceOwnerManagedSingleUserDevice() {
        synchronized (this) {
            if (!this.mOwners.hasDeviceOwner()) {
                return false;
            }
            long callingIdentity = this.mInjector.binderClearCallingIdentity();
            try {
                if (this.mInjector.userManagerIsSplitSystemUser()) {
                    if (this.mUserManager.getUserCount() != 2 || this.mOwners.getDeviceOwnerUserId() == 0) {
                        z = false;
                    }
                    return z;
                }
                return this.mUserManager.getUserCount() == 1;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(callingIdentity);
            }
        }
    }

    private void ensureDeviceOwnerManagingSingleUser(ComponentName who) throws SecurityException {
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
        }
        if (isDeviceOwnerManagedSingleUserDevice()) {
        } else {
            throw new SecurityException("There should only be one user, managed by Device Owner");
        }
    }

    public boolean requestBugreport(ComponentName who) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        ensureDeviceOwnerManagingSingleUser(who);
        if (this.mRemoteBugreportServiceIsActive.get() || getDeviceOwnerRemoteBugreportUri() != null) {
            Slog.d(LOG_TAG, "Remote bugreport wasn't started because there's already one running.");
            return false;
        }
        long callingIdentity = this.mInjector.binderClearCallingIdentity();
        try {
            ActivityManagerNative.getDefault().requestBugReport(2);
            this.mRemoteBugreportServiceIsActive.set(true);
            this.mRemoteBugreportSharingAccepted.set(false);
            registerRemoteBugreportReceivers();
            this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 678432343, RemoteBugreportUtils.buildNotification(this.mContext, 1), UserHandle.ALL);
            this.mHandler.postDelayed(this.mRemoteBugreportTimeoutRunnable, LocationFudger.FASTEST_INTERVAL_MS);
            return true;
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Failed to make remote calls to start bugreportremote service", re);
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(callingIdentity);
        }
    }

    synchronized void sendDeviceOwnerCommand(String action, Bundle extras) {
        Intent intent = new Intent(action);
        intent.setComponent(this.mOwners.getDeviceOwnerComponent());
        if (extras != null) {
            intent.putExtras(extras);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
    }

    private synchronized String getDeviceOwnerRemoteBugreportUri() {
        return this.mOwners.getDeviceOwnerRemoteBugreportUri();
    }

    private synchronized void setDeviceOwnerRemoteBugreportUriAndHash(String bugreportUri, String bugreportHash) {
        this.mOwners.setDeviceOwnerRemoteBugreportUriAndHash(bugreportUri, bugreportHash);
    }

    private void registerRemoteBugreportReceivers() {
        try {
            IntentFilter filterFinished = new IntentFilter("android.intent.action.REMOTE_BUGREPORT_DISPATCH", "application/vnd.android.bugreport");
            this.mContext.registerReceiver(this.mRemoteBugreportFinishedReceiver, filterFinished);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Slog.w(LOG_TAG, "Failed to set type application/vnd.android.bugreport", e);
        }
        IntentFilter filterConsent = new IntentFilter();
        filterConsent.addAction("com.android.server.action.BUGREPORT_SHARING_DECLINED");
        filterConsent.addAction("com.android.server.action.BUGREPORT_SHARING_ACCEPTED");
        this.mContext.registerReceiver(this.mRemoteBugreportConsentReceiver, filterConsent);
    }

    private void onBugreportFinished(Intent intent) {
        this.mHandler.removeCallbacks(this.mRemoteBugreportTimeoutRunnable);
        this.mRemoteBugreportServiceIsActive.set(false);
        Uri bugreportUri = intent.getData();
        String bugreportUriString = null;
        if (bugreportUri != null) {
            bugreportUriString = bugreportUri.toString();
        }
        String bugreportHash = intent.getStringExtra("android.intent.extra.REMOTE_BUGREPORT_HASH");
        if (this.mRemoteBugreportSharingAccepted.get()) {
            shareBugreportWithDeviceOwnerIfExists(bugreportUriString, bugreportHash);
            this.mInjector.getNotificationManager().cancel(LOG_TAG, 678432343);
        } else {
            setDeviceOwnerRemoteBugreportUriAndHash(bugreportUriString, bugreportHash);
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
        Bundle extras = new Bundle();
        extras.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 0);
        sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", extras);
        this.mContext.unregisterReceiver(this.mRemoteBugreportConsentReceiver);
        this.mContext.unregisterReceiver(this.mRemoteBugreportFinishedReceiver);
    }

    private void onBugreportSharingAccepted() {
        String bugreportUriString;
        String bugreportHash;
        this.mRemoteBugreportSharingAccepted.set(true);
        synchronized (this) {
            bugreportUriString = getDeviceOwnerRemoteBugreportUri();
            bugreportHash = this.mOwners.getDeviceOwnerRemoteBugreportHash();
        }
        if (bugreportUriString != null) {
            shareBugreportWithDeviceOwnerIfExists(bugreportUriString, bugreportHash);
        } else {
            if (!this.mRemoteBugreportServiceIsActive.get()) {
                return;
            }
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

    private void shareBugreportWithDeviceOwnerIfExists(String bugreportUriString, String bugreportHash) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            try {
                if (bugreportUriString == null) {
                    throw new FileNotFoundException();
                }
                Uri bugreportUri = Uri.parse(bugreportUriString);
                ParcelFileDescriptor pfd = this.mContext.getContentResolver().openFileDescriptor(bugreportUri, "r");
                synchronized (this) {
                    Intent intent = new Intent("android.app.action.BUGREPORT_SHARE");
                    intent.setComponent(this.mOwners.getDeviceOwnerComponent());
                    intent.setDataAndType(bugreportUri, "application/vnd.android.bugreport");
                    intent.putExtra("android.app.extra.BUGREPORT_HASH", bugreportHash);
                    this.mContext.grantUriPermission(this.mOwners.getDeviceOwnerComponent().getPackageName(), bugreportUri, 1);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.of(this.mOwners.getDeviceOwnerUserId()));
                }
                if (pfd != null) {
                    try {
                        pfd.close();
                    } catch (IOException e) {
                    }
                }
                this.mRemoteBugreportSharingAccepted.set(false);
                setDeviceOwnerRemoteBugreportUriAndHash(null, null);
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e2) {
                    }
                }
                this.mRemoteBugreportSharingAccepted.set(false);
                setDeviceOwnerRemoteBugreportUriAndHash(null, null);
                throw th;
            }
        } catch (FileNotFoundException e3) {
            Bundle extras = new Bundle();
            extras.putInt("android.app.extra.BUGREPORT_FAILURE_REASON", 1);
            sendDeviceOwnerCommand("android.app.action.BUGREPORT_FAILED", extras);
            if (0 != 0) {
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e4) {
                }
            }
            this.mRemoteBugreportSharingAccepted.set(false);
            setDeviceOwnerRemoteBugreportUriAndHash(null, null);
        }
    }

    public void setCameraDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 8);
            if (ap.disableCamera != disabled) {
                ap.disableCamera = disabled;
                saveSettingsLocked(userHandle);
            }
        }
        pushUserRestrictions(userHandle);
    }

    public boolean getCameraDisabled(ComponentName who, int userHandle) {
        return getCameraDisabled(who, userHandle, true);
    }

    private boolean getCameraDisabled(ComponentName who, int userHandle, boolean mergeDeviceOwnerRestriction) {
        ActiveAdmin deviceOwner;
        if (!this.mHasFeature) {
            return false;
        }
        synchronized (this) {
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                return admin != null ? admin.disableCamera : false;
            }
            if (mergeDeviceOwnerRestriction && (deviceOwner = getDeviceOwnerAdminLocked()) != null && deviceOwner.disableCamera) {
                return true;
            }
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                if (policy.mAdminList.get(i).disableCamera) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        if (isManagedProfile(userHandle)) {
            if (parent) {
                which &= 48;
            } else {
                which &= 56;
            }
        }
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 9, parent);
            if (ap.disabledKeyguardFeatures != which) {
                ap.disabledKeyguardFeatures = which;
                saveSettingsLocked(userHandle);
            }
        }
    }

    public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) {
        List<ActiveAdmin> admins;
        int i;
        if (!this.mHasFeature) {
            return 0;
        }
        enforceFullCrossUsersPermission(userHandle);
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle, parent);
                    return admin != null ? admin.disabledKeyguardFeatures : 0;
                }
                if (!parent && isManagedProfile(userHandle)) {
                    admins = getUserDataUnchecked(userHandle).mAdminList;
                } else {
                    admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
                }
                int which = 0;
                int N = admins.size();
                for (int i2 = 0; i2 < N; i2++) {
                    ActiveAdmin admin2 = admins.get(i2);
                    int userId = admin2.getUserHandle().getIdentifier();
                    boolean isRequestedUser = !parent && userId == userHandle;
                    if (isRequestedUser || !isManagedProfile(userId)) {
                        i = admin2.disabledKeyguardFeatures;
                    } else {
                        i = admin2.disabledKeyguardFeatures & 48;
                    }
                    which |= i;
                }
                return which;
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void setKeepUninstalledPackages(ComponentName who, List<String> packageList) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkNotNull(packageList, "packageList is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -2);
            admin.keepUninstalledPackages = packageList;
            saveSettingsLocked(userHandle);
            this.mInjector.getPackageManagerInternal().setKeepUninstalledPackages(packageList);
        }
    }

    public List<String> getKeepUninstalledPackages(ComponentName who) {
        List<String> keepUninstalledPackagesLocked;
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            keepUninstalledPackagesLocked = getKeepUninstalledPackagesLocked();
        }
        return keepUninstalledPackagesLocked;
    }

    private List<String> getKeepUninstalledPackagesLocked() {
        ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
        if (deviceOwner != null) {
            return deviceOwner.keepUninstalledPackages;
        }
        return null;
    }

    public boolean setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        if (!this.mHasFeature) {
            return false;
        }
        if (admin == null || !isPackageInstalledForUser(admin.getPackageName(), userId)) {
            throw new IllegalArgumentException("Invalid component " + admin + " for device owner");
        }
        synchronized (this) {
            enforceCanSetDeviceOwnerLocked(userId);
            if (getActiveAdminUncheckedLocked(admin, userId) == null) {
                throw new IllegalArgumentException("Not active admin: " + admin);
            }
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    if (this.mInjector.getIBackupManager() != null) {
                        this.mInjector.getIBackupManager().setBackupServiceActive(0, false);
                        this.mInjector.binderRestoreCallingIdentity(ident);
                        this.mOwners.setDeviceOwner(admin, ownerName, userId);
                        this.mOwners.writeDeviceOwner();
                        updateDeviceOwnerLocked();
                        setDeviceOwnerSystemPropertyLocked();
                        Intent intent = new Intent("android.app.action.DEVICE_OWNER_CHANGED");
                        ident = this.mInjector.binderClearCallingIdentity();
                        try {
                            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
                        } finally {
                        }
                    } else {
                        this.mInjector.binderRestoreCallingIdentity(ident);
                        this.mOwners.setDeviceOwner(admin, ownerName, userId);
                        this.mOwners.writeDeviceOwner();
                        updateDeviceOwnerLocked();
                        setDeviceOwnerSystemPropertyLocked();
                        Intent intent2 = new Intent("android.app.action.DEVICE_OWNER_CHANGED");
                        ident = this.mInjector.binderClearCallingIdentity();
                        this.mContext.sendBroadcastAsUser(intent2, new UserHandle(userId));
                    }
                } finally {
                }
            } catch (RemoteException e) {
                throw new IllegalStateException("Failed deactivating backup service.", e);
            }
        }
        return true;
    }

    public boolean isDeviceOwner(ComponentName who, int userId) {
        boolean zEquals;
        synchronized (this) {
            zEquals = (this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userId) ? this.mOwners.getDeviceOwnerComponent().equals(who) : false;
        }
        return zEquals;
    }

    public boolean isProfileOwner(ComponentName who, int userId) {
        ComponentName profileOwner = getProfileOwner(userId);
        if (who != null) {
            return who.equals(profileOwner);
        }
        return false;
    }

    public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) {
        if (!this.mHasFeature) {
            return null;
        }
        if (!callingUserOnly) {
            enforceManageUsers();
        }
        synchronized (this) {
            if (!this.mOwners.hasDeviceOwner()) {
                return null;
            }
            if (callingUserOnly && this.mInjector.userHandleGetCallingUserId() != this.mOwners.getDeviceOwnerUserId()) {
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
        synchronized (this) {
            deviceOwnerUserId = this.mOwners.hasDeviceOwner() ? this.mOwners.getDeviceOwnerUserId() : -10000;
        }
        return deviceOwnerUserId;
    }

    public String getDeviceOwnerName() {
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        synchronized (this) {
            if (!this.mOwners.hasDeviceOwner()) {
                return null;
            }
            String deviceOwnerPackage = this.mOwners.getDeviceOwnerPackageName();
            return getApplicationLabel(deviceOwnerPackage, 0);
        }
    }

    ActiveAdmin getDeviceOwnerAdminLocked() {
        ComponentName component = this.mOwners.getDeviceOwnerComponent();
        if (component == null) {
            return null;
        }
        DevicePolicyData policy = getUserData(this.mOwners.getDeviceOwnerUserId());
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (component.equals(admin.info.getComponent())) {
                return admin;
            }
        }
        Slog.wtf(LOG_TAG, "Active admin for device owner not found. component=" + component);
        return null;
    }

    public void clearDeviceOwner(String packageName) {
        Preconditions.checkNotNull(packageName, "packageName is null");
        int callingUid = this.mInjector.binderGetCallingUid();
        try {
            int uid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, UserHandle.getUserId(callingUid));
            if (uid != callingUid) {
                throw new SecurityException("Invalid packageName");
            }
            synchronized (this) {
                ComponentName deviceOwnerComponent = this.mOwners.getDeviceOwnerComponent();
                int deviceOwnerUserId = this.mOwners.getDeviceOwnerUserId();
                if (!this.mOwners.hasDeviceOwner() || !deviceOwnerComponent.getPackageName().equals(packageName) || deviceOwnerUserId != UserHandle.getUserId(callingUid)) {
                    throw new SecurityException("clearDeviceOwner can only be called by the device owner");
                }
                enforceUserUnlocked(deviceOwnerUserId);
                ActiveAdmin admin = getDeviceOwnerAdminLocked();
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    clearDeviceOwnerLocked(admin, deviceOwnerUserId);
                    removeActiveAdminLocked(deviceOwnerComponent, deviceOwnerUserId);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException(e);
        }
    }

    private void clearDeviceOwnerLocked(ActiveAdmin admin, int userId) {
        if (admin != null) {
            admin.disableCamera = false;
            admin.userRestrictions = null;
            admin.forceEphemeralUsers = false;
            this.mUserManagerInternal.setForceEphemeralUsers(admin.forceEphemeralUsers);
        }
        clearUserPoliciesLocked(userId);
        this.mOwners.clearDeviceOwner();
        this.mOwners.writeDeviceOwner();
        updateDeviceOwnerLocked();
        disableSecurityLoggingIfNotCompliant();
        try {
            this.mInjector.getIBackupManager().setBackupServiceActive(0, true);
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed reactivating backup service.", e);
        }
    }

    public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) {
        if (!this.mHasFeature) {
            return false;
        }
        if (who == null || !isPackageInstalledForUser(who.getPackageName(), userHandle)) {
            throw new IllegalArgumentException("Component " + who + " not installed for userId:" + userHandle);
        }
        synchronized (this) {
            enforceCanSetProfileOwnerLocked(userHandle);
            if (getActiveAdminUncheckedLocked(who, userHandle) == null) {
                throw new IllegalArgumentException("Not active admin: " + who);
            }
            this.mOwners.setProfileOwner(who, ownerName, userHandle);
            this.mOwners.writeProfileOwner(userHandle);
        }
        return true;
    }

    public void clearProfileOwner(ComponentName who) {
        if (!this.mHasFeature) {
            return;
        }
        UserHandle callingUser = this.mInjector.binderGetCallingUserHandle();
        int userId = callingUser.getIdentifier();
        enforceNotManagedProfile(userId, "clear profile owner");
        enforceUserUnlocked(userId);
        ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
        synchronized (this) {
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                clearProfileOwnerLocked(admin, userId);
                removeActiveAdminLocked(who, userId);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public void clearProfileOwnerLocked(ActiveAdmin admin, int userId) {
        if (admin != null) {
            admin.disableCamera = false;
            admin.userRestrictions = null;
        }
        clearUserPoliciesLocked(userId);
        this.mOwners.removeProfileOwner(userId);
        this.mOwners.writeProfileOwner(userId);
    }

    public void setDeviceOwnerLockScreenInfo(ComponentName who, CharSequence info) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!this.mHasFeature) {
            return;
        }
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            long token = this.mInjector.binderClearCallingIdentity();
            try {
                this.mLockPatternUtils.setDeviceOwnerInfo(info != null ? info.toString() : null);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(token);
            }
        }
    }

    public CharSequence getDeviceOwnerLockScreenInfo() {
        return this.mLockPatternUtils.getDeviceOwnerInfo();
    }

    private void clearUserPoliciesLocked(int userId) {
        DevicePolicyData policy = getUserData(userId);
        policy.mPermissionPolicy = 0;
        policy.mDelegatedCertInstallerPackage = null;
        policy.mApplicationRestrictionsManagingPackage = null;
        policy.mStatusBarDisabled = false;
        policy.mUserProvisioningState = 0;
        saveSettingsLocked(userId);
        try {
            this.mIPackageManager.updatePermissionFlagsForAllApps(4, 0, userId);
            pushUserRestrictions(userId);
        } catch (RemoteException e) {
        }
    }

    public boolean hasUserSetupCompleted() {
        return hasUserSetupCompleted(UserHandle.getCallingUserId());
    }

    private boolean hasUserSetupCompleted(int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        return getUserData(userHandle).mUserSetupComplete;
    }

    public int getUserProvisioningState() {
        if (!this.mHasFeature) {
            return 0;
        }
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        return getUserProvisioningState(userHandle);
    }

    private int getUserProvisioningState(int userHandle) {
        return getUserData(userHandle).mUserProvisioningState;
    }

    public void setUserProvisioningState(int newState, int userHandle) {
        if (!this.mHasFeature) {
            return;
        }
        if (userHandle != this.mOwners.getDeviceOwnerUserId() && !this.mOwners.hasProfileOwner(userHandle) && getManagedUserId(userHandle) == -1) {
            throw new IllegalStateException("Not allowed to change provisioning state unless a device or profile owner is set.");
        }
        synchronized (this) {
            boolean transitionCheckNeeded = true;
            int callingUid = this.mInjector.binderGetCallingUid();
            if (callingUid == 2000 || callingUid == 0) {
                if (getUserProvisioningState(userHandle) != 0 || newState != 3) {
                    throw new IllegalStateException("Not allowed to change provisioning state unless current provisioning state is unmanaged, and new state is finalized.");
                }
                transitionCheckNeeded = false;
            } else {
                enforceCanManageProfileAndDeviceOwners();
            }
            DevicePolicyData policyData = getUserData(userHandle);
            if (transitionCheckNeeded) {
                checkUserProvisioningStateTransition(policyData.mUserProvisioningState, newState);
            }
            policyData.mUserProvisioningState = newState;
            saveSettingsLocked(userHandle);
        }
    }

    private void checkUserProvisioningStateTransition(int currentState, int newState) {
        switch (currentState) {
            case 0:
                if (newState != 0) {
                    return;
                }
                break;
            case 1:
            case 2:
                if (newState == 3) {
                    return;
                }
                break;
            case 4:
                if (newState == 0) {
                    return;
                }
                break;
        }
        throw new IllegalStateException("Cannot move to user provisioning state [" + newState + "] from state [" + currentState + "]");
    }

    public void setProfileEnabled(ComponentName who) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            enforceManagedProfile(userId, "enable the profile");
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mUserManager.setUserEnabled(userId);
                UserInfo parent = this.mUserManager.getProfileParent(userId);
                Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_ADDED");
                intent.putExtra("android.intent.extra.USER", new UserHandle(userId));
                intent.addFlags(1342177280);
                this.mContext.sendBroadcastAsUser(intent, new UserHandle(parent.id));
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public void setProfileName(ComponentName who, String profileName) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userId = UserHandle.getCallingUserId();
        getActiveAdminForCallerLocked(who, -1);
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManager.setUserName(userId, profileName);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public ComponentName getProfileOwner(int userHandle) {
        ComponentName profileOwnerComponent;
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (this) {
            profileOwnerComponent = this.mOwners.getProfileOwnerComponent(userHandle);
        }
        return profileOwnerComponent;
    }

    ActiveAdmin getProfileOwnerAdminLocked(int userHandle) {
        ComponentName profileOwner = this.mOwners.getProfileOwnerComponent(userHandle);
        if (profileOwner == null) {
            return null;
        }
        DevicePolicyData policy = getUserData(userHandle);
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (profileOwner.equals(admin.info.getComponent())) {
                return admin;
            }
        }
        return null;
    }

    public String getProfileOwnerName(int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceManageUsers();
        ComponentName profileOwner = getProfileOwner(userHandle);
        if (profileOwner == null) {
            return null;
        }
        return getApplicationLabel(profileOwner.getPackageName(), userHandle);
    }

    private String getApplicationLabel(String packageName, int userHandle) {
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            UserHandle handle = new UserHandle(userHandle);
            Context userContext = this.mContext.createPackageContextAsUser(packageName, 0, handle);
            ApplicationInfo appInfo = userContext.getApplicationInfo();
            CharSequence result = null;
            if (appInfo != null) {
                PackageManager pm = userContext.getPackageManager();
                result = pm.getApplicationLabel(appInfo);
            }
            return result != null ? result.toString() : null;
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.w(LOG_TAG, packageName + " is not installed for user " + userHandle, nnfe);
            return null;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    private void enforceCanSetProfileOwnerLocked(int userHandle) {
        UserInfo info = getUserInfo(userHandle);
        if (info == null) {
            throw new IllegalArgumentException("Attempted to set profile owner for invalid userId: " + userHandle);
        }
        if (info.isGuest()) {
            throw new IllegalStateException("Cannot set a profile owner on a guest");
        }
        if (this.mOwners.hasProfileOwner(userHandle)) {
            throw new IllegalStateException("Trying to set the profile owner, but profile owner is already set.");
        }
        if (this.mOwners.hasDeviceOwner() && this.mOwners.getDeviceOwnerUserId() == userHandle) {
            throw new IllegalStateException("Trying to set the profile owner, but the user already has a device owner.");
        }
        int callingUid = this.mInjector.binderGetCallingUid();
        if (callingUid == 2000 || callingUid == 0) {
            if (hasUserSetupCompleted(userHandle) && AccountManager.get(this.mContext).getAccountsAsUser(userHandle).length > 0) {
                throw new IllegalStateException("Not allowed to set the profile owner because there are already some accounts on the profile");
            }
        } else {
            enforceCanManageProfileAndDeviceOwners();
            if (!hasUserSetupCompleted(userHandle) || isCallerWithSystemUid()) {
            } else {
                throw new IllegalStateException("Cannot set the profile owner on a user which is already set-up");
            }
        }
    }

    private void enforceCanSetDeviceOwnerLocked(int userId) {
        boolean isAdb = true;
        int callingUid = this.mInjector.binderGetCallingUid();
        if (callingUid != 2000 && callingUid != 0) {
            isAdb = false;
        }
        if (!isAdb) {
            enforceCanManageProfileAndDeviceOwners();
        }
        int code = checkSetDeviceOwnerPreCondition(userId, isAdb);
        switch (code) {
            case 0:
                return;
            case 1:
                throw new IllegalStateException("Trying to set the device owner, but device owner is already set.");
            case 2:
                throw new IllegalStateException("Trying to set the device owner, but the user already has a profile owner.");
            case 3:
                throw new IllegalStateException("User not running: " + userId);
            case 4:
                throw new IllegalStateException("Cannot set the device owner if the device is already set-up");
            case 5:
                throw new IllegalStateException("Not allowed to set the device owner because there are already several users on the device");
            case 6:
                throw new IllegalStateException("Not allowed to set the device owner because there are already some accounts on the device");
            case 7:
                throw new IllegalStateException("User is not system user");
            default:
                throw new IllegalStateException("Unknown @DeviceOwnerPreConditionCode " + code);
        }
    }

    private void enforceUserUnlocked(int userId) {
        Preconditions.checkState(this.mUserManager.isUserUnlocked(userId), "User must be running and unlocked");
    }

    private void enforceUserUnlocked(int userId, boolean parent) {
        if (parent) {
            enforceUserUnlocked(getProfileParentId(userId));
        } else {
            enforceUserUnlocked(userId);
        }
    }

    private void enforceManageUsers() {
        boolean z = true;
        int callingUid = this.mInjector.binderGetCallingUid();
        if (!isCallerWithSystemUid() && callingUid != 0) {
            z = false;
        }
        if (z) {
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
    }

    private void enforceFullCrossUsersPermission(int userHandle) {
        enforceSystemUserOrPermission(userHandle, "android.permission.INTERACT_ACROSS_USERS_FULL");
    }

    private void enforceCrossUsersPermission(int userHandle) {
        enforceSystemUserOrPermission(userHandle, "android.permission.INTERACT_ACROSS_USERS");
    }

    private void enforceSystemUserOrPermission(int userHandle, String permission) {
        boolean z = true;
        if (userHandle < 0) {
            throw new IllegalArgumentException("Invalid userId " + userHandle);
        }
        int callingUid = this.mInjector.binderGetCallingUid();
        if (userHandle == UserHandle.getUserId(callingUid)) {
            return;
        }
        if (!isCallerWithSystemUid() && callingUid != 0) {
            z = false;
        }
        if (z) {
            return;
        }
        this.mContext.enforceCallingOrSelfPermission(permission, "Must be system or have " + permission + " permission");
    }

    private void enforceManagedProfile(int userHandle, String message) {
        if (isManagedProfile(userHandle)) {
        } else {
            throw new SecurityException("You can not " + message + " outside a managed profile.");
        }
    }

    private void enforceNotManagedProfile(int userHandle, String message) {
        if (!isManagedProfile(userHandle)) {
        } else {
            throw new SecurityException("You can not " + message + " for a managed profile.");
        }
    }

    private void ensureCallerPackage(String packageName) {
        if (packageName == null) {
            Preconditions.checkState(isCallerWithSystemUid(), "Only caller can omit package name");
            return;
        }
        int callingUid = this.mInjector.binderGetCallingUid();
        int userId = this.mInjector.userHandleGetCallingUserId();
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, 0, userId);
            Preconditions.checkState(ai.uid == callingUid, "Unmatching package name");
        } catch (RemoteException e) {
        }
    }

    private boolean isCallerWithSystemUid() {
        return UserHandle.isSameApp(this.mInjector.binderGetCallingUid(), 1000);
    }

    private int getProfileParentId(int userHandle) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo parentUser = this.mUserManager.getProfileParent(userHandle);
            if (parentUser != null) {
                userHandle = parentUser.id;
            }
            return userHandle;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private int getCredentialOwner(int userHandle, boolean parent) {
        long ident = this.mInjector.binderClearCallingIdentity();
        if (parent) {
            try {
                UserInfo parentProfile = this.mUserManager.getProfileParent(userHandle);
                if (parentProfile != null) {
                    userHandle = parentProfile.id;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        return this.mUserManager.getCredentialOwnerProfile(userHandle);
    }

    private boolean isManagedProfile(int userHandle) {
        return getUserInfo(userHandle).isManagedProfile();
    }

    private void enableIfNecessary(String packageName, int userId) {
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_VERSION, userId);
            if (ai.enabledSetting != 4) {
                return;
            }
            this.mIPackageManager.setApplicationEnabledSetting(packageName, 0, 1, userId, "DevicePolicyManager");
        } catch (RemoteException e) {
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump DevicePolicyManagerService from from pid=" + this.mInjector.binderGetCallingPid() + ", uid=" + this.mInjector.binderGetCallingUid());
            return;
        }
        synchronized (this) {
            pw.println("Current Device Policy Manager state:");
            this.mOwners.dump("  ", pw);
            int userCount = this.mUserData.size();
            for (int u = 0; u < userCount; u++) {
                DevicePolicyData policy = getUserData(this.mUserData.keyAt(u));
                pw.println();
                pw.println("  Enabled Device Admins (User " + policy.mUserHandle + ", provisioningState: " + policy.mUserProvisioningState + "):");
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin ap = policy.mAdminList.get(i);
                    if (ap != null) {
                        pw.print("    ");
                        pw.print(ap.info.getComponent().flattenToShortString());
                        pw.println(":");
                        ap.dump("      ", pw);
                    }
                }
                if (!policy.mRemovingAdmins.isEmpty()) {
                    pw.println("    Removing Device Admins (User " + policy.mUserHandle + "): " + policy.mRemovingAdmins);
                }
                pw.println(" ");
                pw.print("    mPasswordOwner=");
                pw.println(policy.mPasswordOwner);
            }
            pw.println();
            pw.println("Encryption Status: " + getEncryptionStatusName(getEncryptionStatus()));
        }
    }

    private String getEncryptionStatusName(int encryptionStatus) {
        switch (encryptionStatus) {
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
                return "unknown";
        }
    }

    public void addPersistentPreferredActivity(ComponentName who, IntentFilter filter, ComponentName activity) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.addPersistentPreferredActivity(filter, activity, userHandle);
            } catch (RemoteException e) {
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public void clearPackagePersistentPreferredActivities(ComponentName who, String packageName) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mIPackageManager.clearPackagePersistentPreferredActivities(packageName, userHandle);
            } catch (RemoteException e) {
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) {
        Preconditions.checkNotNull(admin, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
            if (packageName != null && !isPackageInstalledForUser(packageName, userHandle)) {
                return false;
            }
            DevicePolicyData policy = getUserData(userHandle);
            policy.mApplicationRestrictionsManagingPackage = packageName;
            saveSettingsLocked(userHandle);
            return true;
        }
    }

    public String getApplicationRestrictionsManagingPackage(ComponentName admin) {
        String str;
        Preconditions.checkNotNull(admin, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
            DevicePolicyData policy = getUserData(userHandle);
            str = policy.mApplicationRestrictionsManagingPackage;
        }
        return str;
    }

    public boolean isCallerApplicationRestrictionsManagingPackage() {
        int callingUid = this.mInjector.binderGetCallingUid();
        int userHandle = UserHandle.getUserId(callingUid);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mApplicationRestrictionsManagingPackage == null) {
                return false;
            }
            try {
                int uid = this.mContext.getPackageManager().getPackageUidAsUser(policy.mApplicationRestrictionsManagingPackage, userHandle);
                return uid == callingUid;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }

    private void enforceCanManageApplicationRestrictions(ComponentName who) {
        if (who != null) {
            synchronized (this) {
                getActiveAdminForCallerLocked(who, -1);
            }
        } else if (isCallerApplicationRestrictionsManagingPackage()) {
        } else {
            throw new SecurityException("No admin component given, and caller cannot manage application restrictions for other apps.");
        }
    }

    public void setApplicationRestrictions(ComponentName who, String packageName, Bundle settings) {
        enforceCanManageApplicationRestrictions(who);
        UserHandle userHandle = this.mInjector.binderGetCallingUserHandle();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            this.mUserManager.setApplicationRestrictions(packageName, settings, userHandle);
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(admin, "admin is null");
        Preconditions.checkNotNull(agent, "agent is null");
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(admin, 9, parent);
            ap.trustAgentInfos.put(agent.flattenToString(), new ActiveAdmin.TrustAgentInfo(args));
            saveSettingsLocked(userHandle);
        }
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userHandle, boolean parent) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(agent, "agent null");
        enforceFullCrossUsersPermission(userHandle);
        synchronized (this) {
            String componentName = agent.flattenToString();
            if (admin != null) {
                ActiveAdmin ap = getActiveAdminUncheckedLocked(admin, userHandle, parent);
                if (ap == null) {
                    return null;
                }
                ActiveAdmin.TrustAgentInfo trustAgentInfo = ap.trustAgentInfos.get(componentName);
                if (trustAgentInfo == null || trustAgentInfo.options == null) {
                    return null;
                }
                List<PersistableBundle> result = new ArrayList<>();
                result.add(trustAgentInfo.options);
                return result;
            }
            List<PersistableBundle> result2 = null;
            List<ActiveAdmin> admins = getActiveAdminsForLockscreenPoliciesLocked(userHandle, parent);
            boolean allAdminsHaveOptions = true;
            int N = admins.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                ActiveAdmin active = admins.get(i);
                boolean disablesTrust = (active.disabledKeyguardFeatures & 16) != 0;
                ActiveAdmin.TrustAgentInfo info = active.trustAgentInfos.get(componentName);
                if (info != null && info.options != null && !info.options.isEmpty()) {
                    if (disablesTrust) {
                        if (result2 == null) {
                            result2 = new ArrayList<>();
                        }
                        result2.add(info.options);
                    } else {
                        Log.w(LOG_TAG, "Ignoring admin " + active.info + " because it has trust options but doesn't declare KEYGUARD_DISABLE_TRUST_AGENTS");
                    }
                } else if (disablesTrust) {
                    allAdminsHaveOptions = false;
                    break;
                }
                i++;
            }
            if (!allAdminsHaveOptions) {
                result2 = null;
            }
            return result2;
        }
    }

    public void setRestrictionsProvider(ComponentName who, ComponentName permissionProvider) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            int userHandle = UserHandle.getCallingUserId();
            DevicePolicyData userData = getUserData(userHandle);
            userData.mRestrictionsProvider = permissionProvider;
            saveSettingsLocked(userHandle);
        }
    }

    public ComponentName getRestrictionsProvider(int userHandle) {
        ComponentName componentName;
        synchronized (this) {
            if (!isCallerWithSystemUid()) {
                throw new SecurityException("Only the system can query the permission provider");
            }
            DevicePolicyData userData = getUserData(userHandle);
            componentName = userData != null ? userData.mRestrictionsProvider : null;
        }
        return componentName;
    }

    public void addCrossProfileIntentFilter(ComponentName who, IntentFilter filter, int flags) {
        UserInfo parent;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    parent = this.mUserManager.getProfileParent(callingUserId);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            } catch (RemoteException e) {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
            if (parent == null) {
                Slog.e(LOG_TAG, "Cannot call addCrossProfileIntentFilter if there is no parent");
                return;
            }
            if ((flags & 1) != 0) {
                this.mIPackageManager.addCrossProfileIntentFilter(filter, who.getPackageName(), callingUserId, parent.id, 0);
            }
            if ((flags & 2) != 0) {
                this.mIPackageManager.addCrossProfileIntentFilter(filter, who.getPackageName(), parent.id, callingUserId, 0);
            }
        }
    }

    public void clearCrossProfileIntentFilters(ComponentName who) {
        UserInfo parent;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                parent = this.mUserManager.getProfileParent(callingUserId);
            } catch (RemoteException e) {
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
            if (parent == null) {
                Slog.e(LOG_TAG, "Cannot call clearCrossProfileIntentFilter if there is no parent");
            } else {
                this.mIPackageManager.clearCrossProfileIntentFilters(callingUserId, who.getPackageName());
                this.mIPackageManager.clearCrossProfileIntentFilters(parent.id, who.getPackageName());
            }
        }
    }

    private boolean checkPackagesInPermittedListOrSystem(List<String> enabledPackages, List<String> permittedList, int userIdToCheck) {
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo user = getUserInfo(userIdToCheck);
            if (user.isManagedProfile()) {
                userIdToCheck = user.profileGroupId;
            }
            for (String enabledPackage : enabledPackages) {
                boolean systemService = false;
                try {
                    ApplicationInfo applicationInfo = this.mIPackageManager.getApplicationInfo(enabledPackage, PackageManagerService.DumpState.DUMP_PREFERRED_XML, userIdToCheck);
                    systemService = (applicationInfo.flags & 1) != 0;
                } catch (RemoteException e) {
                    Log.i(LOG_TAG, "Can't talk to package managed", e);
                }
                if (!systemService && !permittedList.contains(enabledPackage)) {
                    return false;
                }
            }
            this.mInjector.binderRestoreCallingIdentity(id);
            return true;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    private AccessibilityManager getAccessibilityManagerForUser(int userId) {
        IBinder iBinder = ServiceManager.getService("accessibility");
        return new AccessibilityManager(this.mContext, iBinder == null ? null : IAccessibilityManager.Stub.asInterface(iBinder), userId);
    }

    public boolean setPermittedAccessibilityServices(ComponentName who, List packageList) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (packageList != null) {
            int userId = UserHandle.getCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                UserInfo user = getUserInfo(userId);
                if (user.isManagedProfile()) {
                    userId = user.profileGroupId;
                }
                AccessibilityManager accessibilityManager = getAccessibilityManagerForUser(userId);
                List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(-1);
                if (enabledServices != null) {
                    List<String> enabledPackages = new ArrayList<>();
                    for (AccessibilityServiceInfo service : enabledServices) {
                        enabledPackages.add(service.getResolveInfo().serviceInfo.packageName);
                    }
                    if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList, userId)) {
                        Slog.e(LOG_TAG, "Cannot set permitted accessibility services, because it contains already enabled accesibility services.");
                        return false;
                    }
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            admin.permittedAccessiblityServices = packageList;
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
        return true;
    }

    public List getPermittedAccessibilityServices(ComponentName who) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            list = admin.permittedAccessiblityServices;
        }
        return list;
    }

    public List getPermittedAccessibilityServicesForUser(int userId) throws Throwable {
        List<String> result;
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (this) {
            List<String> result2 = null;
            try {
                int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userId);
                int i = 0;
                int length = profileIds.length;
                while (i < length) {
                    int profileId = profileIds[i];
                    DevicePolicyData policy = getUserDataUnchecked(profileId);
                    int N = policy.mAdminList.size();
                    int j = 0;
                    List<String> result3 = result2;
                    while (j < N) {
                        try {
                            ActiveAdmin admin = policy.mAdminList.get(j);
                            List<String> fromAdmin = admin.permittedAccessiblityServices;
                            if (fromAdmin == null) {
                                result = result3;
                            } else if (result3 == null) {
                                result = new ArrayList<>(fromAdmin);
                            } else {
                                result3.retainAll(fromAdmin);
                                result = result3;
                            }
                            j++;
                            result3 = result;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    i++;
                    result2 = result3;
                }
                if (result2 != null) {
                    long id = this.mInjector.binderClearCallingIdentity();
                    try {
                        UserInfo user = getUserInfo(userId);
                        if (user.isManagedProfile()) {
                            userId = user.profileGroupId;
                        }
                        AccessibilityManager accessibilityManager = getAccessibilityManagerForUser(userId);
                        List<AccessibilityServiceInfo> installedServices = accessibilityManager.getInstalledAccessibilityServiceList();
                        if (installedServices != null) {
                            for (AccessibilityServiceInfo service : installedServices) {
                                ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                                ApplicationInfo applicationInfo = serviceInfo.applicationInfo;
                                if ((applicationInfo.flags & 1) != 0) {
                                    result2.add(serviceInfo.packageName);
                                }
                            }
                        }
                    } finally {
                        this.mInjector.binderRestoreCallingIdentity(id);
                    }
                }
                return result2;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public boolean isAccessibilityServicePermittedByAdmin(ComponentName who, String packageName, int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(packageName, "packageName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query if an accessibility service is disabled by admin");
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            if (admin == null) {
                return false;
            }
            if (admin.permittedAccessiblityServices == null) {
                return true;
            }
            return checkPackagesInPermittedListOrSystem(Arrays.asList(packageName), admin.permittedAccessiblityServices, userHandle);
        }
    }

    private boolean checkCallerIsCurrentUserOrProfile() {
        int callingUserId = UserHandle.getCallingUserId();
        long token = this.mInjector.binderClearCallingIdentity();
        try {
            UserInfo callingUser = getUserInfo(callingUserId);
            UserInfo currentUser = this.mInjector.getIActivityManager().getCurrentUser();
            if (callingUser.isManagedProfile() && callingUser.profileGroupId != currentUser.id) {
                Slog.e(LOG_TAG, "Cannot set permitted input methods for managed profile of a user that isn't the foreground user.");
                return false;
            }
            if (callingUser.isManagedProfile() || callingUserId == currentUser.id) {
                this.mInjector.binderRestoreCallingIdentity(token);
                return true;
            }
            Slog.e(LOG_TAG, "Cannot set permitted input methods of a user that isn't the foreground user.");
            return false;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to talk to activity managed.", e);
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(token);
        }
    }

    public boolean setPermittedInputMethods(ComponentName who, List packageList) {
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!checkCallerIsCurrentUserOrProfile()) {
            return false;
        }
        if (packageList != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService(InputMethodManager.class);
            List<InputMethodInfo> enabledImes = inputMethodManager.getEnabledInputMethodList();
            if (enabledImes != null) {
                List<String> enabledPackages = new ArrayList<>();
                for (InputMethodInfo ime : enabledImes) {
                    enabledPackages.add(ime.getPackageName());
                }
                if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList, this.mInjector.binderGetCallingUserHandle().getIdentifier())) {
                    Slog.e(LOG_TAG, "Cannot set permitted input methods, because it contains already enabled input method.");
                    return false;
                }
            }
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            admin.permittedInputMethods = packageList;
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
        return true;
    }

    public List getPermittedInputMethods(ComponentName who) {
        List<String> list;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            list = admin.permittedInputMethods;
        }
        return list;
    }

    public List getPermittedInputMethodsForCurrentUser() throws Throwable {
        List<String> result;
        try {
            UserInfo currentUser = this.mInjector.getIActivityManager().getCurrentUser();
            int userId = currentUser.id;
            synchronized (this) {
                List<String> result2 = null;
                try {
                    int[] profileIds = this.mUserManager.getProfileIdsWithDisabled(userId);
                    int i = 0;
                    int length = profileIds.length;
                    while (i < length) {
                        int profileId = profileIds[i];
                        DevicePolicyData policy = getUserDataUnchecked(profileId);
                        int N = policy.mAdminList.size();
                        int j = 0;
                        List<String> result3 = result2;
                        while (j < N) {
                            try {
                                ActiveAdmin admin = policy.mAdminList.get(j);
                                List<String> fromAdmin = admin.permittedInputMethods;
                                if (fromAdmin == null) {
                                    result = result3;
                                } else if (result3 == null) {
                                    result = new ArrayList<>(fromAdmin);
                                } else {
                                    result3.retainAll(fromAdmin);
                                    result = result3;
                                }
                                j++;
                                result3 = result;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        i++;
                        result2 = result3;
                    }
                    if (result2 != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService(InputMethodManager.class);
                        List<InputMethodInfo> imes = inputMethodManager.getInputMethodList();
                        long id = this.mInjector.binderClearCallingIdentity();
                        if (imes != null) {
                            try {
                                for (InputMethodInfo ime : imes) {
                                    ServiceInfo serviceInfo = ime.getServiceInfo();
                                    ApplicationInfo applicationInfo = serviceInfo.applicationInfo;
                                    if ((applicationInfo.flags & 1) != 0) {
                                        result2.add(serviceInfo.packageName);
                                    }
                                }
                            } finally {
                                this.mInjector.binderRestoreCallingIdentity(id);
                            }
                        }
                    }
                    return result2;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to make remote calls to get current user", e);
            return null;
        }
    }

    public boolean isInputMethodPermittedByAdmin(ComponentName who, String packageName, int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        Preconditions.checkStringNotEmpty(packageName, "packageName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query if an input method is disabled by admin");
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            if (admin == null) {
                return false;
            }
            if (admin.permittedInputMethods == null) {
                return true;
            }
            return checkPackagesInPermittedListOrSystem(Arrays.asList(packageName), admin.permittedInputMethods, userHandle);
        }
    }

    private void sendAdminEnabledBroadcastLocked(int userHandle) {
        DevicePolicyData policyData = getUserData(userHandle);
        if (!policyData.mAdminBroadcastPending) {
            return;
        }
        ActiveAdmin admin = getProfileOwnerAdminLocked(userHandle);
        if (admin != null) {
            PersistableBundle initBundle = policyData.mInitBundle;
            sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_ENABLED", initBundle == null ? null : new Bundle(initBundle), null);
        }
        policyData.mInitBundle = null;
        policyData.mAdminBroadcastPending = false;
        saveSettingsLocked(userHandle);
    }

    public UserHandle createAndManageUser(ComponentName admin, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) {
        long id;
        Preconditions.checkNotNull(admin, "admin is null");
        Preconditions.checkNotNull(profileOwner, "profileOwner is null");
        if (!admin.getPackageName().equals(profileOwner.getPackageName())) {
            throw new IllegalArgumentException("profileOwner " + profileOwner + " and admin " + admin + " are not in the same package");
        }
        if (!this.mInjector.binderGetCallingUserHandle().isSystem()) {
            throw new SecurityException("createAndManageUser was called from non-system user");
        }
        if (!this.mInjector.userManagerIsSplitSystemUser() && (flags & 2) != 0) {
            throw new IllegalArgumentException("Ephemeral users are only supported on systems with a split system user.");
        }
        UserHandle user = null;
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -2);
            id = this.mInjector.binderClearCallingIdentity();
            int userInfoFlags = 0;
            if ((flags & 2) != 0) {
                userInfoFlags = 256;
            }
            try {
                UserInfo userInfo = this.mUserManagerInternal.createUserEvenWhenDisallowed(name, userInfoFlags);
                if (userInfo != null) {
                    user = userInfo.getUserHandle();
                }
            } finally {
            }
        }
        if (user == null) {
            return null;
        }
        id = this.mInjector.binderClearCallingIdentity();
        try {
            String adminPkg = admin.getPackageName();
            int userHandle = user.getIdentifier();
            if (!this.mIPackageManager.isPackageAvailable(adminPkg, userHandle)) {
                this.mIPackageManager.installExistingPackageAsUser(adminPkg, userHandle);
            }
            setActiveAdmin(profileOwner, true, userHandle);
            synchronized (this) {
                DevicePolicyData policyData = getUserData(userHandle);
                policyData.mInitBundle = adminExtras;
                policyData.mAdminBroadcastPending = true;
                saveSettingsLocked(userHandle);
            }
            String ownerName = getProfileOwnerName(Process.myUserHandle().getIdentifier());
            setProfileOwner(profileOwner, ownerName, userHandle);
            if ((flags & 1) != 0) {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 1, userHandle);
            }
            return user;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to make remote calls for createAndManageUser, removing created user", e);
            this.mUserManager.removeUser(user.getIdentifier());
            return null;
        } finally {
        }
    }

    public boolean removeUser(ComponentName who, UserHandle userHandle) {
        boolean zRemoveUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                zRemoveUser = this.mUserManager.removeUser(userHandle.getIdentifier());
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return zRemoveUser;
    }

    public boolean switchUser(ComponentName who, UserHandle userHandle) {
        boolean zSwitchUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            long id = this.mInjector.binderClearCallingIdentity();
            int userId = 0;
            if (userHandle != null) {
                try {
                    try {
                        userId = userHandle.getIdentifier();
                        zSwitchUser = this.mInjector.getIActivityManager().switchUser(userId);
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, "Couldn't switch user", e);
                        return false;
                    }
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            } else {
                zSwitchUser = this.mInjector.getIActivityManager().switchUser(userId);
            }
        }
        return zSwitchUser;
    }

    public Bundle getApplicationRestrictions(ComponentName who, String packageName) {
        enforceCanManageApplicationRestrictions(who);
        UserHandle userHandle = this.mInjector.binderGetCallingUserHandle();
        long id = this.mInjector.binderClearCallingIdentity();
        try {
            Bundle bundle = this.mUserManager.getApplicationRestrictions(packageName, userHandle);
            if (bundle == null) {
                bundle = Bundle.EMPTY;
            }
            return bundle;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public String[] setPackagesSuspended(ComponentName who, String[] packageNames, boolean suspended) {
        String[] packagesSuspendedAsUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                packagesSuspendedAsUser = this.mIPackageManager.setPackagesSuspendedAsUser(packageNames, suspended, callingUserId);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Failed talking to the package manager", re);
                return packageNames;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return packagesSuspendedAsUser;
    }

    public boolean isPackageSuspended(ComponentName who, String packageName) {
        boolean zIsPackageSuspendedForUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                zIsPackageSuspendedForUser = this.mIPackageManager.isPackageSuspendedForUser(packageName, callingUserId);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Failed talking to the package manager", re);
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return zIsPackageSuspendedForUser;
    }

    public void setUserRestriction(ComponentName who, String key, boolean enabledFromThisOwner) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!UserRestrictionsUtils.isValidRestriction(key)) {
            return;
        }
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(who, -1);
            boolean isDeviceOwner = isDeviceOwner(who, userHandle);
            if (isDeviceOwner) {
                if (!UserRestrictionsUtils.canDeviceOwnerChange(key)) {
                    throw new SecurityException("Device owner cannot set user restriction " + key);
                }
            } else if (!UserRestrictionsUtils.canProfileOwnerChange(key, userHandle)) {
                throw new SecurityException("Profile owner cannot set user restriction " + key);
            }
            activeAdmin.ensureUserRestrictions().putBoolean(key, enabledFromThisOwner);
            saveSettingsLocked(userHandle);
            pushUserRestrictions(userHandle);
            sendChangedNotification(userHandle);
        }
    }

    private void pushUserRestrictions(int userId) {
        Bundle global;
        synchronized (this) {
            Bundle local = new Bundle();
            if (this.mOwners.isDeviceOwnerUserId(userId)) {
                global = new Bundle();
                ActiveAdmin deviceOwner = getDeviceOwnerAdminLocked();
                if (deviceOwner == null) {
                    return;
                }
                UserRestrictionsUtils.sortToGlobalAndLocal(deviceOwner.userRestrictions, global, local);
                if (deviceOwner.disableCamera) {
                    global.putBoolean("no_camera", true);
                }
            } else {
                global = null;
                ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userId);
                if (profileOwner != null) {
                    UserRestrictionsUtils.merge(local, profileOwner.userRestrictions);
                }
            }
            if (getCameraDisabled(null, userId, false)) {
                local.putBoolean("no_camera", true);
            }
            this.mUserManagerInternal.setDevicePolicyUserRestrictions(userId, local, global);
        }
    }

    public Bundle getUserRestrictions(ComponentName who) {
        Bundle bundle;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(who, -1);
            bundle = activeAdmin.userRestrictions;
        }
        return bundle;
    }

    public boolean setApplicationHidden(ComponentName who, String packageName, boolean hidden) {
        boolean applicationHiddenSettingAsUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                applicationHiddenSettingAsUser = this.mIPackageManager.setApplicationHiddenSettingAsUser(packageName, hidden, callingUserId);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Failed to setApplicationHiddenSetting", re);
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public boolean isApplicationHidden(ComponentName who, String packageName) {
        boolean applicationHiddenSettingAsUser;
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                applicationHiddenSettingAsUser = this.mIPackageManager.getApplicationHiddenSettingAsUser(packageName, callingUserId);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Failed to getApplicationHiddenSettingAsUser", re);
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public void enableSystemApp(ComponentName who, String packageName) {
        int parentUserId;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    parentUserId = getProfileParentId(userId);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            } catch (RemoteException re) {
                Slog.wtf(LOG_TAG, "Failed to install " + packageName, re);
            }
            if (!isSystemApp(this.mIPackageManager, packageName, parentUserId)) {
                throw new IllegalArgumentException("Only system apps can be enabled this way.");
            }
            this.mIPackageManager.installExistingPackageAsUser(packageName, userId);
            this.mInjector.binderRestoreCallingIdentity(id);
        }
    }

    public int enableSystemAppWithIntent(ComponentName who, Intent intent) {
        int numberOfAppsInstalled;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                int parentUserId = getProfileParentId(userId);
                List<ResolveInfo> activitiesToEnable = this.mIPackageManager.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, parentUserId).getList();
                numberOfAppsInstalled = 0;
                if (activitiesToEnable != null) {
                    for (ResolveInfo info : activitiesToEnable) {
                        if (info.activityInfo != null) {
                            String packageName = info.activityInfo.packageName;
                            if (isSystemApp(this.mIPackageManager, packageName, parentUserId)) {
                                numberOfAppsInstalled++;
                                this.mIPackageManager.installExistingPackageAsUser(packageName, userId);
                            } else {
                                Slog.d(LOG_TAG, "Not enabling " + packageName + " since is not a system app");
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.wtf(LOG_TAG, "Failed to resolve intent for: " + intent);
                return 0;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
        return numberOfAppsInstalled;
    }

    private boolean isSystemApp(IPackageManager pm, String packageName, int userId) throws RemoteException {
        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_PREFERRED_XML, userId);
        if (appInfo == null) {
            throw new IllegalArgumentException("The application " + packageName + " is not present on this device");
        }
        return (appInfo.flags & 1) != 0;
    }

    public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1);
            if (disabled) {
                ap.accountTypesWithManagementDisabled.add(accountType);
            } else {
                ap.accountTypesWithManagementDisabled.remove(accountType);
            }
            saveSettingsLocked(UserHandle.getCallingUserId());
        }
    }

    public String[] getAccountTypesWithManagementDisabled() {
        return getAccountTypesWithManagementDisabledAsUser(UserHandle.getCallingUserId());
    }

    public String[] getAccountTypesWithManagementDisabledAsUser(int userId) {
        String[] strArr;
        enforceFullCrossUsersPermission(userId);
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (this) {
            DevicePolicyData policy = getUserData(userId);
            int N = policy.mAdminList.size();
            ArraySet<String> resultSet = new ArraySet<>();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin = policy.mAdminList.get(i);
                resultSet.addAll(admin.accountTypesWithManagementDisabled);
            }
            strArr = (String[]) resultSet.toArray(new String[resultSet.size()]);
        }
        return strArr;
    }

    public void setUninstallBlocked(ComponentName who, String packageName, boolean uninstallBlocked) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    this.mIPackageManager.setBlockUninstallForUser(packageName, uninstallBlocked, userId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to setBlockUninstallForUser", re);
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public boolean isUninstallBlocked(ComponentName who, String packageName) {
        boolean blockUninstallForUser;
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who != null) {
                getActiveAdminForCallerLocked(who, -1);
                long id = this.mInjector.binderClearCallingIdentity();
                try {
                    blockUninstallForUser = this.mIPackageManager.getBlockUninstallForUser(packageName, userId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to getBlockUninstallForUser", re);
                    return false;
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(id);
                }
            } else {
                long id2 = this.mInjector.binderClearCallingIdentity();
                blockUninstallForUser = this.mIPackageManager.getBlockUninstallForUser(packageName, userId);
            }
        }
        return blockUninstallForUser;
    }

    public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            if (admin.disableCallerId != disabled) {
                admin.disableCallerId = disabled;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public boolean getCrossProfileCallerIdDisabled(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            z = admin.disableCallerId;
        }
        return z;
    }

    public boolean getCrossProfileCallerIdDisabledForUser(int userId) {
        boolean z;
        enforceCrossUsersPermission(userId);
        synchronized (this) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableCallerId : false;
        }
        return z;
    }

    public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            if (admin.disableContactsSearch != disabled) {
                admin.disableContactsSearch = disabled;
                saveSettingsLocked(this.mInjector.userHandleGetCallingUserId());
            }
        }
    }

    public boolean getCrossProfileContactsSearchDisabled(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            z = admin.disableContactsSearch;
        }
        return z;
    }

    public boolean getCrossProfileContactsSearchDisabledForUser(int userId) {
        boolean z;
        enforceCrossUsersPermission(userId);
        synchronized (this) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableContactsSearch : false;
        }
        return z;
    }

    public void startManagedQuickContact(String actualLookupKey, long actualContactId, boolean isContactIdIgnored, long actualDirectoryId, Intent originalIntent) {
        Intent intent = ContactsContract.QuickContact.rebuildManagedQuickContactsIntent(actualLookupKey, actualContactId, isContactIdIgnored, actualDirectoryId, originalIntent);
        int callingUserId = UserHandle.getCallingUserId();
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            synchronized (this) {
                int managedUserId = getManagedUserId(callingUserId);
                if (managedUserId < 0) {
                    return;
                }
                if (isCrossProfileQuickContactDisabled(managedUserId)) {
                    return;
                }
                ContactsInternal.startQuickContactWithErrorToastForUser(this.mContext, intent, new UserHandle(managedUserId));
            }
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private boolean isCrossProfileQuickContactDisabled(int userId) {
        if (getCrossProfileCallerIdDisabledForUser(userId)) {
            return getCrossProfileContactsSearchDisabledForUser(userId);
        }
        return false;
    }

    public int getManagedUserId(int callingUserId) {
        for (UserInfo ui : this.mUserManager.getProfiles(callingUserId)) {
            if (ui.id != callingUserId && ui.isManagedProfile()) {
                return ui.id;
            }
        }
        return -1;
    }

    public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            if (admin.disableBluetoothContactSharing != disabled) {
                admin.disableBluetoothContactSharing = disabled;
                saveSettingsLocked(UserHandle.getCallingUserId());
            }
        }
    }

    public boolean getBluetoothContactSharingDisabled(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return false;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            z = admin.disableBluetoothContactSharing;
        }
        return z;
    }

    public boolean getBluetoothContactSharingDisabledForUser(int userId) {
        boolean z;
        synchronized (this) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            z = admin != null ? admin.disableBluetoothContactSharing : false;
        }
        return z;
    }

    public void setLockTaskPackages(ComponentName who, String[] packages) throws SecurityException {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin deviceOwner = getActiveAdminWithPolicyForUidLocked(who, -2, this.mInjector.binderGetCallingUid());
            ActiveAdmin profileOwner = getActiveAdminWithPolicyForUidLocked(who, -1, this.mInjector.binderGetCallingUid());
            if (deviceOwner != null || (profileOwner != null && isAffiliatedUser())) {
                int userHandle = this.mInjector.userHandleGetCallingUserId();
                setLockTaskPackagesLocked(userHandle, new ArrayList(Arrays.asList(packages)));
            } else {
                throw new SecurityException("Admin " + who + " is neither the device owner or affiliated user's profile owner.");
            }
        }
    }

    private void setLockTaskPackagesLocked(int userHandle, List<String> packages) {
        DevicePolicyData policy = getUserData(userHandle);
        policy.mLockTaskPackages = packages;
        saveSettingsLocked(userHandle);
        updateLockTaskPackagesLocked(packages, userHandle);
    }

    public String[] getLockTaskPackages(ComponentName who) {
        String[] strArr;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            int userHandle = this.mInjector.binderGetCallingUserHandle().getIdentifier();
            List<String> packages = getLockTaskPackagesLocked(userHandle);
            strArr = (String[]) packages.toArray(new String[packages.size()]);
        }
        return strArr;
    }

    private List<String> getLockTaskPackagesLocked(int userHandle) {
        DevicePolicyData policy = getUserData(userHandle);
        return policy.mLockTaskPackages;
    }

    public boolean isLockTaskPermitted(String pkg) {
        int uid = this.mInjector.binderGetCallingUid();
        int userHandle = UserHandle.getUserId(uid);
        DevicePolicyData policy = getUserData(userHandle);
        synchronized (this) {
            for (int i = 0; i < policy.mLockTaskPackages.size(); i++) {
                String lockTaskPackage = policy.mLockTaskPackages.get(i);
                if (lockTaskPackage.equals(pkg)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userHandle) {
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("notifyLockTaskModeChanged can only be called by system");
        }
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            Bundle adminExtras = new Bundle();
            adminExtras.putString("android.app.extra.LOCK_TASK_PACKAGE", pkg);
            for (ActiveAdmin admin : policy.mAdminList) {
                boolean ownsDevice = isDeviceOwner(admin.info.getComponent(), userHandle);
                boolean ownsProfile = isProfileOwner(admin.info.getComponent(), userHandle);
                if (ownsDevice || ownsProfile) {
                    if (isEnabled) {
                        sendAdminCommandLocked(admin, "android.app.action.LOCK_TASK_ENTERING", adminExtras, null);
                    } else {
                        sendAdminCommandLocked(admin, "android.app.action.LOCK_TASK_EXITING");
                    }
                }
            }
        }
    }

    public void setGlobalSetting(ComponentName who, String setting, String value) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            if (GLOBAL_SETTINGS_DEPRECATED.contains(setting)) {
                Log.i(LOG_TAG, "Global setting no longer supported: " + setting);
                return;
            }
            if (!GLOBAL_SETTINGS_WHITELIST.contains(setting)) {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", setting));
            }
            if ("stay_on_while_plugged_in".equals(setting)) {
                long timeMs = getMaximumTimeToLock(who, this.mInjector.userHandleGetCallingUserId(), false);
                if (timeMs > 0 && timeMs < 2147483647L) {
                    return;
                }
            }
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mInjector.settingsGlobalPutString(setting, value);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public void setSecureSetting(ComponentName who, String setting, String value) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            if (isDeviceOwner(who, callingUserId)) {
                if (!SECURE_SETTINGS_DEVICEOWNER_WHITELIST.contains(setting)) {
                    throw new SecurityException(String.format("Permission denial: Device owners cannot update %1$s", setting));
                }
            } else if (!SECURE_SETTINGS_WHITELIST.contains(setting)) {
                throw new SecurityException(String.format("Permission denial: Profile owners cannot update %1$s", setting));
            }
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mInjector.settingsSecurePutStringForUser(setting, value, callingUserId);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public void setMasterVolumeMuted(ComponentName who, boolean on) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long identity = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    IAudioService iAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
                    iAudioService.setMasterMute(on, 0, this.mContext.getPackageName(), userId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to setMasterMute", re);
                    this.mInjector.binderRestoreCallingIdentity(identity);
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(identity);
            }
        }
    }

    public boolean isMasterVolumeMuted(ComponentName who) {
        boolean zIsMasterMute;
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
            zIsMasterMute = audioManager.isMasterMute();
        }
        return zIsMasterMute;
    }

    public void setUserIcon(ComponentName who, Bitmap icon) {
        synchronized (this) {
            Preconditions.checkNotNull(who, "ComponentName is null");
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long id = this.mInjector.binderClearCallingIdentity();
            try {
                this.mUserManagerInternal.setUserIcon(userId, icon);
            } finally {
                this.mInjector.binderRestoreCallingIdentity(id);
            }
        }
    }

    public boolean setKeyguardDisabled(ComponentName who, boolean disabled) {
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
        }
        int userId = UserHandle.getCallingUserId();
        long ident = this.mInjector.binderClearCallingIdentity();
        if (disabled) {
            try {
                if (this.mLockPatternUtils.isSecure(userId)) {
                    return false;
                }
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
        this.mLockPatternUtils.setLockScreenDisabled(disabled, userId);
        this.mInjector.binderRestoreCallingIdentity(ident);
        return true;
    }

    public boolean setStatusBarDisabled(ComponentName who, boolean disabled) {
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            DevicePolicyData policy = getUserData(userId);
            if (policy.mStatusBarDisabled != disabled) {
                if (!setStatusBarDisabledInternal(disabled, userId)) {
                    return false;
                }
                policy.mStatusBarDisabled = disabled;
                saveSettingsLocked(userId);
            }
            return true;
        }
    }

    private boolean setStatusBarDisabledInternal(boolean disabled, int userId) {
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            IStatusBarService statusBarService = IStatusBarService.Stub.asInterface(ServiceManager.checkService(TAG_STATUS_BAR));
            if (statusBarService == null) {
                return false;
            }
            int flags1 = disabled ? STATUS_BAR_DISABLE_MASK : 0;
            int flags2 = disabled ? 1 : 0;
            statusBarService.disableForUser(flags1, this.mToken, this.mContext.getPackageName(), userId);
            statusBarService.disable2ForUser(flags2, this.mToken, this.mContext.getPackageName(), userId);
            return true;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to disable the status bar", e);
            return false;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    void updateUserSetupComplete() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        int N = users.size();
        for (int i = 0; i < N; i++) {
            int userHandle = users.get(i).id;
            if (this.mInjector.settingsSecureGetIntForUser("user_setup_complete", 0, userHandle) != 0) {
                DevicePolicyData policy = getUserData(userHandle);
                if (policy.mUserSetupComplete) {
                    continue;
                } else {
                    policy.mUserSetupComplete = true;
                    synchronized (this) {
                        saveSettingsLocked(userHandle);
                    }
                }
            }
        }
    }

    private class SetupContentObserver extends ContentObserver {
        private final Uri mDeviceProvisioned;
        private final Uri mUserSetupComplete;

        public SetupContentObserver(Handler handler) {
            super(handler);
            this.mUserSetupComplete = Settings.Secure.getUriFor("user_setup_complete");
            this.mDeviceProvisioned = Settings.Global.getUriFor("device_provisioned");
        }

        void register() {
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mUserSetupComplete, false, this, -1);
            DevicePolicyManagerService.this.mInjector.registerContentObserver(this.mDeviceProvisioned, false, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (this.mUserSetupComplete.equals(uri)) {
                DevicePolicyManagerService.this.updateUserSetupComplete();
            } else {
                if (!this.mDeviceProvisioned.equals(uri)) {
                    return;
                }
                synchronized (DevicePolicyManagerService.this) {
                    DevicePolicyManagerService.this.setDeviceOwnerSystemPropertyLocked();
                }
            }
        }
    }

    final class LocalService extends DevicePolicyManagerInternal {
        private List<DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener> mWidgetProviderListeners;

        LocalService() {
        }

        public List<String> getCrossProfileWidgetProviders(int profileId) {
            synchronized (DevicePolicyManagerService.this) {
                if (DevicePolicyManagerService.this.mOwners == null) {
                    return Collections.emptyList();
                }
                ComponentName ownerComponent = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(profileId);
                if (ownerComponent == null) {
                    return Collections.emptyList();
                }
                DevicePolicyData policy = DevicePolicyManagerService.this.getUserDataUnchecked(profileId);
                ActiveAdmin admin = policy.mAdminMap.get(ownerComponent);
                if (admin == null || admin.crossProfileWidgetProviders == null || admin.crossProfileWidgetProviders.isEmpty()) {
                    return Collections.emptyList();
                }
                return admin.crossProfileWidgetProviders;
            }
        }

        public void addOnCrossProfileWidgetProvidersChangeListener(DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener listener) {
            synchronized (DevicePolicyManagerService.this) {
                if (this.mWidgetProviderListeners == null) {
                    this.mWidgetProviderListeners = new ArrayList();
                }
                if (!this.mWidgetProviderListeners.contains(listener)) {
                    this.mWidgetProviderListeners.add(listener);
                }
            }
        }

        public boolean isActiveAdminWithPolicy(int uid, int reqPolicy) {
            boolean z;
            synchronized (DevicePolicyManagerService.this) {
                z = DevicePolicyManagerService.this.getActiveAdminWithPolicyForUidLocked(null, reqPolicy, uid) != null;
            }
            return z;
        }

        private void notifyCrossProfileProvidersChanged(int userId, List<String> packages) {
            List<DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener> listeners;
            synchronized (DevicePolicyManagerService.this) {
                listeners = new ArrayList<>(this.mWidgetProviderListeners);
            }
            int listenerCount = listeners.size();
            for (int i = 0; i < listenerCount; i++) {
                DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener listener = listeners.get(i);
                listener.onCrossProfileWidgetProvidersChanged(userId, packages);
            }
        }

        public Intent createPackageSuspendedDialogIntent(String packageName, int userId) {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
            intent.putExtra("android.intent.extra.USER_ID", userId);
            intent.setFlags(268435456);
            ComponentName profileOwner = DevicePolicyManagerService.this.mOwners.getProfileOwnerComponent(userId);
            if (profileOwner != null) {
                intent.putExtra("android.app.extra.DEVICE_ADMIN", profileOwner);
                return intent;
            }
            Pair<Integer, ComponentName> deviceOwner = DevicePolicyManagerService.this.mOwners.getDeviceOwnerUserIdAndComponent();
            if (deviceOwner != null && ((Integer) deviceOwner.first).intValue() == userId) {
                intent.putExtra("android.app.extra.DEVICE_ADMIN", (Parcelable) deviceOwner.second);
                return intent;
            }
            return intent;
        }
    }

    private static boolean isLimitPasswordAllowed(ActiveAdmin admin, int minPasswordQuality) {
        if (admin.passwordQuality < minPasswordQuality) {
            return false;
        }
        return admin.info.usesPolicy(0);
    }

    public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) {
        if (policy != null && !policy.isValid()) {
            throw new IllegalArgumentException("Invalid system update policy.");
        }
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
            if (policy == null) {
                this.mOwners.clearSystemUpdatePolicy();
            } else {
                this.mOwners.setSystemUpdatePolicy(policy);
            }
            this.mOwners.writeDeviceOwner();
        }
        this.mContext.sendBroadcastAsUser(new Intent("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED"), UserHandle.SYSTEM);
    }

    public SystemUpdatePolicy getSystemUpdatePolicy() {
        synchronized (this) {
            SystemUpdatePolicy policy = this.mOwners.getSystemUpdatePolicy();
            if (policy != null && !policy.isValid()) {
                Slog.w(LOG_TAG, "Stored system update policy is invalid, return null instead.");
                return null;
            }
            return policy;
        }
    }

    boolean isCallerDeviceOwner(int callerUid) {
        synchronized (this) {
            if (!this.mOwners.hasDeviceOwner()) {
                return false;
            }
            if (UserHandle.getUserId(callerUid) != this.mOwners.getDeviceOwnerUserId()) {
                return false;
            }
            String deviceOwnerPackageName = this.mOwners.getDeviceOwnerComponent().getPackageName();
            String[] pkgs = this.mContext.getPackageManager().getPackagesForUid(callerUid);
            for (String pkg : pkgs) {
                if (deviceOwnerPackageName.equals(pkg)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void notifyPendingSystemUpdate(long updateReceivedTime) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NOTIFY_PENDING_SYSTEM_UPDATE", "Only the system update service can broadcast update information");
        if (UserHandle.getCallingUserId() != 0) {
            Slog.w(LOG_TAG, "Only the system update service in the system user can broadcast update information.");
            return;
        }
        Intent intent = new Intent("android.app.action.NOTIFY_PENDING_SYSTEM_UPDATE");
        intent.putExtra("android.app.extra.SYSTEM_UPDATE_RECEIVED_TIME", updateReceivedTime);
        synchronized (this) {
            String packageName = this.mOwners.hasDeviceOwner() ? this.mOwners.getDeviceOwnerComponent().getPackageName() : null;
            if (packageName == null) {
                return;
            }
            UserHandle deviceOwnerUser = new UserHandle(this.mOwners.getDeviceOwnerUserId());
            ActivityInfo[] receivers = null;
            try {
                receivers = this.mContext.getPackageManager().getPackageInfo(packageName, 2).receivers;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "Cannot find device owner package", e);
            }
            if (receivers != null) {
                long ident = this.mInjector.binderClearCallingIdentity();
                for (int i = 0; i < receivers.length; i++) {
                    try {
                        if ("android.permission.BIND_DEVICE_ADMIN".equals(receivers[i].permission)) {
                            intent.setComponent(new ComponentName(packageName, receivers[i].name));
                            this.mContext.sendBroadcastAsUser(intent, deviceOwnerUser);
                        }
                    } finally {
                        this.mInjector.binderRestoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    public void setPermissionPolicy(ComponentName admin, int policy) throws RemoteException {
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
            DevicePolicyData userPolicy = getUserData(userId);
            if (userPolicy.mPermissionPolicy != policy) {
                userPolicy.mPermissionPolicy = policy;
                saveSettingsLocked(userId);
            }
        }
    }

    public int getPermissionPolicy(ComponentName admin) throws RemoteException {
        int i;
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            DevicePolicyData userPolicy = getUserData(userId);
            i = userPolicy.mPermissionPolicy;
        }
        return i;
    }

    public boolean setPermissionGrantState(ComponentName admin, String packageName, String permission, int grantState) throws RemoteException {
        UserHandle user = this.mInjector.binderGetCallingUserHandle();
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                try {
                    if (getTargetSdk(packageName, user.getIdentifier()) < 23) {
                        return false;
                    }
                    if (!isRuntimePermission(permission)) {
                        EventLog.writeEvent(1397638484, "62623498", Integer.valueOf(user.getIdentifier()), "");
                        return false;
                    }
                    PackageManager packageManager = this.mContext.getPackageManager();
                    switch (grantState) {
                        case 0:
                            packageManager.updatePermissionFlags(permission, packageName, 4, 0, user);
                            break;
                        case 1:
                            packageManager.grantRuntimePermission(packageName, permission, user);
                            packageManager.updatePermissionFlags(permission, packageName, 4, 4, user);
                            break;
                        case 2:
                            packageManager.revokeRuntimePermission(packageName, permission, user);
                            packageManager.updatePermissionFlags(permission, packageName, 4, 4, user);
                            break;
                    }
                    return true;
                } catch (SecurityException e) {
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e2) {
                return false;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    public boolean isRuntimePermission(String permissionName) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = this.mContext.getPackageManager();
        PermissionInfo permissionInfo = packageManager.getPermissionInfo(permissionName, 0);
        return (permissionInfo.protectionLevel & 15) == 1;
    }

    public int getPermissionGrantState(ComponentName admin, String packageName, String permission) throws RemoteException {
        int i;
        PackageManager packageManager = this.mContext.getPackageManager();
        UserHandle user = this.mInjector.binderGetCallingUserHandle();
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
            long ident = this.mInjector.binderClearCallingIdentity();
            try {
                int granted = this.mIPackageManager.checkPermission(permission, packageName, user.getIdentifier());
                int permFlags = packageManager.getPermissionFlags(permission, packageName, user);
                if ((permFlags & 4) != 4) {
                    return 0;
                }
                if (granted == 0) {
                    i = 1;
                } else {
                    i = 2;
                }
                return i;
            } finally {
                this.mInjector.binderRestoreCallingIdentity(ident);
            }
        }
    }

    boolean isPackageInstalledForUser(String packageName, int userHandle) {
        try {
            PackageInfo pi = this.mInjector.getIPackageManager().getPackageInfo(packageName, 0, userHandle);
            if (pi != null) {
                return pi.applicationInfo.flags != 0;
            }
            return false;
        } catch (RemoteException re) {
            throw new RuntimeException("Package manager has died", re);
        }
    }

    public boolean isProvisioningAllowed(String action) {
        if (!this.mHasFeature) {
            return false;
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        if ("android.app.action.PROVISION_MANAGED_PROFILE".equals(action)) {
            if (!hasFeatureManagedUsers()) {
                return false;
            }
            synchronized (this) {
                if (this.mOwners.hasDeviceOwner()) {
                    if (!this.mInjector.userManagerIsSplitSystemUser()) {
                        return false;
                    }
                    if (this.mOwners.getDeviceOwnerUserId() != 0) {
                        return false;
                    }
                    if (callingUserId == 0) {
                        return false;
                    }
                }
                if (getProfileOwner(callingUserId) != null) {
                    return false;
                }
                long ident = this.mInjector.binderClearCallingIdentity();
                try {
                    return this.mUserManager.canAddMoreManagedProfiles(callingUserId, true);
                } finally {
                    this.mInjector.binderRestoreCallingIdentity(ident);
                }
            }
        }
        if ("android.app.action.PROVISION_MANAGED_DEVICE".equals(action)) {
            return isDeviceOwnerProvisioningAllowed(callingUserId);
        }
        if ("android.app.action.PROVISION_MANAGED_USER".equals(action)) {
            return hasFeatureManagedUsers() && this.mInjector.userManagerIsSplitSystemUser() && callingUserId != 0 && !hasUserSetupCompleted(callingUserId);
        }
        if ("android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE".equals(action)) {
            if (this.mInjector.userManagerIsSplitSystemUser()) {
                return isDeviceOwnerProvisioningAllowed(callingUserId);
            }
            return false;
        }
        throw new IllegalArgumentException("Unknown provisioning action " + action);
    }

    private synchronized int checkSetDeviceOwnerPreCondition(int deviceOwnerUserId, boolean isAdb) {
        if (this.mOwners.hasDeviceOwner()) {
            return 1;
        }
        if (this.mOwners.hasProfileOwner(deviceOwnerUserId)) {
            return 2;
        }
        if (!this.mUserManager.isUserRunning(new UserHandle(deviceOwnerUserId))) {
            return 3;
        }
        if (isAdb) {
            if (hasUserSetupCompleted(0) && !this.mInjector.userManagerIsSplitSystemUser()) {
                if (this.mUserManager.getUserCount() > 1) {
                    return 5;
                }
                if (AccountManager.get(this.mContext).getAccounts().length > 0) {
                    return 6;
                }
            }
            return 0;
        }
        if (!this.mInjector.userManagerIsSplitSystemUser()) {
            if (deviceOwnerUserId != 0) {
                return 7;
            }
            if (hasUserSetupCompleted(0)) {
                return 4;
            }
        }
        return 0;
    }

    private boolean isDeviceOwnerProvisioningAllowed(int deviceOwnerUserId) {
        return checkSetDeviceOwnerPreCondition(deviceOwnerUserId, false) == 0;
    }

    private boolean hasFeatureManagedUsers() {
        try {
            return this.mIPackageManager.hasSystemFeature("android.software.managed_users", 0);
        } catch (RemoteException e) {
            return false;
        }
    }

    public String getWifiMacAddress(ComponentName admin) {
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            WifiInfo wifiInfo = this.mInjector.getWifiManager().getConnectionInfo();
            if (wifiInfo == null) {
                return null;
            }
            return wifiInfo.hasRealMacAddress() ? wifiInfo.getMacAddress() : null;
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    private int getTargetSdk(String packageName, int userId) {
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, 0, userId);
            if (ai == null) {
                return 0;
            }
            int targetSdkVersion = ai.targetSdkVersion;
            return targetSdkVersion;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public boolean isManagedProfile(ComponentName admin) {
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        UserInfo user = getUserInfo(callingUserId);
        if (user != null) {
            return user.isManagedProfile();
        }
        return false;
    }

    public boolean isSystemOnlyUser(ComponentName admin) {
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        return UserManager.isSplitSystemUser() && callingUserId == 0;
    }

    public void reboot(ComponentName admin) {
        Preconditions.checkNotNull(admin);
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -2);
        }
        long ident = this.mInjector.binderClearCallingIdentity();
        try {
            if (this.mTelephonyManager.getCallState() != 0) {
                throw new IllegalStateException("Cannot be called with ongoing call on the device");
            }
            this.mInjector.powerManagerReboot("deviceowner");
        } finally {
            this.mInjector.binderRestoreCallingIdentity(ident);
        }
    }

    public void setShortSupportMessage(ComponentName who, CharSequence message) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid());
            if (!TextUtils.equals(admin.shortSupportMessage, message)) {
                admin.shortSupportMessage = message;
                saveSettingsLocked(userHandle);
            }
        }
    }

    public CharSequence getShortSupportMessage(ComponentName who) {
        CharSequence charSequence;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid());
            charSequence = admin.shortSupportMessage;
        }
        return charSequence;
    }

    public void setLongSupportMessage(ComponentName who, CharSequence message) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid());
            if (!TextUtils.equals(admin.longSupportMessage, message)) {
                admin.longSupportMessage = message;
                saveSettingsLocked(userHandle);
            }
        }
    }

    public CharSequence getLongSupportMessage(ComponentName who) {
        CharSequence charSequence;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForUidLocked(who, this.mInjector.binderGetCallingUid());
            charSequence = admin.longSupportMessage;
        }
        return charSequence;
    }

    public CharSequence getShortSupportMessageForUser(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query support message for user");
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            if (admin == null) {
                return null;
            }
            return admin.shortSupportMessage;
        }
    }

    public CharSequence getLongSupportMessageForUser(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        if (!isCallerWithSystemUid()) {
            throw new SecurityException("Only the system can query support message for user");
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            if (admin == null) {
                return null;
            }
            return admin.longSupportMessage;
        }
    }

    public void setOrganizationColor(ComponentName who, int color) {
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        enforceManagedProfile(userHandle, "set organization color");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            admin.organizationColor = color;
            saveSettingsLocked(userHandle);
        }
    }

    public void setOrganizationColorForUser(int color, int userId) {
        if (!this.mHasFeature) {
            return;
        }
        enforceFullCrossUsersPermission(userId);
        enforceManageUsers();
        enforceManagedProfile(userId, "set organization color");
        synchronized (this) {
            ActiveAdmin admin = getProfileOwnerAdminLocked(userId);
            admin.organizationColor = color;
            saveSettingsLocked(userId);
        }
    }

    public int getOrganizationColor(ComponentName who) {
        int i;
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "get organization color");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            i = admin.organizationColor;
        }
        return i;
    }

    public int getOrganizationColorForUser(int userHandle) {
        int i;
        if (!this.mHasFeature) {
            return ActiveAdmin.DEF_ORGANIZATION_COLOR;
        }
        enforceFullCrossUsersPermission(userHandle);
        enforceManagedProfile(userHandle, "get organization color");
        synchronized (this) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userHandle);
            if (profileOwner != null) {
                i = profileOwner.organizationColor;
            } else {
                i = ActiveAdmin.DEF_ORGANIZATION_COLOR;
            }
        }
        return i;
    }

    public void setOrganizationName(ComponentName who, CharSequence text) {
        String string = null;
        if (!this.mHasFeature) {
            return;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        int userHandle = this.mInjector.userHandleGetCallingUserId();
        enforceManagedProfile(userHandle, "set organization name");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            if (!TextUtils.equals(admin.organizationName, text)) {
                if (text != null && text.length() != 0) {
                    string = text.toString();
                }
                admin.organizationName = string;
                saveSettingsLocked(userHandle);
            }
        }
    }

    public CharSequence getOrganizationName(ComponentName who) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        Preconditions.checkNotNull(who, "ComponentName is null");
        enforceManagedProfile(this.mInjector.userHandleGetCallingUserId(), "get organization name");
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            str = admin.organizationName;
        }
        return str;
    }

    public CharSequence getOrganizationNameForUser(int userHandle) {
        String str;
        if (!this.mHasFeature) {
            return null;
        }
        enforceFullCrossUsersPermission(userHandle);
        enforceManagedProfile(userHandle, "get organization name");
        synchronized (this) {
            ActiveAdmin profileOwner = getProfileOwnerAdminLocked(userHandle);
            str = profileOwner != null ? profileOwner.organizationName : null;
        }
        return str;
    }

    public void setAffiliationIds(ComponentName admin, List<String> ids) {
        Set<String> affiliationIds = new ArraySet<>(ids);
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -1);
            getUserData(callingUserId).mAffiliationIds = affiliationIds;
            saveSettingsLocked(callingUserId);
            if (callingUserId != 0 && isDeviceOwner(admin, callingUserId)) {
                getUserData(0).mAffiliationIds = affiliationIds;
                saveSettingsLocked(0);
            }
        }
    }

    public boolean isAffiliatedUser() {
        int callingUserId = this.mInjector.userHandleGetCallingUserId();
        synchronized (this) {
            if (this.mOwners.getDeviceOwnerUserId() == callingUserId) {
                return true;
            }
            ComponentName profileOwner = getProfileOwner(callingUserId);
            if (profileOwner == null || !profileOwner.getPackageName().equals(this.mOwners.getDeviceOwnerPackageName())) {
                return false;
            }
            Set<String> userAffiliationIds = getUserData(callingUserId).mAffiliationIds;
            Set<String> deviceAffiliationIds = getUserData(0).mAffiliationIds;
            for (String id : userAffiliationIds) {
                if (deviceAffiliationIds.contains(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    private synchronized void disableSecurityLoggingIfNotCompliant() {
        if (!isDeviceOwnerManagedSingleUserDevice()) {
            this.mInjector.securityLogSetLoggingEnabledProperty(false);
            Slog.w(LOG_TAG, "Security logging turned off as it's no longer a single user device.");
        }
    }

    public void setSecurityLoggingEnabled(ComponentName admin, boolean enabled) {
        Preconditions.checkNotNull(admin);
        ensureDeviceOwnerManagingSingleUser(admin);
        synchronized (this) {
            if (enabled == this.mInjector.securityLogGetLoggingEnabledProperty()) {
                return;
            }
            this.mInjector.securityLogSetLoggingEnabledProperty(enabled);
            if (enabled) {
                this.mSecurityLogMonitor.start();
            } else {
                this.mSecurityLogMonitor.stop();
            }
        }
    }

    public boolean isSecurityLoggingEnabled(ComponentName admin) {
        boolean zSecurityLogGetLoggingEnabledProperty;
        Preconditions.checkNotNull(admin);
        synchronized (this) {
            getActiveAdminForCallerLocked(admin, -2);
            zSecurityLogGetLoggingEnabledProperty = this.mInjector.securityLogGetLoggingEnabledProperty();
        }
        return zSecurityLogGetLoggingEnabledProperty;
    }

    public ParceledListSlice<SecurityLog.SecurityEvent> retrievePreRebootSecurityLogs(ComponentName admin) {
        Preconditions.checkNotNull(admin);
        ensureDeviceOwnerManagingSingleUser(admin);
        if (!this.mContext.getResources().getBoolean(R.^attr-private.pointerIconAllScroll)) {
            return null;
        }
        ArrayList<SecurityLog.SecurityEvent> output = new ArrayList<>();
        try {
            SecurityLog.readPreviousEvents(output);
            return new ParceledListSlice<>(output);
        } catch (IOException e) {
            Slog.w(LOG_TAG, "Fail to read previous events", e);
            return new ParceledListSlice<>(Collections.emptyList());
        }
    }

    public ParceledListSlice<SecurityLog.SecurityEvent> retrieveSecurityLogs(ComponentName admin) {
        Preconditions.checkNotNull(admin);
        ensureDeviceOwnerManagingSingleUser(admin);
        List<SecurityLog.SecurityEvent> logs = this.mSecurityLogMonitor.retrieveLogs();
        if (logs != null) {
            return new ParceledListSlice<>(logs);
        }
        return null;
    }

    private void enforceCanManageDeviceAdmin() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
    }

    private void enforceCanManageProfileAndDeviceOwners() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", null);
    }

    public boolean isUninstallInQueue(String packageName) {
        boolean zContains;
        enforceCanManageDeviceAdmin();
        int userId = this.mInjector.userHandleGetCallingUserId();
        Pair<String, Integer> packageUserPair = new Pair<>(packageName, Integer.valueOf(userId));
        synchronized (this) {
            zContains = this.mPackagesToRemove.contains(packageUserPair);
        }
        return zContains;
    }

    public void uninstallPackageWithActiveAdmins(final String packageName) {
        enforceCanManageDeviceAdmin();
        Preconditions.checkArgument(!TextUtils.isEmpty(packageName));
        final int userId = this.mInjector.userHandleGetCallingUserId();
        enforceUserUnlocked(userId);
        ComponentName profileOwner = getProfileOwner(userId);
        if (profileOwner != null && packageName.equals(profileOwner.getPackageName())) {
            throw new IllegalArgumentException("Cannot uninstall a package with a profile owner");
        }
        ComponentName deviceOwner = getDeviceOwnerComponent(false);
        if (getDeviceOwnerUserId() == userId && deviceOwner != null && packageName.equals(deviceOwner.getPackageName())) {
            throw new IllegalArgumentException("Cannot uninstall a package with a device owner");
        }
        Pair<String, Integer> packageUserPair = new Pair<>(packageName, Integer.valueOf(userId));
        synchronized (this) {
            this.mPackagesToRemove.add(packageUserPair);
        }
        List<ComponentName> allActiveAdmins = getActiveAdmins(userId);
        final List<ComponentName> packageActiveAdmins = new ArrayList<>();
        if (allActiveAdmins != null) {
            for (ComponentName activeAdmin : allActiveAdmins) {
                if (packageName.equals(activeAdmin.getPackageName())) {
                    packageActiveAdmins.add(activeAdmin);
                    removeActiveAdmin(activeAdmin, userId);
                }
            }
        }
        if (packageActiveAdmins.size() == 0) {
            startUninstallIntent(packageName, userId);
        } else {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (ComponentName activeAdmin2 : packageActiveAdmins) {
                        DevicePolicyManagerService.this.removeAdminArtifacts(activeAdmin2, userId);
                    }
                    DevicePolicyManagerService.this.startUninstallIntent(packageName, userId);
                }
            }, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    private void removePackageIfRequired(String packageName, int userId) {
        if (packageHasActiveAdmins(packageName, userId)) {
            return;
        }
        startUninstallIntent(packageName, userId);
    }

    private void startUninstallIntent(String packageName, int userId) {
        Pair<String, Integer> packageUserPair = new Pair<>(packageName, Integer.valueOf(userId));
        synchronized (this) {
            if (!this.mPackagesToRemove.contains(packageUserPair)) {
                return;
            }
            this.mPackagesToRemove.remove(packageUserPair);
            try {
                if (this.mInjector.getIPackageManager().getPackageInfo(packageName, 0, userId) == null) {
                    return;
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Failure talking to PackageManager while getting package info");
            }
            try {
                this.mInjector.getIActivityManager().forceStopPackage(packageName, userId);
            } catch (RemoteException e2) {
                Log.e(LOG_TAG, "Failure talking to ActivityManager while force stopping package");
            }
            Uri packageURI = Uri.parse("package:" + packageName);
            Intent uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", packageURI);
            uninstallIntent.setFlags(268435456);
            this.mContext.startActivityAsUser(uninstallIntent, UserHandle.of(userId));
        }
    }

    private void removeAdminArtifacts(ComponentName adminReceiver, int userHandle) {
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
            if (admin == null) {
                return;
            }
            DevicePolicyData policy = getUserData(userHandle);
            boolean doProxyCleanup = admin.info.usesPolicy(5);
            policy.mAdminList.remove(admin);
            policy.mAdminMap.remove(adminReceiver);
            validatePasswordOwnerLocked(policy);
            if (doProxyCleanup) {
                resetGlobalProxyLocked(policy);
            }
            saveSettingsLocked(userHandle);
            updateMaximumTimeToLockLocked(userHandle);
            policy.mRemovingAdmins.remove(adminReceiver);
            pushUserRestrictions(userHandle);
        }
    }
}
