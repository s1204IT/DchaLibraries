package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.os.UserHandle;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class DefaultWorkBrowserPreferenceController extends DefaultBrowserPreferenceController {
    private final UserHandle mUserHandle;

    public DefaultWorkBrowserPreferenceController(Context context) {
        super(context);
        this.mUserHandle = Utils.getManagedProfile(this.mUserManager);
        if (this.mUserHandle != null) {
            this.mUserId = this.mUserHandle.getIdentifier();
        }
    }

    @Override // com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "work_default_browser";
    }

    @Override // com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (this.mUserHandle == null) {
            return false;
        }
        return super.isAvailable();
    }
}
