package com.android.contacts.list;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;

public class EmailAddressPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> {
    private OnEmailAddressPickerActionListener mListener;

    public EmailAddressPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(3);
    }

    public void setOnEmailAddressPickerActionListener(OnEmailAddressPickerActionListener onEmailAddressPickerActionListener) {
        this.mListener = onEmailAddressPickerActionListener;
    }

    @Override
    protected void onItemClick(int i, long j) {
        EmailAddressListAdapter emailAddressListAdapter = (EmailAddressListAdapter) getAdapter();
        if (getAdapter().getItem(i) == null) {
            return;
        }
        pickEmailAddress(emailAddressListAdapter.getDataUri(i));
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        EmailAddressListAdapter emailAddressListAdapter = new EmailAddressListAdapter(getActivity());
        emailAddressListAdapter.setSectionHeaderDisplayEnabled(true);
        emailAddressListAdapter.setDisplayPhotos(true);
        return emailAddressListAdapter;
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        Log.d("EmailAddressPickerFragment", "[onCreateView]");
        setVisibleScrollbarEnabled(!isLegacyCompatibilityMode());
    }

    private void pickEmailAddress(Uri uri) {
        this.mListener.onPickEmailAddressAction(uri);
    }
}
