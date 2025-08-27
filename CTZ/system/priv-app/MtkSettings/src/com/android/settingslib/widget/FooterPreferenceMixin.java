package com.android.settingslib.widget;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.SetPreferenceScreen;

/* loaded from: classes.dex */
public class FooterPreferenceMixin implements LifecycleObserver, SetPreferenceScreen {
    private FooterPreference mFooterPreference;
    private final PreferenceFragment mFragment;

    public FooterPreferenceMixin(PreferenceFragment preferenceFragment, Lifecycle lifecycle) {
        this.mFragment = preferenceFragment;
        lifecycle.addObserver(this);
    }

    @Override // com.android.settingslib.core.lifecycle.events.SetPreferenceScreen
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (this.mFooterPreference != null) {
            preferenceScreen.addPreference(this.mFooterPreference);
        }
    }

    public FooterPreference createFooterPreference() {
        PreferenceScreen preferenceScreen = this.mFragment.getPreferenceScreen();
        if (this.mFooterPreference != null && preferenceScreen != null) {
            preferenceScreen.removePreference(this.mFooterPreference);
        }
        this.mFooterPreference = new FooterPreference(getPrefContext());
        if (preferenceScreen != null) {
            preferenceScreen.addPreference(this.mFooterPreference);
        }
        return this.mFooterPreference;
    }

    private Context getPrefContext() {
        return this.mFragment.getPreferenceManager().getContext();
    }

    public boolean hasFooter() {
        return this.mFooterPreference != null;
    }
}
