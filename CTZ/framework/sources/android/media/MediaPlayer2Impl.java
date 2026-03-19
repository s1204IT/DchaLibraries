package android.media;

import android.app.ActivityThread;
import android.app.Application;
import android.app.backup.FullBackup;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioRouting;
import android.media.AudioTrack;
import android.media.MediaDrm;
import android.media.MediaPlayer2;
import android.media.MediaPlayer2Impl;
import android.media.MediaPlayerBase;
import android.media.MediaTimeProvider;
import android.media.SubtitleController;
import android.media.SubtitleTrack;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
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
import dalvik.system.CloseGuard;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoBridge;
import libcore.io.Streams;

public final class MediaPlayer2Impl extends MediaPlayer2 {
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
    private static final int MEDIA_INFO = 200;
    private static final int MEDIA_META_DATA = 202;
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
    private static final int NEXT_SOURCE_STATE_ERROR = -1;
    private static final int NEXT_SOURCE_STATE_INIT = 0;
    private static final int NEXT_SOURCE_STATE_PREPARED = 2;
    private static final int NEXT_SOURCE_STATE_PREPARING = 1;
    private static final String TAG = "MediaPlayer2Impl";
    private boolean mActiveDrmScheme;
    private AtomicInteger mBufferedPercentageCurrent;
    private AtomicInteger mBufferedPercentageNext;
    private DataSourceDesc mCurrentDSD;
    private long mCurrentSrcId;

    @GuardedBy("mTaskLock")
    private Task mCurrentTask;
    private boolean mDrmConfigAllowed;
    private ArrayList<Pair<Executor, MediaPlayer2.DrmEventCallback>> mDrmEventCallbackRecords;
    private final Object mDrmEventCbLock;
    private DrmInfoImpl mDrmInfoImpl;
    private boolean mDrmInfoResolved;
    private final Object mDrmLock;
    private MediaDrm mDrmObj;
    private boolean mDrmProvisioningInProgress;
    private ProvisioningThread mDrmProvisioningThread;
    private byte[] mDrmSessionId;
    private UUID mDrmUUID;
    private ArrayList<Pair<Executor, MediaPlayer2.MediaPlayer2EventCallback>> mEventCallbackRecords;
    private final Object mEventCbLock;
    private EventHandler mEventHandler;
    private HandlerThread mHandlerThread;
    private BitSet mInbandTrackIndices;
    private Vector<Pair<Integer, SubtitleTrack>> mIndexTrackPairs;
    private int mListenerContext;
    private long mNativeContext;
    private long mNativeSurfaceTexture;
    private List<DataSourceDesc> mNextDSDs;
    private boolean mNextSourcePlayPending;
    private int mNextSourceState;
    private long mNextSrcId;
    private MediaPlayer2.OnDrmConfigHelper mOnDrmConfigHelper;
    private MediaPlayer2.OnSubtitleDataListener mOnSubtitleDataListener;
    private Vector<InputStream> mOpenSubtitleSources;

    @GuardedBy("mTaskLock")
    private final List<Task> mPendingTasks;
    private AudioDeviceInfo mPreferredDevice;
    private boolean mPrepareDrmInProgress;

    @GuardedBy("mRoutingChangeListeners")
    private ArrayMap<AudioRouting.OnRoutingChangedListener, NativeRoutingEventHandlerDelegate> mRoutingChangeListeners;
    private boolean mScreenOnWhilePlaying;
    private int mSelectedSubtitleTrackIndex;
    private long mSrcIdGenerator;
    private boolean mStayAwake;
    private SubtitleController mSubtitleController;
    private MediaPlayer2.OnSubtitleDataListener mSubtitleDataListener;
    private SurfaceHolder mSurfaceHolder;
    private final Handler mTaskHandler;
    private final Object mTaskLock;
    private TimeProvider mTimeProvider;
    private volatile float mVolume;
    private PowerManager.WakeLock mWakeLock = null;
    private int mStreamType = Integer.MIN_VALUE;
    private final CloseGuard mGuard = CloseGuard.get();
    private final Object mSrcLock = new Object();

    private native void _attachAuxEffect(int i);

    private native int _getAudioStreamType() throws IllegalStateException;

    private native void _notifyAt(long j);

    private native void _pause() throws IllegalStateException;

    private native void _prepareDrm(byte[] bArr, byte[] bArr2);

    private native void _release();

    private native void _releaseDrm();

    private native void _reset();

    private final native void _seekTo(long j, int i);

    private native void _setAudioSessionId(int i);

    private native void _setAuxEffectSendLevel(float f);

    private native void _setBufferingParams(BufferingParams bufferingParams);

    private native void _setPlaybackParams(PlaybackParams playbackParams);

    private native void _setSyncParams(SyncParams syncParams);

    private native void _setVideoSurface(Surface surface);

    private native void _setVolume(float f, float f2);

    private native void _start() throws IllegalStateException;

    private native void _stop() throws IllegalStateException;

    private native Parcel getParameter(int i);

    private native void nativeHandleDataSourceCallback(boolean z, long j, Media2DataSource media2DataSource);

    private native void nativeHandleDataSourceFD(boolean z, long j, FileDescriptor fileDescriptor, long j2, long j3) throws IOException;

    private native void nativeHandleDataSourceUrl(boolean z, long j, Media2HTTPService media2HTTPService, String str, String[] strArr, String[] strArr2) throws IOException;

    private native void nativePlayNextDataSource(long j);

    private final native void native_enableDeviceCallback(boolean z);

    private final native void native_finalize();

    private native int native_getMediaPlayer2State();

    private final native boolean native_getMetadata(boolean z, boolean z2, Parcel parcel);

    private native PersistableBundle native_getMetrics();

    private final native int native_getRoutedDeviceId();

    private static final native void native_init();

    private final native int native_invoke(Parcel parcel, Parcel parcel2);

    private final native int native_setMetadataFilter(Parcel parcel);

    private final native boolean native_setOutputDevice(int i);

    private final native void native_setup(Object obj);

    private static final native void native_stream_event_onStreamDataRequest(long j, long j2, long j3);

    private static final native void native_stream_event_onStreamPresentationEnd(long j, long j2);

    private static final native void native_stream_event_onTearDown(long j, long j2);

    private native void setLooping(boolean z);

    private native boolean setParameter(int i, Parcel parcel);

    public native void _prepare();

    @Override
    public native int getAudioSessionId();

    @Override
    public native BufferingParams getBufferingParams();

    @Override
    public native long getCurrentPosition();

    @Override
    public native long getDuration();

    @Override
    public native PlaybackParams getPlaybackParams();

    @Override
    public native SyncParams getSyncParams();

    @Override
    public native int getVideoHeight();

    @Override
    public native int getVideoWidth();

    @Override
    public native boolean isLooping();

    @Override
    public native boolean isPlaying();

    static long access$708(MediaPlayer2Impl mediaPlayer2Impl) {
        long j = mediaPlayer2Impl.mSrcIdGenerator;
        mediaPlayer2Impl.mSrcIdGenerator = 1 + j;
        return j;
    }

    static {
        System.loadLibrary("media2_jni");
        native_init();
    }

    public MediaPlayer2Impl() {
        this.mSrcIdGenerator = 0L;
        long j = this.mSrcIdGenerator;
        this.mSrcIdGenerator = j + 1;
        this.mCurrentSrcId = j;
        long j2 = this.mSrcIdGenerator;
        this.mSrcIdGenerator = 1 + j2;
        this.mNextSrcId = j2;
        this.mNextSourceState = 0;
        this.mNextSourcePlayPending = false;
        this.mBufferedPercentageCurrent = new AtomicInteger(0);
        this.mBufferedPercentageNext = new AtomicInteger(0);
        this.mVolume = 1.0f;
        this.mDrmLock = new Object();
        this.mTaskLock = new Object();
        this.mPendingTasks = new LinkedList();
        this.mPreferredDevice = null;
        this.mRoutingChangeListeners = new ArrayMap<>();
        this.mIndexTrackPairs = new Vector<>();
        this.mInbandTrackIndices = new BitSet();
        this.mSelectedSubtitleTrackIndex = -1;
        this.mSubtitleDataListener = new MediaPlayer2.OnSubtitleDataListener() {
            @Override
            public void onSubtitleData(MediaPlayer2 mediaPlayer2, SubtitleData subtitleData) {
                int trackIndex = subtitleData.getTrackIndex();
                synchronized (MediaPlayer2Impl.this.mIndexTrackPairs) {
                    for (Pair pair : MediaPlayer2Impl.this.mIndexTrackPairs) {
                        if (pair.first != 0 && ((Integer) pair.first).intValue() == trackIndex && pair.second != 0) {
                            ((SubtitleTrack) pair.second).onData(subtitleData);
                        }
                    }
                }
            }
        };
        this.mEventCbLock = new Object();
        this.mEventCallbackRecords = new ArrayList<>();
        this.mDrmEventCbLock = new Object();
        this.mDrmEventCallbackRecords = new ArrayList<>();
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
        this.mHandlerThread = new HandlerThread("MediaPlayer2TaskThread");
        this.mHandlerThread.start();
        this.mTaskHandler = new Handler(this.mHandlerThread.getLooper());
        this.mTimeProvider = new TimeProvider(this);
        this.mOpenSubtitleSources = new Vector<>();
        this.mGuard.open("close");
        native_setup(new WeakReference(this));
    }

    @Override
    public void close() {
        synchronized (this.mGuard) {
            release();
        }
    }

