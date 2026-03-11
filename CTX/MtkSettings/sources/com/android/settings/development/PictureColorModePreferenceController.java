package com.android.settings.development;

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class PictureColorModePreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private ColorModePreference mPreference;

    public PictureColorModePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return getColorModeDescriptionsSize() > 1 && !isWideColorGamut();
    }

    @Override
    public String getPreferenceKey() {
        return "picture_color_mode";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (ColorModePreference) preferenceScreen.findPreference(getPreferenceKey());
        if (this.mPreference != null) {
            this.mPreference.updateCurrentAndSupported();
        }
    }

    @Override
    public void onResume() {
        if (this.mPreference == null) {
            return;
        }
        this.mPreference.startListening();
        this.mPreference.updateCurrentAndSupported();
    }

    @Override
    public void onPause() {
        if (this.mPreference == null) {
            return;
        }
        this.mPreference.stopListening();
    }

    boolean isWideColorGamut() {
        return this.mContext.getResources().getConfiguration().isScreenWideColorGamut();
    }

    int getColorModeDescriptionsSize() {
        return ColorModePreference.getColorModeDescriptions(this.mContext).size();
    }
}
