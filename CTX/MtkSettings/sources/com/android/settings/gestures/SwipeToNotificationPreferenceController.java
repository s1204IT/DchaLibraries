package com.android.settings.gestures;

import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.Utils;

public class SwipeToNotificationPreferenceController extends GesturePreferenceController {
    private static final int OFF = 0;
    private static final int ON = 1;
    private static final String PREF_KEY_VIDEO = "gesture_swipe_down_fingerprint_video";
    private static final String SECURE_KEY = "system_navigation_keys_enabled";

    public SwipeToNotificationPreferenceController(Context context, String str) {
        super(context, str);
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences sharedPreferences) {
        return !isGestureAvailable(context) || sharedPreferences.getBoolean("pref_swipe_to_notification_suggestion_complete", false);
    }

    private static boolean isGestureAvailable(Context context) {
        return Utils.hasFingerprintHardware(context) && context.getResources().getBoolean(R.^attr-private.pointerIconArrow);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(this.mContext) ? 0 : 2;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_swipe_down_fingerprint");
    }

    @Override
    public boolean setChecked(boolean z) {
        setSwipeToNotification(this.mContext, z);
        return true;
    }

    @Override
    public boolean isChecked() {
        return isSwipeToNotificationOn(this.mContext);
    }

    public static boolean isSwipeToNotificationOn(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SECURE_KEY, 0) == 1;
    }

    public static boolean setSwipeToNotification(Context context, boolean z) {
        return Settings.Secure.putInt(context.getContentResolver(), SECURE_KEY, z ? 1 : 0);
    }

    public static boolean isAvailable(Context context) {
        return isGestureAvailable(context);
    }
}
