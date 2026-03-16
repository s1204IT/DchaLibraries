package android.media;

import android.app.ActivityThread;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.app.IAppOpsService;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.NioUtils;

public class AudioTrack {
    private static final int CHANNEL_COUNT_MAX = 8;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_INVALID_OPERATION = -3;
    private static final int ERROR_NATIVESETUP_AUDIOSYSTEM = -16;
    private static final int ERROR_NATIVESETUP_INVALIDCHANNELMASK = -17;
    private static final int ERROR_NATIVESETUP_INVALIDFORMAT = -18;
    private static final int ERROR_NATIVESETUP_INVALIDSTREAMTYPE = -19;
    private static final int ERROR_NATIVESETUP_NATIVEINITFAILED = -20;
    private static final float GAIN_MAX = 1.0f;
    private static final float GAIN_MIN = 0.0f;
    public static final int MODE_STATIC = 0;
    public static final int MODE_STREAM = 1;
    private static final int NATIVE_EVENT_MARKER = 3;
    private static final int NATIVE_EVENT_NEW_POS = 4;
    public static final int PLAYSTATE_PAUSED = 2;
    public static final int PLAYSTATE_PLAYING = 3;
    public static final int PLAYSTATE_STOPPED = 1;
    private static final int SAMPLE_RATE_HZ_MAX = 96000;
    private static final int SAMPLE_RATE_HZ_MIN = 4000;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_NO_STATIC_DATA = 2;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int SUCCESS = 0;
    private static final int SUPPORTED_OUT_CHANNELS = 7420;
    private static final String TAG = "android.media.AudioTrack";
    public static final int WRITE_BLOCKING = 0;
    public static final int WRITE_NON_BLOCKING = 1;
    private final IAppOpsService mAppOps;
    private final AudioAttributes mAttributes;
    private int mAudioFormat;
    private int mChannelConfiguration;
    private int mChannelCount;
    private int mChannels;
    private int mDataLoadMode;
    private NativeEventHandlerDelegate mEventHandlerDelegate;
    private final Looper mInitializationLooper;
    private long mJniData;
    private int mNativeBufferSizeInBytes;
    private int mNativeBufferSizeInFrames;
    private long mNativeTrackInJavaObj;
    private int mPlayState;
    private final Object mPlayStateLock;
    private int mSampleRate;
    private int mSessionId;
    private int mState;
    private int mStreamType;

    public interface OnPlaybackPositionUpdateListener {
        void onMarkerReached(AudioTrack audioTrack);

        void onPeriodicNotification(AudioTrack audioTrack);
    }

    private final native int native_attachAuxEffect(int i);

    private final native void native_finalize();

    private final native void native_flush();

    private final native int native_get_latency();

    private final native int native_get_marker_pos();

    private static final native int native_get_min_buff_size(int i, int i2, int i3);

    private final native int native_get_native_frame_count();

    private static final native int native_get_output_sample_rate(int i);

    private final native int native_get_playback_rate();

    private final native int native_get_pos_update_period();

    private final native int native_get_position();

    private final native int native_get_timestamp(long[] jArr);

    private final native void native_pause();

    private final native void native_release();

    private final native int native_reload_static();

    private final native int native_setAuxEffectSendLevel(float f);

    private final native void native_setVolume(float f, float f2);

    private final native int native_set_loop(int i, int i2, int i3);

    private final native int native_set_marker_pos(int i);

    private final native int native_set_playback_rate(int i);

    private final native int native_set_pos_update_period(int i);

    private final native int native_set_position(int i);

    private final native int native_setup(Object obj, Object obj2, int i, int i2, int i3, int i4, int i5, int[] iArr);

    private final native void native_start();

    private final native void native_stop();

    private final native int native_write_byte(byte[] bArr, int i, int i2, int i3, boolean z);

    private final native int native_write_float(float[] fArr, int i, int i2, int i3, boolean z);

    private final native int native_write_native_bytes(Object obj, int i, int i2, int i3, boolean z);

