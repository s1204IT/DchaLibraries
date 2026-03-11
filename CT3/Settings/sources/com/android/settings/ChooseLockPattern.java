package com.android.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.SaveChosenLockWorkerBase;
import com.android.settings.notification.RedactionInterstitial;
import com.google.android.collect.Lists;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
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

    public static Intent createIntent(Context context, boolean requirePassword, boolean confirmCredentials, int userId) {
        Intent intent = new Intent(context, (Class<?>) ChooseLockPattern.class);
        intent.putExtra("key_lock_method", "pattern");
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("extra_require_password", requirePassword);
        intent.putExtra("android.intent.extra.USER_ID", userId);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, String pattern, int userId) {
        Intent intent = createIntent(context, requirePassword, false, userId);
        intent.putExtra("password", pattern);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, long challenge, int userId) {
        Intent intent = createIntent(context, requirePassword, false, userId);
        intent.putExtra("has_challenge", true);
        intent.putExtra("challenge", challenge);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_pattern_header);
        setTitle(msg);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public static class ChooseLockPatternFragment extends InstrumentedFragment implements View.OnClickListener, SaveChosenLockWorkerBase.Listener {

        private static final int[] f2x9316aa93 = null;
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private String mCurrentPattern;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected TextView mFooterText;
        private boolean mHasChallenge;
        protected TextView mHeaderText;
        protected LockPatternView mLockPatternView;
        private SaveAndFinishWorker mSaveAndFinishWorker;
        private int mUserId;
        protected List<LockPatternView.Cell> mChosenPattern = null;
        private boolean mHideDrawer = false;
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
                if (ChooseLockPatternFragment.this.mUiStage == Stage.NeedToConfirm || ChooseLockPatternFragment.this.mUiStage == Stage.ConfirmWrong) {
                    if (ChooseLockPatternFragment.this.mChosenPattern == null) {
                        throw new IllegalStateException("null chosen pattern in stage 'need to confirm");
                    }
                    if (ChooseLockPatternFragment.this.mChosenPattern.equals(pattern)) {
                        ChooseLockPatternFragment.this.updateStage(Stage.ChoiceConfirmed);
                        return;
                    } else {
                        ChooseLockPatternFragment.this.updateStage(Stage.ConfirmWrong);
                        return;
                    }
                }
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
        private Runnable mClearPatternRunnable = new Runnable() {
            @Override
            public void run() {
                ChooseLockPatternFragment.this.mLockPatternView.clearPattern();
            }
        };

        private static int[] m279xd0a3486f() {
            if (f2x9316aa93 != null) {
                return f2x9316aa93;
            }
            int[] iArr = new int[Stage.valuesCustom().length];
            try {
                iArr[Stage.ChoiceConfirmed.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Stage.ChoiceTooShort.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Stage.ConfirmWrong.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Stage.FirstChoiceValid.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[Stage.HelpScreen.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[Stage.Introduction.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[Stage.NeedToConfirm.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            f2x9316aa93 = iArr;
            return iArr;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case 55:
                    if (resultCode != -1) {
                        getActivity().setResult(1);
                        getActivity().finish();
                    } else {
                        this.mCurrentPattern = data.getStringExtra("password");
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

        @Override
        protected int getMetricsCategory() {
            return 29;
        }

        enum LeftButtonMode {
            Cancel(R.string.cancel, true),
            CancelDisabled(R.string.cancel, false),
            Retry(R.string.lockpattern_retry_button_text, true),
            RetryDisabled(R.string.lockpattern_retry_button_text, false),
            Gone(-1, false);

            final boolean enabled;
            final int text;

            public static LeftButtonMode[] valuesCustom() {
                return values();
            }

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

            public static RightButtonMode[] valuesCustom() {
                return values();
            }

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

            public static Stage[] valuesCustom() {
                return values();
            }

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
            Intent intent = getActivity().getIntent();
            this.mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
            if (intent.getBooleanExtra("for_cred_req_boot", false)) {
                SaveAndFinishWorker w = new SaveAndFinishWorker(null);
                boolean required = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
                String current = intent.getStringExtra("password");
                w.setBlocking(true);
                w.setListener(this);
                w.start(this.mChooseLockSettingsHelper.utils(), required, false, 0L, LockPatternUtils.stringToPattern(current), current, this.mUserId);
            }
            this.mHideDrawer = getActivity().getIntent().getBooleanExtra(":settings:hide_drawer", false);
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
            Intent intent = getActivity().getIntent();
            this.mCurrentPattern = intent.getStringExtra("password");
            this.mHasChallenge = intent.getBooleanExtra("has_challenge", false);
            this.mChallenge = intent.getLongExtra("challenge", 0L);
            if (savedInstanceState == null) {
                if (confirmCredentials) {
                    updateStage(Stage.NeedToConfirm);
                    boolean launchedConfirmationActivity = this.mChooseLockSettingsHelper.launchConfirmationActivity(55, getString(R.string.unlock_set_unlock_launch_picker_title), true, this.mUserId);
                    if (launchedConfirmationActivity) {
                        return;
                    }
                    updateStage(Stage.Introduction);
                    return;
                }
                updateStage(Stage.Introduction);
                return;
            }
            String patternString = savedInstanceState.getString("chosenPattern");
            if (patternString != null) {
                this.mChosenPattern = LockPatternUtils.stringToPattern(patternString);
            }
            if (this.mCurrentPattern == null) {
                this.mCurrentPattern = savedInstanceState.getString("currentPattern");
            }
            updateStage(Stage.valuesCustom()[savedInstanceState.getInt("uiStage")]);
            this.mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag("save_and_finish_worker");
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(this.mUiStage);
            if (this.mSaveAndFinishWorker == null) {
                return;
            }
            setRightButtonEnabled(false);
            this.mSaveAndFinishWorker.setListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            if (this.mSaveAndFinishWorker == null) {
                return;
            }
            this.mSaveAndFinishWorker.setListener(null);
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context, this.mUserId);
        }

        public void handleLeftButton() {
            if (this.mUiStage.leftMode == LeftButtonMode.Retry) {
                this.mChosenPattern = null;
                this.mLockPatternView.clearPattern();
                updateStage(Stage.Introduction);
            } else {
                if (this.mUiStage.leftMode == LeftButtonMode.Cancel) {
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
                startSaveAndFinish();
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
            } else {
                if (v != this.mFooterRightButton) {
                    return;
                }
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
            if (this.mCurrentPattern == null) {
                return;
            }
            outState.putString("currentPattern", this.mCurrentPattern);
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
            boolean announceAlways = false;
            switch (m279xd0a3486f()[this.mUiStage.ordinal()]) {
                case DefaultWfcSettingsExt.CREATE:
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                    announceAlways = true;
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                    announceAlways = true;
                    break;
                case 5:
                    this.mLockPatternView.setPattern(LockPatternView.DisplayMode.Animate, this.mAnimatePattern);
                    break;
                case 6:
                    this.mLockPatternView.clearPattern();
                    break;
                case 7:
                    this.mLockPatternView.clearPattern();
                    break;
            }
            if (previousStage == stage && !announceAlways) {
                return;
            }
            this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
        }

        private void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000L);
        }

        private void startSaveAndFinish() {
            SaveAndFinishWorker saveAndFinishWorker = null;
            if (this.mSaveAndFinishWorker != null) {
                Log.w("ChooseLockPattern", "startSaveAndFinish with an existing SaveAndFinishWorker.");
                return;
            }
            setRightButtonEnabled(false);
            this.mSaveAndFinishWorker = new SaveAndFinishWorker(saveAndFinishWorker);
            this.mSaveAndFinishWorker.setListener(this);
            getFragmentManager().beginTransaction().add(this.mSaveAndFinishWorker, "save_and_finish_worker").commit();
            getFragmentManager().executePendingTransactions();
            boolean required = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
            this.mSaveAndFinishWorker.start(this.mChooseLockSettingsHelper.utils(), required, this.mHasChallenge, this.mChallenge, this.mChosenPattern, this.mCurrentPattern, this.mUserId);
        }

        @Override
        public void onChosenLockSaveFinished(boolean wasSecureBefore, Intent resultData) {
            Intent intent;
            getActivity().setResult(1, resultData);
            if (!wasSecureBefore && (intent = getRedactionInterstitialIntent(getActivity())) != null) {
                intent.putExtra(":settings:hide_drawer", this.mHideDrawer);
                startActivity(intent);
            }
            getActivity().finish();
        }
    }

    private static class SaveAndFinishWorker extends SaveChosenLockWorkerBase {
        private List<LockPatternView.Cell> mChosenPattern;
        private String mCurrentPattern;
        private boolean mLockVirgin;

        SaveAndFinishWorker(SaveAndFinishWorker saveAndFinishWorker) {
            this();
        }

        private SaveAndFinishWorker() {
        }

        public void start(LockPatternUtils utils, boolean credentialRequired, boolean hasChallenge, long challenge, List<LockPatternView.Cell> chosenPattern, String currentPattern, int userId) {
            prepare(utils, credentialRequired, hasChallenge, challenge, userId);
            this.mCurrentPattern = currentPattern;
            this.mChosenPattern = chosenPattern;
            this.mUserId = userId;
            this.mLockVirgin = !this.mUtils.isPatternEverChosen(this.mUserId);
            start();
        }

        @Override
        protected Intent saveAndVerifyInBackground() {
            byte[] bArrVerifyPattern;
            int userId = this.mUserId;
            this.mUtils.saveLockPattern(this.mChosenPattern, this.mCurrentPattern, userId);
            if (!this.mHasChallenge) {
                return null;
            }
            try {
                bArrVerifyPattern = this.mUtils.verifyPattern(this.mChosenPattern, this.mChallenge, userId);
            } catch (LockPatternUtils.RequestThrottledException e) {
                bArrVerifyPattern = null;
            }
            if (bArrVerifyPattern == null) {
                Log.e("ChooseLockPattern", "critical: no token returned for known good pattern");
            }
            Intent result = new Intent();
            result.putExtra("hw_auth_token", bArrVerifyPattern);
            return result;
        }

        @Override
        protected void finish(Intent resultData) {
            if (this.mLockVirgin) {
                this.mUtils.setVisiblePatternEnabled(true, this.mUserId);
            }
            super.finish(resultData);
        }
    }
}
