package com.android.contacts.list;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import java.util.ArrayList;

public class ContactPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> implements ShortcutIntentBuilder.OnShortcutIntentCreatedListener {
    ArrayList<ContactListFilter> mAccountFilters;
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

    public ContactPickerFragment(ArrayList<ContactListFilter> accountFilters) {
        this.mAccountFilters = accountFilters;
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setQuickContactEnabled(false);
        setDirectorySearchMode(2);
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener listener) {
        this.mListener = listener;
    }

    public boolean isCreateContactEnabled() {
        return this.mCreateContactEnabled;
    }

    public void setCreateContactEnabled(boolean flag) {
        this.mCreateContactEnabled = flag;
    }

    public void setEditMode(boolean flag) {
        this.mEditMode = flag;
    }

    public void setShortcutRequested(boolean flag) {
        this.mShortcutRequested = flag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("editMode", this.mEditMode);
        outState.putBoolean("createContactEnabled", this.mCreateContactEnabled);
        outState.putBoolean("shortcutRequested", this.mShortcutRequested);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);
        if (savedState != null) {
            this.mEditMode = savedState.getBoolean("editMode");
            this.mCreateContactEnabled = savedState.getBoolean("createContactEnabled");
            this.mShortcutRequested = savedState.getBoolean("shortcutRequested");
        }
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        if (this.mCreateContactEnabled && isLegacyCompatibilityMode()) {
            getListView().addHeaderView(inflater.inflate(R.layout.create_new_contact, (ViewGroup) null, false));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0 && this.mCreateContactEnabled) {
            this.mListener.onCreateNewContactAction();
        } else {
            super.onItemClick(parent, view, position, id);
        }
    }

    @Override
    protected void onItemClick(int position, long id) {
        Uri uri;
        if (isLegacyCompatibilityMode()) {
            uri = ((LegacyContactListAdapter) getAdapter()).getPersonUri(position);
        } else {
            uri = ((ContactListAdapter) getAdapter()).getContactUri(position);
        }
        if (uri != null) {
            if (this.mEditMode) {
                editContact(uri);
            } else if (this.mShortcutRequested) {
                ShortcutIntentBuilder builder = new ShortcutIntentBuilder(getActivity(), this);
                builder.createContactShortcutIntent(uri);
            } else {
                pickContact(uri);
            }
        }
    }

    public void editContact(Uri contactUri) {
        this.mListener.onEditContactAction(contactUri);
    }

    public void pickContact(Uri uri) {
        this.mListener.onPickContactAction(uri);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            HeaderEntryContactListAdapter adapter = new HeaderEntryContactListAdapter(getActivity());
            if (this.mAccountFilters == null || this.mAccountFilters.isEmpty()) {
                adapter.setFilter(ContactListFilter.createFilterWithType(-2));
            } else {
                adapter.setFilters(this.mAccountFilters);
            }
            adapter.setSectionHeaderDisplayEnabled(true);
            adapter.setDisplayPhotos(true);
            adapter.setQuickContactEnabled(false);
            adapter.setShowCreateContact(this.mCreateContactEnabled);
            return adapter;
        }
        LegacyContactListAdapter adapter2 = new LegacyContactListAdapter(getActivity());
        adapter2.setSectionHeaderDisplayEnabled(false);
        adapter2.setDisplayPhotos(false);
        return adapter2;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        ContactEntryListAdapter adapter = getAdapter();
        adapter.setEmptyListEnabled(!isCreateContactEnabled());
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_picker_content, (ViewGroup) null);
    }

    @Override
    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
        this.mListener.onShortcutIntentCreated(shortcutIntent);
    }

    @Override
    public void onPickerResult(Intent data) {
        this.mListener.onPickContactAction(data.getData());
    }
}
