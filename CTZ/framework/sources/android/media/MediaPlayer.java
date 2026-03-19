package android.media;

import android.app.ActivityThread;
import android.app.Application;
import android.app.backup.FullBackup;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioRouting;
import android.media.MediaDrm;
import android.media.MediaTimeProvider;
import android.media.SubtitleController;
import android.media.SubtitleTrack;
import android.media.VolumeShaper;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.TimeUtils;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import libcore.io.IoBridge;
import libcore.io.Streams;

public class MediaPlayer extends PlayerBase implements SubtitleController.Listener, VolumeAutomation, AudioRouting {
    public static final boolean APPLY_METADATA_FILTER = true;
    public static final boolean BYPASS_METADATA_FILTER = false;
    private static final boolean DEBUG;
    private static final String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE = 2;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE_FD = 3;
    private static final int INVOKE_ID_DESELECT_TRACK = 5;
    private static final int INVOKE_ID_GET_SELECTED_TRACK = 7;
    private static final int INVOKE_ID_GET_TRACK_INFO = 1;
    private static final int INVOKE_ID_SELECT_TRACK = 4;
    private static final int INVOKE_ID_SET_VIDEO_SCALE_MODE = 6;
    private static final int KEY_PARAMETER_AUDIO_ATTRIBUTES = 1400;
    private static final int MEDIA_AUDIO_ROUTING_CHANGED = 10000;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_DRM_INFO = 210;
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
    public static final int MEDIA_INFO_AUDIO_NOT_PLAYING = 804;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_VIDEO_NOT_PLAYING = 805;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    private static final int MEDIA_META_DATA = 202;
    public static final String MEDIA_MIMETYPE_TEXT_CEA_608 = "text/cea-608";
    public static final String MEDIA_MIMETYPE_TEXT_CEA_708 = "text/cea-708";
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
    public static final String MEDIA_MIMETYPE_TEXT_VTT = "text/vtt";
    private static final int MEDIA_NOP = 0;
    private static final int MEDIA_NOTIFY_TIME = 98;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_TIMED_TEXT = 99;
    private static final int MEDIA_TIME_DISCONTINUITY = 211;
    public static final boolean METADATA_ALL = false;
    public static final boolean METADATA_UPDATE_ONLY = true;
    public static final int PLAYBACK_RATE_AUDIO_MODE_DEFAULT = 0;
    public static final int PLAYBACK_RATE_AUDIO_MODE_RESAMPLE = 2;
    public static final int PLAYBACK_RATE_AUDIO_MODE_STRETCH = 1;
    public static final int PREPARE_DRM_STATUS_PREPARATION_ERROR = 3;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR = 1;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR = 2;
    public static final int PREPARE_DRM_STATUS_SUCCESS = 0;
    public static final int SEEK_CLOSEST = 3;
    public static final int SEEK_CLOSEST_SYNC = 2;
    public static final int SEEK_NEXT_SYNC = 1;
    public static final int SEEK_PREVIOUS_SYNC = 0;
    private static final String TAG = "MediaPlayer";
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    private boolean mActiveDrmScheme;
    private boolean mBypassInterruptionPolicy;
    private boolean mDrmConfigAllowed;
    private DrmInfo mDrmInfo;
    private boolean mDrmInfoResolved;
    private final Object mDrmLock;
    private MediaDrm mDrmObj;
    private boolean mDrmProvisioningInProgress;
    private ProvisioningThread mDrmProvisioningThread;
    private byte[] mDrmSessionId;
    private UUID mDrmUUID;
    private EventHandler mEventHandler;
    private Handler mExtSubtitleDataHandler;
    private OnSubtitleDataListener mExtSubtitleDataListener;
    private BitSet mInbandTrackIndices;
    private Vector<Pair<Integer, SubtitleTrack>> mIndexTrackPairs;
    private final OnSubtitleDataListener mIntSubtitleDataListener;
    private int mListenerContext;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private final OnCompletionListener mOnCompletionInternalListener;
    private OnCompletionListener mOnCompletionListener;
    private OnDrmConfigHelper mOnDrmConfigHelper;
    private OnDrmInfoHandlerDelegate mOnDrmInfoHandlerDelegate;
    private OnDrmPreparedHandlerDelegate mOnDrmPreparedHandlerDelegate;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private Handler mOnMediaTimeDiscontinuityHandler;
    private OnMediaTimeDiscontinuityListener mOnMediaTimeDiscontinuityListener;
    private OnPreparedListener mOnPreparedListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnTimedMetaDataAvailableListener mOnTimedMetaDataAvailableListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private Vector<InputStream> mOpenSubtitleSources;
    private AudioDeviceInfo mPreferredDevice;
    private boolean mPrepareDrmInProgress;

    @GuardedBy("mRoutingChangeListeners")
    private ArrayMap<AudioRouting.OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> mRoutingChangeListeners;
    private boolean mScreenOnWhilePlaying;
    private int mSelectedSubtitleTrackIndex;
    private boolean mStayAwake;
    private int mStreamType;
    private SubtitleController mSubtitleController;
    private boolean mSubtitleDataListenerDisabled;
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

    public interface OnDrmConfigHelper {
        void onDrmConfig(MediaPlayer mediaPlayer);
    }

    public interface OnDrmInfoListener {
        void onDrmInfo(MediaPlayer mediaPlayer, DrmInfo drmInfo);
    }

    public interface OnDrmPreparedListener {
        void onDrmPrepared(MediaPlayer mediaPlayer, int i);
    }

    public interface OnErrorListener {
        boolean onError(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnInfoListener {
        boolean onInfo(MediaPlayer mediaPlayer, int i, int i2);
    }

    public interface OnMediaTimeDiscontinuityListener {
        void onMediaTimeDiscontinuity(MediaPlayer mediaPlayer, MediaTimestamp mediaTimestamp);
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

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlaybackRateAudioMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PrepareDrmStatusCode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SeekMode {
    }

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _notifyAt(long j);

    private native void _pause() throws IllegalStateException;

    private native void _prepare() throws IllegalStateException, IOException;

    private native void _prepareDrm(byte[] bArr, byte[] bArr2);

    private native void _release();

    private native void _releaseDrm();

    private native void _reset();

    private final native void _seekTo(long j, int i);

    private native void _setAudioStreamType(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setDataSource(MediaDataSource mediaDataSource) throws IllegalStateException, IllegalArgumentException;

    private native void _setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IllegalStateException, IOException, IllegalArgumentException;

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native void nativeSetDataSource(IBinder iBinder, String str, String[] strArr, String[] strArr2) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException;

    private native int native_applyVolumeShaper(VolumeShaper.Configuration configuration, VolumeShaper.Operation operation);

    private final native void native_enableDeviceCallback(boolean z);

    private final native void native_finalize();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private native PersistableBundle native_getMetrics();

    private final native int native_getRoutedDeviceId();

    private native VolumeShaper.State native_getVolumeShaperState(int i);

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    public static native int native_pullBatteryData(Parcel parcel);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native boolean native_setOutputDevice(int i);

    private final native int native_setRetransmitEndpoint(String str, int i);

    private final native void native_setup(Object obj);

    private native boolean setParameter(int i, Parcel parcel);

    public native void attachAuxEffect(int i);

    public native int getAudioSessionId();

    public native BufferingParams getBufferingParams();

    public native int getCurrentPosition();

    public native int getDuration();

    public native PlaybackParams getPlaybackParams();

    public native SyncParams getSyncParams();

    public native int getVideoHeight();

    public native int getVideoWidth();

    public native boolean isLooping();

    public native boolean isPlaying();

    public native void prepareAsync() throws IllegalStateException;

    public native void setAudioSessionId(int i) throws IllegalStateException, IllegalArgumentException;

    public native void setBufferingParams(BufferingParams bufferingParams);

    public native void setLooping(boolean z);

    public native void setNextMediaPlayer(MediaPlayer mediaPlayer);

    public native void setPlaybackParams(PlaybackParams playbackParams);

    public native void setSyncParams(SyncParams syncParams);

    static {
        System.loadLibrary("media_jni");
        native_init();
        DEBUG = !"user".equals(Build.TYPE);
    }

    public MediaPlayer() {
        super(new AudioAttributes.Builder().build(), 2);
        this.mWakeLock = null;
        this.mStreamType = Integer.MIN_VALUE;
        this.mUsage = -1;
        this.mDrmLock = new Object();
        this.mPreferredDevice = null;
        this.mRoutingChangeListeners = new ArrayMap<>();
        this.mIndexTrackPairs = new Vector<>();
        this.mInbandTrackIndices = new BitSet();
        this.mSelectedSubtitleTrackIndex = -1;
        this.mIntSubtitleDataListener = new OnSubtitleDataListener() {
            @Override
            public void onSubtitleData(MediaPlayer mediaPlayer, SubtitleData subtitleData) {
                int trackIndex = subtitleData.getTrackIndex();
                synchronized (MediaPlayer.this.mIndexTrackPairs) {
                    for (Pair pair : MediaPlayer.this.mIndexTrackPairs) {
                        if (pair.first != 0 && ((Integer) pair.first).intValue() == trackIndex && pair.second != 0) {
                            ((SubtitleTrack) pair.second).onData(subtitleData);
                        }
                    }
                }
            }
        };
        this.mOnCompletionInternalListener = new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                MediaPlayer.this.baseStop();
            }
        };
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
        this.mTimeProvider = new TimeProvider(this);
        this.mOpenSubtitleSources = new Vector<>();
        native_setup(new WeakReference(this));
        baseRegisterPlayer();
    }

    public Parcel newRequest() {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.writeInterfaceToken(IMEDIA_PLAYER);
        return parcelObtain;
    }

    public void invoke(Parcel parcel, Parcel parcel2) {
        int iNative_invoke = native_invoke(parcel, parcel2);
        parcel2.setDataPosition(0);
        if (iNative_invoke != 0) {
            throw new RuntimeException("failure code: " + iNative_invoke);
        }
    }

    public void setDisplay(SurfaceHolder surfaceHolder) {
        Surface surface;
        this.mSurfaceHolder = surfaceHolder;
        if (surfaceHolder != null) {
            surface = surfaceHolder.getSurface();
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

    public void setVideoScalingMode(int i) {
        if (!isVideoScalingModeSupported(i)) {
            throw new IllegalArgumentException("Scaling mode " + i + " is not supported");
        }
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IMEDIA_PLAYER);
            parcelObtain.writeInt(6);
            parcelObtain.writeInt(i);
            invoke(parcelObtain, parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    public static MediaPlayer create(Context context, Uri uri) {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder surfaceHolder) {
        int iNewAudioSessionId = AudioSystem.newAudioSessionId();
        if (iNewAudioSessionId <= 0) {
            iNewAudioSessionId = 0;
        }
        return create(context, uri, surfaceHolder, null, iNewAudioSessionId);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder surfaceHolder, AudioAttributes audioAttributes, int i) {
        if (DEBUG) {
            Log.d(TAG, "MediaPlayer create called");
        }
        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            if (audioAttributes == null) {
                audioAttributes = new AudioAttributes.Builder().build();
            }
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setAudioSessionId(i);
            mediaPlayer.setDataSource(context, uri);
            if (surfaceHolder != null) {
                mediaPlayer.setDisplay(surfaceHolder);
            }
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException e) {
            Log.d(TAG, "create failed:", e);
            return null;
        } catch (IllegalArgumentException e2) {
            Log.d(TAG, "create failed:", e2);
            return null;
        } catch (SecurityException e3) {
            Log.d(TAG, "create failed:", e3);
            return null;
        }
    }

    public static MediaPlayer create(Context context, int i) {
        int iNewAudioSessionId = AudioSystem.newAudioSessionId();
        if (iNewAudioSessionId <= 0) {
            iNewAudioSessionId = 0;
        }
        return create(context, i, null, iNewAudioSessionId);
    }

    public static MediaPlayer create(Context context, int i, AudioAttributes audioAttributes, int i2) {
        try {
            AssetFileDescriptor assetFileDescriptorOpenRawResourceFd = context.getResources().openRawResourceFd(i);
            if (assetFileDescriptorOpenRawResourceFd == null) {
                return null;
            }
            MediaPlayer mediaPlayer = new MediaPlayer();
            if (audioAttributes == null) {
                audioAttributes = new AudioAttributes.Builder().build();
            }
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setAudioSessionId(i2);
            mediaPlayer.setDataSource(assetFileDescriptorOpenRawResourceFd.getFileDescriptor(), assetFileDescriptorOpenRawResourceFd.getStartOffset(), assetFileDescriptorOpenRawResourceFd.getLength());
            assetFileDescriptorOpenRawResourceFd.close();
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException e) {
            Log.d(TAG, "create failed:", e);
            return null;
        } catch (IllegalArgumentException e2) {
            Log.d(TAG, "create failed:", e2);
            return null;
        } catch (SecurityException e3) {
            Log.d(TAG, "create failed:", e3);
            return null;
        }
    }

    public void setDataSource(Context context, Uri uri) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(context, uri, (Map<String, String>) null, (List<HttpCookie>) null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> map, List<HttpCookie> list) throws IOException {
        CookieHandler cookieHandler;
        if (context == null) {
            throw new NullPointerException("context param can not be null.");
        }
        if (uri == null) {
            throw new NullPointerException("uri param can not be null.");
        }
        if (list != null && (cookieHandler = CookieHandler.getDefault()) != null && !(cookieHandler instanceof CookieManager)) {
            throw new IllegalArgumentException("The cookie handler has to be of CookieManager type when cookies are provided.");
        }
        ContentResolver contentResolver = context.getContentResolver();
        String scheme = uri.getScheme();
        String authorityWithoutUserId = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            setDataSource(uri.getPath());
            return;
        }
        if ("content".equals(scheme) && "settings".equals(authorityWithoutUserId)) {
            int defaultType = RingtoneManager.getDefaultType(uri);
            Uri cacheForType = RingtoneManager.getCacheForType(defaultType, context.getUserId());
            Uri actualDefaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, defaultType);
            if (attemptDataSource(contentResolver, cacheForType) || attemptDataSource(contentResolver, actualDefaultRingtoneUri)) {
                return;
            }
            setDataSource(uri.toString(), map, list);
            return;
        }
        if (attemptDataSource(contentResolver, uri)) {
            return;
        }
        setDataSource(uri.toString(), map, list);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> map) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(context, uri, map, (List<HttpCookie>) null);
    }

