package com.android.settings.display;

import android.R;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class CameraGesturePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public CameraGesturePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "camera_gesture";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "camera_gesture_disabled", 0) == 0);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (this.mContext.getResources().getInteger(R.integer.config_autoBrightnessShortTermModelTimeout) != -1) && !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "camera_gesture_disabled", !((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }
}
