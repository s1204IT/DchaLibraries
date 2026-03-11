package com.android.settings.development.featureflags;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import android.util.FeatureFlagUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import java.util.Iterator;
import java.util.Map;

public class FeatureFlagsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart {
    private PreferenceScreen mScreen;

    public FeatureFlagsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mScreen = preferenceScreen;
    }

    @Override
    public void onStart() {
        Map allFeatureFlags;
        if (this.mScreen == null || (allFeatureFlags = FeatureFlagUtils.getAllFeatureFlags()) == null) {
            return;
        }
        this.mScreen.removeAll();
        Context context = this.mScreen.getContext();
        Iterator it = allFeatureFlags.keySet().iterator();
        while (it.hasNext()) {
            this.mScreen.addPreference(new FeatureFlagPreference(context, (String) it.next()));
        }
    }
}
