package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public class SdpMnsRecord implements Parcelable {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public SdpMnsRecord createFromParcel(Parcel parcel) {
            return new SdpMnsRecord(parcel);
        }

        @Override
        public SdpMnsRecord[] newArray(int i) {
            return new SdpMnsRecord[i];
        }
    };
    private final int mL2capPsm;
    private final int mProfileVersion;
    private final int mRfcommChannelNumber;
    private final String mServiceName;
    private final int mSupportedFeatures;

    public SdpMnsRecord(int i, int i2, int i3, int i4, String str) {
        this.mL2capPsm = i;
        this.mRfcommChannelNumber = i2;
        this.mSupportedFeatures = i4;
        this.mServiceName = str;
        this.mProfileVersion = i3;
    }

    public SdpMnsRecord(Parcel parcel) {
        this.mRfcommChannelNumber = parcel.readInt();
        this.mL2capPsm = parcel.readInt();
        this.mServiceName = parcel.readString();
        this.mSupportedFeatures = parcel.readInt();
        this.mProfileVersion = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getL2capPsm() {
        return this.mL2capPsm;
    }

    public int getRfcommChannelNumber() {
        return this.mRfcommChannelNumber;
    }

    public int getSupportedFeatures() {
        return this.mSupportedFeatures;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public int getProfileVersion() {
        return this.mProfileVersion;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRfcommChannelNumber);
        parcel.writeInt(this.mL2capPsm);
        parcel.writeString(this.mServiceName);
        parcel.writeInt(this.mSupportedFeatures);
        parcel.writeInt(this.mProfileVersion);
    }

    public String toString() {
        String str = "Bluetooth MNS SDP Record:\n";
        if (this.mRfcommChannelNumber != -1) {
            str = "Bluetooth MNS SDP Record:\nRFCOMM Chan Number: " + this.mRfcommChannelNumber + "\n";
        }
        if (this.mL2capPsm != -1) {
            str = str + "L2CAP PSM: " + this.mL2capPsm + "\n";
        }
        if (this.mServiceName != null) {
            str = str + "Service Name: " + this.mServiceName + "\n";
        }
        if (this.mSupportedFeatures != -1) {
            str = str + "Supported features: " + this.mSupportedFeatures + "\n";
        }
        if (this.mProfileVersion != -1) {
            return str + "Profile_version: " + this.mProfileVersion + "\n";
        }
        return str;
    }
}
