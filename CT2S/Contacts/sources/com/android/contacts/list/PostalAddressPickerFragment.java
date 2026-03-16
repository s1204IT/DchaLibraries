package com.android.contacts.list;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactEntryListFragment;

public class PostalAddressPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> {
    private OnPostalAddressPickerActionListener mListener;

    public PostalAddressPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(3);
    }

    public void setOnPostalAddressPickerActionListener(OnPostalAddressPickerActionListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onItemClick(int position, long id) {
        if (getAdapter().getItem(position) != null) {
            if (!isLegacyCompatibilityMode()) {
                PostalAddressListAdapter adapter = (PostalAddressListAdapter) getAdapter();
                pickPostalAddress(adapter.getDataUri(position));
            } else {
                LegacyPostalAddressListAdapter adapter2 = (LegacyPostalAddressListAdapter) getAdapter();
                pickPostalAddress(adapter2.getContactMethodUri(position));
            }
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            PostalAddressListAdapter adapter = new PostalAddressListAdapter(getActivity());
            adapter.setSectionHeaderDisplayEnabled(true);
            adapter.setDisplayPhotos(true);
            return adapter;
        }
        LegacyPostalAddressListAdapter adapter2 = new LegacyPostalAddressListAdapter(getActivity());
        adapter2.setSectionHeaderDisplayEnabled(false);
        adapter2.setDisplayPhotos(false);
        return adapter2;
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

    private void pickPostalAddress(Uri uri) {
        this.mListener.onPickPostalAddressAction(uri);
    }
}
