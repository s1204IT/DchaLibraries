package com.android.settings.development;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.AbstractEnableAdbPreferenceController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDevExt;

/* loaded from: classes.dex */
public class AdbPreferenceController extends AbstractEnableAdbPreferenceController implements PreferenceControllerMixin {
    private IDevExt mDevExt;
    private final DevelopmentSettingsDashboardFragment mFragment;

    public AdbPreferenceController(Context context, DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment) {
        super(context);
        this.mFragment = developmentSettingsDashboardFragment;
        this.mDevExt = UtilsExt.getDevExt(context);
    }

    @Override // com.android.settingslib.development.AbstractEnableAdbPreferenceController, com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mDevExt.customUSBPreference(this.mPreference);
    }

    @Override // com.android.settingslib.development.AbstractEnableAdbPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mDevExt.customUSBPreference(this.mPreference);
    }

    public void onAdbDialogConfirmed() {
        writeAdbSetting(true);
    }

    public void onAdbDialogDismissed() {
        updateState(this.mPreference);
    }

    @Override // com.android.settingslib.core.ConfirmationDialogController
    public void showConfirmationDialog(Preference preference) {
        EnableAdbWarningDialog.show(this.mFragment);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeAdbSetting(false);
        this.mPreference.setChecked(false);
        this.mDevExt.customUSBPreference(this.mPreference);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    public void onDeveloperOptionsEnabled() {
        super.onDeveloperOptionsEnabled();
        this.mDevExt.customUSBPreference(this.mPreference);
    }
}
