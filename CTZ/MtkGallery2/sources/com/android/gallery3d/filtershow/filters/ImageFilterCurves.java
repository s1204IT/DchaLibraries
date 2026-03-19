package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.imageshow.Spline;

public class ImageFilterCurves extends ImageFilter {
    FilterCurvesRepresentation mParameters = new FilterCurvesRepresentation();

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterCurvesRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterCurvesRepresentation) filterRepresentation;
    }

    public ImageFilterCurves() {
        this.mName = "Curves";
        reset();
    }

    public void populateArray(int[] iArr, int i) {
        Spline spline = this.mParameters.getSpline(i);
        if (spline == null) {
            return;
        }
        float[] appliedCurve = spline.getAppliedCurve();
        for (int i2 = 0; i2 < 256; i2++) {
            iArr[i2] = (int) (appliedCurve[i2] * 255.0f);
        }
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        int[] iArr;
        int[] iArr2;
        if (!this.mParameters.getSpline(0).isOriginal()) {
            int[] iArr3 = new int[256];
            populateArray(iArr3, 0);
            nativeApplyGradientFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), iArr3, iArr3, iArr3);
        }
        int[] iArr4 = null;
        if (this.mParameters.getSpline(1).isOriginal()) {
            iArr = null;
        } else {
            int[] iArr5 = new int[256];
            populateArray(iArr5, 1);
            iArr = iArr5;
        }
        if (this.mParameters.getSpline(2).isOriginal()) {
            iArr2 = null;
        } else {
            int[] iArr6 = new int[256];
            populateArray(iArr6, 2);
            iArr2 = iArr6;
        }
        if (!this.mParameters.getSpline(3).isOriginal()) {
            iArr4 = new int[256];
            populateArray(iArr4, 3);
        }
        nativeApplyGradientFilter(bitmap, bitmap.getWidth(), bitmap.getHeight(), iArr, iArr2, iArr4);
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
