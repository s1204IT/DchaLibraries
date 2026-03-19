package android.view.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;
import java.util.Map;

class ParcelableMap extends HashMap<AutofillId, AutofillValue> implements Parcelable {
    public static final Parcelable.Creator<ParcelableMap> CREATOR = new Parcelable.Creator<ParcelableMap>() {
        @Override
        public ParcelableMap createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            ParcelableMap parcelableMap = new ParcelableMap(i);
            for (int i2 = 0; i2 < i; i2++) {
                parcelableMap.put((AutofillId) parcel.readParcelable(null), (AutofillValue) parcel.readParcelable(null));
            }
            return parcelableMap;
        }

        @Override
        public ParcelableMap[] newArray(int i) {
            return new ParcelableMap[i];
        }
    };

    ParcelableMap(int i) {
        super(i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(size());
        for (Map.Entry<AutofillId, AutofillValue> entry : entrySet()) {
            parcel.writeParcelable(entry.getKey(), 0);
            parcel.writeParcelable(entry.getValue(), 0);
        }
    }
}
