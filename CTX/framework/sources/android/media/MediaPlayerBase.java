package android.media;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class MediaPlayerBase implements AutoCloseable {
    public static final int BUFFERING_STATE_BUFFERING_AND_PLAYABLE = 1;
    public static final int BUFFERING_STATE_BUFFERING_AND_STARVED = 2;
    public static final int BUFFERING_STATE_BUFFERING_COMPLETE = 3;
    public static final int BUFFERING_STATE_UNKNOWN = 0;
    public static final int PLAYER_STATE_ERROR = 3;
    public static final int PLAYER_STATE_IDLE = 0;
    public static final int PLAYER_STATE_PAUSED = 1;
    public static final int PLAYER_STATE_PLAYING = 2;
    public static final long UNKNOWN_TIME = -1;

    @Retention(RetentionPolicy.SOURCE)
    public @interface BuffState {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {
    }

    public abstract AudioAttributes getAudioAttributes();

    public abstract int getBufferingState();

    public abstract DataSourceDesc getCurrentDataSource();

    public abstract int getPlayerState();

    public abstract float getPlayerVolume();

    public abstract void loopCurrent(boolean z);

    public abstract void pause();

    public abstract void play();

    public abstract void prepare();

    public abstract void registerPlayerEventCallback(Executor executor, PlayerEventCallback playerEventCallback);

    public abstract void reset();

    public abstract void seekTo(long j);

    public abstract void setAudioAttributes(AudioAttributes audioAttributes);

    public abstract void setDataSource(DataSourceDesc dataSourceDesc);

    public abstract void setNextDataSource(DataSourceDesc dataSourceDesc);

    public abstract void setNextDataSources(List<DataSourceDesc> list);

    public abstract void setPlaybackSpeed(float f);

    public abstract void setPlayerVolume(float f);

    public abstract void skipToNext();

    public abstract void unregisterPlayerEventCallback(PlayerEventCallback playerEventCallback);

    public long getCurrentPosition() {
        return -1L;
    }

    public long getDuration() {
        return -1L;
    }

    public long getBufferedPosition() {
        return -1L;
    }

    public float getPlaybackSpeed() {
        return 1.0f;
    }

    public boolean isReversePlaybackSupported() {
        return false;
    }

    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    public static abstract class PlayerEventCallback {
        public void onCurrentDataSourceChanged(MediaPlayerBase mediaPlayerBase, DataSourceDesc dataSourceDesc) {
        }

        public void onMediaPrepared(MediaPlayerBase mediaPlayerBase, DataSourceDesc dataSourceDesc) {
        }

        public void onPlayerStateChanged(MediaPlayerBase mediaPlayerBase, int i) {
        }

        public void onBufferingStateChanged(MediaPlayerBase mediaPlayerBase, DataSourceDesc dataSourceDesc, int i) {
        }

        public void onPlaybackSpeedChanged(MediaPlayerBase mediaPlayerBase, float f) {
        }

        public void onSeekCompleted(MediaPlayerBase mediaPlayerBase, long j) {
        }
    }
}
