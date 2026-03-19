package android.telephony.euicc;

import android.os.Parcel;
import android.os.Parcelable;

public final class EuiccInfo implements Parcelable {
    public static final Parcelable.Creator<EuiccInfo> CREATOR = new Parcelable.Creator<EuiccInfo>() {
        @Override
        public EuiccInfo createFromParcel(Parcel parcel) {
            return new EuiccInfo(parcel);
        }

        @Override
        public EuiccInfo[] newArray(int i) {
            return new EuiccInfo[i];
        }
    };
    private final String osVersion;

    public String getOsVersion() {
        return this.osVersion;
    }

    public EuiccInfo(String str) {
        this.osVersion = str;
    }

    private EuiccInfo(Parcel parcel) {
        this.osVersion = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.osVersion);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
