package com.android.systemui.statusbar.policy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.stack.ScrollContainer;

public class RemoteInputView extends LinearLayout implements View.OnClickListener, TextWatcher {
    public static final Object VIEW_TAG = new Object();
    private RemoteInputController mController;
    private RemoteEditText mEditText;
    private NotificationData.Entry mEntry;
    private PendingIntent mPendingIntent;
    private ProgressBar mProgressBar;
    private RemoteInput mRemoteInput;
    private RemoteInput[] mRemoteInputs;
    private boolean mRemoved;
    private ScrollContainer mScrollContainer;
    private View mScrollContainerChild;
    private ImageButton mSendButton;

    public RemoteInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean isSoftImeEvent = event == null ? actionId == 6 || actionId == 5 || actionId == 4 : false;
                boolean isKeyboardEnterKey = event != null && KeyEvent.isConfirmKey(event.getKeyCode()) && event.getAction() == 0;
                if (!isSoftImeEvent && !isKeyboardEnterKey) {
                    return false;
                }
                if (RemoteInputView.this.mEditText.length() > 0) {
                    RemoteInputView.this.sendRemoteInput();
                    return true;
                }
                return true;
            }
        });
        this.mEditText.addTextChangedListener(this);
        this.mEditText.setInnerFocusable(false);
        this.mEditText.mRemoteInputView = this;
    }

    public void sendRemoteInput() {
        Bundle results = new Bundle();
        results.putString(this.mRemoteInput.getResultKey(), this.mEditText.getText().toString());
        Intent fillInIntent = new Intent().addFlags(268435456);
        RemoteInput.addResultsToIntent(this.mRemoteInputs, fillInIntent, results);
        this.mEditText.setEnabled(false);
        this.mSendButton.setVisibility(4);
        this.mProgressBar.setVisibility(0);
        this.mEntry.remoteInputText = this.mEditText.getText();
        this.mController.addSpinning(this.mEntry.key);
        this.mController.removeRemoteInput(this.mEntry);
        this.mEditText.mShowImeOnInputConnection = false;
        this.mController.remoteInputSent(this.mEntry);
        MetricsLogger.action(this.mContext, 398, this.mEntry.notification.getPackageName());
        try {
            this.mPendingIntent.send(this.mContext, 0, fillInIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.i("RemoteInput", "Unable to send remote input result", e);
            MetricsLogger.action(this.mContext, 399, this.mEntry.notification.getPackageName());
        }
    }

    public static RemoteInputView inflate(Context context, ViewGroup root, NotificationData.Entry entry, RemoteInputController controller) {
        RemoteInputView v = (RemoteInputView) LayoutInflater.from(context).inflate(R.layout.remote_input, root, false);
        v.mController = controller;
        v.mEntry = entry;
        v.setTag(VIEW_TAG);
        return v;
    }

    @Override
    public void onClick(View v) {
        if (v != this.mSendButton) {
            return;
        }
        sendRemoteInput();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }

    public void onDefocus() {
        this.mController.removeRemoteInput(this.mEntry);
        this.mEntry.remoteInputText = this.mEditText.getText();
        if (!this.mRemoved) {
            setVisibility(4);
        }
        MetricsLogger.action(this.mContext, 400, this.mEntry.notification.getPackageName());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mEntry.row.isChangingPosition() || getVisibility() != 0 || !this.mEditText.isFocusable()) {
            return;
        }
        this.mEditText.requestFocus();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mEntry.row.isChangingPosition()) {
            return;
        }
        this.mController.removeRemoteInput(this.mEntry);
        this.mController.removeSpinning(this.mEntry.key);
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.mPendingIntent = pendingIntent;
    }

    public void setRemoteInput(RemoteInput[] remoteInputs, RemoteInput remoteInput) {
        this.mRemoteInputs = remoteInputs;
        this.mRemoteInput = remoteInput;
        this.mEditText.setHint(this.mRemoteInput.getLabel());
    }

    public void focus() {
        MetricsLogger.action(this.mContext, 397, this.mEntry.notification.getPackageName());
        setVisibility(0);
        this.mController.addRemoteInput(this.mEntry);
        this.mEditText.setInnerFocusable(true);
        this.mEditText.mShowImeOnInputConnection = true;
        this.mEditText.setText(this.mEntry.remoteInputText);
        this.mEditText.setSelection(this.mEditText.getText().length());
        this.mEditText.requestFocus();
        updateSendButton();
    }

    public void onNotificationUpdateOrReset() {
        boolean sending = this.mProgressBar.getVisibility() == 0;
        if (!sending) {
            return;
        }
        reset();
    }

    private void reset() {
        this.mEditText.getText().clear();
        this.mEditText.setEnabled(true);
        this.mSendButton.setVisibility(0);
        this.mProgressBar.setVisibility(4);
        this.mController.removeSpinning(this.mEntry.key);
        updateSendButton();
        onDefocus();
    }

    private void updateSendButton() {
        this.mSendButton.setEnabled(this.mEditText.getText().length() != 0);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateSendButton();
    }

    public void close() {
        this.mEditText.defocusIfNeeded();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            findScrollContainer();
            if (this.mScrollContainer != null) {
                this.mScrollContainer.requestDisallowLongPress();
                this.mScrollContainer.requestDisallowDismiss();
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean requestScrollTo() {
        findScrollContainer();
        this.mScrollContainer.lockScrollTo(this.mScrollContainerChild);
        return true;
    }

    private void findScrollContainer() {
        if (this.mScrollContainer != null) {
            return;
        }
        this.mScrollContainerChild = null;
        for (ViewParent parent = this; parent != 0; parent = parent.getParent()) {
            if (this.mScrollContainerChild == null && (parent instanceof ExpandableView)) {
                this.mScrollContainerChild = (View) parent;
            }
            if (parent.getParent() instanceof ScrollContainer) {
                this.mScrollContainer = (ScrollContainer) parent.getParent();
                if (this.mScrollContainerChild != null) {
                    return;
                }
                this.mScrollContainerChild = (View) parent;
                return;
            }
        }
    }

    public boolean isActive() {
        return this.mEditText.isFocused();
    }

    public void stealFocusFrom(RemoteInputView other) {
        other.close();
        setPendingIntent(other.mPendingIntent);
        setRemoteInput(other.mRemoteInputs, other.mRemoteInput);
        focus();
    }

    public boolean updatePendingIntentFromActions(Notification.Action[] actions) {
        Intent current;
        if (this.mPendingIntent == null || actions == null || (current = this.mPendingIntent.getIntent()) == null) {
            return false;
        }
        for (Notification.Action a : actions) {
            RemoteInput[] inputs = a.getRemoteInputs();
            if (a.actionIntent != null && inputs != null) {
                Intent candidate = a.actionIntent.getIntent();
                if (current.filterEquals(candidate)) {
                    RemoteInput input = null;
                    for (RemoteInput i : inputs) {
                        if (i.getAllowFreeFormInput()) {
                            input = i;
                        }
                    }
                    if (input != null) {
                        setPendingIntent(a.actionIntent);
                        setRemoteInput(inputs, input);
                        return true;
                    }
                } else {
                    continue;
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

    public static class RemoteEditText extends EditText {
        private final Drawable mBackground;
        private RemoteInputView mRemoteInputView;
        boolean mShowImeOnInputConnection;

        public RemoteEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mBackground = getBackground();
        }

        public void defocusIfNeeded() {
            if ((this.mRemoteInputView != null && this.mRemoteInputView.mEntry.row.isChangingPosition()) || !isFocusable() || !isEnabled()) {
                return;
            }
            setInnerFocusable(false);
            if (this.mRemoteInputView != null) {
                this.mRemoteInputView.onDefocus();
            }
            this.mShowImeOnInputConnection = false;
        }

        @Override
        protected void onVisibilityChanged(View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (isShown()) {
                return;
            }
            defocusIfNeeded();
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (focused) {
                return;
            }
            defocusIfNeeded();
        }

        @Override
        public void getFocusedRect(Rect r) {
            super.getFocusedRect(r);
            r.top = this.mScrollY;
            r.bottom = this.mScrollY + (this.mBottom - this.mTop);
        }

        @Override
        public boolean requestRectangleOnScreen(Rect rectangle) {
            return this.mRemoteInputView.requestScrollTo();
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == 4 && event.getAction() == 1) {
                defocusIfNeeded();
                InputMethodManager imm = InputMethodManager.getInstance();
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
                return true;
            }
            return super.onKeyPreIme(keyCode, event);
        }

        @Override
        public boolean onCheckIsTextEditor() {
            boolean flyingOut = this.mRemoteInputView != null ? this.mRemoteInputView.mRemoved : false;
            if (flyingOut) {
                return false;
            }
            return super.onCheckIsTextEditor();
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            final InputMethodManager imm;
            InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
            if (this.mShowImeOnInputConnection && inputConnection != null && (imm = InputMethodManager.getInstance()) != null) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        imm.viewClicked(RemoteEditText.this);
                        imm.showSoftInput(RemoteEditText.this, 0);
                    }
                });
            }
            return inputConnection;
        }

        @Override
        public void onCommitCompletion(CompletionInfo text) {
            clearComposingText();
            setText(text.getText());
            setSelection(getText().length());
        }

        void setInnerFocusable(boolean focusable) {
            setFocusableInTouchMode(focusable);
            setFocusable(focusable);
            setCursorVisible(focusable);
            if (focusable) {
                requestFocus();
                setBackground(this.mBackground);
            } else {
                setBackground(null);
            }
        }
    }
}
