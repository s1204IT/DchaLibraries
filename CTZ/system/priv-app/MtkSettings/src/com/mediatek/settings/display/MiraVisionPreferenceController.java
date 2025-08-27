package com.mediatek.settings.display;

import android.content.Context;
import android.os.UserHandle;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;

/* loaded from: classes.dex */
public class MiraVisionPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public MiraVisionPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (FeatureOption.MTK_MIRAVISION_SETTING_SUPPORT || FeatureOption.MTK_BLULIGHT_DEFENDER_SUPPORT || FeatureOption.MTK_AAL_SUPPORT) && (UserHandle.myUserId() == 0 || !FeatureOption.MTK_PRODUCT_IS_TABLET);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "mira_vision";
    }
}
