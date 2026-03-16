package jp.co.omronsoft.iwnnime.ml.candidate;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class CandidatesViewPager extends ViewPager {
    private int mDisplayWidth;
    private int mOffset;
    private int mPosition;

    public CandidatesViewPager(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public CandidatesViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @Override
    protected int computeHorizontalScrollRange() {
        int ret = this.mDisplayWidth;
        PagerAdapter adapter = getAdapter();
        return ret * adapter.getCount();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        int ret = (this.mPosition * this.mDisplayWidth) + this.mOffset;
        return ret;
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
    }

    public void setDisplayWidth(int width) {
        this.mDisplayWidth = width;
    }

    public void onPageScrolled(int position, int positionOffsetPixels) {
        this.mPosition = position;
        this.mOffset = positionOffsetPixels;
    }
}
