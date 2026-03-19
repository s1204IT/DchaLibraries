package android.app.timezone;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.TimeZoneRulesDataContract;
import android.text.format.DateFormat;

public final class DistroRulesVersion implements Parcelable {
    public static final Parcelable.Creator<DistroRulesVersion> CREATOR = new Parcelable.Creator<DistroRulesVersion>() {
        @Override
        public DistroRulesVersion createFromParcel(Parcel parcel) {
            return new DistroRulesVersion(parcel.readString(), parcel.readInt());
        }

        @Override
        public DistroRulesVersion[] newArray(int i) {
            return new DistroRulesVersion[i];
        }
    };
    private final int mRevision;
    private final String mRulesVersion;

    public DistroRulesVersion(String str, int i) {
        this.mRulesVersion = Utils.validateRulesVersion("rulesVersion", str);
        this.mRevision = Utils.validateVersion(TimeZoneRulesDataContract.Operation.COLUMN_REVISION, i);
    }

    public String getRulesVersion() {
        return this.mRulesVersion;
    }

    public int getRevision() {
        return this.mRevision;
    }

    public boolean isOlderThan(DistroRulesVersion distroRulesVersion) {
        int iCompareTo = this.mRulesVersion.compareTo(distroRulesVersion.mRulesVersion);
        if (iCompareTo < 0) {
            return true;
        }
        if (iCompareTo <= 0 && this.mRevision < distroRulesVersion.mRevision) {
            return true;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mRulesVersion);
        parcel.writeInt(this.mRevision);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DistroRulesVersion distroRulesVersion = (DistroRulesVersion) obj;
        if (this.mRevision != distroRulesVersion.mRevision) {
            return false;
        }
        return this.mRulesVersion.equals(distroRulesVersion.mRulesVersion);
    }

    public int hashCode() {
        return (31 * this.mRulesVersion.hashCode()) + this.mRevision;
    }

    public String toString() {
        return "DistroRulesVersion{mRulesVersion='" + this.mRulesVersion + DateFormat.QUOTE + ", mRevision='" + this.mRevision + DateFormat.QUOTE + '}';
    }

    public String toDumpString() {
        return this.mRulesVersion + "," + this.mRevision;
    }
}
