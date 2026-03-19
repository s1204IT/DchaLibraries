package android.support.v7.graphics.drawable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.appcompat.R;
import com.android.contacts.ContactPhotoManager;

public class DrawerArrowDrawable extends Drawable {
    private static final float ARROW_HEAD_ANGLE = (float) Math.toRadians(45.0d);
    private float mArrowHeadLength;
    private float mArrowShaftLength;
    private float mBarGap;
    private float mBarLength;
    private float mMaxCutForBarSize;
    private float mProgress;
    private final int mSize;
    private boolean mSpin;
    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();
    private boolean mVerticalMirror = false;
    private int mDirection = 2;

    public DrawerArrowDrawable(Context context) {
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeJoin(Paint.Join.MITER);
        this.mPaint.setStrokeCap(Paint.Cap.BUTT);
        this.mPaint.setAntiAlias(true);
        TypedArray a = context.getTheme().obtainStyledAttributes(null, R.styleable.DrawerArrowToggle, R.attr.drawerArrowStyle, R.style.Base_Widget_AppCompat_DrawerArrowToggle);
        setColor(a.getColor(R.styleable.DrawerArrowToggle_color, 0));
        setBarThickness(a.getDimension(R.styleable.DrawerArrowToggle_thickness, ContactPhotoManager.OFFSET_DEFAULT));
        setSpinEnabled(a.getBoolean(R.styleable.DrawerArrowToggle_spinBars, true));
        setGapSize(Math.round(a.getDimension(R.styleable.DrawerArrowToggle_gapBetweenBars, ContactPhotoManager.OFFSET_DEFAULT)));
        this.mSize = a.getDimensionPixelSize(R.styleable.DrawerArrowToggle_drawableSize, 0);
        this.mBarLength = Math.round(a.getDimension(R.styleable.DrawerArrowToggle_barLength, ContactPhotoManager.OFFSET_DEFAULT));
        this.mArrowHeadLength = Math.round(a.getDimension(R.styleable.DrawerArrowToggle_arrowHeadLength, ContactPhotoManager.OFFSET_DEFAULT));
        this.mArrowShaftLength = a.getDimension(R.styleable.DrawerArrowToggle_arrowShaftLength, ContactPhotoManager.OFFSET_DEFAULT);
        a.recycle();
    }

    public void setColor(int color) {
        if (color != this.mPaint.getColor()) {
            this.mPaint.setColor(color);
            invalidateSelf();
        }
    }

    public void setBarThickness(float width) {
        if (this.mPaint.getStrokeWidth() != width) {
            this.mPaint.setStrokeWidth(width);
            this.mMaxCutForBarSize = (float) (((double) (width / 2.0f)) * Math.cos(ARROW_HEAD_ANGLE));
            invalidateSelf();
        }
    }

    public void setGapSize(float gap) {
        if (gap != this.mBarGap) {
            this.mBarGap = gap;
            invalidateSelf();
        }
    }

    public void setSpinEnabled(boolean enabled) {
        if (this.mSpin != enabled) {
            this.mSpin = enabled;
            invalidateSelf();
        }
    }

    public void setVerticalMirror(boolean verticalMirror) {
        if (this.mVerticalMirror != verticalMirror) {
            this.mVerticalMirror = verticalMirror;
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int i = this.mDirection;
        boolean flipToPointRight = false;
        if (i != 3) {
            switch (i) {
                case 0:
                    flipToPointRight = false;
                    break;
                case 1:
                    flipToPointRight = true;
                    break;
                default:
                    if (DrawableCompat.getLayoutDirection(this) == 1) {
                        flipToPointRight = true;
                    }
                    break;
            }
        } else if (DrawableCompat.getLayoutDirection(this) == 0) {
            flipToPointRight = true;
        }
        boolean flipToPointRight2 = flipToPointRight;
        float arrowHeadBarLength = lerp(this.mBarLength, (float) Math.sqrt(this.mArrowHeadLength * this.mArrowHeadLength * 2.0f), this.mProgress);
        float arrowShaftLength = lerp(this.mBarLength, this.mArrowShaftLength, this.mProgress);
        float arrowShaftCut = Math.round(lerp(ContactPhotoManager.OFFSET_DEFAULT, this.mMaxCutForBarSize, this.mProgress));
        float rotation = lerp(ContactPhotoManager.OFFSET_DEFAULT, ARROW_HEAD_ANGLE, this.mProgress);
        float canvasRotate = lerp(flipToPointRight2 ? 0.0f : -180.0f, flipToPointRight2 ? 180.0f : 0.0f, this.mProgress);
        float arrowWidth = Math.round(((double) arrowHeadBarLength) * Math.cos(rotation));
        float arrowHeight = Math.round(((double) arrowHeadBarLength) * Math.sin(rotation));
        this.mPath.rewind();
        float topBottomBarOffset = lerp(this.mBarGap + this.mPaint.getStrokeWidth(), -this.mMaxCutForBarSize, this.mProgress);
        float arrowEdge = (-arrowShaftLength) / 2.0f;
        this.mPath.moveTo(arrowEdge + arrowShaftCut, ContactPhotoManager.OFFSET_DEFAULT);
        this.mPath.rLineTo(arrowShaftLength - (arrowShaftCut * 2.0f), ContactPhotoManager.OFFSET_DEFAULT);
        this.mPath.moveTo(arrowEdge, topBottomBarOffset);
        this.mPath.rLineTo(arrowWidth, arrowHeight);
        this.mPath.moveTo(arrowEdge, -topBottomBarOffset);
        this.mPath.rLineTo(arrowWidth, -arrowHeight);
        this.mPath.close();
        canvas.save();
        float barThickness = this.mPaint.getStrokeWidth();
        int remainingSpace = (int) ((bounds.height() - (3.0f * barThickness)) - (this.mBarGap * 2.0f));
        float yOffset = (remainingSpace / 4) * 2;
        canvas.translate(bounds.centerX(), yOffset + (1.5f * barThickness) + this.mBarGap);
        if (this.mSpin) {
            canvas.rotate((this.mVerticalMirror ^ flipToPointRight2 ? -1 : 1) * canvasRotate);
        } else if (flipToPointRight2) {
            canvas.rotate(180.0f);
        }
        canvas.drawPath(this.mPath, this.mPaint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != this.mPaint.getAlpha()) {
            this.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mSize;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    public void setProgress(float progress) {
        if (this.mProgress != progress) {
            this.mProgress = progress;
            invalidateSelf();
        }
    }

    private static float lerp(float a, float b, float t) {
        return ((b - a) * t) + a;
    }
}
