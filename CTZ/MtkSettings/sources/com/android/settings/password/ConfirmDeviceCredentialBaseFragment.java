package com.android.settings.password;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.UserInfo;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.fingerprint.FingerprintUiHelper;
import com.android.settings.password.ConfirmLockPassword;
import com.android.settings.password.ConfirmLockPattern;

public abstract class ConfirmDeviceCredentialBaseFragment extends InstrumentedFragment implements FingerprintUiHelper.Callback {
    protected Button mCancelButton;
    protected DevicePolicyManager mDevicePolicyManager;
    protected int mEffectiveUserId;
    protected TextView mErrorTextView;
    private FingerprintUiHelper mFingerprintHelper;
    protected ImageView mFingerprintIcon;
    protected boolean mFrp;
    private CharSequence mFrpAlternateButtonText;
    protected LockPatternUtils mLockPatternUtils;
    protected int mUserId;
    protected UserManager mUserManager;
    protected boolean mReturnCredentials = false;
    protected final Handler mHandler = new Handler();
    private final Runnable mResetErrorRunnable = new Runnable() {
        @Override
        public void run() {
            ConfirmDeviceCredentialBaseFragment.this.mErrorTextView.setText("");
        }
    };

    protected abstract void authenticationSucceeded();

    protected abstract int getLastTryErrorMessage(int i);

    protected abstract void onShowError();

