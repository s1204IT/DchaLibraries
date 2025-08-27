package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class CameraLaserSensorPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String BUILD_TYPE = "ro.build.type";

    @VisibleForTesting
    static final int DISABLED = 2;

    @VisibleForTesting
    static final int ENABLED = 0;

    @VisibleForTesting
    static final String ENG_BUILD = "eng";

    @VisibleForTesting
    static final String PROPERTY_CAMERA_LASER_SENSOR = "persist.camera.stats.disablehaf";

    @VisibleForTesting
    static final String USERDEBUG_BUILD = "userdebug";

    @VisibleForTesting
    static final String USER_BUILD = "user";

    public CameraLaserSensorPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "camera_laser_sensor_switch";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(PROPERTY_CAMERA_LASER_SENSOR, Integer.toString(((Boolean) obj).booleanValue() ? 0 : 2));
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(isLaserSensorEnabled());
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(PROPERTY_CAMERA_LASER_SENSOR, Integer.toString(2));
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    private boolean isLaserSensorEnabled() {
        return TextUtils.equals(Integer.toString(0), SystemProperties.get(PROPERTY_CAMERA_LASER_SENSOR, Integer.toString(0)));
    }
}
