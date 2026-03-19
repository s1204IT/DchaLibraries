package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class SmsRawData implements Parcelable {
    public static final Parcelable.Creator<SmsRawData> CREATOR = new Parcelable.Creator<SmsRawData>() {
        @Override
        public SmsRawData createFromParcel(Parcel parcel) {
            byte[] bArr = new byte[parcel.readInt()];
            parcel.readByteArray(bArr);
            return new SmsRawData(bArr);
        }

        @Override
        public SmsRawData[] newArray(int i) {
            return new SmsRawData[i];
        }
    };
    byte[] data;

    public SmsRawData(byte[] bArr) {
        this.data = bArr;
    }

    public byte[] getBytes() {
        return this.data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.data.length);
        parcel.writeByteArray(this.data);
    }
}
