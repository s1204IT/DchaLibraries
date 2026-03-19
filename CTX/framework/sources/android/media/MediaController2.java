package android.media;

import android.app.PendingIntent;
import android.content.Context;
import android.media.MediaSession2;
import android.media.update.ApiLoader;
import android.media.update.MediaController2Provider;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import java.util.List;
import java.util.concurrent.Executor;

public class MediaController2 implements AutoCloseable {
    private final MediaController2Provider mProvider;

    public static abstract class ControllerCallback {
        public void onConnected(MediaController2 mediaController2, SessionCommandGroup2 sessionCommandGroup2) {
        }

        public void onDisconnected(MediaController2 mediaController2) {
        }

        public void onCustomLayoutChanged(MediaController2 mediaController2, List<MediaSession2.CommandButton> list) {
        }

        public void onPlaybackInfoChanged(MediaController2 mediaController2, PlaybackInfo playbackInfo) {
        }

        public void onAllowedCommandsChanged(MediaController2 mediaController2, SessionCommandGroup2 sessionCommandGroup2) {
        }

        public void onCustomCommand(MediaController2 mediaController2, SessionCommand2 sessionCommand2, Bundle bundle, ResultReceiver resultReceiver) {
        }

        public void onPlayerStateChanged(MediaController2 mediaController2, int i) {
        }

        public void onPlaybackSpeedChanged(MediaController2 mediaController2, float f) {
        }

        public void onBufferingStateChanged(MediaController2 mediaController2, MediaItem2 mediaItem2, int i) {
        }

        public void onSeekCompleted(MediaController2 mediaController2, long j) {
        }

        public void onError(MediaController2 mediaController2, int i, Bundle bundle) {
        }

        public void onCurrentMediaItemChanged(MediaController2 mediaController2, MediaItem2 mediaItem2) {
        }

        public void onPlaylistChanged(MediaController2 mediaController2, List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        }

        public void onPlaylistMetadataChanged(MediaController2 mediaController2, MediaMetadata2 mediaMetadata2) {
        }

        public void onShuffleModeChanged(MediaController2 mediaController2, int i) {
        }

        public void onRepeatModeChanged(MediaController2 mediaController2, int i) {
        }
    }

    public static final class PlaybackInfo {
        public static final int PLAYBACK_TYPE_LOCAL = 1;
        public static final int PLAYBACK_TYPE_REMOTE = 2;
        private final MediaController2Provider.PlaybackInfoProvider mProvider;

        public PlaybackInfo(MediaController2Provider.PlaybackInfoProvider playbackInfoProvider) {
            this.mProvider = playbackInfoProvider;
        }

        public MediaController2Provider.PlaybackInfoProvider getProvider() {
            return this.mProvider;
        }

        public int getPlaybackType() {
            return this.mProvider.getPlaybackType_impl();
        }

        public AudioAttributes getAudioAttributes() {
            return this.mProvider.getAudioAttributes_impl();
        }

        public int getControlType() {
            return this.mProvider.getControlType_impl();
        }

        public int getMaxVolume() {
            return this.mProvider.getMaxVolume_impl();
        }

        public int getCurrentVolume() {
            return this.mProvider.getCurrentVolume_impl();
        }
    }

    public MediaController2(Context context, SessionToken2 sessionToken2, Executor executor, ControllerCallback controllerCallback) {
        this.mProvider = createProvider(context, sessionToken2, executor, controllerCallback);
        this.mProvider.initialize();
    }

    MediaController2Provider createProvider(Context context, SessionToken2 sessionToken2, Executor executor, ControllerCallback controllerCallback) {
        return ApiLoader.getProvider().createMediaController2(context, this, sessionToken2, executor, controllerCallback);
    }

    @Override
    public void close() {
        this.mProvider.close_impl();
    }

    public MediaController2Provider getProvider() {
        return this.mProvider;
    }

    public SessionToken2 getSessionToken() {
        return this.mProvider.getSessionToken_impl();
    }

    public boolean isConnected() {
        return this.mProvider.isConnected_impl();
    }

    public void play() {
        this.mProvider.play_impl();
    }

    public void pause() {
        this.mProvider.pause_impl();
    }

    public void stop() {
        this.mProvider.stop_impl();
    }

    public void prepare() {
        this.mProvider.prepare_impl();
    }

