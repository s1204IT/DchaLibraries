package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.util.ArrayList;

public class PointCloud {
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
        private float width = 200.0f;
        private float alpha = 0.0f;

        public WaveManager() {
        }

        public void setRadius(float f) {
            this.radius = f;
        }

        public void setAlpha(float f) {
            this.alpha = f;
        }
    }

    public class GlowManager {
        private float x;
        private float y;
        private float radius = 0.0f;
        private float alpha = 0.0f;

        public GlowManager() {
        }

        public void setX(float f) {
            this.x = f;
        }

        public void setY(float f) {
            this.y = f;
        }

        public void setRadius(float f) {
            this.radius = f;
        }
    }

    class Point {
        float radius;
        float x;
        float y;

        public Point(float f, float f2, float f3) {
            this.x = f;
            this.y = f2;
            this.radius = f3;
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

    public void setCenter(float f, float f2) {
        this.mCenterX = f;
        this.mCenterY = f2;
    }

    public void makePointCloud(float f, float f2) {
        if (f == 0.0f) {
            Log.w("PointCloud", "Must specify an inner radius");
            return;
        }
        this.mOuterRadius = f2;
        this.mPointCloud.clear();
        float f3 = f2 - f;
        float f4 = (6.2831855f * f) / 8.0f;
        int iRound = Math.round(f3 / f4);
        float f5 = f3 / iRound;
        float f6 = f;
        int i = 0;
        while (i <= iRound) {
            int i2 = (int) ((6.2831855f * f6) / f4);
            float f7 = 6.2831855f / i2;
            float f8 = 1.5707964f;
            for (int i3 = 0; i3 < i2; i3++) {
                double d = f8;
                f8 += f7;
                this.mPointCloud.add(new Point(((float) Math.cos(d)) * f6, ((float) Math.sin(d)) * f6, f6));
            }
            i++;
            f6 += f5;
        }
    }

    public void setScale(float f) {
        this.mScale = f;
    }

    private static float hypot(float f, float f2) {
        return (float) Math.hypot(f, f2);
    }

    private static float max(float f, float f2) {
        return f > f2 ? f : f2;
    }

    public int getAlphaForPoint(Point point) {
        float fMax;
        float fHypot = hypot(this.glowManager.x - point.x, this.glowManager.y - point.y);
        float fMax2 = 0.0f;
        if (fHypot < this.glowManager.radius) {
            fMax = max(0.0f, (float) Math.pow(Math.cos((((double) fHypot) * 0.7853981633974483d) / ((double) this.glowManager.radius)), 10.0d)) * this.glowManager.alpha;
        } else {
            fMax = 0.0f;
        }
        float fHypot2 = hypot(point.x, point.y) - this.waveManager.radius;
        if (fHypot2 < this.waveManager.width * 0.5f && fHypot2 < 0.0f) {
            fMax2 = this.waveManager.alpha * max(0.0f, (float) Math.pow(Math.cos((0.7853981633974483d * ((double) fHypot2)) / ((double) this.waveManager.width)), 20.0d));
        }
        return (int) (max(fMax, fMax2) * 255.0f);
    }

    private float interp(float f, float f2, float f3) {
        return f + ((f2 - f) * f3);
    }

    public void draw(Canvas canvas) {
        ArrayList<Point> arrayList = this.mPointCloud;
        canvas.save(1);
        canvas.scale(this.mScale, this.mScale, this.mCenterX, this.mCenterY);
        for (int i = 0; i < arrayList.size(); i++) {
            Point point = arrayList.get(i);
            float fInterp = interp(4.0f, 2.0f, point.radius / this.mOuterRadius);
            float f = point.x + this.mCenterX;
            float f2 = point.y + this.mCenterY;
            int alphaForPoint = getAlphaForPoint(point);
            if (alphaForPoint != 0) {
                if (this.mDrawable != null) {
                    canvas.save(1);
                    float f3 = fInterp / 4.0f;
                    canvas.scale(f3, f3, f, f2);
                    canvas.translate(f - (this.mDrawable.getIntrinsicWidth() * 0.5f), f2 - (this.mDrawable.getIntrinsicHeight() * 0.5f));
                    this.mDrawable.setAlpha(alphaForPoint);
                    this.mDrawable.draw(canvas);
                    canvas.restore();
                } else {
                    this.mPaint.setAlpha(alphaForPoint);
                    canvas.drawCircle(f, f2, fInterp, this.mPaint);
                }
            }
        }
        canvas.restore();
    }
}
