package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.list.ContactListFilter;
import com.mediatek.contacts.util.Log;

public class PhoneAndEmailsPickerFragment extends DataKindBasePickerFragment {
    private int mNumberBalance = 100;

    public void setNumberBalance(int i) {
        this.mNumberBalance = i;
    }

    @Override
    protected PhoneAndEmailsPickerAdapter createListAdapter() {
        PhoneAndEmailsPickerAdapter phoneAndEmailsPickerAdapter = new PhoneAndEmailsPickerAdapter(getActivity(), getListView());
        phoneAndEmailsPickerAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
        return phoneAndEmailsPickerAdapter;
    }

    @Override
    public void onOptionAction() {
        long[] checkedItemIds = getCheckedItemIds();
        if (checkedItemIds == null) {
            Log.w("PhoneAndEmailsPickerFragment", "[onOptionAction]idArray = null, return! ");
            return;
        }
        Log.i("PhoneAndEmailsPickerFragment", "[onOptionAction]mNumberBalance = " + this.mNumberBalance + ",idArray.length = " + checkedItemIds.length);
        if (checkedItemIds.length > this.mNumberBalance) {
            Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.contact_recent_number_limit, String.valueOf(this.mNumberBalance)), 0).show();
            return;
        }
        Activity activity = getActivity();
        Intent intent = new Intent();
        intent.putExtra("com.mediatek.contacts.list.pickdataresult", checkedItemIds);
        activity.setResult(-1, intent);
        activity.finish();
    }
}
