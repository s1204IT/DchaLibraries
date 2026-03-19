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
import android.support.v4.app.DialogFragment;
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

    static {
        sDefaultRootExtras.putBoolean("android.support.v4.media.root_default_root", true);
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
            this.mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    MediaController2ImplLegacy.this.mCallback.onDisconnected(MediaController2ImplLegacy.this.mInstance);
                }
            });
        }
    }

    void onConnectedNotLocked(Bundle data) {
        data.setClassLoader(MediaSession2.class.getClassLoader());
        final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(data.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
        int playerState = data.getInt("android.support.v4.media.argument.PLAYER_STATE");
        MediaItem2 currentMediaItem = MediaItem2.fromBundle(data.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
        int bufferingState = data.getInt("android.support.v4.media.argument.BUFFERING_STATE");
        PlaybackStateCompat playbackStateCompat = (PlaybackStateCompat) data.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
        int repeatMode = data.getInt("android.support.v4.media.argument.REPEAT_MODE");
        int shuffleMode = data.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
        List<MediaItem2> playlist = MediaUtils2.convertToMediaItem2List(data.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
        MediaController2.PlaybackInfo playbackInfo = MediaController2.PlaybackInfo.fromBundle(data.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
        MediaMetadata2 metadata = MediaMetadata2.fromBundle(data.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
        if (DEBUG) {
            Log.d("MC2ImplLegacy", "onConnectedNotLocked token=" + this.mToken + ", allowedCommands=" + allowedCommands);
        }
        try {
            synchronized (this.mLock) {
                if (this.mIsReleased) {
                    if (close) {
                        return;
                    } else {
                        return;
                    }
                }
                if (this.mConnected) {
                    Log.e("MC2ImplLegacy", "Cannot be notified about the connection result many times. Probably a bug or malicious app.");
                    if (1 != 0) {
                        close();
                        return;
                    }
                    return;
                }
                this.mAllowedCommands = allowedCommands;
                this.mPlayerState = playerState;
                this.mCurrentMediaItem = currentMediaItem;
                this.mBufferingState = bufferingState;
                this.mPlaybackStateCompat = playbackStateCompat;
                this.mRepeatMode = repeatMode;
                this.mShuffleMode = shuffleMode;
                this.mPlaylist = playlist;
                this.mPlaylistMetadata = metadata;
                this.mConnected = true;
                this.mPlaybackInfo = playbackInfo;
                this.mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        MediaController2ImplLegacy.this.mCallback.onConnected(MediaController2ImplLegacy.this.mInstance, allowedCommands);
                    }
                });
                if (0 != 0) {
                    close();
                }
            }
        } finally {
            if (0 != 0) {
                close();
            }
        }
    }

    class AnonymousClass3 extends ResultReceiver {
        final MediaController2ImplLegacy this$0;

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (this.this$0.mHandlerThread.isAlive()) {
                switch (resultCode) {
                    case -1:
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                AnonymousClass3.this.this$0.mCallback.onDisconnected(AnonymousClass3.this.this$0.mInstance);
                            }
                        });
                        this.this$0.close();
                        break;
                    case DialogFragment.STYLE_NORMAL:
                        this.this$0.onConnectedNotLocked(resultData);
                        break;
                }
            }
        }
    }

    private void sendCommand(String command) throws Throwable {
        sendCommand(command, null, null);
    }

    private void sendCommand(String command, ResultReceiver receiver) throws Throwable {
        sendCommand(command, null, receiver);
    }

    private void sendCommand(String command, Bundle args, ResultReceiver receiver) throws Throwable {
        if (args == null) {
            args = new Bundle();
        }
        synchronized (this.mLock) {
            ControllerCompatCallback callback = null;
            try {
                MediaControllerCompat controller = this.mControllerCompat;
                try {
                    callback = this.mControllerCompatCallback;
                    BundleCompat.putBinder(args, "android.support.v4.media.argument.ICONTROLLER_CALLBACK", callback.getIControllerCallback().asBinder());
                    args.putString("android.support.v4.media.argument.PACKAGE_NAME", this.mContext.getPackageName());
                    args.putInt("android.support.v4.media.argument.UID", Process.myUid());
                    args.putInt("android.support.v4.media.argument.PID", Process.myPid());
                    controller.sendCommand(command, args, receiver);
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

    private final class ControllerCompatCallback extends MediaControllerCompat.Callback {
        final MediaController2ImplLegacy this$0;

        @Override
        public void onSessionReady() throws Throwable {
            this.this$0.sendCommand("android.support.v4.media.controller.command.CONNECT", new ResultReceiver(this.this$0.mHandler) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    if (ControllerCompatCallback.this.this$0.mHandlerThread.isAlive()) {
                        switch (resultCode) {
                            case -1:
                                ControllerCompatCallback.this.this$0.mCallbackExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        ControllerCompatCallback.this.this$0.mCallback.onDisconnected(ControllerCompatCallback.this.this$0.mInstance);
                                    }
                                });
                                ControllerCompatCallback.this.this$0.close();
                                break;
                            case DialogFragment.STYLE_NORMAL:
                                ControllerCompatCallback.this.this$0.onConnectedNotLocked(resultData);
                                break;
                        }
                    }
                }
            });
        }

        @Override
        public void onSessionDestroyed() {
            this.this$0.close();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            synchronized (this.this$0.mLock) {
                this.this$0.mPlaybackStateCompat = state;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (this.this$0.mLock) {
                this.this$0.mMediaMetadataCompat = metadata;
            }
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            if (extras != null) {
                extras.setClassLoader(MediaSession2.class.getClassLoader());
            }
            switch (event) {
                case "android.support.v4.media.session.event.ON_ALLOWED_COMMANDS_CHANGED":
                    final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(extras.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mAllowedCommands = allowedCommands;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onAllowedCommandsChanged(ControllerCompatCallback.this.this$0.mInstance, allowedCommands);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYER_STATE_CHANGED":
                    final int playerState = extras.getInt("android.support.v4.media.argument.PLAYER_STATE");
                    PlaybackStateCompat state = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state == null) {
                        return;
                    }
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlayerState = playerState;
                        this.this$0.mPlaybackStateCompat = state;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlayerStateChanged(ControllerCompatCallback.this.this$0.mInstance, playerState);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED":
                    final MediaItem2 item = MediaItem2.fromBundle(extras.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mCurrentMediaItem = item;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onCurrentMediaItemChanged(ControllerCompatCallback.this.this$0.mInstance, item);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_ERROR":
                    final int errorCode = extras.getInt("android.support.v4.media.argument.ERROR_CODE");
                    final Bundle errorExtras = extras.getBundle("android.support.v4.media.argument.EXTRAS");
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onError(ControllerCompatCallback.this.this$0.mInstance, errorCode, errorExtras);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_ROUTES_INFO_CHANGED":
                    final List<Bundle> routes = MediaUtils2.convertToBundleList(extras.getParcelableArray("android.support.v4.media.argument.ROUTE_BUNDLE"));
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onRoutesInfoChanged(ControllerCompatCallback.this.this$0.mInstance, routes);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYLIST_CHANGED":
                    final MediaMetadata2 playlistMetadata = MediaMetadata2.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    final List<MediaItem2> playlist = MediaUtils2.convertToMediaItem2List(extras.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaylist = playlist;
                        this.this$0.mPlaylistMetadata = playlistMetadata;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlaylistChanged(ControllerCompatCallback.this.this$0.mInstance, playlist, playlistMetadata);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYLIST_METADATA_CHANGED":
                    final MediaMetadata2 playlistMetadata2 = MediaMetadata2.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaylistMetadata = playlistMetadata2;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlaylistMetadataChanged(ControllerCompatCallback.this.this$0.mInstance, playlistMetadata2);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_REPEAT_MODE_CHANGED":
                    final int repeatMode = extras.getInt("android.support.v4.media.argument.REPEAT_MODE");
                    synchronized (this.this$0.mLock) {
                        this.this$0.mRepeatMode = repeatMode;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onRepeatModeChanged(ControllerCompatCallback.this.this$0.mInstance, repeatMode);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_SHUFFLE_MODE_CHANGED":
                    final int shuffleMode = extras.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
                    synchronized (this.this$0.mLock) {
                        this.this$0.mShuffleMode = shuffleMode;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onShuffleModeChanged(ControllerCompatCallback.this.this$0.mInstance, shuffleMode);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.SEND_CUSTOM_COMMAND":
                    Bundle commandBundle = extras.getBundle("android.support.v4.media.argument.CUSTOM_COMMAND");
                    if (commandBundle == null) {
                        return;
                    }
                    final SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
                    final Bundle args = extras.getBundle("android.support.v4.media.argument.ARGUMENTS");
                    final ResultReceiver receiver = (ResultReceiver) extras.getParcelable("android.support.v4.media.argument.RESULT_RECEIVER");
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onCustomCommand(ControllerCompatCallback.this.this$0.mInstance, command, args, receiver);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.SET_CUSTOM_LAYOUT":
                    final List<MediaSession2.CommandButton> layout = MediaUtils2.convertToCommandButtonList(extras.getParcelableArray("android.support.v4.media.argument.COMMAND_BUTTONS"));
                    if (layout != null) {
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onCustomLayoutChanged(ControllerCompatCallback.this.this$0.mInstance, layout);
                            }
                        });
                        return;
                    }
                    return;
                case "android.support.v4.media.session.event.ON_PLAYBACK_INFO_CHANGED":
                    final MediaController2.PlaybackInfo info = MediaController2.PlaybackInfo.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
                    if (info == null) {
                        return;
                    }
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaybackInfo = info;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlaybackInfoChanged(ControllerCompatCallback.this.this$0.mInstance, info);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_PLAYBACK_SPEED_CHANGED":
                    final PlaybackStateCompat state2 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state2 == null) {
                        return;
                    }
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaybackStateCompat = state2;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlaybackSpeedChanged(ControllerCompatCallback.this.this$0.mInstance, state2.getPlaybackSpeed());
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_BUFFERING_STATE_CHANGED":
                    final MediaItem2 item2 = MediaItem2.fromBundle(extras.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    final int bufferingState = extras.getInt("android.support.v4.media.argument.BUFFERING_STATE");
                    PlaybackStateCompat state3 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (item2 == null || state3 == null) {
                        return;
                    }
                    synchronized (this.this$0.mLock) {
                        this.this$0.mBufferingState = bufferingState;
                        this.this$0.mPlaybackStateCompat = state3;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onBufferingStateChanged(ControllerCompatCallback.this.this$0.mInstance, item2, bufferingState);
                        }
                    });
                    return;
                case "android.support.v4.media.session.event.ON_SEEK_COMPLETED":
                    final long position = extras.getLong("android.support.v4.media.argument.SEEK_POSITION");
                    PlaybackStateCompat state4 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state4 == null) {
                        return;
                    }
                    synchronized (this.this$0.mLock) {
                        this.this$0.mPlaybackStateCompat = state4;
                        break;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onSeekCompleted(ControllerCompatCallback.this.this$0.mInstance, position);
                        }
                    });
                    return;
                default:
                    return;
            }
        }
    }
}
