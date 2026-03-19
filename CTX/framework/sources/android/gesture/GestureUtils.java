package android.gesture;

import android.graphics.RectF;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public final class GestureUtils {
    private static final float NONUNIFORM_SCALE = (float) Math.sqrt(2.0d);
    private static final float SCALING_THRESHOLD = 0.26f;

    private GestureUtils() {
    }

    static void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.e(GestureConstants.LOG_TAG, "Could not close stream", e);
            }
        }
    }

    public static float[] spatialSampling(Gesture gesture, int i) {
        return spatialSampling(gesture, i, false);
    }

    public static float[] spatialSampling(Gesture gesture, int i, boolean z) {
        int size;
        int i2;
        float f;
        float f2;
        float f3;
        float f4;
        float f5;
        float f6;
        float f7;
        float f8;
        float f9 = i - 1;
        float[] fArr = new float[i * i];
        float f10 = 0.0f;
        Arrays.fill(fArr, 0.0f);
        RectF boundingBox = gesture.getBoundingBox();
        float fWidth = boundingBox.width();
        float fHeight = boundingBox.height();
        float f11 = f9 / fWidth;
        float f12 = f9 / fHeight;
        if (!z) {
            float f13 = fWidth / fHeight;
            if (f13 > 1.0f) {
                f13 = 1.0f / f13;
            }
            if (f13 >= SCALING_THRESHOLD) {
                if (f11 > f12) {
                    float f14 = NONUNIFORM_SCALE * f12;
                    if (f14 < f11) {
                        f11 = f14;
                    }
                } else {
                    float f15 = NONUNIFORM_SCALE * f11;
                    if (f15 < f12) {
                        f12 = f15;
                    }
                }
                float f16 = -boundingBox.centerX();
                float f17 = -boundingBox.centerY();
                float f18 = f9 / 2.0f;
                ArrayList<GestureStroke> strokes = gesture.getStrokes();
                size = strokes.size();
                i2 = 0;
                while (i2 < size) {
                    float[] fArr2 = strokes.get(i2).points;
                    int length = fArr2.length;
                    float[] fArr3 = new float[length];
                    for (int i3 = 0; i3 < length; i3 += 2) {
                        fArr3[i3] = ((fArr2[i3] + f16) * f11) + f18;
                        int i4 = i3 + 1;
                        fArr3[i4] = ((fArr2[i4] + f17) * f12) + f18;
                    }
                    float f19 = -1.0f;
                    int i5 = 0;
                    float f20 = -1.0f;
                    while (i5 < length) {
                        if (fArr3[i5] >= f10) {
                            f = fArr3[i5];
                        } else {
                            f = f10;
                        }
                        int i6 = i5 + 1;
                        if (fArr3[i6] >= f10) {
                            f2 = fArr3[i6];
                        } else {
                            f2 = f10;
                        }
                        float f21 = f > f9 ? f9 : f;
                        if (f2 > f9) {
                            f3 = f9;
                        } else {
                            f3 = f9;
                            f9 = f2;
                        }
                        plot(f21, f9, fArr, i);
                        if (f19 != -1.0f) {
                            if (f19 > f21) {
                                f4 = f17;
                                f5 = f16;
                                float fCeil = (float) Math.ceil(f21);
                                f8 = f20;
                                float f22 = (f8 - f9) / (f19 - f21);
                                while (fCeil < f19) {
                                    plot(fCeil, ((fCeil - f21) * f22) + f9, fArr, i);
                                    fCeil += 1.0f;
                                    f18 = f18;
                                }
                                f6 = f18;
                            } else {
                                f4 = f17;
                                f5 = f16;
                                f6 = f18;
                                f8 = f20;
                                if (f19 < f21) {
                                    f7 = f11;
                                    float f23 = (f8 - f9) / (f19 - f21);
                                    for (float fCeil2 = (float) Math.ceil(f19); fCeil2 < f21; fCeil2 += 1.0f) {
                                        plot(fCeil2, ((fCeil2 - f21) * f23) + f9, fArr, i);
                                    }
                                }
                                if (f8 <= f9) {
                                    float f24 = (f19 - f21) / (f8 - f9);
                                    for (float fCeil3 = (float) Math.ceil(f9); fCeil3 < f8; fCeil3 += 1.0f) {
                                        plot(((fCeil3 - f9) * f24) + f21, fCeil3, fArr, i);
                                    }
                                } else if (f8 < f9) {
                                    float f25 = (f19 - f21) / (f8 - f9);
                                    for (float fCeil4 = (float) Math.ceil(f8); fCeil4 < f9; fCeil4 += 1.0f) {
                                        plot(((fCeil4 - f9) * f25) + f21, fCeil4, fArr, i);
                                    }
                                }
                            }
                            f7 = f11;
                            if (f8 <= f9) {
                            }
                        } else {
                            f4 = f17;
                            f5 = f16;
                            f6 = f18;
                            f7 = f11;
                        }
                        i5 += 2;
                        f20 = f9;
                        f19 = f21;
                        f9 = f3;
                        f16 = f5;
                        f17 = f4;
                        f18 = f6;
                        f11 = f7;
                        f10 = 0.0f;
                    }
                    i2++;
                    f16 = f16;
                    f10 = 0.0f;
                }
                return fArr;
            }
            if (f11 >= f12) {
                f11 = f12;
            }
        } else if (f11 >= f12) {
            f11 = f12;
        }
        f12 = f11;
        float f162 = -boundingBox.centerX();
        float f172 = -boundingBox.centerY();
        float f182 = f9 / 2.0f;
        ArrayList<GestureStroke> strokes2 = gesture.getStrokes();
        size = strokes2.size();
        i2 = 0;
        while (i2 < size) {
        }
        return fArr;
    }

    private static void plot(float f, float f2, float[] fArr, int i) {
        if (f < 0.0f) {
            f = 0.0f;
        }
        if (f2 < 0.0f) {
            f2 = 0.0f;
        }
        double d = f;
        int iFloor = (int) Math.floor(d);
        int iCeil = (int) Math.ceil(d);
        double d2 = f2;
        int iFloor2 = (int) Math.floor(d2);
        int iCeil2 = (int) Math.ceil(d2);
        if (f == iFloor && f2 == iFloor2) {
            int i2 = (iCeil2 * i) + iCeil;
            if (fArr[i2] < 1.0f) {
                fArr[i2] = 1.0f;
                return;
            }
            return;
        }
        double dPow = Math.pow(r4 - f, 2.0d);
        double dPow2 = Math.pow(iFloor2 - f2, 2.0d);
        double dPow3 = Math.pow(iCeil - f, 2.0d);
        double dPow4 = Math.pow(iCeil2 - f2, 2.0d);
        float fSqrt = (float) Math.sqrt(dPow + dPow2);
        float fSqrt2 = (float) Math.sqrt(dPow2 + dPow3);
        float fSqrt3 = (float) Math.sqrt(dPow + dPow4);
        float fSqrt4 = (float) Math.sqrt(dPow3 + dPow4);
        float f3 = fSqrt + fSqrt2 + fSqrt3 + fSqrt4;
        float f4 = fSqrt / f3;
        int i3 = iFloor2 * i;
        int i4 = i3 + iFloor;
        if (f4 > fArr[i4]) {
            fArr[i4] = f4;
        }
        float f5 = fSqrt2 / f3;
        int i5 = i3 + iCeil;
        if (f5 > fArr[i5]) {
            fArr[i5] = f5;
        }
        float f6 = fSqrt3 / f3;
        int i6 = iCeil2 * i;
        int i7 = iFloor + i6;
        if (f6 > fArr[i7]) {
            fArr[i7] = f6;
        }
        float f7 = fSqrt4 / f3;
        int i8 = i6 + iCeil;
        if (f7 > fArr[i8]) {
            fArr[i8] = f7;
        }
    }

    public static float[] temporalSampling(GestureStroke gestureStroke, int i) {
        float f;
        float f2;
        float f3 = gestureStroke.length / (i - 1);
        int i2 = i * 2;
        float[] fArr = new float[i2];
        float[] fArr2 = gestureStroke.points;
        int i3 = 0;
        float f4 = fArr2[0];
        int i4 = 1;
        float f5 = fArr2[1];
        fArr[0] = f4;
        fArr[1] = f5;
        int length = fArr2.length / 2;
        float f6 = Float.MIN_VALUE;
        float f7 = f4;
        float f8 = f5;
        float f9 = Float.MIN_VALUE;
        float f10 = 0.0f;
        int i5 = 2;
        float f11 = Float.MIN_VALUE;
        while (i3 < length) {
            if (f11 == f6) {
                i3++;
                if (i3 >= length) {
                    break;
                }
                int i6 = i3 * 2;
                f = fArr2[i6];
                f2 = fArr2[i6 + i4];
            } else {
                float f12 = f9;
                f = f11;
                f2 = f12;
            }
            float f13 = f - f7;
            float f14 = f2 - f8;
            float f15 = f7;
            float f16 = f10;
            float fHypot = (float) Math.hypot(f13, f14);
            f10 = f16 + fHypot;
            if (f10 >= f3) {
                float f17 = (f3 - f16) / fHypot;
                f7 = f15 + (f13 * f17);
                f8 += f17 * f14;
                fArr[i5] = f7;
                int i7 = i5 + 1;
                fArr[i7] = f8;
                i4 = 1;
                i5 = i7 + 1;
                f10 = 0.0f;
                float f18 = f;
                f9 = f2;
                f11 = f18;
            } else {
                i4 = 1;
                f8 = f2;
                f7 = f;
                f11 = Float.MIN_VALUE;
                f9 = Float.MIN_VALUE;
            }
            f6 = Float.MIN_VALUE;
        }
        float f19 = f7;
        while (i5 < i2) {
            fArr[i5] = f19;
            fArr[i5 + 1] = f8;
            i5 += 2;
        }
        return fArr;
    }

    static float[] computeCentroid(float[] fArr) {
        int length = fArr.length;
        float f = 0.0f;
        float f2 = 0.0f;
        int i = 0;
        while (i < length) {
            f += fArr[i];
            int i2 = i + 1;
            f2 += fArr[i2];
            i = i2 + 1;
        }
        float f3 = length;
        return new float[]{(f * 2.0f) / f3, (2.0f * f2) / f3};
    }

    private static float[][] computeCoVariance(float[] fArr) {
        float[][] fArr2 = (float[][]) Array.newInstance((Class<?>) float.class, 2, 2);
        fArr2[0][0] = 0.0f;
        fArr2[0][1] = 0.0f;
        fArr2[1][0] = 0.0f;
        fArr2[1][1] = 0.0f;
        int length = fArr.length;
        int i = 0;
        while (i < length) {
            float f = fArr[i];
            int i2 = i + 1;
            float f2 = fArr[i2];
            float[] fArr3 = fArr2[0];
            fArr3[0] = fArr3[0] + (f * f);
            float[] fArr4 = fArr2[0];
            fArr4[1] = fArr4[1] + (f * f2);
            fArr2[1][0] = fArr2[0][1];
            float[] fArr5 = fArr2[1];
            fArr5[1] = fArr5[1] + (f2 * f2);
            i = i2 + 1;
        }
        float[] fArr6 = fArr2[0];
        float f3 = length / 2;
        fArr6[0] = fArr6[0] / f3;
        float[] fArr7 = fArr2[0];
        fArr7[1] = fArr7[1] / f3;
        float[] fArr8 = fArr2[1];
        fArr8[0] = fArr8[0] / f3;
        float[] fArr9 = fArr2[1];
        fArr9[1] = fArr9[1] / f3;
        return fArr2;
    }

    static float computeTotalLength(float[] fArr) {
        int length = fArr.length - 4;
        float fHypot = 0.0f;
        for (int i = 0; i < length; i += 2) {
            fHypot = (float) (((double) fHypot) + Math.hypot(fArr[r3] - fArr[i], fArr[i + 3] - fArr[i + 1]));
        }
        return fHypot;
    }

    static float computeStraightness(float[] fArr) {
        return ((float) Math.hypot(fArr[2] - fArr[0], fArr[3] - fArr[1])) / computeTotalLength(fArr);
    }

    static float computeStraightness(float[] fArr, float f) {
        return ((float) Math.hypot(fArr[2] - fArr[0], fArr[3] - fArr[1])) / f;
    }

    static float squaredEuclideanDistance(float[] fArr, float[] fArr2) {
        int length = fArr.length;
        float f = 0.0f;
        for (int i = 0; i < length; i++) {
            float f2 = fArr[i] - fArr2[i];
            f += f2 * f2;
        }
        return f / length;
    }

    static float cosineDistance(float[] fArr, float[] fArr2) {
        int length = fArr.length;
        float f = 0.0f;
        for (int i = 0; i < length; i++) {
            f += fArr[i] * fArr2[i];
        }
        return (float) Math.acos(f);
    }

    static float minimumCosineDistance(float[] fArr, float[] fArr2, int i) {
        int length = fArr.length;
        float f = 0.0f;
        float f2 = 0.0f;
        for (int i2 = 0; i2 < length; i2 += 2) {
            int i3 = i2 + 1;
            f += (fArr[i2] * fArr2[i2]) + (fArr[i3] * fArr2[i3]);
            f2 += (fArr[i2] * fArr2[i3]) - (fArr[i3] * fArr2[i2]);
        }
        if (f != 0.0f) {
            double d = f2 / f;
            double dAtan = Math.atan(d);
            if (i > 2 && Math.abs(dAtan) >= 3.141592653589793d / ((double) i)) {
                return (float) Math.acos(f);
            }
            double dCos = Math.cos(dAtan);
            return (float) Math.acos((((double) f) * dCos) + (((double) f2) * d * dCos));
        }
        return 1.5707964f;
    }

    public static OrientedBoundingBox computeOrientedBoundingBox(ArrayList<GesturePoint> arrayList) {
        int size = arrayList.size();
        float[] fArr = new float[size * 2];
        for (int i = 0; i < size; i++) {
            GesturePoint gesturePoint = arrayList.get(i);
            int i2 = i * 2;
            fArr[i2] = gesturePoint.x;
            fArr[i2 + 1] = gesturePoint.y;
        }
        return computeOrientedBoundingBox(fArr, computeCentroid(fArr));
    }

    public static OrientedBoundingBox computeOrientedBoundingBox(float[] fArr) {
        int length = fArr.length;
        float[] fArr2 = new float[length];
        for (int i = 0; i < length; i++) {
            fArr2[i] = fArr[i];
        }
        return computeOrientedBoundingBox(fArr2, computeCentroid(fArr2));
    }

    private static OrientedBoundingBox computeOrientedBoundingBox(float[] fArr, float[] fArr2) {
        float fAtan2;
        translate(fArr, -fArr2[0], -fArr2[1]);
        float[] fArrComputeOrientation = computeOrientation(computeCoVariance(fArr));
        if (fArrComputeOrientation[0] != 0.0f || fArrComputeOrientation[1] != 0.0f) {
            fAtan2 = (float) Math.atan2(fArrComputeOrientation[1], fArrComputeOrientation[0]);
            rotate(fArr, -fAtan2);
        } else {
            fAtan2 = -1.5707964f;
        }
        int length = fArr.length;
        float f = Float.MAX_VALUE;
        float f2 = Float.MIN_VALUE;
        float f3 = Float.MIN_VALUE;
        float f4 = Float.MAX_VALUE;
        int i = 0;
        while (i < length) {
            if (fArr[i] < f) {
                f = fArr[i];
            }
            if (fArr[i] > f2) {
                f2 = fArr[i];
            }
            int i2 = i + 1;
            if (fArr[i2] < f4) {
                f4 = fArr[i2];
            }
            if (fArr[i2] > f3) {
                f3 = fArr[i2];
            }
            i = i2 + 1;
        }
        return new OrientedBoundingBox((float) (((double) (fAtan2 * 180.0f)) / 3.141592653589793d), fArr2[0], fArr2[1], f2 - f, f3 - f4);
    }

    private static float[] computeOrientation(float[][] fArr) {
        float[] fArr2 = new float[2];
        if (fArr[0][1] == 0.0f || fArr[1][0] == 0.0f) {
            fArr2[0] = 1.0f;
            fArr2[1] = 0.0f;
        }
        float f = ((-fArr[0][0]) - fArr[1][1]) / 2.0f;
        float fSqrt = (float) Math.sqrt(Math.pow(f, 2.0d) - ((double) ((fArr[0][0] * fArr[1][1]) - (fArr[0][1] * fArr[1][0]))));
        float f2 = -f;
        float f3 = f2 + fSqrt;
        float f4 = f2 - fSqrt;
        if (f3 == f4) {
            fArr2[0] = 0.0f;
            fArr2[1] = 0.0f;
        } else {
            if (f3 > f4) {
                f4 = f3;
            }
            fArr2[0] = 1.0f;
            fArr2[1] = (f4 - fArr[0][0]) / fArr[0][1];
        }
        return fArr2;
    }

    static float[] rotate(float[] fArr, float f) {
        double d = f;
        float fCos = (float) Math.cos(d);
        float fSin = (float) Math.sin(d);
        int length = fArr.length;
        for (int i = 0; i < length; i += 2) {
            int i2 = i + 1;
            float f2 = (fArr[i] * fCos) - (fArr[i2] * fSin);
            float f3 = (fArr[i] * fSin) + (fArr[i2] * fCos);
            fArr[i] = f2;
            fArr[i2] = f3;
        }
        return fArr;
    }

    static float[] translate(float[] fArr, float f, float f2) {
        int length = fArr.length;
        for (int i = 0; i < length; i += 2) {
            fArr[i] = fArr[i] + f;
            int i2 = i + 1;
            fArr[i2] = fArr[i2] + f2;
        }
        return fArr;
    }

    static float[] scale(float[] fArr, float f, float f2) {
        int length = fArr.length;
        for (int i = 0; i < length; i += 2) {
            fArr[i] = fArr[i] * f;
            int i2 = i + 1;
            fArr[i2] = fArr[i2] * f2;
        }
        return fArr;
    }
}
