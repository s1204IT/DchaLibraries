package com.android.settings.notification;

import android.content.Context;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.ZenModeSettings;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeBlockedEffectsPreferenceController extends AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private final ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModeBlockedEffectsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "zen_mode_block_effects_settings", lifecycle);
        this.mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_block_effects_settings";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return this.mSummaryBuilder.getBlockedEffectsSummary(getPolicy());
    }
}
