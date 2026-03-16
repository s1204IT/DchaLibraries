package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterNegative extends ImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2);

    public ImageFilterNegative() {
        this.mName = "Negative";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterRepresentation representation = new FilterDirectRepresentation("Negative");
        representation.setSerializationName("NEGATIVE");
        representation.setFilterClass(ImageFilterNegative.class);
        representation.setTextId(R.string.negative);
        representation.setShowParameterValue(false);
        representation.setEditorId(R.id.imageOnlyEditor);
        representation.setSupportsPartialRendering(true);
        representation.setIsBooleanFilter(true);
        return representation;
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        nativeApplyFilter(bitmap, w, h);
        return bitmap;
    }
}
