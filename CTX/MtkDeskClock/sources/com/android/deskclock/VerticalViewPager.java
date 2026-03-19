package com.android.deskclock;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class VerticalViewPager extends ViewPager {
    public VerticalViewPager(Context context) {
        this(context, null);
    }

    public VerticalViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    @Override
    public boolean canScrollHorizontally(int i) {
        return false;
    }

    @Override
    public boolean canScrollVertically(int i) {
        return super.canScrollHorizontally(i);
    }

    private void init() {
        setPageTransformer(true, new VerticalPageTransformer());
        setOverScrollMode(2);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean zOnInterceptTouchEvent = super.onInterceptTouchEvent(flipXY(motionEvent));
        flipXY(motionEvent);
        return zOnInterceptTouchEvent;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent = super.onTouchEvent(flipXY(motionEvent));
        flipXY(motionEvent);
        return zOnTouchEvent;
    }

    private MotionEvent flipXY(MotionEvent motionEvent) {
        float width = getWidth();
        float height = getHeight();
        motionEvent.setLocation((motionEvent.getY() / height) * width, (motionEvent.getX() / width) * height);
        return motionEvent;
    }

    private static final class VerticalPageTransformer implements ViewPager.PageTransformer {
        private VerticalPageTransformer() {
        }

        @Override
        public void transformPage(View view, float f) {
            int width = view.getWidth();
            int height = view.getHeight();
            if (f < -1.0f) {
                view.setAlpha(0.0f);
            } else {
                if (f <= 1.0f) {
                    view.setAlpha(1.0f);
                    view.setTranslationX(width * (-f));
                    view.setTranslationY(f * height);
                    return;
                }
                view.setAlpha(0.0f);
            }
        }
    }
}
