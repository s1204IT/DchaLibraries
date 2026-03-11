package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.os.UserManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ConfirmLockPassword;
import com.android.settings.ConfirmLockPattern;

public final class ChooseLockSettingsHelper {
    private Activity mActivity;
    private Fragment mFragment;
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

    public boolean launchConfirmationActivity(int request, CharSequence title) {
        return launchConfirmationActivity(request, title, (CharSequence) null, (CharSequence) null, false, false);
    }

    boolean launchConfirmationActivity(int request, CharSequence title, boolean returnCredentials) {
        return launchConfirmationActivity(request, title, (CharSequence) null, (CharSequence) null, returnCredentials, false);
    }

    public boolean launchConfirmationActivity(int request, CharSequence title, boolean returnCredentials, int userId) {
        return launchConfirmationActivity(request, title, null, null, returnCredentials, false, false, 0L, Utils.enforceSameOwner(this.mActivity, userId));
    }

    boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, boolean returnCredentials, boolean external) {
        return launchConfirmationActivity(request, title, header, description, returnCredentials, external, false, 0L, Utils.getCredentialOwnerUserId(this.mActivity));
    }

    boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, boolean returnCredentials, boolean external, int userId) {
        return launchConfirmationActivity(request, title, header, description, returnCredentials, external, false, 0L, Utils.enforceSameOwner(this.mActivity, userId));
    }

    public boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, long challenge) {
        return launchConfirmationActivity(request, title, header, description, true, false, true, challenge, Utils.getCredentialOwnerUserId(this.mActivity));
    }

    public boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, long challenge, int userId) {
        return launchConfirmationActivity(request, title, header, description, true, false, true, challenge, Utils.enforceSameOwner(this.mActivity, userId));
    }

    public boolean launchConfirmationActivityWithExternalAndChallenge(int request, CharSequence title, CharSequence header, CharSequence description, boolean external, long challenge, int userId) {
        return launchConfirmationActivity(request, title, header, description, false, external, true, challenge, Utils.enforceSameOwner(this.mActivity, userId));
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence description, boolean returnCredentials, boolean external, boolean hasChallenge, long challenge, int userId) {
        Class<?> cls;
        Class<?> cls2;
        int effectiveUserId = UserManager.get(this.mActivity).getCredentialOwnerProfile(userId);
        switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(effectiveUserId)) {
            case 65536:
                if (returnCredentials || hasChallenge) {
                    cls2 = ConfirmLockPattern.InternalActivity.class;
                } else {
                    cls2 = ConfirmLockPattern.class;
                }
                boolean launched = launchConfirmationActivity(request, title, header, description, cls2, returnCredentials, external, hasChallenge, challenge, userId);
                return launched;
            case 131072:
            case 196608:
            case 262144:
            case 327680:
            case 393216:
            case 524288:
                if (returnCredentials || hasChallenge) {
                    cls = ConfirmLockPassword.InternalActivity.class;
                } else {
                    cls = ConfirmLockPassword.class;
                }
                boolean launched2 = launchConfirmationActivity(request, title, header, description, cls, returnCredentials, external, hasChallenge, challenge, userId);
                return launched2;
            default:
                return false;
        }
    }

    private boolean launchConfirmationActivity(int request, CharSequence title, CharSequence header, CharSequence message, Class<?> activityClass, boolean returnCredentials, boolean external, boolean hasChallenge, long challenge, int userId) {
        Intent intent = new Intent();
        intent.putExtra("com.android.settings.ConfirmCredentials.title", title);
        intent.putExtra("com.android.settings.ConfirmCredentials.header", header);
        intent.putExtra("com.android.settings.ConfirmCredentials.details", message);
        intent.putExtra("com.android.settings.ConfirmCredentials.allowFpAuthentication", external);
        intent.putExtra("com.android.settings.ConfirmCredentials.darkTheme", external);
        intent.putExtra("com.android.settings.ConfirmCredentials.showCancelButton", external);
        intent.putExtra("com.android.settings.ConfirmCredentials.showWhenLocked", external);
        intent.putExtra("return_credentials", returnCredentials);
        intent.putExtra("has_challenge", hasChallenge);
        intent.putExtra("challenge", challenge);
        intent.putExtra(":settings:hide_drawer", true);
        intent.putExtra("android.intent.extra.USER_ID", userId);
        intent.setClassName("com.android.settings", activityClass.getName());
        if (external) {
            intent.addFlags(33554432);
            if (this.mFragment != null) {
                copyOptionalExtras(this.mFragment.getActivity().getIntent(), intent);
                this.mFragment.startActivity(intent);
            } else {
                copyOptionalExtras(this.mActivity.getIntent(), intent);
                this.mActivity.startActivity(intent);
            }
        } else if (this.mFragment != null) {
            this.mFragment.startActivityForResult(intent, request);
        } else {
            this.mActivity.startActivityForResult(intent, request);
        }
        return true;
    }

    private void copyOptionalExtras(Intent inIntent, Intent outIntent) {
        IntentSender intentSender = (IntentSender) inIntent.getParcelableExtra("android.intent.extra.INTENT");
        if (intentSender != null) {
            outIntent.putExtra("android.intent.extra.INTENT", intentSender);
        }
        int taskId = inIntent.getIntExtra("android.intent.extra.TASK_ID", -1);
        if (taskId != -1) {
            outIntent.putExtra("android.intent.extra.TASK_ID", taskId);
        }
        if (intentSender == null && taskId == -1) {
            return;
        }
        outIntent.addFlags(8388608);
        outIntent.addFlags(1073741824);
    }
}
