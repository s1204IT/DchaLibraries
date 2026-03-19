package com.android.contacts.list;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.R;
import com.android.contacts.group.GroupUtil;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import java.util.TreeSet;

public class MultiSelectPhoneNumbersListFragment extends MultiSelectContactsListFragment<MultiSelectPhoneNumbersListAdapter> {
    public MultiSelectPhoneNumbersListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(false);
        setSearchMode(false);
        setHasOptionsMenu(true);
        setListType(12);
    }

    @Override
    public MultiSelectPhoneNumbersListAdapter createListAdapter() {
        MultiSelectPhoneNumbersListAdapter multiSelectPhoneNumbersListAdapter = new MultiSelectPhoneNumbersListAdapter(getActivity());
        multiSelectPhoneNumbersListAdapter.setArguments(getArguments());
        return multiSelectPhoneNumbersListAdapter;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.items_multi_select, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem menuItemFindItem = menu.findItem(R.id.menu_send);
        menuItemFindItem.setVisible(((MultiSelectPhoneNumbersListAdapter) getAdapter()).hasSelectedItems());
        menuItemFindItem.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MultiSelectPhoneNumbersListFragment.this.onOptionsItemSelected(menuItemFindItem);
            }
        });
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        getActivity().finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_send) {
            String stringExtra = getActivity().getIntent().getStringExtra("com.android.contacts.extra.SELECTION_SEND_SCHEME");
            GroupUtil.startSendToSelectionActivity(this, TextUtils.join(",", GroupUtil.getSendToDataForIds(getActivity(), ((MultiSelectPhoneNumbersListAdapter) getAdapter()).getSelectedContactIdsArray(), stringExtra)), stringExtra, getActivity().getIntent().getStringExtra("com.android.contacts.extra.SELECTION_SEND_TITLE"));
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        long[] longArrayExtra = getActivity().getIntent().getLongArrayExtra("com.android.contacts.extra.SELECTION_DEFAULT_SELECTION");
        if (longArrayExtra != null && longArrayExtra.length != 0) {
            TreeSet<Long> treeSet = new TreeSet<>();
            for (long j : longArrayExtra) {
                treeSet.add(Long.valueOf(j));
            }
            ((MultiSelectPhoneNumbersListAdapter) getAdapter()).setSelectedContactIds(treeSet);
            onSelectedContactsChanged();
        }
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        displayCheckBoxes(true);
    }

    @Override
    protected boolean onItemLongClick(int i, long j) {
        return true;
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    @Override
    protected int getSubId() {
        if (getActivity() != null) {
            return getActivity().getIntent().getIntExtra("com.android.contacts.extra.GROUP_ACCOUNT_SUBID", SubInfoUtils.getInvalidSubId());
        }
        return SubInfoUtils.getInvalidSubId();
    }
}
