package com.android.settings.display;

import android.R;
import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dream.DreamSettings;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;

/* loaded from: classes.dex */
public class ScreenSaverPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public ScreenSaverPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.^attr-private.dialogTitleDecorLayout) && !FeatureOption.MTK_GMO_RAM_OPTIMIZE;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "screensaver";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setSummary(DreamSettings.getSummaryTextWithDreamName(this.mContext));
    }
}
