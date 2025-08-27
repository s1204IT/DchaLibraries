package com.android.settings.graph;

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
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;

/* loaded from: classes.dex */
public class UsageGraph extends View {
    private int mAccentColor;
    private final int mCornerRadius;
    private final Drawable mDivider;
    private final int mDividerSize;
    private final Paint mDottedPaint;
    private final Paint mFillPaint;
    private final Paint mLinePaint;
    private final SparseIntArray mLocalPaths;
    private final SparseIntArray mLocalProjectedPaths;
    private float mMaxX;
    private float mMaxY;
    private float mMiddleDividerLoc;
    private int mMiddleDividerTint;
    private final Path mPath;
    private final SparseIntArray mPaths;
    private final SparseIntArray mProjectedPaths;
    private final Drawable mTintedDivider;
    private int mTopDividerTint;

    public UsageGraph(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPath = new Path();
        this.mPaths = new SparseIntArray();
        this.mLocalPaths = new SparseIntArray();
        this.mProjectedPaths = new SparseIntArray();
        this.mLocalProjectedPaths = new SparseIntArray();
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
        this.mCornerRadius = resources.getDimensionPixelSize(R.dimen.usage_graph_line_corner_radius);
        this.mLinePaint.setPathEffect(new CornerPathEffect(this.mCornerRadius));
        this.mLinePaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.usage_graph_line_width));
        this.mFillPaint = new Paint(this.mLinePaint);
        this.mFillPaint.setStyle(Paint.Style.FILL);
        this.mDottedPaint = new Paint(this.mLinePaint);
        this.mDottedPaint.setStyle(Paint.Style.STROKE);
        float dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.usage_graph_dot_size);
        float dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.usage_graph_dot_interval);
        this.mDottedPaint.setStrokeWidth(3.0f * dimensionPixelSize);
        this.mDottedPaint.setPathEffect(new DashPathEffect(new float[]{dimensionPixelSize, dimensionPixelSize2}, 0.0f));
        this.mDottedPaint.setColor(context.getColor(R.color.usage_graph_dots));
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.listDivider, typedValue, true);
        this.mDivider = context.getDrawable(typedValue.resourceId);
        this.mTintedDivider = context.getDrawable(typedValue.resourceId);
        this.mDividerSize = resources.getDimensionPixelSize(R.dimen.usage_graph_divider_size);
    }

    void clearPaths() {
        this.mPaths.clear();
        this.mLocalPaths.clear();
        this.mProjectedPaths.clear();
        this.mLocalProjectedPaths.clear();
    }

    void setMax(int i, int i2) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mMaxX = i;
        this.mMaxY = i2;
        calculateLocalPaths();
        postInvalidate();
        BatteryUtils.logRuntime("UsageGraph", "setMax", jCurrentTimeMillis);
    }

    void setDividerLoc(int i) {
        this.mMiddleDividerLoc = 1.0f - (i / this.mMaxY);
    }

    void setDividerColors(int i, int i2) {
        this.mMiddleDividerTint = i;
        this.mTopDividerTint = i2;
    }

    public void addPath(SparseIntArray sparseIntArray) {
        addPathAndUpdate(sparseIntArray, this.mPaths, this.mLocalPaths);
    }

    public void addProjectedPath(SparseIntArray sparseIntArray) {
        addPathAndUpdate(sparseIntArray, this.mProjectedPaths, this.mLocalProjectedPaths);
    }

    private void addPathAndUpdate(SparseIntArray sparseIntArray, SparseIntArray sparseIntArray2, SparseIntArray sparseIntArray3) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        int size = sparseIntArray.size();
        for (int i = 0; i < size; i++) {
            sparseIntArray2.put(sparseIntArray.keyAt(i), sparseIntArray.valueAt(i));
        }
        sparseIntArray2.put(sparseIntArray.keyAt(sparseIntArray.size() - 1) + 1, -1);
        calculateLocalPaths(sparseIntArray2, sparseIntArray3);
        postInvalidate();
        BatteryUtils.logRuntime("UsageGraph", "addPathAndUpdate", jCurrentTimeMillis);
    }

    void setAccentColor(int i) {
        this.mAccentColor = i;
        this.mLinePaint.setColor(this.mAccentColor);
        updateGradient();
        postInvalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        super.onSizeChanged(i, i2, i3, i4);
        updateGradient();
        calculateLocalPaths();
        BatteryUtils.logRuntime("UsageGraph", "onSizeChanged", jCurrentTimeMillis);
    }

    private void calculateLocalPaths() {
        calculateLocalPaths(this.mPaths, this.mLocalPaths);
        calculateLocalPaths(this.mProjectedPaths, this.mLocalProjectedPaths);
    }

    void calculateLocalPaths(SparseIntArray sparseIntArray, SparseIntArray sparseIntArray2) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (getWidth() == 0) {
            return;
        }
        sparseIntArray2.clear();
        int i = -1;
        boolean z = false;
        int i2 = 0;
        for (int i3 = 0; i3 < sparseIntArray.size(); i3++) {
            int iKeyAt = sparseIntArray.keyAt(i3);
            int iValueAt = sparseIntArray.valueAt(i3);
            if (iValueAt == -1) {
                if (i3 == 1) {
                    sparseIntArray2.put(getX(iKeyAt + 1) - 1, getY(0.0f));
                } else {
                    if (i3 == sparseIntArray.size() - 1 && z) {
                        sparseIntArray2.put(i2, i);
                    }
                    sparseIntArray2.put(i2 + 1, -1);
                    z = false;
                }
            } else {
                int x = getX(iKeyAt);
                int y = getY(iValueAt);
                if (sparseIntArray2.size() > 0) {
                    int iKeyAt2 = sparseIntArray2.keyAt(sparseIntArray2.size() - 1);
                    int iValueAt2 = sparseIntArray2.valueAt(sparseIntArray2.size() - 1);
                    if (iValueAt2 != -1 && !hasDiff(iKeyAt2, x) && !hasDiff(iValueAt2, y)) {
                        i = y;
                        i2 = x;
                        z = true;
                    }
                }
                sparseIntArray2.put(x, y);
                i = y;
                i2 = x;
                z = false;
            }
        }
        BatteryUtils.logRuntime("UsageGraph", "calculateLocalPaths", jCurrentTimeMillis);
    }

    private boolean hasDiff(int i, int i2) {
        return Math.abs(i2 - i) >= this.mCornerRadius;
    }

    private int getX(float f) {
        return (int) ((f / this.mMaxX) * getWidth());
    }

    private int getY(float f) {
        return (int) (getHeight() * (1.0f - (f / this.mMaxY)));
    }

    private void updateGradient() {
        this.mFillPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, getHeight(), getColor(this.mAccentColor, 0.2f), 0, Shader.TileMode.CLAMP));
    }

    private int getColor(int i, float f) {
        return i & ((((int) (255.0f * f)) << 24) | 16777215);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mMiddleDividerLoc != 0.0f) {
            drawDivider(0, canvas, this.mTopDividerTint);
        }
        drawDivider((int) ((canvas.getHeight() - this.mDividerSize) * this.mMiddleDividerLoc), canvas, this.mMiddleDividerTint);
        drawDivider(canvas.getHeight() - this.mDividerSize, canvas, -1);
        if (this.mLocalPaths.size() == 0 && this.mLocalProjectedPaths.size() == 0) {
            return;
        }
        drawLinePath(canvas, this.mLocalProjectedPaths, this.mDottedPaint);
        drawFilledPath(canvas, this.mLocalPaths, this.mFillPaint);
        drawLinePath(canvas, this.mLocalPaths, this.mLinePaint);
        BatteryUtils.logRuntime("UsageGraph", "onDraw", jCurrentTimeMillis);
    }

    private void drawLinePath(Canvas canvas, SparseIntArray sparseIntArray, Paint paint) {
        if (sparseIntArray.size() == 0) {
            return;
        }
        this.mPath.reset();
        this.mPath.moveTo(sparseIntArray.keyAt(0), sparseIntArray.valueAt(0));
        int i = 1;
        while (i < sparseIntArray.size()) {
            int iKeyAt = sparseIntArray.keyAt(i);
            int iValueAt = sparseIntArray.valueAt(i);
            if (iValueAt == -1) {
                i++;
                if (i < sparseIntArray.size()) {
                    this.mPath.moveTo(sparseIntArray.keyAt(i), sparseIntArray.valueAt(i));
                }
            } else {
                this.mPath.lineTo(iKeyAt, iValueAt);
            }
            i++;
        }
        canvas.drawPath(this.mPath, paint);
    }

    private void drawFilledPath(Canvas canvas, SparseIntArray sparseIntArray, Paint paint) {
        this.mPath.reset();
        float fKeyAt = sparseIntArray.keyAt(0);
        this.mPath.moveTo(sparseIntArray.keyAt(0), sparseIntArray.valueAt(0));
        float fKeyAt2 = fKeyAt;
        int i = 1;
        while (i < sparseIntArray.size()) {
            int iKeyAt = sparseIntArray.keyAt(i);
            int iValueAt = sparseIntArray.valueAt(i);
            if (iValueAt == -1) {
                this.mPath.lineTo(sparseIntArray.keyAt(i - 1), getHeight());
                this.mPath.lineTo(fKeyAt2, getHeight());
                this.mPath.close();
                i++;
                if (i < sparseIntArray.size()) {
                    fKeyAt2 = sparseIntArray.keyAt(i);
                    this.mPath.moveTo(sparseIntArray.keyAt(i), sparseIntArray.valueAt(i));
                }
            } else {
                this.mPath.lineTo(iKeyAt, iValueAt);
            }
            i++;
        }
        canvas.drawPath(this.mPath, paint);
    }

    private void drawDivider(int i, Canvas canvas, int i2) {
        Drawable drawable = this.mDivider;
        if (i2 != -1) {
            this.mTintedDivider.setTint(i2);
            drawable = this.mTintedDivider;
        }
        drawable.setBounds(0, i, canvas.getWidth(), this.mDividerSize + i);
        drawable.draw(canvas);
    }
}
