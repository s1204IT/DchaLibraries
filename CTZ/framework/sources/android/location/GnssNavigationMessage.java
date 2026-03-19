package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;

public final class GnssNavigationMessage implements Parcelable {
    public static final int STATUS_PARITY_PASSED = 1;
    public static final int STATUS_PARITY_REBUILT = 2;
    public static final int STATUS_UNKNOWN = 0;
    public static final int TYPE_BDS_D1 = 1281;
    public static final int TYPE_BDS_D2 = 1282;
    public static final int TYPE_GAL_F = 1538;
    public static final int TYPE_GAL_I = 1537;
    public static final int TYPE_GLO_L1CA = 769;
    public static final int TYPE_GPS_CNAV2 = 260;
    public static final int TYPE_GPS_L1CA = 257;
    public static final int TYPE_GPS_L2CNAV = 258;
    public static final int TYPE_GPS_L5CNAV = 259;
    public static final int TYPE_UNKNOWN = 0;
    private byte[] mData;
    private int mMessageId;
    private int mStatus;
    private int mSubmessageId;
    private int mSvid;
    private int mType;
    private static final byte[] EMPTY_ARRAY = new byte[0];
    public static final Parcelable.Creator<GnssNavigationMessage> CREATOR = new Parcelable.Creator<GnssNavigationMessage>() {
        @Override
        public GnssNavigationMessage createFromParcel(Parcel parcel) {
            GnssNavigationMessage gnssNavigationMessage = new GnssNavigationMessage();
            gnssNavigationMessage.setType(parcel.readInt());
            gnssNavigationMessage.setSvid(parcel.readInt());
            gnssNavigationMessage.setMessageId(parcel.readInt());
            gnssNavigationMessage.setSubmessageId(parcel.readInt());
            byte[] bArr = new byte[parcel.readInt()];
            parcel.readByteArray(bArr);
            gnssNavigationMessage.setData(bArr);
            gnssNavigationMessage.setStatus(parcel.readInt());
            return gnssNavigationMessage;
        }

        @Override
        public GnssNavigationMessage[] newArray(int i) {
            return new GnssNavigationMessage[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface GnssNavigationMessageType {
    }

    public static abstract class Callback {
        public static final int STATUS_LOCATION_DISABLED = 2;
        public static final int STATUS_NOT_SUPPORTED = 0;
        public static final int STATUS_READY = 1;

        @Retention(RetentionPolicy.SOURCE)
        public @interface GnssNavigationMessageStatus {
        }

        public void onGnssNavigationMessageReceived(GnssNavigationMessage gnssNavigationMessage) {
        }

        public void onStatusChanged(int i) {
        }
    }

    public GnssNavigationMessage() {
        initialize();
    }

    public void set(GnssNavigationMessage gnssNavigationMessage) {
        this.mType = gnssNavigationMessage.mType;
        this.mSvid = gnssNavigationMessage.mSvid;
        this.mMessageId = gnssNavigationMessage.mMessageId;
        this.mSubmessageId = gnssNavigationMessage.mSubmessageId;
        this.mData = gnssNavigationMessage.mData;
        this.mStatus = gnssNavigationMessage.mStatus;
    }

    public void reset() {
        initialize();
    }

    public int getType() {
        return this.mType;
    }

    public void setType(int i) {
        this.mType = i;
    }

    private String getTypeString() {
        int i = this.mType;
        if (i == 0) {
            return "Unknown";
        }
        if (i != 769) {
            switch (i) {
                case 257:
                    return "GPS L1 C/A";
                case 258:
                    return "GPS L2-CNAV";
                case 259:
                    return "GPS L5-CNAV";
                case 260:
                    return "GPS CNAV2";
                default:
                    switch (i) {
                        case 1281:
                            return "Beidou D1";
                        case 1282:
                            return "Beidou D2";
                        default:
                            switch (i) {
                                case TYPE_GAL_I:
                                    return "Galileo I";
                                case TYPE_GAL_F:
                                    return "Galileo F";
                                default:
                                    return "<Invalid:" + this.mType + ">";
                            }
                    }
            }
        }
        return "Glonass L1 C/A";
    }

    public int getSvid() {
        return this.mSvid;
    }

    public void setSvid(int i) {
        this.mSvid = i;
    }

    public int getMessageId() {
        return this.mMessageId;
    }

    public void setMessageId(int i) {
        this.mMessageId = i;
    }

    public int getSubmessageId() {
        return this.mSubmessageId;
    }

    public void setSubmessageId(int i) {
        this.mSubmessageId = i;
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

    public int getStatus() {
        return this.mStatus;
    }

    public void setStatus(int i) {
        this.mStatus = i;
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
                return "<Invalid:" + this.mStatus + ">";
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        parcel.writeInt(this.mSvid);
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
        StringBuilder sb = new StringBuilder("GnssNavigationMessage:\n");
        sb.append(String.format("   %-15s = %s\n", "Type", getTypeString()));
        sb.append(String.format("   %-15s = %s\n", "Svid", Integer.valueOf(this.mSvid)));
        sb.append(String.format("   %-15s = %s\n", "Status", getStatusString()));
        sb.append(String.format("   %-15s = %s\n", "MessageId", Integer.valueOf(this.mMessageId)));
        sb.append(String.format("   %-15s = %s\n", "SubmessageId", Integer.valueOf(this.mSubmessageId)));
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
        this.mType = 0;
        this.mSvid = 0;
        this.mMessageId = -1;
        this.mSubmessageId = -1;
        this.mData = EMPTY_ARRAY;
        this.mStatus = 0;
    }
}
