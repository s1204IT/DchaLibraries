package com.android.contacts.list;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;

public class PostalAddressPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> {
    private OnPostalAddressPickerActionListener mListener;

    public PostalAddressPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(3);
    }

    public void setOnPostalAddressPickerActionListener(OnPostalAddressPickerActionListener onPostalAddressPickerActionListener) {
        this.mListener = onPostalAddressPickerActionListener;
    }

    @Override
    protected void onItemClick(int i, long j) {
        if (getAdapter().getItem(i) == null) {
            return;
        }
        if (!isLegacyCompatibilityMode()) {
            pickPostalAddress(((PostalAddressListAdapter) getAdapter()).getDataUri(i));
        } else {
            pickPostalAddress(((LegacyPostalAddressListAdapter) getAdapter()).getContactMethodUri(i));
        }
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        if (!isLegacyCompatibilityMode()) {
            PostalAddressListAdapter postalAddressListAdapter = new PostalAddressListAdapter(getActivity());
            postalAddressListAdapter.setSectionHeaderDisplayEnabled(true);
            postalAddressListAdapter.setDisplayPhotos(true);
            return postalAddressListAdapter;
        }
        LegacyPostalAddressListAdapter legacyPostalAddressListAdapter = new LegacyPostalAddressListAdapter(getActivity());
        legacyPostalAddressListAdapter.setSectionHeaderDisplayEnabled(false);
        legacyPostalAddressListAdapter.setDisplayPhotos(false);
        return legacyPostalAddressListAdapter;
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        setVisibleScrollbarEnabled(!isLegacyCompatibilityMode());
    }

    private void pickPostalAddress(Uri uri) {
        this.mListener.onPickPostalAddressAction(uri);
    }
}
