package com.android.settings.applications.appinfo;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStorageSettings;
import com.android.settings.applications.FetchPackageStorageAsyncLoader;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AppStoragePreferenceController extends AppInfoPreferenceControllerBase implements LoaderManager.LoaderCallbacks<StorageStatsSource.AppStorageStats>, LifecycleObserver, OnPause, OnResume {
    private StorageStatsSource.AppStorageStats mLastResult;

    public AppStoragePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mParent.getAppEntry() != null) {
            preference.setSummary(getStorageSummary(this.mLastResult, (this.mParent.getAppEntry().info.flags & 262144) != 0));
        }
    }

    @Override
    public void onResume() {
        LoaderManager loaderManager = this.mParent.getLoaderManager();
        AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
        loaderManager.restartLoader(3, Bundle.EMPTY, this);
    }

    @Override
    public void onPause() {
        LoaderManager loaderManager = this.mParent.getLoaderManager();
        AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
        loaderManager.destroyLoader(3);
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppStorageSettings.class;
    }

    CharSequence getStorageSummary(StorageStatsSource.AppStorageStats appStorageStats, boolean z) {
        int i;
        if (appStorageStats == null) {
            return this.mContext.getText(R.string.computing_size);
        }
        Context context = this.mContext;
        if (z) {
            i = R.string.storage_type_external;
        } else {
            i = R.string.storage_type_internal;
        }
        return this.mContext.getString(R.string.storage_summary_format, Formatter.formatFileSize(this.mContext, appStorageStats.getTotalBytes()), context.getString(i).toString().toLowerCase());
    }

    @Override
    public Loader<StorageStatsSource.AppStorageStats> onCreateLoader(int i, Bundle bundle) {
        return new FetchPackageStorageAsyncLoader(this.mContext, new StorageStatsSource(this.mContext), this.mParent.getAppEntry().info, UserHandle.of(UserHandle.myUserId()));
    }

    @Override
    public void onLoadFinished(Loader<StorageStatsSource.AppStorageStats> loader, StorageStatsSource.AppStorageStats appStorageStats) {
        this.mLastResult = appStorageStats;
        updateState(this.mPreference);
    }

    @Override
    public void onLoaderReset(Loader<StorageStatsSource.AppStorageStats> loader) {
    }
}
