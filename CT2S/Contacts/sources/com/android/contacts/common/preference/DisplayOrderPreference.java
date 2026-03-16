package com.android.contacts.common.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import com.android.contacts.R;

public final class DisplayOrderPreference extends ListPreference {
    private Context mContext;
    private ContactsPreferences mPreferences;

    public DisplayOrderPreference(Context context) {
        super(context);
        prepare();
    }

    public DisplayOrderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        prepare();
    }

    private void prepare() {
        this.mContext = getContext();
        this.mPreferences = new ContactsPreferences(this.mContext);
        setEntries(new String[]{this.mContext.getString(R.string.display_options_view_given_name_first), this.mContext.getString(R.string.display_options_view_family_name_first)});
        setEntryValues(new String[]{String.valueOf(1), String.valueOf(2)});
        setValue(String.valueOf(this.mPreferences.getDisplayOrder()));
    }

    @Override
    protected boolean shouldPersist() {
        return false;
    }

    @Override
    public CharSequence getSummary() {
        switch (this.mPreferences.getDisplayOrder()) {
            case 1:
                return this.mContext.getString(R.string.display_options_view_given_name_first);
            case 2:
                return this.mContext.getString(R.string.display_options_view_family_name_first);
            default:
                return null;
        }
    }

    @Override
    protected boolean persistString(String value) {
        int newValue = Integer.parseInt(value);
        if (newValue != this.mPreferences.getDisplayOrder()) {
            this.mPreferences.setDisplayOrder(newValue);
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
