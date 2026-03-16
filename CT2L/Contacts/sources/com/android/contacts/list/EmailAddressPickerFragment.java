package com.android.contacts.list;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactEntryListFragment;

public class EmailAddressPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> {
    private OnEmailAddressPickerActionListener mListener;

    public EmailAddressPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(3);
    }

    public void setOnEmailAddressPickerActionListener(OnEmailAddressPickerActionListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onItemClick(int position, long id) {
        EmailAddressListAdapter adapter = (EmailAddressListAdapter) getAdapter();
        if (getAdapter().getItem(position) != null) {
            pickEmailAddress(adapter.getDataUri(position));
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        EmailAddressListAdapter adapter = new EmailAddressListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        setVisibleScrollbarEnabled(!isLegacyCompatibilityMode());
    }

    private void pickEmailAddress(Uri uri) {
        this.mListener.onPickEmailAddressAction(uri);
    }
}
