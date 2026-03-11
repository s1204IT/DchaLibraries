package com.android.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v14.preference.ListPreferenceDialogFragment;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.settings.CustomListPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;
import java.util.ArrayList;
import java.util.List;

public class RestrictedListPreference extends CustomListPreference {
    private final RestrictedPreferenceHelper mHelper;
    private final List<RestrictedItem> mRestrictedItems;

    public static class RestrictedItem {
        public final RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public final CharSequence entry;
        public final CharSequence entryValue;
    }

    public RestrictedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRestrictedItems = new ArrayList();
        this.mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    public RestrictedListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mRestrictedItems = new ArrayList();
        this.mHelper = new RestrictedPreferenceHelper(context, this, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        this.mHelper.onBindViewHolder(holder);
    }

    @Override
    public void performClick() {
        if (this.mHelper.performClick()) {
            return;
        }
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

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
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

    protected ListAdapter createListAdapter() {
        return new RestrictedArrayAdapter(getContext(), getEntries(), getSelectedValuePos());
    }

    public int getSelectedValuePos() {
        String selectedValue = getValue();
        if (selectedValue == null) {
            return -1;
        }
        int selectedIndex = findIndexOfValue(selectedValue);
        return selectedIndex;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        builder.setAdapter(createListAdapter(), listener);
    }

    public class RestrictedArrayAdapter extends ArrayAdapter<CharSequence> {
        private final int mSelectedIndex;

        public RestrictedArrayAdapter(Context context, CharSequence[] objects, int selectedIndex) {
            super(context, R.layout.restricted_dialog_singlechoice, R.id.text1, objects);
            this.mSelectedIndex = selectedIndex;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View root = super.getView(position, convertView, parent);
            CharSequence entry = getItem(position);
            CheckedTextView text = (CheckedTextView) root.findViewById(R.id.text1);
            ImageView padlock = (ImageView) root.findViewById(R.id.restricted_lock_icon);
            if (RestrictedListPreference.this.isRestrictedForEntry(entry)) {
                text.setEnabled(false);
                text.setChecked(false);
                padlock.setVisibility(0);
            } else {
                if (this.mSelectedIndex != -1) {
                    text.setChecked(position == this.mSelectedIndex);
                }
                if (!text.isEnabled()) {
                    text.setEnabled(true);
                }
                padlock.setVisibility(8);
            }
            return root;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    public static class RestrictedListPreferenceDialogFragment extends CustomListPreference.CustomListPreferenceDialogFragment {
        private int mLastCheckedPosition = -1;

        public static ListPreferenceDialogFragment newInstance(String key) {
            ListPreferenceDialogFragment fragment = new RestrictedListPreferenceDialogFragment();
            Bundle b = new Bundle(1);
            b.putString("key", key);
            fragment.setArguments(b);
            return fragment;
        }

        public RestrictedListPreference getCustomizablePreference() {
            return (RestrictedListPreference) getPreference();
        }

        @Override
        protected DialogInterface.OnClickListener getOnItemClickListener() {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RestrictedListPreference preference = RestrictedListPreferenceDialogFragment.this.getCustomizablePreference();
                    if (which < 0 || which >= preference.getEntryValues().length) {
                        return;
                    }
                    String entryValue = preference.getEntryValues()[which].toString();
                    RestrictedItem item = preference.getRestrictedItemForEntryValue(entryValue);
                    if (item != null) {
                        ListView listView = ((AlertDialog) dialog).getListView();
                        listView.setItemChecked(RestrictedListPreferenceDialogFragment.this.getLastCheckedPosition(), true);
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(RestrictedListPreferenceDialogFragment.this.getContext(), item.enforcedAdmin);
                    } else {
                        RestrictedListPreferenceDialogFragment.this.setClickedDialogEntryIndex(which);
                    }
                    if (!RestrictedListPreferenceDialogFragment.this.getCustomizablePreference().isAutoClosePreference()) {
                        return;
                    }
                    RestrictedListPreferenceDialogFragment.this.onClick(dialog, -1);
                    dialog.dismiss();
                }
            };
        }

        public int getLastCheckedPosition() {
            if (this.mLastCheckedPosition == -1) {
                this.mLastCheckedPosition = getCustomizablePreference().getSelectedValuePos();
            }
            return this.mLastCheckedPosition;
        }

        @Override
        protected void setClickedDialogEntryIndex(int which) {
            super.setClickedDialogEntryIndex(which);
            this.mLastCheckedPosition = which;
        }
    }
}
