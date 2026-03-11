package android.support.v4.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaController2;
import android.support.v4.media.MediaSession2;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.util.List;
import java.util.concurrent.Executor;

@TargetApi(16)
class MediaController2ImplLegacy implements MediaController2.SupportLibraryImpl {
    private static final boolean DEBUG = Log.isLoggable("MC2ImplLegacy", 3);
    static final Bundle sDefaultRootExtras = new Bundle();
    private SessionCommandGroup2 mAllowedCommands;
    private MediaBrowserCompat mBrowserCompat;
    private int mBufferingState;
    private final MediaController2.ControllerCallback mCallback;
    private final Executor mCallbackExecutor;
    private volatile boolean mConnected;
    private final Context mContext;
    private MediaControllerCompat mControllerCompat;
    private ControllerCompatCallback mControllerCompatCallback;
    private MediaItem2 mCurrentMediaItem;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private MediaController2 mInstance;
    private boolean mIsReleased;
    final Object mLock;
    private MediaMetadataCompat mMediaMetadataCompat;
    private MediaController2.PlaybackInfo mPlaybackInfo;
    private PlaybackStateCompat mPlaybackStateCompat;
    private int mPlayerState;
    private List<MediaItem2> mPlaylist;
    private MediaMetadata2 mPlaylistMetadata;
    private int mRepeatMode;
    private int mShuffleMode;
    private final SessionToken2 mToken;

    class AnonymousClass3 extends ResultReceiver {
        final MediaController2ImplLegacy this$0;

        @Override
        protected void onReceiveResult(int i, Bundle bundle) {
            if (this.this$0.mHandlerThread.isAlive()) {
                switch (i) {
                    case -1:
                        this.this$0.mCallbackExecutor.execute(new Runnable(this) {
                            final AnonymousClass3 this$1;

                            {
                                this.this$1 = this;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onDisconnected(this.this$1.this$0.mInstance);
                            }
                        });
                        this.this$0.close();
                        break;
                    case 0:
                        this.this$0.onConnectedNotLocked(bundle);
                        break;
                }
            }
        }
    }

    private final class ControllerCompatCallback extends MediaControllerCompat.Callback {
        final MediaController2ImplLegacy this$0;

        class AnonymousClass1 extends ResultReceiver {
            final ControllerCompatCallback this$1;

            AnonymousClass1(ControllerCompatCallback controllerCompatCallback, Handler handler) {
                super(handler);
                this.this$1 = controllerCompatCallback;
            }

