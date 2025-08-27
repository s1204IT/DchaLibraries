package com.android.settings.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.util.Preconditions;
import com.android.settings.R;

/* loaded from: classes.dex */
public class ChartGridView extends View {
    private Drawable mBorder;
    private ChartAxis mHoriz;
    private int mLabelColor;
    private Layout mLabelEnd;
    private Layout mLabelMid;
    private int mLabelSize;
    private Layout mLabelStart;
    private Drawable mPrimary;
    private Drawable mSecondary;
    private ChartAxis mVert;

    public ChartGridView(Context context) {
        this(context, null, 0);
    }

    public ChartGridView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ChartGridView(Context context, AttributeSet attributeSet, int i) throws Resources.NotFoundException {
        super(context, attributeSet, i);
        setWillNotDraw(false);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ChartGridView, i, 0);
        this.mPrimary = typedArrayObtainStyledAttributes.getDrawable(3);
        this.mSecondary = typedArrayObtainStyledAttributes.getDrawable(4);
        this.mBorder = typedArrayObtainStyledAttributes.getDrawable(2);
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(typedArrayObtainStyledAttributes.getResourceId(0, -1), com.android.internal.R.styleable.TextAppearance);
        this.mLabelSize = typedArrayObtainStyledAttributes2.getDimensionPixelSize(0, 0);
        typedArrayObtainStyledAttributes2.recycle();
        this.mLabelColor = typedArrayObtainStyledAttributes.getColorStateList(1).getDefaultColor();
        typedArrayObtainStyledAttributes.recycle();
    }

    void init(ChartAxis chartAxis, ChartAxis chartAxis2) {
        this.mHoriz = (ChartAxis) Preconditions.checkNotNull(chartAxis, "missing horiz");
        this.mVert = (ChartAxis) Preconditions.checkNotNull(chartAxis2, "missing vert");
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight() - getPaddingBottom();
        Drawable drawable = this.mSecondary;
        if (drawable != null) {
            int intrinsicHeight = drawable.getIntrinsicHeight();
            for (float f : this.mVert.getTickPoints()) {
                drawable.setBounds(0, (int) f, width, (int) Math.min(intrinsicHeight + f, height));
                drawable.draw(canvas);
            }
        }
        Drawable drawable2 = this.mPrimary;
        if (drawable2 != null) {
            int intrinsicWidth = drawable2.getIntrinsicWidth();
            drawable2.getIntrinsicHeight();
            for (float f2 : this.mHoriz.getTickPoints()) {
                drawable2.setBounds((int) f2, 0, (int) Math.min(intrinsicWidth + f2, width), height);
                drawable2.draw(canvas);
            }
        }
        this.mBorder.setBounds(0, 0, width, height);
        this.mBorder.draw(canvas);
        int height2 = this.mLabelStart != null ? this.mLabelStart.getHeight() / 8 : 0;
        Layout layout = this.mLabelStart;
        if (layout != null) {
            int iSave = canvas.save();
            canvas.translate(0.0f, height + height2);
            layout.draw(canvas);
            canvas.restoreToCount(iSave);
        }
        Layout layout2 = this.mLabelMid;
        if (layout2 != null) {
            int iSave2 = canvas.save();
            canvas.translate((width - layout2.getWidth()) / 2, height + height2);
            layout2.draw(canvas);
            canvas.restoreToCount(iSave2);
        }
        Layout layout3 = this.mLabelEnd;
        if (layout3 != null) {
            int iSave3 = canvas.save();
            canvas.translate(width - layout3.getWidth(), height + height2);
            layout3.draw(canvas);
            canvas.restoreToCount(iSave3);
        }
    }
}
