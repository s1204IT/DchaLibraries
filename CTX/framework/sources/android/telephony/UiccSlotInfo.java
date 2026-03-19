package android.telephony;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

@SystemApi
public class UiccSlotInfo implements Parcelable {
    public static final int CARD_STATE_INFO_ABSENT = 1;
    public static final int CARD_STATE_INFO_ERROR = 3;
    public static final int CARD_STATE_INFO_PRESENT = 2;
    public static final int CARD_STATE_INFO_RESTRICTED = 4;
    public static final Parcelable.Creator<UiccSlotInfo> CREATOR = new Parcelable.Creator<UiccSlotInfo>() {
        @Override
        public UiccSlotInfo createFromParcel(Parcel parcel) {
            return new UiccSlotInfo(parcel);
        }

        @Override
        public UiccSlotInfo[] newArray(int i) {
            return new UiccSlotInfo[i];
        }
    };
    private final String mCardId;
    private final int mCardStateInfo;
    private final boolean mIsActive;
    private final boolean mIsEuicc;
    private final boolean mIsExtendedApduSupported;
    private final int mLogicalSlotIdx;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CardStateInfo {
    }

    private UiccSlotInfo(Parcel parcel) {
        this.mIsActive = parcel.readByte() != 0;
        this.mIsEuicc = parcel.readByte() != 0;
        this.mCardId = parcel.readString();
        this.mCardStateInfo = parcel.readInt();
        this.mLogicalSlotIdx = parcel.readInt();
        this.mIsExtendedApduSupported = parcel.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(this.mIsActive ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.mIsEuicc ? (byte) 1 : (byte) 0);
        parcel.writeString(this.mCardId);
        parcel.writeInt(this.mCardStateInfo);
        parcel.writeInt(this.mLogicalSlotIdx);
        parcel.writeByte(this.mIsExtendedApduSupported ? (byte) 1 : (byte) 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public UiccSlotInfo(boolean z, boolean z2, String str, int i, int i2, boolean z3) {
        this.mIsActive = z;
        this.mIsEuicc = z2;
        this.mCardId = str;
        this.mCardStateInfo = i;
        this.mLogicalSlotIdx = i2;
        this.mIsExtendedApduSupported = z3;
    }

    public boolean getIsActive() {
        return this.mIsActive;
    }

    public boolean getIsEuicc() {
        return this.mIsEuicc;
    }

    public String getCardId() {
        return this.mCardId;
    }

    public int getCardStateInfo() {
        return this.mCardStateInfo;
    }

    public int getLogicalSlotIdx() {
        return this.mLogicalSlotIdx;
    }

    public boolean getIsExtendedApduSupported() {
        return this.mIsExtendedApduSupported;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        UiccSlotInfo uiccSlotInfo = (UiccSlotInfo) obj;
        if (this.mIsActive == uiccSlotInfo.mIsActive && this.mIsEuicc == uiccSlotInfo.mIsEuicc && Objects.equals(this.mCardId, uiccSlotInfo.mCardId) && this.mCardStateInfo == uiccSlotInfo.mCardStateInfo && this.mLogicalSlotIdx == uiccSlotInfo.mLogicalSlotIdx && this.mIsExtendedApduSupported == uiccSlotInfo.mIsExtendedApduSupported) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((((((((this.mIsActive ? 1 : 0) + 31) * 31) + (this.mIsEuicc ? 1 : 0)) * 31) + Objects.hashCode(this.mCardId)) * 31) + this.mCardStateInfo) * 31) + this.mLogicalSlotIdx)) + (this.mIsExtendedApduSupported ? 1 : 0);
    }

    public String toString() {
        return "UiccSlotInfo (mIsActive=" + this.mIsActive + ", mIsEuicc=" + this.mIsEuicc + ", mCardId=" + this.mCardId + ", cardState=" + this.mCardStateInfo + ", phoneId=" + this.mLogicalSlotIdx + ", mIsExtendedApduSupported=" + this.mIsExtendedApduSupported + ")";
    }
}
