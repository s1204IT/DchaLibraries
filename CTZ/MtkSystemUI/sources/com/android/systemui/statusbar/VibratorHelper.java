package com.android.systemui.statusbar;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

public class VibratorHelper {
    private static final AudioAttributes STATUS_BAR_VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private final Context mContext;
    private boolean mHapticFeedbackEnabled;
    private final ContentObserver mVibrationObserver = new ContentObserver(Handler.getMain()) {
        @Override
        public void onChange(boolean z) {
            VibratorHelper.this.updateHapticFeedBackEnabled();
        }
    };
    private final Vibrator mVibrator;

    public VibratorHelper(Context context) {
        this.mContext = context;
        this.mVibrator = (Vibrator) context.getSystemService(Vibrator.class);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("haptic_feedback_enabled"), true, this.mVibrationObserver);
        this.mVibrationObserver.onChange(false);
    }

    public void vibrate(final int i) {
        if (this.mHapticFeedbackEnabled) {
            AsyncTask.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mVibrator.vibrate(VibrationEffect.get(i, false), VibratorHelper.STATUS_BAR_VIBRATION_ATTRIBUTES);
                }
            });
        }
    }

    private void updateHapticFeedBackEnabled() {
        this.mHapticFeedbackEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) != 0;
    }
}
