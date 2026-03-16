package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.text.format.Time;
import com.android.gallery3d.R;

public class ImageFilterKMeans extends SimpleImageFilter {
    private int mSeed;

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, Bitmap bitmap2, int i3, int i4, Bitmap bitmap3, int i5, int i6, int i7, int i8);

    public ImageFilterKMeans() {
        this.mSeed = 0;
        this.mName = "KMeans";
        Time t = new Time();
        t.setToNow();
        this.mSeed = (int) t.toMillis(false);
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("KMeans");
        representation.setSerializationName("KMEANS");
        representation.setFilterClass(ImageFilterKMeans.class);
        representation.setMaximum(20);
        representation.setMinimum(2);
        representation.setValue(4);
        representation.setDefaultValue(4);
        representation.setPreviewValue(4);
        representation.setTextId(R.string.kmeans);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Bitmap large_bm_ds = bitmap;
            Bitmap small_bm_ds = bitmap;
            int lw = w;
            int lh = h;
            while (lw > 256 && lh > 256) {
                lw /= 2;
                lh /= 2;
            }
            if (lw != w) {
                large_bm_ds = Bitmap.createScaledBitmap(bitmap, lw, lh, true);
            }
            int sw = lw;
            int sh = lh;
            while (sw > 64 && sh > 64) {
                sw /= 2;
                sh /= 2;
            }
            if (sw != lw) {
                small_bm_ds = Bitmap.createScaledBitmap(large_bm_ds, sw, sh, true);
            }
            if (getParameters() != null) {
                int p = Math.max(getParameters().getValue(), getParameters().getMinimum()) % (getParameters().getMaximum() + 1);
                nativeApplyFilter(bitmap, w, h, large_bm_ds, lw, lh, small_bm_ds, sw, sh, p, this.mSeed);
            }
        }
        return bitmap;
    }
}
