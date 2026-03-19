package android.media.session;

import android.media.AudioAttributes;
import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableVolumeInfo implements Parcelable {
    public static final Parcelable.Creator<ParcelableVolumeInfo> CREATOR = new Parcelable.Creator<ParcelableVolumeInfo>() {
        @Override
        public ParcelableVolumeInfo createFromParcel(Parcel parcel) {
            return new ParcelableVolumeInfo(parcel);
        }

        @Override
        public ParcelableVolumeInfo[] newArray(int i) {
            return new ParcelableVolumeInfo[i];
        }
    };
    public AudioAttributes audioAttrs;
    public int controlType;
    public int currentVolume;
    public int maxVolume;
    public int volumeType;

    public ParcelableVolumeInfo(int i, AudioAttributes audioAttributes, int i2, int i3, int i4) {
        this.volumeType = i;
        this.audioAttrs = audioAttributes;
        this.controlType = i2;
        this.maxVolume = i3;
        this.currentVolume = i4;
    }

    public ParcelableVolumeInfo(Parcel parcel) {
        this.volumeType = parcel.readInt();
        this.controlType = parcel.readInt();
        this.maxVolume = parcel.readInt();
        this.currentVolume = parcel.readInt();
        this.audioAttrs = AudioAttributes.CREATOR.createFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.volumeType);
        parcel.writeInt(this.controlType);
        parcel.writeInt(this.maxVolume);
        parcel.writeInt(this.currentVolume);
        this.audioAttrs.writeToParcel(parcel, i);
    }
}
