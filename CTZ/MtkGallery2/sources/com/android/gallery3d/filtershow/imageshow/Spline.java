package com.android.gallery3d.filtershow.imageshow;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Vector;

public class Spline {
    private static Drawable mCurveHandle;
    private static int mCurveHandleSize;
    private static int mCurveWidth;
    private final Paint gPaint;
    private ControlPoint mCurrentControlPoint;
    private final Vector<ControlPoint> mPoints;

    public Spline() {
        this.gPaint = new Paint();
        this.mCurrentControlPoint = null;
        this.mPoints = new Vector<>();
    }

    public Spline(Spline spline) {
        this.gPaint = new Paint();
        this.mCurrentControlPoint = null;
        this.mPoints = new Vector<>();
        for (int i = 0; i < spline.mPoints.size(); i++) {
            ControlPoint controlPointElementAt = spline.mPoints.elementAt(i);
            ControlPoint controlPoint = new ControlPoint(controlPointElementAt);
            this.mPoints.add(controlPoint);
            if (spline.mCurrentControlPoint == controlPointElementAt) {
                this.mCurrentControlPoint = controlPoint;
            }
        }
        Collections.sort(this.mPoints);
    }

    public static void setCurveHandle(Drawable drawable, int i) {
        mCurveHandle = drawable;
        mCurveHandleSize = i;
    }

    public static void setCurveWidth(int i) {
        mCurveWidth = i;
    }

    public static int curveHandleSize() {
        return mCurveHandleSize;
    }

    public static int colorForCurve(int i) {
        switch (i) {
            case 1:
                return -65536;
            case 2:
                return -16711936;
            case 3:
                return -16776961;
            default:
                return -1;
        }
    }

