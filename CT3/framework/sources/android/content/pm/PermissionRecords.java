package android.content.pm;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class PermissionRecords implements Parcelable {
    public static final Parcelable.ClassLoaderCreator<PermissionRecords> CREATOR = new Parcelable.ClassLoaderCreator<PermissionRecords>() {
        @Override
        public PermissionRecords createFromParcel(Parcel in) {
            return createFromParcel(in, (ClassLoader) null);
        }

        @Override
        public PermissionRecords createFromParcel(Parcel in, ClassLoader loader) {
            PermissionRecords reocrds = new PermissionRecords();
            reocrds.pkgName = in.readString();
            reocrds.permName = in.readString();
            in.readList(reocrds.requestTimes, loader);
            return reocrds;
        }

        @Override
        public PermissionRecords[] newArray(int size) {
            return new PermissionRecords[size];
        }
    };
    public String permName;
    public String pkgName;
    public List<Long> requestTimes;

    public PermissionRecords() {
        this.requestTimes = new ArrayList();
    }

    public PermissionRecords(String pkgName, String permName, List<Long> requestTimes) {
        this.requestTimes = new ArrayList();
        this.pkgName = pkgName;
        this.permName = permName;
        this.requestTimes = requestTimes;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.requestTimes.size(); i++) {
            builder.append(this.requestTimes.get(i)).append(",");
        }
        String times = builder.toString();
        if (this.requestTimes.size() > 0) {
            times = times.substring(0, times.length() - 2);
        }
        return "PermissionRecords{" + this.pkgName + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.permName + " times(" + times + ")}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeString(this.pkgName);
        dest.writeString(this.permName);
        dest.writeList(this.requestTimes);
    }
}
