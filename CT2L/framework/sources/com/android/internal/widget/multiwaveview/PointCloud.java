package com.android.internal.widget.multiwaveview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.FloatMath;
import android.util.Log;
import java.util.ArrayList;

public class PointCloud {
    private static final int INNER_POINTS = 8;
    private static final float MAX_POINT_SIZE = 4.0f;
    private static final float MIN_POINT_SIZE = 2.0f;
    private static final float PI = 3.1415927f;
    private static final String TAG = "PointCloud";
    private float mCenterX;
    private float mCenterY;
    private Drawable mDrawable;
    private float mOuterRadius;
    private ArrayList<Point> mPointCloud = new ArrayList<>();
    private float mScale = 1.0f;
    WaveManager waveManager = new WaveManager();
    GlowManager glowManager = new GlowManager();
    private Paint mPaint = new Paint();

    public class WaveManager {
        private float radius = 50.0f;
        private float alpha = 0.0f;

        public WaveManager() {
        }

        public void setRadius(float r) {
            this.radius = r;
        }

        public float getRadius() {
            return this.radius;
        }

        public void setAlpha(float a) {
            this.alpha = a;
        }

        public float getAlpha() {
            return this.alpha;
        }
    }

    public class GlowManager {
        private float x;
        private float y;
        private float radius = 0.0f;
        private float alpha = 0.0f;

        public GlowManager() {
        }

        public void setX(float x1) {
            this.x = x1;
        }

        public float getX() {
            return this.x;
        }

        public void setY(float y1) {
            this.y = y1;
        }

        public float getY() {
            return this.y;
        }

        public void setAlpha(float a) {
            this.alpha = a;
        }

        public float getAlpha() {
            return this.alpha;
        }

        public void setRadius(float r) {
            this.radius = r;
        }

        public float getRadius() {
            return this.radius;
        }
    }

    class Point {
        float radius;
        float x;
        float y;

        public Point(float x2, float y2, float r) {
            this.x = x2;
            this.y = y2;
            this.radius = r;
        }
    }

    public PointCloud(Drawable drawable) {
        this.mPaint.setFilterBitmap(true);
        this.mPaint.setColor(Color.rgb(255, 255, 255));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setDither(true);
        this.mDrawable = drawable;
        if (this.mDrawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
    }

    public void setCenter(float x, float y) {
        this.mCenterX = x;
        this.mCenterY = y;
    }

    public void makePointCloud(float innerRadius, float outerRadius) {
        if (innerRadius == 0.0f) {
            Log.w(TAG, "Must specify an inner radius");
            return;
        }
        this.mOuterRadius = outerRadius;
        this.mPointCloud.clear();
        float pointAreaRadius = outerRadius - innerRadius;
        float ds = (6.2831855f * innerRadius) / 8.0f;
        int bands = Math.round(pointAreaRadius / ds);
        float dr = pointAreaRadius / bands;
        float r = innerRadius;
        int b = 0;
        while (b <= bands) {
            float circumference = 6.2831855f * r;
            int pointsInBand = (int) (circumference / ds);
            float eta = 1.5707964f;
            float dEta = 6.2831855f / pointsInBand;
            for (int i = 0; i < pointsInBand; i++) {
                float x = r * FloatMath.cos(eta);
                float y = r * FloatMath.sin(eta);
                eta += dEta;
                this.mPointCloud.add(new Point(x, y, r));
            }
            b++;
            r += dr;
        }
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public float getScale() {
        return this.mScale;
    }

    private static float hypot(float x, float y) {
        return FloatMath.sqrt((x * x) + (y * y));
    }

    private static float max(float a, float b) {
        return a > b ? a : b;
    }

    public int getAlphaForPoint(Point point) {
        float glowDistance = hypot(this.glowManager.x - point.x, this.glowManager.y - point.y);
        float glowAlpha = 0.0f;
        if (glowDistance < this.glowManager.radius) {
            float cosf = FloatMath.cos((0.7853982f * glowDistance) / this.glowManager.radius);
            glowAlpha = this.glowManager.alpha * max(0.0f, (float) Math.pow(cosf, 10.0d));
        }
        float radius = hypot(point.x, point.y);
        float waveAlpha = 0.0f;
        if (radius < this.waveManager.radius * MIN_POINT_SIZE) {
            float distanceToWaveRing = radius - this.waveManager.radius;
            float cosf2 = FloatMath.cos((1.5707964f * distanceToWaveRing) / this.waveManager.radius);
            waveAlpha = this.waveManager.alpha * max(0.0f, (float) Math.pow(cosf2, 6.0d));
        }
        return (int) (max(glowAlpha, waveAlpha) * 255.0f);
    }

    private float interp(float min, float max, float f) {
        return ((max - min) * f) + min;
    }

    public void draw(Canvas canvas) {
        ArrayList<Point> points = this.mPointCloud;
        canvas.save(1);
        canvas.scale(this.mScale, this.mScale, this.mCenterX, this.mCenterY);
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            float pointSize = interp(MAX_POINT_SIZE, MIN_POINT_SIZE, point.radius / this.mOuterRadius);
            float px = point.x + this.mCenterX;
            float py = point.y + this.mCenterY;
            int alpha = getAlphaForPoint(point);
            if (alpha != 0) {
                if (this.mDrawable != null) {
                    canvas.save(1);
                    float cx = this.mDrawable.getIntrinsicWidth() * 0.5f;
                    float cy = this.mDrawable.getIntrinsicHeight() * 0.5f;
                    float s = pointSize / MAX_POINT_SIZE;
                    canvas.scale(s, s, px, py);
                    canvas.translate(px - cx, py - cy);
                    this.mDrawable.setAlpha(alpha);
                    this.mDrawable.draw(canvas);
                    canvas.restore();
                } else {
                    this.mPaint.setAlpha(alpha);
                    canvas.drawCircle(px, py, pointSize, this.mPaint);
                }
            }
        }
        canvas.restore();
    }
}
