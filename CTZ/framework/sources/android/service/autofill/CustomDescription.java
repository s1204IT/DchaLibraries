package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;

public final class CustomDescription implements Parcelable {
    public static final Parcelable.Creator<CustomDescription> CREATOR = new Parcelable.Creator<CustomDescription>() {
        @Override
        public CustomDescription createFromParcel(Parcel parcel) {
            RemoteViews remoteViews = (RemoteViews) parcel.readParcelable(null);
            if (remoteViews == null) {
                return null;
            }
            Builder builder = new Builder(remoteViews);
            int[] iArrCreateIntArray = parcel.createIntArray();
            if (iArrCreateIntArray != null) {
                InternalTransformation[] internalTransformationArr = (InternalTransformation[]) parcel.readParcelableArray(null, InternalTransformation.class);
                int length = iArrCreateIntArray.length;
                for (int i = 0; i < length; i++) {
                    builder.addChild(iArrCreateIntArray[i], internalTransformationArr[i]);
                }
            }
            InternalValidator[] internalValidatorArr = (InternalValidator[]) parcel.readParcelableArray(null, InternalValidator.class);
            if (internalValidatorArr != null) {
                BatchUpdates[] batchUpdatesArr = (BatchUpdates[]) parcel.readParcelableArray(null, BatchUpdates.class);
                int length2 = internalValidatorArr.length;
                for (int i2 = 0; i2 < length2; i2++) {
                    builder.batchUpdate(internalValidatorArr[i2], batchUpdatesArr[i2]);
                }
            }
            return builder.build();
        }

        @Override
        public CustomDescription[] newArray(int i) {
            return new CustomDescription[i];
        }
    };
    private final RemoteViews mPresentation;
    private final ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
    private final ArrayList<Pair<InternalValidator, BatchUpdates>> mUpdates;

    private CustomDescription(Builder builder) {
        this.mPresentation = builder.mPresentation;
        this.mTransformations = builder.mTransformations;
        this.mUpdates = builder.mUpdates;
    }

    public RemoteViews getPresentation() {
        return this.mPresentation;
    }

    public ArrayList<Pair<Integer, InternalTransformation>> getTransformations() {
        return this.mTransformations;
    }

    public ArrayList<Pair<InternalValidator, BatchUpdates>> getUpdates() {
        return this.mUpdates;
    }

    public static class Builder {
        private boolean mDestroyed;
        private final RemoteViews mPresentation;
        private ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
        private ArrayList<Pair<InternalValidator, BatchUpdates>> mUpdates;

        public Builder(RemoteViews remoteViews) {
            this.mPresentation = (RemoteViews) Preconditions.checkNotNull(remoteViews);
        }

        public Builder addChild(int i, Transformation transformation) {
            throwIfDestroyed();
            Preconditions.checkArgument(transformation instanceof InternalTransformation, "not provided by Android System: " + transformation);
            if (this.mTransformations == null) {
                this.mTransformations = new ArrayList<>();
            }
            this.mTransformations.add(new Pair<>(Integer.valueOf(i), (InternalTransformation) transformation));
            return this;
        }

        public Builder batchUpdate(Validator validator, BatchUpdates batchUpdates) {
            throwIfDestroyed();
            Preconditions.checkArgument(validator instanceof InternalValidator, "not provided by Android System: " + validator);
            Preconditions.checkNotNull(batchUpdates);
            if (this.mUpdates == null) {
                this.mUpdates = new ArrayList<>();
            }
            this.mUpdates.add(new Pair<>((InternalValidator) validator, batchUpdates));
            return this;
        }

        public CustomDescription build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            return new CustomDescription(this);
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
        StringBuilder sb = new StringBuilder("CustomDescription: [presentation=");
        sb.append(this.mPresentation);
        sb.append(", transformations=");
        sb.append(this.mTransformations == null ? "N/A" : Integer.valueOf(this.mTransformations.size()));
        sb.append(", updates=");
        sb.append(this.mUpdates == null ? "N/A" : Integer.valueOf(this.mUpdates.size()));
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mPresentation, i);
        if (this.mPresentation == null) {
            return;
        }
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
        if (this.mUpdates == null) {
            parcel.writeParcelableArray(null, i);
            return;
        }
        int size2 = this.mUpdates.size();
        InternalValidator[] internalValidatorArr = new InternalValidator[size2];
        BatchUpdates[] batchUpdatesArr = new BatchUpdates[size2];
        for (int i3 = 0; i3 < size2; i3++) {
            Pair<InternalValidator, BatchUpdates> pair2 = this.mUpdates.get(i3);
            internalValidatorArr[i3] = pair2.first;
            batchUpdatesArr[i3] = pair2.second;
        }
        parcel.writeParcelableArray(internalValidatorArr, i);
        parcel.writeParcelableArray(batchUpdatesArr, i);
    }
}
