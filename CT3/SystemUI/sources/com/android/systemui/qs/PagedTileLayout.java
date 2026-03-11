package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;

public class PagedTileLayout extends ViewPager implements QSPanel.QSTileLayout {
    private final PagerAdapter mAdapter;
    private View mDecorGroup;
    private final Runnable mDistribute;
    private boolean mListening;
    private int mNumPages;
    private boolean mOffPage;
    private PageIndicator mPageIndicator;
    private PageListener mPageListener;
    private final ArrayList<TilePage> mPages;
    private int mPosition;
    private final ArrayList<QSPanel.TileRecord> mTiles;

    public interface PageListener {
        void onPageChanged(boolean z);
    }

    public PagedTileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTiles = new ArrayList<>();
        this.mPages = new ArrayList<>();
        this.mDistribute = new Runnable() {
            @Override
            public void run() {
                PagedTileLayout.this.distributeTiles();
            }
        };
        this.mAdapter = new PagerAdapter() {
            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                if (PagedTileLayout.this.isLayoutRtl()) {
                    position = (PagedTileLayout.this.mPages.size() - 1) - position;
                }
                ViewGroup view = (ViewGroup) PagedTileLayout.this.mPages.get(position);
                container.addView(view);
                return view;
            }

            @Override
            public int getCount() {
                return PagedTileLayout.this.mNumPages;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        };
        setAdapter(this.mAdapter);
        setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                boolean z = true;
                if (PagedTileLayout.this.mPageIndicator == null || PagedTileLayout.this.mPageListener == null) {
                    return;
                }
                PageListener pageListener = PagedTileLayout.this.mPageListener;
                if (PagedTileLayout.this.isLayoutRtl()) {
                    if (position != PagedTileLayout.this.mPages.size() - 1) {
                        z = false;
                    }
                } else if (position != 0) {
                    z = false;
                }
                pageListener.onPageChanged(z);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                boolean z = false;
                if (PagedTileLayout.this.mPageIndicator == null) {
                    return;
                }
                PagedTileLayout.this.setCurrentPage(PagedTileLayout.this.isLayoutRtl() ? (PagedTileLayout.this.mPages.size() - 1) - position : position, positionOffset != 0.0f);
                PagedTileLayout.this.mPageIndicator.setLocation(position + positionOffset);
                if (PagedTileLayout.this.mPageListener == null) {
                    return;
                }
                PageListener pageListener = PagedTileLayout.this.mPageListener;
                if (positionOffsetPixels == 0 && (!PagedTileLayout.this.isLayoutRtl() ? position == 0 : position == PagedTileLayout.this.mPages.size() - 1)) {
                    z = true;
                }
                pageListener.onPageChanged(z);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        setCurrentItem(0);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        setAdapter(this.mAdapter);
        setCurrentItem(0, false);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        if (isLayoutRtl()) {
            item = (this.mPages.size() - 1) - item;
        }
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (this.mListening) {
            this.mPages.get(this.mPosition).setListening(listening);
            if (!this.mOffPage) {
                return;
            }
            this.mPages.get(this.mPosition + 1).setListening(listening);
            return;
        }
        for (int i = 0; i < this.mPages.size(); i++) {
            this.mPages.get(i).setListening(false);
        }
    }

    public void setCurrentPage(int position, boolean offPage) {
        if (this.mPosition == position && this.mOffPage == offPage) {
            return;
        }
        if (this.mListening) {
            if (this.mPosition != position) {
                setPageListening(this.mPosition, false);
                if (this.mOffPage) {
                    setPageListening(this.mPosition + 1, false);
                }
                setPageListening(position, true);
                if (offPage) {
                    setPageListening(position + 1, true);
                }
            } else if (this.mOffPage != offPage) {
                setPageListening(this.mPosition + 1, offPage);
            }
        }
        this.mPosition = position;
        this.mOffPage = offPage;
    }

    private void setPageListening(int position, boolean listening) {
        if (position >= this.mPages.size()) {
            return;
        }
        this.mPages.get(position).setListening(listening);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPageIndicator = (PageIndicator) findViewById(R.id.page_indicator);
        this.mDecorGroup = findViewById(R.id.page_decor);
        ((ViewPager.LayoutParams) this.mDecorGroup.getLayoutParams()).isDecor = true;
        this.mPages.add((TilePage) LayoutInflater.from(this.mContext).inflate(R.layout.qs_paged_page, (ViewGroup) this, false));
    }

    @Override
    public int getOffsetTop(QSPanel.TileRecord tile) {
        ViewGroup parent = (ViewGroup) tile.tileView.getParent();
        if (parent == null) {
            return 0;
        }
        return parent.getTop() + getTop();
    }

    @Override
    public void addTile(QSPanel.TileRecord tile) {
        this.mTiles.add(tile);
        postDistributeTiles();
    }

    @Override
    public void removeTile(QSPanel.TileRecord tile) {
        if (!this.mTiles.remove(tile)) {
            return;
        }
        postDistributeTiles();
    }

    public void setPageListener(PageListener listener) {
        this.mPageListener = listener;
    }

    private void postDistributeTiles() {
        removeCallbacks(this.mDistribute);
        post(this.mDistribute);
    }

    public void distributeTiles() {
        int NP = this.mPages.size();
        for (int i = 0; i < NP; i++) {
            this.mPages.get(i).removeAllViews();
        }
        int index = 0;
        int NT = this.mTiles.size();
        for (int i2 = 0; i2 < NT; i2++) {
            QSPanel.TileRecord tile = this.mTiles.get(i2);
            if (this.mPages.get(index).isFull() && (index = index + 1) == this.mPages.size()) {
                this.mPages.add((TilePage) LayoutInflater.from(this.mContext).inflate(R.layout.qs_paged_page, (ViewGroup) this, false));
            }
            this.mPages.get(index).addTile(tile);
        }
        if (this.mNumPages == index + 1) {
            return;
        }
        this.mNumPages = index + 1;
        while (this.mPages.size() > this.mNumPages) {
            this.mPages.remove(this.mPages.size() - 1);
        }
        this.mPageIndicator.setNumPages(this.mNumPages);
        setAdapter(this.mAdapter);
        this.mAdapter.notifyDataSetChanged();
        setCurrentItem(0, false);
    }

    @Override
    public boolean updateResources() {
        boolean changed = false;
        for (int i = 0; i < this.mPages.size(); i++) {
            changed |= this.mPages.get(i).updateResources();
        }
        if (changed) {
            distributeTiles();
        }
        return changed;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int maxHeight = 0;
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            int height = getChildAt(i).getMeasuredHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        setMeasuredDimension(getMeasuredWidth(), this.mDecorGroup.getMeasuredHeight() + maxHeight);
    }

    public int getColumnCount() {
        if (this.mPages.size() == 0) {
            return 0;
        }
        return this.mPages.get(0).mColumns;
    }

    public static class TilePage extends TileLayout {
        private int mMaxRows;

        public TilePage(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mMaxRows = 3;
            updateResources();
            setContentDescription(this.mContext.getString(R.string.accessibility_desc_quick_settings));
        }

        @Override
        public boolean updateResources() {
            int rows = getRows();
            boolean changed = rows != this.mMaxRows;
            if (changed) {
                this.mMaxRows = rows;
                requestLayout();
            }
            if (super.updateResources()) {
                return true;
            }
            return changed;
        }

        private int getRows() {
            Resources res = getContext().getResources();
            if (res.getConfiguration().orientation == 1) {
                return 3;
            }
            return Math.max(1, res.getInteger(R.integer.quick_settings_num_rows));
        }

        public boolean isFull() {
            return this.mRecords.size() >= this.mColumns * this.mMaxRows;
        }
    }
}
