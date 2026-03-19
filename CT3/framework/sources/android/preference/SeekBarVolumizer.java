package android.preference;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioAttributes;
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
import com.android.internal.annotations.GuardedBy;

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
    private int mLastAudibleStreamVolume;
    private final int mMaxStreamVolume;
    private boolean mMuted;
    private final NotificationManager mNotificationManager;
    private boolean mNotificationOrRing;
    private int mOriginalStreamVolume;
    private int mRingerMode;

    @GuardedBy("this")
    private Ringtone mRingtone;
    private SeekBar mSeekBar;
    private final int mStreamType;
    private Observer mVolumeObserver;
    private int mZenMode;
    private final H mUiHandler = new H(this, null);
    private final Receiver mReceiver = new Receiver(this, 0 == true ? 1 : 0);
    private int mLastProgress = -1;
    private int mVolumeBeforeMute = -1;

    public interface Callback {
        void onMuted(boolean z, boolean z2);

        void onProgressChanged(SeekBar seekBar, int i, boolean z);

        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);
    }

    public SeekBarVolumizer(Context context, int i, Uri uri, Callback callback) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mStreamType = i;
        this.mAffectedByRingerMode = this.mAudioManager.isStreamAffectedByRingerMode(this.mStreamType);
        this.mNotificationOrRing = isNotificationOrRing(this.mStreamType);
        if (this.mNotificationOrRing) {
            this.mRingerMode = this.mAudioManager.getRingerModeInternal();
        }
        this.mZenMode = this.mNotificationManager.getZenMode();
        this.mMaxStreamVolume = this.mAudioManager.getStreamMaxVolume(this.mStreamType);
        this.mCallback = callback;
        this.mOriginalStreamVolume = this.mAudioManager.getStreamVolume(this.mStreamType);
        this.mLastAudibleStreamVolume = this.mAudioManager.getLastAudibleStreamVolume(this.mStreamType);
        this.mMuted = this.mAudioManager.isStreamMute(this.mStreamType);
        if (this.mCallback != null) {
            this.mCallback.onMuted(this.mMuted, isZenMuted());
        }
        if (uri == null) {
            if (this.mStreamType == 2) {
                uri = Settings.System.DEFAULT_RINGTONE_URI;
            } else if (this.mStreamType == 5) {
                uri = Settings.System.DEFAULT_NOTIFICATION_URI;
            } else {
                uri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            }
        }
        this.mDefaultUri = uri;
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

    private boolean isZenMuted() {
        return (this.mNotificationOrRing && this.mZenMode == 3) || this.mZenMode == 2;
    }

    protected void updateSeekBar() {
        boolean zenMuted = isZenMuted();
        this.mSeekBar.setEnabled(!zenMuted);
        if (zenMuted) {
            this.mSeekBar.setProgress(this.mLastAudibleStreamVolume);
            return;
        }
        if (this.mNotificationOrRing && this.mRingerMode == 1) {
            this.mSeekBar.setProgress(0);
        } else if (this.mMuted) {
            this.mSeekBar.setProgress(0);
        } else {
            this.mSeekBar.setProgress(this.mLastProgress > -1 ? this.mLastProgress : this.mOriginalStreamVolume);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                int volume = msg.arg1;
                if (this.mMuted && volume > 0) {
                    this.mAudioManager.adjustStreamVolume(this.mStreamType, 100, 0);
                } else if (!this.mMuted && volume == 0) {
                    this.mAudioManager.adjustStreamVolume(this.mStreamType, -100, 0);
                }
                this.mAudioManager.setStreamVolume(this.mStreamType, volume, 1024);
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
        synchronized (this) {
            this.mRingtone = RingtoneManager.getRingtone(this.mContext, this.mDefaultUri);
            if (this.mRingtone != null) {
                this.mRingtone.setStreamType(this.mStreamType);
            }
        }
    }

    private void postStartSample() {
        if (this.mHandler == null) {
            return;
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), isSamplePlaying() ? 1000 : 0);
    }

    private void onStartSample() {
        if (isSamplePlaying()) {
            return;
        }
        if (this.mCallback != null) {
            this.mCallback.onSampleStarting(this);
        }
        synchronized (this) {
            if (this.mRingtone != null) {
                try {
                    this.mRingtone.setAudioAttributes(new AudioAttributes.Builder(this.mRingtone.getAudioAttributes()).setFlags(192).build());
                    this.mRingtone.play();
                } catch (Throwable e) {
                    Log.w(TAG, "Error playing ringtone, stream " + this.mStreamType, e);
                }
            }
        }
    }

    private void postStopSample() {
        if (this.mHandler == null) {
            return;
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
    }

    private void onStopSample() {
        synchronized (this) {
            if (this.mRingtone != null) {
                this.mRingtone.stop();
            }
        }
    }

    public void stop() {
        if (this.mHandler == null) {
            return;
        }
        postStopSample();
        this.mContext.getContentResolver().unregisterContentObserver(this.mVolumeObserver);
        this.mReceiver.setListening(false);
        this.mSeekBar.setOnSeekBarChangeListener(null);
        this.mHandler.getLooper().quitSafely();
        this.mHandler = null;
        this.mVolumeObserver = null;
    }

    public void start() {
        if (this.mHandler != null) {
            return;
        }
        HandlerThread thread = new HandlerThread("SeekBarVolumizer.CallbackHandler");
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this);
        this.mHandler.sendEmptyMessage(3);
        this.mVolumeObserver = new Observer(this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.VOLUME_SETTINGS[this.mStreamType]), false, this.mVolumeObserver);
        this.mReceiver.setListening(true);
    }

    public void revertVolume() {
        this.mAudioManager.setStreamVolume(this.mStreamType, this.mOriginalStreamVolume, 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (fromTouch) {
            postSetVolume(progress);
        }
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onProgressChanged(seekBar, progress, fromTouch);
    }

    private void postSetVolume(int progress) {
        if (this.mHandler == null) {
            return;
        }
        this.mLastProgress = progress;
        this.mHandler.removeMessages(0);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, progress, 0));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        postStartSample();
    }

    public boolean isSamplePlaying() {
        boolean zIsPlaying;
        synchronized (this) {
            zIsPlaying = this.mRingtone != null ? this.mRingtone.isPlaying() : false;
        }
        return zIsPlaying;
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
        if (this.mLastProgress < 0) {
            return;
        }
        volumeStore.volume = this.mLastProgress;
        volumeStore.originalVolume = this.mOriginalStreamVolume;
    }

    public void onRestoreInstanceState(VolumePreference.VolumeStore volumeStore) {
        if (volumeStore.volume == -1) {
            return;
        }
        this.mOriginalStreamVolume = volumeStore.originalVolume;
        this.mLastProgress = volumeStore.volume;
        postSetVolume(this.mLastProgress);
    }

    private final class H extends Handler {
        private static final int UPDATE_SLIDER = 1;

        H(SeekBarVolumizer this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != 1 || SeekBarVolumizer.this.mSeekBar == null) {
                return;
            }
            SeekBarVolumizer.this.mLastProgress = msg.arg1;
            SeekBarVolumizer.this.mLastAudibleStreamVolume = Math.abs(msg.arg2);
            boolean muted = msg.arg2 < 0;
            if (muted != SeekBarVolumizer.this.mMuted) {
                SeekBarVolumizer.this.mMuted = muted;
                if (SeekBarVolumizer.this.mCallback != null) {
                    SeekBarVolumizer.this.mCallback.onMuted(SeekBarVolumizer.this.mMuted, SeekBarVolumizer.this.isZenMuted());
                }
            }
            SeekBarVolumizer.this.updateSeekBar();
        }

        public void postUpdateSlider(int volume, int lastAudibleVolume, boolean mute) {
            int arg2 = lastAudibleVolume * (mute ? -1 : 1);
            obtainMessage(1, volume, arg2).sendToTarget();
        }
    }

    private void updateSlider() {
        if (this.mSeekBar == null || this.mAudioManager == null) {
            return;
        }
        int volume = this.mAudioManager.getStreamVolume(this.mStreamType);
        int lastAudibleVolume = this.mAudioManager.getLastAudibleStreamVolume(this.mStreamType);
        boolean mute = this.mAudioManager.isStreamMute(this.mStreamType);
        this.mUiHandler.postUpdateSlider(volume, lastAudibleVolume, mute);
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

        Receiver(SeekBarVolumizer this$0, Receiver receiver) {
            this();
        }

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (this.mListening == listening) {
                return;
            }
            this.mListening = listening;
            if (listening) {
                IntentFilter filter = new IntentFilter(AudioManager.VOLUME_CHANGED_ACTION);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                filter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
                filter.addAction(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
                SeekBarVolumizer.this.mContext.registerReceiver(this, filter);
                return;
            }
            SeekBarVolumizer.this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                int streamValue = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
                updateVolumeSlider(streamType, streamValue);
                return;
            }
            if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                if (SeekBarVolumizer.this.mNotificationOrRing) {
                    SeekBarVolumizer.this.mRingerMode = SeekBarVolumizer.this.mAudioManager.getRingerModeInternal();
                }
                if (!SeekBarVolumizer.this.mAffectedByRingerMode) {
                    return;
                }
                SeekBarVolumizer.this.updateSlider();
                return;
            }
            if (AudioManager.STREAM_DEVICES_CHANGED_ACTION.equals(action)) {
                int streamType2 = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                int streamVolume = SeekBarVolumizer.this.mAudioManager.getStreamVolume(streamType2);
                updateVolumeSlider(streamType2, streamVolume);
            } else {
                if (!NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.equals(action)) {
                    return;
                }
                SeekBarVolumizer.this.mZenMode = SeekBarVolumizer.this.mNotificationManager.getZenMode();
                SeekBarVolumizer.this.updateSlider();
            }
        }

        private void updateVolumeSlider(int streamType, int streamValue) {
            boolean streamMatch;
            if (SeekBarVolumizer.this.mNotificationOrRing) {
                streamMatch = SeekBarVolumizer.isNotificationOrRing(streamType);
            } else {
                streamMatch = streamType == SeekBarVolumizer.this.mStreamType;
            }
            if (SeekBarVolumizer.this.mSeekBar == null || !streamMatch || streamValue == -1) {
                return;
            }
            boolean muted = SeekBarVolumizer.this.mAudioManager.isStreamMute(SeekBarVolumizer.this.mStreamType) || streamValue == 0;
            SeekBarVolumizer.this.mUiHandler.postUpdateSlider(streamValue, SeekBarVolumizer.this.mLastAudibleStreamVolume, muted);
        }
    }
}
