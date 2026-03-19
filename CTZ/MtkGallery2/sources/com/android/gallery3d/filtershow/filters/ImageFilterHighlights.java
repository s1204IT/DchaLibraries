package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.R;

public class ImageFilterHighlights extends SimpleImageFilter {
    SplineMath mSpline = new SplineMath(5);
    double[] mHighlightCurve = {0.0d, 0.32d, 0.418d, 0.476d, 0.642d};

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, float[] fArr);

    public ImageFilterHighlights() {
        this.mName = "Highlights";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation filterBasicRepresentation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        filterBasicRepresentation.setName("Highlights");
        filterBasicRepresentation.setSerializationName("HIGHLIGHTS");
        filterBasicRepresentation.setFilterClass(ImageFilterHighlights.class);
        filterBasicRepresentation.setTextId(R.string.highlight_recovery);
        filterBasicRepresentation.setMinimum(-100);
        filterBasicRepresentation.setMaximum(100);
        filterBasicRepresentation.setDefaultValue(0);
        filterBasicRepresentation.setSupportsPartialRendering(true);
        return filterBasicRepresentation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        if (getParameters() == null) {
            return bitmap;
        }
        double value = ((double) getParameters().getValue()) / 100.0d;
        for (int i2 = 0; i2 < 5; i2++) {
            double d = ((double) i2) / 4.0d;
            this.mSpline.setPoint(i2, d, ((1.0d - value) * d) + (this.mHighlightCurve[i2] * value));
        }
        float[][] fArrCalculatetCurve = this.mSpline.calculatetCurve(256);
        float[] fArr = new float[fArrCalculatetCurve.length];
        for (int i3 = 0; i3 < fArr.length; i3++) {
            fArr[i3] = fArrCalculatetCurve[i3][1];
        }
        nativeApplyFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), fArr);
        return bitmap;
    }
}
