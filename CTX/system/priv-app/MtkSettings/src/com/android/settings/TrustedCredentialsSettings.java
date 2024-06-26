package com.android.settings;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsDialogBuilder;
import com.android.settings.core.InstrumentedFragment;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
/* loaded from: classes.dex */
public class TrustedCredentialsSettings extends InstrumentedFragment implements TrustedCredentialsDialogBuilder.DelegateInterface {
    private AliasOperation mAliasOperation;
    private ArraySet<Integer> mConfirmedCredentialUsers;
    private IntConsumer mConfirmingCredentialListener;
    private int mConfirmingCredentialUser;
    private KeyguardManager mKeyguardManager;
    private TabHost mTabHost;
    private int mTrustAllCaUserId;
    private UserManager mUserManager;
    private ArrayList<GroupAdapter> mGroupAdapters = new ArrayList<>(2);
    private Set<AdapterData.AliasLoader> mAliasLoaders = new ArraySet(2);
    @GuardedBy("mKeyChainConnectionByProfileId")
    private final SparseArray<KeyChain.KeyChainConnection> mKeyChainConnectionByProfileId = new SparseArray<>();
    private BroadcastReceiver mWorkProfileChangedReceiver = new BroadcastReceiver() { // from class: com.android.settings.TrustedCredentialsSettings.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.MANAGED_PROFILE_AVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNAVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNLOCKED".equals(action)) {
                Iterator it = TrustedCredentialsSettings.this.mGroupAdapters.iterator();
                while (it.hasNext()) {
                    ((GroupAdapter) it.next()).load();
                }
            }
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 92;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public enum Tab {
        SYSTEM("system", R.string.trusted_credentials_system_tab, R.id.system_tab, R.id.system_progress, R.id.system_content, true),
        USER("user", R.string.trusted_credentials_user_tab, R.id.user_tab, R.id.user_progress, R.id.user_content, false);
        
        private final int mContentView;
        private final int mLabel;
        private final int mProgress;
        private final boolean mSwitch;
        private final String mTag;
        private final int mView;

        Tab(String str, int i, int i2, int i3, int i4, boolean z) {
            this.mTag = str;
            this.mLabel = i;
            this.mView = i2;
            this.mProgress = i3;
            this.mContentView = i4;
            this.mSwitch = z;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public List<String> getAliases(IKeyChainService iKeyChainService) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return iKeyChainService.getSystemCaAliases().getList();
                case USER:
                    return iKeyChainService.getUserCaAliases().getList();
                default:
                    throw new AssertionError();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean deleted(IKeyChainService iKeyChainService, String str) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return !iKeyChainService.containsCaAlias(str);
                case USER:
                    return false;
                default:
                    throw new AssertionError();
            }
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mUserManager = (UserManager) activity.getSystemService("user");
        this.mKeyguardManager = (KeyguardManager) activity.getSystemService("keyguard");
        this.mTrustAllCaUserId = activity.getIntent().getIntExtra("ARG_SHOW_NEW_FOR_USER", -10000);
        this.mConfirmedCredentialUsers = new ArraySet<>(2);
        this.mConfirmingCredentialUser = -10000;
        if (bundle != null) {
            this.mConfirmingCredentialUser = bundle.getInt("ConfirmingCredentialUser", -10000);
            ArrayList<Integer> integerArrayList = bundle.getIntegerArrayList("ConfirmedCredentialUsers");
            if (integerArrayList != null) {
                this.mConfirmedCredentialUsers.addAll(integerArrayList);
            }
        }
        this.mConfirmingCredentialListener = null;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNLOCKED");
        activity.registerReceiver(this.mWorkProfileChangedReceiver, intentFilter);
        activity.setTitle(R.string.trusted_credentials);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putIntegerArrayList("ConfirmedCredentialUsers", new ArrayList<>(this.mConfirmedCredentialUsers));
        bundle.putInt("ConfirmingCredentialUser", this.mConfirmingCredentialUser);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mTabHost = (TabHost) layoutInflater.inflate(R.layout.trusted_credentials, viewGroup, false);
        this.mTabHost.setup();
        addTab(Tab.SYSTEM);
        addTab(Tab.USER);
        if (getActivity().getIntent() != null && "com.android.settings.TRUSTED_CREDENTIALS_USER".equals(getActivity().getIntent().getAction())) {
            this.mTabHost.setCurrentTabByTag(Tab.USER.mTag);
        }
        return this.mTabHost;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onDestroy() {
        getActivity().unregisterReceiver(this.mWorkProfileChangedReceiver);
        for (AdapterData.AliasLoader aliasLoader : this.mAliasLoaders) {
            aliasLoader.cancel(true);
        }
        this.mAliasLoaders.clear();
        this.mGroupAdapters.clear();
        if (this.mAliasOperation != null) {
            this.mAliasOperation.cancel(true);
            this.mAliasOperation = null;
        }
        closeKeyChainConnections();
        super.onDestroy();
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            int i3 = this.mConfirmingCredentialUser;
            IntConsumer intConsumer = this.mConfirmingCredentialListener;
            this.mConfirmingCredentialUser = -10000;
            this.mConfirmingCredentialListener = null;
            if (i2 == -1) {
                this.mConfirmedCredentialUsers.add(Integer.valueOf(i3));
                if (intConsumer != null) {
                    intConsumer.accept(i3);
                }
            }
        }
    }

