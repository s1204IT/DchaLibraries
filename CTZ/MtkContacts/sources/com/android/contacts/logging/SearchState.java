package com.android.contacts.logging;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.base.MoreObjects;

public final class SearchState implements Parcelable {
    public static final Parcelable.Creator<SearchState> CREATOR = new Parcelable.Creator<SearchState>() {
        @Override
        public SearchState createFromParcel(Parcel parcel) {
            return new SearchState(parcel);
        }

        @Override
        public SearchState[] newArray(int i) {
            return new SearchState[i];
        }
    };
    public int numPartitions;
    public int numResults;
    public int queryLength;
    public int numResultsInSelectedPartition = -1;
    public int selectedPartition = -1;
    public int selectedIndexInPartition = -1;
    public int selectedIndex = -1;

    public SearchState() {
    }

    protected SearchState(Parcel parcel) {
        readFromParcel(parcel);
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("queryLength", this.queryLength).add("numPartitions", this.numPartitions).add("numResults", this.numResults).add("numResultsInSelectedPartition", this.numResultsInSelectedPartition).add("selectedPartition", this.selectedPartition).add("selectedIndexInPartition", this.selectedIndexInPartition).add("selectedIndex", this.selectedIndex).toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.queryLength);
        parcel.writeInt(this.numPartitions);
        parcel.writeInt(this.numResults);
        parcel.writeInt(this.numResultsInSelectedPartition);
        parcel.writeInt(this.selectedPartition);
        parcel.writeInt(this.selectedIndexInPartition);
        parcel.writeInt(this.selectedIndex);
    }

    private void readFromParcel(Parcel parcel) {
        this.queryLength = parcel.readInt();
        this.numPartitions = parcel.readInt();
        this.numResults = parcel.readInt();
        this.numResultsInSelectedPartition = parcel.readInt();
        this.selectedPartition = parcel.readInt();
        this.selectedIndexInPartition = parcel.readInt();
        this.selectedIndex = parcel.readInt();
    }
}
