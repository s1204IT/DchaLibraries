package com.android.storagemanager.deletionhelper;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import com.android.storagemanager.deletionhelper.AppsAsyncLoader;
import com.android.storagemanager.deletionhelper.DeletionType;
import com.android.storagemanager.deletionhelper.PackageDeletionTask;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AppDeletionType implements LoaderManager.LoaderCallbacks<List<AppsAsyncLoader.PackageInfo>>, DeletionType {
    private AppListener mAppListener;
    private List<AppsAsyncLoader.PackageInfo> mApps;
    private HashSet<String> mCheckedApplications;
    private Context mContext;
    private DeletionType.FreeableChangedListener mListener;
    private int mLoadingStatus = 0;
    private int mThresholdType;

    public interface AppListener {
        void onAppRebuild(List<AppsAsyncLoader.PackageInfo> list);
    }

    public AppDeletionType(DeletionHelperSettings deletionHelperSettings, HashSet<String> hashSet, int i) {
        this.mThresholdType = i;
        this.mContext = deletionHelperSettings.getContext();
        if (hashSet != null) {
            this.mCheckedApplications = hashSet;
        } else {
            this.mCheckedApplications = new HashSet<>();
        }
        Bundle bundle = new Bundle(1);
        bundle.putInt("threshold_type", this.mThresholdType);
        deletionHelperSettings.getLoaderManager().initLoader(25, bundle, this);
    }

    @Override
    public void registerFreeableChangedListener(DeletionType.FreeableChangedListener freeableChangedListener) {
        this.mListener = freeableChangedListener;
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onSaveInstanceStateBundle(Bundle bundle) {
        bundle.putSerializable("checkedSet", this.mCheckedApplications);
    }

    @Override
    public void clearFreeableData(final Activity activity) {
        if (this.mApps == null) {
            return;
        }
        ArraySet arraySet = new ArraySet();
        Iterator<AppsAsyncLoader.PackageInfo> it = this.mApps.iterator();
        while (it.hasNext()) {
            String str = it.next().packageName;
            if (this.mCheckedApplications.contains(str)) {
                arraySet.add(str);
            }
        }
        new PackageDeletionTask(activity.getPackageManager(), arraySet, new PackageDeletionTask.Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError() {
                Log.e("AppDeletionType", "An error occurred while uninstalling packages.");
                MetricsLogger.action(activity, 471);
            }
        }).run();
    }

    public void registerView(AppDeletionPreferenceGroup appDeletionPreferenceGroup) {
        this.mAppListener = appDeletionPreferenceGroup;
    }

    public void setChecked(String str, boolean z) {
        if (z) {
            this.mCheckedApplications.add(str);
        } else {
            this.mCheckedApplications.remove(str);
        }
        maybeNotifyListener();
    }

    public long getTotalAppsFreeableSpace(boolean z) {
        if (this.mApps == null) {
            return 0L;
        }
        int size = this.mApps.size();
        long j = 0;
        for (int i = 0; i < size; i++) {
            AppsAsyncLoader.PackageInfo packageInfo = this.mApps.get(i);
            long j2 = packageInfo.size;
            String str = packageInfo.packageName;
            if ((z || this.mCheckedApplications.contains(str)) && j2 > 0) {
                j += j2;
            }
        }
        return j;
    }

    public boolean isChecked(String str) {
        return this.mCheckedApplications.contains(str);
    }

    private AppsAsyncLoader.AppFilter getFilter(int i) {
        if (i == 1) {
            return AppsAsyncLoader.FILTER_NO_THRESHOLD;
        }
        return AppsAsyncLoader.FILTER_USAGE_STATS;
    }

    private void maybeNotifyListener() {
        if (this.mListener != null) {
            this.mListener.onFreeableChanged(this.mApps.size(), getTotalAppsFreeableSpace(false));
        }
    }

    public long getDeletionThreshold() {
        if (this.mThresholdType == 1) {
            return 0L;
        }
        return 90L;
    }

    @Override
    public int getLoadingStatus() {
        return this.mLoadingStatus;
    }

    @Override
    public int getContentCount() {
        return this.mApps.size();
    }

    @Override
    public void setLoadingStatus(int i) {
        this.mLoadingStatus = i;
    }

    @Override
    public Loader<List<AppsAsyncLoader.PackageInfo>> onCreateLoader(int i, Bundle bundle) {
        return new AppsAsyncLoader.Builder(this.mContext).setUid(UserHandle.myUserId()).setUuid("private").setStorageStatsSource(new StorageStatsSource(this.mContext)).setPackageManager(new PackageManagerWrapper(this.mContext.getPackageManager())).setUsageStatsManager((UsageStatsManager) this.mContext.getSystemService("usagestats")).setFilter(getFilter(bundle.getInt("threshold_type", 0))).build();
    }

    @Override
    public void onLoadFinished(Loader<List<AppsAsyncLoader.PackageInfo>> loader, List<AppsAsyncLoader.PackageInfo> list) {
        this.mApps = list;
        updateLoadingStatus();
        maybeNotifyListener();
        this.mAppListener.onAppRebuild(this.mApps);
    }

    @Override
    public void onLoaderReset(Loader<List<AppsAsyncLoader.PackageInfo>> loader) {
    }
}
