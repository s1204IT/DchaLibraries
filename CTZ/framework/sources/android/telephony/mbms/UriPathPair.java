package android.telephony.mbms;

import android.annotation.SystemApi;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class UriPathPair implements Parcelable {
    public static final Parcelable.Creator<UriPathPair> CREATOR = new Parcelable.Creator<UriPathPair>() {
        @Override
        public UriPathPair createFromParcel(Parcel parcel) {
            return new UriPathPair(parcel);
        }

        @Override
        public UriPathPair[] newArray(int i) {
            return new UriPathPair[i];
        }
    };
    private final Uri mContentUri;
    private final Uri mFilePathUri;

    public UriPathPair(Uri uri, Uri uri2) {
        if (uri == null || !ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            throw new IllegalArgumentException("File URI must have file scheme");
        }
        if (uri2 == null || !"content".equals(uri2.getScheme())) {
            throw new IllegalArgumentException("Content URI must have content scheme");
        }
        this.mFilePathUri = uri;
        this.mContentUri = uri2;
    }

    private UriPathPair(Parcel parcel) {
        this.mFilePathUri = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
        this.mContentUri = (Uri) parcel.readParcelable(Uri.class.getClassLoader());
    }

    public Uri getFilePathUri() {
        return this.mFilePathUri;
    }

    public Uri getContentUri() {
        return this.mContentUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mFilePathUri, i);
        parcel.writeParcelable(this.mContentUri, i);
    }
}
