package android.util;

public abstract class Spline {
    public abstract float interpolate(float f);

    public static Spline createSpline(float[] fArr, float[] fArr2) {
        if (!isStrictlyIncreasing(fArr)) {
            throw new IllegalArgumentException("The control points must all have strictly increasing X values.");
        }
        if (isMonotonic(fArr2)) {
            return createMonotoneCubicSpline(fArr, fArr2);
        }
        return createLinearSpline(fArr, fArr2);
    }

    public static Spline createMonotoneCubicSpline(float[] fArr, float[] fArr2) {
        return new MonotoneCubicSpline(fArr, fArr2);
    }

    public static Spline createLinearSpline(float[] fArr, float[] fArr2) {
        return new LinearSpline(fArr, fArr2);
    }

    private static boolean isStrictlyIncreasing(float[] fArr) {
        if (fArr == null || fArr.length < 2) {
            throw new IllegalArgumentException("There must be at least two control points.");
        }
        float f = fArr[0];
        int i = 1;
        while (i < fArr.length) {
            float f2 = fArr[i];
            if (f2 <= f) {
                return false;
            }
            i++;
            f = f2;
        }
        return true;
    }

    private static boolean isMonotonic(float[] fArr) {
        if (fArr == null || fArr.length < 2) {
            throw new IllegalArgumentException("There must be at least two control points.");
        }
        float f = fArr[0];
        int i = 1;
        while (i < fArr.length) {
            float f2 = fArr[i];
            if (f2 < f) {
                return false;
            }
            i++;
            f = f2;
        }
        return true;
    }

    public static class MonotoneCubicSpline extends Spline {
        private float[] mM;
        private float[] mX;
        private float[] mY;

        public MonotoneCubicSpline(float[] fArr, float[] fArr2) {
            if (fArr == null || fArr2 == null || fArr.length != fArr2.length || fArr.length < 2) {
                throw new IllegalArgumentException("There must be at least two control points and the arrays must be of equal length.");
            }
            int length = fArr.length;
            int i = length - 1;
            float[] fArr3 = new float[i];
            float[] fArr4 = new float[length];
            int i2 = 0;
            while (i2 < i) {
                int i3 = i2 + 1;
                float f = fArr[i3] - fArr[i2];
                if (f <= 0.0f) {
                    throw new IllegalArgumentException("The control points must all have strictly increasing X values.");
                }
                fArr3[i2] = (fArr2[i3] - fArr2[i2]) / f;
                i2 = i3;
            }
            fArr4[0] = fArr3[0];
            for (int i4 = 1; i4 < i; i4++) {
                fArr4[i4] = (fArr3[i4 - 1] + fArr3[i4]) * 0.5f;
            }
            fArr4[i] = fArr3[length - 2];
            for (int i5 = 0; i5 < i; i5++) {
                if (fArr3[i5] == 0.0f) {
                    fArr4[i5] = 0.0f;
                    fArr4[i5 + 1] = 0.0f;
                } else {
                    float f2 = fArr4[i5] / fArr3[i5];
                    int i6 = i5 + 1;
                    float f3 = fArr4[i6] / fArr3[i5];
                    if (f2 < 0.0f || f3 < 0.0f) {
                        throw new IllegalArgumentException("The control points must have monotonic Y values.");
                    }
                    float fHypot = (float) Math.hypot(f2, f3);
                    if (fHypot > 3.0f) {
                        float f4 = 3.0f / fHypot;
                        fArr4[i5] = fArr4[i5] * f4;
                        fArr4[i6] = fArr4[i6] * f4;
                    }
                }
            }
            this.mX = fArr;
            this.mY = fArr2;
            this.mM = fArr4;
        }

        @Override
        public float interpolate(float f) {
            int length = this.mX.length;
            if (Float.isNaN(f)) {
                return f;
            }
            int i = 0;
            if (f <= this.mX[0]) {
                return this.mY[0];
            }
            int i2 = length - 1;
            if (f >= this.mX[i2]) {
                return this.mY[i2];
            }
            while (true) {
                int i3 = i + 1;
                if (f < this.mX[i3]) {
                    float f2 = this.mX[i3] - this.mX[i];
                    float f3 = (f - this.mX[i]) / f2;
                    float f4 = 2.0f * f3;
                    float f5 = (this.mY[i] * (1.0f + f4)) + (this.mM[i] * f2 * f3);
                    float f6 = 1.0f - f3;
                    return (f5 * f6 * f6) + (((this.mY[i3] * (3.0f - f4)) + (f2 * this.mM[i3] * (f3 - 1.0f))) * f3 * f3);
                }
                if (f != this.mX[i3]) {
                    i = i3;
                } else {
                    return this.mY[i3];
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int length = this.mX.length;
            sb.append("MonotoneCubicSpline{[");
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("(");
                sb.append(this.mX[i]);
                sb.append(", ");
                sb.append(this.mY[i]);
                sb.append(": ");
                sb.append(this.mM[i]);
                sb.append(")");
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    public static class LinearSpline extends Spline {
        private final float[] mM;
        private final float[] mX;
        private final float[] mY;

        public LinearSpline(float[] fArr, float[] fArr2) {
            if (fArr == null || fArr2 == null || fArr.length != fArr2.length || fArr.length < 2) {
                throw new IllegalArgumentException("There must be at least two control points and the arrays must be of equal length.");
            }
            int length = fArr.length - 1;
            this.mM = new float[length];
            int i = 0;
            while (i < length) {
                int i2 = i + 1;
                this.mM[i] = (fArr2[i2] - fArr2[i]) / (fArr[i2] - fArr[i]);
                i = i2;
            }
            this.mX = fArr;
            this.mY = fArr2;
        }

        @Override
        public float interpolate(float f) {
            int length = this.mX.length;
            if (Float.isNaN(f)) {
                return f;
            }
            int i = 0;
            if (f <= this.mX[0]) {
                return this.mY[0];
            }
            int i2 = length - 1;
            if (f >= this.mX[i2]) {
                return this.mY[i2];
            }
            while (true) {
                int i3 = i + 1;
                if (f < this.mX[i3]) {
                    return this.mY[i] + (this.mM[i] * (f - this.mX[i]));
                }
                if (f != this.mX[i3]) {
                    i = i3;
                } else {
                    return this.mY[i3];
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            int length = this.mX.length;
            sb.append("LinearSpline{[");
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("(");
                sb.append(this.mX[i]);
                sb.append(", ");
                sb.append(this.mY[i]);
                if (i < length - 1) {
                    sb.append(": ");
                    sb.append(this.mM[i]);
                }
                sb.append(")");
            }
            sb.append("]}");
            return sb.toString();
        }
    }
}
