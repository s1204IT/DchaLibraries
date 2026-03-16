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
        FilterRepresentation representation = new FilterDirectRepresentation("WBalance");
        representation.setSerializationName("WBALANCE");
        representation.setFilterClass(ImageFilterWBalance.class);
        representation.setFilterType(3);
        representation.setTextId(R.string.wbalance);
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
        nativeApplyFilter(bitmap, w, h, -1, -1);
        return bitmap;
    }
}
