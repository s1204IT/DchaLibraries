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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.settings.notification.RedactionInterstitial;

public class ChooseLockPassword extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, int quality, boolean isFallback, int minLength, int maxLength, boolean requirePasswordToDecrypt, boolean confirmCredentials) {
        Intent intent = new Intent().setClass(context, ChooseLockPassword.class);
        intent.putExtra("lockscreen.password_type", quality);
        intent.putExtra("lockscreen.password_min", minLength);
        intent.putExtra("lockscreen.password_max", maxLength);
        intent.putExtra("confirm_credentials", confirmCredentials);
        intent.putExtra("lockscreen.biometric_weak_fallback", isFallback);
        intent.putExtra("extra_require_password", requirePasswordToDecrypt);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_password_header);
        setTitle(msg);
    }

    public static class ChooseLockPasswordFragment extends Fragment implements TextWatcher, View.OnClickListener, TextView.OnEditorActionListener {
        private Button mCancelButton;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private String mFirstPin;
        private TextView mHeaderText;
        private boolean mIsAlphaMode;
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private KeyboardView mKeyboardView;
        private LockPatternUtils mLockPatternUtils;
        private Button mNextButton;
        private TextView mPasswordEntry;
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
        private boolean mDone = false;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    ChooseLockPasswordFragment.this.updateStage((Stage) msg.obj);
                }
            }
        };

        protected enum Stage {
            Introduction(R.string.lockpassword_choose_your_password_header, R.string.lockpassword_choose_your_pin_header, R.string.lockpassword_continue_label),
            NeedToConfirm(R.string.lockpassword_confirm_your_password_header, R.string.lockpassword_confirm_your_pin_header, R.string.lockpassword_ok_label),
            ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match, R.string.lockpassword_confirm_pins_dont_match, R.string.lockpassword_continue_label);

            public final int alphaHint;
            public final int buttonText;
            public final int numericHint;

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
            this.mRequestedQuality = Math.max(intent.getIntExtra("lockscreen.password_type", this.mRequestedQuality), this.mLockPatternUtils.getRequestedPasswordQuality());
            this.mPasswordMinLength = Math.max(intent.getIntExtra("lockscreen.password_min", this.mPasswordMinLength), this.mLockPatternUtils.getRequestedMinimumPasswordLength());
            this.mPasswordMaxLength = intent.getIntExtra("lockscreen.password_max", this.mPasswordMaxLength);
            this.mPasswordMinLetters = Math.max(intent.getIntExtra("lockscreen.password_min_letters", this.mPasswordMinLetters), this.mLockPatternUtils.getRequestedPasswordMinimumLetters());
            this.mPasswordMinUpperCase = Math.max(intent.getIntExtra("lockscreen.password_min_uppercase", this.mPasswordMinUpperCase), this.mLockPatternUtils.getRequestedPasswordMinimumUpperCase());
            this.mPasswordMinLowerCase = Math.max(intent.getIntExtra("lockscreen.password_min_lowercase", this.mPasswordMinLowerCase), this.mLockPatternUtils.getRequestedPasswordMinimumLowerCase());
            this.mPasswordMinNumeric = Math.max(intent.getIntExtra("lockscreen.password_min_numeric", this.mPasswordMinNumeric), this.mLockPatternUtils.getRequestedPasswordMinimumNumeric());
            this.mPasswordMinSymbols = Math.max(intent.getIntExtra("lockscreen.password_min_symbols", this.mPasswordMinSymbols), this.mLockPatternUtils.getRequestedPasswordMinimumSymbols());
            this.mPasswordMinNonLetter = Math.max(intent.getIntExtra("lockscreen.password_min_nonletter", this.mPasswordMinNonLetter), this.mLockPatternUtils.getRequestedPasswordMinimumNonLetter());
            this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
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
            this.mIsAlphaMode = 262144 == this.mRequestedQuality || 327680 == this.mRequestedQuality || 393216 == this.mRequestedQuality;
            this.mKeyboardView = view.findViewById(R.id.keyboard);
            this.mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
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
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    this.mChooseLockSettingsHelper.launchConfirmationActivity(58, null, null);
                }
            } else {
                this.mFirstPin = savedInstanceState.getString("first_pin");
                String state = savedInstanceState.getString("ui_stage");
                if (state != null) {
                    this.mUiStage = Stage.valueOf(state);
                    updateStage(this.mUiStage);
                }
            }
            this.mDone = false;
            if (activity instanceof SettingsActivity) {
                SettingsActivity sa = (SettingsActivity) activity;
                int id = this.mIsAlphaMode ? R.string.lockpassword_choose_your_password_header : R.string.lockpassword_choose_your_pin_header;
                CharSequence title = getText(id);
                sa.setTitle(title);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(this.mUiStage);
            this.mKeyboardView.requestFocus();
        }

        @Override
        public void onPause() {
            this.mHandler.removeMessages(1);
            super.onPause();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("ui_stage", this.mUiStage.name());
            outState.putString("first_pin", this.mFirstPin);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case 58:
                    if (resultCode != -1) {
                        getActivity().setResult(1);
                        getActivity().finish();
                    }
                    break;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context);
        }

        protected void updateStage(Stage stage) {
            Stage previousStage = this.mUiStage;
            this.mUiStage = stage;
            updateUi();
            if (previousStage != stage) {
                this.mHeaderText.announceForAccessibility(this.mHeaderText.getText());
            }
        }

        private String validatePassword(String password) {
            if (password.length() < this.mPasswordMinLength) {
                return getString(this.mIsAlphaMode ? R.string.lockpassword_password_too_short : R.string.lockpassword_pin_too_short, Integer.valueOf(this.mPasswordMinLength));
            }
            if (password.length() > this.mPasswordMaxLength) {
                return getString(this.mIsAlphaMode ? R.string.lockpassword_password_too_long : R.string.lockpassword_pin_too_long, Integer.valueOf(this.mPasswordMaxLength + 1));
            }
            int letters = 0;
            int numbers = 0;
            int lowercase = 0;
            int symbols = 0;
            int uppercase = 0;
            int nonletter = 0;
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
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
            if (this.mLockPatternUtils.checkPasswordHistory(password)) {
                return getString(this.mIsAlphaMode ? R.string.lockpassword_password_recently_used : R.string.lockpassword_pin_recently_used);
            }
            return null;
        }

        public void handleNext() {
            if (!this.mDone) {
                String pin = this.mPasswordEntry.getText().toString();
                if (!TextUtils.isEmpty(pin)) {
                    String errorMsg = null;
                    if (this.mUiStage == Stage.Introduction) {
                        errorMsg = validatePassword(pin);
                        if (errorMsg == null) {
                            this.mFirstPin = pin;
                            this.mPasswordEntry.setText("");
                            updateStage(Stage.NeedToConfirm);
                        }
                    } else if (this.mUiStage == Stage.NeedToConfirm) {
                        if (this.mFirstPin.equals(pin)) {
                            boolean isFallback = getActivity().getIntent().getBooleanExtra("lockscreen.biometric_weak_fallback", false);
                            boolean wasSecureBefore = this.mLockPatternUtils.isSecure();
                            this.mLockPatternUtils.clearLock(isFallback);
                            boolean required = getActivity().getIntent().getBooleanExtra("extra_require_password", true);
                            this.mLockPatternUtils.setCredentialRequiredToDecrypt(required);
                            this.mLockPatternUtils.saveLockPassword(pin, this.mRequestedQuality, isFallback);
                            getActivity().setResult(1);
                            getActivity().finish();
                            this.mDone = true;
                            if (!wasSecureBefore) {
                                startActivity(getRedactionInterstitialIntent(getActivity()));
                            }
                        } else {
                            CharSequence tmp = this.mPasswordEntry.getText();
                            if (tmp != null) {
                                Selection.setSelection((Spannable) tmp, 0, tmp.length());
                            }
                            updateStage(Stage.ConfirmWrong);
                        }
                    }
                    if (errorMsg != null) {
                        showError(errorMsg, this.mUiStage);
                    }
                }
            }
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
            String password = this.mPasswordEntry.getText().toString();
            int length = password.length();
            if (this.mUiStage == Stage.Introduction && length > 0) {
                if (length < this.mPasswordMinLength) {
                    String msg = getString(this.mIsAlphaMode ? R.string.lockpassword_password_too_short : R.string.lockpassword_pin_too_short, Integer.valueOf(this.mPasswordMinLength));
                    this.mHeaderText.setText(msg);
                    setNextEnabled(false);
                } else {
                    String error = validatePassword(password);
                    if (error != null) {
                        this.mHeaderText.setText(error);
                        setNextEnabled(false);
                    } else {
                        this.mHeaderText.setText(R.string.lockpassword_press_continue);
                        setNextEnabled(true);
                    }
                }
            } else {
                this.mHeaderText.setText(this.mIsAlphaMode ? this.mUiStage.alphaHint : this.mUiStage.numericHint);
                setNextEnabled(length > 0);
            }
            setNextText(this.mUiStage.buttonText);
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
    }
}
