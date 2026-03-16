package com.android.contacts.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.SimPhonebookService;
import com.android.contacts.SimPhonebookService2;
import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.dialog.ClearFrequentsDialog;
import com.android.contacts.common.interactions.ImportExportDialogFragment;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactTileAdapter;
import com.android.contacts.common.list.ViewPagerTabs;
import com.android.contacts.common.preference.DisplayOptionsPreferenceFragment;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.common.util.ViewUtil;
import com.android.contacts.list.ContactTileListFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.list.OnContactBrowserActionListener;
import com.android.contacts.list.OnContactsUnavailableActionListener;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.preference.ContactsPreferenceActivity;
import com.android.contacts.quickcontact.QuickContactActivity;
import com.android.contacts.util.AccountPromptUtils;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.HelpUtils;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class PeopleActivity extends ContactsActivity implements View.OnClickListener, View.OnCreateContextMenuListener, ActionBarAdapter.Listener, ContactListFilterController.ContactListFilterListener, ProviderStatusWatcher.ProviderStatusListener, DialogManager.DialogShowingViewActivity {
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();
    private ActionBarAdapter mActionBarAdapter;
    private DefaultContactBrowseListFragment mAllFragment;
    private ContactListFilterController mContactListFilterController;
    private ContactsUnavailableFragment mContactsUnavailableFragment;
    private boolean mDisableOptionItemSelected;
    private boolean mEnableDebugMenuOptions;
    private ContactTileListFragment mFavoritesFragment;
    private boolean mFragmentInitialized;
    private boolean mIsRecreatedInstance;
    private boolean mOptionsMenuContactsAvailable;
    private ProviderStatusWatcher.Status mProviderStatus;
    private ContactsRequest mRequest;
    private ViewPager mTabPager;
    private TabPagerAdapter mTabPagerAdapter;
    private String[] mTabTitles;
    private ViewPagerTabs mViewPagerTabs;
    private final DialogManager mDialogManager = new DialogManager(this);
    protected final Object mLock = new Object();
    private ContactTileListFragment.Listener mFavoritesFragmentListener = new StrequentContactListFragmentListener();
    private final TabPagerListener mTabPagerListener = new TabPagerListener();
    private final int mInstanceId = sNextInstanceId.getAndIncrement();
    private ContactsIntentResolver mIntentResolver = new ContactsIntentResolver(this);
    private ProviderStatusWatcher mProviderStatusWatcher = ProviderStatusWatcher.getInstance(this);

    public String toString() {
        return String.format("%s@%d", getClass().getSimpleName(), Integer.valueOf(this.mInstanceId));
    }

    public boolean areContactsAvailable() {
        return this.mProviderStatus != null && this.mProviderStatus.status == 0;
    }

    private boolean areContactWritableAccountsAvailable() {
        return ContactsUtils.areContactWritableAccountsAvailable(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactsUnavailableFragment) {
            this.mContactsUnavailableFragment = (ContactsUnavailableFragment) fragment;
            this.mContactsUnavailableFragment.setOnContactsUnavailableActionListener(new ContactsUnavailableFragmentListener());
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "PeopleActivity.onCreate start");
        }
        super.onCreate(savedState);
        if (!processIntent(false)) {
            finish();
            return;
        }
        this.mContactListFilterController = ContactListFilterController.getInstance(this);
        this.mContactListFilterController.checkFilterValidity(false);
        this.mContactListFilterController.addListener(this);
        this.mProviderStatusWatcher.addListener(this);
        this.mIsRecreatedInstance = savedState != null;
        createViewsAndFragments(savedState);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "PeopleActivity.onCreate finish");
        }
        getWindow().setBackgroundDrawable(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        intent.getAction();
        setIntent(intent);
        if ("reboot".equals(intent.getAction())) {
            Log.d("PeopleActivity", "need reboot");
            Process.killProcess(Process.myPid());
        } else {
            if (!processIntent(true)) {
                finish();
                return;
            }
            this.mActionBarAdapter.initialize(null, this.mRequest);
            this.mContactListFilterController.checkFilterValidity(false);
            configureFragments(true);
            invalidateOptionsMenuIfNeeded();
        }
    }

    private boolean processIntent(boolean forNewIntent) {
        this.mRequest = this.mIntentResolver.resolveIntent(getIntent());
        if (Log.isLoggable("PeopleActivity", 3)) {
            Log.d("PeopleActivity", this + " processIntent: forNewIntent=" + forNewIntent + " intent=" + getIntent() + " request=" + this.mRequest);
        }
        if (!this.mRequest.isValid()) {
            setResult(0);
            return false;
        }
        Intent redirect = this.mRequest.getRedirectIntent();
        if (redirect != null) {
            startActivity(redirect);
            return false;
        }
        if (this.mRequest.getActionCode() == 140) {
            Intent redirect2 = new Intent(this, (Class<?>) QuickContactActivity.class);
            redirect2.setAction("android.intent.action.VIEW");
            redirect2.setData(this.mRequest.getContactUri());
            startActivity(redirect2);
            return false;
        }
        return true;
    }

    private void createViewsAndFragments(Bundle savedState) {
        getWindow().requestFeature(1);
        setContentView(R.layout.people_activity);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        this.mTabTitles = new String[2];
        this.mTabTitles[0] = getString(R.string.favorites_tab_label);
        this.mTabTitles[1] = getString(R.string.all_contacts_tab_label);
        this.mTabPager = (ViewPager) getView(R.id.tab_pager);
        this.mTabPagerAdapter = new TabPagerAdapter();
        this.mTabPager.setAdapter(this.mTabPagerAdapter);
        this.mTabPager.setOnPageChangeListener(this.mTabPagerListener);
        Toolbar toolbar = (Toolbar) getView(R.id.toolbar);
        setActionBar(toolbar);
        ViewPagerTabs portraitViewPagerTabs = (ViewPagerTabs) findViewById(R.id.lists_pager_header);
        ViewPagerTabs landscapeViewPagerTabs = null;
        if (portraitViewPagerTabs == null) {
            landscapeViewPagerTabs = (ViewPagerTabs) getLayoutInflater().inflate(R.layout.people_activity_tabs_lands, (ViewGroup) toolbar, false);
            this.mViewPagerTabs = landscapeViewPagerTabs;
        } else {
            this.mViewPagerTabs = portraitViewPagerTabs;
        }
        this.mViewPagerTabs.setViewPager(this.mTabPager);
        this.mFavoritesFragment = (ContactTileListFragment) fragmentManager.findFragmentByTag("tab-pager-favorite");
        this.mAllFragment = (DefaultContactBrowseListFragment) fragmentManager.findFragmentByTag("tab-pager-all");
        if (this.mFavoritesFragment == null) {
            this.mFavoritesFragment = new ContactTileListFragment();
            this.mAllFragment = new DefaultContactBrowseListFragment();
            transaction.add(R.id.tab_pager, this.mFavoritesFragment, "tab-pager-favorite");
            transaction.add(R.id.tab_pager, this.mAllFragment, "tab-pager-all");
        }
        this.mFavoritesFragment.setListener(this.mFavoritesFragmentListener);
        this.mAllFragment.setOnContactListActionListener(new ContactBrowserActionListener());
        transaction.hide(this.mFavoritesFragment);
        transaction.hide(this.mAllFragment);
        transaction.commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
        this.mFavoritesFragment.setDisplayType(ContactTileAdapter.DisplayType.STREQUENT);
        this.mActionBarAdapter = new ActionBarAdapter(this, this, getActionBar(), portraitViewPagerTabs, landscapeViewPagerTabs, toolbar);
        this.mActionBarAdapter.initialize(savedState, this.mRequest);
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());
        View floatingActionButtonContainer = findViewById(R.id.floating_action_button_container);
        ViewUtil.setupFloatingActionButton(floatingActionButtonContainer, getResources());
        ImageButton floatingActionButton = (ImageButton) findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(this);
        invalidateOptionsMenuIfNeeded();
    }

    @Override
    protected void onStart() {
        if (!this.mFragmentInitialized) {
            this.mFragmentInitialized = true;
            configureFragments(this.mIsRecreatedInstance ? false : true);
        }
        super.onStart();
    }

    @Override
    protected void onPause() {
        this.mOptionsMenuContactsAvailable = false;
        this.mProviderStatusWatcher.stop();
        super.onPause();
    }

    private void tryToLoadSimPB() {
        long timeSimReady = System.currentTimeMillis();
        Log.d("PeopleActivity", "[+]onResume:");
        Log.d("PeopleActivity", "[+]timeSimReady:" + timeSimReady);
        Intent serviceIntent = new Intent(getBaseContext(), (Class<?>) SimPhonebookService.class);
        serviceIntent.setAction("loadSimPhonebook");
        getBaseContext().startService(serviceIntent);
        TelephonyManager tm = TelephonyManager.from(this);
        if (tm != null && tm.isMultiSimEnabled()) {
            long timeSimReady2 = System.currentTimeMillis();
            Log.d("PeopleActivity", "[+]onResume:");
            Log.d("PeopleActivity", "[+]timeSimReady2:" + timeSimReady2);
            Intent serviceIntent2 = new Intent(getBaseContext(), (Class<?>) SimPhonebookService2.class);
            serviceIntent2.setAction("loadSimPhonebook");
            getBaseContext().startService(serviceIntent2);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryToLoadSimPB();
        this.mProviderStatusWatcher.start();
        updateViewConfiguration(true);
        this.mActionBarAdapter.setListener(this);
        this.mDisableOptionItemSelected = false;
        if (this.mTabPager != null) {
            this.mTabPager.setOnPageChangeListener(this.mTabPagerListener);
        }
        updateFragmentsVisibility();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.mProviderStatusWatcher.removeListener(this);
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.setListener(null);
        }
        if (this.mContactListFilterController != null) {
            this.mContactListFilterController.removeListener(this);
        }
        super.onDestroy();
    }

    private void configureFragments(boolean fromRequest) {
        int tabToOpen;
        if (fromRequest) {
            ContactListFilter filter = null;
            int actionCode = this.mRequest.getActionCode();
            boolean searchMode = this.mRequest.isSearchMode();
            switch (actionCode) {
                case 15:
                    filter = ContactListFilter.createFilterWithType(-2);
                    tabToOpen = 1;
                    break;
                case 17:
                    filter = ContactListFilter.createFilterWithType(-5);
                    tabToOpen = 1;
                    break;
                case 30:
                case 40:
                case 50:
                    tabToOpen = 0;
                    break;
                case 140:
                    tabToOpen = 1;
                    break;
                default:
                    tabToOpen = -1;
                    break;
            }
            if (tabToOpen != -1) {
                this.mActionBarAdapter.setCurrentTab(tabToOpen);
            }
            if (filter != null) {
                this.mContactListFilterController.setContactListFilter(filter, false);
                searchMode = false;
            }
            if (this.mRequest.getContactUri() != null) {
                searchMode = false;
            }
            this.mActionBarAdapter.setSearchMode(searchMode);
            configureContactListFragmentForRequest();
        }
        configureContactListFragment();
        invalidateOptionsMenuIfNeeded();
    }

    @Override
    public void onContactListFilterChanged() {
        if (this.mAllFragment != null && this.mAllFragment.isAdded()) {
            this.mAllFragment.setFilter(this.mContactListFilterController.getFilter());
            invalidateOptionsMenuIfNeeded();
        }
    }

    @Override
    public void onAction(int action) {
        switch (action) {
            case 0:
                String queryString = this.mActionBarAdapter.getQueryString();
                setQueryTextToFragment(queryString);
                updateDebugOptionsVisibility("debug debug!".equals(queryString));
                return;
            case 1:
                configureFragments(false);
                updateFragmentsVisibility();
                invalidateOptionsMenu();
                return;
            case 2:
                setQueryTextToFragment("");
                updateFragmentsVisibility();
                invalidateOptionsMenu();
                return;
            default:
                throw new IllegalStateException("Unkonwn ActionBarAdapter action: " + action);
        }
    }

    @Override
    public void onSelectedTabChanged() {
        updateFragmentsVisibility();
    }

    @Override
    public void onUpButtonPressed() {
        onBackPressed();
    }

    private void updateDebugOptionsVisibility(boolean visible) {
        if (this.mEnableDebugMenuOptions != visible) {
            this.mEnableDebugMenuOptions = visible;
            invalidateOptionsMenu();
        }
    }

    private void updateFragmentsVisibility() {
        int tab = this.mActionBarAdapter.getCurrentTab();
        if (this.mActionBarAdapter.isSearchMode()) {
            this.mTabPagerAdapter.setSearchMode(true);
        } else {
            boolean wasSearchMode = this.mTabPagerAdapter.isSearchMode();
            this.mTabPagerAdapter.setSearchMode(false);
            if (this.mTabPager.getCurrentItem() != tab) {
                this.mTabPager.setCurrentItem(tab, wasSearchMode ? false : true);
            }
        }
        invalidateOptionsMenu();
        showEmptyStateForTab(tab);
    }

    private void showEmptyStateForTab(int tab) {
        if (this.mContactsUnavailableFragment != null) {
            switch (getTabPositionForTextDirection(tab)) {
                case 0:
                    this.mContactsUnavailableFragment.setMessageText(R.string.listTotalAllContactsZeroStarred, -1);
                    break;
                case 1:
                    this.mContactsUnavailableFragment.setMessageText(R.string.noContacts, -1);
                    break;
            }
            this.mViewPagerTabs.onPageScrolled(tab, 0.0f, 0);
        }
    }

    private class TabPagerListener implements ViewPager.OnPageChangeListener {
        TabPagerListener() {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (!PeopleActivity.this.mTabPagerAdapter.isSearchMode()) {
                PeopleActivity.this.mViewPagerTabs.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (!PeopleActivity.this.mTabPagerAdapter.isSearchMode()) {
                PeopleActivity.this.mViewPagerTabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (!PeopleActivity.this.mTabPagerAdapter.isSearchMode()) {
                PeopleActivity.this.mActionBarAdapter.setCurrentTab(position, false);
                PeopleActivity.this.mViewPagerTabs.onPageSelected(position);
                PeopleActivity.this.showEmptyStateForTab(position);
                PeopleActivity.this.invalidateOptionsMenu();
            }
        }
    }

    private class TabPagerAdapter extends PagerAdapter {
        private FragmentTransaction mCurTransaction = null;
        private Fragment mCurrentPrimaryItem;
        private final FragmentManager mFragmentManager;
        private boolean mTabPagerAdapterSearchMode;

        public TabPagerAdapter() {
            this.mFragmentManager = PeopleActivity.this.getFragmentManager();
        }

        public boolean isSearchMode() {
            return this.mTabPagerAdapterSearchMode;
        }

        public void setSearchMode(boolean searchMode) {
            if (searchMode != this.mTabPagerAdapterSearchMode) {
                this.mTabPagerAdapterSearchMode = searchMode;
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return this.mTabPagerAdapterSearchMode ? 1 : 2;
        }

        @Override
        public int getItemPosition(Object object) {
            if (this.mTabPagerAdapterSearchMode) {
                if (object == PeopleActivity.this.mAllFragment) {
                    return 0;
                }
            } else {
                if (object == PeopleActivity.this.mFavoritesFragment) {
                    return PeopleActivity.this.getTabPositionForTextDirection(0);
                }
                if (object == PeopleActivity.this.mAllFragment) {
                    return PeopleActivity.this.getTabPositionForTextDirection(1);
                }
            }
            return -2;
        }

        @Override
        public void startUpdate(ViewGroup container) {
        }

        private Fragment getFragment(int position) {
            int position2 = PeopleActivity.this.getTabPositionForTextDirection(position);
            if (this.mTabPagerAdapterSearchMode) {
                if (position2 != 0) {
                    Log.w("PeopleActivity", "Request fragment at position=" + position2 + ", eventhough we are in search mode");
                }
                return PeopleActivity.this.mAllFragment;
            }
            if (position2 == 0) {
                return PeopleActivity.this.mFavoritesFragment;
            }
            if (position2 == 1) {
                return PeopleActivity.this.mAllFragment;
            }
            throw new IllegalArgumentException("position: " + position2);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (this.mCurTransaction == null) {
                this.mCurTransaction = this.mFragmentManager.beginTransaction();
            }
            Fragment f = getFragment(position);
            this.mCurTransaction.show(f);
            f.setUserVisibleHint(f == this.mCurrentPrimaryItem);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (this.mCurTransaction == null) {
                this.mCurTransaction = this.mFragmentManager.beginTransaction();
            }
            this.mCurTransaction.hide((Fragment) object);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (this.mCurTransaction != null) {
                this.mCurTransaction.commitAllowingStateLoss();
                this.mCurTransaction = null;
                this.mFragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((Fragment) object).getView() == view;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            Fragment fragment = (Fragment) object;
            if (this.mCurrentPrimaryItem != fragment) {
                if (this.mCurrentPrimaryItem != null) {
                    this.mCurrentPrimaryItem.setUserVisibleHint(false);
                }
                if (fragment != null) {
                    fragment.setUserVisibleHint(true);
                }
                this.mCurrentPrimaryItem = fragment;
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return PeopleActivity.this.mTabTitles[position];
        }
    }

    private void setQueryTextToFragment(String query) {
        this.mAllFragment.setQueryString(query, true);
        this.mAllFragment.setVisibleScrollbarEnabled(this.mAllFragment.isSearchMode() ? false : true);
    }

    private void configureContactListFragmentForRequest() {
        Uri contactUri = this.mRequest.getContactUri();
        if (contactUri != null) {
            this.mAllFragment.setSelectedContactUri(contactUri);
        }
        this.mAllFragment.setFilter(this.mContactListFilterController.getFilter());
        setQueryTextToFragment(this.mActionBarAdapter.getQueryString());
        if (this.mRequest.isDirectorySearchEnabled()) {
            this.mAllFragment.setDirectorySearchMode(1);
        } else {
            this.mAllFragment.setDirectorySearchMode(0);
        }
    }

    private void configureContactListFragment() {
        this.mAllFragment.setFilter(this.mContactListFilterController.getFilter());
        this.mAllFragment.setVerticalScrollbarPosition(getScrollBarPosition());
        this.mAllFragment.setSelectionVisible(false);
    }

    private int getScrollBarPosition() {
        return isRTL() ? 1 : 2;
    }

    private boolean isRTL() {
        Locale locale = Locale.getDefault();
        return TextUtils.getLayoutDirectionFromLocale(locale) == 1;
    }

    @Override
    public void onProviderStatusChange() {
        updateViewConfiguration(false);
    }

    private void updateViewConfiguration(boolean forceUpdate) {
        ProviderStatusWatcher.Status providerStatus = this.mProviderStatusWatcher.getProviderStatus();
        if (forceUpdate || this.mProviderStatus == null || providerStatus.status != this.mProviderStatus.status) {
            this.mProviderStatus = providerStatus;
            View contactsUnavailableView = findViewById(R.id.contacts_unavailable_view);
            if (this.mProviderStatus.status == 0) {
                contactsUnavailableView.setVisibility(8);
                if (this.mTabPager != null) {
                    this.mTabPager.setVisibility(0);
                }
                if (this.mAllFragment != null) {
                    this.mAllFragment.setEnabled(true);
                }
            } else {
                UserManager userManager = UserManager.get(this);
                boolean disallowModifyAccounts = userManager.getUserRestrictions().getBoolean("no_modify_accounts");
                if (!disallowModifyAccounts && !areContactWritableAccountsAvailable() && AccountPromptUtils.shouldShowAccountPrompt(this)) {
                    AccountPromptUtils.neverShowAccountPromptAgain(this);
                    AccountPromptUtils.launchAccountPrompt(this);
                    return;
                }
                if (this.mAllFragment != null) {
                    this.mAllFragment.setEnabled(false);
                }
                if (this.mContactsUnavailableFragment == null) {
                    this.mContactsUnavailableFragment = new ContactsUnavailableFragment();
                    this.mContactsUnavailableFragment.setOnContactsUnavailableActionListener(new ContactsUnavailableFragmentListener());
                    getFragmentManager().beginTransaction().replace(R.id.contacts_unavailable_container, this.mContactsUnavailableFragment).commitAllowingStateLoss();
                }
                this.mContactsUnavailableFragment.updateStatus(this.mProviderStatus);
                contactsUnavailableView.setVisibility(0);
                if (this.mTabPager != null) {
                    this.mTabPager.setVisibility(8);
                }
                showEmptyStateForTab(this.mActionBarAdapter.getCurrentTab());
            }
            invalidateOptionsMenuIfNeeded();
        }
    }

    private final class ContactBrowserActionListener implements OnContactBrowserActionListener {
        ContactBrowserActionListener() {
        }

        @Override
        public void onSelectionChange() {
        }

        @Override
        public void onViewContactAction(Uri contactLookupUri) {
            Intent intent = ContactsContract.QuickContact.composeQuickContactsIntent(PeopleActivity.this, (Rect) null, contactLookupUri, 4, null);
            PeopleActivity.this.startActivity(intent);
        }

        @Override
        public void onInvalidSelection() {
            ContactListFilter filter;
            ContactListFilter currentFilter = PeopleActivity.this.mAllFragment.getFilter();
            if (currentFilter != null && currentFilter.filterType == -6) {
                filter = ContactListFilter.createFilterWithType(-2);
                PeopleActivity.this.mAllFragment.setFilter(filter);
            } else {
                filter = ContactListFilter.createFilterWithType(-6);
                PeopleActivity.this.mAllFragment.setFilter(filter, false);
            }
            PeopleActivity.this.mContactListFilterController.setContactListFilter(filter, true);
        }
    }

    private class ContactsUnavailableFragmentListener implements OnContactsUnavailableActionListener {
        ContactsUnavailableFragmentListener() {
        }

        @Override
        public void onCreateNewContactAction() {
            PeopleActivity.this.startActivity(new Intent("android.intent.action.INSERT", ContactsContract.Contacts.CONTENT_URI));
        }

        @Override
        public void onAddAccountAction() {
            if (BenesseExtension.getDchaState() == 0) {
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.setFlags(524288);
                intent.putExtra("authorities", new String[]{"com.android.contacts"});
                PeopleActivity.this.startActivity(intent);
            }
        }

        @Override
        public void onImportContactsFromFileAction() {
            ImportExportDialogFragment.show(PeopleActivity.this.getFragmentManager(), PeopleActivity.this.areContactsAvailable(), PeopleActivity.class);
        }

        @Override
        public void onFreeInternalStorageAction() {
            if (BenesseExtension.getDchaState() == 0) {
                PeopleActivity.this.startActivity(new Intent("android.settings.MANAGE_APPLICATIONS_SETTINGS"));
            }
        }
    }

    private final class StrequentContactListFragmentListener implements ContactTileListFragment.Listener {
        StrequentContactListFragmentListener() {
        }

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            Intent intent = ContactsContract.QuickContact.composeQuickContactsIntent(PeopleActivity.this, targetRect, contactUri, 4, null);
            PeopleActivity.this.startActivity(intent);
            Log.d("ContactsPerf", "contactUri = " + contactUri);
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            Log.w("PeopleActivity", "unexpected invocation of onCallNumberDirectly()");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!areContactsAvailable()) {
            return false;
        }
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.people_options, menu);
        return true;
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (isOptionsMenuChanged()) {
            invalidateOptionsMenu();
        }
    }

    public boolean isOptionsMenuChanged() {
        if (this.mOptionsMenuContactsAvailable != areContactsAvailable()) {
            return true;
        }
        return this.mAllFragment != null && this.mAllFragment.isOptionsMenuChanged();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean z = false;
        this.mOptionsMenuContactsAvailable = areContactsAvailable();
        if (!this.mOptionsMenuContactsAvailable) {
            return false;
        }
        MenuItem contactsFilterMenu = menu.findItem(R.id.menu_contacts_filter);
        MenuItem clearFrequentsMenu = menu.findItem(R.id.menu_clear_frequents);
        MenuItem helpMenu = menu.findItem(R.id.menu_help);
        boolean isSearchMode = this.mActionBarAdapter.isSearchMode();
        if (isSearchMode) {
            contactsFilterMenu.setVisible(false);
            clearFrequentsMenu.setVisible(false);
            helpMenu.setVisible(false);
        } else {
            switch (getTabPositionForTextDirection(this.mActionBarAdapter.getCurrentTab())) {
                case 0:
                    contactsFilterMenu.setVisible(false);
                    clearFrequentsMenu.setVisible(hasFrequents());
                    break;
                case 1:
                    contactsFilterMenu.setVisible(true);
                    clearFrequentsMenu.setVisible(false);
                    break;
            }
            HelpUtils.prepareHelpMenuItem(this, helpMenu, R.string.help_url_people_main);
        }
        boolean showMiscOptions = !isSearchMode;
        makeMenuItemVisible(menu, R.id.menu_search, showMiscOptions);
        makeMenuItemVisible(menu, R.id.menu_import_export, showMiscOptions);
        makeMenuItemVisible(menu, R.id.menu_accounts, showMiscOptions);
        if (showMiscOptions && !ContactsPreferenceActivity.isEmpty(this)) {
            z = true;
        }
        makeMenuItemVisible(menu, R.id.menu_settings, z);
        makeMenuItemVisible(menu, R.id.export_database, this.mEnableDebugMenuOptions);
        return true;
    }

    private boolean hasFrequents() {
        return this.mFavoritesFragment.hasFrequents();
    }

    private void makeMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.mDisableOptionItemSelected) {
            return false;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                if (this.mActionBarAdapter.isUpShowing()) {
                    onBackPressed();
                }
                return true;
            case R.id.menu_search:
                onSearchRequested();
                return true;
            case R.id.menu_contacts_filter:
                AccountFilterUtil.startAccountFilterActivityForResult(this, 2, this.mContactListFilterController.getFilter());
                return true;
            case R.id.menu_import_export:
                ImportExportDialogFragment.show(getFragmentManager(), areContactsAvailable(), PeopleActivity.class);
                return true;
            case R.id.menu_clear_frequents:
                ClearFrequentsDialog.show(getFragmentManager());
                return true;
            case R.id.menu_accounts:
                if (BenesseExtension.getDchaState() != 0) {
                    return true;
                }
                Intent intent = new Intent("android.settings.SYNC_SETTINGS");
                intent.putExtra("authorities", new String[]{"com.android.contacts"});
                intent.setFlags(524288);
                startActivity(intent);
                return true;
            case R.id.menu_settings:
                Intent intent2 = new Intent(this, (Class<?>) ContactsPreferenceActivity.class);
                intent2.putExtra(":android:show_fragment", DisplayOptionsPreferenceFragment.class.getName());
                intent2.putExtra(":android:show_fragment_title", R.string.activity_title_settings);
                startActivity(intent2);
                return true;
            case R.id.export_database:
                Intent intent3 = new Intent("com.android.providers.contacts.DUMP_DATABASE");
                intent3.setFlags(524288);
                startActivity(intent3);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onSearchRequested() {
        this.mActionBarAdapter.setSearchMode(true);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == -1) {
                    this.mAllFragment.onPickerResult(data);
                }
                break;
            case 2:
                AccountFilterUtil.handleAccountFilterResult(this.mContactListFilterController, resultCode, data);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 67:
                if (deleteSelection()) {
                    return true;
                }
                break;
            default:
                int unicodeChar = event.getUnicodeChar();
                if (unicodeChar != 0 && (Integer.MIN_VALUE & unicodeChar) == 0 && !Character.isWhitespace(unicodeChar)) {
                    String query = new String(new int[]{unicodeChar}, 0, 1);
                    if (!this.mActionBarAdapter.isSearchMode()) {
                        this.mActionBarAdapter.setSearchMode(true);
                        this.mActionBarAdapter.setQueryString(query);
                        return true;
                    }
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (this.mActionBarAdapter.isSearchMode()) {
            this.mActionBarAdapter.setSearchMode(false);
        } else {
            super.onBackPressed();
        }
    }

    private boolean deleteSelection() {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mActionBarAdapter.onSaveInstanceState(outState);
        this.mDisableOptionItemSelected = true;
        this.mActionBarAdapter.setListener(null);
        if (this.mTabPager != null) {
            this.mTabPager.setOnPageChangeListener(null);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (this.mActionBarAdapter.isSearchMode()) {
            this.mActionBarAdapter.setFocusOnSearchView();
        }
    }

    @Override
    public DialogManager getDialogManager() {
        return this.mDialogManager;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.floating_action_button:
                Intent intent = new Intent("android.intent.action.INSERT", ContactsContract.Contacts.CONTENT_URI);
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    intent.putExtras(extras);
                }
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.missing_app, 0).show();
                    return;
                }
                break;
            default:
                Log.wtf("PeopleActivity", "Unexpected onClick event from " + view);
                break;
        }
    }

    private int getTabPositionForTextDirection(int position) {
        if (isRTL()) {
            return 1 - position;
        }
        return position;
    }
}
