package android.service.autofill;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;

public final class SaveRequest implements Parcelable {
    public static final Parcelable.Creator<SaveRequest> CREATOR = new Parcelable.Creator<SaveRequest>() {
        @Override
        public SaveRequest createFromParcel(Parcel parcel) {
            return new SaveRequest(parcel);
        }

        @Override
        public SaveRequest[] newArray(int i) {
            return new SaveRequest[i];
        }
    };
    private final Bundle mClientState;
    private final ArrayList<String> mDatasetIds;
    private final ArrayList<FillContext> mFillContexts;

    public SaveRequest(ArrayList<FillContext> arrayList, Bundle bundle, ArrayList<String> arrayList2) {
        this.mFillContexts = (ArrayList) Preconditions.checkNotNull(arrayList, "fillContexts");
        this.mClientState = bundle;
        this.mDatasetIds = arrayList2;
    }

    private SaveRequest(Parcel parcel) {
        this(parcel.createTypedArrayList(FillContext.CREATOR), parcel.readBundle(), parcel.createStringArrayList());
    }

    public List<FillContext> getFillContexts() {
        return this.mFillContexts;
    }

    public Bundle getClientState() {
        return this.mClientState;
    }

    public List<String> getDatasetIds() {
        return this.mDatasetIds;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(this.mFillContexts, i);
        parcel.writeBundle(this.mClientState);
        parcel.writeStringList(this.mDatasetIds);
    }
}
