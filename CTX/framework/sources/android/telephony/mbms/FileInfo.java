package android.telephony.mbms;

import android.annotation.SystemApi;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class FileInfo implements Parcelable {
    public static final Parcelable.Creator<FileInfo> CREATOR = new Parcelable.Creator<FileInfo>() {
        @Override
        public FileInfo createFromParcel(Parcel parcel) {
            return new FileInfo(parcel);
        }

        @Override
        public FileInfo[] newArray(int i) {
            return new FileInfo[i];
        }
    };
    private final String mimeType;
    private final Uri uri;

    @SystemApi
    public FileInfo(Uri uri, String str) {
        this.uri = uri;
        this.mimeType = str;
    }

    private FileInfo(Parcel parcel) {
        this.uri = (Uri) parcel.readParcelable(null);
        this.mimeType = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.uri, i);
        parcel.writeString(this.mimeType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Uri getUri() {
        return this.uri;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FileInfo fileInfo = (FileInfo) obj;
        if (Objects.equals(this.uri, fileInfo.uri) && Objects.equals(this.mimeType, fileInfo.mimeType)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.uri, this.mimeType);
    }
}
