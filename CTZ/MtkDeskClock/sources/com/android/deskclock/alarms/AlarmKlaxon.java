package com.android.deskclock.alarms;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.UserManager;
import android.os.Vibrator;
import com.android.deskclock.AsyncRingtonePlayer;
import com.android.deskclock.LogUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.AlarmInstance;

final class AlarmKlaxon {
    private static AsyncRingtonePlayer sAsyncRingtonePlayer;
    private static final long[] VIBRATE_PATTERN = {500, 500};
    private static boolean sStarted = false;

    private AlarmKlaxon() {
    }

    public static void stop(Context context) {
        if (sStarted) {
            LogUtils.v("AlarmKlaxon.stop()", new Object[0]);
            sStarted = false;
            getAsyncRingtonePlayer(context).stop();
            ((Vibrator) context.getSystemService("vibrator")).cancel();
        }
    }

    public static void start(Context context, AlarmInstance alarmInstance) {
        stop(context);
        LogUtils.v("AlarmKlaxon.start()", new Object[0]);
        if (!AlarmInstance.NO_RINGTONE_URI.equals(alarmInstance.mRingtone)) {
            long alarmCrescendoDuration = DataModel.getDataModel().getAlarmCrescendoDuration();
            if (UserManager.get(context.getApplicationContext()).isUserUnlocked()) {
                getAsyncRingtonePlayer(context).play(alarmInstance.mRingtone, alarmCrescendoDuration);
            } else {
                getAsyncRingtonePlayer(context).play(RingtoneManager.getDefaultUri(4), alarmCrescendoDuration);
            }
        }
        if (alarmInstance.mVibrate) {
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
