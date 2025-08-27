package androidx.versionedparcelable;

import android.os.Parcel;
import android.util.SparseIntArray;

/* loaded from: classes.dex */
class VersionedParcelParcel extends VersionedParcel {
    private int mCurrentField;
    private final int mEnd;
    private int mNextRead;
    private final int mOffset;
    private final Parcel mParcel;
    private final SparseIntArray mPositionLookup;
    private final String mPrefix;

    VersionedParcelParcel(Parcel p) {
        this(p, p.dataPosition(), p.dataSize(), "");
    }

    VersionedParcelParcel(Parcel p, int offset, int end, String prefix) {
        this.mPositionLookup = new SparseIntArray();
        this.mCurrentField = -1;
        this.mNextRead = 0;
        this.mParcel = p;
        this.mOffset = offset;
        this.mEnd = end;
        this.mNextRead = this.mOffset;
        this.mPrefix = prefix;
    }

    @Override // androidx.versionedparcelable.VersionedParcel
    public void closeField() {
        if (this.mCurrentField >= 0) {
            int currentFieldPosition = this.mPositionLookup.get(this.mCurrentField);
            int position = this.mParcel.dataPosition();
            int size = position - currentFieldPosition;
            this.mParcel.setDataPosition(currentFieldPosition);
            this.mParcel.writeInt(size);
            this.mParcel.setDataPosition(position);
        }
    }

    @Override // androidx.versionedparcelable.VersionedParcel
    protected VersionedParcel createSubParcel() {
        return new VersionedParcelParcel(this.mParcel, this.mParcel.dataPosition(), this.mNextRead == this.mOffset ? this.mEnd : this.mNextRead, this.mPrefix + "  ");
    }

    @Override // androidx.versionedparcelable.VersionedParcel
    public void writeString(String val) {
        this.mParcel.writeString(val);
    }

    @Override // androidx.versionedparcelable.VersionedParcel
    public String readString() {
        return this.mParcel.readString();
    }
}
