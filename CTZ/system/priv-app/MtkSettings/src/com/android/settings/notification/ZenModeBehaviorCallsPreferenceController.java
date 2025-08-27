package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.ZenModeSettings;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeBehaviorCallsPreferenceController extends AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private final ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModeBehaviorCallsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "zen_mode_calls_settings", lifecycle);
        this.mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_calls_settings";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(this.mSummaryBuilder.getCallsSettingSummary(getPolicy()));
    }
}
