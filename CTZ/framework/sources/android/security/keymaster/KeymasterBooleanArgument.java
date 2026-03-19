package android.security.keymaster;

import android.os.Parcel;

class KeymasterBooleanArgument extends KeymasterArgument {
    public final boolean value;

    public KeymasterBooleanArgument(int i) {
        super(i);
        this.value = true;
        if (KeymasterDefs.getTagType(i) != 1879048192) {
            throw new IllegalArgumentException("Bad bool tag " + i);
        }
    }

    public KeymasterBooleanArgument(int i, Parcel parcel) {
        super(i);
        this.value = true;
    }

    @Override
    public void writeValue(Parcel parcel) {
    }
}
