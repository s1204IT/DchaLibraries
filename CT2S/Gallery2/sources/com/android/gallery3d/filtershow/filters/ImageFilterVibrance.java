package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterVibrance extends SimpleImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, float f);

    public ImageFilterVibrance() {
        this.mName = "Vibrance";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("Vibrance");
        representation.setSerializationName("VIBRANCE");
        representation.setFilterClass(ImageFilterVibrance.class);
        representation.setTextId(R.string.vibrance);
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
            float value = getParameters().getValue();
            nativeApplyFilter(bitmap, w, h, value);
        }
        return bitmap;
    }
}
