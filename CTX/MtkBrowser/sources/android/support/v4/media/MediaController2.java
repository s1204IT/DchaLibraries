package android.support.v4.media;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaSession2;
import java.util.List;

@TargetApi(19)
public class MediaController2 implements AutoCloseable {
    private final SupportLibraryImpl mImpl;

    public static abstract class ControllerCallback {
        public void onAllowedCommandsChanged(MediaController2 mediaController2, SessionCommandGroup2 sessionCommandGroup2) {
        }

        public void onBufferingStateChanged(MediaController2 mediaController2, MediaItem2 mediaItem2, int i) {
        }

        public void onConnected(MediaController2 mediaController2, SessionCommandGroup2 sessionCommandGroup2) {
        }

        public void onCurrentMediaItemChanged(MediaController2 mediaController2, MediaItem2 mediaItem2) {
        }

        public void onCustomCommand(MediaController2 mediaController2, SessionCommand2 sessionCommand2, Bundle bundle, ResultReceiver resultReceiver) {
        }

        public void onCustomLayoutChanged(MediaController2 mediaController2, List<MediaSession2.CommandButton> list) {
        }

        public void onDisconnected(MediaController2 mediaController2) {
        }

        public void onError(MediaController2 mediaController2, int i, Bundle bundle) {
        }

        public void onPlaybackInfoChanged(MediaController2 mediaController2, PlaybackInfo playbackInfo) {
        }

        public void onPlaybackSpeedChanged(MediaController2 mediaController2, float f) {
        }

        public void onPlayerStateChanged(MediaController2 mediaController2, int i) {
        }

        public void onPlaylistChanged(MediaController2 mediaController2, List<MediaItem2> list, MediaMetadata2 mediaMetadata2) {
        }

        public void onPlaylistMetadataChanged(MediaController2 mediaController2, MediaMetadata2 mediaMetadata2) {
        }

        public void onRepeatModeChanged(MediaController2 mediaController2, int i) {
        }

        public void onRoutesInfoChanged(MediaController2 mediaController2, List<Bundle> list) {
        }

        public void onSeekCompleted(MediaController2 mediaController2, long j) {
        }

        public void onShuffleModeChanged(MediaController2 mediaController2, int i) {
        }
    }

    public static final class PlaybackInfo {
        private final AudioAttributesCompat mAudioAttrsCompat;
        private final int mControlType;
        private final int mCurrentVolume;
        private final int mMaxVolume;
        private final int mPlaybackType;

        PlaybackInfo(int i, AudioAttributesCompat audioAttributesCompat, int i2, int i3, int i4) {
            this.mPlaybackType = i;
            this.mAudioAttrsCompat = audioAttributesCompat;
            this.mControlType = i2;
            this.mMaxVolume = i3;
            this.mCurrentVolume = i4;
        }

        static PlaybackInfo createPlaybackInfo(int i, AudioAttributesCompat audioAttributesCompat, int i2, int i3, int i4) {
            return new PlaybackInfo(i, audioAttributesCompat, i2, i3, i4);
        }

        static PlaybackInfo fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            return createPlaybackInfo(bundle.getInt("android.media.audio_info.playback_type"), AudioAttributesCompat.fromBundle(bundle.getBundle("android.media.audio_info.audio_attrs")), bundle.getInt("android.media.audio_info.control_type"), bundle.getInt("android.media.audio_info.max_volume"), bundle.getInt("android.media.audio_info.current_volume"));
        }
    }

    interface SupportLibraryImpl extends AutoCloseable {
    }

    @Override
    public void close() {
        try {
            this.mImpl.close();
        } catch (Exception e) {
        }
    }
}
