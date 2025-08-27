package com.android.settings.notification;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class DockingSoundPreferenceController extends SettingPrefController {
    public DockingSoundPreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, Lifecycle lifecycle) {
        super(context, settingsPreferenceFragment, lifecycle);
        this.mPreference = new SettingPref(1, "docking_sounds", "dock_sounds_enabled", 1, new int[0]) { // from class: com.android.settings.notification.DockingSoundPreferenceController.1
            @Override // com.android.settings.notification.SettingPref
            public boolean isApplicable(Context context2) {
                return context2.getResources().getBoolean(R.bool.has_dock_settings);
            }
        };
    }
}
