package com.android.settings.gestures;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

public class DoubleTapScreenPreferenceController extends GesturePreferenceController {
    private static final String PREF_KEY_VIDEO = "gesture_double_tap_screen_video";
    private final int OFF;
    private final int ON;
    private final String SECURE_KEY;
    private AmbientDisplayConfiguration mAmbientConfig;
    private final String mDoubleTapScreenPrefKey;
    private final int mUserId;

    public DoubleTapScreenPreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
        this.SECURE_KEY = "doze_pulse_on_double_tap";
        this.mUserId = UserHandle.myUserId();
        this.mDoubleTapScreenPrefKey = str;
    }

    public DoubleTapScreenPreferenceController setConfig(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        this.mAmbientConfig = ambientDisplayConfiguration;
        return this;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences sharedPreferences) {
        return isSuggestionComplete(new AmbientDisplayConfiguration(context), sharedPreferences);
    }

    static boolean isSuggestionComplete(AmbientDisplayConfiguration ambientDisplayConfiguration, SharedPreferences sharedPreferences) {
        return !ambientDisplayConfiguration.pulseOnDoubleTapAvailable() || sharedPreferences.getBoolean("pref_double_tap_screen_suggestion_complete", false);
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mAmbientConfig == null) {
            this.mAmbientConfig = new AmbientDisplayConfiguration(this.mContext);
        }
        if (!this.mAmbientConfig.doubleTapSensorAvailable()) {
            return 2;
        }
        if (!this.mAmbientConfig.ambientDisplayAvailable()) {
            return 4;
        }
        return 0;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_double_tap_screen");
    }

    @Override
    public boolean setChecked(boolean z) {
        return Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_pulse_on_double_tap", z ? 1 : 0);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return this.mAmbientConfig.pulseOnDoubleTapEnabled(this.mUserId);
    }

    @Override
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("doze_pulse_on_double_tap", 2, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, DoubleTapScreenSettings.class.getName(), this.mDoubleTapScreenPrefKey, this.mContext.getString(R.string.display_settings)), isAvailable(), 1);
    }

    @Override
    protected boolean canHandleClicks() {
        return !this.mAmbientConfig.alwaysOnEnabled(this.mUserId);
    }
}