    private boolean attemptDataSource(ContentResolver contentResolver, Uri uri) {
        try {
            AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            Throwable th = null;
            try {
                setDataSource(assetFileDescriptorOpenAssetFileDescriptor);
                if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                }
                return true;
            } catch (Throwable th2) {
                if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                    if (0 != 0) {
                        try {
                            assetFileDescriptorOpenAssetFileDescriptor.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        assetFileDescriptorOpenAssetFileDescriptor.close();
                    }
                }
                throw th2;
            }
        } catch (IOException | NullPointerException | SecurityException e) {
            Log.w(TAG, "Couldn't open " + uri + ": " + e);
            return false;
        }
    }

    public void setDataSource(String str) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(str, (Map<String, String>) null, (List<HttpCookie>) null);
    }

    public void setDataSource(String str, Map<String, String> map) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        setDataSource(str, map, (List<HttpCookie>) null);
    }

    private void setDataSource(String str, Map<String, String> map, List<HttpCookie> list) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        String[] strArr;
        String[] strArr2 = null;
        if (map != null) {
            strArr2 = new String[map.size()];
            strArr = new String[map.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                strArr2[i] = entry.getKey();
                strArr[i] = entry.getValue();
                i++;
            }
        } else {
            strArr = null;
        }
        setDataSource(str, strArr2, strArr, list);
    }

    private void setDataSource(String str, String[] strArr, String[] strArr2, List<HttpCookie> list) throws IllegalStateException, IOException, SecurityException, IllegalArgumentException {
        Uri uri = Uri.parse(str);
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            str = uri.getPath();
        } else if (scheme != null) {
            nativeSetDataSource(MediaHTTPService.createHttpServiceBinderIfNecessary(str, list), str, strArr, strArr2);
            return;
        }
        File file = new File(str);
        if (file.exists()) {
            FileInputStream fileInputStream = new FileInputStream(file);
            setDataSource(fileInputStream.getFD());
            fileInputStream.close();
            return;
        }
        throw new IOException("setDataSource failed.");
    }

    public void setDataSource(AssetFileDescriptor assetFileDescriptor) throws IllegalStateException, IOException, IllegalArgumentException {
        Preconditions.checkNotNull(assetFileDescriptor);
        if (assetFileDescriptor.getDeclaredLength() < 0) {
            setDataSource(assetFileDescriptor.getFileDescriptor());
        } else {
            setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getDeclaredLength());
        }
    }

    public void setDataSource(FileDescriptor fileDescriptor) throws IllegalStateException, IOException, IllegalArgumentException {
        setDataSource(fileDescriptor, 0L, DataSourceDesc.LONG_MAX);
    }

    public void setDataSource(FileDescriptor fileDescriptor, long j, long j2) throws IllegalStateException, IOException, IllegalArgumentException {
        _setDataSource(fileDescriptor, j, j2);
    }

    public void setDataSource(MediaDataSource mediaDataSource) throws IllegalStateException, IllegalArgumentException {
        _setDataSource(mediaDataSource);
    }

    public void prepare() throws IllegalStateException, IOException {
        _prepare();
        scanInternalSubtitleTracks();
        synchronized (this.mDrmLock) {
            this.mDrmInfoResolved = true;
        }
    }

    public void start() throws IllegalStateException {
        if (DEBUG) {
            Log.d(TAG, "MediaPlayer start called");
        }
        final int startDelayMs = getStartDelayMs();
        if (startDelayMs == 0) {
            startImpl();
        } else {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(startDelayMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MediaPlayer.this.baseSetStartDelayMs(0);
                    try {
                        MediaPlayer.this.startImpl();
                    } catch (IllegalStateException e2) {
                    }
                }
            }.start();
        }
        if (DEBUG) {
            Log.d(TAG, "MediaPlayer start end");
        }
    }

    private void startImpl() {
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
        baseStop();
    }

    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
        basePause();
    }

    @Override
    void playerStart() {
        start();
    }

    @Override
    void playerPause() {
        pause();
    }

    @Override
    void playerStop() {
        stop();
    }

    @Override
    int playerApplyVolumeShaper(VolumeShaper.Configuration configuration, VolumeShaper.Operation operation) {
        return native_applyVolumeShaper(configuration, operation);
    }

    @Override
    VolumeShaper.State playerGetVolumeShaperState(int i) {
        return native_getVolumeShaperState(i);
    }

    @Override
    public VolumeShaper createVolumeShaper(VolumeShaper.Configuration configuration) {
        return new VolumeShaper(configuration, this);
    }

    @Override
    public boolean setPreferredDevice(AudioDeviceInfo audioDeviceInfo) {
        if (audioDeviceInfo != null && !audioDeviceInfo.isSink()) {
            return false;
        }
        boolean zNative_setOutputDevice = native_setOutputDevice(audioDeviceInfo != null ? audioDeviceInfo.getId() : 0);
        if (zNative_setOutputDevice) {
            synchronized (this) {
                this.mPreferredDevice = audioDeviceInfo;
            }
        }
        return zNative_setOutputDevice;
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
        AudioDeviceInfo[] devicesStatic = AudioManager.getDevicesStatic(2);
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

    public void setWakeMode(Context context, int i) {
        boolean z = true;
        if (SystemProperties.getBoolean("audio.offload.ignore_setawake", false)) {
            Log.w(TAG, "IGNORING setWakeMode " + i);
            return;
        }
        if (this.mWakeLock != null) {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            } else {
                z = false;
            }
            this.mWakeLock = null;
        } else {
            z = false;
        }
        this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(i | 536870912, MediaPlayer.class.getName());
        this.mWakeLock.setReferenceCounted(false);
        if (z) {
            this.mWakeLock.acquire();
        }
    }

    public void setScreenOnWhilePlaying(boolean z) {
        if (this.mScreenOnWhilePlaying != z) {
            if (z && this.mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            this.mScreenOnWhilePlaying = z;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean z) {
        if (this.mWakeLock != null) {
            if (z && !this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            } else if (!z && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
        this.mStayAwake = z;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (this.mSurfaceHolder != null) {
            this.mSurfaceHolder.setKeepScreenOn(this.mScreenOnWhilePlaying && this.mStayAwake);
        }
    }

    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    public PlaybackParams easyPlaybackParams(float f, int i) {
        PlaybackParams playbackParams = new PlaybackParams();
        playbackParams.allowDefaults();
        switch (i) {
            case 0:
                playbackParams.setSpeed(f).setPitch(1.0f);
                return playbackParams;
            case 1:
                playbackParams.setSpeed(f).setPitch(1.0f).setAudioFallbackMode(2);
                return playbackParams;
            case 2:
                playbackParams.setSpeed(f).setPitch(f);
                return playbackParams;
            default:
                throw new IllegalArgumentException("Audio playback mode " + i + " is not supported");
        }
    }

    public void seekTo(long j, int i) {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("Illegal seek mode: " + i);
        }
        if (j > 2147483647L) {
            Log.w(TAG, "seekTo offset " + j + " is too large, cap to 2147483647");
            j = 2147483647L;
        } else if (j < -2147483648L) {
            Log.w(TAG, "seekTo offset " + j + " is too small, cap to -2147483648");
            j = -2147483648L;
        }
        _seekTo(j, i);
    }

    public void seekTo(int i) throws IllegalStateException {
        seekTo(i, 0);
    }

    public MediaTimestamp getTimestamp() {
        try {
            return new MediaTimestamp(((long) getCurrentPosition()) * 1000, System.nanoTime(), isPlaying() ? getPlaybackParams().getSpeed() : 0.0f);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public Metadata getMetadata(boolean z, boolean z2) {
        Parcel parcelObtain = Parcel.obtain();
        Metadata metadata = new Metadata();
        if (!native_getMetadata(z, z2, parcelObtain)) {
            parcelObtain.recycle();
            return null;
        }
        if (!metadata.parse(parcelObtain)) {
            parcelObtain.recycle();
            return null;
        }
        return metadata;
    }

    public int setMetadataFilter(Set<Integer> set, Set<Integer> set2) {
        Parcel parcelNewRequest = newRequest();
        int iDataSize = parcelNewRequest.dataSize() + (4 * (set.size() + 1 + 1 + set2.size()));
        if (parcelNewRequest.dataCapacity() < iDataSize) {
            parcelNewRequest.setDataCapacity(iDataSize);
        }
        parcelNewRequest.writeInt(set.size());
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            parcelNewRequest.writeInt(it.next().intValue());
        }
        parcelNewRequest.writeInt(set2.size());
        Iterator<Integer> it2 = set2.iterator();
        while (it2.hasNext()) {
            parcelNewRequest.writeInt(it2.next().intValue());
        }
        return native_setMetadataFilter(parcelNewRequest);
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
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        synchronized (this) {
            this.mSubtitleDataListenerDisabled = false;
            this.mExtSubtitleDataListener = null;
            this.mExtSubtitleDataHandler = null;
            this.mOnMediaTimeDiscontinuityListener = null;
            this.mOnMediaTimeDiscontinuityHandler = null;
        }
        this.mOnDrmConfigHelper = null;
        this.mOnDrmInfoHandlerDelegate = null;
        this.mOnDrmPreparedHandlerDelegate = null;
        resetDrmState();
        if (DEBUG) {
            Log.d(TAG, "_release native called");
        }
        _release();
        if (DEBUG) {
            Log.d(TAG, "_release native finished");
        }
    }

    public void reset() {
        this.mSelectedSubtitleTrackIndex = -1;
        synchronized (this.mOpenSubtitleSources) {
            Iterator<InputStream> it = this.mOpenSubtitleSources.iterator();
            while (it.hasNext()) {
                try {
                    it.next().close();
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
        resetDrmState();
    }

    public void notifyAt(long j) {
        _notifyAt(j);
    }

    public void setAudioStreamType(int i) {
        deprecateStreamTypeForPlayback(i, TAG, "setAudioStreamType()");
        baseUpdateAudioAttributes(new AudioAttributes.Builder().setInternalLegacyStreamType(i).build());
        _setAudioStreamType(i);
        this.mStreamType = i;
    }

    public void setAudioAttributes(AudioAttributes audioAttributes) throws IllegalArgumentException {
        if (audioAttributes == null) {
            throw new IllegalArgumentException("Cannot set AudioAttributes to null");
        }
        baseUpdateAudioAttributes(audioAttributes);
        this.mUsage = audioAttributes.getUsage();
        this.mBypassInterruptionPolicy = (audioAttributes.getAllFlags() & 64) != 0;
        Parcel parcelObtain = Parcel.obtain();
        audioAttributes.writeToParcel(parcelObtain, 1);
        setParameter(1400, parcelObtain);
        parcelObtain.recycle();
    }

    public void setVolume(float f, float f2) {
        baseSetVolume(f, f2);
    }

    @Override
    void playerSetVolume(boolean z, float f, float f2) {
        if (z) {
            f = 0.0f;
        }
        if (z) {
            f2 = 0.0f;
        }
        _setVolume(f, f2);
    }

    public void setVolume(float f) {
        setVolume(f, f);
    }

    public void setAuxEffectSendLevel(float f) {
        baseSetAuxEffectSendLevel(f);
    }

    @Override
    int playerSetAuxEffectSendLevel(boolean z, float f) {
        if (z) {
            f = 0.0f;
        }
        _setAuxEffectSendLevel(f);
        return 0;
    }

    public static class TrackInfo implements Parcelable {
        static final Parcelable.Creator<TrackInfo> CREATOR = new Parcelable.Creator<TrackInfo>() {
            @Override
            public TrackInfo createFromParcel(Parcel parcel) {
                return new TrackInfo(parcel);
            }

            @Override
            public TrackInfo[] newArray(int i) {
                return new TrackInfo[i];
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

        @Retention(RetentionPolicy.SOURCE)
        public @interface TrackType {
        }

        public int getTrackType() {
            return this.mTrackType;
        }

        public String getLanguage() {
            String string = this.mFormat.getString("language");
            return string == null ? "und" : string;
        }

        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfo(Parcel parcel) {
            this.mTrackType = parcel.readInt();
            this.mFormat = MediaFormat.createSubtitleFormat(parcel.readString(), parcel.readString());
            if (this.mTrackType == 4) {
                this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, parcel.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, parcel.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, parcel.readInt());
            }
        }

        TrackInfo(int i, MediaFormat mediaFormat) {
            this.mTrackType = i;
            this.mFormat = mediaFormat;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mTrackType);
            parcel.writeString(this.mFormat.getString(MediaFormat.KEY_MIME));
            parcel.writeString(getLanguage());
            if (this.mTrackType == 4) {
                parcel.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
                parcel.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
                parcel.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(getClass().getName());
            sb.append('{');
            switch (this.mTrackType) {
                case 1:
                    sb.append("VIDEO");
                    break;
                case 2:
                    sb.append("AUDIO");
                    break;
                case 3:
                    sb.append("TIMEDTEXT");
                    break;
                case 4:
                    sb.append("SUBTITLE");
                    break;
                default:
                    sb.append(IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
                    break;
            }
            sb.append(", " + this.mFormat.toString());
            sb.append("}");
            return sb.toString();
        }
    }

    public TrackInfo[] getTrackInfo() throws IllegalStateException {
        TrackInfo[] trackInfoArr;
        TrackInfo[] inbandTrackInfo = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            trackInfoArr = new TrackInfo[this.mIndexTrackPairs.size()];
            for (int i = 0; i < trackInfoArr.length; i++) {
                Pair<Integer, SubtitleTrack> pair = this.mIndexTrackPairs.get(i);
                if (pair.first != null) {
                    trackInfoArr[i] = inbandTrackInfo[pair.first.intValue()];
                } else {
                    SubtitleTrack subtitleTrack = pair.second;
                    trackInfoArr[i] = new TrackInfo(subtitleTrack.getTrackType(), subtitleTrack.getFormat());
                }
            }
        }
        return trackInfoArr;
    }

    private TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IMEDIA_PLAYER);
            parcelObtain.writeInt(1);
            invoke(parcelObtain, parcelObtain2);
            return (TrackInfo[]) parcelObtain2.createTypedArray(TrackInfo.CREATOR);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    private static boolean availableMimeTypeForExternalSource(String str) {
        if ("application/x-subrip".equals(str)) {
            return true;
        }
        return false;
    }

    public void setSubtitleAnchor(SubtitleController subtitleController, SubtitleController.Anchor anchor) {
        this.mSubtitleController = subtitleController;
        this.mSubtitleController.setAnchor(anchor);
    }

    private synchronized void setSubtitleAnchor() {
        if (this.mSubtitleController == null && ActivityThread.currentApplication() != null) {
            final HandlerThread handlerThread = new HandlerThread("SetSubtitleAnchorThread");
            handlerThread.start();
            new Handler(handlerThread.getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Application applicationCurrentApplication = ActivityThread.currentApplication();
                    MediaPlayer.this.mSubtitleController = new SubtitleController(applicationCurrentApplication, MediaPlayer.this.mTimeProvider, MediaPlayer.this);
                    MediaPlayer.this.mSubtitleController.setAnchor(new SubtitleController.Anchor() {
                        @Override
                        public void setSubtitleWidget(SubtitleTrack.RenderingWidget renderingWidget) {
                        }

                        @Override
                        public Looper getSubtitleLooper() {
                            return Looper.getMainLooper();
                        }
                    });
                    handlerThread.getLooper().quitSafely();
                }
            });
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "failed to join SetSubtitleAnchorThread");
            }
        }
    }

    @Override
    public void onSubtitleTrackSelected(SubtitleTrack subtitleTrack) {
        if (this.mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, false);
            } catch (IllegalStateException e) {
            }
            this.mSelectedSubtitleTrackIndex = -1;
        }
        synchronized (this) {
            this.mSubtitleDataListenerDisabled = true;
        }
        if (subtitleTrack == null) {
            return;
        }
        synchronized (this.mIndexTrackPairs) {
            Iterator<Pair<Integer, SubtitleTrack>> it = this.mIndexTrackPairs.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Pair<Integer, SubtitleTrack> next = it.next();
                if (next.first != null && next.second == subtitleTrack) {
                    this.mSelectedSubtitleTrackIndex = next.first.intValue();
                    break;
                }
            }
        }
        if (this.mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(this.mSelectedSubtitleTrackIndex, true);
            } catch (IllegalStateException e2) {
            }
            synchronized (this) {
                this.mSubtitleDataListenerDisabled = false;
            }
        }
    }

    public void addSubtitleSource(final InputStream inputStream, final MediaFormat mediaFormat) throws IllegalStateException {
        if (inputStream != null) {
            synchronized (this.mOpenSubtitleSources) {
                this.mOpenSubtitleSources.add(inputStream);
            }
        } else {
            Log.w(TAG, "addSubtitleSource called with null InputStream");
        }
        getMediaTimeProvider();
        final HandlerThread handlerThread = new HandlerThread("SubtitleReadThread", 9);
        handlerThread.start();
        new Handler(handlerThread.getLooper()).post(new Runnable() {
            private int addTrack() {
                SubtitleTrack subtitleTrackAddTrack;
                if (inputStream == null || MediaPlayer.this.mSubtitleController == null || (subtitleTrackAddTrack = MediaPlayer.this.mSubtitleController.addTrack(mediaFormat)) == null) {
                    return 901;
                }
                Scanner scanner = new Scanner(inputStream, "UTF-8");
                String next = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer.this.mOpenSubtitleSources) {
                    MediaPlayer.this.mOpenSubtitleSources.remove(inputStream);
                }
                scanner.close();
                synchronized (MediaPlayer.this.mIndexTrackPairs) {
                    MediaPlayer.this.mIndexTrackPairs.add(Pair.create(null, subtitleTrackAddTrack));
                }
                Handler handler = MediaPlayer.this.mTimeProvider.mEventHandler;
                handler.sendMessage(handler.obtainMessage(1, 4, 0, Pair.create(subtitleTrackAddTrack, next.getBytes())));
                return 803;
            }

            @Override
            public void run() {
                int iAddTrack = addTrack();
                if (MediaPlayer.this.mEventHandler != null) {
                    MediaPlayer.this.mEventHandler.sendMessage(MediaPlayer.this.mEventHandler.obtainMessage(200, iAddTrack, 0, null));
                }
                handlerThread.getLooper().quitSafely();
            }
        });
    }

    private void scanInternalSubtitleTracks() {
        setSubtitleAnchor();
        populateInbandTracks();
        if (this.mSubtitleController != null) {
            this.mSubtitleController.selectDefaultTrack();
        }
    }

    private void populateInbandTracks() {
        TrackInfo[] inbandTrackInfo = getInbandTrackInfo();
        synchronized (this.mIndexTrackPairs) {
            for (int i = 0; i < inbandTrackInfo.length; i++) {
                if (!this.mInbandTrackIndices.get(i)) {
                    this.mInbandTrackIndices.set(i);
                    if (inbandTrackInfo[i] == null) {
                        Log.w(TAG, "unexpected NULL track at index " + i);
                    }
                    if (inbandTrackInfo[i] != null && inbandTrackInfo[i].getTrackType() == 4) {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), this.mSubtitleController.addTrack(inbandTrackInfo[i].getFormat())));
                    } else {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), null));
                    }
                }
            }
        }
    }

    public void addTimedTextSource(String str, String str2) throws IllegalStateException, IOException, IllegalArgumentException {
        if (!availableMimeTypeForExternalSource(str2)) {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + str2);
        }
        File file = new File(str);
        if (file.exists()) {
            FileInputStream fileInputStream = new FileInputStream(file);
            addTimedTextSource(fileInputStream.getFD(), str2);
            fileInputStream.close();
            return;
        }
        throw new IOException(str);
    }

    public void addTimedTextSource(Context context, Uri uri, String str) throws Throwable {
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(ContentResolver.SCHEME_FILE)) {
            addTimedTextSource(uri.getPath(), str);
            return;
        }
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            if (assetFileDescriptorOpenAssetFileDescriptor == null) {
                if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                    return;
                }
                return;
            }
            try {
                addTimedTextSource(assetFileDescriptorOpenAssetFileDescriptor.getFileDescriptor(), str);
                if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                }
            } catch (IOException e) {
                assetFileDescriptor = assetFileDescriptorOpenAssetFileDescriptor;
                if (assetFileDescriptor == null) {
                    return;
                }
                assetFileDescriptor.close();
            } catch (SecurityException e2) {
                assetFileDescriptor = assetFileDescriptorOpenAssetFileDescriptor;
                if (assetFileDescriptor == null) {
                    return;
                }
                assetFileDescriptor.close();
            } catch (Throwable th) {
                th = th;
                assetFileDescriptor = assetFileDescriptorOpenAssetFileDescriptor;
                if (assetFileDescriptor != null) {
                    assetFileDescriptor.close();
                }
                throw th;
            }
        } catch (IOException e3) {
        } catch (SecurityException e4) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public void addTimedTextSource(FileDescriptor fileDescriptor, String str) throws IllegalStateException, IllegalArgumentException {
        addTimedTextSource(fileDescriptor, 0L, DataSourceDesc.LONG_MAX, str);
    }

    public void addTimedTextSource(FileDescriptor fileDescriptor, final long j, final long j2, String str) throws IllegalStateException, IllegalArgumentException {
        if (!availableMimeTypeForExternalSource(str)) {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + str);
        }
        try {
            final FileDescriptor fileDescriptorDup = Os.dup(fileDescriptor);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, str);
            mediaFormat.setInteger(MediaFormat.KEY_IS_TIMED_TEXT, 1);
            if (this.mSubtitleController == null) {
                setSubtitleAnchor();
            }
            if (!this.mSubtitleController.hasRendererFor(mediaFormat)) {
                this.mSubtitleController.registerRenderer(new SRTRenderer(ActivityThread.currentApplication(), this.mEventHandler));
            }
            final SubtitleTrack subtitleTrackAddTrack = this.mSubtitleController.addTrack(mediaFormat);
            synchronized (this.mIndexTrackPairs) {
                this.mIndexTrackPairs.add(Pair.create(null, subtitleTrackAddTrack));
            }
            getMediaTimeProvider();
            final HandlerThread handlerThread = new HandlerThread("TimedTextReadThread", 9);
            handlerThread.start();
            new Handler(handlerThread.getLooper()).post(new Runnable() {
                private int addTrack() {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        try {
                            Os.lseek(fileDescriptorDup, j, OsConstants.SEEK_SET);
                            byte[] bArr = new byte[4096];
                            long j3 = 0;
                            while (j3 < j2) {
                                int i = IoBridge.read(fileDescriptorDup, bArr, 0, (int) Math.min(bArr.length, j2 - j3));
                                if (i < 0) {
                                    break;
                                }
                                byteArrayOutputStream.write(bArr, 0, i);
                                j3 += (long) i;
                            }
                            Handler handler = MediaPlayer.this.mTimeProvider.mEventHandler;
                            handler.sendMessage(handler.obtainMessage(1, 4, 0, Pair.create(subtitleTrackAddTrack, byteArrayOutputStream.toByteArray())));
                            try {
                                Os.close(fileDescriptorDup);
                            } catch (ErrnoException e) {
                                Log.e(MediaPlayer.TAG, e.getMessage(), e);
                            }
                            return 803;
                        } catch (Throwable th) {
                            try {
                                Os.close(fileDescriptorDup);
                            } catch (ErrnoException e2) {
                                Log.e(MediaPlayer.TAG, e2.getMessage(), e2);
                            }
                            throw th;
                        }
                    } catch (Exception e3) {
                        Log.e(MediaPlayer.TAG, e3.getMessage(), e3);
                        try {
                            Os.close(fileDescriptorDup);
                        } catch (ErrnoException e4) {
                            Log.e(MediaPlayer.TAG, e4.getMessage(), e4);
                        }
                        return 900;
                    }
                }

                @Override
                public void run() {
                    int iAddTrack = addTrack();
                    if (MediaPlayer.this.mEventHandler != null) {
                        MediaPlayer.this.mEventHandler.sendMessage(MediaPlayer.this.mEventHandler.obtainMessage(200, iAddTrack, 0, null));
                    }
                    handlerThread.getLooper().quitSafely();
                }
            });
        } catch (ErrnoException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public int getSelectedTrack(int i) throws IllegalStateException {
        SubtitleTrack selectedTrack;
        if (this.mSubtitleController != null && ((i == 4 || i == 3) && (selectedTrack = this.mSubtitleController.getSelectedTrack()) != null)) {
            synchronized (this.mIndexTrackPairs) {
                for (int i2 = 0; i2 < this.mIndexTrackPairs.size(); i2++) {
                    if (this.mIndexTrackPairs.get(i2).second == selectedTrack && selectedTrack.getTrackType() == i) {
                        return i2;
                    }
                }
            }
        }
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IMEDIA_PLAYER);
            parcelObtain.writeInt(7);
            parcelObtain.writeInt(i);
            invoke(parcelObtain, parcelObtain2);
            int i3 = parcelObtain2.readInt();
            synchronized (this.mIndexTrackPairs) {
                for (int i4 = 0; i4 < this.mIndexTrackPairs.size(); i4++) {
                    Pair<Integer, SubtitleTrack> pair = this.mIndexTrackPairs.get(i4);
                    if (pair.first != null && pair.first.intValue() == i3) {
                        return i4;
                    }
                }
                return -1;
            }
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    public void selectTrack(int i) throws IllegalStateException {
        selectOrDeselectTrack(i, true);
    }

    public void deselectTrack(int i) throws IllegalStateException {
        selectOrDeselectTrack(i, false);
    }

    private void selectOrDeselectTrack(int i, boolean z) throws IllegalStateException {
        populateInbandTracks();
        try {
            Pair<Integer, SubtitleTrack> pair = this.mIndexTrackPairs.get(i);
            SubtitleTrack subtitleTrack = pair.second;
            if (subtitleTrack == null) {
                selectOrDeselectInbandTrack(pair.first.intValue(), z);
                return;
            }
            if (this.mSubtitleController == null) {
                return;
            }
            if (!z) {
                if (this.mSubtitleController.getSelectedTrack() == subtitleTrack) {
                    this.mSubtitleController.selectTrack(null);
                    return;
                } else {
                    Log.w(TAG, "trying to deselect track that was not selected");
                    return;
                }
            }
            if (subtitleTrack.getTrackType() == 3) {
                int selectedTrack = getSelectedTrack(3);
                synchronized (this.mIndexTrackPairs) {
                    if (selectedTrack >= 0) {
                        try {
                            if (selectedTrack < this.mIndexTrackPairs.size()) {
                                Pair<Integer, SubtitleTrack> pair2 = this.mIndexTrackPairs.get(selectedTrack);
                                if (pair2.first != null && pair2.second == null) {
                                    selectOrDeselectInbandTrack(pair2.first.intValue(), false);
                                }
                            }
                        } finally {
                        }
                    }
                }
            }
            this.mSubtitleController.selectTrack(subtitleTrack);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private void selectOrDeselectInbandTrack(int i, boolean z) throws IllegalStateException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInterfaceToken(IMEDIA_PLAYER);
            parcelObtain.writeInt(z ? 4 : 5);
            parcelObtain.writeInt(i);
            invoke(parcelObtain, parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    public void setRetransmitEndpoint(InetSocketAddress inetSocketAddress) throws IllegalStateException, IllegalArgumentException {
        String hostAddress;
        int port;
        if (inetSocketAddress != null) {
            hostAddress = inetSocketAddress.getAddress().getHostAddress();
            port = inetSocketAddress.getPort();
        } else {
            hostAddress = null;
            port = 0;
        }
        int iNative_setRetransmitEndpoint = native_setRetransmitEndpoint(hostAddress, port);
        if (iNative_setRetransmitEndpoint != 0) {
            throw new IllegalArgumentException("Illegal re-transmit endpoint; native ret " + iNative_setRetransmitEndpoint);
        }
    }

    protected void finalize() {
        baseRelease();
        if (DEBUG) {
            Log.d(TAG, "finalize() native_finalize called");
        }
        native_finalize();
        if (DEBUG) {
            Log.d(TAG, "finalize() native_finalize finished");
        }
    }

    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    private class EventHandler extends Handler {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mediaPlayer, Looper looper) {
            super(looper);
            this.mMediaPlayer = mediaPlayer;
        }

        @Override
        public void handleMessage(Message message) {
            boolean zOnError;
            OnDrmInfoHandlerDelegate onDrmInfoHandlerDelegate;
            final OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener;
            Handler handler;
            final MediaTimestamp mediaTimestamp;
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer.TAG, "mediaplayer went away with unhandled events");
                return;
            }
            int i = message.what;
            if (i == 10000) {
                AudioManager.resetAudioPortGeneration();
                synchronized (MediaPlayer.this.mRoutingChangeListeners) {
                    Iterator it = MediaPlayer.this.mRoutingChangeListeners.values().iterator();
                    while (it.hasNext()) {
                        ((NativeRoutingEventHandlerDelegate) it.next()).notifyClient();
                    }
                }
                return;
            }
            DrmInfo drmInfoMakeCopy = null;
            switch (i) {
                case 0:
                    return;
                case 1:
                    try {
                        MediaPlayer.this.scanInternalSubtitleTracks();
                        break;
                    } catch (RuntimeException e) {
                        sendMessage(obtainMessage(100, 1, -1010, null));
                    }
                    OnPreparedListener onPreparedListener = MediaPlayer.this.mOnPreparedListener;
                    if (onPreparedListener != null) {
                        onPreparedListener.onPrepared(this.mMediaPlayer);
                        return;
                    }
                    return;
                case 2:
                    MediaPlayer.this.mOnCompletionInternalListener.onCompletion(this.mMediaPlayer);
                    OnCompletionListener onCompletionListener = MediaPlayer.this.mOnCompletionListener;
                    if (onCompletionListener != null) {
                        onCompletionListener.onCompletion(this.mMediaPlayer);
                    }
                    MediaPlayer.this.stayAwake(false);
                    return;
                case 3:
                    OnBufferingUpdateListener onBufferingUpdateListener = MediaPlayer.this.mOnBufferingUpdateListener;
                    if (onBufferingUpdateListener != null) {
                        onBufferingUpdateListener.onBufferingUpdate(this.mMediaPlayer, message.arg1);
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
                        onVideoSizeChangedListener.onVideoSizeChanged(this.mMediaPlayer, message.arg1, message.arg2);
                        return;
                    }
                    return;
                case 6:
                case 7:
                    TimeProvider timeProvider = MediaPlayer.this.mTimeProvider;
                    if (timeProvider != null) {
                        timeProvider.onPaused(message.what == 7);
                        return;
                    }
                    return;
                case 8:
                    TimeProvider timeProvider2 = MediaPlayer.this.mTimeProvider;
                    if (timeProvider2 != null) {
                        timeProvider2.onStopped();
                        return;
                    }
                    return;
                case 9:
                    break;
                default:
                    switch (i) {
                        case 98:
                            TimeProvider timeProvider3 = MediaPlayer.this.mTimeProvider;
                            if (timeProvider3 != null) {
                                timeProvider3.onNotifyTime();
                                return;
                            }
                            return;
                        case 99:
                            OnTimedTextListener onTimedTextListener = MediaPlayer.this.mOnTimedTextListener;
                            if (onTimedTextListener == null) {
                                return;
                            }
                            if (message.obj == null) {
                                onTimedTextListener.onTimedText(this.mMediaPlayer, null);
                                return;
                            } else {
                                if (message.obj instanceof Parcel) {
                                    Parcel parcel = (Parcel) message.obj;
                                    TimedText timedText = new TimedText(parcel);
                                    parcel.recycle();
                                    onTimedTextListener.onTimedText(this.mMediaPlayer, timedText);
                                    return;
                                }
                                return;
                            }
                        case 100:
                            Log.e(MediaPlayer.TAG, "Error (" + message.arg1 + "," + message.arg2 + ")");
                            OnErrorListener onErrorListener = MediaPlayer.this.mOnErrorListener;
                            if (onErrorListener != null) {
                                zOnError = onErrorListener.onError(this.mMediaPlayer, message.arg1, message.arg2);
                            } else {
                                zOnError = false;
                            }
                            MediaPlayer.this.mOnCompletionInternalListener.onCompletion(this.mMediaPlayer);
                            OnCompletionListener onCompletionListener2 = MediaPlayer.this.mOnCompletionListener;
                            if (onCompletionListener2 != null && !zOnError) {
                                onCompletionListener2.onCompletion(this.mMediaPlayer);
                            }
                            MediaPlayer.this.stayAwake(false);
                            return;
                        default:
                            switch (i) {
                                case 200:
                                    int i2 = message.arg1;
                                    switch (i2) {
                                        case 700:
                                            Log.i(MediaPlayer.TAG, "Info (" + message.arg1 + "," + message.arg2 + ")");
                                            break;
                                        case 701:
                                        case 702:
                                            TimeProvider timeProvider4 = MediaPlayer.this.mTimeProvider;
                                            if (timeProvider4 != null) {
                                                timeProvider4.onBuffering(message.arg1 == 701);
                                            }
                                            break;
                                        default:
                                            switch (i2) {
                                                case 802:
                                                    try {
                                                        MediaPlayer.this.scanInternalSubtitleTracks();
                                                        break;
                                                    } catch (RuntimeException e2) {
                                                        sendMessage(obtainMessage(100, 1, -1010, null));
                                                        break;
                                                    }
                                                case 803:
                                                    message.arg1 = 802;
                                                    if (MediaPlayer.this.mSubtitleController != null) {
                                                        MediaPlayer.this.mSubtitleController.selectDefaultTrack();
                                                    }
                                                    break;
                                            }
                                            break;
                                    }
                                    OnInfoListener onInfoListener = MediaPlayer.this.mOnInfoListener;
                                    if (onInfoListener != null) {
                                        onInfoListener.onInfo(this.mMediaPlayer, message.arg1, message.arg2);
                                        return;
                                    }
                                    return;
                                case 201:
                                    synchronized (this) {
                                        if (MediaPlayer.this.mSubtitleDataListenerDisabled) {
                                            return;
                                        }
                                        final OnSubtitleDataListener onSubtitleDataListener = MediaPlayer.this.mExtSubtitleDataListener;
                                        Handler handler2 = MediaPlayer.this.mExtSubtitleDataHandler;
                                        if (message.obj instanceof Parcel) {
                                            Parcel parcel2 = (Parcel) message.obj;
                                            final SubtitleData subtitleData = new SubtitleData(parcel2);
                                            parcel2.recycle();
                                            MediaPlayer.this.mIntSubtitleDataListener.onSubtitleData(this.mMediaPlayer, subtitleData);
                                            if (onSubtitleDataListener != null) {
                                                if (handler2 == null) {
                                                    onSubtitleDataListener.onSubtitleData(this.mMediaPlayer, subtitleData);
                                                    return;
                                                } else {
                                                    handler2.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            onSubtitleDataListener.onSubtitleData(EventHandler.this.mMediaPlayer, subtitleData);
                                                        }
                                                    });
                                                    return;
                                                }
                                            }
                                            return;
                                        }
                                        return;
                                    }
                                case 202:
                                    OnTimedMetaDataAvailableListener onTimedMetaDataAvailableListener = MediaPlayer.this.mOnTimedMetaDataAvailableListener;
                                    if (onTimedMetaDataAvailableListener != null && (message.obj instanceof Parcel)) {
                                        Parcel parcel3 = (Parcel) message.obj;
                                        TimedMetaData timedMetaDataCreateTimedMetaDataFromParcel = TimedMetaData.createTimedMetaDataFromParcel(parcel3);
                                        parcel3.recycle();
                                        onTimedMetaDataAvailableListener.onTimedMetaDataAvailable(this.mMediaPlayer, timedMetaDataCreateTimedMetaDataFromParcel);
                                        return;
                                    }
                                    return;
                                default:
                                    switch (i) {
                                        case 210:
                                            Log.v(MediaPlayer.TAG, "MEDIA_DRM_INFO " + MediaPlayer.this.mOnDrmInfoHandlerDelegate);
                                            if (message.obj == null) {
                                                Log.w(MediaPlayer.TAG, "MEDIA_DRM_INFO msg.obj=NULL");
                                                return;
                                            }
                                            if (message.obj instanceof Parcel) {
                                                synchronized (MediaPlayer.this.mDrmLock) {
                                                    if (MediaPlayer.this.mOnDrmInfoHandlerDelegate != null && MediaPlayer.this.mDrmInfo != null) {
                                                        drmInfoMakeCopy = MediaPlayer.this.mDrmInfo.makeCopy();
                                                    }
                                                    onDrmInfoHandlerDelegate = MediaPlayer.this.mOnDrmInfoHandlerDelegate;
                                                    break;
                                                }
                                                if (onDrmInfoHandlerDelegate != null) {
                                                    onDrmInfoHandlerDelegate.notifyClient(drmInfoMakeCopy);
                                                    return;
                                                }
                                                return;
                                            }
                                            Log.w(MediaPlayer.TAG, "MEDIA_DRM_INFO msg.obj of unexpected type " + message.obj);
                                            return;
                                        case 211:
                                            synchronized (this) {
                                                onMediaTimeDiscontinuityListener = MediaPlayer.this.mOnMediaTimeDiscontinuityListener;
                                                handler = MediaPlayer.this.mOnMediaTimeDiscontinuityHandler;
                                                break;
                                            }
                                            if (onMediaTimeDiscontinuityListener != null && (message.obj instanceof Parcel)) {
                                                Parcel parcel4 = (Parcel) message.obj;
                                                parcel4.setDataPosition(0);
                                                long j = parcel4.readLong();
                                                long j2 = parcel4.readLong();
                                                float f = parcel4.readFloat();
                                                parcel4.recycle();
                                                if (j != -1 && j2 != -1) {
                                                    mediaTimestamp = new MediaTimestamp(j, j2 * 1000, f);
                                                } else {
                                                    mediaTimestamp = MediaTimestamp.TIMESTAMP_UNKNOWN;
                                                }
                                                if (handler == null) {
                                                    onMediaTimeDiscontinuityListener.onMediaTimeDiscontinuity(this.mMediaPlayer, mediaTimestamp);
                                                    return;
                                                } else {
                                                    handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            onMediaTimeDiscontinuityListener.onMediaTimeDiscontinuity(EventHandler.this.mMediaPlayer, mediaTimestamp);
                                                        }
                                                    });
                                                    return;
                                                }
                                            }
                                            return;
                                        default:
                                            Log.e(MediaPlayer.TAG, "Unknown message type " + message.what);
                                            return;
                                    }
                            }
                    }
            }
            TimeProvider timeProvider5 = MediaPlayer.this.mTimeProvider;
            if (timeProvider5 != null) {
                timeProvider5.onSeekComplete(this.mMediaPlayer);
            }
        }
    }

    private static void postEventFromNative(Object obj, int i, int i2, int i3, Object obj2) {
        MediaPlayer mediaPlayer = (MediaPlayer) ((WeakReference) obj).get();
        if (mediaPlayer == null) {
            return;
        }
        if (i == 1) {
            synchronized (mediaPlayer.mDrmLock) {
                mediaPlayer.mDrmInfoResolved = true;
            }
        } else if (i != 200) {
            if (i == 210) {
                Log.v(TAG, "postEventFromNative MEDIA_DRM_INFO");
                if (obj2 instanceof Parcel) {
                    DrmInfo drmInfo = new DrmInfo((Parcel) obj2);
                    synchronized (mediaPlayer.mDrmLock) {
                        mediaPlayer.mDrmInfo = drmInfo;
                    }
                } else {
                    Log.w(TAG, "MEDIA_DRM_INFO msg.obj of unexpected type " + obj2);
                }
            }
        } else if (i2 == 2) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer.this.start();
                }
            }).start();
            Thread.yield();
        }
        if (mediaPlayer.mEventHandler != null) {
            mediaPlayer.mEventHandler.sendMessage(mediaPlayer.mEventHandler.obtainMessage(i, i2, i3, obj2));
        }
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = onPreparedListener;
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.mOnCompletionListener = onCompletionListener;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener onBufferingUpdateListener) {
        this.mOnBufferingUpdateListener = onBufferingUpdateListener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener onSeekCompleteListener) {
        this.mOnSeekCompleteListener = onSeekCompleteListener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener onVideoSizeChangedListener) {
        this.mOnVideoSizeChangedListener = onVideoSizeChangedListener;
    }

    public void setOnTimedTextListener(OnTimedTextListener onTimedTextListener) {
        this.mOnTimedTextListener = onTimedTextListener;
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener onSubtitleDataListener, Handler handler) {
        if (onSubtitleDataListener == null) {
            throw new IllegalArgumentException("Illegal null listener");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Illegal null handler");
        }
        setOnSubtitleDataListenerInt(onSubtitleDataListener, handler);
    }

    public void setOnSubtitleDataListener(OnSubtitleDataListener onSubtitleDataListener) {
        if (onSubtitleDataListener == null) {
            throw new IllegalArgumentException("Illegal null listener");
        }
        setOnSubtitleDataListenerInt(onSubtitleDataListener, null);
    }

    public void clearOnSubtitleDataListener() {
        setOnSubtitleDataListenerInt(null, null);
    }

    private void setOnSubtitleDataListenerInt(OnSubtitleDataListener onSubtitleDataListener, Handler handler) {
        synchronized (this) {
            this.mExtSubtitleDataListener = onSubtitleDataListener;
            this.mExtSubtitleDataHandler = handler;
        }
    }

    public void setOnMediaTimeDiscontinuityListener(OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener, Handler handler) {
        if (onMediaTimeDiscontinuityListener == null) {
            throw new IllegalArgumentException("Illegal null listener");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Illegal null handler");
        }
        setOnMediaTimeDiscontinuityListenerInt(onMediaTimeDiscontinuityListener, handler);
    }

    public void setOnMediaTimeDiscontinuityListener(OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener) {
        if (onMediaTimeDiscontinuityListener == null) {
            throw new IllegalArgumentException("Illegal null listener");
        }
        setOnMediaTimeDiscontinuityListenerInt(onMediaTimeDiscontinuityListener, null);
    }

    public void clearOnMediaTimeDiscontinuityListener() {
        setOnMediaTimeDiscontinuityListenerInt(null, null);
    }

    private void setOnMediaTimeDiscontinuityListenerInt(OnMediaTimeDiscontinuityListener onMediaTimeDiscontinuityListener, Handler handler) {
        synchronized (this) {
            this.mOnMediaTimeDiscontinuityListener = onMediaTimeDiscontinuityListener;
            this.mOnMediaTimeDiscontinuityHandler = handler;
        }
    }

    public void setOnTimedMetaDataAvailableListener(OnTimedMetaDataAvailableListener onTimedMetaDataAvailableListener) {
        this.mOnTimedMetaDataAvailableListener = onTimedMetaDataAvailableListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setOnInfoListener(OnInfoListener onInfoListener) {
        this.mOnInfoListener = onInfoListener;
    }

    public void setOnDrmConfigHelper(OnDrmConfigHelper onDrmConfigHelper) {
        synchronized (this.mDrmLock) {
            this.mOnDrmConfigHelper = onDrmConfigHelper;
        }
    }

    public void setOnDrmInfoListener(OnDrmInfoListener onDrmInfoListener) {
        setOnDrmInfoListener(onDrmInfoListener, null);
    }

    public void setOnDrmInfoListener(OnDrmInfoListener onDrmInfoListener, Handler handler) {
        synchronized (this.mDrmLock) {
            try {
                if (onDrmInfoListener != null) {
                    this.mOnDrmInfoHandlerDelegate = new OnDrmInfoHandlerDelegate(this, onDrmInfoListener, handler);
                } else {
                    this.mOnDrmInfoHandlerDelegate = null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void setOnDrmPreparedListener(OnDrmPreparedListener onDrmPreparedListener) {
        setOnDrmPreparedListener(onDrmPreparedListener, null);
    }

    public void setOnDrmPreparedListener(OnDrmPreparedListener onDrmPreparedListener, Handler handler) {
        synchronized (this.mDrmLock) {
            try {
                if (onDrmPreparedListener != null) {
                    this.mOnDrmPreparedHandlerDelegate = new OnDrmPreparedHandlerDelegate(this, onDrmPreparedListener, handler);
                } else {
                    this.mOnDrmPreparedHandlerDelegate = null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private class OnDrmInfoHandlerDelegate {
        private Handler mHandler;
        private MediaPlayer mMediaPlayer;
        private OnDrmInfoListener mOnDrmInfoListener;

        OnDrmInfoHandlerDelegate(MediaPlayer mediaPlayer, OnDrmInfoListener onDrmInfoListener, Handler handler) {
            this.mMediaPlayer = mediaPlayer;
            this.mOnDrmInfoListener = onDrmInfoListener;
            if (handler != null) {
                this.mHandler = handler;
            }
        }

        void notifyClient(final DrmInfo drmInfo) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        OnDrmInfoHandlerDelegate.this.mOnDrmInfoListener.onDrmInfo(OnDrmInfoHandlerDelegate.this.mMediaPlayer, drmInfo);
                    }
                });
            } else {
                this.mOnDrmInfoListener.onDrmInfo(this.mMediaPlayer, drmInfo);
            }
        }
    }

    private class OnDrmPreparedHandlerDelegate {
        private Handler mHandler;
        private MediaPlayer mMediaPlayer;
        private OnDrmPreparedListener mOnDrmPreparedListener;

        OnDrmPreparedHandlerDelegate(MediaPlayer mediaPlayer, OnDrmPreparedListener onDrmPreparedListener, Handler handler) {
            this.mMediaPlayer = mediaPlayer;
            this.mOnDrmPreparedListener = onDrmPreparedListener;
            if (handler == null) {
                if (MediaPlayer.this.mEventHandler != null) {
                    this.mHandler = MediaPlayer.this.mEventHandler;
                    return;
                } else {
                    Log.e(MediaPlayer.TAG, "OnDrmPreparedHandlerDelegate: Unexpected null mEventHandler");
                    return;
                }
            }
            this.mHandler = handler;
        }

        void notifyClient(final int i) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        OnDrmPreparedHandlerDelegate.this.mOnDrmPreparedListener.onDrmPrepared(OnDrmPreparedHandlerDelegate.this.mMediaPlayer, i);
                    }
                });
            } else {
                Log.e(MediaPlayer.TAG, "OnDrmPreparedHandlerDelegate:notifyClient: Unexpected null mHandler");
            }
        }
    }

    public DrmInfo getDrmInfo() {
        DrmInfo drmInfoMakeCopy;
        synchronized (this.mDrmLock) {
            if (!this.mDrmInfoResolved && this.mDrmInfo == null) {
                Log.v(TAG, "The Player has not been prepared yet");
                throw new IllegalStateException("The Player has not been prepared yet");
            }
            if (this.mDrmInfo == null) {
                drmInfoMakeCopy = null;
            } else {
                drmInfoMakeCopy = this.mDrmInfo.makeCopy();
            }
        }
        return drmInfoMakeCopy;
    }

    public void prepareDrm(UUID uuid) throws UnsupportedSchemeException, ProvisioningNetworkErrorException, ResourceBusyException, ProvisioningServerErrorException {
        boolean z;
        OnDrmPreparedHandlerDelegate onDrmPreparedHandlerDelegate;
        Log.v(TAG, "prepareDrm: uuid: " + uuid + " mOnDrmConfigHelper: " + this.mOnDrmConfigHelper);
        synchronized (this.mDrmLock) {
            if (this.mDrmInfo == null) {
                Log.e(TAG, "prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.");
                throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved before this call.");
            }
            if (this.mActiveDrmScheme) {
                String str = "prepareDrm(): Wrong usage: There is already an active DRM scheme with " + this.mDrmUUID;
                Log.e(TAG, str);
                throw new IllegalStateException(str);
            }
            if (this.mPrepareDrmInProgress) {
                Log.e(TAG, "prepareDrm(): Wrong usage: There is already a pending prepareDrm call.");
                throw new IllegalStateException("prepareDrm(): Wrong usage: There is already a pending prepareDrm call.");
            }
            if (this.mDrmProvisioningInProgress) {
                Log.e(TAG, "prepareDrm(): Unexpectd: Provisioning is already in progress.");
                throw new IllegalStateException("prepareDrm(): Unexpectd: Provisioning is already in progress.");
            }
            cleanDrmObj();
            z = true;
            this.mPrepareDrmInProgress = true;
            onDrmPreparedHandlerDelegate = this.mOnDrmPreparedHandlerDelegate;
            try {
                prepareDrm_createDrmStep(uuid);
                this.mDrmConfigAllowed = true;
            } catch (Exception e) {
                Log.w(TAG, "prepareDrm(): Exception ", e);
                this.mPrepareDrmInProgress = false;
                throw e;
            }
        }
        if (this.mOnDrmConfigHelper != null) {
            this.mOnDrmConfigHelper.onDrmConfig(this);
        }
        synchronized (this.mDrmLock) {
            try {
                this.mDrmConfigAllowed = false;
                try {
                    try {
                        try {
                            prepareDrm_openSessionStep(uuid);
                            this.mDrmUUID = uuid;
                            this.mActiveDrmScheme = true;
                            if (!this.mDrmProvisioningInProgress) {
                                this.mPrepareDrmInProgress = false;
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (!this.mDrmProvisioningInProgress) {
                                this.mPrepareDrmInProgress = false;
                            }
                            if (z) {
                                cleanDrmObj();
                            }
                            throw th;
                        }
                    } catch (NotProvisionedException e2) {
                        Log.w(TAG, "prepareDrm: NotProvisionedException");
                        int iHandleProvisioninig = HandleProvisioninig(uuid);
                        if (iHandleProvisioninig != 0) {
                            switch (iHandleProvisioninig) {
                                case 1:
                                    Log.e(TAG, "prepareDrm: Provisioning was required but failed due to a network error.");
                                    throw new ProvisioningNetworkErrorException("prepareDrm: Provisioning was required but failed due to a network error.");
                                case 2:
                                    Log.e(TAG, "prepareDrm: Provisioning was required but the request was denied by the server.");
                                    throw new ProvisioningServerErrorException("prepareDrm: Provisioning was required but the request was denied by the server.");
                                default:
                                    Log.e(TAG, "prepareDrm: Post-provisioning preparation failed.");
                                    throw new IllegalStateException("prepareDrm: Post-provisioning preparation failed.");
                            }
                        }
                        if (!this.mDrmProvisioningInProgress) {
                            this.mPrepareDrmInProgress = false;
                        }
                        z = false;
                    }
                } catch (IllegalStateException e3) {
                    Log.e(TAG, "prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
                    throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
                } catch (Exception e4) {
                    Log.e(TAG, "prepareDrm: Exception " + e4);
                    throw e4;
                }
            } catch (Throwable th2) {
                th = th2;
                z = false;
            }
        }
        if (!z || onDrmPreparedHandlerDelegate == null) {
            return;
        }
        onDrmPreparedHandlerDelegate.notifyClient(0);
    }

    public void releaseDrm() throws NoDrmSchemeException {
        Log.v(TAG, "releaseDrm:");
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                Log.e(TAG, "releaseDrm(): No active DRM scheme to release.");
                throw new NoDrmSchemeException("releaseDrm: No active DRM scheme to release.");
            }
            try {
                _releaseDrm();
                cleanDrmObj();
                this.mActiveDrmScheme = false;
            } catch (IllegalStateException e) {
                Log.w(TAG, "releaseDrm: Exception ", e);
                throw new IllegalStateException("releaseDrm: The player is not in a valid state.");
            } catch (Exception e2) {
                Log.e(TAG, "releaseDrm: Exception ", e2);
            }
        }
    }

    public MediaDrm.KeyRequest getKeyRequest(byte[] bArr, byte[] bArr2, String str, int i, Map<String, String> map) throws NoDrmSchemeException {
        HashMap<String, String> map2;
        MediaDrm.KeyRequest keyRequest;
        Log.v(TAG, "getKeyRequest:  keySetId: " + bArr + " initData:" + bArr2 + " mimeType: " + str + " keyType: " + i + " optionalParameters: " + map);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                Log.e(TAG, "getKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeException("getKeyRequest: Has to set a DRM scheme first.");
            }
            if (i != 3) {
                try {
                    bArr = this.mDrmSessionId;
                    byte[] bArr3 = bArr;
                    if (map == null) {
                        map2 = new HashMap<>(map);
                    } else {
                        map2 = null;
                    }
                    keyRequest = this.mDrmObj.getKeyRequest(bArr3, bArr2, str, i, map2);
                    Log.v(TAG, "getKeyRequest:   --> request: " + keyRequest);
                } catch (NotProvisionedException e) {
                    Log.w(TAG, "getKeyRequest NotProvisionedException: Unexpected. Shouldn't have reached here.");
                    throw new IllegalStateException("getKeyRequest: Unexpected provisioning error.");
                } catch (Exception e2) {
                    Log.w(TAG, "getKeyRequest Exception " + e2);
                    throw e2;
                }
            } else {
                byte[] bArr32 = bArr;
                if (map == null) {
                }
                keyRequest = this.mDrmObj.getKeyRequest(bArr32, bArr2, str, i, map2);
                Log.v(TAG, "getKeyRequest:   --> request: " + keyRequest);
            }
        }
        return keyRequest;
    }

    public byte[] provideKeyResponse(byte[] bArr, byte[] bArr2) throws DeniedByServerException, NoDrmSchemeException {
        byte[] bArr3;
        byte[] bArrProvideKeyResponse;
        Log.v(TAG, "provideKeyResponse: keySetId: " + bArr + " response: " + bArr2);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                Log.e(TAG, "getKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeException("getKeyRequest: Has to set a DRM scheme first.");
            }
            if (bArr == null) {
                try {
                    bArr3 = this.mDrmSessionId;
                } catch (NotProvisionedException e) {
                    Log.w(TAG, "provideKeyResponse NotProvisionedException: Unexpected. Shouldn't have reached here.");
                    throw new IllegalStateException("provideKeyResponse: Unexpected provisioning error.");
                } catch (Exception e2) {
                    Log.w(TAG, "provideKeyResponse Exception " + e2);
                    throw e2;
                }
            } else {
                bArr3 = bArr;
            }
            bArrProvideKeyResponse = this.mDrmObj.provideKeyResponse(bArr3, bArr2);
            Log.v(TAG, "provideKeyResponse: keySetId: " + bArr + " response: " + bArr2 + " --> " + bArrProvideKeyResponse);
        }
        return bArrProvideKeyResponse;
    }

    public void restoreKeys(byte[] bArr) throws NoDrmSchemeException {
        Log.v(TAG, "restoreKeys: keySetId: " + bArr);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                Log.w(TAG, "restoreKeys NoDrmSchemeException");
                throw new NoDrmSchemeException("restoreKeys: Has to set a DRM scheme first.");
            }
            try {
                this.mDrmObj.restoreKeys(this.mDrmSessionId, bArr);
            } catch (Exception e) {
                Log.w(TAG, "restoreKeys Exception " + e);
                throw e;
            }
        }
    }

    public String getDrmPropertyString(String str) throws NoDrmSchemeException {
        String propertyString;
        Log.v(TAG, "getDrmPropertyString: propertyName: " + str);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme && !this.mDrmConfigAllowed) {
                Log.w(TAG, "getDrmPropertyString NoDrmSchemeException");
                throw new NoDrmSchemeException("getDrmPropertyString: Has to prepareDrm() first.");
            }
            try {
                propertyString = this.mDrmObj.getPropertyString(str);
            } catch (Exception e) {
                Log.w(TAG, "getDrmPropertyString Exception " + e);
                throw e;
            }
        }
        Log.v(TAG, "getDrmPropertyString: propertyName: " + str + " --> value: " + propertyString);
        return propertyString;
    }

    public void setDrmPropertyString(String str, String str2) throws NoDrmSchemeException {
        Log.v(TAG, "setDrmPropertyString: propertyName: " + str + " value: " + str2);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme && !this.mDrmConfigAllowed) {
                Log.w(TAG, "setDrmPropertyString NoDrmSchemeException");
                throw new NoDrmSchemeException("setDrmPropertyString: Has to prepareDrm() first.");
            }
            try {
                this.mDrmObj.setPropertyString(str, str2);
            } catch (Exception e) {
                Log.w(TAG, "setDrmPropertyString Exception " + e);
                throw e;
            }
        }
    }

    public static final class DrmInfo {
        private Map<UUID, byte[]> mapPssh;
        private UUID[] supportedSchemes;

        public Map<UUID, byte[]> getPssh() {
            return this.mapPssh;
        }

        public UUID[] getSupportedSchemes() {
            return this.supportedSchemes;
        }

        private DrmInfo(Map<UUID, byte[]> map, UUID[] uuidArr) {
            this.mapPssh = map;
            this.supportedSchemes = uuidArr;
        }

        private DrmInfo(Parcel parcel) {
            Log.v(MediaPlayer.TAG, "DrmInfo(" + parcel + ") size " + parcel.dataSize());
            int i = parcel.readInt();
            byte[] bArr = new byte[i];
            parcel.readByteArray(bArr);
            Log.v(MediaPlayer.TAG, "DrmInfo() PSSH: " + arrToHex(bArr));
            this.mapPssh = parsePSSH(bArr, i);
            Log.v(MediaPlayer.TAG, "DrmInfo() PSSH: " + this.mapPssh);
            int i2 = parcel.readInt();
            this.supportedSchemes = new UUID[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                byte[] bArr2 = new byte[16];
                parcel.readByteArray(bArr2);
                this.supportedSchemes[i3] = bytesToUUID(bArr2);
                Log.v(MediaPlayer.TAG, "DrmInfo() supportedScheme[" + i3 + "]: " + this.supportedSchemes[i3]);
            }
            Log.v(MediaPlayer.TAG, "DrmInfo() Parcel psshsize: " + i + " supportedDRMsCount: " + i2);
        }

        private DrmInfo makeCopy() {
            return new DrmInfo(this.mapPssh, this.supportedSchemes);
        }

        private String arrToHex(byte[] bArr) {
            String str = "0x";
            for (byte b : bArr) {
                str = str + String.format("%02x", Byte.valueOf(b));
            }
            return str;
        }

        private UUID bytesToUUID(byte[] bArr) {
            long j = 0;
            long j2 = 0;
            for (int i = 0; i < 8; i++) {
                int i2 = 8 * (7 - i);
                j |= (((long) bArr[i]) & 255) << i2;
                j2 |= (((long) bArr[i + 8]) & 255) << i2;
            }
            return new UUID(j, j2);
        }

        private Map<UUID, byte[]> parsePSSH(byte[] bArr, int i) {
            HashMap map = new HashMap();
            int i2 = i;
            int i3 = 0;
            int i4 = 0;
            while (i2 > 0) {
                if (i2 < 16) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse UUID: (%d < 16) pssh: %d", Integer.valueOf(i2), Integer.valueOf(i)));
                    return null;
                }
                int i5 = i3 + 16;
                UUID uuidBytesToUUID = bytesToUUID(Arrays.copyOfRange(bArr, i3, i5));
                int i6 = i2 - 16;
                if (i6 < 4) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse datalen: (%d < 4) pssh: %d", Integer.valueOf(i6), Integer.valueOf(i)));
                    return null;
                }
                int i7 = i5 + 4;
                byte[] bArrCopyOfRange = Arrays.copyOfRange(bArr, i5, i7);
                int i8 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? ((bArrCopyOfRange[2] & 255) << 16) | ((bArrCopyOfRange[3] & 255) << 24) | ((bArrCopyOfRange[1] & 255) << 8) | (bArrCopyOfRange[0] & 255) : ((bArrCopyOfRange[1] & 255) << 16) | ((bArrCopyOfRange[0] & 255) << 24) | ((bArrCopyOfRange[2] & 255) << 8) | (bArrCopyOfRange[3] & 255);
                int i9 = i6 - 4;
                if (i9 < i8) {
                    Log.w(MediaPlayer.TAG, String.format("parsePSSH: len is too short to parse data: (%d < %d) pssh: %d", Integer.valueOf(i9), Integer.valueOf(i8), Integer.valueOf(i)));
                    return null;
                }
                int i10 = i7 + i8;
                byte[] bArrCopyOfRange2 = Arrays.copyOfRange(bArr, i7, i10);
                i2 = i9 - i8;
                Log.v(MediaPlayer.TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d", Integer.valueOf(i4), uuidBytesToUUID, arrToHex(bArrCopyOfRange2), Integer.valueOf(i)));
                i4++;
                map.put(uuidBytesToUUID, bArrCopyOfRange2);
                i3 = i10;
            }
            return map;
        }
    }

    public static final class NoDrmSchemeException extends MediaDrmException {
        public NoDrmSchemeException(String str) {
            super(str);
        }
    }

    public static final class ProvisioningNetworkErrorException extends MediaDrmException {
        public ProvisioningNetworkErrorException(String str) {
            super(str);
        }
    }

    public static final class ProvisioningServerErrorException extends MediaDrmException {
        public ProvisioningServerErrorException(String str) {
            super(str);
        }
    }

    private void prepareDrm_createDrmStep(UUID uuid) throws Exception {
        Log.v(TAG, "prepareDrm_createDrmStep: UUID: " + uuid);
        try {
            this.mDrmObj = new MediaDrm(uuid);
            Log.v(TAG, "prepareDrm_createDrmStep: Created mDrmObj=" + this.mDrmObj);
        } catch (Exception e) {
            Log.e(TAG, "prepareDrm_createDrmStep: MediaDrm failed with " + e);
            throw e;
        }
    }

    private void prepareDrm_openSessionStep(UUID uuid) throws Exception {
        Log.v(TAG, "prepareDrm_openSessionStep: uuid: " + uuid);
        try {
            this.mDrmSessionId = this.mDrmObj.openSession();
            Log.v(TAG, "prepareDrm_openSessionStep: mDrmSessionId=" + this.mDrmSessionId);
            _prepareDrm(getByteArrayFromUUID(uuid), this.mDrmSessionId);
            Log.v(TAG, "prepareDrm_openSessionStep: _prepareDrm/Crypto succeeded");
        } catch (Exception e) {
            Log.e(TAG, "prepareDrm_openSessionStep: open/crypto failed with " + e);
            throw e;
        }
    }

    private class ProvisioningThread extends Thread {
        public static final int TIMEOUT_MS = 60000;
        private Object drmLock;
        private boolean finished;
        private MediaPlayer mediaPlayer;
        private OnDrmPreparedHandlerDelegate onDrmPreparedHandlerDelegate;
        private int status;
        private String urlStr;
        private UUID uuid;

        private ProvisioningThread() {
        }

        public int status() {
            return this.status;
        }

        public ProvisioningThread initialize(MediaDrm.ProvisionRequest provisionRequest, UUID uuid, MediaPlayer mediaPlayer) {
            this.drmLock = mediaPlayer.mDrmLock;
            this.onDrmPreparedHandlerDelegate = mediaPlayer.mOnDrmPreparedHandlerDelegate;
            this.mediaPlayer = mediaPlayer;
            this.urlStr = provisionRequest.getDefaultUrl() + "&signedRequest=" + new String(provisionRequest.getData());
            this.uuid = uuid;
            this.status = 3;
            Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread is initialised url: " + this.urlStr);
            return this;
        }

        @Override
        public void run() throws Throwable {
            byte[] fully;
            boolean z;
            boolean zResumePrepareDrm;
            boolean zResumePrepareDrm2;
            URL url;
            HttpURLConnection httpURLConnection;
            Exception e;
            try {
                try {
                    url = new URL(this.urlStr);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoOutput(false);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setConnectTimeout(60000);
                    httpURLConnection.setReadTimeout(60000);
                    httpURLConnection.connect();
                    fully = Streams.readFully(httpURLConnection.getInputStream());
                    try {
                        Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread run: response " + fully.length + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + fully);
                    } catch (Exception e2) {
                        e = e2;
                        this.status = 1;
                        Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: connect " + e + " url: " + url);
                    }
                } catch (Exception e3) {
                    fully = null;
                    e = e3;
                } catch (Throwable th2) {
                    fully = null;
                    th = th2;
                    httpURLConnection.disconnect();
                    throw th;
                }
            } catch (Exception e4) {
                e = e4;
                fully = null;
                this.status = 1;
                Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: openConnection " + e);
                if (fully == null) {
                }
                if (this.onDrmPreparedHandlerDelegate == null) {
                }
                this.finished = true;
            }
            try {
                httpURLConnection.disconnect();
            } catch (Exception e5) {
                e = e5;
                this.status = 1;
                Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: openConnection " + e);
            }
            if (fully == null) {
                try {
                    MediaPlayer.this.mDrmObj.provideProvisionResponse(fully);
                    Log.v(MediaPlayer.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse SUCCEEDED!");
                    z = true;
                } catch (Exception e6) {
                    this.status = 2;
                    Log.w(MediaPlayer.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse " + e6);
                    z = false;
                }
            } else {
                z = false;
            }
            if (this.onDrmPreparedHandlerDelegate == null) {
                synchronized (this.drmLock) {
                    if (z) {
                        try {
                            zResumePrepareDrm2 = this.mediaPlayer.resumePrepareDrm(this.uuid);
                            this.status = zResumePrepareDrm2 ? 0 : 3;
                        } finally {
                        }
                    } else {
                        zResumePrepareDrm2 = false;
                    }
                    this.mediaPlayer.mDrmProvisioningInProgress = false;
                    this.mediaPlayer.mPrepareDrmInProgress = false;
                    if (!zResumePrepareDrm2) {
                        MediaPlayer.this.cleanDrmObj();
                    }
                }
                this.onDrmPreparedHandlerDelegate.notifyClient(this.status);
            } else {
                if (z) {
                    zResumePrepareDrm = this.mediaPlayer.resumePrepareDrm(this.uuid);
                    this.status = zResumePrepareDrm ? 0 : 3;
                } else {
                    zResumePrepareDrm = false;
                }
                this.mediaPlayer.mDrmProvisioningInProgress = false;
                this.mediaPlayer.mPrepareDrmInProgress = false;
                if (!zResumePrepareDrm) {
                    MediaPlayer.this.cleanDrmObj();
                }
            }
            this.finished = true;
        }
    }

    private int HandleProvisioninig(UUID uuid) {
        if (this.mDrmProvisioningInProgress) {
            Log.e(TAG, "HandleProvisioninig: Unexpected mDrmProvisioningInProgress");
            return 3;
        }
        MediaDrm.ProvisionRequest provisionRequest = this.mDrmObj.getProvisionRequest();
        if (provisionRequest == null) {
            Log.e(TAG, "HandleProvisioninig: getProvisionRequest returned null.");
            return 3;
        }
        Log.v(TAG, "HandleProvisioninig provReq  data: " + provisionRequest.getData() + " url: " + provisionRequest.getDefaultUrl());
        this.mDrmProvisioningInProgress = true;
        this.mDrmProvisioningThread = new ProvisioningThread().initialize(provisionRequest, uuid, this);
        this.mDrmProvisioningThread.start();
        if (this.mOnDrmPreparedHandlerDelegate != null) {
            return 0;
        }
        try {
            this.mDrmProvisioningThread.join();
        } catch (Exception e) {
            Log.w(TAG, "HandleProvisioninig: Thread.join Exception " + e);
        }
        int iStatus = this.mDrmProvisioningThread.status();
        this.mDrmProvisioningThread = null;
        return iStatus;
    }

    private boolean resumePrepareDrm(UUID uuid) {
        Log.v(TAG, "resumePrepareDrm: uuid: " + uuid);
        try {
            prepareDrm_openSessionStep(uuid);
            this.mDrmUUID = uuid;
            this.mActiveDrmScheme = true;
            return true;
        } catch (Exception e) {
            Log.w(TAG, "HandleProvisioninig: Thread run _prepareDrm resume failed with " + e);
            return false;
        }
    }

    private void resetDrmState() {
        synchronized (this.mDrmLock) {
            Log.v(TAG, "resetDrmState:  mDrmInfo=" + this.mDrmInfo + " mDrmProvisioningThread=" + this.mDrmProvisioningThread + " mPrepareDrmInProgress=" + this.mPrepareDrmInProgress + " mActiveDrmScheme=" + this.mActiveDrmScheme);
            this.mDrmInfoResolved = false;
            this.mDrmInfo = null;
            if (this.mDrmProvisioningThread != null) {
                try {
                    this.mDrmProvisioningThread.join();
                } catch (InterruptedException e) {
                    Log.w(TAG, "resetDrmState: ProvThread.join Exception " + e);
                }
                this.mDrmProvisioningThread = null;
                this.mPrepareDrmInProgress = false;
                this.mActiveDrmScheme = false;
                cleanDrmObj();
            } else {
                this.mPrepareDrmInProgress = false;
                this.mActiveDrmScheme = false;
                cleanDrmObj();
            }
        }
    }

    private void cleanDrmObj() {
        Log.v(TAG, "cleanDrmObj: mDrmObj=" + this.mDrmObj + " mDrmSessionId=" + this.mDrmSessionId);
        if (this.mDrmSessionId != null) {
            this.mDrmObj.closeSession(this.mDrmSessionId);
            this.mDrmSessionId = null;
        }
        if (this.mDrmObj != null) {
            this.mDrmObj.release();
            this.mDrmObj = null;
        }
    }

    private static final byte[] getByteArrayFromUUID(UUID uuid) {
        long mostSignificantBits = uuid.getMostSignificantBits();
        long leastSignificantBits = uuid.getLeastSignificantBits();
        byte[] bArr = new byte[16];
        for (int i = 0; i < 8; i++) {
            int i2 = (7 - i) * 8;
            bArr[i] = (byte) (mostSignificantBits >>> i2);
            bArr[8 + i] = (byte) (leastSignificantBits >>> i2);
        }
        return bArr;
    }

    private boolean isVideoScalingModeSupported(int i) {
        return i == 1 || i == 2;
    }

    static class TimeProvider implements OnSeekCompleteListener, MediaTimeProvider {
        private static final long MAX_EARLY_CALLBACK_US = 1000;
        private static final long MAX_NS_WITHOUT_POSITION_CHECK = 5000000000L;
        private static final int NOTIFY = 1;
        private static final int NOTIFY_SEEK = 3;
        private static final int NOTIFY_STOP = 2;
        private static final int NOTIFY_TIME = 0;
        private static final int NOTIFY_TRACK_DATA = 4;
        private static final String TAG = "MTP";
        private static final long TIME_ADJUSTMENT_RATE = 2;
        private boolean mBuffering;
        private Handler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastReportedTime;
        private long mLastTimeUs;
        private MediaTimeProvider.OnMediaTimeListener[] mListeners;
        private MediaPlayer mPlayer;
        private boolean mRefresh;
        private long[] mTimes;
        private boolean mPaused = true;
        private boolean mStopped = true;
        private boolean mPausing = false;
        private boolean mSeeking = false;
        public boolean DEBUG = !"user".equals(Build.TYPE);

        public TimeProvider(MediaPlayer mediaPlayer) {
            this.mLastTimeUs = 0L;
            this.mRefresh = false;
            this.mPlayer = mediaPlayer;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
            }
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper == null && (looperMyLooper = Looper.getMainLooper()) == null) {
                this.mHandlerThread = new HandlerThread("MediaPlayerMTPEventThread", -2);
                this.mHandlerThread.start();
                looperMyLooper = this.mHandlerThread.getLooper();
            }
            this.mEventHandler = new EventHandler(looperMyLooper);
            this.mListeners = new MediaTimeProvider.OnMediaTimeListener[0];
            this.mTimes = new long[0];
            this.mLastTimeUs = 0L;
        }

        private void scheduleNotification(int i, long j) {
            if (this.mSeeking && i == 0) {
                return;
            }
            if (this.DEBUG) {
                Log.v(TAG, "scheduleNotification " + i + " in " + j);
            }
            this.mEventHandler.removeMessages(1);
            this.mEventHandler.sendMessageDelayed(this.mEventHandler.obtainMessage(1, i, 0), (int) (j / 1000));
        }

        public void close() {
            this.mEventHandler.removeMessages(1);
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
                this.mHandlerThread = null;
            }
        }

        protected void finalize() {
            if (this.mHandlerThread != null) {
                this.mHandlerThread.quitSafely();
            }
        }

        public void onNotifyTime() {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onNotifyTime: ");
                }
                scheduleNotification(0, 0L);
            }
        }

        public void onPaused(boolean z) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onPaused: " + z);
                }
                if (this.mStopped) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    scheduleNotification(3, 0L);
                } else {
                    this.mPausing = z;
                    this.mSeeking = false;
                    scheduleNotification(0, 0L);
                }
            }
        }

        public void onBuffering(boolean z) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "onBuffering: " + z);
                }
                this.mBuffering = z;
                scheduleNotification(0, 0L);
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
        public void onSeekComplete(MediaPlayer mediaPlayer) {
            synchronized (this) {
                this.mStopped = false;
                this.mSeeking = true;
                scheduleNotification(3, 0L);
            }
        }

        public void onNewPlayer() {
            if (this.mRefresh) {
                synchronized (this) {
                    this.mStopped = false;
                    this.mSeeking = true;
                    this.mBuffering = false;
                    scheduleNotification(3, 0L);
                }
            }
        }

        private synchronized void notifySeek() {
            this.mSeeking = false;
            try {
                long currentTimeUs = getCurrentTimeUs(true, false);
                if (this.DEBUG) {
                    Log.d(TAG, "onSeekComplete at " + currentTimeUs);
                }
                for (MediaTimeProvider.OnMediaTimeListener onMediaTimeListener : this.mListeners) {
                    if (onMediaTimeListener == null) {
                        break;
                    }
                    onMediaTimeListener.onSeek(currentTimeUs);
                }
            } catch (IllegalStateException e) {
                if (this.DEBUG) {
                    Log.d(TAG, "onSeekComplete but no player");
                }
                this.mPausing = true;
                notifyTimedEvent(false);
            }
        }

        private synchronized void notifyTrackData(Pair<SubtitleTrack, byte[]> pair) {
            pair.first.onData(pair.second, true, -1L);
        }

        private synchronized void notifyStop() {
            for (MediaTimeProvider.OnMediaTimeListener onMediaTimeListener : this.mListeners) {
                if (onMediaTimeListener == null) {
                    break;
                }
                onMediaTimeListener.onStop();
            }
        }

        private int registerListener(MediaTimeProvider.OnMediaTimeListener onMediaTimeListener) {
            int i = 0;
            while (i < this.mListeners.length && this.mListeners[i] != onMediaTimeListener && this.mListeners[i] != null) {
                i++;
            }
            if (i >= this.mListeners.length) {
                int i2 = i + 1;
                MediaTimeProvider.OnMediaTimeListener[] onMediaTimeListenerArr = new MediaTimeProvider.OnMediaTimeListener[i2];
                long[] jArr = new long[i2];
                System.arraycopy(this.mListeners, 0, onMediaTimeListenerArr, 0, this.mListeners.length);
                System.arraycopy(this.mTimes, 0, jArr, 0, this.mTimes.length);
                this.mListeners = onMediaTimeListenerArr;
                this.mTimes = jArr;
            }
            if (this.mListeners[i] == null) {
                this.mListeners[i] = onMediaTimeListener;
                this.mTimes[i] = -1;
            }
            return i;
        }

        @Override
        public void notifyAt(long j, MediaTimeProvider.OnMediaTimeListener onMediaTimeListener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "notifyAt " + j);
                }
                this.mTimes[registerListener(onMediaTimeListener)] = j;
                scheduleNotification(0, 0L);
            }
        }

        @Override
        public void scheduleUpdate(MediaTimeProvider.OnMediaTimeListener onMediaTimeListener) {
            synchronized (this) {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduleUpdate");
                }
                int iRegisterListener = registerListener(onMediaTimeListener);
                if (!this.mStopped) {
                    this.mTimes[iRegisterListener] = 0;
                    scheduleNotification(0, 0L);
                }
            }
        }

        @Override
        public void cancelNotifications(MediaTimeProvider.OnMediaTimeListener onMediaTimeListener) {
            synchronized (this) {
                int i = 0;
                while (true) {
                    if (i >= this.mListeners.length) {
                        break;
                    }
                    if (this.mListeners[i] == onMediaTimeListener) {
                        int i2 = i + 1;
                        System.arraycopy(this.mListeners, i2, this.mListeners, i, (this.mListeners.length - i) - 1);
                        System.arraycopy(this.mTimes, i2, this.mTimes, i, (this.mTimes.length - i) - 1);
                        this.mListeners[this.mListeners.length - 1] = null;
                        this.mTimes[this.mTimes.length - 1] = -1;
                        break;
                    }
                    if (this.mListeners[i] == null) {
                        break;
                    } else {
                        i++;
                    }
                }
                scheduleNotification(0, 0L);
            }
        }

        private synchronized void notifyTimedEvent(boolean z) {
            long currentTimeUs;
            try {
                currentTimeUs = getCurrentTimeUs(z, true);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
                this.mPausing = true;
                currentTimeUs = getCurrentTimeUs(z, true);
            }
            if (this.mSeeking) {
                return;
            }
            if (this.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("notifyTimedEvent(");
                sb.append(this.mLastTimeUs);
                sb.append(" -> ");
                sb.append(currentTimeUs);
                sb.append(") from {");
                boolean z2 = true;
                for (long j : this.mTimes) {
                    if (j != -1) {
                        if (!z2) {
                            sb.append(", ");
                        }
                        sb.append(j);
                        z2 = false;
                    }
                }
                sb.append("}");
                Log.d(TAG, sb.toString());
            }
            Vector vector = new Vector();
            long j2 = currentTimeUs;
            for (int i = 0; i < this.mTimes.length && this.mListeners[i] != null; i++) {
                if (this.mTimes[i] > -1) {
                    if (this.mTimes[i] <= 1000 + currentTimeUs) {
                        vector.add(this.mListeners[i]);
                        if (this.DEBUG) {
                            Log.d(TAG, Environment.MEDIA_REMOVED);
                        }
                        this.mTimes[i] = -1;
                    } else if (j2 == currentTimeUs || this.mTimes[i] < j2) {
                        j2 = this.mTimes[i];
                    }
                }
            }
            if (j2 <= currentTimeUs || this.mPaused) {
                this.mEventHandler.removeMessages(1);
            } else {
                if (this.DEBUG) {
                    Log.d(TAG, "scheduling for " + j2 + " and " + currentTimeUs);
                }
                this.mPlayer.notifyAt(j2);
            }
            Iterator it = vector.iterator();
            while (it.hasNext()) {
                ((MediaTimeProvider.OnMediaTimeListener) it.next()).onTimedEvent(currentTimeUs);
            }
        }

        @Override
        public long getCurrentTimeUs(boolean z, boolean z2) throws IllegalStateException {
            synchronized (this) {
                if (this.mPaused && !z) {
                    return this.mLastReportedTime;
                }
                try {
                    this.mLastTimeUs = ((long) this.mPlayer.getCurrentPosition()) * 1000;
                    this.mPaused = !this.mPlayer.isPlaying() || this.mBuffering;
                    if (this.DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(this.mPaused ? "paused" : "playing");
                        sb.append(" at ");
                        sb.append(this.mLastTimeUs);
                        Log.v(TAG, sb.toString());
                    }
                    if (!z2 || this.mLastTimeUs >= this.mLastReportedTime) {
                        this.mLastReportedTime = this.mLastTimeUs;
                    } else if (this.mLastReportedTime - this.mLastTimeUs > TimeUtils.NANOS_PER_MS) {
                        this.mStopped = false;
                        this.mSeeking = true;
                        scheduleNotification(3, 0L);
                    }
                    return this.mLastReportedTime;
                } catch (IllegalStateException e) {
                    if (this.mPausing) {
                        this.mPausing = false;
                        if (!z2 || this.mLastReportedTime < this.mLastTimeUs) {
                            this.mLastReportedTime = this.mLastTimeUs;
                        }
                        this.mPaused = true;
                        if (this.DEBUG) {
                            Log.d(TAG, "illegal state, but pausing: estimating at " + this.mLastReportedTime);
                        }
                        return this.mLastReportedTime;
                    }
                    throw e;
                }
            }
        }

        private class EventHandler extends Handler {
            public EventHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                if (TimeProvider.this.DEBUG) {
                    Log.d(TimeProvider.TAG, "handleMessage_notify msg:(" + message.what + ", " + message.arg1 + ", " + message.arg2 + ")");
                }
                if (message.what == 1) {
                    int i = message.arg1;
                    if (i != 0) {
                        switch (i) {
                            case 2:
                                TimeProvider.this.notifyStop();
                                break;
                            case 3:
                                TimeProvider.this.notifySeek();
                                break;
                            case 4:
                                TimeProvider.this.notifyTrackData((Pair) message.obj);
                                break;
                        }
                    }
                    TimeProvider.this.notifyTimedEvent(true);
                }
            }
        }
    }

    public static final class MetricsConstants {
        public static final String CODEC_AUDIO = "android.media.mediaplayer.audio.codec";
        public static final String CODEC_VIDEO = "android.media.mediaplayer.video.codec";
        public static final String DURATION = "android.media.mediaplayer.durationMs";
        public static final String ERRORS = "android.media.mediaplayer.err";
        public static final String ERROR_CODE = "android.media.mediaplayer.errcode";
        public static final String FRAMES = "android.media.mediaplayer.frames";
        public static final String FRAMES_DROPPED = "android.media.mediaplayer.dropped";
        public static final String HEIGHT = "android.media.mediaplayer.height";
        public static final String MIME_TYPE_AUDIO = "android.media.mediaplayer.audio.mime";
        public static final String MIME_TYPE_VIDEO = "android.media.mediaplayer.video.mime";
        public static final String PLAYING = "android.media.mediaplayer.playingMs";
        public static final String WIDTH = "android.media.mediaplayer.width";

        private MetricsConstants() {
        }
    }
}
