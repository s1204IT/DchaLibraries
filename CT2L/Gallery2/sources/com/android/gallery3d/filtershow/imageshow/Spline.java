package com.android.gallery3d.filtershow.imageshow;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
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
            ControlPoint p = spline.mPoints.elementAt(i);
            ControlPoint newPoint = new ControlPoint(p);
            this.mPoints.add(newPoint);
            if (spline.mCurrentControlPoint == p) {
                this.mCurrentControlPoint = newPoint;
            }
        }
        Collections.sort(this.mPoints);
    }

    public static void setCurveHandle(Drawable drawable, int size) {
        mCurveHandle = drawable;
        mCurveHandleSize = size;
    }

    public static void setCurveWidth(int width) {
        mCurveWidth = width;
    }

    public static int curveHandleSize() {
        return mCurveHandleSize;
    }

    public static int colorForCurve(int curveIndex) {
        switch (curveIndex) {
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

    public boolean sameValues(Spline other) {
        if (this == other) {
            return true;
        }
        if (other != null && getNbPoints() == other.getNbPoints()) {
            for (int i = 0; i < getNbPoints(); i++) {
                ControlPoint p = this.mPoints.elementAt(i);
                ControlPoint otherPoint = other.mPoints.elementAt(i);
                if (!p.sameValues(otherPoint)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void didMovePoint(ControlPoint point) {
        this.mCurrentControlPoint = point;
    }

    public void movePoint(int pick, float x, float y) {
        if (pick >= 0 && pick <= this.mPoints.size() - 1) {
            ControlPoint point = this.mPoints.elementAt(pick);
            point.x = x;
            point.y = y;
            didMovePoint(point);
        }
    }

    public boolean isOriginal() {
        if (getNbPoints() != 2) {
            return false;
        }
        if (this.mPoints.elementAt(0).x == 0.0f && this.mPoints.elementAt(0).y == 1.0f) {
            return this.mPoints.elementAt(1).x == 1.0f && this.mPoints.elementAt(1).y == 0.0f;
        }
        return false;
    }

    public void reset() {
        this.mPoints.clear();
        addPoint(0.0f, 1.0f);
        addPoint(1.0f, 0.0f);
    }

    private void drawHandles(Canvas canvas, Drawable indicator, float centerX, float centerY) {
        int left = ((int) centerX) - (mCurveHandleSize / 2);
        int top = ((int) centerY) - (mCurveHandleSize / 2);
        indicator.setBounds(left, top, mCurveHandleSize + left, mCurveHandleSize + top);
        indicator.draw(canvas);
    }

    public float[] getAppliedCurve() {
        float[] curve = new float[NotificationCompat.FLAG_LOCAL_ONLY];
        ControlPoint[] points = new ControlPoint[this.mPoints.size()];
        for (int i = 0; i < this.mPoints.size(); i++) {
            ControlPoint p = this.mPoints.get(i);
            points[i] = new ControlPoint(p.x, p.y);
        }
        double[] derivatives = solveSystem(points);
        int start = 0;
        int end = NotificationCompat.FLAG_LOCAL_ONLY;
        if (points[0].x != 0.0f) {
            start = (int) (points[0].x * 256.0f);
        }
        if (points[points.length - 1].x != 1.0f) {
            end = (int) (points[points.length - 1].x * 256.0f);
        }
        for (int i2 = 0; i2 < start; i2++) {
            curve[i2] = 1.0f - points[0].y;
        }
        for (int i3 = end; i3 < 256; i3++) {
            curve[i3] = 1.0f - points[points.length - 1].y;
        }
        for (int i4 = start; i4 < end; i4++) {
            double x = ((double) i4) / 256.0d;
            int pivot = 0;
            for (int j = 0; j < points.length - 1; j++) {
                if (x >= points[j].x && x <= points[j + 1].x) {
                    pivot = j;
                }
            }
            ControlPoint cur = points[pivot];
            ControlPoint next = points[pivot + 1];
            if (x <= next.x) {
                double x1 = cur.x;
                double x2 = next.x;
                double y1 = cur.y;
                double y2 = next.y;
                double delta = x2 - x1;
                double delta2 = delta * delta;
                double b = (x - x1) / delta;
                double a = 1.0d - b;
                double ta = a * y1;
                double tb = b * y2;
                double tc = (((a * a) * a) - a) * derivatives[pivot];
                double td = (((b * b) * b) - b) * derivatives[pivot + 1];
                double y = ta + tb + ((delta2 / 6.0d) * (tc + td));
                if (y > 1.0d) {
                    y = 1.0d;
                }
                if (y < 0.0d) {
                    y = 0.0d;
                }
                curve[i4] = (float) (1.0d - y);
            } else {
                curve[i4] = 1.0f - next.y;
            }
        }
        return curve;
    }

    private void drawGrid(Canvas canvas, float w, float h) {
        this.gPaint.setARGB(128, 150, 150, 150);
        this.gPaint.setStrokeWidth(1.0f);
        float f = h / 9.0f;
        float f2 = w / 9.0f;
        this.gPaint.setARGB(255, 100, 100, 100);
        this.gPaint.setStrokeWidth(2.0f);
        canvas.drawLine(0.0f, h, w, 0.0f, this.gPaint);
        this.gPaint.setARGB(128, 200, 200, 200);
        this.gPaint.setStrokeWidth(4.0f);
        float stepH = h / 3.0f;
        float stepW = w / 3.0f;
        for (int j = 1; j < 3; j++) {
            canvas.drawLine(0.0f, j * stepH, w, j * stepH, this.gPaint);
            canvas.drawLine(j * stepW, 0.0f, j * stepW, h, this.gPaint);
        }
        canvas.drawLine(0.0f, 0.0f, 0.0f, h, this.gPaint);
        canvas.drawLine(w, 0.0f, w, h, this.gPaint);
        canvas.drawLine(0.0f, 0.0f, w, 0.0f, this.gPaint);
        canvas.drawLine(0.0f, h, w, h, this.gPaint);
    }

    public void draw(Canvas canvas, int color, int canvasWidth, int canvasHeight, boolean showHandles, boolean moving) {
        float w = canvasWidth - mCurveHandleSize;
        float h = canvasHeight - mCurveHandleSize;
        float dx = mCurveHandleSize / 2;
        float dy = mCurveHandleSize / 2;
        ControlPoint[] points = new ControlPoint[this.mPoints.size()];
        for (int i = 0; i < this.mPoints.size(); i++) {
            ControlPoint p = this.mPoints.get(i);
            points[i] = new ControlPoint(p.x * w, p.y * h);
        }
        double[] derivatives = solveSystem(points);
        Path path = new Path();
        path.moveTo(0.0f, points[0].y);
        for (int i2 = 0; i2 < points.length - 1; i2++) {
            double x1 = points[i2].x;
            double x2 = points[i2 + 1].x;
            double y1 = points[i2].y;
            double y2 = points[i2 + 1].y;
            for (double x = x1; x < x2; x += 20.0d) {
                double delta = x2 - x1;
                double delta2 = delta * delta;
                double b = (x - x1) / delta;
                double a = 1.0d - b;
                double ta = a * y1;
                double tb = b * y2;
                double tc = (((a * a) * a) - a) * derivatives[i2];
                double td = (((b * b) * b) - b) * derivatives[i2 + 1];
                double y = ta + tb + ((delta2 / 6.0d) * (tc + td));
                if (y > h) {
                    y = h;
                }
                if (y < 0.0d) {
                    y = 0.0d;
                }
                path.lineTo((float) x, (float) y);
            }
        }
        canvas.save();
        canvas.translate(dx, dy);
        drawGrid(canvas, w, h);
        ControlPoint lastPoint = points[points.length - 1];
        path.lineTo(lastPoint.x, lastPoint.y);
        path.lineTo(w, lastPoint.y);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        int curveWidth = mCurveWidth;
        if (showHandles) {
            curveWidth = (int) (((double) curveWidth) * 1.5d);
        }
        paint.setStrokeWidth(curveWidth + 2);
        paint.setColor(-16777216);
        canvas.drawPath(path, paint);
        if (moving && this.mCurrentControlPoint != null) {
            float px = this.mCurrentControlPoint.x * w;
            float py = this.mCurrentControlPoint.y * h;
            paint.setStrokeWidth(3.0f);
            paint.setColor(-16777216);
            canvas.drawLine(px, py, px, h, paint);
            canvas.drawLine(0.0f, py, px, py, paint);
            paint.setStrokeWidth(1.0f);
            paint.setColor(color);
            canvas.drawLine(px, py, px, h, paint);
            canvas.drawLine(0.0f, py, px, py, paint);
        }
        paint.setStrokeWidth(curveWidth);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        if (showHandles) {
            for (int i3 = 0; i3 < points.length; i3++) {
                float x3 = points[i3].x;
                drawHandles(canvas, mCurveHandle, x3, points[i3].y);
            }
        }
        canvas.restore();
    }

    double[] solveSystem(ControlPoint[] points) {
        int n = points.length;
        double[][] system = (double[][]) Array.newInstance((Class<?>) Double.TYPE, n, 3);
        double[] result = new double[n];
        double[] solution = new double[n];
        system[0][1] = 1.0d;
        system[n - 1][1] = 1.0d;
        for (int i = 1; i < n - 1; i++) {
            double deltaPrevX = points[i].x - points[i - 1].x;
            double deltaX = points[i + 1].x - points[i - 1].x;
            double deltaNextX = points[i + 1].x - points[i].x;
            double deltaNextY = points[i + 1].y - points[i].y;
            double deltaPrevY = points[i].y - points[i - 1].y;
            system[i][0] = 0.16666666666666666d * deltaPrevX;
            system[i][1] = 0.3333333333333333d * deltaX;
            system[i][2] = 0.16666666666666666d * deltaNextX;
            result[i] = (deltaNextY / deltaNextX) - (deltaPrevY / deltaPrevX);
        }
        for (int i2 = 1; i2 < n; i2++) {
            double m = system[i2][0] / system[i2 - 1][1];
            system[i2][1] = system[i2][1] - (system[i2 - 1][2] * m);
            result[i2] = result[i2] - (result[i2 - 1] * m);
        }
        solution[n - 1] = result[n - 1] / system[n - 1][1];
        for (int i3 = n - 2; i3 >= 0; i3--) {
            solution[i3] = (result[i3] - (system[i3][2] * solution[i3 + 1])) / system[i3][1];
        }
        return solution;
    }

    public int addPoint(float x, float y) {
        return addPoint(new ControlPoint(x, y));
    }

    public int addPoint(ControlPoint v) {
        this.mPoints.add(v);
        Collections.sort(this.mPoints);
        return this.mPoints.indexOf(v);
    }

    public void deletePoint(int n) {
        this.mPoints.remove(n);
        if (this.mPoints.size() < 2) {
            reset();
        }
        Collections.sort(this.mPoints);
    }

    public int getNbPoints() {
        return this.mPoints.size();
    }

    public ControlPoint getPoint(int n) {
        return this.mPoints.elementAt(n);
    }

    public boolean isPointContained(float x, int n) {
        for (int i = 0; i < n; i++) {
            ControlPoint point = this.mPoints.elementAt(i);
            if (point.x > x) {
                return false;
            }
        }
        for (int i2 = n + 1; i2 < this.mPoints.size(); i2++) {
            ControlPoint point2 = this.mPoints.elementAt(i2);
            if (point2.x < x) {
                return false;
            }
        }
        return true;
    }
}
