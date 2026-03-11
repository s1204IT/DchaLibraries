package com.android.settings.applications.appinfo;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.datausage.AppDataUsage;
import com.android.settings.datausage.DataUsageList;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settingslib.AppItem;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;

public class AppDataUsagePreferenceController extends AppInfoPreferenceControllerBase implements LoaderManager.LoaderCallbacks<ChartData>, LifecycleObserver, OnPause, OnResume {
    private ChartData mChartData;
    private INetworkStatsSession mStatsSession;

    public AppDataUsagePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public int getAvailabilityStatus() {
        return !isBandwidthControlEnabled() ? 1 : 0;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            try {
                this.mStatsSession = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats")).openSession();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getDataSummary());
    }

    @Override
    public void onResume() {
        if (this.mStatsSession != null) {
            int i = this.mParent.getAppEntry().info.uid;
            AppItem appItem = new AppItem(i);
            appItem.addUid(i);
            LoaderManager loaderManager = this.mParent.getLoaderManager();
            AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
            loaderManager.restartLoader(2, ChartDataLoader.buildArgs(getTemplate(this.mContext), appItem), this);
        }
    }

    @Override
    public void onPause() {
        LoaderManager loaderManager = this.mParent.getLoaderManager();
        AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
        loaderManager.destroyLoader(2);
    }

    @Override
    public Loader<ChartData> onCreateLoader(int i, Bundle bundle) {
        return new ChartDataLoader(this.mContext, this.mStatsSession, bundle);
    }

    @Override
    public void onLoadFinished(Loader<ChartData> loader, ChartData chartData) {
        this.mChartData = chartData;
        updateState(this.mPreference);
    }

    @Override
    public void onLoaderReset(Loader<ChartData> loader) {
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppDataUsage.class;
    }

    private CharSequence getDataSummary() {
        if (this.mChartData != null) {
            long totalBytes = this.mChartData.detail.getTotalBytes();
            if (totalBytes == 0) {
                return this.mContext.getString(R.string.no_data_usage);
            }
            return this.mContext.getString(R.string.data_summary_format, Formatter.formatFileSize(this.mContext, totalBytes), DateUtils.formatDateTime(this.mContext, this.mChartData.detail.getStart(), 65552));
        }
        return this.mContext.getString(R.string.computing_size);
    }

    private static NetworkTemplate getTemplate(Context context) {
        if (DataUsageList.hasReadyMobileRadio(context)) {
            return NetworkTemplate.buildTemplateMobileWildcard();
        }
        if (DataUsageUtils.hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }

    boolean isBandwidthControlEnabled() {
        return Utils.isBandwidthControlEnabled();
    }
}