    private final native int native_write_short(short[] sArr, int i, int i2, int i3);

    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) throws IllegalArgumentException {
        this(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode, 0);
    }

    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode, int sessionId) throws IllegalArgumentException {
        this(new AudioAttributes.Builder().setLegacyStreamType(streamType).build(), new AudioFormat.Builder().setChannelMask(channelConfig).setEncoding(audioFormat).setSampleRate(sampleRateInHz).build(), bufferSizeInBytes, mode, sessionId);
    }

    public AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId) throws IllegalArgumentException {
        int rate;
        this.mState = 0;
        this.mPlayState = 1;
        this.mPlayStateLock = new Object();
        this.mNativeBufferSizeInBytes = 0;
        this.mNativeBufferSizeInFrames = 0;
        this.mChannelCount = 1;
        this.mChannels = 4;
        this.mStreamType = 3;
        this.mDataLoadMode = 1;
        this.mChannelConfiguration = 4;
        this.mAudioFormat = 2;
        this.mSessionId = 0;
        if (attributes == null) {
            throw new IllegalArgumentException("Illegal null AudioAttributes");
        }
        if (format == null) {
            throw new IllegalArgumentException("Illegal null AudioFormat");
        }
        Looper looper = Looper.myLooper();
        looper = looper == null ? Looper.getMainLooper() : looper;
        if ((format.getPropertySetMask() & 2) != 0) {
            rate = format.getSampleRate();
        } else {
            rate = AudioSystem.getPrimaryOutputSamplingRate();
            if (rate <= 0) {
                rate = 44100;
            }
        }
        int channelMask = (format.getPropertySetMask() & 4) != 0 ? format.getChannelMask() : 12;
        int encoding = (format.getPropertySetMask() & 1) != 0 ? format.getEncoding() : 1;
        audioParamCheck(rate, channelMask, encoding, mode);
        this.mStreamType = -1;
        audioBuffSizeCheck(bufferSizeInBytes);
        this.mInitializationLooper = looper;
        IBinder b = ServiceManager.getService(Context.APP_OPS_SERVICE);
        this.mAppOps = IAppOpsService.Stub.asInterface(b);
        this.mAttributes = new AudioAttributes.Builder(attributes).build();
        if (sessionId < 0) {
            throw new IllegalArgumentException("Invalid audio session ID: " + sessionId);
        }
        int[] session = {sessionId};
        int initResult = native_setup(new WeakReference(this), this.mAttributes, this.mSampleRate, this.mChannels, this.mAudioFormat, this.mNativeBufferSizeInBytes, this.mDataLoadMode, session);
        if (initResult != 0) {
            loge("Error code " + initResult + " when initializing AudioTrack.");
            return;
        }
        this.mSessionId = session[0];
        if (this.mDataLoadMode == 0) {
            this.mState = 2;
        } else {
            this.mState = 1;
        }
    }

    private void audioParamCheck(int sampleRateInHz, int channelConfig, int audioFormat, int mode) {
        if (sampleRateInHz < SAMPLE_RATE_HZ_MIN || sampleRateInHz > SAMPLE_RATE_HZ_MAX) {
            throw new IllegalArgumentException(sampleRateInHz + "Hz is not a supported sample rate.");
        }
        this.mSampleRate = sampleRateInHz;
        this.mChannelConfiguration = channelConfig;
        switch (channelConfig) {
            case 1:
            case 2:
            case 4:
                this.mChannelCount = 1;
                this.mChannels = 4;
                break;
            case 3:
            case 12:
                this.mChannelCount = 2;
                this.mChannels = 12;
                break;
            default:
                if (!isMultichannelConfigSupported(channelConfig)) {
                    throw new IllegalArgumentException("Unsupported channel configuration.");
                }
                this.mChannels = channelConfig;
                this.mChannelCount = Integer.bitCount(channelConfig);
                break;
                break;
        }
        if (audioFormat == 1) {
            audioFormat = 2;
        }
        if (!AudioFormat.isValidEncoding(audioFormat)) {
            throw new IllegalArgumentException("Unsupported audio encoding.");
        }
        this.mAudioFormat = audioFormat;
        if ((mode != 1 && mode != 0) || (mode != 1 && !AudioFormat.isEncodingLinearPcm(this.mAudioFormat))) {
            throw new IllegalArgumentException("Invalid mode.");
        }
        this.mDataLoadMode = mode;
    }

    private static boolean isMultichannelConfigSupported(int channelConfig) {
        if ((channelConfig & SUPPORTED_OUT_CHANNELS) != channelConfig) {
            loge("Channel configuration features unsupported channels");
            return false;
        }
        int channelCount = Integer.bitCount(channelConfig);
        if (channelCount > 8) {
            loge("Channel configuration contains too many channels " + channelCount + ">8");
            return false;
        }
        if ((channelConfig & 12) != 12) {
            loge("Front channels must be present in multichannel configurations");
            return false;
        }
        if ((channelConfig & 192) != 0 && (channelConfig & 192) != 192) {
            loge("Rear channels can't be used independently");
            return false;
        }
        if ((channelConfig & GLES30.GL_COLOR) != 0 && (channelConfig & GLES30.GL_COLOR) != 6144) {
            loge("Side channels can't be used independently");
            return false;
        }
        return true;
    }

    private void audioBuffSizeCheck(int audioBufferSize) {
        int frameSizeInBytes;
        if (AudioFormat.isEncodingLinearPcm(this.mAudioFormat)) {
            frameSizeInBytes = this.mChannelCount * AudioFormat.getBytesPerSample(this.mAudioFormat);
        } else {
            frameSizeInBytes = 1;
        }
        if (audioBufferSize % frameSizeInBytes != 0 || audioBufferSize < 1) {
            throw new IllegalArgumentException("Invalid audio buffer size.");
        }
        this.mNativeBufferSizeInBytes = audioBufferSize;
        this.mNativeBufferSizeInFrames = audioBufferSize / frameSizeInBytes;
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
        native_finalize();
    }

    public static float getMinVolume() {
        return 0.0f;
    }

    public static float getMaxVolume() {
        return 1.0f;
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getPlaybackRate() {
        return native_get_playback_rate();
    }

    public int getAudioFormat() {
        return this.mAudioFormat;
    }

    public int getStreamType() {
        return this.mStreamType;
    }

    public int getChannelConfiguration() {
        return this.mChannelConfiguration;
    }

    public int getChannelCount() {
        return this.mChannelCount;
    }

    public int getState() {
        return this.mState;
    }

    public int getPlayState() {
        int i;
        synchronized (this.mPlayStateLock) {
            i = this.mPlayState;
        }
        return i;
    }

    @Deprecated
    protected int getNativeFrameCount() {
        return native_get_native_frame_count();
    }

    public int getNotificationMarkerPosition() {
        return native_get_marker_pos();
    }

    public int getPositionNotificationPeriod() {
        return native_get_pos_update_period();
    }

    public int getPlaybackHeadPosition() {
        return native_get_position();
    }

    public int getLatency() {
        return native_get_latency();
    }

    public static int getNativeOutputSampleRate(int streamType) {
        return native_get_output_sample_rate(streamType);
    }

    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat) {
        int channelCount;
        switch (channelConfig) {
            case 2:
            case 4:
                channelCount = 1;
                break;
            case 3:
            case 12:
                channelCount = 2;
                break;
            default:
                if ((channelConfig & SUPPORTED_OUT_CHANNELS) != channelConfig) {
                    loge("getMinBufferSize(): Invalid channel configuration.");
                    return -2;
                }
                channelCount = Integer.bitCount(channelConfig);
                break;
                break;
        }
        if (!AudioFormat.isValidEncoding(audioFormat)) {
            loge("getMinBufferSize(): Invalid audio format.");
            return -2;
        }
        if (sampleRateInHz < SAMPLE_RATE_HZ_MIN || sampleRateInHz > SAMPLE_RATE_HZ_MAX) {
            loge("getMinBufferSize(): " + sampleRateInHz + " Hz is not a supported sample rate.");
            return -2;
        }
        int size = native_get_min_buff_size(sampleRateInHz, channelCount, audioFormat);
        if (size <= 0) {
            loge("getMinBufferSize(): error querying hardware");
            return -1;
        }
        return size;
    }

    public int getAudioSessionId() {
        return this.mSessionId;
    }

    public boolean getTimestamp(AudioTimestamp timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException();
        }
        long[] longArray = new long[2];
        int ret = native_get_timestamp(longArray);
        if (ret != 0) {
            return false;
        }
        timestamp.framePosition = longArray[0];
        timestamp.nanoTime = longArray[1];
        return true;
    }

    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
        setPlaybackPositionUpdateListener(listener, null);
    }

    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener, Handler handler) {
        if (listener != null) {
            this.mEventHandlerDelegate = new NativeEventHandlerDelegate(this, listener, handler);
        } else {
            this.mEventHandlerDelegate = null;
        }
    }

    private static float clampGainOrLevel(float gainOrLevel) {
        if (Float.isNaN(gainOrLevel)) {
            throw new IllegalArgumentException();
        }
        if (gainOrLevel < 0.0f) {
            return 0.0f;
        }
        if (gainOrLevel > 1.0f) {
            return 1.0f;
        }
        return gainOrLevel;
    }

    public int setStereoVolume(float leftGain, float rightGain) {
        if (isRestricted()) {
            return 0;
        }
        if (this.mState == 0) {
            return -3;
        }
        native_setVolume(clampGainOrLevel(leftGain), clampGainOrLevel(rightGain));
        return 0;
    }

    public int setVolume(float gain) {
        return setStereoVolume(gain, gain);
    }

    public int setPlaybackRate(int sampleRateInHz) {
        if (this.mState != 1) {
            return -3;
        }
        if (sampleRateInHz <= 0) {
            return -2;
        }
        return native_set_playback_rate(sampleRateInHz);
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

    public int setPlaybackHeadPosition(int positionInFrames) {
        if (this.mDataLoadMode == 1 || this.mState == 0 || getPlayState() == 3) {
            return -3;
        }
        if (positionInFrames < 0 || positionInFrames > this.mNativeBufferSizeInFrames) {
            return -2;
        }
        return native_set_position(positionInFrames);
    }

    public int setLoopPoints(int startInFrames, int endInFrames, int loopCount) {
        if (this.mDataLoadMode == 1 || this.mState == 0 || getPlayState() == 3) {
            return -3;
        }
        if (loopCount != 0 && (startInFrames < 0 || startInFrames >= this.mNativeBufferSizeInFrames || startInFrames >= endInFrames || endInFrames > this.mNativeBufferSizeInFrames)) {
            return -2;
        }
        return native_set_loop(startInFrames, endInFrames, loopCount);
    }

    @Deprecated
    protected void setState(int state) {
        this.mState = state;
    }

    public void play() throws IllegalStateException {
        if (this.mState != 1) {
            throw new IllegalStateException("play() called on uninitialized AudioTrack.");
        }
        if (isRestricted()) {
            setVolume(0.0f);
        }
        synchronized (this.mPlayStateLock) {
            native_start();
            this.mPlayState = 3;
        }
    }

    private boolean isRestricted() {
        try {
            int usage = AudioAttributes.usageForLegacyStreamType(this.mStreamType);
            int mode = this.mAppOps.checkAudioOperation(28, usage, Process.myUid(), ActivityThread.currentPackageName());
            return mode != 0;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void stop() throws IllegalStateException {
        if (this.mState != 1) {
            throw new IllegalStateException("stop() called on uninitialized AudioTrack.");
        }
        synchronized (this.mPlayStateLock) {
            native_stop();
            this.mPlayState = 1;
        }
    }

    public void pause() throws IllegalStateException {
        if (this.mState != 1) {
            throw new IllegalStateException("pause() called on uninitialized AudioTrack.");
        }
        synchronized (this.mPlayStateLock) {
            native_pause();
            this.mPlayState = 2;
        }
    }

    public void flush() {
        if (this.mState == 1) {
            native_flush();
        }
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if (this.mState == 0 || this.mAudioFormat == 4) {
            return -3;
        }
        if (audioData == null || offsetInBytes < 0 || sizeInBytes < 0 || offsetInBytes + sizeInBytes < 0 || offsetInBytes + sizeInBytes > audioData.length) {
            return -2;
        }
        int ret = native_write_byte(audioData, offsetInBytes, sizeInBytes, this.mAudioFormat, true);
        if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
            this.mState = 1;
            return ret;
        }
        return ret;
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
        if (this.mState == 0 || this.mAudioFormat == 4) {
            return -3;
        }
        if (audioData == null || offsetInShorts < 0 || sizeInShorts < 0 || offsetInShorts + sizeInShorts < 0 || offsetInShorts + sizeInShorts > audioData.length) {
            return -2;
        }
        int ret = native_write_short(audioData, offsetInShorts, sizeInShorts, this.mAudioFormat);
        if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
            this.mState = 1;
            return ret;
        }
        return ret;
    }

    public int write(float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode) {
        if (this.mState == 0) {
            Log.e(TAG, "AudioTrack.write() called in invalid state STATE_UNINITIALIZED");
            return -3;
        }
        if (this.mAudioFormat != 4) {
            Log.e(TAG, "AudioTrack.write(float[] ...) requires format ENCODING_PCM_FLOAT");
            return -3;
        }
        if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        }
        if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0 || offsetInFloats + sizeInFloats < 0 || offsetInFloats + sizeInFloats > audioData.length) {
            Log.e(TAG, "AudioTrack.write() called with invalid array, offset, or size");
            return -2;
        }
        int ret = native_write_float(audioData, offsetInFloats, sizeInFloats, this.mAudioFormat, writeMode == 0);
        if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
            this.mState = 1;
            return ret;
        }
        return ret;
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode) {
        int ret;
        if (this.mState == 0) {
            Log.e(TAG, "AudioTrack.write() called in invalid state STATE_UNINITIALIZED");
            return -3;
        }
        if (writeMode != 0 && writeMode != 1) {
            Log.e(TAG, "AudioTrack.write() called with invalid blocking mode");
            return -2;
        }
        if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) {
            Log.e(TAG, "AudioTrack.write() called with invalid size (" + sizeInBytes + ") value");
            return -2;
        }
        if (audioData.isDirect()) {
            ret = native_write_native_bytes(audioData, audioData.position(), sizeInBytes, this.mAudioFormat, writeMode == 0);
        } else {
            ret = native_write_byte(NioUtils.unsafeArray(audioData), audioData.position() + NioUtils.unsafeArrayOffset(audioData), sizeInBytes, this.mAudioFormat, writeMode == 0);
        }
        if (this.mDataLoadMode == 0 && this.mState == 2 && ret > 0) {
            this.mState = 1;
        }
        if (ret > 0) {
            audioData.position(audioData.position() + ret);
            return ret;
        }
        return ret;
    }

    public int reloadStaticData() {
        if (this.mDataLoadMode == 1 || this.mState != 1) {
            return -3;
        }
        return native_reload_static();
    }

    public int attachAuxEffect(int effectId) {
        if (this.mState == 0) {
            return -3;
        }
        return native_attachAuxEffect(effectId);
    }

    public int setAuxEffectSendLevel(float level) {
        if (isRestricted()) {
            return 0;
        }
        if (this.mState == 0) {
            return -3;
        }
        int err = native_setAuxEffectSendLevel(clampGainOrLevel(level));
        return err != 0 ? -1 : 0;
    }

    private class NativeEventHandlerDelegate {
        private final Handler mHandler;

        NativeEventHandlerDelegate(final AudioTrack track, final OnPlaybackPositionUpdateListener listener, Handler handler) {
            Looper looper;
            if (handler == null) {
                looper = AudioTrack.this.mInitializationLooper;
            } else {
                looper = handler.getLooper();
            }
            if (looper != null) {
                this.mHandler = new Handler(looper) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (track != null) {
                            switch (msg.what) {
                                case 3:
                                    if (listener != null) {
                                        listener.onMarkerReached(track);
                                    }
                                    break;
                                case 4:
                                    if (listener != null) {
                                        listener.onPeriodicNotification(track);
                                    }
                                    break;
                                default:
                                    AudioTrack.loge("Unknown native event type: " + msg.what);
                                    break;
                            }
                        }
                    }
                };
            } else {
                this.mHandler = null;
            }
        }

        Handler getHandler() {
            return this.mHandler;
        }
    }

    private static void postEventFromNative(Object audiotrack_ref, int what, int arg1, int arg2, Object obj) {
        NativeEventHandlerDelegate delegate;
        Handler handler;
        AudioTrack track = (AudioTrack) ((WeakReference) audiotrack_ref).get();
        if (track != null && (delegate = track.mEventHandlerDelegate) != null && (handler = delegate.getHandler()) != null) {
            Message m = handler.obtainMessage(what, arg1, arg2, obj);
            handler.sendMessage(m);
        }
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
