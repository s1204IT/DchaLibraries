package android.support.v4.media;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.BaseMediaPlayer;
import android.support.v4.media.MediaController2;
import android.support.v4.media.MediaMetadata2;
import android.support.v4.media.MediaPlaylistAgent;
import android.support.v4.media.MediaSession2;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.ObjectsCompat;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

@TargetApi(19)
class MediaSession2ImplBase implements MediaSession2.SupportLibraryImpl {
    private final AudioFocusHandler mAudioFocusHandler;
    private final AudioManager mAudioManager;
    private final MediaSession2.SessionCallback mCallback;
    private final Executor mCallbackExecutor;
    private final Context mContext;

    @GuardedBy("mLock")
    private MediaSession2.OnDataSourceMissingHelper mDsmHelper;
    private final Handler mHandler;
    private final MediaSession2 mInstance;

    @GuardedBy("mLock")
    private MediaController2.PlaybackInfo mPlaybackInfo;

    @GuardedBy("mLock")
    private BaseMediaPlayer mPlayer;
    private final BaseMediaPlayer.PlayerEventCallback mPlayerEventCallback;

    @GuardedBy("mLock")
    private MediaPlaylistAgent mPlaylistAgent;
    private final MediaPlaylistAgent.PlaylistEventCallback mPlaylistEventCallback;
    private final MediaSession2Stub mSession2Stub;
    private final PendingIntent mSessionActivity;
    private final MediaSessionCompat mSessionCompat;
    private final MediaSessionLegacyStub mSessionLegacyStub;

    @GuardedBy("mLock")
    private SessionPlaylistAgentImplBase mSessionPlaylistAgent;
    private final SessionToken2 mSessionToken;

