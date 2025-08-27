package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.PasswordTextView;

/* loaded from: classes.dex */
public abstract class KeyguardPinBasedInputView extends KeyguardAbsKeyInputView implements View.OnKeyListener, View.OnTouchListener {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private View mButton0;
    private View mButton1;
    private View mButton2;
    private View mButton3;
    private View mButton4;
    private View mButton5;
    private View mButton6;
    private View mButton7;
    private View mButton8;
    private View mButton9;
    private View mDeleteButton;
    private View mOkButton;

    @VisibleForTesting
    public PasswordTextView mPasswordEntry;

    public KeyguardPinBasedInputView(Context context) {
        this(context, null);
    }

    public KeyguardPinBasedInputView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.view.ViewGroup
    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        return this.mPasswordEntry.requestFocus(i, rect);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected void resetState() {
        setPasswordEntryEnabled(true);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected void setPasswordEntryEnabled(boolean z) {
        this.mPasswordEntry.setEnabled(z);
        this.mOkButton.setEnabled(z);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected void setPasswordEntryInputEnabled(boolean z) {
        this.mPasswordEntry.setEnabled(z);
        this.mOkButton.setEnabled(z);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView, android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            performClick(this.mOkButton);
            return true;
        }
        if (i == 67) {
            performClick(this.mDeleteButton);
            return true;
        }
        if (i >= 7 && i <= 16) {
            performNumberClick(i - 7);
            return true;
        }
        if (i >= 144 && i <= 153) {
            performNumberClick(i - 144);
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected int getPromptReasonStringRes(int i) {
        switch (i) {
        }
        return com.android.systemui.R.string.kg_prompt_reason_timeout_pin;
    }

    private void performClick(View view) {
        view.performClick();
    }

    private void performNumberClick(int i) {
        switch (i) {
            case 0:
                performClick(this.mButton0);
                break;
            case 1:
                performClick(this.mButton1);
                break;
            case 2:
                performClick(this.mButton2);
                break;
            case 3:
                performClick(this.mButton3);
                break;
            case 4:
                performClick(this.mButton4);
                break;
            case 5:
                performClick(this.mButton5);
                break;
            case 6:
                performClick(this.mButton6);
                break;
            case 7:
                performClick(this.mButton7);
                break;
            case 8:
                performClick(this.mButton8);
                break;
            case 9:
                performClick(this.mButton9);
                break;
        }
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected void resetPasswordText(boolean z, boolean z2) {
        this.mPasswordEntry.reset(z, z2);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    protected String getPasswordText() {
        return this.mPasswordEntry.getText();
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView, android.view.View
    protected void onFinishInflate() {
        this.mPasswordEntry = (PasswordTextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setOnKeyListener(this);
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.setUserActivityListener(new PasswordTextView.UserActivityListener() { // from class: com.android.keyguard.KeyguardPinBasedInputView.1
            @Override // com.android.keyguard.PasswordTextView.UserActivityListener
            public void onUserActivity() {
                KeyguardPinBasedInputView.this.onUserInput();
            }
        });
        this.mOkButton = findViewById(com.android.systemui.R.id.key_enter);
        if (this.mOkButton != null) {
            this.mOkButton.setOnTouchListener(this);
            this.mOkButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.keyguard.KeyguardPinBasedInputView.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                        KeyguardPinBasedInputView.this.verifyPasswordAndUnlock();
                    }
                }
            });
            this.mOkButton.setOnHoverListener(new LiftToActivateListener(getContext()));
        }
        this.mDeleteButton = findViewById(com.android.systemui.R.id.delete_button);
        this.mDeleteButton.setVisibility(0);
        this.mDeleteButton.setOnTouchListener(this);
        this.mDeleteButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.keyguard.KeyguardPinBasedInputView.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPinBasedInputView.this.mPasswordEntry.deleteLastChar();
                }
            }
        });
        this.mDeleteButton.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.android.keyguard.KeyguardPinBasedInputView.4
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPinBasedInputView.this.resetPasswordText(true, true);
                }
                KeyguardPinBasedInputView.this.doHapticKeyClick();
                return true;
            }
        });
        this.mButton0 = findViewById(com.android.systemui.R.id.key0);
        this.mButton1 = findViewById(com.android.systemui.R.id.key1);
        this.mButton2 = findViewById(com.android.systemui.R.id.key2);
        this.mButton3 = findViewById(com.android.systemui.R.id.key3);
        this.mButton4 = findViewById(com.android.systemui.R.id.key4);
        this.mButton5 = findViewById(com.android.systemui.R.id.key5);
        this.mButton6 = findViewById(com.android.systemui.R.id.key6);
        this.mButton7 = findViewById(com.android.systemui.R.id.key7);
        this.mButton8 = findViewById(com.android.systemui.R.id.key8);
        this.mButton9 = findViewById(com.android.systemui.R.id.key9);
        this.mPasswordEntry.requestFocus();
        super.onFinishInflate();
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardSecurityView
    public void onResume(int i) {
        super.onResume(i);
        this.mPasswordEntry.requestFocus();
    }

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            doHapticKeyClick();
            return false;
        }
        return false;
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getAction() == 0) {
            if (DEBUG) {
                Log.d("KeyguardPinBasedInputView", "keyCode: " + i + " event: " + keyEvent);
            }
            return onKeyDown(i, keyEvent);
        }
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public CharSequence getTitle() {
        return getContext().getString(android.R.string.config_systemImageEditor);
    }
}
