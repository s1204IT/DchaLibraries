package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;

public class PresSipResponse implements Parcelable {
    public static final Parcelable.Creator<PresSipResponse> CREATOR = new Parcelable.Creator<PresSipResponse>() {
        @Override
        public PresSipResponse createFromParcel(Parcel parcel) {
            return new PresSipResponse(parcel);
        }

        @Override
        public PresSipResponse[] newArray(int i) {
            return new PresSipResponse[i];
        }
    };
    private PresCmdId mCmdId;
    private String mReasonPhrase;
    private int mRequestId;
    private int mRetryAfter;
    private int mSipResponseCode;

    public PresCmdId getCmdId() {
        return this.mCmdId;
    }

    public void setCmdId(PresCmdId presCmdId) {
        this.mCmdId = presCmdId;
    }

    public int getRequestId() {
        return this.mRequestId;
    }

    public void setRequestId(int i) {
        this.mRequestId = i;
    }

    public int getSipResponseCode() {
        return this.mSipResponseCode;
    }

    public void setSipResponseCode(int i) {
        this.mSipResponseCode = i;
    }

    public String getReasonPhrase() {
        return this.mReasonPhrase;
    }

    public void setReasonPhrase(String str) {
        this.mReasonPhrase = str;
    }

    public int getRetryAfter() {
        return this.mRetryAfter;
    }

    public void setRetryAfter(int i) {
        this.mRetryAfter = i;
    }

    public PresSipResponse() {
        this.mCmdId = new PresCmdId();
        this.mRequestId = 0;
        this.mSipResponseCode = 0;
        this.mRetryAfter = 0;
        this.mReasonPhrase = "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRequestId);
        parcel.writeInt(this.mSipResponseCode);
        parcel.writeString(this.mReasonPhrase);
        parcel.writeParcelable(this.mCmdId, i);
        parcel.writeInt(this.mRetryAfter);
    }

    private PresSipResponse(Parcel parcel) {
        this.mCmdId = new PresCmdId();
        this.mRequestId = 0;
        this.mSipResponseCode = 0;
        this.mRetryAfter = 0;
        this.mReasonPhrase = "";
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mRequestId = parcel.readInt();
        this.mSipResponseCode = parcel.readInt();
        this.mReasonPhrase = parcel.readString();
        this.mCmdId = (PresCmdId) parcel.readParcelable(PresCmdId.class.getClassLoader());
        this.mRetryAfter = parcel.readInt();
    }
}
