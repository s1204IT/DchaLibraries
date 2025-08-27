package com.android.settings.development;

import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/* loaded from: classes.dex */
public class DevelopmentSwitchBarController implements LifecycleObserver, OnStart, OnStop {
    private final boolean mIsAvailable;
    private final DevelopmentSettingsDashboardFragment mSettings;
    private final SwitchBar mSwitchBar;

    public DevelopmentSwitchBarController(DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment, SwitchBar switchBar, boolean z, Lifecycle lifecycle) {
        this.mSwitchBar = switchBar;
        this.mIsAvailable = z && !Utils.isMonkeyRunning();
        this.mSettings = developmentSettingsDashboardFragment;
        if (!this.mIsAvailable) {
            this.mSwitchBar.setEnabled(false);
        } else {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mSwitchBar.setChecked(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this.mSettings.getContext()));
        this.mSwitchBar.addOnSwitchChangeListener(this.mSettings);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        this.mSwitchBar.removeOnSwitchChangeListener(this.mSettings);
    }
}
