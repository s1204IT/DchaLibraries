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
        FilterRepresentation defaultRepresentation = super.getDefaultRepresentation();
        defaultRepresentation.setName("Edge");
        defaultRepresentation.setSerializationName("EDGE");
        defaultRepresentation.setFilterClass(ImageFilterEdge.class);
        defaultRepresentation.setTextId(R.string.edge);
        defaultRepresentation.setSupportsPartialRendering(true);
        return defaultRepresentation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (getParameters() == null) {
            return bitmap;
        }
        nativeApplyFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), (getParameters().getValue() + 101) / 100.0f);
        return bitmap;
    }
}
