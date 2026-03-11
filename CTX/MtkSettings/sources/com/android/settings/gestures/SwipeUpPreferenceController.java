package com.android.settings.gestures;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;

public class SwipeUpPreferenceController extends GesturePreferenceController {
    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String PREF_KEY_VIDEO = "gesture_swipe_up_video";
    private final int OFF;
    private final int ON;
    private final UserManager mUserManager;

    public SwipeUpPreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    static boolean isGestureAvailable(Context context) {
        if (context.getResources().getBoolean(R.^attr-private.pointerIconHandwriting)) {
            return context.getPackageManager().resolveService(new Intent(ACTION_QUICKSTEP).setPackage(ComponentName.unflattenFromString(context.getString(R.string.app_blocked_message)).getPackageName()), 1048576) != null;
        }
        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(this.mContext) ? 0 : 2;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_swipe_up");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean z) {
        setSwipeUpPreference(this.mContext, this.mUserManager, z ? 1 : 0);
        return true;
    }

    public static void setSwipeUpPreference(Context context, UserManager userManager, int i) {
        Settings.Secure.putInt(context.getContentResolver(), "swipe_up_to_switch_apps_enabled", i);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "swipe_up_to_switch_apps_enabled", this.mContext.getResources().getBoolean(R.^attr-private.pointerIconHand) ? 1 : 0) != 0;
    }
}