    public boolean sameValues(Spline spline) {
        if (this == spline) {
            return true;
        }
        if (spline == null || getNbPoints() != spline.getNbPoints()) {
            return false;
        }
        for (int i = 0; i < getNbPoints(); i++) {
            if (!this.mPoints.elementAt(i).sameValues(spline.mPoints.elementAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void didMovePoint(ControlPoint controlPoint) {
        this.mCurrentControlPoint = controlPoint;
    }

    public void movePoint(int i, float f, float f2) {
        if (i < 0 || i > this.mPoints.size() - 1) {
            return;
        }
        ControlPoint controlPointElementAt = this.mPoints.elementAt(i);
        controlPointElementAt.x = f;
        controlPointElementAt.y = f2;
        didMovePoint(controlPointElementAt);
    }

    public boolean isOriginal() {
        return getNbPoints() == 2 && this.mPoints.elementAt(0).x == 0.0f && this.mPoints.elementAt(0).y == 1.0f && this.mPoints.elementAt(1).x == 1.0f && this.mPoints.elementAt(1).y == 0.0f;
    }

    public void reset() {
        this.mPoints.clear();
        addPoint(0.0f, 1.0f);
        addPoint(1.0f, 0.0f);
    }

    private void drawHandles(Canvas canvas, Drawable drawable, float f, float f2) {
        int i = ((int) f) - (mCurveHandleSize / 2);
        int i2 = ((int) f2) - (mCurveHandleSize / 2);
        drawable.setBounds(i, i2, mCurveHandleSize + i, mCurveHandleSize + i2);
        drawable.draw(canvas);
    }

    public float[] getAppliedCurve() {
        int i;
        int i2;
        float[] fArr;
        int i3;
        char c;
        float[] fArr2 = new float[256];
        ControlPoint[] controlPointArr = new ControlPoint[this.mPoints.size()];
        int i4 = 0;
        for (int i5 = 0; i5 < this.mPoints.size(); i5++) {
            ControlPoint controlPoint = this.mPoints.get(i5);
            controlPointArr[i5] = new ControlPoint(controlPoint.x, controlPoint.y);
        }
        double[] dArrSolveSystem = solveSystem(controlPointArr);
        if (controlPointArr[0].x != 0.0f) {
            i = (int) (controlPointArr[0].x * 256.0f);
        } else {
            i = 0;
        }
        if (controlPointArr[controlPointArr.length - 1].x != 1.0f) {
            i2 = (int) (controlPointArr[controlPointArr.length - 1].x * 256.0f);
        } else {
            i2 = 256;
        }
        for (int i6 = 0; i6 < i; i6++) {
            fArr2[i6] = 1.0f - controlPointArr[0].y;
        }
        for (int i7 = i2; i7 < 256; i7++) {
            fArr2[i7] = 1.0f - controlPointArr[controlPointArr.length - 1].y;
        }
        while (i < i2) {
            double d = ((double) i) / 256.0d;
            int i8 = i4;
            int i9 = i8;
            while (i8 < controlPointArr.length - 1) {
                if (d >= controlPointArr[i8].x && d <= controlPointArr[i8 + 1].x) {
                    i9 = i8;
                }
                i8++;
            }
            ControlPoint controlPoint2 = controlPointArr[i9];
            int i10 = i9 + 1;
            ControlPoint controlPoint3 = controlPointArr[i10];
            if (d <= controlPoint3.x) {
                double d2 = controlPoint2.x;
                i3 = i;
                double d3 = controlPoint3.x;
                double d4 = controlPoint2.y;
                fArr = fArr2;
                double d5 = controlPoint3.y;
                double d6 = d3 - d2;
                double d7 = (d - d2) / d6;
                double d8 = 1.0d - d7;
                double d9 = (d4 * d8) + (d5 * d7) + (((d6 * d6) / 6.0d) * (((((d8 * d8) * d8) - d8) * dArrSolveSystem[i9]) + ((((d7 * d7) * d7) - d7) * dArrSolveSystem[i10])));
                if (d9 > 1.0d) {
                    d9 = 1.0d;
                }
                if (d9 < 0.0d) {
                    d9 = 0.0d;
                }
                fArr[i3] = (float) (1.0d - d9);
                c = 0;
            } else {
                fArr = fArr2;
                i3 = i;
                c = 0;
                fArr[i3] = 1.0f - controlPoint3.y;
            }
            i = i3 + 1;
            fArr2 = fArr;
            i4 = 0;
        }
        return fArr2;
    }

    private void drawGrid(Canvas canvas, float f, float f2) {
        this.gPaint.setARGB(128, 150, 150, 150);
        this.gPaint.setStrokeWidth(1.0f);
        this.gPaint.setARGB(255, 100, 100, 100);
        this.gPaint.setStrokeWidth(2.0f);
        canvas.drawLine(0.0f, f2, f, 0.0f, this.gPaint);
        this.gPaint.setARGB(128, 200, 200, 200);
        this.gPaint.setStrokeWidth(4.0f);
        float f3 = f2 / 3.0f;
        float f4 = f / 3.0f;
        for (int i = 1; i < 3; i++) {
            float f5 = i;
            float f6 = f5 * f3;
            canvas.drawLine(0.0f, f6, f, f6, this.gPaint);
            float f7 = f5 * f4;
            canvas.drawLine(f7, 0.0f, f7, f2, this.gPaint);
        }
        canvas.drawLine(0.0f, 0.0f, 0.0f, f2, this.gPaint);
        canvas.drawLine(f, 0.0f, f, f2, this.gPaint);
        canvas.drawLine(0.0f, 0.0f, f, 0.0f, this.gPaint);
        canvas.drawLine(0.0f, f2, f, f2, this.gPaint);
    }

    public void draw(Canvas canvas, int i, int i2, int i3, boolean z, boolean z2) {
        int i4;
        float f = i2 - mCurveHandleSize;
        float f2 = i3 - mCurveHandleSize;
        float f3 = mCurveHandleSize / 2;
        float f4 = mCurveHandleSize / 2;
        ControlPoint[] controlPointArr = new ControlPoint[this.mPoints.size()];
        for (int i5 = 0; i5 < this.mPoints.size(); i5++) {
            ControlPoint controlPoint = this.mPoints.get(i5);
            controlPointArr[i5] = new ControlPoint(controlPoint.x * f, controlPoint.y * f2);
        }
        double[] dArrSolveSystem = solveSystem(controlPointArr);
        Path path = new Path();
        path.moveTo(0.0f, controlPointArr[0].y);
        int i6 = 0;
        while (i6 < controlPointArr.length - 1) {
            double d = controlPointArr[i6].x;
            int i7 = i6 + 1;
            double d2 = controlPointArr[i7].x;
            float f5 = f;
            double d3 = controlPointArr[i6].y;
            ControlPoint[] controlPointArr2 = controlPointArr;
            double d4 = controlPointArr[i7].y;
            float f6 = f3;
            float f7 = f4;
            double d5 = d;
            while (d5 < d2) {
                double d6 = d2 - d;
                double d7 = d6 * d6;
                double d8 = (d5 - d) / d6;
                double d9 = 1.0d - d8;
                double d10 = (d9 * d3) + (d8 * d4) + ((d7 / 6.0d) * (((((d9 * d9) * d9) - d9) * dArrSolveSystem[i6]) + ((((d8 * d8) * d8) - d8) * dArrSolveSystem[i7])));
                double d11 = d3;
                double d12 = f2;
                if (d10 > d12) {
                    d10 = d12;
                }
                double d13 = 0.0d;
                if (d10 >= 0.0d) {
                    d13 = d10;
                }
                path.lineTo((float) d5, (float) d13);
                d5 += 20.0d;
                d3 = d11;
                dArrSolveSystem = dArrSolveSystem;
            }
            i6 = i7;
            f = f5;
            controlPointArr = controlPointArr2;
            f3 = f6;
            f4 = f7;
        }
        float f8 = f;
        ControlPoint[] controlPointArr3 = controlPointArr;
        canvas.save();
        canvas.translate(f3, f4);
        drawGrid(canvas, f8, f2);
        ControlPoint controlPoint2 = controlPointArr3[controlPointArr3.length - 1];
        path.lineTo(controlPoint2.x, controlPoint2.y);
        path.lineTo(f8, controlPoint2.y);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        int i8 = mCurveWidth;
        if (z) {
            i8 = (int) (((double) i8) * 1.5d);
        }
        int i9 = i8;
        paint.setStrokeWidth(i9 + 2);
        paint.setColor(-16777216);
        canvas.drawPath(path, paint);
        if (z2 && this.mCurrentControlPoint != null) {
            float f9 = this.mCurrentControlPoint.x * f8;
            float f10 = this.mCurrentControlPoint.y * f2;
            paint.setStrokeWidth(3.0f);
            paint.setColor(-16777216);
            canvas.drawLine(f9, f10, f9, f2, paint);
            canvas.drawLine(0.0f, f10, f9, f10, paint);
            paint.setStrokeWidth(1.0f);
            paint.setColor(i);
            i4 = i;
            canvas.drawLine(f9, f10, f9, f2, paint);
            canvas.drawLine(0.0f, f10, f9, f10, paint);
        } else {
            i4 = i;
        }
        paint.setStrokeWidth(i9);
        paint.setColor(i4);
        canvas.drawPath(path, paint);
        if (z) {
            for (int i10 = 0; i10 < controlPointArr3.length; i10++) {
                drawHandles(canvas, mCurveHandle, controlPointArr3[i10].x, controlPointArr3[i10].y);
            }
        }
        canvas.restore();
    }

    double[] solveSystem(ControlPoint[] controlPointArr) {
        int length = controlPointArr.length;
        double[][] dArr = (double[][]) Array.newInstance((Class<?>) double.class, length, 3);
        double[] dArr2 = new double[length];
        double[] dArr3 = new double[length];
        dArr[0][1] = 1.0d;
        int i = length - 1;
        dArr[i][1] = 1.0d;
        int i2 = 1;
        while (i2 < i) {
            int i3 = i2 - 1;
            double d = controlPointArr[i2].x - controlPointArr[i3].x;
            int i4 = i2 + 1;
            double d2 = controlPointArr[i4].x - controlPointArr[i3].x;
            double d3 = controlPointArr[i4].x - controlPointArr[i2].x;
            double[] dArr4 = dArr3;
            double d4 = controlPointArr[i4].y - controlPointArr[i2].y;
            double d5 = controlPointArr[i2].y - controlPointArr[i3].y;
            dArr[i2][0] = 0.16666666666666666d * d;
            dArr[i2][1] = 0.3333333333333333d * d2;
            dArr[i2][2] = 0.16666666666666666d * d3;
            dArr2[i2] = (d4 / d3) - (d5 / d);
            i2 = i4;
            dArr3 = dArr4;
        }
        double[] dArr5 = dArr3;
        for (int i5 = 1; i5 < length; i5++) {
            int i6 = i5 - 1;
            double d6 = dArr[i5][0] / dArr[i6][1];
            dArr[i5][1] = dArr[i5][1] - (dArr[i6][2] * d6);
            dArr2[i5] = dArr2[i5] - (d6 * dArr2[i6]);
        }
        dArr5[i] = dArr2[i] / dArr[i][1];
        for (int i7 = length - 2; i7 >= 0; i7--) {
            dArr5[i7] = (dArr2[i7] - (dArr[i7][2] * dArr5[i7 + 1])) / dArr[i7][1];
        }
        return dArr5;
    }

    public int addPoint(float f, float f2) {
        return addPoint(new ControlPoint(f, f2));
    }

    public int addPoint(ControlPoint controlPoint) {
        this.mPoints.add(controlPoint);
        Collections.sort(this.mPoints);
        return this.mPoints.indexOf(controlPoint);
    }

    public void deletePoint(int i) {
        this.mPoints.remove(i);
        if (this.mPoints.size() < 2) {
            reset();
        }
        Collections.sort(this.mPoints);
    }

    public int getNbPoints() {
        return this.mPoints.size();
    }

    public ControlPoint getPoint(int i) {
        return this.mPoints.elementAt(i);
    }

    public boolean isPointContained(float f, int i) {
        for (int i2 = 0; i2 < i; i2++) {
            if (this.mPoints.elementAt(i2).x > f) {
                return false;
            }
        }
        for (int i3 = i + 1; i3 < this.mPoints.size(); i3++) {
            if (this.mPoints.elementAt(i3).x < f) {
                return false;
            }
        }
        return true;
    }
}
