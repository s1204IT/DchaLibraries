package com.android.calendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.ListView;

public class StickyHeaderListView extends FrameLayout implements AbsListView.OnScrollListener {
    protected Adapter mAdapter;
    protected boolean mChildViewsCreated;
    protected Context mContext;
    protected int mCurrentSectionPos;
    protected boolean mDoHeaderReset;
    protected View mDummyHeader;
    protected HeaderHeightListener mHeaderHeightListener;
    protected HeaderIndexer mIndexer;
    private int mLastStickyHeaderHeight;
    protected ListView mListView;
    protected int mListViewHeadersCount;
    protected AbsListView.OnScrollListener mListener;
    protected int mNextSectionPosition;
    private View mSeparatorView;
    private int mSeparatorWidth;
    protected View mStickyHeader;

    public interface HeaderHeightListener {
        void OnHeaderHeightChanged(int i);
    }

    public interface HeaderIndexer {
        int getHeaderItemsNumber(int i);

        int getHeaderPositionFromItemPosition(int i);
    }

    public void setAdapter(Adapter adapter) {
        if (adapter != null) {
            this.mAdapter = adapter;
        }
    }

    public void setIndexer(HeaderIndexer indexer) {
        this.mIndexer = indexer;
    }

    public void setListView(ListView lv) {
        this.mListView = lv;
        this.mListView.setOnScrollListener(this);
        this.mListViewHeadersCount = this.mListView.getHeaderViewsCount();
    }

    public void setOnScrollListener(AbsListView.OnScrollListener listener) {
        this.mListener = listener;
    }

    public void setHeaderHeightListener(HeaderHeightListener listener) {
        this.mHeaderHeightListener = listener;
    }

    public StickyHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mChildViewsCreated = false;
        this.mDoHeaderReset = false;
        this.mContext = null;
        this.mAdapter = null;
        this.mIndexer = null;
        this.mHeaderHeightListener = null;
        this.mStickyHeader = null;
        this.mDummyHeader = null;
        this.mListView = null;
        this.mListener = null;
        this.mLastStickyHeaderHeight = 0;
        this.mCurrentSectionPos = -1;
        this.mNextSectionPosition = -1;
        this.mListViewHeadersCount = 0;
        this.mContext = context;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (this.mListener != null) {
            this.mListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        updateStickyHeader(firstVisibleItem);
        if (this.mListener != null) {
            this.mListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    public void setHeaderSeparator(int color, int width) {
        this.mSeparatorView = new View(this.mContext);
        ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(-1, width, 48);
        this.mSeparatorView.setLayoutParams(params);
        this.mSeparatorView.setBackgroundColor(color);
        this.mSeparatorWidth = width;
        addView(this.mSeparatorView);
    }

    protected void updateStickyHeader(int firstVisibleItem) {
        int sectionSize;
        if (this.mAdapter == null && this.mListView != null) {
            setAdapter(this.mListView.getAdapter());
        }
        int firstVisibleItem2 = firstVisibleItem - this.mListViewHeadersCount;
        if (this.mAdapter != null && this.mIndexer != null && this.mDoHeaderReset) {
            int sectionPos = this.mIndexer.getHeaderPositionFromItemPosition(firstVisibleItem2);
            boolean newView = false;
            if (sectionPos != this.mCurrentSectionPos) {
                if (sectionPos == -1) {
                    sectionSize = 0;
                    removeView(this.mStickyHeader);
                    this.mStickyHeader = this.mDummyHeader;
                    if (this.mSeparatorView != null) {
                        this.mSeparatorView.setVisibility(8);
                    }
                    newView = true;
                } else {
                    sectionSize = this.mIndexer.getHeaderItemsNumber(sectionPos);
                    View v = this.mAdapter.getView(this.mListViewHeadersCount + sectionPos, null, this.mListView);
                    v.measure(View.MeasureSpec.makeMeasureSpec(this.mListView.getWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(this.mListView.getHeight(), Integer.MIN_VALUE));
                    removeView(this.mStickyHeader);
                    this.mStickyHeader = v;
                    newView = true;
                }
                this.mCurrentSectionPos = sectionPos;
                this.mNextSectionPosition = sectionSize + sectionPos + 1;
            }
            if (this.mStickyHeader != null) {
                int sectionLastItemPosition = (this.mNextSectionPosition - firstVisibleItem2) - 1;
                int stickyHeaderHeight = this.mStickyHeader.getHeight();
                if (stickyHeaderHeight == 0) {
                    stickyHeaderHeight = this.mStickyHeader.getMeasuredHeight();
                }
                if (this.mHeaderHeightListener != null && this.mLastStickyHeaderHeight != stickyHeaderHeight) {
                    this.mLastStickyHeaderHeight = stickyHeaderHeight;
                    this.mHeaderHeightListener.OnHeaderHeightChanged(stickyHeaderHeight);
                }
                View SectionLastView = this.mListView.getChildAt(sectionLastItemPosition);
                if (SectionLastView != null && SectionLastView.getBottom() <= stickyHeaderHeight) {
                    int lastViewBottom = SectionLastView.getBottom();
                    this.mStickyHeader.setTranslationY(lastViewBottom - stickyHeaderHeight);
                    if (this.mSeparatorView != null) {
                        this.mSeparatorView.setVisibility(8);
                    }
                } else if (stickyHeaderHeight != 0) {
                    this.mStickyHeader.setTranslationY(0.0f);
                    if (this.mSeparatorView != null && !this.mStickyHeader.equals(this.mDummyHeader)) {
                        this.mSeparatorView.setVisibility(0);
                    }
                }
                if (newView) {
                    this.mStickyHeader.setVisibility(4);
                    addView(this.mStickyHeader);
                    if (this.mSeparatorView != null && !this.mStickyHeader.equals(this.mDummyHeader)) {
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, this.mSeparatorWidth);
                        params.setMargins(0, this.mStickyHeader.getMeasuredHeight(), 0, 0);
                        this.mSeparatorView.setLayoutParams(params);
                        this.mSeparatorView.setVisibility(0);
                    }
                    this.mStickyHeader.setVisibility(0);
                }
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!this.mChildViewsCreated) {
            setChildViews();
        }
        this.mDoHeaderReset = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mChildViewsCreated) {
            setChildViews();
        }
        this.mDoHeaderReset = true;
    }

    private void setChildViews() {
        int iChildNum = getChildCount();
        for (int i = 0; i < iChildNum; i++) {
            View v = getChildAt(i);
            if (v instanceof ListView) {
                setListView((ListView) v);
            }
        }
        if (this.mListView == null) {
            setListView(new ListView(this.mContext));
        }
        this.mDummyHeader = new View(this.mContext);
        ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(-1, 1, 48);
        this.mDummyHeader.setLayoutParams(params);
        this.mDummyHeader.setBackgroundColor(0);
        this.mChildViewsCreated = true;
    }
}
