package com.android.settingslib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

/* loaded from: classes.dex */
public class CustomDialogPreference extends DialogPreference {
    private CustomPreferenceDialogFragment mFragment;
    private DialogInterface.OnShowListener mOnShowListener;

    public CustomDialogPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public CustomDialogPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public CustomDialogPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CustomDialogPreference(Context context) {
        super(context);
    }

    public Dialog getDialog() {
        if (this.mFragment != null) {
            return this.mFragment.getDialog();
        }
        return null;
    }

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener) {
        this.mOnShowListener = onShowListener;
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
    }

    protected void onDialogClosed(boolean z) {
    }

    protected void onClick(DialogInterface dialogInterface, int i) {
    }

    protected void onBindDialogView(View view) {
    }

    private void setFragment(CustomPreferenceDialogFragment customPreferenceDialogFragment) {
        this.mFragment = customPreferenceDialogFragment;
    }

    private DialogInterface.OnShowListener getOnShowListener() {
        return this.mOnShowListener;
    }

    public static class CustomPreferenceDialogFragment extends PreferenceDialogFragment {
        public static CustomPreferenceDialogFragment newInstance(String str) {
            CustomPreferenceDialogFragment customPreferenceDialogFragment = new CustomPreferenceDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", str);
            customPreferenceDialogFragment.setArguments(bundle);
            return customPreferenceDialogFragment;
        }

        private CustomDialogPreference getCustomizablePreference() {
            return (CustomDialogPreference) getPreference();
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            getCustomizablePreference().setFragment(this);
            getCustomizablePreference().onPrepareDialogBuilder(builder, this);
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment
        public void onDialogClosed(boolean z) {
            getCustomizablePreference().onDialogClosed(z);
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);
            getCustomizablePreference().onBindDialogView(view);
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment, android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Dialog dialogOnCreateDialog = super.onCreateDialog(bundle);
            dialogOnCreateDialog.setOnShowListener(getCustomizablePreference().getOnShowListener());
            return dialogOnCreateDialog;
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment, android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            super.onClick(dialogInterface, i);
            getCustomizablePreference().onClick(dialogInterface, i);
        }
    }
}
