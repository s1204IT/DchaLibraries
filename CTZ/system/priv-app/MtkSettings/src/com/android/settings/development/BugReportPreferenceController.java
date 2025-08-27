package com.android.settings.development;

import android.content.Context;
import android.os.UserManager;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class BugReportPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    private final UserManager mUserManager;

    public BugReportPreferenceController(Context context) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !this.mUserManager.hasUserRestriction("no_debugging_features");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bugreport";
    }
}
