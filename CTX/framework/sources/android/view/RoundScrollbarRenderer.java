package android.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

class RoundScrollbarRenderer {
    private static final int DEFAULT_THUMB_COLOR = 1291845631;
    private static final int DEFAULT_TRACK_COLOR = 654311423;
    private static final int MAX_SCROLLBAR_ANGLE_SWIPE = 16;
    private static final int MIN_SCROLLBAR_ANGLE_SWIPE = 6;
    private static final int SCROLLBAR_ANGLE_RANGE = 90;
    private static final float WIDTH_PERCENTAGE = 0.02f;
    private final View mParent;
    private final Paint mThumbPaint = new Paint();
    private final Paint mTrackPaint = new Paint();
    private final RectF mRect = new RectF();

    public RoundScrollbarRenderer(View view) {
        this.mThumbPaint.setAntiAlias(true);
        this.mThumbPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mThumbPaint.setStyle(Paint.Style.STROKE);
        this.mTrackPaint.setAntiAlias(true);
        this.mTrackPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mTrackPaint.setStyle(Paint.Style.STROKE);
        this.mParent = view;
    }

    public void drawRoundScrollbars(Canvas canvas, float f, Rect rect) {
        if (f == 0.0f) {
            return;
        }
        float fComputeVerticalScrollRange = this.mParent.computeVerticalScrollRange();
        float fComputeVerticalScrollExtent = this.mParent.computeVerticalScrollExtent();
        if (fComputeVerticalScrollExtent <= 0.0f || fComputeVerticalScrollRange <= fComputeVerticalScrollExtent) {
            return;
        }
        float fMax = Math.max(0, this.mParent.computeVerticalScrollOffset());
        float fComputeVerticalScrollExtent2 = this.mParent.computeVerticalScrollExtent();
        float width = this.mParent.getWidth() * WIDTH_PERCENTAGE;
        this.mThumbPaint.setStrokeWidth(width);
        this.mTrackPaint.setStrokeWidth(width);
        setThumbColor(applyAlpha(DEFAULT_THUMB_COLOR, f));
        setTrackColor(applyAlpha(DEFAULT_TRACK_COLOR, f));
        float fClamp = clamp((fComputeVerticalScrollExtent2 / fComputeVerticalScrollRange) * 90.0f, 6.0f, 16.0f);
        float fClamp2 = clamp(((fMax * (90.0f - fClamp)) / (fComputeVerticalScrollRange - fComputeVerticalScrollExtent2)) - 45.0f, -45.0f, 45.0f - fClamp);
        float f2 = width / 2.0f;
        this.mRect.set(rect.left - f2, rect.top, rect.right - f2, rect.bottom);
        canvas.drawArc(this.mRect, -45.0f, 90.0f, false, this.mTrackPaint);
        canvas.drawArc(this.mRect, fClamp2, fClamp, false, this.mThumbPaint);
    }

    private static float clamp(float f, float f2, float f3) {
        if (f < f2) {
            return f2;
        }
        if (f > f3) {
            return f3;
        }
        return f;
    }

    private static int applyAlpha(int i, float f) {
        return Color.argb((int) (Color.alpha(i) * f), Color.red(i), Color.green(i), Color.blue(i));
    }

    private void setThumbColor(int i) {
        if (this.mThumbPaint.getColor() != i) {
            this.mThumbPaint.setColor(i);
        }
    }

    private void setTrackColor(int i) {
        if (this.mTrackPaint.getColor() != i) {
            this.mTrackPaint.setColor(i);
        }
    }
}
