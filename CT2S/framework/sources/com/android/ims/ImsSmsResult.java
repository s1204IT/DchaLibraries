package com.android.ims;

import android.os.Parcel;
import android.os.Parcelable;

public class ImsSmsResult implements Parcelable {
    public static final Parcelable.Creator<ImsSmsResult> CREATOR = new Parcelable.Creator<ImsSmsResult>() {
        @Override
        public ImsSmsResult createFromParcel(Parcel in) {
            return new ImsSmsResult(in);
        }

        @Override
        public ImsSmsResult[] newArray(int size) {
            return new ImsSmsResult[size];
        }
    };
    public String mAckPdu;
    public int mErrorCode;
    public int mMessageRef;

    public ImsSmsResult() {
    }

    public ImsSmsResult(int msgRef, String ackPdu, int errCode) {
        this.mMessageRef = msgRef;
        this.mAckPdu = ackPdu;
        this.mErrorCode = errCode;
    }

    public ImsSmsResult(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMessageRef);
        dest.writeString(this.mAckPdu);
        dest.writeInt(this.mErrorCode);
    }

    public String toString() {
        return super.toString() + ", MessageRef: " + this.mMessageRef + ", AckPdu=" + this.mAckPdu + ", ErrorCode=" + this.mErrorCode;
    }

    private void readFromParcel(Parcel in) {
        this.mMessageRef = in.readInt();
        this.mAckPdu = in.readString();
        this.mErrorCode = in.readInt();
    }
}
