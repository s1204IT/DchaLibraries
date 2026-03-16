package com.android.deskclock;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import java.io.IOException;

public class TimerRingService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private int mInitialCallState;
    private MediaPlayer mMediaPlayer;
    private TelephonyManager mTelephonyManager;
    private boolean mPlaying = false;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            if (state != 0 && state != TimerRingService.this.mInitialCallState) {
                TimerRingService.this.stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        AlarmAlertWakeLock.acquireScreenCpuWakeLock(this);
    }

    @Override
    public void onDestroy() {
        stop();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return 2;
        }
        play();
        this.mInitialCallState = this.mTelephonyManager.getCallState();
        return 1;
    }

    private void play() {
        if (!this.mPlaying) {
            LogUtils.v("TimerRingService.play()", new Object[0]);
            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    LogUtils.e("Error occurred while playing audio.", new Object[0]);
                    mp.stop();
                    mp.release();
                    TimerRingService.this.mMediaPlayer = null;
                    return true;
                }
            });
            try {
                if (this.mTelephonyManager.getCallState() != 0) {
                    LogUtils.v("Using the in-call alarm", new Object[0]);
                    this.mMediaPlayer.setVolume(0.125f, 0.125f);
                    setDataSourceFromResource(getResources(), this.mMediaPlayer, R.raw.in_call_alarm);
                } else {
                    AssetFileDescriptor afd = getAssets().openFd("sounds/Timer_Expire.ogg");
                    this.mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                }
                startAlarm(this.mMediaPlayer);
            } catch (Exception e) {
                LogUtils.v("Using the fallback ringtone", new Object[0]);
                try {
                    this.mMediaPlayer.reset();
                    setDataSourceFromResource(getResources(), this.mMediaPlayer, R.raw.fallbackring);
                    startAlarm(this.mMediaPlayer);
                } catch (Exception ex2) {
                    LogUtils.e("Failed to play fallback ringtone", ex2);
                }
            }
            this.mPlaying = true;
        }
    }

    private void startAlarm(MediaPlayer player) throws IllegalStateException, IOException, IllegalArgumentException {
        AudioManager audioManager = (AudioManager) getSystemService("audio");
        if (audioManager.getStreamVolume(4) != 0) {
            player.setAudioStreamType(4);
            player.setLooping(true);
            player.prepare();
            audioManager.requestAudioFocus(this, 4, 2);
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resources, MediaPlayer player, int res) throws IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    public void stop() {
        LogUtils.v("TimerRingService.stop()", new Object[0]);
        if (this.mPlaying) {
            this.mPlaying = false;
            if (this.mMediaPlayer != null) {
                this.mMediaPlayer.stop();
                AudioManager audioManager = (AudioManager) getSystemService("audio");
                audioManager.abandonAudioFocus(this);
                this.mMediaPlayer.release();
                this.mMediaPlayer = null;
            }
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
    }
}
