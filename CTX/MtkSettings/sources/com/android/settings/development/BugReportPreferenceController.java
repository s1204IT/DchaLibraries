package com.android.settings.development;

import android.content.Context;
import android.os.UserManager;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BugReportPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    private final UserManager mUserManager;

    public BugReportPreferenceController(Context context) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override
    public boolean isAvailable() {
        return !this.mUserManager.hasUserRestriction("no_debugging_features");
    }

    @Override
    public String getPreferenceKey() {
        return "bugreport";
    }
}
