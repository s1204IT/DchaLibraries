package com.android.settingslib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v14.preference.EditTextPreferenceDialogFragment;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

/* loaded from: classes.dex */
public class CustomEditTextPreference extends EditTextPreference {
    private CustomPreferenceDialogFragment mFragment;

    public CustomEditTextPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public CustomEditTextPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public CustomEditTextPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CustomEditTextPreference(Context context) {
        super(context);
    }

    public EditText getEditText() {
        Dialog dialog;
        if (this.mFragment != null && (dialog = this.mFragment.getDialog()) != null) {
            return (EditText) dialog.findViewById(android.R.id.edit);
        }
        return null;
    }

    public boolean isDialogOpen() {
        return getDialog() != null && getDialog().isShowing();
    }

    public Dialog getDialog() {
        if (this.mFragment != null) {
            return this.mFragment.getDialog();
        }
        return null;
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
    }

    protected void onDialogClosed(boolean z) {
    }

    protected void onClick(DialogInterface dialogInterface, int i) {
    }

    protected void onBindDialogView(View view) {
        EditText editText = (EditText) view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setInputType(16385);
            editText.requestFocus();
        }
    }

    private void setFragment(CustomPreferenceDialogFragment customPreferenceDialogFragment) {
        this.mFragment = customPreferenceDialogFragment;
    }

    public static class CustomPreferenceDialogFragment extends EditTextPreferenceDialogFragment {
        public static CustomPreferenceDialogFragment newInstance(String str) {
            CustomPreferenceDialogFragment customPreferenceDialogFragment = new CustomPreferenceDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", str);
            customPreferenceDialogFragment.setArguments(bundle);
            return customPreferenceDialogFragment;
        }

        private CustomEditTextPreference getCustomizablePreference() {
            return (CustomEditTextPreference) getPreference();
        }

        @Override // android.support.v14.preference.EditTextPreferenceDialogFragment, android.support.v14.preference.PreferenceDialogFragment
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);
            getCustomizablePreference().onBindDialogView(view);
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            getCustomizablePreference().setFragment(this);
            getCustomizablePreference().onPrepareDialogBuilder(builder, this);
        }

        @Override // android.support.v14.preference.EditTextPreferenceDialogFragment, android.support.v14.preference.PreferenceDialogFragment
        public void onDialogClosed(boolean z) {
            super.onDialogClosed(z);
            getCustomizablePreference().onDialogClosed(z);
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment, android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            super.onClick(dialogInterface, i);
            getCustomizablePreference().onClick(dialogInterface, i);
        }
    }
}
