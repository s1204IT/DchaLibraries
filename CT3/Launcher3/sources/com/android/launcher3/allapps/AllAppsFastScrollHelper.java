package com.android.launcher3.allapps;

import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import com.android.launcher3.BaseRecyclerViewFastScrollBar;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import java.util.HashSet;

public class AllAppsFastScrollHelper implements AllAppsGridAdapter.BindViewCallback {
    private AlphabeticalAppsList mApps;
    String mCurrentFastScrollSection;
    int mFastScrollFrameIndex;
    private boolean mHasFastScrollTouchSettled;
    private boolean mHasFastScrollTouchSettledAtLeastOnce;
    private AllAppsRecyclerView mRv;
    String mTargetFastScrollSection;
    int mTargetFastScrollPosition = -1;
    private HashSet<BaseRecyclerViewFastScrollBar.FastScrollFocusableView> mTrackedFastScrollViews = new HashSet<>();
    final int[] mFastScrollFrames = new int[10];
    Runnable mSmoothSnapNextFrameRunnable = new Runnable() {
        @Override
        public void run() {
            if (AllAppsFastScrollHelper.this.mFastScrollFrameIndex >= AllAppsFastScrollHelper.this.mFastScrollFrames.length) {
                return;
            }
            AllAppsFastScrollHelper.this.mRv.scrollBy(0, AllAppsFastScrollHelper.this.mFastScrollFrames[AllAppsFastScrollHelper.this.mFastScrollFrameIndex]);
            AllAppsFastScrollHelper.this.mFastScrollFrameIndex++;
            AllAppsFastScrollHelper.this.mRv.postOnAnimation(AllAppsFastScrollHelper.this.mSmoothSnapNextFrameRunnable);
        }
    };
    Runnable mFastScrollToTargetSectionRunnable = new Runnable() {
        @Override
        public void run() {
            AllAppsFastScrollHelper.this.mCurrentFastScrollSection = AllAppsFastScrollHelper.this.mTargetFastScrollSection;
            AllAppsFastScrollHelper.this.mHasFastScrollTouchSettled = true;
            AllAppsFastScrollHelper.this.mHasFastScrollTouchSettledAtLeastOnce = true;
            AllAppsFastScrollHelper.this.updateTrackedViewsFastScrollFocusState();
        }
    };

    public AllAppsFastScrollHelper(AllAppsRecyclerView rv, AlphabeticalAppsList apps) {
        this.mRv = rv;
        this.mApps = apps;
    }

    public void onSetAdapter(AllAppsGridAdapter adapter) {
        adapter.setBindViewCallback(this);
    }

    public boolean smoothScrollToSection(int scrollY, int availableScrollHeight, AlphabeticalAppsList.FastScrollSectionInfo info) {
        if (this.mTargetFastScrollPosition != info.fastScrollToItem.position) {
            this.mTargetFastScrollPosition = info.fastScrollToItem.position;
            smoothSnapToPosition(scrollY, availableScrollHeight, info);
            return true;
        }
        return false;
    }

    private void smoothSnapToPosition(int scrollY, int availableScrollHeight, AlphabeticalAppsList.FastScrollSectionInfo info) {
        int i;
        this.mRv.removeCallbacks(this.mSmoothSnapNextFrameRunnable);
        this.mRv.removeCallbacks(this.mFastScrollToTargetSectionRunnable);
        trackAllChildViews();
        if (this.mHasFastScrollTouchSettled) {
            this.mCurrentFastScrollSection = info.sectionName;
            this.mTargetFastScrollSection = null;
            updateTrackedViewsFastScrollFocusState();
        } else {
            this.mCurrentFastScrollSection = null;
            this.mTargetFastScrollSection = info.sectionName;
            this.mHasFastScrollTouchSettled = false;
            updateTrackedViewsFastScrollFocusState();
            AllAppsRecyclerView allAppsRecyclerView = this.mRv;
            Runnable runnable = this.mFastScrollToTargetSectionRunnable;
            if (this.mHasFastScrollTouchSettledAtLeastOnce) {
                i = 200;
            } else {
                i = 100;
            }
            allAppsRecyclerView.postDelayed(runnable, i);
        }
        int newScrollY = Math.min(availableScrollHeight, this.mRv.getPaddingTop() + this.mRv.getTop(info.fastScrollToItem.rowIndex));
        int numFrames = this.mFastScrollFrames.length;
        for (int i2 = 0; i2 < numFrames; i2++) {
            this.mFastScrollFrames[i2] = (newScrollY - scrollY) / numFrames;
        }
        this.mFastScrollFrameIndex = 0;
        this.mRv.postOnAnimation(this.mSmoothSnapNextFrameRunnable);
    }

