package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.android.gallery3d.R;

public class ImageFilterBwFilter extends SimpleImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, int i3, int i4, int i5);

    public ImageFilterBwFilter() {
        this.mName = "BW Filter";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("BW Filter");
        representation.setSerializationName("BWFILTER");
        representation.setFilterClass(ImageFilterBwFilter.class);
        representation.setMaximum(180);
        representation.setMinimum(-180);
        representation.setTextId(R.string.bwfilter);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            float[] hsv = {getParameters().getValue() + 180, 1.0f, 1.0f};
            int rgb = Color.HSVToColor(hsv);
            int r = (rgb >> 16) & 255;
            int g = (rgb >> 8) & 255;
            int b = (rgb >> 0) & 255;
            nativeApplyFilter(bitmap, w, h, r, g, b);
        }
        return bitmap;
    }
}
