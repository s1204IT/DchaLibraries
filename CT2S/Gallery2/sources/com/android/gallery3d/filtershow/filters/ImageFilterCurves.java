package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.filtershow.imageshow.Spline;

public class ImageFilterCurves extends ImageFilter {
    FilterCurvesRepresentation mParameters = new FilterCurvesRepresentation();

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterCurvesRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterCurvesRepresentation parameters = (FilterCurvesRepresentation) representation;
        this.mParameters = parameters;
    }

    public ImageFilterCurves() {
        this.mName = "Curves";
        reset();
    }

    public void populateArray(int[] array, int curveIndex) {
        Spline spline = this.mParameters.getSpline(curveIndex);
        if (spline != null) {
            float[] curve = spline.getAppliedCurve();
            for (int i = 0; i < 256; i++) {
                array[i] = (int) (curve[i] * 255.0f);
            }
        }
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (!this.mParameters.getSpline(0).isOriginal()) {
            int[] rgbGradient = new int[NotificationCompat.FLAG_LOCAL_ONLY];
            populateArray(rgbGradient, 0);
            nativeApplyGradientFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), rgbGradient, rgbGradient, rgbGradient);
        }
        int[] redGradient = null;
        if (!this.mParameters.getSpline(1).isOriginal()) {
            redGradient = new int[NotificationCompat.FLAG_LOCAL_ONLY];
            populateArray(redGradient, 1);
        }
        int[] greenGradient = null;
        if (!this.mParameters.getSpline(2).isOriginal()) {
            greenGradient = new int[NotificationCompat.FLAG_LOCAL_ONLY];
            populateArray(greenGradient, 2);
        }
        int[] blueGradient = null;
        if (!this.mParameters.getSpline(3).isOriginal()) {
            blueGradient = new int[NotificationCompat.FLAG_LOCAL_ONLY];
            populateArray(blueGradient, 3);
        }
        nativeApplyGradientFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), redGradient, greenGradient, blueGradient);
        return bitmap;
    }

    public void reset() {
        Spline spline = new Spline();
        spline.addPoint(0.0f, 1.0f);
        spline.addPoint(1.0f, 0.0f);
        for (int i = 0; i < 4; i++) {
            this.mParameters.setSpline(i, new Spline(spline));
        }
    }
}
