package android.media;

import android.media.update.ApiLoader;
import android.media.update.MediaPlaylistAgentProvider;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class MediaPlaylistAgent {
    public static final int REPEAT_MODE_ALL = 2;
    public static final int REPEAT_MODE_GROUP = 3;
    public static final int REPEAT_MODE_NONE = 0;
    public static final int REPEAT_MODE_ONE = 1;
    public static final int SHUFFLE_MODE_ALL = 1;
    public static final int SHUFFLE_MODE_GROUP = 2;
    public static final int SHUFFLE_MODE_NONE = 0;
    private final MediaPlaylistAgentProvider mProvider = ApiLoader.getProvider().createMediaPlaylistAgent(this);

    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ShuffleMode {
    }

    public static abstract class PlaylistEventCallback {
        public void onPlaylistChanged(MediaPlaylistAgent mediaPlaylistAgent, List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        }

        public void onPlaylistMetadataChanged(MediaPlaylistAgent mediaPlaylistAgent, MediaMetadata2 mediaMetadata2) {
        }

        public void onShuffleModeChanged(MediaPlaylistAgent mediaPlaylistAgent, int i) {
        }

        public void onRepeatModeChanged(MediaPlaylistAgent mediaPlaylistAgent, int i) {
        }
    }

    public final void registerPlaylistEventCallback(Executor executor, PlaylistEventCallback playlistEventCallback) {
        this.mProvider.registerPlaylistEventCallback_impl(executor, playlistEventCallback);
    }

    public final void unregisterPlaylistEventCallback(PlaylistEventCallback playlistEventCallback) {
        this.mProvider.unregisterPlaylistEventCallback_impl(playlistEventCallback);
    }

    public final void notifyPlaylistChanged() {
        this.mProvider.notifyPlaylistChanged_impl();
    }

    public final void notifyPlaylistMetadataChanged() {
        this.mProvider.notifyPlaylistMetadataChanged_impl();
    }

    public final void notifyShuffleModeChanged() {
        this.mProvider.notifyShuffleModeChanged_impl();
    }

    public final void notifyRepeatModeChanged() {
        this.mProvider.notifyRepeatModeChanged_impl();
    }

    public List<MediaItem2> getPlaylist() {
        return this.mProvider.getPlaylist_impl();
    }

    public void setPlaylist(List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        this.mProvider.setPlaylist_impl(list, mediaMetadata2);
    }

    public MediaMetadata2 getPlaylistMetadata() {
        return this.mProvider.getPlaylistMetadata_impl();
    }

    public void updatePlaylistMetadata(MediaMetadata2 mediaMetadata2) {
        this.mProvider.updatePlaylistMetadata_impl(mediaMetadata2);
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

    public void skipToPlaylistItem(MediaItem2 mediaItem2) {
        this.mProvider.skipToPlaylistItem_impl(mediaItem2);
    }

    public void skipToPreviousItem() {
        this.mProvider.skipToPreviousItem_impl();
    }

    public void skipToNextItem() {
        this.mProvider.skipToNextItem_impl();
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

    public MediaItem2 getMediaItem(DataSourceDesc dataSourceDesc) {
        return this.mProvider.getMediaItem_impl(dataSourceDesc);
    }
}
