package com.android.settings.applications.appinfo;

import android.content.Context;
import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public class DefaultHomeShortcutPreferenceController extends DefaultAppShortcutPreferenceControllerBase {
    private static final String KEY = "default_home";

    public DefaultHomeShortcutPreferenceController(Context context, String str) {
        super(context, KEY, str);
    }

    @Override
    protected boolean hasAppCapability() {
        return DefaultHomePreferenceController.hasHomePreference(this.mPackageName, this.mContext);
    }

    @Override
    protected boolean isDefaultApp() {
        return DefaultHomePreferenceController.isHomeDefault(this.mPackageName, new PackageManagerWrapper(this.mContext.getPackageManager()));
    }
}
