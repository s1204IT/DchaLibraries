package android.media;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.hardware.Camera;
import android.media.AudioRouting;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import com.android.internal.annotations.GuardedBy;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MediaRecorder implements AudioRouting {
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_RECORDER_ERROR_UNKNOWN = 1;
    private static final int MEDIA_RECORDER_EVENT_INFO = 2;
    public static final int MEDIA_RECORDER_INFO_CAMERA_RELEASE = 1999;
    public static final int MEDIA_RECORDER_INFO_MAX_DURATION_REACHED = 800;
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING = 802;
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED = 801;
    public static final int MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED = 803;
    public static final int MEDIA_RECORDER_INFO_UNKNOWN = 1;
    public static final int MEDIA_RECORDER_TRACK_INFO_COMPLETION_STATUS = 1000;
    public static final int MEDIA_RECORDER_TRACK_INFO_DATA_KBYTES = 1009;
    public static final int MEDIA_RECORDER_TRACK_INFO_DURATION_MS = 1003;
    public static final int MEDIA_RECORDER_TRACK_INFO_ENCODED_FRAMES = 1005;
    public static final int MEDIA_RECORDER_TRACK_INFO_INITIAL_DELAY_MS = 1007;
    public static final int MEDIA_RECORDER_TRACK_INFO_LIST_END = 2000;
    public static final int MEDIA_RECORDER_TRACK_INFO_LIST_START = 1000;
    public static final int MEDIA_RECORDER_TRACK_INFO_MAX_CHUNK_DUR_MS = 1004;
    public static final int MEDIA_RECORDER_TRACK_INFO_PROGRESS_IN_TIME = 1001;
    public static final int MEDIA_RECORDER_TRACK_INFO_START_OFFSET_MS = 1008;
    public static final int MEDIA_RECORDER_TRACK_INFO_TYPE = 1002;
    public static final int MEDIA_RECORDER_TRACK_INTER_CHUNK_TIME_MS = 1006;
    private static final String TAG = "MediaRecorder";
    private int mChannelCount;
    private EventHandler mEventHandler;
    private FileDescriptor mFd;
    private File mFile;
    private long mNativeContext;
    protected OnInfoListener mOnCameraReleasedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private String mPath;
    private AudioDeviceInfo mPreferredDevice = null;

    @GuardedBy("mRoutingChangeListeners")
    private ArrayMap<AudioRouting.OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> mRoutingChangeListeners = new ArrayMap<>();
    private Surface mSurface;

    public interface OnErrorListener {
        void onError(MediaRecorder mediaRecorder, int i, int i2);
    }

    public interface OnInfoListener {
        void onInfo(MediaRecorder mediaRecorder, int i, int i2);
    }

    private native void _prepare() throws IllegalStateException, IOException;

    private native void _setNextOutputFile(FileDescriptor fileDescriptor) throws IllegalStateException, IOException;

    private native void _setOutputFile(FileDescriptor fileDescriptor) throws IllegalStateException, IOException;

    private final native void native_enableDeviceCallback(boolean z);

    private final native void native_finalize();

    private final native int native_getActiveMicrophones(ArrayList<MicrophoneInfo> arrayList);

    private native PersistableBundle native_getMetrics();

    private final native int native_getRoutedDeviceId();

    private static final native void native_init();

    private native void native_reset();

    private final native boolean native_setInputDevice(int i);

    private final native void native_setInputSurface(Surface surface);

    private final native void native_setup(Object obj, String str, String str2) throws IllegalStateException;

    private native void setParameter(String str);

    public native int getMaxAmplitude() throws IllegalStateException;

    public native Surface getSurface();

    public native void pause() throws IllegalStateException;

    public native void release();

    public native void resume() throws IllegalStateException;

    public native void setAudioEncoder(int i) throws IllegalStateException;

    public native void setAudioSource(int i) throws IllegalStateException;

    @Deprecated
    public native void setCamera(Camera camera);

    public native void setMaxDuration(int i) throws IllegalArgumentException;

    public native void setMaxFileSize(long j) throws IllegalArgumentException;

    public native void setOutputFormat(int i) throws IllegalStateException;

    public native void setVideoEncoder(int i) throws IllegalStateException;

    public native void setVideoFrameRate(int i) throws IllegalStateException;

    public native void setVideoSize(int i, int i2) throws IllegalStateException;

    public native void setVideoSource(int i) throws IllegalStateException;

    public native void start() throws IllegalStateException;

    public native void stop() throws IllegalStateException;

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public MediaRecorder() {
        Looper looperMyLooper = Looper.myLooper();
        if (looperMyLooper != null) {
            this.mEventHandler = new EventHandler(this, looperMyLooper);
        } else {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                this.mEventHandler = new EventHandler(this, mainLooper);
            } else {
                this.mEventHandler = null;
            }
        }
        this.mChannelCount = 1;
        native_setup(new WeakReference(this), ActivityThread.currentPackageName(), ActivityThread.currentOpPackageName());
    }

    public void setInputSurface(Surface surface) {
        if (!(surface instanceof MediaCodec.PersistentSurface)) {
            throw new IllegalArgumentException("not a PersistentSurface");
        }
        native_setInputSurface(surface);
    }

    public void setPreviewDisplay(Surface surface) {
        this.mSurface = surface;
    }

    public final class AudioSource {
        public static final int AUDIO_SOURCE_INVALID = -1;
        public static final int CAMCORDER = 5;
        public static final int DEFAULT = 0;

        @SystemApi
        public static final int HOTWORD = 1999;
        public static final int MIC = 1;

        @SystemApi
        public static final int RADIO_TUNER = 1998;
        public static final int REMOTE_SUBMIX = 8;
        public static final int UNPROCESSED = 9;
        public static final int VOICE_CALL = 4;
        public static final int VOICE_COMMUNICATION = 7;
        public static final int VOICE_DOWNLINK = 3;
        public static final int VOICE_RECOGNITION = 6;
        public static final int VOICE_UPLINK = 2;

        private AudioSource() {
        }
    }

    public static boolean isSystemOnlyAudioSource(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 9:
                return false;
            case 8:
            default:
                return true;
        }
    }

    public static final String toLogFriendlyAudioSource(int i) {
        switch (i) {
            case -1:
                return "AUDIO_SOURCE_INVALID";
            case 0:
                return "DEFAULT";
            case 1:
                return "MIC";
            case 2:
                return "VOICE_UPLINK";
            case 3:
                return "VOICE_DOWNLINK";
            case 4:
                return "VOICE_CALL";
            case 5:
                return "CAMCORDER";
            case 6:
                return "VOICE_RECOGNITION";
            case 7:
                return "VOICE_COMMUNICATION";
            case 8:
                return "REMOTE_SUBMIX";
            case 9:
                return "UNPROCESSED";
            default:
                switch (i) {
                    case AudioSource.RADIO_TUNER:
                        return "RADIO_TUNER";
                    case 1999:
                        return "HOTWORD";
                    default:
                        return "unknown source " + i;
                }
        }
    }

    public final class VideoSource {
        public static final int CAMERA = 1;
        public static final int DEFAULT = 0;
        public static final int SURFACE = 2;

        private VideoSource() {
        }
    }

    public final class OutputFormat {
        public static final int AAC_ADIF = 5;
        public static final int AAC_ADTS = 6;
        public static final int AMR_NB = 3;
        public static final int AMR_WB = 4;
        public static final int DEFAULT = 0;
        public static final int MPEG_2_TS = 8;
        public static final int MPEG_4 = 2;
        public static final int OUTPUT_FORMAT_RTP_AVP = 7;
        public static final int RAW_AMR = 3;
        public static final int THREE_GPP = 1;
        public static final int WEBM = 9;

        private OutputFormat() {
        }
    }

    public final class AudioEncoder {
        public static final int AAC = 3;
        public static final int AAC_ELD = 5;
        public static final int AMR_NB = 1;
        public static final int AMR_WB = 2;
        public static final int DEFAULT = 0;
        public static final int HE_AAC = 4;
        public static final int VORBIS = 6;

        private AudioEncoder() {
        }
    }

    public final class VideoEncoder {
        public static final int DEFAULT = 0;
        public static final int H263 = 1;
        public static final int H264 = 2;
        public static final int HEVC = 5;
        public static final int MPEG_4_SP = 3;
        public static final int VP8 = 4;

        private VideoEncoder() {
        }
    }

    public static final int getAudioSourceMax() {
        return 9;
    }

    public void setProfile(CamcorderProfile camcorderProfile) {
        setOutputFormat(camcorderProfile.fileFormat);
        setVideoFrameRate(camcorderProfile.videoFrameRate);
        setVideoSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
        setVideoEncodingBitRate(camcorderProfile.videoBitRate);
        setVideoEncoder(camcorderProfile.videoCodec);
        if (camcorderProfile.quality < 1000 || camcorderProfile.quality > 1007) {
            setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            setAudioChannels(camcorderProfile.audioChannels);
            setAudioSamplingRate(camcorderProfile.audioSampleRate);
            setAudioEncoder(camcorderProfile.audioCodec);
        }
    }

    public void setCaptureRate(double d) {
        setParameter("time-lapse-enable=1");
        setParameter("time-lapse-fps=" + d);
    }

    public void setOrientationHint(int i) {
        if (i != 0 && i != 90 && i != 180 && i != 270) {
            throw new IllegalArgumentException("Unsupported angle: " + i);
        }
        setParameter("video-param-rotation-angle-degrees=" + i);
    }

    public void setLocation(float f, float f2) {
        int i = (int) (((double) (f * 10000.0f)) + 0.5d);
        int i2 = (int) (((double) (10000.0f * f2)) + 0.5d);
        if (i > 900000 || i < -900000) {
            throw new IllegalArgumentException("Latitude: " + f + " out of range.");
        }
        if (i2 > 1800000 || i2 < -1800000) {
            throw new IllegalArgumentException("Longitude: " + f2 + " out of range");
        }
        setParameter("param-geotag-latitude=" + i);
        setParameter("param-geotag-longitude=" + i2);
    }

    public void setAudioSamplingRate(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Audio sampling rate is not positive");
        }
        setParameter("audio-param-sampling-rate=" + i);
    }

    public void setAudioChannels(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Number of channels is not positive");
        }
        this.mChannelCount = i;
        setParameter("audio-param-number-of-channels=" + i);
    }

    public void setAudioEncodingBitRate(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Audio encoding bit rate is not positive");
        }
        setParameter("audio-param-encoding-bitrate=" + i);
    }

    public void setVideoEncodingBitRate(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("Video encoding bit rate is not positive");
        }
        setParameter("video-param-encoding-bitrate=" + i);
    }

    public void setVideoEncodingProfileLevel(int i, int i2) {
        if (i <= 0) {
            throw new IllegalArgumentException("Video encoding profile is not positive");
        }
        if (i2 <= 0) {
            throw new IllegalArgumentException("Video encoding level is not positive");
        }
        setParameter("video-param-encoder-profile=" + i);
        setParameter("video-param-encoder-level=" + i2);
    }

    public void setAuxiliaryOutputFile(FileDescriptor fileDescriptor) {
        Log.w(TAG, "setAuxiliaryOutputFile(FileDescriptor) is no longer supported.");
    }

    public void setAuxiliaryOutputFile(String str) {
        Log.w(TAG, "setAuxiliaryOutputFile(String) is no longer supported.");
    }

    public void setOutputFile(FileDescriptor fileDescriptor) throws IllegalStateException {
        this.mPath = null;
        this.mFile = null;
        this.mFd = fileDescriptor;
    }

    public void setOutputFile(File file) {
        this.mPath = null;
        this.mFd = null;
        this.mFile = file;
    }

    public void setNextOutputFile(FileDescriptor fileDescriptor) throws IOException {
        _setNextOutputFile(fileDescriptor);
    }

    public void setOutputFile(String str) throws IllegalStateException {
        this.mFd = null;
        this.mFile = null;
        this.mPath = str;
    }

    public void setNextOutputFile(File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        try {
            _setNextOutputFile(randomAccessFile.getFD());
        } finally {
            randomAccessFile.close();
        }
    }

    public void prepare() throws IllegalStateException, IOException {
        if (this.mPath != null) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(this.mPath, "rw");
            try {
                _setOutputFile(randomAccessFile.getFD());
            } finally {
                randomAccessFile.close();
            }
        } else if (this.mFd != null) {
            _setOutputFile(this.mFd);
        } else if (this.mFile != null) {
            RandomAccessFile randomAccessFile2 = new RandomAccessFile(this.mFile, "rw");
            try {
                _setOutputFile(randomAccessFile2.getFD());
            } finally {
                randomAccessFile2.close();
            }
        } else {
            throw new IOException("No valid output file");
        }
        _prepare();
    }

    public void reset() {
        native_reset();
        this.mEventHandler.removeCallbacksAndMessages(null);
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setOnInfoListener(OnInfoListener onInfoListener) {
        this.mOnInfoListener = onInfoListener;
    }

    private class EventHandler extends Handler {
        private static final int MEDIA_RECORDER_AUDIO_ROUTING_CHANGED = 10000;
        private static final int MEDIA_RECORDER_EVENT_ERROR = 1;
        private static final int MEDIA_RECORDER_EVENT_INFO = 2;
        private static final int MEDIA_RECORDER_EVENT_LIST_END = 99;
        private static final int MEDIA_RECORDER_EVENT_LIST_START = 1;
        private static final int MEDIA_RECORDER_TRACK_EVENT_ERROR = 100;
        private static final int MEDIA_RECORDER_TRACK_EVENT_INFO = 101;
        private static final int MEDIA_RECORDER_TRACK_EVENT_LIST_END = 1000;
        private static final int MEDIA_RECORDER_TRACK_EVENT_LIST_START = 100;
        private MediaRecorder mMediaRecorder;

        public EventHandler(MediaRecorder mediaRecorder, Looper looper) {
            super(looper);
            this.mMediaRecorder = mediaRecorder;
        }

        @Override
        public void handleMessage(Message message) {
            if (this.mMediaRecorder.mNativeContext != 0) {
                switch (message.what) {
                    case 1:
                    case 100:
                        if (MediaRecorder.this.mOnErrorListener != null) {
                            MediaRecorder.this.mOnErrorListener.onError(this.mMediaRecorder, message.arg1, message.arg2);
                            return;
                        }
                        return;
                    case 2:
                    case 101:
                        if (MediaRecorder.this.mOnInfoListener != null) {
                            MediaRecorder.this.mOnInfoListener.onInfo(this.mMediaRecorder, message.arg1, message.arg2);
                            return;
                        }
                        return;
                    case 10000:
                        AudioManager.resetAudioPortGeneration();
                        synchronized (MediaRecorder.this.mRoutingChangeListeners) {
                            Iterator it = MediaRecorder.this.mRoutingChangeListeners.values().iterator();
                            while (it.hasNext()) {
                                ((NativeRoutingEventHandlerDelegate) it.next()).notifyClient();
                            }
                            break;
                        }
                        return;
                    default:
                        Log.e(MediaRecorder.TAG, "Unknown message type " + message.what);
                        return;
                }
            }
            Log.w(MediaRecorder.TAG, "mediarecorder went away with unhandled events");
        }
    }

    @Override
    public boolean setPreferredDevice(AudioDeviceInfo audioDeviceInfo) {
        if (audioDeviceInfo != null && !audioDeviceInfo.isSource()) {
            return false;
        }
        boolean zNative_setInputDevice = native_setInputDevice(audioDeviceInfo != null ? audioDeviceInfo.getId() : 0);
        if (zNative_setInputDevice) {
            synchronized (this) {
                this.mPreferredDevice = audioDeviceInfo;
            }
        }
        return zNative_setInputDevice;
    }

    @Override
    public AudioDeviceInfo getPreferredDevice() {
        AudioDeviceInfo audioDeviceInfo;
        synchronized (this) {
            audioDeviceInfo = this.mPreferredDevice;
        }
        return audioDeviceInfo;
    }

    @Override
    public AudioDeviceInfo getRoutedDevice() {
        int iNative_getRoutedDeviceId = native_getRoutedDeviceId();
        if (iNative_getRoutedDeviceId == 0) {
            return null;
        }
        AudioDeviceInfo[] devicesStatic = AudioManager.getDevicesStatic(1);
        for (int i = 0; i < devicesStatic.length; i++) {
            if (devicesStatic[i].getId() == iNative_getRoutedDeviceId) {
                return devicesStatic[i];
            }
        }
        return null;
    }

    @GuardedBy("mRoutingChangeListeners")
    private void enableNativeRoutingCallbacksLocked(boolean z) {
        if (this.mRoutingChangeListeners.size() == 0) {
            native_enableDeviceCallback(z);
        }
    }

    @Override
    public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener onRoutingChangedListener, Handler handler) {
        synchronized (this.mRoutingChangeListeners) {
            if (onRoutingChangedListener != null) {
                try {
                    if (!this.mRoutingChangeListeners.containsKey(onRoutingChangedListener)) {
                        enableNativeRoutingCallbacksLocked(true);
                        ArrayMap<AudioRouting.OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> arrayMap = this.mRoutingChangeListeners;
                        if (handler == null) {
                            handler = this.mEventHandler;
                        }
                        arrayMap.put(onRoutingChangedListener, new NativeRoutingEventHandlerDelegate(this, onRoutingChangedListener, handler));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    @Override
    public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener onRoutingChangedListener) {
        synchronized (this.mRoutingChangeListeners) {
            if (this.mRoutingChangeListeners.containsKey(onRoutingChangedListener)) {
                this.mRoutingChangeListeners.remove(onRoutingChangedListener);
                enableNativeRoutingCallbacksLocked(false);
            }
        }
    }

    public List<MicrophoneInfo> getActiveMicrophones() throws IOException {
        AudioDeviceInfo routedDevice;
        ArrayList<MicrophoneInfo> arrayList = new ArrayList<>();
        int iNative_getActiveMicrophones = native_getActiveMicrophones(arrayList);
        if (iNative_getActiveMicrophones != 0) {
            if (iNative_getActiveMicrophones != -3) {
                Log.e(TAG, "getActiveMicrophones failed:" + iNative_getActiveMicrophones);
            }
            Log.i(TAG, "getActiveMicrophones failed, fallback on routed device info");
        }
        AudioManager.setPortIdForMicrophones(arrayList);
        if (arrayList.size() == 0 && (routedDevice = getRoutedDevice()) != null) {
            MicrophoneInfo microphoneInfoMicrophoneInfoFromAudioDeviceInfo = AudioManager.microphoneInfoFromAudioDeviceInfo(routedDevice);
            ArrayList arrayList2 = new ArrayList();
            for (int i = 0; i < this.mChannelCount; i++) {
                arrayList2.add(new Pair(Integer.valueOf(i), 1));
            }
            microphoneInfoMicrophoneInfoFromAudioDeviceInfo.setChannelMapping(arrayList2);
            arrayList.add(microphoneInfoMicrophoneInfoFromAudioDeviceInfo);
        }
        return arrayList;
    }

    private static void postEventFromNative(Object obj, int i, int i2, int i3, Object obj2) {
        MediaRecorder mediaRecorder = (MediaRecorder) ((WeakReference) obj).get();
        if (mediaRecorder != null && mediaRecorder.mEventHandler != null) {
            if (i == 2 && i2 == 1999) {
                Log.v(TAG, "MediaRecorder MEDIA_RECORDER_INFO_CAMERA_RELEASE");
                if (mediaRecorder.mOnCameraReleasedListener != null) {
                    mediaRecorder.mOnCameraReleasedListener.onInfo(mediaRecorder, 1999, 0);
                    return;
                }
                return;
            }
            mediaRecorder.mEventHandler.sendMessage(mediaRecorder.mEventHandler.obtainMessage(i, i2, i3, obj2));
        }
    }

    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    protected void finalize() {
        native_finalize();
    }

    public void setOnCameraReleasedListener(OnInfoListener onInfoListener) {
        this.mOnCameraReleasedListener = onInfoListener;
    }

    public static final class MetricsConstants {
        public static final String AUDIO_BITRATE = "android.media.mediarecorder.audio-bitrate";
        public static final String AUDIO_CHANNELS = "android.media.mediarecorder.audio-channels";
        public static final String AUDIO_SAMPLERATE = "android.media.mediarecorder.audio-samplerate";
        public static final String AUDIO_TIMESCALE = "android.media.mediarecorder.audio-timescale";
        public static final String CAPTURE_FPS = "android.media.mediarecorder.capture-fps";
        public static final String CAPTURE_FPS_ENABLE = "android.media.mediarecorder.capture-fpsenable";
        public static final String FRAMERATE = "android.media.mediarecorder.frame-rate";
        public static final String HEIGHT = "android.media.mediarecorder.height";
        public static final String MOVIE_TIMESCALE = "android.media.mediarecorder.movie-timescale";
        public static final String ROTATION = "android.media.mediarecorder.rotation";
        public static final String VIDEO_BITRATE = "android.media.mediarecorder.video-bitrate";
        public static final String VIDEO_IFRAME_INTERVAL = "android.media.mediarecorder.video-iframe-interval";
        public static final String VIDEO_LEVEL = "android.media.mediarecorder.video-encoder-level";
        public static final String VIDEO_PROFILE = "android.media.mediarecorder.video-encoder-profile";
        public static final String VIDEO_TIMESCALE = "android.media.mediarecorder.video-timescale";
        public static final String WIDTH = "android.media.mediarecorder.width";

        private MetricsConstants() {
        }
    }
}
