package android.media;

import android.app.ActivityThread;
import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaTimeProvider;
import android.media.SubtitleController;
import android.media.SubtitleTrack;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.util.Preconditions;
import com.mediatek.common.MPlugin;
import com.mediatek.common.media.IOmaSettingHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import libcore.io.IoBridge;
import libcore.io.Libcore;

public class MediaPlayer extends PlayerBase implements SubtitleController.Listener {
    public static final boolean APPLY_METADATA_FILTER = true;
    public static final boolean BYPASS_METADATA_FILTER = false;
    private static final String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE = 2;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE_FD = 3;
    private static final int INVOKE_ID_DESELECT_TRACK = 5;
    private static final int INVOKE_ID_GET_SELECTED_TRACK = 7;
    private static final int INVOKE_ID_GET_TRACK_INFO = 1;
    private static final int INVOKE_ID_SELECT_TRACK = 4;
    private static final int INVOKE_ID_SET_VIDEO_SCALE_MODE = 6;
    private static final int KEY_PARAMETER_AUDIO_ATTRIBUTES = 1400;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_DURATION_UPDATE = 300;
    private static final int MEDIA_ERROR = 100;
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_SYSTEM = Integer.MIN_VALUE;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    private static final int MEDIA_INFO = 200;
    public static final int MEDIA_INFO_AUDIO_NOT_SUPPORTED = 862;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_PAUSE_COMPLETED = 858;
    public static final int MEDIA_INFO_PLAY_COMPLETED = 859;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_VIDEO_NOT_SUPPORTED = 860;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    private static final int MEDIA_META_DATA = 202;
    public static final String MEDIA_MIMETYPE_TEXT_CEA_608 = "text/cea-608";
    public static final String MEDIA_MIMETYPE_TEXT_CEA_708 = "text/cea-708";
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
    public static final String MEDIA_MIMETYPE_TEXT_VTT = "text/vtt";
    private static final int MEDIA_NOP = 0;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_PAUSE_COMPLETE = 600;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_PLAY_COMPLETE = 601;
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_TIMED_TEXT = 99;
    public static final boolean METADATA_ALL = false;
    public static final boolean METADATA_UPDATE_ONLY = true;
    public static final int PLAYBACK_RATE_AUDIO_MODE_DEFAULT = 0;
    public static final int PLAYBACK_RATE_AUDIO_MODE_RESAMPLE = 2;
    public static final int PLAYBACK_RATE_AUDIO_MODE_STRETCH = 1;
    private static final String TAG = "MediaPlayer";
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    private boolean mBypassInterruptionPolicy;
    private EventHandler mEventHandler;
    private BitSet mInbandTrackIndices;
    private Vector<Pair<Integer, SubtitleTrack>> mIndexTrackPairs;
    private int mListenerContext;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnCompletionListener mOnCompletionListener;
    private OnDurationUpdateListener mOnDurationUpdateListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnPreparedListener mOnPreparedListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnSubtitleDataListener mOnSubtitleDataListener;
    private OnTimedMetaDataAvailableListener mOnTimedMetaDataAvailableListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private Vector<InputStream> mOpenSubtitleSources;
    private boolean mScreenOnWhilePlaying;
    private int mSelectedSubtitleTrackIndex;
    private boolean mStayAwake;
    private int mStreamType;
    private SubtitleController mSubtitleController;
    private OnSubtitleDataListener mSubtitleDataListener;
    private SurfaceHolder mSurfaceHolder;
    private TimeProvider mTimeProvider;
    private int mUsage;
    private PowerManager.WakeLock mWakeLock;

