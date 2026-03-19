package com.android.ims.internal.uce.options;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.ims.internal.uce.common.CapInfo;
import com.android.ims.internal.uce.common.StatusCode;

public class OptionsCmdStatus implements Parcelable {
    public static final Parcelable.Creator<OptionsCmdStatus> CREATOR = new Parcelable.Creator<OptionsCmdStatus>() {
        @Override
        public OptionsCmdStatus createFromParcel(Parcel parcel) {
            return new OptionsCmdStatus(parcel);
        }

        @Override
        public OptionsCmdStatus[] newArray(int i) {
            return new OptionsCmdStatus[i];
        }
    };
    private CapInfo mCapInfo;
    private OptionsCmdId mCmdId;
    private StatusCode mStatus;
    private int mUserData;

    public OptionsCmdId getCmdId() {
        return this.mCmdId;
    }

    public void setCmdId(OptionsCmdId optionsCmdId) {
        this.mCmdId = optionsCmdId;
    }

    public int getUserData() {
        return this.mUserData;
    }

    public void setUserData(int i) {
        this.mUserData = i;
    }

    public StatusCode getStatus() {
        return this.mStatus;
    }

    public void setStatus(StatusCode statusCode) {
        this.mStatus = statusCode;
    }

    public OptionsCmdStatus() {
        this.mStatus = new StatusCode();
        this.mCapInfo = new CapInfo();
        this.mCmdId = new OptionsCmdId();
        this.mUserData = 0;
    }

    public CapInfo getCapInfo() {
        return this.mCapInfo;
    }

    public void setCapInfo(CapInfo capInfo) {
        this.mCapInfo = capInfo;
    }

    public static OptionsCmdStatus getOptionsCmdStatusInstance() {
        return new OptionsCmdStatus();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mUserData);
        parcel.writeParcelable(this.mCmdId, i);
        parcel.writeParcelable(this.mStatus, i);
        parcel.writeParcelable(this.mCapInfo, i);
    }

    private OptionsCmdStatus(Parcel parcel) {
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mUserData = parcel.readInt();
        this.mCmdId = (OptionsCmdId) parcel.readParcelable(OptionsCmdId.class.getClassLoader());
        this.mStatus = (StatusCode) parcel.readParcelable(StatusCode.class.getClassLoader());
        this.mCapInfo = (CapInfo) parcel.readParcelable(CapInfo.class.getClassLoader());
    }
}
