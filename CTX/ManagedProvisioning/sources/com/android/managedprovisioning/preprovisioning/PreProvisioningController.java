package com.android.managedprovisioning.preprovisioning;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.UserManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.analytics.ProvisioningAnalyticsTracker;
import com.android.managedprovisioning.analytics.TimeLogger;
import com.android.managedprovisioning.common.IllegalProvisioningArgumentException;
import com.android.managedprovisioning.common.MdmPackageInfo;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.CustomizationParams;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.parser.MessageParser;
import com.android.managedprovisioning.preprovisioning.terms.TermsDocument;
import com.android.managedprovisioning.preprovisioning.terms.TermsProvider;
import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PreProvisioningController {
    private final ActivityManager mActivityManager;
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private final EncryptionController mEncryptionController;
    private final KeyguardManager mKeyguardManager;
    private final MessageParser mMessageParser;
    private final PackageManager mPackageManager;
    private ProvisioningParams mParams;
    private final PersistentDataBlockManager mPdbManager;
    private final ProvisioningAnalyticsTracker mProvisioningAnalyticsTracker;
    private final SettingsFacade mSettingsFacade;
    private final TimeLogger mTimeLogger;
    private final Ui mUi;
    private final UserManager mUserManager;
    private final Utils mUtils;

    interface Ui {
        void initiateUi(int i, int i2, String str, Drawable drawable, boolean z, boolean z2, List<String> list, CustomizationParams customizationParams);

        void requestEncryption(ProvisioningParams provisioningParams);

        void requestWifiPick();

        void showCurrentLauncherInvalid();

        void showDeleteManagedProfileDialog(ComponentName componentName, String str, int i);

        void showErrorAndClose(Integer num, int i, String str);

        void startProvisioning(int i, ProvisioningParams provisioningParams);
    }

    public PreProvisioningController(Context context, Ui ui) {
        this(context, ui, new TimeLogger(context, 520), new MessageParser(context), new Utils(), new SettingsFacade(), EncryptionController.getInstance(context));
    }

    @VisibleForTesting
    PreProvisioningController(Context context, Ui ui, TimeLogger timeLogger, MessageParser messageParser, Utils utils, SettingsFacade settingsFacade, EncryptionController encryptionController) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "Context must not be null");
        this.mUi = (Ui) Preconditions.checkNotNull(ui, "Ui must not be null");
        this.mTimeLogger = (TimeLogger) Preconditions.checkNotNull(timeLogger, "Time logger must not be null");
        this.mMessageParser = (MessageParser) Preconditions.checkNotNull(messageParser, "MessageParser must not be null");
        this.mSettingsFacade = (SettingsFacade) Preconditions.checkNotNull(settingsFacade);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils, "Utils must not be null");
        this.mEncryptionController = (EncryptionController) Preconditions.checkNotNull(encryptionController, "EncryptionController must not be null");
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mPackageManager = this.mContext.getPackageManager();
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mPdbManager = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
        this.mProvisioningAnalyticsTracker = ProvisioningAnalyticsTracker.getInstance();
    }

    public void initiateProvisioning(Intent intent, ProvisioningParams provisioningParams, String str) {
        int iAlreadyHasManagedProfile;
        this.mProvisioningAnalyticsTracker.logProvisioningSessionStarted(this.mContext);
        if (!tryParseParameters(intent, provisioningParams) || !checkFactoryResetProtection(this.mParams, str) || !verifyActionAndCaller(intent, str) || !checkDevicePolicyPreconditions()) {
            return;
        }
        boolean z = false;
        if (isProfileOwnerProvisioning() && (iAlreadyHasManagedProfile = this.mUtils.alreadyHasManagedProfile(this.mContext)) != -1) {
            this.mUi.showDeleteManagedProfileDialog(this.mDevicePolicyManager.getProfileOwnerAsUser(iAlreadyHasManagedProfile), this.mDevicePolicyManager.getProfileOwnerNameAsUser(iAlreadyHasManagedProfile), iAlreadyHasManagedProfile);
            z = true;
        }
        if (!isProfileOwnerProvisioning() && !this.mUtils.isConnectedToNetwork(this.mContext) && this.mParams.wifiInfo == null && this.mParams.deviceAdminDownloadInfo != null && !this.mParams.useMobileData) {
            if (this.mKeyguardManager.inKeyguardRestrictedInputMode()) {
                ProvisionLogger.logi("Cannot pick wifi because the screen is locked.");
            } else {
                if (canRequestWifiPick()) {
                    this.mUi.requestWifiPick();
                    return;
                }
                this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "Cannot pick WiFi because there is no handler to the intent");
            }
        }
        this.mTimeLogger.start();
        this.mProvisioningAnalyticsTracker.logPreProvisioningStarted(this.mContext, intent);
        if (this.mParams.skipUserConsent || isSilentProvisioningForTestingDeviceOwner() || isSilentProvisioningForTestingManagedProfile()) {
            if (!z) {
                continueProvisioningAfterUserConsent();
                return;
            }
            return;
        }
        CustomizationParams customizationParamsCreateInstance = CustomizationParams.createInstance(this.mParams, this.mContext, this.mUtils);
        if (isProfileOwnerProvisioning()) {
            this.mUi.initiateUi(R.layout.intro_profile_owner, R.string.setup_profile, null, null, true, this.mDevicePolicyManager.isDeviceManaged(), getDisclaimerHeadings(), customizationParamsCreateInstance);
            return;
        }
        String strInferDeviceAdminPackageName = this.mParams.inferDeviceAdminPackageName();
        MdmPackageInfo mdmPackageInfoCreateFromPackageName = MdmPackageInfo.createFromPackageName(this.mContext, strInferDeviceAdminPackageName);
        if (mdmPackageInfoCreateFromPackageName != null) {
            strInferDeviceAdminPackageName = mdmPackageInfoCreateFromPackageName.appLabel;
        } else if (this.mParams.deviceAdminLabel != null) {
            strInferDeviceAdminPackageName = this.mParams.deviceAdminLabel;
        }
        this.mUi.initiateUi(R.layout.intro_device_owner, R.string.setup_device, strInferDeviceAdminPackageName, mdmPackageInfoCreateFromPackageName != null ? mdmPackageInfoCreateFromPackageName.packageIcon : getDeviceAdminIconDrawable(this.mParams.deviceAdminIconFilePath), false, false, getDisclaimerHeadings(), customizationParamsCreateInstance);
    }

    private List<String> getDisclaimerHeadings() {
        return (List) new TermsProvider(this.mContext, new StoreUtils.TextFileReader() {
            @Override
            public final String read(File file) {
                return StoreUtils.readString(file);
            }
        }, this.mUtils).getTerms(this.mParams, 1).stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((TermsDocument) obj).getHeading();
            }
        }).collect(Collectors.toList());
    }

    private Drawable getDeviceAdminIconDrawable(String str) {
        Bitmap bitmapDecodeFile;
        if (str == null || (bitmapDecodeFile = BitmapFactory.decodeFile(this.mParams.deviceAdminIconFilePath)) == null) {
            return null;
        }
        return new BitmapDrawable(this.mContext.getResources(), bitmapDecodeFile);
    }

    public void continueProvisioningAfterUserConsent() {
        if (isEncryptionRequired()) {
            if (this.mDevicePolicyManager.getStorageEncryptionStatus() == 0) {
                this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.device_doesnt_allow_encryption_contact_admin, "This device does not support encryption, and android.app.extra.PROVISIONING_SKIP_ENCRYPTION was not passed.");
                return;
            } else {
                this.mUi.requestEncryption(this.mParams);
                return;
            }
        }
        if (isProfileOwnerProvisioning()) {
            if (!this.mUtils.currentLauncherSupportsManagedProfiles(this.mContext)) {
                this.mUi.showCurrentLauncherInvalid();
                return;
            }
            this.mEncryptionController.cancelEncryptionReminder();
            stopTimeLogger();
            this.mUi.startProvisioning(this.mUserManager.getUserHandle(), this.mParams);
            return;
        }
        this.mEncryptionController.cancelEncryptionReminder();
        if (isMeatUserCreationRequired(this.mParams.provisioningAction)) {
            new CreatePrimaryUserTask().execute(new Void[0]);
        } else {
            stopTimeLogger();
            this.mUi.startProvisioning(this.mUserManager.getUserHandle(), this.mParams);
        }
    }

    @VisibleForTesting
    boolean checkFactoryResetProtection(ProvisioningParams provisioningParams, String str) {
        if (skipFactoryResetProtectionCheck(provisioningParams, str) || !factoryResetProtected()) {
            return true;
        }
        this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.device_has_reset_protection_contact_admin, "Factory reset protection blocks provisioning.");
        return false;
    }

    private boolean skipFactoryResetProtectionCheck(ProvisioningParams provisioningParams, String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String string = this.mContext.getResources().getString(android.R.string.anr_process);
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(str, 0);
            if (packageInfo == null || packageInfo.applicationInfo == null || !packageInfo.applicationInfo.isSystemApp() || TextUtils.isEmpty(string) || !str.equals(string) || provisioningParams == null) {
                return false;
            }
            return provisioningParams.startedByTrustedSource;
        } catch (PackageManager.NameNotFoundException e) {
            ProvisionLogger.loge("Calling package not found.", e);
            return false;
        }
    }

    @VisibleForTesting
    protected boolean checkDevicePolicyPreconditions() {
        int iCheckProvisioningPreCondition;
        if (isSilentProvisioningForTestingDeviceOwner() || (iCheckProvisioningPreCondition = this.mDevicePolicyManager.checkProvisioningPreCondition(this.mParams.provisioningAction, this.mParams.inferDeviceAdminPackageName())) == 0) {
            return true;
        }
        this.mProvisioningAnalyticsTracker.logProvisioningNotAllowed(this.mContext, iCheckProvisioningPreCondition);
        showProvisioningErrorAndClose(this.mParams.provisioningAction, iCheckProvisioningPreCondition);
        return false;
    }

    private boolean tryParseParameters(Intent intent, ProvisioningParams provisioningParams) {
        if (provisioningParams == null) {
            try {
                provisioningParams = this.mMessageParser.parse(intent);
            } catch (IllegalProvisioningArgumentException e) {
                this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, e.getMessage());
                return false;
            }
        }
        this.mParams = provisioningParams;
        return true;
    }

    @VisibleForTesting
    protected boolean verifyActionAndCaller(Intent intent, String str) {
        if (verifyActionAndCallerInner(intent, str)) {
            return true;
        }
        this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "invalid intent or calling package");
        return false;
    }

    private boolean verifyActionAndCallerInner(Intent intent, String str) {
        if ("com.android.managedprovisioning.action.RESUME_PROVISIONING".equals(intent.getAction())) {
            return verifyActivityAlias(intent, "PreProvisioningActivityAfterEncryption");
        }
        if ("android.nfc.action.NDEF_DISCOVERED".equals(intent.getAction())) {
            return verifyActivityAlias(intent, "PreProvisioningActivityViaNfc");
        }
        if ("android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE".equals(intent.getAction())) {
            return verifyActivityAlias(intent, "PreProvisioningActivityViaTrustedApp");
        }
        return verifyCaller(str);
    }

    private boolean verifyActivityAlias(Intent intent, String str) {
        ComponentName component = intent.getComponent();
        if (component == null || component.getClassName() == null) {
            ProvisionLogger.loge("null class in component when verifying activity alias " + str);
            return false;
        }
        if (!component.getClassName().endsWith(str)) {
            ProvisionLogger.loge("Looking for activity alias " + str + ", but got " + component.getClassName());
            return false;
        }
        return true;
    }

    private boolean verifyCaller(String str) {
        if (str == null) {
            ProvisionLogger.loge("Calling package is null. Was startActivityForResult used to start this activity?");
            return false;
        }
        if (!str.equals(this.mParams.inferDeviceAdminPackageName())) {
            ProvisionLogger.loge("Permission denied, calling package tried to set a different package as owner. ");
            return false;
        }
        return true;
    }

    private boolean isEncryptionRequired() {
        return !this.mParams.skipEncryption && this.mUtils.isEncryptionRequired();
    }

    private boolean isSilentProvisioningForTestingDeviceOwner() {
        ComponentName deviceOwnerComponentOnCallingUser = this.mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser();
        ComponentName componentName = this.mParams.deviceAdminComponentName;
        String str = this.mParams.provisioningAction;
        return ((str.hashCode() == -920528692 && str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) ? (byte) 0 : (byte) -1) == 0 && isPackageTestOnly() && deviceOwnerComponentOnCallingUser != null && componentName != null && deviceOwnerComponentOnCallingUser.equals(componentName);
    }

    private boolean isSilentProvisioningForTestingManagedProfile() {
        return "android.app.action.PROVISION_MANAGED_PROFILE".equals(this.mParams.provisioningAction) && isPackageTestOnly();
    }

    private boolean isPackageTestOnly() {
        return this.mUtils.isPackageTestOnly(this.mContext.getPackageManager(), this.mParams.inferDeviceAdminPackageName(), this.mUserManager.getUserHandle());
    }

    private boolean factoryResetProtected() {
        if (this.mSettingsFacade.isDeviceProvisioned(this.mContext)) {
            ProvisionLogger.logd("Device is provisioned, FRP not required.");
            return false;
        }
        if (this.mPdbManager == null) {
            ProvisionLogger.logd("Reset protection not supported.");
            return false;
        }
        int dataBlockSize = this.mPdbManager.getDataBlockSize();
        ProvisionLogger.logd("Data block size: " + dataBlockSize);
        return dataBlockSize > 0;
    }

    public boolean isMeatUserCreationRequired(String str) {
        if (!this.mUtils.isSplitSystemUser() || !"android.app.action.PROVISION_MANAGED_DEVICE".equals(str)) {
            return false;
        }
        List users = this.mUserManager.getUsers();
        if (users.size() <= 1) {
            return true;
        }
        this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "Cannot start Device Owner Provisioning because there are already " + users.size() + " users");
        return false;
    }

    private boolean canRequestWifiPick() {
        return this.mPackageManager.resolveActivity(this.mUtils.getWifiPickIntent(), 0) != null;
    }

    public boolean isProfileOwnerProvisioning() {
        return this.mUtils.isProfileOwnerAction(this.mParams.provisioningAction);
    }

    public ProvisioningParams getParams() {
        return this.mParams;
    }

    public void stopTimeLogger() {
        this.mTimeLogger.stop();
    }

    public void logPreProvisioningCancelled() {
        this.mProvisioningAnalyticsTracker.logProvisioningCancelled(this.mContext, 1);
    }

    public void removeUser(int i) {
        this.mUserManager.removeUserEvenWhenDisallowed(i);
    }

    public void checkResumeSilentProvisioning() {
        if (this.mParams.skipUserConsent || isSilentProvisioningForTestingDeviceOwner() || isSilentProvisioningForTestingManagedProfile()) {
            continueProvisioningAfterUserConsent();
        }
    }

    private class CreatePrimaryUserTask extends AsyncTask<Void, Void, UserInfo> {
        private CreatePrimaryUserTask() {
        }

        @Override
        protected UserInfo doInBackground(Void... voidArr) {
            UserInfo userInfoCreateUser = PreProvisioningController.this.mUserManager.createUser(PreProvisioningController.this.mContext.getString(R.string.default_first_meat_user_name), 3);
            if (userInfoCreateUser != null) {
                ProvisionLogger.logi("Created user " + userInfoCreateUser.id + " to hold the device owner");
            }
            return userInfoCreateUser;
        }

        @Override
        protected void onPostExecute(UserInfo userInfo) {
            if (userInfo == null) {
                PreProvisioningController.this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "Could not create user to hold the device owner");
                return;
            }
            PreProvisioningController.this.mActivityManager.switchUser(userInfo.id);
            PreProvisioningController.this.stopTimeLogger();
            PreProvisioningController.this.mUi.startProvisioning(userInfo.id, PreProvisioningController.this.mParams);
        }
    }

    private void showProvisioningErrorAndClose(String str, int i) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -920528692) {
            if (iHashCode != -514404415) {
                if (iHashCode != -340845101) {
                    b = (iHashCode == 631897778 && str.equals("android.app.action.PROVISION_MANAGED_SHAREABLE_DEVICE")) ? (byte) 3 : (byte) -1;
                } else if (str.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    b = 1;
                }
            } else if (str.equals("android.app.action.PROVISION_MANAGED_USER")) {
                b = 0;
            }
        } else if (str.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
            b = 2;
        }
        switch (b) {
            case 0:
                this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "Exiting managed user provisioning, setup incomplete");
                break;
            case EncryptionController.NOTIFICATION_ID:
                showManagedProfileErrorAndClose(i);
                break;
            case 2:
            case 3:
                showDeviceOwnerErrorAndClose(i);
                break;
        }
    }

    private void showManagedProfileErrorAndClose(int i) {
        UserInfo userInfo = this.mUserManager.getUserInfo(this.mUserManager.getUserHandle());
        ProvisionLogger.logw("DevicePolicyManager.checkProvisioningPreCondition returns code: " + i);
        if (i != 9) {
            if (i == 11) {
                if (!userInfo.canHaveProfile()) {
                    this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_add_work_profile), R.string.work_profile_cant_be_added_contact_admin, "Exiting managed profile provisioning, calling user cannot have managed profiles");
                    return;
                } else if (isRemovingManagedProfileDisallowed()) {
                    this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_replace_or_remove_work_profile), R.string.for_help_contact_admin, "Exiting managed profile provisioning, removing managed profile is disallowed");
                    return;
                } else {
                    this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_add_work_profile), R.string.work_profile_cant_be_added_contact_admin, "Exiting managed profile provisioning, cannot add more managed profiles");
                    return;
                }
            }
            switch (i) {
                case 14:
                    this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_add_work_profile), R.string.contact_your_admin_for_help, "Exiting managed profile provisioning, a device owner exists");
                    break;
                case 15:
                    break;
                default:
                    this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_add_work_profile), R.string.contact_your_admin_for_help, "Managed profile provisioning not allowed for an unknown reason, code: " + i);
                    break;
            }
        }
        this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_add_work_profile), R.string.work_profile_cant_be_added_contact_admin, "Exiting managed profile provisioning, managed profiles feature is not available");
    }

    private boolean isRemovingManagedProfileDisallowed() {
        return this.mUtils.alreadyHasManagedProfile(this.mContext) != -1 && this.mUserManager.hasUserRestriction("no_remove_managed_profile");
    }

    private void showDeviceOwnerErrorAndClose(int i) {
        if (i == 1 || i == 4) {
            this.mUi.showErrorAndClose(Integer.valueOf(R.string.device_already_set_up), R.string.if_questions_contact_admin, "Device already provisioned.");
            return;
        }
        if (i == 7) {
            this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "Device owner can only be set up for USER_SYSTEM.");
        } else if (i == 12) {
            this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "System User Device owner can only be set on a split-user system.");
        } else {
            this.mUi.showErrorAndClose(Integer.valueOf(R.string.cant_set_up_device), R.string.contact_your_admin_for_help, "Device Owner provisioning not allowed for an unknown reason.");
        }
    }
}
