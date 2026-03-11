package com.android.settings.bluetooth;

import android.content.Context;
import android.os.UserHandle;
import com.android.settingslib.RestrictedLockUtils;

public class RestrictionUtils {
    public RestrictedLockUtils.EnforcedAdmin checkIfRestrictionEnforced(Context context, String str) {
        return RestrictedLockUtils.checkIfRestrictionEnforced(context, str, UserHandle.myUserId());
    }
}
