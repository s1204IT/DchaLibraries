package android.os;

import android.os.Parcelable;

public final class ParcelableException extends RuntimeException implements Parcelable {
    public static final Parcelable.Creator<ParcelableException> CREATOR = new Parcelable.Creator<ParcelableException>() {
        @Override
        public ParcelableException createFromParcel(Parcel parcel) {
            return new ParcelableException(ParcelableException.readFromParcel(parcel));
        }

        @Override
        public ParcelableException[] newArray(int i) {
            return new ParcelableException[i];
        }
    };

    public ParcelableException(Throwable th) {
        super(th);
    }

    public <T extends Throwable> void maybeRethrow(Class<T> cls) throws Throwable {
        if (cls.isAssignableFrom(getCause().getClass())) {
            throw getCause();
        }
    }

    public static Throwable readFromParcel(Parcel parcel) {
        String string = parcel.readString();
        String string2 = parcel.readString();
        try {
            Class<?> cls = Class.forName(string, true, Parcelable.class.getClassLoader());
            if (Throwable.class.isAssignableFrom(cls)) {
                return (Throwable) cls.getConstructor(String.class).newInstance(string2);
            }
        } catch (ReflectiveOperationException e) {
        }
        return new RuntimeException(string + ": " + string2);
    }

    public static void writeToParcel(Parcel parcel, Throwable th) {
        parcel.writeString(th.getClass().getName());
        parcel.writeString(th.getMessage());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        writeToParcel(parcel, getCause());
    }
}
