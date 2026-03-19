package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterShadows extends SimpleImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, float f);

    public ImageFilterShadows() {
        this.mName = "Shadows";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        filterBasicRepresentation.setName("Shadows");
        filterBasicRepresentation.setSerializationName("SHADOWS");
        filterBasicRepresentation.setFilterClass(ImageFilterShadows.class);
        filterBasicRepresentation.setTextId(R.string.shadow_recovery);
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
        nativeApplyFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), getParameters().getValue());
        return bitmap;
    }
}
