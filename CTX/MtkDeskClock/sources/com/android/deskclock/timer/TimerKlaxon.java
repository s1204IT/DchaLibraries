package com.android.deskclock.timer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Vibrator;
import com.android.deskclock.AsyncRingtonePlayer;
import com.android.deskclock.LogUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

public abstract class TimerKlaxon {
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;
    private static final long[] VIBRATE_PATTERN = {500, 500};
    private static boolean sStarted = false;

    private TimerKlaxon() {
    }

    public static void stop(Context context) {
        if (sStarted) {
            LogUtils.i("TimerKlaxon.stop()", new Object[0]);
            sStarted = false;
            getAsyncRingtonePlayer(context).stop();
            ((Vibrator) context.getSystemService("vibrator")).cancel();
        }
    }

    public static void start(Context context) {
        stop(context);
        LogUtils.i("TimerKlaxon.start()", new Object[0]);
        if (DataModel.getDataModel().isTimerRingtoneSilent()) {
            LogUtils.i("Playing silent ringtone for timer", new Object[0]);
        } else {
            getAsyncRingtonePlayer(context).play(DataModel.getDataModel().getTimerRingtoneUri(), DataModel.getDataModel().getTimerCrescendoDuration());
        }
        if (DataModel.getDataModel().getTimerVibrate()) {
            Vibrator vibrator = getVibrator(context);
            if (Utils.isLOrLater()) {
                vibrateLOrLater(vibrator);
            } else {
                vibrator.vibrate(VIBRATE_PATTERN, 0);
            }
        }
        sStarted = true;
    }

    @TargetApi(21)
    private static void vibrateLOrLater(Vibrator vibrator) {
        vibrator.vibrate(VIBRATE_PATTERN, 0, new AudioAttributes.Builder().setUsage(4).setContentType(4).build());
    }

    private static Vibrator getVibrator(Context context) {
        return (Vibrator) context.getSystemService("vibrator");
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }
        return sAsyncRingtonePlayer;
    }
}