    public interface OnBufferingUpdateListener {
        void onBufferingUpdate(MediaPlayer mediaPlayer, int i);
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mediaPlayer);
    }

    public interface OnDurationUpdateListener {
        void onDurationUpdate(MediaPlayer mediaPlayer, int i);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnInfoListener {
        boolean onInfo(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnPreparedListener {
        void onPrepared(MediaPlayer mediaPlayer);
    }

    public interface OnSeekCompleteListener {
        void onSeekComplete(MediaPlayer mediaPlayer);
    }

    public interface OnSubtitleDataListener {
        void onSubtitleData(MediaPlayer mediaPlayer, SubtitleData subtitleData);
    }

    public interface OnTimedMetaDataAvailableListener {
        void onTimedMetaDataAvailable(MediaPlayer mediaPlayer, TimedMetaData timedMetaData);
    }

    public interface OnTimedTextListener {
        void onTimedText(MediaPlayer mediaPlayer, TimedText timedText);
    }

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i2);
    }

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _pause() throws IllegalStateException;

    private native void _prepare() throws IllegalStateException, IOException;

    private native void _release();

    private native void _reset();

    private native void _setAudioStreamType(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setDataSource(MediaDataSource mediaDataSource) throws IllegalStateException, IllegalArgumentException;

    private native void _setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IllegalStateException, IOException, IllegalArgumentException;

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native void getParameter(int i, Parcel parcel);

    private native void nativeSetDataSource(IBinder iBinder, String str, String[] strArr, String[] strArr2) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException;

    private final native void native_finalize();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    public static native int native_pullBatteryData(Parcel parcel);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native int native_setRetransmitEndpoint(String str, int i);

    private final native void native_setup(Object obj);

    private native boolean setParameter(int i, Parcel parcel);

    public native void attachAuxEffect(int i);

    public native int getAudioSessionId();

    public native int getCurrentPosition();

    public native int getDuration();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    public native int getVideoHeight();

    public native int getVideoWidth();

    public native boolean isLooping();

    public native boolean isPlaying();

    public native void prepareAsync() throws IllegalStateException;

    public native void seekTo(int i) throws IllegalStateException;

    public native void setAudioSessionId(int i) throws IllegalStateException, IllegalArgumentException;

    public native void setLooping(boolean z);

    public native void setNextMediaPlayer(MediaPlayer mediaPlayer);

    public native void setPlaybackParams(PlaybackParams playbackParams);

    public native void setSyncParams(SyncParams syncParams);

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public MediaPlayer() {
        super(new AudioAttributes.Builder().build());
        this.mWakeLock = null;
        this.mStreamType = Integer.MIN_VALUE;
        this.mUsage = -1;
        this.mIndexTrackPairs = new Vector<>();
        this.mInbandTrackIndices = new BitSet();
        this.mSelectedSubtitleTrackIndex = -1;
        this.mSubtitleDataListener = new OnSubtitleDataListener() {
            @Override
            public void onSubtitleData(MediaPlayer mp, SubtitleData data) {
                int index = data.getTrackIndex();
                synchronized (MediaPlayer.this.mIndexTrackPairs) {
                    for (Pair<Integer, SubtitleTrack> p : MediaPlayer.this.mIndexTrackPairs) {
                        if (p.first != null && ((Integer) p.first).intValue() == index && p.second != null) {
                            SubtitleTrack track = (SubtitleTrack) p.second;
                            track.onData(data);
                        }
                    }
                }
            }
        };
        Looper looper = Looper.myLooper();
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            Looper looper2 = Looper.getMainLooper();
            if (looper2 != null) {
                this.mEventHandler = new EventHandler(this, looper2);
            } else {
                this.mEventHandler = null;
            }
        }
        this.mTimeProvider = new TimeProvider(this);
        this.mOpenSubtitleSources = new Vector<>();
        native_setup(new WeakReference(this));
    }

    public Parcel newRequest() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInterfaceToken(IMEDIA_PLAYER);
        return parcel;
    }

    public void invoke(Parcel request, Parcel reply) {
        int retcode = native_invoke(request, reply);
        reply.setDataPosition(0);
        if (retcode == 0) {
        } else {
            throw new RuntimeException("failure code: " + retcode);
        }
    }

    public void setDisplay(SurfaceHolder sh) {
        Surface surface;
        this.mSurfaceHolder = sh;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setSurface(Surface surface) {
        if (this.mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        this.mSurfaceHolder = null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setVideoScalingMode(int mode) {
        if (!isVideoScalingModeSupported(mode)) {
            String msg = "Scaling mode " + mode + " is not supported";
            throw new IllegalArgumentException(msg);
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(6);
            request.writeInt(mode);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public static MediaPlayer create(Context context, Uri uri) {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {
        int s = AudioSystem.newAudioSessionId();
        if (s <= 0) {
            s = 0;
        }
        return create(context, uri, holder, null, s);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            MediaPlayer mp = new MediaPlayer();
            AudioAttributes aa = audioAttributes != null ? audioAttributes : new AudioAttributes.Builder().build();
            mp.setAudioAttributes(aa);
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(context, uri);
            if (holder != null) {
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public static MediaPlayer create(Context context, int resid) {
        int s = AudioSystem.newAudioSessionId();
        if (s <= 0) {
            s = 0;
        }
        return create(context, resid, null, s);
    }

    public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) {
                return null;
            }
            MediaPlayer mp = new MediaPlayer();
            AudioAttributes aa = audioAttributes != null ? audioAttributes : new AudioAttributes.Builder().build();
            mp.setAudioAttributes(aa);
            mp.setAudioSessionId(audioSessionId);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            return null;
        } catch (IllegalArgumentException ex2) {
            Log.d(TAG, "create failed:", ex2);
            return null;
        } catch (SecurityException ex3) {
            Log.d(TAG, "create failed:", ex3);
            return null;
        }
    }

    public void setDataSource(Context context, Uri uri) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(context, uri, (Map<String, String>) null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        ContentResolver resolver = context.getContentResolver();
        String scheme = uri.getScheme();
        if (scheme == null) {
            setDataSource(uri.toString());
            return;
        }
        if ("file".equals(scheme)) {
            setDataSource(uri.getPath());
            return;
        }
        if ("content".equals(scheme) && "settings".equals(uri.getAuthority())) {
            int type = RingtoneManager.getDefaultType(uri);
            Uri cacheUri = RingtoneManager.getCacheForType(type);
            Uri actualUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
            if (attemptDataSource(resolver, cacheUri) || attemptDataSource(resolver, actualUri)) {
                return;
            }
            setDataSource(uri.toString(), headers);
            return;
        }
        if (attemptDataSource(resolver, uri)) {
            return;
        }
        IOmaSettingHelper helper = (IOmaSettingHelper) MPlugin.createInstance(IOmaSettingHelper.class.getName(), context);
        if (helper != null) {
            headers = helper.setSettingHeader(context, uri, headers);
        } else {
            Log.w(TAG, "IOmaSettingHelper plugin returns null, uses default headers");
        }
        setDataSource(uri.toString(), headers);
    }

    private boolean attemptDataSource(ContentResolver resolver, Uri uri) {
        Throwable th = null;
        AssetFileDescriptor afd = null;
        try {
            try {
                afd = resolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
                setDataSource(afd);
                if (afd != null) {
                    try {
                        afd.close();
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                if (th != null) {
                    throw th;
                }
                return true;
            } catch (IOException | NullPointerException | SecurityException ex) {
                Log.w(TAG, "Couldn't open " + uri + ": " + ex);
                return false;
            }
        } catch (Throwable th3) {
            th = th3;
            if (afd != null) {
            }
            if (th == null) {
            }
        }
    }

    public void setDataSource(String path) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(path, (String[]) null, (String[]) null);
    }

    public void setDataSource(String path, Map<String, String> headers) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        String[] keys = null;
        String[] values = null;
        if (headers != null) {
            keys = new String[headers.size()];
            values = new String[headers.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                keys[i] = entry.getKey();
                values[i] = entry.getValue();
                i++;
            }
        }
        setDataSource(path, keys, values);
    }

    private void setDataSource(String path, String[] keys, String[] values) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();
        if ("file".equals(scheme)) {
            path = uri.getPath();
        } else if (scheme != null) {
            nativeSetDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(path), path, keys, values);
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            setDataSource(fd);
            is.close();
            return;
        }
        throw new IOException("setDataSource failed.");
    }

    public void setDataSource(AssetFileDescriptor afd) throws IllegalStateException, IOException, IllegalArgumentException {
        Preconditions.checkNotNull(afd);
        if (afd.getDeclaredLength() < 0) {
            setDataSource(afd.getFileDescriptor());
        } else {
            setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
        }
    }

    public void setDataSource(FileDescriptor fd) throws IllegalStateException, IOException, IllegalArgumentException {
        setDataSource(fd, 0L, 576460752303423487L);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IllegalStateException, IOException, IllegalArgumentException {
        _setDataSource(fd, offset, length);
    }

    public void setDataSource(MediaDataSource dataSource) throws IllegalStateException, IllegalArgumentException {
        _setDataSource(dataSource);
    }

    public void prepare() throws IllegalStateException, IOException {
        _prepare();
        scanInternalSubtitleTracks();
    }

    public void start() throws IllegalStateException {
        baseStart();
        stayAwake(true);
        _start();
    }

    private int getAudioStreamType() {
        if (this.mStreamType == Integer.MIN_VALUE) {
            this.mStreamType = _getAudioStreamType();
        }
        return this.mStreamType;
    }

    public void stop() throws IllegalStateException {
        stayAwake(false);
        _stop();
    }

    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
    }

    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (SystemProperties.getBoolean("audio.offload.ignore_setawake", false)) {
            Log.w(TAG, "IGNORING setWakeMode " + mode);
            return;
        }
        if (this.mWakeLock != null) {
            if (this.mWakeLock.isHeld()) {
                washeld = true;
                this.mWakeLock.release();
            }
            this.mWakeLock = null;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(536870912 | mode, MediaPlayer.class.getName());
        this.mWakeLock.setReferenceCounted(false);
        if (!washeld) {
            return;
        }
        this.mWakeLock.acquire();
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (this.mScreenOnWhilePlaying == screenOn) {
            return;
        }
        if (screenOn && this.mSurfaceHolder == null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
        }
        this.mScreenOnWhilePlaying = screenOn;
        updateSurfaceScreenOn();
    }

    private void stayAwake(boolean awake) {
        if (this.mWakeLock != null) {
            if (awake && !this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            } else if (!awake && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
        this.mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (this.mSurfaceHolder == null) {
            return;
        }
        this.mSurfaceHolder.setKeepScreenOn(this.mScreenOnWhilePlaying ? this.mStayAwake : false);
    }

    public PlaybackParams easyPlaybackParams(float rate, int audioMode) {
        PlaybackParams params = new PlaybackParams();
        params.allowDefaults();
        switch (audioMode) {
            case 0:
                params.setSpeed(rate).setPitch(1.0f);
                return params;
            case 1:
                params.setSpeed(rate).setPitch(1.0f).setAudioFallbackMode(2);
                return params;
            case 2:
                params.setSpeed(rate).setPitch(rate);
                return params;
            default:
                String msg = "Audio playback mode " + audioMode + " is not supported";
                throw new IllegalArgumentException(msg);
        }
    }

    public MediaTimestamp getTimestamp() {
        try {
            return new MediaTimestamp(((long) getCurrentPosition()) * 1000, System.nanoTime(), isPlaying() ? getPlaybackParams().getSpeed() : 0.0f);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public Metadata getMetadata(boolean update_only, boolean apply_filter) {
        Parcel reply = Parcel.obtain();
        Metadata data = new Metadata();
        if (!native_getMetadata(update_only, apply_filter, reply)) {
            reply.recycle();
            return null;
        }
        if (!data.parse(reply)) {
            reply.recycle();
            return null;
        }
        return data;
    }

    public int setMetadataFilter(Set<Integer> allow, Set<Integer> block) {
        Parcel request = newRequest();
        int capacity = request.dataSize() + ((allow.size() + 1 + 1 + block.size()) * 4);
        if (request.dataCapacity() < capacity) {
            request.setDataCapacity(capacity);
        }
        request.writeInt(allow.size());
        for (Integer t : allow) {
            request.writeInt(t.intValue());
        }
        request.writeInt(block.size());
        for (Integer t2 : block) {
            request.writeInt(t2.intValue());
        }
        return native_setMetadataFilter(request);
    }

    public void release() {
        baseRelease();
        stayAwake(false);
        updateSurfaceScreenOn();
        this.mOnPreparedListener = null;
        this.mOnBufferingUpdateListener = null;
        this.mOnCompletionListener = null;
        this.mOnSeekCompleteListener = null;
        this.mOnErrorListener = null;
        this.mOnInfoListener = null;
        this.mOnVideoSizeChangedListener = null;
        this.mOnTimedTextListener = null;
        this.mOnDurationUpdateListener = null;
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        this.mOnSubtitleDataListener = null;
        _release();
    }

    public void reset() {
        this.mSelectedSubtitleTrackIndex = -1;
        synchronized (this.mOpenSubtitleSources) {
            for (InputStream is : this.mOpenSubtitleSources) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            this.mOpenSubtitleSources.clear();
        }
        if (this.mSubtitleController != null) {
            this.mSubtitleController.reset();
        }
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        stayAwake(false);
        _reset();
        if (this.mEventHandler != null) {
            this.mEventHandler.removeCallbacksAndMessages(null);
        }
        synchronized (this.mIndexTrackPairs) {
            this.mIndexTrackPairs.clear();
            this.mInbandTrackIndices.clear();
        }
    }

    public void setAudioStreamType(int streamtype) {
        baseUpdateAudioAttributes(new AudioAttributes.Builder().setInternalLegacyStreamType(streamtype).build());
        _setAudioStreamType(streamtype);
        this.mStreamType = streamtype;
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException {
        if (attributes == null) {
            throw new IllegalArgumentException("Cannot set AudioAttributes to null");
        }
        baseUpdateAudioAttributes(attributes);
        this.mUsage = attributes.getUsage();
        this.mBypassInterruptionPolicy = (attributes.getAllFlags() & 64) != 0;
        Parcel pattributes = Parcel.obtain();
        attributes.writeToParcel(pattributes, 1);
        setParameter(KEY_PARAMETER_AUDIO_ATTRIBUTES, pattributes);
        pattributes.recycle();
    }

    public void setVolume(float leftVolume, float rightVolume) {
        baseSetVolume(leftVolume, rightVolume);
    }

    @Override
    void playerSetVolume(float leftVolume, float rightVolume) {
        _setVolume(leftVolume, rightVolume);
    }

    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    public boolean setParameter(int key, String value) {
        Parcel p = Parcel.obtain();
        p.writeString(value);
        boolean ret = setParameter(key, p);
        p.recycle();
        return ret;
    }

    public boolean setParameter(int key, int value) {
        Parcel p = Parcel.obtain();
        p.writeInt(value);
        boolean ret = setParameter(key, p);
        p.recycle();
        return ret;
    }

    public Parcel getParcelParameter(int key) {
        Parcel p = Parcel.obtain();
        getParameter(key, p);
        return p;
    }

    public String getStringParameter(int key) {
        Parcel p = Parcel.obtain();
        getParameter(key, p);
        String ret = p.readString();
        p.recycle();
        return ret;
    }

    public int getIntParameter(int key) {
        Parcel p = Parcel.obtain();
        getParameter(key, p);
        int ret = p.readInt();
        p.recycle();
        return ret;
    }

    public void setAuxEffectSendLevel(float level) {
        baseSetAuxEffectSendLevel(level);
    }

    @Override
    int playerSetAuxEffectSendLevel(float level) {
        _setAuxEffectSendLevel(level);
        return 0;
    }

    public static class TrackInfo implements Parcelable {
        static final Parcelable.Creator<TrackInfo> CREATOR = new Parcelable.Creator<TrackInfo>() {
            @Override
            public TrackInfo createFromParcel(Parcel in) {
                return new TrackInfo(in);
            }

            @Override
            public TrackInfo[] newArray(int size) {
                return new TrackInfo[size];
            }
        };
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_METADATA = 5;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        final MediaFormat mFormat;
        final int mTrackType;

        public int getTrackType() {
            return this.mTrackType;
        }

        public String getLanguage() {
            String language = this.mFormat.getString("language");
            return language == null ? "und" : language;
        }

        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfo(Parcel in) {
            this.mTrackType = in.readInt();
            String mime = in.readString();
            String language = in.readString();
            this.mFormat = MediaFormat.createSubtitleFormat(mime, language);
            if (this.mTrackType != 4) {
                return;
            }
            this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, in.readInt());
            this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, in.readInt());
            this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, in.readInt());
        }

        TrackInfo(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mTrackType);
            dest.writeString(this.mFormat.getString(MediaFormat.KEY_MIME));
            dest.writeString(getLanguage());
            if (this.mTrackType != 4) {
                return;
            }
            dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
            dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
            dest.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
        }

        public String toString() {
            StringBuilder out = new StringBuilder(128);
            out.append(getClass().getName());
            out.append('{');
            switch (this.mTrackType) {
                case 1:
                    out.append("VIDEO");
                    break;
                case 2:
                    out.append("AUDIO");
                    break;
                case 3:
                    out.append("TIMEDTEXT");
                    break;
                case 4:
                    out.append("SUBTITLE");
                    break;
                default:
                    out.append("UNKNOWN");
                    break;
            }
            out.append(", ").append(this.mFormat.toString());
            out.append("}");
            return out.toString();
        }
    }

    public TrackInfo[] getTrackInfo() throws IllegalStateException {
        TrackInfo[] allTrackInfo;
        TrackInfo[] trackInfo = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            allTrackInfo = new TrackInfo[this.mIndexTrackPairs.size()];
            for (int i = 0; i < allTrackInfo.length; i++) {
                Pair<Integer, SubtitleTrack> p = this.mIndexTrackPairs.get(i);
                if (p.first != null) {
                    allTrackInfo[i] = trackInfo[((Integer) p.first).intValue()];
                } else {
                    SubtitleTrack track = (SubtitleTrack) p.second;
                    allTrackInfo[i] = new TrackInfo(track.getTrackType(), track.getFormat());
                }
            }
        }
        return allTrackInfo;
    }

    private TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(1);
            invoke(request, reply);
            TrackInfo[] trackInfo = (TrackInfo[]) reply.createTypedArray(TrackInfo.CREATOR);
            return trackInfo;
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    private static boolean availableMimeTypeForExternalSource(String mimeType) {
        if (MEDIA_MIMETYPE_TEXT_SUBRIP.equals(mimeType)) {
            return true;
        }
        return false;
    }

    public void setSubtitleAnchor(SubtitleController controller, SubtitleController.Anchor anchor) {
        this.mSubtitleController = controller;
        this.mSubtitleController.setAnchor(anchor);
    }

    private synchronized void setSubtitleAnchor() {
        if (this.mSubtitleController == null) {
            final HandlerThread thread = new HandlerThread("SetSubtitleAnchorThread");
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Context context = ActivityThread.currentApplication();
                    MediaPlayer.this.mSubtitleController = new SubtitleController(context, MediaPlayer.this.mTimeProvider, MediaPlayer.this);
                    MediaPlayer.this.mSubtitleController.setAnchor(new SubtitleController.Anchor() {
                        @Override
                        public void setSubtitleWidget(SubtitleTrack.RenderingWidget subtitleWidget) {
                        }

                        @Override
                        public Looper getSubtitleLooper() {
                            return Looper.getMainLooper();
                        }
                    });
                    thread.getLooper().quitSafely();
                }
            });
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "failed to join SetSubtitleAnchorThread");
            }
        }
    }

    @Override
    public void onSubtitleTrackSelected(SubtitleTrack track) {
        if (this.mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, false);
            } catch (IllegalStateException e) {
            }
            this.mSelectedSubtitleTrackIndex = -1;
        }
        setOnSubtitleDataListener(null);
        if (track == null) {
            return;
        }
        synchronized (this.mIndexTrackPairs) {
            Iterator p$iterator = this.mIndexTrackPairs.iterator();
            while (true) {
                if (!p$iterator.hasNext()) {
                    break;
                }
                Pair<Integer, SubtitleTrack> p = (Pair) p$iterator.next();
                if (p.first != null && p.second == track) {
                    break;
                }
            }
        }
        if (this.mSelectedSubtitleTrackIndex < 0) {
            return;
        }
        try {
            selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, true);
        } catch (IllegalStateException e2) {
        }
        setOnSubtitleDataListener(this.mSubtitleDataListener);
    }

    public void addSubtitleSource(final InputStream is, final MediaFormat format) throws IllegalStateException {
        Log.d(TAG, "addSubtitleSource: MediaFormat = " + format);
        if (is != null) {
            synchronized (this.mOpenSubtitleSources) {
                this.mOpenSubtitleSources.add(is);
            }
        } else {
            Log.w(TAG, "addSubtitleSource called with null InputStream");
        }
        getMediaTimeProvider();
        final HandlerThread thread = new HandlerThread("SubtitleReadThread", 9);
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        handler.post(new Runnable() {
            private int addTrack() {
                if (is == null || MediaPlayer.this.mSubtitleController == null) {
                    Log.e(MediaPlayer.TAG, "addSubtitleSource: MEDIA_INFO_UNSUPPORTED_SUBTITLE");
                    return MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE;
                }
                SubtitleTrack track = MediaPlayer.this.mSubtitleController.addTrack(format);
                if (track == null) {
                    Log.e(MediaPlayer.TAG, "addSubtitleSource: MEDIA_INFO_UNSUPPORTED_SUBTITLE");
                    return MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE;
                }
                Scanner scanner = new Scanner(is, "UTF-8");
                String contents = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer.this.mOpenSubtitleSources) {
                    MediaPlayer.this.mOpenSubtitleSources.remove(is);
                }
                scanner.close();
                synchronized (MediaPlayer.this.mIndexTrackPairs) {
                    MediaPlayer.this.mIndexTrackPairs.add(Pair.create(null, track));
                }
                Handler h = MediaPlayer.this.mTimeProvider.mEventHandler;
                Pair<SubtitleTrack, byte[]> trackData = Pair.create(track, contents.getBytes());
                Message m = h.obtainMessage(1, 4, 0, trackData);
                h.sendMessage(m);
                return 803;
            }

            @Override
            public void run() {
                int res = addTrack();
                if (MediaPlayer.this.mEventHandler != null) {
                    Message m = MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null);
                    MediaPlayer.this.mEventHandler.sendMessage(m);
                }
                thread.getLooper().quitSafely();
            }
        });
    }

    private void scanInternalSubtitleTracks() {
        if (this.mSubtitleController == null) {
            Log.d(TAG, "setSubtitleAnchor in MediaPlayer");
            setSubtitleAnchor();
        }
        populateInbandTracks();
        if (this.mSubtitleController == null) {
            return;
        }
        this.mSubtitleController.selectDefaultTrack();
    }

    private void populateInbandTracks() {
        TrackInfo[] tracks = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            for (int i = 0; i < tracks.length; i++) {
                if (!this.mInbandTrackIndices.get(i)) {
                    this.mInbandTrackIndices.set(i);
                    if (tracks[i].getTrackType() == 4) {
                        SubtitleTrack track = this.mSubtitleController.addTrack(tracks[i].getFormat());
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), track));
                    } else {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), null));
                    }
                }
            }
        }
    }

    public void addTimedTextSource(String path, String mimeType) throws IllegalStateException, IOException, IllegalArgumentException {
        if (!availableMimeTypeForExternalSource(mimeType)) {
            String msg = "Illegal mimeType for timed text source: " + mimeType;
            throw new IllegalArgumentException(msg);
        }
        File file = new File(path);
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            addTimedTextSource(fd, mimeType);
            is.close();
            return;
        }
        throw new IOException(path);
    }

    public void addTimedTextSource(Context context, Uri uri, String mimeType) throws IllegalStateException, IOException, IllegalArgumentException {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("file")) {
            addTimedTextSource(uri.getPath(), mimeType);
            return;
        }
        AutoCloseable autoCloseable = null;
        try {
            try {
                ContentResolver resolver = context.getContentResolver();
                AssetFileDescriptor fd = resolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
                if (fd == null) {
                    Log.e(TAG, "addTimedTextSource: Null fd! uri=" + uri);
                    if (fd != null) {
                        fd.close();
                        return;
                    }
                    return;
                }
                addTimedTextSource(fd.getFileDescriptor(), mimeType);
                if (fd != null) {
                    fd.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "addTimedTextSource: IOException! uri=" + uri);
                if (0 != 0) {
                    autoCloseable.close();
                }
            } catch (SecurityException ex) {
                Log.e(TAG, "addTimedTextSource: SecurityException! uri=" + uri, ex);
                if (0 != 0) {
                    autoCloseable.close();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                autoCloseable.close();
            }
            throw th;
        }
    }

    public void addTimedTextSource(FileDescriptor fd, String mimeType) throws IllegalStateException, IllegalArgumentException {
        addTimedTextSource(fd, 0L, 576460752303423487L, mimeType);
    }

    public void addTimedTextSource(FileDescriptor fd, final long offset, final long length, String mime) throws IllegalStateException, IllegalArgumentException {
        if (!availableMimeTypeForExternalSource(mime)) {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + mime);
        }
        try {
            final FileDescriptor fd2 = Libcore.os.dup(fd);
            MediaFormat fFormat = new MediaFormat();
            fFormat.setString(MediaFormat.KEY_MIME, mime);
            fFormat.setInteger(MediaFormat.KEY_IS_TIMED_TEXT, 1);
            if (this.mSubtitleController == null) {
                setSubtitleAnchor();
            }
            if (!this.mSubtitleController.hasRendererFor(fFormat)) {
                Context context = ActivityThread.currentApplication();
                this.mSubtitleController.registerRenderer(new SRTRenderer(context, this.mEventHandler));
            }
            final SubtitleTrack track = this.mSubtitleController.addTrack(fFormat);
            synchronized (this.mIndexTrackPairs) {
                this.mIndexTrackPairs.add(Pair.create(null, track));
            }
            getMediaTimeProvider();
            final HandlerThread thread = new HandlerThread("TimedTextReadThread", 9);
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            handler.post(new Runnable() {
                private int addTrack() {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        Libcore.os.lseek(fd2, offset, OsConstants.SEEK_SET);
                        byte[] buffer = new byte[4096];
                        long total = 0;
                        while (total < length) {
                            int bytesToRead = (int) Math.min(buffer.length, length - total);
                            int bytes = IoBridge.read(fd2, buffer, 0, bytesToRead);
                            if (bytes < 0) {
                                break;
                            }
                            bos.write(buffer, 0, bytes);
                            total += (long) bytes;
                        }
                        Handler h = MediaPlayer.this.mTimeProvider.mEventHandler;
                        Pair<SubtitleTrack, byte[]> trackData = Pair.create(track, bos.toByteArray());
                        Message m = h.obtainMessage(1, 4, 0, trackData);
                        h.sendMessage(m);
                        return 803;
                    } catch (Exception e) {
                        Log.e(MediaPlayer.TAG, e.getMessage(), e);
                        return MediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR;
                    }
                }

                @Override
                public void run() {
                    int res = addTrack();
                    if (MediaPlayer.this.mEventHandler != null) {
                        Message m = MediaPlayer.this.mEventHandler.obtainMessage(200, res, 0, null);
                        MediaPlayer.this.mEventHandler.sendMessage(m);
                    }
                    thread.getLooper().quitSafely();
                }
            });
        } catch (ErrnoException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public int getSelectedTrack(int trackType) throws IllegalStateException {
        SubtitleTrack subtitleTrack;
        if (this.mSubtitleController != null && ((trackType == 4 || trackType == 3) && (subtitleTrack = this.mSubtitleController.getSelectedTrack()) != null)) {
            synchronized (this.mIndexTrackPairs) {
                for (int i = 0; i < this.mIndexTrackPairs.size(); i++) {
                    if (this.mIndexTrackPairs.get(i).second == subtitleTrack && subtitleTrack.getTrackType() == trackType) {
                        return i;
                    }
                }
            }
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(7);
            request.writeInt(trackType);
            invoke(request, reply);
            int inbandTrackIndex = reply.readInt();
            synchronized (this.mIndexTrackPairs) {
                for (int i2 = 0; i2 < this.mIndexTrackPairs.size(); i2++) {
                    Pair<Integer, SubtitleTrack> p = this.mIndexTrackPairs.get(i2);
                    if (p.first != null && ((Integer) p.first).intValue() == inbandTrackIndex) {
                        return i2;
                    }
                }
                return -1;
            }
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public void selectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, true);
    }

    public void deselectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, false);
    }

    private void selectOrDeselectTrack(int index, boolean select) throws IllegalStateException {
        populateInbandTracks();
        try {
            Pair<Integer, SubtitleTrack> p = this.mIndexTrackPairs.get(index);
            SubtitleTrack track = (SubtitleTrack) p.second;
            if (track == null) {
                selectOrDeselectInbandTrack(((Integer) p.first).intValue(), select);
                return;
            }
            if (this.mSubtitleController == null) {
                return;
            }
            if (!select) {
                if (this.mSubtitleController.getSelectedTrack() == track) {
                    this.mSubtitleController.selectTrack(null);
                    return;
                } else {
                    Log.w(TAG, "trying to deselect track that was not selected");
                    return;
                }
            }
            if (track.getTrackType() == 3) {
                int ttIndex = getSelectedTrack(3);
                synchronized (this.mIndexTrackPairs) {
                    if (ttIndex >= 0) {
                        if (ttIndex < this.mIndexTrackPairs.size()) {
                            Pair<Integer, SubtitleTrack> p2 = this.mIndexTrackPairs.get(ttIndex);
                            if (p2.first != null && p2.second == null) {
                                selectOrDeselectInbandTrack(((Integer) p2.first).intValue(), false);
                            }
                        }
                    }
                }
            }
            this.mSubtitleController.selectTrack(track);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private void selectOrDeselectInbandTrack(int index, boolean select) throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(select ? 4 : 5);
            request.writeInt(index);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    public void setRetransmitEndpoint(InetSocketAddress endpoint) throws IllegalStateException, IllegalArgumentException {
        String addrString = null;
        int port = 0;
        if (endpoint != null) {
            addrString = endpoint.getAddress().getHostAddress();
            port = endpoint.getPort();
        }
        int ret = native_setRetransmitEndpoint(addrString, port);
        if (ret == 0) {
        } else {
            throw new IllegalArgumentException("Illegal re-transmit endpoint; native ret " + ret);
        }
    }

    protected void finalize() {
        baseRelease();
        native_finalize();
    }

    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    private class EventHandler extends Handler {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            this.mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer.TAG, "mediaplayer went away with unhandled events");
                return;
            }
            Log.d(MediaPlayer.TAG, "handleMessage msg:(" + msg.what + ", " + msg.arg1 + ", " + msg.arg2 + ")");
            switch (msg.what) {
                case 0:
                    return;
                case 1:
                    try {
                        MediaPlayer.this.scanInternalSubtitleTracks();
                        break;
                    } catch (RuntimeException e) {
                        Message msg2 = obtainMessage(100, 1, MediaPlayer.MEDIA_ERROR_UNSUPPORTED, null);
                        sendMessage(msg2);
                    }
                    OnPreparedListener onPreparedListener = MediaPlayer.this.mOnPreparedListener;
                    if (onPreparedListener != null) {
                        onPreparedListener.onPrepared(this.mMediaPlayer);
                        return;
                    }
                    return;
                case 2:
                    OnCompletionListener onCompletionListener = MediaPlayer.this.mOnCompletionListener;
                    if (onCompletionListener != null) {
                        onCompletionListener.onCompletion(this.mMediaPlayer);
                    }
                    MediaPlayer.this.stayAwake(false);
                    return;
                case 3:
                    OnBufferingUpdateListener onBufferingUpdateListener = MediaPlayer.this.mOnBufferingUpdateListener;
                    if (onBufferingUpdateListener != null) {
                        onBufferingUpdateListener.onBufferingUpdate(this.mMediaPlayer, msg.arg1);
                        return;
                    }
                    return;
                case 4:
                    OnSeekCompleteListener onSeekCompleteListener = MediaPlayer.this.mOnSeekCompleteListener;
                    if (onSeekCompleteListener != null) {
                        onSeekCompleteListener.onSeekComplete(this.mMediaPlayer);
                    }
                    break;
                case 5:
                    OnVideoSizeChangedListener onVideoSizeChangedListener = MediaPlayer.this.mOnVideoSizeChangedListener;
                    if (onVideoSizeChangedListener != null) {
                        onVideoSizeChangedListener.onVideoSizeChanged(this.mMediaPlayer, msg.arg1, msg.arg2);
                        return;
                    }
                    return;
                case 6:
                case 7:
                    TimeProvider timeProvider = MediaPlayer.this.mTimeProvider;
                    if (timeProvider == null) {
                        return;
                    }
                    timeProvider.onPaused(msg.what == 7);
                    return;
                case 8:
                    TimeProvider timeProvider2 = MediaPlayer.this.mTimeProvider;
                    if (timeProvider2 == null) {
                        return;
                    }
                    timeProvider2.onStopped();
                    return;
                case 9:
                    break;
                case 99:
                    OnTimedTextListener onTimedTextListener = MediaPlayer.this.mOnTimedTextListener;
                    if (onTimedTextListener == null) {
                        return;
                    }
                    if (msg.obj == null) {
                        onTimedTextListener.onTimedText(this.mMediaPlayer, null);
                        return;
                    } else {
                        if (msg.obj instanceof Parcel) {
                            Parcel parcel = (Parcel) msg.obj;
                            TimedText text = new TimedText(parcel);
                            parcel.recycle();
                            onTimedTextListener.onTimedText(this.mMediaPlayer, text);
                            return;
                        }
                        return;
                    }
                case 100:
                    Log.e(MediaPlayer.TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                    boolean error_was_handled = false;
                    OnErrorListener onErrorListener = MediaPlayer.this.mOnErrorListener;
                    if (onErrorListener != null) {
                        error_was_handled = onErrorListener.onError(this.mMediaPlayer, msg.arg1, msg.arg2);
                    }
                    OnCompletionListener onCompletionListener2 = MediaPlayer.this.mOnCompletionListener;
                    if (onCompletionListener2 != null && !error_was_handled) {
                        onCompletionListener2.onCompletion(this.mMediaPlayer);
                    }
                    MediaPlayer.this.stayAwake(false);
                    return;
                case 200:
                    switch (msg.arg1) {
                        case 700:
                            Log.i(MediaPlayer.TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                            break;
                        case 701:
                        case 702:
                            TimeProvider timeProvider3 = MediaPlayer.this.mTimeProvider;
                            if (timeProvider3 != null) {
                                timeProvider3.onBuffering(msg.arg1 == 701);
                            }
                            break;
                        case 802:
                            try {
                                MediaPlayer.this.scanInternalSubtitleTracks();
                                break;
                            } catch (RuntimeException e2) {
                                Message msg22 = obtainMessage(100, 1, MediaPlayer.MEDIA_ERROR_UNSUPPORTED, null);
                                sendMessage(msg22);
                                break;
                            }
                        case 803:
                            msg.arg1 = 802;
                            if (MediaPlayer.this.mSubtitleController != null) {
                                MediaPlayer.this.mSubtitleController.selectDefaultTrack();
                            }
                            break;
                    }
                    OnInfoListener onInfoListener = MediaPlayer.this.mOnInfoListener;
                    if (onInfoListener != null) {
                        onInfoListener.onInfo(this.mMediaPlayer, msg.arg1, msg.arg2);
                        return;
                    }
                    return;
                case 201:
                    OnSubtitleDataListener onSubtitleDataListener = MediaPlayer.this.mOnSubtitleDataListener;
                    if (onSubtitleDataListener != null && (msg.obj instanceof Parcel)) {
                        Parcel parcel2 = (Parcel) msg.obj;
                        SubtitleData data = new SubtitleData(parcel2);
                        parcel2.recycle();
                        onSubtitleDataListener.onSubtitleData(this.mMediaPlayer, data);
                        return;
                    }
                    return;
                case 202:
                    OnTimedMetaDataAvailableListener onTimedMetaDataAvailableListener = MediaPlayer.this.mOnTimedMetaDataAvailableListener;
                    if (onTimedMetaDataAvailableListener != null && (msg.obj instanceof Parcel)) {
                        Parcel parcel3 = (Parcel) msg.obj;
                        TimedMetaData data2 = TimedMetaData.createTimedMetaDataFromParcel(parcel3);
                        parcel3.recycle();
                        onTimedMetaDataAvailableListener.onTimedMetaDataAvailable(this.mMediaPlayer, data2);
                        return;
                    }
                    return;
                case 300:
                    Log.v(MediaPlayer.TAG, "Duration update (duration=" + msg.arg1 + ")");
                    if (MediaPlayer.this.mOnDurationUpdateListener == null) {
                        return;
                    }
                    MediaPlayer.this.mOnDurationUpdateListener.onDurationUpdate(this.mMediaPlayer, msg.arg1);
                    return;
                case 600:
                    if (MediaPlayer.this.mOnInfoListener == null) {
                        return;
                    }
                    if (msg.arg1 != 0) {
                        Log.e(MediaPlayer.TAG, "MEDIA_PAUSE_COMPLETE failed " + msg.arg1);
                    }
                    MediaPlayer.this.mOnInfoListener.onInfo(this.mMediaPlayer, MediaPlayer.MEDIA_INFO_PAUSE_COMPLETED, msg.arg1);
                    return;
                case 601:
                    if (MediaPlayer.this.mOnInfoListener == null) {
                        return;
                    }
                    if (msg.arg1 != 0) {
                        Log.e(MediaPlayer.TAG, "MEDIA_PLAY_COMPLETE failed " + msg.arg1);
                    }
                    MediaPlayer.this.mOnInfoListener.onInfo(this.mMediaPlayer, MediaPlayer.MEDIA_INFO_PLAY_COMPLETED, msg.arg1);
                    return;
                default:
                    Log.e(MediaPlayer.TAG, "Unknown message type " + msg.what);
                    return;
            }
            TimeProvider timeProvider4 = MediaPlayer.this.mTimeProvider;
            if (timeProvider4 != null) {
                timeProvider4.onSeekComplete(this.mMediaPlayer);
            }
        }
    }

    private static void postEventFromNative(Object mediaplayer_ref, int what, int arg1, int arg2, Object obj) {
        MediaPlayer mp = (MediaPlayer) ((WeakReference) mediaplayer_ref).get();
        if (mp == null) {
            Log.e(TAG, "postEventFromNative: Null mp! what=" + what + ", arg1=" + arg1 + ", arg2=" + arg2);
            return;
        }
        if (what == 200 && arg1 == 2) {
            mp.start();
        }
        if (mp.mEventHandler == null) {
            return;
        }
        Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
        mp.mEventHandler.sendMessage(m);
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.mOnCompletionListener = listener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        this.mOnBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        this.mOnSeekCompleteListener = listener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        this.mOnVideoSizeChangedListener = listener;
    }

    public void setOnTimedTextListener(OnTimedTextListener listener) {
        this.mOnTimedTextListener = listener;
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener listener) {
        this.mOnSubtitleDataListener = listener;
    }

    public void setOnTimedMetaDataAvailableListener(OnTimedMetaDataAvailableListener listener) {
        this.mOnTimedMetaDataAvailableListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
    }

    public void setOnInfoListener(OnInfoListener listener) {
        this.mOnInfoListener = listener;
    }

    private boolean isVideoScalingModeSupported(int mode) {
        return mode == 1 || mode == 2;
    }

    static class TimeProvider implements OnSeekCompleteListener, MediaTimeProvider {
        private static final long MAX_EARLY_CALLBACK_US = 1000;
        private static final long MAX_NS_WITHOUT_POSITION_CHECK = 5000000000L;
        private static final int NOTIFY = 1;
        private static final int NOTIFY_SEEK = 3;
        private static final int NOTIFY_STOP = 2;
        private static final int NOTIFY_TIME = 0;
        private static final int NOTIFY_TRACK_DATA = 4;
        private static final int REFRESH_AND_NOTIFY_TIME = 1;
        private static final String TAG = "MTP";
        private static final long TIME_ADJUSTMENT_RATE = 2;
        private boolean mBuffering;
        private Handler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastNanoTime;
        private long mLastReportedTime;
        private long mLastTimeUs;
        private MediaTimeProvider.OnMediaTimeListener[] mListeners;
        private MediaPlayer mPlayer;
        private boolean mRefresh;
        private long mTimeAdjustment;
        private long[] mTimes;
        private boolean mPaused = true;
        private boolean mStopped = true;
        private boolean mPausing = false;
        private boolean mSeeking = false;
        public boolean DEBUG = false;

        public TimeProvider(MediaPlayer mp) {
            this.mLastTimeUs = 0L;
            this.mRefresh = false;
            this.mPlayer = mp;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
            }
            Looper looper = Looper.myLooper();
            if (looper == null && (looper = Looper.getMainLooper()) == null) {
                this.mHandlerThread = new HandlerThread("MediaPlayerMTPEventThread", -2);
                this.mHandlerThread.start();
                looper = this.mHandlerThread.getLooper();
            }
            this.mEventHandler = new EventHandler(looper);
            this.mListeners = new MediaTimeProvider.OnMediaTimeListener[0];
            this.mTimes = new long[0];
            this.mLastTimeUs = 0L;
            this.mTimeAdjustment = 0L;
        }

        private void scheduleNotification(int type, long delayUs) {
            if (this.mSeeking && (type == 0 || type == 1)) {
                return;
            }
            if (this.DEBUG) {
                Log.v(TAG, "scheduleNotification " + type + " in " + delayUs);
            }
            this.mEventHandler.removeMessages(1);
            Message msg = this.mEventHandler.obtainMessage(1, type, 0);
            this.mEventHandler.sendMessageDelayed(msg, (int) (delayUs / 1000));
        }

        public void close() {
            this.mEventHandler.removeMessages(1);
            if (this.mHandlerThread == null) {
                return;
            }
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }

        protected void finalize() {
            if (this.mHandlerThread == null) {
                return;
            }
            this.mHandlerThread.quitSafely();
        }

        public void onPaused(boolean paused) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onPaused: " + paused);
                }
                if (this.mStopped) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    scheduleNotification(3, 0L);
                } else {
                    this.mPausing = paused;
                    this.mSeeking = false;
                    scheduleNotification(1, 0L);
                }
            }
        }

        public void onBuffering(boolean buffering) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onBuffering: " + buffering);
                }
                this.mBuffering = buffering;
                scheduleNotification(1, 0L);
            }
        }

        public void onStopped() {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onStopped");
                }
                this.mPaused = true;
                this.mStopped = true;
                this.mSeeking = false;
                this.mBuffering = false;
                scheduleNotification(2, 0L);
            }
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            synchronized (this) {
                this.mStopped = false;
                this.mSeeking = true;
                scheduleNotification(3, 0L);
            }
        }

        public void onNewPlayer() {
            if (!this.mRefresh) {
                return;
            }
            synchronized (this) {
                this.mStopped = false;
                this.mSeeking = true;
                this.mBuffering = false;
                scheduleNotification(3, 0L);
            }
        }

        private synchronized void notifySeek() {
            synchronized (this) {
                this.mSeeking = false;
                try {
                    long timeUs = getCurrentTimeUs(true, false);
                    if (this.DEBUG) {
                        Log.d(TAG, "onSeekComplete at " + timeUs);
                    }
                    for (MediaTimeProvider.OnMediaTimeListener listener : this.mListeners) {
                        if (listener == null) {
                            break;
                        }
                        listener.onSeek(timeUs);
                    }
                } catch (IllegalStateException e) {
                    if (this.DEBUG) {
                        Log.d(TAG, "onSeekComplete but no player");
                    }
                    this.mPausing = true;
                    notifyTimedEvent(false);
                }
            }
        }

        private synchronized void notifyTrackData(Pair<SubtitleTrack, byte[]> trackData) {
            SubtitleTrack track = (SubtitleTrack) trackData.first;
            byte[] data = (byte[]) trackData.second;
            track.onData(data, true, -1L);
        }

        private synchronized void notifyStop() {
            for (MediaTimeProvider.OnMediaTimeListener listener : this.mListeners) {
                if (listener == null) {
                    break;
                }
                listener.onStop();
            }
        }

        private int registerListener(MediaTimeProvider.OnMediaTimeListener listener) {
            int i = 0;
            while (i < this.mListeners.length && this.mListeners[i] != listener && this.mListeners[i] != null) {
                i++;
            }
            if (i >= this.mListeners.length) {
                MediaTimeProvider.OnMediaTimeListener[] newListeners = new MediaTimeProvider.OnMediaTimeListener[i + 1];
                long[] newTimes = new long[i + 1];
                System.arraycopy(this.mListeners, 0, newListeners, 0, this.mListeners.length);
                System.arraycopy(this.mTimes, 0, newTimes, 0, this.mTimes.length);
                this.mListeners = newListeners;
                this.mTimes = newTimes;
            }
            if (this.mListeners[i] == null) {
                this.mListeners[i] = listener;
                this.mTimes[i] = -1;
            }
            return i;
        }

        @Override
        public void notifyAt(long timeUs, MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "notifyAt " + timeUs);
                }
                this.mTimes[registerListener(listener)] = timeUs;
                scheduleNotification(0, 0L);
            }
        }

        @Override
        public void scheduleUpdate(MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduleUpdate");
                }
                int i = registerListener(listener);
                if (!this.mStopped) {
                    this.mTimes[i] = 0;
                    scheduleNotification(0, 0L);
                }
            }
        }

        @Override
        public void cancelNotifications(MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized (this) {
                int i = 0;
                while (true) {
                    if (i < this.mListeners.length) {
                        if (this.mListeners[i] == listener) {
                            System.arraycopy(this.mListeners, i + 1, this.mListeners, i, (this.mListeners.length - i) - 1);
                            System.arraycopy(this.mTimes, i + 1, this.mTimes, i, (this.mTimes.length - i) - 1);
                            this.mListeners[this.mListeners.length - 1] = null;
                            this.mTimes[this.mTimes.length - 1] = -1;
                            break;
                        }
                        if (this.mListeners[i] == null) {
                            break;
                        } else {
                            i++;
                        }
                    } else {
                        break;
                    }
                }
                scheduleNotification(0, 0L);
            }
        }

        private synchronized void notifyTimedEvent(boolean refreshTime) {
            long nowUs;
            try {
                nowUs = getCurrentTimeUs(refreshTime, true);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
                this.mPausing = true;
                nowUs = getCurrentTimeUs(refreshTime, true);
            }
            long nextTimeUs = nowUs;
            if (this.mSeeking) {
                return;
            }
            if (this.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("notifyTimedEvent(").append(this.mLastTimeUs).append(" -> ").append(nowUs).append(") from {");
                boolean first = true;
                for (long time : this.mTimes) {
                    if (time != -1) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append(time);
                        first = false;
                    }
                }
                sb.append("}");
                Log.d(TAG, sb.toString());
            }
            Vector<MediaTimeProvider.OnMediaTimeListener> activatedListeners = new Vector<>();
            for (int ix = 0; ix < this.mTimes.length && this.mListeners[ix] != null; ix++) {
                if (this.mTimes[ix] > -1) {
                    if (this.mTimes[ix] <= 1000 + nowUs) {
                        activatedListeners.add(this.mListeners[ix]);
                        if (this.DEBUG) {
                            Log.d(TAG, Environment.MEDIA_REMOVED);
                        }
                        this.mTimes[ix] = -1;
                    } else if (nextTimeUs == nowUs || this.mTimes[ix] < nextTimeUs) {
                        nextTimeUs = this.mTimes[ix];
                    }
                }
            }
            if (nextTimeUs > nowUs && !this.mPaused) {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduling for " + nextTimeUs + " and " + nowUs);
                }
                scheduleNotification(0, nextTimeUs - nowUs);
            } else {
                this.mEventHandler.removeMessages(1);
            }
            for (MediaTimeProvider.OnMediaTimeListener listener : activatedListeners) {
                listener.onTimedEvent(nowUs);
            }
        }

        private long getEstimatedTime(long nanoTime, boolean monotonic) {
            if (this.mPaused) {
                this.mLastReportedTime = this.mLastTimeUs + this.mTimeAdjustment;
            } else {
                long timeSinceRead = (nanoTime - this.mLastNanoTime) / 1000;
                this.mLastReportedTime = this.mLastTimeUs + timeSinceRead;
                if (this.mTimeAdjustment > 0) {
                    long adjustment = this.mTimeAdjustment - (timeSinceRead / 2);
                    if (adjustment <= 0) {
                        this.mTimeAdjustment = 0L;
                    } else {
                        this.mLastReportedTime += adjustment;
                    }
                }
            }
            return this.mLastReportedTime;
        }

        @Override
        public long getCurrentTimeUs(boolean refreshTime, boolean monotonic) throws IllegalStateException {
            synchronized (this) {
                if (this.mPaused && !refreshTime) {
                    return this.mLastReportedTime;
                }
                long nanoTime = System.nanoTime();
                if (refreshTime || nanoTime >= this.mLastNanoTime + MAX_NS_WITHOUT_POSITION_CHECK) {
                    try {
                        this.mLastTimeUs = ((long) this.mPlayer.getCurrentPosition()) * 1000;
                        this.mPaused = this.mPlayer.isPlaying() ? this.mBuffering : true;
                        if (this.DEBUG) {
                            Log.v(TAG, (this.mPaused ? "paused" : "playing") + " at " + this.mLastTimeUs);
                        }
                        this.mLastNanoTime = nanoTime;
                        if (monotonic && this.mLastTimeUs < this.mLastReportedTime) {
                            this.mTimeAdjustment = this.mLastReportedTime - this.mLastTimeUs;
                            if (this.mTimeAdjustment > 1000000) {
                                this.mStopped = false;
                                this.mSeeking = true;
                                scheduleNotification(3, 0L);
                            }
                        } else {
                            this.mTimeAdjustment = 0L;
                        }
                    } catch (IllegalStateException e) {
                        if (this.mPausing) {
                            this.mPausing = false;
                            getEstimatedTime(nanoTime, monotonic);
                            this.mPaused = true;
                            if (this.DEBUG) {
                                Log.d(TAG, "illegal state, but pausing: estimating at " + this.mLastReportedTime);
                            }
                            return this.mLastReportedTime;
                        }
                        throw e;
                    }
                }
                return getEstimatedTime(nanoTime, monotonic);
            }
        }

        private class EventHandler extends Handler {
            public EventHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                }
                switch (msg.arg1) {
                    case 0:
                        TimeProvider.this.notifyTimedEvent(false);
                        break;
                    case 1:
                        TimeProvider.this.notifyTimedEvent(true);
                        break;
                    case 2:
                        TimeProvider.this.notifyStop();
                        break;
                    case 3:
                        TimeProvider.this.notifySeek();
                        break;
                    case 4:
                        TimeProvider.this.notifyTrackData((Pair) msg.obj);
                        break;
                }
            }
        }
    }

    public void setOnDurationUpdateListener(OnDurationUpdateListener listener) {
        this.mOnDurationUpdateListener = listener;
    }
}
