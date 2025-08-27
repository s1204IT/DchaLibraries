package com.android.settings.notification;

import android.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class EmergencyBroadcastPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private AccountRestrictionHelper mHelper;
    private PackageManager mPm;
    private final String mPrefKey;
    private UserManager mUserManager;

    public EmergencyBroadcastPreferenceController(Context context, String str) {
        this(context, new AccountRestrictionHelper(context), str);
    }

    EmergencyBroadcastPreferenceController(Context context, AccountRestrictionHelper accountRestrictionHelper, String str) {
        super(context);
        this.mPrefKey = str;
        this.mHelper = accountRestrictionHelper;
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mPm = this.mContext.getPackageManager();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        ((RestrictedPreference) preference).checkRestrictionAndSetDisabled("no_config_cell_broadcasts");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mPrefKey;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mUserManager.isAdminUser() && isCellBroadcastAppLinkEnabled() && !this.mHelper.hasBaseUserRestriction("no_config_cell_broadcasts", UserHandle.myUserId());
    }

    private boolean isCellBroadcastAppLinkEnabled() throws Resources.NotFoundException {
        boolean z = this.mContext.getResources().getBoolean(R.^attr-private.colorSwitchThumbNormal);
        if (!z) {
            return z;
        }
        try {
            if (this.mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver") == 2) {
                return false;
            }
            return z;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
