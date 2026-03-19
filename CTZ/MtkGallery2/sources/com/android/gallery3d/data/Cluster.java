package com.android.gallery3d.data;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import java.util.ArrayList;

class Cluster {
    public boolean mGeographicallySeparatedFromPrevCluster = false;
    private ArrayList<SmallItem> mItems = new ArrayList<>();

    public void addItem(SmallItem smallItem) {
        this.mItems.add(smallItem);
    }

    public int size() {
        return this.mItems.size();
    }

    public SmallItem getLastItem() {
        int size = this.mItems.size();
        if (size == 0) {
            return null;
        }
        return this.mItems.get(size - 1);
    }

    public ArrayList<SmallItem> getItems() {
        return this.mItems;
    }

    public String generateCaption(Context context) {
        int size = this.mItems.size();
        long j = 0;
        long jMax = 0;
        for (int i = 0; i < size; i++) {
            long j2 = this.mItems.get(i).dateInMs;
            if (j2 != 0) {
                if (j == 0) {
                    j = j2;
                    jMax = j;
                } else {
                    long jMin = Math.min(j, j2);
                    jMax = Math.max(jMax, j2);
                    j = jMin;
                }
            }
        }
        if (j == 0) {
            return "";
        }
        String string = DateFormat.format("MMddyy", j).toString();
        String string2 = DateFormat.format("MMddyy", jMax).toString();
        if (string.substring(4).equals(string2.substring(4))) {
            String dateRange = DateUtils.formatDateRange(context, j, jMax, 524288);
            if (string.equals(string2) && !DateUtils.formatDateTime(context, j, 65552).equals(DateUtils.formatDateTime(context, j, 65556))) {
                long j3 = (j + jMax) / 2;
                return DateUtils.formatDateRange(context, j3, j3, 65553);
            }
            return dateRange;
        }
        return DateUtils.formatDateRange(context, j, jMax, 65584);
    }
}
