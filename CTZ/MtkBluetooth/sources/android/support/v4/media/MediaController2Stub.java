package android.support.v4.media;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.IMediaController2;
import android.support.v4.media.MediaController2;
import android.support.v4.media.MediaSession2;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class MediaController2Stub extends IMediaController2.Stub {
    private static final boolean DEBUG = true;
    private static final String TAG = "MediaController2Stub";
    private final WeakReference<MediaController2ImplBase> mController;

    MediaController2Stub(MediaController2ImplBase controller) {
        this.mController = new WeakReference<>(controller);
    }

    @Override
    public void onCurrentMediaItemChanged(Bundle item) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyCurrentMediaItemChanged(MediaItem2.fromBundle(item));
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onPlayerStateChanged(long eventTimeMs, long positionMs, int state) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyPlayerStateChanges(eventTimeMs, positionMs, state);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyPlaybackSpeedChanges(eventTimeMs, positionMs, speed);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onBufferingStateChanged(Bundle itemBundle, int state, long bufferedPositionMs) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyBufferingStateChanged(MediaItem2.fromBundle(itemBundle), state, bufferedPositionMs);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onPlaylistChanged(List<Bundle> playlistBundle, Bundle metadataBundle) {
        try {
            MediaController2ImplBase controller = getController();
            if (playlistBundle == null) {
                Log.w(TAG, "onPlaylistChanged(): Ignoring null playlist from " + controller);
                return;
            }
            List<MediaItem2> playlist = new ArrayList<>();
            for (Bundle bundle : playlistBundle) {
                MediaItem2 item = MediaItem2.fromBundle(bundle);
                if (item == null) {
                    Log.w(TAG, "onPlaylistChanged(): Ignoring null item in playlist");
                } else {
                    playlist.add(item);
                }
            }
            MediaMetadata2 metadata = MediaMetadata2.fromBundle(metadataBundle);
            controller.notifyPlaylistChanges(playlist, metadata);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onPlaylistMetadataChanged(Bundle metadataBundle) throws RuntimeException {
        try {
            MediaController2ImplBase controller = getController();
            MediaMetadata2 metadata = MediaMetadata2.fromBundle(metadataBundle);
            controller.notifyPlaylistMetadataChanges(metadata);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyRepeatModeChanges(repeatMode);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onShuffleModeChanged(int shuffleMode) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyShuffleModeChanges(shuffleMode);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onPlaybackInfoChanged(Bundle playbackInfo) throws RuntimeException {
        Log.d(TAG, "onPlaybackInfoChanged");
        try {
            MediaController2ImplBase controller = getController();
            MediaController2.PlaybackInfo info = MediaController2.PlaybackInfo.fromBundle(playbackInfo);
            if (info == null) {
                Log.w(TAG, "onPlaybackInfoChanged(): Ignoring null playbackInfo");
            } else {
                controller.notifyPlaybackInfoChanges(info);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifySeekCompleted(eventTimeMs, positionMs, seekPositionMs);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onError(int errorCode, Bundle extras) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyError(errorCode, extras);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onRoutesInfoChanged(List<Bundle> routes) {
        try {
            MediaController2ImplBase controller = getController();
            controller.notifyRoutesInfoChanged(routes);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onConnected(IMediaSession2 sessionBinder, Bundle commandGroup, int playerState, Bundle currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed, long bufferedPositionMs, Bundle playbackInfo, int shuffleMode, int repeatMode, List<Bundle> itemBundleList, PendingIntent sessionActivity) throws Throwable {
        MediaController2ImplBase controller = this.mController.get();
        if (controller == null) {
            Log.d(TAG, "onConnected after MediaController2.close()");
            return;
        }
        List<MediaItem2> itemList = null;
        if (itemBundleList != null) {
            itemList = new ArrayList<>();
            for (int i = 0; i < itemBundleList.size(); i++) {
                MediaItem2 item = MediaItem2.fromBundle(itemBundleList.get(i));
                if (item != null) {
                    itemList.add(item);
                }
            }
        }
        controller.onConnectedNotLocked(sessionBinder, SessionCommandGroup2.fromBundle(commandGroup), playerState, MediaItem2.fromBundle(currentItem), positionEventTimeMs, positionMs, playbackSpeed, bufferedPositionMs, MediaController2.PlaybackInfo.fromBundle(playbackInfo), repeatMode, shuffleMode, itemList, sessionActivity);
    }

    @Override
    public void onDisconnected() {
        MediaController2ImplBase controller = this.mController.get();
        if (controller == null) {
            Log.d(TAG, "onDisconnected after MediaController2.close()");
        } else {
            controller.getInstance().close();
        }
    }

    @Override
    public void onCustomLayoutChanged(List<Bundle> commandButtonlist) {
        if (commandButtonlist == null) {
            Log.w(TAG, "onCustomLayoutChanged(): Ignoring null commandButtonlist");
            return;
        }
        try {
            MediaController2ImplBase controller = getController();
            if (controller == null) {
                return;
            }
            List<MediaSession2.CommandButton> layout = new ArrayList<>();
            for (int i = 0; i < commandButtonlist.size(); i++) {
                MediaSession2.CommandButton button = MediaSession2.CommandButton.fromBundle(commandButtonlist.get(i));
                if (button != null) {
                    layout.add(button);
                }
            }
            controller.onCustomLayoutChanged(layout);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onAllowedCommandsChanged(Bundle commandsBundle) {
        try {
            MediaController2ImplBase controller = getController();
            if (controller == null) {
                return;
            }
            SessionCommandGroup2 commands = SessionCommandGroup2.fromBundle(commandsBundle);
            if (commands == null) {
                Log.w(TAG, "onAllowedCommandsChanged(): Ignoring null commands");
            } else {
                controller.onAllowedCommandsChanged(commands);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onCustomCommand(Bundle commandBundle, Bundle args, ResultReceiver receiver) {
        try {
            MediaController2ImplBase controller = getController();
            SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
            if (command == null) {
                Log.w(TAG, "onCustomCommand(): Ignoring null command");
            } else {
                controller.onCustomCommand(command, args, receiver);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onGetLibraryRootDone(final Bundle rootHints, final String rootMediaId, final Bundle rootExtra) throws RuntimeException {
        try {
            final MediaBrowser2 browser = getBrowser();
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onGetLibraryRootDone(browser, rootHints, rootMediaId, rootExtra);
                }
            });
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onGetItemDone(final String mediaId, final Bundle itemBundle) throws RuntimeException {
        if (mediaId == null) {
            Log.w(TAG, "onGetItemDone(): Ignoring null mediaId");
            return;
        }
        try {
            final MediaBrowser2 browser = getBrowser();
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onGetItemDone(browser, mediaId, MediaItem2.fromBundle(itemBundle));
                }
            });
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onGetChildrenDone(final String parentId, final int page, final int pageSize, final List<Bundle> itemBundleList, final Bundle extras) throws RuntimeException {
        if (parentId == null) {
            Log.w(TAG, "onGetChildrenDone(): Ignoring null parentId");
            return;
        }
        try {
            final MediaBrowser2 browser = getBrowser();
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onGetChildrenDone(browser, parentId, page, pageSize, MediaUtils2.convertBundleListToMediaItem2List(itemBundleList), extras);
                }
            });
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onSearchResultChanged(final String query, final int itemCount, final Bundle extras) throws RuntimeException {
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "onSearchResultChanged(): Ignoring empty query");
            return;
        }
        try {
            final MediaBrowser2 browser = getBrowser();
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onSearchResultChanged(browser, query, itemCount, extras);
                }
            });
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onGetSearchResultDone(final String query, final int page, final int pageSize, final List<Bundle> itemBundleList, final Bundle extras) throws RuntimeException {
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "onGetSearchResultDone(): Ignoring empty query");
            return;
        }
        try {
            final MediaBrowser2 browser = getBrowser();
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onGetSearchResultDone(browser, query, page, pageSize, MediaUtils2.convertBundleListToMediaItem2List(itemBundleList), extras);
                }
            });
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    @Override
    public void onChildrenChanged(final String parentId, final int itemCount, final Bundle extras) {
        if (parentId == null) {
            Log.w(TAG, "onChildrenChanged(): Ignoring null parentId");
            return;
        }
        try {
            final MediaBrowser2 browser = getBrowser();
            if (browser == null) {
                return;
            }
            browser.getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    browser.getCallback().onChildrenChanged(browser, parentId, itemCount, extras);
                }
            });
        } catch (IllegalStateException e) {
            Log.w(TAG, "Don't fail silently here. Highly likely a bug");
        }
    }

    public void destroy() {
        this.mController.clear();
    }

    private MediaController2ImplBase getController() throws IllegalStateException {
        MediaController2ImplBase controller = this.mController.get();
        if (controller == null) {
            throw new IllegalStateException("Controller is released");
        }
        return controller;
    }

    private MediaBrowser2 getBrowser() throws IllegalStateException {
        MediaController2ImplBase controller = getController();
        if (controller.getInstance() instanceof MediaBrowser2) {
            return (MediaBrowser2) controller.getInstance();
        }
        return null;
    }
}
