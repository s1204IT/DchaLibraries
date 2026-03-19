package android.security.keymaster;

import android.os.Parcel;

class KeymasterBlobArgument extends KeymasterArgument {
    public final byte[] blob;

    public KeymasterBlobArgument(int tag, byte[] blob) {
        super(tag);
        switch (KeymasterDefs.getTagType(tag)) {
            case Integer.MIN_VALUE:
            case KeymasterDefs.KM_BYTES:
                this.blob = blob;
                return;
            default:
                throw new IllegalArgumentException("Bad blob tag " + tag);
        }
    }

    public KeymasterBlobArgument(int tag, Parcel in) {
        super(tag);
        this.blob = in.createByteArray();
    }

    @Override
    public void writeValue(Parcel out) {
        out.writeByteArray(this.blob);
    }
}
