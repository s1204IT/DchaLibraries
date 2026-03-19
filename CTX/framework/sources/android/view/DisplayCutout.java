package android.view;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.PathParser;
import android.util.proto.ProtoOutputStream;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DisplayCutout {
    private static final String BOTTOM_MARKER = "@bottom";
    private static final String DP_MARKER = "@dp";
    public static final String EMULATION_OVERLAY_CATEGORY = "com.android.internal.display_cutout_emulation";
    private static final String RIGHT_MARKER = "@right";
    private static final String TAG = "DisplayCutout";

    @GuardedBy("CACHE_LOCK")
    private static float sCachedDensity;

    @GuardedBy("CACHE_LOCK")
    private static int sCachedDisplayHeight;

    @GuardedBy("CACHE_LOCK")
    private static int sCachedDisplayWidth;

    @GuardedBy("CACHE_LOCK")
    private static String sCachedSpec;
    private final Region mBounds;
    private final Rect mSafeInsets;
    private static final Rect ZERO_RECT = new Rect();
    private static final Region EMPTY_REGION = new Region();
    public static final DisplayCutout NO_CUTOUT = new DisplayCutout(ZERO_RECT, EMPTY_REGION, false);
    private static final Pair<Path, DisplayCutout> NULL_PAIR = new Pair<>(null, null);
    private static final Object CACHE_LOCK = new Object();

    @GuardedBy("CACHE_LOCK")
    private static Pair<Path, DisplayCutout> sCachedCutout = NULL_PAIR;

    public DisplayCutout(Rect rect, List<Rect> list) {
        this(rect != null ? new Rect(rect) : ZERO_RECT, boundingRectsToRegion(list), true);
    }

    private DisplayCutout(Rect rect, Region region, boolean z) {
        if (rect == null) {
            rect = ZERO_RECT;
        } else if (z) {
            rect = new Rect(rect);
        }
        this.mSafeInsets = rect;
        if (region == null) {
            region = Region.obtain();
        } else if (z) {
            region = Region.obtain(region);
        }
        this.mBounds = region;
    }

    public boolean isEmpty() {
        return this.mSafeInsets.equals(ZERO_RECT);
    }

    public boolean isBoundsEmpty() {
        return this.mBounds.isEmpty();
    }

    public int getSafeInsetTop() {
        return this.mSafeInsets.top;
    }

    public int getSafeInsetBottom() {
        return this.mSafeInsets.bottom;
    }

    public int getSafeInsetLeft() {
        return this.mSafeInsets.left;
    }

    public int getSafeInsetRight() {
        return this.mSafeInsets.right;
    }

    public Rect getSafeInsets() {
        return new Rect(this.mSafeInsets);
    }

    public Region getBounds() {
        return Region.obtain(this.mBounds);
    }

    public List<Rect> getBoundingRects() {
        ArrayList arrayList = new ArrayList();
        Region regionObtain = Region.obtain();
        regionObtain.set(this.mBounds);
        regionObtain.op(0, 0, Integer.MAX_VALUE, getSafeInsetTop(), Region.Op.INTERSECT);
        if (!regionObtain.isEmpty()) {
            arrayList.add(regionObtain.getBounds());
        }
        regionObtain.set(this.mBounds);
        regionObtain.op(0, 0, getSafeInsetLeft(), Integer.MAX_VALUE, Region.Op.INTERSECT);
        if (!regionObtain.isEmpty()) {
            arrayList.add(regionObtain.getBounds());
        }
        regionObtain.set(this.mBounds);
        regionObtain.op(getSafeInsetLeft() + 1, getSafeInsetTop() + 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Region.Op.INTERSECT);
        if (!regionObtain.isEmpty()) {
            arrayList.add(regionObtain.getBounds());
        }
        regionObtain.recycle();
        return arrayList;
    }

    public int hashCode() {
        return (this.mSafeInsets.hashCode() * 31) + this.mBounds.getBounds().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DisplayCutout)) {
            return false;
        }
        DisplayCutout displayCutout = (DisplayCutout) obj;
        return this.mSafeInsets.equals(displayCutout.mSafeInsets) && this.mBounds.equals(displayCutout.mBounds);
    }

    public String toString() {
        return "DisplayCutout{insets=" + this.mSafeInsets + " boundingRect=" + this.mBounds.getBounds() + "}";
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        this.mSafeInsets.writeToProto(protoOutputStream, 1146756268033L);
        this.mBounds.getBounds().writeToProto(protoOutputStream, 1146756268034L);
        protoOutputStream.end(jStart);
    }

    public DisplayCutout inset(int i, int i2, int i3, int i4) {
        if (this.mBounds.isEmpty() || (i == 0 && i2 == 0 && i3 == 0 && i4 == 0)) {
            return this;
        }
        Rect rect = new Rect(this.mSafeInsets);
        Region regionObtain = Region.obtain(this.mBounds);
        if (i2 > 0 || rect.top > 0) {
            rect.top = atLeastZero(rect.top - i2);
        }
        if (i4 > 0 || rect.bottom > 0) {
            rect.bottom = atLeastZero(rect.bottom - i4);
        }
        if (i > 0 || rect.left > 0) {
            rect.left = atLeastZero(rect.left - i);
        }
        if (i3 > 0 || rect.right > 0) {
            rect.right = atLeastZero(rect.right - i3);
        }
        regionObtain.translate(-i, -i2);
        return new DisplayCutout(rect, regionObtain, false);
    }

    public DisplayCutout replaceSafeInsets(Rect rect) {
        return new DisplayCutout(new Rect(rect), this.mBounds, false);
    }

    private static int atLeastZero(int i) {
        if (i < 0) {
            return 0;
        }
        return i;
    }

    public static DisplayCutout fromBoundingRect(int i, int i2, int i3, int i4) {
        Path path = new Path();
        path.reset();
        float f = i;
        float f2 = i2;
        path.moveTo(f, f2);
        float f3 = i4;
        path.lineTo(f, f3);
        float f4 = i3;
        path.lineTo(f4, f3);
        path.lineTo(f4, f2);
        path.close();
        return fromBounds(path);
    }

    public static DisplayCutout fromBounds(Path path) {
        RectF rectF = new RectF();
        path.computeBounds(rectF, false);
        Region regionObtain = Region.obtain();
        regionObtain.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
        Region region = new Region();
        region.setPath(path, regionObtain);
        regionObtain.recycle();
        return new DisplayCutout(ZERO_RECT, region, false);
    }

    public static DisplayCutout fromResources(Resources resources, int i, int i2) {
        return fromSpec(resources.getString(R.string.config_mainBuiltInDisplayCutout), i, i2, DisplayMetrics.DENSITY_DEVICE_STABLE / 160.0f);
    }

    public static Path pathFromResources(Resources resources, int i, int i2) {
        return pathAndDisplayCutoutFromSpec(resources.getString(R.string.config_mainBuiltInDisplayCutout), i, i2, DisplayMetrics.DENSITY_DEVICE_STABLE / 160.0f).first;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static DisplayCutout fromSpec(String str, int i, int i2, float f) {
        return pathAndDisplayCutoutFromSpec(str, i, i2, f).second;
    }

    private static Pair<Path, DisplayCutout> pathAndDisplayCutoutFromSpec(String str, int i, int i2, float f) {
        float f2;
        if (TextUtils.isEmpty(str)) {
            return NULL_PAIR;
        }
        synchronized (CACHE_LOCK) {
            if (str.equals(sCachedSpec) && sCachedDisplayWidth == i && sCachedDisplayHeight == i2 && sCachedDensity == f) {
                return sCachedCutout;
            }
            String strTrim = str.trim();
            if (strTrim.endsWith(RIGHT_MARKER)) {
                f2 = i;
                strTrim = strTrim.substring(0, strTrim.length() - RIGHT_MARKER.length()).trim();
            } else {
                f2 = i / 2.0f;
            }
            boolean zEndsWith = strTrim.endsWith(DP_MARKER);
            if (zEndsWith) {
                strTrim = strTrim.substring(0, strTrim.length() - DP_MARKER.length());
            }
            String strTrim2 = null;
            if (strTrim.contains(BOTTOM_MARKER)) {
                String[] strArrSplit = strTrim.split(BOTTOM_MARKER, 2);
                String strTrim3 = strArrSplit[0].trim();
                strTrim2 = strArrSplit[1].trim();
                strTrim = strTrim3;
            }
            try {
                Path pathCreatePathFromPathData = PathParser.createPathFromPathData(strTrim);
                Matrix matrix = new Matrix();
                if (zEndsWith) {
                    matrix.postScale(f, f);
                }
                matrix.postTranslate(f2, 0.0f);
                pathCreatePathFromPathData.transform(matrix);
                if (strTrim2 != null) {
                    try {
                        Path pathCreatePathFromPathData2 = PathParser.createPathFromPathData(strTrim2);
                        matrix.postTranslate(0.0f, i2);
                        pathCreatePathFromPathData2.transform(matrix);
                        pathCreatePathFromPathData.addPath(pathCreatePathFromPathData2);
                    } catch (Throwable th) {
                        Log.wtf(TAG, "Could not inflate bottom cutout: ", th);
                        return NULL_PAIR;
                    }
                }
                Pair<Path, DisplayCutout> pair = new Pair<>(pathCreatePathFromPathData, fromBounds(pathCreatePathFromPathData));
                synchronized (CACHE_LOCK) {
                    sCachedSpec = strTrim;
                    sCachedDisplayWidth = i;
                    sCachedDisplayHeight = i2;
                    sCachedDensity = f;
                    sCachedCutout = pair;
                }
                return pair;
            } catch (Throwable th2) {
                Log.wtf(TAG, "Could not inflate cutout: ", th2);
                return NULL_PAIR;
            }
        }
    }

    private static Region boundingRectsToRegion(List<Rect> list) {
        Region regionObtain = Region.obtain();
        if (list != null) {
            Iterator<Rect> it = list.iterator();
            while (it.hasNext()) {
                regionObtain.op(it.next(), Region.Op.UNION);
            }
        }
        return regionObtain;
    }

    public static final class ParcelableWrapper implements Parcelable {
        public static final Parcelable.Creator<ParcelableWrapper> CREATOR = new Parcelable.Creator<ParcelableWrapper>() {
            @Override
            public ParcelableWrapper createFromParcel(Parcel parcel) {
                return new ParcelableWrapper(ParcelableWrapper.readCutoutFromParcel(parcel));
            }

            @Override
            public ParcelableWrapper[] newArray(int i) {
                return new ParcelableWrapper[i];
            }
        };
        private DisplayCutout mInner;

        public ParcelableWrapper() {
            this(DisplayCutout.NO_CUTOUT);
        }

        public ParcelableWrapper(DisplayCutout displayCutout) {
            this.mInner = displayCutout;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            writeCutoutToParcel(this.mInner, parcel, i);
        }

        public static void writeCutoutToParcel(DisplayCutout displayCutout, Parcel parcel, int i) {
            if (displayCutout == null) {
                parcel.writeInt(-1);
            } else {
                if (displayCutout == DisplayCutout.NO_CUTOUT) {
                    parcel.writeInt(0);
                    return;
                }
                parcel.writeInt(1);
                parcel.writeTypedObject(displayCutout.mSafeInsets, i);
                parcel.writeTypedObject(displayCutout.mBounds, i);
            }
        }

        public void readFromParcel(Parcel parcel) {
            this.mInner = readCutoutFromParcel(parcel);
        }

        public static DisplayCutout readCutoutFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            if (i == -1) {
                return null;
            }
            if (i == 0) {
                return DisplayCutout.NO_CUTOUT;
            }
            return new DisplayCutout((Rect) parcel.readTypedObject(Rect.CREATOR), (Region) parcel.readTypedObject(Region.CREATOR), false);
        }

        public DisplayCutout get() {
            return this.mInner;
        }

        public void set(ParcelableWrapper parcelableWrapper) {
            this.mInner = parcelableWrapper.get();
        }

        public void set(DisplayCutout displayCutout) {
            this.mInner = displayCutout;
        }

        public int hashCode() {
            return this.mInner.hashCode();
        }

        public boolean equals(Object obj) {
            return (obj instanceof ParcelableWrapper) && this.mInner.equals(((ParcelableWrapper) obj).mInner);
        }

        public String toString() {
            return String.valueOf(this.mInner);
        }
    }
}
