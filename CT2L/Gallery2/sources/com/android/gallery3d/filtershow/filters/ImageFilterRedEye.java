package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

public class ImageFilterRedEye extends ImageFilter {
    FilterRedEyeRepresentation mParameters = new FilterRedEyeRepresentation();

    protected native void nativeApplyFilter(Bitmap bitmap, int i, int i2, short[] sArr);

    public ImageFilterRedEye() {
        this.mName = "Red Eye";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterRedEyeRepresentation();
    }

    public void clear() {
        this.mParameters.clearCandidates();
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterRedEyeRepresentation parameters = (FilterRedEyeRepresentation) representation;
        this.mParameters = parameters;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        short[] rect = new short[4];
        int size = this.mParameters.getNumberOfCandidates();
        Matrix originalToScreen = getOriginalToScreenMatrix(w, h);
        for (int i = 0; i < size; i++) {
            RectF r = new RectF(((RedEyeCandidate) this.mParameters.getCandidate(i)).mRect);
            originalToScreen.mapRect(r);
            if (r.intersect(0.0f, 0.0f, w, h)) {
                rect[0] = (short) r.left;
                rect[1] = (short) r.top;
                rect[2] = (short) r.width();
                rect[3] = (short) r.height();
                nativeApplyFilter(bitmap, w, h, rect);
            }
        }
        return bitmap;
    }
}
