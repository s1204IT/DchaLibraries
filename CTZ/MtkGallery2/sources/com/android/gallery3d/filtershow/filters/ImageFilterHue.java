package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.BasicEditor;

public class ImageFilterHue extends SimpleImageFilter {
    private ColorSpaceMatrix cmatrix;

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, float[] fArr);

    public ImageFilterHue() {
        this.cmatrix = null;
        this.mName = "Hue";
        this.cmatrix = new ColorSpaceMatrix();
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        filterBasicRepresentation.setName("Hue");
        filterBasicRepresentation.setSerializationName("HUE");
        filterBasicRepresentation.setFilterClass(ImageFilterHue.class);
        filterBasicRepresentation.setMinimum(-180);
        filterBasicRepresentation.setMaximum(180);
        filterBasicRepresentation.setTextId(R.string.hue);
        filterBasicRepresentation.setEditorId(BasicEditor.ID);
        filterBasicRepresentation.setSupportsPartialRendering(true);
        return filterBasicRepresentation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (getParameters() == null) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float value = getParameters().getValue();
        this.cmatrix.identity();
        this.cmatrix.setHue(value);
        nativeApplyFilter(bitmap, width, height, this.cmatrix.getMatrix());
        return bitmap;
    }
}
