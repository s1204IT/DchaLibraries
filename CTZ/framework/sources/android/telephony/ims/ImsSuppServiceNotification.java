package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

@SystemApi
public final class ImsSuppServiceNotification implements Parcelable {
    public static final Parcelable.Creator<ImsSuppServiceNotification> CREATOR = new Parcelable.Creator<ImsSuppServiceNotification>() {
        @Override
        public ImsSuppServiceNotification createFromParcel(Parcel parcel) {
            return new ImsSuppServiceNotification(parcel);
        }

        @Override
        public ImsSuppServiceNotification[] newArray(int i) {
            return new ImsSuppServiceNotification[i];
        }
    };
    private static final String TAG = "ImsSuppServiceNotification";
    public final int code;
    public final String[] history;
    public final int index;
    public final int notificationType;
    public final String number;
    public final int type;

    public ImsSuppServiceNotification(int i, int i2, int i3, int i4, String str, String[] strArr) {
        this.notificationType = i;
        this.code = i2;
        this.index = i3;
        this.type = i4;
        this.number = str;
        this.history = strArr;
    }

    public ImsSuppServiceNotification(Parcel parcel) {
        this.notificationType = parcel.readInt();
        this.code = parcel.readInt();
        this.index = parcel.readInt();
        this.type = parcel.readInt();
        this.number = parcel.readString();
        this.history = parcel.createStringArray();
    }

    public String toString() {
        return "{ notificationType=" + this.notificationType + ", code=" + this.code + ", index=" + this.index + ", type=" + this.type + ", number=" + this.number + ", history=" + Arrays.toString(this.history) + " }";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.notificationType);
        parcel.writeInt(this.code);
        parcel.writeInt(this.index);
        parcel.writeInt(this.type);
        parcel.writeString(this.number);
        parcel.writeStringArray(this.history);
    }
}
