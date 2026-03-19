package com.android.gallery3d.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

        public Point(double d, double d2) {
            this.latRad = Math.toRadians(d);
            this.lngRad = Math.toRadians(d2);
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
                mediaItem.getLatLong(dArr);
                smallItem.lat = dArr[0];
                smallItem.lng = dArr[1];
                smallItemArr[i] = smallItem;
            }

            @Override
            public boolean stopConsume() {
                return LocationClustering.this.mStopEnumerate;
            }
        });
        ArrayList arrayList = new ArrayList();
        ArrayList<SmallItem> arrayList2 = new ArrayList<>();
        ArrayList arrayList3 = new ArrayList();
        for (int i = 0; i < totalMediaItemCount; i++) {
            SmallItem smallItem = smallItemArr[i];
            if (smallItem != null) {
                if (GalleryUtils.isValidLocation(smallItem.lat, smallItem.lng)) {
                    arrayList.add(smallItem);
                    arrayList3.add(new Point(smallItem.lat, smallItem.lng));
                } else {
                    arrayList2.add(smallItem);
                }
            }
        }
        ArrayList<ArrayList<SmallItem>> arrayList4 = new ArrayList();
        int size = arrayList.size();
        if (size > 0) {
            int[] iArr = new int[1];
            int[] iArrKMeans = kMeans((Point[]) arrayList3.toArray(new Point[size]), iArr);
            for (int i2 = 0; i2 < iArr[0]; i2++) {
                arrayList4.add(new ArrayList());
            }
            for (int i3 = 0; i3 < size; i3++) {
                ((ArrayList) arrayList4.get(iArrKMeans[i3])).add((SmallItem) arrayList.get(i3));
            }
        }
        ReverseGeocoder reverseGeocoder = new ReverseGeocoder(this.mContext);
        this.mNames = new ArrayList<>();
        this.mClusters = new ArrayList<>();
        boolean z = false;
        for (ArrayList<SmallItem> arrayList5 : arrayList4) {
            String strGenerateName = generateName(arrayList5, reverseGeocoder, true);
            if (strGenerateName != null) {
                this.mNames.add(strGenerateName);
                this.mClusters.add(arrayList5);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append((float) arrayList5.get(0).lat);
                sb.append(",");
                sb.append((float) arrayList5.get(0).lng);
                this.mNames.add(new String(sb));
                this.mClusters.add(arrayList5);
                z = true;
            }
        }
        if (arrayList2.size() > 0) {
            this.mNames.add(this.mNoLocationString);
            this.mClusters.add(arrayList2);
        }
        if (z) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LocationClustering.this.mContext, R.string.no_connectivity, 1).show();
                }
            });
        }
    }

    private static String generateName(ArrayList<SmallItem> arrayList, ReverseGeocoder reverseGeocoder, boolean z) {
        ReverseGeocoder.SetLatLong setLatLong = new ReverseGeocoder.SetLatLong();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            SmallItem smallItem = arrayList.get(i);
            double d = smallItem.lat;
            double d2 = smallItem.lng;
            if (setLatLong.mMinLatLatitude > d) {
                setLatLong.mMinLatLatitude = d;
                setLatLong.mMinLatLongitude = d2;
            }
            if (setLatLong.mMaxLatLatitude < d) {
                setLatLong.mMaxLatLatitude = d;
                setLatLong.mMaxLatLongitude = d2;
            }
            if (setLatLong.mMinLonLongitude > d2) {
                setLatLong.mMinLonLatitude = d;
                setLatLong.mMinLonLongitude = d2;
            }
            if (setLatLong.mMaxLonLongitude < d2) {
                setLatLong.mMaxLonLatitude = d;
                setLatLong.mMaxLonLongitude = d2;
            }
        }
        return reverseGeocoder.computeAddress(setLatLong, z);
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
        ArrayList<SmallItem> arrayList = this.mClusters.get(i);
        ArrayList<Path> arrayList2 = new ArrayList<>(arrayList.size());
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            arrayList2.add(arrayList.get(i2).path);
        }
        return arrayList2;
    }

    @Override
    public String getClusterName(int i) {
        return this.mNames.get(i);
    }

    private static int[] kMeans(Point[] pointArr, int[] iArr) {
        int i;
        int i2;
        float f;
        boolean z;
        int i3;
        int i4;
        Point[] pointArr2 = pointArr;
        int length = pointArr2.length;
        int iMin = Math.min(length, 1);
        int iMin2 = Math.min(length, 20);
        Point[] pointArr3 = new Point[iMin2];
        Point[] pointArr4 = new Point[iMin2];
        int[] iArr2 = new int[iMin2];
        int[] iArr3 = new int[length];
        int i5 = 0;
        for (int i6 = 0; i6 < iMin2; i6++) {
            pointArr3[i6] = new Point();
            pointArr4[i6] = new Point();
        }
        int[] iArr4 = new int[length];
        iArr[0] = 1;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = Float.MAX_VALUE;
        while (iMin <= iMin2) {
            int i7 = length / iMin;
            int i8 = i5;
            while (i8 < iMin) {
                Point point = pointArr2[i8 * i7];
                pointArr3[i8].latRad = point.latRad;
                pointArr3[i8].lngRad = point.lngRad;
                i8++;
                iArr4 = iArr4;
            }
            int[] iArr5 = iArr4;
            int i9 = 0;
            while (i9 < 30) {
                for (int i10 = 0; i10 < iMin; i10++) {
                    pointArr4[i10].latRad = 0.0d;
                    pointArr4[i10].lngRad = 0.0d;
                    iArr2[i10] = 0;
                }
                int i11 = 0;
                float f5 = 0.0f;
                while (i11 < length) {
                    Point point2 = pointArr2[i11];
                    int i12 = 0;
                    int i13 = 0;
                    float f6 = Float.MAX_VALUE;
                    while (i12 < iMin) {
                        int i14 = length;
                        int i15 = i9;
                        int i16 = iMin2;
                        int i17 = iMin;
                        float f7 = f2;
                        float f8 = f5;
                        float fFastDistanceMeters = (float) GalleryUtils.fastDistanceMeters(point2.latRad, point2.lngRad, pointArr3[i12].latRad, pointArr3[i12].lngRad);
                        if (fFastDistanceMeters < 1.0f) {
                            fFastDistanceMeters = 0.0f;
                        }
                        if (fFastDistanceMeters < f6) {
                            f6 = fFastDistanceMeters;
                            i13 = i12;
                        }
                        i12++;
                        length = i14;
                        i9 = i15;
                        iMin2 = i16;
                        iMin = i17;
                        f2 = f7;
                        f5 = f8;
                    }
                    iArr3[i11] = i13;
                    iArr2[i13] = iArr2[i13] + 1;
                    pointArr4[i13].latRad += point2.latRad;
                    pointArr4[i13].lngRad += point2.lngRad;
                    f5 += f6;
                    i11++;
                    length = length;
                    i9 = i9;
                    iMin2 = iMin2;
                    iMin = iMin;
                    pointArr2 = pointArr;
                }
                i = length;
                int i18 = i9;
                int i19 = iMin;
                i2 = iMin2;
                f = f2;
                float f9 = f5;
                z = true;
                int i20 = 0;
                while (true) {
                    iMin = i19;
                    if (i20 >= iMin) {
                        break;
                    }
                    if (iArr2[i20] > 0) {
                        pointArr3[i20].latRad = pointArr4[i20].latRad / ((double) iArr2[i20]);
                        pointArr3[i20].lngRad = pointArr4[i20].lngRad / ((double) iArr2[i20]);
                    }
                    i20++;
                    i19 = iMin;
                }
                if (f9 != 0.0f && Math.abs(f - f9) / f9 >= 0.01f) {
                    i9 = i18 + 1;
                    length = i;
                    iMin2 = i2;
                    f2 = f9;
                    f3 = f2;
                    pointArr2 = pointArr;
                } else {
                    f3 = f9;
                    break;
                }
            }
            i = length;
            i2 = iMin2;
            f = f2;
            z = true;
            int[] iArr6 = new int[iMin];
            int i21 = 0;
            for (int i22 = 0; i22 < iMin; i22++) {
                if (iArr2[i22] > 0) {
                    iArr6[i22] = i21;
                    i21++;
                }
            }
            float fSqrt = ((float) Math.sqrt(i21)) * f3;
            if (fSqrt < f4) {
                i4 = 0;
                iArr[0] = i21;
                int i23 = 0;
                while (true) {
                    i3 = i;
                    if (i23 >= i3) {
                        break;
                    }
                    iArr5[i23] = iArr6[iArr3[i23]];
                    i23++;
                    i = i3;
                }
                if (fSqrt == 0.0f) {
                    return iArr5;
                }
                f4 = fSqrt;
            } else {
                i3 = i;
                i4 = 0;
            }
            iMin++;
            length = i3;
            i5 = i4;
            iArr4 = iArr5;
            iMin2 = i2;
            f2 = f;
            pointArr2 = pointArr;
        }
        return iArr4;
    }

    @Override
    public boolean reGenerateName() {
        if (this.mClusters == null) {
            return false;
        }
        ReverseGeocoder reverseGeocoder = new ReverseGeocoder(this.mContext);
        synchronized (this.mClusters) {
            int size = this.mClusters.size();
            for (int i = 0; i < size; i++) {
                ArrayList<SmallItem> arrayList = this.mClusters.get(i);
                if (this.mNames.get(i) != this.mNoLocationString) {
                    String strGenerateName = generateName(arrayList, reverseGeocoder, false);
                    if (strGenerateName != null) {
                        this.mNames.set(i, strGenerateName);
                    }
                } else {
                    this.mNoLocationString = this.mContext.getResources().getString(R.string.no_location);
                    this.mNames.set(i, this.mNoLocationString);
                }
            }
        }
        return true;
    }
}
