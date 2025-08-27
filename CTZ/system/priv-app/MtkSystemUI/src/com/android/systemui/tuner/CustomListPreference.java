package com.android.systemui.tuner;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v14.preference.ListPreferenceDialogFragment;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

/* loaded from: classes.dex */
public class CustomListPreference extends ListPreference {
    public CustomListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CustomListPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
    }

    protected void onDialogClosed(boolean z) {
    }

    protected Dialog onDialogCreated(DialogFragment dialogFragment, Dialog dialog) {
        return dialog;
    }

    protected boolean isAutoClosePreference() {
        return true;
    }

    protected void onDialogStateRestored(DialogFragment dialogFragment, Dialog dialog, Bundle bundle) {
    }

    public static class CustomListPreferenceDialogFragment extends ListPreferenceDialogFragment {
        private int mClickedDialogEntryIndex;

        public static ListPreferenceDialogFragment newInstance(String str) {
            CustomListPreferenceDialogFragment customListPreferenceDialogFragment = new CustomListPreferenceDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", str);
            customListPreferenceDialogFragment.setArguments(bundle);
            return customListPreferenceDialogFragment;
        }

        public CustomListPreference getCustomizablePreference() {
            return (CustomListPreference) getPreference();
        }

        @Override // android.support.v14.preference.ListPreferenceDialogFragment, android.support.v14.preference.PreferenceDialogFragment
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            this.mClickedDialogEntryIndex = getCustomizablePreference().findIndexOfValue(getCustomizablePreference().getValue());
            getCustomizablePreference().onPrepareDialogBuilder(builder, getOnItemClickListener());
            if (!getCustomizablePreference().isAutoClosePreference()) {
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() { // from class: com.android.systemui.tuner.CustomListPreference.CustomListPreferenceDialogFragment.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        CustomListPreferenceDialogFragment.this.onItemConfirmed();
                    }
                });
            }
        }

        @Override // android.support.v14.preference.PreferenceDialogFragment, android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Dialog dialogOnCreateDialog = super.onCreateDialog(bundle);
            if (bundle != null) {
                this.mClickedDialogEntryIndex = bundle.getInt("settings.CustomListPrefDialog.KEY_CLICKED_ENTRY_INDEX", this.mClickedDialogEntryIndex);
            }
            return getCustomizablePreference().onDialogCreated(this, dialogOnCreateDialog);
        }

        @Override // android.support.v14.preference.ListPreferenceDialogFragment, android.support.v14.preference.PreferenceDialogFragment, android.app.DialogFragment, android.app.Fragment
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putInt("settings.CustomListPrefDialog.KEY_CLICKED_ENTRY_INDEX", this.mClickedDialogEntryIndex);
        }

        @Override // android.app.DialogFragment, android.app.Fragment
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            getCustomizablePreference().onDialogStateRestored(this, getDialog(), bundle);
        }

        protected DialogInterface.OnClickListener getOnItemClickListener() {
            return new DialogInterface.OnClickListener() { // from class: com.android.systemui.tuner.CustomListPreference.CustomListPreferenceDialogFragment.2
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    CustomListPreferenceDialogFragment.this.setClickedDialogEntryIndex(i);
                    if (CustomListPreferenceDialogFragment.this.getCustomizablePreference().isAutoClosePreference()) {
                        CustomListPreferenceDialogFragment.this.onItemConfirmed();
                    }
                }
            };
        }

        protected void setClickedDialogEntryIndex(int i) {
            this.mClickedDialogEntryIndex = i;
        }

        private String getValue() {
            CustomListPreference customizablePreference = getCustomizablePreference();
            if (this.mClickedDialogEntryIndex >= 0 && customizablePreference.getEntryValues() != null) {
                return customizablePreference.getEntryValues()[this.mClickedDialogEntryIndex].toString();
            }
            return null;
        }

        protected void onItemConfirmed() {
            onClick(getDialog(), -1);
            getDialog().dismiss();
        }

        @Override // android.support.v14.preference.ListPreferenceDialogFragment, android.support.v14.preference.PreferenceDialogFragment
        public void onDialogClosed(boolean z) {
            getCustomizablePreference().onDialogClosed(z);
            CustomListPreference customizablePreference = getCustomizablePreference();
            String value = getValue();
            if (z && value != null && customizablePreference.callChangeListener(value)) {
                customizablePreference.setValue(value);
            }
        }
    }
}
