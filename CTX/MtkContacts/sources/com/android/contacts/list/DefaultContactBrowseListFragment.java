package com.android.contacts.list;

import android.accounts.Account;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.interactions.ContactMultiDeletionInteraction;
import com.android.contacts.list.EnableGlobalSyncDialogFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.logging.Logger;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.SharedPreferenceUtil;
import com.android.contacts.util.SyncUtil;
import com.android.contactsbind.FeatureHighlightHelper;
import com.android.contactsbind.experiments.Flags;
import com.google.common.util.concurrent.Futures;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.Log;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

public class DefaultContactBrowseListFragment extends ContactBrowseListFragment implements EnableGlobalSyncDialogFragment.Listener {
    private View mAccountFilterContainer;
    private ActionBarAdapter mActionBarAdapter;
    private PeopleActivity mActivity;
    private View mAlertContainer;
    private ImageView mAlertDismissIcon;
    private TextView mAlertText;
    private ContactListFilterController mContactListFilterController;
    private boolean mContactsAvailable;
    private ContactsRequest mContactsRequest;
    private boolean mDisableOptionItemSelected;
    private View mEmptyAccountView;
    private View mEmptyHomeView;
    private boolean mEnableDebugMenuOptions;
    private boolean mFragmentInitialized;
    private boolean mFromOnNewIntent;
    private boolean mIsDeletionInProgress;
    private boolean mIsRecreatedInstance;
    private boolean mOptionsMenuContactsAvailable;
    private View mSearchHeaderView;
    private View mSearchProgress;
    private TextView mSearchProgressText;
    private boolean mSearchResultClicked;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Future<List<AccountInfo>> mWritableAccountsFuture;
    private final Handler mHandler = new Handler();
    private final Runnable mCancelRefresh = new Runnable() {
        @Override
        public void run() {
            if (DefaultContactBrowseListFragment.this.mSwipeRefreshLayout.isRefreshing()) {
                DefaultContactBrowseListFragment.this.mSwipeRefreshLayout.setRefreshing(false);
            }
        }
    };
    private int mReasonSyncOff = 0;
    private boolean mCanSetActionBar = false;
    private final ActionBarAdapter.Listener mActionBarListener = new ActionBarAdapter.Listener() {
        @Override
        public void onAction(int i) {
            switch (i) {
                case 0:
                    String queryString = DefaultContactBrowseListFragment.this.mActionBarAdapter.getQueryString();
                    setQueryTextToFragment(queryString);
                    updateDebugOptionsVisibility("debug debug!".equals(queryString));
                    return;
                case 1:
                    if (!DefaultContactBrowseListFragment.this.mIsRecreatedInstance) {
                        Logger.logScreenView(DefaultContactBrowseListFragment.this.mActivity, 1);
                    }
                    startSearchOrSelectionMode();
                    return;
                case 2:
                    DefaultContactBrowseListFragment.this.displayCheckBoxes(true);
                    startSearchOrSelectionMode();
                    return;
                case 3:
                    if (TextUtils.isEmpty(DefaultContactBrowseListFragment.this.getQueryString())) {
                        DefaultContactBrowseListFragment.this.maybeShowHamburgerFeatureHighlight();
                    }
                    setQueryTextToFragment("");
                    DefaultContactBrowseListFragment.this.maybeHideCheckBoxes();
                    DefaultContactBrowseListFragment.this.mActivity.invalidateOptionsMenu();
                    DefaultContactBrowseListFragment.this.mActivity.showFabWithAnimation(true);
                    DefaultContactBrowseListFragment.this.setSyncOffAlert();
                    DefaultContactBrowseListFragment.this.setSwipeRefreshLayoutEnabledOrNot(DefaultContactBrowseListFragment.this.getFilter());
                    return;
                case CompatUtils.TYPE_ASSERT:
                    DefaultContactBrowseListFragment.this.mActivity.showFabWithAnimation(true);
                    return;
                default:
                    throw new IllegalStateException("Unknown ActionBarAdapter action: " + i);
            }
        }

        private void startSearchOrSelectionMode() {
            DefaultContactBrowseListFragment.this.configureContactListFragment();
            DefaultContactBrowseListFragment.this.maybeHideCheckBoxes();
            DefaultContactBrowseListFragment.this.mActivity.invalidateOptionsMenu();
            DefaultContactBrowseListFragment.this.mActivity.showFabWithAnimation(false);
            Context context = DefaultContactBrowseListFragment.this.getContext();
            if (!SharedPreferenceUtil.getHamburgerPromoTriggerActionHappenedBefore(context)) {
                SharedPreferenceUtil.setHamburgerPromoTriggerActionHappenedBefore(context);
            }
        }

        private void updateDebugOptionsVisibility(boolean z) {
            if (DefaultContactBrowseListFragment.this.mEnableDebugMenuOptions != z) {
                DefaultContactBrowseListFragment.this.mEnableDebugMenuOptions = z;
                DefaultContactBrowseListFragment.this.mActivity.invalidateOptionsMenu();
            }
        }

        private void setQueryTextToFragment(String str) {
            DefaultContactBrowseListFragment.this.setQueryString(str, true);
            DefaultContactBrowseListFragment.this.setVisibleScrollbarEnabled(!DefaultContactBrowseListFragment.this.isSearchMode());
        }

        @Override
        public void onUpButtonPressed() {
            DefaultContactBrowseListFragment.this.mActivity.onBackPressed();
        }
    };
    private final View.OnClickListener mAddContactListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startEditorIntent(DefaultContactBrowseListFragment.this.getContext(), DefaultContactBrowseListFragment.this.mActivity.getIntent(), DefaultContactBrowseListFragment.this.getFilter());
        }
    };

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        setQuickContactEnabled(false);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setDisplayDirectoryHeader(false);
        setHasOptionsMenu(true);
    }

    public boolean wasSearchResultClicked() {
        return this.mSearchResultClicked;
    }

    public void resetSearchResultClicked() {
        this.mSearchResultClicked = false;
    }

    @Override
    public CursorLoader createCursorLoader(Context context) {
        return new FavoritesAndContactsLoader(context);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == 0) {
            bindListHeader(cursor == null ? 0 : cursor.getCount());
        }
        super.onLoadFinished(loader, cursor);
        if (!isSearchMode()) {
            maybeShowHamburgerFeatureHighlight();
        }
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.updateOverflowButtonColor();
        }
    }

    private void maybeShowHamburgerFeatureHighlight() {
        if (this.mActionBarAdapter != null && !this.mActionBarAdapter.isSearchMode() && !this.mActionBarAdapter.isSelectionMode() && !isTalkbackOnAndOnPreLollipopMr1() && SharedPreferenceUtil.getShouldShowHamburgerPromo(getContext()) && FeatureHighlightHelper.showHamburgerFeatureHighlight(this.mActivity)) {
            SharedPreferenceUtil.setHamburgerPromoDisplayedBefore(getContext());
        }
    }

    private boolean isTalkbackOnAndOnPreLollipopMr1() {
        return ((AccessibilityManager) getContext().getSystemService("accessibility")).isTouchExplorationEnabled() && !CompatUtils.isLollipopMr1Compatible();
    }

    private void bindListHeader(int i) {
        ContactListFilter filter = getFilter();
        if (!isSearchMode() && i <= 0 && shouldShowEmptyView(filter)) {
            if (filter != null && filter.isContactsFilterType()) {
                makeViewVisible(this.mEmptyHomeView);
                return;
            } else {
                makeViewVisible(this.mEmptyAccountView);
                return;
            }
        }
        makeViewVisible(this.mAccountFilterContainer);
        if (isSearchMode()) {
            hideHeaderAndAddPadding(getContext(), getListView(), this.mAccountFilterContainer);
            return;
        }
        if (filter.filterType == -3) {
            bindListHeaderCustom(getListView(), this.mAccountFilterContainer);
        } else if (filter.filterType != -2) {
            bindListHeader(getContext(), getListView(), this.mAccountFilterContainer, new AccountWithDataSet(filter.accountName, filter.accountType, filter.dataSet), i);
        } else {
            hideHeaderAndAddPadding(getContext(), getListView(), this.mAccountFilterContainer);
        }
    }

    private boolean shouldShowEmptyView(ContactListFilter contactListFilter) {
        if (contactListFilter == null) {
            return true;
        }
        if (-1 == contactListFilter.filterType || -2 == contactListFilter.filterType) {
            List<AccountInfo> writableGoogleAccounts = AccountTypeManager.getInstance(getContext()).getWritableGoogleAccounts();
            if (writableGoogleAccounts != null && writableGoogleAccounts.size() > 0) {
                Iterator<AccountInfo> it = writableGoogleAccounts.iterator();
                while (it.hasNext()) {
                    Account accountOrNull = it.next().getAccount().getAccountOrNull();
                    if (SyncUtil.isSyncStatusPendingOrActive(accountOrNull) || SyncUtil.isUnsyncableGoogleAccount(accountOrNull)) {
                        return false;
                    }
                }
            }
        } else if (contactListFilter.filterType == 0) {
            Account account = new Account(contactListFilter.accountName, contactListFilter.accountType);
            return (SyncUtil.isSyncStatusPendingOrActive(account) || SyncUtil.isUnsyncableGoogleAccount(account)) ? false : true;
        }
        return true;
    }

    private void makeViewVisible(View view) {
        this.mEmptyAccountView.setVisibility(view == this.mEmptyAccountView ? 0 : 8);
        this.mEmptyHomeView.setVisibility(view == this.mEmptyHomeView ? 0 : 8);
        this.mAccountFilterContainer.setVisibility(view == this.mAccountFilterContainer ? 0 : 8);
    }

    public void scrollToTop() {
        if (getListView() != null) {
            getListView().setSelection(0);
        }
    }

    @Override
    protected void onItemClick(int i, long j) {
        Uri contactUri = getAdapter().getContactUri(i);
        if (contactUri == null) {
            Log.e("DefaultListFragment", "[onItemClick]uri is null!!! position=" + i + ", id=" + j);
            return;
        }
        if (getAdapter().isDisplayingCheckBoxes()) {
            super.onItemClick(i, j);
            return;
        }
        if (isSearchMode()) {
            this.mSearchResultClicked = true;
            Logger.logSearchEvent(createSearchStateForSearchResultClick(i));
        }
        ExtensionManager.getInstance();
        if (ExtensionManager.getRcsExtension().addRcsProfileEntryListener(contactUri, false)) {
            return;
        }
        viewContact(i, contactUri, getAdapter().isEnterpriseContact(i));
        Log.d("DefaultListFragment", "[onItemClick]position=" + i + ", id=" + j);
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter defaultContactListAdapter = new DefaultContactListAdapter(getContext());
        defaultContactListAdapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        defaultContactListAdapter.setDisplayPhotos(true);
        defaultContactListAdapter.setPhotoPosition(ContactListItemView.getDefaultPhotoPosition(false));
        return defaultContactListAdapter;
    }

    @Override
    public ContactListFilter getFilter() {
        return this.mContactListFilterController.getFilter();
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        View viewInflate = layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
        this.mAccountFilterContainer = viewInflate.findViewById(R.id.account_filter_header_container);
        FrameLayout frameLayout = (FrameLayout) viewInflate.findViewById(R.id.contact_list);
        this.mEmptyAccountView = getEmptyAccountView(layoutInflater);
        this.mEmptyHomeView = getEmptyHomeView(layoutInflater);
        frameLayout.addView(this.mEmptyAccountView);
        frameLayout.addView(this.mEmptyHomeView);
        return viewInflate;
    }

    private View getEmptyHomeView(LayoutInflater layoutInflater) {
        View viewInflate = layoutInflater.inflate(R.layout.empty_home_view, (ViewGroup) null);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.empty_home_image);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.setMargins(0, (getResources().getDisplayMetrics().heightPixels / 2) - getResources().getDimensionPixelSize(R.dimen.empty_home_view_image_offset), 0, 0);
        layoutParams.gravity = 1;
        imageView.setLayoutParams(layoutParams);
        ((Button) viewInflate.findViewById(R.id.add_contact_button)).setOnClickListener(this.mAddContactListener);
        return viewInflate;
    }

    private View getEmptyAccountView(LayoutInflater layoutInflater) {
        View viewInflate = layoutInflater.inflate(R.layout.empty_account_view, (ViewGroup) null);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.empty_account_image);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.setMargins(0, (getResources().getDisplayMetrics().heightPixels / getResources().getInteger(R.integer.empty_account_view_image_margin_divisor)) + getResources().getDimensionPixelSize(R.dimen.empty_account_view_image_offset), 0, 0);
        layoutParams.gravity = 1;
        imageView.setLayoutParams(layoutParams);
        ((Button) viewInflate.findViewById(R.id.add_contact_button)).setOnClickListener(this.mAddContactListener);
        return viewInflate;
    }

    @Override
    public void onCreate(Bundle bundle) {
        ContactListFilter contactListFilterCreateContactsFilter;
        super.onCreate(bundle);
        this.mIsRecreatedInstance = bundle != null;
        this.mContactListFilterController = ContactListFilterController.getInstance(getContext());
        this.mContactListFilterController.checkFilterValidity(false);
        if (this.mIsRecreatedInstance) {
            contactListFilterCreateContactsFilter = getFilter();
        } else {
            contactListFilterCreateContactsFilter = AccountFilterUtil.createContactsFilter(getContext());
        }
        setContactListFilter(contactListFilterCreateContactsFilter);
    }

    @Override
    protected void onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        super.onCreateView(layoutInflater, viewGroup);
        initSwipeRefreshLayout();
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().createEntryView(getListView(), getActivity());
        FrameLayout frameLayout = new FrameLayout(layoutInflater.getContext());
        this.mSearchHeaderView = layoutInflater.inflate(R.layout.search_header, (ViewGroup) null, false);
        frameLayout.addView(this.mSearchHeaderView);
        getListView().addHeaderView(frameLayout, null, false);
        checkHeaderViewVisibility();
        this.mSearchProgress = getView().findViewById(R.id.search_progress);
        this.mSearchProgressText = (TextView) this.mSearchHeaderView.findViewById(R.id.totalContactsText);
        this.mAlertContainer = getView().findViewById(R.id.alert_container);
        this.mAlertText = (TextView) this.mAlertContainer.findViewById(R.id.alert_text);
        this.mAlertDismissIcon = (ImageView) this.mAlertContainer.findViewById(R.id.alert_dismiss_icon);
        this.mAlertText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DefaultContactBrowseListFragment.this.turnSyncOn();
            }
        });
        this.mAlertDismissIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DefaultContactBrowseListFragment.this.dismiss();
            }
        });
        this.mAlertContainer.setVisibility(8);
    }

    private void turnSyncOn() {
        ContactListFilter filter = getFilter();
        if (filter.filterType == 0 && this.mReasonSyncOff == 2) {
            ContentResolver.setSyncAutomatically(new Account(filter.accountName, filter.accountType), "com.android.contacts", true);
            this.mAlertContainer.setVisibility(8);
        } else {
            new EnableGlobalSyncDialogFragment();
            EnableGlobalSyncDialogFragment.show(this, filter);
        }
    }

    @Override
    public void onEnableAutoSync(ContactListFilter contactListFilter) {
        ContentResolver.setMasterSyncAutomatically(true);
        List<Account> syncableAccounts = contactListFilter.getSyncableAccounts(AccountInfo.extractAccounts((List) Futures.getUnchecked(this.mWritableAccountsFuture)));
        if (syncableAccounts != null && syncableAccounts.size() > 0) {
            for (Account account : syncableAccounts) {
                ContentResolver.setSyncAutomatically(new Account(account.name, account.type), "com.android.contacts", true);
            }
        }
        this.mAlertContainer.setVisibility(8);
    }

    private void dismiss() {
        if (this.mReasonSyncOff == 1) {
            SharedPreferenceUtil.incNumOfDismissesForAutoSyncOff(getContext());
        } else if (this.mReasonSyncOff == 2) {
            SharedPreferenceUtil.incNumOfDismissesForAccountSyncOff(getContext(), getFilter().accountName);
        }
        this.mAlertContainer.setVisibility(8);
    }

    private void initSwipeRefreshLayout() {
        this.mSwipeRefreshLayout = (SwipeRefreshLayout) this.mView.findViewById(R.id.swipe_refresh);
        if (this.mSwipeRefreshLayout == null) {
            return;
        }
        this.mSwipeRefreshLayout.setEnabled(true);
        this.mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                DefaultContactBrowseListFragment.this.mHandler.removeCallbacks(DefaultContactBrowseListFragment.this.mCancelRefresh);
                if (!SyncUtil.isNetworkConnected(DefaultContactBrowseListFragment.this.getContext())) {
                    DefaultContactBrowseListFragment.this.mSwipeRefreshLayout.setRefreshing(false);
                    ((PeopleActivity) DefaultContactBrowseListFragment.this.getActivity()).showConnectionErrorMsg();
                } else {
                    DefaultContactBrowseListFragment.this.syncContacts(DefaultContactBrowseListFragment.this.getFilter());
                    DefaultContactBrowseListFragment.this.mHandler.postDelayed(DefaultContactBrowseListFragment.this.mCancelRefresh, Flags.getInstance().getInteger("PullToRefresh__cancel_refresh_millis"));
                }
            }
        });
        this.mSwipeRefreshLayout.setColorSchemeResources(R.color.swipe_refresh_color1, R.color.swipe_refresh_color2, R.color.swipe_refresh_color3, R.color.swipe_refresh_color4);
        this.mSwipeRefreshLayout.setDistanceToTriggerSync((int) getResources().getDimension(R.dimen.pull_to_refresh_distance));
    }

    private void syncContacts(ContactListFilter contactListFilter) {
        if (contactListFilter == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("expedited", true);
        bundle.putBoolean("force", true);
        List<Account> syncableAccounts = contactListFilter.getSyncableAccounts(AccountInfo.extractAccounts((List) Futures.getUnchecked(this.mWritableAccountsFuture)));
        if (syncableAccounts != null && syncableAccounts.size() > 0) {
            for (Account account : syncableAccounts) {
                if (!SyncUtil.isSyncStatusPendingOrActive(account) || SyncUtil.isUnsyncableGoogleAccount(account)) {
                    ContentResolver.requestSync(account, "com.android.contacts", bundle);
                }
            }
        }
    }

    private void setSyncOffAlert() {
        ContactListFilter filter = getFilter();
        Account account = (filter.filterType == 0 && filter.isGoogleAccountType()) ? new Account(filter.accountName, filter.accountType) : null;
        if (account == null && !filter.isContactsFilterType()) {
            this.mAlertContainer.setVisibility(8);
            return;
        }
        this.mReasonSyncOff = SyncUtil.calculateReasonSyncOff(getContext(), account);
        boolean zIsAlertVisible = SyncUtil.isAlertVisible(getContext(), account, this.mReasonSyncOff);
        setSyncOffMsg(this.mReasonSyncOff);
        this.mAlertContainer.setVisibility(zIsAlertVisible ? 0 : 8);
    }

    private void setSyncOffMsg(int i) {
        Resources resources = getResources();
        switch (i) {
            case 1:
                this.mAlertText.setText(resources.getString(R.string.auto_sync_off));
                break;
            case 2:
                this.mAlertText.setText(resources.getString(R.string.account_sync_off));
                break;
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mActivity = (PeopleActivity) getActivity();
        this.mActionBarAdapter = new ActionBarAdapter(this.mActivity, this.mActionBarListener, this.mActivity.getSupportActionBar(), this.mActivity.getToolbar(), R.string.enter_contact_name, this);
        this.mActionBarAdapter.setShowHomeIcon(true);
        initializeActionBarAdapter(bundle);
        if (isSearchMode()) {
            this.mActionBarAdapter.setFocusOnSearchView();
        }
        setCheckBoxListListener(new CheckBoxListListener());
        setOnContactListActionListener(new ContactBrowserActionListener());
        if (bundle != null) {
            if (bundle.getBoolean("deletionInProgress")) {
                deleteSelectedContacts();
            }
            this.mSearchResultClicked = bundle.getBoolean("search_result_clicked");
        }
        setDirectorySearchMode();
        this.mCanSetActionBar = true;
    }

    public void initializeActionBarAdapter(Bundle bundle) {
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.initialize(bundle, this.mContactsRequest);
        }
    }

    private void configureFragment() {
        if (this.mFragmentInitialized && !this.mFromOnNewIntent) {
            return;
        }
        this.mFragmentInitialized = true;
        if (this.mFromOnNewIntent || !this.mIsRecreatedInstance) {
            this.mFromOnNewIntent = false;
            configureFragmentForRequest();
        }
        configureContactListFragment();
    }

    private void configureFragmentForRequest() {
        ContactListFilter contactListFilterCreateContactsFilter;
        int actionCode = this.mContactsRequest.getActionCode();
        boolean zIsSearchMode = this.mContactsRequest.isSearchMode();
        if (actionCode == 15) {
            contactListFilterCreateContactsFilter = AccountFilterUtil.createContactsFilter(getContext());
        } else if (actionCode == 17) {
            contactListFilterCreateContactsFilter = ContactListFilter.createFilterWithType(-5);
        } else {
            contactListFilterCreateContactsFilter = null;
        }
        if (contactListFilterCreateContactsFilter != null) {
            setContactListFilter(contactListFilterCreateContactsFilter);
            zIsSearchMode = false;
        }
        if (this.mContactsRequest.getContactUri() != null) {
            zIsSearchMode = false;
        }
        this.mActionBarAdapter.setSearchMode(zIsSearchMode);
        configureContactListFragmentForRequest();
    }

    private void configureContactListFragmentForRequest() {
        Uri contactUri = this.mContactsRequest.getContactUri();
        if (contactUri != null) {
            setSelectedContactUri(contactUri);
        }
        setQueryString(this.mActionBarAdapter.getQueryString(), true);
        setVisibleScrollbarEnabled(!isSearchMode());
    }

    private void setDirectorySearchMode() {
        if (this.mContactsRequest != null && this.mContactsRequest.isDirectorySearchEnabled()) {
            setDirectorySearchMode(1);
        } else {
            setDirectorySearchMode(0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        configureFragment();
        maybeShowHamburgerFeatureHighlight();
        this.mActionBarAdapter.setListener(this.mActionBarListener);
        this.mDisableOptionItemSelected = false;
        maybeHideCheckBoxes();
        this.mWritableAccountsFuture = AccountTypeManager.getInstance(getContext()).filterAccountsAsync(AccountTypeManager.writableFilter());
    }

    private void maybeHideCheckBoxes() {
        if (!this.mActionBarAdapter.isSelectionMode()) {
            displayCheckBoxes(false);
        }
    }

    @Override
    public ActionBarAdapter getActionBarAdapter() {
        return this.mActionBarAdapter;
    }

    @Override
    protected void setSearchMode(boolean z) {
        super.setSearchMode(z);
        checkHeaderViewVisibility();
        if (!z) {
            showSearchProgress(false);
        }
    }

    private void showSearchProgress(boolean z) {
        if (this.mSearchProgress != null) {
            this.mSearchProgress.setVisibility(z ? 0 : 8);
        }
    }

    private void checkHeaderViewVisibility() {
        if (this.mSearchHeaderView != null) {
            this.mSearchHeaderView.setVisibility(8);
        }
    }

    @Override
    protected void setListHeader() {
        ContactListAdapter adapter;
        if (!isSearchMode() || (adapter = getAdapter()) == null) {
            return;
        }
        if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
            this.mSearchHeaderView.setVisibility(8);
            showSearchProgress(false);
            return;
        }
        this.mSearchHeaderView.setVisibility(0);
        if (adapter.isLoading()) {
            this.mSearchProgressText.setText(R.string.search_results_searching);
            showSearchProgress(true);
        } else {
            this.mSearchProgressText.setText(R.string.listFoundAllContactsZero);
            this.mSearchProgressText.sendAccessibilityEvent(4);
            showSearchProgress(false);
        }
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return this.mSwipeRefreshLayout;
    }

    private final class CheckBoxListListener implements MultiSelectContactsListFragment.OnCheckBoxListActionListener {
        private CheckBoxListListener() {
        }

        @Override
        public void onStartDisplayingCheckBoxes() {
            DefaultContactBrowseListFragment.this.mActionBarAdapter.setSelectionMode(true);
            DefaultContactBrowseListFragment.this.mActivity.invalidateOptionsMenu();
        }

        @Override
        public void onSelectedContactIdsChanged() {
            DefaultContactBrowseListFragment.this.mActionBarAdapter.setSelectionCount(DefaultContactBrowseListFragment.this.getSelectedContactIds().size());
            DefaultContactBrowseListFragment.this.mActivity.invalidateOptionsMenu();
            DefaultContactBrowseListFragment.this.mActionBarAdapter.updateOverflowButtonColor();
        }

        @Override
        public void onStopDisplayingCheckBoxes() {
            DefaultContactBrowseListFragment.this.mActionBarAdapter.setSelectionMode(false);
        }
    }

    public void setFilterAndUpdateTitle(ContactListFilter contactListFilter) {
        setFilterAndUpdateTitle(contactListFilter, true);
    }

    private void setFilterAndUpdateTitle(ContactListFilter contactListFilter, boolean z) {
        setContactListFilter(contactListFilter);
        updateListFilter(contactListFilter, z);
        this.mActivity.setTitle(AccountFilterUtil.getActionBarTitleForFilter(this.mActivity, contactListFilter));
        setSyncOffAlert();
        setSwipeRefreshLayoutEnabledOrNot(contactListFilter);
    }

    private void setSwipeRefreshLayoutEnabledOrNot(ContactListFilter contactListFilter) {
        SwipeRefreshLayout swipeRefreshLayout = getSwipeRefreshLayout();
        if (swipeRefreshLayout == null) {
            if (Log.isLoggable("DefaultListFragment", 3)) {
                Log.d("DefaultListFragment", "Can not load swipeRefreshLayout, swipeRefreshLayout is null");
                return;
            }
            return;
        }
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setEnabled(false);
        if (contactListFilter != null && !this.mActionBarAdapter.isSearchMode() && !this.mActionBarAdapter.isSelectionMode()) {
            if (contactListFilter.isSyncable() || (contactListFilter.shouldShowSyncState() && SyncUtil.hasSyncableAccount(AccountTypeManager.getInstance(getContext())))) {
                swipeRefreshLayout.setEnabled(true);
            }
        }
    }

    private void configureContactListFragment() {
        setFilterAndUpdateTitle(getFilter());
        setVerticalScrollbarPosition(getScrollBarPosition());
        setSelectionVisible(false);
        this.mActivity.invalidateOptionsMenu();
    }

    private int getScrollBarPosition() {
        boolean z;
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) != 1) {
            z = false;
        } else {
            z = true;
        }
        return z ? 1 : 2;
    }

    private final class ContactBrowserActionListener implements OnContactBrowserActionListener {
        ContactBrowserActionListener() {
        }

        @Override
        public void onSelectionChange() {
        }

        @Override
        public void onViewContactAction(int i, Uri uri, boolean z) {
            int i2 = 4;
            if (z) {
                ContactsContract.QuickContact.showQuickContact(DefaultContactBrowseListFragment.this.getContext(), new Rect(), uri, 4, (String[]) null);
                return;
            }
            if (!DefaultContactBrowseListFragment.this.isSearchMode()) {
                if (DefaultContactBrowseListFragment.this.isAllContactsFilter(DefaultContactBrowseListFragment.this.getFilter())) {
                    if (i < DefaultContactBrowseListFragment.this.getAdapter().getNumberOfFavorites()) {
                        i2 = 3;
                    }
                } else {
                    i2 = 8;
                }
            } else {
                i2 = 1;
            }
            Logger.logListEvent(2, DefaultContactBrowseListFragment.this.getListTypeIncludingSearch(), DefaultContactBrowseListFragment.this.getAdapter().getCount(), i, 0);
            ImplicitIntentsUtil.startQuickContact(DefaultContactBrowseListFragment.this.getActivity(), uri, i2);
        }

        @Override
        public void onInvalidSelection() {
            ContactListFilter contactListFilterCreateFilterWithType;
            ContactListFilter filter = DefaultContactBrowseListFragment.this.getFilter();
            if (filter != null && filter.filterType == -6) {
                contactListFilterCreateFilterWithType = AccountFilterUtil.createContactsFilter(DefaultContactBrowseListFragment.this.getContext());
                DefaultContactBrowseListFragment.this.setFilterAndUpdateTitle(contactListFilterCreateFilterWithType);
            } else {
                contactListFilterCreateFilterWithType = ContactListFilter.createFilterWithType(-6);
                DefaultContactBrowseListFragment.this.setFilterAndUpdateTitle(contactListFilterCreateFilterWithType, false);
            }
            DefaultContactBrowseListFragment.this.setContactListFilter(contactListFilterCreateFilterWithType);
        }
    }

    private boolean isAllContactsFilter(ContactListFilter contactListFilter) {
        return contactListFilter != null && contactListFilter.isContactsFilterType();
    }

    public void setContactsAvailable(boolean z) {
        this.mContactsAvailable = z;
    }

    private void setContactListFilter(ContactListFilter contactListFilter) {
        this.mContactListFilterController.setContactListFilter(contactListFilter, isAllContactsFilter(contactListFilter));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (!this.mContactsAvailable || this.mActivity.isInSecondLevel()) {
            return;
        }
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.people_options, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.mOptionMenu = menu;
        this.mOptionsMenuContactsAvailable = this.mContactsAvailable;
        if (!this.mOptionsMenuContactsAvailable) {
            return;
        }
        boolean z = this.mActionBarAdapter.isSearchMode() || this.mActionBarAdapter.isSelectionMode();
        makeMenuItemVisible(menu, R.id.menu_search, !z);
        boolean z2 = this.mActionBarAdapter.isSelectionMode() && getSelectedContactIds().size() != 0;
        makeMenuItemVisible(menu, R.id.menu_share, z2);
        makeMenuItemVisible(menu, R.id.menu_delete, z2);
        makeMenuItemVisible(menu, R.id.menu_join, this.mActionBarAdapter.isSelectionMode() && getSelectedContactIds().size() > 1);
        makeMenuItemVisible(menu, R.id.export_database, this.mEnableDebugMenuOptions && hasExportIntentHandler());
        for (int i = 0; i < menu.size(); i++) {
            Drawable icon = menu.getItem(i).getIcon();
            if (icon != null && !z) {
                icon.mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.actionbar_icon_color), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    private void makeMenuItemVisible(Menu menu, int i, boolean z) {
        MenuItem menuItemFindItem = menu.findItem(i);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(z);
        }
    }

    private boolean hasExportIntentHandler() {
        Intent intent = new Intent();
        intent.setAction("com.android.providers.contacts.DUMP_DATABASE");
        List<ResolveInfo> listQueryIntentActivities = getContext().getPackageManager().queryIntentActivities(intent, 65536);
        return listQueryIntentActivities != null && listQueryIntentActivities.size() > 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mDisableOptionItemSelected) {
            return false;
        }
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            if (this.mActionBarAdapter.isUpShowing()) {
                this.mActivity.onBackPressed();
            }
            return true;
        }
        if (itemId == R.id.menu_search) {
            if (!this.mActionBarAdapter.isSelectionMode()) {
                this.mActionBarAdapter.setSearchMode(true);
            }
            return true;
        }
        if (itemId == R.id.menu_share) {
            shareSelectedContacts();
            return true;
        }
        if (itemId == R.id.menu_join) {
            Logger.logListEvent(6, getListTypeIncludingSearch(), getAdapter().getCount(), -1, getAdapter().getSelectedContactIds().size());
            joinSelectedContacts();
            return true;
        }
        if (itemId == R.id.menu_delete) {
            if (this.mIsDeletionInProgress) {
                Log.e("DefaultListFragment", "ignore delete request due to deletion dialog already shown.");
            } else {
                deleteSelectedContacts();
            }
            return true;
        }
        if (itemId == R.id.export_database) {
            Intent intent = new Intent("com.android.providers.contacts.DUMP_DATABASE");
            intent.setFlags(524288);
            ImplicitIntentsUtil.startActivityOutsideApp(getContext(), intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void shareSelectedContacts() {
        Log.d("DefaultListFragment", "[shareSelectedContacts]...");
        StringBuilder sb = new StringBuilder();
        Iterator<Long> it = getSelectedContactIds().iterator();
        while (it.hasNext()) {
            Uri lookupUri = ContactsContract.Contacts.getLookupUri(getContext().getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, it.next().longValue()));
            if (lookupUri != null) {
                List<String> pathSegments = lookupUri.getPathSegments();
                if (pathSegments.size() >= 2) {
                    String str = pathSegments.get(pathSegments.size() - 2);
                    if (sb.length() > 0) {
                        sb.append(':');
                    }
                    sb.append(Uri.encode(str));
                }
            }
        }
        if (sb.length() == 0) {
            return;
        }
        Uri uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(sb.toString()));
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/x-vcard");
        intent.putExtra("android.intent.extra.STREAM", uriWithAppendedPath);
        intent.putExtra("CALLING_ACTIVITY", PeopleActivity.class.getName());
        try {
            startActivityForResult(Intent.createChooser(intent, getResources().getQuantityString(R.plurals.title_share_via, getSelectedContactIds().size())), 0);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), R.string.share_error, 0).show();
        }
    }

    private void joinSelectedContacts() {
        Log.d("DefaultListFragment", "[joinSelectedContacts]...");
        Context context = getContext();
        context.startService(ContactSaveService.createJoinSeveralContactsIntent(context, getSelectedContactIdsArray()));
        this.mActionBarAdapter.setSelectionMode(false);
    }

    private void deleteSelectedContacts() {
        Log.d("DefaultListFragment", "[deleteSelectedContacts]...");
        ContactMultiDeletionInteraction.start(this, getSelectedContactIds()).setListener(new MultiDeleteListener());
        this.mIsDeletionInProgress = true;
    }

    private final class MultiDeleteListener implements ContactMultiDeletionInteraction.MultiContactDeleteListener {
        private MultiDeleteListener() {
        }

        @Override
        public void onDeletionFinished() {
            Logger.logListEvent(5, DefaultContactBrowseListFragment.this.getListTypeIncludingSearch(), DefaultContactBrowseListFragment.this.getAdapter().getCount(), -1, DefaultContactBrowseListFragment.this.getSelectedContactIds().size());
            DefaultContactBrowseListFragment.this.mActionBarAdapter.setSelectionMode(false);
            DefaultContactBrowseListFragment.this.mIsDeletionInProgress = false;
            Log.d("DefaultListFragment", "[MultiDeleteListener]onDeletionFinished:" + DefaultContactBrowseListFragment.this.mIsDeletionInProgress);
        }

        @Override
        public void onDeletionCancelled() {
            DefaultContactBrowseListFragment.this.mIsDeletionInProgress = false;
            Log.d("DefaultListFragment", "[MultiDeleteListener]onDeletionCancelled:" + DefaultContactBrowseListFragment.this.mIsDeletionInProgress);
        }
    }

    private int getListTypeIncludingSearch() {
        if (isSearchMode()) {
            return 4;
        }
        return getListType();
    }

    public void setParameters(ContactsRequest contactsRequest, boolean z) {
        this.mContactsRequest = contactsRequest;
        this.mFromOnNewIntent = z;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        switch (i) {
            case 0:
                break;
            case 1:
                if (i2 == -1) {
                    onPickerResult(intent);
                }
                break;
            default:
                return;
        }
        Logger.logListEvent(4, getListTypeIncludingSearch(), getAdapter().getCount(), -1, getAdapter().getSelectedContactIds().size());
    }

    public boolean getOptionsMenuContactsAvailable() {
        return this.mOptionsMenuContactsAvailable;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.setListener(null);
            this.mActionBarAdapter.onSaveInstanceState(bundle);
        }
        this.mDisableOptionItemSelected = true;
        bundle.putBoolean("deletionInProgress", this.mIsDeletionInProgress);
        bundle.putBoolean("search_result_clicked", this.mSearchResultClicked);
    }

    @Override
    public void onPause() {
        this.mOptionsMenuContactsAvailable = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.setListener(null);
        }
        super.onDestroy();
    }

    public boolean onKeyDown(int i) {
        if (this.mActionBarAdapter != null && this.mActionBarAdapter.isSelectionMode()) {
            return true;
        }
        if (this.mActionBarAdapter == null || this.mActionBarAdapter.isSearchMode()) {
            return false;
        }
        String str = new String(new int[]{i}, 0, 1);
        this.mActionBarAdapter.setSearchMode(true);
        this.mActionBarAdapter.setQueryString(str);
        return true;
    }

    public boolean canSetActionBar() {
        return this.mCanSetActionBar;
    }
}
