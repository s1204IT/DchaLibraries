package com.android.settings.applications.defaultapps;

import android.content.Context;
import android.os.UserHandle;
import com.android.settings.Utils;

public class DefaultWorkPhonePreferenceController extends DefaultPhonePreferenceController {
    private final UserHandle mUserHandle;

    public DefaultWorkPhonePreferenceController(Context context) {
        super(context);
        this.mUserHandle = Utils.getManagedProfile(this.mUserManager);
        if (this.mUserHandle != null) {
            this.mUserId = this.mUserHandle.getIdentifier();
        }
    }

    @Override
    public boolean isAvailable() {
        if (this.mUserHandle == null) {
            return false;
        }
        return super.isAvailable();
    }

    @Override
    public String getPreferenceKey() {
        return "work_default_phone_app";
    }
}
