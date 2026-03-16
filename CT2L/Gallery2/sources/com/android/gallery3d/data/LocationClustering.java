package com.android.gallery3d.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.FloatMath;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ReverseGeocoder;
import java.util.ArrayList;

class LocationClustering extends Clustering {
    private ArrayList<ArrayList<SmallItem>> mClusters;
    private Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ArrayList<String> mNames;
    private String mNoLocationString;

    private static class Point {
        public double latRad;
        public double lngRad;

        public Point(double lat, double lng) {
            this.latRad = Math.toRadians(lat);
            this.lngRad = Math.toRadians(lng);
        }

        public Point() {
        }
    }

    private static class SmallItem {
        double lat;
        double lng;
        Path path;

        private SmallItem() {
        }
    }

    public LocationClustering(Context context) {
        this.mContext = context;
        this.mNoLocationString = this.mContext.getResources().getString(R.string.no_location);
    }

    @Override
    public void run(MediaSet baseSet) {
        final int total = baseSet.getTotalMediaItemCount();
        final SmallItem[] buf = new SmallItem[total];
        final double[] latLong = new double[2];
        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (index >= 0 && index < total) {
                    SmallItem s = new SmallItem();
                    s.path = item.getPath();
                    item.getLatLong(latLong);
                    s.lat = latLong[0];
                    s.lng = latLong[1];
                    buf[index] = s;
                }
            }
        });
        ArrayList<SmallItem> withLatLong = new ArrayList<>();
        ArrayList<SmallItem> withoutLatLong = new ArrayList<>();
        ArrayList<Point> points = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            SmallItem s = buf[i];
            if (s != null) {
                if (GalleryUtils.isValidLocation(s.lat, s.lng)) {
                    withLatLong.add(s);
                    points.add(new Point(s.lat, s.lng));
                } else {
                    withoutLatLong.add(s);
                }
            }
        }
        ArrayList<ArrayList<SmallItem>> clusters = new ArrayList<>();
        int m = withLatLong.size();
        if (m > 0) {
            Point[] pointsArray = new Point[m];
            int[] bestK = new int[1];
            int[] index = kMeans((Point[]) points.toArray(pointsArray), bestK);
            for (int i2 = 0; i2 < bestK[0]; i2++) {
                clusters.add(new ArrayList<>());
            }
            for (int i3 = 0; i3 < m; i3++) {
                clusters.get(index[i3]).add(withLatLong.get(i3));
            }
        }
        ReverseGeocoder geocoder = new ReverseGeocoder(this.mContext);
        this.mNames = new ArrayList<>();
        boolean hasUnresolvedAddress = false;
        this.mClusters = new ArrayList<>();
        for (ArrayList<SmallItem> cluster : clusters) {
            String name = generateName(cluster, geocoder);
            if (name != null) {
                this.mNames.add(name);
                this.mClusters.add(cluster);
            } else {
                withoutLatLong.addAll(cluster);
                hasUnresolvedAddress = true;
            }
        }
        if (withoutLatLong.size() > 0) {
            this.mNames.add(this.mNoLocationString);
            this.mClusters.add(withoutLatLong);
        }
        if (hasUnresolvedAddress) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LocationClustering.this.mContext, R.string.no_connectivity, 1).show();
                }
            });
        }
    }

    private static String generateName(ArrayList<SmallItem> items, ReverseGeocoder geocoder) {
        ReverseGeocoder.SetLatLong set = new ReverseGeocoder.SetLatLong();
        int n = items.size();
        for (int i = 0; i < n; i++) {
            SmallItem item = items.get(i);
            double itemLatitude = item.lat;
            double itemLongitude = item.lng;
            if (set.mMinLatLatitude > itemLatitude) {
                set.mMinLatLatitude = itemLatitude;
                set.mMinLatLongitude = itemLongitude;
            }
            if (set.mMaxLatLatitude < itemLatitude) {
                set.mMaxLatLatitude = itemLatitude;
                set.mMaxLatLongitude = itemLongitude;
            }
            if (set.mMinLonLongitude > itemLongitude) {
                set.mMinLonLatitude = itemLatitude;
                set.mMinLonLongitude = itemLongitude;
            }
            if (set.mMaxLonLongitude < itemLongitude) {
                set.mMaxLonLatitude = itemLatitude;
                set.mMaxLonLongitude = itemLongitude;
            }
        }
        return geocoder.computeAddress(set);
    }

    @Override
    public int getNumberOfClusters() {
        return this.mClusters.size();
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        ArrayList<SmallItem> items = this.mClusters.get(index);
        ArrayList<Path> result = new ArrayList<>(items.size());
        int n = items.size();
        for (int i = 0; i < n; i++) {
            result.add(items.get(i).path);
        }
        return result;
    }

    @Override
    public String getClusterName(int index) {
        return this.mNames.get(index);
    }

    private static int[] kMeans(Point[] points, int[] bestK) {
        int realK;
        int n = points.length;
        int minK = Math.min(n, 1);
        int maxK = Math.min(n, 20);
        Point[] center = new Point[maxK];
        Point[] groupSum = new Point[maxK];
        int[] groupCount = new int[maxK];
        int[] grouping = new int[n];
        for (int i = 0; i < maxK; i++) {
            center[i] = new Point();
            groupSum[i] = new Point();
        }
        float bestScore = Float.MAX_VALUE;
        int[] bestGrouping = new int[n];
        bestK[0] = 1;
        float lastDistance = 0.0f;
        float totalDistance = 0.0f;
        for (int k = minK; k <= maxK; k++) {
            int delta = n / k;
            for (int i2 = 0; i2 < k; i2++) {
                Point p = points[i2 * delta];
                center[i2].latRad = p.latRad;
                center[i2].lngRad = p.lngRad;
            }
            for (int iter = 0; iter < 30; iter++) {
                for (int i3 = 0; i3 < k; i3++) {
                    groupSum[i3].latRad = 0.0d;
                    groupSum[i3].lngRad = 0.0d;
                    groupCount[i3] = 0;
                }
                totalDistance = 0.0f;
                for (int i4 = 0; i4 < n; i4++) {
                    Point p2 = points[i4];
                    float bestDistance = Float.MAX_VALUE;
                    int bestIndex = 0;
                    for (int j = 0; j < k; j++) {
                        float distance = (float) GalleryUtils.fastDistanceMeters(p2.latRad, p2.lngRad, center[j].latRad, center[j].lngRad);
                        if (distance < 1.0f) {
                            distance = 0.0f;
                        }
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestIndex = j;
                        }
                    }
                    grouping[i4] = bestIndex;
                    groupCount[bestIndex] = groupCount[bestIndex] + 1;
                    groupSum[bestIndex].latRad += p2.latRad;
                    groupSum[bestIndex].lngRad += p2.lngRad;
                    totalDistance += bestDistance;
                }
                for (int i5 = 0; i5 < k; i5++) {
                    if (groupCount[i5] > 0) {
                        center[i5].latRad = groupSum[i5].latRad / ((double) groupCount[i5]);
                        center[i5].lngRad = groupSum[i5].lngRad / ((double) groupCount[i5]);
                    }
                }
                if (totalDistance == 0.0f || Math.abs(lastDistance - totalDistance) / totalDistance < 0.01f) {
                    break;
                }
                lastDistance = totalDistance;
            }
            int[] reassign = new int[k];
            int realK2 = 0;
            int i6 = 0;
            while (true) {
                realK = realK2;
                if (i6 >= k) {
                    break;
                }
                if (groupCount[i6] > 0) {
                    realK2 = realK + 1;
                    reassign[i6] = realK;
                } else {
                    realK2 = realK;
                }
                i6++;
            }
            float score = totalDistance * FloatMath.sqrt(realK);
            if (score < bestScore) {
                bestScore = score;
                bestK[0] = realK;
                for (int i7 = 0; i7 < n; i7++) {
                    bestGrouping[i7] = reassign[grouping[i7]];
                }
                if (score == 0.0f) {
                    break;
                }
            }
        }
        return bestGrouping;
    }
}
