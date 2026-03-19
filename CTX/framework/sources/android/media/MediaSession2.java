package android.media;

import android.app.PendingIntent;
import android.content.Context;
import android.media.MediaSession2;
import android.media.update.ApiLoader;
import android.media.update.MediaSession2Provider;
import android.media.update.ProviderCreator;
import android.net.Uri;
import android.os.Bundle;
import android.os.IInterface;
import android.os.ResultReceiver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

public class MediaSession2 implements AutoCloseable {
    public static final int ERROR_CODE_ACTION_ABORTED = 10;
    public static final int ERROR_CODE_APP_ERROR = 1;
    public static final int ERROR_CODE_AUTHENTICATION_EXPIRED = 3;
    public static final int ERROR_CODE_CONCURRENT_STREAM_LIMIT = 5;
    public static final int ERROR_CODE_CONTENT_ALREADY_PLAYING = 8;
    public static final int ERROR_CODE_END_OF_QUEUE = 11;
    public static final int ERROR_CODE_NOT_AVAILABLE_IN_REGION = 7;
    public static final int ERROR_CODE_NOT_SUPPORTED = 2;
    public static final int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = 6;
    public static final int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = 4;
    public static final int ERROR_CODE_SETUP_REQUIRED = 12;
    public static final int ERROR_CODE_SKIP_LIMIT_REACHED = 9;
    public static final int ERROR_CODE_UNKNOWN_ERROR = 0;
    private final MediaSession2Provider mProvider;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {
    }

    public interface OnDataSourceMissingHelper {
        DataSourceDesc onDataSourceMissing(MediaSession2 mediaSession2, MediaItem2 mediaItem2);
    }

    public static abstract class SessionCallback {
        public SessionCommandGroup2 onConnect(MediaSession2 mediaSession2, ControllerInfo controllerInfo) {
            SessionCommandGroup2 sessionCommandGroup2 = new SessionCommandGroup2();
            sessionCommandGroup2.addAllPredefinedCommands();
            return sessionCommandGroup2;
        }

        public void onDisconnected(MediaSession2 mediaSession2, ControllerInfo controllerInfo) {
        }

        public boolean onCommandRequest(MediaSession2 mediaSession2, ControllerInfo controllerInfo, SessionCommand2 sessionCommand2) {
            return true;
        }

        public void onSetRating(MediaSession2 mediaSession2, ControllerInfo controllerInfo, String str, Rating2 rating2) {
        }

        public void onCustomCommand(MediaSession2 mediaSession2, ControllerInfo controllerInfo, SessionCommand2 sessionCommand2, Bundle bundle, ResultReceiver resultReceiver) {
        }

        public void onPlayFromMediaId(MediaSession2 mediaSession2, ControllerInfo controllerInfo, String str, Bundle bundle) {
        }

        public void onPlayFromSearch(MediaSession2 mediaSession2, ControllerInfo controllerInfo, String str, Bundle bundle) {
        }

        public void onPlayFromUri(MediaSession2 mediaSession2, ControllerInfo controllerInfo, Uri uri, Bundle bundle) {
        }

        public void onPrepareFromMediaId(MediaSession2 mediaSession2, ControllerInfo controllerInfo, String str, Bundle bundle) {
        }

        public void onPrepareFromSearch(MediaSession2 mediaSession2, ControllerInfo controllerInfo, String str, Bundle bundle) {
        }

        public void onPrepareFromUri(MediaSession2 mediaSession2, ControllerInfo controllerInfo, Uri uri, Bundle bundle) {
        }

        public void onFastForward(MediaSession2 mediaSession2) {
        }

        public void onRewind(MediaSession2 mediaSession2) {
        }

        public void onCurrentMediaItemChanged(MediaSession2 mediaSession2, MediaPlayerBase mediaPlayerBase, MediaItem2 mediaItem2) {
        }

        public void onMediaPrepared(MediaSession2 mediaSession2, MediaPlayerBase mediaPlayerBase, MediaItem2 mediaItem2) {
        }

        public void onPlayerStateChanged(MediaSession2 mediaSession2, MediaPlayerBase mediaPlayerBase, int i) {
        }

        public void onBufferingStateChanged(MediaSession2 mediaSession2, MediaPlayerBase mediaPlayerBase, MediaItem2 mediaItem2, int i) {
        }

        public void onPlaybackSpeedChanged(MediaSession2 mediaSession2, MediaPlayerBase mediaPlayerBase, float f) {
        }

        public void onSeekCompleted(MediaSession2 mediaSession2, MediaPlayerBase mediaPlayerBase, long j) {
        }

        public void onPlaylistChanged(MediaSession2 mediaSession2, MediaPlaylistAgent mediaPlaylistAgent, List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        }

