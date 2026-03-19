package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Collections;
import java.util.List;

public class StringParceledListSlice extends BaseParceledListSlice<String> {
    public static final Parcelable.ClassLoaderCreator<StringParceledListSlice> CREATOR = new Parcelable.ClassLoaderCreator<StringParceledListSlice>() {
        @Override
        public StringParceledListSlice createFromParcel(Parcel parcel) {
            return new StringParceledListSlice(parcel, null);
        }

        @Override
        public StringParceledListSlice createFromParcel(Parcel parcel, ClassLoader classLoader) {
            return new StringParceledListSlice(parcel, classLoader);
        }

        @Override
        public StringParceledListSlice[] newArray(int i) {
            return new StringParceledListSlice[i];
        }
    };

    @Override
    public List<String> getList() {
        return super.getList();
    }

    @Override
    public void setInlineCountLimit(int i) {
        super.setInlineCountLimit(i);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }

    public StringParceledListSlice(List<String> list) {
        super(list);
    }

    private StringParceledListSlice(Parcel parcel, ClassLoader classLoader) {
        super(parcel, classLoader);
    }

    public static StringParceledListSlice emptyList() {
        return new StringParceledListSlice(Collections.emptyList());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    protected void writeElement(String str, Parcel parcel, int i) {
        parcel.writeString(str);
    }

    @Override
    protected void writeParcelableCreator(String str, Parcel parcel) {
    }

    @Override
    protected Parcelable.Creator<?> readParcelableCreator(Parcel parcel, ClassLoader classLoader) {
        return Parcel.STRING_CREATOR;
    }
}
