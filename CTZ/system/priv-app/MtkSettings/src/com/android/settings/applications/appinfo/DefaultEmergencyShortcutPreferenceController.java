package com.android.settings.applications.appinfo;

import android.content.Context;
import com.android.settings.applications.defaultapps.DefaultEmergencyPreferenceController;

/* loaded from: classes.dex */
public class DefaultEmergencyShortcutPreferenceController extends DefaultAppShortcutPreferenceControllerBase {
    private static final String KEY = "default_emergency_app";

    public DefaultEmergencyShortcutPreferenceController(Context context, String str) {
        super(context, KEY, str);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean hasAppCapability() {
        return DefaultEmergencyPreferenceController.hasEmergencyPreference(this.mPackageName, this.mContext);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean isDefaultApp() {
        return DefaultEmergencyPreferenceController.isEmergencyDefault(this.mPackageName, this.mContext);
    }
}
