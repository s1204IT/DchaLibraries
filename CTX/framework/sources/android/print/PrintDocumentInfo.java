package android.print;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PrintDocumentInfo implements Parcelable {
    public static final int CONTENT_TYPE_DOCUMENT = 0;
    public static final int CONTENT_TYPE_PHOTO = 1;
    public static final int CONTENT_TYPE_UNKNOWN = -1;
    public static final Parcelable.Creator<PrintDocumentInfo> CREATOR = new Parcelable.Creator<PrintDocumentInfo>() {
        @Override
        public PrintDocumentInfo createFromParcel(Parcel parcel) {
            return new PrintDocumentInfo(parcel);
        }

        @Override
        public PrintDocumentInfo[] newArray(int i) {
            return new PrintDocumentInfo[i];
        }
    };
    public static final int PAGE_COUNT_UNKNOWN = -1;
    private int mContentType;
    private long mDataSize;
    private String mName;
    private int mPageCount;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentType {
    }

    private PrintDocumentInfo() {
    }

    private PrintDocumentInfo(PrintDocumentInfo printDocumentInfo) {
        this.mName = printDocumentInfo.mName;
        this.mPageCount = printDocumentInfo.mPageCount;
        this.mContentType = printDocumentInfo.mContentType;
        this.mDataSize = printDocumentInfo.mDataSize;
    }

    private PrintDocumentInfo(Parcel parcel) {
        this.mName = (String) Preconditions.checkStringNotEmpty(parcel.readString());
        this.mPageCount = parcel.readInt();
        Preconditions.checkArgument(this.mPageCount == -1 || this.mPageCount > 0);
        this.mContentType = parcel.readInt();
        this.mDataSize = Preconditions.checkArgumentNonnegative(parcel.readLong());
    }

    public String getName() {
        return this.mName;
    }

    public int getPageCount() {
        return this.mPageCount;
    }

    public int getContentType() {
        return this.mContentType;
    }

    public long getDataSize() {
        return this.mDataSize;
    }

    public void setDataSize(long j) {
        this.mDataSize = j;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeInt(this.mPageCount);
        parcel.writeInt(this.mContentType);
        parcel.writeLong(this.mDataSize);
    }

    public int hashCode() {
        return (31 * ((((((((this.mName != null ? this.mName.hashCode() : 0) + 31) * 31) + this.mContentType) * 31) + this.mPageCount) * 31) + ((int) this.mDataSize))) + ((int) (this.mDataSize >> 32));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PrintDocumentInfo printDocumentInfo = (PrintDocumentInfo) obj;
        if (TextUtils.equals(this.mName, printDocumentInfo.mName) && this.mContentType == printDocumentInfo.mContentType && this.mPageCount == printDocumentInfo.mPageCount && this.mDataSize == printDocumentInfo.mDataSize) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "PrintDocumentInfo{name=" + this.mName + ", pageCount=" + this.mPageCount + ", contentType=" + contentTypeToString(this.mContentType) + ", dataSize=" + this.mDataSize + "}";
    }

    private String contentTypeToString(int i) {
        switch (i) {
            case 0:
                return "CONTENT_TYPE_DOCUMENT";
            case 1:
                return "CONTENT_TYPE_PHOTO";
            default:
                return "CONTENT_TYPE_UNKNOWN";
        }
    }

    public static final class Builder {
        private final PrintDocumentInfo mPrototype;

        public Builder(String str) {
            if (TextUtils.isEmpty(str)) {
                throw new IllegalArgumentException("name cannot be empty");
            }
            this.mPrototype = new PrintDocumentInfo();
            this.mPrototype.mName = str;
        }

        public Builder setPageCount(int i) {
            if (i >= 0 || i == -1) {
                this.mPrototype.mPageCount = i;
                return this;
            }
            throw new IllegalArgumentException("pageCount must be greater than or equal to zero or DocumentInfo#PAGE_COUNT_UNKNOWN");
        }

        public Builder setContentType(int i) {
            this.mPrototype.mContentType = i;
            return this;
        }

        public PrintDocumentInfo build() {
            if (this.mPrototype.mPageCount == 0) {
                this.mPrototype.mPageCount = -1;
            }
            return new PrintDocumentInfo();
        }
    }
}
