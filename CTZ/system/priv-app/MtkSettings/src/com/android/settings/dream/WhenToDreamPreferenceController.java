package com.android.settings.dream;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;

/* loaded from: classes.dex */
public class WhenToDreamPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final DreamBackend mBackend;

    WhenToDreamPreferenceController(Context context) {
        super(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(preference.getContext().getString(DreamSettings.getDreamSettingDescriptionResId(this.mBackend.getWhenToDreamSetting())));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "when_to_start";
    }
}