    private boolean isInternalActivity() {
        return (getActivity() instanceof ConfirmLockPassword.InternalActivity) || (getActivity() instanceof ConfirmLockPattern.InternalActivity);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFrpAlternateButtonText = getActivity().getIntent().getCharSequenceExtra("android.app.extra.ALTERNATE_BUTTON_LABEL");
        this.mReturnCredentials = getActivity().getIntent().getBooleanExtra("return_credentials", false);
        this.mUserId = Utils.getUserIdFromBundle(getActivity(), getActivity().getIntent().getExtras(), isInternalActivity());
        this.mFrp = this.mUserId == -9999;
        this.mUserManager = UserManager.get(getActivity());
        this.mEffectiveUserId = this.mUserManager.getCredentialOwnerProfile(this.mUserId);
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
        this.mDevicePolicyManager = (DevicePolicyManager) getActivity().getSystemService("device_policy");
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mCancelButton = (Button) view.findViewById(R.id.cancelButton);
        this.mFingerprintIcon = (ImageView) view.findViewById(R.id.fingerprintIcon);
        this.mFingerprintHelper = new FingerprintUiHelper(this.mFingerprintIcon, (TextView) view.findViewById(R.id.errorText), this, this.mEffectiveUserId);
        int i = 0;
        boolean booleanExtra = getActivity().getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.showCancelButton", false);
        final boolean z = this.mFrp && !TextUtils.isEmpty(this.mFrpAlternateButtonText);
        Button button = this.mCancelButton;
        if (!booleanExtra && !z) {
            i = 8;
        }
        button.setVisibility(i);
        if (z) {
            this.mCancelButton.setText(this.mFrpAlternateButtonText);
        }
        this.mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                if (z) {
                    ConfirmDeviceCredentialBaseFragment.this.getActivity().setResult(1);
                }
                ConfirmDeviceCredentialBaseFragment.this.getActivity().finish();
            }
        });
        int credentialOwnerUserId = Utils.getCredentialOwnerUserId(getActivity(), Utils.getUserIdFromBundle(getActivity(), getActivity().getIntent().getExtras(), isInternalActivity()));
        if (this.mUserManager.isManagedProfile(credentialOwnerUserId)) {
            setWorkChallengeBackground(view, credentialOwnerUserId);
        }
    }

    private boolean isFingerprintDisabledByAdmin() {
        return (this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mEffectiveUserId) & 32) != 0;
    }

    protected boolean isStrongAuthRequired() {
        return (!this.mFrp && this.mLockPatternUtils.isFingerprintAllowedForUser(this.mEffectiveUserId) && this.mUserManager.isUserUnlocked(this.mUserId)) ? false : true;
    }

    private boolean isFingerprintAllowed() {
        return (this.mReturnCredentials || !getActivity().getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.allowFpAuthentication", false) || isStrongAuthRequired() || isFingerprintDisabledByAdmin()) ? false : true;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLockScreen();
    }

    protected void refreshLockScreen() {
        if (isFingerprintAllowed()) {
            this.mFingerprintHelper.startListening();
        } else if (this.mFingerprintHelper.isListening()) {
            this.mFingerprintHelper.stopListening();
        }
        updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId));
    }

    protected void setAccessibilityTitle(CharSequence charSequence) {
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            CharSequence charSequenceExtra = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.title");
            if (charSequence == null) {
                return;
            }
            if (charSequenceExtra == null) {
                getActivity().setTitle(charSequence);
                return;
            }
            getActivity().setTitle(Utils.createAccessibleSequence(charSequenceExtra, charSequenceExtra + "," + charSequence));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mFingerprintHelper.isListening()) {
            this.mFingerprintHelper.stopListening();
        }
    }

    @Override
    public void onAuthenticated() {
        if (getActivity() != null && getActivity().isResumed()) {
            ((TrustManager) getActivity().getSystemService("trust")).setDeviceLockedForUser(this.mEffectiveUserId, false);
            authenticationSucceeded();
            checkForPendingIntent();
        }
    }

    @Override
    public void onFingerprintIconVisibilityChanged(boolean z) {
    }

    public void prepareEnterAnimation() {
    }

    public void startEnterAnimation() {
    }

    protected void checkForPendingIntent() {
        int intExtra = getActivity().getIntent().getIntExtra("android.intent.extra.TASK_ID", -1);
        if (intExtra != -1) {
            try {
                ActivityManager.getService().startActivityFromRecents(intExtra, ActivityOptions.makeBasic().toBundle());
                return;
            } catch (RemoteException e) {
            }
        }
        IntentSender intentSender = (IntentSender) getActivity().getIntent().getParcelableExtra("android.intent.extra.INTENT");
        if (intentSender != null) {
            try {
                getActivity().startIntentSenderForResult(intentSender, -1, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e2) {
            }
        }
    }

    private void setWorkChallengeBackground(View view, int i) {
        View viewFindViewById = getActivity().findViewById(R.id.main_content);
        if (viewFindViewById != null) {
            viewFindViewById.setPadding(0, 0, 0, 0);
        }
        view.setBackground(new ColorDrawable(this.mDevicePolicyManager.getOrganizationColorForUser(i)));
        ImageView imageView = (ImageView) view.findViewById(R.id.background_image);
        if (imageView != null) {
            Drawable drawable = getResources().getDrawable(R.drawable.work_challenge_background);
            drawable.setColorFilter(getResources().getColor(R.color.confirm_device_credential_transparent_black), PorterDuff.Mode.DARKEN);
            imageView.setImageDrawable(drawable);
            Point point = new Point();
            getActivity().getWindowManager().getDefaultDisplay().getSize(point);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(-1, point.y));
        }
    }

    protected void reportSuccessfulAttempt() {
        this.mLockPatternUtils.reportSuccessfulPasswordAttempt(this.mEffectiveUserId);
        if (this.mUserManager.isManagedProfile(this.mEffectiveUserId)) {
            this.mLockPatternUtils.userPresent(this.mEffectiveUserId);
        }
    }

    protected void reportFailedAttempt() {
        updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId) + 1);
        this.mLockPatternUtils.reportFailedPasswordAttempt(this.mEffectiveUserId);
    }

    protected void updateErrorMessage(int i) {
        int maximumFailedPasswordsForWipe = this.mLockPatternUtils.getMaximumFailedPasswordsForWipe(this.mEffectiveUserId);
        if (maximumFailedPasswordsForWipe <= 0 || i <= 0) {
            return;
        }
        if (this.mErrorTextView != null) {
            showError(getActivity().getString(R.string.lock_failed_attempts_before_wipe, new Object[]{Integer.valueOf(i), Integer.valueOf(maximumFailedPasswordsForWipe)}), 0L);
        }
        int i2 = maximumFailedPasswordsForWipe - i;
        if (i2 > 1) {
            return;
        }
        FragmentManager childFragmentManager = getChildFragmentManager();
        int userTypeForWipe = getUserTypeForWipe();
        if (i2 == 1) {
            LastTryDialog.show(childFragmentManager, getActivity().getString(R.string.lock_last_attempt_before_wipe_warning_title), getLastTryErrorMessage(userTypeForWipe), android.R.string.ok, false);
        } else {
            LastTryDialog.show(childFragmentManager, null, getWipeMessage(userTypeForWipe), R.string.lock_failed_attempts_now_wiping_dialog_dismiss, true);
        }
    }

    private int getUserTypeForWipe() {
        UserInfo userInfo = this.mUserManager.getUserInfo(this.mDevicePolicyManager.getProfileWithMinimumFailedPasswordsForWipe(this.mEffectiveUserId));
        if (userInfo == null || userInfo.isPrimary()) {
            return 1;
        }
        if (userInfo.isManagedProfile()) {
            return 2;
        }
        return 3;
    }

    private int getWipeMessage(int i) {
        switch (i) {
            case 1:
                return R.string.lock_failed_attempts_now_wiping_device;
            case 2:
                return R.string.lock_failed_attempts_now_wiping_profile;
            case 3:
                return R.string.lock_failed_attempts_now_wiping_user;
            default:
                throw new IllegalArgumentException("Unrecognized user type:" + i);
        }
    }

    protected void showError(CharSequence charSequence, long j) {
        this.mErrorTextView.setText(charSequence);
        onShowError();
        this.mHandler.removeCallbacks(this.mResetErrorRunnable);
        if (j != 0) {
            this.mHandler.postDelayed(this.mResetErrorRunnable, j);
        }
    }

    protected void showError(int i, long j) {
        showError(getText(i), j);
    }

    public static class LastTryDialog extends DialogFragment {
        private static final String TAG = LastTryDialog.class.getSimpleName();

        static boolean show(FragmentManager fragmentManager, String str, int i, int i2, boolean z) {
            LastTryDialog lastTryDialog = (LastTryDialog) fragmentManager.findFragmentByTag(TAG);
            if (lastTryDialog != null && !lastTryDialog.isRemoving()) {
                return false;
            }
            Bundle bundle = new Bundle();
            bundle.putString("title", str);
            bundle.putInt("message", i);
            bundle.putInt("button", i2);
            bundle.putBoolean("dismiss", z);
            LastTryDialog lastTryDialog2 = new LastTryDialog();
            lastTryDialog2.setArguments(bundle);
            lastTryDialog2.show(fragmentManager, TAG);
            fragmentManager.executePendingTransactions();
            return true;
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(getActivity()).setTitle(getArguments().getString("title")).setMessage(getArguments().getInt("message")).setPositiveButton(getArguments().getInt("button"), (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            return alertDialogCreate;
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            super.onDismiss(dialogInterface);
            if (getActivity() != null && getArguments().getBoolean("dismiss")) {
                getActivity().finish();
            }
        }
    }
}
