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
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("Saturated");
        representation.setSerializationName("SATURATED");
        representation.setFilterClass(ImageFilterSaturated.class);
        representation.setTextId(R.string.saturation);
        representation.setMinimum(-100);
        representation.setMaximum(100);
        representation.setDefaultValue(0);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int p = getParameters().getValue();
            float value = 1.0f + (p / 100.0f);
            nativeApplyFilter(bitmap, w, h, value);
        }
        return bitmap;
    }
}
