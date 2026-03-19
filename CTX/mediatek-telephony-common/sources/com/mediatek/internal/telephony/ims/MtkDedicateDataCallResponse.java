package com.mediatek.internal.telephony.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class MtkDedicateDataCallResponse implements Parcelable {
    public static final Parcelable.Creator<MtkDedicateDataCallResponse> CREATOR = new Parcelable.Creator<MtkDedicateDataCallResponse>() {
        @Override
        public MtkDedicateDataCallResponse createFromParcel(Parcel parcel) {
            return MtkDedicateDataCallResponse.readFrom(parcel);
        }

        @Override
        public MtkDedicateDataCallResponse[] newArray(int i) {
            return new MtkDedicateDataCallResponse[i];
        }
    };
    public static final String REASON_BEARER_ACTIVATION = "activation";
    public static final String REASON_BEARER_DEACTIVATION = "deactivation";
    public static final String REASON_BEARER_MODIFICATION = "modification";
    public int mActive;
    public int mBearerId;
    public int mCid;
    public int mDefaultCid;
    public int mFailCause;
    public int mInterfaceId;
    public MtkQosStatus mMtkQosStatus;
    public MtkTftStatus mMtkTftStatus;
    public String mPcscfAddress;
    public int mSignalingFlag;

    public enum SetupResult {
        SUCCESS,
        FAIL;

        public int failCause = 0;

        SetupResult() {
        }
    }

    public MtkDedicateDataCallResponse(int i, int i2, int i3, int i4, int i5, int i6, int i7, MtkQosStatus mtkQosStatus, MtkTftStatus mtkTftStatus, String str) {
        this.mInterfaceId = i;
        this.mDefaultCid = i2;
        this.mCid = i3;
        this.mActive = i4;
        this.mSignalingFlag = i5;
        this.mBearerId = i6;
        this.mFailCause = i7;
        this.mMtkQosStatus = mtkQosStatus;
        this.mMtkTftStatus = mtkTftStatus;
        this.mPcscfAddress = str;
    }

    public static MtkDedicateDataCallResponse readFrom(Parcel parcel) {
        String string;
        int i = parcel.readInt();
        int i2 = parcel.readInt();
        int i3 = parcel.readInt();
        int i4 = parcel.readInt();
        int i5 = parcel.readInt();
        int i6 = parcel.readInt();
        int i7 = parcel.readInt();
        MtkQosStatus from = parcel.readInt() == 1 ? MtkQosStatus.readFrom(parcel) : null;
        MtkTftStatus from2 = parcel.readInt() == 1 ? MtkTftStatus.readFrom(parcel) : null;
        if (parcel.readInt() == 1) {
            string = parcel.readString();
        } else {
            string = null;
        }
        return new MtkDedicateDataCallResponse(i, i2, i3, i4, i5, i6, i7, from, from2, string);
    }

    public String toString() {
        return "[interfaceId=" + this.mInterfaceId + ", defaultCid=" + this.mDefaultCid + ", cid=" + this.mCid + ", active=" + this.mActive + ", signalingFlag=" + this.mSignalingFlag + ", bearerId=" + this.mBearerId + ", failCause=" + this.mFailCause + ", QOS=" + this.mMtkQosStatus + ", TFT=" + this.mMtkTftStatus + ", PCSCF=" + this.mPcscfAddress + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mInterfaceId);
        parcel.writeInt(this.mDefaultCid);
        parcel.writeInt(this.mCid);
        parcel.writeInt(this.mActive);
        parcel.writeInt(this.mSignalingFlag);
        parcel.writeInt(this.mBearerId);
        parcel.writeInt(this.mFailCause);
        parcel.writeInt(this.mMtkQosStatus == null ? 0 : 1);
        if (this.mMtkQosStatus != null) {
            this.mMtkQosStatus.writeTo(parcel);
        }
        parcel.writeInt(this.mMtkTftStatus == null ? 0 : 1);
        if (this.mMtkTftStatus != null) {
            this.mMtkTftStatus.writeTo(parcel);
        }
        parcel.writeInt(this.mPcscfAddress == null ? 0 : 1);
        parcel.writeString(this.mPcscfAddress);
    }
}
