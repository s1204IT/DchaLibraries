package com.android.contacts.activities;

import android.accounts.Account;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.Toast;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.drawer.DrawerFragment;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.group.GroupMembersFragment;
import com.android.contacts.group.GroupNameEditDialogFragment;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.logging.Logger;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferenceActivity;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.SharedPreferenceUtil;
import com.android.contacts.util.SyncUtil;
import com.android.contacts.util.ViewUtil;
import com.android.contacts.widget.FloatingActionButtonController;
import com.android.contactsbind.FeatureHighlightHelper;
import com.android.contactsbind.HelpUtils;
import com.android.contactsbind.ObjectFactory;
import com.google.common.util.concurrent.Futures;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.model.AccountTypeManagerEx;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimGroupProcessor;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.PDebug;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PeopleActivity extends AppCompatContactsActivity implements DrawerFragment.DrawerFragmentListener, SelectAccountDialogFragment.Listener, SimGroupProcessor.Listener {
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();
    private AccountTypeManager mAccountTypeManager;
    private ContactListFilterController mContactListFilterController;
    private DefaultContactBrowseListFragment mContactsListFragment;
    private ContactsView mCurrentView;
    private DrawerFragment mDrawerFragment;
    private DrawerLayout mDrawerLayout;
    private View mFloatingActionButtonContainer;
    private FloatingActionButtonController mFloatingActionButtonController;
    private Uri mGroupUri;
    private final int mInstanceId;
    private ContactsIntentResolver mIntentResolver;
    private boolean mIsRecreatedInstance;
    private CoordinatorLayout mLayoutRoot;
    private GroupMembersFragment mMembersFragment;
    private AccountWithDataSet mNewGroupAccount;
    private Integer mProviderStatus;
    private ProviderStatusWatcher mProviderStatusWatcher;
    private ContactsRequest mRequest;
    private BroadcastReceiver mSaveServiceListener;
    private boolean mShouldSwitchToAllContacts;
    private boolean mShouldSwitchToGroupView;
    private Object mStatusChangeListenerHandle;
    private ContactsActionBarDrawerToggle mToggle;
    private Toolbar mToolbar;
    private boolean wasLastFabAnimationScaleIn = false;
    private final Handler mHandler = new Handler();
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int i) {
            PeopleActivity.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PeopleActivity.this.onSyncStateUpdated();
                }
            });
        }
    };
    private boolean mResumed = false;
    private final ContactListFilterController.ContactListFilterListener mFilterListener = new ContactListFilterController.ContactListFilterListener() {
        @Override
        public void onContactListFilterChanged() {
            ContactListFilter filter = PeopleActivity.this.mContactListFilterController.getFilter();
            PeopleActivity.this.handleFilterChangeForFragment(filter);
            PeopleActivity.this.handleFilterChangeForActivity(filter);
        }
    };
    private final ProviderStatusWatcher.ProviderStatusListener mProviderStatusListener = new ProviderStatusWatcher.ProviderStatusListener() {
        @Override
        public void onProviderStatusChange() {
            Log.d("PeopleActivity", "[onProviderStatusChange]");
            PeopleActivity.this.updateViewConfiguration(false);
        }
    };
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("PeopleActivity", "[onReceive] Received Intent:" + intent);
            PeopleActivity.this.closeMenusIfOpen(true, true);
            PeopleActivity.this.updateViewConfiguration(true);
            int intExtra = intent.getIntExtra("subscription", -1);
            GroupNameEditDialogFragment groupNameEditDialogFragment = (GroupNameEditDialogFragment) PeopleActivity.this.getFragmentManager().findFragmentByTag("groupNameEditDialog");
            if (groupNameEditDialogFragment != null) {
                int subId = groupNameEditDialogFragment.getSubId();
                Log.i("PeopleActivity", "[onReceive] subId=" + intExtra + ", groupSubId=" + subId);
                if (subId >= 0 && subId == intExtra) {
                    try {
                        groupNameEditDialogFragment.dismiss();
                    } catch (Exception e) {
                        Log.e("PeopleActivity", "Error dismissing progress dialog", e);
                    }
                }
            }
            GroupDeletionDialogFragment groupDeletionDialogFragment = (GroupDeletionDialogFragment) PeopleActivity.this.getFragmentManager().findFragmentByTag("deleteGroup");
            if (groupDeletionDialogFragment != null && intExtra >= 0 && intExtra == groupDeletionDialogFragment.getSubId()) {
                try {
                    groupDeletionDialogFragment.dismiss();
                } catch (Exception e2) {
                    Log.e("PeopleActivity", "Error dismissing deletion dialog", e2);
                }
            }
        }
    };

    public enum ContactsView {
        NONE,
        ALL_CONTACTS,
        ASSISTANT,
        GROUP_VIEW,
        ACCOUNT_VIEW
    }

    private void onSyncStateUpdated() {
        ContactListFilter filter;
        List<AccountWithDataSet> listEmptyList;
        if (!isListFragmentInSearchMode() && !isListFragmentInSelectionMode() && (filter = this.mContactListFilterController.getFilter()) != null) {
            SwipeRefreshLayout swipeRefreshLayout = this.mContactsListFragment.getSwipeRefreshLayout();
            if (swipeRefreshLayout == null) {
                if (Log.isLoggable("PeopleActivity", 3)) {
                    Log.d("PeopleActivity", "Can not load swipeRefreshLayout, swipeRefreshLayout is null");
                    return;
                }
                return;
            }
            if (filter.filterType == 0 && filter.isGoogleAccountType()) {
                listEmptyList = Collections.singletonList(new AccountWithDataSet(filter.accountName, filter.accountType, null));
            } else if (filter.shouldShowSyncState()) {
                listEmptyList = AccountInfo.extractAccounts(this.mAccountTypeManager.getWritableGoogleAccounts());
            } else {
                listEmptyList = Collections.emptyList();
            }
            if (SyncUtil.isAnySyncing(listEmptyList)) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public void showConnectionErrorMsg() {
        Snackbar.make(this.mLayoutRoot, R.string.connection_error_message, 0).show();
    }

    private class ContactsActionBarDrawerToggle extends ActionBarDrawerToggle {
        private boolean mMenuClickedBefore;

        public ContactsActionBarDrawerToggle(AppCompatActivity appCompatActivity, DrawerLayout drawerLayout, Toolbar toolbar, int i, int i2) {
            super(appCompatActivity, drawerLayout, toolbar, i, i2);
            this.mMenuClickedBefore = SharedPreferenceUtil.getHamburgerMenuClickedBefore(PeopleActivity.this);
        }

        @Override
        public void onDrawerOpened(View view) {
            super.onDrawerOpened(view);
            if (!this.mMenuClickedBefore) {
                SharedPreferenceUtil.setHamburgerMenuClickedBefore(PeopleActivity.this);
                this.mMenuClickedBefore = true;
            }
            if (PeopleActivity.this.mDrawerFragment != null) {
                PeopleActivity.this.mDrawerFragment.updateConfCallState();
            }
            view.requestFocus();
            PeopleActivity.this.invalidateOptionsMenu();
            stopSearchAndSelection();
            PeopleActivity.this.updateStatusBarBackground();
        }

        private void stopSearchAndSelection() {
            MultiSelectContactsListFragment listFragment;
            ActionBarAdapter actionBarAdapter;
            if (PeopleActivity.this.isAllContactsView() || PeopleActivity.this.isAccountView()) {
                listFragment = PeopleActivity.this.getListFragment();
            } else if (PeopleActivity.this.isGroupView()) {
                listFragment = PeopleActivity.this.getGroupFragment();
            } else {
                listFragment = null;
            }
            if (listFragment == null || (actionBarAdapter = listFragment.getActionBarAdapter()) == null) {
                return;
            }
            if (actionBarAdapter.isSearchMode()) {
                actionBarAdapter.setSearchMode(false);
            } else if (actionBarAdapter.isSelectionMode()) {
                actionBarAdapter.setSelectionMode(false);
            }
        }

        @Override
        public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            PeopleActivity.this.invalidateOptionsMenu();
        }

        @Override
        public void onDrawerStateChanged(int i) {
            super.onDrawerStateChanged(i);
            if (i != 0) {
                PeopleActivity.this.updateStatusBarBackground();
            }
        }
    }

    public PeopleActivity() {
        Log.w("PeopleActivity", "[PeopleActivity]new");
        this.mInstanceId = sNextInstanceId.getAndIncrement();
        this.mIntentResolver = new ContactsIntentResolver(this);
        this.mProviderStatusWatcher = ProviderStatusWatcher.getInstance(ContactsApplicationEx.getContactsApplication());
    }

    public String toString() {
        return String.format("%s@%d", getClass().getSimpleName(), Integer.valueOf(this.mInstanceId));
    }

    private boolean areContactsAvailable() {
        return this.mProviderStatus != null && this.mProviderStatus.equals(0);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        Log.w("PeopleActivity", "[onCreate] savedState=" + bundle);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "PeopleActivity.onCreate start");
        }
        setTheme(R.style.PeopleActivityTheme);
        super.onCreate(bundle);
        SimGroupProcessor.registerListener(this);
        this.mAccountTypeManager = AccountTypeManager.getInstance(this);
        this.mContactListFilterController = ContactListFilterController.getInstance(this);
        RequestPermissionsActivity.startPermissionActivityIfNeeded(this);
        Log.d("PeopleActivity", "[onCreate] refresh all SIM contacts");
        sendBroadcast(new Intent("com.android.contacts.REFRESH_SIM_CONTACT"));
        if (!processIntent(false)) {
            finish();
            Log.w("PeopleActivity", "[onCreate]can not process intent:" + getIntent());
            return;
        }
        Log.d("PeopleActivity", "[Performance test][Contacts] loading data start time: [" + System.currentTimeMillis() + "]");
        this.mContactListFilterController.checkFilterValidity(false);
        super.setContentView(R.layout.contacts_drawer_activity);
        this.mToolbar = (Toolbar) getView(R.id.toolbar);
        setSupportActionBar(this.mToolbar);
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());
        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.mDrawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.drawer);
        this.mToggle = new ContactsActionBarDrawerToggle(this, this.mDrawerLayout, this.mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.mDrawerLayout.setDrawerListener(this.mToggle);
        this.mToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PeopleActivity.this.onBackPressed();
            }
        });
        if (bundle != null) {
            this.mCurrentView = ContactsView.values()[bundle.getInt("contactsView")];
        } else {
            this.mCurrentView = ContactsView.ALL_CONTACTS;
        }
        if (bundle != null && bundle.containsKey("newGroupAccount")) {
            this.mNewGroupAccount = AccountWithDataSet.unstringify(bundle.getString("newGroupAccount"));
        }
        this.mContactListFilterController.addListener(this.mFilterListener);
        this.mProviderStatusWatcher.addListener(this.mProviderStatusListener);
        this.mIsRecreatedInstance = bundle != null;
        if (this.mIsRecreatedInstance) {
            this.mGroupUri = (Uri) bundle.getParcelable("groupUri");
        }
        PDebug.Start("createViewsAndFragments");
        createViewsAndFragments();
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "PeopleActivity.onCreate finish");
        }
        getWindow().setBackgroundDrawable(null);
        PDebug.End("Contacts.onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        PDebug.Start("onNewIntent");
        Log.d("PeopleActivity", "[onNewIntent] action = " + intent.getAction() + ", mGroupUri = " + intent.getData());
        boolean booleanExtra = intent.getBooleanExtra("haveToastDone", false);
        String action = intent.getAction();
        if ("createGroup".equals(action)) {
            this.mGroupUri = intent.getData();
            if (this.mGroupUri == null) {
                if (booleanExtra) {
                    return;
                }
                toast(R.string.groupSavedErrorToast);
                return;
            }
            if (Log.isLoggable("PeopleActivity", 2)) {
                Log.v("PeopleActivity", "Received group URI " + this.mGroupUri);
            }
            switchView(ContactsView.GROUP_VIEW);
            if (this.mMembersFragment != null) {
                this.mMembersFragment.toastForSaveAction(action);
                return;
            }
            return;
        }
        if (isGroupSaveAction(action)) {
            this.mGroupUri = intent.getData();
            if (this.mGroupUri == null) {
                if (!"updateGroup".equals(action)) {
                    popSecondLevel();
                }
                if (booleanExtra) {
                    return;
                }
                toast(R.string.groupSavedErrorToast);
                return;
            }
            if (Log.isLoggable("PeopleActivity", 2)) {
                Log.v("PeopleActivity", "Received group URI " + this.mGroupUri);
            }
            if (GroupUtil.ACTION_REMOVE_FROM_GROUP.equals(action)) {
                switchToOrUpdateGroupView(action);
            } else {
                switchView(ContactsView.GROUP_VIEW);
            }
            if (this.mMembersFragment != null) {
                this.mMembersFragment.toastForSaveAction(action);
            }
        }
        setIntent(intent);
        if (!processIntent(true)) {
            finish();
            Log.w("PeopleActivity", "[onNewIntent]can not process intent:" + getIntent());
            return;
        }
        Log.d("PeopleActivity", "[onNewIntent]");
        this.mContactListFilterController.checkFilterValidity(false);
        if (!isInSecondLevel()) {
            this.mContactsListFragment.setParameters(this.mRequest, true);
            this.mContactsListFragment.initializeActionBarAdapter(null);
        }
        if (this.mRequest != null && this.mRequest.getActionCode() == 15) {
            this.mShouldSwitchToAllContacts = true;
            Log.d("PeopleActivity", "[onNewIntent]set mShouldSwitchToAllContacts");
        }
        initializeFabVisibility();
        invalidateOptionsMenuIfNeeded();
        PDebug.End("onNewIntent");
    }

    private static boolean isGroupSaveAction(String str) {
        return "updateGroup".equals(str) || GroupUtil.ACTION_ADD_TO_GROUP.equals(str) || GroupUtil.ACTION_REMOVE_FROM_GROUP.equals(str);
    }

    private void toast(int i) {
        if (i >= 0) {
            Toast.makeText(this, i, 0).show();
        }
    }

    private boolean processIntent(boolean z) {
        this.mRequest = this.mIntentResolver.resolveIntent(getIntent());
        if (Log.isLoggable("PeopleActivity", 3)) {
            Log.d("PeopleActivity", this + " processIntent: forNewIntent=" + z + " intent=" + getIntent() + " request=" + this.mRequest);
        }
        if (!this.mRequest.isValid()) {
            Log.w("PeopleActivity", "[processIntent]request is inValid");
            setResult(0);
            return false;
        }
        Log.d("PeopleActivity", "[processIntent]action code=" + this.mRequest.getActionCode());
        int actionCode = this.mRequest.getActionCode();
        if (actionCode == 140) {
            ImplicitIntentsUtil.startQuickContact(this, this.mRequest.getContactUri(), 0);
            return false;
        }
        switch (actionCode) {
            case 22:
                onCreateGroupMenuItemClicked();
                return true;
            case 23:
            case 24:
                this.mShouldSwitchToGroupView = true;
                return true;
            default:
                return true;
        }
    }

    private void createViewsAndFragments() {
        Log.d("PeopleActivity", "[createViewsAndFragments]");
        PDebug.Start("createViewsAndFragments, prepare fragments");
        setContentView(R.layout.people_activity);
        FragmentManager fragmentManager = getFragmentManager();
        setUpListFragment(fragmentManager);
        this.mMembersFragment = (GroupMembersFragment) fragmentManager.findFragmentByTag("contacts-groups");
        this.mFloatingActionButtonContainer = findViewById(R.id.floating_action_button_container);
        ImageButton imageButton = (ImageButton) findViewById(R.id.floating_action_button);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountFilterUtil.startEditorIntent(PeopleActivity.this, PeopleActivity.this.getIntent(), PeopleActivity.this.mContactListFilterController.getFilter());
            }
        });
        this.mFloatingActionButtonController = new FloatingActionButtonController(this, this.mFloatingActionButtonContainer, imageButton);
        invalidateOptionsMenuIfNeeded();
        this.mLayoutRoot = (CoordinatorLayout) findViewById(R.id.root);
        if (this.mShouldSwitchToGroupView && !this.mIsRecreatedInstance) {
            this.mGroupUri = this.mRequest.getContactUri();
            switchToOrUpdateGroupView(GroupUtil.ACTION_SWITCH_GROUP);
            this.mShouldSwitchToGroupView = false;
        }
    }

    @Override
    public void setContentView(int i) {
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.content_frame);
        if (viewGroup != null) {
            viewGroup.removeAllViews();
        }
        LayoutInflater.from(this).inflate(i, viewGroup);
    }

    private void setUpListFragment(FragmentManager fragmentManager) {
        this.mContactsListFragment = (DefaultContactBrowseListFragment) fragmentManager.findFragmentByTag("contacts-all");
        if (this.mContactsListFragment == null) {
            this.mContactsListFragment = new DefaultContactBrowseListFragment();
            this.mContactsListFragment.setAnimateOnLoad(true);
            fragmentManager.beginTransaction().add(R.id.contacts_list_container, this.mContactsListFragment, "contacts-all").commit();
            fragmentManager.executePendingTransactions();
        }
        this.mContactsListFragment.setContactsAvailable(areContactsAvailable());
        this.mContactsListFragment.setListType(this.mContactListFilterController.getFilterListType());
        this.mContactsListFragment.setParameters(this.mRequest, false);
    }

    @Override
    protected void onStart() {
        Log.i("PeopleActivity", "[onStart]mIsRecreatedInstance = " + this.mIsRecreatedInstance);
        AccountTypeManagerEx.registerReceiverOnSimStateAndInfoChanged(this, this.mBroadcastReceiver);
        super.onStart();
    }

    @Override
    protected void onPause() {
        Log.i("PeopleActivity", "[onPause]");
        this.mResumed = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mSaveServiceListener);
        closeMenusIfOpen(false, true);
        super.onPause();
        ContentResolver.removeStatusChangeListener(this.mStatusChangeListenerHandle);
        onSyncStateUpdated();
    }

    @Override
    public void onMultiWindowModeChanged(boolean z) {
        initializeHomeVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w("PeopleActivity", "[onResume]");
        if (this.mDrawerLayout.isDrawerOpen(8388611)) {
            updateStatusBarBackground();
        }
        if (this.mShouldSwitchToAllContacts) {
            switchToAllContacts();
        }
        this.mProviderStatusWatcher.start();
        updateViewConfiguration(true);
        this.mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(7, this.mSyncStatusObserver);
        onSyncStateUpdated();
        initializeFabVisibility();
        initializeHomeVisibility();
        this.mSaveServiceListener = new SaveServiceListener();
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mSaveServiceListener, new IntentFilter(ContactSaveService.BROADCAST_GROUP_DELETED));
        Log.d("PeopleActivity", "[Performance test][Contacts] loading data end time: [" + System.currentTimeMillis() + "]");
        this.mResumed = true;
        PDebug.End("Contacts.onResume");
    }

    @Override
    protected void onStop() {
        Log.w("PeopleActivity", "[onStop]");
        PDebug.Start("onStop");
        unregisterReceiver(this.mBroadcastReceiver);
        this.mProviderStatusWatcher.stop();
        super.onStop();
        PDebug.End("onStop");
    }

    public void updateStatusBarBackground() {
        updateStatusBarBackground(-1);
    }

    public void updateStatusBarBackground(int i) {
        if (CompatUtils.isLollipopCompatible()) {
            if (i == -1) {
                this.mDrawerLayout.setStatusBarBackgroundColor(MaterialColorMapUtils.getStatusBarColor(this));
            } else {
                this.mDrawerLayout.setStatusBarBackgroundColor(i);
            }
            this.mDrawerLayout.invalidate();
            getWindow().setStatusBarColor(0);
        }
    }

    @Override
    protected void onDestroy() {
        Log.w("PeopleActivity", "[onDestroy]");
        PDebug.Start("onDestroy");
        this.mProviderStatusWatcher.removeListener(this.mProviderStatusListener);
        this.mContactListFilterController.removeListener(this.mFilterListener);
        SimGroupProcessor.unregisterListener(this);
        super.onDestroy();
        PDebug.End("onDestroy");
    }

    private void initializeFabVisibility() {
        this.mFloatingActionButtonContainer.setVisibility(shouldHideFab() ? 8 : 0);
        this.mFloatingActionButtonController.resetIn();
        this.wasLastFabAnimationScaleIn = !shouldHideFab();
    }

    private void initializeHomeVisibility() {
        if (getToolbar() != null) {
            if (isListFragmentInSelectionMode() || isListFragmentInSearchMode() || isGroupsFragmentInSelectionMode() || isGroupsFragmentInSearchMode()) {
                getToolbar().setNavigationIcon((Drawable) null);
            }
        }
    }

    private boolean shouldHideFab() {
        return (this.mContactsListFragment != null && this.mContactsListFragment.getActionBarAdapter() == null) || isInSecondLevel() || isListFragmentInSearchMode() || isListFragmentInSelectionMode();
    }

    public void showFabWithAnimation(boolean z) {
        if (this.mFloatingActionButtonContainer == null) {
            return;
        }
        if (z) {
            if (!this.wasLastFabAnimationScaleIn) {
                this.mFloatingActionButtonContainer.setVisibility(0);
                this.mFloatingActionButtonController.scaleIn(0);
            }
            this.wasLastFabAnimationScaleIn = true;
            return;
        }
        if (this.wasLastFabAnimationScaleIn) {
            this.mFloatingActionButtonContainer.setVisibility(0);
            this.mFloatingActionButtonController.scaleOut();
        }
        this.wasLastFabAnimationScaleIn = false;
    }

    private void updateViewConfiguration(boolean z) {
        Log.d("PeopleActivity", "[updateViewConfiguration]forceUpdate = " + z);
        int providerStatus = this.mProviderStatusWatcher.getProviderStatus();
        if (z || this.mProviderStatus == null || !this.mProviderStatus.equals(Integer.valueOf(providerStatus))) {
            this.mProviderStatus = Integer.valueOf(providerStatus);
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
            if (shouldShowList()) {
                if (this.mContactsListFragment != null) {
                    Fragment fragmentFindFragmentByTag = fragmentManager.findFragmentByTag("contacts-unavailable");
                    if (fragmentFindFragmentByTag != null) {
                        fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
                    }
                    if (this.mContactsListFragment.isHidden()) {
                        fragmentTransactionBeginTransaction.show(this.mContactsListFragment);
                    }
                    this.mContactsListFragment.setContactsAvailable(areContactsAvailable());
                    this.mContactsListFragment.setEnabled(true);
                }
            } else {
                if (this.mContactsListFragment != null) {
                    this.mContactsListFragment.setEnabled(false);
                }
                ContactsUnavailableFragment contactsUnavailableFragment = new ContactsUnavailableFragment();
                fragmentTransactionBeginTransaction.hide(this.mContactsListFragment);
                fragmentTransactionBeginTransaction.replace(R.id.contacts_unavailable_container, contactsUnavailableFragment, "contacts-unavailable");
                contactsUnavailableFragment.updateStatus(this.mProviderStatus.intValue());
            }
            if (!fragmentTransactionBeginTransaction.isEmpty() && isSafeToCommitTransactions()) {
                fragmentTransactionBeginTransaction.commit();
                fragmentManager.executePendingTransactions();
            } else {
                Log.e("PeopleActivity", "[updateViewConfiguration]Igore commit. isEmpty:" + fragmentTransactionBeginTransaction.isEmpty() + ", isSafe:" + isSafeToCommitTransactions());
            }
            invalidateOptionsMenuIfNeeded();
        }
    }

    private boolean shouldShowList() {
        if (this.mProviderStatus == null) {
            return false;
        }
        if ((!this.mProviderStatus.equals(2) || !this.mAccountTypeManager.hasNonLocalAccount()) && !this.mProviderStatus.equals(0)) {
            ExtensionManager.getInstance();
            if (!ExtensionManager.getRcsExtension().isRcsServiceAvailable()) {
                return false;
            }
        }
        return true;
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (this.mContactsListFragment != null && this.mContactsListFragment.getOptionsMenuContactsAvailable() != areContactsAvailable()) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mDrawerLayout.isDrawerOpen(8388611)) {
            if (i == 4) {
                return super.onKeyDown(i, keyEvent);
            }
            return false;
        }
        int unicodeChar = keyEvent.getUnicodeChar();
        if (unicodeChar != 0 && (Integer.MIN_VALUE & unicodeChar) == 0 && !Character.isWhitespace(unicodeChar) && this.mContactsListFragment.onKeyDown(unicodeChar)) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public void onBackPressed() {
        if (!isSafeToCommitTransactions()) {
            return;
        }
        if (this.mDrawerLayout.isDrawerOpen(8388611)) {
            closeDrawer();
            return;
        }
        if (isGroupView()) {
            onBackPressedGroupView();
            return;
        }
        if (isAssistantView()) {
            onBackPressedAssistantView();
        } else {
            if (FeatureHighlightHelper.tryRemoveHighlight(this) || maybeHandleInListFragment()) {
                return;
            }
            super.onBackPressed();
        }
    }

    private void onBackPressedGroupView() {
        if (this.mMembersFragment == null) {
            Log.d("PeopleActivity", "[onBackPressedGroupView] mMembersFragment is null !!");
            switchToAllContacts();
            return;
        }
        if (this.mMembersFragment.isEditMode()) {
            this.mMembersFragment.exitEditMode();
            return;
        }
        if (this.mMembersFragment.getActionBarAdapter().isSelectionMode()) {
            this.mMembersFragment.getActionBarAdapter().setSelectionMode(false);
            this.mMembersFragment.displayCheckBoxes(false);
        } else if (this.mMembersFragment.getActionBarAdapter().isSearchMode()) {
            this.mMembersFragment.getActionBarAdapter().setSearchMode(false);
        } else {
            switchToAllContacts();
        }
    }

    private void onBackPressedAssistantView() {
        if (!isInThirdLevel()) {
            switchToAllContacts();
        } else {
            setDrawerLockMode(true);
            super.onBackPressed();
        }
    }

    private boolean maybeHandleInListFragment() {
        if (isListFragmentInSelectionMode()) {
            this.mContactsListFragment.getActionBarAdapter().setSelectionMode(false);
            return true;
        }
        if (isListFragmentInSearchMode()) {
            this.mContactsListFragment.getActionBarAdapter().setSearchMode(false);
            if (this.mContactsListFragment.wasSearchResultClicked()) {
                this.mContactsListFragment.resetSearchResultClicked();
            } else {
                Logger.logScreenView(this, 2);
                Logger.logSearchEvent(this.mContactsListFragment.createSearchState());
            }
            return true;
        }
        if (AccountFilterUtil.isAllContactsFilter(this.mContactListFilterController.getFilter()) || this.mContactsListFragment.isHidden()) {
            return false;
        }
        switchToAllContacts();
        return true;
    }

    private boolean isListFragmentInSelectionMode() {
        return (this.mContactsListFragment == null || this.mContactsListFragment.getActionBarAdapter() == null || !this.mContactsListFragment.getActionBarAdapter().isSelectionMode()) ? false : true;
    }

    private boolean isListFragmentInSearchMode() {
        return (this.mContactsListFragment == null || this.mContactsListFragment.getActionBarAdapter() == null || !this.mContactsListFragment.getActionBarAdapter().isSearchMode()) ? false : true;
    }

    private boolean isGroupsFragmentInSelectionMode() {
        return (this.mMembersFragment == null || this.mMembersFragment.getActionBarAdapter() == null || !this.mMembersFragment.getActionBarAdapter().isSelectionMode()) ? false : true;
    }

    private boolean isGroupsFragmentInSearchMode() {
        return (this.mMembersFragment == null || this.mMembersFragment.getActionBarAdapter() == null || !this.mMembersFragment.getActionBarAdapter().isSearchMode()) ? false : true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        Log.i("PeopleActivity", "[onSaveInstanceState]");
        super.onSaveInstanceState(bundle);
        if (this.mNewGroupAccount != null) {
            bundle.putString("newGroupAccount", this.mNewGroupAccount.stringify());
        }
        bundle.putInt("contactsView", this.mCurrentView.ordinal());
        bundle.putParcelable("groupUri", this.mGroupUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mGroupUri = (Uri) bundle.getParcelable("groupUri");
    }

    private void onGroupDeleted(final Intent intent) {
        if (ContactSaveService.canUndo(intent)) {
            int i = ((AccessibilityManager) getSystemService("accessibility")).isEnabled() ? 15000 : 0;
            String string = getString(R.string.groupDeletedToast);
            Snackbar actionTextColor = Snackbar.make(this.mLayoutRoot, string, i).setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int intExtra = intent.getIntExtra("subId", SubInfoUtils.getInvalidSubId());
                    if (intExtra > 0) {
                        ContactSaveService.startService(PeopleActivity.this, SimGroupUtils.createUndoIntentForIcc(PeopleActivity.this, intent, intExtra));
                    } else {
                        ContactSaveService.startService(PeopleActivity.this, ContactSaveService.createUndoIntent(PeopleActivity.this, intent));
                    }
                }
            }).setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_action_text));
            this.mLayoutRoot.announceForAccessibility(string);
            this.mLayoutRoot.announceForAccessibility(getString(R.string.undo));
            actionTextColor.show();
        }
    }

    private class SaveServiceListener extends BroadcastReceiver {
        private SaveServiceListener() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (((action.hashCode() == 1201034394 && action.equals(ContactSaveService.BROADCAST_GROUP_DELETED)) ? (byte) 0 : (byte) -1) == 0) {
                PeopleActivity.this.onGroupDeleted(intent);
            }
        }
    }

    private void onGroupMenuItemClicked(long j) {
        if (isGroupView() && this.mMembersFragment != null && this.mMembersFragment.isCurrentGroup(j)) {
            return;
        }
        this.mGroupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, j);
        switchToOrUpdateGroupView(GroupUtil.ACTION_SWITCH_GROUP);
    }

    private void onFilterMenuItemClicked(Intent intent) {
        if (isInSecondLevel()) {
            popSecondLevel();
            showFabWithAnimation(true);
            ContactListFilter filter = this.mContactListFilterController.getFilter();
            Log.d("PeopleActivity", "[onFilterMenuItemClicked] current=" + filter);
            this.mContactListFilterController.setContactListFilter(AccountFilterUtil.createContactsFilter(this), false);
            this.mContactListFilterController.setContactListFilter(filter, false);
        }
        this.mCurrentView = ContactsView.ACCOUNT_VIEW;
        AccountFilterUtil.handleAccountFilterResult(this.mContactListFilterController, -1, intent);
    }

    private void switchToOrUpdateGroupView(String str) {
        if (this.mMembersFragment != null && !this.mMembersFragment.isInactive()) {
            this.mMembersFragment.updateExistingGroupFragment(this.mGroupUri, str);
        } else {
            switchView(ContactsView.GROUP_VIEW);
        }
    }

    protected void launchAssistant() {
        switchView(ContactsView.ASSISTANT);
    }

    private void switchView(ContactsView contactsView) {
        if (!isSafeToCommitTransactions()) {
            Log.e("PeopleActivity", "[switchView]Ignore switchView. isSafe:" + isSafeToCommitTransactions());
            return;
        }
        this.mCurrentView = contactsView;
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
        popSecondLevel();
        if (isGroupView()) {
            this.mMembersFragment = GroupMembersFragment.newInstance(this.mGroupUri);
            fragmentTransactionBeginTransaction.replace(R.id.contacts_list_container, this.mMembersFragment, "contacts-groups");
        } else if (isAssistantView()) {
            Fragment fragmentFindFragmentByTag = fragmentManager.findFragmentByTag("contacts-assistant");
            Fragment fragmentFindFragmentByTag2 = fragmentManager.findFragmentByTag("contacts-unavailable");
            if (fragmentFindFragmentByTag == null) {
                fragmentFindFragmentByTag = ObjectFactory.getAssistantFragment();
            }
            if (fragmentFindFragmentByTag2 != null) {
                fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag2);
            }
            fragmentTransactionBeginTransaction.replace(R.id.contacts_list_container, fragmentFindFragmentByTag, "contacts-assistant");
            resetToolBarStatusBarColor();
        }
        fragmentTransactionBeginTransaction.addToBackStack("second-level");
        fragmentTransactionBeginTransaction.commit();
        fragmentManager.executePendingTransactions();
        showFabWithAnimation(false);
    }

    public void switchToAllContacts() {
        popSecondLevel();
        this.mShouldSwitchToAllContacts = false;
        this.mCurrentView = ContactsView.ALL_CONTACTS;
        this.mDrawerFragment.setNavigationItemChecked(ContactsView.ALL_CONTACTS);
        showFabWithAnimation(true);
        this.mContactsListFragment.scrollToTop();
        resetFilter();
        setTitle(getString(R.string.contactsList));
    }

    private void resetFilter() {
        Intent intent = new Intent();
        intent.putExtra("contactListFilter", AccountFilterUtil.createContactsFilter(this));
        AccountFilterUtil.handleAccountFilterResult(this.mContactListFilterController, -1, intent);
    }

    private void resetToolBarStatusBarColor() {
        findViewById(R.id.toolbar_frame).setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color));
        updateStatusBarBackground(ContextCompat.getColor(this, R.color.primary_color_dark));
    }

    protected DefaultContactBrowseListFragment getListFragment() {
        return this.mContactsListFragment;
    }

    protected GroupMembersFragment getGroupFragment() {
        return this.mMembersFragment;
    }

    private void handleFilterChangeForFragment(ContactListFilter contactListFilter) {
        if (this.mContactsListFragment.canSetActionBar()) {
            this.mContactsListFragment.setFilterAndUpdateTitle(contactListFilter);
            this.mContactsListFragment.scrollToTop();
        }
    }

    private void handleFilterChangeForActivity(ContactListFilter contactListFilter) {
        if ((isAssistantView() || isAccountView()) && contactListFilter.isContactsFilterType()) {
            Log.d("PeopleActivity", "[handleFilterChangeForActivity]set mShouldSwitchToAllContacts");
            this.mShouldSwitchToAllContacts = true;
        }
        if (CompatUtils.isNCompatible()) {
            getWindow().getDecorView().sendAccessibilityEvent(32);
        }
        invalidateOptionsMenu();
    }

    public void updateDrawerGroupMenu(long j) {
        if (this.mDrawerFragment != null) {
            this.mDrawerFragment.updateGroupMenu(j);
        }
    }

    public void setDrawerLockMode(boolean z) {
        this.mDrawerLayout.setDrawerLockMode(z ? 0 : 1);
        if (z) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            this.mToggle.setDrawerIndicatorEnabled(true);
        } else {
            this.mToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public Toolbar getToolbar() {
        return this.mToolbar;
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        this.mToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mToggle.onConfigurationChanged(configuration);
        initializeHomeVisibility();
    }

    protected void onCreateGroupMenuItemClicked() {
        Account account;
        String string;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            account = (Account) extras.getParcelable("android.provider.extra.ACCOUNT");
        } else {
            account = null;
        }
        if (account == null) {
            selectAccountForNewGroup();
            return;
        }
        if (extras != null) {
            string = extras.getString("android.provider.extra.DATA_SET");
        } else {
            string = null;
        }
        onAccountChosen(new AccountWithDataSet(account.name, account.type, string), null);
    }

    private void selectAccountForNewGroup() {
        List list = (List) Futures.getUnchecked(AccountTypeManager.getInstance(this).filterAccountsAsync(AccountTypeManager.AccountFilter.GROUPS_WRITABLE));
        if (list.isEmpty()) {
            Toast.makeText(this, R.string.groupCreateFailedToast, 0).show();
        } else if (list.size() == 1) {
            onAccountChosen(((AccountInfo) list.get(0)).getAccount(), null);
        } else {
            SelectAccountDialogFragment.show(getFragmentManager(), R.string.dialog_new_group_account, AccountTypeManager.AccountFilter.GROUPS_WRITABLE, null, "selectAccountDialog");
        }
    }

    @Override
    public void onAccountChosen(AccountWithDataSet accountWithDataSet, Bundle bundle) {
        this.mNewGroupAccount = accountWithDataSet;
        GroupNameEditDialogFragment.newInstanceForCreation(this.mNewGroupAccount, "createGroup").show(getFragmentManager(), "groupNameEditDialog");
    }

    @Override
    public void onAccountSelectorCancelled() {
    }

    @Override
    public void onDrawerItemClicked() {
        closeDrawer();
    }

    @Override
    public void onContactsViewSelected(ContactsView contactsView) {
        if (contactsView == ContactsView.ALL_CONTACTS) {
            switchToAllContacts();
        } else {
            if (contactsView == ContactsView.ASSISTANT) {
                launchAssistant();
                return;
            }
            throw new IllegalStateException("Unknown view " + contactsView);
        }
    }

    @Override
    public void onCreateLabelButtonClicked() {
        if (isSafeToCommitTransactions()) {
            onCreateGroupMenuItemClicked();
        }
    }

    @Override
    public void onOpenSettings() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PeopleActivity.this.startActivity(PeopleActivity.this.createPreferenceIntent());
            }
        }, 300L);
    }

    @Override
    public void onLaunchHelpFeedback() {
        HelpUtils.launchHelpAndFeedbackForMainScreen(this);
    }

    @Override
    public void onConferenceCallClicked() {
        ActivitiesUtils.conferenceCall(this);
    }

    @Override
    public void onGroupViewSelected(GroupListItem groupListItem) {
        onGroupMenuItemClicked(groupListItem.getGroupId());
    }

    @Override
    public void onAccountViewSelected(ContactListFilter contactListFilter) {
        Log.d("PeopleActivity", "[onAccountViewSelected] filter=" + contactListFilter + ", accountType=" + contactListFilter.accountType + ", accountName=" + Log.anonymize(contactListFilter.accountName));
        Intent intent = new Intent();
        intent.putExtra("contactListFilter", contactListFilter);
        onFilterMenuItemClicked(intent);
    }

    public boolean isGroupView() {
        return this.mCurrentView == ContactsView.GROUP_VIEW;
    }

    protected boolean isAssistantView() {
        return this.mCurrentView == ContactsView.ASSISTANT;
    }

    protected boolean isAllContactsView() {
        return this.mCurrentView == ContactsView.ALL_CONTACTS;
    }

    protected boolean isAccountView() {
        return this.mCurrentView == ContactsView.ACCOUNT_VIEW;
    }

    public boolean isInSecondLevel() {
        return isGroupView() || isAssistantView();
    }

    private boolean isInThirdLevel() {
        return isLastBackStackTag("third-level");
    }

    private boolean isLastBackStackTag(String str) {
        int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount <= 0) {
            return false;
        }
        FragmentManager.BackStackEntry backStackEntryAt = getFragmentManager().getBackStackEntryAt(backStackEntryCount - 1);
        if (str == null) {
            return backStackEntryAt.getName() == null;
        }
        return str.equals(backStackEntryAt.getName());
    }

    private void popSecondLevel() {
        if (!isSafeToCommitTransactions()) {
            Log.e("PeopleActivity", "[popSecondLevel] ignore popSecondLevel. isSafe: false");
            return;
        }
        getFragmentManager().popBackStackImmediate("assistant-helper", 1);
        getFragmentManager().popBackStackImmediate("second-level", 1);
        this.mMembersFragment = null;
        resetToolBarStatusBarColor();
    }

    public void closeDrawer() {
        this.mDrawerLayout.closeDrawer(8388611);
    }

    private Intent createPreferenceIntent() {
        Intent intent = new Intent(this, (Class<?>) ContactsPreferenceActivity.class);
        intent.putExtra("newLocalProfile", "newLocalProfile");
        return intent;
    }

    @Override
    public void onSimGroupCompleted(Intent intent) {
        Log.d("PeopleActivity", "[onSIMGroupCompleted]callbackIntent = " + intent);
        onNewIntent(intent);
    }

    private void closeMenusIfOpen(boolean z, boolean z2) {
        MultiSelectContactsListFragment listFragment;
        if (isAllContactsView() || isAccountView()) {
            listFragment = getListFragment();
        } else if (isGroupView()) {
            listFragment = getGroupFragment();
        } else {
            listFragment = null;
        }
        if (listFragment != null) {
            listFragment.closeMenusIfOpen(z, z2);
        }
    }

    public boolean getResumed() {
        return this.mResumed;
    }
}
