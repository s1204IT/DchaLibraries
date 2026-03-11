package com.android.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import java.util.List;

public class ConfirmLockPattern extends SettingsActivity {

    public static class InternalActivity extends ConfirmLockPattern {
    }

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_confirm_your_pattern_header);
        setTitle(msg);
        getWindow().addFlags(8192);
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

    public static class ConfirmLockPatternFragment extends Fragment {
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
                if (ConfirmLockPatternFragment.this.mLockPatternUtils.checkPattern(pattern)) {
                    Intent intent = new Intent();
                    if (ConfirmLockPatternFragment.this.getActivity() instanceof InternalActivity) {
                        intent.putExtra("type", 2);
                        intent.putExtra("password", LockPatternUtils.patternToString(pattern));
                    }
                    ConfirmLockPatternFragment.this.getActivity().setResult(-1, intent);
                    ConfirmLockPatternFragment.this.getActivity().finish();
                    return;
                }
                if (pattern.size() < 4 || ConfirmLockPatternFragment.access$304(ConfirmLockPatternFragment.this) < 5) {
                    ConfirmLockPatternFragment.this.updateStage(Stage.NeedToUnlockWrong);
                    ConfirmLockPatternFragment.this.postClearPatternRunnable();
                } else {
                    long deadline = ConfirmLockPatternFragment.this.mLockPatternUtils.setLockoutAttemptDeadline();
                    ConfirmLockPatternFragment.this.handleAttemptLockout(deadline);
                }
            }
        };
        private CountDownTimer mCountdownTimer;
        private CharSequence mFooterText;
        private TextView mFooterTextView;
        private CharSequence mFooterWrongText;
        private CharSequence mHeaderText;
        private TextView mHeaderTextView;
        private CharSequence mHeaderWrongText;
        private LockPatternUtils mLockPatternUtils;
        private LockPatternView mLockPatternView;
        private int mNumWrongConfirmAttempts;

        static int access$304(ConfirmLockPatternFragment x0) {
            int i = x0.mNumWrongConfirmAttempts + 1;
            x0.mNumWrongConfirmAttempts = i;
            return i;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_lock_pattern, (ViewGroup) null);
            this.mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            this.mLockPatternView = view.findViewById(R.id.lockPattern);
            this.mFooterTextView = (TextView) view.findViewById(R.id.footerText);
            LinearLayoutWithDefaultTouchRecepient topLayout = view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(this.mLockPatternView);
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                this.mHeaderText = intent.getCharSequenceExtra("com.android.settings.ConfirmLockPattern.header");
                this.mFooterText = intent.getCharSequenceExtra("com.android.settings.ConfirmLockPattern.footer");
                this.mHeaderWrongText = intent.getCharSequenceExtra("com.android.settings.ConfirmLockPattern.header_wrong");
                this.mFooterWrongText = intent.getCharSequenceExtra("com.android.settings.ConfirmLockPattern.footer_wrong");
            }
            this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
            this.mLockPatternView.setOnPatternListener(this.mConfirmExistingLockPatternListener);
            updateStage(Stage.NeedToUnlock);
            if (savedInstanceState != null) {
                this.mNumWrongConfirmAttempts = savedInstanceState.getInt("num_wrong_attempts");
            } else if (!this.mLockPatternUtils.savedPatternExists()) {
                getActivity().setResult(-1);
                getActivity().finish();
            }
            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt("num_wrong_attempts", this.mNumWrongConfirmAttempts);
        }

        @Override
        public void onPause() {
            super.onPause();
            if (this.mCountdownTimer != null) {
                this.mCountdownTimer.cancel();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline();
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            } else if (!this.mLockPatternView.isEnabled()) {
                this.mNumWrongConfirmAttempts = 0;
                updateStage(Stage.NeedToUnlock);
            }
        }

        public void updateStage(Stage stage) {
            switch (stage) {
                case NeedToUnlock:
                    if (this.mHeaderText != null) {
                        this.mHeaderTextView.setText(this.mHeaderText);
                    } else {
                        this.mHeaderTextView.setText(R.string.lockpattern_need_to_unlock);
                    }
                    if (this.mFooterText != null) {
                        this.mFooterTextView.setText(this.mFooterText);
                    } else {
                        this.mFooterTextView.setText(R.string.lockpattern_need_to_unlock_footer);
                    }
                    this.mLockPatternView.setEnabled(true);
                    this.mLockPatternView.enableInput();
                    break;
                case NeedToUnlockWrong:
                    if (this.mHeaderWrongText != null) {
                        this.mHeaderTextView.setText(this.mHeaderWrongText);
                    } else {
                        this.mHeaderTextView.setText(R.string.lockpattern_need_to_unlock_wrong);
                    }
                    if (this.mFooterWrongText != null) {
                        this.mFooterTextView.setText(this.mFooterWrongText);
                    } else {
                        this.mFooterTextView.setText(R.string.lockpattern_need_to_unlock_wrong_footer);
                    }
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    this.mLockPatternView.setEnabled(true);
                    this.mLockPatternView.enableInput();
                    break;
                case LockedOut:
                    this.mLockPatternView.clearPattern();
                    this.mLockPatternView.setEnabled(false);
                    break;
            }
            this.mHeaderTextView.announceForAccessibility(this.mHeaderTextView.getText());
        }

        public void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000L);
        }

        public void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    ConfirmLockPatternFragment.this.mHeaderTextView.setText(R.string.lockpattern_too_many_failed_confirmation_attempts_header);
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    ConfirmLockPatternFragment.this.mFooterTextView.setText(ConfirmLockPatternFragment.this.getString(R.string.lockpattern_too_many_failed_confirmation_attempts_footer, Integer.valueOf(secondsCountdown)));
                }

                @Override
                public void onFinish() {
                    ConfirmLockPatternFragment.this.mNumWrongConfirmAttempts = 0;
                    ConfirmLockPatternFragment.this.updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }
    }
}
