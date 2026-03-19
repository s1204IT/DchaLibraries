package android.hardware.location;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Logging.Session;
import java.util.Arrays;

@SystemApi
@Deprecated
public class ContextHubMessage implements Parcelable {
    public static final Parcelable.Creator<ContextHubMessage> CREATOR = new Parcelable.Creator<ContextHubMessage>() {
        @Override
        public ContextHubMessage createFromParcel(Parcel parcel) {
            return new ContextHubMessage(parcel);
        }

        @Override
        public ContextHubMessage[] newArray(int i) {
            return new ContextHubMessage[i];
        }
    };
    private static final int DEBUG_LOG_NUM_BYTES = 16;
    private byte[] mData;
    private int mType;
    private int mVersion;

    public int getMsgType() {
        return this.mType;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public byte[] getData() {
        return Arrays.copyOf(this.mData, this.mData.length);
    }

    public void setMsgType(int i) {
        this.mType = i;
    }

    public void setVersion(int i) {
        this.mVersion = i;
    }

    public void setMsgData(byte[] bArr) {
        this.mData = Arrays.copyOf(bArr, bArr.length);
    }

    public ContextHubMessage(int i, int i2, byte[] bArr) {
        this.mType = i;
        this.mVersion = i2;
        this.mData = Arrays.copyOf(bArr, bArr.length);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ContextHubMessage(Parcel parcel) {
        this.mType = parcel.readInt();
        this.mVersion = parcel.readInt();
        this.mData = new byte[parcel.readInt()];
        parcel.readByteArray(this.mData);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        parcel.writeInt(this.mVersion);
        parcel.writeInt(this.mData.length);
        parcel.writeByteArray(this.mData);
    }

    public String toString() {
        int length = this.mData.length;
        String str = "ContextHubMessage[type = " + this.mType + ", length = " + this.mData.length + " bytes](";
        if (length > 0) {
            str = str + "data = 0x";
        }
        int i = 0;
        while (i < Math.min(length, 16)) {
            str = str + Byte.toHexString(this.mData[i], true);
            i++;
            if (i % 4 == 0) {
                str = str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER;
            }
        }
        if (length > 16) {
            str = str + Session.TRUNCATE_STRING;
        }
        return str + ")";
    }
}
