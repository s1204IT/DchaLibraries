package android.util;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;

public class FeatureFlagUtils {
    private static final Map<String, String> DEFAULT_FLAGS = new HashMap();
    public static final String FFLAG_OVERRIDE_PREFIX = "sys.fflag.override.";
    public static final String FFLAG_PREFIX = "sys.fflag.";

    static {
        DEFAULT_FLAGS.put("settings_battery_display_app_list", "false");
        DEFAULT_FLAGS.put("settings_zone_picker_v2", "false");
        DEFAULT_FLAGS.put("settings_about_phone_v2", "false");
        DEFAULT_FLAGS.put("settings_bluetooth_while_driving", "false");
        DEFAULT_FLAGS.put("settings_data_usage_v2", "false");
        DEFAULT_FLAGS.put("settings_audio_switcher", "false");
        DEFAULT_FLAGS.put("settings_systemui_theme", "false");
    }

    public static boolean isEnabled(Context context, String str) {
        if (context != null) {
            String string = Settings.Global.getString(context.getContentResolver(), str);
            if (!TextUtils.isEmpty(string)) {
                return Boolean.parseBoolean(string);
            }
        }
        String str2 = SystemProperties.get(FFLAG_OVERRIDE_PREFIX + str);
        if (!TextUtils.isEmpty(str2)) {
            return Boolean.parseBoolean(str2);
        }
        return Boolean.parseBoolean(getAllFeatureFlags().get(str));
    }

    public static void setEnabled(Context context, String str, boolean z) {
        SystemProperties.set(FFLAG_OVERRIDE_PREFIX + str, z ? "true" : "false");
    }

    public static Map<String, String> getAllFeatureFlags() {
        return DEFAULT_FLAGS;
    }
}
