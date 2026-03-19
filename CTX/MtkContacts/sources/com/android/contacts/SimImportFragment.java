package com.android.contacts;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.database.SimContactDao;
import com.android.contacts.editor.AccountHeaderPresenter;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.SimCard;
import com.android.contacts.model.SimContact;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.concurrent.ContactsExecutors;
import com.android.contacts.util.concurrent.ListenableFutureLoader;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class SimImportFragment extends Fragment implements LoaderManager.LoaderCallbacks<LoaderResult>, AbsListView.OnScrollListener, AdapterView.OnItemClickListener {
    private View mAccountHeaderContainer;
    private AccountHeaderPresenter mAccountHeaderPresenter;
    private float mAccountScrolledElevationPixels;
    private AccountTypeManager mAccountTypeManager;
    private SimContactAdapter mAdapter;
    private View mImportButton;
    private ListView mListView;
    private ContentLoadingProgressBar mLoadingIndicator;
    private final Map<AccountWithDataSet, long[]> mPerAccountCheckedIds = new ArrayMap();
    private ContactsPreferences mPreferences;
    private Bundle mSavedInstanceState;
    private int mSubscriptionId;
    private Toolbar mToolbar;

    public static class LoaderResult {
        public List<AccountInfo> accounts;
        public Map<AccountWithDataSet, Set<SimContact>> accountsMap;
        public ArrayList<SimContact> contacts;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSavedInstanceState = bundle;
        this.mPreferences = new ContactsPreferences(getContext());
        this.mAccountTypeManager = AccountTypeManager.getInstance(getActivity());
        this.mAdapter = new SimContactAdapter(getActivity());
        Bundle arguments = getArguments();
        this.mSubscriptionId = arguments != null ? arguments.getInt("subscriptionId", -1) : -1;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.fragment_sim_import, viewGroup, false);
        this.mAccountHeaderContainer = viewInflate.findViewById(R.id.account_header_container);
        this.mAccountScrolledElevationPixels = getResources().getDimension(R.dimen.contact_list_header_elevation);
        this.mAccountHeaderPresenter = new AccountHeaderPresenter(this.mAccountHeaderContainer);
        if (bundle != null) {
            this.mAccountHeaderPresenter.onRestoreInstanceState(bundle);
        } else {
            this.mAccountHeaderPresenter.setCurrentAccount(this.mPreferences.getDefaultAccount());
        }
        this.mAccountHeaderPresenter.setObserver(new AccountHeaderPresenter.Observer() {
            @Override
            public void onChange(AccountHeaderPresenter accountHeaderPresenter) {
                SimImportFragment.this.rememberSelectionsForCurrentAccount();
                SimImportFragment.this.mAdapter.setAccount(accountHeaderPresenter.getCurrentAccount());
                SimImportFragment.this.showSelectionsForCurrentAccount();
                SimImportFragment.this.updateToolbarWithCurrentSelections();
            }
        });
        this.mAdapter.setAccount(this.mAccountHeaderPresenter.getCurrentAccount());
        this.mListView = (ListView) viewInflate.findViewById(R.id.list);
        this.mListView.setOnScrollListener(this);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setChoiceMode(2);
        this.mListView.setOnItemClickListener(this);
        this.mImportButton = viewInflate.findViewById(R.id.import_button);
        this.mImportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimImportFragment.this.importCurrentSelections();
                SimImportFragment.this.getActivity().setResult(-1);
                SimImportFragment.this.getActivity().finish();
            }
        });
        this.mToolbar = (Toolbar) viewInflate.findViewById(R.id.toolbar);
        this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SimImportFragment.this.getActivity().setResult(0);
                SimImportFragment.this.getActivity().finish();
            }
        });
        this.mLoadingIndicator = (ContentLoadingProgressBar) viewInflate.findViewById(R.id.loading_progress);
        return viewInflate;
    }

    private void rememberSelectionsForCurrentAccount() {
        AccountWithDataSet account = this.mAdapter.getAccount();
        if (account == null) {
            return;
        }
        long[] checkedItemIds = this.mListView.getCheckedItemIds();
        Arrays.sort(checkedItemIds);
        this.mPerAccountCheckedIds.put(account, checkedItemIds);
    }

    private void showSelectionsForCurrentAccount() {
        long[] jArr = this.mPerAccountCheckedIds.get(this.mAdapter.getAccount());
        if (jArr == null) {
            selectAll();
            return;
        }
        int count = this.mListView.getCount();
        for (int i = 0; i < count; i++) {
            this.mListView.setItemChecked(i, Arrays.binarySearch(jArr, this.mListView.getItemIdAtPosition(i)) >= 0);
        }
    }

    private void selectAll() {
        int count = this.mListView.getCount();
        for (int i = 0; i < count; i++) {
            this.mListView.setItemChecked(i, true);
        }
    }

    private void updateToolbarWithCurrentSelections() {
        SparseBooleanArray checkedItemPositions = this.mListView.getCheckedItemPositions();
        int i = 0;
        for (int i2 = 0; i2 < checkedItemPositions.size(); i2++) {
            if (checkedItemPositions.valueAt(i2) && !this.mAdapter.existsInCurrentAccount(checkedItemPositions.keyAt(i2))) {
                i++;
            }
        }
        if (i == 0) {
            this.mImportButton.setVisibility(8);
            this.mToolbar.setTitle(R.string.sim_import_title_none_selected);
        } else {
            this.mToolbar.setTitle(String.valueOf(i));
            this.mImportButton.setVisibility(0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mAdapter.isEmpty() && getLoaderManager().getLoader(0).isStarted()) {
            this.mLoadingIndicator.show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        rememberSelectionsForCurrentAccount();
        this.mListView.clearChoices();
        super.onSaveInstanceState(bundle);
        this.mAccountHeaderPresenter.onSaveInstanceState(bundle);
        saveAdapterSelectedStates(bundle);
    }

    @Override
    public Loader<LoaderResult> onCreateLoader(int i, Bundle bundle) {
        return new SimContactLoader(getContext(), this.mSubscriptionId);
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult> loader, LoaderResult loaderResult) {
        this.mLoadingIndicator.hide();
        if (loaderResult == null) {
            return;
        }
        this.mAccountHeaderPresenter.setAccounts(loaderResult.accounts);
        restoreAdapterSelectedStates(loaderResult.accounts);
        this.mAdapter.setData(loaderResult);
        this.mListView.setEmptyView(getView().findViewById(R.id.empty_message));
        showSelectionsForCurrentAccount();
        updateToolbarWithCurrentSelections();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult> loader) {
    }

    private void restoreAdapterSelectedStates(List<AccountInfo> list) {
        if (this.mSavedInstanceState == null) {
            return;
        }
        for (AccountInfo accountInfo : list) {
            this.mPerAccountCheckedIds.put(accountInfo.getAccount(), this.mSavedInstanceState.getLongArray(accountInfo.getAccount().stringify() + "_selectedIds"));
        }
        this.mSavedInstanceState = null;
    }

    private void saveAdapterSelectedStates(Bundle bundle) {
        if (this.mAdapter == null) {
            return;
        }
        for (Map.Entry<AccountWithDataSet, long[]> entry : this.mPerAccountCheckedIds.entrySet()) {
            bundle.putLongArray(entry.getKey().stringify() + "_selectedIds", entry.getValue());
        }
    }

    private void importCurrentSelections() {
        SparseBooleanArray checkedItemPositions = this.mListView.getCheckedItemPositions();
        ArrayList arrayList = new ArrayList(checkedItemPositions.size());
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i) && !this.mAdapter.existsInCurrentAccount(i)) {
                arrayList.add(this.mAdapter.getItem(checkedItemPositions.keyAt(i)));
            }
        }
        SimImportService.startImport(getContext(), this.mSubscriptionId, arrayList, this.mAccountHeaderPresenter.getCurrentAccount());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (this.mAdapter.existsInCurrentAccount(i)) {
            Snackbar.make(getView(), R.string.sim_import_contact_exists_toast, 0).show();
        } else {
            updateToolbarWithCurrentSelections();
        }
    }

    @Override
    public Context getContext() {
        if (CompatUtils.isMarshmallowCompatible()) {
            return super.getContext();
        }
        return getActivity();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        if (absListView != null && absListView.getChildAt(0) != null && absListView.getChildAt(0).getTop() < 0) {
            i++;
        }
        if (i == 0) {
            ViewCompat.setElevation(this.mAccountHeaderContainer, ContactPhotoManager.OFFSET_DEFAULT);
        } else {
            ViewCompat.setElevation(this.mAccountHeaderContainer, this.mAccountScrolledElevationPixels);
        }
    }

    public static SimImportFragment newInstance(int i) {
        SimImportFragment simImportFragment = new SimImportFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("subscriptionId", i);
        simImportFragment.setArguments(bundle);
        return simImportFragment;
    }

    private static class SimContactAdapter extends ArrayAdapter<SimContact> {
        private Map<AccountWithDataSet, Set<SimContact>> mExistingMap;
        private LayoutInflater mInflater;
        private AccountWithDataSet mSelectedAccount;

        public SimContactAdapter(Context context) {
            super(context, 0);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        @Override
        public long getItemId(int i) {
            if (i < 0 || i >= getCount()) {
                return -1L;
            }
            return getItem(i).getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int i) {
            return existsInCurrentAccount(i) ? 1 : 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int i2;
            TextView textView = (TextView) view;
            if (textView == null) {
                if (existsInCurrentAccount(i)) {
                    i2 = R.layout.sim_import_list_item_disabled;
                } else {
                    i2 = R.layout.sim_import_list_item;
                }
                textView = (TextView) this.mInflater.inflate(i2, viewGroup, false);
            }
            textView.setText(getItemLabel(getItem(i)));
            return textView;
        }

        public void setData(LoaderResult loaderResult) {
            clear();
            addAll(loaderResult.contacts);
            this.mExistingMap = loaderResult.accountsMap;
        }

        public void setAccount(AccountWithDataSet accountWithDataSet) {
            this.mSelectedAccount = accountWithDataSet;
            notifyDataSetChanged();
        }

        public AccountWithDataSet getAccount() {
            return this.mSelectedAccount;
        }

        public boolean existsInCurrentAccount(int i) {
            return existsInCurrentAccount(getItem(i));
        }

        public boolean existsInCurrentAccount(SimContact simContact) {
            if (this.mSelectedAccount == null || !this.mExistingMap.containsKey(this.mSelectedAccount)) {
                return false;
            }
            return this.mExistingMap.get(this.mSelectedAccount).contains(simContact);
        }

        private String getItemLabel(SimContact simContact) {
            if (simContact.hasName()) {
                return simContact.getName();
            }
            if (simContact.hasPhone()) {
                return simContact.getPhone();
            }
            if (simContact.hasEmails()) {
                return simContact.getEmails()[0];
            }
            return "";
        }
    }

    private static class SimContactLoader extends ListenableFutureLoader<LoaderResult> {
        private AccountTypeManager mAccountTypeManager;
        private SimContactDao mDao;
        private final int mSubscriptionId;

        public SimContactLoader(Context context, int i) {
            super(context, new IntentFilter(AccountTypeManager.BROADCAST_ACCOUNTS_CHANGED));
            this.mDao = SimContactDao.create(context);
            this.mAccountTypeManager = AccountTypeManager.getInstance(getContext());
            this.mSubscriptionId = i;
        }

        @Override
        protected ListenableFuture<LoaderResult> loadData() {
            return Futures.transform(Futures.allAsList(this.mAccountTypeManager.filterAccountsAsync(AccountTypeManager.writableFilter()), ContactsExecutors.getSimReadExecutor().submit((Callable) new Callable<Object>() {
                @Override
                public Object call2() throws Exception {
                    return SimContactLoader.this.loadFromSim();
                }
            })), new Function<List<Object>, LoaderResult>() {
                @Override
                public LoaderResult apply(List<Object> list) {
                    List<AccountInfo> list2 = (List) list.get(0);
                    LoaderResult loaderResult = (LoaderResult) list.get(1);
                    loaderResult.accounts = list2;
                    return loaderResult;
                }
            });
        }

        private LoaderResult loadFromSim() {
            SimCard simBySubscriptionId = this.mDao.getSimBySubscriptionId(this.mSubscriptionId);
            LoaderResult loaderResult = new LoaderResult();
            if (simBySubscriptionId == null) {
                loaderResult.contacts = new ArrayList<>();
                loaderResult.accountsMap = Collections.emptyMap();
                return loaderResult;
            }
            loaderResult.contacts = this.mDao.loadContactsForSim(simBySubscriptionId);
            loaderResult.accountsMap = this.mDao.findAccountsOfExistingSimContacts(loaderResult.contacts);
            return loaderResult;
        }
    }
}