    @GuardedBy("mLock")
    private VolumeProviderCompat mVolumeProvider;
    static final String TAG = "MS2ImplBase";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);
    final Object mLock = new Object();
    private final HandlerThread mHandlerThread = new HandlerThread("MediaController2_Thread");

    @FunctionalInterface
    interface NotifyRunnable {
        void run(MediaSession2.ControllerCb controllerCb) throws RemoteException;
    }

    MediaSession2ImplBase(MediaSession2 instance, Context context, String id, BaseMediaPlayer player, MediaPlaylistAgent playlistAgent, VolumeProviderCompat volumeProvider, PendingIntent sessionActivity, Executor callbackExecutor, MediaSession2.SessionCallback callback) throws Throwable {
        this.mContext = context;
        this.mInstance = instance;
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper());
        this.mSession2Stub = new MediaSession2Stub(this);
        this.mSessionLegacyStub = new MediaSessionLegacyStub(this);
        this.mSessionActivity = sessionActivity;
        this.mCallback = callback;
        this.mCallbackExecutor = callbackExecutor;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mPlayerEventCallback = new MyPlayerEventCallback();
        this.mPlaylistEventCallback = new MyPlaylistEventCallback();
        this.mAudioFocusHandler = new AudioFocusHandler(context, getInstance());
        String libraryService = getServiceName(context, MediaLibraryService2.SERVICE_INTERFACE, id);
        String sessionService = getServiceName(context, MediaSessionService2.SERVICE_INTERFACE, id);
        if (sessionService != null && libraryService != null) {
            throw new IllegalArgumentException("Ambiguous session type. Multiple session services define the same id=" + id);
        }
        if (libraryService != null) {
            this.mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(), 2, context.getPackageName(), libraryService, id, this.mSession2Stub));
        } else if (sessionService != null) {
            this.mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(), 1, context.getPackageName(), sessionService, id, this.mSession2Stub));
        } else {
            this.mSessionToken = new SessionToken2(new SessionToken2ImplBase(Process.myUid(), 0, context.getPackageName(), null, id, this.mSession2Stub));
        }
        this.mSessionCompat = new MediaSessionCompat(context, id, this.mSessionToken);
        this.mSessionCompat.setCallback(this.mSessionLegacyStub, this.mHandler);
        this.mSessionCompat.setSessionActivity(sessionActivity);
        updatePlayer(player, playlistAgent, volumeProvider);
    }

    @Override
    public void updatePlayer(@NonNull BaseMediaPlayer player, @Nullable MediaPlaylistAgent playlistAgent, @Nullable VolumeProviderCompat volumeProvider) throws Throwable {
        boolean hasPlaybackInfoChanged;
        if (player == null) {
            throw new IllegalArgumentException("player shouldn't be null");
        }
        final MediaController2.PlaybackInfo info = createPlaybackInfo(volumeProvider, player.getAudioAttributes());
        synchronized (this.mLock) {
            MediaPlaylistAgent oldAgent = null;
            try {
                boolean hasPlayerChanged = this.mPlayer != player;
                try {
                    boolean hasAgentChanged = this.mPlaylistAgent != playlistAgent;
                    try {
                        hasPlaybackInfoChanged = this.mPlaybackInfo != info;
                        BaseMediaPlayer oldPlayer = this.mPlayer;
                        try {
                            oldAgent = this.mPlaylistAgent;
                            this.mPlayer = player;
                            if (playlistAgent == null) {
                                this.mSessionPlaylistAgent = new SessionPlaylistAgentImplBase(this, this.mPlayer);
                                if (this.mDsmHelper != null) {
                                    this.mSessionPlaylistAgent.setOnDataSourceMissingHelper(this.mDsmHelper);
                                }
                                playlistAgent = this.mSessionPlaylistAgent;
                            }
                            this.mPlaylistAgent = playlistAgent;
                            this.mVolumeProvider = volumeProvider;
                            this.mPlaybackInfo = info;
                            if (volumeProvider == null) {
                                int stream = getLegacyStreamType(player.getAudioAttributes());
                                this.mSessionCompat.setPlaybackToLocal(stream);
                            }
                            if (player != oldPlayer) {
                                player.registerPlayerEventCallback(this.mCallbackExecutor, this.mPlayerEventCallback);
                                if (oldPlayer != null) {
                                    oldPlayer.unregisterPlayerEventCallback(this.mPlayerEventCallback);
                                }
                            }
                            if (playlistAgent != oldAgent) {
                                playlistAgent.registerPlaylistEventCallback(this.mCallbackExecutor, this.mPlaylistEventCallback);
                                if (oldAgent != null) {
                                    oldAgent.unregisterPlaylistEventCallback(this.mPlaylistEventCallback);
                                }
                            }
                            if (oldPlayer != null) {
                                if (hasAgentChanged) {
                                    notifyAgentUpdatedNotLocked(oldAgent);
                                }
                                if (hasPlayerChanged) {
                                    notifyPlayerUpdatedNotLocked(oldPlayer);
                                }
                                if (hasPlaybackInfoChanged) {
                                    notifyToAllControllers(new NotifyRunnable() {
                                        @Override
                                        public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                                            callback.onPlaybackInfoChanged(info);
                                        }
                                    });
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    throw th;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        while (true) {
                            throw th;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    hasPlaybackInfoChanged = false;
                    while (true) {
                        throw th;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                hasPlaybackInfoChanged = false;
            }
        }
    }

    private MediaController2.PlaybackInfo createPlaybackInfo(VolumeProviderCompat volumeProvider, AudioAttributesCompat attrs) {
        if (volumeProvider == null) {
            int stream = getLegacyStreamType(attrs);
            int controlType = 2;
            if (Build.VERSION.SDK_INT >= 21 && this.mAudioManager.isVolumeFixed()) {
                controlType = 0;
            }
            MediaController2.PlaybackInfo info = MediaController2.PlaybackInfo.createPlaybackInfo(1, attrs, controlType, this.mAudioManager.getStreamMaxVolume(stream), this.mAudioManager.getStreamVolume(stream));
            return info;
        }
        MediaController2.PlaybackInfo info2 = MediaController2.PlaybackInfo.createPlaybackInfo(2, attrs, volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume(), volumeProvider.getCurrentVolume());
        return info2;
    }

    private int getLegacyStreamType(@Nullable AudioAttributesCompat attrs) {
        int stream;
        if (attrs == null || (stream = attrs.getLegacyStreamType()) == Integer.MIN_VALUE) {
            return 3;
        }
        return stream;
    }

    @Override
    public void close() {
        synchronized (this.mLock) {
            if (this.mPlayer == null) {
                return;
            }
            this.mAudioFocusHandler.close();
            this.mPlayer.unregisterPlayerEventCallback(this.mPlayerEventCallback);
            this.mPlayer = null;
            this.mSessionCompat.release();
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onDisconnected();
                }
            });
            this.mHandler.removeCallbacksAndMessages(null);
            if (this.mHandlerThread.isAlive()) {
                if (Build.VERSION.SDK_INT >= 18) {
                    this.mHandlerThread.quitSafely();
                } else {
                    this.mHandlerThread.quit();
                }
            }
        }
    }

    @Override
    @NonNull
    public BaseMediaPlayer getPlayer() {
        BaseMediaPlayer baseMediaPlayer;
        synchronized (this.mLock) {
            baseMediaPlayer = this.mPlayer;
        }
        return baseMediaPlayer;
    }

    @Override
    @NonNull
    public MediaPlaylistAgent getPlaylistAgent() {
        MediaPlaylistAgent mediaPlaylistAgent;
        synchronized (this.mLock) {
            mediaPlaylistAgent = this.mPlaylistAgent;
        }
        return mediaPlaylistAgent;
    }

    @Override
    @Nullable
    public VolumeProviderCompat getVolumeProvider() {
        VolumeProviderCompat volumeProviderCompat;
        synchronized (this.mLock) {
            volumeProviderCompat = this.mVolumeProvider;
        }
        return volumeProviderCompat;
    }

    @Override
    @NonNull
    public SessionToken2 getToken() {
        return this.mSessionToken;
    }

    @Override
    @NonNull
    public List<MediaSession2.ControllerInfo> getConnectedControllers() {
        return this.mSession2Stub.getConnectedControllers();
    }

    @Override
    public void setCustomLayout(@NonNull MediaSession2.ControllerInfo controller, @NonNull final List<MediaSession2.CommandButton> layout) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (layout == null) {
            throw new IllegalArgumentException("layout shouldn't be null");
        }
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onCustomLayoutChanged(layout);
            }
        });
    }

    @Override
    public void setAllowedCommands(@NonNull MediaSession2.ControllerInfo controller, @NonNull final SessionCommandGroup2 commands) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (commands == null) {
            throw new IllegalArgumentException("commands shouldn't be null");
        }
        this.mSession2Stub.setAllowedCommands(controller, commands);
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onAllowedCommandsChanged(commands);
            }
        });
    }

    @Override
    public void sendCustomCommand(@NonNull final SessionCommand2 command, @Nullable final Bundle args) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onCustomCommand(command, args, null);
            }
        });
    }

    @Override
    public void sendCustomCommand(@NonNull MediaSession2.ControllerInfo controller, @NonNull final SessionCommand2 command, @Nullable final Bundle args, @Nullable final ResultReceiver receiver) {
        if (controller == null) {
            throw new IllegalArgumentException("controller shouldn't be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onCustomCommand(command, args, receiver);
            }
        });
    }

    @Override
    public void play() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        if (this.mAudioFocusHandler.onPlayRequested()) {
                            player.play();
                            return;
                        } else {
                            Log.w(TAG, "play() wouldn't be called because of the failure in audio focus");
                            return;
                        }
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void pause() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        if (this.mAudioFocusHandler.onPauseRequested()) {
                            player.pause();
                            return;
                        } else {
                            Log.w(TAG, "pause() wouldn't be called of the failure in audio focus");
                            return;
                        }
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void reset() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        player.reset();
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void prepare() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        player.prepare();
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void seekTo(long pos) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        player.seekTo(pos);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void skipForward() {
    }

    @Override
    public void skipBackward() {
    }

    @Override
    public void notifyError(final int errorCode, @Nullable final Bundle extras) {
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onError(errorCode, extras);
            }
        });
    }

    @Override
    public void notifyRoutesInfoChanged(@NonNull MediaSession2.ControllerInfo controller, @Nullable final List<Bundle> routes) {
        notifyToController(controller, new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onRoutesInfoChanged(routes);
            }
        });
    }

    @Override
    public int getPlayerState() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        return player.getPlayerState();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return 3;
                    }
                    return 3;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public long getCurrentPosition() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        return player.getCurrentPosition();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return -1L;
                    }
                    return -1L;
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

    @Override
    public long getDuration() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        return player.getDuration();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return -1L;
                    }
                    return -1L;
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

    @Override
    public long getBufferedPosition() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        return player.getBufferedPosition();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return -1L;
                    }
                    return -1L;
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

    @Override
    public int getBufferingState() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        return player.getBufferingState();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return 0;
                    }
                    return 0;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public float getPlaybackSpeed() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        return player.getPlaybackSpeed();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return 1.0f;
                    }
                    return 1.0f;
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

    @Override
    public void setPlaybackSpeed(float speed) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    BaseMediaPlayer player = this.mPlayer;
                    if (player != null) {
                        player.setPlaybackSpeed(speed);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void setOnDataSourceMissingHelper(@NonNull MediaSession2.OnDataSourceMissingHelper helper) {
        if (helper == null) {
            throw new IllegalArgumentException("helper shouldn't be null");
        }
        synchronized (this.mLock) {
            this.mDsmHelper = helper;
            if (this.mSessionPlaylistAgent != null) {
                this.mSessionPlaylistAgent.setOnDataSourceMissingHelper(helper);
            }
        }
    }

    @Override
    public void clearOnDataSourceMissingHelper() {
        synchronized (this.mLock) {
            this.mDsmHelper = null;
            if (this.mSessionPlaylistAgent != null) {
                this.mSessionPlaylistAgent.clearOnDataSourceMissingHelper();
            }
        }
    }

    @Override
    public List<MediaItem2> getPlaylist() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        return agent.getPlaylist();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                    }
                    return null;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) throws Throwable {
        if (list == null) {
            throw new IllegalArgumentException("list shouldn't be null");
        }
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.setPlaylist(list, metadata);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) throws Throwable {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.skipToPlaylistItem(item);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void skipToPreviousItem() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.skipToPreviousItem();
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void skipToNextItem() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.skipToNextItem();
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public MediaMetadata2 getPlaylistMetadata() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        return agent.getPlaylistMetadata();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                    }
                    return null;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) throws Throwable {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.addPlaylistItem(index, item);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) throws Throwable {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.removePlaylistItem(item);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) throws Throwable {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.replacePlaylistItem(index, item);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public MediaItem2 getCurrentMediaItem() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        return agent.getCurrentMediaItem();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                    }
                    return null;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.updatePlaylistMetadata(metadata);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public int getRepeatMode() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        return agent.getRepeatMode();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return 0;
                    }
                    return 0;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void setRepeatMode(int repeatMode) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.setRepeatMode(repeatMode);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    public int getShuffleMode() throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        return agent.getShuffleMode();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
                        return 0;
                    }
                    return 0;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public void setShuffleMode(int shuffleMode) throws Throwable {
        synchronized (this.mLock) {
            try {
                try {
                    MediaPlaylistAgent agent = this.mPlaylistAgent;
                    if (agent != null) {
                        agent.setShuffleMode(shuffleMode);
                    } else if (DEBUG) {
                        Log.d(TAG, "API calls after the close()", new IllegalStateException());
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

    @Override
    @NonNull
    public MediaSession2 getInstance() {
        return this.mInstance;
    }

    @Override
    @NonNull
    public IBinder getSessionBinder() {
        return this.mSession2Stub.asBinder();
    }

    @Override
    public Context getContext() {
        return this.mContext;
    }

    @Override
    public Executor getCallbackExecutor() {
        return this.mCallbackExecutor;
    }

    @Override
    public MediaSession2.SessionCallback getCallback() {
        return this.mCallback;
    }

    @Override
    public MediaSessionCompat getSessionCompat() {
        return this.mSessionCompat;
    }

    @Override
    public AudioFocusHandler getAudioFocusHandler() {
        return this.mAudioFocusHandler;
    }

    @Override
    public boolean isClosed() {
        return !this.mHandlerThread.isAlive();
    }

    @Override
    public PlaybackStateCompat getPlaybackStateCompat() {
        PlaybackStateCompat playbackStateCompatBuild;
        synchronized (this.mLock) {
            int state = MediaUtils2.convertToPlaybackStateCompatState(getPlayerState(), getBufferingState());
            playbackStateCompatBuild = new PlaybackStateCompat.Builder().setState(state, getCurrentPosition(), getPlaybackSpeed()).setActions(3670015L).setBufferedPosition(getBufferedPosition()).build();
        }
        return playbackStateCompatBuild;
    }

    @Override
    public MediaController2.PlaybackInfo getPlaybackInfo() {
        MediaController2.PlaybackInfo playbackInfo;
        synchronized (this.mLock) {
            playbackInfo = this.mPlaybackInfo;
        }
        return playbackInfo;
    }

    @Override
    public PendingIntent getSessionActivity() {
        return this.mSessionActivity;
    }

    private static String getServiceName(Context context, String serviceAction, String id) {
        PackageManager manager = context.getPackageManager();
        Intent serviceIntent = new Intent(serviceAction);
        serviceIntent.setPackage(context.getPackageName());
        List<ResolveInfo> services = manager.queryIntentServices(serviceIntent, 128);
        String serviceName = null;
        if (services != null) {
            for (int i = 0; i < services.size(); i++) {
                String serviceId = SessionToken2.getSessionId(services.get(i));
                if (serviceId != null && TextUtils.equals(id, serviceId) && services.get(i).serviceInfo != null) {
                    if (serviceName != null) {
                        throw new IllegalArgumentException("Ambiguous session type. Multiple session services define the same id=" + id);
                    }
                    serviceName = services.get(i).serviceInfo.name;
                }
            }
        }
        return serviceName;
    }

    private void notifyAgentUpdatedNotLocked(MediaPlaylistAgent oldAgent) throws Throwable {
        List<MediaItem2> oldPlaylist = oldAgent.getPlaylist();
        final List<MediaItem2> newPlaylist = getPlaylist();
        if (!ObjectsCompat.equals(oldPlaylist, newPlaylist)) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(newPlaylist, MediaSession2ImplBase.this.getPlaylistMetadata());
                }
            });
        } else {
            MediaMetadata2 oldMetadata = oldAgent.getPlaylistMetadata();
            final MediaMetadata2 newMetadata = getPlaylistMetadata();
            if (!ObjectsCompat.equals(oldMetadata, newMetadata)) {
                notifyToAllControllers(new NotifyRunnable() {
                    @Override
                    public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                        callback.onPlaylistMetadataChanged(newMetadata);
                    }
                });
            }
        }
        MediaItem2 oldCurrentItem = oldAgent.getCurrentMediaItem();
        final MediaItem2 newCurrentItem = getCurrentMediaItem();
        if (!ObjectsCompat.equals(oldCurrentItem, newCurrentItem)) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onCurrentMediaItemChanged(newCurrentItem);
                }
            });
        }
        final int repeatMode = getRepeatMode();
        if (oldAgent.getRepeatMode() != repeatMode) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
        final int shuffleMode = getShuffleMode();
        if (oldAgent.getShuffleMode() != shuffleMode) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }
    }

    private void notifyPlayerUpdatedNotLocked(BaseMediaPlayer oldPlayer) throws Throwable {
        final long currentTimeMs = SystemClock.elapsedRealtime();
        final long positionMs = getCurrentPosition();
        final int playerState = getPlayerState();
        notifyToAllControllers(new NotifyRunnable() {
            @Override
            public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                callback.onPlayerStateChanged(currentTimeMs, positionMs, playerState);
            }
        });
        final MediaItem2 item = getCurrentMediaItem();
        if (item != null) {
            final int bufferingState = getBufferingState();
            final long bufferedPositionMs = getBufferedPosition();
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onBufferingStateChanged(item, bufferingState, bufferedPositionMs);
                }
            });
        }
        final float speed = getPlaybackSpeed();
        if (speed != oldPlayer.getPlaybackSpeed()) {
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onPlaybackSpeedChanged(currentTimeMs, positionMs, speed);
                }
            });
        }
    }

    public void notifyPlaylistChangedOnExecutor(MediaPlaylistAgent playlistAgent, final List<MediaItem2> list, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onPlaylistChanged(this.mInstance, playlistAgent, list, metadata);
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onPlaylistChanged(list, metadata);
                }
            });
        }
    }

    public void notifyPlaylistMetadataChangedOnExecutor(MediaPlaylistAgent playlistAgent, final MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onPlaylistMetadataChanged(this.mInstance, playlistAgent, metadata);
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onPlaylistMetadataChanged(metadata);
                }
            });
        }
    }

    public void notifyRepeatModeChangedOnExecutor(MediaPlaylistAgent playlistAgent, final int repeatMode) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onRepeatModeChanged(this.mInstance, playlistAgent, repeatMode);
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onRepeatModeChanged(repeatMode);
                }
            });
        }
    }

    public void notifyShuffleModeChangedOnExecutor(MediaPlaylistAgent playlistAgent, final int shuffleMode) {
        synchronized (this.mLock) {
            if (playlistAgent != this.mPlaylistAgent) {
                return;
            }
            this.mCallback.onShuffleModeChanged(this.mInstance, playlistAgent, shuffleMode);
            notifyToAllControllers(new NotifyRunnable() {
                @Override
                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                    callback.onShuffleModeChanged(shuffleMode);
                }
            });
        }
    }

    void notifyToController(@NonNull final MediaSession2.ControllerInfo controller, @NonNull NotifyRunnable runnable) {
        if (controller == null) {
            return;
        }
        try {
            runnable.run(controller.getControllerCb());
        } catch (DeadObjectException e) {
            if (DEBUG) {
                Log.d(TAG, controller.toString() + " is gone", e);
            }
            this.mSession2Stub.removeControllerInfo(controller);
            this.mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    MediaSession2ImplBase.this.mCallback.onDisconnected(MediaSession2ImplBase.this.getInstance(), controller);
                }
            });
        } catch (RemoteException e2) {
            Log.w(TAG, "Exception in " + controller.toString(), e2);
        }
    }

    void notifyToAllControllers(@NonNull NotifyRunnable runnable) {
        List<MediaSession2.ControllerInfo> controllers = getConnectedControllers();
        for (int i = 0; i < controllers.size(); i++) {
            notifyToController(controllers.get(i), runnable);
        }
    }

    private static class MyPlayerEventCallback extends BaseMediaPlayer.PlayerEventCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        private MyPlayerEventCallback(MediaSession2ImplBase session) {
            this.mSession = new WeakReference<>(session);
        }

        @Override
        public void onCurrentDataSourceChanged(final BaseMediaPlayer player, final DataSourceDesc dsd) {
            final MediaSession2ImplBase session = getSession();
            if (session == null) {
                return;
            }
            session.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    final MediaItem2 item;
                    if (dsd != null) {
                        item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                        if (item == null) {
                            Log.w(MediaSession2ImplBase.TAG, "Cannot obtain media item from the dsd=" + dsd);
                            return;
                        }
                    } else {
                        item = null;
                    }
                    session.getCallback().onCurrentMediaItemChanged(session.getInstance(), player, item);
                    session.notifyToAllControllers(new NotifyRunnable() {
                        @Override
                        public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                            callback.onCurrentMediaItemChanged(item);
                        }
                    });
                }
            });
        }

        @Override
        public void onMediaPrepared(final BaseMediaPlayer mpb, final DataSourceDesc dsd) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || dsd == null) {
                return;
            }
            session.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() throws Throwable {
                    MediaMetadata2 metadata;
                    MediaItem2 item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                    if (item == null) {
                        return;
                    }
                    if (item.equals(session.getCurrentMediaItem())) {
                        long duration = session.getDuration();
                        if (duration < 0) {
                            return;
                        }
                        MediaMetadata2 metadata2 = item.getMetadata();
                        if (metadata2 != null) {
                            if (!metadata2.containsKey("android.media.metadata.DURATION")) {
                                metadata = new MediaMetadata2.Builder(metadata2).putLong("android.media.metadata.DURATION", duration).build();
                            } else {
                                long durationFromMetadata = metadata2.getLong("android.media.metadata.DURATION");
                                if (duration != durationFromMetadata) {
                                    Log.w(MediaSession2ImplBase.TAG, "duration mismatch for an item. duration from player=" + duration + " duration from metadata=" + durationFromMetadata + ". May be a timing issue?");
                                }
                                metadata = null;
                            }
                        } else {
                            metadata = new MediaMetadata2.Builder().putLong("android.media.metadata.DURATION", duration).putString("android.media.metadata.MEDIA_ID", item.getMediaId()).build();
                        }
                        if (metadata != null) {
                            item.setMetadata(metadata);
                            session.notifyToAllControllers(new NotifyRunnable() {
                                @Override
                                public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                                    callback.onPlaylistChanged(session.getPlaylist(), session.getPlaylistMetadata());
                                }
                            });
                        }
                    }
                    session.getCallback().onMediaPrepared(session.getInstance(), mpb, item);
                }
            });
        }

        @Override
        public void onPlayerStateChanged(final BaseMediaPlayer player, final int state) {
            final MediaSession2ImplBase session = getSession();
            if (session == null) {
                return;
            }
            session.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    session.mAudioFocusHandler.onPlayerStateChanged(state);
                    session.getCallback().onPlayerStateChanged(session.getInstance(), player, state);
                    session.notifyToAllControllers(new NotifyRunnable() {
                        @Override
                        public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                            callback.onPlayerStateChanged(SystemClock.elapsedRealtime(), player.getCurrentPosition(), state);
                        }
                    });
                }
            });
        }

        @Override
        public void onBufferingStateChanged(final BaseMediaPlayer mpb, final DataSourceDesc dsd, final int state) {
            final MediaSession2ImplBase session = getSession();
            if (session == null || dsd == null) {
                return;
            }
            session.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    final MediaItem2 item = MyPlayerEventCallback.this.getMediaItem(session, dsd);
                    if (item == null) {
                        return;
                    }
                    session.getCallback().onBufferingStateChanged(session.getInstance(), mpb, item, state);
                    session.notifyToAllControllers(new NotifyRunnable() {
                        @Override
                        public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                            callback.onBufferingStateChanged(item, state, mpb.getBufferedPosition());
                        }
                    });
                }
            });
        }

        @Override
        public void onPlaybackSpeedChanged(final BaseMediaPlayer mpb, final float speed) {
            final MediaSession2ImplBase session = getSession();
            if (session == null) {
                return;
            }
            session.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    session.getCallback().onPlaybackSpeedChanged(session.getInstance(), mpb, speed);
                    session.notifyToAllControllers(new NotifyRunnable() {
                        @Override
                        public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                            callback.onPlaybackSpeedChanged(SystemClock.elapsedRealtime(), session.getCurrentPosition(), speed);
                        }
                    });
                }
            });
        }

        @Override
        public void onSeekCompleted(final BaseMediaPlayer mpb, final long position) {
            final MediaSession2ImplBase session = getSession();
            if (session == null) {
                return;
            }
            session.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    session.getCallback().onSeekCompleted(session.getInstance(), mpb, position);
                    session.notifyToAllControllers(new NotifyRunnable() {
                        @Override
                        public void run(MediaSession2.ControllerCb callback) throws RemoteException {
                            callback.onSeekCompleted(SystemClock.elapsedRealtime(), session.getCurrentPosition(), position);
                        }
                    });
                }
            });
        }

        private MediaSession2ImplBase getSession() {
            MediaSession2ImplBase session = this.mSession.get();
            if (session == null && MediaSession2ImplBase.DEBUG) {
                Log.d(MediaSession2ImplBase.TAG, "Session is closed", new IllegalStateException());
            }
            return session;
        }

        public MediaItem2 getMediaItem(MediaSession2ImplBase session, DataSourceDesc dsd) {
            MediaPlaylistAgent agent = session.getPlaylistAgent();
            if (agent == null) {
                if (MediaSession2ImplBase.DEBUG) {
                    Log.d(MediaSession2ImplBase.TAG, "Session is closed", new IllegalStateException());
                    return null;
                }
                return null;
            }
            MediaItem2 item = agent.getMediaItem(dsd);
            if (item == null && MediaSession2ImplBase.DEBUG) {
                Log.d(MediaSession2ImplBase.TAG, "Could not find matching item for dsd=" + dsd, new NoSuchElementException());
            }
            return item;
        }
    }

    private static class MyPlaylistEventCallback extends MediaPlaylistAgent.PlaylistEventCallback {
        private final WeakReference<MediaSession2ImplBase> mSession;

        private MyPlaylistEventCallback(MediaSession2ImplBase session) {
            this.mSession = new WeakReference<>(session);
        }

        @Override
        public void onPlaylistChanged(MediaPlaylistAgent playlistAgent, List<MediaItem2> list, MediaMetadata2 metadata) {
            MediaSession2ImplBase session = this.mSession.get();
            if (session != null) {
                session.notifyPlaylistChangedOnExecutor(playlistAgent, list, metadata);
            }
        }

        @Override
        public void onPlaylistMetadataChanged(MediaPlaylistAgent playlistAgent, MediaMetadata2 metadata) {
            MediaSession2ImplBase session = this.mSession.get();
            if (session != null) {
                session.notifyPlaylistMetadataChangedOnExecutor(playlistAgent, metadata);
            }
        }

        @Override
        public void onRepeatModeChanged(MediaPlaylistAgent playlistAgent, int repeatMode) {
            MediaSession2ImplBase session = this.mSession.get();
            if (session != null) {
                session.notifyRepeatModeChangedOnExecutor(playlistAgent, repeatMode);
            }
        }

        @Override
        public void onShuffleModeChanged(MediaPlaylistAgent playlistAgent, int shuffleMode) {
            MediaSession2ImplBase session = this.mSession.get();
            if (session != null) {
                session.notifyShuffleModeChangedOnExecutor(playlistAgent, shuffleMode);
            }
        }
    }
}
