package com.android.launcher3;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public abstract class BaseContainerView extends FrameLayout implements Insettable {
    private final int mContainerBoundsInset;
    private View mContent;
    protected final Rect mContentPadding;
    protected final int mHorizontalPadding;
    private final Rect mInsets;
    private final Drawable mRevealDrawable;
    private View mRevealView;

    protected abstract void onUpdateBgPadding(Rect rect, Rect rect2);

    public BaseContainerView(Context context) {
        this(context, null);
    }

    public BaseContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mInsets = new Rect();
        this.mContentPadding = new Rect();
        this.mContainerBoundsInset = getResources().getDimensionPixelSize(R.dimen.container_bounds_inset);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BaseContainerView, defStyleAttr, 0);
        this.mRevealDrawable = a.getDrawable(0);
        a.recycle();
        int maxSize = getResources().getDimensionPixelSize(R.dimen.container_max_width);
        int minMargin = getResources().getDimensionPixelSize(R.dimen.container_min_margin);
        int width = ((Launcher) context).getDeviceProfile().availableWidthPx;
        if (maxSize > 0) {
            this.mHorizontalPadding = Math.max(minMargin, (width - maxSize) / 2);
        } else {
            this.mHorizontalPadding = Math.max(minMargin, (int) getResources().getFraction(R.fraction.container_margin, width, 1));
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = findViewById(R.id.main_content);
        this.mRevealView = findViewById(R.id.reveal_view);
    }

    @Override
    public final void setInsets(Rect insets) {
        this.mInsets.set(insets);
        updateBackgroundAndPaddings();
    }

    protected void updateBackgroundAndPaddings() {
        Rect padding = new Rect(this.mHorizontalPadding, this.mInsets.top + this.mContainerBoundsInset, this.mHorizontalPadding, this.mInsets.bottom + this.mContainerBoundsInset);
        if (padding.equals(this.mContentPadding)) {
            return;
        }
        this.mContentPadding.set(padding);
        onUpdateBackgroundAndPaddings(padding);
    }

    private void onUpdateBackgroundAndPaddings(Rect padding) {
        setPadding(0, padding.top, 0, padding.bottom);
        InsetDrawable background = new InsetDrawable(this.mRevealDrawable, padding.left, 0, padding.right, 0);
        this.mRevealView.setBackground(background.getConstantState().newDrawable());
        this.mContent.setBackground(background);
        this.mContent.setPadding(0, 0, 0, 0);
        Rect bgPadding = new Rect();
        background.getPadding(bgPadding);
        onUpdateBgPadding(padding, bgPadding);
    }

    public final View getContentView() {
        return this.mContent;
    }

    public final View getRevealView() {
        return this.mRevealView;
    }
}
