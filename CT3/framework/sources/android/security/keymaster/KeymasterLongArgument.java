package android.security.keymaster;

import android.os.Parcel;

class KeymasterLongArgument extends KeymasterArgument {
    public final long value;

    public KeymasterLongArgument(int tag, long value) {
        super(tag);
        switch (KeymasterDefs.getTagType(tag)) {
            case KeymasterDefs.KM_ULONG_REP:
            case KeymasterDefs.KM_ULONG:
                this.value = value;
                return;
            default:
                throw new IllegalArgumentException("Bad long tag " + tag);
        }
    }

    public KeymasterLongArgument(int tag, Parcel in) {
        super(tag);
        this.value = in.readLong();
    }

    @Override
    public void writeValue(Parcel out) {
        out.writeLong(this.value);
    }
}
