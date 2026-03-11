package com.android.keyguard;

import android.R;
import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;
import com.android.internal.widget.TextViewInputDisabler;
import java.util.List;

public class KeyguardPasswordView extends KeyguardAbsKeyInputView implements KeyguardSecurityView, TextView.OnEditorActionListener, TextWatcher {
    private final int mDisappearYTranslation;
    private Interpolator mFastOutLinearInInterpolator;
    InputMethodManager mImm;
    private Interpolator mLinearOutSlowInInterpolator;
    private TextView mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryDisabler;
    private final boolean mShowImeAtScreenOn;

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShowImeAtScreenOn = context.getResources().getBoolean(R$bool.kg_show_ime_at_screen_on);
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_linear_in);
    }

    @Override
    protected void resetState() {
        this.mSecurityMessageDisplay.setMessage(R$string.kg_password_instructions, true);
        boolean wasDisabled = this.mPasswordEntry.isEnabled();
        setPasswordEntryEnabled(true);
        setPasswordEntryInputEnabled(true);
        if (!wasDisabled) {
            return;
        }
        this.mImm.showSoftInput(this.mPasswordEntry, 1);
    }

    @Override
    protected int getPasswordTextViewId() {
        return R$id.passwordEntry;
    }

    @Override
    public boolean needsInput() {
        Log.d("KeyguardPasswordView", "needsInput() - returns true.");
        return true;
    }

    @Override
    public void onResume(final int reason) {
        super.onResume(reason);
        post(new Runnable() {
            @Override
            public void run() {
                if (!KeyguardPasswordView.this.isShown() || !KeyguardPasswordView.this.mPasswordEntry.isEnabled()) {
                    return;
                }
                KeyguardPasswordView.this.mPasswordEntry.requestFocus();
                Log.d("KeyguardPasswordView", "reason = " + reason + ", mShowImeAtScreenOn = " + KeyguardPasswordView.this.mShowImeAtScreenOn);
                if (reason == 1 && !KeyguardPasswordView.this.mShowImeAtScreenOn) {
                    return;
                }
                Log.d("KeyguardPasswordView", "onResume() - call showSoftInput()");
                KeyguardPasswordView.this.mImm.showSoftInput(KeyguardPasswordView.this.mPasswordEntry, 1);
            }
        });
    }

    @Override
    protected int getPromtReasonStringRes(int reason) {
        switch (reason) {
            case 0:
                return 0;
            case 1:
                return R$string.kg_prompt_reason_restart_password;
            case 2:
                return R$string.kg_prompt_reason_timeout_password;
            case 3:
                return R$string.kg_prompt_reason_device_admin;
            case 4:
                return R$string.kg_prompt_reason_user_request;
            default:
                return R$string.kg_prompt_reason_timeout_password;
        }
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
        this.mPasswordEntryDisabler = new TextViewInputDisabler(this.mPasswordEntry);
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
        this.mPasswordEntry.requestFocus();
        View switchImeButton = findViewById(R$id.switch_ime_button);
        if (switchImeButton != null && hasMultipleEnabledIMEsOrSubtypes(this.mImm, false)) {
            switchImeButton.setVisibility(0);
            imeOrDeleteButtonVisible = true;
            switchImeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    KeyguardPasswordView.this.mCallback.userActivity();
                    KeyguardPasswordView.this.mImm.showInputMethodPicker(false);
                }
            });
        }
        if (imeOrDeleteButtonVisible) {
            return;
        }
        ViewGroup.LayoutParams params = this.mPasswordEntry.getLayoutParams();
        if (!(params instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) params;
        mlp.setMarginStart(0);
        this.mPasswordEntry.setLayoutParams(params);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void resetPasswordText(boolean animate, boolean announce) {
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

    @Override
    protected void setPasswordEntryInputEnabled(boolean enabled) {
        this.mPasswordEntryDisabler.setInputEnabled(enabled);
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
    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
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
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.userActivity();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (TextUtils.isEmpty(s)) {
            return;
        }
        onUserInput();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean isSoftImeEvent = event == null ? actionId == 0 || actionId == 6 || actionId == 5 : false;
        boolean isKeyboardEnterKey = event != null && KeyEvent.isConfirmKey(event.getKeyCode()) && event.getAction() == 0;
        if (!isSoftImeEvent && !isKeyboardEnterKey) {
            return false;
        }
        verifyPasswordAndUnlock();
        return true;
    }
}
