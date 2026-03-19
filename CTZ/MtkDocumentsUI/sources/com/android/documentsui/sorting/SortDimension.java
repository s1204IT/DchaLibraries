package com.android.documentsui.sorting;

import android.os.Parcel;
import android.os.Parcelable;

public class SortDimension implements Parcelable {
    public static Parcelable.Creator<SortDimension> CREATOR = new Parcelable.Creator<SortDimension>() {
        @Override
        public SortDimension createFromParcel(Parcel parcel) {
            SortDimension sortDimension = new SortDimension(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
            sortDimension.mSortDirection = parcel.readInt();
            sortDimension.mVisibility = parcel.readInt();
            return sortDimension;
        }

        @Override
        public SortDimension[] newArray(int i) {
            return new SortDimension[i];
        }
    };
    private final int mDataType;
    private final int mDefaultSortDirection;
    private final int mId;
    private final int mLabelId;
    private final int mSortCapability;
    int mSortDirection;
    int mVisibility;

    private SortDimension(int i, int i2, int i3, int i4, int i5) {
        this.mSortDirection = 0;
        this.mId = i;
        this.mLabelId = i2;
        this.mDataType = i3;
        this.mSortCapability = i4;
        this.mDefaultSortDirection = i5;
    }

    public int getId() {
        return this.mId;
    }

    public int getLabelId() {
        return this.mLabelId;
    }

    public int getDataType() {
        return this.mDataType;
    }

    public int getSortCapability() {
        return this.mSortCapability;
    }

    public int getDefaultSortDirection() {
        return this.mDefaultSortDirection;
    }

    public int getNextDirection() {
        int i = this.mDefaultSortDirection == 1 ? 2 : 1;
        if (this.mSortDirection != this.mDefaultSortDirection) {
            return this.mDefaultSortDirection;
        }
        return i;
    }

    public int getSortDirection() {
        return this.mSortDirection;
    }

    public int getVisibility() {
        return this.mVisibility;
    }

    public int hashCode() {
        return this.mId;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SortDimension)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        SortDimension sortDimension = (SortDimension) obj;
        if (this.mId != sortDimension.mId || this.mLabelId != sortDimension.mLabelId || this.mDataType != sortDimension.mDataType || this.mSortCapability != sortDimension.mSortCapability || this.mDefaultSortDirection != sortDimension.mDefaultSortDirection || this.mSortDirection != sortDimension.mSortDirection || this.mVisibility != sortDimension.mVisibility) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "SortDimension{id=" + this.mId + ", labelId=" + this.mLabelId + ", dataType=" + this.mDataType + ", sortCapability=" + this.mSortCapability + ", defaultSortDirection=" + this.mDefaultSortDirection + ", sortDirection=" + this.mSortDirection + ", visibility=" + this.mVisibility + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mId);
        parcel.writeInt(this.mLabelId);
        parcel.writeInt(this.mDataType);
        parcel.writeInt(this.mSortCapability);
        parcel.writeInt(this.mDefaultSortDirection);
        parcel.writeInt(this.mSortDirection);
        parcel.writeInt(this.mVisibility);
    }

    static class Builder {
        private int mId;
        private int mLabelId;
        private int mDataType = 0;
        private int mSortCapability = 3;
        private int mDefaultSortDirection = 1;
        private int mVisibility = 0;

        Builder() {
        }

        Builder withId(int i) {
            this.mId = i;
            return this;
        }

        Builder withLabelId(int i) {
            this.mLabelId = i;
            return this;
        }

        Builder withDataType(int i) {
            this.mDataType = i;
            return this;
        }

        Builder withSortCapability(int i) {
            this.mSortCapability = i;
            return this;
        }

        Builder withVisibility(int i) {
            this.mVisibility = i;
            return this;
        }

        Builder withDefaultSortDirection(int i) {
            this.mDefaultSortDirection = i;
            return this;
        }

        SortDimension build() {
            if (this.mLabelId == 0) {
                throw new IllegalStateException("Must set labelId.");
            }
            SortDimension sortDimension = new SortDimension(this.mId, this.mLabelId, this.mDataType, this.mSortCapability, this.mDefaultSortDirection);
            sortDimension.mVisibility = this.mVisibility;
            return sortDimension;
        }
    }
}
