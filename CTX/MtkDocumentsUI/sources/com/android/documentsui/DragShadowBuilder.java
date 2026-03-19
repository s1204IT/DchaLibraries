package com.android.documentsui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

class DragShadowBuilder extends View.DragShadowBuilder {
    private final int mHeight;
    private final DropBadgeView mIcon;
    private int mPadding;
    private final int mShadowRadius;
    private final View mShadowView;
    private final TextView mTitle;
    private final int mWidth;
    private Paint paint;

    DragShadowBuilder(Context context) {
        this.mWidth = context.getResources().getDimensionPixelSize(R.dimen.drag_shadow_width);
        this.mHeight = context.getResources().getDimensionPixelSize(R.dimen.drag_shadow_height);
        this.mShadowRadius = context.getResources().getDimensionPixelSize(R.dimen.drag_shadow_radius);
        this.mPadding = context.getResources().getDimensionPixelSize(R.dimen.drag_shadow_padding);
        this.mShadowView = LayoutInflater.from(context).inflate(R.layout.drag_shadow_layout, (ViewGroup) null);
        this.mTitle = (TextView) this.mShadowView.findViewById(android.R.id.title);
        this.mIcon = (DropBadgeView) this.mShadowView.findViewById(android.R.id.icon);
        this.mShadowView.setLayerType(1, this.paint);
        this.paint = new Paint(1);
    }

    @Override
    public void onProvideShadowMetrics(Point point, Point point2) {
        point.set(this.mWidth, this.mHeight);
        point2.set(this.mWidth, this.mHeight);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        Rect clipBounds = canvas.getClipBounds();
        this.mShadowView.measure(View.MeasureSpec.makeMeasureSpec(clipBounds.right - clipBounds.left, 1073741824), View.MeasureSpec.makeMeasureSpec(clipBounds.bottom - clipBounds.top, 1073741824));
        this.mShadowView.layout(clipBounds.left, clipBounds.top, clipBounds.right, clipBounds.bottom);
        this.paint.setColor(0);
        this.paint.setShadowLayer(this.mShadowRadius, 0.0f, 0.0f, Color.argb(25, 0, 0, 0));
        canvas.drawRect(clipBounds.left + this.mPadding, clipBounds.top + this.mPadding, clipBounds.right - this.mPadding, clipBounds.bottom - this.mPadding, this.paint);
        this.paint.setShadowLayer(this.mShadowRadius, 0.0f, this.mShadowRadius, Color.argb(61, 0, 0, 0));
        canvas.drawRect(clipBounds.left + this.mPadding, clipBounds.top + this.mPadding, clipBounds.right - this.mPadding, clipBounds.bottom - this.mPadding, this.paint);
        this.mShadowView.draw(canvas);
    }

    void updateTitle(String str) {
        this.mTitle.setText(str);
    }

    void updateIcon(Drawable drawable) {
        this.mIcon.updateIcon(drawable);
    }

    void onStateUpdated(int i) {
        this.mIcon.updateState(i);
    }
}
