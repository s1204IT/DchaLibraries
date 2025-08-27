package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.TogglePreferenceController;

/* loaded from: classes.dex */
public class LockdownButtonPreferenceController extends TogglePreferenceController {
    private static final String KEY_LOCKDOWN_ENALBED = "security_setting_lockdown_enabled";
    private final LockPatternUtils mLockPatternUtils;

    public LockdownButtonPreferenceController(Context context) {
        super(context, KEY_LOCKDOWN_ENALBED);
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mLockPatternUtils.isSecure(UserHandle.myUserId())) {
            return 0;
        }
        return 3;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "lockdown_in_power_menu", 0) != 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "lockdown_in_power_menu", z ? 1 : 0);
        return true;
    }
}
