package androidx.slice.widget;

import android.R;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LargeTemplateView extends SliceChildView {
    private final LargeSliceAdapter mAdapter;
    private ArrayList<SliceItem> mDisplayedItems;
    private int mDisplayedItemsHeight;
    private final View mForeground;
    private ListContent mListContent;
    private int[] mLoc;
    private SliceView mParent;
    private final RecyclerView mRecyclerView;
    private boolean mScrollingEnabled;

    public LargeTemplateView(Context context) {
        super(context);
        this.mDisplayedItems = new ArrayList<>();
        this.mDisplayedItemsHeight = 0;
        this.mLoc = new int[2];
        this.mRecyclerView = new RecyclerView(getContext());
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        this.mAdapter = new LargeSliceAdapter(context);
        this.mRecyclerView.setAdapter(this.mAdapter);
        addView(this.mRecyclerView);
        this.mForeground = new View(getContext());
        this.mForeground.setBackground(SliceViewUtil.getDrawable(getContext(), R.attr.selectableItemBackground));
        addView(this.mForeground);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mForeground.getLayoutParams();
        lp.width = -1;
        lp.height = -1;
        this.mForeground.setLayoutParams(lp);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mParent = (SliceView) getParent();
        this.mAdapter.setParents(this.mParent, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (!this.mScrollingEnabled && this.mDisplayedItems.size() > 0 && this.mDisplayedItemsHeight != height) {
            updateDisplayedItems(height);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void onForegroundActivated(MotionEvent event) {
        if (this.mParent != null && !this.mParent.isSliceViewClickable()) {
            this.mForeground.setPressed(false);
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.mForeground.getLocationOnScreen(this.mLoc);
            int x = (int) (event.getRawX() - this.mLoc[0]);
            int y = (int) (event.getRawY() - this.mLoc[1]);
            this.mForeground.getBackground().setHotspot(x, y);
        }
        int action = event.getActionMasked();
        if (action == 0) {
            this.mForeground.setPressed(true);
        } else if (action == 3 || action == 1 || action == 2) {
            this.mForeground.setPressed(false);
        }
    }

    @Override
    public void setMode(int newMode) {
        if (this.mMode != newMode) {
            this.mMode = newMode;
            if (this.mListContent != null && this.mListContent.isValid()) {
                int sliceHeight = this.mListContent.getLargeHeight(-1, this.mScrollingEnabled);
                updateDisplayedItems(sliceHeight);
            }
        }
    }

    @Override
    public int getActualHeight() {
        return this.mDisplayedItemsHeight;
    }

    @Override
    public int getSmallHeight() {
        if (this.mListContent == null || !this.mListContent.isValid()) {
            return 0;
        }
        return this.mListContent.getSmallHeight();
    }

    @Override
    public void setTint(int tint) {
        super.setTint(tint);
        updateDisplayedItems(getMeasuredHeight());
    }

    @Override
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        this.mObserver = observer;
        if (this.mAdapter != null) {
            this.mAdapter.setSliceObserver(this.mObserver);
        }
    }

    @Override
    public void setSliceActions(List<SliceAction> actions) {
        this.mAdapter.setSliceActions(actions);
    }

    @Override
    public void setSliceContent(ListContent sliceContent) {
        this.mListContent = sliceContent;
        int sliceHeight = this.mListContent.getLargeHeight(-1, this.mScrollingEnabled);
        updateDisplayedItems(sliceHeight);
    }

    @Override
    public void setStyle(AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super.setStyle(attrs, defStyleAttrs, defStyleRes);
        this.mAdapter.setStyle(attrs, defStyleAttrs, defStyleRes);
    }

    @Override
    public void setShowLastUpdated(boolean showLastUpdated) {
        super.setShowLastUpdated(showLastUpdated);
        this.mAdapter.setShowLastUpdated(showLastUpdated);
    }

    @Override
    public void setLastUpdated(long lastUpdated) {
        super.setLastUpdated(lastUpdated);
        this.mAdapter.setLastUpdated(lastUpdated);
    }

    public void setScrollable(boolean scrollingEnabled) {
        if (this.mScrollingEnabled != scrollingEnabled) {
            this.mScrollingEnabled = scrollingEnabled;
            if (this.mListContent != null && this.mListContent.isValid()) {
                int sliceHeight = this.mListContent.getLargeHeight(-1, this.mScrollingEnabled);
                updateDisplayedItems(sliceHeight);
            }
        }
    }

    private void updateDisplayedItems(int height) {
        if (this.mListContent == null || !this.mListContent.isValid()) {
            resetView();
            return;
        }
        int mode = getMode();
        if (mode == 1) {
            this.mDisplayedItems = new ArrayList<>(Arrays.asList(this.mListContent.getRowItems().get(0)));
        } else if (!this.mScrollingEnabled && height != 0) {
            this.mDisplayedItems = this.mListContent.getItemsForNonScrollingList(height);
        } else {
            this.mDisplayedItems = this.mListContent.getRowItems();
        }
        this.mDisplayedItemsHeight = this.mListContent.getListHeight(this.mDisplayedItems);
        this.mAdapter.setSliceItems(this.mDisplayedItems, this.mTintColor, mode);
        updateOverscroll();
    }

    private void updateOverscroll() {
        boolean scrollable = this.mDisplayedItemsHeight > getMeasuredHeight();
        this.mRecyclerView.setOverScrollMode((this.mScrollingEnabled && scrollable) ? 1 : 2);
    }

    @Override
    public void resetView() {
        this.mDisplayedItemsHeight = 0;
        this.mDisplayedItems.clear();
        this.mAdapter.setSliceItems(null, -1, getMode());
        this.mListContent = null;
    }
}
