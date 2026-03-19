package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.mediatek.contacts.list.DataKindBasePickerAdapter;
import com.mediatek.contacts.util.Log;

public abstract class DataKindBasePickerFragment extends AbstractPickerFragment implements DataKindBasePickerAdapter.SelectedContactsListener {
    private View mAccountFilterHeader;

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        this.mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        this.mAccountFilterHeader.setClickable(false);
        this.mAccountFilterHeader.setVisibility(8);
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        DataKindBasePickerAdapter dataKindBasePickerAdapter = (DataKindBasePickerAdapter) getAdapter();
        if (dataKindBasePickerAdapter == null) {
            Log.w("DataKindBasePickerFragment", "[configureAdapter]adapter = null.");
        } else {
            dataKindBasePickerAdapter.setPinnedPartitionHeadersEnabled(false);
            dataKindBasePickerAdapter.setSelectedContactsListener((DataKindBasePickerAdapter.SelectedContactsListener) this);
        }
    }

    public void onOptionAction() {
        long[] checkedItemIds = getCheckedItemIds();
        if (checkedItemIds == null) {
            Log.w("DataKindBasePickerFragment", "[onOptionAction]idArray = null.");
            return;
        }
        Activity activity = getActivity();
        Intent intent = new Intent();
        intent.putExtra("com.mediatek.contacts.list.pickdataresult", checkedItemIds);
        activity.setResult(-1, intent);
        activity.finish();
    }

    @Override
    public long getListItemDataId(int i) {
        DataKindBasePickerAdapter dataKindBasePickerAdapter = (DataKindBasePickerAdapter) getAdapter();
        if (dataKindBasePickerAdapter != null) {
            return dataKindBasePickerAdapter.getDataId(i);
        }
        return -1L;
    }

    public void onSelectedContactsChangedViaCheckBox() {
        int size = ((MultiSelectEntryContactListAdapter) getAdapter()).getSelectedContactIds().size();
        Log.i("DataKindBasePickerFragment", "[onSelectedContactsChangedViaCheckBox] checkCount : " + size);
        updateSelectedItemsView(size);
    }
}
