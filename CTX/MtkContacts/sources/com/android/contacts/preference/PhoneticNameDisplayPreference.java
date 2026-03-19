package com.android.contacts.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import com.android.contacts.R;

public final class PhoneticNameDisplayPreference extends ListPreference {
    private Context mContext;
    private ContactsPreferences mPreferences;

    public PhoneticNameDisplayPreference(Context context) {
        super(context);
        prepare();
    }

    public PhoneticNameDisplayPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        prepare();
    }

    private void prepare() {
        this.mContext = getContext();
        this.mPreferences = new ContactsPreferences(this.mContext);
        setEntries(new String[]{this.mContext.getString(R.string.editor_options_always_show_phonetic_names), this.mContext.getString(R.string.editor_options_hide_phonetic_names_if_empty)});
        setEntryValues(new String[]{String.valueOf(0), String.valueOf(1)});
        setValue(String.valueOf(this.mPreferences.getPhoneticNameDisplayPreference()));
    }

    @Override
    protected boolean shouldPersist() {
        return false;
    }

    @Override
    public CharSequence getSummary() {
        switch (this.mPreferences.getPhoneticNameDisplayPreference()) {
            case 0:
                return this.mContext.getString(R.string.editor_options_always_show_phonetic_names);
            case 1:
                return this.mContext.getString(R.string.editor_options_hide_phonetic_names_if_empty);
            default:
                return null;
        }
    }

    @Override
    protected boolean persistString(String str) {
        int i = Integer.parseInt(str);
        if (i != this.mPreferences.getPhoneticNameDisplayPreference()) {
            this.mPreferences.setPhoneticNameDisplayPreference(i);
            notifyChanged();
            return true;
        }
        return true;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNegativeButton((CharSequence) null, (DialogInterface.OnClickListener) null);
    }
}
