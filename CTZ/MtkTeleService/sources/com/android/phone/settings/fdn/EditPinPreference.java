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

    public interface OnPinEnteredListener {
        void onPinEntered(EditPinPreference editPinPreference, boolean z);
    }

    public void setOnPinEnteredListener(OnPinEnteredListener onPinEnteredListener) {
        this.mPinListener = onPinEnteredListener;
    }

    public EditPinPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public EditPinPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected View onCreateDialogView() {
        setDialogLayoutResource(R.layout.pref_dialog_editpin);
        View viewOnCreateDialogView = super.onCreateDialogView();
        getEditText().setInputType(18);
        return viewOnCreateDialogView;
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
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        if (this.mPinListener != null) {
            this.mPinListener.onPinEntered(this, z);
        }
    }

    public void showPinDialog() {
        showDialog(null);
    }
}
