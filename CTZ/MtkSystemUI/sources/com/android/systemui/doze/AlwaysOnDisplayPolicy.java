package com.android.systemui.doze;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.systemui.R;

public class AlwaysOnDisplayPolicy {
    public int[] dimmingScrimArray;
    private final Context mContext;
    private final KeyValueListParser mParser;
    private SettingsObserver mSettingsObserver;
    public long proxCooldownPeriodMs;
    public long proxCooldownTriggerMs;
    public long proxScreenOffDelayMs;
    public int[] screenBrightnessArray;
    public long wallpaperFadeOutDuration;
    public long wallpaperVisibilityDuration;

    public AlwaysOnDisplayPolicy(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.mContext = applicationContext;
        this.mParser = new KeyValueListParser(',');
        this.mSettingsObserver = new SettingsObserver(applicationContext.getMainThreadHandler());
        this.mSettingsObserver.observe();
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ALWAYS_ON_DISPLAY_CONSTANTS_URI;

        SettingsObserver(Handler handler) {
            super(handler);
            this.ALWAYS_ON_DISPLAY_CONSTANTS_URI = Settings.Global.getUriFor("always_on_display_constants");
        }

        void observe() {
            AlwaysOnDisplayPolicy.this.mContext.getContentResolver().registerContentObserver(this.ALWAYS_ON_DISPLAY_CONSTANTS_URI, false, this, -1);
            update(null);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (uri == null || this.ALWAYS_ON_DISPLAY_CONSTANTS_URI.equals(uri)) {
                Resources resources = AlwaysOnDisplayPolicy.this.mContext.getResources();
                try {
                    AlwaysOnDisplayPolicy.this.mParser.setString(Settings.Global.getString(AlwaysOnDisplayPolicy.this.mContext.getContentResolver(), "always_on_display_constants"));
                } catch (IllegalArgumentException e) {
                    Log.e("AlwaysOnDisplayPolicy", "Bad AOD constants");
                }
                AlwaysOnDisplayPolicy.this.proxScreenOffDelayMs = AlwaysOnDisplayPolicy.this.mParser.getLong("prox_screen_off_delay", 10000L);
                AlwaysOnDisplayPolicy.this.proxCooldownTriggerMs = AlwaysOnDisplayPolicy.this.mParser.getLong("prox_cooldown_trigger", 2000L);
                AlwaysOnDisplayPolicy.this.proxCooldownPeriodMs = AlwaysOnDisplayPolicy.this.mParser.getLong("prox_cooldown_period", 5000L);
                AlwaysOnDisplayPolicy.this.wallpaperFadeOutDuration = AlwaysOnDisplayPolicy.this.mParser.getLong("wallpaper_fade_out_duration", 400L);
                AlwaysOnDisplayPolicy.this.wallpaperVisibilityDuration = AlwaysOnDisplayPolicy.this.mParser.getLong("wallpaper_visibility_timeout", 60000L);
                AlwaysOnDisplayPolicy.this.screenBrightnessArray = AlwaysOnDisplayPolicy.this.mParser.getIntArray("screen_brightness_array", resources.getIntArray(R.array.config_doze_brightness_sensor_to_brightness));
                AlwaysOnDisplayPolicy.this.dimmingScrimArray = AlwaysOnDisplayPolicy.this.mParser.getIntArray("dimming_scrim_array", resources.getIntArray(R.array.config_doze_brightness_sensor_to_scrim_opacity));
            }
        }
    }
}
