package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterSaturated extends SimpleImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, float f);

    public ImageFilterSaturated() {
        this.mName = "Saturated";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        filterBasicRepresentation.setName("Saturated");
        filterBasicRepresentation.setSerializationName("SATURATED");
        filterBasicRepresentation.setFilterClass(ImageFilterSaturated.class);
        filterBasicRepresentation.setTextId(R.string.saturation);
        filterBasicRepresentation.setMinimum(-100);
        filterBasicRepresentation.setMaximum(100);
        filterBasicRepresentation.setDefaultValue(0);
        filterBasicRepresentation.setSupportsPartialRendering(true);
        return filterBasicRepresentation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (getParameters() == null) {
            return bitmap;
        }
        nativeApplyFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), 1.0f + (getParameters().getValue() / 100.0f));
        return bitmap;
    }
}
