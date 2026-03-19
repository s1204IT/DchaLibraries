package org.gsma.joyn.vsh;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoSharingServiceConfiguration implements Parcelable {
    public static final Parcelable.Creator<VideoSharingServiceConfiguration> CREATOR = new Parcelable.Creator<VideoSharingServiceConfiguration>() {
        @Override
        public VideoSharingServiceConfiguration createFromParcel(Parcel source) {
            return new VideoSharingServiceConfiguration(source);
        }

        @Override
        public VideoSharingServiceConfiguration[] newArray(int size) {
            return new VideoSharingServiceConfiguration[size];
        }
    };
    private long maxTime;

    public VideoSharingServiceConfiguration(long maxTime) {
        this.maxTime = maxTime;
    }

    public VideoSharingServiceConfiguration(Parcel source) {
        this.maxTime = source.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.maxTime);
    }

    public long getMaxTime() {
        return this.maxTime;
    }
}