        public void onPlaylistMetadataChanged(MediaSession2 mediaSession2, MediaPlaylistAgent mediaPlaylistAgent, MediaMetadata2 mediaMetadata2) {
        }

        public void onShuffleModeChanged(MediaSession2 mediaSession2, MediaPlaylistAgent mediaPlaylistAgent, int i) {
        }

        public void onRepeatModeChanged(MediaSession2 mediaSession2, MediaPlaylistAgent mediaPlaylistAgent, int i) {
        }
    }

    static abstract class BuilderBase<T extends MediaSession2, U extends BuilderBase<T, U, C>, C extends SessionCallback> {
        private final MediaSession2Provider.BuilderBaseProvider<T, C> mProvider;

        BuilderBase(ProviderCreator<BuilderBase<T, U, C>, MediaSession2Provider.BuilderBaseProvider<T, C>> providerCreator) {
            this.mProvider = providerCreator.createProvider(this);
        }

        U setPlayer(MediaPlayerBase mediaPlayerBase) {
            this.mProvider.setPlayer_impl(mediaPlayerBase);
            return this;
        }

        U setPlaylistAgent(MediaPlaylistAgent mediaPlaylistAgent) {
            this.mProvider.setPlaylistAgent_impl(mediaPlaylistAgent);
            return this;
        }

        U setVolumeProvider(VolumeProvider2 volumeProvider2) {
            this.mProvider.setVolumeProvider_impl(volumeProvider2);
            return this;
        }

        U setSessionActivity(PendingIntent pendingIntent) {
            this.mProvider.setSessionActivity_impl(pendingIntent);
            return this;
        }

        U setId(String str) {
            this.mProvider.setId_impl(str);
            return this;
        }

        U setSessionCallback(Executor executor, C c) {
            this.mProvider.setSessionCallback_impl(executor, c);
            return this;
        }

        T build() {
            return (T) this.mProvider.build_impl();
        }
    }

    public static final class Builder extends BuilderBase<MediaSession2, Builder, SessionCallback> {
        public Builder(final Context context) {
            super(new ProviderCreator() {
                @Override
                public final Object createProvider(Object obj) {
                    return ApiLoader.getProvider().createMediaSession2Builder(context, (MediaSession2.Builder) ((MediaSession2.BuilderBase) obj));
                }
            });
        }

        @Override
        public Builder setPlayer(MediaPlayerBase mediaPlayerBase) {
            return (Builder) super.setPlayer(mediaPlayerBase);
        }

        @Override
        public Builder setPlaylistAgent(MediaPlaylistAgent mediaPlaylistAgent) {
            return (Builder) super.setPlaylistAgent(mediaPlaylistAgent);
        }

        @Override
        public Builder setVolumeProvider(VolumeProvider2 volumeProvider2) {
            return (Builder) super.setVolumeProvider(volumeProvider2);
        }

        @Override
        public Builder setSessionActivity(PendingIntent pendingIntent) {
            return (Builder) super.setSessionActivity(pendingIntent);
        }

        @Override
        public Builder setId(String str) {
            return (Builder) super.setId(str);
        }

        @Override
        public Builder setSessionCallback(Executor executor, SessionCallback sessionCallback) {
            return (Builder) super.setSessionCallback(executor, sessionCallback);
        }

        @Override
        public MediaSession2 build() {
            return super.build();
        }
    }

    public static final class ControllerInfo {
        private final MediaSession2Provider.ControllerInfoProvider mProvider;

        public ControllerInfo(Context context, int i, int i2, String str, IInterface iInterface) {
            this.mProvider = ApiLoader.getProvider().createMediaSession2ControllerInfo(context, this, i, i2, str, iInterface);
        }

        public String getPackageName() {
            return this.mProvider.getPackageName_impl();
        }

        public int getUid() {
            return this.mProvider.getUid_impl();
        }

        public boolean isTrusted() {
            return this.mProvider.isTrusted_impl();
        }

        public MediaSession2Provider.ControllerInfoProvider getProvider() {
            return this.mProvider;
        }

        public int hashCode() {
            return this.mProvider.hashCode_impl();
        }

        public boolean equals(Object obj) {
            return this.mProvider.equals_impl(obj);
        }

        public String toString() {
            return this.mProvider.toString_impl();
        }
    }

    public static final class CommandButton {
        private final MediaSession2Provider.CommandButtonProvider mProvider;

        public CommandButton(MediaSession2Provider.CommandButtonProvider commandButtonProvider) {
            this.mProvider = commandButtonProvider;
        }

        public SessionCommand2 getCommand() {
            return this.mProvider.getCommand_impl();
        }

        public int getIconResId() {
            return this.mProvider.getIconResId_impl();
        }

        public String getDisplayName() {
            return this.mProvider.getDisplayName_impl();
        }

