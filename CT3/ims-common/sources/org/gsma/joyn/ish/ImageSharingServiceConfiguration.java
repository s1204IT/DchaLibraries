package org.gsma.joyn.ish;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageSharingServiceConfiguration {
    public static final Parcelable.Creator<ImageSharingServiceConfiguration> CREATOR = new Parcelable.Creator<ImageSharingServiceConfiguration>() {
        @Override
        public ImageSharingServiceConfiguration createFromParcel(Parcel source) {
            return new ImageSharingServiceConfiguration(source);
        }

        @Override
        public ImageSharingServiceConfiguration[] newArray(int size) {
            return new ImageSharingServiceConfiguration[size];
        }
    };
    private long maxSize;
    private long warnSize;

    public ImageSharingServiceConfiguration(long warnSize, long maxSize) {
        this.warnSize = warnSize;
        this.maxSize = maxSize;
    }

    public ImageSharingServiceConfiguration(Parcel source) {
        this.warnSize = source.readLong();
        this.maxSize = source.readLong();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.warnSize);
        dest.writeLong(this.maxSize);
    }

    public long getWarnSize() {
        return this.warnSize;
    }

    public long getMaxSize() {
        return this.maxSize;
    }
}
