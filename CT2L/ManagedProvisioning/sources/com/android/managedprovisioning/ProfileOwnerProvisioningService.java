package com.android.managedprovisioning;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import java.io.IOException;

public class ProfileOwnerProvisioningService extends Service {
    private AccountManager mAccountManager;
    private Account mAccountToMigrate;
    private ComponentName mActiveAdminComponentName;
    private PersistableBundle mAdminExtrasBundle;
    private IPackageManager mIpm;
    private UserInfo mManagedProfileUserInfo;
    private String mMdmPackageName;
    private UserManager mUserManager;
    private AsyncTask<Intent, Void, Void> runnerTask;
    private String mLastErrorMessage = null;
    private boolean mDone = false;
    private boolean mCancelInFuture = false;

    private class RunnerTask extends AsyncTask<Intent, Void, Void> {
        private RunnerTask() {
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            ProfileOwnerProvisioningService.this.initialize(intents[0]);
            ProfileOwnerProvisioningService.this.startManagedProfileProvisioning();
            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mIpm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        this.mAccountManager = (AccountManager) getSystemService("account");
        this.mUserManager = (UserManager) getSystemService("user");
        this.runnerTask = new RunnerTask();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("com.android.managedprovisioning.CANCEL_PROVISIONING".equals(intent.getAction())) {
            ProvisionLogger.logd("Cancelling profile owner provisioning service");
            cancelProvisioning();
        } else {
            ProvisionLogger.logd("Starting profile owner provisioning service");
            try {
                this.runnerTask.execute(intent);
            } catch (IllegalStateException e) {
                ProvisionLogger.logd("ProfileOwnerProvisioningService: Provisioning already started, second provisioning intent not being processed, only reporting status.");
                reportStatus();
            }
        }
        return 2;
    }

    private void reportStatus() {
        if (this.mLastErrorMessage != null) {
            sendError();
        }
        synchronized (this) {
            if (this.mDone) {
                notifyActivityOfSuccess();
            }
        }
    }

    private void cancelProvisioning() {
        synchronized (this) {
            if (!this.mDone) {
                this.mCancelInFuture = true;
            } else {
                cleanup();
            }
        }
    }

    private void initialize(Intent intent) {
        this.mMdmPackageName = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME");
        this.mAccountToMigrate = (Account) intent.getParcelableExtra("android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE");
        if (this.mAccountToMigrate != null) {
            ProvisionLogger.logi("Migrating account to managed profile");
        }
        this.mAdminExtrasBundle = (PersistableBundle) intent.getParcelableExtra("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE");
        this.mActiveAdminComponentName = getAdminReceiverComponent(this.mMdmPackageName);
    }

    private ComponentName getAdminReceiverComponent(String packageName) {
        ComponentName adminReceiverComponent = null;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(packageName, 2);
            ActivityInfo[] arr$ = pi.receivers;
            int len$ = arr$.length;
            int i$ = 0;
            ComponentName adminReceiverComponent2 = null;
            while (i$ < len$) {
                try {
                    ActivityInfo ai = arr$[i$];
                    i$++;
                    adminReceiverComponent2 = (TextUtils.isEmpty(ai.permission) || !ai.permission.equals("android.permission.BIND_DEVICE_ADMIN")) ? adminReceiverComponent2 : new ComponentName(packageName, ai.name);
                } catch (PackageManager.NameNotFoundException e) {
                    adminReceiverComponent = adminReceiverComponent2;
                    error("Error: The provided mobile device management package does not define a deviceadmin receiver component in its manifest.");
                    return adminReceiverComponent;
                }
            }
            return adminReceiverComponent2;
        } catch (PackageManager.NameNotFoundException e2) {
        }
    }

    private void startManagedProfileProvisioning() {
        ProvisionLogger.logd("Starting managed profile provisioning");
        createProfile(getString(R.string.default_managed_profile_name));
        if (this.mManagedProfileUserInfo != null) {
            new DeleteNonRequiredAppsTask(this, this.mMdmPackageName, this.mManagedProfileUserInfo.id, R.array.required_apps_managed_profile, R.array.vendor_required_apps_managed_profile, true, true, new DeleteNonRequiredAppsTask.Callback() {
                @Override
                public void onSuccess() {
                    ProfileOwnerProvisioningService.this.setUpProfileAndFinish();
                }

                @Override
                public void onError() {
                    ProfileOwnerProvisioningService.this.error("Delete non required apps task failed.");
                }
            }).run();
        }
    }

    private void setUpProfileAndFinish() {
        installMdmOnManagedProfile();
        setMdmAsActiveAdmin();
        setMdmAsManagedProfileOwner();
        CrossProfileIntentFiltersHelper.setFilters(getPackageManager(), getUserId(), this.mManagedProfileUserInfo.id);
        if (!startManagedProfile(this.mManagedProfileUserInfo.id)) {
            error("Could not start user in background");
            return;
        }
        copyAccount(this.mAccountToMigrate);
        synchronized (this) {
            this.mDone = true;
            if (this.mCancelInFuture) {
                cleanup();
            } else {
                notifyActivityOfSuccess();
            }
        }
    }

