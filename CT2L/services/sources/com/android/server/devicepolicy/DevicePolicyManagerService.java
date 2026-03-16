package com.android.server.devicepolicy;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.AccountManager;
import android.app.ActivityManager;
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
import android.app.backup.IBackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.IAudioService;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
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
import android.provider.Settings;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.service.persistentdata.PersistentDataBlockManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.os.storage.ExternalStorageFormatter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DevicePolicyManagerService extends IDevicePolicyManager.Stub {
    protected static final String ACTION_EXPIRED_PASSWORD_NOTIFICATION = "com.android.server.ACTION_EXPIRED_PASSWORD_NOTIFICATION";
    private static final String ATTR_PERMISSION_PROVIDER = "permission-provider";
    private static final String ATTR_SETUP_COMPLETE = "setup-complete";
    private static final boolean DBG = false;
    private static final Set<String> DEVICE_OWNER_USER_RESTRICTIONS = new HashSet();
    private static final String DEVICE_POLICIES_XML = "device_policies.xml";
    private static final long EXPIRATION_GRACE_PERIOD_MS = 432000000;
    private static final Set<String> GLOBAL_SETTINGS_WHITELIST;
    private static final String LOCK_TASK_COMPONENTS_XML = "lock-task-component";
    private static final String LOG_TAG = "DevicePolicyManagerService";
    private static final int MONITORING_CERT_NOTIFICATION_ID = 17039592;
    private static final long MS_PER_DAY = 86400000;
    private static final int REQUEST_EXPIRE_PASSWORD = 5571;
    private static final Set<String> SECURE_SETTINGS_DEVICEOWNER_WHITELIST;
    private static final Set<String> SECURE_SETTINGS_WHITELIST;
    public static final String SYSTEM_PROP_DISABLE_CAMERA = "sys.secpolicy.camera.disabled";
    final Context mContext;
    private DeviceOwner mDeviceOwner;
    private boolean mHasFeature;
    IWindowManager mIWindowManager;
    NotificationManager mNotificationManager;
    final PowerManager mPowerManager;
    final UserManager mUserManager;
    final PowerManager.WakeLock mWakeLock;
    final SparseArray<DevicePolicyData> mUserData = new SparseArray<>();
    Handler mHandler = new Handler();
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final int userHandle = intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId());
            if ("android.intent.action.BOOT_COMPLETED".equals(action) || DevicePolicyManagerService.ACTION_EXPIRED_PASSWORD_NOTIFICATION.equals(action)) {
                DevicePolicyManagerService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DevicePolicyManagerService.this.handlePasswordExpirationNotification(userHandle);
                    }
                });
            }
            if ("android.intent.action.BOOT_COMPLETED".equals(action) || "android.security.STORAGE_CHANGED".equals(action)) {
                new MonitoringCertNotificationTask().execute(intent);
            }
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                DevicePolicyManagerService.this.removeUserData(userHandle);
                return;
            }
            if ("android.intent.action.USER_STARTED".equals(action) || "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                if ("android.intent.action.USER_STARTED".equals(action)) {
                    synchronized (DevicePolicyManagerService.this) {
                        DevicePolicyManagerService.this.mUserData.remove(userHandle);
                    }
                }
                DevicePolicyManagerService.this.handlePackagesChanged(null, userHandle);
                return;
            }
            if ("android.intent.action.PACKAGE_CHANGED".equals(action) || ("android.intent.action.PACKAGE_ADDED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", DevicePolicyManagerService.DBG))) {
                DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !intent.getBooleanExtra("android.intent.extra.REPLACING", DevicePolicyManagerService.DBG)) {
                DevicePolicyManagerService.this.handlePackagesChanged(intent.getData().getSchemeSpecificPart(), userHandle);
            }
        }
    };
    final PowerManagerInternal mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
    final LocalService mLocalService = new LocalService();

    static {
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_usb_file_transfer");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_config_tethering");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_factory_reset");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_add_user");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_config_cell_broadcasts");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_config_mobile_networks");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_physical_media");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_unmute_microphone");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_adjust_volume");
        DEVICE_OWNER_USER_RESTRICTIONS.add("no_sms");
        SECURE_SETTINGS_WHITELIST = new HashSet();
        SECURE_SETTINGS_WHITELIST.add("default_input_method");
        SECURE_SETTINGS_WHITELIST.add("skip_first_use_hints");
        SECURE_SETTINGS_WHITELIST.add("install_non_market_apps");
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST = new HashSet();
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST.addAll(SECURE_SETTINGS_WHITELIST);
        SECURE_SETTINGS_DEVICEOWNER_WHITELIST.add("location_mode");
        GLOBAL_SETTINGS_WHITELIST = new HashSet();
        GLOBAL_SETTINGS_WHITELIST.add("adb_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("auto_time");
        GLOBAL_SETTINGS_WHITELIST.add("auto_time_zone");
        GLOBAL_SETTINGS_WHITELIST.add("bluetooth_on");
        GLOBAL_SETTINGS_WHITELIST.add("data_roaming");
        GLOBAL_SETTINGS_WHITELIST.add("development_settings_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("mode_ringer");
        GLOBAL_SETTINGS_WHITELIST.add("network_preference");
        GLOBAL_SETTINGS_WHITELIST.add("usb_mass_storage_enabled");
        GLOBAL_SETTINGS_WHITELIST.add("wifi_on");
        GLOBAL_SETTINGS_WHITELIST.add("wifi_sleep_policy");
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
            if (phase == 480) {
                this.mService.systemReady();
            }
        }
    }

    public static class DevicePolicyData {
        ComponentName mRestrictionsProvider;
        int mUserHandle;
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
        boolean mUserSetupComplete = DevicePolicyManagerService.DBG;
        final HashMap<ComponentName, ActiveAdmin> mAdminMap = new HashMap<>();
        final ArrayList<ActiveAdmin> mAdminList = new ArrayList<>();
        final ArrayList<ComponentName> mRemovingAdmins = new ArrayList<>();
        final List<String> mLockTaskPackages = new ArrayList();

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
        static final long DEF_PASSWORD_EXPIRATION_DATE = 0;
        static final long DEF_PASSWORD_EXPIRATION_TIMEOUT = 0;
        static final int DEF_PASSWORD_HISTORY_LENGTH = 0;
        private static final String TAG_ACCOUNT_TYPE = "account-type";
        private static final String TAG_CROSS_PROFILE_WIDGET_PROVIDERS = "cross-profile-widget-providers";
        private static final String TAG_DISABLE_ACCOUNT_MANAGEMENT = "disable-account-management";
        private static final String TAG_DISABLE_CALLER_ID = "disable-caller-id";
        private static final String TAG_DISABLE_CAMERA = "disable-camera";
        private static final String TAG_DISABLE_KEYGUARD_FEATURES = "disable-keyguard-features";
        private static final String TAG_DISABLE_SCREEN_CAPTURE = "disable-screen-capture";
        private static final String TAG_ENCRYPTION_REQUESTED = "encryption-requested";
        private static final String TAG_GLOBAL_PROXY_EXCLUSION_LIST = "global-proxy-exclusion-list";
        private static final String TAG_GLOBAL_PROXY_SPEC = "global-proxy-spec";
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
        private static final String TAG_PACKAGE_LIST_ITEM = "item";
        private static final String TAG_PASSWORD_EXPIRATION_DATE = "password-expiration-date";
        private static final String TAG_PASSWORD_EXPIRATION_TIMEOUT = "password-expiration-timeout";
        private static final String TAG_PASSWORD_HISTORY_LENGTH = "password-history-length";
        private static final String TAG_PASSWORD_QUALITY = "password-quality";
        private static final String TAG_PERMITTED_ACCESSIBILITY_SERVICES = "permitted-accessiblity-services";
        private static final String TAG_PERMITTED_IMES = "permitted-imes";
        private static final String TAG_POLICIES = "policies";
        private static final String TAG_PROVIDER = "provider";
        private static final String TAG_REQUIRE_AUTO_TIME = "require_auto_time";
        private static final String TAG_SPECIFIES_GLOBAL_PROXY = "specifies-global-proxy";
        private static final String TAG_TRUST_AGENT_COMPONENT = "component";
        private static final String TAG_TRUST_AGENT_COMPONENT_OPTIONS = "trust-agent-component-options";
        List<String> crossProfileWidgetProviders;
        final DeviceAdminInfo info;
        List<String> permittedAccessiblityServices;
        List<String> permittedInputMethods;
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
        boolean encryptionRequested = DevicePolicyManagerService.DBG;
        boolean disableCamera = DevicePolicyManagerService.DBG;
        boolean disableCallerId = DevicePolicyManagerService.DBG;
        boolean disableScreenCapture = DevicePolicyManagerService.DBG;
        boolean requireAutoTime = DevicePolicyManagerService.DBG;
        Set<String> accountTypesWithManagementDisabled = new HashSet();
        boolean specifiesGlobalProxy = DevicePolicyManagerService.DBG;
        String globalProxySpec = null;
        String globalProxyExclusionList = null;
        HashMap<String, TrustAgentInfo> trustAgentInfos = new HashMap<>();

        static class TrustAgentInfo {
            public PersistableBundle options;

            TrustAgentInfo(PersistableBundle bundle) {
                this.options = bundle;
            }
        }

        ActiveAdmin(DeviceAdminInfo _info) {
            this.info = _info;
        }

        int getUid() {
            return this.info.getActivityInfo().applicationInfo.uid;
        }

        public UserHandle getUserHandle() {
            return new UserHandle(UserHandle.getUserId(this.info.getActivityInfo().applicationInfo.uid));
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
        }

        void writePackageListToXml(XmlSerializer out, String outerTag, List<String> packageList) throws IllegalStateException, IOException, IllegalArgumentException {
            if (packageList != null) {
                out.startTag(null, outerTag);
                for (String packageName : packageList) {
                    out.startTag(null, TAG_PACKAGE_LIST_ITEM);
                    out.attribute(null, ATTR_VALUE, packageName);
                    out.endTag(null, TAG_PACKAGE_LIST_ITEM);
                }
                out.endTag(null, outerTag);
            }
        }

        void readFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1) {
                    return;
                }
                if (type != 3 || parser.getDepth() > outerDepth) {
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
                        } else if (TAG_DISABLE_SCREEN_CAPTURE.equals(tag)) {
                            this.disableScreenCapture = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
                        } else if (TAG_REQUIRE_AUTO_TIME.equals(tag)) {
                            this.requireAutoTime = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_VALUE));
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
                        } else {
                            Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown admin tag: " + tag);
                        }
                        XmlUtils.skipCurrentTag(parser);
                    }
                } else {
                    return;
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
            Set<String> result = new HashSet<>();
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

        private HashMap<String, TrustAgentInfo> getAllTrustAgentInfos(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            HashMap<String, TrustAgentInfo> result = new HashMap<>();
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
                        PersistableBundle bundle = new PersistableBundle();
                        PersistableBundle.restoreFromXml(parser);
                        result.options = bundle;
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + tagDAM);
                    }
                }
            }
            return result;
        }

        private List<String> getCrossProfileWidgetProviders(XmlPullParser parser, String tag) throws XmlPullParserException, IOException {
            int outerDepthDAM = parser.getDepth();
            ArrayList<String> result = null;
            while (true) {
                int typeDAM = parser.next();
                if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                    break;
                }
                if (typeDAM != 3 && typeDAM != 4) {
                    String tagDAM = parser.getName();
                    if (TAG_PROVIDER.equals(tagDAM)) {
                        String provider = parser.getAttributeValue(null, ATTR_VALUE);
                        if (result == null) {
                            result = new ArrayList<>();
                        }
                        result.add(provider);
                    } else {
                        Slog.w(DevicePolicyManagerService.LOG_TAG, "Unknown tag under " + tag + ": " + tagDAM);
                    }
                }
            }
            return result;
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
            pw.print("disableScreenCapture=");
            pw.println(this.disableScreenCapture);
            pw.print(prefix);
            pw.print("requireAutoTime=");
            pw.println(this.requireAutoTime);
            pw.print(prefix);
            pw.print("disabledKeyguardFeatures=");
            pw.println(this.disabledKeyguardFeatures);
            pw.print(prefix);
            pw.print("crossProfileWidgetProviders=");
            pw.println(this.crossProfileWidgetProviders);
            if (this.permittedAccessiblityServices != null) {
                pw.print(prefix);
                pw.print("permittedAccessibilityServices=");
                pw.println(this.permittedAccessiblityServices.toString());
            }
            if (this.permittedInputMethods != null) {
                pw.print(prefix);
                pw.print("permittedInputMethods=");
                pw.println(this.permittedInputMethods.toString());
            }
        }
    }

    private void handlePackagesChanged(String packageName, int userHandle) {
        boolean removed = DBG;
        DevicePolicyData policy = getUserData(userHandle);
        IPackageManager pm = AppGlobals.getPackageManager();
        synchronized (this) {
            for (int i = policy.mAdminList.size() - 1; i >= 0; i--) {
                ActiveAdmin aa = policy.mAdminList.get(i);
                try {
                    String adminPackage = aa.info.getPackageName();
                    if ((packageName == null || packageName.equals(adminPackage)) && (pm.getPackageInfo(adminPackage, 0, userHandle) == null || pm.getReceiverInfo(aa.info.getComponent(), 0, userHandle) == null)) {
                        removed = true;
                        policy.mAdminList.remove(i);
                        policy.mAdminMap.remove(aa.info.getComponent());
                    }
                } catch (RemoteException e) {
                }
            }
            if (removed) {
                validatePasswordOwnerLocked(policy);
                syncDeviceCapabilitiesLocked(policy);
                saveSettingsLocked(policy.mUserHandle);
            }
        }
    }

    public DevicePolicyManagerService(Context context) {
        this.mContext = context;
        this.mUserManager = UserManager.get(this.mContext);
        this.mHasFeature = context.getPackageManager().hasSystemFeature("android.software.device_admin");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, "DPM");
        if (this.mHasFeature) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction(ACTION_EXPIRED_PASSWORD_NOTIFICATION);
            filter.addAction("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.USER_STARTED");
            filter.addAction("android.security.STORAGE_CHANGED");
            filter.setPriority(1000);
            context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mHandler);
            IntentFilter filter2 = new IntentFilter();
            filter2.addAction("android.intent.action.PACKAGE_CHANGED");
            filter2.addAction("android.intent.action.PACKAGE_REMOVED");
            filter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            filter2.addAction("android.intent.action.PACKAGE_ADDED");
            filter2.addDataScheme("package");
            context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter2, null, this.mHandler);
            LocalServices.addService(DevicePolicyManagerInternal.class, this.mLocalService);
        }
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
        long ident = Binder.clearCallingIdentity();
        try {
            return getUserData(userHandle);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void removeUserData(int userHandle) {
        synchronized (this) {
            if (userHandle == 0) {
                Slog.w(LOG_TAG, "Tried to remove device policy file for user 0! Ignoring.");
                return;
            }
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.removeProfileOwner(userHandle);
                this.mDeviceOwner.writeOwnerFile();
            }
            DevicePolicyData policy = this.mUserData.get(userHandle);
            if (policy != null) {
                this.mUserData.remove(userHandle);
            }
            File policyFile = new File(Environment.getUserSystemDirectory(userHandle), DEVICE_POLICIES_XML);
            policyFile.delete();
            Slog.i(LOG_TAG, "Removed device policy file " + policyFile.getAbsolutePath());
            updateScreenCaptureDisabledInWindowManager(userHandle, DBG);
        }
    }

    void loadDeviceOwner() {
        synchronized (this) {
            this.mDeviceOwner = DeviceOwner.load();
        }
    }

    protected void setExpirationAlarmCheckLocked(Context context, DevicePolicyData policy) {
        long alarmTime;
        long expiration = getPasswordExpirationLocked(null, policy.mUserHandle);
        long now = System.currentTimeMillis();
        long timeToExpire = expiration - now;
        if (expiration == 0) {
            alarmTime = 0;
        } else if (timeToExpire <= 0) {
            alarmTime = now + MS_PER_DAY;
        } else {
            long alarmInterval = timeToExpire % MS_PER_DAY;
            if (alarmInterval == 0) {
                alarmInterval = MS_PER_DAY;
            }
            alarmTime = now + alarmInterval;
        }
        long token = Binder.clearCallingIdentity();
        try {
            AlarmManager am = (AlarmManager) context.getSystemService("alarm");
            PendingIntent pi = PendingIntent.getBroadcastAsUser(context, REQUEST_EXPIRE_PASSWORD, new Intent(ACTION_EXPIRED_PASSWORD_NOTIFICATION), 1207959552, new UserHandle(policy.mUserHandle));
            am.cancel(pi);
            if (alarmTime != 0) {
                am.set(1, alarmTime, pi);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private IWindowManager getWindowManager() {
        if (this.mIWindowManager == null) {
            IBinder b = ServiceManager.getService("window");
            this.mIWindowManager = IWindowManager.Stub.asInterface(b);
        }
        return this.mIWindowManager;
    }

    private NotificationManager getNotificationManager() {
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        }
        return this.mNotificationManager;
    }

    ActiveAdmin getActiveAdminUncheckedLocked(ComponentName who, int userHandle) {
        ActiveAdmin admin = getUserData(userHandle).mAdminMap.get(who);
        if (admin != null && who.getPackageName().equals(admin.info.getActivityInfo().packageName) && who.getClassName().equals(admin.info.getActivityInfo().name)) {
            return admin;
        }
        return null;
    }

    ActiveAdmin getActiveAdminForCallerLocked(ComponentName who, int reqPolicy) throws SecurityException {
        int callingUid = Binder.getCallingUid();
        int userHandle = UserHandle.getUserId(callingUid);
        DevicePolicyData policy = getUserData(userHandle);
        List<ActiveAdmin> candidates = new ArrayList<>();
        if (who != null) {
            ActiveAdmin admin = policy.mAdminMap.get(who);
            if (admin == null) {
                throw new SecurityException("No active admin " + who);
            }
            if (admin.getUid() != callingUid) {
                throw new SecurityException("Admin " + who + " is not owned by uid " + Binder.getCallingUid());
            }
            candidates.add(admin);
        } else {
            for (ActiveAdmin admin2 : policy.mAdminList) {
                if (admin2.getUid() == callingUid) {
                    candidates.add(admin2);
                }
            }
        }
        for (ActiveAdmin admin3 : candidates) {
            boolean ownsDevice = isDeviceOwner(admin3.info.getPackageName());
            boolean ownsProfile = getProfileOwner(userHandle) != null && getProfileOwner(userHandle).getPackageName().equals(admin3.info.getPackageName());
            if (reqPolicy == -2) {
                if (ownsDevice) {
                    return admin3;
                }
            } else if (reqPolicy == -1) {
                if (ownsDevice || ownsProfile) {
                    return admin3;
                }
            } else if (admin3.info.usesPolicy(reqPolicy)) {
                return admin3;
            }
        }
        if (who != null) {
            if (reqPolicy == -2) {
                throw new SecurityException("Admin " + candidates.get(0).info.getComponent() + " does not own the device");
            }
            if (reqPolicy == -1) {
                throw new SecurityException("Admin " + candidates.get(0).info.getComponent() + " does not own the profile");
            }
            throw new SecurityException("Admin " + candidates.get(0).info.getComponent() + " did not specify uses-policy for: " + candidates.get(0).info.getTagForPolicy(reqPolicy));
        }
        throw new SecurityException("No active admin owned by uid " + Binder.getCallingUid() + " for policy #" + reqPolicy);
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
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                ActiveAdmin admin = policy.mAdminList.get(i);
                if (admin.info.usesPolicy(reqPolicy)) {
                    sendAdminCommandLocked(admin, action);
                }
            }
        }
    }

    private void sendAdminCommandToSelfAndProfilesLocked(String action, int reqPolicy, int userHandle) {
        List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
        for (UserInfo ui : profiles) {
            int id = ui.id;
            sendAdminCommandLocked(action, reqPolicy, id);
        }
    }

    void removeActiveAdminLocked(final ComponentName adminReceiver, int userHandle) {
        final ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
        if (admin != null) {
            synchronized (this) {
                getUserData(userHandle).mRemovingAdmins.add(adminReceiver);
            }
            sendAdminCommandLocked(admin, "android.app.action.DEVICE_ADMIN_DISABLED", new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    synchronized (DevicePolicyManagerService.this) {
                        int userHandle2 = admin.getUserHandle().getIdentifier();
                        DevicePolicyData policy = DevicePolicyManagerService.this.getUserData(userHandle2);
                        boolean doProxyCleanup = admin.info.usesPolicy(5);
                        policy.mAdminList.remove(admin);
                        policy.mAdminMap.remove(adminReceiver);
                        DevicePolicyManagerService.this.validatePasswordOwnerLocked(policy);
                        DevicePolicyManagerService.this.syncDeviceCapabilitiesLocked(policy);
                        if (doProxyCleanup) {
                            DevicePolicyManagerService.this.resetGlobalProxyLocked(DevicePolicyManagerService.this.getUserData(userHandle2));
                        }
                        DevicePolicyManagerService.this.saveSettingsLocked(userHandle2);
                        DevicePolicyManagerService.this.updateMaximumTimeToLockLocked(policy);
                        policy.mRemovingAdmins.remove(adminReceiver);
                    }
                }
            });
        }
    }

    public DeviceAdminInfo findAdmin(ComponentName adminName, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceCrossUserPermission(userHandle);
        Intent resolveIntent = new Intent();
        resolveIntent.setComponent(adminName);
        List<ResolveInfo> infos = this.mContext.getPackageManager().queryBroadcastReceivers(resolveIntent, 32896, userHandle);
        if (infos == null || infos.size() <= 0) {
            throw new IllegalArgumentException("Unknown admin: " + adminName);
        }
        try {
            return new DeviceAdminInfo(this.mContext, infos.get(0));
        } catch (IOException e) {
            Slog.w(LOG_TAG, "Bad device admin requested for user=" + userHandle + ": " + adminName, e);
            return null;
        } catch (XmlPullParserException e2) {
            Slog.w(LOG_TAG, "Bad device admin requested for user=" + userHandle + ": " + adminName, e2);
            return null;
        }
    }

    private static JournaledFile makeJournaledFile(int userHandle) {
        String base = userHandle == 0 ? "/data/system/device_policies.xml" : new File(Environment.getUserSystemDirectory(userHandle), DEVICE_POLICIES_XML).getAbsolutePath();
        return new JournaledFile(new File(base), new File(base + ".tmp"));
    }

    private void saveSettingsLocked(int userHandle) {
        FileOutputStream stream;
        DevicePolicyData policy = getUserData(userHandle);
        JournaledFile journal = makeJournaledFile(userHandle);
        FileOutputStream stream2 = null;
        try {
            stream = new FileOutputStream(journal.chooseForWrite(), DBG);
        } catch (IOException e) {
        }
        try {
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, "utf-8");
            out.startDocument(null, true);
            out.startTag(null, "policies");
            if (policy.mRestrictionsProvider != null) {
                out.attribute(null, ATTR_PERMISSION_PROVIDER, policy.mRestrictionsProvider.flattenToString());
            }
            if (policy.mUserSetupComplete) {
                out.attribute(null, ATTR_SETUP_COMPLETE, Boolean.toString(true));
            }
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin ap = policy.mAdminList.get(i);
                if (ap != null) {
                    out.startTag(null, "admin");
                    out.attribute(null, "name", ap.info.getComponent().flattenToString());
                    ap.writeToXml(out);
                    out.endTag(null, "admin");
                }
            }
            if (policy.mPasswordOwner >= 0) {
                out.startTag(null, "password-owner");
                out.attribute(null, "value", Integer.toString(policy.mPasswordOwner));
                out.endTag(null, "password-owner");
            }
            if (policy.mFailedPasswordAttempts != 0) {
                out.startTag(null, "failed-password-attempts");
                out.attribute(null, "value", Integer.toString(policy.mFailedPasswordAttempts));
                out.endTag(null, "failed-password-attempts");
            }
            if (policy.mActivePasswordQuality != 0 || policy.mActivePasswordLength != 0 || policy.mActivePasswordUpperCase != 0 || policy.mActivePasswordLowerCase != 0 || policy.mActivePasswordLetters != 0 || policy.mActivePasswordNumeric != 0 || policy.mActivePasswordSymbols != 0 || policy.mActivePasswordNonLetter != 0) {
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
            for (int i2 = 0; i2 < policy.mLockTaskPackages.size(); i2++) {
                String component = policy.mLockTaskPackages.get(i2);
                out.startTag(null, LOCK_TASK_COMPONENTS_XML);
                out.attribute(null, "name", component);
                out.endTag(null, LOCK_TASK_COMPONENTS_XML);
            }
            out.endTag(null, "policies");
            out.endDocument();
            stream.flush();
            FileUtils.sync(stream);
            stream.close();
            journal.commit();
            sendChangedNotification(userHandle);
        } catch (IOException e2) {
            stream2 = stream;
            if (stream2 != null) {
                try {
                    stream2.close();
                } catch (IOException e3) {
                }
            }
            journal.rollback();
        }
    }

    private void sendChangedNotification(int userHandle) {
        Intent intent = new Intent("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intent.setFlags(1073741824);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void loadSettingsLocked(DevicePolicyData policy, int userHandle) {
        FileInputStream stream;
        XmlPullParser parser;
        int type;
        String tag;
        JournaledFile journal = makeJournaledFile(userHandle);
        FileInputStream stream2 = null;
        File file = journal.chooseForRead();
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            e = e2;
        } catch (IndexOutOfBoundsException e3) {
            e = e3;
        } catch (NullPointerException e4) {
            e = e4;
        } catch (NumberFormatException e5) {
            e = e5;
        } catch (XmlPullParserException e6) {
            e = e6;
        }
        try {
            parser = Xml.newPullParser();
            parser.setInput(stream, null);
            do {
                type = parser.next();
                if (type == 1) {
                    break;
                }
            } while (type != 2);
            tag = parser.getName();
        } catch (FileNotFoundException e7) {
            stream2 = stream;
        } catch (IOException e8) {
            e = e8;
            stream2 = stream;
            Slog.w(LOG_TAG, "failed parsing " + file + " " + e);
        } catch (IndexOutOfBoundsException e9) {
            e = e9;
            stream2 = stream;
            Slog.w(LOG_TAG, "failed parsing " + file + " " + e);
        } catch (NullPointerException e10) {
            e = e10;
            stream2 = stream;
            Slog.w(LOG_TAG, "failed parsing " + file + " " + e);
        } catch (NumberFormatException e11) {
            e = e11;
            stream2 = stream;
            Slog.w(LOG_TAG, "failed parsing " + file + " " + e);
        } catch (XmlPullParserException e12) {
            e = e12;
            stream2 = stream;
            Slog.w(LOG_TAG, "failed parsing " + file + " " + e);
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
        parser.next();
        int outerDepth = parser.getDepth();
        policy.mLockTaskPackages.clear();
        policy.mAdminList.clear();
        policy.mAdminMap.clear();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type2 != 3 && type2 != 4) {
                String tag2 = parser.getName();
                if ("admin".equals(tag2)) {
                    String name = parser.getAttributeValue(null, "name");
                    try {
                        DeviceAdminInfo dai = findAdmin(ComponentName.unflattenFromString(name), userHandle);
                        if (dai != null) {
                            ActiveAdmin ap = new ActiveAdmin(dai);
                            ap.readFromXml(parser);
                            policy.mAdminMap.put(ap.info.getComponent(), ap);
                        }
                    } catch (RuntimeException e13) {
                        Slog.w(LOG_TAG, "Failed loading admin " + name, e13);
                    }
                } else if ("failed-password-attempts".equals(tag2)) {
                    policy.mFailedPasswordAttempts = Integer.parseInt(parser.getAttributeValue(null, "value"));
                    XmlUtils.skipCurrentTag(parser);
                } else if ("password-owner".equals(tag2)) {
                    policy.mPasswordOwner = Integer.parseInt(parser.getAttributeValue(null, "value"));
                    XmlUtils.skipCurrentTag(parser);
                } else if ("active-password".equals(tag2)) {
                    policy.mActivePasswordQuality = Integer.parseInt(parser.getAttributeValue(null, "quality"));
                    policy.mActivePasswordLength = Integer.parseInt(parser.getAttributeValue(null, "length"));
                    policy.mActivePasswordUpperCase = Integer.parseInt(parser.getAttributeValue(null, "uppercase"));
                    policy.mActivePasswordLowerCase = Integer.parseInt(parser.getAttributeValue(null, "lowercase"));
                    policy.mActivePasswordLetters = Integer.parseInt(parser.getAttributeValue(null, "letters"));
                    policy.mActivePasswordNumeric = Integer.parseInt(parser.getAttributeValue(null, "numeric"));
                    policy.mActivePasswordSymbols = Integer.parseInt(parser.getAttributeValue(null, "symbols"));
                    policy.mActivePasswordNonLetter = Integer.parseInt(parser.getAttributeValue(null, "nonletter"));
                    XmlUtils.skipCurrentTag(parser);
                } else if (LOCK_TASK_COMPONENTS_XML.equals(tag2)) {
                    policy.mLockTaskPackages.add(parser.getAttributeValue(null, "name"));
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(LOG_TAG, "Unknown tag: " + tag2);
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        stream2 = stream;
        if (stream2 != null) {
            try {
                stream2.close();
            } catch (IOException e14) {
            }
        }
        policy.mAdminList.addAll(policy.mAdminMap.values());
        LockPatternUtils utils = new LockPatternUtils(this.mContext);
        if (utils.getActivePasswordQuality() < policy.mActivePasswordQuality) {
            Slog.w(LOG_TAG, "Active password quality 0x" + Integer.toHexString(policy.mActivePasswordQuality) + " does not match actual quality 0x" + Integer.toHexString(utils.getActivePasswordQuality()));
            policy.mActivePasswordQuality = 0;
            policy.mActivePasswordLength = 0;
            policy.mActivePasswordUpperCase = 0;
            policy.mActivePasswordLowerCase = 0;
            policy.mActivePasswordLetters = 0;
            policy.mActivePasswordNumeric = 0;
            policy.mActivePasswordSymbols = 0;
            policy.mActivePasswordNonLetter = 0;
        }
        validatePasswordOwnerLocked(policy);
        syncDeviceCapabilitiesLocked(policy);
        updateMaximumTimeToLockLocked(policy);
    }

    static void validateQualityConstant(int quality) {
        switch (quality) {
            case 0:
            case 32768:
            case 65536:
            case 131072:
            case 196608:
            case 262144:
            case 327680:
            case 393216:
                return;
            default:
                throw new IllegalArgumentException("Invalid quality constant: 0x" + Integer.toHexString(quality));
        }
    }

    void validatePasswordOwnerLocked(DevicePolicyData policy) {
        if (policy.mPasswordOwner >= 0) {
            boolean haveOwner = DBG;
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
            if (!haveOwner) {
                Slog.w(LOG_TAG, "Previous password owner " + policy.mPasswordOwner + " no longer active; disabling");
                policy.mPasswordOwner = -1;
            }
        }
    }

    void syncDeviceCapabilitiesLocked(DevicePolicyData policy) {
        boolean systemState = SystemProperties.getBoolean(SYSTEM_PROP_DISABLE_CAMERA, DBG);
        boolean cameraDisabled = getCameraDisabled(null, policy.mUserHandle);
        if (cameraDisabled != systemState) {
            long token = Binder.clearCallingIdentity();
            String value = cameraDisabled ? "1" : "0";
            try {
                SystemProperties.set(SYSTEM_PROP_DISABLE_CAMERA, value);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public void systemReady() {
        if (this.mHasFeature) {
            getUserData(0);
            loadDeviceOwner();
            cleanUpOldUsers();
            new SetupContentObserver(this.mHandler).register(this.mContext.getContentResolver());
            updateUserSetupComplete();
            List<UserInfo> users = this.mUserManager.getUsers(true);
            int N = users.size();
            for (int i = 0; i < N; i++) {
                int userHandle = users.get(i).id;
                updateScreenCaptureDisabledInWindowManager(userHandle, getScreenCaptureDisabled(null, userHandle));
            }
        }
    }

    private void cleanUpOldUsers() {
        Set<Integer> usersWithProfileOwners;
        HashSet hashSet;
        synchronized (this) {
            usersWithProfileOwners = this.mDeviceOwner != null ? this.mDeviceOwner.getProfileOwnerKeys() : new HashSet<>();
            hashSet = new HashSet();
            for (int i = 0; i < this.mUserData.size(); i++) {
                hashSet.add(Integer.valueOf(this.mUserData.keyAt(i)));
            }
        }
        List<UserInfo> allUsers = this.mUserManager.getUsers();
        Set<Integer> deletedUsers = new HashSet<>();
        deletedUsers.addAll(usersWithProfileOwners);
        deletedUsers.addAll(hashSet);
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
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo ui : profiles) {
                int profileUserHandle = ui.id;
                DevicePolicyData policy = getUserData(profileUserHandle);
                int count = policy.mAdminList.size();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        ActiveAdmin admin = policy.mAdminList.get(i);
                        if (admin.info.usesPolicy(6) && admin.passwordExpirationTimeout > 0 && now >= admin.passwordExpirationDate - EXPIRATION_GRACE_PERIOD_MS && admin.passwordExpirationDate > 0) {
                            sendAdminCommandLocked(admin, "android.app.action.ACTION_PASSWORD_EXPIRING");
                        }
                    }
                }
            }
            setExpirationAlarmCheckLocked(this.mContext, getUserData(userHandle));
        }
    }

    private class MonitoringCertNotificationTask extends AsyncTask<Intent, Void, Void> {
        private MonitoringCertNotificationTask() {
        }

        @Override
        protected Void doInBackground(Intent... params) {
            int userHandle = params[0].getIntExtra("android.intent.extra.user_handle", -1);
            if (userHandle == -1) {
                for (UserInfo userInfo : DevicePolicyManagerService.this.mUserManager.getUsers()) {
                    manageNotification(userInfo.getUserHandle());
                }
                return null;
            }
            manageNotification(new UserHandle(userHandle));
            return null;
        }

        private void manageNotification(UserHandle userHandle) {
            String contentText;
            int smallIconId;
            if (DevicePolicyManagerService.this.mUserManager.isUserRunning(userHandle)) {
                boolean hasCert = DevicePolicyManagerService.DBG;
                try {
                    KeyChain.KeyChainConnection kcs = KeyChain.bindAsUser(DevicePolicyManagerService.this.mContext, userHandle);
                    try {
                        try {
                            if (!kcs.getService().getUserCaAliases().getList().isEmpty()) {
                                hasCert = true;
                            }
                            kcs.close();
                        } catch (RemoteException e) {
                            Log.e(DevicePolicyManagerService.LOG_TAG, "Could not connect to KeyChain service", e);
                            kcs.close();
                        }
                    } catch (Throwable th) {
                        kcs.close();
                        throw th;
                    }
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e3) {
                    Log.e(DevicePolicyManagerService.LOG_TAG, "Could not connect to KeyChain service", e3);
                }
                if (!hasCert) {
                    DevicePolicyManagerService.this.getNotificationManager().cancelAsUser(null, 17039592, userHandle);
                    return;
                }
                String ownerName = DevicePolicyManagerService.this.getDeviceOwnerName();
                if (DevicePolicyManagerService.this.isManagedProfile(userHandle.getIdentifier())) {
                    contentText = DevicePolicyManagerService.this.mContext.getString(R.string.accessibility_button_prompt_text);
                    smallIconId = R.drawable.jog_tab_bar_right_end_confirm_red;
                } else if (ownerName != null) {
                    contentText = DevicePolicyManagerService.this.mContext.getString(R.string.accessibility_dialog_button_allow, ownerName);
                    smallIconId = R.drawable.jog_tab_bar_right_end_confirm_red;
                } else {
                    contentText = DevicePolicyManagerService.this.mContext.getString(R.string.accessibility_button_instructional_text);
                    smallIconId = R.drawable.stat_sys_warning;
                }
                Intent dialogIntent = new Intent("com.android.settings.MONITORING_CERT_INFO");
                dialogIntent.setFlags(268468224);
                dialogIntent.setPackage("com.android.settings");
                PendingIntent notifyIntent = PendingIntent.getActivityAsUser(DevicePolicyManagerService.this.mContext, 0, dialogIntent, 134217728, null, userHandle);
                try {
                    Context userContext = DevicePolicyManagerService.this.mContext.createPackageContextAsUser("android", 0, userHandle);
                    Notification noti = new Notification.Builder(userContext).setSmallIcon(smallIconId).setContentTitle(DevicePolicyManagerService.this.mContext.getString(17039592)).setContentText(contentText).setContentIntent(notifyIntent).setPriority(1).setShowWhen(DevicePolicyManagerService.DBG).setColor(DevicePolicyManagerService.this.mContext.getResources().getColor(R.color.system_accent3_600)).build();
                    DevicePolicyManagerService.this.getNotificationManager().notifyAsUser(null, 17039592, noti, userHandle);
                } catch (PackageManager.NameNotFoundException e4) {
                    Log.e(DevicePolicyManagerService.LOG_TAG, "Create context as " + userHandle + " failed", e4);
                }
            }
        }
    }

    public void setActiveAdmin(ComponentName adminReceiver, boolean refreshing, int userHandle) {
        if (this.mHasFeature) {
            setActiveAdmin(adminReceiver, refreshing, userHandle, null);
        }
    }

    private void setActiveAdmin(ComponentName adminReceiver, boolean refreshing, int userHandle, Bundle onEnableData) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
        enforceCrossUserPermission(userHandle);
        DevicePolicyData policy = getUserData(userHandle);
        DeviceAdminInfo info = findAdmin(adminReceiver, userHandle);
        if (info == null) {
            throw new IllegalArgumentException("Bad admin: " + adminReceiver);
        }
        synchronized (this) {
            long ident = Binder.clearCallingIdentity();
            if (!refreshing) {
                try {
                    if (getActiveAdminUncheckedLocked(adminReceiver, userHandle) != null) {
                        throw new IllegalArgumentException("Admin is already added");
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            ActiveAdmin newAdmin = new ActiveAdmin(info);
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
        boolean z = DBG;
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (getActiveAdminUncheckedLocked(adminReceiver, userHandle) != null) {
                    z = true;
                }
            }
        }
        return z;
    }

    public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) {
        boolean zContains;
        if (!this.mHasFeature) {
            return DBG;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            DevicePolicyData policyData = getUserData(userHandle);
            zContains = policyData.mRemovingAdmins.contains(adminReceiver);
        }
        return zContains;
    }

    public boolean hasGrantedPolicy(ComponentName adminReceiver, int policyId, int userHandle) {
        boolean zUsesPolicy;
        if (!this.mHasFeature) {
            return DBG;
        }
        enforceCrossUserPermission(userHandle);
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
        enforceCrossUserPermission(userHandle);
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
        boolean z;
        if (!this.mHasFeature) {
            return DBG;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            int N = policy.mAdminList.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    z = false;
                    break;
                }
                if (!policy.mAdminList.get(i).info.getPackageName().equals(packageName)) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
        }
        return z;
    }

    public void removeActiveAdmin(ComponentName adminReceiver, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(adminReceiver, userHandle);
                if (admin != null) {
                    if (admin.getUid() != Binder.getCallingUid()) {
                        if (!isDeviceOwner(adminReceiver.getPackageName())) {
                            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_DEVICE_ADMINS", null);
                        } else {
                            return;
                        }
                    }
                    long ident = Binder.clearCallingIdentity();
                    try {
                        removeActiveAdminLocked(adminReceiver, userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    public void setPasswordQuality(ComponentName who, int quality, int userHandle) {
        if (this.mHasFeature) {
            validateQualityConstant(quality);
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.passwordQuality != quality) {
                    ap.passwordQuality = quality;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordQuality(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int mode = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int mode2 = admin != null ? admin.passwordQuality : 0;
                return mode2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (mode < admin2.passwordQuality) {
                        mode = admin2.passwordQuality;
                    }
                }
            }
            return mode;
        }
    }

    public void setPasswordMinimumLength(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.minimumPasswordLength != length) {
                    ap.minimumPasswordLength = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordMinimumLength(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordLength : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordLength) {
                        length = admin2.minimumPasswordLength;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordHistoryLength(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.passwordHistoryLength != length) {
                    ap.passwordHistoryLength = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordHistoryLength(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.passwordHistoryLength : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.passwordHistoryLength) {
                        length = admin2.passwordHistoryLength;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordExpirationTimeout(ComponentName who, long timeout, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                if (timeout < 0) {
                    throw new IllegalArgumentException("Timeout must be >= 0 ms");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 6);
                long expiration = timeout > 0 ? timeout + System.currentTimeMillis() : 0L;
                ap.passwordExpirationDate = expiration;
                ap.passwordExpirationTimeout = timeout;
                if (timeout > 0) {
                    Slog.w(LOG_TAG, "setPasswordExpiration(): password will expire on " + DateFormat.getDateTimeInstance(2, 2).format(new Date(expiration)));
                }
                saveSettingsLocked(userHandle);
                setExpirationAlarmCheckLocked(this.mContext, getUserData(userHandle));
            }
        }
    }

    public long getPasswordExpirationTimeout(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            long timeout = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                long timeout2 = admin != null ? admin.passwordExpirationTimeout : 0L;
                return timeout2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (timeout == 0 || (admin2.passwordExpirationTimeout != 0 && timeout > admin2.passwordExpirationTimeout)) {
                        timeout = admin2.passwordExpirationTimeout;
                    }
                }
            }
            return timeout;
        }
    }

    public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) throws Throwable {
        int userId = UserHandle.getCallingUserId();
        List<String> changedProviders = null;
        synchronized (this) {
            try {
                ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
                if (activeAdmin.crossProfileWidgetProviders == null) {
                    activeAdmin.crossProfileWidgetProviders = new ArrayList();
                }
                List<String> providers = activeAdmin.crossProfileWidgetProviders;
                if (!providers.contains(packageName)) {
                    providers.add(packageName);
                    List<String> changedProviders2 = new ArrayList<>(providers);
                    try {
                        saveSettingsLocked(userId);
                        changedProviders = changedProviders2;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (changedProviders == null) {
                    return DBG;
                }
                this.mLocalService.notifyCrossProfileProvidersChanged(userId, changedProviders);
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) throws Throwable {
        int userId = UserHandle.getCallingUserId();
        List<String> changedProviders = null;
        synchronized (this) {
            try {
                ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
                if (activeAdmin.crossProfileWidgetProviders == null) {
                    return DBG;
                }
                List<String> providers = activeAdmin.crossProfileWidgetProviders;
                if (providers.remove(packageName)) {
                    List<String> changedProviders2 = new ArrayList<>(providers);
                    try {
                        saveSettingsLocked(userId);
                        changedProviders = changedProviders2;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (changedProviders == null) {
                    return DBG;
                }
                this.mLocalService.notifyCrossProfileProvidersChanged(userId, changedProviders);
                return true;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public List<String> getCrossProfileWidgetProviders(ComponentName admin) {
        List<String> arrayList;
        synchronized (this) {
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(admin, -1);
            if (activeAdmin.crossProfileWidgetProviders == null || activeAdmin.crossProfileWidgetProviders.isEmpty()) {
                arrayList = null;
            } else if (Binder.getCallingUid() == Process.myUid()) {
                arrayList = new ArrayList<>(activeAdmin.crossProfileWidgetProviders);
            } else {
                arrayList = activeAdmin.crossProfileWidgetProviders;
            }
        }
        return arrayList;
    }

    private long getPasswordExpirationLocked(ComponentName who, int userHandle) {
        long timeout = 0;
        if (who != null) {
            ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
            if (admin != null) {
                return admin.passwordExpirationDate;
            }
            return 0L;
        }
        List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
        for (UserInfo userInfo : profiles) {
            DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
            int N = policy.mAdminList.size();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin2 = policy.mAdminList.get(i);
                if (timeout == 0 || (admin2.passwordExpirationDate != 0 && timeout > admin2.passwordExpirationDate)) {
                    timeout = admin2.passwordExpirationDate;
                }
            }
        }
        return timeout;
    }

    public long getPasswordExpiration(ComponentName who, int userHandle) {
        long passwordExpirationLocked;
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            passwordExpirationLocked = getPasswordExpirationLocked(who, userHandle);
        }
        return passwordExpirationLocked;
    }

    public void setPasswordMinimumUpperCase(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.minimumPasswordUpperCase != length) {
                    ap.minimumPasswordUpperCase = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordMinimumUpperCase(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordUpperCase : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordUpperCase) {
                        length = admin2.minimumPasswordUpperCase;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumLowerCase(ComponentName who, int length, int userHandle) {
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
            if (ap.minimumPasswordLowerCase != length) {
                ap.minimumPasswordLowerCase = length;
                saveSettingsLocked(userHandle);
            }
        }
    }

    public int getPasswordMinimumLowerCase(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordLowerCase : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordLowerCase) {
                        length = admin2.minimumPasswordLowerCase;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumLetters(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.minimumPasswordLetters != length) {
                    ap.minimumPasswordLetters = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordMinimumLetters(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordLetters : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordLetters) {
                        length = admin2.minimumPasswordLetters;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumNumeric(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.minimumPasswordNumeric != length) {
                    ap.minimumPasswordNumeric = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordMinimumNumeric(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordNumeric : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordNumeric) {
                        length = admin2.minimumPasswordNumeric;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumSymbols(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.minimumPasswordSymbols != length) {
                    ap.minimumPasswordSymbols = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordMinimumSymbols(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordSymbols : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordSymbols) {
                        length = admin2.minimumPasswordSymbols;
                    }
                }
            }
            return length;
        }
    }

    public void setPasswordMinimumNonLetter(ComponentName who, int length, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 0);
                if (ap.minimumPasswordNonLetter != length) {
                    ap.minimumPasswordNonLetter = length;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getPasswordMinimumNonLetter(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            int length = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                int length2 = admin != null ? admin.minimumPasswordNonLetter : 0;
                return length2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (length < admin2.minimumPasswordNonLetter) {
                        length = admin2.minimumPasswordNonLetter;
                    }
                }
            }
            return length;
        }
    }

    public boolean isActivePasswordSufficient(int userHandle) {
        if (!this.mHasFeature) {
            return true;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            UserInfo parent = getProfileParent(userHandle);
            int id = parent == null ? userHandle : parent.id;
            DevicePolicyData policy = getUserDataUnchecked(id);
            getActiveAdminForCallerLocked(null, 0);
            if (policy.mActivePasswordQuality < getPasswordQuality(null, userHandle) || policy.mActivePasswordLength < getPasswordMinimumLength(null, userHandle)) {
                return DBG;
            }
            if (policy.mActivePasswordQuality != 393216) {
                return true;
            }
            return policy.mActivePasswordUpperCase >= getPasswordMinimumUpperCase(null, userHandle) && policy.mActivePasswordLowerCase >= getPasswordMinimumLowerCase(null, userHandle) && policy.mActivePasswordLetters >= getPasswordMinimumLetters(null, userHandle) && policy.mActivePasswordNumeric >= getPasswordMinimumNumeric(null, userHandle) && policy.mActivePasswordSymbols >= getPasswordMinimumSymbols(null, userHandle) && policy.mActivePasswordNonLetter >= getPasswordMinimumNonLetter(null, userHandle);
        }
    }

    public int getCurrentFailedPasswordAttempts(int userHandle) {
        int i;
        synchronized (this) {
            getActiveAdminForCallerLocked(null, 1);
            UserInfo parent = getProfileParent(userHandle);
            int id = parent == null ? userHandle : parent.id;
            DevicePolicyData policy = getUserDataUnchecked(id);
            i = policy.mFailedPasswordAttempts;
        }
        return i;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName who, int num, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                getActiveAdminForCallerLocked(who, 4);
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 1);
                if (ap.maximumFailedPasswordsForWipe != num) {
                    ap.maximumFailedPasswordsForWipe = num;
                    saveSettingsLocked(userHandle);
                }
            }
        }
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName who, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                ActiveAdmin admin = who != null ? getActiveAdminUncheckedLocked(who, userHandle) : getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle);
                i = admin != null ? admin.maximumFailedPasswordsForWipe : 0;
            }
        }
        return i;
    }

    public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                ActiveAdmin admin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle);
                identifier = admin != null ? admin.getUserHandle().getIdentifier() : -10000;
            }
        }
        return identifier;
    }

    private ActiveAdmin getAdminWithMinimumFailedPasswordsForWipeLocked(int userHandle) {
        int count = 0;
        ActiveAdmin strictestAdmin = null;
        for (UserInfo userInfo : this.mUserManager.getProfiles(userHandle)) {
            DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
            for (ActiveAdmin admin : policy.mAdminList) {
                if (admin.maximumFailedPasswordsForWipe != 0 && (count == 0 || count > admin.maximumFailedPasswordsForWipe || (userInfo.isPrimary() && count >= admin.maximumFailedPasswordsForWipe))) {
                    count = admin.maximumFailedPasswordsForWipe;
                    strictestAdmin = admin;
                }
            }
        }
        return strictestAdmin;
    }

    public boolean resetPassword(String passwordOrNull, int flags, int userHandle) {
        if (!this.mHasFeature) {
            return DBG;
        }
        enforceCrossUserPermission(userHandle);
        enforceNotManagedProfile(userHandle, "reset the password");
        String password = passwordOrNull != null ? passwordOrNull : "";
        synchronized (this) {
            getActiveAdminForCallerLocked(null, 2);
            int quality = getPasswordQuality(null, userHandle);
            if (quality != 0) {
                int realQuality = LockPatternUtils.computePasswordQuality(password);
                if (realQuality < quality && quality != 393216) {
                    Slog.w(LOG_TAG, "resetPassword: password quality 0x" + Integer.toHexString(realQuality) + " does not meet required quality 0x" + Integer.toHexString(quality));
                    return DBG;
                }
                quality = Math.max(realQuality, quality);
            }
            int length = getPasswordMinimumLength(null, userHandle);
            if (password.length() < length) {
                Slog.w(LOG_TAG, "resetPassword: password length " + password.length() + " does not meet required length " + length);
                return DBG;
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
                    } else if (c >= '0' && c <= '9') {
                        numbers++;
                        nonletter++;
                    } else {
                        symbols++;
                        nonletter++;
                    }
                }
                int neededLetters = getPasswordMinimumLetters(null, userHandle);
                if (letters < neededLetters) {
                    Slog.w(LOG_TAG, "resetPassword: number of letters " + letters + " does not meet required number of letters " + neededLetters);
                    return DBG;
                }
                int neededNumbers = getPasswordMinimumNumeric(null, userHandle);
                if (numbers < neededNumbers) {
                    Slog.w(LOG_TAG, "resetPassword: number of numerical digits " + numbers + " does not meet required number of numerical digits " + neededNumbers);
                    return DBG;
                }
                int neededLowerCase = getPasswordMinimumLowerCase(null, userHandle);
                if (lowercase < neededLowerCase) {
                    Slog.w(LOG_TAG, "resetPassword: number of lowercase letters " + lowercase + " does not meet required number of lowercase letters " + neededLowerCase);
                    return DBG;
                }
                int neededUpperCase = getPasswordMinimumUpperCase(null, userHandle);
                if (uppercase < neededUpperCase) {
                    Slog.w(LOG_TAG, "resetPassword: number of uppercase letters " + uppercase + " does not meet required number of uppercase letters " + neededUpperCase);
                    return DBG;
                }
                int neededSymbols = getPasswordMinimumSymbols(null, userHandle);
                if (symbols < neededSymbols) {
                    Slog.w(LOG_TAG, "resetPassword: number of special symbols " + symbols + " does not meet required number of special symbols " + neededSymbols);
                    return DBG;
                }
                int neededNonLetter = getPasswordMinimumNonLetter(null, userHandle);
                if (nonletter < neededNonLetter) {
                    Slog.w(LOG_TAG, "resetPassword: number of non-letter characters " + nonletter + " does not meet required number of non-letter characters " + neededNonLetter);
                    return DBG;
                }
            }
            int callingUid = Binder.getCallingUid();
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mPasswordOwner >= 0 && policy.mPasswordOwner != callingUid) {
                Slog.w(LOG_TAG, "resetPassword: already set by another uid and not entered by user");
                return DBG;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                LockPatternUtils utils = new LockPatternUtils(this.mContext);
                if (!TextUtils.isEmpty(password)) {
                    utils.saveLockPassword(password, quality, DBG, userHandle);
                } else {
                    utils.clearLock(DBG, userHandle);
                }
                boolean requireEntry = (flags & 1) != 0 ? true : DBG;
                if (requireEntry) {
                    utils.requireCredentialEntry(-1);
                }
                synchronized (this) {
                    int newOwner = requireEntry ? callingUid : -1;
                    if (policy.mPasswordOwner != newOwner) {
                        policy.mPasswordOwner = newOwner;
                        saveSettingsLocked(userHandle);
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    public void setMaximumTimeToLock(ComponentName who, long timeMs, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 3);
                if (ap.maximumTimeToUnlock != timeMs) {
                    ap.maximumTimeToUnlock = timeMs;
                    saveSettingsLocked(userHandle);
                    updateMaximumTimeToLockLocked(getUserData(userHandle));
                }
            }
        }
    }

    void updateMaximumTimeToLockLocked(DevicePolicyData policy) {
        long timeMs = getMaximumTimeToLock(null, policy.mUserHandle);
        if (policy.mLastMaximumTimeToLock != timeMs) {
            long ident = Binder.clearCallingIdentity();
            if (timeMs <= 0) {
                timeMs = 2147483647L;
            } else {
                try {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", 0);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
            policy.mLastMaximumTimeToLock = timeMs;
            this.mPowerManagerInternal.setMaximumScreenOffTimeoutFromDeviceAdmin((int) timeMs);
        }
    }

    public long getMaximumTimeToLock(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return 0L;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            long time = 0;
            if (who != null) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                long time2 = admin != null ? admin.maximumTimeToUnlock : 0L;
                return time2;
            }
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin2 = policy.mAdminList.get(i);
                    if (time == 0) {
                        time = admin2.maximumTimeToUnlock;
                    } else if (admin2.maximumTimeToUnlock != 0 && time > admin2.maximumTimeToUnlock) {
                        time = admin2.maximumTimeToUnlock;
                    }
                }
            }
            return time;
        }
    }

    public void lockNow() {
        if (this.mHasFeature) {
            synchronized (this) {
                getActiveAdminForCallerLocked(null, 3);
                lockNowUnchecked();
            }
        }
    }

    private void lockNowUnchecked() {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 1, 0);
            new LockPatternUtils(this.mContext).requireCredentialEntry(-1);
            getWindowManager().lockNow((Bundle) null);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isExtStorageEncrypted() {
        String state = SystemProperties.get("vold.decrypt");
        if ("".equals(state)) {
            return DBG;
        }
        return true;
    }

    public void enforceCanManageCaCerts(ComponentName who) {
        if (who == null) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CA_CERTIFICATES", null);
        } else {
            synchronized (this) {
                getActiveAdminForCallerLocked(who, -1);
            }
        }
    }

    public boolean installCaCert(ComponentName admin, byte[] certBuffer) throws RemoteException {
        enforceCanManageCaCerts(admin);
        try {
            X509Certificate cert = parseCert(certBuffer);
            byte[] pemCert = Credentials.convertToPem(new Certificate[]{cert});
            UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
            long id = Binder.clearCallingIdentity();
            try {
                KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
                try {
                    keyChainConnection.getService().installCaCertificate(pemCert);
                    return true;
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "installCaCertsToKeyChain(): ", e);
                    return DBG;
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e1) {
                Log.w(LOG_TAG, "installCaCertsToKeyChain(): ", e1);
                Thread.currentThread().interrupt();
            } finally {
                Binder.restoreCallingIdentity(id);
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Problem reading cert", ioe);
            return DBG;
        } catch (CertificateException ce) {
            Log.e(LOG_TAG, "Problem converting cert", ce);
            return DBG;
        }
    }

    private static X509Certificate parseCert(byte[] certBuffer) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBuffer));
    }

    public void uninstallCaCert(ComponentName admin, String alias) {
        enforceCanManageCaCerts(admin);
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        long id = Binder.clearCallingIdentity();
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
            try {
                try {
                    keyChainConnection.getService().deleteCaCertificate(alias);
                    keyChainConnection.close();
                } catch (Throwable th) {
                    keyChainConnection.close();
                    throw th;
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "from CaCertUninstaller: ", e);
                keyChainConnection.close();
            }
        } catch (InterruptedException ie) {
            Log.w(LOG_TAG, "CaCertUninstaller: ", ie);
            Thread.currentThread().interrupt();
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    public boolean installKeyPair(ComponentName who, byte[] privKey, byte[] cert, String alias) {
        if (who == null) {
            throw new NullPointerException("ComponentName is null");
        }
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -1);
        }
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        long id = Binder.clearCallingIdentity();
        try {
            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, userHandle);
            try {
                IKeyChainService keyChain = keyChainConnection.getService();
                return keyChain.installKeyPair(privKey, cert, alias);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Installing certificate", e);
                return DBG;
            } finally {
                keyChainConnection.close();
            }
        } catch (InterruptedException e2) {
            Log.w(LOG_TAG, "Interrupted while installing certificate", e2);
            Thread.currentThread().interrupt();
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    private void wipeDataNoLock(boolean wipeExtRequested, String reason) {
        boolean forceExtWipe = (Environment.isExternalStorageRemovable() || !isExtStorageEncrypted()) ? DBG : true;
        if ((forceExtWipe || wipeExtRequested) && !Environment.isExternalStorageEmulated()) {
            Intent intent = new Intent("com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET");
            intent.putExtra("always_reset", true);
            intent.putExtra("android.intent.extra.REASON", reason);
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            this.mWakeLock.acquire(10000L);
            this.mContext.startService(intent);
            return;
        }
        try {
            RecoverySystem.rebootWipeUserData(this.mContext, reason);
        } catch (IOException | SecurityException e) {
            Slog.w(LOG_TAG, "Failed requesting data wipe", e);
        }
    }

    public void wipeData(int flags, int userHandle) {
        String source;
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                ActiveAdmin admin = getActiveAdminForCallerLocked(null, 4);
                ComponentName cname = admin.info.getComponent();
                if (cname != null) {
                    source = cname.flattenToShortString();
                } else {
                    source = admin.info.getPackageName();
                }
                long ident = Binder.clearCallingIdentity();
                if ((flags & 2) != 0) {
                    if (userHandle == 0) {
                        try {
                            if (isDeviceOwner(admin.info.getPackageName())) {
                                PersistentDataBlockManager manager = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
                                if (manager != null) {
                                    manager.wipe();
                                }
                            }
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    }
                    throw new SecurityException("Only device owner admins can set WIPE_RESET_PROTECTION_DATA");
                }
            }
            boolean wipeExtRequested = (flags & 1) != 0 ? true : DBG;
            wipeDeviceNoLock(wipeExtRequested, userHandle, "DevicePolicyManager.wipeData() from " + source);
        }
    }

    private void wipeDeviceNoLock(boolean wipeExtRequested, final int userHandle, String reason) {
        long iden = Binder.clearCallingIdentity();
        try {
            if (userHandle == 0) {
                wipeDataNoLock(wipeExtRequested, reason);
            } else {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IActivityManager am = ActivityManagerNative.getDefault();
                            if (am.getCurrentUser().id == userHandle) {
                                am.switchUser(0);
                            }
                            if (!DevicePolicyManagerService.this.mUserManager.removeUser(userHandle)) {
                                Slog.w(DevicePolicyManagerService.LOG_TAG, "Couldn't remove user " + userHandle);
                            }
                        } catch (RemoteException e) {
                        }
                    }
                });
            }
        } finally {
            Binder.restoreCallingIdentity(iden);
        }
    }

    public void getRemoveWarning(ComponentName comp, final RemoteCallback result, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            synchronized (this) {
                ActiveAdmin admin = getActiveAdminUncheckedLocked(comp, userHandle);
                if (admin == null) {
                    try {
                        result.sendResult((Bundle) null);
                    } catch (RemoteException e) {
                    }
                    return;
                }
                Intent intent = new Intent("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
                intent.setFlags(268435456);
                intent.setComponent(admin.info.getComponent());
                this.mContext.sendOrderedBroadcastAsUser(intent, new UserHandle(userHandle), null, new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent2) {
                        try {
                            result.sendResult(getResultExtras(DevicePolicyManagerService.DBG));
                        } catch (RemoteException e2) {
                        }
                    }
                }, null, -1, null, null);
            }
        }
    }

    public void setActivePasswordState(int quality, int length, int letters, int uppercase, int lowercase, int numbers, int symbols, int nonletter, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            enforceNotManagedProfile(userHandle, "set the active password");
            this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
            DevicePolicyData p = getUserData(userHandle);
            validateQualityConstant(quality);
            synchronized (this) {
                if (p.mActivePasswordQuality != quality || p.mActivePasswordLength != length || p.mFailedPasswordAttempts != 0 || p.mActivePasswordLetters != letters || p.mActivePasswordUpperCase != uppercase || p.mActivePasswordLowerCase != lowercase || p.mActivePasswordNumeric != numbers || p.mActivePasswordSymbols != symbols || p.mActivePasswordNonLetter != nonletter) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        p.mActivePasswordQuality = quality;
                        p.mActivePasswordLength = length;
                        p.mActivePasswordLetters = letters;
                        p.mActivePasswordLowerCase = lowercase;
                        p.mActivePasswordUpperCase = uppercase;
                        p.mActivePasswordNumeric = numbers;
                        p.mActivePasswordSymbols = symbols;
                        p.mActivePasswordNonLetter = nonletter;
                        p.mFailedPasswordAttempts = 0;
                        saveSettingsLocked(userHandle);
                        updatePasswordExpirationsLocked(userHandle);
                        setExpirationAlarmCheckLocked(this.mContext, p);
                        sendAdminCommandToSelfAndProfilesLocked("android.app.action.ACTION_PASSWORD_CHANGED", 0, userHandle);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    private void updatePasswordExpirationsLocked(int userHandle) {
        List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
        for (UserInfo userInfo : profiles) {
            int profileId = userInfo.id;
            DevicePolicyData policy = getUserDataUnchecked(profileId);
            int N = policy.mAdminList.size();
            if (N > 0) {
                for (int i = 0; i < N; i++) {
                    ActiveAdmin admin = policy.mAdminList.get(i);
                    if (admin.info.usesPolicy(6)) {
                        long timeout = admin.passwordExpirationTimeout;
                        long expiration = timeout > 0 ? timeout + System.currentTimeMillis() : 0L;
                        admin.passwordExpirationDate = expiration;
                    }
                }
            }
            saveSettingsLocked(profileId);
        }
    }

    public void reportFailedPasswordAttempt(int userHandle) {
        enforceCrossUserPermission(userHandle);
        enforceNotManagedProfile(userHandle, "report failed password attempt");
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        long ident = Binder.clearCallingIdentity();
        boolean wipeData = DBG;
        int identifier = 0;
        try {
            synchronized (this) {
                DevicePolicyData policy = getUserData(userHandle);
                policy.mFailedPasswordAttempts++;
                saveSettingsLocked(userHandle);
                if (this.mHasFeature) {
                    ActiveAdmin strictestAdmin = getAdminWithMinimumFailedPasswordsForWipeLocked(userHandle);
                    int max = strictestAdmin != null ? strictestAdmin.maximumFailedPasswordsForWipe : 0;
                    if (max > 0 && policy.mFailedPasswordAttempts >= max) {
                        wipeData = true;
                        identifier = strictestAdmin.getUserHandle().getIdentifier();
                    }
                    sendAdminCommandToSelfAndProfilesLocked("android.app.action.ACTION_PASSWORD_FAILED", 1, userHandle);
                }
            }
            if (wipeData) {
                wipeDeviceNoLock(DBG, identifier, "reportFailedPasswordAttempt()");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void reportSuccessfulPasswordAttempt(int userHandle) {
        enforceCrossUserPermission(userHandle);
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            if (policy.mFailedPasswordAttempts != 0 || policy.mPasswordOwner >= 0) {
                long ident = Binder.clearCallingIdentity();
                try {
                    policy.mFailedPasswordAttempts = 0;
                    policy.mPasswordOwner = -1;
                    saveSettingsLocked(userHandle);
                    if (this.mHasFeature) {
                        sendAdminCommandToSelfAndProfilesLocked("android.app.action.ACTION_PASSWORD_SUCCEEDED", 1, userHandle);
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    public ComponentName setGlobalProxy(ComponentName who, String proxySpec, String exclusionList, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceCrossUserPermission(userHandle);
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
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
                Slog.w(LOG_TAG, "Only the owner is allowed to set the global proxy. User " + userHandle + " is not permitted.");
                return null;
            }
            if (proxySpec == null) {
                admin.specifiesGlobalProxy = DBG;
                admin.globalProxySpec = null;
                admin.globalProxyExclusionList = null;
            } else {
                admin.specifiesGlobalProxy = true;
                admin.globalProxySpec = proxySpec;
                admin.globalProxyExclusionList = exclusionList;
            }
            long origId = Binder.clearCallingIdentity();
            try {
                resetGlobalProxyLocked(policy);
                Binder.restoreCallingIdentity(origId);
                return null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
                throw th;
            }
        }
    }

    public ComponentName getGlobalProxyAdmin(int userHandle) {
        ComponentName component = null;
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                DevicePolicyData policy = getUserData(0);
                int N = policy.mAdminList.size();
                int i = 0;
                while (true) {
                    if (i >= N) {
                        break;
                    }
                    ActiveAdmin ap = policy.mAdminList.get(i);
                    if (!ap.specifiesGlobalProxy) {
                        i++;
                    } else {
                        component = ap.info.getComponent();
                        break;
                    }
                }
            }
        }
        return component;
    }

    public void setRecommendedGlobalProxy(ComponentName who, ProxyInfo proxyInfo) {
        synchronized (this) {
            getActiveAdminForCallerLocked(who, -2);
        }
        long token = Binder.clearCallingIdentity();
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            connectivityManager.setGlobalProxy(proxyInfo);
        } finally {
            Binder.restoreCallingIdentity(token);
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
        ContentResolver res = this.mContext.getContentResolver();
        ProxyInfo proxyProperties = new ProxyInfo(data[0], proxyPort, exclusionList2);
        if (!proxyProperties.isValid()) {
            Slog.e(LOG_TAG, "Invalid proxy properties, ignoring: " + proxyProperties.toString());
            return;
        }
        Settings.Global.putString(res, "global_http_proxy_host", data[0]);
        Settings.Global.putInt(res, "global_http_proxy_port", proxyPort);
        Settings.Global.putString(res, "global_http_proxy_exclusion_list", exclusionList2);
    }

    public int setStorageEncryption(ComponentName who, boolean encrypt, int userHandle) {
        int i = 0;
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                if (userHandle != 0 || UserHandle.getCallingUserId() != 0) {
                    Slog.w(LOG_TAG, "Only owner is allowed to set storage encryption. User " + UserHandle.getCallingUserId() + " is not permitted.");
                } else {
                    ActiveAdmin ap = getActiveAdminForCallerLocked(who, 7);
                    if (isEncryptionSupported()) {
                        if (ap.encryptionRequested != encrypt) {
                            ap.encryptionRequested = encrypt;
                            saveSettingsLocked(userHandle);
                        }
                        DevicePolicyData policy = getUserData(0);
                        boolean newRequested = DBG;
                        int N = policy.mAdminList.size();
                        for (int i2 = 0; i2 < N; i2++) {
                            newRequested |= policy.mAdminList.get(i2).encryptionRequested;
                        }
                        setEncryptionRequested(newRequested);
                        i = newRequested ? 3 : 1;
                    }
                }
            }
        }
        return i;
    }

    public boolean getStorageEncryption(ComponentName who, int userHandle) {
        if (!this.mHasFeature) {
            return DBG;
        }
        enforceCrossUserPermission(userHandle);
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
            return DBG;
        }
    }

    public int getStorageEncryptionStatus(int userHandle) {
        if (!this.mHasFeature) {
        }
        enforceCrossUserPermission(userHandle);
        return getEncryptionStatus();
    }

    private boolean isEncryptionSupported() {
        if (getEncryptionStatus() != 0) {
            return true;
        }
        return DBG;
    }

    private int getEncryptionStatus() {
        String status = SystemProperties.get("ro.crypto.state", "unsupported");
        if (!"encrypted".equalsIgnoreCase(status)) {
            return !"unencrypted".equalsIgnoreCase(status) ? 0 : 1;
        }
        long token = Binder.clearCallingIdentity();
        try {
            return LockPatternUtils.isDeviceEncrypted() ? 3 : 1;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void setEncryptionRequested(boolean encrypt) {
    }

    public void setScreenCaptureDisabled(ComponentName who, int userHandle, boolean disabled) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1);
                if (ap.disableScreenCapture != disabled) {
                    ap.disableScreenCapture = disabled;
                    saveSettingsLocked(userHandle);
                    updateScreenCaptureDisabledInWindowManager(userHandle, disabled);
                }
            }
        }
    }

    public boolean getScreenCaptureDisabled(ComponentName who, int userHandle) {
        boolean z = DBG;
        if (this.mHasFeature) {
            synchronized (this) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                    if (admin != null) {
                        z = admin.disableScreenCapture;
                    }
                } else {
                    DevicePolicyData policy = getUserData(userHandle);
                    int N = policy.mAdminList.size();
                    int i = 0;
                    while (true) {
                        if (i >= N) {
                            break;
                        }
                        if (!policy.mAdminList.get(i).disableScreenCapture) {
                            i++;
                        } else {
                            z = true;
                            break;
                        }
                    }
                }
            }
        }
        return z;
    }

    private void updateScreenCaptureDisabledInWindowManager(int userHandle, boolean disabled) {
        long ident = Binder.clearCallingIdentity();
        try {
            getWindowManager().setScreenCaptureDisabled(userHandle, disabled);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Unable to notify WindowManager.", e);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setAutoTimeRequired(ComponentName who, int userHandle, boolean required) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -2);
                if (admin.requireAutoTime != required) {
                    admin.requireAutoTime = required;
                    saveSettingsLocked(userHandle);
                }
            }
            if (required) {
                long ident = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "auto_time", 1);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    public boolean getAutoTimeRequired() {
        boolean z = DBG;
        if (this.mHasFeature) {
            synchronized (this) {
                ActiveAdmin deviceOwner = getDeviceOwnerAdmin();
                if (deviceOwner != null) {
                    z = deviceOwner.requireAutoTime;
                }
            }
        }
        return z;
    }

    public void setCameraDisabled(ComponentName who, boolean disabled, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 8);
                if (ap.disableCamera != disabled) {
                    ap.disableCamera = disabled;
                    saveSettingsLocked(userHandle);
                }
                syncDeviceCapabilitiesLocked(getUserData(userHandle));
            }
        }
    }

    public boolean getCameraDisabled(ComponentName who, int userHandle) {
        boolean z = DBG;
        if (this.mHasFeature) {
            synchronized (this) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                    if (admin != null) {
                        z = admin.disableCamera;
                    }
                } else {
                    DevicePolicyData policy = getUserData(userHandle);
                    int N = policy.mAdminList.size();
                    int i = 0;
                    while (true) {
                        if (i >= N) {
                            break;
                        }
                        if (!policy.mAdminList.get(i).disableCamera) {
                            i++;
                        } else {
                            z = true;
                            break;
                        }
                    }
                }
            }
        }
        return z;
    }

    public void setKeyguardDisabledFeatures(ComponentName who, int which, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            enforceNotManagedProfile(userHandle, "disable keyguard features");
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, 9);
                if (ap.disabledKeyguardFeatures != which) {
                    ap.disabledKeyguardFeatures = which;
                    saveSettingsLocked(userHandle);
                }
                syncDeviceCapabilitiesLocked(getUserData(userHandle));
            }
        }
    }

    public int getKeyguardDisabledFeatures(ComponentName who, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            synchronized (this) {
                if (who != null) {
                    ActiveAdmin admin = getActiveAdminUncheckedLocked(who, userHandle);
                    which = admin != null ? admin.disabledKeyguardFeatures : 0;
                } else {
                    DevicePolicyData policy = getUserData(userHandle);
                    int N = policy.mAdminList.size();
                    which = 0;
                    for (int i = 0; i < N; i++) {
                        which |= policy.mAdminList.get(i).disabledKeyguardFeatures;
                    }
                }
            }
        }
        return which;
    }

    public boolean setDeviceOwner(String packageName, String ownerName) {
        if (!this.mHasFeature) {
            return DBG;
        }
        if (packageName == null || !DeviceOwner.isInstalled(packageName, this.mContext.getPackageManager())) {
            throw new IllegalArgumentException("Invalid package name " + packageName + " for device owner");
        }
        synchronized (this) {
            if (!allowedToSetDeviceOwnerOnDevice()) {
                throw new IllegalStateException("Trying to set device owner but device is already provisioned.");
            }
            if (this.mDeviceOwner != null && this.mDeviceOwner.hasDeviceOwner()) {
                throw new IllegalStateException("Trying to set device owner but device owner is already set.");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    IBackupManager ibm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    ibm.setBackupServiceActive(0, DBG);
                    Binder.restoreCallingIdentity(ident);
                    if (this.mDeviceOwner == null) {
                        this.mDeviceOwner = DeviceOwner.createWithDeviceOwner(packageName, ownerName);
                        this.mDeviceOwner.writeOwnerFile();
                        return true;
                    }
                    this.mDeviceOwner.setDeviceOwner(packageName, ownerName);
                    this.mDeviceOwner.writeOwnerFile();
                    return true;
                } catch (RemoteException e) {
                    throw new IllegalStateException("Failed deactivating backup service.", e);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    public boolean isDeviceOwner(String packageName) {
        boolean z = DBG;
        if (this.mHasFeature) {
            synchronized (this) {
                if (this.mDeviceOwner != null && this.mDeviceOwner.hasDeviceOwner() && this.mDeviceOwner.getDeviceOwnerPackageName().equals(packageName)) {
                    z = true;
                }
            }
        }
        return z;
    }

    public String getDeviceOwner() {
        String deviceOwnerPackageName = null;
        if (this.mHasFeature) {
            synchronized (this) {
                if (this.mDeviceOwner != null && this.mDeviceOwner.hasDeviceOwner()) {
                    deviceOwnerPackageName = this.mDeviceOwner.getDeviceOwnerPackageName();
                }
            }
        }
        return deviceOwnerPackageName;
    }

    public String getDeviceOwnerName() {
        String deviceOwnerName = null;
        if (this.mHasFeature) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
            synchronized (this) {
                if (this.mDeviceOwner != null) {
                    deviceOwnerName = this.mDeviceOwner.getDeviceOwnerName();
                }
            }
        }
        return deviceOwnerName;
    }

    private ActiveAdmin getDeviceOwnerAdmin() {
        String deviceOwnerPackageName = getDeviceOwner();
        if (deviceOwnerPackageName == null) {
            return null;
        }
        DevicePolicyData policy = getUserData(0);
        int n = policy.mAdminList.size();
        for (int i = 0; i < n; i++) {
            ActiveAdmin admin = policy.mAdminList.get(i);
            if (deviceOwnerPackageName.equals(admin.info.getPackageName())) {
                return admin;
            }
        }
        return null;
    }

    public void clearDeviceOwner(String packageName) {
        if (packageName == null) {
            throw new NullPointerException("packageName is null");
        }
        try {
            int uid = this.mContext.getPackageManager().getPackageUid(packageName, 0);
            if (uid != Binder.getCallingUid()) {
                throw new SecurityException("Invalid packageName");
            }
            if (!isDeviceOwner(packageName)) {
                throw new SecurityException("clearDeviceOwner can only be called by the device owner");
            }
            synchronized (this) {
                long ident = Binder.clearCallingIdentity();
                try {
                    clearUserRestrictions(new UserHandle(0));
                    if (this.mDeviceOwner != null) {
                        this.mDeviceOwner.clearDeviceOwner();
                        this.mDeviceOwner.writeOwnerFile();
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException(e);
        }
    }

    public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) {
        if (!this.mHasFeature) {
            return DBG;
        }
        if (ActivityManager.checkComponentPermission("android.permission.CREATE_USERS", Binder.getCallingUid(), -1, true) == -1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        }
        UserInfo info = this.mUserManager.getUserInfo(userHandle);
        if (info == null) {
            throw new IllegalArgumentException("Attempted to set profile owner for invalid userId: " + userHandle);
        }
        if (info.isGuest()) {
            throw new IllegalStateException("Cannot set a profile owner on a guest");
        }
        if (who == null || !DeviceOwner.isInstalledForUser(who.getPackageName(), userHandle)) {
            throw new IllegalArgumentException("Component " + who + " not installed for userId:" + userHandle);
        }
        synchronized (this) {
            if (UserHandle.getAppId(Binder.getCallingUid()) != 1000 && hasUserSetupCompleted(userHandle)) {
                throw new IllegalStateException("Trying to set profile owner but user is already set-up.");
            }
            if (this.mDeviceOwner == null) {
                this.mDeviceOwner = DeviceOwner.createWithProfileOwner(who, ownerName, userHandle);
                this.mDeviceOwner.writeOwnerFile();
                return true;
            }
            this.mDeviceOwner.setProfileOwner(who, ownerName, userHandle);
            this.mDeviceOwner.writeOwnerFile();
            return true;
        }
    }

    public void clearProfileOwner(ComponentName who) {
        if (this.mHasFeature) {
            UserHandle callingUser = Binder.getCallingUserHandle();
            getActiveAdminForCallerLocked(who, -1);
            synchronized (this) {
                long ident = Binder.clearCallingIdentity();
                try {
                    clearUserRestrictions(callingUser);
                    if (this.mDeviceOwner != null) {
                        this.mDeviceOwner.removeProfileOwner(callingUser.getIdentifier());
                        this.mDeviceOwner.writeOwnerFile();
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    private void clearUserRestrictions(UserHandle userHandle) {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        Bundle userRestrictions = this.mUserManager.getUserRestrictions();
        this.mUserManager.setUserRestrictions(new Bundle(), userHandle);
        if (userRestrictions.getBoolean("no_adjust_volume")) {
            audioManager.setMasterMute(DBG);
        }
        if (userRestrictions.getBoolean("no_unmute_microphone")) {
            audioManager.setMicrophoneMute(DBG);
        }
    }

    public boolean hasUserSetupCompleted() {
        return hasUserSetupCompleted(UserHandle.getCallingUserId());
    }

    private boolean hasUserSetupCompleted(int userHandle) {
        DevicePolicyData policy;
        if (!this.mHasFeature || (policy = getUserData(userHandle)) == null || policy.mUserSetupComplete) {
            return true;
        }
        return DBG;
    }

    public void setProfileEnabled(ComponentName who) {
        if (this.mHasFeature) {
            int userHandle = UserHandle.getCallingUserId();
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                getActiveAdminForCallerLocked(who, -1);
                int userId = UserHandle.getCallingUserId();
                long id = Binder.clearCallingIdentity();
                try {
                    this.mUserManager.setUserEnabled(userId);
                    Intent intent = new Intent("android.intent.action.MANAGED_PROFILE_ADDED");
                    intent.putExtra("android.intent.extra.USER", new UserHandle(userHandle));
                    intent.addFlags(1342177280);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.OWNER);
                } finally {
                    restoreCallingIdentity(id);
                }
            }
        }
    }

    public void setProfileName(ComponentName who, String profileName) {
        int userId = UserHandle.getCallingUserId();
        if (who == null) {
            throw new NullPointerException("ComponentName is null");
        }
        getActiveAdminForCallerLocked(who, -1);
        long id = Binder.clearCallingIdentity();
        try {
            this.mUserManager.setUserName(userId, profileName);
        } finally {
            restoreCallingIdentity(id);
        }
    }

    public ComponentName getProfileOwner(int userHandle) {
        ComponentName profileOwnerComponent = null;
        if (this.mHasFeature) {
            synchronized (this) {
                if (this.mDeviceOwner != null) {
                    profileOwnerComponent = this.mDeviceOwner.getProfileOwnerComponent(userHandle);
                }
            }
        }
        return profileOwnerComponent;
    }

    private ActiveAdmin getProfileOwnerAdmin(int userHandle) {
        ComponentName profileOwner = this.mDeviceOwner != null ? this.mDeviceOwner.getProfileOwnerComponent(userHandle) : null;
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
        String profileOwnerName = null;
        if (this.mHasFeature) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
            synchronized (this) {
                if (this.mDeviceOwner != null) {
                    profileOwnerName = this.mDeviceOwner.getProfileOwnerName(userHandle);
                }
            }
        }
        return profileOwnerName;
    }

    private boolean allowedToSetDeviceOwnerOnDevice() {
        int callingId = Binder.getCallingUid();
        if (callingId == 2000 || callingId == 0) {
            if (AccountManager.get(this.mContext).getAccounts().length == 0) {
                return true;
            }
            return DBG;
        }
        if (hasUserSetupCompleted(0)) {
            return DBG;
        }
        return true;
    }

    private void enforceCrossUserPermission(int userHandle) {
        if (userHandle < 0) {
            throw new IllegalArgumentException("Invalid userId " + userHandle);
        }
        int callingUid = Binder.getCallingUid();
        if (userHandle != UserHandle.getUserId(callingUid) && callingUid != 1000 && callingUid != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Must be system or have INTERACT_ACROSS_USERS_FULL permission");
        }
    }

    private void enforceSystemProcess(String message) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException(message);
        }
    }

    private void enforceNotManagedProfile(int userHandle, String message) {
        if (isManagedProfile(userHandle)) {
            throw new SecurityException("You can not " + message + " for a managed profile. ");
        }
    }

    private UserInfo getProfileParent(int userHandle) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mUserManager.getProfileParent(userHandle);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isManagedProfile(int userHandle) {
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mUserManager.getUserInfo(userHandle).isManagedProfile();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void enableIfNecessary(String packageName, int userId) {
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            ApplicationInfo ai = ipm.getApplicationInfo(packageName, 32768, userId);
            if (ai.enabledSetting == 4) {
                ipm.setApplicationEnabledSetting(packageName, 0, 1, userId, "DevicePolicyManager");
            }
        } catch (RemoteException e) {
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump DevicePolicyManagerService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        Printer p = new PrintWriterPrinter(pw);
        synchronized (this) {
            p.println("Current Device Policy Manager state:");
            int userCount = this.mUserData.size();
            for (int u = 0; u < userCount; u++) {
                DevicePolicyData policy = getUserData(this.mUserData.keyAt(u));
                p.println("  Enabled Device Admins (User " + policy.mUserHandle + "):");
                int N = policy.mAdminList.size();
                for (int i = 0; i < N; i++) {
                    ActiveAdmin ap = policy.mAdminList.get(i);
                    if (ap != null) {
                        pw.print("  ");
                        pw.print(ap.info.getComponent().flattenToShortString());
                        pw.println(":");
                        ap.dump("    ", pw);
                    }
                }
                if (!policy.mRemovingAdmins.isEmpty()) {
                    p.println("  Removing Device Admins (User " + policy.mUserHandle + "): " + policy.mRemovingAdmins);
                }
                pw.println(" ");
                pw.print("  mPasswordOwner=");
                pw.println(policy.mPasswordOwner);
            }
        }
    }

    public void addPersistentPreferredActivity(ComponentName who, IntentFilter filter, ComponentName activity) {
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            IPackageManager pm = AppGlobals.getPackageManager();
            long id = Binder.clearCallingIdentity();
            try {
                pm.addPersistentPreferredActivity(filter, activity, userHandle);
                restoreCallingIdentity(id);
            } catch (RemoteException e) {
                restoreCallingIdentity(id);
            } catch (Throwable th) {
                restoreCallingIdentity(id);
                throw th;
            }
        }
    }

    public void clearPackagePersistentPreferredActivities(ComponentName who, String packageName) {
        int userHandle = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            IPackageManager pm = AppGlobals.getPackageManager();
            long id = Binder.clearCallingIdentity();
            try {
                pm.clearPackagePersistentPreferredActivities(packageName, userHandle);
                restoreCallingIdentity(id);
            } catch (RemoteException e) {
                restoreCallingIdentity(id);
            } catch (Throwable th) {
                restoreCallingIdentity(id);
                throw th;
            }
        }
    }

    public void setApplicationRestrictions(ComponentName who, String packageName, Bundle settings) {
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            long id = Binder.clearCallingIdentity();
            try {
                this.mUserManager.setApplicationRestrictions(packageName, settings, userHandle);
            } finally {
                restoreCallingIdentity(id);
            }
        }
    }

    public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, int userHandle) {
        if (this.mHasFeature) {
            enforceCrossUserPermission(userHandle);
            enforceNotManagedProfile(userHandle, "set trust agent configuration");
            synchronized (this) {
                if (admin == null) {
                    throw new NullPointerException("admin is null");
                }
                if (agent == null) {
                    throw new NullPointerException("agent is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(admin, 9);
                ap.trustAgentInfos.put(agent.flattenToString(), new ActiveAdmin.TrustAgentInfo(args));
                saveSettingsLocked(userHandle);
                syncDeviceCapabilitiesLocked(getUserData(userHandle));
            }
        }
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userHandle) {
        if (!this.mHasFeature) {
            return null;
        }
        enforceCrossUserPermission(userHandle);
        if (agent == null) {
            throw new NullPointerException("agent is null");
        }
        synchronized (this) {
            String componentName = agent.flattenToString();
            if (admin != null) {
                ActiveAdmin ap = getActiveAdminUncheckedLocked(admin, userHandle);
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
            List<UserInfo> profiles = this.mUserManager.getProfiles(userHandle);
            List<PersistableBundle> result2 = null;
            boolean allAdminsHaveOptions = true;
            for (UserInfo userInfo : profiles) {
                DevicePolicyData policy = getUserDataUnchecked(userInfo.id);
                int N = policy.mAdminList.size();
                int i = 0;
                while (true) {
                    if (i < N) {
                        ActiveAdmin active = policy.mAdminList.get(i);
                        boolean disablesTrust = (active.disabledKeyguardFeatures & 16) != 0 ? true : DBG;
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
                            allAdminsHaveOptions = DBG;
                            break;
                        }
                        i++;
                    }
                }
            }
            if (!allAdminsHaveOptions) {
                result2 = null;
            }
            return result2;
        }
    }

    public void setRestrictionsProvider(ComponentName who, ComponentName permissionProvider) {
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
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
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Only the system can query the permission provider");
            }
            DevicePolicyData userData = getUserData(userHandle);
            componentName = userData != null ? userData.mRestrictionsProvider : null;
        }
        return componentName;
    }

    public void addCrossProfileIntentFilter(ComponentName who, IntentFilter filter, int flags) {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            IPackageManager pm = AppGlobals.getPackageManager();
            long id = Binder.clearCallingIdentity();
            if ((flags & 1) != 0) {
                try {
                    pm.addCrossProfileIntentFilter(filter, who.getPackageName(), this.mContext.getUserId(), callingUserId, 0, 0);
                } catch (RemoteException e) {
                    restoreCallingIdentity(id);
                } catch (Throwable th) {
                    restoreCallingIdentity(id);
                    throw th;
                }
            }
            if ((flags & 2) != 0) {
                pm.addCrossProfileIntentFilter(filter, who.getPackageName(), this.mContext.getUserId(), 0, callingUserId, 0);
            }
            restoreCallingIdentity(id);
        }
    }

    public void clearCrossProfileIntentFilters(ComponentName who) {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            IPackageManager pm = AppGlobals.getPackageManager();
            long id = Binder.clearCallingIdentity();
            try {
                pm.clearCrossProfileIntentFilters(callingUserId, who.getPackageName(), callingUserId);
                pm.clearCrossProfileIntentFilters(0, who.getPackageName(), callingUserId);
                restoreCallingIdentity(id);
            } catch (RemoteException e) {
                restoreCallingIdentity(id);
            } catch (Throwable th) {
                restoreCallingIdentity(id);
                throw th;
            }
        }
    }

    private boolean checkPackagesInPermittedListOrSystem(List<String> enabledPackages, List<String> permittedList) {
        int userIdToCheck = UserHandle.getCallingUserId();
        long id = Binder.clearCallingIdentity();
        try {
            UserInfo user = this.mUserManager.getUserInfo(userIdToCheck);
            if (user.isManagedProfile()) {
                userIdToCheck = user.profileGroupId;
            }
            IPackageManager pm = AppGlobals.getPackageManager();
            for (String enabledPackage : enabledPackages) {
                boolean systemService = DBG;
                try {
                    ApplicationInfo applicationInfo = pm.getApplicationInfo(enabledPackage, PackageManagerService.DumpState.DUMP_INSTALLS, userIdToCheck);
                    systemService = (applicationInfo.flags & 1) != 0 ? true : DBG;
                } catch (RemoteException e) {
                    Log.i(LOG_TAG, "Can't talk to package managed", e);
                }
                if (!systemService && !permittedList.contains(enabledPackage)) {
                    return DBG;
                }
            }
            restoreCallingIdentity(id);
            return true;
        } finally {
            restoreCallingIdentity(id);
        }
    }

    private AccessibilityManager getAccessibilityManagerForUser(int userId) {
        IBinder iBinder = ServiceManager.getService("accessibility");
        IAccessibilityManager service = iBinder == null ? null : IAccessibilityManager.Stub.asInterface(iBinder);
        return new AccessibilityManager(this.mContext, service, userId);
    }

    public boolean setPermittedAccessibilityServices(ComponentName who, List packageList) {
        if (!this.mHasFeature) {
            return DBG;
        }
        if (who == null) {
            throw new NullPointerException("ComponentName is null");
        }
        if (packageList != null) {
            int userId = UserHandle.getCallingUserId();
            long id = Binder.clearCallingIdentity();
            try {
                UserInfo user = this.mUserManager.getUserInfo(userId);
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
                    if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList)) {
                        Slog.e(LOG_TAG, "Cannot set permitted accessibility services, because it contains already enabled accesibility services.");
                        return DBG;
                    }
                }
            } finally {
                restoreCallingIdentity(id);
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
        if (who == null) {
            throw new NullPointerException("ComponentName is null");
        }
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
                List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
                int PROFILES_SIZE = profiles.size();
                int i = 0;
                while (i < PROFILES_SIZE) {
                    DevicePolicyData policy = getUserDataUnchecked(profiles.get(i).id);
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
                    long id = Binder.clearCallingIdentity();
                    try {
                        UserInfo user = this.mUserManager.getUserInfo(userId);
                        if (user.isManagedProfile()) {
                            userId = user.profileGroupId;
                        }
                        AccessibilityManager accessibilityManager = getAccessibilityManagerForUser(userId);
                        List<AccessibilityServiceInfo> installedServices = accessibilityManager.getInstalledAccessibilityServiceList();
                        IPackageManager pm = AppGlobals.getPackageManager();
                        if (installedServices != null) {
                            for (AccessibilityServiceInfo service : installedServices) {
                                String packageName = service.getResolveInfo().serviceInfo.packageName;
                                try {
                                    ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_INSTALLS, userId);
                                    if ((applicationInfo.flags & 1) != 0) {
                                        result2.add(packageName);
                                    }
                                } catch (RemoteException e) {
                                    Log.i(LOG_TAG, "Accessibility service in missing package", e);
                                }
                            }
                        }
                    } finally {
                        restoreCallingIdentity(id);
                    }
                }
                return result2;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private boolean checkCallerIsCurrentUserOrProfile() {
        int callingUserId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            UserInfo callingUser = this.mUserManager.getUserInfo(callingUserId);
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            if (callingUser.isManagedProfile() && callingUser.profileGroupId != currentUser.id) {
                Slog.e(LOG_TAG, "Cannot set permitted input methods for managed profile of a user that isn't the foreground user.");
                return DBG;
            }
            if (callingUser.isManagedProfile() || callingUserId == currentUser.id) {
                Binder.restoreCallingIdentity(token);
                return true;
            }
            Slog.e(LOG_TAG, "Cannot set permitted input methods of a user that isn't the foreground user.");
            return DBG;
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Failed to talk to activity managed.", e);
            return DBG;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean setPermittedInputMethods(ComponentName who, List packageList) {
        if (!this.mHasFeature) {
            return DBG;
        }
        if (who == null) {
            throw new NullPointerException("ComponentName is null");
        }
        if (!checkCallerIsCurrentUserOrProfile()) {
            return DBG;
        }
        if (packageList != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService("input_method");
            List<InputMethodInfo> enabledImes = inputMethodManager.getEnabledInputMethodList();
            if (enabledImes != null) {
                List<String> enabledPackages = new ArrayList<>();
                for (InputMethodInfo ime : enabledImes) {
                    enabledPackages.add(ime.getPackageName());
                }
                if (!checkPackagesInPermittedListOrSystem(enabledPackages, packageList)) {
                    Slog.e(LOG_TAG, "Cannot set permitted input methods, because it contains already enabled input method.");
                    return DBG;
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
        if (who == null) {
            throw new NullPointerException("ComponentName is null");
        }
        synchronized (this) {
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            list = admin.permittedInputMethods;
        }
        return list;
    }

    public List getPermittedInputMethodsForCurrentUser() throws Throwable {
        List<String> result;
        try {
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            int userId = currentUser.id;
            synchronized (this) {
                List<String> result2 = null;
                try {
                    List<UserInfo> profiles = this.mUserManager.getProfiles(userId);
                    int PROFILES_SIZE = profiles.size();
                    int i = 0;
                    while (i < PROFILES_SIZE) {
                        DevicePolicyData policy = getUserDataUnchecked(profiles.get(i).id);
                        int N = policy.mAdminList.size();
                        int j = 0;
                        while (true) {
                            result = result2;
                            if (j < N) {
                                try {
                                    ActiveAdmin admin = policy.mAdminList.get(j);
                                    List<String> fromAdmin = admin.permittedInputMethods;
                                    if (fromAdmin == null) {
                                        result2 = result;
                                    } else if (result == null) {
                                        result2 = new ArrayList<>(fromAdmin);
                                    } else {
                                        result.retainAll(fromAdmin);
                                        result2 = result;
                                    }
                                    j++;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            }
                        }
                        i++;
                        result2 = result;
                    }
                    if (result2 != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService("input_method");
                        List<InputMethodInfo> imes = inputMethodManager.getInputMethodList();
                        long id = Binder.clearCallingIdentity();
                        try {
                            IPackageManager pm = AppGlobals.getPackageManager();
                            if (imes != null) {
                                for (InputMethodInfo ime : imes) {
                                    String packageName = ime.getPackageName();
                                    try {
                                        ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_INSTALLS, userId);
                                        if ((applicationInfo.flags & 1) != 0) {
                                            result2.add(packageName);
                                        }
                                    } catch (RemoteException e) {
                                        Log.i(LOG_TAG, "Input method for missing package", e);
                                    }
                                }
                            }
                        } finally {
                            restoreCallingIdentity(id);
                        }
                    }
                    return result2;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        } catch (RemoteException e2) {
            Slog.e(LOG_TAG, "Failed to make remote calls to get current user", e2);
            return null;
        }
    }

    public UserHandle createUser(ComponentName who, String name) {
        UserHandle userHandle;
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -2);
            long id = Binder.clearCallingIdentity();
            try {
                UserInfo userInfo = this.mUserManager.createUser(name, 0);
                if (userInfo != null) {
                    userHandle = userInfo.getUserHandle();
                } else {
                    userHandle = null;
                }
                return userHandle;
            } finally {
                restoreCallingIdentity(id);
            }
        }
    }

    public UserHandle createAndInitializeUser(ComponentName who, String name, String ownerName, ComponentName profileOwnerComponent, Bundle adminExtras) {
        UserHandle user = createUser(who, name);
        if (user == null) {
            return null;
        }
        long id = Binder.clearCallingIdentity();
        try {
            String profileOwnerPkg = profileOwnerComponent.getPackageName();
            IPackageManager ipm = AppGlobals.getPackageManager();
            IActivityManager activityManager = ActivityManagerNative.getDefault();
            try {
                if (!ipm.isPackageAvailable(profileOwnerPkg, user.getIdentifier())) {
                    ipm.installExistingPackageAsUser(profileOwnerPkg, user.getIdentifier());
                }
                activityManager.startUserInBackground(user.getIdentifier());
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Failed to make remote calls for configureUser", e);
            }
            setActiveAdmin(profileOwnerComponent, true, user.getIdentifier(), adminExtras);
            setProfileOwner(profileOwnerComponent, ownerName, user.getIdentifier());
            return user;
        } finally {
            restoreCallingIdentity(id);
        }
    }

    public boolean removeUser(ComponentName who, UserHandle userHandle) {
        boolean zRemoveUser;
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -2);
            long id = Binder.clearCallingIdentity();
            try {
                zRemoveUser = this.mUserManager.removeUser(userHandle.getIdentifier());
            } finally {
                restoreCallingIdentity(id);
            }
        }
        return zRemoveUser;
    }

    public boolean switchUser(ComponentName who, UserHandle userHandle) {
        boolean zSwitchUser;
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -2);
            long id = Binder.clearCallingIdentity();
            int userId = 0;
            try {
                if (userHandle != null) {
                    userId = userHandle.getIdentifier();
                }
                zSwitchUser = ActivityManagerNative.getDefault().switchUser(userId);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Couldn't switch user", e);
                zSwitchUser = DBG;
            } finally {
                restoreCallingIdentity(id);
            }
        }
        return zSwitchUser;
    }

    public Bundle getApplicationRestrictions(ComponentName who, String packageName) {
        Bundle applicationRestrictions;
        UserHandle userHandle = new UserHandle(UserHandle.getCallingUserId());
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            long id = Binder.clearCallingIdentity();
            try {
                applicationRestrictions = this.mUserManager.getApplicationRestrictions(packageName, userHandle);
            } finally {
                restoreCallingIdentity(id);
            }
        }
        return applicationRestrictions;
    }

    public void setUserRestriction(ComponentName who, String key, boolean enabled) {
        UserHandle user = new UserHandle(UserHandle.getCallingUserId());
        int userHandle = user.getIdentifier();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(who, -1);
            boolean isDeviceOwner = isDeviceOwner(activeAdmin.info.getPackageName());
            if (!isDeviceOwner && userHandle != 0 && DEVICE_OWNER_USER_RESTRICTIONS.contains(key)) {
                throw new SecurityException("Profile owners cannot set user restriction " + key);
            }
            boolean alreadyRestricted = this.mUserManager.hasUserRestriction(key, user);
            IAudioService iAudioService = null;
            if ("no_unmute_microphone".equals(key) || "no_adjust_volume".equals(key)) {
                iAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
            }
            if (enabled && !alreadyRestricted) {
                try {
                    if ("no_unmute_microphone".equals(key)) {
                        iAudioService.setMicrophoneMute(true, who.getPackageName());
                    } else if ("no_adjust_volume".equals(key)) {
                        iAudioService.setMasterMute(true, 0, who.getPackageName(), (IBinder) null);
                    }
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to talk to AudioService.", re);
                }
                long id = Binder.clearCallingIdentity();
                if (!enabled) {
                    this.mUserManager.setUserRestriction(key, enabled, user);
                    if (enabled != alreadyRestricted) {
                    }
                }
            } else {
                long id2 = Binder.clearCallingIdentity();
                if (!enabled && !alreadyRestricted) {
                    try {
                        if ("no_config_wifi".equals(key)) {
                            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "wifi_networks_available_notification_on", 0, userHandle);
                        } else if ("no_usb_file_transfer".equals(key)) {
                            UsbManager manager = (UsbManager) this.mContext.getSystemService("usb");
                            manager.setCurrentFunction("none", DBG);
                        } else if ("no_share_location".equals(key)) {
                            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "location_mode", 0, userHandle);
                            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", "", userHandle);
                        } else if ("no_debugging_features".equals(key)) {
                            if (userHandle == 0) {
                                Settings.Global.putStringForUser(this.mContext.getContentResolver(), "adb_enabled", "0", userHandle);
                            }
                        } else if ("ensure_verify_apps".equals(key)) {
                            Settings.Global.putStringForUser(this.mContext.getContentResolver(), "package_verifier_enable", "1", userHandle);
                            Settings.Global.putStringForUser(this.mContext.getContentResolver(), "verifier_verify_adb_installs", "1", userHandle);
                        } else if ("no_install_unknown_sources".equals(key)) {
                            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "install_non_market_apps", 0, userHandle);
                        }
                        this.mUserManager.setUserRestriction(key, enabled, user);
                        if (enabled != alreadyRestricted) {
                            if (!enabled) {
                            }
                            sendChangedNotification(userHandle);
                        }
                    } finally {
                        restoreCallingIdentity(id2);
                    }
                } else {
                    this.mUserManager.setUserRestriction(key, enabled, user);
                    if (enabled != alreadyRestricted && "no_share_location".equals(key)) {
                        long version = SystemProperties.getLong("sys.settings_secure_version", 0L) + 1;
                        SystemProperties.set("sys.settings_secure_version", Long.toString(version));
                        Uri url = Uri.withAppendedPath(Settings.Secure.CONTENT_URI, "location_providers_allowed");
                        this.mContext.getContentResolver().notifyChange(url, null, true, userHandle);
                        if (!enabled) {
                            if (!"no_unmute_microphone".equals(key)) {
                            }
                        }
                        sendChangedNotification(userHandle);
                    } else {
                        if (!enabled && alreadyRestricted) {
                            try {
                                if (!"no_unmute_microphone".equals(key)) {
                                    iAudioService.setMicrophoneMute(DBG, who.getPackageName());
                                } else if ("no_adjust_volume".equals(key)) {
                                    iAudioService.setMasterMute(DBG, 0, who.getPackageName(), (IBinder) null);
                                }
                            } catch (RemoteException re2) {
                                Slog.e(LOG_TAG, "Failed to talk to AudioService.", re2);
                            }
                        }
                        sendChangedNotification(userHandle);
                    }
                }
            }
        }
    }

    public boolean setApplicationHidden(ComponentName who, String packageName, boolean hidden) {
        boolean applicationHiddenSettingAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            long id = Binder.clearCallingIdentity();
            try {
                try {
                    IPackageManager pm = AppGlobals.getPackageManager();
                    applicationHiddenSettingAsUser = pm.setApplicationHiddenSettingAsUser(packageName, hidden, callingUserId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to setApplicationHiddenSetting", re);
                    restoreCallingIdentity(id);
                    applicationHiddenSettingAsUser = DBG;
                }
            } finally {
                restoreCallingIdentity(id);
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public boolean isApplicationHidden(ComponentName who, String packageName) {
        boolean applicationHiddenSettingAsUser;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            long id = Binder.clearCallingIdentity();
            try {
                try {
                    IPackageManager pm = AppGlobals.getPackageManager();
                    applicationHiddenSettingAsUser = pm.getApplicationHiddenSettingAsUser(packageName, callingUserId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to getApplicationHiddenSettingAsUser", re);
                    restoreCallingIdentity(id);
                    applicationHiddenSettingAsUser = DBG;
                }
            } finally {
                restoreCallingIdentity(id);
            }
        }
        return applicationHiddenSettingAsUser;
    }

    public void enableSystemApp(ComponentName who, String packageName) {
        UserInfo primaryUser;
        IPackageManager pm;
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long id = Binder.clearCallingIdentity();
            try {
                try {
                    UserManager um = UserManager.get(this.mContext);
                    primaryUser = um.getProfileParent(userId);
                    if (primaryUser == null) {
                        primaryUser = um.getUserInfo(userId);
                    }
                    pm = AppGlobals.getPackageManager();
                } catch (RemoteException re) {
                    Slog.wtf(LOG_TAG, "Failed to install " + packageName, re);
                    restoreCallingIdentity(id);
                }
                if (!isSystemApp(pm, packageName, primaryUser.id)) {
                    throw new IllegalArgumentException("Only system apps can be enabled this way.");
                }
                pm.installExistingPackageAsUser(packageName, userId);
                restoreCallingIdentity(id);
            } catch (Throwable th) {
                restoreCallingIdentity(id);
                throw th;
            }
        }
    }

    public int enableSystemAppWithIntent(ComponentName who, Intent intent) {
        int numberOfAppsInstalled;
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            int userId = UserHandle.getCallingUserId();
            long id = Binder.clearCallingIdentity();
            try {
                UserManager um = UserManager.get(this.mContext);
                UserInfo primaryUser = um.getProfileParent(userId);
                if (primaryUser == null) {
                    primaryUser = um.getUserInfo(userId);
                }
                IPackageManager pm = AppGlobals.getPackageManager();
                List<ResolveInfo> activitiesToEnable = pm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0, primaryUser.id);
                numberOfAppsInstalled = 0;
                if (activitiesToEnable != null) {
                    for (ResolveInfo info : activitiesToEnable) {
                        if (info.activityInfo != null) {
                            if (!isSystemApp(pm, info.activityInfo.packageName, primaryUser.id)) {
                                throw new IllegalArgumentException("Only system apps can be enabled this way.");
                            }
                            numberOfAppsInstalled++;
                            pm.installExistingPackageAsUser(info.activityInfo.packageName, userId);
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.wtf(LOG_TAG, "Failed to resolve intent for: " + intent);
                numberOfAppsInstalled = 0;
            } finally {
                restoreCallingIdentity(id);
            }
        }
        return numberOfAppsInstalled;
    }

    private boolean isSystemApp(IPackageManager pm, String packageName, int userId) throws RemoteException {
        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManagerService.DumpState.DUMP_INSTALLS, userId);
        if ((appInfo.flags & 1) > 0) {
            return true;
        }
        return DBG;
    }

    public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled) {
        if (this.mHasFeature) {
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin ap = getActiveAdminForCallerLocked(who, -1);
                if (disabled) {
                    ap.accountTypesWithManagementDisabled.add(accountType);
                } else {
                    ap.accountTypesWithManagementDisabled.remove(accountType);
                }
                saveSettingsLocked(UserHandle.getCallingUserId());
            }
        }
    }

    public String[] getAccountTypesWithManagementDisabled() {
        return getAccountTypesWithManagementDisabledAsUser(UserHandle.getCallingUserId());
    }

    public String[] getAccountTypesWithManagementDisabledAsUser(int userId) {
        String[] strArr;
        enforceCrossUserPermission(userId);
        if (!this.mHasFeature) {
            return null;
        }
        synchronized (this) {
            DevicePolicyData policy = getUserData(userId);
            int N = policy.mAdminList.size();
            HashSet<String> resultSet = new HashSet<>();
            for (int i = 0; i < N; i++) {
                ActiveAdmin admin = policy.mAdminList.get(i);
                resultSet.addAll(admin.accountTypesWithManagementDisabled);
            }
            strArr = (String[]) resultSet.toArray(new String[resultSet.size()]);
        }
        return strArr;
    }

    public void setUninstallBlocked(ComponentName who, String packageName, boolean uninstallBlocked) {
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            long id = Binder.clearCallingIdentity();
            try {
                try {
                    IPackageManager pm = AppGlobals.getPackageManager();
                    pm.setBlockUninstallForUser(packageName, uninstallBlocked, userId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to setBlockUninstallForUser", re);
                    restoreCallingIdentity(id);
                }
            } finally {
                restoreCallingIdentity(id);
            }
        }
    }

    public boolean isUninstallBlocked(ComponentName who, String packageName) {
        boolean blockUninstallForUser;
        int userId = UserHandle.getCallingUserId();
        synchronized (this) {
            if (who != null) {
                getActiveAdminForCallerLocked(who, -1);
                long id = Binder.clearCallingIdentity();
                try {
                    IPackageManager pm = AppGlobals.getPackageManager();
                    blockUninstallForUser = pm.getBlockUninstallForUser(packageName, userId);
                } catch (RemoteException re) {
                    Slog.e(LOG_TAG, "Failed to getBlockUninstallForUser", re);
                    return DBG;
                } finally {
                    restoreCallingIdentity(id);
                }
            } else {
                long id2 = Binder.clearCallingIdentity();
                IPackageManager pm2 = AppGlobals.getPackageManager();
                blockUninstallForUser = pm2.getBlockUninstallForUser(packageName, userId);
            }
        }
        return blockUninstallForUser;
    }

    public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) {
        if (this.mHasFeature) {
            synchronized (this) {
                if (who == null) {
                    throw new NullPointerException("ComponentName is null");
                }
                ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
                if (admin.disableCallerId != disabled) {
                    admin.disableCallerId = disabled;
                    saveSettingsLocked(UserHandle.getCallingUserId());
                }
            }
        }
    }

    public boolean getCrossProfileCallerIdDisabled(ComponentName who) {
        boolean z;
        if (!this.mHasFeature) {
            return DBG;
        }
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            ActiveAdmin admin = getActiveAdminForCallerLocked(who, -1);
            z = admin.disableCallerId;
        }
        return z;
    }

    public boolean getCrossProfileCallerIdDisabledForUser(int userId) {
        boolean z;
        synchronized (this) {
            ActiveAdmin admin = getProfileOwnerAdmin(userId);
            z = admin != null ? admin.disableCallerId : DBG;
        }
        return z;
    }

    public void setLockTaskPackages(ComponentName who, String[] packages) throws SecurityException {
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -2);
            int userHandle = Binder.getCallingUserHandle().getIdentifier();
            DevicePolicyData policy = getUserData(userHandle);
            policy.mLockTaskPackages.clear();
            if (packages != null) {
                for (String pkg : packages) {
                    policy.mLockTaskPackages.add(pkg);
                }
            }
            saveSettingsLocked(userHandle);
        }
    }

    public String[] getLockTaskPackages(ComponentName who) {
        String[] strArr;
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -2);
            int userHandle = Binder.getCallingUserHandle().getIdentifier();
            DevicePolicyData policy = getUserData(userHandle);
            strArr = (String[]) policy.mLockTaskPackages.toArray(new String[0]);
        }
        return strArr;
    }

    public boolean isLockTaskPermitted(String pkg) {
        int uid = Binder.getCallingUid();
        int userHandle = UserHandle.getUserId(uid);
        DevicePolicyData policy = getUserData(userHandle);
        synchronized (this) {
            for (int i = 0; i < policy.mLockTaskPackages.size(); i++) {
                String lockTaskPackage = policy.mLockTaskPackages.get(i);
                if (lockTaskPackage.equals(pkg)) {
                    return true;
                }
            }
            return DBG;
        }
    }

    public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userHandle) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("notifyLockTaskModeChanged can only be called by system");
        }
        synchronized (this) {
            DevicePolicyData policy = getUserData(userHandle);
            Bundle adminExtras = new Bundle();
            adminExtras.putString("android.app.extra.LOCK_TASK_PACKAGE", pkg);
            for (ActiveAdmin admin : policy.mAdminList) {
                boolean ownsDevice = isDeviceOwner(admin.info.getPackageName());
                boolean ownsProfile = (getProfileOwner(userHandle) == null || !getProfileOwner(userHandle).equals(admin.info.getPackageName())) ? DBG : true;
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
        ContentResolver contentResolver = this.mContext.getContentResolver();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -2);
            if (!GLOBAL_SETTINGS_WHITELIST.contains(setting)) {
                throw new SecurityException(String.format("Permission denial: device owners cannot update %1$s", setting));
            }
            long id = Binder.clearCallingIdentity();
            try {
                Settings.Global.putString(contentResolver, setting, value);
            } finally {
                restoreCallingIdentity(id);
            }
        }
    }

    public void setSecureSetting(ComponentName who, String setting, String value) {
        int callingUserId = UserHandle.getCallingUserId();
        ContentResolver contentResolver = this.mContext.getContentResolver();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            ActiveAdmin activeAdmin = getActiveAdminForCallerLocked(who, -1);
            if (isDeviceOwner(activeAdmin.info.getPackageName())) {
                if (!SECURE_SETTINGS_DEVICEOWNER_WHITELIST.contains(setting)) {
                    throw new SecurityException(String.format("Permission denial: Device owners cannot update %1$s", setting));
                }
            } else if (!SECURE_SETTINGS_WHITELIST.contains(setting)) {
                throw new SecurityException(String.format("Permission denial: Profile owners cannot update %1$s", setting));
            }
            long id = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putStringForUser(contentResolver, setting, value, callingUserId);
            } finally {
                restoreCallingIdentity(id);
            }
        }
    }

    public void setMasterVolumeMuted(ComponentName who, boolean on) {
        this.mContext.getContentResolver();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            IAudioService iAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
            try {
                iAudioService.setMasterMute(on, 0, who.getPackageName(), (IBinder) null);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Failed to setMasterMute", re);
            }
        }
    }

    public boolean isMasterVolumeMuted(ComponentName who) {
        boolean zIsMasterMute;
        this.mContext.getContentResolver();
        synchronized (this) {
            if (who == null) {
                throw new NullPointerException("ComponentName is null");
            }
            getActiveAdminForCallerLocked(who, -1);
            AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
            zIsMasterMute = audioManager.isMasterMute();
        }
        return zIsMasterMute;
    }

    void updateUserSetupComplete() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        ContentResolver resolver = this.mContext.getContentResolver();
        int N = users.size();
        for (int i = 0; i < N; i++) {
            int userHandle = users.get(i).id;
            if (Settings.Secure.getIntForUser(resolver, "user_setup_complete", 0, userHandle) != 0) {
                DevicePolicyData policy = getUserData(userHandle);
                if (!policy.mUserSetupComplete) {
                    policy.mUserSetupComplete = true;
                    synchronized (this) {
                        saveSettingsLocked(userHandle);
                    }
                }
            }
        }
    }

    private class SetupContentObserver extends ContentObserver {
        private final Uri mUserSetupComplete;

        public SetupContentObserver(Handler handler) {
            super(handler);
            this.mUserSetupComplete = Settings.Secure.getUriFor("user_setup_complete");
        }

        void register(ContentResolver resolver) {
            resolver.registerContentObserver(this.mUserSetupComplete, DevicePolicyManagerService.DBG, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (this.mUserSetupComplete.equals(uri)) {
                DevicePolicyManagerService.this.updateUserSetupComplete();
            }
        }
    }

    private final class LocalService extends DevicePolicyManagerInternal {
        private List<DevicePolicyManagerInternal.OnCrossProfileWidgetProvidersChangeListener> mWidgetProviderListeners;

        private LocalService() {
        }

        public List<String> getCrossProfileWidgetProviders(int profileId) {
            List<String> listEmptyList;
            synchronized (DevicePolicyManagerService.this) {
                if (DevicePolicyManagerService.this.mDeviceOwner != null) {
                    ComponentName ownerComponent = DevicePolicyManagerService.this.mDeviceOwner.getProfileOwnerComponent(profileId);
                    if (ownerComponent == null) {
                        listEmptyList = Collections.emptyList();
                    } else {
                        DevicePolicyData policy = DevicePolicyManagerService.this.getUserDataUnchecked(profileId);
                        ActiveAdmin admin = policy.mAdminMap.get(ownerComponent);
                        if (admin == null || admin.crossProfileWidgetProviders == null || admin.crossProfileWidgetProviders.isEmpty()) {
                            listEmptyList = Collections.emptyList();
                        } else {
                            listEmptyList = admin.crossProfileWidgetProviders;
                        }
                    }
                } else {
                    listEmptyList = Collections.emptyList();
                }
            }
            return listEmptyList;
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
    }
}