    @Override
    public void play() {
        addTask(new Task(5, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.stayAwake(true);
                MediaPlayer2Impl.this._start();
            }
        });
    }

    @Override
    public void prepare() {
        addTask(new Task(6, true) {
            @Override
            void process() {
                MediaPlayer2Impl.this._prepare();
            }
        });
    }

    @Override
    public void pause() {
        addTask(new Task(4, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.stayAwake(false);
                MediaPlayer2Impl.this._pause();
            }
        });
    }

    @Override
    public void skipToNext() {
        addTask(new Task(29, false) {
            @Override
            void process() {
            }
        });
    }

    @Override
    public long getBufferedPosition() {
        return (getDuration() * ((long) this.mBufferedPercentageCurrent.get())) / 100;
    }

    @Override
    public int getPlayerState() {
        switch (getMediaPlayer2State()) {
            case 1:
                return 0;
            case 2:
            case 3:
                return 1;
            case 4:
                return 2;
            default:
                return 3;
        }
    }

    @Override
    public int getBufferingState() {
        return 0;
    }

    @Override
    public void setAudioAttributes(final AudioAttributes audioAttributes) {
        addTask(new Task(16, false) {
            @Override
            void process() {
                if (audioAttributes == null) {
                    throw new IllegalArgumentException("Cannot set AudioAttributes to null");
                }
                Parcel parcelObtain = Parcel.obtain();
                audioAttributes.writeToParcel(parcelObtain, 1);
                MediaPlayer2Impl.this.setParameter(1400, parcelObtain);
                parcelObtain.recycle();
            }
        });
    }

    @Override
    public AudioAttributes getAudioAttributes() {
        Parcel parameter = getParameter(1400);
        AudioAttributes audioAttributesCreateFromParcel = AudioAttributes.CREATOR.createFromParcel(parameter);
        parameter.recycle();
        return audioAttributesCreateFromParcel;
    }

    @Override
    public void setDataSource(final DataSourceDesc dataSourceDesc) {
        addTask(new Task(19, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(dataSourceDesc, "the DataSourceDesc cannot be null");
                synchronized (MediaPlayer2Impl.this.mSrcLock) {
                    MediaPlayer2Impl.this.mCurrentDSD = dataSourceDesc;
                    MediaPlayer2Impl.this.mCurrentSrcId = MediaPlayer2Impl.access$708(MediaPlayer2Impl.this);
                    try {
                        MediaPlayer2Impl.this.handleDataSource(true, dataSourceDesc, MediaPlayer2Impl.this.mCurrentSrcId);
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    @Override
    public void setNextDataSource(final DataSourceDesc dataSourceDesc) {
        addTask(new Task(22, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(dataSourceDesc, "the DataSourceDesc cannot be null");
                synchronized (MediaPlayer2Impl.this.mSrcLock) {
                    MediaPlayer2Impl.this.mNextDSDs = new ArrayList(1);
                    MediaPlayer2Impl.this.mNextDSDs.add(dataSourceDesc);
                    MediaPlayer2Impl.this.mNextSrcId = MediaPlayer2Impl.access$708(MediaPlayer2Impl.this);
                    MediaPlayer2Impl.this.mNextSourceState = 0;
                    MediaPlayer2Impl.this.mNextSourcePlayPending = false;
                }
                if (MediaPlayer2Impl.this.getMediaPlayer2State() != 1) {
                    synchronized (MediaPlayer2Impl.this.mSrcLock) {
                        MediaPlayer2Impl.this.prepareNextDataSource_l();
                    }
                }
            }
        });
    }

    @Override
    public void setNextDataSources(final List<DataSourceDesc> list) {
        addTask(new Task(23, false) {
            @Override
            void process() {
                if (list == null || list.size() == 0) {
                    throw new IllegalArgumentException("data source list cannot be null or empty.");
                }
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    if (((DataSourceDesc) it.next()) == null) {
                        throw new IllegalArgumentException("DataSourceDesc in the source list cannot be null.");
                    }
                }
                synchronized (MediaPlayer2Impl.this.mSrcLock) {
                    MediaPlayer2Impl.this.mNextDSDs = new ArrayList(list);
                    MediaPlayer2Impl.this.mNextSrcId = MediaPlayer2Impl.access$708(MediaPlayer2Impl.this);
                    MediaPlayer2Impl.this.mNextSourceState = 0;
                    MediaPlayer2Impl.this.mNextSourcePlayPending = false;
                }
                if (MediaPlayer2Impl.this.getMediaPlayer2State() != 1) {
                    synchronized (MediaPlayer2Impl.this.mSrcLock) {
                        MediaPlayer2Impl.this.prepareNextDataSource_l();
                    }
                }
            }
        });
    }

    @Override
    public DataSourceDesc getCurrentDataSource() {
        DataSourceDesc dataSourceDesc;
        synchronized (this.mSrcLock) {
            dataSourceDesc = this.mCurrentDSD;
        }
        return dataSourceDesc;
    }

    @Override
    public void loopCurrent(final boolean z) {
        addTask(new Task(3, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.setLooping(z);
            }
        });
    }

    @Override
    public void setPlaybackSpeed(final float f) {
        addTask(new Task(25, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this._setPlaybackParams(MediaPlayer2Impl.this.getPlaybackParams().setSpeed(f));
            }
        });
    }

    @Override
    public float getPlaybackSpeed() {
        return getPlaybackParams().getSpeed();
    }

    @Override
    public boolean isReversePlaybackSupported() {
        return false;
    }

    @Override
    public void setPlayerVolume(final float f) {
        addTask(new Task(26, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mVolume = f;
                MediaPlayer2Impl.this._setVolume(f, f);
            }
        });
    }

    @Override
    public float getPlayerVolume() {
        return this.mVolume;
    }

    @Override
    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    @Override
    public void registerPlayerEventCallback(Executor executor, MediaPlayerBase.PlayerEventCallback playerEventCallback) {
    }

    @Override
    public void unregisterPlayerEventCallback(MediaPlayerBase.PlayerEventCallback playerEventCallback) {
    }

    @Override
    public Parcel newRequest() {
        return Parcel.obtain();
    }

    @Override
    public void invoke(Parcel parcel, Parcel parcel2) {
        int iNative_invoke = native_invoke(parcel, parcel2);
        parcel2.setDataPosition(0);
        if (iNative_invoke != 0) {
            throw new RuntimeException("failure code: " + iNative_invoke);
        }
    }

    class AnonymousClass12 extends Task {
        final Object val$label;

        AnonymousClass12(int i, boolean z, Object obj) {
            super(i, z);
            this.val$label = obj;
        }

        @Override
        void process() {
            synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                for (final Pair pair : MediaPlayer2Impl.this.mEventCallbackRecords) {
                    Executor executor = (Executor) pair.first;
                    final Object obj = this.val$label;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            MediaPlayer2Impl.AnonymousClass12 anonymousClass12 = this.f$0;
                            ((MediaPlayer2.MediaPlayer2EventCallback) pair.second).onCommandLabelReached(MediaPlayer2Impl.this, obj);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void notifyWhenCommandLabelReached(Object obj) {
        addTask(new AnonymousClass12(1003, false, obj));
    }

    @Override
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

    @Override
    public void setSurface(final Surface surface) {
        addTask(new Task(27, false) {
            @Override
            void process() {
                if (MediaPlayer2Impl.this.mScreenOnWhilePlaying && surface != null) {
                    Log.w(MediaPlayer2Impl.TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
                }
                MediaPlayer2Impl.this.mSurfaceHolder = null;
                MediaPlayer2Impl.this._setVideoSurface(surface);
                MediaPlayer2Impl.this.updateSurfaceScreenOn();
            }
        });
    }

    @Override
    public void setVideoScalingMode(final int i) {
        addTask(new Task(1002, false) {
            @Override
            void process() {
                if (!MediaPlayer2Impl.this.isVideoScalingModeSupported(i)) {
                    throw new IllegalArgumentException("Scaling mode " + i + " is not supported");
                }
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInt(6);
                    parcelObtain.writeInt(i);
                    MediaPlayer2Impl.this.invoke(parcelObtain, parcelObtain2);
                } finally {
                    parcelObtain.recycle();
                    parcelObtain2.recycle();
                }
            }
        });
    }

    @Override
    public void clearPendingCommands() {
    }

    private void addTask(Task task) {
        synchronized (this.mTaskLock) {
            this.mPendingTasks.add(task);
            processPendingTask_l();
        }
    }

    @GuardedBy("mTaskLock")
    private void processPendingTask_l() {
        if (this.mCurrentTask == null && !this.mPendingTasks.isEmpty()) {
            Task taskRemove = this.mPendingTasks.remove(0);
            this.mCurrentTask = taskRemove;
            this.mTaskHandler.post(taskRemove);
        }
    }

    private void handleDataSource(boolean z, DataSourceDesc dataSourceDesc, long j) throws IOException {
        Preconditions.checkNotNull(dataSourceDesc, "the DataSourceDesc cannot be null");
        switch (dataSourceDesc.getType()) {
            case 1:
                handleDataSource(z, j, dataSourceDesc.getMedia2DataSource());
                break;
            case 2:
                handleDataSource(z, j, dataSourceDesc.getFileDescriptor(), dataSourceDesc.getFileDescriptorOffset(), dataSourceDesc.getFileDescriptorLength());
                break;
            case 3:
                handleDataSource(z, j, dataSourceDesc.getUriContext(), dataSourceDesc.getUri(), dataSourceDesc.getUriHeaders(), dataSourceDesc.getUriCookies());
                break;
        }
    }

    private void handleDataSource(boolean z, long j, Context context, Uri uri, Map<String, String> map, List<HttpCookie> list) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        String scheme = uri.getScheme();
        String authorityWithoutUserId = ContentProvider.getAuthorityWithoutUserId(uri.getAuthority());
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            handleDataSource(z, j, uri.getPath(), (Map<String, String>) null, (List<HttpCookie>) null);
            return;
        }
        if ("content".equals(scheme) && "settings".equals(authorityWithoutUserId)) {
            int defaultType = RingtoneManager.getDefaultType(uri);
            Uri cacheForType = RingtoneManager.getCacheForType(defaultType, context.getUserId());
            Uri actualDefaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, defaultType);
            if (attemptDataSource(z, j, contentResolver, cacheForType) || attemptDataSource(z, j, contentResolver, actualDefaultRingtoneUri)) {
                return;
            }
            handleDataSource(z, j, uri.toString(), map, list);
            return;
        }
        if (attemptDataSource(z, j, contentResolver, uri)) {
            return;
        }
        handleDataSource(z, j, uri.toString(), map, list);
    }

    private boolean attemptDataSource(boolean z, long j, ContentResolver contentResolver, Uri uri) {
        try {
            AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
            Throwable th = null;
            try {
                if (assetFileDescriptorOpenAssetFileDescriptor.getDeclaredLength() < 0) {
                    handleDataSource(z, j, assetFileDescriptorOpenAssetFileDescriptor.getFileDescriptor(), 0L, DataSourceDesc.LONG_MAX);
                } else {
                    handleDataSource(z, j, assetFileDescriptorOpenAssetFileDescriptor.getFileDescriptor(), assetFileDescriptorOpenAssetFileDescriptor.getStartOffset(), assetFileDescriptorOpenAssetFileDescriptor.getDeclaredLength());
                }
                if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                }
                return true;
            } catch (Throwable th2) {
                if (assetFileDescriptorOpenAssetFileDescriptor == null) {
                    throw th2;
                }
                if (0 == 0) {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                    throw th2;
                }
                try {
                    assetFileDescriptorOpenAssetFileDescriptor.close();
                    throw th2;
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                    throw th2;
                }
            }
        } catch (IOException | NullPointerException | SecurityException e) {
            Log.w(TAG, "Couldn't open " + uri + ": " + e);
            return false;
        }
    }

    private void handleDataSource(boolean z, long j, String str, Map<String, String> map, List<HttpCookie> list) throws IOException {
        String[] strArr;
        String[] strArr2;
        if (map == null) {
            strArr = null;
            strArr2 = null;
        } else {
            String[] strArr3 = new String[map.size()];
            String[] strArr4 = new String[map.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                strArr3[i] = entry.getKey();
                strArr4[i] = entry.getValue();
                i++;
            }
            strArr = strArr3;
            strArr2 = strArr4;
        }
        handleDataSource(z, j, str, strArr, strArr2, list);
    }

    private void handleDataSource(boolean z, long j, String str, String[] strArr, String[] strArr2, List<HttpCookie> list) throws IOException {
        String path;
        Uri uri = Uri.parse(str);
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            path = uri.getPath();
        } else {
            if (scheme != null) {
                nativeHandleDataSourceUrl(z, j, Media2HTTPService.createHTTPService(str, list), str, strArr, strArr2);
                return;
            }
            path = str;
        }
        File file = new File(path);
        if (file.exists()) {
            FileInputStream fileInputStream = new FileInputStream(file);
            handleDataSource(z, j, fileInputStream.getFD(), 0L, DataSourceDesc.LONG_MAX);
            fileInputStream.close();
            return;
        }
        throw new IOException("handleDataSource failed.");
    }

    private void handleDataSource(boolean z, long j, FileDescriptor fileDescriptor, long j2, long j3) throws IOException {
        nativeHandleDataSourceFD(z, j, fileDescriptor, j2, j3);
    }

    private void handleDataSource(boolean z, long j, Media2DataSource media2DataSource) {
        nativeHandleDataSourceCallback(z, j, media2DataSource);
    }

    private void prepareNextDataSource_l() {
        if (this.mNextDSDs == null || this.mNextDSDs.isEmpty() || this.mNextSourceState != 0) {
            return;
        }
        try {
            this.mNextSourceState = 1;
            handleDataSource(false, this.mNextDSDs.get(0), this.mNextSrcId);
        } catch (Exception e) {
            final Message messageObtainMessage = this.mEventHandler.obtainMessage(100, 1, -1010, null);
            final long j = this.mNextSrcId;
            this.mEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer2Impl.this.mEventHandler.handleMessage(messageObtainMessage, j);
                }
            });
        }
    }

    private void playNextDataSource_l() {
        if (this.mNextDSDs == null || this.mNextDSDs.isEmpty()) {
            return;
        }
        if (this.mNextSourceState == 2) {
            this.mCurrentDSD = this.mNextDSDs.get(0);
            this.mCurrentSrcId = this.mNextSrcId;
            this.mBufferedPercentageCurrent.set(this.mBufferedPercentageNext.get());
            this.mNextDSDs.remove(0);
            long j = this.mSrcIdGenerator;
            this.mSrcIdGenerator = 1 + j;
            this.mNextSrcId = j;
            this.mBufferedPercentageNext.set(0);
            this.mNextSourceState = 0;
            this.mNextSourcePlayPending = false;
            final long j2 = this.mCurrentSrcId;
            try {
                nativePlayNextDataSource(j2);
                return;
            } catch (Exception e) {
                final Message messageObtainMessage = this.mEventHandler.obtainMessage(100, 1, -1010, null);
                this.mEventHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MediaPlayer2Impl.this.mEventHandler.handleMessage(messageObtainMessage, j2);
                    }
                });
                return;
            }
        }
        if (this.mNextSourceState == 0) {
            prepareNextDataSource_l();
        }
        this.mNextSourcePlayPending = true;
    }

    private int getAudioStreamType() {
        if (this.mStreamType == Integer.MIN_VALUE) {
            this.mStreamType = _getAudioStreamType();
        }
        return this.mStreamType;
    }

    @Override
    public void stop() {
        stayAwake(false);
        _stop();
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

    @Override
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
        this.mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(i | 536870912, MediaPlayer2Impl.class.getName());
        this.mWakeLock.setReferenceCounted(false);
        if (z) {
            this.mWakeLock.acquire();
        }
    }

    @Override
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

    @Override
    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    @Override
    public int getMediaPlayer2State() {
        return native_getMediaPlayer2State();
    }

    @Override
    public void setBufferingParams(final BufferingParams bufferingParams) {
        addTask(new Task(1001, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(bufferingParams, "the BufferingParams cannot be null");
                MediaPlayer2Impl.this._setBufferingParams(bufferingParams);
            }
        });
    }

    @Override
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

    @Override
    public void setPlaybackParams(final PlaybackParams playbackParams) {
        addTask(new Task(24, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(playbackParams, "the PlaybackParams cannot be null");
                MediaPlayer2Impl.this._setPlaybackParams(playbackParams);
            }
        });
    }

    @Override
    public void setSyncParams(final SyncParams syncParams) {
        addTask(new Task(28, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(syncParams, "the SyncParams cannot be null");
                MediaPlayer2Impl.this._setSyncParams(syncParams);
            }
        });
    }

    @Override
    public void seekTo(final long j, final int i) {
        addTask(new Task(14, true) {
            @Override
            void process() {
                if (i < 0 || i > 3) {
                    throw new IllegalArgumentException("Illegal seek mode: " + i);
                }
                long j2 = j;
                if (j2 > 2147483647L) {
                    Log.w(MediaPlayer2Impl.TAG, "seekTo offset " + j2 + " is too large, cap to 2147483647");
                    j2 = 2147483647L;
                } else if (j2 < -2147483648L) {
                    Log.w(MediaPlayer2Impl.TAG, "seekTo offset " + j2 + " is too small, cap to -2147483648");
                    j2 = -2147483648L;
                }
                MediaPlayer2Impl.this._seekTo(j2, i);
            }
        });
    }

    @Override
    public MediaTimestamp getTimestamp() {
        try {
            return new MediaTimestamp(getCurrentPosition() * 1000, System.nanoTime(), isPlaying() ? getPlaybackParams().getSpeed() : 0.0f);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
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

    @Override
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

    @Override
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
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.clear();
        }
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.clear();
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

    @Override
    public void notifyAt(long j) {
        _notifyAt(j);
    }

    @Override
    public void setAudioSessionId(final int i) {
        addTask(new Task(17, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this._setAudioSessionId(i);
            }
        });
    }

    @Override
    public void attachAuxEffect(final int i) {
        addTask(new Task(1, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this._attachAuxEffect(i);
            }
        });
    }

    @Override
    public void setAuxEffectSendLevel(final float f) {
        addTask(new Task(18, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this._setAuxEffectSendLevel(f);
            }
        });
    }

    public static final class TrackInfoImpl extends MediaPlayer2.TrackInfo {
        static final Parcelable.Creator<TrackInfoImpl> CREATOR = new Parcelable.Creator<TrackInfoImpl>() {
            @Override
            public TrackInfoImpl createFromParcel(Parcel parcel) {
                return new TrackInfoImpl(parcel);
            }

            @Override
            public TrackInfoImpl[] newArray(int i) {
                return new TrackInfoImpl[i];
            }
        };
        final MediaFormat mFormat;
        final int mTrackType;

        @Override
        public int getTrackType() {
            return this.mTrackType;
        }

        @Override
        public String getLanguage() {
            String string = this.mFormat.getString("language");
            return string == null ? "und" : string;
        }

        @Override
        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfoImpl(Parcel parcel) {
            this.mTrackType = parcel.readInt();
            this.mFormat = MediaFormat.createSubtitleFormat(parcel.readString(), parcel.readString());
            if (this.mTrackType == 4) {
                this.mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, parcel.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, parcel.readInt());
                this.mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, parcel.readInt());
            }
        }

        TrackInfoImpl(int i, MediaFormat mediaFormat) {
            this.mTrackType = i;
            this.mFormat = mediaFormat;
        }

        void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mTrackType);
            parcel.writeString(getLanguage());
            if (this.mTrackType == 4) {
                parcel.writeString(this.mFormat.getString(MediaFormat.KEY_MIME));
                parcel.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
                parcel.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
                parcel.writeInt(this.mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
            }
        }

        @Override
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

    @Override
    public List<MediaPlayer2.TrackInfo> getTrackInfo() {
        List<MediaPlayer2.TrackInfo> listAsList;
        TrackInfoImpl[] inbandTrackInfoImpl = getInbandTrackInfoImpl();
        synchronized (this.mIndexTrackPairs) {
            TrackInfoImpl[] trackInfoImplArr = new TrackInfoImpl[this.mIndexTrackPairs.size()];
            for (int i = 0; i < trackInfoImplArr.length; i++) {
                Pair<Integer, SubtitleTrack> pair = this.mIndexTrackPairs.get(i);
                if (pair.first != null) {
                    trackInfoImplArr[i] = inbandTrackInfoImpl[pair.first.intValue()];
                } else {
                    SubtitleTrack subtitleTrack = pair.second;
                    trackInfoImplArr[i] = new TrackInfoImpl(subtitleTrack.getTrackType(), subtitleTrack.getFormat());
                }
            }
            listAsList = Arrays.asList(trackInfoImplArr);
        }
        return listAsList;
    }

    private TrackInfoImpl[] getInbandTrackInfoImpl() throws IllegalStateException {
        Parcel parcelObtain = Parcel.obtain();
        Parcel parcelObtain2 = Parcel.obtain();
        try {
            parcelObtain.writeInt(1);
            invoke(parcelObtain, parcelObtain2);
            return (TrackInfoImpl[]) parcelObtain2.createTypedArray(TrackInfoImpl.CREATOR);
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

    @Override
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
                    MediaPlayer2Impl.this.mSubtitleController = new SubtitleController(applicationCurrentApplication, MediaPlayer2Impl.this.mTimeProvider, MediaPlayer2Impl.this);
                    MediaPlayer2Impl.this.mSubtitleController.setAnchor(new SubtitleController.Anchor() {
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
        setOnSubtitleDataListener(null);
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
            setOnSubtitleDataListener(this.mSubtitleDataListener);
        }
    }

    @Override
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
                if (inputStream == null || MediaPlayer2Impl.this.mSubtitleController == null || (subtitleTrackAddTrack = MediaPlayer2Impl.this.mSubtitleController.addTrack(mediaFormat)) == null) {
                    return 901;
                }
                Scanner scanner = new Scanner(inputStream, "UTF-8");
                String next = scanner.useDelimiter("\\A").next();
                synchronized (MediaPlayer2Impl.this.mOpenSubtitleSources) {
                    MediaPlayer2Impl.this.mOpenSubtitleSources.remove(inputStream);
                }
                scanner.close();
                synchronized (MediaPlayer2Impl.this.mIndexTrackPairs) {
                    MediaPlayer2Impl.this.mIndexTrackPairs.add(Pair.create(null, subtitleTrackAddTrack));
                }
                TimeProvider.EventHandler eventHandler = MediaPlayer2Impl.this.mTimeProvider.mEventHandler;
                eventHandler.sendMessage(eventHandler.obtainMessage(1, 4, 0, Pair.create(subtitleTrackAddTrack, next.getBytes())));
                return 803;
            }

            @Override
            public void run() {
                int iAddTrack = addTrack();
                if (MediaPlayer2Impl.this.mEventHandler != null) {
                    MediaPlayer2Impl.this.mEventHandler.sendMessage(MediaPlayer2Impl.this.mEventHandler.obtainMessage(200, iAddTrack, 0, null));
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
        TrackInfoImpl[] inbandTrackInfoImpl = getInbandTrackInfoImpl();
        synchronized (this.mIndexTrackPairs) {
            for (int i = 0; i < inbandTrackInfoImpl.length; i++) {
                if (!this.mInbandTrackIndices.get(i)) {
                    this.mInbandTrackIndices.set(i);
                    if (inbandTrackInfoImpl[i].getTrackType() == 4) {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), this.mSubtitleController.addTrack(inbandTrackInfoImpl[i].getFormat())));
                    } else {
                        this.mIndexTrackPairs.add(Pair.create(Integer.valueOf(i), null));
                    }
                }
            }
        }
    }

    @Override
    public void addTimedTextSource(String str, String str2) throws IOException {
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

    @Override
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

    @Override
    public void addTimedTextSource(FileDescriptor fileDescriptor, String str) {
        addTimedTextSource(fileDescriptor, 0L, DataSourceDesc.LONG_MAX, str);
    }

    @Override
    public void addTimedTextSource(FileDescriptor fileDescriptor, final long j, final long j2, String str) {
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
                            TimeProvider.EventHandler eventHandler = MediaPlayer2Impl.this.mTimeProvider.mEventHandler;
                            eventHandler.sendMessage(eventHandler.obtainMessage(1, 4, 0, Pair.create(subtitleTrackAddTrack, byteArrayOutputStream.toByteArray())));
                            try {
                                Os.close(fileDescriptorDup);
                            } catch (ErrnoException e) {
                                Log.e(MediaPlayer2Impl.TAG, e.getMessage(), e);
                            }
                            return 803;
                        } catch (Throwable th) {
                            try {
                                Os.close(fileDescriptorDup);
                            } catch (ErrnoException e2) {
                                Log.e(MediaPlayer2Impl.TAG, e2.getMessage(), e2);
                            }
                            throw th;
                        }
                    } catch (Exception e3) {
                        Log.e(MediaPlayer2Impl.TAG, e3.getMessage(), e3);
                        try {
                            Os.close(fileDescriptorDup);
                        } catch (ErrnoException e4) {
                            Log.e(MediaPlayer2Impl.TAG, e4.getMessage(), e4);
                        }
                        return 900;
                    }
                }

                @Override
                public void run() {
                    int iAddTrack = addTrack();
                    if (MediaPlayer2Impl.this.mEventHandler != null) {
                        MediaPlayer2Impl.this.mEventHandler.sendMessage(MediaPlayer2Impl.this.mEventHandler.obtainMessage(200, iAddTrack, 0, null));
                    }
                    handlerThread.getLooper().quitSafely();
                }
            });
        } catch (ErrnoException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getSelectedTrack(int i) {
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

    @Override
    public void selectTrack(final int i) {
        addTask(new Task(15, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.selectOrDeselectTrack(i, true);
            }
        });
    }

    @Override
    public void deselectTrack(final int i) {
        addTask(new Task(2, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.selectOrDeselectTrack(i, false);
            }
        });
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
            parcelObtain.writeInt(z ? 4 : 5);
            parcelObtain.writeInt(i);
            invoke(parcelObtain, parcelObtain2);
        } finally {
            parcelObtain.recycle();
            parcelObtain2.recycle();
        }
    }

    protected void finalize() throws Throwable {
        if (this.mGuard != null) {
            this.mGuard.warnIfOpen();
        }
        close();
        native_finalize();
    }

    private void release() {
        stayAwake(false);
        updateSurfaceScreenOn();
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.clear();
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
        if (this.mTimeProvider != null) {
            this.mTimeProvider.close();
            this.mTimeProvider = null;
        }
        this.mOnSubtitleDataListener = null;
        this.mOnDrmConfigHelper = null;
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.clear();
        }
        resetDrmState();
        _release();
    }

    @Override
    public MediaTimeProvider getMediaTimeProvider() {
        if (this.mTimeProvider == null) {
            this.mTimeProvider = new TimeProvider(this);
        }
        return this.mTimeProvider;
    }

    private class EventHandler extends Handler {
        private MediaPlayer2Impl mMediaPlayer;

        public EventHandler(MediaPlayer2Impl mediaPlayer2Impl, Looper looper) {
            super(looper);
            this.mMediaPlayer = mediaPlayer2Impl;
        }

        @Override
        public void handleMessage(Message message) {
            handleMessage(message, 0L);
        }

        public void handleMessage(Message message, long j) {
            final DrmInfoImpl drmInfoImplMakeCopy;
            final DataSourceDesc dataSourceDesc;
            if (this.mMediaPlayer.mNativeContext == 0) {
                Log.w(MediaPlayer2Impl.TAG, "mediaplayer2 went away with unhandled events");
                return;
            }
            final int i = message.arg1;
            final int i2 = message.arg2;
            int i3 = message.what;
            final TimedMetaData timedMetaDataCreateTimedMetaDataFromParcel = null;
            final TimedText timedText = null;
            if (i3 == 210) {
                if (message.obj == null) {
                    Log.w(MediaPlayer2Impl.TAG, "MEDIA_DRM_INFO msg.obj=NULL");
                    return;
                }
                if (message.obj instanceof Parcel) {
                    synchronized (MediaPlayer2Impl.this.mDrmLock) {
                        drmInfoImplMakeCopy = MediaPlayer2Impl.this.mDrmInfoImpl != null ? MediaPlayer2Impl.this.mDrmInfoImpl.makeCopy() : null;
                    }
                    if (drmInfoImplMakeCopy != null) {
                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                            for (final Pair pair : MediaPlayer2Impl.this.mDrmEventCallbackRecords) {
                                ((Executor) pair.first).execute(new Runnable() {
                                    @Override
                                    public final void run() {
                                        MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                        ((MediaPlayer2.DrmEventCallback) pair.second).onDrmInfo(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, drmInfoImplMakeCopy);
                                    }
                                });
                            }
                        }
                        return;
                    }
                    return;
                }
                Log.w(MediaPlayer2Impl.TAG, "MEDIA_DRM_INFO msg.obj of unexpected type " + message.obj);
                return;
            }
            if (i3 == 10000) {
                AudioManager.resetAudioPortGeneration();
                synchronized (MediaPlayer2Impl.this.mRoutingChangeListeners) {
                    Iterator it = MediaPlayer2Impl.this.mRoutingChangeListeners.values().iterator();
                    while (it.hasNext()) {
                        ((NativeRoutingEventHandlerDelegate) it.next()).notifyClient();
                    }
                }
                return;
            }
            switch (i3) {
                case 0:
                    return;
                case 1:
                    try {
                        MediaPlayer2Impl.this.scanInternalSubtitleTracks();
                        break;
                    } catch (RuntimeException e) {
                        sendMessage(obtainMessage(100, 1, -1010, null));
                    }
                    synchronized (MediaPlayer2Impl.this.mSrcLock) {
                        Log.i(MediaPlayer2Impl.TAG, "MEDIA_PREPARED: srcId=" + j + ", currentSrcId=" + MediaPlayer2Impl.this.mCurrentSrcId + ", nextSrcId=" + MediaPlayer2Impl.this.mNextSrcId);
                        if (j == MediaPlayer2Impl.this.mCurrentSrcId) {
                            dataSourceDesc = MediaPlayer2Impl.this.mCurrentDSD;
                            MediaPlayer2Impl.this.prepareNextDataSource_l();
                        } else if (MediaPlayer2Impl.this.mNextDSDs != null && !MediaPlayer2Impl.this.mNextDSDs.isEmpty() && j == MediaPlayer2Impl.this.mNextSrcId) {
                            dataSourceDesc = (DataSourceDesc) MediaPlayer2Impl.this.mNextDSDs.get(0);
                            MediaPlayer2Impl.this.mNextSourceState = 2;
                            if (MediaPlayer2Impl.this.mNextSourcePlayPending) {
                                MediaPlayer2Impl.this.playNextDataSource_l();
                            }
                        } else {
                            dataSourceDesc = null;
                        }
                        break;
                    }
                    if (dataSourceDesc != null) {
                        synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                            for (final Pair pair2 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                ((Executor) pair2.first).execute(new Runnable() {
                                    @Override
                                    public final void run() {
                                        MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                        ((MediaPlayer2.MediaPlayer2EventCallback) pair2.second).onInfo(eventHandler.mMediaPlayer, dataSourceDesc, 100, 0);
                                    }
                                });
                            }
                            break;
                        }
                    }
                    synchronized (MediaPlayer2Impl.this.mTaskLock) {
                        if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 6 && MediaPlayer2Impl.this.mCurrentTask.mDSD == dataSourceDesc && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                            MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                            MediaPlayer2Impl.this.mCurrentTask = null;
                            MediaPlayer2Impl.this.processPendingTask_l();
                        }
                        break;
                    }
                    return;
                case 2:
                    final DataSourceDesc dataSourceDesc2 = MediaPlayer2Impl.this.mCurrentDSD;
                    synchronized (MediaPlayer2Impl.this.mSrcLock) {
                        if (j == MediaPlayer2Impl.this.mCurrentSrcId) {
                            Log.i(MediaPlayer2Impl.TAG, "MEDIA_PLAYBACK_COMPLETE: srcId=" + j + ", currentSrcId=" + MediaPlayer2Impl.this.mCurrentSrcId + ", nextSrcId=" + MediaPlayer2Impl.this.mNextSrcId);
                            MediaPlayer2Impl.this.playNextDataSource_l();
                        }
                        break;
                    }
                    synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                        for (final Pair pair3 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                            ((Executor) pair3.first).execute(new Runnable() {
                                @Override
                                public final void run() {
                                    MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                    ((MediaPlayer2.MediaPlayer2EventCallback) pair3.second).onInfo(eventHandler.mMediaPlayer, dataSourceDesc2, 5, 0);
                                }
                            });
                        }
                        break;
                    }
                    MediaPlayer2Impl.this.stayAwake(false);
                    return;
                case 3:
                    final int i4 = message.arg1;
                    synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                        if (j == MediaPlayer2Impl.this.mCurrentSrcId) {
                            MediaPlayer2Impl.this.mBufferedPercentageCurrent.set(i4);
                            for (final Pair pair4 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                ((Executor) pair4.first).execute(new Runnable() {
                                    @Override
                                    public final void run() {
                                        MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                        ((MediaPlayer2.MediaPlayer2EventCallback) pair4.second).onInfo(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, 704, i4);
                                    }
                                });
                            }
                        } else if (j == MediaPlayer2Impl.this.mNextSrcId && !MediaPlayer2Impl.this.mNextDSDs.isEmpty()) {
                            MediaPlayer2Impl.this.mBufferedPercentageNext.set(i4);
                            final DataSourceDesc dataSourceDesc3 = (DataSourceDesc) MediaPlayer2Impl.this.mNextDSDs.get(0);
                            for (final Pair pair5 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                ((Executor) pair5.first).execute(new Runnable() {
                                    @Override
                                    public final void run() {
                                        MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                        ((MediaPlayer2.MediaPlayer2EventCallback) pair5.second).onInfo(eventHandler.mMediaPlayer, dataSourceDesc3, 704, i4);
                                    }
                                });
                            }
                        }
                        break;
                    }
                    return;
                case 4:
                    synchronized (MediaPlayer2Impl.this.mTaskLock) {
                        if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 14 && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                            MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                            MediaPlayer2Impl.this.mCurrentTask = null;
                            MediaPlayer2Impl.this.processPendingTask_l();
                        }
                        break;
                    }
                    break;
                case 5:
                    final int i5 = message.arg1;
                    final int i6 = message.arg2;
                    synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                        for (final Pair pair6 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                            ((Executor) pair6.first).execute(new Runnable() {
                                @Override
                                public final void run() {
                                    MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                    ((MediaPlayer2.MediaPlayer2EventCallback) pair6.second).onVideoSizeChanged(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, i5, i6);
                                }
                            });
                        }
                        break;
                    }
                    return;
                case 6:
                case 7:
                    TimeProvider timeProvider = MediaPlayer2Impl.this.mTimeProvider;
                    if (timeProvider != null) {
                        timeProvider.onPaused(message.what == 7);
                        return;
                    }
                    return;
                case 8:
                    TimeProvider timeProvider2 = MediaPlayer2Impl.this.mTimeProvider;
                    if (timeProvider2 != null) {
                        timeProvider2.onStopped();
                        return;
                    }
                    return;
                case 9:
                    break;
                default:
                    switch (i3) {
                        case 98:
                            TimeProvider timeProvider3 = MediaPlayer2Impl.this.mTimeProvider;
                            if (timeProvider3 != null) {
                                timeProvider3.onNotifyTime();
                                return;
                            }
                            return;
                        case 99:
                            if (message.obj instanceof Parcel) {
                                Parcel parcel = (Parcel) message.obj;
                                timedText = new TimedText(parcel);
                                parcel.recycle();
                            }
                            synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                for (final Pair pair7 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                    ((Executor) pair7.first).execute(new Runnable() {
                                        @Override
                                        public final void run() {
                                            MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                            ((MediaPlayer2.MediaPlayer2EventCallback) pair7.second).onTimedText(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, timedText);
                                        }
                                    });
                                }
                                break;
                            }
                            return;
                        case 100:
                            Log.e(MediaPlayer2Impl.TAG, "Error (" + message.arg1 + "," + message.arg2 + ")");
                            synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                for (final Pair pair8 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                    ((Executor) pair8.first).execute(new Runnable() {
                                        @Override
                                        public final void run() {
                                            MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                            ((MediaPlayer2.MediaPlayer2EventCallback) pair8.second).onError(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, i, i2);
                                        }
                                    });
                                    ((Executor) pair8.first).execute(new Runnable() {
                                        @Override
                                        public final void run() {
                                            MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                            ((MediaPlayer2.MediaPlayer2EventCallback) pair8.second).onInfo(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, 5, 0);
                                        }
                                    });
                                }
                                break;
                            }
                            MediaPlayer2Impl.this.stayAwake(false);
                            return;
                        default:
                            switch (i3) {
                                case 200:
                                    int i7 = message.arg1;
                                    if (i7 != 2) {
                                        switch (i7) {
                                            case 700:
                                                Log.i(MediaPlayer2Impl.TAG, "Info (" + message.arg1 + "," + message.arg2 + ")");
                                                break;
                                            case 701:
                                            case 702:
                                                TimeProvider timeProvider4 = MediaPlayer2Impl.this.mTimeProvider;
                                                if (timeProvider4 != null) {
                                                    timeProvider4.onBuffering(message.arg1 == 701);
                                                }
                                                break;
                                            default:
                                                switch (i7) {
                                                    case 802:
                                                        try {
                                                            MediaPlayer2Impl.this.scanInternalSubtitleTracks();
                                                            break;
                                                        } catch (RuntimeException e2) {
                                                            sendMessage(obtainMessage(100, 1, -1010, null));
                                                            break;
                                                        }
                                                    case 803:
                                                        message.arg1 = 802;
                                                        if (MediaPlayer2Impl.this.mSubtitleController != null) {
                                                            MediaPlayer2Impl.this.mSubtitleController.selectDefaultTrack();
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                    } else if (j == MediaPlayer2Impl.this.mCurrentSrcId) {
                                        MediaPlayer2Impl.this.prepareNextDataSource_l();
                                    }
                                    synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                        for (final Pair pair9 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                            ((Executor) pair9.first).execute(new Runnable() {
                                                @Override
                                                public final void run() {
                                                    MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                                    ((MediaPlayer2.MediaPlayer2EventCallback) pair9.second).onInfo(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, i, i2);
                                                }
                                            });
                                        }
                                        break;
                                    }
                                    return;
                                case 201:
                                    MediaPlayer2.OnSubtitleDataListener onSubtitleDataListener = MediaPlayer2Impl.this.mOnSubtitleDataListener;
                                    if (onSubtitleDataListener != null && (message.obj instanceof Parcel)) {
                                        Parcel parcel2 = (Parcel) message.obj;
                                        SubtitleData subtitleData = new SubtitleData(parcel2);
                                        parcel2.recycle();
                                        onSubtitleDataListener.onSubtitleData(this.mMediaPlayer, subtitleData);
                                        return;
                                    }
                                    return;
                                case 202:
                                    if (message.obj instanceof Parcel) {
                                        Parcel parcel3 = (Parcel) message.obj;
                                        timedMetaDataCreateTimedMetaDataFromParcel = TimedMetaData.createTimedMetaDataFromParcel(parcel3);
                                        parcel3.recycle();
                                    }
                                    synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                                        for (final Pair pair10 : MediaPlayer2Impl.this.mEventCallbackRecords) {
                                            ((Executor) pair10.first).execute(new Runnable() {
                                                @Override
                                                public final void run() {
                                                    MediaPlayer2Impl.EventHandler eventHandler = this.f$0;
                                                    ((MediaPlayer2.MediaPlayer2EventCallback) pair10.second).onTimedMetaDataAvailable(eventHandler.mMediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, timedMetaDataCreateTimedMetaDataFromParcel);
                                                }
                                            });
                                        }
                                        break;
                                    }
                                    return;
                                default:
                                    Log.e(MediaPlayer2Impl.TAG, "Unknown message type " + message.what);
                                    return;
                            }
                    }
            }
            TimeProvider timeProvider5 = MediaPlayer2Impl.this.mTimeProvider;
            if (timeProvider5 != null) {
                timeProvider5.onSeekComplete(this.mMediaPlayer);
            }
        }
    }

    private static void postEventFromNative(Object obj, final long j, int i, int i2, int i3, Object obj2) {
        MediaPlayer2Impl mediaPlayer2Impl = (MediaPlayer2Impl) ((WeakReference) obj).get();
        if (mediaPlayer2Impl == null) {
            return;
        }
        if (i == 1) {
            synchronized (mediaPlayer2Impl.mDrmLock) {
                mediaPlayer2Impl.mDrmInfoResolved = true;
            }
        } else if (i != 200) {
            if (i == 210) {
                Log.v(TAG, "postEventFromNative MEDIA_DRM_INFO");
                if (obj2 instanceof Parcel) {
                    DrmInfoImpl drmInfoImpl = new DrmInfoImpl((Parcel) obj2);
                    synchronized (mediaPlayer2Impl.mDrmLock) {
                        mediaPlayer2Impl.mDrmInfoImpl = drmInfoImpl;
                    }
                } else {
                    Log.w(TAG, "MEDIA_DRM_INFO msg.obj of unexpected type " + obj2);
                }
            }
        } else if (i2 == 2) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer2Impl.this.play();
                }
            }).start();
            Thread.yield();
        }
        if (mediaPlayer2Impl.mEventHandler != null) {
            final Message messageObtainMessage = mediaPlayer2Impl.mEventHandler.obtainMessage(i, i2, i3, obj2);
            mediaPlayer2Impl.mEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    MediaPlayer2Impl.this.mEventHandler.handleMessage(messageObtainMessage, j);
                }
            });
        }
    }

    @Override
    public void setMediaPlayer2EventCallback(Executor executor, MediaPlayer2.MediaPlayer2EventCallback mediaPlayer2EventCallback) {
        if (mediaPlayer2EventCallback == null) {
            throw new IllegalArgumentException("Illegal null MediaPlayer2EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Illegal null Executor for the MediaPlayer2EventCallback");
        }
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.add(new Pair<>(executor, mediaPlayer2EventCallback));
        }
    }

    @Override
    public void clearMediaPlayer2EventCallback() {
        synchronized (this.mEventCbLock) {
            this.mEventCallbackRecords.clear();
        }
    }

    @Override
    public void setOnSubtitleDataListener(MediaPlayer2.OnSubtitleDataListener onSubtitleDataListener) {
        this.mOnSubtitleDataListener = onSubtitleDataListener;
    }

    @Override
    public void setOnDrmConfigHelper(MediaPlayer2.OnDrmConfigHelper onDrmConfigHelper) {
        synchronized (this.mDrmLock) {
            this.mOnDrmConfigHelper = onDrmConfigHelper;
        }
    }

    @Override
    public void setDrmEventCallback(Executor executor, MediaPlayer2.DrmEventCallback drmEventCallback) {
        if (drmEventCallback == null) {
            throw new IllegalArgumentException("Illegal null MediaPlayer2EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Illegal null Executor for the MediaPlayer2EventCallback");
        }
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.add(new Pair<>(executor, drmEventCallback));
        }
    }

    @Override
    public void clearDrmEventCallback() {
        synchronized (this.mDrmEventCbLock) {
            this.mDrmEventCallbackRecords.clear();
        }
    }

    @Override
    public MediaPlayer2.DrmInfo getDrmInfo() {
        DrmInfoImpl drmInfoImplMakeCopy;
        synchronized (this.mDrmLock) {
            if (!this.mDrmInfoResolved && this.mDrmInfoImpl == null) {
                Log.v(TAG, "The Player has not been prepared yet");
                throw new IllegalStateException("The Player has not been prepared yet");
            }
            if (this.mDrmInfoImpl == null) {
                drmInfoImplMakeCopy = null;
            } else {
                drmInfoImplMakeCopy = this.mDrmInfoImpl.makeCopy();
            }
        }
        return drmInfoImplMakeCopy;
    }

    @Override
    public void prepareDrm(UUID uuid) throws UnsupportedSchemeException, MediaPlayer2.ProvisioningNetworkErrorException, ResourceBusyException, MediaPlayer2.ProvisioningServerErrorException {
        boolean z;
        Log.v(TAG, "prepareDrm: uuid: " + uuid + " mOnDrmConfigHelper: " + this.mOnDrmConfigHelper);
        synchronized (this.mDrmLock) {
            if (this.mDrmInfoImpl == null) {
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
            this.mOnDrmConfigHelper.onDrmConfig(this, this.mCurrentDSD);
        }
        synchronized (this.mDrmLock) {
            try {
                this.mDrmConfigAllowed = false;
                try {
                    try {
                        try {
                            try {
                                prepareDrm_openSessionStep(uuid);
                                this.mDrmUUID = uuid;
                                this.mActiveDrmScheme = true;
                                if (!this.mDrmProvisioningInProgress) {
                                    this.mPrepareDrmInProgress = false;
                                }
                            } catch (IllegalStateException e2) {
                                Log.e(TAG, "prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
                                throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be in the prepared state to call prepareDrm().");
                            }
                        } catch (Exception e3) {
                            Log.e(TAG, "prepareDrm: Exception " + e3);
                            throw e3;
                        }
                    } catch (Throwable th) {
                        th = th;
                        z = false;
                        if (!this.mDrmProvisioningInProgress) {
                            this.mPrepareDrmInProgress = false;
                        }
                        if (z) {
                            cleanDrmObj();
                        }
                        throw th;
                    }
                } catch (NotProvisionedException e4) {
                    Log.w(TAG, "prepareDrm: NotProvisionedException");
                    int iHandleProvisioninig = HandleProvisioninig(uuid);
                    if (iHandleProvisioninig != 0) {
                        switch (iHandleProvisioninig) {
                            case 1:
                                Log.e(TAG, "prepareDrm: Provisioning was required but failed due to a network error.");
                                throw new ProvisioningNetworkErrorExceptionImpl("prepareDrm: Provisioning was required but failed due to a network error.");
                            case 2:
                                Log.e(TAG, "prepareDrm: Provisioning was required but the request was denied by the server.");
                                throw new ProvisioningServerErrorExceptionImpl("prepareDrm: Provisioning was required but the request was denied by the server.");
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
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (z) {
            synchronized (this.mDrmEventCbLock) {
                for (final Pair<Executor, MediaPlayer2.DrmEventCallback> pair : this.mDrmEventCallbackRecords) {
                    pair.first.execute(new Runnable() {
                        @Override
                        public final void run() {
                            MediaPlayer2Impl mediaPlayer2Impl = this.f$0;
                            ((MediaPlayer2.DrmEventCallback) pair.second).onDrmPrepared(mediaPlayer2Impl, mediaPlayer2Impl.mCurrentDSD, 0);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void releaseDrm() throws MediaPlayer2.NoDrmSchemeException {
        addTask(new Task(12, false) {
            @Override
            void process() throws MediaPlayer2.NoDrmSchemeException {
                synchronized (MediaPlayer2Impl.this.mDrmLock) {
                    Log.v(MediaPlayer2Impl.TAG, "releaseDrm:");
                    if (MediaPlayer2Impl.this.mActiveDrmScheme) {
                        try {
                            MediaPlayer2Impl.this._releaseDrm();
                            MediaPlayer2Impl.this.cleanDrmObj();
                            MediaPlayer2Impl.this.mActiveDrmScheme = false;
                        } catch (IllegalStateException e) {
                            Log.w(MediaPlayer2Impl.TAG, "releaseDrm: Exception ", e);
                            throw new IllegalStateException("releaseDrm: The player is not in a valid state.");
                        } catch (Exception e2) {
                            Log.e(MediaPlayer2Impl.TAG, "releaseDrm: Exception ", e2);
                        }
                    } else {
                        Log.e(MediaPlayer2Impl.TAG, "releaseDrm(): No active DRM scheme to release.");
                        throw new NoDrmSchemeExceptionImpl("releaseDrm: No active DRM scheme to release.");
                    }
                }
            }
        });
    }

    @Override
    public MediaDrm.KeyRequest getDrmKeyRequest(byte[] bArr, byte[] bArr2, String str, int i, Map<String, String> map) throws MediaPlayer2.NoDrmSchemeException {
        HashMap<String, String> map2;
        MediaDrm.KeyRequest keyRequest;
        Log.v(TAG, "getDrmKeyRequest:  keySetId: " + bArr + " initData:" + bArr2 + " mimeType: " + str + " keyType: " + i + " optionalParameters: " + map);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                Log.e(TAG, "getDrmKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeExceptionImpl("getDrmKeyRequest: Has to set a DRM scheme first.");
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
                    Log.v(TAG, "getDrmKeyRequest:   --> request: " + keyRequest);
                } catch (NotProvisionedException e) {
                    Log.w(TAG, "getDrmKeyRequest NotProvisionedException: Unexpected. Shouldn't have reached here.");
                    throw new IllegalStateException("getDrmKeyRequest: Unexpected provisioning error.");
                } catch (Exception e2) {
                    Log.w(TAG, "getDrmKeyRequest Exception " + e2);
                    throw e2;
                }
            } else {
                byte[] bArr32 = bArr;
                if (map == null) {
                }
                keyRequest = this.mDrmObj.getKeyRequest(bArr32, bArr2, str, i, map2);
                Log.v(TAG, "getDrmKeyRequest:   --> request: " + keyRequest);
            }
        }
        return keyRequest;
    }

    @Override
    public byte[] provideDrmKeyResponse(byte[] bArr, byte[] bArr2) throws DeniedByServerException, MediaPlayer2.NoDrmSchemeException {
        byte[] bArr3;
        byte[] bArrProvideKeyResponse;
        Log.v(TAG, "provideDrmKeyResponse: keySetId: " + bArr + " response: " + bArr2);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme) {
                Log.e(TAG, "getDrmKeyRequest NoDrmSchemeException");
                throw new NoDrmSchemeExceptionImpl("getDrmKeyRequest: Has to set a DRM scheme first.");
            }
            if (bArr == null) {
                try {
                    bArr3 = this.mDrmSessionId;
                } catch (NotProvisionedException e) {
                    Log.w(TAG, "provideDrmKeyResponse NotProvisionedException: Unexpected. Shouldn't have reached here.");
                    throw new IllegalStateException("provideDrmKeyResponse: Unexpected provisioning error.");
                } catch (Exception e2) {
                    Log.w(TAG, "provideDrmKeyResponse Exception " + e2);
                    throw e2;
                }
            } else {
                bArr3 = bArr;
            }
            bArrProvideKeyResponse = this.mDrmObj.provideKeyResponse(bArr3, bArr2);
            Log.v(TAG, "provideDrmKeyResponse: keySetId: " + bArr + " response: " + bArr2 + " --> " + bArrProvideKeyResponse);
        }
        return bArrProvideKeyResponse;
    }

    @Override
    public void restoreDrmKeys(final byte[] bArr) throws MediaPlayer2.NoDrmSchemeException {
        addTask(new Task(13, false) {
            @Override
            void process() throws MediaPlayer2.NoDrmSchemeException {
                Log.v(MediaPlayer2Impl.TAG, "restoreDrmKeys: keySetId: " + bArr);
                synchronized (MediaPlayer2Impl.this.mDrmLock) {
                    if (MediaPlayer2Impl.this.mActiveDrmScheme) {
                        try {
                            MediaPlayer2Impl.this.mDrmObj.restoreKeys(MediaPlayer2Impl.this.mDrmSessionId, bArr);
                        } catch (Exception e) {
                            Log.w(MediaPlayer2Impl.TAG, "restoreKeys Exception " + e);
                            throw e;
                        }
                    } else {
                        Log.w(MediaPlayer2Impl.TAG, "restoreDrmKeys NoDrmSchemeException");
                        throw new NoDrmSchemeExceptionImpl("restoreDrmKeys: Has to set a DRM scheme first.");
                    }
                }
            }
        });
    }

    @Override
    public String getDrmPropertyString(String str) throws MediaPlayer2.NoDrmSchemeException {
        String propertyString;
        Log.v(TAG, "getDrmPropertyString: propertyName: " + str);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme && !this.mDrmConfigAllowed) {
                Log.w(TAG, "getDrmPropertyString NoDrmSchemeException");
                throw new NoDrmSchemeExceptionImpl("getDrmPropertyString: Has to prepareDrm() first.");
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

    @Override
    public void setDrmPropertyString(String str, String str2) throws MediaPlayer2.NoDrmSchemeException {
        Log.v(TAG, "setDrmPropertyString: propertyName: " + str + " value: " + str2);
        synchronized (this.mDrmLock) {
            if (!this.mActiveDrmScheme && !this.mDrmConfigAllowed) {
                Log.w(TAG, "setDrmPropertyString NoDrmSchemeException");
                throw new NoDrmSchemeExceptionImpl("setDrmPropertyString: Has to prepareDrm() first.");
            }
            try {
                this.mDrmObj.setPropertyString(str, str2);
            } catch (Exception e) {
                Log.w(TAG, "setDrmPropertyString Exception " + e);
                throw e;
            }
        }
    }

    public static final class DrmInfoImpl extends MediaPlayer2.DrmInfo {
        private Map<UUID, byte[]> mapPssh;
        private UUID[] supportedSchemes;

        @Override
        public Map<UUID, byte[]> getPssh() {
            return this.mapPssh;
        }

        @Override
        public List<UUID> getSupportedSchemes() {
            return Arrays.asList(this.supportedSchemes);
        }

        private DrmInfoImpl(Map<UUID, byte[]> map, UUID[] uuidArr) {
            this.mapPssh = map;
            this.supportedSchemes = uuidArr;
        }

        private DrmInfoImpl(Parcel parcel) {
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl(" + parcel + ") size " + parcel.dataSize());
            int i = parcel.readInt();
            byte[] bArr = new byte[i];
            parcel.readByteArray(bArr);
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() PSSH: " + arrToHex(bArr));
            this.mapPssh = parsePSSH(bArr, i);
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() PSSH: " + this.mapPssh);
            int i2 = parcel.readInt();
            this.supportedSchemes = new UUID[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                byte[] bArr2 = new byte[16];
                parcel.readByteArray(bArr2);
                this.supportedSchemes[i3] = bytesToUUID(bArr2);
                Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() supportedScheme[" + i3 + "]: " + this.supportedSchemes[i3]);
            }
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() Parcel psshsize: " + i + " supportedDRMsCount: " + i2);
        }

        private DrmInfoImpl makeCopy() {
            return new DrmInfoImpl(this.mapPssh, this.supportedSchemes);
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
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse UUID: (%d < 16) pssh: %d", Integer.valueOf(i2), Integer.valueOf(i)));
                    return null;
                }
                int i5 = i3 + 16;
                UUID uuidBytesToUUID = bytesToUUID(Arrays.copyOfRange(bArr, i3, i5));
                int i6 = i2 - 16;
                if (i6 < 4) {
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse datalen: (%d < 4) pssh: %d", Integer.valueOf(i6), Integer.valueOf(i)));
                    return null;
                }
                int i7 = i5 + 4;
                byte[] bArrCopyOfRange = Arrays.copyOfRange(bArr, i5, i7);
                int i8 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? ((bArrCopyOfRange[2] & 255) << 16) | ((bArrCopyOfRange[3] & 255) << 24) | ((bArrCopyOfRange[1] & 255) << 8) | (bArrCopyOfRange[0] & 255) : ((bArrCopyOfRange[1] & 255) << 16) | ((bArrCopyOfRange[0] & 255) << 24) | ((bArrCopyOfRange[2] & 255) << 8) | (bArrCopyOfRange[3] & 255);
                int i9 = i6 - 4;
                if (i9 < i8) {
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse data: (%d < %d) pssh: %d", Integer.valueOf(i9), Integer.valueOf(i8), Integer.valueOf(i)));
                    return null;
                }
                int i10 = i7 + i8;
                byte[] bArrCopyOfRange2 = Arrays.copyOfRange(bArr, i7, i10);
                i2 = i9 - i8;
                Log.v(MediaPlayer2Impl.TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d", Integer.valueOf(i4), uuidBytesToUUID, arrToHex(bArrCopyOfRange2), Integer.valueOf(i)));
                i4++;
                map.put(uuidBytesToUUID, bArrCopyOfRange2);
                i3 = i10;
            }
            return map;
        }
    }

    public static final class NoDrmSchemeExceptionImpl extends MediaPlayer2.NoDrmSchemeException {
        public NoDrmSchemeExceptionImpl(String str) {
            super(str);
        }
    }

    public static final class ProvisioningNetworkErrorExceptionImpl extends MediaPlayer2.ProvisioningNetworkErrorException {
        public ProvisioningNetworkErrorExceptionImpl(String str) {
            super(str);
        }
    }

    public static final class ProvisioningServerErrorExceptionImpl extends MediaPlayer2.ProvisioningServerErrorException {
        public ProvisioningServerErrorExceptionImpl(String str) {
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

    private static boolean setAudioOutputDeviceById(AudioTrack audioTrack, int i) {
        if (audioTrack == null) {
            return false;
        }
        if (i == 0) {
            audioTrack.setPreferredDevice(null);
            return true;
        }
        for (AudioDeviceInfo audioDeviceInfo : AudioManager.getDevicesStatic(2)) {
            if (audioDeviceInfo.getId() == i) {
                audioTrack.setPreferredDevice(audioDeviceInfo);
                return true;
            }
        }
        return false;
    }

    private static class StreamEventCallback extends AudioTrack.StreamEventCallback {
        public long mJAudioTrackPtr;
        public long mNativeCallbackPtr;
        public long mUserDataPtr;

        public StreamEventCallback(long j, long j2, long j3) {
            this.mJAudioTrackPtr = j;
            this.mNativeCallbackPtr = j2;
            this.mUserDataPtr = j3;
        }

        @Override
        public void onTearDown(AudioTrack audioTrack) {
            MediaPlayer2Impl.native_stream_event_onTearDown(this.mNativeCallbackPtr, this.mUserDataPtr);
        }

        @Override
        public void onStreamPresentationEnd(AudioTrack audioTrack) {
            MediaPlayer2Impl.native_stream_event_onStreamPresentationEnd(this.mNativeCallbackPtr, this.mUserDataPtr);
        }

        @Override
        public void onStreamDataRequest(AudioTrack audioTrack) {
            MediaPlayer2Impl.native_stream_event_onStreamDataRequest(this.mJAudioTrackPtr, this.mNativeCallbackPtr, this.mUserDataPtr);
        }
    }

    private class ProvisioningThread extends Thread {
        public static final int TIMEOUT_MS = 60000;
        private Object drmLock;
        private boolean finished;
        private MediaPlayer2Impl mediaPlayer;
        private int status;
        private String urlStr;
        private UUID uuid;

        private ProvisioningThread() {
        }

        public int status() {
            return this.status;
        }

        public ProvisioningThread initialize(MediaDrm.ProvisionRequest provisionRequest, UUID uuid, MediaPlayer2Impl mediaPlayer2Impl) {
            this.drmLock = mediaPlayer2Impl.mDrmLock;
            this.mediaPlayer = mediaPlayer2Impl;
            this.urlStr = provisionRequest.getDefaultUrl() + "&signedRequest=" + new String(provisionRequest.getData());
            this.uuid = uuid;
            this.status = 3;
            Log.v(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread is initialised url: " + this.urlStr);
            return this;
        }

        @Override
        public void run() throws Throwable {
            byte[] fully;
            boolean z;
            boolean z2;
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
                        Log.v(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: response " + fully.length + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + fully);
                    } catch (Exception e2) {
                        e = e2;
                        this.status = 1;
                        Log.w(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: connect " + e + " url: " + url);
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
                Log.w(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: openConnection " + e);
                if (fully == null) {
                }
                synchronized (MediaPlayer2Impl.this.mDrmEventCbLock) {
                }
            }
            try {
                httpURLConnection.disconnect();
            } catch (Exception e5) {
                e = e5;
                this.status = 1;
                Log.w(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: openConnection " + e);
            }
            if (fully == null) {
                try {
                    MediaPlayer2Impl.this.mDrmObj.provideProvisionResponse(fully);
                    Log.v(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse SUCCEEDED!");
                    z = true;
                } catch (Exception e6) {
                    this.status = 2;
                    Log.w(MediaPlayer2Impl.TAG, "HandleProvisioninig: Thread run: provideProvisionResponse " + e6);
                    z = false;
                }
            } else {
                z = false;
            }
            synchronized (MediaPlayer2Impl.this.mDrmEventCbLock) {
                z2 = !MediaPlayer2Impl.this.mDrmEventCallbackRecords.isEmpty();
            }
            int i = 3;
            if (z2) {
                synchronized (this.drmLock) {
                    if (z) {
                        try {
                            zResumePrepareDrm2 = this.mediaPlayer.resumePrepareDrm(this.uuid);
                            if (zResumePrepareDrm2) {
                                i = 0;
                            }
                            this.status = i;
                        } finally {
                        }
                    } else {
                        zResumePrepareDrm2 = false;
                    }
                    this.mediaPlayer.mDrmProvisioningInProgress = false;
                    this.mediaPlayer.mPrepareDrmInProgress = false;
                    if (!zResumePrepareDrm2) {
                        MediaPlayer2Impl.this.cleanDrmObj();
                    }
                }
                synchronized (MediaPlayer2Impl.this.mDrmEventCbLock) {
                    for (final Pair pair : MediaPlayer2Impl.this.mDrmEventCallbackRecords) {
                        ((Executor) pair.first).execute(new Runnable() {
                            @Override
                            public final void run() {
                                MediaPlayer2Impl.ProvisioningThread provisioningThread = this.f$0;
                                ((MediaPlayer2.DrmEventCallback) pair.second).onDrmPrepared(provisioningThread.mediaPlayer, MediaPlayer2Impl.this.mCurrentDSD, provisioningThread.status);
                            }
                        });
                    }
                }
            } else {
                if (z) {
                    zResumePrepareDrm = this.mediaPlayer.resumePrepareDrm(this.uuid);
                    if (zResumePrepareDrm) {
                        i = 0;
                    }
                    this.status = i;
                } else {
                    zResumePrepareDrm = false;
                }
                this.mediaPlayer.mDrmProvisioningInProgress = false;
                this.mediaPlayer.mPrepareDrmInProgress = false;
                if (!zResumePrepareDrm) {
                    MediaPlayer2Impl.this.cleanDrmObj();
                }
            }
            this.finished = true;
        }
    }

    private int HandleProvisioninig(UUID uuid) {
        boolean z;
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
        synchronized (this.mDrmEventCbLock) {
            z = !this.mDrmEventCallbackRecords.isEmpty();
        }
        if (z) {
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
            Log.v(TAG, "resetDrmState:  mDrmInfoImpl=" + this.mDrmInfoImpl + " mDrmProvisioningThread=" + this.mDrmProvisioningThread + " mPrepareDrmInProgress=" + this.mPrepareDrmInProgress + " mActiveDrmScheme=" + this.mActiveDrmScheme);
            this.mDrmInfoResolved = false;
            this.mDrmInfoImpl = null;
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

    static class TimeProvider implements MediaTimeProvider {
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
        private EventHandler mEventHandler;
        private HandlerThread mHandlerThread;
        private long mLastReportedTime;
        private long mLastTimeUs;
        private MediaTimeProvider.OnMediaTimeListener[] mListeners;
        private MediaPlayer2Impl mPlayer;
        private boolean mRefresh;
        private long[] mTimes;
        private boolean mPaused = true;
        private boolean mStopped = true;
        private boolean mPausing = false;
        private boolean mSeeking = false;
        public boolean DEBUG = false;

        public TimeProvider(MediaPlayer2Impl mediaPlayer2Impl) {
            this.mLastTimeUs = 0L;
            this.mRefresh = false;
            this.mPlayer = mediaPlayer2Impl;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                this.mRefresh = true;
            }
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper == null && (looperMyLooper = Looper.getMainLooper()) == null) {
                this.mHandlerThread = new HandlerThread("MediaPlayer2MTPEventThread", -2);
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

        public void onSeekComplete(MediaPlayer2Impl mediaPlayer2Impl) {
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
                    this.mLastTimeUs = this.mPlayer.getCurrentPosition() * 1000;
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

    private abstract class Task implements Runnable {
        private DataSourceDesc mDSD;
        private final int mMediaCallType;
        private final boolean mNeedToWaitForEventToComplete;

        abstract void process() throws IOException, MediaPlayer2.NoDrmSchemeException;

        public Task(int i, boolean z) {
            this.mMediaCallType = i;
            this.mNeedToWaitForEventToComplete = z;
        }

        @Override
        public void run() {
            int i;
            try {
                process();
                i = 0;
            } catch (MediaPlayer2.NoDrmSchemeException e) {
                i = 5;
            } catch (IOException e2) {
                i = 4;
            } catch (IllegalArgumentException e3) {
                i = 2;
            } catch (IllegalStateException e4) {
                i = 1;
            } catch (SecurityException e5) {
                i = 3;
            } catch (Exception e6) {
                i = Integer.MIN_VALUE;
            }
            synchronized (MediaPlayer2Impl.this.mSrcLock) {
                this.mDSD = MediaPlayer2Impl.this.mCurrentDSD;
            }
            if (!this.mNeedToWaitForEventToComplete || i != 0) {
                sendCompleteNotification(i);
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    MediaPlayer2Impl.this.mCurrentTask = null;
                    MediaPlayer2Impl.this.processPendingTask_l();
                }
            }
        }

        private void sendCompleteNotification(final int i) {
            if (this.mMediaCallType != 1003) {
                synchronized (MediaPlayer2Impl.this.mEventCbLock) {
                    for (final Pair pair : MediaPlayer2Impl.this.mEventCallbackRecords) {
                        ((Executor) pair.first).execute(new Runnable() {
                            @Override
                            public final void run() {
                                MediaPlayer2Impl.Task task = this.f$0;
                                ((MediaPlayer2.MediaPlayer2EventCallback) pair.second).onCallCompleted(MediaPlayer2Impl.this, task.mDSD, task.mMediaCallType, i);
                            }
                        });
                    }
                }
            }
        }
    }
}
