package com.android.contacts.list;

import android.net.Uri;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.PhoneNumberPickerFragment;

public class LegacyPhoneNumberPickerFragment extends PhoneNumberPickerFragment {
    private static final String TAG = LegacyPhoneNumberPickerFragment.class.getSimpleName();

    @Override
    protected boolean getVisibleScrollbarEnabled() {
        return false;
    }

    @Override
    protected Uri getPhoneUri(int position) {
        LegacyPhoneNumberListAdapter adapter = (LegacyPhoneNumberListAdapter) getAdapter();
        return adapter.getPhoneUri(position);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        LegacyPhoneNumberListAdapter adapter = new LegacyPhoneNumberListAdapter(getActivity());
        adapter.setDisplayPhotos(true);
        return adapter;
    }

    @Override
    protected void setPhotoPosition(ContactEntryListAdapter adapter) {
    }

    @Override
    protected void startPhoneNumberShortcutIntent(Uri uri) {
        throw new UnsupportedOperationException();
    }
}
