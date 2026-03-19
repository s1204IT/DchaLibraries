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
        public int compare(SmallItem smallItem, SmallItem smallItem2) {
            return -Utils.compare(smallItem.dateInMs, smallItem2.dateInMs);
        }
    }

    public TimeClustering(Context context) {
        this.mContext = context;
        this.mStopEnumerate = false;
    }

    @Override
    public void run(MediaSet mediaSet) {
        final int totalMediaItemCount = mediaSet.getTotalMediaItemCount();
        final SmallItem[] smallItemArr = new SmallItem[totalMediaItemCount];
        final double[] dArr = new double[2];
        mediaSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                if (i < 0 || i >= totalMediaItemCount) {
                    return;
                }
                SmallItem smallItem = new SmallItem();
                smallItem.path = mediaItem.getPath();
                smallItem.dateInMs = mediaItem.getDateInMs();
                mediaItem.getLatLong(dArr);
                smallItem.lat = dArr[0];
                smallItem.lng = dArr[1];
                smallItemArr[i] = smallItem;
            }

            @Override
            public boolean stopConsume() {
                return TimeClustering.this.mStopEnumerate;
            }
        });
        ArrayList arrayList = new ArrayList(totalMediaItemCount);
        for (int i = 0; i < totalMediaItemCount; i++) {
            if (smallItemArr[i] != null) {
                arrayList.add(smallItemArr[i]);
            }
        }
        Collections.sort(arrayList, sDateComparator);
        int size = arrayList.size();
        long jMax = 0;
        long jMin = 0;
        for (int i2 = 0; i2 < size; i2++) {
            long j = ((SmallItem) arrayList.get(i2)).dateInMs;
            if (j != 0) {
                if (jMin == 0) {
                    jMax = j;
                    jMin = jMax;
                } else {
                    jMin = Math.min(jMin, j);
                    jMax = Math.max(jMax, j);
                }
            }
        }
        setTimeRange(jMax - jMin, size);
        for (int i3 = 0; i3 < size; i3++) {
            compute((SmallItem) arrayList.get(i3));
        }
        compute(null);
        int size2 = this.mClusters.size();
        this.mNames = new String[size2];
        for (int i4 = 0; i4 < size2; i4++) {
            this.mNames[i4] = this.mClusters.get(i4).generateCaption(this.mContext);
        }
    }

    @Override
    public int getNumberOfClusters() {
        if (this.mClusters != null) {
            return this.mClusters.size();
        }
        return 0;
    }

    @Override
    public ArrayList<Path> getCluster(int i) {
        ArrayList<SmallItem> items = this.mClusters.get(i).getItems();
        ArrayList<Path> arrayList = new ArrayList<>(items.size());
        int size = items.size();
        for (int i2 = 0; i2 < size; i2++) {
            arrayList.add(items.get(i2).path);
        }
        return arrayList;
    }

    @Override
    public String getClusterName(int i) {
        return this.mNames[i];
    }

    private void setTimeRange(long j, int i) {
        if (i != 0) {
            int i2 = i / 9;
            this.mMinClusterSize = i2 / 2;
            this.mMaxClusterSize = i2 * 2;
            this.mClusterSplitTime = (j / ((long) i)) * ((long) CLUSTER_SPLIT_MULTIPLIER);
        }
        this.mClusterSplitTime = Utils.clamp(this.mClusterSplitTime, 60000L, 7200000L);
        this.mLargeClusterSplitTime = this.mClusterSplitTime / 2;
        this.mMinClusterSize = Utils.clamp(this.mMinClusterSize, 8, 15);
        this.mMaxClusterSize = Utils.clamp(this.mMaxClusterSize, 20, 50);
    }

    private void compute(SmallItem smallItem) {
        boolean z;
        if (smallItem != null) {
            int size = this.mClusters.size();
            int size2 = this.mCurrCluster.size();
            if (size2 == 0) {
                this.mCurrCluster.addItem(smallItem);
                return;
            }
            SmallItem lastItem = this.mCurrCluster.getLastItem();
            boolean z2 = false;
            if (isGeographicallySeparated(lastItem, smallItem)) {
                this.mClusters.add(this.mCurrCluster);
                z = true;
            } else {
                if (size2 > this.mMaxClusterSize) {
                    splitAndAddCurrentCluster();
                } else if (timeDistance(lastItem, smallItem) < this.mClusterSplitTime) {
                    this.mCurrCluster.addItem(smallItem);
                    z = false;
                    z2 = true;
                } else if (size > 0 && size2 < this.mMinClusterSize && !this.mCurrCluster.mGeographicallySeparatedFromPrevCluster) {
                    mergeAndAddCurrentCluster();
                } else {
                    this.mClusters.add(this.mCurrCluster);
                }
                z = false;
            }
            if (!z2) {
                this.mCurrCluster = new Cluster();
                if (z) {
                    this.mCurrCluster.mGeographicallySeparatedFromPrevCluster = true;
                }
                this.mCurrCluster.addItem(smallItem);
                return;
            }
            return;
        }
        if (this.mCurrCluster.size() > 0) {
            int size3 = this.mClusters.size();
            int size4 = this.mCurrCluster.size();
            if (size4 > this.mMaxClusterSize) {
                splitAndAddCurrentCluster();
            } else if (size3 > 0 && size4 < this.mMinClusterSize && !this.mCurrCluster.mGeographicallySeparatedFromPrevCluster) {
                mergeAndAddCurrentCluster();
            } else {
                this.mClusters.add(this.mCurrCluster);
            }
            this.mCurrCluster = new Cluster();
        }
    }

    private void splitAndAddCurrentCluster() {
        ArrayList<SmallItem> items = this.mCurrCluster.getItems();
        int size = this.mCurrCluster.size();
        int partitionIndexForCurrentCluster = getPartitionIndexForCurrentCluster();
        if (partitionIndexForCurrentCluster != -1) {
            Cluster cluster = new Cluster();
            for (int i = 0; i < partitionIndexForCurrentCluster; i++) {
                cluster.addItem(items.get(i));
            }
            this.mClusters.add(cluster);
            Cluster cluster2 = new Cluster();
            while (partitionIndexForCurrentCluster < size) {
                cluster2.addItem(items.get(partitionIndexForCurrentCluster));
                partitionIndexForCurrentCluster++;
            }
            this.mClusters.add(cluster2);
            return;
        }
        this.mClusters.add(this.mCurrCluster);
    }

    private int getPartitionIndexForCurrentCluster() {
        ArrayList<SmallItem> items = this.mCurrCluster.getItems();
        int size = this.mCurrCluster.size();
        int i = this.mMinClusterSize;
        int i2 = -1;
        if (size > i + 1) {
            float f = 2.0f;
            int i3 = i;
            while (i3 < size - i) {
                SmallItem smallItem = items.get(i3 - 1);
                SmallItem smallItem2 = items.get(i3);
                int i4 = i3 + 1;
                SmallItem smallItem3 = items.get(i4);
                long j = smallItem3.dateInMs;
                long j2 = smallItem2.dateInMs;
                ArrayList<SmallItem> arrayList = items;
                int i5 = size;
                long j3 = smallItem.dateInMs;
                if (j != 0 && j2 != 0 && j3 != 0) {
                    float fAbs = Math.abs(j - j2);
                    float fAbs2 = Math.abs(j2 - j3);
                    float fMax = Math.max(fAbs / (fAbs2 + 0.01f), fAbs2 / (fAbs + 0.01f));
                    if (fMax > f) {
                        if (timeDistance(smallItem2, smallItem) > this.mLargeClusterSplitTime) {
                            f = fMax;
                            i2 = i3;
                        } else if (timeDistance(smallItem3, smallItem2) > this.mLargeClusterSplitTime) {
                            f = fMax;
                            i2 = i4;
                        }
                    }
                }
                i3 = i4;
                items = arrayList;
                size = i5;
            }
        }
        return i2;
    }

    private void mergeAndAddCurrentCluster() {
        int size = this.mClusters.size() - 1;
        Cluster cluster = this.mClusters.get(size);
        ArrayList<SmallItem> items = this.mCurrCluster.getItems();
        int size2 = this.mCurrCluster.size();
        if (cluster.size() < this.mMinClusterSize) {
            for (int i = 0; i < size2; i++) {
                cluster.addItem(items.get(i));
            }
            this.mClusters.set(size, cluster);
            return;
        }
        this.mClusters.add(this.mCurrCluster);
    }

    private static boolean isGeographicallySeparated(SmallItem smallItem, SmallItem smallItem2) {
        return GalleryUtils.isValidLocation(smallItem.lat, smallItem.lng) && GalleryUtils.isValidLocation(smallItem2.lat, smallItem2.lng) && GalleryUtils.toMile(GalleryUtils.fastDistanceMeters(Math.toRadians(smallItem.lat), Math.toRadians(smallItem.lng), Math.toRadians(smallItem2.lat), Math.toRadians(smallItem2.lng))) > 20.0d;
    }

    private static long timeDistance(SmallItem smallItem, SmallItem smallItem2) {
        return Math.abs(smallItem.dateInMs - smallItem2.dateInMs);
    }

    @Override
    public boolean reGenerateName() {
        if (this.mClusters == null) {
            return false;
        }
        Log.d("Gallery2/TimeClustering", "<reGenerateName>refreshClusterName");
        synchronized (this.mClusters) {
            int size = this.mClusters.size();
            for (int i = 0; i < size; i++) {
                this.mNames[i] = this.mClusters.get(i).generateCaption(this.mContext);
            }
        }
        return true;
    }
}
