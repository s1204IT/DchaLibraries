package com.android.settingslib.graph;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.View;
import com.android.settingslib.R$color;
import com.android.settingslib.R$dimen;

public class UsageGraph extends View {
    private int mAccentColor;
    private final int mCornerRadius;
    private final Drawable mDivider;
    private final int mDividerSize;
    private final Paint mDottedPaint;
    private final Paint mFillPaint;
    private final Paint mLinePaint;
    private final SparseIntArray mLocalPaths;
    private float mMaxX;
    private float mMaxY;
    private float mMiddleDividerLoc;
    private int mMiddleDividerTint;
    private final Path mPath;
    private final SparseIntArray mPaths;
    private boolean mProjectUp;
    private boolean mShowProjection;
    private final Drawable mTintedDivider;
    private int mTopDividerTint;

    public UsageGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPath = new Path();
        this.mPaths = new SparseIntArray();
        this.mLocalPaths = new SparseIntArray();
        this.mMaxX = 100.0f;
        this.mMaxY = 100.0f;
        this.mMiddleDividerLoc = 0.5f;
        this.mMiddleDividerTint = -1;
        this.mTopDividerTint = -1;
        Resources resources = context.getResources();
        this.mLinePaint = new Paint();
        this.mLinePaint.setStyle(Paint.Style.STROKE);
        this.mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        this.mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        this.mLinePaint.setAntiAlias(true);
        this.mCornerRadius = resources.getDimensionPixelSize(R$dimen.usage_graph_line_corner_radius);
        this.mLinePaint.setPathEffect(new CornerPathEffect(this.mCornerRadius));
        this.mLinePaint.setStrokeWidth(resources.getDimensionPixelSize(R$dimen.usage_graph_line_width));
        this.mFillPaint = new Paint(this.mLinePaint);
        this.mFillPaint.setStyle(Paint.Style.FILL);
        this.mDottedPaint = new Paint(this.mLinePaint);
        this.mDottedPaint.setStyle(Paint.Style.STROKE);
        float dots = resources.getDimensionPixelSize(R$dimen.usage_graph_dot_size);
        float interval = resources.getDimensionPixelSize(R$dimen.usage_graph_dot_interval);
        this.mDottedPaint.setStrokeWidth(3.0f * dots);
        this.mDottedPaint.setPathEffect(new DashPathEffect(new float[]{dots, interval}, 0.0f));
        this.mDottedPaint.setColor(context.getColor(R$color.usage_graph_dots));
        TypedValue v = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.listDivider, v, true);
        this.mDivider = context.getDrawable(v.resourceId);
        this.mTintedDivider = context.getDrawable(v.resourceId);
        this.mDividerSize = resources.getDimensionPixelSize(R$dimen.usage_graph_divider_size);
    }

    void clearPaths() {
        this.mPaths.clear();
    }

    void setMax(int maxX, int maxY) {
        this.mMaxX = maxX;
        this.mMaxY = maxY;
    }

    void setDividerLoc(int height) {
        this.mMiddleDividerLoc = 1.0f - (height / this.mMaxY);
    }

    void setDividerColors(int middleColor, int topColor) {
        this.mMiddleDividerTint = middleColor;
        this.mTopDividerTint = topColor;
    }

    public void addPath(SparseIntArray points) {
        for (int i = 0; i < points.size(); i++) {
            this.mPaths.put(points.keyAt(i), points.valueAt(i));
        }
        this.mPaths.put(points.keyAt(points.size() - 1) + 1, -1);
        calculateLocalPaths();
        postInvalidate();
    }

    void setAccentColor(int color) {
        this.mAccentColor = color;
        this.mLinePaint.setColor(this.mAccentColor);
        updateGradient();
        postInvalidate();
    }

    void setShowProjection(boolean showProjection, boolean projectUp) {
        this.mShowProjection = showProjection;
        this.mProjectUp = projectUp;
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient();
        calculateLocalPaths();
    }

    private void calculateLocalPaths() {
        if (getWidth() == 0) {
            return;
        }
        this.mLocalPaths.clear();
        int pendingXLoc = 0;
        int pendingYLoc = -1;
        for (int i = 0; i < this.mPaths.size(); i++) {
            int x = this.mPaths.keyAt(i);
            int y = this.mPaths.valueAt(i);
            if (y == -1) {
                if (i == this.mPaths.size() - 1 && pendingYLoc != -1) {
                    this.mLocalPaths.put(pendingXLoc, pendingYLoc);
                }
                pendingYLoc = -1;
                this.mLocalPaths.put(pendingXLoc + 1, -1);
            } else {
                int lx = getX(x);
                int ly = getY(y);
                pendingXLoc = lx;
                if (this.mLocalPaths.size() > 0) {
                    int lastX = this.mLocalPaths.keyAt(this.mLocalPaths.size() - 1);
                    int lastY = this.mLocalPaths.valueAt(this.mLocalPaths.size() - 1);
                    if (lastY != -1 && !hasDiff(lastX, lx) && !hasDiff(lastY, ly)) {
                        pendingYLoc = ly;
                    } else {
                        this.mLocalPaths.put(lx, ly);
                    }
                }
            }
        }
    }

    private boolean hasDiff(int x1, int x2) {
        return Math.abs(x2 - x1) >= this.mCornerRadius;
    }

    private int getX(float x) {
        return (int) ((x / this.mMaxX) * getWidth());
    }

    private int getY(float y) {
        return (int) (getHeight() * (1.0f - (y / this.mMaxY)));
    }

    private void updateGradient() {
        this.mFillPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, getHeight(), getColor(this.mAccentColor, 0.2f), 0, Shader.TileMode.CLAMP));
    }

    private int getColor(int color, float alphaScale) {
        return ((((int) (255.0f * alphaScale)) << 24) | 16777215) & color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mMiddleDividerLoc != 0.0f) {
            drawDivider(0, canvas, this.mTopDividerTint);
        }
        drawDivider((int) ((canvas.getHeight() - this.mDividerSize) * this.mMiddleDividerLoc), canvas, this.mMiddleDividerTint);
        drawDivider(canvas.getHeight() - this.mDividerSize, canvas, -1);
        if (this.mLocalPaths.size() == 0) {
            return;
        }
        if (this.mShowProjection) {
            drawProjection(canvas);
        }
        drawFilledPath(canvas);
        drawLinePath(canvas);
    }

    private void drawProjection(Canvas canvas) {
        this.mPath.reset();
        int x = this.mLocalPaths.keyAt(this.mLocalPaths.size() - 2);
        int y = this.mLocalPaths.valueAt(this.mLocalPaths.size() - 2);
        this.mPath.moveTo(x, y);
        this.mPath.lineTo(canvas.getWidth(), this.mProjectUp ? 0 : canvas.getHeight());
        canvas.drawPath(this.mPath, this.mDottedPaint);
    }

    private void drawLinePath(Canvas canvas) {
        this.mPath.reset();
        this.mPath.moveTo(this.mLocalPaths.keyAt(0), this.mLocalPaths.valueAt(0));
        int i = 1;
        while (i < this.mLocalPaths.size()) {
            int x = this.mLocalPaths.keyAt(i);
            int y = this.mLocalPaths.valueAt(i);
            if (y == -1) {
                i++;
                if (i < this.mLocalPaths.size()) {
                    this.mPath.moveTo(this.mLocalPaths.keyAt(i), this.mLocalPaths.valueAt(i));
                }
            } else {
                this.mPath.lineTo(x, y);
            }
            i++;
        }
        canvas.drawPath(this.mPath, this.mLinePaint);
    }

    private void drawFilledPath(Canvas canvas) {
        this.mPath.reset();
        float lastStartX = this.mLocalPaths.keyAt(0);
        this.mPath.moveTo(this.mLocalPaths.keyAt(0), this.mLocalPaths.valueAt(0));
        int i = 1;
        while (i < this.mLocalPaths.size()) {
            int x = this.mLocalPaths.keyAt(i);
            int y = this.mLocalPaths.valueAt(i);
            if (y == -1) {
                this.mPath.lineTo(this.mLocalPaths.keyAt(i - 1), getHeight());
                this.mPath.lineTo(lastStartX, getHeight());
                this.mPath.close();
                i++;
                if (i < this.mLocalPaths.size()) {
                    lastStartX = this.mLocalPaths.keyAt(i);
                    this.mPath.moveTo(this.mLocalPaths.keyAt(i), this.mLocalPaths.valueAt(i));
                }
            } else {
                this.mPath.lineTo(x, y);
            }
            i++;
        }
        canvas.drawPath(this.mPath, this.mFillPaint);
    }

    private void drawDivider(int y, Canvas canvas, int tintColor) {
        Drawable d = this.mDivider;
        if (tintColor != -1) {
            this.mTintedDivider.setTint(tintColor);
            d = this.mTintedDivider;
        }
        d.setBounds(0, y, canvas.getWidth(), this.mDividerSize + y);
        d.draw(canvas);
    }
}
