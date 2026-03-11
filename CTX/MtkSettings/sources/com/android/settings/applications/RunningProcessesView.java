package com.android.settings.applications;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.BidiFormatter;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.util.MemInfoReader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.RunningState;
import com.android.settings.core.SubSettingLauncher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class RunningProcessesView extends FrameLayout implements AbsListView.RecyclerListener, AdapterView.OnItemClickListener, RunningState.OnRefreshUiListener {
    long SECONDARY_SERVER_MEM;
    final HashMap<View, ActiveItem> mActiveItems;
    ServiceListAdapter mAdapter;
    ActivityManager mAm;
    TextView mAppsProcessPrefix;
    TextView mAppsProcessText;
    TextView mBackgroundProcessPrefix;
    TextView mBackgroundProcessText;
    StringBuilder mBuilder;
    ProgressBar mColorBar;
    long mCurHighRam;
    long mCurLowRam;
    long mCurMedRam;
    RunningState.BaseItem mCurSelected;
    boolean mCurShowCached;
    long mCurTotalRam;
    Runnable mDataAvail;
    TextView mForegroundProcessPrefix;
    TextView mForegroundProcessText;
    View mHeader;
    ListView mListView;
    MemInfoReader mMemInfoReader;
    final int mMyUserId;
    SettingsPreferenceFragment mOwner;
    RunningState mState;

    public static class ActiveItem {
        long mFirstRunTime;
        ViewHolder mHolder;
        RunningState.BaseItem mItem;
        View mRootView;
        boolean mSetBackground;

        void updateTime(Context context, StringBuilder sb) {
            TextView textView;
            if (this.mItem instanceof RunningState.ServiceItem) {
                textView = this.mHolder.size;
            } else {
                String str = this.mItem.mSizeStr != null ? this.mItem.mSizeStr : "";
                if (!str.equals(this.mItem.mCurSizeStr)) {
                    this.mItem.mCurSizeStr = str;
                    this.mHolder.size.setText(str);
                }
                if (this.mItem.mBackground) {
                    if (!this.mSetBackground) {
                        this.mSetBackground = true;
                        this.mHolder.uptime.setText("");
                    }
                } else if (this.mItem instanceof RunningState.MergedItem) {
                    textView = this.mHolder.uptime;
                }
                textView = null;
            }
            if (textView != null) {
                boolean z = false;
                this.mSetBackground = false;
                if (this.mFirstRunTime >= 0) {
                    textView.setText(DateUtils.formatElapsedTime(sb, (SystemClock.elapsedRealtime() - this.mFirstRunTime) / 1000));
                    return;
                }
                if ((this.mItem instanceof RunningState.MergedItem) && ((RunningState.MergedItem) this.mItem).mServices.size() > 0) {
                    z = true;
                }
                if (z) {
                    textView.setText(context.getResources().getText(R.string.service_restarting));
                } else {
                    textView.setText("");
                }
            }
        }
    }

    public static class ViewHolder {
        public TextView description;
        public ImageView icon;
        public TextView name;
        public View rootView;
        public TextView size;
        public TextView uptime;

        public ViewHolder(View view) {
            this.rootView = view;
            this.icon = (ImageView) view.findViewById(R.id.icon);
            this.name = (TextView) view.findViewById(R.id.name);
            this.description = (TextView) view.findViewById(R.id.description);
            this.size = (TextView) view.findViewById(R.id.size);
            this.uptime = (TextView) view.findViewById(R.id.uptime);
            view.setTag(this);
        }

        public ActiveItem bind(RunningState runningState, RunningState.BaseItem baseItem, StringBuilder sb) {
            ActiveItem activeItem;
            synchronized (runningState.mLock) {
                PackageManager packageManager = this.rootView.getContext().getPackageManager();
                if (baseItem.mPackageInfo == null && (baseItem instanceof RunningState.MergedItem) && ((RunningState.MergedItem) baseItem).mProcess != null) {
                    ((RunningState.MergedItem) baseItem).mProcess.ensureLabel(packageManager);
                    baseItem.mPackageInfo = ((RunningState.MergedItem) baseItem).mProcess.mPackageInfo;
                    baseItem.mDisplayLabel = ((RunningState.MergedItem) baseItem).mProcess.mDisplayLabel;
                }
                this.name.setText(baseItem.mDisplayLabel);
                activeItem = new ActiveItem();
                activeItem.mRootView = this.rootView;
                activeItem.mItem = baseItem;
                activeItem.mHolder = this;
                activeItem.mFirstRunTime = baseItem.mActiveSince;
                if (baseItem.mBackground) {
                    this.description.setText(this.rootView.getContext().getText(R.string.cached));
                } else {
                    this.description.setText(baseItem.mDescription);
                }
                baseItem.mCurSizeStr = null;
                this.icon.setImageDrawable(baseItem.loadIcon(this.rootView.getContext(), runningState));
                this.icon.setVisibility(0);
                activeItem.updateTime(this.rootView.getContext(), sb);
            }
            return activeItem;
        }
    }

    class ServiceListAdapter extends BaseAdapter {
        final LayoutInflater mInflater;
        final ArrayList<RunningState.MergedItem> mItems = new ArrayList<>();
        ArrayList<RunningState.MergedItem> mOrigItems;
        boolean mShowBackground;
        final RunningState mState;

        ServiceListAdapter(RunningState runningState) {
            this.mState = runningState;
            this.mInflater = (LayoutInflater) RunningProcessesView.this.getContext().getSystemService("layout_inflater");
            refreshItems();
        }

        void setShowBackground(boolean z) {
            if (this.mShowBackground != z) {
                this.mShowBackground = z;
                this.mState.setWatchingBackgroundItems(z);
                refreshItems();
                RunningProcessesView.this.refreshUi(true);
            }
        }

        boolean getShowBackground() {
            return this.mShowBackground;
        }

        void refreshItems() {
            ArrayList<RunningState.MergedItem> currentBackgroundItems = this.mShowBackground ? this.mState.getCurrentBackgroundItems() : this.mState.getCurrentMergedItems();
            if (this.mOrigItems != currentBackgroundItems) {
                this.mOrigItems = currentBackgroundItems;
                if (currentBackgroundItems == null) {
                    this.mItems.clear();
                    return;
                }
                this.mItems.clear();
                this.mItems.addAll(currentBackgroundItems);
                if (this.mShowBackground) {
                    Collections.sort(this.mItems, this.mState.mBackgroundComparator);
                }
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getCount() {
            return this.mItems.size();
        }

        @Override
        public boolean isEmpty() {
            return this.mState.hasData() && this.mItems.size() == 0;
        }

        @Override
        public Object getItem(int i) {
            return this.mItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return this.mItems.get(i).hashCode();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return !this.mItems.get(i).mIsProcess;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = newView(viewGroup);
            }
            bindView(view, i);
            return view;
        }

        public View newView(ViewGroup viewGroup) {
            View viewInflate = this.mInflater.inflate(R.layout.running_processes_item, viewGroup, false);
            new ViewHolder(viewInflate);
            return viewInflate;
        }

        public void bindView(View view, int i) {
            synchronized (this.mState.mLock) {
                if (i >= this.mItems.size()) {
                    return;
                }
                RunningProcessesView.this.mActiveItems.put(view, ((ViewHolder) view.getTag()).bind(this.mState, this.mItems.get(i), RunningProcessesView.this.mBuilder));
            }
        }
    }

    void refreshUi(boolean z) {
        long freeSize;
        long j;
        if (z) {
            ServiceListAdapter serviceListAdapter = this.mAdapter;
            serviceListAdapter.refreshItems();
            serviceListAdapter.notifyDataSetChanged();
        }
        if (this.mDataAvail != null) {
            this.mDataAvail.run();
            this.mDataAvail = null;
        }
        this.mMemInfoReader.readMemInfo();
        synchronized (this.mState.mLock) {
            if (this.mCurShowCached != this.mAdapter.mShowBackground) {
                this.mCurShowCached = this.mAdapter.mShowBackground;
                if (this.mCurShowCached) {
                    this.mForegroundProcessPrefix.setText(getResources().getText(R.string.running_processes_header_used_prefix));
                    this.mAppsProcessPrefix.setText(getResources().getText(R.string.running_processes_header_cached_prefix));
                } else {
                    this.mForegroundProcessPrefix.setText(getResources().getText(R.string.running_processes_header_system_prefix));
                    this.mAppsProcessPrefix.setText(getResources().getText(R.string.running_processes_header_apps_prefix));
                }
            }
            long totalSize = this.mMemInfoReader.getTotalSize();
            if (this.mCurShowCached) {
                freeSize = this.mMemInfoReader.getFreeSize() + this.mMemInfoReader.getCachedSize();
                j = this.mState.mBackgroundProcessMemory;
            } else {
                freeSize = this.mMemInfoReader.getFreeSize() + this.mMemInfoReader.getCachedSize() + this.mState.mBackgroundProcessMemory;
                j = this.mState.mServiceProcessMemory;
            }
            long j2 = (totalSize - j) - freeSize;
            if (this.mCurTotalRam != totalSize || this.mCurHighRam != j2 || this.mCurMedRam != j || this.mCurLowRam != freeSize) {
                this.mCurTotalRam = totalSize;
                this.mCurHighRam = j2;
                this.mCurMedRam = j;
                this.mCurLowRam = freeSize;
                BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                this.mBackgroundProcessText.setText(getResources().getString(R.string.running_processes_header_ram, bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(getContext(), freeSize))));
                this.mAppsProcessText.setText(getResources().getString(R.string.running_processes_header_ram, bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(getContext(), j))));
                this.mForegroundProcessText.setText(getResources().getString(R.string.running_processes_header_ram, bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(getContext(), j2))));
                float f = totalSize;
                int i = (int) ((j2 / f) * 100.0f);
                this.mColorBar.setProgress(i);
                this.mColorBar.setSecondaryProgress(i + ((int) ((j / f) * 100.0f)));
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        RunningState.MergedItem mergedItem = (RunningState.MergedItem) ((ListView) adapterView).getAdapter().getItem(i);
        this.mCurSelected = mergedItem;
        startServiceDetailsActivity(mergedItem);
    }

    private void startServiceDetailsActivity(RunningState.MergedItem mergedItem) {
        if (this.mOwner != null && mergedItem != null) {
            Bundle bundle = new Bundle();
            if (mergedItem.mProcess != null) {
                bundle.putInt("uid", mergedItem.mProcess.mUid);
                bundle.putString("process", mergedItem.mProcess.mProcessName);
            }
            bundle.putInt("user_id", mergedItem.mUserId);
            bundle.putBoolean("background", this.mAdapter.mShowBackground);
            new SubSettingLauncher(getContext()).setDestination(RunningServiceDetails.class.getName()).setArguments(bundle).setTitle(R.string.runningservicedetails_settings_title).setSourceMetricsCategory(this.mOwner.getMetricsCategory()).launch();
        }
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        this.mActiveItems.remove(view);
    }

    public RunningProcessesView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mActiveItems = new HashMap<>();
        this.mBuilder = new StringBuilder(128);
        this.mCurTotalRam = -1L;
        this.mCurHighRam = -1L;
        this.mCurMedRam = -1L;
        this.mCurLowRam = -1L;
        this.mCurShowCached = false;
        this.mMemInfoReader = new MemInfoReader();
        this.mMyUserId = UserHandle.myUserId();
    }

    public void doCreate() {
        this.mAm = (ActivityManager) getContext().getSystemService("activity");
        this.mState = RunningState.getInstance(getContext());
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        layoutInflater.inflate(R.layout.running_processes_view, this);
        this.mListView = (ListView) findViewById(android.R.id.list);
        View viewFindViewById = findViewById(android.R.id.empty);
        if (viewFindViewById != null) {
            this.mListView.setEmptyView(viewFindViewById);
        }
        this.mListView.setOnItemClickListener(this);
        this.mListView.setRecyclerListener(this);
        this.mAdapter = new ServiceListAdapter(this.mState);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mHeader = layoutInflater.inflate(R.layout.running_processes_header, (ViewGroup) null);
        this.mListView.addHeaderView(this.mHeader, null, false);
        this.mColorBar = (ProgressBar) this.mHeader.findViewById(R.id.color_bar);
        Context context = getContext();
        this.mColorBar.setProgressTintList(ColorStateList.valueOf(context.getColor(R.color.running_processes_system_ram)));
        this.mColorBar.setSecondaryProgressTintList(ColorStateList.valueOf(Utils.getColorAccent(context)));
        this.mColorBar.setSecondaryProgressTintMode(PorterDuff.Mode.SRC);
        this.mColorBar.setProgressBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.running_processes_free_ram)));
        this.mColorBar.setProgressBackgroundTintMode(PorterDuff.Mode.SRC);
        this.mBackgroundProcessPrefix = (TextView) this.mHeader.findViewById(R.id.freeSizePrefix);
        this.mAppsProcessPrefix = (TextView) this.mHeader.findViewById(R.id.appsSizePrefix);
        this.mForegroundProcessPrefix = (TextView) this.mHeader.findViewById(R.id.systemSizePrefix);
        this.mBackgroundProcessText = (TextView) this.mHeader.findViewById(R.id.freeSize);
        this.mAppsProcessText = (TextView) this.mHeader.findViewById(R.id.appsSize);
        this.mForegroundProcessText = (TextView) this.mHeader.findViewById(R.id.systemSize);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        this.mAm.getMemoryInfo(memoryInfo);
        this.SECONDARY_SERVER_MEM = memoryInfo.secondaryServerThreshold;
    }

    public void doPause() {
        this.mState.pause();
        this.mDataAvail = null;
        this.mOwner = null;
    }

    public boolean doResume(SettingsPreferenceFragment settingsPreferenceFragment, Runnable runnable) {
        this.mOwner = settingsPreferenceFragment;
        this.mState.resume(this);
        if (this.mState.hasData()) {
            refreshUi(true);
            return true;
        }
        this.mDataAvail = runnable;
        return false;
    }

    void updateTimes() {
        Iterator<ActiveItem> it = this.mActiveItems.values().iterator();
        while (it.hasNext()) {
            ActiveItem next = it.next();
            if (next.mRootView.getWindowToken() == null) {
                it.remove();
            } else {
                next.updateTime(getContext(), this.mBuilder);
            }
        }
    }

    @Override
    public void onRefreshUi(int i) {
        switch (i) {
            case 0:
                updateTimes();
                break;
            case 1:
                refreshUi(false);
                updateTimes();
                break;
            case 2:
                refreshUi(true);
                updateTimes();
                break;
        }
    }
}
