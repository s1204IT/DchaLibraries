package com.android.settings.applications.appinfo;

import android.content.Context;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;

/* loaded from: classes.dex */
public class DefaultPhoneShortcutPreferenceController extends DefaultAppShortcutPreferenceControllerBase {
    private static final String KEY = "default_phone_app";

    public DefaultPhoneShortcutPreferenceController(Context context, String str) {
        super(context, KEY, str);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean hasAppCapability() {
        return DefaultPhonePreferenceController.hasPhonePreference(this.mPackageName, this.mContext);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean isDefaultApp() {
        return DefaultPhonePreferenceController.isPhoneDefault(this.mPackageName, this.mContext);
    }
}
