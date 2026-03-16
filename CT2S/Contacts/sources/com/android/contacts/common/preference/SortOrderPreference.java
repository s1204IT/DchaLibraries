package com.android.contacts.common.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import com.android.contacts.R;

public final class SortOrderPreference extends ListPreference {
    private Context mContext;
    private ContactsPreferences mPreferences;

    public SortOrderPreference(Context context) {
        super(context);
        prepare();
    }

    public SortOrderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        prepare();
    }

    private void prepare() {
        this.mContext = getContext();
        this.mPreferences = new ContactsPreferences(this.mContext);
        setEntries(new String[]{this.mContext.getString(R.string.display_options_sort_by_given_name), this.mContext.getString(R.string.display_options_sort_by_family_name)});
        setEntryValues(new String[]{String.valueOf(1), String.valueOf(2)});
        setValue(String.valueOf(this.mPreferences.getSortOrder()));
    }

    @Override
    protected boolean shouldPersist() {
        return false;
    }

    @Override
    public CharSequence getSummary() {
        switch (this.mPreferences.getSortOrder()) {
            case 1:
                return this.mContext.getString(R.string.display_options_sort_by_given_name);
            case 2:
                return this.mContext.getString(R.string.display_options_sort_by_family_name);
            default:
                return null;
        }
    }

    @Override
    protected boolean persistString(String value) {
        int newValue = Integer.parseInt(value);
        if (newValue != this.mPreferences.getSortOrder()) {
            this.mPreferences.setSortOrder(newValue);
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
