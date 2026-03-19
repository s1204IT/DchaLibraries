package android.app.slice;

import android.os.Parcel;
import android.os.Parcelable;

public final class SliceSpec implements Parcelable {
    public static final Parcelable.Creator<SliceSpec> CREATOR = new Parcelable.Creator<SliceSpec>() {
        @Override
        public SliceSpec createFromParcel(Parcel parcel) {
            return new SliceSpec(parcel);
        }

        @Override
        public SliceSpec[] newArray(int i) {
            return new SliceSpec[i];
        }
    };
    private final int mRevision;
    private final String mType;

    public SliceSpec(String str, int i) {
        this.mType = str;
        this.mRevision = i;
    }

    public SliceSpec(Parcel parcel) {
        this.mType = parcel.readString();
        this.mRevision = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mType);
        parcel.writeInt(this.mRevision);
    }

    public String getType() {
        return this.mType;
    }

    public int getRevision() {
        return this.mRevision;
    }

    public boolean canRender(SliceSpec sliceSpec) {
        return this.mType.equals(sliceSpec.mType) && this.mRevision >= sliceSpec.mRevision;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SliceSpec)) {
            return false;
        }
        SliceSpec sliceSpec = (SliceSpec) obj;
        return this.mType.equals(sliceSpec.mType) && this.mRevision == sliceSpec.mRevision;
    }

    public String toString() {
        return String.format("SliceSpec{%s,%d}", this.mType, Integer.valueOf(this.mRevision));
    }
}
