package com.android.systemui.qs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.systemui.R;
import java.util.ArrayList;

public class PageIndicator extends ViewGroup {
    private boolean mAnimating;
    private final Runnable mAnimationDone;
    private final int mPageDotWidth;
    private final int mPageIndicatorHeight;
    private final int mPageIndicatorWidth;
    private int mPosition;
    private final ArrayList<Integer> mQueuedPositions;

    public PageIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mQueuedPositions = new ArrayList<>();
        this.mPosition = -1;
        this.mAnimationDone = new Runnable() {
            @Override
            public void run() {
                PageIndicator.this.mAnimating = false;
                if (PageIndicator.this.mQueuedPositions.size() != 0) {
                    PageIndicator.this.setPosition(((Integer) PageIndicator.this.mQueuedPositions.remove(0)).intValue());
                }
            }
        };
        this.mPageIndicatorWidth = (int) this.mContext.getResources().getDimension(R.dimen.qs_page_indicator_width);
        this.mPageIndicatorHeight = (int) this.mContext.getResources().getDimension(R.dimen.qs_page_indicator_height);
        this.mPageDotWidth = (int) (this.mPageIndicatorWidth * 0.4f);
    }

    public void setNumPages(int i) {
        setVisibility(i > 1 ? 0 : 8);
        if (this.mAnimating) {
            Log.w("PageIndicator", "setNumPages during animation");
        }
        while (i < getChildCount()) {
            removeViewAt(getChildCount() - 1);
        }
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(new int[]{android.R.attr.colorControlActivated});
        int color = typedArrayObtainStyledAttributes.getColor(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        while (i > getChildCount()) {
            ImageView imageView = new ImageView(this.mContext);
            imageView.setImageResource(R.drawable.minor_a_b);
            imageView.setImageTintList(ColorStateList.valueOf(color));
            addView(imageView, new ViewGroup.LayoutParams(this.mPageIndicatorWidth, this.mPageIndicatorHeight));
        }
        setIndex(this.mPosition >> 1);
    }

    public void setLocation(float f) {
        int i = (int) f;
        setContentDescription(getContext().getString(R.string.accessibility_quick_settings_page, Integer.valueOf(i + 1), Integer.valueOf(getChildCount())));
        int i2 = (i << 1) | (f != ((float) i) ? 1 : 0);
        int iIntValue = this.mPosition;
        if (this.mQueuedPositions.size() != 0) {
            iIntValue = this.mQueuedPositions.get(this.mQueuedPositions.size() - 1).intValue();
        }
        if (i2 == iIntValue) {
            return;
        }
        if (this.mAnimating) {
            this.mQueuedPositions.add(Integer.valueOf(i2));
        } else {
            setPosition(i2);
        }
    }

    public void setPosition(int i) {
        if (isVisibleToUser() && Math.abs(this.mPosition - i) == 1) {
            animate(this.mPosition, i);
        } else {
            setIndex(i >> 1);
        }
        this.mPosition = i;
    }

    private void setIndex(int i) {
        int childCount = getChildCount();
        int i2 = 0;
        while (i2 < childCount) {
            ImageView imageView = (ImageView) getChildAt(i2);
            imageView.setTranslationX(0.0f);
            imageView.setImageResource(R.drawable.major_a_b);
            imageView.setAlpha(getAlpha(i2 == i));
            i2++;
        }
    }

    private void animate(int i, int i2) {
        int i3 = i >> 1;
        int i4 = i2 >> 1;
        setIndex(i3);
        boolean z = (i & 1) != 0;
        boolean z2 = !z ? i >= i2 : i <= i2;
        int iMin = Math.min(i3, i4);
        int iMax = Math.max(i3, i4);
        if (iMax == iMin) {
            iMax++;
        }
        ImageView imageView = (ImageView) getChildAt(iMin);
        ImageView imageView2 = (ImageView) getChildAt(iMax);
        if (imageView == null || imageView2 == null) {
            return;
        }
        imageView2.setTranslationX(imageView.getX() - imageView2.getX());
        playAnimation(imageView, getTransition(z, z2, false));
        imageView.setAlpha(getAlpha(false));
        playAnimation(imageView2, getTransition(z, z2, true));
        imageView2.setAlpha(getAlpha(true));
        this.mAnimating = true;
    }

    private float getAlpha(boolean z) {
        return z ? 1.0f : 0.42f;
    }

    private void playAnimation(ImageView imageView, int i) {
        AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) getContext().getDrawable(i);
        imageView.setImageDrawable(animatedVectorDrawable);
        animatedVectorDrawable.forceAnimationOnUI();
        animatedVectorDrawable.start();
        postDelayed(this.mAnimationDone, 250L);
    }

    private int getTransition(boolean z, boolean z2, boolean z3) {
        if (z3) {
            if (z) {
                if (z2) {
                    return R.drawable.major_b_a_animation;
                }
                return R.drawable.major_b_c_animation;
            }
            if (z2) {
                return R.drawable.major_a_b_animation;
            }
            return R.drawable.major_c_b_animation;
        }
        if (z) {
            if (z2) {
                return R.drawable.minor_b_c_animation;
            }
            return R.drawable.minor_b_a_animation;
        }
        if (z2) {
            return R.drawable.minor_c_b_animation;
        }
        return R.drawable.minor_a_b_animation;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int childCount = getChildCount();
        if (childCount == 0) {
            super.onMeasure(i, i2);
            return;
        }
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mPageIndicatorWidth, 1073741824);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(this.mPageIndicatorHeight, 1073741824);
        for (int i3 = 0; i3 < childCount; i3++) {
            getChildAt(i3).measure(iMakeMeasureSpec, iMakeMeasureSpec2);
        }
        setMeasuredDimension(((this.mPageIndicatorWidth - this.mPageDotWidth) * (childCount - 1)) + this.mPageDotWidth, this.mPageIndicatorHeight);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }
        for (int i5 = 0; i5 < childCount; i5++) {
            int i6 = (this.mPageIndicatorWidth - this.mPageDotWidth) * i5;
            getChildAt(i5).layout(i6, 0, this.mPageIndicatorWidth + i6, this.mPageIndicatorHeight);
        }
    }
}
