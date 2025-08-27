package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/* loaded from: classes.dex */
public class PagedTileLayout extends ViewPager implements QSPanel.QSTileLayout {
    private static final Interpolator SCROLL_CUBIC = new Interpolator() { // from class: com.android.systemui.qs.-$$Lambda$PagedTileLayout$fHkBmUM3ca-ZV4_eDd9ap-VT7Ho
        @Override // android.animation.TimeInterpolator
        public final float getInterpolation(float f) {
            return PagedTileLayout.lambda$static$0(f);
        }
    };
    private final PagerAdapter mAdapter;
    private int mAnimatingToPage;
    private AnimatorSet mBounceAnimatorSet;
    private final Runnable mDistribute;
    private float mLastExpansion;
    private boolean mListening;
    private int mNumPages;
    private final ViewPager.OnPageChangeListener mOnPageChangeListener;
    private PageIndicator mPageIndicator;
    private float mPageIndicatorPosition;
    private PageListener mPageListener;
    private final ArrayList<TilePage> mPages;
    private Scroller mScroller;
    private final ArrayList<QSPanel.TileRecord> mTiles;

    public interface PageListener {
        void onPageChanged(boolean z);
    }

    static /* synthetic */ float lambda$static$0(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * f2) + 1.0f;
    }

    public PagedTileLayout(Context context, AttributeSet attributeSet) throws Resources.NotFoundException {
        super(context, attributeSet);
        this.mTiles = new ArrayList<>();
        this.mPages = new ArrayList<>();
        this.mAnimatingToPage = -1;
        this.mDistribute = new Runnable() { // from class: com.android.systemui.qs.PagedTileLayout.1
            AnonymousClass1() {
            }

            @Override // java.lang.Runnable
            public void run() throws Resources.NotFoundException {
                PagedTileLayout.this.distributeTiles();
            }
        };
        this.mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() { // from class: com.android.systemui.qs.PagedTileLayout.3
            AnonymousClass3() {
            }

            @Override // android.support.v4.view.ViewPager.SimpleOnPageChangeListener, android.support.v4.view.ViewPager.OnPageChangeListener
            public void onPageSelected(int i) {
                PagedTileLayout.this.updateSelected();
                if (PagedTileLayout.this.mPageIndicator != null && PagedTileLayout.this.mPageListener != null) {
                    PageListener pageListener = PagedTileLayout.this.mPageListener;
                    boolean z = false;
                    if (!PagedTileLayout.this.isLayoutRtl() ? i == 0 : i == PagedTileLayout.this.mPages.size() - 1) {
                        z = true;
                    }
                    pageListener.onPageChanged(z);
                }
            }

            @Override // android.support.v4.view.ViewPager.SimpleOnPageChangeListener, android.support.v4.view.ViewPager.OnPageChangeListener
            public void onPageScrolled(int i, float f, int i2) {
                if (PagedTileLayout.this.mPageIndicator == null) {
                    return;
                }
                PagedTileLayout.this.mPageIndicatorPosition = i + f;
                PagedTileLayout.this.mPageIndicator.setLocation(PagedTileLayout.this.mPageIndicatorPosition);
                if (PagedTileLayout.this.mPageListener != null) {
                    PageListener pageListener = PagedTileLayout.this.mPageListener;
                    boolean z = true;
                    if (i2 != 0 || (!PagedTileLayout.this.isLayoutRtl() ? i != 0 : i != PagedTileLayout.this.mPages.size() - 1)) {
                        z = false;
                    }
                    pageListener.onPageChanged(z);
                }
            }
        };
        this.mAdapter = new PagerAdapter() { // from class: com.android.systemui.qs.PagedTileLayout.4
            AnonymousClass4() {
            }

            @Override // android.support.v4.view.PagerAdapter
            public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
                viewGroup.removeView((View) obj);
                PagedTileLayout.this.updateListening();
            }

            @Override // android.support.v4.view.PagerAdapter
            public Object instantiateItem(ViewGroup viewGroup, int i) {
                if (PagedTileLayout.this.isLayoutRtl()) {
                    i = (PagedTileLayout.this.mPages.size() - 1) - i;
                }
                ViewGroup viewGroup2 = (ViewGroup) PagedTileLayout.this.mPages.get(i);
                try {
                    viewGroup.addView(viewGroup2);
                } catch (IllegalStateException e) {
                    Log.e("PagedTileLayout", "Err when add " + viewGroup2 + " to " + i);
                }
                PagedTileLayout.this.updateListening();
                return viewGroup2;
            }

            @Override // android.support.v4.view.PagerAdapter
            public int getCount() {
                return PagedTileLayout.this.mNumPages;
            }

            @Override // android.support.v4.view.PagerAdapter
            public boolean isViewFromObject(View view, Object obj) {
                return view == obj;
            }
        };
        this.mScroller = new Scroller(context, SCROLL_CUBIC);
        setAdapter(this.mAdapter);
        setOnPageChangeListener(this.mOnPageChangeListener);
        setCurrentItem(0, false);
    }

    @Override // android.view.View
    public void onRtlPropertiesChanged(int i) throws Resources.NotFoundException {
        super.onRtlPropertiesChanged(i);
        setAdapter(this.mAdapter);
        setCurrentItem(0, false);
    }

    @Override // android.support.v4.view.ViewPager
    public void setCurrentItem(int i, boolean z) throws Resources.NotFoundException {
        if (isLayoutRtl()) {
            i = (this.mPages.size() - 1) - i;
        }
        super.setCurrentItem(i, z);
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void setListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        updateListening();
    }

    private void updateListening() {
        Iterator<TilePage> it = this.mPages.iterator();
        while (it.hasNext()) {
            TilePage next = it.next();
            next.setListening(next.getParent() == null ? false : this.mListening);
        }
    }

    @Override // android.support.v4.view.ViewPager, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mAnimatingToPage != -1) {
            return true;
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override // android.support.v4.view.ViewPager, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mAnimatingToPage != -1) {
            return true;
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override // android.support.v4.view.ViewPager, android.view.View
    public void computeScroll() throws Resources.NotFoundException {
        if (!this.mScroller.isFinished() && this.mScroller.computeScrollOffset()) {
            scrollTo(this.mScroller.getCurrX(), this.mScroller.getCurrY());
            float scrollX = getScrollX() / getWidth();
            int i = (int) scrollX;
            this.mOnPageChangeListener.onPageScrolled(i, scrollX - i, getScrollX());
            postInvalidateOnAnimation();
            return;
        }
        if (this.mAnimatingToPage != -1) {
            setCurrentItem(this.mAnimatingToPage, true);
            this.mBounceAnimatorSet.start();
            setOffscreenPageLimit(1);
            this.mAnimatingToPage = -1;
        }
        super.computeScroll();
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPages.add((TilePage) LayoutInflater.from(getContext()).inflate(R.layout.qs_paged_page, (ViewGroup) this, false));
    }

    public void setPageIndicator(PageIndicator pageIndicator) {
        this.mPageIndicator = pageIndicator;
        this.mPageIndicator.setNumPages(this.mNumPages);
        this.mPageIndicator.setLocation(this.mPageIndicatorPosition);
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public int getOffsetTop(QSPanel.TileRecord tileRecord) {
        ViewGroup viewGroup = (ViewGroup) tileRecord.tileView.getParent();
        if (viewGroup == null) {
            return 0;
        }
        return viewGroup.getTop() + getTop();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void addTile(QSPanel.TileRecord tileRecord) {
        this.mTiles.add(tileRecord);
        postDistributeTiles();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void removeTile(QSPanel.TileRecord tileRecord) {
        if (this.mTiles.remove(tileRecord)) {
            postDistributeTiles();
        }
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void setExpansion(float f) {
        this.mLastExpansion = f;
        updateSelected();
    }

    private void updateSelected() {
        if (this.mLastExpansion > 0.0f && this.mLastExpansion < 1.0f) {
            return;
        }
        boolean z = this.mLastExpansion == 1.0f;
        setImportantForAccessibility(4);
        int i = 0;
        while (i < this.mPages.size()) {
            this.mPages.get(i).setSelected(i == getCurrentItem() ? z : false);
            i++;
        }
        setImportantForAccessibility(0);
    }

    public void setPageListener(PageListener pageListener) {
        this.mPageListener = pageListener;
    }

    private void postDistributeTiles() {
        removeCallbacks(this.mDistribute);
        post(this.mDistribute);
    }

    private void distributeTiles() throws Resources.NotFoundException {
        int size = this.mPages.size();
        for (int i = 0; i < size; i++) {
            this.mPages.get(i).removeAllViews();
        }
        int size2 = this.mTiles.size();
        int i2 = 0;
        for (int i3 = 0; i3 < size2; i3++) {
            QSPanel.TileRecord tileRecord = this.mTiles.get(i3);
            if (this.mPages.get(i2).isFull() && (i2 = i2 + 1) == this.mPages.size()) {
                this.mPages.add((TilePage) LayoutInflater.from(getContext()).inflate(R.layout.qs_paged_page, (ViewGroup) this, false));
            }
            this.mPages.get(i2).addTile(tileRecord);
        }
        int i4 = i2 + 1;
        if (this.mNumPages != i4) {
            this.mNumPages = i4;
            while (this.mPages.size() > this.mNumPages) {
                this.mPages.remove(this.mPages.size() - 1);
            }
            this.mPageIndicator.setNumPages(this.mNumPages);
            setAdapter(this.mAdapter);
            this.mAdapter.notifyDataSetChanged();
            setCurrentItem(0, false);
        }
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public boolean updateResources() throws Resources.NotFoundException {
        setPadding(0, 0, 0, getContext().getResources().getDimensionPixelSize(R.dimen.qs_paged_tile_layout_padding_bottom));
        boolean zUpdateResources = false;
        for (int i = 0; i < this.mPages.size(); i++) {
            zUpdateResources |= this.mPages.get(i).updateResources();
        }
        if (zUpdateResources) {
            distributeTiles();
        }
        return zUpdateResources;
    }

    @Override // android.support.v4.view.ViewPager, android.view.View
    protected void onMeasure(int i, int i2) throws Resources.NotFoundException {
        super.onMeasure(i, i2);
        int childCount = getChildCount();
        int i3 = 0;
        for (int i4 = 0; i4 < childCount; i4++) {
            int measuredHeight = getChildAt(i4).getMeasuredHeight();
            if (measuredHeight > i3) {
                i3 = measuredHeight;
            }
        }
        setMeasuredDimension(getMeasuredWidth(), i3 + getPaddingBottom());
    }

    /* renamed from: com.android.systemui.qs.PagedTileLayout$1 */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() throws Resources.NotFoundException {
            PagedTileLayout.this.distributeTiles();
        }
    }

    public int getColumnCount() {
        if (this.mPages.size() == 0) {
            return 0;
        }
        return this.mPages.get(0).mColumns;
    }

    public void startTileReveal(Set<String> set, Runnable runnable) {
        if (set.isEmpty() || this.mPages.size() < 2 || getScrollX() != 0) {
            return;
        }
        int size = this.mPages.size() - 1;
        TilePage tilePage = this.mPages.get(size);
        ArrayList arrayList = new ArrayList();
        Iterator<QSPanel.TileRecord> it = tilePage.mRecords.iterator();
        while (it.hasNext()) {
            QSPanel.TileRecord next = it.next();
            if (set.contains(next.tile.getTileSpec())) {
                arrayList.add(setupBounceAnimator(next.tileView, arrayList.size()));
            }
        }
        if (arrayList.isEmpty()) {
            return;
        }
        this.mBounceAnimatorSet = new AnimatorSet();
        this.mBounceAnimatorSet.playTogether(arrayList);
        this.mBounceAnimatorSet.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.qs.PagedTileLayout.2
            final /* synthetic */ Runnable val$postAnimation;

            AnonymousClass2(Runnable runnable2) {
                runnable = runnable2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                PagedTileLayout.this.mBounceAnimatorSet = null;
                runnable.run();
            }
        });
        this.mAnimatingToPage = size;
        setOffscreenPageLimit(this.mAnimatingToPage);
        this.mScroller.startScroll(getScrollX(), getScrollY(), getWidth() * this.mAnimatingToPage, 0, 750);
        postInvalidateOnAnimation();
    }

    /* renamed from: com.android.systemui.qs.PagedTileLayout$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        final /* synthetic */ Runnable val$postAnimation;

        AnonymousClass2(Runnable runnable2) {
            runnable = runnable2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            PagedTileLayout.this.mBounceAnimatorSet = null;
            runnable.run();
        }
    }

    private static Animator setupBounceAnimator(View view, int i) {
        view.setAlpha(0.0f);
        view.setScaleX(0.0f);
        view.setScaleY(0.0f);
        ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat((Property<?, Float>) View.ALPHA, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_X, 1.0f), PropertyValuesHolder.ofFloat((Property<?, Float>) View.SCALE_Y, 1.0f));
        objectAnimatorOfPropertyValuesHolder.setDuration(450L);
        objectAnimatorOfPropertyValuesHolder.setStartDelay(i * 85);
        objectAnimatorOfPropertyValuesHolder.setInterpolator(new OvershootInterpolator(1.3f));
        return objectAnimatorOfPropertyValuesHolder;
    }

    /* renamed from: com.android.systemui.qs.PagedTileLayout$3 */
    class AnonymousClass3 extends ViewPager.SimpleOnPageChangeListener {
        AnonymousClass3() {
        }

        @Override // android.support.v4.view.ViewPager.SimpleOnPageChangeListener, android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageSelected(int i) {
            PagedTileLayout.this.updateSelected();
            if (PagedTileLayout.this.mPageIndicator != null && PagedTileLayout.this.mPageListener != null) {
                PageListener pageListener = PagedTileLayout.this.mPageListener;
                boolean z = false;
                if (!PagedTileLayout.this.isLayoutRtl() ? i == 0 : i == PagedTileLayout.this.mPages.size() - 1) {
                    z = true;
                }
                pageListener.onPageChanged(z);
            }
        }

        @Override // android.support.v4.view.ViewPager.SimpleOnPageChangeListener, android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
            if (PagedTileLayout.this.mPageIndicator == null) {
                return;
            }
            PagedTileLayout.this.mPageIndicatorPosition = i + f;
            PagedTileLayout.this.mPageIndicator.setLocation(PagedTileLayout.this.mPageIndicatorPosition);
            if (PagedTileLayout.this.mPageListener != null) {
                PageListener pageListener = PagedTileLayout.this.mPageListener;
                boolean z = true;
                if (i2 != 0 || (!PagedTileLayout.this.isLayoutRtl() ? i != 0 : i != PagedTileLayout.this.mPages.size() - 1)) {
                    z = false;
                }
                pageListener.onPageChanged(z);
            }
        }
    }

    public static class TilePage extends TileLayout {
        private int mMaxRows;

        public TilePage(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.mMaxRows = 3;
            updateResources();
        }

        @Override // com.android.systemui.qs.TileLayout, com.android.systemui.qs.QSPanel.QSTileLayout
        public boolean updateResources() {
            int rows = getRows();
            boolean z = rows != this.mMaxRows;
            if (z) {
                this.mMaxRows = rows;
                requestLayout();
            }
            return super.updateResources() || z;
        }

        private int getRows() {
            return Math.max(1, getResources().getInteger(R.integer.quick_settings_num_rows));
        }

        public boolean isFull() {
            return this.mRecords.size() >= this.mColumns * this.mMaxRows;
        }
    }

    /* renamed from: com.android.systemui.qs.PagedTileLayout$4 */
    class AnonymousClass4 extends PagerAdapter {
        AnonymousClass4() {
        }

        @Override // android.support.v4.view.PagerAdapter
        public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
            viewGroup.removeView((View) obj);
            PagedTileLayout.this.updateListening();
        }

        @Override // android.support.v4.view.PagerAdapter
        public Object instantiateItem(ViewGroup viewGroup, int i) {
            if (PagedTileLayout.this.isLayoutRtl()) {
                i = (PagedTileLayout.this.mPages.size() - 1) - i;
            }
            ViewGroup viewGroup2 = (ViewGroup) PagedTileLayout.this.mPages.get(i);
            try {
                viewGroup.addView(viewGroup2);
            } catch (IllegalStateException e) {
                Log.e("PagedTileLayout", "Err when add " + viewGroup2 + " to " + i);
            }
            PagedTileLayout.this.updateListening();
            return viewGroup2;
        }

        @Override // android.support.v4.view.PagerAdapter
        public int getCount() {
            return PagedTileLayout.this.mNumPages;
        }

        @Override // android.support.v4.view.PagerAdapter
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }
    }
}
