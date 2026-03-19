package com.mediatek.contacts.list;

import com.android.contacts.list.ContactListFilter;

public class PhoneNumbersPickerFragment extends DataKindBasePickerFragment {
    @Override
    protected PhoneNumbersPickerAdapter createListAdapter() {
        PhoneNumbersPickerAdapter phoneNumbersPickerAdapter = new PhoneNumbersPickerAdapter(getActivity(), getListView());
        phoneNumbersPickerAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
        return phoneNumbersPickerAdapter;
    }
}