    public void fastForward() {
        this.mProvider.fastForward_impl();
    }

    public void rewind() {
        this.mProvider.rewind_impl();
    }

    public void seekTo(long j) {
        this.mProvider.seekTo_impl(j);
    }

    public void skipForward() {
    }

    public void skipBackward() {
    }

    public void playFromMediaId(String str, Bundle bundle) {
        this.mProvider.playFromMediaId_impl(str, bundle);
    }

    public void playFromSearch(String str, Bundle bundle) {
        this.mProvider.playFromSearch_impl(str, bundle);
    }

    public void playFromUri(Uri uri, Bundle bundle) {
        this.mProvider.playFromUri_impl(uri, bundle);
    }

    public void prepareFromMediaId(String str, Bundle bundle) {
        this.mProvider.prepareFromMediaId_impl(str, bundle);
    }

    public void prepareFromSearch(String str, Bundle bundle) {
        this.mProvider.prepareFromSearch_impl(str, bundle);
    }

    public void prepareFromUri(Uri uri, Bundle bundle) {
        this.mProvider.prepareFromUri_impl(uri, bundle);
    }

    public void setVolumeTo(int i, int i2) {
        this.mProvider.setVolumeTo_impl(i, i2);
    }

    public void adjustVolume(int i, int i2) {
        this.mProvider.adjustVolume_impl(i, i2);
    }

    public PendingIntent getSessionActivity() {
        return this.mProvider.getSessionActivity_impl();
    }

    public int getPlayerState() {
        return this.mProvider.getPlayerState_impl();
    }

    public long getCurrentPosition() {
        return this.mProvider.getCurrentPosition_impl();
    }

    public float getPlaybackSpeed() {
        return this.mProvider.getPlaybackSpeed_impl();
    }

    public void setPlaybackSpeed(float f) {
    }

    public int getBufferingState() {
        return 0;
    }

    public long getBufferedPosition() {
        return this.mProvider.getBufferedPosition_impl();
    }

    public PlaybackInfo getPlaybackInfo() {
        return this.mProvider.getPlaybackInfo_impl();
    }

    public void setRating(String str, Rating2 rating2) {
        this.mProvider.setRating_impl(str, rating2);
    }

    public void sendCustomCommand(SessionCommand2 sessionCommand2, Bundle bundle, ResultReceiver resultReceiver) {
        this.mProvider.sendCustomCommand_impl(sessionCommand2, bundle, resultReceiver);
    }

    public List<MediaItem2> getPlaylist() {
        return this.mProvider.getPlaylist_impl();
    }

    public void setPlaylist(List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        this.mProvider.setPlaylist_impl(list, mediaMetadata2);
    }

    public void updatePlaylistMetadata(MediaMetadata2 mediaMetadata2) {
        this.mProvider.updatePlaylistMetadata_impl(mediaMetadata2);
    }

    public MediaMetadata2 getPlaylistMetadata() {
        return this.mProvider.getPlaylistMetadata_impl();
    }

    public void addPlaylistItem(int i, MediaItem2 mediaItem2) {
        this.mProvider.addPlaylistItem_impl(i, mediaItem2);
    }

    public void removePlaylistItem(MediaItem2 mediaItem2) {
        this.mProvider.removePlaylistItem_impl(mediaItem2);
    }

    public void replacePlaylistItem(int i, MediaItem2 mediaItem2) {
        this.mProvider.replacePlaylistItem_impl(i, mediaItem2);
    }

    public MediaItem2 getCurrentMediaItem() {
        return this.mProvider.getCurrentMediaItem_impl();
    }

    public void skipToPreviousItem() {
        this.mProvider.skipToPreviousItem_impl();
    }

    public void skipToNextItem() {
        this.mProvider.skipToNextItem_impl();
    }

    public void skipToPlaylistItem(MediaItem2 mediaItem2) {
        this.mProvider.skipToPlaylistItem_impl(mediaItem2);
    }

    public int getRepeatMode() {
        return this.mProvider.getRepeatMode_impl();
    }

    public void setRepeatMode(int i) {
        this.mProvider.setRepeatMode_impl(i);
    }

    public int getShuffleMode() {
        return this.mProvider.getShuffleMode_impl();
    }

    public void setShuffleMode(int i) {
        this.mProvider.setShuffleMode_impl(i);
    }
}
