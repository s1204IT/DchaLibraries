package android.support.v4.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaController2;
import android.support.v4.media.MediaSession2;
import android.support.v4.media.session.IMediaControllerCallback;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@TargetApi(19)
class MediaSessionLegacyStub extends MediaSessionCompat.Callback {
    final Context mContext;
    final MediaSession2.SupportLibraryImpl mSession;
    private static final String TAG = "MediaSessionLegacyStub";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest = new SparseArray<>();
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArrayMap<IBinder, MediaSession2.ControllerInfo> mControllers = new ArrayMap<>();

    @GuardedBy("mLock")
    private final Set<IBinder> mConnectingControllers = new HashSet();

    @GuardedBy("mLock")
    private final ArrayMap<MediaSession2.ControllerInfo, SessionCommandGroup2> mAllowedCommandGroupMap = new ArrayMap<>();

    @FunctionalInterface
    private interface Session2Runnable {
        void run(MediaSession2.ControllerInfo controllerInfo) throws RemoteException;
    }

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2();
        group.addAllPlaybackCommands();
        group.addAllPlaylistCommands();
        group.addAllVolumeCommands();
        Set<SessionCommand2> commands = group.getCommands();
        for (SessionCommand2 command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    MediaSessionLegacyStub(MediaSession2.SupportLibraryImpl session) {
        this.mSession = session;
        this.mContext = this.mSession.getContext();
    }

    @Override
    public void onPrepare() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MediaSessionLegacyStub.this.mSession.isClosed()) {
                    return;
                }
                MediaSessionLegacyStub.this.mSession.prepare();
            }
        });
    }

    @Override
    public void onPlay() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MediaSessionLegacyStub.this.mSession.isClosed()) {
                    return;
                }
                MediaSessionLegacyStub.this.mSession.play();
            }
        });
    }

    @Override
    public void onPause() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MediaSessionLegacyStub.this.mSession.isClosed()) {
                    return;
                }
                MediaSessionLegacyStub.this.mSession.pause();
            }
        });
    }

    @Override
    public void onStop() {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MediaSessionLegacyStub.this.mSession.isClosed()) {
                    return;
                }
                MediaSessionLegacyStub.this.mSession.reset();
            }
        });
    }

    @Override
    public void onSeekTo(final long pos) {
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MediaSessionLegacyStub.this.mSession.isClosed()) {
                    return;
                }
                MediaSessionLegacyStub.this.mSession.seekTo(pos);
            }
        });
    }

    List<MediaSession2.ControllerInfo> getConnectedControllers() {
        ArrayList<MediaSession2.ControllerInfo> controllers = new ArrayList<>();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                controllers.add(this.mControllers.valueAt(i));
            }
        }
        return controllers;
    }

    void setAllowedCommands(MediaSession2.ControllerInfo controller, SessionCommandGroup2 commands) {
        synchronized (this.mLock) {
            this.mAllowedCommandGroupMap.put(controller, commands);
        }
    }

    public boolean isAllowedCommand(MediaSession2.ControllerInfo controller, SessionCommand2 command) {
        SessionCommandGroup2 allowedCommands;
        synchronized (this.mLock) {
            allowedCommands = this.mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(MediaSession2.ControllerInfo controller, int commandCode) {
        SessionCommandGroup2 allowedCommands;
        synchronized (this.mLock) {
            allowedCommands = this.mAllowedCommandGroupMap.get(controller);
        }
        return allowedCommands != null && allowedCommands.hasCommand(commandCode);
    }

    private void onCommand2(@NonNull IBinder caller, int commandCode, @NonNull Session2Runnable runnable) {
        onCommand2Internal(caller, null, commandCode, runnable);
    }

    private void onCommand2(@NonNull IBinder caller, @NonNull SessionCommand2 sessionCommand, @NonNull Session2Runnable runnable) {
        onCommand2Internal(caller, sessionCommand, 0, runnable);
    }

    private void onCommand2Internal(@NonNull IBinder caller, @Nullable final SessionCommand2 sessionCommand, final int commandCode, @NonNull final Session2Runnable runnable) {
        final MediaSession2.ControllerInfo controller;
        synchronized (this.mLock) {
            controller = this.mControllers.get(caller);
        }
        if (this.mSession == null || controller == null) {
            return;
        }
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SessionCommand2 command;
                if (sessionCommand != null) {
                    if (MediaSessionLegacyStub.this.isAllowedCommand(controller, sessionCommand)) {
                        command = (SessionCommand2) MediaSessionLegacyStub.sCommandsForOnCommandRequest.get(sessionCommand.getCommandCode());
                    } else {
                        return;
                    }
                } else if (MediaSessionLegacyStub.this.isAllowedCommand(controller, commandCode)) {
                    command = (SessionCommand2) MediaSessionLegacyStub.sCommandsForOnCommandRequest.get(commandCode);
                } else {
                    return;
                }
                if (command != null) {
                    boolean accepted = MediaSessionLegacyStub.this.mSession.getCallback().onCommandRequest(MediaSessionLegacyStub.this.mSession.getInstance(), controller, command);
                    if (!accepted) {
                        if (MediaSessionLegacyStub.DEBUG) {
                            Log.d(MediaSessionLegacyStub.TAG, "Command (" + command + ") from " + controller + " was rejected by " + MediaSessionLegacyStub.this.mSession);
                            return;
                        }
                        return;
                    }
                }
                try {
                    runnable.run(controller);
                } catch (RemoteException e) {
                    Log.w(MediaSessionLegacyStub.TAG, "Exception in " + controller.toString(), e);
                }
            }
        });
    }

    void removeControllerInfo(MediaSession2.ControllerInfo controller) {
        synchronized (this.mLock) {
            MediaSession2.ControllerInfo controller2 = this.mControllers.remove(controller.getId());
            if (DEBUG) {
                Log.d(TAG, "releasing " + controller2);
            }
        }
    }

    private MediaSession2.ControllerInfo createControllerInfo(Bundle extras) {
        IMediaControllerCallback callback = IMediaControllerCallback.Stub.asInterface(BundleCompat.getBinder(extras, "android.support.v4.media.argument.ICONTROLLER_CALLBACK"));
        String packageName = extras.getString("android.support.v4.media.argument.PACKAGE_NAME");
        int uid = extras.getInt("android.support.v4.media.argument.UID");
        int pid = extras.getInt("android.support.v4.media.argument.PID");
        return new MediaSession2.ControllerInfo(packageName, pid, uid, new ControllerLegacyCb(callback));
    }

    private void connect(Bundle extras, final ResultReceiver cb) {
        final MediaSession2.ControllerInfo controllerInfo = createControllerInfo(extras);
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SessionCommandGroup2 allowedCommands;
                List<MediaItem2> playlist;
                if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                    synchronized (MediaSessionLegacyStub.this.mLock) {
                        MediaSessionLegacyStub.this.mConnectingControllers.add(controllerInfo.getId());
                    }
                    SessionCommandGroup2 allowedCommands2 = MediaSessionLegacyStub.this.mSession.getCallback().onConnect(MediaSessionLegacyStub.this.mSession.getInstance(), controllerInfo);
                    boolean accept = allowedCommands2 != null || controllerInfo.isTrusted();
                    MediaItem2 currentMediaItem = null;
                    if (accept) {
                        if (MediaSessionLegacyStub.DEBUG) {
                            Log.d(MediaSessionLegacyStub.TAG, "Accepting connection, controllerInfo=" + controllerInfo + " allowedCommands=" + allowedCommands2);
                        }
                        if (allowedCommands2 == null) {
                            allowedCommands = new SessionCommandGroup2();
                        } else {
                            allowedCommands = allowedCommands2;
                        }
                        synchronized (MediaSessionLegacyStub.this.mLock) {
                            MediaSessionLegacyStub.this.mConnectingControllers.remove(controllerInfo.getId());
                            MediaSessionLegacyStub.this.mControllers.put(controllerInfo.getId(), controllerInfo);
                            MediaSessionLegacyStub.this.mAllowedCommandGroupMap.put(controllerInfo, allowedCommands);
                        }
                        Bundle resultData = new Bundle();
                        resultData.putBundle("android.support.v4.media.argument.ALLOWED_COMMANDS", allowedCommands.toBundle());
                        resultData.putInt("android.support.v4.media.argument.PLAYER_STATE", MediaSessionLegacyStub.this.mSession.getPlayerState());
                        resultData.putInt("android.support.v4.media.argument.BUFFERING_STATE", MediaSessionLegacyStub.this.mSession.getBufferingState());
                        resultData.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
                        resultData.putInt("android.support.v4.media.argument.REPEAT_MODE", MediaSessionLegacyStub.this.mSession.getRepeatMode());
                        resultData.putInt("android.support.v4.media.argument.SHUFFLE_MODE", MediaSessionLegacyStub.this.mSession.getShuffleMode());
                        if (!allowedCommands.hasCommand(18)) {
                            playlist = null;
                        } else {
                            playlist = MediaSessionLegacyStub.this.mSession.getPlaylist();
                        }
                        if (playlist != null) {
                            resultData.putParcelableArray("android.support.v4.media.argument.PLAYLIST", MediaUtils2.convertMediaItem2ListToParcelableArray(playlist));
                        }
                        if (allowedCommands.hasCommand(20)) {
                            currentMediaItem = MediaSessionLegacyStub.this.mSession.getCurrentMediaItem();
                        }
                        if (currentMediaItem != null) {
                            resultData.putBundle("android.support.v4.media.argument.MEDIA_ITEM", currentMediaItem.toBundle());
                        }
                        resultData.putBundle("android.support.v4.media.argument.PLAYBACK_INFO", MediaSessionLegacyStub.this.mSession.getPlaybackInfo().toBundle());
                        MediaMetadata2 playlistMetadata = MediaSessionLegacyStub.this.mSession.getPlaylistMetadata();
                        if (playlistMetadata != null) {
                            resultData.putBundle("android.support.v4.media.argument.PLAYLIST_METADATA", playlistMetadata.toBundle());
                        }
                        if (!MediaSessionLegacyStub.this.mSession.isClosed()) {
                            cb.send(0, resultData);
                            return;
                        }
                        return;
                    }
                    synchronized (MediaSessionLegacyStub.this.mLock) {
                        MediaSessionLegacyStub.this.mConnectingControllers.remove(controllerInfo.getId());
                    }
                    if (MediaSessionLegacyStub.DEBUG) {
                        Log.d(MediaSessionLegacyStub.TAG, "Rejecting connection, controllerInfo=" + controllerInfo);
                    }
                    cb.send(-1, null);
                }
            }
        });
    }

    private void disconnect(Bundle extras) {
        final MediaSession2.ControllerInfo controllerInfo = createControllerInfo(extras);
        this.mSession.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MediaSessionLegacyStub.this.mSession.isClosed()) {
                    return;
                }
                MediaSessionLegacyStub.this.mSession.getCallback().onDisconnected(MediaSessionLegacyStub.this.mSession.getInstance(), controllerInfo);
            }
        });
    }

    final class ControllerLegacyCb extends MediaSession2.ControllerCb {
        private final IMediaControllerCallback mIControllerCallback;

        ControllerLegacyCb(@NonNull IMediaControllerCallback callback) {
            this.mIControllerCallback = callback;
        }

        @Override
        @NonNull
        IBinder getId() {
            return this.mIControllerCallback.asBinder();
        }

        @Override
        void onCustomLayoutChanged(List<MediaSession2.CommandButton> layout) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putParcelableArray("android.support.v4.media.argument.COMMAND_BUTTONS", MediaUtils2.convertCommandButtonListToParcelableArray(layout));
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.SET_CUSTOM_LAYOUT", bundle);
        }

        @Override
        void onPlaybackInfoChanged(MediaController2.PlaybackInfo info) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.PLAYBACK_INFO", info.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYBACK_INFO_CHANGED", bundle);
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.ALLOWED_COMMANDS", commands.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_ALLOWED_COMMANDS_CHANGED", bundle);
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.CUSTOM_COMMAND", command.toBundle());
            bundle.putBundle("android.support.v4.media.argument.ARGUMENTS", args);
            bundle.putParcelable("android.support.v4.media.argument.RESULT_RECEIVER", receiver);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.SEND_CUSTOM_COMMAND", bundle);
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.PLAYER_STATE", playerState);
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYER_STATE_CHANGED", bundle);
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYBACK_SPEED_CHANGED", bundle);
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int state, long bufferedPositionMs) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item.toBundle());
            bundle.putInt("android.support.v4.media.argument.BUFFERING_STATE", state);
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_BUFFERING_STATE_CHANGED", bundle);
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long position) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putLong("android.support.v4.media.argument.SEEK_POSITION", position);
            bundle.putParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT", MediaSessionLegacyStub.this.mSession.getPlaybackStateCompat());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_SEEK_COMPLETED", bundle);
        }

        @Override
        void onError(int errorCode, Bundle extras) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.ERROR_CODE", errorCode);
            bundle.putBundle("android.support.v4.media.argument.EXTRAS", extras);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_ERROR", bundle);
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.MEDIA_ITEM", item == null ? null : item.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED", bundle);
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putParcelableArray("android.support.v4.media.argument.PLAYLIST", MediaUtils2.convertMediaItem2ListToParcelableArray(playlist));
            bundle.putBundle("android.support.v4.media.argument.PLAYLIST_METADATA", metadata == null ? null : metadata.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYLIST_CHANGED", bundle);
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBundle("android.support.v4.media.argument.PLAYLIST_METADATA", metadata == null ? null : metadata.toBundle());
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_PLAYLIST_METADATA_CHANGED", bundle);
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.SHUFFLE_MODE", shuffleMode);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_SHUFFLE_MODE_CHANGED", bundle);
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putInt("android.support.v4.media.argument.REPEAT_MODE", repeatMode);
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_REPEAT_MODE_CHANGED", bundle);
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            Bundle bundle = null;
            if (routes != null) {
                bundle = new Bundle();
                bundle.putParcelableArray("android.support.v4.media.argument.ROUTE_BUNDLE", (Parcelable[]) routes.toArray(new Bundle[0]));
            }
            this.mIControllerCallback.onEvent("android.support.v4.media.session.event.ON_ROUTES_INFO_CHANGED", bundle);
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra) throws RemoteException {
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras) throws RemoteException {
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result, Bundle extras) throws RemoteException {
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras) throws RemoteException {
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result, Bundle extras) throws RemoteException {
        }

        @Override
        void onDisconnected() throws RemoteException {
            this.mIControllerCallback.onSessionDestroyed();
        }
    }
}
