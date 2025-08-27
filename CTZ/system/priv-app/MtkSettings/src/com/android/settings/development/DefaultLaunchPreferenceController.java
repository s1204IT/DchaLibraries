package com.android.settings.development;

import android.content.Context;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class DefaultLaunchPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    private final String mPreferenceKey;

    public DefaultLaunchPreferenceController(Context context, String str) {
        super(context);
        this.mPreferenceKey = str;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mPreferenceKey;
    }
}
