package com.android.calendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
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

    public void setIndexer(HeaderIndexer headerIndexer) {
        this.mIndexer = headerIndexer;
    }

    public void setListView(ListView listView) {
        this.mListView = listView;
        this.mListView.setOnScrollListener(this);
        this.mListViewHeadersCount = this.mListView.getHeaderViewsCount();
    }

    public void setOnScrollListener(AbsListView.OnScrollListener onScrollListener) {
        this.mListener = onScrollListener;
    }

    public void setHeaderHeightListener(HeaderHeightListener headerHeightListener) {
        this.mHeaderHeightListener = headerHeightListener;
    }

    public StickyHeaderListView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (this.mListener != null) {
            this.mListener.onScrollStateChanged(absListView, i);
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        updateStickyHeader(i);
        if (this.mListener != null) {
            this.mListener.onScroll(absListView, i, i2, i3);
        }
    }

    public void setHeaderSeparator(int i, int i2) {
        this.mSeparatorView = new View(this.mContext);
        this.mSeparatorView.setLayoutParams(new FrameLayout.LayoutParams(-1, i2, 48));
        this.mSeparatorView.setBackgroundColor(i);
        this.mSeparatorWidth = i2;
        addView(this.mSeparatorView);
    }

    protected void updateStickyHeader(int i) {
        boolean z;
        int headerItemsNumber;
        if (this.mAdapter == null && this.mListView != null) {
            setAdapter(this.mListView.getAdapter());
        }
        int i2 = i - this.mListViewHeadersCount;
        if (this.mAdapter != null && this.mIndexer != null && this.mDoHeaderReset) {
            int headerPositionFromItemPosition = this.mIndexer.getHeaderPositionFromItemPosition(i2);
            if (headerPositionFromItemPosition != this.mCurrentSectionPos) {
                if (headerPositionFromItemPosition == -1) {
                    removeView(this.mStickyHeader);
                    this.mStickyHeader = this.mDummyHeader;
                    if (this.mSeparatorView != null) {
                        this.mSeparatorView.setVisibility(8);
                    }
                    headerItemsNumber = 0;
                } else {
                    headerItemsNumber = this.mIndexer.getHeaderItemsNumber(headerPositionFromItemPosition);
                    View view = this.mAdapter.getView(this.mListViewHeadersCount + headerPositionFromItemPosition, null, this.mListView);
                    view.measure(View.MeasureSpec.makeMeasureSpec(this.mListView.getWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(this.mListView.getHeight(), Integer.MIN_VALUE));
                    removeView(this.mStickyHeader);
                    this.mStickyHeader = view;
                }
                this.mCurrentSectionPos = headerPositionFromItemPosition;
                this.mNextSectionPosition = headerItemsNumber + headerPositionFromItemPosition + 1;
                z = true;
            } else {
                z = false;
            }
            if (this.mStickyHeader != null) {
                int i3 = (this.mNextSectionPosition - i2) - 1;
                int height = this.mStickyHeader.getHeight();
                if (height == 0) {
                    height = this.mStickyHeader.getMeasuredHeight();
                }
                if (this.mHeaderHeightListener != null && this.mLastStickyHeaderHeight != height) {
                    this.mLastStickyHeaderHeight = height;
                    this.mHeaderHeightListener.OnHeaderHeightChanged(height);
                }
                View childAt = this.mListView.getChildAt(i3);
                if (childAt != null && childAt.getBottom() <= height) {
                    this.mStickyHeader.setTranslationY(childAt.getBottom() - height);
                    if (this.mSeparatorView != null) {
                        this.mSeparatorView.setVisibility(8);
                    }
                } else if (height != 0) {
                    this.mStickyHeader.setTranslationY(0.0f);
                    if (this.mSeparatorView != null && !this.mStickyHeader.equals(this.mDummyHeader)) {
                        this.mSeparatorView.setVisibility(0);
                    }
                }
                if (z) {
                    this.mStickyHeader.setVisibility(4);
                    addView(this.mStickyHeader);
                    if (this.mSeparatorView != null && !this.mStickyHeader.equals(this.mDummyHeader)) {
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, this.mSeparatorWidth);
                        layoutParams.setMargins(0, this.mStickyHeader.getMeasuredHeight(), 0, 0);
                        this.mSeparatorView.setLayoutParams(layoutParams);
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
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ?? childAt = getChildAt(i);
            if (childAt instanceof ListView) {
                setListView(childAt);
            }
        }
        if (this.mListView == null) {
            setListView(new ListView(this.mContext));
        }
        this.mDummyHeader = new View(this.mContext);
        this.mDummyHeader.setLayoutParams(new FrameLayout.LayoutParams(-1, 1, 48));
        this.mDummyHeader.setBackgroundColor(0);
        this.mChildViewsCreated = true;
    }
}
