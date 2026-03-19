package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.SettingsStringUtil;

public final class BluetoothMasInstance implements Parcelable {
    public static final Parcelable.Creator<BluetoothMasInstance> CREATOR = new Parcelable.Creator<BluetoothMasInstance>() {
        @Override
        public BluetoothMasInstance createFromParcel(Parcel parcel) {
            return new BluetoothMasInstance(parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public BluetoothMasInstance[] newArray(int i) {
            return new BluetoothMasInstance[i];
        }
    };
    private final int mChannel;
    private final int mId;
    private final int mMsgTypes;
    private final String mName;

    public static final class MessageType {
        public static final int EMAIL = 1;
        public static final int MMS = 8;
        public static final int SMS_CDMA = 4;
        public static final int SMS_GSM = 2;
    }

    public BluetoothMasInstance(int i, String str, int i2, int i3) {
        this.mId = i;
        this.mName = str;
        this.mChannel = i2;
        this.mMsgTypes = i3;
    }

    public boolean equals(Object obj) {
        return (obj instanceof BluetoothMasInstance) && this.mId == ((BluetoothMasInstance) obj).mId;
    }

    public int hashCode() {
        return this.mId + (this.mChannel << 8) + (this.mMsgTypes << 16);
    }

    public String toString() {
        return Integer.toString(this.mId) + SettingsStringUtil.DELIMITER + this.mName + SettingsStringUtil.DELIMITER + this.mChannel + SettingsStringUtil.DELIMITER + Integer.toHexString(this.mMsgTypes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mId);
        parcel.writeString(this.mName);
        parcel.writeInt(this.mChannel);
        parcel.writeInt(this.mMsgTypes);
    }

    public int getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName;
    }

    public int getChannel() {
        return this.mChannel;
    }

    public int getMsgTypes() {
        return this.mMsgTypes;
    }

    public boolean msgSupported(int i) {
        return (i & this.mMsgTypes) != 0;
    }
}