    private void closeKeyChainConnections() {
        synchronized (this.mKeyChainConnectionByProfileId) {
            int size = this.mKeyChainConnectionByProfileId.size();
            for (int i = 0; i < size; i++) {
                this.mKeyChainConnectionByProfileId.valueAt(i).close();
            }
            this.mKeyChainConnectionByProfileId.clear();
        }
    }

    private void addTab(Tab tab) {
        boolean z;
        this.mTabHost.addTab(this.mTabHost.newTabSpec(tab.mTag).setIndicator(getActivity().getString(tab.mLabel)).setContent(tab.mView));
        GroupAdapter groupAdapter = new GroupAdapter(tab);
        this.mGroupAdapters.add(groupAdapter);
        int groupCount = groupAdapter.getGroupCount();
        ViewGroup viewGroup = (ViewGroup) this.mTabHost.findViewById(tab.mContentView);
        viewGroup.getLayoutTransition().enableTransitionType(4);
        LayoutInflater from = LayoutInflater.from(getActivity());
        for (int i = 0; i < groupAdapter.getGroupCount(); i++) {
            boolean isManagedProfile = groupAdapter.getUserInfoByGroup(i).isManagedProfile();
            ChildAdapter childAdapter = groupAdapter.getChildAdapter(i);
            LinearLayout linearLayout = (LinearLayout) from.inflate(R.layout.trusted_credential_list_container, viewGroup, false);
            childAdapter.setContainerView(linearLayout);
            boolean z2 = true;
            if (groupCount <= 1) {
                z = false;
            } else {
                z = true;
            }
            childAdapter.showHeader(z);
            childAdapter.showDivider(isManagedProfile);
            if (groupCount > 2 && isManagedProfile) {
                z2 = false;
            }
            childAdapter.setExpandIfAvailable(z2);
            if (isManagedProfile) {
                viewGroup.addView(linearLayout);
            } else {
                viewGroup.addView(linearLayout, 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean startConfirmCredential(int i) {
        Intent createConfirmDeviceCredentialIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, i);
        if (createConfirmDeviceCredentialIntent == null) {
            return false;
        }
        this.mConfirmingCredentialUser = i;
        startActivityForResult(createConfirmDeviceCredentialIntent, 1);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class GroupAdapter extends BaseExpandableListAdapter implements View.OnClickListener, ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener {
        private final AdapterData mData;

        private GroupAdapter(Tab tab) {
            this.mData = new AdapterData(tab, this);
            load();
        }

        @Override // android.widget.ExpandableListAdapter
        public int getGroupCount() {
            return this.mData.mCertHoldersByUserId.size();
        }

        @Override // android.widget.ExpandableListAdapter
        public int getChildrenCount(int i) {
            List list = (List) this.mData.mCertHoldersByUserId.valueAt(i);
            if (list != null) {
                return list.size();
            }
            return 0;
        }

        @Override // android.widget.ExpandableListAdapter
        public UserHandle getGroup(int i) {
            return new UserHandle(this.mData.mCertHoldersByUserId.keyAt(i));
        }

        @Override // android.widget.ExpandableListAdapter
        public CertHolder getChild(int i, int i2) {
            return (CertHolder) ((List) this.mData.mCertHoldersByUserId.get(getUserIdByGroup(i))).get(i2);
        }

        @Override // android.widget.ExpandableListAdapter
        public long getGroupId(int i) {
            return getUserIdByGroup(i);
        }

        private int getUserIdByGroup(int i) {
            return this.mData.mCertHoldersByUserId.keyAt(i);
        }

        public UserInfo getUserInfoByGroup(int i) {
            return TrustedCredentialsSettings.this.mUserManager.getUserInfo(getUserIdByGroup(i));
        }

        @Override // android.widget.ExpandableListAdapter
        public long getChildId(int i, int i2) {
            return i2;
        }

        @Override // android.widget.ExpandableListAdapter
        public boolean hasStableIds() {
            return false;
        }

        @Override // android.widget.ExpandableListAdapter
        public View getGroupView(int i, boolean z, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = Utils.inflateCategoryHeader((LayoutInflater) TrustedCredentialsSettings.this.getActivity().getSystemService("layout_inflater"), viewGroup);
            }
            TextView textView = (TextView) view.findViewById(16908310);
            if (getUserInfoByGroup(i).isManagedProfile()) {
                textView.setText(R.string.category_work);
            } else {
                textView.setText(R.string.category_personal);
            }
            textView.setTextAlignment(6);
            return view;
        }

        @Override // android.widget.ExpandableListAdapter
        public View getChildView(int i, int i2, boolean z, View view, ViewGroup viewGroup) {
            return getViewForCertificate(getChild(i, i2), this.mData.mTab, view, viewGroup);
        }

        @Override // android.widget.ExpandableListAdapter
        public boolean isChildSelectable(int i, int i2) {
            return true;
        }

        @Override // android.widget.ExpandableListView.OnChildClickListener
        public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long j) {
            TrustedCredentialsSettings.this.showCertDialog(getChild(i, i2));
            return true;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            TrustedCredentialsSettings.this.removeOrInstallCert((CertHolder) view.getTag());
        }

        @Override // android.widget.ExpandableListView.OnGroupClickListener
        public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long j) {
            return !checkGroupExpandableAndStartWarningActivity(i);
        }

        public void load() {
            AdapterData adapterData = this.mData;
            Objects.requireNonNull(adapterData);
            new AdapterData.AliasLoader().execute(new Void[0]);
        }

        public void remove(CertHolder certHolder) {
            this.mData.remove(certHolder);
        }

        public ChildAdapter getChildAdapter(int i) {
            return new ChildAdapter(this, i);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int i) {
            return checkGroupExpandableAndStartWarningActivity(i, true);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int i, boolean z) {
            UserHandle group = getGroup(i);
            int identifier = group.getIdentifier();
            if (!TrustedCredentialsSettings.this.mUserManager.isQuietModeEnabled(group)) {
                if (!TrustedCredentialsSettings.this.mUserManager.isUserUnlocked(group) && new LockPatternUtils(TrustedCredentialsSettings.this.getActivity()).isSeparateProfileChallengeEnabled(identifier)) {
                    if (z) {
                        TrustedCredentialsSettings.this.startConfirmCredential(identifier);
                    }
                    return false;
                }
                return true;
            }
            Intent createInQuietModeDialogIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(identifier);
            if (z) {
                TrustedCredentialsSettings.this.getActivity().startActivity(createInQuietModeDialogIntent);
            }
            return false;
        }

        private View getViewForCertificate(CertHolder certHolder, Tab tab, View view, ViewGroup viewGroup) {
            View view2;
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                view2 = LayoutInflater.from(TrustedCredentialsSettings.this.getActivity()).inflate(R.layout.trusted_credential, viewGroup, false);
                view2.setTag(viewHolder);
                viewHolder.mSubjectPrimaryView = (TextView) view2.findViewById(R.id.trusted_credential_subject_primary);
                viewHolder.mSubjectSecondaryView = (TextView) view2.findViewById(R.id.trusted_credential_subject_secondary);
                viewHolder.mSwitch = (Switch) view2.findViewById(R.id.trusted_credential_status);
                viewHolder.mSwitch.setOnClickListener(this);
            } else {
                view2 = view;
                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.mSubjectPrimaryView.setText(certHolder.mSubjectPrimary);
            viewHolder.mSubjectSecondaryView.setText(certHolder.mSubjectSecondary);
            if (tab.mSwitch) {
                viewHolder.mSwitch.setChecked(!certHolder.mDeleted);
                viewHolder.mSwitch.setEnabled(!TrustedCredentialsSettings.this.mUserManager.hasUserRestriction("no_config_credentials", new UserHandle(certHolder.mProfileId)));
                viewHolder.mSwitch.setVisibility(0);
                viewHolder.mSwitch.setTag(certHolder);
            }
            return view2;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class ViewHolder {
            private TextView mSubjectPrimaryView;
            private TextView mSubjectSecondaryView;
            private Switch mSwitch;

            private ViewHolder() {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ChildAdapter extends BaseAdapter implements View.OnClickListener, AdapterView.OnItemClickListener {
        private final int[] EMPTY_STATE_SET;
        private final int[] GROUP_EXPANDED_STATE_SET;
        private final LinearLayout.LayoutParams HIDE_CONTAINER_LAYOUT_PARAMS;
        private final LinearLayout.LayoutParams HIDE_LIST_LAYOUT_PARAMS;
        private final LinearLayout.LayoutParams SHOW_LAYOUT_PARAMS;
        private LinearLayout mContainerView;
        private final int mGroupPosition;
        private ViewGroup mHeaderView;
        private ImageView mIndicatorView;
        private boolean mIsListExpanded;
        private ListView mListView;
        private final DataSetObserver mObserver;
        private final GroupAdapter mParent;

        private ChildAdapter(GroupAdapter groupAdapter, int i) {
            this.GROUP_EXPANDED_STATE_SET = new int[]{16842920};
            this.EMPTY_STATE_SET = new int[0];
            this.HIDE_CONTAINER_LAYOUT_PARAMS = new LinearLayout.LayoutParams(-1, -2, 0.0f);
            this.HIDE_LIST_LAYOUT_PARAMS = new LinearLayout.LayoutParams(-1, 0);
            this.SHOW_LAYOUT_PARAMS = new LinearLayout.LayoutParams(-1, -1, 1.0f);
            this.mObserver = new DataSetObserver() { // from class: com.android.settings.TrustedCredentialsSettings.ChildAdapter.1
                @Override // android.database.DataSetObserver
                public void onChanged() {
                    super.onChanged();
                    ChildAdapter.super.notifyDataSetChanged();
                }

                @Override // android.database.DataSetObserver
                public void onInvalidated() {
                    super.onInvalidated();
                    ChildAdapter.super.notifyDataSetInvalidated();
                }
            };
            this.mIsListExpanded = true;
            this.mParent = groupAdapter;
            this.mGroupPosition = i;
            this.mParent.registerDataSetObserver(this.mObserver);
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return this.mParent.getChildrenCount(this.mGroupPosition);
        }

        @Override // android.widget.Adapter
        public CertHolder getItem(int i) {
            return this.mParent.getChild(this.mGroupPosition, i);
        }

        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return this.mParent.getChildId(this.mGroupPosition, i);
        }

        @Override // android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            return this.mParent.getChildView(this.mGroupPosition, i, false, view, viewGroup);
        }

        @Override // android.widget.BaseAdapter
        public void notifyDataSetChanged() {
            this.mParent.notifyDataSetChanged();
        }

        @Override // android.widget.BaseAdapter
        public void notifyDataSetInvalidated() {
            this.mParent.notifyDataSetInvalidated();
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            this.mIsListExpanded = checkGroupExpandableAndStartWarningActivity() && !this.mIsListExpanded;
            refreshViews();
        }

        @Override // android.widget.AdapterView.OnItemClickListener
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            TrustedCredentialsSettings.this.showCertDialog(getItem(i));
        }

        public void setContainerView(LinearLayout linearLayout) {
            this.mContainerView = linearLayout;
            this.mListView = (ListView) this.mContainerView.findViewById(R.id.cert_list);
            this.mListView.setAdapter((ListAdapter) this);
            this.mListView.setOnItemClickListener(this);
            this.mListView.setItemsCanFocus(true);
            this.mHeaderView = (ViewGroup) this.mContainerView.findViewById(R.id.header_view);
            this.mHeaderView.setOnClickListener(this);
            this.mIndicatorView = (ImageView) this.mHeaderView.findViewById(R.id.group_indicator);
            this.mIndicatorView.setImageDrawable(getGroupIndicator());
            FrameLayout frameLayout = (FrameLayout) this.mHeaderView.findViewById(R.id.header_content_container);
            frameLayout.addView(this.mParent.getGroupView(this.mGroupPosition, true, null, frameLayout));
        }

        public void showHeader(boolean z) {
            this.mHeaderView.setVisibility(z ? 0 : 8);
        }

        public void showDivider(boolean z) {
            this.mHeaderView.findViewById(R.id.header_divider).setVisibility(z ? 0 : 8);
        }

        public void setExpandIfAvailable(boolean z) {
            boolean z2 = false;
            if (z && this.mParent.checkGroupExpandableAndStartWarningActivity(this.mGroupPosition, false)) {
                z2 = true;
            }
            this.mIsListExpanded = z2;
            refreshViews();
        }

        private boolean checkGroupExpandableAndStartWarningActivity() {
            return this.mParent.checkGroupExpandableAndStartWarningActivity(this.mGroupPosition);
        }

        private void refreshViews() {
            this.mIndicatorView.setImageState(this.mIsListExpanded ? this.GROUP_EXPANDED_STATE_SET : this.EMPTY_STATE_SET, false);
            this.mListView.setLayoutParams(this.mIsListExpanded ? this.SHOW_LAYOUT_PARAMS : this.HIDE_LIST_LAYOUT_PARAMS);
            this.mContainerView.setLayoutParams(this.mIsListExpanded ? this.SHOW_LAYOUT_PARAMS : this.HIDE_CONTAINER_LAYOUT_PARAMS);
        }

        private Drawable getGroupIndicator() {
            TypedArray obtainStyledAttributes = TrustedCredentialsSettings.this.getActivity().obtainStyledAttributes(null, com.android.internal.R.styleable.ExpandableListView, 16842863, 0);
            Drawable drawable = obtainStyledAttributes.getDrawable(0);
            obtainStyledAttributes.recycle();
            return drawable;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AdapterData {
        private final GroupAdapter mAdapter;
        private final SparseArray<List<CertHolder>> mCertHoldersByUserId;
        private final Tab mTab;

        private AdapterData(Tab tab, GroupAdapter groupAdapter) {
            this.mCertHoldersByUserId = new SparseArray<>();
            this.mAdapter = groupAdapter;
            this.mTab = tab;
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class AliasLoader extends AsyncTask<Void, Integer, SparseArray<List<CertHolder>>> {
            private View mContentView;
            private Context mContext;
            private ProgressBar mProgressBar;

            public AliasLoader() {
                this.mContext = TrustedCredentialsSettings.this.getActivity();
                TrustedCredentialsSettings.this.mAliasLoaders.add(this);
                for (UserHandle userHandle : TrustedCredentialsSettings.this.mUserManager.getUserProfiles()) {
                    AdapterData.this.mCertHoldersByUserId.put(userHandle.getIdentifier(), new ArrayList());
                }
            }

            private boolean shouldSkipProfile(UserHandle userHandle) {
                return TrustedCredentialsSettings.this.mUserManager.isQuietModeEnabled(userHandle) || !TrustedCredentialsSettings.this.mUserManager.isUserUnlocked(userHandle.getIdentifier());
            }

            @Override // android.os.AsyncTask
            protected void onPreExecute() {
                FrameLayout tabContentView = TrustedCredentialsSettings.this.mTabHost.getTabContentView();
                this.mProgressBar = (ProgressBar) tabContentView.findViewById(AdapterData.this.mTab.mProgress);
                this.mContentView = tabContentView.findViewById(AdapterData.this.mTab.mContentView);
                this.mProgressBar.setVisibility(0);
                this.mContentView.setVisibility(8);
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public SparseArray<List<CertHolder>> doInBackground(Void... voidArr) {
                List<UserHandle> list;
                int i;
                SparseArray sparseArray;
                SparseArray<List<CertHolder>> sparseArray2 = new SparseArray<>();
                try {
                    synchronized (TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId) {
                        List<UserHandle> userProfiles = TrustedCredentialsSettings.this.mUserManager.getUserProfiles();
                        int size = userProfiles.size();
                        SparseArray sparseArray3 = new SparseArray(size);
                        int i2 = 0;
                        for (int i3 = 0; i3 < size; i3++) {
                            UserHandle userHandle = userProfiles.get(i3);
                            int identifier = userHandle.getIdentifier();
                            if (!shouldSkipProfile(userHandle)) {
                                KeyChain.KeyChainConnection bindAsUser = KeyChain.bindAsUser(this.mContext, userHandle);
                                TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.put(identifier, bindAsUser);
                                List aliases = AdapterData.this.mTab.getAliases(bindAsUser.getService());
                                if (isCancelled()) {
                                    return new SparseArray<>();
                                }
                                i2 += aliases.size();
                                sparseArray3.put(identifier, aliases);
                            }
                        }
                        int i4 = 0;
                        int i5 = 0;
                        while (i4 < size) {
                            UserHandle userHandle2 = userProfiles.get(i4);
                            int identifier2 = userHandle2.getIdentifier();
                            List list2 = (List) sparseArray3.get(identifier2);
                            if (!isCancelled()) {
                                KeyChain.KeyChainConnection keyChainConnection = (KeyChain.KeyChainConnection) TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.get(identifier2);
                                if (!shouldSkipProfile(userHandle2) && list2 != null && keyChainConnection != null) {
                                    IKeyChainService service = keyChainConnection.getService();
                                    ArrayList arrayList = new ArrayList(i2);
                                    int size2 = list2.size();
                                    int i6 = i5;
                                    int i7 = 0;
                                    while (i7 < size2) {
                                        String str = (String) list2.get(i7);
                                        int i8 = size;
                                        SparseArray sparseArray4 = sparseArray3;
                                        ArrayList arrayList2 = arrayList;
                                        int i9 = identifier2;
                                        arrayList2.add(new CertHolder(service, AdapterData.this.mAdapter, AdapterData.this.mTab, str, KeyChain.toCertificate(service.getEncodedCaCertificate(str, true)), i9));
                                        int i10 = i6 + 1;
                                        publishProgress(Integer.valueOf(i10), Integer.valueOf(i2));
                                        i7++;
                                        identifier2 = i9;
                                        arrayList = arrayList2;
                                        list2 = list2;
                                        i6 = i10;
                                        userProfiles = userProfiles;
                                        size = i8;
                                        size2 = size2;
                                        sparseArray3 = sparseArray4;
                                    }
                                    list = userProfiles;
                                    i = size;
                                    sparseArray = sparseArray3;
                                    ArrayList arrayList3 = arrayList;
                                    Collections.sort(arrayList3);
                                    sparseArray2.put(identifier2, arrayList3);
                                    i5 = i6;
                                    i4++;
                                    userProfiles = list;
                                    size = i;
                                    sparseArray3 = sparseArray;
                                }
                                list = userProfiles;
                                i = size;
                                sparseArray = sparseArray3;
                                sparseArray2.put(identifier2, new ArrayList(0));
                                i4++;
                                userProfiles = list;
                                size = i;
                                sparseArray3 = sparseArray;
                            } else {
                                return new SparseArray<>();
                            }
                        }
                        return sparseArray2;
                    }
                } catch (RemoteException e) {
                    Log.e("TrustedCredentialsSettings", "Remote exception while loading aliases.", e);
                    return new SparseArray<>();
                } catch (InterruptedException e2) {
                    Log.e("TrustedCredentialsSettings", "InterruptedException while loading aliases.", e2);
                    return new SparseArray<>();
                }
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public void onProgressUpdate(Integer... numArr) {
                int intValue = numArr[0].intValue();
                int intValue2 = numArr[1].intValue();
                if (intValue2 != this.mProgressBar.getMax()) {
                    this.mProgressBar.setMax(intValue2);
                }
                this.mProgressBar.setProgress(intValue);
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public void onPostExecute(SparseArray<List<CertHolder>> sparseArray) {
                AdapterData.this.mCertHoldersByUserId.clear();
                int size = sparseArray.size();
                for (int i = 0; i < size; i++) {
                    AdapterData.this.mCertHoldersByUserId.put(sparseArray.keyAt(i), sparseArray.valueAt(i));
                }
                AdapterData.this.mAdapter.notifyDataSetChanged();
                this.mProgressBar.setVisibility(8);
                this.mContentView.setVisibility(0);
                this.mProgressBar.setProgress(0);
                TrustedCredentialsSettings.this.mAliasLoaders.remove(this);
                showTrustAllCaDialogIfNeeded();
            }

            private boolean isUserTabAndTrustAllCertMode() {
                return TrustedCredentialsSettings.this.isTrustAllCaCertModeInProgress() && AdapterData.this.mTab == Tab.USER;
            }

            private void showTrustAllCaDialogIfNeeded() {
                List<CertHolder> list;
                if (!isUserTabAndTrustAllCertMode() || (list = (List) AdapterData.this.mCertHoldersByUserId.get(TrustedCredentialsSettings.this.mTrustAllCaUserId)) == null) {
                    return;
                }
                ArrayList arrayList = new ArrayList();
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
                for (CertHolder certHolder : list) {
                    if (certHolder != null && !devicePolicyManager.isCaCertApproved(certHolder.mAlias, TrustedCredentialsSettings.this.mTrustAllCaUserId)) {
                        arrayList.add(certHolder);
                    }
                }
                if (arrayList.size() == 0) {
                    Log.w("TrustedCredentialsSettings", "no cert is pending approval for user " + TrustedCredentialsSettings.this.mTrustAllCaUserId);
                    return;
                }
                TrustedCredentialsSettings.this.showTrustAllCaDialog(arrayList);
            }
        }

        public void remove(CertHolder certHolder) {
            List<CertHolder> list;
            if (this.mCertHoldersByUserId != null && (list = this.mCertHoldersByUserId.get(certHolder.mProfileId)) != null) {
                list.remove(certHolder);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class CertHolder implements Comparable<CertHolder> {
        private final GroupAdapter mAdapter;
        private final String mAlias;
        private boolean mDeleted;
        public int mProfileId;
        private final IKeyChainService mService;
        private final SslCertificate mSslCert;
        private final String mSubjectPrimary;
        private final String mSubjectSecondary;
        private final Tab mTab;
        private final X509Certificate mX509Cert;

        private CertHolder(IKeyChainService iKeyChainService, GroupAdapter groupAdapter, Tab tab, String str, X509Certificate x509Certificate, int i) {
            this.mProfileId = i;
            this.mService = iKeyChainService;
            this.mAdapter = groupAdapter;
            this.mTab = tab;
            this.mAlias = str;
            this.mX509Cert = x509Certificate;
            this.mSslCert = new SslCertificate(x509Certificate);
            String cName = this.mSslCert.getIssuedTo().getCName();
            String oName = this.mSslCert.getIssuedTo().getOName();
            String uName = this.mSslCert.getIssuedTo().getUName();
            if (!oName.isEmpty()) {
                if (!cName.isEmpty()) {
                    this.mSubjectPrimary = oName;
                    this.mSubjectSecondary = cName;
                } else {
                    this.mSubjectPrimary = oName;
                    this.mSubjectSecondary = uName;
                }
            } else if (!cName.isEmpty()) {
                this.mSubjectPrimary = cName;
                this.mSubjectSecondary = "";
            } else {
                this.mSubjectPrimary = this.mSslCert.getIssuedTo().getDName();
                this.mSubjectSecondary = "";
            }
            try {
                this.mDeleted = this.mTab.deleted(this.mService, this.mAlias);
            } catch (RemoteException e) {
                Log.e("TrustedCredentialsSettings", "Remote exception while checking if alias " + this.mAlias + " is deleted.", e);
                this.mDeleted = false;
            }
        }

        @Override // java.lang.Comparable
        public int compareTo(CertHolder certHolder) {
            int compareToIgnoreCase = this.mSubjectPrimary.compareToIgnoreCase(certHolder.mSubjectPrimary);
            if (compareToIgnoreCase != 0) {
                return compareToIgnoreCase;
            }
            return this.mSubjectSecondary.compareToIgnoreCase(certHolder.mSubjectSecondary);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof CertHolder)) {
                return false;
            }
            return this.mAlias.equals(((CertHolder) obj).mAlias);
        }

        public int hashCode() {
            return this.mAlias.hashCode();
        }

        public int getUserId() {
            return this.mProfileId;
        }

        public String getAlias() {
            return this.mAlias;
        }

        public boolean isSystemCert() {
            return this.mTab == Tab.SYSTEM;
        }

        public boolean isDeleted() {
            return this.mDeleted;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTrustAllCaCertModeInProgress() {
        return this.mTrustAllCaUserId != -10000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showTrustAllCaDialog(List<CertHolder> list) {
        new TrustedCredentialsDialogBuilder(getActivity(), this).setCertHolders((CertHolder[]) list.toArray(new CertHolder[list.size()])).setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.settings.TrustedCredentialsSettings.2
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                TrustedCredentialsSettings.this.getActivity().getIntent().removeExtra("ARG_SHOW_NEW_FOR_USER");
                TrustedCredentialsSettings.this.mTrustAllCaUserId = -10000;
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showCertDialog(CertHolder certHolder) {
        new TrustedCredentialsDialogBuilder(getActivity(), this).setCertHolder(certHolder).show();
    }

    @Override // com.android.settings.TrustedCredentialsDialogBuilder.DelegateInterface
    public List<X509Certificate> getX509CertsFromCertHolder(CertHolder certHolder) {
        ArrayList arrayList = null;
        try {
            synchronized (this.mKeyChainConnectionByProfileId) {
                try {
                    IKeyChainService service = this.mKeyChainConnectionByProfileId.get(certHolder.mProfileId).getService();
                    List caCertificateChainAliases = service.getCaCertificateChainAliases(certHolder.mAlias, true);
                    int size = caCertificateChainAliases.size();
                    ArrayList arrayList2 = new ArrayList(size);
                    for (int i = 0; i < size; i++) {
                        try {
                            arrayList2.add(KeyChain.toCertificate(service.getEncodedCaCertificate((String) caCertificateChainAliases.get(i), true)));
                        } catch (Throwable th) {
                            th = th;
                            arrayList = arrayList2;
                        }
                    }
                    return arrayList2;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            throw th;
        } catch (RemoteException e) {
            ArrayList arrayList3 = arrayList;
            Log.e("TrustedCredentialsSettings", "RemoteException while retrieving certificate chain for root " + certHolder.mAlias, e);
            return arrayList3;
        }
    }

    @Override // com.android.settings.TrustedCredentialsDialogBuilder.DelegateInterface
    public void removeOrInstallCert(CertHolder certHolder) {
        new AliasOperation(certHolder).execute(new Void[0]);
    }

    @Override // com.android.settings.TrustedCredentialsDialogBuilder.DelegateInterface
    public boolean startConfirmCredentialIfNotConfirmed(int i, IntConsumer intConsumer) {
        if (this.mConfirmedCredentialUsers.contains(Integer.valueOf(i))) {
            return false;
        }
        boolean startConfirmCredential = startConfirmCredential(i);
        if (startConfirmCredential) {
            this.mConfirmingCredentialListener = intConsumer;
        }
        return startConfirmCredential;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;

        private AliasOperation(CertHolder certHolder) {
            this.mCertHolder = certHolder;
            TrustedCredentialsSettings.this.mAliasOperation = this;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Boolean doInBackground(Void... voidArr) {
            try {
                synchronized (TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId) {
                    IKeyChainService service = ((KeyChain.KeyChainConnection) TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.get(this.mCertHolder.mProfileId)).getService();
                    if (!this.mCertHolder.mDeleted) {
                        return Boolean.valueOf(service.deleteCaCertificate(this.mCertHolder.mAlias));
                    }
                    service.installCaCertificate(this.mCertHolder.mX509Cert.getEncoded());
                    return true;
                }
            } catch (RemoteException | IllegalStateException | SecurityException | CertificateEncodingException e) {
                Log.w("TrustedCredentialsSettings", "Error while toggling alias " + this.mCertHolder.mAlias, e);
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Boolean bool) {
            if (bool.booleanValue()) {
                if (!this.mCertHolder.mTab.mSwitch) {
                    this.mCertHolder.mAdapter.remove(this.mCertHolder);
                } else {
                    this.mCertHolder.mDeleted = !this.mCertHolder.mDeleted;
                }
                this.mCertHolder.mAdapter.notifyDataSetChanged();
            } else {
                this.mCertHolder.mAdapter.load();
            }
            TrustedCredentialsSettings.this.mAliasOperation = null;
        }
    }
}
