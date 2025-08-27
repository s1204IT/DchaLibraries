package com.mediatek.settings.system;

import android.content.Context;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;

/* loaded from: classes.dex */
public class DrmResetPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public DrmResetPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return FeatureOption.MTK_DRM_APP;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "drm_settings";
    }
}