    private boolean startManagedProfile(int userId) {
        ProvisionLogger.logd("Starting user in background");
        IActivityManager iActivityManager = ActivityManagerNative.getDefault();
        try {
            return iActivityManager.startUserInBackground(userId);
        } catch (RemoteException neverThrown) {
            ProvisionLogger.loge("This should not happen.", neverThrown);
            return false;
        }
    }

    private void notifyActivityOfSuccess() {
        Intent completeIntent = new Intent("android.app.action.PROFILE_PROVISIONING_COMPLETE");
        completeIntent.setComponent(this.mActiveAdminComponentName);
        completeIntent.addFlags(268435488);
        if (this.mAdminExtrasBundle != null) {
            completeIntent.putExtra("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", this.mAdminExtrasBundle);
        }
        Intent successIntent = new Intent("com.android.managedprovisioning.provisioning_success");
        successIntent.putExtra("com.android.managedprovisioning.extra.profile_user_id", this.mManagedProfileUserInfo.id);
        successIntent.putExtra("com.android.managedprovisioning.extra.pending_success_intent", completeIntent);
        successIntent.putExtra("com.android.managedprovisioning.extra.profile_user_serial_number", this.mManagedProfileUserInfo.serialNumber);
        LocalBroadcastManager.getInstance(this).sendBroadcast(successIntent);
    }

    private void copyAccount(Account account) {
        if (account == null) {
            ProvisionLogger.logd("No account to migrate to the managed profile.");
            return;
        }
        ProvisionLogger.logd("Attempting to copy account to user " + this.mManagedProfileUserInfo.id);
        try {
            if (((Boolean) this.mAccountManager.copyAccountToUser(account, this.mManagedProfileUserInfo.getUserHandle(), null, null).getResult()).booleanValue()) {
                ProvisionLogger.logi("Copied account to user " + this.mManagedProfileUserInfo.id);
            } else {
                ProvisionLogger.loge("Could not copy account to user " + this.mManagedProfileUserInfo.id);
            }
        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
            ProvisionLogger.logw("Exception copying account to user " + this.mManagedProfileUserInfo.id, e);
        }
    }

    private void createProfile(String profileName) {
        ProvisionLogger.logd("Creating managed profile with name " + profileName);
        this.mManagedProfileUserInfo = this.mUserManager.createProfileForUser(profileName, 96, Process.myUserHandle().getIdentifier());
        if (this.mManagedProfileUserInfo == null) {
            if (UserManager.getMaxSupportedUsers() == this.mUserManager.getUserCount()) {
                error("Profile creation failed, maximum number of users reached.");
            } else {
                error("Couldn't create profile. Reason unknown.");
            }
        }
    }

    private void installMdmOnManagedProfile() {
        ProvisionLogger.logd("Installing mobile device management app " + this.mMdmPackageName + " on managed profile");
        try {
            int status = this.mIpm.installExistingPackageAsUser(this.mMdmPackageName, this.mManagedProfileUserInfo.id);
            switch (status) {
                case -111:
                    error("Could not install mobile device management app on managed profile because the user is restricted");
                    error("Could not install mobile device management app on managed profile because the package could not be found");
                    error("Could not install mobile device management app on managed profile. Unknown status: " + status);
                    break;
                case -3:
                    error("Could not install mobile device management app on managed profile because the package could not be found");
                    error("Could not install mobile device management app on managed profile. Unknown status: " + status);
                    break;
                case 1:
                    break;
                default:
                    error("Could not install mobile device management app on managed profile. Unknown status: " + status);
                    break;
            }
        } catch (RemoteException neverThrown) {
            ProvisionLogger.loge("This should not happen.", neverThrown);
        }
    }

    private void setMdmAsManagedProfileOwner() {
        ProvisionLogger.logd("Setting package " + this.mMdmPackageName + " as managed profile owner.");
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
        if (!dpm.setProfileOwner(this.mActiveAdminComponentName, this.mMdmPackageName, this.mManagedProfileUserInfo.id)) {
            ProvisionLogger.logw("Could not set profile owner.");
            error("Could not set profile owner.");
        }
    }

    private void setMdmAsActiveAdmin() {
        ProvisionLogger.logd("Setting package " + this.mMdmPackageName + " as active admin.");
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
        dpm.setActiveAdmin(this.mActiveAdminComponentName, true, this.mManagedProfileUserInfo.id);
    }

    private void error(String dialogMessage) {
        this.mLastErrorMessage = dialogMessage;
        sendError();
    }

    private void sendError() {
        Intent intent = new Intent("com.android.managedprovisioning.error");
        intent.putExtra("ProvisioingErrorLogMessage", this.mLastErrorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void cleanup() {
        if (this.mManagedProfileUserInfo != null) {
            ProvisionLogger.logd("Removing managed profile");
            this.mUserManager.removeUser(this.mManagedProfileUserInfo.id);
        }
        Intent cancelIntent = new Intent("com.android.managedprovisioning.cancelled");
        LocalBroadcastManager.getInstance(this).sendBroadcast(cancelIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
