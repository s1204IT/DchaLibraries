package androidx.versionedparcelable;

import android.os.Parcel;
import android.util.SparseIntArray;

class VersionedParcelParcel extends VersionedParcel {
    private int mCurrentField;
    private final int mEnd;
    private int mNextRead;
    private final int mOffset;
    private final Parcel mParcel;
    private final SparseIntArray mPositionLookup;
    private final String mPrefix;

    VersionedParcelParcel(Parcel parcel) {
        this(parcel, parcel.dataPosition(), parcel.dataSize(), "");
    }

    VersionedParcelParcel(Parcel parcel, int i, int i2, String str) {
        this.mPositionLookup = new SparseIntArray();
        this.mCurrentField = -1;
        this.mNextRead = 0;
        this.mParcel = parcel;
        this.mOffset = i;
        this.mEnd = i2;
        this.mNextRead = this.mOffset;
        this.mPrefix = str;
    }

    @Override
    public void closeField() {
        if (this.mCurrentField >= 0) {
            int i = this.mPositionLookup.get(this.mCurrentField);
            int iDataPosition = this.mParcel.dataPosition();
            this.mParcel.setDataPosition(i);
            this.mParcel.writeInt(iDataPosition - i);
            this.mParcel.setDataPosition(iDataPosition);
        }
    }

    @Override
    protected VersionedParcel createSubParcel() {
        return new VersionedParcelParcel(this.mParcel, this.mParcel.dataPosition(), this.mNextRead == this.mOffset ? this.mEnd : this.mNextRead, this.mPrefix + "  ");
    }

    @Override
    public String readString() {
        return this.mParcel.readString();
    }

    @Override
    public void writeString(String str) {
        this.mParcel.writeString(str);
    }
}
