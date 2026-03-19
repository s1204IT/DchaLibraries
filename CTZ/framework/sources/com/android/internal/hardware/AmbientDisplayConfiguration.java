package com.android.internal.hardware;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.R;

public class AmbientDisplayConfiguration {
    private final Context mContext;

    public AmbientDisplayConfiguration(Context context) {
        this.mContext = context;
    }

    public boolean enabled(int i) {
        return pulseOnNotificationEnabled(i) || pulseOnPickupEnabled(i) || pulseOnDoubleTapEnabled(i) || pulseOnLongPressEnabled(i) || alwaysOnEnabled(i);
    }

    public boolean available() {
        return pulseOnNotificationAvailable() || pulseOnPickupAvailable() || pulseOnDoubleTapAvailable();
    }

    public boolean pulseOnNotificationEnabled(int i) {
        return boolSettingDefaultOn(Settings.Secure.DOZE_ENABLED, i) && pulseOnNotificationAvailable();
    }

    public boolean pulseOnNotificationAvailable() {
        return ambientDisplayAvailable();
    }

    public boolean pulseOnPickupEnabled(int i) {
        return (boolSettingDefaultOn(Settings.Secure.DOZE_PULSE_ON_PICK_UP, i) || alwaysOnEnabled(i)) && pulseOnPickupAvailable();
    }

    public boolean pulseOnPickupAvailable() {
        return dozePulsePickupSensorAvailable() && ambientDisplayAvailable();
    }

    public boolean dozePulsePickupSensorAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_dozePulsePickup);
    }

    public boolean pulseOnPickupCanBeModified(int i) {
        return !alwaysOnEnabled(i);
    }

    public boolean pulseOnDoubleTapEnabled(int i) {
        return boolSettingDefaultOn(Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP, i) && pulseOnDoubleTapAvailable();
    }

    public boolean pulseOnDoubleTapAvailable() {
        return doubleTapSensorAvailable() && ambientDisplayAvailable();
    }

    public boolean doubleTapSensorAvailable() {
        return !TextUtils.isEmpty(doubleTapSensorType());
    }

    public String doubleTapSensorType() {
        return this.mContext.getResources().getString(R.string.config_dozeDoubleTapSensorType);
    }

    public String longPressSensorType() {
        return this.mContext.getResources().getString(R.string.config_dozeLongPressSensorType);
    }

    public boolean pulseOnLongPressEnabled(int i) {
        return pulseOnLongPressAvailable() && boolSettingDefaultOff(Settings.Secure.DOZE_PULSE_ON_LONG_PRESS, i);
    }

    private boolean pulseOnLongPressAvailable() {
        return !TextUtils.isEmpty(longPressSensorType());
    }

    public boolean alwaysOnEnabled(int i) {
        return boolSettingDefaultOn(Settings.Secure.DOZE_ALWAYS_ON, i) && alwaysOnAvailable() && !accessibilityInversionEnabled(i);
    }

    public boolean alwaysOnAvailable() {
        return (alwaysOnDisplayDebuggingEnabled() || alwaysOnDisplayAvailable()) && ambientDisplayAvailable();
    }

    public boolean alwaysOnAvailableForUser(int i) {
        return alwaysOnAvailable() && !accessibilityInversionEnabled(i);
    }

    public String ambientDisplayComponent() {
        return this.mContext.getResources().getString(R.string.config_dozeComponent);
    }

    public boolean accessibilityInversionEnabled(int i) {
        return boolSettingDefaultOff(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, i);
    }

    public boolean ambientDisplayAvailable() {
        return !TextUtils.isEmpty(ambientDisplayComponent());
    }

    private boolean alwaysOnDisplayAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_dozeAlwaysOnDisplayAvailable);
    }

    private boolean alwaysOnDisplayDebuggingEnabled() {
        return SystemProperties.getBoolean("debug.doze.aod", false) && Build.IS_DEBUGGABLE;
    }

    private boolean boolSettingDefaultOn(String str, int i) {
        return boolSetting(str, i, 1);
    }

    private boolean boolSettingDefaultOff(String str, int i) {
        return boolSetting(str, i, 0);
    }

    private boolean boolSetting(String str, int i, int i2) {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), str, i2, i) != 0;
    }
}
