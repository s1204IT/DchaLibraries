package com.android.internal.colorextraction;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.os.Trace;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.types.ExtractionType;
import com.android.internal.colorextraction.types.Tonal;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ColorExtractor implements WallpaperManager.OnColorsChangedListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "ColorExtractor";
    public static final int TYPE_DARK = 1;
    public static final int TYPE_EXTRA_DARK = 2;
    public static final int TYPE_NORMAL = 0;
    private static final int[] sGradientTypes = {0, 1, 2};
    private final Context mContext;
    private final ExtractionType mExtractionType;
    protected final SparseArray<GradientColors[]> mGradientColors;
    protected WallpaperColors mLockColors;
    private final ArrayList<WeakReference<OnColorsChangedListener>> mOnColorsChangedListeners;
    protected WallpaperColors mSystemColors;

    public interface OnColorsChangedListener {
        void onColorsChanged(ColorExtractor colorExtractor, int i);
    }

    public ColorExtractor(Context context) {
        this(context, new Tonal(context));
    }

    @VisibleForTesting
    public ColorExtractor(Context context, ExtractionType extractionType) {
        this.mContext = context;
        this.mExtractionType = extractionType;
        this.mGradientColors = new SparseArray<>();
        for (int i : new int[]{2, 1}) {
            GradientColors[] gradientColorsArr = new GradientColors[sGradientTypes.length];
            this.mGradientColors.append(i, gradientColorsArr);
            for (int i2 : sGradientTypes) {
                gradientColorsArr[i2] = new GradientColors();
            }
        }
        this.mOnColorsChangedListeners = new ArrayList<>();
        GradientColors[] gradientColorsArr2 = this.mGradientColors.get(1);
        GradientColors[] gradientColorsArr3 = this.mGradientColors.get(2);
        WallpaperManager wallpaperManager = (WallpaperManager) this.mContext.getSystemService(WallpaperManager.class);
        if (wallpaperManager == null) {
            Log.w(TAG, "Can't listen to color changes!");
        } else {
            wallpaperManager.addOnColorsChangedListener(this, null);
            Trace.beginSection("ColorExtractor#getWallpaperColors");
            this.mSystemColors = wallpaperManager.getWallpaperColors(1);
            this.mLockColors = wallpaperManager.getWallpaperColors(2);
            Trace.endSection();
        }
        extractInto(this.mSystemColors, gradientColorsArr2[0], gradientColorsArr2[1], gradientColorsArr2[2]);
        extractInto(this.mLockColors, gradientColorsArr3[0], gradientColorsArr3[1], gradientColorsArr3[2]);
    }

    public GradientColors getColors(int i) {
        return getColors(i, 1);
    }

    public GradientColors getColors(int i, int i2) {
        if (i2 != 0 && i2 != 1 && i2 != 2) {
            throw new IllegalArgumentException("type should be TYPE_NORMAL, TYPE_DARK or TYPE_EXTRA_DARK");
        }
        if (i != 2 && i != 1) {
            throw new IllegalArgumentException("which should be FLAG_SYSTEM or FLAG_NORMAL");
        }
        return this.mGradientColors.get(i)[i2];
    }

    public WallpaperColors getWallpaperColors(int i) {
        if (i == 2) {
            return this.mLockColors;
        }
        if (i == 1) {
            return this.mSystemColors;
        }
        throw new IllegalArgumentException("Invalid value for which: " + i);
    }

    @Override
    public void onColorsChanged(WallpaperColors wallpaperColors, int i) {
        boolean z;
        if ((i & 2) != 0) {
            this.mLockColors = wallpaperColors;
            GradientColors[] gradientColorsArr = this.mGradientColors.get(2);
            extractInto(wallpaperColors, gradientColorsArr[0], gradientColorsArr[1], gradientColorsArr[2]);
            z = true;
        } else {
            z = false;
        }
        if ((i & 1) != 0) {
            this.mSystemColors = wallpaperColors;
            GradientColors[] gradientColorsArr2 = this.mGradientColors.get(1);
            extractInto(wallpaperColors, gradientColorsArr2[0], gradientColorsArr2[1], gradientColorsArr2[2]);
            z = true;
        }
        if (z) {
            triggerColorsChanged(i);
        }
    }

    protected void triggerColorsChanged(int i) {
        ArrayList arrayList = new ArrayList(this.mOnColorsChangedListeners);
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            WeakReference weakReference = (WeakReference) arrayList.get(i2);
            OnColorsChangedListener onColorsChangedListener = (OnColorsChangedListener) weakReference.get();
            if (onColorsChangedListener == null) {
                this.mOnColorsChangedListeners.remove(weakReference);
            } else {
                onColorsChangedListener.onColorsChanged(this, i);
            }
        }
    }

    private void extractInto(WallpaperColors wallpaperColors, GradientColors gradientColors, GradientColors gradientColors2, GradientColors gradientColors3) {
        this.mExtractionType.extractInto(wallpaperColors, gradientColors, gradientColors2, gradientColors3);
    }

    public void destroy() {
        WallpaperManager wallpaperManager = (WallpaperManager) this.mContext.getSystemService(WallpaperManager.class);
        if (wallpaperManager != null) {
            wallpaperManager.removeOnColorsChangedListener(this);
        }
    }

    public void addOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener) {
        this.mOnColorsChangedListeners.add(new WeakReference<>(onColorsChangedListener));
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener) {
        ArrayList arrayList = new ArrayList(this.mOnColorsChangedListeners);
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            WeakReference weakReference = (WeakReference) arrayList.get(i);
            if (weakReference.get() == onColorsChangedListener) {
                this.mOnColorsChangedListeners.remove(weakReference);
                return;
            }
        }
    }

    public static class GradientColors {
        private int mMainColor;
        private int mSecondaryColor;
        private boolean mSupportsDarkText;

        public void setMainColor(int i) {
            this.mMainColor = i;
        }

        public void setSecondaryColor(int i) {
            this.mSecondaryColor = i;
        }

        public void setSupportsDarkText(boolean z) {
            this.mSupportsDarkText = z;
        }

        public void set(GradientColors gradientColors) {
            this.mMainColor = gradientColors.mMainColor;
            this.mSecondaryColor = gradientColors.mSecondaryColor;
            this.mSupportsDarkText = gradientColors.mSupportsDarkText;
        }

        public int getMainColor() {
            return this.mMainColor;
        }

        public int getSecondaryColor() {
            return this.mSecondaryColor;
        }

        public boolean supportsDarkText() {
            return this.mSupportsDarkText;
        }

        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            GradientColors gradientColors = (GradientColors) obj;
            return gradientColors.mMainColor == this.mMainColor && gradientColors.mSecondaryColor == this.mSecondaryColor && gradientColors.mSupportsDarkText == this.mSupportsDarkText;
        }

        public int hashCode() {
            return (31 * ((this.mMainColor * 31) + this.mSecondaryColor)) + (!this.mSupportsDarkText ? 1 : 0);
        }

        public String toString() {
            return "GradientColors(" + Integer.toHexString(this.mMainColor) + ", " + Integer.toHexString(this.mSecondaryColor) + ")";
        }
    }
}
