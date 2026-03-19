package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;

@TargetApi(12)
class DateBucket implements Comparable<DateBucket> {
    final SimpleDate date;
    final int itemsStartIndex;
    final int numItems;
    final int unifiedEndIndex;
    final int unifiedStartIndex;

    public DateBucket(SimpleDate simpleDate, int i, int i2, int i3, int i4) {
        this.date = simpleDate;
        this.unifiedStartIndex = i;
        this.unifiedEndIndex = i2;
        this.itemsStartIndex = i3;
        this.numItems = i4;
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
        if (obj == 0 || !(obj instanceof DateBucket)) {
            return false;
        }
        if (this.date == null) {
            if (obj.date != null) {
                return false;
            }
        } else if (!this.date.equals(obj.date)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(DateBucket dateBucket) {
        return this.date.compareTo(dateBucket.date);
    }
}
