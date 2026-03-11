package com.android.settings.password;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.UserManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.password.ConfirmLockPassword;
import com.android.settings.password.ConfirmLockPattern;

public final class ChooseLockSettingsHelper {
    private Activity mActivity;
    private Fragment mFragment;

    @VisibleForTesting
    LockPatternUtils mLockPatternUtils;

    public ChooseLockSettingsHelper(Activity activity) {
        this.mActivity = activity;
        this.mLockPatternUtils = new LockPatternUtils(activity);
    }

    public ChooseLockSettingsHelper(Activity activity, Fragment fragment) {
        this(activity);
        this.mFragment = fragment;
    }

    public LockPatternUtils utils() {
        return this.mLockPatternUtils;
    }

    public boolean launchConfirmationActivity(int i, CharSequence charSequence) {
        return launchConfirmationActivity(i, charSequence, (CharSequence) null, (CharSequence) null, false, false);
    }

    public boolean launchConfirmationActivity(int i, CharSequence charSequence, boolean z) {
        return launchConfirmationActivity(i, charSequence, (CharSequence) null, (CharSequence) null, z, false);
    }

    public boolean launchConfirmationActivity(int i, CharSequence charSequence, boolean z, int i2) {
        return launchConfirmationActivity(i, charSequence, null, null, z, false, false, 0L, Utils.enforceSameOwner(this.mActivity, i2));
    }

    boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, boolean z, boolean z2) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, z, z2, false, 0L, Utils.getCredentialOwnerUserId(this.mActivity));
    }

    boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, boolean z, boolean z2, int i2) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, z, z2, false, 0L, Utils.enforceSameOwner(this.mActivity, i2));
    }

    public boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, long j) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, true, false, true, j, Utils.getCredentialOwnerUserId(this.mActivity));
    }

    public boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, long j, int i2) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, true, false, true, j, Utils.enforceSameOwner(this.mActivity, i2));
    }

    public boolean launchConfirmationActivityWithExternalAndChallenge(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, boolean z, long j, int i2) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, false, z, true, j, Utils.enforceSameOwner(this.mActivity, i2));
    }

    public boolean launchConfirmationActivityForAnyUser(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, int i2) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("allow_any_user", true);
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, false, false, true, 0L, i2, bundle);
    }

    private boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, boolean z, boolean z2, boolean z3, long j, int i2) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, z, z2, z3, j, i2, null, null);
    }

    private boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, boolean z, boolean z2, boolean z3, long j, int i2, Bundle bundle) {
        return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, z, z2, z3, j, i2, null, bundle);
    }

    public boolean launchFrpConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        return launchConfirmationActivity(i, null, charSequence, charSequence2, false, true, false, 0L, -9999, charSequence3, null);
    }

    private boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, boolean z, boolean z2, boolean z3, long j, int i2, CharSequence charSequence4, Bundle bundle) {
        Class<?> cls;
        Class<?> cls2;
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserManager.get(this.mActivity).getCredentialOwnerProfile(i2));
        if (keyguardStoredPasswordQuality == 65536) {
            if (z || z3) {
                cls = ConfirmLockPattern.InternalActivity.class;
            } else {
                cls = ConfirmLockPattern.class;
            }
            return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, cls, z, z2, z3, j, i2, charSequence4, bundle);
        }
        if (keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608 || keyguardStoredPasswordQuality == 262144 || keyguardStoredPasswordQuality == 327680 || keyguardStoredPasswordQuality == 393216 || keyguardStoredPasswordQuality == 524288) {
            if (z || z3) {
                cls2 = ConfirmLockPassword.InternalActivity.class;
            } else {
                cls2 = ConfirmLockPassword.class;
            }
            return launchConfirmationActivity(i, charSequence, charSequence2, charSequence3, cls2, z, z2, z3, j, i2, charSequence4, bundle);
        }
        return false;
    }

    private boolean launchConfirmationActivity(int i, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, Class<?> cls, boolean z, boolean z2, boolean z3, long j, int i2, CharSequence charSequence4, Bundle bundle) {
        Intent intent = new Intent();
        intent.putExtra("com.android.settings.ConfirmCredentials.title", charSequence);
        intent.putExtra("com.android.settings.ConfirmCredentials.header", charSequence2);
        intent.putExtra("com.android.settings.ConfirmCredentials.details", charSequence3);
        intent.putExtra("com.android.settings.ConfirmCredentials.allowFpAuthentication", z2);
        intent.putExtra("com.android.settings.ConfirmCredentials.darkTheme", false);
        intent.putExtra("com.android.settings.ConfirmCredentials.showCancelButton", false);
        intent.putExtra("com.android.settings.ConfirmCredentials.showWhenLocked", z2);
        intent.putExtra("return_credentials", z);
        intent.putExtra("has_challenge", z3);
        intent.putExtra("challenge", j);
        intent.putExtra(":settings:hide_drawer", true);
        intent.putExtra("android.intent.extra.USER_ID", i2);
        intent.putExtra("android.app.extra.ALTERNATE_BUTTON_LABEL", charSequence4);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.setClassName("com.android.settings", cls.getName());
        if (z2) {
            intent.addFlags(33554432);
            if (this.mFragment != null) {
                copyOptionalExtras(this.mFragment.getActivity().getIntent(), intent);
                this.mFragment.startActivity(intent);
            } else {
                copyOptionalExtras(this.mActivity.getIntent(), intent);
                this.mActivity.startActivity(intent);
            }
        } else if (this.mFragment != null) {
            copyInternalExtras(this.mFragment.getActivity().getIntent(), intent);
            this.mFragment.startActivityForResult(intent, i);
        } else {
            copyInternalExtras(this.mActivity.getIntent(), intent);
            this.mActivity.startActivityForResult(intent, i);
        }
        return true;
    }

    private void copyOptionalExtras(Intent intent, Intent intent2) {
        IntentSender intentSender = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
        if (intentSender != null) {
            intent2.putExtra("android.intent.extra.INTENT", intentSender);
        }
        int intExtra = intent.getIntExtra("android.intent.extra.TASK_ID", -1);
        if (intExtra != -1) {
            intent2.putExtra("android.intent.extra.TASK_ID", intExtra);
        }
        if (intentSender != null || intExtra != -1) {
            intent2.addFlags(8388608);
            intent2.addFlags(1073741824);
        }
    }

    private void copyInternalExtras(Intent intent, Intent intent2) {
        String stringExtra = intent.getStringExtra("theme");
        if (stringExtra != null) {
            intent2.putExtra("theme", stringExtra);
        }
    }
}
