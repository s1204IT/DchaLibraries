package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public class VerifyAppsOverUsbPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, AdbOnChangeListener {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;
    private final PackageManagerWrapper mPackageManager;
    private final RestrictedLockUtilsDelegate mRestrictedLockUtils;

    class RestrictedLockUtilsDelegate {
        RestrictedLockUtilsDelegate() {
        }

        public RestrictedLockUtils.EnforcedAdmin checkIfRestrictionEnforced(Context context, String str, int i) {
            return RestrictedLockUtils.checkIfRestrictionEnforced(context, str, i);
        }
    }

    public VerifyAppsOverUsbPreferenceController(Context context) {
        super(context);
        this.mRestrictedLockUtils = new RestrictedLockUtilsDelegate();
        this.mPackageManager = new PackageManagerWrapper(context.getPackageManager());
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_setting_visible", 1) > 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "verify_apps_over_usb";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "verifier_verify_adb_installs", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
        if (!shouldBeEnabled()) {
            restrictedSwitchPreference.setChecked(false);
            restrictedSwitchPreference.setDisabledByAdmin(null);
            restrictedSwitchPreference.setEnabled(false);
            return;
        }
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = this.mRestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "ensure_verify_apps", UserHandle.myUserId());
        if (enforcedAdminCheckIfRestrictionEnforced != null) {
            restrictedSwitchPreference.setChecked(true);
            restrictedSwitchPreference.setDisabledByAdmin(enforcedAdminCheckIfRestrictionEnforced);
        } else {
            restrictedSwitchPreference.setEnabled(true);
            restrictedSwitchPreference.setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "verifier_verify_adb_installs", 1) != 0);
        }
    }

    @Override // com.android.settings.development.AdbOnChangeListener
    public void onAdbSettingChanged() {
        if (isAvailable()) {
            updateState(this.mPreference);
        }
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        updateState(this.mPreference);
    }

    private boolean shouldBeEnabled() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (Settings.Global.getInt(contentResolver, "adb_enabled", 0) == 0 || Settings.Global.getInt(contentResolver, "package_verifier_enable", 1) == 0) {
            return false;
        }
        Intent intent = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        intent.setType("application/vnd.android.package-archive");
        intent.addFlags(1);
        return this.mPackageManager.queryBroadcastReceivers(intent, 0).size() != 0;
    }
}
