package android.os;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.media.AudioAttributes;
import android.util.Log;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class Vibrator {
    private static final String TAG = "Vibrator";
    public static final int VIBRATION_INTENSITY_HIGH = 3;
    public static final int VIBRATION_INTENSITY_LOW = 1;
    public static final int VIBRATION_INTENSITY_MEDIUM = 2;
    public static final int VIBRATION_INTENSITY_OFF = 0;
    private final int mDefaultHapticFeedbackIntensity;
    private final int mDefaultNotificationVibrationIntensity;
    private final String mPackageName;

    @Retention(RetentionPolicy.SOURCE)
    public @interface VibrationIntensity {
    }

    public abstract void cancel();

    public abstract boolean hasAmplitudeControl();

    public abstract boolean hasVibrator();

    public abstract void vibrate(int i, String str, VibrationEffect vibrationEffect, AudioAttributes audioAttributes);

    public Vibrator() {
        this.mPackageName = ActivityThread.currentPackageName();
        ContextImpl systemContext = ActivityThread.currentActivityThread().getSystemContext();
        this.mDefaultHapticFeedbackIntensity = loadDefaultIntensity(systemContext, R.integer.config_defaultHapticFeedbackIntensity);
        this.mDefaultNotificationVibrationIntensity = loadDefaultIntensity(systemContext, R.integer.config_defaultNotificationVibrationIntensity);
    }

    protected Vibrator(Context context) {
        this.mPackageName = context.getOpPackageName();
        this.mDefaultHapticFeedbackIntensity = loadDefaultIntensity(context, R.integer.config_defaultHapticFeedbackIntensity);
        this.mDefaultNotificationVibrationIntensity = loadDefaultIntensity(context, R.integer.config_defaultNotificationVibrationIntensity);
    }

    private int loadDefaultIntensity(Context context, int i) {
        if (context != null) {
            return context.getResources().getInteger(i);
        }
        return 2;
    }

    public int getDefaultHapticFeedbackIntensity() {
        return this.mDefaultHapticFeedbackIntensity;
    }

    public int getDefaultNotificationVibrationIntensity() {
        return this.mDefaultNotificationVibrationIntensity;
    }

    @Deprecated
    public void vibrate(long j) {
        vibrate(j, (AudioAttributes) null);
    }

    @Deprecated
    public void vibrate(long j, AudioAttributes audioAttributes) {
        try {
            vibrate(VibrationEffect.createOneShot(j, -1), audioAttributes);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to create VibrationEffect", e);
        }
    }

    @Deprecated
    public void vibrate(long[] jArr, int i) {
        vibrate(jArr, i, null);
    }

    @Deprecated
    public void vibrate(long[] jArr, int i, AudioAttributes audioAttributes) {
        if (i < -1 || i >= jArr.length) {
            Log.e(TAG, "vibrate called with repeat index out of bounds (pattern.length=" + jArr.length + ", index=" + i + ")");
            throw new ArrayIndexOutOfBoundsException();
        }
        try {
            vibrate(VibrationEffect.createWaveform(jArr, i), audioAttributes);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to create VibrationEffect", e);
        }
    }

    public void vibrate(VibrationEffect vibrationEffect) {
        vibrate(vibrationEffect, (AudioAttributes) null);
    }

    public void vibrate(VibrationEffect vibrationEffect, AudioAttributes audioAttributes) {
        vibrate(Process.myUid(), this.mPackageName, vibrationEffect, audioAttributes);
    }
}
