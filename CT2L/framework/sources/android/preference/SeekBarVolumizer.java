package android.preference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.VolumePreference;
import android.provider.Settings;
import android.util.Log;
import android.widget.SeekBar;

public class SeekBarVolumizer implements SeekBar.OnSeekBarChangeListener, Handler.Callback {
    private static final int CHECK_RINGTONE_PLAYBACK_DELAY_MS = 1000;
    private static final int MSG_INIT_SAMPLE = 3;
    private static final int MSG_SET_STREAM_VOLUME = 0;
    private static final int MSG_START_SAMPLE = 1;
    private static final int MSG_STOP_SAMPLE = 2;
    private static final String TAG = "SeekBarVolumizer";
    private boolean mAffectedByRingerMode;
    private final AudioManager mAudioManager;
    private final Callback mCallback;
    private final Context mContext;
    private final Uri mDefaultUri;
    private Handler mHandler;
    private final int mMaxStreamVolume;
    private boolean mMuted;
    private boolean mNotificationOrRing;
    private int mOriginalStreamVolume;
    private final Receiver mReceiver;
    private int mRingerMode;
    private Ringtone mRingtone;
    private SeekBar mSeekBar;
    private final int mStreamType;
    private final H mUiHandler;
    private Observer mVolumeObserver;
    private int mLastProgress = -1;
    private int mVolumeBeforeMute = -1;

    public interface Callback {
        void onMuted(boolean z);

