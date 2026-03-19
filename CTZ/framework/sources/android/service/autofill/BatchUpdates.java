package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;

public final class BatchUpdates implements Parcelable {
    public static final Parcelable.Creator<BatchUpdates> CREATOR = new Parcelable.Creator<BatchUpdates>() {
        @Override
        public BatchUpdates createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            int[] iArrCreateIntArray = parcel.createIntArray();
            if (iArrCreateIntArray != null) {
                InternalTransformation[] internalTransformationArr = (InternalTransformation[]) parcel.readParcelableArray(null, InternalTransformation.class);
                int length = iArrCreateIntArray.length;
                for (int i = 0; i < length; i++) {
                    builder.transformChild(iArrCreateIntArray[i], internalTransformationArr[i]);
                }
            }
            RemoteViews remoteViews = (RemoteViews) parcel.readParcelable(null);
            if (remoteViews != null) {
                builder.updateTemplate(remoteViews);
            }
            return builder.build();
        }

        @Override
        public BatchUpdates[] newArray(int i) {
            return new BatchUpdates[i];
        }
    };
    private final ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
    private final RemoteViews mUpdates;

    private BatchUpdates(Builder builder) {
        this.mTransformations = builder.mTransformations;
        this.mUpdates = builder.mUpdates;
    }

    public ArrayList<Pair<Integer, InternalTransformation>> getTransformations() {
        return this.mTransformations;
    }

    public RemoteViews getUpdates() {
        return this.mUpdates;
    }

    public static class Builder {
        private boolean mDestroyed;
        private ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
        private RemoteViews mUpdates;

        public Builder updateTemplate(RemoteViews remoteViews) {
            throwIfDestroyed();
            this.mUpdates = (RemoteViews) Preconditions.checkNotNull(remoteViews);
            return this;
        }

        public Builder transformChild(int i, Transformation transformation) {
            throwIfDestroyed();
            Preconditions.checkArgument(transformation instanceof InternalTransformation, "not provided by Android System: " + transformation);
            if (this.mTransformations == null) {
                this.mTransformations = new ArrayList<>();
            }
            this.mTransformations.add(new Pair<>(Integer.valueOf(i), (InternalTransformation) transformation));
            return this;
        }

        public BatchUpdates build() {
            throwIfDestroyed();
            Preconditions.checkState((this.mUpdates == null && this.mTransformations == null) ? false : true, "must call either updateTemplate() or transformChild() at least once");
            this.mDestroyed = true;
            return new BatchUpdates(this);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder sb = new StringBuilder("BatchUpdates: [");
        sb.append(", transformations=");
        sb.append(this.mTransformations == null ? "N/A" : Integer.valueOf(this.mTransformations.size()));
        sb.append(", updates=");
        sb.append(this.mUpdates);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mTransformations == null) {
            parcel.writeIntArray(null);
        } else {
            int size = this.mTransformations.size();
            int[] iArr = new int[size];
            InternalTransformation[] internalTransformationArr = new InternalTransformation[size];
            for (int i2 = 0; i2 < size; i2++) {
                Pair<Integer, InternalTransformation> pair = this.mTransformations.get(i2);
                iArr[i2] = pair.first.intValue();
                internalTransformationArr[i2] = pair.second;
            }
            parcel.writeIntArray(iArr);
            parcel.writeParcelableArray(internalTransformationArr, i);
        }
        parcel.writeParcelable(this.mUpdates, i);
    }
}
