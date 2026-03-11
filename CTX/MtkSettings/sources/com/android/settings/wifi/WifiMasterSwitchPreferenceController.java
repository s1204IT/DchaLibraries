package com.android.settings.wifi;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SummaryUpdater;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class WifiMasterSwitchPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, SummaryUpdater.OnSummaryChangeListener, LifecycleObserver, OnPause, OnResume, OnStart, OnStop {
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final WifiSummaryUpdater mSummaryHelper;
    private WifiEnabler mWifiEnabler;
    private MasterSwitchPreference mWifiPreference;

    public WifiMasterSwitchPreferenceController(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        super(context);
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        this.mSummaryHelper = new WifiSummaryUpdater(this.mContext, this);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mWifiPreference = (MasterSwitchPreference) preferenceScreen.findPreference("toggle_wifi");
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_wifi_settings);
    }

    @Override
    public String getPreferenceKey() {
        return "toggle_wifi";
    }

    @Override
    public void onResume() {
        this.mSummaryHelper.register(true);
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.resume(this.mContext);
        }
    }

    @Override
    public void onPause() {
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.pause();
        }
        this.mSummaryHelper.register(false);
    }

    @Override
    public void onStart() {
        this.mWifiEnabler = new WifiEnabler(this.mContext, new MasterSwitchController(this.mWifiPreference), this.mMetricsFeatureProvider);
    }

    @Override
    public void onStop() {
        if (this.mWifiEnabler != null) {
            this.mWifiEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onSummaryChanged(String str) {
        if (this.mWifiPreference != null) {
            this.mWifiPreference.setSummary(str);
        }
    }
}
