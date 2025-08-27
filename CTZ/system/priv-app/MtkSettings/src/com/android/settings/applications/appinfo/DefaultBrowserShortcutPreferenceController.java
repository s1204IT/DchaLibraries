package com.android.settings.applications.appinfo;

import android.content.Context;
import android.os.UserHandle;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;

/* loaded from: classes.dex */
public class DefaultBrowserShortcutPreferenceController extends DefaultAppShortcutPreferenceControllerBase {
    private static final String KEY = "default_browser";

    public DefaultBrowserShortcutPreferenceController(Context context, String str) {
        super(context, KEY, str);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean hasAppCapability() {
        return DefaultBrowserPreferenceController.hasBrowserPreference(this.mPackageName, this.mContext);
    }

    @Override // com.android.settings.applications.appinfo.DefaultAppShortcutPreferenceControllerBase
    protected boolean isDefaultApp() {
        return new DefaultBrowserPreferenceController(this.mContext).isBrowserDefault(this.mPackageName, UserHandle.myUserId());
    }
}
