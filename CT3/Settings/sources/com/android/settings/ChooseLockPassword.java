package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.SaveChosenLockWorkerBase;
import com.android.settings.notification.RedactionInterstitial;

public class ChooseLockPassword extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, boolean confirmCredentials) {
        Intent intent = new Intent().setClass(context, ChooseLockPassword.class);
        intent.putExtra("lockscreen.password_type", quality);
        intent.putExtra("lockscreen.password_min", minLength);
        intent.putExtra("lockscreen.password_max", maxLength);
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("extra_require_password", requirePasswordToDecrypt);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password) {
        Intent intent = createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, false);
        intent.putExtra("password", password);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
        Intent intent = createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, password);
        intent.putExtra("android.intent.extra.USER_ID", userId);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge) {
        Intent intent = createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, false);
        intent.putExtra("has_challenge", true);
        intent.putExtra("challenge", challenge);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength, int maxLength, boolean requirePasswordToDecrypt, long challenge, int userId) {
        Intent intent = createIntent(context, quality, minLength, maxLength, requirePasswordToDecrypt, challenge);
        intent.putExtra("android.intent.extra.USER_ID", userId);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    Class<? extends Fragment> getFragmentClass() {
        return ChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_password_header);
        setTitle(msg);
    }

    public static class ChooseLockPasswordFragment extends InstrumentedFragment implements View.OnClickListener, TextView.OnEditorActionListener, TextWatcher, SaveChosenLockWorkerBase.Listener {
        private Button mCancelButton;
        private long mChallenge;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private String mChosenPassword;
        private String mCurrentPassword;
        private String mFirstPin;
        private boolean mHasChallenge;
        private TextView mHeaderText;
        private boolean mIsAlphaMode;
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private KeyboardView mKeyboardView;
        private LockPatternUtils mLockPatternUtils;
        private Button mNextButton;
        private TextView mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private SaveAndFinishWorker mSaveAndFinishWorker;
        private int mUserId;
        private int mPasswordMinLength = 4;
        private int mPasswordMaxLength = 16;
        private int mPasswordMinLetters = 0;
        private int mPasswordMinUpperCase = 0;
        private int mPasswordMinLowerCase = 0;
        private int mPasswordMinSymbols = 0;
        private int mPasswordMinNumeric = 0;
        private int mPasswordMinNonLetter = 0;
        private int mRequestedQuality = 131072;
        private Stage mUiStage = Stage.Introduction;
        private boolean mHideDrawer = false;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    return;
                }
                ChooseLockPasswordFragment.this.updateStage((Stage) msg.obj);
            }
        };

        protected enum Stage {
            Introduction(R.string.lockpassword_choose_your_password_header, R.string.lockpassword_choose_your_pin_header, R.string.lockpassword_continue_label),
            NeedToConfirm(R.string.lockpassword_confirm_your_password_header, R.string.lockpassword_confirm_your_pin_header, R.string.lockpassword_ok_label),
            ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match, R.string.lockpassword_confirm_pins_dont_match, R.string.lockpassword_continue_label);

            public final int alphaHint;
            public final int buttonText;
            public final int numericHint;

            public static Stage[] valuesCustom() {
                return values();
            }

            Stage(int hintInAlpha, int hintInNumeric, int nextButtonText) {
                this.alphaHint = hintInAlpha;
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mLockPatternUtils = new LockPatternUtils(getActivity());
            Intent intent = getActivity().getIntent();
            if (!(getActivity() instanceof ChooseLockPassword)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            this.mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
            this.mRequestedQuality = Math.max(intent.getIntExtra("lockscreen.password_type", this.mRequestedQuality), this.mLockPatternUtils.getRequestedPasswordQuality(this.mUserId));
            this.mPasswordMinLength = Math.max(Math.max(4, intent.getIntExtra("lockscreen.password_min", this.mPasswordMinLength)), this.mLockPatternUtils.getRequestedMinimumPasswordLength(this.mUserId));
            this.mPasswordMaxLength = intent.getIntExtra("lockscreen.password_max", this.mPasswordMaxLength);
            this.mPasswordMinLetters = Math.max(intent.getIntExtra("lockscreen.password_min_letters", this.mPasswordMinLetters), this.mLockPatternUtils.getRequestedPasswordMinimumLetters(this.mUserId));
            this.mPasswordMinUpperCase = Math.max(intent.getIntExtra("lockscreen.password_min_uppercase", this.mPasswordMinUpperCase), this.mLockPatternUtils.getRequestedPasswordMinimumUpperCase(this.mUserId));
            this.mPasswordMinLowerCase = Math.max(intent.getIntExtra("lockscreen.password_min_lowercase", this.mPasswordMinLowerCase), this.mLockPatternUtils.getRequestedPasswordMinimumLowerCase(this.mUserId));
            this.mPasswordMinNumeric = Math.max(intent.getIntExtra("lockscreen.password_min_numeric", this.mPasswordMinNumeric), this.mLockPatternUtils.getRequestedPasswordMinimumNumeric(this.mUserId));
            this.mPasswordMinSymbols = Math.max(intent.getIntExtra("lockscreen.password_min_symbols", this.mPasswordMinSymbols), this.mLockPatternUtils.getRequestedPasswordMinimumSymbols(this.mUserId));
            this.mPasswordMinNonLetter = Math.max(intent.getIntExtra("lockscreen.password_min_nonletter", this.mPasswordMinNonLetter), this.mLockPatternUtils.getRequestedPasswordMinimumNonLetter(this.mUserId));
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            this.mHideDrawer = getActivity().getIntent().getBooleanExtra(":settings:hide_drawer", false);
            if (!intent.getBooleanExtra("for_cred_req_boot", false)) {
                return;
            }
            SaveAndFinishWorker w = new SaveAndFinishWorker(null);
            boolean required = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
            String current = intent.getStringExtra("password");
            w.setBlocking(true);
            w.setListener(this);
            w.start(this.mChooseLockSettingsHelper.utils(), required, false, 0L, current, current, this.mRequestedQuality, this.mUserId);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_lock_password, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            this.mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            this.mCancelButton.setOnClickListener(this);
            this.mNextButton = (Button) view.findViewById(R.id.next_button);
            this.mNextButton.setOnClickListener(this);
            boolean z = 262144 == this.mRequestedQuality || 327680 == this.mRequestedQuality || 393216 == this.mRequestedQuality;
            this.mIsAlphaMode = z;
            this.mKeyboardView = view.findViewById(R.id.keyboard);
            this.mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
            this.mPasswordEntryInputDisabler = new TextViewInputDisabler(this.mPasswordEntry);
            Activity activity = getActivity();
            this.mKeyboardHelper = new PasswordEntryKeyboardHelper(activity, this.mKeyboardView, this.mPasswordEntry);
            this.mKeyboardHelper.setKeyboardMode(this.mIsAlphaMode ? 0 : 1);
            this.mHeaderText = (TextView) view.findViewById(R.id.headerText);
            this.mKeyboardView.requestFocus();
            int currentType = this.mPasswordEntry.getInputType();
            TextView textView = this.mPasswordEntry;
            if (!this.mIsAlphaMode) {
                currentType = 18;
            }
            textView.setInputType(currentType);
            Intent intent = getActivity().getIntent();
            boolean confirmCredentials = intent.getBooleanExtra("confirm_credentials", true);
            this.mCurrentPassword = intent.getStringExtra("password");
            this.mHasChallenge = intent.getBooleanExtra("has_challenge", false);
            this.mChallenge = intent.getLongExtra("challenge", 0L);
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    this.mChooseLockSettingsHelper.launchConfirmationActivity(58, getString(R.string.unlock_set_unlock_launch_picker_title), true, this.mUserId);
                }
            } else {
                this.mFirstPin = savedInstanceState.getString("first_pin");
                String state = savedInstanceState.getString("ui_stage");
                if (state != null) {
                    this.mUiStage = Stage.valueOf(state);
                    updateStage(this.mUiStage);
                }
                if (this.mCurrentPassword == null) {
                    this.mCurrentPassword = savedInstanceState.getString("current_password");
                }
                this.mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag("save_and_finish_worker");
            }
            if (!(activity instanceof SettingsActivity)) {
                return;
            }
            SettingsActivity sa = (SettingsActivity) activity;
            int id = this.mIsAlphaMode ? R.string.lockpassword_choose_your_password_header : R.string.lockpassword_choose_your_pin_header;
            CharSequence title = getText(id);
            sa.setTitle(title);
        }

        @Override
        protected int getMetricsCategory() {
            return 28;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(this.mUiStage);
            if (this.mSaveAndFinishWorker != null) {
                this.mSaveAndFinishWorker.setListener(this);
            } else {
                this.mKeyboardView.requestFocus();
            }
        }

        @Override
        public void onPause() {
            this.mHandler.removeMessages(1);
            if (this.mSaveAndFinishWorker != null) {
                this.mSaveAndFinishWorker.setListener(null);
            }
            super.onPause();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("ui_stage", this.mUiStage.name());
            outState.putString("first_pin", this.mFirstPin);
            outState.putString("current_password", this.mCurrentPassword);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case 58:
                    if (resultCode != -1) {
                        getActivity().setResult(1);
                        getActivity().finish();
                    } else {
                        this.mCurrentPassword = data.getStringExtra("password");
                    }
                    break;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context, this.mUserId);
        }

        protected void updateStage(Stage stage) {
            Stage previousStage = this.mUiStage;
            this.mUiStage = stage;
            updateUi();
            if (previousStage == stage) {
                return;
            }
            this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
        }

        private String validatePassword(String password) {
            int i;
            int i2;
            if (password.length() < this.mPasswordMinLength) {
                if (this.mIsAlphaMode) {
                    i2 = R.string.lockpassword_password_too_short;
                } else {
                    i2 = R.string.lockpassword_pin_too_short;
                }
                return getString(i2, new Object[]{Integer.valueOf(this.mPasswordMinLength)});
            }
            if (password.length() > this.mPasswordMaxLength) {
                if (this.mIsAlphaMode) {
                    i = R.string.lockpassword_password_too_long;
                } else {
                    i = R.string.lockpassword_pin_too_long;
                }
                return getString(i, new Object[]{Integer.valueOf(this.mPasswordMaxLength + 1)});
            }
            int letters = 0;
            int numbers = 0;
            int lowercase = 0;
            int symbols = 0;
            int uppercase = 0;
            int nonletter = 0;
            for (int i3 = 0; i3 < password.length(); i3++) {
                char c = password.charAt(i3);
                if (c < ' ' || c > 127) {
                    return getString(R.string.lockpassword_illegal_character);
                }
                if (c >= '0' && c <= '9') {
                    numbers++;
                    nonletter++;
                } else if (c >= 'A' && c <= 'Z') {
                    letters++;
                    uppercase++;
                } else if (c >= 'a' && c <= 'z') {
                    letters++;
                    lowercase++;
                } else {
                    symbols++;
                    nonletter++;
                }
            }
            if (131072 == this.mRequestedQuality || 196608 == this.mRequestedQuality) {
                if (letters > 0 || symbols > 0) {
                    return getString(R.string.lockpassword_pin_contains_non_digits);
                }
                int sequence = LockPatternUtils.maxLengthSequence(password);
                if (196608 == this.mRequestedQuality && sequence > 3) {
                    return getString(R.string.lockpassword_pin_no_sequential_digits);
                }
            } else if (393216 == this.mRequestedQuality) {
                if (letters < this.mPasswordMinLetters) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_letters, this.mPasswordMinLetters), Integer.valueOf(this.mPasswordMinLetters));
                }
                if (numbers < this.mPasswordMinNumeric) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_numeric, this.mPasswordMinNumeric), Integer.valueOf(this.mPasswordMinNumeric));
                }
                if (lowercase < this.mPasswordMinLowerCase) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_lowercase, this.mPasswordMinLowerCase), Integer.valueOf(this.mPasswordMinLowerCase));
                }
                if (uppercase < this.mPasswordMinUpperCase) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_uppercase, this.mPasswordMinUpperCase), Integer.valueOf(this.mPasswordMinUpperCase));
                }
                if (symbols < this.mPasswordMinSymbols) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_symbols, this.mPasswordMinSymbols), Integer.valueOf(this.mPasswordMinSymbols));
                }
                if (nonletter < this.mPasswordMinNonLetter) {
                    return String.format(getResources().getQuantityString(R.plurals.lockpassword_password_requires_nonletter, this.mPasswordMinNonLetter), Integer.valueOf(this.mPasswordMinNonLetter));
                }
            } else {
                boolean alphabetic = 262144 == this.mRequestedQuality;
                boolean alphanumeric = 327680 == this.mRequestedQuality;
                if ((alphabetic || alphanumeric) && letters == 0) {
                    return getString(R.string.lockpassword_password_requires_alpha);
                }
                if (alphanumeric && numbers == 0) {
                    return getString(R.string.lockpassword_password_requires_digit);
                }
            }
            if (this.mLockPatternUtils.checkPasswordHistory(password, this.mUserId)) {
                return getString(this.mIsAlphaMode ? R.string.lockpassword_password_recently_used : R.string.lockpassword_pin_recently_used);
            }
            return null;
        }

        public void handleNext() {
            if (this.mSaveAndFinishWorker != null) {
                return;
            }
            this.mChosenPassword = this.mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(this.mChosenPassword)) {
                return;
            }
            String errorMsg = null;
            if (this.mUiStage == Stage.Introduction) {
                errorMsg = validatePassword(this.mChosenPassword);
                if (errorMsg == null) {
                    this.mFirstPin = this.mChosenPassword;
                    this.mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                }
            } else if (this.mUiStage == Stage.NeedToConfirm) {
                if (this.mFirstPin.equals(this.mChosenPassword)) {
                    startSaveAndFinish();
                } else {
                    CharSequence tmp = this.mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    updateStage(Stage.ConfirmWrong);
                }
            }
            if (errorMsg == null) {
                return;
            }
            showError(errorMsg, this.mUiStage);
        }

        protected void setNextEnabled(boolean enabled) {
            this.mNextButton.setEnabled(enabled);
        }

        protected void setNextText(int text) {
            this.mNextButton.setText(text);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel_button:
                    if (this.mPasswordEntry != null && getActivity() != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService("input_method");
                        imm.hideSoftInputFromWindow(this.mPasswordEntry.getWindowToken(), 0);
                    }
                    getActivity().finish();
                    break;
                case R.id.next_button:
                    handleNext();
                    break;
            }
        }

        private void showError(String msg, Stage next) {
            this.mHeaderText.setText(msg);
            this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            Message mesg = this.mHandler.obtainMessage(1, next);
            this.mHandler.removeMessages(1);
            this.mHandler.sendMessageDelayed(mesg, 3000L);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId != 0 && actionId != 6 && actionId != 5) {
                return false;
            }
            handleNext();
            return true;
        }

        private void updateUi() {
            boolean canInput = this.mSaveAndFinishWorker == null;
            String password = this.mPasswordEntry.getText().toString();
            int length = password.length();
            if (this.mUiStage == Stage.Introduction) {
                if (length < this.mPasswordMinLength) {
                    String msg = getString(this.mIsAlphaMode ? R.string.lockpassword_password_too_short : R.string.lockpassword_pin_too_short, new Object[]{Integer.valueOf(this.mPasswordMinLength)});
                    this.mHeaderText.setText(msg);
                    setNextEnabled(false);
                } else {
                    String error = validatePassword(password);
                    if (error != null) {
                        this.mHeaderText.setText(error);
                        setNextEnabled(false);
                    } else {
                        this.mHeaderText.setText((CharSequence) null);
                        setNextEnabled(true);
                    }
                }
            } else {
                this.mHeaderText.setText(this.mIsAlphaMode ? this.mUiStage.alphaHint : this.mUiStage.numericHint);
                setNextEnabled(canInput && length > 0);
            }
            setNextText(this.mUiStage.buttonText);
            this.mPasswordEntryInputDisabler.setInputEnabled(canInput);
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (this.mUiStage == Stage.ConfirmWrong) {
                this.mUiStage = Stage.NeedToConfirm;
            }
            updateUi();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        private void startSaveAndFinish() {
            SaveAndFinishWorker saveAndFinishWorker = null;
            if (this.mSaveAndFinishWorker != null) {
                Log.w("ChooseLockPassword", "startSaveAndFinish with an existing SaveAndFinishWorker.");
                return;
            }
            this.mPasswordEntryInputDisabler.setInputEnabled(false);
            setNextEnabled(false);
            this.mSaveAndFinishWorker = new SaveAndFinishWorker(saveAndFinishWorker);
            this.mSaveAndFinishWorker.setListener(this);
            getFragmentManager().beginTransaction().add(this.mSaveAndFinishWorker, "save_and_finish_worker").commit();
            getFragmentManager().executePendingTransactions();
            boolean required = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
            this.mSaveAndFinishWorker.start(this.mLockPatternUtils, required, this.mHasChallenge, this.mChallenge, this.mChosenPassword, this.mCurrentPassword, this.mRequestedQuality, this.mUserId);
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
        private String mChosenPassword;
        private String mCurrentPassword;
        private int mRequestedQuality;

        SaveAndFinishWorker(SaveAndFinishWorker saveAndFinishWorker) {
            this();
        }

        private SaveAndFinishWorker() {
        }

        public void start(LockPatternUtils utils, boolean required, boolean hasChallenge, long challenge, String chosenPassword, String currentPassword, int requestedQuality, int userId) {
            prepare(utils, required, hasChallenge, challenge, userId);
            this.mChosenPassword = chosenPassword;
            this.mCurrentPassword = currentPassword;
            this.mRequestedQuality = requestedQuality;
            this.mUserId = userId;
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
            Intent result = new Intent();
            result.putExtra("hw_auth_token", bArrVerifyPassword);
            return result;
        }
    }
}
