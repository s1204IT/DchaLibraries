package com.android.settings.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class LocationPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private Context mContext;
    BroadcastReceiver mLocationProvidersChangedReceiver;
    private Preference mPreference;

    public LocationPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mContext = context;
        this.mLocationProvidersChangedReceiver = new BroadcastReceiver() { // from class: com.android.settings.location.LocationPreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.location.PROVIDERS_CHANGED")) {
                    LocationPreferenceController.this.updateSummary();
                }
            }
        };
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference("location");
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mLocationProvidersChangedReceiver != null) {
            this.mContext.registerReceiver(this.mLocationProvidersChangedReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mLocationProvidersChangedReceiver != null) {
            this.mContext.unregisterReceiver(this.mLocationProvidersChangedReceiver);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setSummary(getLocationSummary(this.mContext));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "location";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    public void updateSummary() {
        updateState(this.mPreference);
    }

    public static String getLocationSummary(Context context) {
        if (Settings.Secure.getInt(context.getContentResolver(), "location_mode", 0) != 0) {
            return context.getString(R.string.location_on_summary);
        }
        return context.getString(R.string.location_off_summary);
    }
}
