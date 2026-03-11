package com.android.settings.password;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;

public class ConfirmDeviceCredentialActivity extends Activity {
    public static final String TAG = ConfirmDeviceCredentialActivity.class.getSimpleName();

    public static class InternalActivity extends ConfirmDeviceCredentialActivity {
    }

    public static Intent createIntent(CharSequence charSequence, CharSequence charSequence2) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", ConfirmDeviceCredentialActivity.class.getName());
        intent.putExtra("android.app.extra.TITLE", charSequence);
        intent.putExtra("android.app.extra.DESCRIPTION", charSequence2);
        return intent;
    }

    @Override
    public void onCreate(Bundle bundle) {
        int userIdFromBundle;
        boolean zLaunchConfirmationActivity;
        super.onCreate(bundle);
        Intent intent = getIntent();
        String stringExtra = intent.getStringExtra("android.app.extra.TITLE");
        String stringExtra2 = intent.getStringExtra("android.app.extra.DESCRIPTION");
        String stringExtra3 = intent.getStringExtra("android.app.extra.ALTERNATE_BUTTON_LABEL");
        boolean zEquals = "android.app.action.CONFIRM_FRP_CREDENTIAL".equals(intent.getAction());
        int credentialOwnerUserId = Utils.getCredentialOwnerUserId(this);
        if (isInternalActivity()) {
            try {
                userIdFromBundle = Utils.getUserIdFromBundle(this, intent.getExtras());
            } catch (SecurityException e) {
                Log.e(TAG, "Invalid intent extra", e);
                userIdFromBundle = credentialOwnerUserId;
            }
        } else {
            userIdFromBundle = credentialOwnerUserId;
        }
        boolean zIsManagedProfile = UserManager.get(this).isManagedProfile(userIdFromBundle);
        if (stringExtra == null && zIsManagedProfile) {
            stringExtra = getTitleFromOrganizationName(userIdFromBundle);
        }
        String str = stringExtra;
        ChooseLockSettingsHelper chooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
        LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
        if (zEquals) {
            zLaunchConfirmationActivity = chooseLockSettingsHelper.launchFrpConfirmationActivity(0, str, stringExtra2, stringExtra3);
        } else if (zIsManagedProfile && isInternalActivity() && !lockPatternUtils.isSeparateProfileChallengeEnabled(userIdFromBundle)) {
            zLaunchConfirmationActivity = chooseLockSettingsHelper.launchConfirmationActivityWithExternalAndChallenge(0, null, str, stringExtra2, true, 0L, userIdFromBundle);
        } else {
            zLaunchConfirmationActivity = chooseLockSettingsHelper.launchConfirmationActivity(0, null, str, stringExtra2, false, true, userIdFromBundle);
        }
        if (!zLaunchConfirmationActivity) {
            Log.d(TAG, "No pattern, password or PIN set.");
            setResult(-1);
        }
        finish();
    }

    private boolean isInternalActivity() {
        return this instanceof InternalActivity;
    }

    private String getTitleFromOrganizationName(int i) {
        CharSequence organizationNameForUser;
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService("device_policy");
        if (devicePolicyManager != null) {
            organizationNameForUser = devicePolicyManager.getOrganizationNameForUser(i);
        } else {
            organizationNameForUser = null;
        }
        if (organizationNameForUser != null) {
            return organizationNameForUser.toString();
        }
        return null;
    }
}
