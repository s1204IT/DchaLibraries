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
    public void useRepresentation(FilterRepresentation filterRepresentation) {
        this.mParameters = (FilterRedEyeRepresentation) filterRepresentation;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float f, int i) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        short[] sArr = new short[4];
        int numberOfCandidates = this.mParameters.getNumberOfCandidates();
        Matrix originalToScreenMatrix = getOriginalToScreenMatrix(width, height);
        for (int i2 = 0; i2 < numberOfCandidates; i2++) {
            RectF rectF = new RectF(((RedEyeCandidate) this.mParameters.getCandidate(i2)).mRect);
            originalToScreenMatrix.mapRect(rectF);
            if (rectF.intersect(0.0f, 0.0f, width, height)) {
                sArr[0] = (short) rectF.left;
                sArr[1] = (short) rectF.top;
                sArr[2] = (short) rectF.width();
                sArr[3] = (short) rectF.height();
                nativeApplyFilter(bitmap, width, height, sArr);
            }
        }
        return bitmap;
    }
}
