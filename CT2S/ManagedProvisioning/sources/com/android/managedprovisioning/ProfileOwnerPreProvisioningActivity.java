package com.android.managedprovisioning;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.managedprovisioning.UserConsentDialog;
import java.util.List;

public class ProfileOwnerPreProvisioningActivity extends Activity implements UserConsentDialog.ConsentCallback {
    protected static final ComponentName ALIAS_CHECK_CALLER = new ComponentName("com.android.managedprovisioning", "com.android.managedprovisioning.ProfileOwnerProvisioningActivity");
    protected static final ComponentName ALIAS_NO_CHECK_CALLER = new ComponentName("com.android.managedprovisioning", "com.android.managedprovisioning.ProfileOwnerProvisioningActivityNoCallerCheck");
    private String mMdmPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        View contentView = inflater.inflate(R.layout.user_consent, (ViewGroup) null);
        setContentView(contentView);
        if (!systemHasManagedProfileFeature()) {
            showErrorAndClose(R.string.managed_provisioning_not_supported, "Exiting managed profile provisioning, managed profiles feature is not available");
            return;
        }
        if (Process.myUserHandle().getIdentifier() != 0) {
            showErrorAndClose(R.string.user_is_not_owner, "Exiting managed profile provisioning, calling user is not owner.");
            return;
        }
        try {
            initialize(getIntent());
            setMdmIcon(this.mMdmPackageName);
            boolean hasManageUsersPermission = getComponentName().equals(ALIAS_NO_CHECK_CALLER);
            if (!hasManageUsersPermission) {
                String callingPackage = getCallingPackage();
                if (callingPackage == null) {
                    showErrorAndClose(R.string.managed_provisioning_error_text, "Calling package is null. Was startActivityForResult used to start this activity?");
                    return;
                } else if (!callingPackage.equals(this.mMdmPackageName) && !packageHasManageUsersPermission(callingPackage)) {
                    showErrorAndClose(R.string.managed_provisioning_error_text, "Permission denied, calling package tried to set a different package as profile owner. The system MANAGE_USERS permission is required.");
                    return;
                }
            }
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
            String deviceOwner = dpm.getDeviceOwner();
            if (deviceOwner != null && !deviceOwner.equals(this.mMdmPackageName)) {
                showErrorAndClose(R.string.managed_provisioning_error_text, "Permission denied, profile owner must be in the same package as device owner.");
                return;
            }
            int existingManagedProfileUserId = alreadyHasManagedProfile();
            if (existingManagedProfileUserId != -1) {
                showManagedProfileExistsDialog(existingManagedProfileUserId);
            } else {
                showStartProvisioningScreen();
            }
        } catch (ProvisioningFailedException e) {
            showErrorAndClose(R.string.managed_provisioning_error_text, e.getMessage());
        }
    }

    private void showStartProvisioningScreen() {
        Button positiveButton = (Button) findViewById(R.id.positive_button);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileOwnerPreProvisioningActivity.this.checkEncryptedAndStartProvisioningService();
            }
        });
    }

    private boolean packageHasManageUsersPermission(String pkg) {
        return getPackageManager().checkPermission("android.permission.MANAGE_USERS", pkg) == 0;
    }

    private boolean systemHasManagedProfileFeature() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature("android.software.managed_users");
    }

    private boolean currentLauncherSupportsManagedProfiles() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        PackageManager pm = getPackageManager();
        ResolveInfo launcherResolveInfo = pm.resolveActivity(intent, 65536);
        if (launcherResolveInfo == null) {
            return false;
        }
        try {
            ApplicationInfo launcherAppInfo = getPackageManager().getApplicationInfo(launcherResolveInfo.activityInfo.packageName, 0);
            return versionNumberAtLeastL(launcherAppInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= 21;
    }

    private void setMdmIcon(String packageName) {
        if (packageName != null) {
            PackageManager pm = getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                if (ai != null) {
                    Drawable packageIcon = pm.getApplicationIcon(packageName);
                    ImageView imageView = (ImageView) findViewById(R.id.mdm_icon_view);
                    imageView.setImageDrawable(packageIcon);
                    String appLabel = pm.getApplicationLabel(ai).toString();
                    TextView deviceManagerName = (TextView) findViewById(R.id.device_manager_name);
                    deviceManagerName.setText(appLabel);
                }
            } catch (PackageManager.NameNotFoundException e) {
                ProvisionLogger.loge("Package does not exist. Should never happen.");
            }
        }
    }

    private void initialize(Intent intent) throws ProvisioningFailedException {
        try {
            this.mMdmPackageName = intent.getStringExtra("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME");
            if (TextUtils.isEmpty(this.mMdmPackageName)) {
                throw new ProvisioningFailedException("Missing intent extra: android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME");
            }
            try {
                getPackageManager().getPackageInfo(this.mMdmPackageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                throw new ProvisioningFailedException("Mdm " + this.mMdmPackageName + " is not installed. ", e);
            }
        } catch (ClassCastException e2) {
            throw new ProvisioningFailedException("Extra android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE must be of type PersistableBundle.", e2);
        }
    }

    private void checkEncryptedAndStartProvisioningService() {
        if (EncryptDeviceActivity.isDeviceEncrypted() || SystemProperties.getBoolean("persist.sys.no_req_encrypt", false)) {
            UserConsentDialog.newInstance(1).show(getFragmentManager(), "UserConsentDialogFragment");
            return;
        }
        Bundle resumeExtras = getIntent().getExtras();
        resumeExtras.putString("com.android.managedprovisioning.RESUME_TARGET", "profile_owner");
        Intent encryptIntent = new Intent(this, (Class<?>) EncryptDeviceActivity.class).putExtra("com.android.managedprovisioning.RESUME", resumeExtras);
        startActivityForResult(encryptIntent, 2);
    }

    @Override
    public void onDialogConsent() {
        setupEnvironmentAndProvision();
    }

    @Override
    public void onDialogCancel() {
    }

    private void setupEnvironmentAndProvision() {
        BootReminder.cancelProvisioningReminder(this);
        if (!currentLauncherSupportsManagedProfiles()) {
            showCurrentLauncherInvalid();
        } else {
            startProfileOwnerProvisioning();
        }
    }

    private void pickLauncher() {
        Intent changeLauncherIntent = new Intent("android.settings.HOME_SETTINGS");
        changeLauncherIntent.putExtra("support_managed_profiles", true);
        startActivityForResult(changeLauncherIntent, 1);
    }

    private void startProfileOwnerProvisioning() {
        Intent intent = new Intent(this, (Class<?>) ProfileOwnerProvisioningActivity.class);
        intent.putExtras(getIntent());
        startActivityForResult(intent, 3);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if (resultCode == 0) {
                ProvisionLogger.loge("User canceled device encryption.");
                setResult(0);
                finish();
            }
        } else if (requestCode == 1) {
            if (resultCode == 0) {
                showCurrentLauncherInvalid();
            } else if (resultCode == -1) {
                startProfileOwnerProvisioning();
            }
        }
        if (requestCode == 3) {
            setResult(resultCode);
            finish();
        }
    }

    private void showCurrentLauncherInvalid() {
        new AlertDialog.Builder(this).setCancelable(false).setMessage(R.string.managed_provisioning_not_supported_by_launcher).setNegativeButton(R.string.cancel_provisioning, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                ProfileOwnerPreProvisioningActivity.this.setResult(0);
                ProfileOwnerPreProvisioningActivity.this.finish();
            }
        }).setPositiveButton(R.string.pick_launcher, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ProfileOwnerPreProvisioningActivity.this.pickLauncher();
            }
        }).show();
    }

    public void showErrorAndClose(int resourceId, String logText) {
        ProvisionLogger.loge(logText);
        new AlertDialog.Builder(this).setTitle(R.string.provisioning_error_title).setMessage(getString(resourceId)).setCancelable(false).setPositiveButton(R.string.device_owner_error_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                ProfileOwnerPreProvisioningActivity.this.setResult(0);
                ProfileOwnerPreProvisioningActivity.this.finish();
            }
        }).show();
    }

    int alreadyHasManagedProfile() {
        UserManager userManager = (UserManager) getSystemService("user");
        List<UserInfo> profiles = userManager.getProfiles(getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                return userInfo.getUserHandle().getIdentifier();
            }
        }
        return -1;
    }

    private void showManagedProfileExistsDialog(final int existingManagedProfileUserId) {
        DialogInterface.OnClickListener warningListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DialogInterface.OnClickListener deleteListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which2) {
                        UserManager userManager = (UserManager) ProfileOwnerPreProvisioningActivity.this.getSystemService("user");
                        userManager.removeUser(existingManagedProfileUserId);
                        ProfileOwnerPreProvisioningActivity.this.showStartProvisioningScreen();
                    }
                };
                ProfileOwnerPreProvisioningActivity.this.buildDeleteManagedProfileDialog(ProfileOwnerPreProvisioningActivity.this.getString(R.string.sure_you_want_to_delete_profile), deleteListener).show();
            }
        };
        buildDeleteManagedProfileDialog(getString(R.string.managed_profile_already_present), warningListener).show();
    }

    private AlertDialog buildDeleteManagedProfileDialog(String message, DialogInterface.OnClickListener deleteListener) {
        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileOwnerPreProvisioningActivity.this.finish();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setCancelable(false).setPositiveButton(getString(R.string.delete_profile), deleteListener).setNegativeButton(getString(R.string.cancel_delete_profile), cancelListener);
        return builder.create();
    }

    private class ProvisioningFailedException extends Exception {
        public ProvisioningFailedException(String message) {
            super(message);
        }

        public ProvisioningFailedException(String message, Throwable t) {
            super(message, t);
        }
    }
}
