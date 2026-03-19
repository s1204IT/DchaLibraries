package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class SmsCbMessage implements Parcelable {
    public static final Parcelable.Creator<SmsCbMessage> CREATOR = new Parcelable.Creator<SmsCbMessage>() {
        @Override
        public SmsCbMessage createFromParcel(Parcel parcel) {
            return new SmsCbMessage(parcel);
        }

        @Override
        public SmsCbMessage[] newArray(int i) {
            return new SmsCbMessage[i];
        }
    };
    public static final int GEOGRAPHICAL_SCOPE_CELL_WIDE = 3;
    public static final int GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE = 0;
    public static final int GEOGRAPHICAL_SCOPE_LA_WIDE = 2;
    public static final int GEOGRAPHICAL_SCOPE_PLMN_WIDE = 1;
    protected static final String LOG_TAG = "SMSCB";
    public static final int MESSAGE_FORMAT_3GPP = 1;
    public static final int MESSAGE_FORMAT_3GPP2 = 2;
    public static final int MESSAGE_PRIORITY_EMERGENCY = 3;
    public static final int MESSAGE_PRIORITY_INTERACTIVE = 1;
    public static final int MESSAGE_PRIORITY_NORMAL = 0;
    public static final int MESSAGE_PRIORITY_URGENT = 2;
    private final String mBody;
    private final SmsCbCmasInfo mCmasWarningInfo;
    private final SmsCbEtwsInfo mEtwsWarningInfo;
    private final int mGeographicalScope;
    private final String mLanguage;
    private final SmsCbLocation mLocation;
    private final int mMessageFormat;
    private final int mPriority;
    private final int mSerialNumber;
    private final int mServiceCategory;

    public SmsCbMessage(int i, int i2, int i3, SmsCbLocation smsCbLocation, int i4, String str, String str2, int i5, SmsCbEtwsInfo smsCbEtwsInfo, SmsCbCmasInfo smsCbCmasInfo) {
        this.mMessageFormat = i;
        this.mGeographicalScope = i2;
        this.mSerialNumber = i3;
        this.mLocation = smsCbLocation;
        this.mServiceCategory = i4;
        this.mLanguage = str;
        this.mBody = str2;
        this.mPriority = i5;
        this.mEtwsWarningInfo = smsCbEtwsInfo;
        this.mCmasWarningInfo = smsCbCmasInfo;
    }

    public SmsCbMessage(Parcel parcel) {
        this.mMessageFormat = parcel.readInt();
        this.mGeographicalScope = parcel.readInt();
        this.mSerialNumber = parcel.readInt();
        this.mLocation = new SmsCbLocation(parcel);
        this.mServiceCategory = parcel.readInt();
        this.mLanguage = parcel.readString();
        this.mBody = parcel.readString();
        this.mPriority = parcel.readInt();
        int i = parcel.readInt();
        if (i == 67) {
            this.mEtwsWarningInfo = null;
            this.mCmasWarningInfo = makeSmsCbCmasInfo(parcel);
        } else if (i == 69) {
            this.mEtwsWarningInfo = new SmsCbEtwsInfo(parcel);
            this.mCmasWarningInfo = null;
        } else {
            this.mEtwsWarningInfo = null;
            this.mCmasWarningInfo = null;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mMessageFormat);
        parcel.writeInt(this.mGeographicalScope);
        parcel.writeInt(this.mSerialNumber);
        this.mLocation.writeToParcel(parcel, i);
        parcel.writeInt(this.mServiceCategory);
        parcel.writeString(this.mLanguage);
        parcel.writeString(this.mBody);
        parcel.writeInt(this.mPriority);
        if (this.mEtwsWarningInfo != null) {
            parcel.writeInt(69);
            this.mEtwsWarningInfo.writeToParcel(parcel, i);
        } else if (this.mCmasWarningInfo != null) {
            parcel.writeInt(67);
            this.mCmasWarningInfo.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(48);
        }
    }

    public int getGeographicalScope() {
        return this.mGeographicalScope;
    }

    public int getSerialNumber() {
        return this.mSerialNumber;
    }

    public SmsCbLocation getLocation() {
        return this.mLocation;
    }

    public int getServiceCategory() {
        return this.mServiceCategory;
    }

    public String getLanguageCode() {
        return this.mLanguage;
    }

    public String getMessageBody() {
        return this.mBody;
    }

    public int getMessageFormat() {
        return this.mMessageFormat;
    }

    public int getMessagePriority() {
        return this.mPriority;
    }

    public SmsCbEtwsInfo getEtwsWarningInfo() {
        return this.mEtwsWarningInfo;
    }

    public SmsCbCmasInfo getCmasWarningInfo() {
        return this.mCmasWarningInfo;
    }

    public boolean isEmergencyMessage() {
        return this.mPriority == 3;
    }

    public boolean isEtwsMessage() {
        return this.mEtwsWarningInfo != null;
    }

    public boolean isCmasMessage() {
        return this.mCmasWarningInfo != null;
    }

    public String toString() {
        String str;
        String str2;
        StringBuilder sb = new StringBuilder();
        sb.append("SmsCbMessage{geographicalScope=");
        sb.append(this.mGeographicalScope);
        sb.append(", serialNumber=");
        sb.append(this.mSerialNumber);
        sb.append(", location=");
        sb.append(this.mLocation);
        sb.append(", serviceCategory=");
        sb.append(this.mServiceCategory);
        sb.append(", language=");
        sb.append(this.mLanguage);
        sb.append(", body=");
        sb.append(this.mBody);
        sb.append(", priority=");
        sb.append(this.mPriority);
        if (this.mEtwsWarningInfo != null) {
            str = ", " + this.mEtwsWarningInfo.toString();
        } else {
            str = "";
        }
        sb.append(str);
        if (this.mCmasWarningInfo != null) {
            str2 = ", " + this.mCmasWarningInfo.toString();
        } else {
            str2 = "";
        }
        sb.append(str2);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private SmsCbCmasInfo makeSmsCbCmasInfo(Parcel parcel) {
        try {
            return (SmsCbCmasInfo) Class.forName("mediatek.telephony.MtkSmsCbCmasInfo").getConstructor(Parcel.class).newInstance(parcel);
        } catch (Exception e) {
            return new SmsCbCmasInfo(parcel);
        }
    }
}
