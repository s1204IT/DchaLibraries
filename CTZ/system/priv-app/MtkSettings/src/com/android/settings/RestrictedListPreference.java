package com.android.settings;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v14.preference.ListPreferenceDialogFragment;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import com.android.settings.CustomListPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class RestrictedListPreference extends CustomListPreference {
    private final RestrictedPreferenceHelper mHelper;
    private int mProfileUserId;
    private boolean mRequiresActiveUnlockedProfile;
    private final List<RestrictedItem> mRestrictedItems;

    public RestrictedListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRestrictedItems = new ArrayList();
        this.mRequiresActiveUnlockedProfile = false;
        setWidgetLayoutResource(R.layout.restricted_icon);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attributeSet);
    }

    public RestrictedListPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mRestrictedItems = new ArrayList();
        this.mRequiresActiveUnlockedProfile = false;
        this.mHelper = new RestrictedPreferenceHelper(context, this, attributeSet);
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mHelper.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(R.id.restricted_icon);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(isDisabledByAdmin() ? 0 : 8);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void performClick() {
        if (this.mRequiresActiveUnlockedProfile) {
            if (Utils.startQuietModeDialogIfNecessary(getContext(), UserManager.get(getContext()), this.mProfileUserId)) {
                return;
            }
            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService("keyguard");
            if (keyguardManager.isDeviceLocked(this.mProfileUserId)) {
                getContext().startActivity(keyguardManager.createConfirmDeviceCredentialIntent(null, null, this.mProfileUserId));
                return;
            }
        }
        if (!this.mHelper.performClick()) {
            super.performClick();
        }
    }

    @Override // android.support.v7.preference.Preference
    public void setEnabled(boolean z) {
        if (z && isDisabledByAdmin()) {
            this.mHelper.setDisabledByAdmin(null);
        } else {
            super.setEnabled(z);
        }
    }

    @Override // android.support.v7.preference.Preference
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        if (this.mHelper.setDisabledByAdmin(enforcedAdmin)) {
            notifyChanged();
        }
    }

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
    }

    public void setRequiresActiveUnlockedProfile(boolean z) {
        this.mRequiresActiveUnlockedProfile = z;
    }

    public void setProfileUserId(int i) {
        this.mProfileUserId = i;
    }

    public boolean isRestrictedForEntry(CharSequence charSequence) {
        if (charSequence == null) {
            return false;
        }
        Iterator<RestrictedItem> it = this.mRestrictedItems.iterator();
        while (it.hasNext()) {
            if (charSequence.equals(it.next().entry)) {
                return true;
            }
        }
        return false;
    }

    public void addRestrictedItem(RestrictedItem restrictedItem) {
        this.mRestrictedItems.add(restrictedItem);
    }

    public void clearRestrictedItems() {
        this.mRestrictedItems.clear();
    }

    private RestrictedItem getRestrictedItemForEntryValue(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        for (RestrictedItem restrictedItem : this.mRestrictedItems) {
            if (charSequence.equals(restrictedItem.entryValue)) {
                return restrictedItem;
            }
        }
        return null;
    }

    protected ListAdapter createListAdapter() {
        return new RestrictedArrayAdapter(getContext(), getEntries(), getSelectedValuePos());
    }

    public int getSelectedValuePos() {
        String value = getValue();
        if (value == null) {
            return -1;
        }
        return findIndexOfValue(value);
    }

    @Override // com.android.settings.CustomListPreference
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        builder.setAdapter(createListAdapter(), onClickListener);
    }

    public class RestrictedArrayAdapter extends ArrayAdapter<CharSequence> {
        private final int mSelectedIndex;

        public RestrictedArrayAdapter(Context context, CharSequence[] charSequenceArr, int i) {
            super(context, R.layout.restricted_dialog_singlechoice, R.id.text1, charSequenceArr);
            this.mSelectedIndex = i;
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            View view2 = super.getView(i, view, viewGroup);
            CharSequence item = getItem(i);
            CheckedTextView checkedTextView = (CheckedTextView) view2.findViewById(R.id.text1);
            ImageView imageView = (ImageView) view2.findViewById(R.id.restricted_lock_icon);
            if (RestrictedListPreference.this.isRestrictedForEntry(item)) {
                checkedTextView.setEnabled(false);
                checkedTextView.setChecked(false);
                imageView.setVisibility(0);
            } else {
                if (this.mSelectedIndex != -1) {
                    checkedTextView.setChecked(i == this.mSelectedIndex);
                }
                if (!checkedTextView.isEnabled()) {
                    checkedTextView.setEnabled(true);
                }
                imageView.setVisibility(8);
            }
            return view2;
        }

        @Override // android.widget.BaseAdapter, android.widget.Adapter
        public boolean hasStableIds() {
            return true;
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public long getItemId(int i) {
            return i;
        }
    }

    public static class RestrictedListPreferenceDialogFragment extends CustomListPreference.CustomListPreferenceDialogFragment {
        private int mLastCheckedPosition = -1;

        public static ListPreferenceDialogFragment newInstance(String str) {
            RestrictedListPreferenceDialogFragment restrictedListPreferenceDialogFragment = new RestrictedListPreferenceDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", str);
            restrictedListPreferenceDialogFragment.setArguments(bundle);
            return restrictedListPreferenceDialogFragment;
        }

        private RestrictedListPreference getCustomizablePreference() {
            return (RestrictedListPreference) getPreference();
        }

        @Override // com.android.settings.CustomListPreference.CustomListPreferenceDialogFragment
        protected DialogInterface.OnClickListener getOnItemClickListener() {
            return new DialogInterface.OnClickListener() { // from class: com.android.settings.RestrictedListPreference.RestrictedListPreferenceDialogFragment.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    RestrictedListPreference customizablePreference = RestrictedListPreferenceDialogFragment.this.getCustomizablePreference();
                    if (i >= 0 && i < customizablePreference.getEntryValues().length) {
                        RestrictedItem restrictedItemForEntryValue = customizablePreference.getRestrictedItemForEntryValue(customizablePreference.getEntryValues()[i].toString());
                        if (restrictedItemForEntryValue != null) {
                            ((AlertDialog) dialogInterface).getListView().setItemChecked(RestrictedListPreferenceDialogFragment.this.getLastCheckedPosition(), true);
                            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(RestrictedListPreferenceDialogFragment.this.getContext(), restrictedItemForEntryValue.enforcedAdmin);
                        } else {
                            RestrictedListPreferenceDialogFragment.this.setClickedDialogEntryIndex(i);
                        }
                        if (RestrictedListPreferenceDialogFragment.this.getCustomizablePreference().isAutoClosePreference()) {
                            RestrictedListPreferenceDialogFragment.this.onClick(dialogInterface, -1);
                            dialogInterface.dismiss();
                        }
                    }
                }
            };
        }

        private int getLastCheckedPosition() {
            if (this.mLastCheckedPosition == -1) {
                this.mLastCheckedPosition = getCustomizablePreference().getSelectedValuePos();
            }
            return this.mLastCheckedPosition;
        }

        @Override // com.android.settings.CustomListPreference.CustomListPreferenceDialogFragment
        protected void setClickedDialogEntryIndex(int i) {
            super.setClickedDialogEntryIndex(i);
            this.mLastCheckedPosition = i;
        }
    }

    public static class RestrictedItem {
        public final RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public final CharSequence entry;
        public final CharSequence entryValue;

        public RestrictedItem(CharSequence charSequence, CharSequence charSequence2, RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
            this.entry = charSequence;
            this.entryValue = charSequence2;
            this.enforcedAdmin = enforcedAdmin;
        }
    }
}
