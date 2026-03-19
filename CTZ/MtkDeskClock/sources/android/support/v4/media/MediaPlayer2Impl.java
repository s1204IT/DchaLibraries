package android.support.v4.media;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.DeniedByServerException;
import android.media.MediaDataSource;
import android.media.MediaDrm;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.media.PlaybackParams;
import android.media.ResourceBusyException;
import android.media.SubtitleData;
import android.media.SyncParams;
import android.media.TimedMetaData;
import android.media.UnsupportedSchemeException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.media.BaseMediaPlayer;
import android.support.v4.media.MediaPlayer2;
import android.support.v4.media.PlaybackParams2;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@TargetApi(28)
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public final class MediaPlayer2Impl extends MediaPlayer2 {
    private static final int SOURCE_STATE_ERROR = -1;
    private static final int SOURCE_STATE_INIT = 0;
    private static final int SOURCE_STATE_PREPARED = 2;
    private static final int SOURCE_STATE_PREPARING = 1;
    private static final String TAG = "MediaPlayer2Impl";
    private static ArrayMap<Integer, Integer> sErrorEventMap;
    private static ArrayMap<Integer, Integer> sInfoEventMap = new ArrayMap<>();
    private static ArrayMap<Integer, Integer> sPrepareDrmStatusMap;
    private static ArrayMap<Integer, Integer> sStateMap;
    private BaseMediaPlayerImpl mBaseMediaPlayerImpl;

    @GuardedBy("mTaskLock")
    private Task mCurrentTask;
    private Pair<Executor, MediaPlayer2.DrmEventCallback> mDrmEventCallbackRecord;
    private final Handler mEndPositionHandler;
    private Pair<Executor, MediaPlayer2.EventCallback> mMp2EventCallbackRecord;
    private MediaPlayerSourceQueue mPlayer;
    private final Handler mTaskHandler;
    private final Object mTaskLock = new Object();

    @GuardedBy("mTaskLock")
    private final ArrayDeque<Task> mPendingTasks = new ArrayDeque<>();
    private final Object mLock = new Object();
    private ArrayMap<BaseMediaPlayer.PlayerEventCallback, Executor> mPlayerEventCallbackMap = new ArrayMap<>();
    private HandlerThread mHandlerThread = new HandlerThread("MediaPlayer2TaskThread");

    private interface DrmEventNotifier {
        void notify(MediaPlayer2.DrmEventCallback drmEventCallback);
    }

    private interface Mp2EventNotifier {
        void notify(MediaPlayer2.EventCallback eventCallback);
    }

    private interface PlayerEventNotifier {
        void notify(BaseMediaPlayer.PlayerEventCallback playerEventCallback);
    }

    static {
        sInfoEventMap.put(1, 1);
        sInfoEventMap.put(2, 2);
        sInfoEventMap.put(3, 3);
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_START), Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_START));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_END), Integer.valueOf(MediaPlayer2.MEDIA_INFO_BUFFERING_END));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_BAD_INTERLEAVING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_BAD_INTERLEAVING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE), Integer.valueOf(MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_METADATA_UPDATE), Integer.valueOf(MediaPlayer2.MEDIA_INFO_METADATA_UPDATE));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_AUDIO_NOT_PLAYING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_AUDIO_NOT_PLAYING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_NOT_PLAYING), Integer.valueOf(MediaPlayer2.MEDIA_INFO_VIDEO_NOT_PLAYING));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_UNSUPPORTED_SUBTITLE), Integer.valueOf(MediaPlayer2.MEDIA_INFO_UNSUPPORTED_SUBTITLE));
        sInfoEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_INFO_SUBTITLE_TIMED_OUT), Integer.valueOf(MediaPlayer2.MEDIA_INFO_SUBTITLE_TIMED_OUT));
        sErrorEventMap = new ArrayMap<>();
        sErrorEventMap.put(1, 1);
        sErrorEventMap.put(200, 200);
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_IO), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_IO));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_MALFORMED), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_MALFORMED));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED));
        sErrorEventMap.put(Integer.valueOf(MediaPlayer2.MEDIA_ERROR_TIMED_OUT), Integer.valueOf(MediaPlayer2.MEDIA_ERROR_TIMED_OUT));
        sPrepareDrmStatusMap = new ArrayMap<>();
        sPrepareDrmStatusMap.put(0, 0);
        sPrepareDrmStatusMap.put(1, 1);
        sPrepareDrmStatusMap.put(2, 2);
        sPrepareDrmStatusMap.put(2, 2);
        sStateMap = new ArrayMap<>();
        sStateMap.put(1001, 0);
        sStateMap.put(1002, 1);
        sStateMap.put(1003, 1);
        sStateMap.put(1004, 2);
        sStateMap.put(Integer.valueOf(MediaPlayer2.MEDIAPLAYER2_STATE_ERROR), 3);
    }

    private void handleDataSourceError(final DataSourceError err) throws Throwable {
        if (err == null) {
            return;
        }
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2.EventCallback callback) {
                callback.onError(MediaPlayer2Impl.this, err.mDSD, err.mWhat, err.mExtra);
            }
        });
    }

    public MediaPlayer2Impl() {
        this.mHandlerThread.start();
        Looper looper = this.mHandlerThread.getLooper();
        this.mEndPositionHandler = new Handler(looper);
        this.mTaskHandler = new Handler(looper);
        this.mPlayer = new MediaPlayerSourceQueue();
    }

    @Override
    public BaseMediaPlayer getBaseMediaPlayer() {
        BaseMediaPlayerImpl baseMediaPlayerImpl;
        synchronized (this.mLock) {
            if (this.mBaseMediaPlayerImpl == null) {
                this.mBaseMediaPlayerImpl = new BaseMediaPlayerImpl();
            }
            baseMediaPlayerImpl = this.mBaseMediaPlayerImpl;
        }
        return baseMediaPlayerImpl;
    }

    @Override
    public void close() {
        this.mPlayer.release();
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
    }

    @Override
    public void play() {
        addTask(new Task(5, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.play();
            }
        });
    }

    @Override
    public void prepare() {
        addTask(new Task(6, true) {
            @Override
            void process() throws IOException {
                MediaPlayer2Impl.this.mPlayer.prepareAsync();
            }
        });
    }

    @Override
    public void pause() {
        addTask(new Task(4, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.pause();
            }
        });
    }

    @Override
    public void skipToNext() {
        addTask(new Task(29, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.skipToNext();
            }
        });
    }

    @Override
    public long getCurrentPosition() {
        return this.mPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return this.mPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return this.mPlayer.getBufferedPosition();
    }

    @Override
    public int getState() {
        return this.mPlayer.getMediaPlayer2State();
    }

    private int getPlayerState() {
        return this.mPlayer.getPlayerState();
    }

    private int getBufferingState() {
        return this.mPlayer.getBufferingState();
    }

    @Override
    public void setAudioAttributes(@NonNull final AudioAttributesCompat attributes) {
        addTask(new Task(16, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.setAudioAttributes(attributes);
            }
        });
    }

    @Override
    @NonNull
    public AudioAttributesCompat getAudioAttributes() {
        return this.mPlayer.getAudioAttributes();
    }

    @Override
    public void setDataSource(@NonNull final DataSourceDesc dsd) {
        addTask(new Task(19, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                try {
                    MediaPlayer2Impl.this.mPlayer.setFirst(dsd);
                } catch (IOException e) {
                    Log.e(MediaPlayer2Impl.TAG, "process: setDataSource", e);
                }
            }
        });
    }

    @Override
    public void setNextDataSource(@NonNull final DataSourceDesc dsd) {
        addTask(new Task(22, false) {
            @Override
            void process() throws Throwable {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.setNext(dsd));
            }
        });
    }

    @Override
    public void setNextDataSources(@NonNull final List<DataSourceDesc> dsds) {
        addTask(new Task(23, false) {
            @Override
            void process() throws Throwable {
                if (dsds == null || dsds.size() == 0) {
                    throw new IllegalArgumentException("data source list cannot be null or empty.");
                }
                for (DataSourceDesc dsd : dsds) {
                    if (dsd == null) {
                        throw new IllegalArgumentException("DataSourceDesc in the source list cannot be null.");
                    }
                }
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.setNextMultiple(dsds));
            }
        });
    }

    @Override
    @NonNull
    public DataSourceDesc getCurrentDataSource() {
        return this.mPlayer.getFirst().getDSD();
    }

    @Override
    public void loopCurrent(final boolean loop) {
        addTask(new Task(3, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.setLooping(loop);
            }
        });
    }

    @Override
    public void setPlayerVolume(final float volume) {
        addTask(new Task(26, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.setVolume(volume);
            }
        });
    }

    @Override
    public float getPlayerVolume() {
        return this.mPlayer.getVolume();
    }

    @Override
    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    private void registerPlayerEventCallback(@NonNull Executor e, @NonNull BaseMediaPlayer.PlayerEventCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null PlayerEventCallback");
        }
        if (e == null) {
            throw new IllegalArgumentException("Illegal null Executor for the PlayerEventCallback");
        }
        synchronized (this.mLock) {
            this.mPlayerEventCallbackMap.put(cb, e);
        }
    }

    private void unregisterPlayerEventCallback(@NonNull BaseMediaPlayer.PlayerEventCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null PlayerEventCallback");
        }
        synchronized (this.mLock) {
            this.mPlayerEventCallbackMap.remove(cb);
        }
    }

    @Override
    public void notifyWhenCommandLabelReached(final Object label) {
        addTask(new Task(1003, false) {
            @Override
            void process() throws Throwable {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onCommandLabelReached(MediaPlayer2Impl.this, label);
                    }
                });
            }
        });
    }

    @Override
    public void setSurface(final Surface surface) {
        addTask(new Task(27, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.setSurface(surface);
            }
        });
    }

    @Override
    public void clearPendingCommands() {
        synchronized (this.mTaskLock) {
            this.mPendingTasks.clear();
        }
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
            Task task = this.mPendingTasks.removeFirst();
            this.mCurrentTask = task;
            this.mTaskHandler.post(task);
        }
    }

    private static void handleDataSource(MediaPlayerSource src) throws IOException {
        final DataSourceDesc dsd = src.getDSD();
        Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
        MediaPlayer player = src.mPlayer;
        switch (dsd.getType()) {
            case 1:
                player.setDataSource(new MediaDataSource() {
                    Media2DataSource mDataSource;

                    {
                        this.mDataSource = dsd.getMedia2DataSource();
                    }

                    @Override
                    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
                        return this.mDataSource.readAt(position, buffer, offset, size);
                    }

                    @Override
                    public long getSize() throws IOException {
                        return this.mDataSource.getSize();
                    }

                    @Override
                    public void close() throws IOException {
                        this.mDataSource.close();
                    }
                });
                break;
            case 2:
                player.setDataSource(dsd.getFileDescriptor(), dsd.getFileDescriptorOffset(), dsd.getFileDescriptorLength());
                break;
            case 3:
                player.setDataSource(dsd.getUriContext(), dsd.getUri(), dsd.getUriHeaders(), dsd.getUriCookies());
                break;
        }
    }

    @Override
    public int getVideoWidth() {
        return this.mPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return this.mPlayer.getVideoHeight();
    }

    @Override
    public PersistableBundle getMetrics() {
        return this.mPlayer.getMetrics();
    }

    @Override
    public void setPlaybackParams(@NonNull final PlaybackParams2 params) {
        addTask(new Task(24, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.setPlaybackParamsInternal(params.getPlaybackParams());
            }
        });
    }

    @Override
    @NonNull
    public PlaybackParams2 getPlaybackParams() {
        return new PlaybackParams2.Builder(this.mPlayer.getPlaybackParams()).build();
    }

    @Override
    public void seekTo(final long msec, final int mode) {
        addTask(new Task(14, true) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.seekTo(msec, mode);
            }
        });
    }

    @Override
    @Nullable
    public MediaTimestamp2 getTimestamp() {
        return this.mPlayer.getTimestamp();
    }

    @Override
    public void reset() {
        this.mPlayer.reset();
        synchronized (this.mLock) {
            this.mMp2EventCallbackRecord = null;
            this.mPlayerEventCallbackMap.clear();
            this.mDrmEventCallbackRecord = null;
        }
    }

    @Override
    public void setAudioSessionId(final int sessionId) {
        addTask(new Task(17, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.setAudioSessionId(sessionId);
            }
        });
    }

    @Override
    public int getAudioSessionId() {
        return this.mPlayer.getAudioSessionId();
    }

    @Override
    public void attachAuxEffect(final int effectId) {
        addTask(new Task(1, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.attachAuxEffect(effectId);
            }
        });
    }

    @Override
    public void setAuxEffectSendLevel(final float level) {
        addTask(new Task(18, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.setAuxEffectSendLevel(level);
            }
        });
    }

    public static final class TrackInfoImpl extends MediaPlayer2.TrackInfo {
        final MediaFormat mFormat;
        final int mTrackType;

        @Override
        public int getTrackType() {
            return this.mTrackType;
        }

        @Override
        public String getLanguage() {
            String language = this.mFormat.getString("language");
            return language == null ? "und" : language;
        }

        @Override
        public MediaFormat getFormat() {
            if (this.mTrackType == 3 || this.mTrackType == 4) {
                return this.mFormat;
            }
            return null;
        }

        TrackInfoImpl(int type, MediaFormat format) {
            this.mTrackType = type;
            this.mFormat = format;
        }

        @Override
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
            out.append(", " + this.mFormat.toString());
            out.append("}");
            return out.toString();
        }
    }

    @Override
    public List<MediaPlayer2.TrackInfo> getTrackInfo() {
        MediaPlayer.TrackInfo[] list = this.mPlayer.getTrackInfo();
        List<MediaPlayer2.TrackInfo> trackList = new ArrayList<>();
        for (MediaPlayer.TrackInfo info : list) {
            trackList.add(new TrackInfoImpl(info.getTrackType(), info.getFormat()));
        }
        return trackList;
    }

    @Override
    public int getSelectedTrack(int trackType) {
        return this.mPlayer.getSelectedTrack(trackType);
    }

    @Override
    public void selectTrack(final int index) {
        addTask(new Task(15, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.selectTrack(index);
            }
        });
    }

    @Override
    public void deselectTrack(final int index) {
        addTask(new Task(2, false) {
            @Override
            void process() {
                MediaPlayer2Impl.this.mPlayer.deselectTrack(index);
            }
        });
    }

    @Override
    public void setEventCallback(@NonNull Executor executor, @NonNull MediaPlayer2.EventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Illegal null Executor for the EventCallback");
        }
        synchronized (this.mLock) {
            this.mMp2EventCallbackRecord = new Pair<>(executor, eventCallback);
        }
    }

    @Override
    public void clearEventCallback() {
        synchronized (this.mLock) {
            this.mMp2EventCallbackRecord = null;
        }
    }

    @Override
    public void setOnDrmConfigHelper(final MediaPlayer2.OnDrmConfigHelper listener) {
        this.mPlayer.setOnDrmConfigHelper(new MediaPlayer.OnDrmConfigHelper() {
            @Override
            public void onDrmConfig(MediaPlayer mp) {
                MediaPlayerSource src = MediaPlayer2Impl.this.mPlayer.getSourceForPlayer(mp);
                DataSourceDesc dsd = src == null ? null : src.getDSD();
                listener.onDrmConfig(MediaPlayer2Impl.this, dsd);
            }
        });
    }

    @Override
    public void setDrmEventCallback(@NonNull Executor executor, @NonNull MediaPlayer2.DrmEventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Illegal null Executor for the EventCallback");
        }
        synchronized (this.mLock) {
            this.mDrmEventCallbackRecord = new Pair<>(executor, eventCallback);
        }
    }

    @Override
    public void clearDrmEventCallback() {
        synchronized (this.mLock) {
            this.mDrmEventCallbackRecord = null;
        }
    }

    @Override
    public MediaPlayer2.DrmInfo getDrmInfo() {
        MediaPlayer.DrmInfo info = this.mPlayer.getDrmInfo();
        if (info == null) {
            return null;
        }
        return new DrmInfoImpl(info.getPssh(), info.getSupportedSchemes());
    }

    @Override
    public void prepareDrm(@NonNull UUID uuid) throws UnsupportedSchemeException, ResourceBusyException, MediaPlayer2.ProvisioningServerErrorException, MediaPlayer2.ProvisioningNetworkErrorException {
        try {
            this.mPlayer.prepareDrm(uuid);
        } catch (MediaPlayer.ProvisioningNetworkErrorException e) {
            throw new MediaPlayer2.ProvisioningNetworkErrorException(e.getMessage());
        } catch (MediaPlayer.ProvisioningServerErrorException e2) {
            throw new MediaPlayer2.ProvisioningServerErrorException(e2.getMessage());
        }
    }

    @Override
    public void releaseDrm() throws MediaPlayer2.NoDrmSchemeException {
        try {
            this.mPlayer.releaseDrm();
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @Override
    @NonNull
    public MediaDrm.KeyRequest getDrmKeyRequest(@Nullable byte[] keySetId, @Nullable byte[] initData, @Nullable String mimeType, int keyType, @Nullable Map<String, String> optionalParameters) throws MediaPlayer2.NoDrmSchemeException {
        try {
            return this.mPlayer.getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @Override
    public byte[] provideDrmKeyResponse(@Nullable byte[] keySetId, @NonNull byte[] response) throws MediaPlayer2.NoDrmSchemeException, DeniedByServerException {
        try {
            return this.mPlayer.provideKeyResponse(keySetId, response);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @Override
    public void restoreDrmKeys(@NonNull byte[] keySetId) throws MediaPlayer2.NoDrmSchemeException {
        try {
            this.mPlayer.restoreKeys(keySetId);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @Override
    @NonNull
    public String getDrmPropertyString(@NonNull String propertyName) throws MediaPlayer2.NoDrmSchemeException {
        try {
            return this.mPlayer.getDrmPropertyString(propertyName);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    @Override
    public void setDrmPropertyString(@NonNull String propertyName, @NonNull String value) throws MediaPlayer2.NoDrmSchemeException {
        try {
            this.mPlayer.setDrmPropertyString(propertyName, value);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new MediaPlayer2.NoDrmSchemeException(e.getMessage());
        }
    }

    private void setPlaybackParamsInternal(final PlaybackParams params) {
        PlaybackParams current = this.mPlayer.getPlaybackParams();
        this.mPlayer.setPlaybackParams(params);
        if (current.getSpeed() != params.getSpeed()) {
            notifyPlayerEvent(new PlayerEventNotifier() {
                @Override
                public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                    cb.onPlaybackSpeedChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, params.getSpeed());
                }
            });
        }
    }

    private void notifyMediaPlayer2Event(final Mp2EventNotifier notifier) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    final Pair<Executor, MediaPlayer2.EventCallback> record = this.mMp2EventCallbackRecord;
                    if (record != null) {
                        ((Executor) record.first).execute(new Runnable() {
                            @Override
                            public void run() {
                                notifier.notify((MediaPlayer2.EventCallback) record.second);
                            }
                        });
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void notifyPlayerEvent(final PlayerEventNotifier notifier) {
        ArrayMap<BaseMediaPlayer.PlayerEventCallback, Executor> map;
        synchronized (this.mLock) {
            map = new ArrayMap<>(this.mPlayerEventCallbackMap);
        }
        int callbackCount = map.size();
        for (int i = 0; i < callbackCount; i++) {
            Executor executor = map.valueAt(i);
            final BaseMediaPlayer.PlayerEventCallback cb = map.keyAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(cb);
                }
            });
        }
    }

    private void notifyDrmEvent(final DrmEventNotifier notifier) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    final Pair<Executor, MediaPlayer2.DrmEventCallback> record = this.mDrmEventCallbackRecord;
                    if (record != null) {
                        ((Executor) record.first).execute(new Runnable() {
                            @Override
                            public void run() {
                                notifier.notify((MediaPlayer2.DrmEventCallback) record.second);
                            }
                        });
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void setEndPositionTimerIfNeeded(final MediaPlayer.OnCompletionListener completionListener, final MediaPlayerSource src, MediaTimestamp timedsd) {
        if (src == this.mPlayer.getFirst()) {
            this.mEndPositionHandler.removeCallbacksAndMessages(null);
            DataSourceDesc dsd = src.getDSD();
            if (dsd.getEndPosition() != 576460752303423487L && timedsd.getMediaClockRate() > 0.0f) {
                long nowNs = System.nanoTime();
                long elapsedTimeUs = (nowNs - timedsd.getAnchorSytemNanoTime()) / 1000;
                long nowMediaMs = (timedsd.getAnchorMediaTimeUs() + elapsedTimeUs) / 1000;
                long timeLeftMs = (long) ((dsd.getEndPosition() - nowMediaMs) / timedsd.getMediaClockRate());
                this.mEndPositionHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (MediaPlayer2Impl.this.mPlayer.getFirst() == src) {
                            MediaPlayer2Impl.this.mPlayer.pause();
                            completionListener.onCompletion(src.mPlayer);
                        }
                    }
                }, timeLeftMs >= 0 ? timeLeftMs : 0L);
            }
        }
    }

    private void setUpListeners(final MediaPlayerSource src) {
        MediaPlayer p = src.mPlayer;
        final MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) throws Throwable {
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.onPrepared(mp));
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback callback) {
                        MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                        DataSourceDesc dsd = src.getDSD();
                        callback.onInfo(mp2, dsd, 100, 0);
                    }
                });
                MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                        cb.onMediaPrepared(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src.getDSD());
                    }
                });
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 6 && MediaPlayer2Impl.this.mCurrentTask.mDSD == src.getDSD() && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                        MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                        MediaPlayer2Impl.this.mCurrentTask = null;
                        MediaPlayer2Impl.this.processPendingTask_l();
                    }
                }
            }
        };
        p.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (src.getDSD().getStartPosition() != 0) {
                    src.mPlayer.seekTo((int) src.getDSD().getStartPosition(), 3);
                } else {
                    preparedListener.onPrepared(mp);
                }
            }
        });
        p.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, final int width, final int height) throws Throwable {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onVideoSizeChanged(MediaPlayer2Impl.this, src.getDSD(), width, height);
                    }
                });
            }
        });
        p.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) throws Throwable {
                if (what != 3) {
                    switch (what) {
                        case MediaPlayer2.MEDIA_INFO_BUFFERING_START:
                            MediaPlayer2Impl.this.mPlayer.setBufferingState(mp, 2);
                            break;
                        case MediaPlayer2.MEDIA_INFO_BUFFERING_END:
                            MediaPlayer2Impl.this.mPlayer.setBufferingState(mp, 1);
                            break;
                    }
                    return false;
                }
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), 3, 0);
                    }
                });
                return false;
            }
        });
        final MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) throws Throwable {
                MediaPlayer2Impl.this.handleDataSourceError(MediaPlayer2Impl.this.mPlayer.onCompletion(mp));
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                        DataSourceDesc dsd = src.getDSD();
                        cb.onInfo(mp2, dsd, 5, 0);
                    }
                });
            }
        };
        p.setOnCompletionListener(completionListener);
        p.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, final int what, final int extra) throws Throwable {
                MediaPlayer2Impl.this.mPlayer.onError(mp);
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                        MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(Integer.MIN_VALUE);
                        MediaPlayer2Impl.this.mCurrentTask = null;
                        MediaPlayer2Impl.this.processPendingTask_l();
                    }
                }
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        int w = ((Integer) MediaPlayer2Impl.sErrorEventMap.getOrDefault(Integer.valueOf(what), 1)).intValue();
                        cb.onError(MediaPlayer2Impl.this, src.getDSD(), w, extra);
                    }
                });
                return true;
            }
        });
        p.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (src.mMp2State != 1001 || src.getDSD().getStartPosition() == 0) {
                    synchronized (MediaPlayer2Impl.this.mTaskLock) {
                        if (MediaPlayer2Impl.this.mCurrentTask != null && MediaPlayer2Impl.this.mCurrentTask.mMediaCallType == 14 && MediaPlayer2Impl.this.mCurrentTask.mNeedToWaitForEventToComplete) {
                            MediaPlayer2Impl.this.mCurrentTask.sendCompleteNotification(0);
                            MediaPlayer2Impl.this.mCurrentTask = null;
                            MediaPlayer2Impl.this.processPendingTask_l();
                        }
                    }
                    final long seekPos = MediaPlayer2Impl.this.getCurrentPosition();
                    MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                        @Override
                        public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                            cb.onSeekCompleted(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, seekPos);
                        }
                    });
                    return;
                }
                preparedListener.onPrepared(mp);
            }
        });
        p.setOnTimedMetaDataAvailableListener(new MediaPlayer.OnTimedMetaDataAvailableListener() {
            @Override
            public void onTimedMetaDataAvailable(MediaPlayer mp, final TimedMetaData data) throws Throwable {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onTimedMetaDataAvailable(MediaPlayer2Impl.this, src.getDSD(), data);
                    }
                });
            }
        });
        p.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, final int what, final int extra) throws Throwable {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        int w = ((Integer) MediaPlayer2Impl.sInfoEventMap.getOrDefault(Integer.valueOf(what), 1)).intValue();
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), w, extra);
                    }
                });
                return true;
            }
        });
        p.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, final int percent) throws Throwable {
                if (percent >= 100) {
                    MediaPlayer2Impl.this.mPlayer.setBufferingState(mp, 3);
                }
                src.mBufferedPercentage.set(percent);
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, percent);
                    }
                });
            }
        });
        p.setOnMediaTimeDiscontinuityListener(new MediaPlayer.OnMediaTimeDiscontinuityListener() {
            @Override
            public void onMediaTimeDiscontinuity(MediaPlayer mp, final MediaTimestamp timestamp) throws Throwable {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onMediaTimeDiscontinuity(MediaPlayer2Impl.this, src.getDSD(), new MediaTimestamp2(timestamp));
                    }
                });
                MediaPlayer2Impl.this.setEndPositionTimerIfNeeded(completionListener, src, timestamp);
            }
        });
        p.setOnSubtitleDataListener(new MediaPlayer.OnSubtitleDataListener() {
            @Override
            public void onSubtitleData(MediaPlayer mp, final SubtitleData data) throws Throwable {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onSubtitleData(MediaPlayer2Impl.this, src.getDSD(), new SubtitleData2(data));
                    }
                });
            }
        });
        p.setOnDrmInfoListener(new MediaPlayer.OnDrmInfoListener() {
            @Override
            public void onDrmInfo(MediaPlayer mp, final MediaPlayer.DrmInfo drmInfo) throws Throwable {
                MediaPlayer2Impl.this.notifyDrmEvent(new DrmEventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.DrmEventCallback cb) {
                        cb.onDrmInfo(MediaPlayer2Impl.this, src.getDSD(), new DrmInfoImpl(drmInfo.getPssh(), drmInfo.getSupportedSchemes()));
                    }
                });
            }
        });
        p.setOnDrmPreparedListener(new MediaPlayer.OnDrmPreparedListener() {
            @Override
            public void onDrmPrepared(MediaPlayer mp, final int status) throws Throwable {
                MediaPlayer2Impl.this.notifyDrmEvent(new DrmEventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.DrmEventCallback cb) {
                        int s = ((Integer) MediaPlayer2Impl.sPrepareDrmStatusMap.getOrDefault(Integer.valueOf(status), 3)).intValue();
                        cb.onDrmPrepared(MediaPlayer2Impl.this, src.getDSD(), s);
                    }
                });
            }
        });
    }

    public static final class DrmInfoImpl extends MediaPlayer2.DrmInfo {
        private Map<UUID, byte[]> mMapPssh;
        private UUID[] mSupportedSchemes;

        @Override
        public Map<UUID, byte[]> getPssh() {
            return this.mMapPssh;
        }

        @Override
        public List<UUID> getSupportedSchemes() {
            return Arrays.asList(this.mSupportedSchemes);
        }

        private DrmInfoImpl(Map<UUID, byte[]> pssh, UUID[] supportedSchemes) {
            this.mMapPssh = pssh;
            this.mSupportedSchemes = supportedSchemes;
        }

        private DrmInfoImpl(Parcel parcel) {
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl(" + parcel + ") size " + parcel.dataSize());
            int psshsize = parcel.readInt();
            byte[] pssh = new byte[psshsize];
            parcel.readByteArray(pssh);
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() PSSH: " + arrToHex(pssh));
            this.mMapPssh = parsePSSH(pssh, psshsize);
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() PSSH: " + this.mMapPssh);
            int supportedDRMsCount = parcel.readInt();
            this.mSupportedSchemes = new UUID[supportedDRMsCount];
            for (int i = 0; i < supportedDRMsCount; i++) {
                byte[] uuid = new byte[16];
                parcel.readByteArray(uuid);
                this.mSupportedSchemes[i] = bytesToUUID(uuid);
                Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() supportedScheme[" + i + "]: " + this.mSupportedSchemes[i]);
            }
            Log.v(MediaPlayer2Impl.TAG, "DrmInfoImpl() Parcel psshsize: " + psshsize + " supportedDRMsCount: " + supportedDRMsCount);
        }

        private DrmInfoImpl makeCopy() {
            return new DrmInfoImpl(this.mMapPssh, this.mSupportedSchemes);
        }

        private String arrToHex(byte[] bytes) {
            String out = "0x";
            for (byte b : bytes) {
                out = out + String.format("%02x", Byte.valueOf(b));
            }
            return out;
        }

        private UUID bytesToUUID(byte[] uuid) {
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb |= (((long) uuid[i]) & 255) << ((7 - i) * 8);
                lsb |= (((long) uuid[i + 8]) & 255) << (8 * (7 - i));
            }
            return new UUID(msb, lsb);
        }

        private Map<UUID, byte[]> parsePSSH(byte[] pssh, int psshsize) {
            int i;
            byte b;
            Map<UUID, byte[]> result = new HashMap<>();
            char c = 0;
            int numentries = 0;
            int numentries2 = psshsize;
            int len = 0;
            while (numentries2 > 0) {
                if (numentries2 < 16) {
                    Object[] objArr = new Object[2];
                    objArr[c] = Integer.valueOf(numentries2);
                    objArr[1] = Integer.valueOf(psshsize);
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse UUID: (%d < 16) pssh: %d", objArr));
                    return null;
                }
                UUID uuid = bytesToUUID(Arrays.copyOfRange(pssh, len, len + 16));
                int i2 = len + 16;
                int len2 = numentries2 - 16;
                if (len2 < 4) {
                    Object[] objArr2 = new Object[2];
                    objArr2[c] = Integer.valueOf(len2);
                    objArr2[1] = Integer.valueOf(psshsize);
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse datalen: (%d < 4) pssh: %d", objArr2));
                    return null;
                }
                byte[] subset = Arrays.copyOfRange(pssh, i2, i2 + 4);
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    i = ((subset[3] & 255) << 24) | ((subset[2] & 255) << 16) | ((subset[1] & 255) << 8);
                    b = subset[0];
                } else {
                    i = ((subset[0] & 255) << 24) | ((subset[1] & 255) << 16) | ((subset[2] & 255) << 8);
                    b = subset[3];
                }
                int datalen = i | (b & 255);
                int i3 = i2 + 4;
                int len3 = len2 - 4;
                if (len3 < datalen) {
                    Log.w(MediaPlayer2Impl.TAG, String.format("parsePSSH: len is too short to parse data: (%d < %d) pssh: %d", Integer.valueOf(len3), Integer.valueOf(datalen), Integer.valueOf(psshsize)));
                    return null;
                }
                byte[] data = Arrays.copyOfRange(pssh, i3, i3 + datalen);
                len = i3 + datalen;
                numentries2 = len3 - datalen;
                Log.v(MediaPlayer2Impl.TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d", Integer.valueOf(numentries), uuid, arrToHex(data), Integer.valueOf(psshsize)));
                numentries++;
                result.put(uuid, data);
                c = 0;
            }
            return result;
        }
    }

    public static final class NoDrmSchemeExceptionImpl extends MediaPlayer2.NoDrmSchemeException {
        public NoDrmSchemeExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningNetworkErrorExceptionImpl extends MediaPlayer2.ProvisioningNetworkErrorException {
        public ProvisioningNetworkErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final class ProvisioningServerErrorExceptionImpl extends MediaPlayer2.ProvisioningServerErrorException {
        public ProvisioningServerErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    private abstract class Task implements Runnable {
        private DataSourceDesc mDSD;
        private final int mMediaCallType;
        private final boolean mNeedToWaitForEventToComplete;

        abstract void process() throws MediaPlayer2.NoDrmSchemeException, IOException;

        Task(int mediaCallType, boolean needToWaitForEventToComplete) {
            this.mMediaCallType = mediaCallType;
            this.mNeedToWaitForEventToComplete = needToWaitForEventToComplete;
        }

        @Override
        public void run() throws Throwable {
            int status = 0;
            try {
                process();
            } catch (IOException e) {
                status = 4;
            } catch (IllegalArgumentException e2) {
                status = 2;
            } catch (IllegalStateException e3) {
                status = 1;
            } catch (SecurityException e4) {
                status = 3;
            } catch (Exception e5) {
                status = Integer.MIN_VALUE;
            }
            this.mDSD = MediaPlayer2Impl.this.getCurrentDataSource();
            if (!this.mNeedToWaitForEventToComplete || status != 0) {
                sendCompleteNotification(status);
                synchronized (MediaPlayer2Impl.this.mTaskLock) {
                    MediaPlayer2Impl.this.mCurrentTask = null;
                    MediaPlayer2Impl.this.processPendingTask_l();
                }
            }
        }

        private void sendCompleteNotification(final int status) throws Throwable {
            if (this.mMediaCallType != 1003) {
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback cb) {
                        cb.onCallCompleted(MediaPlayer2Impl.this, Task.this.mDSD, Task.this.mMediaCallType, status);
                    }
                });
            }
        }
    }

    private static class DataSourceError {
        final DataSourceDesc mDSD;
        final int mExtra;
        final int mWhat;

        DataSourceError(DataSourceDesc dsd, int what, int extra) {
            this.mDSD = dsd;
            this.mWhat = what;
            this.mExtra = extra;
        }
    }

    private class MediaPlayerSource {
        volatile DataSourceDesc mDSD;
        boolean mPlayPending;
        final MediaPlayer mPlayer = new MediaPlayer();
        final AtomicInteger mBufferedPercentage = new AtomicInteger(0);
        int mSourceState = 0;
        int mMp2State = 1001;
        int mBufferingState = 0;
        int mPlayerState = 0;

        MediaPlayerSource(DataSourceDesc dsd) {
            this.mDSD = dsd;
            MediaPlayer2Impl.this.setUpListeners(this);
        }

        DataSourceDesc getDSD() {
            return this.mDSD;
        }
    }

    private class MediaPlayerSourceQueue {
        AudioAttributesCompat mAudioAttributes;
        Integer mAudioSessionId;
        Integer mAuxEffect;
        Float mAuxEffectSendLevel;
        PlaybackParams mPlaybackParams;
        Surface mSurface;
        SyncParams mSyncParams;
        List<MediaPlayerSource> mQueue = new ArrayList();
        Float mVolume = Float.valueOf(1.0f);

        MediaPlayerSourceQueue() {
            this.mQueue.add(MediaPlayer2Impl.this.new MediaPlayerSource(null));
        }

        synchronized MediaPlayer getCurrentPlayer() {
            return this.mQueue.get(0).mPlayer;
        }

        synchronized MediaPlayerSource getFirst() {
            return this.mQueue.get(0);
        }

        synchronized void setFirst(DataSourceDesc dsd) throws IOException {
            if (this.mQueue.isEmpty()) {
                this.mQueue.add(0, MediaPlayer2Impl.this.new MediaPlayerSource(dsd));
            } else {
                this.mQueue.get(0).mDSD = dsd;
                MediaPlayer2Impl.this.setUpListeners(this.mQueue.get(0));
            }
            MediaPlayer2Impl.handleDataSource(this.mQueue.get(0));
        }

        synchronized DataSourceError setNext(DataSourceDesc dsd) {
            MediaPlayerSource src = MediaPlayer2Impl.this.new MediaPlayerSource(dsd);
            if (this.mQueue.isEmpty()) {
                this.mQueue.add(src);
                return prepareAt(0);
            }
            this.mQueue.add(1, src);
            return prepareAt(1);
        }

        synchronized DataSourceError setNextMultiple(List<DataSourceDesc> descs) {
            List<MediaPlayerSource> sources = new ArrayList<>();
            for (DataSourceDesc dsd : descs) {
                sources.add(MediaPlayer2Impl.this.new MediaPlayerSource(dsd));
            }
            if (this.mQueue.isEmpty()) {
                this.mQueue.addAll(sources);
                return prepareAt(0);
            }
            this.mQueue.addAll(1, sources);
            return prepareAt(1);
        }

        synchronized void play() {
            MediaPlayerSource src = this.mQueue.get(0);
            if (src.mSourceState == 2) {
                src.mPlayer.start();
                setMp2State(src.mPlayer, 1004);
            } else {
                throw new IllegalStateException();
            }
        }

        synchronized void prepare() {
            getCurrentPlayer().prepareAsync();
        }

        synchronized void release() {
            getCurrentPlayer().release();
        }

        synchronized void prepareAsync() {
            MediaPlayer mp = getCurrentPlayer();
            mp.prepareAsync();
            setBufferingState(mp, 2);
        }

        synchronized void pause() {
            MediaPlayer mp = getCurrentPlayer();
            mp.pause();
            setMp2State(mp, 1003);
        }

        synchronized long getCurrentPosition() {
            return getCurrentPlayer().getCurrentPosition();
        }

        synchronized long getDuration() {
            return getCurrentPlayer().getDuration();
        }

        synchronized long getBufferedPosition() {
            MediaPlayerSource src;
            src = this.mQueue.get(0);
            return (((long) src.mPlayer.getDuration()) * ((long) src.mBufferedPercentage.get())) / 100;
        }

        synchronized void setAudioAttributes(AudioAttributesCompat attributes) {
            this.mAudioAttributes = attributes;
            AudioAttributes attr = this.mAudioAttributes == null ? null : (AudioAttributes) this.mAudioAttributes.unwrap();
            getCurrentPlayer().setAudioAttributes(attr);
        }

        synchronized AudioAttributesCompat getAudioAttributes() {
            return this.mAudioAttributes;
        }

        synchronized DataSourceError onPrepared(MediaPlayer mp) {
            for (int i = 0; i < this.mQueue.size(); i++) {
                MediaPlayerSource src = this.mQueue.get(i);
                if (mp == src.mPlayer) {
                    if (i == 0) {
                        if (src.mPlayPending) {
                            src.mPlayPending = false;
                            src.mPlayer.start();
                            setMp2State(src.mPlayer, 1004);
                        } else {
                            setMp2State(src.mPlayer, 1002);
                        }
                    }
                    src.mSourceState = 2;
                    setBufferingState(src.mPlayer, 1);
                    return prepareAt(i + 1);
                }
            }
            return null;
        }

        synchronized DataSourceError onCompletion(MediaPlayer mp) {
            if (!this.mQueue.isEmpty() && mp == getCurrentPlayer()) {
                if (this.mQueue.size() == 1) {
                    setMp2State(mp, 1003);
                    final DataSourceDesc dsd = this.mQueue.get(0).getDSD();
                    MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                        @Override
                        public void notify(MediaPlayer2.EventCallback callback) {
                            callback.onInfo(MediaPlayer2Impl.this, dsd, 6, 0);
                        }
                    });
                    return null;
                }
                moveToNext();
            }
            return playCurrent();
        }

        synchronized void moveToNext() {
            MediaPlayerSource src1 = this.mQueue.remove(0);
            src1.mPlayer.release();
            if (this.mQueue.isEmpty()) {
                throw new IllegalStateException("player/source queue emptied");
            }
            final MediaPlayerSource src2 = this.mQueue.get(0);
            if (src1.mPlayerState != src2.mPlayerState) {
                MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                        cb.onPlayerStateChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src2.mPlayerState);
                    }
                });
            }
            MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                @Override
                public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                    cb.onCurrentDataSourceChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, src2.mDSD);
                }
            });
        }

        synchronized DataSourceError playCurrent() {
            DataSourceError err;
            err = null;
            final MediaPlayerSource src = this.mQueue.get(0);
            if (this.mSurface != null) {
                src.mPlayer.setSurface(this.mSurface);
            }
            if (this.mVolume != null) {
                src.mPlayer.setVolume(this.mVolume.floatValue(), this.mVolume.floatValue());
            }
            if (this.mAudioAttributes != null) {
                src.mPlayer.setAudioAttributes((AudioAttributes) this.mAudioAttributes.unwrap());
            }
            if (this.mAuxEffect != null) {
                src.mPlayer.attachAuxEffect(this.mAuxEffect.intValue());
            }
            if (this.mAuxEffectSendLevel != null) {
                src.mPlayer.setAuxEffectSendLevel(this.mAuxEffectSendLevel.floatValue());
            }
            if (this.mSyncParams != null) {
                src.mPlayer.setSyncParams(this.mSyncParams);
            }
            if (this.mPlaybackParams != null) {
                src.mPlayer.setPlaybackParams(this.mPlaybackParams);
            }
            if (src.mSourceState == 2) {
                src.mPlayer.start();
                setMp2State(src.mPlayer, 1004);
                MediaPlayer2Impl.this.notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2.EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(), 2, 0);
                    }
                });
            } else {
                if (src.mSourceState == 0) {
                    err = prepareAt(0);
                }
                src.mPlayPending = true;
            }
            return err;
        }

        synchronized void onError(MediaPlayer mp) {
            setMp2State(mp, MediaPlayer2.MEDIAPLAYER2_STATE_ERROR);
            setBufferingState(mp, 0);
        }

        synchronized DataSourceError prepareAt(int n) {
            if (n < this.mQueue.size() && this.mQueue.get(n).mSourceState == 0 && (n == 0 || getPlayerState() != 0)) {
                MediaPlayerSource src = this.mQueue.get(n);
                try {
                    if (this.mAudioSessionId != null) {
                        src.mPlayer.setAudioSessionId(this.mAudioSessionId.intValue());
                    }
                    src.mSourceState = 1;
                    MediaPlayer2Impl.handleDataSource(src);
                    src.mPlayer.prepareAsync();
                    return null;
                } catch (Exception e) {
                    DataSourceDesc dsd = src.getDSD();
                    setMp2State(src.mPlayer, MediaPlayer2.MEDIAPLAYER2_STATE_ERROR);
                    return new DataSourceError(dsd, 1, MediaPlayer2.MEDIA_ERROR_UNSUPPORTED);
                }
            }
            return null;
        }

        synchronized void skipToNext() {
            if (this.mQueue.size() <= 1) {
                throw new IllegalStateException("No next source available");
            }
            MediaPlayerSource src = this.mQueue.get(0);
            moveToNext();
            if (src.mPlayerState == 2 || src.mPlayPending) {
                playCurrent();
            }
        }

        synchronized void setLooping(boolean loop) {
            getCurrentPlayer().setLooping(loop);
        }

        synchronized void setPlaybackParams(PlaybackParams playbackParams) {
            getCurrentPlayer().setPlaybackParams(playbackParams);
            this.mPlaybackParams = playbackParams;
        }

        synchronized float getVolume() {
            return this.mVolume.floatValue();
        }

        synchronized void setVolume(float volume) {
            this.mVolume = Float.valueOf(volume);
            getCurrentPlayer().setVolume(volume, volume);
        }

        synchronized void setSurface(Surface surface) {
            this.mSurface = surface;
            getCurrentPlayer().setSurface(surface);
        }

        synchronized int getVideoWidth() {
            return getCurrentPlayer().getVideoWidth();
        }

        synchronized int getVideoHeight() {
            return getCurrentPlayer().getVideoHeight();
        }

        synchronized PersistableBundle getMetrics() {
            return getCurrentPlayer().getMetrics();
        }

        synchronized PlaybackParams getPlaybackParams() {
            return getCurrentPlayer().getPlaybackParams();
        }

        synchronized void setSyncParams(SyncParams params) {
            getCurrentPlayer().setSyncParams(params);
            this.mSyncParams = params;
        }

        synchronized SyncParams getSyncParams() {
            return getCurrentPlayer().getSyncParams();
        }

        synchronized void seekTo(long msec, int mode) {
            getCurrentPlayer().seekTo(msec, mode);
        }

        synchronized void reset() {
            MediaPlayerSource src = this.mQueue.get(0);
            src.mPlayer.reset();
            src.mBufferedPercentage.set(0);
            this.mVolume = Float.valueOf(1.0f);
            this.mSurface = null;
            this.mAuxEffect = null;
            this.mAuxEffectSendLevel = null;
            this.mAudioAttributes = null;
            this.mAudioSessionId = null;
            this.mSyncParams = null;
            this.mPlaybackParams = null;
            setMp2State(src.mPlayer, 1001);
            setBufferingState(src.mPlayer, 0);
        }

        synchronized MediaTimestamp2 getTimestamp() {
            MediaTimestamp t;
            t = getCurrentPlayer().getTimestamp();
            return t == null ? null : new MediaTimestamp2(t);
        }

        synchronized void setAudioSessionId(int sessionId) {
            getCurrentPlayer().setAudioSessionId(sessionId);
        }

        synchronized int getAudioSessionId() {
            return getCurrentPlayer().getAudioSessionId();
        }

        synchronized void attachAuxEffect(int effectId) {
            getCurrentPlayer().attachAuxEffect(effectId);
            this.mAuxEffect = Integer.valueOf(effectId);
        }

        synchronized void setAuxEffectSendLevel(float level) {
            getCurrentPlayer().setAuxEffectSendLevel(level);
            this.mAuxEffectSendLevel = Float.valueOf(level);
        }

        synchronized MediaPlayer.TrackInfo[] getTrackInfo() {
            return getCurrentPlayer().getTrackInfo();
        }

        synchronized int getSelectedTrack(int trackType) {
            return getCurrentPlayer().getSelectedTrack(trackType);
        }

        synchronized void selectTrack(int index) {
            getCurrentPlayer().selectTrack(index);
        }

        synchronized void deselectTrack(int index) {
            getCurrentPlayer().deselectTrack(index);
        }

        synchronized MediaPlayer.DrmInfo getDrmInfo() {
            return getCurrentPlayer().getDrmInfo();
        }

        synchronized void prepareDrm(UUID uuid) throws UnsupportedSchemeException, MediaPlayer.ProvisioningNetworkErrorException, ResourceBusyException, MediaPlayer.ProvisioningServerErrorException {
            getCurrentPlayer().prepareDrm(uuid);
        }

        synchronized void releaseDrm() throws MediaPlayer.NoDrmSchemeException {
            getCurrentPlayer().stop();
            getCurrentPlayer().releaseDrm();
        }

        synchronized byte[] provideKeyResponse(byte[] keySetId, byte[] response) throws DeniedByServerException, MediaPlayer.NoDrmSchemeException {
            return getCurrentPlayer().provideKeyResponse(keySetId, response);
        }

        synchronized void restoreKeys(byte[] keySetId) throws MediaPlayer.NoDrmSchemeException {
            getCurrentPlayer().restoreKeys(keySetId);
        }

        synchronized String getDrmPropertyString(String propertyName) throws MediaPlayer.NoDrmSchemeException {
            return getCurrentPlayer().getDrmPropertyString(propertyName);
        }

        synchronized void setDrmPropertyString(String propertyName, String value) throws MediaPlayer.NoDrmSchemeException {
            getCurrentPlayer().setDrmPropertyString(propertyName, value);
        }

        synchronized void setOnDrmConfigHelper(MediaPlayer.OnDrmConfigHelper onDrmConfigHelper) {
            getCurrentPlayer().setOnDrmConfigHelper(onDrmConfigHelper);
        }

        synchronized MediaDrm.KeyRequest getKeyRequest(byte[] keySetId, byte[] initData, String mimeType, int keyType, Map<String, String> optionalParameters) throws MediaPlayer.NoDrmSchemeException {
            return getCurrentPlayer().getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
        }

        synchronized void setMp2State(MediaPlayer mp, int mp2State) {
            for (MediaPlayerSource src : this.mQueue) {
                if (src.mPlayer == mp) {
                    if (src.mMp2State == mp2State) {
                        return;
                    }
                    src.mMp2State = mp2State;
                    final int playerState = ((Integer) MediaPlayer2Impl.sStateMap.get(Integer.valueOf(mp2State))).intValue();
                    if (src.mPlayerState == playerState) {
                        return;
                    }
                    src.mPlayerState = playerState;
                    MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                        @Override
                        public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                            cb.onPlayerStateChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, playerState);
                        }
                    });
                    return;
                }
            }
        }

        synchronized void setBufferingState(MediaPlayer mp, final int state) {
            for (final MediaPlayerSource src : this.mQueue) {
                if (src.mPlayer == mp) {
                    if (src.mBufferingState == state) {
                        return;
                    }
                    src.mBufferingState = state;
                    MediaPlayer2Impl.this.notifyPlayerEvent(new PlayerEventNotifier() {
                        @Override
                        public void notify(BaseMediaPlayer.PlayerEventCallback cb) {
                            DataSourceDesc dsd = src.getDSD();
                            cb.onBufferingStateChanged(MediaPlayer2Impl.this.mBaseMediaPlayerImpl, dsd, state);
                        }
                    });
                    return;
                }
            }
        }

        synchronized int getMediaPlayer2State() {
            return this.mQueue.get(0).mMp2State;
        }

        synchronized int getBufferingState() {
            return this.mQueue.get(0).mBufferingState;
        }

        synchronized int getPlayerState() {
            return this.mQueue.get(0).mPlayerState;
        }

        synchronized MediaPlayerSource getSourceForPlayer(MediaPlayer mp) {
            for (MediaPlayerSource src : this.mQueue) {
                if (src.mPlayer == mp) {
                    return src;
                }
            }
            return null;
        }
    }

    private class BaseMediaPlayerImpl extends BaseMediaPlayer {
        private BaseMediaPlayerImpl() {
        }

        @Override
        public void play() {
            MediaPlayer2Impl.this.play();
        }

        @Override
        public void prepare() {
            MediaPlayer2Impl.this.prepare();
        }

        @Override
        public void pause() {
            MediaPlayer2Impl.this.pause();
        }

        @Override
        public void reset() {
            MediaPlayer2Impl.this.reset();
        }

        @Override
        public void skipToNext() {
            MediaPlayer2Impl.this.skipToNext();
        }

        @Override
        public void seekTo(long pos) {
            MediaPlayer2Impl.this.seekTo(pos);
        }

        @Override
        public long getCurrentPosition() {
            return MediaPlayer2Impl.this.getCurrentPosition();
        }

        @Override
        public long getDuration() {
            return MediaPlayer2Impl.this.getDuration();
        }

        @Override
        public long getBufferedPosition() {
            return MediaPlayer2Impl.this.getBufferedPosition();
        }

        @Override
        public int getPlayerState() {
            return MediaPlayer2Impl.this.getPlayerState();
        }

        @Override
        public int getBufferingState() {
            return MediaPlayer2Impl.this.getBufferingState();
        }

        @Override
        public void setAudioAttributes(AudioAttributesCompat attributes) {
            MediaPlayer2Impl.this.setAudioAttributes(attributes);
        }

        @Override
        public AudioAttributesCompat getAudioAttributes() {
            return MediaPlayer2Impl.this.getAudioAttributes();
        }

        @Override
        public void setDataSource(DataSourceDesc dsd) {
            MediaPlayer2Impl.this.setDataSource(dsd);
        }

        @Override
        public void setNextDataSource(DataSourceDesc dsd) {
            MediaPlayer2Impl.this.setNextDataSource(dsd);
        }

        @Override
        public void setNextDataSources(List<DataSourceDesc> dsds) {
            MediaPlayer2Impl.this.setNextDataSources(dsds);
        }

        @Override
        public DataSourceDesc getCurrentDataSource() {
            return MediaPlayer2Impl.this.getCurrentDataSource();
        }

        @Override
        public void loopCurrent(boolean loop) {
            MediaPlayer2Impl.this.loopCurrent(loop);
        }

        @Override
        public void setPlaybackSpeed(float speed) {
            MediaPlayer2Impl.this.setPlaybackParams(new PlaybackParams2.Builder(MediaPlayer2Impl.this.getPlaybackParams().getPlaybackParams()).setSpeed(speed).build());
        }

        @Override
        public float getPlaybackSpeed() {
            return MediaPlayer2Impl.this.getPlaybackParams().getSpeed().floatValue();
        }

        @Override
        public void setPlayerVolume(float volume) {
            MediaPlayer2Impl.this.setPlayerVolume(volume);
        }

        @Override
        public float getPlayerVolume() {
            return MediaPlayer2Impl.this.getPlayerVolume();
        }

        @Override
        public void registerPlayerEventCallback(Executor e, BaseMediaPlayer.PlayerEventCallback cb) {
            MediaPlayer2Impl.this.registerPlayerEventCallback(e, cb);
        }

        @Override
        public void unregisterPlayerEventCallback(BaseMediaPlayer.PlayerEventCallback cb) {
            MediaPlayer2Impl.this.unregisterPlayerEventCallback(cb);
        }

        @Override
        public void close() throws Exception {
            MediaPlayer2Impl.this.close();
        }
    }
}
