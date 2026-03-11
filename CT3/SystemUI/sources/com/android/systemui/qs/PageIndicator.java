package com.android.systemui.qs;

import android.content.Context;
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

    public PageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mQueuedPositions = new ArrayList<>();
        this.mPosition = -1;
        this.mAnimationDone = new Runnable() {
            @Override
            public void run() {
                PageIndicator.this.mAnimating = false;
                if (PageIndicator.this.mQueuedPositions.size() == 0) {
                    return;
                }
                PageIndicator.this.setPosition(((Integer) PageIndicator.this.mQueuedPositions.remove(0)).intValue());
            }
        };
        this.mPageIndicatorWidth = (int) this.mContext.getResources().getDimension(R.dimen.qs_page_indicator_width);
        this.mPageIndicatorHeight = (int) this.mContext.getResources().getDimension(R.dimen.qs_page_indicator_height);
        this.mPageDotWidth = (int) (this.mPageIndicatorWidth * 0.4f);
    }

    public void setNumPages(int numPages) {
        setVisibility(numPages > 1 ? 0 : 4);
        if (this.mAnimating) {
            Log.w("PageIndicator", "setNumPages during animation");
        }
        while (numPages < getChildCount()) {
            removeViewAt(getChildCount() - 1);
        }
        while (numPages > getChildCount()) {
            ImageView v = new ImageView(this.mContext);
            v.setImageResource(R.drawable.minor_a_b);
            addView(v, new ViewGroup.LayoutParams(this.mPageIndicatorWidth, this.mPageIndicatorHeight));
        }
        setIndex(this.mPosition >> 1);
    }

    public void setLocation(float location) {
        int index = (int) location;
        setContentDescription(getContext().getString(R.string.accessibility_quick_settings_page, Integer.valueOf(index + 1), Integer.valueOf(getChildCount())));
        int position = (index << 1) | (location == ((float) index) ? 0 : 1);
        int lastPosition = this.mPosition;
        if (this.mQueuedPositions.size() != 0) {
            lastPosition = this.mQueuedPositions.get(this.mQueuedPositions.size() - 1).intValue();
        }
        if (position == lastPosition) {
            return;
        }
        if (this.mAnimating) {
            this.mQueuedPositions.add(Integer.valueOf(position));
        } else {
            setPosition(position);
        }
    }

    public void setPosition(int position) {
        if (isVisibleToUser() && Math.abs(this.mPosition - position) == 1) {
            animate(this.mPosition, position);
        } else {
            setIndex(position >> 1);
        }
        this.mPosition = position;
    }

    private void setIndex(int index) {
        int N = getChildCount();
        int i = 0;
        while (i < N) {
            ImageView v = (ImageView) getChildAt(i);
            v.setTranslationX(0.0f);
            v.setImageResource(R.drawable.major_a_b);
            v.setAlpha(getAlpha(i == index));
            i++;
        }
    }

    private void animate(int from, int to) {
        int fromIndex = from >> 1;
        int toIndex = to >> 1;
        setIndex(fromIndex);
        boolean fromTransition = (from & 1) != 0;
        boolean isAState = !fromTransition ? from >= to : from <= to;
        int firstIndex = Math.min(fromIndex, toIndex);
        int secondIndex = Math.max(fromIndex, toIndex);
        if (secondIndex == firstIndex) {
            secondIndex++;
        }
        ImageView first = (ImageView) getChildAt(firstIndex);
        ImageView second = (ImageView) getChildAt(secondIndex);
        if (first == null || second == null) {
            return;
        }
        second.setTranslationX(first.getX() - second.getX());
        playAnimation(first, getTransition(fromTransition, isAState, false));
        first.setAlpha(getAlpha(false));
        playAnimation(second, getTransition(fromTransition, isAState, true));
        second.setAlpha(getAlpha(true));
        this.mAnimating = true;
    }

    private float getAlpha(boolean isMajor) {
        return isMajor ? 1.0f : 0.3f;
    }

    private void playAnimation(ImageView imageView, int res) {
        AnimatedVectorDrawable avd = (AnimatedVectorDrawable) getContext().getDrawable(res);
        imageView.setImageDrawable(avd);
        avd.forceAnimationOnUI();
        avd.start();
        postDelayed(this.mAnimationDone, 250L);
    }

    private int getTransition(boolean fromB, boolean isMajorAState, boolean isMajor) {
        if (isMajor) {
            if (fromB) {
                if (isMajorAState) {
                    return R.drawable.major_b_a_animation;
                }
                return R.drawable.major_b_c_animation;
            }
            if (isMajorAState) {
                return R.drawable.major_a_b_animation;
            }
            return R.drawable.major_c_b_animation;
        }
        if (fromB) {
            if (isMajorAState) {
                return R.drawable.minor_b_c_animation;
            }
            return R.drawable.minor_b_a_animation;
        }
        if (isMajorAState) {
            return R.drawable.minor_c_b_animation;
        }
        return R.drawable.minor_a_b_animation;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int N = getChildCount();
        if (N == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int widthChildSpec = View.MeasureSpec.makeMeasureSpec(this.mPageIndicatorWidth, 1073741824);
        int heightChildSpec = View.MeasureSpec.makeMeasureSpec(this.mPageIndicatorHeight, 1073741824);
        for (int i = 0; i < N; i++) {
            getChildAt(i).measure(widthChildSpec, heightChildSpec);
        }
        int width = ((this.mPageIndicatorWidth - this.mPageDotWidth) * N) + this.mPageDotWidth;
        setMeasuredDimension(width, this.mPageIndicatorHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int N = getChildCount();
        if (N == 0) {
            return;
        }
        for (int i = 0; i < N; i++) {
            int left = (this.mPageIndicatorWidth - this.mPageDotWidth) * i;
            getChildAt(i).layout(left, 0, this.mPageIndicatorWidth + left, this.mPageIndicatorHeight);
        }
    }
}
