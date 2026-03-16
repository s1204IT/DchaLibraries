package android.print;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class PrinterId implements Parcelable {
    public static final Parcelable.Creator<PrinterId> CREATOR = new Parcelable.Creator<PrinterId>() {
        @Override
        public PrinterId createFromParcel(Parcel parcel) {
            return new PrinterId(parcel);
        }

        @Override
        public PrinterId[] newArray(int size) {
            return new PrinterId[size];
        }
    };
    private final String mLocalId;
    private final ComponentName mServiceName;

    public PrinterId(ComponentName serviceName, String localId) {
        this.mServiceName = serviceName;
        this.mLocalId = localId;
    }

    private PrinterId(Parcel parcel) {
        this.mServiceName = (ComponentName) parcel.readParcelable(null);
        this.mLocalId = parcel.readString();
    }

    public ComponentName getServiceName() {
        return this.mServiceName;
    }

    public String getLocalId() {
        return this.mLocalId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mServiceName, flags);
        parcel.writeString(this.mLocalId);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object != null && getClass() == object.getClass()) {
            PrinterId other = (PrinterId) object;
            if (this.mServiceName == null) {
                if (other.mServiceName != null) {
                    return false;
                }
            } else if (!this.mServiceName.equals(other.mServiceName)) {
                return false;
            }
            return TextUtils.equals(this.mLocalId, other.mLocalId);
        }
        return false;
    }

    public int hashCode() {
        int hashCode = (this.mServiceName != null ? this.mServiceName.hashCode() : 1) + 31;
        return (hashCode * 31) + this.mLocalId.hashCode();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PrinterId{");
        builder.append("serviceName=").append(this.mServiceName.flattenToString());
        builder.append(", localId=").append(this.mLocalId);
        builder.append('}');
        return builder.toString();
    }
}
