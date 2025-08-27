package com.android.settings.fuelgauge;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class AutoRestrictionPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    private static final String KEY_SMART_BATTERY = "auto_restriction";
    private static final int OFF = 0;
    private static final int ON = 1;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public AutoRestrictionPreferenceController(Context context) {
        super(context, KEY_SMART_BATTERY);
        this.mPowerUsageFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mPowerUsageFeatureProvider.isSmartBatterySupported()) {
            return 2;
        }
        return 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((SwitchPreference) preference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "app_auto_restriction_enabled", 1) == 1);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "app_auto_restriction_enabled", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }
}
