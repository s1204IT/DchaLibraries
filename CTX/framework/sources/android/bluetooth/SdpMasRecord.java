package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public class SdpMasRecord implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public SdpMasRecord createFromParcel(Parcel parcel) {
            return new SdpMasRecord(parcel);
        }

        @Override
        public SdpRecord[] newArray(int i) {
            return new SdpRecord[i];
        }
    };
    private final int mL2capPsm;
    private final int mMasInstanceId;
    private final int mProfileVersion;
    private final int mRfcommChannelNumber;
    private final String mServiceName;
    private final int mSupportedFeatures;
    private final int mSupportedMessageTypes;

    public static final class MessageType {
        public static final int EMAIL = 1;
        public static final int MMS = 8;
        public static final int SMS_CDMA = 4;
        public static final int SMS_GSM = 2;
    }

    public SdpMasRecord(int i, int i2, int i3, int i4, int i5, int i6, String str) {
        this.mMasInstanceId = i;
        this.mL2capPsm = i2;
        this.mRfcommChannelNumber = i3;
        this.mProfileVersion = i4;
        this.mSupportedFeatures = i5;
        this.mSupportedMessageTypes = i6;
        this.mServiceName = str;
    }

    public SdpMasRecord(Parcel parcel) {
        this.mMasInstanceId = parcel.readInt();
        this.mL2capPsm = parcel.readInt();
        this.mRfcommChannelNumber = parcel.readInt();
        this.mProfileVersion = parcel.readInt();
        this.mSupportedFeatures = parcel.readInt();
        this.mSupportedMessageTypes = parcel.readInt();
        this.mServiceName = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getMasInstanceId() {
        return this.mMasInstanceId;
    }

    public int getL2capPsm() {
        return this.mL2capPsm;
    }

    public int getRfcommCannelNumber() {
        return this.mRfcommChannelNumber;
    }

    public int getProfileVersion() {
        return this.mProfileVersion;
    }

    public int getSupportedFeatures() {
        return this.mSupportedFeatures;
    }

    public int getSupportedMessageTypes() {
        return this.mSupportedMessageTypes;
    }

    public boolean msgSupported(int i) {
        return (i & this.mSupportedMessageTypes) != 0;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mMasInstanceId);
        parcel.writeInt(this.mL2capPsm);
        parcel.writeInt(this.mRfcommChannelNumber);
        parcel.writeInt(this.mProfileVersion);
        parcel.writeInt(this.mSupportedFeatures);
        parcel.writeInt(this.mSupportedMessageTypes);
        parcel.writeString(this.mServiceName);
    }

    public String toString() {
        String str = "Bluetooth MAS SDP Record:\n";
        if (this.mMasInstanceId != -1) {
            str = "Bluetooth MAS SDP Record:\nMas Instance Id: " + this.mMasInstanceId + "\n";
        }
        if (this.mRfcommChannelNumber != -1) {
            str = str + "RFCOMM Chan Number: " + this.mRfcommChannelNumber + "\n";
        }
        if (this.mL2capPsm != -1) {
            str = str + "L2CAP PSM: " + this.mL2capPsm + "\n";
        }
        if (this.mServiceName != null) {
            str = str + "Service Name: " + this.mServiceName + "\n";
        }
        if (this.mProfileVersion != -1) {
            str = str + "Profile version: " + this.mProfileVersion + "\n";
        }
        if (this.mSupportedMessageTypes != -1) {
            str = str + "Supported msg types: " + this.mSupportedMessageTypes + "\n";
        }
        if (this.mSupportedFeatures != -1) {
            return str + "Supported features: " + this.mSupportedFeatures + "\n";
        }
        return str;
    }
}
