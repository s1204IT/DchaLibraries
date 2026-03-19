package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.Arrays;

public class SmsCbEtwsInfo implements Parcelable {
    public static final Parcelable.Creator<SmsCbEtwsInfo> CREATOR = new Parcelable.Creator<SmsCbEtwsInfo>() {
        @Override
        public SmsCbEtwsInfo createFromParcel(Parcel parcel) {
            return new SmsCbEtwsInfo(parcel);
        }

        @Override
        public SmsCbEtwsInfo[] newArray(int i) {
            return new SmsCbEtwsInfo[i];
        }
    };
    public static final int ETWS_WARNING_TYPE_EARTHQUAKE = 0;
    public static final int ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI = 2;
    public static final int ETWS_WARNING_TYPE_OTHER_EMERGENCY = 4;
    public static final int ETWS_WARNING_TYPE_TEST_MESSAGE = 3;
    public static final int ETWS_WARNING_TYPE_TSUNAMI = 1;
    public static final int ETWS_WARNING_TYPE_UNKNOWN = -1;
    private final boolean mActivatePopup;
    private final boolean mEmergencyUserAlert;
    private final boolean mPrimary;
    private final byte[] mWarningSecurityInformation;
    private final int mWarningType;

    public SmsCbEtwsInfo(int i, boolean z, boolean z2, boolean z3, byte[] bArr) {
        this.mWarningType = i;
        this.mEmergencyUserAlert = z;
        this.mActivatePopup = z2;
        this.mPrimary = z3;
        this.mWarningSecurityInformation = bArr;
    }

    SmsCbEtwsInfo(Parcel parcel) {
        this.mWarningType = parcel.readInt();
        this.mEmergencyUserAlert = parcel.readInt() != 0;
        this.mActivatePopup = parcel.readInt() != 0;
        this.mPrimary = parcel.readInt() != 0;
        this.mWarningSecurityInformation = parcel.createByteArray();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mWarningType);
        parcel.writeInt(this.mEmergencyUserAlert ? 1 : 0);
        parcel.writeInt(this.mActivatePopup ? 1 : 0);
        parcel.writeInt(this.mPrimary ? 1 : 0);
        parcel.writeByteArray(this.mWarningSecurityInformation);
    }

    public int getWarningType() {
        return this.mWarningType;
    }

    public boolean isEmergencyUserAlert() {
        return this.mEmergencyUserAlert;
    }

    public boolean isPopupAlert() {
        return this.mActivatePopup;
    }

    public boolean isPrimary() {
        return this.mPrimary;
    }

    public long getPrimaryNotificationTimestamp() {
        if (this.mWarningSecurityInformation == null || this.mWarningSecurityInformation.length < 7) {
            return 0L;
        }
        int iGsmBcdByteToInt = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[0]);
        int iGsmBcdByteToInt2 = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[1]);
        int iGsmBcdByteToInt3 = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[2]);
        int iGsmBcdByteToInt4 = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[3]);
        int iGsmBcdByteToInt5 = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[4]);
        int iGsmBcdByteToInt6 = IccUtils.gsmBcdByteToInt(this.mWarningSecurityInformation[5]);
        byte b = this.mWarningSecurityInformation[6];
        int iGsmBcdByteToInt7 = IccUtils.gsmBcdByteToInt((byte) (b & (-9)));
        if ((b & 8) != 0) {
            iGsmBcdByteToInt7 = -iGsmBcdByteToInt7;
        }
        Time time = new Time(Time.TIMEZONE_UTC);
        time.year = iGsmBcdByteToInt + 2000;
        time.month = iGsmBcdByteToInt2 - 1;
        time.monthDay = iGsmBcdByteToInt3;
        time.hour = iGsmBcdByteToInt4;
        time.minute = iGsmBcdByteToInt5;
        time.second = iGsmBcdByteToInt6;
        return time.toMillis(true) - ((long) (((iGsmBcdByteToInt7 * 15) * 60) * 1000));
    }

    public byte[] getPrimaryNotificationSignature() {
        if (this.mWarningSecurityInformation == null || this.mWarningSecurityInformation.length < 50) {
            return null;
        }
        return Arrays.copyOfRange(this.mWarningSecurityInformation, 7, 50);
    }

    public String toString() {
        return "SmsCbEtwsInfo{warningType=" + this.mWarningType + ", emergencyUserAlert=" + this.mEmergencyUserAlert + ", activatePopup=" + this.mActivatePopup + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
