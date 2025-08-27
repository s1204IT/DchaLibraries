package com.android.settings.applications.appinfo;

import android.content.Context;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;

/* loaded from: classes.dex */
public class DefaultSmsShortcutPreferenceController extends DefaultAppShortcutPreferenceControllerBase {
    private static final String KEY = "default_sms_app";

    public DefaultSmsShortcutPreferenceController(Context context, String str) {
        super(context, KEY, str);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean hasAppCapability() {
        return DefaultSmsPreferenceController.hasSmsPreference(this.mPackageName, this.mContext);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean isDefaultApp() {
        return DefaultSmsPreferenceController.isSmsDefault(this.mPackageName, this.mContext);
    }
}
