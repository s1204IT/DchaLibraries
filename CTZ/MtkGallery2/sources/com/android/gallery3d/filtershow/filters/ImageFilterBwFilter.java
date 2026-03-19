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
        FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        filterBasicRepresentation.setName("BW Filter");
        filterBasicRepresentation.setSerializationName("BWFILTER");
        filterBasicRepresentation.setFilterClass(ImageFilterBwFilter.class);
        filterBasicRepresentation.setMaximum(180);
        filterBasicRepresentation.setMinimum(-180);
        filterBasicRepresentation.setTextId(R.string.bwfilter);
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
        int iHSVToColor = Color.HSVToColor(new float[]{180 + getParameters().getValue(), 1.0f, 1.0f});
        nativeApplyFilter(bitmap, width, height, 255 & (iHSVToColor >> 16), 255 & (iHSVToColor >> 8), 255 & (iHSVToColor >> 0));
        return bitmap;
    }
}
