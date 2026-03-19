package android.telephony.euicc;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

@SystemApi
public final class EuiccNotification implements Parcelable {
    public static final int ALL_EVENTS = 15;
    public static final Parcelable.Creator<EuiccNotification> CREATOR = new Parcelable.Creator<EuiccNotification>() {
        @Override
        public EuiccNotification createFromParcel(Parcel parcel) {
            return new EuiccNotification(parcel);
        }

        @Override
        public EuiccNotification[] newArray(int i) {
            return new EuiccNotification[i];
        }
    };
    public static final int EVENT_DELETE = 8;
    public static final int EVENT_DISABLE = 4;
    public static final int EVENT_ENABLE = 2;
    public static final int EVENT_INSTALL = 1;
    private final byte[] mData;
    private final int mEvent;
    private final int mSeq;
    private final String mTargetAddr;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
    }

    public EuiccNotification(int i, String str, int i2, byte[] bArr) {
        this.mSeq = i;
        this.mTargetAddr = str;
        this.mEvent = i2;
        this.mData = bArr;
    }

    public int getSeq() {
        return this.mSeq;
    }

    public String getTargetAddr() {
        return this.mTargetAddr;
    }

    public int getEvent() {
        return this.mEvent;
    }

    public byte[] getData() {
        return this.mData;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EuiccNotification euiccNotification = (EuiccNotification) obj;
        if (this.mSeq == euiccNotification.mSeq && Objects.equals(this.mTargetAddr, euiccNotification.mTargetAddr) && this.mEvent == euiccNotification.mEvent && Arrays.equals(this.mData, euiccNotification.mData)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((this.mSeq + 31) * 31) + Objects.hashCode(this.mTargetAddr)) * 31) + this.mEvent)) + Arrays.hashCode(this.mData);
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("EuiccNotification (seq=");
        sb.append(this.mSeq);
        sb.append(", targetAddr=");
        sb.append(this.mTargetAddr);
        sb.append(", event=");
        sb.append(this.mEvent);
        sb.append(", data=");
        if (this.mData == null) {
            str = "null";
        } else {
            str = "byte[" + this.mData.length + "]";
        }
        sb.append(str);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSeq);
        parcel.writeString(this.mTargetAddr);
        parcel.writeInt(this.mEvent);
        parcel.writeByteArray(this.mData);
    }

    private EuiccNotification(Parcel parcel) {
        this.mSeq = parcel.readInt();
        this.mTargetAddr = parcel.readString();
        this.mEvent = parcel.readInt();
        this.mData = parcel.createByteArray();
    }
}
