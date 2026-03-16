package android.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.IAudioService;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class AudioRecord {
    private static final int AUDIORECORD_ERROR_SETUP_INVALIDCHANNELMASK = -17;
    private static final int AUDIORECORD_ERROR_SETUP_INVALIDFORMAT = -18;
    private static final int AUDIORECORD_ERROR_SETUP_INVALIDSOURCE = -19;
    private static final int AUDIORECORD_ERROR_SETUP_NATIVEINITFAILED = -20;
    private static final int AUDIORECORD_ERROR_SETUP_ZEROFRAMECOUNT = -16;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_INVALID_OPERATION = -3;
    private static final int NATIVE_EVENT_MARKER = 2;
    private static final int NATIVE_EVENT_NEW_POS = 3;
    public static final int RECORDSTATE_RECORDING = 3;
    public static final int RECORDSTATE_STOPPED = 1;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_UNINITIALIZED = 0;
    public static final String SUBMIX_FIXED_VOLUME = "fixedVolume";
    public static final int SUCCESS = 0;
    private static final String TAG = "android.media.AudioRecord";
    private AudioAttributes mAudioAttributes;
    private int mAudioFormat;
    private int mChannelCount;
    private int mChannelMask;
    private NativeEventHandler mEventHandler;
    private final IBinder mICallBack;
    private Looper mInitializationLooper;
    private boolean mIsSubmixFullVolume;
    private int mNativeBufferSizeInBytes;
    private long mNativeCallbackCookie;
    private long mNativeRecorderInJavaObj;
    private OnRecordPositionUpdateListener mPositionListener;
    private final Object mPositionListenerLock;
    private int mRecordSource;
    private int mRecordingState;
    private final Object mRecordingStateLock;
    private int mSampleRate;
    private int mSessionId;
    private int mState;

    public interface OnRecordPositionUpdateListener {
        void onMarkerReached(AudioRecord audioRecord);

        void onPeriodicNotification(AudioRecord audioRecord);
    }

    private final native void native_finalize();

    private final native int native_get_marker_pos();

    private static final native int native_get_min_buff_size(int i, int i2, int i3);

    private final native int native_get_pos_update_period();

    private final native int native_read_in_byte_array(byte[] bArr, int i, int i2);

    private final native int native_read_in_direct_buffer(Object obj, int i);

    private final native int native_read_in_short_array(short[] sArr, int i, int i2);

    private final native void native_release();

    private final native int native_set_marker_pos(int i);

    private final native int native_set_pos_update_period(int i);

    private final native int native_setup(Object obj, Object obj2, int i, int i2, int i3, int i4, int[] iArr);

    private final native int native_start(int i, int i2);

    private final native void native_stop();

    public AudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) throws IllegalArgumentException {
        this(new AudioAttributes.Builder().setInternalCapturePreset(audioSource).build(), new AudioFormat.Builder().setChannelMask(getChannelMaskFromLegacyConfig(channelConfig, true)).setEncoding(audioFormat).setSampleRate(sampleRateInHz).build(), bufferSizeInBytes, 0);
    }

    public AudioRecord(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int sessionId) throws IllegalArgumentException {
        int rate;
        this.mState = 0;
        this.mRecordingState = 1;
        this.mRecordingStateLock = new Object();
        this.mPositionListener = null;
        this.mPositionListenerLock = new Object();
        this.mEventHandler = null;
        this.mInitializationLooper = null;
        this.mNativeBufferSizeInBytes = 0;
        this.mSessionId = 0;
        this.mIsSubmixFullVolume = false;
        this.mICallBack = new Binder();
        this.mRecordingState = 1;
        if (attributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        if (format == null) {
            throw new IllegalArgumentException("Illegal null AudioFormat");
        }
        Looper looperMyLooper = Looper.myLooper();
        this.mInitializationLooper = looperMyLooper;
        if (looperMyLooper == null) {
            this.mInitializationLooper = Looper.getMainLooper();
        }
        if (attributes.getCapturePreset() == 8) {
            AudioAttributes.Builder filteredAttr = new AudioAttributes.Builder();
            for (String tag : attributes.getTags()) {
                if (tag.equalsIgnoreCase(SUBMIX_FIXED_VOLUME)) {
                    this.mIsSubmixFullVolume = true;
                    Log.v(TAG, "Will record from REMOTE_SUBMIX at full fixed volume");
                } else {
                    filteredAttr.addTag(tag);
                }
            }
            filteredAttr.setInternalCapturePreset(attributes.getCapturePreset());
            this.mAudioAttributes = filteredAttr.build();
        } else {
            this.mAudioAttributes = attributes;
        }
        if ((format.getPropertySetMask() & 2) != 0) {
            rate = format.getSampleRate();
        } else {
            rate = AudioSystem.getPrimaryOutputSamplingRate();
            if (rate <= 0) {
                rate = 44100;
            }
        }
        int encoding = (format.getPropertySetMask() & 1) != 0 ? format.getEncoding() : 1;
        audioParamCheck(attributes.getCapturePreset(), rate, encoding);
        this.mChannelCount = AudioFormat.channelCountFromInChannelMask(format.getChannelMask());
        this.mChannelMask = getChannelMaskFromLegacyConfig(format.getChannelMask(), false);
        audioBuffSizeCheck(bufferSizeInBytes);
        int[] session = {sessionId};
        int initResult = native_setup(new WeakReference(this), this.mAudioAttributes, this.mSampleRate, this.mChannelMask, this.mAudioFormat, this.mNativeBufferSizeInBytes, session);
        if (initResult != 0) {
            loge("Error code " + initResult + " when initializing native AudioRecord object.");
        } else {
            this.mSessionId = session[0];
            this.mState = 1;
        }
    }

    private static int getChannelMaskFromLegacyConfig(int inChannelConfig, boolean allowLegacyConfig) {
        int mask;
        switch (inChannelConfig) {
            case 1:
            case 2:
            case 16:
                mask = 16;
                break;
            case 3:
            case 12:
                mask = 12;
                break;
            case 48:
                mask = inChannelConfig;
                break;
            default:
                throw new IllegalArgumentException("Unsupported channel configuration.");
        }
        if (!allowLegacyConfig && (inChannelConfig == 2 || inChannelConfig == 3)) {
            throw new IllegalArgumentException("Unsupported deprecated configuration.");
        }
        return mask;
    }

    private void audioParamCheck(int audioSource, int sampleRateInHz, int audioFormat) throws IllegalArgumentException {
        if (audioSource < 0 || (audioSource > MediaRecorder.getAudioSourceMax() && audioSource != 1998 && audioSource != 1999)) {
            throw new IllegalArgumentException("Invalid audio source.");
        }
        this.mRecordSource = audioSource;
        if (sampleRateInHz < 4000 || sampleRateInHz > 48000) {
            throw new IllegalArgumentException(sampleRateInHz + "Hz is not a supported sample rate.");
        }
        this.mSampleRate = sampleRateInHz;
        switch (audioFormat) {
            case 1:
                this.mAudioFormat = 2;
                return;
            case 2:
            case 3:
                this.mAudioFormat = audioFormat;
                return;
            default:
                throw new IllegalArgumentException("Unsupported sample encoding. Should be ENCODING_PCM_8BIT or ENCODING_PCM_16BIT.");
        }
    }

    private void audioBuffSizeCheck(int audioBufferSize) throws IllegalArgumentException {
        int frameSizeInBytes = this.mChannelCount * AudioFormat.getBytesPerSample(this.mAudioFormat);
        if (audioBufferSize % frameSizeInBytes != 0 || audioBufferSize < 1) {
            throw new IllegalArgumentException("Invalid audio buffer size.");
        }
        this.mNativeBufferSizeInBytes = audioBufferSize;
    }

    public void release() {
        try {
            stop();
        } catch (IllegalStateException e) {
        }
        native_release();
        this.mState = 0;
    }

    protected void finalize() {
        release();
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getAudioSource() {
        return this.mRecordSource;
    }

    public int getAudioFormat() {
        return this.mAudioFormat;
    }

    public int getChannelConfiguration() {
        return this.mChannelMask;
    }

    public int getChannelCount() {
        return this.mChannelCount;
    }

    public int getState() {
        return this.mState;
    }

    public int getRecordingState() {
        int i;
        synchronized (this.mRecordingStateLock) {
            i = this.mRecordingState;
        }
        return i;
    }

    public int getNotificationMarkerPosition() {
        return native_get_marker_pos();
    }

    public int getPositionNotificationPeriod() {
        return native_get_pos_update_period();
    }

    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
        int channelCount;
        switch (channelConfig) {
            case 1:
            case 2:
            case 16:
                channelCount = 1;
                break;
            case 3:
            case 12:
            case 48:
                channelCount = 2;
                break;
            default:
                loge("getMinBufferSize(): Invalid channel configuration.");
                return -2;
        }
        if (audioFormat != 2) {
            loge("getMinBufferSize(): Invalid audio format.");
            return -2;
        }
        int size = native_get_min_buff_size(sampleRateInHz, channelCount, audioFormat);
        if (size == 0) {
            return -2;
        }
        if (size == -1) {
            return -1;
        }
        return size;
    }

    public int getAudioSessionId() {
        return this.mSessionId;
    }

    public void startRecording() throws IllegalStateException {
        if (this.mState != 1) {
            throw new IllegalStateException("startRecording() called on an uninitialized AudioRecord.");
        }
        synchronized (this.mRecordingStateLock) {
            if (native_start(0, 0) == 0) {
                handleFullVolumeRec(true);
                this.mRecordingState = 3;
            }
        }
    }

    public void startRecording(MediaSyncEvent syncEvent) throws IllegalStateException {
        if (this.mState != 1) {
            throw new IllegalStateException("startRecording() called on an uninitialized AudioRecord.");
        }
        synchronized (this.mRecordingStateLock) {
            if (native_start(syncEvent.getType(), syncEvent.getAudioSessionId()) == 0) {
                handleFullVolumeRec(true);
                this.mRecordingState = 3;
            }
        }
    }

    public void stop() throws IllegalStateException {
        if (this.mState != 1) {
            throw new IllegalStateException("stop() called on an uninitialized AudioRecord.");
        }
        synchronized (this.mRecordingStateLock) {
            handleFullVolumeRec(false);
            native_stop();
            this.mRecordingState = 1;
        }
    }

    private void handleFullVolumeRec(boolean starting) {
        if (this.mIsSubmixFullVolume) {
            IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
            IAudioService ias = IAudioService.Stub.asInterface(b);
            try {
                ias.forceRemoteSubmixFullVolume(starting, this.mICallBack);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to AudioService when handling full submix volume", e);
            }
        }
    }

    public int read(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if (this.mState != 1) {
            return -3;
        }
        if (audioData == null || offsetInBytes < 0 || sizeInBytes < 0 || offsetInBytes + sizeInBytes < 0 || offsetInBytes + sizeInBytes > audioData.length) {
            return -2;
        }
        return native_read_in_byte_array(audioData, offsetInBytes, sizeInBytes);
    }

    public int read(short[] audioData, int offsetInShorts, int sizeInShorts) {
        if (this.mState != 1) {
            return -3;
        }
        if (audioData == null || offsetInShorts < 0 || sizeInShorts < 0 || offsetInShorts + sizeInShorts < 0 || offsetInShorts + sizeInShorts > audioData.length) {
            return -2;
        }
        return native_read_in_short_array(audioData, offsetInShorts, sizeInShorts);
    }

    public int read(ByteBuffer audioBuffer, int sizeInBytes) {
        if (this.mState != 1) {
            return -3;
        }
        if (audioBuffer == null || sizeInBytes < 0) {
            return -2;
        }
        return native_read_in_direct_buffer(audioBuffer, sizeInBytes);
    }

    public void setRecordPositionUpdateListener(OnRecordPositionUpdateListener listener) {
        setRecordPositionUpdateListener(listener, null);
    }

    public void setRecordPositionUpdateListener(OnRecordPositionUpdateListener listener, Handler handler) {
        synchronized (this.mPositionListenerLock) {
            this.mPositionListener = listener;
            if (listener != null) {
                if (handler != null) {
                    this.mEventHandler = new NativeEventHandler(this, handler.getLooper());
                } else {
                    this.mEventHandler = new NativeEventHandler(this, this.mInitializationLooper);
                }
            } else {
                this.mEventHandler = null;
            }
        }
    }

    public int setNotificationMarkerPosition(int markerInFrames) {
        if (this.mState == 0) {
            return -3;
        }
        return native_set_marker_pos(markerInFrames);
    }

    public int setPositionNotificationPeriod(int periodInFrames) {
        if (this.mState == 0) {
            return -3;
        }
        return native_set_pos_update_period(periodInFrames);
    }

    private class NativeEventHandler extends Handler {
        private final AudioRecord mAudioRecord;

        NativeEventHandler(AudioRecord recorder, Looper looper) {
            super(looper);
            this.mAudioRecord = recorder;
        }

        @Override
        public void handleMessage(Message msg) {
            OnRecordPositionUpdateListener listener;
            synchronized (AudioRecord.this.mPositionListenerLock) {
                listener = this.mAudioRecord.mPositionListener;
            }
            switch (msg.what) {
                case 2:
                    if (listener != null) {
                        listener.onMarkerReached(this.mAudioRecord);
                        return;
                    }
                    return;
                case 3:
                    if (listener != null) {
                        listener.onPeriodicNotification(this.mAudioRecord);
                        return;
                    }
                    return;
                default:
                    AudioRecord.loge("Unknown native event type: " + msg.what);
                    return;
            }
        }
    }

    private static void postEventFromNative(Object audiorecord_ref, int what, int arg1, int arg2, Object obj) {
        AudioRecord recorder = (AudioRecord) ((WeakReference) audiorecord_ref).get();
        if (recorder != null && recorder.mEventHandler != null) {
            Message m = recorder.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            recorder.mEventHandler.sendMessage(m);
        }
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
