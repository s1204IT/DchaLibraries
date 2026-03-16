package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;

@TargetApi(12)
class DateBucket implements Comparable<DateBucket> {
    final SimpleDate date;
    final int itemsStartIndex;
    final int numItems;
    final int unifiedEndIndex;
    final int unifiedStartIndex;

    public DateBucket(SimpleDate date, int unifiedStartIndex, int unifiedEndIndex, int itemsStartIndex, int numItems) {
        this.date = date;
        this.unifiedStartIndex = unifiedStartIndex;
        this.unifiedEndIndex = unifiedEndIndex;
        this.itemsStartIndex = itemsStartIndex;
        this.numItems = numItems;
    }

    public String toString() {
        return this.date.toString();
    }

    public int hashCode() {
        return this.date.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof DateBucket)) {
            DateBucket other = (DateBucket) obj;
            return this.date == null ? other.date == null : this.date.equals(other.date);
        }
        return false;
    }

    @Override
    public int compareTo(DateBucket another) {
        return this.date.compareTo(another.date);
    }
}
