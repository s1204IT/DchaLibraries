package com.android.contacts.list;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.ShortcutIntentBuilder;
import com.mediatek.contacts.util.Log;

public class ContactPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> implements ShortcutIntentBuilder.OnShortcutIntentCreatedListener {
    private int mAccountType = 0;
    private boolean mCreateContactEnabled;
    private boolean mEditMode;
    private OnContactPickerActionListener mListener;
    private boolean mShortcutRequested;

    public ContactPickerFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setQuickContactEnabled(false);
        setDirectorySearchMode(2);
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener onContactPickerActionListener) {
        this.mListener = onContactPickerActionListener;
    }

    public void setCreateContactEnabled(boolean z) {
        this.mCreateContactEnabled = z;
    }

    public void setEditMode(boolean z) {
        this.mEditMode = z;
    }

    public void setAccountType(int i) {
        this.mAccountType = i;
    }

    public void setShortcutRequested(boolean z) {
        this.mShortcutRequested = z;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("editMode", this.mEditMode);
        bundle.putBoolean("createContactEnabled", this.mCreateContactEnabled);
        bundle.putBoolean("shortcutRequested", this.mShortcutRequested);
        bundle.putInt("account_type", this.mAccountType);
    }

    @Override
    public void restoreSavedState(Bundle bundle) {
        super.restoreSavedState(bundle);
        if (bundle == null) {
            return;
        }
        this.mEditMode = bundle.getBoolean("editMode");
        this.mCreateContactEnabled = bundle.getBoolean("createContactEnabled");
        this.mShortcutRequested = bundle.getBoolean("shortcutRequested");
        this.mAccountType = bundle.getInt("account_type");
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        setEmptyView();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (i == 0 && this.mCreateContactEnabled && this.mListener != null) {
            this.mListener.onCreateNewContactAction();
        } else {
            super.onItemClick(adapterView, view, i, j);
        }
    }

    @Override
    protected void onItemClick(int i, long j) {
        Uri contactUri;
        if (isLegacyCompatibilityMode()) {
            contactUri = ((LegacyContactListAdapter) getAdapter()).getPersonUri(i);
        } else {
            contactUri = ((ContactListAdapter) getAdapter()).getContactUri(i);
        }
        if (contactUri == null) {
            return;
        }
        if (this.mEditMode) {
            editContact(contactUri);
        } else if (this.mShortcutRequested) {
            new ShortcutIntentBuilder(getActivity(), this).createContactShortcutIntent(contactUri);
        } else {
            pickContact(contactUri);
        }
    }

    public void editContact(Uri uri) {
        if (this.mListener != null) {
            this.mListener.onEditContactAction(uri);
        }
    }

    public void pickContact(Uri uri) {
        if (this.mListener != null) {
            this.mListener.onPickContactAction(uri);
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            HeaderEntryContactListAdapter headerEntryContactListAdapter = new HeaderEntryContactListAdapter(getActivity());
            headerEntryContactListAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
            setOnlyShowPhoneContacts(headerEntryContactListAdapter, true);
            headerEntryContactListAdapter.setSectionHeaderDisplayEnabled(true);
            headerEntryContactListAdapter.setDisplayPhotos(true);
            headerEntryContactListAdapter.setQuickContactEnabled(false);
            headerEntryContactListAdapter.setShowCreateContact(this.mCreateContactEnabled);
            return headerEntryContactListAdapter;
        }
        LegacyContactListAdapter legacyContactListAdapter = new LegacyContactListAdapter(getActivity());
        legacyContactListAdapter.setSectionHeaderDisplayEnabled(false);
        legacyContactListAdapter.setDisplayPhotos(false);
        return legacyContactListAdapter;
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.contact_picker_content, (ViewGroup) null);
    }

    @Override
    public void onShortcutIntentCreated(Uri uri, Intent intent) {
        if (this.mListener != null) {
            this.mListener.onShortcutIntentCreated(intent);
        }
    }

    @Override
    public void onPickerResult(Intent intent) {
        if (this.mListener != null) {
            this.mListener.onPickContactAction(intent.getData());
        }
    }

    private void setEmptyView() {
        TextView textView = (TextView) getView().findViewById(R.id.contact_list_empty);
        if (textView != null) {
            textView.setText(R.string.listFoundAllContactsZero);
        }
    }

    private void setOnlyShowPhoneContacts(HeaderEntryContactListAdapter headerEntryContactListAdapter, boolean z) {
        if (this.mEditMode || this.mShortcutRequested || this.mAccountType == 1) {
            Log.d("ContactPickerFragment", "[setOnlyShowPhoneContacts] only show phone contact");
            headerEntryContactListAdapter.setOnlyShowPhoneContacts(z);
        }
    }
}