    public void onFastScrollCompleted() {
        this.mRv.removeCallbacks(this.mSmoothSnapNextFrameRunnable);
        this.mRv.removeCallbacks(this.mFastScrollToTargetSectionRunnable);
        this.mHasFastScrollTouchSettled = false;
        this.mHasFastScrollTouchSettledAtLeastOnce = false;
        this.mCurrentFastScrollSection = null;
        this.mTargetFastScrollSection = null;
        this.mTargetFastScrollPosition = -1;
        updateTrackedViewsFastScrollFocusState();
        this.mTrackedFastScrollViews.clear();
    }

    @Override
    public void onBindView(AllAppsGridAdapter.ViewHolder holder) {
        if ((this.mCurrentFastScrollSection == null && this.mTargetFastScrollSection == null) || !(holder.mContent instanceof BaseRecyclerViewFastScrollBar.FastScrollFocusableView)) {
            return;
        }
        BaseRecyclerViewFastScrollBar.FastScrollFocusableView v = (BaseRecyclerViewFastScrollBar.FastScrollFocusableView) holder.mContent;
        updateViewFastScrollFocusState(v, holder.getPosition(), false);
        this.mTrackedFastScrollViews.add(v);
    }

    private void trackAllChildViews() {
        int childCount = this.mRv.getChildCount();
        for (int i = 0; i < childCount; i++) {
            KeyEvent.Callback childAt = this.mRv.getChildAt(i);
            if (childAt instanceof BaseRecyclerViewFastScrollBar.FastScrollFocusableView) {
                this.mTrackedFastScrollViews.add((BaseRecyclerViewFastScrollBar.FastScrollFocusableView) childAt);
            }
        }
    }

    private void updateTrackedViewsFastScrollFocusState() {
        for (BaseRecyclerViewFastScrollBar.FastScrollFocusableView fastScrollFocusableView : this.mTrackedFastScrollViews) {
            RecyclerView.ViewHolder viewHolder = this.mRv.getChildViewHolder((View) fastScrollFocusableView);
            int pos = viewHolder != null ? viewHolder.getPosition() : -1;
            updateViewFastScrollFocusState(fastScrollFocusableView, pos, true);
        }
    }

    private void updateViewFastScrollFocusState(BaseRecyclerViewFastScrollBar.FastScrollFocusableView v, int pos, boolean animated) {
        boolean highlight = false;
        FastBitmapDrawable.State newState = FastBitmapDrawable.State.NORMAL;
        if (this.mCurrentFastScrollSection != null && pos > -1) {
            AlphabeticalAppsList.AdapterItem item = this.mApps.getAdapterItems().get(pos);
            if (item.sectionName.equals(this.mCurrentFastScrollSection) && item.position == this.mTargetFastScrollPosition) {
                highlight = true;
            }
            newState = highlight ? FastBitmapDrawable.State.FAST_SCROLL_HIGHLIGHTED : FastBitmapDrawable.State.FAST_SCROLL_UNHIGHLIGHTED;
        }
        v.setFastScrollFocusState(newState, animated);
    }
}
