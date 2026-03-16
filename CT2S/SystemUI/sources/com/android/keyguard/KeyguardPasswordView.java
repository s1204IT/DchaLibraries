package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.os.BenesseExtension;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;
import java.util.List;

public class KeyguardPasswordView extends KeyguardAbsKeyInputView implements TextWatcher, TextView.OnEditorActionListener, KeyguardSecurityView {
    private final int mDisappearYTranslation;
    private Interpolator mFastOutLinearInInterpolator;
    InputMethodManager mImm;
    private Interpolator mLinearOutSlowInInterpolator;
    private TextView mPasswordEntry;
    private final boolean mShowImeAtScreenOn;

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowImeAtScreenOn = context.getResources().getBoolean(R.bool.kg_show_ime_at_screen_on);
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in);
    }

    @Override
    protected void resetState() {
        this.mSecurityMessageDisplay.setMessage(R.string.kg_password_instructions, false);
        this.mPasswordEntry.setEnabled(true);
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.passwordEntry;
    }

    @Override
    public boolean needsInput() {
        return true;
    }

    @Override
    public void onResume(final int reason) {
        super.onResume(reason);
        post(new Runnable() {
            @Override
            public void run() {
                if (KeyguardPasswordView.this.isShown()) {
                    KeyguardPasswordView.this.mPasswordEntry.requestFocus();
                    if (reason != 1 || KeyguardPasswordView.this.mShowImeAtScreenOn) {
                        KeyguardPasswordView.this.mImm.showSoftInput(KeyguardPasswordView.this.mPasswordEntry, 1);
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mImm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    @Override
    public void reset() {
        super.reset();
        this.mPasswordEntry.requestFocus();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        boolean imeOrDeleteButtonVisible = false;
        this.mImm = (InputMethodManager) getContext().getSystemService("input_method");
        this.mPasswordEntry = (TextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setKeyListener(TextKeyListener.getInstance());
        this.mPasswordEntry.setInputType(129);
        this.mPasswordEntry.setOnEditorActionListener(this);
        this.mPasswordEntry.addTextChangedListener(this);
        this.mPasswordEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KeyguardPasswordView.this.mCallback.userActivity();
            }
        });
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (KeyguardPasswordView.this.mCallback != null) {
                    KeyguardPasswordView.this.mCallback.userActivity();
                }
            }
        });
        this.mPasswordEntry.requestFocus();
        View switchImeButton = findViewById(R.id.switch_ime_button);
        if (switchImeButton != null && hasMultipleEnabledIMEsOrSubtypes(this.mImm, false)) {
            switchImeButton.setVisibility(0);
            imeOrDeleteButtonVisible = true;
            switchImeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BenesseExtension.getDchaState() == 0) {
                        KeyguardPasswordView.this.mCallback.userActivity();
                        KeyguardPasswordView.this.mImm.showInputMethodPicker();
                    }
                }
            });
        }
        if (!imeOrDeleteButtonVisible) {
            ViewGroup.LayoutParams params = this.mPasswordEntry.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) params;
                mlp.setMarginStart(0);
                this.mPasswordEntry.setLayoutParams(params);
            }
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void resetPasswordText(boolean animate) {
        this.mPasswordEntry.setText("");
    }

    @Override
    protected String getPasswordText() {
        return this.mPasswordEntry.getText().toString();
    }

    @Override
    protected void setPasswordEntryEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager imm, boolean shouldIncludeAuxiliarySubtypes) {
        List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();
        int filteredImisCount = 0;
        for (InputMethodInfo imi : enabledImis) {
            if (filteredImisCount > 1) {
                return true;
            }
            List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
            if (subtypes.isEmpty()) {
                filteredImisCount++;
            } else {
                int auxCount = 0;
                for (InputMethodSubtype subtype : subtypes) {
                    if (subtype.isAuxiliary()) {
                        auxCount++;
                    }
                }
                int nonAuxCount = subtypes.size() - auxCount;
                if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                    filteredImisCount++;
                }
            }
        }
        return filteredImisCount > 1 || imm.getEnabledInputMethodSubtypeList(null, false).size() > 1;
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public int getWrongPasswordStringId() {
        return R.string.kg_wrong_password;
    }

    @Override
    public void startAppearAnimation() {
        setAlpha(0.0f);
        setTranslationY(0.0f);
        animate().alpha(1.0f).withLayer().setDuration(300L).setInterpolator(this.mLinearOutSlowInInterpolator);
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        animate().alpha(0.0f).translationY(this.mDisappearYTranslation).setInterpolator(this.mFastOutLinearInInterpolator).setDuration(100L).withEndAction(finishRunnable);
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (this.mCallback != null) {
            this.mCallback.userActivity();
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean isSoftImeEvent = event == null && (actionId == 0 || actionId == 6 || actionId == 5);
        boolean isKeyboardEnterKey = event != null && KeyEvent.isConfirmKey(event.getKeyCode()) && event.getAction() == 0;
        if (!isSoftImeEvent && !isKeyboardEnterKey) {
            return false;
        }
        verifyPasswordAndUnlock();
        return true;
    }
}
