package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
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
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("Highlights");
        representation.setSerializationName("HIGHLIGHTS");
        representation.setFilterClass(ImageFilterHighlights.class);
        representation.setTextId(R.string.highlight_recovery);
        representation.setMinimum(-100);
        representation.setMaximum(100);
        representation.setDefaultValue(0);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() != null) {
            float p = getParameters().getValue();
            double t = ((double) p) / 100.0d;
            for (int i = 0; i < 5; i++) {
                double x = ((double) i) / 4.0d;
                double y = (this.mHighlightCurve[i] * t) + ((1.0d - t) * x);
                this.mSpline.setPoint(i, x, y);
            }
            float[][] curve = this.mSpline.calculatetCurve(NotificationCompat.FLAG_LOCAL_ONLY);
            float[] luminanceMap = new float[curve.length];
            for (int i2 = 0; i2 < luminanceMap.length; i2++) {
                luminanceMap[i2] = curve[i2][1];
            }
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            nativeApplyFilter(bitmap, w, h, luminanceMap);
        }
        return bitmap;
    }
}
