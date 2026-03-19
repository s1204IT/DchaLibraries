package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.text.TextUtils;
import com.android.internal.telephony.IccCardConstants;
import java.util.Objects;

public final class DisconnectCause implements Parcelable {
    public static final int ANSWERED_ELSEWHERE = 11;
    public static final int BUSY = 7;
    public static final int CALL_PULLED = 12;
    public static final int CANCELED = 4;
    public static final int CONNECTION_MANAGER_NOT_SUPPORTED = 10;
    public static final Parcelable.Creator<DisconnectCause> CREATOR = new Parcelable.Creator<DisconnectCause>() {
        @Override
        public DisconnectCause createFromParcel(Parcel parcel) {
            return new DisconnectCause(parcel.readInt(), TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel), TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel), parcel.readString(), parcel.readInt());
        }

        @Override
        public DisconnectCause[] newArray(int i) {
            return new DisconnectCause[i];
        }
    };
    public static final int ERROR = 1;
    public static final int LOCAL = 2;
    public static final int MISSED = 5;
    public static final int OTHER = 9;
    public static final String REASON_IMS_ACCESS_BLOCKED = "REASON_IMS_ACCESS_BLOCKED";
    public static final String REASON_WIFI_ON_BUT_WFC_OFF = "REASON_WIFI_ON_BUT_WFC_OFF";
    public static final int REJECTED = 6;
    public static final int REMOTE = 3;
    public static final int RESTRICTED = 8;
    public static final int UNKNOWN = 0;
    private int mDisconnectCode;
    private CharSequence mDisconnectDescription;
    private CharSequence mDisconnectLabel;
    private String mDisconnectReason;
    private int mToneToPlay;

    public DisconnectCause(int i) {
        this(i, null, null, null, -1);
    }

    public DisconnectCause(int i, String str) {
        this(i, null, null, str, -1);
    }

    public DisconnectCause(int i, CharSequence charSequence, CharSequence charSequence2, String str) {
        this(i, charSequence, charSequence2, str, -1);
    }

    public DisconnectCause(int i, CharSequence charSequence, CharSequence charSequence2, String str, int i2) {
        this.mDisconnectCode = i;
        this.mDisconnectLabel = charSequence;
        this.mDisconnectDescription = charSequence2;
        this.mDisconnectReason = str;
        this.mToneToPlay = i2;
    }

    public int getCode() {
        return this.mDisconnectCode;
    }

    public CharSequence getLabel() {
        return this.mDisconnectLabel;
    }

    public CharSequence getDescription() {
        return this.mDisconnectDescription;
    }

    public String getReason() {
        return this.mDisconnectReason;
    }

    public int getTone() {
        return this.mToneToPlay;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mDisconnectCode);
        TextUtils.writeToParcel(this.mDisconnectLabel, parcel, i);
        TextUtils.writeToParcel(this.mDisconnectDescription, parcel, i);
        parcel.writeString(this.mDisconnectReason);
        parcel.writeInt(this.mToneToPlay);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int hashCode() {
        return Objects.hashCode(Integer.valueOf(this.mDisconnectCode)) + Objects.hashCode(this.mDisconnectLabel) + Objects.hashCode(this.mDisconnectDescription) + Objects.hashCode(this.mDisconnectReason) + Objects.hashCode(Integer.valueOf(this.mToneToPlay));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DisconnectCause)) {
            return false;
        }
        DisconnectCause disconnectCause = (DisconnectCause) obj;
        return Objects.equals(Integer.valueOf(this.mDisconnectCode), Integer.valueOf(disconnectCause.getCode())) && Objects.equals(this.mDisconnectLabel, disconnectCause.getLabel()) && Objects.equals(this.mDisconnectDescription, disconnectCause.getDescription()) && Objects.equals(this.mDisconnectReason, disconnectCause.getReason()) && Objects.equals(Integer.valueOf(this.mToneToPlay), Integer.valueOf(disconnectCause.getTone()));
    }

    public String toString() {
        String str;
        switch (this.mDisconnectCode) {
            case 0:
                str = IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
                break;
            case 1:
                str = "ERROR";
                break;
            case 2:
                str = CalendarContract.ACCOUNT_TYPE_LOCAL;
                break;
            case 3:
                str = "REMOTE";
                break;
            case 4:
                str = "CANCELED";
                break;
            case 5:
                str = "MISSED";
                break;
            case 6:
                str = "REJECTED";
                break;
            case 7:
                str = "BUSY";
                break;
            case 8:
                str = "RESTRICTED";
                break;
            case 9:
                str = "OTHER";
                break;
            case 10:
                str = "CONNECTION_MANAGER_NOT_SUPPORTED";
                break;
            case 11:
                str = "ANSWERED_ELSEWHERE";
                break;
            case 12:
                str = "CALL_PULLED";
                break;
            default:
                str = "invalid code: " + this.mDisconnectCode;
                break;
        }
        return "DisconnectCause [ Code: (" + str + ") Label: (" + (this.mDisconnectLabel == null ? "" : this.mDisconnectLabel.toString()) + ") Description: (" + (this.mDisconnectDescription == null ? "" : this.mDisconnectDescription.toString()) + ") Reason: (" + (this.mDisconnectReason == null ? "" : this.mDisconnectReason) + ") Tone: (" + this.mToneToPlay + ") ]";
    }
}
