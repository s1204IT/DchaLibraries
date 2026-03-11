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

public class PickupGesturePreferenceController extends GesturePreferenceController {
    private static final String PREF_KEY_VIDEO = "gesture_pick_up_video";
    private final int OFF;
    private final int ON;
    private final String SECURE_KEY;
    private AmbientDisplayConfiguration mAmbientConfig;
    private final String mPickUpPrefKey;
    private final int mUserId;

    public PickupGesturePreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
        this.SECURE_KEY = "doze_pulse_on_pick_up";
        this.mUserId = UserHandle.myUserId();
        this.mPickUpPrefKey = str;
    }

    public PickupGesturePreferenceController setConfig(AmbientDisplayConfiguration ambientDisplayConfiguration) {
        this.mAmbientConfig = ambientDisplayConfiguration;
        return this;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean("pref_pickup_gesture_suggestion_complete", false) || !new AmbientDisplayConfiguration(context).pulseOnPickupAvailable();
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mAmbientConfig == null) {
            this.mAmbientConfig = new AmbientDisplayConfiguration(this.mContext);
        }
        if (!this.mAmbientConfig.dozePulsePickupSensorAvailable()) {
            return 2;
        }
        if (!this.mAmbientConfig.ambientDisplayAvailable()) {
            return 4;
        }
        return 0;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_pick_up");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return this.mAmbientConfig.pulseOnPickupEnabled(this.mUserId);
    }

    @Override
    public String getPreferenceKey() {
        return this.mPickUpPrefKey;
    }

    @Override
    public boolean setChecked(boolean z) {
        return Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_pulse_on_pick_up", z ? 1 : 0);
    }

    @Override
    public boolean canHandleClicks() {
        return pulseOnPickupCanBeModified();
    }

    @Override
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("doze_pulse_on_pick_up", 2, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, PickupGestureSettings.class.getName(), this.mPickUpPrefKey, this.mContext.getString(R.string.display_settings)), isAvailable(), 1);
    }

    boolean pulseOnPickupCanBeModified() {
        return this.mAmbientConfig.pulseOnPickupCanBeModified(this.mUserId);
    }
}
