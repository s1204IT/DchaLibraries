package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;

public class PresPublishTriggerType implements Parcelable {
    public static final Parcelable.Creator<PresPublishTriggerType> CREATOR = new Parcelable.Creator<PresPublishTriggerType>() {
        @Override
        public PresPublishTriggerType createFromParcel(Parcel parcel) {
            return new PresPublishTriggerType(parcel);
        }

        @Override
        public PresPublishTriggerType[] newArray(int i) {
            return new PresPublishTriggerType[i];
        }
    };
    public static final int UCE_PRES_PUBLISH_TRIGGER_ETAG_EXPIRED = 0;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_2G = 6;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_3G = 5;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_EHRPD = 3;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_HSPAPLUS = 4;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_IWLAN = 8;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_LTE_VOPS_DISABLED = 1;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_LTE_VOPS_ENABLED = 2;
    public static final int UCE_PRES_PUBLISH_TRIGGER_MOVE_TO_WLAN = 7;
    public static final int UCE_PRES_PUBLISH_TRIGGER_UNKNOWN = 9;
    private int mPublishTriggerType;

    public int getPublishTrigeerType() {
        return this.mPublishTriggerType;
    }

    public void setPublishTrigeerType(int i) {
        this.mPublishTriggerType = i;
    }

    public PresPublishTriggerType() {
        this.mPublishTriggerType = 9;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPublishTriggerType);
    }

    private PresPublishTriggerType(Parcel parcel) {
        this.mPublishTriggerType = 9;
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mPublishTriggerType = parcel.readInt();
    }
}
