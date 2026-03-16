package com.android.contacts.common.list;

import android.R;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ViewPagerTabs extends HorizontalScrollView implements ViewPager.OnPageChangeListener {
    ViewPager mPager;
    int mPrevSelected;
    int mSidePadding;
    private ViewPagerTabStrip mTabStrip;
    final boolean mTextAllCaps;
    final ColorStateList mTextColor;
    final int mTextSize;
    final int mTextStyle;
    private static final ViewOutlineProvider VIEW_BOUNDS_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };
    private static final int[] ATTRS = {R.attr.textSize, R.attr.textStyle, R.attr.textColor, R.attr.textAllCaps};

    private class OnTabLongClickListener implements View.OnLongClickListener {
        final int mPosition;

        public OnTabLongClickListener(int position) {
            this.mPosition = position;
        }

        @Override
        public boolean onLongClick(View v) {
            int[] screenPos = new int[2];
            ViewPagerTabs.this.getLocationOnScreen(screenPos);
            Context context = ViewPagerTabs.this.getContext();
            int width = ViewPagerTabs.this.getWidth();
            int height = ViewPagerTabs.this.getHeight();
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            Toast toast = Toast.makeText(context, ViewPagerTabs.this.mPager.getAdapter().getPageTitle(this.mPosition), 0);
            toast.setGravity(49, (screenPos[0] + (width / 2)) - (screenWidth / 2), screenPos[1] + height);
            toast.show();
            return true;
        }
    }

    public ViewPagerTabs(Context context) {
        this(context, null);
    }

    public ViewPagerTabs(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewPagerTabs(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPrevSelected = -1;
        setFillViewport(true);
        this.mSidePadding = (int) (getResources().getDisplayMetrics().density * 10.0f);
        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        this.mTextSize = a.getDimensionPixelSize(0, 0);
        this.mTextStyle = a.getInt(1, 0);
        this.mTextColor = a.getColorStateList(2);
        this.mTextAllCaps = a.getBoolean(3, false);
        this.mTabStrip = new ViewPagerTabStrip(context);
        addView(this.mTabStrip, new FrameLayout.LayoutParams(-2, -1));
        a.recycle();
        setOutlineProvider(VIEW_BOUNDS_OUTLINE_PROVIDER);
    }

    public void setViewPager(ViewPager viewPager) {
        this.mPager = viewPager;
        addTabs(this.mPager.getAdapter());
    }

    private void addTabs(PagerAdapter adapter) {
        this.mTabStrip.removeAllViews();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            addTab(adapter.getPageTitle(i), i);
        }
    }

    private void addTab(CharSequence tabTitle, final int position) {
        TextView textView = new TextView(getContext());
        textView.setText(tabTitle);
        textView.setBackgroundResource(com.android.contacts.R.drawable.view_pager_tab_background);
        textView.setGravity(17);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPagerTabs.this.mPager.setCurrentItem(ViewPagerTabs.this.getRtlPosition(position));
            }
        });
        textView.setOnLongClickListener(new OnTabLongClickListener(position));
        if (this.mTextStyle > 0) {
            textView.setTypeface(textView.getTypeface(), this.mTextStyle);
        }
        if (this.mTextSize > 0) {
            textView.setTextSize(0, this.mTextSize);
        }
        if (this.mTextColor != null) {
            textView.setTextColor(this.mTextColor);
        }
        textView.setAllCaps(this.mTextAllCaps);
        textView.setPadding(this.mSidePadding, 0, this.mSidePadding, 0);
        this.mTabStrip.addView(textView, new LinearLayout.LayoutParams(-2, -1, 1.0f));
        if (position == 0) {
            this.mPrevSelected = 0;
            textView.setSelected(true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        int position2 = getRtlPosition(position);
        int tabStripChildCount = this.mTabStrip.getChildCount();
        if (tabStripChildCount != 0 && position2 >= 0 && position2 < tabStripChildCount) {
            this.mTabStrip.onPageScrolled(position2, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        int position2 = getRtlPosition(position);
        int tabStripChildCount = this.mTabStrip.getChildCount();
        if (tabStripChildCount != 0 && position2 >= 0 && position2 < tabStripChildCount) {
            if (this.mPrevSelected >= 0 && this.mPrevSelected < tabStripChildCount) {
                this.mTabStrip.getChildAt(this.mPrevSelected).setSelected(false);
            }
            View selectedChild = this.mTabStrip.getChildAt(position2);
            selectedChild.setSelected(true);
            int scrollPos = selectedChild.getLeft() - ((getWidth() - selectedChild.getWidth()) / 2);
            smoothScrollTo(scrollPos, 0);
            this.mPrevSelected = position2;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private int getRtlPosition(int position) {
        if (getLayoutDirection() == 1) {
            return (this.mTabStrip.getChildCount() - 1) - position;
        }
        return position;
    }
}
