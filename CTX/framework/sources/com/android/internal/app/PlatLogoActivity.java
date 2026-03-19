package com.android.internal.app;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class PlatLogoActivity extends Activity {
    TimeAnimator anim;
    PBackground bg;
    FrameLayout layout;

    private class PBackground extends Drawable {
        private int darkest;
        private float dp;
        private float maxRadius;
        private float offset;
        private int[] palette;
        private float radius;
        private float x;
        private float y;

        public PBackground() {
            randomizePalette();
        }

        public void setRadius(float f) {
            this.radius = Math.max(48.0f * this.dp, f);
        }

        public void setPosition(float f, float f2) {
            this.x = f;
            this.y = f2;
        }

        public void setOffset(float f) {
            this.offset = f;
        }

        public float lum(int i) {
            return (((Color.red(i) * 299.0f) + (Color.green(i) * 587.0f)) + (Color.blue(i) * 114.0f)) / 1000.0f;
        }

        public void randomizePalette() {
            int iRandom = ((int) (Math.random() * 2.0d)) + 2;
            float[] fArr = {((float) Math.random()) * 360.0f, 1.0f, 1.0f};
            this.palette = new int[iRandom];
            this.darkest = 0;
            for (int i = 0; i < iRandom; i++) {
                this.palette[i] = Color.HSVToColor(fArr);
                fArr[0] = fArr[0] + (360.0f / iRandom);
                if (lum(this.palette[i]) < lum(this.palette[this.darkest])) {
                    this.darkest = i;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (int i2 : this.palette) {
                sb.append(String.format("#%08x ", Integer.valueOf(i2)));
            }
            Log.v("PlatLogoActivity", "color palette: " + ((Object) sb));
        }

        @Override
        public void draw(Canvas canvas) {
            if (this.dp == 0.0f) {
                this.dp = PlatLogoActivity.this.getResources().getDisplayMetrics().density;
            }
            float width = canvas.getWidth();
            float height = canvas.getHeight();
            float f = 2.0f;
            if (this.radius == 0.0f) {
                setPosition(width / 2.0f, height / 2.0f);
                setRadius(width / 6.0f);
            }
            float f2 = this.radius * 0.667f;
            Paint paint = new Paint();
            paint.setStrokeCap(Paint.Cap.BUTT);
            canvas.translate(this.x, this.y);
            Path path = new Path();
            path.moveTo(-this.radius, height);
            path.lineTo(-this.radius, 0.0f);
            path.arcTo(-this.radius, -this.radius, this.radius, this.radius, -180.0f, 270.0f, false);
            path.lineTo(-this.radius, this.radius);
            paint.setStyle(Paint.Style.FILL);
            float fMax = Math.max(canvas.getWidth(), canvas.getHeight()) * 1.414f;
            int i = 0;
            while (true) {
                float f3 = f2 * f;
                if (fMax > (this.radius * f) + f3) {
                    paint.setColor(this.palette[i % this.palette.length] | (-16777216));
                    float f4 = (-fMax) / f;
                    float f5 = fMax / f;
                    canvas.drawOval(f4, f4, f5, f5, paint);
                    fMax = (float) (((double) fMax) - (((double) f2) * (1.100000023841858d + Math.sin(((i / 20.0f) + this.offset) * 3.14159f))));
                    i++;
                    path = path;
                    height = height;
                    f = 2.0f;
                } else {
                    Path path2 = path;
                    paint.setColor(this.palette[(this.darkest + 1) % this.palette.length] | (-16777216));
                    canvas.drawOval(-this.radius, -this.radius, this.radius, this.radius, paint);
                    path2.reset();
                    path2.moveTo(-this.radius, height);
                    path2.lineTo(-this.radius, 0.0f);
                    path2.arcTo(-this.radius, -this.radius, this.radius, this.radius, -180.0f, 270.0f, false);
                    path2.lineTo((-this.radius) + f2, this.radius);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(f3);
                    paint.setColor(this.palette[this.darkest]);
                    canvas.drawPath(path2, paint);
                    paint.setStrokeWidth(f2);
                    paint.setColor(-1);
                    canvas.drawPath(path2, paint);
                    return;
                }
            }
        }

        @Override
        public void setAlpha(int i) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.layout = new FrameLayout(this);
        setContentView(this.layout);
        this.bg = new PBackground();
        this.layout.setBackground(this.bg);
        this.layout.setOnTouchListener(new View.OnTouchListener() {
            final MotionEvent.PointerCoords pc0 = new MotionEvent.PointerCoords();
            final MotionEvent.PointerCoords pc1 = new MotionEvent.PointerCoords();

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int actionMasked = motionEvent.getActionMasked();
                if ((actionMasked == 0 || actionMasked == 2) && motionEvent.getPointerCount() > 1) {
                    motionEvent.getPointerCoords(0, this.pc0);
                    motionEvent.getPointerCoords(1, this.pc1);
                    PlatLogoActivity.this.bg.setRadius(((float) Math.hypot(this.pc0.x - this.pc1.x, this.pc0.y - this.pc1.y)) / 2.0f);
                }
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.bg.randomizePalette();
        this.anim = new TimeAnimator();
        this.anim.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator timeAnimator, long j, long j2) {
                PlatLogoActivity.this.bg.setOffset(j / 60000.0f);
                PlatLogoActivity.this.bg.invalidateSelf();
            }
        });
        this.anim.start();
    }

    @Override
    public void onStop() {
        if (this.anim != null) {
            this.anim.cancel();
            this.anim = null;
        }
        super.onStop();
    }
}
