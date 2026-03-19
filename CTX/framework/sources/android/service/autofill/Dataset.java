package android.service.autofill;

import android.content.IntentSender;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class Dataset implements Parcelable {
    public static final Parcelable.Creator<Dataset> CREATOR = new Parcelable.Creator<Dataset>() {
        @Override
        public Dataset createFromParcel(Parcel parcel) {
            Builder builder;
            RemoteViews remoteViews = (RemoteViews) parcel.readParcelable(null);
            if (remoteViews == null) {
                builder = new Builder();
            } else {
                builder = new Builder(remoteViews);
            }
            ArrayList arrayListCreateTypedArrayList = parcel.createTypedArrayList(AutofillId.CREATOR);
            ArrayList arrayListCreateTypedArrayList2 = parcel.createTypedArrayList(AutofillValue.CREATOR);
            ArrayList arrayListCreateTypedArrayList3 = parcel.createTypedArrayList(RemoteViews.CREATOR);
            ArrayList arrayListCreateTypedArrayList4 = parcel.createTypedArrayList(DatasetFieldFilter.CREATOR);
            for (int i = 0; i < arrayListCreateTypedArrayList.size(); i++) {
                builder.setLifeTheUniverseAndEverything((AutofillId) arrayListCreateTypedArrayList.get(i), (AutofillValue) arrayListCreateTypedArrayList2.get(i), (RemoteViews) arrayListCreateTypedArrayList3.get(i), (DatasetFieldFilter) arrayListCreateTypedArrayList4.get(i));
            }
            builder.setAuthentication((IntentSender) parcel.readParcelable(null));
            builder.setId(parcel.readString());
            return builder.build();
        }

        @Override
        public Dataset[] newArray(int i) {
            return new Dataset[i];
        }
    };
    private final IntentSender mAuthentication;
    private final ArrayList<DatasetFieldFilter> mFieldFilters;
    private final ArrayList<AutofillId> mFieldIds;
    private final ArrayList<RemoteViews> mFieldPresentations;
    private final ArrayList<AutofillValue> mFieldValues;
    String mId;
    private final RemoteViews mPresentation;

    private Dataset(Builder builder) {
        this.mFieldIds = builder.mFieldIds;
        this.mFieldValues = builder.mFieldValues;
        this.mFieldPresentations = builder.mFieldPresentations;
        this.mFieldFilters = builder.mFieldFilters;
        this.mPresentation = builder.mPresentation;
        this.mAuthentication = builder.mAuthentication;
        this.mId = builder.mId;
    }

    public ArrayList<AutofillId> getFieldIds() {
        return this.mFieldIds;
    }

    public ArrayList<AutofillValue> getFieldValues() {
        return this.mFieldValues;
    }

    public RemoteViews getFieldPresentation(int i) {
        RemoteViews remoteViews = this.mFieldPresentations.get(i);
        return remoteViews != null ? remoteViews : this.mPresentation;
    }

    public DatasetFieldFilter getFilter(int i) {
        return this.mFieldFilters.get(i);
    }

    public IntentSender getAuthentication() {
        return this.mAuthentication;
    }

    public boolean isEmpty() {
        return this.mFieldIds == null || this.mFieldIds.isEmpty();
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder sb = new StringBuilder("Dataset[");
        if (this.mId == null) {
            sb.append("noId");
        } else {
            sb.append("id=");
            sb.append(this.mId.length());
            sb.append("_chars");
        }
        if (this.mFieldIds != null) {
            sb.append(", fieldIds=");
            sb.append(this.mFieldIds);
        }
        if (this.mFieldValues != null) {
            sb.append(", fieldValues=");
            sb.append(this.mFieldValues);
        }
        if (this.mFieldPresentations != null) {
            sb.append(", fieldPresentations=");
            sb.append(this.mFieldPresentations.size());
        }
        if (this.mFieldFilters != null) {
            sb.append(", fieldFilters=");
            sb.append(this.mFieldFilters.size());
        }
        if (this.mPresentation != null) {
            sb.append(", hasPresentation");
        }
        if (this.mAuthentication != null) {
            sb.append(", hasAuthentication");
        }
        sb.append(']');
        return sb.toString();
    }

    public String getId() {
        return this.mId;
    }

    public static final class Builder {
        private IntentSender mAuthentication;
        private boolean mDestroyed;
        private ArrayList<DatasetFieldFilter> mFieldFilters;
        private ArrayList<AutofillId> mFieldIds;
        private ArrayList<RemoteViews> mFieldPresentations;
        private ArrayList<AutofillValue> mFieldValues;
        private String mId;
        private RemoteViews mPresentation;

        public Builder(RemoteViews remoteViews) {
            Preconditions.checkNotNull(remoteViews, "presentation must be non-null");
            this.mPresentation = remoteViews;
        }

        public Builder() {
        }

        public Builder setAuthentication(IntentSender intentSender) {
            throwIfDestroyed();
            this.mAuthentication = intentSender;
            return this;
        }

        public Builder setId(String str) {
            throwIfDestroyed();
            this.mId = str;
            return this;
        }

        public Builder setValue(AutofillId autofillId, AutofillValue autofillValue) {
            throwIfDestroyed();
            setLifeTheUniverseAndEverything(autofillId, autofillValue, null, null);
            return this;
        }

        public Builder setValue(AutofillId autofillId, AutofillValue autofillValue, RemoteViews remoteViews) {
            throwIfDestroyed();
            Preconditions.checkNotNull(remoteViews, "presentation cannot be null");
            setLifeTheUniverseAndEverything(autofillId, autofillValue, remoteViews, null);
            return this;
        }

        public Builder setValue(AutofillId autofillId, AutofillValue autofillValue, Pattern pattern) {
            throwIfDestroyed();
            Preconditions.checkState(this.mPresentation != null, "Dataset presentation not set on constructor");
            setLifeTheUniverseAndEverything(autofillId, autofillValue, null, new DatasetFieldFilter(pattern));
            return this;
        }

        public Builder setValue(AutofillId autofillId, AutofillValue autofillValue, Pattern pattern, RemoteViews remoteViews) {
            throwIfDestroyed();
            Preconditions.checkNotNull(remoteViews, "presentation cannot be null");
            setLifeTheUniverseAndEverything(autofillId, autofillValue, remoteViews, new DatasetFieldFilter(pattern));
            return this;
        }

        private void setLifeTheUniverseAndEverything(AutofillId autofillId, AutofillValue autofillValue, RemoteViews remoteViews, DatasetFieldFilter datasetFieldFilter) {
            Preconditions.checkNotNull(autofillId, "id cannot be null");
            if (this.mFieldIds != null) {
                int iIndexOf = this.mFieldIds.indexOf(autofillId);
                if (iIndexOf >= 0) {
                    this.mFieldValues.set(iIndexOf, autofillValue);
                    this.mFieldPresentations.set(iIndexOf, remoteViews);
                    this.mFieldFilters.set(iIndexOf, datasetFieldFilter);
                    return;
                }
            } else {
                this.mFieldIds = new ArrayList<>();
                this.mFieldValues = new ArrayList<>();
                this.mFieldPresentations = new ArrayList<>();
                this.mFieldFilters = new ArrayList<>();
            }
            this.mFieldIds.add(autofillId);
            this.mFieldValues.add(autofillValue);
            this.mFieldPresentations.add(remoteViews);
            this.mFieldFilters.add(datasetFieldFilter);
        }

        public Dataset build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            if (this.mFieldIds == null) {
                throw new IllegalStateException("at least one value must be set");
            }
            return new Dataset(this);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mPresentation, i);
        parcel.writeTypedList(this.mFieldIds, i);
        parcel.writeTypedList(this.mFieldValues, i);
        parcel.writeTypedList(this.mFieldPresentations, i);
        parcel.writeTypedList(this.mFieldFilters, i);
        parcel.writeParcelable(this.mAuthentication, i);
        parcel.writeString(this.mId);
    }

    public static final class DatasetFieldFilter implements Parcelable {
        public static final Parcelable.Creator<DatasetFieldFilter> CREATOR = new Parcelable.Creator<DatasetFieldFilter>() {
            @Override
            public DatasetFieldFilter createFromParcel(Parcel parcel) {
                return new DatasetFieldFilter((Pattern) parcel.readSerializable());
            }

            @Override
            public DatasetFieldFilter[] newArray(int i) {
                return new DatasetFieldFilter[i];
            }
        };
        public final Pattern pattern;

        private DatasetFieldFilter(Pattern pattern) {
            this.pattern = pattern;
        }

        public String toString() {
            if (!Helper.sDebug) {
                return super.toString();
            }
            if (this.pattern == null) {
                return "null";
            }
            return this.pattern.pattern().length() + "_chars";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeSerializable(this.pattern);
        }
    }
}
