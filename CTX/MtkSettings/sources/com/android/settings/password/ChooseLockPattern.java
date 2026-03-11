package com.android.settings.password;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.notification.RedactionInterstitial;
import com.android.settings.password.SaveChosenLockWorkerBase;
import com.android.setupwizardlib.GlifLayout;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChooseLockPattern extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return intent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int i, boolean z) {
        super.onApplyThemeResource(theme, SetupWizardUtils.getTheme(getIntent()), z);
    }

    public static class IntentBuilder {
        private final Intent mIntent;

        public IntentBuilder(Context context) {
            this.mIntent = new Intent(context, (Class<?>) ChooseLockPattern.class);
            this.mIntent.putExtra("extra_require_password", false);
            this.mIntent.putExtra("confirm_credentials", false);
            this.mIntent.putExtra("has_challenge", false);
        }

        public IntentBuilder setUserId(int i) {
            this.mIntent.putExtra("android.intent.extra.USER_ID", i);
            return this;
        }

        public IntentBuilder setChallenge(long j) {
            this.mIntent.putExtra("has_challenge", true);
            this.mIntent.putExtra("challenge", j);
            return this;
        }

        public IntentBuilder setPattern(String str) {
            this.mIntent.putExtra("password", str);
            return this;
        }

        public IntentBuilder setForFingerprint(boolean z) {
            this.mIntent.putExtra("for_fingerprint", z);
            return this;
        }

        public Intent build() {
            return this.mIntent;
        }
    }

    @Override
    protected boolean isValidFragment(String str) {
        return ChooseLockPatternFragment.class.getName().equals(str);
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPatternFragment.class;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(getIntent().getBooleanExtra("for_fingerprint", false) ? R.string.lockpassword_choose_your_pattern_header_for_fingerprint : R.string.lockpassword_choose_your_screen_lock_header);
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        return super.onKeyDown(i, keyEvent);
    }

    public static class ChooseLockPatternFragment extends InstrumentedFragment implements View.OnClickListener, SaveChosenLockWorkerBase.Listener {
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private String mCurrentPattern;
        private ColorStateList mDefaultHeaderColorList;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected TextView mFooterText;
        protected boolean mForFingerprint;
        private boolean mHasChallenge;
        protected TextView mHeaderText;
        protected LockPatternView mLockPatternView;
        protected TextView mMessageText;
        private SaveAndFinishWorker mSaveAndFinishWorker;
        private ScrollView mTitleHeaderScrollView;
        protected TextView mTitleText;
        protected int mUserId;
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

            public void onPatternDetected(List<LockPatternView.Cell> list) {
                if (ChooseLockPatternFragment.this.mUiStage != Stage.NeedToConfirm && ChooseLockPatternFragment.this.mUiStage != Stage.ConfirmWrong) {
                    if (ChooseLockPatternFragment.this.mUiStage != Stage.Introduction && ChooseLockPatternFragment.this.mUiStage != Stage.ChoiceTooShort) {
                        throw new IllegalStateException("Unexpected stage " + ChooseLockPatternFragment.this.mUiStage + " when entering the pattern.");
                    }
                    if (list.size() < 4) {
                        ChooseLockPatternFragment.this.updateStage(Stage.ChoiceTooShort);
                        return;
                    }
                    ChooseLockPatternFragment.this.mChosenPattern = new ArrayList(list);
                    ChooseLockPatternFragment.this.updateStage(Stage.FirstChoiceValid);
                    return;
                }
                if (ChooseLockPatternFragment.this.mChosenPattern == null) {
                    throw new IllegalStateException("null chosen pattern in stage 'need to confirm");
                }
                if (ChooseLockPatternFragment.this.mChosenPattern.equals(list)) {
                    ChooseLockPatternFragment.this.updateStage(Stage.ChoiceConfirmed);
                } else {
                    ChooseLockPatternFragment.this.updateStage(Stage.ConfirmWrong);
                }
            }

            public void onPatternCellAdded(List<LockPatternView.Cell> list) {
            }

            private void patternInProgress() {
                ChooseLockPatternFragment.this.mHeaderText.setText(R.string.lockpattern_recording_inprogress);
                if (ChooseLockPatternFragment.this.mDefaultHeaderColorList != null) {
                    ChooseLockPatternFragment.this.mHeaderText.setTextColor(ChooseLockPatternFragment.this.mDefaultHeaderColorList);
                }
                ChooseLockPatternFragment.this.mFooterText.setText("");
                ChooseLockPatternFragment.this.mFooterLeftButton.setEnabled(false);
                ChooseLockPatternFragment.this.mFooterRightButton.setEnabled(false);
                if (ChooseLockPatternFragment.this.mTitleHeaderScrollView != null) {
                    ChooseLockPatternFragment.this.mTitleHeaderScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            ChooseLockPatternFragment.this.mTitleHeaderScrollView.fullScroll(130);
                        }
                    });
                }
            }
        };
        private Stage mUiStage = Stage.Introduction;
        private Runnable mClearPatternRunnable = new Runnable() {
            @Override
            public void run() {
                ChooseLockPatternFragment.this.mLockPatternView.clearPattern();
            }
        };

        @Override
        public void onActivityResult(int i, int i2, Intent intent) {
            super.onActivityResult(i, i2, intent);
            if (i == 55) {
                if (i2 != -1) {
                    getActivity().setResult(1);
                    getActivity().finish();
                } else {
                    this.mCurrentPattern = intent.getStringExtra("password");
                }
                updateStage(Stage.Introduction);
            }
        }

        protected void setRightButtonEnabled(boolean z) {
            this.mFooterRightButton.setEnabled(z);
        }

        protected void setRightButtonText(int i) {
            this.mFooterRightButton.setText(i);
        }

        @Override
        public int getMetricsCategory() {
            return 29;
        }

        enum LeftButtonMode {
            Retry(R.string.lockpattern_retry_button_text, true),
            RetryDisabled(R.string.lockpattern_retry_button_text, false),
            Gone(-1, false);

            final boolean enabled;
            final int text;

            LeftButtonMode(int i, boolean z) {
                this.text = i;
                this.enabled = z;
            }
        }

        enum RightButtonMode {
            Continue(R.string.next_label, true),
            ContinueDisabled(R.string.next_label, false),
            Confirm(R.string.lockpattern_confirm_button_text, true),
            ConfirmDisabled(R.string.lockpattern_confirm_button_text, false),
            Ok(android.R.string.ok, true);

            final boolean enabled;
            final int text;

            RightButtonMode(int i, boolean z) {
                this.text = i;
                this.enabled = z;
            }
        }

        protected enum Stage {
            Introduction(R.string.lock_settings_picker_fingerprint_added_security_message, R.string.lockpassword_choose_your_pattern_message, R.string.lockpattern_recording_intro_header, LeftButtonMode.Gone, RightButtonMode.ContinueDisabled, -1, true),
            HelpScreen(-1, -1, R.string.lockpattern_settings_help_how_to_record, LeftButtonMode.Gone, RightButtonMode.Ok, -1, false),
            ChoiceTooShort(R.string.lock_settings_picker_fingerprint_added_security_message, R.string.lockpassword_choose_your_pattern_message, R.string.lockpattern_recording_incorrect_too_short, LeftButtonMode.Retry, RightButtonMode.ContinueDisabled, -1, true),
            FirstChoiceValid(R.string.lock_settings_picker_fingerprint_added_security_message, R.string.lockpassword_choose_your_pattern_message, R.string.lockpattern_pattern_entered_header, LeftButtonMode.Retry, RightButtonMode.Continue, -1, false),
            NeedToConfirm(-1, -1, R.string.lockpattern_need_to_confirm, LeftButtonMode.Gone, RightButtonMode.ConfirmDisabled, -1, true),
            ConfirmWrong(-1, -1, R.string.lockpattern_need_to_unlock_wrong, LeftButtonMode.Gone, RightButtonMode.ConfirmDisabled, -1, true),
            ChoiceConfirmed(-1, -1, R.string.lockpattern_pattern_confirmed_header, LeftButtonMode.Gone, RightButtonMode.Confirm, -1, false);

            final int footerMessage;
            final int headerMessage;
            final LeftButtonMode leftMode;
            final int message;
            final int messageForFingerprint;
            final boolean patternEnabled;
            final RightButtonMode rightMode;

            Stage(int i, int i2, int i3, LeftButtonMode leftButtonMode, RightButtonMode rightButtonMode, int i4, boolean z) {
                this.headerMessage = i3;
                this.messageForFingerprint = i;
                this.message = i2;
                this.leftMode = leftButtonMode;
                this.rightMode = rightButtonMode;
                this.footerMessage = i4;
                this.patternEnabled = z;
            }
        }

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            if (!(getActivity() instanceof ChooseLockPattern)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            Intent intent = getActivity().getIntent();
            this.mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
            if (intent.getBooleanExtra("for_cred_req_boot", false)) {
                SaveAndFinishWorker saveAndFinishWorker = new SaveAndFinishWorker();
                boolean booleanExtra = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
                String stringExtra = intent.getStringExtra("password");
                saveAndFinishWorker.setBlocking(true);
                saveAndFinishWorker.setListener(this);
                saveAndFinishWorker.start(this.mChooseLockSettingsHelper.utils(), booleanExtra, false, 0L, LockPatternUtils.stringToPattern(stringExtra), stringExtra, this.mUserId);
            }
            this.mHideDrawer = getActivity().getIntent().getBooleanExtra(":settings:hide_drawer", false);
            this.mForFingerprint = intent.getBooleanExtra("for_fingerprint", false);
        }

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            GlifLayout glifLayout = (GlifLayout) layoutInflater.inflate(R.layout.choose_lock_pattern, viewGroup, false);
            glifLayout.setHeaderText(getActivity().getTitle());
            if (getResources().getBoolean(R.bool.config_lock_pattern_minimal_ui)) {
                View viewFindViewById = glifLayout.findViewById(R.id.suw_layout_icon);
                if (viewFindViewById != null) {
                    viewFindViewById.setVisibility(8);
                }
            } else if (this.mForFingerprint) {
                glifLayout.setIcon(getActivity().getDrawable(R.drawable.ic_fingerprint_header));
            }
            return glifLayout;
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            this.mTitleText = (TextView) view.findViewById(R.id.suw_layout_title);
            this.mHeaderText = (TextView) view.findViewById(R.id.headerText);
            this.mDefaultHeaderColorList = this.mHeaderText.getTextColors();
            this.mMessageText = (TextView) view.findViewById(R.id.message);
            this.mLockPatternView = view.findViewById(R.id.lockPattern);
            this.mLockPatternView.setOnPatternListener(this.mChooseNewLockPatternListener);
            this.mLockPatternView.setTactileFeedbackEnabled(this.mChooseLockSettingsHelper.utils().isTactileFeedbackEnabled());
            this.mLockPatternView.setFadePattern(false);
            this.mFooterText = (TextView) view.findViewById(R.id.footerText);
            this.mFooterLeftButton = (TextView) view.findViewById(R.id.footerLeftButton);
            this.mFooterRightButton = (TextView) view.findViewById(R.id.footerRightButton);
            this.mTitleHeaderScrollView = (ScrollView) view.findViewById(R.id.scroll_layout_title_header);
            this.mFooterLeftButton.setOnClickListener(this);
            this.mFooterRightButton.setOnClickListener(this);
            view.findViewById(R.id.topLayout).setDefaultTouchRecepient(this.mLockPatternView);
            boolean booleanExtra = getActivity().getIntent().getBooleanExtra("confirm_credentials", true);
            Intent intent = getActivity().getIntent();
            this.mCurrentPattern = intent.getStringExtra("password");
            this.mHasChallenge = intent.getBooleanExtra("has_challenge", false);
            this.mChallenge = intent.getLongExtra("challenge", 0L);
            if (bundle == null) {
                if (booleanExtra) {
                    updateStage(Stage.NeedToConfirm);
                    if (!this.mChooseLockSettingsHelper.launchConfirmationActivity(55, getString(R.string.unlock_set_unlock_launch_picker_title), true, this.mUserId)) {
                        updateStage(Stage.Introduction);
                        return;
                    }
                    return;
                }
                updateStage(Stage.Introduction);
                return;
            }
            String string = bundle.getString("chosenPattern");
            if (string != null) {
                this.mChosenPattern = LockPatternUtils.stringToPattern(string);
            }
            if (this.mCurrentPattern == null) {
                this.mCurrentPattern = bundle.getString("currentPattern");
            }
            updateStage(Stage.values()[bundle.getInt("uiStage")]);
            this.mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag("save_and_finish_worker");
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(this.mUiStage);
            if (this.mSaveAndFinishWorker != null) {
                setRightButtonEnabled(false);
                this.mSaveAndFinishWorker.setListener(this);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (this.mSaveAndFinishWorker != null) {
                this.mSaveAndFinishWorker.setListener(null);
            }
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
                throw new IllegalStateException("left footer button pressed, but stage of " + this.mUiStage + " doesn't make sense");
            }
        }

        public void handleRightButton() {
            if (this.mUiStage.rightMode == RightButtonMode.Continue) {
                if (this.mUiStage != Stage.FirstChoiceValid) {
                    throw new IllegalStateException("expected ui stage " + Stage.FirstChoiceValid + " when button is " + RightButtonMode.Continue);
                }
                updateStage(Stage.NeedToConfirm);
                return;
            }
            if (this.mUiStage.rightMode == RightButtonMode.Confirm) {
                if (this.mUiStage != Stage.ChoiceConfirmed) {
                    throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed + " when button is " + RightButtonMode.Confirm);
                }
                startSaveAndFinish();
                return;
            }
            if (this.mUiStage.rightMode == RightButtonMode.Ok) {
                if (this.mUiStage != Stage.HelpScreen) {
                    throw new IllegalStateException("Help screen is only mode with ok button, but stage is " + this.mUiStage);
                }
                this.mLockPatternView.clearPattern();
                this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                updateStage(Stage.Introduction);
            }
        }

        @Override
        public void onClick(View view) {
            if (view == this.mFooterLeftButton) {
                handleLeftButton();
            } else if (view == this.mFooterRightButton) {
                handleRightButton();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putInt("uiStage", this.mUiStage.ordinal());
            if (this.mChosenPattern != null) {
                bundle.putString("chosenPattern", LockPatternUtils.patternToString(this.mChosenPattern));
            }
            if (this.mCurrentPattern != null) {
                bundle.putString("currentPattern", this.mCurrentPattern);
            }
        }

        protected void updateStage(Stage stage) {
            Stage stage2 = this.mUiStage;
            this.mUiStage = stage;
            boolean z = true;
            if (stage == Stage.ChoiceTooShort) {
                this.mHeaderText.setText(getResources().getString(stage.headerMessage, 4));
            } else {
                this.mHeaderText.setText(stage.headerMessage);
            }
            int i = this.mForFingerprint ? stage.messageForFingerprint : stage.message;
            if (i == -1) {
                this.mMessageText.setText("");
            } else {
                this.mMessageText.setText(i);
            }
            if (stage.footerMessage == -1) {
                this.mFooterText.setText("");
            } else {
                this.mFooterText.setText(stage.footerMessage);
            }
            if (stage == Stage.ConfirmWrong || stage == Stage.ChoiceTooShort) {
                TypedValue typedValue = new TypedValue();
                getActivity().getTheme().resolveAttribute(R.attr.colorError, typedValue, true);
                this.mHeaderText.setTextColor(typedValue.data);
            } else {
                if (this.mDefaultHeaderColorList != null) {
                    this.mHeaderText.setTextColor(this.mDefaultHeaderColorList);
                }
                if (stage == Stage.NeedToConfirm && this.mForFingerprint) {
                    this.mHeaderText.setText("");
                    this.mTitleText.setText(R.string.lockpassword_draw_your_pattern_again_header);
                }
            }
            updateFooterLeftButton(stage, this.mFooterLeftButton);
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
                    z = false;
                    break;
                case HelpScreen:
                    this.mLockPatternView.setPattern(LockPatternView.DisplayMode.Animate, this.mAnimatePattern);
                    z = false;
                    break;
                case ChoiceTooShort:
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
                case FirstChoiceValid:
                default:
                    z = false;
                    break;
                case NeedToConfirm:
                    this.mLockPatternView.clearPattern();
                    z = false;
                    break;
                case ConfirmWrong:
                    this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
            }
            if (stage2 != stage || z) {
                this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            }
        }

        protected void updateFooterLeftButton(Stage stage, TextView textView) {
            if (stage.leftMode == LeftButtonMode.Gone) {
                textView.setVisibility(8);
                return;
            }
            textView.setVisibility(0);
            textView.setText(stage.leftMode.text);
            textView.setEnabled(stage.leftMode.enabled);
        }

        private void postClearPatternRunnable() {
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 2000L);
        }

        private void startSaveAndFinish() {
            if (this.mSaveAndFinishWorker != null) {
                Log.w("ChooseLockPattern", "startSaveAndFinish with an existing SaveAndFinishWorker.");
                return;
            }
            setRightButtonEnabled(false);
            this.mSaveAndFinishWorker = new SaveAndFinishWorker();
            this.mSaveAndFinishWorker.setListener(this);
            getFragmentManager().beginTransaction().add(this.mSaveAndFinishWorker, "save_and_finish_worker").commit();
            getFragmentManager().executePendingTransactions();
            this.mSaveAndFinishWorker.start(this.mChooseLockSettingsHelper.utils(), getActivity().getIntent().getBooleanExtra("extra_require_password", true), this.mHasChallenge, this.mChallenge, this.mChosenPattern, this.mCurrentPattern, this.mUserId);
        }

        @Override
        public void onChosenLockSaveFinished(boolean z, Intent intent) {
            Intent redactionInterstitialIntent;
            getActivity().setResult(1, intent);
            if (!z && (redactionInterstitialIntent = getRedactionInterstitialIntent(getActivity())) != null) {
                redactionInterstitialIntent.putExtra(":settings:hide_drawer", this.mHideDrawer);
                startActivity(redactionInterstitialIntent);
            }
            getActivity().finish();
        }
    }

    public static class SaveAndFinishWorker extends SaveChosenLockWorkerBase {
        private List<LockPatternView.Cell> mChosenPattern;
        private String mCurrentPattern;
        private boolean mLockVirgin;

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
        }

        @Override
        public void setBlocking(boolean z) {
            super.setBlocking(z);
        }

        @Override
        public void setListener(SaveChosenLockWorkerBase.Listener listener) {
            super.setListener(listener);
        }

        public void start(LockPatternUtils lockPatternUtils, boolean z, boolean z2, long j, List<LockPatternView.Cell> list, String str, int i) {
            prepare(lockPatternUtils, z, z2, j, i);
            this.mCurrentPattern = str;
            this.mChosenPattern = list;
            this.mUserId = i;
            this.mLockVirgin = !this.mUtils.isPatternEverChosen(this.mUserId);
            start();
        }

        @Override
        protected Intent saveAndVerifyInBackground() {
            byte[] bArrVerifyPattern;
            int i = this.mUserId;
            this.mUtils.saveLockPattern(this.mChosenPattern, this.mCurrentPattern, i);
            if (!this.mHasChallenge) {
                return null;
            }
            try {
                bArrVerifyPattern = this.mUtils.verifyPattern(this.mChosenPattern, this.mChallenge, i);
            } catch (LockPatternUtils.RequestThrottledException e) {
                bArrVerifyPattern = null;
            }
            if (bArrVerifyPattern == null) {
                Log.e("ChooseLockPattern", "critical: no token returned for known good pattern");
            }
            Intent intent = new Intent();
            intent.putExtra("hw_auth_token", bArrVerifyPattern);
            return intent;
        }

        @Override
        protected void finish(Intent intent) {
            if (this.mLockVirgin) {
                this.mUtils.setVisiblePatternEnabled(true, this.mUserId);
            }
            super.finish(intent);
        }
    }
}
