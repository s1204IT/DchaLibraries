package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ContentProviderResult implements Parcelable {
    public static final Parcelable.Creator<ContentProviderResult> CREATOR = new Parcelable.Creator<ContentProviderResult>() {
        @Override
        public ContentProviderResult createFromParcel(Parcel parcel) {
            return new ContentProviderResult(parcel);
        }

        @Override
        public ContentProviderResult[] newArray(int i) {
            return new ContentProviderResult[i];
        }
    };
    public final Integer count;
    public final Uri uri;

    public ContentProviderResult(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        this.uri = uri;
        this.count = null;
    }

    public ContentProviderResult(int i) {
        this.count = Integer.valueOf(i);
        this.uri = null;
    }

    public ContentProviderResult(Parcel parcel) {
        if (parcel.readInt() == 1) {
            this.count = Integer.valueOf(parcel.readInt());
            this.uri = null;
        } else {
            this.count = null;
            this.uri = Uri.CREATOR.createFromParcel(parcel);
        }
    }

    public ContentProviderResult(ContentProviderResult contentProviderResult, int i) {
        this.uri = ContentProvider.maybeAddUserId(contentProviderResult.uri, i);
        this.count = contentProviderResult.count;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.uri == null) {
            parcel.writeInt(1);
            parcel.writeInt(this.count.intValue());
        } else {
            parcel.writeInt(2);
            this.uri.writeToParcel(parcel, 0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        if (this.uri != null) {
            return "ContentProviderResult(uri=" + this.uri.toString() + ")";
        }
        return "ContentProviderResult(count=" + this.count + ")";
    }
}
