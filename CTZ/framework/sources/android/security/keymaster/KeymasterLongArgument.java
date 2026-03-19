package android.security.keymaster;

import android.os.Parcel;

class KeymasterLongArgument extends KeymasterArgument {
    public final long value;

    public KeymasterLongArgument(int i, long j) {
        super(i);
        int tagType = KeymasterDefs.getTagType(i);
        if (tagType != -1610612736 && tagType != 1342177280) {
            throw new IllegalArgumentException("Bad long tag " + i);
        }
        this.value = j;
    }

    public KeymasterLongArgument(int i, Parcel parcel) {
        super(i);
        this.value = parcel.readLong();
    }

    @Override
    public void writeValue(Parcel parcel) {
        parcel.writeLong(this.value);
    }
}
