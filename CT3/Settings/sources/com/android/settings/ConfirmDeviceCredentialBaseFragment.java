package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.KeyStore;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.fingerprint.FingerprintUiHelper;

public abstract class ConfirmDeviceCredentialBaseFragment extends OptionsMenuFragment implements FingerprintUiHelper.Callback {
    private boolean mAllowFpAuthentication;
    protected Button mCancelButton;
    protected int mEffectiveUserId;
    protected TextView mErrorTextView;
    private FingerprintUiHelper mFingerprintHelper;
    protected ImageView mFingerprintIcon;
    protected boolean mIsStrongAuthRequired;
    protected LockPatternUtils mLockPatternUtils;
    protected int mUserId;
    protected boolean mReturnCredentials = false;
    protected final Handler mHandler = new Handler();
    private final Runnable mResetErrorRunnable = new Runnable() {
        @Override
        public void run() {
            ConfirmDeviceCredentialBaseFragment.this.mErrorTextView.setText("");
        }
    };

    protected abstract void authenticationSucceeded();

    protected abstract int getLastTryErrorMessage();

    protected abstract void onShowError();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean z = false;
        super.onCreate(savedInstanceState);
        this.mAllowFpAuthentication = getActivity().getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.allowFpAuthentication", false);
        this.mReturnCredentials = getActivity().getIntent().getBooleanExtra("return_credentials", false);
        Intent intent = getActivity().getIntent();
        this.mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
        UserManager userManager = UserManager.get(getActivity());
        this.mEffectiveUserId = userManager.getCredentialOwnerProfile(this.mUserId);
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
        this.mIsStrongAuthRequired = isFingerprintDisallowedByStrongAuth();
        if (this.mAllowFpAuthentication && !isFingerprintDisabledByAdmin() && !this.mReturnCredentials && !this.mIsStrongAuthRequired) {
            z = true;
        }
        this.mAllowFpAuthentication = z;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mCancelButton = (Button) view.findViewById(R.id.cancelButton);
        this.mFingerprintIcon = (ImageView) view.findViewById(R.id.fingerprintIcon);
        this.mFingerprintHelper = new FingerprintUiHelper(this.mFingerprintIcon, (TextView) view.findViewById(R.id.errorText), this, this.mEffectiveUserId);
        boolean showCancelButton = getActivity().getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.showCancelButton", false);
        this.mCancelButton.setVisibility(showCancelButton ? 0 : 8);
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfirmDeviceCredentialBaseFragment.this.getActivity().finish();
            }
        });
        int credentialOwnerUserId = Utils.getCredentialOwnerUserId(getActivity(), Utils.getUserIdFromBundle(getActivity(), getActivity().getIntent().getExtras()));
        if (!Utils.isManagedProfile(UserManager.get(getActivity()), credentialOwnerUserId)) {
            return;
        }
        setWorkChallengeBackground(view, credentialOwnerUserId);
    }

    private boolean isFingerprintDisabledByAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        int disabledFeatures = dpm.getKeyguardDisabledFeatures(null, this.mEffectiveUserId);
        return (disabledFeatures & 32) != 0;
    }

    private boolean isFingerprintDisallowedByStrongAuth() {
        return (this.mLockPatternUtils.isFingerprintAllowedForUser(this.mEffectiveUserId) && KeyStore.getInstance().state(this.mUserId) == KeyStore.State.UNLOCKED) ? false : true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mAllowFpAuthentication) {
            this.mFingerprintHelper.startListening();
        }
        if (!isProfileChallenge()) {
            return;
        }
        updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId));
    }

    protected void setAccessibilityTitle(CharSequence supplementalText) {
        CharSequence titleText;
        Intent intent = getActivity().getIntent();
        if (intent == null || (titleText = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.title")) == null || supplementalText == null) {
            return;
        }
        String accessibilityTitle = titleText + "," + supplementalText;
        getActivity().setTitle(Utils.createAccessibleSequence(titleText, accessibilityTitle));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.mAllowFpAuthentication) {
            return;
        }
        this.mFingerprintHelper.stopListening();
    }

    @Override
    public void onAuthenticated() {
        if (getActivity() == null || !getActivity().isResumed()) {
            return;
        }
        TrustManager trustManager = (TrustManager) getActivity().getSystemService("trust");
        trustManager.setDeviceLockedForUser(this.mEffectiveUserId, false);
        authenticationSucceeded();
        authenticationSucceeded();
        checkForPendingIntent();
    }

    @Override
    public void onFingerprintIconVisibilityChanged(boolean visible) {
    }

    public void prepareEnterAnimation() {
    }

    public void startEnterAnimation() {
    }

    protected void checkForPendingIntent() {
        int taskId = getActivity().getIntent().getIntExtra("android.intent.extra.TASK_ID", -1);
        if (taskId != -1) {
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchStackId(-1);
                activityManager.startActivityFromRecents(taskId, options.toBundle());
                return;
            } catch (RemoteException e) {
            }
        }
        IntentSender intentSender = (IntentSender) getActivity().getIntent().getParcelableExtra("android.intent.extra.INTENT");
        if (intentSender == null) {
            return;
        }
        try {
            getActivity().startIntentSenderForResult(intentSender, -1, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e2) {
        }
    }

    private void setWorkChallengeBackground(View baseView, int userId) {
        View mainContent = getActivity().findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setPadding(0, 0, 0, 0);
        }
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        baseView.setBackground(new ColorDrawable(dpm.getOrganizationColorForUser(userId)));
        ImageView imageView = (ImageView) baseView.findViewById(R.id.background_image);
        if (imageView == null) {
            return;
        }
        Drawable image = getResources().getDrawable(R.drawable.work_challenge_background);
        image.setColorFilter(getResources().getColor(R.color.confirm_device_credential_transparent_black), PorterDuff.Mode.DARKEN);
        imageView.setImageDrawable(image);
        Point screenSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(-1, screenSize.y));
    }

    protected boolean isProfileChallenge() {
        return Utils.isManagedProfile(UserManager.get(getContext()), this.mEffectiveUserId);
    }

    protected void reportSuccessfullAttempt() {
        if (!isProfileChallenge()) {
            return;
        }
        this.mLockPatternUtils.reportSuccessfulPasswordAttempt(this.mEffectiveUserId);
        this.mLockPatternUtils.userPresent(this.mEffectiveUserId);
    }

    protected void reportFailedAttempt() {
        if (!isProfileChallenge()) {
            return;
        }
        updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId) + 1);
        this.mLockPatternUtils.reportFailedPasswordAttempt(this.mEffectiveUserId);
    }

    protected void updateErrorMessage(int numAttempts) {
        int maxAttempts = this.mLockPatternUtils.getMaximumFailedPasswordsForWipe(this.mEffectiveUserId);
        if (maxAttempts <= 0 || numAttempts <= 0) {
            return;
        }
        int remainingAttempts = maxAttempts - numAttempts;
        if (remainingAttempts == 1) {
            String title = getActivity().getString(R.string.lock_profile_wipe_warning_title);
            String message = getActivity().getString(getLastTryErrorMessage());
            showDialog(title, message, android.R.string.ok, false);
        } else if (remainingAttempts <= 0) {
            String message2 = getActivity().getString(R.string.lock_profile_wipe_content);
            showDialog(null, message2, R.string.lock_profile_wipe_dismiss, true);
        }
        if (this.mErrorTextView == null) {
            return;
        }
        String message3 = getActivity().getString(R.string.lock_profile_wipe_attempts, new Object[]{Integer.valueOf(numAttempts), Integer.valueOf(maxAttempts)});
        showError(message3, 0L);
    }

    protected void showError(CharSequence msg, long timeout) {
        this.mErrorTextView.setText(msg);
        onShowError();
        this.mHandler.removeCallbacks(this.mResetErrorRunnable);
        if (timeout == 0) {
            return;
        }
        this.mHandler.postDelayed(this.mResetErrorRunnable, timeout);
    }

    protected void showError(int msg, long timeout) {
        showError(getText(msg), timeout);
    }

    private void showDialog(String title, String message, int buttonString, final boolean dismiss) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).setPositiveButton(buttonString, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                if (!dismiss) {
                    return;
                }
                ConfirmDeviceCredentialBaseFragment.this.getActivity().finish();
            }
        }).create();
        dialog.show();
    }
}
