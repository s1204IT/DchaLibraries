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
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("Hue");
        representation.setSerializationName("HUE");
        representation.setFilterClass(ImageFilterHue.class);
        representation.setMinimum(-180);
        representation.setMaximum(180);
        representation.setTextId(R.string.hue);
        representation.setEditorId(BasicEditor.ID);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            float value = getParameters().getValue();
            this.cmatrix.identity();
            this.cmatrix.setHue(value);
            nativeApplyFilter(bitmap, w, h, this.cmatrix.getMatrix());
        }
        return bitmap;
    }
}
