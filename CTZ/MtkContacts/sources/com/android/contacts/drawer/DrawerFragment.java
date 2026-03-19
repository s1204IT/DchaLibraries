package com.android.contacts.drawer;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ims.feature.ImsFeature;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.contacts.GroupListLoader;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contactsbind.ObjectFactory;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.ims.MtkImsConnectionStateListener;
import com.mediatek.ims.internal.MtkImsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DrawerFragment extends Fragment implements AccountsLoader.AccountsListener {
    private boolean mAccountsLoaded;
    private PeopleActivity.ContactsView mCurrentContactsView;
    private DrawerAdapter mDrawerAdapter;
    private ListView mDrawerListView;
    private boolean mGroupsLoaded;
    private boolean mHasGroupWritableAccounts;
    private DrawerFragmentListener mListener;
    private WelcomeContentObserver mObserver;
    private ScrimDrawable mScrimDrawable;
    private List<GroupListItem> mGroupListItems = new ArrayList();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            String action = intent.getAction();
            Log.d("DrawerFragment", "[onReceive] intent.getAction() = " + action);
            if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                DrawerFragment.this.mDrawerAdapter.notifyConfCallStateChanged();
            } else if (action.equals("com.mediatek.ims.MTK_IMS_SERVICE_UP")) {
                DrawerFragment.this.registerImsListeners();
            }
        }
    };
    private final LoaderManager.LoaderCallbacks<List<ContactListFilter>> mFiltersLoaderListener = new LoaderManager.LoaderCallbacks<List<ContactListFilter>>() {
        @Override
        public Loader<List<ContactListFilter>> onCreateLoader(int i, Bundle bundle) {
            return new AccountFilterUtil.FilterLoader(DrawerFragment.this.getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<ContactListFilter>> loader, List<ContactListFilter> list) {
            if (list != null) {
                if (list == null || list.size() < 2) {
                    DrawerFragment.this.mDrawerAdapter.setAccounts(new ArrayList());
                } else {
                    DrawerFragment.this.mDrawerAdapter.setAccounts(list);
                }
            }
            DrawerFragment.this.checkSelectedAccountViewValidate(list);
        }

        @Override
        public void onLoaderReset(Loader<List<ContactListFilter>> loader) {
        }
    };
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupListLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int i, Bundle bundle) {
            return new GroupListLoader(DrawerFragment.this.getActivity());
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                DrawerFragment.this.mGroupListItems.clear();
                cursor.moveToPosition(-1);
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (cursor.moveToNext()) {
                        DrawerFragment.this.mGroupListItems.add(GroupUtil.getGroupListItem(cursor, i));
                    }
                }
                DrawerFragment.this.mGroupsLoaded = true;
                DrawerFragment.this.notifyIfReady();
                DrawerFragment.this.checkSelectedGroupViewValidate(DrawerFragment.this.mGroupListItems);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private final AdapterView.OnItemClickListener mOnDrawerItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            if (DrawerFragment.this.mListener == null) {
                return;
            }
            int id = view.getId();
            if (id == R.id.nav_all_contacts) {
                DrawerFragment.this.mListener.onContactsViewSelected(PeopleActivity.ContactsView.ALL_CONTACTS);
                DrawerFragment.this.setNavigationItemChecked(PeopleActivity.ContactsView.ALL_CONTACTS);
            } else if (id == R.id.nav_assistant) {
                DrawerFragment.this.mListener.onContactsViewSelected(PeopleActivity.ContactsView.ASSISTANT);
                DrawerFragment.this.setNavigationItemChecked(PeopleActivity.ContactsView.ASSISTANT);
            } else if (id == R.id.nav_group) {
                GroupListItem groupListItem = (GroupListItem) view.getTag();
                DrawerFragment.this.mListener.onGroupViewSelected(groupListItem);
                DrawerFragment.this.mDrawerAdapter.setSelectedGroupId(groupListItem.getGroupId());
                DrawerFragment.this.setNavigationItemChecked(PeopleActivity.ContactsView.GROUP_VIEW);
            } else if (id == R.id.nav_filter) {
                ContactListFilter contactListFilter = (ContactListFilter) view.getTag();
                DrawerFragment.this.mListener.onAccountViewSelected(contactListFilter);
                DrawerFragment.this.mDrawerAdapter.setSelectedAccount(contactListFilter);
                DrawerFragment.this.setNavigationItemChecked(PeopleActivity.ContactsView.ACCOUNT_VIEW);
            } else if (id == R.id.nav_create_label) {
                DrawerFragment.this.mListener.onCreateLabelButtonClicked();
            } else if (id == R.id.nav_settings) {
                DrawerFragment.this.mListener.onOpenSettings();
            } else if (id == R.id.nav_help) {
                DrawerFragment.this.mListener.onLaunchHelpFeedback();
            } else if (id == R.id.nav_conf_call) {
                DrawerFragment.this.mListener.onConferenceCallClicked();
            } else {
                Log.e("DrawerFragment", "Unknown view:" + id);
                return;
            }
            DrawerFragment.this.mListener.onDrawerItemClicked();
        }
    };
    private Map<Integer, ImsStateListener> mImsStateListenMap = new HashMap();
    private SubscriptionManager.OnSubscriptionsChangedListener mSubListener = new SubListener();

    public interface DrawerFragmentListener {
        void onAccountViewSelected(ContactListFilter contactListFilter);

        void onConferenceCallClicked();

        void onContactsViewSelected(PeopleActivity.ContactsView contactsView);

        void onCreateLabelButtonClicked();

        void onDrawerItemClicked();

        void onGroupViewSelected(GroupListItem groupListItem);

        void onLaunchHelpFeedback();

        void onOpenSettings();
    }

    private final class WelcomeContentObserver extends ContentObserver {
        private WelcomeContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            DrawerFragment.this.mDrawerAdapter.notifyDataSetChanged();
        }
    }

    public void updateConfCallState() {
        if (this.mDrawerAdapter != null) {
            this.mDrawerAdapter.notifyConfCallStateChanged();
        }
    }

    private void checkSelectedAccountViewValidate(List<ContactListFilter> list) {
        Iterator<ContactListFilter> it = list.iterator();
        while (it.hasNext()) {
            Log.d("DrawerFragment", "[mFiltersLoaderListener]checkSelectedAccountViewValidate:" + Log.anonymize(it.next().accountName));
        }
        boolean z = false;
        if (this.mDrawerAdapter.getSelectedAccount() != null && this.mCurrentContactsView == PeopleActivity.ContactsView.ACCOUNT_VIEW) {
            Iterator<ContactListFilter> it2 = list.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                if (this.mDrawerAdapter.getSelectedAccount().equals(it2.next())) {
                    z = true;
                    break;
                }
            }
            if (!z && this.mListener != null && getActivity() != null && !getActivity().isFinishing()) {
                Log.d("DrawerFragment", "[mFiltersLoaderListener]convert to ALL_CONTACTS due to " + Log.anonymize(this.mDrawerAdapter.getSelectedAccount().accountName) + " already not exist");
                switchToAllContacts();
            }
        }
    }

    private void checkSelectedGroupViewValidate(List<GroupListItem> list) {
        Iterator<GroupListItem> it = list.iterator();
        while (it.hasNext()) {
            Log.d("DrawerFragment", "[mFiltersLoaderListener]checkSelectedGroupViewValidate:" + it.next().getGroupId());
        }
        boolean z = false;
        if (this.mDrawerAdapter.getSelectedGroupId() >= 0 && this.mCurrentContactsView == PeopleActivity.ContactsView.GROUP_VIEW) {
            Iterator<GroupListItem> it2 = list.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                if (this.mDrawerAdapter.getSelectedGroupId() == it2.next().getGroupId()) {
                    z = true;
                    break;
                }
            }
            if (!z && this.mListener != null && getActivity() != null && !getActivity().isFinishing()) {
                Log.d("DrawerFragment", "[mFiltersLoaderListener]convert to ALL_CONTACTS due to " + this.mDrawerAdapter.getSelectedGroupId() + " already not exist");
                switchToAllContacts();
            }
        }
    }

    private void switchToAllContacts() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (DrawerFragment.this.getActivity() != null && !DrawerFragment.this.getActivity().isFinishing()) {
                    DrawerFragment.this.mListener.onContactsViewSelected(PeopleActivity.ContactsView.ALL_CONTACTS);
                    DrawerFragment.this.setNavigationItemChecked(PeopleActivity.ContactsView.ALL_CONTACTS);
                }
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DrawerFragmentListener) {
            this.mListener = (DrawerFragmentListener) activity;
            return;
        }
        throw new IllegalArgumentException("Activity must implement " + DrawerFragmentListener.class.getName());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewInflate = layoutInflater.inflate(R.layout.drawer_fragment, (ViewGroup) null);
        this.mDrawerListView = (ListView) viewInflate.findViewById(R.id.list);
        this.mDrawerAdapter = new DrawerAdapter(getActivity());
        this.mDrawerAdapter.setSelectedContactsView(this.mCurrentContactsView);
        loadGroupsAndFilters();
        this.mDrawerListView.setAdapter((ListAdapter) this.mDrawerAdapter);
        this.mDrawerListView.setOnItemClickListener(this.mOnDrawerItemClickListener);
        if (bundle != null) {
            PeopleActivity.ContactsView contactsView = PeopleActivity.ContactsView.values()[bundle.getInt("contactsView")];
            setNavigationItemChecked(contactsView);
            this.mDrawerAdapter.setSelectedGroupId(bundle.getLong("selectedGroup"));
            ContactListFilter contactListFilter = (ContactListFilter) bundle.getParcelable("selectedAccount");
            this.mDrawerAdapter.setSelectedAccount(contactListFilter);
            if (contactListFilter != null && this.mListener != null && PeopleActivity.ContactsView.ACCOUNT_VIEW == contactsView) {
                Log.d("DrawerFragment", "[onCreateView]onAccountViewSelected filter=" + contactListFilter);
                this.mListener.onAccountViewSelected(contactListFilter);
            }
        } else {
            setNavigationItemChecked(PeopleActivity.ContactsView.ALL_CONTACTS);
        }
        FrameLayout frameLayout = (FrameLayout) viewInflate.findViewById(R.id.drawer_fragment_root);
        frameLayout.setFitsSystemWindows(true);
        frameLayout.setOnApplyWindowInsetsListener(new WindowInsetsListener());
        frameLayout.setForegroundGravity(55);
        this.mScrimDrawable = new ScrimDrawable();
        frameLayout.setForeground(this.mScrimDrawable);
        registerSubListener();
        registerImsListeners();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("com.mediatek.ims.MTK_IMS_SERVICE_UP");
        getContext().registerReceiver(this.mReceiver, intentFilter);
        return viewInflate;
    }

    @Override
    public void onResume() {
        super.onResume();
        Uri welcomeUri = ObjectFactory.getWelcomeUri();
        if (welcomeUri != null) {
            this.mObserver = new WelcomeContentObserver(new Handler());
            getActivity().getContentResolver().registerContentObserver(welcomeUri, false, this.mObserver);
        }
        this.mDrawerAdapter.notifyConfCallStateChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("contactsView", this.mCurrentContactsView.ordinal());
        bundle.putLong("selectedGroup", this.mDrawerAdapter.getSelectedGroupId());
        bundle.putParcelable("selectedAccount", this.mDrawerAdapter.getSelectedAccount());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(this.mObserver);
        }
    }

    private void loadGroupsAndFilters() {
        Log.d("DrawerFragment", "[loadGroupsAndFilters]");
        getLoaderManager().initLoader(3, null, this.mFiltersLoaderListener);
        AccountsLoader.loadAccounts(this, 2, AccountTypeManager.AccountFilter.GROUPS_WRITABLE);
        getLoaderManager().initLoader(1, null, this.mGroupListLoaderListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getContext() != null) {
            getContext().unregisterReceiver(this.mReceiver);
        }
        unregisterImsListeners();
        unregisterSubListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    public void setNavigationItemChecked(PeopleActivity.ContactsView contactsView) {
        this.mCurrentContactsView = contactsView;
        if (this.mDrawerAdapter != null) {
            this.mDrawerAdapter.setSelectedContactsView(contactsView);
        }
    }

    public void updateGroupMenu(long j) {
        this.mDrawerAdapter.setSelectedGroupId(j);
        setNavigationItemChecked(PeopleActivity.ContactsView.GROUP_VIEW);
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> list) {
        this.mHasGroupWritableAccounts = !list.isEmpty();
        this.mAccountsLoaded = true;
        notifyIfReady();
    }

    private void notifyIfReady() {
        if (this.mAccountsLoaded && this.mGroupsLoaded) {
            Iterator<GroupListItem> it = this.mGroupListItems.iterator();
            while (it.hasNext()) {
                if (GroupUtil.isEmptyFFCGroup(it.next())) {
                    it.remove();
                }
            }
            this.mDrawerAdapter.setGroups(this.mGroupListItems, this.mHasGroupWritableAccounts);
        }
    }

    private void applyTopInset(int i) {
        this.mScrimDrawable.setIntrinsicHeight(i);
        this.mDrawerListView.setPadding(this.mDrawerListView.getPaddingLeft(), i, this.mDrawerListView.getPaddingRight(), this.mDrawerListView.getPaddingBottom());
    }

    private class WindowInsetsListener implements View.OnApplyWindowInsetsListener {
        private WindowInsetsListener() {
        }

        @Override
        public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
            DrawerFragment.this.applyTopInset(windowInsets.getSystemWindowInsetTop());
            return windowInsets;
        }
    }

    class ImsStateListener extends MtkImsConnectionStateListener {
        int subId;

        public ImsStateListener(int i) {
            this.subId = i;
        }

        public void onCapabilitiesStatusChanged(ImsFeature.Capabilities capabilities) {
            if (DrawerFragment.this.getActivity() == null) {
                Log.d("DrawerFragment", "[onCapabilitiesStatusChanged] Activity is null, return.");
            } else {
                final boolean zIsCapable = capabilities.isCapable(1);
                DrawerFragment.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (DrawerFragment.this.mDrawerAdapter != null && DrawerFragment.this.mDrawerAdapter.isVolteStateChanged(ImsStateListener.this.subId, zIsCapable)) {
                            DrawerFragment.this.mDrawerAdapter.notifyConfCallStateChanged();
                        }
                    }
                });
            }
        }
    }

    private void registerImsListeners() {
        List<SubscriptionInfo> activatedSubInfoList = SubInfoUtils.getActivatedSubInfoList();
        if (activatedSubInfoList == null || activatedSubInfoList.size() <= 0) {
            Log.w("DrawerFragment", "[registerImsListeners] No valid subscriptionInfoList");
            return;
        }
        for (SubscriptionInfo subscriptionInfo : activatedSubInfoList) {
            MtkImsManager imsManager = ImsManager.getInstance(getContext(), subscriptionInfo.getSimSlotIndex());
            if (imsManager != null) {
                try {
                    imsManager.removeImsConnectionStateListener(this.mImsStateListenMap.remove(Integer.valueOf(subscriptionInfo.getSimSlotIndex())));
                    ImsStateListener imsStateListener = new ImsStateListener(subscriptionInfo.getSubscriptionId());
                    this.mImsStateListenMap.put(Integer.valueOf(subscriptionInfo.getSimSlotIndex()), imsStateListener);
                    imsManager.addImsConnectionStateListener(imsStateListener);
                    Log.i("DrawerFragment", "[registerImsListeners] succeed for subId = " + subscriptionInfo.getSubscriptionId());
                } catch (ImsException e) {
                    Log.w("DrawerFragment", "[registerImsListeners] fail for subId = " + subscriptionInfo.getSubscriptionId());
                }
            }
        }
    }

    private void unregisterImsListeners() {
        for (Integer num : this.mImsStateListenMap.keySet()) {
            MtkImsManager imsManager = ImsManager.getInstance(getContext(), num.intValue());
            if (imsManager != null && this.mImsStateListenMap.containsKey(num)) {
                try {
                    Log.i("DrawerFragment", "[unregisterImsListeners] succeed for subId = " + this.mImsStateListenMap.get(num).subId);
                    imsManager.removeImsConnectionStateListener(this.mImsStateListenMap.get(num));
                } catch (ImsException e) {
                    Log.w("DrawerFragment", "[unregisterImsListeners] fail for subId = " + this.mImsStateListenMap.get(num).subId);
                }
            }
        }
        if (!this.mImsStateListenMap.isEmpty()) {
            this.mImsStateListenMap.clear();
        }
    }

    private class SubListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        private SubListener() {
        }

        @Override
        public void onSubscriptionsChanged() {
            Log.i("DrawerFragment", "[onSubscriptionsChanged]");
            DrawerFragment.this.registerImsListeners();
            DrawerFragment.this.mDrawerAdapter.notifySubChanged();
        }
    }

    private void registerSubListener() {
        SubscriptionManager.from(getContext()).addOnSubscriptionsChangedListener(this.mSubListener);
    }

    private void unregisterSubListener() {
        SubscriptionManager.from(getContext()).removeOnSubscriptionsChangedListener(this.mSubListener);
    }
}
