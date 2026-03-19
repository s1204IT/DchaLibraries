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
import android.service.notification.ZenModeConfig;
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
    private boolean mAllowAlarms;
    private boolean mAllowMedia;
    private boolean mAllowRinger;
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
    private NotificationManager.Policy mNotificationPolicy;
    private int mOriginalStreamVolume;
    private final Receiver mReceiver;
    private int mRingerMode;

    @GuardedBy("this")
    private Ringtone mRingtone;
    private SeekBar mSeekBar;
    private final int mStreamType;
    private final H mUiHandler;
    private Observer mVolumeObserver;
    private int mZenMode;
    private int mLastProgress = -1;
    private int mVolumeBeforeMute = -1;

    public interface Callback {
        void onMuted(boolean z, boolean z2);

        void onProgressChanged(SeekBar seekBar, int i, boolean z);

        void onSampleStarting(SeekBarVolumizer seekBarVolumizer);
    }

    public SeekBarVolumizer(Context context, int i, Uri uri, Callback callback) {
        this.mUiHandler = new H();
        this.mReceiver = new Receiver();
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mNotificationPolicy = this.mNotificationManager.getNotificationPolicy();
        this.mAllowAlarms = (this.mNotificationPolicy.priorityCategories & 32) != 0;
        this.mAllowMedia = (this.mNotificationPolicy.priorityCategories & 64) != 0;
        this.mAllowRinger = !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(this.mNotificationPolicy);
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

    private static boolean isNotificationOrRing(int i) {
        return i == 2 || i == 5;
    }

    private static boolean isAlarmsStream(int i) {
        return i == 4;
    }

    private static boolean isMediaStream(int i) {
        return i == 3;
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
        if ((this.mNotificationOrRing && this.mZenMode == 3) || this.mZenMode == 2) {
            return true;
        }
        if (this.mZenMode == 1) {
            if (!this.mAllowAlarms && isAlarmsStream(this.mStreamType)) {
                return true;
            }
            if (!this.mAllowMedia && isMediaStream(this.mStreamType)) {
                return true;
            }
            if (!this.mAllowRinger && isNotificationOrRing(this.mStreamType)) {
                return true;
            }
        }
        return false;
    }

    protected void updateSeekBar() {
        boolean zIsZenMuted = isZenMuted();
        this.mSeekBar.setEnabled(!zIsZenMuted);
        if (zIsZenMuted) {
            this.mSeekBar.setProgress(this.mLastAudibleStreamVolume, true);
            return;
        }
        if (this.mNotificationOrRing && this.mRingerMode == 1) {
            this.mSeekBar.setProgress(0, true);
        } else if (this.mMuted) {
            this.mSeekBar.setProgress(0, true);
        } else {
            this.mSeekBar.setProgress(this.mLastProgress > -1 ? this.mLastProgress : this.mOriginalStreamVolume, true);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 0:
                if (this.mMuted && this.mLastProgress > 0) {
                    this.mAudioManager.adjustStreamVolume(this.mStreamType, 100, 0);
                } else if (!this.mMuted && this.mLastProgress == 0) {
                    this.mAudioManager.adjustStreamVolume(this.mStreamType, -100, 0);
                }
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
                Log.e(TAG, "invalid SeekBarVolumizer message: " + message.what);
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
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), isSamplePlaying() ? 1000L : 0L);
    }

    private void onStartSample() {
        if (!isSamplePlaying()) {
            if (this.mCallback != null) {
                this.mCallback.onSampleStarting(this);
            }
            synchronized (this) {
                if (this.mRingtone != null) {
                    try {
                        this.mRingtone.setAudioAttributes(new AudioAttributes.Builder(this.mRingtone.getAudioAttributes()).setFlags(128).build());
                        this.mRingtone.play();
                    } catch (Throwable th) {
                        Log.w(TAG, "Error playing ringtone, stream " + this.mStreamType, th);
                    }
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
        HandlerThread handlerThread = new HandlerThread("SeekBarVolumizer.CallbackHandler");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper(), this);
        this.mHandler.sendEmptyMessage(3);
        this.mVolumeObserver = new Observer(this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.VOLUME_SETTINGS[this.mStreamType]), false, this.mVolumeObserver);
        this.mReceiver.setListening(true);
    }

    public void revertVolume() {
        this.mAudioManager.setStreamVolume(this.mStreamType, this.mOriginalStreamVolume, 0);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z) {
            postSetVolume(i);
        }
        if (this.mCallback != null) {
            this.mCallback.onProgressChanged(seekBar, i, z);
        }
    }

    private void postSetVolume(int i) {
        if (this.mHandler == null) {
            return;
        }
        this.mLastProgress = i;
        this.mHandler.removeMessages(0);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        postStartSample();
    }

    public boolean isSamplePlaying() {
        boolean z;
        synchronized (this) {
            z = this.mRingtone != null && this.mRingtone.isPlaying();
        }
        return z;
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

    public void changeVolumeBy(int i) {
        this.mSeekBar.incrementProgressBy(i);
        postSetVolume(this.mSeekBar.getProgress());
        postStartSample();
        this.mVolumeBeforeMute = -1;
    }

    public void muteVolume() {
        if (this.mVolumeBeforeMute != -1) {
            this.mSeekBar.setProgress(this.mVolumeBeforeMute, true);
            postSetVolume(this.mVolumeBeforeMute);
            postStartSample();
            this.mVolumeBeforeMute = -1;
            return;
        }
        this.mVolumeBeforeMute = this.mSeekBar.getProgress();
        this.mSeekBar.setProgress(0, true);
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
        public void handleMessage(Message message) {
            if (message.what == 1 && SeekBarVolumizer.this.mSeekBar != null) {
                SeekBarVolumizer.this.mLastProgress = message.arg1;
                SeekBarVolumizer.this.mLastAudibleStreamVolume = message.arg2;
                boolean zBooleanValue = ((Boolean) message.obj).booleanValue();
                if (zBooleanValue != SeekBarVolumizer.this.mMuted) {
                    SeekBarVolumizer.this.mMuted = zBooleanValue;
                    if (SeekBarVolumizer.this.mCallback != null) {
                        SeekBarVolumizer.this.mCallback.onMuted(SeekBarVolumizer.this.mMuted, SeekBarVolumizer.this.isZenMuted());
                    }
                }
                SeekBarVolumizer.this.updateSeekBar();
            }
        }

        public void postUpdateSlider(int i, int i2, boolean z) {
            obtainMessage(1, i, i2, new Boolean(z)).sendToTarget();
        }
    }

    private void updateSlider() {
        if (this.mSeekBar != null && this.mAudioManager != null) {
            this.mUiHandler.postUpdateSlider(this.mAudioManager.getStreamVolume(this.mStreamType), this.mAudioManager.getLastAudibleStreamVolume(this.mStreamType), this.mAudioManager.isStreamMute(this.mStreamType));
        }
    }

    private final class Observer extends ContentObserver {
        public Observer(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            SeekBarVolumizer.this.updateSlider();
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mListening;

        private Receiver() {
        }

        public void setListening(boolean z) {
            if (this.mListening == z) {
                return;
            }
            this.mListening = z;
            if (!z) {
                SeekBarVolumizer.this.mContext.unregisterReceiver(this);
                return;
            }
            IntentFilter intentFilter = new IntentFilter(AudioManager.VOLUME_CHANGED_ACTION);
            intentFilter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
            intentFilter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
            intentFilter.addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED);
            intentFilter.addAction(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
            SeekBarVolumizer.this.mContext.registerReceiver(this, intentFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
                updateVolumeSlider(intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1), intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1));
                return;
            }
            if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                if (SeekBarVolumizer.this.mNotificationOrRing) {
                    SeekBarVolumizer.this.mRingerMode = SeekBarVolumizer.this.mAudioManager.getRingerModeInternal();
                }
                if (SeekBarVolumizer.this.mAffectedByRingerMode) {
                    SeekBarVolumizer.this.updateSlider();
                    return;
                }
                return;
            }
            if (AudioManager.STREAM_DEVICES_CHANGED_ACTION.equals(action)) {
                int intExtra = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                updateVolumeSlider(intExtra, SeekBarVolumizer.this.mAudioManager.getStreamVolume(intExtra));
                return;
            }
            if (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.equals(action)) {
                SeekBarVolumizer.this.mZenMode = SeekBarVolumizer.this.mNotificationManager.getZenMode();
                SeekBarVolumizer.this.updateSlider();
            } else if (NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED.equals(action)) {
                SeekBarVolumizer.this.mNotificationPolicy = SeekBarVolumizer.this.mNotificationManager.getNotificationPolicy();
                SeekBarVolumizer.this.mAllowAlarms = (SeekBarVolumizer.this.mNotificationPolicy.priorityCategories & 32) != 0;
                SeekBarVolumizer.this.mAllowMedia = (SeekBarVolumizer.this.mNotificationPolicy.priorityCategories & 64) != 0;
                SeekBarVolumizer.this.mAllowRinger = !ZenModeConfig.areAllPriorityOnlyNotificationZenSoundsMuted(SeekBarVolumizer.this.mNotificationPolicy);
                SeekBarVolumizer.this.updateSlider();
            }
        }

        private void updateVolumeSlider(int i, int i2) {
            boolean zIsNotificationOrRing;
            if (SeekBarVolumizer.this.mNotificationOrRing) {
                zIsNotificationOrRing = SeekBarVolumizer.isNotificationOrRing(i);
            } else {
                zIsNotificationOrRing = i == SeekBarVolumizer.this.mStreamType;
            }
            if (SeekBarVolumizer.this.mSeekBar != null && zIsNotificationOrRing && i2 != -1) {
                SeekBarVolumizer.this.mUiHandler.postUpdateSlider(i2, SeekBarVolumizer.this.mLastAudibleStreamVolume, SeekBarVolumizer.this.mAudioManager.isStreamMute(SeekBarVolumizer.this.mStreamType) || i2 == 0);
            }
        }
    }
}
