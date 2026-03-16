package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ContentProviderResult implements Parcelable {
    public static final Parcelable.Creator<ContentProviderResult> CREATOR = new Parcelable.Creator<ContentProviderResult>() {
        @Override
        public ContentProviderResult createFromParcel(Parcel source) {
            return new ContentProviderResult(source);
        }

        @Override
        public ContentProviderResult[] newArray(int size) {
            return new ContentProviderResult[size];
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

    public ContentProviderResult(int count) {
        this.count = Integer.valueOf(count);
        this.uri = null;
    }

    public ContentProviderResult(Parcel source) {
        int type = source.readInt();
        if (type == 1) {
            this.count = Integer.valueOf(source.readInt());
            this.uri = null;
        } else {
            this.count = null;
            this.uri = Uri.CREATOR.createFromParcel(source);
        }
    }

    public ContentProviderResult(ContentProviderResult cpr, int userId) {
        this.uri = ContentProvider.maybeAddUserId(cpr.uri, userId);
        this.count = cpr.count;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (this.uri == null) {
            dest.writeInt(1);
            dest.writeInt(this.count.intValue());
        } else {
            dest.writeInt(2);
            this.uri.writeToParcel(dest, 0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return this.uri != null ? "ContentProviderResult(uri=" + this.uri.toString() + ")" : "ContentProviderResult(count=" + this.count + ")";
    }
}
