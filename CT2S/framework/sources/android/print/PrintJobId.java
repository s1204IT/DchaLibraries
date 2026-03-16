package android.print;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.UUID;

public final class PrintJobId implements Parcelable {
    public static final Parcelable.Creator<PrintJobId> CREATOR = new Parcelable.Creator<PrintJobId>() {
        @Override
        public PrintJobId createFromParcel(Parcel parcel) {
            return new PrintJobId(parcel.readString());
        }

        @Override
        public PrintJobId[] newArray(int size) {
            return new PrintJobId[size];
        }
    };
    private final String mValue;

    public PrintJobId() {
        this(UUID.randomUUID().toString());
    }

    public PrintJobId(String value) {
        this.mValue = value;
    }

    public int hashCode() {
        int result = (this.mValue != null ? this.mValue.hashCode() : 0) + 31;
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            PrintJobId other = (PrintJobId) obj;
            return TextUtils.equals(this.mValue, other.mValue);
        }
        return false;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String flattenToString() {
        return this.mValue;
    }

    public static PrintJobId unflattenFromString(String string) {
        return new PrintJobId(string);
    }
}
