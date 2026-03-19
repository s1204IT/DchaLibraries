package android.print;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.UUID;

public final class PrintJobId implements Parcelable {
    public static final Parcelable.Creator<PrintJobId> CREATOR = new Parcelable.Creator<PrintJobId>() {
        @Override
        public PrintJobId createFromParcel(Parcel parcel) {
            return new PrintJobId((String) Preconditions.checkNotNull(parcel.readString()));
        }

        @Override
        public PrintJobId[] newArray(int i) {
            return new PrintJobId[i];
        }
    };
    private final String mValue;

    public PrintJobId() {
        this(UUID.randomUUID().toString());
    }

    public PrintJobId(String str) {
        this.mValue = str;
    }

    public int hashCode() {
        return 31 + this.mValue.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass() && this.mValue.equals(((PrintJobId) obj).mValue)) {
            return true;
        }
        return false;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String flattenToString() {
        return this.mValue;
    }

    public static PrintJobId unflattenFromString(String str) {
        return new PrintJobId(str);
    }
}
