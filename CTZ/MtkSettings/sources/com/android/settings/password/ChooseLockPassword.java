package com.android.settings.password;

import android.app.Activity;
import android.app.Fragment;
import android.app.admin.PasswordMetrics;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.notification.RedactionInterstitial;
import com.android.settings.password.SaveChosenLockWorkerBase;
import com.android.settings.widget.ImeAwareEditText;
import com.android.setupwizardlib.GlifLayout;
import java.util.ArrayList;
import java.util.function.ToIntFunction;

public class ChooseLockPassword extends SettingsActivity {
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
            this.mIntent = new Intent(context, (Class<?>) ChooseLockPassword.class);
            this.mIntent.putExtra("confirm_credentials", false);
            this.mIntent.putExtra("extra_require_password", false);
            this.mIntent.putExtra("has_challenge", false);
        }

        public IntentBuilder setPasswordQuality(int i) {
            this.mIntent.putExtra("lockscreen.password_type", i);
            return this;
        }

        public IntentBuilder setPasswordLengthRange(int i, int i2) {
            this.mIntent.putExtra("lockscreen.password_min", i);
            this.mIntent.putExtra("lockscreen.password_max", i2);
            return this;
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

        public IntentBuilder setPassword(String str) {
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
        return ChooseLockPasswordFragment.class.getName().equals(str);
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        int i;
        super.onCreate(bundle);
        if (getIntent().getBooleanExtra("for_fingerprint", false)) {
            i = R.string.lockpassword_choose_your_password_header_for_fingerprint;
        } else {
            i = R.string.lockpassword_choose_your_screen_lock_header;
        }
        setTitle(getText(i));
        ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
    }

    public static class ChooseLockPasswordFragment extends InstrumentedFragment implements TextWatcher, View.OnClickListener, TextView.OnEditorActionListener, SaveChosenLockWorkerBase.Listener {
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private String mChosenPassword;
        private Button mClearButton;
        private String mCurrentPassword;
        private String mFirstPin;
        protected boolean mForFingerprint;
        private boolean mHasChallenge;
        protected boolean mIsAlphaMode;
        private GlifLayout mLayout;
        private LockPatternUtils mLockPatternUtils;
        private TextView mMessage;
        private Button mNextButton;
        private ImeAwareEditText mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private byte[] mPasswordHistoryHashFactor;
        private PasswordRequirementAdapter mPasswordRequirementAdapter;
        private int[] mPasswordRequirements;
        private RecyclerView mPasswordRestrictionView;
        private SaveAndFinishWorker mSaveAndFinishWorker;
        protected Button mSkipButton;
        private TextChangedHandler mTextChangedHandler;
        protected int mUserId;
        private int mPasswordMinLength = 4;
        private int mPasswordMaxLength = 16;
        private int mPasswordMinLetters = 0;
        private int mPasswordMinUpperCase = 0;
        private int mPasswordMinLowerCase = 0;
        private int mPasswordMinSymbols = 0;
        private int mPasswordMinNumeric = 0;
        private int mPasswordMinNonLetter = 0;
        private int mPasswordMinLengthToFulfillAllPolicies = 0;
        private boolean mHideDrawer = false;
        private int mRequestedQuality = 131072;
        protected Stage mUiStage = Stage.Introduction;

        protected enum Stage {
            Introduction(R.string.lockpassword_choose_your_screen_lock_header, R.string.lockpassword_choose_your_password_header_for_fingerprint, R.string.lockpassword_choose_your_screen_lock_header, R.string.lockpassword_choose_your_pin_header_for_fingerprint, R.string.lockpassword_choose_your_password_message, R.string.lock_settings_picker_fingerprint_added_security_message, R.string.lockpassword_choose_your_pin_message, R.string.lock_settings_picker_fingerprint_added_security_message, R.string.next_label),
            NeedToConfirm(R.string.lockpassword_confirm_your_password_header, R.string.lockpassword_confirm_your_password_header, R.string.lockpassword_confirm_your_pin_header, R.string.lockpassword_confirm_your_pin_header, 0, 0, 0, 0, R.string.lockpassword_confirm_label),
            ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match, R.string.lockpassword_confirm_passwords_dont_match, R.string.lockpassword_confirm_pins_dont_match, R.string.lockpassword_confirm_pins_dont_match, 0, 0, 0, 0, R.string.lockpassword_confirm_label);

            public final int alphaHint;
            public final int alphaHintForFingerprint;
            public final int alphaMessage;
            public final int alphaMessageForFingerprint;
            public final int buttonText;
            public final int numericHint;
            public final int numericHintForFingerprint;
            public final int numericMessage;
            public final int numericMessageForFingerprint;

            Stage(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
                this.alphaHint = i;
                this.alphaHintForFingerprint = i2;
                this.numericHint = i3;
                this.numericHintForFingerprint = i4;
                this.alphaMessage = i5;
                this.alphaMessageForFingerprint = i6;
                this.numericMessage = i7;
                this.numericMessageForFingerprint = i8;
                this.buttonText = i9;
            }

            public int getHint(boolean z, boolean z2) {
                return z ? z2 ? this.alphaHintForFingerprint : this.alphaHint : z2 ? this.numericHintForFingerprint : this.numericHint;
            }

            public int getMessage(boolean z, boolean z2) {
                return z ? z2 ? this.alphaMessageForFingerprint : this.alphaMessage : z2 ? this.numericMessageForFingerprint : this.numericMessage;
            }
        }

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            Intent intent = getActivity().getIntent();
            if (!(getActivity() instanceof ChooseLockPassword)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            this.mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
            this.mForFingerprint = intent.getBooleanExtra("for_fingerprint", false);
            processPasswordRequirements(intent);
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            this.mHideDrawer = getActivity().getIntent().getBooleanExtra(":settings:hide_drawer", false);
            if (intent.getBooleanExtra("for_cred_req_boot", false)) {
                SaveAndFinishWorker saveAndFinishWorker = new SaveAndFinishWorker();
                boolean booleanExtra = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
                String stringExtra = intent.getStringExtra("password");
                saveAndFinishWorker.setBlocking(true);
                saveAndFinishWorker.setListener(this);
                saveAndFinishWorker.start(this.mChooseLockSettingsHelper.utils(), booleanExtra, false, 0L, stringExtra, stringExtra, this.mRequestedQuality, this.mUserId);
            }
            this.mTextChangedHandler = new TextChangedHandler();
        }

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
            return layoutInflater.inflate(R.layout.choose_lock_password, viewGroup, false);
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            this.mLayout = (GlifLayout) view;
            ((ViewGroup) view.findViewById(R.id.password_container)).setOpticalInsets(Insets.NONE);
            this.mSkipButton = (Button) view.findViewById(R.id.skip_button);
            this.mSkipButton.setOnClickListener(this);
            this.mNextButton = (Button) view.findViewById(R.id.next_button);
            this.mNextButton.setOnClickListener(this);
            this.mClearButton = (Button) view.findViewById(R.id.clear_button);
            this.mClearButton.setOnClickListener(this);
            this.mMessage = (TextView) view.findViewById(R.id.message);
            if (this.mForFingerprint) {
                this.mLayout.setIcon(getActivity().getDrawable(R.drawable.ic_fingerprint_header));
            }
            this.mIsAlphaMode = 262144 == this.mRequestedQuality || 327680 == this.mRequestedQuality || 393216 == this.mRequestedQuality;
            setupPasswordRequirementsView(view);
            this.mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            this.mPasswordEntry = (ImeAwareEditText) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
            this.mPasswordEntry.requestFocus();
            this.mPasswordEntryInputDisabler = new TextViewInputDisabler(this.mPasswordEntry);
            Activity activity = getActivity();
            int inputType = this.mPasswordEntry.getInputType();
            ImeAwareEditText imeAwareEditText = this.mPasswordEntry;
            if (!this.mIsAlphaMode) {
                inputType = 18;
            }
            imeAwareEditText.setInputType(inputType);
            this.mPasswordEntry.setTypeface(Typeface.create(getContext().getString(android.R.string.aerr_process_repeated), 0));
            Intent intent = getActivity().getIntent();
            boolean booleanExtra = intent.getBooleanExtra("confirm_credentials", true);
            this.mCurrentPassword = intent.getStringExtra("password");
            this.mHasChallenge = intent.getBooleanExtra("has_challenge", false);
            this.mChallenge = intent.getLongExtra("challenge", 0L);
            if (bundle == null) {
                updateStage(Stage.Introduction);
                if (booleanExtra) {
                    this.mChooseLockSettingsHelper.launchConfirmationActivity(58, getString(R.string.unlock_set_unlock_launch_picker_title), true, this.mUserId);
                }
            } else {
                this.mFirstPin = bundle.getString("first_pin");
                String string = bundle.getString("ui_stage");
                if (string != null) {
                    this.mUiStage = Stage.valueOf(string);
                    updateStage(this.mUiStage);
                }
                if (this.mCurrentPassword == null) {
                    this.mCurrentPassword = bundle.getString("current_password");
                }
                this.mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag("save_and_finish_worker");
            }
            if (activity instanceof SettingsActivity) {
                int hint = Stage.Introduction.getHint(this.mIsAlphaMode, this.mForFingerprint);
                ((SettingsActivity) activity).setTitle(hint);
                this.mLayout.setHeaderText(hint);
            }
        }

        private void setupPasswordRequirementsView(View view) {
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            if (this.mPasswordMinUpperCase > 0) {
                arrayList.add(1);
                arrayList2.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_uppercase, this.mPasswordMinUpperCase, Integer.valueOf(this.mPasswordMinUpperCase)));
            }
            if (this.mPasswordMinLowerCase > 0) {
                arrayList.add(2);
                arrayList2.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_lowercase, this.mPasswordMinLowerCase, Integer.valueOf(this.mPasswordMinLowerCase)));
            }
            if (this.mPasswordMinLetters > 0 && this.mPasswordMinLetters > this.mPasswordMinUpperCase + this.mPasswordMinLowerCase) {
                arrayList.add(0);
                arrayList2.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_letters, this.mPasswordMinLetters, Integer.valueOf(this.mPasswordMinLetters)));
            }
            if (this.mPasswordMinNumeric > 0) {
                arrayList.add(4);
                arrayList2.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_numeric, this.mPasswordMinNumeric, Integer.valueOf(this.mPasswordMinNumeric)));
            }
            if (this.mPasswordMinSymbols > 0) {
                arrayList.add(3);
                arrayList2.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_symbols, this.mPasswordMinSymbols, Integer.valueOf(this.mPasswordMinSymbols)));
            }
            if (this.mPasswordMinNonLetter > 0 && this.mPasswordMinNonLetter > this.mPasswordMinNumeric + this.mPasswordMinSymbols) {
                arrayList.add(5);
                arrayList2.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_nonletter, this.mPasswordMinNonLetter, Integer.valueOf(this.mPasswordMinNonLetter)));
            }
            this.mPasswordRequirements = arrayList.stream().mapToInt(new ToIntFunction() {
                @Override
                public final int applyAsInt(Object obj) {
                    return ((Integer) obj).intValue();
                }
            }).toArray();
            this.mPasswordRestrictionView = (RecyclerView) view.findViewById(R.id.password_requirements_view);
            this.mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            this.mPasswordRequirementAdapter = new PasswordRequirementAdapter();
            this.mPasswordRestrictionView.setAdapter(this.mPasswordRequirementAdapter);
        }

        @Override
        public int getMetricsCategory() {
            return 28;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(this.mUiStage);
            if (this.mSaveAndFinishWorker != null) {
                this.mSaveAndFinishWorker.setListener(this);
            } else {
                this.mPasswordEntry.requestFocus();
                this.mPasswordEntry.scheduleShowSoftInput();
            }
        }

        @Override
        public void onPause() {
            if (this.mSaveAndFinishWorker != null) {
                this.mSaveAndFinishWorker.setListener(null);
            }
            super.onPause();
        }

        @Override
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putString("ui_stage", this.mUiStage.name());
            bundle.putString("first_pin", this.mFirstPin);
            bundle.putString("current_password", this.mCurrentPassword);
        }

        @Override
        public void onActivityResult(int i, int i2, Intent intent) {
            super.onActivityResult(i, i2, intent);
            if (i == 58) {
                if (i2 != -1) {
                    getActivity().setResult(1);
                    getActivity().finish();
                } else {
                    this.mCurrentPassword = intent.getStringExtra("password");
                }
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context, this.mUserId);
        }

        protected void updateStage(Stage stage) {
            Stage stage2 = this.mUiStage;
            this.mUiStage = stage;
            updateUi();
            if (stage2 != stage) {
                this.mLayout.announceForAccessibility(this.mLayout.getHeaderText());
            }
        }

        private void processPasswordRequirements(Intent intent) {
            int requestedPasswordQuality = this.mLockPatternUtils.getRequestedPasswordQuality(this.mUserId);
            this.mRequestedQuality = Math.max(intent.getIntExtra("lockscreen.password_type", this.mRequestedQuality), requestedPasswordQuality);
            this.mPasswordMinLength = Math.max(Math.max(4, intent.getIntExtra("lockscreen.password_min", this.mPasswordMinLength)), this.mLockPatternUtils.getRequestedMinimumPasswordLength(this.mUserId));
            this.mPasswordMaxLength = intent.getIntExtra("lockscreen.password_max", this.mPasswordMaxLength);
            this.mPasswordMinLetters = Math.max(intent.getIntExtra("lockscreen.password_min_letters", this.mPasswordMinLetters), this.mLockPatternUtils.getRequestedPasswordMinimumLetters(this.mUserId));
            this.mPasswordMinUpperCase = Math.max(intent.getIntExtra("lockscreen.password_min_uppercase", this.mPasswordMinUpperCase), this.mLockPatternUtils.getRequestedPasswordMinimumUpperCase(this.mUserId));
            this.mPasswordMinLowerCase = Math.max(intent.getIntExtra("lockscreen.password_min_lowercase", this.mPasswordMinLowerCase), this.mLockPatternUtils.getRequestedPasswordMinimumLowerCase(this.mUserId));
            this.mPasswordMinNumeric = Math.max(intent.getIntExtra("lockscreen.password_min_numeric", this.mPasswordMinNumeric), this.mLockPatternUtils.getRequestedPasswordMinimumNumeric(this.mUserId));
            this.mPasswordMinSymbols = Math.max(intent.getIntExtra("lockscreen.password_min_symbols", this.mPasswordMinSymbols), this.mLockPatternUtils.getRequestedPasswordMinimumSymbols(this.mUserId));
            this.mPasswordMinNonLetter = Math.max(intent.getIntExtra("lockscreen.password_min_nonletter", this.mPasswordMinNonLetter), this.mLockPatternUtils.getRequestedPasswordMinimumNonLetter(this.mUserId));
            if (requestedPasswordQuality != 262144) {
                if (requestedPasswordQuality == 327680) {
                    if (this.mPasswordMinLetters == 0) {
                        this.mPasswordMinLetters = 1;
                    }
                    if (this.mPasswordMinNumeric == 0) {
                        this.mPasswordMinNumeric = 1;
                    }
                } else if (requestedPasswordQuality != 393216) {
                    this.mPasswordMinNumeric = 0;
                    this.mPasswordMinLetters = 0;
                    this.mPasswordMinUpperCase = 0;
                    this.mPasswordMinLowerCase = 0;
                    this.mPasswordMinSymbols = 0;
                    this.mPasswordMinNonLetter = 0;
                }
            } else if (this.mPasswordMinLetters == 0) {
                this.mPasswordMinLetters = 1;
            }
            this.mPasswordMinLengthToFulfillAllPolicies = getMinLengthToFulfillAllPolicies();
        }

        private int validatePassword(String str) {
            int i;
            int i2;
            PasswordMetrics passwordMetricsComputeForPassword = PasswordMetrics.computeForPassword(str);
            if (str.length() < this.mPasswordMinLength) {
                if (this.mPasswordMinLength > this.mPasswordMinLengthToFulfillAllPolicies) {
                    i = 2;
                } else {
                    i = 0;
                }
            } else if (str.length() > this.mPasswordMaxLength) {
                i = 4;
            } else {
                if (this.mLockPatternUtils.getRequestedPasswordQuality(this.mUserId) == 196608 && passwordMetricsComputeForPassword.numeric == str.length() && PasswordMetrics.maxLengthSequence(str) > 3) {
                    i = 16;
                } else {
                    i = 0;
                }
                if (this.mLockPatternUtils.checkPasswordHistory(str, getPasswordHistoryHashFactor(), this.mUserId)) {
                    i |= 32;
                }
            }
            for (int i3 = 0; i3 < str.length(); i3++) {
                char cCharAt = str.charAt(i3);
                if (cCharAt < ' ' || cCharAt > 127) {
                    i |= 1;
                    break;
                }
            }
            if ((this.mRequestedQuality == 131072 || this.mRequestedQuality == 196608) && (passwordMetricsComputeForPassword.letters > 0 || passwordMetricsComputeForPassword.symbols > 0)) {
                i |= 8;
            }
            for (int i4 = 0; i4 < this.mPasswordRequirements.length; i4++) {
                switch (this.mPasswordRequirements[i4]) {
                    case 0:
                        if (passwordMetricsComputeForPassword.letters < this.mPasswordMinLetters) {
                            i2 = i | 64;
                            i = i2;
                        }
                        break;
                    case 1:
                        if (passwordMetricsComputeForPassword.upperCase < this.mPasswordMinUpperCase) {
                            i2 = i | 128;
                            i = i2;
                        }
                        break;
                    case 2:
                        if (passwordMetricsComputeForPassword.lowerCase < this.mPasswordMinLowerCase) {
                            i2 = i | 256;
                            i = i2;
                        }
                        break;
                    case 3:
                        if (passwordMetricsComputeForPassword.symbols < this.mPasswordMinSymbols) {
                            i2 = i | 1024;
                            i = i2;
                        }
                        break;
                    case 4:
                        if (passwordMetricsComputeForPassword.numeric < this.mPasswordMinNumeric) {
                            i2 = i | 512;
                            i = i2;
                        }
                        break;
                    case 5:
                        if (passwordMetricsComputeForPassword.nonLetter < this.mPasswordMinNonLetter) {
                            i2 = i | 2048;
                            i = i2;
                        }
                        break;
                }
            }
            return i;
        }

        private byte[] getPasswordHistoryHashFactor() {
            if (this.mPasswordHistoryHashFactor == null) {
                this.mPasswordHistoryHashFactor = this.mLockPatternUtils.getPasswordHistoryHashFactor(this.mCurrentPassword, this.mUserId);
            }
            return this.mPasswordHistoryHashFactor;
        }

        public void handleNext() {
            if (this.mSaveAndFinishWorker != null) {
                return;
            }
            this.mChosenPassword = this.mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(this.mChosenPassword)) {
                return;
            }
            if (this.mUiStage == Stage.Introduction) {
                if (validatePassword(this.mChosenPassword) == 0) {
                    this.mFirstPin = this.mChosenPassword;
                    this.mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                    return;
                }
                return;
            }
            if (this.mUiStage == Stage.NeedToConfirm) {
                if (this.mFirstPin.equals(this.mChosenPassword)) {
                    startSaveAndFinish();
                    return;
                }
                Editable text = this.mPasswordEntry.getText();
                if (text != null) {
                    Selection.setSelection(text, 0, text.length());
                }
                updateStage(Stage.ConfirmWrong);
            }
        }

        protected void setNextEnabled(boolean z) {
            this.mNextButton.setEnabled(z);
        }

        protected void setNextText(int i) {
            this.mNextButton.setText(i);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.clear_button) {
                this.mPasswordEntry.setText("");
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

        String[] convertErrorCodeToMessages(int i) {
            int i2;
            int i3;
            ArrayList arrayList = new ArrayList();
            if ((i & 1) > 0) {
                arrayList.add(getString(R.string.lockpassword_illegal_character));
            }
            if ((i & 8) > 0) {
                arrayList.add(getString(R.string.lockpassword_pin_contains_non_digits));
            }
            if ((i & 128) > 0) {
                arrayList.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_uppercase, this.mPasswordMinUpperCase, Integer.valueOf(this.mPasswordMinUpperCase)));
            }
            if ((i & 256) > 0) {
                arrayList.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_lowercase, this.mPasswordMinLowerCase, Integer.valueOf(this.mPasswordMinLowerCase)));
            }
            if ((i & 64) > 0) {
                arrayList.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_letters, this.mPasswordMinLetters, Integer.valueOf(this.mPasswordMinLetters)));
            }
            if ((i & 512) > 0) {
                arrayList.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_numeric, this.mPasswordMinNumeric, Integer.valueOf(this.mPasswordMinNumeric)));
            }
            if ((i & 1024) > 0) {
                arrayList.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_symbols, this.mPasswordMinSymbols, Integer.valueOf(this.mPasswordMinSymbols)));
            }
            if ((i & 2048) > 0) {
                arrayList.add(getResources().getQuantityString(R.plurals.lockpassword_password_requires_nonletter, this.mPasswordMinNonLetter, Integer.valueOf(this.mPasswordMinNonLetter)));
            }
            if ((i & 2) > 0) {
                if (this.mIsAlphaMode) {
                    i3 = R.string.lockpassword_password_too_short;
                } else {
                    i3 = R.string.lockpassword_pin_too_short;
                }
                arrayList.add(getString(i3, new Object[]{Integer.valueOf(this.mPasswordMinLength)}));
            }
            if ((i & 4) > 0) {
                if (this.mIsAlphaMode) {
                    i2 = R.string.lockpassword_password_too_long;
                } else {
                    i2 = R.string.lockpassword_pin_too_long;
                }
                arrayList.add(getString(i2, new Object[]{Integer.valueOf(this.mPasswordMaxLength + 1)}));
            }
            if ((i & 16) > 0) {
                arrayList.add(getString(R.string.lockpassword_pin_no_sequential_digits));
            }
            if ((i & 32) > 0) {
                arrayList.add(getString(this.mIsAlphaMode ? R.string.lockpassword_password_recently_used : R.string.lockpassword_pin_recently_used));
            }
            return (String[]) arrayList.toArray(new String[0]);
        }

        private int getMinLengthToFulfillAllPolicies() {
            return Math.max(this.mPasswordMinLetters, this.mPasswordMinUpperCase + this.mPasswordMinLowerCase) + Math.max(this.mPasswordMinNonLetter, this.mPasswordMinSymbols + this.mPasswordMinNumeric);
        }

        protected void updateUi() {
            boolean z = this.mSaveAndFinishWorker == null;
            String string = this.mPasswordEntry.getText().toString();
            int length = string.length();
            if (this.mUiStage == Stage.Introduction) {
                this.mPasswordRestrictionView.setVisibility(0);
                int iValidatePassword = validatePassword(string);
                this.mPasswordRequirementAdapter.setRequirements(convertErrorCodeToMessages(iValidatePassword));
                setNextEnabled(iValidatePassword == 0);
            } else {
                this.mPasswordRestrictionView.setVisibility(8);
                setHeaderText(getString(this.mUiStage.getHint(this.mIsAlphaMode, this.mForFingerprint)));
                setNextEnabled(z && length >= this.mPasswordMinLength);
                this.mClearButton.setEnabled(z && length > 0);
            }
            int message = this.mUiStage.getMessage(this.mIsAlphaMode, this.mForFingerprint);
            if (message != 0) {
                this.mMessage.setVisibility(0);
                this.mMessage.setText(message);
            } else {
                this.mMessage.setVisibility(4);
            }
            this.mClearButton.setVisibility(toVisibility(this.mUiStage != Stage.Introduction));
            setNextText(this.mUiStage.buttonText);
            this.mPasswordEntryInputDisabler.setInputEnabled(z);
        }

        private int toVisibility(boolean z) {
            return z ? 0 : 8;
        }

        private void setHeaderText(String str) {
            if (!TextUtils.isEmpty(this.mLayout.getHeaderText()) && this.mLayout.getHeaderText().toString().equals(str)) {
                return;
            }
            this.mLayout.setHeaderText(str);
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (this.mUiStage == Stage.ConfirmWrong) {
                this.mUiStage = Stage.NeedToConfirm;
            }
            this.mTextChangedHandler.notifyAfterTextChanged();
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        private void startSaveAndFinish() {
            if (this.mSaveAndFinishWorker != null) {
                Log.w("ChooseLockPassword", "startSaveAndFinish with an existing SaveAndFinishWorker.");
                return;
            }
            this.mPasswordEntryInputDisabler.setInputEnabled(false);
            setNextEnabled(false);
            this.mSaveAndFinishWorker = new SaveAndFinishWorker();
            this.mSaveAndFinishWorker.setListener(this);
            getFragmentManager().beginTransaction().add(this.mSaveAndFinishWorker, "save_and_finish_worker").commit();
            getFragmentManager().executePendingTransactions();
            this.mSaveAndFinishWorker.start(this.mLockPatternUtils, getActivity().getIntent().getBooleanExtra("extra_require_password", true), this.mHasChallenge, this.mChallenge, this.mChosenPassword, this.mCurrentPassword, this.mRequestedQuality, this.mUserId);
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

        class TextChangedHandler extends Handler {
            TextChangedHandler() {
            }

            public void notifyAfterTextChanged() {
                removeMessages(1);
                sendEmptyMessageDelayed(1, 100L);
            }

            @Override
            public void handleMessage(Message message) {
                if (ChooseLockPasswordFragment.this.getActivity() != null && message.what == 1) {
                    ChooseLockPasswordFragment.this.updateUi();
                }
            }
        }
    }

    public static class SaveAndFinishWorker extends SaveChosenLockWorkerBase {
        private String mChosenPassword;
        private String mCurrentPassword;
        private int mRequestedQuality;

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

        public void start(LockPatternUtils lockPatternUtils, boolean z, boolean z2, long j, String str, String str2, int i, int i2) {
            prepare(lockPatternUtils, z, z2, j, i2);
            this.mChosenPassword = str;
            this.mCurrentPassword = str2;
            this.mRequestedQuality = i;
            this.mUserId = i2;
            start();
        }

        @Override
        protected Intent saveAndVerifyInBackground() {
            byte[] bArrVerifyPassword;
            this.mUtils.saveLockPassword(this.mChosenPassword, this.mCurrentPassword, this.mRequestedQuality, this.mUserId);
            if (!this.mHasChallenge) {
                return null;
            }
            try {
                bArrVerifyPassword = this.mUtils.verifyPassword(this.mChosenPassword, this.mChallenge, this.mUserId);
            } catch (LockPatternUtils.RequestThrottledException e) {
                bArrVerifyPassword = null;
            }
            if (bArrVerifyPassword == null) {
                Log.e("ChooseLockPassword", "critical: no token returned for known good password.");
            }
            Intent intent = new Intent();
            intent.putExtra("hw_auth_token", bArrVerifyPassword);
            return intent;
        }
    }
}