        public Bundle getExtras() {
            return this.mProvider.getExtras_impl();
        }

        public boolean isEnabled() {
            return this.mProvider.isEnabled_impl();
        }

        public MediaSession2Provider.CommandButtonProvider getProvider() {
            return this.mProvider;
        }

        public static final class Builder {
            private final MediaSession2Provider.CommandButtonProvider.BuilderProvider mProvider = ApiLoader.getProvider().createMediaSession2CommandButtonBuilder(this);

            public Builder setCommand(SessionCommand2 sessionCommand2) {
                return this.mProvider.setCommand_impl(sessionCommand2);
            }

            public Builder setIconResId(int i) {
                return this.mProvider.setIconResId_impl(i);
            }

            public Builder setDisplayName(String str) {
                return this.mProvider.setDisplayName_impl(str);
            }

            public Builder setEnabled(boolean z) {
                return this.mProvider.setEnabled_impl(z);
            }

            public Builder setExtras(Bundle bundle) {
                return this.mProvider.setExtras_impl(bundle);
            }

            public CommandButton build() {
                return this.mProvider.build_impl();
            }
        }
    }

    public MediaSession2(MediaSession2Provider mediaSession2Provider) {
        this.mProvider = mediaSession2Provider;
    }

    public MediaSession2Provider getProvider() {
        return this.mProvider;
    }

    public void updatePlayer(MediaPlayerBase mediaPlayerBase, MediaPlaylistAgent mediaPlaylistAgent, VolumeProvider2 volumeProvider2) {
        this.mProvider.updatePlayer_impl(mediaPlayerBase, mediaPlaylistAgent, volumeProvider2);
    }

    @Override
    public void close() {
        this.mProvider.close_impl();
    }

    public MediaPlayerBase getPlayer() {
        return this.mProvider.getPlayer_impl();
    }

    public MediaPlaylistAgent getPlaylistAgent() {
        return this.mProvider.getPlaylistAgent_impl();
    }

    public VolumeProvider2 getVolumeProvider() {
        return this.mProvider.getVolumeProvider_impl();
    }

    public SessionToken2 getToken() {
        return this.mProvider.getToken_impl();
    }

    public List<ControllerInfo> getConnectedControllers() {
        return this.mProvider.getConnectedControllers_impl();
    }

    public void setAudioFocusRequest(AudioFocusRequest audioFocusRequest) {
    }

    public void setCustomLayout(ControllerInfo controllerInfo, List<CommandButton> list) {
        this.mProvider.setCustomLayout_impl(controllerInfo, list);
    }

    public void setAllowedCommands(ControllerInfo controllerInfo, SessionCommandGroup2 sessionCommandGroup2) {
        this.mProvider.setAllowedCommands_impl(controllerInfo, sessionCommandGroup2);
    }

    public void sendCustomCommand(SessionCommand2 sessionCommand2, Bundle bundle) {
        this.mProvider.sendCustomCommand_impl(sessionCommand2, bundle);
    }

    public void sendCustomCommand(ControllerInfo controllerInfo, SessionCommand2 sessionCommand2, Bundle bundle, ResultReceiver resultReceiver) {
        this.mProvider.sendCustomCommand_impl(controllerInfo, sessionCommand2, bundle, resultReceiver);
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

    public void seekTo(long j) {
        this.mProvider.seekTo_impl(j);
    }

    public void skipForward() {
    }

    public void skipBackward() {
    }

    public void notifyError(int i, Bundle bundle) {
        this.mProvider.notifyError_impl(i, bundle);
    }

    public int getPlayerState() {
        return this.mProvider.getPlayerState_impl();
    }

    public long getCurrentPosition() {
        return this.mProvider.getCurrentPosition_impl();
    }

    public long getBufferedPosition() {
        return this.mProvider.getBufferedPosition_impl();
    }

    public int getBufferingState() {
        return 0;
    }

    public float getPlaybackSpeed() {
        return -1.0f;
    }

    public void setPlaybackSpeed(float f) {
    }

    public void setOnDataSourceMissingHelper(OnDataSourceMissingHelper onDataSourceMissingHelper) {
        this.mProvider.setOnDataSourceMissingHelper_impl(onDataSourceMissingHelper);
    }

    public void clearOnDataSourceMissingHelper() {
        this.mProvider.clearOnDataSourceMissingHelper_impl();
    }

    public List<MediaItem2> getPlaylist() {
        return this.mProvider.getPlaylist_impl();
    }

    public void setPlaylist(List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        this.mProvider.setPlaylist_impl(list, mediaMetadata2);
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
        return this.mProvider.getCurrentPlaylistItem_impl();
    }

    public void updatePlaylistMetadata(MediaMetadata2 mediaMetadata2) {
        this.mProvider.updatePlaylistMetadata_impl(mediaMetadata2);
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
