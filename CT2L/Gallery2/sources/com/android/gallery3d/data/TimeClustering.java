package com.android.gallery3d.data;

import android.content.Context;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.GalleryUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TimeClustering extends Clustering {
    private static int CLUSTER_SPLIT_MULTIPLIER = 3;
    private static final Comparator<SmallItem> sDateComparator = new DateComparator();
    private Context mContext;
    private String[] mNames;
    private long mClusterSplitTime = 3630000;
    private long mLargeClusterSplitTime = this.mClusterSplitTime / 2;
    private int mMinClusterSize = 11;
    private int mMaxClusterSize = 35;
    private ArrayList<Cluster> mClusters = new ArrayList<>();
    private Cluster mCurrCluster = new Cluster();

    private static class DateComparator implements Comparator<SmallItem> {
        private DateComparator() {
        }

        @Override
        public int compare(SmallItem item1, SmallItem item2) {
            return -Utils.compare(item1.dateInMs, item2.dateInMs);
        }
    }

    public TimeClustering(Context context) {
        this.mContext = context;
    }

    @Override
    public void run(MediaSet baseSet) {
        final int total = baseSet.getTotalMediaItemCount();
        final SmallItem[] buf = new SmallItem[total];
        final double[] latLng = new double[2];
        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (index >= 0 && index < total) {
                    SmallItem s = new SmallItem();
                    s.path = item.getPath();
                    s.dateInMs = item.getDateInMs();
                    item.getLatLong(latLng);
                    s.lat = latLng[0];
                    s.lng = latLng[1];
                    buf[index] = s;
                }
            }
        });
        ArrayList<SmallItem> items = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            if (buf[i] != null) {
                items.add(buf[i]);
            }
        }
        Collections.sort(items, sDateComparator);
        int n = items.size();
        long minTime = 0;
        long maxTime = 0;
        for (int i2 = 0; i2 < n; i2++) {
            long t = items.get(i2).dateInMs;
            if (t != 0) {
                if (minTime == 0) {
                    maxTime = t;
                    minTime = t;
                } else {
                    minTime = Math.min(minTime, t);
                    maxTime = Math.max(maxTime, t);
                }
            }
        }
        setTimeRange(maxTime - minTime, n);
        for (int i3 = 0; i3 < n; i3++) {
            compute(items.get(i3));
        }
        compute(null);
        int m = this.mClusters.size();
        this.mNames = new String[m];
        for (int i4 = 0; i4 < m; i4++) {
            this.mNames[i4] = this.mClusters.get(i4).generateCaption(this.mContext);
        }
    }

    @Override
    public int getNumberOfClusters() {
        return this.mClusters.size();
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        ArrayList<SmallItem> items = this.mClusters.get(index).getItems();
        ArrayList<Path> result = new ArrayList<>(items.size());
        int n = items.size();
        for (int i = 0; i < n; i++) {
            result.add(items.get(i).path);
        }
        return result;
    }

    @Override
    public String getClusterName(int index) {
        return this.mNames[index];
    }

    private void setTimeRange(long timeRange, int numItems) {
        if (numItems != 0) {
            int meanItemsPerCluster = numItems / 9;
            this.mMinClusterSize = meanItemsPerCluster / 2;
            this.mMaxClusterSize = meanItemsPerCluster * 2;
            this.mClusterSplitTime = (timeRange / ((long) numItems)) * ((long) CLUSTER_SPLIT_MULTIPLIER);
        }
        this.mClusterSplitTime = Utils.clamp(this.mClusterSplitTime, 60000L, 7200000L);
        this.mLargeClusterSplitTime = this.mClusterSplitTime / 2;
        this.mMinClusterSize = Utils.clamp(this.mMinClusterSize, 8, 15);
        this.mMaxClusterSize = Utils.clamp(this.mMaxClusterSize, 20, 50);
    }

    private void compute(SmallItem currentItem) {
        if (currentItem != null) {
            int numClusters = this.mClusters.size();
            int numCurrClusterItems = this.mCurrCluster.size();
            boolean geographicallySeparateItem = false;
            boolean itemAddedToCurrentCluster = false;
            if (numCurrClusterItems == 0) {
                this.mCurrCluster.addItem(currentItem);
                return;
            }
            SmallItem prevItem = this.mCurrCluster.getLastItem();
            if (isGeographicallySeparated(prevItem, currentItem)) {
                this.mClusters.add(this.mCurrCluster);
                geographicallySeparateItem = true;
            } else if (numCurrClusterItems > this.mMaxClusterSize) {
                splitAndAddCurrentCluster();
            } else if (timeDistance(prevItem, currentItem) < this.mClusterSplitTime) {
                this.mCurrCluster.addItem(currentItem);
                itemAddedToCurrentCluster = true;
            } else if (numClusters > 0 && numCurrClusterItems < this.mMinClusterSize && !this.mCurrCluster.mGeographicallySeparatedFromPrevCluster) {
                mergeAndAddCurrentCluster();
            } else {
                this.mClusters.add(this.mCurrCluster);
            }
            if (!itemAddedToCurrentCluster) {
                this.mCurrCluster = new Cluster();
                if (geographicallySeparateItem) {
                    this.mCurrCluster.mGeographicallySeparatedFromPrevCluster = true;
                }
                this.mCurrCluster.addItem(currentItem);
                return;
            }
            return;
        }
        if (this.mCurrCluster.size() > 0) {
            int numClusters2 = this.mClusters.size();
            int numCurrClusterItems2 = this.mCurrCluster.size();
            if (numCurrClusterItems2 > this.mMaxClusterSize) {
                splitAndAddCurrentCluster();
            } else if (numClusters2 > 0 && numCurrClusterItems2 < this.mMinClusterSize && !this.mCurrCluster.mGeographicallySeparatedFromPrevCluster) {
                mergeAndAddCurrentCluster();
            } else {
                this.mClusters.add(this.mCurrCluster);
            }
            this.mCurrCluster = new Cluster();
        }
    }

    private void splitAndAddCurrentCluster() {
        ArrayList<SmallItem> currClusterItems = this.mCurrCluster.getItems();
        int numCurrClusterItems = this.mCurrCluster.size();
        int secondPartitionStartIndex = getPartitionIndexForCurrentCluster();
        if (secondPartitionStartIndex != -1) {
            Cluster partitionedCluster = new Cluster();
            for (int j = 0; j < secondPartitionStartIndex; j++) {
                partitionedCluster.addItem(currClusterItems.get(j));
            }
            this.mClusters.add(partitionedCluster);
            Cluster partitionedCluster2 = new Cluster();
            for (int j2 = secondPartitionStartIndex; j2 < numCurrClusterItems; j2++) {
                partitionedCluster2.addItem(currClusterItems.get(j2));
            }
            this.mClusters.add(partitionedCluster2);
            return;
        }
        this.mClusters.add(this.mCurrCluster);
    }

    private int getPartitionIndexForCurrentCluster() {
        int partitionIndex = -1;
        float largestChange = 2.0f;
        ArrayList<SmallItem> currClusterItems = this.mCurrCluster.getItems();
        int numCurrClusterItems = this.mCurrCluster.size();
        int minClusterSize = this.mMinClusterSize;
        if (numCurrClusterItems > minClusterSize + 1) {
            for (int i = minClusterSize; i < numCurrClusterItems - minClusterSize; i++) {
                SmallItem prevItem = currClusterItems.get(i - 1);
                SmallItem currItem = currClusterItems.get(i);
                SmallItem nextItem = currClusterItems.get(i + 1);
                long timeNext = nextItem.dateInMs;
                long timeCurr = currItem.dateInMs;
                long timePrev = prevItem.dateInMs;
                if (timeNext != 0 && timeCurr != 0 && timePrev != 0) {
                    long diff1 = Math.abs(timeNext - timeCurr);
                    long diff2 = Math.abs(timeCurr - timePrev);
                    float change = Math.max(diff1 / (diff2 + 0.01f), diff2 / (diff1 + 0.01f));
                    if (change > largestChange) {
                        if (timeDistance(currItem, prevItem) > this.mLargeClusterSplitTime) {
                            partitionIndex = i;
                            largestChange = change;
                        } else if (timeDistance(nextItem, currItem) > this.mLargeClusterSplitTime) {
                            partitionIndex = i + 1;
                            largestChange = change;
                        }
                    }
                }
            }
        }
        return partitionIndex;
    }

    private void mergeAndAddCurrentCluster() {
        int numClusters = this.mClusters.size();
        Cluster prevCluster = this.mClusters.get(numClusters - 1);
        ArrayList<SmallItem> currClusterItems = this.mCurrCluster.getItems();
        int numCurrClusterItems = this.mCurrCluster.size();
        if (prevCluster.size() < this.mMinClusterSize) {
            for (int i = 0; i < numCurrClusterItems; i++) {
                prevCluster.addItem(currClusterItems.get(i));
            }
            this.mClusters.set(numClusters - 1, prevCluster);
            return;
        }
        this.mClusters.add(this.mCurrCluster);
    }

    private static boolean isGeographicallySeparated(SmallItem itemA, SmallItem itemB) {
        if (!GalleryUtils.isValidLocation(itemA.lat, itemA.lng) || !GalleryUtils.isValidLocation(itemB.lat, itemB.lng)) {
            return false;
        }
        double distance = GalleryUtils.fastDistanceMeters(Math.toRadians(itemA.lat), Math.toRadians(itemA.lng), Math.toRadians(itemB.lat), Math.toRadians(itemB.lng));
        return GalleryUtils.toMile(distance) > 20.0d;
    }

    private static long timeDistance(SmallItem a, SmallItem b) {
        return Math.abs(a.dateInMs - b.dateInMs);
    }
}
