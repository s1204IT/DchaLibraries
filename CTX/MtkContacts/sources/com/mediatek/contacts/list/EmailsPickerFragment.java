package com.mediatek.contacts.list;

import com.android.contacts.list.ContactListFilter;
import com.mediatek.contacts.util.Log;

public class EmailsPickerFragment extends DataKindBasePickerFragment {
    @Override
    protected EmailsPickerAdapter createListAdapter() {
        Log.d("EmailsPickerFragment", "[createListAdapter]");
        EmailsPickerAdapter emailsPickerAdapter = new EmailsPickerAdapter(getActivity(), getListView());
        emailsPickerAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
        return emailsPickerAdapter;
    }
}
