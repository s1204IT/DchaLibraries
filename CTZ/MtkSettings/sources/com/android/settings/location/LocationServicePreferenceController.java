package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.List;

public class LocationServicePreferenceController extends LocationBasePreferenceController implements LifecycleObserver, OnPause, OnResume {
    static final IntentFilter INTENT_FILTER_INJECTED_SETTING_CHANGED = new IntentFilter("android.location.InjectedSettingChanged");
    private PreferenceCategory mCategoryLocationServices;
    private final LocationSettings mFragment;
    BroadcastReceiver mInjectedSettingsReceiver;
    private final SettingsInjector mInjector;

    public LocationServicePreferenceController(Context context, LocationSettings locationSettings, Lifecycle lifecycle) {
        this(context, locationSettings, lifecycle, new SettingsInjector(context));
    }

    LocationServicePreferenceController(Context context, LocationSettings locationSettings, Lifecycle lifecycle, SettingsInjector settingsInjector) {
        super(context, lifecycle);
        this.mFragment = locationSettings;
        this.mInjector = settingsInjector;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return "location_services";
    }

    @Override
    public boolean isAvailable() {
        return this.mInjector.hasInjectedSettings(this.mLocationEnabler.isManagedProfileRestrictedByBase() ? UserHandle.myUserId() : -2);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mCategoryLocationServices = (PreferenceCategory) preferenceScreen.findPreference("location_services");
    }

    @Override
    public void updateState(Preference preference) {
        this.mCategoryLocationServices.removeAll();
        List<Preference> locationServices = getLocationServices();
        for (Preference preference2 : locationServices) {
            if (preference2 instanceof RestrictedAppPreference) {
                ((RestrictedAppPreference) preference2).checkRestrictionAndSetDisabled();
            }
        }
        LocationSettings.addPreferencesSorted(locationServices, this.mCategoryLocationServices);
    }

    @Override
    public void onLocationModeChanged(int i, boolean z) {
        this.mInjector.reloadStatusMessages();
    }

    @Override
    public void onResume() {
        if (this.mInjectedSettingsReceiver == null) {
            this.mInjectedSettingsReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Log.isLoggable("LocationServicePrefCtrl", 3)) {
                        Log.d("LocationServicePrefCtrl", "Received settings change intent: " + intent);
                    }
                    LocationServicePreferenceController.this.mInjector.reloadStatusMessages();
                }
            };
        }
        this.mContext.registerReceiver(this.mInjectedSettingsReceiver, INTENT_FILTER_INJECTED_SETTING_CHANGED);
    }

    @Override
    public void onPause() {
        this.mContext.unregisterReceiver(this.mInjectedSettingsReceiver);
    }

    private List<Preference> getLocationServices() {
        return this.mInjector.getInjectedSettings(this.mFragment.getPreferenceManager().getContext(), this.mLocationEnabler.isManagedProfileRestrictedByBase() ? UserHandle.myUserId() : -2);
    }
}
