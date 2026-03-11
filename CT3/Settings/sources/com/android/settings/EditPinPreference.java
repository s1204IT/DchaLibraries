package com.android.settings;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

class EditPinPreference extends CustomEditTextPreference {
    private Context mContext;
    private OnPinEnteredListener mPinListener;

    interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference editPinPreference, boolean z);
    }

    public EditPinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    public EditPinPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
    }

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        this.mPinListener = listener;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText editText = (EditText) view.findViewById(android.R.id.edit);
        if (editText == null) {
            return;
        }
        editText.setInputType(18);
        float padding = this.mContext.getResources().getDimension(R.dimen.pin_lock_padding);
        ViewParent parent = editText.getParent();
        if (parent == null) {
            return;
        }
        ((ViewGroup) parent).setPadding((int) padding, 0, (int) padding, 0);
    }

    public boolean isDialogOpen() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            return dialog.isShowing();
        }
        return false;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (this.mPinListener == null) {
            return;
        }
        this.mPinListener.onPinEntered(this, positiveResult);
    }

    public void showPinDialog() {
        Dialog dialog = getDialog();
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        onClick();
    }
}
