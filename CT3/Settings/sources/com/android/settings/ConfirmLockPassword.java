package com.android.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.CredentialCheckResultTracker;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.util.ArrayList;

public class ConfirmLockPassword extends ConfirmDeviceCredentialBaseActivity {
    private static final int[] DETAIL_TEXTS = {R.string.lockpassword_confirm_your_pin_generic, R.string.lockpassword_confirm_your_password_generic, R.string.lockpassword_confirm_your_pin_generic_profile, R.string.lockpassword_confirm_your_password_generic_profile, R.string.lockpassword_strong_auth_required_reason_restart_device_pin, R.string.lockpassword_strong_auth_required_reason_restart_device_password, R.string.lockpassword_strong_auth_required_reason_restart_work_pin, R.string.lockpassword_strong_auth_required_reason_restart_work_password};

    public static class InternalActivity extends ConfirmLockPassword {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", ConfirmLockPasswordFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ConfirmLockPasswordFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment == null || !(fragment instanceof ConfirmLockPasswordFragment)) {
            return;
        }
        ((ConfirmLockPasswordFragment) fragment).onWindowFocusChanged(hasFocus);
    }

    public static class ConfirmLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment implements View.OnClickListener, TextView.OnEditorActionListener, CredentialCheckResultTracker.Listener {
        private AppearAnimationUtils mAppearAnimationUtils;
        private boolean mBlockImm;
        private CountDownTimer mCountdownTimer;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private TextView mDetailsTextView;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private TextView mHeaderTextView;
        private InputMethodManager mImm;
        private boolean mIsAlpha;
        private TextView mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private boolean mDisappearing = false;
        private boolean mUsingFingerprint = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int storedQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mEffectiveUserId);
            View view = inflater.inflate(R.layout.confirm_lock_password, (ViewGroup) null);
            this.mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntryInputDisabler = new TextViewInputDisabler(this.mPasswordEntry);
            this.mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            this.mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            this.mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            boolean z = 262144 == storedQuality || 327680 == storedQuality || 393216 == storedQuality || 524288 == storedQuality;
            this.mIsAlpha = z;
            this.mImm = (InputMethodManager) getActivity().getSystemService("input_method");
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.header");
                CharSequence detailsMessage = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.details");
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getString(getDefaultHeader());
                }
                if (TextUtils.isEmpty(detailsMessage)) {
                    detailsMessage = getString(getDefaultDetails());
                }
                this.mHeaderTextView.setText(headerMessage);
                this.mDetailsTextView.setText(detailsMessage);
            }
            int currentType = this.mPasswordEntry.getInputType();
            TextView textView = this.mPasswordEntry;
            if (!this.mIsAlpha) {
                currentType = 18;
            }
            textView.setInputType(currentType);
            this.mAppearAnimationUtils = new AppearAnimationUtils(getContext(), 220L, 2.0f, 1.0f, AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in));
            this.mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(), 110L, 1.0f, 0.5f, AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_linear_in));
            setAccessibilityTitle(this.mHeaderTextView.getText());
            this.mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager().findFragmentByTag("check_lock_result");
            if (this.mCredentialCheckResultTracker == null) {
                this.mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(this.mCredentialCheckResultTracker, "check_lock_result").commit();
            }
            return view;
        }

        private int getDefaultHeader() {
            return this.mIsAlpha ? R.string.lockpassword_confirm_your_password_header : R.string.lockpassword_confirm_your_pin_header;
        }

        private int getDefaultDetails() {
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), this.mEffectiveUserId);
            int index = ((isProfile ? 1 : 0) << 1) + ((this.mIsStrongAuthRequired ? 1 : 0) << 2) + (this.mIsAlpha ? 1 : 0);
            return ConfirmLockPassword.DETAIL_TEXTS[index];
        }

        private int getErrorMessage() {
            return this.mIsAlpha ? R.string.lockpassword_invalid_password : R.string.lockpassword_invalid_pin;
        }

        @Override
        protected int getLastTryErrorMessage() {
            return this.mIsAlpha ? R.string.lock_profile_wipe_warning_content_password : R.string.lock_profile_wipe_warning_content_pin;
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            this.mHeaderTextView.setAlpha(0.0f);
            this.mDetailsTextView.setAlpha(0.0f);
            this.mCancelButton.setAlpha(0.0f);
            this.mPasswordEntry.setAlpha(0.0f);
            this.mFingerprintIcon.setAlpha(0.0f);
            this.mBlockImm = true;
        }

        private View[] getActiveViews() {
            ArrayList<View> result = new ArrayList<>();
            result.add(this.mHeaderTextView);
            result.add(this.mDetailsTextView);
            if (this.mCancelButton.getVisibility() == 0) {
                result.add(this.mCancelButton);
            }
            result.add(this.mPasswordEntry);
            if (this.mFingerprintIcon.getVisibility() == 0) {
                result.add(this.mFingerprintIcon);
            }
            return (View[]) result.toArray(new View[0]);
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            this.mAppearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                @Override
                public void run() {
                    ConfirmLockPasswordFragment.this.mBlockImm = false;
                    ConfirmLockPasswordFragment.this.resetState();
                }
            });
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
        protected int getMetricsCategory() {
            return 30;
        }

        @Override
        public void onResume() {
            super.onResume();
            long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(this.mEffectiveUserId);
            if (deadline != 0) {
                this.mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else {
                resetState();
                this.mErrorTextView.setText("");
                if (isProfileChallenge()) {
                    updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId));
                }
            }
            this.mCredentialCheckResultTracker.setListener(this);
        }

        @Override
        protected void authenticationSucceeded() {
            this.mCredentialCheckResultTracker.setResult(true, new Intent(), 0, this.mEffectiveUserId);
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean visible) {
            this.mUsingFingerprint = visible;
        }

        public void resetState() {
            if (this.mBlockImm) {
                return;
            }
            this.mPasswordEntry.setEnabled(true);
            this.mPasswordEntryInputDisabler.setInputEnabled(true);
            if (!shouldAutoShowSoftKeyboard()) {
                return;
            }
            this.mImm.showSoftInput(this.mPasswordEntry, 1);
        }

        public boolean shouldAutoShowSoftKeyboard() {
            return this.mPasswordEntry.isEnabled() && !this.mUsingFingerprint;
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (!hasFocus || this.mBlockImm) {
                return;
            }
            this.mPasswordEntry.post(new Runnable() {
                @Override
                public void run() {
                    if (ConfirmLockPasswordFragment.this.shouldAutoShowSoftKeyboard()) {
                        ConfirmLockPasswordFragment.this.resetState();
                    } else {
                        ConfirmLockPasswordFragment.this.mImm.hideSoftInputFromWindow(ConfirmLockPasswordFragment.this.mPasswordEntry.getWindowToken(), 1);
                    }
                }
            });
        }

        private void handleNext() {
            if (getActivity() == null) {
                Log.e("ConfirmLockPassword", "error,getActivity() is null");
                return;
            }
            if (this.mPendingLockCheck != null || this.mDisappearing) {
                return;
            }
            this.mPasswordEntryInputDisabler.setInputEnabled(false);
            String pin = this.mPasswordEntry.getText().toString();
            boolean verifyChallenge = getActivity().getIntent().getBooleanExtra("has_challenge", false);
            Intent intent = new Intent();
            if (verifyChallenge) {
                if (isInternalActivity()) {
                    startVerifyPassword(pin, intent);
                    return;
                } else {
                    this.mCredentialCheckResultTracker.setResult(false, intent, 0, this.mEffectiveUserId);
                    return;
                }
            }
            startCheckPassword(pin, intent);
        }

        public boolean isInternalActivity() {
            return getActivity() instanceof InternalActivity;
        }

        private void startVerifyPassword(String pin, final Intent intent) {
            AsyncTask<?, ?, ?> asyncTaskVerifyTiedProfileChallenge;
            long challenge = getActivity().getIntent().getLongExtra("challenge", 0L);
            int localEffectiveUserId = this.mEffectiveUserId;
            final int localUserId = this.mUserId;
            LockPatternChecker.OnVerifyCallback onVerifyCallback = new LockPatternChecker.OnVerifyCallback() {
                public void onVerified(byte[] token, int timeoutMs) {
                    ConfirmLockPasswordFragment.this.mPendingLockCheck = null;
                    boolean matched = false;
                    if (token != null) {
                        matched = true;
                        if (ConfirmLockPasswordFragment.this.mReturnCredentials) {
                            intent.putExtra("hw_auth_token", token);
                        }
                    }
                    ConfirmLockPasswordFragment.this.mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs, localUserId);
                }
            };
            if (localEffectiveUserId == localUserId) {
                asyncTaskVerifyTiedProfileChallenge = LockPatternChecker.verifyPassword(this.mLockPatternUtils, pin, challenge, localUserId, onVerifyCallback);
            } else {
                asyncTaskVerifyTiedProfileChallenge = LockPatternChecker.verifyTiedProfileChallenge(this.mLockPatternUtils, pin, false, challenge, localUserId, onVerifyCallback);
            }
            this.mPendingLockCheck = asyncTaskVerifyTiedProfileChallenge;
        }

        private void startCheckPassword(final String pin, final Intent intent) {
            final int localEffectiveUserId = this.mEffectiveUserId;
            this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, pin, localEffectiveUserId, new LockPatternChecker.OnCheckCallback() {
                public void onChecked(boolean matched, int timeoutMs) {
                    ConfirmLockPasswordFragment.this.mPendingLockCheck = null;
                    if (matched && ConfirmLockPasswordFragment.this.isInternalActivity() && ConfirmLockPasswordFragment.this.mReturnCredentials) {
                        intent.putExtra("type", ConfirmLockPasswordFragment.this.mIsAlpha ? 0 : 3);
                        intent.putExtra("password", pin);
                    }
                    ConfirmLockPasswordFragment.this.mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs, localEffectiveUserId);
                }
            });
        }

        private void startDisappearAnimation(final Intent intent) {
            if (this.mDisappearing) {
                return;
            }
            this.mDisappearing = true;
            if (getActivity().getThemeResId() == 2131689967) {
                this.mDisappearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                    @Override
                    public void run() {
                        if (ConfirmLockPasswordFragment.this.getActivity() == null || ConfirmLockPasswordFragment.this.getActivity().isFinishing()) {
                            return;
                        }
                        ConfirmLockPasswordFragment.this.getActivity().setResult(-1, intent);
                        ConfirmLockPasswordFragment.this.getActivity().finish();
                        ConfirmLockPasswordFragment.this.getActivity().overridePendingTransition(R.anim.confirm_credential_close_enter, R.anim.confirm_credential_close_exit);
                    }
                });
            } else {
                getActivity().setResult(-1, intent);
                getActivity().finish();
            }
        }

        private void onPasswordChecked(boolean matched, Intent intent, int timeoutMs, int effectiveUserId, boolean newResult) {
            this.mPasswordEntryInputDisabler.setInputEnabled(true);
            if (matched) {
                if (newResult) {
                    reportSuccessfullAttempt();
                }
                startDisappearAnimation(intent);
                checkForPendingIntent();
                return;
            }
            if (timeoutMs > 0) {
                long deadline = this.mLockPatternUtils.setLockoutAttemptDeadline(effectiveUserId, timeoutMs);
                handleAttemptLockout(deadline);
            } else {
                showError(getErrorMessage(), 3000L);
            }
            if (!newResult) {
                return;
            }
            reportFailedAttempt();
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs, int effectiveUserId, boolean newResult) {
            onPasswordChecked(matched, intent, timeoutMs, effectiveUserId, newResult);
        }

        @Override
        protected void onShowError() {
            this.mPasswordEntry.setText((CharSequence) null);
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mPasswordEntry.setEnabled(false);
            this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (ConfirmLockPasswordFragment.this.isAdded()) {
                        int secondsCountdown = (int) (millisUntilFinished / 1000);
                        ConfirmLockPasswordFragment.this.showError(ConfirmLockPasswordFragment.this.getString(R.string.lockpattern_too_many_failed_confirmation_attempts, new Object[]{Integer.valueOf(secondsCountdown)}), 0L);
                    }
                }

                @Override
                public void onFinish() {
                    ConfirmLockPasswordFragment.this.resetState();
                    ConfirmLockPasswordFragment.this.mErrorTextView.setText("");
                    if (!ConfirmLockPasswordFragment.this.isProfileChallenge()) {
                        return;
                    }
                    ConfirmLockPasswordFragment.this.updateErrorMessage(ConfirmLockPasswordFragment.this.mLockPatternUtils.getCurrentFailedPasswordAttempts(ConfirmLockPasswordFragment.this.mEffectiveUserId));
                }
            }.start();
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel_button:
                    getActivity().setResult(0);
                    getActivity().finish();
                    break;
                case R.id.next_button:
                    handleNext();
                    break;
            }
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId != 0 && actionId != 6 && actionId != 5) {
                return false;
            }
            handleNext();
            return true;
        }
    }
}
