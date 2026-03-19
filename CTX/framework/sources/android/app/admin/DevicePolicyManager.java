package android.app.admin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.IServiceConnection;
import android.app.admin.SecurityLog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SettingsStringUtil;
import android.security.AttestedKeyPair;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.AttestationUtils;
import android.security.keystore.KeyAttestationException;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.telephony.data.ApnSetting;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.org.conscrypt.TrustedCertificateStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class DevicePolicyManager {

    @SystemApi
    public static final String ACCOUNT_FEATURE_DEVICE_OR_PROFILE_OWNER_ALLOWED = "android.account.DEVICE_OR_PROFILE_OWNER_ALLOWED";

    @SystemApi
    public static final String ACCOUNT_FEATURE_DEVICE_OR_PROFILE_OWNER_DISALLOWED = "android.account.DEVICE_OR_PROFILE_OWNER_DISALLOWED";
    public static final String ACTION_ADD_DEVICE_ADMIN = "android.app.action.ADD_DEVICE_ADMIN";
    public static final String ACTION_APPLICATION_DELEGATION_SCOPES_CHANGED = "android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED";
    public static final String ACTION_BUGREPORT_SHARING_ACCEPTED = "com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED";
    public static final String ACTION_BUGREPORT_SHARING_DECLINED = "com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED";
    public static final String ACTION_DATA_SHARING_RESTRICTION_APPLIED = "android.app.action.DATA_SHARING_RESTRICTION_APPLIED";
    public static final String ACTION_DATA_SHARING_RESTRICTION_CHANGED = "android.app.action.DATA_SHARING_RESTRICTION_CHANGED";
    public static final String ACTION_DEVICE_ADMIN_SERVICE = "android.app.action.DEVICE_ADMIN_SERVICE";
    public static final String ACTION_DEVICE_OWNER_CHANGED = "android.app.action.DEVICE_OWNER_CHANGED";
    public static final String ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED = "android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED";
    public static final String ACTION_MANAGED_PROFILE_PROVISIONED = "android.app.action.MANAGED_PROFILE_PROVISIONED";
    public static final String ACTION_MANAGED_USER_CREATED = "android.app.action.MANAGED_USER_CREATED";
    public static final String ACTION_PROFILE_OWNER_CHANGED = "android.app.action.PROFILE_OWNER_CHANGED";
    public static final String ACTION_PROVISIONING_SUCCESSFUL = "android.app.action.PROVISIONING_SUCCESSFUL";

    @SystemApi
    public static final String ACTION_PROVISION_FINALIZATION = "android.app.action.PROVISION_FINALIZATION";
    public static final String ACTION_PROVISION_MANAGED_DEVICE = "android.app.action.PROVISION_MANAGED_DEVICE";

    @SystemApi
    public static final String ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE = "android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE";
    public static final String ACTION_PROVISION_MANAGED_PROFILE = "android.app.action.PROVISION_MANAGED_PROFILE";
    public static final String ACTION_PROVISION_MANAGED_SHAREABLE_DEVICE = "android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE";
    public static final String ACTION_PROVISION_MANAGED_USER = "android.app.action.PROVISION_MANAGED_USER";
    public static final String ACTION_REMOTE_BUGREPORT_DISPATCH = "android.intent.action.REMOTE_BUGREPORT_DISPATCH";
    public static final String ACTION_SET_NEW_PARENT_PROFILE_PASSWORD = "android.app.action.SET_NEW_PARENT_PROFILE_PASSWORD";
    public static final String ACTION_SET_NEW_PASSWORD = "android.app.action.SET_NEW_PASSWORD";

    @SystemApi
    public static final String ACTION_SET_PROFILE_OWNER = "android.app.action.SET_PROFILE_OWNER";
    public static final String ACTION_SHOW_DEVICE_MONITORING_DIALOG = "android.app.action.SHOW_DEVICE_MONITORING_DIALOG";
    public static final String ACTION_START_ENCRYPTION = "android.app.action.START_ENCRYPTION";

    @SystemApi
    public static final String ACTION_STATE_USER_SETUP_COMPLETE = "android.app.action.STATE_USER_SETUP_COMPLETE";
    public static final String ACTION_SYSTEM_UPDATE_POLICY_CHANGED = "android.app.action.SYSTEM_UPDATE_POLICY_CHANGED";
    public static final int CODE_ACCOUNTS_NOT_EMPTY = 6;
    public static final int CODE_ADD_MANAGED_PROFILE_DISALLOWED = 15;
    public static final int CODE_CANNOT_ADD_MANAGED_PROFILE = 11;
    public static final int CODE_DEVICE_ADMIN_NOT_SUPPORTED = 13;
    public static final int CODE_HAS_DEVICE_OWNER = 1;
    public static final int CODE_HAS_PAIRED = 8;
    public static final int CODE_MANAGED_USERS_NOT_SUPPORTED = 9;
    public static final int CODE_NONSYSTEM_USER_EXISTS = 5;
    public static final int CODE_NOT_SYSTEM_USER = 7;
    public static final int CODE_NOT_SYSTEM_USER_SPLIT = 12;
    public static final int CODE_OK = 0;
    public static final int CODE_SPLIT_SYSTEM_USER_DEVICE_SYSTEM_USER = 14;
    public static final int CODE_SYSTEM_USER = 10;
    public static final int CODE_USER_HAS_PROFILE_OWNER = 2;
    public static final int CODE_USER_NOT_RUNNING = 3;
    public static final int CODE_USER_SETUP_COMPLETED = 4;
    public static final long DEFAULT_STRONG_AUTH_TIMEOUT_MS = 259200000;
    public static final String DELEGATION_APP_RESTRICTIONS = "delegation-app-restrictions";
    public static final String DELEGATION_BLOCK_UNINSTALL = "delegation-block-uninstall";
    public static final String DELEGATION_CERT_INSTALL = "delegation-cert-install";
    public static final String DELEGATION_ENABLE_SYSTEM_APP = "delegation-enable-system-app";
    public static final String DELEGATION_INSTALL_EXISTING_PACKAGE = "delegation-install-existing-package";
    public static final String DELEGATION_KEEP_UNINSTALLED_PACKAGES = "delegation-keep-uninstalled-packages";
    public static final String DELEGATION_PACKAGE_ACCESS = "delegation-package-access";
    public static final String DELEGATION_PERMISSION_GRANT = "delegation-permission-grant";
    public static final int ENCRYPTION_STATUS_ACTIVATING = 2;
    public static final int ENCRYPTION_STATUS_ACTIVE = 3;
    public static final int ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY = 4;
    public static final int ENCRYPTION_STATUS_ACTIVE_PER_USER = 5;
    public static final int ENCRYPTION_STATUS_INACTIVE = 1;
    public static final int ENCRYPTION_STATUS_UNSUPPORTED = 0;
    public static final String EXTRA_ADD_EXPLANATION = "android.app.extra.ADD_EXPLANATION";
    public static final String EXTRA_BUGREPORT_NOTIFICATION_TYPE = "android.app.extra.bugreport_notification_type";
    public static final String EXTRA_DELEGATION_SCOPES = "android.app.extra.DELEGATION_SCOPES";
    public static final String EXTRA_DEVICE_ADMIN = "android.app.extra.DEVICE_ADMIN";

    @SystemApi
    public static final String EXTRA_PROFILE_OWNER_NAME = "android.app.extra.PROFILE_OWNER_NAME";
    public static final String EXTRA_PROVISIONING_ACCOUNT_TO_MIGRATE = "android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE";
    public static final String EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE = "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE = "android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION";

    @SystemApi
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_ICON_URI";

    @SystemApi
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_LABEL";

    @Deprecated
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM = "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM";
    public static final String EXTRA_PROVISIONING_DISCLAIMERS = "android.app.extra.PROVISIONING_DISCLAIMERS";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_CONTENT = "android.app.extra.PROVISIONING_DISCLAIMER_CONTENT";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_HEADER = "android.app.extra.PROVISIONING_DISCLAIMER_HEADER";

    @Deprecated
    public static final String EXTRA_PROVISIONING_EMAIL_ADDRESS = "android.app.extra.PROVISIONING_EMAIL_ADDRESS";
    public static final String EXTRA_PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION = "android.app.extra.PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION";
    public static final String EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED = "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED";
    public static final String EXTRA_PROVISIONING_LOCALE = "android.app.extra.PROVISIONING_LOCALE";
    public static final String EXTRA_PROVISIONING_LOCAL_TIME = "android.app.extra.PROVISIONING_LOCAL_TIME";
    public static final String EXTRA_PROVISIONING_LOGO_URI = "android.app.extra.PROVISIONING_LOGO_URI";
    public static final String EXTRA_PROVISIONING_MAIN_COLOR = "android.app.extra.PROVISIONING_MAIN_COLOR";

    @SystemApi
    public static final String EXTRA_PROVISIONING_ORGANIZATION_NAME = "android.app.extra.PROVISIONING_ORGANIZATION_NAME";
    public static final String EXTRA_PROVISIONING_SKIP_ENCRYPTION = "android.app.extra.PROVISIONING_SKIP_ENCRYPTION";
    public static final String EXTRA_PROVISIONING_SKIP_USER_CONSENT = "android.app.extra.PROVISIONING_SKIP_USER_CONSENT";
    public static final String EXTRA_PROVISIONING_SKIP_USER_SETUP = "android.app.extra.PROVISIONING_SKIP_USER_SETUP";

    @SystemApi
    public static final String EXTRA_PROVISIONING_SUPPORT_URL = "android.app.extra.PROVISIONING_SUPPORT_URL";
    public static final String EXTRA_PROVISIONING_TIME_ZONE = "android.app.extra.PROVISIONING_TIME_ZONE";
    public static final String EXTRA_PROVISIONING_USE_MOBILE_DATA = "android.app.extra.PROVISIONING_USE_MOBILE_DATA";
    public static final String EXTRA_PROVISIONING_WIFI_HIDDEN = "android.app.extra.PROVISIONING_WIFI_HIDDEN";
    public static final String EXTRA_PROVISIONING_WIFI_PAC_URL = "android.app.extra.PROVISIONING_WIFI_PAC_URL";
    public static final String EXTRA_PROVISIONING_WIFI_PASSWORD = "android.app.extra.PROVISIONING_WIFI_PASSWORD";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_BYPASS = "android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_HOST = "android.app.extra.PROVISIONING_WIFI_PROXY_HOST";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_PORT = "android.app.extra.PROVISIONING_WIFI_PROXY_PORT";
    public static final String EXTRA_PROVISIONING_WIFI_SECURITY_TYPE = "android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE";
    public static final String EXTRA_PROVISIONING_WIFI_SSID = "android.app.extra.PROVISIONING_WIFI_SSID";
    public static final String EXTRA_REMOTE_BUGREPORT_HASH = "android.intent.extra.REMOTE_BUGREPORT_HASH";
    public static final String EXTRA_RESTRICTION = "android.app.extra.RESTRICTION";
    public static final int FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY = 1;
    public static final int FLAG_MANAGED_CAN_ACCESS_PARENT = 2;
    public static final int FLAG_PARENT_CAN_ACCESS_MANAGED = 1;
    public static final int ID_TYPE_BASE_INFO = 1;
    public static final int ID_TYPE_IMEI = 4;
    public static final int ID_TYPE_MEID = 8;
    public static final int ID_TYPE_SERIAL = 2;
    public static final int INSTALLKEY_REQUEST_CREDENTIALS_ACCESS = 1;
    public static final int INSTALLKEY_SET_USER_SELECTABLE = 2;
    public static final int KEYGUARD_DISABLE_BIOMETRICS = 416;
    public static final int KEYGUARD_DISABLE_FACE = 128;
    public static final int KEYGUARD_DISABLE_FEATURES_ALL = Integer.MAX_VALUE;
    public static final int KEYGUARD_DISABLE_FEATURES_NONE = 0;
    public static final int KEYGUARD_DISABLE_FINGERPRINT = 32;
    public static final int KEYGUARD_DISABLE_IRIS = 256;
    public static final int KEYGUARD_DISABLE_REMOTE_INPUT = 64;
    public static final int KEYGUARD_DISABLE_SECURE_CAMERA = 2;
    public static final int KEYGUARD_DISABLE_SECURE_NOTIFICATIONS = 4;
    public static final int KEYGUARD_DISABLE_TRUST_AGENTS = 16;
    public static final int KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS = 8;
    public static final int KEYGUARD_DISABLE_WIDGETS_ALL = 1;
    public static final int LEAVE_ALL_SYSTEM_APPS_ENABLED = 16;
    public static final int LOCK_TASK_FEATURE_GLOBAL_ACTIONS = 16;
    public static final int LOCK_TASK_FEATURE_HOME = 4;
    public static final int LOCK_TASK_FEATURE_KEYGUARD = 32;
    public static final int LOCK_TASK_FEATURE_NONE = 0;
    public static final int LOCK_TASK_FEATURE_NOTIFICATIONS = 2;
    public static final int LOCK_TASK_FEATURE_OVERVIEW = 8;
    public static final int LOCK_TASK_FEATURE_SYSTEM_INFO = 1;
    public static final int MAKE_USER_DEMO = 4;
    public static final int MAKE_USER_EPHEMERAL = 2;
    public static final String MIME_TYPE_PROVISIONING_NFC = "application/com.android.managedprovisioning";
    public static final int NOTIFICATION_BUGREPORT_ACCEPTED_NOT_FINISHED = 2;
    public static final int NOTIFICATION_BUGREPORT_FINISHED_NOT_ACCEPTED = 3;
    public static final int NOTIFICATION_BUGREPORT_STARTED = 1;
    public static final int PASSWORD_QUALITY_ALPHABETIC = 262144;
    public static final int PASSWORD_QUALITY_ALPHANUMERIC = 327680;
    public static final int PASSWORD_QUALITY_BIOMETRIC_WEAK = 32768;
    public static final int PASSWORD_QUALITY_COMPLEX = 393216;
    public static final int PASSWORD_QUALITY_MANAGED = 524288;
    public static final int PASSWORD_QUALITY_NUMERIC = 131072;
    public static final int PASSWORD_QUALITY_NUMERIC_COMPLEX = 196608;
    public static final int PASSWORD_QUALITY_SOMETHING = 65536;
    public static final int PASSWORD_QUALITY_UNSPECIFIED = 0;
    public static final int PERMISSION_GRANT_STATE_DEFAULT = 0;
    public static final int PERMISSION_GRANT_STATE_DENIED = 2;
    public static final int PERMISSION_GRANT_STATE_GRANTED = 1;
    public static final int PERMISSION_POLICY_AUTO_DENY = 2;
    public static final int PERMISSION_POLICY_AUTO_GRANT = 1;
    public static final int PERMISSION_POLICY_PROMPT = 0;
    public static final String POLICY_DISABLE_CAMERA = "policy_disable_camera";
    public static final String POLICY_DISABLE_SCREEN_CAPTURE = "policy_disable_screen_capture";
    public static final String POLICY_MANDATORY_BACKUPS = "policy_mandatory_backups";
    public static final String POLICY_SUSPEND_PACKAGES = "policy_suspend_packages";
    public static final int PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER = 432;
    public static final int RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT = 2;
    public static final int RESET_PASSWORD_REQUIRE_ENTRY = 1;
    public static final int SKIP_SETUP_WIZARD = 1;

    @SystemApi
    public static final int STATE_USER_PROFILE_COMPLETE = 4;

    @SystemApi
    public static final int STATE_USER_SETUP_COMPLETE = 2;

    @SystemApi
    public static final int STATE_USER_SETUP_FINALIZED = 3;

    @SystemApi
    public static final int STATE_USER_SETUP_INCOMPLETE = 1;

    @SystemApi
    public static final int STATE_USER_UNMANAGED = 0;
    private static String TAG = "DevicePolicyManager";
    public static final int WIPE_EUICC = 4;
    public static final int WIPE_EXTERNAL_STORAGE = 1;
    public static final int WIPE_RESET_PROTECTION_DATA = 2;
    private final Context mContext;
    private final boolean mParentInstance;
    private final IDevicePolicyManager mService;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AttestationIdType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface CreateAndManageUserFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface LockNowFlag {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface LockTaskFeature {
    }

    public interface OnClearApplicationUserDataListener {
        void onApplicationUserDataCleared(String str, boolean z);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProvisioningPreCondition {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SystemSettingsWhitelist {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface UserProvisioningState {
    }

    public DevicePolicyManager(Context context, IDevicePolicyManager iDevicePolicyManager) {
        this(context, iDevicePolicyManager, false);
    }

    @VisibleForTesting
    protected DevicePolicyManager(Context context, IDevicePolicyManager iDevicePolicyManager, boolean z) {
        this.mContext = context;
        this.mService = iDevicePolicyManager;
        this.mParentInstance = z;
    }

    @VisibleForTesting
    protected int myUserId() {
        return this.mContext.getUserId();
    }

    public boolean isAdminActive(ComponentName componentName) {
        throwIfParentInstance("isAdminActive");
        return isAdminActiveAsUser(componentName, myUserId());
    }

    public boolean isAdminActiveAsUser(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.isAdminActive(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isRemovingAdmin(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.isRemovingAdmin(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<ComponentName> getActiveAdmins() {
        throwIfParentInstance("getActiveAdmins");
        return getActiveAdminsAsUser(myUserId());
    }

    public List<ComponentName> getActiveAdminsAsUser(int i) {
        if (this.mService != null) {
            try {
                return this.mService.getActiveAdmins(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    @SystemApi
    public boolean packageHasActiveAdmins(String str) {
        return packageHasActiveAdmins(str, myUserId());
    }

    public boolean packageHasActiveAdmins(String str, int i) {
        if (this.mService != null) {
            try {
                return this.mService.packageHasActiveAdmins(str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void removeActiveAdmin(ComponentName componentName) {
        throwIfParentInstance("removeActiveAdmin");
        if (this.mService != null) {
            try {
                this.mService.removeActiveAdmin(componentName, myUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean hasGrantedPolicy(ComponentName componentName, int i) {
        throwIfParentInstance("hasGrantedPolicy");
        if (this.mService != null) {
            try {
                return this.mService.hasGrantedPolicy(componentName, i, myUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isSeparateProfileChallengeAllowed(int i) {
        if (this.mService != null) {
            try {
                return this.mService.isSeparateProfileChallengeAllowed(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setPasswordQuality(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordQuality(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordQuality(ComponentName componentName) {
        return getPasswordQuality(componentName, myUserId());
    }

    public int getPasswordQuality(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordQuality(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumLength(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumLength(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumLength(ComponentName componentName) {
        return getPasswordMinimumLength(componentName, myUserId());
    }

    public int getPasswordMinimumLength(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumLength(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumUpperCase(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumUpperCase(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumUpperCase(ComponentName componentName) {
        return getPasswordMinimumUpperCase(componentName, myUserId());
    }

    public int getPasswordMinimumUpperCase(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumUpperCase(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumLowerCase(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumLowerCase(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumLowerCase(ComponentName componentName) {
        return getPasswordMinimumLowerCase(componentName, myUserId());
    }

    public int getPasswordMinimumLowerCase(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumLowerCase(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumLetters(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumLetters(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumLetters(ComponentName componentName) {
        return getPasswordMinimumLetters(componentName, myUserId());
    }

    public int getPasswordMinimumLetters(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumLetters(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumNumeric(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumNumeric(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumNumeric(ComponentName componentName) {
        return getPasswordMinimumNumeric(componentName, myUserId());
    }

    public int getPasswordMinimumNumeric(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumNumeric(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumSymbols(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumSymbols(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumSymbols(ComponentName componentName) {
        return getPasswordMinimumSymbols(componentName, myUserId());
    }

    public int getPasswordMinimumSymbols(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumSymbols(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordMinimumNonLetter(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordMinimumNonLetter(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getPasswordMinimumNonLetter(ComponentName componentName) {
        return getPasswordMinimumNonLetter(componentName, myUserId());
    }

    public int getPasswordMinimumNonLetter(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordMinimumNonLetter(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setPasswordHistoryLength(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordHistoryLength(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setPasswordExpirationTimeout(ComponentName componentName, long j) {
        if (this.mService != null) {
            try {
                this.mService.setPasswordExpirationTimeout(componentName, j, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public long getPasswordExpirationTimeout(ComponentName componentName) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordExpirationTimeout(componentName, myUserId(), this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0L;
    }

    public long getPasswordExpiration(ComponentName componentName) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordExpiration(componentName, myUserId(), this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0L;
    }

    public int getPasswordHistoryLength(ComponentName componentName) {
        return getPasswordHistoryLength(componentName, myUserId());
    }

    public int getPasswordHistoryLength(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getPasswordHistoryLength(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public int getPasswordMaximumLength(int i) {
        return 16;
    }

    public boolean isActivePasswordSufficient() {
        if (this.mService != null) {
            try {
                return this.mService.isActivePasswordSufficient(myUserId(), this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isUsingUnifiedPassword(ComponentName componentName) {
        throwIfParentInstance("isUsingUnifiedPassword");
        if (this.mService != null) {
            try {
                return this.mService.isUsingUnifiedPassword(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    public boolean isProfileActivePasswordSufficientForParent(int i) {
        if (this.mService != null) {
            try {
                return this.mService.isProfileActivePasswordSufficientForParent(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public int getCurrentFailedPasswordAttempts() {
        return getCurrentFailedPasswordAttempts(myUserId());
    }

    public int getCurrentFailedPasswordAttempts(int i) {
        if (this.mService != null) {
            try {
                return this.mService.getCurrentFailedPasswordAttempts(i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1;
    }

    public boolean getDoNotAskCredentialsOnBoot() {
        if (this.mService != null) {
            try {
                return this.mService.getDoNotAskCredentialsOnBoot();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setMaximumFailedPasswordsForWipe(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName componentName) {
        return getMaximumFailedPasswordsForWipe(componentName, myUserId());
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getMaximumFailedPasswordsForWipe(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public int getProfileWithMinimumFailedPasswordsForWipe(int i) {
        if (this.mService != null) {
            try {
                return this.mService.getProfileWithMinimumFailedPasswordsForWipe(i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -10000;
    }

    public boolean resetPassword(String str, int i) {
        throwIfParentInstance("resetPassword");
        if (this.mService != null) {
            try {
                return this.mService.resetPassword(str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean setResetPasswordToken(ComponentName componentName, byte[] bArr) {
        throwIfParentInstance("setResetPasswordToken");
        if (this.mService != null) {
            try {
                return this.mService.setResetPasswordToken(componentName, bArr);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean clearResetPasswordToken(ComponentName componentName) {
        throwIfParentInstance("clearResetPasswordToken");
        if (this.mService != null) {
            try {
                return this.mService.clearResetPasswordToken(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isResetPasswordTokenActive(ComponentName componentName) {
        throwIfParentInstance("isResetPasswordTokenActive");
        if (this.mService != null) {
            try {
                return this.mService.isResetPasswordTokenActive(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean resetPasswordWithToken(ComponentName componentName, String str, byte[] bArr, int i) {
        throwIfParentInstance("resetPassword");
        if (this.mService != null) {
            try {
                return this.mService.resetPasswordWithToken(componentName, str, bArr, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setMaximumTimeToLock(ComponentName componentName, long j) {
        if (this.mService != null) {
            try {
                this.mService.setMaximumTimeToLock(componentName, j, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public long getMaximumTimeToLock(ComponentName componentName) {
        return getMaximumTimeToLock(componentName, myUserId());
    }

    public long getMaximumTimeToLock(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getMaximumTimeToLock(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0L;
    }

    public void setRequiredStrongAuthTimeout(ComponentName componentName, long j) {
        if (this.mService != null) {
            try {
                this.mService.setRequiredStrongAuthTimeout(componentName, j, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public long getRequiredStrongAuthTimeout(ComponentName componentName) {
        return getRequiredStrongAuthTimeout(componentName, myUserId());
    }

    public long getRequiredStrongAuthTimeout(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getRequiredStrongAuthTimeout(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return DEFAULT_STRONG_AUTH_TIMEOUT_MS;
    }

    public void lockNow() {
        lockNow(0);
    }

    public void lockNow(int i) {
        if (this.mService != null) {
            try {
                this.mService.lockNow(i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void wipeData(int i) {
        throwIfParentInstance("wipeData");
        wipeDataInternal(i, this.mContext.getString(R.string.work_profile_deleted_description_dpm_wipe));
    }

    public void wipeData(int i, CharSequence charSequence) {
        throwIfParentInstance("wipeData");
        Preconditions.checkNotNull(charSequence, "CharSequence is null");
        wipeDataInternal(i, charSequence.toString());
    }

    private void wipeDataInternal(int i, String str) {
        if (this.mService != null) {
            try {
                this.mService.wipeDataWithReason(i, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public ComponentName setGlobalProxy(ComponentName componentName, Proxy proxy, List<String> list) {
        String string;
        throwIfParentInstance("setGlobalProxy");
        if (proxy == null) {
            throw new NullPointerException();
        }
        String str = null;
        if (this.mService == null) {
            return null;
        }
        try {
            if (!proxy.equals(Proxy.NO_PROXY)) {
                if (!proxy.type().equals(Proxy.Type.HTTP)) {
                    throw new IllegalArgumentException();
                }
                InetSocketAddress inetSocketAddress = (InetSocketAddress) proxy.address();
                String hostName = inetSocketAddress.getHostName();
                int port = inetSocketAddress.getPort();
                str = hostName + SettingsStringUtil.DELIMITER + Integer.toString(port);
                if (list == null) {
                    string = "";
                } else {
                    StringBuilder sb = new StringBuilder();
                    boolean z = true;
                    for (String str2 : list) {
                        if (!z) {
                            sb.append(",");
                        } else {
                            z = false;
                        }
                        sb.append(str2.trim());
                    }
                    string = sb.toString();
                }
                if (android.net.Proxy.validate(hostName, Integer.toString(port), string) != 0) {
                    throw new IllegalArgumentException();
                }
            } else {
                string = null;
            }
            return this.mService.setGlobalProxy(componentName, str, string);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setRecommendedGlobalProxy(ComponentName componentName, ProxyInfo proxyInfo) {
        throwIfParentInstance("setRecommendedGlobalProxy");
        if (this.mService != null) {
            try {
                this.mService.setRecommendedGlobalProxy(componentName, proxyInfo);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public ComponentName getGlobalProxyAdmin() {
        if (this.mService != null) {
            try {
                return this.mService.getGlobalProxyAdmin(myUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public int setStorageEncryption(ComponentName componentName, boolean z) {
        throwIfParentInstance("setStorageEncryption");
        if (this.mService != null) {
            try {
                return this.mService.setStorageEncryption(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public boolean getStorageEncryption(ComponentName componentName) {
        throwIfParentInstance("getStorageEncryption");
        if (this.mService != null) {
            try {
                return this.mService.getStorageEncryption(componentName, myUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public int getStorageEncryptionStatus() {
        throwIfParentInstance("getStorageEncryptionStatus");
        return getStorageEncryptionStatus(myUserId());
    }

    public int getStorageEncryptionStatus(int i) {
        if (this.mService != null) {
            try {
                return this.mService.getStorageEncryptionStatus(this.mContext.getPackageName(), i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public boolean approveCaCert(String str, int i, boolean z) {
        if (this.mService != null) {
            try {
                return this.mService.approveCaCert(str, i, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isCaCertApproved(String str, int i) {
        if (this.mService != null) {
            try {
                return this.mService.isCaCertApproved(str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean installCaCert(ComponentName componentName, byte[] bArr) {
        throwIfParentInstance("installCaCert");
        if (this.mService != null) {
            try {
                return this.mService.installCaCert(componentName, this.mContext.getPackageName(), bArr);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void uninstallCaCert(ComponentName componentName, byte[] bArr) {
        throwIfParentInstance("uninstallCaCert");
        if (this.mService != null) {
            try {
                this.mService.uninstallCaCerts(componentName, this.mContext.getPackageName(), new String[]{getCaCertAlias(bArr)});
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (CertificateException e2) {
                Log.w(TAG, "Unable to parse certificate", e2);
            }
        }
    }

    public List<byte[]> getInstalledCaCerts(ComponentName componentName) {
        ArrayList arrayList = new ArrayList();
        throwIfParentInstance("getInstalledCaCerts");
        if (this.mService != null) {
            try {
                this.mService.enforceCanManageCaCerts(componentName, this.mContext.getPackageName());
                TrustedCertificateStore trustedCertificateStore = new TrustedCertificateStore();
                for (String str : trustedCertificateStore.userAliases()) {
                    try {
                        arrayList.add(trustedCertificateStore.getCertificate(str).getEncoded());
                    } catch (CertificateException e) {
                        Log.w(TAG, "Could not encode certificate: " + str, e);
                    }
                }
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }
        return arrayList;
    }

    public void uninstallAllUserCaCerts(ComponentName componentName) {
        throwIfParentInstance("uninstallAllUserCaCerts");
        if (this.mService != null) {
            try {
                this.mService.uninstallCaCerts(componentName, this.mContext.getPackageName(), (String[]) new TrustedCertificateStore().userAliases().toArray(new String[0]));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean hasCaCertInstalled(ComponentName componentName, byte[] bArr) {
        throwIfParentInstance("hasCaCertInstalled");
        if (this.mService != null) {
            try {
                this.mService.enforceCanManageCaCerts(componentName, this.mContext.getPackageName());
                return getCaCertAlias(bArr) != null;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (CertificateException e2) {
                Log.w(TAG, "Could not parse certificate", e2);
            }
        }
        return false;
    }

    public boolean installKeyPair(ComponentName componentName, PrivateKey privateKey, Certificate certificate, String str) {
        return installKeyPair(componentName, privateKey, new Certificate[]{certificate}, str, false);
    }

    public boolean installKeyPair(ComponentName componentName, PrivateKey privateKey, Certificate[] certificateArr, String str, boolean z) {
        int i;
        if (z) {
            i = 3;
        } else {
            i = 2;
        }
        return installKeyPair(componentName, privateKey, certificateArr, str, i);
    }

    public boolean installKeyPair(ComponentName componentName, PrivateKey privateKey, Certificate[] certificateArr, String str, int i) {
        byte[] bArrConvertToPem;
        throwIfParentInstance("installKeyPair");
        boolean z = (i & 1) == 1;
        boolean z2 = (i & 2) == 2;
        try {
            byte[] bArrConvertToPem2 = Credentials.convertToPem(certificateArr[0]);
            if (certificateArr.length <= 1) {
                bArrConvertToPem = null;
            } else {
                bArrConvertToPem = Credentials.convertToPem((Certificate[]) Arrays.copyOfRange(certificateArr, 1, certificateArr.length));
            }
            return this.mService.installKeyPair(componentName, this.mContext.getPackageName(), ((PKCS8EncodedKeySpec) KeyFactory.getInstance(privateKey.getAlgorithm()).getKeySpec(privateKey, PKCS8EncodedKeySpec.class)).getEncoded(), bArrConvertToPem2, bArrConvertToPem, str, z, z2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (IOException | CertificateException e2) {
            Log.w(TAG, "Could not pem-encode certificate", e2);
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e3) {
            Log.w(TAG, "Failed to obtain private key material", e3);
            return false;
        }
    }

    public boolean removeKeyPair(ComponentName componentName, String str) {
        throwIfParentInstance("removeKeyPair");
        try {
            return this.mService.removeKeyPair(componentName, this.mContext.getPackageName(), str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public AttestedKeyPair generateKeyPair(ComponentName componentName, String str, KeyGenParameterSpec keyGenParameterSpec, int i) {
        X509Certificate[] certificateChain;
        throwIfParentInstance("generateKeyPair");
        try {
            ParcelableKeyGenParameterSpec parcelableKeyGenParameterSpec = new ParcelableKeyGenParameterSpec(keyGenParameterSpec);
            KeymasterCertificateChain keymasterCertificateChain = new KeymasterCertificateChain();
            if (!this.mService.generateKeyPair(componentName, this.mContext.getPackageName(), str, parcelableKeyGenParameterSpec, i, keymasterCertificateChain)) {
                Log.e(TAG, "Error generating key via DevicePolicyManagerService.");
                return null;
            }
            String keystoreAlias = keyGenParameterSpec.getKeystoreAlias();
            KeyPair keyPair = KeyChain.getKeyPair(this.mContext, keystoreAlias);
            try {
                if (AttestationUtils.isChainValid(keymasterCertificateChain)) {
                    certificateChain = AttestationUtils.parseCertificateChain(keymasterCertificateChain);
                } else {
                    certificateChain = null;
                }
                return new AttestedKeyPair(keyPair, certificateChain);
            } catch (KeyAttestationException e) {
                Log.e(TAG, "Error parsing attestation chain for alias " + keystoreAlias, e);
                this.mService.removeKeyPair(componentName, this.mContext.getPackageName(), keystoreAlias);
                return null;
            }
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        } catch (KeyChainException e3) {
            Log.w(TAG, "Failed to generate key", e3);
            return null;
        } catch (InterruptedException e4) {
            Log.w(TAG, "Interrupted while generating key", e4);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public boolean isDeviceIdAttestationSupported() {
        return this.mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_DEVICE_ID_ATTESTATION);
    }

    public boolean setKeyPairCertificate(ComponentName componentName, String str, List<Certificate> list, boolean z) {
        byte[] bArrConvertToPem;
        throwIfParentInstance("setKeyPairCertificate");
        try {
            byte[] bArrConvertToPem2 = Credentials.convertToPem(list.get(0));
            if (list.size() <= 1) {
                bArrConvertToPem = null;
            } else {
                bArrConvertToPem = Credentials.convertToPem((Certificate[]) list.subList(1, list.size()).toArray(new Certificate[0]));
            }
            return this.mService.setKeyPairCertificate(componentName, this.mContext.getPackageName(), str, bArrConvertToPem2, bArrConvertToPem, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (IOException | CertificateException e2) {
            Log.w(TAG, "Could not pem-encode certificate", e2);
            return false;
        }
    }

    private static String getCaCertAlias(byte[] bArr) throws CertificateException {
        return new TrustedCertificateStore().getCertificateAlias((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr)));
    }

    @Deprecated
    public void setCertInstallerPackage(ComponentName componentName, String str) throws SecurityException {
        throwIfParentInstance("setCertInstallerPackage");
        if (this.mService != null) {
            try {
                this.mService.setCertInstallerPackage(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Deprecated
    public String getCertInstallerPackage(ComponentName componentName) throws SecurityException {
        throwIfParentInstance("getCertInstallerPackage");
        if (this.mService != null) {
            try {
                return this.mService.getCertInstallerPackage(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void setDelegatedScopes(ComponentName componentName, String str, List<String> list) {
        throwIfParentInstance("setDelegatedScopes");
        if (this.mService != null) {
            try {
                this.mService.setDelegatedScopes(componentName, str, list);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public List<String> getDelegatedScopes(ComponentName componentName, String str) {
        throwIfParentInstance("getDelegatedScopes");
        if (this.mService != null) {
            try {
                return this.mService.getDelegatedScopes(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public List<String> getDelegatePackages(ComponentName componentName, String str) {
        throwIfParentInstance("getDelegatePackages");
        if (this.mService != null) {
            try {
                return this.mService.getDelegatePackages(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void setAlwaysOnVpnPackage(ComponentName componentName, String str, boolean z) throws UnsupportedOperationException, PackageManager.NameNotFoundException {
        throwIfParentInstance("setAlwaysOnVpnPackage");
        if (this.mService != null) {
            try {
                if (!this.mService.setAlwaysOnVpnPackage(componentName, str, z)) {
                    throw new PackageManager.NameNotFoundException(str);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String getAlwaysOnVpnPackage(ComponentName componentName) {
        throwIfParentInstance("getAlwaysOnVpnPackage");
        if (this.mService != null) {
            try {
                return this.mService.getAlwaysOnVpnPackage(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void setCameraDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setCameraDisabled");
        if (this.mService != null) {
            try {
                this.mService.setCameraDisabled(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getCameraDisabled(ComponentName componentName) {
        throwIfParentInstance("getCameraDisabled");
        return getCameraDisabled(componentName, myUserId());
    }

    public boolean getCameraDisabled(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getCameraDisabled(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean requestBugreport(ComponentName componentName) {
        throwIfParentInstance("requestBugreport");
        if (this.mService != null) {
            try {
                return this.mService.requestBugreport(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean getGuestUserDisabled(ComponentName componentName) {
        return false;
    }

    public void setScreenCaptureDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setScreenCaptureDisabled");
        if (this.mService != null) {
            try {
                this.mService.setScreenCaptureDisabled(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getScreenCaptureDisabled(ComponentName componentName) {
        throwIfParentInstance("getScreenCaptureDisabled");
        return getScreenCaptureDisabled(componentName, myUserId());
    }

    public boolean getScreenCaptureDisabled(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getScreenCaptureDisabled(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setAutoTimeRequired(ComponentName componentName, boolean z) {
        throwIfParentInstance("setAutoTimeRequired");
        if (this.mService != null) {
            try {
                this.mService.setAutoTimeRequired(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getAutoTimeRequired() {
        throwIfParentInstance("getAutoTimeRequired");
        if (this.mService != null) {
            try {
                return this.mService.getAutoTimeRequired();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setForceEphemeralUsers(ComponentName componentName, boolean z) {
        throwIfParentInstance("setForceEphemeralUsers");
        if (this.mService != null) {
            try {
                this.mService.setForceEphemeralUsers(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getForceEphemeralUsers(ComponentName componentName) {
        throwIfParentInstance("getForceEphemeralUsers");
        if (this.mService != null) {
            try {
                return this.mService.getForceEphemeralUsers(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setKeyguardDisabledFeatures(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                this.mService.setKeyguardDisabledFeatures(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getKeyguardDisabledFeatures(ComponentName componentName) {
        return getKeyguardDisabledFeatures(componentName, myUserId());
    }

    public int getKeyguardDisabledFeatures(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getKeyguardDisabledFeatures(componentName, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setActiveAdmin(ComponentName componentName, boolean z, int i) {
        if (this.mService != null) {
            try {
                this.mService.setActiveAdmin(componentName, z, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setActiveAdmin(ComponentName componentName, boolean z) {
        setActiveAdmin(componentName, z, myUserId());
    }

    public void getRemoveWarning(ComponentName componentName, RemoteCallback remoteCallback) {
        if (this.mService != null) {
            try {
                this.mService.getRemoveWarning(componentName, remoteCallback, myUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setActivePasswordState(PasswordMetrics passwordMetrics, int i) {
        if (this.mService != null) {
            try {
                this.mService.setActivePasswordState(passwordMetrics, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportPasswordChanged(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportPasswordChanged(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportFailedPasswordAttempt(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportFailedPasswordAttempt(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportSuccessfulPasswordAttempt(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportSuccessfulPasswordAttempt(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportFailedFingerprintAttempt(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportFailedFingerprintAttempt(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportSuccessfulFingerprintAttempt(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportSuccessfulFingerprintAttempt(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportKeyguardDismissed(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportKeyguardDismissed(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void reportKeyguardSecured(int i) {
        if (this.mService != null) {
            try {
                this.mService.reportKeyguardSecured(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean setDeviceOwner(ComponentName componentName) {
        return setDeviceOwner(componentName, (String) null);
    }

    public boolean setDeviceOwner(ComponentName componentName, int i) {
        return setDeviceOwner(componentName, null, i);
    }

    public boolean setDeviceOwner(ComponentName componentName, String str) {
        return setDeviceOwner(componentName, str, 0);
    }

    public boolean setDeviceOwner(ComponentName componentName, String str, int i) throws IllegalStateException, IllegalArgumentException {
        if (this.mService != null) {
            try {
                return this.mService.setDeviceOwner(componentName, str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isDeviceOwnerApp(String str) {
        throwIfParentInstance("isDeviceOwnerApp");
        return isDeviceOwnerAppOnCallingUser(str);
    }

    public boolean isDeviceOwnerAppOnCallingUser(String str) {
        return isDeviceOwnerAppOnAnyUserInner(str, true);
    }

    public boolean isDeviceOwnerAppOnAnyUser(String str) {
        return isDeviceOwnerAppOnAnyUserInner(str, false);
    }

    public ComponentName getDeviceOwnerComponentOnCallingUser() {
        return getDeviceOwnerComponentInner(true);
    }

    @SystemApi
    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return getDeviceOwnerComponentInner(false);
    }

    private boolean isDeviceOwnerAppOnAnyUserInner(String str, boolean z) {
        ComponentName deviceOwnerComponentInner;
        if (str == null || (deviceOwnerComponentInner = getDeviceOwnerComponentInner(z)) == null) {
            return false;
        }
        return str.equals(deviceOwnerComponentInner.getPackageName());
    }

    private ComponentName getDeviceOwnerComponentInner(boolean z) {
        if (this.mService != null) {
            try {
                return this.mService.getDeviceOwnerComponent(z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public int getDeviceOwnerUserId() {
        if (this.mService != null) {
            try {
                return this.mService.getDeviceOwnerUserId();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -10000;
    }

    @Deprecated
    public void clearDeviceOwnerApp(String str) {
        throwIfParentInstance("clearDeviceOwnerApp");
        if (this.mService != null) {
            try {
                this.mService.clearDeviceOwner(str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public String getDeviceOwner() {
        throwIfParentInstance("getDeviceOwner");
        ComponentName deviceOwnerComponentOnCallingUser = getDeviceOwnerComponentOnCallingUser();
        if (deviceOwnerComponentOnCallingUser != null) {
            return deviceOwnerComponentOnCallingUser.getPackageName();
        }
        return null;
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public boolean isDeviceManaged() {
        try {
            return this.mService.hasDeviceOwner();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public String getDeviceOwnerNameOnAnyUser() {
        throwIfParentInstance("getDeviceOwnerNameOnAnyUser");
        if (this.mService != null) {
            try {
                return this.mService.getDeviceOwnerName();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public String getDeviceInitializerApp() {
        return null;
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public ComponentName getDeviceInitializerComponent() {
        return null;
    }

    @SystemApi
    @Deprecated
    public boolean setActiveProfileOwner(ComponentName componentName, @Deprecated String str) throws IllegalArgumentException {
        throwIfParentInstance("setActiveProfileOwner");
        if (this.mService == null) {
            return false;
        }
        try {
            int iMyUserId = myUserId();
            this.mService.setActiveAdmin(componentName, false, iMyUserId);
            return this.mService.setProfileOwner(componentName, str, iMyUserId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void clearProfileOwner(ComponentName componentName) {
        throwIfParentInstance("clearProfileOwner");
        if (this.mService != null) {
            try {
                this.mService.clearProfileOwner(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean hasUserSetupCompleted() {
        if (this.mService != null) {
            try {
                return this.mService.hasUserSetupCompleted();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    public boolean setProfileOwner(ComponentName componentName, @Deprecated String str, int i) throws IllegalArgumentException {
        if (this.mService != null) {
            if (str == null) {
                str = "";
            }
            try {
                return this.mService.setProfileOwner(componentName, str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setDeviceOwnerLockScreenInfo(ComponentName componentName, CharSequence charSequence) {
        throwIfParentInstance("setDeviceOwnerLockScreenInfo");
        if (this.mService != null) {
            try {
                this.mService.setDeviceOwnerLockScreenInfo(componentName, charSequence);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public CharSequence getDeviceOwnerLockScreenInfo() {
        throwIfParentInstance("getDeviceOwnerLockScreenInfo");
        if (this.mService != null) {
            try {
                return this.mService.getDeviceOwnerLockScreenInfo();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public String[] setPackagesSuspended(ComponentName componentName, String[] strArr, boolean z) {
        throwIfParentInstance("setPackagesSuspended");
        if (this.mService != null) {
            try {
                return this.mService.setPackagesSuspended(componentName, this.mContext.getPackageName(), strArr, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return strArr;
    }

    public boolean isPackageSuspended(ComponentName componentName, String str) throws PackageManager.NameNotFoundException {
        throwIfParentInstance("isPackageSuspended");
        if (this.mService != null) {
            try {
                return this.mService.isPackageSuspended(componentName, this.mContext.getPackageName(), str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (IllegalArgumentException e2) {
                throw new PackageManager.NameNotFoundException(str);
            }
        }
        return false;
    }

    public void setProfileEnabled(ComponentName componentName) {
        throwIfParentInstance("setProfileEnabled");
        if (this.mService != null) {
            try {
                this.mService.setProfileEnabled(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setProfileName(ComponentName componentName, String str) {
        throwIfParentInstance("setProfileName");
        if (this.mService != null) {
            try {
                this.mService.setProfileName(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean isProfileOwnerApp(String str) {
        throwIfParentInstance("isProfileOwnerApp");
        if (this.mService == null) {
            return false;
        }
        try {
            ComponentName profileOwner = this.mService.getProfileOwner(myUserId());
            if (profileOwner != null) {
                return profileOwner.getPackageName().equals(str);
            }
            return false;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public ComponentName getProfileOwner() throws IllegalArgumentException {
        throwIfParentInstance("getProfileOwner");
        return getProfileOwnerAsUser(this.mContext.getUserId());
    }

    public ComponentName getProfileOwnerAsUser(int i) throws IllegalArgumentException {
        if (this.mService != null) {
            try {
                return this.mService.getProfileOwner(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public String getProfileOwnerName() throws IllegalArgumentException {
        if (this.mService != null) {
            try {
                return this.mService.getProfileOwnerName(this.mContext.getUserId());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    @SystemApi
    public String getProfileOwnerNameAsUser(int i) throws IllegalArgumentException {
        throwIfParentInstance("getProfileOwnerNameAsUser");
        if (this.mService != null) {
            try {
                return this.mService.getProfileOwnerName(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void addPersistentPreferredActivity(ComponentName componentName, IntentFilter intentFilter, ComponentName componentName2) {
        throwIfParentInstance("addPersistentPreferredActivity");
        if (this.mService != null) {
            try {
                this.mService.addPersistentPreferredActivity(componentName, intentFilter, componentName2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void clearPackagePersistentPreferredActivities(ComponentName componentName, String str) {
        throwIfParentInstance("clearPackagePersistentPreferredActivities");
        if (this.mService != null) {
            try {
                this.mService.clearPackagePersistentPreferredActivities(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setDefaultSmsApplication(ComponentName componentName, String str) {
        throwIfParentInstance("setDefaultSmsApplication");
        if (this.mService != null) {
            try {
                this.mService.setDefaultSmsApplication(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Deprecated
    public void setApplicationRestrictionsManagingPackage(ComponentName componentName, String str) throws PackageManager.NameNotFoundException {
        throwIfParentInstance("setApplicationRestrictionsManagingPackage");
        if (this.mService != null) {
            try {
                if (!this.mService.setApplicationRestrictionsManagingPackage(componentName, str)) {
                    throw new PackageManager.NameNotFoundException(str);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Deprecated
    public String getApplicationRestrictionsManagingPackage(ComponentName componentName) {
        throwIfParentInstance("getApplicationRestrictionsManagingPackage");
        if (this.mService != null) {
            try {
                return this.mService.getApplicationRestrictionsManagingPackage(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    @Deprecated
    public boolean isCallerApplicationRestrictionsManagingPackage() {
        throwIfParentInstance("isCallerApplicationRestrictionsManagingPackage");
        if (this.mService != null) {
            try {
                return this.mService.isCallerApplicationRestrictionsManagingPackage(this.mContext.getPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setApplicationRestrictions(ComponentName componentName, String str, Bundle bundle) {
        throwIfParentInstance("setApplicationRestrictions");
        if (this.mService != null) {
            try {
                this.mService.setApplicationRestrictions(componentName, this.mContext.getPackageName(), str, bundle);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle) {
        if (this.mService != null) {
            try {
                this.mService.setTrustAgentConfiguration(componentName, componentName2, persistableBundle, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2) {
        return getTrustAgentConfiguration(componentName, componentName2, myUserId());
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getTrustAgentConfiguration(componentName, componentName2, i, this.mParentInstance);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return new ArrayList();
    }

    public void setCrossProfileCallerIdDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setCrossProfileCallerIdDisabled");
        if (this.mService != null) {
            try {
                this.mService.setCrossProfileCallerIdDisabled(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getCrossProfileCallerIdDisabled(ComponentName componentName) {
        throwIfParentInstance("getCrossProfileCallerIdDisabled");
        if (this.mService != null) {
            try {
                return this.mService.getCrossProfileCallerIdDisabled(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean getCrossProfileCallerIdDisabled(UserHandle userHandle) {
        if (this.mService != null) {
            try {
                return this.mService.getCrossProfileCallerIdDisabledForUser(userHandle.getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setCrossProfileContactsSearchDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setCrossProfileContactsSearchDisabled");
        if (this.mService != null) {
            try {
                this.mService.setCrossProfileContactsSearchDisabled(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getCrossProfileContactsSearchDisabled(ComponentName componentName) {
        throwIfParentInstance("getCrossProfileContactsSearchDisabled");
        if (this.mService != null) {
            try {
                return this.mService.getCrossProfileContactsSearchDisabled(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean getCrossProfileContactsSearchDisabled(UserHandle userHandle) {
        if (this.mService != null) {
            try {
                return this.mService.getCrossProfileContactsSearchDisabledForUser(userHandle.getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void startManagedQuickContact(String str, long j, boolean z, long j2, Intent intent) {
        if (this.mService != null) {
            try {
                this.mService.startManagedQuickContact(str, j, z, j2, intent);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void startManagedQuickContact(String str, long j, Intent intent) {
        startManagedQuickContact(str, j, false, 0L, intent);
    }

    public void setBluetoothContactSharingDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setBluetoothContactSharingDisabled");
        if (this.mService != null) {
            try {
                this.mService.setBluetoothContactSharingDisabled(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean getBluetoothContactSharingDisabled(ComponentName componentName) {
        throwIfParentInstance("getBluetoothContactSharingDisabled");
        if (this.mService != null) {
            try {
                return this.mService.getBluetoothContactSharingDisabled(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    public boolean getBluetoothContactSharingDisabled(UserHandle userHandle) {
        if (this.mService != null) {
            try {
                return this.mService.getBluetoothContactSharingDisabledForUser(userHandle.getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    public void addCrossProfileIntentFilter(ComponentName componentName, IntentFilter intentFilter, int i) {
        throwIfParentInstance("addCrossProfileIntentFilter");
        if (this.mService != null) {
            try {
                this.mService.addCrossProfileIntentFilter(componentName, intentFilter, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void clearCrossProfileIntentFilters(ComponentName componentName) {
        throwIfParentInstance("clearCrossProfileIntentFilters");
        if (this.mService != null) {
            try {
                this.mService.clearCrossProfileIntentFilters(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean setPermittedAccessibilityServices(ComponentName componentName, List<String> list) {
        throwIfParentInstance("setPermittedAccessibilityServices");
        if (this.mService != null) {
            try {
                return this.mService.setPermittedAccessibilityServices(componentName, list);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<String> getPermittedAccessibilityServices(ComponentName componentName) {
        throwIfParentInstance("getPermittedAccessibilityServices");
        if (this.mService != null) {
            try {
                return this.mService.getPermittedAccessibilityServices(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public boolean isAccessibilityServicePermittedByAdmin(ComponentName componentName, String str, int i) {
        if (this.mService != null) {
            try {
                return this.mService.isAccessibilityServicePermittedByAdmin(componentName, str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public List<String> getPermittedAccessibilityServices(int i) {
        throwIfParentInstance("getPermittedAccessibilityServices");
        if (this.mService != null) {
            try {
                return this.mService.getPermittedAccessibilityServicesForUser(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public boolean setPermittedInputMethods(ComponentName componentName, List<String> list) {
        throwIfParentInstance("setPermittedInputMethods");
        if (this.mService != null) {
            try {
                return this.mService.setPermittedInputMethods(componentName, list);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<String> getPermittedInputMethods(ComponentName componentName) {
        throwIfParentInstance("getPermittedInputMethods");
        if (this.mService != null) {
            try {
                return this.mService.getPermittedInputMethods(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public boolean isInputMethodPermittedByAdmin(ComponentName componentName, String str, int i) {
        if (this.mService != null) {
            try {
                return this.mService.isInputMethodPermittedByAdmin(componentName, str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @SystemApi
    public List<String> getPermittedInputMethodsForCurrentUser() {
        throwIfParentInstance("getPermittedInputMethodsForCurrentUser");
        if (this.mService != null) {
            try {
                return this.mService.getPermittedInputMethodsForCurrentUser();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public boolean setPermittedCrossProfileNotificationListeners(ComponentName componentName, List<String> list) {
        throwIfParentInstance("setPermittedCrossProfileNotificationListeners");
        if (this.mService != null) {
            try {
                return this.mService.setPermittedCrossProfileNotificationListeners(componentName, list);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<String> getPermittedCrossProfileNotificationListeners(ComponentName componentName) {
        throwIfParentInstance("getPermittedCrossProfileNotificationListeners");
        if (this.mService != null) {
            try {
                return this.mService.getPermittedCrossProfileNotificationListeners(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public boolean isNotificationListenerServicePermitted(String str, int i) {
        if (this.mService != null) {
            try {
                return this.mService.isNotificationListenerServicePermitted(str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return true;
    }

    public List<String> getKeepUninstalledPackages(ComponentName componentName) {
        throwIfParentInstance("getKeepUninstalledPackages");
        if (this.mService != null) {
            try {
                return this.mService.getKeepUninstalledPackages(componentName, this.mContext.getPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void setKeepUninstalledPackages(ComponentName componentName, List<String> list) {
        throwIfParentInstance("setKeepUninstalledPackages");
        if (this.mService != null) {
            try {
                this.mService.setKeepUninstalledPackages(componentName, this.mContext.getPackageName(), list);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @Deprecated
    public UserHandle createUser(ComponentName componentName, String str) {
        return null;
    }

    @Deprecated
    public UserHandle createAndInitializeUser(ComponentName componentName, String str, String str2, ComponentName componentName2, Bundle bundle) {
        return null;
    }

    public UserHandle createAndManageUser(ComponentName componentName, String str, ComponentName componentName2, PersistableBundle persistableBundle, int i) {
        throwIfParentInstance("createAndManageUser");
        try {
            return this.mService.createAndManageUser(componentName, str, componentName2, persistableBundle, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw new UserManager.UserOperationException(e2.getMessage(), e2.errorCode);
        }
    }

    public boolean removeUser(ComponentName componentName, UserHandle userHandle) {
        throwIfParentInstance("removeUser");
        try {
            return this.mService.removeUser(componentName, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean switchUser(ComponentName componentName, UserHandle userHandle) {
        throwIfParentInstance("switchUser");
        try {
            return this.mService.switchUser(componentName, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int startUserInBackground(ComponentName componentName, UserHandle userHandle) {
        throwIfParentInstance("startUserInBackground");
        try {
            return this.mService.startUserInBackground(componentName, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int stopUser(ComponentName componentName, UserHandle userHandle) {
        throwIfParentInstance("stopUser");
        try {
            return this.mService.stopUser(componentName, userHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int logoutUser(ComponentName componentName) {
        throwIfParentInstance("logoutUser");
        try {
            return this.mService.logoutUser(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<UserHandle> getSecondaryUsers(ComponentName componentName) {
        throwIfParentInstance("getSecondaryUsers");
        try {
            return this.mService.getSecondaryUsers(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isEphemeralUser(ComponentName componentName) {
        throwIfParentInstance("isEphemeralUser");
        try {
            return this.mService.isEphemeralUser(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getApplicationRestrictions(ComponentName componentName, String str) {
        throwIfParentInstance("getApplicationRestrictions");
        if (this.mService != null) {
            try {
                return this.mService.getApplicationRestrictions(componentName, this.mContext.getPackageName(), str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void addUserRestriction(ComponentName componentName, String str) {
        throwIfParentInstance("addUserRestriction");
        if (this.mService != null) {
            try {
                this.mService.setUserRestriction(componentName, str, true);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void clearUserRestriction(ComponentName componentName, String str) {
        throwIfParentInstance("clearUserRestriction");
        if (this.mService != null) {
            try {
                this.mService.setUserRestriction(componentName, str, false);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public Bundle getUserRestrictions(ComponentName componentName) {
        Bundle userRestrictions;
        throwIfParentInstance("getUserRestrictions");
        if (this.mService != null) {
            try {
                userRestrictions = this.mService.getUserRestrictions(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } else {
            userRestrictions = null;
        }
        return userRestrictions == null ? new Bundle() : userRestrictions;
    }

    public Intent createAdminSupportIntent(String str) {
        throwIfParentInstance("createAdminSupportIntent");
        if (this.mService != null) {
            try {
                return this.mService.createAdminSupportIntent(str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public boolean setApplicationHidden(ComponentName componentName, String str, boolean z) {
        throwIfParentInstance("setApplicationHidden");
        if (this.mService != null) {
            try {
                return this.mService.setApplicationHidden(componentName, this.mContext.getPackageName(), str, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean isApplicationHidden(ComponentName componentName, String str) {
        throwIfParentInstance("isApplicationHidden");
        if (this.mService != null) {
            try {
                return this.mService.isApplicationHidden(componentName, this.mContext.getPackageName(), str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void enableSystemApp(ComponentName componentName, String str) {
        throwIfParentInstance("enableSystemApp");
        if (this.mService != null) {
            try {
                this.mService.enableSystemApp(componentName, this.mContext.getPackageName(), str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int enableSystemApp(ComponentName componentName, Intent intent) {
        throwIfParentInstance("enableSystemApp");
        if (this.mService != null) {
            try {
                return this.mService.enableSystemAppWithIntent(componentName, this.mContext.getPackageName(), intent);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public boolean installExistingPackage(ComponentName componentName, String str) {
        throwIfParentInstance("installExistingPackage");
        if (this.mService != null) {
            try {
                return this.mService.installExistingPackage(componentName, this.mContext.getPackageName(), str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setAccountManagementDisabled(ComponentName componentName, String str, boolean z) {
        throwIfParentInstance("setAccountManagementDisabled");
        if (this.mService != null) {
            try {
                this.mService.setAccountManagementDisabled(componentName, str, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String[] getAccountTypesWithManagementDisabled() {
        throwIfParentInstance("getAccountTypesWithManagementDisabled");
        return getAccountTypesWithManagementDisabledAsUser(myUserId());
    }

    public String[] getAccountTypesWithManagementDisabledAsUser(int i) {
        if (this.mService != null) {
            try {
                return this.mService.getAccountTypesWithManagementDisabledAsUser(i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void setLockTaskPackages(ComponentName componentName, String[] strArr) throws SecurityException {
        throwIfParentInstance("setLockTaskPackages");
        if (this.mService != null) {
            try {
                this.mService.setLockTaskPackages(componentName, strArr);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public String[] getLockTaskPackages(ComponentName componentName) {
        throwIfParentInstance("getLockTaskPackages");
        if (this.mService != null) {
            try {
                return this.mService.getLockTaskPackages(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return new String[0];
    }

    public boolean isLockTaskPermitted(String str) {
        throwIfParentInstance("isLockTaskPermitted");
        if (this.mService != null) {
            try {
                return this.mService.isLockTaskPermitted(str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setLockTaskFeatures(ComponentName componentName, int i) {
        throwIfParentInstance("setLockTaskFeatures");
        if (this.mService != null) {
            try {
                this.mService.setLockTaskFeatures(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public int getLockTaskFeatures(ComponentName componentName) {
        throwIfParentInstance("getLockTaskFeatures");
        if (this.mService != null) {
            try {
                return this.mService.getLockTaskFeatures(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setGlobalSetting(ComponentName componentName, String str, String str2) {
        throwIfParentInstance("setGlobalSetting");
        if (this.mService != null) {
            try {
                this.mService.setGlobalSetting(componentName, str, str2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setSystemSetting(ComponentName componentName, String str, String str2) {
        throwIfParentInstance("setSystemSetting");
        if (this.mService != null) {
            try {
                this.mService.setSystemSetting(componentName, str, str2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean setTime(ComponentName componentName, long j) {
        throwIfParentInstance("setTime");
        if (this.mService != null) {
            try {
                return this.mService.setTime(componentName, j);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean setTimeZone(ComponentName componentName, String str) {
        throwIfParentInstance("setTimeZone");
        if (this.mService != null) {
            try {
                return this.mService.setTimeZone(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setSecureSetting(ComponentName componentName, String str, String str2) {
        throwIfParentInstance("setSecureSetting");
        if (this.mService != null) {
            try {
                this.mService.setSecureSetting(componentName, str, str2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setRestrictionsProvider(ComponentName componentName, ComponentName componentName2) {
        throwIfParentInstance("setRestrictionsProvider");
        if (this.mService != null) {
            try {
                this.mService.setRestrictionsProvider(componentName, componentName2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setMasterVolumeMuted(ComponentName componentName, boolean z) {
        throwIfParentInstance("setMasterVolumeMuted");
        if (this.mService != null) {
            try {
                this.mService.setMasterVolumeMuted(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean isMasterVolumeMuted(ComponentName componentName) {
        throwIfParentInstance("isMasterVolumeMuted");
        if (this.mService != null) {
            try {
                return this.mService.isMasterVolumeMuted(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public void setUninstallBlocked(ComponentName componentName, String str, boolean z) {
        throwIfParentInstance("setUninstallBlocked");
        if (this.mService != null) {
            try {
                this.mService.setUninstallBlocked(componentName, this.mContext.getPackageName(), str, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean isUninstallBlocked(ComponentName componentName, String str) {
        throwIfParentInstance("isUninstallBlocked");
        if (this.mService != null) {
            try {
                return this.mService.isUninstallBlocked(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean addCrossProfileWidgetProvider(ComponentName componentName, String str) {
        throwIfParentInstance("addCrossProfileWidgetProvider");
        if (this.mService != null) {
            try {
                return this.mService.addCrossProfileWidgetProvider(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean removeCrossProfileWidgetProvider(ComponentName componentName, String str) {
        throwIfParentInstance("removeCrossProfileWidgetProvider");
        if (this.mService != null) {
            try {
                return this.mService.removeCrossProfileWidgetProvider(componentName, str);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<String> getCrossProfileWidgetProviders(ComponentName componentName) {
        throwIfParentInstance("getCrossProfileWidgetProviders");
        if (this.mService != null) {
            try {
                List<String> crossProfileWidgetProviders = this.mService.getCrossProfileWidgetProviders(componentName);
                if (crossProfileWidgetProviders != null) {
                    return crossProfileWidgetProviders;
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return Collections.emptyList();
    }

    public void setUserIcon(ComponentName componentName, Bitmap bitmap) {
        throwIfParentInstance("setUserIcon");
        try {
            this.mService.setUserIcon(componentName, bitmap);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSystemUpdatePolicy(ComponentName componentName, SystemUpdatePolicy systemUpdatePolicy) {
        throwIfParentInstance("setSystemUpdatePolicy");
        if (this.mService != null) {
            try {
                this.mService.setSystemUpdatePolicy(componentName, systemUpdatePolicy);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public SystemUpdatePolicy getSystemUpdatePolicy() {
        throwIfParentInstance("getSystemUpdatePolicy");
        if (this.mService != null) {
            try {
                return this.mService.getSystemUpdatePolicy();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void clearSystemUpdatePolicyFreezePeriodRecord() {
        throwIfParentInstance("clearSystemUpdatePolicyFreezePeriodRecord");
        if (this.mService == null) {
            return;
        }
        try {
            this.mService.clearSystemUpdatePolicyFreezePeriodRecord();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setKeyguardDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setKeyguardDisabled");
        try {
            return this.mService.setKeyguardDisabled(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setStatusBarDisabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setStatusBarDisabled");
        try {
            return this.mService.setStatusBarDisabled(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void notifyPendingSystemUpdate(long j) {
        throwIfParentInstance("notifyPendingSystemUpdate");
        if (this.mService != null) {
            try {
                this.mService.notifyPendingSystemUpdate(SystemUpdateInfo.of(j));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    @SystemApi
    public void notifyPendingSystemUpdate(long j, boolean z) {
        throwIfParentInstance("notifyPendingSystemUpdate");
        if (this.mService != null) {
            try {
                this.mService.notifyPendingSystemUpdate(SystemUpdateInfo.of(j, z));
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public SystemUpdateInfo getPendingSystemUpdate(ComponentName componentName) {
        throwIfParentInstance("getPendingSystemUpdate");
        try {
            return this.mService.getPendingSystemUpdate(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPermissionPolicy(ComponentName componentName, int i) {
        throwIfParentInstance("setPermissionPolicy");
        try {
            this.mService.setPermissionPolicy(componentName, this.mContext.getPackageName(), i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getPermissionPolicy(ComponentName componentName) {
        throwIfParentInstance("getPermissionPolicy");
        try {
            return this.mService.getPermissionPolicy(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setPermissionGrantState(ComponentName componentName, String str, String str2, int i) {
        throwIfParentInstance("setPermissionGrantState");
        try {
            return this.mService.setPermissionGrantState(componentName, this.mContext.getPackageName(), str, str2, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getPermissionGrantState(ComponentName componentName, String str, String str2) {
        throwIfParentInstance("getPermissionGrantState");
        try {
            return this.mService.getPermissionGrantState(componentName, this.mContext.getPackageName(), str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isProvisioningAllowed(String str) {
        throwIfParentInstance("isProvisioningAllowed");
        try {
            return this.mService.isProvisioningAllowed(str, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkProvisioningPreCondition(String str, String str2) {
        try {
            return this.mService.checkProvisioningPreCondition(str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isManagedProfile(ComponentName componentName) {
        throwIfParentInstance("isManagedProfile");
        try {
            return this.mService.isManagedProfile(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSystemOnlyUser(ComponentName componentName) {
        try {
            return this.mService.isSystemOnlyUser(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getWifiMacAddress(ComponentName componentName) {
        throwIfParentInstance("getWifiMacAddress");
        try {
            return this.mService.getWifiMacAddress(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reboot(ComponentName componentName) {
        throwIfParentInstance("reboot");
        try {
            this.mService.reboot(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setShortSupportMessage(ComponentName componentName, CharSequence charSequence) {
        throwIfParentInstance("setShortSupportMessage");
        if (this.mService != null) {
            try {
                this.mService.setShortSupportMessage(componentName, charSequence);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public CharSequence getShortSupportMessage(ComponentName componentName) {
        throwIfParentInstance("getShortSupportMessage");
        if (this.mService != null) {
            try {
                return this.mService.getShortSupportMessage(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public void setLongSupportMessage(ComponentName componentName, CharSequence charSequence) {
        throwIfParentInstance("setLongSupportMessage");
        if (this.mService != null) {
            try {
                this.mService.setLongSupportMessage(componentName, charSequence);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public CharSequence getLongSupportMessage(ComponentName componentName) {
        throwIfParentInstance("getLongSupportMessage");
        if (this.mService != null) {
            try {
                return this.mService.getLongSupportMessage(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public CharSequence getShortSupportMessageForUser(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getShortSupportMessageForUser(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public CharSequence getLongSupportMessageForUser(ComponentName componentName, int i) {
        if (this.mService != null) {
            try {
                return this.mService.getLongSupportMessageForUser(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public DevicePolicyManager getParentProfileInstance(ComponentName componentName) {
        throwIfParentInstance("getParentProfileInstance");
        try {
            if (!this.mService.isManagedProfile(componentName)) {
                throw new SecurityException("The current user does not have a parent profile.");
            }
            return new DevicePolicyManager(this.mContext, this.mService, true);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSecurityLoggingEnabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setSecurityLoggingEnabled");
        try {
            this.mService.setSecurityLoggingEnabled(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSecurityLoggingEnabled(ComponentName componentName) {
        throwIfParentInstance("isSecurityLoggingEnabled");
        try {
            return this.mService.isSecurityLoggingEnabled(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<SecurityLog.SecurityEvent> retrieveSecurityLogs(ComponentName componentName) {
        throwIfParentInstance("retrieveSecurityLogs");
        try {
            ParceledListSlice parceledListSliceRetrieveSecurityLogs = this.mService.retrieveSecurityLogs(componentName);
            if (parceledListSliceRetrieveSecurityLogs != null) {
                return parceledListSliceRetrieveSecurityLogs.getList();
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long forceSecurityLogs() {
        if (this.mService == null) {
            return 0L;
        }
        try {
            return this.mService.forceSecurityLogs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public DevicePolicyManager getParentProfileInstance(UserInfo userInfo) {
        this.mContext.checkSelfPermission(Manifest.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS);
        if (!userInfo.isManagedProfile()) {
            throw new SecurityException("The user " + userInfo.id + " does not have a parent profile.");
        }
        return new DevicePolicyManager(this.mContext, this.mService, true);
    }

    public List<String> setMeteredDataDisabledPackages(ComponentName componentName, List<String> list) {
        throwIfParentInstance("setMeteredDataDisabled");
        if (this.mService != null) {
            try {
                return this.mService.setMeteredDataDisabledPackages(componentName, list);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return list;
    }

    public List<String> getMeteredDataDisabledPackages(ComponentName componentName) {
        throwIfParentInstance("getMeteredDataDisabled");
        if (this.mService != null) {
            try {
                return this.mService.getMeteredDataDisabledPackages(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return new ArrayList();
    }

    public boolean isMeteredDataDisabledPackageForUser(ComponentName componentName, String str, int i) {
        throwIfParentInstance("getMeteredDataDisabledForUser");
        if (this.mService != null) {
            try {
                return this.mService.isMeteredDataDisabledPackageForUser(componentName, str, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<SecurityLog.SecurityEvent> retrievePreRebootSecurityLogs(ComponentName componentName) {
        throwIfParentInstance("retrievePreRebootSecurityLogs");
        try {
            ParceledListSlice parceledListSliceRetrievePreRebootSecurityLogs = this.mService.retrievePreRebootSecurityLogs(componentName);
            if (parceledListSliceRetrievePreRebootSecurityLogs != null) {
                return parceledListSliceRetrievePreRebootSecurityLogs.getList();
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setOrganizationColor(ComponentName componentName, int i) {
        throwIfParentInstance("setOrganizationColor");
        try {
            this.mService.setOrganizationColor(componentName, i | (-16777216));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setOrganizationColorForUser(int i, int i2) {
        try {
            this.mService.setOrganizationColorForUser(i | (-16777216), i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getOrganizationColor(ComponentName componentName) {
        throwIfParentInstance("getOrganizationColor");
        try {
            return this.mService.getOrganizationColor(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getOrganizationColorForUser(int i) {
        try {
            return this.mService.getOrganizationColorForUser(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setOrganizationName(ComponentName componentName, CharSequence charSequence) {
        throwIfParentInstance("setOrganizationName");
        try {
            this.mService.setOrganizationName(componentName, charSequence);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public CharSequence getOrganizationName(ComponentName componentName) {
        throwIfParentInstance("getOrganizationName");
        try {
            return this.mService.getOrganizationName(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public CharSequence getDeviceOwnerOrganizationName() {
        try {
            return this.mService.getDeviceOwnerOrganizationName();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public CharSequence getOrganizationNameForUser(int i) {
        try {
            return this.mService.getOrganizationNameForUser(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public int getUserProvisioningState() {
        throwIfParentInstance("getUserProvisioningState");
        if (this.mService != null) {
            try {
                return this.mService.getUserProvisioningState();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return 0;
    }

    public void setUserProvisioningState(int i, int i2) {
        if (this.mService != null) {
            try {
                this.mService.setUserProvisioningState(i, i2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void setAffiliationIds(ComponentName componentName, Set<String> set) {
        throwIfParentInstance("setAffiliationIds");
        if (set == null) {
            throw new IllegalArgumentException("ids must not be null");
        }
        try {
            this.mService.setAffiliationIds(componentName, new ArrayList(set));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Set<String> getAffiliationIds(ComponentName componentName) {
        throwIfParentInstance("getAffiliationIds");
        try {
            return new ArraySet(this.mService.getAffiliationIds(componentName));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAffiliatedUser() {
        throwIfParentInstance("isAffiliatedUser");
        try {
            return this.mService.isAffiliatedUser();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isUninstallInQueue(String str) {
        try {
            return this.mService.isUninstallInQueue(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void uninstallPackageWithActiveAdmins(String str) {
        try {
            this.mService.uninstallPackageWithActiveAdmins(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forceRemoveActiveAdmin(ComponentName componentName, int i) {
        try {
            this.mService.forceRemoveActiveAdmin(componentName, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isDeviceProvisioned() {
        try {
            return this.mService.isDeviceProvisioned();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setDeviceProvisioningConfigApplied() {
        try {
            this.mService.setDeviceProvisioningConfigApplied();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isDeviceProvisioningConfigApplied() {
        try {
            return this.mService.isDeviceProvisioningConfigApplied();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forceUpdateUserSetupComplete() {
        try {
            this.mService.forceUpdateUserSetupComplete();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void throwIfParentInstance(String str) {
        if (this.mParentInstance) {
            throw new SecurityException(str + " cannot be called on the parent instance");
        }
    }

    public void setBackupServiceEnabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setBackupServiceEnabled");
        try {
            this.mService.setBackupServiceEnabled(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isBackupServiceEnabled(ComponentName componentName) {
        throwIfParentInstance("isBackupServiceEnabled");
        try {
            return this.mService.isBackupServiceEnabled(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setMandatoryBackupTransport(ComponentName componentName, ComponentName componentName2) {
        throwIfParentInstance("setMandatoryBackupTransport");
        try {
            return this.mService.setMandatoryBackupTransport(componentName, componentName2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ComponentName getMandatoryBackupTransport() {
        throwIfParentInstance("getMandatoryBackupTransport");
        try {
            return this.mService.getMandatoryBackupTransport();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNetworkLoggingEnabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setNetworkLoggingEnabled");
        try {
            this.mService.setNetworkLoggingEnabled(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isNetworkLoggingEnabled(ComponentName componentName) {
        throwIfParentInstance("isNetworkLoggingEnabled");
        try {
            return this.mService.isNetworkLoggingEnabled(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<NetworkEvent> retrieveNetworkLogs(ComponentName componentName, long j) {
        throwIfParentInstance("retrieveNetworkLogs");
        try {
            return this.mService.retrieveNetworkLogs(componentName, j);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean bindDeviceAdminServiceAsUser(ComponentName componentName, Intent intent, ServiceConnection serviceConnection, int i, UserHandle userHandle) {
        throwIfParentInstance("bindDeviceAdminServiceAsUser");
        try {
            IServiceConnection serviceDispatcher = this.mContext.getServiceDispatcher(serviceConnection, this.mContext.getMainThreadHandler(), i);
            intent.prepareToLeaveProcess(this.mContext);
            return this.mService.bindDeviceAdminServiceAsUser(componentName, this.mContext.getIApplicationThread(), this.mContext.getActivityToken(), intent, serviceDispatcher, i, userHandle.getIdentifier());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName componentName) {
        throwIfParentInstance("getBindDeviceAdminTargetUsers");
        try {
            return this.mService.getBindDeviceAdminTargetUsers(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getLastSecurityLogRetrievalTime() {
        try {
            return this.mService.getLastSecurityLogRetrievalTime();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getLastBugReportRequestTime() {
        try {
            return this.mService.getLastBugReportRequestTime();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getLastNetworkLogRetrievalTime() {
        try {
            return this.mService.getLastNetworkLogRetrievalTime();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isCurrentInputMethodSetByOwner() {
        try {
            return this.mService.isCurrentInputMethodSetByOwner();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getOwnerInstalledCaCerts(UserHandle userHandle) {
        try {
            return this.mService.getOwnerInstalledCaCerts(userHandle).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearApplicationUserData(ComponentName componentName, String str, Executor executor, OnClearApplicationUserDataListener onClearApplicationUserDataListener) {
        throwIfParentInstance("clearAppData");
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(onClearApplicationUserDataListener);
        try {
            this.mService.clearApplicationUserData(componentName, str, new AnonymousClass1(executor, onClearApplicationUserDataListener));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass1 extends IPackageDataObserver.Stub {
        final Executor val$executor;
        final OnClearApplicationUserDataListener val$listener;

        AnonymousClass1(Executor executor, OnClearApplicationUserDataListener onClearApplicationUserDataListener) {
            this.val$executor = executor;
            this.val$listener = onClearApplicationUserDataListener;
        }

        @Override
        public void onRemoveCompleted(final String str, final boolean z) {
            Executor executor = this.val$executor;
            final OnClearApplicationUserDataListener onClearApplicationUserDataListener = this.val$listener;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    onClearApplicationUserDataListener.onApplicationUserDataCleared(str, z);
                }
            });
        }
    }

    public void setLogoutEnabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setLogoutEnabled");
        try {
            this.mService.setLogoutEnabled(componentName, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isLogoutEnabled() {
        throwIfParentInstance("isLogoutEnabled");
        try {
            return this.mService.isLogoutEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Set<String> getDisallowedSystemApps(ComponentName componentName, int i, String str) {
        try {
            return new ArraySet(this.mService.getDisallowedSystemApps(componentName, i, str));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void transferOwnership(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle) {
        throwIfParentInstance("transferOwnership");
        try {
            this.mService.transferOwnership(componentName, componentName2, persistableBundle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setStartUserSessionMessage(ComponentName componentName, CharSequence charSequence) {
        throwIfParentInstance("setStartUserSessionMessage");
        try {
            this.mService.setStartUserSessionMessage(componentName, charSequence);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setEndUserSessionMessage(ComponentName componentName, CharSequence charSequence) {
        throwIfParentInstance("setEndUserSessionMessage");
        try {
            this.mService.setEndUserSessionMessage(componentName, charSequence);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public CharSequence getStartUserSessionMessage(ComponentName componentName) {
        throwIfParentInstance("getStartUserSessionMessage");
        try {
            return this.mService.getStartUserSessionMessage(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public CharSequence getEndUserSessionMessage(ComponentName componentName) {
        throwIfParentInstance("getEndUserSessionMessage");
        try {
            return this.mService.getEndUserSessionMessage(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int addOverrideApn(ComponentName componentName, ApnSetting apnSetting) {
        throwIfParentInstance("addOverrideApn");
        if (this.mService != null) {
            try {
                return this.mService.addOverrideApn(componentName, apnSetting);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return -1;
    }

    public boolean updateOverrideApn(ComponentName componentName, int i, ApnSetting apnSetting) {
        throwIfParentInstance("updateOverrideApn");
        if (this.mService != null) {
            try {
                return this.mService.updateOverrideApn(componentName, i, apnSetting);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public boolean removeOverrideApn(ComponentName componentName, int i) {
        throwIfParentInstance("removeOverrideApn");
        if (this.mService != null) {
            try {
                return this.mService.removeOverrideApn(componentName, i);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public List<ApnSetting> getOverrideApns(ComponentName componentName) {
        throwIfParentInstance("getOverrideApns");
        if (this.mService != null) {
            try {
                return this.mService.getOverrideApns(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return Collections.emptyList();
    }

    public void setOverrideApnsEnabled(ComponentName componentName, boolean z) {
        throwIfParentInstance("setOverrideApnEnabled");
        if (this.mService != null) {
            try {
                this.mService.setOverrideApnsEnabled(componentName, z);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public boolean isOverrideApnEnabled(ComponentName componentName) {
        throwIfParentInstance("isOverrideApnEnabled");
        if (this.mService != null) {
            try {
                return this.mService.isOverrideApnEnabled(componentName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    public PersistableBundle getTransferOwnershipBundle() {
        throwIfParentInstance("getTransferOwnershipBundle");
        try {
            return this.mService.getTransferOwnershipBundle();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
