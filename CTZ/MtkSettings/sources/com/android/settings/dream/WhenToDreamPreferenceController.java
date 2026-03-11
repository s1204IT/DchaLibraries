package com.android.settings.dream;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;

public class WhenToDreamPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final DreamBackend mBackend;

    WhenToDreamPreferenceController(Context context) {
        super(context);
        this.mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(preference.getContext().getString(DreamSettings.getDreamSettingDescriptionResId(this.mBackend.getWhenToDreamSetting())));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "when_to_start";
    }
}
