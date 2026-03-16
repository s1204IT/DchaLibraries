package com.android.datetimepicker;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;

public class HapticFeedbackController {
    private final ContentObserver mContentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
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
        Uri uri = Settings.System.getUriFor("haptic_feedback_enabled");
        this.mContext.getContentResolver().registerContentObserver(uri, false, this.mContentObserver);
    }

    public void stop() {
        this.mVibrator = null;
        this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    public void tryVibrate() {
        if (this.mVibrator != null && this.mIsGloballyEnabled) {
            long now = SystemClock.uptimeMillis();
            if (now - this.mLastVibrate >= 125) {
                this.mVibrator.vibrate(5L);
                this.mLastVibrate = now;
            }
        }
    }
}
