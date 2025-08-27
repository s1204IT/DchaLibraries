package com.android.settings.gestures;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class DoubleTwistPreferenceController extends GesturePreferenceController {
    private static final String PREF_KEY_VIDEO = "gesture_double_twist_video";
    private final int OFF;
    private final int ON;
    private final String mDoubleTwistPrefKey;
    private final UserManager mUserManager;

    public DoubleTwistPreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
        this.mDoubleTwistPrefKey = str;
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences sharedPreferences) {
        return !isGestureAvailable(context) || sharedPreferences.getBoolean("pref_double_twist_suggestion_complete", false);
    }

    public static boolean isGestureAvailable(Context context) throws Resources.NotFoundException {
        Resources resources = context.getResources();
        String string = resources.getString(R.string.gesture_double_twist_sensor_name);
        String string2 = resources.getString(R.string.gesture_double_twist_sensor_vendor);
        if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
            for (Sensor sensor : ((SensorManager) context.getSystemService("sensor")).getSensorList(-1)) {
                if (string.equals(sensor.getName()) && string2.equals(sensor.getVendor())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return isGestureAvailable(this.mContext) ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_double_twist");
    }

    @Override // com.android.settings.gestures.GesturePreferenceController
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mDoubleTwistPrefKey;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        setDoubleTwistPreference(this.mContext, this.mUserManager, z ? 1 : 0);
        return true;
    }

    public static void setDoubleTwistPreference(Context context, UserManager userManager, int i) {
        Settings.Secure.putInt(context.getContentResolver(), "camera_double_twist_to_flip_enabled", i);
        int managedProfileId = getManagedProfileId(userManager);
        if (managedProfileId != -10000) {
            Settings.Secure.putIntForUser(context.getContentResolver(), "camera_double_twist_to_flip_enabled", i, managedProfileId);
        }
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "camera_double_twist_to_flip_enabled", 1) != 0;
    }

    public static int getManagedProfileId(UserManager userManager) {
        return Utils.getManagedProfileId(userManager, UserHandle.myUserId());
    }
}
