package com.android.internal.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.Button;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
public class EmphasizedNotificationButton extends Button {
    private final RippleDrawable mRipple;
    private final int mStrokeColor;
    private final int mStrokeWidth;

    public EmphasizedNotificationButton(Context context) {
        this(context, null);
    }

    public EmphasizedNotificationButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public EmphasizedNotificationButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public EmphasizedNotificationButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mRipple = (RippleDrawable) ((DrawableWrapper) getBackground().mutate()).getDrawable();
        this.mStrokeWidth = getResources().getDimensionPixelSize(R.dimen.emphasized_button_stroke_width);
        this.mStrokeColor = getContext().getColor(R.color.material_grey_300);
        this.mRipple.mutate();
    }

    @RemotableViewMethod
    public void setRippleColor(ColorStateList colorStateList) {
        this.mRipple.setColor(colorStateList);
        invalidate();
    }

    @RemotableViewMethod
    public void setButtonBackground(ColorStateList colorStateList) {
        ((GradientDrawable) this.mRipple.getDrawable(0)).setColor(colorStateList);
        invalidate();
    }

    @RemotableViewMethod
    public void setHasStroke(boolean z) {
        ((GradientDrawable) this.mRipple.getDrawable(0)).setStroke(z ? this.mStrokeWidth : 0, this.mStrokeColor);
        invalidate();
    }
}
