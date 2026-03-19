package com.android.deskclock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import com.android.deskclock.LogUtils;
import java.io.IOException;
import java.lang.reflect.Method;

public final class AsyncRingtonePlayer {
    private static final String CRESCENDO_DURATION_KEY = "CRESCENDO_DURATION_KEY";
    private static final int EVENT_PLAY = 1;
    private static final int EVENT_STOP = 2;
    private static final int EVENT_VOLUME = 3;
    private static final float IN_CALL_VOLUME = 0.125f;
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AsyncRingtonePlayer");
    private static final String RINGTONE_URI_KEY = "RINGTONE_URI_KEY";
    private final Context mContext;
    private Handler mHandler;
    private PlaybackDelegate mPlaybackDelegate;

    private interface PlaybackDelegate {
        boolean adjustVolume(Context context);

        boolean play(Context context, Uri uri, long j);

        void stop(Context context);
    }

    public AsyncRingtonePlayer(Context context) {
        this.mContext = context;
    }

    public void play(Uri uri, long j) {
        LOGGER.d("Posting play.", new Object[0]);
        postMessage(1, uri, j, 0L);
    }

    public void stop() {
        LOGGER.d("Posting stop.", new Object[0]);
        postMessage(2, null, 0L, 0L);
    }

    private void scheduleVolumeAdjustment() {
        LOGGER.v("Adjusting volume.", new Object[0]);
        this.mHandler.removeMessages(3);
        postMessage(3, null, 0L, 50L);
    }

    private void postMessage(int i, Uri uri, long j, long j2) {
        synchronized (this) {
            if (this.mHandler == null) {
                this.mHandler = getNewHandler();
            }
            Message messageObtainMessage = this.mHandler.obtainMessage(i);
            if (uri != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(RINGTONE_URI_KEY, uri);
                bundle.putLong(CRESCENDO_DURATION_KEY, j);
                messageObtainMessage.setData(bundle);
            }
            this.mHandler.sendMessageDelayed(messageObtainMessage, j2);
        }
    }

