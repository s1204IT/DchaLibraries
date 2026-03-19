package com.android.datetimepicker;

import android.content.Context;
import android.database.ContentObserver;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;

public class HapticFeedbackController {
    private final ContentObserver mContentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean z) {
            HapticFeedbackController.this.mIsGloballyEnabled = HapticFeedbackController.checkGlobalSetting(HapticFeedbackController.this.mContext);
        }
    };
    private final Context mContext;
    private boolean mIsGloballyEnabled;
    private long mLastVibrate;
    private Vibrator mVibrator;

    private static boolean checkGlobalSetting(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "haptic_feedback_enabled", 0) == 1;
    }

    public HapticFeedbackController(Context context) {
        this.mContext = context;
    }

    public void start() {
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mIsGloballyEnabled = checkGlobalSetting(this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("haptic_feedback_enabled"), false, this.mContentObserver);
    }

    public void stop() {
        this.mVibrator = null;
        this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    public void tryVibrate() {
        if (this.mVibrator != null && this.mIsGloballyEnabled) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis - this.mLastVibrate >= 125) {
                this.mVibrator.vibrate(5L);
                this.mLastVibrate = jUptimeMillis;
            }
        }
    }
}
