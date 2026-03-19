package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class PresResInstanceInfo implements Parcelable {
    public static final Parcelable.Creator<PresResInstanceInfo> CREATOR = new Parcelable.Creator<PresResInstanceInfo>() {
        @Override
        public PresResInstanceInfo createFromParcel(Parcel parcel) {
            return new PresResInstanceInfo(parcel);
        }

        @Override
        public PresResInstanceInfo[] newArray(int i) {
            return new PresResInstanceInfo[i];
        }
    };
    public static final int UCE_PRES_RES_INSTANCE_STATE_ACTIVE = 0;
    public static final int UCE_PRES_RES_INSTANCE_STATE_PENDING = 1;
    public static final int UCE_PRES_RES_INSTANCE_STATE_TERMINATED = 2;
    public static final int UCE_PRES_RES_INSTANCE_STATE_UNKNOWN = 3;
    public static final int UCE_PRES_RES_INSTANCE_UNKNOWN = 4;
    private String mId;
    private String mPresentityUri;
    private String mReason;
    private int mResInstanceState;
    private PresTupleInfo[] mTupleInfoArray;

    public int getResInstanceState() {
        return this.mResInstanceState;
    }

    public void setResInstanceState(int i) {
        this.mResInstanceState = i;
    }

    public String getResId() {
        return this.mId;
    }

    public void setResId(String str) {
        this.mId = str;
    }

    public String getReason() {
        return this.mReason;
    }

    public void setReason(String str) {
        this.mReason = str;
    }

    public String getPresentityUri() {
        return this.mPresentityUri;
    }

    public void setPresentityUri(String str) {
        this.mPresentityUri = str;
    }

    public PresTupleInfo[] getTupleInfo() {
        return this.mTupleInfoArray;
    }

    public void setTupleInfo(PresTupleInfo[] presTupleInfoArr) {
        this.mTupleInfoArray = new PresTupleInfo[presTupleInfoArr.length];
        this.mTupleInfoArray = presTupleInfoArr;
    }

    public PresResInstanceInfo() {
        this.mId = "";
        this.mReason = "";
        this.mPresentityUri = "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mId);
        parcel.writeString(this.mReason);
        parcel.writeInt(this.mResInstanceState);
        parcel.writeString(this.mPresentityUri);
        parcel.writeParcelableArray(this.mTupleInfoArray, i);
    }

    private PresResInstanceInfo(Parcel parcel) {
        this.mId = "";
        this.mReason = "";
        this.mPresentityUri = "";
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mId = parcel.readString();
        this.mReason = parcel.readString();
        this.mResInstanceState = parcel.readInt();
        this.mPresentityUri = parcel.readString();
        Parcelable[] parcelableArray = parcel.readParcelableArray(PresTupleInfo.class.getClassLoader());
        this.mTupleInfoArray = new PresTupleInfo[0];
        if (parcelableArray != null) {
            this.mTupleInfoArray = (PresTupleInfo[]) Arrays.copyOf(parcelableArray, parcelableArray.length, PresTupleInfo[].class);
        }
    }
}
