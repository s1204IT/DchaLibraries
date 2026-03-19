package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterWBalance extends ImageFilter {
    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, int i3, int i4);

    public ImageFilterWBalance() {
        this.mName = "WBalance";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterDirectRepresentation filterDirectRepresentation = new FilterDirectRepresentation("WBalance");
        filterDirectRepresentation.setSerializationName("WBALANCE");
        filterDirectRepresentation.setFilterClass(ImageFilterWBalance.class);
        filterDirectRepresentation.setFilterType(3);
        filterDirectRepresentation.setTextId(R.string.wbalance);
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
        nativeApplyFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), -1, -1);
        return bitmap;
    }
}
