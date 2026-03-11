package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class ShowPasswordPreferenceController extends TogglePreferenceController {
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final int MY_USER_ID = UserHandle.myUserId();
    private final LockPatternUtils mLockPatternUtils;

    public ShowPasswordPreferenceController(Context context) {
        super(context, KEY_SHOW_PASSWORD);
        this.mLockPatternUtils = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(this.mContext.getContentResolver(), KEY_SHOW_PASSWORD, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean z) {
        Settings.System.putInt(this.mContext.getContentResolver(), KEY_SHOW_PASSWORD, z ? 1 : 0);
        this.mLockPatternUtils.setVisiblePasswordEnabled(z, MY_USER_ID);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_show_password) ? 0 : 2;
    }
}
