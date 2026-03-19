package android.app;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.SettingsStringUtil;

public class GrantedUriPermission implements Parcelable {
    public static final Parcelable.Creator<GrantedUriPermission> CREATOR = new Parcelable.Creator<GrantedUriPermission>() {
        @Override
        public GrantedUriPermission createFromParcel(Parcel parcel) {
            return new GrantedUriPermission(parcel);
        }

        @Override
        public GrantedUriPermission[] newArray(int i) {
            return new GrantedUriPermission[i];
        }
    };
    public final String packageName;
    public final Uri uri;

    public GrantedUriPermission(Uri uri, String str) {
        this.uri = uri;
        this.packageName = str;
    }

    public String toString() {
        return this.packageName + SettingsStringUtil.DELIMITER + this.uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.uri, i);
        parcel.writeString(this.packageName);
    }

    private GrantedUriPermission(Parcel parcel) {
        this.uri = (Uri) parcel.readParcelable(null);
        this.packageName = parcel.readString();
    }
}
