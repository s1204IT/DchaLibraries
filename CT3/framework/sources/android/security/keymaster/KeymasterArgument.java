package android.security.keymaster;

import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;

abstract class KeymasterArgument implements Parcelable {
    public static final Parcelable.Creator<KeymasterArgument> CREATOR = new Parcelable.Creator<KeymasterArgument>() {
        @Override
        public KeymasterArgument createFromParcel(Parcel in) {
            int pos = in.dataPosition();
            int tag = in.readInt();
            switch (KeymasterDefs.getTagType(tag)) {
                case Integer.MIN_VALUE:
                case KeymasterDefs.KM_BYTES:
                    return new KeymasterBlobArgument(tag, in);
                case KeymasterDefs.KM_ULONG_REP:
                case KeymasterDefs.KM_ULONG:
                    return new KeymasterLongArgument(tag, in);
                case 268435456:
                case 536870912:
                case 805306368:
                case 1073741824:
                    return new KeymasterIntArgument(tag, in);
                case KeymasterDefs.KM_DATE:
                    return new KeymasterDateArgument(tag, in);
                case KeymasterDefs.KM_BOOL:
                    return new KeymasterBooleanArgument(tag, in);
                default:
                    throw new ParcelFormatException("Bad tag: " + tag + " at " + pos);
            }
        }

        @Override
        public KeymasterArgument[] newArray(int size) {
            return new KeymasterArgument[size];
        }
    };
    public final int tag;

    public abstract void writeValue(Parcel parcel);

    protected KeymasterArgument(int tag) {
        this.tag = tag;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.tag);
        writeValue(out);
    }
}
