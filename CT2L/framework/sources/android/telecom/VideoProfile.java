package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoProfile implements Parcelable {
    public static final Parcelable.Creator<VideoProfile> CREATOR = new Parcelable.Creator<VideoProfile>() {
        @Override
        public VideoProfile createFromParcel(Parcel source) {
            int state = source.readInt();
            int quality = source.readInt();
            VideoProfile.class.getClassLoader();
            return new VideoProfile(state, quality);
        }

        @Override
        public VideoProfile[] newArray(int size) {
            return new VideoProfile[size];
        }
    };
    public static final int QUALITY_DEFAULT = 4;
    public static final int QUALITY_HIGH = 1;
    public static final int QUALITY_LOW = 3;
    public static final int QUALITY_MEDIUM = 2;
    private final int mQuality;
    private final int mVideoState;

    public VideoProfile(int videoState) {
        this(videoState, 4);
    }

    public VideoProfile(int videoState, int quality) {
        this.mVideoState = videoState;
        this.mQuality = quality;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public int getQuality() {
        return this.mQuality;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mVideoState);
        dest.writeInt(this.mQuality);
    }

    public static class VideoState {
        public static final int AUDIO_ONLY = 0;
        public static final int BIDIRECTIONAL = 3;
        public static final int PAUSED = 4;
        public static final int RX_ENABLED = 2;
        public static final int TX_ENABLED = 1;

        public static boolean isAudioOnly(int videoState) {
            return (hasState(videoState, 1) || hasState(videoState, 2)) ? false : true;
        }

        public static boolean isTransmissionEnabled(int videoState) {
            return hasState(videoState, 1);
        }

        public static boolean isReceptionEnabled(int videoState) {
            return hasState(videoState, 2);
        }

        public static boolean isBidirectional(int videoState) {
            return hasState(videoState, 3);
        }

        public static boolean isPaused(int videoState) {
            return hasState(videoState, 4);
        }

        private static boolean hasState(int videoState, int state) {
            return (videoState & state) == state;
        }
    }
}
