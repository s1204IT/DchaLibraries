package com.android.phone.settings.fdn;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import com.android.phone.R;

public class EditPinPreference extends EditTextPreference {
    private OnPinEnteredListener mPinListener;
    private boolean shouldHideButtons;

    interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference editPinPreference, boolean z);
    }

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        this.mPinListener = listener;
    }

    public EditPinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditPinPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateDialogView() {
        setDialogLayoutResource(R.layout.pref_dialog_editpin);
        View dialog = super.onCreateDialogView();
        getEditText().setInputType(18);
        return dialog;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.shouldHideButtons = view.findViewById(android.R.id.edit) == null;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        if (this.shouldHideButtons) {
            builder.setPositiveButton((CharSequence) null, this);
            builder.setNegativeButton((CharSequence) null, this);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (this.mPinListener != null) {
            this.mPinListener.onPinEntered(this, positiveResult);
        }
    }

    public void showPinDialog() {
        showDialog(null);
    }
}
