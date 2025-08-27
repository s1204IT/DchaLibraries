package com.android.settings.gestures;

import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

/* loaded from: classes.dex */
public class DoubleTapPowerPreferenceController extends GesturePreferenceController {
    static final int OFF = 1;
    static final int ON = 0;
    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";
    private final String SECURE_KEY;
    private final String mDoubleTapPowerKey;

    public DoubleTapPowerPreferenceController(Context context, String str) {
        super(context, str);
        this.SECURE_KEY = "camera_double_tap_power_gesture_disabled";
        this.mDoubleTapPowerKey = str;
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences sharedPreferences) {
        return !isGestureAvailable(context) || sharedPreferences.getBoolean("pref_double_tap_power_suggestion_complete", false);
    }

    private static boolean isGestureAvailable(Context context) {
        return context.getResources().getBoolean(R.^attr-private.colorListDivider);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return isGestureAvailable(this.mContext) ? 0 : 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_double_tap_power");
    }

    @Override // com.android.settings.gestures.GesturePreferenceController
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "camera_double_tap_power_gesture_disabled", 0) == 0;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return Settings.Secure.putInt(this.mContext.getContentResolver(), "camera_double_tap_power_gesture_disabled", !z ? 1 : 0);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("camera_double_tap_power_gesture_disabled", 2, 0, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, DoubleTapPowerSettings.class.getName(), this.mDoubleTapPowerKey, this.mContext.getString(com.android.settings.R.string.display_settings)), isAvailable(), 0);
    }
}
