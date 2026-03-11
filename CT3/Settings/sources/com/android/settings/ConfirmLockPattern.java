package com.android.settings;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.CredentialCheckResultTracker;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfirmLockPattern extends ConfirmDeviceCredentialBaseActivity {

    public static class InternalActivity extends ConfirmLockPattern {
    }

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut;

        public static Stage[] valuesCustom() {
            return values();
        }
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", ConfirmLockPatternFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ConfirmLockPatternFragment.class.getName().equals(fragmentName);
    }

    public static class ConfirmLockPatternFragment extends ConfirmDeviceCredentialBaseFragment implements AppearAnimationCreator<Object>, CredentialCheckResultTracker.Listener {

        private static final int[] f3comandroidsettingsConfirmLockPattern$StageSwitchesValues = null;
        private AppearAnimationUtils mAppearAnimationUtils;
        private CountDownTimer mCountdownTimer;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private CharSequence mDetailsText;
        private TextView mDetailsTextView;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private CharSequence mHeaderText;
        private TextView mHeaderTextView;
        private View mLeftSpacerLandscape;
        private LockPatternView mLockPatternView;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private View mRightSpacerLandscape;
        private boolean mDisappearing = false;
        private Runnable mClearPatternRunnable = new Runnable() {
            @Override
            public void run() {
                ConfirmLockPatternFragment.this.mLockPatternView.clearPattern();
            }
        };
        private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener = new LockPatternView.OnPatternListener() {
            public void onPatternStart() {
                ConfirmLockPatternFragment.this.mLockPatternView.removeCallbacks(ConfirmLockPatternFragment.this.mClearPatternRunnable);
            }

            public void onPatternCleared() {
                ConfirmLockPatternFragment.this.mLockPatternView.removeCallbacks(ConfirmLockPatternFragment.this.mClearPatternRunnable);
            }

            public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            }

            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (ConfirmLockPatternFragment.this.mPendingLockCheck != null || ConfirmLockPatternFragment.this.mDisappearing) {
                    return;
                }
                ConfirmLockPatternFragment.this.mLockPatternView.setEnabled(false);
                boolean verifyChallenge = ConfirmLockPatternFragment.this.getActivity().getIntent().getBooleanExtra("has_challenge", false);
                Intent intent = new Intent();
                if (verifyChallenge) {
                    if (isInternalActivity()) {
                        startVerifyPattern(pattern, intent);
                        return;
                    } else {
                        ConfirmLockPatternFragment.this.mCredentialCheckResultTracker.setResult(false, intent, 0, ConfirmLockPatternFragment.this.mEffectiveUserId);
                        return;
                    }
                }
                startCheckPattern(pattern, intent);
            }

            public boolean isInternalActivity() {
                return ConfirmLockPatternFragment.this.getActivity() instanceof InternalActivity;
            }

            private void startVerifyPattern(List<LockPatternView.Cell> pattern, final Intent intent) {
                AsyncTask asyncTaskVerifyTiedProfileChallenge;
                final int localEffectiveUserId = ConfirmLockPatternFragment.this.mEffectiveUserId;
                int localUserId = ConfirmLockPatternFragment.this.mUserId;
                long challenge = ConfirmLockPatternFragment.this.getActivity().getIntent().getLongExtra("challenge", 0L);
                LockPatternChecker.OnVerifyCallback onVerifyCallback = new LockPatternChecker.OnVerifyCallback() {
                    public void onVerified(byte[] token, int timeoutMs) {
                        ConfirmLockPatternFragment.this.mPendingLockCheck = null;
                        boolean matched = false;
                        if (token != null) {
                            matched = true;
                            if (ConfirmLockPatternFragment.this.mReturnCredentials) {
                                intent.putExtra("hw_auth_token", token);
                            }
                        }
                        ConfirmLockPatternFragment.this.mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs, localEffectiveUserId);
                    }
                };
                ConfirmLockPatternFragment confirmLockPatternFragment = ConfirmLockPatternFragment.this;
                if (localEffectiveUserId == localUserId) {
                    asyncTaskVerifyTiedProfileChallenge = LockPatternChecker.verifyPattern(ConfirmLockPatternFragment.this.mLockPatternUtils, pattern, challenge, localUserId, onVerifyCallback);
                } else {
                    asyncTaskVerifyTiedProfileChallenge = LockPatternChecker.verifyTiedProfileChallenge(ConfirmLockPatternFragment.this.mLockPatternUtils, LockPatternUtils.patternToString(pattern), true, challenge, localUserId, onVerifyCallback);
                }
                confirmLockPatternFragment.mPendingLockCheck = asyncTaskVerifyTiedProfileChallenge;
            }

            private void startCheckPattern(final List<LockPatternView.Cell> pattern, final Intent intent) {
                if (pattern.size() < 4) {
                    ConfirmLockPatternFragment.this.mCredentialCheckResultTracker.setResult(false, intent, 0, ConfirmLockPatternFragment.this.mEffectiveUserId);
                    return;
                }
                final int localEffectiveUserId = ConfirmLockPatternFragment.this.mEffectiveUserId;
                ConfirmLockPatternFragment.this.mPendingLockCheck = LockPatternChecker.checkPattern(ConfirmLockPatternFragment.this.mLockPatternUtils, pattern, localEffectiveUserId, new LockPatternChecker.OnCheckCallback() {
                    public void onChecked(boolean matched, int timeoutMs) {
                        ConfirmLockPatternFragment.this.mPendingLockCheck = null;
                        if (matched && isInternalActivity() && ConfirmLockPatternFragment.this.mReturnCredentials) {
                            intent.putExtra("type", 2);
                            intent.putExtra("password", LockPatternUtils.patternToString(pattern));
                        }
                        ConfirmLockPatternFragment.this.mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs, localEffectiveUserId);
                    }
                });
            }
        };

        private static int[] m301getcomandroidsettingsConfirmLockPattern$StageSwitchesValues() {
            if (f3comandroidsettingsConfirmLockPattern$StageSwitchesValues != null) {
                return f3comandroidsettingsConfirmLockPattern$StageSwitchesValues;
            }
            int[] iArr = new int[Stage.valuesCustom().length];
            try {
                iArr[Stage.LockedOut.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Stage.NeedToUnlock.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Stage.NeedToUnlockWrong.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f3comandroidsettingsConfirmLockPattern$StageSwitchesValues = iArr;
            return iArr;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_lock_pattern, (ViewGroup) null);
            this.mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            this.mLockPatternView = view.findViewById(R.id.lockPattern);
            this.mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            this.mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            this.mLeftSpacerLandscape = view.findViewById(R.id.leftSpacer);
            this.mRightSpacerLandscape = view.findViewById(R.id.rightSpacer);
            LinearLayoutWithDefaultTouchRecepient topLayout = view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(this.mLockPatternView);
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                this.mHeaderText = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.header");
                this.mDetailsText = intent.getCharSequenceExtra("com.android.settings.ConfirmCredentials.details");
            }
            this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
            this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(this.mEffectiveUserId));
            this.mLockPatternView.setOnPatternListener(this.mConfirmExistingLockPatternListener);
            updateStage(Stage.NeedToUnlock);
            if (savedInstanceState == null && !this.mLockPatternUtils.isLockPatternEnabled(this.mEffectiveUserId)) {
                getActivity().setResult(-1, new Intent());
                getActivity().finish();
            }
            this.mAppearAnimationUtils = new AppearAnimationUtils(getContext(), 220L, 2.0f, 1.3f, AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.linear_out_slow_in));
            this.mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(), 125L, 4.0f, 0.3f, AnimationUtils.loadInterpolator(getContext(), android.R.interpolator.fast_out_linear_in), new AppearAnimationUtils.RowTranslationScaler() {
                @Override
                public float getRowTranslationScale(int row, int numRows) {
                    return (numRows - row) / numRows;
                }
            });
            setAccessibilityTitle(this.mHeaderTextView.getText());
            this.mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager().findFragmentByTag("check_lock_result");
            if (this.mCredentialCheckResultTracker == null) {
                this.mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(this.mCredentialCheckResultTracker, "check_lock_result").commit();
            }
            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
        }

        @Override
        public void onPause() {
            super.onPause();
            if (this.mCountdownTimer != null) {
                this.mCountdownTimer.cancel();
            }
            this.mCredentialCheckResultTracker.setListener(null);
        }

        @Override
        protected int getMetricsCategory() {
            return 31;
        }

        @Override
        public void onResume() {
            super.onResume();
            long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(this.mEffectiveUserId);
            if (deadline != 0) {
                this.mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else if (!this.mLockPatternView.isEnabled()) {
                updateStage(Stage.NeedToUnlock);
            }
            this.mCredentialCheckResultTracker.setListener(this);
        }

        @Override
        protected void onShowError() {
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            this.mHeaderTextView.setAlpha(0.0f);
            this.mCancelButton.setAlpha(0.0f);
            this.mLockPatternView.setAlpha(0.0f);
            this.mDetailsTextView.setAlpha(0.0f);
            this.mFingerprintIcon.setAlpha(0.0f);
        }

        private int getDefaultDetails() {
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), this.mEffectiveUserId);
            if (isProfile) {
                if (this.mIsStrongAuthRequired) {
                    return R.string.lockpassword_strong_auth_required_reason_restart_work_pattern;
                }
                return R.string.lockpassword_confirm_your_pattern_generic_profile;
            }
            if (this.mIsStrongAuthRequired) {
                return R.string.lockpassword_strong_auth_required_reason_restart_device_pattern;
            }
            return R.string.lockpassword_confirm_your_pattern_generic;
        }

        private Object[][] getActiveViews() {
            ArrayList<ArrayList<Object>> result = new ArrayList<>();
            result.add(new ArrayList<>(Collections.singletonList(this.mHeaderTextView)));
            result.add(new ArrayList<>(Collections.singletonList(this.mDetailsTextView)));
            if (this.mCancelButton.getVisibility() == 0) {
                result.add(new ArrayList<>(Collections.singletonList(this.mCancelButton)));
            }
            LockPatternView.CellState[][] cellStates = this.mLockPatternView.getCellStates();
            for (int i = 0; i < cellStates.length; i++) {
                ArrayList<Object> row = new ArrayList<>();
                for (int j = 0; j < cellStates[i].length; j++) {
                    row.add(cellStates[i][j]);
                }
                result.add(row);
            }
            if (this.mFingerprintIcon.getVisibility() == 0) {
                result.add(new ArrayList<>(Collections.singletonList(this.mFingerprintIcon)));
            }
            Object[][] resultArr = (Object[][]) Array.newInstance((Class<?>) Object.class, result.size(), cellStates[0].length);
            for (int i2 = 0; i2 < result.size(); i2++) {
                ArrayList<Object> row2 = result.get(i2);
                for (int j2 = 0; j2 < row2.size(); j2++) {
                    resultArr[i2][j2] = row2.get(j2);
                }
            }
            return resultArr;
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            this.mLockPatternView.setAlpha(1.0f);
            this.mAppearAnimationUtils.startAnimation2d(getActiveViews(), null, this);
        }

        public void updateStage(Stage stage) {
            switch (m301getcomandroidsettingsConfirmLockPattern$StageSwitchesValues()[stage.ordinal()]) {
                case DefaultWfcSettingsExt.PAUSE:
                    this.mLockPatternView.clearPattern();
                    this.mLockPatternView.setEnabled(false);
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    if (this.mHeaderText != null) {
                        this.mHeaderTextView.setText(this.mHeaderText);
                    } else {
                        this.mHeaderTextView.setText(R.string.lockpassword_confirm_your_pattern_header);
                    }
                    if (this.mDetailsText != null) {
                        this.mDetailsTextView.setText(this.mDetailsText);
                    } else {
                        this.mDetailsTextView.setText(getDefaultDetails());
                    }
                    this.mErrorTextView.setText("");
                    if (isProfileChallenge()) {
                        updateErrorMessage(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(this.mEffectiveUserId));
                    }
                    this.mLockPatternView.setEnabled(true);
                    this.mLockPatternView.enableInput();
                    this.mLockPatternView.clearPattern();
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    this.mErrorTextView.setText(R.string.lockpattern_need_to_unlock_wrong);
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    this.mLockPatternView.setEnabled(true);
                    this.mLockPatternView.enableInput();
                    break;
            }
            this.mHeaderTextView.announceForAccessibility(this.mHeaderTextView.getText());
        }

        private void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000L);
        }

        @Override
        protected void authenticationSucceeded() {
            this.mCredentialCheckResultTracker.setResult(true, new Intent(), 0, this.mEffectiveUserId);
        }

        private void startDisappearAnimation(final Intent intent) {
            if (this.mDisappearing) {
                return;
            }
            this.mDisappearing = true;
            if (getActivity().getThemeResId() == 2131689967) {
                this.mLockPatternView.clearPattern();
                this.mDisappearAnimationUtils.startAnimation2d(getActiveViews(), new Runnable() {
                    @Override
                    public void run() {
                        if (ConfirmLockPatternFragment.this.getActivity() == null || ConfirmLockPatternFragment.this.getActivity().isFinishing()) {
                            return;
                        }
                        ConfirmLockPatternFragment.this.getActivity().setResult(-1, intent);
                        ConfirmLockPatternFragment.this.getActivity().finish();
                        ConfirmLockPatternFragment.this.getActivity().overridePendingTransition(R.anim.confirm_credential_close_enter, R.anim.confirm_credential_close_exit);
                    }
                }, this);
            } else {
                getActivity().setResult(-1, intent);
                getActivity().finish();
            }
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean visible) {
            if (this.mLeftSpacerLandscape == null || this.mRightSpacerLandscape == null) {
                return;
            }
            this.mLeftSpacerLandscape.setVisibility(visible ? 8 : 0);
            this.mRightSpacerLandscape.setVisibility(visible ? 8 : 0);
        }

        private void onPatternChecked(boolean matched, Intent intent, int timeoutMs, int effectiveUserId, boolean newResult) {
            this.mLockPatternView.setEnabled(true);
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
                updateStage(Stage.NeedToUnlockWrong);
                postClearPatternRunnable();
            }
            if (!newResult) {
                return;
            }
            reportFailedAttempt();
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs, int effectiveUserId, boolean newResult) {
            onPatternChecked(matched, intent, timeoutMs, effectiveUserId, newResult);
        }

        @Override
        protected int getLastTryErrorMessage() {
            return R.string.lock_profile_wipe_warning_content_pattern;
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    ConfirmLockPatternFragment.this.mErrorTextView.setText(ConfirmLockPatternFragment.this.getString(R.string.lockpattern_too_many_failed_confirmation_attempts, new Object[]{Integer.valueOf(secondsCountdown)}));
                }

                @Override
                public void onFinish() {
                    ConfirmLockPatternFragment.this.updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }

        @Override
        public void createAnimation(Object obj, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable finishListener) {
            if (obj instanceof LockPatternView.CellState) {
                LockPatternView.CellState animatedCell = (LockPatternView.CellState) obj;
                this.mLockPatternView.startCellStateAnimation(animatedCell, 1.0f, appearing ? 1.0f : 0.0f, appearing ? translationY : 0.0f, appearing ? 0.0f : translationY, appearing ? 0.0f : 1.0f, 1.0f, delay, duration, interpolator, finishListener);
            } else {
                this.mAppearAnimationUtils.createAnimation((View) obj, delay, duration, translationY, appearing, interpolator, finishListener);
            }
        }
    }
}
