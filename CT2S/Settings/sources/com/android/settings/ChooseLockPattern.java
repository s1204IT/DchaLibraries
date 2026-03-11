package com.android.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.notification.RedactionInterstitial;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChooseLockPattern extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, boolean isFallback, boolean requirePassword, boolean confirmCredentials) {
        Intent intent = new Intent(context, (Class<?>) ChooseLockPattern.class);
        intent.putExtra("key_lock_method", "pattern");
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("lockscreen.biometric_weak_fallback", isFallback);
        intent.putExtra("extra_require_password", requirePassword);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPatternFragment.class;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_pattern_header);
        setTitle(msg);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public static class ChooseLockPatternFragment extends Fragment implements View.OnClickListener {
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected TextView mFooterText;
        protected TextView mHeaderText;
        protected LockPatternView mLockPatternView;
        protected List<LockPatternView.Cell> mChosenPattern = null;
        private final List<LockPatternView.Cell> mAnimatePattern = Collections.unmodifiableList(Lists.newArrayList(new LockPatternView.Cell[]{LockPatternView.Cell.of(0, 0), LockPatternView.Cell.of(0, 1), LockPatternView.Cell.of(1, 1), LockPatternView.Cell.of(2, 1)}));
        protected LockPatternView.OnPatternListener mChooseNewLockPatternListener = new LockPatternView.OnPatternListener() {
            public void onPatternStart() {
                ChooseLockPatternFragment.this.mLockPatternView.removeCallbacks(ChooseLockPatternFragment.this.mClearPatternRunnable);
                patternInProgress();
            }

            public void onPatternCleared() {
                ChooseLockPatternFragment.this.mLockPatternView.removeCallbacks(ChooseLockPatternFragment.this.mClearPatternRunnable);
            }

            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (ChooseLockPatternFragment.this.mUiStage != Stage.NeedToConfirm && ChooseLockPatternFragment.this.mUiStage != Stage.ConfirmWrong) {
                    if (ChooseLockPatternFragment.this.mUiStage == Stage.Introduction || ChooseLockPatternFragment.this.mUiStage == Stage.ChoiceTooShort) {
                        if (pattern.size() < 4) {
                            ChooseLockPatternFragment.this.updateStage(Stage.ChoiceTooShort);
                            return;
                        }
                        ChooseLockPatternFragment.this.mChosenPattern = new ArrayList(pattern);
                        ChooseLockPatternFragment.this.updateStage(Stage.FirstChoiceValid);
                        return;
                    }
                    throw new IllegalStateException("Unexpected stage " + ChooseLockPatternFragment.this.mUiStage + " when entering the pattern.");
                }
                if (ChooseLockPatternFragment.this.mChosenPattern == null) {
                    throw new IllegalStateException("null chosen pattern in stage 'need to confirm");
                }
                if (ChooseLockPatternFragment.this.mChosenPattern.equals(pattern)) {
                    ChooseLockPatternFragment.this.updateStage(Stage.ChoiceConfirmed);
                } else {
                    ChooseLockPatternFragment.this.updateStage(Stage.ConfirmWrong);
                }
            }

            public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            }

            private void patternInProgress() {
                ChooseLockPatternFragment.this.mHeaderText.setText(R.string.lockpattern_recording_inprogress);
                ChooseLockPatternFragment.this.mFooterText.setText("");
                ChooseLockPatternFragment.this.mFooterLeftButton.setEnabled(false);
                ChooseLockPatternFragment.this.mFooterRightButton.setEnabled(false);
            }
        };
        private Stage mUiStage = Stage.Introduction;
        private boolean mDone = false;
        private Runnable mClearPatternRunnable = new Runnable() {
            @Override
            public void run() {
                ChooseLockPatternFragment.this.mLockPatternView.clearPattern();
            }
        };

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case 55:
                    if (resultCode != -1) {
                        getActivity().setResult(1);
                        getActivity().finish();
                    }
                    updateStage(Stage.Introduction);
                    break;
            }
        }

        protected void setRightButtonEnabled(boolean enabled) {
            this.mFooterRightButton.setEnabled(enabled);
        }

        protected void setRightButtonText(int text) {
            this.mFooterRightButton.setText(text);
        }

        enum LeftButtonMode {
            Cancel(R.string.cancel, true),
            CancelDisabled(R.string.cancel, false),
            Retry(R.string.lockpattern_retry_button_text, true),
            RetryDisabled(R.string.lockpattern_retry_button_text, false),
            Gone(-1, false);

            final boolean enabled;
            final int text;

            LeftButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }
        }

        enum RightButtonMode {
            Continue(R.string.lockpattern_continue_button_text, true),
            ContinueDisabled(R.string.lockpattern_continue_button_text, false),
            Confirm(R.string.lockpattern_confirm_button_text, true),
            ConfirmDisabled(R.string.lockpattern_confirm_button_text, false),
            Ok(android.R.string.ok, true);

            final boolean enabled;
            final int text;

            RightButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }
        }

        protected enum Stage {
            Introduction(R.string.lockpattern_recording_intro_header, LeftButtonMode.Cancel, RightButtonMode.ContinueDisabled, -1, true),
            HelpScreen(R.string.lockpattern_settings_help_how_to_record, LeftButtonMode.Gone, RightButtonMode.Ok, -1, false),
            ChoiceTooShort(R.string.lockpattern_recording_incorrect_too_short, LeftButtonMode.Retry, RightButtonMode.ContinueDisabled, -1, true),
            FirstChoiceValid(R.string.lockpattern_pattern_entered_header, LeftButtonMode.Retry, RightButtonMode.Continue, -1, false),
            NeedToConfirm(R.string.lockpattern_need_to_confirm, LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled, -1, true),
            ConfirmWrong(R.string.lockpattern_need_to_unlock_wrong, LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled, -1, true),
            ChoiceConfirmed(R.string.lockpattern_pattern_confirmed_header, LeftButtonMode.Cancel, RightButtonMode.Confirm, -1, false);

            final int footerMessage;
            final int headerMessage;
            final LeftButtonMode leftMode;
            final boolean patternEnabled;
            final RightButtonMode rightMode;

            Stage(int headerMessage, LeftButtonMode leftMode, RightButtonMode rightMode, int footerMessage, boolean patternEnabled) {
                this.headerMessage = headerMessage;
                this.leftMode = leftMode;
                this.rightMode = rightMode;
                this.footerMessage = footerMessage;
                this.patternEnabled = patternEnabled;
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            if (!(getActivity() instanceof ChooseLockPattern)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_lock_pattern, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            this.mHeaderText = (TextView) view.findViewById(R.id.headerText);
            this.mLockPatternView = view.findViewById(R.id.lockPattern);
            this.mLockPatternView.setOnPatternListener(this.mChooseNewLockPatternListener);
            this.mLockPatternView.setTactileFeedbackEnabled(this.mChooseLockSettingsHelper.utils().isTactileFeedbackEnabled());
            this.mFooterText = (TextView) view.findViewById(R.id.footerText);
            this.mFooterLeftButton = (TextView) view.findViewById(R.id.footerLeftButton);
            this.mFooterRightButton = (TextView) view.findViewById(R.id.footerRightButton);
            this.mFooterLeftButton.setOnClickListener(this);
            this.mFooterRightButton.setOnClickListener(this);
            LinearLayoutWithDefaultTouchRecepient topLayout = view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(this.mLockPatternView);
            boolean confirmCredentials = getActivity().getIntent().getBooleanExtra("confirm_credentials", true);
            if (savedInstanceState == null) {
                if (confirmCredentials) {
                    updateStage(Stage.NeedToConfirm);
                    boolean launchedConfirmationActivity = this.mChooseLockSettingsHelper.launchConfirmationActivity(55, null, null);
                    if (!launchedConfirmationActivity) {
                        updateStage(Stage.Introduction);
                    }
                } else {
                    updateStage(Stage.Introduction);
                }
            } else {
                String patternString = savedInstanceState.getString("chosenPattern");
                if (patternString != null) {
                    this.mChosenPattern = LockPatternUtils.stringToPattern(patternString);
                }
                updateStage(Stage.values()[savedInstanceState.getInt("uiStage")]);
            }
            this.mDone = false;
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context);
        }

        public void handleLeftButton() {
            if (this.mUiStage.leftMode == LeftButtonMode.Retry) {
                this.mChosenPattern = null;
                this.mLockPatternView.clearPattern();
                updateStage(Stage.Introduction);
            } else {
                if (this.mUiStage.leftMode == LeftButtonMode.Cancel) {
                    getActivity().setResult(1);
                    getActivity().finish();
                    return;
                }
                throw new IllegalStateException("left footer button pressed, but stage of " + this.mUiStage + " doesn't make sense");
            }
        }

        public void handleRightButton() {
            if (this.mUiStage.rightMode == RightButtonMode.Continue) {
                if (this.mUiStage != Stage.FirstChoiceValid) {
                    throw new IllegalStateException("expected ui stage " + Stage.FirstChoiceValid + " when button is " + RightButtonMode.Continue);
                }
                updateStage(Stage.NeedToConfirm);
            } else if (this.mUiStage.rightMode == RightButtonMode.Confirm) {
                if (this.mUiStage != Stage.ChoiceConfirmed) {
                    throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed + " when button is " + RightButtonMode.Confirm);
                }
                saveChosenPatternAndFinish();
            } else if (this.mUiStage.rightMode == RightButtonMode.Ok) {
                if (this.mUiStage != Stage.HelpScreen) {
                    throw new IllegalStateException("Help screen is only mode with ok button, but stage is " + this.mUiStage);
                }
                this.mLockPatternView.clearPattern();
                this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                updateStage(Stage.Introduction);
            }
        }

        @Override
        public void onClick(View v) {
            if (v == this.mFooterLeftButton) {
                handleLeftButton();
            } else if (v == this.mFooterRightButton) {
                handleRightButton();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("uiStage", this.mUiStage.ordinal());
            if (this.mChosenPattern != null) {
                outState.putString("chosenPattern", LockPatternUtils.patternToString(this.mChosenPattern));
            }
        }

        protected void updateStage(Stage stage) {
            Stage previousStage = this.mUiStage;
            this.mUiStage = stage;
            if (stage == Stage.ChoiceTooShort) {
                this.mHeaderText.setText(getResources().getString(stage.headerMessage, 4));
            } else {
                this.mHeaderText.setText(stage.headerMessage);
            }
            if (stage.footerMessage == -1) {
                this.mFooterText.setText("");
            } else {
                this.mFooterText.setText(stage.footerMessage);
            }
            if (stage.leftMode == LeftButtonMode.Gone) {
                this.mFooterLeftButton.setVisibility(8);
            } else {
                this.mFooterLeftButton.setVisibility(0);
                this.mFooterLeftButton.setText(stage.leftMode.text);
                this.mFooterLeftButton.setEnabled(stage.leftMode.enabled);
            }
            setRightButtonText(stage.rightMode.text);
            setRightButtonEnabled(stage.rightMode.enabled);
            if (stage.patternEnabled) {
                this.mLockPatternView.enableInput();
            } else {
                this.mLockPatternView.disableInput();
            }
            this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
            switch (this.mUiStage) {
                case Introduction:
                    this.mLockPatternView.clearPattern();
                    break;
                case HelpScreen:
                    this.mLockPatternView.setPattern(LockPatternView.DisplayMode.Animate, this.mAnimatePattern);
                    break;
                case ChoiceTooShort:
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
                case NeedToConfirm:
                    this.mLockPatternView.clearPattern();
                    break;
                case ConfirmWrong:
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
            }
            if (previousStage != stage) {
                this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            }
        }

        private void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000L);
        }

        private void saveChosenPatternAndFinish() {
            if (!this.mDone) {
                LockPatternUtils utils = this.mChooseLockSettingsHelper.utils();
                boolean lockVirgin = !utils.isPatternEverChosen();
                boolean isFallback = getActivity().getIntent().getBooleanExtra("lockscreen.biometric_weak_fallback", false);
                boolean wasSecureBefore = utils.isSecure();
                boolean required = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
                utils.setCredentialRequiredToDecrypt(required);
                utils.setLockPatternEnabled(true);
                utils.saveLockPattern(this.mChosenPattern, isFallback);
                if (lockVirgin) {
                    utils.setVisiblePatternEnabled(true);
                }
                if (!wasSecureBefore) {
                    startActivity(getRedactionInterstitialIntent(getActivity()));
                }
                getActivity().setResult(1);
                getActivity().finish();
                this.mDone = true;
            }
        }
    }
}
