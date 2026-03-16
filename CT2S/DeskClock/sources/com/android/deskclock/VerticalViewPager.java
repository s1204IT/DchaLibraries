package com.android.deskclock;

import android.content.Context;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

public class VerticalViewPager extends ViewPager {
    private boolean mHorizontalDrag;
    private float mLastMotionX;
    private float mLastMotionY;
    private final ViewPager.PageTransformer mPageTransformer;
    private ViewPager mParentViewPager;
    private float mTouchSlop;
    private boolean mVerticalDrag;

    public VerticalViewPager(Context context) {
        super(context, null);
        this.mPageTransformer = new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View view, float position) {
                int pageWidth = view.getWidth();
                int pageHeight = view.getHeight();
                if (position < -1.0f) {
                    view.setAlpha(0.0f);
                    return;
                }
                if (position <= 1.0f) {
                    view.setAlpha(1.0f);
                    view.setTranslationX(pageWidth * (-position));
                    float yPosition = position * pageHeight;
                    view.setTranslationY(yPosition);
                    return;
                }
                view.setAlpha(0.0f);
            }
        };
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPageTransformer = new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View view, float position) {
                int pageWidth = view.getWidth();
                int pageHeight = view.getHeight();
                if (position < -1.0f) {
                    view.setAlpha(0.0f);
                    return;
                }
                if (position <= 1.0f) {
                    view.setAlpha(1.0f);
                    view.setTranslationX(pageWidth * (-position));
                    float yPosition = position * pageHeight;
                    view.setTranslationY(yPosition);
                    return;
                }
                view.setAlpha(0.0f);
            }
        };
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        init();
    }

    private void init() {
        setPageTransformer(true, this.mPageTransformer);
        setOverScrollMode(2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            initializeParent();
            float x = ev.getX();
            float y = ev.getY();
            switch (ev.getAction()) {
                case 0:
                    this.mLastMotionX = x;
                    this.mLastMotionY = y;
                    if (this.mParentViewPager.onTouchEvent(ev)) {
                        return verticalDrag(ev);
                    }
                    return false;
                case 1:
                    break;
                case 2:
                    float xDiff = Math.abs(x - this.mLastMotionX);
                    float yDiff = Math.abs(y - this.mLastMotionY);
                    if (!this.mHorizontalDrag && !this.mVerticalDrag) {
                        if (xDiff > this.mTouchSlop && xDiff > yDiff) {
                            this.mHorizontalDrag = true;
                        } else if (yDiff > this.mTouchSlop && yDiff > xDiff) {
                            this.mVerticalDrag = true;
                        }
                    }
                    if (this.mHorizontalDrag) {
                        return this.mParentViewPager.onTouchEvent(ev);
                    }
                    if (this.mVerticalDrag) {
                        return verticalDrag(ev);
                    }
                    break;
                default:
                    this.mHorizontalDrag = false;
                    this.mVerticalDrag = false;
                    return false;
            }
            if (this.mHorizontalDrag) {
                this.mHorizontalDrag = false;
                return this.mParentViewPager.onTouchEvent(ev);
            }
            if (this.mVerticalDrag) {
                this.mVerticalDrag = false;
                return verticalDrag(ev);
            }
            this.mHorizontalDrag = false;
            this.mVerticalDrag = false;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void initializeParent() {
        if (this.mParentViewPager == null) {
            ViewParent parent = getParent().getParent().getParent();
            if (parent instanceof ViewPager) {
                this.mParentViewPager = (ViewPager) parent;
            }
        }
    }

    private boolean verticalDrag(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        ev.setLocation(y, x);
        return super.onTouchEvent(ev);
    }
}
