package android.app.backup;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public class RestoreSet implements Parcelable {
    public static final Parcelable.Creator<RestoreSet> CREATOR = new Parcelable.Creator<RestoreSet>() {
        @Override
        public RestoreSet createFromParcel(Parcel parcel) {
            return new RestoreSet(parcel);
        }

        @Override
        public RestoreSet[] newArray(int i) {
            return new RestoreSet[i];
        }
    };
    public String device;
    public String name;
    public long token;

    public RestoreSet() {
    }

    public RestoreSet(String str, String str2, long j) {
        this.name = str;
        this.device = str2;
        this.token = j;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeString(this.device);
        parcel.writeLong(this.token);
    }

    private RestoreSet(Parcel parcel) {
        this.name = parcel.readString();
        this.device = parcel.readString();
        this.token = parcel.readLong();
    }
}
