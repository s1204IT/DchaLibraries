package com.mediatek.contacts.list;

import android.content.Intent;
import android.os.Bundle;
import com.android.contacts.list.ContactListFilter;

public class DataItemsPickerFragment extends DataKindBasePickerFragment {
    private Intent mIntent;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mIntent = (Intent) getArguments().getParcelable("intent");
    }

    @Override
    protected DataItemsPickerAdapter createListAdapter() {
        DataItemsPickerAdapter dataItemsPickerAdapter = new DataItemsPickerAdapter(getActivity(), getListView());
        dataItemsPickerAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
        if (this.mIntent != null) {
            long[] longArrayExtra = this.mIntent.getLongArrayExtra("restrictlist");
            if (longArrayExtra != null && longArrayExtra.length > 0) {
                dataItemsPickerAdapter.setRestrictList(longArrayExtra);
            }
            dataItemsPickerAdapter.setMimetype(this.mIntent.getType());
        }
        return dataItemsPickerAdapter;
    }
}
