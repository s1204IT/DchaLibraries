package android.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.security.InvalidParameterException;

@SystemApi
public class GpsNavigationMessage implements Parcelable {
    public static final short STATUS_PARITY_PASSED = 1;
    public static final short STATUS_PARITY_REBUILT = 2;
    public static final short STATUS_UNKNOWN = 0;
    public static final byte TYPE_CNAV2 = 4;
    public static final byte TYPE_L1CA = 1;
    public static final byte TYPE_L2CNAV = 2;
    public static final byte TYPE_L5CNAV = 3;
    public static final byte TYPE_UNKNOWN = 0;
    private byte[] mData;
    private short mMessageId;
    private byte mPrn;
    private short mStatus;
    private short mSubmessageId;
    private byte mType;
    private static final byte[] EMPTY_ARRAY = new byte[0];
    public static final Parcelable.Creator<GpsNavigationMessage> CREATOR = new Parcelable.Creator<GpsNavigationMessage>() {
        @Override
        public GpsNavigationMessage createFromParcel(Parcel parcel) {
            GpsNavigationMessage gpsNavigationMessage = new GpsNavigationMessage();
            gpsNavigationMessage.setType(parcel.readByte());
            gpsNavigationMessage.setPrn(parcel.readByte());
            gpsNavigationMessage.setMessageId((short) parcel.readInt());
            gpsNavigationMessage.setSubmessageId((short) parcel.readInt());
            byte[] bArr = new byte[parcel.readInt()];
            parcel.readByteArray(bArr);
            gpsNavigationMessage.setData(bArr);
            gpsNavigationMessage.setStatus((short) parcel.readInt());
            return gpsNavigationMessage;
        }

        @Override
        public GpsNavigationMessage[] newArray(int i) {
            return new GpsNavigationMessage[i];
        }
    };

    GpsNavigationMessage() {
        initialize();
    }

    public void set(GpsNavigationMessage gpsNavigationMessage) {
        this.mType = gpsNavigationMessage.mType;
        this.mPrn = gpsNavigationMessage.mPrn;
        this.mMessageId = gpsNavigationMessage.mMessageId;
        this.mSubmessageId = gpsNavigationMessage.mSubmessageId;
        this.mData = gpsNavigationMessage.mData;
        this.mStatus = gpsNavigationMessage.mStatus;
    }

    public void reset() {
        initialize();
    }

    public byte getType() {
        return this.mType;
    }

    public void setType(byte b) {
        this.mType = b;
    }

    private String getTypeString() {
        switch (this.mType) {
            case 0:
                return "Unknown";
            case 1:
                return "L1 C/A";
            case 2:
                return "L2-CNAV";
            case 3:
                return "L5-CNAV";
            case 4:
                return "CNAV-2";
            default:
                return "<Invalid:" + ((int) this.mType) + ">";
        }
    }

    public byte getPrn() {
        return this.mPrn;
    }

    public void setPrn(byte b) {
        this.mPrn = b;
    }

    public short getMessageId() {
        return this.mMessageId;
    }

    public void setMessageId(short s) {
        this.mMessageId = s;
    }

    public short getSubmessageId() {
        return this.mSubmessageId;
    }

    public void setSubmessageId(short s) {
        this.mSubmessageId = s;
    }

    public byte[] getData() {
        return this.mData;
    }

    public void setData(byte[] bArr) {
        if (bArr == null) {
            throw new InvalidParameterException("Data must be a non-null array");
        }
        this.mData = bArr;
    }

    public short getStatus() {
        return this.mStatus;
    }

    public void setStatus(short s) {
        this.mStatus = s;
    }

    private String getStatusString() {
        switch (this.mStatus) {
            case 0:
                return "Unknown";
            case 1:
                return "ParityPassed";
            case 2:
                return "ParityRebuilt";
            default:
                return "<Invalid:" + ((int) this.mStatus) + ">";
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(this.mType);
        parcel.writeByte(this.mPrn);
        parcel.writeInt(this.mMessageId);
        parcel.writeInt(this.mSubmessageId);
        parcel.writeInt(this.mData.length);
        parcel.writeByteArray(this.mData);
        parcel.writeInt(this.mStatus);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GpsNavigationMessage:\n");
        sb.append(String.format("   %-15s = %s\n", "Type", getTypeString()));
        sb.append(String.format("   %-15s = %s\n", "Prn", Byte.valueOf(this.mPrn)));
        sb.append(String.format("   %-15s = %s\n", "Status", getStatusString()));
        sb.append(String.format("   %-15s = %s\n", "MessageId", Short.valueOf(this.mMessageId)));
        sb.append(String.format("   %-15s = %s\n", "SubmessageId", Short.valueOf(this.mSubmessageId)));
        sb.append(String.format("   %-15s = %s\n", "Data", "{"));
        String str = "        ";
        for (byte b : this.mData) {
            sb.append(str);
            sb.append((int) b);
            str = ", ";
        }
        sb.append(" }");
        return sb.toString();
    }

    private void initialize() {
        this.mType = (byte) 0;
        this.mPrn = (byte) 0;
        this.mMessageId = (short) -1;
        this.mSubmessageId = (short) -1;
        this.mData = EMPTY_ARRAY;
        this.mStatus = (short) 0;
    }
}
