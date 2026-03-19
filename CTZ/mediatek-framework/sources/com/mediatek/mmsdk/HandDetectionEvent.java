package com.mediatek.mmsdk;

import android.os.Parcel;
import android.os.Parcelable;

public class HandDetectionEvent implements Parcelable {
    public static final Parcelable.Creator<HandDetectionEvent> CREATOR = new Parcelable.Creator<HandDetectionEvent>() {
        @Override
        public HandDetectionEvent createFromParcel(Parcel parcel) {
            return new HandDetectionEvent(parcel);
        }

        @Override
        public HandDetectionEvent[] newArray(int i) {
            return new HandDetectionEvent[i];
        }
    };
    private Parcelable boundBox;
    private float confidence;
    private int id;
    private int pose;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.boundBox, i);
        parcel.writeFloat(this.confidence);
        parcel.writeInt(this.id);
        parcel.writeInt(this.pose);
    }

    public void readFromParcel(Parcel parcel) {
        this.boundBox = parcel.readParcelable(null);
        this.confidence = parcel.readFloat();
        this.id = parcel.readInt();
        this.pose = parcel.readInt();
    }

    private HandDetectionEvent(Parcel parcel) {
        this.boundBox = parcel.readParcelable(null);
        this.confidence = parcel.readFloat();
        this.id = parcel.readInt();
        this.pose = parcel.readInt();
    }
}
