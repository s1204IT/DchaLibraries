package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;
import java.util.ArrayList;
import java.util.List;

public class RestrictedDropDownPreference extends DropDownPreference {
    private final RestrictedPreferenceHelper mHelper;
    private final AdapterView.OnItemSelectedListener mItemSelectedListener;
    private Preference.OnPreferenceClickListener mPreClickListener;
    private List<RestrictedItem> mRestrictedItems;
    private ReselectionSpinner mSpinner;
    private boolean mUserClicked;

    public RestrictedDropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRestrictedItems = new ArrayList();
        this.mUserClicked = false;
        this.mItemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (RestrictedDropDownPreference.this.mUserClicked) {
                    RestrictedDropDownPreference.this.mUserClicked = false;
                    if (position < 0 || position >= RestrictedDropDownPreference.this.getEntryValues().length) {
                        return;
                    }
                    String value = RestrictedDropDownPreference.this.getEntryValues()[position].toString();
                    RestrictedItem item = RestrictedDropDownPreference.this.getRestrictedItemForEntryValue(value);
                    if (item != null) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(RestrictedDropDownPreference.this.getContext(), item.enforcedAdmin);
                        RestrictedDropDownPreference.this.mSpinner.setSelection(RestrictedDropDownPreference.this.findIndexOfValue(RestrictedDropDownPreference.this.getValue()));
                    } else {
                        if (value.equals(RestrictedDropDownPreference.this.getValue()) || !RestrictedDropDownPreference.this.callChangeListener(value)) {
                            return;
                        }
                        RestrictedDropDownPreference.this.setValue(value);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        setLayoutResource(R.layout.restricted_preference_dropdown);
        setWidgetLayoutResource(R.layout.restricted_icon);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    @Override
    protected ArrayAdapter createAdapter() {
        return new RestrictedArrayItemAdapter(getContext());
    }

    @Override
    public void setValue(String value) {
        if (getRestrictedItemForEntryValue(value) != null) {
            return;
        }
        super.setValue(value);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        this.mSpinner = (ReselectionSpinner) view.itemView.findViewById(R.id.spinner);
        this.mSpinner.setPreference(this);
        super.onBindViewHolder(view);
        this.mHelper.onBindViewHolder(view);
        this.mSpinner.setOnItemSelectedListener(this.mItemSelectedListener);
        View restrictedIcon = view.findViewById(R.id.restricted_icon);
        if (restrictedIcon == null) {
            return;
        }
        restrictedIcon.setVisibility(isDisabledByAdmin() ? 0 : 8);
    }

    public boolean isRestrictedForEntry(CharSequence entry) {
        if (entry == null) {
            return false;
        }
        for (RestrictedItem item : this.mRestrictedItems) {
            if (entry.equals(item.entry)) {
                return true;
            }
        }
        return false;
    }

    public RestrictedItem getRestrictedItemForEntryValue(CharSequence entryValue) {
        if (entryValue == null) {
            return null;
        }
        for (RestrictedItem item : this.mRestrictedItems) {
            if (entryValue.equals(item.entryValue)) {
                return item;
            }
        }
        return null;
    }

    public RestrictedItem getRestrictedItemForPosition(int position) {
        if (position < 0 || position >= getEntryValues().length) {
            return null;
        }
        CharSequence entryValue = getEntryValues()[position];
        return getRestrictedItemForEntryValue(entryValue);
    }

    public void addRestrictedItem(RestrictedItem item) {
        this.mRestrictedItems.add(item);
    }

    public void clearRestrictedItems() {
        this.mRestrictedItems.clear();
    }

    @Override
    public void performClick() {
        if ((this.mPreClickListener != null && this.mPreClickListener.onPreferenceClick(this)) || this.mHelper.performClick()) {
            return;
        }
        this.mUserClicked = true;
        super.performClick();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && isDisabledByAdmin()) {
            this.mHelper.setDisabledByAdmin(null);
        } else {
            super.setEnabled(enabled);
        }
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        if (!this.mHelper.setDisabledByAdmin(admin)) {
            return;
        }
        notifyChanged();
    }

    public void setOnPreClickListener(Preference.OnPreferenceClickListener l) {
        this.mPreClickListener = l;
    }

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
    }

    public void setUserClicked(boolean userClicked) {
        this.mUserClicked = userClicked;
    }

    public boolean isUserClicked() {
        return this.mUserClicked;
    }

    private class RestrictedArrayItemAdapter extends ArrayAdapter<String> {
        public RestrictedArrayItemAdapter(Context context) {
            super(context, R.layout.spinner_dropdown_restricted_item, android.R.id.text1);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rootView = super.getView(position, convertView, parent);
            CharSequence entry = getItem(position);
            boolean isEntryRestricted = RestrictedDropDownPreference.this.isRestrictedForEntry(entry);
            TextView text = (TextView) rootView.findViewById(android.R.id.text1);
            if (text != null) {
                text.setEnabled(!isEntryRestricted);
            }
            View restrictedIcon = rootView.findViewById(R.id.restricted_icon);
            if (restrictedIcon != null) {
                restrictedIcon.setVisibility(isEntryRestricted ? 0 : 8);
            }
            return rootView;
        }
    }

    public static class ReselectionSpinner extends Spinner {
        private RestrictedDropDownPreference pref;

        public ReselectionSpinner(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void setPreference(RestrictedDropDownPreference pref) {
            this.pref = pref;
        }

        @Override
        public void setSelection(int position) {
            int previousSelectedPosition = getSelectedItemPosition();
            super.setSelection(position);
            if (position != previousSelectedPosition || !this.pref.isUserClicked()) {
                return;
            }
            this.pref.setUserClicked(false);
            RestrictedItem item = this.pref.getRestrictedItemForPosition(position);
            if (item == null) {
                return;
            }
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), item.enforcedAdmin);
        }
    }

    public static class RestrictedItem {
        public final RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public final CharSequence entry;
        public final CharSequence entryValue;

        public RestrictedItem(CharSequence entry, CharSequence entryValue, RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
            this.entry = entry;
            this.entryValue = entryValue;
            this.enforcedAdmin = enforcedAdmin;
        }
    }
}
