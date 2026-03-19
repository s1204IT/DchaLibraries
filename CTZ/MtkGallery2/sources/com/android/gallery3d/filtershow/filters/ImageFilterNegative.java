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
        FilterDirectRepresentation filterDirectRepresentation = new FilterDirectRepresentation("Negative");
        filterDirectRepresentation.setSerializationName("NEGATIVE");
        filterDirectRepresentation.setFilterClass(ImageFilterNegative.class);
        filterDirectRepresentation.setTextId(R.string.negative);
        filterDirectRepresentation.setShowParameterValue(false);
        filterDirectRepresentation.setEditorId(R.id.imageOnlyEditor);
        filterDirectRepresentation.setSupportsPartialRendering(true);
        filterDirectRepresentation.setIsBooleanFilter(true);
        return filterDirectRepresentation;
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        nativeApplyFilter(bitmap, bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }
}
