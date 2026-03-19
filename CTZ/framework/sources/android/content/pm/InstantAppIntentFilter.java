package android.content.pm;

import android.annotation.SystemApi;
import android.content.IntentFilter;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

@SystemApi
public final class InstantAppIntentFilter implements Parcelable {
    public static final Parcelable.Creator<InstantAppIntentFilter> CREATOR = new Parcelable.Creator<InstantAppIntentFilter>() {
        @Override
        public InstantAppIntentFilter createFromParcel(Parcel parcel) {
            return new InstantAppIntentFilter(parcel);
        }

        @Override
        public InstantAppIntentFilter[] newArray(int i) {
            return new InstantAppIntentFilter[i];
        }
    };
    private final List<IntentFilter> mFilters = new ArrayList();
    private final String mSplitName;

    public InstantAppIntentFilter(String str, List<IntentFilter> list) {
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException();
        }
        this.mSplitName = str;
        this.mFilters.addAll(list);
    }

    InstantAppIntentFilter(Parcel parcel) {
        this.mSplitName = parcel.readString();
        parcel.readList(this.mFilters, null);
    }

    public String getSplitName() {
        return this.mSplitName;
    }

    public List<IntentFilter> getFilters() {
        return this.mFilters;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mSplitName);
        parcel.writeList(this.mFilters);
    }
}
