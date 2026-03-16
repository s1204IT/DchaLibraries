package com.android.settings.applications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.internal.app.IMediaContainerService;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.UserSpinnerAdapter;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationsState;
import com.android.settings.deviceinfo.StorageMeasurement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ManageApplications extends Fragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, AdapterView.OnItemSelectedListener, AppClickListener {
    private boolean mActivityResumed;
    private ApplicationsState mApplicationsState;
    private CharSequence mComputingSizeStr;
    private volatile IMediaContainerService mContainerService;
    private ViewGroup mContentContainer;
    private Context mContext;
    private String mCurrentPkgName;
    private LayoutInflater mInflater;
    CharSequence mInvalidSizeStr;
    private int mNumTabs;
    private Menu mOptionsMenu;
    private ViewGroup mPinnedHeader;
    private UserSpinnerAdapter mProfileSpinnerAdapter;
    AlertDialog mResetDialog;
    private View mRootView;
    private Spinner mSpinner;
    private ViewPager mViewPager;
    private int mSortOrder = 4;
    private final ArrayList<TabInfo> mTabs = new ArrayList<>();
    TabInfo mCurTab = null;
    private boolean mShowBackground = false;
    private int mDefaultListType = -1;
    private final ServiceConnection mContainerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ManageApplications.this.mContainerService = IMediaContainerService.Stub.asInterface(service);
            for (int i = 0; i < ManageApplications.this.mTabs.size(); i++) {
                ((TabInfo) ManageApplications.this.mTabs.get(i)).setContainerService(ManageApplications.this.mContainerService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ManageApplications.this.mContainerService = null;
        }
    };

    public static class TabInfo implements AdapterView.OnItemClickListener {
        public ApplicationsAdapter mApplications;
        public final ApplicationsState mApplicationsState;
        public final AppClickListener mClickListener;
        public final CharSequence mComputingSizeStr;
        private IMediaContainerService mContainerService;
        public final int mFilter;
        public LayoutInflater mInflater;
        public final CharSequence mInvalidSizeStr;
        public final CharSequence mLabel;
        private View mListContainer;
        public final int mListType;
        private ListView mListView;
        private View mLoadingContainer;
        public final ManageApplications mOwner;
        public View mRootView;
        private RunningProcessesView mRunningProcessesView;
        private final Bundle mSavedInstanceState;
        private long mFreeStorage = 0;
        private long mAppStorage = 0;
        private long mTotalStorage = 0;
        final Runnable mRunningProcessesAvail = new Runnable() {
            @Override
            public void run() {
                TabInfo.this.handleRunningProcessesAvail();
            }
        };

        public TabInfo(ManageApplications owner, ApplicationsState apps, CharSequence label, int listType, AppClickListener clickListener, Bundle savedInstanceState) {
            this.mOwner = owner;
            this.mApplicationsState = apps;
            this.mLabel = label;
            this.mListType = listType;
            switch (listType) {
                case 0:
                    this.mFilter = 1;
                    break;
                case 1:
                case 3:
                default:
                    this.mFilter = 0;
                    break;
                case 2:
                    this.mFilter = 2;
                    break;
                case 4:
                    this.mFilter = 3;
                    break;
            }
            this.mClickListener = clickListener;
            this.mInvalidSizeStr = owner.getActivity().getText(R.string.invalid_size_value);
            this.mComputingSizeStr = owner.getActivity().getText(R.string.computing_size);
            this.mSavedInstanceState = savedInstanceState;
        }

        public void setContainerService(IMediaContainerService containerService) {
            this.mContainerService = containerService;
            updateStorageUsage();
        }

        public View build(LayoutInflater inflater, ViewGroup contentParent, View contentChild) {
            if (this.mRootView != null) {
                return this.mRootView;
            }
            this.mInflater = inflater;
            this.mRootView = inflater.inflate(this.mListType == 1 ? R.layout.manage_applications_running : R.layout.manage_applications_apps, (ViewGroup) null);
            this.mLoadingContainer = this.mRootView.findViewById(R.id.loading_container);
            this.mLoadingContainer.setVisibility(0);
            this.mListContainer = this.mRootView.findViewById(R.id.list_container);
            if (this.mListContainer != null) {
                View emptyView = this.mListContainer.findViewById(android.R.id.empty);
                ListView lv = (ListView) this.mListContainer.findViewById(android.R.id.list);
                if (emptyView != null) {
                    lv.setEmptyView(emptyView);
                }
                lv.setOnItemClickListener(this);
                lv.setSaveEnabled(true);
                lv.setItemsCanFocus(true);
                lv.setTextFilterEnabled(true);
                this.mListView = lv;
                this.mApplications = new ApplicationsAdapter(this.mApplicationsState, this, this.mFilter);
                this.mListView.setAdapter((ListAdapter) this.mApplications);
                this.mListView.setRecyclerListener(this.mApplications);
                Utils.prepareCustomPreferencesList(contentParent, contentChild, this.mListView, false);
                if (this.mFilter == 2) {
                }
                applyCurrentStorage();
            }
            this.mRunningProcessesView = (RunningProcessesView) this.mRootView.findViewById(R.id.running_processes);
            if (this.mRunningProcessesView != null) {
                this.mRunningProcessesView.doCreate(this.mSavedInstanceState);
            }
            return this.mRootView;
        }

        public void detachView() {
            ViewGroup group;
            if (this.mRootView != null && (group = (ViewGroup) this.mRootView.getParent()) != null) {
                group.removeView(this.mRootView);
            }
        }

        public void resume(int sortOrder) {
            if (this.mApplications != null) {
                this.mApplications.resume(sortOrder);
            }
            if (this.mRunningProcessesView != null) {
                boolean haveData = this.mRunningProcessesView.doResume(this.mOwner, this.mRunningProcessesAvail);
                if (haveData) {
                    this.mRunningProcessesView.setVisibility(0);
                    this.mLoadingContainer.setVisibility(4);
                } else {
                    this.mLoadingContainer.setVisibility(0);
                }
            }
        }

        public void pause() {
            if (this.mApplications != null) {
                this.mApplications.pause();
            }
            if (this.mRunningProcessesView != null) {
                this.mRunningProcessesView.doPause();
            }
        }

        public void release() {
            if (this.mApplications != null) {
                this.mApplications.release();
            }
        }

        void updateStorageUsage() {
            if (this.mOwner.getActivity() != null && this.mApplications != null) {
                this.mFreeStorage = 0L;
                this.mAppStorage = 0L;
                this.mTotalStorage = 0L;
                if (this.mFilter == 2) {
                    if (this.mContainerService != null) {
                        try {
                            long[] stats = this.mContainerService.getFileSystemStats(Environment.getExternalStorageDirectory().getPath());
                            this.mTotalStorage = stats[0];
                            this.mFreeStorage = stats[1];
                        } catch (RemoteException e) {
                            Log.w("ManageApplications", "Problem in container service", e);
                        }
                    }
                    if (this.mApplications != null) {
                        int N = this.mApplications.getCount();
                        for (int i = 0; i < N; i++) {
                            ApplicationsState.AppEntry ae = this.mApplications.getAppEntry(i);
                            this.mAppStorage += ae.externalCodeSize + ae.externalDataSize + ae.externalCacheSize;
                        }
                    }
                } else {
                    if (this.mContainerService != null) {
                        try {
                            long[] stats2 = this.mContainerService.getFileSystemStats(Environment.getDataDirectory().getPath());
                            this.mTotalStorage = stats2[0];
                            this.mFreeStorage = stats2[1];
                        } catch (RemoteException e2) {
                            Log.w("ManageApplications", "Problem in container service", e2);
                        }
                    }
                    boolean emulatedStorage = Environment.isExternalStorageEmulated();
                    if (this.mApplications != null) {
                        int N2 = this.mApplications.getCount();
                        for (int i2 = 0; i2 < N2; i2++) {
                            ApplicationsState.AppEntry ae2 = this.mApplications.getAppEntry(i2);
                            this.mAppStorage += ae2.codeSize + ae2.dataSize;
                            if (emulatedStorage) {
                                this.mAppStorage += ae2.externalCodeSize + ae2.externalDataSize;
                            }
                        }
                    }
                    this.mFreeStorage += this.mApplicationsState.sumCacheSizes();
                }
                applyCurrentStorage();
            }
        }

        void applyCurrentStorage() {
            if (this.mRootView == null) {
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            this.mClickListener.onItemClick(this, parent, view, position, id);
        }

        void handleRunningProcessesAvail() {
            this.mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(this.mOwner.getActivity(), android.R.anim.fade_out));
            this.mRunningProcessesView.startAnimation(AnimationUtils.loadAnimation(this.mOwner.getActivity(), android.R.anim.fade_in));
            this.mRunningProcessesView.setVisibility(0);
            this.mLoadingContainer.setVisibility(8);
        }
    }

    class MyPagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {
        int mCurPos = 0;

        MyPagerAdapter() {
        }

        @Override
        public int getCount() {
            return ManageApplications.this.mNumTabs;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TabInfo tab = (TabInfo) ManageApplications.this.mTabs.get(position);
            View root = tab.build(ManageApplications.this.mInflater, ManageApplications.this.mContentContainer, ManageApplications.this.mRootView);
            container.addView(root);
            root.setTag(R.id.name, tab);
            return root;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ((TabInfo) ManageApplications.this.mTabs.get(position)).mLabel;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            this.mCurPos = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == 0) {
                ManageApplications.this.updateCurrentTab(this.mCurPos);
            }
        }
    }

    static class ApplicationsAdapter extends BaseAdapter implements AbsListView.RecyclerListener, Filterable, ApplicationsState.Callbacks {
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private final Context mContext;
        CharSequence mCurFilterPrefix;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private final int mFilterMode;
        private boolean mResumed;
        private final ApplicationsState.Session mSession;
        private final ApplicationsState mState;
        private final TabInfo mTab;
        private boolean mWaitingForData;
        private final ArrayList<View> mActive = new ArrayList<>();
        private int mLastSortMode = -1;
        private int mWhichSize = 0;
        private Filter mFilter = new Filter() {
            @Override
            protected Filter.FilterResults performFiltering(CharSequence constraint) {
                ArrayList<ApplicationsState.AppEntry> entries = ApplicationsAdapter.this.applyPrefixFilter(constraint, ApplicationsAdapter.this.mBaseEntries);
                Filter.FilterResults fr = new Filter.FilterResults();
                fr.values = entries;
                fr.count = entries.size();
                return fr;
            }

            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                ApplicationsAdapter.this.mCurFilterPrefix = constraint;
                ApplicationsAdapter.this.mEntries = (ArrayList) results.values;
                ApplicationsAdapter.this.notifyDataSetChanged();
                ApplicationsAdapter.this.mTab.updateStorageUsage();
            }
        };

        public ApplicationsAdapter(ApplicationsState state, TabInfo tab, int filterMode) {
            this.mState = state;
            this.mSession = state.newSession(this);
            this.mTab = tab;
            this.mContext = tab.mOwner.getActivity();
            this.mFilterMode = filterMode;
        }

        public void resume(int sort) {
            if (!this.mResumed) {
                this.mResumed = true;
                this.mSession.resume();
                this.mLastSortMode = sort;
                rebuild(true);
                return;
            }
            rebuild(sort);
        }

        public void pause() {
            if (this.mResumed) {
                this.mResumed = false;
                this.mSession.pause();
            }
        }

        public void release() {
            this.mSession.release();
        }

        public void rebuild(int sort) {
            if (sort != this.mLastSortMode) {
                this.mLastSortMode = sort;
                rebuild(true);
            }
        }

        public void rebuild(boolean eraseold) {
            ApplicationsState.AppFilter filterObj;
            Comparator<ApplicationsState.AppEntry> comparatorObj;
            boolean emulated = Environment.isExternalStorageEmulated();
            if (emulated) {
                this.mWhichSize = 0;
            } else {
                this.mWhichSize = 1;
            }
            switch (this.mFilterMode) {
                case 1:
                    filterObj = ApplicationsState.THIRD_PARTY_FILTER;
                    break;
                case 2:
                    filterObj = ApplicationsState.ON_SD_CARD_FILTER;
                    if (!emulated) {
                        this.mWhichSize = 2;
                    }
                    break;
                case 3:
                    filterObj = ApplicationsState.DISABLED_FILTER;
                    break;
                default:
                    filterObj = ApplicationsState.ALL_ENABLED_FILTER;
                    break;
            }
            switch (this.mLastSortMode) {
                case 5:
                    switch (this.mWhichSize) {
                        case 1:
                            comparatorObj = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                            break;
                        case 2:
                            comparatorObj = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                            break;
                        default:
                            comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                            break;
                    }
                    break;
                default:
                    comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                    break;
            }
            ArrayList<ApplicationsState.AppEntry> entries = this.mSession.rebuild(filterObj, comparatorObj);
            if (entries != null || eraseold) {
                this.mBaseEntries = entries;
                if (this.mBaseEntries != null) {
                    this.mEntries = applyPrefixFilter(this.mCurFilterPrefix, this.mBaseEntries);
                } else {
                    this.mEntries = null;
                }
                notifyDataSetChanged();
                this.mTab.updateStorageUsage();
                if (entries != null) {
                    this.mTab.mListContainer.setVisibility(0);
                    this.mTab.mLoadingContainer.setVisibility(8);
                } else {
                    this.mWaitingForData = true;
                    this.mTab.mListContainer.setVisibility(4);
                    this.mTab.mLoadingContainer.setVisibility(0);
                }
            }
        }

        ArrayList<ApplicationsState.AppEntry> applyPrefixFilter(CharSequence prefix, ArrayList<ApplicationsState.AppEntry> origEntries) {
            if (prefix == null || prefix.length() == 0) {
                return origEntries;
            }
            String prefixStr = ApplicationsState.normalize(prefix.toString());
            String spacePrefixStr = " " + prefixStr;
            ArrayList<ApplicationsState.AppEntry> newEntries = new ArrayList<>();
            for (int i = 0; i < origEntries.size(); i++) {
                ApplicationsState.AppEntry entry = origEntries.get(i);
                String nlabel = entry.getNormalizedLabel();
                if (nlabel.startsWith(prefixStr) || nlabel.indexOf(spacePrefixStr) != -1) {
                    newEntries.add(entry);
                }
            }
            return newEntries;
        }

        @Override
        public void onRunningStateChanged(boolean running) {
            this.mTab.mOwner.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
            if (this.mTab.mLoadingContainer.getVisibility() == 0) {
                this.mTab.mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(this.mContext, android.R.anim.fade_out));
                this.mTab.mListContainer.startAnimation(AnimationUtils.loadAnimation(this.mContext, android.R.anim.fade_in));
            }
            this.mTab.mListContainer.setVisibility(0);
            this.mTab.mLoadingContainer.setVisibility(8);
            this.mWaitingForData = false;
            this.mBaseEntries = apps;
            this.mEntries = applyPrefixFilter(this.mCurFilterPrefix, this.mBaseEntries);
            notifyDataSetChanged();
            this.mTab.updateStorageUsage();
        }

        @Override
        public void onPackageListChanged() {
            rebuild(false);
        }

        @Override
        public void onPackageIconChanged() {
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i = 0; i < this.mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder) this.mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        holder.updateSizeText(this.mTab.mInvalidSizeStr, this.mWhichSize);
                    }
                    if (holder.entry.info.packageName.equals(this.mTab.mOwner.mCurrentPkgName) && this.mLastSortMode == 5) {
                        rebuild(false);
                    }
                    this.mTab.updateStorageUsage();
                    return;
                }
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (this.mLastSortMode == 5) {
                rebuild(false);
            }
            this.mTab.updateStorageUsage();
        }

        @Override
        public int getCount() {
            if (this.mEntries != null) {
                return this.mEntries.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return this.mEntries.get(position);
        }

        public ApplicationsState.AppEntry getAppEntry(int position) {
            return this.mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return this.mEntries.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder = AppViewHolder.createOrRecycle(this.mTab.mInflater, convertView);
            View convertView2 = holder.rootView;
            ApplicationsState.AppEntry entry = this.mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                }
                this.mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                holder.updateSizeText(this.mTab.mInvalidSizeStr, this.mWhichSize);
                if ((entry.info.flags & 8388608) == 0) {
                    holder.disabled.setVisibility(0);
                    holder.disabled.setText(R.string.not_installed);
                } else if (!entry.info.enabled) {
                    holder.disabled.setVisibility(0);
                    holder.disabled.setText(R.string.disabled);
                } else {
                    holder.disabled.setVisibility(8);
                }
                if (this.mFilterMode == 2) {
                    holder.checkBox.setVisibility(0);
                    holder.checkBox.setChecked((entry.info.flags & 262144) != 0);
                } else {
                    holder.checkBox.setVisibility(8);
                }
            }
            this.mActive.remove(convertView2);
            this.mActive.add(convertView2);
            return convertView2;
        }

        @Override
        public Filter getFilter() {
            return this.mFilter;
        }

        @Override
        public void onMovedToScrapHeap(View view) {
            this.mActive.remove(view);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.mContext = getActivity();
        this.mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
        int defaultListType = 0;
        String className = getArguments() != null ? getArguments().getString("classname") : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(Settings.RunningServicesActivity.class.getName()) || className.endsWith(".RunningServices")) {
            defaultListType = 1;
        } else if (className.equals(Settings.StorageUseActivity.class.getName()) || "android.intent.action.MANAGE_PACKAGE_STORAGE".equals(action) || className.endsWith(".StorageUse")) {
            this.mSortOrder = 5;
            defaultListType = 3;
        } else if ("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS".equals(action)) {
            defaultListType = 3;
        }
        if (savedInstanceState != null) {
            this.mSortOrder = savedInstanceState.getInt("sortOrder", this.mSortOrder);
            int tmp = savedInstanceState.getInt("defaultListType", -1);
            if (tmp != -1) {
                defaultListType = tmp;
            }
            this.mShowBackground = savedInstanceState.getBoolean("showBackground", false);
        }
        this.mDefaultListType = defaultListType;
        Intent containerIntent = new Intent().setComponent(StorageMeasurement.DEFAULT_CONTAINER_COMPONENT);
        getActivity().bindService(containerIntent, this.mContainerConnection, 1);
        this.mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        this.mComputingSizeStr = getActivity().getText(R.string.computing_size);
        TabInfo tab = new TabInfo(this, this.mApplicationsState, getActivity().getString(R.string.filter_apps_third_party), 0, this, savedInstanceState);
        this.mTabs.add(tab);
        if (!Environment.isExternalStorageEmulated()) {
            TabInfo tab2 = new TabInfo(this, this.mApplicationsState, getActivity().getString(R.string.filter_apps_onsdcard), 2, this, savedInstanceState);
            this.mTabs.add(tab2);
        }
        TabInfo tab3 = new TabInfo(this, this.mApplicationsState, getActivity().getString(R.string.filter_apps_running), 1, this, savedInstanceState);
        this.mTabs.add(tab3);
        TabInfo tab4 = new TabInfo(this, this.mApplicationsState, getActivity().getString(R.string.filter_apps_all), 3, this, savedInstanceState);
        this.mTabs.add(tab4);
        TabInfo tab5 = new TabInfo(this, this.mApplicationsState, getActivity().getString(R.string.filter_apps_disabled), 4, this, savedInstanceState);
        this.mTabs.add(tab5);
        this.mNumTabs = this.mTabs.size();
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        this.mProfileSpinnerAdapter = Utils.createUserSpinnerAdapter(um, this.mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mInflater = inflater;
        View rootView = this.mInflater.inflate(R.layout.manage_applications_content, container, false);
        this.mContentContainer = container;
        this.mRootView = rootView;
        this.mPinnedHeader = (ViewGroup) this.mRootView.findViewById(R.id.pinned_header);
        if (this.mProfileSpinnerAdapter != null) {
            this.mSpinner = (Spinner) inflater.inflate(R.layout.spinner_view, (ViewGroup) null);
            this.mSpinner.setAdapter((SpinnerAdapter) this.mProfileSpinnerAdapter);
            this.mSpinner.setOnItemSelectedListener(this);
            this.mPinnedHeader.addView(this.mSpinner);
            this.mPinnedHeader.setVisibility(0);
        }
        this.mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter();
        this.mViewPager.setAdapter(adapter);
        this.mViewPager.setOnPageChangeListener(adapter);
        PagerTabStrip tabs = (PagerTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(R.color.theme_accent);
        if (container instanceof PreferenceFrameLayout) {
            rootView.getLayoutParams().removeBorders = true;
        }
        if (savedInstanceState != null && savedInstanceState.getBoolean("resetDialog")) {
            buildResetDialog();
        }
        if (savedInstanceState == null) {
            int extraCurrentListType = getActivity().getIntent().getIntExtra("currentListType", -1);
            int currentListType = extraCurrentListType != -1 ? extraCurrentListType : this.mDefaultListType;
            int i = 0;
            while (true) {
                if (i >= this.mNumTabs) {
                    break;
                }
                TabInfo tab = this.mTabs.get(i);
                if (tab.mListType != currentListType) {
                    i++;
                } else {
                    this.mViewPager.setCurrentItem(i);
                    break;
                }
            }
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mActivityResumed = true;
        updateCurrentTab(this.mViewPager.getCurrentItem());
        updateNumTabs();
        updateOptionsMenu();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sortOrder", this.mSortOrder);
        if (this.mDefaultListType != -1) {
            outState.putInt("defaultListType", this.mDefaultListType);
        }
        outState.putBoolean("showBackground", this.mShowBackground);
        if (this.mResetDialog != null) {
            outState.putBoolean("resetDialog", true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mActivityResumed = false;
        for (int i = 0; i < this.mTabs.size(); i++) {
            this.mTabs.get(i).pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mResetDialog != null) {
            this.mResetDialog.dismiss();
            this.mResetDialog = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (int i = 0; i < this.mTabs.size(); i++) {
            this.mTabs.get(i).detachView();
            this.mTabs.get(i).release();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && this.mCurrentPkgName != null) {
            this.mApplicationsState.requestSize(this.mCurrentPkgName);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        UserHandle selectedUser = this.mProfileSpinnerAdapter.getUserHandle(position);
        if (selectedUser.getIdentifier() != UserHandle.myUserId()) {
            Intent intent = new Intent("android.settings.APPLICATION_SETTINGS");
            intent.addFlags(268435456);
            intent.addFlags(32768);
            int currentTab = this.mViewPager.getCurrentItem();
            intent.putExtra("currentListType", this.mTabs.get(currentTab).mListType);
            this.mContext.startActivityAsUser(intent, selectedUser);
            this.mSpinner.setSelection(0);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void updateNumTabs() {
        int newNum = this.mApplicationsState.haveDisabledApps() ? this.mTabs.size() : this.mTabs.size() - 1;
        if (newNum != this.mNumTabs) {
            this.mNumTabs = newNum;
            if (this.mViewPager != null) {
                this.mViewPager.getAdapter().notifyDataSetChanged();
            }
        }
    }

    TabInfo tabForType(int type) {
        for (int i = 0; i < this.mTabs.size(); i++) {
            TabInfo tab = this.mTabs.get(i);
            if (tab.mListType == type) {
                return tab;
            }
        }
        return null;
    }

    private void startApplicationDetailsActivity() {
        Bundle args = new Bundle();
        args.putString("package", this.mCurrentPkgName);
        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.startPreferencePanel(InstalledAppDetails.class.getName(), args, R.string.application_info_label, null, this, 1);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mOptionsMenu = menu;
        menu.add(0, 4, 1, R.string.sort_order_alpha).setShowAsAction(0);
        menu.add(0, 5, 2, R.string.sort_order_size).setShowAsAction(0);
        menu.add(0, 6, 3, R.string.show_running_services).setShowAsAction(1);
        menu.add(0, 7, 3, R.string.show_background_processes).setShowAsAction(1);
        menu.add(0, 8, 4, R.string.reset_app_preferences).setShowAsAction(0);
        updateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    @Override
    public void onDestroyOptionsMenu() {
        this.mOptionsMenu = null;
    }

    @Override
    public void onDestroy() {
        getActivity().unbindService(this.mContainerConnection);
        super.onDestroy();
    }

    void updateOptionsMenu() {
        if (this.mOptionsMenu != null) {
            if (this.mCurTab != null && this.mCurTab.mListType == 1) {
                TabInfo tab = tabForType(1);
                boolean showingBackground = (tab == null || tab.mRunningProcessesView == null) ? false : tab.mRunningProcessesView.mAdapter.getShowBackground();
                this.mOptionsMenu.findItem(4).setVisible(false);
                this.mOptionsMenu.findItem(5).setVisible(false);
                this.mOptionsMenu.findItem(6).setVisible(showingBackground);
                this.mOptionsMenu.findItem(7).setVisible(showingBackground ? false : true);
                this.mOptionsMenu.findItem(8).setVisible(false);
                this.mShowBackground = showingBackground;
                return;
            }
            this.mOptionsMenu.findItem(4).setVisible(this.mSortOrder != 4);
            this.mOptionsMenu.findItem(5).setVisible(this.mSortOrder != 5);
            this.mOptionsMenu.findItem(6).setVisible(false);
            this.mOptionsMenu.findItem(7).setVisible(false);
            this.mOptionsMenu.findItem(8).setVisible(true);
        }
    }

    void buildResetDialog() {
        if (this.mResetDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.reset_app_preferences_title);
            builder.setMessage(R.string.reset_app_preferences_desc);
            builder.setPositiveButton(R.string.reset_app_preferences_button, this);
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            this.mResetDialog = builder.show();
            this.mResetDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.mResetDialog == dialog) {
            this.mResetDialog = null;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.mResetDialog == dialog) {
            final PackageManager pm = getActivity().getPackageManager();
            final IPackageManager mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            final INotificationManager nm = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            final NetworkPolicyManager npm = NetworkPolicyManager.from(getActivity());
            final AppOpsManager aom = (AppOpsManager) getActivity().getSystemService("appops");
            final Handler handler = new Handler(getActivity().getMainLooper());
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    List<ApplicationInfo> apps = pm.getInstalledApplications(512);
                    for (int i = 0; i < apps.size(); i++) {
                        ApplicationInfo app = apps.get(i);
                        try {
                            nm.setNotificationsEnabledForPackage(app.packageName, app.uid, true);
                        } catch (RemoteException e) {
                        }
                        if (!app.enabled && pm.getApplicationEnabledSetting(app.packageName) == 3) {
                            pm.setApplicationEnabledSetting(app.packageName, 0, 1);
                        }
                    }
                    try {
                        mIPm.resetPreferredActivities(UserHandle.myUserId());
                    } catch (RemoteException e2) {
                    }
                    aom.resetAllModes();
                    int[] restrictedUids = npm.getUidsWithPolicy(1);
                    int currentUserId = ActivityManager.getCurrentUser();
                    for (int uid : restrictedUids) {
                        if (UserHandle.getUserId(uid) == currentUserId) {
                            npm.setUidPolicy(uid, 0);
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (ManageApplications.this.getActivity() != null && ManageApplications.this.mActivityResumed) {
                                for (int i2 = 0; i2 < ManageApplications.this.mTabs.size(); i2++) {
                                    TabInfo tab = (TabInfo) ManageApplications.this.mTabs.get(i2);
                                    if (tab.mApplications != null) {
                                        tab.mApplications.pause();
                                    }
                                }
                                if (ManageApplications.this.mCurTab != null) {
                                    ManageApplications.this.mCurTab.resume(ManageApplications.this.mSortOrder);
                                }
                            }
                        }
                    });
                    return null;
                }
            }.execute(new Void[0]);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if (menuId == 4 || menuId == 5) {
            this.mSortOrder = menuId;
            if (this.mCurTab != null && this.mCurTab.mApplications != null) {
                this.mCurTab.mApplications.rebuild(this.mSortOrder);
            }
        } else if (menuId == 6) {
            this.mShowBackground = false;
            if (this.mCurTab != null && this.mCurTab.mRunningProcessesView != null) {
                this.mCurTab.mRunningProcessesView.mAdapter.setShowBackground(false);
            }
        } else if (menuId == 7) {
            this.mShowBackground = true;
            if (this.mCurTab != null && this.mCurTab.mRunningProcessesView != null) {
                this.mCurTab.mRunningProcessesView.mAdapter.setShowBackground(true);
            }
        } else {
            if (menuId != 8) {
                return false;
            }
            buildResetDialog();
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onItemClick(TabInfo tab, AdapterView<?> parent, View view, int position, long id) {
        if (tab.mApplications != null && tab.mApplications.getCount() > position) {
            ApplicationsState.AppEntry entry = tab.mApplications.getAppEntry(position);
            this.mCurrentPkgName = entry.info.packageName;
            startApplicationDetailsActivity();
        }
    }

    public void updateCurrentTab(int position) {
        TabInfo tab = this.mTabs.get(position);
        this.mCurTab = tab;
        if (this.mActivityResumed) {
            this.mCurTab.build(this.mInflater, this.mContentContainer, this.mRootView);
            this.mCurTab.resume(this.mSortOrder);
        } else {
            this.mCurTab.pause();
        }
        for (int i = 0; i < this.mTabs.size(); i++) {
            TabInfo t = this.mTabs.get(i);
            if (t != this.mCurTab) {
                t.pause();
            }
        }
        this.mCurTab.updateStorageUsage();
        updateOptionsMenu();
        Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }
}
