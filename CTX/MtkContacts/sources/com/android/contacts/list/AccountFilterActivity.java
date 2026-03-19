package com.android.contacts.list;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.mediatek.contacts.eventhandler.BaseEventHandlerActivity;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public class AccountFilterActivity extends BaseEventHandlerActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = AccountFilterActivity.class.getSimpleName();
    private int mCurrentFilterType;
    private ContactListFilterView mCustomFilterView;
    private boolean mIsCustomFilterViewSelected;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.contact_list_filter);
        this.mListView = (ListView) findViewById(android.R.id.list);
        this.mListView.setOnItemClickListener(this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mCurrentFilterType = ContactListFilterController.getInstance(this).isCustomFilterPersisted() ? -3 : -2;
        ArrayList arrayList = new ArrayList();
        arrayList.add(ContactListFilter.createFilterWithType(-2));
        arrayList.add(ContactListFilter.createFilterWithType(-3));
        this.mListView.setAdapter((ListAdapter) new FilterListAdapter(this, arrayList, this.mCurrentFilterType));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        ContactListFilterView contactListFilterView = (ContactListFilterView) view;
        ContactListFilter contactListFilter = (ContactListFilter) view.getTag();
        if (contactListFilter == null) {
            return;
        }
        if (contactListFilter.filterType == -3) {
            this.mCustomFilterView = contactListFilterView;
            this.mIsCustomFilterViewSelected = contactListFilterView.isChecked();
            Intent intentPutExtra = new Intent(this, (Class<?>) CustomContactListFilterActivity.class).putExtra("currentListFilterType", this.mCurrentFilterType);
            contactListFilterView.setActivated(true);
            contactListFilterView.announceForAccessibility(contactListFilterView.generateContentDescription());
            startActivityForResult(intentPutExtra, 0);
            return;
        }
        contactListFilterView.setActivated(true);
        contactListFilterView.announceForAccessibility(contactListFilterView.generateContentDescription());
        Intent intent = new Intent();
        intent.putExtra("contactListFilter", contactListFilter);
        setResult(-1, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i2 == 0 && this.mCustomFilterView != null && !this.mIsCustomFilterViewSelected) {
            this.mCustomFilterView.setActivated(false);
            return;
        }
        if (i2 == -1 && i == 0) {
            Intent intent2 = new Intent();
            intent2.putExtra("contactListFilter", ContactListFilter.createFilterWithType(-3));
            setResult(-1, intent2);
            finish();
        }
    }

    private static class FilterListAdapter extends BaseAdapter {
        private final AccountTypeManager mAccountTypes;
        private final int mCurrentFilter;
        private final List<ContactListFilter> mFilters;
        private final LayoutInflater mLayoutInflater;

        public FilterListAdapter(Context context, List<ContactListFilter> list, int i) {
            this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mFilters = list;
            this.mCurrentFilter = i;
            this.mAccountTypes = AccountTypeManager.getInstance(context);
        }

        @Override
        public int getCount() {
            return this.mFilters.size();
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public ContactListFilter getItem(int i) {
            return this.mFilters.get(i);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ContactListFilterView contactListFilterView;
            if (view == null) {
                contactListFilterView = (ContactListFilterView) this.mLayoutInflater.inflate(R.layout.contact_list_filter_item, viewGroup, false);
            } else {
                contactListFilterView = (ContactListFilterView) view;
            }
            contactListFilterView.setSingleAccount(this.mFilters.size() == 1);
            ContactListFilter contactListFilter = this.mFilters.get(i);
            contactListFilterView.setContactListFilter(contactListFilter);
            contactListFilterView.bindView(this.mAccountTypes);
            contactListFilterView.setTag(contactListFilter);
            contactListFilterView.setActivated(contactListFilter.filterType == this.mCurrentFilter);
            return contactListFilterView;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onReceiveEvent(String str, Intent intent) {
        Log.i(TAG, "[onReceiveEvent] eventType: " + str + ", extraData: " + intent);
        if ("PhbChangeEvent".equals(str) && !isFinishing()) {
            Log.i(TAG, "[onReceiveEvent] phb state changed, finish!");
            finish();
        }
    }
}