            @Override
            protected void onReceiveResult(int i, Bundle bundle) {
                if (this.this$1.this$0.mHandlerThread.isAlive()) {
                    switch (i) {
                        case -1:
                            this.this$1.this$0.mCallbackExecutor.execute(new Runnable(this) {
                                final AnonymousClass1 this$2;

                                {
                                    this.this$2 = this;
                                }

                                @Override
                                public void run() {
                                    this.this$2.this$1.this$0.mCallback.onDisconnected(this.this$2.this$1.this$0.mInstance);
                                }
                            });
                            this.this$1.this$0.close();
                            break;
                        case 0:
                            this.this$1.this$0.onConnectedNotLocked(bundle);
                            break;
                    }
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat mediaMetadataCompat) {
            synchronized (this.this$0.mLock) {
                this.this$0.mMediaMetadataCompat = mediaMetadataCompat;
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat playbackStateCompat) {
            synchronized (this.this$0.mLock) {
                this.this$0.mPlaybackStateCompat = playbackStateCompat;
            }
        }

        @Override
        public void onSessionDestroyed() {
            this.this$0.close();
        }

        @Override
        public void onSessionEvent(String str, Bundle bundle) {
            if (bundle != null) {
                bundle.setClassLoader(MediaSession2.class.getClassLoader());
            }
            switch (str) {
                case "android.support.v4.media.session.event.ON_ALLOWED_COMMANDS_CHANGED":
                    SessionCommandGroup2 sessionCommandGroup2FromBundle = SessionCommandGroup2.fromBundle(bundle.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mAllowedCommands = sessionCommandGroup2FromBundle;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, sessionCommandGroup2FromBundle) {
                        final ControllerCompatCallback this$1;
                        final SessionCommandGroup2 val$allowedCommands;

                        {
                            this.this$1 = this;
                            this.val$allowedCommands = sessionCommandGroup2FromBundle;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onAllowedCommandsChanged(this.this$1.this$0.mInstance, this.val$allowedCommands);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYER_STATE_CHANGED":
                    int i = bundle.getInt("android.support.v4.media.argument.PLAYER_STATE");
                    PlaybackStateCompat playbackStateCompat = (PlaybackStateCompat) bundle.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (playbackStateCompat != null) {
                        synchronized (this.this$0.mLock) {
                            this.this$0.mPlayerState = i;
                            this.this$0.mPlaybackStateCompat = playbackStateCompat;
                            break;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable(this, i) {
                            final ControllerCompatCallback this$1;
                            final int val$playerState;

                            {
                                this.this$1 = this;
                                this.val$playerState = i;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onPlayerStateChanged(this.this$1.this$0.mInstance, this.val$playerState);
                            }
                        });
                        return;
                    }
                    return;
                case "android.support.v4.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED":
                    MediaItem2 mediaItem2FromBundle = MediaItem2.fromBundle(bundle.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mCurrentMediaItem = mediaItem2FromBundle;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, mediaItem2FromBundle) {
                        final ControllerCompatCallback this$1;
                        final MediaItem2 val$item;

                        {
                            this.this$1 = this;
                            this.val$item = mediaItem2FromBundle;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onCurrentMediaItemChanged(this.this$1.this$0.mInstance, this.val$item);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_ERROR":
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, bundle.getInt("android.support.v4.media.argument.ERROR_CODE"), bundle.getBundle("android.support.v4.media.argument.EXTRAS")) {
                        final ControllerCompatCallback this$1;
                        final int val$errorCode;
                        final Bundle val$errorExtras;

                        {
                            this.this$1 = this;
                            this.val$errorCode = i;
                            this.val$errorExtras = bundle;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onError(this.this$1.this$0.mInstance, this.val$errorCode, this.val$errorExtras);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_ROUTES_INFO_CHANGED":
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, MediaUtils2.convertToBundleList(bundle.getParcelableArray("android.support.v4.media.argument.ROUTE_BUNDLE"))) {
                        final ControllerCompatCallback this$1;
                        final List val$routes;

                        {
                            this.this$1 = this;
                            this.val$routes = list;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onRoutesInfoChanged(this.this$1.this$0.mInstance, this.val$routes);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYLIST_CHANGED":
                    MediaMetadata2 mediaMetadata2FromBundle = MediaMetadata2.fromBundle(bundle.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    List<MediaItem2> listConvertToMediaItem2List = MediaUtils2.convertToMediaItem2List(bundle.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaylist = listConvertToMediaItem2List;
                        this.this$0.mPlaylistMetadata = mediaMetadata2FromBundle;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, listConvertToMediaItem2List, mediaMetadata2FromBundle) {
                        final ControllerCompatCallback this$1;
                        final List val$playlist;
                        final MediaMetadata2 val$playlistMetadata;

                        {
                            this.this$1 = this;
                            this.val$playlist = listConvertToMediaItem2List;
                            this.val$playlistMetadata = mediaMetadata2FromBundle;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onPlaylistChanged(this.this$1.this$0.mInstance, this.val$playlist, this.val$playlistMetadata);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYLIST_METADATA_CHANGED":
                    MediaMetadata2 mediaMetadata2FromBundle2 = MediaMetadata2.fromBundle(bundle.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaylistMetadata = mediaMetadata2FromBundle2;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, mediaMetadata2FromBundle2) {
                        final ControllerCompatCallback this$1;
                        final MediaMetadata2 val$playlistMetadata;

                        {
                            this.this$1 = this;
                            this.val$playlistMetadata = mediaMetadata2FromBundle2;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onPlaylistMetadataChanged(this.this$1.this$0.mInstance, this.val$playlistMetadata);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_REPEAT_MODE_CHANGED":
                    int i2 = bundle.getInt("android.support.v4.media.argument.REPEAT_MODE");
                    synchronized (this.this$0.mLock) {
                        this.this$0.mRepeatMode = i2;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, i2) {
                        final ControllerCompatCallback this$1;
                        final int val$repeatMode;

                        {
                            this.this$1 = this;
                            this.val$repeatMode = i2;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onRepeatModeChanged(this.this$1.this$0.mInstance, this.val$repeatMode);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_SHUFFLE_MODE_CHANGED":
                    int i3 = bundle.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
                    synchronized (this.this$0.mLock) {
                        this.this$0.mShuffleMode = i3;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, i3) {
                        final ControllerCompatCallback this$1;
                        final int val$shuffleMode;

                        {
                            this.this$1 = this;
                            this.val$shuffleMode = i3;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onShuffleModeChanged(this.this$1.this$0.mInstance, this.val$shuffleMode);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.SEND_CUSTOM_COMMAND":
                    Bundle bundle2 = bundle.getBundle("android.support.v4.media.argument.CUSTOM_COMMAND");
                    if (bundle2 != null) {
                        this.this$0.mCallbackExecutor.execute(new Runnable(this, SessionCommand2.fromBundle(bundle2), bundle.getBundle("android.support.v4.media.argument.ARGUMENTS"), (ResultReceiver) bundle.getParcelable("android.support.v4.media.argument.RESULT_RECEIVER")) {
                            final ControllerCompatCallback this$1;
                            final Bundle val$args;
                            final SessionCommand2 val$command;
                            final ResultReceiver val$receiver;

                            {
                                this.this$1 = this;
                                this.val$command = sessionCommand2;
                                this.val$args = bundle;
                                this.val$receiver = resultReceiver;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onCustomCommand(this.this$1.this$0.mInstance, this.val$command, this.val$args, this.val$receiver);
                            }
                        });
                        return;
                    }
                    return;
                case "android.support.v4.media.session.event.SET_CUSTOM_LAYOUT":
                    List<MediaSession2.CommandButton> listConvertToCommandButtonList = MediaUtils2.convertToCommandButtonList(bundle.getParcelableArray("android.support.v4.media.argument.COMMAND_BUTTONS"));
                    if (listConvertToCommandButtonList != null) {
                        this.this$0.mCallbackExecutor.execute(new Runnable(this, listConvertToCommandButtonList) {
                            final ControllerCompatCallback this$1;
                            final List val$layout;

                            {
                                this.this$1 = this;
                                this.val$layout = listConvertToCommandButtonList;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onCustomLayoutChanged(this.this$1.this$0.mInstance, this.val$layout);
                            }
                        });
                        return;
                    }
                    return;
                case "android.support.v4.media.session.event.ON_PLAYBACK_INFO_CHANGED":
                    MediaController2.PlaybackInfo playbackInfoFromBundle = MediaController2.PlaybackInfo.fromBundle(bundle.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
                    if (playbackInfoFromBundle != null) {
                        synchronized (this.this$0.mLock) {
                            this.this$0.mPlaybackInfo = playbackInfoFromBundle;
                            break;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable(this, playbackInfoFromBundle) {
                            final ControllerCompatCallback this$1;
                            final MediaController2.PlaybackInfo val$info;

                            {
                                this.this$1 = this;
                                this.val$info = playbackInfoFromBundle;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onPlaybackInfoChanged(this.this$1.this$0.mInstance, this.val$info);
                            }
                        });
                        return;
                    }
                    return;
                case "android.support.v4.media.session.event.ON_PLAYBACK_SPEED_CHANGED":
                    PlaybackStateCompat playbackStateCompat2 = (PlaybackStateCompat) bundle.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (playbackStateCompat2 != null) {
                        synchronized (this.this$0.mLock) {
                            this.this$0.mPlaybackStateCompat = playbackStateCompat2;
                            break;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable(this, playbackStateCompat2) {
                            final ControllerCompatCallback this$1;
                            final PlaybackStateCompat val$state;

                            {
                                this.this$1 = this;
                                this.val$state = playbackStateCompat2;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onPlaybackSpeedChanged(this.this$1.this$0.mInstance, this.val$state.getPlaybackSpeed());
                            }
                        });
                        return;
                    }
                    return;
                case "android.support.v4.media.session.event.ON_BUFFERING_STATE_CHANGED":
                    MediaItem2 mediaItem2FromBundle2 = MediaItem2.fromBundle(bundle.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    int i4 = bundle.getInt("android.support.v4.media.argument.BUFFERING_STATE");
                    PlaybackStateCompat playbackStateCompat3 = (PlaybackStateCompat) bundle.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (mediaItem2FromBundle2 == null || playbackStateCompat3 == null) {
                        return;
                    }
                    synchronized (this.this$0.mLock) {
                        this.this$0.mBufferingState = i4;
                        this.this$0.mPlaybackStateCompat = playbackStateCompat3;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable(this, mediaItem2FromBundle2, i4) {
                        final ControllerCompatCallback this$1;
                        final int val$bufferingState;
                        final MediaItem2 val$item;

                        {
                            this.this$1 = this;
                            this.val$item = mediaItem2FromBundle2;
                            this.val$bufferingState = i4;
                        }

                        @Override
                        public void run() {
                            this.this$1.this$0.mCallback.onBufferingStateChanged(this.this$1.this$0.mInstance, this.val$item, this.val$bufferingState);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_SEEK_COMPLETED":
                    long j = bundle.getLong("android.support.v4.media.argument.SEEK_POSITION");
                    PlaybackStateCompat playbackStateCompat4 = (PlaybackStateCompat) bundle.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (playbackStateCompat4 != null) {
                        synchronized (this.this$0.mLock) {
                            this.this$0.mPlaybackStateCompat = playbackStateCompat4;
                            break;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable(this, j) {
                            final ControllerCompatCallback this$1;
                            final long val$position;

                            {
                                this.this$1 = this;
                                this.val$position = j;
                            }

                            @Override
                            public void run() {
                                this.this$1.this$0.mCallback.onSeekCompleted(this.this$1.this$0.mInstance, this.val$position);
                            }
                        });
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        @Override
        public void onSessionReady() throws Throwable {
            this.this$0.sendCommand("android.support.v4.media.controller.command.CONNECT", new AnonymousClass1(this, this.this$0.mHandler));
        }
    }

    static {
        sDefaultRootExtras.putBoolean("android.support.v4.media.root_default_root", true);
    }

    private void sendCommand(String str) throws Throwable {
        sendCommand(str, null, null);
    }

    private void sendCommand(String str, Bundle bundle, ResultReceiver resultReceiver) throws Throwable {
        if (bundle == null) {
            bundle = new Bundle();
        }
        synchronized (this.mLock) {
            try {
                MediaControllerCompat mediaControllerCompat = this.mControllerCompat;
                try {
                    BundleCompat.putBinder(bundle, "android.support.v4.media.argument.ICONTROLLER_CALLBACK", this.mControllerCompatCallback.getIControllerCallback().asBinder());
                    bundle.putString("android.support.v4.media.argument.PACKAGE_NAME", this.mContext.getPackageName());
                    bundle.putInt("android.support.v4.media.argument.UID", Process.myUid());
                    bundle.putInt("android.support.v4.media.argument.PID", Process.myPid());
                    mediaControllerCompat.sendCommand(str, bundle, resultReceiver);
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
            }
        }
    }

    public void sendCommand(String str, ResultReceiver resultReceiver) throws Throwable {
        sendCommand(str, null, resultReceiver);
    }

    @Override
    public void close() {
        if (DEBUG) {
            Log.d("MC2ImplLegacy", "release from " + this.mToken);
        }
        synchronized (this.mLock) {
            if (this.mIsReleased) {
                return;
            }
            this.mHandler.removeCallbacksAndMessages(null);
            if (Build.VERSION.SDK_INT >= 18) {
                this.mHandlerThread.quitSafely();
            } else {
                this.mHandlerThread.quit();
            }
            this.mIsReleased = true;
            sendCommand("android.support.v4.media.controller.command.DISCONNECT");
            if (this.mControllerCompat != null) {
                this.mControllerCompat.unregisterCallback(this.mControllerCompatCallback);
            }
            if (this.mBrowserCompat != null) {
                this.mBrowserCompat.disconnect();
                this.mBrowserCompat = null;
            }
            if (this.mControllerCompat != null) {
                this.mControllerCompat.unregisterCallback(this.mControllerCompatCallback);
                this.mControllerCompat = null;
            }
            this.mConnected = false;
            this.mCallbackExecutor.execute(new Runnable(this) {
                final MediaController2ImplLegacy this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void run() {
                    this.this$0.mCallback.onDisconnected(this.this$0.mInstance);
                }
            });
        }
    }

    void onConnectedNotLocked(Bundle bundle) {
        bundle.setClassLoader(MediaSession2.class.getClassLoader());
        SessionCommandGroup2 sessionCommandGroup2FromBundle = SessionCommandGroup2.fromBundle(bundle.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
        int i = bundle.getInt("android.support.v4.media.argument.PLAYER_STATE");
        MediaItem2 mediaItem2FromBundle = MediaItem2.fromBundle(bundle.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
        int i2 = bundle.getInt("android.support.v4.media.argument.BUFFERING_STATE");
        PlaybackStateCompat playbackStateCompat = (PlaybackStateCompat) bundle.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
        int i3 = bundle.getInt("android.support.v4.media.argument.REPEAT_MODE");
        int i4 = bundle.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
        List<MediaItem2> listConvertToMediaItem2List = MediaUtils2.convertToMediaItem2List(bundle.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
        MediaController2.PlaybackInfo playbackInfoFromBundle = MediaController2.PlaybackInfo.fromBundle(bundle.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
        MediaMetadata2 mediaMetadata2FromBundle = MediaMetadata2.fromBundle(bundle.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
        if (DEBUG) {
            Log.d("MC2ImplLegacy", "onConnectedNotLocked token=" + this.mToken + ", allowedCommands=" + sessionCommandGroup2FromBundle);
        }
        try {
            synchronized (this.mLock) {
                try {
                    if (this.mIsReleased) {
                        return;
                    }
                    if (this.mConnected) {
                        Log.e("MC2ImplLegacy", "Cannot be notified about the connection result many times. Probably a bug or malicious app.");
                        try {
                            close();
                            return;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    this.mAllowedCommands = sessionCommandGroup2FromBundle;
                    this.mPlayerState = i;
                    this.mCurrentMediaItem = mediaItem2FromBundle;
                    this.mBufferingState = i2;
                    this.mPlaybackStateCompat = playbackStateCompat;
                    this.mRepeatMode = i3;
                    this.mShuffleMode = i4;
                    this.mPlaylist = listConvertToMediaItem2List;
                    this.mPlaylistMetadata = mediaMetadata2FromBundle;
                    this.mConnected = true;
                    this.mPlaybackInfo = playbackInfoFromBundle;
                    this.mCallbackExecutor.execute(new Runnable(this, sessionCommandGroup2FromBundle) {
                        final MediaController2ImplLegacy this$0;
                        final SessionCommandGroup2 val$allowedCommands;

                        {
                            this.this$0 = this;
                            this.val$allowedCommands = sessionCommandGroup2FromBundle;
                        }

                        @Override
                        public void run() {
                            this.this$0.mCallback.onConnected(this.this$0.mInstance, this.val$allowedCommands);
                        }
                    });
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (Throwable th3) {
            if (0 != 0) {
                close();
            }
            throw th3;
        }
    }
}
