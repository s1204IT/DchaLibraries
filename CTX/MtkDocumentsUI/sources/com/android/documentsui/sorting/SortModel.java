package com.android.documentsui.sorting;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import com.android.documentsui.R;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.sorting.SortDimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class SortModel implements Parcelable {
    static final boolean $assertionsDisabled = false;
    public static Parcelable.Creator<SortModel> CREATOR = new Parcelable.Creator<SortModel>() {
        @Override
        public SortModel createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            ArrayList arrayList = new ArrayList(i);
            for (int i2 = 0; i2 < i; i2++) {
                arrayList.add((SortDimension) parcel.readParcelable(getClass().getClassLoader()));
            }
            SortModel sortModel = new SortModel(arrayList);
            sortModel.mDefaultDimensionId = parcel.readInt();
            sortModel.mSortedDimension = sortModel.getDimensionById(parcel.readInt());
            return sortModel;
        }

        @Override
        public SortModel[] newArray(int i) {
            return new SortModel[i];
        }
    };
    private final SparseArray<SortDimension> mDimensions;
    private final transient List<UpdateListener> mListeners;
    private transient Consumer<SortDimension> mMetricRecorder;
    private SortDimension mSortedDimension;
    private int mDefaultDimensionId = 0;
    private boolean mIsUserSpecified = false;

    public interface UpdateListener {
        void onModelUpdate(SortModel sortModel, int i);
    }

    SortModel(Collection<SortDimension> collection) {
        this.mDimensions = new SparseArray<>(collection.size());
        for (SortDimension sortDimension : collection) {
            if (sortDimension.getId() == 0) {
                throw new IllegalArgumentException("SortDimension id can't be 0.");
            }
            if (this.mDimensions.get(sortDimension.getId()) != null) {
                throw new IllegalStateException("SortDimension id must be unique. Duplicate id: " + sortDimension.getId());
            }
            this.mDimensions.put(sortDimension.getId(), sortDimension);
        }
        this.mListeners = new ArrayList();
    }

    public int getSize() {
        return this.mDimensions.size();
    }

    public SortDimension getDimensionAt(int i) {
        return this.mDimensions.valueAt(i);
    }

    public SortDimension getDimensionById(int i) {
        return this.mDimensions.get(i);
    }

    public int getSortedDimensionId() {
        if (this.mSortedDimension != null) {
            return this.mSortedDimension.getId();
        }
        return 0;
    }

    public int getCurrentSortDirection() {
        if (this.mSortedDimension != null) {
            return this.mSortedDimension.getSortDirection();
        }
        return 0;
    }

    public void setDefaultDimension(int i) {
        boolean z = this.mDefaultDimensionId != i;
        this.mDefaultDimensionId = i;
        if (z) {
            sortOnDefault();
        }
    }

    void setMetricRecorder(Consumer<SortDimension> consumer) {
        this.mMetricRecorder = consumer;
    }

    public void sortByUser(int i, int i2) {
        SortDimension sortDimension = this.mDimensions.get(i);
        if (sortDimension == null) {
            throw new IllegalArgumentException("Unknown column id: " + i);
        }
        sortByDimension(sortDimension, i2);
        if (this.mMetricRecorder != null) {
            this.mMetricRecorder.accept(sortDimension);
        }
        this.mIsUserSpecified = true;
    }

    private void sortByDimension(SortDimension sortDimension, int i) {
        if (sortDimension == this.mSortedDimension && this.mSortedDimension.mSortDirection == i) {
            return;
        }
        if ((sortDimension.getSortCapability() & i) == 0) {
            throw new IllegalStateException("Dimension with id: " + sortDimension.getId() + " can't be sorted in direction:" + i);
        }
        switch (i) {
            case 1:
            case 2:
                sortDimension.mSortDirection = i;
                if (this.mSortedDimension != null && this.mSortedDimension != sortDimension) {
                    this.mSortedDimension.mSortDirection = 0;
                }
                this.mSortedDimension = sortDimension;
                notifyListeners(2);
                return;
            default:
                throw new IllegalArgumentException("Unknown sort direction: " + i);
        }
    }

    public void setDimensionVisibility(int i, int i2) {
        this.mDimensions.get(i).mVisibility = i2;
        notifyListeners(1);
    }

    public Cursor sortCursor(Cursor cursor, Lookup<String, String> lookup) {
        if (this.mSortedDimension != null) {
            return new SortingCursorWrapper(cursor, this.mSortedDimension, lookup);
        }
        return cursor;
    }

    public void addQuerySortArgs(Bundle bundle) {
        int sortedDimensionId = getSortedDimensionId();
        if (sortedDimensionId == 0) {
            return;
        }
        if (sortedDimensionId == 16908310) {
            bundle.putStringArray("android:query-arg-sort-columns", new String[]{"_display_name"});
        } else if (sortedDimensionId == R.id.date) {
            bundle.putStringArray("android:query-arg-sort-columns", new String[]{"last_modified"});
        } else {
            if (sortedDimensionId == R.id.file_type) {
                return;
            }
            if (sortedDimensionId == R.id.size) {
                bundle.putStringArray("android:query-arg-sort-columns", new String[]{"_size"});
            } else {
                throw new IllegalStateException("Unexpected sort dimension id: " + sortedDimensionId);
            }
        }
        SortDimension dimensionById = getDimensionById(sortedDimensionId);
        switch (dimensionById.getSortDirection()) {
            case 1:
                bundle.putInt("android:query-arg-sort-direction", 0);
                return;
            case 2:
                bundle.putInt("android:query-arg-sort-direction", 1);
                return;
            default:
                throw new IllegalStateException("Unexpected sort direction: " + dimensionById.getSortDirection());
        }
    }

    public String getDocumentSortQuery() {
        String str;
        String str2;
        int sortedDimensionId = getSortedDimensionId();
        if (sortedDimensionId == 0) {
            return null;
        }
        if (sortedDimensionId == 16908310) {
            str = "_display_name";
        } else if (sortedDimensionId == R.id.date) {
            str = "last_modified";
        } else {
            if (sortedDimensionId == R.id.file_type) {
                return null;
            }
            if (sortedDimensionId == R.id.size) {
                str = "_size";
            } else {
                throw new IllegalStateException("Unexpected sort dimension id: " + sortedDimensionId);
            }
        }
        SortDimension dimensionById = getDimensionById(sortedDimensionId);
        switch (dimensionById.getSortDirection()) {
            case 1:
                str2 = " ASC";
                break;
            case 2:
                str2 = " DESC";
                break;
            default:
                throw new IllegalStateException("Unexpected sort direction: " + dimensionById.getSortDirection());
        }
        return str + str2;
    }

    private void notifyListeners(int i) {
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onModelUpdate(this, i);
        }
    }

    public void addListener(UpdateListener updateListener) {
        this.mListeners.add(updateListener);
    }

    public void removeListener(UpdateListener updateListener) {
        this.mListeners.remove(updateListener);
    }

    private void sortOnDefault() {
        if (!this.mIsUserSpecified) {
            SortDimension sortDimension = this.mDimensions.get(this.mDefaultDimensionId);
            if (sortDimension == null) {
                if (SharedMinimal.DEBUG) {
                    Log.d("SortModel", "No default sort dimension.");
                    return;
                }
                return;
            }
            sortByDimension(sortDimension, sortDimension.getDefaultSortDirection());
        }
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SortModel)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SortModel sortModel = (SortModel) obj;
        if (this.mDimensions.size() != sortModel.mDimensions.size()) {
            return false;
        }
        for (int i = 0; i < this.mDimensions.size(); i++) {
            SortDimension sortDimensionValueAt = this.mDimensions.valueAt(i);
            if (!sortDimensionValueAt.equals(sortModel.getDimensionById(sortDimensionValueAt.getId()))) {
                return false;
            }
        }
        if (this.mDefaultDimensionId != sortModel.mDefaultDimensionId) {
            return false;
        }
        if (this.mSortedDimension != sortModel.mSortedDimension && !this.mSortedDimension.equals(sortModel.mSortedDimension)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "SortModel{dimensions=" + this.mDimensions + ", defaultDimensionId=" + this.mDefaultDimensionId + ", sortedDimension=" + this.mSortedDimension + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mDimensions.size());
        for (int i2 = 0; i2 < this.mDimensions.size(); i2++) {
            parcel.writeParcelable(this.mDimensions.valueAt(i2), i);
        }
        parcel.writeInt(this.mDefaultDimensionId);
        parcel.writeInt(getSortedDimensionId());
    }

    public static SortModel createModel() {
        ArrayList arrayList = new ArrayList(4);
        SortDimension.Builder builder = new SortDimension.Builder();
        arrayList.add(builder.withId(android.R.id.title).withLabelId(R.string.sort_dimension_name).withDataType(0).withSortCapability(3).withDefaultSortDirection(1).withVisibility(0).build());
        arrayList.add(builder.withId(android.R.id.summary).withLabelId(R.string.sort_dimension_summary).withDataType(0).withSortCapability(0).withVisibility(4).build());
        arrayList.add(builder.withId(R.id.size).withLabelId(R.string.sort_dimension_size).withDataType(1).withSortCapability(3).withDefaultSortDirection(1).withVisibility(0).build());
        arrayList.add(builder.withId(R.id.file_type).withLabelId(R.string.sort_dimension_file_type).withDataType(0).withSortCapability(3).withDefaultSortDirection(1).withVisibility(0).build());
        arrayList.add(builder.withId(R.id.date).withLabelId(R.string.sort_dimension_date).withDataType(1).withSortCapability(3).withDefaultSortDirection(2).withVisibility(0).build());
        return new SortModel(arrayList);
    }
}
