package com.android.contacts.common.list;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class AccountFilterActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String TAG = AccountFilterActivity.class.getSimpleName();
    private ContactListFilter mCurrentFilter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.contact_list_filter);
        this.mListView = (ListView) findViewById(android.R.id.list);
        this.mListView.setOnItemClickListener(this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        this.mCurrentFilter = (ContactListFilter) getIntent().getParcelableExtra("currentFilter");
        getLoaderManager().initLoader(0, null, new MyLoaderCallbacks());
    }

    private static class FilterLoader extends AsyncTaskLoader<List<ContactListFilter>> {
        private Context mContext;

        public FilterLoader(Context context) {
            super(context);
            this.mContext = context;
        }

        @Override
        public List<ContactListFilter> loadInBackground() {
            return AccountFilterActivity.loadAccountFilters(this.mContext);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            onStopLoading();
        }
    }

    private static List<ContactListFilter> loadAccountFilters(Context context) {
        ArrayList<ContactListFilter> result = Lists.newArrayList();
        ArrayList<ContactListFilter> accountFilters = Lists.newArrayList();
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> accounts = accountTypes.getAccounts(false);
        for (AccountWithDataSet account : accounts) {
            AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            if (!accountType.isExtension() || account.hasData(context)) {
                Drawable icon = accountType != null ? accountType.getDisplayIcon(context) : null;
                accountFilters.add(ContactListFilter.createAccountFilter(account.type, account.name, account.dataSet, icon));
            }
        }
        result.add(ContactListFilter.createFilterWithType(-2));
        int count = accountFilters.size();
        if (count >= 1) {
            if (count > 1) {
                result.addAll(accountFilters);
            }
            result.add(ContactListFilter.createFilterWithType(-3));
        }
        return result;
    }

    private class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<ContactListFilter>> {
        private MyLoaderCallbacks() {
        }

        @Override
        public Loader<List<ContactListFilter>> onCreateLoader(int id, Bundle args) {
            return new FilterLoader(AccountFilterActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<List<ContactListFilter>> loader, List<ContactListFilter> data) {
            if (data == null) {
                Log.e(AccountFilterActivity.TAG, "Failed to load filters");
            } else {
                AccountFilterActivity.this.mListView.setAdapter((ListAdapter) new FilterListAdapter(AccountFilterActivity.this, data, AccountFilterActivity.this.mCurrentFilter));
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ContactListFilter>> loader) {
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ContactListFilter filter = (ContactListFilter) view.getTag();
        if (filter != null) {
            if (filter.filterType == -3) {
                startActivityForResult(new Intent(this, (Class<?>) CustomContactListFilterActivity.class), 0);
                return;
            }
            Intent intent = new Intent();
            intent.putExtra("contactListFilter", filter);
            setResult(-1, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1) {
            switch (requestCode) {
                case 0:
                    Intent intent = new Intent();
                    ContactListFilter filter = ContactListFilter.createFilterWithType(-3);
                    intent.putExtra("contactListFilter", filter);
                    setResult(-1, intent);
                    finish();
                    break;
            }
        }
    }

    private static class FilterListAdapter extends BaseAdapter {
        private final AccountTypeManager mAccountTypes;
        private final ContactListFilter mCurrentFilter;
        private final List<ContactListFilter> mFilters;
        private final LayoutInflater mLayoutInflater;

        public FilterListAdapter(Context context, List<ContactListFilter> filters, ContactListFilter current) {
            this.mLayoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mFilters = filters;
            this.mCurrentFilter = current;
            this.mAccountTypes = AccountTypeManager.getInstance(context);
        }

        @Override
        public int getCount() {
            return this.mFilters.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public ContactListFilter getItem(int position) {
            return this.mFilters.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ContactListFilterView view;
            if (convertView != null) {
                view = (ContactListFilterView) convertView;
            } else {
                view = (ContactListFilterView) this.mLayoutInflater.inflate(R.layout.contact_list_filter_item, parent, false);
            }
            view.setSingleAccount(this.mFilters.size() == 1);
            ContactListFilter filter = this.mFilters.get(position);
            view.setContactListFilter(filter);
            view.bindView(this.mAccountTypes);
            view.setTag(filter);
            view.setActivated(filter.equals(this.mCurrentFilter));
            return view;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
