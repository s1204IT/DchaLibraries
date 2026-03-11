package com.android.settings.password;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.R;
import com.android.settings.password.ConfirmDeviceCredentialBaseActivity;
import com.android.settings.password.ConfirmLockPassword;
import com.android.settings.password.CredentialCheckResultTracker;
import com.android.settings.widget.ImeAwareEditText;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.util.ArrayList;

public class ConfirmLockPassword extends ConfirmDeviceCredentialBaseActivity {
    private static final int[] DETAIL_TEXTS = {R.string.lockpassword_confirm_your_pin_generic, R.string.lockpassword_confirm_your_password_generic, R.string.lockpassword_confirm_your_pin_generic_profile, R.string.lockpassword_confirm_your_password_generic_profile, R.string.lockpassword_strong_auth_required_device_pin, R.string.lockpassword_strong_auth_required_device_password, R.string.lockpassword_strong_auth_required_work_pin, R.string.lockpassword_strong_auth_required_work_password};

    public static class InternalActivity extends ConfirmLockPassword {
    }

    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", ConfirmLockPasswordFragment.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String str) {
        return ConfirmLockPasswordFragment.class.getName().equals(str);
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        Fragment fragmentFindFragmentById = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragmentFindFragmentById != null && (fragmentFindFragmentById instanceof ConfirmLockPasswordFragment)) {
            ((ConfirmLockPasswordFragment) fragmentFindFragmentById).onWindowFocusChanged(z);
        }
    }

    public static class ConfirmLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment implements View.OnClickListener, TextView.OnEditorActionListener, CredentialCheckResultTracker.Listener {
        private AppearAnimationUtils mAppearAnimationUtils;
        private CountDownTimer mCountdownTimer;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private TextView mDetailsTextView;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private TextView mHeaderTextView;
        private InputMethodManager mImm;
        private boolean mIsAlpha;
        private ImeAwareEditText mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private boolean mDisappearing = false;
        private boolean mUsingFingerprint = false;

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            int i;
            int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mEffectiveUserId);
            if (((ConfirmLockPassword) getActivity()).getConfirmCredentialTheme() == ConfirmDeviceCredentialBaseActivity.ConfirmCredentialTheme.NORMAL) {
                i = R.layout.confirm_lock_password_normal;
            } else {
                i = R.layout.confirm_lock_password;
            }
            View viewInflate = layoutInflater.inflate(i, viewGroup, false);
            this.mPasswordEntry = (ImeAwareEditText) viewInflate.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.requestFocus();
            this.mPasswordEntryInputDisabler = new TextViewInputDisabler(this.mPasswordEntry);
            this.mHeaderTextView = (TextView) viewInflate.findViewById(R.id.headerText);
            if (this.mHeaderTextView == null) {
                this.mHeaderTextView = (TextView) viewInflate.findViewById(R.id.suw_layout_title);
            }
            this.mDetailsTextView = (TextView) viewInflate.findViewById(R.id.detailsText);
            this.mErrorTextView = (TextView) viewInflate.findViewById(R.id.errorText);
            this.mIsAlpha = 262144 == keyguardStoredPasswordQuality || 327680 == keyguardStoredPasswordQuality || 393216 == keyguardStoredPasswordQuality || 524288 == keyguardStoredPasswordQuality;
            this.mImm = (InputMethodManager) getActivity().getSystemService("input_method");
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence charSequenceExtra = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.header");
                CharSequence charSequenceExtra2 = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.details");
                if (TextUtils.isEmpty(charSequenceExtra)) {
                    charSequenceExtra = getString(getDefaultHeader());
                }
                if (TextUtils.isEmpty(charSequenceExtra2)) {
                    charSequenceExtra2 = getString(getDefaultDetails());
                }
                this.mHeaderTextView.setText(charSequenceExtra);
                this.mDetailsTextView.setText(charSequenceExtra2);
            }
            int inputType = this.mPasswordEntry.getInputType();
            ImeAwareEditText imeAwareEditText = this.mPasswordEntry;
            if (!this.mIsAlpha) {
                inputType = 18;
            }
            imeAwareEditText.setInputType(inputType);
            this.mPasswordEntry.setTypeface(Typeface.create(getContext().getString(android.R.string.aerr_process_repeated), 0));
            this.mAppearAnimationUtils = new AppearAnimationUtils(getContext(), 220L, 2.0f, 1.0f, AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in));
            this.mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(), 110L, 1.0f, 0.5f, AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_linear_in));
            setAccessibilityTitle(this.mHeaderTextView.getText());
            this.mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager().findFragmentByTag("check_lock_result");
            if (this.mCredentialCheckResultTracker == null) {
                this.mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(this.mCredentialCheckResultTracker, "check_lock_result").commit();
            }
            return viewInflate;
        }

        private int getDefaultHeader() {
            return this.mFrp ? this.mIsAlpha ? R.string.lockpassword_confirm_your_password_header_frp : R.string.lockpassword_confirm_your_pin_header_frp : this.mIsAlpha ? R.string.lockpassword_confirm_your_password_header : R.string.lockpassword_confirm_your_pin_header;
        }

        private int getDefaultDetails() {
            if (this.mFrp) {
                return this.mIsAlpha ? R.string.lockpassword_confirm_your_password_details_frp : R.string.lockpassword_confirm_your_pin_details_frp;
            }
            return ConfirmLockPassword.DETAIL_TEXTS[((isStrongAuthRequired() ? 1 : 0) << 2) + ((UserManager.get(getActivity()).isManagedProfile(this.mEffectiveUserId) ? 1 : 0) << 1) + (this.mIsAlpha ? 1 : 0)];
        }

        private int getErrorMessage() {
            return this.mIsAlpha ? R.string.lockpassword_invalid_password : R.string.lockpassword_invalid_pin;
        }

        @Override
        protected int getLastTryErrorMessage(int i) {
            switch (i) {
                case 1:
                    return this.mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_device : R.string.lock_last_pin_attempt_before_wipe_device;
                case 2:
                    return this.mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_profile : R.string.lock_last_pin_attempt_before_wipe_profile;
                case 3:
                    return this.mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_user : R.string.lock_last_pin_attempt_before_wipe_user;
                default:
                    throw new IllegalArgumentException("Unrecognized user type:" + i);
            }
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            this.mHeaderTextView.setAlpha(0.0f);
            this.mDetailsTextView.setAlpha(0.0f);
            this.mCancelButton.setAlpha(0.0f);
            this.mPasswordEntry.setAlpha(0.0f);
            this.mErrorTextView.setAlpha(0.0f);
            this.mFingerprintIcon.setAlpha(0.0f);
        }

        private View[] getActiveViews() {
            ArrayList arrayList = new ArrayList();
            arrayList.add(this.mHeaderTextView);
            arrayList.add(this.mDetailsTextView);
            if (this.mCancelButton.getVisibility() == 0) {
                arrayList.add(this.mCancelButton);
            }
            arrayList.add(this.mPasswordEntry);
            arrayList.add(this.mErrorTextView);
            if (this.mFingerprintIcon.getVisibility() == 0) {
                arrayList.add(this.mFingerprintIcon);
            }
            return (View[]) arrayList.toArray(new View[0]);
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            this.mAppearAnimationUtils.startAnimation(getActiveViews(), new $$Lambda$ConfirmLockPassword$ConfirmLockPasswordFragment$Myp25CGN_sn9Gs6wDwuZ61aKfg8(this));
        }

        @Override
        public void onPause() {
            super.onPause();
            if (this.mCountdownTimer != null) {
                this.mCountdownTimer.cancel();
                this.mCountdownTimer = null;
            }
            this.mCredentialCheckResultTracker.setListener(null);
        }

        @Override
        public int getMetricsCategory() {
            return 30;
        }

        @Override
        public void onResume() {
            super.onResume();
            long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(this.mEffectiveUserId);
            if (lockoutAttemptDeadline != 0) {
                this.mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(lockoutAttemptDeadline);
            } else {
                updatePasswordEntry();
                this.mErrorTextView.setText("");
                updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId));
            }
            this.mCredentialCheckResultTracker.setListener(this);
        }

        @Override
        protected void authenticationSucceeded() {
            this.mCredentialCheckResultTracker.setResult(true, new Intent(), 0, this.mEffectiveUserId);
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean z) {
            this.mUsingFingerprint = z;
        }

        public void updatePasswordEntry() {
            boolean z = this.mLockPatternUtils.getLockoutAttemptDeadline(this.mEffectiveUserId) != 0;
            this.mPasswordEntry.setEnabled(!z);
            this.mPasswordEntryInputDisabler.setInputEnabled(!z);
            if (z || this.mUsingFingerprint) {
                this.mImm.hideSoftInputFromWindow(this.mPasswordEntry.getWindowToken(), 0);
            } else {
                this.mPasswordEntry.scheduleShowSoftInput();
            }
        }

        public void onWindowFocusChanged(boolean z) {
            if (!z) {
                return;
            }
            this.mPasswordEntry.post(new $$Lambda$ConfirmLockPassword$ConfirmLockPasswordFragment$Myp25CGN_sn9Gs6wDwuZ61aKfg8(this));
        }

        private void handleNext() {
            if (this.mPendingLockCheck != null || this.mDisappearing) {
                return;
            }
            String string = this.mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(string)) {
                return;
            }
            this.mPasswordEntryInputDisabler.setInputEnabled(false);
            boolean booleanExtra = getActivity().getIntent().getBooleanExtra("has_challenge", false);
            Intent intent = new Intent();
            if (booleanExtra) {
                if (isInternalActivity()) {
                    startVerifyPassword(string, intent);
                    return;
                } else {
                    this.mCredentialCheckResultTracker.setResult(false, intent, 0, this.mEffectiveUserId);
                    return;
                }
            }
            startCheckPassword(string, intent);
        }

        public boolean isInternalActivity() {
            return getActivity() instanceof InternalActivity;
        }

        private void startVerifyPassword(String str, final Intent intent) {
            AsyncTask<?, ?, ?> asyncTaskVerifyTiedProfileChallenge;
            long longExtra = getActivity().getIntent().getLongExtra("challenge", 0L);
            final int i = this.mEffectiveUserId;
            int i2 = this.mUserId;
            LockPatternChecker.OnVerifyCallback onVerifyCallback = new LockPatternChecker.OnVerifyCallback() {
                public void onVerified(byte[] bArr, int i3) {
                    boolean z;
                    ConfirmLockPasswordFragment.this.mPendingLockCheck = null;
                    if (bArr != null) {
                        z = true;
                        if (ConfirmLockPasswordFragment.this.mReturnCredentials) {
                            intent.putExtra("hw_auth_token", bArr);
                        }
                    } else {
                        z = false;
                    }
                    ConfirmLockPasswordFragment.this.mCredentialCheckResultTracker.setResult(z, intent, i3, i);
                }
            };
            if (i == i2) {
                asyncTaskVerifyTiedProfileChallenge = LockPatternChecker.verifyPassword(this.mLockPatternUtils, str, longExtra, i2, onVerifyCallback);
            } else {
                asyncTaskVerifyTiedProfileChallenge = LockPatternChecker.verifyTiedProfileChallenge(this.mLockPatternUtils, str, false, longExtra, i2, onVerifyCallback);
            }
            this.mPendingLockCheck = asyncTaskVerifyTiedProfileChallenge;
        }

        private void startCheckPassword(final String str, final Intent intent) {
            final int i = this.mEffectiveUserId;
            this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, str, i, new LockPatternChecker.OnCheckCallback() {
                public void onChecked(boolean z, int i2) {
                    ConfirmLockPasswordFragment.this.mPendingLockCheck = null;
                    if (z && ConfirmLockPasswordFragment.this.isInternalActivity() && ConfirmLockPasswordFragment.this.mReturnCredentials) {
                        intent.putExtra("type", ConfirmLockPasswordFragment.this.mIsAlpha ? 0 : 3);
                        intent.putExtra("password", str);
                    }
                    ConfirmLockPasswordFragment.this.mCredentialCheckResultTracker.setResult(z, intent, i2, i);
                }
            });
        }

        private void startDisappearAnimation(final Intent intent) {
            if (this.mDisappearing) {
                return;
            }
            this.mDisappearing = true;
            final ConfirmLockPassword confirmLockPassword = (ConfirmLockPassword) getActivity();
            if (confirmLockPassword == null || confirmLockPassword.isFinishing()) {
                return;
            }
            if (confirmLockPassword.getConfirmCredentialTheme() == ConfirmDeviceCredentialBaseActivity.ConfirmCredentialTheme.DARK) {
                this.mDisappearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                    @Override
                    public final void run() {
                        ConfirmLockPassword.ConfirmLockPasswordFragment.lambda$startDisappearAnimation$0(confirmLockPassword, intent);
                    }
                });
            } else {
                confirmLockPassword.setResult(-1, intent);
                confirmLockPassword.finish();
            }
        }

        static void lambda$startDisappearAnimation$0(ConfirmLockPassword confirmLockPassword, Intent intent) {
            confirmLockPassword.setResult(-1, intent);
            confirmLockPassword.finish();
            confirmLockPassword.overridePendingTransition(R.anim.confirm_credential_close_enter, R.anim.confirm_credential_close_exit);
        }

        private void onPasswordChecked(boolean z, Intent intent, int i, int i2, boolean z2) {
            this.mPasswordEntryInputDisabler.setInputEnabled(true);
            if (z) {
                if (z2) {
                    reportSuccessfulAttempt();
                }
                startDisappearAnimation(intent);
                checkForPendingIntent();
                return;
            }
            if (i > 0) {
                refreshLockScreen();
                handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(i2, i));
            } else {
                showError(getErrorMessage(), 3000L);
            }
            if (z2) {
                reportFailedAttempt();
            }
        }

        @Override
        public void onCredentialChecked(boolean z, Intent intent, int i, int i2, boolean z2) {
            onPasswordChecked(z, intent, i, i2, z2);
        }

        @Override
        protected void onShowError() {
            this.mPasswordEntry.setText((CharSequence) null);
        }

        private void handleAttemptLockout(long j) {
            this.mCountdownTimer = new CountDownTimer(j - SystemClock.elapsedRealtime(), 1000L) {
                @Override
                public void onTick(long j2) {
                    ConfirmLockPasswordFragment.this.showError(ConfirmLockPasswordFragment.this.getString(R.string.lockpattern_too_many_failed_confirmation_attempts, new Object[]{Integer.valueOf((int) (j2 / 1000))}), 0L);
                }

                @Override
                public void onFinish() {
                    ConfirmLockPasswordFragment.this.updatePasswordEntry();
                    ConfirmLockPasswordFragment.this.mErrorTextView.setText("");
                    ConfirmLockPasswordFragment.this.updateErrorMessage(ConfirmLockPasswordFragment.this.mLockPatternUtils.getCurrentFailedPasswordAttempts(ConfirmLockPasswordFragment.this.mEffectiveUserId));
                }
            }.start();
            updatePasswordEntry();
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.cancel_button) {
                getActivity().setResult(0);
                getActivity().finish();
            } else if (id == R.id.next_button) {
                handleNext();
            }
        }

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (i == 0 || i == 6 || i == 5) {
                handleNext();
                return true;
            }
            return false;
        }
    }
}
