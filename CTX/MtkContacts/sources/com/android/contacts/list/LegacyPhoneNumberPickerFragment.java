package com.android.contacts.list;

import android.net.Uri;

public class LegacyPhoneNumberPickerFragment extends PhoneNumberPickerFragment {
    @Override
    protected boolean getVisibleScrollbarEnabled() {
        return false;
    }

    @Override
    protected Uri getPhoneUri(int i) {
        return ((LegacyPhoneNumberListAdapter) getAdapter()).getPhoneUri(i);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        LegacyPhoneNumberListAdapter legacyPhoneNumberListAdapter = new LegacyPhoneNumberListAdapter(getActivity());
        legacyPhoneNumberListAdapter.setDisplayPhotos(true);
        return legacyPhoneNumberListAdapter;
    }

    @Override
    protected void setPhotoPosition(ContactEntryListAdapter contactEntryListAdapter) {
    }

    @Override
    protected void startPhoneNumberShortcutIntent(Uri uri, boolean z) {
        throw new UnsupportedOperationException();
    }
}
