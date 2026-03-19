package com.android.settings.widget;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;

public final class SlidingTabLayout extends FrameLayout implements View.OnClickListener {
    private final View mIndicatorView;
    private final LayoutInflater mLayoutInflater;
    private int mSelectedPosition;
    private float mSelectionOffset;
    private final LinearLayout mTitleView;
    private RtlCompatibleViewPager mViewPager;

    public SlidingTabLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mTitleView = new LinearLayout(context);
        this.mTitleView.setGravity(1);
        this.mIndicatorView = this.mLayoutInflater.inflate(R.layout.sliding_tab_indicator_view, (ViewGroup) this, false);
        addView(this.mTitleView, -1, -2);
        addView(this.mIndicatorView, this.mIndicatorView.getLayoutParams());
    }

    public void setViewPager(RtlCompatibleViewPager rtlCompatibleViewPager) {
        this.mTitleView.removeAllViews();
        this.mViewPager = rtlCompatibleViewPager;
        if (rtlCompatibleViewPager != null) {
            rtlCompatibleViewPager.addOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int childCount = this.mTitleView.getChildCount();
        if (childCount > 0) {
            this.mIndicatorView.measure(View.MeasureSpec.makeMeasureSpec(this.mTitleView.getMeasuredWidth() / childCount, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mIndicatorView.getMeasuredHeight(), 1073741824));
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mTitleView.getChildCount() > 0) {
            int measuredHeight = getMeasuredHeight();
            int measuredHeight2 = this.mIndicatorView.getMeasuredHeight();
            int measuredWidth = this.mIndicatorView.getMeasuredWidth();
            int measuredWidth2 = getMeasuredWidth();
            this.mTitleView.layout(getPaddingLeft(), 0, this.mTitleView.getMeasuredWidth() + getPaddingRight(), this.mTitleView.getMeasuredHeight());
            if (!isRtlMode()) {
                this.mIndicatorView.layout(0, measuredHeight - measuredHeight2, measuredWidth, measuredHeight);
            } else {
                this.mIndicatorView.layout(measuredWidth2 - measuredWidth, measuredHeight - measuredHeight2, measuredWidth2, measuredHeight);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int childCount = this.mTitleView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (view == this.mTitleView.getChildAt(i)) {
                this.mViewPager.setCurrentItem(i);
                return;
            }
        }
    }

    private void onViewPagerPageChanged(int i, float f) {
        this.mSelectedPosition = i;
        this.mSelectionOffset = f;
        this.mIndicatorView.setTranslationX(isRtlMode() ? -getIndicatorLeft() : getIndicatorLeft());
    }

    private void populateTabStrip() {
        PagerAdapter adapter = this.mViewPager.getAdapter();
        int i = 0;
        while (i < adapter.getCount()) {
            TextView textView = (TextView) this.mLayoutInflater.inflate(R.layout.sliding_tab_title_view, (ViewGroup) this.mTitleView, false);
            textView.setText(adapter.getPageTitle(i));
            textView.setOnClickListener(this);
            this.mTitleView.addView(textView);
            textView.setSelected(i == this.mViewPager.getCurrentItem());
            i++;
        }
    }

    private int getIndicatorLeft() {
        int left = this.mTitleView.getChildAt(this.mSelectedPosition).getLeft();
        if (this.mSelectionOffset > 0.0f && this.mSelectedPosition < getChildCount() - 1) {
            return (int) ((this.mSelectionOffset * this.mTitleView.getChildAt(this.mSelectedPosition + 1).getLeft()) + ((1.0f - this.mSelectionOffset) * left));
        }
        return left;
    }

    private boolean isRtlMode() {
        return getLayoutDirection() == 1;
    }

    private final class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int mScrollState;

        private InternalViewPagerListener() {
        }

        @Override
        public void onPageScrolled(int i, float f, int i2) {
            int childCount = SlidingTabLayout.this.mTitleView.getChildCount();
            if (childCount != 0 && i >= 0 && i < childCount) {
                SlidingTabLayout.this.onViewPagerPageChanged(i, f);
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
            this.mScrollState = i;
        }

        @Override
        public void onPageSelected(int i) {
            int rtlAwareIndex = SlidingTabLayout.this.mViewPager.getRtlAwareIndex(i);
            if (this.mScrollState == 0) {
                SlidingTabLayout.this.onViewPagerPageChanged(rtlAwareIndex, 0.0f);
            }
            int childCount = SlidingTabLayout.this.mTitleView.getChildCount();
            int i2 = 0;
            while (i2 < childCount) {
                SlidingTabLayout.this.mTitleView.getChildAt(i2).setSelected(rtlAwareIndex == i2);
                i2++;
            }
        }
    }
}
