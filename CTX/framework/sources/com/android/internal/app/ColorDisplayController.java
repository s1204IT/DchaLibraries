package com.android.internal.app;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class ColorDisplayController {
    public static final int AUTO_MODE_CUSTOM = 1;
    public static final int AUTO_MODE_DISABLED = 0;
    public static final int AUTO_MODE_TWILIGHT = 2;
    public static final int COLOR_MODE_AUTOMATIC = 3;
    public static final int COLOR_MODE_BOOSTED = 1;
    public static final int COLOR_MODE_NATURAL = 0;
    public static final int COLOR_MODE_SATURATED = 2;
    private static final boolean DEBUG = false;
    private static final String TAG = "ColorDisplayController";
    private Callback mCallback;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private MetricsLogger mMetricsLogger;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AutoMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorMode {
    }

    public ColorDisplayController(Context context) {
        this(context, ActivityManager.getCurrentUser());
    }

    public ColorDisplayController(Context context, int i) {
        this.mContext = context.getApplicationContext();
        this.mUserId = i;
        this.mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean z, Uri uri) {
                super.onChange(z, uri);
                String lastPathSegment = uri == null ? null : uri.getLastPathSegment();
                if (lastPathSegment != null) {
                    ColorDisplayController.this.onSettingChanged(lastPathSegment);
                }
            }
        };
    }

    public boolean isActivated() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_ACTIVATED, 0, this.mUserId) == 1;
    }

    public boolean setActivated(boolean z) {
        if (isActivated() != z) {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME, LocalDateTime.now().toString(), this.mUserId);
        }
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_ACTIVATED, z ? 1 : 0, this.mUserId);
    }

    public LocalDateTime getLastActivatedTime() {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME, this.mUserId);
        if (stringForUser != null) {
            try {
                return LocalDateTime.parse(stringForUser);
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(stringForUser)), ZoneId.systemDefault());
                } catch (NumberFormatException | DateTimeException e2) {
                    return null;
                }
            }
        }
        return null;
    }

    public int getAutoMode() {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_AUTO_MODE, -1, this.mUserId);
        if (intForUser == -1) {
            intForUser = this.mContext.getResources().getInteger(R.integer.config_defaultNightDisplayAutoMode);
        }
        if (intForUser != 0 && intForUser != 1 && intForUser != 2) {
            Slog.e(TAG, "Invalid autoMode: " + intForUser);
            return 0;
        }
        return intForUser;
    }

    public int getAutoModeRaw() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_AUTO_MODE, -1, this.mUserId);
    }

    public boolean setAutoMode(int i) {
        if (i != 0 && i != 1 && i != 2) {
            throw new IllegalArgumentException("Invalid autoMode: " + i);
        }
        if (getAutoMode() != i) {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME, null, this.mUserId);
            getMetricsLogger().write(new LogMaker(MetricsProto.MetricsEvent.ACTION_NIGHT_DISPLAY_AUTO_MODE_CHANGED).setType(4).setSubtype(i));
        }
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_AUTO_MODE, i, this.mUserId);
    }

    public LocalTime getCustomStartTime() {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_CUSTOM_START_TIME, -1, this.mUserId);
        if (intForUser == -1) {
            intForUser = this.mContext.getResources().getInteger(R.integer.config_defaultNightDisplayCustomStartTime);
        }
        return LocalTime.ofSecondOfDay(intForUser / 1000);
    }

    public boolean setCustomStartTime(LocalTime localTime) {
        if (localTime == null) {
            throw new IllegalArgumentException("startTime cannot be null");
        }
        getMetricsLogger().write(new LogMaker(1310).setType(4).setSubtype(0));
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_CUSTOM_START_TIME, localTime.toSecondOfDay() * 1000, this.mUserId);
    }

    public LocalTime getCustomEndTime() {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_CUSTOM_END_TIME, -1, this.mUserId);
        if (intForUser == -1) {
            intForUser = this.mContext.getResources().getInteger(R.integer.config_defaultNightDisplayCustomEndTime);
        }
        return LocalTime.ofSecondOfDay(intForUser / 1000);
    }

    public boolean setCustomEndTime(LocalTime localTime) {
        if (localTime == null) {
            throw new IllegalArgumentException("endTime cannot be null");
        }
        getMetricsLogger().write(new LogMaker(1310).setType(4).setSubtype(1));
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_CUSTOM_END_TIME, localTime.toSecondOfDay() * 1000, this.mUserId);
    }

    public int getColorTemperature() {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE, -1, this.mUserId);
        if (intForUser == -1) {
            intForUser = getDefaultColorTemperature();
        }
        int minimumColorTemperature = getMinimumColorTemperature();
        int maximumColorTemperature = getMaximumColorTemperature();
        if (intForUser < minimumColorTemperature) {
            return minimumColorTemperature;
        }
        return intForUser > maximumColorTemperature ? maximumColorTemperature : intForUser;
    }

    public boolean setColorTemperature(int i) {
        return Settings.Secure.putIntForUser(this.mContext.getContentResolver(), Settings.Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE, i, this.mUserId);
    }

    private int getCurrentColorModeFromSystemProperties() {
        int i = SystemProperties.getInt("persist.sys.sf.native_mode", 0);
        if (i == 0) {
            return "1.0".equals(SystemProperties.get("persist.sys.sf.color_saturation")) ? 0 : 1;
        }
        if (i == 1) {
            return 2;
        }
        if (i == 2) {
            return 3;
        }
        return -1;
    }

    private boolean isColorModeAvailable(int i) {
        int[] intArray = this.mContext.getResources().getIntArray(R.array.config_availableColorModes);
        if (intArray != null) {
            for (int i2 : intArray) {
                if (i2 == i) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getColorMode() {
        if (getAccessibilityTransformActivated()) {
            if (isColorModeAvailable(2)) {
                return 2;
            }
            if (isColorModeAvailable(3)) {
                return 3;
            }
        }
        int intForUser = Settings.System.getIntForUser(this.mContext.getContentResolver(), Settings.System.DISPLAY_COLOR_MODE, -1, this.mUserId);
        if (intForUser == -1) {
            intForUser = getCurrentColorModeFromSystemProperties();
        }
        if (isColorModeAvailable(intForUser)) {
            return intForUser;
        }
        if (intForUser == 1 && isColorModeAvailable(0)) {
            return 0;
        }
        if (intForUser == 2 && isColorModeAvailable(3)) {
            return 3;
        }
        return (intForUser == 3 && isColorModeAvailable(2)) ? 2 : -1;
    }

    public void setColorMode(int i) {
        if (!isColorModeAvailable(i)) {
            throw new IllegalArgumentException("Invalid colorMode: " + i);
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), Settings.System.DISPLAY_COLOR_MODE, i, this.mUserId);
    }

    public int getMinimumColorTemperature() {
        return this.mContext.getResources().getInteger(R.integer.config_nightDisplayColorTemperatureMin);
    }

    public int getMaximumColorTemperature() {
        return this.mContext.getResources().getInteger(R.integer.config_nightDisplayColorTemperatureMax);
    }

    public int getDefaultColorTemperature() {
        return this.mContext.getResources().getInteger(R.integer.config_nightDisplayColorTemperatureDefault);
    }

    public boolean getAccessibilityTransformActivated() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        return Settings.Secure.getIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0, this.mUserId) == 1 || Settings.Secure.getIntForUser(contentResolver, Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0, this.mUserId) == 1;
    }

    private void onSettingChanged(String str) {
        if (this.mCallback != null) {
            switch (str) {
                case "night_display_activated":
                    this.mCallback.onActivated(isActivated());
                    break;
                case "night_display_auto_mode":
                    this.mCallback.onAutoModeChanged(getAutoMode());
                    break;
                case "night_display_custom_start_time":
                    this.mCallback.onCustomStartTimeChanged(getCustomStartTime());
                    break;
                case "night_display_custom_end_time":
                    this.mCallback.onCustomEndTimeChanged(getCustomEndTime());
                    break;
                case "night_display_color_temperature":
                    this.mCallback.onColorTemperatureChanged(getColorTemperature());
                    break;
                case "display_color_mode":
                    this.mCallback.onDisplayColorModeChanged(getColorMode());
                    break;
                case "accessibility_display_inversion_enabled":
                case "accessibility_display_daltonizer_enabled":
                    this.mCallback.onAccessibilityTransformChanged(getAccessibilityTransformActivated());
                    break;
            }
        }
    }

    public void setListener(Callback callback) {
        Callback callback2 = this.mCallback;
        if (callback2 != callback) {
            this.mCallback = callback;
            if (callback == null) {
                this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
                return;
            }
            if (callback2 == null) {
                ContentResolver contentResolver = this.mContext.getContentResolver();
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_ACTIVATED), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_AUTO_MODE), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_CUSTOM_START_TIME), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_CUSTOM_END_TIME), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.DISPLAY_COLOR_MODE), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED), false, this.mContentObserver, this.mUserId);
                contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED), false, this.mContentObserver, this.mUserId);
            }
        }
    }

    private MetricsLogger getMetricsLogger() {
        if (this.mMetricsLogger == null) {
            this.mMetricsLogger = new MetricsLogger();
        }
        return this.mMetricsLogger;
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(R.bool.config_nightDisplayAvailable);
    }

    public interface Callback {
        default void onActivated(boolean z) {
        }

        default void onAutoModeChanged(int i) {
        }

        default void onCustomStartTimeChanged(LocalTime localTime) {
        }

        default void onCustomEndTimeChanged(LocalTime localTime) {
        }

        default void onColorTemperatureChanged(int i) {
        }

        default void onDisplayColorModeChanged(int i) {
        }

        default void onAccessibilityTransformChanged(boolean z) {
        }
    }
}
