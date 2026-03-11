package com.android.settings;

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
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.util.ParcelableString;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsDialogBuilder;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

public class TrustedCredentialsSettings extends OptionsMenuFragment implements TrustedCredentialsDialogBuilder.DelegateInterface {
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
    private final SparseArray<KeyChain.KeyChainConnection> mKeyChainConnectionByProfileId = new SparseArray<>();
    private BroadcastReceiver mWorkProfileChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.intent.action.MANAGED_PROFILE_AVAILABLE".equals(action) && !"android.intent.action.MANAGED_PROFILE_UNAVAILABLE".equals(action) && !"android.intent.action.MANAGED_PROFILE_UNLOCKED".equals(action)) {
                return;
            }
            for (GroupAdapter adapter : TrustedCredentialsSettings.this.mGroupAdapters) {
                adapter.load();
            }
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 92;
    }

    private enum Tab {
        SYSTEM("system", R.string.trusted_credentials_system_tab, R.id.system_tab, R.id.system_progress, R.id.system_personal_container, R.id.system_work_container, R.id.system_expandable_list, R.id.system_content, true),
        USER("user", R.string.trusted_credentials_user_tab, R.id.user_tab, R.id.user_progress, R.id.user_personal_container, R.id.user_work_container, R.id.user_expandable_list, R.id.user_content, false);


        private static final int[] f6x88974370 = null;
        private final int mContentView;
        private final int mExpandableList;
        private final int mLabel;
        private final int mPersonalList;
        private final int mProgress;
        private final boolean mSwitch;
        private final String mTag;
        private final int mView;
        private final int mWorkList;

        private static int[] m584xab29b14() {
            if (f6x88974370 != null) {
                return f6x88974370;
            }
            int[] iArr = new int[valuesCustom().length];
            try {
                iArr[SYSTEM.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[USER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            f6x88974370 = iArr;
            return iArr;
        }

        public static Tab[] valuesCustom() {
            return values();
        }

        Tab(String tag, int label, int view, int progress, int personalList, int workList, int expandableList, int contentView, boolean withSwitch) {
            this.mTag = tag;
            this.mLabel = label;
            this.mView = view;
            this.mProgress = progress;
            this.mPersonalList = personalList;
            this.mWorkList = workList;
            this.mExpandableList = expandableList;
            this.mContentView = contentView;
            this.mSwitch = withSwitch;
        }

        public List<ParcelableString> getAliases(IKeyChainService service) throws RemoteException {
            switch (m584xab29b14()[ordinal()]) {
                case DefaultWfcSettingsExt.PAUSE:
                    return service.getSystemCaAliases().getList();
                case DefaultWfcSettingsExt.CREATE:
                    return service.getUserCaAliases().getList();
                default:
                    throw new AssertionError();
            }
        }

        public boolean deleted(IKeyChainService service, String alias) throws RemoteException {
            switch (m584xab29b14()[ordinal()]) {
                case DefaultWfcSettingsExt.PAUSE:
                    return !service.containsCaAlias(alias);
                case DefaultWfcSettingsExt.CREATE:
                    return false;
                default:
                    throw new AssertionError();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUserManager = (UserManager) getActivity().getSystemService("user");
        this.mKeyguardManager = (KeyguardManager) getActivity().getSystemService("keyguard");
        this.mTrustAllCaUserId = getActivity().getIntent().getIntExtra("ARG_SHOW_NEW_FOR_USER", -10000);
        this.mConfirmedCredentialUsers = new ArraySet<>(2);
        this.mConfirmingCredentialUser = -10000;
        if (savedInstanceState != null) {
            this.mConfirmingCredentialUser = savedInstanceState.getInt("ConfirmingCredentialUser", -10000);
            ArrayList<Integer> users = savedInstanceState.getIntegerArrayList("ConfirmedCredentialUsers");
            if (users != null) {
                this.mConfirmedCredentialUsers.addAll(users);
            }
        }
        this.mConfirmingCredentialListener = null;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNLOCKED");
        getActivity().registerReceiver(this.mWorkProfileChangedReceiver, filter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList("ConfirmedCredentialUsers", new ArrayList<>(this.mConfirmedCredentialUsers));
        outState.putInt("ConfirmingCredentialUser", this.mConfirmingCredentialUser);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.mTabHost = (TabHost) inflater.inflate(R.layout.trusted_credentials, parent, false);
        this.mTabHost.setup();
        addTab(Tab.SYSTEM);
        addTab(Tab.USER);
        if (getActivity().getIntent() != null && "com.android.settings.TRUSTED_CREDENTIALS_USER".equals(getActivity().getIntent().getAction())) {
            this.mTabHost.setCurrentTabByTag(Tab.USER.mTag);
        }
        return this.mTabHost;
    }

    @Override
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1) {
            return;
        }
        int userId = this.mConfirmingCredentialUser;
        IntConsumer listener = this.mConfirmingCredentialListener;
        this.mConfirmingCredentialUser = -10000;
        this.mConfirmingCredentialListener = null;
        if (resultCode != -1) {
            return;
        }
        this.mConfirmedCredentialUsers.add(Integer.valueOf(userId));
        if (listener == null) {
            return;
        }
        listener.accept(userId);
    }

    private void closeKeyChainConnections() {
        int n = this.mKeyChainConnectionByProfileId.size();
        for (int i = 0; i < n; i++) {
            this.mKeyChainConnectionByProfileId.valueAt(i).close();
        }
        this.mKeyChainConnectionByProfileId.clear();
    }

    private void addTab(Tab tab) {
        TabHost.TabSpec systemSpec = this.mTabHost.newTabSpec(tab.mTag).setIndicator(getActivity().getString(tab.mLabel)).setContent(tab.mView);
        this.mTabHost.addTab(systemSpec);
        int profilesSize = this.mUserManager.getUserProfiles().size();
        GroupAdapter groupAdapter = new GroupAdapter(this, tab, null);
        this.mGroupAdapters.add(groupAdapter);
        if (profilesSize == 1) {
            ChildAdapter adapter = groupAdapter.getChildAdapter(0);
            adapter.setContainerViewId(tab.mPersonalList);
            adapter.prepare();
            return;
        }
        if (profilesSize == 2) {
            int workIndex = groupAdapter.getUserInfoByGroup(1).isManagedProfile() ? 1 : 0;
            int personalIndex = workIndex != 1 ? 1 : 0;
            ChildAdapter personalAdapter = groupAdapter.getChildAdapter(personalIndex);
            personalAdapter.setContainerViewId(tab.mPersonalList);
            personalAdapter.showHeader(true);
            personalAdapter.prepare();
            ChildAdapter workAdapter = groupAdapter.getChildAdapter(workIndex);
            workAdapter.setContainerViewId(tab.mWorkList);
            workAdapter.showHeader(true);
            workAdapter.showDivider(true);
            workAdapter.prepare();
            return;
        }
        if (profilesSize < 3) {
            return;
        }
        groupAdapter.setExpandableListView((ExpandableListView) this.mTabHost.findViewById(tab.mExpandableList));
    }

    public boolean startConfirmCredential(int userId) {
        Intent newIntent = this.mKeyguardManager.createConfirmDeviceCredentialIntent(null, null, userId);
        if (newIntent == null) {
            return false;
        }
        this.mConfirmingCredentialUser = userId;
        startActivityForResult(newIntent, 1);
        return true;
    }

    private class GroupAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener {
        private final AdapterData mData;

        GroupAdapter(TrustedCredentialsSettings this$0, Tab tab, GroupAdapter groupAdapter) {
            this(tab);
        }

        private GroupAdapter(Tab tab) {
            this.mData = new AdapterData(TrustedCredentialsSettings.this, tab, this, null);
            load();
        }

        @Override
        public int getGroupCount() {
            return this.mData.mCertHoldersByUserId.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            List<CertHolder> certHolders = (List) this.mData.mCertHoldersByUserId.valueAt(groupPosition);
            if (certHolders != null) {
                return certHolders.size();
            }
            return 0;
        }

        @Override
        public UserHandle getGroup(int groupPosition) {
            return new UserHandle(this.mData.mCertHoldersByUserId.keyAt(groupPosition));
        }

        @Override
        public CertHolder getChild(int groupPosition, int childPosition) {
            return (CertHolder) ((List) this.mData.mCertHoldersByUserId.get(getUserIdByGroup(groupPosition))).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return getUserIdByGroup(groupPosition);
        }

        private int getUserIdByGroup(int groupPosition) {
            return this.mData.mCertHoldersByUserId.keyAt(groupPosition);
        }

        public UserInfo getUserInfoByGroup(int groupPosition) {
            return TrustedCredentialsSettings.this.mUserManager.getUserInfo(getUserIdByGroup(groupPosition));
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) TrustedCredentialsSettings.this.getActivity().getSystemService("layout_inflater");
                convertView = Utils.inflateCategoryHeader(inflater, parent);
            }
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            if (getUserInfoByGroup(groupPosition).isManagedProfile()) {
                title.setText(R.string.category_work);
            } else {
                title.setText(R.string.category_personal);
            }
            title.setTextAlignment(6);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            return getViewForCertificate(getChild(groupPosition, childPosition), this.mData.mTab, convertView, parent);
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
            TrustedCredentialsSettings.this.showCertDialog(getChild(groupPosition, childPosition));
            return true;
        }

        @Override
        public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long id) {
            return !checkGroupExpandableAndStartWarningActivity(groupPosition);
        }

        public void load() {
            AdapterData adapterData = this.mData;
            adapterData.getClass();
            adapterData.new AliasLoader().execute(new Void[0]);
        }

        public void remove(CertHolder certHolder) {
            this.mData.remove(certHolder);
        }

        public void setExpandableListView(ExpandableListView lv) {
            lv.setAdapter(this);
            lv.setOnGroupClickListener(this);
            lv.setOnChildClickListener(this);
            lv.setVisibility(0);
        }

        public ChildAdapter getChildAdapter(int groupPosition) {
            return new ChildAdapter(TrustedCredentialsSettings.this, this, groupPosition, null);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int groupPosition) {
            return checkGroupExpandableAndStartWarningActivity(groupPosition, true);
        }

        public boolean checkGroupExpandableAndStartWarningActivity(int groupPosition, boolean startActivity) {
            UserHandle groupUser = getGroup(groupPosition);
            int groupUserId = groupUser.getIdentifier();
            if (TrustedCredentialsSettings.this.mUserManager.isQuietModeEnabled(groupUser)) {
                Intent intent = UnlaunchableAppActivity.createInQuietModeDialogIntent(groupUserId);
                if (startActivity) {
                    TrustedCredentialsSettings.this.getActivity().startActivity(intent);
                }
                return false;
            }
            if (!TrustedCredentialsSettings.this.mUserManager.isUserUnlocked(groupUser)) {
                LockPatternUtils lockPatternUtils = new LockPatternUtils(TrustedCredentialsSettings.this.getActivity());
                if (lockPatternUtils.isSeparateProfileChallengeEnabled(groupUserId)) {
                    if (startActivity) {
                        TrustedCredentialsSettings.this.startConfirmCredential(groupUserId);
                    }
                    return false;
                }
                return true;
            }
            return true;
        }

        private View getViewForCertificate(CertHolder certHolder, Tab mTab, View convertView, ViewGroup parent) {
            ViewHolder holder;
            ViewHolder viewHolder = null;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(TrustedCredentialsSettings.this.getActivity());
                convertView = inflater.inflate(R.layout.trusted_credential, parent, false);
                holder = new ViewHolder(this, viewHolder);
                holder.mSubjectPrimaryView = (TextView) convertView.findViewById(R.id.trusted_credential_subject_primary);
                holder.mSubjectSecondaryView = (TextView) convertView.findViewById(R.id.trusted_credential_subject_secondary);
                holder.mSwitch = (Switch) convertView.findViewById(R.id.trusted_credential_status);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mSubjectPrimaryView.setText(certHolder.mSubjectPrimary);
            holder.mSubjectSecondaryView.setText(certHolder.mSubjectSecondary);
            if (mTab.mSwitch) {
                holder.mSwitch.setChecked(!certHolder.mDeleted);
                holder.mSwitch.setEnabled(TrustedCredentialsSettings.this.mUserManager.hasUserRestriction("no_config_credentials", new UserHandle(certHolder.mProfileId)) ? false : true);
                holder.mSwitch.setVisibility(0);
            }
            return convertView;
        }

        private class ViewHolder {
            private TextView mSubjectPrimaryView;
            private TextView mSubjectSecondaryView;
            private Switch mSwitch;

            ViewHolder(GroupAdapter this$1, ViewHolder viewHolder) {
                this();
            }

            private ViewHolder() {
            }
        }
    }

    private class ChildAdapter extends BaseAdapter implements View.OnClickListener, AdapterView.OnItemClickListener {
        private final int[] EMPTY_STATE_SET;
        private final int[] GROUP_EXPANDED_STATE_SET;
        private final LinearLayout.LayoutParams HIDE_LAYOUT_PARAMS;
        private final LinearLayout.LayoutParams SHOW_LAYOUT_PARAMS;
        private LinearLayout mContainerView;
        private final int mGroupPosition;
        private ViewGroup mHeaderView;
        private ImageView mIndicatorView;
        private boolean mIsListExpanded;
        private ListView mListView;
        private final DataSetObserver mObserver;
        private final GroupAdapter mParent;

        ChildAdapter(TrustedCredentialsSettings this$0, GroupAdapter parent, int groupPosition, ChildAdapter childAdapter) {
            this(parent, groupPosition);
        }

        private ChildAdapter(GroupAdapter parent, int groupPosition) {
            this.GROUP_EXPANDED_STATE_SET = new int[]{android.R.attr.state_expanded};
            this.EMPTY_STATE_SET = new int[0];
            this.HIDE_LAYOUT_PARAMS = new LinearLayout.LayoutParams(-1, -2);
            this.SHOW_LAYOUT_PARAMS = new LinearLayout.LayoutParams(-1, -1, 1.0f);
            this.mObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    ChildAdapter.super.notifyDataSetChanged();
                }

                @Override
                public void onInvalidated() {
                    super.onInvalidated();
                    ChildAdapter.super.notifyDataSetInvalidated();
                }
            };
            this.mIsListExpanded = true;
            this.mParent = parent;
            this.mGroupPosition = groupPosition;
            this.mParent.registerDataSetObserver(this.mObserver);
        }

        @Override
        public int getCount() {
            return this.mParent.getChildrenCount(this.mGroupPosition);
        }

        @Override
        public CertHolder getItem(int position) {
            return this.mParent.getChild(this.mGroupPosition, position);
        }

        @Override
        public long getItemId(int position) {
            return this.mParent.getChildId(this.mGroupPosition, position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return this.mParent.getChildView(this.mGroupPosition, position, false, convertView, parent);
        }

        @Override
        public void notifyDataSetChanged() {
            this.mParent.notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetInvalidated() {
            this.mParent.notifyDataSetInvalidated();
        }

        @Override
        public void onClick(View view) {
            boolean z = false;
            if (checkGroupExpandableAndStartWarningActivity() && !this.mIsListExpanded) {
                z = true;
            }
            this.mIsListExpanded = z;
            refreshViews();
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            TrustedCredentialsSettings.this.showCertDialog(getItem(pos));
        }

        public void setContainerViewId(int viewId) {
            this.mContainerView = (LinearLayout) TrustedCredentialsSettings.this.mTabHost.findViewById(viewId);
            this.mContainerView.setVisibility(0);
            this.mListView = (ListView) this.mContainerView.findViewById(R.id.cert_list);
            this.mListView.setAdapter((ListAdapter) this);
            this.mListView.setOnItemClickListener(this);
            this.mHeaderView = (ViewGroup) this.mContainerView.findViewById(R.id.header_view);
            this.mHeaderView.setOnClickListener(this);
            this.mIndicatorView = (ImageView) this.mHeaderView.findViewById(R.id.group_indicator);
            this.mIndicatorView.setImageDrawable(getGroupIndicator());
            FrameLayout headerContentContainer = (FrameLayout) this.mHeaderView.findViewById(R.id.header_content_container);
            headerContentContainer.addView(this.mParent.getGroupView(this.mGroupPosition, true, null, headerContentContainer));
        }

        public void showHeader(boolean showHeader) {
            this.mHeaderView.setVisibility(showHeader ? 0 : 8);
        }

        public void showDivider(boolean showDivider) {
            View dividerView = this.mHeaderView.findViewById(R.id.header_divider);
            dividerView.setVisibility(showDivider ? 0 : 8);
        }

        public void prepare() {
            this.mIsListExpanded = this.mParent.checkGroupExpandableAndStartWarningActivity(this.mGroupPosition, false);
            refreshViews();
        }

        private boolean checkGroupExpandableAndStartWarningActivity() {
            return this.mParent.checkGroupExpandableAndStartWarningActivity(this.mGroupPosition);
        }

        private void refreshViews() {
            this.mIndicatorView.setImageState(this.mIsListExpanded ? this.GROUP_EXPANDED_STATE_SET : this.EMPTY_STATE_SET, false);
            this.mListView.setVisibility(this.mIsListExpanded ? 0 : 8);
            this.mContainerView.setLayoutParams(this.mIsListExpanded ? this.SHOW_LAYOUT_PARAMS : this.HIDE_LAYOUT_PARAMS);
        }

        private Drawable getGroupIndicator() {
            TypedArray a = TrustedCredentialsSettings.this.getActivity().obtainStyledAttributes(null, com.android.internal.R.styleable.ExpandableListView, android.R.attr.expandableListViewStyle, 0);
            Drawable groupIndicator = a.getDrawable(0);
            a.recycle();
            return groupIndicator;
        }
    }

    private class AdapterData {
        private final GroupAdapter mAdapter;
        private final SparseArray<List<CertHolder>> mCertHoldersByUserId;
        private final Tab mTab;

        AdapterData(TrustedCredentialsSettings this$0, Tab tab, GroupAdapter adapter, AdapterData adapterData) {
            this(tab, adapter);
        }

        private AdapterData(Tab tab, GroupAdapter adapter) {
            this.mCertHoldersByUserId = new SparseArray<>();
            this.mAdapter = adapter;
            this.mTab = tab;
        }

        private class AliasLoader extends AsyncTask<Void, Integer, SparseArray<List<CertHolder>>> {
            private View mContentView;
            private Context mContext;
            private ProgressBar mProgressBar;

            public AliasLoader() {
                this.mContext = TrustedCredentialsSettings.this.getActivity();
                TrustedCredentialsSettings.this.mAliasLoaders.add(this);
                List<UserHandle> profiles = TrustedCredentialsSettings.this.mUserManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                    AdapterData.this.mCertHoldersByUserId.put(profile.getIdentifier(), new ArrayList());
                }
            }

            private boolean shouldSkipProfile(UserHandle userHandle) {
                return TrustedCredentialsSettings.this.mUserManager.isQuietModeEnabled(userHandle) || !TrustedCredentialsSettings.this.mUserManager.isUserUnlocked(userHandle.getIdentifier());
            }

            @Override
            protected void onPreExecute() {
                View content = TrustedCredentialsSettings.this.mTabHost.getTabContentView();
                this.mProgressBar = (ProgressBar) content.findViewById(AdapterData.this.mTab.mProgress);
                this.mContentView = content.findViewById(AdapterData.this.mTab.mContentView);
                this.mProgressBar.setVisibility(0);
                this.mContentView.setVisibility(8);
            }

            @Override
            public SparseArray<List<CertHolder>> doInBackground(Void... params) {
                SparseArray<List<CertHolder>> certHoldersByProfile = new SparseArray<>();
                try {
                    List<UserHandle> profiles = TrustedCredentialsSettings.this.mUserManager.getUserProfiles();
                    int n = profiles.size();
                    SparseArray<List<ParcelableString>> aliasesByProfileId = new SparseArray<>(n);
                    int max = 0;
                    int progress = 0;
                    for (int i = 0; i < n; i++) {
                        UserHandle profile = profiles.get(i);
                        int profileId = profile.getIdentifier();
                        if (!shouldSkipProfile(profile)) {
                            KeyChain.KeyChainConnection keyChainConnection = KeyChain.bindAsUser(this.mContext, profile);
                            TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.put(profileId, keyChainConnection);
                            List<ParcelableString> aliases = AdapterData.this.mTab.getAliases(keyChainConnection.getService());
                            if (isCancelled()) {
                                return new SparseArray<>();
                            }
                            max += aliases.size();
                            aliasesByProfileId.put(profileId, aliases);
                        }
                    }
                    for (int i2 = 0; i2 < n; i2++) {
                        UserHandle profile2 = profiles.get(i2);
                        int profileId2 = profile2.getIdentifier();
                        List<ParcelableString> aliases2 = aliasesByProfileId.get(profileId2);
                        if (isCancelled()) {
                            return new SparseArray<>();
                        }
                        KeyChain.KeyChainConnection keyChainConnection2 = (KeyChain.KeyChainConnection) TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.get(profileId2);
                        if (shouldSkipProfile(profile2) || aliases2 == null || keyChainConnection2 == null) {
                            certHoldersByProfile.put(profileId2, new ArrayList(0));
                        } else {
                            IKeyChainService service = keyChainConnection2.getService();
                            List<CertHolder> certHolders = new ArrayList<>(max);
                            int aliasMax = aliases2.size();
                            for (int j = 0; j < aliasMax; j++) {
                                String alias = aliases2.get(j).string;
                                byte[] encodedCertificate = service.getEncodedCaCertificate(alias, true);
                                X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                                certHolders.add(new CertHolder(service, AdapterData.this.mAdapter, AdapterData.this.mTab, alias, cert, profileId2, null));
                                progress++;
                                publishProgress(Integer.valueOf(progress), Integer.valueOf(max));
                            }
                            Collections.sort(certHolders);
                            certHoldersByProfile.put(profileId2, certHolders);
                        }
                    }
                    return certHoldersByProfile;
                } catch (RemoteException e) {
                    Log.e("TrustedCredentialsSettings", "Remote exception while loading aliases.", e);
                    return new SparseArray<>();
                } catch (InterruptedException e2) {
                    Log.e("TrustedCredentialsSettings", "InterruptedException while loading aliases.", e2);
                    return new SparseArray<>();
                }
            }

            @Override
            public void onProgressUpdate(Integer... progressAndMax) {
                int progress = progressAndMax[0].intValue();
                int max = progressAndMax[1].intValue();
                if (max != this.mProgressBar.getMax()) {
                    this.mProgressBar.setMax(max);
                }
                this.mProgressBar.setProgress(progress);
            }

            @Override
            public void onPostExecute(SparseArray<List<CertHolder>> certHolders) {
                AdapterData.this.mCertHoldersByUserId.clear();
                int n = certHolders.size();
                for (int i = 0; i < n; i++) {
                    AdapterData.this.mCertHoldersByUserId.put(certHolders.keyAt(i), certHolders.valueAt(i));
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
                List<CertHolder> certHolders;
                if (!isUserTabAndTrustAllCertMode() || (certHolders = (List) AdapterData.this.mCertHoldersByUserId.get(TrustedCredentialsSettings.this.mTrustAllCaUserId)) == null) {
                    return;
                }
                List<CertHolder> unapprovedUserCertHolders = new ArrayList<>();
                DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
                for (CertHolder cert : certHolders) {
                    if (cert != null && !dpm.isCaCertApproved(cert.mAlias, TrustedCredentialsSettings.this.mTrustAllCaUserId)) {
                        unapprovedUserCertHolders.add(cert);
                    }
                }
                if (unapprovedUserCertHolders.size() == 0) {
                    Log.w("TrustedCredentialsSettings", "no cert is pending approval for user " + TrustedCredentialsSettings.this.mTrustAllCaUserId);
                } else {
                    TrustedCredentialsSettings.this.showTrustAllCaDialog(unapprovedUserCertHolders);
                }
            }
        }

        public void remove(CertHolder certHolder) {
            List<CertHolder> certs;
            if (this.mCertHoldersByUserId == null || (certs = this.mCertHoldersByUserId.get(certHolder.mProfileId)) == null) {
                return;
            }
            certs.remove(certHolder);
        }
    }

    static class CertHolder implements Comparable<CertHolder> {
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

        CertHolder(IKeyChainService service, GroupAdapter adapter, Tab tab, String alias, X509Certificate x509Cert, int profileId, CertHolder certHolder) {
            this(service, adapter, tab, alias, x509Cert, profileId);
        }

        private CertHolder(IKeyChainService service, GroupAdapter adapter, Tab tab, String alias, X509Certificate x509Cert, int profileId) {
            this.mProfileId = profileId;
            this.mService = service;
            this.mAdapter = adapter;
            this.mTab = tab;
            this.mAlias = alias;
            this.mX509Cert = x509Cert;
            this.mSslCert = new SslCertificate(x509Cert);
            String cn = this.mSslCert.getIssuedTo().getCName();
            String o = this.mSslCert.getIssuedTo().getOName();
            String ou = this.mSslCert.getIssuedTo().getUName();
            if (!o.isEmpty()) {
                if (!cn.isEmpty()) {
                    this.mSubjectPrimary = o;
                    this.mSubjectSecondary = cn;
                } else {
                    this.mSubjectPrimary = o;
                    this.mSubjectSecondary = ou;
                }
            } else if (!cn.isEmpty()) {
                this.mSubjectPrimary = cn;
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

        @Override
        public int compareTo(CertHolder o) {
            int primary = this.mSubjectPrimary.compareToIgnoreCase(o.mSubjectPrimary);
            if (primary != 0) {
                return primary;
            }
            return this.mSubjectSecondary.compareToIgnoreCase(o.mSubjectSecondary);
        }

        public boolean equals(Object o) {
            if (!(o instanceof CertHolder)) {
                return false;
            }
            CertHolder other = (CertHolder) o;
            return this.mAlias.equals(other.mAlias);
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

    public boolean isTrustAllCaCertModeInProgress() {
        return this.mTrustAllCaUserId != -10000;
    }

    public void showTrustAllCaDialog(List<CertHolder> unapprovedCertHolders) {
        CertHolder[] arr = (CertHolder[]) unapprovedCertHolders.toArray(new CertHolder[unapprovedCertHolders.size()]);
        new TrustedCredentialsDialogBuilder(getActivity(), this).setCertHolders(arr).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                TrustedCredentialsSettings.this.getActivity().getIntent().removeExtra("ARG_SHOW_NEW_FOR_USER");
                TrustedCredentialsSettings.this.mTrustAllCaUserId = -10000;
            }
        }).show();
    }

    public void showCertDialog(CertHolder certHolder) {
        new TrustedCredentialsDialogBuilder(getActivity(), this).setCertHolder(certHolder).show();
    }

    @Override
    public List<X509Certificate> getX509CertsFromCertHolder(CertHolder certHolder) {
        List<X509Certificate> certificates = null;
        try {
            KeyChain.KeyChainConnection keyChainConnection = this.mKeyChainConnectionByProfileId.get(certHolder.mProfileId);
            IKeyChainService service = keyChainConnection.getService();
            List<String> chain = service.getCaCertificateChainAliases(certHolder.mAlias, true);
            int n = chain.size();
            List<X509Certificate> certificates2 = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                try {
                    byte[] encodedCertificate = service.getEncodedCaCertificate(chain.get(i), true);
                    X509Certificate certificate = KeyChain.toCertificate(encodedCertificate);
                    certificates2.add(certificate);
                } catch (RemoteException e) {
                    ex = e;
                    certificates = certificates2;
                    Log.e("TrustedCredentialsSettings", "RemoteException while retrieving certificate chain for root " + certHolder.mAlias, ex);
                    return certificates;
                }
            }
            return certificates2;
        } catch (RemoteException e2) {
            ex = e2;
        }
    }

    @Override
    public void removeOrInstallCert(CertHolder certHolder) {
        new AliasOperation(this, certHolder, null).execute(new Void[0]);
    }

    @Override
    public boolean startConfirmCredentialIfNotConfirmed(int userId, IntConsumer onCredentialConfirmedListener) {
        if (this.mConfirmedCredentialUsers.contains(Integer.valueOf(userId))) {
            return false;
        }
        boolean result = startConfirmCredential(userId);
        if (result) {
            this.mConfirmingCredentialListener = onCredentialConfirmedListener;
        }
        return result;
    }

    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;

        AliasOperation(TrustedCredentialsSettings this$0, CertHolder certHolder, AliasOperation aliasOperation) {
            this(certHolder);
        }

        private AliasOperation(CertHolder certHolder) {
            this.mCertHolder = certHolder;
            TrustedCredentialsSettings.this.mAliasOperation = this;
        }

        @Override
        public Boolean doInBackground(Void... params) {
            try {
                KeyChain.KeyChainConnection keyChainConnection = (KeyChain.KeyChainConnection) TrustedCredentialsSettings.this.mKeyChainConnectionByProfileId.get(this.mCertHolder.mProfileId);
                IKeyChainService service = keyChainConnection.getService();
                if (this.mCertHolder.mDeleted) {
                    byte[] bytes = this.mCertHolder.mX509Cert.getEncoded();
                    service.installCaCertificate(bytes);
                    return true;
                }
                return Boolean.valueOf(service.deleteCaCertificate(this.mCertHolder.mAlias));
            } catch (RemoteException | IllegalStateException | SecurityException | CertificateEncodingException e) {
                Log.w("TrustedCredentialsSettings", "Error while toggling alias " + this.mCertHolder.mAlias, e);
                return false;
            }
        }

        @Override
        public void onPostExecute(Boolean ok) {
            if (ok.booleanValue()) {
                if (this.mCertHolder.mTab.mSwitch) {
                    this.mCertHolder.mDeleted = !this.mCertHolder.mDeleted;
                } else {
                    this.mCertHolder.mAdapter.remove(this.mCertHolder);
                }
                this.mCertHolder.mAdapter.notifyDataSetChanged();
            } else {
                this.mCertHolder.mAdapter.load();
            }
            TrustedCredentialsSettings.this.mAliasOperation = null;
        }
    }
}
