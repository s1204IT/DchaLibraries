package com.android.phone.common.compat;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.contacts.ContactPhotoManager;

public class PathInterpolatorCompat {
    public static Interpolator create(float f, float f2, float f3, float f4) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new PathInterpolator(f, f2, f3, f4);
        }
        return new PathInterpolatorBase(f, f2, f3, f4);
    }

    private static class PathInterpolatorBase implements Interpolator {
        private final float[] mX;
        private final float[] mY;

        public PathInterpolatorBase(Path path) {
            PathMeasure pathMeasure = new PathMeasure(path, false);
            float length = pathMeasure.getLength();
            int i = ((int) (length / 0.002f)) + 1;
            this.mX = new float[i];
            this.mY = new float[i];
            float[] fArr = new float[2];
            for (int i2 = 0; i2 < i; i2++) {
                pathMeasure.getPosTan((i2 * length) / (i - 1), fArr, null);
                this.mX[i2] = fArr[0];
                this.mY[i2] = fArr[1];
            }
        }

        public PathInterpolatorBase(float f, float f2, float f3, float f4) {
            this(createCubic(f, f2, f3, f4));
        }

        @Override
        public float getInterpolation(float f) {
            if (f <= ContactPhotoManager.OFFSET_DEFAULT) {
                return ContactPhotoManager.OFFSET_DEFAULT;
            }
            if (f >= 1.0f) {
                return 1.0f;
            }
            int i = 0;
            int length = this.mX.length - 1;
            while (length - i > 1) {
                int i2 = (i + length) / 2;
                if (f < this.mX[i2]) {
                    length = i2;
                } else {
                    i = i2;
                }
            }
            float f2 = this.mX[length] - this.mX[i];
            if (f2 == ContactPhotoManager.OFFSET_DEFAULT) {
                return this.mY[i];
            }
            float f3 = (f - this.mX[i]) / f2;
            float f4 = this.mY[i];
            return f4 + (f3 * (this.mY[length] - f4));
        }

        private static Path createCubic(float f, float f2, float f3, float f4) {
            Path path = new Path();
            path.moveTo(ContactPhotoManager.OFFSET_DEFAULT, ContactPhotoManager.OFFSET_DEFAULT);
            path.cubicTo(f, f2, f3, f4, 1.0f, 1.0f);
            return path;
        }
    }
}
