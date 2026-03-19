package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.Editable;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassifier;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import java.util.function.Consumer;

public class RemoteInputView extends LinearLayout implements TextWatcher, View.OnClickListener {
    public static final Object VIEW_TAG = new Object();
    private RemoteInputController mController;
    private RemoteEditText mEditText;
    private NotificationData.Entry mEntry;
    private Consumer<Boolean> mOnVisibilityChangedListener;
    private PendingIntent mPendingIntent;
    private ProgressBar mProgressBar;
    private RemoteInput mRemoteInput;
    private RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler;
    private RemoteInput[] mRemoteInputs;
    private boolean mRemoved;
    private boolean mResetting;
    private int mRevealCx;
    private int mRevealCy;
    private int mRevealR;
    private ImageButton mSendButton;
    public final Object mToken;
    private NotificationViewWrapper mWrapper;

    public RemoteInputView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mToken = new Object();
        this.mRemoteInputQuickSettingsDisabler = (RemoteInputQuickSettingsDisabler) Dependency.get(RemoteInputQuickSettingsDisabler.class);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mProgressBar = (ProgressBar) findViewById(R.id.remote_input_progress);
        this.mSendButton = (ImageButton) findViewById(R.id.remote_input_send);
        this.mSendButton.setOnClickListener(this);
        this.mEditText = (RemoteEditText) getChildAt(0);
        this.mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean z = keyEvent == null && (i == 6 || i == 5 || i == 4);
                boolean z2 = keyEvent != null && KeyEvent.isConfirmKey(keyEvent.getKeyCode()) && keyEvent.getAction() == 0;
                if (z || z2) {
                    if (RemoteInputView.this.mEditText.length() > 0) {
                        RemoteInputView.this.sendRemoteInput();
                    }
                    return true;
                }
                return false;
            }
        });
        this.mEditText.addTextChangedListener(this);
        this.mEditText.setInnerFocusable(false);
        this.mEditText.mRemoteInputView = this;
    }

    private void sendRemoteInput() {
        Bundle bundle = new Bundle();
        bundle.putString(this.mRemoteInput.getResultKey(), this.mEditText.getText().toString());
        Intent intentAddFlags = new Intent().addFlags(268435456);
        RemoteInput.addResultsToIntent(this.mRemoteInputs, intentAddFlags, bundle);
        RemoteInput.setResultsSource(intentAddFlags, 0);
        this.mEditText.setEnabled(false);
        this.mSendButton.setVisibility(4);
        this.mProgressBar.setVisibility(0);
        this.mEntry.remoteInputText = this.mEditText.getText();
        this.mEntry.lastRemoteInputSent = SystemClock.elapsedRealtime();
        this.mController.addSpinning(this.mEntry.key, this.mToken);
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mEditText.mShowImeOnInputConnection = false;
        this.mController.remoteInputSent(this.mEntry);
        this.mEntry.setHasSentReply();
        ((ShortcutManager) getContext().getSystemService(ShortcutManager.class)).onApplicationActive(this.mEntry.notification.getPackageName(), this.mEntry.notification.getUser().getIdentifier());
        MetricsLogger.action(this.mContext, 398, this.mEntry.notification.getPackageName());
        try {
            this.mPendingIntent.send(this.mContext, 0, intentAddFlags);
        } catch (PendingIntent.CanceledException e) {
            Log.i("RemoteInput", "Unable to send remote input result", e);
            MetricsLogger.action(this.mContext, 399, this.mEntry.notification.getPackageName());
        }
    }

    public CharSequence getText() {
        return this.mEditText.getText();
    }

    public static RemoteInputView inflate(Context context, ViewGroup viewGroup, NotificationData.Entry entry, RemoteInputController remoteInputController) {
        RemoteInputView remoteInputView = (RemoteInputView) LayoutInflater.from(context).inflate(R.layout.remote_input, viewGroup, false);
        remoteInputView.mController = remoteInputController;
        remoteInputView.mEntry = entry;
        remoteInputView.mEditText.setRestrictedAcrossUser(true);
        remoteInputView.setTag(VIEW_TAG);
        remoteInputView.mEditText.setTextClassifier(TextClassifier.NO_OP);
        return remoteInputView;
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int i) {
        try {
            UserHandle user = this.mEntry.notification.getUser();
            UserHandle userHandleOf = UserHandle.of(ActivityManager.getCurrentUser());
            if (!UserHandle.ALL.equals(user) && !userHandleOf.equals(user)) {
                EventLog.writeEvent(1397638484, "123232892", -1, "");
            }
        } catch (Throwable th) {
            Log.i("RemoteInput", "Error attempting to log security fix for bug 123232892", th);
        }
        return super.startActionMode(callback, i);
    }

    @Override
    public void onClick(View view) {
        if (view == this.mSendButton) {
            sendRemoteInput();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        return true;
    }

    private void onDefocus(boolean z) {
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mEntry.remoteInputText = this.mEditText.getText();
        if (!this.mRemoved) {
            if (z && this.mRevealR > 0) {
                Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(this, this.mRevealCx, this.mRevealCy, this.mRevealR, 0.0f);
                animatorCreateCircularReveal.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                animatorCreateCircularReveal.setDuration(150L);
                animatorCreateCircularReveal.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        RemoteInputView.this.setVisibility(4);
                        if (RemoteInputView.this.mWrapper != null) {
                            RemoteInputView.this.mWrapper.setRemoteInputVisible(false);
                        }
                    }
                });
                animatorCreateCircularReveal.start();
            } else {
                setVisibility(4);
                if (this.mWrapper != null) {
                    this.mWrapper.setRemoteInputVisible(false);
                }
            }
        }
        this.mRemoteInputQuickSettingsDisabler.setRemoteInputActive(false);
        MetricsLogger.action(this.mContext, 400, this.mEntry.notification.getPackageName());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mEntry.row.isChangingPosition() && getVisibility() == 0 && this.mEditText.isFocusable()) {
            this.mEditText.requestFocus();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mEntry.row.isChangingPosition() || isTemporarilyDetached()) {
            return;
        }
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mController.removeSpinning(this.mEntry.key, this.mToken);
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.mPendingIntent = pendingIntent;
    }

    public void setRemoteInput(RemoteInput[] remoteInputArr, RemoteInput remoteInput) {
        this.mRemoteInputs = remoteInputArr;
        this.mRemoteInput = remoteInput;
        this.mEditText.setHint(this.mRemoteInput.getLabel());
    }

    public void focusAnimated() {
        if (getVisibility() != 0) {
            Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(this, this.mRevealCx, this.mRevealCy, 0.0f, this.mRevealR);
            animatorCreateCircularReveal.setDuration(360L);
            animatorCreateCircularReveal.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            animatorCreateCircularReveal.start();
        }
        focus();
    }

    public void focus() {
        MetricsLogger.action(this.mContext, 397, this.mEntry.notification.getPackageName());
        setVisibility(0);
        if (this.mWrapper != null) {
            this.mWrapper.setRemoteInputVisible(true);
        }
        if (UserHandle.myUserId() != ActivityManager.getCurrentUser()) {
            this.mEditText.setInputType(this.mEditText.getInputType() | 524288);
        }
        this.mEditText.setInnerFocusable(true);
        this.mEditText.mShowImeOnInputConnection = true;
        this.mEditText.setText(this.mEntry.remoteInputText);
        this.mEditText.setSelection(this.mEditText.getText().length());
        this.mEditText.requestFocus();
        this.mController.addRemoteInput(this.mEntry, this.mToken);
        this.mRemoteInputQuickSettingsDisabler.setRemoteInputActive(true);
        updateSendButton();
    }

    public void onNotificationUpdateOrReset() {
        boolean z;
        if (this.mProgressBar.getVisibility() != 0) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            reset();
        }
        if (isActive() && this.mWrapper != null) {
            this.mWrapper.setRemoteInputVisible(true);
        }
    }

    private void reset() {
        this.mResetting = true;
        this.mEntry.remoteInputTextWhenReset = SpannedString.valueOf(this.mEditText.getText());
        this.mEditText.getText().clear();
        this.mEditText.setEnabled(true);
        this.mSendButton.setVisibility(0);
        this.mProgressBar.setVisibility(4);
        this.mController.removeSpinning(this.mEntry.key, this.mToken);
        updateSendButton();
        onDefocus(false);
        this.mResetting = false;
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
        if (this.mResetting && view == this.mEditText) {
            return false;
        }
        return super.onRequestSendAccessibilityEvent(view, accessibilityEvent);
    }

    private void updateSendButton() {
        this.mSendButton.setEnabled(this.mEditText.getText().length() != 0);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        updateSendButton();
    }

    public void close() {
        this.mEditText.defocusIfNeeded(false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mController.requestDisallowLongPressAndDismiss();
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    public boolean requestScrollTo() {
        this.mController.lockScrollTo(this.mEntry);
        return true;
    }

    public boolean isActive() {
        return this.mEditText.isFocused() && this.mEditText.isEnabled();
    }

    public void stealFocusFrom(RemoteInputView remoteInputView) {
        remoteInputView.close();
        setPendingIntent(remoteInputView.mPendingIntent);
        setRemoteInput(remoteInputView.mRemoteInputs, remoteInputView.mRemoteInput);
        setRevealParameters(remoteInputView.mRevealCx, remoteInputView.mRevealCy, remoteInputView.mRevealR);
        focus();
    }

    public boolean updatePendingIntentFromActions(Notification.Action[] actionArr) {
        Intent intent;
        if (this.mPendingIntent == null || actionArr == null || (intent = this.mPendingIntent.getIntent()) == null) {
            return false;
        }
        for (Notification.Action action : actionArr) {
            RemoteInput[] remoteInputs = action.getRemoteInputs();
            if (action.actionIntent != null && remoteInputs != null && intent.filterEquals(action.actionIntent.getIntent())) {
                RemoteInput remoteInput = null;
                for (RemoteInput remoteInput2 : remoteInputs) {
                    if (remoteInput2.getAllowFreeFormInput()) {
                        remoteInput = remoteInput2;
                    }
                }
                if (remoteInput != null) {
                    setPendingIntent(action.actionIntent);
                    setRemoteInput(remoteInputs, remoteInput);
                    return true;
                }
            }
        }
        return false;
    }

    public PendingIntent getPendingIntent() {
        return this.mPendingIntent;
    }

    public void setRemoved() {
        this.mRemoved = true;
    }

    public void setRevealParameters(int i, int i2, int i3) {
        this.mRevealCx = i;
        this.mRevealCy = i2;
        this.mRevealR = i3;
    }

    @Override
    public void dispatchStartTemporaryDetach() {
        super.dispatchStartTemporaryDetach();
        detachViewFromParent(this.mEditText);
    }

    @Override
    public void dispatchFinishTemporaryDetach() {
        if (isAttachedToWindow()) {
            attachViewToParent(this.mEditText, 0, this.mEditText.getLayoutParams());
        } else {
            removeDetachedView(this.mEditText, false);
        }
        super.dispatchFinishTemporaryDetach();
    }

    public void setWrapper(NotificationViewWrapper notificationViewWrapper) {
        this.mWrapper = notificationViewWrapper;
    }

    public void setOnVisibilityChangedListener(Consumer<Boolean> consumer) {
        this.mOnVisibilityChangedListener = consumer;
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this && this.mOnVisibilityChangedListener != null) {
            this.mOnVisibilityChangedListener.accept(Boolean.valueOf(i == 0));
        }
    }

    public boolean isSending() {
        return getVisibility() == 0 && this.mController.isSpinning(this.mEntry.key, this.mToken);
    }

    public static class RemoteEditText extends EditText {
        private final Drawable mBackground;
        private RemoteInputView mRemoteInputView;
        boolean mShowImeOnInputConnection;

        public RemoteEditText(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mBackground = getBackground();
        }

        private void defocusIfNeeded(boolean z) {
            if ((this.mRemoteInputView != null && this.mRemoteInputView.mEntry.row.isChangingPosition()) || isTemporarilyDetached()) {
                if (isTemporarilyDetached() && this.mRemoteInputView != null) {
                    this.mRemoteInputView.mEntry.remoteInputText = getText();
                    return;
                }
                return;
            }
            if (isFocusable() && isEnabled()) {
                setInnerFocusable(false);
                if (this.mRemoteInputView != null) {
                    this.mRemoteInputView.onDefocus(z);
                }
                this.mShowImeOnInputConnection = false;
            }
        }

        @Override
        protected void onVisibilityChanged(View view, int i) {
            super.onVisibilityChanged(view, i);
            if (!isShown()) {
                defocusIfNeeded(false);
            }
        }

        @Override
        protected void onFocusChanged(boolean z, int i, Rect rect) {
            super.onFocusChanged(z, i, rect);
            if (!z) {
                defocusIfNeeded(true);
            }
        }

        @Override
        public void getFocusedRect(Rect rect) {
            super.getFocusedRect(rect);
            rect.top = this.mScrollY;
            rect.bottom = this.mScrollY + (this.mBottom - this.mTop);
        }

        @Override
        public boolean requestRectangleOnScreen(Rect rect) {
            return this.mRemoteInputView.requestScrollTo();
        }

        @Override
        public boolean onKeyDown(int i, KeyEvent keyEvent) {
            if (i == 4) {
                return true;
            }
            return super.onKeyDown(i, keyEvent);
        }

        @Override
        public boolean onKeyUp(int i, KeyEvent keyEvent) {
            if (i == 4) {
                defocusIfNeeded(true);
                return true;
            }
            return super.onKeyUp(i, keyEvent);
        }

        @Override
        public boolean onKeyPreIme(int i, KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == 4 && keyEvent.getAction() == 1) {
                defocusIfNeeded(true);
            }
            return super.onKeyPreIme(i, keyEvent);
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return !(this.mRemoteInputView != null && this.mRemoteInputView.mRemoved) && super.onCheckIsTextEditor();
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
            final InputMethodManager inputMethodManager;
            InputConnection inputConnectionOnCreateInputConnection = super.onCreateInputConnection(editorInfo);
            if (this.mShowImeOnInputConnection && inputConnectionOnCreateInputConnection != null && (inputMethodManager = InputMethodManager.getInstance()) != null) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        inputMethodManager.viewClicked(RemoteEditText.this);
                        inputMethodManager.showSoftInput(RemoteEditText.this, 0);
                    }
                });
            }
            return inputConnectionOnCreateInputConnection;
        }

        @Override
        public void onCommitCompletion(CompletionInfo completionInfo) {
            clearComposingText();
            setText(completionInfo.getText());
            setSelection(getText().length());
        }

        void setInnerFocusable(boolean z) {
            setFocusableInTouchMode(z);
            setFocusable(z);
            setCursorVisible(z);
            if (z) {
                requestFocus();
                setBackground(this.mBackground);
            } else {
                setBackground(null);
            }
        }
    }
}