        void onProgressChanged(SeekBar seekBar, int i, boolean z);

        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);
    }

    public SeekBarVolumizer(Context context, int streamType, Uri defaultUri, Callback callback) {
        this.mUiHandler = new H();
        this.mReceiver = new Receiver();
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mStreamType = streamType;
        this.mAffectedByRingerMode = this.mAudioManager.isStreamAffectedByRingerMode(this.mStreamType);
        this.mNotificationOrRing = isNotificationOrRing(this.mStreamType);
        if (this.mNotificationOrRing) {
            this.mRingerMode = this.mAudioManager.getRingerModeInternal();
        }
        this.mMaxStreamVolume = this.mAudioManager.getStreamMaxVolume(this.mStreamType);
        this.mCallback = callback;
        this.mOriginalStreamVolume = this.mAudioManager.getStreamVolume(this.mStreamType);
        this.mMuted = this.mAudioManager.isStreamMute(this.mStreamType);
        if (this.mCallback != null) {
            this.mCallback.onMuted(this.mMuted);
        }
        if (defaultUri == null) {
            if (this.mStreamType == 2) {
                defaultUri = Settings.System.DEFAULT_RINGTONE_URI;
            } else if (this.mStreamType == 5) {
                defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI;
            } else {
                defaultUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            }
        }
        this.mDefaultUri = defaultUri;
    }

    private static boolean isNotificationOrRing(int stream) {
        return stream == 2 || stream == 5;
    }

    public void setSeekBar(SeekBar seekBar) {
        if (this.mSeekBar != null) {
            this.mSeekBar.setOnSeekBarChangeListener(null);
        }
        this.mSeekBar = seekBar;
        this.mSeekBar.setOnSeekBarChangeListener(null);
        this.mSeekBar.setMax(this.mMaxStreamVolume);
        updateSeekBar();
        this.mSeekBar.setOnSeekBarChangeListener(this);
    }

    protected void updateSeekBar() {
        if (this.mNotificationOrRing && this.mRingerMode == 1) {
            this.mSeekBar.setEnabled(true);
            this.mSeekBar.setProgress(0);
        } else if (this.mMuted) {
            this.mSeekBar.setEnabled(false);
            this.mSeekBar.setProgress(0);
        } else {
            this.mSeekBar.setEnabled(true);
            this.mSeekBar.setProgress(this.mLastProgress > -1 ? this.mLastProgress : this.mOriginalStreamVolume);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                this.mAudioManager.setStreamVolume(this.mStreamType, this.mLastProgress, 1024);
                break;
            case 1:
                onStartSample();
                break;
            case 2:
                onStopSample();
                break;
            case 3:
                onInitSample();
                break;
            default:
                Log.e(TAG, "invalid SeekBarVolumizer message: " + msg.what);
                break;
        }
        return true;
    }

    private void onInitSample() {
        this.mRingtone = RingtoneManager.getRingtone(this.mContext, this.mDefaultUri);
        if (this.mRingtone != null) {
            this.mRingtone.setStreamType(this.mStreamType);
        }
    }

    private void postStartSample() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(1);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), isSamplePlaying() ? 1000L : 0L);
        }
    }

    private void onStartSample() {
        if (!isSamplePlaying()) {
            if (this.mCallback != null) {
                this.mCallback.onSampleStarting(this);
            }
            if (this.mRingtone != null) {
                try {
                    this.mRingtone.play();
                } catch (Throwable e) {
                    Log.w(TAG, "Error playing ringtone, stream " + this.mStreamType, e);
                }
            }
        }
    }

    private void postStopSample() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        }
    }

    private void onStopSample() {
        if (this.mRingtone != null) {
            this.mRingtone.stop();
        }
    }

    public void stop() {
        if (this.mHandler != null) {
            postStopSample();
            this.mContext.getContentResolver().unregisterContentObserver(this.mVolumeObserver);
            this.mReceiver.setListening(false);
            this.mSeekBar.setOnSeekBarChangeListener(null);
            this.mHandler.getLooper().quitSafely();
            this.mHandler = null;
            this.mVolumeObserver = null;
        }
    }

    public void start() {
        if (this.mHandler == null) {
            HandlerThread thread = new HandlerThread("SeekBarVolumizer.CallbackHandler");
            thread.start();
            this.mHandler = new Handler(thread.getLooper(), this);
            this.mHandler.sendEmptyMessage(3);
            this.mVolumeObserver = new Observer(this.mHandler);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.VOLUME_SETTINGS[this.mStreamType]), false, this.mVolumeObserver);
            this.mReceiver.setListening(true);
        }
    }

    public void revertVolume() {
        this.mAudioManager.setStreamVolume(this.mStreamType, this.mOriginalStreamVolume, 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (fromTouch) {
            postSetVolume(progress);
        }
        if (this.mCallback != null) {
            this.mCallback.onProgressChanged(seekBar, progress, fromTouch);
        }
    }

    private void postSetVolume(int progress) {
        if (this.mHandler != null) {
            this.mLastProgress = progress;
            this.mHandler.removeMessages(0);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        postStartSample();
    }

    public boolean isSamplePlaying() {
        return this.mRingtone != null && this.mRingtone.isPlaying();
    }

    public void startSample() {
        postStartSample();
    }

    public void stopSample() {
        postStopSample();
    }

    public SeekBar getSeekBar() {
        return this.mSeekBar;
    }

    public void changeVolumeBy(int amount) {
        this.mSeekBar.incrementProgressBy(amount);
        postSetVolume(this.mSeekBar.getProgress());
        postStartSample();
        this.mVolumeBeforeMute = -1;
    }

    public void muteVolume() {
        if (this.mVolumeBeforeMute != -1) {
            this.mSeekBar.setProgress(this.mVolumeBeforeMute);
            postSetVolume(this.mVolumeBeforeMute);
            postStartSample();
            this.mVolumeBeforeMute = -1;
            return;
        }
        this.mVolumeBeforeMute = this.mSeekBar.getProgress();
        this.mSeekBar.setProgress(0);
        postStopSample();
        postSetVolume(0);
    }

    public void onSaveInstanceState(VolumePreference.VolumeStore volumeStore) {
        if (this.mLastProgress >= 0) {
            volumeStore.volume = this.mLastProgress;
            volumeStore.originalVolume = this.mOriginalStreamVolume;
        }
    }

    public void onRestoreInstanceState(VolumePreference.VolumeStore volumeStore) {
        if (volumeStore.volume != -1) {
            this.mOriginalStreamVolume = volumeStore.originalVolume;
            this.mLastProgress = volumeStore.volume;
            postSetVolume(this.mLastProgress);
        }
    }

    private final class H extends Handler {
        private static final int UPDATE_SLIDER = 1;

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1 && SeekBarVolumizer.this.mSeekBar != null) {
                SeekBarVolumizer.this.mLastProgress = msg.arg1;
                boolean muted = msg.arg2 != 0;
                if (muted != SeekBarVolumizer.this.mMuted) {
                    SeekBarVolumizer.this.mMuted = muted;
                    if (SeekBarVolumizer.this.mCallback != null) {
                        SeekBarVolumizer.this.mCallback.onMuted(SeekBarVolumizer.this.mMuted);
                    }
                }
                SeekBarVolumizer.this.updateSeekBar();
            }
        }

        public void postUpdateSlider(int volume, boolean mute) {
            obtainMessage(1, volume, mute ? 1 : 0).sendToTarget();
        }
    }

    private void updateSlider() {
        if (this.mSeekBar != null && this.mAudioManager != null) {
            int volume = this.mAudioManager.getStreamVolume(this.mStreamType);
            boolean mute = this.mAudioManager.isStreamMute(this.mStreamType);
            this.mUiHandler.postUpdateSlider(volume, mute);
        }
    }

    private final class Observer extends ContentObserver {
        public Observer(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            SeekBarVolumizer.this.updateSlider();
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mListening;

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (this.mListening != listening) {
                this.mListening = listening;
                if (!listening) {
                    SeekBarVolumizer.this.mContext.unregisterReceiver(this);
                    return;
                }
                IntentFilter filter = new IntentFilter(AudioManager.VOLUME_CHANGED_ACTION);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                SeekBarVolumizer.this.mContext.registerReceiver(this, filter);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                int streamValue = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
                boolean streamMatch = SeekBarVolumizer.this.mNotificationOrRing ? SeekBarVolumizer.isNotificationOrRing(streamType) : streamType == SeekBarVolumizer.this.mStreamType;
                if (SeekBarVolumizer.this.mSeekBar != null && streamMatch && streamValue != -1) {
                    boolean muted = SeekBarVolumizer.this.mAudioManager.isStreamMute(SeekBarVolumizer.this.mStreamType);
                    SeekBarVolumizer.this.mUiHandler.postUpdateSlider(streamValue, muted);
                    return;
                }
                return;
            }
            if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                if (SeekBarVolumizer.this.mNotificationOrRing) {
                    SeekBarVolumizer.this.mRingerMode = SeekBarVolumizer.this.mAudioManager.getRingerModeInternal();
                }
                if (SeekBarVolumizer.this.mAffectedByRingerMode) {
                    SeekBarVolumizer.this.updateSlider();
                }
            }
        }
    }
}