    @SuppressLint({"HandlerLeak"})
    private Handler getNewHandler() {
        HandlerThread handlerThread = new HandlerThread("ringtone-player");
        handlerThread.start();
        return new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        Bundle data = message.getData();
                        if (AsyncRingtonePlayer.this.getPlaybackDelegate().play(AsyncRingtonePlayer.this.mContext, (Uri) data.getParcelable(AsyncRingtonePlayer.RINGTONE_URI_KEY), data.getLong(AsyncRingtonePlayer.CRESCENDO_DURATION_KEY))) {
                            AsyncRingtonePlayer.this.scheduleVolumeAdjustment();
                        }
                        break;
                    case 2:
                        AsyncRingtonePlayer.this.getPlaybackDelegate().stop(AsyncRingtonePlayer.this.mContext);
                        break;
                    case 3:
                        if (AsyncRingtonePlayer.this.getPlaybackDelegate().adjustVolume(AsyncRingtonePlayer.this.mContext)) {
                            AsyncRingtonePlayer.this.scheduleVolumeAdjustment();
                        }
                        break;
                }
            }
        };
    }

    private static boolean isInTelephoneCall(Context context) {
        return ((TelephonyManager) context.getSystemService("phone")).getCallState() != 0;
    }

    private static Uri getInCallRingtoneUri(Context context) {
        return Utils.getResourceUri(context, R.raw.alarm_expire);
    }

    private static Uri getFallbackRingtoneUri(Context context) {
        return Utils.getResourceUri(context, R.raw.alarm_expire);
    }

    private void checkAsyncRingtonePlayerThread() {
        if (Looper.myLooper() != this.mHandler.getLooper()) {
            LOGGER.e("Must be on the AsyncRingtonePlayer thread!", new IllegalStateException());
        }
    }

    private static float computeVolume(long j, long j2, long j3) {
        float f = 1.0f - ((j2 - j) / j3);
        float fPow = (float) Math.pow(10.0d, r4 / 20.0f);
        LOGGER.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)", Float.valueOf(f * 100.0f), Float.valueOf(fPow), Float.valueOf((f * 40.0f) - 40.0f));
        return fPow;
    }

    private PlaybackDelegate getPlaybackDelegate() {
        checkAsyncRingtonePlayerThread();
        if (this.mPlaybackDelegate == null) {
            if (Utils.isMOrLater()) {
                this.mPlaybackDelegate = new RingtonePlaybackDelegate();
            } else {
                this.mPlaybackDelegate = new MediaPlayerPlaybackDelegate();
            }
        }
        return this.mPlaybackDelegate;
    }

    private class MediaPlayerPlaybackDelegate implements PlaybackDelegate {
        private AudioManager mAudioManager;
        private long mCrescendoDuration;
        private long mCrescendoStopTime;
        private MediaPlayer mMediaPlayer;

        private MediaPlayerPlaybackDelegate() {
            this.mCrescendoDuration = 0L;
            this.mCrescendoStopTime = 0L;
        }

        @Override
        public boolean play(final Context context, Uri uri, long j) {
            AsyncRingtonePlayer.this.checkAsyncRingtonePlayerThread();
            this.mCrescendoDuration = j;
            AsyncRingtonePlayer.LOGGER.i("Play ringtone via android.media.MediaPlayer.", new Object[0]);
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) context.getSystemService("audio");
            }
            boolean zIsInTelephoneCall = AsyncRingtonePlayer.isInTelephoneCall(context);
            if (zIsInTelephoneCall) {
                uri = AsyncRingtonePlayer.getInCallRingtoneUri(context);
            }
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(4);
                AsyncRingtonePlayer.LOGGER.v("Using default alarm: " + uri.toString(), new Object[0]);
            }
            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                    AsyncRingtonePlayer.LOGGER.e("Error occurred while playing audio. Stopping AlarmKlaxon.", new Object[0]);
                    MediaPlayerPlaybackDelegate.this.stop(context);
                    return true;
                }
            });
            try {
                this.mMediaPlayer.setDataSource(context, uri);
                return startPlayback(zIsInTelephoneCall);
            } catch (Throwable th) {
                AsyncRingtonePlayer.LOGGER.e("Using the fallback ringtone, could not play " + uri, th);
                try {
                    this.mMediaPlayer.reset();
                    this.mMediaPlayer.setDataSource(context, AsyncRingtonePlayer.getFallbackRingtoneUri(context));
                    return startPlayback(zIsInTelephoneCall);
                } catch (Throwable th2) {
                    AsyncRingtonePlayer.LOGGER.e("Failed to play fallback ringtone", th2);
                    return false;
                }
            }
        }

        private boolean startPlayback(boolean z) throws IOException {
            boolean z2 = false;
            if (this.mAudioManager.getStreamVolume(4) == 0) {
                return false;
            }
            if (Utils.isLOrLater()) {
                this.mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(4).setContentType(4).build());
            }
            if (z) {
                AsyncRingtonePlayer.LOGGER.v("Using the in-call alarm", new Object[0]);
                this.mMediaPlayer.setVolume(AsyncRingtonePlayer.IN_CALL_VOLUME, AsyncRingtonePlayer.IN_CALL_VOLUME);
            } else if (this.mCrescendoDuration > 0) {
                this.mMediaPlayer.setVolume(0.0f, 0.0f);
                this.mCrescendoStopTime = Utils.now() + this.mCrescendoDuration;
                z2 = true;
            }
            this.mMediaPlayer.setAudioStreamType(4);
            this.mMediaPlayer.setLooping(true);
            this.mMediaPlayer.prepare();
            this.mAudioManager.requestAudioFocus(null, 4, 2);
            this.mMediaPlayer.start();
            return z2;
        }

        @Override
        public void stop(Context context) {
            AsyncRingtonePlayer.this.checkAsyncRingtonePlayerThread();
            AsyncRingtonePlayer.LOGGER.i("Stop ringtone via android.media.MediaPlayer.", new Object[0]);
            this.mCrescendoDuration = 0L;
            this.mCrescendoStopTime = 0L;
            if (this.mMediaPlayer != null) {
                this.mMediaPlayer.stop();
                this.mMediaPlayer.release();
                this.mMediaPlayer = null;
            }
            if (this.mAudioManager != null) {
                this.mAudioManager.abandonAudioFocus(null);
            }
        }

        @Override
        public boolean adjustVolume(Context context) {
            AsyncRingtonePlayer.this.checkAsyncRingtonePlayerThread();
            if (this.mMediaPlayer == null || !this.mMediaPlayer.isPlaying()) {
                this.mCrescendoDuration = 0L;
                this.mCrescendoStopTime = 0L;
                return false;
            }
            long jNow = Utils.now();
            if (jNow <= this.mCrescendoStopTime) {
                float fComputeVolume = AsyncRingtonePlayer.computeVolume(jNow, this.mCrescendoStopTime, this.mCrescendoDuration);
                this.mMediaPlayer.setVolume(fComputeVolume, fComputeVolume);
                AsyncRingtonePlayer.LOGGER.i("MediaPlayer volume set to " + fComputeVolume, new Object[0]);
                return true;
            }
            this.mCrescendoDuration = 0L;
            this.mCrescendoStopTime = 0L;
            this.mMediaPlayer.setVolume(1.0f, 1.0f);
            return false;
        }
    }

    private class RingtonePlaybackDelegate implements PlaybackDelegate {
        private AudioManager mAudioManager;
        private long mCrescendoDuration;
        private long mCrescendoStopTime;
        private Ringtone mRingtone;
        private Method mSetLoopingMethod;
        private Method mSetVolumeMethod;

        private RingtonePlaybackDelegate() {
            this.mCrescendoDuration = 0L;
            this.mCrescendoStopTime = 0L;
            try {
                this.mSetVolumeMethod = Ringtone.class.getDeclaredMethod("setVolume", Float.TYPE);
            } catch (NoSuchMethodException e) {
                AsyncRingtonePlayer.LOGGER.e("Unable to locate method: Ringtone.setVolume(float).", e);
            }
            try {
                this.mSetLoopingMethod = Ringtone.class.getDeclaredMethod("setLooping", Boolean.TYPE);
            } catch (NoSuchMethodException e2) {
                AsyncRingtonePlayer.LOGGER.e("Unable to locate method: Ringtone.setLooping(boolean).", e2);
            }
        }

        @Override
        public boolean play(Context context, Uri uri, long j) {
            AsyncRingtonePlayer.this.checkAsyncRingtonePlayerThread();
            this.mCrescendoDuration = j;
            AsyncRingtonePlayer.LOGGER.i("Play ringtone via android.media.Ringtone.", new Object[0]);
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) context.getSystemService("audio");
            }
            boolean zIsInTelephoneCall = AsyncRingtonePlayer.isInTelephoneCall(context);
            if (zIsInTelephoneCall) {
                uri = AsyncRingtonePlayer.getInCallRingtoneUri(context);
            }
            this.mRingtone = RingtoneManager.getRingtone(context, uri);
            if (UserManager.get(context.getApplicationContext()).isUserUnlocked() && !Utils.isRingtoneExisted(context, uri)) {
                this.mRingtone = null;
            }
            if (this.mRingtone == null) {
                uri = RingtoneManager.getDefaultUri(4);
                this.mRingtone = RingtoneManager.getRingtone(context, uri);
            }
            if (this.mRingtone == null) {
                AsyncRingtonePlayer.LOGGER.i("Unable to locate alarm ringtone, using internal fallback ringtone.", new Object[0]);
                uri = AsyncRingtonePlayer.getFallbackRingtoneUri(context);
                this.mRingtone = RingtoneManager.getRingtone(context, uri);
            }
            try {
                this.mSetLoopingMethod.invoke(this.mRingtone, true);
            } catch (Exception e) {
                AsyncRingtonePlayer.LOGGER.e("Unable to turn looping on for android.media.Ringtone", e);
                this.mRingtone = null;
            }
            try {
                return startPlayback(zIsInTelephoneCall);
            } catch (Throwable th) {
                AsyncRingtonePlayer.LOGGER.e("Using the fallback ringtone, could not play " + uri, th);
                this.mRingtone = RingtoneManager.getRingtone(context, AsyncRingtonePlayer.getFallbackRingtoneUri(context));
                try {
                    return startPlayback(zIsInTelephoneCall);
                } catch (Throwable th2) {
                    AsyncRingtonePlayer.LOGGER.e("Failed to play fallback ringtone", th2);
                    return false;
                }
            }
        }

        private boolean startPlayback(boolean z) {
            if (Utils.isLOrLater()) {
                this.mRingtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(4).setContentType(4).build());
            }
            boolean z2 = false;
            if (z) {
                AsyncRingtonePlayer.LOGGER.v("Using the in-call alarm", new Object[0]);
                setRingtoneVolume(AsyncRingtonePlayer.IN_CALL_VOLUME);
            } else if (this.mCrescendoDuration > 0) {
                setRingtoneVolume(0.0f);
                this.mCrescendoStopTime = Utils.now() + this.mCrescendoDuration;
                z2 = true;
            }
            this.mAudioManager.requestAudioFocus(null, 4, 2);
            this.mRingtone.play();
            return z2;
        }

        private void setRingtoneVolume(float f) {
            try {
                this.mSetVolumeMethod.invoke(this.mRingtone, Float.valueOf(f));
            } catch (Exception e) {
                AsyncRingtonePlayer.LOGGER.e("Unable to set volume for android.media.Ringtone", e);
            }
        }

        @Override
        public void stop(Context context) {
            AsyncRingtonePlayer.this.checkAsyncRingtonePlayerThread();
            AsyncRingtonePlayer.LOGGER.i("Stop ringtone via android.media.Ringtone.", new Object[0]);
            this.mCrescendoDuration = 0L;
            this.mCrescendoStopTime = 0L;
            if (this.mRingtone != null && this.mRingtone.isPlaying()) {
                AsyncRingtonePlayer.LOGGER.d("Ringtone.stop() invoked.", new Object[0]);
                this.mRingtone.stop();
            }
            this.mRingtone = null;
            if (this.mAudioManager != null) {
                this.mAudioManager.abandonAudioFocus(null);
            }
        }

        @Override
        public boolean adjustVolume(Context context) {
            AsyncRingtonePlayer.this.checkAsyncRingtonePlayerThread();
            if (this.mRingtone == null || !this.mRingtone.isPlaying()) {
                this.mCrescendoDuration = 0L;
                this.mCrescendoStopTime = 0L;
                return false;
            }
            long jNow = Utils.now();
            if (jNow > this.mCrescendoStopTime) {
                this.mCrescendoDuration = 0L;
                this.mCrescendoStopTime = 0L;
                setRingtoneVolume(1.0f);
                return false;
            }
            setRingtoneVolume(AsyncRingtonePlayer.computeVolume(jNow, this.mCrescendoStopTime, this.mCrescendoDuration));
            return true;
        }
    }
}
