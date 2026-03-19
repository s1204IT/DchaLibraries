package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Collections;
import java.util.List;

public class ParceledListSlice<T extends Parcelable> extends BaseParceledListSlice<T> {
    public static final Parcelable.ClassLoaderCreator<ParceledListSlice> CREATOR = new Parcelable.ClassLoaderCreator<ParceledListSlice>() {
        @Override
        public ParceledListSlice createFromParcel(Parcel parcel) {
            return new ParceledListSlice(parcel, null);
        }

        @Override
        public ParceledListSlice createFromParcel(Parcel parcel, ClassLoader classLoader) {
            return new ParceledListSlice(parcel, classLoader);
        }

        @Override
        public ParceledListSlice[] newArray(int i) {
            return new ParceledListSlice[i];
        }
    };

    @Override
    public List getList() {
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

    public ParceledListSlice(List<T> list) {
        super(list);
    }

    private ParceledListSlice(Parcel parcel, ClassLoader classLoader) {
        super(parcel, classLoader);
    }

    public static <T extends Parcelable> ParceledListSlice<T> emptyList() {
        return new ParceledListSlice<>(Collections.emptyList());
    }

    @Override
    public int describeContents() {
        List list = getList();
        int iDescribeContents = 0;
        for (int i = 0; i < list.size(); i++) {
            iDescribeContents |= ((Parcelable) list.get(i)).describeContents();
        }
        return iDescribeContents;
    }

    @Override
    protected void writeElement(T t, Parcel parcel, int i) {
        t.writeToParcel(parcel, i);
    }

    @Override
    protected void writeParcelableCreator(T t, Parcel parcel) {
        parcel.writeParcelableCreator(t);
    }

    @Override
    protected Parcelable.Creator<?> readParcelableCreator(Parcel parcel, ClassLoader classLoader) {
        return parcel.readParcelableCreator(classLoader);
    }
}
