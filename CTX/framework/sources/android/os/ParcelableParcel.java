package android.os;

import android.os.Parcelable;
import android.util.MathUtils;

public class ParcelableParcel implements Parcelable {
    public static final Parcelable.ClassLoaderCreator<ParcelableParcel> CREATOR = new Parcelable.ClassLoaderCreator<ParcelableParcel>() {
        @Override
        public ParcelableParcel createFromParcel(Parcel parcel) {
            return new ParcelableParcel(parcel, null);
        }

        @Override
        public ParcelableParcel createFromParcel(Parcel parcel, ClassLoader classLoader) {
            return new ParcelableParcel(parcel, classLoader);
        }

        @Override
        public ParcelableParcel[] newArray(int i) {
            return new ParcelableParcel[i];
        }
    };
    final ClassLoader mClassLoader;
    final Parcel mParcel = Parcel.obtain();

    public ParcelableParcel(ClassLoader classLoader) {
        this.mClassLoader = classLoader;
    }

    public ParcelableParcel(Parcel parcel, ClassLoader classLoader) {
        this.mClassLoader = classLoader;
        int i = parcel.readInt();
        if (i < 0) {
            throw new IllegalArgumentException("Negative size read from parcel");
        }
        int iDataPosition = parcel.dataPosition();
        parcel.setDataPosition(MathUtils.addOrThrow(iDataPosition, i));
        this.mParcel.appendFrom(parcel, iDataPosition, i);
    }

    public Parcel getParcel() {
        this.mParcel.setDataPosition(0);
        return this.mParcel;
    }

    public ClassLoader getClassLoader() {
        return this.mClassLoader;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mParcel.dataSize());
        parcel.appendFrom(this.mParcel, 0, this.mParcel.dataSize());
    }
}
