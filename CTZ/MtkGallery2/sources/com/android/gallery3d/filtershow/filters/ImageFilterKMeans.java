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
        Time time = new Time();
        time.setToNow();
        this.mSeed = (int) time.toMillis(false);
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        filterBasicRepresentation.setName("KMeans");
        filterBasicRepresentation.setSerializationName("KMEANS");
        filterBasicRepresentation.setFilterClass(ImageFilterKMeans.class);
        filterBasicRepresentation.setMaximum(20);
        filterBasicRepresentation.setMinimum(2);
        filterBasicRepresentation.setValue(4);
        filterBasicRepresentation.setDefaultValue(4);
        filterBasicRepresentation.setPreviewValue(4);
        filterBasicRepresentation.setTextId(R.string.kmeans);
        filterBasicRepresentation.setSupportsPartialRendering(true);
        return filterBasicRepresentation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (getParameters() == null) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int i2 = width;
        int i3 = height;
        while (i2 > 256 && i3 > 256) {
            i2 /= 2;
            i3 /= 2;
        }
        Bitmap bitmapCreateScaledBitmap = i2 != width ? Bitmap.createScaledBitmap(bitmap, i2, i3, true) : bitmap;
        int i4 = i2;
        int i5 = i3;
        while (i4 > 64 && i5 > 64) {
            i4 /= 2;
            i5 /= 2;
        }
        Bitmap bitmapCreateScaledBitmap2 = i4 != i2 ? Bitmap.createScaledBitmap(bitmapCreateScaledBitmap, i4, i5, true) : bitmap;
        if (getParameters() != null) {
            nativeApplyFilter(bitmap, width, height, bitmapCreateScaledBitmap, i2, i3, bitmapCreateScaledBitmap2, i4, i5, Math.max(getParameters().getValue(), getParameters().getMinimum()) % (getParameters().getMaximum() + 1), this.mSeed);
        }
        return bitmap;
    }
}
