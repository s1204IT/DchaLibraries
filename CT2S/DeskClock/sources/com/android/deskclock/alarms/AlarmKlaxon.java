package com.android.deskclock.alarms;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.AlarmInstance;
import java.io.IOException;

public class AlarmKlaxon {
    private static final long[] sVibratePattern = {500, 500};
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(4).build();
    private static boolean sStarted = false;
    private static MediaPlayer sMediaPlayer = null;

    public static void stop(Context context) {
        LogUtils.v("AlarmKlaxon.stop()", new Object[0]);
        if (sStarted) {
            sStarted = false;
            if (sMediaPlayer != null) {
                sMediaPlayer.stop();
                AudioManager audioManager = (AudioManager) context.getSystemService("audio");
                audioManager.abandonAudioFocus(null);
                sMediaPlayer.release();
                sMediaPlayer = null;
            }
            ((Vibrator) context.getSystemService("vibrator")).cancel();
        }
    }

    public static void start(final Context context, AlarmInstance instance, boolean inTelephoneCall) {
        LogUtils.v("AlarmKlaxon.start()", new Object[0]);
        stop(context);
        if (!AlarmInstance.NO_RINGTONE_URI.equals(instance.mRingtone)) {
            Uri alarmNoise = instance.mRingtone;
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(4);
                LogUtils.v("Using default alarm: " + alarmNoise.toString(), new Object[0]);
            }
            sMediaPlayer = new MediaPlayer();
            sMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    LogUtils.e("Error occurred while playing audio. Stopping AlarmKlaxon.", new Object[0]);
                    AlarmKlaxon.stop(context);
                    return true;
                }
            });
            try {
                if (inTelephoneCall) {
                    LogUtils.v("Using the in-call alarm", new Object[0]);
                    sMediaPlayer.setVolume(0.125f, 0.125f);
                    setDataSourceFromResource(context, sMediaPlayer, R.raw.in_call_alarm);
                } else {
                    sMediaPlayer.setDataSource(context, alarmNoise);
                }
                startAlarm(context, sMediaPlayer);
            } catch (Exception e) {
                LogUtils.v("Using the fallback ringtone", new Object[0]);
                try {
                    sMediaPlayer.reset();
                    setDataSourceFromResource(context, sMediaPlayer, R.raw.fallbackring);
                    startAlarm(context, sMediaPlayer);
                } catch (Exception ex2) {
                    LogUtils.e("Failed to play fallback ringtone", ex2);
                }
            }
        }
        if (instance.mVibrate) {
            Vibrator vibrator = (Vibrator) context.getSystemService("vibrator");
            vibrator.vibrate(sVibratePattern, 0, VIBRATION_ATTRIBUTES);
        }
        sStarted = true;
    }

    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService("audio");
        if (audioManager.getStreamVolume(4) != 0) {
            player.setAudioStreamType(4);
            player.setLooping(true);
            player.prepare();
            audioManager.requestAudioFocus(null, 4, 2);
            player.start();
        }
    }

    private static void setDataSourceFromResource(Context context, MediaPlayer player, int res) throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }
}
