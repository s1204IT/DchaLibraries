package android.telephony.mbms;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FileServiceInfo extends ServiceInfo implements Parcelable {
    public static final Parcelable.Creator<FileServiceInfo> CREATOR = new Parcelable.Creator<FileServiceInfo>() {
        @Override
        public FileServiceInfo createFromParcel(Parcel parcel) {
            return new FileServiceInfo(parcel);
        }

        @Override
        public FileServiceInfo[] newArray(int i) {
            return new FileServiceInfo[i];
        }
    };
    private final List<FileInfo> files;

    @SystemApi
    public FileServiceInfo(Map<Locale, String> map, String str, List<Locale> list, String str2, Date date, Date date2, List<FileInfo> list2) {
        super(map, str, list, str2, date, date2);
        this.files = new ArrayList(list2);
    }

    FileServiceInfo(Parcel parcel) {
        super(parcel);
        this.files = new ArrayList();
        parcel.readList(this.files, FileInfo.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeList(this.files);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public List<FileInfo> getFiles() {
        return this.files;
    }
}
