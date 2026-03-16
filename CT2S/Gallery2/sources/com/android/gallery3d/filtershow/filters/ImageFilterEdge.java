package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterEdge extends SimpleImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, float f);

    public ImageFilterEdge() {
        this.mName = "Edge";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterRepresentation representation = super.getDefaultRepresentation();
        representation.setName("Edge");
        representation.setSerializationName("EDGE");
        representation.setFilterClass(ImageFilterEdge.class);
        representation.setTextId(R.string.edge);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            float p = getParameters().getValue() + 101;
            nativeApplyFilter(bitmap, w, h, p / 100.0f);
        }
        return bitmap;
    }
}
